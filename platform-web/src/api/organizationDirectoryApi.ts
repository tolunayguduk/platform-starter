import { apiFetch } from './client';
import type { OrganizationMemberSummary, OrganizationProfile, OrganizationSearchResult } from '../types/organization';

/** Public organization browsing - any authenticated user, not just admin-panel-eligible ones. */
export function searchOrganizations(accessToken: string, query: string): Promise<OrganizationSearchResult[]> {
  return apiFetch<OrganizationSearchResult[]>(`/api/organizations/search?query=${encodeURIComponent(query)}`, { accessToken });
}

export function fetchOrganizationProfile(accessToken: string, id: string): Promise<OrganizationProfile> {
  return apiFetch<OrganizationProfile>(`/api/organizations/${encodeURIComponent(id)}`, { accessToken });
}

/** Backs the landing page's member-list popup. */
export function fetchOrganizationMembers(accessToken: string, id: string): Promise<OrganizationMemberSummary[]> {
  return apiFetch<OrganizationMemberSummary[]>(`/api/organizations/${encodeURIComponent(id)}/members`, { accessToken });
}
