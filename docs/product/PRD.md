# LifeOS — PRD Phase 1: Foundation & Task Management

> **Trạng thái:** v1 — đã duyệt (2026-11-05). `[CONFIRMED]` là đã chốt; quyết định ở §12. Chỉ còn Q-12 chưa chốt (ngoài Phase 1).
> **Nguồn:** `docs/product/PRODUCT_CONTEXT.md`, `docs/engineering/*`.
> **Deliverable của Phase 1:** Quản lý Task online/offline. Phase 1 thuộc MVP (MVP = Phase 1–2).

---

## 1. Mục tiêu và phạm vi

### 1.1. Mục tiêu

1. Người dùng đăng ký, đăng nhập và dùng LifeOS trên Android và iOS.
2. Người dùng quản lý Task, Project, Category; mọi thao tác cốt lõi chạy được **khi không có Internet**.
3. Dữ liệu đồng bộ an toàn với backend qua push/pull tối thiểu, đúng giao thức đã chốt (idempotent, version, cursor `seq`, tombstone).
4. Dựng nền tảng kỹ thuật để các phase sau không phải viết lại: schema đồng bộ, module boundary, outbox, observability cơ bản.

### 1.2. Trong phạm vi (In scope)

| Nhóm | Nội dung |
|---|---|
| Identity | Đăng ký, xác minh email, đăng nhập, đăng xuất, refresh token, quên/đặt lại mật khẩu, xem/sửa hồ sơ, đổi mật khẩu |
| Thiết bị | Đăng ký thiết bị, xem danh sách, thu hồi thiết bị |
| User Settings | `timezone`, `locale`, `default_currency`, `week_start_day` |
| Task | Tạo, xem, sửa, xóa, hoàn thành, mở lại, độ ưu tiên, hạn, phân loại, lọc, tìm kiếm, sắp xếp |
| Project | Tạo, xem, sửa, lưu trữ, xóa, gắn Task, xem tiến độ |
| Category | Tạo, xem, sửa, xóa danh mục Task |
| Offline | Lưu local SQLite, hàng đợi thay đổi (outbox), push/pull tối thiểu, trạng thái sync, đồng bộ thủ công, đánh dấu conflict |
| Thông báo | Local notification cơ bản nhắc hạn Task |
| Nền tảng | Spring Boot modular monolith, PostgreSQL, Flyway, outbox gửi email, Actuator |

### 1.3. Ngoài phạm vi Phase 1 (Out of scope)

- Habit, Routine, Calendar, Finance, Note, Goal, Dashboard, Life Review, AI (các phase sau).
- Module Notification đầy đủ: push FCM, in-app notification, preferences, quiet hours, reminder phía server (Phase 4).
- UI giải quyết conflict, đồng bộ nhiều thiết bị phức tạp, full resync, recovery nâng cao (Phase 5).
- Subtask, phụ thuộc giữa Task, Task lặp (recurrence), ước lượng thời gian (chốt trước Phase 2).
- Đăng nhập mạng xã hội. Redis, RabbitMQ. Microservices.

`[CONFIRMED]` Cũng để ngoài Phase 1: Bulk Update Tasks, Task History, Reschedule như chức năng riêng (đổi hạn đã nằm trong Update Task), Delete Account, mã hóa SQLite at rest, export dữ liệu.

---

## 2. Người dùng và bối cảnh

- **Actor chính:** Personal User đã đăng ký (baseline §3.1). Không có Admin/Staff.
- **Nền tảng:** Android + iOS (Flutter). Giao diện chỉ tiếng Việt; chuỗi nằm trong file localization `[CONFIRMED]`.
- **Một người dùng có thể dùng nhiều thiết bị**, nhưng Phase 1 chỉ đảm bảo một thiết bị hoạt động ổn định; nhiều thiết bị hoạt động đúng ở mức giao thức, chưa tối ưu trải nghiệm (Phase 5).

### 2.1. User stories chính

