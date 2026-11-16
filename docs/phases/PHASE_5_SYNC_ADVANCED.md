# LifeOS — Phase 5: Offline Sync nâng cao (đặc tả)

> **Trạng thái:** v1 — **ĐÃ DUYỆT 2026-11-07** (người dùng duyệt `BACKEND_MASTER_PLAN.md`: toàn bộ quyết định D150–D179, quyết định mặc định ở §4 và thay đổi hợp đồng ở §5 của kế hoạch đó). Các mục `[OPEN]` trong tài liệu này được giải quyết bằng bảng §4 của `BACKEND_MASTER_PLAN.md`; khi mâu thuẫn, kế hoạch tổng thể thắng.
> **Nguồn đã chốt:** `SYNC_RULES.md`, `SYNC_PROTOCOL.md` (D81–D92 đã duyệt; D93–D96 chờ duyệt), `SYSTEM_ARCHITECTURE.md` §4, §3.12; `PRODUCT_CONTEXT.md` §12 Phase 5, §16 (retention `[OPEN]`).
> **Phụ thuộc:** backend giai đoạn 3 (sync cơ bản) hoàn tất. Các entity mới của Phase 3 (Finance) và Phase 4 (Calendar/Notification) đã đăng ký handler. Phase này **không đổi** giao thức push/pull cốt lõi; nó thêm giao diện xử lý xung đột, phục hồi, resync, retention và quan sát.
> **Ngoài phạm vi:** CRDT/hợp nhất theo văn bản thời gian thực, chia sẻ dữ liệu giữa nhiều người dùng, đồng bộ ngang hàng (P2P), mã hóa đầu-cuối (E2EE).

---

## 1. Mục tiêu và phạm vi

Giai đoạn 3 cho giao thức đúng đắn ở mức server. Phase 5 làm cho nó **dùng được khi có sự cố thật**: hai thiết bị sửa cùng dữ liệu, mất mạng nhiều ngày, cài lại app, máy mất/đổi, dữ liệu local hỏng, server dọn lịch sử. Nguyên tắc: **không mất dữ liệu người dùng một cách im lặng**; mọi quyết định mất mát phải do người dùng chọn hoặc có lưu vết để khôi phục.

Trong phạm vi: màn hình giải quyết xung đột (Task/Project/Category/Finance/Calendar/Reminder), quản lý thiết bị, đồng bộ nhiều thiết bị, phát hiện và phục hồi trạng thái local hỏng, **full resync**, xử lý `cursor` quá cũ do retention, dọn `change_logs`/`sync_mutations`/tombstone theo chính sách, tạm dừng/tiếp tục sync, quan sát và công cụ chẩn đoán cho người dùng và vận hành.

## 2. Yêu cầu chức năng

