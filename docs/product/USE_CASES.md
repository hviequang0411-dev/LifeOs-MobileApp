# LifeOS — Use Cases Phase 1

> **Trạng thái:** v1 — đã duyệt 2026-11-05.
> **Nguồn:** `PRD.md` (v1). Mỗi use case ghi `FR` tương ứng để truy vết.
> **Quy ước:** Actor mặc định là *Người dùng* (Personal User). *Hệ thống* gồm app Flutter + backend. "Offline" nghĩa là không có Internet; mọi thao tác ghi đều đi qua SQLite + outbox (trừ khi nói khác).

Mã lỗi (`errorCode`) chỉ minh họa, chốt chi tiết trong `API_CONVENTIONS.md`.

---

## A. Identity & Account

### UC-ID-01 Đăng ký tài khoản (FR-ID-01)
- **Tiền điều kiện:** có Internet.
- **Luồng chính:** 1) Nhập email, mật khẩu, tên hiển thị. 2) App gửi yêu cầu. 3) Backend kiểm tra định dạng và chính sách mật khẩu (≥ 8 ký tự). 4) Tạo `users` (chưa xác minh), `user_settings` (timezone lấy từ thiết bị, VND; ghi `change_logs` nên `user_sync_state` được tạo lười theo D21), và `outbox_events` gửi email xác minh, trong cùng một transaction. 5) App báo "kiểm tra email".
- **Ngoại lệ:** email sai định dạng → 400; mật khẩu yếu → 400 kèm `fieldErrors`; email đã tồn tại → phản hồi **giống** trường hợp thành công (không lộ email tồn tại); mất mạng → báo cần Internet, không tạo gì.
- **Hậu điều kiện:** tài khoản tồn tại, chưa đăng nhập được.

### UC-ID-02 Xác minh email (FR-ID-02)
- **Luồng chính:** Người dùng mở liên kết trong email → backend kiểm tra token (hash, một lần, trong 24 giờ) → đánh dấu đã xác minh → đăng nhập được.
- **Thay thế:** *Gửi lại email xác minh* — vô hiệu token cũ, tạo token mới qua outbox.
- **Ngoại lệ:** token hết hạn/đã dùng/sai → báo lỗi và gợi ý gửi lại.

### UC-ID-03 Đăng nhập (FR-ID-03, FR-ID-09, FR-ID-11)
- **Tiền điều kiện:** có Internet; email đã xác minh.
- **Luồng chính:** 1) Nhập email + mật khẩu. 2) Backend xác thực, tạo/cập nhật `devices`, cấp access token (15 phút) + refresh token (30 ngày, lưu hash). 3) App lưu token trong secure storage. 4) Lần đầu trên thiết bị: nhận `user_settings`; nếu chưa đặt timezone thì app gửi timezone của thiết bị. 5) App chạy pull đầu tiên (cursor `0`).
- **Ngoại lệ:** sai thông tin → 401 (không nói sai email hay mật khẩu); chưa xác minh → báo và cho gửi lại; quá 5 lần sai/15 phút → tạm khóa theo tài khoản + IP, báo thời gian chờ.

### UC-ID-04 Làm mới token (FR-ID-04)
- **Luồng chính:** access token hết hạn → app gọi refresh với refresh token → backend cấp cặp mới, vô hiệu refresh cũ (xoay vòng).
- **Ngoại lệ:** refresh token cũ bị dùng lại → vô hiệu chuỗi token của thiết bị, yêu cầu đăng nhập lại. Refresh hết hạn/thiết bị bị thu hồi → yêu cầu đăng nhập lại; **dữ liệu local chưa sync được giữ nguyên**.
- **Ghi chú:** khi offline, app vẫn dùng bình thường; refresh chỉ xảy ra khi online.

