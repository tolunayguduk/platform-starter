package com.platform.user.controller;

import com.platform.user.controller.model.UserProfileSummaryDto;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * Public user browsing - same tier as OrganizationDirectoryController (no AdminAccessGuard, any
 * authenticated user). Viewing someone's basic profile page has nothing to do with admin-panel
 * access.
 */
@RequestMapping("/api/users")
public interface UserDirectoryController {

    @GetMapping("/{id}")
    UserProfileSummaryDto getProfile(@PathVariable String id);
}
