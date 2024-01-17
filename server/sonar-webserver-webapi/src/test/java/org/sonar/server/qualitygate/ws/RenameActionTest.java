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
package org.sonar.server.qualitygate.ws;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.utils.System2;
import org.sonar.db.DbTester;
import org.sonar.db.qualitygate.QualityGateDto;
import org.sonar.server.component.TestComponentFinder;
import org.sonar.server.exceptions.ForbiddenException;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.ws.WsActionTester;
import org.sonarqube.ws.Qualitygates.QualityGate;

import static java.lang.String.format;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.tuple;
import static org.sonar.db.permission.GlobalPermission.ADMINISTER_QUALITY_GATES;
import static org.sonar.db.permission.GlobalPermission.ADMINISTER_QUALITY_PROFILES;

public class RenameActionTest {

  @Rule
  public ExpectedException expectedException = ExpectedException.none();
  @Rule
  public UserSessionRule userSession = UserSessionRule.standalone();
  @Rule
  public DbTester db = DbTester.create(System2.INSTANCE);

  private final WsActionTester ws = new WsActionTester(
    new RenameAction(db.getDbClient(), new QualityGatesWsSupport(db.getDbClient(), userSession, TestComponentFinder.from(db))));

  @Test
  public void verify_definition() {
    WebService.Action action = ws.getDef();
    assertThat(action.key()).isEqualTo("rename");
    assertThat(action.since()).isEqualTo("4.3");
    assertThat(action.changelog()).isNotEmpty();
    assertThat(action.params())
      .extracting(WebService.Param::key, WebService.Param::isRequired)
      .containsExactlyInAnyOrder(
        tuple("id", false),
        tuple("currentName", false),
        tuple("name", true));
  }

  @Test
  public void rename() {
    QualityGateDto qualityGate = db.qualityGates().insertQualityGate(qg -> qg.setName("old name"));
    userSession.logIn("john").addPermission(ADMINISTER_QUALITY_GATES);

    ws.newRequest()
      .setParam("id", qualityGate.getUuid())
      .setParam("name", "new name")
      .execute();

    assertThat(db.getDbClient().qualityGateDao().selectByUuid(db.getSession(), qualityGate.getUuid()).getName()).isEqualTo("new name");
  }

  @Test
  public void response_contains_quality_gate() {
    userSession.logIn("john").addPermission(ADMINISTER_QUALITY_GATES);
    QualityGateDto qualityGate = db.qualityGates().insertQualityGate(qg -> qg.setName("old name"));

    QualityGate result = ws.newRequest()
      .setParam("id", qualityGate.getUuid())
      .setParam("name", "new name")
      .executeProtobuf(QualityGate.class);

    assertThat(result.getId()).isEqualTo(qualityGate.getUuid());
    assertThat(result.getName()).isEqualTo("new name");
  }

  @Test
  public void rename_with_same_name() {
    userSession.logIn("john").addPermission(ADMINISTER_QUALITY_GATES);
    QualityGateDto qualityGate = db.qualityGates().insertQualityGate(qg -> qg.setName("name"));

    ws.newRequest()
      .setParam("id", qualityGate.getUuid())
      .setParam("name", "name")
      .execute();

    assertThat(db.getDbClient().qualityGateDao().selectByUuid(db.getSession(), qualityGate.getUuid()).getName()).isEqualTo("name");
  }

  @Test
  public void fail_on_built_in_quality_gate() {
    userSession.logIn("john").addPermission(ADMINISTER_QUALITY_GATES);
    QualityGateDto qualityGate = db.qualityGates().insertQualityGate(qg -> qg.setBuiltIn(true));

    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage(format("Operation forbidden for built-in Quality Gate '%s'", qualityGate.getName()));

    ws.newRequest()
      .setParam("id", qualityGate.getUuid())
      .setParam("name", "name")
      .execute();
  }

  @Test
  public void fail_on_empty_name() {
    userSession.logIn("john").addPermission(ADMINISTER_QUALITY_GATES);
    QualityGateDto qualityGate = db.qualityGates().insertQualityGate();

    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("The 'name' parameter is missing");

    ws.newRequest()
      .setParam("id", qualityGate.getUuid())
      .setParam("name", "")
      .execute();
  }

  @Test
  public void fail_when_using_existing_name() {
    userSession.logIn("john").addPermission(ADMINISTER_QUALITY_GATES);
    QualityGateDto qualityGate1 = db.qualityGates().insertQualityGate();
    QualityGateDto qualityGate2 = db.qualityGates().insertQualityGate();

    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage(format("Name '%s' has already been taken", qualityGate2.getName()));

    ws.newRequest()
      .setParam("id", qualityGate1.getUuid())
      .setParam("name", qualityGate2.getName())
      .execute();
  }

  @Test
  public void fail_on_unknown_quality_gate() {
    userSession.logIn("john").addPermission(ADMINISTER_QUALITY_GATES);

    expectedException.expect(NotFoundException.class);

    ws.newRequest()
      .setParam("id", "123")
      .setParam("name", "new name")
      .execute();
  }

  @Test
  public void fail_when_not_quality_gates_administer() {
    userSession.logIn("john").addPermission(ADMINISTER_QUALITY_PROFILES);
    QualityGateDto qualityGate = db.qualityGates().insertQualityGate(qg -> qg.setName("old name"));

    expectedException.expect(ForbiddenException.class);

    ws.newRequest()
      .setParam("id", qualityGate.getUuid())
      .setParam("name", "new name")
      .execute();
  }

}
