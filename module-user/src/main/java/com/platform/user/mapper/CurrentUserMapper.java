package com.platform.user.mapper;

import com.platform.user.controller.model.CurrentUserDto;
import com.platform.user.service.model.CurrentUserResult;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

@Mapper(componentModel = "spring")
public interface CurrentUserMapper {

    CurrentUserMapper INSTANCE = Mappers.getMapper(CurrentUserMapper.class);

    public CurrentUserDto toDto(CurrentUserResult result);
}
