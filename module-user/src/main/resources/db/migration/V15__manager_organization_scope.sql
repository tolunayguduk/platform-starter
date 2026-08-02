-- MANAGER is the role self-registration grants to a user who creates their own organization
-- (see platform.registration.organization-admin-role, RegistrationServiceImpl) - give it
-- ORGANIZATION scope out of the box so the realm's pre-seeded default roles already match the
-- intended design (ADMIN = platform-wide, MANAGER = own organization only) without requiring a
-- manual admin panel step first. If organization-admin-role is ever reconfigured to a different
-- role name, that role's scope still needs setting via the admin panel (or a follow-up migration).
INSERT INTO role_state (role_name, enabled, scope)
VALUES ('MANAGER', TRUE, 'ORGANIZATION')
ON DUPLICATE KEY UPDATE scope = 'ORGANIZATION';
