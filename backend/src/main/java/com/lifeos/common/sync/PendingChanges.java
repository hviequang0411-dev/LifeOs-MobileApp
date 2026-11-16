package com.lifeos.common.sync;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Bộ gom change_logs của cascade nhiều module (D86): listener của một sự kiện miền chỉ GHI dữ liệu nghiệp vụ (và flush)
 * rồi {@link #add} thay đổi; module phát sự kiện gọi {@link #recordAll} sau khi mọi listener xong, trước change_logs của
 * chính nó. Nhờ vậy không có ghi nghiệp vụ nào sau lần ChangeRecorder.record đầu tiên của transaction.
 * Thay đổi do cascade luôn có mutation_id NULL (SYNC_PROTOCOL §3.2).
 */
public final class PendingChanges {

    private record Change(String entityType, UUID entityId, ChangeOperation operation, long version) {
    }

    private final List<Change> changes = new ArrayList<>();

    public void add(String entityType, UUID entityId, ChangeOperation operation, long version) {
        changes.add(new Change(entityType, entityId, operation, version));
    }

    public void recordAll(ChangeRecorder recorder, UUID userId) {
        for (Change c : changes) {
            recorder.record(userId, c.entityType(), c.entityId(), c.operation(), c.version(), null);
        }
        changes.clear();
    }
}
