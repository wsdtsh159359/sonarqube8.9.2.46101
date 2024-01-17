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
import { translate } from 'sonar-ui-common/helpers/l10n';
import { getBaseUrl } from 'sonar-ui-common/helpers/urls';
import { withAppState } from '../../hoc/withAppState';
import SentenceWithHighlights from './SentenceWithHighlights';

export interface AllSetProps {
  appState: T.AppState;
}

export function AllSet(props: AllSetProps) {
  const {
    appState: { branchesEnabled }
  } = props;

  return (
    <div className="abs-width-600">
      <p className="big-spacer-bottom">
        <SentenceWithHighlights
          highlightKeys={['all_set']}
          translationKey="onboarding.tutorial.ci_outro.all_set"
        />
      </p>
      <div className="display-flex-row big-spacer-bottom">
        <div>
          <img
            alt="" // Should be ignored by screen readers
            className="big-spacer-right"
            width={30}
            src={`${getBaseUrl()}/images/tutorials/commit.svg`}
          />
        </div>
        <div>
          <p className="little-spacer-bottom">
            <strong>{translate('onboarding.tutorial.ci_outro.commit')}</strong>
          </p>
          <p>
            {branchesEnabled
              ? translate('onboarding.tutorial.ci_outro.commit.why')
              : translate('onboarding.tutorial.ci_outro.commit.why.no_branches')}
          </p>
        </div>
      </div>
      <div className="display-flex-row huge-spacer-bottom">
        <div>
          <img
            alt="" // Should be ignored by screen readers
            className="big-spacer-right"
            width={30}
            src={`${getBaseUrl()}/images/tutorials/refresh.svg`}
          />
        </div>
        <div>
          <p className="little-spacer-bottom">
            <strong>{translate('onboarding.tutorial.ci_outro.refresh')}</strong>
          </p>
          <p>{translate('onboarding.tutorial.ci_outro.refresh.why')}</p>
        </div>
      </div>
    </div>
  );
}

export default withAppState(AllSet);
