package com.platform.user.controller.model;

/** Whether the calling user can reach the admin panel at all, and how far - backs
 * RequireAdmin/UserMenu on the frontend so admin-panel access is never gated on a hardcoded role
 * name there either. platformScoped implies full access; organizationScoped implies access
 * confined to their own organization(s); neither means no admin-panel access. */
public record AdminAccessScopeDto(boolean platformScoped, boolean organizationScoped) {
}
