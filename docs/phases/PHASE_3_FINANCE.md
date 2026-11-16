# LifeOS — Phase 3: Personal Finance (đặc tả)

> **Trạng thái:** v1 — **ĐÃ DUYỆT 2026-11-07** (người dùng duyệt `BACKEND_MASTER_PLAN.md`: toàn bộ quyết định D100–D119, quyết định mặc định ở §4 và thay đổi hợp đồng ở §5 của kế hoạch đó). Các mục `[OPEN]` trong tài liệu này được giải quyết bằng bảng §4 của `BACKEND_MASTER_PLAN.md`; khi mâu thuẫn, kế hoạch tổng thể thắng.
> **Nguồn đã chốt (không viết lại):** `PRODUCT_CONTEXT.md` Module 06 và §8.2; `DATABASE_RULES.md` §8 (UUID, `NUMERIC` cho tiền, cột chuẩn, partial unique); `SYSTEM_ARCHITECTURE.md` §3.3–3.8, §4, §9; `SYNC_PROTOCOL.md`; `SECURITY_RULES.md`; `API_CONVENTIONS.md`; `TESTING_RULES.md`.
> **Phụ thuộc:** Giai đoạn backend 1–4 (Identity, Task, Sync, Outbox). Không phụ thuộc Phase 2 (Habit). Phase 5 và Phase 7 phụ thuộc phase này.
> **Ngoài phạm vi** (`PRODUCT_CONTEXT.md` §14): tích hợp ngân hàng/Open Banking, giao dịch tự động từ ngân hàng, kế toán đầy đủ, đầu tư/giao dịch chứng khoán, cổng thanh toán. Nợ và cho vay (debt/lending), hoàn tiền (refund) và quy đổi đa tiền tệ: **không làm ở Phase 3** (xem D104, D118).

---

## 1. Mục tiêu và phạm vi

Quản lý thu nhập, chi tiêu và dòng tiền cá nhân hoạt động **offline-first** như Task: ghi local ngay, đồng bộ sau. Tiền là dữ liệu nhạy cảm nhất của hệ thống: sai một chữ số là lỗi nghiêm trọng, nên phase này ưu tiên **đúng đắn, không làm tròn ngầm, không tự động hợp nhất**.

Trong phạm vi: tài khoản tài chính, danh mục thu/chi, giao dịch thu/chi, chuyển khoản giữa tài khoản, ngân sách tháng, giao dịch định kỳ, mục tiêu tiết kiệm, báo cáo tháng, dòng tiền, tìm kiếm và lọc giao dịch, đối soát số dư.

## 2. Yêu cầu chức năng

| ID | Yêu cầu | Ghi chú / tiêu chí chấp nhận |
|---|---|---|
| FR-FIN-01 | Tạo, xem, sửa, lưu trữ tài khoản | Tên (1–60), loại (`CASH`,`BANK`,`E_WALLET`,`CREDIT_CARD`,`OTHER`), `currency` (ISO 4217), `openingBalance`. Tiền tệ **không đổi** sau khi tài khoản có giao dịch |
| FR-FIN-02 | Xóa tài khoản | Chỉ khi không có giao dịch/chuyển khoản nào (kể cả đã xóa mềm không tính); ngược lại 409 và gợi ý lưu trữ |
| FR-FIN-03 | Danh mục thu/chi | Hai loại `INCOME`/`EXPENSE`; có danh mục hệ thống mặc định (tạo lúc đăng ký, xem §6.3); tên duy nhất theo (user, loại) |
| FR-FIN-04 | Tạo, xem, sửa, xóa giao dịch thu/chi | Thuộc đúng một tài khoản; `amount > 0`; `occurredOn` là ngày nghiệp vụ theo timezone user |
| FR-FIN-05 | Chuyển khoản giữa hai tài khoản | **Không** là thu/chi; cùng tiền tệ; hai tài khoản khác nhau |
| FR-FIN-06 | Ngân sách | Theo tháng (lịch), tổng chi hoặc theo một danh mục chi; theo dõi mức dùng; xem lịch sử các tháng trước |
| FR-FIN-07 | Giao dịch định kỳ | Tạo/sửa/tạm dừng/tiếp tục/xóa; server sinh giao dịch **không trùng** khi retry |
| FR-FIN-08 | Mục tiêu tiết kiệm | Mục tiêu, số tiền mục tiêu, hạn tùy chọn; ghi khoản góp; xem tiến độ |
| FR-FIN-09 | Số dư và đối soát | Số dư luôn tính từ lịch sử; có thao tác đối soát với số dư sao kê người dùng nhập |
| FR-FIN-10 | Báo cáo tháng và dòng tiền | Theo tiền tệ; loại trừ chuyển khoản và giao dịch đã xóa |
| FR-FIN-11 | Tìm kiếm và lọc giao dịch | Theo loại, tài khoản, danh mục, khoảng ngày, khoảng số tiền, từ khóa ghi chú |
| FR-FIN-12 | Hoạt động offline | Mọi chức năng FR-FIN-01..09 và 11 chạy khi mất mạng; số dư và mức dùng ngân sách tính local |
| FR-FIN-13 | Nhắc thanh toán/ngưỡng ngân sách | Thông báo khi ngân sách chạm 80% và 100%; nhắc giao dịch định kỳ sắp tới (qua Phase 4; xem §10) |

