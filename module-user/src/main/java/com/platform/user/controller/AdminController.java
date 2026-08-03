package com.platform.user.controller;

import com.platform.user.constant.AdminTableKey;
import com.platform.user.constant.RoleScope;
import com.platform.user.constant.StatsRange;
import com.platform.user.controller.model.AdminAuditRowsDto;
import com.platform.user.controller.model.AdminRoleDto;
import com.platform.user.controller.model.AdminRowDto;
import com.platform.user.controller.model.AdminTableDto;
import com.platform.user.controller.model.AdminTableRowsDto;
import com.platform.user.controller.model.AdminUserAuditEventDto;
import com.platform.user.controller.model.AdminUserDto;
import com.platform.user.controller.model.CreateAdminRowRequestDto;
import com.platform.user.controller.model.CreateRoleRequestDto;
import com.platform.user.controller.model.RegistrationStatsPointDto;
import com.platform.user.controller.model.UpdateAdminRowRequestDto;
import com.platform.user.controller.model.UpdateRoleDescriptionRequestDto;
import com.platform.user.controller.model.UpdateRoleScopeRequestDto;
import com.platform.user.controller.model.UpdateRoleStatusRequestDto;
import com.platform.user.controller.model.UpdateUserIdentityRequestDto;
import com.platform.user.controller.model.UpdateUserRolesRequestDto;
import com.platform.user.controller.model.UpdateUserStatusRequestDto;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;
import java.util.Map;

/**
 * Admin panel: user list/roles and registration stats. Gated on holding at least one
 * admin-panel-eligible role (see AdminAccessGuard/AdminAccessScopeService) - never a hardcoded
 * role name; PLATFORM-scope callers are unrestricted, ORGANIZATION-scope callers are confined to
 * their own organization(s) at the service layer.
 */
@RequestMapping("/api/admin")
public interface AdminController {

    @GetMapping("/users")
    List<AdminUserDto> listUsers(@AuthenticationPrincipal Jwt jwt);

    /** Every realm role this application manages, with its Keycloak-stored description - backs the
     * role picker in the role-function manager (and anywhere else the admin panel needs the live
     * set of assignable roles). Not scope-filtered - see AdminUserService.listManagedRoles. */
    @GetMapping("/roles")
    List<AdminRoleDto> listManagedRoles();

    /** Defines a brand new role in Keycloak (PLATFORM-scope only). */
    @PostMapping("/roles")
    void createRole(@RequestBody CreateRoleRequestDto request, @AuthenticationPrincipal Jwt jwt);

    /** Updates a role's description - purely descriptive, no authorization meaning (PLATFORM-scope only). */
    @PutMapping("/roles/{name}")
    void updateRoleDescription(@PathVariable String name, @RequestBody UpdateRoleDescriptionRequestDto request,
                                @AuthenticationPrincipal Jwt jwt);

    /** Temporarily disables/re-enables a role (PLATFORM-scope only) - see AdminUserService.updateRoleStatus. */
    @PutMapping("/roles/{name}/status")
    void updateRoleStatus(@PathVariable String name, @RequestBody UpdateRoleStatusRequestDto request,
                           @AuthenticationPrincipal Jwt jwt);

    /** Sets whether a role reaches the whole platform or just its holders' own organization(s) -
     * PLATFORM-scope only. See RoleScope. */
    @PutMapping("/roles/{name}/scope")
    void updateRoleScope(@PathVariable String name, @RequestBody UpdateRoleScopeRequestDto request,
                         @AuthenticationPrincipal Jwt jwt);

    /** Deletes a role entirely and cleans up its function grants (PLATFORM-scope only). Rejects
     * removing the last enabled PLATFORM-scope role. */
    @DeleteMapping("/roles/{name}")
    void deleteRole(@PathVariable String name, @AuthenticationPrincipal Jwt jwt);

    @PutMapping("/users/{id}/roles")
    void updateUserRoles(@PathVariable String id, @RequestBody UpdateUserRolesRequestDto request,
                          @AuthenticationPrincipal Jwt jwt);

