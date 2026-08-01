package com.platform.user.service.model;

import java.time.Instant;

public record AdminUserAuditEventResult(Instant time, String operationType, String resourcePath, String representation) {
}
