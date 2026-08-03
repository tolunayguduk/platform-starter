package com.platform.user.controller;

import com.platform.user.controller.model.AdminUserDto;
import com.platform.user.controller.model.CreateOrganizationRequestDto;
import com.platform.user.controller.model.OrganizationDto;
import com.platform.user.controller.model.OrganizationMembershipRequestDto;
import com.platform.user.controller.model.RenameOrganizationRequestDto;
import com.platform.user.controller.model.UpdateMembershipApprovalRequestDto;
import com.platform.user.controller.model.UpdateOrganizationDescriptionRequestDto;
import com.platform.user.controller.model.UpdateOrganizationImagesRequestDto;
import com.platform.user.mapper.AdminOrganizationMapper;
import com.platform.user.mapper.AdminUserMapper;
import com.platform.user.service.AdminOrganizationService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@PreAuthorize("@adminAccessGuard.check(authentication)")
public class AdminOrganizationControllerImpl implements AdminOrganizationController {

    private final AdminOrganizationService adminOrganizationService;
    private final AdminOrganizationMapper adminOrganizationMapper;
    private final AdminUserMapper adminUserMapper;

    public AdminOrganizationControllerImpl(AdminOrganizationService adminOrganizationService,
                                            AdminOrganizationMapper adminOrganizationMapper,
                                            AdminUserMapper adminUserMapper) {
        this.adminOrganizationService = adminOrganizationService;
        this.adminOrganizationMapper = adminOrganizationMapper;
        this.adminUserMapper = adminUserMapper;
    }

    @Override
    public List<OrganizationDto> listOrganizations(Jwt jwt) {
        return adminOrganizationMapper.toDtoList(adminOrganizationService.listOrganizations(jwt.getSubject()));
    }

    @Override
    public OrganizationDto createOrganization(CreateOrganizationRequestDto request, Jwt jwt) {
        return adminOrganizationMapper.toDto(adminOrganizationService.createOrganization(request.name(), jwt.getSubject()));
    }

    @Override
    public void updateOrganizationDescription(String id, UpdateOrganizationDescriptionRequestDto request, Jwt jwt) {
        adminOrganizationService.updateOrganizationDescription(id, request.description(), jwt.getSubject());
    }

    @Override
    public void renameOrganization(String id, RenameOrganizationRequestDto request, Jwt jwt) {
        adminOrganizationService.renameOrganization(id, request.name(), jwt.getSubject());
    }

    @Override
    public void updateOrganizationImages(String id, UpdateOrganizationImagesRequestDto request, Jwt jwt) {
        adminOrganizationService.updateOrganizationImages(id, request.coverImageUrl(), request.logoImageUrl(), jwt.getSubject());
    }

    @Override
    public void updateMembershipApprovalSetting(String id, UpdateMembershipApprovalRequestDto request, Jwt jwt) {
        adminOrganizationService.updateMembershipApprovalSetting(id, request.requiresApproval(), jwt.getSubject());
    }

    @Override
    public void deleteOrganization(String id, Jwt jwt) {
        adminOrganizationService.deleteOrganization(id, jwt.getSubject());
    }

    @Override
    public List<AdminUserDto> getOrganizationMembers(String id, Jwt jwt) {
        return adminUserMapper.toUserDtoList(adminOrganizationService.getOrganizationMembers(id, jwt.getSubject()));
    }

    @Override
    public AdminUserDto findUserByIdentifier(String identifier, Jwt jwt) {
        return adminUserMapper.toDto(adminOrganizationService.findUserByIdentifier(identifier, jwt.getSubject()));
    }

    @Override
    public void inviteMember(String id, String userId, Jwt jwt) {
        adminOrganizationService.inviteMember(id, userId, jwt.getSubject());
    }

    @Override
    public void removeMember(String id, String userId, Jwt jwt) {
        adminOrganizationService.removeMember(id, userId, jwt.getSubject());
    }

    @Override
    public List<OrganizationMembershipRequestDto> listPendingJoinRequests(String id, Jwt jwt) {
        return adminOrganizationMapper.toRequestDtoList(adminOrganizationService.listPendingJoinRequests(id, jwt.getSubject()));
    }

    @Override
    public void approveJoinRequest(String id, Long requestId, Jwt jwt) {
        adminOrganizationService.approveJoinRequest(requestId, jwt.getSubject());
    }

    @Override
    public void rejectJoinRequest(String id, Long requestId, Jwt jwt) {
        adminOrganizationService.rejectJoinRequest(requestId, jwt.getSubject());
    }
}
