package com.lifeos.sync.service;

import com.lifeos.common.sync.SyncEntityHandler;
import com.lifeos.sync.repository.RetentionStore;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Retention của dữ liệu đồng bộ (PHASE_5 D153, D154, D156; BACKEND_MASTER_PLAN §4: change_logs 180 ngày + giữ ≥ 1000
 * seq gần nhất, sync_mutations 35 ngày, tombstone 180 ngày). Theo lô 5000 dòng, mỗi lô một transaction NGẮN:
 * <ul>
 *   <li>change_logs: khóa user_sync_state FOR UPDATE (tuần tự với ChangeRecorder), xóa lô, nâng min_retained_seq =
 *       seq lớn nhất đã xóa trong CÙNG transaction. Pull đọc min + trang trong REPEATABLE READ nên không sót im lặng.</li>
 *   <li>tombstone: xóa cứng khi deleted_at quá hạn, mọi change_logs của entity đã bị dọn (thiết bị hợp lệ không cần nữa)
 *       và không còn dòng con tham chiếu; thứ tự con trước cha. Giao dịch Finance sinh từ định kỳ bị giữ khi dòng định
 *       kỳ còn tồn tại (khóa UNIQUE D110, §4 kế hoạch: không tạo bảng phụ).</li>
 * </ul>
 */
@Service
public class SyncRetentionService {

    public static final Duration CHANGE_LOG_RETENTION = Duration.ofDays(180);
    public static final long MIN_KEPT_SEQ = 1000;
    public static final Duration MUTATION_RETENTION = Duration.ofDays(35);
    public static final Duration TOMBSTONE_RETENTION = Duration.ofDays(180);
    public static final Duration DIAGNOSTICS_RETENTION = Duration.ofDays(30);
    static final int BATCH = 5000;
    private static final Logger log = LoggerFactory.getLogger(SyncRetentionService.class);

    /**
     * Thứ tự xóa cứng (con trước cha) và khóa ngoại của dòng con trỏ tới từng bảng ("bảng_con.cột"). Mọi bảng có
     * SyncEntityHandler phải có mặt (kiểm ở {@link #runTombstones}).
     */
    static final Map<String, List<String>> TOMBSTONE_PLAN = plan();

    private static Map<String, List<String>> plan() {
        Map<String, List<String>> p = new LinkedHashMap<>();
        p.put("reviews", List.of());
        p.put("note_tags", List.of());
        p.put("note_links", List.of());
        p.put("time_blocks", List.of());
        p.put("event_exceptions", List.of());
        p.put("reminders", List.of());
        p.put("routine_step_logs", List.of());
        p.put("habit_checkins", List.of());
        p.put("saving_contributions", List.of());
        p.put("financial_transactions", List.of());
        p.put("account_transfers", List.of());
        p.put("budgets", List.of("budget_alerts.budget_id"));
        p.put("recurring_transactions", List.of("financial_transactions.recurring_transaction_id"));
        p.put("tasks", List.of("time_blocks.task_id"));
        p.put("routine_logs", List.of("routine_step_logs.routine_log_id"));
        p.put("routine_steps", List.of("routine_step_logs.step_id"));
        p.put("routines", List.of("routine_steps.routine_id", "routine_logs.routine_id"));
        p.put("habits", List.of("habit_checkins.habit_id"));
        p.put("calendar_events", List.of("event_exceptions.event_id"));
        p.put("saving_goals", List.of("saving_contributions.goal_id"));
        p.put("financial_categories", List.of("financial_transactions.category_id", "budgets.category_id",
                "recurring_transactions.category_id"));
        p.put("financial_accounts", List.of("financial_transactions.account_id", "account_transfers.from_account_id",
                "account_transfers.to_account_id", "recurring_transactions.account_id"));
        p.put("projects", List.of("tasks.project_id"));
        p.put("categories", List.of("tasks.category_id"));
        p.put("milestones", List.of("tasks.milestone_id"));
        p.put("goals", List.of("tasks.goal_id", "milestones.goal_id"));
        p.put("notes", List.of("note_tags.note_id", "note_links.note_id"));
        p.put("tags", List.of("note_tags.tag_id"));
        p.put("user_settings", List.of());
        return p;
    }

