package com.platform.user.controller;

import com.platform.user.controller.model.ChangePasswordRequestDto;
import com.platform.user.controller.model.MyProfileDto;
import com.platform.user.controller.model.UpdateMyProfileRequestDto;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Lets the current user view/manage their own data: identity fields mirrored from Keycloak
 * (username/email/firstName/lastName) and the MySQL-only profile/contact categories. Always
 * scoped to the caller's own JWT subject - never accepts a userId parameter.
 */
public interface ProfileController {

    @GetMapping("/api/me/profile")
    MyProfileDto getProfile(@AuthenticationPrincipal Jwt jwt);

    @PutMapping("/api/me/profile")
    MyProfileDto updateProfile(@AuthenticationPrincipal Jwt jwt, @RequestBody UpdateMyProfileRequestDto request);

    @PostMapping("/api/me/password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void changePassword(@AuthenticationPrincipal Jwt jwt, @RequestBody ChangePasswordRequestDto request);
}
