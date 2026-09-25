/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.upgrades.lab.agent.remote.service;

import com.liferay.portal.kernel.service.ServiceWrapper;
import com.liferay.portal.kernel.service.persistence.BasePersistence;

/**
 * Provides a wrapper for {@link UpgradeRunLocalService}.
 *
 * @author Albert Gomes Cabral
 * @see UpgradeRunLocalService
 * @generated
 */
public class UpgradeRunLocalServiceWrapper
	implements ServiceWrapper<UpgradeRunLocalService>, UpgradeRunLocalService {

	public UpgradeRunLocalServiceWrapper() {
		this(null);
	}

	public UpgradeRunLocalServiceWrapper(
		UpgradeRunLocalService upgradeRunLocalService) {

		_upgradeRunLocalService = upgradeRunLocalService;
	}

	@Override
	public com.liferay.upgrades.lab.agent.remote.model.UpgradeRun addUpgradeRun(
			String branch, String credentialKeyReference, String customerName,
			String dbTargetType, String dbTargetVersion, String nodeVersion,
			String repositoryURL, String searchVersion, String targetRelease,
			String upgradeSourceVersion, String upgradeTargetJavaVersion,
			long userId)
		throws com.liferay.portal.kernel.exception.PortalException {

		return _upgradeRunLocalService.addUpgradeRun(
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
	@Override
	public com.liferay.upgrades.lab.agent.remote.model.UpgradeRun addUpgradeRun(
		com.liferay.upgrades.lab.agent.remote.model.UpgradeRun upgradeRun) {

		return _upgradeRunLocalService.addUpgradeRun(upgradeRun);
	}

	@Override
	public com.liferay.upgrades.lab.agent.remote.model.UpgradeRun
			cancelUpgradeRun(long companyId, String externalReferenceCode)
		throws com.liferay.portal.kernel.exception.PortalException {

		return _upgradeRunLocalService.cancelUpgradeRun(
			companyId, externalReferenceCode);
	}

	/**
	 * @throws PortalException
	 */
	@Override
	public com.liferay.portal.kernel.model.PersistedModel createPersistedModel(
			java.io.Serializable primaryKeyObj)
		throws com.liferay.portal.kernel.exception.PortalException {

		return _upgradeRunLocalService.createPersistedModel(primaryKeyObj);
	}

	/**
	 * Creates a new upgrade run with the primary key. Does not add the upgrade run to the database.
	 *
	 * @param upgradeRunId the primary key for the new upgrade run
	 * @return the new upgrade run
	 */
	@Override
	public com.liferay.upgrades.lab.agent.remote.model.UpgradeRun
		createUpgradeRun(long upgradeRunId) {

		return _upgradeRunLocalService.createUpgradeRun(upgradeRunId);
	}

	/**
	 * @throws PortalException
	 */
	@Override
	public com.liferay.portal.kernel.model.PersistedModel deletePersistedModel(
			com.liferay.portal.kernel.model.PersistedModel persistedModel)
		throws com.liferay.portal.kernel.exception.PortalException {

		return _upgradeRunLocalService.deletePersistedModel(persistedModel);
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
	@Override
	public com.liferay.upgrades.lab.agent.remote.model.UpgradeRun
			deleteUpgradeRun(long upgradeRunId)
		throws com.liferay.portal.kernel.exception.PortalException {

		return _upgradeRunLocalService.deleteUpgradeRun(upgradeRunId);
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
	@Override
	public com.liferay.upgrades.lab.agent.remote.model.UpgradeRun
		deleteUpgradeRun(
			com.liferay.upgrades.lab.agent.remote.model.UpgradeRun upgradeRun) {

		return _upgradeRunLocalService.deleteUpgradeRun(upgradeRun);
	}

	@Override
	public <T> T dslQuery(com.liferay.petra.sql.dsl.query.DSLQuery dslQuery) {
		return _upgradeRunLocalService.dslQuery(dslQuery);
	}

	@Override
	public int dslQueryCount(
		com.liferay.petra.sql.dsl.query.DSLQuery dslQuery) {

		return _upgradeRunLocalService.dslQueryCount(dslQuery);
	}

	@Override
	public com.liferay.portal.kernel.dao.orm.DynamicQuery dynamicQuery() {
		return _upgradeRunLocalService.dynamicQuery();
	}

	/**
	 * Performs a dynamic query on the database and returns the matching rows.
	 *
	 * @param dynamicQuery the dynamic query
	 * @return the matching rows
	 */
	@Override
	public <T> java.util.List<T> dynamicQuery(
		com.liferay.portal.kernel.dao.orm.DynamicQuery dynamicQuery) {

		return _upgradeRunLocalService.dynamicQuery(dynamicQuery);
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
	@Override
	public <T> java.util.List<T> dynamicQuery(
		com.liferay.portal.kernel.dao.orm.DynamicQuery dynamicQuery, int start,
		int end) {

		return _upgradeRunLocalService.dynamicQuery(dynamicQuery, start, end);
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
	@Override
	public <T> java.util.List<T> dynamicQuery(
		com.liferay.portal.kernel.dao.orm.DynamicQuery dynamicQuery, int start,
		int end,
		com.liferay.portal.kernel.util.OrderByComparator<T> orderByComparator) {

		return _upgradeRunLocalService.dynamicQuery(
			dynamicQuery, start, end, orderByComparator);
	}

	/**
	 * Returns the number of rows matching the dynamic query.
	 *
	 * @param dynamicQuery the dynamic query
	 * @return the number of rows matching the dynamic query
	 */
	@Override
	public long dynamicQueryCount(
		com.liferay.portal.kernel.dao.orm.DynamicQuery dynamicQuery) {

		return _upgradeRunLocalService.dynamicQueryCount(dynamicQuery);
	}

	/**
	 * Returns the number of rows matching the dynamic query.
	 *
	 * @param dynamicQuery the dynamic query
	 * @param projection the projection to apply to the query
	 * @return the number of rows matching the dynamic query
	 */
	@Override
	public long dynamicQueryCount(
		com.liferay.portal.kernel.dao.orm.DynamicQuery dynamicQuery,
		com.liferay.portal.kernel.dao.orm.Projection projection) {

		return _upgradeRunLocalService.dynamicQueryCount(
			dynamicQuery, projection);
	}

	@Override
	public com.liferay.upgrades.lab.agent.remote.model.UpgradeRun
		fetchUpgradeRun(long upgradeRunId) {

		return _upgradeRunLocalService.fetchUpgradeRun(upgradeRunId);
	}

	@Override
	public com.liferay.upgrades.lab.agent.remote.model.UpgradeRun
		fetchUpgradeRunByExternalReferenceCode(
			String externalReferenceCode, long companyId) {

		return _upgradeRunLocalService.fetchUpgradeRunByExternalReferenceCode(
			externalReferenceCode, companyId);
	}

	/**
	 * Returns the upgrade run with the matching UUID and company.
	 *
	 * @param uuid the upgrade run's UUID
	 * @param companyId the primary key of the company
	 * @return the matching upgrade run, or <code>null</code> if a matching upgrade run could not be found
	 */
	@Override
	public com.liferay.upgrades.lab.agent.remote.model.UpgradeRun
		fetchUpgradeRunByUuidAndCompanyId(String uuid, long companyId) {

		return _upgradeRunLocalService.fetchUpgradeRunByUuidAndCompanyId(
			uuid, companyId);
	}

	@Override
	public com.liferay.portal.kernel.dao.orm.ActionableDynamicQuery
		getActionableDynamicQuery() {

		return _upgradeRunLocalService.getActionableDynamicQuery();
	}

	@Override
	public com.liferay.portal.kernel.dao.orm.ExportActionableDynamicQuery
		getExportActionableDynamicQuery(
			com.liferay.exportimport.kernel.lar.PortletDataContext
				portletDataContext) {

		return _upgradeRunLocalService.getExportActionableDynamicQuery(
			portletDataContext);
	}

	@Override
	public com.liferay.portal.kernel.dao.orm.IndexableActionableDynamicQuery
		getIndexableActionableDynamicQuery() {

		return _upgradeRunLocalService.getIndexableActionableDynamicQuery();
	}

	/**
	 * Returns the OSGi service identifier.
	 *
	 * @return the OSGi service identifier
	 */
	@Override
	public String getOSGiServiceIdentifier() {
		return _upgradeRunLocalService.getOSGiServiceIdentifier();
	}

	/**
	 * @throws PortalException
	 */
	@Override
	public com.liferay.portal.kernel.model.PersistedModel getPersistedModel(
			java.io.Serializable primaryKeyObj)
		throws com.liferay.portal.kernel.exception.PortalException {

		return _upgradeRunLocalService.getPersistedModel(primaryKeyObj);
	}

	/**
	 * Returns the upgrade run with the primary key.
	 *
	 * @param upgradeRunId the primary key of the upgrade run
	 * @return the upgrade run
	 * @throws PortalException if a upgrade run with the primary key could not be found
	 */
	@Override
	public com.liferay.upgrades.lab.agent.remote.model.UpgradeRun getUpgradeRun(
			long upgradeRunId)
		throws com.liferay.portal.kernel.exception.PortalException {

		return _upgradeRunLocalService.getUpgradeRun(upgradeRunId);
	}

	@Override
	public com.liferay.upgrades.lab.agent.remote.model.UpgradeRun
			getUpgradeRunByExternalReferenceCode(
				String externalReferenceCode, long companyId)
		throws com.liferay.portal.kernel.exception.PortalException {

		return _upgradeRunLocalService.getUpgradeRunByExternalReferenceCode(
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
	@Override
	public com.liferay.upgrades.lab.agent.remote.model.UpgradeRun
			getUpgradeRunByUuidAndCompanyId(String uuid, long companyId)
		throws com.liferay.portal.kernel.exception.PortalException {

		return _upgradeRunLocalService.getUpgradeRunByUuidAndCompanyId(
			uuid, companyId);
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
	@Override
	public java.util.List
		<com.liferay.upgrades.lab.agent.remote.model.UpgradeRun> getUpgradeRuns(
			int start, int end) {

		return _upgradeRunLocalService.getUpgradeRuns(start, end);
	}

	/**
	 * Returns the number of upgrade runs.
	 *
	 * @return the number of upgrade runs
	 */
	@Override
	public int getUpgradeRunsCount() {
		return _upgradeRunLocalService.getUpgradeRunsCount();
	}

	@Override
	public com.liferay.upgrades.lab.agent.remote.model.UpgradeRun
			refreshUpgradeRun(long companyId, String externalReferenceCode)
		throws com.liferay.portal.kernel.exception.PortalException {

		return _upgradeRunLocalService.refreshUpgradeRun(
			companyId, externalReferenceCode);
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
	@Override
	public com.liferay.upgrades.lab.agent.remote.model.UpgradeRun
		updateUpgradeRun(
			com.liferay.upgrades.lab.agent.remote.model.UpgradeRun upgradeRun) {

		return _upgradeRunLocalService.updateUpgradeRun(upgradeRun);
	}

	@Override
	public BasePersistence<?> getBasePersistence() {
		return _upgradeRunLocalService.getBasePersistence();
	}

	@Override
	public UpgradeRunLocalService getWrappedService() {
		return _upgradeRunLocalService;
	}

	@Override
	public void setWrappedService(
		UpgradeRunLocalService upgradeRunLocalService) {

		_upgradeRunLocalService = upgradeRunLocalService;
	}

	private UpgradeRunLocalService _upgradeRunLocalService;

}
// LIFERAY-SERVICE-BUILDER-HASH:-190527365