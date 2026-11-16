# Báo cáo Giai đoạn 3 backend — Sync push/pull

> **Trạng thái:** hoàn thành, **đã được người dùng duyệt ngày 2026-11-07**.
> **Phạm vi:** Giai đoạn 3 trong lộ trình dựng backend cho **Phase 1 sản phẩm** (Foundation & Task Management). Không phải "Phase 3 Finance" của `docs/phases/`.
> **Đặc tả áp dụng:** `docs/engineering/SYNC_PROTOCOL.md` (D81–D96), `SYNC_RULES.md` §9, `SYSTEM_ARCHITECTURE.md` §3.3–3.8, §4, §6; prompt giao việc `docs/review/CLAUDE_CODE_PHASE3_PROMPT.md`.
> **Kết quả cuối:** `mvn -f backend/pom.xml clean verify` xanh: **83 unit + 410 IT = 493 test**, 0 bỏ qua, 0 `@Disabled`; 3 phút 17 giây (cần Docker).

---

## 1. Tóm tắt

Backend có đồng bộ offline-first cho 4 `entityType` (`task`, `project`, `category`, `user_settings`):

| Endpoint | Chức năng |
|---|---|
| `POST /api/v1/sync/push` | Lô ≤ 50 mutation. Mỗi mutation một transaction; kết quả `APPLIED` / `CONFLICT` (kèm `serverSnapshot`) / `REJECTED` (kèm `errorCode`, `fieldErrors`) / `DUPLICATE` (phát lại nguyên văn kết quả đã lưu + `originalStatus`). Lỗi cấu trúc → 400 cả request; hết khóa/deadlock → 503 + `Retry-After: 2`; lỗi không dự kiến → 500, không lưu kết quả |
| `GET /api/v1/sync/pull?cursor=&limit=` | Pull theo state: đọc `change_logs` theo `seq` của user, gom theo entity, snapshot theo lô; `UPSERT` kèm `data` hoặc `DELETE` (tombstone). Giao dịch chỉ đọc, không khóa `user_sync_state` |

Mọi bước 3a–3e đều theo quy trình kế hoạch → duyệt → code → `mvn clean verify` → báo cáo → duyệt.

| Bước | Nội dung | Test (sau bước) | `verify` |
|---|---|---|---|
| 0 | Đọc tài liệu, kiểm thứ tự khóa D86 trên mã Giai đoạn 1–2, nộp kế hoạch | 81 + 213 | 2:29 |
| 3a | Hạ tầng push, handler Project + Category, sửa mã Giai đoạn 2 (D86/D96/Q2/Q4), `lock_timeout` + 503, A13, guard D86 | 83 + 256 | 2:41 |
| 3b | Handler Task + UserSettings, ma trận §4 đủ 4 entity, `updatedAt` cho settings (quyết định A) | 83 + 273 | 2:40 |
| 3c | Pull, cursor, metric pull, test hợp đồng `data` (SYN-42) | 83 + 284 | 2:32 |
| 3d | Cascade qua sync, test đồng thời lặp 20 lần | 83 + 408 | 2:33 |
| 3e | SYN-46/47, che log thân request sync, `@ApiResponse` cho OpenAPI | 83 + 410 | 3:17 |

---

## 2. Quyết định đã duyệt và áp dụng

| Nguồn | Nội dung chính |
|---|---|
| D81–D92 | Ma trận kết quả, ngữ nghĩa payload, hình dạng kết quả/DUPLICATE, tombstone, phân loại lỗi, thứ tự khóa, `lock_timeout` 5s, `user_settings` chỉ UPDATE, pull, kiểm cấu trúc, metric, SYN-22..48 |
| D93 | `entityType` > 30 ký tự và `payload` không phải object → 400; vị trí lỗi trong `fieldErrors`; trường sai kiểu → `REJECTED` riêng mutation; thứ tự kiểm từng mutation |
| D94 | SYN-17/45 chạy trong context riêng với `lock-timeout=1s`; test khẳng định mặc định 5s |
| D95 | Luật ArchUnit A13 |
| D96 | Thứ tự kiểm ở `*CommandService`: tồn tại → version → giá trị → tham chiếu → áp dụng (REST lẫn sync) |
| Q1–Q16 | `MutationOutcome` sealed; `mutationId` qua tham số tường minh, cascade luôn `NULL`; `archived` trong lệnh update Project; tombstone nhận diện qua `loadSnapshots`; … |
| Quyết định A (3b) | Thêm `updatedAt` vào `SettingsResponse` để `data` của `user_settings` = Response REST bỏ `id`/`version` = danh sách §6.1 |

