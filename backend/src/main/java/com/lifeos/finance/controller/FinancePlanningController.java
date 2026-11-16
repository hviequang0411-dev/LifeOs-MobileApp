package com.lifeos.finance.controller;

import com.lifeos.common.response.PageResponse;
import com.lifeos.common.security.CurrentUser;
import com.lifeos.common.util.Patch;
import com.lifeos.common.web.VersionRequest;
import com.lifeos.finance.dto.BudgetResponse;
import com.lifeos.finance.dto.BudgetUsageResponse;
import com.lifeos.finance.dto.ContributionResponse;
import com.lifeos.finance.dto.CreateBudgetRequest;
import com.lifeos.finance.dto.CreateContributionRequest;
import com.lifeos.finance.dto.CreateGoalRequest;
import com.lifeos.finance.dto.CreateRecurringRequest;
import com.lifeos.finance.dto.GoalResponse;
import com.lifeos.finance.dto.GoalView;
import com.lifeos.finance.dto.RecurringResponse;
import com.lifeos.finance.dto.UpdateBudgetRequest;
import com.lifeos.finance.dto.UpdateGoalRequest;
import com.lifeos.finance.dto.UpdateRecurringRequest;
import com.lifeos.finance.service.BudgetCommandService;
import com.lifeos.finance.service.FinanceCommands;
import com.lifeos.finance.service.FinanceQueryService;
import com.lifeos.finance.service.RecurringCommandService;
import com.lifeos.finance.service.SavingCommandService;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.validation.Valid;
import java.net.URI;
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

/** Ngân sách, giao dịch định kỳ, mục tiêu tiết kiệm (PHASE_3 §6). userId luôn lấy từ CurrentUser. */
@RestController
@RequestMapping("/api/v1/finance")
class FinancePlanningController {

    private final CurrentUser currentUser;
    private final BudgetCommandService budgets;
    private final RecurringCommandService recurring;
    private final SavingCommandService savings;
    private final FinanceQueryService queries;

    FinancePlanningController(CurrentUser currentUser, BudgetCommandService budgets,
            RecurringCommandService recurring, SavingCommandService savings, FinanceQueryService queries) {
        this.currentUser = currentUser;
        this.budgets = budgets;
        this.recurring = recurring;
        this.savings = savings;
        this.queries = queries;
    }

    // ---------------------------------------------------------------- ngân sách

