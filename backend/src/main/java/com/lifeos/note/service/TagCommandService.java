package com.lifeos.note.service;

import com.lifeos.common.exception.ConflictException;
import com.lifeos.common.exception.ErrorCode;
import com.lifeos.common.exception.NaturalKeyConflictException;
import com.lifeos.common.exception.NotFoundException;
import com.lifeos.common.persistence.VersionedWrites;
import com.lifeos.common.sync.ChangeOperation;
import com.lifeos.common.sync.ChangeRecorder;
import com.lifeos.note.dto.TagResponse;
import com.lifeos.note.entity.NoteTag;
import com.lifeos.note.entity.Tag;
import com.lifeos.note.mapper.NoteMapper;
import com.lifeos.note.repository.NoteTagRepository;
import com.lifeos.note.repository.TagRepository;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

/**
 * Lệnh ghi thẻ (FR-NOTE-03, D184). Tên duy nhất không phân biệt hoa thường: CREATE trùng → 409 TAG_NAME_TAKEN (sync:
 * CONFLICT ALREADY_EXISTS kèm thẻ đang có, để client hợp nhất); đổi tên trùng → 409 TAG_NAME_TAKEN. Xóa thẻ: khóa
 * FOR UPDATE → xóa mềm thẻ và mọi note_tag của thẻ (UC-NOTE-04).
 */
@Service
public class TagCommandService {

    private static final String PK = "pk_tags";
    private static final String NAME_UNIQUE = "ux_tags_user_name";
    private static final UUID NO_ID = new UUID(0, 0);

    private final TagRepository tags;
    private final NoteTagRepository noteTags;
    private final ChangeRecorder changeRecorder;
    private final VersionedWrites versionedWrites;
    private final NoteMapper mapper;
    private final Clock clock;

    public TagCommandService(TagRepository tags, NoteTagRepository noteTags, ChangeRecorder changeRecorder,
            VersionedWrites versionedWrites, NoteMapper mapper, Clock clock) {
        this.tags = tags;
        this.noteTags = noteTags;
        this.changeRecorder = changeRecorder;
        this.versionedWrites = versionedWrites;
        this.mapper = mapper;
        this.clock = clock;
    }

    public TagResponse create(UUID userId, NoteCommands.CreateTag cmd, UUID mutationId) {
        try {
            return versionedWrites.write(() -> {
                if (tags.existsById(cmd.id())) {
                    throw new ConflictException(ErrorCode.ENTITY_ALREADY_EXISTS);
                }
                String name = NoteValues.tagName(cmd.name().value());
                String color = NoteValues.color(cmd.color().value());
                Optional<Tag> existing = tags.findLiveByName(userId, name, NO_ID);
                if (existing.isPresent()) {
                    throw new NaturalKeyConflictException(ErrorCode.TAG_NAME_TAKEN, existing.get().getId());
                }
                Tag tag = new Tag(cmd.id(), userId);
                tag.setName(name);
                tag.setColor(color);
                Tag saved = tags.saveAndFlush(tag);
                changeRecorder.record(userId, Tag.ENTITY_TYPE, saved.getId(), ChangeOperation.CREATE,
                        saved.getVersion(), mutationId);
                return mapper.toResponse(saved);
            }, () -> 0L);
        } catch (DataIntegrityViolationException e) {
            throw translate(e);
        }
    }

    public TagResponse update(UUID userId, UUID tagId, NoteCommands.UpdateTag cmd, UUID mutationId) {
        try {
            return versionedWrites.write(() -> {
                Tag tag = tags.findActive(tagId, userId).orElseThrow(TagCommandService::notFound);
                VersionedWrites.requireVersion(tag, cmd.expectedVersion());
                String name = cmd.name().present() ? NoteValues.tagName(cmd.name().value()) : tag.getName();
                String color = cmd.color().present() ? NoteValues.color(cmd.color().value()) : tag.getColor();
                if (tags.findLiveByName(userId, name, tagId).isPresent()) {
                    throw new ConflictException(ErrorCode.TAG_NAME_TAKEN);
                }
                boolean changed = tag.setName(name);
                changed |= tag.setColor(color);
                if (!changed) {
                    return mapper.toResponse(tag);
                }
                Tag saved = tags.saveAndFlush(tag);
                changeRecorder.record(userId, Tag.ENTITY_TYPE, saved.getId(), ChangeOperation.UPDATE,
                        saved.getVersion(), mutationId);
                return mapper.toResponse(saved);
            }, () -> tags.findVersion(tagId, userId).orElseThrow(TagCommandService::notFound));
        } catch (DataIntegrityViolationException e) {
            throw translate(e);
        }
    }

    public long delete(UUID userId, UUID tagId, long expectedVersion, UUID mutationId) {
        return versionedWrites.write(() -> {
            Tag tag = tags.findActiveForUpdate(tagId, userId).orElseThrow(TagCommandService::notFound);
            VersionedWrites.requireVersion(tag, expectedVersion);
            Instant now = clock.instant();
            tag.softDelete(now);
            Tag saved = tags.saveAndFlush(tag);
            List<NoteTag> children = noteTags.findLiveByTag(userId, tagId);
            children.forEach(x -> x.softDelete(now));
            for (NoteTag x : noteTags.saveAllAndFlush(children)) {
                changeRecorder.record(userId, NoteTag.ENTITY_TYPE, x.getId(), ChangeOperation.DELETE, x.getVersion(),
                        null);
            }
            changeRecorder.record(userId, Tag.ENTITY_TYPE, saved.getId(), ChangeOperation.DELETE, saved.getVersion(),
                    mutationId);
            return saved.getVersion();
        }, () -> tags.findVersion(tagId, userId).orElseThrow(TagCommandService::notFound));
    }

    private static RuntimeException translate(DataIntegrityViolationException e) {
        if (NoteCommandService.mentions(e, PK)) {
            return new ConflictException(ErrorCode.ENTITY_ALREADY_EXISTS);
        }
        if (NoteCommandService.mentions(e, NAME_UNIQUE)) {
            return new ConflictException(ErrorCode.TAG_NAME_TAKEN);
        }
        return e;
    }

    static NotFoundException notFound() {
        return new NotFoundException(ErrorCode.TAG_NOT_FOUND);
    }
}
