package com.lifeos.analytics.service;

import com.lifeos.common.util.Patch;
import java.util.UUID;

/** Lệnh review (REST và sync gọi cùng service). toString không in nội dung. */
public final class ReviewCommands {

    private ReviewCommands() {
    }

    public record Create(UUID id, Patch<String> periodType, Patch<String> periodKey, Patch<String> reflection,
            Patch<Integer> mood) {
        @Override
        public String toString() {
            return "ReviewCommands.Create[id=" + id + "]";
        }
    }

    /** periodType/periodKey bất biến (đổi → VALIDATION_FAILED). */
    public record Update(long expectedVersion, Patch<String> periodType, Patch<String> periodKey,
            Patch<String> reflection, Patch<Integer> mood) {
        @Override
        public String toString() {
            return "ReviewCommands.Update[expectedVersion=" + expectedVersion + "]";
        }
    }
}
