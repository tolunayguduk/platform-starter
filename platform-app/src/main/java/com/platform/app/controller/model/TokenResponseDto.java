package com.platform.app.controller.model;

import com.fasterxml.jackson.annotation.JsonProperty;

/** Wire shape is snake_case (platform-web's api/auth.ts TokenResponse) - matches the OAuth2 token
 * response convention Keycloak itself uses, which this DTO's field names alone would not produce. */
public record TokenResponseDto(
        @JsonProperty("access_token") String accessToken,
        @JsonProperty("id_token") String idToken,
        @JsonProperty("refresh_token") String refreshToken,
        @JsonProperty("expires_in") long expiresIn) {
}
