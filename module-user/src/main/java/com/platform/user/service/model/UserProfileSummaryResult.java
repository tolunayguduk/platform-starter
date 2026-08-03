package com.platform.user.service.model;

import java.util.List;

/**
 * A user's public profile page - deliberately much lighter than ProfileResult (the self-only
 * one): no email, phone, address, birth date or consents. Anyone authenticated can view anyone
 * else's username/display name/avatar and which organizations they belong to (already visible
 * anyway via each organization's own member-list popup - this is just the same fact from the
 * other direction), nothing more.
 */
public record UserProfileSummaryResult(
        String keycloakUserId, String username, String fullName, String avatarUrl,
        List<OrganizationSearchResult> organizations) {
}
