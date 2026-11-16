# Prompt review LifeOS (vòng 5: toàn bộ backend sau BACKEND_MASTER_PLAN)

Dán phần dưới dòng `---` vào GPT và **đính kèm các file ở mục 1** (nhóm P0 bắt buộc, P1 nên có, P2 nếu giới hạn cho phép). Đây là review **backend đã cài đặt** (tài liệu + migration + các lớp mã trọng tâm), không review Flutter (chưa làm).

---

Bạn là kiến trúc sư backend senior kiêm security reviewer và chuyên gia dữ liệu tài chính. Hãy rà soát **backend của LifeOS** (ứng dụng quản lý cuộc sống cá nhân, offline-first; Java 21, Spring Boot, Modular Monolith, PostgreSQL 17, Flyway, Maven). Backend vừa được một AI agent (Claude Code) hoàn thiện từ đặc tả: 12 module, 29 `entityType` đồng bộ, 847 test xanh theo báo cáo của chính agent. **Đừng tin báo cáo; hãy kiểm tra.** Câu hỏi chính: *"Với dữ liệu thật của người dùng thật (tiền, thói quen, ghi chú riêng tư), hệ thống này có thể làm mất, lệch, trùng, lộ dữ liệu hoặc thông báo sai/lặp/mất không?"*

## 0. Bước 0 — chứng minh đã đọc (bắt buộc, làm trước mọi nhận xét)

Trong phần đầu câu trả lời, liệt kê từng file bạn **thực sự đọc được** (tên file + 1 dòng nội dung then chốt, kèm **một trích nguyên văn ≤ 15 từ** từ file đó). File không đọc được: ghi rõ "KHÔNG ĐỌC ĐƯỢC". Mọi phát hiện sau đó chỉ được dựa trên file đã đọc. **Không bịa trích dẫn**; một vòng review trước đã bị bác hoàn toàn vì trích dẫn không có trong tài liệu.

## 1. Tài liệu và mã cần đọc

**P0 — bắt buộc**

| # | File | Vai trò |
|---|---|---|
| 1 | `docs/review/BACKEND_FINAL_REPORT.md` | Tổng quan, 75 quyết định `[PROPOSED]`, rủi ro tự nhận, hoãn |
| 2 | `docs/engineering/BACKEND_MASTER_PLAN.md` | Quy tắc tự chủ, quyết định mặc định (§4), thay đổi hợp đồng cho phép (§5) |
| 3 | `docs/engineering/SYNC_PROTOCOL.md` + `SYNC_RULES.md` | Hợp đồng đồng bộ (D81–D96 và mở rộng) |
| 4 | `docs/phases/PHASE_3_FINANCE.md` | Tiền, số dư, định kỳ, ngân sách |
| 5 | `docs/phases/PHASE_4_CALENDAR_NOTIFICATION.md` | Lịch lặp, reminder, thông báo, outbox |
| 6 | `docs/phases/PHASE_5_SYNC_ADVANCED.md` | Retention, snapshot, 410, chẩn đoán |
| 7 | `backend/src/main/resources/db/migration/V5…V11` | 7 migration mới |
| 8 | `docs/engineering/SECURITY_RULES.md`, `JAVA_SPRING_RULES.md`, `DATABASE_RULES.md`, `API_CONVENTIONS.md` | Quy tắc đã chốt |

**P1 — nên có**

| # | File | Vai trò |
|---|---|---|
| 9 | `docs/phases/PHASE_2_HABIT_ROUTINE.md`, `PHASE_6_KNOWLEDGE_GOALS.md`, `PHASE_7_INTELLIGENCE_ANALYTICS.md` | Đặc tả còn lại |
| 10 | `docs/review/BACKEND_PROGRESS.md` | Nhật ký từng WP: test cũ đã sửa, ánh xạ test ID |
| 11 | Mã: `SyncPushService`, `SyncPullService`, handler của `finance.sync`, `RecurringTransactionJob`, `BudgetAlert*`, `ReminderScheduler`, `NotificationOrchestrator`, delivery worker, `QuietHours*`, `UnsubscribeToken*` | Lõi tiền/đồng bộ/thông báo |
| 12 | Mã: `GoalCommandService`, `TaskCommandService`, `GoalApiImpl`, `PendingChanges`, các listener cascade | Cascade nhiều module, D86 |
| 13 | Mã: `UserDataExporter`, `UserDataCatalog`, `AccountDeletionGuardFilter`, job xóa tài khoản, `NoteSearchStore`, `SyncRetentionJob`, snapshot service | Riêng tư, xóa, tìm kiếm, retention |
| 14 | `docs/ops/ALERTS.md`, `docker-compose.yml`, `application*.yml` | Vận hành |

