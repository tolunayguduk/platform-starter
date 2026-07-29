package com.platform.security;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.platform.error.BusinessException;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

/**
 * Direct (Resource Owner Password Credentials) grant against Keycloak's token endpoint, used
 * only by the React SPA's own login form (POST /api/auth/login, see AuthController) so the login
 * page can be fully custom instead of Keycloak's hosted one. ROPC is deprecated in OAuth 2.1 (the
 * password passes through this app's code, unlike the authorization-code flow) - kept
 * deliberately narrow: this is the only place a raw
 * password is ever handled, and it is never persisted, only forwarded to Keycloak and discarded.
 */
public class KeycloakTokenClient {

    private final RestClient restClient;
    private final ClientRegistration clientRegistration;

    public KeycloakTokenClient(RestClient.Builder builder, ClientRegistrationRepository clientRegistrationRepository) {
        this.clientRegistration = clientRegistrationRepository.findByRegistrationId("keycloak");
        this.restClient = builder.build();
    }

    public TokenResponse passwordGrant(String username, String password) {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "password");
        form.add("client_id", clientRegistration.getClientId());
        form.add("client_secret", clientRegistration.getClientSecret());
        form.add("username", username);
        form.add("password", password);
        form.add("scope", "openid profile email");

        return restClient.post()
                .uri(clientRegistration.getProviderDetails().getTokenUri())
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(form)
                .retrieve()
                .onStatus(HttpStatusCode::is4xxClientError, (request, response) -> {
                    throw new BusinessException("AUTHZ-4011",
                            "error.login.invalid_credentials", "Keycloak rejected password grant for " + username);
                })
                .body(TokenResponse.class);
    }

    /**
     * Exchanges a still-valid refresh token for a new access token, so the React SPA can renew
     * its session silently instead of forcing the user back through /api/auth/login.
     */
    public TokenResponse refreshGrant(String refreshToken) {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "refresh_token");
        form.add("client_id", clientRegistration.getClientId());
        form.add("client_secret", clientRegistration.getClientSecret());
        form.add("refresh_token", refreshToken);

        return restClient.post()
                .uri(clientRegistration.getProviderDetails().getTokenUri())
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(form)
                .retrieve()
                .onStatus(HttpStatusCode::is4xxClientError, (request, response) -> {
                    throw new BusinessException("AUTHZ-4012",
                            "error.login.session_expired", "Keycloak rejected refresh_token grant");
                })
                .body(TokenResponse.class);
    }

    /**
     * Server-side ("back-channel") session revocation - ends the Keycloak SSO session directly
     * via the refresh token, no browser redirect through Keycloak's own logout page needed.
     */
    public void endSession(String refreshToken) {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("client_id", clientRegistration.getClientId());
        form.add("client_secret", clientRegistration.getClientSecret());
        form.add("refresh_token", refreshToken);

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

    public record TokenResponse(
            @JsonProperty("access_token") String accessToken,
            @JsonProperty("id_token") String idToken,
            @JsonProperty("refresh_token") String refreshToken,
            @JsonProperty("expires_in") long expiresIn) {
    }
}
