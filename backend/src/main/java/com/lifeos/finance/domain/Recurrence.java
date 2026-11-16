package com.lifeos.finance.domain;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Objects;

/**
 * Quy tắc lặp của giao dịch định kỳ (PHASE_3 §7, D110). Lần xuất hiện thứ k (k ≥ 0) luôn tính TỪ {@code startOn}, không
 * cộng dồn từ lần trước, nên không trôi ngày:
 * <ul>
 *   <li>DAILY: startOn + k·n ngày; WEEKLY: + k·n tuần (giữ thứ).</li>
 *   <li>MONTHLY: + k·n tháng; tháng không có ngày đó dùng ngày cuối tháng (31/1 → 28 hoặc 29/2 → 31/3).</li>
 *   <li>YEARLY: + k·n năm (29/2 → 28/2 năm không nhuận, về lại 29/2 năm nhuận).</li>
 * </ul>
 */
public record Recurrence(String frequency, int interval, LocalDate startOn) {

    public static final String DAILY = "DAILY";
    public static final String WEEKLY = "WEEKLY";
    public static final String MONTHLY = "MONTHLY";
    public static final String YEARLY = "YEARLY";

    public Recurrence {
        Objects.requireNonNull(frequency, "frequency");
        Objects.requireNonNull(startOn, "startOn");
        if (interval < 1) {
            throw new IllegalArgumentException("interval");
        }
        if (!DAILY.equals(frequency) && !WEEKLY.equals(frequency) && !MONTHLY.equals(frequency)
                && !YEARLY.equals(frequency)) {
            throw new IllegalArgumentException("frequency");
        }
    }

    /** Ngày của lần xuất hiện thứ k. */
    public LocalDate occurrence(long k) {
        if (k < 0) {
            throw new IllegalArgumentException("k");
        }
        long steps = k * interval;
        return switch (frequency) {
            case DAILY -> startOn.plusDays(steps);
            case WEEKLY -> startOn.plusWeeks(steps);
            case MONTHLY -> startOn.plusMonths(steps);
            default -> startOn.plusYears(steps);
        };
    }

    /** Lần xuất hiện đầu tiên có ngày ≥ {@code day} (startOn nếu day ≤ startOn). */
    public LocalDate firstOnOrAfter(LocalDate day) {
        if (!day.isAfter(startOn)) {
            return startOn;
        }
        ChronoUnit unit = switch (frequency) {
            case DAILY -> ChronoUnit.DAYS;
            case WEEKLY -> ChronoUnit.WEEKS;
            case MONTHLY -> ChronoUnit.MONTHS;
            default -> ChronoUnit.YEARS;
        };
        // Ước lượng thấp hơn hoặc bằng chỉ số đúng (between làm tròn xuống; trừ thêm 1 cho kẹp cuối tháng), rồi tiến.
        long k = Math.max(0, unit.between(startOn, day) / interval - 1);
        LocalDate date = occurrence(k);
        while (date.isBefore(day)) {
            k++;
            date = occurrence(k);
        }
        return date;
    }

    /** Lần xuất hiện đầu tiên có ngày &gt; {@code day}. */
    public LocalDate firstAfter(LocalDate day) {
        return firstOnOrAfter(day.plusDays(1));
    }
}
