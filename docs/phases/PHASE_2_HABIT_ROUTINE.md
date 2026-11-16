# LifeOS — Phase 2: Habit & Routine (đặc tả)

> **Trạng thái:** v1 — **ĐÃ DUYỆT 2026-11-07** (người dùng duyệt `BACKEND_MASTER_PLAN.md`: toàn bộ quyết định D230–D259, quyết định mặc định ở §4 và thay đổi hợp đồng ở §5 của kế hoạch đó). Các mục `[OPEN]` trong tài liệu này được giải quyết bằng bảng §4 của `BACKEND_MASTER_PLAN.md`; khi mâu thuẫn, kế hoạch tổng thể thắng.
> **Nguồn đã chốt:** `PRODUCT_CONTEXT.md` Module 03, 04, §8.1 (bảng `habits`, `habit_checkins`, `routines`, `routine_steps`, `routine_logs`), §8.2 (DATE cho ngày nghiệp vụ), §9.4 (Habit Check-in: idempotent upsert + unique), §12 Phase 2 (MVP kết thúc ở đây), §19.1 mục 1 (MVP gồm local notification cơ bản); `DATABASE_RULES.md` §8; `SYNC_PROTOCOL.md`; `SECURITY_RULES.md`; `API_CONVENTIONS.md`.
> **Phụ thuộc:** backend Phase 1 (Identity, Task, Sync, Outbox). Không phụ thuộc Finance/Calendar. Phase 4, 6, 7 dùng lại định nghĩa chuỗi/tỷ lệ ở đây (D201 Phase 7).
> **Ngoài phạm vi:** habit nhóm/chia sẻ, thử thách, huy hiệu/gamification, habit định lượng (ví dụ "uống 2 lít"), liên kết Habit–Goal (Phase 6), AI phân tích (Phase 8), "Daily Goals" (chưa được định nghĩa ở Module 08).

---

## 1. Mục tiêu và phạm vi

Theo dõi **hành vi lặp lại** (Habit) và **chuỗi hoạt động có trình tự** (Routine) hằng ngày, chạy offline-first. Habit trả lời "hôm nay tôi đã làm chưa, duy trì được bao lâu"; Routine trả lời "buổi sáng tôi đi qua các bước nào". Nguyên tắc: **ngày nghiệp vụ là DATE theo múi giờ người dùng**, check-in **idempotent** (không đếm đôi), chuỗi/tỷ lệ **tính từ dữ liệu gốc** (không lưu), bỏ qua (skip) có chủ đích **không phá chuỗi**.

Trong phạm vi: Habit với lịch linh hoạt, check-in/undo/skip, lịch sử, chuỗi, thống kê; Routine ngày/tuần với các bước, chạy một routine, hoàn thành/bỏ qua từng bước, lịch sử; giờ nhắc đơn giản (local notification, không cần server); "Daily Dashboard/My Day" **tính ở client** từ SQLite (Task + Habit + Routine).

## 2. Yêu cầu chức năng

