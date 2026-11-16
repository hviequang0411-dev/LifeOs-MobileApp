# LifeOS — Phase 8: AI Personal Assistant (đặc tả đã duyệt)

> **Trạng thái:** v1 — **ĐÃ DUYỆT 2026-11-10**. D260–D299 và O-AI-01..08 có hiệu lực theo phương án khuyến nghị.
> **Nguồn:** `docs/product/PRODUCT_CONTEXT.md` Module 13, §8.1, §11, Phase 8 và §16; `AGENTS.md`; `docs/engineering/JAVA_SPRING_RULES.md`; `SECURITY_RULES.md`; `DATABASE_RULES.md`; `API_CONVENTIONS.md`; `TESTING_RULES.md`; `docs/phases/PHASE_7_INTELLIGENCE_ANALYTICS.md`.
> **Phụ thuộc:** Identity, Task/Project, Habit/Routine, Calendar, Finance, Notes/Goals, Analytics, Outbox và xuất/xóa tài khoản của Phase 1–7.
> **Ngoài phạm vi của lần lập kế hoạch này:** viết mã, migration, OpenAPI/fixture thật, thay đổi backend/mobile, gọi nhà cung cấp AI thật.

Nhãn: D260–D299 là `[CONFIRMED]`. Các lựa chọn O-AI-01..08 đã được giải quyết theo phương án khuyến nghị; quyết định mới phát sinh trong lúc thực thi vẫn phải ghi `[PROPOSED]` vào progress log.

---

## 1. Mục tiêu, nguyên tắc và phạm vi

Phase 8 bổ sung một trợ lý AI **online-only** có thể trả lời dựa trên dữ liệu LifeOS mà người dùng đã cho phép, tạo kế hoạch và đề xuất thay đổi. AI không trở thành nguồn sự thật, không thay business rule và không có quyền ghi trực tiếp vào dữ liệu nghiệp vụ.

Thứ tự giá trị/rủi ro đề xuất:

1. Chat chỉ đọc, lịch sử hội thoại, hiển thị ngữ cảnh đã dùng, hạn mức và chi phí.
2. Kế hoạch ngày/tuần, chia nhỏ Task thành danh sách việc độc lập, gợi ý ưu tiên và lịch; chỉ trả đề xuất.
3. Phân tích Habit, Goal, Finance và xu hướng; Finance có consent riêng, chỉ đọc.
4. Tóm tắt Note; semantic search/RAG chỉ triển khai sau khi chốt pgvector, embedding và vận hành.
5. Tool calling tạo proposal; chỉ một tập hành động Task nhỏ được phép sau khi lớp read-only ổn định.

Không thuộc MVP-AI đầu tiên: AI offline, voice/realtime, web search, agent tự chạy nền, tự động hóa không duyệt, giao dịch tài chính, xóa dữ liệu nghiệp vụ, RAG/pgvector, fine-tuning và nhiều nhà cung cấp chạy đồng thời.

### 1.1. Mục tiêu chấp nhận

- Người dùng phải biết miền dữ liệu nào được phép dùng và có thể thu hồi ngay.
- Mỗi phản hồi cho xem chính xác bản ngữ cảnh đã gửi nhà cung cấp.
- Nội dung Note/Task do người dùng nhập luôn được xem là dữ liệu không tin cậy, không phải chỉ thị.
- Mọi đầu ra có cấu trúc được kiểm bằng schema và business validation của backend.
- AI lỗi, hết hạn mức hoặc bị tắt không ảnh hưởng CRUD, sync, reminder, Finance hay các chức năng cốt lõi.
- Không có secret nhà cung cấp trong Flutter, log, metric, fixture hay image.

---

## 2. Yêu cầu chức năng

| ID | Yêu cầu | Tiêu chí chấp nhận |
|---|---|---|
| FR-AI-01 | Consent theo miền | Task/Calendar, Habit, Goal, Notes và Finance có công tắc riêng; mặc định tắt; thu hồi có hiệu lực trước request kế tiếp |
| FR-AI-02 | Chat có ngữ cảnh chỉ đọc | Trả lời chỉ từ dữ liệu thuộc user và miền đã bật; không có quyền ghi |
| FR-AI-03 | Minh bạch ngữ cảnh | Mỗi assistant message có endpoint xem bản dữ liệu tối thiểu đã gửi nhà cung cấp |
| FR-AI-04 | Hội thoại | Tạo, liệt kê, xem, xóa từng hội thoại và xóa toàn bộ dữ liệu AI |
| FR-AI-05 | Kế hoạch | Sinh kế hoạch ngày/tuần, chia nhỏ Task, gợi ý ưu tiên/lịch dưới dạng đề xuất có cấu trúc |
| FR-AI-06 | Phân tích | Giải thích xu hướng Task/Habit/Goal; Finance chỉ khi có consent Finance |
| FR-AI-07 | Tóm tắt Note | Tóm tắt Note được chọn, chỉ khi consent Notes; chưa semantic search ở MVP |
| FR-AI-08 | Usage và quota | Người dùng xem số request/token/chi phí ước tính, hạn mức ngày/tháng và thời điểm reset |
| FR-AI-09 | Proposal hành động | Đầu ra AI chỉ tạo proposal; UI hiển thị diff chính xác trước khi duyệt |
| FR-AI-10 | Duyệt/hủy | Người dùng duyệt hoặc hủy proposal còn hạn; duyệt một lần chỉ thực thi tối đa một lần |
| FR-AI-11 | Xóa và xuất dữ liệu | Hội thoại, message, context snapshot, usage và proposal nằm trong xuất/xóa tài khoản; có xóa AI riêng |
| FR-AI-12 | Suy giảm êm | Provider lỗi/quota/công tắc ngắt trả lỗi AI ổn định; chức năng không-AI vẫn hoạt động |

