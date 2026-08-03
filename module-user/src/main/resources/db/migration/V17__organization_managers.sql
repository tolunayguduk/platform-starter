CREATE TABLE organization_manager (
    id                           BIGINT AUTO_INCREMENT PRIMARY KEY,
    organization_id              VARCHAR(64) NOT NULL,
    keycloak_user_id             VARCHAR(64) NOT NULL,
    granted_at                   TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    granted_by_keycloak_user_id  VARCHAR(64) NOT NULL,
    UNIQUE KEY uq_om_org_user (organization_id, keycloak_user_id),
    INDEX idx_om_user (keycloak_user_id),
    INDEX idx_om_org (organization_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- RoleScope.ORGANIZATION no longer means anything now that org-admin authority is table-driven -
-- collapse any existing rows (today: MANAGER, see V15) down to NONE.
UPDATE role_state SET scope = 'NONE' WHERE scope = 'ORGANIZATION';
