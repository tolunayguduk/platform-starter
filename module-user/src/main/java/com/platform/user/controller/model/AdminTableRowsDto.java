package com.platform.user.controller.model;

import java.util.List;
import java.util.Map;

public record AdminTableRowsDto(List<String> columns, String primaryKeyColumn, List<Map<String, Object>> rows) {
}
