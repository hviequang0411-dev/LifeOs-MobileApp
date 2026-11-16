package com.lifeos.sync.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.Map;
import java.util.UUID;

/**
 * {@code serverSnapshot} của kết quả CONFLICT (SYNC_PROTOCOL §2.2, §6.1): {entityType, entityId, version, deleted,
 * data}. Entity đã xóa: deleted = true, data vắng mặt.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ServerSnapshot(String entityType, UUID entityId, long version, boolean deleted,
        Map<String, Object> data) {

    @Override
    public String toString() {
        return "ServerSnapshot[entityType=" + entityType + ", entityId=" + entityId + ", version=" + version + "]";
    }
}
