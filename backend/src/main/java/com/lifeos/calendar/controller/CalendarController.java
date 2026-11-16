package com.lifeos.calendar.controller;

import com.lifeos.calendar.dto.CalendarWindowResponse;
import com.lifeos.calendar.dto.CreateEventRequest;
import com.lifeos.calendar.dto.CreateExceptionRequest;
import com.lifeos.calendar.dto.CreateTimeBlockRequest;
import com.lifeos.calendar.dto.EventResponse;
import com.lifeos.calendar.dto.ExceptionResponse;
import com.lifeos.calendar.dto.SplitEventRequest;
import com.lifeos.calendar.dto.TimeBlockResponse;
import com.lifeos.calendar.dto.UpdateEventRequest;
import com.lifeos.calendar.dto.UpdateTimeBlockRequest;
import com.lifeos.calendar.service.CalendarCommands;
import com.lifeos.calendar.service.CalendarQueryService;
import com.lifeos.calendar.service.EventCommandService;
import com.lifeos.calendar.service.EventExceptionCommandService;
import com.lifeos.calendar.service.TimeBlockCommandService;
import com.lifeos.common.response.PageResponse;
import com.lifeos.common.security.CurrentUser;
import com.lifeos.common.util.Patch;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.validation.Valid;
import java.net.URI;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Lịch: sự kiện, ngoại lệ, time block, cửa sổ hợp nhất (PHASE_4 §6). userId luôn lấy từ CurrentUser. */
@RestController
@RequestMapping("/api/v1")
class CalendarController {

    private final CurrentUser currentUser;
    private final EventCommandService events;
    private final EventExceptionCommandService exceptions;
    private final TimeBlockCommandService blocks;
    private final CalendarQueryService queries;

    CalendarController(CurrentUser currentUser, EventCommandService events, EventExceptionCommandService exceptions,
            TimeBlockCommandService blocks, CalendarQueryService queries) {
        this.currentUser = currentUser;
        this.events = events;
        this.exceptions = exceptions;
        this.blocks = blocks;
        this.queries = queries;
    }

