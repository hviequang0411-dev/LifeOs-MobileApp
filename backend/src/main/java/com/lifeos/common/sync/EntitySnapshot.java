package com.lifeos.common.sync;

import java.util.Map;
import java.util.UUID;

/** Trạng thái hiện tại của một entity (kể cả tombstone) để phục vụ pull theo state (D18). */
public record EntitySnapshot(String entityType, UUID entityId, long version, boolean deleted,
        Map<String, Object> data) {
}
