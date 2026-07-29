package com.platform.user.web;

import com.platform.user.admin.AdminUserService;
import com.platform.user.admin.AdminUserView;
import com.platform.user.admin.RegistrationStatsPoint;
import com.platform.user.admin.StatsRange;
import com.platform.user.admin.UpdateUserRolesRequest;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Admin panel: user list/roles and registration stats. Gated on the ADMIN realm role directly
 * (not the fine-grained permission matrix - this is a role check, not a per-action permission).
 */
@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final AdminUserService adminUserService;

    public AdminController(AdminUserService adminUserService) {
        this.adminUserService = adminUserService;
    }

    @GetMapping("/users")
    public List<AdminUserView> listUsers() {
        return adminUserService.listUsers();
    }

    @PutMapping("/users/{id}/roles")
    public void updateUserRoles(@PathVariable String id, @RequestBody UpdateUserRolesRequest request,
                                 @AuthenticationPrincipal Jwt jwt) {
        adminUserService.updateUserRoles(id, request.roles(), jwt.getSubject());
    }

    @GetMapping("/stats/registrations")
    public List<RegistrationStatsPoint> registrationStats(@RequestParam StatsRange range) {
        return adminUserService.getRegistrationStats(range);
    }
}