package com.platform.user.service;

import com.platform.user.service.model.CurrentUserResult;

import java.util.List;

public interface CurrentUserService {

    /**
     * Resolves the logged-in user's info for GET /api/me: identity fields (username/email) are
     * always read live from the JWT - Keycloak is the only source of truth for them, there is no
     * local mirror anymore - plus fullName from the UserProfile GDPR category, if one exists.
     */
    CurrentUserResult getCurrentUser(String keycloakUserId, String username, String email, List<String> roles);
}
