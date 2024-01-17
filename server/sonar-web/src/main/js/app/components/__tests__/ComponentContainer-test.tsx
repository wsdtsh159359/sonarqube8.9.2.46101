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
import { getProjectAlmBinding } from '../../../api/alm-settings';
import { getBranches, getPullRequests } from '../../../api/branches';
import { getAnalysisStatus, getTasksForComponent } from '../../../api/ce';
import { getComponentData } from '../../../api/components';
import { getComponentNavigation } from '../../../api/nav';
import { mockBranch, mockMainBranch, mockPullRequest } from '../../../helpers/mocks/branch-like';
import { mockTask } from '../../../helpers/mocks/tasks';
import { mockComponent, mockLocation, mockRouter } from '../../../helpers/testMocks';
import { AlmKeys } from '../../../types/alm-settings';
import { ComponentQualifier } from '../../../types/component';
import { TaskStatuses } from '../../../types/tasks';
import { ComponentContainer } from '../ComponentContainer';
import PageUnavailableDueToIndexation from '../indexation/PageUnavailableDueToIndexation';

jest.mock('../../../api/branches', () => {
  const { mockMainBranch, mockPullRequest } = jest.requireActual(
    '../../../helpers/mocks/branch-like'
  );
  return {
    getBranches: jest
      .fn()
      .mockResolvedValue([mockMainBranch({ status: { qualityGateStatus: 'OK' } })]),
    getPullRequests: jest
      .fn()
      .mockResolvedValue([
        mockPullRequest({ key: 'pr-89', status: { qualityGateStatus: 'ERROR' } }),
        mockPullRequest({ key: 'pr-90', title: 'PR Feature 2' })
      ])
  };
});

jest.mock('../../../api/ce', () => ({
  getAnalysisStatus: jest.fn().mockResolvedValue({ component: { warnings: [] } }),
  getTasksForComponent: jest.fn().mockResolvedValue({ queue: [] })
}));

jest.mock('../../../api/components', () => ({
  getComponentData: jest.fn().mockResolvedValue({ component: { analysisDate: '2018-07-30' } })
}));

jest.mock('../../../api/nav', () => ({
  getComponentNavigation: jest.fn().mockResolvedValue({
    breadcrumbs: [{ key: 'portfolioKey', name: 'portfolio', qualifier: 'VW' }],
    key: 'portfolioKey'
  })
}));

jest.mock('../../../api/alm-settings', () => ({
  getProjectAlmBinding: jest.fn().mockResolvedValue(undefined)
}));

// mock this, because some of its children are using redux store
jest.mock('../nav/component/ComponentNav', () => ({
  default: () => null
}));

const Inner = () => <div />;

beforeEach(() => {
  jest.clearAllMocks();
});

it('changes component', () => {
  const wrapper = shallowRender();
  wrapper.setState({
    branchLikes: [mockMainBranch()],
    component: { qualifier: 'TRK', visibility: 'public' } as T.Component,
    loading: false
  });

  (wrapper.find(Inner).prop('onComponentChange') as Function)({ visibility: 'private' });
  expect(wrapper.state().component).toEqual({ qualifier: 'TRK', visibility: 'private' });
});

it('loads the project binding, if any', async () => {
  (getProjectAlmBinding as jest.Mock).mockResolvedValueOnce(undefined).mockResolvedValueOnce({
    alm: AlmKeys.GitHub,
    key: 'foo'
  });

  const wrapper = shallowRender();
  await waitAndUpdate(wrapper);
  expect(getProjectAlmBinding).toBeCalled();
  expect(wrapper.state().projectBinding).toBeUndefined();

  wrapper.setProps({ location: mockLocation({ query: { id: 'bar' } }) });
  await waitAndUpdate(wrapper);
  expect(wrapper.state().projectBinding).toEqual({ alm: AlmKeys.GitHub, key: 'foo' });
});

it("doesn't load branches portfolio", async () => {
  const wrapper = shallowRender({ location: mockLocation({ query: { id: 'portfolioKey' } }) });
  await new Promise(setImmediate);
  expect(getBranches).not.toBeCalled();
  expect(getPullRequests).not.toBeCalled();
  expect(getComponentData).toBeCalledWith({ component: 'portfolioKey', branch: undefined });
  expect(getComponentNavigation).toBeCalledWith({ component: 'portfolioKey', branch: undefined });
  wrapper.update();
  expect(wrapper.find(Inner).exists()).toBe(true);
});

