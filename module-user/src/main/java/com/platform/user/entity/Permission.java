package com.platform.user.entity;

import com.platform.user.constant.UiPolicy;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Fine-grained permission key, e.g. "report:list", "payment:approve".
 * uiPolicy drives how a denied action should render on the frontend (see UiPermissionsController) -
 * this is a UX hint only, never the actual security boundary (that's @PreAuthorize + MatrixPermissionEvaluator).
 */
@Entity
@Table(name = "permission")
public class Permission {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** "resource:action" format, e.g. "report:list". Column name backtick-quoted because "key" is
     * a reserved word in MySQL - without this, Hibernate's generated UPDATE/INSERT SQL breaks
     * (the raw JDBC reads in AdminTableServiceImpl already quote it for the same reason). */
    @Column(name = "`key`", nullable = false, unique = true)
    private String key;

    @Enumerated(EnumType.STRING)
    @Column(name = "ui_policy", nullable = false)
    private UiPolicy uiPolicy = UiPolicy.HIDE_IF_DENIED;

    /** Human-readable explanation of what this function does - purely descriptive, shown in the
     * admin panel's function catalog, never read by any authorization or UI-policy logic. */
    @Column
    private String description;

    /** When false, this function can't be granted to a role and every existing grant of it becomes
     * inert - see RolePermissionRepository, whose granted/visibleDenied/hidden queries all exclude
     * disabled permissions so this is enforced in the real authorization path, not just the UI. */
    @Column(nullable = false)
    private boolean enabled = true;

    public Long getId() {
        return id;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public UiPolicy getUiPolicy() {
        return uiPolicy;
    }

    public void setUiPolicy(UiPolicy uiPolicy) {
        this.uiPolicy = uiPolicy;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
}
