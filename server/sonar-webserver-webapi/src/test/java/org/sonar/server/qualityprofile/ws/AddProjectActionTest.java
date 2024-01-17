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
package org.sonar.server.qualityprofile.ws;

import java.net.HttpURLConnection;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.api.resources.Languages;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.web.UserRole;
import org.sonar.db.DbClient;
import org.sonar.db.DbTester;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.project.ProjectDto;
import org.sonar.db.qualityprofile.QProfileDto;
import org.sonar.db.user.UserDto;
import org.sonar.server.component.TestComponentFinder;
import org.sonar.server.exceptions.ForbiddenException;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.exceptions.UnauthorizedException;
import org.sonar.server.language.LanguageTesting;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.ws.TestRequest;
import org.sonar.server.ws.TestResponse;
import org.sonar.server.ws.WsActionTester;

import static java.lang.String.format;
import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.db.permission.GlobalPermission.ADMINISTER_QUALITY_PROFILES;

public class AddProjectActionTest {

  private static final String LANGUAGE_1 = "xoo";
  private static final String LANGUAGE_2 = "foo";

  @Rule
  public DbTester db = DbTester.create();
  @Rule
  public ExpectedException expectedException = ExpectedException.none();
  @Rule
  public UserSessionRule userSession = UserSessionRule.standalone();

  private final DbClient dbClient = db.getDbClient();
  private final Languages languages = LanguageTesting.newLanguages(LANGUAGE_1, LANGUAGE_2);
  private final QProfileWsSupport wsSupport = new QProfileWsSupport(dbClient, userSession);
  private final AddProjectAction underTest = new AddProjectAction(dbClient, userSession, languages, TestComponentFinder.from(db), wsSupport);
  private final WsActionTester tester = new WsActionTester(underTest);

  @Test
  public void definition() {
    WebService.Action definition = tester.getDef();
    assertThat(definition.since()).isEqualTo("5.2");
    assertThat(definition.isPost()).isTrue();

    // parameters
    assertThat(definition.params()).extracting(WebService.Param::key)
      .containsExactlyInAnyOrder("qualityProfile", "project", "language");
    WebService.Param project = definition.param("project");
    assertThat(project.isRequired()).isTrue();
    WebService.Param languageParam = definition.param("language");
    assertThat(languageParam.possibleValues()).containsOnly(LANGUAGE_1, LANGUAGE_2);
    assertThat(languageParam.exampleValue()).isNull();
  }

  @Test
  public void add_project_on_profile() {
    logInAsProfileAdmin();
    ProjectDto project = db.components().insertPrivateProjectDto();
    QProfileDto profile = db.qualityProfiles().insert(qp -> qp.setLanguage("xoo"));

    TestResponse response = call(project, profile);
    assertThat(response.getStatus()).isEqualTo(HttpURLConnection.HTTP_NO_CONTENT);

    assertProjectIsAssociatedToProfile(project, profile);
  }

  @Test
  public void as_qprofile_editor() {
    UserDto user = db.users().insertUser();
    QProfileDto qualityProfile = db.qualityProfiles().insert(qp -> qp.setLanguage(LANGUAGE_1));
    db.qualityProfiles().addUserPermission(qualityProfile, user);
    ProjectDto project = db.components().insertPrivateProjectDto();
    userSession.logIn(user);

    call(project, qualityProfile);

    assertProjectIsAssociatedToProfile(project, qualityProfile);
  }

  @Test
  public void change_association() {
    logInAsProfileAdmin();

    ProjectDto project = db.components().insertPrivateProjectDto();
    // two profiles on same language
    QProfileDto profile1 = db.qualityProfiles().insert(p -> p.setLanguage(LANGUAGE_1));
    QProfileDto profile2 = db.qualityProfiles().insert(p -> p.setLanguage(LANGUAGE_1));
    db.qualityProfiles().associateWithProject(project, profile1);

    call(project, profile2);

    assertProjectIsNotAssociatedToProfile(project, profile1);
    assertProjectIsAssociatedToProfile(project, profile2);
  }

