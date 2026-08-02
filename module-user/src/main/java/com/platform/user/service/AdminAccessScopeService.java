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

    /** Which of managedRoles this user currently holds in Keycloak - the same per-role
     * membership lookup every admin-panel role resolution needs (effective roles, UI
     * permissions, access scope). */
    Set<String> resolveUserRoles(String keycloakUserId, List<String> managedRoles);
}
