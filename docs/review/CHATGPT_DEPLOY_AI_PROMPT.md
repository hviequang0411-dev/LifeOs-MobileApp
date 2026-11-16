# Prompt giao việc cho ChatGPT — Deploy (hạ tầng, CI/CD, phát hành) + AI Assistant (Phase 8)

Dán phần dưới dòng `---` vào ChatGPT (ưu tiên môi trường có quyền đọc/ghi repo và chạy lệnh, ví dụ Codex/agent mode) và **đính kèm các file ở mục 3**. Có **đúng một điểm dừng**: sau khi nộp hai tài liệu kế hoạch (Phần A). Sau khi người dùng duyệt, làm liên tục (Phần B).

> Ứng dụng **Flutter + UI** do agent khác (Antigravity) làm — xem `docs/review/ANTIGRAVITY_FLUTTER_UI_PROMPT.md`. Ranh giới sở hữu ở mục 2.

---

Bạn là kỹ sư backend/DevOps cấp cao kiêm chuyên gia an toàn AI ứng dụng. **LifeOS** là ứng dụng quản lý cuộc sống cá nhân offline-first (Flutter + SQLite/Drift; backend Java 21, Spring Boot Modular Monolith; PostgreSQL 17; Flyway; Maven). Backend Phase 1–7 đã hoàn thiện và được kiểm thử (`docs/review/BACKEND_FINAL_REPORT.md`: 12 module, 29 `entityType` đồng bộ, 847 test xanh). Nhiệm vụ của bạn gồm **hai mảng**:

1. **Deploy:** đưa hệ thống lên môi trường chạy thật, an toàn và có thể vận hành bởi một người; CI/CD; giám sát; sao lưu/khôi phục; chuẩn bị phát hành ứng dụng lên cửa hàng.
2. **AI Assistant (Phase 8):** thiết kế và cài đặt module `ai` ở backend theo `PRODUCT_CONTEXT.md` Module 13 và §11, **đặt quyền riêng tư và kiểm soát của người dùng lên trên tính năng**.

## 1. Quy tắc nền

- Tài liệu trong repo là **nguồn sự thật**. `[CONFIRMED]` bắt buộc; `[PROPOSED]` chưa duyệt; `[OPEN]` là câu hỏi. **Không bịa quy tắc.** Phần AI **chưa có đặc tả** — bạn phải viết (Phần A) và chờ duyệt trước khi cài đặt. Quyết định chi tiết khác mà tài liệu không nêu: chọn phương án đơn giản, an toàn, nhất quán, ghi vào nhật ký với nhãn `[PROPOSED]`.
- **Không được tuyên bố đã kiểm thử điều bạn không chạy.** Nếu môi trường của bạn không chạy được `mvn`/Docker/Testcontainers: viết mã + test đầy đủ, đánh dấu rõ **"CHƯA CHẠY"** trong nhật ký và báo cáo, và liệt kê lệnh để người dùng/agent khác chạy. Không nới test để "qua".
- Nếu bạn **không có quyền ghi vào repo**: xuất từng file đầy đủ (đường dẫn + nội dung) để người dùng chép; không xuất diff một phần.
- Giữ nguyên bất biến backend: `@Transactional` chỉ ở `..service..`; module chỉ phụ thuộc nhau qua `api/`; ArchUnit A1–A14 xanh; thứ tự khóa D86 (khóa/flush nghiệp vụ trước, `ChangeRecorder.record` cuối); cascade `mutation_id` NULL; không gọi mạng ngoài trong transaction DB; không dữ liệu nhạy cảm trong log/metric/lỗi; IDOR và mass-assignment test cho mọi route mới; migration chỉ thêm mới (V12+), không sửa V1–V11; test đồng thời lặp ≥ 20 lần, không `Thread.sleep`.

## 2. Ranh giới sở hữu (nhiều agent cùng làm trong repo)

| Bạn sở hữu (được ghi) | Chỉ đọc | Của agent khác — không đụng |
|---|---|---|
| `deploy/`, `ops/`, Dockerfile, compose (prod/staging), `ci/*` (trừ `ci/mobile-ci.yml`), workflow deploy/release, `docs/ops/*`, module `ai` trong `backend/` (+ migration V12+, test), `docs/phases/PHASE_8_AI_ASSISTANT.md`, `docs/engineering/DEPLOY_AI_MASTER_PLAN.md`, `docs/review/DEPLOY_AI_*`, `docs/api/openapi.yaml` + `docs/api/fixtures/ai-*.json` (phần AI) | Mọi module backend khác (chỉ sửa khi **tối thiểu** để cung cấp `*Api` cho AI, theo mục 4.5), `docs/engineering/*` và `docs/phases/*` khác | Thư mục ứng dụng Flutter (`mobile/`), `ci/mobile-ci.yml`, `docs/engineering/MOBILE_MASTER_PLAN.md`, `docs/review/MOBILE_*` |

