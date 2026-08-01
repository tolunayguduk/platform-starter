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
}

export interface CurrentUser {
  username: string;
  email: string;
  fullName: string | null;
  roles: string[];
}
