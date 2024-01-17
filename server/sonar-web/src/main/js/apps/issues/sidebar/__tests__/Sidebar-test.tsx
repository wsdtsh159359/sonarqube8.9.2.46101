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
import { shallow, ShallowWrapper } from 'enzyme';
import { flatten } from 'lodash';
import * as React from 'react';
import { mockComponent } from '../../../../helpers/testMocks';
import { getGlobalSettingValue } from '../../../../store/rootReducer';
import { ComponentQualifier } from '../../../../types/component';
import { Query } from '../../utils';
import { mapStateToProps, Sidebar } from '../Sidebar';

jest.mock('../../../../store/rootReducer', () => ({ getGlobalSettingValue: jest.fn() }));

it('should render facets for global page', () => {
  expect(renderSidebar()).toMatchSnapshot();
});

it('should render facets for project', () => {
  expect(renderSidebar({ component: mockComponent() })).toMatchSnapshot();
});

it.each([
  [ComponentQualifier.Application],
  [ComponentQualifier.Portfolio],
  [ComponentQualifier.SubPortfolio],
  [ComponentQualifier.SubProject],
  [ComponentQualifier.Directory],
  [ComponentQualifier.Developper]
])('should render facets for %p', qualifier => {
  expect(renderSidebar({ component: mockComponent({ qualifier }) })).toMatchSnapshot();
});

it('should render facets when my issues are selected', () => {
  expect(renderSidebar({ myIssues: true })).toMatchSnapshot();
});

it('should not render developer nominative facets when asked not to', () => {
  expect(renderSidebar({ disableDeveloperAggregatedInfo: true })).toMatchSnapshot();
});

it('should init the component with the proper store value', () => {
  mapStateToProps({} as any);

  expect(getGlobalSettingValue).toHaveBeenCalledWith(
    expect.any(Object),
    'sonar.developerAggregatedInfo.disabled'
  );
});

const renderSidebar = (props?: Partial<Sidebar['props']>) => {
  return flatten(
    mapChildren(
      shallow<Sidebar>(
        <Sidebar
          component={undefined}
          createdAfterIncludesTime={false}
          facets={{}}
          loadSearchResultCount={jest.fn()}
          loadingFacets={{}}
          myIssues={false}
          onFacetToggle={jest.fn()}
          onFilterChange={jest.fn()}
          openFacets={{}}
          query={{ types: [''] } as Query}
          referencedComponentsById={{}}
          referencedComponentsByKey={{}}
          referencedLanguages={{}}
          referencedRules={{}}
          referencedUsers={{}}
          disableDeveloperAggregatedInfo={false}
          {...props}
        />
      )
    )
  );

  function mapChildren(wrapper: ShallowWrapper) {
    return wrapper.children().map(node => {
      if (typeof node.type() === 'symbol') {
        return node.children().map(n => n.name());
      }
      return node.name();
    });
  }
};
