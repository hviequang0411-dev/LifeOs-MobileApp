package com.lifeos.note.entity;

import com.lifeos.common.persistence.SyncedEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.util.Objects;
import java.util.UUID;

/** Liên kết ghi chú–thẻ (D184): entity đồng bộ riêng, chỉ CREATE/DELETE; noteId/tagId bất biến. */
@Entity
@Table(name = "note_tags")
public class NoteTag extends SyncedEntity {

    public static final String ENTITY_TYPE = "note_tag";

    @Column(name = "note_id", nullable = false, updatable = false)
    private UUID noteId;

    @Column(name = "tag_id", nullable = false, updatable = false)
    private UUID tagId;

    protected NoteTag() {
    }

    public NoteTag(UUID id, UUID userId) {
        super(id, userId);
    }

    public static NoteTag create(UUID id, UUID userId, UUID noteId, UUID tagId) {
        NoteTag e = new NoteTag(id, userId);
        e.noteId = noteId;
        e.tagId = tagId;
        return e;
    }

    public UUID getNoteId() {
        return noteId;
    }

    /** @return true nếu giá trị đổi */
    public boolean setNoteId(UUID value) {
        if (Objects.equals(noteId, value)) {
            return false;
        }
        noteId = value;
        return true;
    }

    public UUID getTagId() {
        return tagId;
    }

    /** @return true nếu giá trị đổi */
    public boolean setTagId(UUID value) {
        if (Objects.equals(tagId, value)) {
            return false;
        }
        tagId = value;
        return true;
    }
}
