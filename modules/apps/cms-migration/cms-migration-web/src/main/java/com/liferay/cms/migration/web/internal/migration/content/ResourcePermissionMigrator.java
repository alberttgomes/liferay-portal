/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.cms.migration.web.internal.migration.content;

import com.liferay.cms.migration.web.internal.migration.CMSMigrationContext;
import com.liferay.depot.constants.DepotRolesConstants;
import com.liferay.object.model.ObjectDefinition;
import com.liferay.object.model.ObjectEntry;
import com.liferay.petra.string.StringBundler;
import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.model.ResourceConstants;
import com.liferay.portal.kernel.model.ResourcePermission;
import com.liferay.portal.kernel.model.Role;
import com.liferay.portal.kernel.model.role.RoleConstants;
import com.liferay.portal.kernel.security.permission.ResourceActionsUtil;
import com.liferay.portal.kernel.service.ResourcePermissionLocalService;
import com.liferay.portal.kernel.service.RoleLocalService;
import com.liferay.portal.kernel.util.HashMapBuilder;
import com.liferay.portal.kernel.util.ListUtil;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author Albert Gomes Cabral
 */
public class ResourcePermissionMigrator {

	public ResourcePermissionMigrator(
		ResourcePermissionLocalService resourcePermissionLocalService,
		RoleLocalService roleLocalService) {

		_resourcePermissionLocalService = resourcePermissionLocalService;
		_roleLocalService = roleLocalService;
	}

	public void migrate(
			CMSMigrationContext cmsMigrationContext, String className,
			long classPK, long groupId, ObjectDefinition objectDefinition,
			ObjectEntry objectEntry)
		throws PortalException {

		List<String> actionIds = ListUtil.filter(
			ResourceActionsUtil.getModelResourceActions(className),
			ResourceActionsUtil.getModelResourceActions(
				objectDefinition.getClassName())::contains);

		long companyId = cmsMigrationContext.getCompanyId();

		Role ownerRole = _getRole(companyId, RoleConstants.OWNER);

		Map<Long, Set<String>> roleIdsToActionIds = _getRoleIdsToActionIds(
			actionIds, className, classPK, companyId, ownerRole.getRoleId());

		Role guestRole = _getRole(companyId, RoleConstants.GUEST);

		if (roleIdsToActionIds.equals(
				_getDefaultRoleIdsToActionIds(
					actionIds, className, groupId, guestRole.getRoleId()))) {

			return;
		}

		Map<Long, String[]> objectEntryRoleIdsToActionIds = new HashMap<>();

		String primKey = String.valueOf(objectEntry.getHeadObjectEntryId());

		for (ResourcePermission resourcePermission :
				_resourcePermissionLocalService.getResourcePermissions(
					companyId, objectDefinition.getClassName(),
					ResourceConstants.SCOPE_INDIVIDUAL, primKey)) {

			if (resourcePermission.getRoleId() != ownerRole.getRoleId()) {
				objectEntryRoleIdsToActionIds.put(
					resourcePermission.getRoleId(), new String[0]);
			}
		}

		for (Map.Entry<Long, Set<String>> entry :
				roleIdsToActionIds.entrySet()) {

			Role role = _roleLocalService.getRole(entry.getKey());

			Role objectEntryRole = _getObjectEntryRole(role);

			if (objectEntryRole == null) {
				cmsMigrationContext.getCMSMigrationReport(
				).addWarning(
					StringBundler.concat(
						"Skipped the permissions of the role ", role.getName(),
						" in the entry ",
						objectEntry.getExternalReferenceCode(),
						" because the role has no asset library counterpart")
				);

				continue;
			}

			Set<String> objectEntryRoleActionIds = entry.getValue();

			if (objectEntryRole.getRoleId() == guestRole.getRoleId()) {
				objectEntryRoleActionIds.removeAll(
					ResourceActionsUtil.getModelResourceGuestUnsupportedActions(
						objectDefinition.getClassName()));
			}

			objectEntryRoleIdsToActionIds.put(
				objectEntryRole.getRoleId(),
				objectEntryRoleActionIds.toArray(new String[0]));
		}

		_resourcePermissionLocalService.setResourcePermissions(
			companyId, objectDefinition.getClassName(),
			ResourceConstants.SCOPE_INDIVIDUAL, primKey,
			objectEntryRoleIdsToActionIds);
	}

