package com.lifeos.ai.dto;

import java.util.List;

public record AiConversationPage(List<AiConversationResponse> items, String nextCursor, boolean hasMore) { }
