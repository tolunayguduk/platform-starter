package com.platform.user.controller;

import com.platform.user.constant.AdminTableKey;
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
import com.platform.user.controller.model.UpdateRoleStatusRequestDto;
import com.platform.user.controller.model.UpdateUserIdentityRequestDto;
import com.platform.user.controller.model.UpdateUserRolesRequestDto;
import com.platform.user.controller.model.UpdateUserStatusRequestDto;
import com.platform.user.mapper.AdminTableMapper;
import com.platform.user.mapper.AdminUserMapper;
import com.platform.user.service.AdminTableService;
import com.platform.user.service.AdminUserService;
import com.platform.user.service.model.UpdateUserIdentityCommand;
import com.platform.user.service.model.UpdateUserRolesCommand;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

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
    public List<AdminRoleDto> listManagedRoles() {
        return adminUserMapper.toRoleDtoList(adminUserService.listManagedRoles());
    }

    @Override
    public void createRole(CreateRoleRequestDto request) {
        adminUserService.createRole(request.name());
    }

    @Override
    public void updateRoleDescription(String name, UpdateRoleDescriptionRequestDto request) {
        adminUserService.updateRoleDescription(name, request.description());
    }

    @Override
    public void updateRoleStatus(String name, UpdateRoleStatusRequestDto request) {
        adminUserService.updateRoleStatus(name, request.enabled());
    }

    @Override
    public void deleteRole(String name) {
        adminUserService.deleteRole(name);
    }

    @Override
    public void updateUserRoles(String id, UpdateUserRolesRequestDto request, Jwt jwt) {
        adminUserService.updateUserRoles(new UpdateUserRolesCommand(id, request.roles(), jwt.getSubject()));
    }

    @Override
    public void updateUserIdentity(String id, UpdateUserIdentityRequestDto request) {
        adminUserService.updateUserIdentity(new UpdateUserIdentityCommand(id, request.username(), request.email()));
    }

    @Override
    public void updateUserStatus(String id, UpdateUserStatusRequestDto request, Jwt jwt) {
        adminUserService.updateUserStatus(id, request.enabled(), jwt.getSubject());
    }

    @Override
    public List<AdminUserAuditEventDto> getUserAuditEvents(String id) {
        return adminUserMapper.toAuditEventDtoList(adminUserService.getUserAuditEvents(id));
    }

    @Override
    public List<AdminUserAuditEventDto> getRecentActivity(int limit) {
        return adminUserMapper.toAuditEventDtoList(adminUserService.getRecentActivity(limit));
    }

    @Override
    public Map<String, String> getUserUiPermissions(String id) {
        return adminUserService.getUserUiPermissions(id);
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

    @Override
    public AdminRowDto updateRow(AdminTableKey key, String pk, UpdateAdminRowRequestDto request) {
        return new AdminRowDto(adminTableService.updateRow(key, pk, request.changes()));
    }

    @Override
    public AdminRowDto createRow(AdminTableKey key, CreateAdminRowRequestDto request) {
        return new AdminRowDto(adminTableService.createRow(key, request.values()));
    }

    @Override
    public void deleteRow(AdminTableKey key, String pk) {
        adminTableService.deleteRow(key, pk);
    }
}
