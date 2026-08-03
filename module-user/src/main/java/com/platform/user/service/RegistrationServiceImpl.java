package com.platform.user.service;

import com.platform.error.BusinessException;
import com.platform.error.TechnicalException;
import com.platform.security.integration.keycloak.KeycloakAdminClient;
import com.platform.security.integration.keycloak.model.CreateKeycloakUserRequest;
import com.platform.security.integration.keycloak.model.KeycloakGroup;
import com.platform.user.entity.OrganizationManager;
import com.platform.user.entity.UserConsent;
import com.platform.user.entity.UserProfile;
import com.platform.user.repository.OrganizationManagerRepository;
import com.platform.user.repository.UserConsentRepository;
import com.platform.user.repository.UserProfileRepository;
import com.platform.user.service.model.RegisterUserCommand;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
public class RegistrationServiceImpl implements RegistrationService {

    private static final Logger log = LoggerFactory.getLogger(RegistrationServiceImpl.class);

    /** Every self-registered user with no organization gets this - Keycloak's own
     * "default-roles-<realm>" composite doesn't expand into the JWT's realm_access.roles claim,
     * so it has to be assigned directly (see the comment further down). */
    private static final String DEFAULT_ROLE = "USER";

    private final KeycloakAdminClient keycloakAdminClient;
    private final UserProfileRepository userProfileRepository;
    private final UserConsentRepository userConsentRepository;
    private final OrganizationMembershipService organizationMembershipService;
    private final OrganizationManagerRepository organizationManagerRepository;

    public RegistrationServiceImpl(KeycloakAdminClient keycloakAdminClient,
                                    UserProfileRepository userProfileRepository,
                                    UserConsentRepository userConsentRepository,
                                    OrganizationMembershipService organizationMembershipService,
                                    OrganizationManagerRepository organizationManagerRepository) {
        this.keycloakAdminClient = keycloakAdminClient;
        this.userProfileRepository = userProfileRepository;
        this.userConsentRepository = userConsentRepository;
        this.organizationMembershipService = organizationMembershipService;
        this.organizationManagerRepository = organizationManagerRepository;
    }

