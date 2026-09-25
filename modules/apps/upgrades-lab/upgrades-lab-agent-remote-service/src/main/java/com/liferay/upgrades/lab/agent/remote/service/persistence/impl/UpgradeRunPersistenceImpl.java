/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.upgrades.lab.agent.remote.service.persistence.impl;

import com.liferay.portal.kernel.configuration.Configuration;
import com.liferay.portal.kernel.dao.orm.EntityCache;
import com.liferay.portal.kernel.dao.orm.FinderCache;
import com.liferay.portal.kernel.dao.orm.FinderPath;
import com.liferay.portal.kernel.dao.orm.Session;
import com.liferay.portal.kernel.dao.orm.SessionFactory;
import com.liferay.portal.kernel.exception.SystemException;
import com.liferay.portal.kernel.sanitizer.Sanitizer;
import com.liferay.portal.kernel.sanitizer.SanitizerException;
import com.liferay.portal.kernel.sanitizer.SanitizerUtil;
import com.liferay.portal.kernel.security.auth.CompanyThreadLocal;
import com.liferay.portal.kernel.security.auth.PrincipalThreadLocal;
import com.liferay.portal.kernel.service.ServiceContext;
import com.liferay.portal.kernel.service.ServiceContextThreadLocal;
import com.liferay.portal.kernel.service.persistence.impl.BasePersistenceImpl;
import com.liferay.portal.kernel.service.persistence.impl.CollectionPersistenceFinder;
import com.liferay.portal.kernel.service.persistence.impl.FinderColumn;
import com.liferay.portal.kernel.service.persistence.impl.UniquePersistenceFinder;
import com.liferay.portal.kernel.util.ContentTypes;
import com.liferay.portal.kernel.util.GetterUtil;
import com.liferay.portal.kernel.util.OrderByComparator;
import com.liferay.portal.kernel.util.ProxyUtil;
import com.liferay.portal.kernel.util.SetUtil;
import com.liferay.portal.kernel.util.Validator;
import com.liferay.portal.kernel.uuid.PortalUUIDUtil;
import com.liferay.upgrades.lab.agent.remote.exception.DuplicateUpgradeRunExternalReferenceCodeException;
import com.liferay.upgrades.lab.agent.remote.exception.NoSuchUpgradeRunException;
import com.liferay.upgrades.lab.agent.remote.model.UpgradeRun;
import com.liferay.upgrades.lab.agent.remote.model.UpgradeRunTable;
import com.liferay.upgrades.lab.agent.remote.model.impl.UpgradeRunImpl;
import com.liferay.upgrades.lab.agent.remote.model.impl.UpgradeRunModelImpl;
import com.liferay.upgrades.lab.agent.remote.service.persistence.UpgradeRunPersistence;
import com.liferay.upgrades.lab.agent.remote.service.persistence.UpgradeRunUtil;
import com.liferay.upgrades.lab.agent.remote.service.persistence.impl.constants.UpgradesPersistenceConstants;

import java.io.Serializable;

import java.lang.reflect.InvocationHandler;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import javax.sql.DataSource;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;

/**
 * The persistence implementation for the upgrade run service.
 *
 * <p>
 * Caching information and settings can be found in <code>portal.properties</code>
 * </p>
 *
 * @author Albert Gomes Cabral
 * @generated
 */
