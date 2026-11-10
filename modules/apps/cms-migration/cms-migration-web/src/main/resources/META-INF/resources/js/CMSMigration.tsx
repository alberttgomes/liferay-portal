/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import ClayLayout from '@clayui/layout';
import React from 'react';

interface IProps {
	title: string;
}

export default function CMSMigration({title}: IProps) {
	return (
		<ClayLayout.ContainerFluid formSize="lg">
			<ClayLayout.Sheet>
				<ClayLayout.SheetHeader>
					<h2 className="sheet-title">{title}</h2>

					<div className="sheet-text">
						{Liferay.Language.get('cms-migration-description')}
					</div>
				</ClayLayout.SheetHeader>
			</ClayLayout.Sheet>
		</ClayLayout.ContainerFluid>
	);
}
