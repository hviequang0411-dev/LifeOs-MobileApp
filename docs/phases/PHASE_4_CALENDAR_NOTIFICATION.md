# LifeOS — Phase 4: Calendar & Notification (đặc tả)

> **Trạng thái:** v1 — **ĐÃ DUYỆT 2026-11-07** (người dùng duyệt `BACKEND_MASTER_PLAN.md`: toàn bộ quyết định D120–D149, quyết định mặc định ở §4 và thay đổi hợp đồng ở §5 của kế hoạch đó). Các mục `[OPEN]` trong tài liệu này được giải quyết bằng bảng §4 của `BACKEND_MASTER_PLAN.md`; khi mâu thuẫn, kế hoạch tổng thể thắng.
> **Nguồn đã chốt:** `PRODUCT_CONTEXT.md` Module 11 và §10; `SYSTEM_ARCHITECTURE.md` §3.11–3.12, §5.6, §9; `DATABASE_RULES.md` §8; `SYNC_PROTOCOL.md`; `SECURITY_RULES.md`; Flutter local notification (iOS cap 64, `NotificationScheduler`) đã có từ Phase 1.
> **Phụ thuộc:** backend giai đoạn 1–4 (Identity, Task, Sync, Outbox). Nhận sự kiện từ Phase 3 (`BUDGET_THRESHOLD`, `RECURRING_DUE`) khi có.
> **Ngoài phạm vi:** đồng bộ lịch Google/Apple, lời mời nhiều người/chia sẻ lịch, SMS, WhatsApp/Zalo, thông báo marketing, AI gợi ý lịch (Phase 8).

---

## 1. Mục tiêu và phạm vi

Thêm **thời gian** vào LifeOS: Calendar Event, Time Block, Reminder, và một hệ thống thông báo đáng tin cậy đa kênh (in-app, FCM, email, local). Nguyên tắc: **nhắc đúng giờ theo múi giờ người dùng, không gửi trùng, không mất khi lỗi, người dùng kiểm soát hoàn toàn** (kênh, giờ yên tĩnh, tắt từng loại).

Trong phạm vi: Calendar Event (một lần và lặp), Time Block gắn Task, Reminder (cho Task/Event/Habit/độc lập), module `notification` (thay `OutboxEventHandler` của `identity`), in-app notification, FCM push, email, tùy chọn thông báo, quiet hours, delivery log và retry, đăng ký/hủy thiết bị push, xem lịch tuần/tháng/ngày hợp nhất Task + Event + Time Block.

## 2. Yêu cầu chức năng

| ID | Yêu cầu | Tiêu chí chấp nhận |
|---|---|---|
| FR-CAL-01 | CRUD Calendar Event | Tiêu đề 1–200, mô tả ≤ 2000, địa điểm ≤ 200, `allDay` hoặc giờ bắt đầu/kết thúc, `timezone` của sự kiện |
| FR-CAL-02 | Sự kiện lặp | `DAILY/WEEKLY/MONTHLY/YEARLY`, interval, ngày kết thúc hoặc số lần; sửa/xóa "chỉ lần này", "từ lần này", "tất cả" (§6) |
| FR-CAL-03 | Time Block | Gán khoảng thời gian cho một Task; một Task có nhiều block; cảnh báo (không chặn) khi trùng giờ |
| FR-CAL-04 | Xem lịch | Ngày/tuần/tháng, hợp nhất Event + Time Block + Task có `dueDate`; hoạt động offline |
| FR-CAL-05 | Phát hiện xung đột lịch | Chỉ cảnh báo trên UI (client tính); server không chặn |
| FR-NTF-01 | Reminder | Gắn với một entity (Task/Event/Habit) hoặc độc lập; mốc tuyệt đối hoặc tương đối (X phút/giờ/ngày trước) |
| FR-NTF-02 | Reminder lặp | Theo quy tắc lặp của entity cha (sự kiện lặp ⇒ mỗi lần xuất hiện có reminder) |
| FR-NTF-03 | Kênh | In-app luôn có; FCM nếu có thiết bị đăng ký; email cho loại được bật; local notification do client lập lịch |
| FR-NTF-04 | Tùy chọn | Bật/tắt theo kênh × loại thông báo; quiet hours theo giờ địa phương; mặc định an toàn (email tắt trừ bảo mật) |
| FR-NTF-05 | Quiet hours | Trong khung quiet hours: push/email hoãn tới hết khung (trừ loại `SECURITY`), in-app vẫn ghi |
| FR-NTF-06 | In-app inbox | Danh sách, đánh dấu đã đọc/tất cả, xóa, đếm chưa đọc; giữ 90 ngày |
| FR-NTF-07 | Độ tin cậy | Mỗi thông báo gửi tối đa một lần/kênh; lỗi tạm thời retry có backoff; lỗi vĩnh viễn ghi log và ngừng |
| FR-NTF-08 | Thiết bị push | Đăng ký/cập nhật/hủy token; token chết bị gỡ tự động |
| FR-NTF-09 | Hủy đăng ký | Email có liên kết hủy từng loại (không cần đăng nhập, dùng token ký) |

