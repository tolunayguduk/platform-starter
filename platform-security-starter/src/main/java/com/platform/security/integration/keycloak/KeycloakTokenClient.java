package com.platform.security.integration.keycloak;

import com.platform.security.integration.keycloak.model.PasswordGrantRequest;
import com.platform.security.integration.keycloak.model.RefreshGrantRequest;
import com.platform.security.integration.keycloak.model.TokenResponse;

/**
 * Direct (Resource Owner Password Credentials) grant against Keycloak's token endpoint, used
 * only by the React SPA's own login form so the login page can be fully custom instead of
 * Keycloak's hosted one. ROPC is deprecated in OAuth 2.1 (the password passes through this app's
 * code, unlike the authorization-code flow) - kept deliberately narrow: this is the only place a
 * raw password is ever handled, and it is never persisted, only forwarded to Keycloak and discarded.
 */
public interface KeycloakTokenClient {

    TokenResponse passwordGrant(PasswordGrantRequest request);

    /** Exchanges a still-valid refresh token for a new access token. */
    TokenResponse refreshGrant(RefreshGrantRequest request);

    /** Server-side ("back-channel") session revocation - ends the Keycloak SSO session directly
     * via the refresh token, no browser redirect through Keycloak's own logout page needed. */
    void endSession(RefreshGrantRequest request);
}