| ID | Yêu cầu | Tiêu chí chấp nhận |
|---|---|---|
| FR-SYN-01 | Màn hình xung đột | Mỗi `CONFLICT` hiện bản của tôi / bản máy chủ theo từng trường khác nhau; chọn "Giữ của tôi", "Lấy máy chủ", hoặc (với trường văn bản) sửa tay |
| FR-SYN-02 | Hàng đợi xung đột | Danh sách xung đột tồn đọng với số đếm huy hiệu; xử lý sau được; không chặn các mutation khác |
| FR-SYN-03 | Giải quyết xung đột an toàn | "Giữ của tôi" tạo mutation mới với `baseVersion` = phiên bản máy chủ hiện tại; nếu lại lệch → xung đột mới (không vòng lặp im lặng) |
| FR-SYN-04 | Mutation bị `REJECTED` | Hiện lý do theo mã lỗi; cho phép sửa và gửi lại, hoặc bỏ thay đổi local (cần xác nhận) |
| FR-SYN-05 | Danh sách thiết bị | Xem thiết bị đã đăng nhập (tên, lần đồng bộ cuối, nền tảng); đăng xuất từ xa một thiết bị |
| FR-SYN-06 | Cursor quá cũ | Server trả `FULL_RESYNC_REQUIRED`; client chạy full resync mà **không mất mutation chờ gửi** |
| FR-SYN-07 | Full resync | Tải lại toàn bộ dữ liệu của user, đối chiếu với local, giữ outbox chưa gửi, thay thế phần còn lại |
| FR-SYN-08 | Phát hiện local hỏng | Kiểm tra toàn vẹn khi mở app và khi sync; lỗi → đề nghị resync, không tự xóa |
| FR-SYN-09 | Đăng nhập thiết bị mới | Tải toàn bộ dữ liệu có thanh tiến độ, tiếp tục được khi bị ngắt |
| FR-SYN-10 | Tạm dừng sync | Người dùng tạm dừng; outbox vẫn tích lũy; hiện trạng thái rõ ràng |
| FR-SYN-11 | Nhật ký đồng bộ | Hiển thị lần cuối thành công, số mutation chờ, lỗi gần nhất; nút "Gửi báo cáo chẩn đoán" (không chứa nội dung dữ liệu) |
| FR-SYN-12 | Retention | Dọn `sync_mutations`, `change_logs`, tombstone theo chính sách §4; không phá tính đúng đắn |
| FR-SYN-13 | Xung đột Finance | Luôn do người dùng chọn (D105); hiển thị số tiền chính xác, không gộp |
| FR-SYN-14 | Hai thiết bị đồng thời | Không mất cập nhật; không trùng bản ghi; thứ tự sự kiện nhất quán |

## 3. Use case chính

| ID | Use case | Luồng và ngoại lệ |
|---|---|---|
| UC-SYN-01 | Sửa cùng Task trên điện thoại và máy tính bảng | Thiết bị B nhận `CONFLICT`; xung đột vào hàng đợi; B chọn từng trường; mutation giải quyết gửi với `baseVersion` mới |
| UC-SYN-02 | Bản local sửa, bản máy chủ đã xóa | `CONFLICT deleted`; chọn "Khôi phục từ máy tôi" (tạo lại với id mới `[OPEN]` D157) hoặc "Chấp nhận xóa" |
| UC-SYN-03 | Điện thoại tắt 3 tháng | `cursor` < `min_retained_seq` → `FULL_RESYNC_REQUIRED` → full resync giữ outbox |
| UC-SYN-04 | Cài lại app | Thiết bị mới đăng nhập, tải toàn bộ, không có outbox |
| UC-SYN-05 | Mất điện giữa chừng khi áp dụng một trang pull | Trạng thái local nhất quán: trang được áp dụng trong **một transaction SQLite**, `cursor` cập nhật cùng transaction |
| UC-SYN-06 | Mutation REJECTED do tham chiếu cha đã xóa | Hiển thị lỗi gộp; chọn bỏ hoặc gán lại cha |
| UC-SYN-07 | Mất thiết bị | Người dùng đăng xuất từ xa thiết bị đó từ thiết bị khác; họ refresh token bị thu hồi |
| UC-SYN-08 | Sao lưu hỏng phía client | Kiểm tra toàn vẹn thất bại → resync; mutation chờ gửi được xuất ra tệp khôi phục trước khi thay thế |
| UC-SYN-09 | Chạy dọn dẹp server | Job xóa dữ liệu quá hạn theo lô, không khóa dài, không đụng dữ liệu còn cần |

## 4. Quy tắc nghiệp vụ

### 4.1. Hợp đồng bổ sung với giao thức (không đổi push/pull cốt lõi)

