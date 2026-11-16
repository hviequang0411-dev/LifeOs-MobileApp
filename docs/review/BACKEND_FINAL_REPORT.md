# LifeOS — Báo cáo cuối hoàn thiện backend (BACKEND_MASTER_PLAN WP1–WP8)

> **Trạng thái:** hoàn thành — chờ người dùng xem xét (2026-11-07).
> **Phạm vi:** toàn bộ phần server của Phase 2–7 theo `docs/engineering/BACKEND_MASTER_PLAN.md` (đã duyệt 2026-11-07).
> **Nhật ký chi tiết từng WP** (thay đổi, ánh xạ test ID, test cũ đã sửa, đủ danh sách quyết định `[PROPOSED]`):
> `docs/review/BACKEND_PROGRESS.md`. Báo cáo này là bản tổng hợp; khi cần chi tiết, mục tương ứng ở nhật ký là nguồn.

---

## 1. Tổng quan

- Backend Spring Boot modular monolith nay gồm 11 module nghiệp vụ/hạ tầng + `common`: `identity`, `project`, `task`,
  `habit`, `routine`, `finance`, `calendar`, `notification`, `goal`, `note`, `analytics`, `sync`.
- 7 migration mới (V5–V11), **29 `entityType` đồng bộ** (4 của Phase 1 + 25 mới), retention/snapshot/chẩn đoán đồng bộ,
  thông báo in-app/push/email qua outbox, analytics/review/dashboard, xuất dữ liệu và xóa tài khoản.
- `mvn -f backend/pom.xml clean verify` xanh **3 lượt liên tiếp**: 847 test (153 unit + 694 IT), 0 bỏ qua,
  0 `@Disabled`; ArchUnit A1–A14 xanh.
- Benchmark trong ngưỡng D173/D207 ⇒ **không cần Redis** (giữ lớp `AggregateCache` no-op theo §4).
- Không có điểm dừng §6 nào phát sinh (không mâu thuẫn tài liệu chưa giải quyết, không đổi hợp đồng ngoài §5, không
  test bắt buộc đỏ quá 3 lần sửa). Một lần Docker Desktop tắt giữa phiên (máy khởi động lại) — đã bật lại, không ảnh
  hưởng kết quả.

## 2. Bảng WP

| WP | Nội dung | Test (unit + IT) | `verify` |
|---|---|---|---|
| WP1 | Giai đoạn 4 (outbox, email, job dọn, docker) — xong trước kế hoạch | 83 + 442 = 525 | 3:24 |
| WP2 | Phase 2 Habit & Routine; V5; 6 entity sync | 94 + 525 = 619 | 3:07 |
| WP3 | Phase 3 Finance; V6; 8 entity sync; ArchUnit A14 | 101 + 591 = 692 | 4:21 |
| WP4 | Phase 4 Calendar & Notification (server); V7, V8; 4 entity sync; email chuyển sang `notification` | 104 + 633 = 737 | 4:29 |
| WP5 | Phase 5 server: retention, 410 full resync, snapshot, chẩn đoán, nhất quán; V9 | 104 + 643 = 747 | 4:38 |
| WP6 | Phase 6 Knowledge & Goals; V10 (unaccent); 6 entity sync; `task` + goalId/milestoneId; note 192 KB | 126 + 681 = 807 | 5:24 |
| WP7 | Phase 7 analytics/review/dashboard, xuất dữ liệu, xóa tài khoản; V11; cache no-op | 153 + 693 = 846 | 6:09 |
| WP8 | Fixture hợp đồng, benchmark, rà soát chéo, tài liệu, báo cáo | 153 + 694 = 847 | 6:03 · 6:47 · 7:02 |

**3 lượt `clean verify` cuối (§8.1):** sau bản sửa scheduler (§8 mục 0): lượt 1 — 847/847, 6:03; lượt 2 — 847/847, 6:47; lượt 3 — 847/847, 7:02 (0 lỗi, 0 bỏ qua). Hai đợt chạy trước bản sửa: mỗi đợt lượt 1 đỏ 1 test (`MailpitDeliveryIT`), lượt 2–3 xanh.