Bạn phải **công bố hợp đồng AI sớm** (OpenAPI + fixture) để agent Flutter làm màn hình AI; ghi mốc đó vào `DEPLOY_AI_MASTER_PLAN.md`.

## 3. Bước 0 — tài liệu cần đọc (đính kèm hoặc mở trong repo)

| # | File | Vai trò |
|---|---|---|
| 1 | `AGENTS.md`, `docs/review/BACKEND_FINAL_REPORT.md`, `docs/engineering/BACKEND_MASTER_PLAN.md` | Trạng thái, rủi ro tự nhận (§8), hoãn (§10), câu hỏi mở (§12), quy tắc tự chủ |
| 2 | `docs/product/PRODUCT_CONTEXT.md` | **Module 13 (AI)**, §11 (AI Backend Design, Safety & Privacy), §8.1 (bảng AI định hướng), Phase 8, mục AI trong phần "chưa quyết" |
| 3 | `docs/architecture/SYSTEM_ARCHITECTURE.md`, `DATABASE_DESIGN.md`, `BACKEND_SKELETON_PLAN.md` | Kiến trúc, module, jobs (§3.12), triển khai |
| 4 | `docs/engineering/SECURITY_RULES.md`, `JAVA_SPRING_RULES.md`, `DATABASE_RULES.md`, `API_CONVENTIONS.md`, `TESTING_RULES.md` | Quy tắc bắt buộc |
| 5 | `docs/phases/PHASE_7_INTELLIGENCE_ANALYTICS.md` (D197: `*QueryApi`/`*StatsApi`; D204–D205: xuất/xóa tài khoản, `UserDataCatalog`) | Khuôn mẫu truy cập dữ liệu liên module và danh mục xuất/xóa |
| 6 | `docs/ops/ALERTS.md`, `docker-compose.yml`, `application*.yml`, `ci/backend-ci.yml` | Hạ tầng hiện có |
| 7 | `docs/api/openapi.yaml` + `docs/api/fixtures/*` | Hợp đồng hiện có |

Không đọc được file nào: **dừng và nêu tên file**; không suy đoán nội dung.

## Phần A — KẾ HOẠCH (điểm dừng duy nhất)

Chưa viết mã. Nộp **hai tài liệu** rồi dừng.

### A1. `docs/phases/PHASE_8_AI_ASSISTANT.md` — đặc tả AI (mọi quyết định mới ghi `[PROPOSED]`, đánh số D260–D299)

Theo cấu trúc của các đặc tả `docs/phases/PHASE_*.md` (mục tiêu/phạm vi, yêu cầu FR-AI-*, use case, quy tắc nghiệp vụ D-*, mô hình dữ liệu + DDL Flyway V12+, API, bảo mật/riêng tư, kế hoạch test AI-xx, bước triển khai AI1…, quyết định + `[OPEN]` kèm khuyến nghị). Bắt buộc giải quyết các mục "chưa quyết" về AI của `PRODUCT_CONTEXT.md`: **nhà cung cấp (OpenAI qua `LLMProvider`/`OpenAIProvider`), chọn mô hình, hạn mức sử dụng, quyền ngữ cảnh, thời gian lưu hội thoại, lập chỉ mục RAG, UX duyệt hành động, xóa dữ liệu AI, kiểm soát chi phí**. Nội dung phải có:

