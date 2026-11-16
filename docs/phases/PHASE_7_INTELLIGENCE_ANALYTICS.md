# LifeOS — Phase 7: Life Intelligence & Analytics (đặc tả)

> **Trạng thái:** v1 — **ĐÃ DUYỆT 2026-11-07** (người dùng duyệt `BACKEND_MASTER_PLAN.md`: toàn bộ quyết định D190–D219, quyết định mặc định ở §4 và thay đổi hợp đồng ở §5 của kế hoạch đó). Các mục `[OPEN]` trong tài liệu này được giải quyết bằng bảng §4 của `BACKEND_MASTER_PLAN.md`; khi mâu thuẫn, kế hoạch tổng thể thắng.
> **Nguồn đã chốt:** `PRODUCT_CONTEXT.md` §12 Phase 7; `SYSTEM_ARCHITECTURE.md` §3.12, §9; `DATABASE_RULES.md` §8; `SECURITY_RULES.md`.
> **Phụ thuộc:** Phase 1 (Task) bắt buộc; Phase 3, 4, 6 cho các nguồn dữ liệu tương ứng; Phase 2 (Habit) tùy chọn. Mỗi báo cáo chỉ dùng các module đã tồn tại — module vắng mặt thì phần đó ẩn.
> **Ngoài phạm vi:** AI/dự đoán/gợi ý (Phase 8), xuất báo cáo PDF đẹp, chia sẻ báo cáo, so sánh với người dùng khác, phân tích trên thiết bị của bên thứ ba.

---

## 1. Mục tiêu và phạm vi

Biến dữ liệu đã có thành **cái nhìn tổng hợp đúng và nhanh**: Daily/Weekly/Monthly Review, bảng điều khiển liên miền (Task + Habit + Tài chính + Lịch + Mục tiêu), phân tích xu hướng, và nền tảng vận hành (Redis cache, giám sát, tối ưu SQL, xuất dữ liệu). Nguyên tắc: **con số phải khớp dữ liệu gốc** (kiểm chứng bằng đối chiếu), **mọi tổng hợp tính theo múi giờ người dùng**, **không phát minh chỉ số mơ hồ**, và **hệ thống vẫn đúng khi cache hỏng hoặc Redis tắt**.

Trong phạm vi: review ngày/tuần/tháng (tự tính + ghi chú tự do của người dùng), dashboard tổng quan, analytics theo miền (Task, Habit, Finance, Calendar/thời gian, Goals), so sánh kỳ trước, Redis cache có kiểm soát, giám sát và cảnh báo, tối ưu truy vấn, xuất dữ liệu cá nhân (JSON/CSV), xóa tài khoản và toàn bộ dữ liệu.

## 2. Yêu cầu chức năng

| ID | Yêu cầu | Tiêu chí chấp nhận |
|---|---|---|
| FR-INT-01 | Daily Review | Tóm tắt ngày: Task hoàn thành/quá hạn/dời, Habit làm/bỏ lỡ, giao dịch tổng thu/chi theo tiền tệ, sự kiện đã diễn ra; kèm ô ghi chú phản tư do người dùng viết |
| FR-INT-02 | Weekly/Monthly Review | Tổng hợp tuần (T2–CN hoặc theo `weekStartDay` cài đặt) và tháng lịch; so sánh với kỳ trước |
| FR-INT-03 | Dashboard | Một màn hình: việc hôm nay, Habit hôm nay, ngân sách tháng, sự kiện sắp tới, Goal đang theo dõi; mỗi thẻ độc lập, lỗi một thẻ không làm hỏng cả màn |
| FR-INT-04 | Analytics Task | Số hoàn thành theo ngày/tuần, tỷ lệ đúng hạn, thời gian từ tạo→xong (trung vị), phân bố theo Project/Category |
| FR-INT-05 | Analytics Habit | Tỷ lệ hoàn thành, chuỗi hiện tại/dài nhất, theo thứ trong tuần (nếu Phase 2) |
| FR-INT-06 | Analytics Finance | Xu hướng thu/chi theo tháng, tỷ trọng danh mục, tốc độ chi so với ngân sách; **luôn tách theo tiền tệ** |
| FR-INT-07 | Analytics thời gian | Phân bổ thời gian từ Time Block/Event theo ngày/tuần |
| FR-INT-08 | Analytics Goals | Tiến độ theo thời gian, Goal có nguy cơ trễ hạn (quy tắc tất định §4) |
| FR-INT-09 | Hoạt động offline | Daily Review/Dashboard/analytics cơ bản tính **local** từ SQLite khi mất mạng; số liệu server là nguồn chuẩn khi online |
| FR-INT-10 | Xuất dữ liệu | Người dùng xuất toàn bộ dữ liệu của mình (JSON đầy đủ + CSV từng bảng chính), bất đồng bộ, tải qua liên kết có thời hạn |
| FR-INT-11 | Xóa tài khoản | Xóa tài khoản và toàn bộ dữ liệu theo quy trình có xác nhận và thời gian chờ |
| FR-INT-12 | Giám sát | Dashboard vận hành (metric, alert) cho các job, sync, thông báo, cache, DB |