## 3. Use case chính

| ID | Use case | Luồng và ngoại lệ |
|---|---|---|
| UC-CAL-01 | Tạo sự kiện lặp hàng tuần | Client ghi local + outbox; server lưu quy tắc; lần xuất hiện được **tính** khi đọc, không lưu từng lần |
| UC-CAL-02 | Sửa một lần xuất hiện | Tạo bản ghi ngoại lệ (`event_exceptions`); chuỗi gốc giữ nguyên |
| UC-CAL-03 | Xóa "từ lần này trở đi" | Đặt `until` của chuỗi gốc = trước lần đó (cắt chuỗi); không tạo chuỗi mới |
| UC-NTF-01 | Reminder 15 phút trước sự kiện | Server tính `fire_at` UTC từ giờ địa phương + timezone; job quét và gửi |
| UC-NTF-02 | Đổi múi giờ khi đi du lịch | Reminder dạng "giờ địa phương" tính lại theo timezone mới (D134) |
| UC-NTF-03 | Thiết bị offline lúc reminder | Local notification đã lập lịch vẫn nổ; push đến sau khi có mạng; **khử trùng** (D132) |
| UC-NTF-04 | FCM báo token không hợp lệ | Xóa token, không retry |
| UC-NTF-05 | Gửi email thất bại 5 lần | Trạng thái `FAILED`, giữ log, không gửi lại; không chặn các thông báo khác |
| UC-NTF-06 | Người dùng tắt loại thông báo | Không tạo delivery cho kênh đó; in-app vẫn tạo nếu loại còn bật in-app |
| UC-NTF-07 | Đổi mật khẩu | Email bảo mật (loại `SECURITY`) gửi bất kể quiet hours/tùy chọn email (D140); thay thế xử lý cũ trong `identity` |

## 4. Quy tắc nghiệp vụ

