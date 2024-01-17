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
import { Button } from 'sonar-ui-common/components/controls/buttons';
import { ClipboardIconButton } from 'sonar-ui-common/components/controls/clipboard';
import { translate } from 'sonar-ui-common/helpers/l10n';
import Step from '../components/Step';
import TokenStepGenerator from '../components/TokenStepGenerator';

export interface EnvironmentVariablesStepProps {
  baseUrl: string;
  component: T.Component;
  currentUser: T.LoggedInUser;
  finished: boolean;
  onDone: () => void;
  onOpen: () => void;
  open: boolean;
}

const pipelineDescriptionLinkLabel = translate(
  'onboarding.tutorial.with.gitlab_ci.env_variables.description.link'
);

export default function EnvironmentVariablesStep(props: EnvironmentVariablesStepProps) {
  const { baseUrl, component, currentUser, finished, open } = props;

  const fieldValueTranslation = translate('onboarding.tutorial.env_variables');

  const renderForm = () => (
    <div className="boxed-group-inner">
      <h2 className="spacer-bottom spacer-top">
        {translate('onboarding.tutorial.with.gitlab_ci.env_variables.section.title')}
      </h2>

      <FormattedMessage
        defaultMessage={translate(
          'onboarding.tutorial.with.gitlab_ci.env_variables.section.description'
        )}
        id="onboarding.tutorial.with.gitlab_ci.env_variables.section.description"
        values={{
          /* This link will be added when the backend provides the project URL */
          link: <strong>{pipelineDescriptionLinkLabel}</strong>
        }}
      />

      <ol className="list-styled big-spacer-top">
        <li className="big-spacer-bottom">
          <FormattedMessage
            defaultMessage={fieldValueTranslation}
            id="onboarding.tutorial.with.gitlab_ci.env_variables.step1"
            values={{
              extra: <ClipboardIconButton copyValue="SONAR_TOKEN" />,
              field: (
                <strong>
                  {translate('onboarding.tutorial.with.gitlab_ci.env_variables.step1')}
                </strong>
              ),
              value: <code className="rule">SONAR_TOKEN</code>
            }}
          />
        </li>
        <TokenStepGenerator component={component} currentUser={currentUser} />
        <li className="big-spacer-bottom">
          {translate('onboarding.tutorial.with.gitlab_ci.env_variables.step3')}
        </li>
        <li className="big-spacer-bottom">
          {translate('onboarding.tutorial.with.gitlab_ci.env_variables.section.step4')}
        </li>
      </ol>

      <hr className="no-horizontal-margins" />

      <h2 className="spacer-bottom big-spacer-top">
        {translate('onboarding.tutorial.with.gitlab_ci.env_variables.section2.title')}
      </h2>

      <FormattedMessage
        defaultMessage={translate(
          'onboarding.tutorial.with.gitlab_ci.env_variables.section2.description'
        )}
        id="onboarding.tutorial.with.gitlab_ci.env_variables.section2.description"
        values={{
          /* This link will be added when the backend provides the project URL */
          link: <strong>{pipelineDescriptionLinkLabel}</strong>
        }}
      />

      <ol className="list-styled big-spacer-top big-spacer-bottom">
        <li className="big-spacer-bottom">
          <FormattedMessage
            defaultMessage={fieldValueTranslation}
            id="onboarding.tutorial.with.gitlab_ci.env_variables.step1"
            values={{
              extra: <ClipboardIconButton copyValue="SONAR_HOST_URL" />,
              field: (
                <strong>
                  {translate('onboarding.tutorial.with.gitlab_ci.env_variables.step1')}
                </strong>
              ),
              value: <code className="rule">SONAR_HOST_URL</code>
            }}
          />
        </li>
        <li className="big-spacer-bottom">
          <FormattedMessage
            defaultMessage={fieldValueTranslation}
            id="onboarding.tutorial.with.gitlab_ci.env_variables.step2"
            values={{
              extra: <ClipboardIconButton copyValue={baseUrl} />,
              field: <strong>{translate('onboarding.tutorial.env_variables.field')}</strong>,
              value: <code className="rule">{baseUrl}</code>
            }}
          />
        </li>
        <li className="big-spacer-bottom">
          {translate('onboarding.tutorial.with.gitlab_ci.env_variables.step3')}
        </li>
        <li className="big-spacer-bottom">
          {translate('onboarding.tutorial.with.gitlab_ci.env_variables.section2.step4')}
        </li>
      </ol>

      <Button className="big-spacer-bottom" onClick={props.onDone}>
        {translate('continue')}
      </Button>
    </div>
  );

  return (
    <Step
      finished={finished}
      onOpen={props.onOpen}
      open={open}
      renderForm={renderForm}
      stepNumber={2}
      stepTitle={translate('onboarding.tutorial.with.gitlab_ci.env_variables.title')}
    />
  );
}
