package com.lifeos.sync.service;

import com.lifeos.common.sync.ChangeOperation;
import com.lifeos.common.sync.ChangeRecorder;
import com.lifeos.sync.repository.ChangeLogRepository;
import com.lifeos.sync.repository.UserSyncStateRepository;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Cài đặt tối thiểu của ChangeRecorder (SYSTEM_ARCHITECTURE §3.6, quyết định (b) Giai đoạn 1): cấp seq bằng upsert
 * user_sync_state rồi ghi change_logs. MANDATORY: bắt buộc chạy trong transaction nghiệp vụ của bên gọi, không bao giờ
 * tự mở transaction riêng (nếu không, change_logs có thể commit khi thay đổi nghiệp vụ rollback).
 * Câu upsert seq là nơi duy nhất chờ khóa dòng user_sync_state; thời gian chạy của nó được đo vào lifeos.sync.lock.wait
 * (D91, D96). lock_timeout (D87) đặt ở mức kết nối.
 */
@Service
public class ChangeRecorderImpl implements ChangeRecorder {

    private final UserSyncStateRepository userSyncState;
    private final ChangeLogRepository changeLogs;
    private final Clock clock;
    private final SyncMetrics metrics;

    public ChangeRecorderImpl(UserSyncStateRepository userSyncState, ChangeLogRepository changeLogs, Clock clock,
            SyncMetrics metrics) {
        this.userSyncState = userSyncState;
        this.changeLogs = changeLogs;
        this.clock = clock;
        this.metrics = metrics;
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public long record(UUID userId, String entityType, UUID entityId, ChangeOperation operation, long entityVersion,
            UUID mutationIdOrNull) {
        Objects.requireNonNull(userId, "userId");
        Objects.requireNonNull(entityType, "entityType");
        Objects.requireNonNull(entityId, "entityId");
        Objects.requireNonNull(operation, "operation");
        if (entityVersion < 1) {
            throw new IllegalArgumentException("entityVersion phải >= 1");
        }
        Instant now = clock.instant();
        long started = System.nanoTime();
        long seq = userSyncState.nextSeq(userId, now);
        metrics.lockWait(Duration.ofNanos(System.nanoTime() - started));
        changeLogs.insert(userId, seq, entityType, entityId, operation, entityVersion, mutationIdOrNull, now);
        return seq;
    }
}