it('updates branches on change', async () => {
  const registerBranchStatus = jest.fn();
  const wrapper = shallowRender({
    location: mockLocation({ query: { id: 'portfolioKey' } }),
    registerBranchStatus
  });
  wrapper.setState({
    branchLikes: [mockMainBranch()],
    component: mockComponent({
      breadcrumbs: [{ key: 'projectKey', name: 'project', qualifier: 'TRK' }]
    }),
    loading: false
  });
  wrapper.find(Inner).prop<Function>('onBranchesChange')();
  expect(getBranches).toBeCalledWith('projectKey');
  expect(getPullRequests).toBeCalledWith('projectKey');
  await waitAndUpdate(wrapper);
  expect(registerBranchStatus).toBeCalledTimes(2);
});

it('fetches status', async () => {
  (getComponentData as jest.Mock<any>).mockResolvedValueOnce({
    component: {}
  });

  shallowRender();
  await new Promise(setImmediate);
  expect(getTasksForComponent).toBeCalledWith('portfolioKey');
});

it('filters correctly the pending tasks for a main branch', () => {
  const wrapper = shallowRender();
  const component = wrapper.instance();
  const mainBranch = mockMainBranch();
  const branch3 = mockBranch({ name: 'branch-3' });
  const branch2 = mockBranch({ name: 'branch-2' });
  const pullRequest = mockPullRequest();

  expect(component.isSameBranch({} /*, undefined*/)).toBe(true);
  expect(component.isSameBranch({}, mainBranch)).toBe(true);
  expect(component.isSameBranch({ branch: mainBranch.name }, mainBranch)).toBe(true);
  expect(component.isSameBranch({}, branch3)).toBe(false);
  expect(component.isSameBranch({ branch: branch3.name }, branch3)).toBe(true);
  expect(component.isSameBranch({ branch: 'feature' }, branch2)).toBe(false);
  expect(component.isSameBranch({ branch: 'branch-6.6' }, branch2)).toBe(false);
  expect(component.isSameBranch({ branch: branch2.name }, branch2)).toBe(true);
  expect(component.isSameBranch({ branch: 'branch-6.7' }, pullRequest)).toBe(false);
  expect(component.isSameBranch({ pullRequest: pullRequest.key }, pullRequest)).toBe(true);

  const currentTask = mockTask({ pullRequest: pullRequest.key, status: TaskStatuses.InProgress });
  const failedTask = { ...currentTask, status: TaskStatuses.Failed };
  const pendingTasks = [currentTask, mockTask({ branch: branch3.name }), mockTask()];
  expect(component.getCurrentTask(currentTask, undefined)).toBeUndefined();
  expect(component.getCurrentTask(failedTask, mainBranch)).toBe(failedTask);
  expect(component.getCurrentTask(currentTask, mainBranch)).toBeUndefined();
  expect(component.getCurrentTask(currentTask, pullRequest)).toMatchObject(currentTask);
  expect(component.getPendingTasks(pendingTasks, mainBranch)).toMatchObject([{}]);
  expect(component.getPendingTasks(pendingTasks, pullRequest)).toMatchObject([currentTask]);
});

it('reload component after task progress finished', async () => {
  jest.useFakeTimers();
  (getTasksForComponent as jest.Mock<any>)
    .mockResolvedValueOnce({
      queue: [{ id: 'foo', status: TaskStatuses.InProgress }]
    })
    .mockResolvedValueOnce({
      queue: []
    });
  const wrapper = shallowRender();

  // First round, there's something in the queue, and component navigation was
  // not called again (it's called once at mount, hence the 1 times assertion
  // here).
  await waitAndUpdate(wrapper);
  expect(getComponentNavigation).toHaveBeenCalledTimes(1);
  expect(getTasksForComponent).toHaveBeenCalledTimes(1);

  jest.runOnlyPendingTimers();

  // Second round, the queue is now empty, hence we assume the previous task
  // was done. We immediately load the component again.
  expect(getTasksForComponent).toHaveBeenCalledTimes(2);

  // Trigger the update.
  await waitAndUpdate(wrapper);
  // The component was correctly re-loaded.
  expect(getComponentNavigation).toHaveBeenCalledTimes(2);
  // The status API call will be called 1 final time after the component is
  // fully loaded, so the total will be 3.
  expect(getTasksForComponent).toHaveBeenCalledTimes(3);

  // Make sure the timeout was cleared. It should not be called again.
  jest.runAllTimers();
  await waitAndUpdate(wrapper);
  // The number of calls haven't changed.
  expect(getComponentNavigation).toHaveBeenCalledTimes(2);
  expect(getTasksForComponent).toHaveBeenCalledTimes(3);
});

