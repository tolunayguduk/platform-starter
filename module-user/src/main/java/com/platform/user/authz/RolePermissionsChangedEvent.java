package com.platform.user.authz;

/** Published after any INSERT/UPDATE/DELETE on role_permission. Triggers cache eviction. */
public record RolePermissionsChangedEvent(Long roleId) {
}
