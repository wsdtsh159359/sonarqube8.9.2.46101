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
import { mockComponent, mockLoggedInUser } from '../../../../helpers/testMocks';
import ManualTutorial from '../ManualTutorial';
import ProjectAnalysisStep from '../ProjectAnalysisStep';
import TokenStep from '../TokenStep';

it('renders correctly', () => {
  expect(shallowRender()).toMatchSnapshot('default');
});

it('allows to navigate between steps', () => {
  const wrapper = shallowRender();
  const instance = wrapper.instance();

  expect(wrapper.find(TokenStep).props().open).toBe(true);

  instance.handleTokenDone('foo');
  expect(wrapper.find(TokenStep).props().open).toBe(false);
  expect(wrapper.find(ProjectAnalysisStep).props().open).toBe(true);

  instance.handleTokenOpen();
  expect(wrapper.find(TokenStep).props().open).toBe(true);
  expect(wrapper.find(ProjectAnalysisStep).props().open).toBe(false);
});

function shallowRender(props: Partial<ManualTutorial['props']> = {}) {
  return shallow<ManualTutorial>(
    <ManualTutorial component={mockComponent()} currentUser={mockLoggedInUser()} {...props} />
  );
}
