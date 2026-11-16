# LifeOS — Sổ quyết định còn treo (`PENDING_DECISIONS.md`)

> Lập 2026-11-14 bởi Claude (vai trò tài liệu/review). Gom MỌI quyết định đang chờ người dùng từ backend, mobile, deploy/AI về một chỗ, kèm khuyến nghị. Duyệt bằng cách gửi các câu ở §5 (chỉnh chỗ nào muốn khác). Sau khi người dùng duyệt, agent liên quan đổi nhãn trong tài liệu gốc và ghi ngày.

---

## 1. Backend — 75 mục `[PROPOSED]` trong `BACKEND_PROGRESS.md` (WP2–WP8)

**Bối cảnh:** 75 quyết định cài đặt phát sinh khi Claude Code làm WP1–WP8, nổi bật 12 mục ở `BACKEND_FINAL_REPORT.md` §7. Chúng đã chạy ổn qua 879 test xanh và một vòng review độc lập (GPT).

**Khuyến nghị: duyệt nguyên trạng toàn bộ, TRỪ 4 mục đã bị vòng review bác/yêu cầu sửa** — 4 mục này đã nằm trong `CLAUDE_CODE_BACKEND_FIX_PROMPT.md` (đang HOÃN), giữ nhãn `[PROPOSED]` tới khi đợt sửa chạy:

