package com.platform.error;

import org.springframework.http.HttpStatus;

/**
 * Unexpected, infrastructure-level failures: DB timeout, external service unreachable,
 * data integrity violation that should never happen in normal operation.
 *
 * Logged at ERROR with full stack trace, triggers monitoring/alerting.
 * Client only ever receives a generic message - technicalMessage / cause is never exposed.
 */
public class TechnicalException extends AppException {

    public TechnicalException(String errorCode, String technicalMessage) {
        super(errorCode, "error.technical.generic", HttpStatus.INTERNAL_SERVER_ERROR, technicalMessage);
    }

    public TechnicalException(String errorCode, String technicalMessage, Throwable cause) {
        super(errorCode, "error.technical.generic", HttpStatus.INTERNAL_SERVER_ERROR, technicalMessage, cause);
    }

    @Override
    public boolean isExpected() {
        return false;
    }
}