1. `[CONFIRMED]` **D120 — Module `notification` mới:** `notification.api/controller/service/domain/repository/channel/job`. Nhận sự kiện qua `outbox_events` (hạ tầng đã có). Module `identity` chỉ **ghi** sự kiện outbox (đã làm cho `PASSWORD_CHANGED`, đăng ký, quên mật khẩu); `OutboxEventHandler` email chuyển sang `notification` đúng như ghi chú kiến trúc. ArchUnit: `notification` không phụ thuộc `..domain..`/`..repository..` của module khác; chỉ `api`.
2. `[CONFIRMED]` **D121 — Mọi thông báo bắt nguồn từ một `notification_event`:** hai nguồn — (a) outbox (sự kiện nghiệp vụ: bảo mật, ngân sách, Habit...), (b) bảng `reminders` (đến hạn theo thời gian). Cả hai hội tụ vào `NotificationOrchestrator.create(...)` → tạo `notifications` (in-app) + `notification_deliveries` (mỗi kênh/thiết bị một dòng) trong **một transaction**; `UNIQUE (dedupe_key)` đảm bảo idempotent. *(Cập nhật 2026-11-15 theo `docs/review/BACKEND_FIX_REPORT.md`: F2 — thông báo cha sống tới khi mọi delivery của nó kết thúc: job dọn inbox không xóa thông báo còn delivery `PENDING`/`IN_FLIGHT`, kể cả khi IN_APP tắt nên `expires_at` = lúc tạo)*
3. `[CONFIRMED]` **D122 — Khóa khử trùng (`dedupe_key`):** `outbox:{outbox_event_id}` hoặc `reminder:{reminder_id}:{occurrence_instant}` (UTC, giây). Retry/chạy lặp job chỉ tạo đúng một thông báo.
4. `[CONFIRMED]` **D123 — Hai tầng nhắc:** (1) **Local notification** do client lập lịch từ dữ liệu đã đồng bộ (đúng giờ, offline); (2) **push/email từ server** để chạm thiết bị khác/khi app đóng. Cặp local+push cho cùng một reminder được khử trùng ở client bằng `dedupe_key` đi kèm payload push (D132). Server **không** biết thiết bị đã nổ local hay chưa.
5. `[CONFIRMED]` **D124 — Giờ lưu trữ:** mọi mốc tuyệt đối lưu `TIMESTAMPTZ` UTC; sự kiện kèm `timezone` IANA và (nếu không `all_day`) giờ địa phương `start_local`/`end_local` (`TIMESTAMP` không múi giờ) là **nguồn sự thật** cho sự kiện lặp và reminder tương đối; `start_at`/`end_at` UTC là giá trị dẫn xuất lưu kèm để truy vấn khoảng. `all_day`: chỉ `start_date`/`end_date` (DATE), không có giờ.
6. `[CONFIRMED]` **D125 — Tính lần xuất hiện:** quy tắc lặp lưu trong bản ghi sự kiện gốc; server/client **mở rộng theo cửa sổ truy vấn** (không lưu từng lần). Tối đa **1000 lần xuất hiện** mỗi cửa sổ, cửa sổ truy vấn tối đa 366 ngày. Lần xuất hiện được định danh bằng `(eventId, originalStartLocal)`.
7. `[CONFIRMED]` **D126 — Quy tắc lặp (tập con của RFC 5545):** `freq ∈ {DAILY,WEEKLY,MONTHLY,YEARLY}`, `interval 1–365`, `byWeekday` (cho WEEKLY, tập thứ), `byMonthDay` (1–31, ngày không tồn tại → ngày cuối tháng, như D110), kết thúc: `until` (ngày) **hoặc** `count` (1–1000) **hoặc** vô hạn. Không hỗ trợ `BYSETPOS`, `EXDATE` tự do, `RDATE`. Dạng JSON lưu trong cột `recurrence` (schema cố định, kiểm ở service) — không lưu chuỗi RRULE thô.
8. `[CONFIRMED]` **D127 — Ngoại lệ lần xuất hiện:** `event_exceptions(event_id, original_start_local, kind, override…)` với `kind ∈ {CANCELLED, MODIFIED}`; MODIFIED mang các trường ghi đè (tiêu đề, giờ, địa điểm, mô tả). Đồng bộ như entity riêng `event_exception`. Xóa chuỗi gốc xóa mềm cả ngoại lệ (cascade phía server như D80, `mutation_id` NULL).
9. `[CONFIRMED]` **D128 — Time Block:** `time_blocks(task_id, start_at, end_at, ...)`; tối đa 24 giờ/block; `end_at > start_at`; xóa Task (D25: Task xóa mềm) xóa mềm các block của nó (cascade phía server). Block không chặn trùng; không gắn Habit ở phase này.
10. `[CONFIRMED]` **D129 — Reminder gắn entity:** `reminders(target_type, target_id, kind, offset_minutes | fire_at_local, ...)`; `target_type ∈ {TASK, EVENT, HABIT, ROUTINE, NONE}`. Với TASK: tương đối theo `dueDate`/giờ hạn; với EVENT: tương đối theo `start`; mỗi target tối đa **10** reminder. Mất target (xóa) ⇒ reminder bị xóa mềm (cascade server). Habit/Routine: dùng `reminder_time` của Phase 2 làm nguồn khi migrate (xem `PHASE_2_HABIT_ROUTINE.md` D250).
11. `[CONFIRMED]` **D130 — Đồng bộ reminder:** `reminder` là entity sync; server **không** nhận `fire_at` tuyệt đối từ client — server tính `next_fire_at` (UTC) từ quy tắc + timezone user/sự kiện (mass-assignment bị bỏ qua).
12. `[CONFIRMED]` **D131 — Job reminder:** `ReminderScheduler` mỗi 30 giây: `SELECT … WHERE status='ACTIVE' AND next_fire_at <= now() ORDER BY next_fire_at LIMIT n FOR UPDATE SKIP LOCKED`; mỗi reminder một transaction: tạo notification qua Orchestrator (dedupe `reminder:{id}:{instant}`), tính `next_fire_at` kế tiếp (hoặc `status='DONE'` nếu hết), tăng `version`. Bù tối đa **một** lần bị lỡ trong vòng **24 giờ** gần nhất (lỗi/downtime), cũ hơn bị bỏ và ghi log `lifeos.notification.reminder.skipped`. D86: thứ tự khóa — dữ liệu nghiệp vụ trước, `ChangeRecorder.record` sau cùng.
13. `[CONFIRMED]` **D132 — Khử trùng ở client:** payload push mang `dedupeKey`; client giữ bảng `delivered_notifications(dedupe_key, shown_at)` 7 ngày; nếu local đã nổ cho cùng khóa (hoặc đã thấy qua push) thì không hiện lại. Local notification dùng cùng công thức khóa.
14. `[CONFIRMED]` **D133 — Giới hạn iOS 64:** `NotificationScheduler` (Flutter) chỉ lập lịch 64 mốc gần nhất; làm mới danh sách khi mở app, sau sync, và qua background fetch (best effort). Server push là mạng an toàn cho các mốc xa. Android dùng `AlarmManager` exact alarm theo quyền `SCHEDULE_EXACT_ALARM` (khi bị từ chối: rơi về inexact + cảnh báo trong Cài đặt).
15. `[CONFIRMED]` **D134 — Timezone:** reminder gắn mốc **giờ địa phương** (ví dụ "7:00 hằng ngày") theo `user_settings.timezone` hiện hành; khi người dùng đổi timezone, server tính lại `next_fire_at` của các reminder dạng giờ địa phương trong cùng transaction đổi (cascade phía server). Reminder gắn Event dùng `timezone` của Event (sự kiện ở Tokyo nhắc theo giờ Tokyo). DST: giờ địa phương không tồn tại (nhảy tiến) → lùi đến mốc hợp lệ kế tiếp; giờ lặp (lùi) → lần xuất hiện **đầu tiên**.
16. `[CONFIRMED]` **D135 — Quiet hours:** `notification_preferences` có `quiet_start`, `quiet_end` (TIME, giờ địa phương), có thể qua nửa đêm. Delivery push/email trong khung → `scheduled_for = quiet_end` (hôm nay hoặc ngày mai). Loại `SECURITY` và `scheduled` reminder được đánh dấu "khẩn" (`urgent=true`, người dùng bật theo từng reminder) bỏ qua quiet hours. In-app không bị hoãn. Local notification: client không hiển thị âm thanh trong khung (đặt kênh im lặng), vẫn hiện trong khay. *(Cập nhật 2026-11-15 theo `docs/review/BACKEND_FIX_REPORT.md`: F2 — push/email bị hoãn tới hết giờ yên tĩnh vẫn được gửi dù thông báo in-app đã hết hạn/đã tắt)*
17. `[CONFIRMED]` **D136 — Tùy chọn mặc định:** `notification_preferences(user_id, category, channel, enabled)`; `category ∈ {SECURITY, REMINDER, TASK, HABIT, FINANCE, SYSTEM}`; `channel ∈ {IN_APP, PUSH, EMAIL}`. Mặc định: IN_APP bật tất cả; PUSH bật `SECURITY,REMINDER,TASK,HABIT`; EMAIL chỉ `SECURITY`. `SECURITY` × (IN_APP, EMAIL) **không thể tắt**. Dòng tùy chọn tạo lười khi cần; thiếu dòng = mặc định.
18. `[CONFIRMED]` **D137 — Delivery & retry:** `notification_deliveries(status ∈ {PENDING, SENT, FAILED, SKIPPED}, attempt_count, next_attempt_at, last_error_code)`. Retry khi lỗi tạm thời (5xx, timeout, FCM `UNAVAILABLE`): backoff 30s, 2m, 10m, 1h, 6h (tối đa 5 lần, jitter ±20%); lỗi vĩnh viễn (FCM `UNREGISTERED`, email bounce cứng, 4xx cấu hình) → `FAILED` ngay (và gỡ token nếu `UNREGISTERED`). Chọn việc bằng `FOR UPDATE SKIP LOCKED`, một delivery một transaction **ngắn**; gọi mạng ngoài **ngoài** transaction DB: đánh dấu `IN_FLIGHT` (hết hạn sau 5 phút để tự khôi phục) → gọi → cập nhật kết quả. *(Cập nhật 2026-11-15 theo `docs/review/BACKEND_FIX_REPORT.md`: F3 — gửi là at-least-once; trạng thái trung gian `IN_FLIGHT` có lease 5 phút và hạn khóa là dấu sở hữu: kết quả gửi/hẹn thử lại chỉ được ghi khi worker còn giữ đúng lease đã cấp, nên worker quá hạn không ghi đè kết quả của worker đã lấy lại (không gửi trùng do ghi đè))*
19. `[CONFIRMED]` **D138 — Nội dung thông báo & riêng tư:** tiêu đề/nội dung sinh ở server từ template + biến (`notification_templates` theo `category` + ngôn ngữ `user_settings.locale`, mặc định `vi`). Push/email **không chứa** nội dung nhạy cảm: số tiền, ghi chú giao dịch, nội dung ghi chú (Notes Phase 6), mật khẩu/token. Task/Event: chỉ tiêu đề (cắt 80 ký tự) nếu người dùng bật `showDetailsOnLockScreen` (mặc định **tắt**: hiện "Bạn có nhắc nhở"). Dữ liệu chi tiết lấy qua deep link `lifeos://…/{id}` sau khi mở app.
20. `[CONFIRMED]` **D139 — Thiết bị push:** theo `PRODUCT_CONTEXT.md` §19.2 (đã chốt) `identity` sở hữu bảng `devices(id, user_id, refresh_family_id, name, platform, app_version, last_sync_at, created_at)` — **Phase 4 tạo bảng này** nếu chưa có (đăng nhập nhận `deviceName`/`platform` tùy chọn, cần duyệt vì sửa hợp đồng auth). `notification` sở hữu `device_push_tokens(id, device_id → devices, user_id, token, last_seen_at, ...)`; `UNIQUE (token)` toàn cục (token đổi chủ khi người dùng khác đăng nhập trên cùng thiết bị: `UPSERT` chuyển sang `device_id` mới); tối đa 10 token/user (vượt → gỡ token cũ nhất theo `last_seen_at`). Đăng xuất/xóa thiết bị (`DELETE /devices/{id}`) gỡ token push của thiết bị đó; thu hồi họ refresh token do phát hiện tái sử dụng không tự gỡ token (xem D141). Hai bảng **không** đồng bộ qua sync (dữ liệu thiết bị).
21. `[CONFIRMED]` **D140 — Email bảo mật:** các sự kiện `REGISTERED_VERIFY`, `PASSWORD_RESET_REQUESTED`, `PASSWORD_CHANGED` đã có ở `identity` → `notification` xử lý, giữ nguyên hành vi (D76). Loại `SECURITY`: bỏ qua tùy chọn và quiet hours; **không** retry quá 24 giờ kể từ tạo (liên kết đã hết hạn).
22. `[CONFIRMED]` **D141 — Làm sạch token thiết bị:** job hằng ngày gỡ token `last_seen_at` > 60 ngày; khi `refresh_tokens` họ bị thu hồi do phát hiện tái sử dụng, **không** tự xóa token push (có thể vẫn cần gửi thông báo bảo mật cho thiết bị khác); không gửi thông báo không phải `SECURITY` tới thiết bị không còn họ token hợp lệ `[OPEN]`.
23. `[CONFIRMED]` **D142 — In-app inbox:** `notifications(id, user_id, category, title, body, data jsonb, read_at, created_at, expires_at)`; **không** đồng bộ qua `change_logs` (dữ liệu dẫn xuất từ server); client lấy bằng `GET /notifications` (cursor) + đếm chưa đọc; đánh dấu đọc là `PATCH` đơn giản (không có `version` — idempotent theo bản chất: `read_at` chỉ đặt nếu NULL). Giữ 90 ngày, job xóa hằng ngày theo lô.
24. `[CONFIRMED]` **D143 — Liên kết hủy đăng ký email:** token ký HMAC (khóa riêng, không dùng khóa JWT) chứa `userId, category, exp(30 ngày)`; `GET /api/v1/notifications/unsubscribe?token=` công khai, giới hạn 20 yêu cầu/giờ/IP, idempotent, chỉ tắt (không bật); không áp dụng cho `SECURITY`.
25. `[CONFIRMED]` **D144 — Template:** lưu ở file resource `notification/templates/{category}.{locale}.properties` (không ở DB ở phase này; bảng `notification_templates` trong `PRODUCT_CONTEXT` hoãn đến khi cần quản trị nội dung `[OPEN]`). Biến thế bằng whitelist; escape HTML cho email; không thể chèn nội dung do người dùng kiểm soát vào dòng tiêu đề email (chặn CRLF — header injection).
26. `[CONFIRMED]` **D145 — Cửa sổ lịch hợp nhất:** `GET /calendar?from=&to=` trả Event (đã mở rộng lặp), Time Block, và Task có hạn trong khoảng; tối đa 366 ngày; mỗi mục có `source`. Client offline tính cùng logic từ SQLite. Hợp đồng chung cho cả hai bằng **bộ vector kiểm thử mở rộng lặp** (`docs/api/fixtures/recurrence-vectors.json`) dùng ở Java và Dart.
27. `[CONFIRMED]` **D146 — Giới hạn:** ≤ 5000 event/user, ≤ 10 reminder/target, ≤ 2000 reminder đang hoạt động/user, payload sync 64 KB. Vượt → `REJECTED LIMIT_EXCEEDED` / 422 `LIMIT_EXCEEDED`.
28. `[CONFIRMED]` **D147 — Quyền riêng tư thiết bị:** FCM token coi là dữ liệu nhạy cảm: không log, không đưa vào metric, không trả ra API trừ `GET /devices` (chỉ 6 ký tự cuối).
29. `[CONFIRMED]` **D148 — Metric:** `lifeos.notification.created{category}`, `lifeos.notification.delivery{channel,result}`, `lifeos.notification.delivery.lag` (từ `scheduled_for` đến gửi), `lifeos.notification.reminder.skipped`, `lifeos.notification.reminder.lag`. Cảnh báo khi `lag` p95 > 2 phút hoặc `FAILED` > 5%/giờ.
30. `[CONFIRMED]` **D149 — Thông báo sự kiện từ module khác:** module `finance` (Phase 3) và `habit` (Phase 2) ghi `outbox_events` với `event_type` mới (`BUDGET_THRESHOLD`, `RECURRING_DUE`, `HABIT_STREAK_AT_RISK`...); `notification` đọc payload JSON có **schema cố định tối thiểu** (`userId`, `category`, `templateKey`, `variables` đã lọc whitelist, không chứa số tiền); loại sự kiện chưa biết bị bỏ qua và đếm metric.

