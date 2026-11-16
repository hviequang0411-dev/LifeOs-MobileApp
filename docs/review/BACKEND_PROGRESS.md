# Nhật ký tiến độ hoàn thiện backend (BACKEND_MASTER_PLAN.md)

> Nhật ký, không phải điểm chờ duyệt (`BACKEND_MASTER_PLAN.md` §3.2). Mỗi WP: trạng thái, số test (unit + IT), thời gian
> `verify`, thay đổi lớn, quyết định phát sinh `[PROPOSED]`, việc hoãn.
>
> **Duyệt nhãn 2026-11-15 (người dùng, theo `PENDING_DECISIONS.md` §1–§2):** toàn bộ quyết định phát sinh WP2–WP8 dưới
> đây chuyển `[CONFIRMED]` nguyên trạng, TRỪ 4 mục bị vòng review bác — các mục đó được chốt theo hành vi SAU đợt sửa
> F5/F6/F7/F9 (ghi tại từng mục; chi tiết ở `BACKEND_FIX_REPORT.md`). Giới hạn đếm không khóa (200 habit, 50 routine,
> ghim 10 ghi chú) chốt là **giới hạn mềm UX**. Open Questions của `BACKEND_FINAL_REPORT.md` §12: OQ1 (FR-GOAL-06) hoãn;
> OQ3 làm trong đợt sửa này; OQ5, OQ6 đã đóng theo quyết định deploy. Xem mục "Đợt sửa backend 1" cuối tài liệu.

| WP | Trạng thái | Test (unit + IT) | `verify` | Ghi chú |
|---|---|---|---|---|
| WP1 | Xong (trước kế hoạch) | 83 + 442 = 525 | 3:24 | Giai đoạn 4 backend, đã duyệt 2026-11-07 — `docs/review/BACKEND_PHASE4_OUTBOX_REPORT.md` |
| WP2 | Xong 2026-11-07 | 94 + 525 = 619 | 3:07 | Phase 2 Habit & Routine; migration V5; 6 entity sync |
| WP3 | Xong 2026-11-07 | 101 + 591 = 692 | 4:21 | Phase 3 Finance; migration V6; 8 entity sync; ArchUnit A14 |
| WP4 | Xong 2026-11-07 | 104 + 633 = 737 | 4:29 | Phase 4 Calendar & Notification (server); V7, V8; 4 entity sync; email chuyển sang `notification` |
| WP5 | Xong 2026-11-07 | 104 + 643 = 747 | 4:38 | Phase 5 server: retention, 410 full resync, snapshot, chẩn đoán, nhất quán; V9 |
| WP6 | Xong 2026-11-07 | 126 + 681 = 807 | 5:24 | Phase 6 Knowledge & Goals; V10 (unaccent); 6 entity sync; task thêm goalId/milestoneId; note 192 KB |
| WP7 | Xong 2026-11-07 | 153 + 693 = 846 | 6:09 | Phase 7: analytics/review/dashboard, xuất dữ liệu, xóa tài khoản; V11; cache no-op (không Redis) |
| WP8 | Xong 2026-11-07 | 153 + 694 = 847 | 6:03–7:02 (3 lượt) | Fixture, benchmark, sửa scheduler một luồng, tài liệu, báo cáo cuối |

---

## WP1 — Giai đoạn 4: outbox worker, email, job dọn, docker compose

