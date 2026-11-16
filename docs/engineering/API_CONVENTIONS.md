# LifeOS — API Conventions v1 (Phase 1)

> **Trạng thái:** v1 — đã duyệt 2026-11-05.
> **Phạm vi:** quy ước REST và danh mục endpoint Phase 1 để backend và Flutter cùng code theo một hợp đồng.
> **Nguồn:** `PRD.md`, `SYSTEM_ARCHITECTURE.md` §4 (hợp đồng sync), `DATABASE_DESIGN.md`, `SECURITY_RULES.md`, `JAVA_SPRING_RULES.md` §5–§6.
> Quy ước nhãn: `[CONFIRMED]` đã chốt · `[PROPOSED]` đề xuất chưa duyệt · `[OPEN]` chưa quyết định. Agent chỉ áp dụng `[CONFIRMED]`.
> Nếu tài liệu này mâu thuẫn với `SYSTEM_ARCHITECTURE.md` §4 về sync, **§4 của kiến trúc là nguồn đúng** cho `push/pull`.

---

## 1. Nguyên tắc chung

1. `[CONFIRMED]` REST over HTTPS, JSON. Tiền tố `/api/v1`.
2. `[CONFIRMED]` Lỗi theo RFC 9457 Problem Details.
3. `[CONFIRMED]` Backend validate mọi thứ; client không được tin.
4. `[CONFIRMED]` ID là UUID; thời điểm ISO-8601 UTC; ngày nghiệp vụ `yyyy-MM-dd`; giờ `HH:mm`.
5. `[CONFIRMED]` D56: tên trường JSON dùng `camelCase`; không bọc (envelope) phản hồi thành công — trả thẳng tài nguyên hoặc đối tượng danh sách; lỗi dùng `application/problem+json`.
6. `[CONFIRMED]` Đường ghi chính của ứng dụng là `POST /sync/push`; REST CRUD là đường phụ cho công cụ, kiểm thử và luồng online-only, nhưng **đi qua cùng Application Service và `ChangeRecorder`** `[CONFIRMED]`.
7. `[CONFIRMED]` Thay đổi tương thích (thêm trường tùy chọn, thêm endpoint) không đổi phiên bản; thay đổi phá vỡ phải lên `/api/v2` và có thời gian chạy song song.

---

## 2. Định dạng, tiêu đề và mã hóa

