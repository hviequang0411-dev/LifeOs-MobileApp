# LifeOS — Testing Rules v1 (Phase 1)

> **Trạng thái:** v1 — đã duyệt 2026-11-05.
> **Phạm vi:** chiến lược và quy tắc kiểm thử cho backend (Java), mobile (Flutter) và hợp đồng giữa hai phía. Bổ sung `CODE_GUIDELINES.md` §13.
> **Nguồn:** `SYSTEM_ARCHITECTURE.md` (ArchUnit A1–A12, invariants I1–I9), `SYNC_RULES.md`, `SECURITY_RULES.md` §16.1 (SEC-01..13), `DATABASE_DESIGN.md`, `API_CONVENTIONS.md`.
> Quy ước nhãn: `[CONFIRMED]` đã chốt · `[PROPOSED]` đề xuất chưa duyệt · `[OPEN]` chưa quyết định.

---

## 1. Nguyên tắc

1. `[CONFIRMED]` Code mới phải có test đi kèm trong cùng PR. Agent không được xóa, nới lỏng hoặc `@Disabled` test để làm build xanh.
2. `[CONFIRMED]` Test hành vi và hợp đồng, không test chi tiết cài đặt.
3. `[CONFIRMED]` Bug được sửa phải có test tái hiện trước (test đỏ → sửa → xanh).
4. `[CONFIRMED]` D64: tập trung công sức ở nơi rủi ro cao nhất: **sync, bảo mật/IDOR, migration DB, ranh giới module**. Các vùng này có test bắt buộc (§6–§9); phần còn lại theo mức hợp lý, không đuổi theo con số coverage.
5. `[CONFIRMED]` Test không phụ thuộc thứ tự chạy, giờ hệ thống thật, mạng ngoài hay dữ liệu dùng chung. Thời gian đi qua `Clock` có thể thay thế; sinh ID/token đi qua thành phần có thể thay thế khi cần xác định.
6. `[CONFIRMED]` Không dùng H2 thay PostgreSQL cho test tích hợp: dùng PostgreSQL thật qua Testcontainers (cùng phiên bản với production).

---

## 2. Công cụ

| Phía | Công cụ | Ghi chú |
|---|---|---|
| Backend đơn vị | JUnit 5, AssertJ, Mockito (chỉ cho biên ngoài) | Domain test không cần Spring |
| Backend tích hợp | Spring Boot Test, Testcontainers (PostgreSQL), MockMvc hoặc `WebTestClient`, GreenMail hoặc stub cho email | Flyway chạy thật |
| Kiến trúc | ArchUnit | Luật A1–A12 |
| Mobile | `flutter_test`, Drift với DB in-memory (`NativeDatabase.memory()`), `mocktail` | Riverpod test qua `ProviderContainer` |
| Hợp đồng | Fixture JSON dùng chung (§10) | Cả hai phía đọc cùng tệp |
| Tải cơ bản | k6 `[CONFIRMED]` (Q-T1) | §11 |

`[CONFIRMED]` Không mock repository trong test của Application Service; dùng DB thật (Testcontainers) để bắt lỗi SQL, ràng buộc và transaction. Mock chỉ dùng cho thứ bên ngoài tiến trình (gửi email, push).

---

## 3. Kim tự tháp và vị trí đặt test

| Tầng | Mục tiêu | Tốc độ | Đặt ở |
|---|---|---|---|
| Đơn vị (domain thuần) | Quy tắc nghiệp vụ, validation, mapper | mili-giây | `src/test/java/.../<module>/domain` |
| Tích hợp module | Application Service + repository + DB thật | giây | `.../<module>/application`, `.../infrastructure` |
| Web/API | Controller, bảo mật, định dạng lỗi, JSON | giây | `.../<module>/web` |
| Kiến trúc | Ranh giới module, layering | giây | `src/test/java/.../architecture` |
| Liên module/E2E backend | Luồng sync nhiều bước, outbox, đăng nhập đến push | giây–chục giây | `src/test/java/.../e2e` |
| Mobile đơn vị/widget | Repository local, SyncEngine, controller Riverpod | mili-giây | `test/` |
| Hợp đồng | Fixture JSON hai phía | mili-giây | `docs/api/fixtures/` (§10) |

