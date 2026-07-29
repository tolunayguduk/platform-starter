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

export function updateUserRoles(accessToken: string, userId: string, roles: string[]): Promise<void> {
  return apiFetch<void>(`/api/admin/users/${userId}/roles`, {
    method: 'PUT',
    body: { roles },
    accessToken,
  });
}

export type StatsRange = 'DAY' | 'WEEK' | 'MONTH' | 'YEAR';

export interface RegistrationStatsPoint {
  bucket: string;
  count: number;
}

export function fetchRegistrationStats(accessToken: string, range: StatsRange): Promise<RegistrationStatsPoint[]> {
  return apiFetch<RegistrationStatsPoint[]>(`/api/admin/stats/registrations?range=${range}`, { accessToken });
}