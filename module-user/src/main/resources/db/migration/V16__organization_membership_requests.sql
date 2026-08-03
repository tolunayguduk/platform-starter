-- Local workflow state for organization membership - Keycloak only has member/not-member, no
-- "pending" concept, so this can't be Keycloak-native the way roles/groups themselves are.
-- Approving a row (INVITE accepted by the target user, or JOIN_REQUEST approved by a manager) is
-- the single trigger point where real Keycloak group membership is actually granted.
CREATE TABLE organization_membership_request (
    id                             BIGINT AUTO_INCREMENT PRIMARY KEY,
    organization_id                VARCHAR(64) NOT NULL,
    keycloak_user_id               VARCHAR(64) NOT NULL,
    request_type                   VARCHAR(20) NOT NULL,
    status                         VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    initiated_by_keycloak_user_id  VARCHAR(64) NOT NULL,
    created_at                     TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    resolved_at                    TIMESTAMP NULL,
    INDEX idx_omr_org (organization_id),
    INDEX idx_omr_user (keycloak_user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
