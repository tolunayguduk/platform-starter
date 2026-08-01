import { apiFetch } from './client';
import type { CurrentUser } from '../types/auth';

export function fetchMe(accessToken: string): Promise<CurrentUser> {
  return apiFetch<CurrentUser>('/api/me', { accessToken });
}
