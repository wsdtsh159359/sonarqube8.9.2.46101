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
package org.sonar.db.component;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.sonar.api.utils.System2;
import org.sonar.db.Dao;
import org.sonar.db.DbSession;
import org.sonar.db.project.ProjectDto;

import static java.util.Collections.emptyList;
import static org.sonar.db.DatabaseUtils.executeLargeInputs;

public class BranchDao implements Dao {

  private final System2 system2;

  public BranchDao(System2 system2) {
    this.system2 = system2;
  }

  public void insert(DbSession dbSession, BranchDto dto) {
    mapper(dbSession).insert(dto, system2.now());
  }

  public void upsert(DbSession dbSession, BranchDto dto) {
    BranchMapper mapper = mapper(dbSession);
    long now = system2.now();
    if (mapper.update(dto, now) == 0) {
      mapper.insert(dto, now);
    }
  }

  public int updateMainBranchName(DbSession dbSession, String projectUuid, String newBranchKey) {
    long now = system2.now();
    return mapper(dbSession).updateMainBranchName(projectUuid, newBranchKey, now);
  }

  public int updateExcludeFromPurge(DbSession dbSession, String branchUuid, boolean excludeFromPurge) {
    long now = system2.now();
    return mapper(dbSession).updateExcludeFromPurge(branchUuid, excludeFromPurge, now);
  }

  public Optional<BranchDto> selectByBranchKey(DbSession dbSession, String projectUuid, String key) {
    return selectByKey(dbSession, projectUuid, key, BranchType.BRANCH);
  }

  public List<BranchDto> selectByBranchKeys(DbSession dbSession, Map<String, String> branchKeyByProjectUuid) {
    if (branchKeyByProjectUuid.isEmpty()) {
      return emptyList();
    }
    return mapper(dbSession).selectByBranchKeys(branchKeyByProjectUuid);
  }

  public Optional<BranchDto> selectByPullRequestKey(DbSession dbSession, String projectUuid, String key) {
    return selectByKey(dbSession, projectUuid, key, BranchType.PULL_REQUEST);
  }

  private static Optional<BranchDto> selectByKey(DbSession dbSession, String projectUuid, String key, BranchType branchType) {
    return Optional.ofNullable(mapper(dbSession).selectByKey(projectUuid, key, branchType));
  }

  public Collection<BranchDto> selectByComponent(DbSession dbSession, ComponentDto component) {
    String projectUuid = component.getMainBranchProjectUuid();
    if (projectUuid == null) {
      projectUuid = component.projectUuid();
    }
    return mapper(dbSession).selectByProjectUuid(projectUuid);
  }

  public Collection<BranchDto> selectByProject(DbSession dbSession, ProjectDto project) {
    return mapper(dbSession).selectByProjectUuid(project.getUuid());
  }

  public List<BranchDto> selectByUuids(DbSession session, Collection<String> uuids) {
    if (uuids.isEmpty()) {
      return emptyList();
    }
    return executeLargeInputs(uuids, mapper(session)::selectByUuids);
  }

  public Optional<BranchDto> selectByUuid(DbSession session, String uuid) {
    return Optional.ofNullable(mapper(session).selectByUuid(uuid));
  }

  public List<String> selectProjectUuidsWithIssuesNeedSync(DbSession session, Collection<String> uuids) {
    if (uuids.isEmpty()) {
      return emptyList();
    }

    return executeLargeInputs(uuids, mapper(session)::selectProjectUuidsWithIssuesNeedSync);
  }

  public boolean hasAnyBranchWhereNeedIssueSync(DbSession session, boolean needIssueSync) {
    return mapper(session).hasAnyBranchWhereNeedIssueSync(needIssueSync) > 0;
  }

  public boolean hasNonMainBranches(DbSession dbSession) {
    return mapper(dbSession).countNonMainBranches() > 0L;
  }

  public long countByTypeAndCreationDate(DbSession dbSession, BranchType branchType, long sinceDate) {
    return mapper(dbSession).countByTypeAndCreationDate(branchType.name(), sinceDate);
  }

  public int countByNeedIssueSync(DbSession session, boolean needIssueSync) {
    return mapper(session).countByNeedIssueSync(needIssueSync);
  }

  public int countAll(DbSession session) {
    return mapper(session).countAll();
  }

  private static BranchMapper mapper(DbSession dbSession) {
    return dbSession.getMapper(BranchMapper.class);
  }

  public List<BranchDto> selectBranchNeedingIssueSync(DbSession dbSession) {
    return mapper(dbSession).selectBranchNeedingIssueSync();
  }

  public long updateAllNeedIssueSync(DbSession dbSession) {
    return mapper(dbSession).updateAllNeedIssueSync(system2.now());
  }

  public long updateNeedIssueSync(DbSession dbSession, String branchUuid, boolean needIssueSync) {
    long now = system2.now();
    return mapper(dbSession).updateNeedIssueSync(branchUuid, needIssueSync, now);
  }

  public void deleteBranch(DbSession dbSession, String projectUuid, String branchKey) {
    mapper(dbSession).deleteBranch(projectUuid, branchKey);
  }

  public boolean doAnyOfComponentsNeedIssueSync(DbSession session, List<String> components) {
    if (!components.isEmpty()) {
      List<Boolean> result = new LinkedList<>();
      return executeLargeInputs(components, input -> {
        boolean groupNeedIssueSync = mapper(session).doAnyOfComponentsNeedIssueSync(input) > 0;
        result.add(groupNeedIssueSync);
        return result;
      }).stream()
        .anyMatch(b -> b);
    }
    return false;
  }
}
