package com.platform.user.service;

import com.platform.user.entity.UserProfile;
import com.platform.user.repository.UserProfileRepository;
import com.platform.user.service.model.CurrentUserResult;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CurrentUserServiceImpl implements CurrentUserService {

    private final UserProfileRepository userProfileRepository;

    public CurrentUserServiceImpl(UserProfileRepository userProfileRepository) {
        this.userProfileRepository = userProfileRepository;
    }

    @Override
    public CurrentUserResult getCurrentUser(String keycloakUserId, String username, String email, List<String> roles) {
        String fullName = userProfileRepository.findById(keycloakUserId)
                .map(UserProfile::getFullName)
                .orElse(null);
        return new CurrentUserResult(username, email, fullName, roles);
    }
}
