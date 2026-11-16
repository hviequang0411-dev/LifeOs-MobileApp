package com.lifeos.sync.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Một phần tử {@code results[i]} của POST /sync/push (SYNC_PROTOCOL §2.2, D83). Chỉ có các trường của đúng trạng thái:
 * APPLIED → entityVersion; CONFLICT → serverSnapshot (+ conflictReason ALREADY_EXISTS ở CREATE trùng khóa tự nhiên,
 * D235); REJECTED → errorCode, message (+ fieldErrors {field, code});
 * DUPLICATE → nguyên văn kết quả đã lưu, thêm originalStatus. Đối tượng lưu vào sync_mutations.result là kết quả gốc
 * (không có DUPLICATE/originalStatus).
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record MutationResult(UUID mutationId, String status, String originalStatus, Long entityVersion,
        String conflictReason, ServerSnapshot serverSnapshot, String errorCode, String message,
        List<Map<String, Object>> fieldErrors) {

    public static final String APPLIED = "APPLIED";
    public static final String CONFLICT = "CONFLICT";
    public static final String REJECTED = "REJECTED";
    public static final String DUPLICATE = "DUPLICATE";

    /** Phát lại kết quả đã lưu (D83): status = DUPLICATE, originalStatus = trạng thái gốc. */
    public MutationResult asDuplicate() {
        return new MutationResult(mutationId, DUPLICATE, status, entityVersion, conflictReason, serverSnapshot,
                errorCode, message, fieldErrors);
    }

    @Override
    public String toString() {
        return "MutationResult[mutationId=" + mutationId + ", status=" + status + "]";
    }
}