## 3. Use case chính

| ID | Use case | Tóm tắt luồng và ngoại lệ |
|---|---|---|
| UC-FIN-01 | Thêm giao dịch chi (quick capture) | Chọn tài khoản, nhập số tiền, tùy chọn danh mục/ngày/ghi chú → ghi local + outbox. Ngoại lệ: tài khoản đã lưu trữ → chặn; số lẻ vượt số chữ số thập phân của tiền tệ → báo lỗi trường |
| UC-FIN-02 | Chuyển khoản | Chọn tài khoản nguồn/đích, số tiền. Ngoại lệ: trùng tài khoản; khác tiền tệ (`FINANCE_CURRENCY_MISMATCH`) |
| UC-FIN-03 | Sửa giao dịch trên hai thiết bị | Thiết bị B push khi `baseVersion` cũ → `CONFLICT`; giao diện hiện hai bản, người dùng chọn (Phase 5). **Không** tự hợp nhất số tiền |
| UC-FIN-04 | Đối soát | Nhập số dư sao kê và ngày; hệ thống trả chênh lệch; người dùng có thể tạo giao dịch điều chỉnh bằng danh mục "Điều chỉnh" |
| UC-FIN-05 | Giao dịch định kỳ chạy | Job sinh giao dịch tới ngày hiện tại theo timezone user, mỗi lần một lần xuất hiện, idempotent |
| UC-FIN-06 | Ngân sách vượt ngưỡng | Ghi giao dịch làm mức dùng ≥ 80% → server tạo sự kiện thông báo một lần/tháng/ngưỡng |
| UC-FIN-07 | Xóa danh mục đang dùng | Chặn (409 `FINANCE_CATEGORY_IN_USE`); cho phép lưu trữ |
| UC-FIN-08 | Xem báo cáo tháng | Chọn tháng và tiền tệ; tổng thu/chi/ròng, theo danh mục, theo tài khoản, chuỗi theo ngày |

## 4. Quy tắc nghiệp vụ