| ID | Yêu cầu | Tiêu chí chấp nhận |
|---|---|---|
| FR-HAB-01 | CRUD Habit | Tên 1–100, mô tả ≤ 500, màu/biểu tượng tùy chọn, `startOn` (mặc định hôm nay), `endOn` tùy chọn |
| FR-HAB-02 | Lịch linh hoạt | `DAILY`; `WEEKDAYS` (tập thứ); `TIMES_PER_WEEK` (n/tuần, 1–7); `EVERY_N_DAYS` (n ngày, 2–30) |
| FR-HAB-03 | Check-in | Đánh dấu hoàn thành cho một ngày; idempotent; không check-in tương lai |
| FR-HAB-04 | Undo check-in | Xóa check-in (tombstone); check-in lại ngày đó được |
| FR-HAB-05 | Skip | Bỏ qua một ngày có chủ đích (kèm ghi chú tùy chọn); không phá chuỗi, không tính vào mẫu số |
| FR-HAB-06 | Lịch sử | Danh sách check-in theo khoảng ngày, theo habit |
| FR-HAB-07 | Chuỗi | Chuỗi hiện tại và dài nhất theo §4 |
| FR-HAB-08 | Thống kê | Tỷ lệ hoàn thành theo tuần/tháng/khoảng tùy chọn; số lần hoàn thành/bỏ qua/bỏ lỡ |
| FR-HAB-09 | Lưu trữ / xóa | Lưu trữ giữ lịch sử, ẩn khỏi hôm nay; xóa xóa mềm cả check-in |
| FR-HAB-10 | Giờ nhắc | `reminderTime` (giờ địa phương) tùy chọn; client lập local notification |
| FR-RTN-01 | CRUD Routine | Tên 1–100; loại `DAILY` hoặc `WEEKLY` (tập thứ); `reminderTime` tùy chọn |
| FR-RTN-02 | Bước | ≤ 30 bước/routine; tiêu đề 1–100; thứ tự; thời lượng ước tính tùy chọn (1–240 phút) |
| FR-RTN-03 | Chạy routine | "Start" tạo lượt chạy cho một ngày; hoàn thành/bỏ qua từng bước; tự hoàn tất khi mọi bước xong |
| FR-RTN-04 | Lịch sử và tỷ lệ | Danh sách lượt chạy; tỷ lệ hoàn thành theo kỳ |
| FR-RTN-05 | Lưu trữ / xóa | Như Habit |
| FR-MYD-01 | My Day (client) | Hiện Task đến hạn/quá hạn, Habit đến lịch hôm nay, Routine đến lịch hôm nay, tất cả chạy offline |
| FR-HAB-11 | Offline | Mọi chức năng trên hoạt động khi mất mạng, đồng bộ sau |

## 3. Use case chính

| ID | Use case | Luồng và ngoại lệ |
|---|---|---|
| UC-HAB-01 | Tạo habit "Chạy bộ" T2/T4/T6 | `WEEKDAYS={1,3,5}`; chỉ các ngày đó đến lịch |
| UC-HAB-02 | Check-in hôm nay | Ghi local ngay; đẩy sau. Ngoại lệ: đã check-in → thao tác idempotent, không báo lỗi |
| UC-HAB-03 | Quên check-in hôm qua | Check-in bù (ngày quá khứ ≥ `startOn`) |
| UC-HAB-04 | Check-in cùng ngày trên hai thiết bị khi offline | Hai dòng khác id → server giữ một, thiết bị còn lại nhận xung đột "đã tồn tại" và dùng bản server (D235) |
| UC-HAB-05 | Bỏ qua ngày ốm | `SKIPPED`: chuỗi giữ nguyên, ngày đó không tính vào tỷ lệ |
| UC-HAB-06 | Đổi múi giờ khi đi du lịch | Ngày nghiệp vụ đã lưu **không đổi**; "hôm nay" tính theo múi giờ mới (D244) |
| UC-HAB-07 | Xóa habit | Cả check-in bị xóa mềm theo (cascade server); xác nhận hiện số check-in |
| UC-RTN-01 | Chạy Morning Routine | Start → hoàn thành/bỏ qua từng bước → tự hoàn tất → lưu vào lịch sử |
| UC-RTN-02 | Sửa routine đang có lượt chạy dở | Lượt chạy giữ nguyên số bước đã chụp lúc Start (D249) |

## 4. Quy tắc nghiệp vụ

### 4.1. Habit

