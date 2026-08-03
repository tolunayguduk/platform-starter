package com.platform.user.service;

import com.platform.error.BusinessException;
import com.platform.security.integration.keycloak.KeycloakAdminClient;
import com.platform.security.integration.keycloak.model.KeycloakUserId;
import com.platform.user.constant.RoleScope;
import com.platform.user.entity.OrganizationManager;
import com.platform.user.entity.RoleState;
import com.platform.user.repository.OrganizationManagerRepository;
import com.platform.user.repository.RoleStateRepository;
import com.platform.user.service.model.AdminAccessScope;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class AdminAccessScopeServiceImpl implements AdminAccessScopeService {

    private final KeycloakAdminClient keycloakAdminClient;
    private final RoleStateRepository roleStateRepository;
    private final OrganizationManagerRepository organizationManagerRepository;

    public AdminAccessScopeServiceImpl(KeycloakAdminClient keycloakAdminClient, RoleStateRepository roleStateRepository,
                                        OrganizationManagerRepository organizationManagerRepository) {
        this.keycloakAdminClient = keycloakAdminClient;
        this.roleStateRepository = roleStateRepository;
        this.organizationManagerRepository = organizationManagerRepository;
    }

    @Override
    public AdminAccessScope resolve(String callerKeycloakUserId) {
        Set<String> callerRoles = resolveUserRoles(callerKeycloakUserId, keycloakAdminClient.listRealmRoles());
        List<RoleScope> enabledScopes = roleStateRepository.findByRoleNameIn(callerRoles).stream()
                .filter(RoleState::isEnabled)
                .map(RoleState::getScope)
                .toList();

        boolean platformScoped = enabledScopes.contains(RoleScope.PLATFORM);
        if (platformScoped) {
            return new AdminAccessScope(true, false, Set.of());
        }

        // Organization-admin authority has nothing to do with roles or mere group membership - it
        // comes solely from being listed in organization_manager. A user who simply joins someone
        // else's organization as an ordinary member must never inherit management rights over it.
        Set<String> organizationGroupIds = organizationManagerRepository.findByKeycloakUserId(callerKeycloakUserId).stream()
                .map(OrganizationManager::getOrganizationId)
                .collect(Collectors.toSet());
        if (organizationGroupIds.isEmpty()) {
            return new AdminAccessScope(false, false, Set.of());
        }
        return new AdminAccessScope(false, true, organizationGroupIds);
    }

    @Override
    public void requirePlatformScope(String callerKeycloakUserId) {
        if (!resolve(callerKeycloakUserId).platformScoped()) {
            throw new BusinessException("USER-4006", "error.admin.platform_scope_required",
                    "Only a platform-scope admin can perform this action");
        }
    }

    @Override
    public Set<String> resolveUserRoles(String keycloakUserId, List<String> managedRoles) {
        // A single "which roles does this one user hold" call, intersected with managedRoles -
        // not one Keycloak round trip per realm role to ask "who holds role X" and check membership.
        Set<String> managedRoleSet = Set.copyOf(managedRoles);
        return keycloakAdminClient.getUserRoleNames(keycloakUserId).stream()
                .filter(managedRoleSet::contains)
                .collect(Collectors.toSet());
    }

    @Override
    public Set<String> resolveVisibleUserIds(AdminAccessScope callerScope) {
        return callerScope.organizationGroupIds().stream()
                .flatMap(groupId -> keycloakAdminClient.getGroupMembers(groupId).stream().map(KeycloakUserId::id))
                .collect(Collectors.toSet());
    }
}