1. **Phạm vi bản đầu (MVP-AI) và các bước tiếp theo.** Đề xuất thứ tự giá trị/rủi ro: (1) chat có ngữ cảnh chỉ đọc + lịch sử + dùng/chi phí; (2) kế hoạch ngày/tuần, chia nhỏ Task, gợi ý ưu tiên/lịch (đề xuất, không ghi); (3) phân tích Habit/Finance/xu hướng (chỉ đọc, có quyền riêng); (4) tóm tắt ghi chú + tìm kiếm ngữ nghĩa/RAG; (5) hành động có duyệt (tool calling). Nêu cái nào để sau.
2. **Quyền riêng tư và đồng ý (consent) — trọng tâm:** `ai_user_preferences` với **đồng ý theo từng miền dữ liệu** (Task/Calendar, Habit, Goal, Notes, **Finance riêng**), **mặc định tắt toàn bộ**; xem và thu hồi bất kỳ lúc nào; Context Builder **chỉ** lấy miền được phép, tối thiểu hóa dữ liệu (trường cần thiết, cắt độ dài, số tiền chỉ khi có quyền Finance), không gửi mật khẩu/token/email/thiết bị/FCM; **hiển thị cho người dùng chính xác dữ liệu nào đã gửi** (xem "ngữ cảnh đã dùng" cho mỗi phản hồi); nhà cung cấp không dùng dữ liệu để huấn luyện (cấu hình/hợp đồng, ghi rõ giả định).
3. **An toàn:** coi mọi nội dung người dùng (ghi chú, tiêu đề, mô tả) là **không tin cậy** (prompt injection): tách rõ chỉ thị hệ thống với dữ liệu, không để nội dung dữ liệu kích hoạt công cụ; đầu ra có cấu trúc (JSON schema) được **backend xác thực** trước khi hiển thị/đề xuất; không để LLM chọn `userId`/id thực thể ngoài tập đã cấp; lọc đầu ra (không lộ system prompt, không URL/HTML nguy hiểm); giới hạn độ dài/độ sâu; chống lạm dụng và leo thang chi phí.
4. **Hành động (tool calling):** mọi hành động ghi **phải** qua `ai_action_proposals` → backend validate → người dùng duyệt (kèm hiển thị đúng thay đổi sẽ xảy ra) → thực thi bằng **các service nghiệp vụ hiện có** với danh tính người dùng (không truy cập DB trực tiếp, không bỏ qua kiểm tra sở hữu/version) → ghi kết quả. Đề xuất có **hạn dùng**, **idempotent** (băm payload + trạng thái), một lần duyệt chỉ thực thi một lần, hủy được. **Mặc định tuyệt đối: AI không tạo/sửa/xóa giao dịch tài chính hay số dư** (chỉ phân tích/gợi ý); nêu danh sách hành động được phép ở bản đầu (ví dụ tạo Task, chia nhỏ thành subtask nếu mô hình có, đề xuất thời gian) và hành động bị cấm.
5. **Dữ liệu AI:** DDL cho `ai_conversations`, `ai_messages`, `ai_usage_logs`, `ai_user_preferences`, `ai_action_proposals`, (nếu có RAG) `ai_documents`, `ai_embeddings` — nêu rõ **có đồng bộ qua sync không** (đề xuất: **không**, dữ liệu phía server; AI cần online), thời gian lưu hội thoại và cách xóa (xóa từng hội thoại, xóa toàn bộ), **tích hợp vào xuất dữ liệu và xóa tài khoản** (`UserDataCatalog`, D204–D205: bảng AI phải có trong danh mục xuất/xóa; test INT-15 kiểu "mọi bảng có `user_id`" phải xanh). Embedding: lưu ở đâu (`pgvector` trong PostgreSQL — nêu hệ quả với image DB, backup, deploy — hay hoãn RAG), cách đánh chỉ mục lại khi ghi chú đổi/xóa, **xóa embedding khi xóa ghi chú/tài khoản**.
6. **Chi phí và hạn mức:** ngân sách và giới hạn theo người dùng/ngày/tháng (token, số yêu cầu, độ dài ngữ cảnh), giới hạn tần suất (`SECURITY_RULES` §6), công tắc ngắt khẩn cấp toàn hệ thống, `ai_usage_logs` (token, độ trễ, chi phí ước tính, mã kết quả, **không lưu nội dung nhạy cảm trong log**), metric và cảnh báo; hành vi khi vượt hạn mức/nhà cung cấp lỗi (suy giảm êm, không ảnh hưởng chức năng cốt lõi).
7. **Kiến trúc:** theo §11.1 (controller/service/provider/tool/…), chỉ truy cập dữ liệu module khác qua các `*Api` mới/hiện có (khuôn mẫu D197), ArchUnit mới cho `ai`; `LLMProvider` trừu tượng (thay được nhà cung cấp), **khóa API chỉ ở server** (biến môi trường/secret manager, không log, xoay được), timeout/retry có giới hạn/circuit breaker, **không gọi nhà cung cấp trong transaction DB**; streaming phản hồi (SSE) hay không — quyết định và hệ quả; tải đồng thời và hàng đợi.
8. **API (`/api/v1/ai/...`) và hợp đồng cho Flutter:** chat, hội thoại, ngữ cảnh đã dùng, kế hoạch, đề xuất/duyệt/hủy, tùy chọn/consent, dùng và hạn mức, xóa dữ liệu AI; mã lỗi `AI_*`; fixture mẫu; quy ước lỗi/hết hạn mức để UI hiển thị.
9. **Kế hoạch kiểm thử AI-xx:** nhà cung cấp **giả (WireMock/stub)** — CI **không** gọi nhà cung cấp thật; IDOR/mass-assignment; consent chặn từng miền (kể cả Finance) và thu hồi có hiệu lực ngay; "golden prompt" kiểm chính xác dữ liệu nào vào prompt; prompt injection (nội dung ghi chú cố điều khiển công cụ/lộ dữ liệu miền khác); xác thực schema đầu ra (đầu ra sai định dạng/độc hại bị chặn); đề xuất: hết hạn, duyệt hai lần, thực thi đồng thời, vượt `version`, cấm hành động tài chính; hạn mức/chi phí/ngắt khẩn cấp; xóa hội thoại/tài khoản xóa sạch kể cả embedding; log/metric không chứa nội dung; không gọi nhà cung cấp trong transaction; hiệu năng và độ trễ; chất lượng đầu ra có bộ đánh giá nhỏ (không chặn CI).

