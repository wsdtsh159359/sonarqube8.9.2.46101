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
package org.sonar.server.platform.db.migration.version.v86;

import java.sql.SQLException;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.db.CoreDbTester;
import org.sonar.server.platform.db.migration.step.MigrationStep;

public class AddIndexToApplicationProjectsTest {
  @Rule
  public CoreDbTester db = CoreDbTester.createForSchema(AddIndexToApplicationProjectsTest.class, "schema.sql");

  private final MigrationStep underTest = new AddIndexToApplicationProjects(db.database());

  @Test
  public void execute() throws SQLException {
    underTest.execute();

    db.assertUniqueIndex("app_projects", "uniq_app_projects", "application_uuid", "project_uuid");
    db.assertIndex("app_projects", "idx_app_proj_application_uuid", "application_uuid");
    db.assertIndex("app_projects", "idx_app_proj_project_uuid", "project_uuid");
  }
}
