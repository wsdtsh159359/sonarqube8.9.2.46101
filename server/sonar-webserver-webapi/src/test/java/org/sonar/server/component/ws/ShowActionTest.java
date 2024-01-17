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
package org.sonar.server.component.ws;

import java.util.Date;
import java.util.Optional;
import javax.annotation.Nullable;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.resources.Qualifiers;
import org.sonar.api.server.ws.Change;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.utils.System2;
import org.sonar.api.web.UserRole;
import org.sonar.db.DbTester;
import org.sonar.db.component.ComponentDto;
import org.sonar.server.component.TestComponentFinder;
import org.sonar.server.exceptions.ForbiddenException;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.issue.index.IssueIndexSyncProgressChecker;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.ws.TestRequest;
import org.sonar.server.ws.WsActionTester;
import org.sonarqube.ws.Components.Component;
import org.sonarqube.ws.Components.ShowWsResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;
import static org.sonar.api.utils.DateUtils.formatDateTime;
import static org.sonar.api.utils.DateUtils.parseDateTime;
import static org.sonar.api.web.UserRole.USER;
import static org.sonar.db.component.BranchType.BRANCH;
import static org.sonar.db.component.BranchType.PULL_REQUEST;
import static org.sonar.db.component.ComponentTesting.newDirectory;
import static org.sonar.db.component.ComponentTesting.newFileDto;
import static org.sonar.db.component.ComponentTesting.newModuleDto;
import static org.sonar.db.component.ComponentTesting.newPrivateProjectDto;
import static org.sonar.db.component.SnapshotTesting.newAnalysis;
import static org.sonar.test.JsonAssert.assertJson;
import static org.sonarqube.ws.client.component.ComponentsWsParameters.PARAM_BRANCH;
import static org.sonarqube.ws.client.component.ComponentsWsParameters.PARAM_COMPONENT;
import static org.sonarqube.ws.client.component.ComponentsWsParameters.PARAM_PULL_REQUEST;

public class ShowActionTest {
  @Rule
  public final UserSessionRule userSession = UserSessionRule.standalone();
  @Rule
  public final DbTester db = DbTester.create(System2.INSTANCE);

  private final WsActionTester ws = new WsActionTester(new ShowAction(userSession, db.getDbClient(), TestComponentFinder.from(db),
    new IssueIndexSyncProgressChecker(db.getDbClient())));

  @Test
  public void verify_definition() {
    WebService.Action action = ws.getDef();

    assertThat(action.since()).isEqualTo("5.4");
    assertThat(action.description()).isNotNull();
    assertThat(action.responseExample()).isNotNull();
    assertThat(action.changelog()).extracting(Change::getVersion, Change::getDescription).containsExactlyInAnyOrder(
      tuple("7.6", "The use of module keys in parameter 'component' is deprecated"));
    assertThat(action.params()).extracting(WebService.Param::key).containsExactlyInAnyOrder("component", "branch", "pullRequest");

    WebService.Param component = action.param(PARAM_COMPONENT);
    assertThat(component.isRequired()).isTrue();
    assertThat(component.description()).isNotNull();
    assertThat(component.exampleValue()).isNotNull();

    WebService.Param branch = action.param(PARAM_BRANCH);
    assertThat(branch.isInternal()).isFalse();
    assertThat(branch.isRequired()).isFalse();
    assertThat(branch.since()).isEqualTo("6.6");

    WebService.Param pullRequest = action.param(PARAM_PULL_REQUEST);
    assertThat(pullRequest.isInternal()).isFalse();
    assertThat(pullRequest.isRequired()).isFalse();
    assertThat(pullRequest.since()).isEqualTo("7.1");
  }

  @Test
  public void json_example() {
    userSession.logIn().setRoot();
    insertJsonExampleComponentsAndSnapshots();

    String response = ws.newRequest()
      .setParam("component", "com.sonarsource:java-markdown:src/main/java/com/sonarsource/markdown/impl/Rule.java")
      .execute()
      .getInput();

    assertJson(response).isSimilarTo(getClass().getResource("show-example.json"));
  }

  @Test
  public void tags_displayed_only_for_project() {
    userSession.logIn().setRoot();
    insertJsonExampleComponentsAndSnapshots();

    String response = ws.newRequest()
      .setParam(PARAM_COMPONENT, "com.sonarsource:java-markdown:src/main/java/com/sonarsource/markdown/impl/Rule.java")
      .execute()
      .getInput();

    assertThat(response).containsOnlyOnce("\"tags\"");
  }

