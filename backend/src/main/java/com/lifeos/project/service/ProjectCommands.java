package com.lifeos.project.service;

import com.lifeos.common.util.Patch;
import java.util.UUID;

/**
 * Lệnh của use case Project (JAVA_SPRING_RULES §2.2: đầu vào Application Service không phụ thuộc HTTP, để sync gọi
 * cùng service). toString không in name/description (SECURITY_RULES §11.1).
 */
public final class ProjectCommands {

    private ProjectCommands() {
    }

    /**
     * Trường dạng {@link Patch} để payload sync sai kiểu được báo ở bước validation của service, sau bước kiểm id đã
     * dùng (D96). Vắng mặt = null.
     */
    public record Create(UUID id, Patch<String> name, Patch<String> description) {

        /** REST: giá trị đã qua Jackson. */
        public Create(UUID id, String name, String description) {
            this(id, Patch.of(name), Patch.of(description));
        }

        @Override
        public String toString() {
            return "ProjectCommands.Create[id=" + id + "]";
        }
    }

    /**
     * JSON Merge Patch: name không cho phép null; description null = xóa mô tả. archived chỉ đến từ sync
     * (SYNC_PROTOCOL §3.2, Q4 Giai đoạn 3): true/false = lưu trữ/bỏ lưu trữ trong CÙNG lần ghi (một lần tăng version),
     * không cho phép null. REST PATCH luôn để vắng (lưu trữ qua /archive, /unarchive).
     */
    public record Update(long expectedVersion, Patch<String> name, Patch<String> description,
            Patch<Boolean> archived) {

        public Update(long expectedVersion, Patch<String> name, Patch<String> description) {
            this(expectedVersion, name, description, Patch.absent());
        }

        @Override
        public String toString() {
            return "ProjectCommands.Update[expectedVersion=" + expectedVersion + "]";
        }
    }
}
