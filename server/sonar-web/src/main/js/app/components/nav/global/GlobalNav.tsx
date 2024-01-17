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
import * as React from 'react';
import { connect } from 'react-redux';
import NavBar from 'sonar-ui-common/components/ui/NavBar';
import { getAppState, getCurrentUser, Store } from '../../../../store/rootReducer';
import { rawSizes } from '../../../theme';
import EmbedDocsPopupHelper from '../../embed-docs-modal/EmbedDocsPopupHelper';
import Search from '../../search/Search';
import './GlobalNav.css';
import GlobalNavBranding from './GlobalNavBranding';
import GlobalNavMenu from './GlobalNavMenu';
import GlobalNavUser from './GlobalNavUser';

export interface GlobalNavProps {
  appState: Pick<T.AppState, 'canAdmin' | 'globalPages' | 'qualifiers'>;
  currentUser: T.CurrentUser;
  location: { pathname: string };
}

export function GlobalNav(props: GlobalNavProps) {
  const { appState, currentUser, location } = props;
  return (
    <NavBar className="navbar-global" height={rawSizes.globalNavHeightRaw} id="global-navigation">
      <GlobalNavBranding />

      <GlobalNavMenu appState={appState} currentUser={currentUser} location={location} />

      <ul className="global-navbar-menu global-navbar-menu-right">
        <EmbedDocsPopupHelper />
        <Search currentUser={currentUser} />
        <GlobalNavUser currentUser={currentUser} />
      </ul>
    </NavBar>
  );
}

const mapStateToProps = (state: Store) => {
  return {
    currentUser: getCurrentUser(state),
    appState: getAppState(state)
  };
};

export default connect(mapStateToProps)(GlobalNav);
