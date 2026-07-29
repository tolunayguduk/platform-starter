package com.platform.user.admin;

import java.util.Set;

public record UpdateUserRolesRequest(Set<String> roles) {
}