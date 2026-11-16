package com.lifeos.note.service;

import com.lifeos.common.exception.ErrorCode;
import com.lifeos.common.exception.FieldErrors;
import com.lifeos.common.exception.NotFoundException;
import com.lifeos.common.exception.RateLimitedException;
import com.lifeos.common.response.PageResponse;
import com.lifeos.common.security.RateLimitPolicy;
import com.lifeos.common.security.RateLimiter;
import com.lifeos.common.web.Cursors;
import com.lifeos.goal.api.GoalApi;
import com.lifeos.note.dto.NoteDetail;
import com.lifeos.note.api.NoteAiQueryApi;
import com.lifeos.note.dto.NoteSummary;
import com.lifeos.note.dto.NoteSyncState;
import com.lifeos.note.dto.TagResponse;
import com.lifeos.note.entity.Note;
import com.lifeos.note.entity.NoteTag;
import com.lifeos.note.entity.Tag;
import com.lifeos.note.mapper.NoteMapper;
import com.lifeos.note.repository.NoteLinkRepository;
import com.lifeos.note.repository.NoteRepository;
import com.lifeos.note.repository.NoteSearchStore;
import com.lifeos.note.repository.NoteTagRepository;
import com.lifeos.note.repository.TagRepository;
import java.text.Normalizer;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Đọc ghi chú, thẻ, liên kết; danh sách lọc và tìm kiếm toàn văn (FR-NOTE-04/05, D182). Truy vấn tìm kiếm không được
 * lưu/log ở server; giới hạn 60/phút/user (NOTE_SEARCH_USER).
 */
@Service
public class NoteQueryService implements NoteAiQueryApi {

    static final String SORT = "updatedAt:desc";
    static final String SEARCH_SORT = "rank:offset";
    private static final Set<String> ARCHIVED_MODES = Set.of("false", "true", "all");

    private final NoteRepository notes;
    private final TagRepository tags;
    private final NoteTagRepository noteTags;
    private final NoteLinkRepository links;
    private final NoteSearchStore search;
    private final GoalApi goalApi;
    private final RateLimiter rateLimiter;
    private final NoteMapper mapper;

    public NoteQueryService(NoteRepository notes, TagRepository tags, NoteTagRepository noteTags,
            NoteLinkRepository links, NoteSearchStore search, GoalApi goalApi, RateLimiter rateLimiter,
            NoteMapper mapper) {
        this.notes = notes;
        this.tags = tags;
        this.noteTags = noteTags;
        this.links = links;
        this.search = search;
        this.goalApi = goalApi;
        this.rateLimiter = rateLimiter;
        this.mapper = mapper;
    }

    @Transactional(readOnly = true)
    public NoteDetail get(UUID userId, UUID noteId) {
        Note note = notes.findActive(noteId, userId).orElseThrow(NoteCommandService::notFound);
        List<UUID> tagIds = noteTags.findLiveByNote(userId, noteId).stream().map(NoteTag::getTagId).toList();
        List<TagResponse> noteTagList = tagIds.isEmpty() ? List.of() : tags.findLiveIn(userId, tagIds).stream()
                .sorted(Comparator.comparing((Tag t) -> t.getName().toLowerCase(java.util.Locale.ROOT))
                        .thenComparing(Tag::getId))
                .map(mapper::toResponse).toList();
        return new NoteDetail(mapper.toResponse(note), noteTagList,
                links.findLiveByNote(userId, noteId).stream().map(mapper::toResponse).toList());
    }

    @Override
    @Transactional(readOnly = true)
    public java.util.Optional<NoteAiQueryApi.Snapshot> aiNote(UUID userId, UUID noteId) {
        return notes.findActive(noteId, userId).map(mapper::toResponse)
                .map(n -> new NoteAiQueryApi.Snapshot(n.id(), n.title(), n.content(), n.version()));
    }

