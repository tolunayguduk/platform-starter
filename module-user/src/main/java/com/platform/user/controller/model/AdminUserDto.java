package com.platform.user.controller.model;

import java.time.Instant;
import java.util.Set;

/** Row in the admin panel's user list. */
public record AdminUserDto(
        String id,
        String username,
        String email,
        String fullName,
        String status,
        Instant createdAt,
        Set<String> roles) {
}
