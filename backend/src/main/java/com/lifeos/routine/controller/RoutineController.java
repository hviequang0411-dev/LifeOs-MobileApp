package com.lifeos.routine.controller;

import com.lifeos.common.response.PageResponse;
import com.lifeos.common.security.CurrentUser;
import com.lifeos.common.util.Patch;
import com.lifeos.common.web.VersionRequest;
import com.lifeos.routine.dto.CreateRoutineRequest;
import com.lifeos.routine.dto.CreateStepRequest;
import com.lifeos.routine.dto.LogResponse;
import com.lifeos.routine.dto.LogView;
import com.lifeos.routine.dto.RoutineDetail;
import com.lifeos.routine.dto.RoutineResponse;
import com.lifeos.routine.dto.RoutineStatsResponse;
import com.lifeos.routine.dto.StartLogRequest;
import com.lifeos.routine.dto.StepLogResponse;
import com.lifeos.routine.dto.StepResponse;
import com.lifeos.routine.dto.StepResultRequest;
import com.lifeos.routine.dto.UpdateRoutineRequest;
import com.lifeos.routine.dto.UpdateStepRequest;
import com.lifeos.routine.service.RoutineCommandService;
import com.lifeos.routine.service.RoutineCommands;
import com.lifeos.routine.service.RoutineLogCommandService;
import com.lifeos.routine.service.RoutineQueryService;
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

/** Routine, bước, lượt chạy và kết quả bước của user hiện tại (PHASE_2 §6). userId luôn lấy từ CurrentUser. */
@RestController
@RequestMapping("/api/v1")
class RoutineController {

    private final CurrentUser currentUser;
    private final RoutineCommandService commands;
    private final RoutineLogCommandService runs;
    private final RoutineQueryService queries;

    RoutineController(CurrentUser currentUser, RoutineCommandService commands, RoutineLogCommandService runs,
            RoutineQueryService queries) {
        this.currentUser = currentUser;
        this.commands = commands;
        this.runs = runs;
        this.queries = queries;
    }

