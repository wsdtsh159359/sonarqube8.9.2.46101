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
import DateFormatter from 'sonar-ui-common/components/intl/DateFormatter';
import { translate, translateWithParameters } from 'sonar-ui-common/helpers/l10n';
import {
  getEdition,
  getEditionDownloadFilename,
  getEditionDownloadUrl
} from '../../../../helpers/editions';
import { EditionKey } from '../../../../types/editions';
import { SystemUpgrade } from '../../../../types/system';
import SystemUpgradeIntermediate from './SystemUpgradeIntermediate';

export interface SystemUpgradeItemProps {
  edition: EditionKey | undefined;
  isLatestVersion: boolean;
  systemUpgrades: SystemUpgrade[];
}

export default function SystemUpgradeItem(props: SystemUpgradeItemProps) {
  const { edition, isLatestVersion, systemUpgrades } = props;
  const lastUpgrade = systemUpgrades[0];
  const downloadUrl = getEditionDownloadUrl(
    getEdition(edition || EditionKey.community),
    lastUpgrade
  );

  return (
    <div className="system-upgrade-version">
      <h3 className="h1 spacer-bottom">
        <strong>
          {isLatestVersion ? translate('system.latest_version') : translate('system.lts_version')}
        </strong>
        {isLatestVersion && (
          <a
            className="spacer-left medium"
            href="https://www.sonarqube.org/whats-new/?referrer=sonarqube"
            rel="noopener noreferrer"
            target="_blank">
            {translate('system.see_whats_new')}
          </a>
        )}
      </h3>
      <p>
        <FormattedMessage
          defaultMessage={translate('system.version_is_availble')}
          id="system.version_is_availble"
          values={{ version: <b>SonarQube {lastUpgrade.version}</b> }}
        />
      </p>
      <p className="spacer-top">{lastUpgrade.description}</p>
      <div className="big-spacer-top">
        {lastUpgrade.releaseDate && (
          <DateFormatter date={lastUpgrade.releaseDate} long={true}>
            {formattedDate => (
              <span>{translateWithParameters('system.released_x', formattedDate)}</span>
            )}
          </DateFormatter>
        )}
        {lastUpgrade.changeLogUrl && (
          <a
            className="spacer-left"
            href={lastUpgrade.changeLogUrl}
            rel="noopener noreferrer"
            target="_blank">
            {translate('system.release_notes')}
          </a>
        )}
      </div>
      <SystemUpgradeIntermediate className="spacer-top" upgrades={systemUpgrades.slice(1)} />
      <div className="big-spacer-top">
        <a
          className="button"
          download={getEditionDownloadFilename(downloadUrl)}
          href={downloadUrl}
          rel="noopener noreferrer"
          target="_blank">
          {translateWithParameters('system.download_x', lastUpgrade.version)}
        </a>
        <a
          className="spacer-left"
          href="https://redirect.sonarsource.com/doc/upgrading.html"
          rel="noopener noreferrer"
          target="_blank">
          {translate('system.how_to_upgrade')}
        </a>
      </div>
    </div>
  );
}
