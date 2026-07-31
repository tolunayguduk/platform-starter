package com.platform.user.service;

import com.platform.error.BusinessException;
import com.platform.security.integration.keycloak.KeycloakAdminClient;
import com.platform.security.integration.keycloak.KeycloakTokenClient;
import com.platform.security.integration.keycloak.model.KeycloakUser;
import com.platform.security.integration.keycloak.model.PasswordGrantRequest;
import com.platform.security.integration.keycloak.model.ResetPasswordRequest;
import com.platform.security.integration.keycloak.model.UpdateKeycloakUserRequest;
import com.platform.user.entity.UserContact;
import com.platform.user.entity.UserProfile;
import com.platform.user.repository.UserConsentRepository;
import com.platform.user.repository.UserContactRepository;
import com.platform.user.repository.UserProfileRepository;
import com.platform.user.service.model.ChangePasswordCommand;
import com.platform.user.service.model.ProfileResult;
import com.platform.user.service.model.UpdateProfileCommand;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class MyProfileServiceImpl implements MyProfileService {

    private final UserProfileRepository userProfileRepository;
    private final UserContactRepository userContactRepository;
    private final UserConsentRepository userConsentRepository;
    private final KeycloakAdminClient keycloakAdminClient;
    private final KeycloakTokenClient keycloakTokenClient;

    public MyProfileServiceImpl(UserProfileRepository userProfileRepository,
                                 UserContactRepository userContactRepository,
                                 UserConsentRepository userConsentRepository,
                                 KeycloakAdminClient keycloakAdminClient,
                                 KeycloakTokenClient keycloakTokenClient) {
        this.userProfileRepository = userProfileRepository;
        this.userContactRepository = userContactRepository;
        this.userConsentRepository = userConsentRepository;
        this.keycloakAdminClient = keycloakAdminClient;
        this.keycloakTokenClient = keycloakTokenClient;
    }

    @Override
    public ProfileResult getProfile(String keycloakUserId) {
        KeycloakUser kcUser = keycloakAdminClient.getUser(keycloakUserId);
        UserProfile profile = userProfileRepository.findById(keycloakUserId).orElseGet(UserProfile::new);
        UserContact contact = userContactRepository.findById(keycloakUserId).orElseGet(UserContact::new);
        List<ProfileResult.ConsentResult> consents = userConsentRepository
                .findByKeycloakUserIdOrderByGrantedAtDesc(keycloakUserId).stream()
                .map(c -> new ProfileResult.ConsentResult(
                        c.getConsentType(), c.getLegalBasis(), c.getPurpose(), c.getGrantedAt(), c.getRevokedAt()))
                .toList();

        return new ProfileResult(
                kcUser.username(), kcUser.email(), kcUser.firstName(), kcUser.lastName(),
                profile.getBirthDate(), profile.getAvatarUrl(), profile.getLocale(),
                contact.getPhoneNumber(), contact.getAlternateEmail(), contact.getAddressLine(),
                contact.getCity(), contact.getCountry(), consents);
    }

    @Override
    @Transactional
    public ProfileResult updateProfile(String keycloakUserId, UpdateProfileCommand command) {
        validate(command);

        // Keycloak first (source of truth for identity) - if it rejects the update (e.g. duplicate
        // email), nothing local has changed yet.
        keycloakAdminClient.updateUser(keycloakUserId,
                new UpdateKeycloakUserRequest(command.email(), command.firstName(), command.lastName()));

        UserProfile profile = userProfileRepository.findById(keycloakUserId).orElseGet(() -> {
            UserProfile p = new UserProfile();
            p.setKeycloakUserId(keycloakUserId);
            return p;
        });
        profile.setFullName((command.firstName() + " " + command.lastName()).trim());
        profile.setBirthDate(command.birthDate());
        profile.setAvatarUrl(command.avatarUrl());
        profile.setLocale(command.locale());
        userProfileRepository.save(profile);

        UserContact contact = userContactRepository.findById(keycloakUserId).orElseGet(() -> {
            UserContact c = new UserContact();
            c.setKeycloakUserId(keycloakUserId);
            return c;
        });
        contact.setPhoneNumber(command.phoneNumber());
        contact.setAlternateEmail(command.alternateEmail());
        contact.setAddressLine(command.addressLine());
        contact.setCity(command.city());
        contact.setCountry(command.country());
        userContactRepository.save(contact);

        return getProfile(keycloakUserId);
    }

    @Override
    public void changePassword(String keycloakUserId, String username, ChangePasswordCommand command) {
        if (!command.newPassword().equals(command.confirmNewPassword())) {
            throw new BusinessException("COMMON-4004", "error.password.mismatch", "New password/confirmation mismatch");
        }
        if (command.newPassword().length() < 8) {
            throw new BusinessException("COMMON-4003", "error.password.too_short", "Password shorter than 8 characters");
        }
        // A valid Bearer token alone must not be enough to change the credential - re-verify the
        // current password directly against Keycloak first.
        try {
            keycloakTokenClient.passwordGrant(new PasswordGrantRequest(username, command.currentPassword()));
        } catch (BusinessException e) {
            throw new BusinessException("AUTHZ-4013",
                    "error.password.current_incorrect", "Current password verification failed for " + username);
        }
        keycloakAdminClient.resetPassword(keycloakUserId, new ResetPasswordRequest(command.newPassword()));
    }

    private void validate(UpdateProfileCommand command) {
        if (isBlank(command.firstName()) || isBlank(command.lastName()) || isBlank(command.email())) {
            throw new BusinessException("COMMON-4001", "error.profile.missing_fields", "Required field missing");
        }
        if (!command.email().contains("@")) {
            throw new BusinessException("COMMON-4002", "error.profile.invalid_email", "Invalid email: " + command.email());
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
