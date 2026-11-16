package com.lifeos.note.dto;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

/** POST /notes. Quick capture: title và content đều có thể rỗng (FR-NOTE-02). */
public record CreateNoteRequest(@NotNull UUID id, String title, String content) {

    @Override
    public String toString() {
        return "CreateNoteRequest[id=" + id + "]";
    }
}
