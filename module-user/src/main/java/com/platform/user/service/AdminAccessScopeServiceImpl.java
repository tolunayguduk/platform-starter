package com.platform.user.service;

import com.platform.security.integration.keycloak.KeycloakAdminClient;
import com.platform.security.integration.keycloak.model.KeycloakGroup;
import com.platform.user.constant.RoleScope;
import com.platform.user.entity.RoleState;
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

    public AdminAccessScopeServiceImpl(KeycloakAdminClient keycloakAdminClient, RoleStateRepository roleStateRepository) {
        this.keycloakAdminClient = keycloakAdminClient;
        this.roleStateRepository = roleStateRepository;
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

        boolean organizationScoped = enabledScopes.contains(RoleScope.ORGANIZATION);
        if (!organizationScoped) {
            return new AdminAccessScope(false, false, Set.of());
        }

        Set<String> organizationGroupIds = keycloakAdminClient.getUserGroups(callerKeycloakUserId).stream()
                .map(KeycloakGroup::id)
                .collect(Collectors.toSet());
        return new AdminAccessScope(false, true, organizationGroupIds);
    }

    @Override
    public Set<String> resolveUserRoles(String keycloakUserId, List<String> managedRoles) {
        return managedRoles.stream()
                .filter(role -> keycloakAdminClient.getUserIdsWithRole(role).contains(keycloakUserId))
                .collect(Collectors.toSet());
    }
}
