/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.bulk.selection.internal;

import com.liferay.bulk.selection.BulkSelection;
import com.liferay.bulk.selection.BulkSelectionAction;
import com.liferay.bulk.selection.BulkSelectionFactory;
import com.liferay.portal.background.task.service.BackgroundTaskLocalService;
import com.liferay.portal.kernel.audit.AuditRequestThreadLocal;
import com.liferay.portal.kernel.backgroundtask.BackgroundTask;
import com.liferay.portal.kernel.model.User;
import com.liferay.portal.kernel.service.ServiceContext;
import com.liferay.portal.kernel.service.UserLocalService;
import com.liferay.portal.kernel.test.ReflectionTestUtil;
import com.liferay.portal.kernel.test.util.RandomTestUtil;
import com.liferay.portal.kernel.util.HashMapBuilder;
import com.liferay.portal.test.rule.LiferayUnitTestRule;

import java.io.Serializable;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.After;
import org.junit.Assert;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;

import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;

/**
 * @author Jürgen Kappler
 */
public class BulkSelectionBackgroundTaskExecutorTest {

	@ClassRule
	@Rule
	public static final LiferayUnitTestRule liferayUnitTestRule =
		LiferayUnitTestRule.INSTANCE;

	@After
	public void tearDown() {
		AuditRequestThreadLocal.removeAuditThreadLocal();
	}

	@Test
	public void testExecute() throws Exception {
		User user = _mockUser();

		AuditRequestThreadLocal auditRequestThreadLocal =
			AuditRequestThreadLocal.getAuditThreadLocal();

		auditRequestThreadLocal.setClientHost(RandomTestUtil.randomString());
		auditRequestThreadLocal.setClientIP(RandomTestUtil.randomString());
		auditRequestThreadLocal.setRealUserEmailAddress(
			RandomTestUtil.randomString());
		auditRequestThreadLocal.setRealUserId(user.getUserId());
		auditRequestThreadLocal.setServerName(RandomTestUtil.randomString());
		auditRequestThreadLocal.setServerPort(RandomTestUtil.randomInt());
		auditRequestThreadLocal.setSessionID(RandomTestUtil.randomString());

		Map<String, Object> auditRequestMap = _getAuditRequestMap();

		AtomicReference<Map<String, Object>> atomicReference =
			new AtomicReference<>();

		BulkSelectionAction<Object> bulkSelectionAction =
			_mockBulkSelectionAction(atomicReference);

		BulkSelectionFactory<Object> bulkSelectionFactory =
			_mockBulkSelectionFactory();

		Map<String, Serializable> taskContextMap = _run(
			bulkSelectionAction, bulkSelectionFactory, user);

		AuditRequestThreadLocal.removeAuditThreadLocal();

		BulkSelectionBackgroundTaskExecutor
			bulkSelectionBackgroundTaskExecutor =
				new BulkSelectionBackgroundTaskExecutor();

		bulkSelectionBackgroundTaskExecutor.activate(
			_mockBundleContext(bulkSelectionAction, bulkSelectionFactory));

		ReflectionTestUtil.setFieldValue(
			bulkSelectionBackgroundTaskExecutor, "_userLocalService",
			_mockUserLocalService(user));

		bulkSelectionBackgroundTaskExecutor.execute(
			_mockBackgroundTask(taskContextMap, user.getUserId()));

		Assert.assertEquals(auditRequestMap, atomicReference.get());

		auditRequestThreadLocal = AuditRequestThreadLocal.getAuditThreadLocal();

		Assert.assertEquals(0, auditRequestThreadLocal.getRealUserId());
	}

	private Map<String, Object> _getAuditRequestMap() {
		AuditRequestThreadLocal auditRequestThreadLocal =
			AuditRequestThreadLocal.getAuditThreadLocal();

		return HashMapBuilder.<String, Object>put(
			"clientHost", auditRequestThreadLocal.getClientHost()
		).put(
			"clientIP", auditRequestThreadLocal.getClientIP()
		).put(
			"realUserEmailAddress",
			auditRequestThreadLocal.getRealUserEmailAddress()
		).put(
			"realUserId", auditRequestThreadLocal.getRealUserId()
		).put(
			"serverName", auditRequestThreadLocal.getServerName()
		).put(
			"serverPort", auditRequestThreadLocal.getServerPort()
		).put(
			"sessionID", auditRequestThreadLocal.getSessionID()
		).build();
	}

	private BackgroundTask _mockBackgroundTask(
		Map<String, Serializable> taskContextMap, long userId) {

		BackgroundTask backgroundTask = Mockito.mock(BackgroundTask.class);

		Mockito.when(
			backgroundTask.getTaskContextMap()
		).thenReturn(
			taskContextMap
		);

		Mockito.when(
			backgroundTask.getUserId()
		).thenReturn(
			userId
		);

		return backgroundTask;
	}

