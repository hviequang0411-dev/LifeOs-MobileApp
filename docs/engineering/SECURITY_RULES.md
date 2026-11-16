# LifeOS — Security Rules v1 (Phase 1)

> **Trạng thái:** v1 — đã duyệt 2026-11-05.
> **Phạm vi:** bảo mật backend Spring Boot, ứng dụng Flutter và đồng bộ dữ liệu cho Phase 1. Mở rộng ở phase sau (FCM, AI, Finance) sẽ bổ sung mục riêng.
> **Nguồn:** `PRD.md` (Q-01, Q-09), `JAVA_SPRING_RULES.md` §7, `SYNC_RULES.md`, `SYSTEM_ARCHITECTURE.md` §3.10, `DATABASE_DESIGN.md`.
> Quy ước nhãn: `[CONFIRMED]` đã chốt · `[PROPOSED]` đề xuất chưa duyệt · `[OPEN]` chưa quyết định. Agent chỉ áp dụng `[CONFIRMED]`.

---

## 1. Mô hình mối đe dọa (tóm tắt)

**Tài sản cần bảo vệ:** tài khoản và phiên đăng nhập; nội dung cá nhân (Task, Project, Category, về sau Finance, Note, Goal); email và tên hiển thị; tính toàn vẹn của dữ liệu đồng bộ.

**Ranh giới tin cậy:** ứng dụng Flutter trên thiết bị người dùng là **không đáng tin** (có thể bị sửa, giả lập, gửi payload tùy ý); chỉ backend là nơi quyết định tính hợp lệ `[CONFIRMED]`.

| # | Mối đe dọa | Biện pháp chính | Mục |
|---|---|---|---|
| T1 | Dò mật khẩu / credential stuffing | Giới hạn tần suất, hash mật khẩu mạnh, phản hồi đồng nhất | §3, §6 |
| T2 | Dò email có tài khoản hay không | Phản hồi và thời gian phản hồi đồng nhất | §3.3 |
| T3 | Gửi email hàng loạt qua endpoint công khai | Rate limit theo email và IP | §6 |
| T4 | Truy cập dữ liệu của user khác (IDOR) | `userId` từ token, truy vấn luôn kèm `user_id`, 404 | §7 |
| T5 | Giả mạo / tái sử dụng token | JWT ký + kiểm tra chặt, refresh xoay vòng, hash trong DB | §4 |
| T6 | Mất thiết bị / token bị đánh cắp | Thu hồi thiết bị, phát hiện dùng lại refresh token | §4.3, §4.4 |
| T7 | Rò rỉ cơ sở dữ liệu | Hash mật khẩu và token, không lưu bí mật thô lâu | §3, §4, §14 |
| T8 | Client độc hại gửi sync tùy ý | Validate như REST, giới hạn kích thước, kiểm tra sở hữu từng mutation | §8, §9 |
| T9 | Rò rỉ qua log, lỗi, email | Quy tắc log, lỗi RFC 9457 không lộ chi tiết | §11, §10 |
| T10 | Lộ bí mật trong mã nguồn / cấu hình | Biến môi trường, quét secret trong CI | §12 |
| T11 | Lỗ hổng thư viện | Quét dependency trong CI | §15 |
| T12 | Dữ liệu cục bộ bị đọc từ thiết bị | Secure storage cho token, loại khỏi backup đám mây, DB riêng mỗi user | §13 |

Ngoài phạm vi Phase 1: mã hóa SQLite at rest, certificate pinning, chống jailbreak/root, 2FA, đăng nhập mạng xã hội, xóa tài khoản `[OPEN]` (baseline §16).

---

## 2. Nguyên tắc chung `[CONFIRMED]`

1. Backend là nơi quyết định tính hợp lệ và quyền truy cập; không tin dữ liệu từ client.
2. `userId` **chỉ** lấy từ security context (`CurrentUser`), không từ path/body/query.
3. Mọi truy vấn dữ liệu cá nhân kèm `user_id` (`findByIdAndUserId`); dữ liệu của user khác trả **404**.
4. Không để API key/secret của nhà cung cấp trong Flutter; secret chỉ từ biến môi trường.
5. Không log token, mật khẩu, nội dung cá nhân.
6. Bảo mật là điều kiện của Definition of Done: PR chạm xác thực, token, sync hoặc truy cập dữ liệu phải qua checklist §16.

