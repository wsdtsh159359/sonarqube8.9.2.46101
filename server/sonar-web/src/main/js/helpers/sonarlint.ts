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
import { getHostUrl } from 'sonar-ui-common/helpers/urls';
import { Ide } from '../types/sonarlint';

const SONARLINT_PORT_START = 64120;
const SONARLINT_PORT_RANGE = 11;

export async function probeSonarLintServers(): Promise<Array<Ide>> {
  const probedPorts = buildPortRange();
  const probeRequests = probedPorts.map(p =>
    fetch(buildSonarLintEndpoint(p, '/status'))
      .then(r => r.json())
      .then(json => {
        const { ideName, description } = json;
        return { port: p, ideName, description } as Ide;
      })
      .catch(() => undefined)
  );
  const results = await Promise.all(probeRequests);
  return results.filter(r => r !== undefined) as Ide[];
}

export function openHotspot(calledPort: number, projectKey: string, hotspotKey: string) {
  const showUrl = new URL(buildSonarLintEndpoint(calledPort, '/hotspots/show'));
  showUrl.searchParams.set('server', getHostUrl());
  showUrl.searchParams.set('project', projectKey);
  showUrl.searchParams.set('hotspot', hotspotKey);
  return fetch(showUrl.toString());
}

/**
 * @returns [ start , ... , start + size - 1 ]
 */
export function buildPortRange(start = SONARLINT_PORT_START, size = SONARLINT_PORT_RANGE) {
  return Array.from(Array(size).keys()).map(p => start + p);
}

function buildSonarLintEndpoint(port: number, path: string) {
  return `http://localhost:${port}/sonarlint/api${path}`;
}
