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
import { shallow } from 'enzyme';
import * as React from 'react';
import { deleteQualityGate } from '../../../../api/quality-gates';
import { mockQualityGate } from '../../../../helpers/mocks/quality-gates';
import { mockRouter } from '../../../../helpers/testMocks';
import { DeleteQualityGateForm } from '../DeleteQualityGateForm';

jest.mock('../../../../api/quality-gates', () => ({
  deleteQualityGate: jest.fn().mockResolvedValue({})
}));

it('should render correctly', () => {
  expect(shallowRender()).toMatchSnapshot();
});

it('should handle onDelete', async () => {
  const onDelete = jest.fn();
  const router = mockRouter();
  const qualityGate = mockQualityGate();
  const wrapper = shallowRender({ onDelete, qualityGate, router });

  await wrapper.instance().onDelete();

  expect(deleteQualityGate).toBeCalledWith({ id: qualityGate.id });
  expect(onDelete).toBeCalled();
  expect(router.push).toBeCalled();
});

function shallowRender(overrides: Partial<DeleteQualityGateForm['props']> = {}) {
  return shallow<DeleteQualityGateForm>(
    <DeleteQualityGateForm
      onDelete={jest.fn()}
      qualityGate={mockQualityGate()}
      router={mockRouter()}
      {...overrides}
    />
  );
}
