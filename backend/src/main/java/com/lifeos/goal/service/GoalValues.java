package com.lifeos.goal.service;

import com.lifeos.common.exception.ErrorCode;
import com.lifeos.common.exception.FieldErrors;
import com.lifeos.common.exception.ValidationException;
import com.lifeos.goal.domain.GoalStatus;
import java.text.Normalizer;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.util.UUID;

/** Quy tắc giá trị Goal/Milestone (FR-GOAL-01/02, D187, D188), dùng chung REST và sync. */
final class GoalValues {

    static final int TITLE_MAX = 200;
    static final int DESCRIPTION_MAX = 2000;
    static final int MAX_MILESTONES = 20;
    static final long POSITION_GAP = 1000;
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("uuuu-MM-dd")
            .withResolverStyle(ResolverStyle.STRICT);

    private GoalValues() {
    }

    static String title(String raw) {
        String value = nfc(raw);
        if (value == null || value.isEmpty()) {
            throw invalid("title", "REQUIRED", "Tiêu đề là bắt buộc.");
        }
        if (value.codePointCount(0, value.length()) > TITLE_MAX) {
            throw invalid("title", "SIZE", "Tối đa " + TITLE_MAX + " ký tự.");
        }
        return value;
    }

    static String description(String raw) {
        String value = nfc(raw);
        if (value == null || value.isEmpty()) {
            return null;
        }
        if (value.codePointCount(0, value.length()) > DESCRIPTION_MAX) {
            throw invalid("description", "SIZE", "Tối đa " + DESCRIPTION_MAX + " ký tự.");
        }
        return value;
    }

    static LocalDate date(String raw, String field) {
        if (raw == null) {
            return null;
        }
        try {
            return LocalDate.parse(raw, DATE);
        } catch (DateTimeParseException e) {
            throw invalid(field, "PATTERN", "Ngày theo dạng yyyy-MM-dd.");
        }
    }

    static void range(LocalDate startOn, LocalDate targetOn) {
        if (startOn != null && targetOn != null && targetOn.isBefore(startOn)) {
            throw invalid("targetOn", "INVALID_VALUE", "targetOn không được trước startOn.");
        }
    }

    static String status(String raw) {
        if (raw == null) {
            throw invalid("status", "REQUIRED", "Trạng thái là bắt buộc.");
        }
        if (!GoalStatus.ALL.contains(raw)) {
            throw invalid("status", "INVALID_VALUE", "Trạng thái không hợp lệ.");
        }
        return raw;
    }

    static void transition(String from, String to) {
        if (!GoalStatus.allowed(from, to)) {
            throw new ValidationException(ErrorCode.GOAL_INVALID_STATUS_TRANSITION,
                    ErrorCode.GOAL_INVALID_STATUS_TRANSITION.title());
        }
    }

    static UUID requiredId(UUID id, String field) {
        if (id == null) {
            throw invalid(field, "REQUIRED", field + " là bắt buộc.");
        }
        return id;
    }

    static void unchanged(boolean present, Object incoming, Object current, String field) {
        if (present && !current.equals(incoming)) {
            throw invalid(field, "INVALID_VALUE", field + " không đổi được.");
        }
    }

    static ValidationException invalid(String field, String code, String message) {
        return FieldErrors.single(ErrorCode.VALIDATION_FAILED, field, code, message);
    }

    static ValidationException reference(ErrorCode code) {
        return new ValidationException(code, code.title());
    }

    private static String nfc(String raw) {
        return raw == null ? null : Normalizer.normalize(raw, Normalizer.Form.NFC).strip();
    }
}
