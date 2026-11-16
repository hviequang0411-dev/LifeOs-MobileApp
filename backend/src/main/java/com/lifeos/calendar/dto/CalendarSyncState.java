package com.lifeos.calendar.dto;

import java.util.UUID;

/** Trạng thái entity calendar cho snapshot sync (kể cả tombstone). */
public record CalendarSyncState(Object response, UUID id, long version, boolean deleted) {
}