1. `[CONFIRMED]` **D230 — Lịch (schedule) là dữ liệu có cấu trúc:** cột `schedule_type ∈ {DAILY, WEEKDAYS, TIMES_PER_WEEK, EVERY_N_DAYS}` cộng `weekdays smallint` (bitmask 7 bit, thứ Hai = bit 0 … Chủ nhật = bit 6, dùng khi `WEEKDAYS`), `times_per_week smallint 1–7`, `interval_days smallint 2–30` (kèm mốc `startOn`). Giá trị không liên quan đến loại lịch phải NULL (CHECK). Một ngày "đến lịch" (*scheduled*): `DAILY` mọi ngày ≥ `startOn`; `WEEKDAYS` ngày có thứ trong tập; `EVERY_N_DAYS` các ngày `startOn + k·n`; `TIMES_PER_WEEK` **không có ngày cố định** (xem D233).
2. `[CONFIRMED]` **D231 — Ngày nghiệp vụ:** `local_date` (DATE) do client gửi theo múi giờ hiện hành của người dùng; server chỉ kiểm: `≥ startOn`, `≤ endOn` nếu có, **không muộn hơn hôm nay + 1 ngày** theo múi giờ `user_settings.timezone` (dung sai +1 ngày cho lệch múi giờ/đồng hồ). Không check-in tương lai thật sự. Check-in bù quá khứ được phép không giới hạn (≥ `startOn`).
3. `[CONFIRMED]` **D232 — Check-in:** `habit_checkins(habit_id, local_date, status ∈ {DONE, SKIPPED}, note ≤ 200)`; `UNIQUE (habit_id, local_date) WHERE deleted_at IS NULL`. **Undo = xóa mềm**; check-in lại cùng ngày tạo dòng mới (partial unique loại tombstone). Đổi DONE↔SKIPPED là `UPDATE status`. Check-in ngày **không đến lịch** (ví dụ habit T2/T4/T6 mà check-in Chủ nhật) được phép và tính là thành tích phụ (không phá, không thêm chuỗi — xem D233).
4. `[CONFIRMED]` **D233 — Chuỗi (streak), tính chứ không lưu:**
   - `DAILY`, `WEEKDAYS`, `EVERY_N_DAYS`: xét **các ngày đến lịch** theo thứ tự thời gian. Chuỗi hiện tại = số ngày đến lịch liên tiếp `DONE` tính ngược từ ngày gần nhất không phải "hôm nay chưa xong"; ngày `SKIPPED` **bị bỏ qua** (không cộng, không đứt); ngày đến lịch không có check-in (đã qua) **làm đứt**; "hôm nay" chưa check-in **không** làm đứt (còn cả ngày). Chuỗi dài nhất = chuỗi liên tiếp dài nhất trong lịch sử với cùng quy tắc.
   - `TIMES_PER_WEEK` (n): đơn vị là **tuần** (bắt đầu theo `user_settings.week_start_day`). Với mỗi tuần: `required = max(n − skipped, 0)`; tuần **đạt** khi `required > 0` và `done ≥ required`; tuần có `required = 0` (đã bỏ qua đủ) là **trung tính** (không cộng, không đứt). Chuỗi = số tuần liên tiếp đạt tính ngược; **tuần hiện tại** chưa đạt không làm đứt nếu vẫn còn đủ ngày để đạt (`done + ngày còn lại ≥ required`), ngược lại tuần đó đã đứt khi hết tuần.
   - Check-in ngoài lịch (D232) không cộng chuỗi, không đứt.
   - Mọi tính toán chỉ dùng bản ghi chưa xóa mềm và ngày ≥ `startOn` (≤ `endOn`).
