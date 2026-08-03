package com.platform.user.service.model;

/**
 * An organization's public landing page - viewable by any authenticated user, not just its own
 * members or an admin-panel-eligible caller. isMember/canEdit/hasPendingJoinRequest are all
 * resolved relative to the caller so the frontend never has to make a second call (or guess) to
 * decide which of "Join" / "Pending" / "Member" / "Edit profile" to show.
 */
public record OrganizationProfileResult(
        String id, String name, String description, String coverImageUrl, String logoImageUrl,
        int memberCount, boolean membershipRequiresApproval,
        boolean isMember, boolean canEdit, boolean hasPendingJoinRequest) {
}
