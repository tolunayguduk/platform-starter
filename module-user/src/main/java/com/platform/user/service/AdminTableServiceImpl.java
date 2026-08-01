package com.platform.user.service;

import com.platform.error.BusinessException;
import com.platform.user.constant.AccessLevel;
import com.platform.user.constant.AdminTableKey;
import com.platform.user.constant.UiPolicy;
import com.platform.user.entity.Permission;
import com.platform.user.entity.RolePermission;
import com.platform.user.entity.UserConsent;
import com.platform.user.entity.UserContact;
import com.platform.user.entity.UserProfile;
import com.platform.user.repository.PermissionRepository;
import com.platform.user.repository.RolePermissionRepository;
import com.platform.user.repository.RoleStateRepository;
import com.platform.user.repository.UserConsentRepository;
import com.platform.user.repository.UserContactRepository;
import com.platform.user.repository.UserProfileRepository;
import com.platform.user.service.model.AdminAuditRowsResult;
import com.platform.user.service.model.AdminTableRowsResult;
import com.platform.user.service.model.AdminTableSummaryResult;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Table/column identifiers here are NEVER client-supplied - the only thing the client picks is an
 * {@link AdminTableKey} enum value (Spring rejects anything that isn't a valid enum name before
 * this class ever runs), which indexes into the hardcoded {@link #REGISTRY} below. Reads are built
 * from those whitelisted, backtick-quoted identifiers only, with every value (primary key lookups)
 * always passed as a bind parameter, never concatenated.
 *
 * <p>Writes never use raw SQL at all - user_profile/user_contact/user_consent/role_permission are
 * all {@code @Audited}, and Envers only records a change when it goes through Hibernate's own
 * dirty-checking (a JPA repository {@code save()}). A hand-rolled {@code UPDATE ... SET} would
 * silently skip the audit trail, so {@link #updateRow} always loads the real entity via its
 * repository, applies only the whitelisted editable fields, and saves it - exactly like every
 * other write in this app.
 */
@Service
public class AdminTableServiceImpl implements AdminTableService {

    private static final int MAIN_TABLE_ROW_LIMIT = 500;
    private static final int AUDIT_ROW_LIMIT = 200;

    private record TableMeta(String tableName, String auditTableName, String primaryKeyColumn,
                              List<String> columns, List<String> editableColumns) {
    }

    private static final Map<AdminTableKey, TableMeta> REGISTRY = new EnumMap<>(AdminTableKey.class);

    static {
        REGISTRY.put(AdminTableKey.USER_PROFILE, new TableMeta(
                "user_profile", "user_profile_aud", "keycloak_user_id",
                List.of("keycloak_user_id", "full_name", "birth_date", "avatar_url", "locale", "deleted_at"),
                List.of("full_name", "birth_date", "avatar_url", "locale")));
        REGISTRY.put(AdminTableKey.USER_CONTACT, new TableMeta(
                "user_contact", "user_contact_aud", "keycloak_user_id",
                List.of("keycloak_user_id", "phone_number", "alternate_email", "address_line", "city", "country"),
                List.of("phone_number", "alternate_email", "address_line", "city", "country")));
        REGISTRY.put(AdminTableKey.USER_CONSENT, new TableMeta(
                "user_consent", "user_consent_aud", "id",
                List.of("id", "keycloak_user_id", "consent_type", "legal_basis", "purpose", "granted_at", "revoked_at", "ip_address"),
                List.of("consent_type", "legal_basis", "purpose")));
        REGISTRY.put(AdminTableKey.PERMISSION, new TableMeta(
                "permission", null, "id",
                List.of("id", "key", "ui_policy", "description", "enabled"),
                List.of("key", "ui_policy", "description", "enabled")));
        REGISTRY.put(AdminTableKey.ROLE_PERMISSION, new TableMeta(
                "role_permission", "role_permission_aud", "id",
                List.of("id", "role_name", "permission_id", "access_level"),
                List.of("role_name", "permission_id", "access_level")));
    }

    private final JdbcTemplate jdbcTemplate;
    private final UserProfileRepository userProfileRepository;
    private final UserContactRepository userContactRepository;
    private final UserConsentRepository userConsentRepository;
    private final PermissionRepository permissionRepository;
    private final RolePermissionRepository rolePermissionRepository;
    private final RoleStateRepository roleStateRepository;
    private final ApplicationEventPublisher eventPublisher;

    public AdminTableServiceImpl(JdbcTemplate jdbcTemplate,
                                  UserProfileRepository userProfileRepository,
                                  UserContactRepository userContactRepository,
                                  UserConsentRepository userConsentRepository,
                                  PermissionRepository permissionRepository,
                                  RolePermissionRepository rolePermissionRepository,
                                  RoleStateRepository roleStateRepository,
                                  ApplicationEventPublisher eventPublisher) {
        this.jdbcTemplate = jdbcTemplate;
        this.userProfileRepository = userProfileRepository;
        this.userContactRepository = userContactRepository;
        this.userConsentRepository = userConsentRepository;
        this.permissionRepository = permissionRepository;
        this.rolePermissionRepository = rolePermissionRepository;
        this.roleStateRepository = roleStateRepository;
        this.eventPublisher = eventPublisher;
    }

    @Override
    public List<AdminTableSummaryResult> listTables() {
        return REGISTRY.entrySet().stream()
                .map(e -> new AdminTableSummaryResult(e.getKey(), e.getValue().auditTableName() != null, e.getValue().editableColumns()))
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

    @Override
    @Transactional
    public Map<String, Object> updateRow(AdminTableKey key, String primaryKeyValue, Map<String, Object> changes) {
        TableMeta meta = REGISTRY.get(key);
        for (String field : changes.keySet()) {
            if (!meta.editableColumns().contains(field)) {
                throw new BusinessException("ADMIN-4002", "error.admin.readonly_field", "Field not editable: " + field);
            }
        }

        try {
            switch (key) {
                case USER_PROFILE -> updateUserProfile(primaryKeyValue, changes);
                case USER_CONTACT -> updateUserContact(primaryKeyValue, changes);
                case USER_CONSENT -> updateUserConsent(primaryKeyValue, changes);
                case PERMISSION -> updatePermission(primaryKeyValue, changes);
                case ROLE_PERMISSION -> updateRolePermission(primaryKeyValue, changes);
            }
        } catch (DataIntegrityViolationException e) {
            throw new BusinessException("ADMIN-4004", "error.admin.constraint_violation",
                    "Update violates a database constraint: " + e.getMostSpecificCause().getMessage());
        } catch (ClassCastException | IllegalArgumentException | java.time.format.DateTimeParseException e) {
            throw new BusinessException("ADMIN-4003", "error.admin.invalid_value", "Invalid value: " + e.getMessage());
        }

        return readRow(meta, primaryKeyValue);
    }

    @Override
    @Transactional
    public Map<String, Object> createRow(AdminTableKey key, Map<String, Object> values) {
        TableMeta meta = REGISTRY.get(key);

        String newId;
        try {
            newId = switch (key) {
                case PERMISSION -> createPermission(values);
                case ROLE_PERMISSION -> createRolePermission(values);
                default -> throw new BusinessException("ADMIN-4005", "error.admin.create_not_supported",
                        "Table " + key + " does not support creating rows");
            };
        } catch (DataIntegrityViolationException e) {
            throw new BusinessException("ADMIN-4004", "error.admin.constraint_violation",
                    "Create violates a database constraint: " + e.getMostSpecificCause().getMessage());
        } catch (ClassCastException | IllegalArgumentException e) {
            throw new BusinessException("ADMIN-4003", "error.admin.invalid_value", "Invalid value: " + e.getMessage());
        }

        return readRow(meta, newId);
    }

    @Override
    @Transactional
    public void deleteRow(AdminTableKey key, String primaryKeyValue) {
        switch (key) {
            case PERMISSION -> deletePermission(primaryKeyValue);
            case ROLE_PERMISSION -> deleteRolePermission(primaryKeyValue);
            default -> throw new BusinessException("ADMIN-4005", "error.admin.delete_not_supported",
                    "Table " + key + " does not support deleting rows");
        }
    }

    // Read the saved row back through the raw-JDBC path (same shape as getRows) rather than
    // converting the entity by hand - every write flushes its repository first, since this query
    // runs on the same connection/transaction and would otherwise see Hibernate's unflushed write.
    private Map<String, Object> readRow(TableMeta meta, String primaryKeyValue) {
        String sql = "SELECT " + quotedColumns(meta.columns()) + " FROM " + quote(meta.tableName())
                + " WHERE " + quote(meta.primaryKeyColumn()) + " = ?";
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql, primaryKeyValue);
        if (rows.isEmpty()) {
            throw new BusinessException("ADMIN-4040", "error.admin.row_not_found", "No such row: " + primaryKeyValue);
        }
        return rows.get(0);
    }

    private void updateUserProfile(String pk, Map<String, Object> changes) {
        UserProfile entity = userProfileRepository.findById(pk)
                .orElseThrow(() -> new BusinessException("ADMIN-4040", "error.admin.row_not_found", "No such row: " + pk));
        if (changes.containsKey("full_name")) {
            entity.setFullName((String) changes.get("full_name"));
        }
        if (changes.containsKey("birth_date")) {
            entity.setBirthDate(parseDate(changes.get("birth_date")));
        }
        if (changes.containsKey("avatar_url")) {
            entity.setAvatarUrl((String) changes.get("avatar_url"));
        }
        if (changes.containsKey("locale")) {
            entity.setLocale((String) changes.get("locale"));
        }
        userProfileRepository.save(entity);
        userProfileRepository.flush();
    }

    private void updateUserContact(String pk, Map<String, Object> changes) {
        UserContact entity = userContactRepository.findById(pk)
                .orElseThrow(() -> new BusinessException("ADMIN-4040", "error.admin.row_not_found", "No such row: " + pk));
        if (changes.containsKey("phone_number")) {
            entity.setPhoneNumber((String) changes.get("phone_number"));
        }
        if (changes.containsKey("alternate_email")) {
            entity.setAlternateEmail((String) changes.get("alternate_email"));
        }
        if (changes.containsKey("address_line")) {
            entity.setAddressLine((String) changes.get("address_line"));
        }
        if (changes.containsKey("city")) {
            entity.setCity((String) changes.get("city"));
        }
        if (changes.containsKey("country")) {
            entity.setCountry((String) changes.get("country"));
        }
        userContactRepository.save(entity);
        userContactRepository.flush();
    }

    private void updateUserConsent(String pk, Map<String, Object> changes) {
        UserConsent entity = userConsentRepository.findById(pk)
                .orElseThrow(() -> new BusinessException("ADMIN-4040", "error.admin.row_not_found", "No such row: " + pk));
        if (changes.containsKey("consent_type")) {
            entity.setConsentType((String) changes.get("consent_type"));
        }
        if (changes.containsKey("legal_basis")) {
            entity.setLegalBasis((String) changes.get("legal_basis"));
        }
        if (changes.containsKey("purpose")) {
            entity.setPurpose((String) changes.get("purpose"));
        }
        userConsentRepository.save(entity);
        userConsentRepository.flush();
    }

    private String createPermission(Map<String, Object> values) {
        if (values.get("key") == null || values.get("ui_policy") == null) {
            throw new BusinessException("ADMIN-4003", "error.admin.invalid_value",
                    "key and ui_policy are both required");
        }
        Permission entity = new Permission();
        entity.setKey((String) values.get("key"));
        entity.setUiPolicy(UiPolicy.valueOf((String) values.get("ui_policy")));
        if (values.get("description") != null) {
            entity.setDescription((String) values.get("description"));
        }
        permissionRepository.save(entity);
        permissionRepository.flush();
        return String.valueOf(entity.getId());
    }

    private void updatePermission(String pk, Map<String, Object> changes) {
        Permission entity = permissionRepository.findById(Long.valueOf(pk))
                .orElseThrow(() -> new BusinessException("ADMIN-4040", "error.admin.row_not_found", "No such row: " + pk));
        if (changes.containsKey("key")) {
            entity.setKey((String) changes.get("key"));
        }
        if (changes.containsKey("ui_policy")) {
            entity.setUiPolicy(UiPolicy.valueOf((String) changes.get("ui_policy")));
        }
        if (changes.containsKey("description")) {
            entity.setDescription((String) changes.get("description"));
        }
        if (changes.containsKey("enabled")) {
            entity.setEnabled((Boolean) changes.get("enabled"));
        }
        permissionRepository.save(entity);
        permissionRepository.flush();
        if (changes.containsKey("enabled")) {
            // Every role that had this function granted needs its cache re-evaluated - disabling
            // it makes those grants inert, enabling it makes them live again.
            for (String roleName : rolePermissionRepository.findByPermission_Id(entity.getId()).stream()
                    .map(RolePermission::getRoleName).distinct().toList()) {
                eventPublisher.publishEvent(new RolePermissionsChangedEvent(roleName));
            }
        }
    }

    private void updateRolePermission(String pk, Map<String, Object> changes) {
        RolePermission entity = rolePermissionRepository.findById(Long.valueOf(pk))
                .orElseThrow(() -> new BusinessException("ADMIN-4040", "error.admin.row_not_found", "No such row: " + pk));
        if (changes.containsKey("role_name")) {
            String roleName = (String) changes.get("role_name");
            assertRoleEnabled(roleName);
            entity.setRoleName(roleName);
        }
        if (changes.containsKey("permission_id")) {
            Long permissionId = ((Number) changes.get("permission_id")).longValue();
            Permission permission = permissionRepository.findById(permissionId)
                    .orElseThrow(() -> new BusinessException("ADMIN-4003", "error.admin.invalid_value",
                            "No such permission id: " + permissionId));
            assertPermissionEnabled(permission);
            entity.setPermission(permission);
        }
        if (changes.containsKey("access_level")) {
            entity.setAccessLevel(AccessLevel.valueOf((String) changes.get("access_level")));
        }
        rolePermissionRepository.save(entity);
        rolePermissionRepository.flush();
        eventPublisher.publishEvent(new RolePermissionsChangedEvent(entity.getRoleName()));
    }

    private String createRolePermission(Map<String, Object> values) {
        if (values.get("role_name") == null || values.get("permission_id") == null || values.get("access_level") == null) {
            throw new BusinessException("ADMIN-4003", "error.admin.invalid_value",
                    "role_name, permission_id and access_level are all required");
        }
        String roleName = (String) values.get("role_name");
        assertRoleEnabled(roleName);
        Long permissionId = ((Number) values.get("permission_id")).longValue();
        Permission permission = permissionRepository.findById(permissionId)
                .orElseThrow(() -> new BusinessException("ADMIN-4003", "error.admin.invalid_value",
                        "No such permission id: " + permissionId));
        assertPermissionEnabled(permission);

        RolePermission entity = new RolePermission();
        entity.setRoleName(roleName);
        entity.setPermission(permission);
        entity.setAccessLevel(AccessLevel.valueOf((String) values.get("access_level")));
        rolePermissionRepository.save(entity);
        rolePermissionRepository.flush();
        eventPublisher.publishEvent(new RolePermissionsChangedEvent(entity.getRoleName()));
        return String.valueOf(entity.getId());
    }

    // Disabled roles/functions can't receive a new (or retargeted) grant - existing grants of them
    // are left in place but inert, per RolePermissionRepository's query-level enabled filtering.
    private void assertRoleEnabled(String roleName) {
        boolean disabled = roleStateRepository.findById(roleName).map(rs -> !rs.isEnabled()).orElse(false);
        if (disabled) {
            throw new BusinessException("ADMIN-4006", "error.admin.role_disabled", "Role is disabled: " + roleName);
        }
    }

    private void assertPermissionEnabled(Permission permission) {
        if (!permission.isEnabled()) {
            throw new BusinessException("ADMIN-4007", "error.admin.function_disabled",
                    "Function is disabled: " + permission.getKey());
        }
    }

    private void deleteRolePermission(String pk) {
        RolePermission entity = rolePermissionRepository.findById(Long.valueOf(pk))
                .orElseThrow(() -> new BusinessException("ADMIN-4040", "error.admin.row_not_found", "No such row: " + pk));
        String roleName = entity.getRoleName();
        rolePermissionRepository.delete(entity);
        rolePermissionRepository.flush();
        eventPublisher.publishEvent(new RolePermissionsChangedEvent(roleName));
    }

    private void deletePermission(String pk) {
        Long permissionId = Long.valueOf(pk);
        Permission entity = permissionRepository.findById(permissionId)
                .orElseThrow(() -> new BusinessException("ADMIN-4040", "error.admin.row_not_found", "No such row: " + pk));
        // Every role's grant of this function becomes meaningless once the function itself is
        // gone - clean those up too rather than leave orphaned rows pointing at a dead permission_id.
        List<RolePermission> grants = rolePermissionRepository.findByPermission_Id(permissionId);
        rolePermissionRepository.deleteAll(grants);
        permissionRepository.delete(entity);
        permissionRepository.flush();
        for (String roleName : grants.stream().map(RolePermission::getRoleName).distinct().toList()) {
            eventPublisher.publishEvent(new RolePermissionsChangedEvent(roleName));
        }
    }

    private LocalDate parseDate(Object value) {
        return value == null ? null : LocalDate.parse((String) value);
    }

    private List<String> concatColumns(List<String> columns, String... extra) {
        List<String> result = new ArrayList<>(columns);
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
