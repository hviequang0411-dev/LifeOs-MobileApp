# Báo cáo đợt sửa backend 1 (`BACKEND_FIX_REPORT.md`)

> Thực hiện 2026-11-15 theo `docs/review/CLAUDE_CODE_BACKEND_FIX_PROMPT.md` (trạng thái HOÃN được người dùng gỡ cùng
> ngày) và các quyết định duyệt nhãn kèm theo (`PENDING_DECISIONS.md` §1–§2). Phạm vi: `backend/`, tài liệu
> `docs/engineering`, `docs/phases`, `docs/review/BACKEND_*`. Mọi phát hiện đều được **tái hiện bằng test ĐỎ trên mã chưa
> sửa trước khi sửa**. Người viết báo cáo cũng là người viết mã — báo cáo này KHÔNG thay cho review độc lập (AGENTS §1.7).

## 1. Kết quả theo phát hiện

Lượt chạy ĐỎ (mã chưa sửa, chỉ có test mới): `Tests run: 52, Failures: 30, Errors: 2` — mọi lỗi đều thuộc F1–F9 như bảng
dưới; 20 lượt của test đua hai worker (F3, test bảo vệ) xanh sẵn. Sau khi sửa: cùng các test đó xanh.

| # | Mức | Trạng thái | Bằng chứng đỏ trên mã cũ | Cách sửa | File chính | Test mới |
|---|---|---|---|---|---|---|
| F1 | BLOCKER | **ĐÃ SỬA (đỏ→xanh)** | PATCH đổi `accountId` USD→VND trả 200 với `"amount":"100","currency":"VND"` (giao dịch và định kỳ) | Đổi sang tài khoản khác tiền tệ ⇒ 422 `FINANCE_CURRENCY_MISMATCH`, kể cả khi gửi kèm `amount`; cùng tiền tệ vẫn được | `TransactionCommandService`, `RecurringCommandService` | `FinanceFixIT.F1_*` (2) |
| F2 | MAJOR | **ĐÃ SỬA (đỏ→xanh)** | IN_APP tắt, PUSH bật, đang giờ yên tĩnh: `deleteExpired` xóa thông báo cha, delivery PENDING bị cascade xóa | Job dọn bỏ qua thông báo còn delivery `PENDING`/`IN_FLIGHT` (`NOT EXISTS`); không đổi lược đồ, không đổi `expires_at` — cách ít xâm lấn nhất, hộp thư vẫn ẩn theo `expires_at` | `NotificationStore.deleteExpired` | `NotificationFixIT.F2_*` |
| F3 | MAJOR | **ĐÃ SỬA (đỏ→xanh)** | Worker A quá lease ghi `PENDING` (hẹn thử lại) đè lên dòng worker B đã lấy lại; B gửi thành công nhưng không ghi được `SENT` ⇒ gửi lại lần nữa | Hạn khóa cấp lúc nhận (`next_attempt_at` của dòng `IN_FLIGHT`, cắt micro giây) là dấu sở hữu; mọi ghi kết quả/hẹn lại phải khớp `status = 'IN_FLIGHT' AND next_attempt_at = :lease` — đúng mẫu `OutboxStore.OWNED`; mất lease ⇒ không ghi, metric `result="lease_lost"`. Không cần cột mới | `NotificationStore.claim/finish`, `DeliveryWorker` | `NotificationFixIT.F3_aWorkerWhoseLease…`, `F3_twoWorkersRacing…` (×20, barrier) |
| F4 | MAJOR | **ĐÃ SỬA (đỏ→xanh)** | 40 ghi chú ≈ 56 KB: toàn bộ ≈ 2,2 MB nằm trong MỘT trang | Kiểm ngân sách (1 MB − vỏ response) TRƯỚC khi thêm từng bản ghi; dừng trước bản ghi vượt trần; `next` = bản ghi cuối đã trả; cỡ lô nạp thích nghi theo kích thước trung bình (không nạp 200 ghi chú lớn rồi bỏ) | `SyncSnapshotService.page` | `SnapshotFixIT.F4_*` (mọi trang ≤ 1.000.000 byte, ≥ 3 trang, không mất/lặp) |
| F5 | MAJOR | **ĐÃ SỬA (đỏ→xanh)** | 6 POST đồng thời: 20/20 lượt lặp đều tạo hơn một yêu cầu | Khóa dòng `users` (`FOR NO KEY UPDATE`) trước khi kiểm/chèn ⇒ "1 đang chạy" và "2 lần/24 giờ" nguyên tử; thêm chỉ mục UNIQUE `ux_data_exports_user_active` (V13) làm lớp bảo vệ ở DB | `DataExportService.request`, `AccountStore.lockUserForExport`, `V13__backend_fixes.sql` | `ExportPurgeFixIT.F5_concurrent…` (×20, barrier 6 luồng × 3 đợt), `F5_theDatabaseAllows…`, `MigrationUpgradeIT.backendFixMigration…` |
| F6 | MAJOR | **ĐÃ SỬA (đỏ→xanh)** | Tài khoản lưu trữ: định kỳ vẫn `ACTIVE`, `nextRunOn` tiến dần, không giao dịch, không thông báo | (1) Lưu trữ tài khoản (REST và sync) tạm dừng mọi định kỳ `ACTIVE` của nó trong cùng transaction; (2) job gặp tài khoản không ghi được ⇒ `PAUSED`, KHÔNG tiến `nextRunOn`; (3) cả hai phát outbox `RECURRING_PAUSED` → thông báo FINANCE `finance.recurring.paused`; (4) không "tiếp tục" được khi tài khoản còn lưu trữ | `RecurringPauser` (mới), `AccountCommandService`, `RecurringRunService`, `RecurringCommandService`, `RecurringTransactionRepository`, `RecurringPausedHandler` (mới), mẫu `FINANCE.{vi,en}.properties` | `FinanceFixIT.F6_*` (2: archive 10 ngày → unarchive → resume, có thông báo trong hộp thư; lưới an toàn của job) |
| F7 | MINOR | **ĐÃ SỬA (đỏ→xanh)** | 200.001 Task hoàn thành trong kỳ: 200 OK với `"completed":200000` | Đọc `MAX_ROWS + 1`; vượt ⇒ 422 `ANALYTICS_RANGE_TOO_LARGE` (chọn "trả lỗi" thay vì cờ `truncated`: không đổi schema response, mã lỗi đã có trong hợp đồng) | `TaskStatsApiImpl`, `TaskStatsApi` | `AnalyticsTruncationIT.F7_*` (đúng 200.000 vẫn 200; 200.001 ⇒ 422) |
| F8 | MINOR | **ĐÃ SỬA (đỏ→xanh)** | Sau `purge`, sự kiện `BUDGET_THRESHOLD` PENDING mang `userId` trong payload vẫn còn | Cột `outbox_events.user_id` (V13, backfill từ aggregate `user`/payload); `OutboxWriter` tự điền; `outbox_events` vào `UserDataCatalog` nên purge xóa theo `user_id` (không xuất trong tệp xuất dữ liệu) | `V13__backend_fixes.sql`, `OutboxWriter`, `UserDataCatalog`, `UserDataPurger` | `ExportPurgeFixIT.F8_*` (2), `MigrationUpgradeIT.backendFixMigration…` |
| F9 | — | **ĐÃ SỬA (đỏ→xanh)** | Token cấp bởi instance đang chạy bị instance mới của `SyncSnapshotService` từ chối (`VALIDATION_FAILED snapshotToken`) | Khóa đọc từ `lifeos.sync.snapshot-hmac-secret` = `${SNAPSHOT_HMAC_SECRET}` (≥ 32 ký tự); `application.yml` không có mặc định ⇒ thiếu ở production thì không khởi động; dev/test có giá trị riêng ghi rõ | `SyncSnapshotService`, `application.yml`, `application-dev.yml`, `application-test.yml` | `SnapshotFixIT.F9_*`, `SnapshotSecretConfigTest` (2) |

