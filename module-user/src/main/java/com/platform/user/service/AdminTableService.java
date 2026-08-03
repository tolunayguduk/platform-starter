package com.platform.user.service;

import com.platform.user.constant.AdminTableKey;
import com.platform.user.service.model.AdminAuditRowsResult;
import com.platform.user.service.model.AdminTableRowsResult;
import com.platform.user.service.model.AdminTableSummaryResult;

import java.util.List;
import java.util.Map;

/**
 * Backs the admin panel's raw database table browser: the MySQL "main tables" (GDPR categories +
 * permission matrix), plus each row's Envers audit history and admin-only edits. Reads go through
 * JdbcTemplate against a whitelisted registry; writes always go through the same typed JPA
 * repositories the rest of the app uses (never raw SQL), so Hibernate/Envers keeps auditing them.
 */
public interface AdminTableService {

    List<AdminTableSummaryResult> listTables();

    /** PERMISSION/ROLE_PERMISSION/USER_CONSENT rows are never shown to an organization-scope
     * caller at all (PLATFORM-scope only); USER_PROFILE/USER_CONTACT rows are shown but filtered
     * down to the caller's own organization's members - see AdminAccessScopeService.resolveVisibleUserIds. */
    AdminTableRowsResult getRows(AdminTableKey key, String callerKeycloakUserId);

    /** PLATFORM-scope only - a manager manages their organization's users/membership, but never
     * gets to see the change history behind it (audit trails on GDPR/role-grant tables alike). */
    AdminAuditRowsResult getAuditRows(AdminTableKey key, String primaryKeyValue, String callerKeycloakUserId);

    /** Applies only the fields present in {@code changes} (partial update) - every other column
     * is left untouched. Rejects any column not in that table's editable set. Writing to
     * PERMISSION or ROLE_PERMISSION (the role/function definitions) is PLATFORM-scope only - see
     * AdminAccessScopeService; every other table is unrestricted here. */
    Map<String, Object> updateRow(AdminTableKey key, String primaryKeyValue, Map<String, Object> changes, String callerKeycloakUserId);

    /** Only ROLE_PERMISSION supports this today (granting a function to a role) - every other
     * table throws error.admin.create_not_supported. PLATFORM-scope only. */
    Map<String, Object> createRow(AdminTableKey key, Map<String, Object> values, String callerKeycloakUserId);

    /** Only ROLE_PERMISSION supports this today (revoking a function from a role) - every other
     * table throws error.admin.delete_not_supported. PLATFORM-scope only. */
    void deleteRow(AdminTableKey key, String primaryKeyValue, String callerKeycloakUserId);
}
