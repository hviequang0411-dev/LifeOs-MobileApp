package com.lifeos.sync.service;

import com.lifeos.common.exception.BusinessException;
import com.lifeos.common.exception.ErrorCode;
import com.lifeos.common.exception.FieldErrors;
import com.lifeos.common.exception.NotFoundException;
import com.lifeos.common.exception.RateLimitedException;
import com.lifeos.common.security.RateLimitPolicy;
import com.lifeos.common.security.RateLimiter;
import com.lifeos.common.sync.EntitySnapshot;
import com.lifeos.common.sync.SyncEntityHandler;
import com.lifeos.sync.dto.ServerSnapshot;
import com.lifeos.sync.dto.SnapshotResponse;
import com.lifeos.sync.repository.RetentionStore;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.UUID;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.json.JsonMapper;

/**
 * Snapshot trạng thái hiện tại cho full resync (PHASE_5 D151, D152, D174). Trang đầu ghi {@code snapshotSeq =
 * last_seq} và cấp {@code snapshotToken} ký HMAC gắn userId, hạn 30 phút; các trang sau gửi lại token và {@code next}
 * (vị trí "entityType:id"). Mỗi trang đọc trong REPEATABLE READ READ ONLY; bản ghi đổi sau snapshotSeq đến qua pull
 * (pull state-based, idempotent). Tombstone không nằm trong snapshot. Trang ≤ 1000 bản ghi và ≤ 1 MB (kiểm trước khi
 * thêm từng bản ghi — đợt sửa F4).
 * <p>
 * Khóa HMAC lấy từ secret {@code SNAPSHOT_HMAC_SECRET} ({@code lifeos.sync.snapshot-hmac-secret}, ≥ 32 ký tự; đợt sửa
 * F9): token còn hiệu lực sau khi khởi động lại và giữa các instance khi rolling deploy. Không có mặc định ở
 * application.yml — thiếu ở production thì ứng dụng không khởi động; profile dev/test có giá trị riêng.
 */
@Service
public class SyncSnapshotService {

    static final Duration TTL = Duration.ofMinutes(30);
    static final int MAX_ITEMS = 1000;
    static final int DEFAULT_ITEMS = 500;
    static final int MAX_BYTES = 1_000_000;
    /** Phần vỏ response ngoài các bản ghi: snapshotToken, snapshotSeq, next, ngoặc. */
    static final int ENVELOPE_RESERVE = 2_048;
    /** Phần vỏ của một nhóm loại ({"type":"…","items":[…]},) không kể tên loại. */
    static final int GROUP_RESERVE = 32;
    static final int SECRET_MIN_LENGTH = 32;

    private final SyncHandlerRegistry handlers;
    private final RetentionStore store;
    private final RateLimiter rateLimiter;
    private final SyncMetrics metrics;
    private final JsonMapper json;
    private final Clock clock;
    private final byte[] key;

    public SyncSnapshotService(SyncHandlerRegistry handlers, RetentionStore store, RateLimiter rateLimiter,
            SyncMetrics metrics, JsonMapper json, Clock clock,
            @Value("${lifeos.sync.snapshot-hmac-secret}") String secret) {
        this.handlers = handlers;
        this.store = store;
        this.rateLimiter = rateLimiter;
        this.metrics = metrics;
        this.json = json;
        this.clock = clock;
        if (secret == null || secret.length() < SECRET_MIN_LENGTH) {
            throw new IllegalStateException("lifeos.sync.snapshot-hmac-secret (SNAPSHOT_HMAC_SECRET) phải có ít nhất "
                    + SECRET_MIN_LENGTH + " ký tự");
        }
        this.key = secret.getBytes(StandardCharsets.UTF_8);
    }

    private record Session(long snapshotSeq, Instant expiresAt) {
    }

