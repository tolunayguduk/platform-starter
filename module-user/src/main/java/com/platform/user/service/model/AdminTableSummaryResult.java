package com.platform.user.service.model;

import com.platform.user.constant.AdminTableKey;

import java.util.List;

public record AdminTableSummaryResult(AdminTableKey key, boolean hasAudit, List<String> editableColumns) {
}
