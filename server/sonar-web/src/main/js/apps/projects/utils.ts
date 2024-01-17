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
import { invert } from 'lodash';
import { translate, translateWithParameters } from 'sonar-ui-common/helpers/l10n';
import { RequestData } from 'sonar-ui-common/helpers/request';
import { Facet, searchProjects } from '../../api/components';
import { getMeasuresForProjects } from '../../api/measures';
import { isDiffMetric } from '../../helpers/measures';
import { MetricKey } from '../../types/metrics';
import { convertToFilter, Query } from './query';

interface SortingOption {
  class?: string;
  value: string;
}

export const PROJECTS_DEFAULT_FILTER = 'sonarqube.projects.default';
export const PROJECTS_FAVORITE = 'favorite';
export const PROJECTS_ALL = 'all';

export const SORTING_METRICS: SortingOption[] = [
  { value: 'name' },
  { value: 'analysis_date' },
  { value: 'reliability' },
  { value: 'security' },
  { value: 'security_review' },
  { value: 'maintainability' },
  { value: 'coverage' },
  { value: 'duplications' },
  { value: 'size' }
];

export const SORTING_LEAK_METRICS: SortingOption[] = [
  { value: 'name' },
  { value: 'analysis_date' },
  { value: 'new_reliability', class: 'projects-leak-sorting-option' },
  { value: 'new_security', class: 'projects-leak-sorting-option' },
  { value: 'new_security_review', class: 'projects-leak-sorting-option' },
  { value: 'new_maintainability', class: 'projects-leak-sorting-option' },
  { value: 'new_coverage', class: 'projects-leak-sorting-option' },
  { value: 'new_duplications', class: 'projects-leak-sorting-option' },
  { value: 'new_lines', class: 'projects-leak-sorting-option' }
];

export const SORTING_SWITCH: T.Dict<string> = {
  analysis_date: 'analysis_date',
  name: 'name',
  reliability: 'new_reliability',
  security: 'new_security',
  security_review: 'new_security_review',
  maintainability: 'new_maintainability',
  coverage: 'new_coverage',
  duplications: 'new_duplications',
  size: 'new_lines',
  new_reliability: 'reliability',
  new_security: 'security',
  new_security_review: 'security_review',
  new_maintainability: 'maintainability',
  new_coverage: 'coverage',
  new_duplications: 'duplications',
  new_lines: 'size'
};

export const VIEWS = [
  { value: 'overall', label: 'overall' },
  { value: 'leak', label: 'new_code' }
];

export const VISUALIZATIONS = [
  'risk',
  'reliability',
  'security',
  'maintainability',
  'coverage',
  'duplications'
];

const PAGE_SIZE = 50;
const PAGE_SIZE_VISUALIZATIONS = 99;

const METRICS = [
  MetricKey.alert_status,
  MetricKey.bugs,
  MetricKey.reliability_rating,
  MetricKey.vulnerabilities,
  MetricKey.security_rating,
  MetricKey.security_hotspots_reviewed,
  MetricKey.security_review_rating,
  MetricKey.code_smells,
  MetricKey.sqale_rating,
  MetricKey.duplicated_lines_density,
  MetricKey.coverage,
  MetricKey.ncloc,
  MetricKey.ncloc_language_distribution,
  MetricKey.projects
];

const LEAK_METRICS = [
  MetricKey.alert_status,
  MetricKey.new_bugs,
  MetricKey.new_reliability_rating,
  MetricKey.new_vulnerabilities,
  MetricKey.new_security_rating,
  MetricKey.new_security_hotspots_reviewed,
  MetricKey.new_security_review_rating,
  MetricKey.new_code_smells,
  MetricKey.new_maintainability_rating,
  MetricKey.new_coverage,
  MetricKey.new_duplicated_lines_density,
  MetricKey.new_lines,
  MetricKey.projects
];

const METRICS_BY_VISUALIZATION: T.Dict<string[]> = {
  risk: [
    MetricKey.reliability_rating,
    MetricKey.security_rating,
    MetricKey.coverage,
    MetricKey.ncloc,
    MetricKey.sqale_index
  ],
  // x, y, size, color
  reliability: [
    MetricKey.ncloc,
    MetricKey.reliability_remediation_effort,
    MetricKey.bugs,
    MetricKey.reliability_rating
  ],
  security: [
    MetricKey.ncloc,
    MetricKey.security_remediation_effort,
    MetricKey.vulnerabilities,
    MetricKey.security_rating
  ],
  maintainability: [
    MetricKey.ncloc,
    MetricKey.sqale_index,
    MetricKey.code_smells,
    MetricKey.sqale_rating
  ],
  coverage: [MetricKey.complexity, MetricKey.coverage, MetricKey.uncovered_lines],
  duplications: [MetricKey.ncloc, MetricKey.duplicated_lines_density, MetricKey.duplicated_blocks]
};

export const FACETS = [
  'reliability_rating',
  'security_rating',
  'security_review_rating',
  'sqale_rating',
  'coverage',
  'duplicated_lines_density',
  'ncloc',
  'alert_status',
  'languages',
  'tags',
  'qualifier'
];

export const LEAK_FACETS = [
  'new_reliability_rating',
  'new_security_rating',
  'new_security_review_rating',
  'new_maintainability_rating',
  'new_coverage',
  'new_duplicated_lines_density',
  'new_lines',
  'alert_status',
  'languages',
  'tags',
  'qualifier'
];

const REVERSED_FACETS = ['coverage', 'new_coverage'];

export function localizeSorting(sort?: string): string {
  return translate('projects.sort', sort || 'name');
}