    public record Result(long mutations, long changeLogs, long tombstones, long diagnostics) {
    }

    private final RetentionStore store;
    private final SyncHandlerRegistry handlers;
    private final SyncMetrics metrics;
    private final TransactionTemplate tx;
    private final Clock clock;

    public SyncRetentionService(RetentionStore store, SyncHandlerRegistry handlers, SyncMetrics metrics,
            TransactionTemplate tx, Clock clock) {
        this.store = store;
        this.handlers = handlers;
        this.metrics = metrics;
        this.tx = tx;
        this.clock = clock;
    }

    public Result runAll() {
        Result result = new Result(runMutations(), runChangeLogs(), runTombstones(), runDiagnostics());
        log.atInfo().addKeyValue("event", "sync_retention").addKeyValue("mutations", result.mutations())
                .addKeyValue("changeLogs", result.changeLogs()).addKeyValue("tombstones", result.tombstones())
                .addKeyValue("diagnostics", result.diagnostics()).log("sync_retention");
        return result;
    }

    public long runMutations() {
        Instant cutoff = clock.instant().minus(MUTATION_RETENTION);
        long total = 0;
        Integer deleted;
        do {
            deleted = tx.execute(s -> store.deleteMutationsBefore(cutoff, BATCH));
            total += deleted == null ? 0 : deleted;
        } while (deleted != null && deleted == BATCH);
        metrics.retentionDeleted("sync_mutations", total);
        return total;
    }

    public long runChangeLogs() {
        Instant cutoff = clock.instant().minus(CHANGE_LOG_RETENTION);
        long total = 0;
        for (UUID userId : store.usersWithOldChangeLogs(cutoff, 10_000)) {
            Long deleted;
            do {
                deleted = tx.execute(s -> {
                    Optional<Long> lastSeq = store.lockState(userId);
                    if (lastSeq.isEmpty()) {
                        return 0L;
                    }
                    long maxSeq = lastSeq.get() - MIN_KEPT_SEQ;
                    if (maxSeq <= 0) {
                        return 0L;
                    }
                    RetentionStore.Deleted gone = store.deleteChangeLogs(userId, maxSeq, cutoff, BATCH);
                    if (gone.count() > 0) {
                        store.raiseMinRetained(userId, gone.maxSeq());
                    }
                    return (long) gone.count();
                });
                total += deleted == null ? 0 : deleted;
            } while (deleted != null && deleted == BATCH);
        }
        metrics.retentionDeleted("change_logs", total);
        return total;
    }

    public long runTombstones() {
        Instant cutoff = clock.instant().minus(TOMBSTONE_RETENTION);
        Map<String, String> typeOfTable = new LinkedHashMap<>();
        for (SyncEntityHandler handler : handlers.all()) {
            if (!TOMBSTONE_PLAN.containsKey(handler.table())) {
                throw new IllegalStateException("Thiếu kế hoạch retention cho bảng " + handler.table());
            }
            typeOfTable.put(handler.table(), handler.entityType());
        }
        tx.executeWithoutResult(s -> store.deleteAlertsOfPurgeableBudgets(cutoff));
        long total = 0;
        for (Map.Entry<String, List<String>> step : TOMBSTONE_PLAN.entrySet()) {
            String type = typeOfTable.get(step.getKey());
            if (type == null) {
                continue;
            }
            long tableTotal = 0;
            Integer deleted;
            do {
                deleted = tx.execute(s -> store.deleteTombstones(step.getKey(), type, step.getValue(), cutoff, BATCH));
                tableTotal += deleted == null ? 0 : deleted;
            } while (deleted != null && deleted == BATCH);
            metrics.retentionDeleted(step.getKey(), tableTotal);
            total += tableTotal;
        }
        return total;
    }

    public long runDiagnostics() {
        Integer deleted = tx.execute(s -> store.deleteDiagnosticsBefore(clock.instant().minus(DIAGNOSTICS_RETENTION)));
        metrics.retentionDeleted("sync_diagnostics", deleted == null ? 0 : deleted);
        return deleted == null ? 0 : deleted;
    }
}
