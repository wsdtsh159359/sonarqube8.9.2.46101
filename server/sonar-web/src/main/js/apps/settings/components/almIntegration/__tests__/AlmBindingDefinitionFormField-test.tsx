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
import { shallow } from 'enzyme';
import * as React from 'react';
import { ButtonLink } from 'sonar-ui-common/components/controls/buttons';
import { click } from 'sonar-ui-common/helpers/testUtils';
import { AlmBindingDefinition } from '../../../../../types/alm-settings';
import {
  AlmBindingDefinitionFormField,
  AlmBindingDefinitionFormFieldProps
} from '../AlmBindingDefinitionFormField';

it('should render correctly', () => {
  expect(shallowRender()).toMatchSnapshot('default');
  expect(shallowRender({ help: 'help' })).toMatchSnapshot('with help');
  expect(shallowRender({ isTextArea: true })).toMatchSnapshot('textarea');
  expect(shallowRender({ optional: true })).toMatchSnapshot('optional');
  expect(shallowRender({ overwriteOnly: true })).toMatchSnapshot('secret');
});

it('should call onFieldChange', () => {
  const onInputChange = jest.fn();
  shallowRender({ onFieldChange: onInputChange })
    .find('input')
    .simulate('change', { currentTarget: { value: '' } });
  expect(onInputChange).toBeCalled();

  const onTextAreaChange = jest.fn();
  shallowRender({ isTextArea: true, onFieldChange: onTextAreaChange })
    .find('textarea')
    .simulate('change', { currentTarget: { value: '' } });
  expect(onTextAreaChange).toBeCalled();
});

it('should correctly toggle visibility for secret fields', () => {
  const onFieldChange = jest.fn();
  const wrapper = shallowRender({ onFieldChange, overwriteOnly: true });
  expect(wrapper.find('input').exists()).toBe(false);

  click(wrapper.find(ButtonLink));
  expect(onFieldChange).toHaveBeenCalledWith('key', '');
  expect(wrapper.find('input').exists()).toBe(true);
});

function shallowRender(
  props: Partial<AlmBindingDefinitionFormFieldProps<AlmBindingDefinition>> = {}
) {
  return shallow(
    <AlmBindingDefinitionFormField
      id="key"
      isTextArea={false}
      maxLength={40}
      onFieldChange={jest.fn()}
      propKey="key"
      value="key"
      {...props}
    />
  );
}
