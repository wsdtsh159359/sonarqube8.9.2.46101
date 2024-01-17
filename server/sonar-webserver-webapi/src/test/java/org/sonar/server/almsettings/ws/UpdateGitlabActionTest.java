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
package org.sonar.server.almsettings.ws;

import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.api.server.ws.WebService;
import org.sonar.db.DbTester;
import org.sonar.db.alm.setting.AlmSettingDto;
import org.sonar.db.user.UserDto;
import org.sonar.server.almsettings.MultipleAlmFeatureProvider;
import org.sonar.server.component.ComponentFinder;
import org.sonar.server.exceptions.ForbiddenException;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.ws.TestRequest;
import org.sonar.server.ws.WsActionTester;

import static java.lang.String.format;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.groups.Tuple.tuple;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class UpdateGitlabActionTest {

  @Rule
  public ExpectedException expectedException = ExpectedException.none();
  @Rule
  public UserSessionRule userSession = UserSessionRule.standalone();
  @Rule
  public DbTester db = DbTester.create();

  private static String GITLAB_URL = "gitlab.com/api/v4";

  private MultipleAlmFeatureProvider multipleAlmFeatureProvider = mock(MultipleAlmFeatureProvider.class);

  private WsActionTester ws = new WsActionTester(new UpdateGitlabAction(db.getDbClient(), userSession,
    new AlmSettingsSupport(db.getDbClient(), userSession, new ComponentFinder(db.getDbClient(), null), multipleAlmFeatureProvider)));

  @Before
  public void before() {
    when(multipleAlmFeatureProvider.enabled()).thenReturn(true);
  }

  @Test
  public void update_without_url() {
    UserDto user = db.users().insertUser();
    userSession.logIn(user).setSystemAdministrator();

    TestRequest request = ws.newRequest()
      .setParam("key", "Gitlab - Dev Team")
      .setParam("personalAccessToken", "98765432100");

    Assertions.assertThatThrownBy(() -> request.execute())
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("The 'url' parameter is missing");
  }

  @Test
  public void update_with_url() {
    UserDto user = db.users().insertUser();
    userSession.logIn(user).setSystemAdministrator();

    AlmSettingDto almSettingDto = db.almSettings().insertGitlabAlmSetting();

    ws.newRequest()
      .setParam("key", almSettingDto.getKey())
      .setParam("url", GITLAB_URL)
      .setParam("personalAccessToken", "10987654321")
      .execute();
    assertThat(db.getDbClient().almSettingDao().selectAll(db.getSession()))
      .extracting(AlmSettingDto::getKey, AlmSettingDto::getUrl, AlmSettingDto::getPersonalAccessToken)
      .containsOnly(tuple(almSettingDto.getKey(), GITLAB_URL, "10987654321"));
  }

  @Test
  public void update_with_new_key() {
    UserDto user = db.users().insertUser();
    userSession.logIn(user).setSystemAdministrator();

    AlmSettingDto almSettingDto = db.almSettings().insertGitlabAlmSetting();

    ws.newRequest()
      .setParam("key", almSettingDto.getKey())
      .setParam("newKey", "Gitlab - Infra Team")
      .setParam("personalAccessToken", "0123456789")
      .setParam("url", GITLAB_URL)
      .execute();

    assertThat(db.getDbClient().almSettingDao().selectAll(db.getSession()))
      .extracting(AlmSettingDto::getKey, AlmSettingDto::getPersonalAccessToken, AlmSettingDto::getUrl)
      .containsOnly(tuple("Gitlab - Infra Team", "0123456789", GITLAB_URL));
  }

  @Test
  public void update_without_pat() {
    UserDto user = db.users().insertUser();
    userSession.logIn(user).setSystemAdministrator();

    AlmSettingDto almSettingDto = db.almSettings().insertGitlabAlmSetting();

    ws.newRequest()
      .setParam("key", almSettingDto.getKey())
      .setParam("url", GITLAB_URL)
      .execute();
    assertThat(db.getDbClient().almSettingDao().selectAll(db.getSession()))
      .extracting(AlmSettingDto::getKey, AlmSettingDto::getUrl, AlmSettingDto::getPersonalAccessToken)
      .containsOnly(tuple(almSettingDto.getKey(), GITLAB_URL, almSettingDto.getPersonalAccessToken()));
  }

  @Test
  public void fail_when_key_does_not_match_existing_alm_setting() {
    UserDto user = db.users().insertUser();
    userSession.logIn(user).setSystemAdministrator();

    expectedException.expect(NotFoundException.class);
    expectedException.expectMessage("ALM setting with key 'unknown' cannot be found");

    ws.newRequest()
      .setParam("key", "unknown")
      .setParam("personalAccessToken", "0123456789")
      .setParam("url", GITLAB_URL)
      .execute();
  }

  @Test
  public void fail_when_new_key_matches_existing_alm_setting() {
    UserDto user = db.users().insertUser();
    userSession.logIn(user).setSystemAdministrator();
    AlmSettingDto almSetting1 = db.almSettings().insertGitlabAlmSetting();
    AlmSettingDto almSetting2 = db.almSettings().insertGitlabAlmSetting();

    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage(format("An ALM setting with key '%s' already exists", almSetting2.getKey()));

    ws.newRequest()
      .setParam("key", almSetting1.getKey())
      .setParam("newKey", almSetting2.getKey())
      .setParam("personalAccessToken", "0123456789")
      .setParam("url", GITLAB_URL)
      .execute();
  }

  @Test
  public void fail_when_missing_administer_system_permission() {
    UserDto user = db.users().insertUser();
    userSession.logIn(user);
    AlmSettingDto almSettingDto = db.almSettings().insertGitlabAlmSetting();

    expectedException.expect(ForbiddenException.class);

    ws.newRequest()
      .setParam("key", almSettingDto.getKey())
      .setParam("newKey", "Gitlab - Infra Team")
      .setParam("personalAccessToken", "0123456789")
      .setParam("url", GITLAB_URL)
      .execute();
  }

  @Test
  public void definition() {
    WebService.Action def = ws.getDef();

    assertThat(def.since()).isEqualTo("8.1");
    assertThat(def.isPost()).isTrue();
    assertThat(def.params())
      .extracting(WebService.Param::key, WebService.Param::isRequired)
      .containsExactlyInAnyOrder(tuple("key", true), tuple("newKey", false), tuple("personalAccessToken", false), tuple("url", true));
  }

}