| Mục giữ lại chờ đợt sửa | Xử lý trong đợt sửa |
|---|---|
| Khóa HMAC snapshot sinh ngẫu nhiên lúc khởi động (WP5 #4) | F9: chuyển sang secret `SNAPSHOT_HMAC_SECRET` |
| Recurring bỏ lặng occurrence khi account archive | F6: bác — tự PAUSED + báo người dùng |
| Analytics cắt 200.000 dòng im lặng | F7: báo truncated hoặc lỗi |
| Giới hạn export 1-active/2-ngày không nguyên tử | F5: khóa + unique index |

**Ghi chú kèm duyệt:** các giới hạn đếm không khóa (200 habit, 50 routine, ghim 10 ghi chú) được chốt là **giới hạn mềm UX** (có thể vượt 1 khi tạo đồng thời) — ghi rõ vào tài liệu, không cần sửa code.

## 2. Backend — Open Questions (`BACKEND_FINAL_REPORT.md` §12)

| # | Câu hỏi | Khuyến nghị |
|---|---|---|
| OQ1 | FR-GOAL-06: thêm `GOAL`/`MILESTONE` vào `reminder.targetType` (đổi hợp đồng Phase 4 + SYNC_PROTOCOL)? | **HOÃN** — tính năng mới, làm thành một đợt riêng sau khi app ổn định; không gộp vào đợt sửa lỗi |
| OQ2 | Sửa `AGENTS.md` §3: module `analytics` thay `dashboard`/`lifereview`? | **DUYỆT** — bản sửa đã soạn sẵn (file `AGENTS.md` kèm theo), người dùng tự đặt vào vì file này được bảo vệ |
| OQ3 | Gauge `lifeos.job.last_success{job}`? | **DUYỆT về chủ trương**, thực thi gộp vào đợt sửa backend (DWP6 cũng đang chờ producer metric này) |
| OQ4 | CI nightly (mô phỏng đa thiết bị đầy đủ + benchmark)? | **DUYỆT về chủ trương**, làm sau khi 2 workflow CI cơ bản chạy thật trên GitHub |
| OQ5 | Kho tệp xuất production? | **ĐÃ ĐÓNG** — trùng O-DEP-05 (persistent disk khi còn một instance), người dùng đã duyệt 2026-11-11 |
| OQ6 | `TZ=UTC` cho image/test? | **ĐÃ ĐÓNG** — `application-prod.yml` của DWP2 đã đặt UTC; chỉ cần Claude Code thêm vào tài liệu cấu hình khi đợt sửa chạy |

## 3. Mobile — còn treo

| Mục | Trạng thái | Khuyến nghị |
|---|---|---|
| MP-01..MP-03 (báo cáo mobile đầu: cấu trúc lưu hybrid cột chỉ mục + `payload_json`…) | `[PROPOSED]` từ 2026-11-08, code đã chạy theo hướng này và qua E2E thật | **DUYỆT nguyên trạng** (hợp thức hóa hiện trạng) |
| 3 mục `[PROPOSED]` đợt Daily Use (phiên bản `flutter_local_notifications` + inexact alarm; 4 công tắc thông báo; danh sách quyền) | Đã có câu duyệt gợi ý ngày 12-11 — **nếu chưa gửi thì gửi** | DUYỆT nguyên trạng |
| `[OPEN]` FCM endpoint | **ĐÃ ĐÓNG** 2026-11-12: dùng `POST /api/v1/devices` + `PUT /api/v1/devices/{id}/push-token` có sẵn trong OpenAPI | Chỉ cần báo Antigravity |
| `[OPEN]` Xuất dữ liệu `LOCAL_ONLY` (Life Wheel, Focus Garden) vào tệp xuất cục bộ | Chưa quyết từ 2026-11-10 | **DUYỆT CÓ** — thêm khóa `"local_extras"` vào tệp xuất, giao Antigravity đợt sau |
| D-M08 (benchmark 50k: 60 s full-suite) | Đã duyệt kèm điều kiện ghi số đo chạy lẻ | Theo dõi trong các báo cáo sau |
| Lỗi mới ghi nhận: `HabitHeatmapWidget` dùng `DateTime.now()` làm golden lệch khi sang ngày (0,07%, thấy ngày 12-11) | Chưa giao | Giao Antigravity đợt sau: dùng `referenceUtcProvider` cho heatmap |
| Đo khung hình profile mode trên máy thật | Chưa đo | Người dùng chạy khi có thiết bị; lệnh trong UPW8 |

## 4. Deploy/AI — còn treo (đều là việc go-live, cần tài khoản/secret con người)

Danh sách đầy đủ ở `DEPLOY_AI_FINAL_REPORT.md` §9. Không mục nào cần quyết ngay; khi muốn go-live thì làm theo thứ tự: tài khoản Render + domain → secret qua protected environment → staging + restore drill → GO. AIP5 (RAG) và AIP6 (action proposal) hoãn theo O-AI-05/06 đã duyệt. Đợt sửa backend F1–F9 (`CLAUDE_CODE_BACKEND_FIX_PROMPT.md`, đang HOÃN) **phải chạy trước khi nhập dữ liệu tài chính thật hoặc go-live** — F1 là lỗi mức Blocker với tiền thật.

## 5. Câu duyệt sẵn (gửi lần lượt cho đúng agent)

**Gửi Claude Code (hoặc lưu lại tới khi kích hoạt đợt sửa):**
```
Tôi duyệt toàn bộ [PROPOSED] trong BACKEND_PROGRESS.md nguyên trạng, trừ 4 mục trong PENDING_DECISIONS.md §1 giữ [PROPOSED] chờ đợt sửa F1–F9. Giới hạn đếm không khóa chốt là giới hạn mềm UX. OQ1 hoãn; OQ2 duyệt theo bản AGENTS.md tôi đặt vào; OQ3, OQ4 duyệt về chủ trương, thực thi sau; OQ5, OQ6 đã đóng. Đổi nhãn và ghi ngày vào BACKEND_PROGRESS.md.
```

**Gửi Antigravity (đợt kế tiếp, gộp với việc khác):**
```
Tôi duyệt MP-01..MP-03 nguyên trạng. [OPEN] xuất LOCAL_ONLY: duyệt CÓ — thêm khóa "local_extras" vào tệp xuất cục bộ. Sửa HabitHeatmapWidget dùng referenceUtcProvider thay DateTime.now() để golden ổn định qua ngày. Ghi nhận các thay đổi nhãn vào MOBILE_PROGRESS.md.
```

---

*Khi mọi mục §1–§3 được duyệt/đóng, dự án chỉ còn: (a) màn AI của Antigravity đang làm, (b) đợt sửa backend F1–F9 chờ kích hoạt, (c) checklist go-live §4.*
