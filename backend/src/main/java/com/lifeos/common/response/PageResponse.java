package com.lifeos.common.response;

import java.util.List;

/** Trang danh sách theo con trỏ (API_CONVENTIONS §7, D59). Không có tổng số bản ghi ở Phase 1. */
public record PageResponse<T>(List<T> items, String nextCursor, boolean hasMore) {
}
