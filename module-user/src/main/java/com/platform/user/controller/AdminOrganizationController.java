package com.platform.user.controller;

import com.platform.user.controller.model.AdminUserDto;
import com.platform.user.controller.model.CreateOrganizationRequestDto;
import com.platform.user.controller.model.OrganizationDto;
import com.platform.user.controller.model.UpdateOrganizationDescriptionRequestDto;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

/**
 * Organizations = Keycloak Groups (see AdminOrganizationService). Gated the same way as
 * AdminController - AdminAccessGuard, never a hardcoded role name; every method additionally
 * enforces its own PLATFORM-only or same-organization guard at the service layer.
 */
@RequestMapping("/api/admin/organizations")
public interface AdminOrganizationController {

    /** Every organization for a PLATFORM-scope caller; only the caller's own for an
     * ORGANIZATION-scope one. */
    @GetMapping
    List<OrganizationDto> listOrganizations(@AuthenticationPrincipal Jwt jwt);

    /** PLATFORM-scope only. */
    @PostMapping
    OrganizationDto createOrganization(@RequestBody CreateOrganizationRequestDto request, @AuthenticationPrincipal Jwt jwt);

    /** PLATFORM: any organization. ORGANIZATION-scope: only their own. */
    @PutMapping("/{id}")
    void updateOrganizationDescription(@PathVariable String id, @RequestBody UpdateOrganizationDescriptionRequestDto request,
                                        @AuthenticationPrincipal Jwt jwt);

    /** PLATFORM-scope only - destructive, no organization self-service delete. */
    @DeleteMapping("/{id}")
    void deleteOrganization(@PathVariable String id, @AuthenticationPrincipal Jwt jwt);

    /** PLATFORM: any organization. ORGANIZATION-scope: only their own. */
    @GetMapping("/{id}/members")
    List<AdminUserDto> getOrganizationMembers(@PathVariable String id, @AuthenticationPrincipal Jwt jwt);

    /** Exact username/email lookup only, never a browse/search - backs the "add member" flow so
     * an organization admin can invite someone without seeing the full user directory. */
    @GetMapping("/user-lookup")
    AdminUserDto findUserByIdentifier(@RequestParam String identifier, @AuthenticationPrincipal Jwt jwt);

    /** PLATFORM: any organization. ORGANIZATION-scope: only their own. */
    @PutMapping("/{id}/members/{userId}")
    void addMember(@PathVariable String id, @PathVariable String userId, @AuthenticationPrincipal Jwt jwt);

    /** PLATFORM: any organization. ORGANIZATION-scope: only their own. */
    @DeleteMapping("/{id}/members/{userId}")
    void removeMember(@PathVariable String id, @PathVariable String userId, @AuthenticationPrincipal Jwt jwt);
}
