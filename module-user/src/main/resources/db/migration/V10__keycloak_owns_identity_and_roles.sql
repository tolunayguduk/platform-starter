-- Keycloak becomes the sole store for user identity and role definitions/assignments. user_core
-- was a redundant cache of fields Keycloak already has (username/email/enabled/createdTimestamp);
-- the local `role` table hardcoded a copy of realm role names that had to be kept in sync by hand.
-- Both are removed here rather than edited in V1/V3, per the project's "never edit an applied
-- migration" convention (see V6's note).

-- 1) GDPR category tables now key directly off the Keycloak user id ("sub" claim) instead of the
--    local user_core.id UUID - there is no local identity row left to join through.
ALTER TABLE user_profile DROP FOREIGN KEY fk_user_profile_core;
ALTER TABLE user_contact DROP FOREIGN KEY fk_user_contact_core;
ALTER TABLE user_consent DROP FOREIGN KEY fk_user_consent_core;

ALTER TABLE user_profile CHANGE COLUMN user_id keycloak_user_id VARCHAR(64) NOT NULL;
ALTER TABLE user_contact CHANGE COLUMN user_id keycloak_user_id VARCHAR(64) NOT NULL;
ALTER TABLE user_consent CHANGE COLUMN user_id keycloak_user_id VARCHAR(64) NOT NULL;

DROP INDEX idx_user_consent_user ON user_consent;
CREATE INDEX idx_user_consent_keycloak_user ON user_consent (keycloak_user_id);

-- 2) user_core is now fully redundant - MyProfileService/AdminUserService read identity straight
--    from Keycloak instead of this cache.
DROP TABLE user_core_aud;
DROP TABLE user_core;

-- 3) role_permission (and its Envers history) now stores the Keycloak realm role name directly -
--    roles are no longer duplicated into a local `role` table at all.
ALTER TABLE role_permission ADD COLUMN role_name VARCHAR(100);
UPDATE role_permission rp JOIN role r ON r.id = rp.role_id SET rp.role_name = r.name;
ALTER TABLE role_permission MODIFY COLUMN role_name VARCHAR(100) NOT NULL;
ALTER TABLE role_permission DROP FOREIGN KEY fk_rp_role;
ALTER TABLE role_permission DROP INDEX uq_role_permission;
ALTER TABLE role_permission DROP COLUMN role_id;
ALTER TABLE role_permission ADD CONSTRAINT uq_role_permission UNIQUE (role_name, permission_id);

ALTER TABLE role_permission_aud ADD COLUMN role_name VARCHAR(100);
UPDATE role_permission_aud rpa JOIN role r ON r.id = rpa.role_id SET rpa.role_name = r.name;
ALTER TABLE role_permission_aud DROP COLUMN role_id;

DROP TABLE role;
