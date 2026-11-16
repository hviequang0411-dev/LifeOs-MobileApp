package com.lifeos.notification.dto;

/** Trạng thái reminder cho snapshot sync (kể cả tombstone). */
public record ReminderSyncState(ReminderResponse reminder, boolean deleted) {
}
