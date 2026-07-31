package com.platform.user.service.model;

import java.util.List;
import java.util.Map;

public record AdminTableRowsResult(List<String> columns, String primaryKeyColumn, List<Map<String, Object>> rows) {
}
