package com.lifeos.ai.service;

import com.lifeos.ai.dto.*;
import com.lifeos.ai.repository.AiStore;
import com.lifeos.common.web.Cursors;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class AiConversationService {
    private static final String CONVERSATION_SORT = "lastMessageAt:desc";
    private static final String MESSAGE_SORT = "createdAt:asc";
    private final AiStore store;
    private final AiConversationTxService tx;

    public AiConversationService(AiStore store, AiConversationTxService tx) { this.store = store; this.tx = tx; }

    public AiConversationResponse create(UUID userId, AiConversationCreateRequest request) {
        return conversation(tx.create(userId, request.title()));
    }

    public AiConversationPage list(UUID userId, Integer limit, String cursor) {
        int size = Cursors.limit(limit); Cursors.Position after = Cursors.decode(cursor, CONVERSATION_SORT);
        Instant at = instant(after); UUID id = after == null ? null : after.id();
        List<AiStore.Conversation> rows = store.conversations(userId, at, id, size + 1);
        boolean more = rows.size() > size; List<AiStore.Conversation> page = more ? rows.subList(0, size) : rows;
        String next = more ? Cursors.encode(new Cursors.Position(CONVERSATION_SORT,
                page.get(page.size() - 1).lastMessageAt().toString(), page.get(page.size() - 1).id())) : null;
        return new AiConversationPage(page.stream().map(AiConversationService::conversation).toList(), next, more);
    }

    public AiConversationDetailResponse get(UUID userId, UUID id, Integer limit, String cursor) {
        AiStore.Conversation c = store.conversation(userId, id).orElseThrow(AiErrors::conversationNotFound);
        return new AiConversationDetailResponse(conversation(c), messages(userId, id, limit, cursor));
    }

    public void delete(UUID userId, UUID id) {
        if (!tx.delete(userId, id)) throw AiErrors.conversationNotFound();
    }

    public AiContextResponse context(UUID userId, UUID messageId) {
        AiStore.Message message = store.message(userId, messageId).filter(m -> "ASSISTANT".equals(m.role()))
                .orElseThrow(AiErrors::conversationNotFound);
        Map<String, Object> data = message.context();
        if (data == null) throw AiErrors.conversationNotFound();
        Instant captured = Instant.parse(String.valueOf(data.get("capturedAt")));
        LinkedHashSet<AiContextDomain> domains = new LinkedHashSet<>();
        for (Object d : list(data.get("domains"))) domains.add(AiContextDomain.valueOf(String.valueOf(d)));
        List<AiContextEntity> entities = new ArrayList<>();
        for (Object raw : list(data.get("entities"))) {
            Map<?, ?> e = (Map<?, ?>) raw;
            entities.add(new AiContextEntity(AiContextDomain.valueOf(String.valueOf(e.get("domain"))),
                    String.valueOf(e.get("entityType")), UUID.fromString(String.valueOf(e.get("entityId"))),
                    e.get("entityVersion") == null ? null : ((Number) e.get("entityVersion")).longValue(),
                    castMap(e.get("fields"))));
        }
        return new AiContextResponse(messageId, captured, domains, entities, Boolean.TRUE.equals(data.get("truncated")));
    }

    private AiMessagePage messages(UUID userId, UUID conversationId, Integer limit, String cursor) {
        int size = Cursors.limit(limit); Cursors.Position after = Cursors.decode(cursor, MESSAGE_SORT);
        List<AiStore.Message> rows = store.messages(userId, conversationId, instant(after),
                after == null ? null : after.id(), size + 1);
        boolean more = rows.size() > size; List<AiStore.Message> page = more ? rows.subList(0, size) : rows;
        String next = more ? Cursors.encode(new Cursors.Position(MESSAGE_SORT,
                page.get(page.size() - 1).createdAt().toString(), page.get(page.size() - 1).id())) : null;
        return new AiMessagePage(page.stream().map(AiConversationService::message).toList(), next, more);
    }

    static AiMessageResponse message(AiStore.Message m) {
        return new AiMessageResponse(m.id(), AiMessageRole.valueOf(m.role()), m.content(),
                AiGenerationMode.valueOf(m.kind()), m.context() != null, m.createdAt());
    }

    private static AiConversationResponse conversation(AiStore.Conversation c) {
        return new AiConversationResponse(c.id(), c.title(), c.createdAt(), c.updatedAt(), c.expiresAt());
    }

    private static Instant instant(Cursors.Position p) {
        if (p == null) return null;
        try { return Instant.parse(p.key()); } catch (DateTimeParseException | NullPointerException e) {
            throw AiErrors.error("AI_RESPONSE_INVALID", org.springframework.http.HttpStatus.BAD_REQUEST,
                    "Con trỏ AI không hợp lệ.");
        }
    }

    private static List<?> list(Object value) { return value instanceof List<?> v ? v : List.of(); }
    @SuppressWarnings("unchecked") private static Map<String, Object> castMap(Object value) {
        return value instanceof Map<?, ?> ? (Map<String, Object>) value : Map.of();
    }
}
