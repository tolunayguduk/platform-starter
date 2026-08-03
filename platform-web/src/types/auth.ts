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

export interface RegisterFields {
  username: string;
  email: string;
  password: string;
  confirmPassword: string;
  firstName: string;
  lastName: string;
  termsAccepted: boolean;
  /** Omitted/blank -> registers as a plain user. Present -> creates a new organization and the
   * registrant becomes its admin (see RegistrationServiceImpl on the backend). Mutually
   * exclusive with joinOrganizationId. */
  organizationName?: string;
  /** Set when arriving via an organization's invite link (?joinOrganization=<id>) - registers as
   * a plain user and files a join request against that organization. */
  joinOrganizationId?: string;
}

export interface CurrentUser {
  username: string;
  email: string;
  fullName: string | null;
  roles: string[];
}