## 5. Mô hình dữ liệu (`V6__calendar_notification.sql`, `V7__notification.sql` — tách để dễ revert)

Cột chuẩn đồng bộ có ở `calendar_events`, `event_exceptions`, `time_blocks`, `reminders`. Các bảng `notifications`, `notification_deliveries`, `notification_preferences`, `device_push_tokens` **không** có cột sync (dữ liệu dẫn xuất/thiết bị/tùy chọn); `notification_preferences` đồng bộ qua REST `GET/PUT` không qua `change_logs` (tùy chọn theo thiết bị khác nhau hiếm; chấp nhận GET khi mở màn Cài đặt) `[OPEN]` có nên đưa vào sync cùng `user_settings`.

```sql
CREATE TABLE calendar_events (
  -- cột chuẩn đồng bộ
  title varchar(200) NOT NULL,
  description varchar(2000),
  location varchar(200),
  all_day boolean NOT NULL DEFAULT false,
  timezone varchar(64) NOT NULL,                 -- IANA
  start_local timestamp, end_local timestamp,    -- NULL khi all_day
  start_date date, end_date date,                -- NOT NULL khi all_day
  start_at timestamptz, end_at timestamptz,      -- dẫn xuất UTC (server đặt)
  recurrence jsonb,                              -- NULL = không lặp (schema D126)
  recurrence_until timestamptz,                  -- dẫn xuất, phục vụ lọc
  CHECK ((all_day AND start_date IS NOT NULL AND end_date IS NOT NULL AND end_date >= start_date
          AND start_local IS NULL AND end_local IS NULL)
      OR (NOT all_day AND start_local IS NOT NULL AND end_local IS NOT NULL AND end_local > start_local
          AND start_date IS NULL AND end_date IS NULL))
);
-- INDEX (user_id, start_at) WHERE deleted_at IS NULL AND recurrence IS NULL
-- INDEX (user_id) WHERE deleted_at IS NULL AND recurrence IS NOT NULL

CREATE TABLE event_exceptions (
  event_id uuid NOT NULL REFERENCES calendar_events(id),
  original_start_local timestamp NOT NULL,       -- hoặc date khi all_day (lưu 00:00)
  kind varchar(9) NOT NULL CHECK (kind IN ('CANCELLED','MODIFIED')),
  title varchar(200), description varchar(2000), location varchar(200),
  start_local timestamp, end_local timestamp
);
-- UNIQUE (event_id, original_start_local) WHERE deleted_at IS NULL

CREATE TABLE time_blocks (
  task_id uuid NOT NULL REFERENCES tasks(id),
  start_at timestamptz NOT NULL, end_at timestamptz NOT NULL,
  note varchar(200),
  CHECK (end_at > start_at AND end_at - start_at <= interval '24 hours')
);
-- INDEX (user_id, start_at) WHERE deleted_at IS NULL ; INDEX (task_id) WHERE deleted_at IS NULL

CREATE TABLE reminders (
  target_type varchar(7) NOT NULL CHECK (target_type IN ('TASK','EVENT','HABIT','ROUTINE','NONE')),
  target_id uuid,                                -- NULL khi NONE (không FK: đa hình, kiểm ở service)
  mode varchar(8) NOT NULL CHECK (mode IN ('OFFSET','LOCAL_AT')),
  offset_minutes integer CHECK (offset_minutes BETWEEN 0 AND 525600),
  local_time time, local_date date,              -- LOCAL_AT: giờ địa phương (lặp hằng ngày nếu local_date NULL)
  repeat_rule jsonb,                             -- chỉ cho target NONE hoặc LOCAL_AT
  urgent boolean NOT NULL DEFAULT false,
  status varchar(6) NOT NULL DEFAULT 'ACTIVE' CHECK (status IN ('ACTIVE','DONE','PAUSED')),
  next_fire_at timestamptz,                      -- server đặt
  last_fired_at timestamptz,
  CHECK ((target_type = 'NONE') = (target_id IS NULL))
);
-- INDEX (next_fire_at) WHERE deleted_at IS NULL AND status = 'ACTIVE'

CREATE TABLE notifications (
  id uuid PRIMARY KEY, user_id uuid NOT NULL REFERENCES users(id),
  category varchar(10) NOT NULL, template_key varchar(60) NOT NULL,
  title varchar(200) NOT NULL, body varchar(500) NOT NULL, data jsonb NOT NULL DEFAULT '{}',
  dedupe_key varchar(200) NOT NULL UNIQUE,
  read_at timestamptz, created_at timestamptz NOT NULL, expires_at timestamptz NOT NULL
);
-- INDEX (user_id, created_at DESC) ; INDEX (user_id) WHERE read_at IS NULL ; INDEX (expires_at)

CREATE TABLE notification_deliveries (
  id uuid PRIMARY KEY, notification_id uuid NOT NULL REFERENCES notifications(id),
  channel varchar(6) NOT NULL CHECK (channel IN ('PUSH','EMAIL')),
  device_token_id uuid,                          -- PUSH: dòng thiết bị (SET NULL khi gỡ)
  status varchar(9) NOT NULL CHECK (status IN ('PENDING','IN_FLIGHT','SENT','FAILED','SKIPPED')),
  scheduled_for timestamptz NOT NULL, attempt_count smallint NOT NULL DEFAULT 0,
  next_attempt_at timestamptz NOT NULL, last_error_code varchar(40),
  sent_at timestamptz
);
-- INDEX (next_attempt_at) WHERE status IN ('PENDING','IN_FLIGHT')

CREATE TABLE notification_preferences (
  user_id uuid NOT NULL REFERENCES users(id),
  category varchar(10) NOT NULL, channel varchar(6) NOT NULL, enabled boolean NOT NULL,
  PRIMARY KEY (user_id, category, channel)
);
-- quiet hours và showDetailsOnLockScreen: cột thêm vào user_settings (sync, UPDATE-only theo D88) ở V6 — cần duyệt riêng, xem [OPEN] 3

CREATE TABLE device_push_tokens (
  id uuid PRIMARY KEY, user_id uuid NOT NULL REFERENCES users(id),
  device_id uuid NOT NULL REFERENCES devices(id),   -- devices thuộc identity (§19.2)
  platform varchar(7) NOT NULL CHECK (platform IN ('ANDROID','IOS')),
  token varchar(512) NOT NULL UNIQUE,
  created_at timestamptz NOT NULL, last_seen_at timestamptz NOT NULL
);
```

