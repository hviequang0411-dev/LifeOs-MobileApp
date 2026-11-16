package com.lifeos.goal.controller;

import com.lifeos.common.response.PageResponse;
import com.lifeos.common.security.CurrentUser;
import com.lifeos.common.util.Patch;
import com.lifeos.common.web.VersionRequest;
import com.lifeos.goal.dto.CreateGoalRequest;
import com.lifeos.goal.dto.CreateMilestoneRequest;
import com.lifeos.goal.dto.GoalDetail;
import com.lifeos.goal.dto.GoalResponse;
import com.lifeos.goal.dto.GoalStatusRequest;
import com.lifeos.goal.dto.MilestoneResponse;
import com.lifeos.goal.dto.UpdateGoalRequest;
import com.lifeos.goal.dto.UpdateMilestoneRequest;
import com.lifeos.goal.service.GoalCommandService;
import com.lifeos.goal.service.GoalCommands;
import com.lifeos.goal.service.GoalQueryService;
import com.lifeos.goal.service.MilestoneCommandService;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.UUID;
import org.springframework.http.HttpStatus;
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

/** Goal và Milestone của user hiện tại (PHASE_6 §6). userId luôn lấy từ CurrentUser. */
@RestController
@RequestMapping("/api/v1")
class GoalController {

    private final CurrentUser currentUser;
    private final GoalCommandService commands;
    private final MilestoneCommandService milestones;
    private final GoalQueryService queries;

    GoalController(CurrentUser currentUser, GoalCommandService commands, MilestoneCommandService milestones,
            GoalQueryService queries) {
        this.currentUser = currentUser;
        this.commands = commands;
        this.milestones = milestones;
        this.queries = queries;
    }

    @ApiResponse(responseCode = "201", description = "Đã tạo",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = GoalResponse.class)))
    @PostMapping("/goals")
    ResponseEntity<GoalResponse> create(@Valid @RequestBody CreateGoalRequest r) {
        GoalResponse created = commands.create(currentUser.userId(), new GoalCommands.Create(r.id(),
                Patch.of(r.title()), Patch.of(r.description()), Patch.of(r.startOn()), Patch.of(r.targetOn()),
                Patch.absent()), null);
        return ResponseEntity.created(URI.create("/api/v1/goals/" + created.id())).body(created);
    }

    @GetMapping("/goals")
    PageResponse<GoalDetail> list(@RequestParam(required = false) String status,
            @RequestParam(required = false) Integer limit, @RequestParam(required = false) String cursor) {
        return queries.list(currentUser.userId(), status, limit, cursor);
    }

    @GetMapping("/goals/{id}")
    GoalDetail get(@PathVariable UUID id) {
        return queries.get(currentUser.userId(), id);
    }

    @PatchMapping("/goals/{id}")
    GoalResponse update(@PathVariable UUID id, @Valid @RequestBody UpdateGoalRequest r) {
        return commands.update(currentUser.userId(), id, new GoalCommands.Update(r.version(), r.title(),
                r.description(), r.startOn(), r.targetOn(), Patch.absent()), null);
    }

    @PostMapping("/goals/{id}/status")
    GoalResponse status(@PathVariable UUID id, @Valid @RequestBody GoalStatusRequest r) {
        return commands.changeStatus(currentUser.userId(), id, r.version(), r.status());
    }

    @ApiResponse(responseCode = "204", description = "Đã xóa (milestone xóa theo; Task chỉ gỡ liên kết)")
    @DeleteMapping("/goals/{id}")
    ResponseEntity<Void> delete(@PathVariable UUID id, @RequestParam long version) {
        commands.delete(currentUser.userId(), id, version, null);
        return ResponseEntity.noContent().build();
    }

    @ApiResponse(responseCode = "201", description = "Đã thêm cột mốc",
            content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = MilestoneResponse.class)))
    @PostMapping("/goals/{id}/milestones")
    ResponseEntity<MilestoneResponse> addMilestone(@PathVariable UUID id,
            @Valid @RequestBody CreateMilestoneRequest r) {
        MilestoneResponse created = milestones.create(currentUser.userId(), new GoalCommands.CreateMilestone(r.id(),
                Patch.of(id), Patch.of(r.title()), Patch.of(r.dueOn()), Patch.of(r.position()), Patch.absent()),
                true, null);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PatchMapping("/milestones/{id}")
    MilestoneResponse updateMilestone(@PathVariable UUID id, @Valid @RequestBody UpdateMilestoneRequest r) {
        return milestones.update(currentUser.userId(), id, new GoalCommands.UpdateMilestone(r.version(),
                Patch.absent(), r.title(), r.dueOn(), r.position(), Patch.absent()), null);
    }

    @PostMapping("/milestones/{id}/complete")
    MilestoneResponse complete(@PathVariable UUID id, @Valid @RequestBody VersionRequest r) {
        return milestones.complete(currentUser.userId(), id, r.version());
    }

    @PostMapping("/milestones/{id}/reopen")
    MilestoneResponse reopen(@PathVariable UUID id, @Valid @RequestBody VersionRequest r) {
        return milestones.reopen(currentUser.userId(), id, r.version());
    }

    @ApiResponse(responseCode = "204", description = "Đã xóa cột mốc (Task giữ goal, gỡ milestone)")
    @DeleteMapping("/milestones/{id}")
    ResponseEntity<Void> deleteMilestone(@PathVariable UUID id, @RequestParam long version) {
        milestones.delete(currentUser.userId(), id, version, null);
        return ResponseEntity.noContent().build();
    }
}
