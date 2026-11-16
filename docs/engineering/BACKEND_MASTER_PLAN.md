# LifeOS — Kế hoạch tổng thể hoàn thiện Backend (một tài liệu, một lần chạy)

> **Trạng thái:** v1 — **ĐÃ DUYỆT 2026-11-07** (người dùng trả lời "duyệt" cho toàn bộ kế hoạch, không loại trừ mục nào). Mọi quyết định D100–D259 của `docs/phases/*`, các quyết định mặc định ở §4 và các thay đổi hợp đồng ở §5 là `[CONFIRMED]`.
> **Người thực hiện:** Claude Code. **Người rà soát:** người dùng (+ Claude ở vai trò tài liệu) **một lần** ở báo cáo cuối.
> **Đã xong (không làm lại):** Giai đoạn 0–3 backend (khung, Identity, Project/Category/Task, Sync push/pull) — 493 test xanh, xem `docs/review/BACKEND_PHASE3_SYNC_REPORT.md`.

---

## 1. Cách dùng tài liệu này

Làm **liên tục từ WP1 đến WP8** trong một phiên làm việc dài, **không dừng để xin duyệt giữa chừng**. Tài liệu này quy định thứ tự, quyết định mặc định, giới hạn quyền tự chủ và cách báo cáo. Chi tiết nghiệp vụ nằm ở các tài liệu được dẫn; khi có mâu thuẫn, thứ tự ưu tiên: **tài liệu này (§4–§5) > đặc tả phase (`docs/phases/*`) > tài liệu `[CONFIRMED]` trước đây trong `docs/engineering` và `docs/architecture` > `AGENTS.md`**. Ngoại lệ: các điểm mâu thuẫn **không** được §5 cho phép sửa thì **dừng** (§6).

Không thuộc phạm vi: ứng dụng Flutter (kể cả phần client của Phase 4/5 như lập lịch local notification, màn hình xung đột, full resync phía client), Phase 8 (AI).

## 2. Các gói công việc (làm đúng thứ tự)

