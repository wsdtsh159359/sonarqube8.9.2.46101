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
import { InjectedIntl } from 'react-intl';
import { Store as ReduxStore } from 'redux';
import { Theme } from 'sonar-ui-common/components/theme';
import { Location, Router } from '../components/hoc/withRouter';
import { Store } from '../store/rootReducer';
import { L10nBundle } from './l10n';

export interface ExtensionStartMethod {
  (params: ExtensionStartMethodParameter | string): ExtensionStartMethodReturnType;
}

export interface ExtensionStartMethodParameter {
  store: ReduxStore<Store, any>;
  el: HTMLElement | undefined | null;
  currentUser: T.CurrentUser;
  intl: InjectedIntl;
  location: Location;
  router: Router;
  theme: Theme;
  baseUrl: string;
  l10nBundle: L10nBundle;
}

export type ExtensionStartMethodReturnType = React.ReactNode | Function | void | undefined | null;
