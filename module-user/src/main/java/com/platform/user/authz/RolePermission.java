package com.platform.user.authz;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import org.hibernate.envers.Audited;
import org.hibernate.envers.RelationTargetAuditMode;

/** The actual "yetki matrisi" - who can add/remove one of these is itself a permission (authz:manage). */
@Entity
@Table(name = "role_permission")
@Audited
public class RolePermission {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "role_id", nullable = false)
    @Audited(targetAuditMode = RelationTargetAuditMode.NOT_AUDITED)
    private Role role;

    @ManyToOne
    @JoinColumn(name = "permission_id", nullable = false)
    @Audited(targetAuditMode = RelationTargetAuditMode.NOT_AUDITED)
    private Permission permission;

    /**
     * GRANTED is the only value MatrixPermissionEvaluator's real authorization check ever sees
     * (RolePermissionLookupService.resolvePermissions filters to it). VISIBLE_DENIED exists purely
     * for UiPermissionsController: it renders the control as DISABLED for a role that is NOT
     * actually authorized, instead of the permission's default hidden/disabled fallback - e.g.
     * MANAGER sees "Raporu Onayla" greyed out rather than hidden, without being able to call it.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "access_level", nullable = false)
    private AccessLevel accessLevel = AccessLevel.GRANTED;

    public enum AccessLevel {
        GRANTED,
        VISIBLE_DENIED
    }

    public Long getId() {
        return id;
    }

    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role = role;
    }

    public Permission getPermission() {
        return permission;
    }

    public void setPermission(Permission permission) {
        this.permission = permission;
    }

    public AccessLevel getAccessLevel() {
        return accessLevel;
    }

    public void setAccessLevel(AccessLevel accessLevel) {
        this.accessLevel = accessLevel;
    }
}
