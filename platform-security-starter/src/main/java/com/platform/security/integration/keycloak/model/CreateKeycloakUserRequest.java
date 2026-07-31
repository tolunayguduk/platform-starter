package com.platform.security.integration.keycloak.model;

public record CreateKeycloakUserRequest(String username, String email, String password, String firstName, String lastName) {
}