5. `[CONFIRMED]` **D234 — Thống kê:** với khoảng `[from,to]`: `scheduled` = số ngày đến lịch (hoặc `n` × số tuần cho `TIMES_PER_WEEK`), `done`, `skipped`, `missed = scheduled − done − skipped` (≥ 0, chỉ tính ngày đã qua, không tính hôm nay chưa xong), `completionRate = done / (scheduled − skipped)` làm tròn ở lớp hiển thị; `null` khi mẫu số 0. Định nghĩa này là **nguồn duy nhất** cho Phase 7 (D201). Lịch sử dùng **lịch hiện tại** cho toàn bộ quá khứ (hạn chế đã biết khi đổi lịch; xem `[OPEN]` 1).
6. `[CONFIRMED]` **D235 — Check-in trùng ngày giữa hai thiết bị:** mỗi thiết bị tạo UUID riêng nên có thể vi phạm `UNIQUE`. Quy tắc sync cho `habit_checkin`: `CREATE` khi đã tồn tại dòng hoạt động cùng `(habit_id, local_date)` → `CONFLICT` với `conflictReason = ALREADY_EXISTS` kèm `serverSnapshot` của dòng hiện có (không phải `REJECTED`, không ghi đè); client **chấp nhận bản server** (xóa dòng local, giữ outbox sạch) trừ khi `status` khác nhau, khi đó hiển thị lựa chọn DONE/SKIPPED. **Đây là thay đổi hợp đồng `SYNC_PROTOCOL.md` §4** (thêm lý do `ALREADY_EXISTS` cho CONFLICT của `CREATE`): cần duyệt riêng. Thay thế nếu không duyệt: `REJECTED HABIT_CHECKIN_EXISTS` và client tự đối chiếu (đơn giản hơn nhưng kém thông tin).
7. `[CONFIRMED]` **D236 — Vòng đời Habit:** `ACTIVE`/lưu trữ qua `archived_at`; lưu trữ ẩn khỏi "hôm nay" và danh sách mặc định, giữ nguyên check-in và thống kê; xóa = xóa mềm, cascade server các check-in (`mutation_id` NULL, sự kiện miền `HabitDeletionStarted` như D80, thứ tự khóa D86). `endOn` đã qua = habit tự hết hạn (không hiện ở "hôm nay"), vẫn xem được.
8. `[CONFIRMED]` **D237 — Giới hạn:** ≤ 200 habit (kể cả lưu trữ) và ≤ 50 routine mỗi người dùng; quá → 422 `LIMIT_EXCEEDED` / `REJECTED LIMIT_EXCEEDED`. Check-in không giới hạn số lượng. *(Chốt 2026-11-15: đây là **giới hạn mềm UX** — kiểm bằng đếm, không khóa, nên hai lệnh tạo đồng thời có thể vượt 1; không phải bất biến dữ liệu.)*

### 4.2. Routine

9. `[CONFIRMED]` **D240 — Routine:** `routines(name, kind ∈ {DAILY, WEEKLY}, weekdays smallint (khi WEEKLY, cùng bitmask D230), reminder_time time NULL, archived_at)`. `DAILY` đến lịch mọi ngày; `WEEKLY` các thứ trong tập (không rỗng).
10. `[CONFIRMED]` **D241 — Bước:** `routine_steps(routine_id, title, position bigint, duration_minutes smallint NULL)`; `position` dùng bước 1000 và trung điểm như milestone (D187 Phase 6), `UNIQUE (routine_id, position) WHERE deleted_at IS NULL`; ≤ 30 bước hoạt động/routine (`LIMIT_EXCEEDED`).
11. `[CONFIRMED]` **D242 — Lượt chạy (log):** `routine_logs(routine_id, local_date, total_steps smallint, started_at, completed_at NULL)`; `UNIQUE (routine_id, local_date) WHERE deleted_at IS NULL` (một lượt/ngày, chạy lại trong ngày dùng lại lượt đó). `total_steps` do **client chụp lúc Start** (số bước hoạt động, 1–30; server kiểm ≤ 30 và ≥ 1 — không kiểm bằng số bước hiện tại vì có thể đã đổi, D249). `completed_at` do server đặt khi `done + skipped = total_steps` (như D19).
12. `[CONFIRMED]` **D243 — Kết quả từng bước:** `routine_step_logs(routine_log_id, step_id, status ∈ {DONE, SKIPPED})`; `UNIQUE (routine_log_id, step_id) WHERE deleted_at IS NULL`; hoàn tác = xóa mềm; đổi DONE↔SKIPPED là UPDATE. `step_id` tham chiếu bước kể cả đã xóa mềm (lịch sử không mất).
13. `[CONFIRMED]` **D244 — Ngày và múi giờ:** mọi `local_date` đã lưu **bất biến** khi người dùng đổi múi giờ; "hôm nay", "tuần này" luôn tính theo múi giờ hiện hành. Hệ quả có chủ đích: đổi múi giờ có thể làm một ngày hiện ra hai lần hoặc bị bỏ lỡ; hệ thống không cố vá.
14. `[CONFIRMED]` **D245 — Tỷ lệ routine:** với khoảng ngày: `scheduled` = số ngày đến lịch đã qua; `completed` = số lượt có `completed_at`; `partial` = lượt có ít nhất một bước DONE nhưng chưa hoàn tất; `completionRate = completed / scheduled` (`null` khi `scheduled = 0`). Routine không có chuỗi ở phase này `[OPEN]` 2.
15. `[CONFIRMED]` **D246 — Lưu trữ/xóa Routine:** như D236; xóa cascade bước, lượt, kết quả bước (tombstone server, `mutation_id` NULL). Xóa một **bước**: tombstone bước, giữ `step_logs` cũ; lượt chạy đã hoàn tất không đổi.
16. `[CONFIRMED]` **D249 — Sửa routine khi có lượt chạy dở:** lượt chạy chỉ dựa vào `total_steps` đã chụp và các `step_logs` đã có; thêm/xóa/đổi bước không đổi lượt đã bắt đầu. Bước mới chỉ xuất hiện từ lượt Start kế tiếp.

