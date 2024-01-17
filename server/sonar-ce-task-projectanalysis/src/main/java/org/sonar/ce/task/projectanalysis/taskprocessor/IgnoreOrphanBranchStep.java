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
package org.sonar.ce.task.projectanalysis.taskprocessor;

import java.util.Optional;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;
import org.sonar.ce.task.CeTask;
import org.sonar.ce.task.step.ComputationStep;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.component.ComponentDto;

public final class IgnoreOrphanBranchStep implements ComputationStep {
  private static final Logger LOG = Loggers.get(IgnoreOrphanBranchStep.class);
  private final CeTask ceTask;
  private final DbClient dbClient;

  public IgnoreOrphanBranchStep(CeTask ceTask, DbClient dbClient) {
    this.ceTask = ceTask;
    this.dbClient = dbClient;
  }

  @Override
  public void execute(Context context) {
    String mainComponentUuid = ceTask.getMainComponent().orElseThrow(() -> new UnsupportedOperationException("main component not found in task")).getUuid();
    String componentUuid = ceTask.getComponent().orElseThrow(() -> new UnsupportedOperationException("component not found in task")).getUuid();

    try (DbSession dbSession = dbClient.openSession(false)) {
      Optional<ComponentDto> componentDto = dbClient.componentDao().selectByUuid(dbSession, mainComponentUuid);
      if(!componentDto.isPresent()){
        LOG.info("reindexation task has been trigger on an orphan branch. removing any exclude_from_purge flag, and skip the indexation");
        dbClient.branchDao().updateExcludeFromPurge(dbSession, componentUuid, false);
        dbClient.branchDao().updateNeedIssueSync(dbSession, componentUuid, false);
        dbSession.commit();
      }
    }
  }

  @Override
  public String getDescription() {
    return "Ignore orphan component";
  }
}
