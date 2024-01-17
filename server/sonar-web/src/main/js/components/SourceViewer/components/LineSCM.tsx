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
import Dropdown from 'sonar-ui-common/components/controls/Dropdown';
import { PopupPlacement } from 'sonar-ui-common/components/ui/popups';
import { translate, translateWithParameters } from 'sonar-ui-common/helpers/l10n';
import SCMPopup from './SCMPopup';

export interface LineSCMProps {
  line: T.SourceLine;
  previousLine: T.SourceLine | undefined;
}

export function LineSCM({ line, previousLine }: LineSCMProps) {
  const hasPopup = !!line.line;
  const cell = (
    <div className="source-line-scm-inner">
      {isSCMChanged(line, previousLine) ? line.scmAuthor || '…' : ' '}
    </div>
  );

  if (hasPopup) {
    let ariaLabel = translate('source_viewer.click_for_scm_info');
    if (line.scmAuthor) {
      ariaLabel = `${translateWithParameters(
        'source_viewer.author_X',
        line.scmAuthor
      )}, ${ariaLabel}`;
    }

    return (
      <td className="source-meta source-line-scm" data-line-number={line.line}>
        <Dropdown overlay={<SCMPopup line={line} />} overlayPlacement={PopupPlacement.RightTop}>
          <div aria-label={ariaLabel} role="button">
            {cell}
          </div>
        </Dropdown>
      </td>
    );
  } else {
    return (
      <td className="source-meta source-line-scm" data-line-number={line.line}>
        {cell}
      </td>
    );
  }
}

function isSCMChanged(s: T.SourceLine, p: T.SourceLine | undefined) {
  let changed = true;
  if (p != null && s.scmRevision != null && p.scmRevision != null) {
    changed = s.scmRevision !== p.scmRevision || s.scmDate !== p.scmDate;
  }
  return changed;
}

export default React.memo(LineSCM);
