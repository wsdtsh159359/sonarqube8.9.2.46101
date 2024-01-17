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
import { Alert } from 'sonar-ui-common/components/ui/Alert';
import { translate } from 'sonar-ui-common/helpers/l10n';
import { AlmKeys } from '../../../types/alm-settings';
import { ALM_INTEGRATION } from '../../settings/components/AdditionalCategoryKeys';

export interface WrongBindingCountAlertProps {
  alm: AlmKeys;
  canAdmin: boolean;
}

export default function WrongBindingCountAlert(props: WrongBindingCountAlertProps) {
  const { alm, canAdmin } = props;

  return (
    <Alert variant="error">
      {canAdmin ? (
        <FormattedMessage
          defaultMessage={translate('onboarding.create_project.wrong_binding_count.admin')}
          id="onboarding.create_project.wrong_binding_count.admin"
          values={{
            alm: translate('onboarding.alm', alm),
            url: (
              <Link
                to={{
                  pathname: '/admin/settings',
                  query: { category: ALM_INTEGRATION }
                }}>
                {translate('settings.page')}
              </Link>
            )
          }}
        />
      ) : (
        <FormattedMessage
          defaultMessage={translate('onboarding.create_project.wrong_binding_count')}
          id="onboarding.create_project.wrong_binding_count"
          values={{
            alm: translate('onboarding.alm', alm)
          }}
        />
      )}
    </Alert>
  );
}
