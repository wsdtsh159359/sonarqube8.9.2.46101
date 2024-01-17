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
import org.sonar.server.exceptions.BadRequestException;
import org.sonar.server.exceptions.ForbiddenException;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.ws.WsActionTester;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.groups.Tuple.tuple;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class CreateAzureActionTest {

  @Rule
  public ExpectedException expectedException = ExpectedException.none();
  @Rule
  public UserSessionRule userSession = UserSessionRule.standalone();
  @Rule
  public DbTester db = DbTester.create();

  private MultipleAlmFeatureProvider multipleAlmFeatureProvider = mock(MultipleAlmFeatureProvider.class);

  private WsActionTester ws = new WsActionTester(new CreateAzureAction(db.getDbClient(), userSession,
    new AlmSettingsSupport(db.getDbClient(), userSession, new ComponentFinder(db.getDbClient(), null),
      multipleAlmFeatureProvider)));

  @Before
  public void before() {
    when(multipleAlmFeatureProvider.enabled()).thenReturn(false);
  }

  @Test
  public void create() {
    UserDto user = db.users().insertUser();
    userSession.logIn(user).setSystemAdministrator();

    ws.newRequest()
      .setParam("key", "Azure Server - Dev Team")
      .setParam("personalAccessToken", "98765432100")
      .setParam("url", "https://ado.sonarqube.com/")
      .execute();

    assertThat(db.getDbClient().almSettingDao().selectAll(db.getSession()))
      .extracting(AlmSettingDto::getKey, AlmSettingDto::getPersonalAccessToken, AlmSettingDto::getUrl)
      .containsOnly(tuple("Azure Server - Dev Team", "98765432100", "https://ado.sonarqube.com/"));
  }

  @Test
  public void fail_when_key_is_already_used() {
    when(multipleAlmFeatureProvider.enabled()).thenReturn(true);
    UserDto user = db.users().insertUser();
    userSession.logIn(user).setSystemAdministrator();
    AlmSettingDto azureAlmSetting = db.almSettings().insertAzureAlmSetting();

    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage(String.format("An ALM setting with key '%s' already exist", azureAlmSetting.getKey()));

    ws.newRequest()
      .setParam("key", azureAlmSetting.getKey())
      .setParam("personalAccessToken", "98765432100")
      .setParam("url", "https://ado.sonarqube.com/")
      .execute();
  }

  @Test
  public void fail_when_no_multiple_instance_allowed() {
    when(multipleAlmFeatureProvider.enabled()).thenReturn(false);
    UserDto user = db.users().insertUser();
    userSession.logIn(user).setSystemAdministrator();
    db.almSettings().insertAzureAlmSetting();

    expectedException.expect(BadRequestException.class);
    expectedException.expectMessage("A AZURE_DEVOPS setting is already defined");

    ws.newRequest()
      .setParam("key", "key")
      .setParam("personalAccessToken", "98765432100")
      .setParam("url", "https://ado.sonarqube.com/")
      .execute();
  }

  @Test
  public void fail_when_missing_administer_system_permission() {
    UserDto user = db.users().insertUser();
    userSession.logIn(user);

    expectedException.expect(ForbiddenException.class);

    ws.newRequest()
      .setParam("key", "Azure Server - Dev Team")
      .setParam("personalAccessToken", "98765432100")
      .execute();
  }

  @Test
  public void definition() {
    WebService.Action def = ws.getDef();

    assertThat(def.since()).isEqualTo("8.1");
    assertThat(def.isPost()).isTrue();
    assertThat(def.params())
      .extracting(WebService.Param::key, WebService.Param::isRequired)
      .containsExactlyInAnyOrder(tuple("key", true), tuple("personalAccessToken", true), tuple("url", true));
  }
}
