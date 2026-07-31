package com.platform.user.controller.model;

import java.util.List;

/** Read-only projection of "who is this Keycloak user in our system" for GET /api/me. */
public record CurrentUserDto(String username, String email, String fullName, List<String> roles) {
}