1. `[CONFIRMED]` **D150 — `FULL_RESYNC_REQUIRED`:** `POST /sync/pull` với `cursor` < `min_retained_seq(user)` → `410 Gone` mã `SYNC_FULL_RESYNC_REQUIRED` kèm `{minRetainedSeq, currentSeq}`. `min_retained_seq` là cột mới ở `user_sync_state` (mặc định 0). Client **không** được dùng `cursor` cũ làm tiếp.
2. `[CONFIRMED]` **D151 — Cách chạy full resync (ở client):** (1) dừng push; (2) chụp outbox chưa gửi sang bảng tạm; (3) `POST /sync/snapshot` phân trang để tải **trạng thái hiện tại** của mọi entity (không phải lịch sử), kèm `snapshotSeq`; (4) trong **một transaction SQLite** xóa và nạp lại các bảng đồng bộ rồi đặt `cursor = snapshotSeq`; (5) áp lại outbox: mỗi mutation giữ `mutationId` cũ nhưng **`baseVersion` được tính lại** từ bản vừa tải nếu entity còn tồn tại; nếu entity không còn → mutation chuyển hàng đợi xung đột `deleted`; (6) tiếp tục pull từ `snapshotSeq`. Nếu bước (3) bị ngắt, local **chưa bị đụng** (nạp vào bảng tạm, hoán đổi nguyên tử ở bước 4). *(Cập nhật 2026-11-15 theo `docs/review/BACKEND_FIX_REPORT.md`: F9 — `snapshotToken` ký bằng secret `SNAPSHOT_HMAC_SECRET` dùng chung mọi instance, còn hiệu lực qua khởi động lại/rolling deploy trong hạn 30 phút)*
3. `[CONFIRMED]` **D152 — Snapshot nhất quán:** `POST /sync/snapshot` mở transaction `REPEATABLE READ READ ONLY`, ghi `snapshotSeq = user_sync_state.last_seq` đọc ở đầu transaction (không khóa ghi), trả dữ liệu từng entityType theo `cursor` nội bộ `(entityType, id)`; các trang sau dùng **cùng `snapshotSeq`** (mã `snapshotToken` ký, hết hạn 30 phút). Bản ghi thay đổi sau `snapshotSeq` sẽ đến qua pull bình thường (state-based pull đã idempotent). Tombstone **không** nằm trong snapshot (client đang thay thế toàn bộ), trừ khi cần cho mutation chờ (xem D151 bước 5, client hỏi `GET /sync/entities/{type}/{id}` khi cần).
4. `[CONFIRMED]` **D153 — Retention `change_logs`:** giữ **180 ngày** và tối thiểu `N=1000` seq gần nhất; job hằng ngày xóa theo lô 5000 dòng, cập nhật `min_retained_seq = (seq lớn nhất bị xóa)` trong **cùng transaction** với lần xóa cuối của user đó (khóa `user_sync_state`; thứ tự khóa theo D86). Thiết bị im lặng > 180 ngày sẽ full resync.
5. `[CONFIRMED]` **D154 — Retention `sync_mutations`:** giữ **35 ngày** (≥ giới hạn thiết bị offline tối đa dự kiến cho việc gửi lại mutation cũ). Mutation cũ hơn bị client từ chối gửi lại: client đặt hạn **30 ngày** cho mutation trong outbox; quá hạn → chuyển hàng đợi "cần xem lại" (không tự gửi, không tự xóa). Server gặp `mutationId` đã dọn sẽ xử lý như mutation mới (có thể tạo trùng nếu bản gốc đã áp dụng — nhưng bản gốc trả về id đã tồn tại → CREATE trả 409 → coi như `DUPLICATE` ở client khi entity đã tồn tại với đúng id; xem D155).
6. `[CONFIRMED]` **D155 — CREATE lặp sau khi dọn:** `CREATE` với `id` đã tồn tại **và cùng chủ** nhưng `mutationId` chưa biết → `REJECTED ENTITY_ALREADY_EXISTS`; client đối chiếu: nếu dữ liệu local trùng bản máy chủ (so sánh theo trường) thì coi như thành công và xóa mutation khỏi outbox; khác nhau → hàng đợi xung đột. (Xác nhận D96: CREATE đã tồn tại → 409/REJECTED trước validation.)
7. `[CONFIRMED]` **D156 — Retention tombstone:** tombstone (`deleted_at` NOT NULL) được **xóa cứng** khi `deleted_at` > **180 ngày** **và** `version`/`seq` của lần xóa < `min_retained_seq(user)` (không còn thiết bị hợp lệ nào cần biết đến) **và** không còn tham chiếu (FK/UNIQUE) tới nó (kể cả khóa `recurrence_date` của giao dịch định kỳ: dòng định kỳ đã xóa cứng trước hoặc khóa UNIQUE được đổi sang bảng riêng — xem `[OPEN]` 3). Thứ tự xóa cứng: con trước cha (`financial_transactions`/`time_blocks`/… trước `financial_accounts`/`tasks`/…).
8. `[CONFIRMED]` **D157 — Xung đột "đã bị xóa":** UPDATE trên tombstone → `CONFLICT deleted` (D84 đã duyệt). Giải quyết: "Chấp nhận xóa" (xóa bản local, bỏ mutation) hoặc "Giữ bản của tôi" = `CREATE` **id mới** với dữ liệu local (không hồi sinh id cũ để tránh va chạm với thiết bị đã nhận tombstone). `[OPEN]` có cần endpoint "restore" hồi sinh id cũ không (khuyến nghị: **không**).

