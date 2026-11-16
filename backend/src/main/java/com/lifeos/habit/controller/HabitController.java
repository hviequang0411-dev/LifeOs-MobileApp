package com.lifeos.habit.controller;

import com.lifeos.common.response.PageResponse;
import com.lifeos.common.security.CurrentUser;
import com.lifeos.common.util.Patch;
import com.lifeos.common.web.VersionRequest;
import com.lifeos.habit.dto.CheckinRequest;
import com.lifeos.habit.dto.CheckinResponse;
import com.lifeos.habit.dto.CreateHabitRequest;
import com.lifeos.habit.dto.HabitResponse;
import com.lifeos.habit.dto.HabitStatsResponse;
import com.lifeos.habit.dto.HabitView;
import com.lifeos.habit.dto.UpdateHabitRequest;
import com.lifeos.habit.service.CheckinCommandService;
import com.lifeos.habit.service.HabitCommandService;
import com.lifeos.habit.service.HabitCommands;
import com.lifeos.habit.service.HabitQueryService;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Habit và check-in của user hiện tại (PHASE_2 §6). userId luôn lấy từ CurrentUser. */
@RestController
@RequestMapping("/api/v1/habits")
class HabitController {

    private final CurrentUser currentUser;
    private final HabitCommandService commands;
    private final CheckinCommandService checkins;
    private final HabitQueryService queries;

    HabitController(CurrentUser currentUser, HabitCommandService commands, CheckinCommandService checkins,
            HabitQueryService queries) {
        this.currentUser = currentUser;
        this.commands = commands;
        this.checkins = checkins;
        this.queries = queries;
    }

    @ApiResponse(responseCode = "201", description = "Đã tạo",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = HabitResponse.class)))
    @PostMapping
    ResponseEntity<HabitResponse> create(@Valid @RequestBody CreateHabitRequest r) {
        HabitResponse created = commands.create(currentUser.userId(), new HabitCommands.Create(r.id(),
                Patch.of(r.name()), Patch.of(r.description()), Patch.of(r.color()), Patch.of(r.icon()),
                Patch.of(r.scheduleType()), Patch.of(r.weekdays()), Patch.of(r.timesPerWeek()),
                Patch.of(r.intervalDays()), Patch.of(r.startOn()), Patch.of(r.endOn()), Patch.of(r.reminderTime())),
                null);
        return ResponseEntity.created(URI.create("/api/v1/habits/" + created.id())).body(created);
    }

    @GetMapping
    PageResponse<HabitView> list(@RequestParam(required = false) String archived,
            @RequestParam(required = false) String scheduledOn, @RequestParam(required = false) Integer limit,
            @RequestParam(required = false) String cursor) {
        return queries.list(currentUser.userId(), archived, scheduledOn, limit, cursor);
    }

    @GetMapping("/stats")
    List<HabitStatsResponse> statsAll(@RequestParam String from, @RequestParam String to) {
        return queries.statsAll(currentUser.userId(), from, to);
    }

    @GetMapping("/{id}")
    HabitView get(@PathVariable UUID id) {
        return queries.get(currentUser.userId(), id);
    }

    @PatchMapping("/{id}")
    HabitResponse update(@PathVariable UUID id, @Valid @RequestBody UpdateHabitRequest r) {
        return commands.update(currentUser.userId(), id, new HabitCommands.Update(r.version(), r.name(),
                r.description(), r.color(), r.icon(), r.scheduleType(), r.weekdays(), r.timesPerWeek(),
                r.intervalDays(), r.startOn(), r.endOn(), r.reminderTime()), null);
    }

    @PostMapping("/{id}/archive")
    HabitResponse archive(@PathVariable UUID id, @Valid @RequestBody VersionRequest r) {
        return commands.archive(currentUser.userId(), id, r.version());
    }

    @PostMapping("/{id}/unarchive")
    HabitResponse unarchive(@PathVariable UUID id, @Valid @RequestBody VersionRequest r) {
        return commands.unarchive(currentUser.userId(), id, r.version());
    }

    @ApiResponse(responseCode = "204", description = "Đã xóa (tombstone, kèm check-in)")
    @DeleteMapping("/{id}")
    ResponseEntity<Void> delete(@PathVariable UUID id, @RequestParam long version) {
        commands.delete(currentUser.userId(), id, version, null);
        return ResponseEntity.noContent().build();
    }

    @ApiResponse(responseCode = "201", description = "Đã tạo check-in",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = CheckinResponse.class)))
    @ApiResponse(responseCode = "200", description = "Đã cập nhật hoặc không đổi (idempotent)",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = CheckinResponse.class)))
    @PutMapping("/{id}/checkins/{date}")
    ResponseEntity<CheckinResponse> checkin(@PathVariable UUID id, @PathVariable String date,
            @RequestBody CheckinRequest r) {
        CheckinCommandService.Upserted result = checkins.upsert(currentUser.userId(), id, date, r.status(), r.note(),
                r.id());
        return ResponseEntity.status(result.created() ? HttpStatus.CREATED : HttpStatus.OK).body(result.checkin());
    }

    @ApiResponse(responseCode = "204", description = "Đã hoàn tác (idempotent)")
    @DeleteMapping("/{id}/checkins/{date}")
    ResponseEntity<Void> undo(@PathVariable UUID id, @PathVariable String date) {
        checkins.undo(currentUser.userId(), id, date);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/checkins")
    PageResponse<CheckinResponse> history(@PathVariable UUID id, @RequestParam(required = false) String from,
            @RequestParam(required = false) String to, @RequestParam(required = false) Integer limit,
            @RequestParam(required = false) String cursor) {
        return queries.history(currentUser.userId(), id, from, to, limit, cursor);
    }

    @GetMapping("/{id}/stats")
    HabitStatsResponse stats(@PathVariable UUID id, @RequestParam String from, @RequestParam String to) {
        return queries.stats(currentUser.userId(), id, from, to);
    }
}
