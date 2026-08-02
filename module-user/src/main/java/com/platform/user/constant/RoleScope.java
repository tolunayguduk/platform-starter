package com.platform.user.constant;

/**
 * Whether - and how far - a role's authority reaches into the admin panel at all. Purely local
 * policy, not Keycloak-owned data (same category as RoleState.enabled).
 *
 * <ul>
 *   <li>{@code NONE} (default): not an admin-panel role at all - e.g. a plain "USER" role never
 *   gets admin-panel access just by existing. This is what makes the other two values opt-in.</li>
 *   <li>{@code ORGANIZATION}: admin-panel access, confined to whatever organization(s) the holder
 *   is personally a member of (see AdminAccessScopeService).</li>
 *   <li>{@code PLATFORM}: admin-panel access, unrestricted.</li>
 * </ul>
 */
public enum RoleScope {
    NONE,
    ORGANIZATION,
    PLATFORM
}
