/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.upgrades.lab.agent.remote.rest.resource.v1_0.test;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.util.ISO8601DateFormat;

import com.liferay.petra.function.transform.TransformUtil;
import com.liferay.petra.reflect.ReflectionUtil;
import com.liferay.petra.string.StringBundler;
import com.liferay.portal.kernel.json.JSONArray;
import com.liferay.portal.kernel.json.JSONDeserializer;
import com.liferay.portal.kernel.json.JSONFactoryUtil;
import com.liferay.portal.kernel.json.JSONObject;
import com.liferay.portal.kernel.json.JSONUtil;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.service.CompanyLocalServiceUtil;
import com.liferay.portal.kernel.test.util.GroupTestUtil;
import com.liferay.portal.kernel.test.util.RandomTestUtil;
import com.liferay.portal.kernel.test.util.UserTestUtil;
import com.liferay.portal.kernel.util.ArrayUtil;
import com.liferay.portal.kernel.util.DateUtil;
import com.liferay.portal.kernel.util.FastDateFormatFactoryUtil;
import com.liferay.portal.kernel.util.LocaleUtil;
import com.liferay.portal.kernel.util.PortalUtil;
import com.liferay.portal.kernel.util.PropsValues;
import com.liferay.portal.kernel.util.StringUtil;
import com.liferay.portal.odata.entity.EntityField;
import com.liferay.portal.odata.entity.EntityModel;
import com.liferay.portal.test.rule.Inject;
import com.liferay.portal.test.rule.LiferayIntegrationTestRule;
import com.liferay.portal.vulcan.resource.EntityModelResource;
import com.liferay.upgrades.lab.agent.remote.rest.client.dto.v1_0.UpgradeRun;
import com.liferay.upgrades.lab.agent.remote.rest.client.http.HttpInvoker;
import com.liferay.upgrades.lab.agent.remote.rest.client.pagination.Page;
import com.liferay.upgrades.lab.agent.remote.rest.client.resource.v1_0.UpgradeRunResource;
import com.liferay.upgrades.lab.agent.remote.rest.client.serdes.v1_0.UpgradeRunSerDes;

import jakarta.annotation.Generated;

import jakarta.ws.rs.core.MultivaluedHashMap;

import java.lang.reflect.Method;

import java.text.Format;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TimeZone;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;

/**
 * @author Albert Gomes Cabral
 * @generated
 */
@Generated("")
public abstract class BaseUpgradeRunResourceTestCase {

	@ClassRule
	@Rule
	public static final LiferayIntegrationTestRule liferayIntegrationTestRule =
		new LiferayIntegrationTestRule();

	@BeforeClass
	public static void setUpClass() throws Exception {
		_format = FastDateFormatFactoryUtil.getSimpleDateFormat(
			"yyyy-MM-dd'T'HH:mm:ss'Z'");
	}

	@Before
	public void setUp() throws Exception {
		irrelevantGroup = GroupTestUtil.addGroup();
		testGroup = GroupTestUtil.addGroup();

		testCompany = CompanyLocalServiceUtil.getCompany(
			testGroup.getCompanyId());

		_upgradeRunResource.setContextCompany(testCompany);

		_testCompanyAdminUser = UserTestUtil.getAdminUser(
			testCompany.getCompanyId());

		upgradeRunResource = UpgradeRunResource.builder(
		).authentication(
			_testCompanyAdminUser.getEmailAddress(),
			PropsValues.DEFAULT_ADMIN_PASSWORD
		).endpoint(
			testCompany.getVirtualHostname(),
			PortalUtil.getPortalServerPort(false), "http"
		).locale(
			LocaleUtil.getDefault()
		).build();
	}

	@After
	public void tearDown() throws Exception {
		GroupTestUtil.deleteGroup(irrelevantGroup);
		GroupTestUtil.deleteGroup(testGroup);
	}

	@Test
	public void testClientSerDesToDTO() throws Exception {
		ObjectMapper objectMapper = getClientSerDesObjectMapper();

		UpgradeRun upgradeRun1 = randomUpgradeRun();

		String json = objectMapper.writeValueAsString(upgradeRun1);

		UpgradeRun upgradeRun2 = UpgradeRunSerDes.toDTO(json);

		Assert.assertTrue(equals(upgradeRun1, upgradeRun2));
	}

	@Test
	public void testClientSerDesToJSON() throws Exception {
		ObjectMapper objectMapper = getClientSerDesObjectMapper();

		UpgradeRun upgradeRun = randomUpgradeRun();

		String json1 = objectMapper.writeValueAsString(upgradeRun);
		String json2 = UpgradeRunSerDes.toJSON(upgradeRun);

		Assert.assertEquals(
			objectMapper.readTree(json1), objectMapper.readTree(json2));
	}

	protected ObjectMapper getClientSerDesObjectMapper() {
		return new ObjectMapper() {
			{
				configure(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY, true);
				configure(
					SerializationFeature.WRITE_ENUMS_USING_TO_STRING, true);
				enable(SerializationFeature.INDENT_OUTPUT);
				setDateFormat(new ISO8601DateFormat());
				setSerializationInclusion(JsonInclude.Include.NON_EMPTY);
				setSerializationInclusion(JsonInclude.Include.NON_NULL);
				setVisibility(
					PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY);
				setVisibility(
					PropertyAccessor.GETTER, JsonAutoDetect.Visibility.NONE);
			}
		};
	}

	@Test
	public void testEscapeRegexInStringFields() throws Exception {
		String regex = "^[0-9]+(\\.[0-9]{1,2})\"?";

		UpgradeRun upgradeRun = randomUpgradeRun();

		upgradeRun.setBranch(regex);
		upgradeRun.setCredentialKeyReference(regex);
		upgradeRun.setCustomerName(regex);
		upgradeRun.setDbTargetType(regex);
		upgradeRun.setDbTargetVersion(regex);
		upgradeRun.setExternalReferenceCode(regex);
		upgradeRun.setNodeVersion(regex);
		upgradeRun.setPullRequestURL(regex);
		upgradeRun.setRepositoryURL(regex);
		upgradeRun.setResultBranch(regex);
		upgradeRun.setSearchVersion(regex);
		upgradeRun.setStatusMessage(regex);
		upgradeRun.setTargetRelease(regex);
		upgradeRun.setUpgradeSourceVersion(regex);
		upgradeRun.setUpgradeTargetJavaVersion(regex);
		upgradeRun.setWorkspacePath(regex);

		String json = UpgradeRunSerDes.toJSON(upgradeRun);

		Assert.assertFalse(json.contains(regex));

		upgradeRun = UpgradeRunSerDes.toDTO(json);

		Assert.assertEquals(regex, upgradeRun.getBranch());
		Assert.assertEquals(regex, upgradeRun.getCredentialKeyReference());
		Assert.assertEquals(regex, upgradeRun.getCustomerName());
		Assert.assertEquals(regex, upgradeRun.getDbTargetType());
		Assert.assertEquals(regex, upgradeRun.getDbTargetVersion());
		Assert.assertEquals(regex, upgradeRun.getExternalReferenceCode());
		Assert.assertEquals(regex, upgradeRun.getNodeVersion());
		Assert.assertEquals(regex, upgradeRun.getPullRequestURL());
		Assert.assertEquals(regex, upgradeRun.getRepositoryURL());
		Assert.assertEquals(regex, upgradeRun.getResultBranch());
		Assert.assertEquals(regex, upgradeRun.getSearchVersion());
		Assert.assertEquals(regex, upgradeRun.getStatusMessage());
		Assert.assertEquals(regex, upgradeRun.getTargetRelease());
		Assert.assertEquals(regex, upgradeRun.getUpgradeSourceVersion());
		Assert.assertEquals(regex, upgradeRun.getUpgradeTargetJavaVersion());
		Assert.assertEquals(regex, upgradeRun.getWorkspacePath());
	}

