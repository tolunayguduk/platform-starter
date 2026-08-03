package com.platform.user.service.model;

public record UpdateUserIdentityCommand(String keycloakUserId, String username, String email, String callerKeycloakUserId) {
}
