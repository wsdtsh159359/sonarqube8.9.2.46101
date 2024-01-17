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
import { get, remove, save } from 'sonar-ui-common/helpers/storage';
import { getIndexationStatus } from '../../../api/ce';
import { IndexationStatus } from '../../../types/indexation';

const POLLING_INTERVAL_MS = 5000;
const LS_INDEXATION_COMPLETED_NOTIFICATION_SHOULD_BE_DISPLAYED =
  'display_indexation_completed_notification';

export default class IndexationNotificationHelper {
  private static interval?: NodeJS.Timeout;

  static startPolling(onNewStatus: (status: IndexationStatus) => void) {
    this.stopPolling();

    // eslint-disable-next-line promise/catch-or-return
    this.poll(onNewStatus).then(status => {
      if (!status.isCompleted) {
        this.interval = setInterval(() => this.poll(onNewStatus), POLLING_INTERVAL_MS);
      }
    });
  }

  static stopPolling() {
    if (this.interval) {
      clearInterval(this.interval);
    }
  }

  static async poll(onNewStatus: (status: IndexationStatus) => void) {
    const status = await getIndexationStatus();

    onNewStatus(status);

    if (status.isCompleted) {
      this.stopPolling();
    }

    return status;
  }

  static markCompletedNotificationAsToDisplay() {
    save(LS_INDEXATION_COMPLETED_NOTIFICATION_SHOULD_BE_DISPLAYED, true.toString());
  }

  static markCompletedNotificationAsDisplayed() {
    remove(LS_INDEXATION_COMPLETED_NOTIFICATION_SHOULD_BE_DISPLAYED);
  }

  static shouldDisplayCompletedNotification() {
    return JSON.parse(
      get(LS_INDEXATION_COMPLETED_NOTIFICATION_SHOULD_BE_DISPLAYED) || false.toString()
    );
  }
}
