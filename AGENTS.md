# AGENTS.md — LifeOS

> **Trạng thái:** v1.2 — đã duyệt 2026-11-13 (chốt ngoại lệ không dùng Spring AI cho adapter đầu tiên tại AIP3).
> Tài liệu điều phối cho AI agent và developer. File này **ngắn có chủ đích**: chỉ chứa cấu trúc, quy tắc bắt buộc, cách chạy test và đường dẫn tới tài liệu chi tiết. Không nhồi toàn bộ coding rules vào đây.

---

## 1. Quy tắc vận hành dành cho Agent (đọc trước tiên)

1. **Rules do con người quyết định.** Agent không được tự đặt, tự thay đổi hoặc tự "cải tiến" coding rules, kiến trúc, module boundary hay quy ước đặt tên. Agent chỉ *áp dụng* những gì đã được duyệt trong `docs/`.
2. **Các file sau chỉ được sửa khi có yêu cầu rõ ràng của người dùng:** `AGENTS.md`, mọi file trong `docs/engineering/`, `docs/architecture/` (kể cả `ADR/`), `docs/product/`.
3. **Gặp điểm chưa được chốt → dừng và hỏi**, hoặc ghi vào mục *Open Questions* của tài liệu liên quan. Không tự chọn rồi implement. Decision log nằm ở `docs/engineering/CODE_GUIDELINES.md` §15; các mục baseline §16 còn mở được chốt theo lịch ở §15.2.
4. **Chỉ mục gắn nhãn `[CONFIRMED]` là bắt buộc.** `[PROPOSED]` (nếu có) là chưa được duyệt; `[OPEN]` là chưa quyết định. Cần dùng mục `[PROPOSED]`/`[OPEN]` thì hỏi người dùng trước.
5. **Plan trước, code sau** với mọi thay đổi chạm nhiều hơn một module, đổi schema DB, đổi API contract hoặc đổi logic sync. Trình bày plan ngắn và chờ duyệt.
6. **Không thêm công nghệ/dependency mới** ngoài stack đã chốt khi chưa có ADR được duyệt.
7. **Không tự review code do chính mình viết rồi coi là đã review.** Kết thúc task phải liệt kê rõ: đã làm gì, giả định nào đã đưa ra, điểm nào cần người xem kỹ.

---

## 2. Tổng quan dự án

LifeOS là nền tảng quản lý cuộc sống cá nhân, **offline-first**, có trợ lý AI.
Chi tiết sản phẩm: `docs/product/` (hiện baseline ở file product context).

| Layer | Công nghệ |
|---|---|
| Mobile | Flutter + SQLite (local) |
| Backend | Java + Spring Boot, **Modular Monolith** |
| Database | PostgreSQL (nguồn dữ liệu trung tâm), pgvector |
| Async | Transactional Outbox (RabbitMQ & Redis: phase sau) |
| Push / Email | Firebase Cloud Messaging / Email provider |
| AI | Adapter đầu tiên dùng SDK OpenAI chính thức sau `LLMProvider`, không dùng Spring AI do lệch baseline/Responses API; CI dùng fake provider |
| Deploy | Docker Compose |

---

## 3. Cấu trúc repo

```text
LifeOS/
├── AGENTS.md
├── docs/
│   ├── product/            # PRD, use cases
│   ├── architecture/       # system, module boundaries, DB design, ADR/
│   └── engineering/        # coding rules, conventions
├── backend/                # Spring Boot (com.lifeos.*) — skeleton S1–S8, xem backend/README.md
└── mobile/                 # Flutter
```

Backend package gốc: `com.lifeos` với các module `identity, task, project, habit, routine, calendar, finance, note, goal, analytics, notification, sync, ai` và `common`.

---

## 4. Quy tắc bắt buộc (tóm tắt)

Chi tiết và lý do ở các file trong `docs/engineering/` (bắt đầu từ `CODE_GUIDELINES.md`).

