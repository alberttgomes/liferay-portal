/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.upgrades.lab.agent.remote.rest.internal.resource.v1_0;

import com.liferay.upgrades.lab.agent.remote.constants.UpgradeRunConstants;
import com.liferay.upgrades.lab.agent.remote.rest.dto.v1_0.UpgradeRun;
import com.liferay.upgrades.lab.agent.remote.rest.resource.v1_0.UpgradeRunResource;
import com.liferay.upgrades.lab.agent.remote.service.UpgradeRunLocalService;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ServiceScope;

/**
 * @author Albert Gomes Cabral
 */
@Component(
	properties = "OSGI-INF/liferay/rest/v1_0/upgrade-run.properties",
	scope = ServiceScope.PROTOTYPE, service = UpgradeRunResource.class
)
public class UpgradeRunResourceImpl extends BaseUpgradeRunResourceImpl {

	@Override
	public void deleteUpgradeRunByExternalReferenceCode(
			String externalReferenceCode)
		throws Exception {

		_upgradeRunLocalService.cancelUpgradeRun(
			contextCompany.getCompanyId(), externalReferenceCode);
	}

	@Override
	public UpgradeRun getUpgradeRunByExternalReferenceCode(
			String externalReferenceCode)
		throws Exception {

		return _toUpgradeRun(
			_upgradeRunLocalService.refreshUpgradeRun(
				contextCompany.getCompanyId(), externalReferenceCode));
	}

	@Override
	public UpgradeRun postUpgradeRun(UpgradeRun upgradeRun) throws Exception {
		return _toUpgradeRun(
			_upgradeRunLocalService.addUpgradeRun(
				upgradeRun.getBranch(), upgradeRun.getCredentialKeyReference(),
				upgradeRun.getCustomerName(), upgradeRun.getDbTargetType(),
				upgradeRun.getDbTargetVersion(), upgradeRun.getNodeVersion(),
				upgradeRun.getRepositoryURL(), upgradeRun.getSearchVersion(),
				upgradeRun.getTargetRelease(),
				upgradeRun.getUpgradeSourceVersion(),
				upgradeRun.getUpgradeTargetJavaVersion(),
				contextUser.getUserId()));
	}

	private UpgradeRun _toUpgradeRun(
		com.liferay.upgrades.lab.agent.remote.model.UpgradeRun upgradeRun) {

		return new UpgradeRun() {
			{
				setBranch(upgradeRun::getBranch);
				setCredentialKeyReference(
					upgradeRun::getCredentialKeyReference);
				setCustomerName(upgradeRun::getCustomerName);
				setDbTargetType(upgradeRun::getDbTargetType);
				setDbTargetVersion(upgradeRun::getDbTargetVersion);
				setExternalReferenceCode(upgradeRun::getExternalReferenceCode);
				setNodeVersion(upgradeRun::getNodeVersion);
				setPullRequestURL(upgradeRun::getPullRequestURL);
				setRepositoryURL(upgradeRun::getRepositoryURL);
				setResultBranch(upgradeRun::getResultBranch);
				setSearchVersion(upgradeRun::getSearchVersion);
				setStatus(
					() -> Status.create(
						UpgradeRunConstants.getStatusLabel(
							upgradeRun.getStatus())));
				setStatusMessage(upgradeRun::getStatusMessage);
				setTargetRelease(upgradeRun::getTargetRelease);
				setUpgradeRunId(upgradeRun::getUpgradeRunId);
				setUpgradeSourceVersion(upgradeRun::getUpgradeSourceVersion);
				setUpgradeTargetJavaVersion(
					upgradeRun::getUpgradeTargetJavaVersion);
				setWorkspacePath(upgradeRun::getWorkspacePath);
			}
		};
	}

	@Reference
	private UpgradeRunLocalService _upgradeRunLocalService;

}