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
import { scrollToElement } from 'sonar-ui-common/helpers/scrolling';
import ScreenPositionHelper from '../../../components/common/ScreenPositionHelper';
import { mockRawHotspot, mockStandards } from '../../../helpers/mocks/security-hotspots';
import { mockComponent } from '../../../helpers/testMocks';
import { SecurityStandard } from '../../../types/security';
import { HotspotStatusFilter } from '../../../types/security-hotspots';
import FilterBar from '../components/FilterBar';
import SecurityHotspotsAppRenderer, {
  SecurityHotspotsAppRendererProps
} from '../SecurityHotspotsAppRenderer';

jest.mock('sonar-ui-common/helpers/scrolling', () => ({
  scrollToElement: jest.fn()
}));

jest.mock('../../../components/common/ScreenPositionHelper');

beforeEach(() => {
  jest.clearAllMocks();
});

it('should render correctly', () => {
  expect(shallowRender()).toMatchSnapshot();
  expect(
    shallowRender({
      filters: { assignedToMe: true, sinceLeakPeriod: false, status: HotspotStatusFilter.TO_REVIEW }
    })
  ).toMatchSnapshot('no hotspots with filters');
  expect(shallowRender({ loading: true })).toMatchSnapshot('loading');
});

it('should render correctly with hotspots', () => {
  const hotspots = [mockRawHotspot({ key: 'h1' }), mockRawHotspot({ key: 'h2' })];
  expect(shallowRender({ hotspots, hotspotsTotal: 2 })).toMatchSnapshot();
  expect(
    shallowRender({ hotspots, hotspotsTotal: 3, selectedHotspot: mockRawHotspot({ key: 'h2' }) })
      .find(ScreenPositionHelper)
      .dive()
  ).toMatchSnapshot();
});

it('should render correctly when filtered by category or cwe', () => {
  const hotspots = [mockRawHotspot({ key: 'h1' }), mockRawHotspot({ key: 'h2' })];

  expect(
    shallowRender({ filterByCWE: '327', hotspots, hotspotsTotal: 2, selectedHotspot: hotspots[0] })
      .find(ScreenPositionHelper)
      .dive()
  ).toMatchSnapshot('cwe');
  expect(
    shallowRender({
      filterByCategory: { category: 'a1', standard: SecurityStandard.OWASP_TOP10 },
      hotspots,
      hotspotsTotal: 2,
      selectedHotspot: hotspots[0]
    })
      .find(ScreenPositionHelper)
      .dive()
  ).toMatchSnapshot('category');
});

it('should properly propagate the "show all" call', () => {
  const onShowAllHotspots = jest.fn();
  const wrapper = shallowRender({ onShowAllHotspots });

  wrapper
    .find(FilterBar)
    .props()
    .onShowAllHotspots();

  expect(onShowAllHotspots).toHaveBeenCalled();
});

describe('side effect', () => {
  const fakeElement = document.createElement('span');
  const fakeParent = document.createElement('div');

  beforeEach(() => {
    jest.spyOn(React, 'useEffect').mockImplementationOnce(f => f());
    jest.spyOn(document, 'querySelector').mockImplementationOnce(() => fakeElement);
    jest.spyOn(React, 'useRef').mockImplementationOnce(() => ({ current: fakeParent }));
  });

  it('should trigger scrolling', () => {
    shallowRender({ selectedHotspot: mockRawHotspot() });

    expect(scrollToElement).toBeCalledWith(
      fakeElement,
      expect.objectContaining({ parent: fakeParent })
    );
  });

  it('should not trigger scrolling if no selected hotspot', () => {
    shallowRender();
    expect(scrollToElement).not.toBeCalled();
  });

  it('should not trigger scrolling if no parent', () => {
    const mockUseRef = jest.spyOn(React, 'useRef');
    mockUseRef.mockReset();
    mockUseRef.mockImplementationOnce(() => ({ current: null }));
    shallowRender({ selectedHotspot: mockRawHotspot() });
    expect(scrollToElement).not.toBeCalled();
  });
});

function shallowRender(props: Partial<SecurityHotspotsAppRendererProps> = {}) {
  return shallow(
    <SecurityHotspotsAppRenderer
      component={mockComponent()}
      filters={{
        assignedToMe: false,
        sinceLeakPeriod: false,
        status: HotspotStatusFilter.TO_REVIEW
      }}
      hotspots={[]}
      hotspotsTotal={0}
      isStaticListOfHotspots={true}
      loading={false}
      loadingMeasure={false}
      loadingMore={false}
      onChangeFilters={jest.fn()}
      onHotspotClick={jest.fn()}
      onLoadMore={jest.fn()}
      onShowAllHotspots={jest.fn()}
      onUpdateHotspot={jest.fn()}
      securityCategories={{}}
      selectedHotspot={undefined}
      standards={mockStandards()}
      {...props}
    />
  );
}
