package com.platform.security.integration.keycloak.model;

/** Keycloak is the source of truth for username/email/firstName/lastName - always read them live from here. */
public record KeycloakUser(String username, String email, String firstName, String lastName) {
}
