package com.lifeos.routine.service;

import com.lifeos.common.exception.ErrorCode;
import com.lifeos.common.exception.FieldErrors;
import com.lifeos.common.response.PageResponse;
import com.lifeos.common.time.UserCalendar;
import com.lifeos.common.web.Cursors;
import com.lifeos.routine.dto.LogView;
import com.lifeos.routine.dto.RoutineDetail;
import com.lifeos.routine.dto.RoutineResponse;
import com.lifeos.routine.dto.RoutineStatsResponse;
import com.lifeos.routine.dto.RoutineSyncState;
import com.lifeos.routine.entity.Routine;
import com.lifeos.routine.entity.RoutineLog;
import com.lifeos.routine.entity.RoutineStepLog;
import com.lifeos.routine.mapper.RoutineMapper;
import com.lifeos.routine.repository.RoutineLogRepository;
import com.lifeos.routine.repository.RoutineRepository;
import com.lifeos.routine.repository.RoutineStepLogRepository;
import com.lifeos.routine.repository.RoutineStepRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.data.domain.Limit;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Đọc Routine, bước, lượt chạy và tỷ lệ (FR-RTN-04, D245). Chỉ bản ghi chưa xóa của user hiện tại. */
@Service
public class RoutineQueryService {

    static final String SORT = "createdAt:desc";
    static final int MAX_RANGE_DAYS = 366;
    private static final Set<String> ARCHIVED_MODES = Set.of("false", "true", "all");

    private final RoutineRepository routines;
    private final RoutineStepRepository steps;
    private final RoutineLogRepository logs;
    private final RoutineStepLogRepository stepLogs;
    private final UserCalendar calendar;
    private final RoutineMapper mapper;

    public RoutineQueryService(RoutineRepository routines, RoutineStepRepository steps, RoutineLogRepository logs,
            RoutineStepLogRepository stepLogs, UserCalendar calendar, RoutineMapper mapper) {
        this.routines = routines;
        this.steps = steps;
        this.logs = logs;
        this.stepLogs = stepLogs;
        this.calendar = calendar;
        this.mapper = mapper;
    }

    @Transactional(readOnly = true)
    public RoutineDetail get(UUID userId, UUID routineId) {
        Routine routine = routines.findActive(routineId, userId).orElseThrow(RoutineCommandService::notFound);
        return new RoutineDetail(mapper.toResponse(routine),
                steps.findLiveByRoutinesOrdered(userId, List.of(routineId)).stream().map(mapper::toResponse).toList());
    }

    @Transactional(readOnly = true)
    public PageResponse<RoutineResponse> list(UUID userId, String archived, Integer limit, String cursor) {
        String mode = archived == null ? "false" : archived;
        if (!ARCHIVED_MODES.contains(mode)) {
            throw FieldErrors.single(ErrorCode.VALIDATION_FAILED, "archived", "INVALID_VALUE",
                    "archived phải là false, true hoặc all.");
        }
        int size = Cursors.limit(limit);
        Cursors.Position after = Cursors.decode(cursor, SORT);
        Instant afterCreatedAt = null;
        if (after != null) {
            try {
                afterCreatedAt = Instant.parse(after.key());
            } catch (DateTimeParseException | NullPointerException e) {
                throw FieldErrors.single(ErrorCode.VALIDATION_FAILED, "cursor", "INVALID_VALUE",
                        "Con trỏ phân trang không hợp lệ.");
            }
        }
        List<Routine> rows = routines.page(userId, mode, after == null, afterCreatedAt,
                after == null ? null : after.id(), Limit.of(size + 1));
        boolean hasMore = rows.size() > size;
        List<Routine> page = hasMore ? rows.subList(0, size) : rows;
        String next = null;
        if (hasMore) {
            Routine last = page.get(page.size() - 1);
            next = Cursors.encode(new Cursors.Position(SORT, last.getCreatedAt().toString(), last.getId()));
        }
        return new PageResponse<>(page.stream().map(mapper::toResponse).toList(), next, hasMore);
    }

    /** GET /routines/{id}/logs?from=&to= (≤ 366 ngày; mặc định 30 ngày gần nhất), mới nhất trước, kèm kết quả bước. */
    @Transactional(readOnly = true)
    public List<LogView> logs(UUID userId, UUID routineId, String rawFrom, String rawTo) {
        routines.findActive(routineId, userId).orElseThrow(RoutineCommandService::notFound);
        LocalDate today = calendar.today(userId);
        LocalDate to = rawTo == null ? today : RoutineValues.date(rawTo, "to");
        LocalDate from = rawFrom == null ? to.minusDays(29) : RoutineValues.date(rawFrom, "from");
        checkRange(from, to);
        List<RoutineLog> runs = logs.range(userId, routineId, from, to);
        Map<UUID, List<RoutineStepLog>> results = runs.isEmpty() ? Map.of()
                : stepLogs.findLiveByLogs(userId, runs.stream().map(RoutineLog::getId).toList()).stream()
                        .collect(Collectors.groupingBy(RoutineStepLog::getRoutineLogId));
        return runs.stream().map(r -> new LogView(mapper.toResponse(r),
                results.getOrDefault(r.getId(), List.of()).stream().map(mapper::toResponse).toList())).toList();
    }

