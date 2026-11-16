package com.lifeos.ai.service;

import com.lifeos.ai.config.AiProperties;
import com.lifeos.ai.provider.LLMProvider;
import com.lifeos.ai.repository.AiStore;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AiConversationTxService {
    private static final Duration RETENTION = Duration.ofDays(90);
    private final AiStore store;
    private final AiProperties properties;
    private final AiCostCalculator costs;
    private final Clock clock;

    public AiConversationTxService(AiStore store, AiProperties properties, AiCostCalculator costs, Clock clock) {
        this.store = store; this.properties = properties; this.costs = costs; this.clock = clock;
    }

    @Transactional
    public AiStore.Conversation create(UUID userId, String title) {
        Instant now = clock.instant();
        String clean = title == null || title.isBlank() ? "Hội thoại mới" : title.strip();
        return store.createConversation(userId, UUID.randomUUID(), clean, now, now.plus(RETENTION));
    }

    @Transactional
    public AiStore.Message saveUser(UUID userId, UUID conversationId, String content, String mode) {
        store.conversation(userId, conversationId).orElseThrow(AiErrors::conversationNotFound);
        Instant now = clock.instant();
        AiStore.Message message = store.createMessage(userId, UUID.randomUUID(), conversationId, "USER", mode,
                content, null, null, null, null, null, now);
        store.touchConversation(userId, conversationId, now, now.plus(RETENTION));
        return message;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public AiStore.Message saveAssistant(UUID userId, UUID conversationId, String mode, String content,
            Map<String, Object> structured, Map<String, Object> context, LLMProvider.Response response,
            UUID correlationId) {
        Instant now = clock.instant();
        AiStore.Message message = store.createMessage(userId, UUID.randomUUID(), conversationId, "ASSISTANT", mode,
                content, structured, context, "ai-v1", properties.getProvider(), properties.getModel(), now);
        store.touchConversation(userId, conversationId, now, now.plus(RETENTION));
        store.recordUsage(userId, conversationId, message.id(), properties.getProvider(), properties.getModel(),
                response.inputTokens(), response.cachedInputTokens(), response.outputTokens(), response.reasoningTokens(),
                costs.estimate(response.inputTokens(), response.outputTokens()), response.latencyMs(),
                response.resultCode(), correlationId, now);
        return message;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void saveFailure(UUID userId, UUID conversationId, String resultCode, long latencyMs, UUID correlationId) {
        store.recordUsage(userId, conversationId, null, properties.getProvider(), properties.getModel(), 0, 0, 0, 0,
                0, latencyMs, resultCode, correlationId, clock.instant());
    }

    @Transactional
    public boolean delete(UUID userId, UUID conversationId) { return store.deleteConversation(userId, conversationId); }
}
