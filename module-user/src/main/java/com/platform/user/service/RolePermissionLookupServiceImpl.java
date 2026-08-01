package com.platform.user.service;

import com.platform.user.entity.RoleState;
import com.platform.user.repository.RolePermissionRepository;
import com.platform.user.repository.RoleStateRepository;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

/**
 * The single implementation of RolePermissionLookupPort in the system. platform-security-starter
 * only knows that interface - swapping this for a call to a dedicated authorization microservice
 * later requires touching only this class, no @PreAuthorize annotation anywhere else changes.
 */
@Service
public class RolePermissionLookupServiceImpl implements RolePermissionLookupService {

    private final RolePermissionRepository repository;
    private final RoleStateRepository roleStateRepository;

    public RolePermissionLookupServiceImpl(RolePermissionRepository repository, RoleStateRepository roleStateRepository) {
        this.repository = repository;
        this.roleStateRepository = roleStateRepository;
    }

    @Override
    @Cacheable(value = "role-permissions", key = "#roleNames")
    public Set<String> resolvePermissions(Collection<String> roleNames) {
        Set<String> enabledRoleNames = excludeDisabledRoles(roleNames);
        return repository.findGrantedByRoleNames(enabledRoleNames).stream()
                .map(rp -> rp.getPermission().getKey())
                .collect(Collectors.toSet());
    }

    @Override
    @Cacheable(value = "role-permissions-visible-denied", key = "#roleNames")
    public Set<String> resolveVisibleDeniedPermissions(Collection<String> roleNames) {
        Set<String> enabledRoleNames = excludeDisabledRoles(roleNames);
        return repository.findVisibleDeniedByRoleNames(enabledRoleNames).stream()
                .map(rp -> rp.getPermission().getKey())
                .collect(Collectors.toSet());
    }

    @Override
    @Cacheable(value = "role-permissions-hidden", key = "#roleNames")
    public Set<String> resolveHiddenPermissions(Collection<String> roleNames) {
        Set<String> enabledRoleNames = excludeDisabledRoles(roleNames);
        return repository.findHiddenByRoleNames(enabledRoleNames).stream()
                .map(rp -> rp.getPermission().getKey())
                .collect(Collectors.toSet());
    }

    // A disabled role contributes nothing to any of the three resolved sets, for either the real
    // authorization check (resolvePermissions, the GRANTED set MatrixPermissionEvaluator reads) or
    // the UI-only ones - "temporarily disabled" means every function under it goes inert, exactly
    // as if the role had no grants at all, without touching a single role_permission row.
    private Set<String> excludeDisabledRoles(Collection<String> roleNames) {
        // roleNames sorted so the cache key is stable regardless of authority iteration order
        Set<String> sortedNames = new TreeSet<>(roleNames);
        Set<String> disabled = roleStateRepository.findByRoleNameIn(sortedNames).stream()
                .filter(rs -> !rs.isEnabled())
                .map(RoleState::getRoleName)
                .collect(Collectors.toSet());
        sortedNames.removeAll(disabled);
        return sortedNames;
    }

    /**
     * Matrix changed (admin panel edit) -> the cache is stale for every role-name-combination
     * that included this role. Simplest safe option: clear the whole cache region rather than
     * try to enumerate every affected key combination.
     */
    @EventListener
    @Caching(evict = {
            @CacheEvict(value = "role-permissions", allEntries = true),
            @CacheEvict(value = "role-permissions-visible-denied", allEntries = true),
            @CacheEvict(value = "role-permissions-hidden", allEntries = true)
    })
    public void onRolePermissionsChanged(RolePermissionsChangedEvent event) {
        // body intentionally empty - eviction is done by the annotation
    }
}
