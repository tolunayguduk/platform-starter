package com.platform.user.repository;

import com.platform.user.entity.RoleState;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface RoleStateRepository extends JpaRepository<RoleState, String> {

    List<RoleState> findByRoleNameIn(Collection<String> roleNames);
}
