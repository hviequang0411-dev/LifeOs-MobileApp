package com.lifeos.habit.dto;

/** Trạng thái check-in cho snapshot sync (kể cả tombstone). */
public record CheckinSyncState(CheckinResponse checkin, boolean deleted) {
}