---

## 3. Mật khẩu và đăng nhập

### 3.1. Chính sách mật khẩu
- `[CONFIRMED]` Tối thiểu **8 ký tự** (PRD Q-01).
- `[CONFIRMED]` D47: tối đa **128 ký tự** (chặn DoS khi băm); chuẩn hóa Unicode NFKC trước khi băm; **không** bắt buộc quy tắc ký tự (chữ hoa/số/ký hiệu); kiểm tra mật khẩu nằm trong danh sách mật khẩu phổ biến là `[OPEN]` (sau Phase 1).

### 3.2. Băm mật khẩu
- `[CONFIRMED]` Qua interface `PasswordHasher`; không bao giờ lưu hoặc log mật khẩu thô.
- `[CONFIRMED]` D16/D46: **Argon2id** (bcrypt chỉ khi thư viện/hiệu năng không phù hợp, khi đó phải xin duyệt). Cấu hình xuất phát từ mức tối thiểu OWASP Password Storage Cheat Sheet (Argon2id: bộ nhớ khoảng 19 MiB, 2 vòng lặp, song song 1) và chốt bằng benchmark trên máy chủ thật, mục tiêu khoảng 100–300 ms mỗi lần băm. Tham số lưu cùng chuỗi hash để nâng cấp về sau; đăng nhập thành công với tham số cũ thì băm lại (rehash).
- Cần kiểm tra phụ thuộc: Argon2 trong Spring Security yêu cầu thư viện mật mã bổ sung; xác nhận khi khởi tạo project.

### 3.3. Chống dò tài khoản (T2) `[CONFIRMED]` nguyên tắc
- Đăng ký, quên mật khẩu, gửi lại xác minh: phản hồi **giống nhau** dù email có tồn tại hay không.
- Đăng nhập sai: thông điệp chung (`AUTH_INVALID_CREDENTIALS`), không nói sai email hay sai mật khẩu.
- `[CONFIRMED]` D47: khi email không tồn tại vẫn thực hiện một phép băm giả để thời gian phản hồi không lộ sự tồn tại; mọi công việc gửi email chạy bất đồng bộ qua outbox nên không ảnh hưởng thời gian phản hồi.
- Chỉ tài khoản đã xác minh email mới đăng nhập được; chưa xác minh trả mã riêng (`AUTH_EMAIL_NOT_VERIFIED`) **sau khi** mật khẩu đúng, để không lộ tài khoản cho kẻ không biết mật khẩu.

### 3.4. Xác minh email và đặt lại mật khẩu
- Token ngẫu nhiên 256 bit từ `SecureRandom`, mã hóa base64url; DB chỉ lưu SHA-256 (`token_hash`) `[CONFIRMED]`.
- Dùng một lần (`used_at`), hạn: xác minh 24 giờ, đặt lại 1 giờ `[CONFIRMED]` (PRD Q-01). Cấp token mới thì vô hiệu token cũ còn hiệu lực của cùng user.
- Đặt lại mật khẩu thành công: vô hiệu **mọi** refresh token của user `[CONFIRMED]`. Đổi mật khẩu: vô hiệu refresh token của các thiết bị khác `[CONFIRMED]`.
- `[CONFIRMED]` D54: gửi email thông báo "mật khẩu đã được thay đổi" sau khi đổi/đặt lại, để chủ tài khoản phát hiện truy cập trái phép.
- Liên kết trong email chỉ dựng từ `APP_BASE_URL` cấu hình sẵn, **không** từ header `Host` của request (chống host header poisoning) `[CONFIRMED]`.
- Không đưa token vào log, tiêu đề email hay tham số có thể bị lưu trong log proxy khi tránh được; tham số truy vấn chứa token phải được client đổi sang POST ngay khi mở liên kết `[CONFIRMED]`.

---

## 4. Token

