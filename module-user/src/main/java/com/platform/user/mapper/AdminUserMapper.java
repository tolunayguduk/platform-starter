package com.platform.user.mapper;

import com.platform.user.controller.model.AdminRoleDto;
import com.platform.user.controller.model.AdminUserAuditEventDto;
import com.platform.user.controller.model.AdminUserDto;
import com.platform.user.controller.model.RegistrationStatsPointDto;
import com.platform.user.service.model.AdminRoleResult;
import com.platform.user.service.model.AdminUserAuditEventResult;
import com.platform.user.service.model.AdminUserResult;
import com.platform.user.service.model.RegistrationStatsPointResult;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

import java.util.List;

@Mapper(componentModel = "spring")
public interface AdminUserMapper {

    AdminUserMapper INSTANCE = Mappers.getMapper(AdminUserMapper.class);

    public AdminUserDto toDto(AdminUserResult result);

    public List<AdminUserDto> toUserDtoList(List<AdminUserResult> results);

    public RegistrationStatsPointDto toDto(RegistrationStatsPointResult result);

    public List<RegistrationStatsPointDto> toStatsDtoList(List<RegistrationStatsPointResult> results);

    public AdminUserAuditEventDto toDto(AdminUserAuditEventResult result);

    public List<AdminUserAuditEventDto> toAuditEventDtoList(List<AdminUserAuditEventResult> results);

    public AdminRoleDto toDto(AdminRoleResult result);

    public List<AdminRoleDto> toRoleDtoList(List<AdminRoleResult> results);
}
