CREATE TABLE user_core (
    id              VARCHAR(36)  NOT NULL PRIMARY KEY,
    keycloak_user_id VARCHAR(64) NOT NULL,
    username        VARCHAR(100) NOT NULL,
    email           VARCHAR(255) NOT NULL,
    status          VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE',
    last_synced_at  DATETIME(6),
    created_at      DATETIME(6)  NOT NULL,
    CONSTRAINT uq_user_core_keycloak_id UNIQUE (keycloak_user_id),
    CONSTRAINT uq_user_core_username UNIQUE (username),
    CONSTRAINT uq_user_core_email UNIQUE (email)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