Ghi chú: quiet hours (`quiet_start`, `quiet_end`) và `show_details_on_lock_screen` thêm vào **`user_settings`** (đã đồng bộ, UPDATE-only theo D88) để mọi thiết bị lập lịch local nhất quán; cập nhật `SYNC_PROTOCOL.md` §3 (khóa payload `user_settings` mới) khi duyệt — đây là thay đổi hợp đồng sync, cần duyệt riêng (xem `[OPEN]` 3).

## 6. API (`/api/v1`)

| Endpoint | Ghi chú |
|---|---|
| `POST/GET /calendar/events`, `GET/PATCH/DELETE /calendar/events/{id}` | `PATCH` đổi chuỗi gốc; `scope` bắt buộc cho thao tác lặp (xem dưới) |
| `POST /calendar/events/{id}/exceptions`, `DELETE …/exceptions/{exceptionId}?version=` | "chỉ lần này" |
| `POST /calendar/events/{id}/split` | "từ lần này": cắt chuỗi gốc (`until`) và tạo chuỗi mới từ lần đó |
| `POST/GET /time-blocks`, `PATCH/DELETE /time-blocks/{id}` | Lọc `taskId, from, to` |
| `GET /calendar?from=&to=` | Cửa sổ hợp nhất D145 |
| `POST/GET /reminders`, `GET/PATCH/DELETE /reminders/{id}`, `POST /reminders/{id}/pause` `/resume` | |
| `GET /notifications?unread=&cursor=`, `GET /notifications/unread-count`, `PATCH /notifications/{id}/read`, `POST /notifications/read-all`, `DELETE /notifications/{id}` | Không `version` (D142) |
| `GET/PUT /notifications/preferences` | `PUT` thay toàn bộ tập (tối đa 6×3 dòng); `SECURITY`×(IN_APP,EMAIL) bị từ chối tắt (`NOTIFICATION_PREFERENCE_LOCKED`, 422) |
| `GET /devices`, `DELETE /devices/{id}` (identity); `PUT /devices/{id}/push-token`, `DELETE /devices/{id}/push-token` (notification) | D139, D147 |
| `GET /notifications/unsubscribe?token=` | Công khai D143 |

