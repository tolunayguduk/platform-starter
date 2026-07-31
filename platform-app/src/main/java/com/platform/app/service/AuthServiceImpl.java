package com.platform.app.service;

import com.platform.security.integration.keycloak.KeycloakTokenClient;
import com.platform.security.integration.keycloak.model.PasswordGrantRequest;
import com.platform.security.integration.keycloak.model.RefreshGrantRequest;
import com.platform.security.integration.keycloak.model.TokenResponse;
import org.springframework.stereotype.Service;

@Service
public class AuthServiceImpl implements AuthService {

    private final KeycloakTokenClient keycloakTokenClient;

    public AuthServiceImpl(KeycloakTokenClient keycloakTokenClient) {
        this.keycloakTokenClient = keycloakTokenClient;
    }

    @Override
    public TokenResponse login(String username, String password) {
        return keycloakTokenClient.passwordGrant(new PasswordGrantRequest(username, password));
    }

    @Override
    public TokenResponse refresh(String refreshToken) {
        return keycloakTokenClient.refreshGrant(new RefreshGrantRequest(refreshToken));
    }

    @Override
    public void logout(String refreshToken) {
        keycloakTokenClient.endSession(new RefreshGrantRequest(refreshToken));
    }
}
