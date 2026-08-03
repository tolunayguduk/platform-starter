package com.platform.app.service;

import com.platform.security.integration.keycloak.KeycloakAdminClient;
import com.platform.security.integration.keycloak.KeycloakTokenClient;
import com.platform.security.integration.keycloak.model.PasswordGrantRequest;
import com.platform.security.integration.keycloak.model.RefreshGrantRequest;
import com.platform.security.integration.keycloak.model.TokenResponse;
import org.springframework.stereotype.Service;

@Service
public class AuthServiceImpl implements AuthService {

    private final KeycloakTokenClient keycloakTokenClient;
    private final KeycloakAdminClient keycloakAdminClient;

    public AuthServiceImpl(KeycloakTokenClient keycloakTokenClient, KeycloakAdminClient keycloakAdminClient) {
        this.keycloakTokenClient = keycloakTokenClient;
        this.keycloakAdminClient = keycloakAdminClient;
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

    @Override
    public String getOrganizationName(String organizationId) {
        return keycloakAdminClient.getGroup(organizationId).name();
    }
}
