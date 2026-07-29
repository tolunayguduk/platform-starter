package com.platform.app;

import com.platform.error.BusinessException;
import com.platform.security.KeycloakTokenClient;
import com.platform.user.registration.RegistrationRequest;
import com.platform.user.registration.RegistrationService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * The only endpoints reachable without a Bearer token (see ResourceServerConfig - permitAll on
 * /api/auth/**). Register creates the account (Keycloak + local mirror via RegistrationService);
 * login/refresh/logout are thin passthroughs to KeycloakTokenClient's ROPC grant - the React SPA
 * holds the resulting tokens itself and sends them back as Authorization: Bearer on every other call.
 */
@RestController
public class AuthController {

    private final KeycloakTokenClient tokenClient;
    private final RegistrationService registrationService;

    public AuthController(KeycloakTokenClient tokenClient, RegistrationService registrationService) {
        this.tokenClient = tokenClient;
        this.registrationService = registrationService;
    }

    @PostMapping("/api/auth/login")
    public KeycloakTokenClient.TokenResponse login(@RequestBody LoginRequest request) {
        return tokenClient.passwordGrant(request.username(), request.password());
    }

    @PostMapping("/api/auth/refresh")
    public KeycloakTokenClient.TokenResponse refresh(@RequestBody RefreshRequest request) {
        return tokenClient.refreshGrant(request.refreshToken());
    }

    @PostMapping("/api/auth/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logout(@RequestBody RefreshRequest request) {
        tokenClient.endSession(request.refreshToken());
    }

    @PostMapping("/api/auth/register")
    @ResponseStatus(HttpStatus.CREATED)
    public void register(@RequestBody RegisterRequest request, HttpServletRequest servletRequest) {
        validate(request);
        try {
            registrationService.register(
                    new RegistrationRequest(request.username(), request.email(), request.password(),
                            request.firstName(), request.lastName()),
                    servletRequest.getRemoteAddr());
        } catch (BusinessException e) {
            throw new BusinessException("AUTHZ-4090", "error.register.already_exists",
                    "Username or email already registered: " + e.getMessage());
        }
    }

    private void validate(RegisterRequest request) {
        if (isBlank(request.username()) || isBlank(request.email()) || isBlank(request.password())
                || isBlank(request.firstName()) || isBlank(request.lastName())) {
            throw new BusinessException("COMMON-4001", "error.register.missing_fields", "Required field missing");
        }
        if (!request.email().contains("@")) {
            throw new BusinessException("COMMON-4002", "error.register.invalid_email", "Invalid email: " + request.email());
        }
        if (request.password().length() < 8) {
            throw new BusinessException("COMMON-4003", "error.register.password_too_short", "Password shorter than 8 characters");
        }
        if (!request.password().equals(request.confirmPassword())) {
            throw new BusinessException("COMMON-4004", "error.register.password_mismatch", "Password/confirmation mismatch");
        }
        if (request.termsAccepted() == null || !request.termsAccepted()) {
            throw new BusinessException("COMMON-4005", "error.register.terms_not_accepted", "Terms of service not accepted");
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    public record LoginRequest(String username, String password) {
    }

    public record RefreshRequest(String refreshToken) {
    }

    public record RegisterRequest(String username, String email, String password, String confirmPassword,
                                   String firstName, String lastName, Boolean termsAccepted) {
    }
}
