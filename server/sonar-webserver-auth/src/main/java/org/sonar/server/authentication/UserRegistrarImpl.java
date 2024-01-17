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
package org.sonar.server.authentication;

import com.google.common.collect.Sets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import org.sonar.api.server.authentication.IdentityProvider;
import org.sonar.api.server.authentication.UserIdentity;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.user.GroupDto;
import org.sonar.db.user.UserDto;
import org.sonar.db.user.UserGroupDto;
import org.sonar.server.authentication.event.AuthenticationException;
import org.sonar.server.user.ExternalIdentity;
import org.sonar.server.user.NewUser;
import org.sonar.server.user.UpdateUser;
import org.sonar.server.user.UserUpdater;
import org.sonar.server.usergroups.DefaultGroupFinder;

import static java.lang.String.format;
import static java.util.Collections.singletonList;
import static org.sonar.core.util.stream.MoreCollectors.uniqueIndex;

public class UserRegistrarImpl implements UserRegistrar {

  private static final Logger LOGGER = Loggers.get(UserRegistrarImpl.class);
  private static final String SQ_AUTHORITY = "sonarqube";

  private final DbClient dbClient;
  private final UserUpdater userUpdater;
  private final DefaultGroupFinder defaultGroupFinder;

  public UserRegistrarImpl(DbClient dbClient, UserUpdater userUpdater, DefaultGroupFinder defaultGroupFinder) {
    this.dbClient = dbClient;
    this.userUpdater = userUpdater;
    this.defaultGroupFinder = defaultGroupFinder;
  }

  @Override
  public UserDto register(UserRegistration registration) {
    try (DbSession dbSession = dbClient.openSession(false)) {
      UserDto userDto = getUser(dbSession, registration.getUserIdentity(), registration.getProvider());
      if (userDto == null) {
        return registerNewUser(dbSession, null, registration);
      }
      if (!userDto.isActive()) {
        return registerNewUser(dbSession, userDto, registration);
      }
      return registerExistingUser(dbSession, userDto, registration);
    }
  }

  @CheckForNull
  private UserDto getUser(DbSession dbSession, UserIdentity userIdentity, IdentityProvider provider) {
    // First, try to authenticate using the external ID
    UserDto user = dbClient.userDao().selectByExternalIdAndIdentityProvider(dbSession, getProviderIdOrProviderLogin(userIdentity), provider.getKey());
    if (user != null) {
      return user;
    }
    // Then, try with the external login, for instance when external ID has changed
    return dbClient.userDao().selectByExternalLoginAndIdentityProvider(dbSession, userIdentity.getProviderLogin(), provider.getKey());
  }

  private UserDto registerNewUser(DbSession dbSession, @Nullable UserDto disabledUser, UserRegistration authenticatorParameters) {
    Optional<UserDto> otherUserToIndex = detectEmailUpdate(dbSession, authenticatorParameters);
    NewUser newUser = createNewUser(authenticatorParameters);
    if (disabledUser == null) {
      return userUpdater.createAndCommit(dbSession, newUser, beforeCommit(dbSession, authenticatorParameters), toArray(otherUserToIndex));
    }
    return userUpdater.reactivateAndCommit(dbSession, disabledUser, newUser, beforeCommit(dbSession, authenticatorParameters), toArray(otherUserToIndex));
  }

  private UserDto registerExistingUser(DbSession dbSession, UserDto userDto, UserRegistration authenticatorParameters) {
    UpdateUser update = new UpdateUser()
      .setEmail(authenticatorParameters.getUserIdentity().getEmail())
      .setName(authenticatorParameters.getUserIdentity().getName())
      .setExternalIdentity(new ExternalIdentity(
        authenticatorParameters.getProvider().getKey(),
        authenticatorParameters.getUserIdentity().getProviderLogin(),
        authenticatorParameters.getUserIdentity().getProviderId()));
    Optional<UserDto> otherUserToIndex = detectEmailUpdate(dbSession, authenticatorParameters);
    userUpdater.updateAndCommit(dbSession, userDto, update, beforeCommit(dbSession, authenticatorParameters), toArray(otherUserToIndex));
    return userDto;
  }

  private Consumer<UserDto> beforeCommit(DbSession dbSession, UserRegistration authenticatorParameters) {
    return user -> syncGroups(dbSession, authenticatorParameters.getUserIdentity(), user);
  }

  private Optional<UserDto> detectEmailUpdate(DbSession dbSession, UserRegistration authenticatorParameters) {
    String email = authenticatorParameters.getUserIdentity().getEmail();
    if (email == null) {
      return Optional.empty();
    }
    List<UserDto> existingUsers = dbClient.userDao().selectByEmail(dbSession, email);
    if (existingUsers.isEmpty()) {
      return Optional.empty();
    }
    if (existingUsers.size() > 1) {
      throw generateExistingEmailError(authenticatorParameters, email);
    }

    UserDto existingUser = existingUsers.get(0);
    if (existingUser == null || isSameUser(existingUser, authenticatorParameters)) {
      return Optional.empty();
    }
    throw generateExistingEmailError(authenticatorParameters, email);
  }