Mã lỗi mới `[CONFIRMED]`: `CALENDAR_EVENT_NOT_FOUND`, `CALENDAR_INVALID_RECURRENCE` (422), `CALENDAR_INVALID_TIMEZONE` (422), `CALENDAR_OCCURRENCE_NOT_FOUND` (404/422 khi `originalStart` không khớp quy tắc), `TIME_BLOCK_NOT_FOUND`, `REMINDER_NOT_FOUND`, `REMINDER_TARGET_NOT_FOUND` (422), `NOTIFICATION_NOT_FOUND`, `NOTIFICATION_PREFERENCE_LOCKED` (422), `DEVICE_NOT_FOUND`, `LIMIT_EXCEEDED` (422).

## 7. Đồng bộ

`entityType` mới: `calendar_event`, `event_exception`, `time_block`, `reminder`. `notifications`, `notification_deliveries`, `device_push_tokens`, `notification_preferences` **không** đi qua sync. Quy tắc:
- `calendar_event`: sửa trên hai thiết bị → `CONFLICT` thông thường (Phase 5 giải quyết giao diện). Server tính lại `start_at/end_at/recurrence_until` (client gửi bị bỏ qua).
- Cascade server: xóa Event → ngoại lệ + reminder của nó; xóa Task → time block + reminder của nó (`mutation_id` NULL).
- `reminder`: `next_fire_at`, `last_fired_at`, `status=DONE` do server; client chỉ gửi `mode/offset/local_*/repeat_rule/urgent/paused`.
- Client sau pull lập lại local notification cho 64 mốc gần nhất (D133).

