# Prompt giao việc cho Claude Code: Backend Giai đoạn 3 (Sync push/pull)

Dán phần dưới dòng `---` vào phiên Claude Code đang mở tại `D:\LifeOS`. **Điều kiện:** D81–D92 trong `docs/engineering/SYNC_PROTOCOL.md` đã được người dùng duyệt (nay là `[CONFIRMED]`). Giai đoạn 2 chưa có báo cáo duyệt chính thức; bước 0 dưới đây là bắt buộc.

---

Giai đoạn 1 (Identity) và Giai đoạn 2 (Project, Category, Task) đã xong. Bây giờ là **Giai đoạn 3: Sync push/pull**. Quy tắc làm việc không đổi so với `docs/review/CLAUDE_CODE_PHASE1_PROMPT.md` §1–§2, §5–§6 (chỉ áp dụng `[CONFIRMED]`; không sửa `docs/`, migration V1–V4; không thêm thư viện ngoài danh sách; mỗi bước: kế hoạch → duyệt → code → `mvn clean verify` → báo cáo → dừng).

## 0. Việc đầu tiên: xác nhận Giai đoạn 2

Chạy `mvn clean verify` ở `backend/` và gửi **báo cáo Giai đoạn 2 theo mẫu §5** (Phase 1 prompt): tổng số test (unit/IT), thời gian, ánh xạ SEC-01/08, IDOR-01..04, SYN-13/14 (phần REST) sang test cụ thể, kết quả EXPLAIN (Q20), cách viết lại A11 và kết quả `ArchitectureRulesSelfTest`, xác nhận `failOnEmptyShould=true`, `docs/api/openapi.yaml` và `docs/api/fixtures/` đã tạo, và mọi điểm lệch tài liệu. Chưa có báo cáo này thì chưa lập kế hoạch Giai đoạn 3.

## 0b. Đọc trước khi làm

Đọc **hết** trước khi lập kế hoạch:

1. `docs/engineering/SYNC_PROTOCOL.md` (đặc tả cài đặt: ma trận kết quả, payload, lỗi, thứ tự khóa, test SYN-22..48). Đây là tài liệu chính của giai đoạn này.
2. `docs/engineering/SYNC_RULES.md` §9; `docs/architecture/SYSTEM_ARCHITECTURE.md` §3.3–3.8, §4, §6 (bất biến I1–I9), D20–D22, D36, D42.
3. `docs/architecture/DATABASE_DESIGN.md` §8 và DDL `V2__sync_infrastructure.sql`.
4. `docs/engineering/SECURITY_RULES.md` §7–§9, §11; `API_CONVENTIONS.md` §5, §8.7; `TESTING_RULES.md` §6 (SYN-01..21 và MSY chỉ để biết hợp đồng với client).
5. Mã hiện có: `common.sync` (`ChangeRecorder`, `SyncEntityHandler`, `Mutation`, `MutationOutcome`, `EntitySnapshot`), `sync.service.ChangeRecorderImpl`, các `*CommandService` và `VersionedWrites` của Giai đoạn 2, `common.util.Patch`.

Mâu thuẫn giữa hai tài liệu, hoặc giữa tài liệu và mã: **dừng và hỏi**. Nếu `MutationOutcome`/`Mutation` hiện tại không đủ biểu diễn kết quả ở `SYNC_PROTOCOL.md` §2.2, nêu đề xuất thay đổi trong kế hoạch (đây là interface trong `common.sync`, thay đổi cần người duyệt).

## 1. Việc đầu tiên: kiểm tra bất biến thứ tự khóa (D86)

Trước khi viết handler, đọc toàn bộ đường ghi của Giai đoạn 1–2 và báo cáo: với mỗi `*CommandService`, `ChangeRecorder.record` có luôn được gọi **sau** mọi khóa/flush dòng nghiệp vụ không? Cascade xóa Project/Category có tải hết Task rồi mới sửa và `flush` tường minh trước khi `record` không? Chỗ nào vi phạm: nêu trong kế hoạch kèm cách sửa (sửa mã Giai đoạn 2 là hợp lệ, có test). Đây là rủi ro deadlock lớn nhất của giai đoạn.

## 2. Các bước đề xuất (kế hoạch có thể điều chỉnh, kèm lý do)

| Bước | Nội dung |
|---|---|
| 3a | Hạ tầng push: DTO + kiểm tra cấu trúc (§2.1), `MutationResultStore`, `SyncPushService` (định tuyến, `DUPLICATE`, T1/T2, phân loại lỗi §5.1), `lock_timeout` ở mức kết nối + ánh xạ 503 cho REST và sync, chỉ số Micrometer. Chưa cần handler thật: dùng handler `project` + `category` (đơn giản) để chạy test thật, **không** viết handler giả chỉ có trong test |
| 3b | `TaskSyncHandler`, `UserSettingsSyncHandler`; toàn bộ ma trận §4 cho 4 `entityType` |
| 3c | Pull (`SyncPullService`, `loadSnapshots`, `data` §6.1) và test hợp đồng `data` ↔ `Response` REST |
| 3d | Cascade qua sync, kiểm tra thứ tự khóa, test đồng thời (SYN-11/12/17/32/44/45), lặp ≥ 20 lần |
| 3e | Hardening: SYN-01..48 đủ bộ, quét log (SYN-47), `AuthorizationMatrixIT` cho `/sync/*`, đo thời gian `verify`, báo cáo cuối giai đoạn |

## 3. Ràng buộc kỹ thuật riêng giai đoạn này

- `@Transactional` chỉ ở `..service..`. T1/T2 dùng `TransactionTemplate` trong `sync.service`.
- Handler nằm ở package `..sync..` của module sở hữu entity; chỉ phụ thuộc `common.*`, `common.sync` và service cùng module. Module `sync` **không** import `task`/`project`/`identity`.
- Không có business rule trong handler; không gọi repository ghi trực tiếp.
- Mọi `ChangeRecorder.record` vẫn chỉ gọi từ `*CommandService` (A9). `mutationId` được truyền xuống service để ghi vào `change_logs` của entity chính.
- Không thêm migration trừ khi tìm ra lỗi thật; nếu cần, nêu lý do và đề xuất (V5) trong kế hoạch.
- Không thêm thư viện. Micrometer đã có qua Actuator.
- Không giảm tham số bảo mật/giới hạn để test chạy nhanh.

## 4. Nội dung kế hoạch cần nộp (chưa viết code)

1. Báo cáo kiểm tra thứ tự khóa (mục 1).
2. Danh sách class theo package, thay đổi cần có trong `common.sync` (nếu có), cấu hình mới.
3. Cách ánh xạ từ kết quả `*CommandService` (exception/outcome) sang `APPLIED/CONFLICT/REJECTED` cho từng `entityType`, kể cả cách lấy `serverSnapshot` (đọc trong transaction mới sau rollback) và cách xử lý `ObjectOptimisticLockingFailureException`.
4. Cách `ChangeRecorder` nhận `mutationId` mà vẫn tuân thủ A9.
5. Ánh xạ test: mỗi SYN-01..48 → lớp/phương thức dự kiến; ID nào chưa làm được và vì sao.
6. Mọi chỗ `SYNC_PROTOCOL.md` còn mơ hồ hoặc mâu thuẫn với mã hiện có, kèm khuyến nghị.

Bắt đầu bằng việc báo cáo bạn đã đọc những file nào, rồi nộp kế hoạch. **Chưa viết code trước khi tôi duyệt.**
