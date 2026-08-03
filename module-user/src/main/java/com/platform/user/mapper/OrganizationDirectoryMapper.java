package com.platform.user.mapper;

import com.platform.user.controller.model.OrganizationMemberSummaryDto;
import com.platform.user.controller.model.OrganizationProfileDto;
import com.platform.user.controller.model.OrganizationSearchResultDto;
import com.platform.user.service.model.OrganizationMemberSummaryResult;
import com.platform.user.service.model.OrganizationProfileResult;
import com.platform.user.service.model.OrganizationSearchResult;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring")
public interface OrganizationDirectoryMapper {

    OrganizationSearchResultDto toDto(OrganizationSearchResult result);

    List<OrganizationSearchResultDto> toDtoList(List<OrganizationSearchResult> results);

    OrganizationProfileDto toDto(OrganizationProfileResult result);

    OrganizationMemberSummaryDto toDto(OrganizationMemberSummaryResult result);

    List<OrganizationMemberSummaryDto> toMemberDtoList(List<OrganizationMemberSummaryResult> results);
}
