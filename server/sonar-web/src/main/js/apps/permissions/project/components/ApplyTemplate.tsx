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
import { ResetButtonLink, SubmitButton } from 'sonar-ui-common/components/controls/buttons';
import Select from 'sonar-ui-common/components/controls/Select';
import SimpleModal from 'sonar-ui-common/components/controls/SimpleModal';
import { Alert } from 'sonar-ui-common/components/ui/Alert';
import DeferredSpinner from 'sonar-ui-common/components/ui/DeferredSpinner';
import MandatoryFieldMarker from 'sonar-ui-common/components/ui/MandatoryFieldMarker';
import MandatoryFieldsExplanation from 'sonar-ui-common/components/ui/MandatoryFieldsExplanation';
import { translate, translateWithParameters } from 'sonar-ui-common/helpers/l10n';
import { applyTemplateToProject, getPermissionTemplates } from '../../../../api/permissions';

interface Props {
  onApply?: () => void;
  onClose: () => void;
  project: { key: string; name: string };
}

interface State {
  done: boolean;
  loading: boolean;
  permissionTemplate?: string;
  permissionTemplates?: T.PermissionTemplate[];
}

export default class ApplyTemplate extends React.PureComponent<Props, State> {
  mounted = false;
  state: State = { done: false, loading: true };

  componentDidMount() {
    this.mounted = true;
    this.fetchPermissionTemplates();
  }

  componentWillUnmount() {
    this.mounted = false;
  }

  fetchPermissionTemplates = () => {
    getPermissionTemplates().then(
      ({ permissionTemplates }) => {
        if (this.mounted) {
          this.setState({ loading: false, permissionTemplates });
        }
      },
      () => {
        if (this.mounted) {
          this.setState({ loading: false });
        }
      }
    );
  };

  handleSubmit = () => {
    if (this.state.permissionTemplate) {
      return applyTemplateToProject({
        projectKey: this.props.project.key,
        templateId: this.state.permissionTemplate
      }).then(() => {
        if (this.mounted) {
          if (this.props.onApply) {
            this.props.onApply();
          }
          this.setState({ done: true });
        }
      });
    } else {
      return Promise.reject(undefined);
    }
  };

  handlePermissionTemplateChange = ({ value }: { value: string }) => {
    this.setState({ permissionTemplate: value });
  };

  render() {
    const header = translateWithParameters(
      'projects_role.apply_template_to_xxx',
      this.props.project.name
    );

    return (
      <SimpleModal
        header={header}
        onClose={this.props.onClose}
        onSubmit={this.handleSubmit}
        size="small">
        {({ onCloseClick, onFormSubmit, submitting }) => (
          <form id="project-permissions-apply-template-form" onSubmit={onFormSubmit}>
            <header className="modal-head">
              <h2>{header}</h2>
            </header>

            <div className="modal-body">
              {this.state.done && (
                <Alert variant="success">{translate('projects_role.apply_template.success')}</Alert>
              )}

              {this.state.loading && <i className="spinner" />}

              {!this.state.done && !this.state.loading && (
                <>
                  <MandatoryFieldsExplanation className="modal-field" />
                  <div className="modal-field">
                    <label htmlFor="project-permissions-template">
                      {translate('template')}
                      <MandatoryFieldMarker />
                    </label>
                    {this.state.permissionTemplates && (
                      <Select
                        clearable={false}
                        id="project-permissions-template"
                        onChange={this.handlePermissionTemplateChange}
                        options={this.state.permissionTemplates.map(permissionTemplate => ({
                          label: permissionTemplate.name,
                          value: permissionTemplate.id
                        }))}
                        value={this.state.permissionTemplate}
                      />
                    )}
                  </div>
                </>
              )}
            </div>

            <footer className="modal-foot">
              <DeferredSpinner className="spacer-right" loading={submitting} />
              {!this.state.done && (
                <SubmitButton disabled={submitting || !this.state.permissionTemplate}>
                  {translate('apply')}
                </SubmitButton>
              )}
              <ResetButtonLink onClick={onCloseClick}>
                {translate(this.state.done ? 'close' : 'cancel')}
              </ResetButtonLink>
            </footer>
          </form>
        )}
      </SimpleModal>
    );
  }
}
