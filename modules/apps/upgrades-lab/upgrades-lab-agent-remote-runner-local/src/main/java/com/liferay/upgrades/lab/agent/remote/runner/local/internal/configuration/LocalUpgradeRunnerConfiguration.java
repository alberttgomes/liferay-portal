/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.upgrades.lab.agent.remote.runner.local.internal.configuration;

import aQute.bnd.annotation.metatype.Meta;

import com.liferay.portal.configuration.metatype.annotations.ExtendedObjectClassDefinition;

/**
 * @author Albert Gomes Cabral
 */
@ExtendedObjectClassDefinition(
	category = "upgrades", scope = ExtendedObjectClassDefinition.Scope.COMPANY
)
@Meta.OCD(
	id = "com.liferay.upgrades.lab.agent.remote.runner.local.internal.configuration.LocalUpgradeRunnerConfiguration",
	localization = "content/Language",
	name = "local-upgrade-runner-configuration-name"
)
public interface LocalUpgradeRunnerConfiguration {

	@Meta.AD(
		description = "upgrade-runner-agent-command-help",
		name = "upgrade-runner-agent-command", required = false
	)
	public String[] agentCommand();

	@Meta.AD(
		deflt = "claude", description = "upgrade-runner-claude-command-help",
		name = "upgrade-runner-claude-command", required = false
	)
	public String[] claudeCommand();

	@Meta.AD(
		description = "upgrade-runner-claude-config-directory-help",
		name = "upgrade-runner-claude-config-directory", required = false
	)
	public String claudeConfigDirectory();

	@Meta.AD(
		description = "upgrade-runner-claude-credential-help",
		name = "upgrade-runner-claude-credential", required = false,
		type = Meta.Type.Password
	)
	public String claudeCredential();

	@Meta.AD(
		deflt = "gh", description = "upgrade-runner-gh-command-help",
		name = "upgrade-runner-gh-command", required = false
	)
	public String[] ghCommand();

	@Meta.AD(
		deflt = "false",
		description = "upgrade-runner-host-credentials-enabled-help",
		name = "upgrade-runner-host-credentials-enabled", required = false
	)
	public boolean hostCredentialsEnabled();

	@Meta.AD(
		deflt = "/opt/liferay/upgrades/license",
		description = "upgrade-runner-license-path-help",
		name = "upgrade-runner-license-path", required = false
	)
	public String licensePath();

}