package com.lifeos.ai.service;

import com.lifeos.ai.config.AiProperties;
import com.lifeos.ai.dto.*;
import com.lifeos.ai.provider.LLMProvider;
import com.lifeos.ai.repository.AiStore;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

/** Read-only structured generations for AIP4. No method in this service can mutate a core domain. */
@Service
public class AiGenerationService {
    private static final Set<String> PLAN_KEYS = Set.of("summary", "items");
    private static final Set<String> PLAN_ITEM_KEYS = Set.of("title", "reason", "priority", "suggestedStartAt",
            "suggestedEndAt", "sourceRefs");
    private static final Set<String> TASK_KEYS = Set.of("items");
    private static final Set<String> TASK_ITEM_KEYS = Set.of("title", "description", "priority", "suggestedDueDate",
            "reason");
    private static final Set<String> INSIGHT_KEYS = Set.of("summary", "observations");
    private static final Set<String> OBSERVATION_KEYS = Set.of("title", "detail", "trend", "sourceRefs");
    private static final Set<String> NOTE_KEYS = Set.of("summary", "keyPoints");
    private static final Set<String> REF_KEYS = Set.of("domain", "entityType", "entityId");

    private final AiQuotaGuard quota;
    private final AiContextBuilder contexts;
    private final AiConversationTxService tx;
    private final LLMProvider provider;
    private final AiConcurrencyGate concurrency;
    private final AiOutputSanitizer sanitizer;
    private final AiProperties properties;
    private final Clock clock;

    public AiGenerationService(AiQuotaGuard quota, AiContextBuilder contexts, AiConversationTxService tx,
            LLMProvider provider, AiConcurrencyGate concurrency, AiOutputSanitizer sanitizer,
            AiProperties properties, Clock clock) {
        this.quota = quota; this.contexts = contexts; this.tx = tx; this.provider = provider;
        this.concurrency = concurrency; this.sanitizer = sanitizer; this.properties = properties; this.clock = clock;
    }

    public AiPlanResponse plan(UUID userId, boolean weekly, AiPlanRequest request) {
        requireFeature(properties.getFeatures().isChat());
        LinkedHashSet<AiContextDomain> domains = domains(request.contextDomains(), AiContextDomain.TASK_CALENDAR);
        LocalDate end = weekly ? request.periodStart().plusDays(6) : request.periodStart();
        AiContextBuilder.Snapshot context = contexts.build(userId, domains, request.periodStart(), end, null, null);
        String mode = weekly ? "WEEKLY_PLAN" : "DAILY_PLAN";
        return invoke(userId, mode, "Create a read-only " + (weekly ? "weekly" : "daily") + " plan.", context,
                Map.of("summary", "string", "items", "array"), structured -> {
                    only(structured, PLAN_KEYS);
                    String summary = text(structured, "summary", 2000, false);
                    List<Map<String, Object>> rows = objects(structured.get("items"), 0, 50);
                    Set<String> allowed = allowedRefs(context);
                    List<AiPlanItem> items = rows.stream().map(row -> planItem(row, allowed)).toList();
                    return id -> new AiPlanResponse(weekly ? "WEEK" : "DAY", request.periodStart(), summary,
                            items, id);
                });
    }

    public AiTaskBreakdownResponse breakdown(UUID userId, UUID taskId, AiTaskBreakdownRequest request) {
        requireFeature(properties.getFeatures().isChat());
        AiContextBuilder.Snapshot context = contexts.buildForTask(userId, request.contextDomains(), taskId);
        String instruction = request.instruction() == null ? "Break this task into independent steps."
                : request.instruction();
        return invoke(userId, "TASK_BREAKDOWN", instruction, context, Map.of("items", "array"), structured -> {
            only(structured, TASK_KEYS);
            List<AiTaskDraft> items = objects(structured.get("items"), 1, 10).stream().map(row -> {
                only(row, TASK_ITEM_KEYS);
                return new AiTaskDraft(text(row, "title", 200, false), text(row, "description", 2000, true),
                        choice(row, "priority", Set.of("LOW", "MEDIUM", "HIGH")),
                        date(row.get("suggestedDueDate")), text(row, "reason", 1000, false));
            }).toList();
            return id -> new AiTaskBreakdownResponse(taskId, items, id);
        });
    }

    public AiInsightResponse habitInsight(UUID userId, AiInsightRequest request) {
        requireRange(request.from(), request.to());
        requireFeature(properties.getFeatures().isInsights());
        LinkedHashSet<AiContextDomain> domains = domains(request.contextDomains(), AiContextDomain.HABIT);
        AiContextBuilder.Snapshot context = contexts.build(userId, domains, request.from(), request.to(), null, null);
        return insight(userId, "HABIT_INSIGHT", "Explain habit trends without changing data.", context);
    }

