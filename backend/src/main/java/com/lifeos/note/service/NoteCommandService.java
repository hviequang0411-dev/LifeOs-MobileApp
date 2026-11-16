package com.lifeos.note.service;

import com.lifeos.common.exception.ConflictException;
import com.lifeos.common.exception.ErrorCode;
import com.lifeos.common.exception.NotFoundException;
import com.lifeos.common.persistence.SyncedEntity;
import com.lifeos.common.persistence.VersionedWrites;
import com.lifeos.common.sync.ChangeOperation;
import com.lifeos.common.sync.ChangeRecorder;
import com.lifeos.note.dto.NoteResponse;
import com.lifeos.note.entity.Note;
import com.lifeos.note.entity.NoteLink;
import com.lifeos.note.entity.NoteTag;
import com.lifeos.note.mapper.NoteMapper;
import com.lifeos.note.repository.NoteLinkRepository;
import com.lifeos.note.repository.NoteRepository;
import com.lifeos.note.repository.NoteTagRepository;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.function.Function;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

/**
 * Lệnh ghi ghi chú (FR-NOTE-01/02, D180, D181). Thứ tự kiểm D96; ChangeRecorder sau mọi ghi (D86). Xóa ghi chú: khóa
 * FOR UPDATE → xóa mềm ghi chú, note_tags, note_links → change_logs con (mutation_id NULL) → change_logs ghi chú.
 * Nội dung/tiêu đề không bao giờ vào log/lỗi (D138).
 */
@Service
public class NoteCommandService {

    private static final String PK = "pk_notes";

    private final NoteRepository notes;
    private final NoteTagRepository noteTags;
    private final NoteLinkRepository links;
    private final ChangeRecorder changeRecorder;
    private final VersionedWrites versionedWrites;
    private final NoteMapper mapper;
    private final Clock clock;

    public NoteCommandService(NoteRepository notes, NoteTagRepository noteTags, NoteLinkRepository links,
            ChangeRecorder changeRecorder, VersionedWrites versionedWrites, NoteMapper mapper, Clock clock) {
        this.notes = notes;
        this.noteTags = noteTags;
        this.links = links;
        this.changeRecorder = changeRecorder;
        this.versionedWrites = versionedWrites;
        this.mapper = mapper;
        this.clock = clock;
    }

    public NoteResponse create(UUID userId, NoteCommands.Create cmd, UUID mutationId) {
        try {
            return versionedWrites.write(() -> {
                if (notes.existsById(cmd.id())) {
                    throw new ConflictException(ErrorCode.ENTITY_ALREADY_EXISTS);
                }
                String title = NoteValues.title(cmd.title().value());
                String content = NoteValues.content(cmd.content().value());
                boolean pinned = Boolean.TRUE.equals(cmd.pinned().value());
                if (pinned && notes.countPinned(userId) >= NoteValues.MAX_PINNED) {
                    throw NoteValues.limitExceeded();
                }
                Note note = new Note(cmd.id(), userId);
                note.setTitle(title);
                note.setContent(content);
                note.setPinned(pinned);
                if (Boolean.TRUE.equals(cmd.archived().value())) {
                    note.setArchivedAt(clock.instant());
                }
                Note saved = notes.saveAndFlush(note);
                changeRecorder.record(userId, Note.ENTITY_TYPE, saved.getId(), ChangeOperation.CREATE,
                        saved.getVersion(), mutationId);
                return mapper.toResponse(saved);
            }, () -> 0L);
        } catch (DataIntegrityViolationException e) {
            if (mentions(e, PK)) {
                throw new ConflictException(ErrorCode.ENTITY_ALREADY_EXISTS);
            }
            throw e;
        }
    }

    public NoteResponse update(UUID userId, UUID noteId, NoteCommands.Update cmd, UUID mutationId) {
        return write(userId, noteId, cmd.expectedVersion(), mutationId, note -> {
            String title = cmd.title().present() ? NoteValues.title(cmd.title().value()) : note.getTitle();
            String content = cmd.content().present() ? NoteValues.content(cmd.content().value()) : note.getContent();
            boolean changed = note.setTitle(title);
            changed |= note.setContent(content);
            if (cmd.pinned().present()) {
                changed |= pin(userId, note, Boolean.TRUE.equals(cmd.pinned().value()));
            }
            if (cmd.archived().present()) {
                changed |= archive(note, Boolean.TRUE.equals(cmd.archived().value()));
            }
            return changed;
        });
    }

