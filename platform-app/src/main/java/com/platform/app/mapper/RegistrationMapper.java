package com.platform.app.mapper;

import com.platform.app.controller.model.RegisterRequestDto;
import com.platform.user.service.model.RegisterUserCommand;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

@Mapper(componentModel = "spring")
public interface RegistrationMapper {

    RegistrationMapper INSTANCE = Mappers.getMapper(RegistrationMapper.class);

    public RegisterUserCommand toCommand(RegisterRequestDto dto, String clientIp);
}
