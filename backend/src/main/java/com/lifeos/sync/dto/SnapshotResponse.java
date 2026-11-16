package com.lifeos.sync.dto;

import java.util.List;

/**
 * POST /sync/snapshot (PHASE_5 D152): {@code snapshotToken} gửi lại ở các trang sau (hạn 30 phút), {@code snapshotSeq}
 * là cursor để pull tiếp sau khi nạp xong, {@code next} là vị trí trang kế tiếp (null = hết).
 */
public record SnapshotResponse(String snapshotToken, long snapshotSeq, List<EntityPage> entities, String next) {

    public record EntityPage(String type, List<ServerSnapshot> items) {
    }

    @Override
    public String toString() {
        return "SnapshotResponse[snapshotSeq=" + snapshotSeq + "]";
    }
}
