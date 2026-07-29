package com.platform.user.registration;

/** Fields already validated by the caller (AuthController) - passwords match, terms accepted. */
public record RegistrationRequest(String username, String email, String password, String firstName, String lastName) {
}
