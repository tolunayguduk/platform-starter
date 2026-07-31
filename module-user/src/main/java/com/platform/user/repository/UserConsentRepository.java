package com.platform.user.repository;

import com.platform.user.entity.UserConsent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface UserConsentRepository extends JpaRepository<UserConsent, String> {

    List<UserConsent> findByKeycloakUserIdOrderByGrantedAtDesc(String keycloakUserId);
}
