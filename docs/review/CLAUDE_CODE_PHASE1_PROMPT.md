# Prompt giao việc cho Claude Code: triển khai Backend Phase 1

Dán phần dưới dòng `---` vào Claude Code, mở tại thư mục gốc `D:\LifeOS`.

---

Bạn sẽ triển khai **backend Phase 1** của LifeOS. Toàn bộ thiết kế đã được con người duyệt; việc của bạn là **cài đặt đúng thiết kế**, không phải thiết kế lại.

## 0. Đọc trước khi làm bất cứ điều gì

Đọc theo thứ tự, và đọc **hết** trước khi viết dòng code đầu tiên:

1. `AGENTS.md` (quy tắc vận hành bắt buộc, thứ tự ưu tiên khi xung đột, Definition of Done)
2. `docs/engineering/CODE_GUIDELINES.md` (nhãn `[CONFIRMED]/[OPEN]`, decision log D1–D73)
3. `docs/engineering/JAVA_SPRING_RULES.md`, `DATABASE_RULES.md`, `SYNC_RULES.md`
4. `docs/architecture/SYSTEM_ARCHITECTURE.md` (đặc biệt §3 backend, §4 hợp đồng sync, §6 bất biến)
5. `docs/architecture/DATABASE_DESIGN.md` (DDL V1–V4 đã nằm trong `backend/src/main/resources/db/migration`)
6. `docs/engineering/SECURITY_RULES.md`, `API_CONVENTIONS.md`, `TESTING_RULES.md`
7. `docs/product/PRD.md` và `docs/product/USE_CASES.md` (yêu cầu và use case cần thỏa mãn)
8. `docs/architecture/BACKEND_SKELETON_PLAN.md` (§8: trạng thái hiện tại của skeleton)
9. `backend/README.md` và toàn bộ mã trong `backend/` (đã có skeleton: `common`, ArchUnit A1–A12, lỗi RFC 9457, bảo mật deny-by-default, Flyway V1–V4, test)

Nếu thấy hai tài liệu mâu thuẫn, hoặc tài liệu mâu thuẫn với thực tế công cụ: **dừng và hỏi**. Không tự chọn bên nào, không âm thầm sửa tài liệu.

## 1. Nguyên tắc không được vi phạm

- Chỉ áp dụng mục `[CONFIRMED]`. Gặp `[OPEN]` hoặc điều tài liệu không nói rõ: dừng và hỏi.
- **Không sửa `docs/`.** Cần đổi tài liệu thì nêu đề xuất trong báo cáo. Ngoại lệ duy nhất: điền lệnh build/test đã chạy thật vào `AGENTS.md` khi con người đồng ý.
- **Không thêm thư viện ngoài danh sách được duyệt ở §3** khi chưa được con người đồng ý.
- **Không sửa migration đã có** (V1–V4). Cần đổi schema thì thêm migration mới và nêu lý do trong báo cáo.
- Không xóa, nới lỏng, hoặc `@Disabled` test để làm build xanh. Không thêm ngoại lệ vào luật ArchUnit.
- `user_id` luôn lấy từ `CurrentUser`, không bao giờ từ request body/path/query. Dữ liệu của user khác trả **404** (không 403), phản hồi không phân biệt được với "không tồn tại".
- Không log mật khẩu, access/refresh/verification/reset token; không trả stack trace/SQL ra client.
- Mọi thao tác ghi entity đồng bộ đi qua Application Service (`@Transactional` chỉ ở `..service..`) và gọi `ChangeRecorder.record(...)` trong cùng transaction.
- Test phải dùng PostgreSQL thật (Testcontainers), không dùng H2. Test dùng container dùng chung **không được giả định DB còn trống**; mỗi test tạo user/dữ liệu riêng bằng UUID mới.
- Mỗi bước nhỏ: build + test xanh trước khi sang bước sau.

## 2. Quy trình làm việc

Làm **lần lượt từng giai đoạn** ở §4. Với mỗi giai đoạn:

1. **Trước khi code:** đưa kế hoạch ngắn (các lớp/endpoint sẽ tạo, test sẽ viết, câu hỏi nếu có) và **chờ con người trả lời "duyệt"**.
2. Cài đặt, viết test cùng lúc với code.
3. Chạy `mvn verify` (cần Docker bật). Chỉ báo "xong" khi **xanh thật**; nêu rõ lệnh đã chạy và kết quả (số test đạt/lỗi).
4. **Báo cáo** theo mẫu ở §5 rồi **dừng, chờ con người cho phép** sang giai đoạn kế tiếp.

Môi trường: JDK 21 trở lên (bytecode Java 21), Maven (nếu chưa có trong PATH: `winget install Apache.Maven` rồi mở lại terminal), Docker Desktop đang chạy. Chạy lệnh trong `D:\LifeOS\backend`.

## 3. Thư viện được duyệt thêm (chỉ khi giai đoạn cần)

