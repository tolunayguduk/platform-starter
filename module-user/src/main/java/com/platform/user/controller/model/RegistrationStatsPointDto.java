package com.platform.user.controller.model;

/** One point on the registrations-over-time chart - bucket is a pre-formatted label (e.g. "14:00", "07-26", "2026-07"). */
public record RegistrationStatsPointDto(String bucket, long count) {
}
