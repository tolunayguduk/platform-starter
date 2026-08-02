-- Whether - and how far - a role's authority reaches into the admin panel at all. Same
-- purely-local-policy category as role_state.enabled. Absence of a row already means
-- "enabled=true" (see V13); it now also means "scope=NONE" - not an admin-panel role at all,
-- which is what makes ORGANIZATION/PLATFORM an explicit, deliberate opt-in rather than something
-- every newly created role (e.g. a plain "USER") would silently get for free.
ALTER TABLE role_state ADD COLUMN scope VARCHAR(20) NOT NULL DEFAULT 'NONE';

-- ADMIN is the one role this admin panel is entirely gated on today - it needs an explicit
-- PLATFORM row so the system doesn't lose its only realm-wide admin on upgrade.
INSERT INTO role_state (role_name, enabled, scope)
VALUES ('ADMIN', TRUE, 'PLATFORM')
ON DUPLICATE KEY UPDATE scope = 'PLATFORM';
