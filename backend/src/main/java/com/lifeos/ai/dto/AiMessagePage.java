package com.lifeos.ai.dto;

import java.util.List;

public record AiMessagePage(List<AiMessageResponse> items, String nextCursor, boolean hasMore) { }
