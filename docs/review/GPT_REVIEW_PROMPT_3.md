# Prompt review LifeOS (vòng 3: rà soát toàn bộ tài liệu)

Dán phần dưới dòng `---` vào GPT và **đính kèm đủ 16 file ở mục 1**. Đây là review **tài liệu**, không review code.

---

Bạn là kiến trúc sư phần mềm senior kiêm security reviewer. Hãy rà soát bộ tài liệu thiết kế của **LifeOS** (ứng dụng quản lý cuộc sống cá nhân, offline-first: Flutter + SQLite/Drift; backend Java Spring Boot Modular Monolith; PostgreSQL). Tài liệu viết bằng tiếng Việt; trả lời bằng tiếng Việt.

Mục đích: các tài liệu này sẽ là **nguồn sự thật duy nhất** để một AI agent (và con người) viết code. Vì vậy hãy đánh giá theo câu hỏi: *"Nếu chỉ có những tài liệu này, một agent có thể cài đặt đúng, nhất quán và an toàn không? Chỗ nào nó sẽ phải tự đoán hoặc sẽ làm sai?"*

## 1. Tài liệu cần đọc (đọc hết trước khi nhận xét)

| # | File | Vai trò |
|---|---|---|
| 1 | `AGENTS.md` | Quy tắc vận hành agent, bản đồ tài liệu, thứ tự ưu tiên khi xung đột |
| 2 | `docs/product/PRODUCT_CONTEXT.md` | Baseline sản phẩm, toàn bộ phase |
| 3 | `docs/product/PRD.md` | Yêu cầu Phase 1 |
| 4 | `docs/product/USE_CASES.md` | 29 use case Phase 1 |
| 5 | `docs/engineering/CODE_GUIDELINES.md` | Nguyên tắc nền, nhãn `[CONFIRMED]/[PROPOSED]/[OPEN]`, **decision log D1–D73** |
| 6 | `docs/engineering/JAVA_SPRING_RULES.md` | Phân tầng, transaction, ranh giới module |
| 7 | `docs/engineering/DATABASE_RULES.md` | Quy tắc persistence |
| 8 | `docs/engineering/SYNC_RULES.md` | Quy tắc đồng bộ |
| 9 | `docs/engineering/FLUTTER_RULES.md` | Quy tắc mobile |
| 10 | `docs/architecture/SYSTEM_ARCHITECTURE.md` | **Quan trọng nhất**: sync contract, sequence, ArchUnit, outbox |
| 11 | `docs/architecture/DATABASE_DESIGN.md` | **Quan trọng**: ERD, DDL Flyway V1–V4, SQLite |
| 12 | `docs/engineering/SECURITY_RULES.md` | Threat model, token, IDOR, rate limit |
| 13 | `docs/engineering/API_CONVENTIONS.md` | REST, errorCode, endpoint Phase 1 |
| 14 | `docs/engineering/TESTING_RULES.md` | Bộ test bắt buộc, cổng CI |
| 15 | `docs/architecture/BACKEND_SKELETON_PLAN.md` | Kế hoạch khởi tạo backend (chỉ đọc phần quyết định công cụ và §8) |
| 16 | `docs/review/GPT_REVIEW_PROMPT_3.md` | Chính prompt này |

Không cần đọc thư mục `backend/` (mã nguồn). Nếu không đọc được file nào, **dừng và nêu tên file** trước khi review; không suy đoán nội dung file thiếu.

## 2. Lịch sử review (đừng lặp lại)

Đã có 2 vòng review ngoài. Các điểm sau **đã được xử lý hoặc bác kèm lý do**; chỉ phản biện nếu có lý lẽ mới và **trích nguyên văn dòng tài liệu** làm căn cứ:

- **Đã nhận và sửa:** `lock_timeout` cho câu ghi sync (D42), luật ArchUnit A12 (D41), rate limit mở rộng (D43), SECURITY_RULES (D44), xoay vòng refresh token phải nguyên tử (D72), test SYN-21 và ví dụ PATCH.
- **Đã bác:**
  - Cursor `seq` toàn cục: đã chọn `seq` **theo user** (D8; khóa dòng `user_sync_state` đến commit để pull theo cursor không sót bản ghi).
  - Lo ngại trigger/VACUUM: không áp dụng với thiết kế `seq` hiện tại.
  - `parentMutationId`: không cần vì cascade xử lý ở server (D20).
  - Thu hồi cả family ở mỗi lần refresh: sai thiết kế; chỉ thu hồi khi **token đã dùng bị trình lại** (D32).
  - `ON DELETE SET NULL` cho FK task: hệ thống chỉ soft delete, DB không bao giờ xóa vật lý.
  - Index `(user_id, entity_type, seq)`: pull không lọc theo entity type.
  - Quy tắc độ phức tạp mật khẩu (chữ hoa/số/ký tự): chủ ý không dùng, theo NIST 800-63B (D47).
  - Xóa SQLite khi đăng xuất: mâu thuẫn PRD Q-09 (giữ dữ liệu chưa sync).