    public NoteResponse pin(UUID userId, UUID noteId, long expectedVersion, boolean pinned) {
        return write(userId, noteId, expectedVersion, null, note -> pin(userId, note, pinned));
    }

    public NoteResponse archive(UUID userId, UUID noteId, long expectedVersion, boolean archived) {
        return write(userId, noteId, expectedVersion, null, note -> archive(note, archived));
    }

    /** Ghim tối đa 10 (đếm mọi ghi chú còn sống đang ghim) → 422 LIMIT_EXCEEDED. */
    private boolean pin(UUID userId, Note note, boolean pinned) {
        if (note.isPinned() == pinned) {
            return false;
        }
        if (pinned && notes.countPinned(userId) >= NoteValues.MAX_PINNED) {
            throw NoteValues.limitExceeded();
        }
        return note.setPinned(pinned);
    }

    private boolean archive(Note note, boolean archived) {
        if (archived == (note.getArchivedAt() != null)) {
            return false;
        }
        return note.setArchivedAt(archived ? clock.instant() : null);
    }

    public long delete(UUID userId, UUID noteId, long expectedVersion, UUID mutationId) {
        return versionedWrites.write(() -> {
            Note note = notes.findActiveForUpdate(noteId, userId).orElseThrow(NoteCommandService::notFound);
            VersionedWrites.requireVersion(note, expectedVersion);
            Instant now = clock.instant();
            note.softDelete(now);
            Note saved = notes.saveAndFlush(note);
            List<NoteTag> tagLinks = noteTags.findLiveByNote(userId, noteId);
            List<NoteLink> targetLinks = links.findLiveByNote(userId, noteId);
            tagLinks.forEach(x -> x.softDelete(now));
            targetLinks.forEach(x -> x.softDelete(now));
            List<NoteTag> deletedTags = noteTags.saveAllAndFlush(tagLinks);
            List<NoteLink> deletedLinks = links.saveAllAndFlush(targetLinks);
            recordDeletes(userId, NoteTag.ENTITY_TYPE, deletedTags);
            recordDeletes(userId, NoteLink.ENTITY_TYPE, deletedLinks);
            changeRecorder.record(userId, Note.ENTITY_TYPE, saved.getId(), ChangeOperation.DELETE,
                    saved.getVersion(), mutationId);
            return saved.getVersion();
        }, () -> notes.findVersion(noteId, userId).orElseThrow(NoteCommandService::notFound));
    }

    private NoteResponse write(UUID userId, UUID noteId, long expectedVersion, UUID mutationId,
            Function<Note, Boolean> change) {
        return versionedWrites.write(() -> {
            Note note = notes.findActive(noteId, userId).orElseThrow(NoteCommandService::notFound);
            VersionedWrites.requireVersion(note, expectedVersion);
            if (!change.apply(note)) {
                return mapper.toResponse(note);
            }
            Note saved = notes.saveAndFlush(note);
            changeRecorder.record(userId, Note.ENTITY_TYPE, saved.getId(), ChangeOperation.UPDATE,
                    saved.getVersion(), mutationId);
            return mapper.toResponse(saved);
        }, () -> notes.findVersion(noteId, userId).orElseThrow(NoteCommandService::notFound));
    }

    private <T extends SyncedEntity> void recordDeletes(UUID userId, String entityType, List<T> deleted) {
        for (T entity : deleted) {
            changeRecorder.record(userId, entityType, entity.getId(), ChangeOperation.DELETE, entity.getVersion(),
                    null);
        }
    }

    static boolean mentions(DataIntegrityViolationException e, String constraint) {
        for (Throwable t = e; t != null; t = t.getCause()) {
            if (t.getMessage() != null && t.getMessage().contains(constraint)) {
                return true;
            }
        }
        return false;
    }

    static NotFoundException notFound() {
        return new NotFoundException(ErrorCode.NOTE_NOT_FOUND);
    }
}