### UC-ID-05 Đăng xuất (FR-ID-05)
- **Luồng chính:** Người dùng chọn Đăng xuất → nếu còn thay đổi chưa sync, app cảnh báo và đề nghị đồng bộ trước → vô hiệu refresh token của thiết bị → xóa token local.
- **Thay thế:** đăng xuất khi offline: cảnh báo còn thay đổi chưa sync; nếu vẫn tiếp tục, xóa token local và đánh dấu refresh token chờ thu hồi lần sau có mạng. Không tự xóa dữ liệu local chưa sync.

### UC-ID-06 Quên và đặt lại mật khẩu (FR-ID-06)
- **Luồng chính:** nhập email → backend (luôn phản hồi thành công) gửi email chứa token (1 giờ, một lần) qua outbox → người dùng đặt mật khẩu mới → backend vô hiệu mọi refresh token của tài khoản.
- **Ngoại lệ:** token hết hạn/đã dùng → báo lỗi; mật khẩu mới yếu → 400.

### UC-ID-07 Đổi mật khẩu (FR-ID-08)
- **Luồng chính:** nhập mật khẩu cũ + mới → backend kiểm tra → cập nhật, vô hiệu refresh token của các thiết bị khác, giữ thiết bị hiện tại.
- **Ngoại lệ:** mật khẩu cũ sai → 400/401; mật khẩu mới yếu → 400.

### UC-ID-08 Xem và sửa hồ sơ, cài đặt (FR-ID-07, FR-ID-11)
- **Luồng chính:** xem/sửa tên hiển thị và `timezone`, `locale`, `default_currency`, `week_start_day`. Dữ liệu `user_settings` là entity đồng bộ → sửa được offline.
- **Quy tắc:** email không đổi được. Đổi timezone không viết lại hạn đã lưu; lịch local notification được đặt lại (UC-NT-02).
- **Ngoại lệ:** timezone không hợp lệ (không thuộc IANA) → 400.

### UC-ID-09 Xem và thu hồi thiết bị (FR-ID-10)
- **Luồng chính:** xem danh sách (tên, nền tảng, hoạt động gần nhất, đánh dấu "thiết bị này") → chọn Thu hồi → backend vô hiệu refresh token của thiết bị.
- **Hậu điều kiện:** thiết bị bị thu hồi không sync được, phải đăng nhập lại; dữ liệu local của thiết bị đó được giữ đến khi đăng nhập lại (Q-09).
- **Ngoại lệ:** thu hồi thiết bị không thuộc user → 404.

---

## B. Task

### UC-TK-01 Tạo Task nhanh (FR-TK-01, FR-SY-01..02)
- **Luồng chính:** 1) Nhập `title` (bắt buộc ≤ 200 ký tự). 2) Tùy chọn: mô tả, hạn (`due_date` + `due_time`), ưu tiên, Category, Project. 3) App tạo UUID, ghi vào SQLite **và** thêm mutation vào outbox trong một local transaction. 4) Task hiện ngay trong danh sách (`sync_state = pending`). 5) Khi online, sync đẩy lên server (UC-SY-02).
- **Mặc định:** không chọn Project → nằm trong Inbox; ưu tiên `NONE`; trạng thái `OPEN`.
- **Ngoại lệ:** `title` rỗng/quá dài → báo lỗi tại form; Category/Project đã bị xóa → không cho chọn.
- **Hậu điều kiện:** Task tồn tại local; sau sync tồn tại trên server với `version = 1`.

### UC-TK-02 Xem danh sách, lọc, tìm kiếm, sắp xếp (FR-TK-02, 09, 10, 11)
- **Luồng chính:** mở danh sách → đọc từ SQLite → áp dụng bộ lọc (trạng thái, Project, Category, ưu tiên, hạn: hôm nay/quá hạn/sắp tới), tìm theo tiêu đề và mô tả, sắp xếp theo hạn/ưu tiên/ngày tạo/ngày cập nhật.
- **Quy tắc:** "hôm nay" tính theo timezone của user; Task đã xóa không hiện; mặc định ẩn Task thuộc Project đã lưu trữ `[xem UC-PJ-03]`; hoạt động hoàn toàn offline.
- **Ngoại lệ:** không có kết quả → trạng thái rỗng kèm gợi ý xóa bộ lọc.

