package com.platform.user.entity;

import com.platform.user.constant.RoleScope;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Local-only policy for a role: whether it's currently enabled for authorization purposes, and
 * how far its authority reaches (scope). Neither is Keycloak-owned data - Keycloak realm roles
 * have no native enabled/disabled or scope concept (only users do); role_name is just a join key
 * into Keycloak's role list, same as in role_permission. Absence of a row means enabled=true and
 * scope=NONE (not an admin-panel role at all) - every role starts enabled and admin-panel-blind
 * until an admin explicitly opts it into ORGANIZATION or PLATFORM scope.
 */
@Entity
@Table(name = "role_state")
public class RoleState {

    @Id
    @Column(name = "role_name")
    private String roleName;

    @Column(nullable = false)
    private boolean enabled = true;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RoleScope scope = RoleScope.NONE;

    public String getRoleName() {
        return roleName;
    }

    public void setRoleName(String roleName) {
        this.roleName = roleName;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public RoleScope getScope() {
        return scope;
    }

    public void setScope(RoleScope scope) {
        this.scope = scope;
    }
}