## 8. Bảo mật

- IDOR cho mọi route; `target_id` của reminder kiểm thuộc user; `taskId` của time block thuộc user (cùng 404/`*_NOT_FOUND` 422 như D79).
- `PUT /devices/{id}/push-token`: giới hạn 20/giờ/user; token chuyển chủ phải ghi nhật ký kiểm toán (không log token).
- Email: chặn header injection (D144); `Reply-To` cố định; liên kết hủy đăng ký đúng D143; không chèn dữ liệu người dùng vào URL ngoài domain.
- Không có thông tin nhạy cảm trong push/email (D138); `SensitiveLogIT` mở rộng cho `notification`.
- FCM service account: biến môi trường/secret manager (`SECURITY_RULES`), không commit; xoay khóa được.
- Quy tắc lỗi: không phân biệt token không tồn tại với token của user khác ở `DELETE /devices/{id}` (404).

## 9. Kế hoạch kiểm thử

| ID | Kịch bản | Kỳ vọng |
|---|---|---|
| CAL-01 | Vector mở rộng lặp (daily/weekly byWeekday/monthly ngày 31/yearly 29-2/until/count/DST) chạy Java **và** Dart | Cùng kết quả từng phần tử |
| CAL-02 | Cửa sổ > 366 ngày, > 1000 lần xuất hiện | 422 `VALIDATION_FAILED`/`LIMIT_EXCEEDED` |
| CAL-03 | Ngoại lệ CANCELLED/MODIFIED, split, xóa chuỗi cascade | Đúng D127, tombstone `mutation_id` NULL |
| CAL-04 | Timezone: sự kiện Tokyo xem từ UTC+7, DST nhảy tiến/lùi | Giờ địa phương đúng, D134 |
| CAL-05 | all_day vs có giờ: CHECK DB và validation | Đúng |
| CAL-06 | Time block: ≤ 24h, xóa Task cascade | Đúng |
| CAL-07 | Sync CONFLICT/REJECTED cho 4 entity, mass assignment (`start_at`, `next_fire_at`) bị bỏ qua | Đúng |
| NTF-01 | Reminder job: chạy 2 lần/song song 20 lần → đúng 1 notification mỗi lần (dedupe) | Đúng |
| NTF-02 | Bù lỗi: lỡ < 24h gửi 1 lần; > 24h bỏ + metric | Đúng D131 |
| NTF-03 | Quiet hours qua nửa đêm, SECURITY/urgent bỏ qua, in-app không hoãn | Đúng D135 |
| NTF-04 | Tùy chọn mặc định, khóa SECURITY, thiếu dòng = mặc định | Đúng D136 |
| NTF-05 | Retry backoff, lỗi vĩnh viễn, IN_FLIGHT hết hạn tự khôi phục, FCM giả lập (WireMock) `UNREGISTERED` gỡ token | Đúng D137 |
| NTF-06 | Không gọi mạng trong transaction DB (kiểm bằng test giữ lock và gọi giả chậm) | Không giữ connection khi gọi ngoài |
| NTF-07 | Email: template whitelist, CRLF trong tiêu đề bị chặn, không có dữ liệu nhạy cảm | Đúng D138/D144 |
| NTF-08 | Hủy đăng ký: token hết hạn/giả mạo/SECURITY bị từ chối, idempotent, rate limit | Đúng D143 |
| NTF-09 | Thiết bị: upsert đổi chủ, giới hạn 10, token chỉ lộ 6 ký tự | Đúng D139/D147 |
| NTF-10 | Inbox: cursor, đọc idempotent, dọn 90 ngày theo lô | Đúng D142 |
| NTF-11 | Chuyển `identity` email → `notification`: hành vi cũ (D76) không đổi, ArchUnit mới (`notification` không phụ thuộc nội bộ module khác) | Test cũ vẫn xanh |
| NTF-12 | Sự kiện outbox loại lạ bị bỏ qua + metric; payload thiếu trường → dead-letter, không chặn hàng đợi | Đúng D149 |
| NTF-13 | Log/metric không chứa token FCM, email nội dung, tiêu đề Task | `SensitiveLogIT` |
| NTF-14 | Flutter: khử trùng local/push theo `dedupeKey`; 64 mốc iOS; đổi timezone lập lại lịch | Test Dart |

