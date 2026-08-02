package com.platform.user.service;

import com.platform.user.constant.RoleScope;
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
 *
 * <p>Every method that takes a {@code callerKeycloakUserId} enforces access scope through
 * {@link AdminAccessScopeService}: a PLATFORM-scoped caller (see {@link RoleScope}) is
 * unrestricted; everyone else is confined to their own organization(s) and can never touch role
 * or function definitions at all, only user↔role assignment and their own organization's
 * membership.
 */
public interface AdminUserService {

    /** Every user, filtered to the caller's own organization(s) unless they're PLATFORM-scoped. */
    List<AdminUserResult> listUsers(String callerKeycloakUserId);

    /** Every realm role this application manages, with its Keycloak-stored description - Keycloak
     * is the only place either is defined, so this is always exactly what the realm currently has,
     * never a hardcoded list. Not scope-filtered - every caller needs the full role list to render
     * role pickers (PLATFORM-scope roles are shown disabled to an org-scoped caller, not hidden). */
    List<AdminRoleResult> listManagedRoles();

    /** Defines a brand new role in Keycloak (PLATFORM-scope only) - functions are then granted to
     * it via AdminTableService (ROLE_PERMISSION), never assigned to a user directly. */
    void createRole(String roleName, String callerKeycloakUserId);

    /** Updates a role's description - purely descriptive, no authorization meaning (PLATFORM-scope only). */
    void updateRoleDescription(String roleName, String description, String callerKeycloakUserId);

    /** Temporarily disables/re-enables a role (PLATFORM-scope only) - purely local policy (Keycloak
     * has no such concept for roles). While disabled: every function granted to it becomes inert
     * (excluded from the real authorization path, see RolePermissionLookupServiceImpl), and it can
     * no longer be newly assigned to a user (see updateUserRoles). Already-held assignments are
     * left alone. Disabling the last enabled PLATFORM-scope role is rejected - see
     * AdminUserServiceImpl.assertNotLastEnabledPlatformRole. */
    void updateRoleStatus(String roleName, boolean enabled, String callerKeycloakUserId);

    /** Sets whether a role's authority is realm-wide (PLATFORM) or confined to the holder's own
     * organization(s) (ORGANIZATION) - PLATFORM-scope only (an org-scoped admin promoting a role to
     * PLATFORM would be a privilege-escalation path otherwise). */
    void updateRoleScope(String roleName, RoleScope scope, String callerKeycloakUserId);

    /** Deletes a role entirely (PLATFORM-scope only) and cleans up its now-orphaned function
     * grants. Deleting the last enabled PLATFORM-scope role is rejected - same guard as
     * updateRoleStatus. */
    void deleteRole(String roleName, String callerKeycloakUserId);

    void updateUserRoles(UpdateUserRolesCommand command);

    /** Enable/disable the account - a disabled user cannot obtain a token from Keycloak at all.
     * currentAdminKeycloakUserId guards against an admin disabling their own account, and (if
     * org-scoped) against reaching outside their own organization. */
    void updateUserStatus(String keycloakUserId, boolean enabled, String currentAdminKeycloakUserId);

    /** Admin-panel edit of username/email - still routed straight through to Keycloak,
     * never persisted locally, same as everything else in this service. */
    void updateUserIdentity(UpdateUserIdentityCommand command);

    /** Audit trail for this user's identity/role changes, sourced from Keycloak's own admin
     * event log rather than any local table. */
    List<AdminUserAuditEventResult> getUserAuditEvents(String keycloakUserId);

    /** Realm-wide recent admin activity (not scoped to one user) - backs the admin panel's
     * "Recent Activity" feed, same Keycloak admin event log as getUserAuditEvents. */
    List<AdminUserAuditEventResult> getRecentActivity(int limit);

    /** Same computation UiPermissionsService already does for "me" (see UiPermissionsController),
     * run for this user's current roles instead - lets an admin see exactly which UI functions
     * this user's roles let them see/use, without duplicating the ENABLED/DISABLED/HIDDEN logic. */
    Map<String, String> getUserUiPermissions(String keycloakUserId);

    List<RegistrationStatsPointResult> getRegistrationStats(StatsRange range);
}