### 4.2. Giải quyết xung đột

9. `[CONFIRMED]` **D158 — Chiến lược theo entity (hoàn thiện `SYNC_RULES.md` §9.5):**

| Entity | Chiến lược |
|---|---|
| Task, Project, Category | So sánh **theo trường** (3-way: base/local/server). Trường chỉ một bên đổi → tự áp dụng; cùng trường đổi khác giá trị → hỏi người dùng. Với `status` Task: ưu tiên xung đột có hỏi, không tự chọn |
| Calendar Event/Exception/Time Block | Theo trường như Task; thay đổi liên quan thời gian (`start*`, `end*`, `timezone`, `recurrence`) luôn hỏi cả bộ (không trộn từng trường thời gian) |
| Reminder | Theo trường; thay đổi `mode/offset/local_*` luôn hỏi cả bộ |
| Finance (mọi entity) | **Luôn hỏi**, không tự áp dụng theo trường (D105); hiện chênh lệch số tiền chính xác |
| `user_settings` | Theo trường (mỗi khóa độc lập); cùng khóa khác giá trị → bản có `updatedAt` mới hơn thắng **kèm thông báo** (ngoại lệ có chủ ý: cài đặt cá nhân, ít rủi ro) `[OPEN]` có áp dụng hay hỏi |
| Habit/Routine, Notes (Phase 6) | Xem tài liệu từng phase |

10. `[CONFIRMED]` **D159 — Hợp nhất 3 chiều ở client:** client có `base` (bản lúc bắt đầu sửa, lưu trong `entity_base_snapshots` trong SQLite cho entity có mutation chờ), `local`, `server` (từ `serverSnapshot`). Hàm thuần `resolve(base, local, server, strategy)` trả `{autoMerged, conflictingFields}`; có test vector dùng chung (`docs/api/fixtures/merge-vectors.json`). Nếu `conflictingFields` rỗng → tạo mutation hợp nhất tự động **và hiển thị trong nhật ký** (không im lặng); ngược lại vào hàng đợi.
11. `[CONFIRMED]` **D160 — Mutation giải quyết xung đột:** là UPDATE bình thường với `baseVersion = server.version`, `mutationId` mới; trước khi gửi, client lưu lựa chọn người dùng trong `conflict_resolutions` (audit local, giữ 30 ngày). Có giới hạn **5 lần** xung đột liên tiếp cho một entity trong 24 giờ trước khi chuyển sang "cần xem lại thủ công" (chống vòng lặp thiết bị khác sửa liên tục).
12. `[CONFIRMED]` **D161 — Mutation phụ thuộc:** nếu mutation M2 phụ thuộc kết quả M1 (CREATE cha → CREATE con) và M1 vào hàng đợi xung đột/REJECTED, M2 ở trạng thái `BLOCKED` (không gửi); khi M1 giải quyết, M2 mở khóa; khi M1 bị bỏ, M2 chuyển "cần xem lại". Client giữ đồ thị phụ thuộc theo `entityType/id` tham chiếu trong payload (danh sách tham chiếu khai báo theo entity, không suy luận động).

