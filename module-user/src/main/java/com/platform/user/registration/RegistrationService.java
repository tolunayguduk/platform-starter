package com.platform.user.registration;

import com.platform.error.TechnicalException;
import com.platform.security.KeycloakAdminClient;
import com.platform.user.identity.UserCore;
import com.platform.user.identity.UserCoreRepository;
import com.platform.user.profile.UserConsent;
import com.platform.user.profile.UserConsentRepository;
import com.platform.user.profile.UserProfile;
import com.platform.user.profile.UserProfileRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

/**
 * Keycloak stays the source of truth for credentials (see UserCore's javadoc), so registration
 * creates the user there first, then mirrors identity/profile/consent locally. If the local part
 * fails, the Keycloak user is deleted to compensate rather than leaving an orphaned account.
 */
@Service
public class RegistrationService {

    private static final Logger log = LoggerFactory.getLogger(RegistrationService.class);

    private final KeycloakAdminClient keycloakAdminClient;
    private final UserCoreRepository userCoreRepository;
    private final UserProfileRepository userProfileRepository;
    private final UserConsentRepository userConsentRepository;

    public RegistrationService(KeycloakAdminClient keycloakAdminClient,
                                UserCoreRepository userCoreRepository,
                                UserProfileRepository userProfileRepository,
                                UserConsentRepository userConsentRepository) {
        this.keycloakAdminClient = keycloakAdminClient;
        this.userCoreRepository = userCoreRepository;
        this.userProfileRepository = userProfileRepository;
        this.userConsentRepository = userConsentRepository;
    }

    @Transactional
    public void register(RegistrationRequest request, String clientIp) {
        // Keycloak's declarative user profile requires firstName AND lastName by default - a blank
        // lastName leaves the account "not fully set up", which rejects even a correct password at
        // login. Both are mandatory fields on the registration form, never derived by splitting a
        // single "full name" string (that heuristic silently produced a blank lastName for anyone
        // with a one-word name).
        String keycloakUserId = keycloakAdminClient.createUser(
                request.username(), request.email(), request.password(), request.firstName(), request.lastName());
        try {
            // Keycloak's realm_access.roles JWT claim does NOT expand the "default-roles-<realm>"
            // composite it auto-assigns on user creation - USER never actually appears in a
            // token unless granted directly, so the permission matrix would never match it.
            keycloakAdminClient.assignRealmRole(keycloakUserId, "USER");

            UserCore userCore = new UserCore();
            userCore.setKeycloakUserId(keycloakUserId);
            userCore.setUsername(request.username());
            userCore.setEmail(request.email());
            userCoreRepository.save(userCore);

            UserProfile profile = new UserProfile();
            profile.setUserId(userCore.getId());
            profile.setFullName(request.firstName() + " " + request.lastName());
            userProfileRepository.save(profile);

            UserConsent consent = new UserConsent();
            consent.setUserId(userCore.getId());
            consent.setConsentType("TERMS_OF_SERVICE");
            consent.setLegalBasis("CONTRACT");
            consent.setPurpose("Hesap oluşturma ve kullanım şartlarının kabulü");
            consent.setGrantedAt(Instant.now());
            consent.setIpAddress(clientIp);
            userConsentRepository.save(consent);
        } catch (RuntimeException e) {
            try {
                keycloakAdminClient.deleteUser(keycloakUserId);
            } catch (RuntimeException cleanupFailure) {
                log.error("Failed to compensate Keycloak user {} after local registration failure", keycloakUserId, cleanupFailure);
            }
            throw new TechnicalException("USER-5001",
                    "Local registration rows failed after Keycloak user creation for " + keycloakUserId, e);
        }
    }
}
