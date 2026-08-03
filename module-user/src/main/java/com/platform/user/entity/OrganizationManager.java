package com.platform.user.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

/**
 * Local authorization data, not identity - Keycloak Groups have no concept of "who administers
 * this group", only member/not-member. This table is the sole source of organization-admin
 * authority: being a manager of an organization has nothing to do with holding any particular
 * Keycloak role, and nothing to do with mere group membership (see AdminAccessScopeService).
 */
@Entity
@Table(name = "organization_manager")
public class OrganizationManager {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "organization_id", nullable = false)
    private String organizationId;

    @Column(name = "keycloak_user_id", nullable = false)
    private String keycloakUserId;

    @Column(name = "granted_at", nullable = false)
    private Instant grantedAt = Instant.now();

    @Column(name = "granted_by_keycloak_user_id", nullable = false)
    private String grantedByKeycloakUserId;

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

    public Instant getGrantedAt() {
        return grantedAt;
    }

    public void setGrantedAt(Instant grantedAt) {
        this.grantedAt = grantedAt;
    }

    public String getGrantedByKeycloakUserId() {
        return grantedByKeycloakUserId;
    }

    public void setGrantedByKeycloakUserId(String grantedByKeycloakUserId) {
        this.grantedByKeycloakUserId = grantedByKeycloakUserId;
    }
}
