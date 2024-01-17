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
package org.sonar.server.platform.db.migration.version.v84.rules.rulesparameters;

import java.sql.SQLException;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.db.CoreDbTester;
import org.sonar.server.platform.db.migration.step.MigrationStep;

public class AddIndexesToRulesParametersTableTest {

  @Rule
  public CoreDbTester db = CoreDbTester.createForSchema(AddIndexesToRulesParametersTableTest.class, "schema.sql");

  private MigrationStep underTest = new AddIndexesToRulesParametersTable(db.database());

  @Test
  public void execute() throws SQLException {
    underTest.execute();

    db.assertIndex("rules_parameters", "rules_parameters_rule_uuid", "rule_uuid");
    db.assertUniqueIndex("rules_parameters", "rules_parameters_unique", "rule_uuid", "name");
  }

  @Test
  public void migration_is_re_entrant() throws SQLException {
    underTest.execute();

    // re-entrant
    underTest.execute();

    db.assertIndex("rules_parameters", "rules_parameters_rule_uuid", "rule_uuid");
    db.assertUniqueIndex("rules_parameters", "rules_parameters_unique", "rule_uuid", "name");
  }
}
