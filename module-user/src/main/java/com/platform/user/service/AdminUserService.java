package com.platform.user.service;

import com.platform.user.constant.StatsRange;
import com.platform.user.service.model.AdminRoleResult;
import com.platform.user.service.model.AdminUserAuditEventResult;
import com.platform.user.service.model.AdminUserResult;
import com.platform.user.service.model.RegistrationStatsPointResult;
import com.platform.user.service.model.UpdateUserIdentityCommand;
import com.platform.user.service.model.UpdateUserRolesCommand;

import java.util.List;
import java.util.Map;

/**
 * Backs the admin panel: identity and registration timestamp come straight from Keycloak - there
 * is no local user mirror anymore. Role membership is likewise resolved and edited only against
 * Keycloak, and the set of roles the panel lets an admin assign is whatever the realm currently
 * defines, never a hardcoded list.
 */
public interface AdminUserService {

    List<AdminUserResult> listUsers();

    /** Every realm role this application manages, with its Keycloak-stored description - Keycloak
     * is the only place either is defined, so this is always exactly what the realm currently has,
     * never a hardcoded list. */
    List<AdminRoleResult> listManagedRoles();

    /** Defines a brand new role in Keycloak - functions are then granted to it via
     * AdminTableService (ROLE_PERMISSION), never assigned to a user directly. */
    void createRole(String roleName);

    /** Updates a role's description - purely descriptive, no authorization meaning. */
    void updateRoleDescription(String roleName, String description);

    /** Temporarily disables/re-enables a role - purely local policy (Keycloak has no such concept
     * for roles). While disabled: every function granted to it becomes inert (excluded from the
     * real authorization path, see RolePermissionLookupServiceImpl), and it can no longer be newly
     * assigned to a user (see updateUserRoles). Already-held assignments are left alone. */
    void updateRoleStatus(String roleName, boolean enabled);

    /** Deletes a role entirely (rejects the one this admin panel itself is gated on) and cleans up
     * its now-orphaned function grants. */
    void deleteRole(String roleName);

    void updateUserRoles(UpdateUserRolesCommand command);

    /** Enable/disable the account - a disabled user cannot obtain a token from Keycloak at all.
     * currentAdminKeycloakUserId guards against an admin disabling their own account. */
    void updateUserStatus(String keycloakUserId, boolean enabled, String currentAdminKeycloakUserId);

    /** Admin-panel edit of username/email - still routed straight through to Keycloak,
     * never persisted locally, same as everything else in this service. */
    void updateUserIdentity(UpdateUserIdentityCommand command);

    /** Audit trail for this user's identity/role changes, sourced from Keycloak's own admin
     * event log rather than any local table. */
    List<AdminUserAuditEventResult> getUserAuditEvents(String keycloakUserId);

    /** Same computation UiPermissionsService already does for "me" (see UiPermissionsController),
     * run for this user's current roles instead - lets an admin see exactly which UI functions
     * this user's roles let them see/use, without duplicating the ENABLED/DISABLED/HIDDEN logic. */
    Map<String, String> getUserUiPermissions(String keycloakUserId);

    List<RegistrationStatsPointResult> getRegistrationStats(StatsRange range);
}
