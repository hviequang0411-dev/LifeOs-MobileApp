package com.lifeos.habit.service;

import com.lifeos.common.exception.ErrorCode;
import com.lifeos.common.exception.FieldErrors;
import com.lifeos.common.response.PageResponse;
import com.lifeos.common.time.UserCalendar;
import com.lifeos.common.web.Cursors;
import com.lifeos.habit.domain.HabitStatistics;
import com.lifeos.habit.api.HabitAiQueryApi;
import com.lifeos.habit.dto.CheckinResponse;
import com.lifeos.habit.dto.CheckinSyncState;
import com.lifeos.habit.dto.HabitStatsResponse;
import com.lifeos.habit.dto.HabitSyncState;
import com.lifeos.habit.dto.HabitView;
import com.lifeos.habit.entity.Habit;
import com.lifeos.habit.entity.HabitCheckin;
import com.lifeos.habit.mapper.HabitMapper;
import com.lifeos.habit.repository.HabitCheckinRepository;
import com.lifeos.habit.repository.HabitRepository;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.data.domain.Limit;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Đọc Habit/check-in, chuỗi và thống kê (FR-HAB-06..08, D233, D234). Chỉ bản ghi chưa xóa của user hiện tại. */
@Service
public class HabitQueryService implements HabitAiQueryApi {

    static final String SORT = "createdAt:desc";
    static final String HISTORY_SORT = "localDate:desc";
    /** D234 / PHASE_2 §6: khoảng thống kê tối đa 366 ngày. */
    static final int MAX_STATS_DAYS = 366;
    private static final Set<String> ARCHIVED_MODES = Set.of("false", "true", "all");

    private final HabitRepository habits;
    private final HabitCheckinRepository checkins;
    private final UserCalendar calendar;
    private final HabitMapper mapper;

    public HabitQueryService(HabitRepository habits, HabitCheckinRepository checkins, UserCalendar calendar,
            HabitMapper mapper) {
        this.habits = habits;
        this.checkins = checkins;
        this.calendar = calendar;
        this.mapper = mapper;
    }

    @Transactional(readOnly = true)
    public HabitView get(UUID userId, UUID habitId) {
        Habit habit = habits.findActive(habitId, userId).orElseThrow(HabitCommandService::notFound);
        return views(userId, List.of(habit)).get(0);
    }

    /**
     * GET /habits?archived=&scheduledOn=&limit=&cursor=. scheduledOn (yyyy-MM-dd) lọc habit đến lịch ngày đó (habit
     * TIMES_PER_WEEK tính là đến lịch mọi ngày trong vòng đời) và trả TẤT CẢ kết quả một trang (≤ 200 habit, D237)
     * [PROPOSED].
     */
    @Transactional(readOnly = true)
    public PageResponse<HabitView> list(UUID userId, String archived, String scheduledOn, Integer limit,
            String cursor) {
        String mode = archived == null ? "false" : archived;
        if (!ARCHIVED_MODES.contains(mode)) {
            throw FieldErrors.single(ErrorCode.VALIDATION_FAILED, "archived", "INVALID_VALUE",
                    "archived phải là false, true hoặc all.");
        }
        if (scheduledOn != null) {
            LocalDate day = HabitValues.date(scheduledOn, "scheduledOn");
            List<Habit> due = habits.page(userId, mode, true, null, null, Limit.of(HabitCommandService.MAX_HABITS))
                    .stream()
                    .filter(h -> h.schedule().isWeekly() ? h.schedule().inRange(day) : h.schedule().isScheduled(day))
                    .toList();
            return new PageResponse<>(views(userId, due), null, false);
        }
        int size = Cursors.limit(limit);
        Cursors.Position after = Cursors.decode(cursor, SORT);
        Instant afterCreatedAt = null;
        if (after != null) {
            try {
                afterCreatedAt = Instant.parse(after.key());
            } catch (DateTimeParseException | NullPointerException e) {
                throw badCursor();
            }
        }
        List<Habit> rows = habits.page(userId, mode, after == null, afterCreatedAt,
                after == null ? null : after.id(), Limit.of(size + 1));
        boolean hasMore = rows.size() > size;
        List<Habit> page = hasMore ? rows.subList(0, size) : rows;
        String next = null;
        if (hasMore) {
            Habit last = page.get(page.size() - 1);
            next = Cursors.encode(new Cursors.Position(SORT, last.getCreatedAt().toString(), last.getId()));
        }
        return new PageResponse<>(views(userId, page), next, hasMore);
    }

    @Override
    @Transactional(readOnly = true)
    public List<HabitAiQueryApi.Snapshot> aiHabits(UUID userId, int limit) {
        return list(userId, "false", null, Math.min(limit, 200), null).items().stream()
                .map(h -> new HabitAiQueryApi.Snapshot(h.id(), h.name(), h.scheduleType(), h.startOn(), h.endOn(),
                        h.currentStreak(), h.longestStreak(), h.version()))
                .toList();
    }

