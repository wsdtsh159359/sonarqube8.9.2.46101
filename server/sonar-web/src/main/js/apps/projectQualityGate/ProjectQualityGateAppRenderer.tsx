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
import { Helmet } from 'react-helmet-async';
import { SubmitButton } from 'sonar-ui-common/components/controls/buttons';
import HelpTooltip from 'sonar-ui-common/components/controls/HelpTooltip';
import Radio from 'sonar-ui-common/components/controls/Radio';
import Select from 'sonar-ui-common/components/controls/Select';
import { Alert } from 'sonar-ui-common/components/ui/Alert';
import { translate } from 'sonar-ui-common/helpers/l10n';
import A11ySkipTarget from '../../app/components/a11y/A11ySkipTarget';
import Suggestions from '../../app/components/embed-docs-modal/Suggestions';
import BuiltInQualityGateBadge from '../quality-gates/components/BuiltInQualityGateBadge';
import { USE_SYSTEM_DEFAULT } from './constants';

export interface ProjectQualityGateAppRendererProps {
  allQualityGates?: T.QualityGate[];
  currentQualityGate?: T.QualityGate;
  loading: boolean;
  onSelect: (id: string) => void;
  onSubmit: () => void;
  selectedQualityGateId: string;
  submitting: boolean;
}

export default function ProjectQualityGateAppRenderer(props: ProjectQualityGateAppRendererProps) {
  const { allQualityGates, currentQualityGate, loading, selectedQualityGateId, submitting } = props;
  const defaultQualityGate = allQualityGates?.find(g => g.isDefault);

  if (loading) {
    return <i className="spinner" />;
  }

  if (
    allQualityGates === undefined ||
    defaultQualityGate === undefined ||
    currentQualityGate === undefined
  ) {
    return null;
  }

  const usesDefault = selectedQualityGateId === USE_SYSTEM_DEFAULT;
  const needsReanalysis = usesDefault
    ? // currentQualityGate.isDefault is not always up to date. We need to check
      // against defaultQualityGate explicitly.
      defaultQualityGate.id !== currentQualityGate.id
    : selectedQualityGateId !== currentQualityGate.id;

  const options = allQualityGates.map(g => ({
    label: g.name,
    value: g.id
  }));

  return (
    <div className="page page-limited" id="project-quality-gate">
      <Suggestions suggestions="project_quality_gate" />
      <Helmet defer={false} title={translate('project_quality_gate.page')} />
      <A11ySkipTarget anchor="qg_main" />

      <header className="page-header">
        <div className="page-title display-flex-center">
          <h1>{translate('project_quality_gate.page')}</h1>
          <HelpTooltip
            className="spacer-left"
            overlay={
              <div className="big-padded-top big-padded-bottom">
                {translate('quality_gates.projects.help')}
              </div>
            }
          />
        </div>
      </header>

      <div className="boxed-group">
        <h2 className="boxed-group-header">{translate('project_quality_gate.subtitle')}</h2>

        <form
          className="boxed-group-inner"
          onSubmit={e => {
            e.preventDefault();
            props.onSubmit();
          }}>
          <p className="big-spacer-bottom">{translate('project_quality_gate.page.description')}</p>

          <div className="big-spacer-bottom">
            <Radio
              className="display-flex-start"
              checked={usesDefault}
              disabled={submitting}
              onCheck={() => props.onSelect(USE_SYSTEM_DEFAULT)}
              value={USE_SYSTEM_DEFAULT}>
              <div className="spacer-left">
                <div className="little-spacer-bottom">
                  {translate('project_quality_gate.always_use_default')}
                </div>
                <div className="display-flex-center">
                  <span className="text-muted little-spacer-right">
                    {translate('current_noun')}:
                  </span>
                  {defaultQualityGate.name}
                  {defaultQualityGate.isBuiltIn && (
                    <BuiltInQualityGateBadge className="spacer-left" />
                  )}
                </div>
              </div>
            </Radio>
          </div>

          <div className="big-spacer-bottom">
            <Radio
              className="display-flex-start"
              checked={!usesDefault}
              disabled={submitting}
              onCheck={value => props.onSelect(value)}
              value={!usesDefault ? selectedQualityGateId : currentQualityGate.id}>
              <div className="spacer-left">
                <div className="little-spacer-bottom">
                  {translate('project_quality_gate.always_use_specific')}
                </div>
                <div className="display-flex-center">
                  <Select
                    className="abs-width-300"
                    clearable={false}
                    disabled={submitting || usesDefault}
                    onChange={({ value }: { value: string }) => props.onSelect(value)}
                    options={options}
                    optionRenderer={option => <span>{option.label}</span>}
                    value={selectedQualityGateId}
                  />
                </div>
              </div>
            </Radio>

            {needsReanalysis && (
              <Alert className="big-spacer-top" variant="warning">
                {translate('project_quality_gate.requires_new_analysis')}
              </Alert>
            )}
          </div>

          <div>
            <SubmitButton disabled={submitting}>{translate('save')}</SubmitButton>
            {submitting && <i className="spinner spacer-left" />}
          </div>
        </form>
      </div>
    </div>
  );
}
