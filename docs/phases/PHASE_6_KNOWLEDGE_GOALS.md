# LifeOS — Phase 6: Knowledge & Goals (đặc tả)

> **Trạng thái:** v1 — **ĐÃ DUYỆT 2026-11-07** (người dùng duyệt `BACKEND_MASTER_PLAN.md`: toàn bộ quyết định D180–D189, quyết định mặc định ở §4 và thay đổi hợp đồng ở §5 của kế hoạch đó). Các mục `[OPEN]` trong tài liệu này được giải quyết bằng bảng §4 của `BACKEND_MASTER_PLAN.md`; khi mâu thuẫn, kế hoạch tổng thể thắng.
> **Nguồn đã chốt:** `PRODUCT_CONTEXT.md` §12 Phase 6; `DATABASE_RULES.md` §8; `SYNC_PROTOCOL.md`; `SECURITY_RULES.md`; `API_CONVENTIONS.md`.
> **Phụ thuộc:** backend giai đoạn 1–4; Phase 5 khuyến nghị (hợp nhất 3 chiều cho văn bản). Task (Phase 1) bắt buộc; Habit (Phase 2) tùy chọn cho liên kết Goal.
> **Ngoài phạm vi:** chỉnh sửa cộng tác, chia sẻ ghi chú, tệp đính kèm/ảnh, soạn thảo rich-text/markdown nâng cao (xem D182), AI tóm tắt/gợi ý (Phase 8), liên kết hai chiều kiểu wiki.

---

## 1. Mục tiêu và phạm vi

Thêm tầng **tri thức và mục tiêu**: ghi chú nhanh có thẻ (tag) và tìm kiếm toàn văn, Goal dài hạn chia thành Milestone, và liên kết Task/Habit vào Goal để thấy tiến độ thật. Ưu tiên: **bắt ý tưởng tức thì offline**, tìm thấy lại nhanh, tiến độ Goal **tính từ dữ liệu thật** (không nhập tay dễ sai). Ghi chú là dữ liệu riêng tư: tìm kiếm không lộ ra log/metric.

## 2. Yêu cầu chức năng

| ID | Yêu cầu | Tiêu chí chấp nhận |
|---|---|---|
| FR-NOTE-01 | CRUD ghi chú | Tiêu đề tùy chọn ≤ 200, nội dung văn bản ≤ 50 000 ký tự; ghim; lưu trữ |
| FR-NOTE-02 | Quick capture | Một thao tác tạo ghi chú rỗng + focus; lưu tự động (debounce 1 s) ở local |
| FR-NOTE-03 | Tag | Gắn nhiều tag (≤ 20/ghi chú); tag do người dùng tạo, tên duy nhất (không phân biệt hoa thường) |
| FR-NOTE-04 | Tìm kiếm | Toàn văn trên tiêu đề + nội dung + tên tag; offline (SQLite FTS5) và online (PostgreSQL FTS) cho cùng một truy vấn cơ bản |
| FR-NOTE-05 | Lọc và sắp xếp | Theo tag (AND/OR), ghim/lưu trữ, thời gian sửa; phân trang |
| FR-NOTE-06 | Liên kết ghi chú ↔ Task/Goal | Gắn ghi chú vào Task hoặc Goal (một chiều từ ghi chú); xem ghi chú liên quan từ Task/Goal |
| FR-GOAL-01 | CRUD Goal | Tiêu đề 1–200, mô tả ≤ 2000, khoảng thời gian (`startOn`, `targetOn`), trạng thái |
| FR-GOAL-02 | Milestone | Mỗi Goal ≤ 20 milestone có thứ tự, hạn tùy chọn, hoàn thành thủ công |
| FR-GOAL-03 | Liên kết Task | Gắn Task (và Habit nếu có) vào Goal hoặc Milestone; một Task thuộc tối đa 1 milestone (và qua đó 1 goal) |
| FR-GOAL-04 | Tiến độ tự động | % tiến độ tính từ Task đã hoàn thành / tổng + milestone hoàn thành (§4); hiển thị cả "con số thật" và cách tính |
| FR-GOAL-05 | Vòng đời | `ACTIVE`, `PAUSED`, `ACHIEVED`, `ABANDONED`; chuyển trạng thái có ghi nhận thời điểm |
| FR-GOAL-06 | Nhắc Goal | Reminder gắn Goal/Milestone (dùng Reminder Phase 4) |
| FR-GOAL-07 | Offline | Mọi chức năng trên chạy offline; tiến độ tính local |

