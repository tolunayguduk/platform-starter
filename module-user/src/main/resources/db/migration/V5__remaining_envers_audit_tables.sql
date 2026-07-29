-- Remaining _AUD shadow tables following the user_core_AUD pattern from V4, for the other
-- @Audited entities: UserProfile, UserContact, UserConsent, RolePermission.
-- REVTYPE: 0=ADD, 1=MOD, 2=DEL (Envers default).

CREATE TABLE user_profile_AUD (
    user_id     VARCHAR(36) NOT NULL,
    rev         INT         NOT NULL,
    revtype     TINYINT,
    full_name   VARCHAR(255),
    birth_date  DATE,
    avatar_url  VARCHAR(500),
    locale      VARCHAR(10),
    deleted_at  DATETIME(6),
    PRIMARY KEY (user_id, rev),
    CONSTRAINT fk_user_profile_aud_rev FOREIGN KEY (rev) REFERENCES platform_rev_info (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE user_contact_AUD (
    user_id         VARCHAR(36) NOT NULL,
    rev             INT         NOT NULL,
    revtype         TINYINT,
    phone_number    VARCHAR(30),
    alternate_email VARCHAR(255),
    address_line    VARCHAR(500),
    city            VARCHAR(100),
    country         VARCHAR(100),
    PRIMARY KEY (user_id, rev),
    CONSTRAINT fk_user_contact_aud_rev FOREIGN KEY (rev) REFERENCES platform_rev_info (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE user_consent_AUD (
    id            VARCHAR(36) NOT NULL,
    rev           INT         NOT NULL,
    revtype       TINYINT,
    user_id       VARCHAR(36),
    consent_type  VARCHAR(50),
    legal_basis   VARCHAR(50),
    purpose       VARCHAR(255),
    granted_at    DATETIME(6),
    revoked_at    DATETIME(6),
    ip_address    VARCHAR(45),
    PRIMARY KEY (id, rev),
    CONSTRAINT fk_user_consent_aud_rev FOREIGN KEY (rev) REFERENCES platform_rev_info (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- role_id/permission_id are plain FK-value columns here (not audited sub-entities), since
-- RolePermission.role/.permission are mapped with @Audited(targetAuditMode = NOT_AUDITED).
CREATE TABLE role_permission_AUD (
    id            BIGINT NOT NULL,
    rev           INT    NOT NULL,
    revtype       TINYINT,
    role_id       BIGINT,
    permission_id BIGINT,
    PRIMARY KEY (id, rev),
    CONSTRAINT fk_role_permission_aud_rev FOREIGN KEY (rev) REFERENCES platform_rev_info (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
