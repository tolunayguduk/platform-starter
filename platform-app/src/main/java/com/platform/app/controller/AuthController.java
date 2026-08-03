package com.platform.app.controller;

import com.platform.app.controller.model.LoginRequestDto;
import com.platform.app.controller.model.OrganizationNameDto;
import com.platform.app.controller.model.RefreshRequestDto;
import com.platform.app.controller.model.RegisterRequestDto;
import com.platform.app.controller.model.TokenResponseDto;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * The only endpoints reachable without a Bearer token (see ResourceServerConfig - permitAll on
 * /api/auth/**). Register creates the account in Keycloak plus this app's own GDPR categories
 * (module-user's RegistrationService); login/refresh/logout are thin passthroughs to Keycloak's
 * token endpoint (via AuthService) - the React SPA holds the resulting tokens itself and sends
 * them back as Authorization: Bearer on every other call.
 */
public interface AuthController {

    @PostMapping("/api/auth/login")
    TokenResponseDto login(@RequestBody LoginRequestDto request);

    @PostMapping("/api/auth/refresh")
    TokenResponseDto refresh(@RequestBody RefreshRequestDto request);

    @PostMapping("/api/auth/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void logout(@RequestBody RefreshRequestDto request);

    @PostMapping("/api/auth/register")
    @ResponseStatus(HttpStatus.CREATED)
    void register(@RequestBody RegisterRequestDto request, HttpServletRequest servletRequest);

    /** Lets the register page show "You're joining: {name}" for an invite-link organization id,
     * without requiring a token (the visitor doesn't have one yet). */
    @GetMapping("/api/auth/organizations/{id}/name")
    OrganizationNameDto getOrganizationName(@PathVariable String id);
}
