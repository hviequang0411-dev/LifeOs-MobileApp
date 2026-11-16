package com.lifeos.ai.service;

import com.lifeos.ai.dto.AiChatRequest;
import com.lifeos.ai.dto.AiChatResponse;
import com.lifeos.ai.config.AiProperties;
import com.lifeos.ai.provider.LLMProvider;
import com.lifeos.ai.repository.AiStore;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
public class AiChatService {
    private final AiQuotaGuard quota;
    private final AiContextBuilder contexts;
    private final AiConversationTxService tx;
    private final LLMProvider provider;
    private final AiConcurrencyGate concurrency;
    private final AiOutputSanitizer sanitizer;
    private final AiProperties properties;
    private final Clock clock;

    public AiChatService(AiQuotaGuard quota, AiContextBuilder contexts, AiConversationTxService tx,
            LLMProvider provider, AiConcurrencyGate concurrency, AiOutputSanitizer sanitizer,
            AiProperties properties, Clock clock) {
        this.quota = quota; this.contexts = contexts; this.tx = tx; this.provider = provider;
        this.concurrency = concurrency; this.sanitizer = sanitizer; this.properties = properties; this.clock = clock;
    }

    public AiChatResponse send(UUID userId, UUID conversationId, AiChatRequest request) {
        quota.check(userId);
        if (!properties.getFeatures().isChat()) {
            throw AiErrors.error("AI_DISABLED", HttpStatus.SERVICE_UNAVAILABLE, "Tính năng chat AI đang tắt.");
        }
        AiStore.Message user = tx.saveUser(userId, conversationId, request.message(), request.mode().name());
        AiContextBuilder.Snapshot snapshot = contexts.build(userId, request.contextDomains(), null, null, null, null);
        UUID correlationId = UUID.randomUUID(); Instant started = clock.instant();
        try {
            LLMProvider.Response generated = concurrency.execute(userId, () -> provider.generate(new LLMProvider.Request(
                    request.mode().name(), request.message(), snapshot.providerPayload(), Map.of())));
            if (!"SUCCESS".equals(generated.resultCode())) {
                throw AiErrors.error("AI_PROVIDER_UNAVAILABLE", HttpStatus.SERVICE_UNAVAILABLE,
                        "Nhà cung cấp AI không hoàn tất phản hồi.");
            }
            String content = sanitizer.sanitize(generated.content());
            AiStore.Message assistant = tx.saveAssistant(userId, conversationId, request.mode().name(), content,
                    generated.structured(), snapshot.providerPayload(), generated, correlationId);
            return new AiChatResponse(conversationId, AiConversationService.message(user),
                    AiConversationService.message(assistant), List.of());
        } catch (RuntimeException e) {
            long latency = Math.max(0, Duration.between(started, clock.instant()).toMillis());
            tx.saveFailure(userId, conversationId, "FAILED", latency, correlationId);
            throw e;
        }
    }
}
