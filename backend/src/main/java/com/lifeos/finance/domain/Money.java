package com.lifeos.finance.domain;

import java.math.BigDecimal;
import java.util.Currency;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * Quy ước tiền của Finance (D100, D101; BACKEND_MASTER_PLAN §5 #8): PostgreSQL {@code NUMERIC(19,4)}, JSON là CHUỖI
 * thập phân, không bao giờ dùng số thực dấu phẩy động. Lớp này chỉ làm việc với {@link BigDecimal} dựng từ chuỗi;
 * không bao giờ làm tròn (giá trị có nhiều chữ số thập phân hơn tiền tệ cho phép bị từ chối, không cắt bớt).
 * <p>
 * Lỗi trả về dạng {@link Problem} (mã fieldError) để tầng service dựng {@code VALIDATION_FAILED}; domain không phụ thuộc
 * exception của ứng dụng.
 */
public final class Money {

    /** D100: |giá trị| &lt; 10^13. */
    public static final BigDecimal LIMIT = new BigDecimal("10000000000000");
    /** Tối đa 4 chữ số thập phân (cột NUMERIC(19,4)). */
    public static final int MAX_SCALE = 4;

    private static final Pattern DECIMAL = Pattern.compile("^-?[0-9]{1,20}(\\.[0-9]{1,20})?$");
    private static final Pattern CURRENCY = Pattern.compile("^[A-Z]{3}$");

    /** Mã fieldError: SCALE (quá số chữ số thập phân của tiền tệ), INVALID_VALUE (sai dạng), OUT_OF_RANGE. */
    public enum Problem {
        INVALID_VALUE, SCALE, OUT_OF_RANGE
    }

    /** Kết quả đọc: đúng một trong hai trường khác null. */
    public record Parsed(BigDecimal value, Problem problem) {
        public boolean ok() {
            return problem == null;
        }
    }

    private Money() {
    }

    /** Tiền tệ ISO 4217 có số chữ số thập phân xác định (loại các mã như XAU, XXX). */
    public static boolean isSupportedCurrency(String code) {
        return fractionDigits(code).isPresent();
    }

    public static Optional<Integer> fractionDigits(String code) {
        if (code == null || !CURRENCY.matcher(code).matches()) {
            return Optional.empty();
        }
        try {
            int digits = Currency.getInstance(code).getDefaultFractionDigits();
            return digits < 0 ? Optional.empty() : Optional.of(digits);
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
    }

    /**
     * Số tiền dương của giao dịch, chuyển khoản, ngân sách, mục tiêu (0 &lt; x &lt; 10^13) theo D101: số chữ số thập phân
     * (bỏ số 0 cuối) không vượt số chữ số của tiền tệ.
     */
    public static Parsed positive(String raw, String currency) {
        Parsed parsed = decimal(raw, currency);
        if (!parsed.ok()) {
            return parsed;
        }
        BigDecimal v = parsed.value();
        if (v.signum() <= 0 || v.compareTo(LIMIT) >= 0) {
            return new Parsed(null, Problem.OUT_OF_RANGE);
        }
        return parsed;
    }

    /** Số dư (đầu kỳ, sao kê): âm được, |x| &lt; 10^13. */
    public static Parsed signed(String raw, String currency) {
        Parsed parsed = decimal(raw, currency);
        if (!parsed.ok()) {
            return parsed;
        }
        if (parsed.value().abs().compareTo(LIMIT) >= 0) {
            return new Parsed(null, Problem.OUT_OF_RANGE);
        }
        return parsed;
    }

    /** Ngưỡng lọc số tiền (không biết tiền tệ): dương, tối đa 4 chữ số thập phân. */
    public static Parsed filter(String raw) {
        Parsed parsed = decimal(raw, null);
        if (parsed.ok() && (parsed.value().signum() <= 0 || parsed.value().compareTo(LIMIT) >= 0)) {
            return new Parsed(null, Problem.OUT_OF_RANGE);
        }
        return parsed;
    }

    /** Kiểm lại số chữ số thập phân của một giá trị đã lưu với tiền tệ khác (đổi tiền tệ/tài khoản). */
    public static boolean fitsCurrency(BigDecimal value, String currency) {
        int allowed = fractionDigits(currency).orElse(MAX_SCALE);
        return scaleOf(value) <= allowed;
    }

    /**
     * Chuỗi JSON của một số tiền: dạng thập phân thuần, bỏ số 0 vô nghĩa ở cuối ({@code "125000"}, {@code "19.99"},
     * {@code "10.5"}, {@code "-3"}) [PROPOSED]. Không làm tròn.
     */
    public static String format(BigDecimal value) {
        if (value == null) {
            return null;
        }
        if (value.signum() == 0) {
            return "0";
        }
        return value.stripTrailingZeros().toPlainString();
    }

    private static Parsed decimal(String raw, String currency) {
        if (raw == null || !DECIMAL.matcher(raw).matches()) {
            return new Parsed(null, Problem.INVALID_VALUE);
        }
        BigDecimal value = new BigDecimal(raw);
        int allowed = currency == null ? MAX_SCALE : Math.min(MAX_SCALE, fractionDigits(currency).orElse(MAX_SCALE));
        if (scaleOf(value) > allowed) {
            return new Parsed(null, Problem.SCALE);
        }
        return new Parsed(value, null);
    }

    private static int scaleOf(BigDecimal value) {
        return value.signum() == 0 ? 0 : Math.max(0, value.stripTrailingZeros().scale());
    }
}
