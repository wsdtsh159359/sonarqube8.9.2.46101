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
import { FormattedMessage } from 'react-intl';
import { SubmitButton } from 'sonar-ui-common/components/controls/buttons';
import ValidationInput from 'sonar-ui-common/components/controls/ValidationInput';
import DetachIcon from 'sonar-ui-common/components/icons/DetachIcon';
import DeferredSpinner from 'sonar-ui-common/components/ui/DeferredSpinner';
import { translate } from 'sonar-ui-common/helpers/l10n';
import { AlmSettingsInstance } from '../../../types/alm-settings';

export interface AzurePersonalAccessTokenFormProps {
  almSetting: AlmSettingsInstance;
  onPersonalAccessTokenCreate: (token: string) => void;
  submitting?: boolean;
  validationFailed: boolean;
}

function getAzurePatUrl(url: string) {
  return `${url.replace(/\/$/, '')}/_usersSettings/tokens`;
}

export default function AzurePersonalAccessTokenForm(props: AzurePersonalAccessTokenFormProps) {
  const {
    almSetting: { alm, url },
    submitting = false,
    validationFailed
  } = props;

  const [touched, setTouched] = React.useState(false);
  React.useEffect(() => {
    setTouched(false);
  }, [submitting]);

  const [token, setToken] = React.useState('');

  const isInvalid = (validationFailed && !touched) || (touched && !token);

  let errorMessage;
  if (!token) {
    errorMessage = translate('onboarding.create_project.pat_form.pat_required');
  } else if (isInvalid) {
    errorMessage = translate('onboarding.create_project.pat_incorrect', alm);
  }

  return (
    <div className="boxed-group abs-width-600">
      <div className="boxed-group-inner">
        <h2>{translate('onboarding.create_project.pat_form.title', alm)}</h2>

        <div className="big-spacer-top big-spacer-bottom">
          <FormattedMessage
            id="onboarding.create_project.pat_help.instructions"
            defaultMessage={translate('onboarding.create_project.pat_help.instructions', alm)}
            values={{
              link: url ? (
                <a
                  className="link-with-icon"
                  href={getAzurePatUrl(url)}
                  rel="noopener noreferrer"
                  target="_blank">
                  <DetachIcon className="little-spacer-right" />
                  <span>
                    {translate('onboarding.create_project.pat_help.instructions.link', alm)}
                  </span>
                </a>
              ) : (
                translate('onboarding.create_project.pat_help.instructions.link', alm)
              ),
              scope: (
                <strong>
                  <em>Code (Read & Write)</em>
                </strong>
              )
            }}
          />
        </div>

        <form
          onSubmit={(e: React.SyntheticEvent<HTMLFormElement>) => {
            e.preventDefault();
            props.onPersonalAccessTokenCreate(token);
          }}>
          <ValidationInput
            error={errorMessage}
            id="personal_access_token"
            isInvalid={isInvalid}
            isValid={false}
            label={translate('onboarding.create_project.enter_pat')}
            required={true}>
            <input
              autoFocus={true}
              className={classNames('width-100 little-spacer-bottom', {
                'is-invalid': isInvalid
              })}
              id="personal_access_token"
              minLength={1}
              name="personal_access_token"
              onChange={(e: React.ChangeEvent<HTMLInputElement>) => {
                setToken(e.target.value);
                setTouched(true);
              }}
              type="text"
              value={token}
            />
          </ValidationInput>

          <SubmitButton disabled={isInvalid || submitting || !touched}>
            {translate('onboarding.create_project.pat_form.list_repositories')}
          </SubmitButton>
          <DeferredSpinner className="spacer-left" loading={submitting} />
        </form>
      </div>
    </div>
  );
}
