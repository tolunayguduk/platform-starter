import { apiFetch } from './client';

export interface AdminUser {
  id: string;
  username: string;
  email: string;
  fullName: string | null;
  status: string;
  createdAt: string;
  roles: string[];
}

export function fetchAdminUsers(accessToken: string): Promise<AdminUser[]> {
  return apiFetch<AdminUser[]>('/api/admin/users', { accessToken });
}

export interface AdminRole {
  name: string;
  description: string | null;
  enabled: boolean;
}

export function fetchAdminRoles(accessToken: string): Promise<AdminRole[]> {
  return apiFetch<AdminRole[]>('/api/admin/roles', { accessToken });
}

export function createAdminRole(accessToken: string, name: string): Promise<void> {
  return apiFetch<void>('/api/admin/roles', {
    method: 'POST',
    body: { name },
    accessToken,
  });
}

export function updateRoleDescription(accessToken: string, name: string, description: string): Promise<void> {
  return apiFetch<void>(`/api/admin/roles/${encodeURIComponent(name)}`, {
    method: 'PUT',
    body: { description },
    accessToken,
  });
}

export function updateRoleStatus(accessToken: string, name: string, enabled: boolean): Promise<void> {
  return apiFetch<void>(`/api/admin/roles/${encodeURIComponent(name)}/status`, {
    method: 'PUT',
    body: { enabled },
    accessToken,
  });
}

export function deleteAdminRole(accessToken: string, name: string): Promise<void> {
  return apiFetch<void>(`/api/admin/roles/${encodeURIComponent(name)}`, {
    method: 'DELETE',
    accessToken,
  });
}

export function updateUserRoles(accessToken: string, userId: string, roles: string[]): Promise<void> {
  return apiFetch<void>(`/api/admin/users/${userId}/roles`, {
    method: 'PUT',
    body: { roles },
    accessToken,
  });
}

export function updateUserIdentity(
  accessToken: string,
  userId: string,
  identity: { username: string; email: string },
): Promise<void> {
  return apiFetch<void>(`/api/admin/users/${userId}/identity`, {
    method: 'PUT',
    body: identity,
    accessToken,
  });
}

export function updateUserStatus(accessToken: string, userId: string, enabled: boolean): Promise<void> {
  return apiFetch<void>(`/api/admin/users/${userId}/status`, {
    method: 'PUT',
    body: { enabled },
    accessToken,
  });
}

export interface AdminUserAuditEvent {
  time: string;
  operationType: string;
  resourcePath: string;
  representation: string | null;
}

export function fetchAdminUserAuditEvents(accessToken: string, userId: string): Promise<AdminUserAuditEvent[]> {
  return apiFetch<AdminUserAuditEvent[]>(`/api/admin/users/${userId}/audit`, { accessToken });
}

/** Values are "ENABLED" | "DISABLED" | "HIDDEN" - see UiPermissionsService on the backend. */
export function fetchAdminUserUiPermissions(accessToken: string, userId: string): Promise<Record<string, string>> {
  return apiFetch<Record<string, string>>(`/api/admin/users/${userId}/ui-permissions`, { accessToken });
}

export type StatsRange = 'DAY' | 'WEEK' | 'MONTH' | 'YEAR';

export interface RegistrationStatsPoint {
  bucket: string;
  count: number;
}

export function fetchRegistrationStats(accessToken: string, range: StatsRange): Promise<RegistrationStatsPoint[]> {
  return apiFetch<RegistrationStatsPoint[]>(`/api/admin/stats/registrations?range=${range}`, { accessToken });
}

export interface AdminTable {
  key: string;
  hasAudit: boolean;
  editableColumns: string[];
}

export interface AdminTableRows {
  columns: string[];
  primaryKeyColumn: string;
  rows: Record<string, unknown>[];
}

export interface AdminAuditRows {
  columns: string[];
  rows: Record<string, unknown>[];
}

export function fetchAdminTables(accessToken: string): Promise<AdminTable[]> {
  return apiFetch<AdminTable[]>('/api/admin/tables', { accessToken });
}

export function fetchAdminTableRows(accessToken: string, key: string): Promise<AdminTableRows> {
  return apiFetch<AdminTableRows>(`/api/admin/tables/${key}/rows`, { accessToken });
}

export function fetchAdminTableAuditRows(accessToken: string, key: string, pk: string): Promise<AdminAuditRows> {
  return apiFetch<AdminAuditRows>(`/api/admin/tables/${key}/rows/${encodeURIComponent(pk)}/audit`, { accessToken });
}

export function updateAdminTableRow(
  accessToken: string,
  key: string,
  pk: string,
  changes: Record<string, unknown>,
): Promise<Record<string, unknown>> {
  return apiFetch<{ row: Record<string, unknown> }>(`/api/admin/tables/${key}/rows/${encodeURIComponent(pk)}`, {
    method: 'PATCH',
    body: { changes },
    accessToken,
  }).then((res) => res.row);
}

/** PERMISSION (defining a new function) and ROLE_PERMISSION (granting one to a role) support
 * both create and delete - deleting a PERMISSION cascades to every role's grant of it. */
export function createAdminTableRow(
  accessToken: string,
  key: string,
  values: Record<string, unknown>,
): Promise<Record<string, unknown>> {
  return apiFetch<{ row: Record<string, unknown> }>(`/api/admin/tables/${key}/rows`, {
    method: 'POST',
    body: { values },
    accessToken,
  }).then((res) => res.row);
}

export function deleteAdminTableRow(accessToken: string, key: string, pk: string): Promise<void> {
  return apiFetch<void>(`/api/admin/tables/${key}/rows/${encodeURIComponent(pk)}`, {
    method: 'DELETE',
    accessToken,
  });
}