**Backend**
- Luồng: `Controller → Application Service → Domain/Repository`. Controller không chứa business logic, không gọi Repository.
- `@Transactional` đặt ở **Application Service**, không ở Controller/Repository. Không gọi hệ thống ngoài (FCM, email, LLM) bên trong transaction.
- Module khác **không** truy cập Repository/Entity nội bộ của module này. Chỉ gọi qua public API của module.
- Không trả Entity ra ngoài API. Dùng DTO riêng cho request/response.
- `user_id` luôn lấy từ security context, **không** nhận từ request body. Mọi truy vấn dữ liệu cá nhân phải scope theo `user_id`.
- Tiền: `NUMERIC`/`BigDecimal` + currency code. Không dùng `float/double`.
- Thời điểm: `TIMESTAMPTZ`. Ngày nghiệp vụ: `DATE`.
- Schema thay đổi qua migration, không dùng `ddl-auto` tự sinh.

**Sync / dữ liệu**
- ID là UUID (client có thể tạo offline). Mỗi entity đồng bộ có `version` và soft delete (tombstone).
- Mutation có `mutationId` để idempotent. Server quản lý `version` và `cursor`; client **không** dùng timestamp tự khai báo để quyết định bản nào mới hơn.
- Không áp dụng Last Write Wins cho mọi entity; xem bảng chiến lược trong `SYNC_RULES.md` §9.5.

**AI**
- AI chỉ đề xuất. Backend validate, người dùng duyệt, rồi Business Service mới thực thi. Không có giao dịch tài chính tự động.
- Không để API key của AI Provider trong Flutter.
- `[CONFIRMED]` AIP1: provider adapter là chi tiết cài đặt sau port LifeOS-owned `LLMProvider`; controller/service/tool và module khác chỉ phụ thuộc port cùng DTO/schema do LifeOS sở hữu. Type của provider SDK không được đi qua boundary này. Test tự động dùng fake `LLMProvider`, không gọi provider thật.
- `[CONFIRMED]` (2026-11-13) AIP3: giữ Spring Boot 4.1.0 và không dùng Spring AI cho adapter đầu tiên vì dòng Spring AI tương thích Boot 4.1 chưa có OpenAI Responses API, còn dòng có Responses API dùng baseline Boot 4.2. `OpenAIProvider` được phép dùng trực tiếp SDK chính thức `com.openai:openai-java` sau `LLMProvider`, bắt buộc `store=false`; không SDK/type OpenAI nào được đi qua boundary. Bằng chứng và quyết định thay thế nằm trong `docs/review/DEPLOY_AI_PROGRESS.md` §13.

**Flutter**
- UI đọc từ SQLite; ghi vào SQLite + Local Outbox trong cùng một local transaction. UI không gọi thẳng API cho dữ liệu cần đồng bộ.

---

## 5. Build & test

> Backend: `mvn clean verify` xanh (2026-11-15, `java.version=21`): **936/936 test đạt** (169 unit + 767 IT), xanh 3 lượt liên tiếp sau đợt sửa backend 1 (F1–F9, migration `V13__backend_fixes.sql` — xem `docs/review/BACKEND_FIX_REPORT.md`). Mốc trước đó: 879/879 ngày 2026-11-14 (sau AIP2–AIP4/AIP7 local), 847/847 ngày 2026-11-07 (trước AI). Giai đoạn 0–4 đã duyệt; đã thực hiện xong `docs/engineering/BACKEND_MASTER_PLAN.md` — tiến độ từng WP ở `docs/review/BACKEND_PROGRESS.md` (WP1–WP8 xong — báo cáo cuối `docs/review/BACKEND_FINAL_REPORT.md`). Chạy khoảng 7–9 phút, cần Docker cho `*IT` (PostgreSQL và Mailpit qua Testcontainers).

