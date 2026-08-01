import { apiFetch } from './client';
import type { ChangePasswordFields, MyProfile, UpdateProfileFields } from '../types/profile';

export function fetchMyProfile(accessToken: string): Promise<MyProfile> {
  return apiFetch<MyProfile>('/api/me/profile', { accessToken });
}

export function updateMyProfile(accessToken: string, fields: UpdateProfileFields): Promise<MyProfile> {
  return apiFetch<MyProfile>('/api/me/profile', { method: 'PUT', body: fields, accessToken });
}

export function changePassword(accessToken: string, fields: ChangePasswordFields): Promise<void> {
  return apiFetch<void>('/api/me/password', { method: 'POST', body: fields, accessToken });
}
