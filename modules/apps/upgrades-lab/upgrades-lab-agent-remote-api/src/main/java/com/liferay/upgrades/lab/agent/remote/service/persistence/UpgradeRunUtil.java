/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.upgrades.lab.agent.remote.service.persistence;

import com.liferay.portal.kernel.dao.orm.DynamicQuery;
import com.liferay.portal.kernel.service.ServiceContext;
import com.liferay.portal.kernel.util.OrderByComparator;
import com.liferay.upgrades.lab.agent.remote.model.UpgradeRun;

import java.io.Serializable;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * The persistence utility for the upgrade run service. This utility wraps <code>com.liferay.upgrades.lab.agent.remote.service.persistence.impl.UpgradeRunPersistenceImpl</code> and provides direct access to the database for CRUD operations. This utility should only be used by the service layer, as it must operate within a transaction. Never access this utility in a JSP, controller, model, or other front-end class.
 *
 * <p>
 * Caching information and settings can be found in <code>portal.properties</code>
 * </p>
 *
 * @author Albert Gomes Cabral
 * @see UpgradeRunPersistence
 * @generated
 */
public class UpgradeRunUtil {

	/*
	 * NOTE FOR DEVELOPERS:
	 *
	 * Never modify this class directly. Modify <code>service.xml</code> and rerun ServiceBuilder to regenerate this class.
	 */

	/**
	 * @see com.liferay.portal.kernel.service.persistence.BasePersistence#cacheResult(List)
	 */
	public static void cacheResult(List<UpgradeRun> upgradeRuns) {
		getPersistence().cacheResult(upgradeRuns);
	}

	/**
	 * @see com.liferay.portal.kernel.service.persistence.BasePersistence#cacheResult(com.liferay.portal.kernel.model.BaseModel)
	 */
	public static void cacheResult(UpgradeRun upgradeRun) {
		getPersistence().cacheResult(upgradeRun);
	}

	/**
	 * @see com.liferay.portal.kernel.service.persistence.BasePersistence#clearCache()
	 */
	public static void clearCache() {
		getPersistence().clearCache();
	}

	/**
	 * @see com.liferay.portal.kernel.service.persistence.BasePersistence#clearCache(com.liferay.portal.kernel.model.BaseModel)
	 */
	public static void clearCache(UpgradeRun upgradeRun) {
		getPersistence().clearCache(upgradeRun);
	}

	/**
	 * @see com.liferay.portal.kernel.service.persistence.BasePersistence#countWithDynamicQuery(DynamicQuery)
	 */
	public static long countWithDynamicQuery(DynamicQuery dynamicQuery) {
		return getPersistence().countWithDynamicQuery(dynamicQuery);
	}

	/**
	 * @see com.liferay.portal.kernel.service.persistence.BasePersistence#fetchByPrimaryKeys(Set)
	 */
	public static Map<Serializable, UpgradeRun> fetchByPrimaryKeys(
		Set<Serializable> primaryKeys) {

		return getPersistence().fetchByPrimaryKeys(primaryKeys);
	}

	/**
	 * @see com.liferay.portal.kernel.service.persistence.BasePersistence#findWithDynamicQuery(DynamicQuery)
	 */
	public static List<UpgradeRun> findWithDynamicQuery(
		DynamicQuery dynamicQuery) {

		return getPersistence().findWithDynamicQuery(dynamicQuery);
	}

	/**
	 * @see com.liferay.portal.kernel.service.persistence.BasePersistence#findWithDynamicQuery(DynamicQuery, int, int)
	 */
	public static List<UpgradeRun> findWithDynamicQuery(
		DynamicQuery dynamicQuery, int start, int end) {

		return getPersistence().findWithDynamicQuery(dynamicQuery, start, end);
	}

	/**
	 * @see com.liferay.portal.kernel.service.persistence.BasePersistence#findWithDynamicQuery(DynamicQuery, int, int, OrderByComparator)
	 */
	public static List<UpgradeRun> findWithDynamicQuery(
		DynamicQuery dynamicQuery, int start, int end,
		OrderByComparator<UpgradeRun> orderByComparator) {

		return getPersistence().findWithDynamicQuery(
			dynamicQuery, start, end, orderByComparator);
	}

	/**
	 * @see com.liferay.portal.kernel.service.persistence.BasePersistence#update(com.liferay.portal.kernel.model.BaseModel)
	 */
	public static UpgradeRun update(UpgradeRun upgradeRun) {
		return getPersistence().update(upgradeRun);
	}

