package com.platform.user.service;

import com.platform.user.service.model.RegisterUserCommand;

/**
 * Keycloak stays the source of truth for identity and credentials, so registration creates the
 * user there first, then writes only the MySQL-only GDPR categories this app owns outright
 * (profile, consent), keyed by the Keycloak user id.
 */
public interface RegistrationService {

    void register(RegisterUserCommand command);
}
