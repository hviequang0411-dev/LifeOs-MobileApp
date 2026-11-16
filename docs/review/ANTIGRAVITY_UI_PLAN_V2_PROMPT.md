# Prompt sửa Kế hoạch nâng cấp giao diện — bản v2 (`ANTIGRAVITY_UI_PLAN_V2_PROMPT.md`)

> Gửi nguyên văn cho Antigravity. Phạm vi: **chỉ sửa tài liệu** `docs/engineering/MOBILE_UI_UPGRADE_PLAN.md` (nâng lên v2) và thêm ảnh dựng vào `docs/review/mobile_screenshots/ui_upgrade/preview/`. **Chưa viết code sản phẩm, chưa làm Giai đoạn 2.** Mọi ranh giới trong `ANTIGRAVITY_UI_UPGRADE_PROMPT.md` §1 vẫn giữ nguyên.

---

Bản kế hoạch v1 có cấu trúc tốt, nhưng người dùng chưa duyệt. Hãy sửa thành v2 theo 8 mục dưới đây. Mỗi mục ghi rõ "đã sửa như thế nào" trong phần **Nhật ký thay đổi v1 → v2** ở đầu tài liệu. Không tự đổi nhãn `[PROPOSED]`/`[OPEN]`; không bịa số liệu.

## 1. Tính lại toàn bộ tỷ lệ tương phản (lỗi nghiêm trọng)

Các số trong v1 sai. Kết quả kiểm tra độc lập theo công thức WCAG 2.x:

| Cặp màu | v1 ghi | Thực tế |
|---|---|---|
| A: hổ phách `#C8681A` trên nền `#F8F7F3` | 4,7 | 3,59 |
| A: hổ phách trên `#FFFFFF` | 4,7 | 3,85 |
| A: chữ mờ `#707C76` trên `#F8F7F3` | 4,6 | 4,05 |
| B: san hô `#F43F5E` trên `#F4F6FB` | 4,6 | 3,40 |
| B: xanh `#0284C7` trên `#F4F6FB` | 4,8 | 3,79 |
| Chữ trắng trên nút hổ phách (cả 3 hướng) | không nêu | 3,85 |

Yêu cầu:
- Viết một script (Dart test hoặc Python, đặt ở `mobile/tool/` hoặc `mobile/test/`) tính tỷ lệ tương phản theo công thức WCAG. Chạy cho **mọi cặp chữ/nền, nút/chữ nút, icon/nền, đường viền chức năng** của cả 3 hướng, Light và Dark.
- Đổi màu cho mọi cặp chữ thường dưới 4,5:1 (chữ lớn ≥ 18,66px đậm hoặc ≥ 24px: tối thiểu 3:1; thành phần không phải chữ cần ≥ 3:1). Với nút điểm danh màu hổ phách: chọn hổ phách tối hơn cho nền nút hoặc chuyển chữ nút sang màu tối, sao cho đạt 4,5:1.
- Đưa bảng kết quả do script sinh ra vào kế hoạch (cột "tính bằng script"), không gõ tay. Đính kèm script và lệnh chạy.

## 2. Ảnh dựng thật cho 3 hướng

Khung ASCII không đủ để người dùng chọn. Làm:
- Với mỗi hướng A, B, C: tạo theme thật (chỉ trong tài liệu/ảnh, chưa đổi theme của app), chụp bằng golden test (font thật) màn **Hôm nay** và **Tài chính**, Light và Dark, phone 390×844. Tổng 3 × 2 × 2 = 12 ảnh.
- Lưu ở `docs/review/mobile_screenshots/ui_upgrade/preview/` và nhúng vào kế hoạch.
- Bỏ khung "hiện trạng" vẽ tay (số liệu bịa như "+15.000.000 ₫"). Thay bằng ảnh thật của bản hiện tại, chú thích điểm yếu lên ảnh hoặc liệt kê theo số.
- Dùng cùng một bộ dữ liệu seed cho cả 3 hướng để so sánh công bằng.

## 3. Ánh xạ đầy đủ "tính năng → gói UPW"

