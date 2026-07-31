package com.platform.security.integration.keycloak.model;

/** Every user in the realm - identity plus Keycloak's own bookkeeping fields (enabled, createdTimestamp). */
public record KeycloakUserSummary(String id, String username, String email, String firstName, String lastName,
                                   boolean enabled, long createdTimestamp) {
}
