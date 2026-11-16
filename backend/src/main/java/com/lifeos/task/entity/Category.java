package com.lifeos.task.entity;

import com.lifeos.common.persistence.SyncedEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.util.Objects;
import java.util.UUID;

/**
 * Danh mục Task (FR-CT-01): entity đồng bộ thuộc module task (D25), id do client tạo. Tên duy nhất theo user không phân
 * biệt hoa thường, bỏ qua bản đã xóa (ux_categories_user_name). Tên đã trim/kiểm độ dài ở CategoryCommandService.
 */
@Entity
@Table(name = "categories")
public class Category extends SyncedEntity {

    /** Giá trị entity_type trong change_logs / sync. */
    public static final String ENTITY_TYPE = "category";

    @Column(nullable = false, length = 50)
    private String name;

    protected Category() {
    }

    private Category(UUID id, UUID userId) {
        super(id, userId);
    }

    public static Category create(UUID id, UUID userId, String name) {
        Category category = new Category(id, userId);
        category.name = Objects.requireNonNull(name);
        return category;
    }

    /** @return true nếu tên đổi giá trị (đổi riêng kiểu chữ cũng là một thay đổi) */
    public boolean rename(String newName) {
        Objects.requireNonNull(newName);
        if (newName.equals(name)) {
            return false;
        }
        name = newName;
        return true;
    }

    public String getName() {
        return name;
    }
}
