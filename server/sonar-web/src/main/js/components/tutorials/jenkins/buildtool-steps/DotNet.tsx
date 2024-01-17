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
import { translate } from 'sonar-ui-common/helpers/l10n';
import RenderOptions from '../../components/RenderOptions';
import { OSs } from '../../types';
import DotNetCore from './DotNetCore';
import DotNetFramework from './DotNetFramework';

export interface DotNetProps {
  component: T.Component;
}

export interface DotNetCoreFrameworkProps {
  component: T.Component;
  os: OSDotNet;
}

export type OSDotNet = OSs.Linux | OSs.Windows;

const DotNetFlavor = { win_core: DotNetCore, win_msbuild: DotNetFramework, linux_core: DotNetCore };
const DotOS: { [key in keyof typeof DotNetFlavor]: OSDotNet } = {
  win_core: OSs.Windows,
  win_msbuild: OSs.Windows,
  linux_core: OSs.Linux
};

export default function DotNet({ component }: DotNetProps) {
  const [flavorComponent, setFlavorComponet] = React.useState<keyof typeof DotNetFlavor>();
  const DotNetTutorial = flavorComponent && DotNetFlavor[flavorComponent];
  return (
    <>
      <li>
        {translate('onboarding.tutorial.with.jenkins.jenkinsfile.dotnet.build_agent')}
        <RenderOptions
          checked={flavorComponent}
          name="flavorComponent"
          optionLabelKey="onboarding.build.dotnet"
          onCheck={value => setFlavorComponet(value as keyof typeof DotNetFlavor)}
          options={Object.keys(DotNetFlavor)}
        />
      </li>
      {DotNetTutorial && flavorComponent && (
        <DotNetTutorial component={component} os={DotOS[flavorComponent]} />
      )}
    </>
  );
}
