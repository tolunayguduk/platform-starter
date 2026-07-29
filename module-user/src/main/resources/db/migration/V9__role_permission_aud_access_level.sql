-- Follow-up to V8 rather than an edit to it (same reasoning as V6: editing an already-applied
-- migration invalidates its checksum). RolePermission is @Audited, so its Envers shadow table
-- needs the same new column V8 added to role_permission.
ALTER TABLE role_permission_aud
    ADD COLUMN access_level VARCHAR(30);