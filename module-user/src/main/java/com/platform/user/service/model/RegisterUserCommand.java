package com.platform.user.service.model;

public record RegisterUserCommand(String username, String email, String password, String confirmPassword,
                                   String firstName, String lastName, Boolean termsAccepted, String clientIp) {
}
