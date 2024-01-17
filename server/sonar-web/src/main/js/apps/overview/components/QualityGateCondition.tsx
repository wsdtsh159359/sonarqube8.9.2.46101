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
import * as classNames from 'classnames';
import * as React from 'react';
import { Link } from 'react-router';
import IssueTypeIcon from 'sonar-ui-common/components/icons/IssueTypeIcon';
import { translate } from 'sonar-ui-common/helpers/l10n';
import { formatMeasure } from 'sonar-ui-common/helpers/measures';
import Measure from '../../../components/measure/Measure';
import DrilldownLink from '../../../components/shared/DrilldownLink';
import { getBranchLikeQuery } from '../../../helpers/branch-like';
import { isDiffMetric } from '../../../helpers/measures';
import { getComponentIssuesUrl } from '../../../helpers/urls';
import { BranchLike } from '../../../types/branch-like';
import { QualityGateStatusConditionEnhanced } from '../../../types/quality-gates';

interface Props {
  branchLike?: BranchLike;
  component: Pick<T.Component, 'key'>;
  condition: QualityGateStatusConditionEnhanced;
}

export default class QualityGateCondition extends React.PureComponent<Props> {
  getIssuesUrl = (sinceLeakPeriod: boolean, customQuery: T.Dict<string>) => {
    const query: T.Dict<string | undefined> = {
      resolved: 'false',
      ...getBranchLikeQuery(this.props.branchLike),
      ...customQuery
    };
    if (sinceLeakPeriod) {
      Object.assign(query, { sinceLeakPeriod: 'true' });
    }
    return getComponentIssuesUrl(this.props.component.key, query);
  };

  getUrlForCodeSmells(sinceLeakPeriod: boolean) {
    return this.getIssuesUrl(sinceLeakPeriod, { types: 'CODE_SMELL' });
  }

  getUrlForBugsOrVulnerabilities(type: string, sinceLeakPeriod: boolean) {
    const RATING_TO_SEVERITIES_MAPPING = [
      'BLOCKER,CRITICAL,MAJOR,MINOR',
      'BLOCKER,CRITICAL,MAJOR',
      'BLOCKER,CRITICAL',
      'BLOCKER'
    ];

    const { condition } = this.props;
    const threshold = condition.level === 'ERROR' ? condition.error : condition.warning;

    return this.getIssuesUrl(sinceLeakPeriod, {
      types: type,
      severities: RATING_TO_SEVERITIES_MAPPING[Number(threshold) - 1]
    });
  }

  getUrlForType(type: string, sinceLeakPeriod: boolean) {
    return type === 'CODE_SMELL'
      ? this.getUrlForCodeSmells(sinceLeakPeriod)
      : this.getUrlForBugsOrVulnerabilities(type, sinceLeakPeriod);
  }

  wrapWithLink(children: React.ReactNode) {
    const { branchLike, component, condition } = this.props;

    const className = classNames(
      'overview-quality-gate-condition',
      `overview-quality-gate-condition-${condition.level.toLowerCase()}`
    );

    const metricKey = condition.measure.metric.key;

    const RATING_METRICS_MAPPING: T.Dict<[string, boolean]> = {
      reliability_rating: ['BUG', false],
      new_reliability_rating: ['BUG', true],
      security_rating: ['VULNERABILITY', false],
      new_security_rating: ['VULNERABILITY', true],
      sqale_rating: ['CODE_SMELL', false],
      new_maintainability_rating: ['CODE_SMELL', true]
    };

    return RATING_METRICS_MAPPING[metricKey] ? (
      <Link className={className} to={this.getUrlForType(...RATING_METRICS_MAPPING[metricKey])}>
        {children}
      </Link>
    ) : (
      <DrilldownLink
        branchLike={branchLike}
        className={className}
        component={component.key}
        metric={condition.measure.metric.key}
        sinceLeakPeriod={condition.period != null}>
        {children}
      </DrilldownLink>
    );
  }

  render() {
    const { condition } = this.props;
    const { measure } = condition;
    const { metric } = measure;

    const isDiff = isDiffMetric(metric.key);

    const threshold = (condition.level === 'ERROR' ? condition.error : condition.warning) as string;
    const actual = (condition.period ? measure.period?.value : measure.value) as string;

    let operator = translate('quality_gates.operator', condition.op);

    if (metric.type === 'RATING') {
      operator = translate('quality_gates.operator', condition.op, 'rating');
    }

    return this.wrapWithLink(
      <div className="overview-quality-gate-condition-container display-flex-center">
        <div className="overview-quality-gate-condition-value text-center spacer-right">
          <Measure
            decimals={2}
            metricKey={measure.metric.key}
            metricType={measure.metric.type}
            value={actual}
          />
        </div>

        <div>
          <span className="overview-quality-gate-condition-metric little-spacer-right">
            <IssueTypeIcon className="little-spacer-right" query={metric.key} />
            {metric.name}
          </span>
          {!isDiff && condition.period != null && (
            <span className="overview-quality-gate-condition-period text-ellipsis little-spacer-right">
              {translate('quality_gates.conditions.new_code')}
            </span>
          )}
          <span className="little-spacer-top small text-muted">
            {operator} {formatMeasure(threshold, metric.type)}
          </span>
        </div>
      </div>
    );
  }
}
