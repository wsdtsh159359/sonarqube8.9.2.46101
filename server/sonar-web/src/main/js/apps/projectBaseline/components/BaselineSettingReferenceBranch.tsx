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
import RadioCard from 'sonar-ui-common/components/controls/RadioCard';
import SearchSelect from 'sonar-ui-common/components/controls/SearchSelect';
import Tooltip from 'sonar-ui-common/components/controls/Tooltip';
import AlertErrorIcon from 'sonar-ui-common/components/icons/AlertErrorIcon';
import MandatoryFieldMarker from 'sonar-ui-common/components/ui/MandatoryFieldMarker';
import MandatoryFieldsExplanation from 'sonar-ui-common/components/ui/MandatoryFieldsExplanation';
import { translate, translateWithParameters } from 'sonar-ui-common/helpers/l10n';

export interface BaselineSettingReferenceBranchProps {
  branchList: BranchOption[];
  className?: string;
  configuredBranchName?: string;
  disabled?: boolean;
  onChangeReferenceBranch: (value: string) => void;
  onSelect: (selection: T.NewCodePeriodSettingType) => void;
  referenceBranch: string;
  selected: boolean;
  settingLevel: 'project' | 'branch';
}

export interface BranchOption {
  disabled?: boolean;
  isInvalid?: boolean;
  isMain: boolean;
  value: string;
}

function renderBranchOption(option: BranchOption) {
  return option.isInvalid ? (
    <Tooltip
      overlay={translateWithParameters('baseline.reference_branch.does_not_exist', option.value)}>
      <span>
        {option.value} <AlertErrorIcon />
      </span>
    </Tooltip>
  ) : (
    <>
      <span
        title={
          option.disabled ? translate('baseline.reference_branch.cannot_be_itself') : undefined
        }>
        {option.value}
      </span>
      {option.isMain && (
        <div className="badge spacer-left">{translate('branches.main_branch')}</div>
      )}
    </>
  );
}

export default function BaselineSettingReferenceBranch(props: BaselineSettingReferenceBranchProps) {
  const { branchList, className, disabled, referenceBranch, selected, settingLevel } = props;

  const currentBranch = branchList.find(b => b.value === referenceBranch) || {
    value: referenceBranch,
    isMain: false,
    isInvalid: true
  };

  return (
    <RadioCard
      className={className}
      disabled={disabled}
      onClick={() => props.onSelect('REFERENCE_BRANCH')}
      selected={selected}
      title={translate('baseline.reference_branch')}>
      <>
        <p>{translate('baseline.reference_branch.description')}</p>
        {selected && (
          <>
            {settingLevel === 'project' && (
              <p className="spacer-top">{translate('baseline.reference_branch.description2')}</p>
            )}
            <div className="big-spacer-top display-flex-column">
              <MandatoryFieldsExplanation className="spacer-bottom" />
              <label className="text-middle" htmlFor="reference_branch">
                <strong>{translate('baseline.reference_branch.choose')}</strong>
                <MandatoryFieldMarker />
              </label>
              <SearchSelect<BranchOption>
                autofocus={false}
                className="little-spacer-top spacer-bottom"
                defaultOptions={branchList}
                minimumQueryLength={1}
                onSearch={q => Promise.resolve(branchList.filter(b => b.value.includes(q)))}
                onSelect={option => props.onChangeReferenceBranch(option.value)}
                renderOption={renderBranchOption}
                value={currentBranch}
              />
            </div>
          </>
        )}
      </>
    </RadioCard>
  );
}