    @Override
    @Transactional
    public void register(RegisterUserCommand command) {
        validate(command);

        // Keycloak's declarative user profile requires firstName AND lastName by default - a blank
        // lastName leaves the account "not fully set up", which rejects even a correct password at
        // login. Both are mandatory on the registration form, never derived by splitting a single
        // "full name" string (that heuristic silently produced a blank lastName for anyone with a
        // one-word name).
        String keycloakUserId;
        try {
            keycloakUserId = keycloakAdminClient.createUser(new CreateKeycloakUserRequest(
                    command.username(), command.email(), command.password(), command.firstName(), command.lastName()));
        } catch (BusinessException e) {
            throw new BusinessException("AUTHZ-4090", "error.register.already_exists",
                    "Username or email already registered: " + e.getMessage());
        }
        String organizationName = command.organizationName() == null ? null : command.organizationName().trim();
        boolean creatingOrganization = organizationName != null && !organizationName.isEmpty();
        String joinOrganizationId = command.joinOrganizationId() == null ? null : command.joinOrganizationId().trim();
        boolean joiningOrganization = joinOrganizationId != null && !joinOrganizationId.isEmpty();
        String createdGroupId = null;
        try {
            // Keycloak's realm_access.roles JWT claim does NOT expand the "default-roles-<realm>"
            // composite it auto-assigns on user creation - USER never actually appears in a token
            // unless granted directly, so the permission matrix would never match it. Everyone gets
            // it, including someone creating their own organization - organization-admin authority
            // comes from organization_manager below, never from a role (see AdminAccessScopeService).
            keycloakAdminClient.assignRealmRole(keycloakUserId, DEFAULT_ROLE);
            if (creatingOrganization) {
                KeycloakGroup group = keycloakAdminClient.createGroup(organizationName);
                createdGroupId = group.id();
                keycloakAdminClient.addUserToGroup(keycloakUserId, createdGroupId);

                OrganizationManager manager = new OrganizationManager();
                manager.setOrganizationId(createdGroupId);
                manager.setKeycloakUserId(keycloakUserId);
                manager.setGrantedByKeycloakUserId(keycloakUserId);
                organizationManagerRepository.save(manager);
            } else if (joiningOrganization) {
                // Joining isn't creating - always a plain USER, and always goes through the same
                // pending/immediate approval logic a post-registration join would (see
                // OrganizationMembershipService.requestToJoin). A bad/nonexistent organization id
                // surfaces as a clean BusinessException, caught below like everything else.
                organizationMembershipService.requestToJoin(joinOrganizationId, keycloakUserId);
            }

            UserProfile profile = new UserProfile();
            profile.setKeycloakUserId(keycloakUserId);
            profile.setFullName(command.firstName() + " " + command.lastName());
            userProfileRepository.save(profile);

            UserConsent consent = new UserConsent();
            consent.setKeycloakUserId(keycloakUserId);
            consent.setConsentType("TERMS_OF_SERVICE");
            consent.setLegalBasis("CONTRACT");
            consent.setPurpose("Hesap oluşturma ve kullanım şartlarının kabulü");
            consent.setGrantedAt(Instant.now());
            consent.setIpAddress(command.clientIp());
            userConsentRepository.save(consent);
        } catch (BusinessException e) {
            // e.g. the chosen organization name is already taken (AUTHZ-4094) - a genuine,
            // expected rejection, not a technical failure. Still rolls back the user (no dangling
            // unclaimed account), but the caller sees the real reason, not a generic 500.
            cleanupAfterFailure(keycloakUserId, createdGroupId);
            throw e;
        } catch (RuntimeException e) {
            cleanupAfterFailure(keycloakUserId, createdGroupId);
            throw new TechnicalException("USER-5001",
                    "Local registration rows failed after Keycloak user creation for " + keycloakUserId, e);
        }
    }

    private void cleanupAfterFailure(String keycloakUserId, String createdGroupId) {
        if (createdGroupId != null) {
            try {
                keycloakAdminClient.deleteGroup(createdGroupId);
            } catch (RuntimeException cleanupFailure) {
                log.error("Failed to compensate Keycloak group {} after local registration failure", createdGroupId, cleanupFailure);
            }
        }
        try {
            keycloakAdminClient.deleteUser(keycloakUserId);
        } catch (RuntimeException cleanupFailure) {
            log.error("Failed to compensate Keycloak user {} after local registration failure", keycloakUserId, cleanupFailure);
        }
    }

    private void validate(RegisterUserCommand command) {
        if (isBlank(command.username()) || isBlank(command.email()) || isBlank(command.password())
                || isBlank(command.firstName()) || isBlank(command.lastName())) {
            throw new BusinessException("COMMON-4001", "error.register.missing_fields", "Required field missing");
        }
        if (!command.email().contains("@")) {
            throw new BusinessException("COMMON-4002", "error.register.invalid_email", "Invalid email: " + command.email());
        }
        if (command.password().length() < 8) {
            throw new BusinessException("COMMON-4003", "error.register.password_too_short", "Password shorter than 8 characters");
        }
        if (!command.password().equals(command.confirmPassword())) {
            throw new BusinessException("COMMON-4004", "error.register.password_mismatch", "Password/confirmation mismatch");
        }
        if (command.termsAccepted() == null || !command.termsAccepted()) {
            throw new BusinessException("COMMON-4005", "error.register.terms_not_accepted", "Terms of service not accepted");
        }
        boolean hasOrganizationName = command.organizationName() != null && !command.organizationName().isBlank();
        boolean hasJoinOrganizationId = command.joinOrganizationId() != null && !command.joinOrganizationId().isBlank();
        if (hasOrganizationName && hasJoinOrganizationId) {
            throw new BusinessException("COMMON-4006", "error.register.organization_conflict",
                    "Cannot both create a new organization and join an existing one");
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
