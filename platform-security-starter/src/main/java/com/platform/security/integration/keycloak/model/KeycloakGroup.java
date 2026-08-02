package com.platform.security.integration.keycloak.model;

/** A Keycloak Group, flattened to what this app actually uses - description comes from the
 * group's "description" attribute (Groups have no native description field like Roles do); that
 * mapping detail stays inside KeycloakAdminClientImpl so every caller sees this flat shape. */
public record KeycloakGroup(String id, String name, String description) {
}
