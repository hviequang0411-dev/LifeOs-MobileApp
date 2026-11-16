# LifeOS — Java / Spring Boot Rules

> **Trạng thái:** v1 — tách từ `CODE_GUIDELINES.md`. **Giữ nguyên số mục** của Engineering Guidelines v1 để các tham chiếu `§x` vẫn đúng.
> **Phạm vi:** Phân tầng, transaction, module boundary, DTO/Entity/Mapper, exception, bảo mật, outbox/notification, AI (§2–§7, §10, §11).
> Quy ước nhãn `[CONFIRMED]` / `[CONFIRMED]` / `[OPEN]` và quy trình review: xem `CODE_GUIDELINES.md` §0 và §16. Agent chỉ áp dụng `[CONFIRMED]`.

---

## 2. Phân tầng Backend

### 2.1. Luồng gọi `[CONFIRMED]`

```text
Controller  →  Application Service  →  Domain (Entity) / Repository
```

| Tầng | Trách nhiệm | MUST NOT |
|---|---|---|
| Controller | Nhận HTTP, validate cú pháp (Bean Validation), lấy user hiện tại, gọi 1 Application Service, trả DTO | Chứa business logic; gọi Repository; mở transaction; trả Entity |
| Application Service | Một use case = một method public. Điều phối: kiểm tra ownership, gọi domain, repository, publish event/outbox. **Là transaction boundary** | Biết về HTTP (`HttpServletRequest`, `ResponseEntity`); trả Entity ra ngoài module |
| Domain / Entity | Giữ invariant và hành vi của chính nó (vd `task.complete()`, `task.reopen()`) | Gọi service/repository; gọi hệ thống ngoài |
| Repository | Truy cập dữ liệu | Chứa business rule; bị module khác gọi |

### 2.2. Quy ước use case `[CONFIRMED]`

- Application Service chia theo use case hoặc nhóm use case gần nhau (`TaskCommandService`, `TaskQueryService`) thay vì một `TaskService` phình to. Command và query tách để dễ áp `readOnly` transaction và cache sau này.
- Đầu vào của Application Service là **Command/Query object** (record), không phải DTO của HTTP, để sync và AI cũng gọi được cùng use case mà không phụ thuộc controller.

---

## 3. Transaction boundary

1. `[CONFIRMED]` Transaction nằm ở **Application Service**. Không đặt `@Transactional` ở Controller hoặc Repository.
2. `[CONFIRMED]` `@Transactional` đặt trên method public của Application Service (command). Query dùng `@Transactional(readOnly = true)`.
3. `[CONFIRMED]` **Không gọi FCM, email, LLM hay bất kỳ HTTP ngoài nào trong transaction.** Thay vào đó ghi `outbox_events` trong *cùng* transaction với dữ liệu nghiệp vụ; worker xử lý sau.
4. `[CONFIRMED]` Một use case = một transaction. Không lồng nhiều transaction độc lập để "gần giống" một use case. Nếu cần nhiều bước độc lập, mỗi bước phải idempotent.
5. `[CONFIRMED]` Không gọi method `@Transactional` của chính class đó qua `this` (self-invocation làm proxy không hoạt động).
6. `[CONFIRMED]` Khi sync push: business change + `sync_mutations` result + `change_logs` được lưu trong cùng transaction (xem §9).
7. `[CONFIRMED]` Transaction ngắn: không giữ transaction trong khi chờ I/O chậm, không load danh sách lớn vào bộ nhớ.

---

## 4. Module boundaries

### 4.1. Nguyên tắc `[CONFIRMED]`

- Modular Monolith, không Microservices ở giai đoạn đầu.
- Module khác **không** truy cập trực tiếp Repository nội bộ. Giao tiếp qua public application service hoặc query interface.

### 4.2. Có được truy cập Repository/Entity của module khác không?

**Không.** `[CONFIRMED]`

- Không inject Repository của module khác.
- Không tham chiếu Entity JPA của module khác, kể cả quan hệ `@ManyToOne`/`@OneToMany` xuyên module. Tham chiếu bằng **ID (UUID)**.
- Đảm bảo toàn vẹn xuyên module bằng: (1) kiểm tra ownership/tồn tại qua public API của module kia (vd `ProjectApi`); (2) `[CONFIRMED]` (D3) **được phép** tạo FK ở DB trên `id` giữa bảng của các module khác nhau, vì cùng một PostgreSQL; JPA chỉ giữ cột UUID, không có quan hệ `@ManyToOne`/`@OneToMany` xuyên module.

