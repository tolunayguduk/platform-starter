package com.platform.user.controller.model;

import com.platform.user.constant.MembershipRequestType;

import java.time.Instant;

public record OrganizationMembershipRequestDto(
        Long id, String organizationId, String organizationName,
        String keycloakUserId, String username,
        MembershipRequestType requestType, Instant createdAt) {
}
