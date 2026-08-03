import type { OrganizationSearchResult } from './organization';

/** A user's public profile page - see GET /api/users/{id}. Much lighter than MyProfile (no
 * email, phone, address, birth date, or consents) - visible to any authenticated user. */
export interface UserProfileSummary {
  keycloakUserId: string;
  username: string;
  fullName: string | null;
  avatarUrl: string | null;
  organizations: OrganizationSearchResult[];
}
