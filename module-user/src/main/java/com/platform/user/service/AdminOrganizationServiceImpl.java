package com.platform.user.service;

import com.platform.error.BusinessException;
import com.platform.security.integration.keycloak.KeycloakAdminClient;
import com.platform.security.integration.keycloak.model.KeycloakGroup;
import com.platform.security.integration.keycloak.model.KeycloakUserId;
import com.platform.security.integration.keycloak.model.KeycloakUserSummary;
import com.platform.user.entity.UserProfile;
import com.platform.user.repository.UserProfileRepository;
import com.platform.user.service.model.AdminAccessScope;
import com.platform.user.service.model.AdminUserResult;
import com.platform.user.service.model.OrganizationResult;
import org.springframework.stereotype.Service;

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

    public AdminOrganizationServiceImpl(KeycloakAdminClient keycloakAdminClient, UserProfileRepository userProfileRepository,
                                         AdminAccessScopeService adminAccessScopeService) {
        this.keycloakAdminClient = keycloakAdminClient;
        this.userProfileRepository = userProfileRepository;
        this.adminAccessScopeService = adminAccessScopeService;
    }

    @Override
    public List<OrganizationResult> listOrganizations(String callerKeycloakUserId) {
        AdminAccessScope scope = adminAccessScopeService.resolve(callerKeycloakUserId);
        List<KeycloakGroup> groups = scope.platformScoped()
                ? keycloakAdminClient.listGroups()
                : keycloakAdminClient.getUserGroups(callerKeycloakUserId);
        return groups.stream()
                .map(g -> new OrganizationResult(g.id(), g.name(), g.description(), keycloakAdminClient.getGroupMembers(g.id()).size()))
                .toList();
    }

    @Override
    public OrganizationResult createOrganization(String name, String callerKeycloakUserId) {
        requirePlatformScope(callerKeycloakUserId);
        if (name == null || name.isBlank()) {
            throw new BusinessException("COMMON-4001", "error.profile.missing_fields", "Organization name required");
        }
        KeycloakGroup group = keycloakAdminClient.createGroup(name.trim());
        return new OrganizationResult(group.id(), group.name(), group.description(), 0);
    }

    @Override
    public void updateOrganizationDescription(String organizationId, String description, String callerKeycloakUserId) {
        assertCanAccessOrganization(callerKeycloakUserId, organizationId);
        keycloakAdminClient.updateGroupDescription(organizationId, description);
    }

    @Override
    public void deleteOrganization(String organizationId, String callerKeycloakUserId) {
        requirePlatformScope(callerKeycloakUserId);
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
    public AdminUserResult findUserByIdentifier(String usernameOrEmail, String callerKeycloakUserId) {
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
    public void addMember(String organizationId, String keycloakUserId, String callerKeycloakUserId) {
        assertCanAccessOrganization(callerKeycloakUserId, organizationId);
        keycloakAdminClient.addUserToGroup(keycloakUserId, organizationId);
    }

    @Override
    public void removeMember(String organizationId, String keycloakUserId, String callerKeycloakUserId) {
        assertCanAccessOrganization(callerKeycloakUserId, organizationId);
        keycloakAdminClient.removeUserFromGroup(keycloakUserId, organizationId);
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

    private void requirePlatformScope(String callerKeycloakUserId) {
        if (!adminAccessScopeService.resolve(callerKeycloakUserId).platformScoped()) {
            throw new BusinessException("USER-4006", "error.admin.platform_scope_required",
                    "Only a platform-scope admin can perform this action");
        }
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
