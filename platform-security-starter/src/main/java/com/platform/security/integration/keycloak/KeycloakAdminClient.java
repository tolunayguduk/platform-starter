package com.platform.security.integration.keycloak;

import com.platform.security.integration.keycloak.model.AdminEvent;
import com.platform.security.integration.keycloak.model.CreateKeycloakUserRequest;
import com.platform.security.integration.keycloak.model.KeycloakUser;
import com.platform.security.integration.keycloak.model.KeycloakUserSummary;
import com.platform.security.integration.keycloak.model.ResetPasswordRequest;
import com.platform.security.integration.keycloak.model.UpdateKeycloakUserRequest;

import java.util.List;

/**
 * Thin wrapper over the Keycloak Admin REST API, authenticated via the "keycloak-admin"
 * client-credentials registration (service account with the realm-management "manage-users"
 * role - see docker/keycloak/realm-platform.json). Keycloak stays the source of truth for
 * credentials and role assignment; this application never mirrors them.
 */
public interface KeycloakAdminClient {

    /** Creates the user in Keycloak and returns its "sub" (Keycloak user id). */
    String createUser(CreateKeycloakUserRequest request);

    /** Compensating action if a registration transaction fails after Keycloak user creation. */
    void deleteUser(String keycloakUserId);

    KeycloakUser getUser(String keycloakUserId);

    /** Partial update (only email/firstName/lastName are ever touched by this app). */
    void updateUser(String keycloakUserId, UpdateKeycloakUserRequest request);

    /** Admin panel identity edit - the only place username is ever changed by this app. */
    void updateUserIdentity(String keycloakUserId, String username, String email);

    /** Enables/disables the account directly in Keycloak - a disabled user cannot obtain a token. */
    void setUserEnabled(String keycloakUserId, boolean enabled);

    /** Sets a new permanent (non-temporary) password directly - caller is responsible for verifying the old one first. */
    void resetPassword(String keycloakUserId, ResetPasswordRequest request);

    /**
     * Grants a realm role directly. Needed because Keycloak's auto-assigned
     * "default-roles-&lt;realm&gt;" composite (which normally covers "every new user gets X") is
     * never expanded into the realm_access.roles JWT claim - the permission matrix (keyed by
     * plain role name) would never see it.
     */
    void assignRealmRole(String keycloakUserId, String roleName);

    /** Revokes a realm role directly - the counterpart to assignRealmRole. */
    void removeRealmRole(String keycloakUserId, String roleName);

    /** All user ids currently holding a given realm role. */
    List<String> getUserIdsWithRole(String roleName);

    /** Every user in the realm - identity plus Keycloak's own bookkeeping fields (enabled,
     * createdTimestamp). There is no local user table left to query. */
    List<KeycloakUserSummary> listUsers();

    /** Every realm role this application manages - Keycloak is the only place role names are
     * defined, so the admin panel's editable-role set is always exactly what the realm has. */
    List<String> listRealmRoles();

    /** This user's admin-event history (identity edits, role-mapping changes) straight from
     * Keycloak's own event log - requires {@code adminEventsEnabled} on the realm. This is the
     * audit trail for Keycloak-owned data; nothing about it is ever cached locally. */
    List<AdminEvent> getUserAdminEvents(String keycloakUserId);
}
