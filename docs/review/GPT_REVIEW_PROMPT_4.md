# Prompt review LifeOS (vòng 4: giao thức đồng bộ — Giai đoạn 3 backend)

Dán phần dưới dòng `---` vào GPT và **đính kèm đủ 12 file ở mục 1**. Đây là review **tài liệu thiết kế**, không review code. Tài liệu trọng tâm là `SYNC_PROTOCOL.md` (mới, các quyết định D81–D92 đang `[PROPOSED]`).

---

Bạn là kiến trúc sư hệ thống phân tán senior kiêm security reviewer. Hãy rà soát **đặc tả giao thức đồng bộ** của **LifeOS** (ứng dụng quản lý cuộc sống cá nhân, offline-first: Flutter + SQLite/Drift; backend Java Spring Boot Modular Monolith; PostgreSQL). Đặc tả này sẽ được một AI agent dùng để cài đặt `POST /sync/push` và `GET /sync/pull` ở backend. Câu hỏi chính: *"Nếu chỉ có những tài liệu này, agent có cài đặt đúng, an toàn và không gây mất/lệch/trùng dữ liệu hay deadlock không?"*

## 1. Tài liệu cần đọc (đọc hết trước khi nhận xét)

| # | File | Vai trò |
|---|---|---|
| 1 | `docs/engineering/SYNC_PROTOCOL.md` | **Trọng tâm**: ma trận kết quả, payload, phân loại lỗi, thứ tự khóa, pull, test SYN-22..48, D81–D92 |
| 2 | `docs/architecture/SYSTEM_ARCHITECTURE.md` | **Nguồn sự thật** của hợp đồng sync: §3.3–3.8, §4, §6 |
| 3 | `docs/engineering/SYNC_RULES.md` | Quy tắc đồng bộ nền |
| 4 | `docs/architecture/DATABASE_DESIGN.md` | DDL `sync_mutations`, `change_logs`, `user_sync_state`, `tasks`, `projects`, `categories`, `user_settings` |
| 5 | `docs/engineering/SECURITY_RULES.md` | Threat model, IDOR, giới hạn kích thước (§9), log (§11) |
| 6 | `docs/engineering/API_CONVENTIONS.md` | Catalog `errorCode`, 400/422/404/409, §8.7 sync, D78–D79 |
| 7 | `docs/engineering/TESTING_RULES.md` | SYN-01..21 và quy ước test đồng thời |
| 8 | `docs/engineering/JAVA_SPRING_RULES.md` | Phân tầng, transaction, ranh giới module |
| 9 | `docs/engineering/CODE_GUIDELINES.md` | Nhãn `[CONFIRMED]/[PROPOSED]/[OPEN]`, decision log |
| 10 | `docs/engineering/FLUTTER_RULES.md` | Phía client của sync (outbox, conflict) |
| 11 | `docs/product/PRD.md` và `USE_CASES.md` | FR-SY-*, UC-SY-*, UC-PJ-04 |
| 12 | `docs/review/GPT_REVIEW_PROMPT_4.md` | Chính prompt này |

Không cần đọc thư mục `backend/`. Nếu không đọc được file nào, **dừng và nêu tên file** trước khi review; không suy đoán nội dung file thiếu.

## 2. Lịch sử review (đừng lặp lại)

Đã có 3 vòng review ngoài. Các điểm sau **đã được xử lý hoặc bác kèm lý do**; chỉ phản biện nếu có lý lẽ mới và **trích nguyên văn dòng tài liệu** làm căn cứ:

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
- **Quyết định đã chốt của vòng này, chỉ phản biện nếu thấy lỗi thật:** `DUPLICATE` phát lại nguyên văn kết quả đã lưu, không đọc lại entity (D83); `DELETE` tombstone → `APPLIED` còn REST → 404 (D84, vì sync có `mutationId` và client phải thoát khỏi hàng đợi); lỗi không dự kiến → 500 không lưu kết quả, không biến thành `REJECTED` (D85); `CREATE` id đã dùng → `ENTITY_ALREADY_EXISTS` giống hệt cho mọi trường hợp (D36/D81).
- **Cố ý hoãn, đừng báo là thiếu:** Flutter SyncEngine và UI conflict (làm riêng), cleanup job, full resync, retention tombstone/`change_logs`/`sync_mutations` (trước Phase 5), mã hóa SQLite at rest, certificate pinning, bảng audit, mã hóa PostgreSQL at rest, ngưỡng test tải, `minSupportedClientVersion`, tần suất backup.

