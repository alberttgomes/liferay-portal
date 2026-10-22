/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.bulk.selection.internal;

import com.liferay.bulk.selection.BulkSelection;
import com.liferay.bulk.selection.BulkSelectionAction;
import com.liferay.bulk.selection.BulkSelectionFactory;
import com.liferay.bulk.selection.BulkSelectionInputParameters;
import com.liferay.bulk.selection.internal.constants.BulkSelectionBackgroundTaskConstants;
import com.liferay.portal.kernel.audit.AuditRequestThreadLocal;
import com.liferay.portal.kernel.backgroundtask.BackgroundTask;
import com.liferay.portal.kernel.backgroundtask.BackgroundTaskExecutor;
import com.liferay.portal.kernel.backgroundtask.BackgroundTaskResult;
import com.liferay.portal.kernel.backgroundtask.BaseBackgroundTaskExecutor;
import com.liferay.portal.kernel.backgroundtask.constants.BackgroundTaskConstants;
import com.liferay.portal.kernel.backgroundtask.display.BackgroundTaskDisplay;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.service.UserLocalService;
import com.liferay.portal.kernel.util.GetterUtil;
import com.liferay.portal.kernel.util.HashMapBuilder;

import java.io.Serializable;

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

/**
 * @author Adolfo Pérez
 */
@Component(
	property = "background.task.executor.class.name=com.liferay.bulk.selection.internal.BulkSelectionBackgroundTaskExecutor",
	service = BackgroundTaskExecutor.class
)
public class BulkSelectionBackgroundTaskExecutor
	extends BaseBackgroundTaskExecutor {

	public BulkSelectionBackgroundTaskExecutor() {
		setIsolationLevel(BackgroundTaskConstants.ISOLATION_LEVEL_TASK_NAME);
	}

	@Override
	public BackgroundTaskExecutor clone() {
		return this;
	}

	@Override
	public BackgroundTaskResult execute(BackgroundTask backgroundTask) {
		Map<String, Serializable> taskContextMap =
			backgroundTask.getTaskContextMap();

		String bulkSelectionActionClassName = (String)taskContextMap.get(
			BulkSelectionBackgroundTaskConstants.
				BULK_SELECTION_ACTION_CLASS_NAME);

		BulkSelectionAction<Object> bulkSelectionAction = _getService(
			(Class<BulkSelectionAction<Object>>)
				(Class<?>)BulkSelectionAction.class,
			bulkSelectionActionClassName);

		if (bulkSelectionAction == null) {
			return BackgroundTaskResult.SUCCESS;
		}

		String bulkSelectionFactoryClassName = (String)taskContextMap.get(
			BulkSelectionBackgroundTaskConstants.
				BULK_SELECTION_FACTORY_CLASS_NAME);

		BulkSelectionFactory<?> bulkSelectionFactory = _getService(
			(Class<BulkSelectionFactory<?>>)
				(Class<?>)BulkSelectionFactory.class,
			bulkSelectionFactoryClassName);

		if (bulkSelectionFactory == null) {
			return BackgroundTaskResult.SUCCESS;
		}

		try {
			Map<String, Serializable> inputMap =
				(Map<String, Serializable>)taskContextMap.get(
					BulkSelectionBackgroundTaskConstants.
						BULK_SELECTION_ACTION_INPUT_MAP);

			boolean assetEntryBulkSelection = (boolean)inputMap.getOrDefault(
				BulkSelectionInputParameters.ASSET_ENTRY_BULK_SELECTION, false);

			Map<String, String[]> parameterMap =
				(Map<String, String[]>)taskContextMap.get(
					BulkSelectionBackgroundTaskConstants.
						BULK_SELECTION_PARAMETER_MAP);

			BulkSelection<?> bulkSelection = bulkSelectionFactory.create(
				parameterMap);

			if (assetEntryBulkSelection) {
				bulkSelection = bulkSelection.toAssetEntryBulkSelection();
			}

			Map<String, Serializable> auditRequestMap = _getAuditRequestMap();

			_setAuditRequest(
				(Map<String, Serializable>)taskContextMap.get(
					BulkSelectionBackgroundTaskConstants.
						BULK_SELECTION_AUDIT_REQUEST_CONTEXT_MAP));

			try {
				bulkSelectionAction.execute(
					_userLocalService.getUser(backgroundTask.getUserId()),
					(BulkSelection<Object>)bulkSelection, inputMap);
			}
			finally {
				_setAuditRequest(auditRequestMap);
			}
		}
		catch (Exception exception) {
			_log.error(exception);
		}

		return BackgroundTaskResult.SUCCESS;
	}

	@Override
	public BackgroundTaskDisplay getBackgroundTaskDisplay(
		BackgroundTask backgroundTask) {

		return null;
	}

	@Activate
	protected void activate(BundleContext bundleContext) {
		_bundleContext = bundleContext;
	}

	private Map<String, Serializable> _getAuditRequestMap() {
		AuditRequestThreadLocal auditRequestThreadLocal =
			AuditRequestThreadLocal.getAuditThreadLocal();

		return HashMapBuilder.<String, Serializable>put(
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

	private <T> T _getService(Class<T> clazz, String concreteClassName) {
		try {
			Collection<ServiceReference<T>> serviceReferences =
				_bundleContext.getServiceReferences(
					clazz, "(component.name=" + concreteClassName + ")");

			Iterator<ServiceReference<T>> iterator =
				serviceReferences.iterator();

			if (iterator.hasNext()) {
				return _bundleContext.getService(iterator.next());
			}

			return null;
		}
		catch (InvalidSyntaxException invalidSyntaxException) {
			if (_log.isDebugEnabled()) {
				_log.debug(invalidSyntaxException);
			}

			return null;
		}
	}

	private void _setAuditRequest(Map<String, Serializable> auditRequestMap) {
		AuditRequestThreadLocal auditRequestThreadLocal =
			AuditRequestThreadLocal.getAuditThreadLocal();

		if (auditRequestMap == null) {
			auditRequestMap = new HashMap<>();
		}

		auditRequestThreadLocal.setClientHost(
			(String)auditRequestMap.get("clientHost"));
		auditRequestThreadLocal.setClientIP(
			(String)auditRequestMap.get("clientIP"));
		auditRequestThreadLocal.setRealUserEmailAddress(
			(String)auditRequestMap.get("realUserEmailAddress"));
		auditRequestThreadLocal.setRealUserId(
			GetterUtil.getLong(auditRequestMap.get("realUserId")));
		auditRequestThreadLocal.setServerName(
			(String)auditRequestMap.get("serverName"));
		auditRequestThreadLocal.setServerPort(
			GetterUtil.getInteger(auditRequestMap.get("serverPort")));
		auditRequestThreadLocal.setSessionID(
			(String)auditRequestMap.get("sessionID"));
	}

	private static final Log _log = LogFactoryUtil.getLog(
		BulkSelectionBackgroundTaskExecutor.class);

	private BundleContext _bundleContext;

	@Reference
	private UserLocalService _userLocalService;

}