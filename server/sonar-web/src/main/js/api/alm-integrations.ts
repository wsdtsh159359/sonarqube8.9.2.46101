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
import { get, getJSON, parseError, post, postJSON } from 'sonar-ui-common/helpers/request';
import throwGlobalError from '../app/utils/throwGlobalError';
import {
  AzureProject,
  AzureRepository,
  BitbucketProject,
  BitbucketRepository,
  GithubOrganization,
  GithubRepository,
  GitlabProject
} from '../types/alm-integration';
import { ProjectBase } from './components';

export function setAlmPersonalAccessToken(almSetting: string, pat: string): Promise<void> {
  return post('/api/alm_integrations/set_pat', { almSetting, pat }).catch(throwGlobalError);
}

export function checkPersonalAccessTokenIsValid(
  almSetting: string
): Promise<{ status: boolean; error?: string }> {
  return get('/api/alm_integrations/check_pat', { almSetting })
    .then(() => ({ status: true }))
    .catch(async (response: Response) => {
      if (response.status === 400) {
        const error = await parseError(response);
        return { status: false, error };
      }
      return throwGlobalError(response);
    });
}

export function getAzureProjects(almSetting: string): Promise<{ projects: AzureProject[] }> {
  return getJSON('/api/alm_integrations/list_azure_projects', { almSetting }).catch(
    throwGlobalError
  );
}

export function getAzureRepositories(
  almSetting: string,
  projectName: string
): Promise<{ repositories: AzureRepository[] }> {
  return getJSON('/api/alm_integrations/search_azure_repos', { almSetting, projectName }).catch(
    throwGlobalError
  );
}

export function searchAzureRepositories(
  almSetting: string,
  searchQuery: string
): Promise<{ repositories: AzureRepository[] }> {
  return getJSON('/api/alm_integrations/search_azure_repos', { almSetting, searchQuery }).catch(
    throwGlobalError
  );
}

export function importAzureRepository(
  almSetting: string,
  projectName: string,
  repositoryName: string
): Promise<{ project: ProjectBase }> {
  return postJSON('/api/alm_integrations/import_azure_project', {
    almSetting,
    projectName,
    repositoryName
  }).catch(throwGlobalError);
}

export function getBitbucketServerProjects(
  almSetting: string
): Promise<{ projects: BitbucketProject[] }> {
  return getJSON('/api/alm_integrations/list_bitbucketserver_projects', { almSetting });
}

export function getBitbucketServerRepositories(
  almSetting: string,
  projectName: string
): Promise<{
  isLastPage: boolean;
  repositories: BitbucketRepository[];
}> {
  return getJSON('/api/alm_integrations/search_bitbucketserver_repos', {
    almSetting,
    projectName
  });
}

export function importBitbucketServerProject(
  almSetting: string,
  projectKey: string,
  repositorySlug: string
): Promise<{ project: ProjectBase }> {
  return postJSON('/api/alm_integrations/import_bitbucketserver_project', {
    almSetting,
    projectKey,
    repositorySlug
  }).catch(throwGlobalError);
}

export function searchForBitbucketServerRepositories(
  almSetting: string,
  repositoryName: string
): Promise<{
  isLastPage: boolean;
  repositories: BitbucketRepository[];
}> {
  return getJSON('/api/alm_integrations/search_bitbucketserver_repos', {
    almSetting,
    repositoryName
  });
}

export function getGithubClientId(almSetting: string): Promise<{ clientId?: string }> {
  return getJSON('/api/alm_integrations/get_github_client_id', { almSetting });
}

export function importGithubRepository(
  almSetting: string,
  organization: string,
  repositoryKey: string
): Promise<{ project: ProjectBase }> {
  return postJSON('/api/alm_integrations/import_github_project', {
    almSetting,
    organization,
    repositoryKey
  }).catch(throwGlobalError);
}

export function getGithubOrganizations(
  almSetting: string,
  token: string
): Promise<{ organizations: GithubOrganization[] }> {
  return getJSON('/api/alm_integrations/list_github_organizations', {
    almSetting,
    token
  }).catch((response?: Response) => {
    if (response && response.status !== 400) {
      throwGlobalError(response);
    }
  });
}

export function getGithubRepositories(data: {
  almSetting: string;
  organization: string;
  pageSize: number;
  page?: number;
  query?: string;
}): Promise<{ repositories: GithubRepository[]; paging: T.Paging }> {
  const { almSetting, organization, pageSize, page = 1, query } = data;
  return getJSON('/api/alm_integrations/list_github_repositories', {
    almSetting,
    organization,
    p: page,
    ps: pageSize,
    q: query || undefined
  }).catch(throwGlobalError);
}

export function getGitlabProjects(data: {
  almSetting: string;
  page?: number;
  pageSize?: number;
  query?: string;
}): Promise<{ projects: GitlabProject[]; projectsPaging: T.Paging }> {
  const { almSetting, pageSize, page, query } = data;
  return getJSON('/api/alm_integrations/search_gitlab_repos', {
    almSetting,
    projectName: query || undefined,
    p: page,
    ps: pageSize
  })
    .then(({ repositories, paging }) => ({ projects: repositories, projectsPaging: paging }))
    .catch(throwGlobalError);
}

export function importGitlabProject(data: {
  almSetting: string;
  gitlabProjectId: string;
}): Promise<{ project: ProjectBase }> {
  const { almSetting, gitlabProjectId } = data;
  return postJSON('/api/alm_integrations/import_gitlab_project', {
    almSetting,
    gitlabProjectId
  }).catch(throwGlobalError);
}
