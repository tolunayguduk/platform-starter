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

    /** Public (permitAll) lookup so the register page can show "You're joining: {name}" instead
     * of a raw organization id when arriving via an invite link - the visitor has no token yet
     * at this point. Throws a clean business error if the id doesn't exist. */
    String getOrganizationName(String organizationId);
}
