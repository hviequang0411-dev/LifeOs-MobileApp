# LifeOS — Engineering Guidelines v1

> **Trạng thái:** v1 — đã duyệt toàn bộ rule và D1–D12. D14–D71 đã duyệt 2026-11-05 (xem §15.1). Các mục `[OPEN]` còn lại được chốt theo lịch ở §15.2.
> **Phạm vi:** Backend (Spring Boot), Offline Sync, Flutter + SQLite, kèm tóm tắt Testing/Observability.
> **Nguồn:** `LifeOS – Product Context & Architecture Baseline`.
> Đã tách thành `JAVA_SPRING_RULES.md`, `DATABASE_RULES.md`, `SYNC_RULES.md`, `FLUTTER_RULES.md`. Còn lại (Planned): `API_CONVENTIONS.md`, `SECURITY_RULES.md`, `TESTING_RULES.md`, `GIT_WORKFLOW.md`.

---

## 0. Cách đọc tài liệu này

Mỗi quy tắc mang một nhãn:

| Nhãn | Ý nghĩa | Agent được phép |
|---|---|---|
| `[CONFIRMED]` | Đã được chốt trong baseline | Áp dụng bắt buộc |
| `[PROPOSED]` | Đề xuất hợp lý nhưng **chưa được duyệt** | Chỉ dùng sau khi người dùng đồng ý |
| `[OPEN]` | Chưa có quyết định | Dừng và hỏi |

Từ khóa **MUST / MUST NOT / SHOULD** theo nghĩa thông thường (RFC 2119).

Mọi thay đổi tài liệu này phải qua review của con người. Ngoại lệ so với guideline cần ADR trong `docs/architecture/ADR/`.

---

## Bản đồ tài liệu engineering

Nội dung chi tiết đã được tách; số mục giữ nguyên.

| Mục | Nội dung | File |
|---|---|---|
| §2–§7 | Phân tầng, transaction, module, DTO, exception, bảo mật | `JAVA_SPRING_RULES.md` |
| §8 | Persistence, timezone, cột đồng bộ | `DATABASE_RULES.md` |
| §9 | Offline sync, idempotency, optimistic locking | `SYNC_RULES.md` |
| §10, §11 | Outbox/Notification, AI | `JAVA_SPRING_RULES.md` |
| §12 | Flutter và SQLite | `FLUTTER_RULES.md` |
| §13–§16 | Testing (tóm tắt), observability, decision log, quy trình review | file này |

---

## 1. Nguyên tắc nền tảng

1. `[CONFIRMED]` PostgreSQL là nguồn sự thật sau đồng bộ; SQLite là local persistence. Backend kiểm tra business rules; client không quyết định tính hợp lệ nghiệp vụ.
2. `[CONFIRMED]` Dữ liệu mỗi user được cô lập. Mọi dữ liệu cá nhân gắn `user_id`.
3. `[CONFIRMED]` Không thêm công nghệ chỉ để kiến trúc phức tạp. Redis/RabbitMQ chỉ thêm khi có nhu cầu thật.
4. `[CONFIRMED]` AI-assisted, human-controlled: AI không tự ghi dữ liệu.

---

## 13. Testing (tóm tắt — chi tiết ở `TESTING_RULES.md`)

`[CONFIRMED]`

- **Unit test:** domain/entity và application service (business rules, state transition, tính tiền).
- **Integration test:** repository + service với PostgreSQL thật (Testcontainers), kiểm tra constraint, index, migration.
- **Architecture test:** ArchUnit kiểm tra layering và module boundary (§2, §4).
- **Sync test bắt buộc:** idempotency (gửi lại cùng `mutationId`), version conflict, tombstone, batch có mutation lỗi xen kẽ, pull theo cursor nhiều trang.
- **Security test:** user A không đọc/sửa được dữ liệu user B ở mọi endpoint dữ liệu cá nhân.
- **Finance test:** làm tròn/Decimal, transfer không vào báo cáo thu/chi, recurring không tạo trùng.
- **Flutter:** unit test cho repository/sync engine với SQLite in-memory; widget test cho luồng chính.
- Mỗi bug fix kèm test tái hiện.

---

## 14. Observability và Logging `[CONFIRMED]`

- Log có cấu trúc, kèm `correlationId`/`requestId`, `userId` (không PII khác).
- Không log: mật khẩu, token, nội dung ghi chú, số tiền chi tiết kèm định danh.
- Metrics qua Actuator + Prometheus: latency API, tỉ lệ conflict sync, độ trễ/số lần retry của outbox, lỗi delivery notification, token/cost AI.
- Mục tiêu p95 < 300ms cho CRUD thường là *định hướng*, cần xác nhận bằng benchmark `[CONFIRMED]` (baseline §13).

---

## 15. Decision Log

### 15.1. Đã chốt

