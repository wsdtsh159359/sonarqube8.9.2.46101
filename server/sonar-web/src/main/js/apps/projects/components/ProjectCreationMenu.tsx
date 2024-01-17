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
import { Button } from 'sonar-ui-common/components/controls/buttons';
import Dropdown from 'sonar-ui-common/components/controls/Dropdown';
import DropdownIcon from 'sonar-ui-common/components/icons/DropdownIcon';
import { translate } from 'sonar-ui-common/helpers/l10n';
import { getAlmSettings } from '../../../api/alm-settings';
import { withCurrentUser } from '../../../components/hoc/withCurrentUser';
import { IMPORT_COMPATIBLE_ALMS } from '../../../helpers/constants';
import { hasGlobalPermission } from '../../../helpers/users';
import { AlmKeys, AlmSettingsInstance } from '../../../types/alm-settings';
import { Permissions } from '../../../types/permissions';
import ProjectCreationMenuItem from './ProjectCreationMenuItem';

interface Props {
  className?: string;
  currentUser: T.LoggedInUser;
}

interface State {
  boundAlms: Array<string>;
}

const almSettingsValidators = {
  [AlmKeys.Azure]: (settings: AlmSettingsInstance) => !!settings.url,
  [AlmKeys.BitbucketServer]: (_: AlmSettingsInstance) => true,
  [AlmKeys.BitbucketCloud]: (_: AlmSettingsInstance) => false,
  [AlmKeys.GitHub]: (_: AlmSettingsInstance) => true,
  [AlmKeys.GitLab]: (settings: AlmSettingsInstance) => !!settings.url
};

export class ProjectCreationMenu extends React.PureComponent<Props, State> {
  mounted = false;
  state: State = { boundAlms: [] };

  componentDidMount() {
    this.mounted = true;

    this.fetchAlmBindings();
  }

  componentWillUnmount() {
    this.mounted = false;
  }

  almSettingIsValid = (settings: AlmSettingsInstance) => {
    return almSettingsValidators[settings.alm](settings);
  };

  fetchAlmBindings = async () => {
    const { currentUser } = this.props;
    const canCreateProject = hasGlobalPermission(currentUser, Permissions.ProjectCreation);

    // getAlmSettings requires branchesEnabled
    if (!canCreateProject) {
      return;
    }

    const almSettings: AlmSettingsInstance[] = await getAlmSettings().catch(() => []);

    // Import is only available if exactly one binding is configured
    const boundAlms = IMPORT_COMPATIBLE_ALMS.filter(key => {
      let currentAlmSettings: AlmSettingsInstance[];
      if (key === AlmKeys.BitbucketServer || key === AlmKeys.BitbucketCloud) {
        currentAlmSettings = almSettings.filter(
          s => s.alm === AlmKeys.BitbucketCloud || s.alm === AlmKeys.BitbucketServer
        );
      } else {
        currentAlmSettings = almSettings.filter(s => s.alm === key);
      }
      return currentAlmSettings.length === 1 && this.almSettingIsValid(currentAlmSettings[0]);
    });

    if (this.mounted) {
      this.setState({
        boundAlms
      });
    }
  };

  render() {
    const { className, currentUser } = this.props;
    const { boundAlms } = this.state;

    const canCreateProject = hasGlobalPermission(currentUser, Permissions.ProjectCreation);

    if (!canCreateProject) {
      return null;
    }

    return (
      <Dropdown
        className={className}
        onOpen={this.fetchAlmBindings}
        overlay={
          <ul className="menu">
            {[...boundAlms, 'manual'].map(alm => (
              <li key={alm}>
                <ProjectCreationMenuItem alm={alm} />
              </li>
            ))}
          </ul>
        }>
        <Button className="button-primary">
          {translate('projects.add')}
          <DropdownIcon className="spacer-left " />
        </Button>
      </Dropdown>
    );
  }
}

export default withCurrentUser(ProjectCreationMenu);
