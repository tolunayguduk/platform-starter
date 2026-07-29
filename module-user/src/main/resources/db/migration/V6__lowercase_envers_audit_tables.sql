-- MySQL runs with lower_case_table_names=0 in the docker-compose setup (Linux container), so table
-- names are case-sensitive. V4/V5 created the Envers _AUD shadow tables with an uppercase suffix
-- (e.g. role_permission_AUD), but Hibernate's default physical naming strategy lowercases identifiers
-- when validating the schema, so it looks for role_permission_aud and fails with "missing table".
-- Renaming to lowercase here (rather than editing V4/V5) avoids invalidating their checksums.

RENAME TABLE user_core_AUD TO user_core_aud;
RENAME TABLE user_profile_AUD TO user_profile_aud;
RENAME TABLE user_contact_AUD TO user_contact_aud;
RENAME TABLE user_consent_AUD TO user_consent_aud;
RENAME TABLE role_permission_AUD TO role_permission_aud;