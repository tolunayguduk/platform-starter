package com.platform.user.entity;

import com.platform.user.constant.MembershipRequestStatus;
import com.platform.user.constant.MembershipRequestType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

/**
 * Local workflow state - not identity, and not mirrored Keycloak data. Keycloak's group
 * membership model only has member/not-member; "pending" only exists here. Approving a request
 * (either an INVITE the target user accepts, or a JOIN_REQUEST a manager approves) is the single
 * trigger point where real Keycloak group membership actually gets granted - see
 * OrganizationMembershipService/AdminOrganizationService.
 */
@Entity
@Table(name = "organization_membership_request")
public class OrganizationMembershipRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "organization_id", nullable = false)
    private String organizationId;

    @Column(name = "keycloak_user_id", nullable = false)
    private String keycloakUserId;

    @Enumerated(EnumType.STRING)
    @Column(name = "request_type", nullable = false)
    private MembershipRequestType requestType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MembershipRequestStatus status = MembershipRequestStatus.PENDING;

    @Column(name = "initiated_by_keycloak_user_id", nullable = false)
    private String initiatedByKeycloakUserId;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "resolved_at")
    private Instant resolvedAt;

    public Long getId() {
        return id;
    }

    public String getOrganizationId() {
        return organizationId;
    }

    public void setOrganizationId(String organizationId) {
        this.organizationId = organizationId;
    }

    public String getKeycloakUserId() {
        return keycloakUserId;
    }

    public void setKeycloakUserId(String keycloakUserId) {
        this.keycloakUserId = keycloakUserId;
    }

    public MembershipRequestType getRequestType() {
        return requestType;
    }

    public void setRequestType(MembershipRequestType requestType) {
        this.requestType = requestType;
    }

    public MembershipRequestStatus getStatus() {
        return status;
    }

    public void setStatus(MembershipRequestStatus status) {
        this.status = status;
    }

    public String getInitiatedByKeycloakUserId() {
        return initiatedByKeycloakUserId;
    }

    public void setInitiatedByKeycloakUserId(String initiatedByKeycloakUserId) {
        this.initiatedByKeycloakUserId = initiatedByKeycloakUserId;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getResolvedAt() {
        return resolvedAt;
    }

    public void setResolvedAt(Instant resolvedAt) {
        this.resolvedAt = resolvedAt;
    }
}
