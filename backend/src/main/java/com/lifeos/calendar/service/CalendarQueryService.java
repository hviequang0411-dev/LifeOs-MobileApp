package com.lifeos.calendar.service;

import com.lifeos.calendar.api.CalendarApi;
import com.lifeos.calendar.api.CalendarAiQueryApi;
import com.lifeos.calendar.domain.EventRecurrence;
import com.lifeos.calendar.dto.CalendarItem;
import com.lifeos.calendar.dto.CalendarSyncState;
import com.lifeos.calendar.dto.CalendarWindowResponse;
import com.lifeos.calendar.dto.EventResponse;
import com.lifeos.calendar.dto.ExceptionResponse;
import com.lifeos.calendar.dto.TimeBlockResponse;
import com.lifeos.calendar.entity.CalendarEvent;
import com.lifeos.calendar.entity.EventException;
import com.lifeos.calendar.entity.TimeBlock;
import com.lifeos.calendar.mapper.CalendarMapper;
import com.lifeos.calendar.repository.CalendarEventRepository;
import com.lifeos.calendar.repository.EventExceptionRepository;
import com.lifeos.calendar.repository.TimeBlockRepository;
import com.lifeos.common.exception.ErrorCode;
import com.lifeos.common.persistence.SyncedEntity;
import com.lifeos.common.response.PageResponse;
import com.lifeos.common.time.UserCalendar;
import com.lifeos.common.web.Cursors;
import com.lifeos.task.api.TaskApi;
import com.lifeos.task.api.TaskDue;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import org.springframework.data.domain.Limit;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Đọc sự kiện, ngoại lệ, time block và cửa sổ lịch hợp nhất (FR-CAL-04, D125, D145). Đồng thời cài đặt
 * {@link CalendarApi} cho module notification.
 */
@Service
public class CalendarQueryService implements CalendarApi, CalendarAiQueryApi {

    static final String SORT = "createdAt:desc";

    private final CalendarEventRepository events;
    private final EventExceptionRepository exceptions;
    private final TimeBlockRepository blocks;
    private final EventCommandService eventCommands;
    private final TaskApi tasks;
    private final UserCalendar calendar;
    private final CalendarMapper mapper;

    public CalendarQueryService(CalendarEventRepository events, EventExceptionRepository exceptions,
            TimeBlockRepository blocks, EventCommandService eventCommands, TaskApi tasks, UserCalendar calendar,
            CalendarMapper mapper) {
        this.events = events;
        this.exceptions = exceptions;
        this.blocks = blocks;
        this.eventCommands = eventCommands;
        this.tasks = tasks;
        this.calendar = calendar;
        this.mapper = mapper;
    }

    @Transactional(readOnly = true)
    public EventResponse event(UUID userId, UUID eventId) {
        return mapper.toResponse(events.findActive(eventId, userId)
                .orElseThrow(() -> CalendarValues.notFound(ErrorCode.CALENDAR_EVENT_NOT_FOUND)));
    }

