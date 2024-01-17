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
package org.sonar.server.user.ws;

import java.util.Optional;
import javax.annotation.Nullable;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.api.impl.utils.AlwaysIncreasingSystem2;
import org.sonar.api.utils.System2;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.DbTester;
import org.sonar.db.alm.setting.AlmSettingDto;
import org.sonar.db.ce.CeTaskMessageType;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.permission.template.PermissionTemplateDto;
import org.sonar.db.permission.template.PermissionTemplateUserDto;
import org.sonar.db.project.ProjectDto;
import org.sonar.db.property.PropertyDto;
import org.sonar.db.property.PropertyQuery;
import org.sonar.db.qualityprofile.QProfileDto;
import org.sonar.db.user.GroupDto;
import org.sonar.db.user.SessionTokenDto;
import org.sonar.db.user.UserDismissedMessageDto;
import org.sonar.db.user.UserDto;
import org.sonar.server.es.EsClient;
import org.sonar.server.es.EsTester;
import org.sonar.server.exceptions.BadRequestException;
import org.sonar.server.exceptions.ForbiddenException;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.exceptions.UnauthorizedException;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.user.index.UserIndexDefinition;
import org.sonar.server.user.index.UserIndexer;
import org.sonar.server.ws.TestRequest;
import org.sonar.server.ws.TestResponse;
import org.sonar.server.ws.WsActionTester;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.elasticsearch.index.query.QueryBuilders.boolQuery;
import static org.elasticsearch.index.query.QueryBuilders.termQuery;
import static org.sonar.api.web.UserRole.CODEVIEWER;
import static org.sonar.api.web.UserRole.USER;
import static org.sonar.db.permission.GlobalPermission.ADMINISTER;
import static org.sonar.db.permission.GlobalPermission.ADMINISTER_QUALITY_PROFILES;
import static org.sonar.db.permission.GlobalPermission.SCAN;
import static org.sonar.db.property.PropertyTesting.newUserPropertyDto;
import static org.sonar.server.user.index.UserIndexDefinition.FIELD_ACTIVE;
import static org.sonar.server.user.index.UserIndexDefinition.FIELD_UUID;
import static org.sonar.test.JsonAssert.assertJson;

public class DeactivateActionTest {

  private final System2 system2 = new AlwaysIncreasingSystem2();

  @Rule
  public ExpectedException expectedException = ExpectedException.none();
  @Rule
  public DbTester db = DbTester.create(system2);
  @Rule
  public EsTester es = EsTester.create();
  @Rule
  public UserSessionRule userSession = UserSessionRule.standalone();

  private final DbClient dbClient = db.getDbClient();
  private final UserIndexer userIndexer = new UserIndexer(dbClient, es.client());
  private final DbSession dbSession = db.getSession();
  private final WsActionTester ws = new WsActionTester(new DeactivateAction(dbClient, userIndexer, userSession, new UserJsonWriter(userSession)));

  @Test
  public void deactivate_user_and_delete_his_related_data() {
    createAdminUser();
    UserDto user = db.users().insertUser(u -> u
      .setLogin("ada.lovelace")
      .setEmail("ada.lovelace@noteg.com")
      .setName("Ada Lovelace")
      .setScmAccounts(singletonList("al")));
    logInAsSystemAdministrator();

    deactivate(user.getLogin());

    verifyThatUserIsDeactivated(user.getLogin());
    assertThat(es.client().search(EsClient.prepareSearch(UserIndexDefinition.TYPE_USER)
      .source(new SearchSourceBuilder()
        .query(boolQuery()
          .must(termQuery(FIELD_UUID, user.getUuid()))
          .must(termQuery(FIELD_ACTIVE, "false")))))
      .getHits().getHits()).hasSize(1);
  }

  @Test
  public void deactivate_user_deletes_his_group_membership() {
    createAdminUser();
    logInAsSystemAdministrator();
    UserDto user = db.users().insertUser();
    GroupDto group1 = db.users().insertGroup();
    db.users().insertGroup();
    db.users().insertMember(group1, user);

    deactivate(user.getLogin());

    assertThat(db.getDbClient().groupMembershipDao().selectGroupUuidsByUserUuid(dbSession, user.getUuid())).isEmpty();
  }