export function parseSorting(sort: string): { sortValue: string; sortDesc: boolean } {
  const desc = sort[0] === '-';
  return { sortValue: desc ? sort.substr(1) : sort, sortDesc: desc };
}

export function fetchProjects(query: Query, isFavorite: boolean, pageIndex = 1) {
  const ps = query.view === 'visualizations' ? PAGE_SIZE_VISUALIZATIONS : PAGE_SIZE;
  const data = convertToQueryData(query, isFavorite, {
    p: pageIndex > 1 ? pageIndex : undefined,
    ps,
    facets: defineFacets(query).join(),
    f: 'analysisDate,leakPeriodDate'
  });
  return searchProjects(data)
    .then(response =>
      Promise.all([fetchProjectMeasures(response.components, query), Promise.resolve(response)])
    )
    .then(([measures, { components, facets, paging }]) => {
      return {
        facets: getFacetsMap(facets),
        projects: components.map(component => {
          const componentMeasures: T.Dict<string> = {};
          measures
            .filter(measure => measure.component === component.key)
            .forEach(measure => {
              const value = isDiffMetric(measure.metric) ? measure.period?.value : measure.value;
              if (value !== undefined) {
                componentMeasures[measure.metric] = value;
              }
            });
          return { ...component, measures: componentMeasures };
        }),
        total: paging.total
      };
    });
}

function defineMetrics(query: Query): string[] {
  switch (query.view) {
    case 'visualizations':
      return METRICS_BY_VISUALIZATION[query.visualization || 'risk'];
    case 'leak':
      return LEAK_METRICS;
    default:
      return METRICS;
  }
}

function defineFacets(query: Query): string[] {
  if (query.view === 'leak') {
    return LEAK_FACETS;
  }
  return FACETS;
}

function convertToQueryData(query: Query, isFavorite: boolean, defaultData = {}) {
  const data: RequestData = { ...defaultData };
  const filter = convertToFilter(query, isFavorite);
  const sort = convertToSorting(query);

  if (filter) {
    data.filter = filter;
  }
  if (sort.s) {
    data.s = sort.s;
  }
  if (sort.asc !== undefined) {
    data.asc = sort.asc;
  }
  return data;
}

export function fetchProjectMeasures(projects: Array<{ key: string }>, query: Query) {
  if (!projects.length) {
    return Promise.resolve([]);
  }

  const projectKeys = projects.map(project => project.key);
  const metrics = defineMetrics(query);
  return getMeasuresForProjects(projectKeys, metrics);
}

function mapFacetValues(values: Array<{ val: string; count: number }>) {
  const map: T.Dict<number> = {};
  values.forEach(value => {
    map[value.val] = value.count;
  });
  return map;
}

const propertyToMetricMap: T.Dict<string | undefined> = {
  analysis_date: 'analysisDate',
  reliability: 'reliability_rating',
  new_reliability: 'new_reliability_rating',
  security: 'security_rating',
  new_security: 'new_security_rating',
  security_review: 'security_review_rating',
  new_security_review: 'new_security_review_rating',
  maintainability: 'sqale_rating',
  new_maintainability: 'new_maintainability_rating',
  coverage: 'coverage',
  new_coverage: 'new_coverage',
  duplications: 'duplicated_lines_density',
  new_duplications: 'new_duplicated_lines_density',
  size: 'ncloc',
  new_lines: 'new_lines',
  gate: 'alert_status',
  languages: 'languages',
  tags: 'tags',
  search: 'query',
  qualifier: 'qualifier'
};

const metricToPropertyMap = invert(propertyToMetricMap);

function getFacetsMap(facets: Facet[]) {
  const map: T.Dict<T.Dict<number>> = {};
  facets.forEach(facet => {
    const property = metricToPropertyMap[facet.property];
    const { values } = facet;
    if (REVERSED_FACETS.includes(property)) {
      values.reverse();
    }
    map[property] = mapFacetValues(values);
  });
  return map;
}

function convertToSorting({ sort }: Query): { s?: string; asc?: boolean } {
  if (sort && sort[0] === '-') {
    return { s: propertyToMetricMap[sort.substr(1)], asc: false };
  }
  return { s: propertyToMetricMap[sort || ''] };
}

const ONE_MINUTE = 60000;
const ONE_HOUR = 60 * ONE_MINUTE;
const ONE_DAY = 24 * ONE_HOUR;
const ONE_MONTH = 30 * ONE_DAY;
const ONE_YEAR = 12 * ONE_MONTH;

function format(periods: Array<{ value: number; label: string }>) {
  let result = '';
  let count = 0;
  let lastId = -1;
  for (let i = 0; i < periods.length && count < 2; i++) {
    if (periods[i].value > 0) {
      count++;
      if (lastId < 0 || lastId + 1 === i) {
        lastId = i;
        result += translateWithParameters(periods[i].label, periods[i].value) + ' ';
      }
    }
  }
  return result;
}

export function formatDuration(ms: number) {
  if (ms < ONE_MINUTE) {
    return translate('duration.seconds');
  }
  const years = Math.floor(ms / ONE_YEAR);
  ms -= years * ONE_YEAR;
  const months = Math.floor(ms / ONE_MONTH);
  ms -= months * ONE_MONTH;
  const days = Math.floor(ms / ONE_DAY);
  ms -= days * ONE_DAY;
  const hours = Math.floor(ms / ONE_HOUR);
  ms -= hours * ONE_HOUR;
  const minutes = Math.floor(ms / ONE_MINUTE);
  return format([
    { value: years, label: 'duration.years' },
    { value: months, label: 'duration.months' },
    { value: days, label: 'duration.days' },
    { value: hours, label: 'duration.hours' },
    { value: minutes, label: 'duration.minutes' }
  ]);
}
