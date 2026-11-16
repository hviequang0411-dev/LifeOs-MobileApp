package com.lifeos.note.service;

import com.lifeos.common.exception.ConflictException;
import com.lifeos.common.exception.ErrorCode;
import com.lifeos.common.exception.NotFoundException;
import com.lifeos.common.persistence.VersionedWrites;
import com.lifeos.common.sync.ChangeOperation;
import com.lifeos.common.sync.ChangeRecorder;
import com.lifeos.goal.api.GoalApi;
import com.lifeos.note.dto.NoteLinkResponse;
import com.lifeos.note.dto.NoteTagResponse;
import com.lifeos.note.dto.TagResponse;
import com.lifeos.note.entity.Note;
import com.lifeos.note.entity.NoteLink;
import com.lifeos.note.entity.NoteTag;
import com.lifeos.note.mapper.NoteMapper;
import com.lifeos.note.repository.NoteLinkRepository;
import com.lifeos.note.repository.NoteRepository;
import com.lifeos.note.repository.NoteTagRepository;
import com.lifeos.note.repository.TagRepository;
import com.lifeos.task.api.TaskApi;
import java.time.Clock;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

/**
 * Lệnh ghi note_tag và note_link (D184, D189): chỉ CREATE/DELETE. Mọi ghi khóa Note FOR UPDATE trước (tuần tự theo
 * ghi chú: giới hạn 20, cặp duy nhất), rồi tham chiếu FOR SHARE (thẻ / Task / Goal). CREATE trùng cặp đang hoạt động →
 * trả bản đã có, không lỗi (idempotent theo cặp, §7).
 */
@Service
public class NoteRelationService {

    /** Kết quả tạo: created = false khi cặp đã tồn tại (trả bản đang có). */
    public record Created<T>(T value, boolean created) {
    }

    private static final String TAG_PK = "pk_note_tags";
    private static final String TAG_PAIR = "ux_note_tags_pair";
    private static final String LINK_PK = "pk_note_links";
    private static final String LINK_TRIPLE = "ux_note_links_triple";

    private final NoteRepository notes;
    private final TagRepository tags;
    private final NoteTagRepository noteTags;
    private final NoteLinkRepository links;
    private final TaskApi taskApi;
    private final GoalApi goalApi;
    private final ChangeRecorder changeRecorder;
    private final VersionedWrites versionedWrites;
    private final NoteMapper mapper;
    private final Clock clock;

    public NoteRelationService(NoteRepository notes, TagRepository tags, NoteTagRepository noteTags,
            NoteLinkRepository links, TaskApi taskApi, GoalApi goalApi, ChangeRecorder changeRecorder,
            VersionedWrites versionedWrites, NoteMapper mapper, Clock clock) {
        this.notes = notes;
        this.tags = tags;
        this.noteTags = noteTags;
        this.links = links;
        this.taskApi = taskApi;
        this.goalApi = goalApi;
        this.changeRecorder = changeRecorder;
        this.versionedWrites = versionedWrites;
        this.mapper = mapper;
        this.clock = clock;
    }

    // ---------- note_tag

    /** noteFromUrl: REST (ghi chú sai → 404) hay sync (→ 422 NOTE_NOT_FOUND). */
    public Created<NoteTagResponse> addTag(UUID userId, UUID id, UUID rawNoteId, UUID rawTagId, boolean noteFromUrl,
            UUID mutationId) {
        try {
            return versionedWrites.write(() -> {
                if (noteTags.existsById(id)) {
                    throw new ConflictException(ErrorCode.ENTITY_ALREADY_EXISTS);
                }
                UUID noteId = NoteValues.requiredId(rawNoteId, "noteId");
                UUID tagId = NoteValues.requiredId(rawTagId, "tagId");
                lockNote(userId, noteId, noteFromUrl);
                if (tags.findActiveForShare(tagId, userId).isEmpty()) {
                    throw NoteValues.reference(ErrorCode.TAG_NOT_FOUND);
                }
                Optional<NoteTag> existing = noteTags.findLivePair(userId, noteId, tagId);
                if (existing.isPresent()) {
                    return new Created<>(mapper.toResponse(existing.get()), false);
                }
                if (noteTags.countLiveByNote(noteId) >= NoteValues.MAX_TAGS_PER_NOTE) {
                    throw NoteValues.tagLimit();
                }
                NoteTag saved = noteTags.saveAndFlush(NoteTag.create(id, userId, noteId, tagId));
                changeRecorder.record(userId, NoteTag.ENTITY_TYPE, saved.getId(), ChangeOperation.CREATE,
                        saved.getVersion(), mutationId);
                return new Created<>(mapper.toResponse(saved), true);
            }, () -> 0L);
        } catch (DataIntegrityViolationException e) {
            throw translate(e, TAG_PK, TAG_PAIR);
        }
    }

