package com.lifeos.finance.service;

import com.lifeos.common.exception.ConflictException;
import com.lifeos.common.exception.ErrorCode;
import com.lifeos.common.exception.FieldErrors;
import com.lifeos.common.exception.NotFoundException;
import com.lifeos.common.exception.ValidationException;
import com.lifeos.common.util.Patch;
import com.lifeos.finance.domain.Money;
import java.math.BigDecimal;
import java.text.Normalizer;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.util.Set;
import java.util.UUID;

/**
 * Kiểm giá trị dùng chung của Finance (PHASE_3 §4–§6). Lỗi trường là 400 VALIDATION_FAILED + fieldErrors (D117, mã
 * REQUIRED | SIZE | PATTERN | INVALID_VALUE | SCALE | OUT_OF_RANGE | IMMUTABLE); lỗi nghiệp vụ dùng mã FINANCE_* (D114).
 * Thông điệp không chứa giá trị người dùng (SECURITY_RULES §11).
 */
public final class FinanceValues {

    public static final Set<String> ACCOUNT_TYPES = Set.of("CASH", "BANK", "E_WALLET", "CREDIT_CARD", "OTHER");
    public static final Set<String> KINDS = Set.of("INCOME", "EXPENSE");
    public static final Set<String> FREQUENCIES = Set.of("DAILY", "WEEKLY", "MONTHLY", "YEARLY");
    public static final String INCOME = "INCOME";
    public static final String EXPENSE = "EXPENSE";
    /** D108: occurredOn không sớm hơn 2000-01-01 và không muộn hơn hôm nay + 1 năm. */
    public static final LocalDate MIN_DATE = LocalDate.of(2000, 1, 1);
    public static final int NOTE_MAX = 500;
    public static final int QUERY_MAX = 100;

    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("uuuu-MM-dd")
            .withResolverStyle(ResolverStyle.STRICT);
    private static final DateTimeFormatter MONTH = DateTimeFormatter.ofPattern("uuuu-MM")
            .withResolverStyle(ResolverStyle.STRICT);

    private FinanceValues() {
    }

    /** NFC + cắt khoảng trắng đầu/cuối; null giữ null. */
    static String clean(String raw) {
        return raw == null ? null : Normalizer.normalize(raw, Normalizer.Form.NFC).strip();
    }

    static String name(String raw, int max) {
        String name = clean(raw);
        if (name == null || name.isEmpty()) {
            throw invalid("name", "REQUIRED", "Tên là bắt buộc.");
        }
        if (name.codePointCount(0, name.length()) > max) {
            throw invalid("name", "SIZE", "Tên tối đa " + max + " ký tự.");
        }
        return name;
    }

    static String note(String raw) {
        String text = clean(raw);
        if (text == null || text.isEmpty()) {
            return null;
        }
        if (text.codePointCount(0, text.length()) > NOTE_MAX) {
            throw invalid("note", "SIZE", "Tối đa " + NOTE_MAX + " ký tự.");
        }
        return text;
    }

    static String oneOf(String raw, Set<String> allowed, String field) {
        if (raw == null) {
            throw invalid(field, "REQUIRED", "Trường này là bắt buộc.");
        }
        if (!allowed.contains(raw)) {
            throw invalid(field, "INVALID_VALUE", "Giá trị không hợp lệ.");
        }
        return raw;
    }

    static String currency(String raw, String field) {
        if (raw == null) {
            throw invalid(field, "REQUIRED", "Tiền tệ là bắt buộc.");
        }
        if (!Money.isSupportedCurrency(raw)) {
            throw invalid(field, "INVALID_VALUE", "Mã tiền tệ ISO 4217 không hợp lệ.");
        }
        return raw;
    }

    /** Số tiền dương theo tiền tệ (D100, D101); không làm tròn. */
    static BigDecimal amount(String raw, String currency, String field) {
        if (raw == null) {
            throw invalid(field, "REQUIRED", "Số tiền là bắt buộc.");
        }
        return checked(Money.positive(raw, currency), field);
    }

