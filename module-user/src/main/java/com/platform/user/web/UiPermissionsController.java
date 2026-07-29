package com.platform.user.web;

import com.platform.user.authz.Permission;
import com.platform.user.authz.RolePermissionLookupService;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Returns e.g. { "report:list": "ENABLED", "billing:refund": "HIDDEN" } for the CURRENT user only
 * (never accepts a userId parameter - see the security discussion: this must always mean "me").
 *
 * IMPORTANT: this is a UX convenience for custom components (e.g. platform-web's PermissionButton) -
 * it is NOT a security boundary. Every protected endpoint still enforces its own
 * @PreAuthorize("hasPermission(...)") independently of what this map says.
 */
@RestController
public class UiPermissionsController {

    private final RolePermissionLookupService lookupService;
    private final AllPermissionsProvider allPermissionsProvider;

    public UiPermissionsController(RolePermissionLookupService lookupService, AllPermissionsProvider allPermissionsProvider) {
        this.lookupService = lookupService;
        this.allPermissionsProvider = allPermissionsProvider;
    }

    @GetMapping("/api/me/ui-permissions")
    public Map<String, String> getMyUiPermissions(Authentication authentication) {
        // GrantedAuthority values carry Spring Security's own "ROLE_" convention (see
        // KeycloakRoleMapper) - the permission matrix keys roles by their plain Keycloak name.
        Set<String> roleNames = authentication.getAuthorities().stream()
                .map(a -> a.getAuthority().replaceFirst("^ROLE_", ""))
                .collect(Collectors.toSet());

        Set<String> granted = lookupService.resolvePermissions(roleNames);
        Set<String> visibleDenied = lookupService.resolveVisibleDeniedPermissions(roleNames);

        Map<String, String> result = new HashMap<>();
        for (Permission permission : allPermissionsProvider.findAll()) {
            if (granted.contains(permission.getKey())) {
                result.put(permission.getKey(), "ENABLED");
            } else if (visibleDenied.contains(permission.getKey())) {
                result.put(permission.getKey(), "DISABLED");
            } else {
                result.put(permission.getKey(),
                        permission.getUiPolicy() == Permission.UiPolicy.DISABLE_IF_DENIED ? "DISABLED" : "HIDDEN");
            }
        }
        return result;
    }
}