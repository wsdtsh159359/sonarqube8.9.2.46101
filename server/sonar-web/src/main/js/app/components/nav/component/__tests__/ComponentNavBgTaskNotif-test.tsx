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
import { FormattedMessage } from 'react-intl';
import { Alert } from 'sonar-ui-common/components/ui/Alert';
import { hasMessage } from 'sonar-ui-common/helpers/l10n';
import { mockTask } from '../../../../../helpers/mocks/tasks';
import { mockComponent, mockLocation } from '../../../../../helpers/testMocks';
import { Task, TaskStatuses, TaskTypes } from '../../../../../types/tasks';
import { ComponentNavBgTaskNotif } from '../ComponentNavBgTaskNotif';

jest.mock('sonar-ui-common/helpers/l10n', () => ({
  ...jest.requireActual('sonar-ui-common/helpers/l10n'),
  hasMessage: jest.fn().mockReturnValue(true)
}));

const UNKNOWN_TASK_TYPE: TaskTypes = 'UNKOWN' as TaskTypes;

it('renders correctly', () => {
  expect(shallowRender()).toMatchSnapshot('default');
  expect(
    shallowRender({
      currentTask: mockTask({
        status: TaskStatuses.Failed,
        errorType: 'LICENSING',
        errorMessage: 'Foo'
      })
    })
  ).toMatchSnapshot('license issue');
  expect(shallowRender({ currentTask: undefined }).type()).toBeNull(); // No task.
});

