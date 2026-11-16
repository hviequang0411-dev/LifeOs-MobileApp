# LifeOS — Kế hoạch khởi tạo Backend Skeleton

> **Trạng thái:** v1 — đã duyệt 2026-11-05; S1–S8 đã triển khai và build/test xanh (Giai đoạn 1–4). Backend đã được mở rộng tới Phase 7 theo `docs/engineering/BACKEND_MASTER_PLAN.md` (WP1–WP8, 2026-11-07): xem `docs/review/BACKEND_FINAL_REPORT.md`. Tài liệu này giữ lại như lịch sử khởi tạo; cấu trúc module hiện hành ở `SYSTEM_ARCHITECTURE.md` §3.1.
> **Mục tiêu:** có một backend chạy được, build/test xanh, đã gài sẵn các hàng rào (ArchUnit, Flyway, Testcontainers, xử lý lỗi) để các module nghiệp vụ viết lên trên mà không phá kiến trúc đã duyệt.
> **Nguồn:** `SYSTEM_ARCHITECTURE.md` (§2 container, §3.1–3.3 package layout, §3.9 ArchUnit), `DATABASE_DESIGN.md` (V1–V4), `JAVA_SPRING_RULES.md`, `TESTING_RULES.md`, `SECURITY_RULES.md`.
> **Ngoài phạm vi:** logic nghiệp vụ của Identity/Task/Sync (làm theo từng bước sau), Flutter skeleton (kế hoạch riêng), AI, deploy production.

---

## 1. Quyết định công cụ (cần duyệt)

| # | Mục | Đề xuất | Lý do |
|---|---|---|---|
| P1 | Cấu trúc Maven | **Một module Maven**, chia module nghiệp vụ bằng package (`com.lifeos.<module>`), ranh giới bảo vệ bằng ArchUnit | Khớp D1 (ArchUnit) và Modular Monolith; một developer nên tránh chi phí multi-module Maven |
| P2 | Java | **Java 21 (LTS)** (đổi từ 25, ngày 2026-11-06) | LTS đã dùng rộng rãi, ít rủi ro công cụ; Spring Boot 4.x hỗ trợ. Có thể build bằng JDK mới hơn. Nâng lên 25 sau khi mọi thứ ổn định |
| P3 | Spring Boot | **4.1.x, bản vá mới nhất tại thời điểm khởi tạo** (xác nhận trên start.spring.io trước khi chốt) | Dòng ổn định hiện tại; Spring Boot 3.5 đã hết hỗ trợ OSS (30/06/2026) |
| P4 | Hệ quả của Boot 4 | Jackson 3, Hibernate 7, Jakarta EE 11, auto-configuration chia nhỏ (starter mới) | Phải dùng đúng tên starter/namespace của Boot 4; không dùng ví dụ cũ của Boot 3 |
| P5 | `groupId` / `artifactId` | `com.lifeos` / `lifeos-backend` | Khớp package gốc đã chốt |
| P6 | PostgreSQL | Phiên bản ổn định mới nhất được hỗ trợ (xác nhận khi khởi tạo); cùng phiên bản cho dev, Testcontainers và production | `TESTING_RULES.md` §1.6 |
| P7 | Chạy local | Docker Compose: PostgreSQL + Mailpit (nhận email dev) | Không cần SMTP thật |
| P8 | CI | GitHub Actions (nếu repo dùng GitHub) hoặc tương đương | Cổng CI ở `TESTING_RULES.md` §12 |
| P9 | Build wrapper | Maven Wrapper (`mvnw`) được commit | Mọi người/CI dùng cùng phiên bản Maven |
| P10 | Phân tích tĩnh | Chốt công cụ ở bước S2 (đề xuất: Checkstyle + SpotBugs hoặc Error Prone, chọn một bộ gọn) | `[OPEN]` nhỏ, chốt khi cài |

Mọi phụ thuộc ngoài danh sách ở §3 cần ADR hoặc duyệt (`AGENTS.md` §1.6).

---

