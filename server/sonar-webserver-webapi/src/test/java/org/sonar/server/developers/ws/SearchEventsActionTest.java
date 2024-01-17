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
package org.sonar.server.developers.ws;

import java.util.Date;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.api.platform.Server;
import org.sonar.api.rules.RuleType;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.server.ws.WebService.Param;
import org.sonar.api.web.UserRole;
import org.sonar.db.DbTester;
import org.sonar.db.ce.CeActivityDto;
import org.sonar.db.ce.CeQueueDto;
import org.sonar.db.ce.CeTaskTypes;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.component.SnapshotDto;
import org.sonar.db.event.EventDto;
import org.sonar.server.es.EsTester;
import org.sonar.server.exceptions.UnauthorizedException;
import org.sonar.server.issue.index.IssueIndex;
import org.sonar.server.issue.index.IssueIndexSyncProgressChecker;
import org.sonar.server.issue.index.IssueIndexer;
import org.sonar.server.issue.index.IssueIteratorFactory;
import org.sonar.server.projectanalysis.ws.EventCategory;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.ws.KeyExamples;
import org.sonar.server.ws.WsActionTester;
import org.sonarqube.ws.Developers.SearchEventsWsResponse;
import org.sonarqube.ws.Developers.SearchEventsWsResponse.Event;

import static java.lang.String.format;
import static org.apache.commons.lang.RandomStringUtils.randomAlphanumeric;
import static org.apache.commons.lang.math.RandomUtils.nextInt;
import static org.apache.commons.lang.math.RandomUtils.nextLong;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.sonar.api.utils.DateUtils.formatDateTime;
import static org.sonar.db.event.EventTesting.newEvent;
import static org.sonar.server.developers.ws.SearchEventsAction.PARAM_FROM;
import static org.sonar.server.developers.ws.SearchEventsAction.PARAM_PROJECTS;
import static org.sonar.test.JsonAssert.assertJson;

public class SearchEventsActionTest {

  private static final RuleType[] RULE_TYPES_EXCEPT_HOTSPOT = Stream.of(RuleType.values())
    .filter(r -> r != RuleType.SECURITY_HOTSPOT)
    .toArray(RuleType[]::new);

  @Rule
  public DbTester db = DbTester.create();
  @Rule
  public EsTester es = EsTester.create();
  @Rule
  public UserSessionRule userSession = UserSessionRule.standalone();
  @Rule
  public ExpectedException expectedException = ExpectedException.none();
  private Server server = mock(Server.class);
  private IssueIndex issueIndex = new IssueIndex(es.client(), null, null, null);
  private IssueIndexSyncProgressChecker issueIndexSyncProgressChecker = mock(IssueIndexSyncProgressChecker.class);
  private IssueIndexer issueIndexer = new IssueIndexer(es.client(), db.getDbClient(), new IssueIteratorFactory(db.getDbClient()), null);
  private WsActionTester ws = new WsActionTester(new SearchEventsAction(db.getDbClient(), userSession, server, issueIndex,
    issueIndexSyncProgressChecker));

  @Test
  public void definition() {
    WebService.Action definition = ws.getDef();

    assertThat(definition.key()).isEqualTo("search_events");
    assertThat(definition.description()).isNotEmpty();
    assertThat(definition.isPost()).isFalse();
    assertThat(definition.isInternal()).isTrue();
    assertThat(definition.since()).isEqualTo("1.0");
    assertThat(definition.description()).isNotEmpty();
    assertThat(definition.responseExampleAsString()).isNotEmpty();
    assertThat(definition.params()).extracting(Param::key).containsOnly("projects", "from");
    Param projects = definition.param("projects");
    assertThat(projects.isRequired()).isTrue();
    assertThat(projects.exampleValue()).isEqualTo("my_project,another_project");
    assertThat(definition.param("from").isRequired()).isTrue();
  }

  @Test
  public void json_example() {
    userSession.logIn().setRoot();
    ComponentDto project = db.components().insertPrivateProject(p -> p.setName("My Project").setDbKey(KeyExamples.KEY_PROJECT_EXAMPLE_001));
    SnapshotDto analysis = insertAnalysis(project, 1_500_000_000_000L);
    EventDto e1 = db.events().insertEvent(newQualityGateEvent(analysis).setName("Failed").setDate(analysis.getCreatedAt()));
    IntStream.range(0, 15).forEach(x -> insertIssue(project, analysis));
    issueIndexer.indexAllIssues();
    when(server.getPublicRootUrl()).thenReturn("https://sonarcloud.io");

    String result = ws.newRequest()
      .setParam(PARAM_PROJECTS, project.getKey())
      .setParam(PARAM_FROM, formatDateTime(analysis.getCreatedAt() - 1_000L))
      .execute().getInput();

    assertJson(result).ignoreFields("date", "link").isSimilarTo(ws.getDef().responseExampleAsString());
  }

