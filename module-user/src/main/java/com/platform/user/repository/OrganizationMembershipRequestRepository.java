package com.platform.user.repository;

import com.platform.user.constant.MembershipRequestStatus;
import com.platform.user.constant.MembershipRequestType;
import com.platform.user.entity.OrganizationMembershipRequest;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface OrganizationMembershipRequestRepository extends JpaRepository<OrganizationMembershipRequest, Long> {

    List<OrganizationMembershipRequest> findByKeycloakUserIdAndRequestTypeAndStatus(
            String keycloakUserId, MembershipRequestType requestType, MembershipRequestStatus status);

    List<OrganizationMembershipRequest> findByOrganizationIdAndRequestTypeAndStatus(
            String organizationId, MembershipRequestType requestType, MembershipRequestStatus status);

    Optional<OrganizationMembershipRequest> findByOrganizationIdAndKeycloakUserIdAndRequestTypeAndStatus(
            String organizationId, String keycloakUserId, MembershipRequestType requestType, MembershipRequestStatus status);

    /** Both INVITE-accepted and JOIN_REQUEST-approved rows count as "joined this organization" -
     * backs the organization-scoped registration stats (see AdminUserServiceImpl). */
    List<OrganizationMembershipRequest> findByOrganizationIdInAndStatus(Set<String> organizationIds, MembershipRequestStatus status);
}
