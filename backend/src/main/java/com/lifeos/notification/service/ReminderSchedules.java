package com.lifeos.notification.service;

import com.lifeos.calendar.api.CalendarApi;
import com.lifeos.common.time.UserCalendar;
import com.lifeos.notification.entity.Reminder;
import com.lifeos.notification.mapper.NotificationJson;
import com.lifeos.task.api.TaskApi;
import com.lifeos.task.api.TaskSchedule;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Optional;
import org.springframework.stereotype.Component;

/**
 * Tính next_fire_at (UTC) của reminder — server luôn tự tính, không nhận từ client (D130):
 * <ul>
 *   <li>OFFSET + TASK: (dueDate + dueTime, hoặc 09:00 nếu Task cả ngày [PROPOSED]) theo múi giờ người dùng − offset;
 *       một lần.</li>
 *   <li>OFFSET + EVENT: giờ bắt đầu của lần xuất hiện kế tiếp (múi giờ của sự kiện, đã áp ngoại lệ) − offset (D134).</li>
 *   <li>LOCAL_AT: giờ địa phương theo múi giờ hiện hành của người dùng (D134): một lần (localDate), hằng ngày (không
 *       localDate) hoặc theo repeatRule bắt đầu từ localDate.</li>
 * </ul>
 * DST: giờ không tồn tại dời tới mốc hợp lệ kế tiếp, giờ lặp lấy lần đầu (ZonedDateTime.ofLocal, D134).
 */
@Component
class ReminderSchedules {

    static final LocalTime ALL_DAY_TASK_TIME = LocalTime.of(9, 0);

    private final TaskApi tasks;
    private final CalendarApi calendarApi;
    private final UserCalendar calendar;
    private final NotificationJson json;

    ReminderSchedules(TaskApi tasks, CalendarApi calendarApi, UserCalendar calendar, NotificationJson json) {
        this.tasks = tasks;
        this.calendarApi = calendarApi;
        this.calendar = calendar;
        this.json = json;
    }

    /** Mốc đầu tiên ≥ {@code from}; rỗng nếu không còn mốc nào. */
    Optional<Instant> next(Reminder r, Instant from) {
        if (!"ACTIVE".equals(r.getStatus())) {
            return Optional.empty();
        }
        if ("OFFSET".equals(r.getMode())) {
            long offsetSeconds = r.getOffsetMinutes() * 60L;
            if ("TASK".equals(r.getTargetType())) {
                Optional<TaskSchedule> schedule = tasks.schedule(r.getUserId(), r.getTargetId());
                if (schedule.isEmpty() || schedule.get().dueDate() == null) {
                    return Optional.empty();
                }
                LocalTime time = schedule.get().dueTime() != null ? schedule.get().dueTime() : ALL_DAY_TASK_TIME;
                Instant fire = local(schedule.get().dueDate().atTime(time), calendar.zone(r.getUserId()))
                        .minusSeconds(offsetSeconds);
                return fire.isBefore(from) ? Optional.empty() : Optional.of(fire);
            }
            return calendarApi.firstStartAtOrAfter(r.getUserId(), r.getTargetId(), from.plusSeconds(offsetSeconds))
                    .map(start -> start.minusSeconds(offsetSeconds));
        }
        ZoneId zone = calendar.zone(r.getUserId());
        if (r.getRepeatRule() != null) {
            LocalDateTime seriesStart = r.getLocalDate().atTime(r.getLocalTime());
            LocalDateTime fromLocal = LocalDateTime.ofInstant(from, zone);
            // Lùi một ngày để không bỏ sót mốc bị dời bởi DST; lọc lại theo thời điểm UTC.
            Optional<LocalDateTime> candidate = calendarApi.firstOccurrence(json.toMap(r.getRepeatRule()),
                    seriesStart, fromLocal.minusDays(1));
            for (int i = 0; i < 3 && candidate.isPresent(); i++) {
                Instant at = local(candidate.get(), zone);
                if (!at.isBefore(from)) {
                    return Optional.of(at);
                }
                candidate = calendarApi.firstOccurrence(json.toMap(r.getRepeatRule()), seriesStart,
                        candidate.get().plusSeconds(1));
            }
            return Optional.empty();
        }
        if (r.getLocalDate() != null) {
            Instant at = local(r.getLocalDate().atTime(r.getLocalTime()), zone);
            return at.isBefore(from) ? Optional.empty() : Optional.of(at);
        }
        LocalDate day = LocalDate.ofInstant(from, zone).minusDays(1);
        for (int i = 0; i < 4; i++) {
            Instant at = local(day.plusDays(i).atTime(r.getLocalTime()), zone);
            if (!at.isBefore(from)) {
                return Optional.of(at);
            }
        }
        return Optional.empty();
    }

    private static Instant local(LocalDateTime local, ZoneId zone) {
        return ZonedDateTime.ofLocal(local, zone, null).toInstant();
    }
}