Quy ước đặt tên: lớp `XxxTest` (đơn vị) và `XxxIT` (tích hợp, chạy ở giai đoạn riêng). Tên phương thức mô tả hành vi: `rejectsPushWhenVersionMismatch`.

---

## 4. Quy tắc viết test

1. Cấu trúc Arrange–Act–Assert; mỗi test một lý do thất bại.
2. Dữ liệu dựng bằng builder/fixture rõ ràng; không dùng dữ liệu seed ẩn.
3. `[CONFIRMED]` Mỗi test tích hợp tạo user/dữ liệu riêng (UUID mới) thay vì dọn bảng toàn cục, để chạy song song được.
4. Không `Thread.sleep`; dùng Awaitility (đã có trong `spring-boot-starter-test`, không thêm phụ thuộc) có timeout cho phần bất đồng bộ (outbox).
5. Không gọi mạng ngoài; email/push dùng stub ghi lại lời gọi.
6. Không in token, mật khẩu thật vào log test; dùng giá trị giả rõ ràng.
7. Assert phải kiểm tra **trạng thái cuối** (DB, phản hồi) chứ không chỉ "không ném lỗi".
8. Test phi xác định (flaky) bị coi là lỗi; sửa hoặc xóa kèm lý do, không thử lại tự động để che.

---

## 5. Backend: phạm vi theo lớp

### 5.1. Domain
- Quy tắc Task: `dueTime` chỉ khi có `dueDate`; `title` 1–200; `description` ≤ 10.000; chuyển `OPEN↔DONE` đặt/xóa `completedAt`.
- Category: tên duy nhất không phân biệt hoa thường theo user, bỏ qua bản đã xóa.
- Project: lưu trữ/bỏ lưu trữ; tiến độ `0/0` → `completionRatio = 0`.
- Mapper (MapStruct): mọi mapping được phủ; `unmappedTargetPolicy=ERROR` làm biên dịch thất bại nếu thiếu.

### 5.2. Application Service
- Mỗi use case: đường thành công, từng đường lỗi nghiệp vụ, kiểm tra `userId`.
- Ranh giới transaction: lỗi giữa chừng không để lại dữ liệu nửa vời (rollback).
- Mọi thao tác ghi gọi `ChangeRecorder` đúng một lần với đúng `entityType`, `operation`, `version` (SYNC-INV).

### 5.3. Web
- Phản hồi đúng mã trạng thái và `errorCode` theo `API_CONVENTIONS.md` §4–§5.
- Định dạng RFC 9457; không lộ stack trace; luôn có `correlationId`.
- Trường lạ, JSON sai, body quá lớn.
- Mỗi endpoint 🔒: thiếu token → 401.

### 5.4. Migration DB
Xem §8.

---

## 6. Bộ test đồng bộ bắt buộc `[CONFIRMED]` D65

Chạy trên PostgreSQL thật. Mỗi mục là một test (tối thiểu) tên bắt đầu `Sync_`.

