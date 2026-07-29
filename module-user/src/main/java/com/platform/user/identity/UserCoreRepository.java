package com.platform.user.identity;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserCoreRepository extends JpaRepository<UserCore, String> {

    Optional<UserCore> findByKeycloakUserId(String keycloakUserId);
}
