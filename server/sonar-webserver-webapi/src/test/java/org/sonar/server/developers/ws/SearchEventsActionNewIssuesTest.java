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

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Date;
import java.util.stream.Stream;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.platform.Server;
import org.sonar.api.rules.RuleType;
import org.sonar.db.DbTester;
import org.sonar.db.ce.CeActivityDto;
import org.sonar.db.ce.CeQueueDto;
import org.sonar.db.ce.CeTaskTypes;
import org.sonar.db.component.BranchType;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.component.SnapshotDto;
import org.sonar.db.rule.RuleDefinitionDto;
import org.sonar.server.es.EsTester;
import org.sonar.server.issue.index.IssueIndex;
import org.sonar.server.issue.index.IssueIndexSyncProgressChecker;
import org.sonar.server.issue.index.IssueIndexer;
import org.sonar.server.issue.index.IssueIteratorFactory;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.ws.WsActionTester;
import org.sonarqube.ws.Developers.SearchEventsWsResponse;
import org.sonarqube.ws.Developers.SearchEventsWsResponse.Event;

import static java.lang.String.format;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.apache.commons.lang.RandomStringUtils.randomAlphanumeric;
import static org.apache.commons.lang.math.RandomUtils.nextInt;
import static org.apache.commons.lang.math.RandomUtils.nextLong;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.sonar.api.utils.DateUtils.formatDateTime;
import static org.sonar.db.component.BranchType.BRANCH;
import static org.sonar.server.developers.ws.SearchEventsAction.PARAM_FROM;
import static org.sonar.server.developers.ws.SearchEventsAction.PARAM_PROJECTS;

public class SearchEventsActionNewIssuesTest {

  private static final RuleType[] RULE_TYPES_EXCEPT_HOTSPOT = Stream.of(RuleType.values())
    .filter(r -> r != RuleType.SECURITY_HOTSPOT)
    .toArray(RuleType[]::new);

  @Rule
  public DbTester db = DbTester.create();
  @Rule
  public EsTester es = EsTester.create();
  @Rule
  public UserSessionRule userSession = UserSessionRule.standalone();

  private Server server = mock(Server.class);

  private IssueIndex issueIndex = new IssueIndex(es.client(), null, null, null);
  private IssueIndexer issueIndexer = new IssueIndexer(es.client(), db.getDbClient(), new IssueIteratorFactory(db.getDbClient()), null);
  private IssueIndexSyncProgressChecker issueIndexSyncProgressChecker = mock(IssueIndexSyncProgressChecker.class);
  private WsActionTester ws = new WsActionTester(new SearchEventsAction(db.getDbClient(), userSession, server, issueIndex,
    issueIndexSyncProgressChecker));

  @Test
  public void issue_event() {
    userSession.logIn().setRoot();
    when(server.getPublicRootUrl()).thenReturn("https://sonarcloud.io");
    ComponentDto project = db.components().insertPrivateProject();
    SnapshotDto analysis = insertAnalysis(project, 1_500_000_000_000L);
    insertIssue(project, analysis);
    insertIssue(project, analysis);
    // will be ignored
    insertSecurityHotspot(project, analysis);
    issueIndexer.indexAllIssues();

    long from = analysis.getCreatedAt() - 1_000_000L;
    SearchEventsWsResponse result = ws.newRequest()
      .setParam(PARAM_PROJECTS, project.getKey())
      .setParam(PARAM_FROM, formatDateTime(from))
      .executeProtobuf(SearchEventsWsResponse.class);

    assertThat(result.getEventsList())
      .extracting(Event::getCategory, Event::getProject, Event::getMessage, Event::getLink, Event::getDate)
      .containsOnly(
        tuple("NEW_ISSUES", project.getKey(), format("You have 2 new issues on project '%s'", project.name()),
          format("https://sonarcloud.io/project/issues?id=%s&createdAfter=%s&assignees=%s&resolved=false", project.getKey(), encode(formatDateTime(from + 1_000L)),
            userSession.getLogin()),
          formatDateTime(analysis.getCreatedAt())));
  }

  @Test
  public void many_issues_events() {
    userSession.logIn().setRoot();
    long from = 1_500_000_000_000L;
    ComponentDto project = db.components().insertPrivateProject(p -> p.setName("SonarQube"));
    SnapshotDto analysis = insertAnalysis(project, from);
    insertIssue(project, analysis);
    insertIssue(project, analysis);
    issueIndexer.indexAllIssues();
    String fromDate = formatDateTime(from - 1_000L);

    SearchEventsWsResponse result = ws.newRequest()
      .setParam(PARAM_PROJECTS, project.getKey())
      .setParam(PARAM_FROM, fromDate)
      .executeProtobuf(SearchEventsWsResponse.class);

    assertThat(result.getEventsList()).extracting(Event::getCategory, Event::getMessage, Event::getProject, Event::getDate)
      .containsExactly(tuple("NEW_ISSUES", "You have 2 new issues on project 'SonarQube'", project.getKey(),
        formatDateTime(from)));
  }