### UC-TK-03 Xem chi tiết Task (FR-TK-02)
- **Luồng chính:** chọn Task → xem mọi trường, trạng thái sync, Project, Category.
- **Thay thế:** Task ở trạng thái `conflict` → hiển thị thông báo conflict (UC-SY-04).

### UC-TK-04 Sửa Task (FR-TK-03, FR-TK-07, FR-TK-08)
- **Luồng chính:** sửa trường bất kỳ (kể cả đổi hạn, Project, Category) → app ghi SQLite + outbox (mutation `UPDATE` kèm `baseVersion` là `version` server đã biết) → UI cập nhật ngay → sync sau.
- **Hậu quả:** nếu hạn đổi, lịch local notification được cập nhật (UC-NT-02).
- **Ngoại lệ:** server trả `CONFLICT` (Task bị sửa ở nơi khác) → UC-SY-04; validate thất bại ở server (`REJECTED`) → đánh dấu lỗi, giữ bản local, báo người dùng.

### UC-TK-05 Hoàn thành và mở lại Task (FR-TK-05)
- **Luồng chính:** *Hoàn thành:* `status = DONE`, ghi `completed_at`. *Mở lại:* `status = OPEN`, xóa `completed_at`. Mỗi lần là một mutation.
- **Hậu quả:** hoàn thành hủy local notification của Task; mở lại đặt lại nếu hạn còn hiệu lực.
- **Quy tắc:** Task đã `DONE` vẫn xem/sửa được.

### UC-TK-06 Xóa Task (FR-TK-04)
- **Luồng chính:** xác nhận xóa → đánh dấu `deleted_at` (tombstone) trong SQLite + mutation `DELETE` → Task biến khỏi danh sách → sync đẩy tombstone lên server.
- **Trường hợp biên:** Task tạo offline và xóa trước khi sync → có thể hủy cả hai mutation chưa gửi; nếu đã gửi, server nhận tạo rồi xóa. Không để bản ghi mồ côi.
- **Hậu quả:** hủy local notification của Task.
- **Ngoại lệ:** server trả `CONFLICT` (Task bị sửa ở nơi khác trước khi xóa) → UC-SY-04.

---

## C. Project và Category

### UC-PJ-01 Tạo và sửa Project (FR-PJ-01)
- **Luồng chính:** nhập `name` (bắt buộc), mô tả → ghi SQLite + outbox → hiện ngay → sync sau.
- **Ngoại lệ:** tên rỗng → báo lỗi. Tạo Project rồi tạo Task trong Project khi offline: mutation Task phụ thuộc mutation Project (FR-SY-09).

### UC-PJ-02 Gắn và bỏ gắn Task vào Project (FR-PJ-04)
- **Luồng chính:** trong UC-TK-04, chọn hoặc bỏ Project của Task. Task thuộc tối đa một Project.
- **Ngoại lệ:** Project đã xóa/không thuộc user → server `REJECTED` (`PROJECT_NOT_FOUND`).

### UC-PJ-03 Lưu trữ Project (FR-PJ-02)
- **Luồng chính:** chọn Lưu trữ → ghi `archived_at` → Project ẩn khỏi danh sách mặc định, vẫn xem được trong mục "Đã lưu trữ"; Task bên trong giữ nguyên.
- **Thay thế:** Bỏ lưu trữ → xóa `archived_at`.