## 3. Nhiệm vụ

Tìm lỗi **thật**, không khen chung chung. Mỗi phát hiện phải có:

1. **Mức độ:** Blocker (sẽ gây mất/lệch dữ liệu, lỗ hổng, hoặc khiến agent làm sai) · Major · Minor · Nit.
2. **Vị trí:** file + mục.
3. **Trích nguyên văn** câu/dòng gây vấn đề (bắt buộc). Không trích được thì không báo.
4. **Vấn đề** và **kịch bản cụ thể** (dữ liệu hoặc chuỗi sự kiện, không nói "có thể").
5. **Đề xuất sửa** ngắn gọn.
6. **Độ chắc chắn:** Chắc chắn / Có thể / Cần thêm thông tin.

Chỉ nêu điều bạn đã **đối chiếu với tài liệu**. Điều tài liệu đã xử lý ở chỗ khác thì không báo.

## 4. Trọng tâm (theo thứ tự quan trọng)

**A. Đúng đắn của ma trận kết quả (`SYNC_PROTOCOL.md` §2–§4)**
- Với mọi tổ hợp (CREATE/UPDATE/DELETE × không tồn tại / của user khác / còn sống / tombstone × `baseVersion` khớp/lệch), kết quả có xác định, không mâu thuẫn, không rò thông tin tồn tại (IDOR) và không làm client kẹt hàng đợi mãi?
- Ngữ nghĩa payload "khóa vắng mặt = giữ nguyên, `null` = xóa" (D82) có tạo ra nhập nhằng hoặc cách mất dữ liệu nào không? Khác gì so với "state đầy đủ" ở `SYSTEM_ARCHITECTURE.md` §4.1 và có mâu thuẫn không?
- Thao tác không đổi gì giữ nguyên version (§4.4): có tạo khoảng hở nào giữa các thiết bị (ví dụ thiết bị B không bao giờ biết thiết bị A đã gửi cùng giá trị)?
- `entityId` của `user_settings` là `user_settings.id` (D88): client có cách đáng tin cậy để biết id này khi cài mới không?

**B. Idempotency và đồng thời (§2.2, §5)**
- Kịch bản: hai request song song cùng `mutationId`; cùng `mutationId` trong một lô; `CONFLICT`/`REJECTED` ghi ở T2 trong khi T1 đã rollback; crash giữa T1 commit và phản hồi; lô bị cắt bởi 503 rồi gửi lại cả lô. Có kịch bản nào dẫn tới áp dụng hai lần, mất kết quả, hoặc kết quả `DUPLICATE` sai?
- `mutationId` tái dùng với metadata khác vẫn `DUPLICATE` (D83): có lỗ hổng bảo mật hoặc lỗi dữ liệu không?
- Phân loại lỗi (D85): 503 cho cả request khi một mutation hết khóa, 500 khi lỗi lạ. Có kịch bản "poison mutation" làm kẹt hàng đợi vô hạn không, và tài liệu có chỉ ra cách thoát?

