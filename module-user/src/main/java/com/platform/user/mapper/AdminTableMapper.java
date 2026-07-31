package com.platform.user.mapper;

import com.platform.user.controller.model.AdminAuditRowsDto;
import com.platform.user.controller.model.AdminTableDto;
import com.platform.user.controller.model.AdminTableRowsDto;
import com.platform.user.service.model.AdminAuditRowsResult;
import com.platform.user.service.model.AdminTableRowsResult;
import com.platform.user.service.model.AdminTableSummaryResult;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

import java.util.List;

@Mapper(componentModel = "spring")
public interface AdminTableMapper {

    AdminTableMapper INSTANCE = Mappers.getMapper(AdminTableMapper.class);

    public AdminTableDto toDto(AdminTableSummaryResult result);

    public List<AdminTableDto> toDtoList(List<AdminTableSummaryResult> results);

    public AdminTableRowsDto toDto(AdminTableRowsResult result);

    public AdminAuditRowsDto toDto(AdminAuditRowsResult result);
}
