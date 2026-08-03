package com.platform.user.controller.model;

import java.util.List;

public record UserProfileSummaryDto(
        String keycloakUserId, String username, String fullName, String avatarUrl,
        List<OrganizationSearchResultDto> organizations) {
}