## 3. Use case chính

| ID | Use case | Luồng và ngoại lệ |
|---|---|---|
| UC-NOTE-01 | Ghi nhanh ý tưởng | Mở app → ghi → tự lưu local → đồng bộ nền. Ngoại lệ: ghi chú > 50 000 ký tự → chặn ở UI |
| UC-NOTE-02 | Sửa cùng ghi chú trên hai thiết bị | `CONFLICT`; hợp nhất 3 chiều theo trường (tiêu đề/nội dung) và **xung đột văn bản hiển thị hai bản cạnh nhau**; không hợp nhất theo dòng tự động (D183) |
| UC-NOTE-03 | Tìm "hóa đơn điện" | FTS trả kết quả xếp hạng, đoạn trích có đánh dấu từ khớp |
| UC-NOTE-04 | Xóa tag | Gỡ khỏi mọi ghi chú (cascade server); xác nhận hiện số ghi chú bị ảnh hưởng |
| UC-GOAL-01 | Tạo Goal "Chạy 10 km" với 3 milestone và gắn Task | Tiến độ cập nhật khi Task hoàn thành (local ngay) |
| UC-GOAL-02 | Xóa Goal | Milestone bị xóa theo; Task **không bị xóa**, chỉ gỡ liên kết (D186) |
| UC-GOAL-03 | Task đã xóa còn trong Goal | Bị loại khỏi tiến độ (tombstone không tính) |
| UC-GOAL-04 | Hoàn thành mọi milestone | Gợi ý đánh dấu `ACHIEVED`; **không tự đổi** trạng thái (người dùng xác nhận) |

## 4. Quy tắc nghiệp vụ

