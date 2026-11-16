package com.lifeos.goal.api;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/** Minimal, ownership-scoped Goal snapshot exposed to AI. */
public interface GoalAiQueryApi {
    record Snapshot(UUID id, String title, String description, LocalDate targetOn, String status,
            String progress, long version) { }

    List<Snapshot> aiGoals(UUID userId, int limit);
}