### 4.3. Thiết bị và phục hồi

13. `[CONFIRMED]` **D162 — Thiết bị đồng bộ = bảng `devices` của `identity`:** `PRODUCT_CONTEXT.md` §19.2 đã chốt `identity` sở hữu `devices` (và `notification` sở hữu `device_push_tokens` tham chiếu `device_id`). Vì vậy **không tạo `sync_devices`**: bảng `devices(id, user_id, refresh_family_id, name, platform, app_version, last_sync_at, created_at)` được tạo ở Phase 4 (xem D139) hoặc ở đây nếu Phase 5 làm trước (migration `IF NOT EXISTS`-an toàn theo thứ tự thực tế), không đồng bộ qua sync; mỗi họ refresh token tương ứng một thiết bị; tạo khi đăng nhập (tên do client gửi, ≤ 80); `GET /devices` liệt kê; `DELETE /devices/{id}` thu hồi họ refresh token (giống D75) và xóa token push liên quan. Đây là **mở rộng** đối với `identity` (đăng nhập nhận `deviceName` tùy chọn, cần duyệt vì sửa hợp đồng auth đã chốt).
14. `[CONFIRMED]` **D163 — Dấu vết thiết bị trong mutation:** `sync_mutations` thêm cột nullable `device_id` (không ảnh hưởng PK) để chẩn đoán; không dùng cho logic nghiệp vụ; không lộ ra thiết bị khác ngoài tên thiết bị trong màn xung đột ("Thiết bị khác — Pixel 8").
15. `[CONFIRMED]` **D164 — Toàn vẹn local:** khi mở app và sau mỗi full resync client chạy: (a) `PRAGMA integrity_check` (nhẹ: `quick_check`); (b) kiểm tra tham chiếu (FK logic) giữa các bảng đồng bộ; (c) kiểm tra `version ≥ 1`, `id` hợp lệ UUID, trường bắt buộc. Lỗi → màn hình "Dữ liệu cục bộ có vấn đề", cho phép: xuất outbox ra tệp, rồi full resync. **Không tự động xóa.**
16. `[CONFIRMED]` **D165 — Áp dụng một trang pull nguyên tử:** client áp dụng tất cả thay đổi của một trang và cập nhật `cursor` trong cùng một transaction SQLite (đã ngầm định trong `SYNC_RULES.md`; xác nhận ở đây là yêu cầu kiểm thử bắt buộc, SYN-A04).
17. `[CONFIRMED]` **D166 — Tạm dừng sync:** cờ local `sync_paused`; khi bật, không push/pull; outbox vẫn tích lũy; cảnh báo nếu outbox > 500 mutation hoặc có mutation > 20 ngày; đăng xuất bị chặn nếu còn mutation chưa gửi, trừ khi người dùng xác nhận mất dữ liệu (D167).
18. `[CONFIRMED]` **D167 — Đăng xuất an toàn:** nếu outbox không rỗng, hiện số mutation và 3 lựa chọn: "Đồng bộ rồi đăng xuất", "Xuất rồi đăng xuất", "Đăng xuất và bỏ thay đổi" (xác nhận kép). Mặc định chọn lựa chọn đầu.
19. `[CONFIRMED]` **D168 — Đa thiết bị đồng thời:** không cần thay đổi giao thức; kiểm chứng bằng kiểm thử đồng thời (SYN-A10..14) với 3 "thiết bị" mô phỏng thực hiện thao tác ngẫu nhiên có seed; mô hình tham chiếu kiểm hội tụ (mọi thiết bị sau khi sync xong có trạng thái giống server).

### 4.4. Quan sát và chẩn đoán