### 4.1. Access token (JWT) `[CONFIRMED]` thời hạn / `[CONFIRMED]` chi tiết
- Sống **15 phút** `[CONFIRMED]`. Claim: `sub` (userId), `did` (deviceId), `jti`, `iat`, `exp`, `iss`, `aud`.
- `[CONFIRMED]` D15: ký HS256 với secret ≥ 256 bit từ biến môi trường (một backend duy nhất); chuyển RS256 khi có nhiều bên xác thực.
- Khi xác thực: **ghim thuật toán** (từ chối `none` và thuật toán khác), kiểm tra chữ ký, `exp` (cho phép lệch đồng hồ tối đa 30 giây), `iss`, `aud`; token sai/hết hạn → 401 `AUTH_TOKEN_EXPIRED` hoặc `AUTH_TOKEN_INVALID` `[CONFIRMED]`.
- Hỗ trợ xoay khóa ký: header `kid`, chấp nhận hai khóa trong thời gian chuyển đổi `[CONFIRMED]`.
- Access token **không** chứa dữ liệu cá nhân ngoài định danh.

### 4.2. Refresh token `[CONFIRMED]` thời hạn và xoay vòng / `[CONFIRMED]` chi tiết
- Chuỗi ngẫu nhiên 256 bit, chỉ lưu SHA-256; sống **30 ngày**, **xoay vòng** mỗi lần dùng (cấp token mới, token cũ đặt `used_at`).
- Gắn với `device_id` và `family_id` (một chuỗi xoay vòng = một family) `[CONFIRMED]` (xem `DATABASE_DESIGN.md` §4.1).
- Mỗi lần xoay vòng, token mới có hạn 30 ngày kể từ lúc cấp (trượt). **Trần tuyệt đối 90 ngày kể từ lúc tạo family** `[CONFIRMED]` D71: `expires_at = min(now + 30 ngày, family_started_at + 90 ngày)`; hết trần thì phải đăng nhập lại.
- **Xoay vòng phải nguyên tử** `[CONFIRMED]` D72: đánh dấu dùng token bằng một câu lệnh có điều kiện, ví dụ `UPDATE refresh_tokens SET used_at = now() WHERE token_hash = :h AND used_at IS NULL AND revoked_at IS NULL AND expires_at > now() RETURNING id, family_id, device_id, family_started_at`, trong cùng transaction với việc chèn token mới. Không có dòng nào được cập nhật → không cấp token; nếu token tồn tại nhưng đã `used_at` thì xử lý như dùng lại (§4.3). Cấm mẫu "SELECT rồi UPDATE" không có điều kiện. Hai request refresh đồng thời với cùng một token: chỉ một request thắng; request còn lại bị coi là dùng lại.
- Refresh chỉ thành công khi: hash khớp, `used_at` và `revoked_at` là NULL, chưa hết hạn, thiết bị chưa bị thu hồi, user còn `ACTIVE`.

### 4.3. Phát hiện dùng lại refresh token (T6) `[CONFIRMED]` D48
- Token đã `used_at` bị trình lại → coi là có thể bị đánh cắp: đặt `revoked_at` cho **toàn bộ family**, trả 401 `AUTH_REFRESH_TOKEN_INVALID`, ghi sự kiện bảo mật (§11.3).
- Đánh đổi đã biết: nếu phản hồi refresh bị mất giữa đường (mạng lỗi sau khi server đã xoay vòng), client gửi lại token cũ sẽ bị đăng xuất. Giảm thiểu: client dùng **single-flight** cho refresh và **ghi token mới vào secure storage trước khi bỏ token cũ**. Dữ liệu local chưa sync không bị xóa khi đăng nhập lại (PRD §7.5). Cửa sổ ân hạn cho trình lại là `[OPEN]` (chưa dùng ở Phase 1).

### 4.4. Thu hồi thiết bị và độ trễ của access token (T6) `[CONFIRMED]` D45
Vấn đề: thu hồi thiết bị vô hiệu refresh token ngay, nhưng access token đã cấp vẫn dùng được tối đa 15 phút.

Đề xuất: giữ một tập `revokedDeviceIds` **trong bộ nhớ** (kèm thời điểm thu hồi); `JwtAuthFilter` từ chối token có `did` nằm trong tập. Mỗi phần tử tự loại sau 15 phút (bằng đời access token tối đa). Khi khởi động, nạp lại các thiết bị bị thu hồi trong 15 phút gần nhất từ `devices.revoked_at`. Không thêm truy vấn DB mỗi request. Khi chạy nhiều instance, chuyển sang bộ nhớ dùng chung (Redis ở phase sau).

Phương án thay thế (đã chấp nhận ở bản kiến trúc đầu): chấp nhận cửa sổ 15 phút. Nếu chọn phương án này, ghi rõ trong tài liệu người dùng.

