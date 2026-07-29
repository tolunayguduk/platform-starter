package com.platform.user.identity;

import com.platform.user.profile.UserProfile;
import com.platform.user.profile.UserProfileRepository;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Resolves the logged-in user's info from our local mirror (UserCore/UserProfile) rather than
 * trusting OIDC claims alone - keeps GET /api/me consistent with what /api/auth/register actually wrote.
 */
@Service
public class CurrentUserService {

    private final UserCoreRepository userCoreRepository;
    private final UserProfileRepository userProfileRepository;

    public CurrentUserService(UserCoreRepository userCoreRepository, UserProfileRepository userProfileRepository) {
        this.userCoreRepository = userCoreRepository;
        this.userProfileRepository = userProfileRepository;
    }

    /**
     * Falls back to the OIDC claims if no local UserCore row exists yet (e.g. a user created
     * directly in Keycloak rather than through /register) instead of failing GET /api/me.
     */
    public CurrentUserView findByKeycloakUserId(String keycloakUserId, String fallbackUsername, String fallbackEmail,
                                                 List<String> roles) {
        return userCoreRepository.findByKeycloakUserId(keycloakUserId)
                .map(userCore -> new CurrentUserView(
                        userCore.getUsername(),
                        userCore.getEmail(),
                        userProfileRepository.findById(userCore.getId()).map(UserProfile::getFullName).orElse(null),
                        roles))
                .orElseGet(() -> new CurrentUserView(fallbackUsername, fallbackEmail, null, roles));
    }
}
