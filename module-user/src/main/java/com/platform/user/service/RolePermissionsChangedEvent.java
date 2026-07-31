package com.platform.user.service;

/** Published after any INSERT/UPDATE/DELETE on role_permission. Triggers cache eviction. */
public record RolePermissionsChangedEvent(String roleName) {
}
