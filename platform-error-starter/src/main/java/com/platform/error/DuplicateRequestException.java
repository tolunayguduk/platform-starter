package com.platform.error;

import org.springframework.http.HttpStatus;

/**
 * Thrown when a request arrives with an idempotency key that has already been processed.
 * Business modules (payment, etc.) throw this instead of silently reprocessing.
 */
public class DuplicateRequestException extends BusinessException {

    public DuplicateRequestException(String idempotencyKey) {
        super("COMMON-4090",
                "error.request.duplicate",
                HttpStatus.CONFLICT,
                "Duplicate request for idempotency key: " + idempotencyKey);
    }
}