### 4.5. Đăng xuất `[CONFIRMED]` hành vi (PRD Q-09)
- `[CONFIRMED]` D75: logout thu hồi **cả family** của refresh token gửi lên (không thu hồi bản ghi thiết bị); token đã dùng/hết hạn/không tồn tại vẫn trả `204` và **không** kích hoạt phát hiện dùng lại. Đăng nhập lại bằng `deviceId` đang có family còn sống thu hồi family cũ (mỗi thiết bị một phiên).
- Đăng xuất vô hiệu refresh token của thiết bị hiện tại; client xóa token local; không tự xóa dữ liệu local chưa sync.
- Đăng xuất khi offline: xóa token local và đánh dấu để thu hồi ở lần có mạng sau `[CONFIRMED]` (UC-ID-05).

---

## 5. Thiết bị

- `devices.id` do server sinh; client lưu lại và gửi khi đăng nhập lại. Thiết bị không thuộc user hoặc đã bị thu hồi → tạo thiết bị mới `[CONFIRMED]` (D31).
- Thu hồi chỉ áp dụng với thiết bị của chính user; ID thiết bị của user khác → 404 `[CONFIRMED]`.
- Thiết bị bị thu hồi không sync được, phải đăng nhập lại; dữ liệu local giữ đến khi đăng nhập lại (PRD Q-09).
- Không lưu định danh phần cứng cố định; `name` và `platform` do client cung cấp, coi là không tin cậy (chỉ hiển thị, escape khi render).

---

## 6. Giới hạn tần suất và chống lạm dụng

`[CONFIRMED]` Đăng nhập: **5 lần sai / 15 phút theo tài khoản + IP** (PRD Q-01).
`[CONFIRMED]` D49: mở rộng như sau. Bộ đếm trong bộ nhớ (Caffeine) ở Phase 1 vì chạy một instance; khởi động lại xóa bộ đếm và được chấp nhận; chuyển Redis khi scale.

| Endpoint | Khóa đếm | Giới hạn đề xuất | Khi vượt |
|---|---|---|---|
| `POST /auth/login` | (email, IP) | 5 sai / 15 phút `[CONFIRMED]` | 429 + `Retry-After` |
| `POST /auth/login` | email (mọi IP) | 20 sai / 15 phút | 429 |
| `POST /auth/login` | IP (mọi email) | 50 sai / 15 phút | 429 |
| `POST /auth/register` | IP | 10 / giờ | 429 |
| `POST /auth/forgot-password`, `/resend-verification` | email | 5 / giờ | trả phản hồi thành công giả, **không** gửi email |
| `POST /auth/forgot-password`, `/resend-verification`, `/register` | IP | 20 / giờ | 429 |
| `POST /auth/reset-password`, `/verify-email` | IP | 20 / giờ | 429 |
| `POST /auth/logout` | IP (bucket riêng) | 60 / giờ `[CONFIRMED]` D74 | 429 |
| `POST /me/change-password` (đã xác thực) | user, chỉ đếm lần nhập sai mật khẩu hiện tại | 5 sai / 15 phút `[CONFIRMED]` D74 | 429 |
| `POST /auth/refresh` | IP (thiết bị chưa biết trước khi tra token; sau khi tra được token, phát hiện dùng lại theo `family_id` — §4.3) | 60 / giờ | 429 |
| API đã xác thực | user | 600 yêu cầu / phút | 429 |

Quy tắc:
- `[CONFIRMED]` D74: các endpoint cùng một dòng bảng dùng **chung một bucket** theo khóa đó (vd. forgot/resend/register chung bucket IP 20/giờ; `/register` còn có bucket riêng 10/giờ); mỗi dòng khác là bucket riêng.
- Throttle **tạm thời**, không khóa vĩnh viễn tài khoản (tránh bị lợi dụng để khóa người khác).
- Đăng nhập đúng đặt lại bộ đếm (email, IP).
- IP lấy từ `X-Forwarded-For` **chỉ** khi request đi qua reverse proxy tin cậy đã cấu hình; ngược lại dùng địa chỉ kết nối trực tiếp.
- Vượt giới hạn: HTTP 429, `errorCode = RATE_LIMITED`, header `Retry-After`; ghi sự kiện bảo mật.
- Giới hạn kích thước và số lượng: xem §9.

---

## 7. Phân quyền và cô lập dữ liệu (T4)