	/**
	 * @see com.liferay.portal.kernel.service.persistence.BasePersistence#update(com.liferay.portal.kernel.model.BaseModel, ServiceContext)
	 */
	public static UpgradeRun update(
		UpgradeRun upgradeRun, ServiceContext serviceContext) {

		return getPersistence().update(upgradeRun, serviceContext);
	}

	/**
	 * Returns an ordered range of all the upgrade runs where uuid = &#63;.
	 *
	 * <p>
	 * Useful when paginating results. Returns a maximum of <code>end - start</code> instances. <code>start</code> and <code>end</code> are not primary keys, they are indexes in the result set. Thus, <code>0</code> refers to the first result in the set. Setting both <code>start</code> and <code>end</code> to <code>com.liferay.portal.kernel.dao.orm.QueryUtil#ALL_POS</code> will return the full result set. If <code>orderByComparator</code> is specified, then the query will include the given ORDER BY logic. If <code>orderByComparator</code> is absent, then the query will include the default ORDER BY logic from <code>com.liferay.upgrades.lab.agent.remote.model.impl.UpgradeRunModelImpl</code>.
	 * </p>
	 *
	 * @param uuid the uuid
	 * @param start the lower bound of the range of upgrade runs
	 * @param end the upper bound of the range of upgrade runs (not inclusive)
	 * @param orderByComparator the comparator to order the results by (optionally <code>null</code>)
	 * @param useFinderCache whether to use the finder cache
	 * @return the ordered range of matching upgrade runs
	 */
	public static List<UpgradeRun> findByUuid(
		String uuid, int start, int end,
		OrderByComparator<UpgradeRun> orderByComparator,
		boolean useFinderCache) {

		return getPersistence().findByUuid(
			uuid, start, end, orderByComparator, useFinderCache);
	}

	/**
	 * Returns the first upgrade run in the ordered set where uuid = &#63;.
	 *
	 * @param uuid the uuid
	 * @param orderByComparator the comparator to order the set by (optionally <code>null</code>)
	 * @return the first matching upgrade run
	 * @throws NoSuchUpgradeRunException if a matching upgrade run could not be found
	 */
	public static UpgradeRun findByUuid_First(
			String uuid, OrderByComparator<UpgradeRun> orderByComparator)
		throws com.liferay.upgrades.lab.agent.remote.exception.
			NoSuchUpgradeRunException {

		return getPersistence().findByUuid_First(uuid, orderByComparator);
	}

	/**
	 * Returns the first upgrade run in the ordered set where uuid = &#63;.
	 *
	 * @param uuid the uuid
	 * @param orderByComparator the comparator to order the set by (optionally <code>null</code>)
	 * @return the first matching upgrade run, or <code>null</code> if a matching upgrade run could not be found
	 */
	public static UpgradeRun fetchByUuid_First(
		String uuid, OrderByComparator<UpgradeRun> orderByComparator) {

		return getPersistence().fetchByUuid_First(uuid, orderByComparator);
	}

	/**
	 * Removes all the upgrade runs where uuid = &#63; from the database.
	 *
	 * @param uuid the uuid
	 */
	public static void removeByUuid(String uuid) {
		getPersistence().removeByUuid(uuid);
	}

	/**
	 * Returns the number of upgrade runs where uuid = &#63;.
	 *
	 * @param uuid the uuid
	 * @return the number of matching upgrade runs
	 */
	public static int countByUuid(String uuid) {
		return getPersistence().countByUuid(uuid);
	}

	/**
	 * Returns an ordered range of all the upgrade runs where uuid = &#63; and companyId = &#63;.
	 *
	 * <p>
	 * Useful when paginating results. Returns a maximum of <code>end - start</code> instances. <code>start</code> and <code>end</code> are not primary keys, they are indexes in the result set. Thus, <code>0</code> refers to the first result in the set. Setting both <code>start</code> and <code>end</code> to <code>com.liferay.portal.kernel.dao.orm.QueryUtil#ALL_POS</code> will return the full result set. If <code>orderByComparator</code> is specified, then the query will include the given ORDER BY logic. If <code>orderByComparator</code> is absent, then the query will include the default ORDER BY logic from <code>com.liferay.upgrades.lab.agent.remote.model.impl.UpgradeRunModelImpl</code>.
	 * </p>
	 *
	 * @param uuid the uuid
	 * @param companyId the company ID
	 * @param start the lower bound of the range of upgrade runs
	 * @param end the upper bound of the range of upgrade runs (not inclusive)
	 * @param orderByComparator the comparator to order the results by (optionally <code>null</code>)
	 * @param useFinderCache whether to use the finder cache
	 * @return the ordered range of matching upgrade runs
	 */
	public static List<UpgradeRun> findByUuid_C(
		String uuid, long companyId, int start, int end,
		OrderByComparator<UpgradeRun> orderByComparator,
		boolean useFinderCache) {

		return getPersistence().findByUuid_C(
			uuid, companyId, start, end, orderByComparator, useFinderCache);
	}

