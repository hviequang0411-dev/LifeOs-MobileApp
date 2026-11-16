package com.lifeos.note.dto;

import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.UUID;

/** PUT /notes/{id}/tags: tập thẻ mới (≤ 20); server so sánh và tạo/xóa note_tag. */
public record ReplaceNoteTagsRequest(@NotNull List<UUID> tagIds) {
}