@Component(service = UpgradeRunPersistence.class)
public class UpgradeRunPersistenceImpl
	extends BasePersistenceImpl<UpgradeRun, NoSuchUpgradeRunException>
	implements UpgradeRunPersistence {

	/*
	 * NOTE FOR DEVELOPERS:
	 *
	 * Never modify or reference this class directly. Always use <code>UpgradeRunUtil</code> to access the upgrade run persistence. Modify <code>service.xml</code> and rerun ServiceBuilder to regenerate this class.
	 */
	public static final String FINDER_CLASS_NAME_ENTITY =
		UpgradeRunImpl.class.getName();

	public static final String FINDER_CLASS_NAME_LIST_WITH_PAGINATION =
		FINDER_CLASS_NAME_ENTITY + ".List1";

	public static final String FINDER_CLASS_NAME_LIST_WITHOUT_PAGINATION =
		FINDER_CLASS_NAME_ENTITY + ".List2";

	private CollectionPersistenceFinder<UpgradeRun, NoSuchUpgradeRunException>
		_collectionPersistenceFinderByUuid;

	/**
	 * Returns an ordered range of all the upgrade runs where uuid = &#63;.
	 *
	 * <p>
	 * Useful when paginating results. Returns a maximum of <code>end - start</code> instances. <code>start</code> and <code>end</code> are not primary keys, they are indexes in the result set. Thus, <code>0</code> refers to the first result in the set. Setting both <code>start</code> and <code>end</code> to <code>com.liferay.portal.kernel.dao.orm.QueryUtil#ALL_POS</code> will return the full result set. If <code>orderByComparator</code> is specified, then the query will include the given ORDER BY logic. If <code>orderByComparator</code> is absent, then the query will include the default ORDER BY logic from <code>UpgradeRunModelImpl</code>.
	 * </p>
	 *
	 * @param uuid the uuid
	 * @param start the lower bound of the range of upgrade runs
	 * @param end the upper bound of the range of upgrade runs (not inclusive)
	 * @param orderByComparator the comparator to order the results by (optionally <code>null</code>)
	 * @param useFinderCache whether to use the finder cache
	 * @return the ordered range of matching upgrade runs
	 */
	@Override
	public List<UpgradeRun> findByUuid(
		String uuid, int start, int end,
		OrderByComparator<UpgradeRun> orderByComparator,
		boolean useFinderCache) {

		return _collectionPersistenceFinderByUuid.find(
			finderCache, new Object[] {uuid}, start, end, orderByComparator,
			useFinderCache);
	}

	/**
	 * Returns the first upgrade run in the ordered set where uuid = &#63;.
	 *
	 * @param uuid the uuid
	 * @param orderByComparator the comparator to order the set by (optionally <code>null</code>)
	 * @return the first matching upgrade run
	 * @throws NoSuchUpgradeRunException if a matching upgrade run could not be found
	 */
	@Override
	public UpgradeRun findByUuid_First(
			String uuid, OrderByComparator<UpgradeRun> orderByComparator)
		throws NoSuchUpgradeRunException {

		return _collectionPersistenceFinderByUuid.findFirst(
			finderCache, new Object[] {uuid}, orderByComparator);
	}

	/**
	 * Returns the first upgrade run in the ordered set where uuid = &#63;.
	 *
	 * @param uuid the uuid
	 * @param orderByComparator the comparator to order the set by (optionally <code>null</code>)
	 * @return the first matching upgrade run, or <code>null</code> if a matching upgrade run could not be found
	 */
	@Override
	public UpgradeRun fetchByUuid_First(
		String uuid, OrderByComparator<UpgradeRun> orderByComparator) {

		return _collectionPersistenceFinderByUuid.fetchFirst(
			finderCache, new Object[] {uuid}, orderByComparator);
	}

	/**
	 * Removes all the upgrade runs where uuid = &#63; from the database.
	 *
	 * @param uuid the uuid
	 */
	@Override
	public void removeByUuid(String uuid) {
		_collectionPersistenceFinderByUuid.remove(
			finderCache, new Object[] {uuid});
	}

	/**
	 * Returns the number of upgrade runs where uuid = &#63;.
	 *
	 * @param uuid the uuid
	 * @return the number of matching upgrade runs
	 */
	@Override
	public int countByUuid(String uuid) {
		return _collectionPersistenceFinderByUuid.count(
			finderCache, new Object[] {uuid});
	}

	private CollectionPersistenceFinder<UpgradeRun, NoSuchUpgradeRunException>
		_collectionPersistenceFinderByUuid_C;

	/**
	 * Returns an ordered range of all the upgrade runs where uuid = &#63; and companyId = &#63;.
	 *
	 * <p>
	 * Useful when paginating results. Returns a maximum of <code>end - start</code> instances. <code>start</code> and <code>end</code> are not primary keys, they are indexes in the result set. Thus, <code>0</code> refers to the first result in the set. Setting both <code>start</code> and <code>end</code> to <code>com.liferay.portal.kernel.dao.orm.QueryUtil#ALL_POS</code> will return the full result set. If <code>orderByComparator</code> is specified, then the query will include the given ORDER BY logic. If <code>orderByComparator</code> is absent, then the query will include the default ORDER BY logic from <code>UpgradeRunModelImpl</code>.
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
	@Override
	public List<UpgradeRun> findByUuid_C(
		String uuid, long companyId, int start, int end,
		OrderByComparator<UpgradeRun> orderByComparator,
		boolean useFinderCache) {

		return _collectionPersistenceFinderByUuid_C.find(
			finderCache, new Object[] {uuid, companyId}, start, end,
			orderByComparator, useFinderCache);
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
	@Override
	public UpgradeRun findByUuid_C_First(
			String uuid, long companyId,
			OrderByComparator<UpgradeRun> orderByComparator)
		throws NoSuchUpgradeRunException {

		return _collectionPersistenceFinderByUuid_C.findFirst(
			finderCache, new Object[] {uuid, companyId}, orderByComparator);
	}

	/**
	 * Returns the first upgrade run in the ordered set where uuid = &#63; and companyId = &#63;.
	 *
	 * @param uuid the uuid
	 * @param companyId the company ID
	 * @param orderByComparator the comparator to order the set by (optionally <code>null</code>)
	 * @return the first matching upgrade run, or <code>null</code> if a matching upgrade run could not be found
	 */
	@Override
	public UpgradeRun fetchByUuid_C_First(
		String uuid, long companyId,
		OrderByComparator<UpgradeRun> orderByComparator) {

		return _collectionPersistenceFinderByUuid_C.fetchFirst(
			finderCache, new Object[] {uuid, companyId}, orderByComparator);
	}

	/**
	 * Removes all the upgrade runs where uuid = &#63; and companyId = &#63; from the database.
	 *
	 * @param uuid the uuid
	 * @param companyId the company ID
	 */
	@Override
	public void removeByUuid_C(String uuid, long companyId) {
		_collectionPersistenceFinderByUuid_C.remove(
			finderCache, new Object[] {uuid, companyId});
	}

	/**
	 * Returns the number of upgrade runs where uuid = &#63; and companyId = &#63;.
	 *
	 * @param uuid the uuid
	 * @param companyId the company ID
	 * @return the number of matching upgrade runs
	 */
	@Override
	public int countByUuid_C(String uuid, long companyId) {
		return _collectionPersistenceFinderByUuid_C.count(
			finderCache, new Object[] {uuid, companyId});
	}

	private UniquePersistenceFinder<UpgradeRun, NoSuchUpgradeRunException>
		_uniquePersistenceFinderByERC_C;

	/**
	 * Returns the upgrade run where externalReferenceCode = &#63; and companyId = &#63; or throws a <code>NoSuchUpgradeRunException</code> if it could not be found.
	 *
	 * @param externalReferenceCode the external reference code
	 * @param companyId the company ID
	 * @return the matching upgrade run
	 * @throws NoSuchUpgradeRunException if a matching upgrade run could not be found
	 */
	@Override
	public UpgradeRun findByERC_C(String externalReferenceCode, long companyId)
		throws NoSuchUpgradeRunException {

		return _uniquePersistenceFinderByERC_C.find(
			finderCache, new Object[] {externalReferenceCode, companyId});
	}

	/**
	 * Returns the upgrade run where externalReferenceCode = &#63; and companyId = &#63; or returns <code>null</code> if it could not be found, optionally using the finder cache.
	 *
	 * @param externalReferenceCode the external reference code
	 * @param companyId the company ID
	 * @param useFinderCache whether to use the finder cache
	 * @return the matching upgrade run, or <code>null</code> if a matching upgrade run could not be found
	 */
	@Override
	public UpgradeRun fetchByERC_C(
		String externalReferenceCode, long companyId, boolean useFinderCache) {

		return _uniquePersistenceFinderByERC_C.fetch(
			finderCache, new Object[] {externalReferenceCode, companyId},
			useFinderCache);
	}

	/**
	 * Removes the upgrade run where externalReferenceCode = &#63; and companyId = &#63; from the database.
	 *
	 * @param externalReferenceCode the external reference code
	 * @param companyId the company ID
	 * @return the upgrade run that was removed
	 */
	@Override
	public UpgradeRun removeByERC_C(
			String externalReferenceCode, long companyId)
		throws NoSuchUpgradeRunException {

		UpgradeRun upgradeRun = findByERC_C(externalReferenceCode, companyId);

		return remove(upgradeRun);
	}

	/**
	 * Returns the number of upgrade runs where externalReferenceCode = &#63; and companyId = &#63;.
	 *
	 * @param externalReferenceCode the external reference code
	 * @param companyId the company ID
	 * @return the number of matching upgrade runs
	 */
	@Override
	public int countByERC_C(String externalReferenceCode, long companyId) {
		return _uniquePersistenceFinderByERC_C.count(
			finderCache, new Object[] {externalReferenceCode, companyId});
	}

	public UpgradeRunPersistenceImpl() {
		Map<String, String> dbColumnNames = new HashMap<String, String>();

		dbColumnNames.put("uuid", "uuid_");

		setDBColumnNames(dbColumnNames);

		setModelClass(UpgradeRun.class);

		setModelImplClass(UpgradeRunImpl.class);
		setModelPKClass(long.class);

		setTable(UpgradeRunTable.INSTANCE);
	}

	/**
	 * Creates a new upgrade run with the primary key. Does not add the upgrade run to the database.
	 *
	 * @param upgradeRunId the primary key for the new upgrade run
	 * @return the new upgrade run
	 */
	@Override
	public UpgradeRun create(long upgradeRunId) {
		UpgradeRun upgradeRun = new UpgradeRunImpl();

		upgradeRun.setNew(true);
		upgradeRun.setPrimaryKey(upgradeRunId);

		String uuid = PortalUUIDUtil.generate();

		upgradeRun.setUuid(uuid);

		upgradeRun.setCompanyId(CompanyThreadLocal.getCompanyId());

		return upgradeRun;
	}

	/**
	 * Removes the upgrade run with the primary key from the database. Also notifies the appropriate model listeners.
	 *
	 * @param upgradeRunId the primary key of the upgrade run
	 * @return the upgrade run that was removed
	 * @throws NoSuchUpgradeRunException if a upgrade run with the primary key could not be found
	 */
	@Override
	public UpgradeRun remove(long upgradeRunId)
		throws NoSuchUpgradeRunException {

		return remove((Serializable)upgradeRunId);
	}

	@Override
	protected UpgradeRun removeImpl(UpgradeRun upgradeRun) {
		Session session = null;

		try {
			session = openSession();

			if (!session.contains(upgradeRun)) {
				upgradeRun = (UpgradeRun)session.get(
					UpgradeRunImpl.class, upgradeRun.getPrimaryKeyObj());
			}

			if (upgradeRun != null) {
				session.delete(upgradeRun);
			}
		}
		catch (Exception exception) {
			throw processException(exception);
		}
		finally {
			closeSession(session);
		}

		if (upgradeRun != null) {
			clearCache(upgradeRun);
		}

		return upgradeRun;
	}

	@Override
	public UpgradeRun updateImpl(UpgradeRun upgradeRun) {
		boolean isNew = upgradeRun.isNew();

		if (!(upgradeRun instanceof UpgradeRunModelImpl)) {
			InvocationHandler invocationHandler = null;

			if (ProxyUtil.isProxyClass(upgradeRun.getClass())) {
				invocationHandler = ProxyUtil.getInvocationHandler(upgradeRun);

				throw new IllegalArgumentException(
					"Implement ModelWrapper in upgradeRun proxy " +
						invocationHandler.getClass());
			}

			throw new IllegalArgumentException(
				"Implement ModelWrapper in custom UpgradeRun implementation " +
					upgradeRun.getClass());
		}

		UpgradeRunModelImpl upgradeRunModelImpl =
			(UpgradeRunModelImpl)upgradeRun;

		if (Validator.isNull(upgradeRun.getUuid())) {
			String uuid = PortalUUIDUtil.generate();

			upgradeRun.setUuid(uuid);
		}

		if (Validator.isNull(upgradeRun.getExternalReferenceCode())) {
			upgradeRun.setExternalReferenceCode(upgradeRun.getUuid());
		}
		else {
			if (!Objects.equals(
					upgradeRunModelImpl.getColumnOriginalValue(
						"externalReferenceCode"),
					upgradeRun.getExternalReferenceCode())) {

				long userId = GetterUtil.getLong(
					PrincipalThreadLocal.getName());

				if (userId > 0) {
					long companyId = upgradeRun.getCompanyId();

					long groupId = 0;

					long classPK = 0;

					if (!isNew) {
						classPK = upgradeRun.getPrimaryKey();
					}

					try {
						upgradeRun.setExternalReferenceCode(
							SanitizerUtil.sanitize(
								companyId, groupId, userId,
								UpgradeRun.class.getName(), classPK,
								ContentTypes.TEXT_HTML, Sanitizer.MODE_ALL,
								upgradeRun.getExternalReferenceCode(), null));
					}
					catch (SanitizerException sanitizerException) {
						throw new SystemException(sanitizerException);
					}
				}
			}

			UpgradeRun ercUpgradeRun = fetchByERC_C(
				upgradeRun.getExternalReferenceCode(),
				upgradeRun.getCompanyId());

			if (isNew) {
				if (ercUpgradeRun != null) {
					throw new DuplicateUpgradeRunExternalReferenceCodeException(
						"Duplicate upgrade run with external reference code " +
							upgradeRun.getExternalReferenceCode() +
								" and company " + upgradeRun.getCompanyId());
				}
			}
			else {
				if ((ercUpgradeRun != null) &&
					(upgradeRun.getUpgradeRunId() !=
						ercUpgradeRun.getUpgradeRunId())) {

					throw new DuplicateUpgradeRunExternalReferenceCodeException(
						"Duplicate upgrade run with external reference code " +
							upgradeRun.getExternalReferenceCode() +
								" and company " + upgradeRun.getCompanyId());
				}
			}
		}

		ServiceContext serviceContext =
			ServiceContextThreadLocal.getServiceContext();

		Date date = new Date();

		if (isNew && (upgradeRun.getCreateDate() == null)) {
			if (serviceContext == null) {
				upgradeRun.setCreateDate(date);
			}
			else {
				upgradeRun.setCreateDate(serviceContext.getCreateDate(date));
			}
		}

		if (!upgradeRunModelImpl.hasSetModifiedDate()) {
			if (serviceContext == null) {
				upgradeRun.setModifiedDate(date);
			}
			else {
				upgradeRun.setModifiedDate(
					serviceContext.getModifiedDate(date));
			}
		}

		Session session = null;

		try {
			session = openSession();

			if (isNew) {
				session.save(upgradeRun);
			}
			else {
				upgradeRun = (UpgradeRun)session.merge(upgradeRun);
			}
		}
		catch (Exception exception) {
			throw processException(exception);
		}
		finally {
			closeSession(session);
		}

		cacheUniqueFindersResult(upgradeRun, false);

		if (isNew) {
			upgradeRun.setNew(false);
		}

		upgradeRun.resetOriginalValues();

		return upgradeRun;
	}

	/**
	 * Returns the upgrade run with the primary key or throws a <code>NoSuchUpgradeRunException</code> if it could not be found.
	 *
	 * @param upgradeRunId the primary key of the upgrade run
	 * @return the upgrade run
	 * @throws NoSuchUpgradeRunException if a upgrade run with the primary key could not be found
	 */
	@Override
	public UpgradeRun findByPrimaryKey(long upgradeRunId)
		throws NoSuchUpgradeRunException {

		return findByPrimaryKey((Serializable)upgradeRunId);
	}

	/**
	 * Returns the upgrade run with the primary key or returns <code>null</code> if it could not be found.
	 *
	 * @param upgradeRunId the primary key of the upgrade run
	 * @return the upgrade run, or <code>null</code> if a upgrade run with the primary key could not be found
	 */
	@Override
	public UpgradeRun fetchByPrimaryKey(long upgradeRunId) {
		return fetchByPrimaryKey((Serializable)upgradeRunId);
	}

	@Override
	public Set<String> getBadColumnNames() {
		return _badColumnNames;
	}

	@Override
	protected EntityCache getEntityCache() {
		return entityCache;
	}

	@Override
	protected String getPKDBName() {
		return "upgradeRunId";
	}

	@Override
	protected String getSelectSQL() {
		return _SQL_SELECT_UPGRADERUN;
	}

	@Override
	protected Map<String, Integer> getTableColumnsMap() {
		return UpgradeRunModelImpl.TABLE_COLUMNS_MAP;
	}

	/**
	 * Initializes the upgrade run persistence.
	 */
	@Activate
	public void activate() {
		_collectionPersistenceFinderByUuid = new CollectionPersistenceFinder<>(
			this,
			new FinderPath(
				FINDER_CLASS_NAME_LIST_WITH_PAGINATION, "findByUuid",
				new String[] {
					String.class.getName(), Integer.class.getName(),
					Integer.class.getName(), OrderByComparator.class.getName()
				},
				new String[] {"uuid_"}, true),
			new FinderPath(
				FINDER_CLASS_NAME_LIST_WITHOUT_PAGINATION, "findByUuid",
				new String[] {String.class.getName()}, new String[] {"uuid_"},
				0, 1, true, null),
			new FinderPath(
				FINDER_CLASS_NAME_LIST_WITHOUT_PAGINATION, "countByUuid",
				new String[] {String.class.getName()}, new String[] {"uuid_"},
				0, 1, false, null),
			_SQL_SELECT_UPGRADERUN_WHERE, _SQL_COUNT_UPGRADERUN_WHERE,
			UpgradeRunModelImpl.ORDER_BY_JPQL, _ENTITY_ALIAS_PREFIX, "", "",
			null,
			new FinderColumn<>(
				"upgradeRun.", "uuid", "uuid_", FinderColumn.Type.STRING, "=",
				true, true, UpgradeRun::getUuid));

		_collectionPersistenceFinderByUuid_C =
			new CollectionPersistenceFinder<>(
				this,
				new FinderPath(
					FINDER_CLASS_NAME_LIST_WITH_PAGINATION, "findByUuid_C",
					new String[] {
						String.class.getName(), Long.class.getName(),
						Integer.class.getName(), Integer.class.getName(),
						OrderByComparator.class.getName()
					},
					new String[] {"uuid_", "companyId"}, true),
				new FinderPath(
					FINDER_CLASS_NAME_LIST_WITHOUT_PAGINATION, "findByUuid_C",
					new String[] {String.class.getName(), Long.class.getName()},
					new String[] {"uuid_", "companyId"}, 0, 1, true, null),
				new FinderPath(
					FINDER_CLASS_NAME_LIST_WITHOUT_PAGINATION, "countByUuid_C",
					new String[] {String.class.getName(), Long.class.getName()},
					new String[] {"uuid_", "companyId"}, 0, 1, false, null),
				_SQL_SELECT_UPGRADERUN_WHERE, _SQL_COUNT_UPGRADERUN_WHERE,
				UpgradeRunModelImpl.ORDER_BY_JPQL, _ENTITY_ALIAS_PREFIX, "", "",
				null,
				new FinderColumn<>(
					"upgradeRun.", "uuid", "uuid_", FinderColumn.Type.STRING,
					"=", true, true, UpgradeRun::getUuid),
				new FinderColumn<>(
					"upgradeRun.", "companyId", FinderColumn.Type.LONG, "=",
					true, true, UpgradeRun::getCompanyId));

		_uniquePersistenceFinderByERC_C = new UniquePersistenceFinder<>(
			this,
			createUniqueFinderPath(
				FINDER_CLASS_NAME_ENTITY, "fetchByERC_C",
				new String[] {String.class.getName(), Long.class.getName()},
				new String[] {"externalReferenceCode", "companyId"}, 0, 1,
				false,
				convertNullFunction(UpgradeRun::getExternalReferenceCode),
				UpgradeRun::getCompanyId),
			_SQL_SELECT_UPGRADERUN_WHERE, "",
			new FinderColumn<>(
				"upgradeRun.", "externalReferenceCode",
				FinderColumn.Type.STRING, "=", true, true,
				UpgradeRun::getExternalReferenceCode),
			new FinderColumn<>(
				"upgradeRun.", "companyId", FinderColumn.Type.LONG, "=", true,
				true, UpgradeRun::getCompanyId));

		UpgradeRunUtil.setPersistence(this);
	}

	@Deactivate
	public void deactivate() {
		UpgradeRunUtil.setPersistence(null);

		entityCache.removeCache(UpgradeRunImpl.class.getName());
	}

	@Override
	@Reference(
		target = UpgradesPersistenceConstants.SERVICE_CONFIGURATION_FILTER,
		unbind = "-"
	)
	public void setConfiguration(Configuration configuration) {
	}

	@Override
	@Reference(
		target = UpgradesPersistenceConstants.ORIGIN_BUNDLE_SYMBOLIC_NAME_FILTER,
		unbind = "-"
	)
	public void setDataSource(DataSource dataSource) {
		super.setDataSource(dataSource);
	}

	@Override
	@Reference(
		target = UpgradesPersistenceConstants.ORIGIN_BUNDLE_SYMBOLIC_NAME_FILTER,
		unbind = "-"
	)
	public void setSessionFactory(SessionFactory sessionFactory) {
		super.setSessionFactory(sessionFactory);
	}

	@Reference
	protected EntityCache entityCache;

	@Reference
	protected FinderCache finderCache;

	private static final String _ENTITY_ALIAS_PREFIX =
		UpgradeRunModelImpl.ENTITY_ALIAS + ".";

	private static final String _SQL_SELECT_UPGRADERUN =
		"SELECT upgradeRun FROM UpgradeRun upgradeRun";

	private static final String _SQL_SELECT_UPGRADERUN_WHERE =
		"SELECT upgradeRun FROM UpgradeRun upgradeRun WHERE ";

	private static final String _SQL_COUNT_UPGRADERUN_WHERE =
		"SELECT COUNT(upgradeRun) FROM UpgradeRun upgradeRun WHERE ";

	private static final Set<String> _badColumnNames = SetUtil.fromArray(
		new String[] {"uuid"});

	@Override
	protected FinderCache getFinderCache() {
		return finderCache;
	}

}
// LIFERAY-SERVICE-BUILDER-HASH:-371626535