| ID | Là người dùng, tôi muốn… | Để… |
|---|---|---|
| US-01 | tạo tài khoản và xác minh email | dữ liệu của tôi gắn với tài khoản riêng |
| US-02 | đăng nhập và giữ đăng nhập | không phải nhập lại mỗi lần mở app |
| US-03 | ghi nhanh một việc cần làm | không quên việc |
| US-04 | đặt hạn, độ ưu tiên và danh mục cho việc | biết việc nào quan trọng và gấp |
| US-05 | gom việc vào một Project | theo dõi tiến độ theo từng dự án |
| US-06 | xem việc theo bộ lọc, tìm kiếm, sắp xếp | tìm đúng việc cần làm |
| US-07 | dùng app khi mất mạng | không bị gián đoạn công việc |
| US-08 | dữ liệu tự đồng bộ khi có mạng | không phải làm thủ công |
| US-09 | nhận nhắc nhở khi việc đến hạn | không trễ hạn |
| US-10 | xem và thu hồi các thiết bị đã đăng nhập | giữ an toàn tài khoản |

---

## 3. Yêu cầu chức năng

Mỗi yêu cầu có ID để truy vết sang test. "AC" là tiêu chí chấp nhận.

### 3.1. Identity & Account (Module 01)

| ID | Yêu cầu | AC |
|---|---|---|
| FR-ID-01 | **Đăng ký** bằng email và mật khẩu | Email chưa tồn tại; mật khẩu tối thiểu 8 ký tự (Q-01); tạo `users` + `user_settings`; `user_sync_state` được tạo lười ở lần ghi `change_logs` đầu tiên (D21), tức ngay trong transaction đăng ký khi ghi `user_settings`; gửi email xác minh qua outbox; không tiết lộ email đã tồn tại hay chưa qua thông điệp lỗi `[CONFIRMED]` |
| FR-ID-02 | **Xác minh email** | Token một lần, hash trong DB, có hạn; xác minh xong tài khoản được kích hoạt; cho phép gửi lại email xác minh |
| FR-ID-03 | **Đăng nhập** | Chỉ tài khoản đã xác minh; trả access token (15 phút) và refresh token (30 ngày); gắn thiết bị (`device_id`); giới hạn 5 lần sai/15 phút theo tài khoản + IP (Q-01) |
| FR-ID-04 | **Refresh token** | Xoay vòng: cấp refresh mới, vô hiệu token cũ; dùng lại token cũ bị từ chối `[CONFIRMED]` và vô hiệu cả chuỗi token của thiết bị đó |
| FR-ID-05 | **Đăng xuất** | Vô hiệu refresh token của thiết bị hiện tại; cảnh báo nếu còn thay đổi chưa sync, không tự xóa dữ liệu local chưa sync (Q-09) |
| FR-ID-06 | **Quên và đặt lại mật khẩu** | Token một lần, hash, có hạn; đặt lại xong vô hiệu mọi refresh token `[CONFIRMED]` |
| FR-ID-07 | **Xem và sửa hồ sơ** | Sửa tên hiển thị; email không đổi trong Phase 1 `[CONFIRMED]` |
| FR-ID-08 | **Đổi mật khẩu** | Yêu cầu mật khẩu cũ; thành công vô hiệu refresh token của các thiết bị khác `[CONFIRMED]` |
| FR-ID-09 | **Đăng ký thiết bị** | Mỗi lần đăng nhập tạo/cập nhật `devices` (tên, nền tảng, lần cuối hoạt động) |
| FR-ID-10 | **Xem và thu hồi thiết bị** | Thu hồi vô hiệu refresh token của thiết bị đó; thiết bị bị thu hồi phải đăng nhập lại |
| FR-ID-11 | **User Settings** | Xem/sửa `timezone` (IANA), `locale`, `default_currency` (mặc định VND), `week_start_day`; timezone tự nhận từ thiết bị ở lần đăng nhập đầu, đổi tay được `[CONFIRMED]`; là entity đồng bộ |

### 3.2. Task (Module 02)

