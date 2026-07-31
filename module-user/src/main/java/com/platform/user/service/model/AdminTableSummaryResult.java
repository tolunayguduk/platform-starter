package com.platform.user.service.model;

import com.platform.user.constant.AdminTableKey;

public record AdminTableSummaryResult(AdminTableKey key, boolean hasAudit) {
}
