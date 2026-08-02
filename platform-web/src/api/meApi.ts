import { apiFetch } from './client';
import type { CurrentUser } from '../types/auth';
import type { AdminAccessScope } from '../types/admin';

export function fetchMe(accessToken: string): Promise<CurrentUser> {
  return apiFetch<CurrentUser>('/api/me', { accessToken });
}

export function fetchAdminAccessScope(accessToken: string): Promise<AdminAccessScope> {
  return apiFetch<AdminAccessScope>('/api/me/admin-scope', { accessToken });
}
