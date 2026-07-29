package com.platform.error;

import org.springframework.http.HttpStatus;

/**
 * Base type for every exception thrown by platform code.
 *
 * errorCode   - stable machine-readable code, e.g. "PAY-4001" (safe to show to clients / support)
 * userMessage - i18n key or human message safe to return to the end user
 * httpStatus  - the HTTP status this exception maps to
 *
 * Business vs Technical vs Security is decided by the concrete subclass, which also decides
 * the default log level and whether monitoring/alerting should be triggered.
 */
public abstract class AppException extends RuntimeException {

    private final String errorCode;
    private final String userMessage;
    private final HttpStatus httpStatus;

    protected AppException(String errorCode, String userMessage, HttpStatus httpStatus, String technicalMessage) {
        super(technicalMessage);
        this.errorCode = errorCode;
        this.userMessage = userMessage;
        this.httpStatus = httpStatus;
    }

    protected AppException(String errorCode, String userMessage, HttpStatus httpStatus, String technicalMessage, Throwable cause) {
        super(technicalMessage, cause);
        this.errorCode = errorCode;
        this.userMessage = userMessage;
        this.httpStatus = httpStatus;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public String getUserMessage() {
        return userMessage;
    }

    public HttpStatus getHttpStatus() {
        return httpStatus;
    }

    /**
     * Whether this exception represents an *expected* condition (WARN log, no alert)
     * or an unexpected one (ERROR log, should page/alert). Overridden per branch.
     */
    public abstract boolean isExpected();
}