  @Test
  public void show_with_browse_permission() {
    ComponentDto project = newPrivateProjectDto("project-uuid");
    db.components().insertProjectAndSnapshot(project);
    userSession.logIn().addProjectPermission(USER, project);

    ShowWsResponse response = newRequest(project.getDbKey());

    assertThat(response.getComponent().getKey()).isEqualTo(project.getDbKey());
  }

  @Test
  public void show_with_ancestors_when_not_project() {
    ComponentDto project = db.components().insertPrivateProject();
    ComponentDto module = db.components().insertComponent(newModuleDto(project));
    ComponentDto directory = db.components().insertComponent(newDirectory(module, "dir"));
    ComponentDto file = db.components().insertComponent(newFileDto(directory));
    userSession.addProjectPermission(USER, project);

    ShowWsResponse response = newRequest(file.getDbKey());

    assertThat(response.getComponent().getKey()).isEqualTo(file.getDbKey());
    assertThat(response.getAncestorsList()).extracting(Component::getKey).containsOnly(directory.getDbKey(), module.getDbKey(), project.getDbKey());
  }

  @Test
  public void show_without_ancestors_when_project() {
    ComponentDto project = db.components().insertPrivateProject();
    db.components().insertComponent(newModuleDto(project));
    userSession.addProjectPermission(USER, project);

    ShowWsResponse response = newRequest(project.getDbKey());

    assertThat(response.getComponent().getKey()).isEqualTo(project.getDbKey());
    assertThat(response.getAncestorsList()).isEmpty();
  }

  @Test
  public void show_with_last_analysis_date() {
    ComponentDto project = db.components().insertPrivateProject();
    db.components().insertSnapshots(
      newAnalysis(project).setCreatedAt(1_000_000_000L).setLast(false),
      newAnalysis(project).setCreatedAt(2_000_000_000L).setLast(false),
      newAnalysis(project).setCreatedAt(3_000_000_000L).setLast(true));
    userSession.addProjectPermission(USER, project);

    ShowWsResponse response = newRequest(project.getDbKey());

    assertThat(response.getComponent().getAnalysisDate()).isNotEmpty().isEqualTo(formatDateTime(new Date(3_000_000_000L)));
  }

  @Test
  public void show_with_new_code_period_date() {
    ComponentDto project = db.components().insertPrivateProject();
    db.components().insertSnapshots(
      newAnalysis(project).setPeriodDate(1_000_000_000L).setLast(false),
      newAnalysis(project).setPeriodDate(2_000_000_000L).setLast(false),
      newAnalysis(project).setPeriodDate(3_000_000_000L).setLast(true));

    userSession.addProjectPermission(USER, project);

    ShowWsResponse response = newRequest(project.getDbKey());

    assertThat(response.getComponent().getLeakPeriodDate()).isNotEmpty().isEqualTo(formatDateTime(new Date(3_000_000_000L)));
  }

  @Test
  public void show_with_ancestors_and_analysis_date() {
    ComponentDto project = db.components().insertPrivateProject();
    db.components().insertSnapshot(newAnalysis(project).setCreatedAt(3_000_000_000L).setLast(true));
    ComponentDto module = db.components().insertComponent(newModuleDto(project));
    ComponentDto directory = db.components().insertComponent(newDirectory(module, "dir"));
    ComponentDto file = db.components().insertComponent(newFileDto(directory));
    userSession.addProjectPermission(USER, project);

    ShowWsResponse response = newRequest(file.getDbKey());

    String expectedDate = formatDateTime(new Date(3_000_000_000L));
    assertThat(response.getAncestorsList()).extracting(Component::getAnalysisDate)
      .containsOnly(expectedDate, expectedDate, expectedDate);
  }

  @Test
  public void should_return_visibility_for_private_project() {
    userSession.logIn().setRoot();
    ComponentDto privateProject = db.components().insertPrivateProject();

    ShowWsResponse result = newRequest(privateProject.getDbKey());
    assertThat(result.getComponent().hasVisibility()).isTrue();
    assertThat(result.getComponent().getVisibility()).isEqualTo("private");
  }

