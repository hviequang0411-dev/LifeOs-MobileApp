# Báo cáo Giai đoạn 4 backend — Outbox worker + gửi email

> **Trạng thái:** hoàn thành, **đã được người dùng duyệt ngày 2026-11-07** (kèm các quyết định `[PROPOSED]` P1–P14 ở §3).
> **Phạm vi:** Giai đoạn 4 trong lộ trình dựng backend cho **Phase 1 sản phẩm**. Handler email nằm trong module
> `identity` (không phải module `notification` — việc của Phase 4 sản phẩm, `docs/phases/PHASE_4_CALENDAR_NOTIFICATION.md`).
> **Đặc tả áp dụng:** `SYSTEM_ARCHITECTURE.md` §3.11, §3.12, §3.14, §7, §12, D23, D28; `JAVA_SPRING_RULES.md` §3, §10;
> `SECURITY_RULES.md` §3.4, §9.5–9.6, §11, §12, §13.6; `DATABASE_DESIGN.md` §4.4, §7; `TESTING_RULES.md` §2, §4, §7 (LOG-02), §11.
> **Kết quả cuối:** `mvn -f backend/pom.xml clean verify` xanh: **83 unit + 442 IT = 525 test**, 0 bỏ qua,
> 0 `@Disabled`; 3 phút 24 giây (cần Docker). `docker build` + `docker compose up` (backend + PostgreSQL + mailpit) đã chạy
> thử trên máy dev: cả ba dịch vụ healthy, đăng ký → email xác minh tới mailpit, sự kiện `SENT` với payload `{}`.

---

## 1. Tóm tắt

Mục tiêu `SYSTEM_ARCHITECTURE.md` §12 "Email xác minh đi qua outbox, worker gửi được sang mailpit" đã đạt và được kiểm
đầu-cuối bằng `MailpitDeliveryIT`: app thật (scheduler bật) → SMTP thật → container mailpit → token trong email dùng được.

| Thành phần | Chức năng |
|---|---|
| `common.outbox.OutboxWorker` | Mỗi 2 giây: thu hồi lease quá hạn → nhận việc đến hạn bằng `FOR UPDATE SKIP LOCKED` (đặt `PROCESSING` + `locked_until`) → gọi handler **ngoài transaction** → `SENT` / `PENDING` + backoff mũ / `FAILED`. Kết thúc luôn ghi đè payload `{}` (D37, LOG-02). Chỉ ghi kết quả khi còn giữ lease |
| `common.outbox.OutboxStore` | SQL JdbcClient (D30); mỗi thao tác một câu lệnh autocommit = một transaction ngắn |
| `common.outbox.OutboxEventHandler`, `OutboxPermanentFailure`, `OutboxEvent`, `OutboxProperties` | Hợp đồng handler (D17), lỗi vĩnh viễn, cấu hình |
| `common.outbox.OutboxCleanupJob` | Hằng ngày xóa outbox `SENT`/`FAILED` hoàn tất quá 7 ngày (§3.12) |
| `common.mail.EmailSender`, `SmtpEmailSender`, `EmailMessage`, `MailProperties` | Gửi email văn bản thuần qua SMTP (`spring-boot-starter-mail`, đã nằm trong danh sách thư viện được duyệt cho Giai đoạn 4); Message-ID = id sự kiện |
| `identity.outbox.*EmailHandler` | `EMAIL_VERIFICATION`, `PASSWORD_RESET`, `PASSWORD_CHANGED` (D54/D76): mẫu cố định, liên kết chỉ từ `APP_BASE_URL` |
| `identity.service.TokenCleanupService` | Hằng ngày xóa refresh/verification/reset token hết hạn quá 7 ngày (§3.12) |
| `common.config.SchedulingConfig` | `@EnableScheduling`, tắt được bằng `lifeos.scheduling.enabled=false` |
| `Dockerfile`, `docker-compose.yml` | Image nhiều tầng, user không phải root (§7); compose thêm dịch vụ `backend` cạnh PostgreSQL + mailpit (§12) |

