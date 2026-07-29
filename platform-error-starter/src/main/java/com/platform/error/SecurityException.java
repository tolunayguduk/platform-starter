package com.platform.error;

import org.springframework.http.HttpStatus;

/**
 * Authentication / authorization failures. Kept separate from BusinessException so that
 * permission-matrix denials, expired tokens, etc. are trivially distinguishable in logs
 * and metrics from ordinary business-rule rejections.
 */
public class SecurityException extends AppException {

    public SecurityException(String errorCode, String userMessage, String technicalMessage) {
        super(errorCode, userMessage, HttpStatus.FORBIDDEN, technicalMessage);
    }

    @Override
    public boolean isExpected() {
        return true;
    }
}
