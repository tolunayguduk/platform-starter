package com.platform.error;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.stream.Collectors;

/**
 * Single place where every exception in the system becomes an HTTP response.
 *
 * Rule this codebase follows: this handler is a safety net, NOT the place where
 * authorization decisions are made - @PreAuthorize on each endpoint remains the real
 * security boundary. This class only decides how to *report* what already happened.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);
    public static final String TRACE_ID_MDC_KEY = "traceId";

    @ExceptionHandler(AppException.class)
    public ResponseEntity<ErrorResponse> handleAppException(AppException ex, HttpServletRequest request) {
        if (ex.isExpected()) {
            log.warn("[{}] {} - {}", ex.getErrorCode(), request.getRequestURI(), ex.getMessage());
        } else {
            log.error("[{}] {} - {}", ex.getErrorCode(), request.getRequestURI(), ex.getMessage(), ex);
            // TODO: hook into alerting (Sentry / Slack webhook) here for unexpected TechnicalExceptions
        }
        return ResponseEntity.status(ex.getHttpStatus())
                .body(toResponse(ex.getErrorCode(), ex.getUserMessage(), request));
    }

    /**
     * @PreAuthorize denials throw here (Spring Security 6's AuthorizationDeniedException extends
     * this), but they're thrown from inside the controller method's AOP proxy - i.e. INSIDE the
     * DispatcherServlet, not the security filter chain - so this @RestControllerAdvice sees them
     * before ExceptionTranslationFilter ever gets a chance to. Without this handler they fall
     * through to handleUnexpected() below and get reported as a 500, masking a plain 403.
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDenied(AccessDeniedException ex, HttpServletRequest request) {
        log.warn("[COMMON-4030] {} - access denied: {}", request.getRequestURI(), ex.getMessage());
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(toResponse("COMMON-4030", "error.access_denied", request));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex, HttpServletRequest request) {
        String detail = ex.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining("; "));
        log.warn("[COMMON-4000] {} - validation failed: {}", request.getRequestURI(), detail);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(toResponse("COMMON-4000", detail, request));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpected(Exception ex, HttpServletRequest request) {
        log.error("[COMMON-5000] Unhandled exception at {}", request.getRequestURI(), ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(toResponse("COMMON-5000", "error.technical.generic", request));
    }

    private ErrorResponse toResponse(String errorCode, String message, HttpServletRequest request) {
        String traceId = MDC.get(TRACE_ID_MDC_KEY);
        return new ErrorResponse(errorCode, message, Instant.now(), traceId, request.getRequestURI());
    }
}