| Giai đoạn | Thư viện | Dùng cho |
|---|---|---|
| 1 Identity | Nimbus JOSE+JWT (hoặc JWT hỗ trợ sẵn của Spring Security), Bouncy Castle `bcprov` (Argon2id), Caffeine | JWT HS256 (D15), Argon2id (D16), rate limit trong bộ nhớ (D24) |
| 1 Identity | `spring-boot-starter-mail` | Gửi email qua SMTP (Mailpit khi dev) |
| 1–2 | `springdoc-openapi` (bản 3.x cho Boot 4), chỉ bật ở profile dev/test | OpenAPI (D63) |
| 1–4 | GreenMail hoặc stub tự viết | Test email |

Thư viện khác: dừng và xin duyệt. Tên starter/phiên bản theo Spring Boot 4.1 (xác nhận bằng tài liệu chính thức, không sao chép cấu hình Boot 3).

## 4. Các giai đoạn

### Giai đoạn 0: Làm skeleton xanh (bắt buộc làm trước)
- `FlywayMigrationIT.allFourMigrationsAreApplied` đang lỗi (`expected 4, was 0`) vì test dùng chung container với `ApplicationStartupIT`, Spring đã migrate trước. Sửa test để kiểm **trạng thái** (`flyway.info().applied()` có 4 bản, phiên bản hiện tại là 4) thay vì đếm số bản vừa chạy; `migrate()` gọi lại phải an toàn.
- Rà soát các test khác có giả định DB trống không.
- Hoàn thành khi `mvn verify` ở `backend/` báo `BUILD SUCCESS`, toàn bộ test đạt (baseline: 21 unit + 12 IT = 33).
- Đổi `archunit.properties` giữ `failOnEmptyShould=false` cho đến khi Giai đoạn 2 xong (xem README).

### Giai đoạn 1: Identity
Phạm vi: `UC-ID-01..` trong `USE_CASES.md`, `FR-ID-*` trong PRD, `API_CONVENTIONS.md` §8.1–8.3, `SECURITY_RULES.md` toàn bộ phần Identity.
- Endpoint: register, verify-email, resend-verification, login, refresh, logout, forgot-password, reset-password, `GET/PATCH /me`, change-password, `GET/PATCH /me/settings`, `GET /devices`, `DELETE /devices/{id}`.
- JwtAuthFilter, access token 15 phút (claim `sub` = userId, `did` = deviceId), `JwtService`, `PasswordHasher` Argon2id, chính sách mật khẩu theo D47.
- Refresh token: SHA-256 hash, xoay vòng theo family, **nguyên tử** bằng `UPDATE ... WHERE used_at IS NULL ... RETURNING` trong cùng transaction với việc chèn token mới (D72), trần family 90 ngày (D71), dùng lại token đã dùng ⇒ thu hồi cả family (D48).
- Thu hồi thiết bị: refresh bị từ chối ngay; access token bị từ chối qua tập thiết bị bị thu hồi trong bộ nhớ (D45).
- Rate limit theo bảng ở `SECURITY_RULES.md` §6 (Caffeine, đúng giá trị đã chốt); chuẩn hóa email; chống dò email (cùng mã và nội dung dù email có tồn tại hay không); chạy băm giả khi email không tồn tại.
- Gửi email xác minh/đặt lại qua **outbox** (`outbox_events`), ghi cùng transaction; **chưa** cần worker (Giai đoạn 4) nhưng phải ghi sự kiện đúng; token thô chỉ tạm thời trong payload (D37).
- Tạo `user_settings` mặc định khi đăng ký (timezone mặc định `Asia/Ho_Chi_Minh` nếu không gửi; kiểm tra IANA hợp lệ bằng `ZoneId`).
- Test bắt buộc: SEC-01..13, AUTHZ-01..04 (kể cả hai refresh đồng thời), ENUM-01..02, LOG-01..02 (`TESTING_RULES.md` §7), cùng test từng use case Identity. Test AUTHZ-02 phải quét `RequestMappingHandlerMapping` để so danh sách route công khai với `SecurityConfig.PUBLIC_POST_ENDPOINTS`.

### Giai đoạn 2: Project, Category, Task
Phạm vi: `UC-PJ-*`, `UC-CT-*`, `UC-TK-*`, `API_CONVENTIONS.md` §8.4–8.6, `SYSTEM_ARCHITECTURE.md` §3.2, §3.8.
- Module `project` (api: `ProjectApi.requireOwned`) và `task` (Task + Category, Category thuộc module `task` theo D25; `task` chỉ phụ thuộc `project` qua `project.api`).
- Controller → `*CommandService`/`*QueryService` → repository; DTO riêng + MapStruct (`unmappedTargetPolicy=ERROR`); không trả/bind Entity.
- `*CommandService` luôn gọi `ChangeRecorder.record(...)`. Vì module `sync` chưa có, tạo một cài đặt **tạm trong test** hoặc cài `ChangeRecorderImpl` tối thiểu theo `SYSTEM_ARCHITECTURE.md` §3.6 (upsert `user_sync_state`, ghi `change_logs`); hỏi con người nếu chưa rõ nên chọn cách nào.
- `version` bắt buộc cho PATCH/action/DELETE, lệch ⇒ 409 `ENTITY_VERSION_CONFLICT` kèm `currentVersion`; xóa mềm; cascade xóa Project/Category phía server (mỗi Task bị ảnh hưởng tăng `version` và có `change_logs` riêng); `GET /projects/{id}/progress`.
- Phân trang theo con trỏ, lọc, sắp xếp theo `API_CONVENTIONS.md` §7.
- Test bắt buộc: IDOR-01..04, SYN-13/14 (phần cascade), test domain (`dueTime` cần `dueDate`, độ dài, `OPEN↔DONE`), tên Category duy nhất không phân biệt hoa thường.
- Khi xong: đổi `failOnEmptyShould` thành `true` trong `archunit.properties` và đảm bảo ArchUnit vẫn xanh.

