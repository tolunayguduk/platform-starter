package com.platform.user.service;

import com.platform.user.service.model.AdminUserResult;
import com.platform.user.service.model.OrganizationResult;

import java.util.List;

/**
 * Organizations are Keycloak Groups - never a parallel local table (same
 * Keycloak-is-the-source-of-truth rule as roles/identity). Every method scopes itself through
 * AdminAccessScopeService: a PLATFORM-scoped caller sees/manages every organization; an
 * ORGANIZATION-scoped caller only their own (a user, and therefore an admin, may belong to more
 * than one) - never both, and never create/delete an organization at all (platform-only,
 * destructive/structural changes stay centrally controlled).
 */
public interface AdminOrganizationService {

    List<OrganizationResult> listOrganizations(String callerKeycloakUserId);

    OrganizationResult createOrganization(String name, String callerKeycloakUserId);

    void updateOrganizationDescription(String organizationId, String description, String callerKeycloakUserId);

    void deleteOrganization(String organizationId, String callerKeycloakUserId);

    List<AdminUserResult> getOrganizationMembers(String organizationId, String callerKeycloakUserId);

    /** Exact username/email match only, never a browse/search - lets an organization admin invite
     * a specific known person without exposing the full user directory to them (that would defeat
     * the whole point of isolating organizations from each other). */
    AdminUserResult findUserByIdentifier(String usernameOrEmail, String callerKeycloakUserId);

    void addMember(String organizationId, String keycloakUserId, String callerKeycloakUserId);

    void removeMember(String organizationId, String keycloakUserId, String callerKeycloakUserId);
}