1. `[CONFIRMED]` **D180 — Ghi chú là văn bản thuần:** `content` là text thuần (UTF-8, chuẩn hóa NFC khi lưu), xuống dòng `\n`. Ở phase này **không** có định dạng rich-text; hiển thị markdown nhẹ ở client (đậm/nghiêng/danh sách) là **tùy chọn hiển thị**, không phải dữ liệu có cấu trúc. Lý do: đồng bộ, tìm kiếm, xung đột đơn giản và đáng tin cậy.
2. `[CONFIRMED]` **D181 — Giới hạn kích thước:** nội dung ≤ 50 000 ký tự (tính theo code point), payload sync ≤ 64 KB (đảm bảo: 50 000 ký tự tiếng Việt có thể ≈ 150 KB UTF-8 → **nâng giới hạn payload riêng cho `note` lên 192 KB**, trong khi body tối đa 1 MB và 50 mutation/yêu cầu giữ nguyên; mutation `note` đếm 1 vào giới hạn 50 và tổng body ≤ 1 MB). Cần duyệt vì thay đổi `SYNC_PROTOCOL.md` §8 (giới hạn per-type).
3. `[CONFIRMED]` **D182 — Tìm kiếm PostgreSQL:** cột `search_vector tsvector` sinh tự động (`GENERATED ALWAYS AS`) từ `setweight(title,'A') || setweight(content,'B')` với cấu hình `simple` (không stemming — an toàn cho tiếng Việt đa ngôn ngữ) + `unaccent` để tìm không dấu; chỉ mục GIN. Tìm tên tag qua join (không nhúng vào vector). `GET /notes?q=` dùng `websearch_to_tsquery('simple', unaccent(:q))`; `q` tối đa 100 ký tự; kết quả xếp theo `ts_rank_cd` rồi `updated_at desc, id desc`; con trỏ phân trang gồm `(rank, updated_at, id)` (hoặc offset giới hạn 1000 cho tìm kiếm — xem `[OPEN]` 2). Extension `unaccent` cần được duyệt (ghi vào `DATABASE_RULES.md` khi duyệt).
4. `[CONFIRMED]` **D183 — SQLite FTS5 (client):** bảng ảo `notes_fts(title, content, tags)` dùng tokenizer `unicode61 remove_diacritics 2`; đồng bộ chỉ mục qua trigger hoặc ở tầng repository trong cùng transaction với ghi chú; xây lại từ `notes` khi phát hiện lệch (cùng đường với toàn vẹn local D164). **Không** cam kết kết quả/xếp hạng giống hệt server; cam kết: tập kết quả cho truy vấn một-hai từ khóa đơn giản **chứa** các ghi chú có đủ các từ (kiểm thử đối chiếu `[OPEN]` 3).
5. `[CONFIRMED]` **D184 — Tag:** `tags(id, user_id, name, color?)`; tên 1–30 ký tự, không chứa khoảng trắng đầu/cuối, `UNIQUE (user_id, lower(name)) WHERE deleted_at IS NULL`; liên kết nhiều-nhiều qua `note_tags(note_id, tag_id)` — **entity đồng bộ riêng** (CREATE/DELETE, không UPDATE) để tránh ghi đè danh sách tag của ghi chú khi hai thiết bị cùng sửa (hợp nhất ở mức liên kết: thêm của máy A và thêm của máy B đều giữ lại). `note_tags` có `UNIQUE (note_id, tag_id) WHERE deleted_at IS NULL`; tối đa 20 tag/ghi chú.
6. `[CONFIRMED]` **D185 — Goal và tiến độ:** tiến độ Goal **tính, không lưu**: nếu Goal có milestone: `progress = mean(progress(milestone))`, với `progress(milestone) = 1.0` nếu milestone `completed`, ngược lại `done_tasks/total_tasks` của Task liên kết (Task chưa xóa; `total=0` ⇒ 0); nếu Goal không có milestone nhưng có Task liên kết trực tiếp: `done/total`; không có gì ⇒ `null` (UI hiển thị "chưa có tiến độ" — không phải 0%). "Hoàn thành" của Task = trạng thái hoàn thành của mô hình Task Phase 1 (`DONE`). Habit đóng góp **không** vào % ở phase này (chỉ hiển thị chuỗi/streak) `[OPEN]` 4.
7. `[CONFIRMED]` **D186 — Liên kết Goal ↔ Task:** cột `milestone_id` và `goal_id` (nullable) trên **`tasks`** (Task thuộc tối đa một milestone; nếu có `milestone_id` thì `goal_id` bằng goal của milestone — do server đặt/kiểm). Đây là **thay đổi entity `task`** (hợp đồng sync `task` thêm khóa `goalId`, `milestoneId`) cần duyệt riêng. Xóa Goal/Milestone → server gỡ liên kết (`goal_id`/`milestone_id` = NULL, tăng `version` Task, `mutation_id` NULL, `ChangeRecorder.record` UPDATE) — **cascade theo D80** (sự kiện `GoalDeletionStarted`), Task không bị xóa.
8. `[CONFIRMED]` **D187 — Milestone:** `milestones(goal_id, title, due_on, position, completed_at)`; `position` là số nguyên thứ tự (đảo thứ tự bằng cách gửi toàn bộ danh sách vị trí qua `PATCH /goals/{id}/milestones/order` hoặc từng `UPDATE position` — **chọn `position` phân đoạn lớn (bước 1000)**, chèn giữa dùng trung điểm; chuẩn hóa lại khi hết khoảng — thao tác phía client, mỗi milestone là một mutation bình thường); ≤ 20 milestone/goal (`LIMIT_EXCEEDED`).
9. `[CONFIRMED]` **D188 — Trạng thái Goal:** `ACTIVE → {PAUSED, ACHIEVED, ABANDONED}`, `PAUSED → ACTIVE`, `ACHIEVED ↔ ACTIVE` (mở lại), `ABANDONED → ACTIVE`; ghi `status_changed_at` (server đặt). `ACHIEVED` không tự động; mọi milestone hoàn thành chỉ gợi ý. Goal `ACHIEVED/ABANDONED` giữ nguyên liên kết; Task vẫn bình thường.
10. `[CONFIRMED]` **D189 — Ghi chú liên kết:** `note_links(note_id, target_type ∈ {TASK, GOAL}, target_id)`; entity đồng bộ như `note_tags` (CREATE/DELETE); `UNIQUE (note_id, target_type, target_id) WHERE deleted_at IS NULL`; kiểm tồn tại và quyền sở hữu mục tiêu ở service (không FK đa hình); xóa Task/Goal → server xóa mềm liên kết (cascade, `mutation_id` NULL). Tối đa 20 liên kết/ghi chú.

