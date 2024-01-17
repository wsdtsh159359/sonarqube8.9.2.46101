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
import { shallow, ShallowWrapper } from 'enzyme';
import * as React from 'react';
import { Button, SubmitButton } from 'sonar-ui-common/components/controls/buttons';
import { click } from 'sonar-ui-common/helpers/testUtils';
import ProjectKeyInput from '../../../components/common/ProjectKeyInput';
import { mockComponent, mockEvent } from '../../../helpers/testMocks';
import UpdateForm, { UpdateFormProps } from '../UpdateForm';

it('should render', () => {
  expect(shallowRender()).toMatchSnapshot('default');
  expect(getForm(shallowRender())).toMatchSnapshot('form');
});

it('should correctly update the form', () => {
  const component = mockComponent();
  const wrapper = shallowRender({ component });
  expectButtonDisabled(wrapper, Button).toBe(true);
  expectButtonDisabled(wrapper, SubmitButton).toBe(true);

  // Changing the key should unlock the form.
  changeInput(wrapper, 'bar');
  expectProjectKeyInputValue(wrapper).toBe('bar');
  expectButtonDisabled(wrapper, Button).toBe(false);
  expectButtonDisabled(wrapper, SubmitButton).toBe(false);

  // Changing it back again should lock the form.
  changeInput(wrapper, component.key);
  expectProjectKeyInputValue(wrapper).toBe(component.key);
  expectButtonDisabled(wrapper, Button).toBe(true);
  expectButtonDisabled(wrapper, SubmitButton).toBe(true);
});

it('should correctly reset the form', () => {
  const component = mockComponent();
  const wrapper = shallowRender({ component });
  changeInput(wrapper, 'bar');
  click(getForm(wrapper).find(Button));
  expectProjectKeyInputValue(wrapper).toBe(component.key);
});

function getForm(wrapper: ShallowWrapper) {
  // We're wrapper by a <ConfirmButton>. Dive twice to get the actual form.
  return wrapper.dive().dive();
}

function expectButtonDisabled(
  wrapper: ShallowWrapper,
  button: React.ComponentType<{ disabled?: boolean }>
) {
  return expect(
    getForm(wrapper)
      .find(button)
      .props().disabled
  );
}

function expectProjectKeyInputValue(wrapper: ShallowWrapper) {
  return expect(
    getForm(wrapper)
      .find(ProjectKeyInput)
      .props().projectKey
  );
}

function changeInput(wrapper: ShallowWrapper, value: string) {
  getForm(wrapper)
    .find(ProjectKeyInput)
    .props()
    .onProjectKeyChange(mockEvent({ currentTarget: { value } }));
}

function shallowRender(props: Partial<UpdateFormProps> = {}) {
  return shallow<UpdateFormProps>(
    <UpdateForm component={mockComponent()} onKeyChange={jest.fn()} {...props} />
  );
}