## 3. Use case chính

| ID | Use case | Luồng và ngoại lệ |
|---|---|---|
| UC-INT-01 | Xem Daily Review lúc 21:00 | Server (hoặc local nếu offline) tính ngày hôm nay theo timezone; người dùng viết phản tư, lưu như entity sync |
| UC-INT-02 | Weekly Review sáng Chủ nhật | Thông báo nhắc (Phase 4, loại `REMINDER`) → mở tổng kết tuần |
| UC-INT-03 | Mở dashboard khi Redis tắt | Truy vấn trực tiếp DB; chậm hơn nhưng đúng; metric ghi nhận `cache.miss` |
| UC-INT-04 | Sửa giao dịch cũ sau khi review tháng đã tạo | Số liệu review tính lại khi mở (review không lưu số liệu, chỉ lưu phản tư) |
| UC-INT-05 | Xuất dữ liệu | Yêu cầu → job nền tạo tệp zip → thông báo `SYSTEM` → liên kết tải hết hạn 24 giờ |
| UC-INT-06 | Xóa tài khoản | Yêu cầu → xác nhận bằng mật khẩu → cooldown 7 ngày có thể hủy → xóa cứng theo lô; thông báo email bảo mật |
| UC-INT-07 | Người dùng đổi timezone | Số liệu ngày/tuần/tháng tính lại theo timezone mới (không có dữ liệu cũ bị sai lệch vĩnh viễn vì không lưu tổng hợp) |

## 4. Quy tắc nghiệp vụ

