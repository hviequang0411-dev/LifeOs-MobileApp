package com.lifeos.finance.domain;

import java.util.List;

/**
 * Danh mục hệ thống mặc định (PHASE_3 §5.1, D113). {@code systemKey} chỉ để nhận diện (job bù không tạo trùng); người
 * dùng sửa tên/lưu trữ như danh mục thường. Hai danh mục kỹ thuật {@code ADJUSTMENT} ("Điều chỉnh", một cho mỗi loại)
 * được tạo sẵn cùng các danh mục khác để client offline có ngay [PROPOSED].
 */
public final class DefaultCategories {

    public static final String ADJUSTMENT = "ADJUSTMENT";

    public record Entry(String systemKey, String kind, String name) {
    }

    public static final List<Entry> ALL = List.of(
            new Entry("SALARY", "INCOME", "Lương"),
            new Entry("BONUS", "INCOME", "Thưởng"),
            new Entry("INVESTMENT_INCOME", "INCOME", "Thu nhập đầu tư"),
            new Entry("SIDE_INCOME", "INCOME", "Làm thêm"),
            new Entry("OTHER_INCOME", "INCOME", "Thu khác"),
            new Entry("FOOD", "EXPENSE", "Ăn uống"),
            new Entry("TRANSPORT", "EXPENSE", "Di chuyển"),
            new Entry("HOUSING", "EXPENSE", "Nhà ở"),
            new Entry("EDUCATION", "EXPENSE", "Giáo dục"),
            new Entry("ENTERTAINMENT", "EXPENSE", "Giải trí"),
            new Entry("SHOPPING", "EXPENSE", "Mua sắm"),
            new Entry("HEALTH", "EXPENSE", "Sức khỏe"),
            new Entry("OTHER_EXPENSE", "EXPENSE", "Chi khác"),
            new Entry(ADJUSTMENT, "INCOME", "Điều chỉnh"),
            new Entry(ADJUSTMENT, "EXPENSE", "Điều chỉnh"));

    private DefaultCategories() {
    }
}
