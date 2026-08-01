package com.platform.user.controller;

import com.platform.user.constant.AdminTableKey;
import com.platform.user.constant.StatsRange;
import com.platform.user.controller.model.AdminAuditRowsDto;
import com.platform.user.controller.model.AdminRowDto;
import com.platform.user.controller.model.AdminTableDto;
import com.platform.user.controller.model.AdminTableRowsDto;
import com.platform.user.controller.model.AdminUserAuditEventDto;
import com.platform.user.controller.model.AdminUserDto;
import com.platform.user.controller.model.CreateAdminRowRequestDto;
import com.platform.user.controller.model.CreateRoleRequestDto;
import com.platform.user.controller.model.RegistrationStatsPointDto;
import com.platform.user.controller.model.UpdateAdminRowRequestDto;
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
 * Admin panel: user list/roles and registration stats. Gated on the ADMIN realm role directly
 * (not the fine-grained permission matrix - this is a role check, not a per-action permission).
 */
@RequestMapping("/api/admin")
public interface AdminController {

    @GetMapping("/users")
    List<AdminUserDto> listUsers();

    /** Every realm role this application manages - backs the role picker in the role-function
     * manager (and anywhere else the admin panel needs the live set of assignable roles). */
    @GetMapping("/roles")
    List<String> listManagedRoles();

    /** Defines a brand new role in Keycloak. */
    @PostMapping("/roles")
    void createRole(@RequestBody CreateRoleRequestDto request);

    @PutMapping("/users/{id}/roles")
    void updateUserRoles(@PathVariable String id, @RequestBody UpdateUserRolesRequestDto request,
                          @AuthenticationPrincipal Jwt jwt);

    /** Username/email edit - routed straight through to Keycloak, never persisted locally. */
    @PutMapping("/users/{id}/identity")
    void updateUserIdentity(@PathVariable String id, @RequestBody UpdateUserIdentityRequestDto request);

    /** Enable/disable the account - a disabled user cannot obtain a token from Keycloak. */
    @PutMapping("/users/{id}/status")
    void updateUserStatus(@PathVariable String id, @RequestBody UpdateUserStatusRequestDto request,
                           @AuthenticationPrincipal Jwt jwt);

    /** This user's audit trail, sourced from Keycloak's own admin event log. */
    @GetMapping("/users/{id}/audit")
    List<AdminUserAuditEventDto> getUserAuditEvents(@PathVariable String id);

    /** e.g. { "report:list": "ENABLED", "billing:refund": "HIDDEN" } for this user's current
     * roles - the same computation /api/me/ui-permissions does for the caller, run for someone else. */
    @GetMapping("/users/{id}/ui-permissions")
    Map<String, String> getUserUiPermissions(@PathVariable String id);

    @GetMapping("/stats/registrations")
    List<RegistrationStatsPointDto> registrationStats(@RequestParam StatsRange range);

    /** Raw MySQL "main tables" (GDPR categories + permission matrix) the DB browser can list. */
    @GetMapping("/tables")
    List<AdminTableDto> listTables();

    @GetMapping("/tables/{key}/rows")
    AdminTableRowsDto getTableRows(@PathVariable AdminTableKey key);

    /** A single row's Envers audit history, keyed by that table's primary key value. */
    @GetMapping("/tables/{key}/rows/{pk}/audit")
    AdminAuditRowsDto getAuditRows(@PathVariable AdminTableKey key, @PathVariable String pk);

    /** Main-table rows only - never targets an audit table (there is no AdminTableKey for one). */
    @PatchMapping("/tables/{key}/rows/{pk}")
    AdminRowDto updateRow(@PathVariable AdminTableKey key, @PathVariable String pk,
                          @RequestBody UpdateAdminRowRequestDto request);

    /** Only ROLE_PERMISSION supports this today (granting a function to a role). */
    @PostMapping("/tables/{key}/rows")
    AdminRowDto createRow(@PathVariable AdminTableKey key, @RequestBody CreateAdminRowRequestDto request);

    /** Only ROLE_PERMISSION supports this today (revoking a function from a role). */
    @DeleteMapping("/tables/{key}/rows/{pk}")
    void deleteRow(@PathVariable AdminTableKey key, @PathVariable String pk);
}
