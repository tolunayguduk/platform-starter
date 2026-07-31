package com.platform.security.integration.keycloak;

import com.platform.error.BusinessException;
import com.platform.security.integration.keycloak.model.PasswordGrantRequest;
import com.platform.security.integration.keycloak.model.RefreshGrantRequest;
import com.platform.security.integration.keycloak.model.TokenResponse;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

public class KeycloakTokenClientImpl implements KeycloakTokenClient {

    private final RestClient restClient;
    private final ClientRegistration clientRegistration;

    public KeycloakTokenClientImpl(RestClient.Builder builder, ClientRegistrationRepository clientRegistrationRepository) {
        this.clientRegistration = clientRegistrationRepository.findByRegistrationId("keycloak");
        this.restClient = builder.build();
    }

    @Override
    public TokenResponse passwordGrant(PasswordGrantRequest request) {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "password");
        form.add("client_id", clientRegistration.getClientId());
        form.add("client_secret", clientRegistration.getClientSecret());
        form.add("username", request.username());
        form.add("password", request.password());
        form.add("scope", "openid profile email");

        return restClient.post()
                .uri(clientRegistration.getProviderDetails().getTokenUri())
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(form)
                .retrieve()
                .onStatus(HttpStatusCode::is4xxClientError, (req, response) -> {
                    throw new BusinessException("AUTHZ-4011",
                            "error.login.invalid_credentials", "Keycloak rejected password grant for " + request.username());
                })
                .body(TokenResponse.class);
    }

    @Override
    public TokenResponse refreshGrant(RefreshGrantRequest request) {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "refresh_token");
        form.add("client_id", clientRegistration.getClientId());
        form.add("client_secret", clientRegistration.getClientSecret());
        form.add("refresh_token", request.refreshToken());

        return restClient.post()
                .uri(clientRegistration.getProviderDetails().getTokenUri())
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(form)
                .retrieve()
                .onStatus(HttpStatusCode::is4xxClientError, (req, response) -> {
                    throw new BusinessException("AUTHZ-4012",
                            "error.login.session_expired", "Keycloak rejected refresh_token grant");
                })
                .body(TokenResponse.class);
    }

    @Override
    public void endSession(RefreshGrantRequest request) {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("client_id", clientRegistration.getClientId());
        form.add("client_secret", clientRegistration.getClientSecret());
        form.add("refresh_token", request.refreshToken());

        restClient.post()
                .uri(endSessionEndpoint())
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(form)
                .retrieve()
                .toBodilessEntity();
    }

    private String endSessionEndpoint() {
        Object endpoint = clientRegistration.getProviderDetails().getConfigurationMetadata().get("end_session_endpoint");
        if (endpoint != null) {
            return endpoint.toString();
        }
        String tokenUri = clientRegistration.getProviderDetails().getTokenUri();
        return tokenUri.substring(0, tokenUri.lastIndexOf('/') + 1) + "logout";
    }
}
