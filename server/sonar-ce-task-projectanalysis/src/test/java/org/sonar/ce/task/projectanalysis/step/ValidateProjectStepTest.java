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
package org.sonar.ce.task.projectanalysis.step;

import java.util.Date;
import javax.annotation.Nullable;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.api.impl.utils.TestSystem2;
import org.sonar.api.utils.DateUtils;
import org.sonar.api.utils.MessageException;
import org.sonar.api.utils.System2;
import org.sonar.ce.task.log.CeTaskMessages;
import org.sonar.ce.task.projectanalysis.analysis.AnalysisMetadataHolderRule;
import org.sonar.ce.task.projectanalysis.analysis.Branch;
import org.sonar.ce.task.projectanalysis.component.Component;
import org.sonar.ce.task.projectanalysis.component.DefaultBranchImpl;
import org.sonar.ce.task.projectanalysis.component.ReportComponent;
import org.sonar.ce.task.projectanalysis.component.TreeRootHolderRule;
import org.sonar.ce.task.step.TestComputationStepContext;
import org.sonar.db.DbClient;
import org.sonar.db.DbTester;
import org.sonar.db.component.BranchType;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.component.ComponentTesting;
import org.sonar.db.component.SnapshotTesting;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

public class ValidateProjectStepTest {

  static long PAST_ANALYSIS_TIME = 1_420_088_400_000L; // 2015-01-01
  static long DEFAULT_ANALYSIS_TIME = 1_433_131_200_000L; // 2015-06-01
  static long NOW = 1_500_000_000_000L;

  static final String PROJECT_KEY = "PROJECT_KEY";
  static final Branch DEFAULT_BRANCH = new DefaultBranchImpl();

  @Rule
  public DbTester db = DbTester.create(System2.INSTANCE);
  @Rule
  public ExpectedException thrown = ExpectedException.none();
  @Rule
  public TreeRootHolderRule treeRootHolder = new TreeRootHolderRule();
  @Rule
  public AnalysisMetadataHolderRule analysisMetadataHolder = new AnalysisMetadataHolderRule()
    .setAnalysisDate(new Date(DEFAULT_ANALYSIS_TIME))
    .setBranch(DEFAULT_BRANCH);
  private System2 system2 = new TestSystem2().setNow(NOW);

  private CeTaskMessages taskMessages = mock(CeTaskMessages.class);
  private DbClient dbClient = db.getDbClient();

  private ValidateProjectStep underTest = new ValidateProjectStep(dbClient, treeRootHolder, analysisMetadataHolder, taskMessages, system2);

  @Test
  public void dont_fail_for_long_forked_from_master_with_modules() {
    ComponentDto masterProject = db.components().insertPublicProject();
    dbClient.componentDao().insert(db.getSession(), ComponentTesting.newModuleDto(masterProject));
    setBranch(BranchType.BRANCH, masterProject.uuid());
    db.getSession().commit();

    treeRootHolder.setRoot(ReportComponent.builder(Component.Type.PROJECT, 1).setUuid("DEFG")
      .setKey("branch")
      .build());

    underTest.execute(new TestComputationStepContext());
    verifyNoInteractions(taskMessages);
  }

  @Test
  public void not_fail_if_analysis_date_is_after_last_analysis() {
    ComponentDto project = ComponentTesting.newPrivateProjectDto("ABCD").setDbKey(PROJECT_KEY);
    db.components().insertComponent(project);
    dbClient.snapshotDao().insert(db.getSession(), SnapshotTesting.newAnalysis(project).setCreatedAt(PAST_ANALYSIS_TIME));
    db.getSession().commit();

    treeRootHolder.setRoot(ReportComponent.builder(Component.Type.PROJECT, 1).setUuid("ABCD").setKey(PROJECT_KEY).build());

    underTest.execute(new TestComputationStepContext());
  }

  @Test
  public void fail_if_pr_is_targeting_branch_with_modules() {
    ComponentDto masterProject = db.components().insertPublicProject();
    ComponentDto mergeBranch = db.components().insertProjectBranch(masterProject, b -> b.setKey("mergeBranch"));
    dbClient.componentDao().insert(db.getSession(), ComponentTesting.newModuleDto(mergeBranch));
    setBranch(BranchType.PULL_REQUEST, mergeBranch.uuid());
    db.getSession().commit();

    treeRootHolder.setRoot(ReportComponent.builder(Component.Type.PROJECT, 1).setUuid("DEFG")
      .setKey("branch")
      .build());

    thrown.expect(MessageException.class);
    thrown.expectMessage("Due to an upgrade, you need first to re-analyze the target branch 'mergeBranch' before analyzing this pull request.");
    underTest.execute(new TestComputationStepContext());
  }

  @Test
  public void fail_if_analysis_date_is_before_last_analysis() {
    analysisMetadataHolder.setAnalysisDate(DateUtils.parseDate("2015-01-01"));

    ComponentDto project = ComponentTesting.newPrivateProjectDto("ABCD").setDbKey(PROJECT_KEY);
    db.components().insertComponent(project);
    dbClient.snapshotDao().insert(db.getSession(), SnapshotTesting.newAnalysis(project).setCreatedAt(1433131200000L)); // 2015-06-01
    db.getSession().commit();

    treeRootHolder.setRoot(ReportComponent.builder(Component.Type.PROJECT, 1).setUuid("ABCD").setKey(PROJECT_KEY).build());

    thrown.expect(MessageException.class);
    thrown.expectMessage("Validation of project failed:");
    thrown.expectMessage("Date of analysis cannot be older than the date of the last known analysis on this project. Value: ");
    thrown.expectMessage("Latest analysis: ");

    underTest.execute(new TestComputationStepContext());
  }

  @Test
  public void add_warning_when_project_key_is_invalid() {
    ComponentDto project = db.components().insertPrivateProject(p -> p.setDbKey("inv$lid!"));
    db.components().insertSnapshot(project, a -> a.setCreatedAt(PAST_ANALYSIS_TIME));
    treeRootHolder.setRoot(ReportComponent.builder(Component.Type.PROJECT, 1)
      .setUuid(project.uuid())
      .setKey(project.getKey())
      .build());

    underTest.execute(new TestComputationStepContext());

    verify(taskMessages, times(1))
      .add(new CeTaskMessages.Message(
        "The project key ‘inv$lid!’ contains invalid characters. Allowed characters are alphanumeric, '-', '_', '.' and ':', with at least one non-digit. " +
          "You should update the project key with the expected format.",
        NOW));
  }

  private void setBranch(BranchType type, @Nullable String mergeBranchUuid) {
    Branch branch = mock(Branch.class);
    when(branch.getType()).thenReturn(type);
    when(branch.getReferenceBranchUuid()).thenReturn(mergeBranchUuid);
    analysisMetadataHolder.setBranch(branch);
  }
}