1. `[CONFIRMED]` (PRODUCT_CONTEXT) Mỗi giao dịch thu/chi thuộc một tài khoản; chuyển khoản không là thu/chi; giao dịch đã xóa không vào báo cáo; không dùng `FLOAT/DOUBLE`.
2. `[CONFIRMED]` **D100 — Biểu diễn tiền:** PostgreSQL `NUMERIC(19,4)` kèm `CHECK (amount > 0 AND amount < 10000000000000)` (< 10¹³). JSON truyền **chuỗi thập phân** (`"125000"`, `"19.99"`), không bao giờ là số JSON. SQLite lưu `INTEGER` = giá trị × 10⁴ (an toàn trong int64). Client cấm `double` cho tiền.
3. `[CONFIRMED]` **D101 — Độ chính xác theo tiền tệ:** số chữ số thập phân nhập vào không vượt `Currency.getDefaultFractionDigits()` của tiền tệ (VND 0, USD 2, JPY 0...). Vượt → `VALIDATION_FAILED` fieldError `amount` mã `SCALE`. Server **không bao giờ làm tròn ngầm**.
4. `[CONFIRMED]` **D102 — Số dư không lưu:** `balance = openingBalance + Σ thu − Σ chi + Σ chuyển đến − Σ chuyển đi` (chỉ bản ghi chưa xóa), tính bằng truy vấn; không có cột số dư. Client tính local bằng cùng công thức. Có chỉ mục phục vụ truy vấn (§6). Phase 7 có thể thêm bảng dẫn xuất (xem Phase 7 D191).
5. `[CONFIRMED]` **D103 — Một tiền tệ cho mỗi tài khoản:** giao dịch mang `currency` của tài khoản (server đặt, client gửi bị bỏ qua). Báo cáo/tổng hợp **luôn tách theo tiền tệ**, không quy đổi. *(Cập nhật 2026-11-15 theo `docs/review/BACKEND_FIX_REPORT.md`: F1 — sửa giao dịch/giao dịch định kỳ để chuyển sang tài khoản KHÁC tiền tệ bị từ chối 422 `FINANCE_CURRENCY_MISMATCH`, kể cả khi gửi kèm `amount`; không tái định giá ngầm. Chuyển giữa hai tài khoản cùng tiền tệ vẫn được)*
6. `[CONFIRMED]` **D104 — Chuyển khoản cùng tiền tệ:** chuyển khác tiền tệ bị từ chối ở Phase 3 (`FINANCE_CURRENCY_MISMATCH`). Chuyển có phí: ghi phí thành một giao dịch chi riêng.
7. `[CONFIRMED]` **D105 — Không hợp nhất tự động cho dữ liệu tài chính:** mọi entity Finance dùng cơ chế `CONFLICT` khi `baseVersion` lệch (đã là mặc định của sync); không có Last-Write-Wins, không có hợp nhất theo trường. Người dùng luôn chọn. (Cập nhật bảng chiến lược `SYNC_RULES.md` §9.5 khi duyệt.)
8. `[CONFIRMED]` **D106 — Danh mục dùng rồi thì không xóa:** danh mục có giao dịch/ngân sách tham chiếu chỉ được **lưu trữ** (ẩn khỏi lựa chọn mới); xóa chỉ khi chưa dùng. Tránh cascade hàng nghìn giao dịch.
9. `[CONFIRMED]` **D107 — Tài khoản lưu trữ:** không thêm giao dịch/chuyển khoản mới (`FINANCE_ACCOUNT_ARCHIVED`); vẫn xuất hiện trong lịch sử và báo cáo; số dư vẫn tính.
10. `[CONFIRMED]` **D108 — Ngày nghiệp vụ:** `occurredOn` (DATE) do client gửi theo timezone user lúc nhập; server chỉ kiểm hợp lệ (không sớm hơn 2000-01-01, không muộn hơn hôm nay + 1 năm). Giao dịch tương lai được phép (lên kế hoạch) nhưng **không** tính vào số dư "hiện tại" (`asOf` mặc định = hôm nay theo timezone user).
11. `[CONFIRMED]` **D109 — Ngân sách:** chu kỳ cố định theo **tháng lịch** của timezone user. Mức dùng = Σ chi (loại `EXPENSE`, chưa xóa) trong tháng của tài khoản cùng tiền tệ với ngân sách; nếu ngân sách có danh mục thì chỉ tính danh mục đó. Ngân sách lặp lại mỗi tháng cho đến khi lưu trữ; lịch sử tháng cũ tính lại từ giao dịch (không lưu). Duy nhất một ngân sách đang hoạt động cho mỗi (user, danh mục hoặc tổng, tiền tệ). Chuyển khoản không tính.
12. `[CONFIRMED]` **D110 — Giao dịch định kỳ:** xem §7.
13. `[CONFIRMED]` **D111 — Khoản góp tiết kiệm độc lập với tài khoản:** góp không tự tạo giao dịch hay chuyển khoản và không đổi số dư tài khoản; chỉ cộng vào tiến độ mục tiêu. Góp là số dương; sửa sai bằng cách xóa khoản góp. Đạt mục tiêu khi Σ góp ≥ mục tiêu: trạng thái `ACHIEVED` do server đặt, người dùng có thể mở lại.
14. `[CONFIRMED]` **D112 — Đối soát:** `POST /finance/accounts/{id}/reconcile` nhận `{statementBalance, asOfDate}` và **chỉ trả** `{computedBalance, difference}`; không ghi dữ liệu. Điều chỉnh là giao dịch thường với danh mục hệ thống "Điều chỉnh".

## 5. Mô hình dữ liệu

Cột chuẩn đồng bộ (`id`, `user_id`, `version`, `deleted_at`, `created_at`, `updated_at`) theo `DATABASE_RULES.md` §8.2 có ở **mọi** bảng dưới đây (không lặp lại trong DDL rút gọn). Migration `V5__finance.sql` (và nối tiếp nếu cần); không sửa V1–V4.