1. `[CONFIRMED]` Mọi endpoint dữ liệu cá nhân yêu cầu xác thực; trừ các endpoint công khai: `register`, `verify-email`, `resend-verification`, `login`, `refresh`, `logout`, `forgot-password`, `reset-password`, `health` (readiness/liveness).
2. `[CONFIRMED]` Truy vấn qua `findByIdAndUserId…`; cấm `findById(id)` trần cho dữ liệu cá nhân; ArchUnit/review kiểm tra.
3. `[CONFIRMED]` Tài nguyên của user khác hoặc không tồn tại: cùng phản hồi **404** (không phân biệt).
4. `[CONFIRMED]` Tham chiếu chéo (`projectId`, `categoryId` trong Task) phải thuộc **cùng user**: kiểm tra qua `ProjectApi`/repository có `user_id`; sai → lỗi nghiệp vụ `*_NOT_FOUND`.
5. `[CONFIRMED]` **Chống mass assignment:** không bao giờ bind request trực tiếp vào entity; DTO chỉ chứa trường được phép; `userId`, `version` do server, `createdAt`, `completedAt` không nhận từ client.
6. `[CONFIRMED]` Client chọn `id` (UUID) cho Task/Project/Category; `CREATE` với `id` đã tồn tại → `REJECTED/409` với cùng phản hồi dù id thuộc user nào (không lộ sự tồn tại) (D36).
7. `[CONFIRMED]` Danh sách (REST) luôn lọc theo `user_id` ở tầng repository, không lọc sau ở tầng service.

---

## 8. Bảo mật đồng bộ (T8)

1. `[CONFIRMED]` Mỗi mutation: xác thực → kiểm tra sở hữu entity → idempotency → baseVersion → business rules → thực thi (thứ tự trong `SYNC_RULES.md` §9.2).
2. `[CONFIRMED]` `entityType` thuộc danh sách trắng (`task`, `project`, `category`, `user_settings`); khác → `REJECTED` (`SYNC_UNKNOWN_ENTITY_TYPE`).
3. `[CONFIRMED]` Payload sync được validate **giống hệt** request REST (cùng Bean Validation/rule ở Application Service); `SyncEntityHandler` không chứa quy tắc riêng.
4. `[CONFIRMED]` Cursor chỉ chứa `seq`; user lấy từ token, nên không thể kéo dữ liệu của user khác bằng cách sửa cursor. `serverSnapshot` trong `CONFLICT` chỉ chứa dữ liệu của chính user.
5. `[CONFIRMED]` Replay: gửi lại mutation đã xử lý chỉ nhận kết quả cũ, không thực thi lại (`UNIQUE (user_id, mutation_id)`).
6. `[CONFIRMED]` Client không điều khiển được `version`, `seq`, `change_logs`; server là nơi duy nhất sinh chúng.
7. `[CONFIRMED]` Xóa Project/Category: server thực hiện cascade, không tin client về danh sách Task bị ảnh hưởng (`SYSTEM_ARCHITECTURE.md` §3.8).

---

## 9. Kiểm tra đầu vào, giới hạn kích thước, chống injection

1. `[CONFIRMED]` Truy vấn DB luôn có tham số (JPA/`JdbcClient` named parameters); **cấm** ghép chuỗi SQL từ dữ liệu người dùng.
2. `[CONFIRMED]` Validation hai lớp (cú pháp ở DTO, nghiệp vụ ở service); độ dài `title` ≤ 200, `description` ≤ 10.000 (PRD Q-10).
3. `[CONFIRMED]` D55: giới hạn kích thước request body **1 MB**; mỗi payload mutation ≤ **64 KB**; lô push ≤ **50** mutation; `limit` pull ≤ 500; vượt → 413/400 (`PAYLOAD_TOO_LARGE`, `SYNC_BATCH_TOO_LARGE`).
4. `[CONFIRMED]` Request JSON: từ chối trường lạ ở endpoint ghi nhạy cảm (auth); với sync/REST dữ liệu cho phép bỏ qua trường lạ để tương thích tiến lên nhưng **không bao giờ** ánh xạ chúng vào entity.
5. `[CONFIRMED]` Nội dung do người dùng nhập (`title`, `name`, `display_name`) coi là văn bản thuần: escape khi đưa vào email HTML; chống chèn header email (loại bỏ CR/LF trong tên hiển thị khi dùng trong tiêu đề).
6. `[CONFIRMED]` Email: mẫu cố định, không cho người dùng điều khiển nội dung ngoài tên hiển thị.
7. `[CONFIRMED]` Timezone phải là IANA hợp lệ (`ZoneId.of`), `default_currency` khớp `^[A-Z]{3}$`, enum thuộc tập cố định.