    @ApiResponse(responseCode = "201", description = "Đã tạo",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = BudgetResponse.class)))
    @PostMapping("/budgets")
    ResponseEntity<BudgetResponse> createBudget(@Valid @RequestBody CreateBudgetRequest r) {
        BudgetResponse created = budgets.create(currentUser.userId(), new FinanceCommands.CreateBudget(r.id(),
                Patch.of(r.categoryId()), Patch.of(r.currency()), Patch.of(r.amount())), null);
        return ResponseEntity.created(URI.create("/api/v1/finance/budgets/" + created.id())).body(created);
    }

    @GetMapping("/budgets")
    PageResponse<BudgetResponse> listBudgets(@RequestParam(required = false) String archived,
            @RequestParam(required = false) Integer limit, @RequestParam(required = false) String cursor) {
        return queries.budgets(currentUser.userId(), archived, limit, cursor);
    }

    @GetMapping("/budgets/usage")
    List<BudgetUsageResponse> usageAll(@RequestParam(required = false) String month) {
        return queries.usageAll(currentUser.userId(), month);
    }

    @GetMapping("/budgets/{id}")
    BudgetResponse getBudget(@PathVariable UUID id) {
        return queries.budget(currentUser.userId(), id);
    }

    @GetMapping("/budgets/{id}/usage")
    BudgetUsageResponse usage(@PathVariable UUID id, @RequestParam(required = false) String month) {
        return queries.usage(currentUser.userId(), id, month);
    }

    @PatchMapping("/budgets/{id}")
    BudgetResponse updateBudget(@PathVariable UUID id, @Valid @RequestBody UpdateBudgetRequest r) {
        return budgets.update(currentUser.userId(), id, new FinanceCommands.UpdateBudget(r.version(), r.categoryId(),
                r.currency(), r.amount(), Patch.absent()), null);
    }

    @PostMapping("/budgets/{id}/archive")
    BudgetResponse archiveBudget(@PathVariable UUID id, @Valid @RequestBody VersionRequest r) {
        return budgets.archive(currentUser.userId(), id, r.version());
    }

    @PostMapping("/budgets/{id}/unarchive")
    BudgetResponse unarchiveBudget(@PathVariable UUID id, @Valid @RequestBody VersionRequest r) {
        return budgets.unarchive(currentUser.userId(), id, r.version());
    }

    @ApiResponse(responseCode = "204", description = "Đã xóa (tombstone)")
    @DeleteMapping("/budgets/{id}")
    ResponseEntity<Void> deleteBudget(@PathVariable UUID id, @RequestParam long version) {
        budgets.delete(currentUser.userId(), id, version, null);
        return ResponseEntity.noContent().build();
    }

    // ---------------------------------------------------------------- định kỳ

    @ApiResponse(responseCode = "201", description = "Đã tạo", content = @Content(mediaType = "application/json",
            schema = @Schema(implementation = RecurringResponse.class)))
    @PostMapping("/recurring")
    ResponseEntity<RecurringResponse> createRecurring(@Valid @RequestBody CreateRecurringRequest r) {
        RecurringResponse created = recurring.create(currentUser.userId(), new FinanceCommands.CreateRecurring(r.id(),
                Patch.of(r.accountId()), Patch.of(r.categoryId()), Patch.of(r.kind()), Patch.of(r.amount()),
                Patch.of(r.note()), Patch.of(r.frequency()), Patch.of(r.intervalCount()), Patch.of(r.startOn()),
                Patch.of(r.endOn()), Patch.absent()), null);
        return ResponseEntity.created(URI.create("/api/v1/finance/recurring/" + created.id())).body(created);
    }

    @GetMapping("/recurring")
    PageResponse<RecurringResponse> listRecurring(@RequestParam(required = false) Integer limit,
            @RequestParam(required = false) String cursor) {
        return queries.recurring(currentUser.userId(), limit, cursor);
    }

    @GetMapping("/recurring/{id}")
    RecurringResponse getRecurring(@PathVariable UUID id) {
        return queries.recurringOne(currentUser.userId(), id);
    }

    @PatchMapping("/recurring/{id}")
    RecurringResponse updateRecurring(@PathVariable UUID id, @Valid @RequestBody UpdateRecurringRequest r) {
        return recurring.update(currentUser.userId(), id, new FinanceCommands.UpdateRecurring(r.version(),
                r.accountId(), r.categoryId(), r.kind(), r.amount(), r.note(), r.frequency(), r.intervalCount(),
                r.startOn(), r.endOn(), Patch.absent()), null);
    }

    @PostMapping("/recurring/{id}/pause")
    RecurringResponse pause(@PathVariable UUID id, @Valid @RequestBody VersionRequest r) {
        return recurring.pause(currentUser.userId(), id, r.version());
    }

    @PostMapping("/recurring/{id}/resume")
    RecurringResponse resume(@PathVariable UUID id, @Valid @RequestBody VersionRequest r) {
        return recurring.resume(currentUser.userId(), id, r.version());
    }

    @ApiResponse(responseCode = "204", description = "Đã xóa (tombstone; giao dịch đã sinh giữ nguyên)")
    @DeleteMapping("/recurring/{id}")
    ResponseEntity<Void> deleteRecurring(@PathVariable UUID id, @RequestParam long version) {
        recurring.delete(currentUser.userId(), id, version, null);
        return ResponseEntity.noContent().build();
    }

    // ---------------------------------------------------------------- mục tiêu tiết kiệm

    @ApiResponse(responseCode = "201", description = "Đã tạo",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = GoalResponse.class)))
    @PostMapping("/saving-goals")
    ResponseEntity<GoalResponse> createGoal(@Valid @RequestBody CreateGoalRequest r) {
        GoalResponse created = savings.createGoal(currentUser.userId(), new FinanceCommands.CreateGoal(r.id(),
                Patch.of(r.name()), Patch.of(r.currency()), Patch.of(r.targetAmount()), Patch.of(r.targetOn()),
                Patch.absent()), null);
        return ResponseEntity.created(URI.create("/api/v1/finance/saving-goals/" + created.id())).body(created);
    }

    @GetMapping("/saving-goals")
    PageResponse<GoalView> listGoals(@RequestParam(required = false) String status,
            @RequestParam(required = false) Integer limit, @RequestParam(required = false) String cursor) {
        return queries.goals(currentUser.userId(), status, limit, cursor);
    }

    @GetMapping("/saving-goals/{id}")
    GoalView getGoal(@PathVariable UUID id) {
        return queries.goal(currentUser.userId(), id);
    }

    @PatchMapping("/saving-goals/{id}")
    GoalResponse updateGoal(@PathVariable UUID id, @Valid @RequestBody UpdateGoalRequest r) {
        return savings.updateGoal(currentUser.userId(), id, new FinanceCommands.UpdateGoal(r.version(), r.name(),
                r.currency(), r.targetAmount(), r.targetOn(), r.status()), null);
    }

    @ApiResponse(responseCode = "204", description = "Đã xóa (tombstone, kèm khoản góp)")
    @DeleteMapping("/saving-goals/{id}")
    ResponseEntity<Void> deleteGoal(@PathVariable UUID id, @RequestParam long version) {
        savings.deleteGoal(currentUser.userId(), id, version, null);
        return ResponseEntity.noContent().build();
    }

    @ApiResponse(responseCode = "201", description = "Đã tạo", content = @Content(mediaType = "application/json",
            schema = @Schema(implementation = ContributionResponse.class)))
    @PostMapping("/saving-goals/{id}/contributions")
    ResponseEntity<ContributionResponse> contribute(@PathVariable UUID id,
            @Valid @RequestBody CreateContributionRequest r) {
        ContributionResponse created = savings.createContribution(currentUser.userId(), id,
                new FinanceCommands.CreateContribution(r.id(), Patch.absent(), Patch.of(r.amount()),
                        Patch.of(r.occurredOn()), Patch.of(r.note())), null);
        return ResponseEntity.created(URI.create("/api/v1/finance/saving-contributions/" + created.id()))
                .body(created);
    }

    @GetMapping("/saving-goals/{id}/contributions")
    PageResponse<ContributionResponse> listContributions(@PathVariable UUID id,
            @RequestParam(required = false) Integer limit, @RequestParam(required = false) String cursor) {
        return queries.contributions(currentUser.userId(), id, limit, cursor);
    }

    @ApiResponse(responseCode = "204", description = "Đã xóa (tombstone)")
    @DeleteMapping("/saving-contributions/{id}")
    ResponseEntity<Void> deleteContribution(@PathVariable UUID id, @RequestParam long version) {
        savings.deleteContribution(currentUser.userId(), id, version, null);
        return ResponseEntity.noContent().build();
    }
}
