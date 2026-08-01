package com.platform.user.controller.model;

import java.util.List;

public record AdminTableDto(String key, boolean hasAudit, List<String> editableColumns) {
}