1. `[CONFIRMED]` **D190 — Tổng hợp luôn tính từ dữ liệu gốc, không lưu số liệu:** Daily/Weekly/Monthly Review không có cột số liệu; chỉ lưu **phản tư người dùng** (`reviews`: kỳ + văn bản). Số liệu tính khi đọc (có cache, D193). Lý do: tránh sai lệch khi dữ liệu cũ được sửa/đồng bộ trễ; không cần đồng bộ số liệu dẫn xuất.
2. `[CONFIRMED]` **D191 — Bảng tổng hợp dẫn xuất chỉ khi đo được cần thiết:** mặc định **không** có bảng tổng hợp (materialized/bảng ngày). Chỉ thêm bảng tổng hợp (ví dụ `finance_daily_totals`) khi benchmark (D207) vượt ngưỡng; khi đó phải: là dẫn xuất xây lại được từ dữ liệu gốc bằng một lệnh, có job đối chiếu, không đồng bộ qua sync, và có tài liệu quyết định riêng.
3. `[CONFIRMED]` **D192 — Ranh giới kỳ theo timezone:** "ngày" = `[00:00, 24:00)` giờ địa phương theo `user_settings.timezone` hiện tại; "tuần" bắt đầu theo `user_settings.weekStartDay` (mặc định thứ Hai); "tháng" = tháng lịch. Truy vấn chuyển ranh giới sang UTC (`AT TIME ZONE`). Giờ DST: ngày có 23/25 giờ vẫn là một ngày. Mọi báo cáo nhận tham số `date|week|month` **theo ngày địa phương**, không nhận mốc UTC tùy ý từ client.
4. `[CONFIRMED]` **D193 — Cache Redis (tùy chọn, chỉ đọc-qua):** cache chỉ cho kết quả **tổng hợp đọc** (dashboard, review, analytics), khóa `lifeos:{env}:agg:{userId}:{kind}:{periodKey}:{schemaVersion}`; TTL ngắn (dashboard 60 s, review/analytics 300 s). **Không** cache dữ liệu nghiệp vụ gốc, không cache token/phiên, không dùng Redis làm nguồn sự thật. Redis tắt/lỗi → bỏ qua cache (fail-open), không lỗi API; timeout 100 ms.
5. `[CONFIRMED]` **D194 — Vô hiệu hóa cache:** ghi thành công của người dùng `u` (qua REST hoặc sync) đặt "dấu thế hệ" `lifeos:{env}:gen:{userId}` (INCR, hoặc xóa theo tiền tố `agg:{userId}:`) **sau khi transaction commit** (`@TransactionalEventListener(AFTER_COMMIT)`), không trong transaction. Sai lệch ngắn hạn (≤ TTL) được chấp nhận và nêu rõ trong UI qua trường `computedAt`. Job nền (recurring Finance, reminder) cũng kích hoạt vô hiệu.
6. `[CONFIRMED]` **D195 — Cô lập dữ liệu trong cache:** khóa luôn chứa `userId`; **không** dùng khóa chung nhiều người dùng; không đặt dữ liệu của user này vào khóa user khác; test IDOR cho lớp cache (cache poisoning chéo user). Giá trị cache không chứa nội dung nhạy cảm tự do (tên, ghi chú) trừ khi cần cho kết quả hiển thị; mã hóa không yêu cầu ở phase này.
7. `[CONFIRMED]` **D196 — Dashboard theo thẻ độc lập:** `GET /dashboard` trả `cards[]`, mỗi thẻ có `status ∈ {OK, ERROR, UNAVAILABLE}` và `data`; truy vấn các thẻ **song song** với timeout 1,5 s/thẻ; thẻ lỗi không làm hỏng phản hồi (HTTP 200 với `status=ERROR` cho thẻ đó). Module chưa kích hoạt trả `UNAVAILABLE`.
8. `[CONFIRMED]` **D197 — Truy vấn qua `*QueryApi` của từng module:** module `analytics` **không** đọc bảng của module khác; gọi `task.api.TaskStatsApi`, `finance.api.FinanceQueryApi`, `habit.api.HabitStatsApi`, `calendar.api.CalendarQueryApi`, `goal.api.GoalQueryApi` trả DTO tổng hợp; ArchUnit: `analytics` chỉ phụ thuộc `..api..` của module khác. (Mở rộng D80: ngoại lệ `TaskQueryApi` ở đó là về cascade, khác với các API thống kê đọc-chỉ ở đây — mỗi API thống kê chỉ đọc, không `@Transactional` ghi.)
9. `[CONFIRMED]` **D198 — Chỉ số Task (định nghĩa chính xác):** `completed(period)` = Task có `completed_at` trong kỳ (chưa xóa); `onTimeRate` = Task hoàn thành có `completed_at ≤ due_end` / tổng Task hoàn thành **có hạn**; `overdueOpen` = Task chưa xong có hạn < hôm nay (giờ địa phương); `medianCycleTime` = trung vị (`completed_at − created_at`) của Task hoàn thành trong kỳ; `null` khi mẫu < 5 (không hiển thị số vô nghĩa). Task xóa mềm không tính. *(Cập nhật 2026-11-15 theo `docs/review/BACKEND_FIX_REPORT.md`: F7 — kỳ có hơn 200.000 Task hoàn thành không bị cắt im lặng nữa: trả 422 `ANALYTICS_RANGE_TOO_LARGE` (áp cho analytics, review và dashboard dùng chung nguồn))*
10. `[CONFIRMED]` **D199 — Chỉ số Finance:** dùng đúng quy tắc Phase 3 (loại trừ chuyển khoản và giao dịch xóa, tách tiền tệ, NUMERIC chính xác, không làm tròn ngầm); `savingsRate = (thu − chi)/thu` chỉ khi `thu > 0`, làm tròn 1 chữ số thập phân **ở lớp hiển thị**; tốc độ chi so với ngân sách = `spent / (limit × tiến độ tháng)` với `tiến độ tháng = ngày đã qua / số ngày của tháng` (≥ 1 = vượt nhịp).
11. `[CONFIRMED]` **D200 — Goal có nguy cơ trễ (quy tắc tất định, không mô hình):** Goal `ACTIVE` có `target_on` và tiến độ `p` (D185 Phase 6) mà `p < elapsedRatio − 0.15` với `elapsedRatio = (hôm nay − start_on)/(target_on − start_on)` (chỉ khi cả hai ngày có và `target_on > start_on`) → `AT_RISK`; quá `target_on` mà chưa xong → `OVERDUE`; nếu không tính được → không gắn nhãn. Ngưỡng 0,15 là hằng số cấu hình `[OPEN]`.
12. `[CONFIRMED]` **D201 — Habit (nếu có):** dùng định nghĩa chuỗi/tỷ lệ của `PHASE_2_HABIT_ROUTINE.md` (D233–D234, D245) làm nguồn duy nhất; Phase này không định nghĩa lại.
13. `[CONFIRMED]` **D202 — Review phản tư:** entity sync `review(period_type ∈ {DAY, WEEK, MONTH}, period_key, reflection text ≤ 5000, mood smallint 1–5 NULL)`; `UNIQUE (user_id, period_type, period_key) WHERE deleted_at IS NULL` (`period_key` = `YYYY-MM-DD` ngày bắt đầu kỳ theo D192); tạo trùng kỳ → 409 `REVIEW_EXISTS`; xung đột sửa → `CONFLICT` văn bản như `note`.
14. `[CONFIRMED]` **D203 — Tính local (offline):** client tính cùng định nghĩa D198–D201 bằng SQL SQLite; **vector kiểm thử chung** (`docs/api/fixtures/analytics-vectors.json`: bộ dữ liệu nhỏ + kết quả mong đợi) chạy ở Java và Dart. Số liệu local có nhãn "ước tính cục bộ" khi chưa đồng bộ xong (`pendingMutations > 0`). Khi online client ưu tiên số server cho kỳ đã đồng bộ.
15. `[CONFIRMED]` **D204 — Xuất dữ liệu:** `POST /account/export` tạo bản ghi `data_exports(status PENDING→RUNNING→READY|FAILED, expires_at)`; job nền (một lần/user đồng thời, ≤ 2 yêu cầu/ngày) đọc theo trang (không giữ transaction dài; dùng snapshot `REPEATABLE READ READ ONLY` mỗi bảng), ghi tệp zip (`data.json` + CSV mỗi bảng chính, UTF-8 có BOM cho CSV) vào kho tạm; liên kết tải ký, 24 giờ, một người dùng; tệp xóa sau hết hạn. Nội dung gồm dữ liệu người dùng, **không** gồm băm mật khẩu, refresh token, token push, `sync_mutations`/`change_logs`. CSV chống **CSV/formula injection** (ô bắt đầu bằng `= + - @` được thêm `'`). *(Cập nhật 2026-11-15 theo `docs/review/BACKEND_FIX_REPORT.md`: F5 — "1 yêu cầu đang chạy" và "2 lần/24 giờ" là giới hạn CỨNG, đúng cả khi gọi đồng thời (khóa dòng `users` + chỉ mục UNIQUE một yêu cầu `PENDING`/`RUNNING` mỗi user, V13). Đã duyệt giữ nguyên: tải qua endpoint xác thực của chủ sở hữu (tối đa 5 lượt, quá ⇒ 410 `EXPORT_EXPIRED`) thay cho URL ký; tệp ở thư mục cục bộ `EXPORT_DIR` khi còn một instance)*
16. `[CONFIRMED]` **D205 — Xóa tài khoản:** `POST /account/deletion` (xác nhận mật khẩu + một lần mã email) → đặt `deletion_scheduled_at = now + 7 ngày`, thu hồi mọi họ refresh token, gửi email bảo mật; trong cooldown đăng nhập lại hiển thị "đang chờ xóa — Hủy"; `DELETE /account/deletion` hủy. Job xóa cứng theo thứ tự con trước cha, theo lô (≤ 1000 dòng/transaction), cuối cùng xóa `users`; ghi **nhật ký kiểm toán không chứa dữ liệu** (`userId` băm + thời điểm). Xuất dữ liệu trước khi xóa được gợi ý. *(Cập nhật 2026-11-15 theo `docs/review/BACKEND_FIX_REPORT.md`: F8 — xóa cứng xóa cả sự kiện outbox còn mang `userId` (cột mới `outbox_events.user_id`, V13), không chỉ sự kiện có aggregate là user)*
17. `[CONFIRMED]` **D206 — Giám sát:** metric chuẩn Micrometer + Prometheus; bảng điều khiển (JSON Grafana đặt ở `ops/grafana/`) gồm: sync (độ trễ, xung đột, 503), jobs (outbox, reminder, recurring, retention: số lượng, lag, thất bại), thông báo (delivery, lag), cache (hit/miss/lỗi), DB (kết nối, chậm), JVM. Cảnh báo tối thiểu được ghi trong tài liệu vận hành (`docs/ops/ALERTS.md`): job không chạy > 3 chu kỳ, DLQ tăng, p95 API > ngưỡng, 5xx > 1%, độ trễ sync > ngưỡng. Mọi nhãn metric là tập hữu hạn (không `userId`, không id thực thể, không nội dung).
18. `[CONFIRMED]` **D207 — Tối ưu SQL có kỷ luật:** mỗi truy vấn tổng hợp có (a) test `EXPLAIN` với bộ dữ liệu lớn (100k Task, 200k giao dịch, 50k ghi chú/user) kiểm không `Seq Scan` trên bảng lớn và có chỉ mục phù hợp; (b) ngưỡng thời gian cố định (dashboard cold < 800 ms p95, review < 500 ms, analytics theo kỳ < 1 s) ghi vào báo cáo nightly (không chặn CI thường); (c) chỉ mục thêm phải có lý do đo được; (d) thay đổi truy vấn không đổi kết quả (đối chiếu với mô hình tham chiếu).
19. `[CONFIRMED]` **D208 — Giới hạn:** `GET` analytics tối đa 24 tháng/366 ngày/lần; `GET /dashboard` 30/phút/user; xuất 2/ngày/user; `reviews` ≤ 1 bản/kỳ; không phân trang trong review (kết quả bị chặn kích thước theo thiết kế: top 10 mỗi nhóm).
20. `[CONFIRMED]` **D209 — Redis vận hành:** cấu hình `maxmemory` + `allkeys-lru`; mật khẩu và TLS ở môi trường thật; không dữ liệu bắt buộc phải sống sót khi Redis mất; bật/tắt bằng cờ cấu hình `lifeos.cache.enabled`; test chạy với Testcontainers Redis **và** với cache tắt.