    public AiInsightResponse financeInsight(UUID userId, AiFinanceInsightRequest request) {
        requireRange(request.from(), request.to());
        requireFeature(properties.getFeatures().isFinance());
        LinkedHashSet<AiContextDomain> domains = domains(request.contextDomains(), AiContextDomain.FINANCE);
        AiContextBuilder.Snapshot context = contexts.build(userId, domains, request.from(), request.to(), null,
                request.currency());
        return insight(userId, "FINANCE_INSIGHT", "Explain finance trends in " + request.currency()
                + " without financial advice or changing data.", context);
    }

    public AiNoteSummaryResponse noteSummary(UUID userId, UUID noteId, AiNoteSummaryRequest request) {
        requireFeature(properties.getFeatures().isNotes());
        AiContextBuilder.Snapshot context = contexts.buildForNote(userId, request.contextDomains(), noteId);
        return invoke(userId, "NOTE_SUMMARY", "Summarize the selected note in " + request.style() + " style.",
                context, Map.of("summary", "string", "keyPoints", "array"), structured -> {
                    only(structured, NOTE_KEYS);
                    String summary = text(structured, "summary", 8000, false);
                    List<?> raw = list(structured.get("keyPoints"), 0, 20);
                    List<String> points = raw.stream().map(value -> bounded(value, 500, false)).toList();
                    return id -> new AiNoteSummaryResponse(noteId, summary, points, id);
                });
    }

    private AiInsightResponse insight(UUID userId, String mode, String instruction,
            AiContextBuilder.Snapshot context) {
        return invoke(userId, mode, instruction, context, Map.of("summary", "string", "observations", "array"),
                structured -> {
                    only(structured, INSIGHT_KEYS);
                    String summary = text(structured, "summary", 2000, false);
                    Set<String> allowed = allowedRefs(context);
                    List<AiObservation> observations = objects(structured.get("observations"), 0, 20).stream()
                            .map(row -> observation(row, allowed)).toList();
                    return id -> new AiInsightResponse(summary, observations, id);
                });
    }

    private <T> T invoke(UUID userId, String mode, String instruction, AiContextBuilder.Snapshot context,
            Map<String, Object> responseSchema, Function<Map<String, Object>, Function<UUID, T>> validator) {
        quota.check(userId);
        AiStore.Conversation conversation = tx.create(userId, mode.replace('_', ' '));
        tx.saveUser(userId, conversation.id(), instruction, mode);
        UUID correlationId = UUID.randomUUID();
        Instant started = clock.instant();
        try {
            LLMProvider.Response generated = concurrency.execute(userId, () -> provider.generate(
                    new LLMProvider.Request(mode, instruction, context.providerPayload(), responseSchema)));
            if (!"SUCCESS".equals(generated.resultCode())) throw invalid("Phản hồi AI chưa hoàn tất.");
            Map<String, Object> safe = sanitizeMap(generated.structured());
            Function<UUID, T> response = validator.apply(safe);
            AiStore.Message assistant = tx.saveAssistant(userId, conversation.id(), mode,
                    sanitizer.sanitize(generated.content()), safe, context.providerPayload(), generated, correlationId);
            return response.apply(assistant.id());
        } catch (RuntimeException e) {
            long latency = Math.max(0, Duration.between(started, clock.instant()).toMillis());
            tx.saveFailure(userId, conversation.id(), "FAILED", latency, correlationId);
            throw e;
        }
    }

    private AiPlanItem planItem(Map<String, Object> row, Set<String> allowed) {
        only(row, PLAN_ITEM_KEYS);
        return new AiPlanItem(text(row, "title", 200, false), text(row, "reason", 1000, false),
                choice(row, "priority", Set.of("LOW", "MEDIUM", "HIGH")), instant(row.get("suggestedStartAt")),
                instant(row.get("suggestedEndAt")), refs(row.get("sourceRefs"), allowed));
    }

    private AiObservation observation(Map<String, Object> row, Set<String> allowed) {
        only(row, OBSERVATION_KEYS);
        return new AiObservation(text(row, "title", 200, false), text(row, "detail", 2000, false),
                choice(row, "trend", Set.of("UP", "DOWN", "STABLE", "MIXED", "UNKNOWN")),
                refs(row.get("sourceRefs"), allowed));
    }

