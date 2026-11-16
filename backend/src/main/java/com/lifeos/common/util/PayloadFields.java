package com.lifeos.common.util;

import com.lifeos.common.exception.ErrorCode;
import com.lifeos.common.exception.FieldErrors;
import java.util.Map;
import java.util.UUID;

/**
 * Đọc một khóa của payload sync (Map đã parse từ JSON) thành {@link Patch} có kiểu (SYNC_PROTOCOL §3, D82, D93):
 * khóa vắng mặt → {@link Patch#absent()}; null tường minh → {@code Patch.of(null)}; sai kiểu JSON →
 * {@link Patch#invalid} mang lỗi VALIDATION_FAILED với fieldError {@code {field, INVALID_VALUE}}. Lỗi kiểu chỉ nổ khi
 * service đọc giá trị, nên giữ đúng thứ tự D96 (tồn tại → version → giá trị). Chỉ kiểm KIỂU; quy tắc giá trị (độ dài,
 * enum, định dạng ngày…) vẫn ở *CommandService như REST.
 */
public final class PayloadFields {

    private PayloadFields() {
    }

    public static Patch<String> string(Map<String, Object> payload, String key) {
        if (!payload.containsKey(key)) {
            return Patch.absent();
        }
        Object value = payload.get(key);
        if (value == null || value instanceof String) {
            return Patch.of((String) value);
        }
        return invalid(key);
    }

    /** UUID dạng chuỗi chuẩn 8-4-4-4-12. */
    public static Patch<UUID> uuid(Map<String, Object> payload, String key) {
        if (!payload.containsKey(key)) {
            return Patch.absent();
        }
        Object value = payload.get(key);
        if (value == null) {
            return Patch.of(null);
        }
        if (value instanceof String text) {
            try {
                UUID parsed = UUID.fromString(text);
                if (parsed.toString().equalsIgnoreCase(text)) {
                    return Patch.of(parsed);
                }
            } catch (IllegalArgumentException e) {
                // rơi xuống invalid
            }
        }
        return invalid(key);
    }

    public static Patch<Boolean> bool(Map<String, Object> payload, String key) {
        if (!payload.containsKey(key)) {
            return Patch.absent();
        }
        Object value = payload.get(key);
        if (value == null || value instanceof Boolean) {
            return Patch.of((Boolean) value);
        }
        return invalid(key);
    }

    /** Số nguyên JSON trong phạm vi int (không nhận số thực, chuỗi số hay boolean). */
    public static Patch<Integer> integer(Map<String, Object> payload, String key) {
        if (!payload.containsKey(key)) {
            return Patch.absent();
        }
        Object value = payload.get(key);
        if (value == null) {
            return Patch.of(null);
        }
        if ((value instanceof Integer || value instanceof Long || value instanceof Short)
                && ((Number) value).longValue() >= Integer.MIN_VALUE
                && ((Number) value).longValue() <= Integer.MAX_VALUE) {
            return Patch.of(((Number) value).intValue());
        }
        return invalid(key);
    }

    /** Số nguyên JSON trong phạm vi long (không nhận số thực, chuỗi số hay boolean). */
    public static Patch<Long> longValue(Map<String, Object> payload, String key) {
        if (!payload.containsKey(key)) {
            return Patch.absent();
        }
        Object value = payload.get(key);
        if (value == null) {
            return Patch.of(null);
        }
        if (value instanceof Integer || value instanceof Long || value instanceof Short) {
            return Patch.of(((Number) value).longValue());
        }
        return invalid(key);
    }

    /** Đối tượng JSON (ví dụ quy tắc lặp D126); mảng, chuỗi, số → sai kiểu. */
    @SuppressWarnings("unchecked")
    public static Patch<Map<String, Object>> object(Map<String, Object> payload, String key) {
        if (!payload.containsKey(key)) {
            return Patch.absent();
        }
        Object value = payload.get(key);
        if (value == null || value instanceof Map) {
            return Patch.of((Map<String, Object>) value);
        }
        return invalid(key);
    }

    private static <T> Patch<T> invalid(String key) {
        return Patch.invalid(() -> FieldErrors.single(ErrorCode.VALIDATION_FAILED, key, "INVALID_VALUE",
                "Giá trị không đúng kiểu."));
    }
}
