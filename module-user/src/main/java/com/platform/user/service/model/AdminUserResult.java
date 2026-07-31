package com.platform.user.service.model;

import java.time.Instant;
import java.util.Set;

public record AdminUserResult(String id, String username, String email, String fullName, String status,
                               Instant createdAt, Set<String> roles) {
}
