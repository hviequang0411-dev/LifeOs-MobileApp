package com.lifeos.calendar.service;

import com.lifeos.calendar.api.EventScheduleChanged;
import com.lifeos.calendar.domain.EventRecurrence;
import com.lifeos.calendar.dto.ExceptionResponse;
import com.lifeos.calendar.entity.CalendarEvent;
import com.lifeos.calendar.entity.EventException;
import com.lifeos.calendar.mapper.CalendarMapper;
import com.lifeos.calendar.repository.CalendarEventRepository;
import com.lifeos.calendar.repository.EventExceptionRepository;
import com.lifeos.common.exception.ConflictException;
import com.lifeos.common.exception.ErrorCode;
import com.lifeos.common.exception.NaturalKeyConflictException;
import com.lifeos.common.persistence.VersionedWrites;
import com.lifeos.common.sync.ChangeOperation;
import com.lifeos.common.sync.ChangeRecorder;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

/**
 * Ngoại lệ một lần xuất hiện ("chỉ lần này", D127). Mọi lệnh ghi khóa sự kiện gốc FOR UPDATE trước (tuần tự hóa với xóa
 * chuỗi, không mồ côi). originalStartLocal phải là một lần xuất hiện của quy tắc hiện tại (ngược lại 422
 * CALENDAR_OCCURRENCE_NOT_FOUND); trùng (sự kiện, lần xuất hiện) còn sống → CONFLICT ALREADY_EXISTS ở sync (D235 tương
 * tự) / 409 ở REST. eventId và originalStartLocal bất biến.
 */
@Service
public class EventExceptionCommandService {

    private static final Set<String> KINDS = Set.of("CANCELLED", "MODIFIED");
    private static final String PK = "pk_event_exceptions";
    private static final String NATURAL = "ux_event_exceptions_event_start";

    private final CalendarEventRepository events;
    private final EventExceptionRepository exceptions;
    private final EventCommandService eventCommands;
    private final ChangeRecorder changeRecorder;
    private final VersionedWrites versionedWrites;
    private final CalendarMapper mapper;
    private final ApplicationEventPublisher publisher;
    private final Clock clock;

    public EventExceptionCommandService(CalendarEventRepository events, EventExceptionRepository exceptions,
            EventCommandService eventCommands, ChangeRecorder changeRecorder, VersionedWrites versionedWrites,
            CalendarMapper mapper, ApplicationEventPublisher publisher, Clock clock) {
        this.events = events;
        this.exceptions = exceptions;
        this.eventCommands = eventCommands;
        this.changeRecorder = changeRecorder;
        this.versionedWrites = versionedWrites;
        this.mapper = mapper;
        this.publisher = publisher;
        this.clock = clock;
    }

    /** @param eventFromUrl sự kiện của URL (REST, 404 nếu không có); null với sync (tham chiếu → 422/REJECTED) */
    public ExceptionResponse create(UUID userId, UUID eventFromUrl, CalendarCommands.CreateException cmd,
            UUID mutationIdOrNull) {
        try {
            return versionedWrites.write(() -> {
                if (exceptions.existsById(cmd.id())) {
                    throw new ConflictException(ErrorCode.ENTITY_ALREADY_EXISTS);
                }
                CalendarCommands.ExceptionFields f = cmd.fields();
                UUID eventId = eventFromUrl != null ? eventFromUrl : f.eventId().value();
                if (eventId == null) {
                    throw CalendarValues.invalid("eventId", "REQUIRED", "eventId là bắt buộc.");
                }
                LocalDateTime original = CalendarValues.localDateTime(f.originalStartLocal().value(),
                        "originalStartLocal");
                if (original == null) {
                    throw CalendarValues.invalid("originalStartLocal", "REQUIRED", "Trường này là bắt buộc.");
                }
                Values v = values(f.kind().value(), f.title().value(), f.description().value(), f.location().value(),
                        f.startLocal().value(), f.endLocal().value());
                CalendarEvent event = events.findActiveForUpdate(eventId, userId).orElseThrow(() -> eventFromUrl != null
                        ? CalendarValues.notFound(ErrorCode.CALENDAR_EVENT_NOT_FOUND)
                        : CalendarValues.badReference(ErrorCode.CALENDAR_EVENT_NOT_FOUND));
                EventRecurrence rule = eventCommands.rule(event);
                if (rule == null || !rule.isOccurrence(EventTimeline.seriesStart(event), original)) {
                    throw CalendarValues.badReference(ErrorCode.CALENDAR_OCCURRENCE_NOT_FOUND);
                }
                exceptions.findLiveAt(eventId, original).ifPresent(existing -> {
                    throw new NaturalKeyConflictException(existing.getId());
                });
                EventException x = EventException.create(cmd.id(), userId, eventId, original);
                v.applyTo(x);
                EventException saved = exceptions.saveAndFlush(x);
                publisher.publishEvent(new EventScheduleChanged(userId, eventId));
                changeRecorder.record(userId, EventException.ENTITY_TYPE, saved.getId(), ChangeOperation.CREATE,
                        saved.getVersion(), mutationIdOrNull);
                return mapper.toResponse(saved);
            }, () -> 0L);
        } catch (DataIntegrityViolationException e) {
            if (EventCommandService.mentions(e, PK)) {
                throw new ConflictException(ErrorCode.ENTITY_ALREADY_EXISTS);
            }
            if (EventCommandService.mentions(e, NATURAL)) {
                throw new ConflictException(ErrorCode.ENTITY_ALREADY_EXISTS);
            }
            throw e;
        }
    }

