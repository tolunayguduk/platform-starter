package com.platform.user.controller;

import com.platform.user.constant.StatsRange;
import com.platform.user.controller.model.AdminUserDto;
import com.platform.user.controller.model.RegistrationStatsPointDto;
import com.platform.user.controller.model.UpdateUserRolesRequestDto;
import com.platform.user.mapper.AdminUserMapper;
import com.platform.user.service.AdminUserService;
import com.platform.user.service.model.UpdateUserRolesCommand;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@PreAuthorize("hasRole('ADMIN')")
public class AdminControllerImpl implements AdminController {

    private final AdminUserService adminUserService;
    private final AdminUserMapper adminUserMapper;

    public AdminControllerImpl(AdminUserService adminUserService, AdminUserMapper adminUserMapper) {
        this.adminUserService = adminUserService;
        this.adminUserMapper = adminUserMapper;
    }

    @Override
    public List<AdminUserDto> listUsers() {
        return adminUserMapper.toUserDtoList(adminUserService.listUsers());
    }

    @Override
    public void updateUserRoles(String id, UpdateUserRolesRequestDto request, Jwt jwt) {
        adminUserService.updateUserRoles(new UpdateUserRolesCommand(id, request.roles(), jwt.getSubject()));
    }

    @Override
    public List<RegistrationStatsPointDto> registrationStats(StatsRange range) {
        return adminUserMapper.toStatsDtoList(adminUserService.getRegistrationStats(range));
    }
}