| ID | Kịch bản | Kỳ vọng |
|---|---|---|
| SYN-01 | Push tạo mới một Task | `APPLIED`, version = 1, có 1 `change_logs`, `seq` tăng |
| SYN-02 | Gửi lại cùng `mutationId` | `DUPLICATE`, trả cùng kết quả đã lưu, không thêm `change_logs` |
| SYN-03 | Push update với `baseVersion` đúng | `APPLIED`, `version` +1 |
| SYN-04 | Push update với `baseVersion` cũ | `CONFLICT`, dữ liệu server không đổi, trả bản server hiện tại |
| SYN-05 | Push delete → pull | Bản ghi tombstone (`deleted_at`) xuất hiện trong pull |
| SYN-06 | Lô có mutation thứ 2 lỗi | Mutation 1 vẫn `APPLIED`, mutation 2 `REJECTED`, mutation 3 vẫn xử lý (mỗi mutation một transaction) |
| SYN-07 | Mutation `REJECTED` rồi gửi lại cùng `mutationId` | Kết quả `REJECTED` được lưu (transaction riêng), không xử lý lại |
| SYN-08 | Lô > 50 mutation | 400 `SYNC_BATCH_TOO_LARGE`, không mutation nào áp dụng |
| SYN-09 | `entityType` lạ | Mutation `REJECTED` với `SYNC_UNKNOWN_ENTITY_TYPE` |
| SYN-10 | Pull phân trang qua nhiều trang | Không sót, không lặp; `nextCursor` đơn điệu theo `seq` |
| SYN-11 | Hai thiết bị cùng user push đồng thời | `seq` không trùng, không thủng thứ tự quan sát được; không deadlock |
| SYN-12 | Hai user push đồng thời | Không chặn lẫn nhau (khóa theo user) |
| SYN-13 | Xóa Project có Task | Server cascade: Task chuyển về Inbox, mỗi Task có `change_logs` riêng, version tăng |
| SYN-21 | Task có mutation UPDATE chờ gửi (`baseVersion` cũ) bị ảnh hưởng bởi cascade xóa Project/Category từ thiết bị khác | Mutation nhận `CONFLICT`, dữ liệu server không bị ghi đè; client giữ bản local (MSY-04) |
| SYN-14 | Xóa Category có Task | Task về "không danh mục", tương tự SYN-13 |
| SYN-15 | Push Task tham chiếu `projectId` đã xóa/không tồn tại/của user khác | `REJECTED` (lý do nghiệp vụ), không rò thông tin |
| SYN-16 | Pull sau khi push của chính thiết bị đó | Thay đổi của chính mình có trong pull nhưng client bỏ qua an toàn (idempotent) |
| SYN-17 | Không lấy được khóa user trong `lock_timeout` | 503 + `Retry-After`; gửi lại cùng `mutationId` thành công |
| SYN-18 | Client gửi `version`, `completedAt`, `updatedAt` tự đặt | Bị bỏ qua; server đặt giá trị |
| SYN-19 | Payload > 64 KB | Mutation `REJECTED`/413 theo `SECURITY_RULES.md` §9 |
| SYN-20 | Dữ liệu user A không bao giờ xuất hiện trong pull của user B | Kiểm bằng dữ liệu của hai user xen kẽ |

Bộ test bổ sung **SYN-22..48** nằm ở `SYNC_PROTOCOL.md` §9 `[CONFIRMED]` D92 và bắt buộc ở Giai đoạn 3.

Test đồng thời (SYN-11/12/17 và SYN-32/44/45) dùng nhiều luồng và `CountDownLatch`; có timeout cứng và chạy ổn định lặp ≥ 20 lần trong CI nightly `[CONFIRMED]`.

### 6.1. Mobile — SyncEngine (Drift in-memory)

| ID | Kịch bản |
|---|---|
| MSY-01 | Ghi local sinh bản ghi outbox cùng transaction với thay đổi dữ liệu |
| MSY-02 | Mất mạng giữa chừng: outbox giữ nguyên, thử lại không nhân đôi (cùng `mutationId`) |
| MSY-03 | `APPLIED` → cập nhật `version`, `sync_state=synced`, xóa khỏi outbox |
| MSY-04 | `CONFLICT` → `sync_state=conflict`, giữ dữ liệu local, không ghi đè |
| MSY-05 | `REJECTED` → đánh dấu lỗi, không thử lại vô hạn |
| MSY-06 | Pull áp dụng upsert theo `version`; bản cũ hơn bị bỏ qua |
| MSY-07 | Pull tombstone → xóa/ẩn local, hủy lịch thông báo cục bộ |
| MSY-08 | Con trỏ chỉ được lưu sau khi toàn bộ trang áp dụng thành công |
| MSY-09 | Refresh token xoay vòng: lưu token mới trước khi bỏ token cũ; 401 đồng thời chỉ kích hoạt một lần refresh |
| MSY-10 | `426` dừng sync nhưng không xóa dữ liệu local |
| MSY-11 | Đăng xuất xóa token nhưng hành vi với dữ liệu local theo quyết định sản phẩm (`FLUTTER_RULES.md`) |

