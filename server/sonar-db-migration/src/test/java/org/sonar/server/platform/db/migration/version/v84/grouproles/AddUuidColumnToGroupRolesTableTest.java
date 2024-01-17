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
package org.sonar.server.platform.db.migration.version.v84.grouproles;

import java.sql.SQLException;
import java.sql.Types;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.core.util.UuidFactoryFast;
import org.sonar.db.CoreDbTester;
import org.sonar.server.platform.db.migration.step.DdlChange;

import static org.assertj.core.api.Assertions.assertThat;

public class AddUuidColumnToGroupRolesTableTest {

  @Rule
  public CoreDbTester db = CoreDbTester.createForSchema(AddUuidColumnToGroupRolesTableTest.class, "schema.sql");

  private DdlChange underTest = new AddUuidColumnToGroupRolesTable(db.database());

  private UuidFactoryFast uuidFactory = UuidFactoryFast.getInstance();

  @Before
  public void setup() {
    insertGroupRoles(1L);
    insertGroupRoles(2L);
    insertGroupRoles(3L);
  }

  @Test
  public void add_uuid_column() throws SQLException {
    underTest.execute();

    db.assertColumnDefinition("group_roles", "uuid", Types.VARCHAR, 40, true);

    assertThat(db.countRowsOfTable("group_roles"))
      .isEqualTo(3);
  }

  private void insertGroupRoles(Long id) {
    db.executeInsert("group_roles",
      "id", id,
      "organization_uuid", uuidFactory.create(),
      "group_id", id + 1,
      "role", id + 2,
      "component_uuid", uuidFactory.create());
  }

}