    /** GET /habits/{id}/checkins?from=&to=&limit=&cursor=: mới nhất trước. Mặc định from = startOn, to = hôm nay + 1. */
    @Transactional(readOnly = true)
    public PageResponse<CheckinResponse> history(UUID userId, UUID habitId, String rawFrom, String rawTo,
            Integer limit, String cursor) {
        Habit habit = habits.findActive(habitId, userId).orElseThrow(HabitCommandService::notFound);
        LocalDate from = rawFrom == null ? habit.getStartOn() : HabitValues.date(rawFrom, "from");
        LocalDate to = rawTo == null ? calendar.today(userId).plusDays(1) : HabitValues.date(rawTo, "to");
        int size = Cursors.limit(limit);
        Cursors.Position after = Cursors.decode(cursor, HISTORY_SORT);
        LocalDate before = null;
        if (after != null) {
            try {
                before = LocalDate.parse(after.key());
            } catch (DateTimeParseException | NullPointerException e) {
                throw badCursor();
            }
        }
        List<HabitCheckin> rows = checkins.history(userId, habitId, from, to, after == null, before,
                Limit.of(size + 1));
        boolean hasMore = rows.size() > size;
        List<HabitCheckin> page = hasMore ? rows.subList(0, size) : rows;
        String next = null;
        if (hasMore) {
            HabitCheckin last = page.get(page.size() - 1);
            next = Cursors.encode(new Cursors.Position(HISTORY_SORT, last.getLocalDate().toString(), last.getId()));
        }
        return new PageResponse<>(page.stream().map(mapper::toResponse).toList(), next, hasMore);
    }

    /** GET /habits/{id}/stats?from=&to= (D234; khoảng ≤ 366 ngày). */
    @Transactional(readOnly = true)
    public HabitStatsResponse stats(UUID userId, UUID habitId, String rawFrom, String rawTo) {
        Habit habit = habits.findActive(habitId, userId).orElseThrow(HabitCommandService::notFound);
        return statsOf(userId, List.of(habit), rawFrom, rawTo).get(0);
    }

    /** GET /habits/stats?from=&to=: mọi habit chưa lưu trữ. */
    @Transactional(readOnly = true)
    public List<HabitStatsResponse> statsAll(UUID userId, String rawFrom, String rawTo) {
        return statsOf(userId, habits.findAllActiveUnarchived(userId), rawFrom, rawTo);
    }

    private List<HabitStatsResponse> statsOf(UUID userId, List<Habit> list, String rawFrom, String rawTo) {
        LocalDate from = requiredDate(rawFrom, "from");
        LocalDate to = requiredDate(rawTo, "to");
        if (to.isBefore(from) || ChronoUnit.DAYS.between(from, to) + 1 > MAX_STATS_DAYS) {
            throw FieldErrors.single(ErrorCode.VALIDATION_FAILED, "to", "INVALID_VALUE",
                    "Khoảng thống kê từ 1 đến " + MAX_STATS_DAYS + " ngày.");
        }
        LocalDate today = calendar.today(userId);
        DayOfWeek weekStart = calendar.weekStart(userId);
        Map<UUID, Map<LocalDate, String>> statuses = statusesOf(userId, list);
        return list.stream().map(h -> {
            HabitStatistics calc = new HabitStatistics(h.schedule(), statuses.getOrDefault(h.getId(), Map.of()),
                    today, weekStart);
            HabitStatistics.Stats s = calc.stats(from, to);
            HabitStatistics.Streaks streaks = calc.streaks();
            return new HabitStatsResponse(h.getId(), from, to, s.scheduled(), s.done(), s.skipped(), s.missed(),
                    s.completionRate(), streaks.current(), streaks.longest());
        }).toList();
    }

    private List<HabitView> views(UUID userId, List<Habit> list) {
        if (list.isEmpty()) {
            return List.of();
        }
        LocalDate today = calendar.today(userId);
        DayOfWeek weekStart = calendar.weekStart(userId);
        Map<UUID, Map<LocalDate, String>> statuses = statusesOf(userId, list);
        return list.stream().map(h -> {
            HabitStatistics.Streaks streaks = new HabitStatistics(h.schedule(),
                    statuses.getOrDefault(h.getId(), Map.of()), today, weekStart).streaks();
            return HabitView.of(mapper.toResponse(h), streaks.current(), streaks.longest());
        }).toList();
    }

    private Map<UUID, Map<LocalDate, String>> statusesOf(UUID userId, List<Habit> list) {
        if (list.isEmpty()) {
            return Map.of();
        }
        Map<UUID, Map<LocalDate, String>> byHabit = new HashMap<>();
        for (HabitCheckin c : checkins.findLiveByHabits(userId, list.stream().map(Habit::getId).toList())) {
            byHabit.computeIfAbsent(c.getHabitId(), k -> new HashMap<>()).put(c.getLocalDate(), c.getStatus());
        }
        return byHabit;
    }

    @Transactional(readOnly = true)
    public List<HabitSyncState> habitSyncStates(UUID userId, Collection<UUID> ids) {
        if (ids.isEmpty()) {
            return List.of();
        }
        return habits.findAllOwned(userId, ids).stream()
                .map(h -> new HabitSyncState(mapper.toResponse(h), h.isDeleted())).toList();
    }

    @Transactional(readOnly = true)
    public List<CheckinSyncState> checkinSyncStates(UUID userId, Collection<UUID> ids) {
        if (ids.isEmpty()) {
            return List.of();
        }
        return checkins.findAllOwned(userId, ids).stream()
                .map(c -> new CheckinSyncState(mapper.toResponse(c), c.isDeleted())).toList();
    }

    private static LocalDate requiredDate(String raw, String field) {
        LocalDate date = HabitValues.date(raw, field);
        if (date == null) {
            throw HabitValues.invalid(field, "REQUIRED", "Trường này là bắt buộc.");
        }
        return date;
    }

    private static RuntimeException badCursor() {
        return FieldErrors.single(ErrorCode.VALIDATION_FAILED, "cursor", "INVALID_VALUE",
                "Con trỏ phân trang không hợp lệ.");
    }
}
