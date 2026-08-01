package com.platform.user.service;

import com.platform.user.constant.UiPolicy;
import com.platform.user.entity.Permission;
import com.platform.user.repository.PermissionRepository;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

@Service
public class UiPermissionsServiceImpl implements UiPermissionsService {

    private final RolePermissionLookupService lookupService;
    private final PermissionRepository permissionRepository;

    public UiPermissionsServiceImpl(RolePermissionLookupService lookupService, PermissionRepository permissionRepository) {
        this.lookupService = lookupService;
        this.permissionRepository = permissionRepository;
    }

    @Override
    public Map<String, String> getUiPermissions(Set<String> roleNames) {
        Set<String> granted = lookupService.resolvePermissions(roleNames);
        Set<String> visibleDenied = lookupService.resolveVisibleDeniedPermissions(roleNames);
        Set<String> explicitlyHidden = lookupService.resolveHiddenPermissions(roleNames);

        Map<String, String> result = new HashMap<>();
        for (Permission permission : permissionRepository.findAll()) {
            if (granted.contains(permission.getKey())) {
                result.put(permission.getKey(), "ENABLED");
            } else if (visibleDenied.contains(permission.getKey())) {
                result.put(permission.getKey(), "DISABLED");
            } else if (explicitlyHidden.contains(permission.getKey())) {
                result.put(permission.getKey(), "HIDDEN");
            } else {
                // No row at all for these roles - fall back to the permission's own default.
                result.put(permission.getKey(),
                        permission.getUiPolicy() == UiPolicy.DISABLE_IF_DENIED ? "DISABLED" : "HIDDEN");
            }
        }
        return result;
    }
}