  @Test
  public void changing_association_does_not_change_other_language_associations() {
    logInAsProfileAdmin();
    ProjectDto project = db.components().insertPrivateProjectDto();
    QProfileDto profile1Language1 = db.qualityProfiles().insert(p -> p.setLanguage(LANGUAGE_1));
    QProfileDto profile2Language2 = db.qualityProfiles().insert(p -> p.setLanguage(LANGUAGE_2));
    QProfileDto profile3Language1 = db.qualityProfiles().insert(p -> p.setLanguage(LANGUAGE_1));
    db.qualityProfiles().associateWithProject(project, profile1Language1, profile2Language2);

    call(project, profile3Language1);

    assertProjectIsAssociatedToProfile(project, profile3Language1);
    assertProjectIsAssociatedToProfile(project, profile2Language2);
  }

  @Test
  public void project_administrator_can_change_profile() {
    ProjectDto project = db.components().insertPrivateProjectDto();
    QProfileDto profile = db.qualityProfiles().insert(qp -> qp.setLanguage("xoo"));
    userSession.logIn(db.users().insertUser()).addProjectPermission(UserRole.ADMIN, project);

    call(project, profile);

    assertProjectIsAssociatedToProfile(project, profile);
  }

  @Test
  public void throw_ForbiddenException_if_not_project_nor_global_administrator() {
    userSession.logIn(db.users().insertUser());
    ProjectDto project = db.components().insertPrivateProjectDto();
    QProfileDto profile = db.qualityProfiles().insert(qp -> qp.setLanguage("xoo"));

    expectedException.expect(ForbiddenException.class);
    expectedException.expectMessage("Insufficient privileges");

    call(project, profile);
  }

  @Test
  public void throw_UnauthorizedException_if_not_logged_in() {
    userSession.anonymous();
    ProjectDto project = db.components().insertPrivateProjectDto();
    QProfileDto profile = db.qualityProfiles().insert();

    expectedException.expect(UnauthorizedException.class);
    expectedException.expectMessage("Authentication is required");

    call(project, profile);
  }

  @Test
  public void throw_NotFoundException_if_project_does_not_exist() {
    logInAsProfileAdmin();
    QProfileDto profile = db.qualityProfiles().insert();

    expectedException.expect(NotFoundException.class);
    expectedException.expectMessage("Project 'unknown' not found");

    tester.newRequest()
      .setParam("project", "unknown")
      .setParam("profileKey", profile.getKee())
      .execute();
  }

  @Test
  public void throw_NotFoundException_if_profile_does_not_exist() {
    logInAsProfileAdmin();
    ComponentDto project = db.components().insertPrivateProject();

    expectedException.expect(NotFoundException.class);
    expectedException.expectMessage("Quality Profile for language 'xoo' and name 'unknown' does not exist");

    tester.newRequest()
      .setParam("project", project.getKey())
      .setParam("language", "xoo")
      .setParam("qualityProfile", "unknown")
      .execute();
  }

  @Test
  public void fail_when_using_branch_db_key() {
    ComponentDto project = db.components().insertPublicProject();
    userSession.logIn(db.users().insertUser()).addProjectPermission(UserRole.ADMIN, project);
    ComponentDto branch = db.components().insertProjectBranch(project);
    QProfileDto profile = db.qualityProfiles().insert();

    expectedException.expect(NotFoundException.class);
    expectedException.expectMessage(format("Project '%s' not found", branch.getDbKey()));

    tester.newRequest()
      .setParam("project", branch.getDbKey())
      .setParam("profileKey", profile.getKee())
      .execute();
  }

  private void assertProjectIsAssociatedToProfile(ProjectDto project, QProfileDto profile) {
    QProfileDto loaded = dbClient.qualityProfileDao().selectAssociatedToProjectAndLanguage(db.getSession(), project, profile.getLanguage());
    assertThat(loaded.getKee()).isEqualTo(profile.getKee());
  }

  private void assertProjectIsNotAssociatedToProfile(ProjectDto project, QProfileDto profile) {
    QProfileDto loaded = dbClient.qualityProfileDao().selectAssociatedToProjectAndLanguage(db.getSession(), project, profile.getLanguage());
    assertThat(loaded == null || !loaded.getKee().equals(profile.getKee())).isTrue();
  }

  private void logInAsProfileAdmin() {
    userSession.logIn(db.users().insertUser()).addPermission(ADMINISTER_QUALITY_PROFILES);
  }

  private TestResponse call(ProjectDto project, QProfileDto qualityProfile) {
    TestRequest request = tester.newRequest()
      .setParam("project", project.getKey())
      .setParam("language", qualityProfile.getLanguage())
      .setParam("qualityProfile", qualityProfile.getName());
    return request.execute();
  }
}
