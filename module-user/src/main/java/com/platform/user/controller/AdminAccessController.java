package com.platform.user.controller;

import com.platform.user.controller.model.AdminAccessScopeDto;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Lets any authenticated user ask "can I reach the admin panel, and how far" - deliberately not
 * gated by AdminAccessGuard itself (that would be circular: you need this answer before you can
 * even try). Never a security boundary on its own; AdminController still independently enforces
 * @PreAuthorize("@adminAccessGuard.check(authentication)") plus every service-layer scope check.
 */
public interface AdminAccessController {

    @GetMapping("/api/me/admin-scope")
    AdminAccessScopeDto getMyAdminScope(@AuthenticationPrincipal Jwt jwt);
}
