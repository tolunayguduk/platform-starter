package com.platform.user.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.envers.Audited;

import java.time.LocalDate;

/**
 * GDPR category: basic identity data. Separate table from the Keycloak-mirrored identity fields
 * so this category can be exported/deleted independently on a "right to erasure" request.
 */
@Entity
@Table(name = "user_profile")
@Audited
public class UserProfile {

    /** Keycloak user id ("sub" claim) - the only identifier for a user anywhere in this app now. */
    @Id
    @Column(name = "keycloak_user_id")
    private String keycloakUserId;

    private String fullName;
    private LocalDate birthDate;
    private String avatarUrl;
    private String locale;

    @Column(name = "deleted_at")
    private java.time.Instant deletedAt; // right-to-erasure support: soft delete + anonymize

    public String getKeycloakUserId() {
        return keycloakUserId;
    }

    public void setKeycloakUserId(String keycloakUserId) {
        this.keycloakUserId = keycloakUserId;
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
