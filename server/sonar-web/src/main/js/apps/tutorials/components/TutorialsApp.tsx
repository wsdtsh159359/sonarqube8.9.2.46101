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
import handleRequiredAuthentication from 'sonar-ui-common/helpers/handleRequiredAuthentication';
import { withCurrentUser } from '../../../components/hoc/withCurrentUser';
import TutorialSelection from '../../../components/tutorials/TutorialSelection';
import { isLoggedIn } from '../../../helpers/users';
import { ProjectAlmBindingResponse } from '../../../types/alm-settings';

export interface TutorialsAppProps {
  component: T.Component;
  currentUser: T.CurrentUser;
  projectBinding?: ProjectAlmBindingResponse;
}

export function TutorialsApp(props: TutorialsAppProps) {
  const { component, currentUser, projectBinding } = props;

  if (!isLoggedIn(currentUser)) {
    handleRequiredAuthentication();
    return null;
  }

  return (
    <div className="page page-limited">
      <TutorialSelection
        component={component}
        currentUser={currentUser}
        projectBinding={projectBinding}
      />
    </div>
  );
}

export default withCurrentUser(TutorialsApp);
