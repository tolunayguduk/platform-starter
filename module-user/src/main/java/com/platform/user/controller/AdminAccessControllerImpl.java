package com.platform.user.controller;

import com.platform.user.controller.model.AdminAccessScopeDto;
import com.platform.user.service.AdminAccessScopeService;
import com.platform.user.service.model.AdminAccessScope;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AdminAccessControllerImpl implements AdminAccessController {

    private final AdminAccessScopeService adminAccessScopeService;

    public AdminAccessControllerImpl(AdminAccessScopeService adminAccessScopeService) {
        this.adminAccessScopeService = adminAccessScopeService;
    }

    @Override
    public AdminAccessScopeDto getMyAdminScope(Jwt jwt) {
        AdminAccessScope scope = adminAccessScopeService.resolve(jwt.getSubject());
        return new AdminAccessScopeDto(scope.platformScoped(), scope.organizationScoped());
    }
}
