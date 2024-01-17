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
package org.sonar.server.platform.db.migration.version.v84.metrics.projectmeasures;

import java.sql.SQLException;
import java.sql.Types;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.db.CoreDbTester;
import org.sonar.server.platform.db.migration.step.DdlChange;

import static org.assertj.core.api.Assertions.assertThat;

public class AddMetricUuidColumnToProjectMeasuresTest {
  @Rule
  public CoreDbTester db = CoreDbTester.createForSchema(AddMetricUuidColumnToProjectMeasuresTest.class, "schema.sql");
  private DdlChange underTest = new AddMetricUuidColumnToProjectMeasures(db.database());

  @Before
  public void setup() {
    insertProjectMeasure(1L);
    insertProjectMeasure(2L);
    insertProjectMeasure(3L);
  }

  @Test
  public void add_active_rule_uuid_column() throws SQLException {
    underTest.execute();

    db.assertColumnDefinition("project_measures", "metric_uuid", Types.VARCHAR, 40, true);

    assertThat(db.countRowsOfTable("project_measures"))
      .isEqualTo(3);
  }

  private void insertProjectMeasure(Long id) {
    db.executeInsert("project_measures",
      "uuid", "uuid" + id,
      "metric_id", id + 1,
      "component_uuid", "component" + id,
      "analysis_uuid", "analysis" + id + 1);
  }
}
