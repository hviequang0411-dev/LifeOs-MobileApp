package com.lifeos.habit.service;

import com.lifeos.common.exception.ErrorCode;
import com.lifeos.common.exception.FieldErrors;
import com.lifeos.common.exception.ValidationException;
import com.lifeos.habit.domain.HabitSchedule;
import java.text.Normalizer;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import org.springframework.http.HttpStatus;

/**
 * Quy tắc giá trị của Habit/check-in (D230, D232, D252), dùng chung REST và sync (SECURITY_RULES §8.3). Chuỗi chuẩn
 * hóa NFC rồi cắt khoảng trắng; ngày "yyyy-MM-dd", giờ "HH:mm".
 */
public final class HabitValues {

    static final int NAME_MAX = 100;
    static final int DESCRIPTION_MAX = 500;
    static final int ICON_MAX = 30;
    static final int NOTE_MAX = 200;
    static final Set<String> SCHEDULE_TYPES = Set.of(HabitSchedule.DAILY, HabitSchedule.WEEKDAYS,
            HabitSchedule.TIMES_PER_WEEK, HabitSchedule.EVERY_N_DAYS);
    static final Set<String> CHECKIN_STATUSES = Set.of("DONE", "SKIPPED");
    private static final Pattern COLOR = Pattern.compile("^#[0-9A-Fa-f]{6}$");
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("uuuu-MM-dd")
            .withResolverStyle(ResolverStyle.STRICT);
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm")
            .withResolverStyle(ResolverStyle.STRICT);

    private HabitValues() {
    }

    /** NFC + cắt khoảng trắng đầu/cuối (D252); null giữ null. */
    static String clean(String raw) {
        return raw == null ? null : Normalizer.normalize(raw, Normalizer.Form.NFC).strip();
    }

    static String name(String raw) {
        String name = clean(raw);
        if (name == null || name.isEmpty()) {
            throw invalid("name", "REQUIRED", "Tên là bắt buộc.");
        }
        if (name.codePointCount(0, name.length()) > NAME_MAX) {
            throw invalid("name", "SIZE", "Tên tối đa " + NAME_MAX + " ký tự.");
        }
        return name;
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

    static String color(String raw) {
        if (raw == null) {
            return null;
        }
        if (!COLOR.matcher(raw).matches()) {
            throw invalid("color", "PATTERN", "Màu theo dạng #RRGGBB.");
        }
        return raw.toUpperCase();
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

    static LocalTime time(String raw, String field) {
        if (raw == null) {
            return null;
        }
        try {
            return LocalTime.parse(raw, TIME);
        } catch (DateTimeParseException e) {
            throw invalid(field, "PATTERN", "Giờ theo dạng HH:mm.");
        }
    }

    /**
     * Lịch (D230): loại hợp lệ; trường của đúng loại có mặt và trong khoảng; trường của loại khác phải null; endOn ≥
     * startOn. Sai → 422 HABIT_INVALID_SCHEDULE (kèm fieldError chỉ trường sai).
     */
    static HabitSchedule schedule(String type, Integer weekdays, Integer timesPerWeek, Integer intervalDays,
            LocalDate startOn, LocalDate endOn) {
        if (type == null) {
            throw invalid("scheduleType", "REQUIRED", "Loại lịch là bắt buộc.");
        }
        if (!SCHEDULE_TYPES.contains(type)) {
            throw invalid("scheduleType", "INVALID_VALUE", "Loại lịch không hợp lệ.");
        }
        requireFor("weekdays", weekdays, HabitSchedule.WEEKDAYS.equals(type), 1, 127);
        requireFor("timesPerWeek", timesPerWeek, HabitSchedule.TIMES_PER_WEEK.equals(type), 1, 7);
        requireFor("intervalDays", intervalDays, HabitSchedule.EVERY_N_DAYS.equals(type), 2, 30);
        if (endOn != null && endOn.isBefore(startOn)) {
            throw schedule("endOn", "endOn phải từ startOn trở đi.");
        }
        return new HabitSchedule(type, weekdays, timesPerWeek, intervalDays, startOn, endOn);
    }

    private static void requireFor(String field, Integer value, boolean needed, int min, int max) {
        if (needed && value == null) {
            throw schedule(field, "Thiếu " + field + " cho loại lịch này.");
        }
        if (!needed && value != null) {
            throw schedule(field, field + " không dùng cho loại lịch này.");
        }
        if (value != null && (value < min || value > max)) {
            throw schedule(field, field + " phải từ " + min + " đến " + max + ".");
        }
    }

    static String status(String raw) {
        if (raw == null) {
            throw invalid("status", "REQUIRED", "Trạng thái là bắt buộc.");
        }
        if (!CHECKIN_STATUSES.contains(raw)) {
            throw invalid("status", "INVALID_VALUE", "Trạng thái phải là DONE hoặc SKIPPED.");
        }
        return raw;
    }

    static ValidationException invalid(String field, String code, String message) {
        return FieldErrors.single(ErrorCode.VALIDATION_FAILED, field, code, message);
    }

    private static ValidationException schedule(String field, String message) {
        ValidationException ex = new ValidationException(ErrorCode.HABIT_INVALID_SCHEDULE,
                HttpStatus.UNPROCESSABLE_CONTENT, ErrorCode.HABIT_INVALID_SCHEDULE.title());
        ex.with(FieldErrors.PROPERTY, List.of(Map.of("field", field, "code", "INVALID_VALUE", "message", message)));
        return ex;
    }
}
