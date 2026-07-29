package com.platform.user.profile;

import java.time.LocalDate;

public record UpdateMyProfileRequest(
        String firstName,
        String lastName,
        String email,
        LocalDate birthDate,
        String avatarUrl,
        String locale,
        String phoneNumber,
        String alternateEmail,
        String addressLine,
        String city,
        String country) {
}