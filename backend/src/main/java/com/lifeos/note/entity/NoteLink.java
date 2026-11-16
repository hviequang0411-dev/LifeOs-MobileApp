package com.lifeos.note.entity;

import com.lifeos.common.persistence.SyncedEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.util.Objects;
import java.util.UUID;

/** Liên kết ghi chú → Task/Goal (FR-NOTE-06, D189): chỉ CREATE/DELETE; bất biến. */
@Entity
@Table(name = "note_links")
public class NoteLink extends SyncedEntity {

    public static final String ENTITY_TYPE = "note_link";

    @Column(name = "note_id", nullable = false, updatable = false)
    private UUID noteId;

    @Column(name = "target_type", nullable = false, updatable = false, length = 4)
    private String targetType;

    @Column(name = "target_id", nullable = false, updatable = false)
    private UUID targetId;

    protected NoteLink() {
    }

    public NoteLink(UUID id, UUID userId) {
        super(id, userId);
    }

    public static NoteLink create(UUID id, UUID userId, UUID noteId, String targetType, UUID targetId) {
        NoteLink e = new NoteLink(id, userId);
        e.noteId = noteId;
        e.targetType = targetType;
        e.targetId = targetId;
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

    public String getTargetType() {
        return targetType;
    }

    /** @return true nếu giá trị đổi */
    public boolean setTargetType(String value) {
        if (Objects.equals(targetType, value)) {
            return false;
        }
        targetType = value;
        return true;
    }

    public UUID getTargetId() {
        return targetId;
    }

    /** @return true nếu giá trị đổi */
    public boolean setTargetId(UUID value) {
        if (Objects.equals(targetId, value)) {
            return false;
        }
        targetId = value;
        return true;
    }
}
