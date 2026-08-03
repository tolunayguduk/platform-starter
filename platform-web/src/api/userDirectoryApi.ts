import { apiFetch } from './client';
import type { UserProfileSummary } from '../types/user';

/** Public user browsing - any authenticated user, not just admin-panel-eligible ones. */
export function fetchUserProfile(accessToken: string, id: string): Promise<UserProfileSummary> {
  return apiFetch<UserProfileSummary>(`/api/users/${encodeURIComponent(id)}`, { accessToken });
}
