package com.platform.user.constant;

/**
 * Whether a role's authority reaches into the admin panel platform-wide. Purely local policy, not
 * Keycloak-owned data (same category as RoleState.enabled).
 *
 * <ul>
 *   <li>{@code NONE} (default): not a platform-wide admin role - e.g. a plain "USER" role never
 *   gets platform-wide admin access just by existing. This is what makes PLATFORM opt-in.</li>
 *   <li>{@code PLATFORM}: unrestricted admin-panel access to everything.</li>
 * </ul>
 *
 * <p>Organization-level admin authority is <em>not</em> a role concept at all - see
 * {@code OrganizationManager}/{@code AdminAccessScopeService}: it's granted per organization,
 * independent of any role, to whoever is explicitly listed as that organization's manager.
 */
public enum RoleScope {
    NONE,
    PLATFORM
}
