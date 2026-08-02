package com.platform.user.controller.model;

import com.platform.user.constant.RoleScope;

public record AdminRoleDto(String name, String description, boolean enabled, RoleScope scope) {
}