---

## 10. Xử lý lỗi và phản hồi

1. `[CONFIRMED]` Lỗi theo RFC 9457 (xem `API_CONVENTIONS.md`); không lộ stack trace, SQL, tên bảng, đường dẫn nội bộ.
2. `[CONFIRMED]` Lỗi 5xx trả thông điệp chung kèm `correlationId`; chi tiết chỉ ở log máy chủ.
3. `[CONFIRMED]` Cùng mã trạng thái và thân lỗi cho "không tồn tại" và "không thuộc về bạn" (404).
4. `[CONFIRMED]` Header phản hồi: `X-Content-Type-Options: nosniff`, `Cache-Control: no-store` cho phản hồi chứa token hoặc dữ liệu cá nhân; ẩn header `Server`/phiên bản.

---

## 11. Log, giám sát và sự kiện bảo mật

### 11.1. Không bao giờ log `[CONFIRMED]`
Mật khẩu, token (access/refresh/xác minh/reset), header `Authorization`, nội dung email, `title`/`description`/`name` của dữ liệu người dùng, số tiền kèm định danh (Finance sau này).

### 11.2. Được log
`userId`, `deviceId`, `correlationId`, loại sự kiện, kết quả, mã lỗi, độ trễ. Địa chỉ IP chỉ ghi trong sự kiện bảo mật (§11.3) và không lưu quá thời hạn lưu log `[CONFIRMED]`.

### 11.3. Sự kiện bảo mật có cấu trúc `[CONFIRMED]` D53
Ghi vào log ứng dụng (chưa có bảng `audit_logs` ở Phase 1): đăng nhập thành công/thất bại, throttle bị kích hoạt, refresh token bị dùng lại, đổi/đặt lại mật khẩu, thu hồi thiết bị, xác minh email, truy cập bị từ chối do không sở hữu. Mỗi sự kiện có `event`, `userId` (nếu có), `deviceId`, `ip`, `correlationId`, `timestamp`. Bảng audit lâu dài `[OPEN]` (baseline §16).

### 11.4. Actuator `[CONFIRMED]` D50
- Công khai: `health` (liveness/readiness) chỉ trả trạng thái tối thiểu.
- `metrics`, `prometheus`, `info` chỉ truy cập được từ mạng nội bộ / cổng quản trị riêng, hoặc có xác thực.
- Tắt `env`, `beans`, `heapdump`, `threaddump`, `loggers` ghi được ở môi trường thật.

---

## 12. Bí mật và cấu hình `[CONFIRMED]` / `[CONFIRMED]`

1. `[CONFIRMED]` Secret (`JWT_SECRET`, mật khẩu DB, SMTP) chỉ từ biến môi trường/secret store; không commit vào repo; có `.gitignore` cho tệp `.env`.
2. `[CONFIRMED]` CI quét secret (ví dụ gitleaks) trên mỗi PR.
3. `[CONFIRMED]` Secret của dev, test, prod khác nhau; secret prod không xuất hiện trên máy dev.
4. `[CONFIRMED]` D51: hai vai trò DB — tài khoản **migration** (có quyền DDL, chỉ chạy Flyway) và tài khoản **ứng dụng** (chỉ DML trên schema); tài khoản ứng dụng không phải superuser.
5. `[CONFIRMED]` Sao lưu `pg_dump` được mã hóa và lưu ngoài máy chủ ứng dụng; khôi phục được diễn tập trước khi phát hành thật.
6. Xoay secret: quy trình ghi trong runbook; JWT hỗ trợ hai khóa (§4.1).

---

## 13. Bảo mật phía Flutter (T12)

