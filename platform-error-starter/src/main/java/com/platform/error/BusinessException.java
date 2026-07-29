package com.platform.error;

import org.springframework.http.HttpStatus;

/**
 * Expected, business-rule-driven failures: insufficient balance, limit exceeded,
 * duplicate submission, invalid state transition, etc.
 *
 * Logged at WARN, does not trigger alerting, transaction is still rolled back.
 * Maps to a 4xx status by default (override per subclass if needed).
 */
public class BusinessException extends AppException {

    public BusinessException(String errorCode, String userMessage, String technicalMessage) {
        super(errorCode, userMessage, HttpStatus.UNPROCESSABLE_ENTITY, technicalMessage);
    }

    public BusinessException(String errorCode, String userMessage, HttpStatus httpStatus, String technicalMessage) {
        super(errorCode, userMessage, httpStatus, technicalMessage);
    }

    @Override
    public boolean isExpected() {
        return true;
    }
}
