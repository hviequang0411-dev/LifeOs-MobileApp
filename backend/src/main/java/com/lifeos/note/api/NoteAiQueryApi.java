package com.lifeos.note.api;

import java.util.Optional;
import java.util.UUID;

/** A selected Note only; MVP deliberately has no semantic/global Note search. */
public interface NoteAiQueryApi {
    record Snapshot(UUID id, String title, String content, long version) { }

    Optional<Snapshot> aiNote(UUID userId, UUID noteId);
}
