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
import CodeSnippet from '../../common/CodeSnippet';
import SentenceWithFilename from './SentenceWithFilename';

export interface DefaultProjectKeyProps {
  component: T.Component;
}

const sonarProjectSnippet = (key: string) => `sonar.projectKey=${key}`;

export default function DefaultProjectKey(props: DefaultProjectKeyProps) {
  const { component } = props;
  return (
    <li className="abs-width-600">
      <SentenceWithFilename
        filename="sonar-project.properties"
        translationKey="onboarding.tutorial.other.project_key"
      />
      <CodeSnippet snippet={sonarProjectSnippet(component.key)} />
    </li>
  );
}
