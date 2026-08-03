package com.platform.user.service;

import com.platform.security.integration.keycloak.KeycloakAdminClient;
import com.platform.security.integration.keycloak.model.KeycloakGroup;
import com.platform.security.integration.keycloak.model.KeycloakUserId;
import com.platform.user.constant.MembershipRequestStatus;
import com.platform.user.constant.MembershipRequestType;
import com.platform.user.entity.UserProfile;
import com.platform.user.repository.OrganizationMembershipRequestRepository;
import com.platform.user.repository.UserProfileRepository;
import com.platform.user.service.model.AdminAccessScope;
import com.platform.user.service.model.OrganizationMemberSummaryResult;
import com.platform.user.service.model.OrganizationProfileResult;
import com.platform.user.service.model.OrganizationSearchResult;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class OrganizationDirectoryServiceImpl implements OrganizationDirectoryService {

    private static final int SEARCH_RESULT_LIMIT = 20;

    private final KeycloakAdminClient keycloakAdminClient;
    private final AdminAccessScopeService adminAccessScopeService;
    private final OrganizationMembershipRequestRepository membershipRequestRepository;
    private final UserProfileRepository userProfileRepository;

    public OrganizationDirectoryServiceImpl(KeycloakAdminClient keycloakAdminClient, AdminAccessScopeService adminAccessScopeService,
                                             OrganizationMembershipRequestRepository membershipRequestRepository,
                                             UserProfileRepository userProfileRepository) {
        this.keycloakAdminClient = keycloakAdminClient;
        this.adminAccessScopeService = adminAccessScopeService;
        this.membershipRequestRepository = membershipRequestRepository;
        this.userProfileRepository = userProfileRepository;
    }

    @Override
    public List<OrganizationSearchResult> search(String query) {
        if (query == null || query.isBlank()) {
            return List.of();
        }
        String needle = query.trim().toLowerCase();
        return keycloakAdminClient.listGroups().stream()
                .filter(g -> g.name().toLowerCase().contains(needle))
                .limit(SEARCH_RESULT_LIMIT)
                .map(g -> new OrganizationSearchResult(g.id(), g.name(), g.coverImageUrl(), g.logoImageUrl(), keycloakAdminClient.getGroupMembers(g.id()).size()))
                .toList();
    }

    @Override
    public OrganizationProfileResult getProfile(String organizationId, String callerKeycloakUserId) {
        KeycloakGroup group = keycloakAdminClient.getGroup(organizationId);
        Set<String> memberIds = keycloakAdminClient.getGroupMembers(organizationId).stream()
                .map(KeycloakUserId::id)
                .collect(Collectors.toSet());
        boolean isMember = memberIds.contains(callerKeycloakUserId);

        AdminAccessScope callerScope = adminAccessScopeService.resolve(callerKeycloakUserId);
        boolean canEdit = callerScope.platformScoped() || callerScope.organizationGroupIds().contains(organizationId);

        boolean hasPendingJoinRequest = membershipRequestRepository
                .findByOrganizationIdAndKeycloakUserIdAndRequestTypeAndStatus(
                        organizationId, callerKeycloakUserId, MembershipRequestType.JOIN_REQUEST, MembershipRequestStatus.PENDING)
                .isPresent();

        return new OrganizationProfileResult(
                group.id(), group.name(), group.description(), group.coverImageUrl(), group.logoImageUrl(),
                memberIds.size(), group.membershipRequiresApproval(),
                isMember, canEdit, hasPendingJoinRequest);
    }

    @Override
    public List<OrganizationMemberSummaryResult> listMembers(String organizationId) {
        keycloakAdminClient.getGroup(organizationId); // throws a clean 404 if the id is garbage
        Set<String> memberIds = keycloakAdminClient.getGroupMembers(organizationId).stream()
                .map(KeycloakUserId::id)
                .collect(Collectors.toSet());
        return keycloakAdminClient.listUsers().stream()
                .filter(u -> memberIds.contains(u.id()))
                .map(u -> new OrganizationMemberSummaryResult(
                        u.id(), u.username(), userProfileRepository.findById(u.id()).map(UserProfile::getFullName).orElse(null)))
                .toList();
    }
}
