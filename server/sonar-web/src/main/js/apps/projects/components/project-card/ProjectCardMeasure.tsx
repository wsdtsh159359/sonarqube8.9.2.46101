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
import * as classNames from 'classnames';
import * as React from 'react';
import IssueTypeIcon from 'sonar-ui-common/components/icons/IssueTypeIcon';

export interface ProjectCardMeasureProps {
  iconKey?: string;
  label: string;
  metricKey: string;
  className?: string;
}

export default function ProjectCardMeasure(
  props: React.PropsWithChildren<ProjectCardMeasureProps>
) {
  const { iconKey, label, metricKey, children, className } = props;

  const icon = <IssueTypeIcon className="spacer-right" query={iconKey || metricKey} />;

  return (
    <div
      data-key={metricKey}
      className={classNames(
        'display-flex-column overflow-hidden it__project_card_measure',
        className
      )}>
      <div className="spacer-bottom display-flex-center" title={label}>
        {icon}
        <span className="text-ellipsis">{label}</span>
      </div>
      <div className="flex-grow display-flex-center project-card-measure-value-line overflow-hidden">
        <span className="invisible">{icon}</span>
        <span className="text-ellipsis">{children}</span>
      </div>
    </div>
  );
}