	@Test
	public void testDeleteUpgradeRunByExternalReferenceCode() throws Exception {
		@SuppressWarnings("PMD.UnusedLocalVariable")
		UpgradeRun upgradeRun =
			testDeleteUpgradeRunByExternalReferenceCode_addUpgradeRun();

		assertHttpResponseStatusCode(
			204,
			upgradeRunResource.
				deleteUpgradeRunByExternalReferenceCodeHttpResponse(
					upgradeRun.getExternalReferenceCode()));

		assertHttpResponseStatusCode(
			404,
			upgradeRunResource.getUpgradeRunByExternalReferenceCodeHttpResponse(
				upgradeRun.getExternalReferenceCode()));
		assertHttpResponseStatusCode(
			404,
			upgradeRunResource.getUpgradeRunByExternalReferenceCodeHttpResponse(
				"-"));
	}

	protected UpgradeRun
			testDeleteUpgradeRunByExternalReferenceCode_addUpgradeRun()
		throws Exception {

		throw new UnsupportedOperationException(
			"This method needs to be implemented");
	}

	@Test
	public void testGraphQLDeleteUpgradeRunByExternalReferenceCode()
		throws Exception {

		// No namespace

		@SuppressWarnings("PMD.UnusedLocalVariable")
		UpgradeRun upgradeRun1 =
			testGraphQLDeleteUpgradeRunByExternalReferenceCode_addUpgradeRun();

		Assert.assertTrue(
			JSONUtil.getValueAsBoolean(
				invokeGraphQLMutation(
					new GraphQLField(
						"deleteUpgradeRunByExternalReferenceCode",
						new HashMap<String, Object>() {
							{
								put(
									"externalReferenceCode",
									"\"" +
										upgradeRun1.getExternalReferenceCode() +
											"\"");
							}
						})),
				"JSONObject/data",
				"Object/deleteUpgradeRunByExternalReferenceCode"));

		JSONArray errorsJSONArray1 = JSONUtil.getValueAsJSONArray(
			invokeGraphQLQuery(
				new GraphQLField(
					"upgradeRunByExternalReferenceCode",
					new HashMap<String, Object>() {
						{
							put(
								"externalReferenceCode",
								"\"" + upgradeRun1.getExternalReferenceCode() +
									"\"");
						}
					},
					getGraphQLFields())),
			"JSONArray/errors");

		Assert.assertTrue(errorsJSONArray1.length() > 0);

		// Using the namespace upgradesLabAgentRemote_v1_0

		@SuppressWarnings("PMD.UnusedLocalVariable")
		UpgradeRun upgradeRun2 =
			testGraphQLDeleteUpgradeRunByExternalReferenceCode_addUpgradeRun();

		Assert.assertTrue(
			JSONUtil.getValueAsBoolean(
				invokeGraphQLMutation(
					new GraphQLField(
						"upgradesLabAgentRemote_v1_0",
						new GraphQLField(
							"deleteUpgradeRunByExternalReferenceCode",
							new HashMap<String, Object>() {
								{
									put(
										"externalReferenceCode",
										"\"" +
											upgradeRun2.
												getExternalReferenceCode() +
													"\"");
								}
							}))),
				"JSONObject/data", "JSONObject/upgradesLabAgentRemote_v1_0",
				"Object/deleteUpgradeRunByExternalReferenceCode"));

		JSONArray errorsJSONArray2 = JSONUtil.getValueAsJSONArray(
			invokeGraphQLQuery(
				new GraphQLField(
					"upgradesLabAgentRemote_v1_0",
					new GraphQLField(
						"upgradeRunByExternalReferenceCode",
						new HashMap<String, Object>() {
							{
								put(
									"externalReferenceCode",
									"\"" +
										upgradeRun2.getExternalReferenceCode() +
											"\"");
							}
						},
						getGraphQLFields()))),
			"JSONArray/errors");

		Assert.assertTrue(errorsJSONArray2.length() > 0);
	}

	protected UpgradeRun
			testGraphQLDeleteUpgradeRunByExternalReferenceCode_addUpgradeRun()
		throws Exception {

		return testGraphQLUpgradeRun_addUpgradeRun();
	}

	@Test
	public void testGetUpgradeRunByExternalReferenceCode() throws Exception {
		UpgradeRun postUpgradeRun =
			testGetUpgradeRunByExternalReferenceCode_addUpgradeRun();

		UpgradeRun getUpgradeRun =
			upgradeRunResource.getUpgradeRunByExternalReferenceCode(
				postUpgradeRun.getExternalReferenceCode());

		assertEquals(postUpgradeRun, getUpgradeRun);
		assertValid(getUpgradeRun);
	}

	protected UpgradeRun
			testGetUpgradeRunByExternalReferenceCode_addUpgradeRun()
		throws Exception {

		throw new UnsupportedOperationException(
			"This method needs to be implemented");
	}

	@Test
	public void testGraphQLGetUpgradeRunByExternalReferenceCode()
		throws Exception {

		UpgradeRun upgradeRun =
			testGraphQLGetUpgradeRunByExternalReferenceCode_addUpgradeRun();

		// No namespace

		Assert.assertTrue(
			equals(
				upgradeRun,
				UpgradeRunSerDes.toDTO(
					JSONUtil.getValueAsString(
						invokeGraphQLQuery(
							new GraphQLField(
								"upgradeRunByExternalReferenceCode",
								new HashMap<String, Object>() {
									{
										put(
											"externalReferenceCode",
											"\"" +
												upgradeRun.
													getExternalReferenceCode() +
														"\"");
									}
								},
								getGraphQLFields())),
						"JSONObject/data",
						"Object/upgradeRunByExternalReferenceCode"))));

		// Using the namespace upgradesLabAgentRemote_v1_0

		Assert.assertTrue(
			equals(
				upgradeRun,
				UpgradeRunSerDes.toDTO(
					JSONUtil.getValueAsString(
						invokeGraphQLQuery(
							new GraphQLField(
								"upgradesLabAgentRemote_v1_0",
								new GraphQLField(
									"upgradeRunByExternalReferenceCode",
									new HashMap<String, Object>() {
										{
											put(
												"externalReferenceCode",
												"\"" +
													upgradeRun.
														getExternalReferenceCode() +
															"\"");
										}
									},
									getGraphQLFields()))),
						"JSONObject/data",
						"JSONObject/upgradesLabAgentRemote_v1_0",
						"Object/upgradeRunByExternalReferenceCode"))));
	}

	@Test
	public void testGraphQLGetUpgradeRunByExternalReferenceCodeNotFound()
		throws Exception {

		String irrelevantExternalReferenceCode =
			"\"" + RandomTestUtil.randomString() + "\"";

		// No namespace

		Assert.assertEquals(
			"Not Found",
			JSONUtil.getValueAsString(
				invokeGraphQLQuery(
					new GraphQLField(
						"upgradeRunByExternalReferenceCode",
						new HashMap<String, Object>() {
							{
								put(
									"externalReferenceCode",
									irrelevantExternalReferenceCode);
							}
						},
						getGraphQLFields())),
				"JSONArray/errors", "Object/0", "JSONObject/extensions",
				"Object/code"));

		// Using the namespace upgradesLabAgentRemote_v1_0

		Assert.assertEquals(
			"Not Found",
			JSONUtil.getValueAsString(
				invokeGraphQLQuery(
					new GraphQLField(
						"upgradesLabAgentRemote_v1_0",
						new GraphQLField(
							"upgradeRunByExternalReferenceCode",
							new HashMap<String, Object>() {
								{
									put(
										"externalReferenceCode",
										irrelevantExternalReferenceCode);
								}
							},
							getGraphQLFields()))),
				"JSONArray/errors", "Object/0", "JSONObject/extensions",
				"Object/code"));
	}

