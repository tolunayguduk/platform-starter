package com.platform.user.identity;

import java.util.List;

/** Read-only projection of "who is this Keycloak user in our system" for GET /api/me. */
public record CurrentUserView(String username, String email, String fullName, List<String> roles) {
}