    static BigDecimal signedAmount(String raw, String currency, String field) {
        if (raw == null) {
            throw invalid(field, "REQUIRED", "Số tiền là bắt buộc.");
        }
        return checked(Money.signed(raw, currency), field);
    }

    static BigDecimal filterAmount(String raw, String field) {
        return raw == null ? null : checked(Money.filter(raw), field);
    }

    /** Giá trị đã lưu phải hợp số chữ số thập phân của tiền tệ mới (đổi tiền tệ/tài khoản). */
    static void requireFits(BigDecimal value, String currency, String field) {
        if (!Money.fitsCurrency(value, currency)) {
            throw scale(field);
        }
    }

    private static BigDecimal checked(Money.Parsed parsed, String field) {
        if (parsed.ok()) {
            return parsed.value();
        }
        throw switch (parsed.problem()) {
            case SCALE -> scale(field);
            case OUT_OF_RANGE -> invalid(field, "OUT_OF_RANGE", "Số tiền nằm ngoài giới hạn cho phép.");
            default -> invalid(field, "INVALID_VALUE", "Số tiền là chuỗi thập phân, ví dụ \"125000\".");
        };
    }

    private static ValidationException scale(String field) {
        return invalid(field, "SCALE", "Số chữ số thập phân vượt quá mức của tiền tệ.");
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

    static LocalDate requiredDate(String raw, String field) {
        LocalDate date = date(raw, field);
        if (date == null) {
            throw invalid(field, "REQUIRED", "Ngày là bắt buộc.");
        }
        return date;
    }

    /** D108: ngày nghiệp vụ trong [2000-01-01, hôm nay + 1 năm]. */
    static LocalDate businessDate(String raw, String field, LocalDate today) {
        LocalDate date = requiredDate(raw, field);
        if (date.isBefore(MIN_DATE) || date.isAfter(today.plusYears(1))) {
            throw invalid(field, "OUT_OF_RANGE", "Ngày nằm ngoài khoảng cho phép.");
        }
        return date;
    }

    static YearMonth month(String raw, String field) {
        if (raw == null) {
            return null;
        }
        try {
            return YearMonth.parse(raw, MONTH);
        } catch (DateTimeParseException e) {
            throw invalid(field, "PATTERN", "Tháng theo dạng yyyy-MM.");
        }
    }

    /** Trường bất biến sau khi tạo: có mặt với giá trị khác hiện tại → VALIDATION_FAILED IMMUTABLE. */
    static <T> void immutable(Patch<T> patch, T current, String field) {
        if (patch.present() && !java.util.Objects.equals(patch.value(), current)) {
            throw invalid(field, "IMMUTABLE", "Không thể thay đổi trường này sau khi tạo.");
        }
    }

    static <T> T pick(Patch<T> patch, T current) {
        return patch.present() ? patch.value() : current;
    }

    static ValidationException invalid(String field, String code, String message) {
        return FieldErrors.single(ErrorCode.VALIDATION_FAILED, field, code, message);
    }

    static NotFoundException notFound(ErrorCode code) {
        return new NotFoundException(code);
    }

    /** Khoản góp không tồn tại: không có mã riêng trong D114, dùng mã chung NOT_FOUND [PROPOSED]. */
    static NotFoundException contributionNotFound() {
        return new NotFoundException(ErrorCode.NOT_FOUND);
    }

    /** Tham chiếu sai trong body/payload (không tồn tại, đã xóa, của user khác): 422 cùng một dạng (D115). */
    static ValidationException badReference(ErrorCode code) {
        return new ValidationException(code, code.title());
    }

    static ValidationException business(ErrorCode code) {
        return new ValidationException(code, code.title());
    }

    static ConflictException conflict(ErrorCode code) {
        return new ConflictException(code);
    }

    static boolean mentions(Throwable e, String constraint) {
        for (Throwable t = e; t != null; t = t.getCause()) {
            if (t.getMessage() != null && t.getMessage().contains(constraint)) {
                return true;
            }
        }
        return false;
    }

    static UUID requiredId(UUID id, String field) {
        if (id == null) {
            throw invalid(field, "REQUIRED", "Trường này là bắt buộc.");
        }
        return id;
    }
}
