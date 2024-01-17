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
package org.sonar.server.platform.db.migration.version.v82;

import java.sql.SQLException;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.db.CoreDbTester;

public class RenameProjectsTableToComponentsTest {
  private static final String OLD_TABLE_NAME = "projects";
  private static final String NEW_TABLE_NAME = "components";

  @Rule
  public CoreDbTester dbTester = CoreDbTester.createForSchema(RenameProjectsTableToComponentsTest.class, "schema.sql");

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  private RenameProjectsTableToComponents underTest = new RenameProjectsTableToComponents(dbTester.database());

  @Test
  public void table_has_been_renamed() throws SQLException {
    underTest.execute();

    dbTester.assertTableDoesNotExist(OLD_TABLE_NAME);

    dbTester.assertTableExists(NEW_TABLE_NAME);
    dbTester.assertPrimaryKey(NEW_TABLE_NAME, "pk_projects", "id");

    dbTester.assertIndex(NEW_TABLE_NAME, "PROJECTS_ORGANIZATION", "organization_uuid");
    dbTester.assertUniqueIndex(NEW_TABLE_NAME, "PROJECTS_KEE", "kee");
    dbTester.assertIndex(NEW_TABLE_NAME, "PROJECTS_ROOT_UUID", "root_uuid");
    dbTester.assertUniqueIndex(NEW_TABLE_NAME, "PROJECTS_UUID", "uuid");
    dbTester.assertIndex(NEW_TABLE_NAME, "PROJECTS_PROJECT_UUID", "project_uuid");
    dbTester.assertIndex(NEW_TABLE_NAME, "PROJECTS_MODULE_UUID", "module_uuid");
    dbTester.assertIndex(NEW_TABLE_NAME, "PROJECTS_QUALIFIER", "qualifier");
  }

}