	/**
	 * Returns the first upgrade run in the ordered set where uuid = &#63; and companyId = &#63;.
	 *
	 * @param uuid the uuid
	 * @param companyId the company ID
	 * @param orderByComparator the comparator to order the set by (optionally <code>null</code>)
	 * @return the first matching upgrade run
	 * @throws NoSuchUpgradeRunException if a matching upgrade run could not be found
	 */
	public static UpgradeRun findByUuid_C_First(
			String uuid, long companyId,
			OrderByComparator<UpgradeRun> orderByComparator)
		throws com.liferay.upgrades.lab.agent.remote.exception.
			NoSuchUpgradeRunException {

		return getPersistence().findByUuid_C_First(
			uuid, companyId, orderByComparator);
	}

	/**
	 * Returns the first upgrade run in the ordered set where uuid = &#63; and companyId = &#63;.
	 *
	 * @param uuid the uuid
	 * @param companyId the company ID
	 * @param orderByComparator the comparator to order the set by (optionally <code>null</code>)
	 * @return the first matching upgrade run, or <code>null</code> if a matching upgrade run could not be found
	 */
	public static UpgradeRun fetchByUuid_C_First(
		String uuid, long companyId,
		OrderByComparator<UpgradeRun> orderByComparator) {

		return getPersistence().fetchByUuid_C_First(
			uuid, companyId, orderByComparator);
	}

	/**
	 * Removes all the upgrade runs where uuid = &#63; and companyId = &#63; from the database.
	 *
	 * @param uuid the uuid
	 * @param companyId the company ID
	 */
	public static void removeByUuid_C(String uuid, long companyId) {
		getPersistence().removeByUuid_C(uuid, companyId);
	}

	/**
	 * Returns the number of upgrade runs where uuid = &#63; and companyId = &#63;.
	 *
	 * @param uuid the uuid
	 * @param companyId the company ID
	 * @return the number of matching upgrade runs
	 */
	public static int countByUuid_C(String uuid, long companyId) {
		return getPersistence().countByUuid_C(uuid, companyId);
	}

	/**
	 * Returns the upgrade run where externalReferenceCode = &#63; and companyId = &#63; or throws a <code>NoSuchUpgradeRunException</code> if it could not be found.
	 *
	 * @param externalReferenceCode the external reference code
	 * @param companyId the company ID
	 * @return the matching upgrade run
	 * @throws NoSuchUpgradeRunException if a matching upgrade run could not be found
	 */
	public static UpgradeRun findByERC_C(
			String externalReferenceCode, long companyId)
		throws com.liferay.upgrades.lab.agent.remote.exception.
			NoSuchUpgradeRunException {

		return getPersistence().findByERC_C(externalReferenceCode, companyId);
	}

	/**
	 * Returns the upgrade run where externalReferenceCode = &#63; and companyId = &#63; or returns <code>null</code> if it could not be found, optionally using the finder cache.
	 *
	 * @param externalReferenceCode the external reference code
	 * @param companyId the company ID
	 * @param useFinderCache whether to use the finder cache
	 * @return the matching upgrade run, or <code>null</code> if a matching upgrade run could not be found
	 */
	public static UpgradeRun fetchByERC_C(
		String externalReferenceCode, long companyId, boolean useFinderCache) {

		return getPersistence().fetchByERC_C(
			externalReferenceCode, companyId, useFinderCache);
	}

	/**
	 * Removes the upgrade run where externalReferenceCode = &#63; and companyId = &#63; from the database.
	 *
	 * @param externalReferenceCode the external reference code
	 * @param companyId the company ID
	 * @return the upgrade run that was removed
	 */
	public static UpgradeRun removeByERC_C(
			String externalReferenceCode, long companyId)
		throws com.liferay.upgrades.lab.agent.remote.exception.
			NoSuchUpgradeRunException {

		return getPersistence().removeByERC_C(externalReferenceCode, companyId);
	}

