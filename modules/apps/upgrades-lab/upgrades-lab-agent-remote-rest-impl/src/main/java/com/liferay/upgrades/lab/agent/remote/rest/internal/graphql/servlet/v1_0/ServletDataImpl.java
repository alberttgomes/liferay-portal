/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.upgrades.lab.agent.remote.rest.internal.graphql.servlet.v1_0;

import com.liferay.portal.kernel.util.ObjectValuePair;
import com.liferay.portal.vulcan.graphql.servlet.ServletData;
import com.liferay.upgrades.lab.agent.remote.rest.internal.graphql.mutation.v1_0.Mutation;
import com.liferay.upgrades.lab.agent.remote.rest.internal.graphql.query.v1_0.Query;
import com.liferay.upgrades.lab.agent.remote.rest.internal.resource.v1_0.UpgradeRunResourceImpl;
import com.liferay.upgrades.lab.agent.remote.rest.resource.v1_0.UpgradeRunResource;

import jakarta.annotation.Generated;

import java.util.HashMap;
import java.util.Map;

import org.osgi.framework.BundleContext;
import org.osgi.service.component.ComponentServiceObjects;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceScope;

/**
 * @author Albert Gomes Cabral
 * @generated
 */
@Component(service = ServletData.class)
@Generated("")
public class ServletDataImpl implements ServletData {

	@Activate
	public void activate(BundleContext bundleContext) {
		Mutation.setUpgradeRunResourceComponentServiceObjects(
			_upgradeRunResourceComponentServiceObjects);

		Query.setUpgradeRunResourceComponentServiceObjects(
			_upgradeRunResourceComponentServiceObjects);
	}

	public String getApplicationName() {
		return "Liferay.Upgrades.Lab.Agent.Remote.REST";
	}

	@Override
	public Mutation getMutation() {
		return new Mutation();
	}

	@Override
	public String getPath() {
		return "/upgrades-lab-agent-remote-graphql/v1_0";
	}

	@Override
	public Query getQuery() {
		return new Query();
	}

	public ObjectValuePair<Class<?>, String> getResourceMethodObjectValuePair(
		String methodName, boolean mutation) {

		if (mutation) {
			return _resourceMethodObjectValuePairs.get(
				"mutation#" + methodName);
		}

		return _resourceMethodObjectValuePairs.get("query#" + methodName);
	}

	private static final Map<String, ObjectValuePair<Class<?>, String>>
		_resourceMethodObjectValuePairs =
			new HashMap<String, ObjectValuePair<Class<?>, String>>() {
				{
					put(
						"mutation#deleteUpgradeRunByExternalReferenceCode",
						new ObjectValuePair<>(
							UpgradeRunResourceImpl.class,
							"deleteUpgradeRunByExternalReferenceCode"));
					put(
						"mutation#createUpgradeRun",
						new ObjectValuePair<>(
							UpgradeRunResourceImpl.class, "postUpgradeRun"));

					put(
						"query#upgradeRunByExternalReferenceCode",
						new ObjectValuePair<>(
							UpgradeRunResourceImpl.class,
							"getUpgradeRunByExternalReferenceCode"));
				}
			};

	@Reference(scope = ReferenceScope.PROTOTYPE_REQUIRED)
	private ComponentServiceObjects<UpgradeRunResource>
		_upgradeRunResourceComponentServiceObjects;

}
// LIFERAY-REST-BUILDER-HASH:-640328982