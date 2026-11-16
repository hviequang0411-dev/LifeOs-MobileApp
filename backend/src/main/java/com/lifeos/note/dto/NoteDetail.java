package com.lifeos.note.dto;

import java.util.List;

/** GET /notes/{id}: ghi chú đầy đủ + thẻ + liên kết. */
public record NoteDetail(NoteResponse note, List<TagResponse> tags, List<NoteLinkResponse> links) {
}