### Giai đoạn 3: Sync push/pull
Phạm vi: `UC-SY-*`, `SYSTEM_ARCHITECTURE.md` §3.3, §3.5–3.7, §4, `SYNC_RULES.md`.
- Module `sync`: `SyncController`, `SyncPushService`, `SyncPullService`, `ChangeRecorderImpl`, repository; handler `*SyncHandler` nằm trong package `sync` của từng module nghiệp vụ, chỉ phụ thuộc `common.sync`.
- Mỗi mutation một transaction; kết quả `CONFLICT/REJECTED` ghi trong transaction riêng; `DUPLICATE` phát lại kết quả đã lưu; lô tối đa 50; payload tối đa 64 KB; `lock_timeout` 5 giây ⇒ 503 + `Retry-After` (D42).
- Pull theo state, phân trang theo `seq` (mặc định 200, tối đa 500), có tombstone.
- Test bắt buộc: **SYN-01..21** (`TESTING_RULES.md` §6), gồm test đồng thời có timeout cứng.

### Giai đoạn 4: Outbox worker và email
Phạm vi: `SYSTEM_ARCHITECTURE.md` §3.11, `DATABASE_DESIGN.md` §4.4, D22–D23, D37.
- Worker chạy cùng process, polling `FOR UPDATE SKIP LOCKED`, lease `locked_until`, backoff mũ, `FAILED` sau số lần tối đa; scrub payload về `{}` sau khi `SENT`/`FAILED`; **không** dùng `@Transactional` ngoài `..service..` (dùng `TransactionTemplate` nếu cần).
- `EmailSender` (port) + cài đặt SMTP; Mailpit khi dev.
- Test: gửi thành công, thử lại, không xử lý trùng với 2 worker, token thô không còn sau khi gửi (LOG-02).

## 5. Mẫu báo cáo sau mỗi giai đoạn (bắt buộc)

1. **Đã làm:** danh sách tính năng/endpoint/lớp chính.
2. **Lệnh đã chạy và kết quả thật:** ví dụ `mvn verify` → số test đạt/lỗi, `BUILD SUCCESS/FAILURE`. Không viết "đã test" nếu chưa chạy.
3. **Test đã thêm:** ID bắt buộc (SYN/SEC/IDOR...) nào đã có, ID nào chưa và vì sao.
4. **Giả định bạn đã đưa ra** (mọi chỗ tài liệu không nói rõ mà bạn phải chọn).
5. **Điểm lệch so với tài liệu** hoặc đề xuất sửa tài liệu (không tự sửa).
6. **Chỗ cần con người xem kỹ** (bảo mật, đồng thời, transaction).
7. **Việc chưa làm / rủi ro còn lại.**

## 6. Checklist tự kiểm tra trước khi báo "xong" (người review sẽ đối chiếu đúng các mục này)

- [ ] `mvn verify` xanh; ArchUnit A1–A12 xanh, không có ngoại lệ mới.
- [ ] Không có `userId` nhận từ client; mọi truy vấn dữ liệu cá nhân có điều kiện `user_id`; dữ liệu user khác ⇒ 404 không phân biệt được.
- [ ] Không có `@Transactional` ngoài `..service..`; không có Entity trong DTO/controller.
- [ ] Mọi `*CommandService` của entity đồng bộ gọi `ChangeRecorder` đúng một lần cho mỗi thay đổi (kể cả cascade).
- [ ] Mã lỗi đúng danh mục `API_CONVENTIONS.md` §5.1; mã HTTP đúng §4 (400 vs 422 vs 404 vs 409).
- [ ] Không có token/mật khẩu trong log hoặc phản hồi lỗi; có test kiểm tra.
- [ ] Mọi giá trị cấu hình nhạy cảm (JWT secret, SMTP) đọc từ biến môi trường, không có giá trị thật trong repo.
- [ ] Test không giả định DB trống, không phụ thuộc thứ tự chạy, không `Thread.sleep`.
- [ ] Không sửa `docs/` và migration V1–V4; không thêm thư viện ngoài danh sách.

Bắt đầu bằng cách đọc tài liệu (§0), rồi **báo cáo bạn đã đọc những file nào** và đưa kế hoạch cho Giai đoạn 0. Chưa viết code trước khi tôi duyệt kế hoạch.
