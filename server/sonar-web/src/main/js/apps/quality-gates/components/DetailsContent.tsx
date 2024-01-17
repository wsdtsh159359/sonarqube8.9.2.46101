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
import HelpTooltip from 'sonar-ui-common/components/controls/HelpTooltip';
import { translate } from 'sonar-ui-common/helpers/l10n';
import Conditions from './Conditions';
import Projects from './Projects';

export interface DetailsContentProps {
  isDefault?: boolean;
  metrics: T.Dict<T.Metric>;
  onAddCondition: (condition: T.Condition) => void;
  onRemoveCondition: (Condition: T.Condition) => void;
  onSaveCondition: (newCondition: T.Condition, oldCondition: T.Condition) => void;
  qualityGate: T.QualityGate;
  updatedConditionId?: number;
}

export function DetailsContent(props: DetailsContentProps) {
  const { isDefault, metrics, qualityGate, updatedConditionId } = props;
  const conditions = qualityGate.conditions || [];
  const actions = qualityGate.actions || ({} as any);

  return (
    <div className="layout-page-main-inner">
      <Conditions
        canEdit={actions.manageConditions}
        conditions={conditions}
        metrics={metrics}
        onAddCondition={props.onAddCondition}
        onRemoveCondition={props.onRemoveCondition}
        onSaveCondition={props.onSaveCondition}
        qualityGate={qualityGate}
        updatedConditionId={updatedConditionId}
      />

      <div className="quality-gate-section" id="quality-gate-projects">
        <header className="display-flex-center spacer-bottom">
          <h3>{translate('quality_gates.projects')}</h3>
          <HelpTooltip
            className="spacer-left"
            overlay={
              <div className="big-padded-top big-padded-bottom">
                {translate('quality_gates.projects.help')}
              </div>
            }
          />
        </header>
        {isDefault ? (
          translate('quality_gates.projects_for_default')
        ) : (
          <Projects
            canEdit={actions.associateProjects}
            // pass unique key to re-mount the component when the quality gate changes
            key={qualityGate.id}
            qualityGate={qualityGate}
          />
        )}
      </div>
    </div>
  );
}

export default React.memo(DetailsContent);
