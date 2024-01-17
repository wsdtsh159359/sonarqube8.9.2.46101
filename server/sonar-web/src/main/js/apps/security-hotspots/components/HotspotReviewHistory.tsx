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
import { Button, DeleteButton, EditButton } from 'sonar-ui-common/components/controls/buttons';
import Dropdown, { DropdownOverlay } from 'sonar-ui-common/components/controls/Dropdown';
import Toggler from 'sonar-ui-common/components/controls/Toggler';
import DateTimeFormatter from 'sonar-ui-common/components/intl/DateTimeFormatter';
import { PopupPlacement } from 'sonar-ui-common/components/ui/popups';
import { translate, translateWithParameters } from 'sonar-ui-common/helpers/l10n';
import IssueChangelogDiff from '../../../components/issue/components/IssueChangelogDiff';
import Avatar from '../../../components/ui/Avatar';
import { sanitizeString } from '../../../helpers/sanitize';
import { Hotspot, ReviewHistoryType } from '../../../types/security-hotspots';
import { getHotspotReviewHistory } from '../utils';
import HotspotCommentPopup from './HotspotCommentPopup';

export interface HotspotReviewHistoryProps {
  hotspot: Hotspot;
  onDeleteComment: (key: string) => void;
  onEditComment: (key: string, comment: string) => void;
}

export default function HotspotReviewHistory(props: HotspotReviewHistoryProps) {
  const { hotspot } = props;
  const reviewHistory = getHotspotReviewHistory(hotspot);
  const [editedCommentKey, setEditedCommentKey] = React.useState('');

  return (
    <>
      {reviewHistory.map((historyElt, historyIndex) => {
        const { user, type, diffs, date, html, key, updatable, markdown } = historyElt;
        return (
          <div
            className={classNames('padded', { 'bordered-top': historyIndex > 0 })}
            key={historyIndex}>
            <div className="display-flex-center">
              {user.name && (
                <>
                  <Avatar
                    className="little-spacer-right"
                    hash={user.avatar}
                    name={user.name}
                    size={20}
                  />
                  <strong>
                    {user.active ? user.name : translateWithParameters('user.x_deleted', user.name)}
                  </strong>
                  {type === ReviewHistoryType.Creation && (
                    <span className="little-spacer-left">
                      {translate('hotspots.review_history.created')}
                    </span>
                  )}
                  {type === ReviewHistoryType.Comment && (
                    <span className="little-spacer-left">
                      {translate('hotspots.review_history.comment_added')}
                    </span>
                  )}
                  <span className="little-spacer-left little-spacer-right">-</span>
                </>
              )}
              <DateTimeFormatter date={date} />
            </div>

            {type === ReviewHistoryType.Diff && diffs && (
              <div className="spacer-top">
                {diffs.map((diff, diffIndex) => (
                  <IssueChangelogDiff diff={diff} key={diffIndex} />
                ))}
              </div>
            )}

            {type === ReviewHistoryType.Comment && key && html && markdown && (
              <div className="spacer-top display-flex-space-between">
                <div
                  className="markdown"
                  // eslint-disable-next-line react/no-danger
                  dangerouslySetInnerHTML={{ __html: sanitizeString(html) }}
                />
                {updatable && (
                  <div>
                    <div className="dropdown">
                      <Toggler
                        onRequestClose={() => {
                          setEditedCommentKey('');
                        }}
                        open={key === editedCommentKey}
                        overlay={
                          <DropdownOverlay placement={PopupPlacement.BottomRight}>
                            <HotspotCommentPopup
                              markdownComment={markdown}
                              onCancelEdit={() => setEditedCommentKey('')}
                              onCommentEditSubmit={comment => {
                                setEditedCommentKey('');
                                props.onEditComment(key, comment);
                              }}
                            />
                          </DropdownOverlay>
                        }>
                        <EditButton
                          className="button-small"
                          onClick={() => setEditedCommentKey(key)}
                        />
                      </Toggler>
                    </div>
                    <Dropdown
                      onOpen={() => setEditedCommentKey('')}
                      overlay={
                        <div className="padded abs-width-150">
                          <p>{translate('issue.comment.delete_confirm_message')}</p>
                          <Button
                            className="button-red big-spacer-top pull-right"
                            onClick={() => props.onDeleteComment(key)}>
                            {translate('delete')}
                          </Button>
                        </div>
                      }
                      overlayPlacement={PopupPlacement.BottomRight}>
                      <DeleteButton className="button-small" />
                    </Dropdown>
                  </div>
                )}
              </div>
            )}
          </div>
        );
      })}
    </>
  );
}