---

## 7. Bộ test bảo mật bắt buộc

`SECURITY_RULES.md` §16.1 định nghĩa SEC-01..13; các test đó là **bắt buộc** và nằm trong CI. Bổ sung `[CONFIRMED]` D66:

| ID | Kịch bản |
|---|---|
| IDOR-01 | Với **mọi** endpoint nhận `{id}` (task/project/category/device): user B truy cập id của user A → 404, thân phản hồi giống hệt id không tồn tại |
| IDOR-02 | Mọi endpoint ghi: user B gửi `id` thuộc A → không thay đổi dữ liệu A, kết quả không phân biệt được với "không tồn tại" |
| IDOR-03 | Tạo với `id` đã tồn tại của user khác → 409 `ENTITY_ALREADY_EXISTS`, cùng phản hồi như id trùng của chính mình |
| IDOR-04 | Tham chiếu chéo (Task trỏ `projectId`/`categoryId` của user khác) bị từ chối |
| AUTHZ-01 | Mọi endpoint 🔒 không token / token sai / hết hạn → 401 (kiểm tra tự động qua danh sách route) |
| AUTHZ-02 | Chỉ các endpoint trong danh sách công khai mở không cần token (kiểm tra tự động; thêm route công khai mà không cập nhật danh sách làm test đỏ) |
| AUTHZ-04 | Hai request `/auth/refresh` đồng thời với cùng một refresh token (≥ 20 lần lặp) | Đúng một request cấp token mới; không bao giờ có hai token hợp lệ cùng family; request thua trả 401 và family bị thu hồi |
| AUTHZ-03 | Thiết bị bị thu hồi: refresh thất bại ngay; access token bị từ chối trong cửa sổ cho phép |
| ENUM-01 | `register`/`forgot-password`/`resend-verification` trả cùng mã và nội dung dù email có tồn tại hay không |
| ENUM-02 | Đăng nhập sai email và sai mật khẩu trả cùng lỗi (thời gian phản hồi gần nhau `[CONFIRMED]`: kiểm tra có chạy băm giả) |
| LOG-01 | Gọi các endpoint nhạy cảm rồi kiểm log: không chứa mật khẩu, access/refresh/verification/reset token |
| LOG-02 | Payload outbox không còn token thô sau khi gửi (scrub-after-send) |

Kiểm tra tự động "mọi endpoint" dựa trên quét bản đồ route của Spring (`RequestMappingHandlerMapping`) để thêm endpoint mới tự động nằm trong phạm vi test, không phụ thuộc người nhớ thêm.

---

## 8. Test cơ sở dữ liệu và migration `[CONFIRMED]` D67

1. Flyway áp dụng V1..Vn lên PostgreSQL trống thành công; kiểm `flyway validate`.
2. Sau migration, schema thật khớp ràng buộc quan trọng: khóa chính/ngoại, `UNIQUE` (email chuẩn hóa, tên Category theo user không phân biệt hoa thường với điều kiện chưa xóa), `CHECK` (độ dài, enum), chỉ mục cần cho pull (`user_id, seq`).
3. Quy tắc cấm sửa migration đã áp dụng: CI so checksum tệp `V*.sql` với nhánh chính; sửa tệp cũ làm CI đỏ.
4. Test nâng cấp: từ schema V(n-1) có dữ liệu mẫu lên V(n) không mất dữ liệu (bắt đầu từ khi có V2 trở đi).
5. Test ràng buộc: vi phạm từng `CHECK`/`UNIQUE` quan trọng bị DB từ chối (không chỉ dựa vào validation ứng dụng).
6. Test `user_sync_state`: tạo lazy bằng upsert khi push đầu tiên, an toàn khi đồng thời.
7. Mobile: schema Drift có test migration (`drift_dev schema` + test kiểm tra từ phiên bản cũ), vì SQLite trên thiết bị người dùng không thể "làm lại".