### 4.3. Giờ nhắc, My Day, bảo mật

17. `[CONFIRMED]` **D250 — Giờ nhắc đơn giản:** `reminder_time` (TIME, giờ địa phương) trên `habits` và `routines`; **client** lập local notification hằng ngày (hoặc theo lịch) cho các ngày đến lịch; server không gửi push ở phase này. Khi Phase 4 có `reminders`, `reminder_time` được chuyển thành bản ghi `reminder` (target `HABIT`/`ROUTINE`) bằng migration dữ liệu có kiểm thử, và Phase 4 phải thêm `ROUTINE` vào `target_type` (hiện chỉ có `TASK/EVENT/HABIT/NONE`).
18. `[CONFIRMED]` **D251 — My Day tính ở client:** không có endpoint `GET /my-day` ở Phase 2 (offline-first: dữ liệu đã nằm trong SQLite). Client có vector kiểm thử `docs/api/fixtures/habit-vectors.json` (dữ liệu nhỏ + kết quả mong đợi: scheduled/streak/stats) chạy ở **Java và Dart** để hai bên cùng định nghĩa D233–D234. Endpoint tổng hợp phía server thuộc Phase 7 (dashboard).
19. `[CONFIRMED]` **D252 — Tên và mô tả:** chuẩn hóa NFC, cắt khoảng trắng đầu/cuối; tên habit không cần duy nhất; tên routine không cần duy nhất; không phân biệt hoa thường khi hiển thị.
20. `[CONFIRMED]` **D253 — Quyền riêng tư:** tên habit/routine, ghi chú check-in, tiêu đề bước là dữ liệu cá nhân: không vào log, metric nhãn, lỗi, push/email (Phase 4 D138).
21. `[CONFIRMED]` **D254 — Sync và quy tắc xung đột:** `habit`, `routine`, `routine_step`: theo trường, cùng khóa khác giá trị → hỏi (D158 Phase 5); `habit_checkin`, `routine_log`, `routine_step_log`: xem D235 và D242–D243; `schedule*` của habit luôn hỏi cả bộ (không trộn từng trường).

## 5. Mô hình dữ liệu (số `V` minh họa — dùng số kế tiếp tại thời điểm làm; cột chuẩn đồng bộ ở mọi bảng)

