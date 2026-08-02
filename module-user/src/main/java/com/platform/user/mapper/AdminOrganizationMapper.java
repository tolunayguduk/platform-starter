package com.platform.user.mapper;

import com.platform.user.controller.model.OrganizationDto;
import com.platform.user.service.model.OrganizationResult;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring")
public interface AdminOrganizationMapper {

    OrganizationDto toDto(OrganizationResult result);

    List<OrganizationDto> toDtoList(List<OrganizationResult> results);
}