---

## 2. Thay đổi mã và cấu hình

- **pom:** thêm `spring-boot-starter-mail` (thư viện duy nhất thêm; có trong danh sách duyệt).
- **Main (mới):** `common.outbox` (6 lớp), `common.mail` (4 lớp), `common.config.SchedulingConfig`, `identity.outbox` (4 lớp),
  `identity.service.TokenCleanupService`; phương thức `deleteExpiredBefore` ở 3 repository token.
- **Không đổi:** migration V1–V4 (bảng `outbox_events` đủ cột), luồng ghi outbox của Giai đoạn 1 (`OutboxWriter`,
  `IdentityOutboxEvents`), mọi API REST/sync (OpenAPI không đổi).
- **Cấu hình (`application.yml`):** `spring.mail.*` (SMTP_HOST bắt buộc, timeout 10s), `lifeos.mail.from` (MAIL_FROM),
  `lifeos.mail.app-base-url` (APP_BASE_URL), `lifeos.outbox.*`, `lifeos.cleanup.cron`, `lifeos.scheduling.enabled`.
  `application-dev.yml`: mặc định trỏ vào mailpit (localhost:1025, không auth/TLS).
- **Test hạ tầng:** `IntegrationTestBase` tắt scheduler và đặt cấu hình mail test cho MỌI context test (kể cả profile
  dev/prod) — tránh worker nền xử lý sự kiện của test khác; `TestMailConfig` + `RecordingEmailSender` (stub, TESTING_RULES §2.3).
- **README backend:** chạy cả stack bằng compose, biến môi trường mới, mô tả worker/job.

---

## 3. Quyết định `[PROPOSED]` (tài liệu không quy định — đã chọn phương án đơn giản, an toàn)

| # | Quyết định | Lý do |
|---|---|---|
| P1 | Tham số worker: `batch-size` 10, `lease` 10 phút, `max-attempts` 8, backoff 30s × 2ⁿ⁻¹ tối đa 1 giờ, không jitter | Tổng thời gian thử ~2 giờ (< hạn token xác minh 24h); lease ≫ 10 email × timeout SMTP 3×10s; không jitter để test xác định (một instance ở Phase 1) |
| P2 | Lease quá hạn tính là **một lần thử** (`attempts+1`, `last_error = LEASE_EXPIRED`); đạt tối đa → `FAILED` + xóa payload | Chặn vòng lặp vô hạn nếu một sự kiện làm worker chết liên tục |
| P3 | Kết thúc (`SENT`/`PENDING`/`FAILED`) chỉ ghi khi `locked_until` còn khớp với lần nhận của worker | Worker chậm không ghi đè kết quả sau khi lease đã bị thu hồi |
| P4 | Lỗi vĩnh viễn → `FAILED` ngay: không có handler (`NO_HANDLER`), payload thiếu trường (`INVALID_PAYLOAD`), token trong liên kết đã hết hạn (`TOKEN_EXPIRED`, không gửi liên kết chết) | Không thử lại thứ chắc chắn thất bại |
| P5 | `last_error` và log chỉ chứa mã cố định hoặc **tên lớp** lỗi, không bao giờ thông điệp lỗi | Thông điệp SMTP có thể chứa địa chỉ người nhận (SECURITY_RULES §11) |
| P6 | Liên kết: `{APP_BASE_URL}/verify-email#token=…`, `{APP_BASE_URL}/reset-password#token=…` (token sau `#`) | Phần sau `#` không gửi tới máy chủ → không vào log proxy (SECURITY_RULES §3.4) |
| P7 | `APP_BASE_URL` bắt buộc https, trừ localhost/127.0.0.1/`*.localhost`/`*.test`; không có query/fragment; sai → app không khởi động | SECURITY_RULES §13.6 (token chỉ qua HTTPS); chống cấu hình sai |
| P8 | Email văn bản thuần (không HTML), tiêu đề cố định tiếng Việt; tên hiển thị bỏ ký tự điều khiển (CR/LF…), tối đa 100 ký tự | SECURITY_RULES §9.5–9.6: không cần escape HTML, không chèn header |
| P9 | Message-ID = `<eventId@outbox.lifeos>` | Idempotent "ở mức có thể" (§3.11): gửi lại cùng sự kiện mang cùng định danh |
| P10 | Job dọn chạy 03:00 UTC hằng ngày (`lifeos.cleanup.cron`); xóa bằng một câu `DELETE` mỗi bảng | Tài liệu chỉ nói "hằng ngày"; quy mô Phase 1 nhỏ |
| P11 | Metric: `lifeos.outbox.events{type,outcome}` và gauge `lifeos.outbox.pending`; không nhãn chứa dữ liệu người dùng | §3.13 "kích thước hàng đợi outbox, số email lỗi" |
| P12 | SMTP môi trường thật mặc định `587` + AUTH + STARTTLS bắt buộc (`SMTP_AUTH`, `SMTP_STARTTLS` tắt được); dev: mailpit không auth/TLS | An toàn mặc định |
| P13 | Compose chạy backend với profile `dev` (file ghi rõ "không dùng cho production"); healthcheck bằng `bash /dev/tcp` | Image JRE không có curl |
| P14 | Tắt health indicator mail (`management.health.mail.enabled=false`) | Email đi bất đồng bộ qua outbox: SMTP hỏng không được làm `/actuator/health` DOWN và rút app khỏi vòng phục vụ; lỗi gửi theo dõi qua metric P11 |

