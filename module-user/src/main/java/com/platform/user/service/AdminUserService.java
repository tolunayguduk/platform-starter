package com.platform.user.service;

import com.platform.user.constant.StatsRange;
import com.platform.user.service.model.AdminUserAuditEventResult;
import com.platform.user.service.model.AdminUserResult;
import com.platform.user.service.model.RegistrationStatsPointResult;
import com.platform.user.service.model.UpdateUserIdentityCommand;
import com.platform.user.service.model.UpdateUserRolesCommand;

import java.util.List;

/**
 * Backs the admin panel: identity and registration timestamp come straight from Keycloak - there
 * is no local user mirror anymore. Role membership is likewise resolved and edited only against
 * Keycloak, and the set of roles the panel lets an admin assign is whatever the realm currently
 * defines, never a hardcoded list.
 */
public interface AdminUserService {

    List<AdminUserResult> listUsers();

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

    List<RegistrationStatsPointResult> getRegistrationStats(StatsRange range);
}
