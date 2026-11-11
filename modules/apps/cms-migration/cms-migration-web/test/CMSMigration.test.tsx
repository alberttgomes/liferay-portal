/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import {render, screen} from '@testing-library/react';
import React from 'react';

import '@testing-library/jest-dom';

import CMSMigration from '../src/main/resources/META-INF/resources/js/CMSMigration';

describe('CMSMigration', () => {
	it('shows the title and the description of the migration', () => {
		render(
			<CMSMigration
				groups={[]}
				portletNamespace="_portletNamespace_"
				spaces={[]}
				startCMSMigrationURL="http://localhost/start"
				title="CMS Migration"
			/>
		);

		expect(
			screen.getByRole('heading', {name: 'CMS Migration'})
		).toBeInTheDocument();
		expect(
			screen.getByText('cms-migration-description')
		).toBeInTheDocument();
	});
});
