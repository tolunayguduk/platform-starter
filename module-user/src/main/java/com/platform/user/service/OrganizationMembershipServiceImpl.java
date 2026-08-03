package com.platform.user.service;

import com.platform.error.BusinessException;
import com.platform.security.integration.keycloak.KeycloakAdminClient;
import com.platform.security.integration.keycloak.model.KeycloakGroup;
import com.platform.security.integration.keycloak.model.KeycloakUserId;
import com.platform.user.constant.MembershipRequestStatus;
import com.platform.user.constant.MembershipRequestType;
import com.platform.user.entity.OrganizationMembershipRequest;
import com.platform.user.repository.OrganizationManagerRepository;
import com.platform.user.repository.OrganizationMembershipRequestRepository;
import com.platform.user.service.model.OrganizationMembershipRequestResult;
import com.platform.user.service.model.OrganizationSearchResult;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class OrganizationMembershipServiceImpl implements OrganizationMembershipService {

    private final KeycloakAdminClient keycloakAdminClient;
    private final OrganizationMembershipRequestRepository membershipRequestRepository;
    private final OrganizationManagerRepository organizationManagerRepository;

    public OrganizationMembershipServiceImpl(KeycloakAdminClient keycloakAdminClient,
                                              OrganizationMembershipRequestRepository membershipRequestRepository,
                                              OrganizationManagerRepository organizationManagerRepository) {
        this.keycloakAdminClient = keycloakAdminClient;
        this.membershipRequestRepository = membershipRequestRepository;
        this.organizationManagerRepository = organizationManagerRepository;
    }

    @Override
    public List<OrganizationMembershipRequestResult> listMyPendingInvites(String keycloakUserId) {
        List<OrganizationMembershipRequest> invites = membershipRequestRepository
                .findByKeycloakUserIdAndRequestTypeAndStatus(keycloakUserId, MembershipRequestType.INVITE, MembershipRequestStatus.PENDING);
        if (invites.isEmpty()) {
            return List.of();
        }
        Map<String, String> namesByOrgId = keycloakAdminClient.listGroups().stream()
                .collect(Collectors.toMap(KeycloakGroup::id, KeycloakGroup::name));
        return invites.stream()
                .map(r -> new OrganizationMembershipRequestResult(
                        r.getId(), r.getOrganizationId(), namesByOrgId.getOrDefault(r.getOrganizationId(), r.getOrganizationId()),
                        r.getKeycloakUserId(), null, r.getRequestType(), r.getCreatedAt()))
                .toList();
    }

    @Override
    @Transactional
    public void acceptInvite(Long requestId, String keycloakUserId) {
        OrganizationMembershipRequest request = loadOwnPendingInvite(requestId, keycloakUserId);
        request.setStatus(MembershipRequestStatus.APPROVED);
        request.setResolvedAt(Instant.now());
        membershipRequestRepository.save(request);
        keycloakAdminClient.addUserToGroup(keycloakUserId, request.getOrganizationId());
    }

    @Override
    @Transactional
    public void declineInvite(Long requestId, String keycloakUserId) {
        OrganizationMembershipRequest request = loadOwnPendingInvite(requestId, keycloakUserId);
        request.setStatus(MembershipRequestStatus.REJECTED);
        request.setResolvedAt(Instant.now());
        membershipRequestRepository.save(request);
    }

    @Override
    @Transactional
    public boolean requestToJoin(String organizationId, String keycloakUserId) {
        KeycloakGroup group = keycloakAdminClient.getGroup(organizationId);

        Set<String> memberIds = keycloakAdminClient.getGroupMembers(organizationId).stream()
                .map(KeycloakUserId::id)
                .collect(Collectors.toSet());
        if (memberIds.contains(keycloakUserId)) {
            throw new BusinessException("ADMIN-4009", "error.admin.already_member",
                    "Already a member of this organization");
        }
        membershipRequestRepository.findByOrganizationIdAndKeycloakUserIdAndRequestTypeAndStatus(
                organizationId, keycloakUserId, MembershipRequestType.JOIN_REQUEST, MembershipRequestStatus.PENDING)
                .ifPresent(existing -> {
                    throw new BusinessException("ADMIN-4012", "error.admin.join_request_already_pending",
                            "A join request is already pending for this organization");
                });

        OrganizationMembershipRequest request = new OrganizationMembershipRequest();
        request.setOrganizationId(organizationId);
        request.setKeycloakUserId(keycloakUserId);
        request.setRequestType(MembershipRequestType.JOIN_REQUEST);
        request.setInitiatedByKeycloakUserId(keycloakUserId);

        if (!group.membershipRequiresApproval()) {
            request.setStatus(MembershipRequestStatus.APPROVED);
            request.setResolvedAt(Instant.now());
            membershipRequestRepository.save(request);
            keycloakAdminClient.addUserToGroup(keycloakUserId, organizationId);
            return true;
        }
        membershipRequestRepository.save(request);
        return false;
    }

    @Override
    @Transactional
    public void leaveOrganization(String organizationId, String keycloakUserId) {
        keycloakAdminClient.getGroup(organizationId); // throws a clean 404 if the id is garbage
        Set<String> memberIds = keycloakAdminClient.getGroupMembers(organizationId).stream()
                .map(KeycloakUserId::id)
                .collect(Collectors.toSet());
        if (!memberIds.contains(keycloakUserId)) {
            throw new BusinessException("USER-4009", "error.admin.not_a_member", "Not a member of this organization");
        }
        keycloakAdminClient.removeUserFromGroup(keycloakUserId, organizationId);
        // No last-manager protection here (unlike AdminOrganizationService.removeOrganizationManager) -
        // a manager can always leave their own org; PLATFORM scope can reassign a manager afterward
        // if that orphans it.
        organizationManagerRepository.deleteByOrganizationIdAndKeycloakUserId(organizationId, keycloakUserId);
    }

    @Override
    public List<OrganizationSearchResult> listMyOrganizations(String keycloakUserId) {
        return keycloakAdminClient.getUserGroups(keycloakUserId).stream()
                .map(g -> new OrganizationSearchResult(g.id(), g.name(), g.coverImageUrl(), g.logoImageUrl(), keycloakAdminClient.getGroupMembers(g.id()).size()))
                .toList();
    }

    private OrganizationMembershipRequest loadOwnPendingInvite(Long requestId, String keycloakUserId) {
        OrganizationMembershipRequest request = membershipRequestRepository.findById(requestId)
                .orElseThrow(() -> new BusinessException("ADMIN-4040", "error.admin.row_not_found", "No such request: " + requestId));
        if (request.getRequestType() != MembershipRequestType.INVITE || request.getStatus() != MembershipRequestStatus.PENDING) {
            throw new BusinessException("ADMIN-4011", "error.admin.request_not_pending", "This request is not a pending invite");
        }
        if (!request.getKeycloakUserId().equals(keycloakUserId)) {
            throw new BusinessException("USER-4010", "error.admin.invite_not_yours", "This invite is not addressed to you");
        }
        return request;
    }
}