Bổ sung (không đánh số riêng, theo mẫu chung): ghi chú đã lưu trữ ẩn khỏi danh sách mặc định nhưng vẫn tìm được khi bật "bao gồm lưu trữ"; ghim tối đa 10; xóa ghi chú là xóa mềm; **nội dung ghi chú không bao giờ vào log/metric/push/email** (Phase 4 D138); tìm kiếm không lưu lịch sử truy vấn ở server. *(Chốt 2026-11-15: "ghim tối đa 10" là **giới hạn mềm UX** — kiểm bằng đếm, không khóa, có thể vượt 1 khi ghim đồng thời. Offset tìm kiếm ghi chú ≤ 1000: đã duyệt.)*

## 5. Mô hình dữ liệu (`V9__knowledge_goals.sql`; cột chuẩn đồng bộ ở mọi bảng)

```sql
CREATE EXTENSION IF NOT EXISTS unaccent;   -- cần duyệt (D182)

CREATE TABLE notes (
  -- cột chuẩn đồng bộ
  title varchar(200), content text NOT NULL DEFAULT '' CHECK (char_length(content) <= 50000),
  pinned boolean NOT NULL DEFAULT false, archived_at timestamptz,
  search_vector tsvector GENERATED ALWAYS AS (
    setweight(to_tsvector('simple', unaccent(coalesce(title,''))), 'A') ||
    setweight(to_tsvector('simple', unaccent(content)), 'B')) STORED
);
-- GIN (search_vector) WHERE deleted_at IS NULL ; INDEX (user_id, updated_at DESC) WHERE deleted_at IS NULL AND archived_at IS NULL

CREATE TABLE tags (
  name varchar(30) NOT NULL CHECK (name = btrim(name) AND char_length(name) >= 1), color char(7)
);
-- UNIQUE (user_id, lower(name)) WHERE deleted_at IS NULL

CREATE TABLE note_tags (note_id uuid NOT NULL REFERENCES notes(id), tag_id uuid NOT NULL REFERENCES tags(id));
-- UNIQUE (note_id, tag_id) WHERE deleted_at IS NULL ; INDEX (user_id, tag_id) WHERE deleted_at IS NULL

CREATE TABLE note_links (
  note_id uuid NOT NULL REFERENCES notes(id),
  target_type varchar(4) NOT NULL CHECK (target_type IN ('TASK','GOAL')), target_id uuid NOT NULL
);
-- UNIQUE (note_id, target_type, target_id) WHERE deleted_at IS NULL ; INDEX (user_id, target_type, target_id) WHERE deleted_at IS NULL

CREATE TABLE goals (
  title varchar(200) NOT NULL, description varchar(2000),
  start_on date, target_on date,
  status varchar(9) NOT NULL DEFAULT 'ACTIVE' CHECK (status IN ('ACTIVE','PAUSED','ACHIEVED','ABANDONED')),
  status_changed_at timestamptz,
  CHECK (target_on IS NULL OR start_on IS NULL OR target_on >= start_on)
);
CREATE TABLE milestones (
  goal_id uuid NOT NULL REFERENCES goals(id), title varchar(200) NOT NULL,
  due_on date, position bigint NOT NULL, completed_at timestamptz
);
-- UNIQUE (goal_id, position) WHERE deleted_at IS NULL ; INDEX (goal_id) WHERE deleted_at IS NULL

ALTER TABLE tasks ADD COLUMN goal_id uuid REFERENCES goals(id), ADD COLUMN milestone_id uuid REFERENCES milestones(id);
-- INDEX (user_id, goal_id) WHERE deleted_at IS NULL AND goal_id IS NOT NULL
```

SQLite: bảng tương ứng + FTS5 `notes_fts`; cột Money/ thời gian theo quy ước hiện có.

## 6. API (`/api/v1`)

