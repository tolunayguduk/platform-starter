package com.platform.user.mapper;

import com.platform.user.controller.model.UserProfileSummaryDto;
import com.platform.user.service.model.UserProfileSummaryResult;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface UserDirectoryMapper {

    UserProfileSummaryDto toDto(UserProfileSummaryResult result);
}
