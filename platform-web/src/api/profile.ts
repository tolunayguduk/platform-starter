import { apiFetch } from './client';

export interface Consent {
  consentType: string;
  legalBasis: string;
  purpose: string | null;
  grantedAt: string | null;
  revokedAt: string | null;
}

/** Wire shape of MyProfileView (module-user) - identity fields come live from Keycloak,
 * everything else from the MySQL-only UserProfile/UserContact/UserConsent categories. */
export interface MyProfile {
  username: string;
  email: string;
  firstName: string;
  lastName: string;
  birthDate: string | null;
  avatarUrl: string | null;
  locale: string | null;
  phoneNumber: string | null;
  alternateEmail: string | null;
  addressLine: string | null;
  city: string | null;
  country: string | null;
  consents: Consent[];
}

export type UpdateProfileFields = Omit<MyProfile, 'username' | 'consents'>;

export function fetchMyProfile(accessToken: string): Promise<MyProfile> {
  return apiFetch<MyProfile>('/api/me/profile', { accessToken });
}

export function updateMyProfile(accessToken: string, fields: UpdateProfileFields): Promise<MyProfile> {
  return apiFetch<MyProfile>('/api/me/profile', { method: 'PUT', body: fields, accessToken });
}

export interface ChangePasswordFields {
  currentPassword: string;
  newPassword: string;
  confirmNewPassword: string;
}

export function changePassword(accessToken: string, fields: ChangePasswordFields): Promise<void> {
  return apiFetch<void>('/api/me/password', { method: 'POST', body: fields, accessToken });
}