  @Test
  public void should_return_visibility_for_public_project() {
    userSession.logIn().setRoot();
    ComponentDto publicProject = db.components().insertPublicProject();

    ShowWsResponse result = newRequest(publicProject.getDbKey());
    assertThat(result.getComponent().hasVisibility()).isTrue();
    assertThat(result.getComponent().getVisibility()).isEqualTo("public");
  }

  @Test
  public void should_return_visibility_for_portfolio() {
    userSession.logIn().setRoot();
    ComponentDto view = db.components().insertPrivatePortfolio();

    ShowWsResponse result = newRequest(view.getDbKey());
    assertThat(result.getComponent().hasVisibility()).isTrue();
  }

  @Test
  public void should_not_return_visibility_for_module() {
    userSession.logIn().setRoot();
    ComponentDto privateProject = db.components().insertPrivateProject();
    ComponentDto module = db.components().insertComponent(newModuleDto(privateProject));

    ShowWsResponse result = newRequest(module.getDbKey());
    assertThat(result.getComponent().hasVisibility()).isFalse();
  }

  @Test
  public void display_version() {
    ComponentDto project = db.components().insertPrivateProject();
    ComponentDto module = db.components().insertComponent(newModuleDto(project));
    ComponentDto directory = db.components().insertComponent(newDirectory(module, "dir"));
    ComponentDto file = db.components().insertComponent(newFileDto(directory));
    db.components().insertSnapshot(project, s -> s.setProjectVersion("1.1"));
    userSession.addProjectPermission(USER, project);

    ShowWsResponse response = newRequest(file.getDbKey());

    assertThat(response.getComponent().getVersion()).isEqualTo("1.1");
    assertThat(response.getAncestorsList())
      .extracting(Component::getVersion)
      .containsOnly("1.1");
  }

  @Test
  public void branch() {
    ComponentDto project = db.components().insertPrivateProject();
    userSession.addProjectPermission(UserRole.USER, project);
    String branchKey = "my_branch";
    ComponentDto branch = db.components().insertProjectBranch(project, b -> b.setKey(branchKey));
    ComponentDto module = db.components().insertComponent(newModuleDto(branch));
    ComponentDto directory = db.components().insertComponent(newDirectory(module, "dir"));
    ComponentDto file = db.components().insertComponent(newFileDto(directory));
    db.components().insertSnapshot(branch, s -> s.setProjectVersion("1.1"));

    ShowWsResponse response = ws.newRequest()
      .setParam(PARAM_COMPONENT, file.getKey())
      .setParam(PARAM_BRANCH, branchKey)
      .executeProtobuf(ShowWsResponse.class);

    assertThat(response.getComponent())
      .extracting(Component::getKey, Component::getBranch, Component::getVersion)
      .containsExactlyInAnyOrder(file.getKey(), branchKey, "1.1");
    assertThat(response.getAncestorsList()).extracting(Component::getKey, Component::getBranch, Component::getVersion)
      .containsExactlyInAnyOrder(
        tuple(directory.getKey(), branchKey, "1.1"),
        tuple(module.getKey(), branchKey, "1.1"),
        tuple(branch.getKey(), branchKey, "1.1"));
  }

  @Test
  public void pull_request() {
    ComponentDto project = db.components().insertPrivateProject();
    userSession.addProjectPermission(UserRole.USER, project);
    String pullRequest = "pr-1234";
    ComponentDto branch = db.components().insertProjectBranch(project, b -> b.setKey(pullRequest).setBranchType(PULL_REQUEST));
    ComponentDto module = db.components().insertComponent(newModuleDto(branch));
    ComponentDto directory = db.components().insertComponent(newDirectory(module, "dir"));
    ComponentDto file = db.components().insertComponent(newFileDto(directory));
    db.components().insertSnapshot(branch, s -> s.setProjectVersion("1.1"));

    ShowWsResponse response = ws.newRequest()
      .setParam(PARAM_COMPONENT, file.getKey())
      .setParam(PARAM_PULL_REQUEST, pullRequest)
      .executeProtobuf(ShowWsResponse.class);

    assertThat(response.getComponent())
      .extracting(Component::getKey, Component::getPullRequest, Component::getVersion)
      .containsExactlyInAnyOrder(file.getKey(), pullRequest, "1.1");
    assertThat(response.getAncestorsList()).extracting(Component::getKey, Component::getPullRequest, Component::getVersion)
      .containsExactlyInAnyOrder(
        tuple(directory.getKey(), pullRequest, "1.1"),
        tuple(module.getKey(), pullRequest, "1.1"),
        tuple(branch.getKey(), pullRequest, "1.1"));
  }