## 5. Mô hình dữ liệu (`V10__analytics.sql`)

```sql
CREATE TABLE reviews (
  -- cột chuẩn đồng bộ
  period_type varchar(5) NOT NULL CHECK (period_type IN ('DAY','WEEK','MONTH')),
  period_key date NOT NULL,                     -- ngày bắt đầu kỳ theo D192
  reflection text NOT NULL DEFAULT '' CHECK (char_length(reflection) <= 5000),
  mood smallint CHECK (mood BETWEEN 1 AND 5)
);
-- UNIQUE (user_id, period_type, period_key) WHERE deleted_at IS NULL

CREATE TABLE data_exports (
  id uuid PRIMARY KEY, user_id uuid NOT NULL REFERENCES users(id),
  status varchar(7) NOT NULL CHECK (status IN ('PENDING','RUNNING','READY','FAILED','EXPIRED')),
  requested_at timestamptz NOT NULL, finished_at timestamptz, expires_at timestamptz,
  file_ref varchar(200), size_bytes bigint, error_code varchar(40)
);
-- INDEX (user_id, requested_at DESC) ; INDEX (status, requested_at) WHERE status IN ('PENDING','RUNNING')

ALTER TABLE users ADD COLUMN deletion_scheduled_at timestamptz;   -- NULL = bình thường
-- INDEX (deletion_scheduled_at) WHERE deletion_scheduled_at IS NOT NULL
-- Chỉ mục phục vụ tổng hợp: thêm theo kết quả D207, mỗi chỉ mục kèm bằng chứng EXPLAIN trong báo cáo.
```