	/**
	 * Returns the number of upgrade runs where externalReferenceCode = &#63; and companyId = &#63;.
	 *
	 * @param externalReferenceCode the external reference code
	 * @param companyId the company ID
	 * @return the number of matching upgrade runs
	 */
	public static int countByERC_C(
		String externalReferenceCode, long companyId) {

		return getPersistence().countByERC_C(externalReferenceCode, companyId);
	}

	/**
	 * Creates a new upgrade run with the primary key. Does not add the upgrade run to the database.
	 *
	 * @param upgradeRunId the primary key for the new upgrade run
	 * @return the new upgrade run
	 */
	public static UpgradeRun create(long upgradeRunId) {
		return getPersistence().create(upgradeRunId);
	}

	/**
	 * Removes the upgrade run with the primary key from the database. Also notifies the appropriate model listeners.
	 *
	 * @param upgradeRunId the primary key of the upgrade run
	 * @return the upgrade run that was removed
	 * @throws NoSuchUpgradeRunException if a upgrade run with the primary key could not be found
	 */
	public static UpgradeRun remove(long upgradeRunId)
		throws com.liferay.upgrades.lab.agent.remote.exception.
			NoSuchUpgradeRunException {

		return getPersistence().remove(upgradeRunId);
	}

	public static UpgradeRun updateImpl(UpgradeRun upgradeRun) {
		return getPersistence().updateImpl(upgradeRun);
	}

	/**
	 * Returns the upgrade run with the primary key or throws a <code>NoSuchUpgradeRunException</code> if it could not be found.
	 *
	 * @param upgradeRunId the primary key of the upgrade run
	 * @return the upgrade run
	 * @throws NoSuchUpgradeRunException if a upgrade run with the primary key could not be found
	 */
	public static UpgradeRun findByPrimaryKey(long upgradeRunId)
		throws com.liferay.upgrades.lab.agent.remote.exception.
			NoSuchUpgradeRunException {

		return getPersistence().findByPrimaryKey(upgradeRunId);
	}

	/**
	 * Returns the upgrade run with the primary key or returns <code>null</code> if it could not be found.
	 *
	 * @param upgradeRunId the primary key of the upgrade run
	 * @return the upgrade run, or <code>null</code> if a upgrade run with the primary key could not be found
	 */
	public static UpgradeRun fetchByPrimaryKey(long upgradeRunId) {
		return getPersistence().fetchByPrimaryKey(upgradeRunId);
	}

	/**
	 * Returns the upgrade run where externalReferenceCode = &#63; and companyId = &#63; or returns <code>null</code> if it could not be found. Uses the finder cache.
	 *
	 * @param externalReferenceCode the external reference code
	 * @param companyId the company ID
	 * @return the matching upgrade run, or <code>null</code> if a matching upgrade run could not be found
	 */
	public static UpgradeRun fetchByERC_C(
		String externalReferenceCode, long companyId) {

		return getPersistence().fetchByERC_C(externalReferenceCode, companyId);
	}

	/**
	 * Returns all the upgrade runs where uuid = &#63;.
	 *
	 * @param uuid the uuid
	 * @return the matching upgrade runs
	 */
	public static List<UpgradeRun> findByUuid(String uuid) {
		return getPersistence().findByUuid(uuid);
	}

	/**
	 * Returns a range of all the upgrade runs where uuid = &#63;.
	 *
	 * <p>
	 * Useful when paginating results. Returns a maximum of <code>end - start</code> instances. <code>start</code> and <code>end</code> are not primary keys, they are indexes in the result set. Thus, <code>0</code> refers to the first result in the set. Setting both <code>start</code> and <code>end</code> to <code>com.liferay.portal.kernel.dao.orm.QueryUtil#ALL_POS</code> will return the full result set. If <code>orderByComparator</code> is specified, then the query will include the given ORDER BY logic. If <code>orderByComparator</code> is absent, then the query will include the default ORDER BY logic from <code>com.liferay.upgrades.lab.agent.remote.model.impl.UpgradeRunModelImpl</code>.
	 * </p>
	 *
	 * @param uuid the uuid
	 * @param start the lower bound of the range of upgrade runs
	 * @param end the upper bound of the range of upgrade runs (not inclusive)
	 * @return the range of matching upgrade runs
	 */
	public static List<UpgradeRun> findByUuid(String uuid, int start, int end) {
		return getPersistence().findByUuid(uuid, start, end);
	}

