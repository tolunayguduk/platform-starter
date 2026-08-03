package com.platform.user.service.model;

/** A member row for the landing page's member-list popup - deliberately lighter than
 * AdminUserResult (no email/roles/status): that's admin-panel-only data, this is shown to any
 * authenticated visitor of the organization's public landing page. */
public record OrganizationMemberSummaryResult(String keycloakUserId, String username, String fullName) {
}
