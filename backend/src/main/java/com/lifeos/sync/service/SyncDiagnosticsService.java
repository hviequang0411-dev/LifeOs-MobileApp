package com.lifeos.sync.service;

import com.lifeos.common.exception.ErrorCode;
import com.lifeos.common.exception.FieldErrors;
import com.lifeos.common.exception.RateLimitedException;
import com.lifeos.common.security.RateLimitPolicy;
import com.lifeos.common.security.RateLimiter;
import com.lifeos.sync.repository.RetentionStore;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.json.JsonMapper;

/**
 * Báo cáo chẩn đoán đồng bộ (PHASE_5 D170, SYN-A16): ≤ 64 KB, 5 lần/giờ/user, lưu 30 ngày, chỉ vận hành đọc. Schema cố
 * định để KHÔNG nhận dữ liệu trường: khóa gốc {appVersion, schemaVersion, cursor, mutationCounts, errors, integrity};
 * mỗi lỗi chỉ {code, entityType, at} (≤ 50); khóa lạ → 400 VALIDATION_FAILED [PROPOSED schema].
 */
@Service
public class SyncDiagnosticsService {

    static final int MAX_BYTES = 65_536;
    private static final Set<String> ROOT = Set.of("appVersion", "schemaVersion", "cursor", "mutationCounts",
            "errors", "integrity");
    private static final Set<String> ERROR = Set.of("code", "entityType", "at");

    private final RetentionStore store;
    private final RateLimiter rateLimiter;
    private final JsonMapper json;
    private final Clock clock;

    public SyncDiagnosticsService(RetentionStore store, RateLimiter rateLimiter, JsonMapper json, Clock clock) {
        this.store = store;
        this.rateLimiter = rateLimiter;
        this.json = json;
        this.clock = clock;
    }

    @Transactional
    public UUID submit(UUID userId, Map<String, Object> report) {
        RateLimiter.Decision decision = rateLimiter.tryAcquire(RateLimitPolicy.SYNC_DIAGNOSTICS_USER,
                userId.toString());
        if (!decision.allowed()) {
            throw new RateLimitedException(decision.retryAfterSeconds());
        }
        for (String key : report.keySet()) {
            if (!ROOT.contains(key)) {
                throw invalid(key);
            }
        }
        if (report.get("mutationCounts") != null && !(report.get("mutationCounts") instanceof Map<?, ?> counts
                && counts.values().stream().allMatch(v -> v instanceof Number))) {
            throw invalid("mutationCounts");
        }
        if (report.get("integrity") != null && !(report.get("integrity") instanceof Map<?, ?> integrity
                && integrity.values().stream().allMatch(v -> v instanceof Boolean || v instanceof Number))) {
            throw invalid("integrity");
        }
        Object errors = report.get("errors");
        if (errors != null) {
            if (!(errors instanceof List<?> list) || list.size() > 50) {
                throw invalid("errors");
            }
            for (Object item : list) {
                if (!(item instanceof Map<?, ?> error)
                        || !ERROR.containsAll(error.keySet().stream().map(String::valueOf).toList())
                        || error.values().stream().anyMatch(v -> v != null && !(v instanceof String))) {
                    throw invalid("errors");
                }
            }
        }
        for (String key : List.of("appVersion", "schemaVersion", "cursor")) {
            Object value = report.get(key);
            if (value != null && !(value instanceof Number) && !(value instanceof String text && text.length() <= 100)) {
                throw invalid(key);
            }
        }
        String payload = json.writeValueAsString(report);
        if (payload.getBytes(StandardCharsets.UTF_8).length > MAX_BYTES) {
            throw FieldErrors.single(ErrorCode.VALIDATION_FAILED, "report", "SIZE", "Tối đa 64 KB.");
        }
        UUID id = UUID.randomUUID();
        store.insertDiagnostics(id, userId, payload, clock.instant());
        return id;
    }

    private static RuntimeException invalid(String field) {
        return FieldErrors.single(ErrorCode.VALIDATION_FAILED, field, "INVALID_VALUE",
                "Báo cáo chẩn đoán chỉ chứa siêu dữ liệu (không có dữ liệu trường).");
    }
}
