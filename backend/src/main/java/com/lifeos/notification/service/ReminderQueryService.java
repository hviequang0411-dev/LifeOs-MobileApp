package com.lifeos.notification.service;

import com.lifeos.common.response.PageResponse;
import com.lifeos.common.web.Cursors;
import com.lifeos.notification.dto.ReminderResponse;
import com.lifeos.notification.dto.ReminderSyncState;
import com.lifeos.notification.entity.Reminder;
import com.lifeos.notification.mapper.ReminderMapper;
import com.lifeos.notification.repository.ReminderRepository;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Limit;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Đọc reminder của user hiện tại (PHASE_4 §6) và trạng thái cho snapshot sync. */
@Service
public class ReminderQueryService {

    static final String SORT = "createdAt:desc";

    private final ReminderRepository reminders;
    private final ReminderMapper mapper;

    public ReminderQueryService(ReminderRepository reminders, ReminderMapper mapper) {
        this.reminders = reminders;
        this.mapper = mapper;
    }

    @Transactional(readOnly = true)
    public ReminderResponse get(UUID userId, UUID id) {
        return mapper.toResponse(reminders.findActive(id, userId).orElseThrow(ReminderCommandService::notFound));
    }

    @Transactional(readOnly = true)
    public PageResponse<ReminderResponse> list(UUID userId, Integer limit, String cursor) {
        int size = Cursors.limit(limit);
        Cursors.Position after = Cursors.decode(cursor, SORT);
        Instant afterAt = null;
        if (after != null) {
            try {
                afterAt = Instant.parse(after.key());
            } catch (DateTimeParseException | NullPointerException e) {
                throw com.lifeos.common.exception.FieldErrors.single(
                        com.lifeos.common.exception.ErrorCode.VALIDATION_FAILED, "cursor", "INVALID_VALUE",
                        "Con trỏ phân trang không hợp lệ.");
            }
        }
        List<Reminder> rows = reminders.page(userId, after == null, afterAt, after == null ? null : after.id(),
                Limit.of(size + 1));
        boolean hasMore = rows.size() > size;
        List<Reminder> page = hasMore ? rows.subList(0, size) : rows;
        String next = null;
        if (hasMore) {
            Reminder last = page.get(page.size() - 1);
            next = Cursors.encode(new Cursors.Position(SORT, last.getCreatedAt().toString(), last.getId()));
        }
        return new PageResponse<>(page.stream().map(mapper::toResponse).toList(), next, hasMore);
    }

    @Transactional(readOnly = true)
    public List<ReminderSyncState> syncStates(UUID userId, Collection<UUID> ids) {
        if (ids.isEmpty()) {
            return List.of();
        }
        return reminders.findAllOwned(userId, ids).stream()
                .map(r -> new ReminderSyncState(mapper.toResponse(r), r.isDeleted())).toList();
    }
}