  @Test
  public void events() {
    userSession.logIn().setRoot();
    when(server.getPublicRootUrl()).thenReturn("https://sonarcloud.io");
    ComponentDto project = db.components().insertPrivateProject();
    ComponentDto branch = db.components().insertProjectBranch(project);
    SnapshotDto projectAnalysis = insertAnalysis(project, 1_500_000_000_000L);
    db.events().insertEvent(newQualityGateEvent(projectAnalysis).setDate(projectAnalysis.getCreatedAt()).setName("Passed"));
    insertIssue(project, projectAnalysis);
    insertIssue(project, projectAnalysis);
    SnapshotDto branchAnalysis = insertAnalysis(branch, 1_501_000_000_000L);
    db.events().insertEvent(newQualityGateEvent(branchAnalysis).setDate(branchAnalysis.getCreatedAt()).setName("Failed"));
    insertIssue(branch, branchAnalysis);
    issueIndexer.indexAllIssues();

    SearchEventsWsResponse result = ws.newRequest()
      .setParam(PARAM_PROJECTS, project.getKey())
      .setParam(PARAM_FROM, formatDateTime(1_499_000_000_000L))
      .executeProtobuf(SearchEventsWsResponse.class);

    assertThat(result.getEventsList())
      .extracting(Event::getCategory, Event::getProject, Event::getMessage)
      .containsOnly(
        tuple("QUALITY_GATE", project.getKey(), format("Quality Gate status of project '%s' changed to 'Passed'", project.name())),
        tuple("QUALITY_GATE", project.getKey(), format("Quality Gate status of project '%s' on branch '%s' changed to 'Failed'", project.name(), branch.getBranch())),
        tuple("NEW_ISSUES", project.getKey(), format("You have 2 new issues on project '%s'", project.name())),
        tuple("NEW_ISSUES", project.getKey(), format("You have 1 new issue on project '%s' on branch '%s'", project.name(), branch.getBranch())));
    verify(issueIndexSyncProgressChecker).checkIfAnyComponentsNeedIssueSync(any(), argThat(arg -> arg.contains(project.getKey())));
  }

  @Test
  public void does_not_return_old_events() {
    userSession.logIn().setRoot();
    ComponentDto project = db.components().insertPrivateProject();
    SnapshotDto analysis = insertAnalysis(project, 1_500_000_000_000L);
    insertIssue(project, analysis);
    db.events().insertEvent(newQualityGateEvent(analysis).setDate(analysis.getCreatedAt()).setName("Passed"));
    SnapshotDto oldAnalysis = insertAnalysis(project, 1_400_000_000_000L);
    insertIssue(project, oldAnalysis);
    db.events().insertEvent(newQualityGateEvent(oldAnalysis).setDate(oldAnalysis.getCreatedAt()).setName("Failed"));
    issueIndexer.indexAllIssues();

    SearchEventsWsResponse result = ws.newRequest()
      .setParam(PARAM_PROJECTS, project.getKey())
      .setParam(PARAM_FROM, formatDateTime(analysis.getCreatedAt() - 1450_000_000_000L))
      .executeProtobuf(SearchEventsWsResponse.class);

    assertThat(result.getEventsList())
      .extracting(Event::getCategory, Event::getDate)
      .containsOnly(
        tuple("NEW_ISSUES", formatDateTime(analysis.getCreatedAt())),
        tuple("QUALITY_GATE", formatDateTime(analysis.getCreatedAt())));
  }

  @Test
  public void empty_response_for_empty_list_of_projects() {
    userSession.logIn().setRoot();

    SearchEventsWsResponse result = ws.newRequest()
      .setParam(PARAM_PROJECTS, "")
      .setParam(PARAM_FROM, "")
      .executeProtobuf(SearchEventsWsResponse.class);

    assertThat(result.getEventsList()).isEmpty();
  }