## 2. Các bước (mỗi bước có tiêu chí hoàn thành)

| Bước | Nội dung | Tiêu chí hoàn thành |
|---|---|---|
| **S1** | Tạo project Maven + Boot 4, `LifeOsApplication`, `application.yml` (profile `dev`, `test`), Maven Wrapper, `.gitignore`, `.editorconfig`, `docker-compose.yml` (Postgres, Mailpit) | `./mvnw verify` chạy; app khởi động với Postgres từ Compose; `GET /actuator/health` trả `UP` |
| **S2** | Cấu hình chất lượng: formatter/linter đã chọn, `spring.jpa.hibernate.ddl-auto=validate`, `open-in-view=false`, bật `-Werror` cho cảnh báo biên dịch quan trọng, MapStruct (`unmappedTargetPolicy=ERROR`) | Build đỏ khi vi phạm; ghi lệnh vào bảng build/test của `AGENTS.md` |
| **S3** | Khung package theo `SYSTEM_ARCHITECTURE.md`: `common` (+ `common.sync`), và module rỗng `identity, task, project?, sync, notification` kèm `api/`, `application/`, `domain/`, `infrastructure/`, `web/`. **Lưu ý:** Category nằm trong `task` (D25); `project` theo đúng module graph đã duyệt | Cấu trúc đúng tài liệu; không có logic |
| **S4** | **ArchUnit A1–A12** (rule tồn tại trước khi có code nghiệp vụ) + test tương ứng | Test kiến trúc xanh trên skeleton; thêm class vi phạm thử → đỏ |
| **S5** | Hạ tầng chung: `Clock` bean, `CurrentUser` abstraction (chưa cài đặt xác thực đầy đủ), `CorrelationIdFilter`, xử lý lỗi toàn cục RFC 9457 với `errorCode` (`GlobalExceptionHandler`), logging JSON/pattern có `correlationId`, che dữ liệu nhạy cảm | Test web: lỗi đúng định dạng `API_CONVENTIONS.md` §5; header `X-Correlation-Id` được trả lại |
| **S6** | **Flyway V1–V4** đúng `DATABASE_DESIGN.md` (identity, sync infra, task domain, outbox) + test migration (`TESTING_RULES.md` §8) | Flyway chạy trên Postgres sạch; test ràng buộc và checksum-bất-biến xanh; `ddl-auto=validate` khớp |
| **S7** | Nền tảng test: base class Testcontainers (PostgreSQL), helper dựng user/dữ liệu, `ArchitectureTest`, test khởi động context | `./mvnw verify` chạy cả test tích hợp; chạy song song được |
| **S8** | CI: build + test + ArchUnit + quét phụ thuộc + quét bí mật; cập nhật bảng build/test trong `AGENTS.md` (thay `TBD` bằng lệnh thật) | Pipeline xanh trên nhánh chính |

Thứ tự S4 trước S5–S6 là có chủ ý: hàng rào kiến trúc phải có trước khi code nghiệp vụ.

---

## 3. Phụ thuộc dự kiến (Phase 1 skeleton)

Spring Boot starters: `web` (webmvc), `validation`, `data-jpa`, `security`, `actuator`; `flyway` + driver PostgreSQL; `mapstruct` (+ processor); `springdoc-openapi` (D63, chỉ dev/test); `caffeine` (rate limit, D24); thư viện JWT (qua Spring Security resource-server/`nimbus-jose-jwt`); Argon2 (cần `bcprov` theo `SECURITY_RULES.md` §3.2). Test: `spring-boot-starter-test`, `testcontainers` (postgresql, junit-jupiter), `archunit-junit5`, `awaitility`, GreenMail hoặc stub email. Tên starter chính xác của Boot 4 xác nhận ở S1.

Không thêm: RabbitMQ, Redis, Spring AI, Kafka, bất kỳ thư viện không có trong danh sách (xem `AGENTS.md` §1.6).

---

## 4. Rủi ro và cách giảm

