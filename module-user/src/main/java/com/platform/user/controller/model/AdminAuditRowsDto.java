package com.platform.user.controller.model;

import java.util.List;
import java.util.Map;

public record AdminAuditRowsDto(List<String> columns, List<Map<String, Object>> rows) {
}
