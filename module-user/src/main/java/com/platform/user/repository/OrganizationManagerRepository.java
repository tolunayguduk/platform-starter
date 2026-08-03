package com.platform.user.repository;

import com.platform.user.entity.OrganizationManager;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OrganizationManagerRepository extends JpaRepository<OrganizationManager, Long> {

    List<OrganizationManager> findByKeycloakUserId(String keycloakUserId);

    List<OrganizationManager> findByOrganizationId(String organizationId);

    boolean existsByOrganizationIdAndKeycloakUserId(String organizationId, String keycloakUserId);

    long countByOrganizationId(String organizationId);

    void deleteByOrganizationIdAndKeycloakUserId(String organizationId, String keycloakUserId);
}