    /**
     * GET /notes: không q → keyset (updatedAt desc, id desc); có q → tìm toàn văn, xếp rank desc, updatedAt desc, id
     * desc, phân trang offset ≤ 1000 mã hóa trong cursor (BACKEND_MASTER_PLAN §4).
     */
    @Transactional(readOnly = true)
    public PageResponse<NoteSummary> list(UUID userId, String rawQ, List<UUID> tagIds, String tagMode,
            Boolean pinned, String archived, Integer limit, String cursor) {
        String mode = archived == null ? "false" : archived;
        if (!ARCHIVED_MODES.contains(mode)) {
            throw FieldErrors.single(ErrorCode.VALIDATION_FAILED, "archived", "INVALID_VALUE",
                    "archived phải là false, true hoặc all.");
        }
        if (tagMode != null && !"all".equals(tagMode) && !"any".equals(tagMode)) {
            throw FieldErrors.single(ErrorCode.VALIDATION_FAILED, "tagMode", "INVALID_VALUE",
                    "tagMode phải là all hoặc any.");
        }
        List<UUID> tagFilter = tagIds == null ? List.of() : List.copyOf(new LinkedHashSet<>(tagIds));
        if (tagFilter.size() > NoteValues.MAX_TAGS_PER_NOTE) {
            throw FieldErrors.single(ErrorCode.VALIDATION_FAILED, "tagId", "SIZE", "Tối đa 20 thẻ.");
        }
        if (!tagFilter.isEmpty() && tags.findLiveIn(userId, tagFilter).size() != tagFilter.size()) {
            throw NoteValues.reference(ErrorCode.TAG_NOT_FOUND);
        }
        int size = Cursors.limit(limit);
        NoteSearchStore.Filter filter = new NoteSearchStore.Filter(mode, pinned, tagFilter, "any".equals(tagMode));
        String q = rawQ == null ? null : Normalizer.normalize(rawQ, Normalizer.Form.NFC).strip();
        if (q != null && !q.isEmpty()) {
            return search(userId, q, filter, size, cursor);
        }
        Cursors.Position after = Cursors.decode(cursor, SORT);
        Instant afterUpdatedAt = after == null ? null : instant(after.key());
        List<NoteSearchStore.Row> rows = search.page(userId, filter, afterUpdatedAt,
                after == null ? null : after.id(), size + 1);
        boolean hasMore = rows.size() > size;
        List<NoteSearchStore.Row> page = hasMore ? rows.subList(0, size) : rows;
        String next = null;
        if (hasMore) {
            NoteSearchStore.Row last = page.get(page.size() - 1);
            next = Cursors.encode(new Cursors.Position(SORT, last.updatedAt().toString(), last.id()));
        }
        return new PageResponse<>(summaries(userId, page), next, hasMore);
    }

    private PageResponse<NoteSummary> search(UUID userId, String q, NoteSearchStore.Filter filter, int size,
            String cursor) {
        if (q.codePointCount(0, q.length()) > NoteValues.QUERY_MAX) {
            throw FieldErrors.single(ErrorCode.VALIDATION_FAILED, "q", "SIZE",
                    "Tối đa " + NoteValues.QUERY_MAX + " ký tự.");
        }
        Cursors.Position after = Cursors.decode(cursor, SEARCH_SORT);
        int offset = 0;
        if (after != null) {
            try {
                offset = Integer.parseInt(after.key());
            } catch (NumberFormatException | NullPointerException e) {
                throw invalidCursor();
            }
            if (offset < 0 || offset > NoteValues.SEARCH_OFFSET_MAX) {
                throw invalidCursor();
            }
        }
        RateLimiter.Decision decision = rateLimiter.tryAcquire(RateLimitPolicy.NOTE_SEARCH_USER, userId.toString());
        if (!decision.allowed()) {
            throw new RateLimitedException(decision.retryAfterSeconds());
        }
        List<NoteSearchStore.Row> rows = search.search(userId, q, filter, offset, size + 1);
        int nextOffset = offset + size;
        boolean hasMore = rows.size() > size && nextOffset <= NoteValues.SEARCH_OFFSET_MAX;
        List<NoteSearchStore.Row> page = rows.size() > size ? rows.subList(0, size) : rows;
        String next = hasMore ? Cursors.encode(new Cursors.Position(SEARCH_SORT, String.valueOf(nextOffset),
                page.get(page.size() - 1).id())) : null;
        return new PageResponse<>(summaries(userId, page), next, hasMore);
    }

