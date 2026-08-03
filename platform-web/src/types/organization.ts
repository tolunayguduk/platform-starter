/** A navbar search hit - see OrganizationDirectoryController/GET /api/organizations/search. Also
 * backs the Settings page's "My Organizations" cards (GET /api/me/organizations), which is why
 * it carries coverImageUrl too even though the navbar dropdown doesn't use it. */
export interface OrganizationSearchResult {
  id: string;
  name: string;
  coverImageUrl: string | null;
  logoImageUrl: string | null;
  memberCount: number;
}

/** An organization's public landing page - see GET /api/organizations/{id}. Viewable by any
 * authenticated user, not just members or admin-panel-eligible callers. isMember/canEdit/
 * hasPendingJoinRequest are all resolved relative to the caller. */
export interface OrganizationProfile {
  id: string;
  name: string;
  description: string | null;
  coverImageUrl: string | null;
  logoImageUrl: string | null;
  memberCount: number;
  membershipRequiresApproval: boolean;
  isMember: boolean;
  canEdit: boolean;
  hasPendingJoinRequest: boolean;
}

/** A member row in the landing page's member-list popup - see GET /api/organizations/{id}/members. */
export interface OrganizationMemberSummary {
  keycloakUserId: string;
  username: string;
  fullName: string | null;
}
