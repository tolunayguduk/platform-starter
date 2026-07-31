package com.platform.user.controller;

import com.platform.user.service.UiPermissionsService;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
public class UiPermissionsControllerImpl implements UiPermissionsController {

    private final UiPermissionsService uiPermissionsService;

    public UiPermissionsControllerImpl(UiPermissionsService uiPermissionsService) {
        this.uiPermissionsService = uiPermissionsService;
    }

    @Override
    public Map<String, String> getMyUiPermissions(Authentication authentication) {
        // GrantedAuthority values carry Spring Security's own "ROLE_" convention (see
        // KeycloakRoleMapper) - the permission matrix keys roles by their plain Keycloak name.
        Set<String> roleNames = authentication.getAuthorities().stream()
                .map(a -> a.getAuthority().replaceFirst("^ROLE_", ""))
                .collect(Collectors.toSet());
        return uiPermissionsService.getUiPermissions(roleNames);
    }
}
