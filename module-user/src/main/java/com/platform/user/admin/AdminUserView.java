package com.platform.user.admin;

import java.time.Instant;
import java.util.Set;

/** Row in the admin panel's user list - identity fields from the local mirror, roles from Keycloak. */
public record AdminUserView(
        String id,
        String username,
        String email,
        String fullName,
        String status,
        Instant createdAt,
        Set<String> roles) {
}