package com.lifeos.task.api;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Minimal, ownership-scoped Task snapshot exposed to the AI context builder. */
public interface TaskAiQueryApi {
    record Snapshot(UUID id, String title, String description, String status, String priority, LocalDate dueDate,
            String dueTime, long version) { }

    List<Snapshot> aiWindow(UUID userId, LocalDate from, LocalDate to, int limit);

    Optional<Snapshot> aiTask(UUID userId, UUID taskId);
}
