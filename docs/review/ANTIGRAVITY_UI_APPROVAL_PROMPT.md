# Lời duyệt kế hoạch UI v2 và lệnh triển khai Giai đoạn 2 (`ANTIGRAVITY_UI_APPROVAL_PROMPT.md`)

> Gửi nguyên văn cho Antigravity. Người dùng đã duyệt như dưới đây. Làm **một lượt liên tục** UPW1 → UPW8 theo `docs/engineering/MOBILE_UI_UPGRADE_PLAN.md` (v2), chỉ nộp **một báo cáo cuối**. Chỉ dừng giữa chừng khi gặp `[OPEN]` mà kế hoạch chưa có đáp án, hoặc khi cần vượt ranh giới §1 của prompt gốc (`ANTIGRAVITY_UI_UPGRADE_PROMPT.md`).

---

## 1. Những gì người dùng đã quyết (ghi vào kế hoạch, đổi nhãn đúng như sau)

- **Hướng thiết kế: Hướng A — Botanical Premium.** `[CONFIRMED]` (người dùng chọn "A", 2026-11-10). Hướng B và C bị loại, không triển khai; giữ ảnh dựng của chúng trong `preview/` làm tư liệu.
- **Khi xóa tài khoản: Phương án C.** `[CONFIRMED]` (người dùng chọn "C"). Nội dung C, được người dùng chốt:
  - App **không bao giờ tự xóa** dữ liệu cục bộ (CSDL gốc, `outbox`, kho `local_extras`), kể cả khi gặp 401, 403, 404 hoặc khi 7 ngày chờ xóa đã hết. Quy tắc PWP3 "không tự xóa SQLite/Outbox" vẫn giữ nguyên.
  - Trong Cài đặt có nút **"Xóa dữ liệu trên máy"** (người dùng chủ động), có hộp xác nhận nêu rõ: số thay đổi chưa đồng bộ trong `outbox` sẽ mất, và kho `LOCAL_ONLY` (Life Wheel, Focus Garden) cũng bị xóa. Cần gõ chữ xác nhận hoặc giữ nút, không xóa bằng một chạm.
  - Hành động này xóa cả `lifeos_<userId>.sqlite` và `lifeos_local_extras_<userId>.sqlite` của người dùng đang đăng nhập, rồi đưa app về màn đăng nhập.
- **Phạm vi đợt này:** theo bảng §6.3 của v2 (tính năng dùng dữ liệu có sẵn + Life Wheel + Focus Garden); 4 tính năng ở trạng thái HOÃN giữ nguyên HOÃN.
- Quyết định `UP-D01..UP-D04` trong kế hoạch được duyệt cùng kế hoạch; cập nhật tiêu đề kế hoạch thành "Đã duyệt 2026-11-10" và chuyển nhãn tương ứng.

## 2. Việc còn `[OPEN]` (không làm, chờ người dùng)

- **Xuất dữ liệu `LOCAL_ONLY`** (gộp Life Wheel và Focus Garden vào tệp xuất, khóa `"local_extras"`): người dùng chưa trả lời. **Không triển khai** trong đợt này; ghi rõ trong `MOBILE_LOCAL_ONLY_FEATURES.md` là `[OPEN]` và trong Cài đặt không hứa hẹn điều này.

## 3. Điều kiện bắt buộc khi triển khai

1. **Bảng tương phản tính bằng script cho Hướng A phải phủ mọi cặp đang dùng thật trong app**, không chỉ các cặp ở bảng mẫu: nút ở mọi trạng thái (điểm danh, đã điểm danh, bỏ qua, vô hiệu), chip ưu tiên, chip rủi ro mục tiêu, nhãn biểu đồ, chữ trên màu nhấn từng module (Việc, Thói quen, Tài chính, Lịch, Tri thức, Mục tiêu), cả Light và Dark. Test `contrast_wcag_test.dart` phải đọc màu trực tiếp từ `app_tokens.dart` (không sao chép hằng số) để không lệch với app thật. Không đạt thì đổi màu, không hạ ngưỡng.
2. Thay vì chỉ đổi màu, hãy thực hiện **đúng các cải tiến bố cục và tính năng** trong §5, §6 của v2 (Life Rings, dòng thời gian, bản đồ nhiệt, Donut, ngân sách, Review Wizard, tìm kiếm toàn cục, Life Wheel, Focus Garden, thẻ ví, thẻ ghi chú, thẻ mục tiêu, wizard, 5 trạng thái giao diện). Ảnh dựng ở bước kế hoạch mới chỉ đổi bảng màu; người dùng kỳ vọng giao diện **khác rõ rệt** về bố cục, độ sâu, kiểu chữ và chuyển động.
3. Dùng token cho mọi màu, khoảng cách, bo góc, chuyển động; không hard-code trong widget. Chữ số tiền, giờ, streak dùng `FontFeature.tabularFigures`.
4. Mọi chuỗi qua `ViStrings`/`app_vi.arb`; không chuỗi kỹ thuật, không mã enum tiếng Anh trên UI.
5. Tuân thủ `FLUTTER_RULES.md`: UI chỉ đọc từ SQLite; ghi qua giao dịch cục bộ + `outbox` cho dữ liệu đã có trong spec; tiền `Decimal10k`; mỗi file dưới ~600 dòng; tách theo `features/<module>/{data,domain,presentation}`.
6. Kho `LOCAL_ONLY`: CSDL Drift riêng `lifeos_local_extras_<userId>.sqlite`, bảng tiền tố `local_`, không `sync_state`, không `outbox`, không API; chú thích `// LOCAL_ONLY` trong code; ghi đầy đủ ở `docs/review/MOBILE_LOCAL_ONLY_FEATURES.md`, kèm ghi chú "backend cần gì để đồng bộ sau" (không triển khai).
7. Đăng xuất giữ nguyên kho `local_extras` của người dùng đó; người dùng khác đăng nhập mở tệp riêng.
8. Ranh giới bất khả xâm phạm (không sửa `backend/`, OpenAPI, `SYNC_PROTOCOL.md`, `SyncEngine`, `api_client.dart`, `entity_types.dart`, 29 bảng gốc, `schemaVersion` CSDL gốc) vẫn giữ. Báo cáo phải có `git diff --stat` chứng minh.

## 4. Bằng chứng trong báo cáo cuối

- Nguyên văn đầu ra của `dart analyze`, `flutter test`, và `flutter test --dart-define=LIFEOS_RUN_REAL_E2E=true` (9 luồng E2E thật vẫn xanh).
- Golden mới với font thật, Light/Dark × phone/tablet, chạy lặp 2 lần không lệch; ảnh **trước/sau** của từng tab và từng màn mới trong `docs/review/mobile_screenshots/ui_upgrade/`.
- Bảng tương phản do script sinh, phủ mọi cặp thật (mục 3.1); kết quả test đọc từ token.
- Tiêu chí hiệu năng: truy vấn đo được; khung hình ghi "chưa đo" kèm lệnh nếu không đo được, không ghi đạt.
- Danh sách file đã tạo/sửa/xóa theo từng gói UPW; `MOBILE_PROGRESS.md` với 3 nhãn trạng thái (`XONG (có bằng chứng)`, `MỘT PHẦN`, `CHƯA LÀM`).
- Mục chưa làm ghi "chưa làm" và lý do. Không báo hoàn thành khi thiếu bằng chứng.
