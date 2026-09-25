/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.upgrades.lab.agent.remote.service;

import com.liferay.petra.sql.dsl.query.DSLQuery;
import com.liferay.portal.kernel.dao.orm.DynamicQuery;
import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.model.PersistedModel;
import com.liferay.portal.kernel.module.service.Snapshot;
import com.liferay.portal.kernel.util.OrderByComparator;
import com.liferay.upgrades.lab.agent.remote.model.UpgradeRun;

import java.io.Serializable;

import java.util.List;

/**
 * Provides the local service utility for UpgradeRun. This utility wraps
 * <code>com.liferay.upgrades.lab.agent.remote.service.impl.UpgradeRunLocalServiceImpl</code> and
 * is an access point for service operations in application layer code running
 * on the local server. Methods of this service will not have security checks
 * based on the propagated JAAS credentials because this service can only be
 * accessed from within the same VM.
 *
 * @author Albert Gomes Cabral
 * @see UpgradeRunLocalService
 * @generated
 */
public class UpgradeRunLocalServiceUtil {

	/*
	 * NOTE FOR DEVELOPERS:
	 *
	 * Never modify this class directly. Add custom service methods to <code>com.liferay.upgrades.lab.agent.remote.service.impl.UpgradeRunLocalServiceImpl</code> and rerun ServiceBuilder to regenerate this class.
	 */
	public static UpgradeRun addUpgradeRun(
			String branch, String credentialKeyReference, String customerName,
			String dbTargetType, String dbTargetVersion, String nodeVersion,
			String repositoryURL, String searchVersion, String targetRelease,
			String upgradeSourceVersion, String upgradeTargetJavaVersion,
			long userId)
		throws PortalException {

		return getService().addUpgradeRun(
			branch, credentialKeyReference, customerName, dbTargetType,
			dbTargetVersion, nodeVersion, repositoryURL, searchVersion,
			targetRelease, upgradeSourceVersion, upgradeTargetJavaVersion,
			userId);
	}

	/**
	 * Adds the upgrade run to the database. Also notifies the appropriate model listeners.
	 *
	 * <p>
	 * <strong>Important:</strong> Inspect UpgradeRunLocalServiceImpl for overloaded versions of the method. If provided, use these entry points to the API, as the implementation logic may require the additional parameters defined there.
	 * </p>
	 *
	 * @param upgradeRun the upgrade run
	 * @return the upgrade run that was added
	 */
	public static UpgradeRun addUpgradeRun(UpgradeRun upgradeRun) {
		return getService().addUpgradeRun(upgradeRun);
	}

	public static UpgradeRun cancelUpgradeRun(
			long companyId, String externalReferenceCode)
		throws PortalException {

		return getService().cancelUpgradeRun(companyId, externalReferenceCode);
	}

	/**
	 * @throws PortalException
	 */
	public static PersistedModel createPersistedModel(
			Serializable primaryKeyObj)
		throws PortalException {

		return getService().createPersistedModel(primaryKeyObj);
	}

	/**
	 * Creates a new upgrade run with the primary key. Does not add the upgrade run to the database.
	 *
	 * @param upgradeRunId the primary key for the new upgrade run
	 * @return the new upgrade run
	 */
	public static UpgradeRun createUpgradeRun(long upgradeRunId) {
		return getService().createUpgradeRun(upgradeRunId);
	}

	/**
	 * @throws PortalException
	 */
	public static PersistedModel deletePersistedModel(
			PersistedModel persistedModel)
		throws PortalException {

		return getService().deletePersistedModel(persistedModel);
	}

	/**
	 * Deletes the upgrade run with the primary key from the database. Also notifies the appropriate model listeners.
	 *
	 * <p>
	 * <strong>Important:</strong> Inspect UpgradeRunLocalServiceImpl for overloaded versions of the method. If provided, use these entry points to the API, as the implementation logic may require the additional parameters defined there.
	 * </p>
	 *
	 * @param upgradeRunId the primary key of the upgrade run
	 * @return the upgrade run that was removed
	 * @throws PortalException if a upgrade run with the primary key could not be found
	 */
	public static UpgradeRun deleteUpgradeRun(long upgradeRunId)
		throws PortalException {

		return getService().deleteUpgradeRun(upgradeRunId);
	}

	/**
	 * Deletes the upgrade run from the database. Also notifies the appropriate model listeners.
	 *
	 * <p>
	 * <strong>Important:</strong> Inspect UpgradeRunLocalServiceImpl for overloaded versions of the method. If provided, use these entry points to the API, as the implementation logic may require the additional parameters defined there.
	 * </p>
	 *
	 * @param upgradeRun the upgrade run
	 * @return the upgrade run that was removed
	 */
	public static UpgradeRun deleteUpgradeRun(UpgradeRun upgradeRun) {
		return getService().deleteUpgradeRun(upgradeRun);
	}