---

## 3. Thay đổi mã

### 3.1. Module mới / thành phần mới
- `sync.controller`: `SyncController`, `SyncPushRequestParser`.
- `sync.service`: `SyncPushService`, `SyncPullService`, `SyncHandlerRegistry`, `SyncCursorCodec`, `SyncMetrics`, `PushMutation`.
- `sync.repository`: `MutationResultStore`, `ChangeLogReader` (JdbcClient).
- `sync.dto`: `PushRequest` (toString che payload), `PushResponse`, `MutationResult`, `ServerSnapshot`, `PullResponse`, `PullChange`.
- Handler: `project.sync.ProjectSyncHandler`, `task.sync.TaskSyncHandler`, `task.sync.CategorySyncHandler`, `identity.sync.UserSettingsSyncHandler`.
- `common`: `MutationOutcome` (sealed `Applied`/`Conflict`/`Rejected`), `SyncEntityHandler.supports(op)`, `LockFailures`, `PayloadFields`, `SnapshotData`; `Patch` thành class có trạng thái `invalid`; `ErrorCode` thêm `SYNC_OPERATION_NOT_ALLOWED`, `USER_SETTINGS_NOT_FOUND`.
- Cấu hình: `lifeos.db.lock-timeout` (5s, qua `options=-c lock_timeout=…` của JDBC), `lifeos.sync.payload-max-bytes` (65536); springdoc `default-produces/consumes-media-type`.

### 3.2. Sửa mã Giai đoạn 1–2
| Thay đổi | Lý do |
|---|---|
| Xóa Project/Category: xóa mềm + flush **trước** cascade; `record` DELETE sau cùng | D86 (khóa/ghi nghiệp vụ trước `ChangeRecorder.record`) |
| Đăng ký: ghi token xác minh + outbox **trước** `createDefault` | D86 |
| Thứ tự kiểm D96 trong Project/Category/Task service | D96 (hệ quả REST: PATCH id không tồn tại + giá trị sai → 404; sai version + giá trị sai → 409) |
| Overload nhận `mutationIdOrNull`; `delete` trả version tombstone | Q2 |
| Bỏ `mutationIdOrNull` khỏi `ProjectDeletionStarted` và `detachFrom*` | Q2, SYNC_PROTOCOL §3.2 (cascade `mutation_id = NULL`) |
| `ProjectCommands.Update` thêm `archived`; các lệnh `Create` nhận `Patch` | Q4; giữ thứ tự D96 cho trường sai kiểu |
| `SettingsResponse` thêm `updatedAt` | Quyết định A |
| `GlobalExceptionHandler`: 55P03/40P01 → 503 + `Retry-After: 2` | D42, D85, D87 |
| `@ApiResponse` (201/202/204) cho 14 method; lỗi RFC 9457 `default`; endpoint công khai `security: []` | Hoàn thiện OpenAPI (hoãn từ Giai đoạn 2) |

Không thêm migration (V1–V4 nguyên vẹn), không thêm thư viện.

---

## 4. Kiểm thử

### 4.1. Bản đồ SYN-01..48 (mỗi ID có ít nhất một test tên `Sync_NN_…`)

| Lớp test | SYN |
|---|---|
| `SyncPushStructureIT` | 08, 36, 37, 38 |
| `SyncPushProjectCategoryIT` | 01, 02, 03, 04, 06, 07, 09, 18, 19, 22–31 |
| `SyncPushTaskIT` | 01, 03, 04, 15, 18, 24–28, 34 |
| `SyncSettingsIT` | 33, 34 |
| `SyncPullIT` | 05, 10, 16, 20, 39, 40, 41, 43 |
| `SyncSnapshotContractIT` | 42 |
| `SyncCascadeIT` | 13, 14, 21, 35 |
| `SyncConcurrencyIT` (`@RepeatedTest(20)`) | 11, 12, 32, 44a/b/c |
| `LockTimeoutIT` (context `lock-timeout=1s`) + `LockTimeoutDefaultIT` | 17, 45 |
| `SyncFailureIT` (trigger PostgreSQL inject lỗi) | 46 |
| `SensitiveLogIT` | 47 |
| `LockOrderGuardIT` + `LockOrderGuard` (bọc DataSource của mọi IT) | 48 |