| Endpoint | Ghi chú |
|---|---|
| `POST/GET /notes`, `GET/PATCH/DELETE /notes/{id}`, `POST /notes/{id}/archive` `/unarchive` `/pin` `/unpin` | Lọc `tagId` (lặp lại = AND; `tagMode=any` = OR), `pinned`, `archived`, `q`; `GET` danh sách **không** trả `content` đầy đủ (chỉ `excerpt` 200 ký tự và `contentLength`), `GET /notes/{id}` trả đầy đủ |
| `POST/GET /tags`, `PATCH/DELETE /tags/{id}` | Xóa tag → gỡ khỏi ghi chú (D184, cascade) |
| `PUT /notes/{id}/tags` | Thay tập tag (≤ 20); server so sánh và tạo/xóa `note_tags` (tiện cho REST; sync dùng entity `note_tag`) |
| `POST/DELETE /notes/{id}/links` | D189 |
| `POST/GET /goals`, `GET/PATCH/DELETE /goals/{id}`, `POST /goals/{id}/status` | `GET` kèm `progress` tính (D185) và `milestones` |
| `POST /goals/{id}/milestones`, `PATCH/DELETE /milestones/{id}`, `POST /milestones/{id}/complete` `/reopen` | |
| `PATCH /tasks/{id}` | Thêm `goalId`/`milestoneId` (D186); kiểm khớp |
| `GET /goals/{id}/tasks`, `GET /goals/{id}/notes` | Danh sách liên quan, phân trang |

Mã lỗi mới `[CONFIRMED]`: `NOTE_NOT_FOUND`, `NOTE_TOO_LARGE` (422, `VALIDATION_FAILED` fieldError `content` mã `MAX_LENGTH` được ưu tiên — dùng `VALIDATION_FAILED`), `TAG_NOT_FOUND`, `TAG_NAME_TAKEN` (409), `NOTE_TAG_LIMIT_EXCEEDED` (422), `GOAL_NOT_FOUND`, `MILESTONE_NOT_FOUND`, `GOAL_INVALID_STATUS_TRANSITION` (422), `MILESTONE_LIMIT_EXCEEDED` (422), `GOAL_LINK_MISMATCH` (422, `milestoneId` không thuộc `goalId`).

## 7. Đồng bộ

`entityType` mới: `note`, `tag`, `note_tag`, `note_link`, `goal`, `milestone`; `task` thêm khóa `goalId`, `milestoneId`.
- `note`: xung đột theo trường; hai bản cùng sửa `content` → luôn hỏi, hiển thị cạnh nhau (D158 Phase 5, ngoại lệ văn bản); `tag` theo trường (`name`).
- `note_tag`, `note_link`: chỉ CREATE/DELETE; UPDATE → `REJECTED SYNC_OPERATION_NOT_ALLOWED`; CREATE trùng cặp đang hoạt động → `APPLIED` idempotent (không lỗi) nhưng trả về bản đã có.
- Cascade server (D80 mở rộng): xóa Note → `note_tags`, `note_links`; xóa Tag → `note_tags`; xóa Task → `note_links` (target); xóa Goal → milestone, `note_links`, gỡ `goal_id/milestone_id` của Task; xóa Milestone → gỡ `milestone_id` của Task. Thứ tự khóa theo D86; tất cả `mutation_id` NULL.
- Giới hạn per-type payload `note` ≤ 192 KB (D181, cần duyệt).

## 8. Bảo mật và riêng tư

- IDOR mọi route; `tagId`/`goalId`/`target_id` thuộc user, sai → 422 `*_NOT_FOUND` (D79).
- Truy vấn tìm kiếm tham số hóa; không nối chuỗi vào `tsquery`; giới hạn độ dài; tốc độ tìm kiếm 60/phút/user.
- Nội dung ghi chú, tiêu đề, tên tag **không** vào log, metric nhãn, lỗi, push, email; `SensitiveLogIT` mở rộng.
- Mã hóa nội dung ghi chú ở mức cột/E2EE: ngoài phạm vi (cùng quyết định at-rest chung `PRODUCT_CONTEXT` §19).
- Xuất ghi chú (Markdown/JSON): ở Phase 7 `[OPEN]` (xem README).

## 9. Kế hoạch kiểm thử

