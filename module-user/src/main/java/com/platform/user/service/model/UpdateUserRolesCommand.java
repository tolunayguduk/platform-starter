package com.platform.user.service.model;

import java.util.Set;

public record UpdateUserRolesCommand(String keycloakUserId, Set<String> roles, String currentAdminKeycloakUserId) {
}