---

## 9. Test ranh giới kiến trúc

- Toàn bộ luật ArchUnit A1–A12 chạy như một test thường trong CI; **không** được tắt/ngoại lệ hóa nếu chưa có quyết định được duyệt (`CODE_GUIDELINES.md` §16).
- Thêm quy tắc kiểm "controller không chứa `@Transactional`", "Entity không xuất hiện ở lớp web" nếu chưa thuộc A1–A12 `[CONFIRMED]`.
- Vi phạm không được sửa bằng cách thêm ngoại lệ; sửa bằng cách đưa qua `api/` hoặc interface trong `common`.

---

## 10. Hợp đồng giữa backend và Flutter `[CONFIRMED]` D68

- Thư mục `docs/api/fixtures/` chứa các cặp request/response JSON mẫu cho: login, refresh, tạo/sửa/xóa Task, push (APPLIED/DUPLICATE/CONFLICT/REJECTED), pull (có tombstone, phân trang), các lỗi chuẩn.
- Backend: test web serialize/deserialize **đúng** fixture (so sánh JSON có cấu trúc, không so chuỗi).
- Flutter: test parse cùng fixture ra DTO và serialize lại.
- Đổi hợp đồng = đổi fixture + OpenAPI trong cùng PR (kiểm bằng CI diff OpenAPI, `API_CONVENTIONS.md` §10).
- Fixture chỉ dùng dữ liệu giả; không dùng dữ liệu thật.

---

## 11. Tải cơ bản và hiệu năng `[CONFIRMED]` D69

Mục đích: phát hiện sớm lỗi khóa/đồng thời, không phải tối ưu sớm.

| Kịch bản | Điều kiện đạt (đề xuất, chỉnh khi có số đo thực tế) |
|---|---|
| Push 50 mutation/lô, 20 user song song | Không lỗi 5xx; p95 < 1 s trên máy CI chuẩn |
| Cùng một user, 5 thiết bị push đồng thời | Không deadlock; `seq` liên tục; 503 chỉ khi quá `lock_timeout` |
| Pull 10.000 thay đổi qua trang 500 | Hoàn tất, bộ nhớ ổn định |
| Outbox 1.000 sự kiện chờ | Được xử lý hết, `SKIP LOCKED` không xử lý trùng với 2 worker |

Chạy theo lịch (nightly) hoặc theo yêu cầu, không chặn mỗi PR. Ngưỡng cụ thể chốt sau khi có số đo đầu tiên (Q-T2, hoãn có chủ đích).

---

## 12. Chính sách phủ sóng và cổng CI `[CONFIRMED]` D70

### 12.1. Cổng bắt buộc cho mọi PR
1. Build + test đơn vị + test tích hợp (Testcontainers) xanh.
2. ArchUnit xanh.
3. Test bắt buộc §6–§9 xanh.
4. Phân tích tĩnh/định dạng theo `CODE_GUIDELINES.md` (công cụ chốt khi khởi tạo skeleton).
5. Quét phụ thuộc có lỗ hổng đã biết, mức nghiêm trọng cao chặn merge `[CONFIRMED]`.
6. Quét bí mật (secret scanning) không phát hiện khóa/mật khẩu trong repo.
7. Flutter: `flutter analyze` và `flutter test` xanh.
8. Hợp đồng: OpenAPI và fixture không có thay đổi phá vỡ chưa được duyệt.