---

## 4. Kiểm thử

| Lớp test | Nội dung |
|---|---|
| `OutboxWorkerIT` (8) | Email xác minh: liên kết từ `APP_BASE_URL` dù request mang `Host`/`X-Forwarded-Host` độc hại; token trong email dùng được; `SENT`, payload `{}` (LOG-02), không gửi lại. Đặt lại mật khẩu + "mật khẩu đã thay đổi". Lỗi tạm thời: backoff 30s rồi 60s, payload giữ đến khi gửi xong. Lỗi kéo dài: `FAILED` sau 8 lần, payload `{}`, `last_error` là tên lớp. Lỗi vĩnh viễn (`TOKEN_EXPIRED`, `NO_HANDLER`, `INVALID_PAYLOAD`). Lease quá hạn được thu hồi, worker cũ không ghi đè. CR/LF trong tên hiển thị. Metric chỉ có nhãn `type`/`outcome` |
| `OutboxConcurrencyIT` (20 + 1) | Hai worker thật chạy song song (`CountDownLatch`, timeout cứng) × **20 lần**: mỗi sự kiện gửi đúng một lần. Dòng bị transaction khác khóa → `SKIP LOCKED` bỏ qua, không chờ |
| `CleanupJobsIT` (2) | Token hết hạn > 7 ngày bị xóa, ≤ 7 ngày và còn hạn giữ lại; outbox `SENT`/`FAILED` > 7 ngày bị xóa, `PENDING` và gần đây giữ lại; chạy lặp an toàn |
| `MailpitDeliveryIT` (1) | Đầu-cuối với scheduler bật, SMTP thật tới container mailpit: đăng ký → email xác minh → xác minh → quên mật khẩu → email đặt lại → đặt lại → email "đã thay đổi"; 3 sự kiện `SENT`, payload `{}` |
| `SensitiveLogIT` (mở rộng) | Chạy worker trong lúc thu log: không mật khẩu, token, email trong log chữ/JSON/stdout |

Không dùng `Thread.sleep`; thời gian điều khiển bằng `MutableClock`, chờ bất đồng bộ bằng Awaitility. Guard thứ tự khóa
D86 (`LockOrderGuard`) vẫn bọc mọi IT: worker không chạm `user_sync_state`/`change_logs` nên không ảnh hưởng.

