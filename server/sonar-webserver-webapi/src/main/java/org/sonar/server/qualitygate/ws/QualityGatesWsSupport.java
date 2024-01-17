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
package org.sonar.server.qualitygate.ws;

import java.util.Objects;
import javax.annotation.Nullable;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.project.ProjectDto;
import org.sonar.db.qualitygate.QualityGateConditionDto;
import org.sonar.db.qualitygate.QualityGateDto;
import org.sonar.server.component.ComponentFinder;
import org.sonar.server.user.UserSession;
import org.sonarqube.ws.Qualitygates;

import static com.google.common.base.Preconditions.checkArgument;
import static org.sonar.api.web.UserRole.ADMIN;
import static org.sonar.db.permission.GlobalPermission.ADMINISTER_QUALITY_GATES;
import static org.sonar.server.exceptions.NotFoundException.checkFound;
import static org.sonar.server.user.AbstractUserSession.insufficientPrivilegesException;

public class QualityGatesWsSupport {

  private final DbClient dbClient;
  private final UserSession userSession;
  private final ComponentFinder componentFinder;

  public QualityGatesWsSupport(DbClient dbClient, UserSession userSession, ComponentFinder componentFinder) {
    this.dbClient = dbClient;
    this.userSession = userSession;
    this.componentFinder = componentFinder;
  }

  public QualityGateDto getByUuid(DbSession dbSession, String qualityGateUuid) {
    return checkFound(
      dbClient.qualityGateDao().selectByUuid(dbSession, qualityGateUuid),
      "No quality gate has been found for id %s", qualityGateUuid);
  }

  public QualityGateDto getByName(DbSession dbSession, String qualityGateName) {
    return checkFound(dbClient.qualityGateDao().selectByName(dbSession, qualityGateName),
      "No quality gate has been found for name %s", qualityGateName);
  }

  QualityGateConditionDto getCondition(DbSession dbSession, String uuid) {
    return checkFound(dbClient.gateConditionDao().selectByUuid(uuid, dbSession), "No quality gate condition with uuid '%s'", uuid);
  }

  boolean isQualityGateAdmin() {
    return userSession.hasPermission(ADMINISTER_QUALITY_GATES);
  }

  Qualitygates.Actions getActions(QualityGateDto qualityGate, @Nullable QualityGateDto defaultQualityGate) {
    boolean isDefault = defaultQualityGate != null && Objects.equals(defaultQualityGate.getUuid(), qualityGate.getUuid());
    boolean isBuiltIn = qualityGate.isBuiltIn();
    boolean isQualityGateAdmin = isQualityGateAdmin();
    return Qualitygates.Actions.newBuilder()
      .setCopy(isQualityGateAdmin)
      .setRename(!isBuiltIn && isQualityGateAdmin)
      .setManageConditions(!isBuiltIn && isQualityGateAdmin)
      .setDelete(!isDefault && !isBuiltIn && isQualityGateAdmin)
      .setSetAsDefault(!isDefault && isQualityGateAdmin)
      .setAssociateProjects(!isDefault && isQualityGateAdmin)
      .build();
  }

  void checkCanEdit(QualityGateDto qualityGate) {
    checkNotBuiltIn(qualityGate);
    userSession.checkPermission(ADMINISTER_QUALITY_GATES);
  }

  void checkCanAdminProject(ProjectDto project) {
    if (userSession.hasPermission(ADMINISTER_QUALITY_GATES)
      || userSession.hasProjectPermission(ADMIN, project)) {
      return;
    }
    throw insufficientPrivilegesException();
  }

  ProjectDto getProject(DbSession dbSession, String projectKey) {
    return componentFinder.getProjectByKey(dbSession, projectKey);
  }

  private static void checkNotBuiltIn(QualityGateDto qualityGate) {
    checkArgument(!qualityGate.isBuiltIn(), "Operation forbidden for built-in Quality Gate '%s'", qualityGate.getName());
  }
}
