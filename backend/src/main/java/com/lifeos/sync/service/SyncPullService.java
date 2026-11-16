package com.lifeos.sync.service;

import com.lifeos.common.exception.ErrorCode;
import com.lifeos.common.exception.FieldErrors;
import com.lifeos.common.sync.EntitySnapshot;
import com.lifeos.common.sync.SyncEntityHandler;
import com.lifeos.sync.dto.PullChange;
import com.lifeos.sync.dto.PullResponse;
import com.lifeos.sync.repository.ChangeLogReader;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * GET /sync/pull theo state (SYSTEM_ARCHITECTURE §3.7, SYNC_PROTOCOL §6, D89). Giao dịch CHỈ ĐỌC; không khóa
 * user_sync_state, không gọi ChangeRecorder (SYN-43).
 * <ol>
 *   <li>limit vắng = lifeos.sync.pull-page-size; ngoài [1, 500] → 400. cursor theo {@link SyncCursorCodec}.</li>
 *   <li>cursor &gt; last_seq (ví dụ DB khôi phục) → trang rỗng, hasMore=false, nextCursor = chính cursor đó.</li>
 *   <li>Đọc N+1 dòng seq &gt; cursor; hasMore = có dòng thứ N+1 (bỏ đi). nextCursor = seq lớn nhất của N dòng đã đọc
 *       (kể cả dòng bị gom), không vượt dòng đã đọc; trang rỗng → nextCursor = cursor đầu vào.</li>
 *   <li>Gom theo (entity_type, entity_id) giữ dòng seq cao nhất; changes[] theo seq của dòng đó.</li>
 *   <li>loadSnapshots MỘT lần mỗi entityType (truy vấn theo lô có user_id). Đã xóa hoặc không còn → DELETE kèm
 *       version; ngược lại UPSERT kèm data.</li>
 * </ol>
 */
@Service
public class SyncPullService {

    /** Trần cứng, không cấu hình được (SYNC_PROTOCOL §8). */
    static final int MAX_LIMIT = 500;

    private static final Logger log = LoggerFactory.getLogger(SyncPullService.class);

    private final ChangeLogReader changeLogs;
    private final SyncHandlerRegistry handlers;
    private final SyncMetrics metrics;
    private final int defaultLimit;

    public SyncPullService(ChangeLogReader changeLogs, SyncHandlerRegistry handlers, SyncMetrics metrics,
            @Value("${lifeos.sync.pull-page-size}") int defaultLimit) {
        this.changeLogs = changeLogs;
        this.handlers = handlers;
        this.metrics = metrics;
        this.defaultLimit = Math.min(defaultLimit, MAX_LIMIT);
    }

    /**
     * REPEATABLE READ: kiểm min_retained_seq và đọc trang trên CÙNG một snapshot, nên retention chạy song song không thể
     * làm trang thiếu dòng một cách im lặng (D150, D153).
     */
    @Transactional(readOnly = true, isolation = org.springframework.transaction.annotation.Isolation.REPEATABLE_READ)
    public PullResponse pull(UUID userId, String cursor, Integer limit) {
        long started = System.nanoTime();
        int size = limit == null ? defaultLimit : limit;
        if (size < 1 || size > MAX_LIMIT) {
            throw FieldErrors.single(ErrorCode.VALIDATION_FAILED, "limit", "SIZE",
                    "limit phải từ 1 đến " + MAX_LIMIT + ".");
        }
        long after = SyncCursorCodec.decode(cursor);
        String unchanged = cursor == null || cursor.isEmpty() ? SyncCursorCodec.encode(after) : cursor;

        long minRetained = changeLogs.minRetainedSeq(userId);
        if (after < minRetained) {
            metrics.fullResyncRequired();
            com.lifeos.common.exception.BusinessException gone = new com.lifeos.common.exception.BusinessException(
                    ErrorCode.SYNC_FULL_RESYNC_REQUIRED);
            gone.with("minRetainedSeq", minRetained).with("currentSeq", changeLogs.lastSeq(userId));
            throw gone;
        }
        PullResponse response;
        if (after > changeLogs.lastSeq(userId)) {
            response = new PullResponse(List.of(), unchanged, false);
        } else {
            List<ChangeLogReader.Row> rows = changeLogs.page(userId, after, size + 1);
            boolean hasMore = rows.size() > size;
            List<ChangeLogReader.Row> page = hasMore ? rows.subList(0, size) : rows;
            String next = page.isEmpty() ? unchanged : SyncCursorCodec.encode(page.get(page.size() - 1).seq());
            response = new PullResponse(changes(userId, page), next, hasMore);
        }

        Duration duration = Duration.ofNanos(System.nanoTime() - started);
        metrics.pull(duration, response.changes().size());
        log.atInfo()
                .addKeyValue("event", "sync_pull")
                .addKeyValue("userId", userId)
                .addKeyValue("changes", response.changes().size())
                .addKeyValue("hasMore", response.hasMore())
                .addKeyValue("durationMs", duration.toMillis())
                .log("sync_pull");
        return response;
    }

    private List<PullChange> changes(UUID userId, List<ChangeLogReader.Row> page) {
        // Gom: giữ dòng seq cao nhất của mỗi entity (page đã theo seq tăng nên dòng sau thắng).
        Map<String, ChangeLogReader.Row> latest = new LinkedHashMap<>();
        for (ChangeLogReader.Row row : page) {
            latest.remove(key(row));
            latest.put(key(row), row);
        }
        List<ChangeLogReader.Row> ordered = new ArrayList<>(latest.values());
        ordered.sort(Comparator.comparingLong(ChangeLogReader.Row::seq));

        Map<String, Map<UUID, EntitySnapshot>> snapshots = new LinkedHashMap<>();
        ordered.stream().collect(Collectors.groupingBy(ChangeLogReader.Row::entityType, LinkedHashMap::new,
                        Collectors.mapping(ChangeLogReader.Row::entityId, Collectors.toList())))
                .forEach((type, ids) -> snapshots.put(type, handler(type).loadSnapshots(userId, ids).stream()
                        .collect(Collectors.toMap(EntitySnapshot::entityId, Function.identity()))));

        List<PullChange> changes = new ArrayList<>(ordered.size());
        for (ChangeLogReader.Row row : ordered) {
            EntitySnapshot snapshot = snapshots.get(row.entityType()).get(row.entityId());
            if (snapshot == null || snapshot.deleted()) {
                long version = snapshot == null ? row.entityVersion() : snapshot.version();
                changes.add(new PullChange(row.entityType(), row.entityId(), PullChange.DELETE, version, null));
            } else {
                changes.add(new PullChange(row.entityType(), row.entityId(), PullChange.UPSERT, snapshot.version(),
                        snapshot.data()));
            }
        }
        return changes;
    }

    private SyncEntityHandler handler(String entityType) {
        // change_logs.entity_type có CHECK (DATABASE_DESIGN §4.2) nên luôn có handler; thiếu là lỗi cấu hình server.
        return handlers.find(entityType)
                .orElseThrow(() -> new IllegalStateException("Không có SyncEntityHandler cho " + entityType));
    }

    private static String key(ChangeLogReader.Row row) {
        return row.entityType() + ":" + row.entityId();
    }
}
