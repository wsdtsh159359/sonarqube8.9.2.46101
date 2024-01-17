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
import { getJSON, post, postJSON } from 'sonar-ui-common/helpers/request';
import throwGlobalError from '../app/utils/throwGlobalError';
import { GetRulesAppResponse, SearchRulesResponse } from '../types/coding-rules';

export function getRulesApp(): Promise<GetRulesAppResponse> {
  return getJSON('/api/rules/app').catch(throwGlobalError);
}

export function searchRules(data: {
  activation?: boolean | string;
  active_severities?: string;
  asc?: boolean | string;
  available_since?: string;
  cwe?: string;
  f?: string;
  facets?: string;
  include_external?: boolean | string;
  inheritance?: string;
  is_template?: boolean | string;
  languages?: string;
  owaspTop10?: string;
  p?: number;
  ps?: number;
  q?: string;
  qprofile?: string;
  repositories?: string;
  rule_key?: string;
  s?: string;
  sansTop25?: string;
  severities?: string;
  sonarsourceSecurity?: string;
  statuses?: string;
  tags?: string;
  template_key?: string;
  types?: string;
}): Promise<SearchRulesResponse> {
  return getJSON('/api/rules/search', data).catch(throwGlobalError);
}

export function takeFacet(response: SearchRulesResponse, property: string) {
  const facet = response.facets?.find(f => f.property === property);
  return facet ? facet.values : [];
}

export function getRuleDetails(parameters: {
  actives?: boolean;
  key: string;
}): Promise<{ actives?: T.RuleActivation[]; rule: T.RuleDetails }> {
  return getJSON('/api/rules/show', parameters).catch(throwGlobalError);
}

export function getRuleTags(parameters: { ps?: number; q: string }): Promise<string[]> {
  return getJSON('/api/rules/tags', parameters).then(r => r.tags, throwGlobalError);
}

export function createRule(data: {
  custom_key: string;
  markdown_description: string;
  name: string;
  params?: string;
  prevent_reactivation?: boolean;
  severity?: string;
  status?: string;
  template_key: string;
  type?: string;
}): Promise<T.RuleDetails> {
  return postJSON('/api/rules/create', data).then(
    r => r.rule,
    response => {
      // do not show global error if the status code is 409
      // this case should be handled inside a component
      if (response && response.status === 409) {
        return Promise.reject(response);
      } else {
        return throwGlobalError(response);
      }
    }
  );
}

export function deleteRule(parameters: { key: string }) {
  return post('/api/rules/delete', parameters).catch(throwGlobalError);
}

export function updateRule(data: {
  key: string;
  markdown_description?: string;
  markdown_note?: string;
  name?: string;
  params?: string;
  remediation_fn_base_effort?: string;
  remediation_fn_type?: string;
  remediation_fy_gap_multiplier?: string;
  severity?: string;
  status?: string;
  tags?: string;
}): Promise<T.RuleDetails> {
  return postJSON('/api/rules/update', data).then(r => r.rule, throwGlobalError);
}