| ID | Yêu cầu | AC |
|---|---|---|
| FR-TK-01 | **Tạo Task** (quick capture) | Chỉ bắt buộc `title`; ID UUID do client tạo; hoạt động offline; nằm trong "Inbox" (không Project) nếu không chọn Project `[CONFIRMED]` |
| FR-TK-02 | **Xem Task** | Xem chi tiết và danh sách; dữ liệu đọc từ SQLite |
| FR-TK-03 | **Sửa Task** | Sửa tiêu đề, mô tả, hạn, ưu tiên, danh mục, Project; kiểm tra `version` (optimistic locking) |
| FR-TK-04 | **Xóa Task** | Soft delete (tombstone), không `DELETE` vật lý; Task đã xóa không xuất hiện trong danh sách |
| FR-TK-05 | **Hoàn thành / Mở lại** | Hoàn thành ghi `completed_at`; mở lại xóa `completed_at`; trạng thái `OPEN` ↔ `DONE` (Q-03) |
| FR-TK-06 | **Độ ưu tiên** | Một giá trị trong tập cố định (Q-03) |
| FR-TK-07 | **Hạn (due date)** | Có thể bỏ trống; `due_date` + `due_time` tùy chọn (Q-04); tính theo timezone user |
| FR-TK-08 | **Phân loại** | Gán tối đa một Category `[CONFIRMED]` |
| FR-TK-09 | **Lọc** | Theo trạng thái, Project, Category, ưu tiên, khoảng hạn (hôm nay, quá hạn, sắp tới) |
| FR-TK-10 | **Tìm kiếm** | Theo tiêu đề và mô tả; chạy trên SQLite local (offline) |
| FR-TK-11 | **Sắp xếp** | Theo hạn, ưu tiên, ngày tạo, ngày cập nhật |

### 3.3. Project (Module 02)

| ID | Yêu cầu | AC |
|---|---|---|
| FR-PJ-01 | **Tạo, xem, sửa Project** | Bắt buộc `name`; mô tả tùy chọn; UUID do client tạo; hoạt động offline |
| FR-PJ-02 | **Lưu trữ (archive) Project** | Project lưu trữ ẩn khỏi danh sách mặc định, vẫn xem được; Task bên trong giữ nguyên |
| FR-PJ-03 | **Xóa Project** | Soft delete; Task thuộc Project chuyển về Inbox (Q-06) |
| FR-PJ-04 | **Gắn / bỏ gắn Task** | Một Task thuộc tối đa một Project; đổi Project là một lần sửa Task |
| FR-PJ-05 | **Xem tiến độ** | `GET /projects/{id}/progress` do module `task` tính: số Task, số đã hoàn thành, tỉ lệ; offline tính từ SQLite `[CONFIRMED]` (D2) |

### 3.4. Category

| ID | Yêu cầu | AC |
|---|---|---|
| FR-CT-01 | Tạo, xem, sửa, xóa Category của Task | Tên duy nhất theo user (bỏ qua bản ghi đã xóa); xóa Category thì Task chuyển về "không danh mục" `[CONFIRMED]`; không có Category mặc định (Q-07) |

### 3.5. Offline & Synchronization (Module 10, phạm vi Phase 1)

| ID | Yêu cầu | AC |
|---|---|---|
| FR-SY-01 | **Dữ liệu offline** | Mọi chức năng ở §3.2–§3.4 hoạt động không Internet; UI chỉ đọc từ SQLite `[CONFIRMED]` |
| FR-SY-02 | **Hàng đợi thay đổi (outbox)** | Mỗi thao tác ghi: một local transaction ghi bảng chính **và** thêm mutation (`mutationId`, `entityType`, `entityId`, `operation`, `baseVersion`, `payload`) |
| FR-SY-03 | **Push** | Gửi mutation theo thứ tự; server xử lý theo từng mutation, trả `APPLIED / DUPLICATE / CONFLICT / REJECTED`; gửi lại cùng `mutationId` không tạo trùng |
| FR-SY-04 | **Pull** | Gửi cursor, nhận thay đổi theo `seq` và `hasMore`; ghi vào SQLite; tombstone được áp dụng |
| FR-SY-05 | **Retry** | Lỗi mạng/tạm thời: thử lại với backoff; chạy lại an toàn |
| FR-SY-06 | **Trạng thái sync** | Người dùng thấy: đã đồng bộ / đang chờ (số thay đổi) / lỗi / có conflict |
| FR-SY-07 | **Đồng bộ thủ công** | Nút "Đồng bộ ngay" |
| FR-SY-08 | **Conflict (Phase 1)** | Khi `CONFLICT`: đánh dấu `sync_state = conflict`, giữ bản local, **không ghi đè**, hiện thông báo đơn giản `[CONFIRMED]`. UI chọn bản làm ở Phase 5 |
| FR-SY-09 | **Mutation phụ thuộc** | Tạo Project rồi Task trong Project: gửi theo thứ tự; nếu mutation cha bị `REJECTED/CONFLICT`, tạm dừng mutation phụ thuộc |

