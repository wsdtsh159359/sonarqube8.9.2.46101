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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import org.sonar.api.platform.Server;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.web.UserRole;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.component.BranchDto;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.component.SnapshotDto;
import org.sonar.db.event.EventDto;
import org.sonar.server.issue.index.IssueIndex;
import org.sonar.server.issue.index.IssueIndexSyncProgressChecker;
import org.sonar.server.issue.index.ProjectStatistics;
import org.sonar.server.projectanalysis.ws.EventCategory;
import org.sonar.server.user.UserSession;
import org.sonar.server.ws.KeyExamples;
import org.sonarqube.ws.Developers.SearchEventsWsResponse;
import org.sonarqube.ws.Developers.SearchEventsWsResponse.Event;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkState;
import static java.lang.String.format;
import static java.lang.String.join;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Comparator.comparing;
import static org.sonar.api.utils.DateUtils.formatDateTime;
import static org.sonar.api.utils.DateUtils.parseDateTimeQuietly;
import static org.sonar.core.util.stream.MoreCollectors.toList;
import static org.sonar.core.util.stream.MoreCollectors.uniqueIndex;
import static org.sonar.db.component.BranchType.BRANCH;
import static org.sonar.db.component.BranchType.PULL_REQUEST;
import static org.sonar.server.developers.ws.UuidFromPairs.componentUuids;
import static org.sonar.server.developers.ws.UuidFromPairs.fromDates;
import static org.sonar.server.exceptions.BadRequestException.checkRequest;
import static org.sonar.server.ws.WsUtils.writeProtobuf;

public class SearchEventsAction implements DevelopersWsAction {

  public static final String PARAM_PROJECTS = "projects";
  public static final String PARAM_FROM = "from";

  private final DbClient dbClient;
  private final UserSession userSession;
  private final Server server;
  private final IssueIndex issueIndex;
  private final IssueIndexSyncProgressChecker issueIndexSyncProgressChecker;

  public SearchEventsAction(DbClient dbClient, UserSession userSession, Server server, IssueIndex issueIndex,
    IssueIndexSyncProgressChecker issueIndexSyncProgressChecker) {
    this.dbClient = dbClient;
    this.userSession = userSession;
    this.server = server;
    this.issueIndex = issueIndex;
    this.issueIndexSyncProgressChecker = issueIndexSyncProgressChecker;
  }

  @Override
  public void define(WebService.NewController controller) {
    WebService.NewAction action = controller.createAction("search_events")
      .setDescription("Search for events.<br/>" +
        "Requires authentication."
        + "<br/>When issue indexation is in progress returns 503 service unavailable HTTP code.")
      .setSince("1.0")
      .setInternal(true)
      .setHandler(this)
      .setResponseExample(SearchEventsAction.class.getResource("search_events-example.json"));

    action.createParam(PARAM_PROJECTS)
      .setRequired(true)
      .setDescription("Comma-separated list of project keys to search notifications for")
      .setExampleValue(join(",", KeyExamples.KEY_PROJECT_EXAMPLE_001, KeyExamples.KEY_PROJECT_EXAMPLE_002));

    action.createParam(PARAM_FROM)
      .setRequired(true)
      .setDescription("Comma-separated list of datetimes. Filter events created after the given date (exclusive).")
      .setExampleValue("2017-10-19T13:00:00+0200");
  }

  @Override
  public void handle(Request request, Response response) throws Exception {
    userSession.checkLoggedIn();
    checkIfNeedIssueSync(request.mandatoryParamAsStrings(PARAM_PROJECTS));
    SearchEventsWsResponse.Builder message = SearchEventsWsResponse.newBuilder();
    computeEvents(request).forEach(message::addEvents);
    writeProtobuf(message.build(), request, response);
  }

  private void checkIfNeedIssueSync(List<String> projectKeys) {
    try (DbSession dbSession = dbClient.openSession(false)) {
      issueIndexSyncProgressChecker.checkIfAnyComponentsNeedIssueSync(dbSession, projectKeys);
    }
  }

  private Stream<Event> computeEvents(Request request) {
    List<String> projectKeys = request.mandatoryParamAsStrings(PARAM_PROJECTS);
    List<Long> fromDates = mandatoryParamAsDateTimes(request, PARAM_FROM);

    if (projectKeys.isEmpty()) {
      return Stream.empty();
    }

    try (DbSession dbSession = dbClient.openSession(false)) {
      List<ComponentDto> authorizedProjects = searchProjects(dbSession, projectKeys);
      Map<String, ComponentDto> componentsByUuid = authorizedProjects.stream().collect(uniqueIndex(ComponentDto::uuid));
      List<UuidFromPair> uuidFromPairs = componentUuidFromPairs(fromDates, projectKeys, authorizedProjects);
      List<SnapshotDto> analyses = dbClient.snapshotDao().selectFinishedByComponentUuidsAndFromDates(dbSession, componentUuids(uuidFromPairs), fromDates(uuidFromPairs));

      if (analyses.isEmpty()) {
        return Stream.empty();
      }

      List<String> projectUuids = analyses.stream().map(SnapshotDto::getComponentUuid).collect(toList());
      Map<String, BranchDto> branchesByUuids = dbClient.branchDao().selectByUuids(dbSession, projectUuids).stream().collect(uniqueIndex(BranchDto::getUuid));

      return Stream.concat(
        computeQualityGateChangeEvents(dbSession, componentsByUuid, branchesByUuids, analyses),
        computeNewIssuesEvents(componentsByUuid, branchesByUuids, uuidFromPairs));
    }
  }

