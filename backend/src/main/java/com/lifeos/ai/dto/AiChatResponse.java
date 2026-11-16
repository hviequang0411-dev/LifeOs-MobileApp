package com.lifeos.ai.dto;

import java.util.List;
import java.util.UUID;

public record AiChatResponse(UUID conversationId, AiMessageResponse userMessage,
        AiMessageResponse assistantMessage, List<Object> actionProposals) { }
