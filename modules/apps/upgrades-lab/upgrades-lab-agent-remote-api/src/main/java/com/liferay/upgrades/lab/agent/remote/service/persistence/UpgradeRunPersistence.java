/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.upgrades.lab.agent.remote.service.persistence;

import com.liferay.portal.kernel.service.persistence.BasePersistence;
import com.liferay.upgrades.lab.agent.remote.exception.NoSuchUpgradeRunException;
import com.liferay.upgrades.lab.agent.remote.model.UpgradeRun;

import org.osgi.annotation.versioning.ProviderType;

/**
 * The persistence interface for the upgrade run service.
 *
 * <p>
 * Caching information and settings can be found in <code>portal.properties</code>
 * </p>
 *
 * @author Albert Gomes Cabral
 * @see UpgradeRunUtil
 * @generated
 */
@ProviderType
public interface UpgradeRunPersistence extends BasePersistence<UpgradeRun> {

	/*
	 * NOTE FOR DEVELOPERS:
	 *
	 * Never modify or reference this interface directly. Always use {@link UpgradeRunUtil} to access the upgrade run persistence. Modify <code>service.xml</code> and rerun ServiceBuilder to regenerate this interface.
	 */

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
	public java.util.List<UpgradeRun> findByUuid(
		String uuid, int start, int end,
		com.liferay.portal.kernel.util.OrderByComparator<UpgradeRun>
			orderByComparator,
		boolean useFinderCache);

	/**
	 * Returns the first upgrade run in the ordered set where uuid = &#63;.
	 *
	 * @param uuid the uuid
	 * @param orderByComparator the comparator to order the set by (optionally <code>null</code>)
	 * @return the first matching upgrade run
	 * @throws NoSuchUpgradeRunException if a matching upgrade run could not be found
	 */
	public UpgradeRun findByUuid_First(
			String uuid,
			com.liferay.portal.kernel.util.OrderByComparator<UpgradeRun>
				orderByComparator)
		throws NoSuchUpgradeRunException;

	/**
	 * Returns the first upgrade run in the ordered set where uuid = &#63;.
	 *
	 * @param uuid the uuid
	 * @param orderByComparator the comparator to order the set by (optionally <code>null</code>)
	 * @return the first matching upgrade run, or <code>null</code> if a matching upgrade run could not be found
	 */
	public UpgradeRun fetchByUuid_First(
		String uuid,
		com.liferay.portal.kernel.util.OrderByComparator<UpgradeRun>
			orderByComparator);

	/**
	 * Removes all the upgrade runs where uuid = &#63; from the database.
	 *
	 * @param uuid the uuid
	 */
	public void removeByUuid(String uuid);

	/**
	 * Returns the number of upgrade runs where uuid = &#63;.
	 *
	 * @param uuid the uuid
	 * @return the number of matching upgrade runs
	 */
	public int countByUuid(String uuid);

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
	public java.util.List<UpgradeRun> findByUuid_C(
		String uuid, long companyId, int start, int end,
		com.liferay.portal.kernel.util.OrderByComparator<UpgradeRun>
			orderByComparator,
		boolean useFinderCache);

	/**
	 * Returns the first upgrade run in the ordered set where uuid = &#63; and companyId = &#63;.
	 *
	 * @param uuid the uuid
	 * @param companyId the company ID
	 * @param orderByComparator the comparator to order the set by (optionally <code>null</code>)
	 * @return the first matching upgrade run
	 * @throws NoSuchUpgradeRunException if a matching upgrade run could not be found
	 */
	public UpgradeRun findByUuid_C_First(
			String uuid, long companyId,
			com.liferay.portal.kernel.util.OrderByComparator<UpgradeRun>
				orderByComparator)
		throws NoSuchUpgradeRunException;

	/**
	 * Returns the first upgrade run in the ordered set where uuid = &#63; and companyId = &#63;.
	 *
	 * @param uuid the uuid
	 * @param companyId the company ID
	 * @param orderByComparator the comparator to order the set by (optionally <code>null</code>)
	 * @return the first matching upgrade run, or <code>null</code> if a matching upgrade run could not be found
	 */
	public UpgradeRun fetchByUuid_C_First(
		String uuid, long companyId,
		com.liferay.portal.kernel.util.OrderByComparator<UpgradeRun>
			orderByComparator);

	/**
	 * Removes all the upgrade runs where uuid = &#63; and companyId = &#63; from the database.
	 *
	 * @param uuid the uuid
	 * @param companyId the company ID
	 */
	public void removeByUuid_C(String uuid, long companyId);

	/**
	 * Returns the number of upgrade runs where uuid = &#63; and companyId = &#63;.
	 *
	 * @param uuid the uuid
	 * @param companyId the company ID
	 * @return the number of matching upgrade runs
	 */
	public int countByUuid_C(String uuid, long companyId);

	/**
	 * Returns the upgrade run where externalReferenceCode = &#63; and companyId = &#63; or throws a <code>NoSuchUpgradeRunException</code> if it could not be found.
	 *
	 * @param externalReferenceCode the external reference code
	 * @param companyId the company ID
	 * @return the matching upgrade run
	 * @throws NoSuchUpgradeRunException if a matching upgrade run could not be found
	 */
	public UpgradeRun findByERC_C(String externalReferenceCode, long companyId)
		throws NoSuchUpgradeRunException;

