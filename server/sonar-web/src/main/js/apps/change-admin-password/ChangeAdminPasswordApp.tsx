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
import { changePassword } from '../../api/users';
import { Location, withRouter } from '../../components/hoc/withRouter';
import { getAppState, Store } from '../../store/rootReducer';
import ChangeAdminPasswordAppRenderer from './ChangeAdminPasswordAppRenderer';
import { DEFAULT_ADMIN_LOGIN, DEFAULT_ADMIN_PASSWORD } from './constants';

interface Props {
  canAdmin?: boolean;
  instanceUsesDefaultAdminCredentials?: boolean;
  location: Location;
}

interface State {
  passwordValue: string;
  confirmPasswordValue: string;
  canSubmit?: boolean;
  submitting: boolean;
  success: boolean;
}

export class ChangeAdminPasswordApp extends React.PureComponent<Props, State> {
  mounted = false;

  constructor(props: Props) {
    super(props);

    this.state = {
      passwordValue: '',
      confirmPasswordValue: '',
      submitting: false,
      success: !props.instanceUsesDefaultAdminCredentials
    };
  }

  componentDidMount() {
    this.mounted = true;
  }

  componentWillUnmount() {
    this.mounted = false;
  }

  handlePasswordChange = (passwordValue: string) => {
    this.setState({ passwordValue }, this.checkCanSubmit);
  };

  handleConfirmPasswordChange = (confirmPasswordValue: string) => {
    this.setState({ confirmPasswordValue }, this.checkCanSubmit);
  };

  handleSubmit = async () => {
    const { canSubmit, passwordValue } = this.state;
    if (canSubmit) {
      this.setState({ submitting: true });
      const success = await changePassword({
        login: DEFAULT_ADMIN_LOGIN,
        password: passwordValue
      }).then(
        () => true,
        () => false
      );
      if (this.mounted) {
        this.setState({ submitting: false, success });
      }
    }
  };

  checkCanSubmit = () => {
    this.setState(({ passwordValue, confirmPasswordValue }) => ({
      canSubmit: passwordValue === confirmPasswordValue && passwordValue !== DEFAULT_ADMIN_PASSWORD
    }));
  };

  render() {
    const { canAdmin, location } = this.props;
    const { canSubmit, confirmPasswordValue, passwordValue, submitting, success } = this.state;
    return (
      <ChangeAdminPasswordAppRenderer
        canAdmin={canAdmin}
        passwordValue={passwordValue}
        confirmPasswordValue={confirmPasswordValue}
        canSubmit={canSubmit}
        onPasswordChange={this.handlePasswordChange}
        onConfirmPasswordChange={this.handleConfirmPasswordChange}
        onSubmit={this.handleSubmit}
        submitting={submitting}
        success={success}
        location={location}
      />
    );
  }
}

export const mapStateToProps = (state: Store) => {
  const { canAdmin, instanceUsesDefaultAdminCredentials } = getAppState(state);
  return { canAdmin, instanceUsesDefaultAdminCredentials };
};

export default connect(mapStateToProps)(withRouter(ChangeAdminPasswordApp));