### 12.2. Phủ sóng
- Không đặt ngưỡng phần trăm toàn cục cứng ở Phase 1. Thay vào đó:
  - Domain và Application Service: mục tiêu **≥ 80%** nhánh (báo cáo JaCoCo), vi phạm cần giải thích trong PR.
  - Vùng có test bắt buộc (§6–§9): **100% kịch bản trong bảng** phải tồn tại; đây là tiêu chí chính, không phải phần trăm dòng.
  - Mã sinh tự động (MapStruct, DTO thuần) loại khỏi tính toán.
- Phủ sóng không được dùng để biện minh test vô nghĩa; reviewer có quyền từ chối test chỉ để tăng số.

### 12.3. Lịch chạy
| Lịch | Nội dung |
|---|---|
| Mỗi PR | Cổng §12.1 |
| Hằng đêm | Lặp test đồng thời ≥ 20 lần, kịch bản tải §11, quét phụ thuộc đầy đủ |
| Trước phát hành | Toàn bộ trên, cộng thử thủ công checklist `PRD.md` tiêu chí chấp nhận Phase 1 |

---

## 13. Quy tắc cho Agent

1. Viết hoặc cập nhật test cùng code; không báo "xong" khi chưa chạy test liên quan (lệnh chạy ở bảng build/test trong `AGENTS.md`, hiện TBD cho đến khi có skeleton).
2. Không được: xóa/sửa test để pass, thêm `@Disabled`/`skip`, nới assert, mock thứ cần kiểm chứng, đổi ngưỡng cổng CI.
3. Khi test hiện có thất bại vì thay đổi hợp lý của hành vi: dừng, nêu rõ, xin duyệt trước khi sửa test.
4. Khi thấy tài liệu và test mâu thuẫn: dừng và hỏi (`[OPEN]`), không tự chọn bên nào.
5. Khi thêm endpoint/entity đồng bộ mới: thêm vào IDOR-01/02, AUTHZ-01, kịch bản SYN tương ứng và fixture trong cùng PR.

---

## 14. Câu hỏi mở

| # | Nội dung |
|---|---|
| Q-T1 | ~~Công cụ tải~~ → **k6** (đã chốt) |
| Q-T2 | Ngưỡng thực tế cho §11 (sau khi có số đo đầu tiên) |
| Q-T3 | ~~Test trên thiết bị thật~~ → Phase 1 chỉ unit/widget; `integration_test` trên thiết bị thật để sau (đã chốt) |
| Q-T4 | ~~Dữ liệu local khi đăng xuất~~ → giữ nguyên, không tự xóa (D26, đã chốt) |

---

## 15. Quyết định (đã duyệt 2026-11-05)

| # | Đề xuất | Mục |
|---|---|---|
| D64 | Tập trung kiểm thử vào sync, bảo mật/IDOR, migration, ranh giới module; còn lại theo mức hợp lý | §1 |
| D65 | Bộ test đồng bộ bắt buộc SYN-01..21 và MSY-01..11; test đồng thời lặp ≥ 20 lần nightly | §6 |
| D66 | Bộ test bảo mật bổ sung IDOR/AUTHZ/ENUM/LOG; kiểm tra "mọi endpoint" quét từ bản đồ route | §7 |
| D67 | Test migration: Flyway sạch, checksum bất biến, ràng buộc DB, nâng cấp có dữ liệu; mobile có test migration Drift | §8 |
| D68 | Fixture JSON dùng chung `docs/api/fixtures/` cho test hợp đồng hai phía | §10 |
| D69 | Kịch bản tải cơ bản chạy nightly/theo yêu cầu, ngưỡng chỉnh theo số đo | §11 |
| D70 | Cổng CI §12.1; phủ sóng: domain/app ≥ 80% nhánh nhưng tiêu chí chính là đủ kịch bản bắt buộc | §12 |

Đã duyệt toàn bộ ngày 2026-11-05; các mục `[OPEN]` còn lại đều có lịch chốt riêng.
