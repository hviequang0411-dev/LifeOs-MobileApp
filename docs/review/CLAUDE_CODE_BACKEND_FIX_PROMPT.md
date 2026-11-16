# Prompt sửa backend theo kết quả review GPT (`CLAUDE_CODE_BACKEND_FIX_PROMPT.md`)

> Gửi nguyên văn cho Claude Code. Phạm vi: `backend/`, `docs/api/openapi.yaml` (phần không-AI), `docs/engineering/*`, `AGENTS.md`, `docs/review/BACKEND_*`. **Không sửa** `mobile/`, `ci/mobile-ci.yml`, tài liệu `MOBILE_*`, `deploy/`, `ops/`, và module `ai` (thuộc agent khác).

---

## 0. Bối cảnh và nguyên tắc

Một agent review độc lập đã rà backend bằng phân tích tĩnh (chưa chạy test) và nêu 8 phát hiện cùng vài mâu thuẫn tài liệu. Nhiệm vụ của bạn:

1. **Tái hiện trước, sửa sau.** Với từng phát hiện F1–F9: viết test tái hiện đúng kịch bản nêu dưới, xác nhận test ĐỎ, rồi mới sửa cho XANH. Nếu không tái hiện được, **không sửa mù**: ghi `KHÔNG TÁI HIỆN ĐƯỢC` kèm bằng chứng (test đã viết, đầu ra, giải thích dòng code) trong báo cáo.
2. Không nới, xóa hay `@Disabled` test hiện có. Test đồng thời dùng latch/barrier, lặp ≥ 20, không `Thread.sleep`.
3. V1–V11 bất biến. Thay đổi lược đồ của đợt sửa này gom vào **một migration mới `V12__backend_fixes.sql`** (xem F5, F8). Lưu ý: tài liệu Phase 8 AI đang ghi V12 cho AI; đợt này lấy V12, nên khi cập nhật tài liệu (mục 3) hãy sửa tham chiếu đó thành "migration AI dùng số kế tiếp còn trống (V13+)".
4. Giữ mọi quy tắc đã chốt: D86 (khóa nghiệp vụ trước, `ChangeRecorder.record` cuối), `userId` từ `CurrentUser`, không gọi mạng trong transaction, tiền không `double`.
5. Làm **một lượt liên tục**, một báo cáo cuối `docs/review/BACKEND_FIX_REPORT.md`. Chỉ dừng nếu một quyết định dưới đây hóa ra mâu thuẫn với một quy tắc `[CONFIRMED]` khác.

Người dùng đã quyết các câu hỏi của reviewer như sau (coi là duyệt chính thức):

- Không cho chuyển transaction/recurring sang tài khoản khác tiền tệ (từ chối, không tái định giá ngầm).
- Notification cha phải sống đến khi mọi delivery kết thúc.
- Delivery là at-least-once, nhưng lease phải chặn gửi trùng trong vận hành bình thường.
- Giới hạn export (1 active/user, 2/ngày) là giới hạn cứng, phải đúng cả khi concurrency.
- Purge user phải xóa cả outbox event còn mang `userId`.

## 1. Các phát hiện phải xử lý

### F1 — BLOCKER: đổi tài khoản làm đổi tiền tệ ngầm
`TransactionCommandService` (~dòng 115) và `RecurringCommandService` (~dòng 137): PATCH chỉ đổi `accountId` sang tài khoản khác currency thì amount giữ nguyên trị số, currency đổi theo tài khoản (100 USD thành 100 VND). Trái D103.
**Sửa:** khi currency tài khoản đích khác currency hiện tại của transaction/recurring → từ chối với error code mới `FINANCE_CURRENCY_MISMATCH` (422). Thêm vào OpenAPI + bảng lỗi. Test: hai tài khoản USD/VND, PATCH đổi account, kỳ vọng 422 và dữ liệu không đổi; test tương tự cho recurring.

### F2 — MAJOR: cleanup xóa notification trước khi push/email được gửi
`NotificationOrchestrator` (~90), `NotificationJobs` (~48), `V8__notification.sql` (~58): tắt IN_APP mà bật PUSH thì `expires_at = now`; cleanup xóa cha và cascade xóa delivery PENDING trong quiet hours → push không bao giờ gửi. Trái D121/D135.
**Sửa:** cleanup chỉ xóa notification khi không còn delivery ở PENDING/IN_FLIGHT (hoặc tách thời hạn inbox khỏi vòng đời delivery — chọn cách ít xâm lấn, nêu lý do). Test: IN_APP tắt, PUSH bật, quiet hours còn 2 giờ, chạy cleanup, delivery vẫn còn và được gửi khi hết quiet hours.

### F3 — MAJOR: delivery lease không có ownership token
`NotificationStore` (~144), `DeliveryWorker` (~163): finish/retry chỉ kiểm `status = 'IN_FLIGHT'`, không so khớp lease → worker A quá hạn và worker B reclaim có thể cùng gửi.
**Sửa:** thêm `claim_token` (hoặc so khớp `locked_until`) theo đúng mẫu `OutboxStore.OWNED` đang có; mọi finish/retry phải khớp token. Test concurrency: 2 worker tranh một delivery, đúng một lần gửi.

