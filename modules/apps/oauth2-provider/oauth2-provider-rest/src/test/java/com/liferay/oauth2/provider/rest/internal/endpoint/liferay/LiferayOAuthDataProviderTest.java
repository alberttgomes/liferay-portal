/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.oauth2.provider.rest.internal.endpoint.liferay;

import com.liferay.oauth2.provider.model.OAuth2Application;
import com.liferay.oauth2.provider.rest.internal.endpoint.constants.OAuth2ProviderRESTEndpointConstants;
import com.liferay.oauth2.provider.service.OAuth2ApplicationLocalService;
import com.liferay.portal.kernel.test.ReflectionTestUtil;
import com.liferay.portal.kernel.test.util.RandomTestUtil;
import com.liferay.portal.kernel.util.HashMapBuilder;
import com.liferay.portal.test.rule.LiferayUnitTestRule;

import org.apache.cxf.rs.security.jose.jwt.JwtClaims;
import org.apache.cxf.rs.security.oauth2.common.Client;
import org.apache.cxf.rs.security.oauth2.tokens.bearer.BearerAccessToken;

import org.junit.Assert;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;

import org.mockito.Mockito;

/**
 * @author Álvaro Saugar
 */
public class LiferayOAuthDataProviderTest {

	@ClassRule
	@Rule
	public static final LiferayUnitTestRule liferayUnitTestRule =
		LiferayUnitTestRule.INSTANCE;

	@Test
	public void testCreateJwtAccessToken() {
		LiferayOAuthDataProvider liferayOAuthDataProvider =
			new LiferayOAuthDataProvider();

		OAuth2ApplicationLocalService oAuth2ApplicationLocalService =
			Mockito.mock(OAuth2ApplicationLocalService.class);

		OAuth2Application oAuth2Application = Mockito.mock(
			OAuth2Application.class);

		String externalReferenceCode = RandomTestUtil.randomString();

		Mockito.when(
			oAuth2Application.getExternalReferenceCode()
		).thenReturn(
			externalReferenceCode
		);

		String clientId = RandomTestUtil.randomString();
		long companyId = RandomTestUtil.randomLong();

		Mockito.when(
			oAuth2ApplicationLocalService.fetchOAuth2Application(
				companyId, clientId)
		).thenReturn(
			oAuth2Application
		);

		ReflectionTestUtil.setFieldValue(
			liferayOAuthDataProvider, "_oAuth2ApplicationLocalService",
			oAuth2ApplicationLocalService);

		Client client = new Client(clientId, null, true);

		client.setProperties(
			HashMapBuilder.put(
				OAuth2ProviderRESTEndpointConstants.PROPERTY_KEY_COMPANY_ID,
				String.valueOf(companyId)
			).build());

		JwtClaims jwtClaims = liferayOAuthDataProvider.createJwtAccessToken(
			new BearerAccessToken(client, RandomTestUtil.randomInt()));

		Assert.assertEquals(
			externalReferenceCode,
			jwtClaims.getClaim("external_reference_code"));
	}

}