Kèm theo (OQ3, người dùng duyệt làm trong đợt này): gauge `lifeos.job.last_success{job}` cho `recurring`, `retention`,
`purge`, `export` — `JobHealth` (mới, `common.config`), nối vào `RecurringTransactionJob`, `SyncJobs.retention`,
`AccountDeletionService.purgeDue`, `DataExportService.runPending`; test `JobHealthIT` (2).

## 2. Migration

`V13__backend_fixes.sql` (prompt gốc gọi là V12; V12 đã thuộc module ai). V1–V12 không đổi. Nội dung: (F5) gộp yêu cầu
xuất đang chạy bị trùng — giữ yêu cầu sớm nhất, phần còn lại `FAILED`/`DUPLICATE` — rồi tạo chỉ mục UNIQUE một yêu cầu
`PENDING`/`RUNNING` mỗi user; (F8) thêm `outbox_events.user_id`, backfill, chỉ mục riêng phần. F3 và F2 không cần đổi
lược đồ. Kiểm bằng `MigrationUpgradeIT.backendFixMigrationDedupesActiveExportsAndBackfillsOutboxOwners` (nâng từ V12 có
dữ liệu) và `FlywayMigrationIT`.

## 3. Test hiện có đã sửa (khai báo đầy đủ)

- `RecurringIT.Fin_10…`: khẳng định cuối (`nextRunOn` của định kỳ trên tài khoản lưu trữ = hôm nay + 1) mã hóa đúng hành
  vi "bỏ lặng, chỉ tiến con trỏ" mà người dùng đã bác ở F6 ⇒ đổi thành `nextRunOn` giữ nguyên và trạng thái `PAUSED`.
  Đây là thay đổi bắt buộc theo quyết định, không phải nới test. Không test nào bị xóa hay `@Disabled`.