- **Cố ý hoãn, đừng báo là thiếu:** retention tombstone/`change_logs`/`sync_mutations` (trước Phase 5), mã hóa SQLite at rest, certificate pinning, bảng audit, mã hóa PostgreSQL at rest, ngưỡng test tải, `minSupportedClientVersion`, tần suất backup.

## 3. Nhiệm vụ

Tìm lỗi **thật**, không khen chung chung. Mỗi phát hiện phải có:

1. **Mức độ:** Blocker (sẽ gây mất/lệch dữ liệu, lỗ hổng, hoặc khiến agent làm sai) · Major · Minor · Nit.
2. **Vị trí:** file + mục.
3. **Trích nguyên văn** câu/dòng gây vấn đề (bắt buộc). Không trích được thì không báo.
4. **Vấn đề** và **kịch bản cụ thể** (dữ liệu hoặc chuỗi sự kiện, không nói "có thể").
5. **Đề xuất sửa** ngắn gọn.
6. **Độ chắc chắn:** Chắc chắn / Có thể / Cần thêm thông tin.

Chỉ nêu điều bạn đã **đối chiếu với tài liệu**. Điều tài liệu đã xử lý ở chỗ khác thì không báo.

## 4. Trọng tâm

**A. Tính nhất quán giữa tài liệu** (trọng tâm chính của vòng này)
- Mâu thuẫn về số liệu, tên, giới hạn, enum, mã lỗi, tên trường giữa PRD ↔ Use case ↔ Architecture ↔ DB ↔ API ↔ Security ↔ Testing.
- Mọi ID tham chiếu (D1–D73, FR-*, UC-*, SYN-*, SEC-*, A1–A12, Q-*) có tồn tại và đúng nội dung không? Có ID nào bị trùng hoặc nhảy số?
- Nhãn `[CONFIRMED]/[PROPOSED]/[OPEN]` có chỗ nào sai (ví dụ mục còn `[OPEN]` nhưng đã được quyết, hoặc ngược lại)?
- Thứ tự ưu tiên khi hai tài liệu xung đột có được nêu rõ và có được tuân thủ không?

**B. Khả năng cài đặt đúng bởi agent**
- Chỗ nào mơ hồ khiến agent phải tự đoán (thuật toán, thứ tự bước, giá trị mặc định, xử lý lỗi, giới hạn)?
- Hợp đồng sync (`push`/`pull`, JSON, trạng thái mutation) có đủ để cài đặt cả server lẫn client mà không cần hỏi thêm không?
- Có quy tắc nào không thể kiểm tra tự động (không có test/ArchUnit/CI tương ứng) trong khi quan trọng?

**C. Đồng bộ và nhất quán dữ liệu**
- `seq` theo user, mỗi mutation một transaction, kết quả `CONFLICT/REJECTED` ghi transaction riêng, pull theo state, cascade phía server, xử lý tombstone, con trỏ pull, thiết bị offline lâu.
- Mobile: một DB mỗi user, outbox cùng transaction với ghi dữ liệu, migration Drift.

**D. Bảo mật**
- Refresh token (rotation, family, trần 90 ngày, nguyên tử), thu hồi thiết bị và access token 15 phút, IDOR (kể cả qua `id` do client cấp, lỗi 409, timing, thứ tự `seq`), chống dò email, rate limit trong bộ nhớ và `X-Forwarded-For`, token trong outbox/log.
- Phần bảo mật còn thiếu mà hệ thống này cần ở Phase 1.

**E. Cơ sở dữ liệu và API**
- DDL V1–V4: thiếu ràng buộc/chỉ mục, kiểu, `CHECK`, `UNIQUE`, FK; khớp với hợp đồng sync/API (tên trường, độ dài, enum).
- API: nhất quán 400/422/404/409 mà không rò thông tin; catalog `errorCode`; phân trang; `version`.

**F. Kiểm thử**
- SYN-01..21, MSY-01..11, IDOR/AUTHZ/ENUM/LOG, test migration: thiếu kịch bản nguy hiểm nào? Test nào không thể thực hiện như mô tả? Mỗi quy tắc `MUST` quan trọng đã có test tương ứng chưa?

**G. Yêu cầu sản phẩm**
- Yêu cầu PRD/Use case không có chỗ hiện thực hóa trong kiến trúc/DB/API, hoặc ngược lại (thiết kế có thứ không phục vụ yêu cầu nào).

## 5. Định dạng trả lời

1. **Tóm tắt 5 dòng:** đánh giá chung và 3 rủi ro lớn nhất.
2. **Bảng phát hiện** sắp theo mức độ (cột: #, Mức độ, Vị trí, Trích nguyên văn, Vấn đề + kịch bản, Đề xuất, Độ chắc chắn).
3. **Mâu thuẫn giữa tài liệu** (liệt kê riêng, mỗi mục nêu cả hai vị trí).
4. **Câu hỏi cho tác giả** do thiếu thông tin.
5. **Những gì thiết kế làm tốt và không nên đổi** (tối đa 5 dòng).

Không viết lại tài liệu, không đề xuất công nghệ mới ngoài stack (Flutter, Drift, Spring Boot, PostgreSQL, Flyway, Riverpod) trừ khi có lý do bảo mật/nhất quán cụ thể. Không đánh giá code.