**P2 — nếu còn chỗ:** test trọng tâm (`LockOrderGuard`, `SyncConcurrencyIT`, `FinanceSyncIT`, `RecurringIT`, `AccountDeletionIT`, `MigrationUpgradeIT`, `BackendBenchmark`), `docs/api/openapi.yaml`, `docs/api/fixtures/*`.

Nếu không đọc hết, **ưu tiên theo trọng tâm A→D** (mục 3) và nêu rõ phần nào chưa được kiểm tra. Không suy đoán nội dung file thiếu.

## 2. Lịch sử và những điều đã chốt (đừng lặp lại)

- **Đã bác ở các vòng trước:** `seq` toàn cục (đã chọn `seq` theo user); `parentMutationId` (cascade ở server); thu hồi cả family mỗi lần refresh (chỉ khi token đã dùng bị trình lại); `ON DELETE SET NULL` (chỉ xóa mềm); yêu cầu độ phức tạp mật khẩu (theo NIST); xóa SQLite khi đăng xuất.
- **Đã chốt, chỉ phản biện nếu có lỗi thật:** D81–D96 (ma trận kết quả sync, `DUPLICATE` phát lại nguyên văn, tombstone DELETE → `APPLIED`, lỗi lạ → 500 không lưu); D86 (khóa/flush nghiệp vụ trước, `ChangeRecorder.record` cuối); D105 (Finance luôn `CONFLICT`, không tự hợp nhất); D100–D104 (`NUMERIC(19,4)`, JSON tiền là chuỗi, một tiền tệ/tài khoản, chuyển khoản cùng tiền tệ); D110 (định kỳ do server sinh, idempotent theo `(recurring_id, recurrence_date)`); D235 (check-in trùng ngày → `CONFLICT ALREADY_EXISTS`).
- **Cố ý hoãn / ngoài phạm vi — đừng báo là thiếu:** toàn bộ Flutter (My Day, lập lịch local, FTS5, hợp nhất 3 chiều, SYN-A02..A04/A09/A11..A13/A15, KNW-05, INT-10 phần Dart); Redis (benchmark đạt ngưỡng, giữ cache no-op); CI GitHub/nightly; FR-GOAL-06; nợ/cho vay/hoàn tiền/quy đổi tiền tệ/nhập CSV; `RECURRING_DUE`/`HABIT_STREAK_AT_RISK` chưa có nguồn phát; mã hóa dữ liệu at rest; chạy nhiều instance.
- **Rủi ro agent tự nhận (hãy đánh giá mức độ thật, đừng chỉ lặp lại):** giới hạn đếm không khóa có thể vượt 1; tìm kiếm ghi chú dùng offset ≤ 1000; khóa HMAC snapshot sinh ngẫu nhiên khi khởi động; cột `time` + `hibernate.jdbc.time_zone=UTC`; `TaskStatsApi.completedBetween` cắt 200 000 dòng; tệp xuất mồ côi khi xóa tài khoản chạy đồng thời; rate limiter/export dir trong bộ nhớ/cục bộ.

## 3. Nhiệm vụ

Tìm lỗi **thật**, không khen chung chung. Mỗi phát hiện phải có:

1. **Mức độ:** Blocker (mất/lệch/lộ dữ liệu, tiền sai, lỗ hổng) · Major · Minor · Nit.
2. **Vị trí:** file + mục/lớp/phương thức.
3. **Trích nguyên văn** dòng/đoạn mã hoặc câu tài liệu gây vấn đề (bắt buộc; không trích được thì không báo).
4. **Vấn đề** và **kịch bản cụ thể** (dữ liệu hoặc chuỗi sự kiện cụ thể, không nói "có thể").
5. **Đề xuất sửa** ngắn.
6. **Độ chắc chắn:** Chắc chắn / Có thể / Cần thêm thông tin.

Chỉ nêu điều đã đối chiếu với mã/tài liệu. Điều đã được test hoặc tài liệu xử lý ở chỗ khác thì không báo.

## 4. Trọng tâm (theo thứ tự quan trọng)

