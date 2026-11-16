package com.lifeos.analytics.domain;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.temporal.TemporalAdjusters;

/**
 * Kỳ tổng hợp D192: ngày [00:00, 24:00) giờ địa phương; tuần bắt đầu theo weekStartDay; tháng lịch. Ranh giới UTC tính
 * bằng {@code atStartOfDay(zone)} nên ngày DST 23/25 giờ vẫn là một ngày. key = ngày bắt đầu kỳ (yyyy-MM-dd).
 */
public record Period(String type, LocalDate start, LocalDate end) {

    public static final String DAY = "DAY";
    public static final String WEEK = "WEEK";
    public static final String MONTH = "MONTH";

    public static Period day(LocalDate date) {
        return new Period(DAY, date, date);
    }

    public static Period week(LocalDate anyDay, DayOfWeek weekStart) {
        LocalDate start = anyDay.with(TemporalAdjusters.previousOrSame(weekStart));
        return new Period(WEEK, start, start.plusDays(6));
    }

    public static Period month(YearMonth month) {
        return new Period(MONTH, month.atDay(1), month.atEndOfMonth());
    }

    /** Kỳ liền trước cùng loại (so sánh kỳ trước, FR-INT-02). */
    public Period previous() {
        return switch (type) {
            case DAY -> day(start.minusDays(1));
            case WEEK -> new Period(WEEK, start.minusDays(7), start.minusDays(1));
            default -> month(YearMonth.from(start).minusMonths(1));
        };
    }

    public String key() {
        return start.toString();
    }

    public Instant startInstant(ZoneId zone) {
        return start.atStartOfDay(zone).toInstant();
    }

    /** Mốc kết thúc (loại trừ): 00:00 ngày sau ngày cuối kỳ. */
    public Instant endInstant(ZoneId zone) {
        return end.plusDays(1).atStartOfDay(zone).toInstant();
    }

    /** periodKey hợp lệ của review (D202): ngày bất kỳ cho DAY, ngày đầu tuần cho WEEK, ngày 1 cho MONTH. */
    public static boolean isPeriodStart(String type, LocalDate key, DayOfWeek weekStart) {
        return switch (type) {
            case DAY -> true;
            case WEEK -> key.getDayOfWeek() == weekStart;
            case MONTH -> key.getDayOfMonth() == 1;
            default -> false;
        };
    }

    /** Ngày bắt đầu bucket chứa {@code date} theo granularity day|week|month. */
    public static LocalDate bucketStart(LocalDate date, String granularity, DayOfWeek weekStart) {
        return switch (granularity) {
            case "week" -> date.with(TemporalAdjusters.previousOrSame(weekStart));
            case "month" -> date.withDayOfMonth(1);
            default -> date;
        };
    }

    public static LocalDate nextBucket(LocalDate bucket, String granularity) {
        return switch (granularity) {
            case "week" -> bucket.plusWeeks(1);
            case "month" -> bucket.plusMonths(1);
            default -> bucket.plusDays(1);
        };
    }
}
