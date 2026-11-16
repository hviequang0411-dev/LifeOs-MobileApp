# LifeOS — Giao thức đồng bộ: đặc tả cài đặt (Giai đoạn 3 backend)

> **Trạng thái:** v1 — D81–D92 đã được người dùng duyệt ngày 2026-11-06 (khi giao Giai đoạn 3), nay là `[CONFIRMED]`. **Đã triển khai và kiểm thử đủ SYN-01..48 ở Giai đoạn 3 backend, duyệt 2026-11-07** — báo cáo: `docs/review/BACKEND_PHASE3_SYNC_REPORT.md`.
> **Nguồn (đã `[CONFIRMED]`, không viết lại ở đây):** `SYSTEM_ARCHITECTURE.md` §3.3, §3.5–3.8, §4 (JSON hợp đồng, nguồn sự thật nếu mâu thuẫn); `SYNC_RULES.md` §9; `DATABASE_DESIGN.md` §8 (`sync_mutations`, `change_logs`, `user_sync_state`); `SECURITY_RULES.md` §9; `API_CONVENTIONS.md` §8.7; `TESTING_RULES.md` §6 (SYN-01..21).
> **Mục đích:** lấp mọi chỗ mà người cài đặt sẽ phải tự quyết định: ma trận kết quả theo (thao tác × trạng thái entity), đặc tả payload từng `entityType`, phân loại lỗi, thứ tự khóa chống deadlock, chi tiết pull, danh sách test bổ sung (SYN-22..45).
> **Phạm vi:** backend. Phía client xem `FLUTTER_RULES.md` và `SYSTEM_ARCHITECTURE.md` §5.

---

## 1. Tổng quan luồng và ranh giới trách nhiệm

| Thành phần | Trách nhiệm | Không được làm |
|---|---|---|
| `sync.controller.SyncController` | Nhận `POST /sync/push`, `GET /sync/pull`; kiểm tra cấu trúc request | Chứa logic nghiệp vụ |
| `sync.service.SyncPushService` | Lặp qua mutation, mở transaction T1 cho từng mutation, ghi kết quả vào `sync_mutations`, định tuyến theo `entityType` | Import class của `task`/`project`/`identity` |
| `sync.service.SyncPullService` | Đọc `change_logs`, gom theo entity, gọi `loadSnapshots`, dựng phản hồi | Ghi dữ liệu; khóa `user_sync_state` |
| `sync.repository.MutationResultStore` (JdbcClient) | Đọc/ghi `sync_mutations` | Nằm ngoài module `sync` |
| `<module>.sync.*SyncHandler` (trong từng module nghiệp vụ) | Dịch `Mutation` → lệnh của `*CommandService`; dịch kết quả → `MutationOutcome`; `loadSnapshots` | Chứa business rule; gọi repository ghi trực tiếp; phụ thuộc module khác ngoài `common` |
| `*CommandService` | Toàn bộ business rule; gọi `ChangeRecorder.record` | Biết mình được gọi từ REST hay sync |

Quy tắc bất biến `[CONFIRMED]` (nhắc lại): sync **không** ghi thẳng vào repository; validate payload sync **giống hệt** REST (cùng service); `ChangeRecorder` là đường duy nhất sinh `change_logs`.

Handler đặt ở package `..sync..` của module sở hữu entity (`task.sync.TaskSyncHandler`, `task.sync.CategorySyncHandler`, `project.sync.ProjectSyncHandler`, `identity.sync.UserSettingsSyncHandler`). ArchUnit hiện có (A1–A12) phải tiếp tục xanh; nếu cần luật mới cho package `..sync..` của module nghiệp vụ (chỉ được phụ thuộc `common.sync`, `common.*` và service cùng module) thì thêm luật, **không** nới luật cũ.

---

## 2. Định dạng request/response (bổ sung cho `SYSTEM_ARCHITECTURE.md` §4)

### 2.1. Kiểm tra cấu trúc toàn request `[CONFIRMED]` D90

Lỗi cấu trúc làm **cả request** thất bại theo RFC 9457 (không có `results`):

| Điều kiện | HTTP | `errorCode` |
|---|---|---|
| JSON hỏng, thiếu `mutations`, kiểu sai | 400 | `MALFORMED_REQUEST` |
| `mutations` rỗng | 400 | `VALIDATION_FAILED` (fieldError `mutations`, mã `SIZE`) |
| `mutations` > 50 | 400 | `SYNC_BATCH_TOO_LARGE` (không mutation nào được xử lý) |
| Một mutation có `mutationId`/`entityId` không phải UUID, `operation` ngoài `CREATE|UPDATE|DELETE`, `baseVersion` không phải số nguyên ≥ 0, `entityType` thiếu hoặc rỗng | 400 | `MALFORMED_REQUEST` (kèm vị trí `mutations[i]`) |
| `entityType` dài hơn 30 ký tự (cột `sync_mutations.entity_type` là `varchar(30)`) | 400 | `MALFORMED_REQUEST` `[CONFIRMED]` D93 |
| `payload` có mặt nhưng không phải JSON object | 400 | `MALFORMED_REQUEST` `[CONFIRMED]` D93 |
| Body > 1 MB | 413 | `PAYLOAD_TOO_LARGE` (do `RequestSizeLimitFilter`; client phải chia lô nhỏ hơn: 50 × 64 KB có thể vượt 1 MB) |

`entityType` **lạ** (không thuộc `task|project|category|user_settings`) **không** là lỗi toàn request: mutation đó nhận `REJECTED` + `SYNC_UNKNOWN_ENTITY_TYPE` (SYN-09), các mutation khác vẫn xử lý.

Với `MALFORMED_REQUEST` của mục này, vị trí lỗi trả trong `fieldErrors` (ví dụ `[{"field":"mutations[3].entityId","code":"INVALID_VALUE"}]`) — ngoại lệ có chủ ý so với `API_CONVENTIONS.md` §5 (D93).

`payload` vắng mặt: `CREATE` coi như `{}` (sẽ `REJECTED VALIDATION_FAILED` do thiếu trường bắt buộc), `UPDATE` coi như `{}` (không đổi gì, §4.4), `DELETE` bỏ qua `payload`. Một trường **trong** `payload` sai kiểu (ví dụ `projectId` không phải UUID, `weekStartDay` là chuỗi): chỉ mutation đó `REJECTED VALIDATION_FAILED` với `fieldErrors[{field, INVALID_VALUE}]`, không làm hỏng cả lô (D93).

Trường lạ ở mức mutation hoặc `payload` bị **bỏ qua** (tương thích tiến hóa, `SECURITY_RULES.md` §9.4).

### 2.2. Hình dạng kết quả từng mutation `[CONFIRMED]` D83

Mọi phần tử `results[i]` có `mutationId` và `status`. Các trường khác theo trạng thái:

| `status` | Trường bắt buộc | Ghi chú |
|---|---|---|
| `APPLIED` | `entityVersion` | `version` của entity sau khi áp dụng. Với thao tác không đổi gì (xem §4.4) vẫn `APPLIED`, `entityVersion` = version hiện tại, không có `change_logs` |
| `CONFLICT` | `serverSnapshot` | `{entityType, entityId, version, deleted, data}`. Nếu entity đã bị xóa: `deleted=true`, `data` vắng mặt |
| `REJECTED` | `errorCode`, `message` | Thêm `fieldErrors` (`[{field, code}]`) khi `errorCode = VALIDATION_FAILED`. `message` là câu chung, **không** chứa dữ liệu người dùng |
| `DUPLICATE` | `originalStatus`, cùng các trường của kết quả gốc | Phát lại **nguyên văn** kết quả đã lưu trong `sync_mutations.result`, thêm `originalStatus ∈ APPLIED|CONFLICT|REJECTED` |

