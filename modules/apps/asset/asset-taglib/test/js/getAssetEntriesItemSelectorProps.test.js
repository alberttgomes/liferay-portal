/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import getAssetEntriesItemSelectorProps from '../../src/main/resources/META-INF/resources/js/getAssetEntriesItemSelectorProps';

const STATUS_FILTER = "(status eq 'approved' or status eq 'scheduled')";

function getAPIURL(assetEntryTypes) {
	const {apiURL} = getAssetEntriesItemSelectorProps({
		assetEntryTypes,
		groupIds: '8,9',
		portletNamespace: 'ns',
	});

	return new URL(apiURL);
}

describe('getAssetEntriesItemSelectorProps', () => {
	describe('apiURL', () => {
		it('falls back to the connected groups when there are no types', () => {
			const url = getAPIURL([]);

			expect(url.searchParams.getAll('groupIds')).toEqual(['8', '9']);
			expect(url.searchParams.get('filter')).toBe(STATUS_FILTER);
		});

		it('filters by class name only when every type shares the same groups', () => {
			const url = getAPIURL([
				{classNameId: 10, groupIds: [1, 2], label: 'Blogs Entry'},
				{classNameId: 20, groupIds: [1, 2], label: 'Web Content'},
			]);

			expect(url.searchParams.getAll('groupIds')).toEqual(['1', '2']);
			expect(url.searchParams.get('filter')).toBe(
				`classNameId in (10,20) and ${STATUS_FILTER}`
			);
		});

		it('filters by class name and group per type when the types have different groups', () => {
			const url = getAPIURL([
				{classNameId: 10, groupIds: [1, 2], label: 'Blogs Entry'},
				{classNameId: 20, groupIds: [3, 2], label: 'Web Content'},
				{classNameId: 30, groupIds: [1, 2], label: 'Document'},
			]);

			expect(url.searchParams.getAll('groupIds')).toEqual([
				'1',
				'2',
				'3',
			]);
			expect(url.searchParams.get('filter')).toBe(
				`((classNameId in (10,30) and groupId in (1,2)) or (classNameId eq 20 and groupId in (3,2))) and ${STATUS_FILTER}`
			);
		});
	});
});
