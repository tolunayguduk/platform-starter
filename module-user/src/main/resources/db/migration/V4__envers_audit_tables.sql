CREATE TABLE platform_rev_info (
    id            INT AUTO_INCREMENT PRIMARY KEY,
    timestamp     BIGINT       NOT NULL,
    username      VARCHAR(150),
    client_ip     VARCHAR(45),
    trace_id      VARCHAR(64),
    record_hash   VARCHAR(64),
    previous_hash VARCHAR(64)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Example _AUD shadow table (Envers convention) for the most sensitive entity, user_core.
-- Use this as the template for user_profile_AUD, user_contact_AUD, role_permission_AUD, etc.
-- as those entities stabilize. REVTYPE: 0=ADD, 1=MOD, 2=DEL (Envers default).
CREATE TABLE user_core_AUD (
    id               VARCHAR(36) NOT NULL,
    rev              INT         NOT NULL,
    revtype          TINYINT,
    keycloak_user_id VARCHAR(64),
    username         VARCHAR(100),
    email            VARCHAR(255),
    status           VARCHAR(20),
    PRIMARY KEY (id, rev),
    CONSTRAINT fk_user_core_aud_rev FOREIGN KEY (rev) REFERENCES platform_rev_info (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Finance-grade hardening (see earlier discussion): the application's runtime DB user should
-- only ever be granted INSERT on *_AUD tables and platform_rev_info, never UPDATE/DELETE.
-- Wire this up as a separate migration/DBA script against the app's runtime MySQL user, e.g.:
--   REVOKE UPDATE, DELETE ON platform_rev_info FROM 'app_runtime_user'@'%';
--   REVOKE UPDATE, DELETE ON user_core_AUD FROM 'app_runtime_user'@'%';