  @Test
  public void does_not_return_events_of_project_for_which_the_current_user_has_no_browse_permission() {
    userSession.logIn();

    ComponentDto project1 = db.components().insertPrivateProject();
    userSession.addProjectPermission(UserRole.CODEVIEWER, project1);
    userSession.addProjectPermission(UserRole.ISSUE_ADMIN, project1);

    ComponentDto project2 = db.components().insertPrivateProject();
    userSession.addProjectPermission(UserRole.USER, project2);

    SnapshotDto a1 = insertAnalysis(project1, 1_500_000_000_000L);
    EventDto e1 = db.events().insertEvent(newQualityGateEvent(a1).setDate(a1.getCreatedAt()));
    insertIssue(project1, a1);
    SnapshotDto a2 = insertAnalysis(project2, 1_500_000_000_000L);
    EventDto e2 = db.events().insertEvent(newQualityGateEvent(a2).setDate(a2.getCreatedAt()));
    insertIssue(project2, a2);
    issueIndexer.indexAllIssues();

    String stringFrom = formatDateTime(a1.getCreatedAt() - 1_000L);
    SearchEventsWsResponse result = ws.newRequest()
      .setParam(PARAM_PROJECTS, String.join(",", project1.getKey(), project2.getKey()))
      .setParam(PARAM_FROM, String.join(",", stringFrom, stringFrom))
      .executeProtobuf(SearchEventsWsResponse.class);

    assertThat(result.getEventsList())
      .extracting(Event::getCategory, Event::getProject)
      .containsOnly(
        tuple("NEW_ISSUES", project2.getKey()),
        tuple(EventCategory.QUALITY_GATE.name(), project2.getKey()));
  }

  @Test
  public void empty_response_if_project_key_is_unknown() {
    userSession.logIn().setRoot();

    long from = 1_500_000_000_000L;
    SearchEventsWsResponse result = ws.newRequest()
      .setParam(PARAM_PROJECTS, "unknown")
      .setParam(PARAM_FROM, formatDateTime(from - 1_000L))
      .executeProtobuf(SearchEventsWsResponse.class);

    assertThat(result.getEventsList()).isEmpty();
  }

  @Test
  public void fail_when_not_loggued() {
    userSession.anonymous();
    ComponentDto project = db.components().insertPrivateProject();

    expectedException.expect(UnauthorizedException.class);

    ws.newRequest()
      .setParam(PARAM_PROJECTS, project.getKey())
      .setParam(PARAM_FROM, formatDateTime(1_000L))
      .execute();
  }

  @Test
  public void fail_if_date_format_is_not_valid() {
    userSession.logIn().setRoot();

    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("'wat' cannot be parsed as either a date or date+time");
    ws.newRequest()
      .setParam(PARAM_PROJECTS, "foo")
      .setParam(PARAM_FROM, "wat")
      .executeProtobuf(SearchEventsWsResponse.class);

  }

  private static EventDto newQualityGateEvent(SnapshotDto analysis) {
    return newEvent(analysis).setCategory(EventCategory.QUALITY_GATE.getLabel());
  }

  private CeActivityDto insertActivity(ComponentDto project, SnapshotDto analysis, CeActivityDto.Status status) {
    CeQueueDto queueDto = new CeQueueDto();
    queueDto.setTaskType(CeTaskTypes.REPORT);
    String mainBranchProjectUuid = project.getMainBranchProjectUuid();
    queueDto.setComponentUuid(mainBranchProjectUuid == null ? project.uuid() : mainBranchProjectUuid);
    queueDto.setUuid(randomAlphanumeric(40));
    queueDto.setCreatedAt(nextLong());
    CeActivityDto activityDto = new CeActivityDto(queueDto);
    activityDto.setStatus(status);
    activityDto.setExecutionTimeMs(nextLong());
    activityDto.setExecutedAt(nextLong());
    activityDto.setAnalysisUuid(analysis.getUuid());
    db.getDbClient().ceActivityDao().insert(db.getSession(), activityDto);
    db.commit();
    return activityDto;
  }

  private void insertIssue(ComponentDto component, SnapshotDto analysis) {
    db.issues().insert(db.rules().insert(), component, component,
      i -> i.setIssueCreationDate(new Date(analysis.getCreatedAt()))
        .setAssigneeUuid(userSession.getUuid())
        .setType(randomRuleTypeExceptHotspot()));
  }

  private SnapshotDto insertAnalysis(ComponentDto project, long analysisDate) {
    SnapshotDto analysis = db.components().insertSnapshot(project, s -> s.setCreatedAt(analysisDate));
    insertActivity(project, analysis, CeActivityDto.Status.SUCCESS);
    return analysis;
  }

  private RuleType randomRuleTypeExceptHotspot() {
    return RULE_TYPES_EXCEPT_HOTSPOT[nextInt(RULE_TYPES_EXCEPT_HOTSPOT.length)];
  }
}
