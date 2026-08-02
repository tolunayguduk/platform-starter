package com.platform.user.service.model;

import com.platform.user.constant.RoleScope;

public record AdminRoleResult(String name, String description, boolean enabled, RoleScope scope) {
}
