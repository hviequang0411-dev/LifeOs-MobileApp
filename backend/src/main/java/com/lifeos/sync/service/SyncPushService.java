package com.lifeos.sync.service;

import com.lifeos.common.exception.BusinessException;
import com.lifeos.common.exception.ConflictException;
import com.lifeos.common.exception.ErrorCode;
import com.lifeos.common.exception.FieldErrors;
import com.lifeos.common.exception.LockFailures;
import com.lifeos.common.exception.NaturalKeyConflictException;
import com.lifeos.common.exception.NotFoundException;
import com.lifeos.common.sync.ChangeOperation;
import com.lifeos.common.sync.EntitySnapshot;
import com.lifeos.common.sync.Mutation;
import com.lifeos.common.sync.MutationOutcome;
import com.lifeos.common.sync.SyncEntityHandler;
import com.lifeos.sync.dto.MutationResult;
import com.lifeos.sync.dto.PushResponse;
import com.lifeos.sync.dto.ServerSnapshot;
import com.lifeos.sync.repository.MutationResultStore;
import java.time.Clock;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.UUID;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import tools.jackson.databind.json.JsonMapper;

/**
 * POST /sync/push (SYNC_PROTOCOL §5, SYSTEM_ARCHITECTURE §3.5). Mỗi mutation một giao dịch T1
 * ({@link TransactionTemplate}; @Transactional chỉ ở ..service.., A10): handler gọi *CommandService (record +
 * insert APPLIED cùng commit). CONFLICT/REJECTED: T1 rollback, kết quả ghi bằng giao dịch T2 mới. Lỗi khóa/deadlock và
 * lỗi không dự kiến dừng cả request, KHÔNG lưu kết quả của mutation đang xử lý (D85). Module này không biết module
 * nghiệp vụ nào: chỉ thu thập {@link SyncEntityHandler} từ context.
 */
@Service
public class SyncPushService {

    private static final Logger log = LoggerFactory.getLogger(SyncPushService.class);

    private final SyncHandlerRegistry handlers;
    private final MutationResultStore store;
    private final TransactionTemplate writeTx;
    private final TransactionTemplate readTx;
    private final JsonMapper json;
    private final SyncMetrics metrics;
    private final Clock clock;
    private final int payloadMaxBytes;

    public SyncPushService(SyncHandlerRegistry handlers, MutationResultStore store,
            PlatformTransactionManager transactionManager, JsonMapper json, SyncMetrics metrics, Clock clock,
            @Value("${lifeos.sync.payload-max-bytes}") int payloadMaxBytes) {
        this.handlers = handlers;
        this.store = store;
        this.writeTx = new TransactionTemplate(transactionManager);
        this.readTx = new TransactionTemplate(transactionManager);
        this.readTx.setReadOnly(true);
        this.json = json;
        this.metrics = metrics;
        this.clock = clock;
        this.payloadMaxBytes = payloadMaxBytes;
    }

    /** Thiết bị của request hiện tại (D163, chỉ để chẩn đoán); đặt trong {@link #push(UUID, UUID, List)}. */
    private static final ThreadLocal<UUID> DEVICE = new ThreadLocal<>();

    public PushResponse push(UUID userId, UUID deviceIdOrNull, List<PushMutation> mutations) {
        DEVICE.set(deviceIdOrNull);
        try {
            return push(userId, mutations);
        } finally {
            DEVICE.remove();
        }
    }

    public PushResponse push(UUID userId, List<PushMutation> mutations) {
        long started = System.nanoTime();
        List<MutationResult> results = new ArrayList<>(mutations.size());
        try {
            for (PushMutation mutation : mutations) {
                results.add(process(userId, mutation));
            }
        } catch (RuntimeException e) {
            if (LockFailures.isLockFailure(e)) {
                metrics.lockTimeout();
            }
            throw e;
        } finally {
            metrics.pushDuration(Duration.ofNanos(System.nanoTime() - started));
        }
        Map<String, Long> counts = new TreeMap<>(results.stream()
                .collect(Collectors.groupingBy(MutationResult::status, Collectors.counting())));
        counts.forEach(metrics::mutations);
        // SYNC_PROTOCOL §7.2: chỉ userId, correlationId (MDC), số mutation theo trạng thái, thời gian. Không payload.
        log.atInfo()
                .addKeyValue("event", "sync_push")
                .addKeyValue("userId", userId)
                .addKeyValue("mutations", results.size())
                .addKeyValue("byStatus", counts)
                .addKeyValue("durationMs", Duration.ofNanos(System.nanoTime() - started).toMillis())
                .log("sync_push");
        return new PushResponse(results);
    }

