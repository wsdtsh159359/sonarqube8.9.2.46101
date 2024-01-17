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
package org.sonar.server.platform.db.migration.version.v84.metrics.qualitygateconditions;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import org.assertj.core.groups.Tuple;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.db.CoreDbTester;
import org.sonar.server.platform.db.migration.step.DataChange;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

public class PopulateQualityGateConditionsMetricUuidTest {
  @Rule
  public CoreDbTester db = CoreDbTester.createForSchema(PopulateQualityGateConditionsMetricUuidTest.class, "schema.sql");

  private DataChange underTest = new PopulateQualityGateConditionsMetricUuid(db.database());

  @Test
  public void populate_uuids() throws SQLException {
    insertMetric(1L);
    insertMetric(2L);
    insertMetric(3L);

    insertQualityGateConditions(4L, 1L);
    insertQualityGateConditions(5L, 2L);
    insertQualityGateConditions(6L, 3L);

    underTest.execute();

    assertThatTableContains(
      tuple("uuid4", 1L, "uuid1"),
      tuple("uuid5", 2L, "uuid2"),
      tuple("uuid6", 3L, "uuid3")
    );
  }

  @Test
  public void delete_entries_with_null_id() throws SQLException {
    insertMetric(1L);
    insertMetric(2L);
    insertMetric(3L);

    insertQualityGateConditions(4L, null);
    insertQualityGateConditions(5L, 2L);
    insertQualityGateConditions(6L, 3L);

    underTest.execute();
    // re-entrant
    underTest.execute();

    assertThatTableContains(
      tuple("uuid5", 2L, "uuid2"),
      tuple("uuid6", 3L, "uuid3")
    );
  }

  @Test
  public void delete_rows_with_orphan_ids() throws SQLException {
    insertMetric(1L);
    insertMetric(2L);
    insertMetric(3L);

    insertQualityGateConditions(4L, 10L);
    insertQualityGateConditions(5L, 2L);
    insertQualityGateConditions(6L, 3L);

    underTest.execute();
    // re-entrant
    underTest.execute();

    assertThatTableContains(
      tuple("uuid5", 2L, "uuid2"),
      tuple("uuid6", 3L, "uuid3")
    );
  }

  @Test
  public void migration_is_reentrant() throws SQLException {
    insertMetric(1L);
    insertMetric(2L);
    insertMetric(3L);

    insertQualityGateConditions(4L, 1L);
    insertQualityGateConditions(5L, 2L);
    insertQualityGateConditions(6L, 3L);

    underTest.execute();
    // re-entrant
    underTest.execute();

    assertThatTableContains(
      tuple("uuid4", 1L, "uuid1"),
      tuple("uuid5", 2L, "uuid2"),
      tuple("uuid6", 3L, "uuid3")
    );
  }

  private void assertThatTableContains(Tuple... tuples) {
    List<Map<String, Object>> select = db.select("select uuid, metric_id, metric_uuid from quality_gate_conditions");
    assertThat(select).extracting(m -> m.get("UUID"), m -> m.get("METRIC_ID"), m -> m.get("METRIC_UUID"))
      .containsExactlyInAnyOrder(tuples);
  }

  private void insertMetric(Long id) {
    db.executeInsert("metrics",
      "id", id,
      "uuid", "uuid" + id,
      "name", "name" + id);
  }

  private void insertQualityGateConditions(Long id, Long metricId) {
    db.executeInsert("quality_gate_conditions",
      "uuid", "uuid" + id,
      "metric_id", metricId);
  }
}
