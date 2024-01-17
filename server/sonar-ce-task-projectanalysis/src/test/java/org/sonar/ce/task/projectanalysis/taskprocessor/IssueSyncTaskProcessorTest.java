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

import java.util.List;
import org.assertj.core.api.Assertions;
import org.junit.Test;
import org.mockito.Mockito;
import org.sonar.ce.task.CeTask;
import org.sonar.ce.task.container.TaskContainer;
import org.sonar.ce.task.step.ComputationStep;
import org.sonar.core.platform.ComponentContainer;

import static org.mockito.ArgumentMatchers.any;
import static org.sonar.ce.task.projectanalysis.taskprocessor.IssueSyncTaskProcessor.*;
import static org.sonar.db.ce.CeTaskTypes.BRANCH_ISSUE_SYNC;

public class IssueSyncTaskProcessorTest {

  private ComponentContainer ceEngineContainer = Mockito.mock(ComponentContainer.class);

  private IssueSyncTaskProcessor underTest = new IssueSyncTaskProcessor(ceEngineContainer);
  private TaskContainer container = Mockito.spy(TaskContainer.class);

  @Test
  public void getHandledCeTaskTypes() {
    Assertions.assertThat(underTest.getHandledCeTaskTypes()).containsExactly(BRANCH_ISSUE_SYNC);
  }

  @Test
  public void newContainerPopulator() {
    CeTask task = new CeTask.Builder()
      .setUuid("TASK_UUID")
      .setType("Type")
      .build();

    IssueSyncTaskProcessor.newContainerPopulator(task).populateContainer(container);
    Mockito.verify(container, Mockito.times(5)).add(any());
  }

  @Test
  public void orderedStepClasses(){
    SyncComputationSteps syncComputationSteps = new SyncComputationSteps(null);

    List<Class<? extends ComputationStep>> steps = syncComputationSteps.orderedStepClasses();

    Assertions.assertThat(steps).containsExactly(IgnoreOrphanBranchStep.class, IndexIssuesStep.class);
  }

}
