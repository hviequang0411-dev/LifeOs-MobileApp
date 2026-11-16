package com.lifeos.project.dto;

/** Trạng thái một Project cho snapshot sync: dữ liệu như REST và cờ tombstone (SYNC_PROTOCOL §6, §6.1). */
public record ProjectSyncState(ProjectResponse project, boolean deleted) {
}
