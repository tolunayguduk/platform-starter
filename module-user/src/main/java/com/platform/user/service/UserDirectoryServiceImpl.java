package com.platform.user.service;

import com.platform.security.integration.keycloak.KeycloakAdminClient;
import com.platform.security.integration.keycloak.model.KeycloakUser;
import com.platform.user.entity.UserProfile;
import com.platform.user.repository.UserProfileRepository;
import com.platform.user.service.model.OrganizationSearchResult;
import com.platform.user.service.model.UserProfileSummaryResult;
import org.springframework.stereotype.Service;

@Service
public class UserDirectoryServiceImpl implements UserDirectoryService {

    private final KeycloakAdminClient keycloakAdminClient;
    private final UserProfileRepository userProfileRepository;

    public UserDirectoryServiceImpl(KeycloakAdminClient keycloakAdminClient, UserProfileRepository userProfileRepository) {
        this.keycloakAdminClient = keycloakAdminClient;
        this.userProfileRepository = userProfileRepository;
    }

    @Override
    public UserProfileSummaryResult getProfile(String keycloakUserId) {
        KeycloakUser kcUser = keycloakAdminClient.getUser(keycloakUserId); // throws a clean 404 if missing
        UserProfile profile = userProfileRepository.findById(keycloakUserId).orElseGet(UserProfile::new);

        var organizations = keycloakAdminClient.getUserGroups(keycloakUserId).stream()
                .map(g -> new OrganizationSearchResult(g.id(), g.name(), g.coverImageUrl(), g.logoImageUrl(),
                        keycloakAdminClient.getGroupMembers(g.id()).size()))
                .toList();

        return new UserProfileSummaryResult(keycloakUserId, kcUser.username(), profile.getFullName(), profile.getAvatarUrl(), organizations);
    }
}