- `ApiDocsProdProfileIT`: cấp thêm `lifeos.sync.snapshot-hmac-secret` (profile prod nay bắt buộc secret này).
- `RecordingPushSender` (hỗ trợ test): thêm hook `onSend` để dựng kịch bản hai worker cho F3.
- `MigrationUpgradeIT`: thêm một test (không sửa test cũ).

## 4. Thay đổi hợp đồng / OpenAPI / tài liệu

- **OpenAPI:** `docs/api/openapi.yaml` KHÔNG đổi — `OpenApiExportIT` xanh sau khi sửa (không endpoint/schema mới; hai mã
  lỗi dùng lại đã có trong hợp đồng). Hành vi mới theo mã lỗi được ghi ở bảng lỗi.
- `docs/engineering/API_CONVENTIONS.md` §5.1: mở rộng nghĩa `FINANCE_CURRENCY_MISMATCH` (F1) và
  `ANALYTICS_RANGE_TOO_LARGE` (F7).
- `docs/engineering/SYNC_PROTOCOL.md`: ghi chú phân trang snapshot theo trần 1 MB và secret ký token (F4, F9); bảng
  payload ghi `FINANCE_CURRENCY_MISMATCH` cho UPDATE đổi `accountId`.
- `docs/engineering/JAVA_SPRING_RULES.md`: module `analytics` thay `dashboard`/`lifereview` (đã duyệt).
- `docs/phases/PHASE_2…7`: ghi "Cập nhật 2026-11-15 theo BACKEND_FIX_REPORT" tại D103, §7 (D110), D121, D135, D137,
  D151, D174, D198, D204, D205; giới hạn mềm UX tại D237 (200 habit / 50 routine) và mục ghim 10 ghi chú (Phase 6).
- `docs/review/BACKEND_PROGRESS.md`: đổi nhãn theo quyết định 2026-11-15 và thêm mục "Đợt sửa backend 1".
- `backend/README.md`: biến `SNAPSHOT_HMAC_SECRET`, `NOTIFICATION_UNSUBSCRIBE_SECRET`, ghi chú `TZ=UTC` (OQ6).

## 5. Chạm ngoài ranh giới (khai rõ theo yêu cầu)

- **`deploy/` — bắt buộc do F9:** compose production/staging truyền biến môi trường tường minh, nên thiếu
  `SNAPSHOT_HMAC_SECRET` thì backend profile prod không khởi động. Đã thêm ĐÚNG một dòng vào mỗi file:
  `deploy/compose.production.yml`, `deploy/compose.staging.yml` (`SNAPSHOT_HMAC_SECRET: ${SNAPSHOT_HMAC_SECRET:?…}`),
  `deploy/.env.production.example`, `deploy/.env.staging.example`. KHÔNG sửa `deploy/scripts/preflight.*`,
  `deploy/README.md`, `deploy/render/` — agent deploy nên bổ sung kiểm độ dài/khác khóa và tài liệu xoay khóa.
- **Module `notification` và `sync`, `identity`, `task`, `common`:** nằm trong `backend/`, chạm theo đúng F2/F3/F4/F5/F7/F8/F9.
- **Không chạm:** `mobile/`, `ops/`, `docs/ops/`, module `ai`, V1–V12, `ci/`.

## 6. Việc CHƯA làm và lý do

- **`AGENTS.md` (danh sách module §3, dòng trạng thái, số test §5):** lệnh sửa bị lớp kiểm quyền tự động của phiên làm
  việc từ chối (file điều phối agent). Tôi không tìm cách vòng qua. Đề nghị người dùng tự sửa: §3 thay
  `dashboard, lifereview` bằng `analytics`; §5 cập nhật số test ở mục 7 dưới đây; thêm dòng "backend fix đợt 1 — xem
  `docs/review/BACKEND_FIX_REPORT.md`"; ghi chú migration đợt sửa là V13 (V12 thuộc ai).