	protected UpgradeRun
			testGraphQLGetUpgradeRunByExternalReferenceCode_addUpgradeRun()
		throws Exception {

		return testGraphQLUpgradeRun_addUpgradeRun();
	}

	@Test
	public void testPostUpgradeRun() throws Exception {
		UpgradeRun randomUpgradeRun = randomUpgradeRun();

		UpgradeRun postUpgradeRun = testPostUpgradeRun_addUpgradeRun(
			randomUpgradeRun);

		assertEquals(randomUpgradeRun, postUpgradeRun);
		assertValid(postUpgradeRun);
	}

	protected UpgradeRun testPostUpgradeRun_addUpgradeRun(UpgradeRun upgradeRun)
		throws Exception {

		throw new UnsupportedOperationException(
			"This method needs to be implemented");
	}

	@Test
	public void testGraphQLPostUpgradeRun() throws Exception {
		UpgradeRun randomUpgradeRun = randomUpgradeRun();

		UpgradeRun upgradeRun = testGraphQLUpgradeRun_addUpgradeRun(
			randomUpgradeRun);

		Assert.assertTrue(equals(randomUpgradeRun, upgradeRun));
	}

	protected UpgradeRun testGraphQLUpgradeRun_addUpgradeRun()
		throws Exception {

		return testGraphQLUpgradeRun_addUpgradeRun(randomUpgradeRun());
	}

	protected UpgradeRun testGraphQLUpgradeRun_addUpgradeRun(
			UpgradeRun upgradeRun)
		throws Exception {

		JSONDeserializer<UpgradeRun> jsonDeserializer =
			JSONFactoryUtil.createJSONDeserializer();

		StringBuilder sb = new StringBuilder("{");

		for (java.lang.reflect.Field field :
				getDeclaredFields(UpgradeRun.class)) {

			if (getGraphQLValue(field.get(upgradeRun)) != null) {
				if (sb.length() > 1) {
					sb.append(", ");
				}

				sb.append(field.getName());
				sb.append(": ");
				sb.append(getGraphQLValue(field.get(upgradeRun)));
			}
		}

		sb.append("}");

		List<GraphQLField> graphQLFields = getGraphQLFields();

		return jsonDeserializer.deserialize(
			JSONUtil.getValueAsString(
				invokeGraphQLMutation(
					new GraphQLField(
						"createUpgradeRun",
						new HashMap<String, Object>() {
							{
								put("upgradeRun", sb.toString());
							}
						},
						graphQLFields)),
				"JSONObject/data", "JSONObject/createUpgradeRun"),
			UpgradeRun.class);
	}

	protected String getGraphQLValue(Object value) throws Exception {
		if (value == null) {
			return null;
		}
		else if (value instanceof Boolean || value instanceof Number) {
			return value.toString();
		}
		else if (value instanceof Date) {
			Date date = (Date)value;

			return "\"" +
				DateUtil.getDate(
					date, "yyyy-MM-dd'T'HH:mm:ss'Z'", LocaleUtil.getDefault(),
					TimeZone.getTimeZone("UTC")) + "\"";
		}
		else if (value instanceof Enum) {
			Enum<?> enm = (Enum<?>)value;

			return enm.name();
		}
		else if (value instanceof Map) {
			Map<?, ?> map = (Map<?, ?>)value;

			List<String> entries = new ArrayList<>();

			for (Map.Entry<?, ?> entry : map.entrySet()) {
				String graphQLValue = getGraphQLValue(entry.getValue());

				if (graphQLValue != null) {
					entries.add(entry.getKey() + ": " + graphQLValue);
				}
			}

			return "{" + String.join(", ", entries) + "}";
		}
		else if (value instanceof Object[]) {
			Object[] array = (Object[])value;

			List<String> entries = new ArrayList<>();

			for (Object entry : array) {
				String graphQLValue = getGraphQLValue(entry);

				if (graphQLValue != null) {
					entries.add(graphQLValue);
				}
			}

			return "[" + String.join(", ", entries) + "]";
		}
		else if (value instanceof String) {
			return "\"" + value + "\"";
		}
		else {
			List<String> entries = new ArrayList<>();

			Class<?> clazz = value.getClass();
			java.lang.reflect.Field[] declaredFields = getDeclaredFields(clazz);

			if (declaredFields.length == 0) {
				declaredFields = getDeclaredFields(clazz.getSuperclass());
			}

			for (java.lang.reflect.Field field : declaredFields) {
				String graphQLValue = getGraphQLValue(field.get(value));

				if (graphQLValue != null) {
					entries.add(field.getName() + ": " + graphQLValue);
				}
			}

			return "{" + String.join(", ", entries) + "}";
		}
	}

	protected void assertContains(
		UpgradeRun upgradeRun, List<UpgradeRun> upgradeRuns) {

		boolean contains = false;

		for (UpgradeRun item : upgradeRuns) {
			if (equals(upgradeRun, item)) {
				contains = true;

				break;
			}
		}

		Assert.assertTrue(
			upgradeRuns + " does not contain " + upgradeRun, contains);
	}

	protected void assertHttpResponseStatusCode(
		int expectedHttpResponseStatusCode,
		HttpInvoker.HttpResponse actualHttpResponse) {

		Assert.assertEquals(
			expectedHttpResponseStatusCode, actualHttpResponse.getStatusCode());
	}

	protected void assertEquals(
		UpgradeRun upgradeRun1, UpgradeRun upgradeRun2) {

		Assert.assertTrue(
			upgradeRun1 + " does not equal " + upgradeRun2,
			equals(upgradeRun1, upgradeRun2));
	}

	protected void assertEquals(
		List<UpgradeRun> upgradeRuns1, List<UpgradeRun> upgradeRuns2) {

		Assert.assertEquals(upgradeRuns1.size(), upgradeRuns2.size());

		for (int i = 0; i < upgradeRuns1.size(); i++) {
			UpgradeRun upgradeRun1 = upgradeRuns1.get(i);
			UpgradeRun upgradeRun2 = upgradeRuns2.get(i);

			assertEquals(upgradeRun1, upgradeRun2);
		}
	}

	protected void assertEqualsIgnoringOrder(
		List<UpgradeRun> upgradeRuns1, List<UpgradeRun> upgradeRuns2) {

		Assert.assertEquals(upgradeRuns1.size(), upgradeRuns2.size());

		for (UpgradeRun upgradeRun1 : upgradeRuns1) {
			boolean contains = false;

			for (UpgradeRun upgradeRun2 : upgradeRuns2) {
				if (equals(upgradeRun1, upgradeRun2)) {
					contains = true;

					break;
				}
			}

			Assert.assertTrue(
				upgradeRuns2 + " does not contain " + upgradeRun1, contains);
		}
	}