### 3.6. Local notification cơ bản

| ID | Yêu cầu | AC |
|---|---|---|
| FR-NT-01 | Nhắc khi Task đến hạn | Flutter đặt local notification theo hạn Task (đúng giờ nếu có `due_time`, 08:00 nếu chỉ có ngày — Q-05); hoạt động offline |
| FR-NT-02 | Cập nhật lịch khi dữ liệu đổi | Sửa hạn, hoàn thành, xóa Task, hoặc nhận thay đổi từ pull → hủy/đặt lại thông báo tương ứng |
| FR-NT-03 | Quyền thông báo | Xin quyền hệ điều hành; từ chối thì app vẫn dùng bình thường và hiển thị gợi ý bật lại |
| FR-NT-04 | Không có backend reminder | Phase 1 chưa có bảng `reminders` và FCM; thuộc Phase 4 (`notification` sở hữu `reminders`) |

---

## 4. Mô hình dữ liệu Phase 1

Mọi bảng đồng bộ có cột chuẩn theo `DATABASE_RULES.md` §8.2: `id` (UUID), `user_id`, `version`, `deleted_at`, `created_at`, `updated_at`.

| Bảng | Module | Đồng bộ | Ghi chú |
|---|---|---|---|
| `users` | identity | không | email, mật khẩu hash, trạng thái xác minh |
| `user_settings` | identity | có | timezone, locale, default_currency, week_start_day |
| `devices` | identity | không | tên, nền tảng, last_seen |
| `refresh_tokens` | identity | không | hash, `device_id`, hạn, xoay vòng |
| `email_verification_tokens`, `password_reset_tokens` | identity | không | hash, một lần, hạn |
| `categories` | task | có | `name`; duy nhất theo user |
| `projects` | project | có | `name`, mô tả, `archived_at` |
| `tasks` | task | có | xem §4.1 |
| `sync_mutations` | sync | không | `UNIQUE (user_id, mutation_id)` |
| `change_logs` | sync | không | `(user_id, seq)` PK |
| `user_sync_state` | sync | không | `last_seq` |
| `outbox_events` | common | không | gửi email xác minh/reset; worker đọc bằng polling PostgreSQL, **không cần RabbitMQ** `[CONFIRMED]` |

### 4.1. Trường của Task

| Trường | Kiểu | Ghi chú |
|---|---|---|
| `title` | text, bắt buộc | tối đa 200 ký tự (Q-10) |
| `description` | text, tùy chọn | tối đa 10.000 ký tự (Q-10) |
| `status` | enum | `OPEN`, `DONE` (Q-03) |
| `priority` | enum | `NONE`, `LOW`, `MEDIUM`, `HIGH` (Q-03) |
| `due_date`, `due_time` | `DATE`, `TIME` NULL | theo timezone user (Q-04) |
| `completed_at` | `TIMESTAMPTZ` NULL | |
| `project_id` | UUID NULL | FK `projects`; tham chiếu bằng ID `[CONFIRMED]` |
| `category_id` | UUID NULL | FK `categories` |

Local SQLite mirror các cột trên, thêm `sync_state` (`synced | pending | conflict`) chỉ ở client `[CONFIRMED]`.

---

## 5. API Phase 1 (tổng quan)

> Định dạng chi tiết (versioning, phân trang, lỗi) sẽ nằm ở `API_CONVENTIONS.md`. Lỗi theo RFC 9457 `[CONFIRMED]`. `/api/v1` và danh sách dưới là `[CONFIRMED]`.

| Nhóm | Endpoint |
|---|---|
| Auth | `POST /api/v1/auth/register`, `/verify-email`, `/resend-verification`, `/login`, `/refresh`, `/logout`, `/forgot-password`, `/reset-password` |
| Account | `GET/PATCH /api/v1/me`, `POST /me/change-password`, `GET/PATCH /me/settings` |
| Devices | `GET /api/v1/devices`, `DELETE /devices/{id}` |
| Sync | `POST /api/v1/sync/push`, `GET /api/v1/sync/pull?cursor=` |
| Task / Project / Category | CRUD REST cho tập dữ liệu; `GET /projects/{id}/progress` ở module task |

