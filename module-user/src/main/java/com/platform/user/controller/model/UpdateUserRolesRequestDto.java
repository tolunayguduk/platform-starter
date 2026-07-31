package com.platform.user.controller.model;

import java.util.Set;

public record UpdateUserRolesRequestDto(Set<String> roles) {
}
