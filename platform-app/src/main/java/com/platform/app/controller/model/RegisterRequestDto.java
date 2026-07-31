package com.platform.app.controller.model;

public record RegisterRequestDto(String username, String email, String password, String confirmPassword,
                                  String firstName, String lastName, Boolean termsAccepted) {
}
