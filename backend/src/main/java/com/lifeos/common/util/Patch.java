package com.lifeos.common.util;

import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Giá trị của một trường trong JSON Merge Patch (RFC 7396, API_CONVENTIONS §4): vắng mặt = không đổi; có mặt với
 * null = xóa giá trị (chỉ hợp lệ với trường cho phép null — service quyết định); có mặt với giá trị = đặt giá trị.
 * <p>
 * Trạng thái thứ tư, chỉ dùng cho payload sync: có mặt nhưng SAI KIỂU JSON ({@link #invalid}). Lỗi được hoãn đến lúc
 * service đọc {@link #value()} — tức là ở bước "validation giá trị", SAU bước kiểm tồn tại và version (thứ tự D96) —
 * nên UPDATE sai version kèm trường sai kiểu vẫn ra CONFLICT như mọi trường hợp lệch version khác.
 */
public final class Patch<T> {

    private static final Patch<?> ABSENT = new Patch<>(false, null, null);

    private final boolean present;
    private final T value;
    private final Supplier<? extends RuntimeException> error;

    private Patch(boolean present, T value, Supplier<? extends RuntimeException> error) {
        this.present = present;
        this.value = value;
        this.error = error;
    }

    @SuppressWarnings("unchecked")
    public static <T> Patch<T> absent() {
        return (Patch<T>) ABSENT;
    }

    public static <T> Patch<T> of(T value) {
        return new Patch<>(true, value, null);
    }

    /** Có mặt nhưng không đọc được thành kiểu mong đợi; {@link #value()} ném lỗi do {@code error} tạo. */
    public static <T> Patch<T> invalid(Supplier<? extends RuntimeException> error) {
        return new Patch<>(true, null, Objects.requireNonNull(error));
    }

    public boolean present() {
        return present;
    }

    /** null nếu vắng mặt hoặc null tường minh; ném lỗi kiểu nếu {@link #invalid}. */
    public T value() {
        if (error != null) {
            throw error.get();
        }
        return value;
    }

    public boolean isExplicitNull() {
        return present && error == null && value == null;
    }

    public void ifPresent(Consumer<T> action) {
        if (present) {
            action.accept(value());
        }
    }

    @Override
    public boolean equals(Object o) {
        return this == o || (o instanceof Patch<?> other && error == null && other.error == null
                && present == other.present && Objects.equals(value, other.value));
    }

    @Override
    public int hashCode() {
        return Objects.hash(present, value);
    }

    @Override
    public String toString() {
        // Không in giá trị (có thể là dữ liệu người dùng, SECURITY_RULES §11.1).
        return !present ? "Patch[absent]" : error != null ? "Patch[invalid]" : "Patch[present]";
    }
}