	/**
	 * Returns an ordered range of all the upgrade runs where uuid = &#63;.
	 *
	 * <p>
	 * Useful when paginating results. Returns a maximum of <code>end - start</code> instances. <code>start</code> and <code>end</code> are not primary keys, they are indexes in the result set. Thus, <code>0</code> refers to the first result in the set. Setting both <code>start</code> and <code>end</code> to <code>com.liferay.portal.kernel.dao.orm.QueryUtil#ALL_POS</code> will return the full result set. If <code>orderByComparator</code> is specified, then the query will include the given ORDER BY logic. If <code>orderByComparator</code> is absent, then the query will include the default ORDER BY logic from <code>com.liferay.upgrades.lab.agent.remote.model.impl.UpgradeRunModelImpl</code>.
	 * </p>
	 *
	 * @param uuid the uuid
	 * @param start the lower bound of the range of upgrade runs
	 * @param end the upper bound of the range of upgrade runs (not inclusive)
	 * @param orderByComparator the comparator to order the results by (optionally <code>null</code>)
	 * @return the ordered range of matching upgrade runs
	 */
	public static List<UpgradeRun> findByUuid(
		String uuid, int start, int end,
		OrderByComparator<UpgradeRun> orderByComparator) {

		return getPersistence().findByUuid(uuid, start, end, orderByComparator);
	}

	/**
	 * Returns all the upgrade runs where uuid = &#63; and companyId = &#63;.
	 *
	 * @param uuid the uuid
	 * @param companyId the company ID
	 * @return the matching upgrade runs
	 */
	public static List<UpgradeRun> findByUuid_C(String uuid, long companyId) {
		return getPersistence().findByUuid_C(uuid, companyId);
	}

	/**
	 * Returns a range of all the upgrade runs where uuid = &#63; and companyId = &#63;.
	 *
	 * <p>
	 * Useful when paginating results. Returns a maximum of <code>end - start</code> instances. <code>start</code> and <code>end</code> are not primary keys, they are indexes in the result set. Thus, <code>0</code> refers to the first result in the set. Setting both <code>start</code> and <code>end</code> to <code>com.liferay.portal.kernel.dao.orm.QueryUtil#ALL_POS</code> will return the full result set. If <code>orderByComparator</code> is specified, then the query will include the given ORDER BY logic. If <code>orderByComparator</code> is absent, then the query will include the default ORDER BY logic from <code>com.liferay.upgrades.lab.agent.remote.model.impl.UpgradeRunModelImpl</code>.
	 * </p>
	 *
	 * @param uuid the uuid
	 * @param companyId the company ID
	 * @param start the lower bound of the range of upgrade runs
	 * @param end the upper bound of the range of upgrade runs (not inclusive)
	 * @return the range of matching upgrade runs
	 */
	public static List<UpgradeRun> findByUuid_C(
		String uuid, long companyId, int start, int end) {

		return getPersistence().findByUuid_C(uuid, companyId, start, end);
	}

	/**
	 * Returns an ordered range of all the upgrade runs where uuid = &#63; and companyId = &#63;.
	 *
	 * <p>
	 * Useful when paginating results. Returns a maximum of <code>end - start</code> instances. <code>start</code> and <code>end</code> are not primary keys, they are indexes in the result set. Thus, <code>0</code> refers to the first result in the set. Setting both <code>start</code> and <code>end</code> to <code>com.liferay.portal.kernel.dao.orm.QueryUtil#ALL_POS</code> will return the full result set. If <code>orderByComparator</code> is specified, then the query will include the given ORDER BY logic. If <code>orderByComparator</code> is absent, then the query will include the default ORDER BY logic from <code>com.liferay.upgrades.lab.agent.remote.model.impl.UpgradeRunModelImpl</code>.
	 * </p>
	 *
	 * @param uuid the uuid
	 * @param companyId the company ID
	 * @param start the lower bound of the range of upgrade runs
	 * @param end the upper bound of the range of upgrade runs (not inclusive)
	 * @param orderByComparator the comparator to order the results by (optionally <code>null</code>)
	 * @return the ordered range of matching upgrade runs
	 */
	public static List<UpgradeRun> findByUuid_C(
		String uuid, long companyId, int start, int end,
		OrderByComparator<UpgradeRun> orderByComparator) {

		return getPersistence().findByUuid_C(
			uuid, companyId, start, end, orderByComparator);
	}

	public static UpgradeRunPersistence getPersistence() {
		return _persistence;
	}

	public static void setPersistence(UpgradeRunPersistence persistence) {
		_persistence = persistence;
	}

	private static volatile UpgradeRunPersistence _persistence;

}
// LIFERAY-SERVICE-BUILDER-HASH:-989048955