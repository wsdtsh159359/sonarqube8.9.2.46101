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

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.utils.System2;
import org.sonar.api.web.UserRole;
import org.sonar.db.DbClient;
import org.sonar.db.DbTester;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.project.ProjectDto;
import org.sonar.db.qualitygate.QualityGateDto;
import org.sonar.server.component.TestComponentFinder;
import org.sonar.server.exceptions.ForbiddenException;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.qualitygate.QualityGateFinder;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.ws.WsActionTester;
import org.sonarqube.ws.Qualitygates;
import org.sonarqube.ws.Qualitygates.GetByProjectResponse;

import static java.lang.String.format;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.sonar.test.JsonAssert.assertJson;

public class GetByProjectActionTest {
  @Rule
  public UserSessionRule userSession = UserSessionRule.standalone();
  @Rule
  public ExpectedException expectedException = ExpectedException.none();
  @Rule
  public DbTester db = DbTester.create(System2.INSTANCE);

  private final DbClient dbClient = db.getDbClient();

  private final WsActionTester ws = new WsActionTester(
    new GetByProjectAction(userSession, dbClient, TestComponentFinder.from(db), new QualityGateFinder(dbClient)));

  @Test
  public void definition() {
    WebService.Action action = ws.getDef();
    assertThat(action.description()).isNotEmpty();
    assertThat(action.isInternal()).isFalse();
    assertThat(action.since()).isEqualTo("6.1");
    assertThat(action.changelog()).isNotEmpty();
    assertThat(action.params())
      .extracting(WebService.Param::key, WebService.Param::isRequired)
      .containsExactlyInAnyOrder(
        tuple("project", true));
  }

  @Test
  public void json_example() {
    ProjectDto project = db.components().insertPrivateProjectDto();
    QualityGateDto qualityGate = db.qualityGates().insertQualityGate(qg -> qg.setName("My team QG"));
    db.qualityGates().associateProjectToQualityGate(project, qualityGate);
    logInAsProjectUser(project);

    String result = ws.newRequest()
      .setParam("project", project.getKey())
      .execute()
      .getInput();

    assertJson(result)
      .ignoreFields("id")
      .isSimilarTo(getClass().getResource("get_by_project-example.json"));
  }

  @Test
  public void default_quality_gate() {
    ProjectDto project = db.components().insertPrivateProjectDto();
    QualityGateDto dbQualityGate = db.qualityGates().insertQualityGate();
    db.qualityGates().setDefaultQualityGate(dbQualityGate);
    logInAsProjectUser(project);

    GetByProjectResponse result = ws.newRequest()
      .setParam("project", project.getKey())
      .executeProtobuf(GetByProjectResponse.class);

    Qualitygates.QualityGate qualityGate = result.getQualityGate();
    assertThat(qualityGate.getId()).isEqualTo(dbQualityGate.getUuid());
    assertThat(qualityGate.getName()).isEqualTo(dbQualityGate.getName());
    assertThat(qualityGate.getDefault()).isTrue();
  }

  @Test
  public void project_quality_gate_over_default() {
    ProjectDto project = db.components().insertPrivateProjectDto();
    QualityGateDto defaultDbQualityGate = db.qualityGates().insertQualityGate();
    db.qualityGates().setDefaultQualityGate(defaultDbQualityGate);
    QualityGateDto qualityGate = db.qualityGates().insertQualityGate();
    db.qualityGates().associateProjectToQualityGate(project, qualityGate);
    logInAsProjectUser(project);

    GetByProjectResponse result = ws.newRequest()
      .setParam("project", project.getKey())
      .executeProtobuf(GetByProjectResponse.class);

    Qualitygates.QualityGate reloaded = result.getQualityGate();
    assertThat(reloaded.getName()).isEqualTo(reloaded.getName());
    assertThat(reloaded.getDefault()).isFalse();
  }

  @Test
  public void get_by_project_key() {
    ProjectDto project = db.components().insertPrivateProjectDto();

    QualityGateDto qualityGate = db.qualityGates().insertQualityGate(qg -> qg.setName("My team QG"));
    db.qualityGates().associateProjectToQualityGate(project, qualityGate);
    logInAsProjectUser(project);

    GetByProjectResponse result = ws.newRequest()
      .setParam("project", project.getKey())
      .executeProtobuf(GetByProjectResponse.class);

    assertThat(result.getQualityGate().getName()).isEqualTo(qualityGate.getName());
  }

  @Test
  public void get_with_project_admin_permission() {
    ComponentDto project = db.components().insertPrivateProject();
    QualityGateDto qualityGate = db.qualityGates().insertQualityGate();
    db.qualityGates().setDefaultQualityGate(qualityGate);
    userSession.logIn().addProjectPermission(UserRole.ADMIN, project);

    GetByProjectResponse result = ws.newRequest()
      .setParam("project", project.getKey())
      .executeProtobuf(GetByProjectResponse.class);

    assertThat(result.getQualityGate().getName()).isEqualTo(qualityGate.getName());
  }

  @Test
  public void get_with_project_user_permission() {
    ComponentDto project = db.components().insertPrivateProject();
    QualityGateDto qualityGate = db.qualityGates().insertQualityGate();
    db.qualityGates().setDefaultQualityGate(qualityGate);
    userSession.logIn().addProjectPermission(UserRole.USER, project);

    GetByProjectResponse result = ws.newRequest()
      .setParam("project", project.getKey())
      .executeProtobuf(GetByProjectResponse.class);

    assertThat(result.getQualityGate().getName()).isEqualTo(qualityGate.getName());
  }

  @Test
  public void fail_when_insufficient_permission() {
    QualityGateDto dbQualityGate = db.qualityGates().insertQualityGate();
    db.qualityGates().setDefaultQualityGate(dbQualityGate);
    ComponentDto project = db.components().insertPrivateProject();
    userSession.logIn();

    expectedException.expect(ForbiddenException.class);

    ws.newRequest()
      .setParam("project", project.getKey())
      .execute();
  }

  @Test
  public void fail_when_project_does_not_exist() {
    expectedException.expect(NotFoundException.class);

    ws.newRequest()
      .setParam("project", "Unknown")
      .execute();
  }

  @Test
  public void fail_when_missing_project_parameter() {
    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("The 'project' parameter is missing");

    ws.newRequest().execute();
  }

  @Test
  public void fail_when_using_branch_db_key() {
    ComponentDto project = db.components().insertPublicProject();
    userSession.logIn().addProjectPermission(UserRole.ADMIN, project);
    ComponentDto branch = db.components().insertProjectBranch(project);

    expectedException.expect(NotFoundException.class);
    expectedException.expectMessage(format("Project '%s' not found", branch.getDbKey()));

    ws.newRequest()
      .setParam("project", branch.getDbKey())
      .execute();
  }

  private void logInAsProjectUser(ProjectDto project) {
    userSession.logIn().addProjectPermission(UserRole.USER, project);
  }
}
