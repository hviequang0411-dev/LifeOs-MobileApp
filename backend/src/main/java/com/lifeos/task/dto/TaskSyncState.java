package com.lifeos.task.dto;

/** Trạng thái một Task cho snapshot sync: dữ liệu như REST và cờ tombstone (SYNC_PROTOCOL §6, §6.1). */
public record TaskSyncState(TaskResponse task, boolean deleted) {
}
