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
import { Link } from 'react-router';
import HelpTooltip from 'sonar-ui-common/components/controls/HelpTooltip';
import DropdownIcon from 'sonar-ui-common/components/icons/DropdownIcon';
import PlusCircleIcon from 'sonar-ui-common/components/icons/PlusCircleIcon';
import { translate, translateWithParameters } from 'sonar-ui-common/helpers/l10n';
import DocumentationTooltip from '../../../../../components/common/DocumentationTooltip';
import BranchLikeIcon from '../../../../../components/icons/BranchLikeIcon';
import { getBranchLikeDisplayName } from '../../../../../helpers/branch-like';
import { getApplicationAdminUrl } from '../../../../../helpers/urls';
import { AlmKeys, ProjectAlmBindingResponse } from '../../../../../types/alm-settings';
import { BranchLike } from '../../../../../types/branch-like';
import { ComponentQualifier } from '../../../../../types/component';
import { colors } from '../../../../theme';

export interface CurrentBranchLikeProps {
  branchesEnabled: boolean;
  component: T.Component;
  currentBranchLike: BranchLike;
  hasManyBranches: boolean;
  projectBinding?: ProjectAlmBindingResponse;
}

export function CurrentBranchLike(props: CurrentBranchLikeProps) {
  const {
    branchesEnabled,
    component,
    component: { configuration },
    currentBranchLike,
    hasManyBranches,
    projectBinding
  } = props;

  const displayName = getBranchLikeDisplayName(currentBranchLike);
  const isApplication = component.qualifier === ComponentQualifier.Application;
  const canAdminComponent = configuration && configuration.showSettings;
  const isGitLab = projectBinding !== undefined && projectBinding.alm === AlmKeys.GitLab;

  const additionalIcon = () => {
    const plusIcon = <PlusCircleIcon fill={colors.blue} size={12} />;

    if (branchesEnabled && hasManyBranches) {
      return <DropdownIcon />;
    }

    if (isApplication) {
      if (!hasManyBranches && canAdminComponent) {
        return (
          <HelpTooltip
            overlay={
              <>
                <p>{translate('application.branches.help')}</p>
                <hr className="spacer-top spacer-bottom" />
                <Link to={getApplicationAdminUrl(component.key)}>
                  {translate('application.branches.link')}
                </Link>
              </>
            }>
            {plusIcon}
          </HelpTooltip>
        );
      }
    } else {
      if (!branchesEnabled) {
        return (
          <DocumentationTooltip
            content={
              projectBinding !== undefined
                ? translateWithParameters(
                    `branch_like_navigation.no_branch_support.content_x.${isGitLab ? 'mr' : 'pr'}`,
                    translate('alm', projectBinding.alm)
                  )
                : translate('branch_like_navigation.no_branch_support.content')
            }
            data-test="branches-support-disabled"
            links={[
              {
                href: 'https://redirect.sonarsource.com/editions/developer.html',
                label: translate('learn_more')
              }
            ]}
            title={
              projectBinding !== undefined
                ? translate(
                    'branch_like_navigation.no_branch_support.title',
                    isGitLab ? 'mr' : 'pr'
                  )
                : translate('branch_like_navigation.no_branch_support.title')
            }>
            {plusIcon}
          </DocumentationTooltip>
        );
      }

      if (!hasManyBranches) {
        return (
          <DocumentationTooltip
            content={translate('branch_like_navigation.only_one_branch.content')}
            data-test="only-one-branch-like"
            links={[
              {
                href: '/documentation/branches/overview/',
                label: translate('branch_like_navigation.only_one_branch.documentation')
              },
              {
                href: '/documentation/analysis/pull-request/',
                label: translate('branch_like_navigation.only_one_branch.pr_analysis')
              },
              {
                href: `/tutorials?id=${component.key}`,
                label: translate('branch_like_navigation.tutorial_for_ci'),
                inPlace: true
              }
            ]}
            title={translate('branch_like_navigation.only_one_branch.title')}>
            {plusIcon}
          </DocumentationTooltip>
        );
      }
    }

    return null;
  };

  return (
    <span className="display-flex-center flex-shrink text-ellipsis">
      <BranchLikeIcon branchLike={currentBranchLike} />
      <span
        className="spacer-left spacer-right flex-shrink text-ellipsis js-branch-like-name"
        title={displayName}>
        {displayName}
      </span>
      {additionalIcon()}
    </span>
  );
}

export default React.memo(CurrentBranchLike);