  private static boolean isSameUser(UserDto existingUser, UserRegistration authenticatorParameters) {
    // Check if same identity provider and same provider id or provider login
    return (Objects.equals(existingUser.getExternalIdentityProvider(), authenticatorParameters.getProvider().getKey()) &&
      (Objects.equals(existingUser.getExternalId(), getProviderIdOrProviderLogin(authenticatorParameters.getUserIdentity()))
        || Objects.equals(existingUser.getExternalLogin(), authenticatorParameters.getUserIdentity().getProviderLogin())));
  }

  private void syncGroups(DbSession dbSession, UserIdentity userIdentity, UserDto userDto) {
    if (!userIdentity.shouldSyncGroups()) {
      return;
    }
    String userLogin = userDto.getLogin();
    Set<String> userGroups = new HashSet<>(dbClient.groupMembershipDao().selectGroupsByLogins(dbSession, singletonList(userLogin)).get(userLogin));
    Set<String> identityGroups = userIdentity.getGroups();
    LOGGER.debug("List of groups returned by the identity provider '{}'", identityGroups);

    Collection<String> groupsToAdd = Sets.difference(identityGroups, userGroups);
    Collection<String> groupsToRemove = Sets.difference(userGroups, identityGroups);
    Collection<String> allGroups = new ArrayList<>(groupsToAdd);
    allGroups.addAll(groupsToRemove);
    Map<String, GroupDto> groupsByName = dbClient.groupDao().selectByNames(dbSession, allGroups)
      .stream()
      .collect(uniqueIndex(GroupDto::getName));

    addGroups(dbSession, userDto, groupsToAdd, groupsByName);
    removeGroups(dbSession, userDto, groupsToRemove, groupsByName);
  }

  private void addGroups(DbSession dbSession, UserDto userDto, Collection<String> groupsToAdd, Map<String, GroupDto> groupsByName) {
    groupsToAdd.stream().map(groupsByName::get).filter(Objects::nonNull).forEach(
      groupDto -> {
        LOGGER.debug("Adding group '{}' to user '{}'", groupDto.getName(), userDto.getLogin());
        dbClient.userGroupDao().insert(dbSession, new UserGroupDto().setGroupUuid(groupDto.getUuid()).setUserUuid(userDto.getUuid()));
      });
  }

  private void removeGroups(DbSession dbSession, UserDto userDto, Collection<String> groupsToRemove, Map<String, GroupDto> groupsByName) {
    Optional<GroupDto> defaultGroup = getDefaultGroup(dbSession);
    groupsToRemove.stream().map(groupsByName::get)
      .filter(Objects::nonNull)
      // user should be member of default group only when organizations are disabled, as the IdentityProvider API doesn't handle yet
      // organizations
      .filter(group -> !defaultGroup.isPresent() || !group.getUuid().equals(defaultGroup.get().getUuid()))
      .forEach(groupDto -> {
        LOGGER.debug("Removing group '{}' from user '{}'", groupDto.getName(), userDto.getLogin());
        dbClient.userGroupDao().delete(dbSession, groupDto.getUuid(), userDto.getUuid());
      });
  }

  private Optional<GroupDto> getDefaultGroup(DbSession dbSession) {
    return Optional.of(defaultGroupFinder.findDefaultGroup(dbSession));
  }

  private static NewUser createNewUser(UserRegistration authenticatorParameters) {
    String identityProviderKey = authenticatorParameters.getProvider().getKey();
    if (!authenticatorParameters.getProvider().allowsUsersToSignUp()) {
      throw AuthenticationException.newBuilder()
        .setSource(authenticatorParameters.getSource())
        .setLogin(authenticatorParameters.getUserIdentity().getProviderLogin())
        .setMessage(format("User signup disabled for provider '%s'", identityProviderKey))
        .setPublicMessage(format("'%s' users are not allowed to sign up", identityProviderKey))
        .build();
    }
    String providerLogin = authenticatorParameters.getUserIdentity().getProviderLogin();
    return NewUser.builder()
      .setLogin(SQ_AUTHORITY.equals(identityProviderKey) ? providerLogin : null)
      .setEmail(authenticatorParameters.getUserIdentity().getEmail())
      .setName(authenticatorParameters.getUserIdentity().getName())
      .setExternalIdentity(
        new ExternalIdentity(
          identityProviderKey,
          providerLogin,
          authenticatorParameters.getUserIdentity().getProviderId()))
      .build();
  }

  private static UserDto[] toArray(Optional<UserDto> userDto) {
    return userDto.map(u -> new UserDto[] {u}).orElse(new UserDto[] {});
  }

  private static AuthenticationException generateExistingEmailError(UserRegistration authenticatorParameters, String email) {
    return AuthenticationException.newBuilder()
      .setSource(authenticatorParameters.getSource())
      .setLogin(authenticatorParameters.getUserIdentity().getProviderLogin())
      .setMessage(format("Email '%s' is already used", email))
      .setPublicMessage(
        "This account is already associated with another authentication method. "
          + "Sign in using the current authentication method, "
          + "or contact your administrator to transfer your account to a different authentication method.")
      .build();
  }

  private static String getProviderIdOrProviderLogin(UserIdentity userIdentity) {
    String providerId = userIdentity.getProviderId();
    return providerId == null ? userIdentity.getProviderLogin() : providerId;
  }

}