1. `[CONFIRMED]` Token lưu bằng secure storage (Keychain/Keystore); **không** dùng SharedPreferences hay file thường.
2. `[CONFIRMED]` Không có API key/secret nhà cung cấp trong ứng dụng.
3. `[CONFIRMED]` D52: loại SQLite và thư mục dữ liệu ứng dụng khỏi backup đám mây (Android: tắt `allowBackup` hoặc khai báo loại trừ; iOS: đặt cờ loại trừ backup cho tệp DB). Token trong secure storage không đưa vào backup.
4. `[CONFIRMED]` D26 / S7: đề xuất **một tệp DB cho mỗi user** (tên gắn `user_id`) trong thư mục riêng của ứng dụng; đăng nhập tài khoản khác không đọc được dữ liệu cũ. Khi đăng xuất không tự xóa dữ liệu chưa sync (PRD Q-09).
5. `[CONFIRMED]` Bản release: tắt log debug, không in token/dữ liệu cá nhân; không bật cổng debug.
6. `[CONFIRMED]` Liên kết sâu (deep link) từ email: kiểm tra scheme/host/đường dẫn khớp danh sách cho phép; token trong liên kết chỉ được gửi tới backend qua HTTPS.
7. `[CONFIRMED]` Thông báo local không hiển thị nội dung nhạy cảm quá mức; chỉ tiêu đề Task (người dùng tự đặt) — xem lại khi có Finance.
8. `[OPEN]` Mã hóa SQLite at rest, certificate pinning, che màn hình khi chuyển ứng dụng (baseline §16).

---

## 14. Truyền tải và hạ tầng

1. `[CONFIRMED]` Giao tiếp client–server qua HTTPS.
2. `[CONFIRMED]` TLS kết thúc ở reverse proxy; backend không nhận HTTP công khai ở môi trường thật; bật HSTS ở proxy.
3. `[CONFIRMED]` CORS tắt (client là ứng dụng native, không phải trình duyệt).
4. `[CONFIRMED]` PostgreSQL không mở ra Internet; chỉ backend truy cập; mật khẩu mạnh, kết nối nội bộ.
5. `[CONFIRMED]` Image Docker: user không phải root, base image tối thiểu, không chứa secret.
6. `[OPEN]` Mã hóa đĩa/at rest cho PostgreSQL ở môi trường thật (phụ thuộc nhà cung cấp hạ tầng).
7. Email môi trường thật `[CONFIRMED]`: cấu hình SPF, DKIM, DMARC cho tên miền gửi.

---

## 15. Phụ thuộc và chuỗi cung ứng

1. `[CONFIRMED]` Giữ số phụ thuộc ở mức tối thiểu (nguyên tắc "không thêm công nghệ chỉ để phức tạp" `[CONFIRMED]`).
2. `[CONFIRMED]` Phiên bản được ghim; commit `pubspec.lock`; phụ thuộc Maven/Gradle khóa phiên bản.
3. `[CONFIRMED]` CI quét lỗ hổng phụ thuộc (OWASP Dependency-Check hoặc công cụ tương đương, Dependabot) và chặn merge khi có lỗ hổng mức cao chưa được chấp nhận.
4. `[CONFIRMED]` Thư viện mật mã/JWT dùng thư viện đã được kiểm chứng; **không** tự viết thuật toán mật mã.

---

## 16. Kiểm thử bảo mật bắt buộc và checklist PR

### 16.1. Test tự động bắt buộc `[CONFIRMED]` (chi tiết ở `TESTING_RULES.md`)
| Mã | Kịch bản |
|---|---|
| SEC-01 | **Ma trận IDOR:** với mỗi endpoint dữ liệu cá nhân, user B không đọc/sửa/xóa dữ liệu của user A (kỳ vọng 404) |
| SEC-02 | JWT: `alg=none`, chữ ký sai, hết hạn, sai `iss`/`aud`, thiếu `did` → 401 |
| SEC-03 | Refresh: xoay vòng đúng; dùng lại token cũ thu hồi cả family; token hết hạn; thiết bị bị thu hồi |
| SEC-04 | Thu hồi thiết bị: access token của thiết bị đó bị từ chối trong cửa sổ cho phép (D45) |
| SEC-05 | Throttle đăng nhập và các endpoint công khai theo bảng §6; `Retry-After` |
| SEC-06 | Phản hồi đồng nhất khi email tồn tại / không tồn tại (đăng ký, quên mật khẩu, gửi lại) |
| SEC-07 | Token xác minh/reset: một lần, hết hạn, token cũ bị vô hiệu khi cấp mới |
| SEC-08 | Mass assignment: gửi `userId`, `version`, `completedAt` trong request → bị bỏ qua |
| SEC-09 | Sync: mutation tham chiếu `projectId` của user khác → `REJECTED`; `entityType` lạ → `REJECTED`; lô > 50 → từ chối |
| SEC-10 | Kích thước: body > 1 MB, payload > 64 KB bị từ chối |
| SEC-11 | Log không chứa mật khẩu/token (kiểm tra chuỗi nhạy cảm trong log khi chạy luồng auth) |
| SEC-12 | Lỗi 5xx không lộ stack trace |
| SEC-13 | Actuator nhạy cảm không truy cập được từ bên ngoài |

