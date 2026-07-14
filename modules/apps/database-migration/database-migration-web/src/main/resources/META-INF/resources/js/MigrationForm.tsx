/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import ClayAlert from '@clayui/alert';
import ClayButton from '@clayui/button';
import ClayForm, {ClayCheckbox, ClayInput} from '@clayui/form';
import {openToast} from 'frontend-js-components-web';
import {fetch} from 'frontend-js-web';
import React, {useState} from 'react';

interface MigrationFormProps {
	currentConnectionAvailable: boolean;
	currentJDBCURL: string;
	currentUserName: string;
	namespace: string;
	onStarted: () => void;
	startMigrationURL: string;
	testConnectionURL: string;
}

type Side = 'source' | 'target';

function getErrorMessage(errorCode: string): string {
	if (errorCode === 'connectionInformationRequired') {
		return Liferay.Language.get(
			'both-source-and-target-connection-details-are-required'
		);
	}

	if (errorCode === 'migrationAlreadyRunning') {
		return Liferay.Language.get('a-database-migration-is-already-running');
	}

	if (errorCode === 'targetDatabaseMustBePostgreSQL') {
		return Liferay.Language.get(
			'the-target-database-url-must-be-postgresql'
		);
	}

	return Liferay.Language.get('an-unexpected-error-occurred');
}

