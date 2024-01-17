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
import { omit, uniqBy } from 'lodash';
import * as React from 'react';
import { lazyLoadComponent } from 'sonar-ui-common/components/lazyLoadComponent';
import { get, save } from 'sonar-ui-common/helpers/storage';
import { getRulesApp } from '../../api/rules';
import { ComponentDescriptor, RuleDescriptor, WorkspaceContext } from './context';
import './styles.css';
import WorkspacePortal from './WorkspacePortal';

const WORKSPACE = 'sonarqube-workspace';
const WorkspaceNav = lazyLoadComponent(() => import('./WorkspaceNav'), 'WorkspaceNav');
const WorkspaceRuleViewer = lazyLoadComponent(
  () => import('./WorkspaceRuleViewer'),
  'WorkspaceRuleViewer'
);
const WorkspaceComponentViewer = lazyLoadComponent(
  () => import('./WorkspaceComponentViewer'),
  'WorkspaceComponentViewer'
);

interface State {
  components: ComponentDescriptor[];
  externalRulesRepoNames: T.Dict<string>;
  height: number;
  maximized?: boolean;
  open: { component?: string; rule?: string };
  rules: RuleDescriptor[];
}

export const MIN_HEIGHT = 0.05;
export const MAX_HEIGHT = 0.85;
export const INITIAL_HEIGHT = 300;

export const TYPE_KEY = '__type__';
export enum WorkspaceTypes {
  Rule = 'rule',
  Component = 'component'
}

export default class Workspace extends React.PureComponent<{}, State> {
  mounted = false;

  constructor(props: {}) {
    super(props);
    this.state = {
      externalRulesRepoNames: {},
      height: INITIAL_HEIGHT,
      open: {},
      ...this.loadWorkspace()
    };
  }

  componentDidMount() {
    this.mounted = true;
    this.fetchRuleNames();
  }

  componentDidUpdate(_: {}, prevState: State) {
    if (prevState.components !== this.state.components || prevState.rules !== this.state.rules) {
      this.saveWorkspace();
    }
  }

  componentWillUnmount() {
    this.mounted = false;
  }

  fetchRuleNames = async () => {
    const { repositories } = await getRulesApp();
    const externalRulesRepoNames: T.Dict<string> = {};
    repositories
      .filter(({ key }) => key.startsWith('external_'))
      .forEach(({ key, name }) => {
        externalRulesRepoNames[key.replace('external_', '')] = name;
      });
    this.setState({ externalRulesRepoNames });
  };

  loadWorkspace = () => {
    try {
      const data: any[] = JSON.parse(get(WORKSPACE) || '');
      const components: ComponentDescriptor[] = data.filter(
        x => x[TYPE_KEY] === WorkspaceTypes.Component
      );
      const rules: RuleDescriptor[] = data.filter(x => x[TYPE_KEY] === WorkspaceTypes.Rule);
      return { components, rules };
    } catch {
      // Fail silently.
      return { components: [], rules: [] };
    }
  };

  saveWorkspace = () => {
    const data = [
      // Do not save line number, next time the file is open, it should be open
      // on the first line.
      ...this.state.components.map(x =>
        omit({ ...x, [TYPE_KEY]: WorkspaceTypes.Component }, 'line')
      ),
      ...this.state.rules.map(x => ({ ...x, [TYPE_KEY]: WorkspaceTypes.Rule }))
    ];
    save(WORKSPACE, JSON.stringify(data));
  };

  handleOpenComponent = (component: ComponentDescriptor) => {
    this.setState((state: State) => ({
      components: uniqBy([...state.components, component], c => c.key),
      open: { component: component.key }
    }));
  };

  handleComponentReopen = (componentKey: string) => {
    this.setState({ open: { component: componentKey } });
  };

  handleOpenRule = (rule: RuleDescriptor) => {
    this.setState((state: State) => ({
      open: { rule: rule.key },
      rules: uniqBy([...state.rules, rule], r => r.key)
    }));
  };

  handleRuleReopen = (ruleKey: string) => {
    this.setState({ open: { rule: ruleKey } });
  };

  handleComponentClose = (componentKey: string) => {
    this.setState((state: State) => ({
      components: state.components.filter(x => x.key !== componentKey),
      open: {
        ...state.open,
        component: state.open.component === componentKey ? undefined : state.open.component
      }
    }));
  };

  handleRuleClose = (ruleKey: string) => {
    this.setState((state: State) => ({
      rules: state.rules.filter(x => x.key !== ruleKey),
      open: {
        ...state.open,
        rule: state.open.rule === ruleKey ? undefined : state.open.rule
      }
    }));
  };

  handleComponentLoad = (details: { key: string; name: string; qualifier: string }) => {
    if (this.mounted) {
      const { key, name, qualifier } = details;
      this.setState((state: State) => ({
        components: state.components.map(component =>
          component.key === key ? { ...component, name, qualifier } : component
        )
      }));
    }
  };

  handleRuleLoad = (details: { key: string; name: string }) => {
    if (this.mounted) {
      const { key, name } = details;
      this.setState((state: State) => ({
        rules: state.rules.map(rule => (rule.key === key ? { ...rule, name } : rule))
      }));
    }
  };

  handleCollapse = () => {
    this.setState({ open: {} });
  };

  handleMaximize = () => {
    this.setState({ maximized: true });
  };

  handleMinimize = () => {
    this.setState({ maximized: false });
  };

  handleResize = (deltaY: number) => {
    const minHeight = window.innerHeight * MIN_HEIGHT;
    const maxHeight = window.innerHeight * MAX_HEIGHT;
    this.setState((state: State) => ({
      height: Math.min(maxHeight, Math.max(minHeight, state.height - deltaY))
    }));
  };

  render() {
    const { components, externalRulesRepoNames, height, maximized, open, rules } = this.state;

    const openComponent = open.component && components.find(x => x.key === open.component);
    const openRule = open.rule && rules.find(x => x.key === open.rule);

    const actualHeight = maximized ? window.innerHeight * MAX_HEIGHT : height;

    return (
      <WorkspaceContext.Provider
        value={{
          externalRulesRepoNames,
          openComponent: this.handleOpenComponent,
          openRule: this.handleOpenRule
        }}>
        {this.props.children}
        <WorkspacePortal>
          {(components.length > 0 || rules.length > 0) && (
            <WorkspaceNav
              components={components}
              onComponentClose={this.handleComponentClose}
              onComponentOpen={this.handleComponentReopen}
              onRuleClose={this.handleRuleClose}
              onRuleOpen={this.handleRuleReopen}
              open={open}
              rules={rules}
            />
          )}
          {openComponent && (
            <WorkspaceComponentViewer
              component={openComponent}
              height={actualHeight}
              maximized={maximized}
              onClose={this.handleComponentClose}
              onCollapse={this.handleCollapse}
              onLoad={this.handleComponentLoad}
              onMaximize={this.handleMaximize}
              onMinimize={this.handleMinimize}
              onResize={this.handleResize}
            />
          )}
          {openRule && (
            <WorkspaceRuleViewer
              height={actualHeight}
              maximized={maximized}
              onClose={this.handleRuleClose}
              onCollapse={this.handleCollapse}
              onLoad={this.handleRuleLoad}
              onMaximize={this.handleMaximize}
              onMinimize={this.handleMinimize}
              onResize={this.handleResize}
              rule={openRule}
            />
          )}
        </WorkspacePortal>
      </WorkspaceContext.Provider>
    );
  }
}
