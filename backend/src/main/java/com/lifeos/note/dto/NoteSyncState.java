package com.lifeos.note.dto;

/** Trạng thái một bản ghi note/tag/note_tag/note_link cho snapshot sync, kể cả tombstone. */
public record NoteSyncState(java.util.UUID id, long version, boolean deleted, Object data) {
}
