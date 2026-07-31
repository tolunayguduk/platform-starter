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

    /** "resource:action" format, e.g. "report:list" */
    @Column(nullable = false, unique = true)
    private String key;

    @Enumerated(EnumType.STRING)
    @Column(name = "ui_policy", nullable = false)
    private UiPolicy uiPolicy = UiPolicy.HIDE_IF_DENIED;

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
}
