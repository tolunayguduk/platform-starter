package com.platform.user.service;

import com.platform.user.service.model.OrganizationMemberSummaryResult;
import com.platform.user.service.model.OrganizationProfileResult;
import com.platform.user.service.model.OrganizationSearchResult;

import java.util.List;

/**
 * Public organization browsing - unlike AdminOrganizationService (admin-panel only, scoped to
 * what the caller manages), every method here is open to any authenticated user: finding and
 * viewing an organization's landing page has nothing to do with admin-panel access, it's how a
 * plain user discovers an organization to request joining it.
 */
public interface OrganizationDirectoryService {

    /** Case-insensitive substring match on name, capped to a reasonable result count - backs the
     * navbar search box. Blank query returns no results (never a "browse everything" listing). */
    List<OrganizationSearchResult> search(String query);

    /** The full landing page for one organization - throws a clean business error if it doesn't
     * exist (same as AuthService.getOrganizationName). */
    OrganizationProfileResult getProfile(String organizationId, String callerKeycloakUserId);

    /** Every member of this organization, lightly - backs the landing page's member-list popup.
     * Public to any authenticated visitor, unlike AdminOrganizationService.getOrganizationMembers
     * (admin-panel only, full AdminUserResult with email/roles/status). */
    List<OrganizationMemberSummaryResult> listMembers(String organizationId);
}
