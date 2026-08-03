package com.platform.user.controller;

import com.platform.user.controller.model.JoinOrganizationRequestDto;
import com.platform.user.controller.model.JoinOrganizationResultDto;
import com.platform.user.controller.model.OrganizationMembershipRequestDto;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;

/**
 * Self-service organization membership - always scoped to the caller's own JWT subject, same tier
 * as ProfileController/UiPermissionsController (no AdminAccessGuard - any authenticated user,
 * with or without any admin-panel access, can accept an invite or ask to join an organization).
 */
public interface OrganizationMembershipController {

    @GetMapping("/api/me/organization-invites")
    List<OrganizationMembershipRequestDto> listMyPendingInvites(@AuthenticationPrincipal Jwt jwt);

    @PostMapping("/api/me/organization-invites/{id}/accept")
    void acceptInvite(@PathVariable Long id, @AuthenticationPrincipal Jwt jwt);

    @PostMapping("/api/me/organization-invites/{id}/decline")
    void declineInvite(@PathVariable Long id, @AuthenticationPrincipal Jwt jwt);

    /** Self-service join via an organization's permanent invite link/code. */
    @PostMapping("/api/me/organizations/join")
    JoinOrganizationResultDto joinOrganization(@RequestBody JoinOrganizationRequestDto request, @AuthenticationPrincipal Jwt jwt);
}
