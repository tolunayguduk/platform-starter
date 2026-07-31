package com.platform.user.service.model;

import java.util.List;
import java.util.Map;

public record AdminAuditRowsResult(List<String> columns, List<Map<String, Object>> rows) {
}
