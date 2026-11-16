package com.lifeos.note.service;

import com.lifeos.common.exception.ErrorCode;
import com.lifeos.common.exception.FieldErrors;
import com.lifeos.common.exception.ValidationException;
import java.text.Normalizer;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;

/** Quy tắc giá trị ghi chú/thẻ/liên kết (D180, D181, D184, D189), dùng chung REST và sync. */
public final class NoteValues {

    public static final int CONTENT_MAX = 50_000;
    static final int TITLE_MAX = 200;
    static final int TAG_NAME_MAX = 30;
    static final int MAX_TAGS_PER_NOTE = 20;
    static final int MAX_LINKS_PER_NOTE = 20;
    static final int MAX_PINNED = 10;
    static final int QUERY_MAX = 100;
    static final int SEARCH_OFFSET_MAX = 1000;
    static final Set<String> TARGET_TYPES = Set.of("TASK", "GOAL");
    private static final Pattern COLOR = Pattern.compile("^#[0-9A-Fa-f]{6}$");

    private NoteValues() {
    }

    /** Tiêu đề tùy chọn: NFC, bỏ khoảng trắng đầu/cuối, rỗng → null, ≤ 200 code point. */
    static String title(String raw) {
        String value = raw == null ? null : Normalizer.normalize(raw, Normalizer.Form.NFC).strip();
        if (value == null || value.isEmpty()) {
            return null;
        }
        if (value.codePointCount(0, value.length()) > TITLE_MAX) {
            throw invalid("title", "SIZE", "Tối đa " + TITLE_MAX + " ký tự.");
        }
        return value;
    }

    /** Nội dung văn bản thuần (D180): NFC, \r\n → \n, giữ khoảng trắng; ≤ 50 000 code point (D181). */
    static String content(String raw) {
        if (raw == null) {
            return "";
        }
        String value = Normalizer.normalize(raw.replace("\r\n", "\n"), Normalizer.Form.NFC);
        if (value.codePointCount(0, value.length()) > CONTENT_MAX) {
            throw invalid("content", "MAX_LENGTH", "Tối đa " + CONTENT_MAX + " ký tự.");
        }
        return value;
    }

    static String tagName(String raw) {
        String value = raw == null ? null : Normalizer.normalize(raw, Normalizer.Form.NFC).strip();
        if (value == null || value.isEmpty()) {
            throw invalid("name", "REQUIRED", "Tên thẻ là bắt buộc.");
        }
        if (value.codePointCount(0, value.length()) > TAG_NAME_MAX) {
            throw invalid("name", "SIZE", "Tối đa " + TAG_NAME_MAX + " ký tự.");
        }
        return value;
    }

    static String color(String raw) {
        if (raw == null) {
            return null;
        }
        if (!COLOR.matcher(raw).matches()) {
            throw invalid("color", "PATTERN", "Màu theo dạng #RRGGBB.");
        }
        return raw.toUpperCase(java.util.Locale.ROOT);
    }

    static String targetType(String raw) {
        if (raw == null) {
            throw invalid("targetType", "REQUIRED", "targetType là bắt buộc.");
        }
        if (!TARGET_TYPES.contains(raw)) {
            throw invalid("targetType", "INVALID_VALUE", "targetType phải là TASK hoặc GOAL.");
        }
        return raw;
    }

    static UUID requiredId(UUID id, String field) {
        if (id == null) {
            throw invalid(field, "REQUIRED", field + " là bắt buộc.");
        }
        return id;
    }

    static ValidationException invalid(String field, String code, String message) {
        return FieldErrors.single(ErrorCode.VALIDATION_FAILED, field, code, message);
    }

    static ValidationException reference(ErrorCode code) {
        return new ValidationException(code, code.title());
    }

    static ValidationException limitExceeded() {
        return new ValidationException(ErrorCode.LIMIT_EXCEEDED, ErrorCode.LIMIT_EXCEEDED.title());
    }

    static ValidationException tagLimit() {
        return new ValidationException(ErrorCode.NOTE_TAG_LIMIT_EXCEEDED, ErrorCode.NOTE_TAG_LIMIT_EXCEEDED.title());
    }
}