```sql
CREATE TABLE financial_accounts (
  -- cột chuẩn đồng bộ
  name varchar(60) NOT NULL,
  account_type varchar(12) NOT NULL CHECK (account_type IN ('CASH','BANK','E_WALLET','CREDIT_CARD','OTHER')),
  currency char(3) NOT NULL CHECK (currency ~ '^[A-Z]{3}$'),
  opening_balance numeric(19,4) NOT NULL DEFAULT 0 CHECK (opening_balance > -10000000000000 AND opening_balance < 10000000000000),
  archived_at timestamptz
);
-- UNIQUE (user_id, lower(name)) WHERE deleted_at IS NULL

CREATE TABLE financial_categories (
  name varchar(50) NOT NULL,
  kind varchar(7) NOT NULL CHECK (kind IN ('INCOME','EXPENSE')),
  system_key varchar(30),            -- khác NULL cho danh mục hệ thống ('ADJUSTMENT', 'OTHER_INCOME', 'OTHER_EXPENSE'...)
  archived_at timestamptz
);
-- UNIQUE (user_id, kind, lower(name)) WHERE deleted_at IS NULL

CREATE TABLE financial_transactions (
  account_id uuid NOT NULL REFERENCES financial_accounts(id),
  category_id uuid REFERENCES financial_categories(id),   -- NULL = chưa phân loại
  kind varchar(7) NOT NULL CHECK (kind IN ('INCOME','EXPENSE')),
  amount numeric(19,4) NOT NULL CHECK (amount > 0 AND amount < 10000000000000),
  currency char(3) NOT NULL,         -- server đặt = currency của tài khoản
  occurred_on date NOT NULL,
  note varchar(500),
  recurring_transaction_id uuid REFERENCES recurring_transactions(id),
  recurrence_date date,              -- ngày lần xuất hiện sinh ra giao dịch này
  CHECK ((recurring_transaction_id IS NULL) = (recurrence_date IS NULL))
);
-- UNIQUE (recurring_transaction_id, recurrence_date)  (kể cả tombstone: chống sinh lại giao dịch đã xóa)
-- INDEX (user_id, account_id, occurred_on) WHERE deleted_at IS NULL
-- INDEX (user_id, occurred_on) WHERE deleted_at IS NULL
-- INDEX (user_id, category_id, occurred_on) WHERE deleted_at IS NULL AND category_id IS NOT NULL

CREATE TABLE account_transfers (
  from_account_id uuid NOT NULL REFERENCES financial_accounts(id),
  to_account_id   uuid NOT NULL REFERENCES financial_accounts(id),
  amount numeric(19,4) NOT NULL CHECK (amount > 0 AND amount < 10000000000000),
  currency char(3) NOT NULL,
  occurred_on date NOT NULL,
  note varchar(500),
  CHECK (from_account_id <> to_account_id)
);
-- INDEX (user_id, from_account_id, occurred_on), (user_id, to_account_id, occurred_on)  WHERE deleted_at IS NULL

CREATE TABLE budgets (
  category_id uuid REFERENCES financial_categories(id),   -- NULL = tổng chi
  currency char(3) NOT NULL,
  amount numeric(19,4) NOT NULL CHECK (amount > 0 AND amount < 10000000000000),
  archived_at timestamptz
);
-- UNIQUE (user_id, COALESCE(category_id, '00000000-0000-0000-0000-000000000000'::uuid), currency) WHERE deleted_at IS NULL AND archived_at IS NULL

CREATE TABLE recurring_transactions (
  account_id uuid NOT NULL REFERENCES financial_accounts(id),
  category_id uuid REFERENCES financial_categories(id),
  kind varchar(7) NOT NULL CHECK (kind IN ('INCOME','EXPENSE')),
  amount numeric(19,4) NOT NULL CHECK (amount > 0 AND amount < 10000000000000),
  currency char(3) NOT NULL,
  note varchar(500),
  frequency varchar(7) NOT NULL CHECK (frequency IN ('DAILY','WEEKLY','MONTHLY','YEARLY')),
  interval_count smallint NOT NULL DEFAULT 1 CHECK (interval_count BETWEEN 1 AND 365),
  start_on date NOT NULL,
  end_on date,                       -- NULL = vô thời hạn
  next_run_on date NOT NULL,         -- ngày lần xuất hiện kế tiếp chưa sinh
  status varchar(6) NOT NULL DEFAULT 'ACTIVE' CHECK (status IN ('ACTIVE','PAUSED')),
  CHECK (end_on IS NULL OR end_on >= start_on)
);
-- INDEX (status, next_run_on) WHERE deleted_at IS NULL AND status = 'ACTIVE'   -- phục vụ job

CREATE TABLE saving_goals (
  name varchar(80) NOT NULL,
  currency char(3) NOT NULL,
  target_amount numeric(19,4) NOT NULL CHECK (target_amount > 0 AND target_amount < 10000000000000),
  target_on date,
  status varchar(8) NOT NULL DEFAULT 'ACTIVE' CHECK (status IN ('ACTIVE','ACHIEVED','ARCHIVED'))
);

CREATE TABLE saving_contributions (
  goal_id uuid NOT NULL REFERENCES saving_goals(id),
  amount numeric(19,4) NOT NULL CHECK (amount > 0 AND amount < 10000000000000),
  occurred_on date NOT NULL,
  note varchar(500)
);
-- INDEX (user_id, goal_id) WHERE deleted_at IS NULL
```

Ghi chú: ràng buộc chéo (danh mục đúng `kind`, tài khoản chưa lưu trữ, `currency` khớp tài khoản) kiểm ở service, vì CHECK không tham chiếu bảng khác. FK chỉ tồn tại vì hệ thống chỉ **xóa mềm**; không `ON DELETE`.

### 5.1. Danh mục hệ thống mặc định `[CONFIRMED]` D113

Tạo lúc đăng ký (cùng transaction, ghi `change_logs` CREATE theo D21): thu — Lương, Thưởng, Thu nhập đầu tư, Làm thêm, Thu khác; chi — Ăn uống, Di chuyển, Nhà ở, Giáo dục, Giải trí, Mua sắm, Sức khỏe, Chi khác; hai danh mục kỹ thuật `ADJUSTMENT` (Điều chỉnh, cho cả hai loại, tạo khi cần đối soát). Người dùng sửa tên/lưu trữ được (là danh mục thường, có `system_key` chỉ để nhận diện). Với tài khoản đã đăng ký trước phase này: job khởi động bù idempotent (không tạo trùng theo `system_key`).

## 6. API (`/api/v1/finance`, tuân theo `API_CONVENTIONS.md`)

