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
import { click } from 'sonar-ui-common/helpers/testUtils';
import { AlmKeys } from '../../../../types/alm-settings';
import {
  CreateProjectModeSelection,
  CreateProjectModeSelectionProps
} from '../CreateProjectModeSelection';
import { CreateProjectModes } from '../types';

it('should render correctly', () => {
  expect(shallowRender()).toMatchSnapshot('default');
  expect(shallowRender({ loadingBindings: true })).toMatchSnapshot('loading instances');
  expect(shallowRender({}, { [AlmKeys.BitbucketServer]: 0, [AlmKeys.GitHub]: 2 })).toMatchSnapshot(
    'invalid configs, not admin'
  );
  expect(
    shallowRender(
      { appState: { canAdmin: true } },
      { [AlmKeys.BitbucketServer]: 0, [AlmKeys.GitHub]: 2 }
    )
  ).toMatchSnapshot('invalid configs, admin');
});

it('should correctly pass the selected mode up', () => {
  const onSelectMode = jest.fn();
  const wrapper = shallowRender({ onSelectMode });

  const almButton = 'button.create-project-mode-type-alm';

  click(wrapper.find('button.create-project-mode-type-manual'));
  expect(onSelectMode).toBeCalledWith(CreateProjectModes.Manual);
  onSelectMode.mockClear();

  click(wrapper.find(almButton).at(0));
  expect(onSelectMode).toBeCalledWith(CreateProjectModes.AzureDevOps);
  onSelectMode.mockClear();

  click(wrapper.find(almButton).at(1));
  expect(onSelectMode).toBeCalledWith(CreateProjectModes.BitbucketServer);
  onSelectMode.mockClear();

  click(wrapper.find(almButton).at(2));
  expect(onSelectMode).toBeCalledWith(CreateProjectModes.GitHub);
  onSelectMode.mockClear();

  click(wrapper.find(almButton).at(3));
  expect(onSelectMode).toBeCalledWith(CreateProjectModes.GitLab);
  onSelectMode.mockClear();
});

function shallowRender(
  props: Partial<CreateProjectModeSelectionProps> = {},
  almCountOverrides = {}
) {
  const almCounts = {
    [AlmKeys.Azure]: 0,
    [AlmKeys.BitbucketServer]: 1,
    [AlmKeys.GitHub]: 0,
    [AlmKeys.GitLab]: 0,
    ...almCountOverrides
  };
  return shallow<CreateProjectModeSelectionProps>(
    <CreateProjectModeSelection
      almCounts={almCounts}
      appState={{ canAdmin: false }}
      loadingBindings={false}
      onSelectMode={jest.fn()}
      {...props}
    />
  );
}