	private BulkSelectionAction<Object> _mockBulkSelectionAction(
			AtomicReference<Map<String, Object>> atomicReference)
		throws Exception {

		BulkSelectionAction<Object> bulkSelectionAction = Mockito.mock(
			BulkSelectionAction.class);

		Mockito.doAnswer(
			invocation -> {
				atomicReference.set(_getAuditRequestMap());

				return null;
			}
		).when(
			bulkSelectionAction
		).execute(
			Mockito.any(), Mockito.any(), Mockito.any()
		);

		return bulkSelectionAction;
	}

	private BulkSelectionFactory<Object> _mockBulkSelectionFactory() {
		BulkSelectionFactory<Object> bulkSelectionFactory = Mockito.mock(
			BulkSelectionFactory.class);

		Mockito.when(
			bulkSelectionFactory.create(Mockito.any())
		).thenReturn(
			Mockito.mock(BulkSelection.class)
		);

		return bulkSelectionFactory;
	}

	private BundleContext _mockBundleContext(
			BulkSelectionAction<Object> bulkSelectionAction,
			BulkSelectionFactory<Object> bulkSelectionFactory)
		throws Exception {

		BundleContext bundleContext = Mockito.mock(BundleContext.class);

		ServiceReference<BulkSelectionAction<Object>>
			bulkSelectionActionServiceReference = Mockito.mock(
				ServiceReference.class);

		Mockito.doReturn(
			Collections.singletonList(bulkSelectionActionServiceReference)
		).when(
			bundleContext
		).getServiceReferences(
			Mockito.eq(BulkSelectionAction.class), Mockito.anyString()
		);

		Mockito.when(
			bundleContext.getService(bulkSelectionActionServiceReference)
		).thenReturn(
			bulkSelectionAction
		);

		ServiceReference<BulkSelectionFactory<Object>>
			bulkSelectionFactoryServiceReference = Mockito.mock(
				ServiceReference.class);

		Mockito.doReturn(
			Collections.singletonList(bulkSelectionFactoryServiceReference)
		).when(
			bundleContext
		).getServiceReferences(
			Mockito.eq(BulkSelectionFactory.class), Mockito.anyString()
		);

		Mockito.when(
			bundleContext.getService(bulkSelectionFactoryServiceReference)
		).thenReturn(
			bulkSelectionFactory
		);

		return bundleContext;
	}

	private User _mockUser() {
		User user = Mockito.mock(User.class);

		Mockito.when(
			user.getUserId()
		).thenReturn(
			RandomTestUtil.randomLong()
		);

		return user;
	}

	private UserLocalService _mockUserLocalService(User user) throws Exception {
		UserLocalService userLocalService = Mockito.mock(
			UserLocalService.class);

		Mockito.when(
			userLocalService.getUser(user.getUserId())
		).thenReturn(
			user
		);

		return userLocalService;
	}

	private Map<String, Serializable> _run(
			BulkSelectionAction<Object> bulkSelectionAction,
			BulkSelectionFactory<Object> bulkSelectionFactory, User user)
		throws Exception {

		BackgroundTaskLocalService backgroundTaskLocalService = Mockito.mock(
			BackgroundTaskLocalService.class);

		BulkSelectionSelectionRunnerImpl bulkSelectionSelectionRunnerImpl =
			new BulkSelectionSelectionRunnerImpl();

		ReflectionTestUtil.setFieldValue(
			bulkSelectionSelectionRunnerImpl, "_backgroundTaskLocalService",
			backgroundTaskLocalService);

		BulkSelection<Object> bulkSelection = Mockito.mock(BulkSelection.class);

		Mockito.when(
			bulkSelection.getBulkSelectionFactoryClass()
		).thenReturn(
			(Class)bulkSelectionFactory.getClass()
		);

		Mockito.when(
			bulkSelection.getParameterMap()
		).thenReturn(
			new HashMap<>()
		);

		bulkSelectionSelectionRunnerImpl.run(
			user, bulkSelection, bulkSelectionAction, new HashMap<>());

		ArgumentCaptor<Map<String, Serializable>> argumentCaptor =
			ArgumentCaptor.forClass(Map.class);

		Mockito.verify(
			backgroundTaskLocalService
		).addBackgroundTask(
			Mockito.anyLong(), Mockito.anyLong(), Mockito.anyString(),
			Mockito.anyString(), argumentCaptor.capture(),
			Mockito.any(ServiceContext.class)
		);

		return argumentCaptor.getValue();
	}

}