Thêm bảng mà mọi tính năng ở §6 của v1 đều có đúng một gói: Life Rings, dòng thời gian, bản đồ nhiệt thói quen, Donut tài chính, Review Wizard, tìm kiếm toàn cục, Life Wheel, Focus Garden, nhật ký tâm trạng, nghi thức sáng/tối, onboarding, huy hiệu. Gói nào quá nặng thì tách. Đánh dấu tính năng **ngoài đợt này** là "hoãn", để người dùng chốt tập con. Phạm vi mặc định đề xuất cho đợt này: các tính năng dùng dữ liệu có sẵn + Life Wheel + Focus Garden; còn lại là "hoãn" cho đến khi người dùng duyệt thêm.

## 4. Tên dữ liệu đúng theo spec

Thay mọi tên bảng/thực thể trong §6 và §7 bằng `entityType` thật trong `docs/engineering/SYNC_PROTOCOL.md` và tên bảng thực tế trong `mobile/lib/core/database/`. Với mỗi tính năng dùng dữ liệu có sẵn, ghi chính xác bảng và cột sẽ đọc. Nếu một tên (ví dụ `time_blocks`) không tồn tại, nói rõ và đổi cách làm. Sửa danh sách bảng hạ tầng cho khớp mã nguồn hiện tại.

## 5. Phân định tâm trạng

`reviews` (thực thể đồng bộ của backend) và `local_mood_logs` (kho cục bộ) đang chồng nhau. Mô tả rõ: bước đánh giá tâm trạng của Review Wizard ghi vào đâu, `local_mood_logs` có liên quan gì, người dùng nhìn thấy hai thứ khác nhau hay một. Nếu không thể tách sạch, **hoãn nhật ký tâm trạng** khỏi đợt này và ghi lý do.

## 6. Kho `local_extras` theo từng người dùng

CSDL gốc theo `userId`; kho phụ phải cùng nguyên tắc để máy dùng chung không lộ dữ liệu giữa các tài khoản. Đề xuất file riêng `lifeos_local_extras_<userId>.db` (hoặc cột `user_id` trên mọi bảng, nêu lý do chọn). Ghi rõ:
- Hành vi khi đăng xuất: giữ lại (đã được người dùng đồng ý hướng này, đề xuất `[PROPOSED]`), không hiển thị cho người dùng khác.
- Hành vi khi xóa tài khoản: xóa kho khi app xác nhận việc xóa hoàn tất. Cách app biết điều đó theo hợp đồng API hiện tại là `[OPEN]`; nêu các lựa chọn, không tự bịa.
- Dữ liệu `LOCAL_ONLY` có vào chức năng xuất dữ liệu cục bộ hay không: nêu `[OPEN]`.

## 7. Tiêu chí hiệu năng đúng nghĩa

"60fps" không đo được bằng thời gian truy vấn. Thay bằng hai tiêu chí tách bạch:
- **Truy vấn:** trang đầu < 1000 ms với 10.000 bản ghi (đã có).
- **Khung hình:** đo bằng integration test ở chế độ profile (`flutter drive --profile` hoặc `traceAction`/`FrameTiming`): tỷ lệ khung chậm (> 16 ms) < 5% khi cuộn danh sách 10.000 mục. Nếu không đo được trên máy phát triển, ghi "chưa đo" cùng lệnh để người dùng chạy, không ghi đạt.

## 8. Màu, icon, font

- Màu **Mục tiêu** (`#1D4ED8`) đang trùng màu "thông tin" và gần màu Lịch. Chọn lại để tách biệt, đạt tương phản, và kiểm tra khả năng phân biệt với người mù màu (mô phỏng deuteranopia/protanopia, nêu kết quả).
- Màu không được là dấu hiệu duy nhất: mỗi module có icon riêng nhất quán; trạng thái ưu tiên/rủi ro đi kèm chữ hoặc icon.
- Rút bộ font còn **tối đa 2 họ** (một cho chữ nội dung và tiêu đề, một tùy chọn cho tiêu đề lớn). Số tiền, giờ, streak dùng số cùng chiều rộng (`FontFeature.tabularFigures`) của font chính, không dùng font đơn cách. Nêu tên font, giấy phép (OFL) và dung lượng assets sau khi đóng gói.

## Báo cáo khi nộp v2

- Nhật ký thay đổi v1 → v2 theo 8 mục trên.
- Danh sách ảnh dựng và script tương phản (đường dẫn, lệnh chạy, đầu ra dán nguyên văn).
- Cập nhật bảng quyết định `[PROPOSED]`/`[OPEN]`; các câu hỏi còn lại gửi người dùng.
- Tuyệt đối chưa bắt đầu Giai đoạn 2. Dừng lại chờ người dùng chọn hướng và duyệt.