- `sync_mutations.result` lưu đúng đối tượng kết quả gốc (không có `DUPLICATE`). `status` trong bảng là trạng thái gốc.
- Phát lại `CONFLICT` trả `serverSnapshot` **tại thời điểm xảy ra conflict** (có thể đã cũ); client lấy trạng thái mới qua pull. Không đọc lại entity khi phát lại (kết quả phải xác định).
- `mutationId` chỉ là khóa idempotency. Nếu cùng `mutationId` được gửi lại với `entityType/entityId/operation/baseVersion` khác bản đã lưu, server vẫn trả `DUPLICATE` của bản đã lưu và ghi một log `sync_mutation_id_reuse` (không ghi payload). Không từ chối, vì client không thể phân biệt retry hợp lệ với lỗi cấu hình.

---

## 3. Đặc tả payload theo `entityType`

Quy tắc chung `[CONFIRMED]` D82:

1. Khóa JSON camelCase. Giá trị ngày/giờ theo `SYSTEM_ARCHITECTURE.md` §4.3.
2. `CREATE`: khóa vắng mặt = giá trị mặc định của REST (nếu có) hoặc `null` (nếu cột cho phép NULL); thiếu khóa **bắt buộc** → `REJECTED VALIDATION_FAILED`.
3. `UPDATE`: **khóa vắng mặt = giữ nguyên**; `null` tường minh = xóa giá trị (chỉ với trường cho phép NULL; ngược lại `VALIDATION_FAILED`). Client chuẩn gửi đủ mọi khóa nên đây là trạng thái đầy đủ; quy tắc này chỉ để client cũ không vô tình xóa trường mới thêm. Dùng lại `common.util.Patch<T>`.
4. Các khóa do server quyết định (`id`, `userId`, `version`, `completedAt`, `createdAt`, `updatedAt`, `deletedAt`) bị bỏ qua nếu có (SYN-18). `entityId` ở mức mutation chính là `id`.
5. Validation và chuẩn hóa (trim, độ dài, enum, ràng buộc chéo như `dueTime` cần `dueDate`) **do `*CommandService` thực hiện**, giống REST. Mã lỗi giống REST (§5.1 `API_CONVENTIONS.md`); thất bại trả về dạng `REJECTED` + `errorCode` (+ `fieldErrors`).
6. Kích thước: `payload` sau khi serialize UTF-8 > 64 KB → `REJECTED PAYLOAD_TOO_LARGE` (kiểm **trước** khi gọi handler; vẫn được lưu kết quả). **Ngoại lệ theo loại** (PHASE_6 D181, BACKEND_MASTER_PLAN §5 #3, WP6): `note` ≤ 192 KB (`SyncEntityHandler.maxPayloadBytes`); body 1 MB và 50 mutation/lô giữ nguyên, một mutation `note` vẫn đếm 1.

### 3.1. `task`

| Khóa | CREATE | UPDATE | Ghi chú |
|---|---|---|---|
| `title` | bắt buộc | tùy chọn | trim, 1–200 |
| `description` | tùy chọn (null) | tùy chọn | không trim |
| `status` | tùy chọn (`OPEN`) | tùy chọn | `OPEN|DONE`; `completedAt` do server đặt/xóa |
| `priority` | tùy chọn (`NONE`) | tùy chọn | `NONE|LOW|MEDIUM|HIGH` |
| `dueDate`, `dueTime` | tùy chọn | tùy chọn | `dueTime` cần `dueDate` (REST D78: 400 → sync: `REJECTED VALIDATION_FAILED` fieldError `dueTime`) |
| `projectId`, `categoryId` | tùy chọn | tùy chọn, `null` = gỡ | tham chiếu không hợp lệ → `REJECTED PROJECT_NOT_FOUND` / `CATEGORY_NOT_FOUND` (SYN-15; cùng lỗi cho: không tồn tại, đã xóa, của user khác) |
| `goalId`, `milestoneId` | tùy chọn | tùy chọn, `null` = gỡ | PHASE_6 D186 (§5 #2, WP6). Có `milestoneId` ⇒ `goalId` = goal của milestone (server đặt; gửi kèm mà lệch → `REJECTED GOAL_LINK_MISMATCH`); chỉ đổi `goalId` ⇒ milestone cũ bị gỡ; tham chiếu sai → `GOAL_NOT_FOUND` / `MILESTONE_NOT_FOUND`. Xóa Goal/Milestone → server gỡ liên kết (UPDATE, `mutation_id` NULL) |

### 3.2. `project`

| Khóa | CREATE | UPDATE | Ghi chú |
|---|---|---|---|
| `name` | bắt buộc | tùy chọn | trim, 1–100 |
| `description` | tùy chọn | tùy chọn | |
| `archived` | tùy chọn (`false`) | tùy chọn | boolean; server đặt/xóa `archived_at`. Đổi sang đúng giá trị hiện tại = không đổi (§4.4) |

`DELETE project` chạy cùng cascade như REST (`SYSTEM_ARCHITECTURE.md` §3.8, D80): các Task bị ảnh hưởng nhận UPDATE riêng với `mutation_id = NULL`; dòng `change_logs` DELETE của Project mang `mutation_id` của mutation.

### 3.3. `category`

| Khóa | CREATE | UPDATE |
|---|---|---|
| `name` | bắt buộc, trim, 1–50, duy nhất theo user không phân biệt hoa thường | tùy chọn |

Trùng tên → `REJECTED CATEGORY_NAME_TAKEN`. `DELETE category` cascade như Project.

### 3.4. `user_settings` `[CONFIRMED]` D88

- Mỗi user có đúng **một** dòng (tạo lúc đăng ký). `entityId` = `user_settings.id` (client biết qua pull, vì đăng ký đã ghi `change_logs` CREATE).
- Chỉ cho `UPDATE`. `CREATE`/`DELETE` → `REJECTED SYNC_OPERATION_NOT_ALLOWED` (mã mới).
- `entityId` không phải dòng settings của user hiện tại → `REJECTED USER_SETTINGS_NOT_FOUND` (mã mới; cùng phản hồi cho không tồn tại và của user khác).
- Khóa: `timezone`, `locale`, `defaultCurrency`, `weekStartDay` (quy tắc giá trị như `PATCH /me/settings`).
- Tên hiển thị (`displayName`) **không** thuộc `user_settings`; không đồng bộ qua sync ở Phase 1.
- WP4 (BACKEND_MASTER_PLAN §5 #1, PHASE_4 D135, D138): thêm khóa `quietStart`, `quietEnd` (`"HH:mm"` hoặc `null`, đi
  theo cặp, khác nhau; có thể qua nửa đêm) và `showDetailsOnLockScreen` (boolean, mặc định `false`). Đổi `timezone`
  làm server tính lại reminder theo giờ địa phương trong cùng transaction (D134; change_logs của reminder đứng trước
  change_logs của `user_settings`).

### 3.5. Phase 2 — `habit`, `habit_checkin`, `routine`, `routine_step`, `routine_log`, `routine_step_log` `[CONFIRMED]` (PHASE_2 §7, D230–D254; triển khai WP2 2026-11-07)

Quy tắc chung §3 giữ nguyên (khóa vắng = giữ nguyên ở `UPDATE`, trường do server quản bị bỏ qua, validation ở service giống REST). Ngày `yyyy-MM-dd`, giờ `HH:mm`, `weekdays` là bitmask (Thứ Hai = 1 … Chủ Nhật = 64).

| `entityType` | Khóa payload | Ghi chú |
|---|---|---|
| `habit` | `name` (bắt buộc khi CREATE), `description`, `color` (`#RRGGBB`), `icon`, `scheduleType` (bắt buộc khi CREATE), `weekdays`, `timesPerWeek`, `intervalDays`, `startOn` (vắng = hôm nay theo múi giờ người dùng), `endOn`, `reminderTime` | Lịch sai D230 → `REJECTED HABIT_INVALID_SCHEDULE`. `scheduleType` có mặt ở UPDATE ⇒ ba trường lịch vắng mặt hiểu là `null` (lịch thay cả bộ, D254). ≤ 200 habit → `LIMIT_EXCEEDED`. `DELETE` cascade mọi check-in (`mutation_id` NULL) |
| `habit_checkin` | `habitId`, `localDate`, `status` (`DONE`/`SKIPPED`), `note` | `habitId`, `localDate` bất biến (đổi → `VALIDATION_FAILED`). Habit tham chiếu không có → `REJECTED HABIT_NOT_FOUND`; ngày ngoài D231 → `REJECTED HABIT_DATE_OUT_OF_RANGE`. `DELETE` = hoàn tác |
| `routine` | `name`, `kind` (`DAILY`/`WEEKLY`), `weekdays`, `reminderTime` | Lịch sai → `ROUTINE_INVALID_SCHEDULE`; ≤ 50 routine. `DELETE` cascade bước, lượt, kết quả bước |
| `routine_step` | `routineId`, `title`, `position` (vắng = max + 1000), `durationMinutes` | `routineId` bất biến; vị trí đã có bước hoạt động khác → `VALIDATION_FAILED` fieldError `position`; ≤ 30 bước → `LIMIT_EXCEEDED`. `DELETE` giữ kết quả bước cũ |
| `routine_log` | `routineId`, `localDate`, `totalSteps` (1–30) | Mọi khóa bất biến sau CREATE; `completedAt` do server. `DELETE` cascade kết quả bước |
| `routine_step_log` | `routineLogId`, `stepId`, `status` | `routineLogId`, `stepId` bất biến; bước phải thuộc routine của lượt (kể cả bước đã xóa mềm, D243) → nếu không `REJECTED ROUTINE_STEP_NOT_FOUND`. Mỗi thay đổi tính lại `completedAt` của lượt (lượt nhận change_logs UPDATE riêng, `mutation_id` NULL) |

Tham chiếu không hợp lệ trong payload luôn là `REJECTED <TYPE>_NOT_FOUND` của **tham chiếu** (422 ở REST), không bị hiểu là entity đích không tồn tại.

### 3.6. Phase 3 — Finance `[CONFIRMED]` (PHASE_3 §8, D100–D119; triển khai WP3 2026-11-07)

Tiền là **chuỗi** thập phân (`API_CONVENTIONS.md` §2); số JSON → `REJECTED VALIDATION_FAILED` fieldError `INVALID_VALUE`. Mọi lệch `baseVersion` → `CONFLICT` (D105, không LWW). Khóa do server quản trong payload bị bỏ qua (`currency` của giao dịch/chuyển khoản/định kỳ, `recurringTransactionId`, `recurrenceDate`, `nextRunOn`, `systemKey`, `status: ACHIEVED`).

| `entityType` | Khóa payload | Ghi chú |
|---|---|---|
| `financial_account` | `name`, `accountType`, `currency`, `openingBalance`, `archived` (UPDATE) | Đổi `currency` khi đã có giao dịch/chuyển khoản/định kỳ → `REJECTED FINANCE_CURRENCY_LOCKED`; `DELETE` theo FR-FIN-02 (`FINANCE_ACCOUNT_HAS_TRANSACTIONS`) |
| `financial_category` | `name`, `kind` (bất biến), `archived` (UPDATE) | `DELETE` danh mục đang dùng → `FINANCE_CATEGORY_IN_USE` |
| `financial_transaction` | `accountId`, `categoryId`, `kind`, `amount`, `occurredOn`, `note` | Tài khoản lưu trữ → `FINANCE_ACCOUNT_ARCHIVED`; danh mục sai loại → `FINANCE_CATEGORY_KIND_MISMATCH` |
| `account_transfer` | `fromAccountId`, `toAccountId` (bất biến), `amount`, `occurredOn`, `note` | Khác tiền tệ → `FINANCE_CURRENCY_MISMATCH` (cũng áp cho UPDATE `financial_transaction` / `recurring_transaction` đổi `accountId` sang tài khoản khác tiền tệ — cập nhật 2026-11-15 theo BACKEND_FIX_REPORT F1) |
| `budget` | `categoryId`, `currency`, `amount`, `archived` (UPDATE) | Trùng ngân sách hoạt động → `FINANCE_BUDGET_EXISTS` |
| `recurring_transaction` | `accountId`, `categoryId`, `kind`, `amount`, `note`, `frequency`, `intervalCount`, `startOn`, `endOn`, `status` (`ACTIVE`/`PAUSED`) | `ACTIVE` sau `PAUSED` đẩy `nextRunOn` tới lần đầu ≥ hôm nay (không bù) |
| `saving_goal` | `name`, `currency`, `targetAmount`, `targetOn`, `status` (`ACTIVE`/`ARCHIVED`) | `DELETE` cascade khoản góp (`mutation_id` NULL) |
| `saving_contribution` | `goalId` (bất biến), `amount`, `occurredOn`, `note` | Mỗi thay đổi tính lại trạng thái mục tiêu (change_logs UPDATE của mục tiêu, `mutation_id` NULL) |

Tham chiếu sai (không tồn tại, đã xóa, của user khác) → `REJECTED FINANCE_<TYPE>_NOT_FOUND` cùng một dạng (D115); mutation phụ thuộc vào một `CREATE` bị `REJECTED` cũng `REJECTED` (not found).

### 3.7. Phase 4 — `calendar_event`, `event_exception`, `time_block`, `reminder` `[CONFIRMED]` (PHASE_4 §7, D124–D134; triển khai WP4 2026-11-07)

Giờ địa phương dạng `yyyy-MM-ddTHH:mm[:ss]` (không múi giờ), thời điểm UTC dạng ISO-8601 có `Z`. Lệch `baseVersion` → `CONFLICT` thông thường. Khóa do server quản bị bỏ qua: `startAt`, `endAt`, `recurrenceUntil` (sự kiện), `nextFireAt`, `lastFiredAt`, `status` (reminder).

| `entityType` | Khóa payload | Ghi chú |
|---|---|---|
| `calendar_event` | `title`, `description`, `location`, `allDay`, `timezone` (IANA), `startLocal`/`endLocal` hoặc `startDate`/`endDate` (cả ngày), `recurrence` (đối tượng D126: `freq`, `interval`, `byWeekday` [`MO`..`SU`], `byMonthDay`, `until` \| `count`) | Quy tắc sai → `REJECTED CALENDAR_INVALID_RECURRENCE`; múi giờ sai → `CALENDAR_INVALID_TIMEZONE`; ≤ 5000 sự kiện (`LIMIT_EXCEEDED`). `allDay` có mặt ở UPDATE ⇒ trường của hình dạng kia tự bỏ. `DELETE` cascade ngoại lệ và reminder của sự kiện (`mutation_id` NULL). "Từ lần này" qua sync = UPDATE `recurrence` của chuỗi cũ + CREATE chuỗi mới |
| `event_exception` | `eventId`, `originalStartLocal` (đều bất biến), `kind` (`CANCELLED`/`MODIFIED`), `title`, `description`, `location`, `startLocal`, `endLocal` | `originalStartLocal` không khớp quy tắc → `REJECTED CALENDAR_OCCURRENCE_NOT_FOUND`; `CREATE` trùng (sự kiện, lần xuất hiện) còn sống → `CONFLICT` `conflictReason: ALREADY_EXISTS` (như §4.1) |
| `time_block` | `taskId` (bất biến), `startAt`, `endAt` (≤ 24 giờ), `note` | Task sai → `REJECTED TASK_NOT_FOUND`; xóa Task cascade block (`mutation_id` NULL) |
| `reminder` | `targetType` (`TASK`/`EVENT`/`HABIT`/`ROUTINE`/`NONE`), `targetId` (bất biến), `mode` (`OFFSET`/`LOCAL_AT`), `offsetMinutes`, `localTime` (`HH:mm`), `localDate`, `repeatRule` (D126, cần `localDate`), `urgent`, `paused` | Mục tiêu sai → `REJECTED REMINDER_TARGET_NOT_FOUND`; ≤ 10/mục tiêu, ≤ 2000 đang hoạt động (`LIMIT_EXCEEDED`). Mỗi lần nổ: `nextFireAt` tính lại, version +1, change_logs UPDATE (`mutation_id` NULL); hết mốc → `status = DONE` |

### 3.8. Phase 6 — `note`, `tag`, `note_tag`, `note_link`, `goal`, `milestone` `[CONFIRMED]` (PHASE_6 §7, D180–D189; triển khai WP6 2026-11-07)

Lệch `baseVersion` → `CONFLICT` thông thường (ghi chú: client hợp nhất theo trường, xung đột `content` luôn hỏi — D183). Khóa do server quản bị bỏ qua: `archivedAt` (ghi chú), `statusChangedAt` (goal), `completedAt` (milestone), `searchVector`.

| `entityType` | Khóa payload | Ghi chú |
|---|---|---|
| `note` | `title` (≤ 200, rỗng → null), `content` (văn bản thuần, NFC, ≤ 50 000 code point), `pinned`, `archived` (cờ; server đặt `archivedAt`) | Payload ≤ 192 KB (§3 mục 6). Vượt 50 000 → `REJECTED VALIDATION_FAILED` fieldError `content` mã `MAX_LENGTH`; ghim > 10 → `LIMIT_EXCEEDED`. `DELETE` cascade `note_tag`, `note_link` (`mutation_id` NULL) |
| `tag` | `name` (1–30, trim, NFC), `color` (`#RRGGBB` hoặc null) | `CREATE` trùng tên (không phân biệt hoa thường) với thẻ đang sống → `CONFLICT` `conflictReason: ALREADY_EXISTS` kèm thẻ đang có (REST: 409 `TAG_NAME_TAKEN`); đổi tên trùng → `REJECTED TAG_NAME_TAKEN`. `DELETE` cascade `note_tag` |
| `note_tag` | `noteId`, `tagId` | **Chỉ CREATE/DELETE** (`UPDATE` → `REJECTED SYNC_OPERATION_NOT_ALLOWED`). `CREATE` trùng cặp đang sống → `APPLIED` với `entityVersion` của bản đang có (không lỗi; client gộp theo cặp). ≤ 20 thẻ/ghi chú → `NOTE_TAG_LIMIT_EXCEEDED`; ghi chú/thẻ sai → `NOTE_NOT_FOUND` / `TAG_NOT_FOUND` |
| `note_link` | `noteId`, `targetType` (`TASK`/`GOAL`), `targetId` | Như `note_tag`; mục tiêu sai/đã xóa/của người khác → `TASK_NOT_FOUND` / `GOAL_NOT_FOUND`; ≤ 20/ghi chú. Xóa Task/Goal → server xóa mềm liên kết (`mutation_id` NULL) |
| `goal` | `title` (1–200), `description` (≤ 2000), `startOn`, `targetOn` (≥ `startOn`), `status` | `status` ở `UPDATE` phải là chuyển hợp lệ D188 (`REJECTED GOAL_INVALID_STATUS_TRANSITION`); `CREATE` vắng `status` = `ACTIVE`. `DELETE` cascade milestone, `note_link`, gỡ liên kết Task |
| `milestone` | `goalId` (bất biến), `title`, `dueOn`, `position` (vắng = max + 1000; trùng → `VALIDATION_FAILED`), `completed` (cờ; server đặt `completedAt`) | Goal sai → `REJECTED GOAL_NOT_FOUND`; ≤ 20/goal → `MILESTONE_LIMIT_EXCEEDED`. `DELETE` gỡ `milestoneId` của Task (giữ `goalId`) |

Thứ tự khóa (D86): Goal FOR UPDATE → milestone → Task; Note FOR UPDATE → Tag/Task/Goal FOR SHARE. Cascade nhiều module đi qua `PendingChanges` (`TaskDeletionStarted`, `GoalDeletionStarted`).

### 3.9. Phase 7 — `review` `[CONFIRMED]` (PHASE_7 §7, D202; triển khai WP7 2026-11-07)

| `entityType` | Khóa payload | Ghi chú |
|---|---|---|
| `review` | `periodType` (`DAY`/`WEEK`/`MONTH`), `periodKey` (ngày bắt đầu kỳ theo D192: tuần theo `weekStartDay`, tháng là ngày 1) — cả hai bất biến; `reflection` (≤ 5000 code point, NFC), `mood` (1–5 hoặc null) | `CREATE` trùng kỳ còn sống → `CONFLICT` `conflictReason: ALREADY_EXISTS` kèm bản đang có (REST: 409 `REVIEW_EXISTS`); lệch `baseVersion` → `CONFLICT` văn bản (client hiển thị hai bản như `note`). Số liệu review/dashboard/analytics **không** đồng bộ (tính khi đọc, D190) |

---

## 4. Ma trận kết quả `[CONFIRMED]` D81, D84

Trạng thái entity xét theo `(userId từ token, entityId)`. "Của user khác" và "không tồn tại" là **một** trạng thái (không rò thông tin, IDOR-01).

### 4.1. `CREATE` (`baseVersion` phải là 0; khác 0 → `REJECTED VALIDATION_FAILED` fieldError `baseVersion`)

| Trạng thái `entityId` | Kết quả |
|---|---|
| Chưa dùng bao giờ | Validate → `APPLIED`, `entityVersion = 1`, `record(CREATE)` |
| Đã dùng (còn sống, tombstone, của user nào cũng vậy) | `REJECTED ENTITY_ALREADY_EXISTS` — **cùng** thân kết quả cho mọi trường hợp (D36); id không bao giờ được dùng lại |

**Khóa tự nhiên (§5 #9 BACKEND_MASTER_PLAN, PHASE_2 D235):** với `habit_checkin` (habit + ngày), `routine_log` (routine + ngày) và `routine_step_log` (lượt + bước), `CREATE` khi đã có bản ghi **hoạt động** cùng khóa tự nhiên (id khác) → `CONFLICT` với `conflictReason: "ALREADY_EXISTS"` và `serverSnapshot` là **bản đang tồn tại**; không ghi gì. Client chấp nhận bản server (D235). `conflictReason` chỉ xuất hiện trong trường hợp này.

Gửi lại **cùng `mutationId`** → `DUPLICATE` (idempotency chặn trước khi đến bước này). `CREATE` với `mutationId` mới nhưng id đã tạo ở lần trước → `ENTITY_ALREADY_EXISTS`; cách client xử lý (coi như đã tạo, rồi pull) thuộc `FLUTTER_RULES.md`.

### 4.2. `UPDATE`

| Trạng thái | `baseVersion` | Kết quả |
|---|---|---|
| Không tồn tại / của user khác | bất kỳ | `REJECTED <TYPE>_NOT_FOUND` |
| Còn sống | `= version` | Validate → `APPLIED`, `version+1`, `record(UPDATE)`. Nếu dữ liệu sau áp dụng giống hệt hiện tại → `APPLIED`, không tăng version, không `change_logs` (§4.4) |
| Còn sống | `≠ version` (cũ hoặc lớn hơn) | `CONFLICT` + `serverSnapshot` hiện tại; không ghi gì vào entity |
| Tombstone (của chính user) | bất kỳ | `CONFLICT` + `serverSnapshot {deleted:true, version}`; client giữ bản local (SYNC_RULES §9.7) |

Lỗi validation hoặc tham chiếu **sau** khi version khớp → `REJECTED` (không `CONFLICT`). Thứ tự kiểm: tồn tại → version → validation/tham chiếu → áp dụng.

### 4.3. `DELETE`

| Trạng thái | `baseVersion` | Kết quả |
|---|---|---|
| Không tồn tại / của user khác | bất kỳ | `REJECTED <TYPE>_NOT_FOUND` |
| Còn sống | `= version` | Soft delete (+ cascade nếu Project/Category), `version+1`, `record(DELETE)` → `APPLIED` |
| Còn sống | `≠ version` | `CONFLICT` + snapshot |
| Tombstone (của chính user) | bất kỳ | `APPLIED`, `entityVersion` = version của tombstone, **không** cascade, **không** `change_logs` (đích mong muốn đã đạt; tránh kẹt hàng đợi khi hai thiết bị cùng xóa) |

(Khác với REST: `DELETE` bản đã xóa → 404, D78. REST không có `mutationId` nên không có khái niệm gửi lại an toàn; sync thì có, và client phải thoát khỏi hàng đợi.)

### 4.4. Thao tác không đổi gì

Nhất quán với D78: `UPDATE` mà dữ liệu cuối cùng không đổi (kể cả `archived` đã đúng giá trị, `status` đã đúng) vẫn kiểm `baseVersion` trước; khớp thì `APPLIED`, **giữ nguyên version**, **không** ghi `change_logs`. Không khớp thì `CONFLICT` như thường.

---

## 5. Thuật toán xử lý `POST /sync/push`

```text
validate cấu trúc toàn request (§2.1)            → 400/413 nếu hỏng
for m in mutations (đúng thứ tự mảng):
    r = store.find(userId, m.mutationId)
    if r exists: results += DUPLICATE(r); continue            # kể cả trùng trong cùng lô
    thứ tự kiểm (D93): entityType lạ → payload > 64 KB → thao tác không được phép (§3.4) → CREATE có baseVersion ≠ 0 → handler
    if m.entityType không hỗ trợ → rejected(SYNC_UNKNOWN_ENTITY_TYPE)    → store trong T2
    if payload > 64 KB → rejected(PAYLOAD_TOO_LARGE)                      → store trong T2
    outcome = tx T1 (TransactionTemplate):
        handler.apply(userId, m)       # gọi *CommandService; có thể ném ConflictException/BusinessException
        nếu Applied: store.insert(APPLIED, result) trong CHÍNH T1; commit
        nếu Conflict/Rejected: rollback T1
    nếu Conflict/Rejected: tx T2 mới: store.insert(...) ON CONFLICT DO NOTHING
         nếu 0 dòng được chèn (request khác cùng mutationId thắng) → đọc lại → DUPLICATE
    nếu insert APPLIED trong T1 vi phạm PK (request song song cùng mutationId): T1 rollback → đọc lại → DUPLICATE
    results += outcome
```

### 5.1. Phân loại lỗi `[CONFIRMED]` D85

| Loại | Ví dụ | Xử lý |
|---|---|---|
| Kết quả nghiệp vụ dự kiến | version lệch, validation, tham chiếu sai, trùng tên, id đã tồn tại, không tìm thấy | Kết quả từng mutation (`CONFLICT`/`REJECTED`), **được lưu**, request vẫn `200` |
| Tranh chấp khóa / deadlock | `lock_timeout` hết hạn (SQLSTATE `55P03`), deadlock (`40P01`) | **Dừng cả request**, trả `503` + `Retry-After: 2`. Các mutation đã `APPLIED` trong lô vẫn giữ (đã commit); client gửi lại **cả lô**, chúng trả `DUPLICATE`. **Không** lưu kết quả cho mutation đang xử lý |
| Lỗi không dự kiến | NPE, lỗi SQL khác, lỗi hạ tầng | **Dừng cả request**, `500` (`INTERNAL_ERROR`, không lộ chi tiết), **không** lưu kết quả; ghi log lỗi có `correlationId`. Không biến thành `REJECTED` (lỗi của server không được ghi nhận như lỗi của dữ liệu client) |

### 5.2. Transaction và khóa `[CONFIRMED]` D86, D87

- Mỗi mutation đúng một T1 (`TransactionTemplate`, ở `sync.service`; `@Transactional` chỉ ở `..service..` theo A10). T1 bao cả `handler.apply`, `ChangeRecorder.record` và `store.insert(APPLIED)` để ba thứ cùng commit hoặc cùng rollback.
- **Bất biến thứ tự khóa (chống deadlock):** trong mọi giao dịch ghi (REST lẫn sync) phải theo đúng thứ tự: (1) lấy khóa dòng nghiệp vụ và **tải hết** dữ liệu cần sửa (`PESSIMISTIC_*`/`FOR SHARE`), Project trước Category trước Task; (2) sửa trong bộ nhớ; (3) `flush` tường minh để mọi `UPDATE`/`INSERT` nghiệp vụ chạy xong; (4) **cuối cùng** mới gọi `ChangeRecorder.record(...)` (lần gọi đầu tiên chiếm khóa dòng `user_sync_state` và giữ đến commit). Hệ quả: khóa `user_sync_state` luôn là khóa **cuối** trong mọi giao dịch nên không thể tạo vòng chờ. Cấm gọi truy vấn JPQL/Criteria có thể kích hoạt auto-flush **giữa** các lần `record` ở cascade; đọc hết trước, ghi sau.
- `lock_timeout` = **5 giây**, đặt ở mức kết nối bằng tham số JDBC `options=-c lock_timeout=5000` (cấu hình `lifeos.db.lock-timeout`, mặc định 5s) để áp dụng thống nhất cho REST và sync. `GlobalExceptionHandler` ánh xạ lỗi khóa hết hạn → `503` + `Retry-After` cho cả hai đường (D42). Migration Flyway cũng bị ảnh hưởng nhưng chỉ chạy lúc khởi động.
- Hai thiết bị cùng user push đồng thời xếp hàng trên `user_sync_state`; khác user không chặn nhau (SYN-11/12).
- Giữ T1 ngắn: không gọi hệ thống ngoài, không tải dữ liệu lớn.

### 5.3. Giới hạn xử lý

Tối đa 50 mutation/lô, 64 KB/payload, 1 MB/body. Thời gian xử lý trung bình/mutation phải đủ nhỏ để 50 mutation hoàn tất trong timeout HTTP của client (đo ở báo cáo; không đặt `statement_timeout` riêng ở Phase 1).

---

## 6. `GET /sync/pull`

Thuật toán gốc: `SYSTEM_ARCHITECTURE.md` §3.7. Bổ sung `[CONFIRMED]` D89:

1. **Giao dịch chỉ đọc**, không khóa `user_sync_state`, không gọi `ChangeRecorder`.
2. `limit`: vắng = `lifeos.sync.pull-page-size` (200); ngoài `[1, 500]` → `400 VALIDATION_FAILED` (fieldError `limit`).
3. `cursor`: vắng hoặc rỗng = `0`. Cursor là base64url của chuỗi số thập phân `seq`. Không giải mã được, âm, hoặc không phải số → `400 VALIDATION_FAILED` (fieldError `cursor`, mã `INVALID_VALUE`). Cursor **lớn hơn** `last_seq` của user (ví dụ DB bị khôi phục) → trả `changes=[]`, `hasMore=false`, `nextCursor` = chính cursor đó (full resync là Phase 5).
4. `nextCursor` = `seq` lớn nhất của các dòng `change_logs` **đã đọc trong trang** (kể cả dòng bị gom), không bao giờ vượt quá dòng đã đọc. Trang rỗng → `nextCursor` = cursor đầu vào.
5. Gom theo `(entity_type, entity_id)` giữ dòng có `seq` cao nhất; thứ tự `changes[]` theo `seq` của dòng đó. `loadSnapshots` gọi **một lần mỗi `entityType`** (truy vấn theo lô, điều kiện `user_id`, không N+1).
6. Tính nhất quán: snapshot được đọc sau `change_logs` ở READ COMMITTED nên có thể mới hơn `seq` của dòng; điều này an toàn vì thay đổi mới hơn sẽ xuất hiện ở trang sau với cùng trạng thái (client áp dụng idempotent theo `version`). Client bỏ qua `change` có `version` ≤ version local đã đồng bộ.
7. `operation = DELETE` khi entity có `deleted_at` hoặc không còn tìm thấy; kèm `version`. Entity chưa từng thấy ở client mà đã là tombstone vẫn được trả (client bỏ qua an toàn).
8. Dữ liệu của user khác **không bao giờ** xuất hiện: `change_logs` và `loadSnapshots` đều lọc `user_id` ở tầng repository (SYN-20).

### 6.0. Retention, full resync, snapshot (PHASE_5 D150–D174; BACKEND_MASTER_PLAN §5 #4; triển khai WP5) `[CONFIRMED]`

- `GET /sync/pull` với `cursor` < `min_retained_seq` → **410** `SYNC_FULL_RESYNC_REQUIRED` `{minRetainedSeq, currentSeq}`. Pull đọc `min_retained_seq` và trang trong cùng giao dịch REPEATABLE READ (retention chạy song song không làm trang thiếu dòng im lặng).
- `POST /sync/snapshot` `{snapshotToken?, next?, limit?}` → `{snapshotToken, snapshotSeq, entities:[{type, items:[serverSnapshot…]}], next}`. Trang đầu (không `snapshotToken`) ghi `snapshotSeq = last_seq` và cấp token ký HMAC gắn user (hạn 30 phút, 6 phiên/giờ/user); trang sau gửi lại token + `next` (`"entityType:id"`, null = hết). Mỗi trang ≤ 1000 bản ghi, ≈ ≤ 1 MB, chỉ bản ghi còn sống (tombstone không có), `data` như §6.1. Sau khi nạp xong: pull tiếp từ `cursor = snapshotSeq` (bản ghi đổi sau snapshotSeq đến qua pull). Token hết hạn → **410** `SYNC_SNAPSHOT_EXPIRED` (tải lại từ đầu); token của user khác/giả → 400 `VALIDATION_FAILED`.
  - *Cập nhật 2026-11-15 theo BACKEND_FIX_REPORT (F4, F9):* trần 1 MB của một trang được kiểm **trước khi thêm từng bản ghi** và tính cả phần vỏ response; trang dừng ngay trước bản ghi làm vượt trần, `next` trỏ tới bản ghi cuối đã trả (không mất, không lặp). Trang luôn có ít nhất một bản ghi và có thể ít hơn `limit` rất nhiều khi bản ghi lớn (ghi chú) — client lặp tới khi `next = null`, không suy "hết dữ liệu" từ số bản ghi của trang. `snapshotToken` ký HMAC bằng secret `SNAPSHOT_HMAC_SECRET` dùng chung mọi instance: còn hiệu lực qua khởi động lại/rolling deploy trong hạn 30 phút.
- `GET /sync/entities/{type}/{id}` → `serverSnapshot` hiện tại (kể cả tombstone còn giữ); không có/của người khác → 404 `NOT_FOUND`; `type` lạ → 400 `SYNC_UNKNOWN_ENTITY_TYPE`; 120/phút/user.
- `POST /sync/diagnostics` (202): chỉ siêu dữ liệu `{appVersion, schemaVersion, cursor, mutationCounts, errors:[{code, entityType, at}] (≤ 50), integrity}`; khóa lạ → 400; ≤ 64 KB; 5/giờ/user; giữ 30 ngày.
- `sync_mutations.device_id` ghi thiết bị của request (chẩn đoán, D163).

### 6.1. `data` của snapshot (pull và `serverSnapshot`)

Cùng tập trường với `Response` của REST, nhưng **bỏ** `id` và `version` (đã nằm ngoài `data`):

| `entityType` | `data` |
|---|---|
| `task` | `title, description, status, priority, dueDate, dueTime, projectId, categoryId, goalId, milestoneId, completedAt, createdAt, updatedAt` (`goalId`, `milestoneId` thêm ở WP6, §5 #2) |
| `project` | `name, description, archivedAt, createdAt, updatedAt` |
| `category` | `name, createdAt, updatedAt` |
| `user_settings` | `timezone, locale, defaultCurrency, weekStartDay, quietStart, quietEnd, showDetailsOnLockScreen, updatedAt` (3 khóa thông báo thêm ở WP4, §5 #1) |
| `habit` | `name, description, color, icon, scheduleType, weekdays, timesPerWeek, intervalDays, startOn, endOn, reminderTime, archivedAt, createdAt, updatedAt` |
| `habit_checkin` | `habitId, localDate, status, note, createdAt, updatedAt` |
| `routine` | `name, kind, weekdays, reminderTime, archivedAt, createdAt, updatedAt` |
| `routine_step` | `routineId, title, position, durationMinutes, createdAt, updatedAt` |
| `routine_log` | `routineId, localDate, totalSteps, startedAt, completedAt, createdAt, updatedAt` |
| `routine_step_log` | `routineLogId, stepId, status, createdAt, updatedAt` |

Chuỗi/thống kê habit (`currentStreak`, `longestStreak`, D233–D234) là giá trị tính khi đọc REST, **không** nằm trong `data`.

| `entityType` (Phase 3) | Khóa của `data` |
|---|---|
| `financial_account` | `name, accountType, currency, openingBalance, archivedAt, createdAt, updatedAt` |
| `financial_category` | `name, kind, systemKey, archivedAt, createdAt, updatedAt` |
| `financial_transaction` | `accountId, categoryId, kind, amount, currency, occurredOn, note, recurringTransactionId, recurrenceDate, createdAt, updatedAt` |
| `account_transfer` | `fromAccountId, toAccountId, amount, currency, occurredOn, note, createdAt, updatedAt` |
| `budget` | `categoryId, currency, amount, archivedAt, createdAt, updatedAt` |
| `recurring_transaction` | `accountId, categoryId, kind, amount, currency, note, frequency, intervalCount, startOn, endOn, nextRunOn, status, createdAt, updatedAt` |
| `saving_goal` | `name, currency, targetAmount, targetOn, status, createdAt, updatedAt` |
| `saving_contribution` | `goalId, amount, occurredOn, note, createdAt, updatedAt` |

Số dư (`balance`), mức dùng ngân sách và tiến độ mục tiêu là giá trị tính khi đọc REST, **không** nằm trong `data`.

| `entityType` (Phase 4) | Khóa của `data` |
|---|---|
| `calendar_event` | `title, description, location, allDay, timezone, startLocal, endLocal, startDate, endDate, startAt, endAt, recurrence, recurrenceUntil, createdAt, updatedAt` |
| `event_exception` | `eventId, originalStartLocal, kind, title, description, location, startLocal, endLocal, createdAt, updatedAt` |
| `time_block` | `taskId, startAt, endAt, note, createdAt, updatedAt` |
| `reminder` | `targetType, targetId, mode, offsetMinutes, localTime, localDate, repeatRule, urgent, status, nextFireAt, lastFiredAt, createdAt, updatedAt` |

Thông báo in-app, delivery, tùy chọn thông báo và token push **không** đi qua sync (PHASE_4 §7; REST riêng).

| `entityType` (Phase 6) | Khóa của `data` |
|---|---|
| `note` | `title, content, pinned, archivedAt, createdAt, updatedAt` |
| `tag` | `name, color, createdAt, updatedAt` |
| `note_tag` | `noteId, tagId, createdAt, updatedAt` |
| `note_link` | `noteId, targetType, targetId, createdAt, updatedAt` |
| `goal` | `title, description, startOn, targetOn, status, statusChangedAt, createdAt, updatedAt` |
| `milestone` | `goalId, title, dueOn, position, completedAt, createdAt, updatedAt` |

Tiến độ Goal (D185) là giá trị tính khi đọc REST, **không** nằm trong `data`.

| `entityType` (Phase 7) | Khóa của `data` |
|---|---|
| `review` | `periodType, periodKey, reflection, mood, createdAt, updatedAt` |

Lưu ý bất đối xứng có chủ đích: payload `project` dùng `archived` (boolean, client không gửi thời điểm); snapshot trả `archivedAt`. Phải có test hợp đồng khẳng định `data` của pull trùng tập trường `Response` REST (chống trôi lệch).

---

## 7. Bảo mật và quan sát

1. Mọi truy vấn có `user_id` lấy từ token; handler không nhận `userId` từ payload.
2. Không ghi `payload`, `title`, `description`, `name` vào log. Log sự kiện `sync_push` chỉ gồm: `userId`, `correlationId`, số mutation theo `status`, thời gian xử lý. Mở rộng `SensitiveLogIT` để quét luồng sync (LOG-01).
3. Chỉ số (Micrometer) `[CONFIRMED]` D91: `lifeos.sync.push.mutations{status}`, `lifeos.sync.push.duration`, `lifeos.sync.lock.timeouts`, `lifeos.sync.lock.wait` (thời gian chờ `user_sync_state`), `lifeos.sync.pull.duration`, `lifeos.sync.pull.changes`. Không gắn nhãn có `userId`.
4. Giới hạn tần suất: chung 600 yêu cầu/phút/user (đã có); không thêm bucket riêng cho sync ở Phase 1.
5. `/sync/*` luôn cần access token; ghi vào `AuthorizationMatrixIT` tự động (quét route).

---

## 8. Cấu hình

| Khóa | Mặc định | Ghi chú |
|---|---|---|
| `lifeos.sync.push-max-batch` | 50 | Đã có |
| `lifeos.sync.pull-page-size` | 200 | Đã có; trần cứng 500 không cấu hình được |
| `lifeos.sync.payload-max-bytes` | 65536 | Giới hạn chung; `note` cố định 196608 (192 KB, D181) trong `NoteSyncHandler` |
| `lifeos.db.lock-timeout` | `5s` | Dùng cho `options=-c lock_timeout=…` của JDBC |

Retention của `sync_mutations`, `change_logs`, tombstone: **chưa làm** ở Giai đoạn 3 (`[OPEN]` đến Phase 5, D38). Không thêm job dọn.

---

## 9. Test bổ sung (SYN-22..SYN-45) `[CONFIRMED]` D92

Bổ sung cho SYN-01..21 (`TESTING_RULES.md` §6). Chạy trên PostgreSQL thật; mỗi test dùng user/UUID riêng; không `Thread.sleep`; test đồng thời có timeout cứng và lặp ≥ 20 lần.

| ID | Kịch bản | Kỳ vọng |
|---|---|---|
| SYN-22 | `CREATE` với id đã tồn tại (còn sống / tombstone / của user khác) | `REJECTED ENTITY_ALREADY_EXISTS`, thân kết quả giống hệt nhau cả ba trường hợp |
| SYN-23 | `UPDATE` / `DELETE` entity không tồn tại hoặc của user khác | `REJECTED <TYPE>_NOT_FOUND` giống hệt nhau; dữ liệu nạn nhân không đổi |
| SYN-24 | `UPDATE` tombstone của chính user | `CONFLICT`, `serverSnapshot.deleted = true` |
| SYN-25 | `DELETE` tombstone của chính user | `APPLIED`, version không đổi, không thêm `change_logs`, không cascade |
| SYN-26 | `UPDATE` không đổi dữ liệu, `baseVersion` đúng | `APPLIED`, version không đổi, không thêm `change_logs` |
| SYN-27 | `UPDATE` khóa vắng mặt vs `null` tường minh | Vắng = giữ nguyên; `null` = xóa (trường nullable); `null` cho `title` → `REJECTED VALIDATION_FAILED` |
| SYN-28 | `UPDATE` với `baseVersion` lớn hơn version server | `CONFLICT` |
| SYN-29 | Phát lại mutation `CONFLICT` và `REJECTED` | `DUPLICATE` với `originalStatus` đúng, cùng `serverSnapshot`/`errorCode` như lần đầu, dù entity đã đổi |
| SYN-30 | Cùng `mutationId` nhưng `entityId`/`operation` khác | `DUPLICATE` của bản đã lưu; có log `sync_mutation_id_reuse`; không áp dụng thêm gì |
| SYN-31 | Hai mutation cùng `mutationId` trong một lô | Mutation thứ hai `DUPLICATE`, chỉ một lần áp dụng |
| SYN-32 | Hai request song song cùng `mutationId` (20 lần) | Đúng một `APPLIED`, một `DUPLICATE`; đúng một `change_logs` |
| SYN-33 | `user_settings`: `UPDATE` hợp lệ; `CREATE`; `DELETE`; `entityId` sai | `APPLIED` v+1 có `change_logs`; `SYNC_OPERATION_NOT_ALLOWED`; `SYNC_OPERATION_NOT_ALLOWED`; `USER_SETTINGS_NOT_FOUND` |
| SYN-34 | Validation giống REST: cùng đầu vào sai gửi qua REST và sync | Cùng `errorCode` và `fieldErrors` |
| SYN-35 | `DELETE project` qua sync có Task | Cascade như REST; `mutation_id` chỉ ở dòng `change_logs` DELETE của Project; các dòng UPDATE của Task có `mutation_id = NULL`; `seq` Task UPDATE < Project DELETE |
| SYN-36 | `entityType`/`operation`/`mutationId` hỏng cấu trúc | 400 toàn request, không mutation nào được lưu (khác `entityType` lạ → `REJECTED`) |
| SYN-37 | Lô rỗng; lô 51 | 400 `VALIDATION_FAILED`; 400 `SYNC_BATCH_TOO_LARGE` |
| SYN-38 | Body > 1 MB | 413 `PAYLOAD_TOO_LARGE`, không mutation nào được xử lý |
| SYN-39 | Pull: cursor hỏng / âm / không phải số; `limit` 0, 501 | 400 `VALIDATION_FAILED` với fieldError đúng |
| SYN-40 | Pull: cursor lớn hơn `last_seq` | 200, rỗng, `nextCursor` = cursor đầu vào |
| SYN-41 | Pull: CREATE rồi DELETE cùng entity trong một trang | Chỉ một `change` DELETE (tombstone) cho entity đó |
| SYN-42 | Pull: dữ liệu `data` so với `Response` REST | Cùng tập trường (test hợp đồng) cho cả 4 `entityType` |
| SYN-43 | Pull không giữ khóa `user_sync_state` | Pull chạy được trong khi một giao dịch ghi của cùng user đang giữ khóa |
| SYN-44 | Deadlock-free: cascade xóa Project song song với (a) sửa Task thuộc Project, (b) tạo Task trỏ tới Project, (c) sửa Category của Task (20 lần mỗi kịch bản) | Không có `40P01`; kết quả hợp lệ (mỗi mutation một trạng thái hợp lệ); không Task mồ côi; `seq` liên tục |
| SYN-45 | Khóa hết hạn giữa lô (giữ khóa `user_sync_state` bằng kết nối riêng) | `503` + `Retry-After`; mutation đã `APPLIED` trước đó được giữ; gửi lại cả lô → mutation cũ `DUPLICATE`, mutation còn lại `APPLIED`; đo `lifeos.sync.lock.timeouts` tăng |
| SYN-46 | Lỗi không dự kiến trong handler (inject lỗi) | `500`, **không** có dòng `sync_mutations` cho mutation đó; gửi lại sau khi hết lỗi → `APPLIED` |
| SYN-47 | Quét log luồng sync | Không có `title/description/name/payload` hay token trong log chữ và JSON |
| SYN-48 | Thứ tự khóa (kiểm tĩnh + động) | Test/ArchUnit/đọc mã chứng minh không có đường ghi nào gọi `ChangeRecorder.record` rồi mới khóa/flush dòng nghiệp vụ |

---

**Ghi chú SYN-17/45 (D94):** hai test này chạy trong context riêng với `lifeos.db.lock-timeout=1s` để mỗi lần chạy ngắn; điều được kiểm là ánh xạ 503 và gửi lại an toàn, không phải hằng số 5 giây. Một test cấu hình riêng khẳng định giá trị mặc định ở context chính là 5 giây (`SHOW lock_timeout` trên kết nối thật). Ánh xạ 503 phải được test cho cả lỗi khóa ở `SELECT … FOR UPDATE/SHARE` lẫn ở lúc flush. SYN-42 phải so `data` với danh sách trường ở §6.1 (không chỉ so với `Response` REST). Mapper phải phân biệt `NotFound` của entity đích với `*_NOT_FOUND` dạng 422 của tham chiếu (có test: UPDATE Task trỏ Project đã xóa → `REJECTED PROJECT_NOT_FOUND`, không bị hiểu là entity đích không tồn tại).

## 10. Quyết định (đã duyệt 2026-11-06)

| ID | Quyết định | Mục |
|---|---|---|
| D81 | Ma trận kết quả theo (thao tác × trạng thái entity); `CREATE` id đã dùng → `ENTITY_ALREADY_EXISTS` giống hệt; thứ tự kiểm: tồn tại → version → validation/tham chiếu | §4 |
| D82 | Ngữ nghĩa payload: `UPDATE` khóa vắng mặt = giữ nguyên, `null` = xóa; trường server-quản bị bỏ qua; validation ở service | §3 |
| D83 | Hình dạng kết quả; `DUPLICATE` phát lại nguyên văn kết quả đã lưu + `originalStatus`; `mutationId` tái dùng khác metadata vẫn `DUPLICATE` + log | §2.2 |
| D84 | `DELETE` tombstone → `APPLIED` không đổi gì; `UPDATE` tombstone → `CONFLICT` kèm `deleted=true` | §4 |
| D85 | Phân loại lỗi: nghiệp vụ → kết quả từng mutation; khóa/deadlock → 503 cả request; lỗi lạ → 500 cả request, không lưu kết quả | §5.1 |
| D86 | Bất biến thứ tự khóa: khóa & tải & flush nghiệp vụ trước, `ChangeRecorder.record` cuối cùng | §5.2 |
| D87 | `lock_timeout` 5s ở mức kết nối JDBC, ánh xạ 503 cho REST và sync | §5.2 |
| D88 | `user_settings`: chỉ `UPDATE`; `entityId` = `user_settings.id`; mã `SYNC_OPERATION_NOT_ALLOWED`, `USER_SETTINGS_NOT_FOUND` | §3.4 |
| D89 | Chi tiết pull: giao dịch chỉ đọc, kiểm tra `cursor`/`limit`, cursor vượt `last_seq`, `nextCursor` không vượt dòng đã đọc, `data` = `Response` REST | §6 |
| D90 | Kiểm tra cấu trúc toàn request; lô rỗng 400; body 1 MB → client chia lô | §2.1 |
| D91 | Chỉ số Micrometer và log sự kiện không chứa dữ liệu người dùng | §7 |
| D92 | Bộ test bổ sung SYN-22..48 | §9 |
| D93 | Chi tiết cấu trúc: `entityType` > 30 ký tự và `payload` không phải object → 400; vị trí lỗi trong `fieldErrors`; `payload` vắng mặt = `{}`; trường sai kiểu trong `payload` → `REJECTED VALIDATION_FAILED` riêng mutation; thứ tự kiểm từng mutation | §2.1, §5 |
| D94 | SYN-17/45 chạy với `lock-timeout=1s` trong context riêng; test cấu hình khẳng định mặc định 5s | §9 |
| D95 | Luật ArchUnit A13: `..{task,project,identity}.sync..` chỉ phụ thuộc `common..`, `..service..` và `..dto..` cùng module, Spring, Jackson | `SYSTEM_ARCHITECTURE.md` §3.9 |
| D96 | Thứ tự kiểm ở `*CommandService` (REST và sync): tồn tại → version → validation giá trị → tham chiếu → áp dụng; `CREATE`: id đã dùng → 409 trước validation. Hệ quả REST: PATCH id không tồn tại kèm giá trị sai → 404; PATCH sai version kèm giá trị sai → 409. Cascade luôn ghi `mutation_id = NULL`; mã Giai đoạn 2 vi phạm D86 (xóa Project/Category, đăng ký) phải sửa; metric `lifeos.sync.lock.wait` là thời gian chạy câu upsert seq (gồm chờ khóa) | §3, §5.2 |
