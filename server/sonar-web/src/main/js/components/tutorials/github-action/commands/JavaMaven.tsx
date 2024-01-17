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
import { FormattedMessage } from 'react-intl';
import { ClipboardIconButton } from 'sonar-ui-common/components/controls/clipboard';
import { translate } from 'sonar-ui-common/helpers/l10n';
import CodeSnippet from '../../../common/CodeSnippet';
import { mavenPomSnippet } from '../../utils';
import CreateYmlFile from './CreateYmlFile';

export interface JavaMavenProps {
  branchesEnabled?: boolean;
  component: T.Component;
}

const mavenYamlTemplte = (branchesEnabled: boolean) => `name: Build
on:
  push:
    branches:
      - master # or the name of your main branch
${branchesEnabled ? '  pull_request:\n    types: [opened, synchronize, reopened]' : ''}
jobs:
  build:
    name: Build
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v2
        with:
          fetch-depth: 0  # Shallow clones should be disabled for a better relevancy of analysis
      - name: Set up JDK 11
        uses: actions/setup-java@v1
        with:
          java-version: 11
      - name: Cache SonarQube packages
        uses: actions/cache@v1
        with:
          path: ~/.sonar/cache
          key: \${{ runner.os }}-sonar
          restore-keys: \${{ runner.os }}-sonar
      - name: Cache Maven packages
        uses: actions/cache@v1
        with:
          path: ~/.m2
          key: \${{ runner.os }}-m2-\${{ hashFiles('**/pom.xml') }}
          restore-keys: \${{ runner.os }}-m2
      - name: Build and analyze
        env:
          GITHUB_TOKEN: \${{ secrets.GITHUB_TOKEN }}  # Needed to get PR information, if any
          SONAR_TOKEN: \${{ secrets.SONAR_TOKEN }}
          SONAR_HOST_URL: \${{ secrets.SONAR_HOST_URL }}
        run: mvn -B verify org.sonarsource.scanner.maven:sonar-maven-plugin:sonar`;

export default function JavaMaven(props: JavaMavenProps) {
  const { component, branchesEnabled } = props;
  return (
    <>
      <li className="abs-width-600">
        <FormattedMessage
          defaultMessage={translate('onboarding.tutorial.with.github_action.yaml.maven.pom')}
          id="onboarding.tutorial.with.github_action.yaml.maven.pom"
          values={{
            pom: (
              <>
                <code className="rule">pom.xml</code>
                <ClipboardIconButton copyValue="pom.xml" />
              </>
            )
          }}
        />
        <CodeSnippet snippet={mavenPomSnippet(component.key)} />
      </li>
      <CreateYmlFile yamlTemplate={mavenYamlTemplte(!!branchesEnabled)} />
    </>
  );
}
