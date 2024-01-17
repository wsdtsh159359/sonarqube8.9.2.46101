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
import { isEqual, omit } from 'lodash';
import * as React from 'react';
import { AlmBindingDefinition } from '../../../../types/alm-settings';
import AlmBindingDefinitionFormModalRenderer from './AlmBindingDefinitionFormModalRenderer';

export interface AlmBindingDefinitionFormChildrenProps<B> {
  formData: B;
  onFieldChange: (fieldId: keyof B, value: string) => void;
}

interface Props<B> {
  bindingDefinition: B;
  children: (props: AlmBindingDefinitionFormChildrenProps<B>) => React.ReactNode;
  help?: React.ReactNode;
  isSecondInstance?: boolean;
  onCancel?: () => void;
  onDelete?: (definitionKey: string) => void;
  onEdit?: (definitionKey: string) => void;
  onSubmit: (data: B, originalKey: string) => void;
  optionalFields?: Array<keyof B>;
}

interface State<B> {
  formData: B;
  touched: boolean;
}

export default class AlmBindingDefinitionForm<
  B extends AlmBindingDefinition
> extends React.PureComponent<Props<B>, State<B>> {
  constructor(props: Props<B>) {
    super(props);
    this.state = { formData: props.bindingDefinition, touched: false };
  }

  componentDidUpdate(prevProps: Props<B>) {
    if (!isEqual(prevProps.bindingDefinition, this.props.bindingDefinition)) {
      this.setState({ formData: this.props.bindingDefinition, touched: false });
    }
  }

  handleCancel = () => {
    this.setState({ formData: this.props.bindingDefinition, touched: false });
    if (this.props.onCancel) {
      this.props.onCancel();
    }
  };

  handleDelete = () => {
    if (this.props.onDelete) {
      this.props.onDelete(this.props.bindingDefinition.key);
    }
  };

  handleEdit = () => {
    if (this.props.onEdit) {
      this.props.onEdit(this.props.bindingDefinition.key);
    }
  };

  handleFieldChange = (fieldId: keyof B, value: string) => {
    this.setState(({ formData }) => ({
      formData: {
        ...formData,
        [fieldId]: value
      },
      touched: true
    }));
  };

  handleFormSubmit = () => {
    this.props.onSubmit(this.state.formData, this.props.bindingDefinition.key);
  };

  canSubmit = () => {
    const { optionalFields } = this.props;
    const { formData, touched } = this.state;

    let values = { ...formData };

    if (optionalFields && optionalFields.length > 0) {
      values = omit(values, optionalFields) as B;
    }

    return touched && !Object.values(values).some(v => !v);
  };

  render() {
    const { bindingDefinition, children, help, isSecondInstance } = this.props;
    const { formData } = this.state;

    const action = bindingDefinition.key ? 'edit' : 'create';

    return (
      <AlmBindingDefinitionFormModalRenderer
        action={action}
        canSubmit={this.canSubmit}
        help={help}
        isSecondInstance={Boolean(isSecondInstance)}
        onCancel={this.handleCancel}
        onSubmit={this.handleFormSubmit}>
        {children({
          formData,
          onFieldChange: this.handleFieldChange
        })}
      </AlmBindingDefinitionFormModalRenderer>
    );
  }
}
