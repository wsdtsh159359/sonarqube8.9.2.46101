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
package org.sonar.db.permission.template;

import javax.annotation.Nullable;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.DbTester;
import org.sonar.db.user.GroupDto;
import org.sonar.db.user.UserDto;

import static java.util.Optional.ofNullable;
import static org.sonar.db.permission.template.PermissionTemplateTesting.newPermissionTemplateCharacteristicDto;
import static org.sonar.db.permission.template.PermissionTemplateTesting.newPermissionTemplateDto;

public class PermissionTemplateDbTester {
  private final DbTester db;
  private final DbClient dbClient;
  private final DbSession dbSession;

  public PermissionTemplateDbTester(DbTester db) {
    this.db = db;
    this.dbClient = db.getDbClient();
    this.dbSession = db.getSession();
  }

  public void setDefaultTemplates(String projectDefaultTemplateUuid, @Nullable String applicationDefaultTemplateUuid, @Nullable String portfoliosDefaultTemplateUuid) {
    db.getDbClient().internalPropertiesDao().save(dbSession, "defaultTemplate.prj", projectDefaultTemplateUuid);
    if (applicationDefaultTemplateUuid != null) {
      db.getDbClient().internalPropertiesDao().save(dbSession, "defaultTemplate.app", applicationDefaultTemplateUuid);
    }
    if (portfoliosDefaultTemplateUuid != null) {
      db.getDbClient().internalPropertiesDao().save(dbSession, "defaultTemplate.port", portfoliosDefaultTemplateUuid);
    }
    dbSession.commit();
  }

  public void setDefaultTemplates(PermissionTemplateDto projectDefaultTemplate, @Nullable PermissionTemplateDto applicationDefaultTemplate,
    @Nullable PermissionTemplateDto portfoliosDefaultTemplate) {
    setDefaultTemplates(projectDefaultTemplate.getUuid(),
      ofNullable(applicationDefaultTemplate).map(PermissionTemplateDto::getUuid).orElse(null),
      ofNullable(portfoliosDefaultTemplate).map(PermissionTemplateDto::getUuid).orElse(null));
  }

  public PermissionTemplateDto insertTemplate() {
    return insertTemplate(newPermissionTemplateDto());
  }

  public PermissionTemplateDto insertTemplate(PermissionTemplateDto template) {
    PermissionTemplateDto templateInDb = dbClient.permissionTemplateDao().insert(dbSession, template);
    db.commit();
    return templateInDb;
  }

  public void addGroupToTemplate(PermissionTemplateDto permissionTemplate, GroupDto group, String permission) {
    addGroupToTemplate(permissionTemplate.getUuid(), group.getUuid(), permission);
  }

  public void addGroupToTemplate(String templateUuid, @Nullable String groupUuid, String permission) {
    dbClient.permissionTemplateDao().insertGroupPermission(dbSession, templateUuid, groupUuid, permission);
    db.commit();
  }

  public void addAnyoneToTemplate(PermissionTemplateDto permissionTemplate, String permission) {
    addGroupToTemplate(permissionTemplate.getUuid(), null, permission);
  }

  public void addUserToTemplate(PermissionTemplateDto permissionTemplate, UserDto user, String permission) {
    addUserToTemplate(permissionTemplate.getUuid(), user.getUuid(), permission);
  }

  public void addUserToTemplate(String templateUuid, String userUuid, String permission) {
    dbClient.permissionTemplateDao().insertUserPermission(dbSession, templateUuid, userUuid, permission);
    db.commit();
  }

  public void addProjectCreatorToTemplate(PermissionTemplateDto permissionTemplate, String permission) {
    addProjectCreatorToTemplate(permissionTemplate.getUuid(), permission);
  }

  public void addProjectCreatorToTemplate(String templateUuid, String permission) {
    dbClient.permissionTemplateCharacteristicDao().insert(dbSession, newPermissionTemplateCharacteristicDto()
      .setWithProjectCreator(true)
      .setTemplateUuid(templateUuid)
      .setPermission(permission));
    db.commit();
  }
}