    private List<AiSourceReference> refs(Object value, Set<String> allowed) {
        List<AiSourceReference> out = new ArrayList<>();
        for (Map<String, Object> row : objects(value, 0, 50)) {
            only(row, REF_KEYS);
            try {
                AiSourceReference ref = new AiSourceReference(AiContextDomain.valueOf(text(row, "domain", 30, false)),
                        text(row, "entityType", 60, false), UUID.fromString(text(row, "entityId", 36, false)));
                if (!allowed.contains(key(ref.domain(), ref.entityType(), ref.entityId())))
                    throw invalid("AI tham chiếu dữ liệu ngoài context cho phép.");
                out.add(ref);
            } catch (RuntimeException e) {
                if (e instanceof com.lifeos.common.exception.BusinessException business) throw business;
                throw invalid("AI trả về source reference không hợp lệ.");
            }
        }
        return List.copyOf(out);
    }

    private static Set<String> allowedRefs(AiContextBuilder.Snapshot context) {
        Set<String> out = new LinkedHashSet<>();
        context.disclosure().entities().forEach(e -> out.add(key(e.domain(), e.entityType(), e.entityId())));
        return out;
    }

    private static String key(AiContextDomain domain, String type, UUID id) {
        return domain.name() + '|' + type + '|' + id;
    }

    private Map<String, Object> sanitizeMap(Map<String, Object> source) {
        if (source == null) throw invalid("AI không trả về structured output.");
        Map<String, Object> out = new LinkedHashMap<>();
        source.forEach((key, value) -> out.put(key, sanitizeValue(value)));
        return out;
    }

    private Object sanitizeValue(Object value) {
        if (value instanceof String string) return sanitizer.sanitize(string);
        if (value instanceof Map<?, ?> map) {
            Map<String, Object> out = new LinkedHashMap<>();
            map.forEach((key, nested) -> out.put(String.valueOf(key), sanitizeValue(nested)));
            return out;
        }
        if (value instanceof List<?> list) return list.stream().map(this::sanitizeValue).toList();
        return value;
    }

    private static LinkedHashSet<AiContextDomain> domains(Set<AiContextDomain> requested,
            AiContextDomain required) {
        LinkedHashSet<AiContextDomain> result = new LinkedHashSet<>(requested);
        result.add(required);
        return result;
    }

    private static void requireFeature(boolean enabled) {
        if (!enabled) throw AiErrors.error("AI_DISABLED", HttpStatus.SERVICE_UNAVAILABLE,
                "Tính năng AI này đang tắt.");
    }

    private static void requireRange(LocalDate from, LocalDate to) {
        if (from.isAfter(to) || from.plusDays(366).isBefore(to)) throw invalid("Khoảng insight không hợp lệ.");
    }

    private static void only(Map<String, Object> map, Set<String> allowed) {
        if (!allowed.containsAll(map.keySet()) || !map.keySet().containsAll(allowed))
            throw invalid("Structured output có trường thiếu hoặc không được phép.");
    }

    private static String text(Map<String, Object> map, String key, int max, boolean nullable) {
        return bounded(map.get(key), max, nullable);
    }

    private static String bounded(Object value, int max, boolean nullable) {
        if (value == null || String.valueOf(value).isBlank()) {
            if (nullable) return null;
            throw invalid("Structured output thiếu chuỗi bắt buộc.");
        }
        String text = String.valueOf(value);
        if (text.length() > max) throw invalid("Structured output vượt giới hạn.");
        return text;
    }

    private static String choice(Map<String, Object> map, String key, Set<String> allowed) {
        String value = text(map, key, 30, false);
        if (!allowed.contains(value)) throw invalid("Structured output có enum không hợp lệ.");
        return value;
    }

    private static Instant instant(Object value) {
        if (value == null || String.valueOf(value).isBlank()) return null;
        try { return Instant.parse(String.valueOf(value)); } catch (DateTimeParseException e) {
            throw invalid("Structured output có date-time không hợp lệ.");
        }
    }

    private static LocalDate date(Object value) {
        if (value == null || String.valueOf(value).isBlank()) return null;
        try { return LocalDate.parse(String.valueOf(value)); } catch (DateTimeParseException e) {
            throw invalid("Structured output có ngày không hợp lệ.");
        }
    }

    private static List<Map<String, Object>> objects(Object value, int min, int max) {
        List<?> source = list(value, min, max);
        List<Map<String, Object>> out = new ArrayList<>();
        for (Object item : source) {
            if (!(item instanceof Map<?, ?> map)) throw invalid("Structured output cần object.");
            Map<String, Object> row = new LinkedHashMap<>();
            map.forEach((key, nested) -> row.put(String.valueOf(key), nested));
            out.add(row);
        }
        return out;
    }

    private static List<?> list(Object value, int min, int max) {
        if (!(value instanceof List<?> list) || list.size() < min || list.size() > max)
            throw invalid("Structured output có số phần tử không hợp lệ.");
        return list;
    }

    private static com.lifeos.common.exception.BusinessException invalid(String detail) {
        return AiErrors.error("AI_RESPONSE_INVALID", HttpStatus.BAD_GATEWAY, detail);
    }
}
