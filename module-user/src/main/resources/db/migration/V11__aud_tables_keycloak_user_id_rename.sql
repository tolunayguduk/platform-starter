-- V10 renamed user_profile/user_contact/user_consent's user_id column to keycloak_user_id, but
-- missed their Envers _aud shadow tables - Hibernate's schema validation requires every @Audited
-- column to have a matching column in its _aud table by name, so this broke startup. Fixing here
-- rather than editing V10, per the project's "never edit an applied migration" convention.

ALTER TABLE user_profile_aud CHANGE COLUMN user_id keycloak_user_id VARCHAR(64) NOT NULL;
ALTER TABLE user_contact_aud CHANGE COLUMN user_id keycloak_user_id VARCHAR(64) NOT NULL;
ALTER TABLE user_consent_aud CHANGE COLUMN user_id keycloak_user_id VARCHAR(64);