`data_exports` và `deletion_scheduled_at` không đồng bộ qua sync. Tệp xuất lưu ở kho tạm cục bộ/đối tượng theo cấu hình (`lifeos.export.storage`), không trong DB.

## 6. API (`/api/v1`)

| Endpoint | Ghi chú |
|---|---|
| `GET /dashboard` | D196; trả `computedAt`, `cards[]` |
| `GET /reviews/daily?date=`, `/weekly?date=`, `/monthly?month=` | Số liệu tính + `reflection` hiện có; `date` là ngày địa phương bất kỳ trong kỳ |
| `POST/GET /reviews`, `GET/PATCH/DELETE /reviews/{id}` | Entity phản tư (D202) |
| `GET /analytics/tasks`, `/habits`, `/finance`, `/time`, `/goals` | Tham số `from,to,granularity`; giới hạn D208; `finance` bắt buộc `currency` |
| `POST /account/export`, `GET /account/export/{id}`, `GET /account/export/{id}/download` | D204 |
| `POST /account/deletion`, `DELETE /account/deletion`, `GET /account/deletion` | D205 |

Mã lỗi mới `[CONFIRMED]`: `REVIEW_NOT_FOUND`, `REVIEW_EXISTS` (409), `ANALYTICS_RANGE_TOO_LARGE` (422), `EXPORT_NOT_FOUND`, `EXPORT_LIMIT_REACHED` (429), `EXPORT_NOT_READY` (409), `EXPORT_EXPIRED` (410), `ACCOUNT_DELETION_PENDING` (403 khi gọi API thường trong cooldown — chỉ cho phép xem trạng thái và hủy), `ACCOUNT_DELETION_NOT_FOUND`.

