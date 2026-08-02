import { apiFetch } from './client';
import type { AdminTable, AdminTableRows, AdminAuditRows, AdminUserAuditEvent, StatsRange, RegistrationStatsPoint } from '../types/admin';
import type { AdminRole, AdminUser, Organization, RoleScope } from '../types/admin';

export function fetchAdminUsers(accessToken: string): Promise<AdminUser[]> {
  return apiFetch<AdminUser[]>('/api/admin/users', { accessToken });
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

export function updateRoleScope(accessToken: string, name: string, scope: RoleScope): Promise<void> {
  return apiFetch<void>(`/api/admin/roles/${encodeURIComponent(name)}/scope`, {
    method: 'PUT',
    body: { scope },
    accessToken,
  });
}

export function deleteAdminRole(accessToken: string, name: string): Promise<void> {
  return apiFetch<void>(`/api/admin/roles/${encodeURIComponent(name)}`, {
    method: 'DELETE',
    accessToken,
  });
}

export function fetchOrganizations(accessToken: string): Promise<Organization[]> {
  return apiFetch<Organization[]>('/api/admin/organizations', { accessToken });
}

export function createOrganization(accessToken: string, name: string): Promise<Organization> {
  return apiFetch<Organization>('/api/admin/organizations', {
    method: 'POST',
    body: { name },
    accessToken,
  });
}

export function updateOrganizationDescription(accessToken: string, id: string, description: string): Promise<void> {
  return apiFetch<void>(`/api/admin/organizations/${encodeURIComponent(id)}`, {
    method: 'PUT',
    body: { description },
    accessToken,
  });
}

export function deleteOrganization(accessToken: string, id: string): Promise<void> {
  return apiFetch<void>(`/api/admin/organizations/${encodeURIComponent(id)}`, {
    method: 'DELETE',
    accessToken,
  });
}

export function fetchOrganizationMembers(accessToken: string, id: string): Promise<AdminUser[]> {
  return apiFetch<AdminUser[]>(`/api/admin/organizations/${encodeURIComponent(id)}/members`, { accessToken });
}

/** Exact username/email lookup only, never a browse/search - backs the "add member" flow. */
export function findUserByIdentifier(accessToken: string, identifier: string): Promise<AdminUser> {
  return apiFetch<AdminUser>(`/api/admin/organizations/user-lookup?identifier=${encodeURIComponent(identifier)}`, { accessToken });
}

export function addOrganizationMember(accessToken: string, organizationId: string, userId: string): Promise<void> {
  return apiFetch<void>(`/api/admin/organizations/${encodeURIComponent(organizationId)}/members/${encodeURIComponent(userId)}`, {
    method: 'PUT',
    accessToken,
  });
}

export function removeOrganizationMember(accessToken: string, organizationId: string, userId: string): Promise<void> {
  return apiFetch<void>(`/api/admin/organizations/${encodeURIComponent(organizationId)}/members/${encodeURIComponent(userId)}`, {
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

export function fetchAdminUserAuditEvents(accessToken: string, userId: string): Promise<AdminUserAuditEvent[]> {
  return apiFetch<AdminUserAuditEvent[]>(`/api/admin/users/${userId}/audit`, { accessToken });
}

export function fetchRecentActivity(accessToken: string, limit = 20): Promise<AdminUserAuditEvent[]> {
  return apiFetch<AdminUserAuditEvent[]>(`/api/admin/activity?limit=${limit}`, { accessToken });
}

export function fetchRegistrationStats(accessToken: string, range: StatsRange): Promise<RegistrationStatsPoint[]> {
  return apiFetch<RegistrationStatsPoint[]>(`/api/admin/stats/registrations?range=${range}`, { accessToken });
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