	protected void assertValid(UpgradeRun upgradeRun) throws Exception {
		boolean valid = true;

		if (upgradeRun.getUpgradeRunId() == null) {
			valid = false;
		}

		for (String additionalAssertFieldName :
				getAdditionalAssertFieldNames()) {

			if (Objects.equals("branch", additionalAssertFieldName)) {
				if (upgradeRun.getBranch() == null) {
					valid = false;
				}

				continue;
			}

			if (Objects.equals(
					"credentialKeyReference", additionalAssertFieldName)) {

				if (upgradeRun.getCredentialKeyReference() == null) {
					valid = false;
				}

				continue;
			}

			if (Objects.equals("customerName", additionalAssertFieldName)) {
				if (upgradeRun.getCustomerName() == null) {
					valid = false;
				}

				continue;
			}

			if (Objects.equals("dbTargetType", additionalAssertFieldName)) {
				if (upgradeRun.getDbTargetType() == null) {
					valid = false;
				}

				continue;
			}

			if (Objects.equals("dbTargetVersion", additionalAssertFieldName)) {
				if (upgradeRun.getDbTargetVersion() == null) {
					valid = false;
				}

				continue;
			}

			if (Objects.equals(
					"externalReferenceCode", additionalAssertFieldName)) {

				if (upgradeRun.getExternalReferenceCode() == null) {
					valid = false;
				}

				continue;
			}

			if (Objects.equals("nodeVersion", additionalAssertFieldName)) {
				if (upgradeRun.getNodeVersion() == null) {
					valid = false;
				}

				continue;
			}

			if (Objects.equals("pullRequestURL", additionalAssertFieldName)) {
				if (upgradeRun.getPullRequestURL() == null) {
					valid = false;
				}

				continue;
			}

			if (Objects.equals("repositoryURL", additionalAssertFieldName)) {
				if (upgradeRun.getRepositoryURL() == null) {
					valid = false;
				}

				continue;
			}

			if (Objects.equals("resultBranch", additionalAssertFieldName)) {
				if (upgradeRun.getResultBranch() == null) {
					valid = false;
				}

				continue;
			}

			if (Objects.equals("searchVersion", additionalAssertFieldName)) {
				if (upgradeRun.getSearchVersion() == null) {
					valid = false;
				}

				continue;
			}

			if (Objects.equals("status", additionalAssertFieldName)) {
				if (upgradeRun.getStatus() == null) {
					valid = false;
				}

				continue;
			}

			if (Objects.equals("statusMessage", additionalAssertFieldName)) {
				if (upgradeRun.getStatusMessage() == null) {
					valid = false;
				}

				continue;
			}

			if (Objects.equals("targetRelease", additionalAssertFieldName)) {
				if (upgradeRun.getTargetRelease() == null) {
					valid = false;
				}

				continue;
			}

			if (Objects.equals("upgradeRunId", additionalAssertFieldName)) {
				if (upgradeRun.getUpgradeRunId() == null) {
					valid = false;
				}

				continue;
			}

			if (Objects.equals(
					"upgradeSourceVersion", additionalAssertFieldName)) {

				if (upgradeRun.getUpgradeSourceVersion() == null) {
					valid = false;
				}

				continue;
			}

			if (Objects.equals(
					"upgradeTargetJavaVersion", additionalAssertFieldName)) {

				if (upgradeRun.getUpgradeTargetJavaVersion() == null) {
					valid = false;
				}

				continue;
			}

			if (Objects.equals("workspacePath", additionalAssertFieldName)) {
				if (upgradeRun.getWorkspacePath() == null) {
					valid = false;
				}

				continue;
			}

			throw new IllegalArgumentException(
				"Invalid additional assert field name " +
					additionalAssertFieldName);
		}

		Assert.assertTrue(valid);
	}

	protected void assertValid(Page<UpgradeRun> page) {
		assertValid(page, Collections.emptyMap());
	}

	protected void assertValid(
		Page<UpgradeRun> page,
		Map<String, Map<String, String>> expectedActions) {

		boolean valid = false;

		java.util.Collection<UpgradeRun> upgradeRuns = page.getItems();

		int size = upgradeRuns.size();

		if ((page.getLastPage() > 0) && (page.getPage() > 0) &&
			(page.getPageSize() > 0) && (page.getTotalCount() > 0) &&
			(size > 0)) {

			valid = true;
		}

		Assert.assertTrue(valid);

		assertValid(page.getActions(), expectedActions);
	}

	protected void assertValid(
		Map<String, Map<String, String>> actions1,
		Map<String, Map<String, String>> actions2) {

		for (String key : actions2.keySet()) {
			Map action = actions1.get(key);

			Assert.assertNotNull(key + " does not contain an action", action);

			Map<String, String> expectedAction = actions2.get(key);

			Assert.assertEquals(
				expectedAction.get("method"), action.get("method"));
			Assert.assertEquals(expectedAction.get("href"), action.get("href"));
		}
	}

	protected String[] getAdditionalAssertFieldNames() {
		return new String[0];
	}

	protected List<GraphQLField> getGraphQLFields() throws Exception {
		List<GraphQLField> graphQLFields = new ArrayList<>();

		graphQLFields.add(new GraphQLField("externalReferenceCode"));

		graphQLFields.add(new GraphQLField("upgradeRunId"));

		for (java.lang.reflect.Field field :
				getDeclaredFields(
					com.liferay.upgrades.lab.agent.remote.rest.dto.v1_0.
						UpgradeRun.class)) {

			if (!ArrayUtil.contains(
					getAdditionalAssertFieldNames(), field.getName())) {

				continue;
			}

			graphQLFields.addAll(getGraphQLFields(field));
		}

		return graphQLFields;
	}

	protected List<GraphQLField> getGraphQLFields(
			java.lang.reflect.Field... fields)
		throws Exception {

		List<GraphQLField> graphQLFields = new ArrayList<>();

		for (java.lang.reflect.Field field : fields) {
			com.liferay.portal.vulcan.graphql.annotation.GraphQLField
				vulcanGraphQLField = field.getAnnotation(
					com.liferay.portal.vulcan.graphql.annotation.GraphQLField.
						class);

			if (vulcanGraphQLField != null) {
				Class<?> clazz = field.getType();

				if (clazz.isArray()) {
					clazz = clazz.getComponentType();
				}

				List<GraphQLField> childrenGraphQLFields = getGraphQLFields(
					getDeclaredFields(clazz));

				graphQLFields.add(
					new GraphQLField(field.getName(), childrenGraphQLFields));
			}
		}

		return graphQLFields;
	}

	protected String[] getIgnoredEntityFieldNames() {
		return new String[0];
	}

