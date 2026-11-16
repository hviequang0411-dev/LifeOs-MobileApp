# Lời duyệt P-DWP và lệnh triển khai AIP2+ (`CHATGPT_AIP2_GO_PROMPT.md`)

> Gửi nguyên văn cho ChatGPT/Codex. Tiếp nối `DEPLOY_AI_FINAL_REPORT.md` (trạng thái MỘT PHẦN — NO-GO, dừng đúng quy trình).

---

## 1. Người dùng đã quyết

- **Duyệt `P-DWP2-01..P-DWP7-03` nguyên trạng** → chuyển các mục này thành `[CONFIRMED]` (2026-11-11) trong tài liệu liên quan.
- **Review backend đã có kết quả; các sửa lỗi backend được người dùng HOÃN** (xem `docs/review/CLAUDE_CODE_BACKEND_FIX_PROMPT.md`, trạng thái HOÃN) → không ai sửa `backend/` song song nữa. **Điều kiện chờ của AIP2 được gỡ: bắt đầu AIP2 → AIP3 → AIP4 → AIP7 theo `DEPLOY_AI_MASTER_PLAN.md`.** AIP5 (RAG) và AIP6 (action proposal) vẫn HOÃN theo O-AI-06/O-AI-05.
- Migration AI dùng **V12** đúng D290. V1–V11 bất biến.

## 2. Việc đầu tiên của AIP2 (bắt buộc, trước mọi dòng code)

Chạy `mvn -f backend/pom.xml clean verify` trên môi trường Java 21 + Maven + Docker và **dán nguyên văn tổng kết** — để chứng minh hai test đã sửa ở AIP1 (`OpenApiExportIT`, `ApiDocsProdProfileIT`) không làm đỏ bộ test hiện có.

- Nếu máy không có Java 21/Maven/Docker: **DỪNG và báo người dùng**, kèm lệnh cài/chạy chính xác. Không viết code backend khi không tự kiểm chứng được bằng test.
- Nếu verify đỏ vì hai test đã sửa: sửa cho xanh trước khi làm tiếp, ghi vào báo cáo.

## 3. Ranh giới khi code AIP2–AIP4, AIP7

- Chỉ sửa trong module `ai`, migration `V12__ai_assistant.sql` (hoặc tên tương đương bắt đầu bằng V12), test AI, và phần bổ sung tối thiểu `*QueryApi` ở module khác đúng như kế hoạch đã duyệt; không đổi route/payload REST/sync cũ; không sửa `mobile/`, `ci/mobile-ci.yml`.
- Khi runtime controller sinh được contract: **xóa hai marker overlay trong `docs/api/openapi.yaml` và ngoại lệ trong `OpenApiExportIT`** đúng cam kết; contract sinh ra phải trùng với contract AIP1 đã công bố (18 path, 42 schema, 15 mã lỗi) — lệch ở đâu ghi `[PROPOSED]` ở đó, vì Flutter sẽ build theo fixture đã công bố.
- Mọi test dùng **fake `LLMProvider`**; không gọi OpenAI thật trong test/CI; `OPENAI_API_KEY` không vào repo, log, báo cáo hay chat. Tuân thủ D86 (khóa nghiệp vụ trước, `ChangeRecorder` cuối — nếu áp dụng), không gọi mạng trong transaction, tiền không `double`.
- Mọi bảng AI có `user_id`, vào `UserDataCatalog`, export/purge đúng FR-AI-11.
- Quyết định phát sinh ghi `[PROPOSED]`, không tự duyệt. Mâu thuẫn spec → `[OPEN]`, dừng hỏi.

## 4. Báo cáo cuối (`docs/review/DEPLOY_AI_PROGRESS.md` cập nhật + phụ lục vào `DEPLOY_AI_FINAL_REPORT.md`)

- Bảng AIP2/3/4/7 với 3 nhãn (`XONG (bằng chứng)`, `MỘT PHẦN`, `CHƯA LÀM`); bản đồ FR-AI → test AI-xx cập nhật trạng thái thật.
- Nguyên văn `mvn clean verify` **xanh 3 lượt liên tiếp** ở cuối AIP7, kèm tổng số test (phải > 847 vì có test AI mới).
- Bằng chứng test injection AI-10..15, consent, quota/budget với fake provider; số đo cost/latency fake provider (ghi rõ là fake).
- Xác nhận marker overlay đã xóa và export OpenAPI từ runtime trùng contract công bố.
- Việc live (Render, secret, ZDR, store) vẫn NO-GO chờ con người — giữ nguyên danh sách §9 của báo cáo trước.