    @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
    public SnapshotResponse page(UUID userId, String token, String next, Integer limit) {
        long started = System.nanoTime();
        int size = limit == null ? DEFAULT_ITEMS : limit;
        if (size < 1 || size > MAX_ITEMS) {
            throw FieldErrors.single(ErrorCode.VALIDATION_FAILED, "limit", "SIZE", "limit từ 1 đến " + MAX_ITEMS + ".");
        }
        Session session;
        String type = null;
        UUID afterId = null;
        if (token == null) {
            RateLimiter.Decision decision = rateLimiter.tryAcquire(RateLimitPolicy.SYNC_SNAPSHOT_USER,
                    userId.toString());
            if (!decision.allowed()) {
                throw new RateLimitedException(decision.retryAfterSeconds());
            }
            session = new Session(store.lastSeq(userId), clock.instant().plus(TTL));
            token = sign(userId, session);
        } else {
            session = verify(userId, token);
            if (next != null) {
                String[] parts = next.split(":", 2);
                if (parts.length != 2 || handlers.find(parts[0]).isEmpty()) {
                    throw FieldErrors.single(ErrorCode.VALIDATION_FAILED, "next", "INVALID_VALUE",
                            "Vị trí trang không hợp lệ.");
                }
                type = parts[0];
                try {
                    afterId = parts[1].isEmpty() ? null : UUID.fromString(parts[1]);
                } catch (IllegalArgumentException e) {
                    throw FieldErrors.single(ErrorCode.VALIDATION_FAILED, "next", "INVALID_VALUE",
                            "Vị trí trang không hợp lệ.");
                }
            }
        }
        List<SyncEntityHandler> all = handlers.all();
        int startIndex = 0;
        if (type != null) {
            for (int i = 0; i < all.size(); i++) {
                if (all.get(i).entityType().equals(type)) {
                    startIndex = i;
                }
            }
        }
        // D174 (đợt sửa F4): trần 1 MB được kiểm TRƯỚC khi thêm từng bản ghi. Ngân sách = trần − phần vỏ response;
        // mỗi bản ghi tính kích thước JSON + dấu phẩy, mỗi nhóm loại tính thêm phần vỏ nhóm. Trang dừng ngay trước
        // bản ghi làm vượt ngân sách và `next` trỏ tới bản ghi cuối ĐÃ đưa vào trang — không mất, không lặp. Ngoại lệ
        // duy nhất: trang luôn chứa ít nhất một bản ghi để phân trang tiến được (một bản ghi tối đa ≈ 192 KB).
        int budget = MAX_BYTES - ENVELOPE_RESERVE;
        List<SnapshotResponse.EntityPage> entities = new ArrayList<>();
        int remaining = size;
        int bytes = 0;
        int count = 0;
        String nextPosition = null;
        for (int index = startIndex; index < all.size(); index++) {
            SyncEntityHandler handler = all.get(index);
            UUID after = index == startIndex ? afterId : null;
            List<ServerSnapshot> items = new ArrayList<>();
            boolean exhausted = false;
            boolean full = false;
            while (remaining > 0 && !full) {
                int want = Math.min(remaining, batchSize(budget - bytes, bytes, count));
                List<UUID> ids = store.liveIds(handler.table(), userId, after, want);
                java.util.Map<UUID, EntitySnapshot> loaded = new java.util.HashMap<>();
                if (!ids.isEmpty()) {
                    for (EntitySnapshot snapshot : handler.loadSnapshots(userId, ids)) {
                        loaded.put(snapshot.entityId(), snapshot);
                    }
                }
                for (UUID id : ids) {
                    EntitySnapshot snapshot = loaded.get(id);
                    if (snapshot != null && !snapshot.deleted()) {
                        ServerSnapshot item = new ServerSnapshot(snapshot.entityType(), snapshot.entityId(),
                                snapshot.version(), false, snapshot.data());
                        int cost = json.writeValueAsBytes(item).length + 1
                                + (items.isEmpty() ? GROUP_RESERVE + handler.entityType().length() : 0);
                        if (count > 0 && bytes + cost > budget) {
                            full = true;
                            break;
                        }
                        items.add(item);
                        bytes += cost;
                        count++;
                    }
                    after = id;
                    remaining--;
                }
                if (!full && ids.size() < want) {
                    exhausted = true;
                    break;
                }
            }
            if (!items.isEmpty()) {
                entities.add(new SnapshotResponse.EntityPage(handler.entityType(), items));
            }
            if (!exhausted) {
                nextPosition = handler.entityType() + ":" + (after == null ? "" : after.toString());
                break;
            }
        }
        metrics.snapshot(Duration.ofNanos(System.nanoTime() - started), count);
        return new SnapshotResponse(token, session.snapshotSeq(), entities, nextPosition);
    }

