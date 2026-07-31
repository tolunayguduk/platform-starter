package com.platform.security.util;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.Set;

/**
 * Keycloak puts realm roles under the non-standard "realm_access.roles" claim rather than the
 * standard "scope" claim - shared by the resource-server JWT path and the password-grant login
 * path so both map roles to Spring GrantedAuthority (and MatrixPermissionEvaluator) the same way.
 */
public final class KeycloakRoleMapper {

    /** Keycloak's own bookkeeping roles - present in every realm/token but never a real
     * application role (JWT-derived role lists, the admin panel's assignable-role list, etc.). */
    private static final Set<String> BOOKKEEPING_ROLES = Set.of("offline_access", "uma_authorization");

    private KeycloakRoleMapper() {
    }

    public static Collection<GrantedAuthority> realmRoleAuthorities(Jwt jwt) {
        Collection<GrantedAuthority> authorities = new ArrayList<>();
        Map<String, Object> realmAccess = jwt.getClaim("realm_access");
        if (realmAccess != null && realmAccess.get("roles") instanceof Collection<?> roles) {
            roles.forEach(role -> authorities.add(new SimpleGrantedAuthority("ROLE_" + role.toString().toUpperCase())));
        }
        return authorities;
    }

    /**
     * True for a realm role this application actually manages - excludes Keycloak's own
     * default-roles-&lt;realm&gt; composite and bookkeeping roles like offline_access/uma_authorization.
     * Single filter shared by MeController (JWT roles claim) and KeycloakAdminClient.listRealmRoles()
     * (the admin panel's assignable-role list) so the two never drift apart.
     */
    public static boolean isApplicationRole(String roleName) {
        return !BOOKKEEPING_ROLES.contains(roleName) && !roleName.startsWith("default-roles-");
    }
}