	/**
	 * Returns the upgrade run where externalReferenceCode = &#63; and companyId = &#63; or returns <code>null</code> if it could not be found, optionally using the finder cache.
	 *
	 * @param externalReferenceCode the external reference code
	 * @param companyId the company ID
	 * @param useFinderCache whether to use the finder cache
	 * @return the matching upgrade run, or <code>null</code> if a matching upgrade run could not be found
	 */
	public UpgradeRun fetchByERC_C(
		String externalReferenceCode, long companyId, boolean useFinderCache);

	/**
	 * Removes the upgrade run where externalReferenceCode = &#63; and companyId = &#63; from the database.
	 *
	 * @param externalReferenceCode the external reference code
	 * @param companyId the company ID
	 * @return the upgrade run that was removed
	 */
	public UpgradeRun removeByERC_C(
			String externalReferenceCode, long companyId)
		throws NoSuchUpgradeRunException;

	/**
	 * Returns the number of upgrade runs where externalReferenceCode = &#63; and companyId = &#63;.
	 *
	 * @param externalReferenceCode the external reference code
	 * @param companyId the company ID
	 * @return the number of matching upgrade runs
	 */
	public int countByERC_C(String externalReferenceCode, long companyId);

	/**
	 * Creates a new upgrade run with the primary key. Does not add the upgrade run to the database.
	 *
	 * @param upgradeRunId the primary key for the new upgrade run
	 * @return the new upgrade run
	 */
	public UpgradeRun create(long upgradeRunId);

	/**
	 * Removes the upgrade run with the primary key from the database. Also notifies the appropriate model listeners.
	 *
	 * @param upgradeRunId the primary key of the upgrade run
	 * @return the upgrade run that was removed
	 * @throws NoSuchUpgradeRunException if a upgrade run with the primary key could not be found
	 */
	public UpgradeRun remove(long upgradeRunId)
		throws NoSuchUpgradeRunException;

	public UpgradeRun updateImpl(UpgradeRun upgradeRun);

	/**
	 * Returns the upgrade run with the primary key or throws a <code>NoSuchUpgradeRunException</code> if it could not be found.
	 *
	 * @param upgradeRunId the primary key of the upgrade run
	 * @return the upgrade run
	 * @throws NoSuchUpgradeRunException if a upgrade run with the primary key could not be found
	 */
	public UpgradeRun findByPrimaryKey(long upgradeRunId)
		throws NoSuchUpgradeRunException;

	/**
	 * Returns the upgrade run with the primary key or returns <code>null</code> if it could not be found.
	 *
	 * @param upgradeRunId the primary key of the upgrade run
	 * @return the upgrade run, or <code>null</code> if a upgrade run with the primary key could not be found
	 */
	public UpgradeRun fetchByPrimaryKey(long upgradeRunId);

	/**
	 * Returns the upgrade run where externalReferenceCode = &#63; and companyId = &#63; or returns <code>null</code> if it could not be found. Uses the finder cache.
	 *
	 * @param externalReferenceCode the external reference code
	 * @param companyId the company ID
	 * @return the matching upgrade run, or <code>null</code> if a matching upgrade run could not be found
	 */
	public default UpgradeRun fetchByERC_C(
		String externalReferenceCode, long companyId) {

		return fetchByERC_C(externalReferenceCode, companyId, true);
	}

	/**
	 * Returns all the upgrade runs where uuid = &#63;.
	 *
	 * @param uuid the uuid
	 * @return the matching upgrade runs
	 */
	public default java.util.List<UpgradeRun> findByUuid(String uuid) {
		return findByUuid(
			uuid, com.liferay.portal.kernel.dao.orm.QueryUtil.ALL_POS,
			com.liferay.portal.kernel.dao.orm.QueryUtil.ALL_POS, null, true);
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
	public default java.util.List<UpgradeRun> findByUuid(
		String uuid, int start, int end) {

		return findByUuid(uuid, start, end, null, true);
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
	public default java.util.List<UpgradeRun> findByUuid(
		String uuid, int start, int end,
		com.liferay.portal.kernel.util.OrderByComparator<UpgradeRun>
			orderByComparator) {

		return findByUuid(uuid, start, end, orderByComparator, true);
	}

	/**
	 * Returns all the upgrade runs where uuid = &#63; and companyId = &#63;.
	 *
	 * @param uuid the uuid
	 * @param companyId the company ID
	 * @return the matching upgrade runs
	 */
	public default java.util.List<UpgradeRun> findByUuid_C(
		String uuid, long companyId) {

		return findByUuid_C(
			uuid, companyId,
			com.liferay.portal.kernel.dao.orm.QueryUtil.ALL_POS,
			com.liferay.portal.kernel.dao.orm.QueryUtil.ALL_POS, null, true);
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
	public default java.util.List<UpgradeRun> findByUuid_C(
		String uuid, long companyId, int start, int end) {

		return findByUuid_C(uuid, companyId, start, end, null, true);
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
	public default java.util.List<UpgradeRun> findByUuid_C(
		String uuid, long companyId, int start, int end,
		com.liferay.portal.kernel.util.OrderByComparator<UpgradeRun>
			orderByComparator) {

		return findByUuid_C(
			uuid, companyId, start, end, orderByComparator, true);
	}

}
// LIFERAY-SERVICE-BUILDER-HASH:1190222588