    private MutationResult process(UUID userId, PushMutation pushed) {
        Mutation m = pushed.mutation();
        Optional<MutationResultStore.Stored> stored = store.find(userId, m.mutationId());
        if (stored.isPresent()) {
            return duplicate(userId, m, stored.get());
        }
        SyncEntityHandler handler = handlers.find(m.entityType()).orElse(null);
        MutationOutcome precheck = precheck(handler, pushed);
        if (precheck != null) {
            return persistOutsideT1(userId, m, precheck);
        }
        try {
            MutationOutcome outcome = writeTx.execute(status -> {
                MutationOutcome applied = handler.apply(userId, m);
                if (applied instanceof MutationOutcome.Applied) {
                    MutationResult result = toResult(m, applied);
                    store.insert(row(userId, m, applied, result), clock.instant());
                } else {
                    status.setRollbackOnly();
                }
                return applied;
            });
            return outcome instanceof MutationOutcome.Applied ? toResult(m, outcome)
                    : persistOutsideT1(userId, m, outcome);
        } catch (DuplicateKeyException e) {
            if (!mentions(e, MutationResultStore.PK_CONSTRAINT)) {
                throw e;
            }
            // Request song song cùng mutationId đã ghi trước: T1 rollback, phát lại kết quả của request đó.
            return duplicate(userId, m, store.find(userId, m.mutationId()).orElseThrow(() -> e));
        } catch (BusinessException e) {
            return persistOutsideT1(userId, m, toOutcome(e, handler, userId, m));
        }
    }

    /** Thứ tự D93: entityType lạ → payload quá lớn → thao tác không được phép → CREATE có baseVersion ≠ 0. */
    private MutationOutcome precheck(SyncEntityHandler handler, PushMutation pushed) {
        Mutation m = pushed.mutation();
        if (handler == null) {
            return new MutationOutcome.Rejected(ErrorCode.SYNC_UNKNOWN_ENTITY_TYPE);
        }
        if (pushed.payloadBytes() > handler.maxPayloadBytes(payloadMaxBytes)) {
            return new MutationOutcome.Rejected(ErrorCode.PAYLOAD_TOO_LARGE);
        }
        if (!handler.supports(m.operation())) {
            return new MutationOutcome.Rejected(ErrorCode.SYNC_OPERATION_NOT_ALLOWED);
        }
        if (m.operation() == ChangeOperation.CREATE && m.baseVersion() != 0) {
            return new MutationOutcome.Rejected(ErrorCode.VALIDATION_FAILED,
                    List.of(Map.of("field", "baseVersion", "code", "INVALID_VALUE")));
        }
        return null;
    }

    /**
     * Ánh xạ lỗi nghiệp vụ của service sang kết quả mutation (SYNC_PROTOCOL §4, D81, D84; Q14). Chạy SAU khi T1 đã
     * rollback; snapshot đọc trong giao dịch chỉ đọc MỚI.
     * <ul>
     *   <li>ENTITY_VERSION_CONFLICT (kể cả do @Version) → CONFLICT + snapshot hiện tại.</li>
     *   <li>NotFoundException (404 — entity ĐÍCH không thấy): tombstone của chính user → UPDATE: CONFLICT deleted;
     *       DELETE: APPLIED version tombstone, không ghi gì. Không thấy → REJECTED &lt;TYPE&gt;_NOT_FOUND.
     *       Tham chiếu sai (projectId/categoryId) là ValidationException 422, KHÔNG đi nhánh này.</li>
     *   <li>Mọi lỗi nghiệp vụ khác → REJECTED cùng errorCode/fieldErrors như REST.</li>
     * </ul>
     */
    private MutationOutcome toOutcome(BusinessException e, SyncEntityHandler handler, UUID userId, Mutation m) {
        if (e instanceof NaturalKeyConflictException duplicate) {
            // D235: CREATE trùng khóa tự nhiên (ví dụ check-in cùng habit + ngày) → CONFLICT kèm bản đang tồn tại.
            return snapshot(handler, userId, duplicate.existingId())
                    .<MutationOutcome>map(s -> new MutationOutcome.Conflict(s, MutationOutcome.Conflict.ALREADY_EXISTS))
                    .orElseGet(() -> new MutationOutcome.Rejected(e.getErrorCode()));
        }
        if (e instanceof ConflictException && e.getErrorCode() == ErrorCode.ENTITY_VERSION_CONFLICT) {
            return snapshot(handler, userId, m.entityId())
                    .<MutationOutcome>map(MutationOutcome.Conflict::new)
                    .orElseGet(() -> new MutationOutcome.Rejected(e.getErrorCode()));
        }
        if (e instanceof NotFoundException && m.operation() != ChangeOperation.CREATE) {
            Optional<EntitySnapshot> tombstone = snapshot(handler, userId, m.entityId()).filter(EntitySnapshot::deleted);
            if (tombstone.isPresent()) {
                return m.operation() == ChangeOperation.DELETE
                        ? new MutationOutcome.Applied(tombstone.get().version())
                        : new MutationOutcome.Conflict(tombstone.get());
            }
            return new MutationOutcome.Rejected(e.getErrorCode());
        }
        return new MutationOutcome.Rejected(e.getErrorCode(), fieldErrors(e));
    }

