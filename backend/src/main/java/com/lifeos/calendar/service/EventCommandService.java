package com.lifeos.calendar.service;

import com.lifeos.calendar.api.EventDeletionStarted;
import com.lifeos.calendar.api.EventScheduleChanged;
import com.lifeos.calendar.domain.EventRecurrence;
import com.lifeos.calendar.dto.EventResponse;
import com.lifeos.calendar.entity.CalendarEvent;
import com.lifeos.calendar.entity.EventException;
import com.lifeos.calendar.mapper.CalendarMapper;
import com.lifeos.calendar.mapper.JsonColumns;
import com.lifeos.calendar.repository.CalendarEventRepository;
import com.lifeos.calendar.repository.EventExceptionRepository;
import com.lifeos.common.exception.ConflictException;
import com.lifeos.common.exception.ErrorCode;
import com.lifeos.common.persistence.VersionedWrites;
import com.lifeos.common.sync.ChangeOperation;
import com.lifeos.common.sync.ChangeRecorder;
import com.lifeos.common.util.Patch;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

/**
 * Lệnh ghi sự kiện lịch (FR-CAL-01/02, D124–D127). startAt/endAt/recurrenceUntil luôn do server tính (D130 tinh thần:
 * client gửi bị bỏ qua). Xóa chuỗi cascade ngoại lệ (mutation_id NULL) và phát {@link EventDeletionStarted} để module
 * notification xóa reminder trong cùng transaction — trước mọi change_logs (D86).
 */
@Service
public class EventCommandService {

    private static final String PK = "pk_calendar_events";

    private final CalendarEventRepository events;
    private final EventExceptionRepository exceptions;
    private final ChangeRecorder changeRecorder;
    private final VersionedWrites versionedWrites;
    private final CalendarMapper mapper;
    private final JsonColumns json;
    private final ApplicationEventPublisher publisher;
    private final Clock clock;

    public EventCommandService(CalendarEventRepository events, EventExceptionRepository exceptions,
            ChangeRecorder changeRecorder, VersionedWrites versionedWrites, CalendarMapper mapper, JsonColumns json,
            ApplicationEventPublisher publisher, Clock clock) {
        this.events = events;
        this.exceptions = exceptions;
        this.changeRecorder = changeRecorder;
        this.versionedWrites = versionedWrites;
        this.mapper = mapper;
        this.json = json;
        this.publisher = publisher;
        this.clock = clock;
    }

    /** Giá trị đã kiểm của một sự kiện (một "bộ" theo hình dạng có giờ / cả ngày). */
    record Shape(String title, String description, String location, boolean allDay, ZoneId zone,
            LocalDateTime startLocal, LocalDateTime endLocal, LocalDate startDate, LocalDate endDate,
            EventRecurrence rule) {
    }

    public EventResponse create(UUID userId, CalendarCommands.CreateEvent cmd, UUID mutationIdOrNull) {
        try {
            return versionedWrites.write(() -> {
                if (events.existsById(cmd.id())) {
                    throw new ConflictException(ErrorCode.ENTITY_ALREADY_EXISTS);
                }
                CalendarCommands.EventFields f = cmd.fields();
                Shape shape = shape(f.title().value(), f.description().value(), f.location().value(),
                        Boolean.TRUE.equals(f.allDay().value()), f.timezone().value(), f.startLocal().value(),
                        f.endLocal().value(), f.startDate().value(), f.endDate().value(), f.recurrence().value());
                if (events.countLive(userId) >= CalendarValues.MAX_EVENTS) {
                    throw CalendarValues.limitExceeded();
                }
                CalendarEvent event = new CalendarEvent(cmd.id(), userId);
                apply(event, shape);
                CalendarEvent saved = events.saveAndFlush(event);
                changeRecorder.record(userId, CalendarEvent.ENTITY_TYPE, saved.getId(), ChangeOperation.CREATE,
                        saved.getVersion(), mutationIdOrNull);
                return mapper.toResponse(saved);
            }, () -> 0L);
        } catch (DataIntegrityViolationException e) {
            if (mentions(e, PK)) {
                throw new ConflictException(ErrorCode.ENTITY_ALREADY_EXISTS);
            }
            throw e;
        }
    }

