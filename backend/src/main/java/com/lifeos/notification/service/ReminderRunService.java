package com.lifeos.notification.service;

import com.lifeos.calendar.api.CalendarApi;
import com.lifeos.common.sync.ChangeOperation;
import com.lifeos.common.sync.ChangeRecorder;
import com.lifeos.notification.domain.NotificationRules;
import com.lifeos.notification.entity.Reminder;
import com.lifeos.notification.repository.ReminderRepository;
import com.lifeos.task.api.TaskApi;
import io.micrometer.core.instrument.MeterRegistry;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.data.domain.Limit;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * ReminderScheduler (PHASE_4 D131): reminder đến hạn được khóa FOR UPDATE SKIP LOCKED, mỗi reminder MỘT transaction:
 * tạo thông báo qua Orchestrator (dedupe {@code reminder:{id}:{epochSecond}} — chạy lặp/song song chỉ một thông báo),
 * tính next_fire_at kế tiếp sau "bây giờ" (bù tối đa một lần bị lỡ trong 24 giờ; cũ hơn bị bỏ + metric
 * {@code lifeos.notification.reminder.skipped}), hết mốc → DONE; version +1, change_logs UPDATE (mutation_id NULL) SAU
 * mọi ghi nghiệp vụ (D86). Đồng thời lập lịch lần đầu cho reminder chưa có next_fire_at (ví dụ do migration V7).
 */
@Service
public class ReminderRunService {

    static final Duration CATCH_UP = Duration.ofHours(24);
    private static final int BATCH = 200;

    private final ReminderRepository reminders;
    private final ReminderSchedules schedules;
    private final NotificationOrchestrator orchestrator;
    private final TaskApi tasks;
    private final CalendarApi calendarApi;
    private final ChangeRecorder changeRecorder;
    private final TransactionTemplate tx;
    private final MeterRegistry meters;
    private final Clock clock;

    public ReminderRunService(ReminderRepository reminders, ReminderSchedules schedules,
            NotificationOrchestrator orchestrator, TaskApi tasks, CalendarApi calendarApi, ChangeRecorder changeRecorder,
            TransactionTemplate tx, MeterRegistry meters, Clock clock) {
        this.reminders = reminders;
        this.schedules = schedules;
        this.orchestrator = orchestrator;
        this.tasks = tasks;
        this.calendarApi = calendarApi;
        this.changeRecorder = changeRecorder;
        this.tx = tx;
        this.meters = meters;
        this.clock = clock;
    }

    /** @return số thông báo đã tạo */
    public int runOnce() {
        for (UUID id : reminders.findUnscheduledIds(Limit.of(BATCH))) {
            tx.executeWithoutResult(status -> scheduleFirst(id));
        }
        int created = 0;
        for (UUID id : reminders.findDueIds(clock.instant(), Limit.of(BATCH))) {
            if (fire(id)) {
                created++;
            }
        }
        return created;
    }

    /** Xử lý một reminder trong transaction riêng (job và test). @return true nếu đã tạo thông báo */
    public boolean fire(UUID reminderId) {
        return Boolean.TRUE.equals(tx.execute(status -> fireInTransaction(reminderId)));
    }

    private boolean fireInTransaction(UUID reminderId) {
        Reminder r = reminders.lockForJob(reminderId).orElse(null);
        Instant now = clock.instant();
        if (r == null || !"ACTIVE".equals(r.getStatus()) || r.getNextFireAt() == null
                || r.getNextFireAt().isAfter(now)) {
            return false;
        }
        Instant due = r.getNextFireAt();
        boolean created = false;
        if (due.isBefore(now.minus(CATCH_UP))) {
            meters.counter("lifeos.notification.reminder.skipped").increment();
        } else {
            Map<String, Object> data = new LinkedHashMap<>();
            data.put("type", r.getTargetType().toLowerCase());
            if (r.getTargetId() != null) {
                data.put("id", r.getTargetId().toString());
            }
            data.put("reminderId", r.getId().toString());
            String detail = switch (r.getTargetType()) {
                case "TASK" -> tasks.title(r.getUserId(), r.getTargetId()).orElse(null);
                case "EVENT" -> calendarApi.title(r.getUserId(), r.getTargetId()).orElse(null);
                default -> null;
            };
            created = orchestrator.createInCurrentTransaction(new NotificationOrchestrator.Request(r.getUserId(),
                    NotificationRules.REMINDER, "reminder.generic", Map.of(),
                    "reminder:" + r.getId() + ":" + due.getEpochSecond(), data, r.isUrgent(), detail)).isPresent();
            meters.timer("lifeos.notification.reminder.lag").record(Duration.between(due, now));
            r.setLastFiredAt(due);
        }
        Instant from = now.isAfter(due) ? now.plusSeconds(1) : due.plusSeconds(1);
        Instant next = schedules.next(r, from).orElse(null);
        r.setNextFireAt(next);
        if (next == null) {
            r.setStatus("DONE");
        }
        Reminder saved = reminders.saveAndFlush(r);
        changeRecorder.record(r.getUserId(), Reminder.ENTITY_TYPE, saved.getId(), ChangeOperation.UPDATE,
                saved.getVersion(), null);
        return created;
    }

    private void scheduleFirst(UUID reminderId) {
        Reminder r = reminders.lockForJob(reminderId).orElse(null);
        if (r == null || r.getNextFireAt() != null || !"ACTIVE".equals(r.getStatus())) {
            return;
        }
        Instant next = schedules.next(r, clock.instant()).orElse(null);
        r.setNextFireAt(next);
        if (next == null) {
            r.setStatus("DONE");
        }
        Reminder saved = reminders.saveAndFlush(r);
        changeRecorder.record(r.getUserId(), Reminder.ENTITY_TYPE, saved.getId(), ChangeOperation.UPDATE,
                saved.getVersion(), null);
    }

    List<UUID> dueIds() {
        return reminders.findDueIds(clock.instant(), Limit.of(BATCH));
    }
}
