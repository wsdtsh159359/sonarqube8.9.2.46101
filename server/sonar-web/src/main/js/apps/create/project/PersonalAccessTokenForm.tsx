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
import { Alert } from 'sonar-ui-common/components/ui/Alert';
import DeferredSpinner from 'sonar-ui-common/components/ui/DeferredSpinner';
import { translate } from 'sonar-ui-common/helpers/l10n';
import { getBaseUrl } from 'sonar-ui-common/helpers/urls';
import { AlmKeys, AlmSettingsInstance } from '../../../types/alm-settings';

export interface PersonalAccessTokenFormProps {
  almSetting: AlmSettingsInstance;
  onPersonalAccessTokenCreate: (token: string) => void;
  submitting?: boolean;
  validationFailed: boolean;
  validationErrorMessage?: string;
}

function getPatUrl(alm: AlmKeys, url: string) {
  if (alm === AlmKeys.BitbucketServer) {
    return `${url.replace(/\/$/, '')}/plugins/servlet/access-tokens/add`;
  } else {
    // GitLab
    return url.endsWith('/api/v4')
      ? `${url.replace('/api/v4', '').replace(/\/$/, '')}/profile/personal_access_tokens`
      : 'https://docs.gitlab.com/ee/user/profile/personal_access_tokens.html#creating-a-personal-access-token';
  }
}

export default function PersonalAccessTokenForm(props: PersonalAccessTokenFormProps) {
  const {
    almSetting: { alm, url },
    submitting = false,
    validationFailed,
    validationErrorMessage
  } = props;
  const [touched, setTouched] = React.useState(false);

  React.useEffect(() => {
    setTouched(false);
  }, [submitting]);

  const isInvalid = validationFailed && !touched;
  const errorMessage =
    validationErrorMessage ?? translate('onboarding.create_project.pat_incorrect', alm);

  return (
    <div className="display-flex-start">
      <form
        className="width-50"
        onSubmit={(e: React.SyntheticEvent<HTMLFormElement>) => {
          e.preventDefault();
          const value = new FormData(e.currentTarget).get('personal_access_token') as string;
          props.onPersonalAccessTokenCreate(value);
        }}>
        <h2 className="big">{translate('onboarding.create_project.pat_form.title', alm)}</h2>
        <p className="big-spacer-top big-spacer-bottom">
          {translate('onboarding.create_project.pat_form.help', alm)}
        </p>

        <ValidationInput
          error={isInvalid ? errorMessage : undefined}
          id="personal_access_token"
          isInvalid={isInvalid}
          isValid={false}
          label={translate('onboarding.create_project.enter_pat')}
          required={true}>
          <input
            autoFocus={true}
            className={classNames('input-super-large', {
              'is-invalid': isInvalid
            })}
            id="personal_access_token"
            minLength={1}
            name="personal_access_token"
            onChange={() => {
              setTouched(true);
            }}
            type="text"
          />
        </ValidationInput>

        <SubmitButton disabled={isInvalid || submitting || !touched}>
          {translate('save')}
        </SubmitButton>
        <DeferredSpinner className="spacer-left" loading={submitting} />
      </form>

      <Alert className="big-spacer-left width-50" display="block" variant="info">
        <h3>{translate('onboarding.create_project.pat_help.title')}</h3>

        <p className="big-spacer-top big-spacer-bottom">
          <FormattedMessage
            id="onboarding.create_project.pat_help.instructions"
            defaultMessage={translate('onboarding.create_project.pat_help.instructions')}
            values={{ alm: translate('onboarding.alm', alm) }}
          />
        </p>

        {url && (
          <div className="text-middle">
            <img
              alt="" // Should be ignored by screen readers
              className="spacer-right"
              height="16"
              src={`${getBaseUrl()}/images/alm/${alm}.svg`}
            />
            <a href={getPatUrl(alm, url)} rel="noopener noreferrer" target="_blank">
              {translate('onboarding.create_project.pat_help.link')}
            </a>
          </div>
        )}

        <p className="big-spacer-top big-spacer-bottom">
          {translate('onboarding.create_project.pat_help.instructions2', alm)}
        </p>

        <ul>
          {alm === AlmKeys.BitbucketServer && (
            <>
              <li>
                <FormattedMessage
                  defaultMessage={translate(
                    'onboarding.create_project.pat_help.bbs_permission_projects'
                  )}
                  id="onboarding.create_project.pat_help.bbs_permission_projects"
                  values={{
                    perm: (
                      <strong>
                        {translate('onboarding.create_project.pat_help.read_permission')}
                      </strong>
                    )
                  }}
                />
              </li>
              <li>
                <FormattedMessage
                  defaultMessage={translate(
                    'onboarding.create_project.pat_help.bbs_permission_repos'
                  )}
                  id="onboarding.create_project.pat_help.bbs_permission_repos"
                  values={{
                    perm: (
                      <strong>
                        {translate('onboarding.create_project.pat_help.read_permission')}
                      </strong>
                    )
                  }}
                />
              </li>
            </>
          )}
          {alm === AlmKeys.GitLab && (
            <li className="spacer-bottom">
              <strong>
                {translate('onboarding.create_project.pat_help.gitlab.read_api_permission')}
              </strong>
            </li>
          )}
        </ul>
      </Alert>
    </div>
  );
}