**C. Khóa và deadlock (§5.2, D86, D87) — phản biện kỹ nhất**
- Bất biến "khóa và flush dữ liệu nghiệp vụ trước, `ChangeRecorder.record` (khóa `user_sync_state`) cuối cùng" có thật sự loại trừ vòng chờ không? Tìm phản ví dụ cụ thể: cascade xóa Project/Category, tạo/sửa Task tham chiếu `projectId`/`categoryId` (`FOR SHARE`), FK check của PostgreSQL (khóa `FOR KEY SHARE` ngầm trên dòng cha), hai thiết bị cùng user, REST xen với sync.
- Auto-flush của Hibernate, `PESSIMISTIC_*`, và `JdbcClient` dùng chung kết nối: bất biến có thể bị phá một cách âm thầm ở đâu? Tài liệu có test/kiểm tra nào phát hiện không (SYN-44, SYN-48)?
- `lock_timeout` đặt ở mức kết nối JDBC (D87): có tác dụng phụ nào (Flyway, job nền, các câu ghi không liên quan sync, pool kết nối, `SET LOCAL`)?
- Mã SQLSTATE nào thực sự cần ánh xạ sang 503 (`55P03`, `40P01`, còn gì khác như `40001`)? Có thiếu không?

**D. Pull (§6)**
- Tính đúng: đọc `change_logs` rồi đọc snapshot ở READ COMMITTED (§6 mục 6): có kịch bản client bỏ sót hoặc áp dụng sai thứ tự không? Cursor "không vượt dòng đã đọc" có đủ không? Cursor lớn hơn `last_seq` trả rỗng (D89) có che giấu lỗi (ví dụ khôi phục DB từ backup) làm client mất đồng bộ im lặng?
- Gom theo entity giữ dòng cuối: có trường hợp thứ tự cascade (Task UPDATE trước Project DELETE) bị phá khi gom?
- `data` của snapshot khớp `Response` REST và bất đối xứng `archived`/`archivedAt`: có chỗ nào client sẽ hiểu sai?

**E. Bảo mật và giới hạn (§2.1, §7, `SECURITY_RULES.md` §9, §11)**
- IDOR qua `entityId`, qua thứ tự/độ dài phản hồi, qua `DUPLICATE`, qua thời gian xử lý. Rò dữ liệu người dùng qua log/metric. Giới hạn 1 MB body so với 50 × 64 KB: có kịch bản client không thể đẩy được một mutation hợp lệ?
- Tấn công từ lô hợp lệ: 50 mutation tốn kém (Argon2 không liên quan, nhưng cascade lớn, payload JSON lồng sâu, chuỗi dài), DoS qua khóa `user_sync_state`.

**F. Test (§9) và tính kiểm chứng**
- SYN-01..48: thiếu kịch bản nguy hiểm nào? Test nào không thể hiện thực hóa như mô tả (đặc biệt SYN-44/45/48)? Có quy tắc `MUST` nào quan trọng mà chưa có test?

**G. Nhất quán giữa tài liệu**
- Mâu thuẫn giữa `SYNC_PROTOCOL.md` và `SYSTEM_ARCHITECTURE.md` §4, `SYNC_RULES.md`, `API_CONVENTIONS.md`, `TESTING_RULES.md`, `DATABASE_DESIGN.md` (tên trường, độ dài, enum, mã lỗi, giá trị mặc định, số lượng). ID tham chiếu (D81–D92, SYN-22..48, SEC-*, A1–A12) có tồn tại và đúng không?

## 5. Định dạng trả lời

1. **Tóm tắt 5 dòng:** đánh giá chung và 3 rủi ro lớn nhất.
2. **Bảng phát hiện** sắp theo mức độ (cột: #, Mức độ, Vị trí, Trích nguyên văn, Vấn đề + kịch bản, Đề xuất, Độ chắc chắn).
3. **Mâu thuẫn giữa tài liệu** (liệt kê riêng, mỗi mục nêu cả hai vị trí).
4. **Câu hỏi cho tác giả** do thiếu thông tin.
5. **Những gì thiết kế làm tốt và không nên đổi** (tối đa 5 dòng).

Không viết lại tài liệu, không đề xuất công nghệ mới ngoài stack (Flutter, Drift, Spring Boot, PostgreSQL, Flyway, Riverpod) trừ khi có lý do bảo mật/nhất quán cụ thể. Không đánh giá code.
