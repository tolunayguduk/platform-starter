package com.platform.user.profile;

import com.platform.error.BusinessException;
import com.platform.error.TechnicalException;
import com.platform.security.KeycloakAdminClient;
import com.platform.security.KeycloakTokenClient;
import com.platform.user.identity.UserCore;
import com.platform.user.identity.UserCoreRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Backs the profile screen: identity fields (username/email/firstName/lastName) are read from
 * and written to Keycloak - the source of truth (see UserCore's javadoc) - while
 * UserProfile/UserContact/UserConsent are MySQL-only categories this app owns outright.
 */
@Service
public class MyProfileService {

    private final UserCoreRepository userCoreRepository;
    private final UserProfileRepository userProfileRepository;
    private final UserContactRepository userContactRepository;
    private final UserConsentRepository userConsentRepository;
    private final KeycloakAdminClient keycloakAdminClient;
    private final KeycloakTokenClient keycloakTokenClient;

    public MyProfileService(UserCoreRepository userCoreRepository,
                             UserProfileRepository userProfileRepository,
                             UserContactRepository userContactRepository,
                             UserConsentRepository userConsentRepository,
                             KeycloakAdminClient keycloakAdminClient,
                             KeycloakTokenClient keycloakTokenClient) {
        this.userCoreRepository = userCoreRepository;
        this.userProfileRepository = userProfileRepository;
        this.userContactRepository = userContactRepository;
        this.userConsentRepository = userConsentRepository;
        this.keycloakAdminClient = keycloakAdminClient;
        this.keycloakTokenClient = keycloakTokenClient;
    }

    public MyProfileView getProfile(String keycloakUserId) {
        UserCore core = findCore(keycloakUserId);
        KeycloakAdminClient.KeycloakUser kcUser = keycloakAdminClient.getUser(keycloakUserId);
        UserProfile profile = userProfileRepository.findById(core.getId()).orElseGet(UserProfile::new);
        UserContact contact = userContactRepository.findById(core.getId()).orElseGet(UserContact::new);
        List<MyProfileView.ConsentView> consents = userConsentRepository
                .findByUserIdOrderByGrantedAtDesc(core.getId()).stream()
                .map(c -> new MyProfileView.ConsentView(
                        c.getConsentType(), c.getLegalBasis(), c.getPurpose(), c.getGrantedAt(), c.getRevokedAt()))
                .toList();

        return new MyProfileView(
                kcUser.username(), kcUser.email(), kcUser.firstName(), kcUser.lastName(),
                profile.getBirthDate(), profile.getAvatarUrl(), profile.getLocale(),
                contact.getPhoneNumber(), contact.getAlternateEmail(), contact.getAddressLine(),
                contact.getCity(), contact.getCountry(), consents);
    }

    @Transactional
    public MyProfileView updateProfile(String keycloakUserId, UpdateMyProfileRequest request) {
        UserCore core = findCore(keycloakUserId);

        // Keycloak first (source of truth for identity) - if it rejects the update (e.g. duplicate
        // email), nothing local has changed yet.
        keycloakAdminClient.updateUser(keycloakUserId, request.email(), request.firstName(), request.lastName());

        core.setEmail(request.email());
        userCoreRepository.save(core);

        UserProfile profile = userProfileRepository.findById(core.getId()).orElseGet(() -> {
            UserProfile p = new UserProfile();
            p.setUserId(core.getId());
            return p;
        });
        profile.setFullName((request.firstName() + " " + request.lastName()).trim());
        profile.setBirthDate(request.birthDate());
        profile.setAvatarUrl(request.avatarUrl());
        profile.setLocale(request.locale());
        userProfileRepository.save(profile);

        UserContact contact = userContactRepository.findById(core.getId()).orElseGet(() -> {
            UserContact c = new UserContact();
            c.setUserId(core.getId());
            return c;
        });
        contact.setPhoneNumber(request.phoneNumber());
        contact.setAlternateEmail(request.alternateEmail());
        contact.setAddressLine(request.addressLine());
        contact.setCity(request.city());
        contact.setCountry(request.country());
        userContactRepository.save(contact);

        return getProfile(keycloakUserId);
    }

    public void changePassword(String keycloakUserId, String username, ChangePasswordRequest request) {
        if (!request.newPassword().equals(request.confirmNewPassword())) {
            throw new BusinessException("COMMON-4004", "error.password.mismatch", "New password/confirmation mismatch");
        }
        if (request.newPassword().length() < 8) {
            throw new BusinessException("COMMON-4003", "error.password.too_short", "Password shorter than 8 characters");
        }
        // A valid Bearer token alone must not be enough to change the credential - re-verify the
        // current password directly against Keycloak first.
        try {
            keycloakTokenClient.passwordGrant(username, request.currentPassword());
        } catch (BusinessException e) {
            throw new BusinessException("AUTHZ-4013",
                    "error.password.current_incorrect", "Current password verification failed for " + username);
        }
        keycloakAdminClient.resetPassword(keycloakUserId, request.newPassword());
    }

    private UserCore findCore(String keycloakUserId) {
        return userCoreRepository.findByKeycloakUserId(keycloakUserId)
                .orElseThrow(() -> new TechnicalException("USER-5002",
                        "No local UserCore mirror for keycloak user " + keycloakUserId));
    }
}