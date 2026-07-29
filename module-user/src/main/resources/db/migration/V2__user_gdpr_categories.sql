CREATE TABLE user_profile (
    user_id     VARCHAR(36)  NOT NULL PRIMARY KEY,
    full_name   VARCHAR(255),
    birth_date  DATE,
    avatar_url  VARCHAR(500),
    locale      VARCHAR(10),
    deleted_at  DATETIME(6),
    CONSTRAINT fk_user_profile_core FOREIGN KEY (user_id) REFERENCES user_core (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE user_contact (
    user_id         VARCHAR(36)  NOT NULL PRIMARY KEY,
    phone_number    VARCHAR(30),
    alternate_email VARCHAR(255),
    address_line    VARCHAR(500),
    city            VARCHAR(100),
    country         VARCHAR(100),
    CONSTRAINT fk_user_contact_core FOREIGN KEY (user_id) REFERENCES user_core (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE user_consent (
    id            VARCHAR(36)  NOT NULL PRIMARY KEY,
    user_id       VARCHAR(36)  NOT NULL,
    consent_type  VARCHAR(50)  NOT NULL,
    legal_basis   VARCHAR(50)  NOT NULL,
    purpose       VARCHAR(255),
    granted_at    DATETIME(6),
    revoked_at    DATETIME(6),
    ip_address    VARCHAR(45),
    CONSTRAINT fk_user_consent_core FOREIGN KEY (user_id) REFERENCES user_core (id),
    INDEX idx_user_consent_user (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