## 7. Đồng bộ

Chỉ `review` là entity sync mới (`entityType: review`). Số liệu dashboard/analytics không đồng bộ (tính theo yêu cầu). Client giữ bảng cache cục bộ tùy chọn `local_aggregates` (xây lại được, không sync). Xóa tài khoản làm vô hiệu mọi token; thiết bị còn lại nhận 401 và hiển thị "tài khoản đang chờ xóa".

## 8. Bảo mật và riêng tư

- IDOR mọi route (kể cả liên kết tải xuất: gắn `userId`, token ký, hết hạn, một lần tải đầu tiên không bắt buộc nhưng giới hạn 5 lần).
- Cache cô lập theo user (D195) có test chéo user; Redis không công khai; khóa/giá trị không chứa mật khẩu/token.
- Xuất dữ liệu: chống CSV injection; kích thước tối đa 500 MB (vượt → `FAILED EXPORT_TOO_LARGE`); không lộ dữ liệu người khác; tệp tạm được xóa an toàn; liên kết tải không dò đoán được (≥ 128 bit ngẫu nhiên + chữ ký).
- Xóa tài khoản: yêu cầu xác nhận mật khẩu + mã email (chống chiếm phiên); cooldown 7 ngày; thu hồi token; nhật ký kiểm toán không chứa dữ liệu cá nhân.
- Log/metric không chứa số liệu cá nhân (số tiền, số Task, nội dung); chỉ đếm tổng hợp toàn hệ thống.
- Tuân thủ quyền cá nhân (truy cập, xóa, di chuyển dữ liệu): đáp ứng bởi D204/D205; chính sách quyền riêng tư pháp lý `[OPEN]` ngoài phạm vi tài liệu kỹ thuật.

## 9. Kế hoạch kiểm thử

