package com.lifeos.routine.dto;

/** Trạng thái một bản ghi routine (bất kỳ loại nào) cho snapshot sync, kể cả tombstone. */
public record RoutineSyncState(java.util.UUID id, long version, boolean deleted, Object data) {
}