Lớp test chậm nhất (lượt 1): `AnalyticsIT` 31,9 s, `FinanceQueryPlanIT` 28,4 s (100k giao dịch), `AnalyticsQueryPlanIT` 21,4 s (100k Task + 100k giao dịch), `MailpitDeliveryIT` 20,8 s (Spring context + scheduler + SMTP thật), `RegistrationAtomicityIT` 12,4 s.

## 3. Migration

| File | Nội dung chính |
|---|---|
| `V5__habit_routine.sql` | `habits`, `habit_checkins`, `routines`, `routine_steps`, `routine_logs`, `routine_step_logs` |
| `V6__finance.sql` | 9 bảng Finance (`NUMERIC(19,4)` + currency, `budget_alerts` không đồng bộ) |
| `V7__calendar_reminder.sql` | `calendar_events`, `event_exceptions`, `time_blocks`, `reminders`; 3 cột thông báo ở `user_settings`; chuyển `reminder_time` habit/routine thành reminder |
| `V8__notification.sql` | `notifications`, `notification_deliveries`, `notification_preferences`, `device_push_tokens` |
| `V9__sync_advanced.sql` | `min_retained_seq`, `sync_mutations.device_id`, `sync_diagnostics`, chỉ mục retention/tombstone |
| `V10__knowledge_goals.sql` | `unaccent` + `lifeos_unaccent`; `notes` (FTS GIN), `tags`, `note_tags`, `note_links`, `goals`, `milestones`; `tasks.goal_id/milestone_id` |
| `V11__analytics_account.sql` | `reviews`, `data_exports`, `account_deletion_codes`, `account_deletion_audit`, `users.deletion_scheduled_at`, `ix_tasks_user_completed` |

Số V trong đặc tả phase lệch 1 so với file thật (V5 của đặc tả Finance = V6 …) — đặc tả chỉ minh họa (§3.4 kế hoạch).
Mọi migration chạy sạch từ DB rỗng và từ DB có dữ liệu cũ (`FlywayMigrationIT`, `MigrationUpgradeIT`, 3 kịch bản nâng cấp).

## 4. Endpoint và entity đồng bộ mới

- **REST** (danh mục đầy đủ: `API_CONVENTIONS.md` §8.8–§8.12, hợp đồng máy đọc: `docs/api/openapi.yaml`):
  Habit/Routine (§8.8), Finance (§8.9), Calendar/Reminder/Notification (§8.10), Sync snapshot/entity/diagnostics (§8.7),
  Notes/Tags/Goals/Milestones (§8.11), Dashboard/Reviews/Analytics/Account export & deletion (§8.12).
- **Entity đồng bộ mới (25):** `habit`, `habit_checkin`, `routine`, `routine_step`, `routine_log`, `routine_step_log`,
  `financial_account`, `financial_category`, `financial_transaction`, `account_transfer`, `budget`,
  `recurring_transaction`, `saving_goal`, `saving_contribution`, `calendar_event`, `event_exception`, `time_block`,
  `reminder`, `note`, `tag`, `note_tag`, `note_link`, `goal`, `milestone`, `review` — payload `SYNC_PROTOCOL.md` §3.5–§3.9,
  `data` §6.1, chiến lược xung đột `SYNC_RULES.md` §9.5.
- **Fixture hợp đồng Java–Dart** (`docs/api/fixtures/`): auth, finance (WP3), `task`, `goal`, `goal-detail`, `note`,
  `tag`, `review`, `calendar-event`, `reminder` (WP8, `ApiFixtureShapesIT`); vector `habit-vectors.json`,
  `recurrence-vectors.json`, `analytics-vectors.json`.

## 5. Ánh xạ test ID → lớp test

Bảng đầy đủ theo từng WP ở `BACKEND_PROGRESS.md` (mục "Ánh xạ test ID" của WP2–WP7). Tóm tắt độ phủ:

