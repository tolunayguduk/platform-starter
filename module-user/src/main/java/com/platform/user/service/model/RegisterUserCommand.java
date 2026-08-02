package com.platform.user.service.model;

/** organizationName is optional - blank/absent means "just register as a plain USER", present
 * means "create a new organization and become its admin" (see RegistrationServiceImpl). */
public record RegisterUserCommand(String username, String email, String password, String confirmPassword,
                                   String firstName, String lastName, Boolean termsAccepted, String clientIp,
                                   String organizationName) {
}
