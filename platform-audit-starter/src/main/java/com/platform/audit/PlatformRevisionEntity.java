package com.platform.audit;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.envers.RevisionEntity;
import org.hibernate.envers.RevisionNumber;
import org.hibernate.envers.RevisionTimestamp;

import java.io.Serializable;

/**
 * Replaces Envers' default REVINFO table (revision number + timestamp only) with one that
 * also records who made the change, from where, and a hash chaining the previous revision -
 * required to detect if someone edited the _AUD tables directly at the DB level.
 *
 * <p>Deliberately does NOT extend {@code DefaultRevisionEntity}: its inherited {@code id} field
 * uses {@code @GeneratedValue} with no explicit strategy, which Hibernate 6 resolves to a
 * sequence-emulation table rather than the plain {@code AUTO_INCREMENT} column Flyway creates.
 */
@Entity
@Table(name = "platform_rev_info")
@RevisionEntity(PlatformRevisionListener.class)
public class PlatformRevisionEntity implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @RevisionNumber
    private int id;

    @RevisionTimestamp
    private long timestamp;

    @Column(name = "username")
    private String username;

    @Column(name = "client_ip")
    private String clientIp;

    @Column(name = "trace_id")
    private String traceId;

    /**
     * SHA-256 of (previousHash + this revision's serialized change), computed by
     * AuditIntegrityService. Any row edited or deleted directly in the DB breaks the chain
     * on the next verification pass.
     */
    @Column(name = "record_hash", length = 64)
    private String recordHash;

    @Column(name = "previous_hash", length = 64)
    private String previousHash;

    public int getId() {
        return id;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getClientIp() {
        return clientIp;
    }

    public void setClientIp(String clientIp) {
        this.clientIp = clientIp;
    }

    public String getTraceId() {
        return traceId;
    }

    public void setTraceId(String traceId) {
        this.traceId = traceId;
    }

    public String getRecordHash() {
        return recordHash;
    }

    public void setRecordHash(String recordHash) {
        this.recordHash = recordHash;
    }

    public String getPreviousHash() {
        return previousHash;
    }

    public void setPreviousHash(String previousHash) {
        this.previousHash = previousHash;
    }
}