  @Test
  public void does_not_return_old_issue() {
    userSession.logIn().setRoot();
    ComponentDto project = db.components().insertPrivateProject();
    SnapshotDto analysis = insertAnalysis(project, 1_500_000_000_000L);
    db.issues().insert(db.rules().insert(), project, project, i -> i.setIssueCreationDate(new Date(analysis.getCreatedAt() - 10_000L)));
    issueIndexer.indexAllIssues();

    SearchEventsWsResponse result = ws.newRequest()
      .setParam(PARAM_PROJECTS, project.getKey())
      .setParam(PARAM_FROM, formatDateTime(analysis.getCreatedAt() - 1_000L))
      .executeProtobuf(SearchEventsWsResponse.class);

    assertThat(result.getEventsList()).isEmpty();
  }

  @Test
  public void return_link_to_issue_search_for_new_issues_event() {
    userSession.logIn("my_login").setRoot();
    ComponentDto project = db.components().insertPrivateProject(p -> p.setDbKey("my_project"));
    SnapshotDto analysis = insertAnalysis(project, 1_400_000_000_000L);
    insertIssue(project, analysis);
    issueIndexer.indexAllIssues();
    when(server.getPublicRootUrl()).thenReturn("https://sonarcloud.io");

    SearchEventsWsResponse result = ws.newRequest()
      .setParam(PARAM_PROJECTS, project.getKey())
      .setParam(PARAM_FROM, formatDateTime(analysis.getCreatedAt() - 1_000L))
      .executeProtobuf(SearchEventsWsResponse.class);

    assertThat(result.getEventsList()).extracting(Event::getLink)
      .containsExactly("https://sonarcloud.io/project/issues?id=my_project&createdAfter=" + encode(formatDateTime(analysis.getCreatedAt())) + "&assignees=my_login&resolved=false");
  }

  @Test
  public void branch_issues_events() {
    userSession.logIn().setRoot();
    when(server.getPublicRootUrl()).thenReturn("https://sonarcloud.io");
    ComponentDto project = db.components().insertPrivateProject();
    ComponentDto branch1 = db.components().insertProjectBranch(project, b -> b.setBranchType(BRANCH).setKey("branch1"));
    SnapshotDto branch1Analysis = insertAnalysis(branch1, 1_500_000_000_000L);
    insertIssue(branch1, branch1Analysis);
    insertIssue(branch1, branch1Analysis);
    ComponentDto branch2 = db.components().insertProjectBranch(project, b -> b.setBranchType(BranchType.BRANCH).setKey("branch"));
    SnapshotDto branch2Analysis = insertAnalysis(branch2, 1_300_000_000_000L);
    insertIssue(branch2, branch2Analysis);
    issueIndexer.indexAllIssues();

    long from = 1_000_000_000_000L;
    SearchEventsWsResponse result = ws.newRequest()
      .setParam(PARAM_PROJECTS, project.getKey())
      .setParam(PARAM_FROM, formatDateTime(from))
      .executeProtobuf(SearchEventsWsResponse.class);

    assertThat(result.getEventsList())
      .extracting(Event::getCategory, Event::getProject, Event::getMessage, Event::getLink, Event::getDate)
      .containsOnly(
        tuple("NEW_ISSUES", project.getKey(), format("You have 2 new issues on project '%s' on branch '%s'", project.name(), branch1.getBranch()),
          format("https://sonarcloud.io/project/issues?id=%s&createdAfter=%s&assignees=%s&resolved=false&branch=%s", branch1.getKey(), encode(formatDateTime(from + 1_000L)),
            userSession.getLogin(), branch1.getBranch()),
          formatDateTime(branch1Analysis.getCreatedAt())),
        tuple("NEW_ISSUES", project.getKey(), format("You have 1 new issue on project '%s' on branch '%s'", project.name(), branch2.getBranch()),
          format("https://sonarcloud.io/project/issues?id=%s&createdAfter=%s&assignees=%s&resolved=false&branch=%s", branch2.getKey(), encode(formatDateTime(from + 1_000L)),
            userSession.getLogin(), branch2.getBranch()),
          formatDateTime(branch2Analysis.getCreatedAt())));
  }