	private Map<Long, Set<String>> _getDefaultRoleIdsToActionIds(
			List<String> actionIds, String className, long groupId,
			long guestRoleId)
		throws PortalException {

		Map<Long, Set<String>> roleIdsToActionIds = new HashMap<>();

		Role defaultGroupRole = _roleLocalService.getDefaultGroupRole(groupId);

		_putSupportedActionIds(
			actionIds,
			ResourceActionsUtil.getModelResourceGroupDefaultActions(className),
			defaultGroupRole.getRoleId(), roleIdsToActionIds);

		_putSupportedActionIds(
			actionIds,
			ResourceActionsUtil.getModelResourceGuestDefaultActions(className),
			guestRoleId, roleIdsToActionIds);

		return roleIdsToActionIds;
	}

	private Role _getObjectEntryRole(Role role) throws PortalException {
		if ((role.getType() == RoleConstants.TYPE_DEPOT) ||
			(role.getType() == RoleConstants.TYPE_REGULAR)) {

			return role;
		}

		String depotRoleName = _depotRoleNames.get(role.getName());

		if (depotRoleName == null) {
			return null;
		}

		return _getRole(role.getCompanyId(), depotRoleName);
	}

	private Role _getRole(long companyId, String name) throws PortalException {
		Role role = _roles.get(name);

		if (role == null) {
			role = _roleLocalService.getRole(companyId, name);

			_roles.put(name, role);
		}

		return role;
	}

	private Map<Long, Set<String>> _getRoleIdsToActionIds(
		List<String> actionIds, String className, long classPK, long companyId,
		long ownerRoleId) {

		Map<Long, Set<String>> roleIdsToActionIds = new HashMap<>();

		Map<Long, Set<String>> availableRoleIdsToActionIds =
			_resourcePermissionLocalService.
				getAvailableResourcePermissionActionIds(
					companyId, className, ResourceConstants.SCOPE_INDIVIDUAL,
					String.valueOf(classPK), actionIds);

		for (Map.Entry<Long, Set<String>> entry :
				availableRoleIdsToActionIds.entrySet()) {

			if (entry.getKey() != ownerRoleId) {
				_putSupportedActionIds(
					actionIds, entry.getValue(), entry.getKey(),
					roleIdsToActionIds);
			}
		}

		return roleIdsToActionIds;
	}

	private void _putSupportedActionIds(
		List<String> actionIds, Collection<String> roleActionIds, long roleId,
		Map<Long, Set<String>> roleIdsToActionIds) {

		Set<String> supportedRoleActionIds = new HashSet<>(roleActionIds);

		supportedRoleActionIds.retainAll(actionIds);

		if (!supportedRoleActionIds.isEmpty()) {
			roleIdsToActionIds.put(roleId, supportedRoleActionIds);
		}
	}

	private static final Map<String, String> _depotRoleNames =
		HashMapBuilder.put(
			RoleConstants.SITE_ADMINISTRATOR,
			DepotRolesConstants.ASSET_LIBRARY_ADMINISTRATOR
		).put(
			RoleConstants.SITE_CONTENT_REVIEWER,
			DepotRolesConstants.ASSET_LIBRARY_CONTENT_REVIEWER
		).put(
			RoleConstants.SITE_MEMBER, DepotRolesConstants.ASSET_LIBRARY_MEMBER
		).put(
			RoleConstants.SITE_OWNER, DepotRolesConstants.ASSET_LIBRARY_OWNER
		).build();

	private final ResourcePermissionLocalService
		_resourcePermissionLocalService;
	private final RoleLocalService _roleLocalService;
	private final Map<String, Role> _roles = new HashMap<>();

}