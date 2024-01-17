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
export const enum SettingsKey {
  DaysBeforeDeletingInactiveBranchesAndPRs = 'sonar.dbcleaner.daysBeforeDeletingInactiveBranchesAndPRs',
  DefaultProjectVisibility = 'projects.default.visibility',
  ServerBaseUrl = 'sonar.core.serverBaseURL',
  PluginRiskConsent = 'sonar.plugins.risk.consent'
}

export type Setting = SettingValue & { definition: SettingDefinition };

export type SettingType =
  | 'STRING'
  | 'TEXT'
  | 'JSON'
  | 'PASSWORD'
  | 'BOOLEAN'
  | 'FLOAT'
  | 'INTEGER'
  | 'LICENSE'
  | 'LONG'
  | 'SINGLE_SELECT_LIST'
  | 'PROPERTY_SET';

export interface SettingDefinition {
  description?: string;
  key: string;
  multiValues?: boolean;
  name?: string;
  options: string[];
  type?: SettingType;
}

export interface SettingFieldDefinition extends SettingDefinition {
  description: string;
  name: string;
}

export interface SettingCategoryDefinition extends SettingDefinition {
  category: string;
  defaultValue?: string;
  deprecatedKey?: string;
  fields: SettingFieldDefinition[];
  multiValues?: boolean;
  subCategory: string;
}

export interface SettingValue {
  fieldValues?: Array<T.Dict<string>>;
  inherited?: boolean;
  key: string;
  parentFieldValues?: Array<T.Dict<string>>;
  parentValue?: string;
  parentValues?: string[];
  value?: string;
  values?: string[];
}
