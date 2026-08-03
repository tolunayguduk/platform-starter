package com.platform.user.controller;

import com.platform.user.controller.model.UserProfileSummaryDto;
import com.platform.user.mapper.UserDirectoryMapper;
import com.platform.user.service.UserDirectoryService;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class UserDirectoryControllerImpl implements UserDirectoryController {

    private final UserDirectoryService userDirectoryService;
    private final UserDirectoryMapper userDirectoryMapper;

    public UserDirectoryControllerImpl(UserDirectoryService userDirectoryService, UserDirectoryMapper userDirectoryMapper) {
        this.userDirectoryService = userDirectoryService;
        this.userDirectoryMapper = userDirectoryMapper;
    }

    @Override
    public UserProfileSummaryDto getProfile(String id) {
        return userDirectoryMapper.toDto(userDirectoryService.getProfile(id));
    }
}