    @Transactional(readOnly = true)
    public List<ExceptionResponse> exceptionsOf(UUID userId, UUID eventId) {
        events.findActive(eventId, userId)
                .orElseThrow(() -> CalendarValues.notFound(ErrorCode.CALENDAR_EVENT_NOT_FOUND));
        return exceptions.findLiveByEvent(userId, eventId).stream().map(mapper::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public PageResponse<EventResponse> events(UUID userId, Integer limit, String cursor) {
        int size = Cursors.limit(limit);
        Cursors.Position after = Cursors.decode(cursor, SORT);
        Instant afterAt = null;
        if (after != null) {
            try {
                afterAt = Instant.parse(after.key());
            } catch (DateTimeParseException | NullPointerException e) {
                throw CalendarValues.invalid("cursor", "INVALID_VALUE", "Con trỏ phân trang không hợp lệ.");
            }
        }
        List<CalendarEvent> rows = events.page(userId, after == null, afterAt, after == null ? null : after.id(),
                Limit.of(size + 1));
        boolean hasMore = rows.size() > size;
        List<CalendarEvent> page = hasMore ? rows.subList(0, size) : rows;
        String next = null;
        if (hasMore) {
            CalendarEvent last = page.get(page.size() - 1);
            next = Cursors.encode(new Cursors.Position(SORT, last.getCreatedAt().toString(), last.getId()));
        }
        return new PageResponse<>(page.stream().map(mapper::toResponse).toList(), next, hasMore);
    }

    @Transactional(readOnly = true)
    public TimeBlockResponse block(UUID userId, UUID blockId) {
        return mapper.toResponse(blocks.findActive(blockId, userId)
                .orElseThrow(() -> CalendarValues.notFound(ErrorCode.TIME_BLOCK_NOT_FOUND)));
    }

    /** GET /time-blocks?taskId=&from=&to= (UTC ISO-8601; mặc định 7 ngày từ hôm nay, tối đa 366 ngày). */
    @Transactional(readOnly = true)
    public List<TimeBlockResponse> blocks(UUID userId, UUID taskId, String rawFrom, String rawTo) {
        Instant from = rawFrom == null ? calendar.today(userId).atStartOfDay(calendar.zone(userId)).toInstant()
                : CalendarValues.instant(rawFrom, "from");
        Instant to = rawTo == null ? from.plus(7, ChronoUnit.DAYS) : CalendarValues.instant(rawTo, "to");
        if (!to.isAfter(from) || ChronoUnit.DAYS.between(from, to) > CalendarValues.MAX_WINDOW_DAYS) {
            throw CalendarValues.invalid("to", "OUT_OF_RANGE", "Khoảng thời gian không hợp lệ (tối đa 366 ngày).");
        }
        return blocks.between(userId, taskId, from, to, Limit.of(CalendarValues.MAX_OCCURRENCES)).stream()
                .map(mapper::toResponse).toList();
    }

    /**
     * Cửa sổ hợp nhất (D145): from/to là ngày theo múi giờ người dùng (bao gồm hai đầu, ≤ 366 ngày); Event đã mở rộng
     * lặp + ngoại lệ, Time Block, Task có hạn. Quá 1000 lần xuất hiện sự kiện → 422 LIMIT_EXCEEDED (D125).
     */
    @Transactional(readOnly = true)
    public CalendarWindowResponse window(UUID userId, String rawFrom, String rawTo) {
        LocalDate from = CalendarValues.date(rawFrom, "from");
        LocalDate to = CalendarValues.date(rawTo, "to");
        if (from == null || to == null) {
            throw CalendarValues.invalid(from == null ? "from" : "to", "REQUIRED", "from và to là bắt buộc.");
        }
        if (to.isBefore(from) || ChronoUnit.DAYS.between(from, to) + 1 > CalendarValues.MAX_WINDOW_DAYS) {
            throw CalendarValues.invalid("to", "OUT_OF_RANGE", "Khoảng ngày không hợp lệ (tối đa 366 ngày).");
        }
        ZoneId zone = calendar.zone(userId);
        Instant start = from.atStartOfDay(zone).toInstant();
        Instant end = to.plusDays(1).atStartOfDay(zone).toInstant();
        List<CalendarItem> items = new ArrayList<>();
        int budget = CalendarValues.MAX_OCCURRENCES;
        for (EventTimeline.Occurrence o : occurrences(userId, start, end, budget + 1)) {
            if (--budget < 0) {
                throw CalendarValues.limitExceeded();
            }
            CalendarEvent e = o.event();
            items.add(new CalendarItem("EVENT", e.getId(), e.getId(), o.originalStartLocal(), o.title(), o.location(),
                    e.isAllDay(), o.startAt(), o.endAt(), o.startDate(), o.endDate(), o.exception(), null, null,
                    null));
        }
        for (TimeBlock b : blocks.between(userId, null, start, end, Limit.of(CalendarValues.MAX_OCCURRENCES))) {
            items.add(new CalendarItem("TIME_BLOCK", b.getId(), null, null, b.getNote(), null, false, b.getStartAt(),
                    b.getEndAt(), null, null, false, b.getTaskId(), null, null));
        }
        for (TaskDue t : tasks.dueBetween(userId, from, to, CalendarValues.MAX_OCCURRENCES)) {
            Instant at = t.dueTime() == null ? t.dueDate().atStartOfDay(zone).toInstant()
                    : CalendarValues.toInstant(t.dueDate().atTime(t.dueTime()), zone);
            items.add(new CalendarItem("TASK", t.taskId(), null, null, t.title(), null, t.dueTime() == null, at, at,
                    null, null, false, t.taskId(), t.dueDate(),
                    t.dueTime() == null ? null : t.dueTime().format(DateTimeFormatter.ofPattern("HH:mm"))));
        }
        items.sort(Comparator.comparing(CalendarItem::startAt).thenComparing(CalendarItem::source)
                .thenComparing(i -> i.id().toString()));
        return new CalendarWindowResponse(from, to, items);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CalendarAiQueryApi.Snapshot> aiWindow(UUID userId, LocalDate from, LocalDate to, int limit) {
        return window(userId, from.toString(), to.toString()).items().stream().limit(limit)
                .map(i -> new CalendarAiQueryApi.Snapshot(i.source(), i.id(), i.title(), i.allDay(),
                        i.startAt(), i.endAt()))
                .toList();
    }

    /** Mọi lần xuất hiện (đã áp ngoại lệ) giao với [from, to), tối đa {@code limit}. */
    private List<EventTimeline.Occurrence> occurrences(UUID userId, Instant from, Instant to, int limit) {
        List<EventTimeline.Occurrence> out = new ArrayList<>();
        for (CalendarEvent e : events.singleBetween(userId, from, to)) {
            out.addAll(EventTimeline.expand(e, null, List.of(), from, to, 1));
        }
        List<CalendarEvent> recurring = events.recurringBefore(userId, from, to);
        Map<UUID, List<EventException>> byEvent = new HashMap<>();
        if (!recurring.isEmpty()) {
            for (EventException x : exceptions.findLiveByEvents(userId,
                    recurring.stream().map(CalendarEvent::getId).toList())) {
                byEvent.computeIfAbsent(x.getEventId(), k -> new ArrayList<>()).add(x);
            }
        }
        for (CalendarEvent e : recurring) {
            out.addAll(EventTimeline.expand(e, eventCommands.rule(e), byEvent.getOrDefault(e.getId(), List.of()),
                    from, to, limit));
            if (out.size() > limit) {
                break;
            }
        }
        return out;
    }

    // ---------------------------------------------------------------- CalendarApi

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public boolean lockForShare(UUID userId, UUID eventId) {
        return events.findActiveForShare(eventId, userId).isPresent();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Instant> firstStartAtOrAfter(UUID userId, UUID eventId, Instant from) {
        Optional<CalendarEvent> found = events.findActive(eventId, userId);
        if (found.isEmpty()) {
            return Optional.empty();
        }
        CalendarEvent e = found.get();
        EventRecurrence rule = eventCommands.rule(e);
        List<EventException> xs = rule == null ? List.of() : exceptions.findLiveByEvent(userId, eventId);
        // Tìm trong các cửa sổ một năm liên tiếp (tối đa 10 năm) để không mở rộng vô hạn.
        Instant windowStart = from;
        for (int i = 0; i < 10; i++) {
            Instant windowEnd = windowStart.plus(366, ChronoUnit.DAYS);
            Optional<Instant> first = EventTimeline.expand(e, rule, xs, windowStart, windowEnd, 2000).stream()
                    .map(EventTimeline.Occurrence::startAt)
                    .filter(s -> !s.isBefore(from))
                    .min(Comparator.naturalOrder());
            if (first.isPresent()) {
                return first;
            }
            if (rule == null || (e.getRecurrenceUntil() != null && !e.getRecurrenceUntil().isAfter(windowEnd))) {
                return Optional.empty();
            }
            windowStart = windowEnd;
        }
        return Optional.empty();
    }

    @Override
    public void validateRule(Map<String, Object> rule) {
        CalendarValues.recurrence(rule);
    }

    @Override
    public Optional<java.time.LocalDateTime> firstOccurrence(Map<String, Object> rule,
            java.time.LocalDateTime seriesStart, java.time.LocalDateTime from) {
        EventRecurrence parsed = CalendarValues.recurrence(rule);
        java.time.LocalDateTime start = from.isBefore(seriesStart) ? seriesStart : from;
        for (int i = 0; i < 20; i++) {
            java.time.LocalDateTime end = start.plusYears(1);
            List<java.time.LocalDateTime> found = parsed.between(seriesStart, start, end, 1);
            if (!found.isEmpty()) {
                return Optional.of(found.get(0));
            }
            if (parsed.until() != null && parsed.until().isBefore(end.toLocalDate())) {
                return Optional.empty();
            }
            start = end;
        }
        return Optional.empty();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<String> title(UUID userId, UUID eventId) {
        return events.findActive(eventId, userId).map(CalendarEvent::getTitle);
    }

    // ---------------------------------------------------------------- snapshot sync

    @Transactional(readOnly = true)
    public List<CalendarSyncState> syncStates(String entityType, UUID userId, Collection<UUID> ids) {
        if (ids.isEmpty()) {
            return List.of();
        }
        return switch (entityType) {
            case CalendarEvent.ENTITY_TYPE -> states(events.findAllOwned(userId, ids), mapper::toResponse);
            case EventException.ENTITY_TYPE -> states(exceptions.findAllOwned(userId, ids), mapper::toResponse);
            case TimeBlock.ENTITY_TYPE -> states(blocks.findAllOwned(userId, ids), mapper::toResponse);
            default -> throw new IllegalArgumentException("entityType");
        };
    }

    private static <E extends SyncedEntity> List<CalendarSyncState> states(List<E> rows, Function<E, ?> toResponse) {
        return rows.stream()
                .map(e -> new CalendarSyncState(toResponse.apply(e), e.getId(), e.getVersion(), e.isDeleted()))
                .toList();
    }
}