    /** GET /goals/{id}/notes: ghi chú liên kết tới Goal (D189), keyset updatedAt desc. */
    @Transactional(readOnly = true)
    public PageResponse<NoteSummary> goalNotes(UUID userId, UUID goalId, Integer limit, String cursor) {
        if (!goalApi.exists(userId, goalId)) {
            throw new NotFoundException(ErrorCode.GOAL_NOT_FOUND);
        }
        int size = Cursors.limit(limit);
        Cursors.Position after = Cursors.decode(cursor, SORT);
        Instant afterUpdatedAt = after == null ? null : instant(after.key());
        List<NoteSearchStore.Row> rows = search.linkedTo(userId, "GOAL", goalId, afterUpdatedAt,
                after == null ? null : after.id(), size + 1);
        boolean hasMore = rows.size() > size;
        List<NoteSearchStore.Row> page = hasMore ? rows.subList(0, size) : rows;
        String next = null;
        if (hasMore) {
            NoteSearchStore.Row last = page.get(page.size() - 1);
            next = Cursors.encode(new Cursors.Position(SORT, last.updatedAt().toString(), last.id()));
        }
        return new PageResponse<>(summaries(userId, page), next, hasMore);
    }

    @Transactional(readOnly = true)
    public List<TagResponse> tags(UUID userId) {
        return tags.findLive(userId).stream().map(mapper::toResponse).toList();
    }

    private List<NoteSummary> summaries(UUID userId, List<NoteSearchStore.Row> rows) {
        if (rows.isEmpty()) {
            return List.of();
        }
        Map<UUID, List<UUID>> tagsByNote = noteTags.findLiveByNotes(userId, rows.stream()
                .map(NoteSearchStore.Row::id).toList()).stream()
                .sorted(Comparator.comparing(NoteTag::getId))
                .collect(Collectors.groupingBy(NoteTag::getNoteId,
                        Collectors.mapping(NoteTag::getTagId, Collectors.toList())));
        return rows.stream().map(r -> new NoteSummary(r.id(), r.title(), r.excerpt(), r.contentLength(), r.pinned(),
                r.archivedAt(), tagsByNote.getOrDefault(r.id(), List.of()), r.version(), r.createdAt(),
                r.updatedAt())).toList();
    }

    @Transactional(readOnly = true)
    public List<NoteSyncState> noteStates(UUID userId, Collection<UUID> ids) {
        return ids.isEmpty() ? List.of() : notes.findAllOwned(userId, ids).stream()
                .map(n -> new NoteSyncState(n.getId(), n.getVersion(), n.isDeleted(), mapper.toResponse(n))).toList();
    }

    @Transactional(readOnly = true)
    public List<NoteSyncState> tagStates(UUID userId, Collection<UUID> ids) {
        return ids.isEmpty() ? List.of() : tags.findAllOwned(userId, ids).stream()
                .map(t -> new NoteSyncState(t.getId(), t.getVersion(), t.isDeleted(), mapper.toResponse(t))).toList();
    }

    @Transactional(readOnly = true)
    public List<NoteSyncState> noteTagStates(UUID userId, Collection<UUID> ids) {
        return ids.isEmpty() ? List.of() : noteTags.findAllOwned(userId, ids).stream()
                .map(x -> new NoteSyncState(x.getId(), x.getVersion(), x.isDeleted(), mapper.toResponse(x))).toList();
    }

    @Transactional(readOnly = true)
    public List<NoteSyncState> linkStates(UUID userId, Collection<UUID> ids) {
        return ids.isEmpty() ? List.of() : links.findAllOwned(userId, ids).stream()
                .map(x -> new NoteSyncState(x.getId(), x.getVersion(), x.isDeleted(), mapper.toResponse(x))).toList();
    }

    private static Instant instant(String raw) {
        try {
            return Instant.parse(raw);
        } catch (DateTimeParseException | NullPointerException e) {
            throw invalidCursor();
        }
    }

    private static RuntimeException invalidCursor() {
        return FieldErrors.single(ErrorCode.VALIDATION_FAILED, "cursor", "INVALID_VALUE",
                "Con trỏ phân trang không hợp lệ.");
    }
}
