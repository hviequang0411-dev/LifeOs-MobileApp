package com.lifeos.notification.dto;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;

/** Nhắc nhở (PHASE_4 §6, D129–D131). nextFireAt, lastFiredAt do server; status DONE do server. */
public record ReminderResponse(UUID id, String targetType, UUID targetId, String mode, Integer offsetMinutes,
        String localTime, LocalDate localDate, Map<String, Object> repeatRule, boolean urgent, String status,
        Instant nextFireAt, Instant lastFiredAt, long version, Instant createdAt, Instant updatedAt) {

    @Override
    public String toString() {
        return "ReminderResponse[id=" + id + ", version=" + version + "]";
    }
}
