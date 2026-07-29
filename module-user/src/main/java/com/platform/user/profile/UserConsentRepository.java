package com.platform.user.profile;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface UserConsentRepository extends JpaRepository<UserConsent, String> {

    List<UserConsent> findByUserIdOrderByGrantedAtDesc(String userId);
}
