/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.upgrades.lab.agent.remote.model;

import com.liferay.portal.kernel.annotation.ImplementationClassName;
import com.liferay.portal.kernel.model.PersistedModel;
import com.liferay.portal.kernel.util.Accessor;

import org.osgi.annotation.versioning.ProviderType;

/**
 * The extended model interface for the UpgradeRun service. Represents a row in the &quot;Upgrades_UpgradeRun&quot; database table, with each column mapped to a property of this class.
 *
 * @author Albert Gomes Cabral
 * @see UpgradeRunModel
 * @generated
 */
@ImplementationClassName(
	"com.liferay.upgrades.lab.agent.remote.model.impl.UpgradeRunImpl"
)
@ProviderType
public interface UpgradeRun extends PersistedModel, UpgradeRunModel {

	/*
	 * NOTE FOR DEVELOPERS:
	 *
	 * Never modify this interface directly. Add methods to <code>com.liferay.upgrades.lab.agent.remote.model.impl.UpgradeRunImpl</code> and rerun ServiceBuilder to automatically copy the method declarations to this interface.
	 */
	public static final Accessor<UpgradeRun, Long> UPGRADE_RUN_ID_ACCESSOR =
		new Accessor<UpgradeRun, Long>() {

			@Override
			public Long get(UpgradeRun upgradeRun) {
				return upgradeRun.getUpgradeRunId();
			}

			@Override
			public Class<Long> getAttributeClass() {
				return Long.class;
			}

			@Override
			public Class<UpgradeRun> getTypeClass() {
				return UpgradeRun.class;
			}

		};

}
// LIFERAY-SERVICE-BUILDER-HASH:1770042352