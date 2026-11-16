package com.lifeos.routine.service;

import com.lifeos.common.exception.ErrorCode;
import com.lifeos.common.exception.FieldErrors;
import com.lifeos.common.exception.ValidationException;
import com.lifeos.routine.entity.Routine;
import java.text.Normalizer;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.http.HttpStatus;

/** Quy tắc giá trị Routine/bước/lượt (D240–D243, D252), dùng chung REST và sync. */
public final class RoutineValues {

    static final int NAME_MAX = 100;
    static final int MAX_ROUTINES = 50;
    static final int MAX_STEPS = 30;
    static final long POSITION_GAP = 1000;
    static final Set<String> KINDS = Set.of(Routine.DAILY, Routine.WEEKLY);
    static final Set<String> STATUSES = Set.of("DONE", "SKIPPED");
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("uuuu-MM-dd")
            .withResolverStyle(ResolverStyle.STRICT);
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm")
            .withResolverStyle(ResolverStyle.STRICT);

    private RoutineValues() {
    }

    static String text(String raw, String field) {
        String value = raw == null ? null : Normalizer.normalize(raw, Normalizer.Form.NFC).strip();
        if (value == null || value.isEmpty()) {
            throw invalid(field, "REQUIRED", "Trường này là bắt buộc.");
        }
        if (value.codePointCount(0, value.length()) > NAME_MAX) {
            throw invalid(field, "SIZE", "Tối đa " + NAME_MAX + " ký tự.");
        }
        return value;
    }

    /** kind ∈ {DAILY, WEEKLY}; WEEKLY cần weekdays 1–127, DAILY không có weekdays (D240) → 422 ROUTINE_INVALID_SCHEDULE. */
    static void schedule(String kind, Integer weekdays) {
        if (kind == null) {
            throw invalid("kind", "REQUIRED", "Loại lịch là bắt buộc.");
        }
        if (!KINDS.contains(kind)) {
            throw invalid("kind", "INVALID_VALUE", "Loại lịch phải là DAILY hoặc WEEKLY.");
        }
        boolean weekly = Routine.WEEKLY.equals(kind);
        if (weekly != (weekdays != null) || (weekdays != null && (weekdays < 1 || weekdays > 127))) {
            ValidationException ex = new ValidationException(ErrorCode.ROUTINE_INVALID_SCHEDULE,
                    HttpStatus.UNPROCESSABLE_CONTENT, ErrorCode.ROUTINE_INVALID_SCHEDULE.title());
            ex.with(FieldErrors.PROPERTY, List.of(Map.of("field", "weekdays", "code", "INVALID_VALUE", "message",
                    "WEEKLY cần weekdays 1–127; DAILY không có weekdays.")));
            throw ex;
        }
    }

    static Integer duration(Integer minutes) {
        if (minutes != null && (minutes < 1 || minutes > 240)) {
            throw invalid("durationMinutes", "SIZE", "Thời lượng từ 1 đến 240 phút.");
        }
        return minutes;
    }

    static int totalSteps(Integer value) {
        if (value == null) {
            throw invalid("totalSteps", "REQUIRED", "totalSteps là bắt buộc.");
        }
        if (value < 1 || value > MAX_STEPS) {
            throw invalid("totalSteps", "SIZE", "totalSteps từ 1 đến " + MAX_STEPS + ".");
        }
        return value;
    }

    static String status(String raw) {
        if (raw == null) {
            throw invalid("status", "REQUIRED", "Trạng thái là bắt buộc.");
        }
        if (!STATUSES.contains(raw)) {
            throw invalid("status", "INVALID_VALUE", "Trạng thái phải là DONE hoặc SKIPPED.");
        }
        return raw;
    }

    static LocalDate date(String raw, String field) {
        if (raw == null) {
            throw invalid(field, "REQUIRED", "Trường này là bắt buộc.");
        }
        try {
            return LocalDate.parse(raw, DATE);
        } catch (DateTimeParseException e) {
            throw invalid(field, "PATTERN", "Ngày theo dạng yyyy-MM-dd.");
        }
    }

    static LocalTime time(String raw) {
        if (raw == null) {
            return null;
        }
        try {
            return LocalTime.parse(raw, TIME);
        } catch (DateTimeParseException e) {
            throw invalid("reminderTime", "PATTERN", "Giờ theo dạng HH:mm.");
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

    static ValidationException limitExceeded() {
        return new ValidationException(ErrorCode.LIMIT_EXCEEDED, ErrorCode.LIMIT_EXCEEDED.title());
    }

    static ValidationException reference(ErrorCode code) {
        return new ValidationException(code, code.title());
    }
}
