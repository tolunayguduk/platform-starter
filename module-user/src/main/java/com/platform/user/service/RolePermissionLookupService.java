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

    /**
     * UI-only: permissions explicitly marked hidden for a role (see
     * RolePermission.AccessLevel.HIDDEN), overriding the permission's own ui_policy-based default.
     * Never used for authorization decisions.
     */
    Set<String> resolveHiddenPermissions(Collection<String> roleNames);
}
