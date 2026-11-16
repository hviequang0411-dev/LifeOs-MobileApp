package com.lifeos.sync.dto;

import java.util.List;

/** Phản hồi GET /sync/pull (SYSTEM_ARCHITECTURE §4.2): changes theo thứ tự seq, nextCursor mờ, hasMore. */
public record PullResponse(List<PullChange> changes, String nextCursor, boolean hasMore) {
}
