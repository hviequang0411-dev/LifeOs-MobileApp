package com.lifeos.note.entity;

import com.lifeos.common.persistence.SyncedEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.util.Objects;
import java.util.UUID;

/** Thẻ ghi chú (FR-NOTE-03, D184): tên 1–30 ký tự, duy nhất theo user (không phân biệt hoa thường). */
@Entity
@Table(name = "tags")
public class Tag extends SyncedEntity {

    public static final String ENTITY_TYPE = "tag";

    @Column(name = "name", nullable = false, length = 30)
    private String name;

    @org.hibernate.annotations.JdbcTypeCode(org.hibernate.type.SqlTypes.CHAR)
    @Column(name = "color", length = 7)
    private String color;

    protected Tag() {
    }

    public Tag(UUID id, UUID userId) {
        super(id, userId);
    }

    public String getName() {
        return name;
    }

    /** @return true nếu giá trị đổi */
    public boolean setName(String value) {
        if (Objects.equals(name, value)) {
            return false;
        }
        name = value;
        return true;
    }

    public String getColor() {
        return color;
    }

    /** @return true nếu giá trị đổi */
    public boolean setColor(String value) {
        if (Objects.equals(color, value)) {
            return false;
        }
        color = value;
        return true;
    }
}