  @Test
  public void deactivate_user_deletes_his_tokens() {
    createAdminUser();
    logInAsSystemAdministrator();
    UserDto user = db.users().insertUser();
    db.users().insertToken(user);
    db.users().insertToken(user);
    db.commit();

    deactivate(user.getLogin());

    assertThat(db.getDbClient().userTokenDao().selectByUser(dbSession, user)).isEmpty();
  }

  @Test
  public void deactivate_user_deletes_his_properties() {
    createAdminUser();
    logInAsSystemAdministrator();
    UserDto user = db.users().insertUser();
    ComponentDto project = db.components().insertPrivateProject();
    db.properties().insertProperty(newUserPropertyDto(user));
    db.properties().insertProperty(newUserPropertyDto(user));
    db.properties().insertProperty(newUserPropertyDto(user).setComponentUuid(project.uuid()));

    deactivate(user.getLogin());

    assertThat(db.getDbClient().propertiesDao().selectByQuery(PropertyQuery.builder().setUserUuid(user.getUuid()).build(), dbSession)).isEmpty();
    assertThat(db.getDbClient().propertiesDao().selectByQuery(PropertyQuery.builder().setUserUuid(user.getUuid()).setComponentUuid(project.uuid()).build(), dbSession)).isEmpty();
  }

  @Test
  public void deactivate_user_deletes_his_permissions() {
    createAdminUser();
    logInAsSystemAdministrator();
    UserDto user = db.users().insertUser();
    ComponentDto project = db.components().insertPrivateProject();
    db.users().insertPermissionOnUser(user, SCAN);
    db.users().insertPermissionOnUser(user, ADMINISTER_QUALITY_PROFILES);
    db.users().insertProjectPermissionOnUser(user, USER, project);
    db.users().insertProjectPermissionOnUser(user, CODEVIEWER, project);

    deactivate(user.getLogin());

    assertThat(db.getDbClient().userPermissionDao().selectGlobalPermissionsOfUser(dbSession, user.getUuid())).isEmpty();
    assertThat(db.getDbClient().userPermissionDao().selectProjectPermissionsOfUser(dbSession, user.getUuid(), project.uuid())).isEmpty();
  }

  @Test
  public void deactivate_user_deletes_his_permission_templates() {
    createAdminUser();
    logInAsSystemAdministrator();
    UserDto user = db.users().insertUser();
    PermissionTemplateDto template = db.permissionTemplates().insertTemplate();
    PermissionTemplateDto anotherTemplate = db.permissionTemplates().insertTemplate();
    db.permissionTemplates().addUserToTemplate(template.getUuid(), user.getUuid(), USER);
    db.permissionTemplates().addUserToTemplate(anotherTemplate.getUuid(), user.getUuid(), CODEVIEWER);

    deactivate(user.getLogin());

    assertThat(db.getDbClient().permissionTemplateDao().selectUserPermissionsByTemplateId(dbSession, template.getUuid())).extracting(PermissionTemplateUserDto::getUserUuid)
      .isEmpty();
    assertThat(db.getDbClient().permissionTemplateDao().selectUserPermissionsByTemplateId(dbSession, anotherTemplate.getUuid())).extracting(PermissionTemplateUserDto::getUserUuid)
      .isEmpty();
  }

  @Test
  public void deactivate_user_deletes_his_qprofiles_permissions() {
    createAdminUser();
    logInAsSystemAdministrator();
    UserDto user = db.users().insertUser();
    QProfileDto profile = db.qualityProfiles().insert();
    db.qualityProfiles().addUserPermission(profile, user);

    deactivate(user.getLogin());

    assertThat(db.getDbClient().qProfileEditUsersDao().exists(dbSession, profile, user)).isFalse();
  }

  @Test
  public void deactivate_user_deletes_his_default_assignee_settings() {
    createAdminUser();
    logInAsSystemAdministrator();
    UserDto user = db.users().insertUser();
    ComponentDto project = db.components().insertPrivateProject();
    ComponentDto anotherProject = db.components().insertPrivateProject();
    db.properties().insertProperty(new PropertyDto().setKey("sonar.issues.defaultAssigneeLogin").setValue(user.getLogin()).setComponentUuid(project.uuid()));
    db.properties().insertProperty(new PropertyDto().setKey("sonar.issues.defaultAssigneeLogin").setValue(user.getLogin()).setComponentUuid(anotherProject.uuid()));
    db.properties().insertProperty(new PropertyDto().setKey("other").setValue(user.getLogin()).setComponentUuid(anotherProject.uuid()));

    deactivate(user.getLogin());

    assertThat(db.getDbClient().propertiesDao().selectByQuery(PropertyQuery.builder().setKey("sonar.issues.defaultAssigneeLogin").build(), db.getSession())).isEmpty();
    assertThat(db.getDbClient().propertiesDao().selectByQuery(PropertyQuery.builder().build(), db.getSession())).extracting(PropertyDto::getKey).containsOnly("other");
  }