### A2. `docs/engineering/DEPLOY_AI_MASTER_PLAN.md` — kế hoạch tổng thể deploy + triển khai AI

Theo cấu trúc `BACKEND_MASTER_PLAN.md` (gói `DWP1…DWPn`, quyết định mặc định cho `[OPEN]`, thay đổi hợp đồng được phép, điều kiện dừng, chuẩn hoàn thành), gồm:

1. **Hạ tầng và môi trường:** dev/staging/production; **ít nhất 2 phương án hosting backend** (ví dụ VPS + Docker Compose; PaaS như Render; dịch vụ container của nhà cung cấp lớn) với **chi phí ước tính, ưu/nhược, một đề xuất có lý do** (một instance, ngân sách nhỏ, một người vận hành); PostgreSQL managed hay tự chạy; hệ quả của việc **chỉ một instance** (rate limiter/khóa HMAC snapshot/`EXPORT_DIR` — `BACKEND_FINAL_REPORT.md` §8) và điều kiện khi cần scale.
2. **Đóng gói và cấu hình:** `Dockerfile` production (không root, `TZ=UTC`, tài nguyên giới hạn, health/readiness), compose **profile prod** (không dùng khóa mặc định; app **không khởi động** nếu thiếu secret production), biến môi trường/secret (JWT, `NOTIFICATION_UNSUBSCRIBE_SECRET`, SMTP, FCM, `EXPORT_DIR`, DB, khóa AI), TLS/domain, reverse proxy và **`X-Forwarded-For` tin cậy đúng** cho rate limit, giới hạn body (1 MB sync, 192 KB ghi chú), CORS (nếu cần), headers bảo mật.
3. **Dữ liệu:** migration an toàn khi nâng cấp (thứ tự, quay lui), **sao lưu PostgreSQL tự động + thử khôi phục thật** (RPO/RTO đề xuất, kết quả thử ghi trong báo cáo), `pgvector` nếu RAG, dọn tệp xuất mồ côi, vòng đời dữ liệu.
4. **CI/CD:** backend (`ci/backend-ci.yml`), job nightly (mô phỏng đa thiết bị đầy đủ `-Dlifeos.sync.simulation.full=true` + `BackendBenchmark`), triển khai có **rollback**, quản lý secret trong CI, quét phụ thuộc/image; workflow ghi vào `ci/`/`deploy/` và nêu rõ phần người dùng phải chép vào `.github/workflows/` nếu bạn không được phép ghi.
5. **Quan sát và vận hành:** Prometheus/Grafana (`ops/`), dashboard và cảnh báo từ `docs/ops/ALERTS.md` (bổ sung gauge `lifeos.job.last_success{job}`, metric/cảnh báo AI: chi phí, lỗi nhà cung cấp, độ trễ, hạn mức), nhật ký có bảo vệ dữ liệu, xoay secret, runbook (`docs/ops/`): triển khai, rollback, khôi phục, sự cố thường gặp, xử lý khi nhà cung cấp AI lỗi/hết ngân sách.
6. **Phát hành ứng dụng (khi có dự án Flutter của agent kia):** pipeline build Android (`aab`, keystore, Google Play kiểm thử nội bộ → kín → production) và iOS (chứng chỉ, TestFlight; **cần macOS runner/Mac của người dùng**), cấu hình FCM/APNs, đánh số phiên bản và chính sách `minSupportedClientVersion`, **bản nháp chính sách quyền riêng tư và mô tả cửa hàng** (nêu rõ dữ liệu AI, quyền đồng ý, nhà cung cấp AI, xóa dữ liệu), tài sản cửa hàng cần người dùng cung cấp.
7. **Gói AI `AIP1…`** theo A1 (mốc **công bố hợp đồng OpenAPI/fixture AI** sớm nhất có thể), thứ tự phụ thuộc với `DWP`.
8. **Việc chỉ con người làm được** (tên miền, hosting, tài khoản OpenAI và ngân sách/giới hạn chi tiêu, Firebase/FCM, Apple Developer, Google Play, SMTP production) — khi nào cần và cách không bị chặn (nhà cung cấp AI giả, mailpit, môi trường cục bộ).
9. **Quyết định mặc định đề xuất** cho các Open Questions ở `BACKEND_FINAL_REPORT.md` §12 liên quan hạ tầng (kho tệp xuất, `TZ=UTC`, CI nightly, gauge job), rủi ro, ước lượng, **quyết định cần người dùng chọn** (tối đa 10, mỗi mục một phương án khuyến nghị, trả lời một dòng).

