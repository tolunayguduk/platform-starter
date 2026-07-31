package com.platform.app.controller;

import com.platform.security.util.KeycloakRoleMapper;
import com.platform.user.controller.model.CurrentUserDto;
import com.platform.user.mapper.CurrentUserMapper;
import com.platform.user.service.CurrentUserService;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
public class MeControllerImpl implements MeController {

    private final CurrentUserService currentUserService;
    private final CurrentUserMapper currentUserMapper;

    public MeControllerImpl(CurrentUserService currentUserService, CurrentUserMapper currentUserMapper) {
        this.currentUserService = currentUserService;
        this.currentUserMapper = currentUserMapper;
    }

    @Override
    public CurrentUserDto me(Jwt jwt) {
        return currentUserMapper.toDto(currentUserService.getCurrentUser(
                jwt.getSubject(), jwt.getClaimAsString("preferred_username"), jwt.getClaimAsString("email"),
                extractRoles(jwt)));
    }

    /**
     * The raw JWT claim (unlike Spring's GrantedAuthority) carries plain Keycloak role names with
     * no "ROLE_" prefix - filtered down to the application's own roles (ADMIN/MANAGER/USER/...),
     * dropping Keycloak's internal default-roles-* composite and service-account bookkeeping roles.
     */
    private List<String> extractRoles(Jwt jwt) {
        Map<String, Object> realmAccess = jwt.getClaim("realm_access");
        if (realmAccess == null || !(realmAccess.get("roles") instanceof List<?> roles)) {
            return List.of();
        }
        return roles.stream()
                .map(Object::toString)
                .filter(KeycloakRoleMapper::isApplicationRole)
                .toList();
    }
}
