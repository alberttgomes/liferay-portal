/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import ClayButton from '@clayui/button';
import {ClayInput, ClayToggle} from '@clayui/form';
import ClayLabel from '@clayui/label';
import {fetch} from 'frontend-js-web';
import React, {useEffect, useState} from 'react';

import Pager from './Pager';
import {SchemaValidation, TableValidation} from './types';

interface SchemaValidationTabProps {
	phase: number;
	schemaValidationURL: string;
}

function getColumnStatus(status: string) {
	if (status === 'NOT_MIGRATED') {
		return {
			displayType: 'danger' as const,
			label: Liferay.Language.get('not-migrated'),
		};
	}

	if (status === 'CUSTOM') {
		return {
			displayType: 'warning' as const,
			label: Liferay.Language.get('custom'),
		};
	}

	if (status === 'ADDED') {
		return {
			displayType: 'info' as const,
			label: Liferay.Language.get('added'),
		};
	}

	return {
		displayType: 'success' as const,
		label: Liferay.Language.get('valid'),
	};
}

function getTableStatus(status: string) {
	if (status === 'NOT_MIGRATED') {
		return {
			displayType: 'danger' as const,
			label: Liferay.Language.get('not-migrated'),
		};
	}

	if (status === 'INCOMPLETE') {
		return {
			displayType: 'danger' as const,
			label: Liferay.Language.get('incomplete'),
		};
	}

	if (status === 'ROW_COUNT_MISMATCH') {
		return {
			displayType: 'warning' as const,
			label: Liferay.Language.get('row-count-mismatch'),
		};
	}

	if (status === 'HAS_CUSTOM_COLUMNS') {
		return {
			displayType: 'warning' as const,
			label: Liferay.Language.get('has-custom-columns'),
		};
	}

	if (status === 'OBJECT') {
		return {
			displayType: 'info' as const,
			label: Liferay.Language.get('liferay-object'),
		};
	}

	if (status === 'CUSTOM_TABLE') {
		return {
			displayType: 'info' as const,
			label: Liferay.Language.get('custom-table'),
		};
	}

	if (status === 'TARGET_ONLY') {
		return {
			displayType: 'info' as const,
			label: Liferay.Language.get('target-only'),
		};
	}

	return {
		displayType: 'success' as const,
		label: Liferay.Language.get('valid'),
	};
}

function needsReview(tableValidation: TableValidation) {
	const {displayType} = getTableStatus(tableValidation.status);

	return displayType === 'danger' || displayType === 'warning';
}

