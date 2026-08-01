package com.platform.user.repository;

import com.platform.user.entity.RolePermission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Collection;
import java.util.List;

public interface RolePermissionRepository extends JpaRepository<RolePermission, Long> {

    // Every one of these three excludes disabled functions (permission.enabled = false) so a
    // disabled function is inert everywhere, including the real hasPermission() authorization
    // path (RolePermissionLookupServiceImpl.resolvePermissions) - not just hidden in the UI.
    // Disabled *roles* are filtered separately, before roleNames ever reaches these queries (see
    // RolePermissionLookupServiceImpl) - keeping that check there means it's shared by all three.

    @Query("select rp from RolePermission rp " +
           "join fetch rp.permission " +
           "where rp.roleName in :roleNames and rp.accessLevel = com.platform.user.constant.AccessLevel.GRANTED " +
           "and rp.permission.enabled = true")
    List<RolePermission> findGrantedByRoleNames(Collection<String> roleNames);

    @Query("select rp from RolePermission rp " +
           "join fetch rp.permission " +
           "where rp.roleName in :roleNames and rp.accessLevel = com.platform.user.constant.AccessLevel.VISIBLE_DENIED " +
           "and rp.permission.enabled = true")
    List<RolePermission> findVisibleDeniedByRoleNames(Collection<String> roleNames);

    @Query("select rp from RolePermission rp " +
           "join fetch rp.permission " +
           "where rp.roleName in :roleNames and rp.accessLevel = com.platform.user.constant.AccessLevel.HIDDEN " +
           "and rp.permission.enabled = true")
    List<RolePermission> findHiddenByRoleNames(Collection<String> roleNames);

    /** Used when a role is deleted entirely - every grant referencing it would otherwise be an
     * orphaned row nothing can ever read again (Keycloak has no matching role name anymore). */
    List<RolePermission> findByRoleName(String roleName);

    /** Used when a function (Permission) is deleted entirely - every role's grant of it would
     * otherwise be an orphaned row pointing at a permission_id that no longer exists. */
    List<RolePermission> findByPermission_Id(Long permissionId);
}
