package com.platform.user.profile;

import java.time.LocalDate;
import java.util.List;

/**
 * Combined view of everything the current user can see/edit about themselves: identity fields
 * (username/email/firstName/lastName) read live from Keycloak, plus the MySQL-only categories
 * (UserProfile, UserContact, UserConsent) that never leave this app.
 */
public record MyProfileView(
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
        List<ConsentView> consents) {

    public record ConsentView(String consentType, String legalBasis, String purpose,
                               java.time.Instant grantedAt, java.time.Instant revokedAt) {
    }
}