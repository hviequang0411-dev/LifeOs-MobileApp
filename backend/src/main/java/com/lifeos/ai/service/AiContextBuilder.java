package com.lifeos.ai.service;

import com.lifeos.ai.dto.AiContextDomain;
import com.lifeos.ai.dto.AiContextEntity;
import com.lifeos.ai.dto.AiContextResponse;
import com.lifeos.ai.dto.AiPreferencesResponse;
import com.lifeos.calendar.api.CalendarAiQueryApi;
import com.lifeos.finance.api.FinanceAiQueryApi;
import com.lifeos.goal.api.GoalAiQueryApi;
import com.lifeos.habit.api.HabitAiQueryApi;
import com.lifeos.note.api.NoteAiQueryApi;
import com.lifeos.task.api.TaskAiQueryApi;
import com.lifeos.common.exception.ErrorCode;
import com.lifeos.common.exception.NotFoundException;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class AiContextBuilder {
    private static final int MAX_ENTITIES = 200;
    private final AiPreferenceService preferences;
    private final TaskAiQueryApi tasks;
    private final CalendarAiQueryApi calendar;
    private final HabitAiQueryApi habits;
    private final GoalAiQueryApi goals;
    private final NoteAiQueryApi notes;
    private final FinanceAiQueryApi finance;
    private final Clock clock;

    public AiContextBuilder(AiPreferenceService preferences, TaskAiQueryApi tasks, CalendarAiQueryApi calendar,
            HabitAiQueryApi habits, GoalAiQueryApi goals, NoteAiQueryApi notes, FinanceAiQueryApi finance,
            Clock clock) {
        this.preferences = preferences;
        this.tasks = tasks;
        this.calendar = calendar;
        this.habits = habits;
        this.goals = goals;
        this.notes = notes;
        this.finance = finance;
        this.clock = clock;
    }

    public Snapshot build(UUID userId, Set<AiContextDomain> requested, LocalDate from, LocalDate to,
            UUID selectedNoteId, String currency) {
        AiPreferencesResponse consent = preferences.get(userId);
        LinkedHashSet<AiContextDomain> domains = new LinkedHashSet<>(requested);
        domains.forEach(domain -> require(consent, domain));
        LocalDate start = from == null ? LocalDate.now(clock.withZone(ZoneOffset.UTC)).minusDays(7) : from;
        LocalDate end = to == null ? start.plusDays(14) : to;
        List<AiContextEntity> entities = new ArrayList<>();
        if (domains.contains(AiContextDomain.TASK_CALENDAR)) {
            tasks.aiWindow(userId, start, end, 50).forEach(t -> add(entities, entity(AiContextDomain.TASK_CALENDAR,
                    "TASK", t.id(), t.version(), map("title", t.title(), "description", trim(t.description(), 1000),
                            "status", t.status(), "priority", t.priority(), "dueDate", t.dueDate(),
                            "dueTime", t.dueTime()))));
            calendar.aiWindow(userId, start, end, 50).forEach(c -> add(entities,
                    entity(AiContextDomain.TASK_CALENDAR, c.source(), c.id(), null,
                            map("title", c.title(), "allDay", c.allDay(), "startAt", c.startAt(),
                                    "endAt", c.endAt()))));
        }
        if (domains.contains(AiContextDomain.HABIT)) {
            habits.aiHabits(userId, 40).forEach(h -> add(entities, entity(AiContextDomain.HABIT, "HABIT", h.id(),
                    h.version(), map("name", h.name(), "scheduleType", h.scheduleType(), "startOn", h.startOn(),
                            "endOn", h.endOn(), "currentStreak", h.currentStreak(),
                            "longestStreak", h.longestStreak()))));
        }
        if (domains.contains(AiContextDomain.GOAL)) {
            goals.aiGoals(userId, 40).forEach(g -> add(entities, entity(AiContextDomain.GOAL, "GOAL", g.id(),
                    g.version(), map("title", g.title(), "description", trim(g.description(), 1000),
                            "targetOn", g.targetOn(), "status", g.status(), "progress", g.progress()))));
        }
        if (domains.contains(AiContextDomain.NOTES) && selectedNoteId != null) {
            notes.aiNote(userId, selectedNoteId).ifPresent(n -> add(entities, entity(AiContextDomain.NOTES, "NOTE",
                    n.id(), n.version(), map("title", trim(n.title(), 200), "content", trim(n.content(), 8000)))));
        }
        if (domains.contains(AiContextDomain.FINANCE)) {
            finance.aiTransactions(userId, start, end, currency, 50).forEach(f -> add(entities,
                    entity(AiContextDomain.FINANCE, "FINANCE_TRANSACTION", f.id(), f.version(),
                            map("kind", f.kind(), "amount", f.amount(), "currency", f.currency(),
                                    "occurredOn", f.occurredOn()))));
        }
        boolean truncated = entities.size() >= MAX_ENTITIES;
        Instant capturedAt = clock.instant();
        AiContextResponse response = new AiContextResponse(null, capturedAt, domains, List.copyOf(entities), truncated);
        return new Snapshot(response, providerMap(response));
    }

    public Snapshot buildForTask(UUID userId, Set<AiContextDomain> requested, UUID taskId) {
        LinkedHashSet<AiContextDomain> domains = new LinkedHashSet<>(requested);
        domains.add(AiContextDomain.TASK_CALENDAR);
        Snapshot base = build(userId, domains, null, null, null, null);
        if (base.disclosure().entities().stream().anyMatch(e -> taskId.equals(e.entityId()))) return base;
        TaskAiQueryApi.Snapshot task = tasks.aiTask(userId, taskId)
                .orElseThrow(() -> new NotFoundException(ErrorCode.TASK_NOT_FOUND));
        List<AiContextEntity> entities = new ArrayList<>(base.disclosure().entities());
        add(entities, entity(AiContextDomain.TASK_CALENDAR, "TASK", task.id(), task.version(),
                map("title", task.title(), "description", trim(task.description(), 1000), "status", task.status(),
                        "priority", task.priority(), "dueDate", task.dueDate(), "dueTime", task.dueTime())));
        AiContextResponse disclosure = new AiContextResponse(null, base.disclosure().capturedAt(), domains,
                List.copyOf(entities), base.disclosure().truncated());
        return new Snapshot(disclosure, providerMap(disclosure));
    }

    public Snapshot buildForNote(UUID userId, Set<AiContextDomain> requested, UUID noteId) {
        LinkedHashSet<AiContextDomain> domains = new LinkedHashSet<>(requested);
        domains.add(AiContextDomain.NOTES);
        Snapshot snapshot = build(userId, domains, null, null, noteId, null);
        if (snapshot.disclosure().entities().stream().noneMatch(e -> noteId.equals(e.entityId()))) {
            throw new NotFoundException(ErrorCode.NOTE_NOT_FOUND);
        }
        return snapshot;
    }

    private static void require(AiPreferencesResponse p, AiContextDomain domain) {
        boolean allowed = switch (domain) {
            case TASK_CALENDAR -> p.taskCalendar(); case HABIT -> p.habit(); case GOAL -> p.goal();
            case NOTES -> p.notes(); case FINANCE -> p.finance();
        };
        if (!allowed) {
            throw AiErrors.error("AI_CONSENT_REQUIRED", org.springframework.http.HttpStatus.FORBIDDEN,
                    "Chưa có consent cho miền dữ liệu.").with("domain", domain.name());
        }
    }

    private static void add(List<AiContextEntity> target, AiContextEntity entity) {
        if (target.size() < MAX_ENTITIES) target.add(entity);
    }

    private static AiContextEntity entity(AiContextDomain d, String type, UUID id, Long version,
            Map<String, Object> fields) { return new AiContextEntity(d, type, id, version, fields); }

    private static Map<String, Object> providerMap(AiContextResponse value) {
        return map("capturedAt", value.capturedAt(), "domains", value.domains().stream().map(Enum::name).toList(),
                "entities", value.entities().stream().map(e -> map("domain", e.domain().name(),
                        "entityType", e.entityType(), "entityId", e.entityId(), "entityVersion", e.entityVersion(),
                        "fields", e.fields())).toList(), "truncated", value.truncated());
    }

    private static Map<String, Object> map(Object... entries) {
        Map<String, Object> out = new LinkedHashMap<>();
        for (int i = 0; i < entries.length; i += 2) if (entries[i + 1] != null) out.put((String) entries[i], entries[i + 1]);
        return out;
    }

    private static String trim(String value, int max) {
        return value == null || value.length() <= max ? value : value.substring(0, max);
    }

    public record Snapshot(AiContextResponse disclosure, Map<String, Object> providerPayload) { }
}
