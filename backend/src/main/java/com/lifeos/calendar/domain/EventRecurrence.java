package com.lifeos.calendar.domain;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * Quy tắc lặp của sự kiện (PHASE_4 D126, tập con RFC 5545): {@code freq ∈ DAILY|WEEKLY|MONTHLY|YEARLY}, {@code interval
 * 1–365}, {@code byWeekday} (WEEKLY, mã MO..SU), {@code byMonthDay} (MONTHLY, 1–31, ngày không tồn tại → ngày cuối tháng),
 * kết thúc bằng {@code until} (ngày, tính theo giờ địa phương của sự kiện) HOẶC {@code count} (1–1000) hoặc vô hạn.
 * <p>
 * Lần xuất hiện được TÍNH (D125), định danh bằng thời điểm bắt đầu gốc theo giờ địa phương. Mỗi "chu kỳ" (ngày/tuần bắt
 * đầu Thứ Hai/tháng/năm) tính từ chu kỳ chứa ngày bắt đầu chuỗi, cách nhau {@code interval}; lần xuất hiện trước ngày bắt
 * đầu chuỗi bị bỏ; {@code count} đếm từ lần đầu. Hợp đồng chung Java/Dart: {@code docs/api/fixtures/recurrence-vectors.json}.
 */
public record EventRecurrence(String freq, int interval, List<DayOfWeek> byWeekday, Integer byMonthDay,
        LocalDate until, Integer count) {

    public static final int MAX_COUNT = 1000;
    public static final int MAX_INTERVAL = 365;
    private static final Set<String> FREQS = Set.of("DAILY", "WEEKLY", "MONTHLY", "YEARLY");
    private static final List<String> DAY_CODES = List.of("MO", "TU", "WE", "TH", "FR", "SA", "SU");
    private static final Set<String> KEYS = Set.of("freq", "interval", "byWeekday", "byMonthDay", "until", "count");

    /** Quy tắc không hợp lệ; {@code field} là khóa sai (fieldError). */
    public static final class Invalid extends RuntimeException {
        private final String field;

        public Invalid(String field) {
            super(field);
            this.field = field;
        }

        public String field() {
            return field;
        }
    }

    /** Đọc và kiểm quy tắc từ JSON đã parse (REST hoặc payload sync). */
    public static EventRecurrence parse(Map<String, Object> raw) {
        for (String key : raw.keySet()) {
            if (!KEYS.contains(key)) {
                throw new Invalid(key);
            }
        }
        if (!(raw.get("freq") instanceof String freq) || !FREQS.contains(freq)) {
            throw new Invalid("freq");
        }
        int interval = 1;
        if (raw.get("interval") != null) {
            if (!(raw.get("interval") instanceof Number n) || !isInteger(n) || n.intValue() < 1
                    || n.intValue() > MAX_INTERVAL) {
                throw new Invalid("interval");
            }
            interval = n.intValue();
        }
        List<DayOfWeek> days = List.of();
        if (raw.get("byWeekday") != null) {
            if (!"WEEKLY".equals(freq) || !(raw.get("byWeekday") instanceof List<?> list) || list.isEmpty()) {
                throw new Invalid("byWeekday");
            }
            Set<DayOfWeek> set = new TreeSet<>();
            for (Object o : list) {
                if (!(o instanceof String code) || !DAY_CODES.contains(code) || !set.add(dayOf(code))) {
                    throw new Invalid("byWeekday");
                }
            }
            days = List.copyOf(set);
        }
        Integer byMonthDay = null;
        if (raw.get("byMonthDay") != null) {
            if (!"MONTHLY".equals(freq) || !(raw.get("byMonthDay") instanceof Number n) || !isInteger(n)
                    || n.intValue() < 1 || n.intValue() > 31) {
                throw new Invalid("byMonthDay");
            }
            byMonthDay = n.intValue();
        }
        LocalDate until = null;
        if (raw.get("until") != null) {
            try {
                until = LocalDate.parse((String) raw.get("until"));
            } catch (ClassCastException | DateTimeParseException e) {
                throw new Invalid("until");
            }
        }
        Integer count = null;
        if (raw.get("count") != null) {
            if (!(raw.get("count") instanceof Number n) || !isInteger(n) || n.intValue() < 1
                    || n.intValue() > MAX_COUNT) {
                throw new Invalid("count");
            }
            count = n.intValue();
        }
        if (until != null && count != null) {
            throw new Invalid("count");
        }
        return new EventRecurrence(freq, interval, days, byMonthDay, until, count);
    }

    /** Dạng JSON chuẩn (khóa vắng = mặc định), dùng để lưu cột jsonb và trả API. */
    public Map<String, Object> toMap() {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("freq", freq);
        map.put("interval", interval);
        if (!byWeekday.isEmpty()) {
            map.put("byWeekday", byWeekday.stream().map(d -> DAY_CODES.get(d.getValue() - 1)).toList());
        }
        if (byMonthDay != null) {
            map.put("byMonthDay", byMonthDay);
        }
        if (until != null) {
            map.put("until", until.toString());
        }
        if (count != null) {
            map.put("count", count);
        }
        return map;
    }

    public EventRecurrence withUntil(LocalDate newUntil) {
        return new EventRecurrence(freq, interval, byWeekday, byMonthDay, newUntil, null);
    }

    public EventRecurrence withCount(Integer newCount) {
        return new EventRecurrence(freq, interval, byWeekday, byMonthDay, null, newCount);
    }

    /**
     * Các lần xuất hiện (giờ bắt đầu địa phương) trong [from, to), tăng dần, tối đa {@code limit} phần tử.
     *
     * @param seriesStart giờ bắt đầu địa phương của lần đầu (sự kiện cả ngày: ngày lúc 00:00)
     */
    public List<LocalDateTime> between(LocalDateTime seriesStart, LocalDateTime from, LocalDateTime to, int limit) {
        List<LocalDateTime> out = new ArrayList<>();
        if (!to.isAfter(from) || limit <= 0) {
            return out;
        }
        long period = count == null ? Math.max(0, periodsBetween(seriesStart.toLocalDate(), from.toLocalDate())
                / interval - 1) : 0;
        int index = 0;
        while (true) {
            List<LocalDate> dates = datesOf(seriesStart.toLocalDate(), period * interval);
            LocalDate first = dates.get(0);
            if (first.atTime(seriesStart.toLocalTime()).compareTo(to) >= 0 || (until != null && first.isAfter(until))) {
                return out;
            }
            for (LocalDate date : dates) {
                LocalDateTime occurrence = date.atTime(seriesStart.toLocalTime());
                if (occurrence.isBefore(seriesStart)) {
                    continue;
                }
                if ((until != null && date.isAfter(until)) || (count != null && index >= count)
                        || occurrence.compareTo(to) >= 0) {
                    return out;
                }
                index++;
                if (!occurrence.isBefore(from)) {
                    out.add(occurrence);
                    if (out.size() >= limit) {
                        return out;
                    }
                }
            }
            period++;
        }
    }

    /** Lần xuất hiện cuối của chuỗi hữu hạn; null nếu vô hạn. */
    public LocalDateTime last(LocalDateTime seriesStart) {
        if (until == null && count == null) {
            return null;
        }
        LocalDateTime to = until != null ? until.plusDays(1).atStartOfDay() : LocalDateTime.MAX.minusYears(1);
        List<LocalDateTime> all = between(seriesStart, seriesStart, to, count != null ? count : Integer.MAX_VALUE);
        return all.isEmpty() ? null : all.get(all.size() - 1);
    }

    /** {@code candidate} có phải một lần xuất hiện của chuỗi không. */
    public boolean isOccurrence(LocalDateTime seriesStart, LocalDateTime candidate) {
        return between(seriesStart, candidate, candidate.plusSeconds(1), 1).contains(candidate);
    }

    /** Số lần xuất hiện bắt đầu trước {@code before}. */
    public int countBefore(LocalDateTime seriesStart, LocalDateTime before) {
        return between(seriesStart, seriesStart, before, Integer.MAX_VALUE).size();
    }

    private List<LocalDate> datesOf(LocalDate start, long steps) {
        return switch (freq) {
            case "DAILY" -> List.of(start.plusDays(steps));
            case "WEEKLY" -> {
                LocalDate weekStart = start.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)).plusWeeks(steps);
                Set<DayOfWeek> days = byWeekday.isEmpty() ? EnumSet.of(start.getDayOfWeek()) : EnumSet.copyOf(byWeekday);
                List<LocalDate> dates = new ArrayList<>();
                for (DayOfWeek d : days) {
                    dates.add(weekStart.plusDays(d.getValue() - 1L));
                }
                yield dates;
            }
            case "MONTHLY" -> {
                YearMonth month = YearMonth.from(start).plusMonths(steps);
                int day = byMonthDay != null ? byMonthDay : start.getDayOfMonth();
                yield List.of(month.atDay(Math.min(day, month.lengthOfMonth())));
            }
            default -> List.of(start.plusYears(steps));
        };
    }

    private long periodsBetween(LocalDate start, LocalDate day) {
        if (!day.isAfter(start)) {
            return 0;
        }
        return switch (freq) {
            case "DAILY" -> ChronoUnit.DAYS.between(start, day);
            case "WEEKLY" -> ChronoUnit.WEEKS.between(start.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)),
                    day);
            case "MONTHLY" -> ChronoUnit.MONTHS.between(YearMonth.from(start), YearMonth.from(day));
            default -> ChronoUnit.YEARS.between(start, day);
        };
    }

    private static DayOfWeek dayOf(String code) {
        return DayOfWeek.of(DAY_CODES.indexOf(code) + 1);
    }

    private static boolean isInteger(Number n) {
        return n instanceof Integer || n instanceof Long || n instanceof Short;
    }
}