**Trạng thái:** đã hoàn tất và được duyệt trước khi bắt đầu kế hoạch tổng thể (§2: "Bỏ qua nếu Claude Code đã hoàn tất
việc này"). Không làm lại. Chi tiết, quyết định P1–P14: `docs/review/BACKEND_PHASE4_OUTBOX_REPORT.md`.

---

## WP2 — Phase 2 Habit & Routine (H1–H4)

**Trạng thái:** xong. `mvn -f backend/pom.xml clean verify` xanh: 94 unit + 525 IT = 619 test, 0 bỏ qua, 3:07. Các lớp
có test đồng thời (`HabitIT`, `HabitSyncIT`, `RoutineIT`) chạy thêm 2 lượt liên tiếp, 77/77 mỗi lượt.

**Thay đổi lớn**
- Module mới `habit` (Habit, HabitCheckin; `habit.domain`: `HabitSchedule`, `HabitStatistics`, `HabitFields`) và
  `routine` (Routine, RoutineStep, RoutineLog, RoutineStepLog), mỗi module có controller, service, sync handler.
- Migration `V5__habit_routine.sql`: 6 bảng theo PHASE_2 §5; nới CHECK `change_logs.entity_type`.
- `common.time.UserCalendar` (identity cài đặt `UserCalendarImpl`): múi giờ, ngày đầu tuần, "hôm nay" theo user.
- Sync: `MutationOutcome.Conflict` có `reason`, `MutationResult.conflictReason`; `NaturalKeyConflictException` →
  `CONFLICT ALREADY_EXISTS` kèm snapshot bản đang tồn tại (§5 #9, D235) cho check-in, lượt chạy, kết quả bước.
- `common.privacy.UserDataCatalog`: danh mục mọi bảng có `user_id` (thứ tự xóa), kiểm bởi `UserDataCatalogIT`.
- `PayloadFields.longValue`; mã lỗi Phase 2 trong `ErrorCode`; ArchUnit: `habit`, `routine` vào danh sách module,
  A7/A12/A13 phủ module mới (chặt hơn, không nới).
- Tài liệu: `SYNC_PROTOCOL.md` §3.5 (payload 6 entity), §4.1 (`conflictReason`), §6.1 (data); `API_CONVENTIONS.md`
  §5.1 (mã lỗi), §8.7–8.8 (endpoint); `DATABASE_DESIGN.md` (V5); `docs/api/openapi.yaml`; `docs/api/fixtures/habit-vectors.json`.

**Ánh xạ test ID**

| ID | Lớp / phương thức |
|---|---|
| HAB-01 | `HabitIT.Hab_01_…` |
| HAB-02 | `HabitIT.Hab_02_…` (Kiritimati +14, Pago Pago −11, HCM) |
| HAB-03 | `HabitIT.Hab_03_repeatedPutIsIdempotent`, `Hab_03_concurrentPuts…` (×20) |
| HAB-04 | `HabitIT.Hab_04_…` |
| HAB-05 | `HabitVectorsTest.Hab_05_…` + `docs/api/fixtures/habit-vectors.json` (8 ca; phía Dart chạy cùng tệp) |
| HAB-06 | `HabitStatisticsPropertyTest.Hab_06_…` (10 seed × 500 thao tác, mô hình tham chiếu độc lập) |
| HAB-07 | `HabitSyncIT.Hab_07_…` |
| HAB-08 | `HabitSyncIT.Hab_08_…` (×20) |
| HAB-09 | `HabitIT.Hab_09_…` |
| HAB-10 | `HabitIT.Hab_10_…`, `HabitSyncIT.Hab_10_…` |
| HAB-11 | `IdorMatrixIT` (mọi route có biến đường dẫn, gồm `{date}`, `{logId}`, `{stepId}`), `HabitIT.validationAndStatsRangeAndMassAssignment`, `SensitiveLogIT` (`runHabitRoutineFlows`) |
| HAB-12 | `UserDataCatalogIT.Hab_12_…` |
| RTN-01..06 | `RoutineIT.Rtn_01_…` … `Rtn_06_…` (RTN-02 đồng thời ×20) |
| Sync tương đương SYN | `HabitSyncIT`, `RoutineSyncIT` (CONFLICT, REJECTED, DUPLICATE, tombstone, cascade `mutation_id` NULL, tham chiếu 422) |
| Hợp đồng `data` | `SyncSnapshotContractIT.Sync_42_phase2…` |
| Migration | `FlywayMigrationIT.allMigrationsAreApplied`, `MigrationUpgradeIT` (từ V4 có dữ liệu Phase 1) |

**Test cũ đã sửa**
- `FlywayMigrationIT.allFourMigrationsAreApplied` → `allMigrationsAreApplied`: danh sách mong đợi lấy từ tệp
  `db/migration/V*__*.sql` (vẫn đòi đúng thứ tự, không thiếu/thừa); `expectedTablesExist` thêm 6 bảng. Lý do: kế hoạch
  bắt buộc thêm migration (§3.4).
- `SyncPushProjectCategoryIT.Sync_09_…`: ví dụ `entityType` lạ đổi từ `"habit"` (nay hợp lệ) sang `"no_such_type"`;
  hành vi kiểm không đổi.
- `IdorMatrixIT`: tổng quát hóa sang mọi biến đường dẫn (chặt hơn).

**Quyết định phát sinh `[CONFIRMED]`** — duyệt nguyên trạng 2026-11-15 (PENDING_DECISIONS §1); mục có ghi chú riêng thì theo ghi chú
1. `UserCalendar` ở `common` (identity cài đặt) thay vì thêm cặp phụ thuộc module — giữ nguyên A6.
2. Chuỗi/thống kê chỉ xét ngày ≤ hôm nay (dung sai +1 ngày của D231 chỉ áp cho việc nhận check-in).
3. Thống kê chỉ đếm DONE/SKIPPED trên ngày đến lịch; check-in ngoài lịch không vào `done`.
4. TIMES_PER_WEEK: tuần bị cắt bởi `startOn`/`endOn` có `required = max(min(n, số ngày khả dụng) − skipped, 0)`;
   thống kê mỗi tuần `scheduled = min(n, số ngày của tuần trong khoảng)`, tuần đang chạy không tính bỏ lỡ.
5. Ghi check-in khóa dòng Habit FOR UPDATE (các check-in của một habit tuần tự; upsert không đua, không mồ côi).
6. PATCH/UPDATE habit có `scheduleType` ⇒ trường lịch vắng mặt hiểu là null (thay cả bộ); tương tự `kind` của routine.
7. `weekdays` trong API là bitmask số nguyên (giống SQLite).
8. `GET /habits?scheduledOn=` trả mọi kết quả một trang; habit TIMES_PER_WEEK tính là đến lịch mọi ngày trong vòng đời.
9. Check-in vào habit đã lưu trữ được phép.
10. Tỷ lệ routine: `scheduled` tính từ ngày tạo routine (theo múi giờ user); hôm nay chỉ tính khi đã hoàn tất.
11. Vị trí bước trùng → 400 `VALIDATION_FAILED` fieldError `position`.
12. Hoàn tác kết quả bước làm lượt đã hoàn tất trở lại chưa xong (`completedAt` = null).
13. Kết quả bước cho bước đã xóa mềm vẫn ghi được trong lượt dở (D243, D249).
14. Không có mã riêng cho kết quả bước không tồn tại: dùng `ROUTINE_STEP_NOT_FOUND`.
15. Giới hạn 200 habit / 50 routine kiểm bằng đếm, không khóa — hai lệnh tạo đồng thời có thể vượt 1 (chấp nhận).
    **Chốt 2026-11-15: đây là giới hạn mềm UX** (không phải bất biến dữ liệu; không cần sửa code).
16. Ngày lượt chạy routine không bị giới hạn khoảng (D231 chỉ cho habit).
17. Lịch sử: check-in mặc định từ `startOn` tới hôm nay + 1; lượt chạy mặc định 30 ngày gần nhất, ≤ 366 ngày.

**Hoãn / ngoài phạm vi:** My Day phía client (D251, §4 kế hoạch); `reminder_time` giữ trên habit/routine, chuyển sang
`reminders` ở WP4.

---

## WP3 — Phase 3 Personal Finance (3a–3e)

**Trạng thái:** xong. `mvn -f backend/pom.xml clean verify` xanh: 101 unit + 591 IT = 692 test, 0 bỏ qua,
4:21. Các lớp có test đồng thời (`FinanceTransactionIT` FIN-13 ×20, `RecurringIT` FIN-09 ×20) chạy thêm 2 lượt liên
tiếp, 49/49 mỗi lượt.

**Thay đổi lớn**
- Module mới `finance`: 8 entity đồng bộ (`financial_account`, `financial_category`, `financial_transaction`,
  `account_transfer`, `budget`, `recurring_transaction`, `saving_goal`, `saving_contribution`), controller
  `/api/v1/finance/*`, 8 sync handler, `finance.domain` (`Money`, `Recurrence`, `DefaultCategories`).
- Migration `V6__finance.sql` (đặc tả gọi là V5 — V5 đã dùng cho Phase 2): 9 bảng (thêm `budget_alerts` cho ngưỡng
  80/100% một lần mỗi tháng), tiền `NUMERIC(19,4)` + CHECK giới hạn, UNIQUE `(recurring_transaction_id,
  recurrence_date)` gồm cả tombstone, chỉ mục một phần cho số dư/báo cáo/lọc; nới CHECK `change_logs.entity_type`.
- Quy ước tiền (§5 #8): JSON chuỗi, REST từ chối số JSON (`MoneyText` → 400 `MALFORMED_REQUEST`), sync từ chối
  (`VALIDATION_FAILED`); SCALE theo tiền tệ, không làm tròn; ArchUnit **A14** cấm `double/float` trong `finance`.
- Số dư luôn tính (D102) bằng SQL tham số hóa (`FinanceJdbc`); ngân sách, báo cáo tháng/dòng tiền loại trừ chuyển
  khoản, tombstone và danh mục `ADJUSTMENT` (D119).
- Ngưỡng ngân sách (D116, FIN-13): khóa ngân sách bị ảnh hưởng FOR UPDATE theo id, `budget_alerts` ON CONFLICT DO
  NOTHING, `outbox_events` `BUDGET_THRESHOLD` payload D149 (không số tiền/tên).
- Giao dịch định kỳ (D110): `RecurringRunService` (mỗi lần xuất hiện một transaction, FOR UPDATE SKIP LOCKED, INSERT …
  ON CONFLICT DO NOTHING, bù ≤ 31), `RecurringTransactionJob` mỗi giờ (`lifeos.finance.recurring-cron`).
- Danh mục mặc định (D113): `common.provisioning.UserProvisioner` (đảo phụ thuộc như ChangeRecorder);
  `RegistrationService` gọi trong cùng transaction (ghi nghiệp vụ trước, change_logs sau — D86);
  `UserProvisioningService.backfill()` bù khi khởi động (`UserProvisioningBackfillJob`, tắt khi
  `lifeos.scheduling.enabled=false`).
- `UserDataCatalog` thêm 9 bảng; `IdorMatrixIT` nhận diện route `/finance/<tài nguyên>`.
- Tài liệu: `SYNC_RULES.md` §9.5 (§5 #6), `DATABASE_RULES.md` §8 mục 9, `API_CONVENTIONS.md` §2 (tiền), §5.1 (mã
  `FINANCE_*`), §8.9; `FLUTTER_RULES.md` §12.2 mục 11 (SQLite ×10⁴); `SYNC_PROTOCOL.md` §3.6, §6.1;
  `DATABASE_DESIGN.md` (V6); `docs/api/openapi.yaml`; fixture `finance-transaction.request/response.json`,
  `finance-account.response.json`.

**Ánh xạ test ID**

| ID | Lớp / phương thức |
|---|---|
| FIN-01 | `MoneyTest.Fin_01_…`, `FinanceAccountIT.Fin_01_…`, ArchUnit A14 (`ArchitectureTest.a14`) |
| FIN-02 | `MoneyTest.Fin_02_…`, `FinanceAccountIT.Fin_02_…` |
| FIN-03 | `FinanceAccountIT.Fin_03_…` (500 thao tác ngẫu nhiên, mô hình tham chiếu độc lập, 3 mốc asOf) |
| FIN-04 | `FinanceTransactionIT.Fin_04_…` |
| FIN-05..08 | `FinanceAccountIT.Fin_05_…` … `Fin_08_…`; sync: `FinanceSyncIT.Fin_19_…` |
| FIN-09 | `RecurringIT.Fin_09_runningTheJobTwice…`, `Fin_09_parallelRuns…` (×20) |
| FIN-10 | `RecurrenceTest.Fin_10_…` (4 test, gồm so với duyệt tuần tự 2.000 ca), `RecurringIT.Fin_10_…` |
| FIN-11 | `RecurringIT.Fin_11_…` |
| FIN-12 | `FinanceTransactionIT.Fin_12_…` (UTC+14) |
| FIN-13 | `FinanceTransactionIT.Fin_13_thresholdsFire…`, `Fin_13_concurrentExpenses…` (×20) |
| FIN-14 | `FinanceTransactionIT.Fin_14_…` |
| FIN-15 | `FinanceAccountIT.Fin_15_…` |
| FIN-16 | `FinanceTransactionIT.Fin_16_…` |
| FIN-17 | `FinanceTransactionIT.Fin_17_…` |
| FIN-18 | `FinanceSyncIT.Fin_18_…` |
| FIN-19 | `FinanceSyncIT.Fin_19_…` |
| FIN-20 | `FinanceSyncIT.Fin_20_…`, `IdorMatrixIT` (mọi route finance có biến đường dẫn) |
| FIN-21 | `SensitiveLogIT` (`runFinanceFlows`) |
| FIN-22 | `FinanceAccountIT.Fin_22_…`, `RegistrationConcurrencyIT` |
| FIN-23 | `FinanceQueryPlanIT.Fin_23_…` (100.000 giao dịch giữa 300.000 của 30 user khác; kế hoạch ghi `target/explain-finance.txt`) |
| Sync tương đương SYN | `FinanceSyncIT` (CONFLICT, REJECTED, DUPLICATE, tombstone, cascade mục tiêu → khoản góp `mutation_id` NULL, vòng CREATE/UPDATE/DELETE của 8 loại) |
| Hợp đồng `data` | `SyncSnapshotContractIT.Sync_42_phase3…`; fixture: `FinanceFixtureContractIT` |
| Migration | `FlywayMigrationIT`, `UserDataCatalogIT`, `MigrationUpgradeIT` |

**Test cũ đã sửa**
- `RegistrationIT.registerCreatesUser…` và `RegistrationConcurrencyIT`: trước đây khẳng định đăng ký ghi đúng 1 dòng
  change_logs; D113 (đã duyệt) thêm 15 danh mục mặc định. Khẳng định user_settings giữ nguyên (lọc theo
  `entity_type`, vẫn seq 1) và thêm khẳng định 15 dòng `financial_category` CREATE `mutation_id` NULL, tổng 16, đúng
  một bộ danh mục khi đăng ký đồng thời (chặt hơn).
- `FlywayMigrationIT.expectedTablesExist`: thêm 9 bảng.

**Quyết định phát sinh `[CONFIRMED]`** — duyệt nguyên trạng 2026-11-15 (PENDING_DECISIONS §1); mục có ghi chú riêng thì theo ghi chú
1. Migration đánh số V6 (đặc tả ghi V5).
2. JSON tiền bỏ số 0 vô nghĩa (`"10.5"`); đầu vào `"10.00"` VND được nhận (số 0 cuối không phải chữ số có nghĩa).
3. Số JSON cho trường tiền ở REST → 400 `MALFORMED_REQUEST` (lỗi đọc JSON), ở sync → `VALIDATION_FAILED`.
4. Hai danh mục `ADJUSTMENT` tạo sẵn lúc đăng ký (không đợi lúc đối soát) để client offline có ngay; job bù bỏ qua mục
   trùng tên người dùng đã có.
5. Ngưỡng ngân sách chỉ xét khoản chi thuộc tháng hiện tại theo múi giờ người dùng; chỉ khi tạo/sửa khoản chi (không
   khi hạ hạn mức ngân sách). `RECURRING_DUE` không phát ở WP3 (nhắc trước hạn thuộc `reminders`, WP4, theo §7).
6. `ratio` (ngân sách, mục tiêu) = 4 chữ số thập phân HALF_UP (tỷ lệ hiển thị, không phải tiền).
7. Danh mục lưu trữ vẫn dùng được cho giao dịch mới (lưu trữ chỉ ẩn khỏi lựa chọn ở UI).
8. "Đang được dùng" của tài khoản/danh mục tính bản ghi CHƯA xóa: giao dịch, chuyển khoản, định kỳ (tài khoản);
   giao dịch, ngân sách, định kỳ (danh mục).
9. Sửa giao dịch của tài khoản đã lưu trữ (không đổi tài khoản) được phép; chuyển sang tài khoản lưu trữ bị chặn.
10. Định kỳ: đổi lịch → `nextRunOn` = lần đầu của lịch mới ≥ max(startOn, nextRunOn cũ); ~~tài khoản lưu trữ/mất → bỏ
    qua lần xuất hiện (chỉ tiến `nextRunOn`)~~ — **phần này KHÔNG duyệt (người dùng bác); thay bằng hành vi sau đợt sửa
    F6, `[CONFIRMED]` 2026-11-15:** lưu trữ tài khoản tạm dừng các định kỳ ACTIVE của nó; job gặp tài khoản không ghi
    được thì chuyển định kỳ sang `PAUSED`, phát thông báo `finance.recurring.paused` và KHÔNG tiến `nextRunOn`; `startOn` theo khoảng ngày nghiệp vụ D108; tạo ở trạng thái `PAUSED`
    đặt `nextRunOn` = lần đầu ≥ hôm nay.
11. Mục tiêu: đổi tiền tệ khi đã có khoản góp → `FINANCE_CURRENCY_LOCKED`; trạng thái tính lại khi khoản góp hoặc
    `targetAmount` đổi; `ACTIVE` gửi lên khi `ACHIEVED` = người dùng mở lại; bỏ lưu trữ tính lại trạng thái.
12. Khoản góp không tồn tại → mã chung `NOT_FOUND` (D114 không có mã riêng).
13. Thêm `POST /budgets/{id}/unarchive` cho đối xứng (sync đã bỏ lưu trữ được qua `archived`).
14. Lệnh ghi khóa FOR SHARE tài khoản/danh mục được tham chiếu; xóa/đổi tiền tệ khóa FOR UPDATE rồi mới kiểm "đang
    dùng". Thứ tự khóa trong module: định kỳ → tài khoản (id tăng) → danh mục → mục tiêu → ngân sách (id tăng) →
    ChangeRecorder.
15. Tên lớp/DTO finance trùng module task được thêm tiền tố `Finance` (`FinanceCategoryCommandService`,
    `FinanceCategoryResponse`…) để không trùng bean/schema OpenAPI.

**Hoãn / ngoài phạm vi:** nợ/cho vay, hoàn tiền, quy đổi tiền tệ, nhập/xuất CSV riêng (D118; xuất dữ liệu ở WP7);
thông báo ngân sách tới thiết bị (handler `BUDGET_THRESHOLD` ở module `notification`, WP4 — đến lúc đó sự kiện chưa
có handler sẽ FAILED `NO_HANDLER`).

---

## WP4 — Phase 4 Calendar & Notification, phần server (4a–4d)

**Trạng thái:** xong. `mvn -f backend/pom.xml clean verify` xanh: 104 unit + 633 IT = 737 test, 0 bỏ qua, 4:29.
Lớp có test đồng thời (`ReminderIT` NTF-01 ×20) cùng `CalendarIT` chạy thêm 2 lượt liên tiếp, 29/29 mỗi lượt. 4e (Flutter) ngoài phạm vi backend.

**Thay đổi lớn**
- Module mới `calendar`: `CalendarEvent` (giờ địa phương + timezone là nguồn sự thật, `startAt/endAt/recurrenceUntil`
  dẫn xuất), `EventException`, `TimeBlock`; `calendar.domain.EventRecurrence` (D126, mở rộng khi đọc D125); cửa sổ hợp
  nhất `GET /calendar` (Event + Time Block + Task có hạn); tách chuỗi ("từ lần này"), cắt chuỗi ("xóa từ lần này"),
  ngoại lệ ("chỉ lần này"); 3 sync handler. Vector chung Java/Dart `docs/api/fixtures/recurrence-vectors.json`.
- Module mới `notification`: `Reminder` (sync), `ReminderSchedules`, `ReminderRunService` (FOR UPDATE SKIP LOCKED, dedupe
  `reminder:{id}:{epochSecond}`, bù ≤ 1 lần trong 24 giờ), `NotificationOrchestrator` (in-app + delivery một transaction,
  dedupe D122, giờ yên tĩnh D135, tùy chọn D136, mẫu D138/D144), `DeliveryWorker` (IN_FLIGHT 5 phút, gọi mạng ngoài
  transaction, backoff D137, gỡ token UNREGISTERED, SECURITY ≤ 24 giờ), `FcmPushSender` (FCM HTTP v1, nimbus ký JWT; tắt
  mặc định) / `DisabledPushSender`, inbox, tùy chọn, token push, hủy đăng ký email (HMAC riêng), `NotificationJobs`.
- **§5 #5:** 3 handler email bảo mật chuyển từ `identity.outbox` sang `notification.outbox` (nội dung, mã lỗi, hành vi D76
  giữ nguyên; mọi test outbox/email cũ xanh không sửa). Handler `BUDGET_THRESHOLD` (schema D149).
- **§5 #1:** `user_settings` thêm `quiet_start`, `quiet_end`, `show_details_on_lock_screen` (REST + payload sync); đổi
  timezone phát `identity.api.UserTimezoneChanged` → reminder tính lại trong cùng transaction (D134).
- **§5 #10:** đăng nhập đã nhận thông tin thiết bị (`device {name, platform, appVersion}`) và tạo dòng `devices` từ
  Giai đoạn 1 — không cần đổi hợp đồng thêm. Bảng `devices` đã có từ V1 (§4 "tạo ở WP4" đã thỏa).
- API công khai mới cho module khác: `task.api` (`TaskApi`, `TaskDeletionStarted`, `TaskScheduleChanged`), `habit.api`,
  `routine.api`, `calendar.api`, `identity.api` (`IdentityDirectory`, `NotificationProfile`, `UserTimezoneChanged`);
  `common.device` (`DeviceLifecycleListener`, `PushTokenLookup` — identity gọi notification qua đảo phụ thuộc);
  `common.sync.PendingChanges` (cascade nhiều listener vẫn đúng D86); `PayloadFields.object`.
- Migration `V7__calendar_reminder.sql` (đặc tả ghi V6) và `V8__notification.sql` (đặc tả ghi V7); V7 chuyển
  `reminder_time` của habit/routine thành reminder `LOCAL_AT` (D250, §4 kế hoạch).
- ArchUnit: `calendar`, `notification` vào danh sách module; ma trận A6 thêm `calendar → task`, `notification → identity,
  task, calendar, habit, routine` (chỉ qua `api/`, A4 vẫn áp dụng).
- `SecurityConfig.PUBLIC_GET_ENDPOINTS` (`/api/v1/notifications/unsubscribe`); `RateLimitPolicy` thêm `PUSH_TOKEN_USER`,
  `UNSUBSCRIBE_IP`; `UserDataCatalog` thêm 8 bảng. Cấu hình: `NOTIFICATION_UNSUBSCRIBE_SECRET` (bắt buộc ở prod),
  `FCM_*`, `API_BASE_URL`.
- Tài liệu: `SYNC_PROTOCOL.md` §3.4 (khóa user_settings mới), §3.7, §6.1; `API_CONVENTIONS.md` §5.1, §8.2, §8.3, §8.10;
  `SYSTEM_ARCHITECTURE.md` §3.11; `DATABASE_DESIGN.md` (V7, V8); `docs/api/openapi.yaml`.

**Ánh xạ test ID**

| ID | Lớp / phương thức |
|---|---|
| CAL-01 | `EventRecurrenceVectorsTest.Cal_01_…` (10 vector, gồm DST New York nhảy tiến/lùi) — phía Dart chạy cùng tệp |
| CAL-02..06 | `CalendarIT.Cal_02_…` … `Cal_06_…` |
| CAL-07 | `CalendarSyncIT.Cal_07_…` (2 test), `SyncSnapshotContractIT.Sync_42_phase4…` |
| NTF-01 | `ReminderIT.Ntf_01_firingTwice…`, `Ntf_01_parallelRuns…` (×20) |
| NTF-02 | `ReminderIT.Ntf_02_…` |
| NTF-03..10 | `NotificationIT.Ntf_03_…` … `Ntf_10_…`; NTF-05 thêm `FcmPushSenderTest.Ntf_05_…` |
| NTF-11 | Test outbox/email cũ không sửa (`OutboxWorkerIT`, `OutboxConcurrencyIT`, `MailpitDeliveryIT`, `RegistrationIT`, `PasswordResetIT`…) + ArchUnit A4/A6 |
| NTF-12 | `NotificationIT.Ntf_12_…` |
| NTF-13 | `SensitiveLogIT` (`runCalendarNotificationFlows`) |
| NTF-14 | Flutter — ngoài phạm vi backend |
| Cascade/đổi múi giờ | `ReminderIT.taskReminderFollowsTheDueDate…`, `ReminderIT.timezoneChange…` |
| IDOR | `IdorMatrixIT` (16 route mới) |
| Migration | `MigrationUpgradeIT.phase4MigrationsCarry…` (nâng cấp từ V6 có dữ liệu Phase 2–3), `FlywayMigrationIT`, `UserDataCatalogIT` |

**Test cũ đã sửa** (đều do thay đổi hợp đồng được phép, không nới kiểm tra)
- `SettingsIT.getReturnsDefaults`, `SyncSettingsIT.staleVersion…`, `SyncSnapshotContractIT` (danh sách khóa
  `user_settings`): thêm 3 khóa §5 #1 và khẳng định giá trị mặc định.
- `DeviceIT.listsOwnDevices…`: thêm `pushTokenSuffix` (D147) và khẳng định null khi chưa có token.
- `AuthorizationMatrixIT`: nhận thêm `SecurityConfig.PUBLIC_GET_ENDPOINTS` (D143) và khẳng định danh sách này khớp
  đúng các route GET công khai (như với POST).
- `RateLimiterTest.policyValuesMatchTheApprovedTable`: thêm 2 chính sách mới, tổng 13.
- `ApiDocsProdProfileIT`: cấp `lifeos.notification.unsubscribe-secret` như đã cấp JWT secret (profile prod bắt buộc
  khóa này — fail-fast có chủ ý).
- `IdorMatrixIT`, `FlywayMigrationIT`: mở rộng phạm vi.

**Quyết định phát sinh `[CONFIRMED]`** — duyệt nguyên trạng 2026-11-15 (PENDING_DECISIONS §1); mục có ghi chú riêng thì theo ghi chú
1. Migration đánh số V7/V8 (đặc tả ghi V6/V7).
2. `byWeekday` dạng mã `MO`..`SU`; tuần bắt đầu Thứ Hai khi tính chu kỳ WEEKLY (như WKST=MO của RFC 5545).
3. PATCH sự kiện có `allDay` ⇒ trường của hình dạng kia tự bỏ; ngoại lệ không còn khớp quy tắc mới được giữ nhưng bị bỏ
   qua khi mở rộng.
4. "Xóa từ lần này" là `DELETE …?fromOriginalStart=`; tách chuỗi xóa mềm ngoại lệ từ lần đó của chuỗi gốc; chuỗi mới
   giữ `count` còn lại.
5. Ngoại lệ không tồn tại dùng `CALENDAR_OCCURRENCE_NOT_FOUND`; trùng (sự kiện, lần xuất hiện) ở sync → `CONFLICT
   ALREADY_EXISTS` (như D235).
6. Reminder theo Task cả ngày nổ lúc 09:00 giờ người dùng; reminder LOCAL_AT dùng múi giờ người dùng (kể cả gắn
   Event); `repeatRule` cần `localDate`; reminder có mốc đã qua lúc tạo/sửa → không nổ (`DONE` ở lượt job kế tiếp).
7. Tắt IN_APP của một loại: thông báo vẫn được ghi (để khử trùng và neo delivery) nhưng hết hạn ngay, không hiện ở inbox.
8. Thông báo bảo mật của identity (xác minh, đặt lại, đổi mật khẩu) vẫn gửi email trực tiếp như trước (D76), không tạo
   thông báo in-app (người dùng chưa xác minh chưa đăng nhập được).
9. Push tắt mặc định (`DisabledPushSender` → FAILED `PUSH_DISABLED`); FCM giả lập trong test bằng
   `com.sun.net.httpserver` của JDK thay cho WireMock (không thêm thư viện).
10. Một token push cho mỗi thiết bị (UNIQUE `device_id`); đăng xuất (logout) cũng gỡ token của thiết bị.
11. Liên kết hủy đăng ký trỏ thẳng API (`API_BASE_URL`, mặc định `APP_BASE_URL`); profile dev có khóa HMAC mặc định riêng
    cho máy dev, prod bắt buộc biến môi trường.
12. Thứ tự khóa module mới: reminder/calendar event (FOR UPDATE) → đối tượng tham chiếu FOR SHARE qua `api/` → ghi →
    change_logs. Cascade nhiều module gom change_logs qua `PendingChanges`.

**Rủi ro cần xem:** Hibernate đang cấu hình `hibernate.jdbc.time_zone=UTC` nên cột `time` (`reminder_time`,
`local_time`, `quiet_start/quiet_end`) được quy đổi theo múi giờ JVM khi ghi/đọc qua JPA. Ghi và đọc đều qua JPA nên
nhất quán, và container chạy JVM ở UTC (không lệch); nhưng SQL thô/công cụ ngoài sẽ thấy giá trị lệch nếu JVM không ở
UTC. Đề xuất (cần duyệt, không tự sửa vì đổi cách lưu dữ liệu đã có): ép JVM `-Duser.timezone=UTC` hoặc map
`LocalTime` không qua quy đổi.

**Hoãn / ngoài phạm vi:** 4e Flutter (lập lịch local, khử trùng `dedupeKey`, iOS 64 mốc); `RECURRING_DUE`,
`HABIT_STREAK_AT_RISK` chưa có nguồn phát (handler theo schema D149 sẵn sàng); bảng `notification_templates` (dùng file).

---

## WP5 — Phase 5 Offline Sync nâng cao, phần server (5a, phần server 5d, 5e)

**Trạng thái:** xong. `mvn -f backend/pom.xml clean verify` xanh: 104 unit + 643 IT = 747 test, 0 bỏ qua, 4:38.
`SyncAdvancedIT`, `SyncMultiDeviceSimulationIT`, `ReminderIT` chạy thêm 2 lượt liên tiếp, 34/34 mỗi lượt. 5b, 5c (client
Flutter) ngoài phạm vi theo kế hoạch.

**Thay đổi lớn**
- Migration `V9__sync_advanced.sql` (đặc tả ghi V8): `user_sync_state.min_retained_seq`, `sync_mutations.device_id`,
  `sync_diagnostics`, chỉ mục `created_at` (retention) và chỉ mục một phần `deleted_at` cho 21 bảng đồng bộ. Bảng
  `devices` đã có từ V1 nên không tạo lại (D162; §4 "một bảng devices").
- `GET /sync/pull`: cursor < `min_retained_seq` → 410 `SYNC_FULL_RESYNC_REQUIRED` `{minRetainedSeq, currentSeq}`; pull
  chạy REPEATABLE READ để kiểm min và đọc trang trên cùng snapshot (retention song song không làm sót im lặng).
- `SyncRetentionService` + `RetentionStore` (D153–D156, §4: 180 ngày + ≥ 1000 seq, 35 ngày, tombstone 180 ngày): lô
  5000, transaction ngắn; change_logs khóa `user_sync_state` rồi nâng `min_retained_seq` cùng transaction; tombstone
  xóa cứng khi đã bị quên (không còn change_logs của entity) và không còn dòng con, thứ tự con trước cha
  (`TOMBSTONE_PLAN`, kiểm đủ mọi bảng có handler); giao dịch định kỳ giữ tombstone khi dòng định kỳ còn (§4).
- `POST /sync/snapshot` (D152, token HMAC 30 phút gắn user, trang ≤ 1000/≈ 1 MB, REPEATABLE READ READ ONLY),
  `GET /sync/entities/{type}/{id}`, `POST /sync/diagnostics` (D170, schema chỉ siêu dữ liệu), `SyncConsistencyService`
  (D172, chỉ đọc, gauge), `SyncJobs` (retention hằng ngày, nhất quán hằng tuần), metric D169
  (`full_resync.required`, `snapshot.duration`, `retention.deleted{table}`, `consistency.violations{check}`).
- `SyncEntityHandler.table()` (mọi handler khai báo bảng của mình — sync chỉ dùng để liệt kê id/xóa cứng/kiểm tra, dữ
  liệu vẫn qua `loadSnapshots`); `SyncHandlerRegistry.all()`.
- Mã lỗi `SYNC_FULL_RESYNC_REQUIRED`, `SYNC_SNAPSHOT_EXPIRED` (410); `RateLimitPolicy` thêm `SYNC_SNAPSHOT_USER`
  (6/giờ), `SYNC_ENTITY_USER` (120/phút), `SYNC_DIAGNOSTICS_USER` (5/giờ); `UserDataCatalog` thêm `sync_diagnostics`.
- Tài liệu (§5 #4, #6): `SYNC_RULES.md` §9.5 (chiến lược theo entity hoàn chỉnh), §9.6 (retention); `SYNC_PROTOCOL.md`
  §6.0; `API_CONVENTIONS.md` §5.1, §8.7; `SYSTEM_ARCHITECTURE.md` §3.12 (bảng job đầy đủ); `DATABASE_DESIGN.md` (V9);
  `docs/api/openapi.yaml`.

**Ánh xạ test ID (phần server)**

| ID | Lớp / phương thức |
|---|---|
| SYN-A01, A06 | `SyncAdvancedIT.SynA_01_SynA_06_…` |
| SYN-A05 | `SyncAdvancedIT.SynA_05_…` (ghi đồng thời giữa các trang, pull từ snapshotSeq bổ sung) |
| SYN-A07 | `SyncAdvancedIT.SynA_07_…` |
| SYN-A08 | `SyncAdvancedIT.SynA_08_…` |
| SYN-A10 | `SyncMultiDeviceSimulationIT.SynA_10_…` (3 seed × 150 bước trong verify; 20 × 1000 với `-Dlifeos.sync.simulation.full=true`) |
| SYN-A14 | `DeviceIT.revoking…`, `NotificationIT.Ntf_09_…` (thu hồi thiết bị gỡ token push) |
| SYN-A16 | `SyncAdvancedIT.SynA_16_…` |
| SYN-A17 | `SyncAdvancedIT.SynA_17_…` |
| SYN-A19 | `SyncAdvancedIT.SynA_19_…`, `IdorMatrixIT` (`/sync/entities/{type}/{id}`) |
| SYN-A20 | `FinanceSyncIT.Fin_18_…` (Finance luôn CONFLICT, số tiền chuỗi) |
| SYN-A18 | Benchmark — WP8 |
| SYN-A02..A04, A09, A11..A13, A15 | Client Flutter — ngoài phạm vi backend |

**Test cũ đã sửa**
- `RateLimiterTest.policyValuesMatchTheApprovedTable`: thêm 3 chính sách, tổng 16.
- Lượt verify đầu đỏ ở `UserDataCatalogIT` (quên đăng ký `sync_diagnostics`) — đã bổ sung vào danh mục, không sửa
  test.
- `IdorMatrixIT`: thêm tài nguyên `sync` cho `/sync/entities/{type}/{id}` (chặt hơn).

**Quyết định phát sinh `[CONFIRMED]`** — duyệt nguyên trạng 2026-11-15 (PENDING_DECISIONS §1); mục có ghi chú riêng thì theo ghi chú
1. Migration đánh số V9; không tạo bảng `devices`; `devices.last_seen_at` hiện có đóng vai "lần đồng bộ cuối" (không
   thêm `last_sync_at`).
2. `min_retained_seq` = seq lớn nhất đã xóa; cursor bằng đúng giá trị đó vẫn hợp lệ.
3. Tombstone "không còn thiết bị cần" = không còn dòng change_logs nào của entity (mọi seq của nó đã < min_retained).
4. ~~Khóa HMAC của `snapshotToken` sinh ngẫu nhiên khi khởi động (token chỉ 30 phút; khởi động lại → client tải
   lại)~~ — **KHÔNG duyệt nguyên trạng; thay bằng hành vi sau đợt sửa F9, `[CONFIRMED]` 2026-11-15:** khóa lấy từ secret
   `SNAPSHOT_HMAC_SECRET` (≥ 32 ký tự, không mặc định ngoài dev/test, thiếu ở production thì không khởi động); token còn
   hiệu lực qua khởi động lại/rolling deploy. Phần còn lại giữ nguyên: token của user khác/giả → 400 `VALIDATION_FAILED`
   (`snapshotToken`), hết hạn → 410.
5. `next` của snapshot dạng `"entityType:id"`; loại entity theo thứ tự chữ cái; không giới hạn "≤ 3 snapshot đang chạy"
   ngoài hạn mức 6 phiên/giờ (token không trạng thái).
6. Schema chẩn đoán cố định (khóa gốc và khóa lỗi whitelist); mọi lần gửi (kể cả bị từ chối) tính vào 5/giờ.
7. `user_settings` chưa có dòng ở `/sync/entities` → 404 như không tồn tại.
8. Mô phỏng đa thiết bị đầy đủ của đặc tả chạy theo cờ (nightly), bản rút gọn chạy trong verify; không sửa `ci/`
   (người dùng tự quản CI) — đề xuất thêm job nightly với cờ trên.

**Hoãn / ngoài phạm vi:** 5b/5c (full resync, hợp nhất 3 chiều, hàng đợi xung đột ở client); `merge-vectors.json` (D159)
thuộc client — chưa tạo; benchmark D173 ở WP8.

---

## WP6 — Phase 6 Knowledge & Goals (6a–6d, phần server)

**Trạng thái:** xong. `mvn -f backend/pom.xml clean verify` xanh: 126 unit + 681 IT = 807 test, 0 bỏ qua, 5:24.
`GoalCascadeConcurrencyIT` (KNW-13, 20 lần lặp) chạy thêm 2 lượt liên tiếp ngoài verify, 20/20 mỗi lượt. KNW-05 (đối chiếu
FTS5 client) thuộc Flutter — ngoài phạm vi.

**Thay đổi lớn**
- Migration `V10__knowledge_goals.sql` (đặc tả gọi là V9): extension `unaccent` + hàm IMMUTABLE `lifeos_unaccent`
  (§5 #7), `notes` (`search_vector` GENERATED cấu hình `simple`, GIN một phần), `tags`, `note_tags`, `note_links`,
  `goals`, `milestones`; `tasks.goal_id`/`milestone_id` + CHECK (§5 #2); nới CHECK `change_logs.entity_type`.
- Module `goal` (Goal, Milestone; `goal.domain.GoalProgress` tính tiến độ D185 bằng phân số chính xác, `GoalStatus`
  D188; `GoalApi`, `GoalDeletionStarted`; `GoalApiImpl` cài đặt `common.goal.GoalLinks`) và module `note` (Note, Tag,
  NoteTag, NoteLink; `NoteSearchStore` SQL tham số hóa; `NoteCascadeListener` cho `TaskDeletionStarted`/
  `GoalDeletionStarted`). 6 sync handler mới; `note_tag`/`note_link` chỉ CREATE/DELETE.
- Task: khóa `goalId`/`milestoneId` (REST + sync, §5 #2), `TaskApi.detachFromGoal/detachFromMilestone/goalCounts`,
  `GET /goals/{id}/tasks`. Task kiểm liên kết qua `common.goal.GoalLinks` (đảo phụ thuộc: không có `task → goal`).
- Sync: `SyncEntityHandler.maxPayloadBytes(default)`; `note` 192 KB (§5 #3). `NaturalKeyConflictException` thêm
  constructor có mã REST riêng (tag trùng tên: REST 409 `TAG_NAME_TAKEN`, sync `CONFLICT ALREADY_EXISTS`).
- ArchUnit: thêm module `goal`, `note`; phụ thuộc được phép `goal → task`, `note → task, goal`. `UserDataCatalog`
  thêm 6 bảng (thứ tự xóa: `note_links`, `note_tags`, `notes`, `tags` … `tasks` … `milestones`, `goals`).
  `SyncRetentionService.TOMBSTONE_PLAN` thêm 6 bảng với tham chiếu con. `RateLimitPolicy.NOTE_SEARCH_USER` (60/phút).
- Tài liệu: `SYNC_PROTOCOL.md` §3 mục 6, §3.1, §3.8, §6.1, §8; `DATABASE_RULES.md` §8 mục 10 (unaccent);
  `API_CONVENTIONS.md` §5.1, §8.4, §8.11; `DATABASE_DESIGN.md` (V10); `SYSTEM_ARCHITECTURE.md` §3.1;
  `docs/api/openapi.yaml`.

**Ánh xạ test ID**

| ID | Lớp / phương thức |
|---|---|
| KNW-01 | `NoteIT.Knw_01_…` |
| KNW-02 | `KnowledgeGoalSyncIT.Knw_02_…` |
| KNW-03 | `NoteIT.Knw_03_…` |
| KNW-04 | `NoteIT.Knw_04_…` (ổn định khi chèn dữ liệu không khớp giữa các trang; xem quyết định 6) |
| KNW-05 | Client Flutter — ngoài phạm vi backend |
| KNW-06 | `NoteIT.Knw_06_…` |
| KNW-07 | `KnowledgeGoalSyncIT.Knw_07_…` |
| KNW-08 | `KnowledgeGoalSyncIT.Knw_08_…` (server trả CONFLICT + bản server; hiển thị cạnh nhau là việc client) |
| KNW-09 | `GoalProgressPropertyTest.Knw_09_…` (20 × 500 vòng so mô hình tham chiếu), `GoalIT.Knw_09_…` |
| KNW-10 | `GoalIT.Knw_10_…`, `KnowledgeGoalSyncIT.Knw_10_…` |
| KNW-11 | `GoalProgressPropertyTest.Knw_11_…`, `GoalIT.Knw_11_…` |
| KNW-12 | `GoalIT.Knw_12_…` |
| KNW-13 | `GoalCascadeConcurrencyIT.Knw_13_…` (`@RepeatedTest(20)`) |
| KNW-14 | `NoteIT.Knw_14_…` |
| KNW-15 | `GoalIT.Knw_15_…`, `NoteIT.Knw_15_…`, `IdorMatrixIT` (21 route mới) |
| KNW-16 | `SensitiveLogIT` (`SensitiveLogScenario.runKnowledgeGoalFlows`) |
| KNW-17 | `NoteSearchPlanIT.Knw_17_…` — `Bitmap Index Scan on ix_notes_search`, ≈ 11 ms với 20 000 ghi chú/user (kế hoạch ở `backend/target/explain-notes.txt`) |
| SYN-42 (Phase 6) | `SyncSnapshotContractIT.Sync_42_phase6…` |
| Migration | `MigrationUpgradeIT.phase6MigrationKeepsExistingTasksAndEnablesAccentInsensitiveSearch` |

**Test cũ đã sửa (BACKEND_MASTER_PLAN §5 #2 cho phép)**
- `TaskIT.quickCapture…`, `SyncPushTaskIT.Sync_04_…`, `SyncSnapshotContractIT.Sync_42_pullData…`: tập khóa của Task
  thêm `goalId`, `milestoneId`.
- `RateLimiterTest`: thêm `NOTE_SEARCH_USER`, tổng 17. `IdorMatrixIT`: khai báo tài nguyên goals, milestones, notes,
  tags và thân request cho route mới (chặt hơn, không nới).

**Quyết định phát sinh `[CONFIRMED]`** — duyệt nguyên trạng 2026-11-15 (PENDING_DECISIONS §1); mục có ghi chú riêng thì theo ghi chú
1. Migration đánh số V10; `unaccent` bọc bằng `lifeos_unaccent` IMMUTABLE (cần cho cột GENERATED và chỉ mục).
2. Tiến độ Goal trả thang 0..1, 4 chữ số (HALF_UP), kèm `method`, số đếm thật và `allMilestonesCompleted` (gợi ý
   ACHIEVED, không tự đổi). Khi Goal có milestone, Task gắn thẳng vào Goal không tính vào % (đúng D185) nhưng vẫn
   trong `doneTasks/totalTasks`.
3. Sửa Task chỉ đổi `goalId` sang goal khác ⇒ milestone cũ tự gỡ; `goalId = null` ⇒ gỡ cả milestone.
4. Sync: `note.pinned/archived`, `milestone.completed` là cờ boolean, thời điểm do server đặt. `goal.status` cho phép
   ở CREATE (vắng = ACTIVE; khác ACTIVE thì đặt `statusChangedAt`); REST đổi trạng thái chỉ qua `/status`.
5. `note_tag`/`note_link` CREATE trùng cặp → `APPLIED` với version bản đang có (id của client không được tạo; client
   gộp theo cặp). DELETE không thấy → mã `NOTE_NOT_FOUND`. `PUT /notes/{id}/tags` không cần `version` của ghi chú,
   `note_tag` mới id do server sinh.
6. Tìm kiếm phân trang offset ≤ 1000 mã hóa trong `cursor` (`rank:offset`); ổn định khi dữ liệu không đổi thứ hạng;
   một ghi chú khớp được sửa giữa hai trang có thể dịch trang (giới hạn của offset, đã chọn ở §4 kế hoạch).
   Tên thẻ khớp cộng 0,05 vào rank. Không có đoạn trích đánh dấu từ khớp ở server (`ts_headline` trên văn bản không
   dấu sẽ mất dấu) — client tự đánh dấu.
7. Ghim tối đa 10 đếm cả ghi chú đã lưu trữ; giới hạn ghim/milestone/thẻ kiểm bằng đếm trong transaction có khóa cha
   (milestone/thẻ/liên kết khóa Goal/Note FOR UPDATE nên chính xác; ghim không có khóa cha nên có thể vượt khi đua).
   **Chốt 2026-11-15: giới hạn ghim 10 ghi chú là giới hạn mềm UX** (có thể vượt 1 khi ghim đồng thời).
8. Màu thẻ chuẩn hóa chữ hoa `#RRGGBB`. `GET /tags` trả mảng (không phân trang) theo tên.
9. FR-GOAL-06 (reminder gắn Goal/Milestone) **hoãn**: cần thêm `targetType` GOAL/MILESTONE cho `reminder` — thay đổi
   hợp đồng Phase 4 ngoài §5; reminder `NONE` dùng tạm. Ghi vào Open Questions của báo cáo cuối.

**Hoãn / ngoài phạm vi:** 6b phần client (FTS5, đối chiếu KNW-05), hợp nhất 3 chiều ở client, FR-GOAL-06 (quyết định 9).

---

## WP7 — Phase 7 Life Intelligence & Analytics (7a–7c, 7e, 7f; 7d theo §4)

**Trạng thái:** xong. `mvn -f backend/pom.xml clean verify` xanh: 153 unit + 693 IT = 846 test, 0 bỏ qua, 6:09.
7d (Redis) **không triển khai** theo §4 mục 7: chỉ lớp trừu tượng `AggregateCache` + bản no-op, `lifeos.cache.enabled=false`.

**Thay đổi lớn**
- Migration `V11__analytics_account.sql` (đặc tả gọi là V10): `reviews`, `data_exports`, `users.deletion_scheduled_at`,
  `account_deletion_codes` (chỉ băm), `account_deletion_audit` (băm userId, 365 ngày), chỉ mục `ix_tasks_user_completed`
  (D207), nới CHECK `change_logs.entity_type` (`review`).
- API thống kê chỉ đọc từng module (D197): `TaskStatsApi`, `HabitStatsApi`, `FinanceStatsApi`, `CalendarStatsApi`,
  `GoalStatsApi` — cài đặt dùng lại service sẵn có (không định nghĩa lại chỉ số của Phase 2/3/6).
- Module mới `analytics`: `domain` (`Period` D192, `TaskMetrics` D198, `FinanceMetrics` D199, `GoalRisk` D200), review
  phản tư (entity sync `review`, D202), `ReviewSummaryService` (ngày/tuần/tháng + kỳ trước), `DashboardService` (thẻ độc
  lập chạy song song trên luồng ảo, 1,5 s, D196), `AnalyticsService` (tasks/habits/finance/time/goals, giới hạn D208),
  `AggregateCache` no-op. ArchUnit: `analytics → task, habit, finance, calendar, goal` (chỉ `api`).
- Xuất dữ liệu (D204): `common.privacy.UserDataExporter` (zip `data.json` + CSV/bảng, BOM, chống formula injection,
  mỗi bảng một transaction REPEATABLE READ READ ONLY với con trỏ fetchSize, giới hạn 500 MB), `DataExportService` (≤ 2/24
  giờ, job 30 s `SKIP LOCKED`, 24 giờ, 5 lượt tải, thông báo SYSTEM qua outbox), REST `/account/export…`.
- Xóa tài khoản (D205): mã 6 chữ số qua email + mật khẩu, cooldown 7 ngày, thu hồi mọi refresh token, email bảo mật;
  `AccountDeletionGuardFilter` (403 `ACCOUNT_DELETION_PENDING` trừ `GET/DELETE /account/deletion`);
  `common.privacy.UserDataPurger` xóa cứng theo `UserDataCatalog` (con trước cha, lô 1000) rồi `users`; job hằng giờ.
- `UserDataCatalog`: thêm `reviews`, `data_exports`, `account_deletion_codes`; danh sách `EXPORT_EXCLUDED` có lý do (INT-15).
- Notification: `SystemNotificationHandler` (SYSTEM, mẫu `export.ready` vi/en), email `ACCOUNT_DELETION_CODE`,
  `ACCOUNT_DELETION_SCHEDULED`. Metric mới: `lifeos.cache.requests{result}`, `lifeos.dashboard.cards{card,status}`,
  `lifeos.export.completed{result}`, `lifeos.account.deletion{event}`.
- Mã lỗi Phase 7 (9 mã); `RateLimitPolicy`: `DASHBOARD_USER` (30/phút), `ACCOUNT_DELETION_CODE_USER` (5/giờ),
  `ACCOUNT_DELETION_FAILURE_USER` (5 lần sai/15 phút).
- Vận hành: `docs/ops/ALERTS.md`, `ops/grafana/lifeos-backend.json`. Vector dùng chung
  `docs/api/fixtures/analytics-vectors.json` (D203).
- Tài liệu: `SYNC_PROTOCOL.md` §3.9, §6.1; `SYNC_RULES.md` §9.5 (dòng Phase 6/7); `API_CONVENTIONS.md` §5.1, §8.12;
  `DATABASE_DESIGN.md` (V11); `SYSTEM_ARCHITECTURE.md` §3.1, §3.12; `docs/api/openapi.yaml`.

**Ánh xạ test ID**

| ID | Lớp / phương thức |
|---|---|
| INT-01 | `AnalyticsIT.Int_01_…` (ví dụ nhiều miền: Task, Habit, Finance 2 tiền tệ + chuyển khoản, Event, Time Block); mô hình tham chiếu ngẫu nhiên cho từng chỉ số ở INT-03/04/05 — **chưa** có property test nhiều miền trên DB (xem điểm cần xem 3) |
| INT-02 | `AnalyticsDomainTest.Int_02_…` (UTC+7, UTC−8, DST nhảy tiến/lùi, tuần CN), `AnalyticsVectorsTest` |
| INT-03 | `AnalyticsDomainTest.Int_03_…` (20 × 100 vòng so mô hình tham chiếu), `AnalyticsIT.Int_03_04_05_…` |
| INT-04 | `AnalyticsDomainTest.Int_04_…`, `AnalyticsIT.Int_01_…`/`Int_03_04_05_…` (tách tiền tệ, loại chuyển khoản, `savingsRate`) |
| INT-05 | `AnalyticsDomainTest.Int_05_…`, `AnalyticsIT.Int_03_04_05_…` |
| INT-06 | `DashboardServiceTest.Int_06_…` (thẻ lỗi, treo, song song), `AnalyticsIT.Int_06_09_…` |
| INT-07, INT-08 | Không áp dụng: Redis hoãn (§4 mục 7); bản no-op không giữ dữ liệu nên không có rò rỉ chéo user |
| INT-09 | `AnalyticsIT.Int_06_09_…` (không cache ⇒ đọc ngay sau ghi; có `computedAt`) |
| INT-10 | `AnalyticsVectorsTest` (phần Java); phần Dart thuộc client |
| INT-11 | `AnalyticsIT.Int_11_…`, `ReviewSyncIT.Int_11_…`, `SyncSnapshotContractIT.Sync_42_phase7…` |
| INT-12 | `DataExportIT` (2 test), `UserDataExporterTest.Int_12_…` |
| INT-13 | Benchmark 200k giao dịch — WP8 |
| INT-14 | `AccountDeletionIT` (2 test; xóa cứng kiểm tự động mọi bảng của danh mục) |
| INT-15 | `UserDataCatalogIT`, `UserDataExporterTest.Int_15_…` |
| INT-16 | `AnalyticsQueryPlanIT.Int_16_…` (100k Task + 100k giao dịch; `backend/target/explain-analytics.txt`) |
| INT-17 | `MetricsTagsIT.Int_17_…` |
| INT-18 | `SensitiveLogIT` (`runAnalyticsAccountFlows`: phản tư, số tiền, mã xác nhận) |
| INT-19 | `ArchitectureTest` (A6 với `analytics`) |

**Test cũ đã sửa**
- `RateLimiterTest`: thêm 3 chính sách, tổng 20. `IdorMatrixIT`: khai báo `reviews`, `account` (xuất dữ liệu).
  `SensitiveLogIT`: thêm luồng Phase 7. `SyncSnapshotContractIT`: thêm `Sync_42_phase7…`. (Chỉ thêm, không nới.)

**Quyết định phát sinh `[CONFIRMED]`** — duyệt nguyên trạng 2026-11-15 (PENDING_DECISIONS §1); mục có ghi chú riêng thì theo ghi chú
1. Migration V11; thêm bảng `account_deletion_codes`, `account_deletion_audit` và cột `data_exports.started_at`,
   `download_count` (đặc tả có giới hạn 5 lượt tải nhưng chưa có cột).
2. Một module `analytics` (theo D197) gom dashboard, review, analytics — `AGENTS.md` §3 còn liệt kê `dashboard`,
   `lifereview` riêng; tài liệu chuyên biệt (PHASE_7) được ưu tiên. Đề nghị người dùng xác nhận và sửa `AGENTS.md` §3.
3. Trung vị số chẵn = trung bình hai giá trị giữa, làm tròn xuống (giây). Hạn không có giờ = hết ngày (00:00 ngày sau,
   giờ địa phương); giờ rơi vào khoảng DST bị nhảy ⇒ dời tới (như `ZonedDateTime.ofLocal`).
4. Review: `overdueOpen` là số quá hạn tính đến hôm nay (không theo kỳ); "Task dời" (FR-INT-01) chưa có — dữ liệu không
   lưu lịch sử đổi hạn. Habit theo thứ trong tuần (FR-INT-05) chưa có — `HabitStatsApi` chỉ trả tổng theo kỳ.
5. Phân bổ thời gian: sự kiện cả ngày đếm vào `events` nhưng 0 phút; chia phút theo ngày địa phương.
6. Tốc độ chi `pace`: "ngày đã qua" tính cả hôm nay. Mặc định granularity `day` (analytics), `month` (finance).
7. Tải tệp xuất qua endpoint có xác thực của chủ sở hữu (không phát URL ký riêng); quá 5 lượt ⇒ 410 `EXPORT_EXPIRED`.
   Yêu cầu xuất khi đang có PENDING/RUNNING ⇒ 200 trả yêu cầu đó (không tính lượt mới). `RUNNING` > 2 giờ ⇒ `FAILED STALE`.
   **Giới hạn 1 yêu cầu đang chạy + 2 lần/24 giờ: bản kiểm-rồi-chèn không nguyên tử KHÔNG duyệt; thay bằng hành vi sau
   đợt sửa F5, `[CONFIRMED]` 2026-11-15:** giới hạn cứng — khóa dòng `users` trước khi kiểm/chèn và chỉ mục UNIQUE
   `ux_data_exports_user_active` (V13).
8. Xóa tài khoản: thêm `POST /account/deletion/code` (gửi mã); sai mã ⇒ 400 `VALIDATION_FAILED` field `code` (không thêm
   mã lỗi ngoài danh sách D205); đăng nhập trong cooldown vẫn được, mọi API khác 403; hủy không gửi email; băm kiểm toán
   = SHA-256(userId) không muối (userId không còn tồn tại sau xóa).
9. Xuất: mọi bảng của danh mục trừ `EXPORT_EXCLUDED`; bỏ mọi cột `*_hash` và kiểu `tsvector`; số thập phân/thời điểm
   xuất dạng chuỗi; hồ sơ `users` không có `password_hash`.
10. Thẻ dashboard cố định 5 thẻ; trạng thái `UNAVAILABLE` chưa phát sinh (mọi module đều có).
11. Ngưỡng D207 (dashboard < 800 ms, review < 500 ms, analytics < 1 s) đo ở benchmark WP8.
12. ~~Chưa có metric "lần chạy thành công cuối" cho job định kỳ — ALERTS.md ghi là đề xuất (Open Question).~~ Đã làm
    trong đợt sửa 2026-11-15 (OQ3): gauge `lifeos.job.last_success{job}`.
13. (Bổ sung 2026-11-15, không nằm trong danh sách gốc) Thống kê Task cắt ở 200.000 dòng im lặng — **KHÔNG duyệt; hành
    vi sau đợt sửa F7, `[CONFIRMED]` 2026-11-15:** đọc `MAX_ROWS + 1`, vượt trần ⇒ 422 `ANALYTICS_RANGE_TOO_LARGE`.

**Hoãn / ngoài phạm vi:** 7d Redis (§4), phần Dart của vector D203 và tính offline (client), INT-13 (WP8),
`local_aggregates` client.

---

## WP8 — Hoàn thiện và báo cáo cuối

**Trạng thái:** xong. `clean verify` xanh **3 lượt liên tiếp**: 153 unit + 694 IT = 847 test, 0 bỏ qua (6:03, 6:47,
7:02). Báo cáo cuối: `docs/review/BACKEND_FINAL_REPORT.md`.

**Thay đổi lớn**
- `ApiFixtureShapesIT` + fixture `task`, `goal`, `goal-detail`, `note`, `tag`, `review`, `calendar-event`, `reminder`
  (`docs/api/fixtures/`; ghi lại bằng `-Dfixtures.export=true`).
- `BackendBenchmark` (không chạy trong `verify` mặc định; `-Dit.test=BackendBenchmark`): push 50, pull 500, snapshot 50k,
  dashboard/review/analytics trên 100k Task + 200k giao dịch + 50k ghi chú, xuất dữ liệu INT-13 — đạt ngưỡng D173/D207.
- **Lỗi phát hiện và sửa:** scheduler một luồng — job xuất dữ liệu (WP7) chặn outbox worker/reminder; triệu chứng
  `MailpitDeliveryIT` đỏ chập chờn (lượt đầu của hai đợt 3 lượt). Sửa: `spring.task.scheduling.pool.size: 4`;
  `MailpitDeliveryIT` kiểm pool ≥ 2 và hoãn dữ liệu chờ do test khác để lại. Lần sửa 1 (chỉ cô lập dữ liệu) không đủ; lần
  sửa 2 xử lý nguyên nhân gốc — 3 lượt liên tiếp xanh sau đó.
- Tài liệu: `SYSTEM_ARCHITECTURE.md` §3.12 (pool scheduler), §12 (tiêu chí Phase 2–7); `BACKEND_SKELETON_PLAN.md`
  (trạng thái); `DATABASE_DESIGN.md` §4.5 (V5–V11, ERD tóm tắt); `AGENTS.md` §5–6.

**Test cũ đã sửa:** `MailpitDeliveryIT` — thêm `@BeforeEach` kiểm pool scheduler và cô lập dữ liệu chờ của test khác
(chặt hơn; timeout 30 s giữ nguyên).

**Quyết định phát sinh `[CONFIRMED]`** — duyệt nguyên trạng 2026-11-15 (PENDING_DECISIONS §1); mục có ghi chú riêng thì theo ghi chú
1. Pool scheduler 4 luồng (không chạy job chồng chính nó; job ghi đã dùng `SKIP LOCKED`/lease).
2. Benchmark là lớp không theo mẫu `*IT` để không chặn CI; đề xuất chạy nightly trong `ci/` (người dùng quản lý).

---

## Đợt sửa backend 1 — 2026-11-15 (sau WP8; `CLAUDE_CODE_BACKEND_FIX_PROMPT.md`, báo cáo `BACKEND_FIX_REPORT.md`)

**Trạng thái:** xong. `mvn -B -ntp -o clean verify` xanh **3 lượt liên tiếp**: 169 unit + 767 IT = **936 test**, 0 bỏ qua
(8:05, 6:55, 8:04). Trước đợt sửa: 167 + 712 = 879. Các lượt không xanh trước đó được khai ở `BACKEND_FIX_REPORT.md` §7.

**Quyết định của người dùng ghi nhận ngày 2026-11-15**
- Toàn bộ quyết định phát sinh WP2–WP8 chuyển `[CONFIRMED]` nguyên trạng, trừ 4 mục bị bác — chốt theo hành vi sau sửa
  F5/F6/F7/F9 (ghi tại WP3 mục 10, WP5 mục 4, WP7 mục 7 và mục 13).
- Giới hạn đếm không khóa (200 habit, 50 routine, ghim 10 ghi chú): giới hạn mềm UX (WP2 mục 15, WP6 mục 7; PHASE_2 D237,
  PHASE_6 §4).
- OQ1 (FR-GOAL-06, reminder cho GOAL/MILESTONE): hoãn. OQ3: làm trong đợt này. OQ5, OQ6: đã đóng theo quyết định deploy
  (`TZ=UTC` ghi vào `backend/README.md`).

**Thay đổi chính**
- F1–F9 đều tái hiện bằng test đỏ rồi sửa (bảng chi tiết ở báo cáo §1). Migration `V13__backend_fixes.sql`
  (chỉ mục UNIQUE một yêu cầu xuất đang chạy mỗi user; `outbox_events.user_id`). V1–V12 không đổi.
- Lớp mới: `finance.service.RecurringPauser`, `notification.outbox.RecurringPausedHandler`, `common.config.JobHealth`.
  Sự kiện outbox mới `RECURRING_PAUSED`; mẫu thông báo `finance.recurring.paused` (vi, en).
- Cấu hình mới bắt buộc ngoài dev/test: `SNAPSHOT_HMAC_SECRET` (`lifeos.sync.snapshot-hmac-secret`, ≥ 32 ký tự).
- Metric mới: `lifeos.job.last_success{job}` và `lifeos.job.expected_interval{job}` (job ∈ recurring, retention, purge,
  export); giá trị nhãn mới `result="lease_lost"` của `lifeos.notification.delivery`.
- `UserDataCatalog` thêm `outbox_events` (xóa khi xóa tài khoản, không xuất).

**Test mới (57):** `FinanceFixIT` (4), `NotificationFixIT` (22), `SnapshotFixIT` (2), `SnapshotSecretConfigTest` (2),
`ExportPurgeFixIT` (23), `AnalyticsTruncationIT` (1), `JobHealthIT` (2), `MigrationUpgradeIT` (+1).

**Test cũ đã sửa:** `RecurringIT.Fin_10` — khẳng định cuối đổi theo F6 (định kỳ trên tài khoản lưu trữ: `PAUSED`,
`nextRunOn` giữ nguyên; hành vi cũ "chỉ tiến `nextRunOn`" đã bị người dùng bác); `ApiDocsProdProfileIT` cấp thêm secret
snapshot. Không test nào bị xóa/`@Disabled`.

**Quyết định phát sinh `[PROPOSED]`** — 12 mục, liệt kê ở `BACKEND_FIX_REPORT.md` §8 (chưa được duyệt).

**Chưa làm:** `AGENTS.md` (§3 module, §5 số test — lệnh sửa bị từ chối bởi lớp kiểm quyền của phiên, cần người dùng tự
sửa); `docs/architecture/*` (thư mục được bảo vệ: V13 trong `DATABASE_DESIGN.md`, biến mới trong `SYSTEM_ARCHITECTURE.md`);
`docs/ops/ALERTS.md` + rule Prometheus (ngoài ranh giới); `deploy/scripts/preflight.*` (chỉ thêm biến vào compose và
`.env.*.example`).