### UC-PJ-04 Xóa Project (FR-PJ-03)
- **Luồng chính:** xác nhận (nêu số Task bị ảnh hưởng) → Project bị soft delete; Task thuộc Project **chuyển về Inbox**, không bị xóa.
- **Quy tắc:** client chỉ gửi **một** mutation `DELETE` Project. Server cascade trong cùng transaction: bỏ `project_id` của các Task còn sống, tăng `version` và ghi `change_logs` cho từng Task. Client cập nhật local để UI nhất quán và nhận version mới qua pull. Nếu người dùng sửa một Task bị ảnh hưởng trước khi pull, mutation đó nhận `CONFLICT` (UC-SY-04): bản local được giữ nguyên, không bị ghi đè và không tự đánh dấu đã đồng bộ; người dùng xử lý theo UC-SY-04.

### UC-PJ-05 Xem tiến độ Project (FR-PJ-05)
- **Luồng chính:** mở Project → xem tổng số Task, số đã hoàn thành, tỉ lệ → offline tính từ SQLite; online có thể gọi `GET /projects/{id}/progress`.
- **Quy tắc:** Task đã xóa không tính. Project không có Task → hiển thị 0/0, không chia cho 0.

### UC-CT-01 Quản lý Category (FR-CT-01)
- **Luồng chính:** tạo/sửa/xóa Category (tên duy nhất theo user, bỏ qua bản ghi đã xóa) → ghi SQLite + outbox.
- **Quy tắc:** không có Category mặc định. Xóa Category → server cascade: Task thuộc nó chuyển về "không danh mục" (client không gửi mutation riêng cho các Task đó, như UC-PJ-04).
- **Ngoại lệ:** tên trùng → báo lỗi tại form; trùng phát sinh khi sync (hai thiết bị tạo cùng tên) → server `REJECTED` (`CATEGORY_NAME_TAKEN`), client đánh dấu lỗi và báo người dùng.

---

## D. Offline & Synchronization

### UC-SY-01 Dùng app khi offline (FR-SY-01..02)
- **Luồng chính:** mọi thao tác ở nhóm B, C chạy bình thường, đọc/ghi SQLite; mỗi lần ghi thêm mutation vào outbox; chỉ báo trạng thái "đang chờ đồng bộ (N thay đổi)".
- **Ngoại lệ (cần Internet):** đăng ký, đăng nhập, xác minh email, quên mật khẩu, thu hồi thiết bị, đổi mật khẩu.

### UC-SY-02 Đẩy thay đổi lên server — Push (FR-SY-03, FR-SY-05)
- **Kích hoạt:** có mạng trở lại, mở app, sau mỗi thao tác ghi khi online, hoặc đồng bộ thủ công.
- **Luồng chính:** 1) Sync engine lấy mutation theo thứ tự. 2) Gửi lô `{mutationId, entityType, entityId, operation, baseVersion, payload}`. 3) Server xử lý **từng mutation** (xác thực → ownership → idempotency → kiểm tra `baseVersion` → business rules → thực thi → tăng `version` → ghi `sync_mutations` + `change_logs` cùng transaction). 4) Trả kết quả từng mutation. 5) Client cập nhật: `APPLIED` → cập nhật `version`, `sync_state = synced`, xóa khỏi outbox; `DUPLICATE` → coi như `APPLIED` bằng kết quả cũ; `CONFLICT` → UC-SY-04; `REJECTED` → đánh dấu lỗi kèm `errorCode`.
- **Ngoại lệ:** mất mạng giữa chừng → giữ mutation, thử lại với backoff, gửi lại cùng `mutationId` an toàn; 401 → UC-ID-04; mutation cha bị `REJECTED/CONFLICT` → tạm dừng mutation phụ thuộc (FR-SY-09).
- **Hậu điều kiện:** không có mutation nào được áp dụng hai lần.

