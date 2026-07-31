package com.platform.security.integration.keycloak.model;

public record UpdateKeycloakUserRequest(String email, String firstName, String lastName) {
}
