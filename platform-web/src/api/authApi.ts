import { apiFetch } from './client';
import type { RegisterFields, TokenResponse } from '../types/auth';

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

export function register(fields: RegisterFields): Promise<void> {
  return apiFetch<void>('/api/auth/register', {
    method: 'POST',
    body: fields,
  });
}
