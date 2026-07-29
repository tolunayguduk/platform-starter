package com.platform.security;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;

/**
 * Keycloak puts realm roles under the non-standard "realm_access.roles" claim rather than the
 * standard "scope" claim - shared by the resource-server JWT path and the password-grant login
 * path so both map roles to Spring GrantedAuthority (and MatrixPermissionEvaluator) the same way.
 */
final class KeycloakRoleMapper {

    private KeycloakRoleMapper() {
    }

    static Collection<GrantedAuthority> realmRoleAuthorities(Jwt jwt) {
        Collection<GrantedAuthority> authorities = new ArrayList<>();
        Map<String, Object> realmAccess = jwt.getClaim("realm_access");
        if (realmAccess != null && realmAccess.get("roles") instanceof Collection<?> roles) {
            roles.forEach(role -> authorities.add(new SimpleGrantedAuthority("ROLE_" + role.toString().toUpperCase())));
        }
        return authorities;
    }
}
