package com.platform.user.service;

import com.platform.error.BusinessException;
import com.platform.user.constant.AdminTableKey;
import com.platform.user.service.model.AdminAuditRowsResult;
import com.platform.user.service.model.AdminTableRowsResult;
import com.platform.user.service.model.AdminTableSummaryResult;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Table/column identifiers here are NEVER client-supplied - the only thing the client picks is an
 * {@link AdminTableKey} enum value (Spring rejects anything that isn't a valid enum name before
 * this class ever runs), which indexes into the hardcoded {@link #REGISTRY} below. SQL is built
 * from those whitelisted, backtick-quoted identifiers only; every value (primary key lookups) is
 * always passed as a bind parameter, never concatenated.
 */
@Service
public class AdminTableServiceImpl implements AdminTableService {

    private static final int MAIN_TABLE_ROW_LIMIT = 500;
    private static final int AUDIT_ROW_LIMIT = 200;

    private record TableMeta(String tableName, String auditTableName, String primaryKeyColumn, List<String> columns) {
    }

    private static final Map<AdminTableKey, TableMeta> REGISTRY = new EnumMap<>(AdminTableKey.class);

    static {
        REGISTRY.put(AdminTableKey.USER_PROFILE, new TableMeta(
                "user_profile", "user_profile_aud", "keycloak_user_id",
                List.of("keycloak_user_id", "full_name", "birth_date", "avatar_url", "locale", "deleted_at")));
        REGISTRY.put(AdminTableKey.USER_CONTACT, new TableMeta(
                "user_contact", "user_contact_aud", "keycloak_user_id",
                List.of("keycloak_user_id", "phone_number", "alternate_email", "address_line", "city", "country")));
        REGISTRY.put(AdminTableKey.USER_CONSENT, new TableMeta(
                "user_consent", "user_consent_aud", "id",
                List.of("id", "keycloak_user_id", "consent_type", "legal_basis", "purpose", "granted_at", "revoked_at", "ip_address")));
        REGISTRY.put(AdminTableKey.PERMISSION, new TableMeta(
                "permission", null, "id",
                List.of("id", "key", "ui_policy")));
        REGISTRY.put(AdminTableKey.ROLE_PERMISSION, new TableMeta(
                "role_permission", "role_permission_aud", "id",
                List.of("id", "role_name", "permission_id", "access_level")));
    }

    private final JdbcTemplate jdbcTemplate;

    public AdminTableServiceImpl(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public List<AdminTableSummaryResult> listTables() {
        return REGISTRY.entrySet().stream()
                .map(e -> new AdminTableSummaryResult(e.getKey(), e.getValue().auditTableName() != null))
                .toList();
    }

    @Override
    public AdminTableRowsResult getRows(AdminTableKey key) {
        TableMeta meta = REGISTRY.get(key);
        String sql = "SELECT " + quotedColumns(meta.columns()) + " FROM " + quote(meta.tableName())
                + " LIMIT " + MAIN_TABLE_ROW_LIMIT;
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql);
        return new AdminTableRowsResult(meta.columns(), meta.primaryKeyColumn(), rows);
    }

    @Override
    public AdminAuditRowsResult getAuditRows(AdminTableKey key, String primaryKeyValue) {
        TableMeta meta = REGISTRY.get(key);
        if (meta.auditTableName() == null) {
            throw new BusinessException("ADMIN-4001", "error.admin.no_audit_table",
                    "Table " + key + " has no audit history");
        }
        List<String> auditColumns = concatColumns(meta.columns(), "rev", "revtype");
        String sql = "SELECT " + quotedColumns(auditColumns) + " FROM " + quote(meta.auditTableName())
                + " WHERE " + quote(meta.primaryKeyColumn()) + " = ? ORDER BY rev DESC LIMIT " + AUDIT_ROW_LIMIT;
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql, primaryKeyValue);
        return new AdminAuditRowsResult(auditColumns, rows);
    }

    private List<String> concatColumns(List<String> columns, String... extra) {
        List<String> result = new java.util.ArrayList<>(columns);
        result.addAll(List.of(extra));
        return result;
    }

    private String quotedColumns(List<String> columns) {
        return columns.stream().map(this::quote).collect(Collectors.joining(", "));
    }

    private String quote(String identifier) {
        return "`" + identifier + "`";
    }
}