  @Test
  public void pull_request_issues_events() {
    userSession.logIn().setRoot();
    when(server.getPublicRootUrl()).thenReturn("https://sonarcloud.io");
    ComponentDto project = db.components().insertPrivateProject();
    ComponentDto nonMainBranch = db.components().insertProjectBranch(project, b -> b.setBranchType(BRANCH).setKey("nonMain"));
    SnapshotDto nonMainBranchAnalysis = insertAnalysis(nonMainBranch, 1_500_000_000_000L);
    insertIssue(nonMainBranch, nonMainBranchAnalysis);
    insertIssue(nonMainBranch, nonMainBranchAnalysis);
    ComponentDto pullRequest = db.components().insertProjectBranch(project, b -> b.setBranchType(BranchType.PULL_REQUEST).setKey("42"));
    SnapshotDto pullRequestAnalysis = insertAnalysis(pullRequest, 1_300_000_000_000L);
    insertIssue(pullRequest, pullRequestAnalysis);
    issueIndexer.indexAllIssues();

    long from = 1_000_000_000_000L;
    SearchEventsWsResponse result = ws.newRequest()
      .setParam(PARAM_PROJECTS, project.getKey())
      .setParam(PARAM_FROM, formatDateTime(from))
      .executeProtobuf(SearchEventsWsResponse.class);

    assertThat(result.getEventsList())
      .extracting(Event::getCategory, Event::getProject, Event::getMessage, Event::getLink, Event::getDate)
      .containsOnly(
        tuple("NEW_ISSUES", project.getKey(), format("You have 2 new issues on project '%s' on branch '%s'", project.name(), nonMainBranch.getBranch()),
          format("https://sonarcloud.io/project/issues?id=%s&createdAfter=%s&assignees=%s&resolved=false&branch=%s", nonMainBranch.getKey(), encode(formatDateTime(from + 1_000L)),
            userSession.getLogin(), nonMainBranch.getBranch()),
          formatDateTime(nonMainBranchAnalysis.getCreatedAt())),
        tuple("NEW_ISSUES", project.getKey(), format("You have 1 new issue on project '%s' on pull request '%s'", project.name(), pullRequest.getPullRequest()),
          format("https://sonarcloud.io/project/issues?id=%s&createdAfter=%s&assignees=%s&resolved=false&pullRequest=%s", pullRequest.getKey(),
            encode(formatDateTime(from + 1_000L)),
            userSession.getLogin(), pullRequest.getPullRequest()),
          formatDateTime(pullRequestAnalysis.getCreatedAt())));
  }

  @Test
  public void encode_link() {
    userSession.logIn("rågnar").setRoot();
    long from = 1_500_000_000_000L;
    ComponentDto project = db.components().insertPrivateProject(p -> p.setDbKey("M&M's"));
    SnapshotDto analysis = insertAnalysis(project, from);
    insertIssue(project, analysis);
    issueIndexer.indexAllIssues();
    when(server.getPublicRootUrl()).thenReturn("http://sonarcloud.io");

    String fromDate = formatDateTime(from - 1_000L);
    SearchEventsWsResponse result = ws.newRequest()
      .setParam(PARAM_PROJECTS, project.getKey())
      .setParam(PARAM_FROM, fromDate)
      .executeProtobuf(SearchEventsWsResponse.class);

    assertThat(result.getEventsList()).extracting(Event::getLink)
      .containsExactly("http://sonarcloud.io/project/issues?id=M%26M%27s&createdAfter=" + encode(formatDateTime(from)) + "&assignees=r%C3%A5gnar&resolved=false");
  }

  private String encode(String text) {
    try {
      return URLEncoder.encode(text, UTF_8.name());
    } catch (UnsupportedEncodingException e) {
      throw new IllegalStateException(format("Cannot encode %s", text), e);
    }
  }

  private void insertIssue(ComponentDto component, SnapshotDto analysis) {
    RuleDefinitionDto rule = db.rules().insert(r -> r.setType(randomRuleTypeExceptHotspot()));
    db.issues().insert(rule, component, component,
      i -> i.setIssueCreationDate(new Date(analysis.getCreatedAt()))
        .setAssigneeUuid(userSession.getUuid())
        .setType(randomRuleTypeExceptHotspot()));
  }

  private void insertSecurityHotspot(ComponentDto component, SnapshotDto analysis) {
    RuleDefinitionDto rule = db.rules().insert(r -> r.setType(RuleType.SECURITY_HOTSPOT));
    db.issues().insert(rule, component, component,
      i -> i.setIssueCreationDate(new Date(analysis.getCreatedAt()))
        .setAssigneeUuid(userSession.getUuid())
        .setType(RuleType.SECURITY_HOTSPOT));
  }

  private SnapshotDto insertAnalysis(ComponentDto project, long analysisDate) {
    SnapshotDto analysis = db.components().insertSnapshot(project, s -> s.setCreatedAt(analysisDate));
    insertActivity(project, analysis, CeActivityDto.Status.SUCCESS);
    return analysis;
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

  private RuleType randomRuleTypeExceptHotspot() {
    return RULE_TYPES_EXCEPT_HOTSPOT[nextInt(RULE_TYPES_EXCEPT_HOTSPOT.length)];
  }
}