| WP | Nội dung | Đặc tả | Ghi chú thứ tự |
|---|---|---|---|
| WP1 | Giai đoạn 4: outbox worker, gửi email (mailpit), job dọn, `docker compose` | `SYSTEM_ARCHITECTURE.md` §3.11, §3.12, §12, D23 | **Bỏ qua nếu Claude Code đã hoàn tất việc này**; nếu đã xong chỉ ghi vào tiến độ |
| WP2 | Phase 2 Habit & Routine (bước H1–H4) | `docs/phases/PHASE_2_HABIT_ROUTINE.md` | Hoàn tất MVP backend |
| WP3 | Phase 3 Personal Finance (3a–3e) | `docs/phases/PHASE_3_FINANCE.md` | Cập nhật `DATABASE_RULES`, `API_CONVENTIONS` về tiền (§5 #8) |
| WP4 | Phase 4 Calendar & Notification, phần **server** (4a–4d) | `docs/phases/PHASE_4_CALENDAR_NOTIFICATION.md` | Tạo bảng `devices` (identity); chuyển `OutboxEventHandler` email sang `notification`; migrate `reminder_time` của Habit/Routine sang `reminders` |
| WP5 | Phase 5 Sync nâng cao, phần **server** (5a, phần server của 5d, 5e) | `docs/phases/PHASE_5_SYNC_ADVANCED.md` | Chỉ: `min_retained_seq`, retention jobs, `410`, snapshot + entities, thiết bị (`DELETE /devices/{id}` đã có từ WP4), chẩn đoán, job nhất quán, metric. **Không** làm 5b, 5c |
| WP6 | Phase 6 Knowledge & Goals (6a–6d) | `docs/phases/PHASE_6_KNOWLEDGE_GOALS.md` | Mở rộng `task` (goalId, milestoneId), payload `note` 192 KB |
| WP7 | Phase 7 Analytics (7a–7c, 7e, 7f; 7d theo §4) | `docs/phases/PHASE_7_INTELLIGENCE_ANALYTICS.md` | Danh mục bảng xuất/xóa phải bao phủ **mọi** bảng của WP2–WP6 |
| WP8 | Hoàn thiện: OpenAPI + fixtures, benchmark, rà soát toàn bộ, báo cáo cuối | §7, §8 | |

Mỗi WP kết thúc khi `mvn -f backend/pom.xml clean verify` **xanh** và các test ID của đặc tả WP đó đều có test tên chứa ID (như `Sync_NN_…`). Chỉ khi đó mới sang WP kế tiếp.

## 3. Nguyên tắc làm việc tự chủ

1. **Không xin duyệt giữa chừng.** Quyết định chi tiết mà tài liệu không nêu: chọn phương án đơn giản, an toàn, nhất quán với mã hiện có; ghi vào `docs/review/BACKEND_PROGRESS.md` mục "Quyết định phát sinh" (nhãn `[PROPOSED]`), không dừng.
2. **Cập nhật `docs/review/BACKEND_PROGRESS.md` sau mỗi WP:** trạng thái, số test (unit + IT), thời gian `verify`, thay đổi lớn, quyết định phát sinh, việc hoãn. Đây là nhật ký, không phải điểm chờ duyệt.
3. **Không làm yếu kiểm thử:** không `@Disabled`, không nới ngưỡng, không xóa test để qua. Chỉ sửa test cũ khi hợp đồng được §5 cho phép thay đổi, và ghi rõ test nào, vì sao.
4. **Không sửa migration đã có (V1…V cuối của giai đoạn trước).** Chỉ thêm migration mới, đánh số kế tiếp; số `V…` trong đặc tả phase chỉ là minh họa.
5. **Giữ mọi bất biến đã chốt:** thứ tự khóa D86, `ChangeRecorder.record` cuối transaction, cascade `mutation_id` NULL, `@Transactional` chỉ ở `..service..`, module chỉ phụ thuộc nhau qua `api/`, ArchUnit A1–A13 (+ luật mới nếu đặc tả yêu cầu), không dữ liệu nhạy cảm trong log/metric/lỗi/push/email, không gọi mạng ngoài trong transaction DB, test đồng thời lặp ≥ 20 lần, không `Thread.sleep`.
6. **Mỗi bảng mới có `user_id`** phải được ghi vào danh mục xuất/xóa tài khoản ngay khi tạo (WP7 INT-15 sẽ kiểm); danh sách này tạo từ WP2, không để dồn đến WP7.
7. **Mỗi entity sync mới** có handler (`*.sync.*`), bản ghi trong ma trận `SYNC_PROTOCOL.md`, test SYN-tương-đương (CONFLICT/REJECTED/DUPLICATE/tombstone/cascade) và đưa vào `AuthorizationMatrixIT`/IDOR tự quét.
8. **Mỗi WP cập nhật tài liệu gốc liên quan trong cùng WP** (OpenAPI xuất lại, `API_CONVENTIONS`, `DATABASE_DESIGN`, `SYNC_PROTOCOL`, `AGENTS.md` §5–6, `SYSTEM_ARCHITECTURE`), không để lệch.
9. Khi một WP vượt hợp lý (ví dụ `verify` > 10 phút), đề xuất `@Tag("slow")` + chạy hằng đêm trong `ci/` thay vì cắt kiểm thử.

## 4. Quyết định mặc định cho mọi mục `[OPEN]` (được duyệt cùng tài liệu này)

| Phase | Mục `[OPEN]` | Quyết định |
|---|---|---|
| 2 | Đổi lịch habit ảnh hưởng quá khứ | Chấp nhận hạn chế: dùng lịch hiện tại cho toàn bộ lịch sử |
| 2 | Routine có chuỗi | Không |
| 2 | Check-in trùng ngày giữa hai thiết bị | Duyệt D235: `CONFLICT` lý do `ALREADY_EXISTS` kèm `serverSnapshot` |
| 2 | Dung sai +1 ngày, check-in bù không giới hạn | Giữ như D231 |
| 2 | `reminder_time` tạm trên habit/routine | Giữ; migrate sang `reminders` ở WP4 |
| 2 | `GET /my-day` phía server | Không làm |
| 3 | Giao dịch "Điều chỉnh" | Tính vào số dư; **không** tính vào ngân sách và báo cáo thu/chi (D119) |
| 3 | Hạn mức bản ghi/user | Không giới hạn |
| 3 | Nhập/xuất CSV Finance | Không làm riêng; nằm trong xuất dữ liệu WP7 |
| 3 | Hiện số tiền trên màn hình khóa | Tắt, không có tùy chọn bật ở phase này |
| 4 | `notification_preferences` qua sync | Không; REST `GET/PUT` |
| 4 | Thông báo không-`SECURITY` tới thiết bị hết họ token hợp lệ | Không chặn (giữ D141 như viết) |
| 4 | Cột quiet hours, `show_details_on_lock_screen` | Thêm vào `user_settings` (duyệt thay đổi §5 #1) |
| 4 | Bảng `notification_templates` | Không; dùng file resource |
| 4 | Nhà cung cấp email production | Spring Mail/SMTP cấu hình qua biến môi trường; SES không bắt buộc |
| 5 | Retention | `change_logs` 180 ngày (+≥1000 seq), `sync_mutations` 35 ngày, tombstone 180 ngày |
| 5 | Xung đột `user_settings` | Việc của client; server không đổi |
| 5 | Khóa UNIQUE định kỳ Finance vs xóa cứng tombstone | Giữ thiết kế Phase 3; xóa cứng giao dịch bị chặn khi dòng định kỳ còn tồn tại; không tạo bảng phụ |
| 5 | Hồi sinh id cũ | Không |
| 5 | Bảng thiết bị | Một bảng `devices` (identity), tạo ở WP4 |
| 6 | `unaccent` + cấu hình `simple` | Duyệt |
| 6 | Phân trang tìm kiếm | Offset ≤ 1000 cho tìm kiếm; cursor cho danh sách thường |
| 6 | Tương đương FTS5/PG | Như D183 (phía client; server chỉ cung cấp vector kiểm thử nếu cần) |
| 6 | Habit trong % tiến độ Goal | Không |
| 6 | Giới hạn ghi chú/user | Không giới hạn |
| 7 | Redis | **Không triển khai Redis ở lần này.** Làm lớp trừu tượng cache với bản no-op (D193–D195, D209 chỉ ở mức interface + cờ `lifeos.cache.enabled=false` mặc định); chỉ thêm Redis nếu benchmark D207 vượt ngưỡng và ghi vào báo cáo |
| 7 | Ngưỡng `AT_RISK` | 0,15 (cấu hình được) |
| 7 | Xóa tài khoản | Cooldown 7 ngày, yêu cầu mật khẩu **và** mã xác nhận qua email |
| 7 | Kho tệp xuất | Thư mục cục bộ cấu hình được, tối đa 500 MB/tệp |
| 7 | CSV có BOM | Có |
| 7 | `weekStartDay` | Đã có trong `user_settings`; dùng đúng quy ước của mã hiện tại, **không đổi** |
| 7 | Lưu giữ nhật ký kiểm toán xóa tài khoản | 365 ngày |

## 5. Thay đổi hợp đồng đã chốt được phép thực hiện (và chỉ những thay đổi này)

| # | Thay đổi | WP | Cập nhật tài liệu cùng WP |
|---|---|---|---|
| 1 | Thêm `quiet_start`, `quiet_end`, `show_details_on_lock_screen` vào `user_settings` + payload sync | 4 | `SYNC_PROTOCOL.md` §3, OpenAPI, `API_CONVENTIONS` §8.2 |
| 2 | Thêm `goalId`, `milestoneId` vào `task` | 6 | `SYNC_PROTOCOL.md` §3, OpenAPI, fixtures |
| 3 | Giới hạn payload riêng cho `note` 192 KB (body ≤ 1 MB, 50 mutation không đổi) | 6 | `SYNC_PROTOCOL.md` §8 |
| 4 | Retention, `410 SYNC_FULL_RESYNC_REQUIRED`, snapshot | 5 | `SYNC_RULES.md`, `SYNC_PROTOCOL.md` §6, `SYSTEM_ARCHITECTURE.md` §3.12 |
| 5 | Chuyển `OutboxEventHandler` email sang module `notification` | 4 | `SYSTEM_ARCHITECTURE.md`, ArchUnit; hành vi email (D76) không đổi, test cũ vẫn xanh |
| 6 | Bảng chiến lược xung đột theo entity (Finance luôn hỏi) | 3, 5 | `SYNC_RULES.md` §9.5 |
| 7 | Extension `unaccent` | 6 | `DATABASE_RULES.md` |
| 8 | Quy ước tiền (`NUMERIC(19,4)`, JSON chuỗi, SQLite ×10⁴) | 3 | `DATABASE_RULES.md`, `API_CONVENTIONS.md`, `FLUTTER_RULES.md` |
| 9 | Lý do `ALREADY_EXISTS` cho `CONFLICT` của `CREATE` | 2 | `SYNC_PROTOCOL.md` §4 |
| 10 | Đăng nhập nhận `deviceName`/`platform` tùy chọn, tạo `devices` | 4 | `API_CONVENTIONS`, `SECURITY_RULES`, OpenAPI |

Bất kỳ thay đổi nào **ngoài** danh sách trên đối với hợp đồng/quy tắc `[CONFIRMED]` (API hiện có, ma trận sync, bảo mật, ArchUnit hiện có, schema V1…) là **không được phép tự quyết**: xử lý theo §6.

## 6. Khi nào phải dừng (và chỉ khi đó)

Dừng, ghi rõ vào `BACKEND_PROGRESS.md` và nhắn người dùng ngay trong phiên khi:
1. Hai tài liệu mâu thuẫn mà §4/§5 không giải quyết được.
2. Cần một thay đổi hợp đồng `[CONFIRMED]` ngoài §5.
3. Một quyết định không thể hoàn tác (xóa dữ liệu thật, đổi khóa chính, migration phá dữ liệu) mà hai hướng đều hợp lý.
4. Một kiểm thử bắt buộc vẫn đỏ sau 3 lần sửa nguyên nhân gốc (không được làm yếu kiểm thử để qua).
5. Môi trường (Docker, mạng, đĩa) khiến không thể chạy `verify`.

Ngoài các trường hợp trên: **tiếp tục làm**, không hỏi.

## 7. Chuẩn hoàn thành chung cho mỗi WP

- `mvn -f backend/pom.xml clean verify` xanh; 0 `@Disabled`; ArchUnit xanh (kể cả luật mới của đặc tả).
- Mọi test ID trong đặc tả WP đó có test tương ứng; bảng ánh xạ ID → lớp test đưa vào báo cáo cuối.
- IDOR/mass assignment cho mọi route mới (`AuthorizationMatrixIT`, `IdorMatrixIT` tự quét); `SensitiveLogIT` mở rộng cho dữ liệu mới.
- Đồng thời: test lặp ≥ 20 lần cho cascade/job/khóa, ổn định 3 lượt liên tiếp.
- `docs/api/openapi.yaml` xuất lại + `ApiFixtureContractIT` cho endpoint/entity mới; fixture mới trong `docs/api/fixtures/` (gồm các vector dùng chung Java–Dart: `habit-vectors.json`, `recurrence-vectors.json`, `merge-vectors.json`, `analytics-vectors.json` khi đặc tả yêu cầu).
- EXPLAIN test cho truy vấn tổng hợp/lọc/tìm kiếm theo đặc tả; kết quả ghi vào báo cáo.
- Migration mới chạy sạch từ DB rỗng và từ DB có dữ liệu Giai đoạn 3 (`FlywayMigrationIT`).

## 8. WP8 — Hoàn thiện và báo cáo cuối

1. Chạy toàn bộ `verify` 3 lần liên tiếp; ghi số test, thời gian, test chậm nhất.
2. Benchmark (không chặn CI): pull 500 thay đổi, push 50 mutation, snapshot 50k bản ghi, các truy vấn D207; đưa số đo vào báo cáo.
3. Rà soát chéo: với từng đặc tả phase, tự đối chiếu danh sách yêu cầu (FR-*), quy tắc (D*) và test ID; liệt kê mục chưa làm kèm lý do (mục này phải trống hoặc có lý do chính đáng).
4. Kiểm tra danh mục xuất/xóa tài khoản bao phủ mọi bảng có `user_id`.
5. Cập nhật: `AGENTS.md` §5–6 (trạng thái, số test, bản đồ tài liệu), `SYSTEM_ARCHITECTURE.md` §12, `BACKEND_SKELETON_PLAN.md`, `DATABASE_DESIGN.md` (ERD/DDL tới migration cuối), `API_CONVENTIONS.md` (catalog mã lỗi mới), `SYNC_PROTOCOL.md`.
6. Viết **`docs/review/BACKEND_FINAL_REPORT.md`** (một báo cáo duy nhất): tổng quan, bảng WP (số test, thời gian), danh sách migration, endpoint/entity sync mới, ánh xạ test ID → lớp, thay đổi hợp đồng đã thực hiện (§5) và test cũ đã sửa, quyết định phát sinh `[PROPOSED]`, lỗi/rủi ro phát hiện, điểm cần người xem kỹ, việc hoãn/chưa làm (gồm Redis, client Flutter, CI GitHub), số đo hiệu năng.

## 9. Ghi chú vận hành

- Nếu Giai đoạn 4 (WP1) đang chạy dở ở một phiên khác, hoàn tất nó trước khi bắt đầu WP2; không làm trùng `OutboxEventHandler` (WP4 mới là nơi chuyển module).
- `ci/backend-ci.yml` và `.github/` do người dùng quản lý; chỉ cập nhật `ci/` nếu cần (ví dụ job nightly) và nêu trong báo cáo.
- Docker/Testcontainers cần có; nếu không có, dừng theo §6.5.

## 10. Câu duyệt cần có từ người dùng

> **"Tôi duyệt BACKEND_MASTER_PLAN.md: toàn bộ D100–D259, các quyết định mặc định ở §4 và các thay đổi hợp đồng ở §5."**

Chỉ sau câu này mới đổi nhãn `[PROPOSED]` thành `[CONFIRMED]` trong `docs/phases/*` và cho phép Claude Code bắt đầu. Có thể duyệt một phần (ví dụ "trừ §4 dòng Redis") — khi đó nêu rõ mục loại trừ.
