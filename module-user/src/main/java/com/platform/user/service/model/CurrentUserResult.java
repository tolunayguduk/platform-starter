package com.platform.user.service.model;

import java.util.List;

public record CurrentUserResult(String username, String email, String fullName, List<String> roles) {
}