    public long removeTag(UUID userId, UUID noteTagId, long expectedVersion, UUID mutationId) {
        return versionedWrites.write(() -> {
            NoteTag x = noteTags.findActive(noteTagId, userId).orElseThrow(NoteRelationService::noteTagNotFound);
            notes.findActiveForUpdate(x.getNoteId(), userId);
            VersionedWrites.requireVersion(x, expectedVersion);
            x.softDelete(clock.instant());
            NoteTag saved = noteTags.saveAndFlush(x);
            changeRecorder.record(userId, NoteTag.ENTITY_TYPE, saved.getId(), ChangeOperation.DELETE,
                    saved.getVersion(), mutationId);
            return saved.getVersion();
        }, () -> noteTags.findVersion(noteTagId, userId).orElseThrow(NoteRelationService::noteTagNotFound));
    }

    /**
     * PUT /notes/{id}/tags: thay tập thẻ (≤ 20). Thẻ sai → 422 TAG_NOT_FOUND. note_tag mới có id do server sinh;
     * change_logs DELETE trước CREATE, mutation_id NULL [PROPOSED: không cần version của ghi chú].
     */
    public List<TagResponse> replaceTags(UUID userId, UUID noteId, List<UUID> rawTagIds) {
        Set<UUID> wanted = new LinkedHashSet<>(rawTagIds);
        if (wanted.contains(null)) {
            throw NoteValues.invalid("tagIds", "INVALID_VALUE", "tagIds không được chứa null.");
        }
        if (wanted.size() > NoteValues.MAX_TAGS_PER_NOTE) {
            throw NoteValues.tagLimit();
        }
        return versionedWrites.write(() -> {
            lockNote(userId, noteId, true);
            for (UUID tagId : wanted.stream().sorted().toList()) {
                if (tags.findActiveForShare(tagId, userId).isEmpty()) {
                    throw NoteValues.reference(ErrorCode.TAG_NOT_FOUND);
                }
            }
            List<NoteTag> current = noteTags.findLiveByNote(userId, noteId);
            Set<UUID> have = current.stream().map(NoteTag::getTagId).collect(Collectors.toSet());
            List<NoteTag> removed = current.stream().filter(x -> !wanted.contains(x.getTagId())).toList();
            removed.forEach(x -> x.softDelete(clock.instant()));
            List<NoteTag> deleted = noteTags.saveAllAndFlush(removed);
            List<NoteTag> added = new ArrayList<>();
            for (UUID tagId : wanted) {
                if (!have.contains(tagId)) {
                    added.add(NoteTag.create(UUID.randomUUID(), userId, noteId, tagId));
                }
            }
            List<NoteTag> created = noteTags.saveAllAndFlush(added);
            for (NoteTag x : deleted) {
                changeRecorder.record(userId, NoteTag.ENTITY_TYPE, x.getId(), ChangeOperation.DELETE, x.getVersion(),
                        null);
            }
            for (NoteTag x : created) {
                changeRecorder.record(userId, NoteTag.ENTITY_TYPE, x.getId(), ChangeOperation.CREATE, x.getVersion(),
                        null);
            }
            return wanted.isEmpty() ? List.<TagResponse>of() : tags.findLiveIn(userId, wanted).stream()
                    .sorted(java.util.Comparator.comparing((com.lifeos.note.entity.Tag t) -> t.getName().toLowerCase(
                            java.util.Locale.ROOT)).thenComparing(com.lifeos.note.entity.Tag::getId))
                    .map(mapper::toResponse).toList();
        }, () -> 0L);
    }

    // ---------- note_link

