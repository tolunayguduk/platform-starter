package com.platform.user.service;

import com.platform.user.service.model.UserProfileSummaryResult;

/**
 * Public user browsing - the counterpart to OrganizationDirectoryService, but for people: every
 * method here is open to any authenticated user, unlike MyProfileService (self-only, full detail
 * including email/phone/address/consents).
 */
public interface UserDirectoryService {

    /** Throws a clean business error if the user doesn't exist. */
    UserProfileSummaryResult getProfile(String keycloakUserId);
}
