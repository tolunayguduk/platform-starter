package com.platform.user.repository;

import com.platform.user.entity.RolePermission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Collection;
import java.util.List;

public interface RolePermissionRepository extends JpaRepository<RolePermission, Long> {

    @Query("select rp from RolePermission rp " +
           "join fetch rp.permission " +
           "where rp.roleName in :roleNames and rp.accessLevel = com.platform.user.constant.AccessLevel.GRANTED")
    List<RolePermission> findGrantedByRoleNames(Collection<String> roleNames);

    @Query("select rp from RolePermission rp " +
           "join fetch rp.permission " +
           "where rp.roleName in :roleNames and rp.accessLevel = com.platform.user.constant.AccessLevel.VISIBLE_DENIED")
    List<RolePermission> findVisibleDeniedByRoleNames(Collection<String> roleNames);
}