### 4.3. Cấu trúc package mỗi module `[CONFIRMED]`

Giữ cấu trúc baseline và thêm `api` làm mặt tiền công khai:

```text
com.lifeos.task/
├── api/            # CÔNG KHAI: interface + DTO/event mà module khác được dùng
├── controller/     # REST (package-private nếu có thể)
├── service/        # Application services (impl của api/)
├── repository/     # package-private
├── entity/         # package-private
├── dto/            # Request/Response của HTTP
└── mapper/
```

- Chỉ `api/` được import từ module khác.
- Mọi class còn lại SHOULD là package-private hoặc ít nhất không được import chéo.
- `[CONFIRMED]` (D1) Kiểm tra tự động bằng **ArchUnit** trong CI: layering (§2), chỉ `api/` được import chéo module, không vòng phụ thuộc, khớp ma trận §4.4.

### 4.4. Ma trận phụ thuộc `[CONFIRMED]`

"A → B" nghĩa là A được gọi `api` của B. Mọi cặp không liệt kê là **cấm**.

| Module | Được phụ thuộc vào |
|---|---|
| `common` | (không module nghiệp vụ nào) |
| `identity` | `common` |
| `project` | `common` (không phụ thuộc `task`) |
| `task` | `common`, `project` (kiểm tra project tồn tại/thuộc user qua `ProjectApi`). Sở hữu tính tiến độ project |
| `habit` | `common` |
| `routine` | `common` (không phụ thuộc `habit` ở MVP; liên kết về sau cần ADR) |
| `calendar` | `common`, `task` (time block gắn task) |
| `finance` | `common` |
| `note` | `common`, `task`, `goal` (link) |
| `goal` | `common`, `task`, `habit` (link + tính progress qua query interface) |
| `analytics` (gom dashboard + review + analytics theo D197 — duyệt 2026-11-15, thay cặp `dashboard`, `lifereview`) | **chỉ đọc** từ `task, habit, routine, calendar, finance, goal` qua query interface |
| `notification` | `common`, `identity` (tham chiếu `device_id`). Sở hữu `reminders` và `device_push_tokens` `[CONFIRMED]`. **Nhận dữ liệu từ module khác qua outbox event**, không bị gọi trực tiếp |
| `sync` | Gọi application service của các module có dữ liệu đồng bộ qua `MutationHandler` đăng ký theo `entityType` |
| `ai` | Đọc qua query interface (ContextBuilder); thực thi hành động đã duyệt qua application service của module đích |

Quy tắc đi kèm:
- **Cấm vòng phụ thuộc.** Nếu hai module cần nhau, tách phần chung xuống `common` hoặc dùng domain event.
- `[CONFIRMED]` (D2) Task ↔ Project một chiều: `task → project`. Tiến độ project do module `task` tính (`TaskQueryApi`) và expose qua `GET /projects/{id}/progress` ở controller của `task`.
- Module nghiệp vụ **không** phụ thuộc `sync`, `notification`, `ai`, `analytics`. Chúng phát event; các module kia lắng nghe/đọc.

### 4.5. Giao tiếp bất đồng bộ giữa module `[CONFIRMED]`

- Domain event nội bộ (Spring `ApplicationEventPublisher`) cho phản ứng *trong cùng transaction* chỉ khi thật sự cần.
- Phản ứng không thuộc transaction (notification, indexing embedding) luôn đi qua **Transactional Outbox**.


### 4.6. Quyết định đã chốt (ghi nhận từ review)