Luồng đồng bộ là đường ghi chính của app; REST CRUD vẫn tồn tại và **phải đi qua cùng Application Service và `ChangeRecorder`** `[CONFIRMED]`.

---

## 6. Yêu cầu phi chức năng

| Mảng | Yêu cầu |
|---|---|
| Offline | CRUD cốt lõi không cần Internet `[CONFIRMED]` |
| Hiệu năng | API p95 < 300 ms cho CRUD thường trong môi trường thử nghiệm; là định hướng, xác nhận bằng benchmark `[CONFIRMED]` |
| Bảo mật | JWT; ownership validate ở backend; `user_id` từ security context; trả 404 với dữ liệu của user khác; không log token/mật khẩu |
| Cô lập dữ liệu | User A không đọc/sửa được dữ liệu user B ở mọi endpoint |
| Độ tin cậy | Idempotent theo `mutationId`; không mất dữ liệu đã được server xác nhận |
| Tính nhất quán | Business change + `sync_mutations` + `change_logs` + (nếu có) `outbox_events` trong cùng transaction |
| Quan sát | Log có cấu trúc kèm correlation ID; Actuator health/metrics; Prometheus/Grafana có thể để sau Phase 1 `[CONFIRMED]` |
| Bản địa hóa | Chỉ tiếng Việt, chuỗi nằm trong localization `[CONFIRMED]` |
| Triển khai | Docker Compose chạy được backend + PostgreSQL ở local |
| Kiểm thử | Theo `CODE_GUIDELINES.md` §13: unit, integration (Testcontainers), ArchUnit, sync tests, security tests, Flutter tests |

---

## 7. Luồng và trường hợp biên quan trọng

1. **Tạo rồi xóa khi chưa sync:** outbox có thể gộp/hủy cặp tạo–xóa; nếu đã gửi thì server nhận tạo rồi xóa, tombstone được đồng bộ. Không được để lại bản ghi mồ côi.
2. **Gửi lại sau mất kết nối giữa chừng:** mutation đã `APPLIED` nhưng client chưa nhận phản hồi → gửi lại cùng `mutationId` nhận `DUPLICATE` kèm kết quả cũ.
3. **Sửa offline trên hai thiết bị:** thiết bị đến sau nhận `CONFLICT`, giữ bản local, đánh dấu conflict (FR-SY-08).
4. **Tạo Task trong Project vừa tạo offline:** mutation Task phụ thuộc mutation Project (FR-SY-09).
5. **Refresh token hết hạn khi offline lâu:** dùng offline vẫn được; khi online cần đăng nhập lại, dữ liệu local chưa sync **không bị xóa**.
6. **Đổi timezone:** hạn đã lưu không bị viết lại; lịch local notification được đặt lại theo timezone mới.
7. **Thu hồi thiết bị đang có mutation chưa gửi:** các mutation đó không được server chấp nhận; hành vi phía client theo Q-09.
8. **Xóa Category/Project đang được dùng:** theo FR-CT-01 / Q-06.
9. **Cursor ngoài hạn lưu trữ:** full resync là Phase 5; Phase 1 chưa có retention nên chưa xảy ra.

---

## 8. Kiến trúc và ràng buộc kỹ thuật áp dụng

- Modular Monolith; module Phase 1: `common`, `identity`, `project`, `task`, `sync`; hàng đợi gửi email qua outbox `[CONFIRMED]`.
- Phụ thuộc module theo `JAVA_SPRING_RULES.md` §4.4: `task → project`; `sync` gọi Application Service qua `MutationHandler`.
- Flutter: Riverpod, Drift, UI đọc từ SQLite, ghi + outbox cùng một local transaction (`FLUTTER_RULES.md`).
- Sync theo `SYNC_RULES.md`; cursor `seq` theo user.
- Ngoại lệ so với guideline cần ADR.

---

## 9. Tiêu chí hoàn thành Phase 1 (demo)

