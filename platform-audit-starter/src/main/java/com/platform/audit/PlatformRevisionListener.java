package com.platform.audit;

import jakarta.servlet.http.HttpServletRequest;
import org.hibernate.envers.RevisionListener;
import org.slf4j.MDC;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class PlatformRevisionListener implements RevisionListener {

    private static final String TRACE_ID_MDC_KEY = "traceId"; // shared convention with platform-observability

    // NOTE: for a real multi-instance deployment this must be looked up (e.g. last row in
    // platform_rev_info) rather than kept in a static field - kept simple here for the skeleton.
    private static volatile String lastHash = "";

    @Override
    public void newRevision(Object revisionEntity) {
        PlatformRevisionEntity revision = (PlatformRevisionEntity) revisionEntity;

        revision.setUsername(currentUsername());
        revision.setClientIp(currentClientIp());
        revision.setTraceId(MDC.get(TRACE_ID_MDC_KEY));

        String previousHash = lastHash;
        String hash = computeHash(previousHash, revision);
        revision.setPreviousHash(previousHash);
        revision.setRecordHash(hash);
        lastHash = hash;
    }

    private String currentUsername() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null ? auth.getName() : "system";
    }

    private String currentClientIp() {
        ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attrs == null) {
            return "internal";
        }
        HttpServletRequest request = attrs.getRequest();
        String forwardedFor = request.getHeader("X-Forwarded-For");
        return forwardedFor != null ? forwardedFor.split(",")[0].trim() : request.getRemoteAddr();
    }

    private String computeHash(String previousHash, PlatformRevisionEntity revision) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            String payload = previousHash + "|" + revision.getUsername() + "|" + revision.getClientIp()
                    + "|" + revision.getTraceId() + "|" + System.nanoTime();
            byte[] hashBytes = digest.digest(payload.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : hashBytes) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }
}
