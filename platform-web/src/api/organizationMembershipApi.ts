import { apiFetch } from './client';
import type { OrganizationMembershipRequest } from '../types/admin';
import type { OrganizationSearchResult } from '../types/organization';

/** Self-service organization membership - always scoped to the caller's own JWT subject, same
 * tier as meApi.ts (no admin-panel access required). */
export function fetchMyOrganizationInvites(accessToken: string): Promise<OrganizationMembershipRequest[]> {
  return apiFetch<OrganizationMembershipRequest[]>('/api/me/organization-invites', { accessToken });
}

export function acceptOrganizationInvite(accessToken: string, requestId: number): Promise<void> {
  return apiFetch<void>(`/api/me/organization-invites/${requestId}/accept`, {
    method: 'POST',
    accessToken,
  });
}

export function declineOrganizationInvite(accessToken: string, requestId: number): Promise<void> {
  return apiFetch<void>(`/api/me/organization-invites/${requestId}/decline`, {
    method: 'POST',
    accessToken,
  });
}

/** Self-service join via an organization's permanent invite link/code - returns whether it was
 * granted immediately or is now pending the organization's approval. */
export function joinOrganization(accessToken: string, organizationId: string): Promise<{ approved: boolean }> {
  return apiFetch<{ approved: boolean }>('/api/me/organizations/join', {
    method: 'POST',
    body: { organizationId },
    accessToken,
  });
}

/** Leaves an organization the caller is currently a member of - the landing page's "Leave" button. */
export function leaveOrganization(accessToken: string, organizationId: string): Promise<void> {
  return apiFetch<void>(`/api/me/organizations/${encodeURIComponent(organizationId)}/membership`, {
    method: 'DELETE',
    accessToken,
  });
}

/** Every organization the caller currently belongs to - backs the Settings page's "My
 * Organizations" tab. */
export function fetchMyOrganizations(accessToken: string): Promise<OrganizationSearchResult[]> {
  return apiFetch<OrganizationSearchResult[]>('/api/me/organizations', { accessToken });
}
