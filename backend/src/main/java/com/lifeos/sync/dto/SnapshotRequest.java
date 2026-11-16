package com.lifeos.sync.dto;

/** POST /sync/snapshot: trang đầu gửi {} (hoặc chỉ limit); trang sau gửi {snapshotToken, next}. */
public record SnapshotRequest(String snapshotToken, String next, Integer limit) {

    @Override
    public String toString() {
        return "SnapshotRequest[next=" + (next != null) + "]";
    }
}