**A. Tiền (Finance, Phase 3)**
- Số dư = opening + thu − chi + chuyển đến − chuyển đi: có đường nào làm số dư lệch (giao dịch tương lai, tombstone, chuyển khoản sửa/xóa, đối soát, tài khoản lưu trữ, `asOf` theo múi giờ)? Có chỗ nào dùng `double/float/BigDecimal` không `scale` rõ, so sánh `equals` thay `compareTo`, làm tròn ngầm (JSON, Jackson, Hibernate, `SUM`, `ROUND`)?
- Giao dịch định kỳ: chạy lặp/song song/crash giữa chừng; ngày 31/tháng ngắn, 29/2, DST, múi giờ người dùng đổi; bù tối đa 31 lần; tạm dừng/tiếp tục; xóa giao dịch đã sinh rồi chạy lại; khóa `SKIP LOCKED` và thứ tự khóa D86.
- Ngân sách: ranh giới tháng theo múi giờ, tách tiền tệ, loại trừ `ADJUSTMENT`/chuyển khoản; cảnh báo ngưỡng 80/100% có thật sự đúng một lần/tháng/ngưỡng khi sửa/xóa giao dịch làm tụt rồi vượt lại?
- Sync Finance: tham chiếu sai, thứ tự phụ thuộc, `currency` bất biến, mass assignment (`currency`, `recurrenceDate`, `nextRunOn`, `ACHIEVED`).

**B. Đồng bộ ở quy mô 29 entity (Phase 1–6)**
- D86 còn đúng sau khi thêm cascade nhiều module (Task→time block/reminder/note_link; Goal→milestone/note_link/gỡ liên kết Task; Habit/Routine; Note/Tag)? Tìm vòng chờ khóa cụ thể (REST xen sync, hai thiết bị, FK `FOR KEY SHARE`).
- Retention: có kịch bản retention xóa `change_logs`/tombstone mà một cursor/mutation hợp lệ vẫn cần (race với push đang chạy, `min_retained_seq` cập nhật sau khi xóa)? Xóa cứng tombstone có phá FK/UNIQUE (khóa định kỳ Finance, `step_id` của `routine_step_logs`, `note_links` đa hình)? Client sau 410 + snapshot có thể mất mutation chờ gửi không (nhìn phía server: cung cấp đủ thông tin chưa)?
- Snapshot: tính nhất quán `REPEATABLE READ`, `snapshotToken` HMAC (khóa ngẫu nhiên khi khởi động, nhiều request song song), phân trang theo `(entityType, id)`, tombstone vắng mặt trong snapshot.
- `ALREADY_EXISTS` mở rộng (thẻ trùng tên, review trùng kỳ, check-in trùng ngày): có thể làm client kẹt hàng đợi hoặc rò thông tin tồn tại sang user khác?

**C. Thông báo và công việc nền (Phase 4)**
- Máy trạng thái `notification_deliveries` (`PENDING/IN_FLIGHT/SENT/FAILED/SKIPPED`): crash sau khi gửi FCM/SMTP nhưng trước khi ghi `SENT` → gửi lặp? lease `IN_FLIGHT` hết hạn đua với worker khác? Có gọi mạng trong transaction DB không?
- `dedupe_key` có thật sự chặn thông báo lặp qua restart/chạy song song; bù tối đa một lần trong 24 giờ; quiet hours qua nửa đêm/DST/đổi múi giờ; reminder tương đối của sự kiện lặp, ngoại lệ lần xuất hiện.
- Nội dung push/email có chứa dữ liệu nhạy cảm (số tiền, ghi chú, tiêu đề Task) trái D138? Chống header injection email; liên kết hủy đăng ký (HMAC riêng, hết hạn, không áp dụng cho `SECURITY`); email bảo mật vẫn hoạt động sau khi chuyển module.
- Lỗi đã biết: scheduler một luồng làm đói outbox (đã sửa pool = 4). Còn job nào có thể bị đói/chạy chồng/chạm cùng dữ liệu (xuất dữ liệu, xóa tài khoản, retention, định kỳ, reminder, outbox)?

**D. Bảo mật và riêng tư toàn hệ thống**
- IDOR/mass assignment cho **mọi** route và entity mới (kể cả tham chiếu đa hình `note_links.target_id`, `reminders.target_id`, `{date}`, `{stepId}`, snapshot/entities/devices).
- Xuất dữ liệu: loại trừ bí mật (`UserDataCatalog.EXPORT_EXCLUDED`), CSV/formula injection, đường dẫn tệp (path traversal), tải qua endpoint xác thực (5 lượt), tệp mồ côi. Xóa tài khoản: cooldown 7 ngày, mã email, thu hồi token, `AccountDeletionGuardFilter` (có đường nào đi vòng?), thứ tự xóa cứng theo `ctid`, bảng nào có `user_id` mà không nằm trong danh mục xuất/xóa.
- Rate limit trong bộ nhớ (khóa theo IP khi chạy sau reverse proxy: `X-Forwarded-For` có bị giả mạo/không được cấu hình?), log/metric/lỗi có lộ dữ liệu cá nhân; secret production (`NOTIFICATION_UNSUBSCRIBE_SECRET`, JWT) và cấu hình mặc định nguy hiểm trong `application*.yml`/compose.