    @ApiResponse(responseCode = "201", description = "Đã tạo",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = RoutineResponse.class)))
    @PostMapping("/routines")
    ResponseEntity<RoutineResponse> create(@Valid @RequestBody CreateRoutineRequest r) {
        RoutineResponse created = commands.create(currentUser.userId(), new RoutineCommands.Create(r.id(),
                Patch.of(r.name()), Patch.of(r.kind()), Patch.of(r.weekdays()), Patch.of(r.reminderTime())), null);
        return ResponseEntity.created(URI.create("/api/v1/routines/" + created.id())).body(created);
    }

    @GetMapping("/routines")
    PageResponse<RoutineResponse> list(@RequestParam(required = false) String archived,
            @RequestParam(required = false) Integer limit, @RequestParam(required = false) String cursor) {
        return queries.list(currentUser.userId(), archived, limit, cursor);
    }

    @GetMapping("/routines/{id}")
    RoutineDetail get(@PathVariable UUID id) {
        return queries.get(currentUser.userId(), id);
    }

    @PatchMapping("/routines/{id}")
    RoutineResponse update(@PathVariable UUID id, @Valid @RequestBody UpdateRoutineRequest r) {
        return commands.update(currentUser.userId(), id, new RoutineCommands.Update(r.version(), r.name(), r.kind(),
                r.weekdays(), r.reminderTime()), null);
    }

    @PostMapping("/routines/{id}/archive")
    RoutineResponse archive(@PathVariable UUID id, @Valid @RequestBody VersionRequest r) {
        return commands.archive(currentUser.userId(), id, r.version());
    }

    @PostMapping("/routines/{id}/unarchive")
    RoutineResponse unarchive(@PathVariable UUID id, @Valid @RequestBody VersionRequest r) {
        return commands.unarchive(currentUser.userId(), id, r.version());
    }

    @ApiResponse(responseCode = "204", description = "Đã xóa (tombstone, kèm bước, lượt, kết quả bước)")
    @DeleteMapping("/routines/{id}")
    ResponseEntity<Void> delete(@PathVariable UUID id, @RequestParam long version) {
        commands.delete(currentUser.userId(), id, version, null);
        return ResponseEntity.noContent().build();
    }

    @ApiResponse(responseCode = "201", description = "Đã thêm bước",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = StepResponse.class)))
    @PostMapping("/routines/{id}/steps")
    ResponseEntity<StepResponse> addStep(@PathVariable UUID id, @Valid @RequestBody CreateStepRequest r) {
        StepResponse created = commands.createStep(currentUser.userId(), new RoutineCommands.CreateStep(r.id(),
                Patch.of(id), Patch.of(r.title()), Patch.of(r.position()), Patch.of(r.durationMinutes())), true, null);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PatchMapping("/routine-steps/{id}")
    StepResponse updateStep(@PathVariable UUID id, @Valid @RequestBody UpdateStepRequest r) {
        return commands.updateStep(currentUser.userId(), id, new RoutineCommands.UpdateStep(r.version(),
                Patch.absent(), r.title(), r.position(), r.durationMinutes()), null);
    }

    @ApiResponse(responseCode = "204", description = "Đã xóa bước (kết quả bước cũ giữ nguyên)")
    @DeleteMapping("/routine-steps/{id}")
    ResponseEntity<Void> deleteStep(@PathVariable UUID id, @RequestParam long version) {
        commands.deleteStep(currentUser.userId(), id, version, null);
        return ResponseEntity.noContent().build();
    }

    @ApiResponse(responseCode = "201", description = "Đã bắt đầu lượt",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = LogResponse.class)))
    @ApiResponse(responseCode = "200", description = "Lượt ngày đó đã có (idempotent)",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = LogResponse.class)))
    @PutMapping("/routines/{id}/logs/{date}")
    ResponseEntity<LogResponse> start(@PathVariable UUID id, @PathVariable String date,
            @RequestBody StartLogRequest r) {
        RoutineLogCommandService.Started started = runs.start(currentUser.userId(), id, date, r.totalSteps(), r.id());
        return ResponseEntity.status(started.created() ? HttpStatus.CREATED : HttpStatus.OK).body(started.log());
    }

    @GetMapping("/routines/{id}/logs")
    List<LogView> logs(@PathVariable UUID id, @RequestParam(required = false) String from,
            @RequestParam(required = false) String to) {
        return queries.logs(currentUser.userId(), id, from, to);
    }

    @GetMapping("/routines/{id}/stats")
    RoutineStatsResponse stats(@PathVariable UUID id, @RequestParam String from, @RequestParam String to) {
        return queries.stats(currentUser.userId(), id, from, to);
    }

    @ApiResponse(responseCode = "201", description = "Đã ghi kết quả bước",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = StepLogResponse.class)))
    @ApiResponse(responseCode = "200", description = "Đã cập nhật hoặc không đổi (idempotent)",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = StepLogResponse.class)))
    @PutMapping("/routine-logs/{logId}/steps/{stepId}")
    ResponseEntity<StepLogResponse> recordStep(@PathVariable UUID logId, @PathVariable UUID stepId,
            @RequestBody StepResultRequest r) {
        RoutineLogCommandService.Recorded recorded = runs.record(currentUser.userId(), logId, stepId, r.status(),
                r.id());
        return ResponseEntity.status(recorded.created() ? HttpStatus.CREATED : HttpStatus.OK)
                .body(recorded.stepLog());
    }

    @ApiResponse(responseCode = "204", description = "Đã hoàn tác (idempotent)")
    @DeleteMapping("/routine-logs/{logId}/steps/{stepId}")
    ResponseEntity<Void> undoStep(@PathVariable UUID logId, @PathVariable UUID stepId) {
        runs.undo(currentUser.userId(), logId, stepId);
        return ResponseEntity.noContent().build();
    }
}
