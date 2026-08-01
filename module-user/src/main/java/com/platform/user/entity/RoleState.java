package com.platform.user.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Whether this role is currently enabled for authorization purposes - purely local policy, not
 * Keycloak-owned data. Keycloak realm roles have no native enabled/disabled concept (only users
 * do); role_name is just a join key into Keycloak's role list, same as in role_permission. Absence
 * of a row means enabled - every role starts enabled until an admin explicitly disables it.
 */
@Entity
@Table(name = "role_state")
public class RoleState {

    @Id
    @Column(name = "role_name")
    private String roleName;

    @Column(nullable = false)
    private boolean enabled = true;

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
}