it.each([
  // failed
  [
    'component_navigation.status.failed',
    'error',
    mockTask({ status: TaskStatuses.Failed, type: UNKNOWN_TASK_TYPE }),
    false,
    false,
    false,
    false
  ],
  [
    'component_navigation.status.failed_X',
    'error',
    mockTask({ status: TaskStatuses.Failed }),
    false,
    false,
    false,
    false
  ],
  [
    'component_navigation.status.failed.admin.link',
    'error',
    mockTask({ status: TaskStatuses.Failed, type: UNKNOWN_TASK_TYPE }),
    false,
    false,
    true,
    false
  ],
  [
    'component_navigation.status.failed_X.admin.link',
    'error',
    mockTask({ status: TaskStatuses.Failed }),
    false,
    false,
    true,
    false
  ],
  [
    'component_navigation.status.failed.admin.help',
    'error',
    mockTask({ status: TaskStatuses.Failed, type: UNKNOWN_TASK_TYPE }),
    false,
    false,
    true,
    true
  ],
  [
    'component_navigation.status.failed_X.admin.help',
    'error',
    mockTask({ status: TaskStatuses.Failed }),
    false,
    false,
    true,
    true
  ],
  // failed_branch
  [
    'component_navigation.status.failed_branch',
    'error',
    mockTask({ status: TaskStatuses.Failed, branch: 'foo', type: UNKNOWN_TASK_TYPE }),
    false,
    false,
    false,
    false
  ],
  [
    'component_navigation.status.failed_branch_X',
    'error',
    mockTask({ status: TaskStatuses.Failed, branch: 'foo' }),
    false,
    false,
    false,
    false
  ],
  [
    'component_navigation.status.failed_branch.admin.link',
    'error',
    mockTask({ status: TaskStatuses.Failed, branch: 'foo', type: UNKNOWN_TASK_TYPE }),
    false,
    false,
    true,
    false
  ],
  [
    'component_navigation.status.failed_branch_X.admin.link',
    'error',
    mockTask({ status: TaskStatuses.Failed, branch: 'foo' }),
    false,
    false,
    true,
    false
  ],
  [
    'component_navigation.status.failed_branch.admin.help',
    'error',
    mockTask({ status: TaskStatuses.Failed, branch: 'foo', type: UNKNOWN_TASK_TYPE }),
    false,
    false,
    true,
    true
  ],
  [
    'component_navigation.status.failed_branch_X.admin.help',
    'error',
    mockTask({ status: TaskStatuses.Failed, branch: 'foo' }),
    false,
    false,
    true,
    true
  ],
  // pending
  [
    'component_navigation.status.pending',
    'info',
    mockTask({ type: UNKNOWN_TASK_TYPE }),
    true,
    false,
    false,
    false
  ],
  ['component_navigation.status.pending_X', 'info', mockTask(), true, false, false, false],
  [
    'component_navigation.status.pending.admin.link',
    'info',
    mockTask({ type: UNKNOWN_TASK_TYPE }),
    true,
    false,
    true,
    false
  ],
  [
    'component_navigation.status.pending_X.admin.link',
    'info',
    mockTask(),
    true,
    false,
    true,
    false
  ],
  [
    'component_navigation.status.pending.admin.help',
    'info',
    mockTask({ type: UNKNOWN_TASK_TYPE }),
    true,
    false,
    true,
    true
  ],
  [
    'component_navigation.status.pending_X.admin.help',
    'info',
    mockTask({ status: TaskStatuses.Failed }),
    true,
    false,
    true,
    true
  ],
  // in_progress
  [
    'component_navigation.status.in_progress',
    'info',
    mockTask({ type: UNKNOWN_TASK_TYPE }),
    true,
    true,
    false,
    false
  ],
  ['component_navigation.status.in_progress_X', 'info', mockTask(), true, true, false, false],
  [
    'component_navigation.status.in_progress.admin.link',
    'info',
    mockTask({ type: UNKNOWN_TASK_TYPE }),
    true,
    true,
    true,
    false
  ],
  [
    'component_navigation.status.in_progress_X.admin.link',
    'info',
    mockTask(),
    true,
    true,
    true,
    false
  ],
  [
    'component_navigation.status.in_progress.admin.help',
    'info',
    mockTask({ type: UNKNOWN_TASK_TYPE }),
    true,
    true,
    true,
    true
  ],
  [
    'component_navigation.status.in_progress_X.admin.help',
    'info',
    mockTask({ status: TaskStatuses.Failed }),
    true,
    true,
    true,
    true
  ]
])(
  'should render the expected message=%p',
  (
    expectedMessage: string,
    alertVariant: string,
    currentTask: Task,
    isPending: boolean,
    isInProgress: boolean,
    showBackgroundTasks: boolean,
    onBackgroudTaskPage: boolean
  ) => {
    if (currentTask.type === UNKNOWN_TASK_TYPE) {
      (hasMessage as jest.Mock).mockReturnValueOnce(false);
    }

    const wrapper = shallowRender({
      component: mockComponent({ configuration: { showBackgroundTasks } }),
      currentTask,
      currentTaskOnSameBranch: !currentTask.branch,
      isPending,
      isInProgress,
      location: mockLocation({
        pathname: onBackgroudTaskPage ? '/project/background_tasks' : '/foo/bar'
      })
    });
    const messageProps = wrapper.find(FormattedMessage).props();

    // Translation key.
    expect(messageProps.defaultMessage).toBe(expectedMessage);

    // Alert variant.
    expect(wrapper.find(Alert).props().variant).toBe(alertVariant);

    // Formatted message values prop.
    if (/_X/.test(expectedMessage)) {
      expect(messageProps.values?.type).toBe(`background_task.type.${currentTask.type}`);
    } else {
      expect(messageProps.values?.type).toBeUndefined();
    }

    if (currentTask.branch) {
      expect(messageProps.values?.branch).toBe(currentTask.branch);
    } else {
      expect(messageProps.values?.branch).toBeUndefined();
    }

    if (showBackgroundTasks) {
      if (onBackgroudTaskPage) {
        expect(messageProps.values?.url).toBeUndefined();
        expect(messageProps.values?.stacktrace).toBe('background_tasks.show_stacktrace');
      } else {
        expect(messageProps.values?.url).toBeDefined();
        expect(messageProps.values?.stacktrace).toBeUndefined();
      }
    } else {
      expect(messageProps.values?.url).toBeUndefined();
      expect(messageProps.values?.stacktrace).toBeUndefined();
    }
  }
);

function shallowRender(props: Partial<ComponentNavBgTaskNotif['props']> = {}) {
  return shallow<ComponentNavBgTaskNotif>(
    <ComponentNavBgTaskNotif
      component={mockComponent()}
      currentTask={mockTask({ status: TaskStatuses.Failed })}
      location={mockLocation()}
      {...props}
    />
  );
}