## 10. Các bước triển khai và tiêu chí hoàn thành

| Bước | Nội dung |
|---|---|
| 4a | Module `notification`: chuyển email handler từ `identity`, orchestrator, preferences, in-app inbox (không FCM) |
| 4b | Thiết bị push, FCM adapter, delivery + retry, quiet hours |
| 4c | Calendar event + lặp + ngoại lệ + vector kiểm thử, `GET /calendar` |
| 4d | Time block, reminder, `ReminderScheduler`, sync 4 entity, cascade |
| 4e | Flutter: lập lịch local, khử trùng, màn hình lịch/inbox/cài đặt; hardening, NTF/CAL |

Hoàn thành khi: `mvn clean verify` xanh, NTF/CAL đầy đủ, thông báo bảo mật không thay đổi hành vi, FCM adapter có chế độ giả lập cho test và cấu hình thật ở môi trường, báo cáo theo mẫu chuẩn.

## 11. Quyết định và câu hỏi mở

D120–D149 như trên. `[OPEN]`: (1) `notification_preferences` có đi qua sync không (khuyến nghị: **không** ở Phase 4, GET/PUT REST); (2) thông báo không-`SECURITY` tới thiết bị không còn họ token hợp lệ (D141); (3) thêm `quiet_start/quiet_end/show_details_on_lock_screen` vào `user_settings` và mở rộng hợp đồng sync `user_settings` (cần duyệt riêng vì đổi `SYNC_PROTOCOL.md` §3.4); (4) bảng `notification_templates` trong DB (khuyến nghị: hoãn, dùng file resource); (5) migrate `reminder_time` của Habit/Routine (Phase 2 D250) sang `reminders`; (6) nhà cung cấp email production (SMTP/SES) và giới hạn gửi/ngày.
