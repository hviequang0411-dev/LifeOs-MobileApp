# LifeOS — Sổ quyết định còn treo (`PENDING_DECISIONS.md`)

> Lập 2026-11-14 bởi Claude (vai trò tài liệu/review). Gom MỌI quyết định đang chờ người dùng từ backend, mobile, deploy/AI về một chỗ, kèm khuyến nghị. Sau khi người dùng duyệt, agent liên quan đổi nhãn trong tài liệu gốc và ghi ngày.
>
> **Đồng bộ 2026-11-19:** §1–§2 đã được người dùng duyệt 2026-11-15 và đợt sửa backend F1–F9 đã chạy xong cùng ngày (`BACKEND_FIX_REPORT.md`). `AIP7-OPENAPI-01` đã `[CONFIRMED]` 2026-11-14. Mục còn mở thật sự nằm ở **§0**.

---

## 0. Còn mở tại 2026-11-19 (đọc mục này trước)

| # | Mục | Nguồn | Khuyến nghị |
|---|---|---|---|
| 0.1 | 12 quyết định `[PROPOSED]` mới phát sinh trong đợt sửa backend (F3/F4/F5/F6/F7/F8/F9/OQ3) | `BACKEND_FIX_REPORT.md` §8 | Xem rồi duyệt; riêng **mục 12** cần xác nhận: agent đã tự đổi 4 mục giữ lại (§1 dưới) sang `[CONFIRMED]` theo hành vi sau sửa |
| 0.2 | Mobile §3: MP-01..MP-03, xuất `LOCAL_ONLY` (`"local_extras"`), sửa `HabitHeatmapWidget` | §3 dưới | Câu duyệt §5 cho Antigravity **chưa được thực thi**: `MOBILE_LOCAL_ONLY_FEATURES.md` vẫn ghi `[OPEN]`, heatmap vẫn dùng `DateTime.now()` (kiểm 2026-11-19) |
| 0.3 | Mobile đối chiếu thay đổi hành vi sau đợt sửa backend | `BACKEND_FIX_REPORT.md` §6 | Giao Antigravity: (a) PATCH đổi tài khoản khác tiền tệ ⇒ 422 `FINANCE_CURRENCY_MISMATCH`; (b) lưu trữ tài khoản làm định kỳ chuyển `PAUSED` (qua pull, `mutation_id` NULL) và thông báo `finance.recurring.paused` |
| 0.4 | `docs/architecture/` chưa ghi V13 (`DATABASE_DESIGN.md` §4.5) và `SNAPSHOT_HMAC_SECRET` (`SYSTEM_ARCHITECTURE.md` §3.14) | `BACKEND_FIX_REPORT.md` §6 | Thư mục được bảo vệ (AGENTS §1.2) — cần người dùng cho phép sửa |
| 0.5 | `deploy/scripts/preflight.*`, `deploy/README.md`, `deploy/render/` chưa kiểm `SNAPSHOT_HMAC_SECRET` (độ dài ≥ 32, khác các khóa khác) và chưa có tài liệu xoay khóa | `BACKEND_FIX_REPORT.md` §5 | Giao agent deploy đợt sau; DWP6 có thể bật rule `LifeOSJobStale` vì producer đã có |
| 0.6 | 503 `Lock not acquired` từng xuất hiện một lần ở `ExportPurgeFixIT.F5_concurrent…` | `BACKEND_FIX_REPORT.md` §7 | Theo dõi; nếu lặp lại trên CI thì điều tra lại cách khóa F5, không coi là môi trường |
| 0.7 | `ci/mobile-ci-1.yml` khác `ci/mobile-ci.yml` — chưa rõ bản nào là chuẩn | Kiểm repo 2026-11-19 | Người dùng/agent mobile xác nhận và xóa bản thừa trước khi cài vào `.github/workflows/` |
| 0.8 | Checklist go-live (§4) | `DEPLOY_AI_FINAL_REPORT.md` §9 | Không cần quyết ngay |