**Lệnh và kết quả:** `mvn -f backend/pom.xml clean verify` (từ `D:\LifeOS`, Docker chạy): BUILD SUCCESS, 83 unit +
442 IT (trước Giai đoạn 4: 83 + 410; +32 IT: OutboxWorkerIT 8, OutboxConcurrencyIT 21, CleanupJobsIT 2, MailpitDeliveryIT 1).
Lần chạy toàn bộ đầu tiên đỏ 2 test (`ApplicationStartupIT`, `ActuatorExposureIT`: `/actuator/health` = 503) — health
indicator mail của `spring-boot-starter-mail` báo DOWN khi không nối được SMTP; sửa ở cấu hình (P14), không sửa test.

**Thời gian `verify`:** 3:24 (Giai đoạn 3: 3:17). IT chậm nhất: `ApplicationStartupIT` 28,9 s (khởi động context đầu),
`MailpitDeliveryIT` 12,4 s (context riêng + chờ worker 2 giây ×3 email), `LockTimeoutIT` 8,9 s, `LoginRateLimitIT`
8,2 s, `SyncConcurrencyIT` 7,0 s. Test outbox khác đều < 2 s. Chưa cần tách test chậm.

**Docker:** `docker build -t lifeos-backend .` 4 phút (lần đầu, tải dependency), image 397 MB, chạy bằng user `lifeos`
(uid 999). `JWT_SECRET=… docker compose up -d --build`: postgres, mailpit, backend đều healthy sau ~40 giây; đăng ký qua
`http://localhost:8080` → email "Xác minh địa chỉ email LifeOS" có trong mailpit, liên kết `http://localhost:8080/verify-email#token=…`;
đã `docker compose down -v` sau khi thử. Lưu ý: image thử được build **trước** khi thêm P14 (tắt health mail); trong compose SMTP tới mailpit thông nên health vẫn UP — chưa build lại sau P14. (Lần build đầu lỗi DNS tạm thời tới Docker Hub, chạy lại thì được.)

---

## 5. Điểm cần người xem kỹ

1. **`IntegrationTestBase` thêm `@TestPropertySource`** (tắt scheduler, cấu hình mail test) áp cho mọi context test.
   Nếu không, các context profile `dev`/`prod` sẽ chạy worker nền trên DB dùng chung và ghi đè payload mà test khác đang
   đọc token. `MailpitDeliveryIT` là context duy nhất bật scheduler và tự đóng sau lớp (`@DirtiesContext`).
2. **Thứ tự trong một lượt worker:** nhận cả lô (≤ 10) với cùng `locked_until`, rồi gửi lần lượt. Lô gửi chậm hơn lease
   (10 phút) thì sự kiện cuối có thể bị worker khác thu hồi và gửi lại (at-least-once; chấp nhận theo §3.11).
3. **Email gửi lại khi DB lỗi sau khi SMTP đã nhận** (ghi `SENT` thất bại): sự kiện kẹt `PROCESSING` tới hết lease rồi gửi
   lại — trùng email có thể xảy ra, đúng đánh đổi at-least-once đã chấp nhận.
4. **Mẫu email** (tiếng Việt, văn bản thuần) và đường dẫn liên kết (P6) cần khớp với cách app Flutter mở deep link — phía
   client chưa có.
5. **`axllent/mailpit:latest`** trong compose và test: không ghim phiên bản (giữ như compose sẵn có).

---

## 6. Việc còn lại / rủi ro

- `SYSTEM_ARCHITECTURE.md` §12: "ArchUnit chạy trong CI" vẫn chờ người dùng chép `ci/backend-ci.yml` vào `.github/` và chạy trên GitHub.
- Email môi trường thật: SPF/DKIM/DMARC (SECURITY_RULES §14.7) — việc hạ tầng.
- Chuyển handler email sang module `notification`, `notification_deliveries`, quiet hours: Phase 4 sản phẩm (đang `[PROPOSED]`).
- Kịch bản tải "outbox 1.000 sự kiện" (TESTING_RULES §11) chạy nightly/theo yêu cầu — chưa làm.
