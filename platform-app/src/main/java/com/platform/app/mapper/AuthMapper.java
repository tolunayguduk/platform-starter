package com.platform.app.mapper;

import com.platform.app.controller.model.TokenResponseDto;
import com.platform.security.integration.keycloak.model.TokenResponse;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

@Mapper(componentModel = "spring")
public interface AuthMapper {

    AuthMapper INSTANCE = Mappers.getMapper(AuthMapper.class);

    public TokenResponseDto toDto(TokenResponse response);
}