```sql
CREATE TABLE habits (
  -- cột chuẩn đồng bộ
  name varchar(100) NOT NULL, description varchar(500), color char(7), icon varchar(30),
  schedule_type varchar(14) NOT NULL CHECK (schedule_type IN ('DAILY','WEEKDAYS','TIMES_PER_WEEK','EVERY_N_DAYS')),
  weekdays smallint CHECK (weekdays BETWEEN 1 AND 127),
  times_per_week smallint CHECK (times_per_week BETWEEN 1 AND 7),
  interval_days smallint CHECK (interval_days BETWEEN 2 AND 30),
  start_on date NOT NULL, end_on date, reminder_time time, archived_at timestamptz,
  CHECK (end_on IS NULL OR end_on >= start_on),
  CHECK ((schedule_type = 'WEEKDAYS')       = (weekdays IS NOT NULL)),
  CHECK ((schedule_type = 'TIMES_PER_WEEK') = (times_per_week IS NOT NULL)),
  CHECK ((schedule_type = 'EVERY_N_DAYS')   = (interval_days IS NOT NULL))
);
-- INDEX (user_id) WHERE deleted_at IS NULL AND archived_at IS NULL

CREATE TABLE habit_checkins (
  habit_id uuid NOT NULL REFERENCES habits(id),
  local_date date NOT NULL,
  status varchar(7) NOT NULL CHECK (status IN ('DONE','SKIPPED')),
  note varchar(200)
);
-- UNIQUE (habit_id, local_date) WHERE deleted_at IS NULL ; INDEX (user_id, habit_id, local_date DESC) WHERE deleted_at IS NULL

CREATE TABLE routines (
  name varchar(100) NOT NULL,
  kind varchar(6) NOT NULL CHECK (kind IN ('DAILY','WEEKLY')),
  weekdays smallint CHECK (weekdays BETWEEN 1 AND 127),
  reminder_time time, archived_at timestamptz,
  CHECK ((kind = 'WEEKLY') = (weekdays IS NOT NULL))
);
CREATE TABLE routine_steps (
  routine_id uuid NOT NULL REFERENCES routines(id),
  title varchar(100) NOT NULL, position bigint NOT NULL,
  duration_minutes smallint CHECK (duration_minutes BETWEEN 1 AND 240)
);
-- UNIQUE (routine_id, position) WHERE deleted_at IS NULL
CREATE TABLE routine_logs (
  routine_id uuid NOT NULL REFERENCES routines(id),
  local_date date NOT NULL, total_steps smallint NOT NULL CHECK (total_steps BETWEEN 1 AND 30),
  started_at timestamptz NOT NULL, completed_at timestamptz
);
-- UNIQUE (routine_id, local_date) WHERE deleted_at IS NULL
CREATE TABLE routine_step_logs (
  routine_log_id uuid NOT NULL REFERENCES routine_logs(id),
  step_id uuid NOT NULL REFERENCES routine_steps(id),
  status varchar(7) NOT NULL CHECK (status IN ('DONE','SKIPPED'))
);
-- UNIQUE (routine_log_id, step_id) WHERE deleted_at IS NULL
```

Ràng buộc chéo (ngày check-in hợp lệ theo D231, `step_id` thuộc đúng routine của log, `total_steps` hợp lệ, giới hạn D237/D241) kiểm ở service. SQLite client: bảng tương ứng, ngày lưu `TEXT 'YYYY-MM-DD'`, bitmask `INTEGER`.

## 6. API (`/api/v1`)

