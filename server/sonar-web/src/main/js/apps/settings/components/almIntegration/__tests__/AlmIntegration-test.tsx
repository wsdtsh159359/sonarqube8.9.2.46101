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
import {
  countBindedProjects,
  deleteConfiguration,
  getAlmDefinitions,
  validateAlmSettings
} from '../../../../../api/alm-settings';
import { mockLocation } from '../../../../../helpers/testMocks';
import { AlmKeys, AlmSettingsBindingStatusType } from '../../../../../types/alm-settings';
import { AlmIntegration } from '../AlmIntegration';

jest.mock('../../../../../api/alm-settings', () => ({
  countBindedProjects: jest.fn().mockResolvedValue(0),
  deleteConfiguration: jest.fn().mockResolvedValue(undefined),
  getAlmDefinitions: jest
    .fn()
    .mockResolvedValue({ azure: [], bitbucket: [], bitbucketcloud: [], github: [], gitlab: [] }),
  validateAlmSettings: jest.fn().mockResolvedValue('')
}));

beforeEach(() => {
  jest.clearAllMocks();
});

it('should render correctly', () => {
  expect(shallowRender()).toMatchSnapshot();
});

it('should validate existing configurations', async () => {
  (getAlmDefinitions as jest.Mock).mockResolvedValueOnce({
    [AlmKeys.Azure]: [{ key: 'a1' }],
    [AlmKeys.BitbucketServer]: [{ key: 'b1' }],
    [AlmKeys.BitbucketCloud]: [{ key: 'bc1' }],
    [AlmKeys.GitHub]: [{ key: 'gh1' }, { key: 'gh2' }],
    [AlmKeys.GitLab]: [{ key: 'gl1' }]
  });

  const wrapper = shallowRender();

  await waitAndUpdate(wrapper);

  expect(validateAlmSettings).toBeCalledTimes(6);
  expect(validateAlmSettings).toBeCalledWith('a1');
  expect(validateAlmSettings).toBeCalledWith('b1');
  expect(validateAlmSettings).toBeCalledWith('bc1');
  expect(validateAlmSettings).toBeCalledWith('gh1');
  expect(validateAlmSettings).toBeCalledWith('gh2');
  expect(validateAlmSettings).toBeCalledWith('gl1');
});

it('should handle alm selection', async () => {
  const wrapper = shallowRender();

  wrapper.setState({ currentAlm: AlmKeys.Azure });

  wrapper.instance().handleSelectAlm(AlmKeys.GitHub);

  await waitAndUpdate(wrapper);

  expect(wrapper.state().currentAlm).toBe(AlmKeys.GitHub);
});

it('should handle delete', async () => {
  const toBeDeleted = '45672';
  (countBindedProjects as jest.Mock).mockResolvedValueOnce(7);
  const wrapper = shallowRender();

  wrapper.instance().handleDelete(toBeDeleted);
  await waitAndUpdate(wrapper);

  expect(wrapper.state().projectCount).toBe(7);
  expect(wrapper.state().definitionKeyForDeletion).toBe(toBeDeleted);
});

it('should delete configuration', async () => {
  (deleteConfiguration as jest.Mock).mockResolvedValueOnce(undefined);
  const wrapper = shallowRender();
  wrapper.instance().deleteConfiguration('8345678');

  await waitAndUpdate(wrapper);
  expect(wrapper.state().projectCount).toBeUndefined();
  expect(wrapper.state().definitionKeyForDeletion).toBeUndefined();
});

it('should validate a configuration', async () => {
  const definitionKey = 'validated-key';
  const failureMessage = 'an error occured';

  const wrapper = shallowRender();
  await waitAndUpdate(wrapper);

  (validateAlmSettings as jest.Mock)
    .mockRejectedValueOnce(undefined)
    .mockResolvedValueOnce(failureMessage)
    .mockResolvedValueOnce('')
    .mockResolvedValueOnce('');

  await wrapper.instance().handleCheck(definitionKey);

  expect(wrapper.state().definitionStatus[definitionKey]).toEqual({
    alertSuccess: true,
    failureMessage: '',
    type: AlmSettingsBindingStatusType.Warning
  });

  await wrapper.instance().handleCheck(definitionKey);

  expect(wrapper.state().definitionStatus[definitionKey]).toEqual({
    alertSuccess: true,
    failureMessage,
    type: AlmSettingsBindingStatusType.Failure
  });

  await wrapper.instance().handleCheck(definitionKey);

  expect(wrapper.state().definitionStatus[definitionKey]).toEqual({
    alertSuccess: true,
    failureMessage: '',
    type: AlmSettingsBindingStatusType.Success
  });
});

it('should fetch settings', async () => {
  const definitions = {
    [AlmKeys.Azure]: [{ key: 'a1' }],
    [AlmKeys.BitbucketServer]: [{ key: 'b1' }],
    [AlmKeys.BitbucketCloud]: [{ key: 'bc1' }],
    [AlmKeys.GitHub]: [{ key: 'gh1' }],
    [AlmKeys.GitLab]: [{ key: 'gl1' }]
  };

  const wrapper = shallowRender();
  await waitAndUpdate(wrapper);

  (getAlmDefinitions as jest.Mock).mockResolvedValueOnce(definitions);

  await wrapper.instance().fetchPullRequestDecorationSetting();

  expect(getAlmDefinitions).toBeCalled();
  expect(wrapper.state().definitions).toEqual(definitions);
  expect(wrapper.state().loadingAlmDefinitions).toBe(false);
});

function shallowRender() {
  return shallow<AlmIntegration>(
    <AlmIntegration appState={{ branchesEnabled: true }} location={mockLocation()} />
  );
}
