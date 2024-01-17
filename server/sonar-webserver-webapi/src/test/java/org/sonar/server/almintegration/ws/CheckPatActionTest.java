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
package org.sonar.server.almintegration.ws;

import org.junit.Rule;
import org.junit.Test;
import org.sonar.alm.client.azure.AzureDevOpsHttpClient;
import org.sonar.alm.client.bitbucketserver.BitbucketServerRestClient;
import org.sonar.alm.client.gitlab.GitlabHttpClient;
import org.sonar.api.server.ws.WebService;
import org.sonar.db.DbTester;
import org.sonar.db.alm.pat.AlmPatDto;
import org.sonar.db.alm.setting.AlmSettingDto;
import org.sonar.db.user.UserDto;
import org.sonar.server.exceptions.ForbiddenException;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.exceptions.UnauthorizedException;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.ws.TestRequest;
import org.sonar.server.ws.WsActionTester;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.sonar.db.alm.integration.pat.AlmPatsTesting.newAlmPatDto;
import static org.sonar.db.permission.GlobalPermission.PROVISION_PROJECTS;

public class CheckPatActionTest {

  public static final String PAT_SECRET = "pat-secret";
  @Rule
  public UserSessionRule userSession = UserSessionRule.standalone();
  @Rule
  public DbTester db = DbTester.create();

  private final AzureDevOpsHttpClient azureDevOpsPrHttpClient = mock(AzureDevOpsHttpClient.class);
  private final BitbucketServerRestClient bitbucketServerRestClient = mock(BitbucketServerRestClient.class);
  private final GitlabHttpClient gitlabPrHttpClient = mock(GitlabHttpClient.class);
  private final WsActionTester ws = new WsActionTester(new CheckPatAction(db.getDbClient(), userSession, azureDevOpsPrHttpClient, bitbucketServerRestClient, gitlabPrHttpClient));

  @Test
  public void check_pat_for_github() {
    UserDto user = db.users().insertUser();
    userSession.logIn(user).addPermission(PROVISION_PROJECTS);
    AlmSettingDto almSetting = db.almSettings().insertGitHubAlmSetting();
    db.almPats().insert(dto -> {
      dto.setAlmSettingUuid(almSetting.getUuid());
      dto.setUserUuid(user.getUuid());
    });

    TestRequest request = ws.newRequest().setParam("almSetting", almSetting.getKey());
    assertThatThrownBy(request::execute)
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("unsupported ALM GITHUB");
  }

  @Test
  public void check_pat_for_azure_devops() {
    UserDto user = db.users().insertUser();
    userSession.logIn(user).addPermission(PROVISION_PROJECTS);
    AlmSettingDto almSetting = db.almSettings().insertAzureAlmSetting();
    db.almPats().insert(dto -> {
      dto.setAlmSettingUuid(almSetting.getUuid());
      dto.setUserUuid(user.getUuid());
      dto.setPersonalAccessToken(PAT_SECRET);
    });

    ws.newRequest()
      .setParam("almSetting", almSetting.getKey())
      .execute();

    assertThat(almSetting.getUrl()).isNotNull();
    verify(azureDevOpsPrHttpClient).checkPAT(almSetting.getUrl(), PAT_SECRET);
  }

  @Test
  public void check_pat_for_bitbucket() {
    UserDto user = db.users().insertUser();
    userSession.logIn(user).addPermission(PROVISION_PROJECTS);
    AlmSettingDto almSetting = db.almSettings().insertBitbucketAlmSetting();
    db.almPats().insert(dto -> {
      dto.setAlmSettingUuid(almSetting.getUuid());
      dto.setUserUuid(user.getUuid());
      dto.setPersonalAccessToken(PAT_SECRET);
    });

    ws.newRequest()
      .setParam("almSetting", almSetting.getKey())
      .execute();

    assertThat(almSetting.getUrl()).isNotNull();
    verify(bitbucketServerRestClient).getRecentRepo(almSetting.getUrl(), PAT_SECRET);
  }

