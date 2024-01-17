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
import { getHostUrl } from 'sonar-ui-common/helpers/urls';
import { BuildTools, ManualTutorialConfig } from '../../types';
import ClangGCCCustom from './ClangGCCCommand';
import DotNet from './DotNet';
import JavaGradle from './JavaGradle';
import JavaMaven from './JavaMaven';
import Other from './Other';

export interface AnalysisCommandProps {
  component: T.Component;
  languageConfig: ManualTutorialConfig;
  token?: string;
}

export default function AnalysisCommand(props: AnalysisCommandProps) {
  const { component, languageConfig, token } = props;

  if (!token) {
    return null;
  }

  const host = getHostUrl();
  const projectKey = component.key;

  switch (languageConfig.buildTool) {
    case BuildTools.Maven:
      return <JavaMaven host={host} projectKey={projectKey} token={token} />;

    case BuildTools.Gradle:
      return <JavaGradle host={host} projectKey={projectKey} token={token} />;

    case BuildTools.DotNet:
      return <DotNet host={host} projectKey={projectKey} token={token} />;

    case BuildTools.CFamily:
      return languageConfig.os !== undefined ? (
        <ClangGCCCustom os={languageConfig.os} host={host} projectKey={projectKey} token={token} />
      ) : null;

    case BuildTools.Other:
      return languageConfig.os !== undefined ? (
        <Other host={host} os={languageConfig.os} projectKey={projectKey} token={token} />
      ) : null;

    default:
      return null;
  }
}