	public static <T> T dslQuery(DSLQuery dslQuery) {
		return getService().dslQuery(dslQuery);
	}

	public static int dslQueryCount(DSLQuery dslQuery) {
		return getService().dslQueryCount(dslQuery);
	}

	public static DynamicQuery dynamicQuery() {
		return getService().dynamicQuery();
	}

	/**
	 * Performs a dynamic query on the database and returns the matching rows.
	 *
	 * @param dynamicQuery the dynamic query
	 * @return the matching rows
	 */
	public static <T> List<T> dynamicQuery(DynamicQuery dynamicQuery) {
		return getService().dynamicQuery(dynamicQuery);
	}

	/**
	 * Performs a dynamic query on the database and returns a range of the matching rows.
	 *
	 * <p>
	 * Useful when paginating results. Returns a maximum of <code>end - start</code> instances. <code>start</code> and <code>end</code> are not primary keys, they are indexes in the result set. Thus, <code>0</code> refers to the first result in the set. Setting both <code>start</code> and <code>end</code> to <code>com.liferay.portal.kernel.dao.orm.QueryUtil#ALL_POS</code> will return the full result set. If <code>orderByComparator</code> is specified, then the query will include the given ORDER BY logic. If <code>orderByComparator</code> is absent, then the query will include the default ORDER BY logic from <code>com.liferay.upgrades.lab.agent.remote.model.impl.UpgradeRunModelImpl</code>.
	 * </p>
	 *
	 * @param dynamicQuery the dynamic query
	 * @param start the lower bound of the range of model instances
	 * @param end the upper bound of the range of model instances (not inclusive)
	 * @return the range of matching rows
	 */
	public static <T> List<T> dynamicQuery(
		DynamicQuery dynamicQuery, int start, int end) {

		return getService().dynamicQuery(dynamicQuery, start, end);
	}

	/**
	 * Performs a dynamic query on the database and returns an ordered range of the matching rows.
	 *
	 * <p>
	 * Useful when paginating results. Returns a maximum of <code>end - start</code> instances. <code>start</code> and <code>end</code> are not primary keys, they are indexes in the result set. Thus, <code>0</code> refers to the first result in the set. Setting both <code>start</code> and <code>end</code> to <code>com.liferay.portal.kernel.dao.orm.QueryUtil#ALL_POS</code> will return the full result set. If <code>orderByComparator</code> is specified, then the query will include the given ORDER BY logic. If <code>orderByComparator</code> is absent, then the query will include the default ORDER BY logic from <code>com.liferay.upgrades.lab.agent.remote.model.impl.UpgradeRunModelImpl</code>.
	 * </p>
	 *
	 * @param dynamicQuery the dynamic query
	 * @param start the lower bound of the range of model instances
	 * @param end the upper bound of the range of model instances (not inclusive)
	 * @param orderByComparator the comparator to order the results by (optionally <code>null</code>)
	 * @return the ordered range of matching rows
	 */
	public static <T> List<T> dynamicQuery(
		DynamicQuery dynamicQuery, int start, int end,
		OrderByComparator<T> orderByComparator) {

		return getService().dynamicQuery(
			dynamicQuery, start, end, orderByComparator);
	}

	/**
	 * Returns the number of rows matching the dynamic query.
	 *
	 * @param dynamicQuery the dynamic query
	 * @return the number of rows matching the dynamic query
	 */
	public static long dynamicQueryCount(DynamicQuery dynamicQuery) {
		return getService().dynamicQueryCount(dynamicQuery);
	}

	/**
	 * Returns the number of rows matching the dynamic query.
	 *
	 * @param dynamicQuery the dynamic query
	 * @param projection the projection to apply to the query
	 * @return the number of rows matching the dynamic query
	 */
	public static long dynamicQueryCount(
		DynamicQuery dynamicQuery,
		com.liferay.portal.kernel.dao.orm.Projection projection) {

		return getService().dynamicQueryCount(dynamicQuery, projection);
	}

	public static UpgradeRun fetchUpgradeRun(long upgradeRunId) {
		return getService().fetchUpgradeRun(upgradeRunId);
	}

	public static UpgradeRun fetchUpgradeRunByExternalReferenceCode(
		String externalReferenceCode, long companyId) {

		return getService().fetchUpgradeRunByExternalReferenceCode(
			externalReferenceCode, companyId);
	}

	/**
	 * Returns the upgrade run with the matching UUID and company.
	 *
	 * @param uuid the upgrade run's UUID
	 * @param companyId the primary key of the company
	 * @return the matching upgrade run, or <code>null</code> if a matching upgrade run could not be found
	 */
	public static UpgradeRun fetchUpgradeRunByUuidAndCompanyId(
		String uuid, long companyId) {

		return getService().fetchUpgradeRunByUuidAndCompanyId(uuid, companyId);
	}

