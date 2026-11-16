package com.lifeos.calendar.api;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/** Minimal, ownership-scoped Calendar snapshot exposed to AI. */
public interface CalendarAiQueryApi {
    record Snapshot(String source, UUID id, String title, boolean allDay, Instant startAt, Instant endAt) { }

    List<Snapshot> aiWindow(UUID userId, LocalDate from, LocalDate to, int limit);
}