	protected boolean equals(UpgradeRun upgradeRun1, UpgradeRun upgradeRun2) {
		if (upgradeRun1 == upgradeRun2) {
			return true;
		}

		for (String additionalAssertFieldName :
				getAdditionalAssertFieldNames()) {

			if (Objects.equals("branch", additionalAssertFieldName)) {
				if (!Objects.deepEquals(
						upgradeRun1.getBranch(), upgradeRun2.getBranch())) {

					return false;
				}

				continue;
			}

			if (Objects.equals(
					"credentialKeyReference", additionalAssertFieldName)) {

				if (!Objects.deepEquals(
						upgradeRun1.getCredentialKeyReference(),
						upgradeRun2.getCredentialKeyReference())) {

					return false;
				}

				continue;
			}

			if (Objects.equals("customerName", additionalAssertFieldName)) {
				if (!Objects.deepEquals(
						upgradeRun1.getCustomerName(),
						upgradeRun2.getCustomerName())) {

					return false;
				}

				continue;
			}

			if (Objects.equals("dbTargetType", additionalAssertFieldName)) {
				if (!Objects.deepEquals(
						upgradeRun1.getDbTargetType(),
						upgradeRun2.getDbTargetType())) {

					return false;
				}

				continue;
			}

			if (Objects.equals("dbTargetVersion", additionalAssertFieldName)) {
				if (!Objects.deepEquals(
						upgradeRun1.getDbTargetVersion(),
						upgradeRun2.getDbTargetVersion())) {

					return false;
				}

				continue;
			}

			if (Objects.equals(
					"externalReferenceCode", additionalAssertFieldName)) {

				if (!Objects.deepEquals(
						upgradeRun1.getExternalReferenceCode(),
						upgradeRun2.getExternalReferenceCode())) {

					return false;
				}

				continue;
			}

			if (Objects.equals("nodeVersion", additionalAssertFieldName)) {
				if (!Objects.deepEquals(
						upgradeRun1.getNodeVersion(),
						upgradeRun2.getNodeVersion())) {

					return false;
				}

				continue;
			}

			if (Objects.equals("pullRequestURL", additionalAssertFieldName)) {
				if (!Objects.deepEquals(
						upgradeRun1.getPullRequestURL(),
						upgradeRun2.getPullRequestURL())) {

					return false;
				}

				continue;
			}

			if (Objects.equals("repositoryURL", additionalAssertFieldName)) {
				if (!Objects.deepEquals(
						upgradeRun1.getRepositoryURL(),
						upgradeRun2.getRepositoryURL())) {

					return false;
				}

				continue;
			}

			if (Objects.equals("resultBranch", additionalAssertFieldName)) {
				if (!Objects.deepEquals(
						upgradeRun1.getResultBranch(),
						upgradeRun2.getResultBranch())) {

					return false;
				}

				continue;
			}

			if (Objects.equals("searchVersion", additionalAssertFieldName)) {
				if (!Objects.deepEquals(
						upgradeRun1.getSearchVersion(),
						upgradeRun2.getSearchVersion())) {

					return false;
				}

				continue;
			}

			if (Objects.equals("status", additionalAssertFieldName)) {
				if (!Objects.deepEquals(
						upgradeRun1.getStatus(), upgradeRun2.getStatus())) {

					return false;
				}

				continue;
			}

			if (Objects.equals("statusMessage", additionalAssertFieldName)) {
				if (!Objects.deepEquals(
						upgradeRun1.getStatusMessage(),
						upgradeRun2.getStatusMessage())) {

					return false;
				}

				continue;
			}

			if (Objects.equals("targetRelease", additionalAssertFieldName)) {
				if (!Objects.deepEquals(
						upgradeRun1.getTargetRelease(),
						upgradeRun2.getTargetRelease())) {

					return false;
				}

				continue;
			}

			if (Objects.equals("upgradeRunId", additionalAssertFieldName)) {
				if (!Objects.deepEquals(
						upgradeRun1.getUpgradeRunId(),
						upgradeRun2.getUpgradeRunId())) {

					return false;
				}

				continue;
			}

			if (Objects.equals(
					"upgradeSourceVersion", additionalAssertFieldName)) {

				if (!Objects.deepEquals(
						upgradeRun1.getUpgradeSourceVersion(),
						upgradeRun2.getUpgradeSourceVersion())) {

					return false;
				}

				continue;
			}

			if (Objects.equals(
					"upgradeTargetJavaVersion", additionalAssertFieldName)) {

				if (!Objects.deepEquals(
						upgradeRun1.getUpgradeTargetJavaVersion(),
						upgradeRun2.getUpgradeTargetJavaVersion())) {

					return false;
				}

				continue;
			}

			if (Objects.equals("workspacePath", additionalAssertFieldName)) {
				if (!Objects.deepEquals(
						upgradeRun1.getWorkspacePath(),
						upgradeRun2.getWorkspacePath())) {

					return false;
				}

				continue;
			}

			throw new IllegalArgumentException(
				"Invalid additional assert field name " +
					additionalAssertFieldName);
		}

		return true;
	}

	protected boolean equals(
		Map<String, Object> map1, Map<String, Object> map2) {

		if (Objects.equals(map1.keySet(), map2.keySet())) {
			for (Map.Entry<String, Object> entry : map1.entrySet()) {
				if (entry.getValue() instanceof Map) {
					if (!equals(
							(Map)entry.getValue(),
							(Map)map2.get(entry.getKey()))) {

						return false;
					}
				}
				else if (!Objects.deepEquals(
							entry.getValue(), map2.get(entry.getKey()))) {

					return false;
				}
			}

			return true;
		}

		return false;
	}

	protected java.lang.reflect.Field[] getDeclaredFields(Class clazz)
		throws Exception {

		if (clazz.getClassLoader() == null) {
			return new java.lang.reflect.Field[0];
		}

		return TransformUtil.transform(
			ReflectionUtil.getDeclaredFields(clazz),
			field -> {
				if (field.isSynthetic()) {
					return null;
				}

				return field;
			},
			java.lang.reflect.Field.class);
	}

	protected java.util.Collection<EntityField> getEntityFields()
		throws Exception {

		if (!(_upgradeRunResource instanceof EntityModelResource)) {
			throw new UnsupportedOperationException(
				"Resource is not an instance of EntityModelResource");
		}

		EntityModelResource entityModelResource =
			(EntityModelResource)_upgradeRunResource;

		EntityModel entityModel = entityModelResource.getEntityModel(
			new MultivaluedHashMap());

		if (entityModel == null) {
			return Collections.emptyList();
		}

		Map<String, EntityField> entityFieldsMap =
			entityModel.getEntityFieldsMap();

		return entityFieldsMap.values();
	}

	protected List<EntityField> getEntityFields(EntityField.Type type)
		throws Exception {

		return TransformUtil.transform(
			getEntityFields(),
			entityField -> {
				if (!Objects.equals(entityField.getType(), type) ||
					ArrayUtil.contains(
						getIgnoredEntityFieldNames(), entityField.getName())) {

					return null;
				}

				return entityField;
			});
	}