| Đặc tả | Test ID | Phía server | Ghi chú |
|---|---|---|---|
| PHASE_2 | HAB-01..HAB-12, RTN-01..RTN-06 | Đủ | `HabitIT`, `HabitStatisticsPropertyTest`, `HabitVectorsTest`, `RoutineIT`, `HabitSyncIT`, `RoutineSyncIT`, … |
| PHASE_3 | FIN-01..FIN-23 | Đủ | `FinanceTransactionIT`, `RecurringIT`, `FinanceSyncIT`, `FinanceQueryPlanIT`, `MoneyTest`, … |
| PHASE_4 | CAL-01..CAL-07, NTF-01..NTF-14 | Đủ phần server | 4e (lập lịch local Flutter) ngoài phạm vi |
| PHASE_5 | SYN-A01..A20 | A01, A05–A08, A10, A14, A16, A17, A19, A20 + A18 (benchmark WP8) | A02–A04, A09, A11–A13, A15: client |
| PHASE_6 | KNW-01..KNW-17 | Đủ trừ KNW-05 | KNW-05 (đối chiếu FTS5) thuộc client |
| PHASE_7 | INT-01..INT-19 | Đủ trừ INT-07/08 (Redis hoãn), INT-10 phần Dart | INT-13 bằng `BackendBenchmark` |

## 6. Thay đổi hợp đồng đã thực hiện (§5) và test cũ đã sửa

| §5 | Thay đổi | WP | Trạng thái |
|---|---|---|---|
| 1 | `user_settings` + `quietStart`, `quietEnd`, `showDetailsOnLockScreen` | 4 | Xong (`SettingsIT`, `SyncSettingsIT`, `SyncSnapshotContractIT` cập nhật tập khóa) |
| 2 | `task` + `goalId`, `milestoneId` | 6 | Xong (`TaskIT`, `SyncPushTaskIT`, `SyncSnapshotContractIT` cập nhật tập khóa) |
| 3 | Payload `note` ≤ 192 KB | 6 | Xong (`SyncEntityHandler.maxPayloadBytes`) |
| 4 | Retention, 410 full resync, snapshot | 5 | Xong |
| 5 | Email handler chuyển sang `notification` | 4 | Xong (hành vi D76 không đổi; test email cũ xanh) |
| 6 | Bảng chiến lược xung đột theo entity | 3, 5, 7 | Xong (`SYNC_RULES.md` §9.5, thêm dòng Phase 6/7 ở WP7) |
| 7 | `unaccent` + `simple` | 6 | Xong (`DATABASE_RULES.md` §8 mục 10) |
| 8 | Quy ước tiền | 3 | Xong (`DATABASE_RULES.md` §8 mục 9, `API_CONVENTIONS.md` §2) |
| 9 | `ALREADY_EXISTS` | 2 | Xong (D235; mở rộng cho thẻ trùng tên, review trùng kỳ) |
| 10 | Thông tin thiết bị khi đăng nhập | — | Đã có từ Phase 1 |

**Test cũ đã sửa** (chỉ khi §5 cho phép hoặc để thêm route/chính sách, không nới): `RegistrationIT`/các test đếm
change_logs đăng ký (15 danh mục Finance mặc định, D113 — WP3); `SettingsIT`, `SyncSettingsIT`, `DeviceIT`,
`AuthorizationMatrixIT`, `ApiDocsProdProfileIT` (WP4); `TaskIT`, `SyncPushTaskIT`, `SyncSnapshotContractIT` (WP6);
`RateLimiterTest` (mỗi WP thêm chính sách, nay 20); `IdorMatrixIT`, `SensitiveLogIT`/`SensitiveLogScenario` (mỗi WP
thêm tài nguyên/luồng). Danh sách và lý do từng test: mục "Test cũ đã sửa" của từng WP trong nhật ký.

## 7. Quyết định phát sinh `[PROPOSED]` (cần người dùng xem)

Tổng cộng 75 mục trong nhật ký (WP2: 17, WP3: 15, WP4: 12, WP5: 8, WP6: 9, WP7: 12, WP8: 2). Những mục có ảnh hưởng hợp đồng
hoặc hành vi người dùng thấy rõ nhất:

