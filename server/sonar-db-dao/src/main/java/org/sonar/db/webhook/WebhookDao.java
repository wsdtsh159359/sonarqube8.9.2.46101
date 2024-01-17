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
package org.sonar.db.webhook;

import java.util.List;
import java.util.Optional;
import org.sonar.api.utils.System2;
import org.sonar.db.Dao;
import org.sonar.db.DbSession;
import org.sonar.db.project.ProjectDto;

public class WebhookDao implements Dao {

  private final System2 system2;

  public WebhookDao(System2 system2) {
    this.system2 = system2;
  }

  public List<WebhookDto> selectGlobalWebhooks(DbSession dbSession) {
    return mapper(dbSession).selectGlobalWebhooksOrderedByName();
  }

  public Optional<WebhookDto> selectByUuid(DbSession dbSession, String uuid) {
    return Optional.ofNullable(mapper(dbSession).selectByUuid(uuid));
  }

  public List<WebhookDto> selectByProject(DbSession dbSession, ProjectDto projectDto) {
    return mapper(dbSession).selectForProjectUuidOrderedByName(projectDto.getUuid());
  }

  public void insert(DbSession dbSession, WebhookDto dto) {
    mapper(dbSession).insert(dto.setCreatedAt(system2.now()).setUpdatedAt(system2.now()));
  }

  public void update(DbSession dbSession, WebhookDto dto) {
    mapper(dbSession).update(dto.setUpdatedAt(system2.now()));
  }

  public void delete(DbSession dbSession, String uuid) {
    mapper(dbSession).delete(uuid);
  }

  public void deleteByProject(DbSession dbSession, ProjectDto projectDto) {
    mapper(dbSession).deleteForProjectUuid(projectDto.getUuid());
  }

  private static WebhookMapper mapper(DbSession dbSession) {
    return dbSession.getMapper(WebhookMapper.class);
  }

}
