package com.platform.user.service;

import com.platform.user.service.model.AdminUserResult;
import com.platform.user.service.model.OrganizationMembershipRequestResult;
import com.platform.user.service.model.OrganizationResult;

import java.util.List;

/**
 * Organizations are Keycloak Groups - never a parallel local table (same
 * Keycloak-is-the-source-of-truth rule as roles/identity). Every method scopes itself through
 * AdminAccessScopeService: a PLATFORM-scoped caller sees/manages every organization; an
 * ORGANIZATION-scoped caller only their own (a user, and therefore an admin, may belong to more
 * than one) - never both, and never create/delete an organization at all (platform-only,
 * destructive/structural changes stay centrally controlled).
 *
 * <p>Membership is never granted unilaterally by a manager - see {@link #inviteMember}. Only the
 * target user accepting an invite (OrganizationMembershipService, self-service) or a manager
 * approving a self-service join request actually grants Keycloak group membership.
 */
public interface AdminOrganizationService {

    List<OrganizationResult> listOrganizations(String callerKeycloakUserId);

    OrganizationResult createOrganization(String name, String callerKeycloakUserId);

    /** Doubles as the organization's landing-page "About us" text. */
    void updateOrganizationDescription(String organizationId, String description, String callerKeycloakUserId);

    /** Rejects a duplicate name the same way createOrganization does. */
    void renameOrganization(String organizationId, String name, String callerKeycloakUserId);

    /** Landing-page cover photo and profile photo/logo - both plain URLs, same convention as a
     * user's own profile avatarUrl (no file storage in this app). Either may be null/blank to clear it. */
    void updateOrganizationImages(String organizationId, String coverImageUrl, String logoImageUrl, String callerKeycloakUserId);

    /** Whether a self-service join request against this organization needs a manager's approval,
     * or is granted immediately - see OrganizationMembershipService.requestToJoin. */
    void updateMembershipApprovalSetting(String organizationId, boolean requiresApproval, String callerKeycloakUserId);

    void deleteOrganization(String organizationId, String callerKeycloakUserId);

    List<AdminUserResult> getOrganizationMembers(String organizationId, String callerKeycloakUserId);

    /** Exact username/email match only, never a browse/search - lets an organization admin invite
     * a specific known person without exposing the full user directory to them (that would defeat
     * the whole point of isolating organizations from each other). */
    AdminUserResult findUserByIdentifier(String usernameOrEmail, String callerKeycloakUserId);

    /** Creates a PENDING invite - does not grant membership. The target user must accept it
     * themselves (OrganizationMembershipService.acceptInvite). Rejects if already a member or an
     * identical pending invite already exists. */
    void inviteMember(String organizationId, String keycloakUserId, String callerKeycloakUserId);

    /** Removing a member is unilateral - no consent needed, unlike adding one. */
    void removeMember(String organizationId, String keycloakUserId, String callerKeycloakUserId);

    /** Pending self-service join requests (not invites) targeting this organization. */
    List<OrganizationMembershipRequestResult> listPendingJoinRequests(String organizationId, String callerKeycloakUserId);

    /** Approves a pending JOIN_REQUEST - grants real Keycloak group membership. */
    void approveJoinRequest(Long requestId, String callerKeycloakUserId);

    void rejectJoinRequest(Long requestId, String callerKeycloakUserId);
}