	public static com.liferay.portal.kernel.dao.orm.ActionableDynamicQuery
		getActionableDynamicQuery() {

		return getService().getActionableDynamicQuery();
	}

	public static com.liferay.portal.kernel.dao.orm.ExportActionableDynamicQuery
		getExportActionableDynamicQuery(
			com.liferay.exportimport.kernel.lar.PortletDataContext
				portletDataContext) {

		return getService().getExportActionableDynamicQuery(portletDataContext);
	}

	public static
		com.liferay.portal.kernel.dao.orm.IndexableActionableDynamicQuery
			getIndexableActionableDynamicQuery() {

		return getService().getIndexableActionableDynamicQuery();
	}

	/**
	 * Returns the OSGi service identifier.
	 *
	 * @return the OSGi service identifier
	 */
	public static String getOSGiServiceIdentifier() {
		return getService().getOSGiServiceIdentifier();
	}

	/**
	 * @throws PortalException
	 */
	public static PersistedModel getPersistedModel(Serializable primaryKeyObj)
		throws PortalException {

		return getService().getPersistedModel(primaryKeyObj);
	}

	/**
	 * Returns the upgrade run with the primary key.
	 *
	 * @param upgradeRunId the primary key of the upgrade run
	 * @return the upgrade run
	 * @throws PortalException if a upgrade run with the primary key could not be found
	 */
	public static UpgradeRun getUpgradeRun(long upgradeRunId)
		throws PortalException {

		return getService().getUpgradeRun(upgradeRunId);
	}

	public static UpgradeRun getUpgradeRunByExternalReferenceCode(
			String externalReferenceCode, long companyId)
		throws PortalException {

		return getService().getUpgradeRunByExternalReferenceCode(
			externalReferenceCode, companyId);
	}

	/**
	 * Returns the upgrade run with the matching UUID and company.
	 *
	 * @param uuid the upgrade run's UUID
	 * @param companyId the primary key of the company
	 * @return the matching upgrade run
	 * @throws PortalException if a matching upgrade run could not be found
	 */
	public static UpgradeRun getUpgradeRunByUuidAndCompanyId(
			String uuid, long companyId)
		throws PortalException {

		return getService().getUpgradeRunByUuidAndCompanyId(uuid, companyId);
	}

	/**
	 * Returns a range of all the upgrade runs.
	 *
	 * <p>
	 * Useful when paginating results. Returns a maximum of <code>end - start</code> instances. <code>start</code> and <code>end</code> are not primary keys, they are indexes in the result set. Thus, <code>0</code> refers to the first result in the set. Setting both <code>start</code> and <code>end</code> to <code>com.liferay.portal.kernel.dao.orm.QueryUtil#ALL_POS</code> will return the full result set. If <code>orderByComparator</code> is specified, then the query will include the given ORDER BY logic. If <code>orderByComparator</code> is absent, then the query will include the default ORDER BY logic from <code>com.liferay.upgrades.lab.agent.remote.model.impl.UpgradeRunModelImpl</code>.
	 * </p>
	 *
	 * @param start the lower bound of the range of upgrade runs
	 * @param end the upper bound of the range of upgrade runs (not inclusive)
	 * @return the range of upgrade runs
	 */
	public static List<UpgradeRun> getUpgradeRuns(int start, int end) {
		return getService().getUpgradeRuns(start, end);
	}

	/**
	 * Returns the number of upgrade runs.
	 *
	 * @return the number of upgrade runs
	 */
	public static int getUpgradeRunsCount() {
		return getService().getUpgradeRunsCount();
	}

	public static UpgradeRun refreshUpgradeRun(
			long companyId, String externalReferenceCode)
		throws PortalException {

		return getService().refreshUpgradeRun(companyId, externalReferenceCode);
	}

	/**
	 * Updates the upgrade run in the database or adds it if it does not yet exist. Also notifies the appropriate model listeners.
	 *
	 * <p>
	 * <strong>Important:</strong> Inspect UpgradeRunLocalServiceImpl for overloaded versions of the method. If provided, use these entry points to the API, as the implementation logic may require the additional parameters defined there.
	 * </p>
	 *
	 * @param upgradeRun the upgrade run
	 * @return the upgrade run that was updated
	 */
	public static UpgradeRun updateUpgradeRun(UpgradeRun upgradeRun) {
		return getService().updateUpgradeRun(upgradeRun);
	}

	public static UpgradeRunLocalService getService() {
		return _serviceSnapshot.get();
	}

	private static final Snapshot<UpgradeRunLocalService> _serviceSnapshot =
		new Snapshot<>(
			UpgradeRunLocalServiceUtil.class, UpgradeRunLocalService.class);

}
// LIFERAY-SERVICE-BUILDER-HASH:292190701