### UC-SY-03 Kéo thay đổi từ server — Pull (FR-SY-04)
- **Luồng chính:** 1) Client gửi cursor đã lưu (`0` lần đầu). 2) Server trả thay đổi có `seq > cursor` theo thứ tự, kèm `nextCursor`, `hasMore`. 3) Client áp dụng trong một local transaction (tạo/sửa/đánh dấu xóa), lưu cursor mới. 4) Lặp lại khi `hasMore`.
- **Quy tắc:** thay đổi tới một entity đang có mutation chờ gửi → để UC-SY-04 xử lý khi push, không ghi đè bản local chưa gửi. Cập nhật lịch local notification cho Task thay đổi (UC-NT-02).
- **Ngoại lệ:** lỗi giữa chừng → không lưu cursor của trang lỗi, thử lại trang đó.

### UC-SY-04 Xử lý conflict ở Phase 1 (FR-SY-08)
- **Kích hoạt:** server trả `CONFLICT` (`baseVersion` ≠ `version` hiện tại).
- **Luồng chính:** client đặt `sync_state = conflict`, **giữ nguyên bản local**, không ghi đè bản server hay bản local, hiện thông báo đơn giản ("có thay đổi xung đột") và biểu tượng trên Task.
- **Ngoài phạm vi Phase 1:** màn chọn bản/hợp nhất (Phase 5).
- **Hậu điều kiện:** không mất dữ liệu; Task conflict không bị đẩy tiếp cho đến khi được xử lý (Phase 5).

### UC-SY-05 Xem trạng thái và đồng bộ thủ công (FR-SY-06, FR-SY-07)
- **Luồng chính:** xem trạng thái (đã đồng bộ / đang chờ N thay đổi / lỗi / có conflict) → bấm "Đồng bộ ngay" → chạy UC-SY-02 rồi UC-SY-03.
- **Ngoại lệ:** không có mạng → thông báo "chưa có kết nối".

---

## E. Local notification

### UC-NT-01 Nhắc khi Task đến hạn (FR-NT-01, FR-NT-03)
- **Luồng chính:** Task có hạn và `OPEN` → app đặt local notification: đúng `due_time` nếu có, nếu chỉ có `due_date` thì 08:00 sáng ngày đó, theo timezone user → đến lúc hiển thị thông báo, chạm vào mở Task.
- **Quyền:** lần đầu xin quyền thông báo; từ chối → app dùng bình thường, hiển thị gợi ý bật lại ở cài đặt.
- **Ngoại lệ:** hạn đã qua khi tạo → không đặt thông báo.

### UC-NT-02 Cập nhật lịch thông báo khi dữ liệu đổi (FR-NT-02)
- **Kích hoạt:** sửa hạn, hoàn thành/mở lại, xóa Task, đổi timezone, hoặc nhận thay đổi từ pull.
- **Luồng chính:** app hủy thông báo cũ của Task và đặt lại nếu Task còn `OPEN` và có hạn trong tương lai; đổi timezone thì đặt lại cho mọi Task có hạn.
- **Quy tắc:** hoạt động offline; không có reminder phía server ở Phase 1 (FR-NT-04).

---

## F. Ma trận truy vết

| Use case | FR |
|---|---|
| UC-ID-01..09 | FR-ID-01..11 |
| UC-TK-01..06 | FR-TK-01..11 |
| UC-PJ-01..05 | FR-PJ-01..05 |
| UC-CT-01 | FR-CT-01 |
| UC-SY-01..05 | FR-SY-01..09 |
| UC-NT-01..02 | FR-NT-01..04 |

## G. Điều cần review kỹ

1. **UC-PJ-04 / UC-CT-01:** cascade do server thực hiện (SYSTEM_ARCHITECTURE §3.8); cần xác nhận chấp nhận `CONFLICT` hiếm gặp khi sửa Task bị ảnh hưởng.
2. **UC-CT-01:** trùng tên Category giữa hai thiết bị được xử lý bằng `REJECTED`; chưa có UI giải quyết ngoài thông báo.
3. **UC-SY-03:** pull gặp entity có mutation chờ gửi — hành vi tạm thời là không ghi đè; kiểm chứng với kịch bản đa thiết bị.
