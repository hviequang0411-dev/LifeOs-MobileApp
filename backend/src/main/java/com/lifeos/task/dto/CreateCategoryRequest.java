package com.lifeos.task.dto;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

/**
 * POST /categories { id, name }. id do client tạo (D62). Trim/độ dài/trùng tên kiểm ở service. Trường lạ (userId,
 * version…) bị bỏ qua và không ánh xạ vào entity (SECURITY_RULES §7.5).
 */
public record CreateCategoryRequest(@NotNull UUID id, @NotNull String name) {

    @Override
    public String toString() {
        return "CreateCategoryRequest[id=" + id + "]";
    }
}
