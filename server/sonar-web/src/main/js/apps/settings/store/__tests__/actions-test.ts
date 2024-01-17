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
import {
  getSettingsAppChangedValue,
  getSettingsAppDefinition
} from '../../../../store/rootReducer';
import { checkValue, fetchSettings } from '../actions';
import { receiveDefinitions } from '../definitions';

jest.mock('../../../../api/settings', () => ({
  getDefinitions: jest.fn().mockResolvedValue([
    {
      key: 'SETTINGS_1_KEY',
      type: 'SETTINGS_1_TYPE'
    },
    {
      key: 'SETTINGS_2_KEY',
      type: 'LICENSE'
    }
  ])
}));

jest.mock('../definitions', () => ({
  receiveDefinitions: jest.fn()
}));

jest.mock('../../../../store/rootReducer', () => ({
  getSettingsAppDefinition: jest.fn(),
  getSettingsAppChangedValue: jest.fn()
}));

it('#fetchSettings should filter LICENSE type settings', async () => {
  const dispatch = jest.fn();

  await fetchSettings()(dispatch);

  expect(receiveDefinitions).toHaveBeenCalledWith([
    {
      key: 'SETTINGS_1_KEY',
      type: 'SETTINGS_1_TYPE'
    }
  ]);
});

describe('checkValue', () => {
  const dispatch = jest.fn();

  beforeEach(() => {
    jest.clearAllMocks();
  });

  it('should correctly identify empty strings', () => {
    (getSettingsAppDefinition as jest.Mock).mockReturnValue({
      defaultValue: 'hello',
      type: 'TEXT'
    });

    (getSettingsAppChangedValue as jest.Mock).mockReturnValue(undefined);
    const key = 'key';
    expect(checkValue(key)(dispatch, jest.fn())).toBe(false);
    expect(dispatch).toBeCalledWith({
      type: 'settingsPage/FAIL_VALIDATION',
      key,
      message: 'settings.state.value_cant_be_empty'
    });
  });

  it('should correctly identify empty with no default', () => {
    (getSettingsAppDefinition as jest.Mock).mockReturnValue({
      type: 'TEXT'
    });

    (getSettingsAppChangedValue as jest.Mock).mockReturnValue(undefined);

    const key = 'key';
    expect(checkValue(key)(dispatch, jest.fn())).toBe(false);
    expect(dispatch).toBeCalledWith({
      type: 'settingsPage/FAIL_VALIDATION',
      key,
      message: 'settings.state.value_cant_be_empty_no_default'
    });
  });

  it('should correctly identify non-empty strings', () => {
    (getSettingsAppDefinition as jest.Mock).mockReturnValue({
      type: 'TEXT'
    });

    (getSettingsAppChangedValue as jest.Mock).mockReturnValue('not empty');
    const key = 'key';
    expect(checkValue(key)(dispatch, jest.fn())).toBe(true);
    expect(dispatch).toBeCalledWith({
      type: 'settingsPage/PASS_VALIDATION',
      key
    });
  });

  it('should correctly identify misformed JSON', () => {
    (getSettingsAppDefinition as jest.Mock).mockReturnValue({
      type: 'JSON'
    });

    (getSettingsAppChangedValue as jest.Mock).mockReturnValue('{JSON: "asd;{');
    const key = 'key';
    expect(checkValue(key)(dispatch, jest.fn())).toBe(false);
    expect(dispatch).toBeCalledWith({
      type: 'settingsPage/FAIL_VALIDATION',
      key,
      message: 'Unexpected token J in JSON at position 1'
    });
  });

  it('should correctly identify correct JSON', () => {
    (getSettingsAppDefinition as jest.Mock).mockReturnValue({
      type: 'JSON'
    });

    (getSettingsAppChangedValue as jest.Mock).mockReturnValue(
      '{"number": 42, "question": "answer"}'
    );
    const key = 'key';
    expect(checkValue(key)(dispatch, jest.fn())).toBe(true);
    expect(dispatch).toBeCalledWith({
      type: 'settingsPage/PASS_VALIDATION',
      key
    });
  });
});
