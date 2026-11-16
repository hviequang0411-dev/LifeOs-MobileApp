package com.lifeos.task.service;

import com.lifeos.common.exception.ErrorCode;
import com.lifeos.common.exception.FieldErrors;
import com.lifeos.common.exception.ValidationException;
import com.lifeos.task.entity.TaskPriority;
import com.lifeos.task.entity.TaskStatus;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;

/**
 * Kiểm và chuyển đổi giá trị trường của Task (PRD §4.1, Q-03, Q-10; API_CONVENTIONS §2; D78). Lỗi → 400
 * VALIDATION_FAILED kèm fieldError. Dùng chung cho REST và sync.
 */
final class TaskValues {

    static final int TITLE_MAX = 200;
    static final int DESCRIPTION_MAX = 10_000;
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("uuuu-MM-dd")
            .withResolverStyle(ResolverStyle.STRICT);
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm")
            .withResolverStyle(ResolverStyle.STRICT);

    private TaskValues() {
    }

    /** Trim rồi kiểm 1–200 ký tự (D78). */
    static String title(String raw) {
        String title = raw == null ? "" : raw.trim();
        if (title.isEmpty()) {
            throw error("title", "REQUIRED", "Tiêu đề là bắt buộc.");
        }
        if (title.codePointCount(0, title.length()) > TITLE_MAX) {
            throw error("title", "SIZE", "Tiêu đề tối đa " + TITLE_MAX + " ký tự.");
        }
        return title;
    }

    /** Giữ nguyên (không trim, D78); null hợp lệ; tối đa 10.000 ký tự. */
    static String description(String description) {
        if (description != null && description.codePointCount(0, description.length()) > DESCRIPTION_MAX) {
            throw error("description", "SIZE", "Mô tả tối đa " + DESCRIPTION_MAX + " ký tự.");
        }
        return description;
    }

    /** null → mặc định OPEN (khi tạo). */
    static TaskStatus status(String raw, boolean allowDefault) {
        if (raw == null) {
            if (allowDefault) {
                return TaskStatus.OPEN;
            }
            throw error("status", "REQUIRED", "Trạng thái là bắt buộc.");
        }
        try {
            return TaskStatus.valueOf(raw);
        } catch (IllegalArgumentException e) {
            throw error("status", "INVALID_VALUE", "Trạng thái phải là OPEN hoặc DONE.");
        }
    }

    /** null → mặc định NONE (khi tạo). */
    static TaskPriority priority(String raw, boolean allowDefault) {
        if (raw == null) {
            if (allowDefault) {
                return TaskPriority.NONE;
            }
            throw error("priority", "REQUIRED", "Độ ưu tiên là bắt buộc.");
        }
        try {
            return TaskPriority.valueOf(raw);
        } catch (IllegalArgumentException e) {
            throw error("priority", "INVALID_VALUE", "Độ ưu tiên phải là NONE, LOW, MEDIUM hoặc HIGH.");
        }
    }

    /** "yyyy-MM-dd"; null hợp lệ. */
    static LocalDate dueDate(String raw) {
        if (raw == null) {
            return null;
        }
        try {
            return LocalDate.parse(raw, DATE);
        } catch (DateTimeParseException e) {
            throw error("dueDate", "PATTERN", "Ngày hạn theo dạng yyyy-MM-dd.");
        }
    }

    /** "HH:mm"; null hợp lệ. */
    static LocalTime dueTime(String raw) {
        if (raw == null) {
            return null;
        }
        try {
            return LocalTime.parse(raw, TIME);
        } catch (DateTimeParseException e) {
            throw error("dueTime", "PATTERN", "Giờ hạn theo dạng HH:mm.");
        }
    }

    /** dueTime chỉ khi có dueDate (D78: 400, fieldError dueTime/INVALID_VALUE, không tự xóa dueTime). */
    static void requireDateForTime(LocalDate dueDate, LocalTime dueTime) {
        if (dueTime != null && dueDate == null) {
            throw error("dueTime", "INVALID_VALUE", "Giờ hạn chỉ được đặt khi có ngày hạn.");
        }
    }

    private static ValidationException error(String field, String code, String message) {
        return FieldErrors.single(ErrorCode.VALIDATION_FAILED, field, code, message);
    }
}