    @ApiResponse(responseCode = "201", description = "Đã tạo",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = EventResponse.class)))
    @PostMapping("/calendar/events")
    ResponseEntity<EventResponse> createEvent(@Valid @RequestBody CreateEventRequest r) {
        EventResponse created = events.create(currentUser.userId(), new CalendarCommands.CreateEvent(r.id(),
                new CalendarCommands.EventFields(Patch.of(r.title()), Patch.of(r.description()),
                        Patch.of(r.location()), Patch.of(r.allDay()), Patch.of(r.timezone()), Patch.of(r.startLocal()),
                        Patch.of(r.endLocal()), Patch.of(r.startDate()), Patch.of(r.endDate()),
                        Patch.of(r.recurrence()))), null);
        return ResponseEntity.created(URI.create("/api/v1/calendar/events/" + created.id())).body(created);
    }

    @GetMapping("/calendar/events")
    PageResponse<EventResponse> listEvents(@RequestParam(required = false) Integer limit,
            @RequestParam(required = false) String cursor) {
        return queries.events(currentUser.userId(), limit, cursor);
    }

    @GetMapping("/calendar/events/{id}")
    EventResponse getEvent(@PathVariable UUID id) {
        return queries.event(currentUser.userId(), id);
    }

    @PatchMapping("/calendar/events/{id}")
    EventResponse updateEvent(@PathVariable UUID id, @Valid @RequestBody UpdateEventRequest r) {
        return events.update(currentUser.userId(), id, new CalendarCommands.UpdateEvent(r.version(),
                new CalendarCommands.EventFields(r.title(), r.description(), r.location(), r.allDay(), r.timezone(),
                        r.startLocal(), r.endLocal(), r.startDate(), r.endDate(), r.recurrence())), null);
    }

    /** fromOriginalStart (giờ địa phương của một lần xuất hiện): "xóa từ lần này trở đi" thay vì xóa cả chuỗi. */
    @ApiResponse(responseCode = "204", description = "Đã xóa chuỗi (tombstone) hoặc đã cắt chuỗi")
    @DeleteMapping("/calendar/events/{id}")
    ResponseEntity<Void> deleteEvent(@PathVariable UUID id, @RequestParam long version,
            @RequestParam(required = false) String fromOriginalStart) {
        events.delete(currentUser.userId(), id, version, local(fromOriginalStart, "fromOriginalStart"), null);
        return ResponseEntity.noContent().build();
    }

    @ApiResponse(responseCode = "201", description = "Đã tạo chuỗi mới", content = @Content(
            mediaType = "application/json", schema = @Schema(implementation = EventResponse.class)))
    @PostMapping("/calendar/events/{id}/split")
    ResponseEntity<EventResponse> split(@PathVariable UUID id, @Valid @RequestBody SplitEventRequest r) {
        EventResponse created = events.split(currentUser.userId(), id, r.version(),
                local(r.originalStartLocal(), "originalStartLocal"), r.newEventId(), r.title(), r.description(),
                r.location(), r.startLocal(), r.endLocal());
        return ResponseEntity.created(URI.create("/api/v1/calendar/events/" + created.id())).body(created);
    }

    @GetMapping("/calendar/events/{id}/exceptions")
    List<ExceptionResponse> listExceptions(@PathVariable UUID id) {
        return queries.exceptionsOf(currentUser.userId(), id);
    }

    @ApiResponse(responseCode = "201", description = "Đã tạo", content = @Content(mediaType = "application/json",
            schema = @Schema(implementation = ExceptionResponse.class)))
    @PostMapping("/calendar/events/{id}/exceptions")
    ResponseEntity<ExceptionResponse> createException(@PathVariable UUID id,
            @Valid @RequestBody CreateExceptionRequest r) {
        ExceptionResponse created = exceptions.create(currentUser.userId(), id, new CalendarCommands.CreateException(
                r.id(), new CalendarCommands.ExceptionFields(Patch.absent(), Patch.of(r.originalStartLocal()),
                        Patch.of(r.kind()), Patch.of(r.title()), Patch.of(r.description()), Patch.of(r.location()),
                        Patch.of(r.startLocal()), Patch.of(r.endLocal()))), null);
        return ResponseEntity.created(URI.create("/api/v1/calendar/events/" + id + "/exceptions/" + created.id()))
                .body(created);
    }

    @ApiResponse(responseCode = "204", description = "Đã xóa ngoại lệ (tombstone)")
    @DeleteMapping("/calendar/events/{id}/exceptions/{exceptionId}")
    ResponseEntity<Void> deleteException(@PathVariable UUID id, @PathVariable UUID exceptionId,
            @RequestParam long version) {
        exceptions.delete(currentUser.userId(), id, exceptionId, version, null);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/calendar")
    CalendarWindowResponse window(@RequestParam String from, @RequestParam String to) {
        return queries.window(currentUser.userId(), from, to);
    }

    @ApiResponse(responseCode = "201", description = "Đã tạo", content = @Content(mediaType = "application/json",
            schema = @Schema(implementation = TimeBlockResponse.class)))
    @PostMapping("/time-blocks")
    ResponseEntity<TimeBlockResponse> createBlock(@Valid @RequestBody CreateTimeBlockRequest r) {
        TimeBlockResponse created = blocks.create(currentUser.userId(), new CalendarCommands.CreateTimeBlock(r.id(),
                Patch.of(r.taskId()), Patch.of(r.startAt()), Patch.of(r.endAt()), Patch.of(r.note())), null);
        return ResponseEntity.created(URI.create("/api/v1/time-blocks/" + created.id())).body(created);
    }

    @GetMapping("/time-blocks")
    List<TimeBlockResponse> listBlocks(@RequestParam(required = false) UUID taskId,
            @RequestParam(required = false) String from, @RequestParam(required = false) String to) {
        return queries.blocks(currentUser.userId(), taskId, from, to);
    }

    @GetMapping("/time-blocks/{id}")
    TimeBlockResponse getBlock(@PathVariable UUID id) {
        return queries.block(currentUser.userId(), id);
    }

    @PatchMapping("/time-blocks/{id}")
    TimeBlockResponse updateBlock(@PathVariable UUID id, @Valid @RequestBody UpdateTimeBlockRequest r) {
        return blocks.update(currentUser.userId(), id, new CalendarCommands.UpdateTimeBlock(r.version(), r.taskId(),
                r.startAt(), r.endAt(), r.note()), null);
    }

    @ApiResponse(responseCode = "204", description = "Đã xóa (tombstone)")
    @DeleteMapping("/time-blocks/{id}")
    ResponseEntity<Void> deleteBlock(@PathVariable UUID id, @RequestParam long version) {
        blocks.delete(currentUser.userId(), id, version, null);
        return ResponseEntity.noContent().build();
    }

    private static LocalDateTime local(String raw, String field) {
        if (raw == null) {
            return null;
        }
        try {
            return LocalDateTime.parse(raw);
        } catch (DateTimeParseException e) {
            throw com.lifeos.common.exception.FieldErrors.single(com.lifeos.common.exception.ErrorCode.VALIDATION_FAILED,
                    field, "PATTERN", "Giờ địa phương theo dạng yyyy-MM-ddTHH:mm[:ss].");
        }
    }
}