| # | Quyết định | Kết quả | Tham chiếu |
|---|---|---|---|
| D1 | Công cụ kiểm tra module boundary | ArchUnit | §4.3 |
| D2 | Ma trận phụ thuộc | Task → Project một chiều; Routine không phụ thuộc Habit ở MVP | §4.4 |
| D3 | FK xuyên module ở DB | Cho phép FK trên `id`; JPA không có quan hệ xuyên module | §4.2 |
| D4 | Entity JPA và domain | Dùng chung ở MVP | §5 |
| D5 | Mapper | MapStruct, `unmappedTargetPolicy = ERROR` | §5 |
| D6 | Định dạng lỗi | RFC 9457 Problem Details + `errorCode` | §6.2 |
| D7 | Migration | Flyway | §8 |
| D8 | Sync cursor | `seq` theo user | §9.3 |
| D9 | State management Flutter | Riverpod | §12.1 |
| D10 | SQLite cho Flutter | Drift | §12.1 |
| D11 | Cột đồng bộ Phase 1 | Cột chuẩn + `change_logs` từ Phase 1 | §8.2 |
| D12 | Timezone | Theo user, tự nhận từ thiết bị, đổi tay được | §8.1 |
| D14–D28 | Quyết định kiến trúc (Maven, JWT HS256, Argon2id, interface `common.sync`, pull theo state, cascade phía server, outbox lease, DB local riêng theo user, v.v.) | Đã duyệt 2026-11-05 | `SYSTEM_ARCHITECTURE.md` §13 |
| D29–D40 | Quyết định thiết kế DB (token family, `user_sync_state`, `sync_mutations`, tìm kiếm local, v.v.) | Đã duyệt 2026-11-05 | `DATABASE_DESIGN.md` §13 |
| D41–D44 | Từ review ngoài: luật A12, `lock_timeout`, rate limit mở rộng, viết SECURITY_RULES | Đã duyệt 2026-11-05 | `SYSTEM_ARCHITECTURE.md` §13 |
| D72 | Xoay vòng refresh token nguyên tử (từ review ngoài vòng 2) | Đã duyệt 2026-11-05 | `SECURITY_RULES.md` §4.2 |
| D73 | Mã lỗi giao thức chung `NOT_FOUND`, `METHOD_NOT_ALLOWED`, `REQUEST_REJECTED` (phát sinh khi dựng skeleton) | Đã duyệt theo ủy quyền "làm theo khuyến nghị"; xem lại khi review | `API_CONVENTIONS.md` §5.1 |
| D45–D55, D71 | Bảo mật: mật khẩu, token, thu hồi thiết bị, rate limit, IDOR, log; trần refresh family 90 ngày | Đã duyệt 2026-11-05 | `SECURITY_RULES.md` §17 |
| D56–D63 | Quy ước API: camelCase, lỗi, `version`, phân trang, 426, OpenAPI | Đã duyệt 2026-11-05 | `API_CONVENTIONS.md` §13 |
| D64–D70 | Kiểm thử: bộ test bắt buộc sync/bảo mật/migration, fixture hợp đồng, cổng CI | Đã duyệt 2026-11-05 | `TESTING_RULES.md` §15 |

### 15.2. D13 — Các mục baseline §16 còn lại: chốt đúng lúc cần

| Khi nào | Mục cần chốt |
|---|---|
| Trước Phase 1 | **Đã chốt:** access token 15 phút, refresh token 30 ngày có xoay vòng; có bảng token xác minh email. **Đã chốt sau đó:** rate limit (SECURITY_RULES §6, D49), chính sách mật khẩu (PRD Q-01), thời hạn token (PRD Q-01, SECURITY D47). **Còn mở:** phiên bản OS tối thiểu (không chặn backend Phase 1) |
| Trước Phase 2 | Vòng đời trạng thái Task, subtask, recurrence, quan hệ Routine/Habit |
| Trước Phase 3 | Đa tiền tệ, opening balance, hoàn tiền, công thức báo cáo |
| Trước Phase 4 | Retry policy, retention của notification, quiet hours chi tiết, reminder lặp theo wall-clock hay instant |
| Trước Phase 5 | Batch size, retention của tombstone và cursor, full resync |
| Trước Phase 8 | AI provider, model, usage limit, retention |

### 15.3. Mục `[PROPOSED]`

Không còn mục `[PROPOSED]` nào chờ duyệt. Mục mới đề xuất từ nay phải gắn `[PROPOSED]` và chờ người dùng duyệt.

---

## 16. Quy trình review guidelines

1. Con người đọc và đánh dấu từng mục `[CONFIRMED]` còn lại thành `[CONFIRMED]`, sửa hoặc loại bỏ.
2. Mục đã duyệt được chuyển vào tài liệu chuyên biệt tương ứng; quyết định kiến trúc quan trọng ghi thành ADR.
3. Chỉ sau bước này agent mới được implement theo guidelines. Agent không tự duyệt.
4. Thay đổi guideline sau đó: PR riêng, chỉ chứa thay đổi tài liệu, do con người duyệt.
