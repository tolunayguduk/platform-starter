package com.platform.user.web;

import com.platform.error.BusinessException;
import com.platform.user.profile.ChangePasswordRequest;
import com.platform.user.profile.MyProfileService;
import com.platform.user.profile.MyProfileView;
import com.platform.user.profile.UpdateMyProfileRequest;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Lets the current user view/manage their own data: identity fields mirrored from Keycloak
 * (username/email/firstName/lastName) and the MySQL-only profile/contact categories. Always
 * scoped to the caller's own JWT subject - never accepts a userId parameter.
 */
@RestController
public class ProfileController {

    private final MyProfileService profileService;

    public ProfileController(MyProfileService profileService) {
        this.profileService = profileService;
    }

    @GetMapping("/api/me/profile")
    public MyProfileView getProfile(@AuthenticationPrincipal Jwt jwt) {
        return profileService.getProfile(jwt.getSubject());
    }

    @PutMapping("/api/me/profile")
    public MyProfileView updateProfile(@AuthenticationPrincipal Jwt jwt, @RequestBody UpdateMyProfileRequest request) {
        validate(request);
        return profileService.updateProfile(jwt.getSubject(), request);
    }

    @PostMapping("/api/me/password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void changePassword(@AuthenticationPrincipal Jwt jwt, @RequestBody ChangePasswordRequest request) {
        profileService.changePassword(jwt.getSubject(), jwt.getClaimAsString("preferred_username"), request);
    }

    private void validate(UpdateMyProfileRequest request) {
        if (isBlank(request.firstName()) || isBlank(request.lastName()) || isBlank(request.email())) {
            throw new BusinessException("COMMON-4001", "error.profile.missing_fields", "Required field missing");
        }
        if (!request.email().contains("@")) {
            throw new BusinessException("COMMON-4002", "error.profile.invalid_email", "Invalid email: " + request.email());
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}