| Endpoint | Ghi chú |
|---|---|
| `POST/GET /habits`, `GET/PATCH/DELETE /habits/{id}`, `POST /habits/{id}/archive` `/unarchive` | `GET` kèm `currentStreak`, `longestStreak` (tính); lọc `archived`, `scheduledOn=YYYY-MM-DD` |
| `PUT /habits/{id}/checkins/{date}` | **Upsert idempotent** theo (habit, ngày): body `{status, note}`; tạo mới (201) hoặc cập nhật (200); id do server dùng id client nếu gửi kèm. `DELETE /habits/{id}/checkins/{date}` = undo (204, idempotent) |
| `GET /habits/{id}/checkins?from=&to=` | Lịch sử, phân trang |
| `GET /habits/{id}/stats?from=&to=`, `GET /habits/stats?from=&to=` | D234; khoảng ≤ 366 ngày |
| `POST/GET /routines`, `GET/PATCH/DELETE /routines/{id}`, `POST /routines/{id}/archive` `/unarchive` | Chi tiết kèm bước |
| `POST/PATCH/DELETE /routines/{id}/steps`, `PATCH /routine-steps/{id}` | `position` như D241 |
| `PUT /routines/{id}/logs/{date}` (Start/idempotent), `PUT /routine-logs/{logId}/steps/{stepId}`, `DELETE …` | Hoàn thành/bỏ qua/hoàn tác bước; `completedAt` do server |
| `GET /routines/{id}/logs?from=&to=`, `GET /routines/{id}/stats?from=&to=` | D245 |

Với các endpoint `PUT` upsert theo ngày không cần `version`; các `PATCH/DELETE` theo `{id}` cần `version` như quy ước. Mã lỗi mới `[CONFIRMED]`: `HABIT_NOT_FOUND`, `HABIT_CHECKIN_NOT_FOUND`, `HABIT_INVALID_SCHEDULE` (422), `HABIT_DATE_OUT_OF_RANGE` (422, D231), `HABIT_CHECKIN_EXISTS` (chỉ dùng nếu D235 không được duyệt), `ROUTINE_NOT_FOUND`, `ROUTINE_STEP_NOT_FOUND`, `ROUTINE_LOG_NOT_FOUND`, `ROUTINE_INVALID_SCHEDULE` (422), `LIMIT_EXCEEDED` (422, dùng chung).

## 7. Đồng bộ

`entityType` mới: `habit`, `habit_checkin`, `routine`, `routine_step`, `routine_log`, `routine_step_log`.
- Cascade server (D80 mở rộng, thứ tự khóa D86, `mutation_id` NULL): xóa Habit → check-in; xóa Routine → bước, lượt, kết quả bước; xóa lượt → kết quả bước.
- `habit_checkin`/`routine_log`/`routine_step_log`: khóa duy nhất theo ngày → xem D235 (cùng cơ chế `ALREADY_EXISTS`).
- `completed_at` của `routine_log` do server đặt; `localDate`, `totalSteps` bất biến sau CREATE (UPDATE đổi → `REJECTED VALIDATION_FAILED`).
- Handler đặt ở `habit.sync.*`, `routine.sync.*`, tuân thủ A13.

## 8. Bảo mật

IDOR mọi route (kể cả `{date}`, `{stepId}`, `{logId}`); `stepId` thuộc routine của chính log và của user; mass assignment (`completedAt`, `userId`) bị bỏ qua; `SensitiveLogIT` mở rộng; giới hạn tần suất ghi: 600 yêu cầu ghi/phút/user dùng chung (check-in hàng loạt khi đồng bộ vẫn đi qua `sync/push` có hạn mức riêng).

## 9. Kế hoạch kiểm thử

