/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.upgrades.lab.agent.remote.rest.internal.jaxrs.exception.mapper;

import com.liferay.portal.vulcan.jaxrs.exception.mapper.BaseExceptionMapper;
import com.liferay.portal.vulcan.jaxrs.exception.mapper.Problem;
import com.liferay.upgrades.lab.agent.remote.exception.UpgradeRunBranchException;

import jakarta.ws.rs.ext.ExceptionMapper;

import org.osgi.service.component.annotations.Component;

/**
 * @author Albert Gomes Cabral
 */
@Component(
	property = {
		"osgi.jaxrs.application.select=(osgi.jaxrs.name=Liferay.Upgrades.Lab.Agent.Remote.REST)",
		"osgi.jaxrs.extension=true",
		"osgi.jaxrs.name=Liferay.Upgrades.Lab.Agent.Remote.REST.UpgradeRunBranchExceptionMapper"
	},
	service = ExceptionMapper.class
)
public class UpgradeRunBranchExceptionMapper
	extends BaseExceptionMapper<UpgradeRunBranchException> {

	@Override
	protected Problem getProblem(
		UpgradeRunBranchException upgradeRunBranchException) {

		return new Problem(upgradeRunBranchException);
	}

}