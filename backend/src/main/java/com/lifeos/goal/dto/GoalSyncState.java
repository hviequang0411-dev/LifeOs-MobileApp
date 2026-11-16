package com.lifeos.goal.dto;

/** Trạng thái một bản ghi goal/milestone cho snapshot sync, kể cả tombstone. */
public record GoalSyncState(java.util.UUID id, long version, boolean deleted, Object data) {
}
