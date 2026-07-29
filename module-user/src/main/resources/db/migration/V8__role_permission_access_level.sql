-- Lets a role see a control as DISABLED without actually being authorized to use it (e.g. MANAGER
-- sees "Raporu Onayla" greyed out), distinct from a permission that's simply absent for a role
-- (falls back to permission.ui_policy, e.g. hidden for USER). GRANTED rows are the only ones the
-- real authorization check (MatrixPermissionEvaluator) ever looks at - VISIBLE_DENIED is UI-only.
ALTER TABLE role_permission
    ADD COLUMN access_level VARCHAR(30) NOT NULL DEFAULT 'GRANTED';

-- USER only sees this control at all when a role has been explicitly configured to see it
-- disabled (see MANAGER below) - the default for everyone else is now "hidden", not "disabled".
UPDATE permission SET ui_policy = 'HIDE_IF_DENIED' WHERE `key` = 'report:approve';

-- MANAGER can no longer actually approve reports (that grant is removed) - they instead get a
-- UI-only "visible but disabled" row so the button still renders, just greyed out.
DELETE FROM role_permission
WHERE role_id = (SELECT id FROM role WHERE name = 'MANAGER')
  AND permission_id = (SELECT id FROM permission WHERE `key` = 'report:approve');

INSERT INTO role_permission (role_id, permission_id, access_level)
SELECT r.id, p.id, 'VISIBLE_DENIED'
FROM role r, permission p
WHERE r.name = 'MANAGER' AND p.`key` = 'report:approve';