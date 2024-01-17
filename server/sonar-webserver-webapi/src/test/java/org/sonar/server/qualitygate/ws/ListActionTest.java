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
import org.sonar.db.DbClient;
import org.sonar.db.DbTester;
import org.sonar.db.qualitygate.QualityGateDto;
import org.sonar.server.component.TestComponentFinder;
import org.sonar.server.qualitygate.QualityGateFinder;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.ws.WsActionTester;
import org.sonarqube.ws.Qualitygates.ListWsResponse.QualityGate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.tuple;
import static org.sonar.db.permission.GlobalPermission.ADMINISTER_QUALITY_GATES;
import static org.sonar.db.permission.GlobalPermission.ADMINISTER_QUALITY_PROFILES;
import static org.sonar.test.JsonAssert.assertJson;
import static org.sonarqube.ws.Qualitygates.ListWsResponse;

public class ListActionTest {

  @Rule
  public ExpectedException expectedException = ExpectedException.none();
  @Rule
  public UserSessionRule userSession = UserSessionRule.standalone();
  @Rule
  public DbTester db = DbTester.create();

  private final DbClient dbClient = db.getDbClient();
  private final QualityGateFinder qualityGateFinder = new QualityGateFinder(dbClient);

  private final WsActionTester ws = new WsActionTester(new ListAction(db.getDbClient(),
    new QualityGatesWsSupport(dbClient, userSession, TestComponentFinder.from(db)), qualityGateFinder));

  @Test
  public void list_quality_gates() {
    QualityGateDto defaultQualityGate = db.qualityGates().insertQualityGate();
    QualityGateDto otherQualityGate = db.qualityGates().insertQualityGate();
    db.qualityGates().setDefaultQualityGate(defaultQualityGate);

    ListWsResponse response = ws.newRequest()
      .executeProtobuf(ListWsResponse.class);

    assertThat(response.getQualitygatesList())
      .extracting(QualityGate::getId, QualityGate::getName, QualityGate::getIsDefault)
      .containsExactlyInAnyOrder(
        tuple(defaultQualityGate.getUuid(), defaultQualityGate.getName(), true),
        tuple(otherQualityGate.getUuid(), otherQualityGate.getName(), false));
  }

  @Test
  public void test_built_in_flag() {
    QualityGateDto qualityGate1 = db.qualityGates().insertQualityGate(qualityGate -> qualityGate.setBuiltIn(true));
    QualityGateDto qualityGate2 = db.qualityGates().insertQualityGate(qualityGate -> qualityGate.setBuiltIn(false));
    db.qualityGates().setDefaultQualityGate(qualityGate1);

    ListWsResponse response = ws.newRequest()
      .executeProtobuf(ListWsResponse.class);

    assertThat(response.getQualitygatesList())
      .extracting(QualityGate::getId, QualityGate::getIsBuiltIn)
      .containsExactlyInAnyOrder(
        tuple(qualityGate1.getUuid(), true),
        tuple(qualityGate2.getUuid(), false));
  }

  @Test
  public void test_deprecated_default_field() {
    QualityGateDto defaultQualityGate = db.qualityGates().insertQualityGate();
    db.qualityGates().setDefaultQualityGate(defaultQualityGate);

    ListWsResponse response = ws.newRequest()
      .executeProtobuf(ListWsResponse.class);

    assertThat(response.getDefault()).isEqualTo(defaultQualityGate.getUuid());
  }

  @Test
  public void no_default_quality_gate() {
    QualityGateDto qualityGate = db.qualityGates().insertQualityGate();

    expectedException.expect(IllegalStateException.class);

    ListWsResponse response = ws.newRequest()
      .executeProtobuf(ListWsResponse.class);

  }