- **`docs/architecture/*` (`DATABASE_DESIGN.md` §4.5 chưa có dòng V13; `SYSTEM_ARCHITECTURE.md` §3.14 chưa liệt kê
  `SNAPSHOT_HMAC_SECRET`):** thư mục được bảo vệ (AGENTS §1.2) và không nằm trong phạm vi prompt — cần người dùng cho phép.
- **`docs/ops/ALERTS.md`, `ops/prometheus/rules/lifeos-future-alerts.yml`:** ngoài ranh giới. Producer metric đã có nên
  DWP6 có thể bật rule `LifeOSJobStale` (tên Prometheus: `lifeos_job_last_success_seconds`,
  `lifeos_job_expected_interval_seconds`).
- **Mobile:** chưa đối chiếu. Hai thay đổi hành vi client cần biết: (a) PATCH đổi tài khoản khác tiền tệ nay 422/REJECTED
  `FINANCE_CURRENCY_MISMATCH`; (b) lưu trữ tài khoản làm đổi `status` các định kỳ liên quan (đến qua pull với
  `mutation_id` NULL) và có thông báo `finance.recurring.paused` (data `type=recurring`).

## 7. Kết quả `mvn clean verify`

Lệnh (trong `backend/`, Java 21, Maven 3.9.16 chế độ offline `-o` vì máy không ra mạng, Docker cho Testcontainers):
`mvn -B -ntp -o clean verify`. **3 lượt liên tiếp xanh, 169 unit + 767 IT = 936 test** (trước đợt sửa: 167 + 712 = 879;
tăng 57). Nguyên văn phần tổng kết của từng lượt:

```text
== lượt 1
[INFO] Tests run: 169, Failures: 0, Errors: 0, Skipped: 0
[INFO] Tests run: 767, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS
[INFO] Total time:  08:05 min
[INFO] Finished at: 2026-11-15T03:47:38+07:00
== lượt 2
[INFO] Tests run: 169, Failures: 0, Errors: 0, Skipped: 0
[INFO] Tests run: 767, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS
[INFO] Total time:  06:55 min
[INFO] Finished at: 2026-11-15T03:54:38+07:00
== lượt 3
[INFO] Tests run: 169, Failures: 0, Errors: 0, Skipped: 0
[INFO] Tests run: 767, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS
[INFO] Total time:  08:04 min
[INFO] Finished at: 2026-11-15T04:02:45+07:00
```

**Các lượt đầy đủ KHÔNG xanh trước ba lượt trên (khai đủ, không giấu):**

1. Lượt thử đầu (12:22): `Tests run: 767, Failures: 1, Errors: 1`.
   - `FinanceFixIT.F6_archiving…`: lỗi của chính test mới — sau khi tiến đồng hồ test 10 ngày, access token (15 phút) hết
     hạn ⇒ 401. Sửa test (cấp token mới), không liên quan mã sản phẩm.
   - `MailpitDeliveryIT`: email đặt lại mật khẩu không tới trong 30 giây; log worker: `PASSWORD_RESET … outcome=retry,
     error=MailSendException` ở lần gửi đầu (SMTP tới container Mailpit lỗi tạm thời, lần thử lại theo backoff 30 giây
     nằm ngoài thời gian chờ của test). Không tái diễn trong 6 lượt đầy đủ sau đó. Test này từng được ghi là chập chờn ở
     WP8; tôi KHÔNG chứng minh được nó không liên quan đợt sửa, chỉ không thấy cơ chế nào liên quan.
2. Hai lượt xanh (09:09, 08:41), rồi lượt thứ ba đỏ: `ExportPurgeFixIT.F5_concurrent…` lần lặp 16 — một trong 6 request
   đồng thời nhận **503** (`Lock not acquired`, hết `lock_timeout` 5 giây) thay vì 200; các khẳng định giới hạn cứng
   (đúng một yêu cầu được tạo) không bị vi phạm. Chạy riêng lớp test 4 lần liền (240 lần lặp) không tái hiện. Giả
   thuyết của tôi (CHƯA chứng minh — container PostgreSQL của lượt đó đã bị dọn nên không còn log): transaction đang giữ
   khóa dòng `users` bị khựng I/O hơn 5 giây lúc commit trên Docker Desktop. Xử lý: test thử lại request khi nhận 503
   (đúng hợp đồng D87 — 503 + `Retry-After` là lỗi tạm thời, client thử lại), giữ nguyên mọi khẳng định. Sau thay đổi
   này mới chạy lại từ đầu ba lượt ở trên. **Điểm cần người xem:** nếu 503 này lặp lại trên CI thì phải điều tra lại
   cách khóa của F5 thay vì coi là môi trường.