  @Test
  public void deactivate_user_deletes_his_user_settings() {
    createAdminUser();
    logInAsSystemAdministrator();
    UserDto user = db.users().insertUser();
    db.users().insertUserSetting(user);
    db.users().insertUserSetting(user);
    UserDto anotherUser = db.users().insertUser();
    db.users().insertUserSetting(anotherUser);

    deactivate(user.getLogin());

    assertThat(db.getDbClient().userPropertiesDao().selectByUser(dbSession, user)).isEmpty();
    assertThat(db.getDbClient().userPropertiesDao().selectByUser(dbSession, anotherUser)).hasSize(1);
  }

  @Test
  public void deactivate_user_deletes_his_alm_pat() {
    createAdminUser();
    logInAsSystemAdministrator();
    AlmSettingDto almSettingDto = db.almSettings().insertBitbucketAlmSetting();
    UserDto user = db.users().insertUser();
    db.almPats().insert(p -> p.setUserUuid(user.getUuid()), p -> p.setAlmSettingUuid(almSettingDto.getUuid()));
    UserDto anotherUser = db.users().insertUser();
    db.almPats().insert(p -> p.setUserUuid(anotherUser.getUuid()), p -> p.setAlmSettingUuid(almSettingDto.getUuid()));

    deactivate(user.getLogin());

    assertThat(db.getDbClient().almPatDao().selectByUserAndAlmSetting(dbSession, user.getUuid(), almSettingDto)).isEmpty();
    assertThat(db.getDbClient().almPatDao().selectByUserAndAlmSetting(dbSession, anotherUser.getUuid(), almSettingDto)).isNotNull();
  }

  @Test
  public void deactivate_user_deletes_his_session_tokens() {
    createAdminUser();
    logInAsSystemAdministrator();
    UserDto user = db.users().insertUser();
    SessionTokenDto sessionToken1 = db.users().insertSessionToken(user);
    SessionTokenDto sessionToken2 = db.users().insertSessionToken(user);
    UserDto anotherUser = db.users().insertUser();
    SessionTokenDto sessionToken3 = db.users().insertSessionToken(anotherUser);

    deactivate(user.getLogin());

    assertThat(db.getDbClient().sessionTokensDao().selectByUuid(dbSession, sessionToken1.getUuid())).isNotPresent();
    assertThat(db.getDbClient().sessionTokensDao().selectByUuid(dbSession, sessionToken2.getUuid())).isNotPresent();
    assertThat(db.getDbClient().sessionTokensDao().selectByUuid(dbSession, sessionToken3.getUuid())).isPresent();
  }

  @Test
  public void deactivate_user_deletes_his_dismissed_messages() {
    createAdminUser();
    logInAsSystemAdministrator();
    ProjectDto project1 = db.components().insertPrivateProjectDto();
    ProjectDto project2 = db.components().insertPrivateProjectDto();
    UserDto user = db.users().insertUser();

    db.users().insertUserDismissedMessage(user, project1, CeTaskMessageType.SUGGEST_DEVELOPER_EDITION_UPGRADE);
    db.users().insertUserDismissedMessage(user, project2, CeTaskMessageType.SUGGEST_DEVELOPER_EDITION_UPGRADE);
    UserDto anotherUser = db.users().insertUser();
    UserDismissedMessageDto msg3 = db.users().insertUserDismissedMessage(anotherUser, project1, CeTaskMessageType.SUGGEST_DEVELOPER_EDITION_UPGRADE);
    UserDismissedMessageDto msg4 = db.users().insertUserDismissedMessage(anotherUser, project2, CeTaskMessageType.SUGGEST_DEVELOPER_EDITION_UPGRADE);

    deactivate(user.getLogin());

    assertThat(db.getDbClient().userDismissedMessagesDao().selectByUser(dbSession, user)).isEmpty();
    assertThat(db.getDbClient().userDismissedMessagesDao().selectByUser(dbSession, anotherUser))
      .extracting(UserDismissedMessageDto::getUuid)
      .containsExactlyInAnyOrder(msg3.getUuid(), msg4.getUuid());
  }