| ID | Kịch bản | Kỳ vọng |
|---|---|---|
| INT-01 | Review ngày/tuần/tháng khớp mô hình tham chiếu viết độc lập (property-based trên dữ liệu ngẫu nhiên nhiều miền) | Khớp tuyệt đối |
| INT-02 | Ranh giới kỳ theo timezone (UTC+7, UTC-8, DST nhảy tiến/lùi, đổi timezone giữa kỳ, `weekStartDay` Chủ nhật) | Đúng D192 |
| INT-03 | Định nghĩa chỉ số Task (mẫu < 5 ⇒ null, xóa mềm không tính, quá hạn theo giờ địa phương) | Đúng D198 |
| INT-04 | Finance analytics: tách tiền tệ, loại trừ chuyển khoản/xóa, `savingsRate`, tốc độ chi | Đúng D199 |
| INT-05 | Goal `AT_RISK/OVERDUE` các ca biên (không có ngày, `target_on = start_on`) | Đúng D200 |
| INT-06 | Dashboard: một thẻ lỗi/timeout/UNAVAILABLE không làm hỏng phản hồi; thẻ song song | Đúng D196 |
| INT-07 | Cache: hit/miss/hết hạn; Redis tắt giữa chừng (fail-open); vô hiệu sau commit (không trước) | Đúng D193/D194 |
| INT-08 | Cache chéo user (khóa, vô hiệu, tiền tố) | Không rò rỉ (D195) |
| INT-09 | Số liệu sau ghi + đọc ngay (nhất quán đọc-ghi trong ≤ TTL; có `computedAt`) | Đúng |
| INT-10 | Client–server: vector `analytics-vectors.json` chạy Java và Dart | Cùng kết quả |
| INT-11 | `review`: UNIQUE theo kỳ, trùng → 409, sync CONFLICT văn bản | Đúng D202 |
| INT-12 | Xuất dữ liệu: đầy đủ bảng, loại trừ bí mật, CSV injection, hết hạn liên kết, giới hạn 2/ngày, người khác không tải được | Đúng D204 |
| INT-13 | Xuất dữ liệu lớn (200k giao dịch): không giữ transaction dài, bộ nhớ chặn | Đạt ngưỡng; ghi kết quả |
| INT-14 | Xóa tài khoản: xác nhận, cooldown, hủy, xóa cứng đúng thứ tự con-trước-cha, mọi bảng sạch (kiểm tự động theo danh mục bảng có `user_id`) | Không còn dòng nào |
| INT-15 | Bảng mới thêm sau này phải được đăng ký vào danh sách xóa/xuất (test kiểm tra mọi bảng có `user_id` đều thuộc một trong hai danh sách xuất/xóa hoặc danh sách miễn trừ có lý do) | Test thất bại khi thiếu |
| INT-16 | `EXPLAIN` + ngưỡng thời gian D207 trên dữ liệu lớn | Ghi báo cáo nightly |
| INT-17 | Giám sát: mọi metric D206 tồn tại, nhãn hữu hạn (kiểm tĩnh không có `userId` làm nhãn) | Đúng |
| INT-18 | Log/metric/lỗi không chứa số liệu cá nhân | `SensitiveLogIT` mở rộng |
| INT-19 | ArchUnit: `analytics` chỉ phụ thuộc `..api..` module khác | Đúng D197 |

## 10. Các bước triển khai và tiêu chí hoàn thành

| Bước | Nội dung |
|---|---|
| 7a | Các `*StatsApi`/`*QueryApi` từng module, mô hình tham chiếu kiểm thử, định nghĩa chỉ số |
| 7b | Review (số liệu + `reviews` sync), vector Java/Dart, Daily/Weekly/Monthly |
| 7c | Dashboard theo thẻ + analytics miền + so sánh kỳ trước |
| 7d | Redis cache fail-open + vô hiệu + test cô lập |
| 7e | Xuất dữ liệu, xóa tài khoản, danh mục bảng cho xuất/xóa |
| 7f | Giám sát, cảnh báo, tối ưu SQL theo D207, hardening, INT-01..19 |

Hoàn thành khi: `mvn clean verify` + `flutter test` xanh; INT đầy đủ; hệ thống đúng khi `lifeos.cache.enabled=false`; mọi bảng có `user_id` được bao phủ bởi xuất/xóa (INT-15); báo cáo theo mẫu chuẩn.

## 11. Quyết định và câu hỏi mở

D190–D209 như trên (D210–D219 để dành). `[OPEN]` cần người dùng quyết: (1) có dùng Redis ở Phase 7 hay hoãn (khuyến nghị: **chỉ khi đo được cần thiết** theo D207; D193–D195, D209 chỉ kích hoạt nếu duyệt); (2) ngưỡng `AT_RISK` 0,15 (D200); (3) thời hạn cooldown xóa tài khoản (khuyến nghị 7 ngày) và có yêu cầu mã email (khuyến nghị có); (4) kho lưu tệp xuất (đĩa cục bộ vs đối tượng/S3) và kích thước tối đa; (5) xuất CSV có BOM hay không (khuyến nghị có, để Excel đọc đúng tiếng Việt); (6) `weekStartDay` và `defaultCurrency` **đã có** trong `user_settings` (Giai đoạn 3 backend, `GET /me/settings`) — không cần đổi hợp đồng sync cho Phase 7; cần xác nhận quy ước giá trị `weekStartDay` (1=thứ Hai … 7=Chủ nhật?) trước khi code; (7) chính sách lưu giữ nhật ký kiểm toán xóa tài khoản.