	protected String getFilterString(
		EntityField entityField, String operator, UpgradeRun upgradeRun) {

		StringBundler sb = new StringBundler();

		String entityFieldName = entityField.getName();

		sb.append(entityFieldName);

		sb.append(" ");
		sb.append(operator);
		sb.append(" ");

		if (entityFieldName.equals("branch")) {
			Object object = upgradeRun.getBranch();

			String value = String.valueOf(object);

			if (operator.equals("contains")) {
				sb = new StringBundler();

				sb.append("contains(");
				sb.append(entityFieldName);
				sb.append(",'");

				if ((object != null) && (value.length() > 2)) {
					sb.append(value.substring(1, value.length() - 1));
				}
				else {
					sb.append(value);
				}

				sb.append("')");
			}
			else if (operator.equals("startswith")) {
				sb = new StringBundler();

				sb.append("startswith(");
				sb.append(entityFieldName);
				sb.append(",'");

				if ((object != null) && (value.length() > 1)) {
					sb.append(value.substring(0, value.length() - 1));
				}
				else {
					sb.append(value);
				}

				sb.append("')");
			}
			else {
				sb.append("'");
				sb.append(value);
				sb.append("'");
			}

			return sb.toString();
		}

		if (entityFieldName.equals("credentialKeyReference")) {
			Object object = upgradeRun.getCredentialKeyReference();

			String value = String.valueOf(object);

			if (operator.equals("contains")) {
				sb = new StringBundler();

				sb.append("contains(");
				sb.append(entityFieldName);
				sb.append(",'");

				if ((object != null) && (value.length() > 2)) {
					sb.append(value.substring(1, value.length() - 1));
				}
				else {
					sb.append(value);
				}

				sb.append("')");
			}
			else if (operator.equals("startswith")) {
				sb = new StringBundler();

				sb.append("startswith(");
				sb.append(entityFieldName);
				sb.append(",'");

				if ((object != null) && (value.length() > 1)) {
					sb.append(value.substring(0, value.length() - 1));
				}
				else {
					sb.append(value);
				}

				sb.append("')");
			}
			else {
				sb.append("'");
				sb.append(value);
				sb.append("'");
			}

			return sb.toString();
		}

		if (entityFieldName.equals("customerName")) {
			Object object = upgradeRun.getCustomerName();

			String value = String.valueOf(object);

			if (operator.equals("contains")) {
				sb = new StringBundler();

				sb.append("contains(");
				sb.append(entityFieldName);
				sb.append(",'");

				if ((object != null) && (value.length() > 2)) {
					sb.append(value.substring(1, value.length() - 1));
				}
				else {
					sb.append(value);
				}

				sb.append("')");
			}
			else if (operator.equals("startswith")) {
				sb = new StringBundler();

				sb.append("startswith(");
				sb.append(entityFieldName);
				sb.append(",'");

				if ((object != null) && (value.length() > 1)) {
					sb.append(value.substring(0, value.length() - 1));
				}
				else {
					sb.append(value);
				}

				sb.append("')");
			}
			else {
				sb.append("'");
				sb.append(value);
				sb.append("'");
			}

			return sb.toString();
		}

		if (entityFieldName.equals("dbTargetType")) {
			Object object = upgradeRun.getDbTargetType();

			String value = String.valueOf(object);

			if (operator.equals("contains")) {
				sb = new StringBundler();

				sb.append("contains(");
				sb.append(entityFieldName);
				sb.append(",'");

				if ((object != null) && (value.length() > 2)) {
					sb.append(value.substring(1, value.length() - 1));
				}
				else {
					sb.append(value);
				}

				sb.append("')");
			}
			else if (operator.equals("startswith")) {
				sb = new StringBundler();

				sb.append("startswith(");
				sb.append(entityFieldName);
				sb.append(",'");

				if ((object != null) && (value.length() > 1)) {
					sb.append(value.substring(0, value.length() - 1));
				}
				else {
					sb.append(value);
				}

				sb.append("')");
			}
			else {
				sb.append("'");
				sb.append(value);
				sb.append("'");
			}

			return sb.toString();
		}

		if (entityFieldName.equals("dbTargetVersion")) {
			Object object = upgradeRun.getDbTargetVersion();

			String value = String.valueOf(object);

			if (operator.equals("contains")) {
				sb = new StringBundler();

				sb.append("contains(");
				sb.append(entityFieldName);
				sb.append(",'");

				if ((object != null) && (value.length() > 2)) {
					sb.append(value.substring(1, value.length() - 1));
				}
				else {
					sb.append(value);
				}

				sb.append("')");
			}
			else if (operator.equals("startswith")) {
				sb = new StringBundler();

				sb.append("startswith(");
				sb.append(entityFieldName);
				sb.append(",'");

				if ((object != null) && (value.length() > 1)) {
					sb.append(value.substring(0, value.length() - 1));
				}
				else {
					sb.append(value);
				}

				sb.append("')");
			}
			else {
				sb.append("'");
				sb.append(value);
				sb.append("'");
			}

			return sb.toString();
		}

		if (entityFieldName.equals("externalReferenceCode")) {
			Object object = upgradeRun.getExternalReferenceCode();

			String value = String.valueOf(object);

			if (operator.equals("contains")) {
				sb = new StringBundler();

				sb.append("contains(");
				sb.append(entityFieldName);
				sb.append(",'");

				if ((object != null) && (value.length() > 2)) {
					sb.append(value.substring(1, value.length() - 1));
				}
				else {
					sb.append(value);
				}

				sb.append("')");
			}
			else if (operator.equals("startswith")) {
				sb = new StringBundler();

				sb.append("startswith(");
				sb.append(entityFieldName);
				sb.append(",'");

				if ((object != null) && (value.length() > 1)) {
					sb.append(value.substring(0, value.length() - 1));
				}
				else {
					sb.append(value);
				}

				sb.append("')");
			}
			else {
				sb.append("'");
				sb.append(value);
				sb.append("'");
			}

			return sb.toString();
		}

		if (entityFieldName.equals("nodeVersion")) {
			Object object = upgradeRun.getNodeVersion();

			String value = String.valueOf(object);

			if (operator.equals("contains")) {
				sb = new StringBundler();

				sb.append("contains(");
				sb.append(entityFieldName);
				sb.append(",'");

				if ((object != null) && (value.length() > 2)) {
					sb.append(value.substring(1, value.length() - 1));
				}
				else {
					sb.append(value);
				}

				sb.append("')");
			}
			else if (operator.equals("startswith")) {
				sb = new StringBundler();

				sb.append("startswith(");
				sb.append(entityFieldName);
				sb.append(",'");

				if ((object != null) && (value.length() > 1)) {
					sb.append(value.substring(0, value.length() - 1));
				}
				else {
					sb.append(value);
				}

				sb.append("')");
			}
			else {
				sb.append("'");
				sb.append(value);
				sb.append("'");
			}

			return sb.toString();
		}

		if (entityFieldName.equals("pullRequestURL")) {
			Object object = upgradeRun.getPullRequestURL();

			String value = String.valueOf(object);

			if (operator.equals("contains")) {
				sb = new StringBundler();

				sb.append("contains(");
				sb.append(entityFieldName);
				sb.append(",'");

				if ((object != null) && (value.length() > 2)) {
					sb.append(value.substring(1, value.length() - 1));
				}
				else {
					sb.append(value);
				}

				sb.append("')");
			}
			else if (operator.equals("startswith")) {
				sb = new StringBundler();

				sb.append("startswith(");
				sb.append(entityFieldName);
				sb.append(",'");

				if ((object != null) && (value.length() > 1)) {
					sb.append(value.substring(0, value.length() - 1));
				}
				else {
					sb.append(value);
				}

				sb.append("')");
			}
			else {
				sb.append("'");
				sb.append(value);
				sb.append("'");
			}

			return sb.toString();
		}

		if (entityFieldName.equals("repositoryURL")) {
			Object object = upgradeRun.getRepositoryURL();

			String value = String.valueOf(object);

			if (operator.equals("contains")) {
				sb = new StringBundler();

				sb.append("contains(");
				sb.append(entityFieldName);
				sb.append(",'");

				if ((object != null) && (value.length() > 2)) {
					sb.append(value.substring(1, value.length() - 1));
				}
				else {
					sb.append(value);
				}

				sb.append("')");
			}
			else if (operator.equals("startswith")) {
				sb = new StringBundler();

				sb.append("startswith(");
				sb.append(entityFieldName);
				sb.append(",'");

				if ((object != null) && (value.length() > 1)) {
					sb.append(value.substring(0, value.length() - 1));
				}
				else {
					sb.append(value);
				}

				sb.append("')");
			}
			else {
				sb.append("'");
				sb.append(value);
				sb.append("'");
			}

			return sb.toString();
		}

		if (entityFieldName.equals("resultBranch")) {
			Object object = upgradeRun.getResultBranch();

			String value = String.valueOf(object);

			if (operator.equals("contains")) {
				sb = new StringBundler();

				sb.append("contains(");
				sb.append(entityFieldName);
				sb.append(",'");

				if ((object != null) && (value.length() > 2)) {
					sb.append(value.substring(1, value.length() - 1));
				}
				else {
					sb.append(value);
				}

				sb.append("')");
			}
			else if (operator.equals("startswith")) {
				sb = new StringBundler();

				sb.append("startswith(");
				sb.append(entityFieldName);
				sb.append(",'");

				if ((object != null) && (value.length() > 1)) {
					sb.append(value.substring(0, value.length() - 1));
				}
				else {
					sb.append(value);
				}

				sb.append("')");
			}
			else {
				sb.append("'");
				sb.append(value);
				sb.append("'");
			}

			return sb.toString();
		}

		if (entityFieldName.equals("searchVersion")) {
			Object object = upgradeRun.getSearchVersion();

			String value = String.valueOf(object);

			if (operator.equals("contains")) {
				sb = new StringBundler();

				sb.append("contains(");
				sb.append(entityFieldName);
				sb.append(",'");

				if ((object != null) && (value.length() > 2)) {
					sb.append(value.substring(1, value.length() - 1));
				}
				else {
					sb.append(value);
				}

				sb.append("')");
			}
			else if (operator.equals("startswith")) {
				sb = new StringBundler();

				sb.append("startswith(");
				sb.append(entityFieldName);
				sb.append(",'");

				if ((object != null) && (value.length() > 1)) {
					sb.append(value.substring(0, value.length() - 1));
				}
				else {
					sb.append(value);
				}

				sb.append("')");
			}
			else {
				sb.append("'");
				sb.append(value);
				sb.append("'");
			}

			return sb.toString();
		}

		if (entityFieldName.equals("status")) {
			throw new IllegalArgumentException(
				"Invalid entity field " + entityFieldName);
		}

		if (entityFieldName.equals("statusMessage")) {
			Object object = upgradeRun.getStatusMessage();

			String value = String.valueOf(object);

			if (operator.equals("contains")) {
				sb = new StringBundler();

				sb.append("contains(");
				sb.append(entityFieldName);
				sb.append(",'");

				if ((object != null) && (value.length() > 2)) {
					sb.append(value.substring(1, value.length() - 1));
				}
				else {
					sb.append(value);
				}

				sb.append("')");
			}
			else if (operator.equals("startswith")) {
				sb = new StringBundler();

				sb.append("startswith(");
				sb.append(entityFieldName);
				sb.append(",'");

				if ((object != null) && (value.length() > 1)) {
					sb.append(value.substring(0, value.length() - 1));
				}
				else {
					sb.append(value);
				}

				sb.append("')");
			}
			else {
				sb.append("'");
				sb.append(value);
				sb.append("'");
			}

			return sb.toString();
		}

		if (entityFieldName.equals("targetRelease")) {
			Object object = upgradeRun.getTargetRelease();

			String value = String.valueOf(object);

			if (operator.equals("contains")) {
				sb = new StringBundler();

				sb.append("contains(");
				sb.append(entityFieldName);
				sb.append(",'");

				if ((object != null) && (value.length() > 2)) {
					sb.append(value.substring(1, value.length() - 1));
				}
				else {
					sb.append(value);
				}

				sb.append("')");
			}
			else if (operator.equals("startswith")) {
				sb = new StringBundler();

				sb.append("startswith(");
				sb.append(entityFieldName);
				sb.append(",'");

				if ((object != null) && (value.length() > 1)) {
					sb.append(value.substring(0, value.length() - 1));
				}
				else {
					sb.append(value);
				}

				sb.append("')");
			}
			else {
				sb.append("'");
				sb.append(value);
				sb.append("'");
			}

			return sb.toString();
		}

		if (entityFieldName.equals("upgradeRunId")) {
			throw new IllegalArgumentException(
				"Invalid entity field " + entityFieldName);
		}

		if (entityFieldName.equals("upgradeSourceVersion")) {
			Object object = upgradeRun.getUpgradeSourceVersion();

			String value = String.valueOf(object);

			if (operator.equals("contains")) {
				sb = new StringBundler();

				sb.append("contains(");
				sb.append(entityFieldName);
				sb.append(",'");

				if ((object != null) && (value.length() > 2)) {
					sb.append(value.substring(1, value.length() - 1));
				}
				else {
					sb.append(value);
				}

				sb.append("')");
			}
			else if (operator.equals("startswith")) {
				sb = new StringBundler();

				sb.append("startswith(");
				sb.append(entityFieldName);
				sb.append(",'");

				if ((object != null) && (value.length() > 1)) {
					sb.append(value.substring(0, value.length() - 1));
				}
				else {
					sb.append(value);
				}

				sb.append("')");
			}
			else {
				sb.append("'");
				sb.append(value);
				sb.append("'");
			}

			return sb.toString();
		}

		if (entityFieldName.equals("upgradeTargetJavaVersion")) {
			Object object = upgradeRun.getUpgradeTargetJavaVersion();

			String value = String.valueOf(object);

			if (operator.equals("contains")) {
				sb = new StringBundler();

				sb.append("contains(");
				sb.append(entityFieldName);
				sb.append(",'");

				if ((object != null) && (value.length() > 2)) {
					sb.append(value.substring(1, value.length() - 1));
				}
				else {
					sb.append(value);
				}

				sb.append("')");
			}
			else if (operator.equals("startswith")) {
				sb = new StringBundler();

				sb.append("startswith(");
				sb.append(entityFieldName);
				sb.append(",'");

				if ((object != null) && (value.length() > 1)) {
					sb.append(value.substring(0, value.length() - 1));
				}
				else {
					sb.append(value);
				}

				sb.append("')");
			}
			else {
				sb.append("'");
				sb.append(value);
				sb.append("'");
			}

			return sb.toString();
		}

		if (entityFieldName.equals("workspacePath")) {
			Object object = upgradeRun.getWorkspacePath();

			String value = String.valueOf(object);

			if (operator.equals("contains")) {
				sb = new StringBundler();

				sb.append("contains(");
				sb.append(entityFieldName);
				sb.append(",'");

				if ((object != null) && (value.length() > 2)) {
					sb.append(value.substring(1, value.length() - 1));
				}
				else {
					sb.append(value);
				}

				sb.append("')");
			}
			else if (operator.equals("startswith")) {
				sb = new StringBundler();

				sb.append("startswith(");
				sb.append(entityFieldName);
				sb.append(",'");

				if ((object != null) && (value.length() > 1)) {
					sb.append(value.substring(0, value.length() - 1));
				}
				else {
					sb.append(value);
				}

				sb.append("')");
			}
			else {
				sb.append("'");
				sb.append(value);
				sb.append("'");
			}

			return sb.toString();
		}

		throw new IllegalArgumentException(
			"Invalid entity field " + entityFieldName);
	}

