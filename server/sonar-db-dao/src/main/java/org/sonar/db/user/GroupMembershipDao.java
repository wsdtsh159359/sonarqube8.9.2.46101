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
package org.sonar.db.user;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Multimap;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.ibatis.session.RowBounds;
import org.sonar.db.Dao;
import org.sonar.db.DbSession;

import static org.sonar.db.DatabaseUtils.executeLargeInputs;

public class GroupMembershipDao implements Dao {
  private static final String QUERY_PARAM_KEY = "query";

  public List<GroupMembershipDto> selectGroups(DbSession session, GroupMembershipQuery query, String userUuid, int offset, int limit) {
    Map<String, Object> params = ImmutableMap.of(QUERY_PARAM_KEY, query, "userUuid", userUuid);
    return mapper(session).selectGroups(params, new RowBounds(offset, limit));
  }

  public int countGroups(DbSession session, GroupMembershipQuery query, String userUuid) {
    Map<String, Object> params = ImmutableMap.of(QUERY_PARAM_KEY, query, "userUuid", userUuid);
    return mapper(session).countGroups(params);
  }

  public List<UserMembershipDto> selectMembers(DbSession session, UserMembershipQuery query, int offset, int limit) {
    Map<String, Object> params = ImmutableMap.of(QUERY_PARAM_KEY, query, "groupUuid", query.groupUuid());
    return mapper(session).selectMembers(params, new RowBounds(offset, limit));
  }

  public int countMembers(DbSession session, UserMembershipQuery query) {
    Map<String, Object> params = ImmutableMap.of(QUERY_PARAM_KEY, query, "groupUuid", query.groupUuid());
    return mapper(session).countMembers(params);
  }

  public Map<String, Integer> countUsersByGroups(DbSession session, Collection<String> groupUuids) {
    Map<String, Integer> result = new HashMap<>();
    executeLargeInputs(
      groupUuids,
      input -> {
        List<GroupUserCount> userCounts = mapper(session).countUsersByGroup(input);
        for (GroupUserCount count : userCounts) {
          result.put(count.groupName(), count.userCount());
        }
        return userCounts;
      });

    return result;
  }

  public Map<String, Integer> countGroupsByUsers(DbSession dbSession, List<String> logins) {
    List<LoginGroupCount> loginGroupCount = mapper(dbSession).countGroupsByUsers(logins);
    Map<String, Integer> map = new HashMap<>();
    for (LoginGroupCount l : loginGroupCount) {
      map.put(l.login(), l.groupCount());
    }
    return map;
  }

  public List<String> selectGroupUuidsByUserUuid(DbSession dbSession, String userUuid) {
    return mapper(dbSession).selectGroupUuidsByUserUuid(userUuid);
  }

  public Multimap<String, String> selectGroupsByLogins(DbSession session, Collection<String> logins) {
    Multimap<String, String> result = ArrayListMultimap.create();
    executeLargeInputs(
      logins,
      input -> {
        List<LoginGroup> groupMemberships = mapper(session).selectGroupsByLogins(input);
        for (LoginGroup membership : groupMemberships) {
          result.put(membership.login(), membership.groupName());
        }
        return groupMemberships;
      });

    return result;
  }

  private static GroupMembershipMapper mapper(DbSession session) {
    return session.getMapper(GroupMembershipMapper.class);
  }
}
