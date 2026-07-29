package com.platform.user.web;

import com.platform.user.authz.Permission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Component;

import java.util.List;

interface PermissionJpaRepository extends JpaRepository<Permission, Long> {
}

@Component
class AllPermissionsProvider {

    private final PermissionJpaRepository repository;

    AllPermissionsProvider(PermissionJpaRepository repository) {
        this.repository = repository;
    }

    List<Permission> findAll() {
        return repository.findAll();
    }
}
