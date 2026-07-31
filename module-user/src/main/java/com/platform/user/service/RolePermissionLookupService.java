package com.platform.user.service;

import com.platform.security.RolePermissionLookupPort;

import java.util.Collection;
import java.util.Set;

public interface RolePermissionLookupService extends RolePermissionLookupPort {

    /**
     * UI-only: permissions a role isn't actually granted but should still render as a visible,
     * disabled control (see RolePermission.AccessLevel.VISIBLE_DENIED) rather than the
     * permission's default hidden/disabled fallback. Never used for authorization decisions.
     */
    Set<String> resolveVisibleDeniedPermissions(Collection<String> roleNames);
}