Quy ước: `version` bắt buộc cho PATCH/action/DELETE; client tạo `id` ở POST; tiền là chuỗi; danh sách dùng cursor; 404 khi không thấy hoặc của user khác; tham chiếu sai ở body → 422.

| Endpoint | Ghi chú |
|---|---|
| `POST/GET /accounts`, `GET/PATCH /accounts/{id}`, `POST /accounts/{id}/archive` `/unarchive`, `DELETE /accounts/{id}?version=` | `GET` kèm `balance` (tính `asOf` hôm nay); `GET /accounts/{id}/balance?asOf=YYYY-MM-DD` |
| `POST /accounts/{id}/reconcile` | D112, chỉ đọc |
| `POST/GET /categories`, `GET/PATCH/DELETE /categories/{id}`, `POST /categories/{id}/archive` `/unarchive` | `?kind=INCOME|EXPENSE&archived=` |
| `POST/GET /transactions`, `GET/PATCH/DELETE /transactions/{id}` | Lọc: `kind, accountId, categoryId, from, to, minAmount, maxAmount, q`; sắp theo `occurredOn desc, id desc`; `q` tìm trong `note` (ILIKE, tối đa 100 ký tự) |
| `POST/GET /transfers`, `GET/PATCH/DELETE /transfers/{id}` | Sửa chuyển khoản không đổi hai tài khoản (xóa và tạo lại) |
| `POST/GET /budgets`, `GET/PATCH/DELETE /budgets/{id}`, `POST /budgets/{id}/archive`, `GET /budgets/{id}/usage?month=YYYY-MM`, `GET /budgets/usage?month=` | Mức dùng: `{spent, limit, ratio, remaining}` |
| `POST/GET /recurring`, `GET/PATCH/DELETE /recurring/{id}`, `POST /recurring/{id}/pause` `/resume` | `PATCH` đổi `amount/category/note` chỉ ảnh hưởng lần xuất hiện **sau** |
| `POST/GET /saving-goals`, `GET/PATCH/DELETE /saving-goals/{id}`, `POST /saving-goals/{id}/contributions`, `GET …/contributions`, `DELETE /saving-contributions/{id}?version=` | Tiến độ: `{saved, target, ratio, remaining}` |
| `GET /reports/monthly?month=YYYY-MM&currency=XXX` | Tổng thu/chi/ròng, theo danh mục, theo tài khoản, chuỗi theo ngày |
| `GET /reports/cash-flow?from=&to=&granularity=month|week|day&currency=` | Tối đa 24 tháng / 366 ngày mỗi lần |

Mã lỗi mới `[CONFIRMED]` D114 (bổ sung catalog `API_CONVENTIONS.md` §5.1): `FINANCE_ACCOUNT_NOT_FOUND` (404 / 422 khi là tham chiếu), `FINANCE_CATEGORY_NOT_FOUND`, `FINANCE_TRANSACTION_NOT_FOUND`, `FINANCE_TRANSFER_NOT_FOUND`, `FINANCE_BUDGET_NOT_FOUND`, `FINANCE_RECURRING_NOT_FOUND`, `FINANCE_GOAL_NOT_FOUND` (404), `FINANCE_ACCOUNT_ARCHIVED` (422), `FINANCE_ACCOUNT_HAS_TRANSACTIONS` (409), `FINANCE_CATEGORY_IN_USE` (409), `FINANCE_CATEGORY_KIND_MISMATCH` (422), `FINANCE_CURRENCY_MISMATCH` (422), `FINANCE_CURRENCY_LOCKED` (409, đổi tiền tệ khi đã có giao dịch), `FINANCE_BUDGET_EXISTS` (409), `FINANCE_NAME_TAKEN` (409). Lỗi số tiền (`SCALE`, giới hạn) dùng `VALIDATION_FAILED` + `fieldErrors`.

## 7. Giao dịch định kỳ (thiết kế chi tiết) `[CONFIRMED]` D110

> **Cập nhật 2026-11-15 theo `docs/review/BACKEND_FIX_REPORT.md` (F6):** tài khoản đã lưu trữ KHÔNG còn làm job "bỏ qua lần xuất hiện và tiến `nextRunOn`". Thay vào đó: (1) lưu trữ tài khoản (REST `/archive` hoặc sync `archived=true`) chuyển mọi giao dịch định kỳ `ACTIVE` của tài khoản sang `PAUSED` trong cùng transaction; (2) job gặp định kỳ `ACTIVE` mà tài khoản không ghi được thì cũng chuyển `PAUSED`; cả hai trường hợp phát sự kiện outbox `RECURRING_PAUSED` → thông báo FINANCE `finance.recurring.paused` (không số tiền/ghi chú) và `nextRunOn` giữ nguyên; (3) không "tiếp tục" được khi tài khoản còn lưu trữ (422 `FINANCE_ACCOUNT_ARCHIVED`); mở lại tài khoản không tự chạy lại — người dùng chọn tiếp tục, theo quy tắc tiếp tục hiện có (không bù lần lỡ trong lúc tạm dừng).