    /**
     * Sửa cả chuỗi ("tất cả", JSON Merge Patch). allDay có mặt ⇒ trường của hình dạng kia tự bỏ (null) [PROPOSED].
     * Ngoại lệ hiện có được giữ; ngoại lệ không còn khớp quy tắc mới bị bỏ qua khi mở rộng.
     */
    public EventResponse update(UUID userId, UUID eventId, CalendarCommands.UpdateEvent cmd, UUID mutationIdOrNull) {
        return versionedWrites.write(() -> {
            CalendarEvent event = events.findActiveForUpdate(eventId, userId)
                    .orElseThrow(() -> CalendarValues.notFound(ErrorCode.CALENDAR_EVENT_NOT_FOUND));
            VersionedWrites.requireVersion(event, cmd.expectedVersion());
            CalendarCommands.EventFields f = cmd.fields();
            boolean allDay = f.allDay().present() ? Boolean.TRUE.equals(f.allDay().value()) : event.isAllDay();
            boolean shapeSwitched = allDay != event.isAllDay();
            Shape shape = shape(
                    pick(f.title(), event.getTitle()),
                    pick(f.description(), event.getDescription()),
                    pick(f.location(), event.getLocation()),
                    allDay,
                    pick(f.timezone(), event.getTimezone()),
                    shapeSwitched && !f.startLocal().present() ? null : pick(f.startLocal(), text(event.getStartLocal())),
                    shapeSwitched && !f.endLocal().present() ? null : pick(f.endLocal(), text(event.getEndLocal())),
                    shapeSwitched && !f.startDate().present() ? null : pick(f.startDate(), text(event.getStartDate())),
                    shapeSwitched && !f.endDate().present() ? null : pick(f.endDate(), text(event.getEndDate())),
                    f.recurrence().present() ? f.recurrence().value() : json.toMap(event.getRecurrence()));
            Instant oldStart = event.getStartAt();
            String oldRule = event.getRecurrence();
            String oldZone = event.getTimezone();
            if (!apply(event, shape)) {
                return mapper.toResponse(event);
            }
            CalendarEvent saved = events.saveAndFlush(event);
            if (!Objects.equals(oldStart, saved.getStartAt()) || !Objects.equals(oldRule, saved.getRecurrence())
                    || !oldZone.equals(saved.getTimezone())) {
                publisher.publishEvent(new EventScheduleChanged(userId, eventId));
            }
            changeRecorder.record(userId, CalendarEvent.ENTITY_TYPE, saved.getId(), ChangeOperation.UPDATE,
                    saved.getVersion(), mutationIdOrNull);
            return mapper.toResponse(saved);
        }, () -> currentVersion(userId, eventId));
    }

    /**
     * Xóa. {@code fromOriginalStart} null hoặc là lần đầu ⇒ xóa cả chuỗi (cascade ngoại lệ, reminder). Ngược lại "xóa
     * từ lần này trở đi" (UC-CAL-03): cắt chuỗi trước lần đó, không tạo chuỗi mới; ngoại lệ từ lần đó bị xóa mềm.
     *
     * @return version mới của sự kiện (tombstone hoặc chuỗi đã cắt)
     */
    public long delete(UUID userId, UUID eventId, long expectedVersion, LocalDateTime fromOriginalStart,
            UUID mutationIdOrNull) {
        return versionedWrites.write(() -> {
            CalendarEvent event = events.findActiveForUpdate(eventId, userId)
                    .orElseThrow(() -> CalendarValues.notFound(ErrorCode.CALENDAR_EVENT_NOT_FOUND));
            VersionedWrites.requireVersion(event, expectedVersion);
            if (fromOriginalStart != null && !fromOriginalStart.equals(EventTimeline.seriesStart(event))) {
                return truncate(userId, event, fromOriginalStart, mutationIdOrNull).getVersion();
            }
            Instant now = clock.instant();
            event.softDelete(now);
            CalendarEvent saved = events.saveAndFlush(event);
            List<EventException> children = exceptions.findLiveByEvent(userId, eventId);
            children.forEach(x -> x.softDelete(now));
            List<EventException> deleted = exceptions.saveAllAndFlush(children);
            publisher.publishEvent(new EventDeletionStarted(userId, eventId));
            for (EventException x : deleted) {
                changeRecorder.record(userId, EventException.ENTITY_TYPE, x.getId(), ChangeOperation.DELETE,
                        x.getVersion(), null);
            }
            changeRecorder.record(userId, CalendarEvent.ENTITY_TYPE, saved.getId(), ChangeOperation.DELETE,
                    saved.getVersion(), mutationIdOrNull);
            return saved.getVersion();
        }, () -> currentVersion(userId, eventId));
    }

