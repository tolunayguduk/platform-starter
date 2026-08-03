package com.platform.security.integration.keycloak.model;

/** A Keycloak Group, flattened to what this app actually uses - description and
 * membershipRequiresApproval both come from group attributes (Groups have no native fields for
 * either, unlike Roles' native description); that mapping detail stays inside
 * KeycloakAdminClientImpl so every caller sees this flat shape. */
public record KeycloakGroup(String id, String name, String description, boolean membershipRequiresApproval) {
}