    /**
     * GET /routines/{id}/stats?from=&to= (D245). scheduled = số ngày đến lịch đã qua trong khoảng, tính từ ngày tạo
     * routine (theo múi giờ người dùng) [PROPOSED: routine không có startOn]; hôm nay chỉ tính khi đã hoàn tất;
     * completed = lượt có completedAt; partial = lượt chưa hoàn tất có ít nhất một bước DONE.
     */
    @Transactional(readOnly = true)
    public RoutineStatsResponse stats(UUID userId, UUID routineId, String rawFrom, String rawTo) {
        Routine routine = routines.findActive(routineId, userId).orElseThrow(RoutineCommandService::notFound);
        LocalDate from = RoutineValues.date(rawFrom, "from");
        LocalDate to = RoutineValues.date(rawTo, "to");
        checkRange(from, to);
        ZoneId zone = calendar.zone(userId);
        LocalDate today = calendar.today(userId);
        LocalDate created = LocalDate.ofInstant(routine.getCreatedAt(), zone);
        List<RoutineLog> runs = logs.range(userId, routineId, from, to);
        Set<LocalDate> completedDays = runs.stream().filter(r -> r.getCompletedAt() != null)
                .map(RoutineLog::getLocalDate).collect(Collectors.toSet());
        int scheduled = 0;
        LocalDate lo = from.isBefore(created) ? created : from;
        LocalDate hi = to.isAfter(today) ? today : to;
        for (LocalDate day = lo; !day.isAfter(hi); day = day.plusDays(1)) {
            if (routine.isScheduled(day) && (day.isBefore(today) || completedDays.contains(day))) {
                scheduled++;
            }
        }
        int completed = completedDays.size();
        Set<UUID> doneLogs = runs.isEmpty() ? Set.of()
                : stepLogs.findLiveByLogs(userId, runs.stream().map(RoutineLog::getId).toList()).stream()
                        .filter(s -> "DONE".equals(s.getStatus())).map(RoutineStepLog::getRoutineLogId)
                        .collect(Collectors.toSet());
        int partial = (int) runs.stream().filter(r -> r.getCompletedAt() == null && doneLogs.contains(r.getId()))
                .count();
        BigDecimal rate = scheduled == 0 ? null
                : BigDecimal.valueOf(Math.min(completed, scheduled))
                        .divide(BigDecimal.valueOf(scheduled), 4, RoundingMode.HALF_UP);
        return new RoutineStatsResponse(routineId, from, to, scheduled, completed, partial, rate);
    }

    private static void checkRange(LocalDate from, LocalDate to) {
        if (to.isBefore(from) || ChronoUnit.DAYS.between(from, to) + 1 > MAX_RANGE_DAYS) {
            throw FieldErrors.single(ErrorCode.VALIDATION_FAILED, "to", "INVALID_VALUE",
                    "Khoảng ngày từ 1 đến " + MAX_RANGE_DAYS + " ngày.");
        }
    }

    // ---------- Snapshot sync (kể cả tombstone)

    @Transactional(readOnly = true)
    public List<RoutineSyncState> routineStates(UUID userId, Collection<UUID> ids) {
        return ids.isEmpty() ? List.of() : routines.findAllOwned(userId, ids).stream()
                .map(r -> new RoutineSyncState(r.getId(), r.getVersion(), r.isDeleted(), mapper.toResponse(r)))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<RoutineSyncState> stepStates(UUID userId, Collection<UUID> ids) {
        return ids.isEmpty() ? List.of() : steps.findAllOwned(userId, ids).stream()
                .map(s -> new RoutineSyncState(s.getId(), s.getVersion(), s.isDeleted(), mapper.toResponse(s)))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<RoutineSyncState> logStates(UUID userId, Collection<UUID> ids) {
        return ids.isEmpty() ? List.of() : logs.findAllOwned(userId, ids).stream()
                .map(l -> new RoutineSyncState(l.getId(), l.getVersion(), l.isDeleted(), mapper.toResponse(l)))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<RoutineSyncState> stepLogStates(UUID userId, Collection<UUID> ids) {
        return ids.isEmpty() ? List.of() : stepLogs.findAllOwned(userId, ids).stream()
                .map(s -> new RoutineSyncState(s.getId(), s.getVersion(), s.isDeleted(), mapper.toResponse(s)))
                .toList();
    }
}