- **Nguồn sự thật:** bản ghi `recurring_transactions`. Job `RecurringTransactionJob` chạy mỗi giờ (một lần ở 00:05 theo timezone user cũng đủ, nhưng chạy mỗi giờ để chịu được lần chạy trễ và nhiều timezone).
- **Chọn việc:** `SELECT … WHERE status='ACTIVE' AND next_run_on <= (hôm nay theo timezone user) AND (end_on IS NULL OR next_run_on <= end_on) ORDER BY next_run_on LIMIT n FOR UPDATE SKIP LOCKED`. Ngày "hôm nay" tính theo `user_settings.timezone` của chủ.
- **Sinh giao dịch (mỗi lần xuất hiện một transaction ngắn):** khóa bản ghi định kỳ; với mỗi `next_run_on ≤ hôm nay` (tối đa **31 lần xuất hiện bù mỗi lần chạy**): chèn giao dịch với `(recurring_transaction_id, recurrence_date)`; `INSERT … ON CONFLICT DO NOTHING` để **idempotent khi job chạy lặp/song song**; tăng `next_run_on`; tăng `version`; `ChangeRecorder.record` cho giao dịch (CREATE, `mutation_id` NULL) và cho bản ghi định kỳ (UPDATE). Tuân thủ thứ tự khóa D86 (khóa/flush dữ liệu nghiệp vụ trước, `record` cuối).
- **Quy tắc ngày:** `MONTHLY` giữ "ngày trong tháng" của `start_on`; tháng không có ngày đó dùng **ngày cuối tháng** (31/1 → 28 hoặc 29/2 → 31/3). `WEEKLY` giữ thứ. `YEARLY` giữ ngày/tháng (29/2 → 28/2 năm không nhuận). Tính từ `start_on` (không cộng dồn từ lần trước) để không trôi ngày.
- **Tạm dừng/tiếp tục:** khi tiếp tục, các lần xuất hiện bị lỡ trong lúc tạm dừng **không** sinh bù: `next_run_on` được đẩy tới lần xuất hiện đầu tiên ≥ hôm nay.
- **Xóa định kỳ:** xóa mềm bản ghi; giao dịch đã sinh giữ nguyên. **Xóa một giao dịch đã sinh** không làm job sinh lại (khóa UNIQUE gồm cả tombstone).
- **Sửa định kỳ qua sync (offline):** xử lý như entity thường. Client **không** tự sinh giao dịch từ định kỳ (chỉ server sinh); giao dịch xuất hiện qua pull. Khi offline lâu, dữ liệu sinh sau khi có mạng; chấp nhận độ trễ (ghi rõ trong UI).
- **Thông báo (Phase 4):** nhắc trước hạn do `reminders` đảm nhiệm; không thuộc job này.

## 8. Đồng bộ (`entityType` mới và quy tắc handler)

Handler đặt ở `finance.sync.*SyncHandler`, tuân thủ `SYNC_PROTOCOL.md` và luật A13.

| `entityType` | Ghi chú payload (khóa chỉnh sửa được) |
|---|---|
| `financial_account` | `name, accountType, currency, openingBalance, archived`; `currency` chỉ đặt lúc CREATE (UPDATE đổi → `REJECTED FINANCE_CURRENCY_LOCKED` nếu đã có giao dịch, ngược lại cho phép); DELETE theo FR-FIN-02 |
| `financial_category` | `name, kind, archived`; `kind` bất biến sau CREATE |
| `financial_transaction` | `accountId, categoryId, kind, amount, occurredOn, note`; `currency`, `recurringTransactionId`, `recurrenceDate` do server |
| `account_transfer` | `fromAccountId, toAccountId, amount, occurredOn, note`; UPDATE không đổi hai tài khoản |
| `budget` | `categoryId, currency, amount, archived` |
| `recurring_transaction` | `accountId, categoryId, kind, amount, note, frequency, intervalCount, startOn, endOn, status`; `nextRunOn` do server |
| `saving_goal` | `name, currency, targetAmount, targetOn, status` (`ACHIEVED` chỉ server đặt; client gửi `ARCHIVED`/`ACTIVE`) |
| `saving_contribution` | `goalId, amount, occurredOn, note` |

Quy tắc chung `[CONFIRMED]` D115:
- Tham chiếu sai (tài khoản/danh mục/mục tiêu không tồn tại, đã xóa, của user khác) → `REJECTED <TYPE>_NOT_FOUND` cùng một dạng; tài khoản lưu trữ → `REJECTED FINANCE_ACCOUNT_ARCHIVED`.
- Thứ tự đẩy: client giữ thứ tự tạo trong outbox; một `CREATE` bị `REJECTED` làm các mutation phụ thuộc cũng bị `REJECTED` (not found) — client hiển thị một lỗi gộp.
- Mọi lệch `baseVersion` → `CONFLICT` (D105). `serverSnapshot.data.amount` là chuỗi.
- Giao dịch do job sinh đến client qua pull dưới dạng `CREATE`.
- Giới hạn payload 64 KB (đủ rộng); không có giới hạn số bản ghi/user ở Phase 3 `[OPEN]` (hạn mức người dùng xem Phase 7).

