package com.platform.user.service;

import com.platform.user.repository.RolePermissionRepository;
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

    public RolePermissionLookupServiceImpl(RolePermissionRepository repository) {
        this.repository = repository;
    }

    @Override
    @Cacheable(value = "role-permissions", key = "#roleNames")
    public Set<String> resolvePermissions(Collection<String> roleNames) {
        // roleNames sorted so the cache key is stable regardless of authority iteration order
        Set<String> sortedNames = new TreeSet<>(roleNames);
        return repository.findGrantedByRoleNames(sortedNames).stream()
                .map(rp -> rp.getPermission().getKey())
                .collect(Collectors.toSet());
    }

    @Override
    @Cacheable(value = "role-permissions-visible-denied", key = "#roleNames")
    public Set<String> resolveVisibleDeniedPermissions(Collection<String> roleNames) {
        Set<String> sortedNames = new TreeSet<>(roleNames);
        return repository.findVisibleDeniedByRoleNames(sortedNames).stream()
                .map(rp -> rp.getPermission().getKey())
                .collect(Collectors.toSet());
    }

    /**
     * Matrix changed (admin panel edit) -> the cache is stale for every role-name-combination
     * that included this role. Simplest safe option: clear the whole cache region rather than
     * try to enumerate every affected key combination.
     */
    @EventListener
    @Caching(evict = {
            @CacheEvict(value = "role-permissions", allEntries = true),
            @CacheEvict(value = "role-permissions-visible-denied", allEntries = true)
    })
    public void onRolePermissionsChanged(RolePermissionsChangedEvent event) {
        // body intentionally empty - eviction is done by the annotation
    }
}
