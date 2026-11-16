package com.lifeos.identity.service;

import com.lifeos.common.config.JobHealth;
import com.lifeos.common.exception.ErrorCode;
import com.lifeos.common.exception.FieldErrors;
import com.lifeos.common.exception.NotFoundException;
import com.lifeos.common.exception.RateLimitedException;
import com.lifeos.common.exception.ValidationException;
import com.lifeos.common.privacy.UserDataPurger;
import com.lifeos.common.security.PasswordHasher;
import com.lifeos.common.security.RateLimitPolicy;
import com.lifeos.common.security.RateLimiter;
import com.lifeos.identity.dto.AccountDeletionResponse;
import com.lifeos.identity.entity.User;
import com.lifeos.identity.repository.AccountStore;
import com.lifeos.identity.repository.RefreshTokenRepository;
import com.lifeos.identity.repository.UserRepository;
import io.micrometer.core.instrument.MeterRegistry;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.HexFormat;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Xóa tài khoản (PHASE_7 D205; §4 kế hoạch: cooldown 7 ngày, mật khẩu + mã email, nhật ký kiểm toán 365 ngày).
 * Mã 6 chữ số, 15 phút, chỉ lưu băm SHA-256. Xác nhận đúng ⇒ đặt deletion_scheduled_at, thu hồi MỌI refresh token,
 * email bảo mật qua outbox. Job hằng giờ xóa cứng tài khoản đến hạn bằng {@link UserDataPurger}; nhật ký kiểm toán chỉ
 * có băm userId, thời điểm và số dòng.
 */
@Service
public class AccountDeletionService {

    static final Duration CODE_TTL = Duration.ofMinutes(15);
    static final Duration AUDIT_RETENTION = Duration.ofDays(365);
    private static final Logger log = LoggerFactory.getLogger(AccountDeletionService.class);
    private static final SecureRandom RANDOM = new SecureRandom();

    private final AccountStore store;
    private final UserRepository users;
    private final RefreshTokenRepository refreshTokens;
    private final IdentityOutboxEvents outbox;
    private final PasswordHasher passwordHasher;
    private final RateLimiter rateLimiter;
    private final UserDataPurger purger;
    private final TransactionTemplate tx;
    private final MeterRegistry metrics;
    private final Clock clock;
    private final Duration cooldown;
    private final JobHealth health;

    public AccountDeletionService(AccountStore store, UserRepository users, RefreshTokenRepository refreshTokens,
            IdentityOutboxEvents outbox, PasswordHasher passwordHasher, RateLimiter rateLimiter, UserDataPurger purger,
            TransactionTemplate tx, MeterRegistry metrics, Clock clock,
            @Value("${lifeos.account.deletion-cooldown:7d}") Duration cooldown, JobHealth health) {
        this.health = health;
        this.store = store;
        this.users = users;
        this.refreshTokens = refreshTokens;
        this.outbox = outbox;
        this.passwordHasher = passwordHasher;
        this.rateLimiter = rateLimiter;
        this.purger = purger;
        this.tx = tx;
        this.metrics = metrics;
        this.clock = clock;
        this.cooldown = cooldown;
    }

    /** POST /account/deletion/code: gửi mã xác nhận qua email (mã cũ chưa dùng bị vô hiệu). 5 lần/giờ. */
    public void requestCode(UUID userId) {
        RateLimiter.Decision decision = rateLimiter.tryAcquire(RateLimitPolicy.ACCOUNT_DELETION_CODE_USER,
                userId.toString());
        if (!decision.allowed()) {
            throw new RateLimitedException(decision.retryAfterSeconds());
        }
        String code = String.format("%06d", RANDOM.nextInt(1_000_000));
        tx.executeWithoutResult(s -> {
            Instant now = clock.instant();
            User user = users.findById(userId).orElseThrow();
            store.invalidateCodes(userId, now);
            Instant expiresAt = now.plus(CODE_TTL);
            store.insertCode(userId, sha256(code), expiresAt);
            outbox.accountDeletionCode(userId, user.getEmail(), user.getDisplayName(), code, expiresAt);
        });
    }

