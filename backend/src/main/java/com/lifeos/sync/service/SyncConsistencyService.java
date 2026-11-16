package com.lifeos.sync.service;

import com.lifeos.common.sync.SyncEntityHandler;
import com.lifeos.sync.repository.RetentionStore;
import java.time.Clock;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Kiểm tra nhất quán server (PHASE_5 D172, SYN-A17): CHỈ ĐỌC, không tự sửa. (a) last_seq ≥ seq lớn nhất; (b) entity còn
 * sống có version khớp dòng change_logs mới nhất; (c) mutation_id mồ côi (trong hạn giữ sync_mutations). Kết quả là
 * gauge {@code lifeos.sync.consistency.violations{check}}.
 */
@Service
public class SyncConsistencyService {

    private static final Logger log = LoggerFactory.getLogger(SyncConsistencyService.class);

    private final RetentionStore store;
    private final SyncHandlerRegistry handlers;
    private final SyncMetrics metrics;
    private final Clock clock;

    public SyncConsistencyService(RetentionStore store, SyncHandlerRegistry handlers, SyncMetrics metrics,
            Clock clock) {
        this.store = store;
        this.handlers = handlers;
        this.metrics = metrics;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public Map<String, Long> run() {
        Map<String, Long> result = new LinkedHashMap<>();
        result.put("seq", store.usersBehindLog());
        long mismatches = 0;
        for (SyncEntityHandler handler : handlers.all()) {
            mismatches += store.versionMismatches(handler.table(), handler.entityType());
        }
        result.put("version", mismatches);
        result.put("mutation", store.orphanMutationIds(clock.instant()
                .minus(SyncRetentionService.MUTATION_RETENTION.minus(Duration.ofDays(1)))));
        result.forEach(metrics::consistency);
        log.atInfo().addKeyValue("event", "sync_consistency").addKeyValue("seq", result.get("seq"))
                .addKeyValue("version", result.get("version")).addKeyValue("mutation", result.get("mutation"))
                .log("sync_consistency");
        return result;
    }
}
