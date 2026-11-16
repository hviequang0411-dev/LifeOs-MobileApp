package com.lifeos.note.service;

import com.lifeos.common.util.Patch;
import java.util.UUID;

/** Lệnh của use case ghi chú/thẻ/liên kết (REST và sync gọi cùng service). toString không in nội dung. */
public final class NoteCommands {

    private NoteCommands() {
    }

    /** pinned/archived (cờ, chỉ sync): archived = true ⇒ server đặt archivedAt [PROPOSED]. */
    public record Create(UUID id, Patch<String> title, Patch<String> content, Patch<Boolean> pinned,
            Patch<Boolean> archived) {
        @Override
        public String toString() {
            return "NoteCommands.Create[id=" + id + "]";
        }
    }

    public record Update(long expectedVersion, Patch<String> title, Patch<String> content, Patch<Boolean> pinned,
            Patch<Boolean> archived) {
        @Override
        public String toString() {
            return "NoteCommands.Update[expectedVersion=" + expectedVersion + "]";
        }
    }

    public record CreateTag(UUID id, Patch<String> name, Patch<String> color) {
        @Override
        public String toString() {
            return "NoteCommands.CreateTag[id=" + id + "]";
        }
    }

    public record UpdateTag(long expectedVersion, Patch<String> name, Patch<String> color) {
        @Override
        public String toString() {
            return "NoteCommands.UpdateTag[expectedVersion=" + expectedVersion + "]";
        }
    }
}
