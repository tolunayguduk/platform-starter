package com.platform.user.service;

import java.util.Map;
import java.util.Set;

/**
 * Computes e.g. { "report:list": "ENABLED", "billing:refund": "HIDDEN" } for a set of role names.
 *
 * IMPORTANT: this is a UX convenience for custom components (e.g. platform-web's PermissionButton) -
 * it is NOT a security boundary. Every protected endpoint still enforces its own
 * @PreAuthorize("hasPermission(...)") independently of what this map says.
 */
public interface UiPermissionsService {

    Map<String, String> getUiPermissions(Set<String> roleNames);
}
