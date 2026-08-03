package com.platform.user.service.model;

/** organizationName and joinOrganizationId are both optional and mutually exclusive:
 * organizationName present -> creates a new organization, registrant becomes its admin;
 * joinOrganizationId present -> requests to join that existing organization as a plain user
 * (pending approval, or immediate, depending on that organization's own setting); neither ->
 * registers as a plain USER with no organization at all. See RegistrationServiceImpl. */
public record RegisterUserCommand(String username, String email, String password, String confirmPassword,
                                   String firstName, String lastName, Boolean termsAccepted, String clientIp,
                                   String organizationName, String joinOrganizationId) {
}
