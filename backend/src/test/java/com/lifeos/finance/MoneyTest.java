package com.lifeos.finance;

import static org.assertj.core.api.Assertions.assertThat;

import com.lifeos.finance.domain.Money;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

/** Quy ước tiền (D100, D101): chuỗi thập phân → BigDecimal, không làm tròn, không số thực. */
class MoneyTest {

    @Test
    void Fin_01_decimalArithmeticIsExact() {
        BigDecimal sum = BigDecimal.ZERO;
        for (int i = 0; i < 1000; i++) {
            sum = sum.add(Money.positive("0.1", "USD").value()).add(Money.positive("0.2", "USD").value());
        }
        assertThat(sum).isEqualByComparingTo("300");
        assertThat(Money.format(sum)).isEqualTo("300");
        assertThat(Money.positive("9999999999999.9999", "BHD").problem()).isEqualTo(Money.Problem.SCALE);
        assertThat(Money.positive("9999999999999.999", "BHD").value()).isEqualByComparingTo("9999999999999.999");
        assertThat(Money.positive("10000000000000", "VND").problem()).isEqualTo(Money.Problem.OUT_OF_RANGE);
        assertThat(Money.positive("0", "VND").problem()).isEqualTo(Money.Problem.OUT_OF_RANGE);
        assertThat(Money.positive("-5", "VND").problem()).isEqualTo(Money.Problem.OUT_OF_RANGE);
        assertThat(Money.signed("-9999999999999", "VND").value()).isEqualByComparingTo("-9999999999999");
        assertThat(Money.positive("1e5", "VND").problem()).isEqualTo(Money.Problem.INVALID_VALUE);
        assertThat(Money.positive(" 5", "VND").problem()).isEqualTo(Money.Problem.INVALID_VALUE);
        assertThat(Money.positive("5.", "VND").problem()).isEqualTo(Money.Problem.INVALID_VALUE);
        assertThat(Money.format(new BigDecimal("125000.0000"))).isEqualTo("125000");
        assertThat(Money.format(new BigDecimal("19.9900"))).isEqualTo("19.99");
        assertThat(Money.format(new BigDecimal("-3.5000"))).isEqualTo("-3.5");
        assertThat(Money.format(new BigDecimal("0.0000"))).isEqualTo("0");
    }

    @Test
    void Fin_02_scaleFollowsTheCurrencyAndIsNeverRounded() {
        assertThat(Money.positive("10.5", "VND").problem()).isEqualTo(Money.Problem.SCALE);
        assertThat(Money.positive("10.555", "USD").problem()).isEqualTo(Money.Problem.SCALE);
        assertThat(Money.positive("10.55", "USD").value()).isEqualByComparingTo("10.55");
        assertThat(Money.positive("10.00", "VND").value()).as("số 0 cuối không phải chữ số có nghĩa")
                .isEqualByComparingTo("10");
        assertThat(Money.positive("1.5", "JPY").problem()).isEqualTo(Money.Problem.SCALE);
        assertThat(Money.positive("1.12345", "KWD").problem()).isEqualTo(Money.Problem.SCALE);
        assertThat(Money.fitsCurrency(new BigDecimal("10.5000"), "VND")).isFalse();
        assertThat(Money.fitsCurrency(new BigDecimal("10.0000"), "VND")).isTrue();
        assertThat(Money.isSupportedCurrency("VND")).isTrue();
        assertThat(Money.isSupportedCurrency("XAU")).as("không có số chữ số thập phân xác định").isFalse();
        assertThat(Money.isSupportedCurrency("ABC")).isFalse();
        assertThat(Money.isSupportedCurrency("vnd")).isFalse();
    }
}