### 4.2. Test khác thêm trong giai đoạn
`CommandOrderIT` (hệ quả D96 ở REST), `ArchitectureRulesSelfTest` (A13), assertion OpenAPI trong `OpenApiExportIT`, AuthorizationMatrixIT tự quét `/sync/push`, `/sync/pull`.

### 4.3. Test cũ đã sửa (đều theo quyết định đã duyệt)
- `ProjectIT`: assertion `ProjectDeletionStarted` (Q2).
- `SettingsIT`: tập khóa có `updatedAt` (quyết định A).
- `SensitiveLogIT`: tách thành 2 method (auth và `Sync_47`), nội dung kiểm giữ nguyên.

### 4.4. Độ ổn định
`SyncConcurrencyIT` chạy 3 lần liên tiếp (363 lượt lặp): không đỏ, không 503. `LockTimeoutIT` xác định (kết nối đối thủ giữ khóa), không `Thread.sleep`.

### 4.5. Thời gian
IT chậm nhất: `ApplicationStartupIT` 39 s (gánh lần khởi động context đầu), `LoginRateLimitIT` 8,7 s, `LockTimeoutIT` 8,6 s, `SyncConcurrencyIT` 7,5 s (121 lượt). Tổng thời gian chạy các class IT khoảng 142 s. Chưa cần tách; nếu vượt 5 phút, đề xuất `@Tag("slow")` cho nhóm trên và chạy hằng đêm (cần sửa CI).

---

## 5. Lỗi và rủi ro phát hiện trong giai đoạn

| Phát hiện | Xử lý |
|---|---|
| Mã Giai đoạn 2 vi phạm câu chữ D86 ở 3 chỗ (không gây deadlock thật) | Sửa ở 3a; guard D86 chặn tái phạm |
| Cascade ghi `mutationId` vào dòng của Task bị cascade, trái §3.2 | Sửa ở 3a (Q2) |
| Mâu thuẫn §6.1 ↔ API_CONVENTIONS §8.2 về `updatedAt` của settings | Người dùng chọn A ở 3b |
| `@RequestBody JsonNode` khiến Spring MVC DEBUG in thân request sync (chỉ được che nhờ cắt 100 ký tự; TRACE sẽ lộ payload) | Sửa ở 3e bằng `PushRequest` có `toString` che; SYN-47 khẳng định |

---

## 6. Điểm cần người xem kỹ (đã nêu trong từng báo cáo bước)
1. `Patch` đổi từ record sang class, có trạng thái `invalid` (3a).
2. `SyncEntityHandler.supports(op)` là default method mới trong `common.sync`, ngoài phạm vi Q1 (3a).
3. `LockOrderGuard` chặn cả `SELECT … FOR UPDATE/SHARE` sau `record`, chặt hơn câu chữ D86.
4. SYN-44 chấp nhận mọi thứ tự xen kẽ hợp lệ; chưa đo phân bố thứ tự thực tế trong 20 lượt.
5. DELETE gặp tombstone → `APPLIED` được lưu bằng T2 (T1 đã rollback).

---

## 7. Việc còn lại
- ~~Tài liệu: API_CONVENTIONS §8.2 thêm `updatedAt` vào phản hồi `GET/PATCH /me/settings` (quyết định A).~~ Đã sửa 2026-11-07.
- **CI (người dùng):** chép `ci/backend-ci.yml` vào `.github/workflows/`; chưa chạy trên GitHub.
- **Giai đoạn 4:** outbox worker + gửi email (mailpit) — tiêu chí "Email xác minh đi qua outbox, worker gửi được sang mailpit" (SYSTEM_ARCHITECTURE §12).
- Retention `sync_mutations`/`change_logs`/tombstone: `[OPEN]` đến Phase 5 (D38), chưa làm theo đúng phạm vi.
- Chưa benchmark pull trang 500 change và push 50 mutation (SYNC_PROTOCOL §5.3).
