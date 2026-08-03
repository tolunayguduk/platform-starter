package com.platform.user.service.model;

public record OrganizationResult(String id, String name, String description, String coverImageUrl, String logoImageUrl,
                                  int memberCount, boolean membershipRequiresApproval) {
}