it('reloads component after task progress finished, and moves straight to current', async () => {
  jest.useFakeTimers();
  (getComponentData as jest.Mock<any>).mockResolvedValueOnce({
    component: { key: 'bar' }
  });
  (getTasksForComponent as jest.Mock<any>)
    .mockResolvedValueOnce({ queue: [] })
    .mockResolvedValueOnce({ queue: [], current: { id: 'foo', status: TaskStatuses.Success } });
  const wrapper = shallowRender();

  // First round, nothing in the queue, and component navigation was not called
  // again (it's called once at mount, hence the 1 times assertion here).
  await waitAndUpdate(wrapper);
  expect(getComponentNavigation).toHaveBeenCalledTimes(1);
  expect(getTasksForComponent).toHaveBeenCalledTimes(1);

  jest.runOnlyPendingTimers();

  // Second round, nothing in the queue, BUT a success task is current. This
  // means the queue was processed too quick for us to see, and we didn't see
  // any pending tasks in the queue. So we immediately load the component again.
  expect(getTasksForComponent).toHaveBeenCalledTimes(2);

  // Trigger the update.
  await waitAndUpdate(wrapper);
  // The component was correctly re-loaded.
  expect(getComponentNavigation).toHaveBeenCalledTimes(2);
  // The status API call will be called 1 final time after the component is
  // fully loaded, so the total will be 3.
  expect(getTasksForComponent).toHaveBeenCalledTimes(3);
});

it('should show component not found if it does not exist', async () => {
  (getComponentNavigation as jest.Mock).mockRejectedValueOnce({ status: 404 });
  const wrapper = shallowRender();
  await waitAndUpdate(wrapper);
  expect(wrapper).toMatchSnapshot();
});

it('should redirect if the user has no access', async () => {
  (getComponentNavigation as jest.Mock).mockRejectedValueOnce({ status: 403 });
  const requireAuthorization = jest.fn();
  const wrapper = shallowRender({ requireAuthorization });
  await waitAndUpdate(wrapper);
  expect(requireAuthorization).toBeCalled();
});

it('should redirect if the component is a portfolio', async () => {
  const componentKey = 'comp-key';
  (getComponentData as jest.Mock<any>).mockResolvedValueOnce({
    component: { key: componentKey, breadcrumbs: [{ qualifier: ComponentQualifier.Portfolio }] }
  });

  const replace = jest.fn();

  const wrapper = shallowRender({
    location: mockLocation({ pathname: '/dashboard' }),
    router: mockRouter({ replace })
  });
  await waitAndUpdate(wrapper);
  expect(replace).toBeCalledWith({ pathname: '/portfolio', query: { id: componentKey } });
});

it('should display display the unavailable page if the component needs issue sync', async () => {
  (getComponentData as jest.Mock).mockResolvedValueOnce({
    component: { key: 'test', qualifier: ComponentQualifier.Project, needIssueSync: true }
  });

  const wrapper = shallowRender();

  await waitAndUpdate(wrapper);

  expect(wrapper.find(PageUnavailableDueToIndexation).exists()).toBe(true);
});

it('should correctly reload last task warnings if anything got dismissed', async () => {
  (getComponentData as jest.Mock<any>).mockResolvedValueOnce({
    component: mockComponent({
      breadcrumbs: [{ key: 'foo', name: 'Foo', qualifier: ComponentQualifier.Project }]
    })
  });
  (getComponentNavigation as jest.Mock).mockResolvedValueOnce({});

  const wrapper = shallowRender();
  await waitAndUpdate(wrapper);
  (getAnalysisStatus as jest.Mock).mockClear();

  wrapper.instance().handleWarningDismiss();
  expect(getAnalysisStatus).toBeCalledTimes(1);
});

function shallowRender(props: Partial<ComponentContainer['props']> = {}) {
  return shallow<ComponentContainer>(
    <ComponentContainer
      location={mockLocation({ query: { id: 'foo' } })}
      registerBranchStatus={jest.fn()}
      requireAuthorization={jest.fn()}
      router={mockRouter()}
      {...props}>
      <Inner />
    </ComponentContainer>
  );
}
