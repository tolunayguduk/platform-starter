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
import com.platform.user.controller.model.UpdateRoleScopeRequestDto;
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
@PreAuthorize("@adminAccessGuard.check(authentication)")
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
    public List<AdminUserDto> listUsers(Jwt jwt) {
        return adminUserMapper.toUserDtoList(adminUserService.listUsers(jwt.getSubject()));
    }

    @Override
    public List<AdminRoleDto> listManagedRoles() {
        return adminUserMapper.toRoleDtoList(adminUserService.listManagedRoles());
    }

    @Override
    public void createRole(CreateRoleRequestDto request, Jwt jwt) {
        adminUserService.createRole(request.name(), jwt.getSubject());
    }

    @Override
    public void updateRoleDescription(String name, UpdateRoleDescriptionRequestDto request, Jwt jwt) {
        adminUserService.updateRoleDescription(name, request.description(), jwt.getSubject());
    }

    @Override
    public void updateRoleStatus(String name, UpdateRoleStatusRequestDto request, Jwt jwt) {
        adminUserService.updateRoleStatus(name, request.enabled(), jwt.getSubject());
    }

    @Override
    public void updateRoleScope(String name, UpdateRoleScopeRequestDto request, Jwt jwt) {
        adminUserService.updateRoleScope(name, request.scope(), jwt.getSubject());
    }

    @Override
    public void deleteRole(String name, Jwt jwt) {
        adminUserService.deleteRole(name, jwt.getSubject());
    }

    @Override
    public void updateUserRoles(String id, UpdateUserRolesRequestDto request, Jwt jwt) {
        adminUserService.updateUserRoles(new UpdateUserRolesCommand(id, request.roles(), jwt.getSubject()));
    }

    @Override
    public void updateUserIdentity(String id, UpdateUserIdentityRequestDto request, Jwt jwt) {
        adminUserService.updateUserIdentity(new UpdateUserIdentityCommand(id, request.username(), request.email(), jwt.getSubject()));
    }

    @Override
    public void updateUserStatus(String id, UpdateUserStatusRequestDto request, Jwt jwt) {
        adminUserService.updateUserStatus(id, request.enabled(), jwt.getSubject());
    }

    @Override
    public List<AdminUserAuditEventDto> getUserAuditEvents(String id, Jwt jwt) {
        return adminUserMapper.toAuditEventDtoList(adminUserService.getUserAuditEvents(id, jwt.getSubject()));
    }

    @Override
    public List<AdminUserAuditEventDto> getRecentActivity(int limit, Jwt jwt) {
        return adminUserMapper.toAuditEventDtoList(adminUserService.getRecentActivity(limit, jwt.getSubject()));
    }

    @Override
    public Map<String, String> getUserUiPermissions(String id) {
        return adminUserService.getUserUiPermissions(id);
    }

    @Override
    public List<RegistrationStatsPointDto> registrationStats(StatsRange range, Jwt jwt) {
        return adminUserMapper.toStatsDtoList(adminUserService.getRegistrationStats(range, jwt.getSubject()));
    }

    @Override
    public List<AdminTableDto> listTables() {
        return adminTableMapper.toDtoList(adminTableService.listTables());
    }

    @Override
    public AdminTableRowsDto getTableRows(AdminTableKey key, Jwt jwt) {
        return adminTableMapper.toDto(adminTableService.getRows(key, jwt.getSubject()));
    }

    @Override
    public AdminAuditRowsDto getAuditRows(AdminTableKey key, String pk, Jwt jwt) {
        return adminTableMapper.toDto(adminTableService.getAuditRows(key, pk, jwt.getSubject()));
    }

    @Override
    public AdminRowDto updateRow(AdminTableKey key, String pk, UpdateAdminRowRequestDto request, Jwt jwt) {
        return new AdminRowDto(adminTableService.updateRow(key, pk, request.changes(), jwt.getSubject()));
    }

    @Override
    public AdminRowDto createRow(AdminTableKey key, CreateAdminRowRequestDto request, Jwt jwt) {
        return new AdminRowDto(adminTableService.createRow(key, request.values(), jwt.getSubject()));
    }

    @Override
    public void deleteRow(AdminTableKey key, String pk, Jwt jwt) {
        adminTableService.deleteRow(key, pk, jwt.getSubject());
    }
}