  private Stream<Event> computeQualityGateChangeEvents(DbSession dbSession, Map<String, ComponentDto> projectsByUuid,
    Map<String, BranchDto> branchesByUuids,
    List<SnapshotDto> analyses) {
    Map<String, EventDto> eventsByComponentUuid = new HashMap<>();
    dbClient.eventDao().selectByAnalysisUuids(dbSession, analyses.stream().map(SnapshotDto::getUuid).collect(toList(analyses.size())))
      .stream()
      .sorted(comparing(EventDto::getDate))
      .filter(e -> EventCategory.QUALITY_GATE.getLabel().equals(e.getCategory()))
      .forEach(e -> eventsByComponentUuid.put(e.getComponentUuid(), e));

    Predicate<EventDto> branchPredicate = e -> branchesByUuids.get(e.getComponentUuid()).getBranchType() == BRANCH;
    return eventsByComponentUuid.values()
      .stream()
      .sorted(comparing(EventDto::getDate))
      .filter(branchPredicate)
      .map(e -> {
        BranchDto branch = branchesByUuids.get(e.getComponentUuid());
        ComponentDto project = projectsByUuid.get(branch.getProjectUuid());
        checkState(project != null, "Found event '%s', for a component that we did not search for", e.getUuid());
        return Event.newBuilder()
          .setCategory(EventCategory.fromLabel(e.getCategory()).name())
          .setProject(project.getKey())
          .setMessage(branch.isMain() ? format("Quality Gate status of project '%s' changed to '%s'", project.name(), e.getName())
            : format("Quality Gate status of project '%s' on branch '%s' changed to '%s'", project.name(), branch.getKey(), e.getName()))
          .setLink(computeDashboardLink(project, branch))
          .setDate(formatDateTime(e.getDate()))
          .build();
      });
  }

  private Stream<Event> computeNewIssuesEvents(Map<String, ComponentDto> projectsByUuid, Map<String, BranchDto> branchesByUuids,
    List<UuidFromPair> uuidFromPairs) {
    Map<String, Long> fromsByProjectUuid = uuidFromPairs.stream().collect(Collectors.toMap(
      UuidFromPair::getComponentUuid,
      UuidFromPair::getFrom));
    List<ProjectStatistics> projectStatistics = issueIndex.searchProjectStatistics(componentUuids(uuidFromPairs), fromDates(uuidFromPairs), userSession.getUuid());
    return projectStatistics
      .stream()
      .map(e -> {
        BranchDto branch = branchesByUuids.get(e.getProjectUuid());
        ComponentDto project = projectsByUuid.get(branch.getProjectUuid());
        long issueCount = e.getIssueCount();
        long lastIssueDate = e.getLastIssueDate();
        String branchType = branch.getBranchType().equals(PULL_REQUEST) ? "pull request" : "branch";
        return Event.newBuilder()
          .setCategory("NEW_ISSUES")
          .setMessage(format("You have %s new %s on project '%s'", issueCount, issueCount == 1 ? "issue" : "issues",
            project.name()) + (branch.isMain() ? "" : format(" on %s '%s'", branchType, branch.getKey())))
          .setLink(computeIssuesSearchLink(project, branch, fromsByProjectUuid.get(project.uuid()), userSession.getLogin()))
          .setProject(project.getKey())
          .setDate(formatDateTime(lastIssueDate))
          .build();
      });
  }

  private List<ComponentDto> searchProjects(DbSession dbSession, List<String> projectKeys) {
    List<ComponentDto> projects = dbClient.componentDao().selectByKeys(dbSession, projectKeys);
    return userSession.keepAuthorizedComponents(UserRole.USER, projects);
  }

  private String computeIssuesSearchLink(ComponentDto component, BranchDto branch, long functionalFromDate, String login) {
    String branchParam = branch.getBranchType().equals(PULL_REQUEST) ? "pullRequest" : "branch";
    String link = format("%s/project/issues?id=%s&createdAfter=%s&assignees=%s&resolved=false",
      server.getPublicRootUrl(), encode(component.getKey()), encode(formatDateTime(functionalFromDate)), encode(login));
    link += branch.isMain() ? "" : format("&%s=%s", branchParam, encode(branch.getKey()));
    return link;
  }

  private String computeDashboardLink(ComponentDto component, BranchDto branch) {
    String link = server.getPublicRootUrl() + "/dashboard?id=" + encode(component.getKey());
    link += branch.isMain() ? "" : format("&branch=%s", encode(branch.getKey()));
    return link;
  }

  private static List<UuidFromPair> componentUuidFromPairs(List<Long> fromDates, List<String> projectKeys, List<ComponentDto> authorizedProjects) {
    checkRequest(projectKeys.size() == fromDates.size(), "The number of components (%s) and from dates (%s) must be the same.", projectKeys.size(), fromDates.size());
    Map<String, Long> fromDatesByProjectKey = IntStream.range(0, projectKeys.size()).boxed()
      .collect(uniqueIndex(projectKeys::get, fromDates::get));
    return authorizedProjects.stream()
      .map(dto -> new UuidFromPair(dto.uuid(), fromDatesByProjectKey.get(dto.getDbKey())))
      .collect(toList(authorizedProjects.size()));
  }

  private static List<Long> mandatoryParamAsDateTimes(Request request, String param) {
    return request.mandatoryParamAsStrings(param).stream()
      .map(stringDate -> {
        Date date = parseDateTimeQuietly(stringDate);
        checkArgument(date != null, "'%s' cannot be parsed as either a date or date+time", stringDate);
        return date.getTime() + 1_000L;
      })
      .collect(toList());
  }

  private static String encode(String text) {
    try {
      return URLEncoder.encode(text, UTF_8.name());
    } catch (UnsupportedEncodingException e) {
      throw new IllegalStateException(format("Cannot encode %s", text), e);
    }
  }
}