1. Đăng ký, xác minh email, đăng nhập trên thiết bị Android và iOS.
2. Tắt mạng: tạo, sửa, hoàn thành, xóa Task; tạo Project/Category; lọc, tìm kiếm, sắp xếp đều hoạt động.
3. Bật mạng: thay đổi tự đồng bộ; gửi lại không tạo trùng; xóa được đồng bộ (tombstone).
4. Hai thiết bị cùng sửa một Task: thiết bị sau thấy trạng thái conflict, **không mất dữ liệu**.
5. Task đến hạn nhắc bằng local notification kể cả khi offline; sửa/hoàn thành/xóa Task cập nhật thông báo.
6. Thu hồi một thiết bị thì thiết bị đó phải đăng nhập lại.
7. Test tự động: ownership giữa hai user, idempotency, version conflict, tombstone, pull nhiều trang, kiểm tra module boundary.
8. Backend + PostgreSQL chạy bằng Docker Compose.

---

## 10. Truy vết với baseline

| Phase 1 trong baseline §12 | Phủ bởi |
|---|---|
| Spring Boot foundation, PostgreSQL | §6, §8 |
| Authentication, User and Device | FR-ID-* |
| Flutter + SQLite, offline local persistence | FR-SY-01..02 |
| CRUD Task, Project and Category | FR-TK-*, FR-PJ-*, FR-CT-* |
| Basic local notification | FR-NT-* |
| (Quyết định review) sync tối thiểu, cột chuẩn, `change_logs` | FR-SY-03..09, §4 |

---

## 11. Rủi ro

| Rủi ro | Giảm thiểu |
|---|---|
| Phase 1 nặng hơn dự kiến do có sync tối thiểu và auth đầy đủ | Cắt phạm vi theo §1.3; ưu tiên Task/Project online-offline trước, auth hoàn thiện song song |
| Outbox gửi email cần Email Provider ngay từ đầu | Dùng provider/sandbox ở môi trường dev (Q-08) |
| Một developer, ba nền tảng (backend, Android, iOS) | Cố định phạm vi, dùng guideline và test tự động làm hàng rào |
| Lệch múi giờ làm sai "hôm nay"/hạn | Quy tắc timezone §8.1 `DATABASE_RULES.md`; test với nhiều timezone |

---

## 12. Quyết định đã duyệt

| # | Quyết định | Kết quả `[CONFIRMED]` |
|---|---|---|
| Q-01 | Giới hạn đăng nhập, mật khẩu, token | 5 lần sai/15 phút theo tài khoản + IP; mật khẩu tối thiểu 8 ký tự; token xác minh email 24 giờ; token reset mật khẩu 1 giờ |
| Q-02 | OS tối thiểu | Theo mức tối thiểu của phiên bản Flutter chọn, chốt khi khởi tạo project |
| Q-03 | Trạng thái và ưu tiên Task | Trạng thái `OPEN`, `DONE`; ưu tiên `NONE`, `LOW`, `MEDIUM`, `HIGH` |
| Q-04 | Hạn Task | Cặp `due_date` (DATE) + `due_time` (TIME, NULL được), theo timezone user |
| Q-05 | Thời điểm nhắc | Có giờ: nhắc đúng giờ; chỉ có ngày: nhắc 08:00 sáng ngày đó |
| Q-06 | Xóa Project | Task thuộc Project chuyển về Inbox (bỏ `project_id`), không bị xóa |
| Q-07 | Category mặc định | Không tạo sẵn |
| Q-08 | Email Provider | SMTP sandbox ở dev; chọn provider prod trước khi phát hành |
| Q-09 | Đăng xuất / thu hồi thiết bị | Đăng xuất cảnh báo nếu còn thay đổi chưa sync, cho đồng bộ trước, không tự xóa dữ liệu local chưa sync. Thu hồi: chặn sync, giữ dữ liệu local đến khi đăng nhập lại |
| Q-10 | Độ dài tối đa | `title` ≤ 200; `description` ≤ 10.000 ký tự |
| Q-11 | Delete Account | Ngoài Phase 1 |
| Q-12 | Reminder lặp (wall-clock hay instant) | Chưa chốt, không thuộc Phase 1; chốt trước Phase 4 |

---

## 13. Lịch sử thay đổi

| Phiên bản | Ngày | Ghi chú |
|---|---|---|
| Draft 1 | 2026-11-05 | Bản đầu |
| v1 | 2026-11-05 | Duyệt toàn bộ đề xuất Q-01..Q-11 |
