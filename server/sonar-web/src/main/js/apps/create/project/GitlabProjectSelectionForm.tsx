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
import * as React from 'react';
import { FormattedMessage } from 'react-intl';
import { Link } from 'react-router';
import { Button } from 'sonar-ui-common/components/controls/buttons';
import ListFooter from 'sonar-ui-common/components/controls/ListFooter';
import SearchBox from 'sonar-ui-common/components/controls/SearchBox';
import Tooltip from 'sonar-ui-common/components/controls/Tooltip';
import CheckIcon from 'sonar-ui-common/components/icons/CheckIcon';
import DetachIcon from 'sonar-ui-common/components/icons/DetachIcon';
import QualifierIcon from 'sonar-ui-common/components/icons/QualifierIcon';
import { Alert } from 'sonar-ui-common/components/ui/Alert';
import DeferredSpinner from 'sonar-ui-common/components/ui/DeferredSpinner';
import { translate } from 'sonar-ui-common/helpers/l10n';
import { getProjectUrl } from '../../../helpers/urls';
import { GitlabProject } from '../../../types/alm-integration';
import { ComponentQualifier } from '../../../types/component';
import { CreateProjectModes } from './types';

export interface GitlabProjectSelectionFormProps {
  importingGitlabProjectId?: string;
  loadingMore: boolean;
  onImport: (gitlabProjectId: string) => void;
  onLoadMore: () => void;
  onSearch: (searchQuery: string) => void;
  projects?: GitlabProject[];
  projectsPaging: T.Paging;
  searching: boolean;
  searchQuery: string;
}

export default function GitlabProjectSelectionForm(props: GitlabProjectSelectionFormProps) {
  const {
    importingGitlabProjectId,
    loadingMore,
    projects = [],
    projectsPaging,
    searching,
    searchQuery
  } = props;

  if (projects.length === 0 && searchQuery.length === 0 && !searching) {
    return (
      <Alert className="spacer-top" variant="warning">
        <FormattedMessage
          defaultMessage={translate('onboarding.create_project.gitlab.no_projects')}
          id="onboarding.create_project.gitlab.no_projects"
          values={{
            link: (
              <Link
                to={{
                  pathname: '/projects/create',
                  query: { mode: CreateProjectModes.GitLab, resetPat: 1 }
                }}>
                {translate('onboarding.create_project.update_your_token')}
              </Link>
            )
          }}
        />
      </Alert>
    );
  }

  return (
    <div className="boxed-group big-padded create-project-import-gitlab">
      <SearchBox
        className="spacer"
        loading={searching}
        minLength={3}
        onChange={props.onSearch}
        placeholder={translate('onboarding.create_project.gitlab.search_prompt')}
      />

      <hr />

      {projects.length === 0 ? (
        <div className="padded">{translate('no_results')}</div>
      ) : (
        <table className="data zebra zebra-hover">
          <tbody>
            {projects.map(project => (
              <tr key={project.id}>
                <td>
                  <Tooltip overlay={project.slug}>
                    <strong className="project-name display-inline-block text-ellipsis">
                      {project.sqProjectKey ? (
                        <Link to={getProjectUrl(project.sqProjectKey)}>
                          <QualifierIcon
                            className="spacer-right"
                            qualifier={ComponentQualifier.Project}
                          />
                          {project.sqProjectName}
                        </Link>
                      ) : (
                        project.name
                      )}
                    </strong>
                  </Tooltip>
                  <br />
                  <Tooltip overlay={project.pathSlug}>
                    <span className="text-muted project-path display-inline-block text-ellipsis">
                      {project.pathName}
                    </span>
                  </Tooltip>
                </td>
                <td>
                  <a
                    className="display-inline-flex-center big-spacer-right"
                    href={project.url}
                    rel="noopener noreferrer"
                    target="_blank">
                    <DetachIcon className="little-spacer-right" />
                    {translate('onboarding.create_project.gitlab.link')}
                  </a>
                </td>
                {project.sqProjectKey ? (
                  <td>
                    <span className="display-flex-center display-flex-justify-end already-set-up">
                      <CheckIcon className="little-spacer-right" size={12} />
                      {translate('onboarding.create_project.repository_imported')}
                    </span>
                  </td>
                ) : (
                  <td className="text-right">
                    <Button
                      disabled={!!importingGitlabProjectId}
                      onClick={() => props.onImport(project.id)}>
                      {translate('onboarding.create_project.gitlab.set_up')}
                      {importingGitlabProjectId === project.id && (
                        <DeferredSpinner className="spacer-left" />
                      )}
                    </Button>
                  </td>
                )}
              </tr>
            ))}
          </tbody>
        </table>
      )}
      <ListFooter
        count={projects.length}
        loadMore={props.onLoadMore}
        loading={loadingMore}
        total={projectsPaging.total}
      />
    </div>
  );
}
