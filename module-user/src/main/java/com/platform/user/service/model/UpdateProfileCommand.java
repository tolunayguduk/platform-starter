package com.platform.user.service.model;

import java.time.LocalDate;

public record UpdateProfileCommand(
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