const MigrationForm: React.FC<MigrationFormProps> = ({
	currentConnectionAvailable,
	currentJDBCURL,
	currentUserName,
	namespace,
	onStarted,
	startMigrationURL,
	testConnectionURL,
}) => {
	const [values, setValues] = useState({
		migrationName: '',
		sourceJDBCURL: '',
		sourcePassword: '',
		sourceUserName: '',
		targetJDBCURL: '',
		targetPassword: '',
		targetUserName: '',
	});
	const [errorCode, setErrorCode] = useState<string | null>(null);
	const [submitting, setSubmitting] = useState(false);
	const [testingSide, setTestingSide] = useState<Side | null>(null);
	const [useCurrentSourceConnection, setUseCurrentSourceConnection] =
		useState(false);

	const usingCurrentSource =
		currentConnectionAvailable && useCurrentSourceConnection;

	const setValue = (name: string, value: string) =>
		setValues((previous) => ({...previous, [name]: value}));

	const handleSubmit = (event: React.FormEvent) => {
		event.preventDefault();

		setErrorCode(null);
		setSubmitting(true);

		const body = new URLSearchParams();

		Object.entries(values).forEach(([name, value]) =>
			body.append(namespace + name, value)
		);

		if (usingCurrentSource) {
			body.append(namespace + 'useCurrentSourceConnection', 'true');
		}

		fetch(startMigrationURL, {body, method: 'POST'})
			.then((response) => response.json())
			.then((json) => {
				setSubmitting(false);

				if (json.started) {
					onStarted();
				}
				else {
					setErrorCode(json.error || 'unexpected');
				}
			})
			.catch(() => {
				setSubmitting(false);
				setErrorCode('unexpected');
			});
	};

	const notifyTestResult = (valid: boolean, message?: string) => {
		setTestingSide(null);

		if (valid) {
			openToast({
				message: Liferay.Language.get('the-connection-was-successful'),
				type: 'success',
			});

			return;
		}

		let toastMessage = Liferay.Language.get(
			'the-connection-could-not-be-established'
		);

		if (message) {
			toastMessage += ' ' + message;
		}

		openToast({message: toastMessage, type: 'danger'});
	};

	const handleTest = (side: Side) => {
		setTestingSide(side);

		const body = new URLSearchParams();

		if (side === 'source' && usingCurrentSource) {
			body.append(namespace + 'useCurrentConnection', 'true');
		}
		else {
			body.append(namespace + 'jdbcURL', values[`${side}JDBCURL`]);
			body.append(namespace + 'userName', values[`${side}UserName`]);
			body.append(namespace + 'password', values[`${side}Password`]);
		}

		fetch(testConnectionURL, {body, method: 'POST'})
			.then((response) => response.json())
			.then((json) => notifyTestResult(json.valid, json.message))
			.catch(() => notifyTestResult(false));
	};

	const renderField = (
		name: keyof typeof values,
		label: string,
		type: string,
		{
			disabled = false,
			placeholder,
			value,
		}: {disabled?: boolean; placeholder?: string; value?: string} = {}
	) => (
		<ClayForm.Group>
			<label htmlFor={name}>{label}</label>

			<ClayInput
				disabled={disabled}
				id={name}
				onChange={(event) => setValue(name, event.target.value)}
				placeholder={placeholder}
				type={type}
				value={value ?? values[name]}
			/>
		</ClayForm.Group>
	);

	const renderTest = (side: Side) => (
		<ClayButton
			className="mt-3"
			disabled={
				testingSide === side ||
				(!(side === 'source' && usingCurrentSource) &&
					(!values[`${side}JDBCURL`] || !values[`${side}UserName`]))
			}
			displayType="secondary"
			onClick={() => handleTest(side)}
			type="button"
		>
			{Liferay.Language.get('test-database-connection')}
		</ClayButton>
	);

	return (
		<ClayForm className="sheet" onSubmit={handleSubmit}>
			{errorCode && (
				<ClayAlert
					displayType="danger"
					title={Liferay.Language.get('error')}
				>
					{getErrorMessage(errorCode)}
				</ClayAlert>
			)}

			{renderField(
				'migrationName',
				Liferay.Language.get('migration-name'),
				'text',
				{
					placeholder: Liferay.Language.get(
						'optional-a-name-is-generated-if-left-blank'
					),
				}
			)}

			{currentConnectionAvailable && (
				<ClayCheckbox
					checked={useCurrentSourceConnection}
					className="mb-3"
					label={Liferay.Language.get(
						'migrate-from-this-liferays-database'
					)}
					onChange={() =>
						setUseCurrentSourceConnection((previous) => !previous)
					}
				/>
			)}

			<div className="row">
				<div className="col-md-6">
					<h4 className="sheet-subtitle">
						{Liferay.Language.get('source-database')}
					</h4>

					{renderField(
						'sourceJDBCURL',
						Liferay.Language.get('jdbc-url'),
						'text',
						{
							disabled: usingCurrentSource,
							placeholder: 'jdbc:mysql://host:3306/lportal',
							value: usingCurrentSource
								? currentJDBCURL
								: undefined,
						}
					)}

					{renderField(
						'sourceUserName',
						Liferay.Language.get('user-name'),
						'text',
						{
							disabled: usingCurrentSource,
							value: usingCurrentSource
								? currentUserName
								: undefined,
						}
					)}

					{renderField(
						'sourcePassword',
						Liferay.Language.get('password'),
						'password',
						{
							disabled: usingCurrentSource,
							placeholder: usingCurrentSource
								? Liferay.Language.get(
										'this-liferays-stored-password-is-used'
									)
								: undefined,
							value: usingCurrentSource ? '' : undefined,
						}
					)}

					{renderTest('source')}
				</div>

				<div className="col-md-6">
					<h4 className="sheet-subtitle">
						{Liferay.Language.get('target-database-postgresql')}
					</h4>

					{renderField(
						'targetJDBCURL',
						Liferay.Language.get('jdbc-url'),
						'text',
						{placeholder: 'jdbc:postgresql://host:5432/lportal'}
					)}

					{renderField(
						'targetUserName',
						Liferay.Language.get('user-name'),
						'text'
					)}

					{renderField(
						'targetPassword',
						Liferay.Language.get('password'),
						'password'
					)}

					{renderTest('target')}
				</div>
			</div>

			<ClayButton className="mt-4" disabled={submitting} type="submit">
				{Liferay.Language.get('start-migration')}
			</ClayButton>
		</ClayForm>
	);
};

export default MigrationForm;
