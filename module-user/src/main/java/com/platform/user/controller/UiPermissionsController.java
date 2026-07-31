package com.platform.user.controller;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.Map;

/**
 * Returns e.g. { "report:list": "ENABLED", "billing:refund": "HIDDEN" } for the CURRENT user only
 * (never accepts a userId parameter - this must always mean "me").
 *
 * IMPORTANT: this is a UX convenience for custom components (e.g. platform-web's PermissionButton) -
 * it is NOT a security boundary. Every protected endpoint still enforces its own
 * @PreAuthorize("hasPermission(...)") independently of what this map says.
 */
public interface UiPermissionsController {

    @GetMapping("/api/me/ui-permissions")
    Map<String, String> getMyUiPermissions(Authentication authentication);
}