| Việc | Lệnh | Trạng thái |
|---|---|---|
| Backend build | `mvn -B -ntp -f backend/pom.xml package -DskipTests` | đã chạy, xanh |
| Backend test | `mvn -f backend/pom.xml test` (unit, không cần Docker) · `mvn -f backend/pom.xml verify` (thêm `*IT`, cần Docker) | đã chạy 2026-11-15, 936/936 đạt (169 unit + 767 IT), xanh 3 lượt liên tiếp |
| Kiểm tra module boundary | `mvn -f backend/pom.xml test -Dtest=ArchitectureTest` (ArchUnit A1–A15) | đã chạy, đạt (nằm trong 169 unit) |
| Benchmark (không chặn CI) | `mvn -f backend/pom.xml verify -Dtest=NoSuchTest -Dsurefire.failIfNoSpecifiedTests=false -Dit.test=BackendBenchmark` (ghi `backend/target/benchmark.md`) | đã chạy 2026-11-07, trong ngưỡng D173/D207 |
| Xuất lại OpenAPI | `mvn -f backend/pom.xml verify -Dit.test=OpenApiExportIT -Dopenapi.export=true` (ghi `docs/api/openapi.yaml`) | đã chạy; `OpenApiExportIT` đỏ khi API đổi mà chưa xuất lại |
| Mobile analyze | `flutter analyze` / `dart analyze` (trong `mobile/`) | đã chạy 2026-11-15, xanh (`No issues found!`) |
| Mobile test | `flutter test --no-pub -j 1` (trong `mobile/`) · benchmark 50k chạy riêng: `flutter test test/domain_modules_and_perf_test.dart` | đã chạy 2026-11-15 (sau AWP-1..6): 104 đạt, 1 bỏ qua (benchmark 50k chạy riêng, 11,0 s), 0 lỗi — xem `docs/review/MOBILE_AI_ASSISTANT_REPORT.md`. Mốc trước: 25/25 ngày 2026-11-08 |
| Chạy local stack | Cả stack: `JWT_SECRET=… docker compose -f backend/docker-compose.yml up -d --build` (backend + PostgreSQL + Mailpit, email xem tại http://localhost:8025) · hoặc `docker compose -f backend/docker-compose.yml up -d postgres mailpit` rồi `mvn -f backend/pom.xml spring-boot:run -Dspring-boot.run.profiles=dev` | đã chạy thử cả stack (2026-11-07): ba dịch vụ healthy, email xác minh tới Mailpit |

---

## 6. Bản đồ tài liệu

| Tài liệu | Nội dung | Trạng thái |
|---|---|---|
| `docs/engineering/CODE_GUIDELINES.md` | Nguyên tắc nền tảng, nhãn rule, decision log D1–D13, quy trình review, bản đồ tài liệu | v1 — đã duyệt |
| `docs/engineering/JAVA_SPRING_RULES.md` | Phân tầng, transaction, module boundary, DTO/mapper, exception, bảo mật, outbox, AI | v1 |
| `docs/engineering/DATABASE_RULES.md` | Persistence, timezone, cột chuẩn đồng bộ | v1 |
| `docs/engineering/SYNC_RULES.md` | Offline sync, idempotency, locking, conflict | v1 |
| `docs/engineering/SYNC_PROTOCOL.md` | Đặc tả cài đặt sync (Giai đoạn 3): ma trận kết quả, payload, lỗi, thứ tự khóa, SYN-22..48 | v1 — D81–D96 đã duyệt, **đã triển khai** (Giai đoạn 3, 2026-11-07) |
| `docs/engineering/FLUTTER_RULES.md` | Flutter + SQLite, outbox phía client | v1 |
| `docs/engineering/API_CONVENTIONS.md` | URL, error format, errorCode, pagination, versioning, danh mục endpoint Phase 1 | v1 — đã duyệt |
| `docs/engineering/SECURITY_RULES.md` | Threat model, JWT/refresh, ownership (IDOR), rate limit, secrets, log | v1 — đã duyệt |
| `docs/engineering/TESTING_RULES.md` | Chiến lược test, bộ test sync/bảo mật/migration bắt buộc, cổng CI | v1 — đã duyệt |
| `docs/engineering/GIT_WORKFLOW.md` | Branch, commit, PR, review | Planned |
| `docs/architecture/SYSTEM_ARCHITECTURE.md` | Kiến trúc Phase 1: backend, sync, Flutter, triển khai | v1 — đã duyệt |
| `docs/architecture/BACKEND_SKELETON_PLAN.md` | Kế hoạch & trạng thái khởi tạo backend (S1–S8) | v1 — đã duyệt, đã triển khai, build xanh |
| `docs/review/BACKEND_PHASE3_SYNC_REPORT.md` | Báo cáo Giai đoạn 3 backend (Sync push/pull): thay đổi, bản đồ SYN-01..48, việc còn lại | Hoàn thành — đã duyệt 2026-11-07 |
| `docs/review/BACKEND_PROGRESS.md` | Nhật ký tiến độ WP1–WP8 của kế hoạch tổng thể: số test, quyết định phát sinh, ánh xạ test ID | Hoàn thành (WP1–WP8) |
| `docs/review/BACKEND_FINAL_REPORT.md` | Báo cáo cuối hoàn thiện backend: WP, migration, entity sync, thay đổi hợp đồng §5, `[PROPOSED]`, rủi ro, việc hoãn, benchmark, Open Questions | Hoàn thành — **chờ người dùng xem xét** |
| `docs/ops/ALERTS.md`, `ops/grafana/lifeos-backend.json` | Cảnh báo vận hành tối thiểu và bảng điều khiển Grafana mẫu (PHASE_7 D206) | v1 — ngưỡng `[PROPOSED]` |
| `docs/review/BACKEND_FIX_REPORT.md` | Báo cáo đợt sửa backend 1 (F1–F9, V13, gauge `lifeos.job.last_success`): test đỏ→xanh, thay đổi, `[PROPOSED]` mới, điểm cần xem | Hoàn thành 2026-11-15 — **chờ người dùng xem xét** |
| `docs/review/BACKEND_PHASE4_OUTBOX_REPORT.md` | Báo cáo Giai đoạn 4 backend (outbox worker, email qua Mailpit, job dọn, Docker): thay đổi, quyết định `[PROPOSED]` P1–P14, điểm cần xem | Hoàn thành — đã duyệt 2026-11-07 |
| `docs/api/openapi.yaml`, `docs/api/fixtures/` | Hợp đồng API xuất từ backend; fixture JSON dùng chung Java–Dart (auth, finance, task, goal, note, tag, review, calendar, reminder) và vector (`habit-`, `recurrence-`, `analytics-`, `merge-vectors.json`) | Sinh tự động / dùng chung Java–Dart, kiểm bởi `OpenApiExportIT` / `ApiFixtureContractIT` / `FinanceFixtureContractIT` / `ApiFixtureShapesIT` và `mobile/test/contract_vectors_test.dart` |
| `docs/engineering/MOBILE_MASTER_PLAN.md` | Kế hoạch tổng thể hoàn thiện ứng dụng Flutter (PWP1–PWP15), ngôn ngữ thiết kế *Calm Botanical* | v1 — **đã duyệt 2026-11-08** |
| `docs/review/MOBILE_PROGRESS.md` | Nhật ký tiến độ PWP1–PWP15 của ứng dụng Flutter, số test, quyết định phát sinh | Hoàn thành (PWP1–PWP15) |
| `docs/review/MOBILE_FINAL_REPORT.md` | Báo cáo cuối hoàn thiện ứng dụng Flutter: PWP1–PWP15, đối chiếu vector, benchmark 50k bản ghi, điểm cần xem | Hoàn thành — **chờ người dùng xem xét** |
| `docs/review/MOBILE_BACKEND_ISSUES.md` | Nhật ký đối chiếu hợp đồng Mobile – Backend | Hoàn thành (0 lỗi phát sinh) |
| `docs/architecture/MODULE_BOUNDARIES.md` | Ma trận phụ thuộc module | Planned |
| `docs/architecture/DATABASE_DESIGN.md` | ERD, DDL, SQLite local, vòng đời dữ liệu | v1 — đã duyệt |
| `docs/architecture/ADR/` | Architecture Decision Records | Planned |
| `docs/product/PRODUCT_CONTEXT.md` | Baseline sản phẩm & kiến trúc (đã rà soát) | v1 |
| `docs/product/PRD.md` | PRD Phase 1 (Foundation & Task Management) | v1 — đã duyệt |
| `docs/product/USE_CASES.md` | Use cases Phase 1 | v1 — đã duyệt |
| `docs/engineering/BACKEND_MASTER_PLAN.md` | Kế hoạch tổng thể hoàn thiện toàn bộ backend (WP1–WP8), quyết định mặc định, giới hạn tự chủ | v1 — **đã duyệt 2026-11-07** |
| `docs/phases/README.md` | Tổng quan Phase 2–7: phụ thuộc, thay đổi hợp đồng cần duyệt riêng, câu hỏi xuyên suốt | v1 — **đã duyệt 2026-11-07** |
| `docs/phases/PHASE_2_HABIT_ROUTINE.md` | Phase 2 Habit & Routine, cuối MVP (D230–D259) | v1 — đã duyệt 2026-11-07 |
| `docs/phases/PHASE_3_FINANCE.md` | Phase 3 Personal Finance (D100–D119) | v1 — đã duyệt 2026-11-07 |
| `docs/phases/PHASE_4_CALENDAR_NOTIFICATION.md` | Phase 4 Calendar & Notification (D120–D149) | v1 — đã duyệt 2026-11-07 |
| `docs/phases/PHASE_5_SYNC_ADVANCED.md` | Phase 5 Offline Sync nâng cao (D150–D179) | v1 — đã duyệt 2026-11-07 |
| `docs/phases/PHASE_6_KNOWLEDGE_GOALS.md` | Phase 6 Knowledge & Goals (D180–D189) | v1 — đã duyệt 2026-11-07 |
| `docs/phases/PHASE_7_INTELLIGENCE_ANALYTICS.md` | Phase 7 Life Intelligence & Analytics (D190–D219) | v1 — đã duyệt 2026-11-07 |
| `docs/phases/PHASE_8_AI_ASSISTANT.md` | Phase 8 AI Assistant, consent/an toàn/hợp đồng/action proposal (D260–D299) | v1 — **đã duyệt 2026-11-10** |
| `docs/engineering/DEPLOY_AI_MASTER_PLAN.md` | Kế hoạch Deploy DWP1–DWP8 và AI AIP1–AIP7 | v1 — **đã duyệt 2026-11-10** |
| `docs/review/DEPLOY_AI_PROGRESS.md` | Nhật ký thực thi DWP/AIP, quyết định phát sinh và lệnh kiểm chứng | Đang thực hiện |
| `docs/review/DEPLOY_AI_FINAL_REPORT.md` | Báo cáo Deploy + AI: AIP2/AIP4 xong, AIP3/AIP7 một phần, DWP8 **NO-GO production**, việc chờ con người | Cập nhật 2026-11-14 (đồng bộ 2026-11-19) |
| `docs/review/MOBILE_AI_ASSISTANT_REPORT.md` | Nghiệm thu Trợ lý AI trên mobile (AWP-1..AWP-6), D-M09 | Hoàn thành 2026-11-15 — **chờ người dùng xem xét** |
| `docs/review/PENDING_DECISIONS.md` | Sổ quyết định còn treo của backend, mobile, deploy/AI; mục còn mở ở §0 | Đồng bộ 2026-11-19 |

Tài liệu trong `docs/phases/` và `BACKEND_MASTER_PLAN.md` **đã có hiệu lực** (người dùng duyệt 2026-11-07). Claude Code hoàn thiện toàn bộ backend theo `BACKEND_MASTER_PLAN.md` (WP1–WP8), làm liên tục, chỉ dừng theo §6 của kế hoạch đó.

Khi hai tài liệu mâu thuẫn: ADR đã duyệt > tài liệu chuyên biệt > `CODE_GUIDELINES.md` > `AGENTS.md`. Nếu không rõ, hỏi người dùng.

---

## 7. Definition of Done (mỗi task)

- [ ] Tuân thủ mọi mục `[CONFIRMED]`; không dùng mục `[PROPOSED]` khi chưa được duyệt.
- [ ] Có test phù hợp và test chạy qua (nếu lệnh test đã được chốt).
- [ ] Ownership/`user_id` được kiểm tra ở mọi truy cập dữ liệu cá nhân.
- [ ] Không có external call trong transaction; event đi qua outbox.
- [ ] Không vi phạm module boundary.
- [ ] Báo cáo cuối task nêu: thay đổi, giả định, điểm cần review, Open Questions mới.
