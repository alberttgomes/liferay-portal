/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.upgrades.lab.agent.remote.rest.client.dto.v1_0;

import com.liferay.upgrades.lab.agent.remote.rest.client.function.UnsafeSupplier;
import com.liferay.upgrades.lab.agent.remote.rest.client.serdes.v1_0.UpgradeRunResumptionSerDes;

import jakarta.annotation.Generated;

import java.io.Serializable;

import java.util.Map;
import java.util.Objects;

/**
 * @author Albert Gomes Cabral
 * @generated
 */
@Generated("")
public class UpgradeRunResumption implements Cloneable, Serializable {

	public static UpgradeRunResumption toDTO(String json) {
		return UpgradeRunResumptionSerDes.toDTO(json);
	}

	public Integer getFirstPhase() {
		return firstPhase;
	}

	public void setFirstPhase(Integer firstPhase) {
		this.firstPhase = firstPhase;
	}

	public void setFirstPhase(
		UnsafeSupplier<Integer, Exception> firstPhaseUnsafeSupplier) {

		try {
			firstPhase = firstPhaseUnsafeSupplier.get();
		}
		catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	protected Integer firstPhase;

	public Map<String, String> getSettings() {
		return settings;
	}

	public void setSettings(Map<String, String> settings) {
		this.settings = settings;
	}

	public void setSettings(
		UnsafeSupplier<Map<String, String>, Exception> settingsUnsafeSupplier) {

		try {
			settings = settingsUnsafeSupplier.get();
		}
		catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	protected Map<String, String> settings;

	@Override
	public UpgradeRunResumption clone() throws CloneNotSupportedException {
		return (UpgradeRunResumption)super.clone();
	}

	@Override
	public boolean equals(Object object) {
		if (this == object) {
			return true;
		}

		if (!(object instanceof UpgradeRunResumption)) {
			return false;
		}

		UpgradeRunResumption upgradeRunResumption =
			(UpgradeRunResumption)object;

		return Objects.equals(toString(), upgradeRunResumption.toString());
	}

	@Override
	public int hashCode() {
		String string = toString();

		return string.hashCode();
	}

	public String toString() {
		return UpgradeRunResumptionSerDes.toJSON(this);
	}

}
// LIFERAY-REST-BUILDER-HASH:-905497994