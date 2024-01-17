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
import Step from '../components/Step';
import { ManualTutorialConfig } from '../types';
import BuildToolForm from './BuildToolForm';
import AnalysisCommand from './commands/AnalysisCommand';

interface Props {
  component: T.Component;
  onFinish?: (projectKey?: string) => void;
  open: boolean;
  stepNumber: number;
  token?: string;
}

interface State {
  config?: ManualTutorialConfig;
}

export default class ProjectAnalysisStep extends React.PureComponent<Props, State> {
  state: State = {};

  handleBuildToolSelect = (config: ManualTutorialConfig) => {
    const { component } = this.props;
    this.setState({ config });
    if (this.props.onFinish) {
      this.props.onFinish(component.key);
    }
  };

  renderForm = () => {
    return (
      <div className="boxed-group-inner">
        <div className="display-flex-column">
          <BuildToolForm onDone={this.handleBuildToolSelect} />

          {this.state.config && (
            <div className="huge-spacer-top">
              <AnalysisCommand
                component={this.props.component}
                languageConfig={this.state.config}
                token={this.props.token}
              />
            </div>
          )}
        </div>
      </div>
    );
  };

  renderResult = () => null;

  render() {
    return (
      <Step
        finished={false}
        onOpen={() => {}}
        open={this.props.open}
        renderForm={this.renderForm}
        renderResult={this.renderResult}
        stepNumber={this.props.stepNumber}
        stepTitle={translate('onboarding.analysis.header')}
      />
    );
  }
}
