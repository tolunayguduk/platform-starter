package com.platform.user.mapper;

import com.platform.user.controller.model.ChangePasswordRequestDto;
import com.platform.user.controller.model.MyProfileDto;
import com.platform.user.controller.model.UpdateMyProfileRequestDto;
import com.platform.user.service.model.ChangePasswordCommand;
import com.platform.user.service.model.ProfileResult;
import com.platform.user.service.model.UpdateProfileCommand;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

@Mapper(componentModel = "spring")
public interface ProfileMapper {

    ProfileMapper INSTANCE = Mappers.getMapper(ProfileMapper.class);

    public MyProfileDto toDto(ProfileResult result);

    public UpdateProfileCommand toCommand(UpdateMyProfileRequestDto dto);

    public ChangePasswordCommand toCommand(ChangePasswordRequestDto dto);
}