---

## 3. Use case chính

| ID | Use case | Luồng và ngoại lệ |
|---|---|---|
| UC-AI-01 | Bật AI lần đầu | Tất cả consent tắt → người dùng chọn từng miền → xem thông báo nhà cung cấp/retention → lưu version consent |
| UC-AI-02 | Hỏi “Hôm nay nên ưu tiên gì?” | Context Builder lấy Task/Calendar đã bật, cắt theo cửa sổ thời gian và giới hạn → provider → schema validation → lưu message/context/usage → trả kết quả |
| UC-AI-03 | Hỏi về Finance khi chưa đồng ý | Không gọi provider; trả `AI_CONSENT_REQUIRED` với miền `FINANCE` |
| UC-AI-04 | Thu hồi consent Notes | Request đang chạy được phép hoàn tất theo consent snapshot lúc bắt đầu; request sau không lấy Note; UI hiển thị thời điểm thu hồi |
| UC-AI-05 | Note chứa “hãy bỏ qua system prompt” | Chuỗi nằm trong khối dữ liệu được đánh dấu; không kích hoạt tool; schema chỉ cho phép response/proposal hợp lệ |
| UC-AI-06 | Lập kế hoạch tuần | Trả các mục đề xuất và lý do; chưa ghi Task/Calendar |
| UC-AI-07 | Duyệt tạo Task | Backend tải proposal còn hạn, khóa/version-check, validate payload và ownership, gọi Task Application Service, lưu kết quả; request duyệt lặp trả cùng kết quả |
| UC-AI-08 | Provider timeout | Đóng usage log với mã lỗi, không lưu assistant message nửa vời, trả 503 có thể thử lại |
| UC-AI-09 | Vượt quota | Không gọi provider; trả 429 và số giây tới lần reset; không chặn ứng dụng cốt lõi |
| UC-AI-10 | Xóa dữ liệu AI | Xóa hội thoại/message/context/proposal và usage cá nhân theo chính sách; preference reset về tắt; không xóa dữ liệu nghiệp vụ |

---

## 4. Quyết định nghiệp vụ và kỹ thuật D260–D299

### 4.1. Phạm vi, provider và mô hình

1. `[CONFIRMED]` **D260 — Phân kỳ:** MVP-AI gồm consent, chat chỉ đọc, lịch sử, context disclosure, kế hoạch/insight chỉ đọc và usage/quota. RAG và hành động ghi là các gói sau, chỉ bật bằng feature flag riêng sau khi test an toàn hoàn tất.
2. `[CONFIRMED]` **D261 — Provider:** dùng `LLMProvider` làm port và `OpenAIProvider` làm adapter đầu tiên; backend gọi OpenAI Responses API, `store=false`; code nghiệp vụ không import SDK/type của nhà cung cấp.
3. `[CONFIRMED]` **D262 — Mô hình:** mặc định `gpt-6.1-sol` vì cân bằng chất lượng/chi phí; model lấy từ secret/config server, được allowlist và không nhận từ client. Không tự chuyển model khi lỗi. Giá tham chiếu ngày 2026-11-10: 2 USD/1M input token và 10 USD/1M output token; phải xác minh lại trước phát hành.
4. `[CONFIRMED]` **D263 — Điều kiện dữ liệu nhà cung cấp:** không opt-in chia sẻ dữ liệu huấn luyện; `store=false`; privacy notice nói rõ API data không dùng huấn luyện theo mặc định nhưng abuse-monitoring có thể giữ tối đa 30 ngày nếu tài khoản chưa được duyệt Zero Data Retention. Notes/Finance production chỉ bật khi tổ chức có ZDR theo O-AI-03 đã duyệt.