    public Created<NoteLinkResponse> addLink(UUID userId, UUID id, UUID rawNoteId, String rawTargetType,
            UUID rawTargetId, boolean noteFromUrl, UUID mutationId) {
        try {
            return versionedWrites.write(() -> {
                if (links.existsById(id)) {
                    throw new ConflictException(ErrorCode.ENTITY_ALREADY_EXISTS);
                }
                UUID noteId = NoteValues.requiredId(rawNoteId, "noteId");
                String targetType = NoteValues.targetType(rawTargetType);
                UUID targetId = NoteValues.requiredId(rawTargetId, "targetId");
                lockNote(userId, noteId, noteFromUrl);
                boolean exists = "TASK".equals(targetType) ? taskApi.lockForShare(userId, targetId).isPresent()
                        : goalApi.lockForShare(userId, targetId);
                if (!exists) {
                    throw NoteValues.reference("TASK".equals(targetType) ? ErrorCode.TASK_NOT_FOUND
                            : ErrorCode.GOAL_NOT_FOUND);
                }
                Optional<NoteLink> existing = links.findLiveTriple(userId, noteId, targetType, targetId);
                if (existing.isPresent()) {
                    return new Created<>(mapper.toResponse(existing.get()), false);
                }
                if (links.countLiveByNote(noteId) >= NoteValues.MAX_LINKS_PER_NOTE) {
                    throw NoteValues.tagLimit();
                }
                NoteLink saved = links.saveAndFlush(NoteLink.create(id, userId, noteId, targetType, targetId));
                changeRecorder.record(userId, NoteLink.ENTITY_TYPE, saved.getId(), ChangeOperation.CREATE,
                        saved.getVersion(), mutationId);
                return new Created<>(mapper.toResponse(saved), true);
            }, () -> 0L);
        } catch (DataIntegrityViolationException e) {
            throw translate(e, LINK_PK, LINK_TRIPLE);
        }
    }

    /** REST DELETE /notes/{noteId}/links/{id}: liên kết phải thuộc ghi chú trong URL (khác → 404). */
    public long removeLink(UUID userId, UUID noteIdOrNull, UUID linkId, long expectedVersion, UUID mutationId) {
        return versionedWrites.write(() -> {
            NoteLink x = links.findActive(linkId, userId)
                    .filter(l -> noteIdOrNull == null || l.getNoteId().equals(noteIdOrNull))
                    .orElseThrow(NoteRelationService::linkNotFound);
            notes.findActiveForUpdate(x.getNoteId(), userId);
            VersionedWrites.requireVersion(x, expectedVersion);
            x.softDelete(clock.instant());
            NoteLink saved = links.saveAndFlush(x);
            changeRecorder.record(userId, NoteLink.ENTITY_TYPE, saved.getId(), ChangeOperation.DELETE,
                    saved.getVersion(), mutationId);
            return saved.getVersion();
        }, () -> links.findVersion(linkId, userId).orElseThrow(NoteRelationService::linkNotFound));
    }

    private Note lockNote(UUID userId, UUID noteId, boolean fromUrl) {
        return notes.findActiveForUpdate(noteId, userId).orElseThrow(() -> fromUrl ? NoteCommandService.notFound()
                : NoteValues.reference(ErrorCode.NOTE_NOT_FOUND));
    }

    private static RuntimeException translate(DataIntegrityViolationException e, String pk, String unique) {
        if (NoteCommandService.mentions(e, pk) || NoteCommandService.mentions(e, unique)) {
            return new ConflictException(ErrorCode.ENTITY_ALREADY_EXISTS);
        }
        return e;
    }

    /** note_tag/note_link không thấy: dùng mã của ghi chú (404 NOTE_NOT_FOUND) [PROPOSED]. */
    static NotFoundException noteTagNotFound() {
        return new NotFoundException(ErrorCode.NOTE_NOT_FOUND);
    }

    static NotFoundException linkNotFound() {
        return new NotFoundException(ErrorCode.NOTE_NOT_FOUND);
    }

    static Map<String, Object> none() {
        return Map.of();
    }

    static <T, R> List<R> map(List<T> in, Function<T, R> f) {
        return in.stream().map(f).toList();
    }
}