    /**
     * Số id nạp mỗi lô: lô đầu 50; sau đó ước lượng theo kích thước trung bình đã thấy để không nạp hàng chục MB
     * (200 ghi chú lớn) rồi bỏ đi vì ngân sách trang chỉ còn chỗ cho vài bản ghi.
     */
    private static int batchSize(int budgetLeft, int bytes, int count) {
        if (count == 0) {
            return 50;
        }
        int average = Math.max(1, bytes / count);
        return Math.max(1, Math.min(200, budgetLeft / average + 1));
    }

    /** GET /sync/entities/{type}/{id}: bản server hiện tại (kể cả tombstone còn giữ); không có / của người khác → 404. */
    @Transactional(readOnly = true)
    public ServerSnapshot entity(UUID userId, String type, UUID id) {
        RateLimiter.Decision decision = rateLimiter.tryAcquire(RateLimitPolicy.SYNC_ENTITY_USER, userId.toString());
        if (!decision.allowed()) {
            throw new RateLimitedException(decision.retryAfterSeconds());
        }
        SyncEntityHandler handler = handlers.find(type)
                .orElseThrow(() -> new BusinessException(ErrorCode.SYNC_UNKNOWN_ENTITY_TYPE));
        List<EntitySnapshot> found;
        try {
            found = handler.loadSnapshots(userId, List.of(id));
        } catch (com.lifeos.common.exception.UnauthorizedException e) {
            // user_settings chưa có dòng: cùng 404 như không tồn tại (IDOR, SYN-A19).
            found = List.of();
        }
        return found.stream().filter(s -> s.entityId().equals(id)).findFirst()
                .map(s -> new ServerSnapshot(s.entityType(), s.entityId(), s.version(), s.deleted(), s.data()))
                .orElseThrow(() -> new NotFoundException(ErrorCode.NOT_FOUND));
    }

    private String sign(UUID userId, Session session) {
        String payload = session.snapshotSeq() + "|" + session.expiresAt().getEpochSecond();
        Base64.Encoder b64 = Base64.getUrlEncoder().withoutPadding();
        return b64.encodeToString(payload.getBytes(StandardCharsets.UTF_8)) + "."
                + b64.encodeToString(mac(userId + "|" + payload));
    }

    private Session verify(UUID userId, String token) {
        String[] parts = token.split("\\.");
        try {
            if (parts.length == 2) {
                String payload = new String(Base64.getUrlDecoder().decode(parts[0]), StandardCharsets.UTF_8);
                if (MessageDigest.isEqual(Base64.getUrlDecoder().decode(parts[1]), mac(userId + "|" + payload))) {
                    String[] fields = payload.split("\\|");
                    Session session = new Session(Long.parseLong(fields[0]), Instant.ofEpochSecond(
                            Long.parseLong(fields[1])));
                    if (session.expiresAt().isBefore(clock.instant())) {
                        throw new BusinessException(ErrorCode.SYNC_SNAPSHOT_EXPIRED);
                    }
                    return session;
                }
            }
        } catch (IllegalArgumentException | ArrayIndexOutOfBoundsException e) {
            // rơi xuống lỗi chung
        }
        throw FieldErrors.single(ErrorCode.VALIDATION_FAILED, "snapshotToken", "INVALID_VALUE",
                "snapshotToken không hợp lệ.");
    }

    private byte[] mac(String payload) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(key, "HmacSHA256"));
            return mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException(e);
        }
    }
}