## 9. Bảo mật và riêng tư

- IDOR: mọi truy vấn có `user_id`; ma trận `IdorMatrixIT` tự động bao phủ route mới (`{id}`); cùng 404 cho không tồn tại/của user khác (IDOR-01..04 áp dụng cho tham chiếu `accountId/categoryId/goalId`).
- Không đưa `amount`, `note`, tên tài khoản/danh mục vào log, metric nhãn, thông báo lỗi, hoặc payload push (thông báo ngân sách chỉ nêu tên danh mục nếu người dùng bật "hiện chi tiết trên màn hình khóa" `[OPEN]`). Mở rộng `SensitiveLogIT`.
- Tìm kiếm `q`: tham số hóa, giới hạn độ dài; không `LIKE` nối chuỗi.
- Xuất/nhập dữ liệu và xóa tài khoản: `[OPEN]` (xem README phase).
- Mã hóa dữ liệu tài chính ở mức cột `[OPEN]` (đã hoãn cùng mã hóa at rest, `PRODUCT_CONTEXT` §19).

## 10. Liên kết với phase khác

Phase 4: ngưỡng ngân sách và nhắc định kỳ sinh `outbox_events` loại `BUDGET_THRESHOLD`/`RECURRING_DUE` do module `finance` ghi trong transaction nghiệp vụ (D116). Phase 5: tombstone giao dịch không bị xóa cứng khi còn khóa UNIQUE định kỳ tham chiếu (xem Phase 5 D156 và `[OPEN]` 3 ở đó). Phase 7: tổng hợp qua `finance.api.FinanceQueryApi`, không truy vấn bảng trực tiếp.

## 11. Kế hoạch kiểm thử (FIN-xx)

Dùng PostgreSQL thật; mỗi test dùng user/UUID riêng; không `Thread.sleep`; test đồng thời lặp ≥ 20 lần.

| ID | Kịch bản | Kỳ vọng |
|---|---|---|
| FIN-01 | Số tiền: `"0.1"+"0.2"` cộng 1000 lần; giá trị cực đại; chuỗi số JSON bị từ chối nếu là số | Chính xác tuyệt đối; không có `double` trong mã (ArchUnit/kiểm tĩnh cấm `double/float` ở package `finance`) |
| FIN-02 | `SCALE`: VND `"10.5"` bị từ chối, USD `"10.555"` bị từ chối | `VALIDATION_FAILED` fieldError `amount` |
| FIN-03 | Số dư = opening + thu − chi + chuyển đến − chuyển đi; bản đã xóa không tính; giao dịch tương lai không tính vào "hôm nay" | Khớp mô hình tham chiếu viết độc lập (property-based, ngẫu nhiên 500 thao tác) |
| FIN-04 | Chuyển khoản không xuất hiện trong báo cáo thu/chi; tổng thu/chi của báo cáo không đổi | Đúng |
| FIN-05 | Chuyển khác tiền tệ / cùng tài khoản | 422 `FINANCE_CURRENCY_MISMATCH` / `VALIDATION_FAILED` |
| FIN-06 | Giao dịch vào tài khoản lưu trữ (REST và sync) | 422 / `REJECTED FINANCE_ACCOUNT_ARCHIVED` |
| FIN-07 | Xóa tài khoản có giao dịch; danh mục đang dùng | 409 `FINANCE_ACCOUNT_HAS_TRANSACTIONS` / `FINANCE_CATEGORY_IN_USE` |
| FIN-08 | Đổi tiền tệ tài khoản đã có giao dịch | 409 `FINANCE_CURRENCY_LOCKED` |
| FIN-09 | Định kỳ: job chạy 2 lần liên tiếp; 2 luồng song song (20 lần) | Mỗi `recurrence_date` đúng một giao dịch; `next_run_on` đúng |
| FIN-10 | Định kỳ: ngày cuối tháng (31/1→28/2→31/3), 29/2, DST/timezone user lệch UTC, bù tối đa 31 lần, tạm dừng/tiếp tục không bù | Đúng quy tắc §7 (Clock cố định trong test) |
| FIN-11 | Xóa giao dịch đã sinh rồi chạy job | Không sinh lại |
| FIN-12 | Ngân sách: tổng và theo danh mục; ranh giới tháng theo timezone user (giao dịch 23:59/00:00); tách tiền tệ; không tính chuyển khoản/đã xóa | Đúng |
| FIN-13 | Ngưỡng 80%/100%: vượt đúng một lần/tháng/ngưỡng dù ghi nhiều giao dịch hoặc retry | Đúng một `outbox_events` mỗi ngưỡng/tháng |
| FIN-14 | Mục tiêu tiết kiệm: tiến độ, `ACHIEVED` do server, xóa khoản góp mở lại đúng | Đúng |
| FIN-15 | Đối soát: chênh lệch đúng, không ghi dữ liệu | Đúng |
| FIN-16 | Báo cáo/dòng tiền: tách tiền tệ, loại trừ tombstone, chuỗi theo ngày đủ ngày không có giao dịch | Đúng |
| FIN-17 | Lọc/tìm kiếm tổ hợp, phân trang không sót/lặp, `q` có ký tự đặc biệt (`%`, `_`, `'`) | Đúng, không lỗi SQL |
| FIN-18 | Sync: CONFLICT khi sửa số tiền trên hai thiết bị; không có LWW | `CONFLICT` kèm `serverSnapshot` (số tiền chuỗi) |
| FIN-19 | Sync: tham chiếu sai, thứ tự phụ thuộc (CREATE tài khoản bị REJECTED → giao dịch phụ thuộc REJECTED) | Đúng |
| FIN-20 | IDOR/mass assignment cho cả 8 entity (`currency`, `recurrenceDate`, `nextRunOn`, `ACHIEVED` bị bỏ qua) | Đúng |
| FIN-21 | Log/lỗi/metric không chứa số tiền hoặc ghi chú | `SensitiveLogIT` mở rộng |
| FIN-22 | Danh mục mặc định tạo lúc đăng ký, job bù không tạo trùng | Đúng |
| FIN-23 | `EXPLAIN` các truy vấn số dư, báo cáo, lọc: không `Seq Scan` trên bảng lớn với 100k giao dịch/user | Ghi kết quả vào báo cáo |

