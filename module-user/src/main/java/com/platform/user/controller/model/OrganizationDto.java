package com.platform.user.controller.model;

public record OrganizationDto(String id, String name, String description, int memberCount, boolean membershipRequiresApproval) {
}