  @Test
  public void check_pat_for_gitlab() {
    UserDto user = db.users().insertUser();
    userSession.logIn(user).addPermission(PROVISION_PROJECTS);
    AlmSettingDto almSetting = db.almSettings().insertGitlabAlmSetting();
    db.almPats().insert(dto -> {
      dto.setAlmSettingUuid(almSetting.getUuid());
      dto.setUserUuid(user.getUuid());
      dto.setPersonalAccessToken(PAT_SECRET);
    });

    ws.newRequest()
      .setParam("almSetting", almSetting.getKey())
      .execute();

    assertThat(almSetting.getUrl()).isNotNull();
    verify(gitlabPrHttpClient).searchProjects(almSetting.getUrl(), PAT_SECRET, null, null, null);
  }

  @Test
  public void fail_when_personal_access_token_is_invalid_for_bitbucket() {
    when(bitbucketServerRestClient.getRecentRepo(any(), any())).thenThrow(new IllegalArgumentException("Invalid personal access token"));
    UserDto user = db.users().insertUser();
    userSession.logIn(user).addPermission(PROVISION_PROJECTS);
    AlmSettingDto almSetting = db.almSettings().insertBitbucketAlmSetting();
    db.almPats().insert(dto -> {
      dto.setAlmSettingUuid(almSetting.getUuid());
      dto.setUserUuid(user.getUuid());
    });

    TestRequest request = ws.newRequest().setParam("almSetting", almSetting.getKey());
    assertThatThrownBy(request::execute)
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("Invalid personal access token");
  }

  @Test
  public void fail_when_personal_access_token_is_invalid_for_gitlab() {
    when(gitlabPrHttpClient.searchProjects(any(), any(), any(), any(), any()))
      .thenThrow(new IllegalArgumentException("Invalid personal access token"));
    UserDto user = db.users().insertUser();
    userSession.logIn(user).addPermission(PROVISION_PROJECTS);
    AlmSettingDto almSetting = db.almSettings().insertGitlabAlmSetting();
    db.almPats().insert(dto -> {
      dto.setAlmSettingUuid(almSetting.getUuid());
      dto.setUserUuid(user.getUuid());
    });

    TestRequest request = ws.newRequest().setParam("almSetting", almSetting.getKey());
    assertThatThrownBy(request::execute)
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("Invalid personal access token");
  }

  @Test
  public void fail_when_not_logged_in() {
    TestRequest request = ws.newRequest().setParam("almSetting", "anyvalue");
    assertThatThrownBy(request::execute)
      .isInstanceOf(UnauthorizedException.class);
  }

  @Test
  public void fail_when_no_creation_project_permission() {
    UserDto user = db.users().insertUser();
    userSession.logIn(user);

    TestRequest request = ws.newRequest().setParam("almSetting", "anyvalue");
    assertThatThrownBy(request::execute)
      .isInstanceOf(ForbiddenException.class)
      .hasMessage("Insufficient privileges");
  }

  @Test
  public void check_pat_is_missing() {
    UserDto user = db.users().insertUser();
    userSession.logIn(user).addPermission(PROVISION_PROJECTS);
    AlmSettingDto almSetting = db.almSettings().insertBitbucketAlmSetting();

    TestRequest request = ws.newRequest().setParam("almSetting", almSetting.getKey());
    assertThatThrownBy(request::execute)
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("personal access token for '" + almSetting.getKey() + "' is missing");
  }

  @Test
  public void fail_check_pat_alm_setting_not_found() {
    UserDto user = db.users().insertUser();
    userSession.logIn(user).addPermission(PROVISION_PROJECTS);
    AlmPatDto almPatDto = newAlmPatDto();
    db.getDbClient().almPatDao().insert(db.getSession(), almPatDto);

    TestRequest request = ws.newRequest().setParam("almSetting", "testKey");
    assertThatThrownBy(request::execute)
      .isInstanceOf(NotFoundException.class)
      .hasMessage("ALM Setting 'testKey' not found");
  }

  @Test
  public void definition() {
    WebService.Action def = ws.getDef();

    assertThat(def.since()).isEqualTo("8.2");
    assertThat(def.isPost()).isFalse();
    assertThat(def.isInternal()).isTrue();
    assertThat(def.params())
      .extracting(WebService.Param::key, WebService.Param::isRequired)
      .containsExactlyInAnyOrder(tuple("almSetting", true));
  }

}