## 8. Quyết định phát sinh trong đợt này — `[PROPOSED]`, chưa được duyệt

1. F6: thông báo `finance.recurring.paused` phát cả khi người dùng tự lưu trữ tài khoản (không chỉ khi job tự tạm dừng);
   mỗi định kỳ một thông báo.
2. F6: lưu trữ tài khoản khóa định kỳ TRƯỚC tài khoản (giữ thứ tự khóa "định kỳ → tài khoản"); định kỳ tạo/chuyển vào
   tài khoản đúng lúc đang lưu trữ có thể lọt và được job tạm dừng ở lượt kế tiếp.
3. F6: "tiếp tục" định kỳ khi tài khoản còn lưu trữ ⇒ 422 `FINANCE_ACCOUNT_ARCHIVED`. Mở lại tài khoản không tự tiếp tục.
4. F6: giữ quy tắc tiếp tục hiện có (không bù lần lỡ khi đang `PAUSED`) — các lần trong thời gian lưu trữ không được
   sinh bù; người dùng được báo thay vì mất lặng. Nếu muốn sinh bù khi tiếp tục thì cần quyết định riêng.
5. F7: chọn trả lỗi thay vì cờ `truncated`; lỗi áp cả review/dashboard (cùng nguồn `TaskStatsApi`).
6. F3: dùng `next_attempt_at` làm dấu lease thay vì thêm cột `claim_token`; thêm giá trị nhãn metric
   `result="lease_lost"`.
7. F4: dành 2.048 byte cho vỏ response và 32 byte + tên loại cho mỗi nhóm; lô nạp đầu 50 id, sau đó thích nghi.
8. F5: yêu cầu trùng lúc migrate chuyển `FAILED`/`DUPLICATE` và vẫn tính vào hạn mức 24 giờ.
9. F8: `OutboxWriter` suy `user_id` từ aggregate `user` hoặc `payload.userId`; không khóa ngoại tới `users`;
   `outbox_events` bị loại khỏi tệp xuất dữ liệu.
10. F9: secret tối thiểu 32 ký tự; dùng trực tiếp chuỗi UTF-8 làm khóa HMAC (không base64 như JWT). Chưa hỗ trợ khóa
    "previous" khi xoay — xoay khóa làm phiên snapshot đang dở (≤ 30 phút) phải tải lại.
11. OQ3: gauge khởi tạo bằng thời điểm khởi động (không phải 0/NaN); thêm gauge phụ `lifeos.job.expected_interval{job}`
    (ngoài yêu cầu, để rule ops dùng được) với chu kỳ mặc định cố định trong mã — đổi cron qua cấu hình không tự đổi theo;
    job `export` ghi nhận thành công khi quét xong hàng đợi kể cả khi một yêu cầu riêng lẻ `FAILED`.
12. Đổi nhãn 4 mục bị giữ lại ở `BACKEND_PROGRESS.md` sang `[CONFIRMED]` theo hành vi sau sửa: tôi hiểu câu "giữ
    [PROPOSED] cho tới khi F5/F6/F7/F9 sửa xong thì chốt theo hành vi sau sửa" là cho phép chốt ngay khi đợt sửa xanh.
    Nếu ý người dùng là tự chốt sau khi xem báo cáo, đổi lại 4 ghi chú đó thành `[PROPOSED]`.

## 9. Điểm cần người xem kỹ

- `AccountCommandService.writeExisting` (thứ tự khóa mới khi lưu trữ) và `RecurringRunService.runOne` (nhánh tạm dừng).
- `NotificationStore.finish` — so khớp `timestamptz` chính xác tới micro giây; `claim` cắt micro trước khi ghi.
- `SyncSnapshotService.page` — vòng lặp phân trang viết lại; hợp đồng `next` không đổi.
- `V13__backend_fixes.sql` — câu UPDATE gộp yêu cầu xuất trùng và backfill chạy trên dữ liệu thật khi deploy.
- `UserDataCatalog`: `outbox_events` nay bị xóa theo `user_id` khi xóa tài khoản, gồm cả email đang chờ của người đó.
