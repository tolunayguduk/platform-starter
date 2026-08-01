package com.platform.user.controller.model;

import java.time.Instant;

public record AdminUserAuditEventDto(Instant time, String operationType, String resourcePath, String representation) {
}
