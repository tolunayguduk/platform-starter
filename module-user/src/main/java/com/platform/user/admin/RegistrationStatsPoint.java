package com.platform.user.admin;

/** One point on the registrations-over-time chart - bucket is a pre-formatted label (e.g. "14:00", "07-26", "2026-07"). */
public record RegistrationStatsPoint(String bucket, long count) {
}