    /**
     * POST /account/deletion. Thứ tự: throttle sai 5 lần/15 phút → mật khẩu (400 AUTH_CURRENT_PASSWORD_WRONG) → mã
     * (400 VALIDATION_FAILED code) → đặt lịch. Đã đang chờ xóa ⇒ trả trạng thái hiện có (idempotent).
     */
    public AccountDeletionResponse schedule(UUID userId, String password, String code) {
        String key = userId.toString();
        RateLimiter.Decision decision = rateLimiter.check(RateLimitPolicy.ACCOUNT_DELETION_FAILURE_USER, key);
        if (!decision.allowed()) {
            throw new RateLimitedException(decision.retryAfterSeconds());
        }
        User user = users.findById(userId).orElseThrow(() -> wrongPassword());
        if (!passwordHasher.matches(password, user.getPasswordHash())) {
            rateLimiter.recordFailure(RateLimitPolicy.ACCOUNT_DELETION_FAILURE_USER, key);
            throw wrongPassword();
        }
        AccountDeletionResponse result = tx.execute(s -> {
            Instant now = clock.instant();
            var existing = store.deletionScheduledAt(userId);
            if (existing.isPresent()) {
                return new AccountDeletionResponse(existing.get(), true);
            }
            var active = store.activeCode(userId, now);
            if (active.isEmpty() || !MessageDigest.isEqual(active.get().codeHash().getBytes(),
                    sha256(code.strip()).getBytes()) || store.consumeCode(active.get().id(), now) != 1) {
                return null;
            }
            Instant deletionAt = now.plus(cooldown);
            store.schedule(userId, deletionAt);
            refreshTokens.revokeAllForUser(userId, now);
            outbox.accountDeletionScheduled(userId, user.getEmail(), user.getDisplayName(), deletionAt);
            return new AccountDeletionResponse(deletionAt, true);
        });
        if (result == null) {
            rateLimiter.recordFailure(RateLimitPolicy.ACCOUNT_DELETION_FAILURE_USER, key);
            throw FieldErrors.single(ErrorCode.VALIDATION_FAILED, "code", "INVALID_VALUE",
                    "Mã xác nhận không đúng hoặc đã hết hạn.");
        }
        rateLimiter.reset(RateLimitPolicy.ACCOUNT_DELETION_FAILURE_USER, key);
        metrics.counter("lifeos.account.deletion", "event", "scheduled").increment();
        return result;
    }

    public AccountDeletionResponse status(UUID userId) {
        return store.deletionScheduledAt(userId).map(at -> new AccountDeletionResponse(at, true))
                .orElseThrow(() -> new NotFoundException(ErrorCode.ACCOUNT_DELETION_NOT_FOUND));
    }

    public void cancel(UUID userId) {
        if (tx.execute(s -> store.cancel(userId)) != 1) {
            throw new NotFoundException(ErrorCode.ACCOUNT_DELETION_NOT_FOUND);
        }
        metrics.counter("lifeos.account.deletion", "event", "cancelled").increment();
    }

    /** Job hằng giờ: xóa cứng tài khoản đến hạn (≤ 20/lượt), dọn nhật ký kiểm toán &gt; 365 ngày. */
    @Scheduled(cron = "${lifeos.account.purge-cron:0 20 * * * *}", zone = "UTC")
    public int purgeDue() {
        int done = 0;
        for (UUID userId : store.dueForDeletion(clock.instant(), 20)) {
            for (String ref : store.fileRefsOf(userId)) {
                try {
                    Files.deleteIfExists(Path.of(ref));
                } catch (Exception e) {
                    log.warn("account_purge_file_delete_failed error={}", e.getClass().getSimpleName());
                }
            }
            long rows = purger.purge(userId);
            store.audit(sha256(userId.toString()), clock.instant(), rows);
            metrics.counter("lifeos.account.deletion", "event", "purged").increment();
            done++;
        }
        store.deleteAuditBefore(clock.instant().minus(AUDIT_RETENTION));
        health.succeeded(JobHealth.Job.PURGE);
        return done;
    }

    private static ValidationException wrongPassword() {
        return new ValidationException(ErrorCode.AUTH_CURRENT_PASSWORD_WRONG, HttpStatus.BAD_REQUEST,
                ErrorCode.AUTH_CURRENT_PASSWORD_WRONG.title());
    }

    static String sha256(String value) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(java.nio.charset.StandardCharsets.UTF_8)));
        } catch (java.security.NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
