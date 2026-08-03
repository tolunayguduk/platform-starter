package com.platform.user.service;

import com.platform.user.service.model.OrganizationMembershipRequestResult;

import java.util.List;

/**
 * Self-service side of organization membership - always scoped to the caller's own JWT subject,
 * same "never gated by AdminAccessGuard" spirit as MyProfileService (an ordinary user with no
 * admin-panel access at all still needs to accept/decline invites and request to join). The
 * admin-facing side (inviting someone, approving a join request) is AdminOrganizationService.
 */
public interface OrganizationMembershipService {

    /** Pending INVITE-type requests addressed to this user. */
    List<OrganizationMembershipRequestResult> listMyPendingInvites(String keycloakUserId);

    /** Grants real Keycloak group membership. Rejects if the request isn't a pending invite
     * addressed to this exact user. */
    void acceptInvite(Long requestId, String keycloakUserId);

    void declineInvite(Long requestId, String keycloakUserId);

    /** Self-service join via an organization's permanent invite link/code. Returns true if
     * membership was granted immediately (the organization's membershipRequiresApproval is off),
     * false if a pending request was created instead (its manager must approve it). */
    boolean requestToJoin(String organizationId, String keycloakUserId);
}
