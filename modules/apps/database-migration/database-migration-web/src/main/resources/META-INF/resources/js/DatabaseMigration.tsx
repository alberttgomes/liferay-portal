/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import ClayAlert from '@clayui/alert';
import ClayButton from '@clayui/button';
import ClayTabs from '@clayui/tabs';
import {fetch} from 'frontend-js-web';
import React, {useCallback, useEffect, useState} from 'react';

import ErrorsTab from './ErrorsTab';
import MigrationForm from './MigrationForm';
import SchemaValidationTab from './SchemaValidationTab';
import StatusTab from './StatusTab';
import {
	PHASE_COMPLETED,
	PHASE_DISCOVERY,
	PHASE_IDLE,
	Props,
	Status,
} from './types';

const POLL_INTERVAL = 2000;

const DatabaseMigration: React.FC<Props> = ({
	namespace,
	schemaValidationURL,
	startMigrationURL,
	statusURL,
	testConnectionURL,
}) => {
	const [status, setStatus] = useState<Status | null>(null);
	const [tracking, setTracking] = useState(false);
	const [activeIndex, setActiveIndex] = useState(0);
	const [showForm, setShowForm] = useState(false);

	const fetchStatus = useCallback(
		() =>
			fetch(statusURL)
				.then((response) => response.json())
				.then((json: Status) => {
					setStatus(json);

					return json;
				})
				.catch(() => null),
		[statusURL]
	);

	useEffect(() => {
		fetchStatus().then((json) => {
			if (json && json.running) {
				setTracking(true);
			}
		});
	}, [fetchStatus]);

	useEffect(() => {
		if (!tracking) {
			return undefined;
		}

		const intervalId = setInterval(() => {
			fetchStatus().then((json) => {
				if (json && !json.running) {
					setTracking(false);
				}
			});
		}, POLL_INTERVAL);

		return () => clearInterval(intervalId);
	}, [tracking, fetchStatus]);

	const handleStarted = () => {
		setStatus({
			elapsedTime: 0,
			errors: [],
			message: Liferay.Language.get('connecting-to-databases'),
			phase: PHASE_DISCOVERY,
			phaseLabel: Liferay.Language.get('discovery'),
			progress: 0,
			running: true,
			tableRowCounts: [],
		});

		setShowForm(false);
		setTracking(true);
	};

	const running = Boolean(status && status.running);

	const started = Boolean(status && status.phase !== PHASE_IDLE);

	const errorCount = status && status.errors ? status.errors.length : 0;

	const formVisible = !started || (showForm && !running);

	const dashboardVisible = Boolean(status) && started && !formVisible;

	return (
		<div className="container-fluid container-fluid-max-xl">
			<div className="mb-4">
				<p className="text-secondary">
					{Liferay.Language.get('database-migration-discovery-help')}
				</p>
			</div>

			{formVisible && (
				<>
					{started && (
						<div className="mb-3">
							<ClayButton
								borderless
								displayType="secondary"
								onClick={() => setShowForm(false)}
								small
							>
								{Liferay.Language.get('back-to-results')}
							</ClayButton>
						</div>
					)}

					<MigrationForm
						namespace={namespace}
						onStarted={handleStarted}
						startMigrationURL={startMigrationURL}
						testConnectionURL={testConnectionURL}
					/>
				</>
			)}

			{dashboardVisible && status && (
				<div className="mt-4">
					{status.phase === PHASE_COMPLETED && (
						<ClayAlert
							displayType="success"
							title={Liferay.Language.get('migration-complete')}
						>
							{Liferay.Language.get(
								'stop-the-portal-and-restart-it-configured-against-the-target-postgresql-database-to-use-the-migrated-data'
							)}
						</ClayAlert>
					)}

					{!running && (
						<div className="d-flex justify-content-end mb-3">
							<ClayButton
								displayType="primary"
								onClick={() => setShowForm(true)}
							>
								{Liferay.Language.get('new-migration')}
							</ClayButton>
						</div>
					)}

					<ClayTabs modern>
						<ClayTabs.Item
							active={activeIndex === 0}
							innerProps={{'aria-controls': 'tab-status'}}
							onClick={() => setActiveIndex(0)}
						>
							{Liferay.Language.get('status')}
						</ClayTabs.Item>

						<ClayTabs.Item
							active={activeIndex === 1}
							innerProps={{'aria-controls': 'tab-errors'}}
							onClick={() => setActiveIndex(1)}
						>
							{`${Liferay.Language.get('errors')} (${errorCount})`}
						</ClayTabs.Item>

						<ClayTabs.Item
							active={activeIndex === 2}
							innerProps={{'aria-controls': 'tab-schema'}}
							onClick={() => setActiveIndex(2)}
						>
							{Liferay.Language.get('schema-validation')}
						</ClayTabs.Item>
					</ClayTabs>

					<ClayTabs.Content activeIndex={activeIndex} fade>
						<ClayTabs.TabPane aria-labelledby="tab-status">
							<StatusTab status={status} />
						</ClayTabs.TabPane>

						<ClayTabs.TabPane aria-labelledby="tab-errors">
							<ErrorsTab errors={status.errors} />
						</ClayTabs.TabPane>

						<ClayTabs.TabPane aria-labelledby="tab-schema">
							<SchemaValidationTab
								phase={status.phase}
								schemaValidationURL={schemaValidationURL}
							/>
						</ClayTabs.TabPane>
					</ClayTabs.Content>
				</div>
			)}
		</div>
	);
};

export default DatabaseMigration;