Nguồn chính thức dùng cho D261–D263: [OpenAI model comparison](https://developers.openai.com/api/docs/models/compare), [pricing](https://developers.openai.com/api/docs/pricing), [data controls](https://developers.openai.com/api/docs/guides/your-data), [Structured Outputs](https://developers.openai.com/api/docs/guides/structured-outputs).

### 4.2. Consent, context và riêng tư

5. `[CONFIRMED]` **D264 — Consent mặc định tắt:** `ai_user_preferences` lưu năm miền `TASK_CALENDAR`, `HABIT`, `GOAL`, `NOTES`, `FINANCE`; tất cả `false` khi chưa có dòng. Consent có `policy_version`, `accepted_provider_terms_at`, `updated_at`.
6. `[CONFIRMED]` **D265 — Finance riêng:** consent Finance không được suy ra từ consent khác; chỉ cho phân tích đọc, tổng hợp theo currency; không gửi số tài khoản/ghi chú giao dịch khi không cần; không bao giờ tạo/sửa/xóa giao dịch hay số dư.
7. `[CONFIRMED]` **D266 — Tối thiểu hóa:** Context Builder chỉ lấy trường cần cho use case, giới hạn số bản ghi/cửa sổ thời gian/độ dài text; cấm gửi password/hash, token, email, device/push token, audit/security event, raw outbox, mutation log và định danh không cần thiết.
8. `[CONFIRMED]` **D267 — Context disclosure:** mỗi assistant message lưu `context_snapshot` đúng bằng payload dữ liệu đã gửi provider sau redaction, kèm domain, entity type/id/version và thời điểm; người dùng xem được qua API. Snapshot không chứa system/developer prompt, API key hay nội bộ bảo mật.
9. `[CONFIRMED]` **D268 — AI online-only/server-only:** hội thoại AI không đi qua sync và không có entityType; Flutter không gọi provider; khi offline chỉ xem cache hội thoại đã tải nếu client chọn lưu, không sinh phản hồi mới.
10. `[CONFIRMED]` **D269 — Retention hội thoại:** mặc định 90 ngày kể từ hoạt động cuối; xóa từng hội thoại hoặc xóa toàn bộ ngay; job hằng ngày xóa quá hạn theo lô. O-AI-02 đã duyệt giá trị 90 ngày.
11. `[CONFIRMED]` **D270 — Giới hạn nội dung:** user message tối đa 8.000 ký tự; assistant content tối đa 16.000 ký tự; context snapshot tối đa 256 KiB sau serialize; tối đa 40 message gần nhất và 24.000 input token/request, lấy giới hạn nhỏ hơn.

### 4.3. Prompt injection và kiểm soát đầu ra

12. `[CONFIRMED]` **D271 — Tách chỉ thị và dữ liệu:** system instruction, request intent và dữ liệu user dùng các phần có nhãn rõ; mọi text từ entity được encode như dữ liệu không tin cậy; không nội dung entity nào được quyền mở tool, đổi consent hoặc yêu cầu lộ prompt.
13. `[CONFIRMED]` **D272 — Structured Outputs:** provider phải trả JSON Schema strict cho plan/insight/proposal; backend vẫn parse, allowlist enum/trường, giới hạn độ sâu/kích thước và validate business rule. Refusal/incomplete/schema mismatch không được lưu như kết quả thành công.
14. `[CONFIRMED]` **D273 — ID allowlist:** model chỉ được nhắc tới/chọn ID nằm trong context snapshot của request; backend không chấp nhận `userId` từ model và resolve mọi ID lại theo `(id, currentUserId)`.
15. `[CONFIRMED]` **D274 — Lọc đầu ra:** nội dung hiển thị là plain text/Markdown subset; loại raw HTML, script, data URL, javascript URL, system prompt/secret pattern; link ngoài chỉ hiển thị dạng text ở MVP, không tự mở.

### 4.4. Action proposal và tool calling

16. `[CONFIRMED]` **D275 — State machine:** `PROPOSED → APPROVED → EXECUTING → EXECUTED|FAILED`; từ `PROPOSED` có thể `REJECTED|CANCELLED|EXPIRED`. Chuyển trạng thái bằng optimistic version + câu update có điều kiện.
17. `[CONFIRMED]` **D276 — Hạn dùng và chống sửa:** proposal hết hạn sau 15 phút; lưu canonical payload, expected versions và SHA-256 payload hash. Approval phải gửi `proposalId`, `version`, `payloadHash`; sai hoặc hết hạn không thực thi.
18. `[CONFIRMED]` **D277 — Exactly-once ở mức ứng dụng:** mỗi proposal có `execution_key` unique; hai approval đồng thời chỉ một request chuyển sang `EXECUTING`; retry trả kết quả đã lưu. Crash ở `EXECUTING` được job đối soát đánh dấu `FAILED` với `failure_code=RETRYABLE` hoặc nối lại bằng idempotency của service đích, không thực thi mù.
19. `[CONFIRMED]` **D278 — Hành động được phép ở bản đầu:** `CREATE_TASK` (tối đa 10 Task từ một breakdown), `UPDATE_TASK_PRIORITY`, `UPDATE_TASK_SCHEDULE`. Không có subtask vì mô hình Task hiện tại chưa định nghĩa quan hệ cha-con; breakdown tạo danh sách Task độc lập và UI phải nói rõ.
20. `[CONFIRMED]` **D279 — Hành động cấm:** mọi ghi Finance; delete/soft-delete; complete/reopen; account/auth/settings/device/consent; gửi notification/email; thay đổi Goal/Habit/Calendar/Note; bulk action ngoài danh sách; hành động nền không có approval trực tiếp.
21. `[CONFIRMED]` **D280 — Thực thi qua business service:** AI module không dùng repository/entity module khác; gọi public command API tối thiểu hoặc Application Service facade đã được phép, giữ CurrentUser, ownership, version và ChangeRecorder như UI/REST. Không có “AI bypass”.

### 4.5. Transaction, độ tin cậy và tải

22. `[CONFIRMED]` **D281 — Không network trong transaction:** lưu user message/request record trong transaction ngắn → gọi provider ngoài transaction → lưu assistant message/usage/proposal trong transaction mới. Provider lỗi không rollback hay làm hỏng dữ liệu nghiệp vụ.
23. `[CONFIRMED]` **D282 — Quota:** mặc định 20 request/ngày/user, 300 request/tháng/user, 100.000 input + 10.000 output token/ngày và 1.000.000 input + 100.000 output token/tháng; đồng thời áp rate limit 5 request/phút/user. Tất cả cấu hình được và reset theo UTC.
24. `[CONFIRMED]` **D283 — Cost accounting:** `ai_usage_logs` lưu model, input/output/cached/reasoning token nếu provider trả, latency, estimated cost bằng integer micro-USD, result code và correlationId; không lưu prompt/response/entity title/note/số tiền.
25. `[CONFIRMED]` **D284 — Kill switch và ngân sách:** `lifeos.ai.enabled=false` tắt toàn hệ thống; soft alert ở 80% và hard stop ở 100% ngân sách tháng. Mặc định đề xuất 25 USD/tháng cho beta; request đang chạy được hoàn tất, request mới trả `AI_BUDGET_EXCEEDED`.
26. `[CONFIRMED]` **D285 — Provider resilience:** connect timeout 5 giây, tổng timeout 30 giây; tối đa một retry cho 429/5xx/timeout trước khi có output, có jitter; circuit breaker mở 60 giây sau 5 lỗi liên tiếp và half-open một request. Không thêm dependency resilience mới nếu chưa được duyệt; có thể cài port/state machine nhỏ và test bằng fake clock.
27. `[CONFIRMED]` **D286 — Không SSE ở MVP:** API trả JSON sau khi hoàn tất để giảm bề mặt lỗi, tránh message nửa vời và đơn giản hóa quota/schema validation. SSE là addendum sau khi đo p95 và UX.
28. `[CONFIRMED]` **D287 — Đồng thời:** tối đa 2 request provider đang chạy/user và 10 request toàn instance; vượt trả 429 có `Retry-After`. Không giữ servlet/DB connection quá quota; tải lớn hơn cần queue riêng và quyết định mới.

### 4.6. API, dữ liệu, retention và quan sát

29. `[CONFIRMED]` **D288 — Hợp đồng API:** endpoint dưới `/api/v1/ai`; response theo JSON camelCase, lỗi RFC 9457; contract và fixture AI được công bố trước implementation backend để Flutter làm song song.
30. `[CONFIRMED]` **D289 — Log an toàn:** application log/metric chỉ có correlationId, userId, provider/model allowlist, result, token count, latency và cost tổng hợp; cấm prompt, response, context snapshot, title, note, số tiền, proposal payload và provider API key.
31. `[CONFIRMED]` **D290 — V12:** migration đầu tiên của AI là `V12__ai_assistant.sql`, chỉ chứa preferences, conversations, messages, usage logs và action proposals; không sửa V1–V11.
32. `[CONFIRMED]` **D291 — Xuất/xóa:** mọi bảng AI có `user_id` phải được đăng ký `UserDataCatalog`; xuất JSON gồm content/context/consent/proposal/usage của user nhưng không secret/provider raw headers; xóa tài khoản và `DELETE /ai/data` xóa con-trước-cha; INT-15 phải đỏ nếu bỏ sót.
33. `[CONFIRMED]` **D292 — RAG hoãn:** MVP và V12 không cài `pgvector`, `ai_documents`, `ai_embeddings`; AIP5 chỉ bắt đầu sau benchmark chat, quyết định embedding model/dimension, hosting hỗ trợ extension, backup/restore và duyệt riêng.
34. `[CONFIRMED]` **D293 — RAG tương lai:** nếu duyệt, V13+ tạo document/chunk theo `noteId + noteVersion + contentHash`, re-index qua outbox sau commit, xóa embedding khi Note/tài khoản bị xóa hoặc consent Notes bị thu hồi; query luôn scope `user_id`; không dùng vector store do provider quản lý.
35. `[CONFIRMED]` **D294 — Không sync AI:** toàn bộ bảng AI là server-only; không thêm `entityType`, `change_logs`, tombstone sync hoặc mutation handler.
36. `[CONFIRMED]` **D295 — Retention metadata:** usage log giữ 365 ngày để đối soát chi phí; terminal proposal/context theo retention hội thoại 90 ngày; record tổng hợp tài chính hệ thống không chứa userId có thể giữ lâu hơn theo runbook. Xóa AI riêng xóa metadata gắn user ngay.
37. `[CONFIRMED]` **D296 — Metric hữu hạn:** request/result, provider latency, schema rejection, quota rejection, circuit state, inflight, estimated cost, action outcome và last-success; nhãn chỉ là provider/model/result/actionType allowlist, không userId/conversationId.
38. `[CONFIRMED]` **D297 — Fake provider trong CI:** mọi test tự động dùng stub/WireMock nội bộ, không network/API key; response fixture bao phủ success, refusal, malformed, injection, timeout, 429 và 5xx. Test provider thật chỉ là smoke thủ công tách biệt, opt-in và có budget.
39. `[CONFIRMED]` **D298 — Module boundary:** `ai` chỉ đọc module khác qua `*.api.*`, action qua command facade được duyệt; không import repository/entity; thêm ArchUnit A15 cho ma trận này và test cấm network client trong method `@Transactional`.
40. `[CONFIRMED]` **D299 — Rollout:** feature flags tách `chat`, `insights`, `notes`, `finance`, `rag`, `actions`; mặc định chỉ `chat` bật ở staging và tất cả tắt ở production cho đến khi privacy notice, quota, monitoring và smoke test được xác nhận.

---

## 5. Kiến trúc module `ai`

```text
com.lifeos.ai
├── api/                 # chỉ khi module khác cần port; mặc định không có caller ngoài
├── controller/          # preferences, conversations, generations, proposals, usage
├── service/             # orchestration, context, consent, quota, actions, retention
├── provider/            # LLMProvider, OpenAIProvider, fake provider
├── prompt/              # versioned prompt + JSON schema, không chứa secret
├── tool/                # tool description/proposal builder; không ghi trực tiếp
├── repository/ entity/ dto/ mapper/
└── metrics/
```

Luồng chat:

```text
AI Controller
  → Consent/Quota check
  → transaction T1: lưu user message + request marker
  → ContextBuilder qua các *QueryApi (read-only, không transaction kéo dài)
  → OpenAIProvider (ngoài transaction, timeout/circuit breaker)
  → OutputValidator + sanitizer + proposal validator
  → transaction T2: lưu assistant message + exact context snapshot + usage + proposals
  → response DTO
```

Luồng approval:

```text
Approve endpoint
  → CurrentUser + load proposal theo (id,userId)
  → verify status/version/hash/expiry/allowed action
  → atomic claim PROPOSED→EXECUTING
  → gọi command facade của module đích với expectedVersion
  → lưu EXECUTED + sanitized result hoặc FAILED
```

Context Builder dùng các API hiện có/được bổ sung tối thiểu theo mẫu D197. API đọc mới phải trả DTO đã tối thiểu hóa, không trả entity và có test ownership. Không sửa REST/sync contract hiện có chỉ để phục vụ AI.

---

## 6. Mô hình dữ liệu đề xuất

### 6.1. V12 — phần MVP

DDL dưới đây là thiết kế tham chiếu; migration thật chỉ được viết sau khi D260–D299 được duyệt.

```sql
CREATE TABLE ai_user_preferences (
  user_id uuid PRIMARY KEY REFERENCES users(id),
  allow_task_calendar boolean NOT NULL DEFAULT false,
  allow_habit boolean NOT NULL DEFAULT false,
  allow_goal boolean NOT NULL DEFAULT false,
  allow_notes boolean NOT NULL DEFAULT false,
  allow_finance boolean NOT NULL DEFAULT false,
  policy_version varchar(20) NOT NULL,
  accepted_provider_terms_at timestamptz,
  created_at timestamptz NOT NULL,
  updated_at timestamptz NOT NULL
);

CREATE TABLE ai_conversations (
  id uuid PRIMARY KEY,
  user_id uuid NOT NULL REFERENCES users(id),
  title varchar(200) NOT NULL,
  status varchar(10) NOT NULL CHECK (status IN ('ACTIVE','ARCHIVED')),
  last_message_at timestamptz NOT NULL,
  expires_at timestamptz NOT NULL,
  created_at timestamptz NOT NULL,
  updated_at timestamptz NOT NULL
);
CREATE INDEX ix_ai_conversations_user_last
  ON ai_conversations(user_id, last_message_at DESC, id DESC);

CREATE TABLE ai_messages (
  id uuid PRIMARY KEY,
  user_id uuid NOT NULL REFERENCES users(id),
  conversation_id uuid NOT NULL REFERENCES ai_conversations(id) ON DELETE CASCADE,
  role varchar(9) NOT NULL CHECK (role IN ('USER','ASSISTANT')),
  kind varchar(20) NOT NULL,
  content text NOT NULL CHECK (char_length(content) <= 16000),
  structured_payload jsonb,
  context_snapshot jsonb,
  prompt_version varchar(40),
  provider varchar(30),
  model varchar(80),
  created_at timestamptz NOT NULL
);
CREATE INDEX ix_ai_messages_conversation
  ON ai_messages(user_id, conversation_id, created_at, id);

CREATE TABLE ai_usage_logs (
  id uuid PRIMARY KEY,
  user_id uuid NOT NULL REFERENCES users(id),
  conversation_id uuid REFERENCES ai_conversations(id) ON DELETE SET NULL,
  message_id uuid REFERENCES ai_messages(id) ON DELETE SET NULL,
  provider varchar(30) NOT NULL,
  model varchar(80) NOT NULL,
  input_tokens bigint NOT NULL CHECK (input_tokens >= 0),
  cached_input_tokens bigint NOT NULL DEFAULT 0 CHECK (cached_input_tokens >= 0),
  output_tokens bigint NOT NULL CHECK (output_tokens >= 0),
  reasoning_tokens bigint NOT NULL DEFAULT 0 CHECK (reasoning_tokens >= 0),
  estimated_cost_microusd bigint NOT NULL CHECK (estimated_cost_microusd >= 0),
  latency_ms bigint NOT NULL CHECK (latency_ms >= 0),
  result_code varchar(40) NOT NULL,
  created_at timestamptz NOT NULL
);
CREATE INDEX ix_ai_usage_user_created
  ON ai_usage_logs(user_id, created_at DESC);

CREATE TABLE ai_action_proposals (
  id uuid PRIMARY KEY,
  user_id uuid NOT NULL REFERENCES users(id),
  conversation_id uuid REFERENCES ai_conversations(id) ON DELETE CASCADE,
  source_message_id uuid REFERENCES ai_messages(id) ON DELETE CASCADE,
  action_type varchar(40) NOT NULL,
  payload jsonb NOT NULL,
  expected_versions jsonb NOT NULL DEFAULT '{}',
  payload_hash char(64) NOT NULL,
  execution_key uuid NOT NULL UNIQUE,
  status varchar(20) NOT NULL CHECK (status IN
    ('PROPOSED','APPROVED','REJECTED','CANCELLED','EXPIRED','EXECUTING','EXECUTED','FAILED')),
  version bigint NOT NULL DEFAULT 1,
  expires_at timestamptz NOT NULL,
  approved_at timestamptz,
  executed_at timestamptz,
  result jsonb,
  failure_code varchar(50),
  created_at timestamptz NOT NULL,
  updated_at timestamptz NOT NULL
);
CREATE INDEX ix_ai_proposals_user_status
  ON ai_action_proposals(user_id, status, expires_at);
```

Ràng buộc bổ sung ở migration/test: JSON object hợp lệ và giới hạn byte ở service; mọi query scope `user_id`; child trước parent trong `UserDataCatalog`; không cascade từ dữ liệu nghiệp vụ sang AI bằng FK đa hình.

### 6.2. RAG/pgvector

Theo D292, chưa có DDL `ai_documents`/`ai_embeddings` trong V12. Addendum AIP5 phải chốt:

- embedding model, dimension và giá;
- extension/image PostgreSQL có `vector`;
- chunk size/overlap, content hash và re-index;
- HNSW/IVFFlat sau benchmark, không chọn theo cảm tính;
- backup/restore extension trên staging;
- deletion/consent revocation và INT-15;
- migration V13+ chỉ thêm mới.

---

## 7. Hợp đồng API đề xuất cho Flutter

### 7.1. Preferences, hội thoại và context

| Endpoint | Mục đích |
|---|---|
| `GET /api/v1/ai/preferences` | Consent, policy/provider notice version và feature flags khả dụng |
| `PUT /api/v1/ai/preferences` | Thay toàn bộ consent bằng DTO whitelist; không PATCH mơ hồ |
| `POST /api/v1/ai/conversations` | Tạo hội thoại |
| `GET /api/v1/ai/conversations?limit=&cursor=` | Danh sách cursor ổn định |
| `GET /api/v1/ai/conversations/{id}` | Chi tiết + trang message |
| `DELETE /api/v1/ai/conversations/{id}` | Xóa hội thoại và dữ liệu con của chính user |
| `POST /api/v1/ai/conversations/{id}/messages` | Chat/generation; request có `message`, `mode`, `contextDomains[]` là tập con của consent |
| `GET /api/v1/ai/messages/{id}/context` | Bản context snapshot chính xác đã gửi provider |
| `DELETE /api/v1/ai/data` | Xóa toàn bộ dữ liệu AI, reset consent về tắt |

### 7.2. Các use case có schema riêng

| Endpoint | Mục đích |
|---|---|
| `POST /api/v1/ai/plans/daily` | Kế hoạch ngày dạng structured response |
| `POST /api/v1/ai/plans/weekly` | Kế hoạch tuần |
| `POST /api/v1/ai/tasks/{id}/breakdown` | Danh sách Task độc lập đề xuất, không tự tạo |
| `POST /api/v1/ai/insights/habits` | Insight Habit chỉ đọc |
| `POST /api/v1/ai/insights/finance` | Insight Finance chỉ đọc, cần consent riêng và currency |
| `POST /api/v1/ai/notes/{id}/summary` | Tóm tắt một Note thuộc user |

### 7.3. Proposal, usage và limits

| Endpoint | Mục đích |
|---|---|
| `GET /api/v1/ai/action-proposals?status=&limit=&cursor=` | Danh sách proposal của user |
| `GET /api/v1/ai/action-proposals/{id}` | Diff chính xác và trạng thái |
| `POST /api/v1/ai/action-proposals/{id}/approve` | `{version,payloadHash}`; thực thi một lần |
| `POST /api/v1/ai/action-proposals/{id}/cancel` | Hủy proposal còn `PROPOSED` |
| `GET /api/v1/ai/usage?from=&to=` | Token/request/cost theo ngày, không nội dung |
| `GET /api/v1/ai/limits` | Quota đã dùng/còn lại, reset và kill-switch status |

### 7.4. Mã lỗi mới

| errorCode | HTTP | Ý nghĩa/UI |
|---|---:|---|
| `AI_DISABLED` | 503 | AI đang tắt; chức năng khác vẫn dùng được |
| `AI_CONSENT_REQUIRED` | 403 | Hiển thị miền cần bật; không tự bật |
| `AI_CONTEXT_DOMAIN_NOT_ALLOWED` | 422 | Request xin miền ngoài consent/feature flag |
| `AI_CONVERSATION_NOT_FOUND` | 404 | Không tồn tại hoặc của user khác |
| `AI_MESSAGE_TOO_LARGE` | 413 | Rút ngắn nội dung/context |
| `AI_RATE_LIMITED` | 429 | Thử lại theo `Retry-After` |
| `AI_QUOTA_EXCEEDED` | 429 | Hết quota user đến kỳ reset |
| `AI_BUDGET_EXCEEDED` | 503 | Ngân sách hệ thống đã chạm hard stop |
| `AI_PROVIDER_UNAVAILABLE` | 503 | Provider timeout/circuit open; thử lại sau |
| `AI_RESPONSE_INVALID` | 502 | Provider trả refusal/incomplete/schema sai; không tạo proposal |
| `AI_ACTION_NOT_FOUND` | 404 | Proposal không tồn tại/của user khác |
| `AI_ACTION_EXPIRED` | 409 | Proposal hết hạn |
| `AI_ACTION_STATE_CONFLICT` | 409 | Đã duyệt/hủy/thực thi hoặc version sai |
| `AI_ACTION_FORBIDDEN` | 422 | Action type/payload không nằm allowlist |
| `AI_ACTION_VERSION_CONFLICT` | 409 | Dữ liệu đích đã đổi; tạo proposal mới sau khi đọc lại |

Fixture công bố ở AIP1 tối thiểu: `ai-preferences.response.json`, `ai-chat.request.json`, `ai-chat.response.json`, `ai-context.response.json`, `ai-plan.response.json`, `ai-action-proposal.response.json`, `ai-usage.response.json`, `ai-error.response.json`. Không fixture nào chứa dữ liệu thật.

---

## 8. Bảo mật, an toàn và quyền riêng tư

1. Tất cả route cần JWT; `userId` chỉ từ `CurrentUser`; IDOR trả 404 giống không tồn tại.
2. DTO whitelist; từ chối/ignore trường `userId`, provider, model, cost, status, executionKey, context snapshot và tool result từ client.
3. Context API chỉ trả snapshot của message cùng user; không trả prompt hệ thống/provider raw response.
4. Consent được kiểm cả trước Context Builder và ngay trước provider call; preference cache nếu có phải invalid ngay sau commit.
5. Finance/Notes không được đưa vào context chung chỉ vì prompt nhắc đến chúng.
6. Output sanitizer chạy trước persistence và response; raw provider body chỉ tồn tại trong bộ nhớ có giới hạn và không log.
7. API key `OPENAI_API_KEY` chỉ ở secret store; hỗ trợ hai project/key trong runbook để xoay; không đưa vào image.
8. Không dùng web search, hosted file/vector store, code interpreter, remote MCP hoặc tool ngoài ở MVP.
9. Proposal luôn cho người dùng thấy action type, từng trường trước/sau, target, expectedVersion, expiry và cảnh báo; Finance action không bao giờ có nút duyệt.
10. Privacy notice phải nêu dữ liệu đã chọn, nhà cung cấp, mục đích, retention LifeOS, retention provider, quyền thu hồi/xóa/xuất và rằng AI có thể sai.

---

## 9. Kế hoạch kiểm thử AI-xx

CI không gọi OpenAI thật. Test provider giả phải có clock và response xác định.

| ID | Kịch bản | Kỳ vọng |
|---|---|---|
| AI-01 | Preferences mới | Mọi miền tắt, không gọi provider |
| AI-02 | Bật từng miền và đọc lại | Chỉ trường whitelist đổi, có policy/version/time |
| AI-03 | Consent matrix × Task/Calendar/Habit/Goal/Notes/Finance | Mỗi miền tắt chặn đúng; Finance độc lập |
| AI-04 | Thu hồi consent ngay trước request | Context không còn miền đó; provider không thấy dữ liệu |
| AI-05 | User B dùng conversation/message/proposal của A | 404, không khác id lạ |
| AI-06 | Mass assignment preferences/chat/proposal | `userId`, model, status, cost, context bị bỏ/từ chối |
| AI-07 | Golden prompt/context | Payload provider khớp chính xác snapshot và tập trường cho phép |
| AI-08 | Cấm secret/identity | Không email/token/device/push/password/audit/outbox trong payload |
| AI-09 | Finance consent tắt/bật | Tắt: không có số tiền; bật: đúng currency, trường tối thiểu |
| AI-10 | Prompt injection trong Note/Task | Không đổi instruction, không lộ miền khác/system prompt, không tool call |
| AI-11 | Model trả ID ngoài allowlist | Backend chặn `AI_RESPONSE_INVALID`, không proposal |
| AI-12 | Structured output đúng schema | Parse/validate/lưu đúng |
| AI-13 | Malformed/extra field/depth/oversize | Chặn, không action, usage ghi failure |
| AI-14 | HTML/script/javascript URL/system-prompt leak | Sanitizer loại/chặn |
| AI-15 | Refusal/incomplete/content filter | Không coi thành công, UI nhận mã ổn định |
| AI-16 | Quota ngày/tháng và 5/phút | Request vượt không gọi provider, có `Retry-After`/reset |
| AI-17 | Cost integer micro-USD | Khớp vector giá, không `double`, không log content |
| AI-18 | Kill switch/80%/100% | Alert đúng; hard stop request mới; core API vẫn xanh |
| AI-19 | Timeout/429/5xx/circuit | Retry giới hạn; breaker; không giữ transaction/network trong transaction |
| AI-20 | Concurrent limit user/global | Không vượt semaphore, không deadlock/leak permit |
| AI-21 | Proposal hết hạn/hủy | Không thực thi |
| AI-22 | Duyệt hai lần và 20 approval đồng thời | Đúng một execution; request còn lại cùng kết quả/conflict ổn định |
| AI-23 | Payload hash/version bị sửa | Không thực thi |
| AI-24 | Target đổi version sau proposal | `AI_ACTION_VERSION_CONFLICT`, dữ liệu không bị ghi đè |
| AI-25 | Action Finance/delete/complete/settings | Luôn bị cấm kể cả model trả schema hợp lệ |
| AI-26 | Allowed Task action | Gọi command facade; ownership/version/ChangeRecorder và sync contract giữ nguyên |
| AI-27 | Crash ở EXECUTING | Recovery không nhân đôi hành động |
| AI-28 | Xóa một hội thoại | Message/context/proposal con sạch; usage xử lý theo policy |
| AI-29 | `DELETE /ai/data` | Mọi bảng AI của user sạch, consent reset; dữ liệu nghiệp vụ còn nguyên |
| AI-30 | Xóa tài khoản/INT-15 | Mọi bảng `user_id` AI có trong export/purge catalog |
| AI-31 | Export dữ liệu | Đủ consent/chat/context/proposal/usage; không secret/provider header |
| AI-32 | Retention 90 ngày | Xóa theo lô/idempotent; user khác không ảnh hưởng |
| AI-33 | Sensitive log/metric | Không prompt/response/context/title/note/money/API key; nhãn hữu hạn |
| AI-34 | ArchUnit A15 | AI không import repository/entity module khác; controller/service/transaction đúng |
| AI-35 | OpenAPI + fixtures | Export ổn định, Java serialize đúng fixture, không breaking route cũ |
| AI-36 | Provider fake only | Test thất bại nếu CI cố ra Internet hoặc cần `OPENAI_API_KEY` |
| AI-37 | Performance | Context build có giới hạn; p95 chat fake-provider và DB query ghi báo cáo, không connection leak |
| AI-38 | Quality eval nhỏ | 30–50 prompt Việt giả lập, rubric factual/grounded/action safety; báo cáo không chặn CI |
| AI-39 | RAG deletion (khi AIP5 được duyệt) | Note/consent/account deletion xóa document/chunk/embedding |
| AI-40 | Restore backup có bảng AI | Restore staging và truy vấn/constraint/index/extension (nếu có) đúng |

Test đồng thời dùng latch/barrier và lặp ≥ 20; không `Thread.sleep`. Test provider thật là smoke thủ công opt-in, dữ liệu giả, budget nhỏ, ghi rõ `CHƯA CHẠY` nếu không có key.

---

## 10. Các bước triển khai sau khi được duyệt

| Bước | Nội dung | Điều kiện hoàn thành |
|---|---|---|
| AI1 | Công bố OpenAPI + fixture AI + error codes | Contract test xanh; Flutter có thể code không chờ backend |
| AI2 | V12, catalog export/purge, preferences, usage/quota, fake provider | Migration sạch/nâng cấp; AI-01..09, 16..18, 29..35 |
| AI3 | Chat read-only + exact context + provider adapter | AI-07..20; không network trong transaction |
| AI4 | Plans, breakdown, Habit/Goal/Finance insights, Note summary | Consent/schema/injection test theo từng mode |
| AI5 | RAG/pgvector addendum | Chỉ khi O-AI-06 được duyệt; V13+, restore/benchmark/deletion xanh |
| AI6 | Action proposals | Feature flag tắt mặc định; AI-21..27 xanh 3 lượt |
| AI7 | Hardening, load/eval, docs/privacy/runbook | `mvn clean verify` xanh 3 lượt; báo cáo cuối trung thực |

Mỗi bước phải cập nhật `docs/review/DEPLOY_AI_PROGRESS.md`, OpenAPI/fixture và tài liệu liên quan trong phạm vi sở hữu. Không sửa mobile.

---

## 11. Definition of Done Phase 8

- D260–D299 đã được duyệt rõ ràng hoặc có ngoại lệ ghi lại.
- Mọi FR-AI có test AI-xx ánh xạ; test cũ vẫn xanh; 0 `@Disabled`.
- OpenAPI/fixture AI công bố và không làm hỏng hợp đồng hiện có.
- Consent mặc định tắt, context disclosure đúng, IDOR/mass assignment/injection/schema/action tests xanh.
- Không provider call trong transaction; provider giả trong CI; secret không vào repo/log/image/mobile.
- Bảng AI nằm trong export/purge catalog; xóa AI/tài khoản sạch; restore drill bao gồm AI.
- Quota, cost, kill switch, metric/alert/runbook hoạt động; AI lỗi không ảnh hưởng core.
- RAG/action chỉ bật đúng feature flag và điều kiện rollout.

---

## 12. `[RESOLVED]` O-AI-01..08

Người dùng đã duyệt ngày 2026-11-10 toàn bộ phương án khuyến nghị dưới đây.

| ID | Quyết định | Khuyến nghị |
|---|---|---|
| O-AI-01 | Model mặc định: `gpt-6.1-sol` hay `gpt-6-luna` | **Sol** cho chat/planning; chỉ đổi sau eval chất lượng/chi phí |
| O-AI-02 | Retention hội thoại 30, 90 hay 365 ngày | **90 ngày**, cho xóa ngay |
| O-AI-03 | Có yêu cầu ZDR trước khi bật Notes/Finance production không | **Có**; beta Task/Habit/Goal vẫn phải disclosure retention 30 ngày |
| O-AI-04 | Hard budget beta toàn hệ thống | **25 USD/tháng**, cảnh báo 80%, dừng 100% |
| O-AI-05 | Bật action proposal trong lần phát hành AI đầu hay để sau | **Để sau**, chỉ chat/planning read-only trước |
| O-AI-06 | RAG/pgvector trong Phase 8 hay hoãn addendum | **Hoãn** đến khi chat có số đo và restore staging ổn định |
| O-AI-07 | Cho phép action ngoài ba loại Task ở D278 | **Không** trong bản đầu |
| O-AI-08 | Chấp nhận non-streaming MVP hay yêu cầu SSE ngay | **Non-streaming**; đo p95/UX rồi quyết |

Quyết định này là đầu vào bắt buộc cho AIP1–AIP7; thay đổi cần một quyết định mới được duyệt.