| Chủ đề | Quy ước |
|---|---|
| Content-Type | Yêu cầu `application/json; charset=utf-8`; lỗi trả `application/problem+json` |
| Mã hóa | UTF-8; nén gzip nếu client hỗ trợ |
| Trường null | Trường tùy chọn không có giá trị trả `null` (không bỏ trường) trong phản hồi |
| Trường lạ trong request | Xem `SECURITY_RULES.md` §9.4 |
| Boolean/enum | `true/false`; enum viết hoa (`OPEN`, `DONE`, `NONE`, `LOW`, `MEDIUM`, `HIGH`) |
| UUID | Chuỗi chữ thường, định dạng chuẩn 8-4-4-4-12 |
| Thời điểm | `2026-11-05T10:20:00Z` (luôn `Z`, có thể có phần thập phân giây) |
| Ngày/giờ nghiệp vụ | `dueDate: "2026-11-06"`, `dueTime: "08:30"` (giờ địa phương theo timezone user, không có múi giờ) |
| Số nguyên lớn | `version` là số nguyên JSON (BIGINT); client dùng kiểu 64-bit |
| Tiền `[CONFIRMED]` (PHASE_3 D100, BACKEND_MASTER_PLAN §5 #8) | **Chuỗi** thập phân, không bao giờ là số JSON: `"amount": "125000"`, `"19.99"`, `"-1500000"` (số dư đầu kỳ). Request gửi số JSON cho trường tiền → 400 `MALFORMED_REQUEST` (REST) / `REJECTED VALIDATION_FAILED` (sync). Phản hồi bỏ số 0 vô nghĩa ở cuối (`"10.5"`, không `"10.50"`). Số chữ số thập phân vượt mức của tiền tệ → `VALIDATION_FAILED` fieldError mã `SCALE`; ngoài giới hạn → `OUT_OF_RANGE`; không làm tròn. Luôn kèm `currency` ISO 4217; tổng hợp tách theo tiền tệ, không quy đổi |

Tiêu đề request/response:

| Tiêu đề | Hướng | Mô tả |
|---|---|---|
| `Authorization: Bearer <accessToken>` | request | Bắt buộc với endpoint không công khai |
| `X-Correlation-Id` | request (tùy chọn), response | Nếu client gửi UUID thì dùng, không thì server sinh; luôn trả lại trong response và log `[CONFIRMED]` |
| `X-LifeOS-Client` | request | `android/1.0.0 (42)` hoặc `ios/1.0.0 (42)`; dùng để áp chính sách phiên bản tối thiểu `[CONFIRMED]` D57 |
| `Retry-After` | response | Với 429 và 503 (giây) |
| `Location` | response | Với 201: URL tài nguyên vừa tạo |

Phiên bản tối thiểu `[CONFIRMED]` D57: server cấu hình `minSupportedClientVersion`; client thấp hơn nhận **426** với `errorCode = CLIENT_UPGRADE_REQUIRED`. Ứng dụng hiển thị yêu cầu cập nhật; dữ liệu local giữ nguyên, không sync cho đến khi cập nhật.

---

## 3. Xác thực

- Endpoint **công khai** (không cần token): `POST /auth/register`, `/auth/verify-email`, `/auth/resend-verification`, `/auth/login`, `/auth/refresh`, `/auth/logout`, `/auth/forgot-password`, `/auth/reset-password`, và `GET /actuator/health` `[CONFIRMED]` (danh sách khớp `SECURITY_RULES.md` §7).
- Còn lại cần access token hợp lệ; thiếu/sai/hết hạn → 401.
- Token, thời hạn, xoay vòng: `SECURITY_RULES.md` §4.

---

## 4. Phương thức HTTP và mã trạng thái

| Hành động | Phương thức | Thành công |
|---|---|---|
| Đọc một tài nguyên | `GET /x/{id}` | 200 |
| Đọc danh sách | `GET /x` | 200 |
| Tạo | `POST /x` (client cấp `id`) | 201 + `Location` |
| Cập nhật một phần | `PATCH /x/{id}` (JSON Merge Patch, RFC 7396) | 200 + tài nguyên mới |
| Hành động nghiệp vụ | `POST /x/{id}/{action}` (`complete`, `reopen`, `archive`, `unarchive`) | 200 + tài nguyên mới |
| Xóa | `DELETE /x/{id}?version=n` | 204 |
| Hành động không trả dữ liệu | `POST …` | 204 hoặc 202 |

Quy tắc `[CONFIRMED]` D62:
- `POST` tạo tài nguyên yêu cầu client gửi `id` (UUID). `id` đã tồn tại → **409** `ENTITY_ALREADY_EXISTS`, cùng phản hồi dù `id` thuộc user nào (`SECURITY_RULES.md` §7.6).
- `PATCH` theo JSON Merge Patch: trường vắng = không đổi; trường `null` = xóa giá trị (chỉ với trường cho phép null). `version` **bắt buộc** (xem §6).
- `DELETE` dùng tham số truy vấn `version` (bắt buộc) vì nhiều client không gửi body cho DELETE.

Bảng ánh xạ trạng thái:

| Mã | Dùng khi |
|---|---|
| 200 / 201 / 202 / 204 | Thành công như trên |
| 400 | JSON sai cú pháp; validation thất bại (`VALIDATION_FAILED`) |
| 401 | Thiếu/sai/hết hạn token; đăng nhập sai |
| 403 | Hiếm: đã xác thực nhưng không được phép làm hành động hệ thống (không dùng cho dữ liệu user khác) |
| 404 | Không tồn tại **hoặc** thuộc user khác (không phân biệt) `[CONFIRMED]` |
| 409 | Xung đột `version` (`ENTITY_VERSION_CONFLICT`); id đã tồn tại; trùng tên (`CATEGORY_NAME_TAKEN`) |
| 413 | Body/payload quá lớn |
| 422 | Dữ liệu hợp lệ về cú pháp nhưng vi phạm quy tắc nghiệp vụ (nếu phân biệt với 400; xem ghi chú) |
| 426 | Client dưới phiên bản tối thiểu |
| 429 | Vượt giới hạn tần suất (kèm `Retry-After`) |
| 500 | Lỗi không lường trước (thông điệp chung + `correlationId`) |
| 503 | Chưa sẵn sàng / hết thời gian chờ khóa sync (kèm `Retry-After`) |

Ghi chú 400 và 422 `[CONFIRMED]`: lỗi **định dạng/ràng buộc trường** → 400 `VALIDATION_FAILED` kèm `fieldErrors`; lỗi **quy tắc nghiệp vụ** (ví dụ `PROJECT_NOT_FOUND` khi gán Task vào Project không tồn tại) → 422 kèm `errorCode` riêng. Ngoại lệ có mã chuyên dụng ở bảng trên (404, 409) giữ nguyên.

---

## 5. Định dạng lỗi `[CONFIRMED]` (RFC 9457) / `[CONFIRMED]` chi tiết

```json
{
  "type": "https://lifeos.app/problems/validation-failed",
  "title": "Dữ liệu không hợp lệ",
  "status": 400,
  "detail": "Một hoặc nhiều trường không hợp lệ.",
  "errorCode": "VALIDATION_FAILED",
  "correlationId": "7f0c1b5e-…",
  "fieldErrors": [
    { "field": "title", "code": "SIZE", "message": "Tiêu đề tối đa 200 ký tự." }
  ]
}
```

- `type`: URI ổn định theo `errorCode` (không bắt buộc phải truy cập được).
- `errorCode`: `UPPER_SNAKE_CASE`, ổn định, là thứ client dùng để rẽ nhánh và tra chuỗi tiếng Việt; **không** parse `detail`/`title`.
- `detail`/`title` tiếng Việt hoặc trung tính, **không** chứa dữ liệu nhạy cảm hoặc chi tiết nội bộ.
- `fieldErrors`: chỉ có với lỗi validation; `field` là tên trường JSON; `code` là mã ổn định (`REQUIRED`, `SIZE`, `PATTERN`, `INVALID_VALUE`).
- Lỗi 5xx: `title` chung, `detail` chung, có `correlationId`.

### 5.1. Danh mục `errorCode` Phase 1 `[CONFIRMED]`

| errorCode | HTTP | Ý nghĩa |
|---|---|---|
| `VALIDATION_FAILED` | 400 | Lỗi định dạng/ràng buộc trường |
| `MALFORMED_REQUEST` | 400 | JSON sai cú pháp / kiểu dữ liệu |
| `AUTH_INVALID_CREDENTIALS` | 401 | Email/mật khẩu không đúng |
| `AUTH_EMAIL_NOT_VERIFIED` | 401 | Mật khẩu đúng nhưng chưa xác minh email |
| `AUTH_TOKEN_EXPIRED` | 401 | Access token hết hạn |
| `AUTH_TOKEN_INVALID` | 401 | Access token sai/bị thu hồi |
| `AUTH_REFRESH_TOKEN_INVALID` | 401 | Refresh token sai, hết hạn, đã dùng, thiết bị bị thu hồi |
| `AUTH_VERIFICATION_TOKEN_INVALID` | 400 | Token xác minh email sai/hết hạn/đã dùng |
| `AUTH_RESET_TOKEN_INVALID` | 400 | Token đặt lại mật khẩu sai/hết hạn/đã dùng |
| `AUTH_PASSWORD_TOO_WEAK` | 400 | Mật khẩu không đạt chính sách (kèm `fieldErrors`) |
| `AUTH_CURRENT_PASSWORD_WRONG` | 400 | Mật khẩu hiện tại sai khi đổi mật khẩu |
| `RATE_LIMITED` | 429 | Vượt giới hạn tần suất |
| `CLIENT_UPGRADE_REQUIRED` | 426 | Phiên bản ứng dụng quá cũ |
| `TASK_NOT_FOUND` / `PROJECT_NOT_FOUND` / `CATEGORY_NOT_FOUND` / `DEVICE_NOT_FOUND` | 404 hoặc 422* | Không tồn tại hoặc không thuộc user |
| `ENTITY_VERSION_CONFLICT` | 409 | `version` không khớp |
| `ENTITY_ALREADY_EXISTS` | 409 | `id` đã tồn tại |
| `CATEGORY_NAME_TAKEN` | 409 | Trùng tên Category |
| `PAYLOAD_TOO_LARGE` | 413 | Vượt giới hạn kích thước |
| `SYNC_BATCH_TOO_LARGE` | 400 | Lô push > 50 mutation |
| `SYNC_UNKNOWN_ENTITY_TYPE` | (trong kết quả mutation) | `entityType` không hỗ trợ |
| `SYNC_OPERATION_NOT_ALLOWED` | (trong kết quả mutation) | Thao tác không được phép với `entityType` (vd `CREATE`/`DELETE` `user_settings`) `[CONFIRMED]` D88 |
| `USER_SETTINGS_NOT_FOUND` | (trong kết quả mutation) | `entityId` không phải dòng settings của user `[CONFIRMED]` D88 |
| `HABIT_NOT_FOUND`, `HABIT_CHECKIN_NOT_FOUND`, `ROUTINE_NOT_FOUND`, `ROUTINE_STEP_NOT_FOUND`, `ROUTINE_LOG_NOT_FOUND` | 404 hoặc 422* | Phase 2 (PHASE_2 §6) `[CONFIRMED]` |
| `HABIT_INVALID_SCHEDULE`, `ROUTINE_INVALID_SCHEDULE` | 422 | Lịch không hợp lệ (D230, D240), kèm `fieldErrors` chỉ trường sai `[CONFIRMED]` |
| `HABIT_DATE_OUT_OF_RANGE` | 422 | Ngày check-in ngoài [startOn, endOn] hoặc sau hôm nay + 1 theo múi giờ người dùng (D231) `[CONFIRMED]` |
| `LIMIT_EXCEEDED` | 422 | Vượt giới hạn số lượng (dùng chung các phase; Phase 2: 200 habit, 50 routine, 30 bước) `[CONFIRMED]` |
| `FINANCE_ACCOUNT_NOT_FOUND`, `FINANCE_CATEGORY_NOT_FOUND`, `FINANCE_TRANSACTION_NOT_FOUND`, `FINANCE_TRANSFER_NOT_FOUND`, `FINANCE_BUDGET_NOT_FOUND`, `FINANCE_RECURRING_NOT_FOUND`, `FINANCE_GOAL_NOT_FOUND` | 404 hoặc 422* | Phase 3 (PHASE_3 §6, D114) `[CONFIRMED]`. Khoản góp không tồn tại dùng `NOT_FOUND` `[PROPOSED]` |
| `FINANCE_ACCOUNT_ARCHIVED` | 422 | Thêm giao dịch/chuyển khoản/định kỳ vào tài khoản đã lưu trữ (D107) `[CONFIRMED]` |
| `FINANCE_ACCOUNT_HAS_TRANSACTIONS` | 409 | Xóa tài khoản còn giao dịch/chuyển khoản/định kỳ chưa xóa (FR-FIN-02) `[CONFIRMED]` |
| `FINANCE_CATEGORY_IN_USE` | 409 | Xóa danh mục đang được giao dịch/ngân sách/định kỳ tham chiếu (D106) `[CONFIRMED]` |
| `FINANCE_CATEGORY_KIND_MISMATCH` | 422 | Danh mục không đúng loại thu/chi `[CONFIRMED]` |
| `FINANCE_CURRENCY_MISMATCH` | 422 | Chuyển khoản khác tiền tệ (D104); sửa giao dịch / giao dịch định kỳ sang tài khoản khác tiền tệ (D103 — cập nhật 2026-11-15 theo BACKEND_FIX_REPORT F1) `[CONFIRMED]` |
| `FINANCE_CURRENCY_LOCKED` | 409 | Đổi tiền tệ tài khoản đã có giao dịch (FIN-08), hoặc mục tiêu đã có khoản góp `[PROPOSED]` |
| `FINANCE_BUDGET_EXISTS` | 409 | Đã có ngân sách đang hoạt động cùng (danh mục hoặc tổng, tiền tệ) (D109) `[CONFIRMED]` |
| `FINANCE_NAME_TAKEN` | 409 | Trùng tên tài khoản / danh mục cùng loại `[CONFIRMED]` |
| `CALENDAR_EVENT_NOT_FOUND`, `CALENDAR_OCCURRENCE_NOT_FOUND`, `TIME_BLOCK_NOT_FOUND`, `REMINDER_NOT_FOUND`, `NOTIFICATION_NOT_FOUND` | 404 hoặc 422* | Phase 4 (PHASE_4 §6) `[CONFIRMED]`; `CALENDAR_OCCURRENCE_NOT_FOUND` 422 khi lần xuất hiện không khớp quy tắc |
| `CALENDAR_INVALID_RECURRENCE`, `CALENDAR_INVALID_TIMEZONE` | 422 | Quy tắc lặp D126 / múi giờ IANA không hợp lệ `[CONFIRMED]` |
| `REMINDER_TARGET_NOT_FOUND` | 422 | Mục tiêu reminder không tồn tại / của người khác `[CONFIRMED]` |
| `NOTIFICATION_PREFERENCE_LOCKED` | 422 | Tắt SECURITY × (IN_APP, EMAIL) (D136) `[CONFIRMED]` |
| `NOTE_NOT_FOUND`, `TAG_NOT_FOUND`, `GOAL_NOT_FOUND`, `MILESTONE_NOT_FOUND` | 404/422\* | PHASE_6 §6 `[CONFIRMED]` |
| `TAG_NAME_TAKEN` | 409 | Tên thẻ trùng (không phân biệt hoa thường) `[CONFIRMED]` |
| `NOTE_TAG_LIMIT_EXCEEDED` | 422 | > 20 thẻ hoặc > 20 liên kết trên một ghi chú `[CONFIRMED]` |
| `GOAL_INVALID_STATUS_TRANSITION` | 422 | Chuyển trạng thái Goal ngoài bảng D188 `[CONFIRMED]` |
| `MILESTONE_LIMIT_EXCEEDED` | 422 | > 20 milestone/goal `[CONFIRMED]` |
| `GOAL_LINK_MISMATCH` | 422 | `milestoneId` không thuộc `goalId` (D186) `[CONFIRMED]` |
| `REVIEW_NOT_FOUND` | 404 | PHASE_7 `[CONFIRMED]` |
| `REVIEW_EXISTS` | 409 | Kỳ đã có review (D202) `[CONFIRMED]` |
| `ANALYTICS_RANGE_TOO_LARGE` | 422 | Vượt 366 ngày (theo ngày) / 24 tháng (tuần, tháng) — D208; hoặc kỳ có hơn 200.000 Task hoàn thành (không cắt im lặng — cập nhật 2026-11-15 theo BACKEND_FIX_REPORT F7; áp cho analytics, review, dashboard) `[CONFIRMED]` |
| `EXPORT_NOT_FOUND` | 404 | `[CONFIRMED]` |
| `EXPORT_LIMIT_REACHED` | 429 | > 2 yêu cầu xuất/24 giờ (D204) `[CONFIRMED]` |
| `EXPORT_NOT_READY` | 409 | Tệp chưa READY `[CONFIRMED]` |
| `EXPORT_EXPIRED` | 410 | Quá 24 giờ hoặc hết 5 lượt tải `[CONFIRMED]` |
| `ACCOUNT_DELETION_PENDING` | 403 | Mọi API cần xác thực trong thời gian chờ xóa, trừ `GET/DELETE /account/deletion` (D205) `[CONFIRMED]` |
| `ACCOUNT_DELETION_NOT_FOUND` | 404 | Không có yêu cầu xóa đang chờ `[CONFIRMED]` |
| `SYNC_FULL_RESYNC_REQUIRED` | 410 | Cursor nhỏ hơn `min_retained_seq` (PHASE_5 D150) `[CONFIRMED]` |
| `SYNC_SNAPSHOT_EXPIRED` | 410 | `snapshotToken` hết hạn 30 phút (D152) `[CONFIRMED]` |
| `SERVICE_UNAVAILABLE` | 503 | Chưa sẵn sàng / hết thời gian chờ khóa |
| `INTERNAL_ERROR` | 500 | Lỗi không lường trước |
| `NOT_FOUND` | 404 | Đường dẫn không tồn tại (lỗi giao thức của framework) `[CONFIRMED]` D73 |
| `METHOD_NOT_ALLOWED` | 405 | Phương thức HTTP không được hỗ trợ `[CONFIRMED]` D73 |
| `REQUEST_REJECTED` | 4xx | Các lỗi giao thức chuẩn khác (ví dụ 415) `[CONFIRMED]` D73 |

\* `*_NOT_FOUND`: 404 khi là **tài nguyên đích của URL** (`GET /tasks/{id}`); 422 khi là **tham chiếu trong body** (ví dụ `projectId` của Task).

Trong kết quả mutation sync (`REJECTED`), `errorCode` dùng cùng danh mục (không có HTTP status riêng).

---

## 6. Kiểm soát đồng thời (optimistic locking) `[CONFIRMED]` cơ chế / `[CONFIRMED]` D58

- Mọi tài nguyên đồng bộ có `version` (số nguyên, bắt đầu 1) trong phản hồi.
- `PATCH`, `POST …/{action}` và `DELETE` **phải** gửi `version` mong đợi (body hoặc query). Không khớp → **409** `ENTITY_VERSION_CONFLICT`; thân lỗi kèm `currentVersion` để client quyết định `[CONFIRMED]`.
- Thành công trả `version` mới.
- Không dùng `ETag/If-Match` ở Phase 1 (giữ một cơ chế duy nhất cho REST và sync).

---

## 7. Phân trang, lọc, sắp xếp `[CONFIRMED]` D59

Danh sách REST dùng phân trang theo con trỏ:

```json
{
  "items": [ { "…": "…" } ],
  "nextCursor": "eyJ…",
  "hasMore": true
}
```

- Tham số: `limit` (mặc định 50, tối đa 200), `cursor` (chuỗi mờ, lấy từ `nextCursor`).
- Thứ tự phải xác định (có tiêu chí phụ `id`) để con trỏ ổn định.
- Danh sách chỉ chứa bản ghi chưa xóa. Tombstone chỉ có qua `sync/pull`.
- Không có đếm tổng (`total`) ở Phase 1.

Lọc Task (`GET /tasks`): `status` (`OPEN|DONE`), `projectId`, `inbox` (`true` = chưa thuộc Project), `categoryId`, `priority`, `dueFrom`, `dueTo` (ngày, bao gồm hai đầu). Các điều kiện kết hợp bằng AND. "Hôm nay/quá hạn/sắp tới" do client tính theo timezone của user rồi truyền thành `dueFrom/dueTo`.

Sắp xếp: `sort=<trường>:<asc|desc>`, trường cho phép `dueDate`, `priority`, `createdAt`, `updatedAt`; mặc định `createdAt:desc`; `dueDate` đặt giá trị null cuối.

Tìm kiếm văn bản (`q`) **không** có ở REST Phase 1: tìm kiếm chạy trên SQLite local (PRD FR-TK-10, `DATABASE_DESIGN.md` D40).

---

## 8. Danh mục endpoint Phase 1

Ký hiệu: 🔓 công khai · 🔒 cần access token.

### 8.1. Auth

| Endpoint | Request | Phản hồi | Lỗi chính |
|---|---|---|---|
| 🔓 `POST /auth/register` | `{ "email", "password", "displayName", "timezone"? }` | `202` (luôn như nhau dù email đã tồn tại) | 400 `VALIDATION_FAILED`, `AUTH_PASSWORD_TOO_WEAK`; 429 |
| 🔓 `POST /auth/verify-email` | `{ "token" }` | `204` | 400 `AUTH_VERIFICATION_TOKEN_INVALID` |
| 🔓 `POST /auth/resend-verification` | `{ "email" }` | `202` (luôn) | 429 |
| 🔓 `POST /auth/login` | xem dưới | `200` xem dưới | 401 `AUTH_INVALID_CREDENTIALS`, `AUTH_EMAIL_NOT_VERIFIED`; 429 |
| 🔓 `POST /auth/refresh` | `{ "refreshToken" }` | `200` bộ token mới | 401 `AUTH_REFRESH_TOKEN_INVALID`; 429 |
| 🔓 `POST /auth/logout` | `{ "refreshToken" }` | `204` (luôn, kể cả token đã hết hạn) | — |
| 🔓 `POST /auth/forgot-password` | `{ "email" }` | `202` (luôn) | 429 |
| 🔓 `POST /auth/reset-password` | `{ "token", "newPassword" }` | `204` | 400 `AUTH_RESET_TOKEN_INVALID`, `AUTH_PASSWORD_TOO_WEAK` |

`timezone` trong `register` tùy chọn (IANA); thiếu thì dùng `Asia/Ho_Chi_Minh` `[CONFIRMED]`; không hợp lệ → 400.

Đăng nhập:

```json
// request
{
  "email": "a@example.com",
  "password": "********",
  "deviceId": "3d6f…",              // tùy chọn: ID server cấp ở lần trước
  "device": { "name": "Pixel 8", "platform": "ANDROID", "appVersion": "1.0.0" }
}
// response 200
{
  "tokenType": "Bearer",
  "accessToken": "eyJ…",
  "expiresIn": 900,
  "refreshToken": "q8Zx…",
  "deviceId": "3d6f…",
  "user": { "id": "…", "email": "a@example.com", "displayName": "An" }
}
```

`POST /auth/refresh` trả `{ tokenType, accessToken, expiresIn, refreshToken, deviceId }`. Client phải **lưu refreshToken mới trước khi bỏ token cũ** (`SECURITY_RULES.md` §4.3).

### 8.2. Tài khoản và cài đặt

| Endpoint | Request | Phản hồi |
|---|---|---|
| 🔒 `GET /me` | — | `{ id, email, displayName, emailVerified, createdAt }` |
| 🔒 `PATCH /me` | `{ displayName? }` | `200` hồ sơ mới |
| 🔒 `POST /me/change-password` | `{ currentPassword, newPassword }` | `204` |
| 🔒 `GET /me/settings` | — | `{ id, timezone, locale, defaultCurrency, weekStartDay, version, updatedAt }` (`updatedAt` thêm 2026-11-07, quyết định A Giai đoạn 3: `data` sync của `user_settings` = Response này bỏ `id`/`version`, `SYNC_PROTOCOL.md` §6.1) |
| 🔒 `PATCH /me/settings` | `{ version, timezone?, locale?, defaultCurrency?, weekStartDay?, quietStart?, quietEnd?, showDetailsOnLockScreen? }` | `200` settings mới. WP4 (§5 #1): Response thêm `quietStart`, `quietEnd` (`"HH:mm"`/`null`, theo cặp), `showDetailsOnLockScreen` (mặc định `false`) |

`email` không đổi được ở Phase 1 (PRD FR-ID-07).

### 8.3. Thiết bị

| Endpoint | Phản hồi |
|---|---|
| 🔒 `GET /devices` | `{ "items": [ { id, name, platform, appVersion, lastSeenAt, current, revokedAt, pushTokenSuffix } ] }` (không phân trang; số thiết bị nhỏ). `pushTokenSuffix`: 6 ký tự cuối token push (PHASE_4 D147, WP4) |
| 🔒 `PUT /devices/{id}/push-token` `{token, platform}`, `DELETE /devices/{id}/push-token` | `204`; thiết bị không thuộc user/đã thu hồi → 404 `DEVICE_NOT_FOUND`; 20 lần/giờ/user; token đổi chủ được chuyển sang thiết bị mới; ≤ 10 token/user (D139). Thu hồi thiết bị hoặc đăng xuất gỡ token của thiết bị đó |
| 🔒 `DELETE /devices/{id}` | `204`; 404 nếu không thuộc user; thu hồi chính thiết bị đang dùng được phép |

### 8.4. Task

Ví dụ cập nhật (`version` bắt buộc; thiếu → 400 `VALIDATION_FAILED` với `fieldErrors[field=version, code=REQUIRED]`):

```json
PATCH /api/v1/tasks/5c1a…
{ "version": 3, "title": "Mua sữa tươi", "dueTime": null }
```

Tài nguyên Task:

```json
{
  "id": "5c1a…",
  "title": "Mua sữa",
  "description": null,
  "status": "OPEN",
  "priority": "NONE",
  "dueDate": "2026-11-06",
  "dueTime": null,
  "completedAt": null,
  "projectId": null,
  "categoryId": null,
  "goalId": null,
  "milestoneId": null,
  "version": 1,
  "createdAt": "2026-11-05T10:20:00Z",
  "updatedAt": "2026-11-05T10:20:00Z"
}
```

| Endpoint | Request | Ghi chú |
|---|---|---|
| 🔒 `GET /tasks` | tham số §7 | `{items,nextCursor,hasMore}` |
| 🔒 `GET /tasks/{id}` | — | 404 nếu không có/không thuộc user |
| 🔒 `POST /tasks` | `{ id, title, description?, status?, priority?, dueDate?, dueTime?, projectId?, categoryId?, goalId?, milestoneId? }` | `201`; `status` mặc định `OPEN`; `title` 1–200; `description` ≤ 10.000; `dueTime` chỉ khi có `dueDate`; `projectId/categoryId` thuộc user; `goalId/milestoneId` (WP6, D186): có `milestoneId` thì goal suy ra từ milestone, lệch → 422 `GOAL_LINK_MISMATCH` |
| 🔒 `PATCH /tasks/{id}` | `{ version, …trường đổi }` | 409 khi lệch `version` |
| 🔒 `POST /tasks/{id}/complete` | `{ version }` | `status=DONE`, server đặt `completedAt` |
| 🔒 `POST /tasks/{id}/reopen` | `{ version }` | `status=OPEN`, `completedAt=null` |
| 🔒 `DELETE /tasks/{id}?version=n` | — | soft delete; `204` |

`completedAt`, `createdAt`, `updatedAt`, `version` do server; client gửi sẽ bị bỏ qua (`SECURITY_RULES.md` §7.5).

### 8.5. Project

Tài nguyên: `{ id, name, description, archivedAt, version, createdAt, updatedAt }`.

| Endpoint | Request / ghi chú |
|---|---|
| 🔒 `GET /projects` | `archived=false` (mặc định) \| `true` \| `all`; phân trang §7 |
| 🔒 `GET /projects/{id}` | — |
| 🔒 `POST /projects` | `{ id, name, description? }`; `name` 1–100 |
| 🔒 `PATCH /projects/{id}` | `{ version, name?, description? }` |
| 🔒 `POST /projects/{id}/archive` / `…/unarchive` | `{ version }` |
| 🔒 `DELETE /projects/{id}?version=n` | soft delete; server cascade Task về Inbox (`SYSTEM_ARCHITECTURE.md` §3.8) |
| 🔒 `GET /projects/{id}/progress` | `{ projectId, totalTasks, completedTasks, openTasks, completionRatio }`; Task đã xóa không tính; không Task → `0/0`, `completionRatio = 0` (module `task` phục vụ endpoint này) |

### 8.6. Category

Tài nguyên: `{ id, name, version, createdAt, updatedAt }`.

| Endpoint | Request / ghi chú |
|---|---|
| 🔒 `GET /categories` | phân trang §7 |
| 🔒 `GET /categories/{id}` | — |
| 🔒 `POST /categories` | `{ id, name }`; `name` 1–50, duy nhất theo user không phân biệt hoa thường (bỏ qua đã xóa); trùng → 409 `CATEGORY_NAME_TAKEN` |
| 🔒 `PATCH /categories/{id}` | `{ version, name? }` |
| 🔒 `DELETE /categories/{id}?version=n` | soft delete; server cascade Task về "không danh mục" |

### 8.7. Sync

Hợp đồng đầy đủ (JSON, trạng thái từng mutation, cursor) nằm ở `SYSTEM_ARCHITECTURE.md` §4; đặc tả cài đặt (ma trận kết quả, payload từng entity, lỗi, khóa) ở `SYNC_PROTOCOL.md`. Tóm tắt:

| Endpoint | Ghi chú |
|---|---|
| 🔒 `POST /sync/push` | Lô ≤ 50 mutation; luôn `200` nếu request hợp lệ, trạng thái nằm ở từng mutation (`APPLIED/DUPLICATE/CONFLICT/REJECTED`); lỗi toàn request theo §5; hết thời gian chờ khóa → `503 + Retry-After` (client gửi lại cùng `mutationId`) |
| 🔒 `GET /sync/pull?cursor=&limit=` | Mặc định 200, tối đa 500; trả `changes[]`, `nextCursor`, `hasMore`; cursor đã bị dọn → 410 `SYNC_FULL_RESYNC_REQUIRED` (WP5) |
| 🔒 `POST /sync/snapshot`, `GET /sync/entities/{type}/{id}`, `POST /sync/diagnostics` | Full resync, tra một entity, báo cáo chẩn đoán (PHASE_5 §6; `SYNC_PROTOCOL.md` §6.0) |

Kết quả `CONFLICT` của `CREATE` trùng khóa tự nhiên (check-in/lượt chạy/kết quả bước) có thêm `conflictReason: "ALREADY_EXISTS"` (`SYNC_PROTOCOL.md` §4.1, Phase 2 D235).

### 8.8. Habit & Routine (Phase 2, `PHASE_2_HABIT_ROUTINE.md` §6) `[CONFIRMED]`

| Endpoint | Ghi chú |
|---|---|
| 🔒 `POST /habits` (201), `GET /habits?archived=&scheduledOn=&limit=&cursor=`, `GET/PATCH /habits/{id}`, `DELETE /habits/{id}?version=` (204), `POST /habits/{id}/archive` `/unarchive` | `GET` kèm `currentStreak`, `longestStreak` (tính khi đọc); `scheduledOn` trả mọi habit đến lịch ngày đó trong một trang |
| 🔒 `PUT /habits/{id}/checkins/{date}` `{id?, status, note?}` | Upsert idempotent theo (habit, ngày): 201 tạo, 200 cập nhật/không đổi; không cần `version` |
| 🔒 `DELETE /habits/{id}/checkins/{date}` | Hoàn tác, 204, idempotent |
| 🔒 `GET /habits/{id}/checkins?from=&to=&limit=&cursor=` | Lịch sử, mới nhất trước |
| 🔒 `GET /habits/{id}/stats?from=&to=`, `GET /habits/stats?from=&to=` | D234; khoảng ≤ 366 ngày |
| 🔒 `POST /routines` (201), `GET /routines`, `GET/PATCH /routines/{id}` (chi tiết kèm bước), `DELETE /routines/{id}?version=`, `POST /routines/{id}/archive` `/unarchive` | |
| 🔒 `POST /routines/{id}/steps` (201), `PATCH /routine-steps/{id}`, `DELETE /routine-steps/{id}?version=` | `position` bước 1000 (D241) |
| 🔒 `PUT /routines/{id}/logs/{date}` `{id?, totalSteps}` | Start idempotent (201/200) |
| 🔒 `PUT /routine-logs/{logId}/steps/{stepId}` `{id?, status}`, `DELETE /routine-logs/{logId}/steps/{stepId}` | Hoàn thành/bỏ qua/hoàn tác bước; `completedAt` của lượt do server |
| 🔒 `GET /routines/{id}/logs?from=&to=`, `GET /routines/{id}/stats?from=&to=` | D245 |

### 8.9. Finance (Phase 3, `PHASE_3_FINANCE.md` §6) `[CONFIRMED]`

Tiền đi dạng chuỗi (§2). Tiền tố `/api/v1/finance`.

| Endpoint | Ghi chú |
|---|---|
| 🔒 `POST /accounts` (201), `GET /accounts?archived=`, `GET/PATCH /accounts/{id}`, `POST /accounts/{id}/archive` `/unarchive`, `DELETE /accounts/{id}?version=` | `GET` kèm `balance` (hôm nay theo múi giờ người dùng, D102, D108) |
| 🔒 `GET /accounts/{id}/balance?asOf=`, `POST /accounts/{id}/reconcile` `{statementBalance, asOfDate}` | Đối soát chỉ đọc: `{computedBalance, difference}` (D112) |
| 🔒 `POST /categories`, `GET /categories?kind=&archived=`, `GET/PATCH/DELETE /categories/{id}`, `POST /categories/{id}/archive` `/unarchive` | `systemKey` khác null với danh mục mặc định (D113); `kind` bất biến |
| 🔒 `POST /transactions`, `GET /transactions?kind=&accountId=&categoryId=&from=&to=&minAmount=&maxAmount=&q=&limit=&cursor=`, `GET/PATCH/DELETE /transactions/{id}` | Sắp `occurredOn desc, id desc` (keyset); `q` ≤ 100 ký tự, tìm nguyên văn trong `note` (ILIKE có escape) |
| 🔒 `POST /transfers`, `GET /transfers?accountId=&from=&to=`, `GET/PATCH/DELETE /transfers/{id}` | Hai tài khoản bất biến (`IMMUTABLE`) |
| 🔒 `POST /budgets`, `GET /budgets?archived=`, `GET/PATCH/DELETE /budgets/{id}`, `POST /budgets/{id}/archive` `/unarchive`, `GET /budgets/{id}/usage?month=`, `GET /budgets/usage?month=` | `{spent, limit, ratio, remaining}`; tháng lịch theo múi giờ người dùng; không tính chuyển khoản, `ADJUSTMENT` (D109, D119). `/unarchive` bổ sung cho đối xứng `[PROPOSED]` |
| 🔒 `POST /recurring`, `GET /recurring`, `GET/PATCH/DELETE /recurring/{id}`, `POST /recurring/{id}/pause` `/resume` | `nextRunOn` do server; giao dịch do job sinh (D110) |
| 🔒 `POST /saving-goals`, `GET /saving-goals?status=`, `GET/PATCH/DELETE /saving-goals/{id}`, `POST/GET /saving-goals/{id}/contributions`, `DELETE /saving-contributions/{id}?version=` | `GET` kèm `{saved, target, ratio, remaining}`; `ACHIEVED` do server (D111) |
| 🔒 `GET /reports/monthly?month=&currency=`, `GET /reports/cash-flow?from=&to=&granularity=day|week|month&currency=` | Một tiền tệ; ≤ 366 ngày (day) / < 24 tháng (week, month) |

### 8.10. Calendar & Notification (Phase 4, `PHASE_4_CALENDAR_NOTIFICATION.md` §6) `[CONFIRMED]`

| Endpoint | Ghi chú |
|---|---|
| 🔒 `POST /calendar/events`, `GET /calendar/events`, `GET/PATCH /calendar/events/{id}` | PATCH = sửa cả chuỗi ("tất cả"); `startAt/endAt/recurrenceUntil` dẫn xuất UTC do server |
| 🔒 `DELETE /calendar/events/{id}?version=&fromOriginalStart=` | Không có `fromOriginalStart` (hoặc là lần đầu) → xóa chuỗi (cascade ngoại lệ, reminder); có → "xóa từ lần này trở đi" (cắt chuỗi) |
| 🔒 `GET/POST /calendar/events/{id}/exceptions`, `DELETE /calendar/events/{id}/exceptions/{exceptionId}?version=` | "Chỉ lần này" (D127) |
| 🔒 `POST /calendar/events/{id}/split` `{version, originalStartLocal, newEventId, title?, …}` | "Từ lần này": cắt chuỗi gốc + tạo chuỗi mới (201) |
| 🔒 `GET /calendar?from=&to=` | Ngày theo múi giờ người dùng (≤ 366 ngày); Event (đã mở rộng lặp) + Time Block + Task có hạn, mỗi mục có `source`; > 1000 lần xuất hiện → 422 `LIMIT_EXCEEDED` (D125, D145) |
| 🔒 `POST/GET /time-blocks?taskId=&from=&to=`, `GET/PATCH/DELETE /time-blocks/{id}` | ≤ 24 giờ, trùng giờ không bị chặn (D128) |
| 🔒 `POST/GET /reminders`, `GET/PATCH/DELETE /reminders/{id}`, `POST /reminders/{id}/pause` `/resume` | `nextFireAt` do server (D130) |
| 🔒 `GET /notifications?unread=&limit=&cursor=`, `GET /notifications/unread-count`, `PATCH /notifications/{id}/read`, `POST /notifications/read-all`, `DELETE /notifications/{id}` | Không `version`; đánh dấu đọc idempotent; giữ 90 ngày (D142) |
| 🔒 `GET/PUT /notifications/preferences` | 18 dòng category × channel; thiếu dòng = mặc định D136 |
| `GET /notifications/unsubscribe?token=` | **Công khai** (`SecurityConfig.PUBLIC_GET_ENDPOINTS`); token HMAC 30 ngày; chỉ tắt email một loại; 204; token sai/hết hạn/SECURITY → 400; 20 lần/giờ/IP (D143) |

### 8.11. Knowledge & Goals (Phase 6, `PHASE_6_KNOWLEDGE_GOALS.md` §6) `[CONFIRMED]`

| Endpoint | Ghi chú |
|---|---|
| 🔒 `POST /notes`, `GET /notes?q=&tagId=&tagMode=&pinned=&archived=&limit=&cursor=` | Danh sách **không** trả `content` (chỉ `excerpt` 200 ký tự, `contentLength`, `tagIds`). `tagId` lặp = AND, `tagMode=any` = OR; thẻ sai → 422 `TAG_NOT_FOUND`; `archived` = `false` (mặc định) \| `true` \| `all`. Không `q`: con trỏ (`updatedAt desc, id desc`). Có `q` (≤ 100 ký tự, không dấu khớp có dấu, cú pháp websearch): xếp `rank desc, updatedAt desc, id desc`, phân trang offset ≤ 1000 mã hóa trong `cursor` (§4 kế hoạch); 60 lần/phút/user |
| 🔒 `GET/PATCH/DELETE /notes/{id}`, `POST /notes/{id}/archive` `/unarchive` `/pin` `/unpin` | `GET` trả `{note, tags, links}` đầy đủ; ghim ≤ 10 (`LIMIT_EXCEEDED`); xóa cascade thẻ gắn và liên kết |
| 🔒 `PUT /notes/{id}/tags` `{tagIds}` | Thay tập thẻ (≤ 20); trả danh sách thẻ; `note_tag` mới id do server sinh |
| 🔒 `POST /notes/{id}/links` `{id, targetType, targetId}`, `DELETE /notes/{id}/links/{linkId}?version=` | 201 tạo / 200 khi liên kết đã có (trả bản đang có); mục tiêu sai → 422 `TASK_NOT_FOUND`/`GOAL_NOT_FOUND` |
| 🔒 `POST/GET /tags`, `PATCH/DELETE /tags/{id}` | `GET` trả mảng theo tên; xóa cascade `note_tag` |
| 🔒 `POST/GET /goals?status=`, `GET/PATCH/DELETE /goals/{id}`, `POST /goals/{id}/status` `{version, status}` | `GET` trả `{goal, progress, milestones}`; `progress = {method: MILESTONES\|TASKS\|NONE, value (0..1, 4 chữ số) \| null, completedMilestones, totalMilestones, doneTasks, totalTasks, allMilestonesCompleted}` (D185); xóa cascade milestone, gỡ liên kết Task |
| 🔒 `POST /goals/{id}/milestones`, `PATCH/DELETE /milestones/{id}`, `POST /milestones/{id}/complete` `/reopen` | ≤ 20/goal, `position` bước 1000, trùng → 400 |
| 🔒 `GET /goals/{id}/tasks`, `GET /goals/{id}/notes` | Phân trang con trỏ; Goal sai → 404 |

### 8.12. Analytics, xuất dữ liệu, xóa tài khoản (Phase 7, `PHASE_7_INTELLIGENCE_ANALYTICS.md` §6) `[CONFIRMED]`

| Endpoint | Ghi chú |
|---|---|
| 🔒 `GET /dashboard` | `{computedAt, cards:[{id, status: OK\|ERROR\|UNAVAILABLE, data}]}`; thẻ `tasks_today`, `habits_today`, `budgets` (kèm `pace`), `upcoming_events`, `goals` (kèm `risk`); song song, 1,5 s/thẻ; 30 lần/phút/user (D196, D208) |
| 🔒 `GET /reviews/daily?date=`, `/reviews/weekly?date=`, `/reviews/monthly?month=yyyy-MM` | Vắng tham số = kỳ hiện tại theo múi giờ người dùng; trả `tasks, habits, finance[] (theo tiền tệ), time, reflection, previous` (tuần/tháng); `overdueOpen` tính đến hôm nay |
| 🔒 `POST/GET /reviews?periodType=`, `GET/PATCH/DELETE /reviews/{id}` | D202; PATCH chỉ `reflection`, `mood` |
| 🔒 `GET /analytics/tasks?from=&to=&granularity=day\|week\|month` | D198 + bucket + kỳ trước cùng độ dài; `medianCycleSeconds` null khi mẫu < 5 |
| 🔒 `GET /analytics/habits?from=&to=`, `GET /analytics/time?from=&to=&granularity=` | ≤ 366 ngày |
| 🔒 `GET /analytics/finance?from=&to=&granularity=&currency=` | `currency` bắt buộc; `savingsRate` 0..1 (4 chữ số), null khi thu = 0 |
| 🔒 `GET /analytics/goals` | `risk` ∈ `AT_RISK`/`OVERDUE`/null (D200, ngưỡng `lifeos.analytics.at-risk-threshold` = 0,15) |
| 🔒 `POST /account/export` | 202 tạo (200 khi đang có yêu cầu chờ/chạy — trả yêu cầu đó); 429 `EXPORT_LIMIT_REACHED` |
| 🔒 `GET /account/export/{id}`, `GET /account/export/{id}/download` | Zip (`data.json` + `csv/<bảng>.csv`, BOM, chống CSV injection); chỉ chủ sở hữu (xác thực), 24 giờ, 5 lượt |
| 🔒 `POST /account/deletion/code` | 202; gửi mã 6 chữ số qua email (15 phút), 5 lần/giờ |
| 🔒 `POST /account/deletion` `{password, code}` | Sai mật khẩu → 400 `AUTH_CURRENT_PASSWORD_WRONG`; sai mã → 400 `VALIDATION_FAILED` (`code`); 5 lần sai/15 phút; thành công → `{deletionScheduledAt (= +7 ngày), cancellable}`, thu hồi mọi refresh token |
| 🔒 `GET /account/deletion`, `DELETE /account/deletion` | Xem/hủy trong cooldown; không có → 404 `ACCOUNT_DELETION_NOT_FOUND` |

---

## 9. Hành vi riêng của ứng dụng offline-first

1. Client không dùng thời gian máy để xác định bản mới hơn; chỉ `version` `[CONFIRMED]`.
2. Mọi thao tác tạo đều có `id` do client sinh nên gửi lại an toàn; kết hợp `mutationId` ở sync.
3. `503 + Retry-After` và lỗi mạng: client thử lại với backoff, giữ mutation trong outbox.
4. `401`: client refresh một lần dùng chung rồi thử lại; refresh thất bại → phiên hết hạn, giữ nguyên dữ liệu local.
5. `426`: dừng sync, yêu cầu cập nhật ứng dụng, không xóa dữ liệu local.
6. `409 ENTITY_VERSION_CONFLICT` ở REST: client hiển thị thông báo; trong luồng sync, conflict được xử lý theo `SYNC_RULES.md` §9.7.

---

## 10. Tương thích và vòng đời API

- Chỉ **thêm** là tương thích: thêm trường phản hồi, thêm tham số tùy chọn, thêm `errorCode`, thêm `entityType`. Client **phải bỏ qua** trường lạ trong phản hồi.
- Không bao giờ đổi nghĩa hoặc xóa trường, đổi kiểu, hoặc đổi tên `errorCode` trong cùng `v1`.
- Giao thức sync có thể tiến hóa theo cách tương thích; thay đổi phá vỡ cần `v2` và hỗ trợ song song vì thiết bị có thể mang thay đổi chưa sync lâu ngày.
- `[CONFIRMED]` D63: tài liệu OpenAPI sinh tự động từ mã (springdoc) và xuất ra `docs/api/openapi.yaml` khi build; CI so sánh với bản đã commit để phát hiện thay đổi phá vỡ. Endpoint OpenAPI chỉ bật ở môi trường dev/test.
- `[CONFIRMED]` Client Flutter viết tay DTO ở Phase 1 (số endpoint nhỏ); fixture JSON dùng chung cho test hợp đồng (`TESTING_RULES.md`).

---

## 11. Checklist khi thêm hoặc đổi endpoint

- [ ] Có trong bảng §8 và (nếu cần) `errorCode` mới trong §5.1.
- [ ] Xác thực/công khai đúng; endpoint công khai mới có rate limit (`SECURITY_RULES.md` §6).
- [ ] `userId` từ `CurrentUser`; truy vấn kèm `user_id`; 404 chung.
- [ ] DTO riêng, không trả/bind Entity.
- [ ] Ghi dữ liệu đi qua Application Service và `ChangeRecorder`.
- [ ] Có validation, giới hạn kích thước, test lỗi và test IDOR.
- [ ] Cập nhật OpenAPI/fixture và test hợp đồng.

---

## 12. Câu hỏi mở

| # | Nội dung |
|---|---|
| Q-A1 | ~~ETag/If-Match~~ → không dùng ở Phase 1; chỉ `version` (đã chốt) |
| Q-A2 | Giá trị `minSupportedClientVersion` — hoãn đến lần phát hành đầu tiên (hoãn có chủ đích) |
| Q-A3 | ~~Sinh client Dart từ OpenAPI~~ → không; DTO viết tay ở Phase 1 (đã chốt) |

---

## 13. Quyết định (đã duyệt 2026-11-05)

| # | Đề xuất | Mục |
|---|---|---|
| D56 | `camelCase`, không envelope thành công, `application/problem+json` cho lỗi | §1, §2 |
| D57 | Header `X-LifeOS-Client`; 426 `CLIENT_UPGRADE_REQUIRED` khi dưới phiên bản tối thiểu | §2 |
| D58 | `version` bắt buộc cho PATCH/action/DELETE; 409 kèm `currentVersion`; không dùng ETag ở Phase 1 | §6 |
| D59 | Phân trang theo con trỏ (`limit` 50/200, `cursor` mờ); lọc và sắp xếp theo §7; không có `q` ở REST | §7 |
| D60 | Hành động `complete`, `reopen`, `archive`, `unarchive` là `POST /x/{id}/{action}` | §4, §8 |
| D61 | Phân biệt 400 (`VALIDATION_FAILED`) và 422 (quy tắc nghiệp vụ); `*_NOT_FOUND` 404 cho URL đích, 422 cho tham chiếu trong body | §4, §5.1 |
| D62 | `POST` tạo bắt buộc `id` do client; trùng → 409; `PATCH` theo JSON Merge Patch; `DELETE` dùng `?version=` | §4 |
| D63 | OpenAPI sinh từ mã, kiểm tra thay đổi phá vỡ trong CI; DTO Flutter viết tay | §10 |
| D78 | Hành vi REST Giai đoạn 2: `DELETE` bản ghi đã xóa → 404; action vô nghĩa (complete Task đã DONE, archive Project đã lưu trữ…) → kiểm `version` rồi 200, **không** tăng version, **không** ghi `change_logs`; `dueTime` không có `dueDate` → 400 `VALIDATION_FAILED` (fieldError `dueTime`, `INVALID_VALUE`), không tự xóa; `PATCH /tasks` được đổi `status` (`completedAt` do server); gán Task vào Project đã lưu trữ được phép, đã xóa → 422; `GET /tasks` không tự ẩn Task của Project đã lưu trữ (client làm); id của tombstone không dùng lại → 409 `ENTITY_ALREADY_EXISTS`; trim `name`/`title` rồi mới kiểm độ dài, `description` giữ nguyên; tên Category so trùng bằng `lower()` của DB, lưu nguyên kiểu chữ | §4–§8 |
| D79 | Sắp xếp: `priority` theo NONE<LOW<MEDIUM<HIGH, trùng khóa so `id` cùng chiều, `dueDate` null cuối ở cả hai chiều; `/projects` và `/categories` mặc định `createdAt:desc, id:desc`, không có `sort`; `completionRatio` là số thập phân 0–1, làm tròn 4 chữ số HALF_UP, không Task → 0; tổ hợp lọc sai (`inbox=true` + `projectId`, `dueFrom > dueTo`, `sort` lạ) → 400 `VALIDATION_FAILED`; `limit` ngoài 1–200 → 400 | §7, §8.6 |

Đã duyệt toàn bộ ngày 2026-11-05; các mục `[OPEN]` còn lại đều có lịch chốt riêng.
