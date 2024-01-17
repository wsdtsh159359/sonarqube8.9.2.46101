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
import { Button } from 'sonar-ui-common/components/controls/buttons';
import { setSimpleSettingValue } from '../../../api/settings';
import { mockLoggedInUser, mockRouter } from '../../../helpers/testMocks';
import { PluginRiskConsent, PluginRiskConsentProps } from '../PluginRiskConsent';

jest.mock('../../../api/settings', () => ({
  setSimpleSettingValue: jest.fn().mockResolvedValue({})
}));

it('should render correctly', () => {
  expect(shallowRender()).toMatchSnapshot('default');
});

it('should redirect non-admin users', () => {
  const replace = jest.fn();
  const wrapper = shallowRender({
    currentUser: mockLoggedInUser(),
    router: mockRouter({ replace })
  });
  expect(wrapper.type()).toBeNull();
  expect(replace).toBeCalled();
});

it('should handle acknowledgement and redirect', async () => {
  const wrapper = shallowRender();

  wrapper
    .find(Button)
    .first()
    .simulate('click');

  await new Promise(setImmediate);

  expect(setSimpleSettingValue).toBeCalled();
});

function shallowRender(props: Partial<PluginRiskConsentProps> = {}) {
  return shallow(
    <PluginRiskConsent
      currentUser={mockLoggedInUser({ permissions: { global: ['admin'] } })}
      router={mockRouter()}
      {...props}
    />
  );
}
