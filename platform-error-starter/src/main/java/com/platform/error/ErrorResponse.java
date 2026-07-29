package com.platform.error;

import java.time.Instant;

/**
 * Uniform error body returned by every endpoint across every module / future microservice.
 */
public record ErrorResponse(
        String errorCode,
        String message,
        Instant timestamp,
        String traceId,
        String path
) {
}