	protected String invoke(String query) throws Exception {
		HttpInvoker httpInvoker = HttpInvoker.newHttpInvoker();

		httpInvoker.body(
			JSONUtil.put(
				"query", query
			).toString(),
			"application/json");
		httpInvoker.httpMethod(HttpInvoker.HttpMethod.POST);
		httpInvoker.path(
			"http://localhost:" + PortalUtil.getPortalServerPort(false) +
				"/o/graphql");
		httpInvoker.userNameAndPassword(
			"test@liferay.com:" + PropsValues.DEFAULT_ADMIN_PASSWORD);

		HttpInvoker.HttpResponse httpResponse = httpInvoker.invoke();

		return httpResponse.getContent();
	}

	protected JSONObject invokeGraphQLMutation(GraphQLField graphQLField)
		throws Exception {

		GraphQLField mutationGraphQLField = new GraphQLField(
			"mutation", graphQLField);

		return JSONFactoryUtil.createJSONObject(
			invoke(mutationGraphQLField.toString()));
	}

	protected JSONObject invokeGraphQLQuery(GraphQLField graphQLField)
		throws Exception {

		GraphQLField queryGraphQLField = new GraphQLField(
			"query", graphQLField);

		return JSONFactoryUtil.createJSONObject(
			invoke(queryGraphQLField.toString()));
	}

