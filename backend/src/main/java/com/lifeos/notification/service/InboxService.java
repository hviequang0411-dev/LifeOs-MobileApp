package com.lifeos.notification.service;

import com.lifeos.common.exception.ErrorCode;
import com.lifeos.common.exception.FieldErrors;
import com.lifeos.common.exception.NotFoundException;
import com.lifeos.common.response.PageResponse;
import com.lifeos.common.web.Cursors;
import com.lifeos.notification.dto.NotificationResponse;
import com.lifeos.notification.dto.UnreadCountResponse;
import com.lifeos.notification.repository.NotificationStore;
import java.time.Clock;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;

/**
 * Hộp thư trong ứng dụng (FR-NTF-06, D142): không đồng bộ qua change_logs; đọc bằng cursor; đánh dấu đọc idempotent,
 * không cần version; giữ 90 ngày. Thông báo của người khác → 404 như không tồn tại.
 */
@Service
public class InboxService {

    static final String SORT = "createdAt:desc";
    private static final TypeReference<Map<String, Object>> MAP = new TypeReference<>() {
    };

    private final NotificationStore store;
    private final JsonMapper json;
    private final Clock clock;

    public InboxService(NotificationStore store, JsonMapper json, Clock clock) {
        this.store = store;
        this.json = json;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public PageResponse<NotificationResponse> list(UUID userId, String unread, Integer limit, String cursor) {
        boolean unreadOnly = "true".equals(unread);
        if (unread != null && !"true".equals(unread) && !"false".equals(unread)) {
            throw FieldErrors.single(ErrorCode.VALIDATION_FAILED, "unread", "INVALID_VALUE", "unread là true/false.");
        }
        int size = Cursors.limit(limit);
        Cursors.Position after = Cursors.decode(cursor, SORT);
        Instant afterAt = null;
        if (after != null) {
            try {
                afterAt = Instant.parse(after.key());
            } catch (DateTimeParseException | NullPointerException e) {
                throw FieldErrors.single(ErrorCode.VALIDATION_FAILED, "cursor", "INVALID_VALUE",
                        "Con trỏ phân trang không hợp lệ.");
            }
        }
        List<NotificationStore.NotificationRow> rows = store.inbox(userId, unreadOnly, clock.instant(), afterAt,
                after == null ? null : after.id(), size + 1);
        boolean hasMore = rows.size() > size;
        List<NotificationStore.NotificationRow> page = hasMore ? rows.subList(0, size) : rows;
        String next = null;
        if (hasMore) {
            NotificationStore.NotificationRow last = page.get(page.size() - 1);
            next = Cursors.encode(new Cursors.Position(SORT, last.createdAt().toString(), last.id()));
        }
        return new PageResponse<>(page.stream().map(this::toResponse).toList(), next, hasMore);
    }

    @Transactional(readOnly = true)
    public UnreadCountResponse unreadCount(UUID userId) {
        return new UnreadCountResponse(store.unreadCount(userId, clock.instant()));
    }

    @Transactional
    public NotificationResponse markRead(UUID userId, UUID id) {
        if (store.markRead(userId, id, clock.instant()) == 0) {
            throw new NotFoundException(ErrorCode.NOTIFICATION_NOT_FOUND);
        }
        return store.notification(id).map(this::toResponse).orElseThrow();
    }

    @Transactional
    public int markAllRead(UUID userId) {
        return store.markAllRead(userId, clock.instant());
    }

    @Transactional
    public void delete(UUID userId, UUID id) {
        if (store.deleteNotification(userId, id) == 0) {
            throw new NotFoundException(ErrorCode.NOTIFICATION_NOT_FOUND);
        }
    }

    private NotificationResponse toResponse(NotificationStore.NotificationRow n) {
        return new NotificationResponse(n.id(), n.category(), n.templateKey(), n.title(), n.body(),
                json.readValue(n.data(), MAP), n.readAt(), n.createdAt(), n.expiresAt());
    }
}
