package com.platform.app;

import com.platform.user.identity.CurrentUserService;
import com.platform.user.identity.CurrentUserView;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.Set;

@RestController
public class MeController {

    /** Keycloak's own bookkeeping roles - never meaningful to the frontend's "what can this user do" checks. */
    private static final Set<String> TECHNICAL_ROLES = Set.of("offline_access", "uma_authorization");

    private final CurrentUserService currentUserService;

    public MeController(CurrentUserService currentUserService) {
        this.currentUserService = currentUserService;
    }

    @GetMapping("/api/me")
    public CurrentUserView me(@AuthenticationPrincipal Jwt jwt) {
        return currentUserService.findByKeycloakUserId(
                jwt.getSubject(), jwt.getClaimAsString("preferred_username"), jwt.getClaimAsString("email"),
                extractRoles(jwt));
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
                .filter(role -> !TECHNICAL_ROLES.contains(role) && !role.startsWith("default-roles-"))
                .toList();
    }
}