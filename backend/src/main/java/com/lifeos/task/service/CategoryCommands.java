package com.lifeos.task.service;

import com.lifeos.common.util.Patch;
import java.util.UUID;

/** Lệnh của use case Category (JAVA_SPRING_RULES §2.2). toString không in name (SECURITY_RULES §11.1). */
public final class CategoryCommands {

    private CategoryCommands() {
    }

    /** name dạng {@link Patch} để payload sync sai kiểu được báo sau bước kiểm id đã dùng (D96). */
    public record Create(UUID id, Patch<String> name) {

        /** REST: giá trị đã qua Jackson. */
        public Create(UUID id, String name) {
            this(id, Patch.of(name));
        }

        @Override
        public String toString() {
            return "CategoryCommands.Create[id=" + id + "]";
        }
    }

    /** JSON Merge Patch: name không cho phép null. */
    public record Update(long expectedVersion, Patch<String> name) {
        @Override
        public String toString() {
            return "CategoryCommands.Update[expectedVersion=" + expectedVersion + "]";
        }
    }
}