- `[CONFIRMED]` `notification` sở hữu `reminders`. Reminder là entity đồng bộ độc lập, tham chiếu nguồn bằng `(entityType, entityId)`; Task/Calendar **không** gọi module notification trực tiếp.
- `[CONFIRMED]` `identity` sở hữu `devices`; `notification` sở hữu `device_push_tokens` (tham chiếu `device_id`).
- `[CONFIRMED]` Tên package: `analytics` (một module cho dashboard + review + analytics theo D197; duyệt 2026-11-15, thay tên dự kiến `lifereview`).
- `[CONFIRMED]` Công cụ và thư viện (D1–D10): ArchUnit; MapStruct (`unmappedTargetPolicy = ERROR`); RFC 9457 Problem Details; Flyway; Riverpod (`flutter_riverpod`, chưa dùng code generation); Drift.
- `[CONFIRMED]` Nền tảng: Android + iOS. Xác thực MVP: email + mật khẩu có xác minh email (cần Email Provider từ Phase 1), không đăng nhập mạng xã hội. Chi tiết timezone/settings ở §8.1.
- `[CONFIRMED]` MVP = Phase 1–2. Phase 1 có sync push/pull tối thiểu theo đúng giao thức sync (§9.7); Phase 5 nâng cấp, không viết lại. Schema Phase 1 có đủ cột đồng bộ và `change_logs` (§8.2).

---

## 5. DTO, Entity, Mapper

1. `[CONFIRMED]` Không trả Entity ra API (xem §2.1).
2. `[CONFIRMED]` Phân biệt 4 loại đối tượng:

| Loại | Ví dụ | Nơi đặt | Ghi chú |
|---|---|---|---|
| HTTP Request/Response DTO | `CreateTaskRequest`, `TaskResponse` | `dto/` | Dùng Java `record`; Bean Validation ở đây |
| Command/Query | `CreateTaskCommand`, `ListTasksQuery` | `service/` hoặc `api/` | Đầu vào Application Service; không phụ thuộc HTTP |
| Cross-module DTO/View | `TaskSummary` | `api/` | Dữ liệu module khác được phép thấy |
| JPA Entity | `Task` | `entity/` | Không rời khỏi module |

3. `[CONFIRMED]` Entity: có `id (UUID)`, `userId`, `version`, `deletedAt`, `createdAt`, `updatedAt`; hành vi nghiệp vụ là method của entity; không dùng `@Data`; `equals/hashCode` theo `id`; không setter công khai cho trạng thái có invariant.
4. `[CONFIRMED]` Mapper (D5): dùng **MapStruct** với `unmappedTargetPolicy = ERROR`; một mapper cho mỗi module, không chứa business logic, không gọi repository/service.
5. `[CONFIRMED]` (D4) Dùng chung một entity cho JPA và domain ở MVP (không tách domain model riêng); xét lại bằng ADR nếu logic phức tạp (vd Finance).
6. `[CONFIRMED]` Tiền: `NUMERIC`/`BigDecimal` kèm `currency_code`. DTO không dùng `double`.

---

## 6. Exception và Validation

### 6.1. Hai lớp validation `[CONFIRMED]`

| Lớp | Kiểm tra gì | Công cụ | Lỗi |
|---|---|---|---|
| Cú pháp (Controller/DTO) | Bắt buộc, độ dài, định dạng, range | Bean Validation | 400 |
| Nghiệp vụ (Application Service/Domain) | Trạng thái hợp lệ, ownership, quy tắc finance, conflict | Code | 4xx theo loại |

Client có thể validate để UX tốt nhưng **không được tin** (`[CONFIRMED]`: backend là nơi quyết định).

### 6.2. Hệ thống exception `[CONFIRMED]`

- Gốc: `BusinessException` mang `errorCode` ổn định (vd `TASK_NOT_FOUND`, `ENTITY_VERSION_CONFLICT`).
- Các nhánh chính: `NotFoundException` (404), `ConflictException` (409, gồm version conflict), `ValidationException` (400/422), `ForbiddenException` (403), `UnauthorizedException` (401).
- Truy cập dữ liệu của user khác trả **404**, không 403, để không lộ sự tồn tại của tài nguyên.
- Một `@RestControllerAdvice` duy nhất ở `common/exception`, trả lỗi theo **RFC 9457 Problem Details** (`ProblemDetail` của Spring) (D6), kèm `errorCode` ổn định và `fieldErrors` cho lỗi validation.
- MUST NOT: lộ stack trace, SQL, tên bảng ra client; nuốt exception (`catch` rỗng); dùng exception làm control flow thông thường.
- Lỗi xảy ra trong một mutation của sync **không làm hỏng cả batch**; xem §9.

