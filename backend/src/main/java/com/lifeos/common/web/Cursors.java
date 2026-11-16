package com.lifeos.common.web;

import com.lifeos.common.exception.ErrorCode;
import com.lifeos.common.exception.FieldErrors;
import com.lifeos.common.exception.ValidationException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Objects;
import java.util.UUID;

/**
 * Con trỏ phân trang mờ cho REST (D59): mã hóa base64url của "sort, key, id" — khóa sắp xếp của bản ghi cuối trang và
 * id làm tiêu chí phụ (keyset). Con trỏ KHÔNG chứa userId (user luôn lấy từ token), nên sửa con trỏ không đọc được dữ
 * liệu người khác. Con trỏ hỏng hoặc không khớp kiểu sắp xếp của request → 400 VALIDATION_FAILED (field cursor).
 */
public final class Cursors {

    private static final char SEP = '\n';
    private static final String NULL_KEY = "\u0000";

    private Cursors() {
    }

    /** Vị trí trong danh sách; key null = bản ghi có khóa sắp xếp null (vd dueDate trống). */
    public record Position(String sort, String key, UUID id) {
        public Position {
            Objects.requireNonNull(sort, "sort");
            Objects.requireNonNull(id, "id");
        }
    }

    public static String encode(Position position) {
        String raw = position.sort() + SEP + (position.key() == null ? NULL_KEY : position.key()) + SEP
                + position.id();
        return Base64.getUrlEncoder().withoutPadding().encodeToString(raw.getBytes(StandardCharsets.UTF_8));
    }

    /** @return null nếu cursor null/rỗng (trang đầu) */
    public static Position decode(String cursor, String expectedSort) {
        if (cursor == null || cursor.isBlank()) {
            return null;
        }
        try {
            String raw = new String(Base64.getUrlDecoder().decode(cursor), StandardCharsets.UTF_8);
            String[] parts = raw.split(String.valueOf(SEP), -1);
            if (parts.length != 3 || !parts[0].equals(expectedSort)) {
                throw invalid();
            }
            String key = NULL_KEY.equals(parts[1]) ? null : parts[1];
            return new Position(parts[0], key, UUID.fromString(parts[2]));
        } catch (IllegalArgumentException e) {
            throw invalid();
        }
    }

    private static ValidationException invalid() {
        return FieldErrors.single(ErrorCode.VALIDATION_FAILED, "cursor", "INVALID_VALUE",
                "Con trỏ phân trang không hợp lệ.");
    }

    /** limit theo API_CONVENTIONS §7: mặc định 50, 1–200; ngoài khoảng → 400 (D79). */
    public static int limit(Integer requested) {
        if (requested == null) {
            return 50;
        }
        if (requested < 1 || requested > 200) {
            throw FieldErrors.single(ErrorCode.VALIDATION_FAILED, "limit", "SIZE", "limit phải từ 1 đến 200.");
        }
        return requested;
    }
}