const SchemaValidationTab: React.FC<SchemaValidationTabProps> = ({
	phase,
	schemaValidationURL,
}) => {
	const [schemaValidation, setSchemaValidation] =
		useState<SchemaValidation | null>(null);
	const [expanded, setExpanded] = useState<string[]>([]);
	const [page, setPage] = useState(1);
	const [delta, setDelta] = useState(20);
	const [searchTerm, setSearchTerm] = useState('');
	const [onlyIssues, setOnlyIssues] = useState(false);

	useEffect(() => {
		fetch(schemaValidationURL)
			.then((response) => response.json())
			.then((json) => setSchemaValidation(json))
			.catch(() => {});
	}, [schemaValidationURL, phase]);

	if (!schemaValidation || !schemaValidation.available) {
		return (
			<p className="pt-3 text-secondary">
				{Liferay.Language.get(
					'the-schema-validation-is-available-after-a-migration-completes'
				)}
			</p>
		);
	}

	const tableValidations = schemaValidation.tableValidations;

	const reviewCount = tableValidations.filter(needsReview).length;
	const validCount = tableValidations.length - reviewCount;

	const filteredTableValidations = tableValidations.filter(
		(tableValidation) => {
			if (onlyIssues && !needsReview(tableValidation)) {
				return false;
			}

			if (
				searchTerm &&
				!tableValidation.tableName
					.toLowerCase()
					.includes(searchTerm.toLowerCase())
			) {
				return false;
			}

			return true;
		}
	);

	const visibleTableValidations = filteredTableValidations.slice(
		(page - 1) * delta,
		(page - 1) * delta + delta
	);

	const toggle = (tableName: string) =>
		setExpanded((previous) =>
			previous.includes(tableName)
				? previous.filter((name) => name !== tableName)
				: [...previous, tableName]
		);

	return (
		<div className="pt-3">
			<p className="text-secondary">
				{Liferay.Language.get(
					'each-column-is-validated-against-liferays-schema-custom-columns-are-flagged-for-review'
				)}
			</p>

			<div className="align-items-center d-flex flex-wrap mb-3">
				<ClayLabel displayType="info">
					{`${tableValidations.length} ${Liferay.Language.get(
						'tables'
					)}`}
				</ClayLabel>

				<ClayLabel displayType="success">
					{`${validCount} ${Liferay.Language.get('valid')}`}
				</ClayLabel>

				<ClayLabel displayType={reviewCount ? 'warning' : 'secondary'}>
					{`${reviewCount} ${Liferay.Language.get('needs-review')}`}
				</ClayLabel>
			</div>

			<div className="align-items-center d-flex mb-3">
				<div className="flex-grow-1 mr-3">
					<ClayInput
						onChange={(event) => {
							setSearchTerm(event.target.value);
							setPage(1);
						}}
						placeholder={Liferay.Language.get('search-tables')}
						type="text"
						value={searchTerm}
					/>
				</div>

				<ClayToggle
					label={Liferay.Language.get('show-only-issues')}
					onToggle={(toggled) => {
						setOnlyIssues(toggled);
						setPage(1);
					}}
					toggled={onlyIssues}
				/>
			</div>

			<table className="table table-autofit table-list">
				<thead>
					<tr>
						<th />

						<th>{Liferay.Language.get('table')}</th>

						<th className="text-right">
							{Liferay.Language.get('source-rows')}
						</th>

						<th className="text-right">
							{Liferay.Language.get('target-rows')}
						</th>

						<th>{Liferay.Language.get('status')}</th>
					</tr>
				</thead>

				<tbody>
					{visibleTableValidations.map((tableValidation) => {
						const status = getTableStatus(tableValidation.status);

						const isExpanded = expanded.includes(
							tableValidation.tableName
						);

						return (
							<React.Fragment key={tableValidation.tableName}>
								<tr>
									<td>
										<ClayButton
											aria-label={Liferay.Language.get(
												'columns'
											)}
											borderless
											displayType="secondary"
											monospaced
											onClick={() =>
												toggle(
													tableValidation.tableName
												)
											}
											size="sm"
										>
											{isExpanded ? '-' : '+'}
										</ClayButton>
									</td>

									<td>{tableValidation.tableName}</td>

									<td className="text-right">
										{tableValidation.status ===
										'TARGET_ONLY'
											? '-'
											: tableValidation.sourceRowCount}
									</td>

									<td className="text-right">
										{tableValidation.status ===
										'NOT_MIGRATED'
											? '-'
											: tableValidation.targetRowCount}
									</td>

									<td>
										<ClayLabel
											displayType={status.displayType}
										>
											{status.label}
										</ClayLabel>
									</td>
								</tr>

								{isExpanded && (
									<tr>
										<td colSpan={5}>
											<table className="table table-autofit table-list">
												<thead>
													<tr>
														<th>
															{Liferay.Language.get(
																'column'
															)}
														</th>

														<th>
															{Liferay.Language.get(
																'source-type'
															)}
														</th>

														<th>
															{Liferay.Language.get(
																'target-type'
															)}
														</th>

														<th>
															{Liferay.Language.get(
																'status'
															)}
														</th>
													</tr>
												</thead>

												<tbody>
													{tableValidation.columnValidations.map(
														(columnValidation) => {
															const columnStatus =
																getColumnStatus(
																	columnValidation.status
																);

															return (
																<tr
																	key={
																		columnValidation.columnName
																	}
																>
																	<td>
																		{
																			columnValidation.columnName
																		}
																	</td>

																	<td>
																		{columnValidation.sourceType ||
																			'-'}
																	</td>

																	<td>
																		{columnValidation.targetType ||
																			'-'}
																	</td>

																	<td>
																		<ClayLabel
																			displayType={
																				columnStatus.displayType
																			}
																		>
																			{
																				columnStatus.label
																			}
																		</ClayLabel>
																	</td>
																</tr>
															);
														}
													)}
												</tbody>
											</table>
										</td>
									</tr>
								)}
							</React.Fragment>
						);
					})}
				</tbody>
			</table>

			<Pager
				activePage={page}
				delta={delta}
				onActiveChange={setPage}
				onDeltaChange={(newDelta) => {
					setDelta(newDelta);
					setPage(1);
				}}
				totalItems={filteredTableValidations.length}
			/>
		</div>
	);
};

export default SchemaValidationTab;