### F4 — MAJOR: trang snapshot không giữ trần 1 MB
`SyncSnapshotService` (~125): kiểm kích thước sau khi đã cộng cả lô → 200 note gần 192 KB cho trang ~38 MB. Trái D174/SYNC_PROTOCOL.
**Sửa:** kiểm trước khi thêm từng item, dừng trước item vượt trần, cursor chính xác, thêm hard limit cho response. Test: nhiều note lớn, trang ≤ trần và không mất/lặp item khi phân trang.

### F5 — MAJOR: giới hạn export không đúng dưới concurrency
`DataExportService` (~63), `AccountStore` (~97): check-then-insert ở READ COMMITTED → 3 POST đồng thời cùng qua.
**Sửa:** khóa dòng `users` trước check/insert (hoặc advisory lock) **và** partial unique index trong `V12__backend_fixes.sql` bảo đảm tối đa một export PENDING/RUNNING mỗi user; quota 2/ngày cập nhật nguyên tử. Test barrier ≥ 20 lượt.

### F6 — MAJOR: recurring bỏ lặng occurrence khi tài khoản archive
`RecurringRunService` (~116): account archive mà recurring ACTIVE → mỗi lượt vẫn tiến `nextRunOn`, mất vĩnh viễn các giao dịch kỳ vọng.
**Sửa (người dùng đã bác phương án bỏ qua lặng):** khi account không ghi được → tự chuyển recurring sang PAUSED và tạo notification báo người dùng; thao tác archive account phải pause các recurring phụ thuộc. Không tiến cursor khi không tạo giao dịch. Test: archive 10 ngày, unarchive, không mất lặng; có notification.

### F7 — MINOR: analytics cắt 200.000 dòng im lặng
`TaskStatsApi`/`TaskStatsApiImpl`: vượt MAX_ROWS thì kết quả sai không báo.
**Sửa:** query MAX_ROWS+1; nếu vượt → trả lỗi kích thước kỳ hoặc cờ `truncated=true` trong response (chọn một, cập nhật OpenAPI). Nếu tổng hợp được tại DB thì tốt hơn, nhưng không bắt buộc trong đợt này.

### F8 — MINOR: purge user sót outbox event
`UserDataPurger` (~41): chỉ xóa `aggregate_type='user'`; event BUDGET_THRESHOLD mang `userId` trong payload không bị xóa.
**Sửa:** thêm cột `user_id` vào `outbox_events` (trong `V12__backend_fixes.sql`, kèm backfill từ payload cho event hiện có nếu suy ra được, còn lại NULL), purge theo cột này. Test: purge user với outbox worker dừng, không còn event mang `userId`.

### F9 — Snapshot HMAC key (từ danh sách [PROPOSED], người dùng đồng ý sửa)
Key đang sinh ngẫu nhiên lúc khởi động → restart/rolling deploy làm hỏng chữ ký cũ.
**Sửa:** đọc từ secret `SNAPSHOT_HMAC_SECRET`; fail-fast ở production khi thiếu; dev/test có default rõ ràng. Cập nhật tài liệu cấu hình.

## 2. Các [PROPOSED] được duyệt giữ nguyên (chỉ cập nhật tài liệu, không đổi code)

- Export qua endpoint xác thực (tối đa 5 lượt tải) thay URL ký: **duyệt** — cập nhật Phase 7/OpenAPI cho khớp.
- Offset tìm kiếm note ≤ 1000: **duyệt**.
- Một module `analytics` thay `dashboard`/`lifereview`: **duyệt** — sửa danh sách module trong `AGENTS.md` §3 và `JAVA_SPRING_RULES.md` cho khớp mã.
- `EXPORT_DIR` cục bộ: **duyệt** cho một instance.

## 3. Cập nhật tài liệu trong cùng đợt

- Bảng lỗi/OpenAPI: thêm `FINANCE_CURRENCY_MISMATCH` và thay đổi của F7.
- `SYNC_PROTOCOL.md`/Phase 5: ghi chú cách thực thi trần trang snapshot nếu hành vi phân trang thay đổi.
- Phase 3/4/7: cập nhật các điểm D103, D121/D135, D137, D174, D204 cho khớp hành vi sau sửa (ghi "cập nhật 2026-11-10 theo BACKEND_FIX_REPORT").
- `AGENTS.md`: danh sách module (mục 2) và một dòng trạng thái "backend fix đợt 1 — xem `docs/review/BACKEND_FIX_REPORT.md`". Tham chiếu V12 của Phase 8 AI đổi thành "V13+ (số kế tiếp còn trống)".

## 4. Báo cáo cuối (`docs/review/BACKEND_FIX_REPORT.md`)

- Bảng F1–F9: trạng thái `ĐÃ SỬA (test đỏ→xanh)` / `KHÔNG TÁI HIỆN ĐƯỢC (bằng chứng)` / `CHƯA LÀM (lý do)`; mỗi mục liệt kê file và test mới.
- Dán nguyên văn tổng kết `mvn clean verify` (xanh 3 lượt liên tiếp, kèm tổng số test — dự kiến > 847 vì có test mới).
- Danh sách thay đổi OpenAPI/tài liệu.
- Mọi quyết định phát sinh ghi `[PROPOSED]`, không tự coi là duyệt.
