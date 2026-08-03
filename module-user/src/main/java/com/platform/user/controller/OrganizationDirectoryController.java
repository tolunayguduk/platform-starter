package com.platform.user.controller;

import com.platform.user.controller.model.OrganizationMemberSummaryDto;
import com.platform.user.controller.model.OrganizationProfileDto;
import com.platform.user.controller.model.OrganizationSearchResultDto;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

/**
 * Public organization browsing - same tier as OrganizationMembershipController/ProfileController
 * (no AdminAccessGuard, any authenticated user). Finding and viewing an organization's landing
 * page is how a plain user discovers it in order to request joining - nothing to do with
 * admin-panel access.
 */
@RequestMapping("/api/organizations")
public interface OrganizationDirectoryController {

    /** Backs the navbar search box. */
    @GetMapping("/search")
    List<OrganizationSearchResultDto> search(@RequestParam(defaultValue = "") String query);

    /** The organization's landing page, including caller-relative state (isMember/canEdit/
     * hasPendingJoinRequest) so the frontend doesn't need a second round trip to decide what to show. */
    @GetMapping("/{id}")
    OrganizationProfileDto getProfile(@PathVariable String id, @AuthenticationPrincipal Jwt jwt);

    /** Backs the landing page's member-list popup - lightweight (username/fullName only), unlike
     * the admin-panel member list. */
    @GetMapping("/{id}/members")
    List<OrganizationMemberSummaryDto> listMembers(@PathVariable String id);
}