| ID | Kịch bản | Kỳ vọng |
|---|---|---|
| KNW-01 | Giới hạn nội dung (đủ/vượt 50 000 ký tự code point, ký tự bổ sung/emoji), NFC | Đúng, `VALIDATION_FAILED` |
| KNW-02 | Payload `note` lớn hợp lệ (≈ 150 KB UTF-8) qua sync; nhiều mutation tổng > 1 MB | OK / 413 đúng |
| KNW-03 | FTS: từ khóa đơn/nhiều từ, không dấu ("hoa don dien" tìm "hóa đơn điện"), ký tự đặc biệt/độ dài 100/101 | Đúng, không lỗi SQL, không injection `tsquery` |
| KNW-04 | Xếp hạng và phân trang ổn định (không sót/lặp) khi dữ liệu đổi giữa các trang | Đúng |
| KNW-05 | Đối chiếu FTS5 (client) và PG: bộ truy vấn chuẩn, tập kết quả đơn giản chứa nhau | Đúng D183 |
| KNW-06 | Tag: tên trùng không phân biệt hoa thường, giới hạn 20, xóa tag cascade | Đúng |
| KNW-07 | `note_tag` sync: hai thiết bị thêm hai tag khác nhau cho cùng ghi chú | Cả hai giữ lại |
| KNW-08 | Ghi chú sửa cùng lúc: CONFLICT nội dung hiển thị hai bản | Đúng |
| KNW-09 | Tiến độ Goal: có/không milestone, Task xóa/chưa xóa, `total=0` ⇒ null, làm tròn | Khớp mô hình tham chiếu (property-based) |
| KNW-10 | Liên kết Task↔Goal/Milestone: khớp `goalId`, xóa Goal/Milestone gỡ liên kết + tăng `version` Task | Đúng, không xóa Task |
| KNW-11 | Chuyển trạng thái Goal hợp lệ/không hợp lệ | Đúng D188 |
| KNW-12 | Milestone: ≤ 20, `position` unique, chèn giữa/chuẩn hóa lại | Đúng |
| KNW-13 | Cascade đồng thời: xóa Goal trong khi sửa Task (CascadeConcurrencyIT mở rộng, ≥ 20 lần) | Không deadlock, nhất quán, D86 |
| KNW-14 | `note_link`: mục tiêu của user khác/đã xóa; xóa Task cascade | Đúng D189 |
| KNW-15 | IDOR + mass assignment (`status_changed_at`, `search_vector`) | Đúng |
| KNW-16 | Log/lỗi/metric không chứa nội dung ghi chú | `SensitiveLogIT` |
| KNW-17 | `EXPLAIN`: GIN được dùng cho tìm kiếm với 20k ghi chú/user | Ghi kết quả |

## 10. Các bước triển khai và tiêu chí hoàn thành

| Bước | Nội dung |
|---|---|
| 6a | `V9` (notes, tags, note_tags), CRUD, tag |
| 6b | Tìm kiếm PG (unaccent, GIN) + FTS5 client + đối chiếu |
| 6c | Goal, Milestone, liên kết Task, tiến độ, cascade |
| 6d | `note_links`, sync 6 entity + `task` mở rộng, hardening, KNW-01..17 |

Hoàn thành khi: `mvn clean verify` + `flutter test` xanh, KNW đầy đủ, mở rộng `SYNC_PROTOCOL.md` đã được duyệt riêng (D181, D186), báo cáo theo mẫu chuẩn.

## 11. Quyết định và câu hỏi mở

D180–D189 như trên. `[OPEN]` (cần người dùng quyết trước khi code): (1) duyệt extension `unaccent` và cấu hình `simple` (khuyến nghị: duyệt); (2) phân trang tìm kiếm: con trỏ theo rank hay offset ≤ 1000 (khuyến nghị: offset giới hạn cho tìm kiếm, con trỏ cho danh sách thường); (3) mức cam kết tương đương FTS5/PG (khuyến nghị: như D183); (4) Habit có đóng góp vào % tiến độ Goal không (khuyến nghị: không, chỉ hiển thị); (5) duyệt thay đổi `task` (D186) và giới hạn payload `note` 192 KB (D181) — đổi `SYNC_PROTOCOL.md`; (6) giới hạn số ghi chú/user (khuyến nghị: không giới hạn, theo dõi Phase 7).
