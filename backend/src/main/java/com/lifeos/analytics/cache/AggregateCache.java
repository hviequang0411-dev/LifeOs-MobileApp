package com.lifeos.analytics.cache;

import java.util.UUID;
import java.util.function.Supplier;

/**
 * Cache đọc-qua cho kết quả tổng hợp (PHASE_7 D193–D195): khóa luôn chứa userId; không cache dữ liệu gốc. Theo
 * BACKEND_MASTER_PLAN §4 mục 7, lần này chỉ có bản no-op ({@code lifeos.cache.enabled=false}); Redis thêm sau nếu
 * benchmark D207 vượt ngưỡng. Cài đặt thật phải fail-open (lỗi/timeout 100 ms ⇒ tính trực tiếp).
 */
public interface AggregateCache {

    <T> T get(UUID userId, String kind, String periodKey, Supplier<T> compute);

    /** Vô hiệu mọi khóa của user (D194, gọi sau commit). */
    void invalidate(UUID userId);
}
