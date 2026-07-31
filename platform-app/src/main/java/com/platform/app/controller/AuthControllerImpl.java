package com.platform.app.controller;

import com.platform.app.controller.model.LoginRequestDto;
import com.platform.app.controller.model.RefreshRequestDto;
import com.platform.app.controller.model.RegisterRequestDto;
import com.platform.app.controller.model.TokenResponseDto;
import com.platform.app.mapper.AuthMapper;
import com.platform.app.mapper.RegistrationMapper;
import com.platform.app.service.AuthService;
import com.platform.user.service.RegistrationService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AuthControllerImpl implements AuthController {

    private final AuthService authService;
    private final RegistrationService registrationService;
    private final AuthMapper authMapper;
    private final RegistrationMapper registrationMapper;

    public AuthControllerImpl(AuthService authService, RegistrationService registrationService,
                               AuthMapper authMapper, RegistrationMapper registrationMapper) {
        this.authService = authService;
        this.registrationService = registrationService;
        this.authMapper = authMapper;
        this.registrationMapper = registrationMapper;
    }

    @Override
    public TokenResponseDto login(LoginRequestDto request) {
        return authMapper.toDto(authService.login(request.username(), request.password()));
    }

    @Override
    public TokenResponseDto refresh(RefreshRequestDto request) {
        return authMapper.toDto(authService.refresh(request.refreshToken()));
    }

    @Override
    public void logout(RefreshRequestDto request) {
        authService.logout(request.refreshToken());
    }

    @Override
    public void register(RegisterRequestDto request, HttpServletRequest servletRequest) {
        registrationService.register(registrationMapper.toCommand(request, servletRequest.getRemoteAddr()));
    }
}
