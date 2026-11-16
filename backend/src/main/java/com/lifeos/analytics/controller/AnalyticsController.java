package com.lifeos.analytics.controller;

import com.lifeos.analytics.dto.CreateReviewRequest;
import com.lifeos.analytics.dto.DashboardResponse;
import com.lifeos.analytics.dto.FinanceAnalytics;
import com.lifeos.analytics.dto.GoalAnalytics;
import com.lifeos.analytics.dto.HabitAnalytics;
import com.lifeos.analytics.dto.ReviewResponse;
import com.lifeos.analytics.dto.ReviewSummary;
import com.lifeos.analytics.dto.TaskAnalytics;
import com.lifeos.analytics.dto.TimeAnalytics;
import com.lifeos.analytics.dto.UpdateReviewRequest;
import com.lifeos.analytics.service.AnalyticsService;
import com.lifeos.analytics.service.DashboardService;
import com.lifeos.analytics.service.ReviewCommandService;
import com.lifeos.analytics.service.ReviewCommands;
import com.lifeos.analytics.service.ReviewQueryService;
import com.lifeos.analytics.service.ReviewSummaryService;
import com.lifeos.common.response.PageResponse;
import com.lifeos.common.security.CurrentUser;
import com.lifeos.common.util.Patch;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.validation.Valid;
import java.net.URI;
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

/** Dashboard, review và analytics của user hiện tại (PHASE_7 §6). userId luôn lấy từ CurrentUser. */
@RestController
@RequestMapping("/api/v1")
class AnalyticsController {

    private final CurrentUser currentUser;
    private final DashboardService dashboard;
    private final ReviewSummaryService summaries;
    private final ReviewCommandService reviewCommands;
    private final ReviewQueryService reviewQueries;
    private final AnalyticsService analytics;

    AnalyticsController(CurrentUser currentUser, DashboardService dashboard, ReviewSummaryService summaries,
            ReviewCommandService reviewCommands, ReviewQueryService reviewQueries, AnalyticsService analytics) {
        this.currentUser = currentUser;
        this.dashboard = dashboard;
        this.summaries = summaries;
        this.reviewCommands = reviewCommands;
        this.reviewQueries = reviewQueries;
        this.analytics = analytics;
    }

    @GetMapping("/dashboard")
    DashboardResponse dashboard() {
        return dashboard.get(currentUser.userId());
    }

    @GetMapping("/reviews/daily")
    ReviewSummary daily(@RequestParam(required = false) String date) {
        return summaries.daily(currentUser.userId(), date);
    }

    @GetMapping("/reviews/weekly")
    ReviewSummary weekly(@RequestParam(required = false) String date) {
        return summaries.weekly(currentUser.userId(), date);
    }

    @GetMapping("/reviews/monthly")
    ReviewSummary monthly(@RequestParam(required = false) String month) {
        return summaries.monthly(currentUser.userId(), month);
    }

    @ApiResponse(responseCode = "201", description = "Đã tạo",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ReviewResponse.class)))
    @PostMapping("/reviews")
    ResponseEntity<ReviewResponse> create(@Valid @RequestBody CreateReviewRequest r) {
        ReviewResponse created = reviewCommands.create(currentUser.userId(), new ReviewCommands.Create(r.id(),
                Patch.of(r.periodType()), Patch.of(r.periodKey()), Patch.of(r.reflection()), Patch.of(r.mood())),
                null);
        return ResponseEntity.created(URI.create("/api/v1/reviews/" + created.id())).body(created);
    }

    @GetMapping("/reviews")
    PageResponse<ReviewResponse> list(@RequestParam(required = false) String periodType,
            @RequestParam(required = false) Integer limit, @RequestParam(required = false) String cursor) {
        return reviewQueries.list(currentUser.userId(), periodType, limit, cursor);
    }

    @GetMapping("/reviews/{id}")
    ReviewResponse get(@PathVariable UUID id) {
        return reviewQueries.get(currentUser.userId(), id);
    }

    @PatchMapping("/reviews/{id}")
    ReviewResponse update(@PathVariable UUID id, @Valid @RequestBody UpdateReviewRequest r) {
        return reviewCommands.update(currentUser.userId(), id, new ReviewCommands.Update(r.version(), Patch.absent(),
                Patch.absent(), r.reflection(), r.mood()), null);
    }

    @ApiResponse(responseCode = "204", description = "Đã xóa")
    @DeleteMapping("/reviews/{id}")
    ResponseEntity<Void> delete(@PathVariable UUID id, @RequestParam long version) {
        reviewCommands.delete(currentUser.userId(), id, version, null);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/analytics/tasks")
    TaskAnalytics tasks(@RequestParam(required = false) String from, @RequestParam(required = false) String to,
            @RequestParam(required = false) String granularity) {
        return analytics.tasks(currentUser.userId(), from, to, granularity);
    }

    @GetMapping("/analytics/habits")
    HabitAnalytics habits(@RequestParam(required = false) String from, @RequestParam(required = false) String to) {
        return analytics.habits(currentUser.userId(), from, to);
    }

    @GetMapping("/analytics/finance")
    FinanceAnalytics finance(@RequestParam(required = false) String from, @RequestParam(required = false) String to,
            @RequestParam(required = false) String granularity, @RequestParam(required = false) String currency) {
        return analytics.finance(currentUser.userId(), from, to, granularity, currency);
    }

    @GetMapping("/analytics/time")
    TimeAnalytics time(@RequestParam(required = false) String from, @RequestParam(required = false) String to,
            @RequestParam(required = false) String granularity) {
        return analytics.time(currentUser.userId(), from, to, granularity);
    }

    @GetMapping("/analytics/goals")
    GoalAnalytics goals() {
        return analytics.goals(currentUser.userId());
    }
}
