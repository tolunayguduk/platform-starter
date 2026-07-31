package com.platform.security.integration.keycloak.model;

public record PasswordGrantRequest(String username, String password) {
}