| Rủi ro | Giảm thiểu |
|---|---|
| Boot 4 đổi starter/namespace, nhiều tài liệu mạng viết cho Boot 3 | Xác nhận theo tài liệu chính thức của Boot 4 ở S1; không sao chép cấu hình cũ |
| Jackson 3 đổi package/API | Dùng Jackson mà Boot 4 quản lý; test serialize bằng fixture JSON (D68) |
| Argon2 cần thư viện mật mã bổ sung | Xác nhận ở S1; nếu không phù hợp phải xin duyệt trước khi chuyển bcrypt |
| ArchUnit quá chặt gây cản trở lúc đầu | Không thêm ngoại lệ; sửa cấu trúc (`CODE_GUIDELINES.md` §16) |
| Testcontainers cần Docker trên máy dev/CI | Ghi rõ yêu cầu trong README backend; CI dùng runner có Docker |

---

## 5. Quy tắc làm việc cho các bước này

1. Mỗi bước là một thay đổi nhỏ, build/test xanh trước khi sang bước sau.
2. Cuối mỗi bước: báo cáo đã làm gì, giả định, điểm cần xem kỹ (`AGENTS.md` §1.7).
3. Không sửa `docs/` trừ việc điền lệnh build/test và phiên bản đã chốt, và ghi rõ ở báo cáo.
4. Gặp mâu thuẫn giữa tài liệu và thực tế công cụ (ví dụ starter không tồn tại): dừng và hỏi, không tự đổi thiết kế.

---

## 6. Sau skeleton

Thứ tự đề xuất: (1) Identity (đăng ký → xác minh → đăng nhập → refresh → thu hồi thiết bị, kèm bộ SEC/AUTHZ), (2) Task/Project/Category CRUD qua Application Service + `ChangeRecorder`, (3) Sync push/pull với bộ SYN-01..21, (4) Outbox + email, (5) Flutter skeleton song song từ bước (2).

---

## 7. Cần bạn duyệt

P1–P10 ở §1 (đặc biệt P1 một module Maven, P2/P3 phiên bản) và thứ tự S1–S8. Nếu duyệt, tôi bắt đầu từ S1 và báo cáo sau mỗi bước.

---

## 8. Kết quả triển khai và các điểm lệch so với kế hoạch

**Cập nhật 2026-11-07:** Giai đoạn 0–3 (skeleton, Identity, Project/Category/Task, Sync push/pull) đã xong và được duyệt; `mvn clean verify` xanh 493/493 (83 unit + 410 IT). Bảng dưới là trạng thái lúc skeleton và được giữ làm lịch sử; các mục đã đổi từ đó: ArchUnit A1–A13 với `failOnEmptyShould=true` và self-test luật đỏ khi có vi phạm; đã thêm nimbus-jose-jwt, bcprov, Caffeine, springdoc; `Mutation`/`MutationOutcome`/`EntitySnapshot` đã chốt ở Giai đoạn 3 (`MutationOutcome` sealed). Chi tiết Giai đoạn 3: `docs/review/BACKEND_PHASE3_SYNC_REPORT.md`.

**Trạng thái xác minh (cập nhật 2026-11-06, sau `mvn verify` đầu tiên):** build bằng IDE với `java.version=21`; thay đổi duy nhất so với bản gốc là `java.version` 25 → 21. Kết quả `mvn verify`: **21 unit test (surefire) đạt; 12 integration test (failsafe) chạy, 11 đạt, 1 lỗi** (tổng 33) — IT gồm ArchitectureTest (A1–A12), ErrorHandlingTest, ApplicationStartupIT và 8/9 test của FlywayMigrationIT. Test lỗi: `FlywayMigrationIT.allFourMigrationsAreApplied` (`expected 4 but was 0`). **Nguyên nhân là lỗi thiết kế của test, không phải của migration:** `ApplicationStartupIT` và `FlywayMigrationIT` dùng chung một container PostgreSQL; Spring đã chạy Flyway trước nên `migrate()` trong `FlywayMigrationIT` không còn gì để chạy và trả `migrationsExecuted = 0`. Việc 8 test ràng buộc DB còn lại đều đạt chứng tỏ V1–V4 đã áp dụng đúng. Cách sửa: kiểm tra trạng thái (`info().applied()` có 4 bản, phiên bản hiện tại là 4) thay vì đếm số bản vừa chạy. **Nguyên tắc rút ra:** test dùng container dùng chung không được giả định DB còn trống.

