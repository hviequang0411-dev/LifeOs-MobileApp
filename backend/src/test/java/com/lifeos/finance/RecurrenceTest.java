package com.lifeos.finance;

import static org.assertj.core.api.Assertions.assertThat;

import com.lifeos.finance.domain.Recurrence;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import org.junit.jupiter.api.Test;

/** Quy tắc ngày của giao dịch định kỳ (PHASE_3 §7, FIN-10): tính từ startOn, kẹp cuối tháng, 29/2. */
class RecurrenceTest {

    private static List<LocalDate> first(Recurrence r, int n) {
        List<LocalDate> out = new ArrayList<>();
        for (int k = 0; k < n; k++) {
            out.add(r.occurrence(k));
        }
        return out;
    }

    @Test
    void Fin_10_monthlyKeepsTheDayOfMonthAndClampsToMonthEnd() {
        Recurrence r = new Recurrence("MONTHLY", 1, LocalDate.of(2027, 1, 31));
        assertThat(first(r, 4)).containsExactly(LocalDate.of(2027, 1, 31), LocalDate.of(2027, 2, 28),
                LocalDate.of(2027, 3, 31), LocalDate.of(2027, 4, 30));
        Recurrence leap = new Recurrence("MONTHLY", 1, LocalDate.of(2028, 1, 31));
        assertThat(leap.occurrence(1)).isEqualTo(LocalDate.of(2028, 2, 29));
        assertThat(leap.occurrence(2)).as("không trôi về 29").isEqualTo(LocalDate.of(2028, 3, 31));
        Recurrence quarterly = new Recurrence("MONTHLY", 3, LocalDate.of(2026, 11, 30));
        assertThat(first(quarterly, 3)).containsExactly(LocalDate.of(2026, 11, 30), LocalDate.of(2027, 2, 28),
                LocalDate.of(2027, 5, 30));
    }

    @Test
    void Fin_10_yearlyOnLeapDayFallsBackTo28ThenReturns() {
        Recurrence r = new Recurrence("YEARLY", 1, LocalDate.of(2028, 2, 29));
        assertThat(first(r, 5)).containsExactly(LocalDate.of(2028, 2, 29), LocalDate.of(2029, 2, 28),
                LocalDate.of(2030, 2, 28), LocalDate.of(2031, 2, 28), LocalDate.of(2032, 2, 29));
    }

    @Test
    void Fin_10_weeklyAndDailyKeepTheirStride() {
        Recurrence weekly = new Recurrence("WEEKLY", 2, LocalDate.of(2026, 11, 2));
        assertThat(first(weekly, 3)).containsExactly(LocalDate.of(2026, 11, 2), LocalDate.of(2026, 11, 16),
                LocalDate.of(2026, 11, 30));
        Recurrence daily = new Recurrence("DAILY", 3, LocalDate.of(2026, 12, 30));
        assertThat(first(daily, 3)).containsExactly(LocalDate.of(2026, 12, 30), LocalDate.of(2027, 1, 2),
                LocalDate.of(2027, 1, 5));
    }

    /** firstOnOrAfter/firstAfter khớp phép duyệt tuần tự trên dữ liệu ngẫu nhiên (mô hình tham chiếu đơn giản). */
    @Test
    void Fin_10_firstOnOrAfterMatchesALinearScan() {
        Random random = new Random(42);
        String[] frequencies = {"DAILY", "WEEKLY", "MONTHLY", "YEARLY"};
        for (int i = 0; i < 2000; i++) {
            LocalDate start = LocalDate.of(2000, 1, 1).plusDays(random.nextInt(12000));
            Recurrence r = new Recurrence(frequencies[random.nextInt(4)], 1 + random.nextInt(12), start);
            LocalDate day = start.plusDays(random.nextInt(4000) - 200);
            LocalDate expected = null;
            for (int k = 0; ; k++) {
                LocalDate d = r.occurrence(k);
                if (!d.isBefore(day)) {
                    expected = d;
                    break;
                }
            }
            assertThat(r.firstOnOrAfter(day)).as(r + " from " + day).isEqualTo(expected);
            assertThat(r.firstAfter(expected)).isAfter(expected);
        }
    }
}
