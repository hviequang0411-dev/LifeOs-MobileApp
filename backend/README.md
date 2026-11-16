# LifeOS — Backend

Spring Boot 4.1 · Java 21 (LTS) · PostgreSQL · Flyway · Modular Monolith (một module Maven, ranh giới bằng ArchUnit).

Tài liệu thiết kế: `../docs/` (bắt đầu từ `../AGENTS.md`). Kế hoạch skeleton: `../docs/architecture/BACKEND_SKELETON_PLAN.md`.

## Yêu cầu

- JDK 21 trở lên (bytecode build ra là Java 21; JDK mới hơn như 23/25 vẫn biên dịch được)
- Maven 3.9+ (Maven Wrapper chưa được tạo: chạy một lần `mvn -N wrapper:wrapper` rồi commit `mvnw`, `.mvn/`)
- Docker (chạy PostgreSQL local và test tích hợp bằng Testcontainers)

## Chạy local

Cách 1 — cả stack bằng Docker Compose (backend + PostgreSQL + Mailpit):

```bash
export JWT_SECRET="$(openssl rand -base64 32)"   # bắt buộc (base64, >= 32 byte); không commit
docker compose up -d --build
curl http://localhost:8080/actuator/health        # {"status":"UP"}; email xem tại http://localhost:8025
```

Cách 2 — chỉ hạ tầng bằng Compose, backend chạy bằng Maven:

```bash
docker compose up -d postgres mailpit             # PostgreSQL (5432) + Mailpit (SMTP 1025, UI http://localhost:8025)
export JWT_SECRET="$(openssl rand -base64 32)"
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

Biến môi trường bắt buộc ở môi trường thật (profile dev có mặc định trỏ vào Mailpit): `DB_URL`, `DB_USER`,
`DB_PASSWORD`, `JWT_SECRET`, `SMTP_HOST`, `MAIL_FROM`, `APP_BASE_URL` (gốc liên kết trong email; phải là https, trừ
localhost/`*.test`). Tùy chọn: `SMTP_PORT` (587), `SMTP_USER`, `SMTP_PASSWORD`, `SMTP_AUTH` (true), `SMTP_STARTTLS`
(true), `JWT_KID`, `JWT_PREVIOUS_SECRET`/`JWT_PREVIOUS_KID` (xoay khóa), `JWT_ISSUER`, `JWT_AUDIENCE`,
`TRUSTED_PROXIES` (regex IP của reverse proxy tin cậy; rỗng = không tin `X-Forwarded-For`).
Cũng bắt buộc ở môi trường thật (dev/test có giá trị riêng, không dùng lại): `NOTIFICATION_UNSUBSCRIBE_SECRET` (≥ 32 ký
tự, khác khóa JWT) và `SNAPSHOT_HMAC_SECRET` (≥ 32 ký tự, khác hai khóa kia; ký `snapshotToken` của full resync — mọi
instance phải dùng CHUNG một giá trị, thiếu thì ứng dụng không khởi động; đổi giá trị chỉ làm các phiên snapshot đang
dở ≤ 30 phút phải tải lại). Múi giờ tiến trình: chạy với `TZ=UTC` (image production đặt `TZ=UTC` và
`-Duser.timezone=UTC`, compose deploy đặt `TZ: UTC`; chạy tay ngoài image thì tự đặt).
OpenAPI (`/v3/api-docs`) chỉ bật ở profile `dev`/`test` và vẫn cần access token.

Email đi qua outbox (`outbox_events`): `OutboxWorker` quét mỗi 2 giây, gửi ngoài transaction, thử lại với backoff mũ
(30 giây × 2ⁿ, tối đa 1 giờ), `FAILED` sau 8 lần; payload (chứa token thô) bị ghi đè `{}` khi xong. Job dọn chạy
03:00 UTC hằng ngày (token hết hạn và outbox `SENT`/`FAILED` quá 7 ngày). Tham số: `lifeos.outbox.*`,
`lifeos.cleanup.cron`; tắt mọi job bằng `lifeos.scheduling.enabled=false` (profile test).

## Build & test

```bash
mvn test            # unit test (không cần Docker), gồm ArchitectureTest (A1–A13)
mvn verify          # thêm test tích hợp *IT (cần Docker): PostgreSQL và Mailpit qua Testcontainers
```

## Quy ước

- Test đơn vị đặt tên `*Test` (surefire), test tích hợp `*IT` (failsafe).
- Migration Flyway trong `src/main/resources/db/migration`. **Không sửa migration đã áp dụng** (CI kiểm tra).
- `archunit.properties` đặt `archRule.failOnEmptyShould=true` (từ cuối Giai đoạn 2): luật không khớp class nào là lỗi.
  `ArchitectureRulesSelfTest` kiểm các luật thật sự đỏ khi gặp class vi phạm (class mẫu trong `architecture.fixture`).
- Thứ tự khóa D86: test tích hợp chạy qua `LockOrderGuard` (chỉ trong cấu hình test) — sau lần gọi
  `ChangeRecorder.record` đầu tiên, giao dịch chỉ còn được ghi `change_logs`/`sync_mutations`; vi phạm làm test đỏ.
- `lifeos.db.lock-timeout` (mặc định `5s`, D87) đặt `lock_timeout` cho mọi kết nối; hết hạn → 503 + `Retry-After: 2`.
- Hợp đồng API: `../docs/api/openapi.yaml` và `../docs/api/fixtures/` (D68). `OpenApiExportIT` đỏ khi API đổi mà file
  chưa xuất lại; xuất lại bằng `mvn verify -Dit.test=OpenApiExportIT -Dopenapi.export=true` rồi commit cùng thay đổi.
- Không thêm dependency ngoài danh sách trong kế hoạch khi chưa được duyệt (`../AGENTS.md` §1.6).