## 12. Các bước triển khai và tiêu chí hoàn thành

| Bước | Nội dung |
|---|---|
| 3a | `V5` (tài khoản, danh mục), danh mục mặc định, CRUD, số dư |
| 3b | Giao dịch, chuyển khoản, đối soát, lọc/tìm kiếm |
| 3c | Ngân sách, mục tiêu tiết kiệm, khoản góp |
| 3d | Giao dịch định kỳ và job |
| 3e | Báo cáo, dòng tiền, sync handler cho cả 8 entity, hardening, FIN-01..23 |

Hoàn thành khi: `mvn clean verify` xanh; mọi FIN-xx có test; OpenAPI/fixture cập nhật; không có số thực dấu phẩy động trong tiền; báo cáo theo mẫu `CLAUDE_CODE_PHASE1_PROMPT.md` §5.

## 13. Quyết định và câu hỏi mở

| ID | Quyết định |
|---|---|
| D100 | Biểu diễn tiền: `NUMERIC(19,4)`, JSON chuỗi, SQLite INTEGER ×10⁴ |
| D101 | Độ chính xác theo tiền tệ; không làm tròn ngầm |
| D102 | Số dư luôn tính, không lưu |
| D103 | Một tiền tệ mỗi tài khoản; báo cáo tách theo tiền tệ |
| D104 | Chuyển khoản chỉ cùng tiền tệ ở Phase 3 |
| D105 | Finance không hợp nhất tự động; luôn `CONFLICT` |
| D106 | Danh mục đã dùng chỉ được lưu trữ |
| D107 | Tài khoản lưu trữ không nhận giao dịch mới |
| D108 | Ngày nghiệp vụ do client gửi; giao dịch tương lai không tính vào số dư hiện tại |
| D109 | Ngân sách tháng lịch theo timezone user; chi theo tiền tệ/danh mục |
| D110 | Định kỳ: server sinh, idempotent theo `(recurring_id, recurrence_date)`, bù ≤ 31, không bù khi tạm dừng |
| D111 | Khoản góp tiết kiệm độc lập với số dư tài khoản |
| D112 | Đối soát chỉ đọc |
| D113 | Danh mục mặc định tạo lúc đăng ký + job bù |
| D114 | Mã lỗi `FINANCE_*` |
| D115 | Quy tắc sync Finance (tham chiếu, thứ tự, CONFLICT) |
| D116 | Sự kiện outbox `BUDGET_THRESHOLD`, `RECURRING_DUE` |
| D117 | Mã lỗi giới hạn tiền: `VALIDATION_FAILED` + `fieldErrors` |
| D118 | Ngoài phạm vi Phase 3: nợ/cho vay, hoàn tiền, quy đổi tiền tệ, nhập/xuất |
| D119 | Mức dùng ngân sách tính theo tài khoản cùng tiền tệ, loại trừ `ADJUSTMENT`? → `[OPEN]` xem dưới |

`[OPEN]` cần người dùng quyết trước khi code: (1) giao dịch "Điều chỉnh" có tính vào mức dùng ngân sách và báo cáo chi không (khuyến nghị: **tính vào số dư, không tính vào ngân sách/báo cáo thu-chi**); (2) hạn mức số bản ghi mỗi user (khuyến nghị: không giới hạn ở Phase 3, theo dõi ở Phase 7); (3) nhập/xuất CSV (khuyến nghị: Phase 7); (4) hiện số tiền trên thông báo màn hình khóa (khuyến nghị: tắt mặc định).
