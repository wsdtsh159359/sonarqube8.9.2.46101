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
import { changePassword } from '../../../api/users';
import { mockLocation } from '../../../helpers/testMocks';
import { getAppState, Store } from '../../../store/rootReducer';
import { ChangeAdminPasswordApp, mapStateToProps } from '../ChangeAdminPasswordApp';
import { DEFAULT_ADMIN_LOGIN, DEFAULT_ADMIN_PASSWORD } from '../constants';

jest.mock('react-redux', () => ({
  connect: jest.fn(() => (a: any) => a)
}));

jest.mock('../../../store/rootReducer', () => ({
  ...jest.requireActual('../../../store/rootReducer'),
  getAppState: jest.fn()
}));

jest.mock('../../../api/users', () => ({
  changePassword: jest.fn().mockResolvedValue(null)
}));

beforeEach(jest.clearAllMocks);

it('should render correctly', () => {
  expect(shallowRender()).toMatchSnapshot('default');
  expect(shallowRender({ instanceUsesDefaultAdminCredentials: undefined })).toMatchSnapshot(
    'admin is not using the default password'
  );
});

it('should correctly handle input changes', () => {
  const wrapper = shallowRender();

  // Set different values; should not allow submission.
  wrapper.instance().handlePasswordChange('new pass');
  wrapper.instance().handleConfirmPasswordChange('confirm pass');

  expect(wrapper.state().passwordValue).toBe('new pass');
  expect(wrapper.state().confirmPasswordValue).toBe('confirm pass');
  expect(wrapper.state().canSubmit).toBe(false);

  // Set the same values; should allow submission.
  wrapper.instance().handleConfirmPasswordChange('new pass');
  expect(wrapper.state().canSubmit).toBe(true);

  // Set the default admin password; should not allow submission.
  wrapper.instance().handlePasswordChange(DEFAULT_ADMIN_PASSWORD);
  expect(wrapper.state().canSubmit).toBe(false);
});

it('should correctly update the password', async () => {
  (changePassword as jest.Mock).mockResolvedValueOnce(null).mockRejectedValueOnce(null);
  const wrapper = shallowRender();
  wrapper.setState({ canSubmit: false, passwordValue: 'new pass' });

  wrapper.instance().handleSubmit();
  expect(wrapper.state().submitting).toBe(false);
  expect(changePassword).not.toBeCalled();

  wrapper.setState({ canSubmit: true });
  wrapper.instance().handleSubmit();
  expect(wrapper.state().submitting).toBe(true);

  await waitAndUpdate(wrapper);

  expect(wrapper.state().submitting).toBe(false);
  expect(wrapper.state().success).toBe(true);
  expect(changePassword).toBeCalledWith({
    login: DEFAULT_ADMIN_LOGIN,
    password: 'new pass'
  });

  wrapper.instance().handleSubmit();
  expect(wrapper.state().submitting).toBe(true);

  await waitAndUpdate(wrapper);

  expect(wrapper.state().submitting).toBe(false);
  expect(wrapper.state().success).toBe(false);
});

describe('redux', () => {
  it('should correctly map state props', () => {
    (getAppState as jest.Mock)
      .mockReturnValueOnce({})
      .mockReturnValueOnce({ canAdmin: false, instanceUsesDefaultAdminCredentials: true });

    expect(mapStateToProps({} as Store)).toEqual({
      canAdmin: undefined,
      instanceUsesDefaultAdminCredentials: undefined
    });

    expect(mapStateToProps({} as Store)).toEqual({
      canAdmin: false,
      instanceUsesDefaultAdminCredentials: true
    });
  });
});

function shallowRender(props: Partial<ChangeAdminPasswordApp['props']> = {}) {
  return shallow<ChangeAdminPasswordApp>(
    <ChangeAdminPasswordApp
      canAdmin={true}
      instanceUsesDefaultAdminCredentials={true}
      location={mockLocation()}
      {...props}
    />
  );
}
