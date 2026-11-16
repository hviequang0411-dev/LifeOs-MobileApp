package com.lifeos.calendar.service;

import com.lifeos.common.util.Patch;
import java.util.Map;
import java.util.UUID;

/** Lệnh của use case calendar (REST và sync dùng chung). toString không in dữ liệu người dùng. */
public final class CalendarCommands {

    private CalendarCommands() {
    }

    public record EventFields(Patch<String> title, Patch<String> description, Patch<String> location,
            Patch<Boolean> allDay, Patch<String> timezone, Patch<String> startLocal, Patch<String> endLocal,
            Patch<String> startDate, Patch<String> endDate, Patch<Map<String, Object>> recurrence) {
        @Override
        public String toString() {
            return "EventFields[]";
        }
    }

    public record CreateEvent(UUID id, EventFields fields) {
    }

    public record UpdateEvent(long expectedVersion, EventFields fields) {
    }

    public record ExceptionFields(Patch<UUID> eventId, Patch<String> originalStartLocal, Patch<String> kind,
            Patch<String> title, Patch<String> description, Patch<String> location, Patch<String> startLocal,
            Patch<String> endLocal) {
        @Override
        public String toString() {
            return "ExceptionFields[]";
        }
    }

    public record CreateException(UUID id, ExceptionFields fields) {
    }

    public record UpdateException(long expectedVersion, ExceptionFields fields) {
    }

    public record CreateTimeBlock(UUID id, Patch<UUID> taskId, Patch<String> startAt, Patch<String> endAt,
            Patch<String> note) {
        @Override
        public String toString() {
            return "CreateTimeBlock[id=" + id + "]";
        }
    }

    public record UpdateTimeBlock(long expectedVersion, Patch<UUID> taskId, Patch<String> startAt,
            Patch<String> endAt, Patch<String> note) {
        @Override
        public String toString() {
            return "UpdateTimeBlock[expectedVersion=" + expectedVersion + "]";
        }
    }
}
