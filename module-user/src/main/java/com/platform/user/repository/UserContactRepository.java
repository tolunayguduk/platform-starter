package com.platform.user.repository;

import com.platform.user.entity.UserContact;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserContactRepository extends JpaRepository<UserContact, String> {
}