**E. Đúng đắn của Habit, Goal, Ghi chú, Analytics (Phase 2, 6, 7)**
- Chuỗi/tỷ lệ Habit (D233–D234) so với mã và vector `habit-vectors.json`: `TIMES_PER_WEEK`, skip, ngoài lịch, `week_start_day`, đổi múi giờ. Tiến độ Goal (D185) và `AT_RISK/OVERDUE` (D200).
- Tìm kiếm ghi chú: `websearch_to_tsquery` tham số hóa, `lifeos_unaccent` có thật sự IMMUTABLE/đúng, cột `GENERATED`, GIN thật sự được dùng, offset ≤ 1000, nội dung 50 000 ký tự/192 KB.
- Analytics (D198–D199, D192): ranh giới ngày/tuần/tháng theo múi giờ, Task xóa mềm, mẫu < 5 → null, `savingsRate`, tách tiền tệ; cắt 200 000 dòng im lặng; dashboard 5 thẻ song song so với kích thước pool.

**F. Cơ sở dữ liệu và migration (V5–V11)**
- Ràng buộc CHECK/UNIQUE từng phần (`WHERE deleted_at IS NULL`) có đúng với logic; FK thiếu/thừa; kiểu cột; chỉ mục thiếu theo mẫu truy vấn; tên dài; migration có thể khóa bảng lớn khi nâng cấp từ DB có dữ liệu; `hibernate.jdbc.time_zone=UTC` với cột `time`; `TZ` của JVM/Docker.

**G. Chất lượng kiểm thử**
- 75 quyết định `[PROPOSED]` và danh sách **test cũ đã sửa** (`BACKEND_FINAL_REPORT.md` §6): sửa nào thực chất làm yếu kiểm thử? Test đồng thời có thật sự phát hiện lỗi (có kiểm phân phối thứ tự, `@RepeatedTest`, `LockOrderGuard` có bị bypass)? ID đặc tả nào ghi "Đủ" mà thực tế chỉ kiểm tay hoặc kiểm yếu?

**H. Nhất quán tài liệu ↔ mã ↔ OpenAPI**
- Mâu thuẫn giữa đặc tả phase, `SYNC_PROTOCOL.md`, `API_CONVENTIONS.md`, `openapi.yaml`, `DATABASE_DESIGN.md`, `AGENTS.md` (ví dụ `AGENTS.md` §3 còn ghi module `dashboard`, `lifereview`). ID tham chiếu (D100–D259, FIN/CAL/NTF/SYN-A/KNW/INT/HAB/RTN) có tồn tại và đúng?

**I. Sẵn sàng vận hành (không phải deploy hoàn chỉnh)**
- Một instance duy nhất: những gì gãy khi chạy 2 instance; health/readiness; cấu hình pool; backup/restore gợi ý; `docker-compose.yml` dev profile có bị dùng nhầm cho production không.

## 5. Định dạng trả lời

1. **Bước 0** (danh sách file đã đọc + trích dẫn).
2. **Tóm tắt 5 dòng:** đánh giá chung và 3 rủi ro lớn nhất với dữ liệu người dùng thật.
3. **Bảng phát hiện** sắp theo mức độ (cột: #, Mức độ, Vị trí, Trích nguyên văn, Vấn đề + kịch bản, Đề xuất, Độ chắc chắn).
4. **Mâu thuẫn giữa tài liệu/mã** (mỗi mục nêu cả hai vị trí).
5. **Đánh giá các quyết định `[PROPOSED]` đáng ngờ** (chọn ≤ 8 trong 75; nêu nên duyệt, sửa hay bác).
6. **Câu hỏi cho tác giả** do thiếu thông tin.
7. **Những gì làm tốt và không nên đổi** (tối đa 5 dòng).
8. **Phần chưa kiểm tra được** (file/hạng mục không đọc được hoặc ngoài khả năng review).

Không viết lại tài liệu, không viết lại mã dài (chỉ vài dòng minh họa khi cần), không đề xuất công nghệ ngoài stack trừ khi có lý do bảo mật/nhất quán cụ thể. Không đánh giá Flutter.
