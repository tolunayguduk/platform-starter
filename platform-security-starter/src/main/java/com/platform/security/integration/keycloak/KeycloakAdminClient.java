package com.platform.security.integration.keycloak;

import com.platform.security.integration.keycloak.model.AdminEvent;
import com.platform.security.integration.keycloak.model.CreateKeycloakUserRequest;
import com.platform.security.integration.keycloak.model.KeycloakGroup;
import com.platform.security.integration.keycloak.model.KeycloakUser;
import com.platform.security.integration.keycloak.model.KeycloakUserId;
import com.platform.security.integration.keycloak.model.KeycloakUserSummary;
import com.platform.security.integration.keycloak.model.RealmRole;
import com.platform.security.integration.keycloak.model.ResetPasswordRequest;
import com.platform.security.integration.keycloak.model.UpdateKeycloakUserRequest;

import java.util.List;

/**
 * Thin wrapper over the Keycloak Admin REST API, authenticated via the "keycloak-admin"
 * client-credentials registration (service account with the realm-management "manage-users",
 * "manage-realm", "view-realm" and "view-events" roles - see docker/keycloak/realm-platform.json).
 * "manage-realm" is specifically what createRealmRole needs - Keycloak gates realm-level role
 * CRUD on it, separately from user management. Keycloak stays the source of truth for credentials
 * and role assignment; this application never mirrors them.
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

    /** Same set as listRealmRoles(), with each role's Keycloak-stored description too - backs the
     * admin panel's role table (name-only lookups elsewhere don't need the extra round trip cost). */
    List<RealmRole> listRealmRolesDetailed();

    /** Creates a new realm role - Keycloak stays the only place role names are defined, so a role
     * only becomes assignable/manageable once it exists here, never as local metadata. */
    void createRealmRole(String roleName);

    /** Updates a role's description - the only field on an existing role this app ever changes. */
    void updateRealmRoleDescription(String roleName, String description);

    /** Deletes a realm role entirely. Any local role_permission rows referencing it become the
     * caller's responsibility to clean up - this client has no knowledge of that table. */
    void deleteRealmRole(String roleName);

    /** This user's admin-event history (identity edits, role-mapping changes) straight from
     * Keycloak's own event log - requires {@code adminEventsEnabled} on the realm. This is the
     * audit trail for Keycloak-owned data; nothing about it is ever cached locally. */
    List<AdminEvent> getUserAdminEvents(String keycloakUserId);

    /** The realm's most recent admin events overall, not filtered to any one user - backs the
     * admin panel's "Recent Activity" feed. Newest first, capped to limit. */
    List<AdminEvent> getRecentAdminEvents(int limit);

    /** Top-level Keycloak Groups - the source of truth for "organizations" (see
     * [[feedback_keycloak_source_of_truth]]-style project convention: never a parallel local
     * table). One group = one organization; no nested sub-groups in this app. */
    List<KeycloakGroup> listGroups();

    /** Creates a new organization. Rejects a duplicate name the same way createUser rejects a
     * duplicate username (HTTP 409 from Keycloak). */
    KeycloakGroup createGroup(String name);

    /** A single organization by id - throws a clean business error if it doesn't exist (a
     * self-service join request can be given an arbitrary/garbage id by whoever pasted it). */
    KeycloakGroup getGroup(String groupId);

    /** Stored as a Keycloak group attribute (Groups have no native description field the way
     * Roles do). Doubles as the "About us" text on the organization's landing page. */
    void updateGroupDescription(String groupId, String description);

    /** Renames the organization - rejects a duplicate name the same way createGroup does. */
    void renameGroup(String groupId, String newName);

    /** Landing-page cover photo, stored as a group attribute (a URL, same convention as a user's
     * own profile avatarUrl - no file storage in this app, just a link). */
    void updateGroupCoverImage(String groupId, String coverImageUrl);

    /** Landing-page profile photo/logo, same URL-attribute convention as updateGroupCoverImage. */
    void updateGroupLogoImage(String groupId, String logoImageUrl);

    /** Whether a user self-requesting to join this organization needs a manager's approval, or
     * becomes a member immediately - see MembershipRequestType.JOIN_REQUEST. */
    void updateGroupMembershipApproval(String groupId, boolean requiresApproval);

    /** Deletes the organization entirely. Member users are simply no longer members of anything -
     * they are never deleted or otherwise touched. */
    void deleteGroup(String groupId);

    /** Every user in this organization - same minimal id-only shape as getUserIdsWithRole,
     * cross-referenced against listUsers()/getUser() by the caller for display fields. */
    List<KeycloakUserId> getGroupMembers(String groupId);

    /** Every organization this user belongs to - a user may belong to more than one. Used both
     * to resolve "which organization(s) does this admin themselves manage" and to show a user's
     * own organization membership in the admin panel. */
    List<KeycloakGroup> getUserGroups(String keycloakUserId);

    /** Adds a user to an organization. Idempotent in Keycloak - adding an existing member again
     * is a no-op, not an error. */
    void addUserToGroup(String keycloakUserId, String groupId);

    /** Removes a user from an organization - the counterpart to addUserToGroup. */
    void removeUserFromGroup(String keycloakUserId, String groupId);
}