1. **Đánh số migration** V6–V11 lệch đặc tả (chỉ minh họa) — mọi WP.
2. **Giới hạn đếm không khóa** (200 habit, 50 routine, ghim 10 ghi chú) có thể vượt 1 khi hai lệnh tạo đồng thời
   (WP2 #15, WP6 #7). Giới hạn có khóa cha (bước routine, milestone, thẻ/liên kết ghi chú) thì chính xác.
3. **Tiền JSON là chuỗi rút gọn** (`"10.5"`); số JSON ở REST → 400 `MALFORMED_REQUEST` (WP3 #2–#3).
4. **Hai danh mục `ADJUSTMENT` + 13 danh mục mặc định tạo lúc đăng ký** (WP3 #4) — đăng ký sinh thêm change_logs.
5. **Push tắt mặc định** (`DisabledPushSender`), bật bằng `FCM_ENABLED` (WP4 #9); một token/thiết bị (WP4 #10).
6. **Khóa HMAC snapshot sinh ngẫu nhiên khi khởi động** (WP5 #4): khởi động lại giữa phiên snapshot ⇒ client làm lại.
7. **Tiến độ Goal thang 0..1, 4 chữ số** kèm `method` và số đếm thật (WP6 #2).
8. **`note_tag`/`note_link` CREATE trùng cặp ⇒ APPLIED version bản có sẵn** — id client không được tạo (WP6 #5).
9. **Tìm kiếm ghi chú phân trang offset ≤ 1000 trong `cursor`** — ghi chú khớp bị sửa giữa hai trang có thể dịch trang
   (WP6 #6, giới hạn của offset đã chọn ở §4).
10. **Một module `analytics`** gom dashboard/review/analytics theo D197 — `AGENTS.md` §3 còn liệt kê `dashboard`,
    `lifereview` (WP7 #2, xem Open Question 2).
11. **Tải tệp xuất qua endpoint có xác thực** thay vì URL ký; quá 5 lượt ⇒ 410 (WP7 #7).
12. **Thêm `POST /account/deletion/code`**; sai mã ⇒ 400 `VALIDATION_FAILED` (`code`) (WP7 #8).

## 8. Lỗi và rủi ro phát hiện

0. **Lỗi đã sửa ở WP8 — scheduler một luồng:** lượt `verify` đầu của WP8 đỏ chập chờn ở `MailpitDeliveryIT` (email tới
   muộn > 30 s). Nguyên nhân gốc: Spring mặc định một luồng cho mọi `@Scheduled`; job xuất dữ liệu (WP7) chạy ngay khi
   khởi động, xử lý yêu cầu `PENDING` (mỗi yêu cầu zip toàn bộ dữ liệu một user) và chiếm luồng duy nhất ⇒ outbox worker
   (~2 s) và reminder bị đói. Ở production lỗi này làm email bảo mật/thông báo trễ trong khi có người xuất dữ liệu.
   Sửa: `spring.task.scheduling.pool.size: 4` (mỗi job vẫn không chạy chồng chính nó), `MailpitDeliveryIT` kiểm pool ≥ 2
   và hoãn dữ liệu chờ do test khác để lại (cô lập). Lần sửa thứ nhất (chỉ cô lập dữ liệu) chưa đủ — ghi nhận theo §6.4
   (2/3 lần sửa).
1. **Cột `time` và `hibernate.jdbc.time_zone=UTC`:** Hibernate dịch `LocalTime` theo múi giờ JVM khi ghi/đọc. Dữ liệu ghi
   và đọc qua ứng dụng nhất quán, nhưng giá trị chèn bằng SQL thô (hoặc so sánh `time` trong SQL) có thể lệch nếu JVM
   không chạy UTC. Giảm thiểu đã áp dụng: thống kê đúng hạn đọc `due_time` qua JPQL. Đề xuất: đặt `TZ=UTC`/
   `-Duser.timezone=UTC` cho image Docker và môi trường test.
2. **Production cần `NOTIFICATION_UNSUBSCRIBE_SECRET`** (≥ 32 ký tự, khác JWT) — thiếu thì không khởi động ở profile prod;
   `docker-compose.yml` hiện dùng profile dev với khóa mặc định.
3. **Một instance:** rate limiter trong bộ nhớ, khóa HMAC snapshot trong bộ nhớ, thư mục xuất dữ liệu cục bộ
   (`EXPORT_DIR`). Scale nhiều instance cần lưu trữ dùng chung và rate limit tập trung.
4. **`TaskStatsApi.completedBetween` chặn 200 000 dòng/kỳ** — vượt thì thống kê bị cắt im lặng (thực tế khó xảy ra với
   giới hạn 24 tháng).
5. **Dashboard chạy 5 thẻ song song/yêu cầu** — với nhiều người dùng đồng thời có thể chiếm nhiều kết nối pool; theo dõi
   `hikaricp_connections_pending` (đã có trong `docs/ops/ALERTS.md`).
6. **Job chạy song song (pool 4 luồng):** job xuất dữ liệu và job xóa tài khoản có thể cùng chạm một user đang được xóa
   cứng — khi đó `data_exports` đã bị xóa, tệp zip vừa tạo thành mồ côi trong `EXPORT_DIR` (không lộ dữ liệu ra API, cần
   dọn thư mục định kỳ). Hiếm: chỉ khi yêu cầu xuất được giành ngay trước thời điểm xóa cứng.
7. **Xóa cứng tài khoản theo `ctid` từng lô:** đúng thứ tự khóa ngoại hiện tại (kiểm bởi `AccountDeletionIT`); bảng mới
   có khóa ngoại tự tham chiếu sẽ cần xem lại thứ tự.

## 9. Điểm cần người xem kỹ

1. Thứ tự khóa và ghi change_logs của cascade nhiều module (D86): `GoalCommandService.delete` (Task gỡ liên kết → sự
   kiện `GoalDeletionStarted` cho note_links → `recordAll` → milestone → goal), `TaskCommandService.delete` với ba listener
   (time block, reminder, note_link) qua `PendingChanges`. `GoalCascadeConcurrencyIT` (20 lần × 3 lượt) xanh.
2. `GoalApiImpl.resolve` (khóa goal trước milestone, đọc milestone không khóa trước) và `TaskCommandService.relink`.
3. `UserDataExporter` ghép JSON thủ công theo luồng (không giữ cả tệp trong bộ nhớ) và `UserDataCatalog.EXPORT_EXCLUDED`
   (bảng/cột nào **không** được xuất).
4. `AccountDeletionGuardFilter` truy vấn DB mỗi request có xác thực (khóa chính `users`) — chấp nhận được ở quy mô hiện tại.
5. `NoteSearchStore`: tsquery chỉ qua `websearch_to_tsquery` tham số hóa; biểu thức nhúng thẳng để dùng GIN (KNW-17).
6. Các quyết định `[PROPOSED]` ở §7 và nhật ký.

## 10. Việc hoãn / chưa làm (kèm lý do)

| Mục | Lý do |
|---|---|
| Redis (PHASE_7 7d, D193–D195, D209; INT-07/08) | §4 mục 7: chỉ thêm khi benchmark vượt ngưỡng — benchmark đạt ngưỡng |
| Toàn bộ client Flutter của Phase 2–7 (My Day, lập lịch local, FTS5 + KNW-05, hợp nhất 3 chiều + `merge-vectors.json`, tính offline + vector Dart, SYN-A02..A04/A09/A11..A13/A15) | Ngoài phạm vi backend |
| CI GitHub / job nightly (mô phỏng đa thiết bị đầy đủ `-Dlifeos.sync.simulation.full=true`, `BackendBenchmark`) | `ci/`, `.github/` do người dùng quản lý — đề xuất thêm job nightly |
| FR-GOAL-06 (reminder gắn Goal/Milestone) | Cần thêm `targetType` cho `reminder` — đổi hợp đồng Phase 4 ngoài §5 (Open Question 1) |
| Phase 3: nợ/cho vay, hoàn tiền, quy đổi tiền tệ, nhập CSV | Ngoài phạm vi đặc tả (D118) |
| Phase 4: nguồn phát `RECURRING_DUE`, `HABIT_STREAK_AT_RISK`; bảng `notification_templates` | Handler theo schema D149 sẵn sàng; mẫu dùng file |
| Phase 7: "Task dời" trong Daily Review, Habit theo thứ trong tuần (FR-INT-01/05) | Cần dữ liệu lịch sử đổi hạn / API thống kê theo ngày — chưa có trong mô hình |
| Metric "lần chạy thành công cuối" của job định kỳ | Chưa có; cảnh báo "job không chạy" hiện chỉ đầy đủ cho outbox/reminder (Open Question 3) |

## 11. Số đo hiệu năng (`BackendBenchmark`, máy phát triển, PostgreSQL 17 Testcontainers)

| Kịch bản | Kết quả |
|---|---|
| Sync push 50 mutation (task CREATE), 12 lô | median 596 ms · max 1116 ms |
| Sync pull 500 thay đổi (một trang) | median 136 ms · p95 321 ms · max 321 ms |
| Snapshot 50 000 Task (101 trang, 50000 bản ghi) | 8473 ms tổng |
| GET /dashboard (ngưỡng p95 < 800 ms) | median 48 ms · p95 60 ms · max 316 ms |
| GET /reviews/daily (ngưỡng < 500 ms) | median 67 ms · p95 86 ms · max 155 ms |
| GET /reviews/monthly (ngưỡng < 500 ms) | median 131 ms · p95 165 ms · max 190 ms |
| GET /analytics/tasks 12 tháng theo tuần (ngưỡng < 1 s) | median 311 ms · p95 422 ms · max 422 ms |
| GET /analytics/finance 12 tháng theo tháng (ngưỡng < 1 s) | median 291 ms · p95 360 ms · max 360 ms |
| GET /notes?q=ghi chu 12345 trên 50 000 ghi chú (khớp 1) | median 24 ms · p95 31 ms · max 47 ms |
| Xuất dữ liệu (INT-13): 200k giao dịch + 100k Task + 50k ghi chú | 15367 ms, zip 22 MB, heap chênh ≈ 93 MB |

Đánh giá: dashboard p95 60 ms (ngưỡng 800 ms), review ngày/tháng p95 86/165 ms (500 ms), analytics 12 tháng p95 ≤ 422 ms (1 s) — đạt D207, **không cần Redis/bảng tổng hợp (D191)**. Push 50 mutation ≈ 0,6 s, pull 500 ≈ 0,14 s, snapshot 50 000 bản ghi ≈ 8,5 s (101 trang) — mốc D173. Xuất dữ liệu INT-13: ≈ 15 s, zip 22 MB, heap tăng ≈ 93 MB (đọc theo lô, không giữ cả tệp).

Bằng chứng kế hoạch truy vấn: `FinanceQueryPlanIT` (100k giao dịch), `NoteSearchPlanIT` (20k ghi chú, GIN, ≈ 11 ms),
`AnalyticsQueryPlanIT` (100k Task + 100k giao dịch, `ix_tasks_user_completed`) — không Seq Scan trên bảng lớn.

## 12. Open Questions mới

1. **FR-GOAL-06:** có mở rộng `reminder.targetType` thêm `GOAL`/`MILESTONE` (đổi hợp đồng Phase 4 + `SYNC_PROTOCOL` §3.7)?
2. **`AGENTS.md` §3:** danh sách module ghi `dashboard`, `lifereview`; mã thực tế là một module `analytics` (D197) và đã
   có `goal`, `note`, `calendar`, `notification`. Xác nhận và cho phép sửa §3?
3. **Giám sát job:** thêm gauge `lifeos.job.last_success{job}` cho recurring/retention/purge/export?
4. **CI nightly:** thêm job chạy mô phỏng đa thiết bị đầy đủ và `BackendBenchmark` trong `ci/`?
5. **Kho tệp xuất ở production:** giữ đĩa cục bộ (một instance) hay chuyển object storage khi scale?
6. **Múi giờ JVM:** chốt `TZ=UTC` cho image và test (rủi ro §8.1)?
7. Duyệt (hoặc sửa) 75 quyết định `[PROPOSED]` trong nhật ký để đổi nhãn sang `[CONFIRMED]`.