    /**
     * "Từ lần này" (FR-CAL-02): cắt chuỗi gốc trước {@code original} và tạo chuỗi mới {@code newEventId} bắt đầu từ lần
     * đó, cùng quy tắc (count còn lại nếu chuỗi gốc dùng count). Trường ghi đè tùy chọn: title, description, location,
     * startLocal/endLocal (giờ của lần đầu chuỗi mới).
     */
    public EventResponse split(UUID userId, UUID eventId, long expectedVersion, LocalDateTime original,
            UUID newEventId, String title, String description, String location, String startLocal,
            String endLocal) {
        try {
            return versionedWrites.write(() -> {
                CalendarEvent event = events.findActiveForUpdate(eventId, userId)
                        .orElseThrow(() -> CalendarValues.notFound(ErrorCode.CALENDAR_EVENT_NOT_FOUND));
                VersionedWrites.requireVersion(event, expectedVersion);
                EventRecurrence rule = rule(event);
                if (rule == null || original.equals(EventTimeline.seriesStart(event))
                        || !rule.isOccurrence(EventTimeline.seriesStart(event), original)) {
                    throw CalendarValues.badReference(ErrorCode.CALENDAR_OCCURRENCE_NOT_FOUND);
                }
                if (events.existsById(newEventId)) {
                    throw new ConflictException(ErrorCode.ENTITY_ALREADY_EXISTS);
                }
                int before = rule.countBefore(EventTimeline.seriesStart(event), original);
                Duration duration = EventTimeline.localDuration(event);
                EventRecurrence newRule = rule.count() != null ? rule.withCount(rule.count() - before) : rule;
                LocalDateTime newStart = startLocal != null ? CalendarValues.localDateTime(startLocal, "startLocal")
                        : original;
                LocalDateTime newEnd = endLocal != null ? CalendarValues.localDateTime(endLocal, "endLocal")
                        : newStart.plus(duration);
                Shape shape = shape(title != null ? title : event.getTitle(),
                        description != null ? description : event.getDescription(),
                        location != null ? location : event.getLocation(), event.isAllDay(), event.getTimezone(),
                        event.isAllDay() ? null : text(newStart), event.isAllDay() ? null : text(newEnd),
                        event.isAllDay() ? text(original.toLocalDate()) : null,
                        event.isAllDay() ? text(original.toLocalDate().plusDays(duration.toDays() - 1)) : null,
                        newRule.toMap());
                if (events.countLive(userId) >= CalendarValues.MAX_EVENTS) {
                    throw CalendarValues.limitExceeded();
                }
                CalendarEvent created = new CalendarEvent(newEventId, userId);
                apply(created, shape);
                CalendarEvent newSaved = events.saveAndFlush(created);
                truncate(userId, event, original, null);
                changeRecorder.record(userId, CalendarEvent.ENTITY_TYPE, newSaved.getId(), ChangeOperation.CREATE,
                        newSaved.getVersion(), null);
                return mapper.toResponse(newSaved);
            }, () -> currentVersion(userId, eventId));
        } catch (DataIntegrityViolationException e) {
            if (mentions(e, PK)) {
                throw new ConflictException(ErrorCode.ENTITY_ALREADY_EXISTS);
            }
            throw e;
        }
    }

    /** Cắt chuỗi trước original (đã khóa FOR UPDATE, đã kiểm version); ghi change_logs của ngoại lệ và chuỗi. */
    private CalendarEvent truncate(UUID userId, CalendarEvent event, LocalDateTime original, UUID mutationIdOrNull) {
        EventRecurrence rule = rule(event);
        LocalDateTime seriesStart = EventTimeline.seriesStart(event);
        if (rule == null || !rule.isOccurrence(seriesStart, original)) {
            throw CalendarValues.badReference(ErrorCode.CALENDAR_OCCURRENCE_NOT_FOUND);
        }
        EventRecurrence cut = rule.count() != null ? rule.withCount(rule.countBefore(seriesStart, original))
                : rule.withUntil(original.toLocalDate().minusDays(1));
        Instant now = clock.instant();
        List<EventException> later = exceptions.findLiveByEvent(userId, event.getId()).stream()
                .filter(x -> !x.getOriginalStartLocal().isBefore(original)).toList();
        later.forEach(x -> x.softDelete(now));
        List<EventException> deleted = exceptions.saveAllAndFlush(later);
        event.setRecurrence(json.toJson(cut.toMap()));
        event.setRecurrenceUntil(recurrenceUntil(event, cut));
        CalendarEvent saved = events.saveAndFlush(event);
        publisher.publishEvent(new EventScheduleChanged(userId, event.getId()));
        for (EventException x : deleted) {
            changeRecorder.record(userId, EventException.ENTITY_TYPE, x.getId(), ChangeOperation.DELETE,
                    x.getVersion(), null);
        }
        changeRecorder.record(userId, CalendarEvent.ENTITY_TYPE, saved.getId(), ChangeOperation.UPDATE,
                saved.getVersion(), mutationIdOrNull);
        return saved;
    }

