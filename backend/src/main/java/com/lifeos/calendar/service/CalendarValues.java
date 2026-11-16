package com.lifeos.calendar.service;

import com.lifeos.calendar.domain.EventRecurrence;
import com.lifeos.common.exception.ErrorCode;
import com.lifeos.common.exception.FieldErrors;
import com.lifeos.common.exception.NotFoundException;
import com.lifeos.common.exception.ValidationException;
import java.text.Normalizer;
import java.time.DateTimeException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeParseException;
import java.util.Map;

/** Kiểm giá trị dùng chung của module calendar (PHASE_4 §2, §6). Thông điệp không chứa giá trị người dùng. */
public final class CalendarValues {

    public static final int TITLE_MAX = 200;
    public static final int DESCRIPTION_MAX = 2000;
    public static final int LOCATION_MAX = 200;
    public static final int NOTE_MAX = 200;
    /** D146: tối đa 5000 sự kiện mỗi người dùng. */
    public static final int MAX_EVENTS = 5000;
    /** D125: cửa sổ truy vấn ≤ 366 ngày, ≤ 1000 lần xuất hiện. */
    public static final int MAX_WINDOW_DAYS = 366;
    public static final int MAX_OCCURRENCES = 1000;

    private CalendarValues() {
    }

    static String clean(String raw) {
        return raw == null ? null : Normalizer.normalize(raw, Normalizer.Form.NFC).strip();
    }

    static String title(String raw) {
        String title = clean(raw);
        if (title == null || title.isEmpty()) {
            throw invalid("title", "REQUIRED", "Tiêu đề là bắt buộc.");
        }
        if (title.codePointCount(0, title.length()) > TITLE_MAX) {
            throw invalid("title", "SIZE", "Tiêu đề tối đa " + TITLE_MAX + " ký tự.");
        }
        return title;
    }

    static String optionalText(String raw, String field, int max) {
        String text = clean(raw);
        if (text == null || text.isEmpty()) {
            return null;
        }
        if (text.codePointCount(0, text.length()) > max) {
            throw invalid(field, "SIZE", "Tối đa " + max + " ký tự.");
        }
        return text;
    }

    static ZoneId zone(String raw) {
        if (raw == null) {
            throw invalid("timezone", "REQUIRED", "Múi giờ là bắt buộc.");
        }
        try {
            if (!raw.contains("/") && !"UTC".equals(raw)) {
                throw new DateTimeException("not IANA region");
            }
            return ZoneId.of(raw);
        } catch (DateTimeException e) {
            throw new ValidationException(ErrorCode.CALENDAR_INVALID_TIMEZONE,
                    ErrorCode.CALENDAR_INVALID_TIMEZONE.title());
        }
    }

    static LocalDateTime localDateTime(String raw, String field) {
        if (raw == null) {
            return null;
        }
        try {
            return LocalDateTime.parse(raw);
        } catch (DateTimeParseException e) {
            throw invalid(field, "PATTERN", "Giờ địa phương theo dạng yyyy-MM-ddTHH:mm[:ss].");
        }
    }

    static LocalDate date(String raw, String field) {
        if (raw == null) {
            return null;
        }
        try {
            return LocalDate.parse(raw);
        } catch (DateTimeParseException e) {
            throw invalid(field, "PATTERN", "Ngày theo dạng yyyy-MM-dd.");
        }
    }

    static Instant instant(String raw, String field) {
        if (raw == null) {
            throw invalid(field, "REQUIRED", "Trường này là bắt buộc.");
        }
        try {
            return Instant.parse(raw);
        } catch (DateTimeParseException e) {
            throw invalid(field, "PATTERN", "Thời điểm theo ISO-8601 (UTC, có Z).");
        }
    }

    static EventRecurrence recurrence(Map<String, Object> raw) {
        if (raw == null) {
            return null;
        }
        try {
            return EventRecurrence.parse(raw);
        } catch (EventRecurrence.Invalid e) {
            ValidationException ex = new ValidationException(ErrorCode.CALENDAR_INVALID_RECURRENCE,
                    ErrorCode.CALENDAR_INVALID_RECURRENCE.title());
            ex.with(FieldErrors.PROPERTY, java.util.List.of(Map.of("field", "recurrence." + e.field(),
                    "code", "INVALID_VALUE", "message", "Giá trị không hợp lệ.")));
            throw ex;
        }
    }

    /**
     * Giờ địa phương → UTC theo D134: giờ không tồn tại (DST nhảy tiến) dời tới mốc hợp lệ kế tiếp; giờ lặp (DST lùi)
     * lấy lần xuất hiện ĐẦU TIÊN (offset sớm hơn).
     */
    public static Instant toInstant(LocalDateTime local, ZoneId zone) {
        return ZonedDateTime.ofLocal(local, zone, null).toInstant();
    }

    static ValidationException invalid(String field, String code, String message) {
        return FieldErrors.single(ErrorCode.VALIDATION_FAILED, field, code, message);
    }

    static NotFoundException notFound(ErrorCode code) {
        return new NotFoundException(code);
    }

    static ValidationException badReference(ErrorCode code) {
        return new ValidationException(code, code.title());
    }

    static ValidationException limitExceeded() {
        return new ValidationException(ErrorCode.LIMIT_EXCEEDED, ErrorCode.LIMIT_EXCEEDED.title());
    }
}