  @Test
  public void verify_need_issue_sync_pr() {
    ComponentDto portfolio1 = db.components().insertPublicPortfolio();
    ComponentDto portfolio2 = db.components().insertPublicPortfolio();
    ComponentDto subview = db.components().insertSubView(portfolio1);

    ComponentDto project1 = db.components().insertPrivateProject();
    ComponentDto branch1 = db.components().insertProjectBranch(project1, b -> b.setBranchType(PULL_REQUEST).setNeedIssueSync(true));
    ComponentDto module = db.components().insertComponent(newModuleDto(branch1));
    ComponentDto directory = db.components().insertComponent(newDirectory(module, "dir"));
    ComponentDto file = db.components().insertComponent(newFileDto(directory));

    ComponentDto project2 = db.components().insertPrivateProject();
    ComponentDto branch2 = db.components().insertProjectBranch(project2, b -> b.setBranchType(BRANCH).setNeedIssueSync(true));
    ComponentDto branch3 = db.components().insertProjectBranch(project2, b -> b.setBranchType(BRANCH).setNeedIssueSync(false));

    ComponentDto project3 = db.components().insertPrivateProject();
    ComponentDto branch4 = db.components().insertProjectBranch(project3, b -> b.setBranchType(PULL_REQUEST).setNeedIssueSync(false));
    ComponentDto moduleOfBranch4 = db.components().insertComponent(newModuleDto(branch4));
    ComponentDto directoryOfBranch4 = db.components().insertComponent(newDirectory(moduleOfBranch4, "dir"));
    ComponentDto fileOfBranch4 = db.components().insertComponent(newFileDto(directoryOfBranch4));
    ComponentDto branch5 = db.components().insertProjectBranch(project3, b -> b.setBranchType(BRANCH).setNeedIssueSync(false));

    userSession.addProjectPermission(UserRole.USER, project1, project2, project3);
    userSession.registerComponents(portfolio1, portfolio2, subview, project1, project2, project3);

    // for portfolios, sub-views need issue sync flag is set to true if any project need sync
    assertNeedIssueSyncEqual(null, null, portfolio1, true);
    assertNeedIssueSyncEqual(null, null, subview, true);
    assertNeedIssueSyncEqual(null, null, portfolio2, true);

    // if branch need sync it is propagated to other components
    assertNeedIssueSyncEqual(null, null, project1, true);
    assertNeedIssueSyncEqual(branch1.getPullRequest(), null, branch1, true);
    assertNeedIssueSyncEqual(branch1.getPullRequest(), null, module, true);
    assertNeedIssueSyncEqual(branch1.getPullRequest(), null, directory, true);
    assertNeedIssueSyncEqual(branch1.getPullRequest(), null, file, true);

    assertNeedIssueSyncEqual(null, null, project2, true);
    assertNeedIssueSyncEqual(null, branch2.getBranch(), branch2, true);
    assertNeedIssueSyncEqual(null, branch3.getBranch(), branch3, true);

    // if all branches are synced, need issue sync on project is is set to false
    assertNeedIssueSyncEqual(null, null, project3, false);
    assertNeedIssueSyncEqual(branch4.getPullRequest(), null, branch4, false);
    assertNeedIssueSyncEqual(branch4.getPullRequest(), null, moduleOfBranch4, false);
    assertNeedIssueSyncEqual(branch4.getPullRequest(), null, directoryOfBranch4, false);
    assertNeedIssueSyncEqual(branch4.getPullRequest(), null, fileOfBranch4, false);
    assertNeedIssueSyncEqual(null, branch5.getBranch(), branch5, false);
  }

  private void assertNeedIssueSyncEqual(@Nullable String pullRequest, @Nullable String branch, ComponentDto component, boolean needIssueSync) {
    TestRequest testRequest = ws.newRequest()
      .setParam(PARAM_COMPONENT, component.getKey());

    Optional.ofNullable(pullRequest).ifPresent(pr -> testRequest.setParam(PARAM_PULL_REQUEST, pr));
    Optional.ofNullable(branch).ifPresent(br -> testRequest.setParam(PARAM_BRANCH, br));

    ShowWsResponse response = testRequest.executeProtobuf(ShowWsResponse.class);

    assertThat(response.getComponent())
      .extracting(Component::getNeedIssueSync)
      .isEqualTo(needIssueSync);
  }

