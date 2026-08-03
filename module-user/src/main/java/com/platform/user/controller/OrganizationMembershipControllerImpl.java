package com.platform.user.controller;

import com.platform.user.controller.model.JoinOrganizationRequestDto;
import com.platform.user.controller.model.JoinOrganizationResultDto;
import com.platform.user.controller.model.OrganizationMembershipRequestDto;
import com.platform.user.mapper.AdminOrganizationMapper;
import com.platform.user.service.OrganizationMembershipService;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class OrganizationMembershipControllerImpl implements OrganizationMembershipController {

    private final OrganizationMembershipService organizationMembershipService;
    private final AdminOrganizationMapper adminOrganizationMapper;

    public OrganizationMembershipControllerImpl(OrganizationMembershipService organizationMembershipService,
                                                  AdminOrganizationMapper adminOrganizationMapper) {
        this.organizationMembershipService = organizationMembershipService;
        this.adminOrganizationMapper = adminOrganizationMapper;
    }

    @Override
    public List<OrganizationMembershipRequestDto> listMyPendingInvites(Jwt jwt) {
        return adminOrganizationMapper.toRequestDtoList(organizationMembershipService.listMyPendingInvites(jwt.getSubject()));
    }

    @Override
    public void acceptInvite(Long id, Jwt jwt) {
        organizationMembershipService.acceptInvite(id, jwt.getSubject());
    }

    @Override
    public void declineInvite(Long id, Jwt jwt) {
        organizationMembershipService.declineInvite(id, jwt.getSubject());
    }

    @Override
    public JoinOrganizationResultDto joinOrganization(JoinOrganizationRequestDto request, Jwt jwt) {
        boolean approved = organizationMembershipService.requestToJoin(request.organizationId(), jwt.getSubject());
        return new JoinOrganizationResultDto(approved);
    }
}