20. `[CONFIRMED]` **D169 — Metric phía server:** `lifeos.sync.push.mutations{result}` (đã có), `lifeos.sync.conflict.rate` (CONFLICT/APPLIED), `lifeos.sync.pull.page.size`, `lifeos.sync.snapshot.duration`, `lifeos.sync.full_resync.required`, `lifeos.sync.retention.deleted{table}`, `lifeos.sync.retention.lag` (tuổi bản ghi cũ nhất cần dọn). Cảnh báo: tỷ lệ xung đột > 10%/giờ, `full_resync.required` tăng đột biến.
21. `[CONFIRMED]` **D170 — Báo cáo chẩn đoán:** gói JSON do client tạo: phiên bản app/schema, `cursor`, số mutation theo trạng thái, 50 lỗi gần nhất (mã lỗi, `entityType`, **không** có dữ liệu trường), kết quả kiểm tra toàn vẹn. Người dùng xem trước khi gửi/lưu; gửi qua `POST /sync/diagnostics` (≤ 64 KB, 5/giờ/user), lưu 30 ngày, chỉ vận hành đọc.
22. `[CONFIRMED]` **D171 — Nhật ký đồng bộ local:** bảng `sync_log` (giữ 500 dòng gần nhất, vòng): thời điểm, loại (push/pull/resync/conflict/error), mã kết quả, `entityType`, số lượng. Không lưu giá trị trường.
23. `[CONFIRMED]` **D172 — Kiểm tra nhất quán server (vận hành):** job tuần chạy kiểm tra tham số hóa (không tự sửa): (a) `user_sync_state.last_seq` ≥ `max(change_logs.seq)`; (b) mọi entity chưa xóa có dòng `change_logs` mới nhất khớp `version`; (c) không có `mutation_id` mồ côi không tồn tại. Kết quả là metric `lifeos.sync.consistency.violations{check}`; vi phạm > 0 gây cảnh báo. Không có ghi dữ liệu.
24. `[CONFIRMED]` **D173 — Quy mô và hiệu năng:** mục tiêu — pull 500 thay đổi < 300 ms p95, snapshot 50k bản ghi < 15 s, full resync cho 50k bản ghi hoàn tất < 90 s trên mạng 10 Mbps; benchmark là test không chặn CI (nightly) với ngưỡng cảnh báo, ghi kết quả vào báo cáo.
25. `[CONFIRMED]` **D174 — Giới hạn:** snapshot trang ≤ 1000 bản ghi/≤ 1 MB; ≤ 3 snapshot đang chạy/user; `POST /sync/snapshot` 6/giờ/user, `GET /sync/entities/…` 120/phút/user. Vượt → 429 `RATE_LIMITED` (theo `SECURITY_RULES` §6). *(Cập nhật 2026-11-15 theo `docs/review/BACKEND_FIX_REPORT.md`: F4 — trần 1 MB của trang snapshot được kiểm TRƯỚC khi thêm từng bản ghi, gồm cả phần vỏ response; trang dừng trước bản ghi làm vượt trần và `next` trỏ tới bản ghi cuối đã trả. Trang luôn có ít nhất một bản ghi nên một trang có thể ít hơn `limit` rất nhiều khi bản ghi lớn — client phải lặp tới khi `next = null`)*

## 5. Thay đổi dữ liệu (`V8__sync_advanced.sql`)

```sql
ALTER TABLE user_sync_state ADD COLUMN min_retained_seq bigint NOT NULL DEFAULT 0;
ALTER TABLE sync_mutations  ADD COLUMN device_id uuid;           -- nullable, không FK cứng (thiết bị có thể bị xóa)
CREATE TABLE devices (   -- module identity; bỏ qua nếu đã tạo ở Phase 4
  id uuid PRIMARY KEY, user_id uuid NOT NULL REFERENCES users(id),
  refresh_family_id uuid NOT NULL,                              -- khớp refresh_tokens.family_id
  name varchar(80) NOT NULL, platform varchar(10) NOT NULL, app_version varchar(30),
  last_sync_at timestamptz, created_at timestamptz NOT NULL,
  UNIQUE (refresh_family_id)
);
-- INDEX (user_id)
CREATE TABLE sync_diagnostics (
  id uuid PRIMARY KEY, user_id uuid NOT NULL REFERENCES users(id),
  payload jsonb NOT NULL, created_at timestamptz NOT NULL
);
-- INDEX (created_at)  -- dọn sau 30 ngày
-- INDEX phục vụ retention: change_logs (user_id, seq) đã có; tasks/projects/... (user_id, deleted_at) WHERE deleted_at IS NOT NULL
```

