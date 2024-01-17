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
/* eslint-disable react/no-unused-state */
import * as React from 'react';
import { withAppState } from '../../../components/hoc/withAppState';
import { IndexationContextInterface, IndexationStatus } from '../../../types/indexation';
import { IndexationContext } from './IndexationContext';
import IndexationNotificationHelper from './IndexationNotificationHelper';

interface Props {
  appState: Pick<T.AppState, 'needIssueSync'>;
}

export class IndexationContextProvider extends React.PureComponent<
  React.PropsWithChildren<Props>,
  IndexationContextInterface
> {
  mounted = false;

  componentDidMount() {
    this.mounted = true;

    if (this.props.appState.needIssueSync) {
      IndexationNotificationHelper.startPolling(this.handleNewStatus);
    } else {
      this.setState({ status: { isCompleted: true, percentCompleted: 100, hasFailures: false } });
    }
  }

  componentWillUnmount() {
    this.mounted = false;

    IndexationNotificationHelper.stopPolling();
  }

  handleNewStatus = (newIndexationStatus: IndexationStatus) => {
    if (this.mounted) {
      this.setState({ status: newIndexationStatus });
    }
  };

  render() {
    return (
      <IndexationContext.Provider value={this.state}>
        {this.props.children}
      </IndexationContext.Provider>
    );
  }
}

export default withAppState(IndexationContextProvider);
