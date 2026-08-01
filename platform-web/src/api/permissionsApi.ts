import { apiFetch } from './client';
import type { UiPermissions } from '../types/permissions';

export function fetchUiPermissions(accessToken: string): Promise<UiPermissions> {
  return apiFetch<UiPermissions>('/api/me/ui-permissions', { accessToken });
}