SQLite (client): `entity_base_snapshots`, `conflict_resolutions`, `sync_log`, bảng tạm `resync_staging_*`. Không có migration phía server cho các bảng này.

## 6. API mới (`/api/v1/sync`, ngoài bộ ba đã có)

| Endpoint | Ghi chú |
|---|---|
| `POST /sync/snapshot` | D152; trả `{snapshotToken, snapshotSeq, entities:[{type, items:[…]}], next}`; kích thước theo D174 |
| `GET /sync/entities/{type}/{id}` | Trả bản server hiện tại (kể cả tombstone ≤ retention) cho client giải quyết; IDOR như REST |
| `GET /devices`, `DELETE /devices/{id}` | D162 (thuộc `identity`) |
| `POST /sync/diagnostics` | D170 |
| `POST /sync/pull` | Thêm mã lỗi 410 `SYNC_FULL_RESYNC_REQUIRED` (D150) |

Mã lỗi mới `[CONFIRMED]`: `SYNC_FULL_RESYNC_REQUIRED` (410), `SYNC_SNAPSHOT_EXPIRED` (410, `snapshotToken` hết hạn → client làm lại từ đầu), `SYNC_DEVICE_NOT_FOUND` (404), `ENTITY_ALREADY_EXISTS` (kết quả REJECTED, D155).

## 7. Bảo mật

- Snapshot/entities: mọi truy vấn theo `user_id`; `snapshotToken` ký HMAC gắn `userId`, không dùng được cho user khác; chống tải hàng loạt (D174).
- Không đưa dữ liệu người dùng vào chẩn đoán (D170) hoặc log; `SensitiveLogIT` mở rộng.
- Thu hồi thiết bị: ngay lập tức vô hiệu họ refresh token (D75); access token còn hạn ≤ 15 phút là rủi ro chấp nhận (ghi rõ trong UI).
- Retention xóa cứng không phá IDOR: không có đường truy cập tới tombstone đã xóa.
- Tệp xuất outbox (D164/D167) chứa dữ liệu thô của người dùng: lưu ở vùng riêng của app, cảnh báo khi chia sẻ; không tự tải lên.

## 8. Kế hoạch kiểm thử (SYN-Axx)

