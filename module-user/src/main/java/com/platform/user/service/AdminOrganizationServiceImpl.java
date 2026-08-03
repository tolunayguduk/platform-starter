package com.platform.user.service;

import com.platform.error.BusinessException;
import com.platform.security.integration.keycloak.KeycloakAdminClient;
import com.platform.security.integration.keycloak.model.KeycloakGroup;
import com.platform.security.integration.keycloak.model.KeycloakUserId;
import com.platform.security.integration.keycloak.model.KeycloakUserSummary;
import com.platform.user.constant.MembershipRequestStatus;
import com.platform.user.constant.MembershipRequestType;
import com.platform.user.entity.OrganizationManager;
import com.platform.user.entity.OrganizationMembershipRequest;
import com.platform.user.entity.UserProfile;
import com.platform.user.repository.OrganizationManagerRepository;
import com.platform.user.repository.OrganizationMembershipRequestRepository;
import com.platform.user.repository.UserProfileRepository;
import com.platform.user.service.model.AdminAccessScope;
import com.platform.user.service.model.AdminUserResult;
import com.platform.user.service.model.OrganizationMembershipRequestResult;
import com.platform.user.service.model.OrganizationResult;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

@Service
public class AdminOrganizationServiceImpl implements AdminOrganizationService {

    private final KeycloakAdminClient keycloakAdminClient;
    private final UserProfileRepository userProfileRepository;
    private final AdminAccessScopeService adminAccessScopeService;
    private final OrganizationMembershipRequestRepository membershipRequestRepository;
    private final OrganizationManagerRepository organizationManagerRepository;

    public AdminOrganizationServiceImpl(KeycloakAdminClient keycloakAdminClient, UserProfileRepository userProfileRepository,
                                         AdminAccessScopeService adminAccessScopeService,
                                         OrganizationMembershipRequestRepository membershipRequestRepository,
                                         OrganizationManagerRepository organizationManagerRepository) {
        this.keycloakAdminClient = keycloakAdminClient;
        this.userProfileRepository = userProfileRepository;
        this.adminAccessScopeService = adminAccessScopeService;
        this.membershipRequestRepository = membershipRequestRepository;
        this.organizationManagerRepository = organizationManagerRepository;
    }

    @Override
    public List<OrganizationResult> listOrganizations(String callerKeycloakUserId) {
        AdminAccessScope scope = adminAccessScopeService.resolve(callerKeycloakUserId);
        // Not every organization the caller happens to be a member of - only the ones they
        // actually manage (scope.organizationGroupIds() is already exactly that set).
        List<KeycloakGroup> groups = scope.platformScoped()
                ? keycloakAdminClient.listGroups()
                : scope.organizationGroupIds().stream().map(keycloakAdminClient::getGroup).toList();
        return groups.stream()
                .map(g -> new OrganizationResult(g.id(), g.name(), g.description(), g.coverImageUrl(), g.logoImageUrl(),
                        keycloakAdminClient.getGroupMembers(g.id()).size(), g.membershipRequiresApproval()))
                .toList();
    }

    @Override
    public OrganizationResult createOrganization(String name, String callerKeycloakUserId) {
        adminAccessScopeService.requirePlatformScope(callerKeycloakUserId);
        if (name == null || name.isBlank()) {
            throw new BusinessException("COMMON-4001", "error.profile.missing_fields", "Organization name required");
        }
        KeycloakGroup group = keycloakAdminClient.createGroup(name.trim());
        return new OrganizationResult(group.id(), group.name(), group.description(), group.coverImageUrl(), group.logoImageUrl(),
                0, group.membershipRequiresApproval());
    }

    @Override
    public void updateOrganizationDescription(String organizationId, String description, String callerKeycloakUserId) {
        assertCanAccessOrganization(callerKeycloakUserId, organizationId);
        keycloakAdminClient.updateGroupDescription(organizationId, description);
    }

    @Override
    public void renameOrganization(String organizationId, String name, String callerKeycloakUserId) {
        assertCanAccessOrganization(callerKeycloakUserId, organizationId);
        if (name == null || name.isBlank()) {
            throw new BusinessException("COMMON-4001", "error.profile.missing_fields", "Organization name required");
        }
        keycloakAdminClient.renameGroup(organizationId, name.trim());
    }

    @Override
    public void updateOrganizationImages(String organizationId, String coverImageUrl, String logoImageUrl, String callerKeycloakUserId) {
        assertCanAccessOrganization(callerKeycloakUserId, organizationId);
        keycloakAdminClient.updateGroupImages(organizationId, coverImageUrl, logoImageUrl);
    }

    @Override
    public void updateMembershipApprovalSetting(String organizationId, boolean requiresApproval, String callerKeycloakUserId) {
        assertCanAccessOrganization(callerKeycloakUserId, organizationId);
        keycloakAdminClient.updateGroupMembershipApproval(organizationId, requiresApproval);
    }

