package com.platform.user.controller;

import com.platform.user.constant.AdminTableKey;
import com.platform.user.constant.StatsRange;
import com.platform.user.controller.model.AdminAuditRowsDto;
import com.platform.user.controller.model.AdminTableDto;
import com.platform.user.controller.model.AdminTableRowsDto;
import com.platform.user.controller.model.AdminUserDto;
import com.platform.user.controller.model.RegistrationStatsPointDto;
import com.platform.user.controller.model.UpdateUserRolesRequestDto;
import com.platform.user.mapper.AdminTableMapper;
import com.platform.user.mapper.AdminUserMapper;
import com.platform.user.service.AdminTableService;
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
    private final AdminTableService adminTableService;
    private final AdminTableMapper adminTableMapper;

    public AdminControllerImpl(AdminUserService adminUserService, AdminUserMapper adminUserMapper,
                                AdminTableService adminTableService, AdminTableMapper adminTableMapper) {
        this.adminUserService = adminUserService;
        this.adminUserMapper = adminUserMapper;
        this.adminTableService = adminTableService;
        this.adminTableMapper = adminTableMapper;
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

    @Override
    public List<AdminTableDto> listTables() {
        return adminTableMapper.toDtoList(adminTableService.listTables());
    }

    @Override
    public AdminTableRowsDto getTableRows(AdminTableKey key) {
        return adminTableMapper.toDto(adminTableService.getRows(key));
    }

    @Override
    public AdminAuditRowsDto getAuditRows(AdminTableKey key, String pk) {
        return adminTableMapper.toDto(adminTableService.getAuditRows(key, pk));
    }
}