---

## 7. Bảo mật và ownership

1. `[CONFIRMED]` Spring Security + JWT. Ownership được validate ở backend.
2. `[CONFIRMED]` `userId` lấy từ security context qua một abstraction (`CurrentUser`), **không** từ path/body.
3. `[CONFIRMED]` Repository của dữ liệu cá nhân dùng các phương thức luôn kèm `userId` (`findByIdAndUserId…`); cấm `findById(id)` trần cho dữ liệu cá nhân.
4. `[CONFIRMED]` API key AI không nằm trong Flutter; mọi AI request đi qua Spring Boot.
5. `[CONFIRMED]` Secrets lấy từ biến môi trường/secret store, không commit vào repo. Không log token, mật khẩu, nội dung tài chính chi tiết, nội dung ghi chú.
6. `[CONFIRMED]` Access token sống **15 phút**; refresh token sống **30 ngày** và được **xoay vòng** (mỗi lần refresh cấp token mới, token cũ bị vô hiệu). Refresh token lưu dạng hash trong `refresh_tokens`, gắn `device_id`; thu hồi thiết bị phải vô hiệu refresh token của thiết bị đó.
7. `[CONFIRMED]` Xác minh email và reset mật khẩu dùng token dùng một lần, lưu dạng hash, có hạn dùng (bảng `email_verification_tokens`, `password_reset_tokens`).
8. `[CONFIRMED]` Rate limit đăng nhập, chính sách mật khẩu, thời hạn token xác minh/reset đã chốt (PRD §12 Q-01..Q-11; `SECURITY_RULES.md` §4–§6, D47–D49). Audit: ghi sự kiện bảo mật có cấu trúc vào log ở Phase 1, bảng `audit_logs` để sau (D53). Chi tiết ở `SECURITY_RULES.md`.

---

## 10. Outbox, Notification, Background job

1. `[CONFIRMED]` Domain transaction ghi dữ liệu nghiệp vụ + `outbox_events`. Worker đọc event chưa xử lý và gọi Dispatcher.
2. `[CONFIRMED]` Delivery lưu riêng theo channel (`notification_deliveries`); retry cho lỗi tạm thời; idempotency chống gửi trùng; tuân thủ preferences và quiet hours.
3. `[CONFIRMED]` Outbox worker phải *at-least-once* → consumer MUST idempotent (khóa theo `event_id` + channel).
4. `[CONFIRMED]` Worker lấy event bằng `FOR UPDATE SKIP LOCKED` hoặc cơ chế tương đương để chạy được nhiều instance sau này.
5. `[CONFIRMED]` Job định kỳ (reminder scheduler, recurring transaction) phải idempotent và chịu được chạy lặp/chạy trễ.
6. `[CONFIRMED]` Timezone theo user (D12); retry/backoff của outbox (D23). `[OPEN]` hoãn đến Phase 4–5: quiet hours (Phase 4) và retention (Phase 5).

---

## 11. AI

1. `[CONFIRMED]` Luồng: authenticate → ContextBuilder lấy dữ liệu *được cấp quyền* → LLM → structured output → Backend validate → người dùng duyệt → Business Service thực thi → ghi kết quả.
2. `[CONFIRMED]` LLM trừu tượng qua interface `LLMProvider`; không phụ thuộc trực tiếp vào SDK nhà cung cấp trong code nghiệp vụ.
3. `[CONFIRMED]` Không gửi dữ liệu không cần thiết; Finance cần quyền truy cập riêng; AI không tự thực hiện giao dịch tài chính.
4. `[CONFIRMED]` AI action đi qua bảng `ai_action_proposals` với trạng thái `PROPOSED → APPROVED/REJECTED → EXECUTED/FAILED`. Thực thi gọi **cùng Application Service** mà UI dùng, không có đường tắt.
5. `[CONFIRMED]` Tool của AI (`TaskTool`…) chỉ *đọc* hoặc *tạo proposal*; không tool nào ghi trực tiếp.
6. `[CONFIRMED]` Ghi `ai_usage_logs` (token, latency, cost) cho mỗi request.
