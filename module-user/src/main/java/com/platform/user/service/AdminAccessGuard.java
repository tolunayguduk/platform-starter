package com.platform.user.service;

import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

/**
 * Backs {@code @PreAuthorize("@adminAccessGuard.check(authentication)")} on the admin-panel
 * controllers - replaces a hardcoded {@code hasRole('ADMIN')} check with the same scope
 * resolution every service-layer guard already uses (see AdminAccessScopeService), so a role only
 * needs {@code role_state.scope} set to ORGANIZATION or PLATFORM to reach the admin panel at all,
 * regardless of what it's named.
 */
@Component("adminAccessGuard")
public class AdminAccessGuard {

    private final AdminAccessScopeService adminAccessScopeService;

    public AdminAccessGuard(AdminAccessScopeService adminAccessScopeService) {
        this.adminAccessScopeService = adminAccessScopeService;
    }

    public boolean check(Authentication authentication) {
        if (!(authentication.getPrincipal() instanceof Jwt jwt)) {
            return false;
        }
        return adminAccessScopeService.resolve(jwt.getSubject()).hasAdminAccess();
    }
}
