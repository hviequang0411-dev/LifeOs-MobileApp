package com.lifeos.project.dto;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

/**
 * POST /projects { id, name, description? }. id do client tạo (D62). Quy tắc giá trị (trim, độ dài) kiểm ở service.
 * Trường lạ (userId, version, createdAt, archivedAt…) bị bỏ qua và không ánh xạ vào entity (SECURITY_RULES §7.5).
 */
public record CreateProjectRequest(@NotNull UUID id, @NotNull String name, String description) {

    @Override
    public String toString() {
        return "CreateProjectRequest[id=" + id + "]";
    }
}