    @Override
    public void deleteOrganization(String organizationId, String callerKeycloakUserId) {
        adminAccessScopeService.requirePlatformScope(callerKeycloakUserId);
        keycloakAdminClient.deleteGroup(organizationId);
    }

    @Override
    public List<AdminUserResult> getOrganizationMembers(String organizationId, String callerKeycloakUserId) {
        assertCanAccessOrganization(callerKeycloakUserId, organizationId);
        Set<String> memberIds = keycloakAdminClient.getGroupMembers(organizationId).stream()
                .map(KeycloakUserId::id)
                .collect(Collectors.toSet());

        Map<String, Set<String>> rolesByKeycloakUserId = new HashMap<>();
        for (String role : keycloakAdminClient.listRealmRoles()) {
            for (String keycloakUserId : keycloakAdminClient.getUserIdsWithRole(role)) {
                if (memberIds.contains(keycloakUserId)) {
                    rolesByKeycloakUserId.computeIfAbsent(keycloakUserId, k -> new TreeSet<>()).add(role);
                }
            }
        }

        return keycloakAdminClient.listUsers().stream()
                .filter(u -> memberIds.contains(u.id()))
                .map(kcUser -> toResult(kcUser, rolesByKeycloakUserId.getOrDefault(kcUser.id(), Set.of())))
                .toList();
    }

    @Override
    public AdminUserResult findUserByIdentifier(String usernameOrEmail) {
        if (usernameOrEmail == null || usernameOrEmail.isBlank()) {
            throw new BusinessException("COMMON-4001", "error.profile.missing_fields", "Username or email required");
        }
        String needle = usernameOrEmail.trim();
        KeycloakUserSummary match = keycloakAdminClient.listUsers().stream()
                .filter(u -> u.username().equalsIgnoreCase(needle) || u.email().equalsIgnoreCase(needle))
                .findFirst()
                .orElseThrow(() -> new BusinessException("ADMIN-4041", "error.admin.user_not_found",
                        "No user matches: " + needle));
        return toResult(match, Set.of());
    }

    @Override
    @Transactional
    public void inviteMember(String organizationId, String keycloakUserId, String callerKeycloakUserId) {
        assertCanAccessOrganization(callerKeycloakUserId, organizationId);
        Set<String> memberIds = keycloakAdminClient.getGroupMembers(organizationId).stream()
                .map(KeycloakUserId::id)
                .collect(Collectors.toSet());
        if (memberIds.contains(keycloakUserId)) {
            throw new BusinessException("ADMIN-4009", "error.admin.already_member",
                    "User is already a member of this organization");
        }
        membershipRequestRepository.findByOrganizationIdAndKeycloakUserIdAndRequestTypeAndStatus(
                organizationId, keycloakUserId, MembershipRequestType.INVITE, MembershipRequestStatus.PENDING)
                .ifPresent(existing -> {
                    throw new BusinessException("ADMIN-4010", "error.admin.invite_already_pending",
                            "An invite is already pending for this user");
                });

        OrganizationMembershipRequest request = new OrganizationMembershipRequest();
        request.setOrganizationId(organizationId);
        request.setKeycloakUserId(keycloakUserId);
        request.setRequestType(MembershipRequestType.INVITE);
        request.setInitiatedByKeycloakUserId(callerKeycloakUserId);
        membershipRequestRepository.save(request);
    }

    @Override
    @Transactional
    public void removeMember(String organizationId, String keycloakUserId, String callerKeycloakUserId) {
        assertCanAccessOrganization(callerKeycloakUserId, organizationId);
        keycloakAdminClient.removeUserFromGroup(keycloakUserId, organizationId);
        organizationManagerRepository.deleteByOrganizationIdAndKeycloakUserId(organizationId, keycloakUserId);
    }

    @Override
    public List<AdminUserResult> listOrganizationManagers(String organizationId, String callerKeycloakUserId) {
        assertCanAccessOrganization(callerKeycloakUserId, organizationId);
        Set<String> managerIds = organizationManagerRepository.findByOrganizationId(organizationId).stream()
                .map(OrganizationManager::getKeycloakUserId)
                .collect(Collectors.toSet());
        return keycloakAdminClient.listUsers().stream()
                .filter(u -> managerIds.contains(u.id()))
                .map(kcUser -> toResult(kcUser, Set.of()))
                .toList();
    }

    @Override
    @Transactional
    public void addOrganizationManager(String organizationId, String targetKeycloakUserId, String callerKeycloakUserId) {
        assertCanAccessOrganization(callerKeycloakUserId, organizationId);
        Set<String> memberIds = keycloakAdminClient.getGroupMembers(organizationId).stream()
                .map(KeycloakUserId::id)
                .collect(Collectors.toSet());
        if (!memberIds.contains(targetKeycloakUserId)) {
            throw new BusinessException("ADMIN-4013", "error.admin.must_be_member_first",
                    "User must already be a member of the organization before becoming its manager");
        }
        if (organizationManagerRepository.existsByOrganizationIdAndKeycloakUserId(organizationId, targetKeycloakUserId)) {
            throw new BusinessException("ADMIN-4014", "error.admin.already_manager",
                    "User is already a manager of this organization");
        }
        OrganizationManager manager = new OrganizationManager();
        manager.setOrganizationId(organizationId);
        manager.setKeycloakUserId(targetKeycloakUserId);
        manager.setGrantedByKeycloakUserId(callerKeycloakUserId);
        organizationManagerRepository.save(manager);
    }