  @Test
  public void throw_ForbiddenException_if_user_doesnt_have_browse_permission_on_project() {
    userSession.logIn();

    ComponentDto componentDto = newPrivateProjectDto("project-uuid");
    db.components().insertProjectAndSnapshot(componentDto);

    String componentDtoDbKey = componentDto.getDbKey();
    assertThatThrownBy(() -> newRequest(componentDtoDbKey))
      .isInstanceOf(ForbiddenException.class);
  }

  @Test
  public void fail_if_component_does_not_exist() {
    assertThatThrownBy(() -> newRequest("unknown-key"))
      .isInstanceOf(NotFoundException.class)
      .hasMessage("Component key 'unknown-key' not found");
  }

  @Test
  public void fail_if_component_is_removed() {
    userSession.logIn().setRoot();
    ComponentDto project = db.components().insertComponent(newPrivateProjectDto());
    db.components().insertComponent(newFileDto(project).setDbKey("file-key").setEnabled(false));

    assertThatThrownBy(() -> newRequest("file-key"))
      .isInstanceOf(NotFoundException.class)
      .hasMessage("Component key 'file-key' not found");
  }

  @Test
  public void fail_if_branch_does_not_exist() {
    ComponentDto project = db.components().insertPrivateProject();
    ComponentDto file = db.components().insertComponent(newFileDto(project));
    userSession.addProjectPermission(UserRole.USER, project);
    db.components().insertProjectBranch(project, b -> b.setKey("my_branch"));

    TestRequest request = ws.newRequest()
      .setParam(PARAM_COMPONENT, file.getKey())
      .setParam(PARAM_BRANCH, "another_branch");

    assertThatThrownBy(request::execute)
      .isInstanceOf(NotFoundException.class)
      .hasMessage(String.format("Component '%s' on branch '%s' not found", file.getKey(), "another_branch"));
  }

  @Test
  public void fail_when_using_branch_db_key() {
    ComponentDto project = db.components().insertPrivateProject();
    userSession.addProjectPermission(UserRole.USER, project);
    ComponentDto branch = db.components().insertProjectBranch(project);

    TestRequest request = ws.newRequest()
      .setParam(PARAM_COMPONENT, branch.getDbKey());
    assertThatThrownBy(() -> request.executeProtobuf(ShowWsResponse.class))
      .isInstanceOf(NotFoundException.class)
      .hasMessage(String.format("Component key '%s' not found", branch.getDbKey()));
  }

  private ShowWsResponse newRequest(@Nullable String key) {
    TestRequest request = ws.newRequest();
    if (key != null) {
      request.setParam(PARAM_COMPONENT, key);
    }
    return request.executeProtobuf(ShowWsResponse.class);
  }

  private void insertJsonExampleComponentsAndSnapshots() {
    ComponentDto project = db.components().insertPrivateProject(c -> c.setUuid("AVIF98jgA3Ax6PH2efOW")
      .setProjectUuid("AVIF98jgA3Ax6PH2efOW")
      .setDbKey("com.sonarsource:java-markdown")
      .setName("Java Markdown")
      .setDescription("Java Markdown Project")
      .setQualifier(Qualifiers.PROJECT),
      p -> p.setTagsString("language, plugin"));
    db.components().insertSnapshot(project, snapshot -> snapshot
      .setProjectVersion("1.1")
      .setCreatedAt(parseDateTime("2017-03-01T11:39:03+0100").getTime())
      .setPeriodDate(parseDateTime("2017-01-01T11:39:03+0100").getTime()));
    ComponentDto directory = newDirectory(project, "AVIF-FfgA3Ax6PH2efPF", "src/main/java/com/sonarsource/markdown/impl")
      .setDbKey("com.sonarsource:java-markdown:src/main/java/com/sonarsource/markdown/impl")
      .setName("src/main/java/com/sonarsource/markdown/impl")
      .setQualifier(Qualifiers.DIRECTORY);
    db.components().insertComponent(directory);
    db.components().insertComponent(
      newFileDto(directory, directory, "AVIF-FffA3Ax6PH2efPD")
        .setDbKey("com.sonarsource:java-markdown:src/main/java/com/sonarsource/markdown/impl/Rule.java")
        .setName("Rule.java")
        .setPath("src/main/java/com/sonarsource/markdown/impl/Rule.java")
        .setLanguage("java")
        .setQualifier(Qualifiers.FILE));
  }
}
