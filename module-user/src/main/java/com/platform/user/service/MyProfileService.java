package com.platform.user.service;

import com.platform.user.service.model.ChangePasswordCommand;
import com.platform.user.service.model.ProfileResult;
import com.platform.user.service.model.UpdateProfileCommand;

/**
 * Backs the profile screen: identity fields (username/email/firstName/lastName) are read from
 * and written to Keycloak - the source of truth - while UserProfile/UserContact/UserConsent are
 * MySQL-only categories this app owns outright, keyed directly by the Keycloak user id.
 */
public interface MyProfileService {

    ProfileResult getProfile(String keycloakUserId);

    ProfileResult updateProfile(String keycloakUserId, UpdateProfileCommand command);

    void changePassword(String keycloakUserId, String username, ChangePasswordCommand command);
}
