package com.platform.security.integration.keycloak.model;

/** One entry from Keycloak's own admin event log ({@code GET /admin-events}) - the audit trail for
 * admin-initiated changes to Keycloak-owned data (identity, role mappings). Never persisted locally. */
public record AdminEvent(long time, String operationType, String resourceType, String resourcePath, String representation) {
}
