package com.platform.user.controller;

import com.platform.user.controller.model.ChangePasswordRequestDto;
import com.platform.user.controller.model.MyProfileDto;
import com.platform.user.controller.model.UpdateMyProfileRequestDto;
import com.platform.user.mapper.ProfileMapper;
import com.platform.user.service.MyProfileService;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ProfileControllerImpl implements ProfileController {

    private final MyProfileService myProfileService;
    private final ProfileMapper profileMapper;

    public ProfileControllerImpl(MyProfileService myProfileService, ProfileMapper profileMapper) {
        this.myProfileService = myProfileService;
        this.profileMapper = profileMapper;
    }

    @Override
    public MyProfileDto getProfile(Jwt jwt) {
        return profileMapper.toDto(myProfileService.getProfile(jwt.getSubject()));
    }

    @Override
    public MyProfileDto updateProfile(Jwt jwt, UpdateMyProfileRequestDto request) {
        return profileMapper.toDto(myProfileService.updateProfile(jwt.getSubject(), profileMapper.toCommand(request)));
    }

    @Override
    public void changePassword(Jwt jwt, ChangePasswordRequestDto request) {
        myProfileService.changePassword(jwt.getSubject(), jwt.getClaimAsString("preferred_username"), profileMapper.toCommand(request));
    }
}
