package com.platform.user.controller.model;

public record OrganizationProfileDto(
        String id, String name, String description, String coverImageUrl, String logoImageUrl,
        int memberCount, boolean membershipRequiresApproval,
        boolean isMember, boolean canEdit, boolean hasPendingJoinRequest) {
}