    private Optional<EntitySnapshot> snapshot(SyncEntityHandler handler, UUID userId, UUID entityId) {
        return Optional.ofNullable(readTx.execute(status -> handler.loadSnapshots(userId, List.of(entityId)).stream()
                .filter(s -> s.entityId().equals(entityId))
                .findFirst()
                .orElse(null)));
    }

    /** Ghi kết quả không áp dụng qua T1 (CONFLICT/REJECTED, hoặc APPLIED của DELETE tombstone) bằng giao dịch T2. */
    private MutationResult persistOutsideT1(UUID userId, Mutation m, MutationOutcome outcome) {
        MutationResult result = toResult(m, outcome);
        Integer inserted = writeTx.execute(status -> store.insertIfAbsent(row(userId, m, outcome, result),
                clock.instant()));
        if (inserted != null && inserted == 0) {
            return duplicate(userId, m, store.find(userId, m.mutationId()).orElseThrow());
        }
        return result;
    }

    /** D83: phát lại nguyên văn kết quả đã lưu + originalStatus; mutationId tái dùng với metadata khác → log. */
    private MutationResult duplicate(UUID userId, Mutation m, MutationResultStore.Stored stored) {
        boolean sameMetadata = stored.entityType().equals(m.entityType()) && stored.entityId().equals(m.entityId())
                && stored.operation().equals(m.operation().name()) && stored.baseVersion() == m.baseVersion();
        if (!sameMetadata) {
            log.atWarn()
                    .addKeyValue("event", "sync_mutation_id_reuse")
                    .addKeyValue("userId", userId)
                    .addKeyValue("mutationId", m.mutationId())
                    .log("sync_mutation_id_reuse");
        }
        return json.readValue(stored.resultJson(), MutationResult.class).asDuplicate();
    }

    private MutationResult toResult(Mutation m, MutationOutcome outcome) {
        return switch (outcome) {
            case MutationOutcome.Applied applied -> new MutationResult(m.mutationId(), MutationResult.APPLIED, null,
                    applied.entityVersion(), null, null, null, null, null);
            case MutationOutcome.Conflict conflict -> {
                EntitySnapshot s = conflict.serverSnapshot();
                yield new MutationResult(m.mutationId(), MutationResult.CONFLICT, null, null, conflict.reason(),
                        new ServerSnapshot(s.entityType(), s.entityId(), s.version(), s.deleted(),
                                s.deleted() ? null : s.data()),
                        null, null, null);
            }
            case MutationOutcome.Rejected rejected -> new MutationResult(m.mutationId(), MutationResult.REJECTED, null,
                    null, null, null, rejected.errorCode().name(), rejected.errorCode().title(),
                    rejected.fieldErrors().isEmpty() ? null : rejected.fieldErrors());
        };
    }

    private MutationResultStore.Row row(UUID userId, Mutation m, MutationOutcome outcome, MutationResult result) {
        Long resultVersion = switch (outcome) {
            case MutationOutcome.Applied applied -> applied.entityVersion();
            case MutationOutcome.Conflict conflict -> conflict.serverSnapshot().version();
            case MutationOutcome.Rejected rejected -> null;
        };
        String errorCode = outcome instanceof MutationOutcome.Rejected rejected ? rejected.errorCode().name() : null;
        return new MutationResultStore.Row(userId, m.mutationId(), m.entityType(), m.entityId(),
                m.operation().name(), m.baseVersion(), result.status(), resultVersion, errorCode,
                json.writeValueAsString(result), DEVICE.get());
    }

    /** fieldErrors của lỗi service → {field, code} (SYNC_PROTOCOL §2.2), bỏ message theo trường. */
    @SuppressWarnings("unchecked")
    private static List<Map<String, Object>> fieldErrors(BusinessException e) {
        Object raw = e.getProperties().get(FieldErrors.PROPERTY);
        if (!(raw instanceof List<?> list)) {
            return List.of();
        }
        List<Map<String, Object>> result = new ArrayList<>();
        for (Object item : list) {
            if (item instanceof Map<?, ?> map) {
                Map<String, Object> entry = new LinkedHashMap<>();
                entry.put("field", map.get("field"));
                entry.put("code", map.get("code"));
                result.add(entry);
            }
        }
        return result;
    }

    private static boolean mentions(Throwable e, String constraint) {
        for (Throwable t = e; t != null; t = t.getCause()) {
            if (t.getMessage() != null && t.getMessage().contains(constraint)) {
                return true;
            }
        }
        return false;
    }
}