## 1. Backend — 75 mục `[PROPOSED]` trong `BACKEND_PROGRESS.md` (WP2–WP8) — **ĐÃ DUYỆT 2026-11-15**

**Bối cảnh:** 75 quyết định cài đặt phát sinh khi Claude Code làm WP1–WP8, nổi bật 12 mục ở `BACKEND_FINAL_REPORT.md` §7.

**Kết quả:** người dùng duyệt nguyên trạng 2026-11-15, trừ 4 mục bị vòng review bác/yêu cầu sửa; 4 mục này đã được sửa trong đợt F1–F9 và chốt theo hành vi sau sửa (xem §0.1 về việc xác nhận chốt):

| Mục giữ lại chờ đợt sửa | Xử lý trong đợt sửa | Trạng thái |
|---|---|---|
| Khóa HMAC snapshot sinh ngẫu nhiên lúc khởi động (WP5 #4) | F9: chuyển sang secret `SNAPSHOT_HMAC_SECRET` | ĐÃ SỬA 2026-11-15 |
| Recurring bỏ lặng occurrence khi account archive | F6: tự PAUSED + báo người dùng | ĐÃ SỬA 2026-11-15 |
| Analytics cắt 200.000 dòng im lặng | F7: trả 422 `ANALYTICS_RANGE_TOO_LARGE` | ĐÃ SỬA 2026-11-15 |
| Giới hạn export 1-active/2-ngày không nguyên tử | F5: khóa dòng `users` + unique index (V13) | ĐÃ SỬA 2026-11-15 |

**Ghi chú kèm duyệt:** các giới hạn đếm không khóa (200 habit, 50 routine, ghim 10 ghi chú) được chốt là **giới hạn mềm UX** — đã ghi vào `docs/phases/` 2026-11-15.

## 2. Backend — Open Questions (`BACKEND_FINAL_REPORT.md` §12)

| # | Câu hỏi | Trạng thái |
|---|---|---|
| OQ1 | FR-GOAL-06: thêm `GOAL`/`MILESTONE` vào `reminder.targetType`? | **HOÃN** (duyệt 2026-11-15) — làm thành đợt riêng sau khi app ổn định |
| OQ2 | Module `analytics` thay `dashboard`/`lifereview`? | **ĐÃ ĐÓNG** — `AGENTS.md` §3 và `JAVA_SPRING_RULES.md` đã dùng `analytics` |
| OQ3 | Gauge `lifeos.job.last_success{job}`? | **ĐÃ LÀM** 2026-11-15 (`JobHealth`, `JobHealthIT`); thêm gauge phụ `lifeos.job.expected_interval{job}` là `[PROPOSED]` (§0.1) |
| OQ4 | CI nightly (mô phỏng đa thiết bị đầy đủ + benchmark)? | **DUYỆT về chủ trương**, làm sau khi 2 workflow CI cơ bản chạy thật trên GitHub |
| OQ5 | Kho tệp xuất production? | **ĐÃ ĐÓNG** — trùng O-DEP-05, duyệt 2026-11-11 |
| OQ6 | `TZ=UTC` cho image/test? | **ĐÃ ĐÓNG** — `application-prod.yml` đặt UTC; ghi chú đã thêm vào `backend/README.md` 2026-11-15 |

## 3. Mobile — còn treo

| Mục | Trạng thái | Khuyến nghị |
|---|---|---|
| MP-01..MP-03 (báo cáo mobile đầu: cấu trúc lưu hybrid cột chỉ mục + `payload_json`…) | `[PROPOSED]` từ 2026-11-08, code đã chạy theo hướng này và qua E2E thật | **DUYỆT nguyên trạng** — câu duyệt §5 chưa được thực thi |
| 3 mục `[PROPOSED]` đợt Daily Use | **ĐÃ ĐÓNG** — D-M05..D-M07 `[CONFIRMED]` 2026-11-12 (`MOBILE_PROGRESS.md`) | — |
| `[OPEN]` FCM endpoint | **ĐÃ ĐÓNG** 2026-11-12 (Q-M01): `POST /api/v1/devices` + `PUT /api/v1/devices/{id}/push-token` | — |
| `[OPEN]` Xuất dữ liệu `LOCAL_ONLY` (Life Wheel, Focus Garden) vào tệp xuất cục bộ | Chưa thực thi; `MOBILE_LOCAL_ONLY_FEATURES.md` vẫn `[OPEN]` | **DUYỆT CÓ** — thêm khóa `"local_extras"` vào tệp xuất, giao Antigravity đợt sau |
| D-M08 (benchmark 50k: 60 s full-suite) | `[CONFIRMED]` 2026-11-14; số đo chạy lẻ gần nhất 11,0 s (2026-11-15) | Theo dõi trong các báo cáo sau |
| D-M09 (lối vào Trợ lý AI) | **ĐÃ ĐÓNG** — `[CONFIRMED]` 2026-11-15 | — |
| `HabitHeatmapWidget` dùng `DateTime.now()` làm golden lệch khi sang ngày | Chưa sửa (kiểm 2026-11-19) | Giao Antigravity đợt sau: dùng `referenceUtcProvider` |
| E2E AI với backend Docker thật | `CHƯA CHẠY` — `FakeLLMProvider` chỉ có `@Profile("test")`, stack Docker chạy `dev` | Cần quyết định cách chạy E2E AI local (profile riêng cho fake provider) — thay đổi backend, cần plan |
| Đo khung hình profile mode trên máy thật | Chưa đo | Người dùng chạy khi có thiết bị; lệnh trong UPW8 |

## 4. Deploy/AI — còn treo (đều là việc go-live, cần tài khoản/secret con người)

Danh sách đầy đủ ở `DEPLOY_AI_FINAL_REPORT.md` §9. Không mục nào cần quyết ngay; khi muốn go-live thì làm theo thứ tự: tài khoản Render + domain → secret qua protected environment (gồm `SNAPSHOT_HMAC_SECRET` mới) → staging + restore drill → GO. AIP5 (RAG) và AIP6 (action proposal) hoãn theo O-AI-05/06 đã duyệt. `AIP7-OPENAPI-01` đã `[CONFIRMED]` 2026-11-14 (giữ overlay bốn path action proposal). Đợt sửa backend F1–F9 — điều kiện tiên quyết trước khi nhập dữ liệu tài chính thật — **đã xong 2026-11-15**. Mã định danh ứng dụng vẫn là mẫu (`com.lifeos.lifeos_mobile` / `com.lifeos.lifeosMobile`), chưa phải `vn.lifeos.app` theo P-DWP7-01.

## 5. Câu duyệt sẵn

**Claude Code — ĐÃ GỬI và thực thi 2026-11-15** (giữ lại để tra cứu):
```
Tôi duyệt toàn bộ [PROPOSED] trong BACKEND_PROGRESS.md nguyên trạng, trừ 4 mục trong PENDING_DECISIONS.md §1 giữ [PROPOSED] chờ đợt sửa F1–F9. Giới hạn đếm không khóa chốt là giới hạn mềm UX. OQ1 hoãn; OQ2 duyệt theo bản AGENTS.md tôi đặt vào; OQ3, OQ4 duyệt về chủ trương, thực thi sau; OQ5, OQ6 đã đóng. Đổi nhãn và ghi ngày vào BACKEND_PROGRESS.md.
```

**Gửi Antigravity (đợt kế tiếp, gộp với §0.3) — CHƯA thực thi:**
```
Tôi duyệt MP-01..MP-03 nguyên trạng. [OPEN] xuất LOCAL_ONLY: duyệt CÓ — thêm khóa "local_extras" vào tệp xuất cục bộ. Sửa HabitHeatmapWidget dùng referenceUtcProvider thay DateTime.now() để golden ổn định qua ngày. Ghi nhận các thay đổi nhãn vào MOBILE_PROGRESS.md.
```

---

*Hiện trạng 2026-11-19: (a) màn AI mobile đã xong (AWP-1..6); (b) đợt sửa backend F1–F9 đã xong; (c) còn các mục §0 và checklist go-live §4.*