| ID | Kịch bản | Kỳ vọng |
|---|---|---|
| HAB-01 | Lịch hợp lệ/không hợp lệ cho 4 loại (CHECK + service), bitmask biên | Đúng D230 |
| HAB-02 | Ngày check-in: `< startOn`, `> endOn`, hôm nay+1, hôm nay+2 theo múi giờ lệch UTC | Đúng D231 |
| HAB-03 | `PUT` check-in lặp 2 lần/đồng thời 20 lần: một dòng, không lỗi | Idempotent, không 5xx |
| HAB-04 | Undo rồi check-in lại cùng ngày | Dòng mới, partial unique đúng |
| HAB-05 | Vector chuỗi/thống kê (`habit-vectors.json`): DAILY, WEEKDAYS, EVERY_N_DAYS, TIMES_PER_WEEK, skip, ngoài lịch, hôm nay chưa xong, `week_start_day` T2 và CN, đổi múi giờ | Java **và** Dart cùng kết quả |
| HAB-06 | Mô hình tham chiếu viết độc lập (property-based 500 thao tác) | Streak/stats khớp tuyệt đối |
| HAB-07 | Sync: hai thiết bị check-in cùng ngày → CONFLICT `ALREADY_EXISTS` (hoặc REJECTED nếu thay thế), client dùng bản server | Đúng D235 |
| HAB-08 | Cascade xóa Habit (đồng thời với check-in, ≥ 20 lần): không deadlock, nhất quán, D86 | Đúng |
| HAB-09 | Lưu trữ giữ lịch sử/stat; `endOn` đã qua | Đúng D236 |
| HAB-10 | Giới hạn 200 habit, `LIMIT_EXCEEDED` ở REST và sync | Đúng D237 |
| RTN-01 | Bước: ≤ 30, `position` unique, chèn giữa/chuẩn hóa lại | Đúng D241 |
| RTN-02 | Start lặp/đồng thời: một lượt/ngày; `total_steps` 0/31 bị từ chối | Đúng D242 |
| RTN-03 | Hoàn thành bước, undo, đổi DONE↔SKIPPED; tự hoàn tất đúng lúc, `completed_at` do server | Đúng D243 |
| RTN-04 | Sửa/xóa bước khi có lượt dở và lượt cũ | Lượt không đổi (D249) |
| RTN-05 | Tỷ lệ/lịch sử routine: `scheduled = 0` ⇒ null, partial | Đúng D245 |
| RTN-06 | Cascade xóa Routine/lượt/bước | Đúng D246 |
| HAB-11 | IDOR + mass assignment + `SensitiveLogIT` cho cả 6 entity | Đúng |
| HAB-12 | Mọi bảng mới có `user_id` được đăng ký vào danh sách xuất/xóa tài khoản (Phase 7 INT-15) | Test thất bại khi thiếu |

## 10. Các bước triển khai và tiêu chí hoàn thành

| Bước | Nội dung |
|---|---|
| H1 | Habit CRUD + lịch + check-in `PUT/DELETE` + lịch sử |
| H2 | Chuỗi, thống kê, vector `habit-vectors.json`, mô hình tham chiếu |
| H3 | Routine, bước, lượt chạy, kết quả bước, thống kê |
| H4 | Sync 6 entity, cascade, giới hạn, hardening, HAB/RTN đầy đủ |

Hoàn thành khi: `mvn clean verify` xanh; HAB/RTN đầy đủ; OpenAPI/fixture cập nhật; thay đổi `SYNC_PROTOCOL.md` cho D235 đã được duyệt (hoặc phương án thay thế); báo cáo theo mẫu chuẩn. Sau bước này **toàn bộ backend của MVP (Phase 1–2) hoàn tất**; phần Flutter MVP là việc riêng.

## 11. Quyết định và câu hỏi mở

D230–D254 như trên (D255–D259 để dành). `[OPEN]` cần người dùng quyết: (1) đổi lịch habit làm thay đổi cách tính quá khứ — chấp nhận hạn chế (khuyến nghị) hay lưu `schedule_effective_from`/lịch sử lịch; (2) Routine có chuỗi ngày liên tiếp không (khuyến nghị: chưa); (3) duyệt D235 (thay đổi hợp đồng sync: `CONFLICT … ALREADY_EXISTS`) hay dùng phương án `REJECTED HABIT_CHECKIN_EXISTS` (khuyến nghị: duyệt D235); (4) dung sai +1 ngày của D231 và cho phép check-in bù không giới hạn (khuyến nghị: giữ); (5) `reminder_time` tạm thời trên habit/routine rồi migrate sang `reminders` ở Phase 4 (khuyến nghị: giữ); (6) có endpoint `GET /my-day` phía server ở Phase 2 hay chỉ tính ở client (khuyến nghị: chỉ client).
