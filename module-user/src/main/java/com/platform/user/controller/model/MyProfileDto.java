package com.platform.user.controller.model;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

/**
 * Combined view of everything the current user can see/edit about themselves: identity fields
 * (username/email/firstName/lastName) read live from Keycloak, plus the MySQL-only categories
 * (UserProfile, UserContact, UserConsent) that never leave this app.
 */
public record MyProfileDto(
        String username,
        String email,
        String firstName,
        String lastName,
        LocalDate birthDate,
        String avatarUrl,
        String locale,
        String phoneNumber,
        String alternateEmail,
        String addressLine,
        String city,
        String country,
        List<ConsentDto> consents) {

    public record ConsentDto(String consentType, String legalBasis, String purpose, Instant grantedAt, Instant revokedAt) {
    }
}
