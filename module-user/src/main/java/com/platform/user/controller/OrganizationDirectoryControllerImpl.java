package com.platform.user.controller;

import com.platform.user.controller.model.OrganizationMemberSummaryDto;
import com.platform.user.controller.model.OrganizationProfileDto;
import com.platform.user.controller.model.OrganizationSearchResultDto;
import com.platform.user.mapper.OrganizationDirectoryMapper;
import com.platform.user.service.OrganizationDirectoryService;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class OrganizationDirectoryControllerImpl implements OrganizationDirectoryController {

    private final OrganizationDirectoryService organizationDirectoryService;
    private final OrganizationDirectoryMapper organizationDirectoryMapper;

    public OrganizationDirectoryControllerImpl(OrganizationDirectoryService organizationDirectoryService,
                                                OrganizationDirectoryMapper organizationDirectoryMapper) {
        this.organizationDirectoryService = organizationDirectoryService;
        this.organizationDirectoryMapper = organizationDirectoryMapper;
    }

    @Override
    public List<OrganizationSearchResultDto> search(String query) {
        return organizationDirectoryMapper.toDtoList(organizationDirectoryService.search(query));
    }

    @Override
    public OrganizationProfileDto getProfile(String id, Jwt jwt) {
        return organizationDirectoryMapper.toDto(organizationDirectoryService.getProfile(id, jwt.getSubject()));
    }

    @Override
    public List<OrganizationMemberSummaryDto> listMembers(String id) {
        return organizationDirectoryMapper.toMemberDtoList(organizationDirectoryService.listMembers(id));
    }
}
