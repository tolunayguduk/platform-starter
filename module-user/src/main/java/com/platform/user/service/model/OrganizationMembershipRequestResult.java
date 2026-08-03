package com.platform.user.service.model;

import com.platform.user.constant.MembershipRequestType;

import java.time.Instant;

public record OrganizationMembershipRequestResult(
        Long id, String organizationId, String organizationName,
        String keycloakUserId, String username,
        MembershipRequestType requestType, Instant createdAt) {
}
