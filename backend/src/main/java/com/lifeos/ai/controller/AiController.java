package com.lifeos.ai.controller;

import com.lifeos.ai.dto.*;
import com.lifeos.ai.service.AiChatService;
import com.lifeos.ai.service.AiConversationService;
import com.lifeos.ai.service.AiDataService;
import com.lifeos.ai.service.AiGenerationService;
import com.lifeos.ai.service.AiPreferenceService;
import com.lifeos.ai.service.AiUsageService;
import com.lifeos.common.security.CurrentUser;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import java.net.URI;
import java.time.LocalDate;
import java.util.UUID;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/ai")
public class AiController {
    private final CurrentUser currentUser;
    private final AiPreferenceService preferences;
    private final AiUsageService usage;
    private final AiDataService data;
    private final AiConversationService conversations;
    private final AiChatService chat;
    private final AiGenerationService generations;

    public AiController(CurrentUser currentUser, AiPreferenceService preferences, AiUsageService usage,
            AiDataService data, AiConversationService conversations, AiChatService chat,
            AiGenerationService generations) {
        this.currentUser = currentUser;
        this.preferences = preferences;
        this.usage = usage;
        this.data = data;
        this.conversations = conversations;
        this.chat = chat;
        this.generations = generations;
    }

    @Operation(operationId = "aiGetPreferences")
    @GetMapping("/preferences")
    public AiPreferencesResponse getPreferences() {
        return preferences.get(currentUser.userId());
    }

    @Operation(operationId = "aiReplacePreferences")
    @PutMapping("/preferences")
    public AiPreferencesResponse replacePreferences(@Valid @RequestBody AiPreferencesRequest request) {
        return preferences.replace(currentUser.userId(), request);
    }

    @Operation(operationId = "aiGetUsage")
    @GetMapping("/usage")
    public AiUsageResponse getUsage(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return usage.usage(currentUser.userId(), from, to);
    }

    @Operation(operationId = "aiGetLimits")
    @GetMapping("/limits")
    public AiLimitsResponse getLimits() {
        return usage.limits(currentUser.userId());
    }

    @Operation(operationId = "aiDeleteAllData")
    @DeleteMapping("/data")
    public ResponseEntity<Void> deleteAllData() {
        data.deleteAll(currentUser.userId());
        return ResponseEntity.noContent().build();
    }

    @Operation(operationId = "aiCreateConversation")
    @PostMapping("/conversations")
    public ResponseEntity<AiConversationResponse> createConversation(
            @Valid @RequestBody AiConversationCreateRequest request) {
        AiConversationResponse created = conversations.create(currentUser.userId(), request);
        return ResponseEntity.created(URI.create("/api/v1/ai/conversations/" + created.id())).body(created);
    }

    @Operation(operationId = "aiListConversations")
    @GetMapping("/conversations")
    public AiConversationPage listConversations(@RequestParam(required = false) Integer limit,
            @RequestParam(required = false) String cursor) {
        return conversations.list(currentUser.userId(), limit, cursor);
    }

    @Operation(operationId = "aiGetConversation")
    @GetMapping("/conversations/{id}")
    public AiConversationDetailResponse getConversation(@PathVariable UUID id,
            @RequestParam(required = false) Integer limit, @RequestParam(required = false) String cursor) {
        return conversations.get(currentUser.userId(), id, limit, cursor);
    }

    @Operation(operationId = "aiDeleteConversation")
    @DeleteMapping("/conversations/{id}")
    public ResponseEntity<Void> deleteConversation(@PathVariable UUID id) {
        conversations.delete(currentUser.userId(), id);
        return ResponseEntity.noContent().build();
    }

    @Operation(operationId = "aiCreateMessage")
    @PostMapping("/conversations/{id}/messages")
    public ResponseEntity<AiChatResponse> createMessage(@PathVariable UUID id,
            @Valid @RequestBody AiChatRequest request) {
        AiChatResponse created = chat.send(currentUser.userId(), id, request);
        return ResponseEntity.created(URI.create("/api/v1/ai/conversations/" + id + "/messages/"
                + created.assistantMessage().id())).body(created);
    }

    @Operation(operationId = "aiGetMessageContext")
    @GetMapping("/messages/{id}/context")
    public AiContextResponse getMessageContext(@PathVariable UUID id) {
        return conversations.context(currentUser.userId(), id);
    }

    @Operation(operationId = "aiCreateDailyPlan")
    @PostMapping("/plans/daily")
    public AiPlanResponse createDailyPlan(@Valid @RequestBody AiPlanRequest request) {
        return generations.plan(currentUser.userId(), false, request);
    }

    @Operation(operationId = "aiCreateWeeklyPlan")
    @PostMapping("/plans/weekly")
    public AiPlanResponse createWeeklyPlan(@Valid @RequestBody AiPlanRequest request) {
        return generations.plan(currentUser.userId(), true, request);
    }

    @Operation(operationId = "aiBreakDownTask")
    @PostMapping("/tasks/{id}/breakdown")
    public AiTaskBreakdownResponse breakDownTask(@PathVariable UUID id,
            @Valid @RequestBody AiTaskBreakdownRequest request) {
        return generations.breakdown(currentUser.userId(), id, request);
    }

    @Operation(operationId = "aiCreateHabitInsight")
    @PostMapping("/insights/habits")
    public AiInsightResponse createHabitInsight(@Valid @RequestBody AiInsightRequest request) {
        return generations.habitInsight(currentUser.userId(), request);
    }

    @Operation(operationId = "aiCreateFinanceInsight")
    @PostMapping("/insights/finance")
    public AiInsightResponse createFinanceInsight(@Valid @RequestBody AiFinanceInsightRequest request) {
        return generations.financeInsight(currentUser.userId(), request);
    }

    @Operation(operationId = "aiSummarizeNote")
    @PostMapping("/notes/{id}/summary")
    public AiNoteSummaryResponse summarizeNote(@PathVariable UUID id,
            @Valid @RequestBody AiNoteSummaryRequest request) {
        return generations.noteSummary(currentUser.userId(), id, request);
    }
}
