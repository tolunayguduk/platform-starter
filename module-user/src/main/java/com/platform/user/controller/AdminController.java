package com.platform.user.controller;

import com.platform.user.constant.AdminTableKey;
import com.platform.user.constant.StatsRange;
import com.platform.user.controller.model.AdminAuditRowsDto;
import com.platform.user.controller.model.AdminTableDto;
import com.platform.user.controller.model.AdminTableRowsDto;
import com.platform.user.controller.model.AdminUserDto;
import com.platform.user.controller.model.RegistrationStatsPointDto;
import com.platform.user.controller.model.UpdateUserRolesRequestDto;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

/**
 * Admin panel: user list/roles and registration stats. Gated on the ADMIN realm role directly
 * (not the fine-grained permission matrix - this is a role check, not a per-action permission).
 */
@RequestMapping("/api/admin")
public interface AdminController {

    @GetMapping("/users")
    List<AdminUserDto> listUsers();

    @PutMapping("/users/{id}/roles")
    void updateUserRoles(@PathVariable String id, @RequestBody UpdateUserRolesRequestDto request,
                          @AuthenticationPrincipal Jwt jwt);

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
}