    /** Username/email edit - routed straight through to Keycloak, never persisted locally.
     * PLATFORM-scope only. */
    @PutMapping("/users/{id}/identity")
    void updateUserIdentity(@PathVariable String id, @RequestBody UpdateUserIdentityRequestDto request, @AuthenticationPrincipal Jwt jwt);

    /** Enable/disable the account - a disabled user cannot obtain a token from Keycloak. */
    @PutMapping("/users/{id}/status")
    void updateUserStatus(@PathVariable String id, @RequestBody UpdateUserStatusRequestDto request,
                           @AuthenticationPrincipal Jwt jwt);

    /** This user's audit trail, sourced from Keycloak's own admin event log - PLATFORM-scope only. */
    @GetMapping("/users/{id}/audit")
    List<AdminUserAuditEventDto> getUserAuditEvents(@PathVariable String id, @AuthenticationPrincipal Jwt jwt);

    /** Recent activity feed, same admin event log as getUserAuditEvents - PLATFORM-scope callers
     * see realm-wide activity, ORGANIZATION-scope callers only activity about their own
     * organization's users. */
    @GetMapping("/activity")
    List<AdminUserAuditEventDto> getRecentActivity(@RequestParam(defaultValue = "20") int limit, @AuthenticationPrincipal Jwt jwt);

    /** e.g. { "report:list": "ENABLED", "billing:refund": "HIDDEN" } for this user's current
     * roles - the same computation /api/me/ui-permissions does for the caller, run for someone
     * else. PLATFORM: any user. ORGANIZATION-scope: only a user who shares an organization with
     * the caller. */
    @GetMapping("/users/{id}/ui-permissions")
    Map<String, String> getUserUiPermissions(@PathVariable String id, @AuthenticationPrincipal Jwt jwt);

    /** Registration counts/trend - PLATFORM-scope callers see realm-wide totals, ORGANIZATION-scope
     * callers only their own organization's registrations. */
    @GetMapping("/stats/registrations")
    List<RegistrationStatsPointDto> registrationStats(@RequestParam StatsRange range, @AuthenticationPrincipal Jwt jwt);

    /** Raw MySQL "main tables" (GDPR categories + permission matrix) the DB browser can list. */
    @GetMapping("/tables")
    List<AdminTableDto> listTables();

    /** PERMISSION/ROLE_PERMISSION/USER_CONSENT: PLATFORM-scope only. USER_PROFILE/USER_CONTACT:
     * filtered to the caller's own organization's members for an ORGANIZATION-scope caller. */
    @GetMapping("/tables/{key}/rows")
    AdminTableRowsDto getTableRows(@PathVariable AdminTableKey key, @AuthenticationPrincipal Jwt jwt);

    /** A single row's Envers audit history, keyed by that table's primary key value -
     * PLATFORM-scope only. */
    @GetMapping("/tables/{key}/rows/{pk}/audit")
    AdminAuditRowsDto getAuditRows(@PathVariable AdminTableKey key, @PathVariable String pk, @AuthenticationPrincipal Jwt jwt);

    /** Main-table rows only - never targets an audit table (there is no AdminTableKey for one).
     * Writing to PERMISSION or ROLE_PERMISSION is PLATFORM-scope only. */
    @PatchMapping("/tables/{key}/rows/{pk}")
    AdminRowDto updateRow(@PathVariable AdminTableKey key, @PathVariable String pk,
                          @RequestBody UpdateAdminRowRequestDto request, @AuthenticationPrincipal Jwt jwt);

    /** Only ROLE_PERMISSION supports this today (granting a function to a role). PLATFORM-scope only. */
    @PostMapping("/tables/{key}/rows")
    AdminRowDto createRow(@PathVariable AdminTableKey key, @RequestBody CreateAdminRowRequestDto request,
                          @AuthenticationPrincipal Jwt jwt);

    /** Only ROLE_PERMISSION supports this today (revoking a function from a role). PLATFORM-scope only. */
    @DeleteMapping("/tables/{key}/rows/{pk}")
    void deleteRow(@PathVariable AdminTableKey key, @PathVariable String pk, @AuthenticationPrincipal Jwt jwt);
}