	protected UpgradeRun randomUpgradeRun() throws Exception {
		return new UpgradeRun() {
			{
				branch = StringUtil.toLowerCase(RandomTestUtil.randomString());
				credentialKeyReference = StringUtil.toLowerCase(
					RandomTestUtil.randomString());
				customerName = StringUtil.toLowerCase(
					RandomTestUtil.randomString());
				dbTargetType = StringUtil.toLowerCase(
					RandomTestUtil.randomString());
				dbTargetVersion = StringUtil.toLowerCase(
					RandomTestUtil.randomString());
				externalReferenceCode = StringUtil.toLowerCase(
					RandomTestUtil.randomString());
				nodeVersion = StringUtil.toLowerCase(
					RandomTestUtil.randomString());
				pullRequestURL = StringUtil.toLowerCase(
					RandomTestUtil.randomString());
				repositoryURL = StringUtil.toLowerCase(
					RandomTestUtil.randomString());
				resultBranch = StringUtil.toLowerCase(
					RandomTestUtil.randomString());
				searchVersion = StringUtil.toLowerCase(
					RandomTestUtil.randomString());
				statusMessage = StringUtil.toLowerCase(
					RandomTestUtil.randomString());
				targetRelease = StringUtil.toLowerCase(
					RandomTestUtil.randomString());
				upgradeRunId = RandomTestUtil.randomLong();
				upgradeSourceVersion = StringUtil.toLowerCase(
					RandomTestUtil.randomString());
				upgradeTargetJavaVersion = StringUtil.toLowerCase(
					RandomTestUtil.randomString());
				workspacePath = StringUtil.toLowerCase(
					RandomTestUtil.randomString());
			}
		};
	}

	protected UpgradeRun randomIrrelevantUpgradeRun() throws Exception {
		UpgradeRun randomIrrelevantUpgradeRun = randomUpgradeRun();

		return randomIrrelevantUpgradeRun;
	}

	protected UpgradeRun randomPatchUpgradeRun() throws Exception {
		return randomUpgradeRun();
	}

	protected UpgradeRunResource upgradeRunResource;
	protected com.liferay.portal.kernel.model.Group irrelevantGroup;
	protected com.liferay.portal.kernel.model.Company testCompany;
	protected com.liferay.portal.kernel.model.Group testGroup;

	protected static class BeanTestUtil {

		public static void copyProperties(Object source, Object target)
			throws Exception {

			Class<?> sourceClass = source.getClass();

			Class<?> targetClass = target.getClass();

			for (java.lang.reflect.Field field :
					_getAllDeclaredFields(sourceClass)) {

				if (field.isSynthetic()) {
					continue;
				}

				Method getMethod = _getMethod(
					sourceClass, field.getName(), "get");

				try {
					Method setMethod = _getMethod(
						targetClass, field.getName(), "set",
						getMethod.getReturnType());

					setMethod.invoke(target, getMethod.invoke(source));
				}
				catch (Exception e) {
					continue;
				}
			}
		}

		public static boolean hasProperty(Object bean, String name) {
			Method setMethod = _getMethod(
				bean.getClass(), "set" + StringUtil.upperCaseFirstLetter(name));

			if (setMethod != null) {
				return true;
			}

			return false;
		}

		public static void setProperty(Object bean, String name, Object value)
			throws Exception {

			Class<?> clazz = bean.getClass();

			Method setMethod = _getMethod(
				clazz, "set" + StringUtil.upperCaseFirstLetter(name));

			if (setMethod == null) {
				throw new NoSuchMethodException();
			}

			Class<?>[] parameterTypes = setMethod.getParameterTypes();

			setMethod.invoke(bean, _translateValue(parameterTypes[0], value));
		}

		private static List<java.lang.reflect.Field> _getAllDeclaredFields(
			Class<?> clazz) {

			List<java.lang.reflect.Field> fields = new ArrayList<>();

			while ((clazz != null) && (clazz != Object.class)) {
				for (java.lang.reflect.Field field :
						clazz.getDeclaredFields()) {

					fields.add(field);
				}

				clazz = clazz.getSuperclass();
			}

			return fields;
		}

		private static Method _getMethod(Class<?> clazz, String name) {
			for (Method method : clazz.getMethods()) {
				if (name.equals(method.getName()) &&
					(method.getParameterCount() == 1) &&
					_parameterTypes.contains(method.getParameterTypes()[0])) {

					return method;
				}
			}

			return null;
		}

		private static Method _getMethod(
				Class<?> clazz, String fieldName, String prefix,
				Class<?>... parameterTypes)
			throws Exception {

			return clazz.getMethod(
				prefix + StringUtil.upperCaseFirstLetter(fieldName),
				parameterTypes);
		}

		private static Object _translateValue(
			Class<?> parameterType, Object value) {

			if ((value instanceof Integer) &&
				parameterType.equals(Long.class)) {

				Integer intValue = (Integer)value;

				return intValue.longValue();
			}

			return value;
		}

		private static final Set<Class<?>> _parameterTypes = new HashSet<>(
			Arrays.asList(
				Boolean.class, Date.class, Double.class, Integer.class,
				Long.class, Map.class, String.class));

	}

	protected class GraphQLField {

		public GraphQLField(String key, GraphQLField... graphQLFields) {
			this(key, new HashMap<>(), graphQLFields);
		}

		public GraphQLField(String key, List<GraphQLField> graphQLFields) {
			this(key, new HashMap<>(), graphQLFields);
		}

		public GraphQLField(
			String key, Map<String, Object> parameterMap,
			GraphQLField... graphQLFields) {

			_key = key;
			_parameterMap = parameterMap;
			_graphQLFields = Arrays.asList(graphQLFields);
		}

		public GraphQLField(
			String key, Map<String, Object> parameterMap,
			List<GraphQLField> graphQLFields) {

			_key = key;
			_parameterMap = parameterMap;
			_graphQLFields = graphQLFields;
		}

		@Override
		public String toString() {
			StringBuilder sb = new StringBuilder(_key);

			if (!_parameterMap.isEmpty()) {
				sb.append("(");

				for (Map.Entry<String, Object> entry :
						_parameterMap.entrySet()) {

					sb.append(entry.getKey());
					sb.append(": ");
					sb.append(entry.getValue());
					sb.append(", ");
				}

				sb.setLength(sb.length() - 2);

				sb.append(")");
			}

			if (!_graphQLFields.isEmpty()) {
				sb.append("{");

				for (GraphQLField graphQLField : _graphQLFields) {
					sb.append(graphQLField.toString());
					sb.append(", ");
				}

				sb.setLength(sb.length() - 2);

				sb.append("}");
			}

			return sb.toString();
		}

		private final List<GraphQLField> _graphQLFields;
		private final String _key;
		private final Map<String, Object> _parameterMap;

	}

	private static final com.liferay.portal.kernel.log.Log _log =
		LogFactoryUtil.getLog(BaseUpgradeRunResourceTestCase.class);

	private static Format _format;

	private com.liferay.portal.kernel.model.User _testCompanyAdminUser;

	@Inject
	private
		com.liferay.upgrades.lab.agent.remote.rest.resource.v1_0.
			UpgradeRunResource _upgradeRunResource;

}
// LIFERAY-REST-BUILDER-HASH:1524647209