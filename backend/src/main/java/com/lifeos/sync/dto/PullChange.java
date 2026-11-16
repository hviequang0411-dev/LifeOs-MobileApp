package com.lifeos.sync.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.Map;
import java.util.UUID;

/**
 * Một phần tử {@code changes[]} của GET /sync/pull (SYSTEM_ARCHITECTURE §4.2): UPSERT kèm data (§6.1) hoặc DELETE
 * (tombstone, không có data) kèm version.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record PullChange(String entityType, UUID entityId, String operation, long version,
        Map<String, Object> data) {

    public static final String UPSERT = "UPSERT";
    public static final String DELETE = "DELETE";

    @Override
    public String toString() {
        return "PullChange[entityType=" + entityType + ", entityId=" + entityId + ", operation=" + operation + "]";
    }
}