    @Override
    @Transactional
    public void removeOrganizationManager(String organizationId, String targetKeycloakUserId, String callerKeycloakUserId) {
        assertCanAccessOrganization(callerKeycloakUserId, organizationId);
        if (organizationManagerRepository.countByOrganizationId(organizationId) <= 1) {
            throw new BusinessException("ADMIN-4015", "error.admin.last_manager",
                    "Cannot remove the organization's last manager");
        }
        organizationManagerRepository.deleteByOrganizationIdAndKeycloakUserId(organizationId, targetKeycloakUserId);
    }

    @Override
    public List<OrganizationMembershipRequestResult> listPendingJoinRequests(String organizationId, String callerKeycloakUserId) {
        assertCanAccessOrganization(callerKeycloakUserId, organizationId);
        String organizationName = resolveOrganizationName(organizationId);
        Map<String, String> usernamesByUserId = keycloakAdminClient.listUsers().stream()
                .collect(Collectors.toMap(KeycloakUserSummary::id, KeycloakUserSummary::username));

        return membershipRequestRepository
                .findByOrganizationIdAndRequestTypeAndStatus(organizationId, MembershipRequestType.JOIN_REQUEST, MembershipRequestStatus.PENDING)
                .stream()
                .map(r -> new OrganizationMembershipRequestResult(
                        r.getId(), r.getOrganizationId(), organizationName,
                        r.getKeycloakUserId(), usernamesByUserId.getOrDefault(r.getKeycloakUserId(), r.getKeycloakUserId()),
                        r.getRequestType(), r.getCreatedAt()))
                .toList();
    }

    @Override
    @Transactional
    public void approveJoinRequest(Long requestId, String callerKeycloakUserId) {
        OrganizationMembershipRequest request = loadPendingJoinRequest(requestId);
        assertCanAccessOrganization(callerKeycloakUserId, request.getOrganizationId());
        request.setStatus(MembershipRequestStatus.APPROVED);
        request.setResolvedAt(Instant.now());
        membershipRequestRepository.save(request);
        keycloakAdminClient.addUserToGroup(request.getKeycloakUserId(), request.getOrganizationId());
    }

    @Override
    @Transactional
    public void rejectJoinRequest(Long requestId, String callerKeycloakUserId) {
        OrganizationMembershipRequest request = loadPendingJoinRequest(requestId);
        assertCanAccessOrganization(callerKeycloakUserId, request.getOrganizationId());
        request.setStatus(MembershipRequestStatus.REJECTED);
        request.setResolvedAt(Instant.now());
        membershipRequestRepository.save(request);
    }

    private OrganizationMembershipRequest loadPendingJoinRequest(Long requestId) {
        OrganizationMembershipRequest request = membershipRequestRepository.findById(requestId)
                .orElseThrow(() -> new BusinessException("ADMIN-4040", "error.admin.row_not_found", "No such request: " + requestId));
        if (request.getRequestType() != MembershipRequestType.JOIN_REQUEST || request.getStatus() != MembershipRequestStatus.PENDING) {
            throw new BusinessException("ADMIN-4011", "error.admin.request_not_pending", "This request is not a pending join request");
        }
        return request;
    }

    private String resolveOrganizationName(String organizationId) {
        return keycloakAdminClient.listGroups().stream()
                .filter(g -> g.id().equals(organizationId))
                .map(KeycloakGroup::name)
                .findFirst()
                .orElse(organizationId);
    }

    private AdminUserResult toResult(KeycloakUserSummary kcUser, Set<String> roles) {
        return new AdminUserResult(
                kcUser.id(),
                kcUser.username(),
                kcUser.email(),
                userProfileRepository.findById(kcUser.id()).map(UserProfile::getFullName).orElse(null),
                kcUser.enabled() ? "ACTIVE" : "DISABLED",
                Instant.ofEpochMilli(kcUser.createdTimestamp()),
                roles);
    }

    private void assertCanAccessOrganization(String callerKeycloakUserId, String organizationId) {
        AdminAccessScope scope = adminAccessScopeService.resolve(callerKeycloakUserId);
        if (scope.platformScoped()) {
            return;
        }
        if (!scope.organizationGroupIds().contains(organizationId)) {
            throw new BusinessException("USER-4007", "error.admin.outside_organization",
                    "Cannot manage a user outside your organization");
        }
    }
}
