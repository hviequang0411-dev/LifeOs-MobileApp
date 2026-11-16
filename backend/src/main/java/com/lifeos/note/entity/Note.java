package com.lifeos.note.entity;

import com.lifeos.common.persistence.SyncedEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/** Ghi chú văn bản thuần (FR-NOTE-01, D180, D181). search_vector là cột sinh ở DB, không ánh xạ. */
@Entity
@Table(name = "notes")
public class Note extends SyncedEntity {

    public static final String ENTITY_TYPE = "note";

    @Column(name = "title", length = 200)
    private String title;

    @Column(name = "content", nullable = false)
    private String content;

    @Column(name = "pinned", nullable = false)
    private boolean pinned;

    @Column(name = "archived_at")
    private Instant archivedAt;

    protected Note() {
    }

    public Note(UUID id, UUID userId) {
        super(id, userId);
    }

    public String getTitle() {
        return title;
    }

    /** @return true nếu giá trị đổi */
    public boolean setTitle(String value) {
        if (Objects.equals(title, value)) {
            return false;
        }
        title = value;
        return true;
    }

    public String getContent() {
        return content;
    }

    /** @return true nếu giá trị đổi */
    public boolean setContent(String value) {
        if (Objects.equals(content, value)) {
            return false;
        }
        content = value;
        return true;
    }

    public boolean isPinned() {
        return pinned;
    }

    /** @return true nếu giá trị đổi */
    public boolean setPinned(boolean value) {
        if (pinned == value) {
            return false;
        }
        pinned = value;
        return true;
    }

    public Instant getArchivedAt() {
        return archivedAt;
    }

    /** @return true nếu giá trị đổi */
    public boolean setArchivedAt(Instant value) {
        if (Objects.equals(archivedAt, value)) {
            return false;
        }
        archivedAt = value;
        return true;
    }
}