| ID | Kịch bản | Kỳ vọng |
|---|---|---|
| SYN-A01 | Pull với cursor < `min_retained_seq` | 410 `SYNC_FULL_RESYNC_REQUIRED` |
| SYN-A02 | Full resync giữ outbox: CREATE/UPDATE/DELETE chờ gửi, entity còn/không còn trên server | `baseVersion` tính lại đúng; mutation mất cha → hàng đợi xung đột |
| SYN-A03 | Snapshot bị ngắt giữa chừng và `snapshotToken` hết hạn | Local không đổi; làm lại được |
| SYN-A04 | Mất điện (kill process) giữa lúc áp dụng trang pull | Hoặc toàn bộ trang + cursor, hoặc không gì cả |
| SYN-A05 | Snapshot nhất quán dưới ghi đồng thời | Pull sau `snapshotSeq` bổ sung đúng, không sót/trùng |
| SYN-A06 | Retention `change_logs`: đúng `min_retained_seq`, không xóa dòng cần cho cursor hợp lệ, theo lô, không khóa dài | Đúng |
| SYN-A07 | Retention `sync_mutations` + CREATE lặp (D155) | `ENTITY_ALREADY_EXISTS` → client coi là thành công khi trùng dữ liệu |
| SYN-A08 | Retention tombstone: bị chặn khi còn tham chiếu/khóa định kỳ, đúng thứ tự con-trước-cha | Đúng |
| SYN-A09 | Hợp nhất 3 chiều: vector dùng chung Java-less (Dart) cho Task/Calendar/Reminder/Finance/settings | Đúng từng vector |
| SYN-A10 | 3 thiết bị mô phỏng, thao tác ngẫu nhiên có seed (≥ 1000 bước, 20 seed) | Hội tụ; không mất cập nhật không có xung đột; không trùng bản ghi |
| SYN-A11 | Giải quyết xung đột lặp (thiết bị khác sửa giữa chừng) | Xung đột mới, giới hạn 5 lần/24h (D160) |
| SYN-A12 | Mutation phụ thuộc BLOCKED/mở khóa/bỏ | Đúng D161 |
| SYN-A13 | Đăng xuất an toàn với outbox không rỗng | Đúng D167 |
| SYN-A14 | Thu hồi thiết bị từ xa: refresh bị từ chối, push token gỡ | Đúng D162 |
| SYN-A15 | Toàn vẹn local: hỏng FK/`version`/UUID → màn hình khôi phục, không xóa | Đúng D164 |
| SYN-A16 | Chẩn đoán không chứa dữ liệu trường; giới hạn/ rate limit | Đúng D170 |
| SYN-A17 | Job nhất quán server phát hiện vi phạm cấy sẵn, không ghi dữ liệu | Đúng D172 |
| SYN-A18 | Hiệu năng nightly: pull 500, snapshot 50k, resync 50k | Trong ngưỡng D173 hoặc cảnh báo |
| SYN-A19 | IDOR snapshot/entities/devices; token snapshot của user khác | 404/403 như quy ước |
| SYN-A20 | Finance conflict: luôn hỏi, số tiền chuỗi chính xác, không tự hợp nhất | Đúng D105/D158 |

## 9. Các bước triển khai và tiêu chí hoàn thành

| Bước | Nội dung |
|---|---|
| 5a | Server: `min_retained_seq`, retention jobs, `410`, snapshot + entities, `V8` |
| 5b | Client: full resync, bảng tạm, nạp nguyên tử, giữ outbox, toàn vẹn local |
| 5c | Client: hợp nhất 3 chiều, hàng đợi xung đột, màn hình giải quyết, phụ thuộc mutation |
| 5d | Thiết bị đồng bộ, đăng xuất an toàn, tạm dừng, nhật ký, chẩn đoán |
| 5e | Kiểm thử đa thiết bị mô phỏng, hiệu năng, quan sát, hardening |

Hoàn thành khi: SYN-A01..20 đạt (test ngẫu nhiên có seed lặp ≥ 20 lần trên CI nightly), `mvn clean verify` và `flutter test` xanh, không có đường nào mất mutation im lặng (kiểm chứng bằng SYN-A02/A04/A10).

## 10. Quyết định và câu hỏi mở

D150–D174 như trên (D175–D179 để dành). `[OPEN]` cần người dùng quyết: (1) thời hạn retention (khuyến nghị 180/35 ngày như trên); (2) `user_settings` xung đột: hỏi hay "bản mới hơn thắng có thông báo" (khuyến nghị: bản mới hơn thắng có thông báo); (3) khóa UNIQUE định kỳ của Finance (D110) cản xóa cứng — khuyến nghị tách `recurrence_occurrences(recurring_id, recurrence_date)` bảng nhỏ giữ vĩnh viễn thay vì giữ tombstone giao dịch; (4) hồi sinh id cũ (khuyến nghị: không); (5) ~~gộp `sync_devices`~~ đã chốt ở PRODUCT_CONTEXT: một bảng `devices` (identity); chỉ cần quyết định phase nào tạo bảng trước (khuyến nghị: Phase 4).