### 16.2. Checklist review PR (tick trước khi merge)
- [ ] Không có `userId` từ request; mọi truy vấn kèm `user_id`.
- [ ] Không bind request vào entity; DTO whitelist.
- [ ] Không log dữ liệu nhạy cảm; không lộ chi tiết nội bộ trong lỗi.
- [ ] Endpoint công khai mới có rate limit và phản hồi đồng nhất.
- [ ] Không ghép chuỗi SQL.
- [ ] Không gọi hệ thống ngoài trong transaction.
- [ ] Có test SEC-xx tương ứng cho thay đổi chạm xác thực/sync.
- [ ] Không thêm secret vào repo/cấu hình mẫu.
- [ ] Phụ thuộc mới đã được cân nhắc (§15).

---

## 17. Quyết định (đã duyệt 2026-11-05)

| # | Đề xuất | Mục |
|---|---|---|
| D44 | Tài liệu này được chấp nhận làm `SECURITY_RULES.md` v1 | — |
| D45 | Thu hồi thiết bị có hiệu lực ngay với access token bằng tập `revokedDeviceIds` trong bộ nhớ (nạp lại khi khởi động); phương án thay thế: chấp nhận cửa sổ 15 phút | §4.4 |
| D46 | Argon2id với tham số chọn bằng benchmark (bcrypt nếu cần); rehash khi tham số đổi | §3.2 |
| D47 | Mật khẩu 8–128 ký tự, NFKC, không quy tắc ký tự; băm giả khi email không tồn tại | §3.1, §3.3 |
| D48 | Dùng lại refresh token đã xoay → thu hồi cả family, không có cửa sổ ân hạn; client ghi token mới trước khi bỏ token cũ | §4.3 |
| D49 | Bảng giới hạn tần suất mở rộng (đăng nhập theo email và IP; register/forgot/resend/reset/refresh; API theo user); throttle tạm thời | §6 |
| D50 | Actuator: chỉ `health` công khai; còn lại nội bộ; tắt endpoint nguy hiểm | §11.4 |
| D51 | Hai vai trò DB: migration và ứng dụng | §12 |
| D52 | Loại DB local khỏi backup đám mây Android/iOS | §13 |
| D53 | Sự kiện bảo mật có cấu trúc trong log ở Phase 1; bảng audit để sau | §11.3 |
| D54 | Gửi email "mật khẩu đã thay đổi" | §3.4 |
| D55 | Giới hạn kích thước: body 1 MB, payload mutation 64 KB, lô push 50 | §9 |
| D76 | Email "mật khẩu đã thay đổi" dùng event outbox mới `PASSWORD_CHANGED` (phát khi đổi và khi đặt lại); quên mật khẩu cho tài khoản chưa xác minh vẫn gửi, đặt lại thành công thì đặt `email_verified_at` nếu đang trống; đăng ký trùng email chưa xác minh gửi lại email xác minh (tính vào hạn mức resend), đã xác minh thì không gửi; unique race vẫn trả 202 | §3.4, §3.1 |
| D75 | Logout thu hồi cả family; đăng nhập lại cùng `deviceId` thu hồi family cũ | §4.5 |
| D74 | Rate limit: bucket chung theo dòng bảng; thêm logout (IP 60/giờ) và change-password (user, 5 sai/15 phút) | §6 |
| D72 | Xoay vòng refresh token nguyên tử (`UPDATE … WHERE used_at IS NULL … RETURNING`); request đồng thời thua bị coi là dùng lại; chưa dùng cửa sổ ân hạn | §4.2 |
| D71 | Refresh token family có trần tuyệt đối 90 ngày (`family_started_at`); trượt 30 ngày trong trần | §4.3 |

`[OPEN]` còn lại (hoãn, ngoài Phase 1): mã hóa SQLite at rest, certificate pinning, bảng audit, mã hóa PostgreSQL at rest.
