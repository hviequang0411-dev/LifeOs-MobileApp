package com.lifeos.identity.service;

import com.lifeos.common.config.JobHealth;
import com.lifeos.common.exception.BusinessException;
import com.lifeos.common.exception.ErrorCode;
import com.lifeos.common.exception.NotFoundException;
import com.lifeos.common.privacy.UserDataExporter;
import com.lifeos.identity.dto.ExportResponse;
import com.lifeos.identity.repository.AccountStore;
import io.micrometer.core.instrument.MeterRegistry;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Xuất dữ liệu cá nhân (PHASE_7 D204; §4 kế hoạch: thư mục cục bộ cấu hình được, ≤ 500 MB/tệp, CSV có BOM). Yêu cầu ≤ 2
 * lần/24 giờ/user, một yêu cầu chạy một lúc (yêu cầu mới khi đang có PENDING/RUNNING ⇒ trả yêu cầu đó) — cả hai là
 * giới hạn CỨNG, đúng cả khi gọi đồng thời (khóa dòng users + chỉ mục UNIQUE một yêu cầu đang chạy). Job nền chạy
 * NGOÀI transaction nghiệp vụ; tệp hết hạn sau 24 giờ, tải tối đa 5 lần, chỉ chủ sở hữu tải được (xác thực).
 */
@Service
public class DataExportService {

    static final int DAILY_LIMIT = 2;
    static final int MAX_DOWNLOADS = 5;
    static final Duration TTL = Duration.ofHours(24);
    static final Duration STALE = Duration.ofHours(2);
    private static final Logger log = LoggerFactory.getLogger(DataExportService.class);

    private final AccountStore store;
    private final UserDataExporter exporter;
    private final IdentityOutboxEvents outbox;
    private final TransactionTemplate tx;
    private final MeterRegistry metrics;
    private final Clock clock;
    private final Path dir;
    private final long maxBytes;
    private final JobHealth health;

    public DataExportService(AccountStore store, UserDataExporter exporter, IdentityOutboxEvents outbox,
            TransactionTemplate tx, MeterRegistry metrics, Clock clock,
            @Value("${lifeos.export.dir:${java.io.tmpdir}/lifeos-exports}") String dir,
            @Value("${lifeos.export.max-bytes:524288000}") long maxBytes, JobHealth health) {
        this.health = health;
        this.store = store;
        this.exporter = exporter;
        this.outbox = outbox;
        this.tx = tx;
        this.metrics = metrics;
        this.clock = clock;
        this.dir = Path.of(dir);
        this.maxBytes = maxBytes;
    }

    /** @return yêu cầu mới (created = true) hoặc yêu cầu đang chờ/chạy */
    public Requested request(UUID userId) {
        return tx.execute(s -> {
            // Đợt sửa F5: giới hạn cứng cả khi gọi đồng thời — khóa dòng users trước khi kiểm/chèn; chỉ mục UNIQUE
            // ux_data_exports_user_active (V13) là lớp bảo vệ thứ hai ở DB.
            if (!store.lockUserForExport(userId)) {
                throw notFound();
            }
            Instant now = clock.instant();
            var active = store.activeExport(userId);
            if (active.isPresent()) {
                return new Requested(view(active.get()), false);
            }
            if (store.countRequestedSince(userId, now.minus(Duration.ofHours(24))) >= DAILY_LIMIT) {
                throw new BusinessException(ErrorCode.EXPORT_LIMIT_REACHED, HttpStatus.TOO_MANY_REQUESTS,
                        ErrorCode.EXPORT_LIMIT_REACHED.title());
            }
            UUID id = UUID.randomUUID();
            store.insertExport(id, userId, now);
            return new Requested(view(store.findExport(userId, id).orElseThrow()), true);
        });
    }

    public record Requested(ExportResponse export, boolean created) {
    }

    public ExportResponse get(UUID userId, UUID id) {
        return view(store.findExport(userId, id).orElseThrow(DataExportService::notFound));
    }

