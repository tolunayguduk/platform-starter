package com.platform.security;

import org.springframework.security.access.PermissionEvaluator;
import org.springframework.security.core.Authentication;

import java.io.Serializable;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Backs @PreAuthorize("hasPermission('report', 'list')").
 * Reads roles from the authenticated JWT, resolves them against RolePermissionLookupPort
 * (which module-user implements, and which is @Cacheable there - see platform-cache-spring-boot-starter).
 *
 * This class is intentionally NOT the security boundary for the UI permission map
 * (/api/me/ui-permissions) - that endpoint calls the same port for display purposes only.
 * This evaluator is what actually gates every protected method call.
 */
public class MatrixPermissionEvaluator implements PermissionEvaluator {

    private final RolePermissionLookupPort lookupPort;

    public MatrixPermissionEvaluator(RolePermissionLookupPort lookupPort) {
        this.lookupPort = lookupPort;
    }

    @Override
    public boolean hasPermission(Authentication authentication, Object targetDomainObject, Object permission) {
        // e.g. hasPermission('report', 'list') -> targetDomainObject="report", permission="list"
        if (authentication == null || !authentication.isAuthenticated() || targetDomainObject == null) {
            return false;
        }
        String requiredKey = targetDomainObject + ":" + permission;
        return resolveGrantedKeys(authentication).contains(requiredKey);
    }

    @Override
    public boolean hasPermission(Authentication authentication, Serializable targetId, String targetType, Object permission) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return false;
        }
        String requiredKey = targetType + ":" + permission;
        return resolveGrantedKeys(authentication).contains(requiredKey);
    }

    private Set<String> resolveGrantedKeys(Authentication authentication) {
        // GrantedAuthority values carry Spring Security's own "ROLE_" convention (see
        // KeycloakRoleMapper) - the permission matrix keys roles by their plain Keycloak name.
        Set<String> roleNames = authentication.getAuthorities().stream()
                .map(a -> a.getAuthority().replaceFirst("^ROLE_", ""))
                .collect(Collectors.toSet());
        return lookupPort.resolvePermissions(roleNames);
    }
}
