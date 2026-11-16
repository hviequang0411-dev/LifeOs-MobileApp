package com.lifeos.analytics.dto;

/** Trạng thái review cho snapshot sync, kể cả tombstone. */
public record ReviewSyncState(java.util.UUID id, long version, boolean deleted, Object data) {
}
