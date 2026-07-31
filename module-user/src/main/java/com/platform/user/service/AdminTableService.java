package com.platform.user.service;

import com.platform.user.constant.AdminTableKey;
import com.platform.user.service.model.AdminAuditRowsResult;
import com.platform.user.service.model.AdminTableRowsResult;
import com.platform.user.service.model.AdminTableSummaryResult;

import java.util.List;

/**
 * Backs the admin panel's raw database table browser: the MySQL "main tables" (GDPR categories +
 * permission matrix), plus each row's Envers audit history. Read-only, admin-only inspection -
 * never a substitute for the typed repositories the rest of the app uses.
 */
public interface AdminTableService {

    List<AdminTableSummaryResult> listTables();

    AdminTableRowsResult getRows(AdminTableKey key);

    AdminAuditRowsResult getAuditRows(AdminTableKey key, String primaryKeyValue);
}
