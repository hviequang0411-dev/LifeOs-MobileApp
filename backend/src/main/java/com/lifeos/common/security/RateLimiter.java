package com.lifeos.common.security;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.Objects;
import org.springframework.stereotype.Component;

/**
 * Bộ đếm tần suất trong bộ nhớ (D24, D49): cửa sổ cố định bắt đầu từ lần đếm đầu tiên của khóa; hết cửa sổ thì đếm
 * lại từ đầu. Retry-After = thời gian còn lại của cửa sổ (làm tròn lên, tối thiểu 1 giây). Thời gian lấy từ Clock để
 * test được; expireAfterWrite của Caffeine chỉ để dọn bộ nhớ. Khởi động lại xóa bộ đếm (chấp nhận ở Phase 1).
 * <p>
 * Hai kiểu dùng:
 * <ul>
 *   <li>{@link #tryAcquire}: đếm mọi yêu cầu (register, refresh, logout, API theo user…).</li>
 *   <li>{@link #check} + {@link #recordFailure} + {@link #reset}: chỉ đếm lần thất bại (login, change-password).</li>
 * </ul>
 */
@Component
public class RateLimiter {

    /** Giới hạn bộ nhớ: khóa ít dùng nhất bị loại trước. */
    private static final long MAX_KEYS = 500_000;

    private final Clock clock;
    private final Cache<String, Window> windows;

    public RateLimiter(Clock clock) {
        this.clock = clock;
        Duration longest = Arrays.stream(RateLimitPolicy.values())
                .map(RateLimitPolicy::window)
                .max(Duration::compareTo)
                .orElseThrow();
        this.windows = Caffeine.newBuilder().maximumSize(MAX_KEYS).expireAfterWrite(longest).build();
    }

    /** Đếm một yêu cầu; bị từ chối nếu đã vượt giới hạn trong cửa sổ hiện tại. */
    public Decision tryAcquire(RateLimitPolicy policy, String key) {
        Instant now = clock.instant();
        Window updated = windows.asMap().compute(cacheKey(policy, key),
                (k, current) -> active(current, policy, now) ? current.increment() : new Window(now, 1));
        return decide(policy, updated, now, updated.count() > policy.limit());
    }

    /** Kiểm tra mà không đếm: bị chặn nếu số lần thất bại đã đạt giới hạn. */
    public Decision check(RateLimitPolicy policy, String key) {
        Instant now = clock.instant();
        Window current = windows.getIfPresent(cacheKey(policy, key));
        if (!active(current, policy, now)) {
            return Decision.ALLOWED;
        }
        return decide(policy, current, now, current.count() >= policy.limit());
    }

    /** Ghi nhận một lần thất bại. */
    public void recordFailure(RateLimitPolicy policy, String key) {
        Instant now = clock.instant();
        windows.asMap().compute(cacheKey(policy, key),
                (k, current) -> active(current, policy, now) ? current.increment() : new Window(now, 1));
    }

    /** Xóa bộ đếm (vd đăng nhập đúng đặt lại bộ đếm (email, IP)). */
    public void reset(RateLimitPolicy policy, String key) {
        windows.invalidate(cacheKey(policy, key));
    }

    private static Decision decide(RateLimitPolicy policy, Window window, Instant now, boolean blocked) {
        if (!blocked) {
            return Decision.ALLOWED;
        }
        Duration remaining = Duration.between(now, window.start().plus(policy.window()));
        long seconds = Math.max(1, (remaining.toMillis() + 999) / 1000);
        return new Decision(false, seconds);
    }

    private static boolean active(Window window, RateLimitPolicy policy, Instant now) {
        return window != null && now.isBefore(window.start().plus(policy.window()));
    }

    private static String cacheKey(RateLimitPolicy policy, String key) {
        Objects.requireNonNull(key, "key");
        return policy.name() + '|' + key;
    }

    private record Window(Instant start, int count) {
        Window increment() {
            return new Window(start, count == Integer.MAX_VALUE ? count : count + 1);
        }
    }

    /** Kết quả kiểm tra; retryAfterSeconds chỉ có nghĩa khi bị từ chối. */
    public record Decision(boolean allowed, long retryAfterSeconds) {
        public static final Decision ALLOWED = new Decision(true, 0);
    }
}