  @Test
  public void actions_with_quality_gate_administer_permission() {
    userSession.logIn("john").addPermission(ADMINISTER_QUALITY_GATES);
    QualityGateDto defaultQualityGate = db.qualityGates().insertQualityGate(qg -> qg.setName("Default").setBuiltIn(false));
    QualityGateDto builtInQualityGate = db.qualityGates().insertQualityGate(qg -> qg.setName("Sonar way").setBuiltIn(true));
    QualityGateDto otherQualityGate = db.qualityGates().insertQualityGate(qg -> qg.setName("Sonar way - Without Coverage").setBuiltIn(false));
    db.qualityGates().setDefaultQualityGate(defaultQualityGate);

    ListWsResponse response = ws.newRequest().executeProtobuf(ListWsResponse.class);

    assertThat(response.getActions())
      .extracting(ListWsResponse.RootActions::getCreate)
      .isEqualTo(true);
    assertThat(response.getQualitygatesList())
      .extracting(QualityGate::getName,
        qg -> qg.getActions().getRename(), qg -> qg.getActions().getDelete(), qg -> qg.getActions().getManageConditions(),
        qp -> qp.getActions().getCopy(), qp -> qp.getActions().getSetAsDefault(), qp -> qp.getActions().getAssociateProjects())
      .containsExactlyInAnyOrder(
        tuple(defaultQualityGate.getName(), true, false, true, true, false, false),
        tuple(builtInQualityGate.getName(), false, false, false, true, true, true),
        tuple(otherQualityGate.getName(), true, true, true, true, true, true));
  }

  @Test
  public void actions_without_quality_gate_administer_permission() {
    userSession.logIn("john").addPermission(ADMINISTER_QUALITY_PROFILES);
    QualityGateDto defaultQualityGate = db.qualityGates().insertQualityGate(qg -> qg.setName("Sonar way").setBuiltIn(true));
    QualityGateDto otherQualityGate = db.qualityGates().insertQualityGate(qg -> qg.setName("Sonar way - Without Coverage").setBuiltIn(false));
    db.qualityGates().setDefaultQualityGate(defaultQualityGate);

    ListWsResponse response = ws.newRequest().executeProtobuf(ListWsResponse.class);

    assertThat(response.getActions())
      .extracting(ListWsResponse.RootActions::getCreate)
      .isEqualTo(false);
    assertThat(response.getQualitygatesList())
      .extracting(QualityGate::getName,
        qg -> qg.getActions().getRename(), qg -> qg.getActions().getDelete(), qg -> qg.getActions().getManageConditions(),
        qp -> qp.getActions().getCopy(), qp -> qp.getActions().getSetAsDefault(), qp -> qp.getActions().getAssociateProjects())
      .containsExactlyInAnyOrder(
        tuple(defaultQualityGate.getName(), false, false, false, false, false, false),
        tuple(otherQualityGate.getName(), false, false, false, false, false, false));
  }

  @Test
  public void json_example() {
    userSession.logIn("admin").addPermission(ADMINISTER_QUALITY_GATES);
    QualityGateDto defaultQualityGate = db.qualityGates().insertQualityGate(qualityGate -> qualityGate.setName("Sonar way").setBuiltIn(true));
    db.qualityGates().insertQualityGate(qualityGate -> qualityGate.setName("Sonar way - Without Coverage").setBuiltIn(false));
    db.qualityGates().setDefaultQualityGate(defaultQualityGate);

    String response = ws.newRequest().execute().getInput();

    assertJson(response).ignoreFields("id", "default")
      .isSimilarTo(getClass().getResource("list-example.json"));
  }

  @Test
  public void verify_definition() {
    WebService.Action action = ws.getDef();
    assertThat(action.since()).isEqualTo("4.3");
    assertThat(action.key()).isEqualTo("list");
    assertThat(action.isPost()).isFalse();
    assertThat(action.isInternal()).isFalse();
    assertThat(action.changelog()).isNotEmpty();
    assertThat(action.params()).extracting(WebService.Param::key, WebService.Param::isRequired).isEmpty();
  }

}
