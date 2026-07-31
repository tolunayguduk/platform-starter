package com.platform.user.service.model;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public record ProfileResult(
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
        List<ConsentResult> consents) {

    public record ConsentResult(String consentType, String legalBasis, String purpose, Instant grantedAt, Instant revokedAt) {
    }
}
