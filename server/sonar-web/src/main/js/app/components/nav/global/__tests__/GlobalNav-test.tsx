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
import { waitAndUpdate } from 'sonar-ui-common/helpers/testUtils';
import { GlobalNav, GlobalNavProps } from '../GlobalNav';

// Solve redux warning issue "No reducer provided for key":
// https://stackoverflow.com/questions/43375079/redux-warning-only-appearing-in-tests
jest.mock('../../../../../store/rootReducer');

const appState: GlobalNavProps['appState'] = {
  globalPages: [],
  canAdmin: false,
  qualifiers: []
};
const location = { pathname: '' };

it('should render correctly', async () => {
  const wrapper = shallowRender();

  expect(wrapper).toMatchSnapshot('anonymous users');
  wrapper.setProps({ currentUser: { isLoggedIn: true } });
  expect(wrapper).toMatchSnapshot('logged in users');

  await waitAndUpdate(wrapper);
});

function shallowRender(props: Partial<GlobalNavProps> = {}) {
  return shallow(
    <GlobalNav
      appState={appState}
      currentUser={{ isLoggedIn: false }}
      location={location}
      {...props}
    />
  );
}
