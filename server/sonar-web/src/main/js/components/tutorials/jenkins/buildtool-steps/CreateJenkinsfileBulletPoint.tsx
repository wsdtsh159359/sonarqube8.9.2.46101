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
import HelpTooltip from 'sonar-ui-common/components/controls/HelpTooltip';
import { Alert } from 'sonar-ui-common/components/ui/Alert';
import CodeSnippet from '../../../common/CodeSnippet';
import SentenceWithFilename from '../../components/SentenceWithFilename';
import SentenceWithHighlights from '../../components/SentenceWithHighlights';

export interface CreateJenkinsfileBulletPointProps {
  snippet: string;
  alertTranslationKeyPart?: string;
}

export default function CreateJenkinsfileBulletPoint(props: CreateJenkinsfileBulletPointProps) {
  const { snippet, alertTranslationKeyPart } = props;

  return (
    <li className="abs-width-600">
      <SentenceWithFilename
        filename="Jenkinsfile"
        translationKey="onboarding.tutorial.with.jenkins.jenkinsfile.jenkinsfile_step"
      />
      {alertTranslationKeyPart !== undefined && (
        <Alert className="spacer-top" variant="info">
          <p className="text-middle">
            <SentenceWithHighlights
              highlightKeys={['default', 'in_jenkins']}
              translationKey={`${alertTranslationKeyPart}.replace`}
            />
            <HelpTooltip
              className="little-spacer-left"
              overlay={
                <>
                  <p className="spacer-bottom">
                    <SentenceWithHighlights
                      highlightKeys={['path']}
                      translationKey={`${alertTranslationKeyPart}.help1`}
                    />
                  </p>
                  <p>
                    <SentenceWithHighlights
                      highlightKeys={['path', 'name']}
                      translationKey={`${alertTranslationKeyPart}.help2`}
                    />
                  </p>
                </>
              }
            />
          </p>
        </Alert>
      )}
      <CodeSnippet snippet={snippet} />
    </li>
  );
}
