package com.platform.user.service;

import com.platform.user.service.model.AdminAccessScope;

import java.util.List;
import java.util.Set;

/**
 * Resolves how far an admin-panel caller's authority reaches: PLATFORM-scoped (any enabled role
 * with RoleScope.PLATFORM - see role_state) sees/manages everything; everyone else is confined to
 * whatever organization(s) (Keycloak Groups) they're personally a member of. Shared by
 * AdminUserService and AdminOrganizationService so isolation is enforced identically everywhere,
 * never re-derived per call site.
 */
public interface AdminAccessScopeService {

    AdminAccessScope resolve(String callerKeycloakUserId);

    /** Throws if the caller isn't PLATFORM-scoped - the shared guard behind every platform-only
     * action (role/function definitions, deleting an organization, PERMISSION/ROLE_PERMISSION
     * writes, etc.), so the check and its error are never re-implemented per call site. */
    void requirePlatformScope(String callerKeycloakUserId);

    /** Which of managedRoles this user currently holds in Keycloak - the same per-role
     * membership lookup every admin-panel role resolution needs (effective roles, UI
     * permissions, access scope). */
    Set<String> resolveUserRoles(String keycloakUserId, List<String> managedRoles);

    /** The Keycloak user ids visible to an organization-scoped caller - members of every
     * organization they manage. Shared by every read that needs to confine itself to "users I'm
     * allowed to see" (the user list, registration stats, the activity feed, the raw database
     * table browser) so they all agree with each other. Meaningless (and never called) for a
     * PLATFORM-scoped caller, who sees everyone. */
    Set<String> resolveVisibleUserIds(AdminAccessScope callerScope);
}