**Khi xong A1 và A2: DỪNG**, thông báo người dùng, chờ câu duyệt (người dùng có thể duyệt hạ tầng và AI độc lập). Không viết mã/hạ tầng thật trước khi được duyệt; được phép tạo thư mục rỗng/nháp.

## Phần B — THỰC THI (sau khi người dùng duyệt)

- **Quy tắc tự chủ y hệt `BACKEND_MASTER_PLAN.md` §3 và §6:** làm liên tục, không xin duyệt giữa chừng; quyết định phát sinh ghi `docs/review/DEPLOY_AI_PROGRESS.md` (nhãn `[PROPOSED]`); cập nhật nhật ký sau mỗi gói (trạng thái, số test, thay đổi, quyết định, việc hoãn, **cái gì CHƯA CHẠY**); **chỉ dừng** khi tài liệu mâu thuẫn không giải quyết được, cần đổi hợp đồng `[CONFIRMED]` ngoài kế hoạch đã duyệt, quyết định không hoàn tác mà hai hướng đều hợp lý, test bắt buộc đỏ sau 3 lần sửa gốc, hoặc thiếu việc chỉ con người làm được mà không đi vòng được.
- **Chạm module khác để phục vụ AI:** chỉ thêm `*Api` đọc (hoặc phương thức gọi service lệnh đã có) tối thiểu, kèm test; không đổi hợp đồng REST/sync hiện có; `mvn clean verify` (847 test) **phải luôn xanh**; mọi test cũ bị sửa phải nêu lý do.
- **AI luôn thua quyền người dùng:** khi mâu thuẫn giữa "AI hữu ích hơn" và đồng ý/riêng tư/an toàn tiền, chọn phía an toàn và ghi vào nhật ký.
- Hạ tầng: mọi script/cấu hình phải **chạy thử được từ đầu** (từ máy sạch) hoặc ghi rõ chưa thử; không để secret thật trong repo; không đưa khóa AI vào image/log/ứng dụng di động.
- Mỗi gói xong khi: kiểm thử liên quan xanh (hoặc ghi rõ "CHƯA CHẠY" + lệnh), tài liệu liên quan cập nhật (`DATABASE_DESIGN`, `API_CONVENTIONS`, OpenAPI + fixture, `docs/ops`, `AGENTS.md` mục tương ứng).

## Bàn giao cuối (một báo cáo duy nhất)

Viết **`docs/review/DEPLOY_AI_FINAL_REPORT.md`**:
1. Tóm tắt, bảng gói (test, thời gian, **CHƯA CHẠY nếu có**), danh sách migration V12+.
2. Hạ tầng đã dựng/viết, cách triển khai từ máy sạch, **kết quả thử khôi phục sao lưu**, runbook, danh sách secret cần cung cấp, chi phí vận hành ước tính hàng tháng (hạ tầng + AI theo giả định sử dụng).
3. AI: bản đồ yêu cầu (FR-AI-*) → test (AI-xx), danh sách endpoint/fixture đã công bố cho Flutter, ma trận consent × miền dữ liệu, hành động được phép/cấm, bằng chứng chống prompt injection, số đo chi phí/độ trễ (với nhà cung cấp giả), tích hợp xuất/xóa dữ liệu.
4. Thay đổi hợp đồng đã thực hiện, test cũ đã sửa, quyết định `[PROPOSED]`, rủi ro, điểm cần người xem kỹ, việc hoãn, việc đang chờ con người.
