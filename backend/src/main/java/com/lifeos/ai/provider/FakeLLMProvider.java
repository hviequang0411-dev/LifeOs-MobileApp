package com.lifeos.ai.provider;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/** Deterministic fake used by all automated tests; never performs network I/O. */
@Component
@Profile("test")
public class FakeLLMProvider implements LLMProvider {
    private final AtomicInteger calls = new AtomicInteger();
    private final AtomicReference<Request> lastRequest = new AtomicReference<>();
    private final AtomicReference<Response> nextResponse = new AtomicReference<>();
    private final AtomicBoolean lastCallInTransaction = new AtomicBoolean();

    @Override
    public Response generate(Request request) {
        calls.incrementAndGet();
        lastRequest.set(request);
        lastCallInTransaction.set(TransactionSynchronizationManager.isActualTransactionActive());
        Response scripted = nextResponse.getAndSet(null);
        if (scripted != null) return scripted;
        Map<String, Object> structured = new LinkedHashMap<>();
        switch (request.mode()) {
            case "DAILY_PLAN", "WEEKLY_PLAN" -> {
                structured.put("summary", "fake-plan");
                structured.put("items", List.of(Map.of("title", "fake-item", "reason", "fake-reason",
                        "priority", "MEDIUM", "suggestedStartAt", nullValue(), "suggestedEndAt", nullValue(),
                        "sourceRefs", List.of())));
            }
            case "TASK_BREAKDOWN" -> structured.put("items", List.of(Map.of("title", "fake-step",
                    "description", "fake-description", "priority", "MEDIUM", "suggestedDueDate", nullValue(),
                    "reason", "fake-reason")));
            case "HABIT_INSIGHT", "FINANCE_INSIGHT" -> {
                structured.put("summary", "fake-insight");
                structured.put("observations", List.of(Map.of("title", "fake-observation", "detail", "fake-detail",
                        "trend", "STABLE", "sourceRefs", List.of())));
            }
            case "NOTE_SUMMARY" -> {
                structured.put("summary", "fake-summary");
                structured.put("keyPoints", List.of("fake-key-point"));
            }
            default -> {
                structured.put("mode", request.mode());
                structured.put("summary", "fake-response");
            }
        }
        return new Response("fake-response", structured, 11, 0, 7, 0, 3, "SUCCESS");
    }

    public int calls() {
        return calls.get();
    }

    public Request lastRequest() {
        return lastRequest.get();
    }

    public boolean lastCallInTransaction() {
        return lastCallInTransaction.get();
    }

    public void respondNext(Response response) {
        nextResponse.set(response);
    }

    public void reset() {
        calls.set(0);
        lastRequest.set(null);
        nextResponse.set(null);
        lastCallInTransaction.set(false);
    }

    private static Object nullValue() {
        return "";
    }
}
