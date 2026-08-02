package com.platform.user.service.model;

import java.util.Set;

/** A resolved admin-panel caller's authority. platformScoped callers bypass every organization
 * filter; organizationScoped callers are confined to organizationGroupIds (the Keycloak Groups
 * they're personally a member of - a user, and therefore an org-scoped admin, may belong to more
 * than one); neither true means this caller has no admin-panel access at all. See
 * AdminAccessScopeService. */
public record AdminAccessScope(boolean platformScoped, boolean organizationScoped, Set<String> organizationGroupIds) {

    public boolean hasAdminAccess() {
        return platformScoped || organizationScoped;
    }
}
