package com.platform.app.service;

import com.platform.security.integration.keycloak.model.TokenResponse;

/**
 * Thin orchestration in front of Keycloak's token endpoint (KeycloakTokenClient, the Integration
 * layer) - exists so AuthController never calls an Integration class directly.
 */
public interface AuthService {

    TokenResponse login(String username, String password);

    TokenResponse refresh(String refreshToken);

    void logout(String refreshToken);
}
