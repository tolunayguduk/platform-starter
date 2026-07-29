package com.platform.security;

import com.platform.error.BusinessException;
import com.platform.error.TechnicalException;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatusCode;
import org.springframework.security.oauth2.client.OAuth2AuthorizeRequest;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.web.client.RestClient;

import java.net.URI;
import java.util.List;
import java.util.Map;

/**
 * Thin wrapper over the Keycloak Admin REST API, authenticated via the "keycloak-admin"
 * client-credentials registration (service account with the realm-management "manage-users"
 * role - see docker/keycloak/realm-platform.json). Used to create users on registration;
 * Keycloak stays the source of truth for credentials, this app only mirrors identity fields.
 */
public class KeycloakAdminClient {

    /** Client-credentials grant has no real end user; this is just a stable cache key for OAuth2AuthorizedClientService. */
    private static final String PRINCIPAL_NAME = "keycloak-admin-client";

    private final RestClient restClient;

    public KeycloakAdminClient(RestClient.Builder builder, OAuth2AuthorizedClientManager authorizedClientManager,
                                String adminBaseUri) {
        this.restClient = builder
                .baseUrl(adminBaseUri)
                .requestInterceptor((request, body, execution) -> {
                    OAuth2AuthorizeRequest authorizeRequest = OAuth2AuthorizeRequest
                            .withClientRegistrationId("keycloak-admin")
                            .principal(PRINCIPAL_NAME)
                            .build();
                    OAuth2AuthorizedClient authorizedClient = authorizedClientManager.authorize(authorizeRequest);
                    if (authorizedClient == null) {
                        throw new TechnicalException("AUTHZ-4010", "Could not obtain a client-credentials token for keycloak-admin");
                    }
                    request.getHeaders().setBearerAuth(authorizedClient.getAccessToken().getTokenValue());
                    return execution.execute(request, body);
                })
                .build();
    }

    /** Creates the user in Keycloak and returns its "sub" (Keycloak user id). */
    public String createUser(String username, String email, String password, String firstName, String lastName) {
        Map<String, Object> body = Map.of(
                "username", username,
                "email", email,
                "firstName", firstName,
                "lastName", lastName,
                "enabled", true,
                "emailVerified", false,
                "credentials", List.of(Map.of(
                        "type", "password",
                        "value", password,
                        "temporary", false))
        );

        URI location = restClient.post()
                .uri("/users")
                .body(body)
                .retrieve()
                .onStatus(HttpStatusCode::is4xxClientError, (request, response) -> {
                    if (response.getStatusCode().value() == 409) {
                        throw new BusinessException("AUTHZ-4090",
                                "error.registration.duplicate", "Keycloak rejected duplicate username/email: " + username);
                    }
                    throw new TechnicalException("AUTHZ-4000",
                            "Keycloak admin API rejected user creation: HTTP " + response.getStatusCode());
                })
                .toBodilessEntity()
                .getHeaders()
                .getLocation();

        if (location == null) {
            throw new TechnicalException("AUTHZ-5000", "Keycloak did not return a Location header for the created user");
        }
        String path = location.getPath();
        return path.substring(path.lastIndexOf('/') + 1);
    }

    /** Compensating action if the local (Keycloak + DB) registration transaction fails after Keycloak user creation. */
    public void deleteUser(String keycloakUserId) {
        restClient.delete().uri("/users/{id}", keycloakUserId).retrieve().toBodilessEntity();
    }

    /** Keycloak is the source of truth for username/email/firstName/lastName - always read them live from here. */
    public KeycloakUser getUser(String keycloakUserId) {
        return restClient.get().uri("/users/{id}", keycloakUserId).retrieve().body(KeycloakUser.class);
    }

    /** Partial update (only username/email/firstName/lastName are ever touched by this app). */
    public void updateUser(String keycloakUserId, String email, String firstName, String lastName) {
        Map<String, Object> body = Map.of("email", email, "firstName", firstName, "lastName", lastName);
        restClient.put().uri("/users/{id}", keycloakUserId)
                .body(body)
                .retrieve()
                .onStatus(HttpStatusCode::is4xxClientError, (request, response) -> {
                    if (response.getStatusCode().value() == 409) {
                        throw new BusinessException("AUTHZ-4091",
                                "error.profile.email_taken", "Keycloak rejected update - email already in use: " + email);
                    }
                    throw new TechnicalException("AUTHZ-4001",
                            "Keycloak admin API rejected user update: HTTP " + response.getStatusCode());
                })
                .toBodilessEntity();
    }

    /** Sets a new permanent (non-temporary) password directly - caller is responsible for verifying the old one first. */
    public void resetPassword(String keycloakUserId, String newPassword) {
        Map<String, Object> body = Map.of("type", "password", "value", newPassword, "temporary", false);
        restClient.put().uri("/users/{id}/reset-password", keycloakUserId)
                .body(body)
                .retrieve()
                .onStatus(HttpStatusCode::is4xxClientError, (request, response) -> {
                    throw new TechnicalException("AUTHZ-4002",
                            "Keycloak admin API rejected password reset: HTTP " + response.getStatusCode());
                })
                .toBodilessEntity();
    }

    /**
     * Grants a realm role directly. Needed because Keycloak's auto-assigned
     * "default-roles-&lt;realm&gt;" composite (which normally covers "every new user gets X") is
     * never expanded into the realm_access.roles JWT claim - the permission matrix (keyed by
     * plain role name) would never see it, so registration must grant USER explicitly instead of
     * relying on that composite.
     */
    public void assignRealmRole(String keycloakUserId, String roleName) {
        RealmRole role = restClient.get().uri("/roles/{roleName}", roleName).retrieve().body(RealmRole.class);
        restClient.post().uri("/users/{id}/role-mappings/realm", keycloakUserId)
                .body(List.of(role))
                .retrieve()
                .onStatus(HttpStatusCode::is4xxClientError, (request, response) -> {
                    throw new TechnicalException("AUTHZ-4004",
                            "Keycloak admin API rejected role assignment of " + roleName + ": HTTP " + response.getStatusCode());
                })
                .toBodilessEntity();
    }

    /** Revokes a realm role directly - the counterpart to assignRealmRole, used by the admin panel. */
    public void removeRealmRole(String keycloakUserId, String roleName) {
        RealmRole role = restClient.get().uri("/roles/{roleName}", roleName).retrieve().body(RealmRole.class);
        restClient.method(HttpMethod.DELETE).uri("/users/{id}/role-mappings/realm", keycloakUserId)
                .body(List.of(role))
                .retrieve()
                .onStatus(HttpStatusCode::is4xxClientError, (request, response) -> {
                    throw new TechnicalException("AUTHZ-4005",
                            "Keycloak admin API rejected role removal of " + roleName + ": HTTP " + response.getStatusCode());
                })
                .toBodilessEntity();
    }

    /** All user ids currently holding a given realm role - the admin panel's user list uses this
     * (called once per managed role) instead of one role lookup per user. */
    public List<String> getUserIdsWithRole(String roleName) {
        List<KeycloakUserId> users = restClient.get().uri("/roles/{roleName}/users", roleName)
                .retrieve()
                .body(new ParameterizedTypeReference<List<KeycloakUserId>>() {
                });
        return users == null ? List.of() : users.stream().map(KeycloakUserId::id).toList();
    }

    public record KeycloakUser(String username, String email, String firstName, String lastName) {
    }

    public record RealmRole(String id, String name) {
    }

    public record KeycloakUserId(String id) {
    }
}
