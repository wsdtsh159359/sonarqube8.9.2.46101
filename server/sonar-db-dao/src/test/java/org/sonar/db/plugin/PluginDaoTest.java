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
package org.sonar.db.plugin;

import javax.annotation.Nullable;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.api.utils.System2;
import org.sonar.db.DbTester;
import org.sonar.db.plugin.PluginDto.Type;

import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.db.plugin.PluginDto.Type.BUNDLED;
import static org.sonar.db.plugin.PluginDto.Type.EXTERNAL;

public class PluginDaoTest {

  @Rule
  public ExpectedException thrown = ExpectedException.none();
  @Rule
  public DbTester db = DbTester.create(System2.INSTANCE);

  private PluginDao underTest = db.getDbClient().pluginDao();

  @Test
  public void selectByKey() {
    insertPlugins();

    assertThat(underTest.selectByKey(db.getSession(), "java2")).isEmpty();

    assertPlugin("java", "a", null, "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa", BUNDLED, 1500000000000L, 1600000000000L);
  }

  @Test
  public void selectAll() {
    insertPlugins();
    assertThat(underTest.selectAll(db.getSession())).hasSize(2);
  }

  @Test
  public void insert() {
    insertPlugins();

    underTest.insert(db.getSession(), new PluginDto()
      .setUuid("c")
      .setKee("javascript")
      .setBasePluginKey("java")
      .setFileHash("cccccccccccccccccccccccccccccccc")
      .setType(EXTERNAL)
      .setCreatedAt(1L)
      .setUpdatedAt(2L));

    assertPlugin("javascript", "c", "java", "cccccccccccccccccccccccccccccccc", EXTERNAL, 1L, 2L);
  }

  @Test
  public void update() {
    insertPlugins();
    PluginDto plugin = underTest.selectByKey(db.getSession(), "java").get();

    plugin.setBasePluginKey("foo");
    plugin.setFileHash("abc");
    plugin.setUpdatedAt(3L);

    underTest.update(db.getSession(), plugin);
    assertPlugin("java", "a", "foo", "abc", BUNDLED, 1500000000000L, 3L);
  }

  private void assertPlugin(String key, String uuid, @Nullable String basePluginKey, String fileHash, Type type, long cretedAt, long updatedAt) {
    PluginDto plugin = underTest.selectByKey(db.getSession(), key).get();
    assertThat(plugin.getUuid()).isEqualTo(uuid);
    assertThat(plugin.getKee()).isEqualTo(key);
    assertThat(plugin.getBasePluginKey()).isEqualTo(basePluginKey);
    assertThat(plugin.getFileHash()).isEqualTo(fileHash);
    assertThat(plugin.getType()).isEqualTo(type);
    assertThat(plugin.getCreatedAt()).isEqualTo(cretedAt);
    assertThat(plugin.getUpdatedAt()).isEqualTo(updatedAt);
  }

  private void insertPlugins() {
    insertPlugin("a", "java", null, "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa", BUNDLED, 1500000000000L, 1600000000000L);
    insertPlugin("b", "javacustom", "java", "bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb", EXTERNAL, 1500000000000L, 1600000000000L);
  }

  private void insertPlugin(String uuid, String key, @Nullable String basePluginKey, String fileHash, Type type, long createdAt, long updatedAt) {
    db.executeInsert("PLUGINS",
      "uuid", uuid,
      "kee", key,
      "base_plugin_key", basePluginKey,
      "file_hash", fileHash,
      "type", type.name(),
      "created_at", createdAt,
      "updated_at", updatedAt);
    db.commit();
  }
}