| Mục kế hoạch | Thực tế | Ghi chú |
|---|---|---|
| P6 PostgreSQL | **17** (`postgres:17-alpine`) cho compose và Testcontainers | Hỗ trợ Flyway chắc chắn; nâng lên 18 sau khi kiểm chứng |
| P9 Maven Wrapper | **Chưa tạo** (cần mạng). Chạy `mvn -N wrapper:wrapper` rồi commit | CI dùng `mvn` của runner |
| P10 phân tích tĩnh | **Chưa chốt/chưa cài** | Cài formatter sau khi build xanh để không đoán định dạng |
| S2 `-Werror` | **Chưa bật**; chỉ `-Xlint:all,-processing,-serial` | Bật sau lần build sạch đầu tiên |
| S3 module | Có `identity, project, task, sync` + `common`; chưa có thư mục `controller/service/...` (rỗng) | Tạo khi có code; ArchUnit đã sẵn luật |
| S4 ArchUnit | A1–A12 đã viết; `failOnEmptyShould=false` tạm thời | Đổi `true` khi Identity + Task có code. **Chưa thử** việc thêm class vi phạm để thấy luật đỏ |
| S5 | `Clock`, `CorrelationIdFilter`, `GlobalExceptionHandler`, `CurrentUser`, `SecurityConfig` (stateless, deny-by-default), entry point 401 RFC 9457 | Chưa có `JwtAuthFilter`, `PasswordHasher` (bước Identity) |
| S5 mã lỗi | Thêm `NOT_FOUND`, `METHOD_NOT_ALLOWED`, `REQUEST_REJECTED` cho lỗi giao thức của Spring | Ghi vào `API_CONVENTIONS.md` §5.1 (D73) |
| S5 log JSON | Cấu hình `logging.structured.format.console=ecs` ở profile `prod` | Chưa kiểm chứng đầu ra có `correlationId` |
| S6 | V1–V4 trích **nguyên văn** từ `DATABASE_DESIGN.md`; `FlywayMigrationIT` kiểm ràng buộc quan trọng | Chưa chạy |
| S7 | Singleton container `PostgresTestContainer`; chưa có helper dựng dữ liệu dùng chung | Thêm khi có test nghiệp vụ |
| S8 | `ci/backend-ci.yml` và `ci/dependabot.yml` (công cụ không được ghi vào `.github/` vì là thư mục được bảo vệ): **bạn cần tự chép** `ci/backend-ci.yml` → `.github/workflows/backend-ci.yml` và `ci/dependabot.yml` → `.github/dependabot.yml`. Nội dung: (build+test, migration immutability, gitleaks, dependency review), `dependabot.yml` | Chưa chạy trên GitHub; cần repo đặt ở gốc `LifeOS` |
| Phụ thuộc | Chưa thêm: JWT/Nimbus, bcprov (Argon2), Caffeine, springdoc, mailpit client | Thêm ở bước Identity/OpenAPI để không có thư viện thừa chưa dùng |
| Mutation/MutationOutcome/EntitySnapshot | Là record khởi đầu (payload `Map<String,Object>`) | `SYSTEM_ARCHITECTURE.md` §3.3 chỉ định interface; kiểu dữ liệu chi tiết chốt khi viết module sync |

**Mâu thuẫn tài liệu phát hiện khi triển khai (chưa sửa, cần bạn quyết):** `JAVA_SPRING_RULES.md` §6.2 ví dụ `TASK_VERSION_CONFLICT`, trong khi `API_CONVENTIONS.md` dùng `ENTITY_VERSION_CONFLICT`. Skeleton dùng `ENTITY_VERSION_CONFLICT`.
