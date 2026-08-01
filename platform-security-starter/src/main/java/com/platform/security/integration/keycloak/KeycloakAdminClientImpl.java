package com.platform.security.integration.keycloak;

import com.platform.error.BusinessException;
import com.platform.error.TechnicalException;
import com.platform.security.integration.keycloak.model.AdminEvent;
import com.platform.security.integration.keycloak.model.CreateKeycloakUserRequest;
import com.platform.security.integration.keycloak.model.KeycloakUser;
import com.platform.security.integration.keycloak.model.KeycloakUserId;
import com.platform.security.integration.keycloak.model.KeycloakUserSummary;
import com.platform.security.integration.keycloak.model.RealmRole;
import com.platform.security.integration.keycloak.model.ResetPasswordRequest;
import com.platform.security.integration.keycloak.model.UpdateKeycloakUserRequest;
import com.platform.security.util.KeycloakRoleMapper;
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

public class KeycloakAdminClientImpl implements KeycloakAdminClient {

    /** Client-credentials grant has no real end user; this is just a stable cache key for OAuth2AuthorizedClientService. */
    private static final String PRINCIPAL_NAME = "keycloak-admin-client";

    /** A single unpaginated page size for listUsers(). Fine for a starter/demo realm; a deployment
     * with more users than this would need real first/max pagination against the Keycloak API. */
    private static final int MAX_USERS_PAGE_SIZE = 1000;

    /** Same reasoning as MAX_USERS_PAGE_SIZE, applied to the admin-event log fetched per user. */
    private static final int MAX_ADMIN_EVENTS_PAGE_SIZE = 200;

    private final RestClient restClient;