    public ExceptionResponse update(UUID userId, UUID exceptionId, CalendarCommands.UpdateException cmd,
            UUID mutationIdOrNull) {
        return versionedWrites.write(() -> {
            EventException x = exceptions.findActive(exceptionId, userId)
                    .orElseThrow(() -> CalendarValues.notFound(ErrorCode.CALENDAR_OCCURRENCE_NOT_FOUND));
            events.findActiveForUpdate(x.getEventId(), userId)
                    .orElseThrow(() -> CalendarValues.notFound(ErrorCode.CALENDAR_OCCURRENCE_NOT_FOUND));
            VersionedWrites.requireVersion(x, cmd.expectedVersion());
            CalendarCommands.ExceptionFields f = cmd.fields();
            if (f.eventId().present() && !x.getEventId().equals(f.eventId().value())) {
                throw CalendarValues.invalid("eventId", "IMMUTABLE", "Không thể thay đổi trường này sau khi tạo.");
            }
            if (f.originalStartLocal().present() && !x.getOriginalStartLocal().equals(
                    CalendarValues.localDateTime(f.originalStartLocal().value(), "originalStartLocal"))) {
                throw CalendarValues.invalid("originalStartLocal", "IMMUTABLE",
                        "Không thể thay đổi trường này sau khi tạo.");
            }
            Values v = values(f.kind().present() ? f.kind().value() : x.getKind(),
                    f.title().present() ? f.title().value() : x.getTitle(),
                    f.description().present() ? f.description().value() : x.getDescription(),
                    f.location().present() ? f.location().value() : x.getLocation(),
                    f.startLocal().present() ? f.startLocal().value() : text(x.getStartLocal()),
                    f.endLocal().present() ? f.endLocal().value() : text(x.getEndLocal()));
            if (!v.applyTo(x)) {
                return mapper.toResponse(x);
            }
            EventException saved = exceptions.saveAndFlush(x);
            publisher.publishEvent(new EventScheduleChanged(userId, x.getEventId()));
            changeRecorder.record(userId, EventException.ENTITY_TYPE, saved.getId(), ChangeOperation.UPDATE,
                    saved.getVersion(), mutationIdOrNull);
            return mapper.toResponse(saved);
        }, () -> currentVersion(userId, exceptionId));
    }

    /** Xóa ngoại lệ = lần xuất hiện trở lại như quy tắc. @return version tombstone */
    public long delete(UUID userId, UUID eventFromUrlOrNull, UUID exceptionId, long expectedVersion,
            UUID mutationIdOrNull) {
        return versionedWrites.write(() -> {
            EventException x = exceptions.findActive(exceptionId, userId)
                    .filter(e -> eventFromUrlOrNull == null || e.getEventId().equals(eventFromUrlOrNull))
                    .orElseThrow(() -> CalendarValues.notFound(ErrorCode.CALENDAR_OCCURRENCE_NOT_FOUND));
            events.findActiveForUpdate(x.getEventId(), userId)
                    .orElseThrow(() -> CalendarValues.notFound(ErrorCode.CALENDAR_OCCURRENCE_NOT_FOUND));
            VersionedWrites.requireVersion(x, expectedVersion);
            x.softDelete(clock.instant());
            EventException saved = exceptions.saveAndFlush(x);
            publisher.publishEvent(new EventScheduleChanged(userId, x.getEventId()));
            changeRecorder.record(userId, EventException.ENTITY_TYPE, saved.getId(), ChangeOperation.DELETE,
                    saved.getVersion(), mutationIdOrNull);
            return saved.getVersion();
        }, () -> currentVersion(userId, exceptionId));
    }

    private record Values(String kind, String title, String description, String location, LocalDateTime startLocal,
            LocalDateTime endLocal) {
        boolean applyTo(EventException x) {
            boolean changed = x.setKind(kind);
            changed |= x.setTitle(title);
            changed |= x.setDescription(description);
            changed |= x.setLocation(location);
            changed |= x.setStartLocal(startLocal);
            changed |= x.setEndLocal(endLocal);
            return changed;
        }
    }

    /** CANCELLED không mang trường ghi đè; MODIFIED: giờ ghi đè đi theo cặp (end sau start). */
    private static Values values(String rawKind, String rawTitle, String rawDescription, String rawLocation,
            String rawStart, String rawEnd) {
        if (rawKind == null || !KINDS.contains(rawKind)) {
            throw CalendarValues.invalid("kind", rawKind == null ? "REQUIRED" : "INVALID_VALUE",
                    "kind là CANCELLED hoặc MODIFIED.");
        }
        if ("CANCELLED".equals(rawKind)) {
            return new Values(rawKind, null, null, null, null, null);
        }
        String title = rawTitle == null ? null : CalendarValues.title(rawTitle);
        String description = CalendarValues.optionalText(rawDescription, "description", CalendarValues.DESCRIPTION_MAX);
        String location = CalendarValues.optionalText(rawLocation, "location", CalendarValues.LOCATION_MAX);
        LocalDateTime start = CalendarValues.localDateTime(rawStart, "startLocal");
        LocalDateTime end = CalendarValues.localDateTime(rawEnd, "endLocal");
        if ((start == null) != (end == null) || (start != null && !end.isAfter(start))) {
            throw CalendarValues.invalid("endLocal", "INVALID_VALUE", "startLocal/endLocal đi cùng nhau, end sau start.");
        }
        return new Values(rawKind, title, description, location, start, end);
    }

    private static String text(Object value) {
        return value == null ? null : value.toString();
    }

    private long currentVersion(UUID userId, UUID exceptionId) {
        return exceptions.findVersion(exceptionId, userId)
                .orElseThrow(() -> CalendarValues.notFound(ErrorCode.CALENDAR_OCCURRENCE_NOT_FOUND));
    }
}
