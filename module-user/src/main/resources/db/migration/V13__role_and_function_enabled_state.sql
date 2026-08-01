-- Keycloak has no native enabled/disabled concept for realm roles (only users), so a role's
-- enabled state for this app's authorization purposes is tracked here instead - the same category
-- of purely-local authorization policy that role_permission/permission already are (role_name is
-- just a join key into Keycloak's role list, never mirrored identity data). Absence of a row means
-- enabled (matches permission.enabled's default), so every pre-existing role starts enabled.
CREATE TABLE role_state (
    role_name VARCHAR(100) PRIMARY KEY,
    enabled   BOOLEAN NOT NULL DEFAULT TRUE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- A disabled function can't be granted to a role, and any existing grant of it becomes inert
-- (excluded from resolvePermissions/resolveVisibleDeniedPermissions/resolveHiddenPermissions)
-- until re-enabled - see RolePermissionRepository.
ALTER TABLE permission ADD COLUMN enabled BOOLEAN NOT NULL DEFAULT TRUE;
