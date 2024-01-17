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
import * as React from 'react';
import { Button } from 'sonar-ui-common/components/controls/buttons';
import ModalButton from 'sonar-ui-common/components/controls/ModalButton';
import { translate } from 'sonar-ui-common/helpers/l10n';
import DocumentationTooltip from '../../../components/common/DocumentationTooltip';
import CreateQualityGateForm from './CreateQualityGateForm';

interface Props {
  canCreate: boolean;
  refreshQualityGates: () => Promise<void>;
}

export default function ListHeader({ canCreate, refreshQualityGates }: Props) {
  return (
    <header className="page-header">
      {canCreate && (
        <div className="page-actions">
          <ModalButton
            modal={({ onClose }) => (
              <CreateQualityGateForm onClose={onClose} onCreate={refreshQualityGates} />
            )}>
            {({ onClick }) => (
              <Button data-test="quality-gates__add" onClick={onClick}>
                {translate('create')}
              </Button>
            )}
          </ModalButton>
        </div>
      )}

      <div className="display-flex-center">
        <h1 className="page-title">{translate('quality_gates.page')}</h1>
        <DocumentationTooltip
          className="spacer-left"
          content={translate('quality_gates.help')}
          links={[
            {
              href: '/documentation/user-guide/quality-gates/',
              label: translate('learn_more')
            }
          ]}
        />
      </div>
    </header>
  );
}
