/*
 * SonarQube
 * Copyright (C) 2009-2021 SonarSource SA
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
import { differenceBy } from 'lodash';
import * as React from 'react';
import { connect } from 'react-redux';
import { getProjectAlmBinding } from '../../api/alm-settings';
import { getBranches, getPullRequests } from '../../api/branches';
import { getAnalysisStatus, getTasksForComponent } from '../../api/ce';
import { getComponentData } from '../../api/components';
import { getComponentNavigation } from '../../api/nav';
import { Location, Router, withRouter } from '../../components/hoc/withRouter';
import {
  getBranchLikeQuery,
  isBranch,
  isMainBranch,
  isPullRequest
} from '../../helpers/branch-like';
import { getPortfolioUrl } from '../../helpers/urls';
import { registerBranchStatus, requireAuthorization } from '../../store/rootActions';
import { ProjectAlmBindingResponse } from '../../types/alm-settings';
import { BranchLike } from '../../types/branch-like';
import { isPortfolioLike } from '../../types/component';
import { Task, TaskStatuses, TaskWarning } from '../../types/tasks';
import ComponentContainerNotFound from './ComponentContainerNotFound';
import { ComponentContext } from './ComponentContext';
import PageUnavailableDueToIndexation from './indexation/PageUnavailableDueToIndexation';
import ComponentNav from './nav/component/ComponentNav';

interface Props {
  children: React.ReactElement;
  location: Pick<Location, 'query' | 'pathname'>;
  registerBranchStatus: (branchLike: BranchLike, component: string, status: T.Status) => void;
  requireAuthorization: (router: Pick<Router, 'replace'>) => void;
  router: Pick<Router, 'replace'>;
}

interface State {
  branchLike?: BranchLike;
  branchLikes: BranchLike[];
  component?: T.Component;
  currentTask?: Task;
  isPending: boolean;
  loading: boolean;
  projectBinding?: ProjectAlmBindingResponse;
  tasksInProgress?: Task[];
  warnings: TaskWarning[];
}

const FETCH_STATUS_WAIT_TIME = 3000;

export class ComponentContainer extends React.PureComponent<Props, State> {
  watchStatusTimer?: number;
  mounted = false;
  state: State = { branchLikes: [], isPending: false, loading: true, warnings: [] };

  componentDidMount() {
    this.mounted = true;
    this.fetchComponent();
  }

  componentDidUpdate(prevProps: Props) {
    if (
      prevProps.location.query.id !== this.props.location.query.id ||
      prevProps.location.query.branch !== this.props.location.query.branch ||
      prevProps.location.query.pullRequest !== this.props.location.query.pullRequest
    ) {
      this.fetchComponent();
    }
  }

  componentWillUnmount() {
    this.mounted = false;
    window.clearTimeout(this.watchStatusTimer);
  }

  addQualifier = (component: T.Component) => ({
    ...component,
    qualifier: component.breadcrumbs[component.breadcrumbs.length - 1].qualifier
  });

  fetchComponent() {
    const { branch, id: key, pullRequest } = this.props.location.query;
    this.setState({ loading: true });

    const onError = (response?: Response) => {
      if (this.mounted) {
        if (response && response.status === 403) {
          this.props.requireAuthorization(this.props.router);
        } else {
          this.setState({ component: undefined, loading: false });
        }
      }
    };

    Promise.all([
      getComponentNavigation({ component: key, branch, pullRequest }),
      getComponentData({ component: key, branch, pullRequest }),
      getProjectAlmBinding(key).catch(() => undefined)
    ])
      .then(([nav, { component }, projectBinding]) => {
        const componentWithQualifier = this.addQualifier({ ...nav, ...component });

        /*
         * There used to be a redirect from /dashboard to /portfolio which caused issues.
         * Links should be fixed to not rely on this redirect, but:
         * This is a fail-safe in case there are still some faulty links remaining.
         */
        if (
          this.props.location.pathname.match('dashboard') &&
          isPortfolioLike(componentWithQualifier.qualifier)
        ) {
          this.props.router.replace(getPortfolioUrl(component.key));
        }

        if (this.mounted) {
          this.setState({ projectBinding });
        }

        return componentWithQualifier;
      }, onError)
      .then(this.fetchBranches)
      .then(
        ({ branchLike, branchLikes, component }) => {
          if (this.mounted) {
            this.setState({
              branchLike,
              branchLikes,
              component,
              loading: false
            });
            this.fetchStatus(component);
            this.fetchWarnings(component, branchLike);
          }
        },
        () => {}
      );
  }

  fetchBranches = (
    component: T.Component
  ): Promise<{
    branchLike?: BranchLike;
    branchLikes: BranchLike[];
    component: T.Component;
  }> => {
    const breadcrumb = component.breadcrumbs.find(({ qualifier }) => {
      return ['APP', 'TRK'].includes(qualifier);
    });

    if (breadcrumb) {
      const { key } = breadcrumb;
      return Promise.all([
        getBranches(key),
        breadcrumb.qualifier === 'APP' ? Promise.resolve([]) : getPullRequests(key)
      ]).then(([branches, pullRequests]) => {
        const branchLikes = [...branches, ...pullRequests];
        const branchLike = this.getCurrentBranchLike(branchLikes);

        this.registerBranchStatuses(branchLikes, component);

        return { branchLike, branchLikes, component };
      });
    } else {
      return Promise.resolve({ branchLikes: [], component });
    }
  };

  fetchStatus = (component: T.Component) => {
    getTasksForComponent(component.key).then(
      ({ current, queue }) => {
        if (this.mounted) {
          let shouldFetchComponent = false;
          this.setState(
            ({ branchLike, component, currentTask, tasksInProgress }) => {
              const newCurrentTask = this.getCurrentTask(current, branchLike);
              const pendingTasks = this.getPendingTasks(queue, branchLike);
              const newTasksInProgress = pendingTasks.filter(
                task => task.status === TaskStatuses.InProgress
              );

              const currentTaskChanged =
                (!currentTask && newCurrentTask) ||
                (currentTask && newCurrentTask && currentTask.id !== newCurrentTask.id);
              const progressChanged =
                tasksInProgress &&
                (newTasksInProgress.length !== tasksInProgress.length ||
                  differenceBy(newTasksInProgress, tasksInProgress, 'id').length > 0);

              shouldFetchComponent = Boolean(currentTaskChanged || progressChanged);
              if (
                !shouldFetchComponent &&
                component &&
                (newTasksInProgress.length > 0 || !component.analysisDate)
              ) {
                // Refresh the status as long as there is tasks in progress or no analysis
                window.clearTimeout(this.watchStatusTimer);
                this.watchStatusTimer = window.setTimeout(
                  () => this.fetchStatus(component),
                  FETCH_STATUS_WAIT_TIME
                );
              }

              const isPending = pendingTasks.some(task => task.status === TaskStatuses.Pending);
              return {
                currentTask: newCurrentTask,
                isPending,
                tasksInProgress: newTasksInProgress
              };
            },
            () => {
              if (shouldFetchComponent) {
                this.fetchComponent();
              }
            }
          );
        }
      },
      () => {}
    );
  };

  fetchWarnings = (component: T.Component, branchLike?: BranchLike) => {
    if (component.qualifier === 'TRK') {
      getAnalysisStatus({
        component: component.key,
        ...getBranchLikeQuery(branchLike)
      }).then(
        ({ component }) => {
          this.setState({ warnings: component.warnings });
        },
        () => {}
      );
    }
  };

  getCurrentBranchLike = (branchLikes: BranchLike[]) => {
    const { query } = this.props.location;
    return query.pullRequest
      ? branchLikes.find(b => isPullRequest(b) && b.key === query.pullRequest)
      : branchLikes.find(b => isBranch(b) && (query.branch ? b.name === query.branch : b.isMain));
  };

  getCurrentTask = (current: Task, branchLike?: BranchLike) => {
    if (!current) {
      return undefined;
    }

    return current.status === TaskStatuses.Failed || this.isSameBranch(current, branchLike)
      ? current
      : undefined;
  };

  getPendingTasks = (pendingTasks: Task[], branchLike?: BranchLike) => {
    return pendingTasks.filter(task => this.isSameBranch(task, branchLike));
  };

  isSameBranch = (task: Pick<Task, 'branch' | 'pullRequest'>, branchLike?: BranchLike) => {
    if (branchLike) {
      if (isMainBranch(branchLike)) {
        return (!task.pullRequest && !task.branch) || branchLike.name === task.branch;
      }
      if (isPullRequest(branchLike)) {
        return branchLike.key === task.pullRequest;
      }
      if (isBranch(branchLike)) {
        return branchLike.name === task.branch;
      }
    }
    return !task.branch && !task.pullRequest;
  };

  registerBranchStatuses = (branchLikes: BranchLike[], component: T.Component) => {
    branchLikes.forEach(branchLike => {
      if (branchLike.status) {
        this.props.registerBranchStatus(
          branchLike,
          component.key,
          branchLike.status.qualityGateStatus
        );
      }
    });
  };

  handleComponentChange = (changes: Partial<T.Component>) => {
    if (this.mounted) {
      this.setState(state => {
        if (state.component) {
          const newComponent: T.Component = { ...state.component, ...changes };
          return { component: newComponent };
        }
        return null;
      });
    }
  };

  handleBranchesChange = () => {
    if (this.mounted && this.state.component) {
      this.fetchBranches(this.state.component).then(
        ({ branchLike, branchLikes }) => {
          if (this.mounted) {
            this.setState({ branchLike, branchLikes });
          }
        },
        () => {}
      );
    }
  };

  handleWarningDismiss = () => {
    const { component } = this.state;
    if (component !== undefined) {
      this.fetchWarnings(component);
    }
  };

  render() {
    const { component, loading } = this.state;

    if (!loading && !component) {
      return <ComponentContainerNotFound />;
    }

    if (component?.needIssueSync) {
      return <PageUnavailableDueToIndexation component={component} />;
    }

    const {
      branchLike,
      branchLikes,
      currentTask,
      isPending,
      projectBinding,
      tasksInProgress
    } = this.state;
    const isInProgress = tasksInProgress && tasksInProgress.length > 0;

    return (
      <div>
        {component && !['FIL', 'UTS'].includes(component.qualifier) && (
          <ComponentNav
            branchLikes={branchLikes}
            component={component}
            currentBranchLike={branchLike}
            currentTask={currentTask}
            currentTaskOnSameBranch={currentTask && this.isSameBranch(currentTask, branchLike)}
            isInProgress={isInProgress}
            isPending={isPending}
            onComponentChange={this.handleComponentChange}
            onWarningDismiss={this.handleWarningDismiss}
            projectBinding={projectBinding}
            warnings={this.state.warnings}
          />
        )}
        {loading ? (
          <div className="page page-limited">
            <i className="spinner" />
          </div>
        ) : (
          <ComponentContext.Provider value={{ branchLike, component }}>
            {React.cloneElement(this.props.children, {
              branchLike,
              branchLikes,
              component,
              isInProgress,
              isPending,
              onBranchesChange: this.handleBranchesChange,
              onComponentChange: this.handleComponentChange,
              projectBinding
            })}
          </ComponentContext.Provider>
        )}
      </div>
    );
  }
}

const mapDispatchToProps = { registerBranchStatus, requireAuthorization };

export default withRouter(connect(null, mapDispatchToProps)(ComponentContainer));