    /** Tệp để tải: READY, chưa hết hạn, còn lượt tải; tăng đếm lượt tải. */
    public Path download(UUID userId, UUID id) {
        AccountStore.Export e = store.findExport(userId, id).orElseThrow(DataExportService::notFound);
        Instant now = clock.instant();
        if ("EXPIRED".equals(e.status()) || ("READY".equals(e.status()) && !e.expiresAt().isAfter(now))) {
            throw new BusinessException(ErrorCode.EXPORT_EXPIRED, HttpStatus.GONE, ErrorCode.EXPORT_EXPIRED.title());
        }
        if (!"READY".equals(e.status()) || e.fileRef() == null || !Files.exists(Path.of(e.fileRef()))) {
            throw new BusinessException(ErrorCode.EXPORT_NOT_READY, HttpStatus.CONFLICT,
                    ErrorCode.EXPORT_NOT_READY.title());
        }
        if (store.countDownload(id, MAX_DOWNLOADS) != 1) {
            throw new BusinessException(ErrorCode.EXPORT_EXPIRED, HttpStatus.GONE, ErrorCode.EXPORT_EXPIRED.title());
        }
        return Path.of(e.fileRef());
    }

    /** Job nền: xử lý các yêu cầu PENDING lần lượt (mỗi lần một yêu cầu); RUNNING quá 2 giờ ⇒ FAILED STALE. */
    @Scheduled(fixedDelayString = "${lifeos.export.poll-interval:30s}")
    public int runPending() {
        store.failStale(clock.instant().minus(STALE), clock.instant());
        int done = 0;
        while (true) {
            var claimed = tx.execute(s -> store.claimPending(clock.instant()));
            if (claimed == null || claimed.isEmpty()) {
                // Lượt quét hàng đợi xong (từng yêu cầu hỏng đã được ghi FAILED riêng): job còn sống.
                health.succeeded(JobHealth.Job.EXPORT);
                return done;
            }
            run(claimed.get());
            done++;
        }
    }

    private void run(AccountStore.Export e) {
        Path file = dir.resolve(e.id() + ".zip");
        try {
            long size = exporter.export(e.userId(), file, maxBytes, clock.instant());
            tx.executeWithoutResult(s -> {
                Instant now = clock.instant();
                store.markReady(e.id(), now, now.plus(TTL), file.toString(), size);
                outbox.systemNotification(e.userId(), "export.ready", Map.of("type", "export", "id",
                        e.id().toString()));
            });
            metrics.counter("lifeos.export.completed", "result", "ready").increment();
        } catch (UserDataExporter.TooLargeException tooLarge) {
            fail(e, file, "EXPORT_TOO_LARGE");
        } catch (Exception ex) {
            log.warn("export_failed error={}", ex.getClass().getSimpleName());
            fail(e, file, "EXPORT_FAILED");
        }
    }

    private void fail(AccountStore.Export e, Path file, String code) {
        try {
            Files.deleteIfExists(file);
        } catch (Exception ignored) {
            // tệp dở dang sẽ được dọn bởi lần chạy sau của expire()
        }
        tx.executeWithoutResult(s -> store.markFailed(e.id(), clock.instant(), code));
        metrics.counter("lifeos.export.completed", "result", "failed").increment();
    }

    /** Job hằng giờ: xóa tệp hết hạn, đánh dấu EXPIRED. */
    @Scheduled(cron = "${lifeos.export.expire-cron:0 5 * * * *}", zone = "UTC")
    public int expire() {
        int n = 0;
        for (AccountStore.Export e : store.expiredReady(clock.instant(), 500)) {
            try {
                Files.deleteIfExists(Path.of(e.fileRef()));
            } catch (Exception ex) {
                log.warn("export_expire_delete_failed error={}", ex.getClass().getSimpleName());
            }
            store.markExpired(e.id());
            n++;
        }
        return n;
    }

    private static ExportResponse view(AccountStore.Export e) {
        return new ExportResponse(e.id(), e.status(), e.requestedAt(), e.finishedAt(), e.expiresAt(), e.sizeBytes(),
                e.errorCode(), Math.max(0, MAX_DOWNLOADS - e.downloadCount()));
    }

    private static NotFoundException notFound() {
        return new NotFoundException(ErrorCode.EXPORT_NOT_FOUND);
    }
}
