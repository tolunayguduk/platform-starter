package com.platform.app.controller;

import com.platform.user.controller.model.CurrentUserDto;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;

public interface MeController {

    @GetMapping("/api/me")
    CurrentUserDto me(@AuthenticationPrincipal Jwt jwt);
}