  @Test
  public void user_cannot_deactivate_itself_on_sonarqube() {
    createAdminUser();
    UserDto user = db.users().insertUser();
    userSession.logIn(user.getLogin()).setSystemAdministrator();

    expectedException.expect(BadRequestException.class);
    expectedException.expectMessage("Self-deactivation is not possible");

    deactivate(user.getLogin());

    verifyThatUserExists(user.getLogin());
  }

  @Test
  public void deactivation_requires_to_be_logged_in() {
    createAdminUser();

    expectedException.expect(UnauthorizedException.class);
    expectedException.expectMessage("Authentication is required");

    deactivate("someone");
  }

  @Test
  public void deactivation_requires_administrator_permission_on_sonarqube() {
    createAdminUser();
    userSession.logIn();

    expectedException.expect(ForbiddenException.class);
    expectedException.expectMessage("Insufficient privileges");

    deactivate("someone");
  }

  @Test
  public void fail_if_user_does_not_exist() {
    createAdminUser();
    logInAsSystemAdministrator();

    expectedException.expect(NotFoundException.class);
    expectedException.expectMessage("User 'someone' doesn't exist");

    deactivate("someone");
  }

  @Test
  public void fail_if_login_is_blank() {
    createAdminUser();
    logInAsSystemAdministrator();

    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("The 'login' parameter is missing");

    deactivate("");
  }

  @Test
  public void fail_if_login_is_missing() {
    createAdminUser();
    logInAsSystemAdministrator();

    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("The 'login' parameter is missing");

    deactivate(null);
  }

  @Test
  public void fail_to_deactivate_last_administrator() {
    UserDto admin = db.users().insertUser();
    db.users().insertPermissionOnUser(admin, ADMINISTER);
    logInAsSystemAdministrator();

    expectedException.expect(BadRequestException.class);
    expectedException.expectMessage("User is last administrator, and cannot be deactivated");

    deactivate(admin.getLogin());
  }

  @Test
  public void administrators_can_be_deactivated_if_there_are_still_other_administrators() {
    UserDto admin = createAdminUser();
    ;
    UserDto anotherAdmin = createAdminUser();
    logInAsSystemAdministrator();

    deactivate(admin.getLogin());

    verifyThatUserIsDeactivated(admin.getLogin());
    verifyThatUserExists(anotherAdmin.getLogin());
  }

  @Test
  public void test_definition() {
    assertThat(ws.getDef().isPost()).isTrue();
    assertThat(ws.getDef().isInternal()).isFalse();
    assertThat(ws.getDef().params()).hasSize(1);
  }

  @Test
  public void test_example() {
    createAdminUser();
    UserDto user = db.users().insertUser(u -> u
      .setLogin("ada.lovelace")
      .setEmail("ada.lovelace@noteg.com")
      .setName("Ada Lovelace")
      .setLocal(true)
      .setScmAccounts(singletonList("al")));
    logInAsSystemAdministrator();

    String json = deactivate(user.getLogin()).getInput();

    assertJson(json).isSimilarTo(ws.getDef().responseExampleAsString());
  }

  private void logInAsSystemAdministrator() {
    userSession.logIn().setSystemAdministrator();
  }

  private TestResponse deactivate(@Nullable String login) {
    return deactivate(ws, login);
  }

  private TestResponse deactivate(WsActionTester ws, @Nullable String login) {
    TestRequest request = ws.newRequest()
      .setMethod("POST");
    Optional.ofNullable(login).ifPresent(t -> request.setParam("login", login));
    return request.execute();
  }

  private void verifyThatUserExists(String login) {
    assertThat(db.users().selectUserByLogin(login)).isPresent();
  }

  private void verifyThatUserIsDeactivated(String login) {
    Optional<UserDto> user = db.users().selectUserByLogin(login);
    assertThat(user).isPresent();
    assertThat(user.get().isActive()).isFalse();
    assertThat(user.get().getEmail()).isNull();
    assertThat(user.get().getScmAccountsAsList()).isEmpty();
  }

  private UserDto createAdminUser() {
    UserDto admin = db.users().insertUser();
    db.users().insertPermissionOnUser(admin, ADMINISTER);
    db.commit();
    return admin;
  }

}
