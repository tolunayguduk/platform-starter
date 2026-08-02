import type { AdminRole } from '../../../types/admin';

type TranslateFn = (key: string, options?: Record<string, unknown>) => string;

/** Shared by EditUserModal and BulkAssignRolesModal - a role can be unpickable for two different
 * reasons (disabled, or PLATFORM-scope while the acting admin is only ORGANIZATION-scoped), each
 * with its own explanatory label so it's clear why, not just greyed out with no context. */
export function buildRoleSelectOptions(availableRoles: AdminRole[], callerIsPlatformScoped: boolean, t: TranslateFn) {
  return availableRoles.map((r) => {
    if (!r.enabled) {
      return { value: r.name, label: t('admin.editUser.roleDisabledLabel', { role: r.name }), disabled: true };
    }
    if (r.scope === 'PLATFORM' && !callerIsPlatformScoped) {
      return { value: r.name, label: t('admin.editUser.rolePlatformOnlyLabel', { role: r.name }), disabled: true };
    }
    return { value: r.name, label: r.name, disabled: false };
  });
}
