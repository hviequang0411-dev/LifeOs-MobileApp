package com.lifeos.calendar.api;

import java.util.UUID;

/** Sự kiện miền: Event vừa xóa mềm + flush, trước change_logs của Event (D86); notification xóa reminder của nó. */
public record EventDeletionStarted(UUID userId, UUID eventId) {
}
