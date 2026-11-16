package com.lifeos.analytics.service;

import com.lifeos.common.exception.ErrorCode;
import com.lifeos.common.exception.FieldErrors;
import com.lifeos.common.exception.ValidationException;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.time.temporal.ChronoUnit;
import java.util.Set;

/** Quy tắc tham số của analytics/review (D192, D208). */
final class AnalyticsValues {

    static final int MAX_DAYS = 366;
    static final int MAX_MONTHS = 24;
    static final Set<String> GRANULARITIES = Set.of("day", "week", "month");
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("uuuu-MM-dd")
            .withResolverStyle(ResolverStyle.STRICT);
    private static final DateTimeFormatter MONTH = DateTimeFormatter.ofPattern("uuuu-MM")
            .withResolverStyle(ResolverStyle.STRICT);

    private AnalyticsValues() {
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

    static LocalDate required(String raw, String field) {
        LocalDate d = date(raw, field);
        if (d == null) {
            throw invalid(field, "REQUIRED", field + " là bắt buộc.");
        }
        return d;
    }

    static YearMonth month(String raw) {
        if (raw == null) {
            return null;
        }
        try {
            return YearMonth.parse(raw, MONTH);
        } catch (DateTimeParseException e) {
            throw invalid("month", "PATTERN", "Tháng theo dạng yyyy-MM.");
        }
    }

    static String granularity(String raw) {
        if (raw == null) {
            return "day";
        }
        if (!GRANULARITIES.contains(raw)) {
            throw invalid("granularity", "INVALID_VALUE", "granularity phải là day, week hoặc month.");
        }
        return raw;
    }

    /** D208: day (hoặc phạm vi theo ngày) ≤ 366 ngày; week/month &lt; 24 tháng; vượt → 422. */
    static void range(LocalDate from, LocalDate to, String granularity, boolean dailyOnly) {
        if (to.isBefore(from)) {
            throw invalid("to", "INVALID_VALUE", "to không được trước from.");
        }
        boolean tooLarge = dailyOnly || "day".equals(granularity)
                ? ChronoUnit.DAYS.between(from, to) + 1 > MAX_DAYS
                : !to.isBefore(from.plusMonths(MAX_MONTHS));
        if (tooLarge) {
            throw new ValidationException(ErrorCode.ANALYTICS_RANGE_TOO_LARGE,
                    ErrorCode.ANALYTICS_RANGE_TOO_LARGE.title());
        }
    }

    static ValidationException invalid(String field, String code, String message) {
        return FieldErrors.single(ErrorCode.VALIDATION_FAILED, field, code, message);
    }
}