    public KeycloakAdminClientImpl(RestClient.Builder builder, OAuth2AuthorizedClientManager authorizedClientManager,
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

    @Override
    public String createUser(CreateKeycloakUserRequest request) {
        Map<String, Object> body = Map.of(
                "username", request.username(),
                "email", request.email(),
                "firstName", request.firstName(),
                "lastName", request.lastName(),
                "enabled", true,
                "emailVerified", false,
                "credentials", List.of(Map.of(
                        "type", "password",
                        "value", request.password(),
                        "temporary", false))
        );

        URI location = restClient.post()
                .uri("/users")
                .body(body)
                .retrieve()
                .onStatus(HttpStatusCode::is4xxClientError, (req, response) -> {
                    if (response.getStatusCode().value() == 409) {
                        throw new BusinessException("AUTHZ-4090",
                                "error.registration.duplicate", "Keycloak rejected duplicate username/email: " + request.username());
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

    @Override
    public void deleteUser(String keycloakUserId) {
        restClient.delete().uri("/users/{id}", keycloakUserId).retrieve().toBodilessEntity();
    }

    @Override
    public KeycloakUser getUser(String keycloakUserId) {
        return restClient.get().uri("/users/{id}", keycloakUserId).retrieve().body(KeycloakUser.class);
    }

    @Override
    public void updateUser(String keycloakUserId, UpdateKeycloakUserRequest request) {
        Map<String, Object> body = Map.of("email", request.email(), "firstName", request.firstName(), "lastName", request.lastName());
        restClient.put().uri("/users/{id}", keycloakUserId)
                .body(body)
                .retrieve()
                .onStatus(HttpStatusCode::is4xxClientError, (req, response) -> {
                    if (response.getStatusCode().value() == 409) {
                        throw new BusinessException("AUTHZ-4091",
                                "error.profile.email_taken", "Keycloak rejected update - email already in use: " + request.email());
                    }
                    throw new TechnicalException("AUTHZ-4001",
                            "Keycloak admin API rejected user update: HTTP " + response.getStatusCode());
                })
                .toBodilessEntity();
    }

    @Override
    public void updateUserIdentity(String keycloakUserId, String username, String email) {
        Map<String, Object> body = Map.of("username", username, "email", email);
        restClient.put().uri("/users/{id}", keycloakUserId)
                .body(body)
                .retrieve()
                .onStatus(HttpStatusCode::is4xxClientError, (req, response) -> {
                    if (response.getStatusCode().value() == 409) {
                        throw new BusinessException("AUTHZ-4092",
                                "error.admin.identity_taken", "Keycloak rejected update - username or email already in use: " + username);
                    }
                    throw new TechnicalException("AUTHZ-4003",
                            "Keycloak admin API rejected identity update: HTTP " + response.getStatusCode());
                })
                .toBodilessEntity();
    }

    @Override
    public void setUserEnabled(String keycloakUserId, boolean enabled) {
        restClient.put().uri("/users/{id}", keycloakUserId)
                .body(Map.of("enabled", enabled))
                .retrieve()
                .onStatus(HttpStatusCode::is4xxClientError, (req, response) -> {
                    throw new TechnicalException("AUTHZ-4006",
                            "Keycloak admin API rejected status update: HTTP " + response.getStatusCode());
                })
                .toBodilessEntity();
    }

    @Override
    public void resetPassword(String keycloakUserId, ResetPasswordRequest request) {
        Map<String, Object> body = Map.of("type", "password", "value", request.newPassword(), "temporary", false);
        restClient.put().uri("/users/{id}/reset-password", keycloakUserId)
                .body(body)
                .retrieve()
                .onStatus(HttpStatusCode::is4xxClientError, (req, response) -> {
                    throw new TechnicalException("AUTHZ-4002",
                            "Keycloak admin API rejected password reset: HTTP " + response.getStatusCode());
                })
                .toBodilessEntity();
    }

    @Override
    public void assignRealmRole(String keycloakUserId, String roleName) {
        RealmRole role = restClient.get().uri("/roles/{roleName}", roleName).retrieve().body(RealmRole.class);
        restClient.post().uri("/users/{id}/role-mappings/realm", keycloakUserId)
                .body(List.of(role))
                .retrieve()
                .onStatus(HttpStatusCode::is4xxClientError, (req, response) -> {
                    throw new TechnicalException("AUTHZ-4004",
                            "Keycloak admin API rejected role assignment of " + roleName + ": HTTP " + response.getStatusCode());
                })
                .toBodilessEntity();
    }

    @Override
    public void removeRealmRole(String keycloakUserId, String roleName) {
        RealmRole role = restClient.get().uri("/roles/{roleName}", roleName).retrieve().body(RealmRole.class);
        restClient.method(HttpMethod.DELETE).uri("/users/{id}/role-mappings/realm", keycloakUserId)
                .body(List.of(role))
                .retrieve()
                .onStatus(HttpStatusCode::is4xxClientError, (req, response) -> {
                    throw new TechnicalException("AUTHZ-4005",
                            "Keycloak admin API rejected role removal of " + roleName + ": HTTP " + response.getStatusCode());
                })
                .toBodilessEntity();
    }

    @Override
    public List<String> getUserIdsWithRole(String roleName) {
        List<KeycloakUserId> users = restClient.get().uri("/roles/{roleName}/users", roleName)
                .retrieve()
                .body(new ParameterizedTypeReference<List<KeycloakUserId>>() {
                });
        return users == null ? List.of() : users.stream().map(KeycloakUserId::id).toList();
    }

    @Override
    public List<KeycloakUserSummary> listUsers() {
        List<KeycloakUserSummary> users = restClient.get().uri("/users?max={max}", MAX_USERS_PAGE_SIZE)
                .retrieve()
                .body(new ParameterizedTypeReference<List<KeycloakUserSummary>>() {
                });
        return users == null ? List.of() : users;
    }

    @Override
    public List<String> listRealmRoles() {
        return listRealmRolesDetailed().stream().map(RealmRole::name).toList();
    }

    @Override
    public List<RealmRole> listRealmRolesDetailed() {
        List<RealmRole> roles = restClient.get().uri("/roles")
                .retrieve()
                .body(new ParameterizedTypeReference<List<RealmRole>>() {
                });
        if (roles == null) {
            return List.of();
        }
        return roles.stream()
                .filter(r -> KeycloakRoleMapper.isApplicationRole(r.name()))
                .toList();
    }

    @Override
    public void updateRealmRoleDescription(String roleName, String description) {
        restClient.put().uri("/roles/{roleName}", roleName)
                .body(Map.of("name", roleName, "description", description == null ? "" : description))
                .retrieve()
                .onStatus(HttpStatusCode::is4xxClientError, (req, response) -> {
                    throw new TechnicalException("AUTHZ-4009",
                            "Keycloak admin API rejected role update: HTTP " + response.getStatusCode());
                })
                .toBodilessEntity();
    }

    @Override
    public void createRealmRole(String roleName) {
        restClient.post().uri("/roles")
                .body(Map.of("name", roleName))
                .retrieve()
                .onStatus(HttpStatusCode::is4xxClientError, (req, response) -> {
                    if (response.getStatusCode().value() == 409) {
                        throw new BusinessException("AUTHZ-4093",
                                "error.admin.role_exists", "Keycloak rejected duplicate role name: " + roleName);
                    }
                    throw new TechnicalException("AUTHZ-4007",
                            "Keycloak admin API rejected role creation: HTTP " + response.getStatusCode());
                })
                .toBodilessEntity();
    }

    @Override
    public void deleteRealmRole(String roleName) {
        restClient.delete().uri("/roles/{roleName}", roleName)
                .retrieve()
                .onStatus(HttpStatusCode::is4xxClientError, (req, response) -> {
                    throw new TechnicalException("AUTHZ-4008",
                            "Keycloak admin API rejected role deletion of " + roleName + ": HTTP " + response.getStatusCode());
                })
                .toBodilessEntity();
    }

    @Override
    public List<AdminEvent> getUserAdminEvents(String keycloakUserId) {
        List<AdminEvent> events = restClient.get().uri("/admin-events?max={max}", MAX_ADMIN_EVENTS_PAGE_SIZE)
                .retrieve()
                .body(new ParameterizedTypeReference<List<AdminEvent>>() {
                });
        if (events == null) {
            return List.of();
        }
        String userPathPrefix = "users/" + keycloakUserId;
        return events.stream()
                .filter(e -> e.resourcePath() != null && e.resourcePath().startsWith(userPathPrefix))
                .sorted((a, b) -> Long.compare(b.time(), a.time()))
                .toList();
    }

    @Override
    public List<AdminEvent> getRecentAdminEvents(int limit) {
        List<AdminEvent> events = restClient.get().uri("/admin-events?max={max}", MAX_ADMIN_EVENTS_PAGE_SIZE)
                .retrieve()
                .body(new ParameterizedTypeReference<List<AdminEvent>>() {
                });
        if (events == null) {
            return List.of();
        }
        return events.stream()
                .sorted((a, b) -> Long.compare(b.time(), a.time()))
                .limit(limit)
                .toList();
    }
}
