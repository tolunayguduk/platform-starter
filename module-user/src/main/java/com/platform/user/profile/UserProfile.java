package com.platform.user.profile;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.envers.Audited;

import java.time.LocalDate;

/**
 * GDPR category: basic identity data. Separate table from user_core so this category can be
 * exported/deleted independently on a "right to erasure" request without touching auth identity.
 */
@Entity
@Table(name = "user_profile")
@Audited
public class UserProfile {

    @Id
    @Column(name = "user_id")
    private String userId; // FK to user_core.id, 1:1

    private String fullName;
    private LocalDate birthDate;
    private String avatarUrl;
    private String locale;

    @Column(name = "deleted_at")
    private java.time.Instant deletedAt; // right-to-erasure support: soft delete + anonymize

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public LocalDate getBirthDate() {
        return birthDate;
    }

    public void setBirthDate(LocalDate birthDate) {
        this.birthDate = birthDate;
    }

    public String getAvatarUrl() {
        return avatarUrl;
    }

    public void setAvatarUrl(String avatarUrl) {
        this.avatarUrl = avatarUrl;
    }

    public String getLocale() {
        return locale;
    }

    public void setLocale(String locale) {
        this.locale = locale;
    }

    public java.time.Instant getDeletedAt() {
        return deletedAt;
    }

    public void setDeletedAt(java.time.Instant deletedAt) {
        this.deletedAt = deletedAt;
    }
}