    Shape shape(String rawTitle, String rawDescription, String rawLocation, boolean allDay, String rawZone,
            String rawStartLocal, String rawEndLocal, String rawStartDate, String rawEndDate,
            Map<String, Object> rawRecurrence) {
        String title = CalendarValues.title(rawTitle);
        String description = CalendarValues.optionalText(rawDescription, "description",
                CalendarValues.DESCRIPTION_MAX);
        String location = CalendarValues.optionalText(rawLocation, "location", CalendarValues.LOCATION_MAX);
        ZoneId zone = CalendarValues.zone(rawZone);
        LocalDateTime startLocal = CalendarValues.localDateTime(rawStartLocal, "startLocal");
        LocalDateTime endLocal = CalendarValues.localDateTime(rawEndLocal, "endLocal");
        LocalDate startDate = CalendarValues.date(rawStartDate, "startDate");
        LocalDate endDate = CalendarValues.date(rawEndDate, "endDate");
        if (allDay) {
            if (startDate == null || endDate == null) {
                throw CalendarValues.invalid(startDate == null ? "startDate" : "endDate", "REQUIRED",
                        "Sự kiện cả ngày cần startDate và endDate.");
            }
            if (endDate.isBefore(startDate)) {
                throw CalendarValues.invalid("endDate", "INVALID_VALUE", "endDate không được trước startDate.");
            }
            if (startLocal != null || endLocal != null) {
                throw CalendarValues.invalid(startLocal != null ? "startLocal" : "endLocal", "INVALID_VALUE",
                        "Sự kiện cả ngày không có giờ.");
            }
        } else {
            if (startLocal == null || endLocal == null) {
                throw CalendarValues.invalid(startLocal == null ? "startLocal" : "endLocal", "REQUIRED",
                        "Sự kiện có giờ cần startLocal và endLocal.");
            }
            if (!endLocal.isAfter(startLocal)) {
                throw CalendarValues.invalid("endLocal", "INVALID_VALUE", "endLocal phải sau startLocal.");
            }
            if (startDate != null || endDate != null) {
                throw CalendarValues.invalid(startDate != null ? "startDate" : "endDate", "INVALID_VALUE",
                        "Sự kiện có giờ không dùng startDate/endDate.");
            }
        }
        return new Shape(title, description, location, allDay, zone, startLocal, endLocal, startDate, endDate,
                CalendarValues.recurrence(rawRecurrence));
    }

    /** @return true nếu có giá trị đổi */
    private boolean apply(CalendarEvent event, Shape s) {
        boolean changed = event.setTitle(s.title());
        changed |= event.setDescription(s.description());
        changed |= event.setLocation(s.location());
        changed |= event.setAllDay(s.allDay());
        changed |= event.setTimezone(s.zone().getId());
        changed |= event.setStartLocal(s.startLocal());
        changed |= event.setEndLocal(s.endLocal());
        changed |= event.setStartDate(s.startDate());
        changed |= event.setEndDate(s.endDate());
        String recurrence = s.rule() == null ? null : json.toJson(s.rule().toMap());
        changed |= !Objects.equals(json.toMap(event.getRecurrence()), json.toMap(recurrence))
                && event.setRecurrence(recurrence);
        if (s.allDay()) {
            changed |= event.setStartAt(CalendarValues.toInstant(s.startDate().atStartOfDay(), s.zone()));
            changed |= event.setEndAt(CalendarValues.toInstant(s.endDate().plusDays(1).atStartOfDay(), s.zone()));
        } else {
            changed |= event.setStartAt(CalendarValues.toInstant(s.startLocal(), s.zone()));
            changed |= event.setEndAt(CalendarValues.toInstant(s.endLocal(), s.zone()));
        }
        changed |= event.setRecurrenceUntil(recurrenceUntil(event, s.rule()));
        return changed;
    }

    /** Thời điểm kết thúc lần xuất hiện cuối (chuỗi hữu hạn); null nếu vô hạn hoặc không lặp. */
    private static Instant recurrenceUntil(CalendarEvent event, EventRecurrence rule) {
        if (rule == null) {
            return null;
        }
        LocalDateTime last = rule.last(EventTimeline.seriesStart(event));
        if (last == null) {
            return rule.until() == null && rule.count() == null ? null : event.getStartAt();
        }
        return CalendarValues.toInstant(last.plus(EventTimeline.localDuration(event)), ZoneId.of(event.getTimezone()));
    }

    EventRecurrence rule(CalendarEvent event) {
        return event.getRecurrence() == null ? null : EventRecurrence.parse(json.toMap(event.getRecurrence()));
    }

    private static <T> T pick(Patch<T> patch, T current) {
        return patch.present() ? patch.value() : current;
    }

    private static String text(Object value) {
        return value == null ? null : value.toString();
    }

    private long currentVersion(UUID userId, UUID eventId) {
        return events.findVersion(eventId, userId)
                .orElseThrow(() -> CalendarValues.notFound(ErrorCode.CALENDAR_EVENT_NOT_FOUND));
    }

    static boolean mentions(Throwable e, String constraint) {
        for (Throwable t = e; t != null; t = t.getCause()) {
            if (t.getMessage() != null && t.getMessage().contains(constraint)) {
                return true;
            }
        }
        return false;
    }
}
