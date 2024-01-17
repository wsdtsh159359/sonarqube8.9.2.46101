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
import * as classNames from 'classnames';
import * as React from 'react';
import { DropdownOverlay } from 'sonar-ui-common/components/controls/Dropdown';
import Toggler from 'sonar-ui-common/components/controls/Toggler';
import Tooltip from 'sonar-ui-common/components/controls/Tooltip';
import { PopupPlacement } from 'sonar-ui-common/components/ui/popups';
import { translate } from 'sonar-ui-common/helpers/l10n';

export interface LineDuplicationBlockProps {
  blocksLoaded: boolean;
  duplicated: boolean;
  index: number;
  line: T.SourceLine;
  onClick?: (line: T.SourceLine) => void;
  renderDuplicationPopup: (index: number, line: number) => React.ReactNode;
}

export function LineDuplicationBlock(props: LineDuplicationBlockProps) {
  const { blocksLoaded, duplicated, index, line } = props;
  const [dropdownOpen, setDropdownOpen] = React.useState(false);

  const className = classNames('source-meta', 'source-line-duplications', {
    'source-line-duplicated': duplicated
  });

  return duplicated ? (
    <td className={className} data-index={index} data-line-number={line.line}>
      <Tooltip
        overlay={dropdownOpen ? undefined : translate('source_viewer.tooltip.duplicated_block')}
        placement="right">
        <div>
          <Toggler
            onRequestClose={() => setDropdownOpen(false)}
            open={dropdownOpen}
            overlay={
              <DropdownOverlay placement={PopupPlacement.RightTop}>
                {props.renderDuplicationPopup(index, line.line)}
              </DropdownOverlay>
            }>
            <div
              aria-label={translate('source_viewer.tooltip.duplicated_block')}
              className="source-line-bar"
              onClick={() => {
                setDropdownOpen(true);
                if (!blocksLoaded && line.duplicated && props.onClick) {
                  props.onClick(line);
                }
              }}
              role="button"
              tabIndex={0}
            />
          </Toggler>
        </div>
      </Tooltip>
    </td>
  ) : (
    <td className={className} data-index={index} data-line-number={line.line}>
      <div className="source-line-bar" />
    </td>
  );
}

export default React.memo(LineDuplicationBlock);
