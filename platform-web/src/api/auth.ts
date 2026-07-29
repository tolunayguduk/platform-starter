import { apiFetch } from './client';

/**
 * Wire shape of KeycloakTokenClient.TokenResponse (platform-security-starter) - its
 * @JsonProperty annotations (kept for deserializing Keycloak's own token responses) apply to
 * serialization too, so /api/auth/login|refresh answer with snake_case keys.
 */
export interface TokenResponse {
  access_token: string;
  id_token: string;
  refresh_token: string;
  expires_in: number;
}

export function login(username: string, password: string): Promise<TokenResponse> {
  return apiFetch<TokenResponse>('/api/auth/login', {
    method: 'POST',
    body: { username, password },
  });
}

export function refresh(refreshToken: string): Promise<TokenResponse> {
  return apiFetch<TokenResponse>('/api/auth/refresh', {
    method: 'POST',
    body: { refreshToken },
  });
}

export function logout(refreshToken: string): Promise<void> {
  return apiFetch<void>('/api/auth/logout', {
    method: 'POST',
    body: { refreshToken },
  });
}

export interface RegisterFields {
  username: string;
  email: string;
  password: string;
  confirmPassword: string;
  firstName: string;
  lastName: string;
  termsAccepted: boolean;
}

export function register(fields: RegisterFields): Promise<void> {
  return apiFetch<void>('/api/auth/register', {
    method: 'POST',
    body: fields,
  });
}

export interface CurrentUser {
  username: string;
  email: string;
  fullName: string | null;
  roles: string[];
}

export function fetchMe(accessToken: string): Promise<CurrentUser> {
  return apiFetch<CurrentUser>('/api/me', { accessToken });
}
