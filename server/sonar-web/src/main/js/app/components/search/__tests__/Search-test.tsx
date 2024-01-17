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
import * as React from 'react';
import { elementKeydown } from 'sonar-ui-common/helpers/testUtils';
import { mockRouter } from '../../../../helpers/testMocks';
import { ComponentQualifier } from '../../../../types/component';
import { Search } from '../Search';

it('selects results', () => {
  const form = shallowRender();
  form.setState({
    more: { TRK: 15, BRC: 0 },
    open: true,
    results: {
      TRK: [component('foo'), component('bar')],
      BRC: [component('qwe', ComponentQualifier.SubProject)]
    },
    selected: 'foo'
  });
  expect(form.state().selected).toBe('foo');
  next(form, 'bar');
  next(form, 'qualifier###TRK');
  next(form, 'qwe');
  next(form, 'qwe');
  prev(form, 'qualifier###TRK');
  prev(form, 'bar');
  select(form, 'foo');
  prev(form, 'foo');
});

it('opens selected project on enter', () => {
  const router = mockRouter();
  const form = shallowRender({ router });
  const selectedKey = 'project';
  form.setState({
    open: true,
    results: { [ComponentQualifier.Project]: [component(selectedKey)] },
    selected: selectedKey
  });

  elementKeydown(form.find('SearchBox'), 13);
  expect(router.push).toBeCalledWith({ pathname: '/dashboard', query: { id: selectedKey } });
});

it('opens selected portfolio on enter', () => {
  const router = mockRouter();
  const form = shallowRender({ router });
  const selectedKey = 'portfolio';
  form.setState({
    open: true,
    results: {
      [ComponentQualifier.Portfolio]: [component(selectedKey, ComponentQualifier.Portfolio)]
    },
    selected: selectedKey
  });

  elementKeydown(form.find('SearchBox'), 13);
  expect(router.push).toBeCalledWith({ pathname: '/portfolio', query: { id: selectedKey } });
});

it('opens selected subportfolio on enter', () => {
  const router = mockRouter();
  const form = shallowRender({ router });
  const selectedKey = 'sbprtfl';
  form.setState({
    open: true,
    results: {
      [ComponentQualifier.SubPortfolio]: [component(selectedKey, ComponentQualifier.SubPortfolio)]
    },
    selected: selectedKey
  });

  elementKeydown(form.find('SearchBox'), 13);
  expect(router.push).toBeCalledWith({ pathname: '/portfolio', query: { id: selectedKey } });
});

it('shows warning about short input', () => {
  const form = shallowRender();
  form.setState({ shortQuery: true });
  expect(form.find('.navbar-search-input-hint')).toMatchSnapshot();
  form.setState({ query: 'foobar x' });
  expect(form.find('.navbar-search-input-hint')).toMatchSnapshot();
});

function shallowRender(props: Partial<Search['props']> = {}) {
  return shallow<Search>(
    // @ts-ignore
    <Search currentUser={{ isLoggedIn: false }} {...props} />
  );
}

function component(key: string, qualifier = ComponentQualifier.Project) {
  return { key, name: key, qualifier };
}

function next(form: ShallowWrapper<Search['props'], Search['state']>, expected: string) {
  elementKeydown(form.find('SearchBox'), 40);
  expect(form.state().selected).toBe(expected);
}

function prev(form: ShallowWrapper<Search['props'], Search['state']>, expected: string) {
  elementKeydown(form.find('SearchBox'), 38);
  expect(form.state().selected).toBe(expected);
}

function select(form: ShallowWrapper<Search['props'], Search['state']>, expected: string) {
  (form.instance() as Search).handleSelect(expected);
  expect(form.state().selected).toBe(expected);
}
