# Kế hoạch Nâng cấp Giao diện Ứng dụng LifeOS Mobile (UI Upgrade Master Plan) — Bản v2
## Nâng cấp trải nghiệm Premium, Hiện đại, Tinh tế và Trực quan cho Hệ điều hành Cuộc sống Cá nhân

> **Tài liệu:** `docs/engineering/MOBILE_UI_UPGRADE_PLAN.md`  
> **Phiên bản:** `v2` (Cập nhật từ phản hồi vòng 1 của Người dùng)  
> **Trạng thái:** `[PROPOSED]` — Chờ người dùng duyệt và chọn Hướng thiết kế  
> **Ngày lập v2:** 2026-11-10  
> **Phạm vi tác động:** Chỉ trong thư mục `mobile/`, kho cục bộ phụ theo người dùng `mobile/lib/core/database/local_extras_database.dart` và tài liệu liên quan `docs/review/MOBILE_LOCAL_ONLY_FEATURES.md`. Tuyệt đối **không** sửa `backend/`, không sửa CSDL gốc đồng bộ, không sửa API contract.

---

## NHẬT KÝ THAY ĐỔI (Changelog v1 → v2 theo 8 yêu cầu)

| # | Yêu cầu người dùng | Đã sửa như thế nào trong Bản v2 |
|:---:|---|---|
| **1** | **Tính lại toàn bộ tỷ lệ tương phản bằng script** | Đã viết script kiểm chứng WCAG 2.x độc lập bằng Python ([`mobile/tool/contrast_calculator.py`](file:///d:/LifeOS/mobile/tool/contrast_calculator.py)) và kiểm thử Dart ([`mobile/test/contrast_wcag_test.dart`](file:///d:/LifeOS/mobile/test/contrast_wcag_test.dart)). Đã tính lại 100% các cặp màu chữ/nền, nút/chữ nút, icon/nền cho cả 3 hướng (Light và Dark). Toàn bộ các cặp không đạt trong v1 đã được điều chỉnh màu và kiểm thử đạt $\ge 4.5:1$ (ví dụ: nút điểm danh hổ phách đổi chữ sang mực than sẫm `#141A17` trên nền `#E58E26` đạt **6.92:1**; chữ phụ `#3D4843` đạt **9.52:1**; chữ mờ đạt **6.99:1**). Bảng kết quả chạy tự động được nhúng nguyên văn vào §3. |
| **2** | **Ảnh dựng thật cho 3 hướng (12 ảnh)** | Đã xóa toàn bộ khung ASCII vẽ tay và số liệu bịa. Đã viết kiểm thử render thật ([`mobile/test/render_preview_goldens_test.dart`](file:///d:/LifeOS/mobile/test/render_preview_goldens_test.dart)) nạp font `LifeOsSans` và `MaterialIcons`, dùng chung bộ dữ liệu chuẩn `DemoWorkspaceSeeder` để chụp đủ **12 ảnh thật (3 hướng × 2 màn Hôm nay/Tài chính × Light/Dark, phone 390×844)** tại [`docs/review/mobile_screenshots/ui_upgrade/preview/`](file:///d:/LifeOS/docs/review/mobile_screenshots/ui_upgrade/preview/) và nhúng trực tiếp vào §3. Tại §2, thay bằng ảnh chụp thực tế bản hiện tại và liệt kê điểm yếu theo số. |
| **3** | **Ánh xạ đầy đủ tính năng → gói UPW** | Đã bổ sung Bảng ma trận ánh xạ 1-1 tại §6.3: toàn bộ 12 tính năng ở §6 đều có gói thực hiện cụ thể. Phạm vi đợt này gồm: các tính năng dùng dữ liệu có sẵn + Life Wheel + Focus Garden. 4 tính năng còn lại (Nhật ký tâm trạng, Nghi thức sáng/tối, Onboarding, Huy hiệu) được đánh dấu **"HOÃN"** và nêu rõ lý do để chờ người dùng duyệt đợt sau. |
| **4** | **Tên dữ liệu đúng theo spec** | Đã đối chiếu và thay thế 100% tên bảng và thực thể bằng `entityType` chuẩn trong `SYNC_PROTOCOL.md` §3.1 và 35 lớp `Table` trong `mobile/lib/core/database/drift_tables.dart`. Xác nhận: bảng `time_blocks` tồn tại trong Drift; toàn bộ 29 bảng đồng bộ lưu thuộc tính nghiệp vụ trong cột `payload_json TEXT`; sửa đúng 6 bảng hạ tầng (`sync_state`, `outbox`, `entity_base_snapshots`, `sync_log`, `scheduled_notifications`, `delivered_notifications`), xóa bỏ tên bịa `sync_meta` và `notifications`. |
| **5** | **Phân định rõ đánh giá tâm trạng** | Đã phân tích rạch ròi: Bước đánh giá tâm trạng của Review Wizard ghi trực tiếp vào trường `moodScore` (1–5) và `reflection` của thực thể `reviews` (đồng bộ với backend). Để tránh chồng lấn và gây khó hiểu cho người dùng, **hoãn tính năng `local_mood_logs`** khỏi đợt này (§6.2 & §6.3). |
| **6** | **Kho `local_extras` theo từng người dùng** | Đổi sang kiến trúc tệp SQLite riêng biệt theo `userId`: `lifeos_local_extras_<userId>.sqlite`. Khi Đăng xuất: giữ nguyên tệp trên máy để khi chính user đó đăng nhập lại thì còn nguyên (`[PROPOSED]`), người dùng khác không thể thấy. Khi Xóa tài khoản: xóa kho phụ khi ứng dụng xác nhận việc xóa hoàn tất; ghi rõ cơ chế phát hiện theo API hiện tại là `[OPEN]` và nêu 2 phương án. Việc xuất dữ liệu `LOCAL_ONLY` ghi `[OPEN]`. |
| **7** | **Tiêu chí hiệu năng đo đạc đúng nghĩa** | Đã tách bạch 2 tiêu chí định lượng: (1) Truy vấn trang đầu $< 1000\text{ms}$ với 10.000 bản ghi; (2) Tỷ lệ khung hình chậm (slow frames $> 16.6\text{ms}$) $< 5\%$ khi cuộn danh sách 10.000 mục đo bằng profile mode (`flutter run --profile`). Mục khung hình ghi trung thực "chưa đo trên profile mode" và đính kèm lệnh để người dùng chạy nghiệm thu. |
| **8** | **Màu, icon, font chuẩn hóa** | Đổi màu **Mục tiêu (Goals)** sang **Tím Hoàng Gia (`#7E22CE` Light / `#C084FC` Dark)**: tách biệt hoàn toàn khỏi Lịch (Chàm `#3730A3`) và Thông tin (Xanh lam `#0284C7`), đã kiểm tra mô phỏng mù màu (protanopia/deuteranopia). Mọi module có icon định danh riêng; trạng thái đi kèm text/icon, không dùng màu đơn độc. Rút gọn còn tối đa **2 họ font**: `Be Vietnam Pro` (chính) và `Plus Jakarta Sans` (tiêu đề), số liệu dùng `FontFeature.tabularFigures()`, bỏ hoàn toàn font monospaced; dung lượng assets $\approx 450\text{KB}$. |

---

## 1. Ranh giới kiến trúc & Giới hạn bất khả xâm phạm

Để đảm bảo an toàn tuyệt đối cho hệ thống dữ liệu phân tán và tính toàn vẹn của LifeOS:

| STT | Nguyên tắc bất khả xâm phạm | Cơ chế đảm bảo |
|:---:|---|---|
| **1** | **Không đụng chạm Backend & Hợp đồng API** | Cấm sửa bất kỳ tệp nào trong `backend/`, `docs/api/openapi.yaml`, `docs/engineering/SYNC_PROTOCOL.md` hoặc các fixture JSON chung. |
| **2** | **Không thay đổi CSDL gốc của LifeOS** | Không sửa đổi 29 bảng đồng bộ, 6 bảng hạ tầng (`outbox`, `sync_state`, `entity_base_snapshots`, `sync_log`, `scheduled_notifications`, `delivered_notifications`), không thêm giá trị vào `SyncEntityType`, không tăng `schemaVersion = 1` của `LifeOsDriftDatabase`. |
| **3** | **Không gửi dữ liệu mới lên Server** | Các tính năng mới `LOCAL_ONLY` bắt buộc phải lưu trong kho CSDL cục bộ riêng theo user (`lifeos_local_extras_<userId>.sqlite`), không bao giờ đẩy qua `SyncEngine` hay gọi endpoint không có trong OpenAPI. |
| **4** | **Bảo toàn tính toàn vẹn tài chính** | Toàn bộ tiền tệ dùng lớp `Money` (`Decimal10k`, số nguyên $\times 10^4$). Tuyệt đối **không dùng `double`** cho tiền bạc ở bất kỳ tầng nào; chuyển đổi chuỗi JSON ↔ số nguyên bằng xử lý chuỗi thập phân. |
| **5** | **Khả năng tiếp cận & Trải nghiệm người dùng** | 100% chuỗi hiển thị qua localization tiếng Việt; tương phản WCAG AA $\ge 4.5:1$ (đã xác thực bằng script); vùng chạm tương tác $\ge 48 \times 48\text{dp}$; hỗ trợ `TextScaler(2.0)` không tràn viền; hỗ trợ `Reduce Motion`. |

---

## 2. Kiểm toán giao diện hiện tại (UI Audit & Chú thích Điểm yếu)

Đối chiếu trực tiếp với các ảnh chụp màn hình kiểm thử thực tế của phiên bản hiện tại trong [`docs/review/mobile_screenshots/`](file:///d:/LifeOS/docs/review/mobile_screenshots/):

| Màn hình thực tế | Ảnh chụp hiện tại | Các điểm yếu cần khắc phục (Liệt kê theo số) |
|---|:---:|---|
| **Hôm nay (My Day) — Sáng** | ![My Day Light](file:///d:/LifeOS/docs/review/mobile_screenshots/phone_light_01_my_day.png) | 1. **Màu sắc đơn điệu:** Chỉ có hai mảng màu lặp lại là xanh lá `#1B6B50` và cam `#D97724`, thiếu bản sắc đa miền.<br>2. **Thẻ thiếu phân cấp (Elevation phẳng):** Các container có viền mỏng 1px phẳng lì, không có độ đổ bóng mềm tạo chiều sâu.<br>3. **Mật độ thông tin thấp:** Không có widget tóm tắt mức độ hoàn thành trong ngày (Rings/Tiến độ).<br>4. **Khoảng thở chưa tối ưu:** Danh sách việc và thói quen kéo dài khiến phải cuộn nhiều. |
| **Tài chính (Finance) — Sáng** | ![Finance Light](file:///d:/LifeOS/docs/review/mobile_screenshots/phone_light_03_finance.png) | 1. **Biểu đồ cột khô cứng:** Biểu đồ dòng tiền 7 ngày đơn sắc, thiếu phân bổ chi tiêu theo nhóm danh mục.<br>2. **Thẻ ví đơn sơ:** Danh sách tài khoản hiển thị dạng danh sách phẳng, chưa mang phong cách thẻ ngân hàng số cao cấp.<br>3. **Thiếu thước đo ngân sách:** Người dùng không nhìn thấy trực quan tốc độ tiêu dùng so với số ngày còn lại trong tháng. |
| **Hôm nay (My Day) — Tối** | ![My Day Dark](file:///d:/LifeOS/docs/review/mobile_screenshots/phone_dark_01_my_day.png) | 1. **Độ tương phản ban đêm chưa tinh tế:** Nền tối `#111513` hơi phẳng, thiếu các lớp bề mặt nổi (surface layers) để phân biệt các khu vực chức năng.<br>2. **Điểm nhấn màu phát quang vi tế:** Các chip trạng thái và icon chưa có độ sáng vi tế (ambient glow) để tạo cảm xúc tập trung. |
| **Kế hoạch & Tri thức** | ![Plan](file:///d:/LifeOS/docs/review/mobile_screenshots/phone_light_02_plan.png)<br>![Knowledge](file:///d:/LifeOS/docs/review/mobile_screenshots/phone_light_04_knowledge.png) | 1. **Kế hoạch:** Chỉ là danh sách sự kiện đơn điệu, thiếu dải chọn tuần tương tác ngang và góc nhìn dòng thời gian kết hợp công việc.<br>2. **Tri thức:** Thẻ ghi chú và mục tiêu dài hạn thiếu màu sắc thẻ và nhãn phân loại nguy cơ trực quan. |

---

## 3. Đề xuất 3 hướng thiết kế Premium

Cả 3 hướng đều giải quyết triệt để sự đơn điệu, bổ sung **bảng màu ngữ nghĩa riêng biệt cho từng Domain** và đã được **kiểm chứng 100% bằng script tính toán tương phản WCAG 2.x**.

### 3.1. Bảng Tỷ lệ Tương phản Toàn diện (Sinh tự động bằng Script)
> **Lệnh kiểm tra:** `python mobile/tool/contrast_calculator.py`  
> **Unit test Dart:** `flutter test --no-pub test/contrast_wcag_test.dart` (4/4 test passed)

| Hướng | Chế độ | Thành phần UI | Màu chữ/thành phần (FG) | Màu nền (BG) | Tỷ lệ (Script) | Ngưỡng WCAG AA | Kết quả |
|---|---|---|---|---|:---:|:---:|:---:|
| **A. Botanical** | Light | Chữ chính trên Nền kem | `#141A17` (Mực than sẫm) | `#F8F7F3` (Kem mỹ thuật) | **16.46:1** | $\ge 4.5:1$ | ✅ ĐẠT |
| **A. Botanical** | Light | Chữ chính trên Thẻ trắng | `#141A17` (Mực than sẫm) | `#FFFFFF` (Trắng bề mặt) | **17.65:1** | $\ge 4.5:1$ | ✅ ĐẠT |
| **A. Botanical** | Light | Chữ phụ trên Thẻ trắng | `#3D4843` (Xám rêu vừa) | `#FFFFFF` (Trắng bề mặt) | **9.52:1** | $\ge 4.5:1$ | ✅ ĐẠT |
| **A. Botanical** | Light | Chữ phụ trên Nền kem | `#3D4843` (Xám rêu vừa) | `#F8F7F3` (Kem mỹ thuật) | **8.88:1** | $\ge 4.5:1$ | ✅ ĐẠT |
| **A. Botanical** | Light | Chữ mờ trên Thẻ trắng | `#505C56` (Xám mờ đạt chuẩn) | `#FFFFFF` (Trắng bề mặt) | **6.99:1** | $\ge 4.5:1$ | ✅ ĐẠT |
| **A. Botanical** | Light | Xanh rừng (Task) trên Nền kem | `#135E44` (Xanh thông rừng) | `#F8F7F3` (Kem mỹ thuật) | **7.22:1** | $\ge 4.5:1$ | ✅ ĐẠT |
| **A. Botanical** | Light | Hổ phách chữ (Habit) trên Nền kem | `#8A3C01` (Hổ phách đậm chữ) | `#F8F7F3` (Kem mỹ thuật) | **7.18:1** | $\ge 4.5:1$ | ✅ ĐẠT |
| **A. Botanical** | Light | **Nút điểm danh: Chữ tối trên Nền cam** | `#141A17` (Mực than sẫm) | `#E58E26` (Cam hổ phách nút) | **6.92:1** | $\ge 4.5:1$ | ✅ ĐẠT |
| **A. Botanical** | Light | Nút đã điểm danh: Chữ xanh trên Thẻ trắng | `#135E44` (Xanh thông rừng) | `#FFFFFF` (Trắng bề mặt) | **7.74:1** | $\ge 4.5:1$ | ✅ ĐẠT |
| **A. Botanical** | Light | Mục tiêu (Tím hoàng gia) trên Thẻ trắng | `#7E22CE` (Tím hoàng gia) | `#FFFFFF` (Trắng bề mặt) | **6.98:1** | $\ge 4.5:1$ | ✅ ĐẠT |
| **A. Botanical** | Light | Lịch (Chàm sâu) trên Thẻ trắng | `#3730A3` (Chàm sâu) | `#FFFFFF` (Trắng bề mặt) | **9.93:1** | $\ge 4.5:1$ | ✅ ĐẠT |
| **A. Botanical** | Light | Đỏ chi tiêu trên Thẻ trắng | `#B91C1C` (Đỏ gạch chuẩn) | `#FFFFFF` (Trắng bề mặt) | **6.47:1** | $\ge 4.5:1$ | ✅ ĐẠT |
| **A. Botanical** | Light | Xanh thu nhập trên Thẻ trắng | `#15803D` (Xanh ngọc lục bảo) | `#FFFFFF` (Trắng bề mặt) | **5.02:1** | $\ge 4.5:1$ | ✅ ĐẠT |
| **A. Botanical** | Dark | Chữ chính trên Nền tối | `#EDF2EE` (Trắng ngọc) | `#0E1311` (Đá phiến ngọc) | **16.55:1** | $\ge 4.5:1$ | ✅ ĐẠT |
| **A. Botanical** | Dark | Chữ chính trên Thẻ tối | `#EDF2EE` (Trắng ngọc) | `#161E1A` (Nhung sẫm) | **15.01:1** | $\ge 4.5:1$ | ✅ ĐẠT |
| **A. Botanical** | Dark | Chữ phụ trên Thẻ tối | `#9BA9A1` (Xám bạc rêu) | `#161E1A` (Nhung sẫm) | **6.95:1** | $\ge 4.5:1$ | ✅ ĐẠT |
| **A. Botanical** | Dark | Chữ mờ trên Thẻ tối | `#7E8F87` (Xám rêu mờ chuẩn) | `#161E1A` (Nhung sẫm) | **4.99:1** | $\ge 4.5:1$ | ✅ ĐẠT |
| **A. Botanical** | Dark | Ngọc bích (Task) trên Thẻ tối | `#34D399` (Ngọc bích sáng) | `#161E1A` (Nhung sẫm) | **8.85:1** | $\ge 4.5:1$ | ✅ ĐẠT |
| **A. Botanical** | Dark | Vàng hổ phách (Habit) trên Thẻ tối | `#FBBF24` (Vàng hổ phách) | `#161E1A` (Nhung sẫm) | **10.19:1** | $\ge 4.5:1$ | ✅ ĐẠT |
| **A. Botanical** | Dark | Mục tiêu (Tím sáng) trên Thẻ tối | `#C084FC` (Tím sáng) | `#161E1A` (Nhung sẫm) | **6.44:1** | $\ge 4.5:1$ | ✅ ĐẠT |
| **A. Botanical** | Dark | Lịch (Chàm sáng) trên Thẻ tối | `#818CF8` (Chàm sáng) | `#161E1A` (Nhung sẫm) | **5.70:1** | $\ge 4.5:1$ | ✅ ĐẠT |
| **B. Aurora** | Light | Chữ chính trên Nền sương lam | `#0B132B` (Xanh đêm sâu) | `#F4F6FB` (Sương sớm lam) | **17.00:1** | $\ge 4.5:1$ | ✅ ĐẠT |
| **B. Aurora** | Light | Chữ phụ trên Thẻ trắng | `#334155` (Xám đá lam) | `#FFFFFF` (Trắng tinh thể) | **10.35:1** | $\ge 4.5:1$ | ✅ ĐẠT |
| **B. Aurora** | Light | Lam ngọc (Cyan) trên Nền sương lam | `#0369A1` (Lam ngọc đậm) | `#F4F6FB` (Sương sớm lam) | **5.49:1** | $\ge 4.5:1$ | ✅ ĐẠT |
| **B. Aurora** | Light | San hô (Coral) trên Nền sương lam | `#BE123C` (San hô đậm) | `#F4F6FB` (Sương sớm lam) | **5.81:1** | $\ge 4.5:1$ | ✅ ĐẠT |
| **B. Aurora** | Light | Mục tiêu (Tím cực quang) trên Thẻ trắng | `#6D28D9` (Tím cực quang) | `#FFFFFF` (Trắng tinh thể) | **7.10:1** | $\ge 4.5:1$ | ✅ ĐẠT |
| **B. Aurora** | Dark | Chữ chính trên Thẻ đêm | `#F8FAFC` (Trắng ánh lam) | `#131B2E` (Bề mặt tinh thể) | **16.40:1** | $\ge 4.5:1$ | ✅ ĐẠT |
| **B. Aurora** | Dark | Chữ phụ trên Thẻ đêm | `#94A3B8` (Xám bạc lam) | `#131B2E` (Bề mặt tinh thể) | **6.69:1** | $\ge 4.5:1$ | ✅ ĐẠT |
| **B. Aurora** | Dark | Cyan Neon trên Thẻ đêm | `#38BDF8` (Cyan Neon) | `#131B2E` (Bề mặt tinh thể) | **8.01:1** | $\ge 4.5:1$ | ✅ ĐẠT |
| **B. Aurora** | Dark | San hô Neon trên Thẻ đêm | `#FB7185` (San hô Neon) | `#131B2E` (Bề mặt tinh thể) | **6.38:1** | $\ge 4.5:1$ | ✅ ĐẠT |
| **B. Aurora** | Dark | Tím Neon trên Thẻ đêm | `#A78BFA` (Tím Neon) | `#131B2E` (Bề mặt tinh thể) | **6.30:1** | $\ge 4.5:1$ | ✅ ĐẠT |
| **C. Ivory & Ink** | Light | Mực carbon trên Nền ngà voi | `#111111` (Mực carbon) | `#FAF9F6` (Ngà voi) | **17.94:1** | $\ge 4.5:1$ | ✅ ĐẠT |
| **C. Ivory & Ink** | Light | Chữ phụ trên Thẻ trắng | `#444444` (Xám chì đậm) | `#FFFFFF` (Trắng tinh khiết) | **9.74:1** | $\ge 4.5:1$ | ✅ ĐẠT |
| **C. Ivory & Ink** | Light | Ngọc lục bảo Thụy Sĩ trên Nền ngà | `#0F5132` (Ngọc lục bảo Thụy Sĩ) | `#FAF9F6` (Ngà voi) | **8.89:1** | $\ge 4.5:1$ | ✅ ĐẠT |
| **C. Ivory & Ink** | Light | Mục tiêu (Thạch anh) trên Thẻ trắng | `#581C87` (Thạch anh sẫm) | `#FFFFFF` (Trắng tinh khiết) | **10.88:1** | $\ge 4.5:1$ | ✅ ĐẠT |
| **C. Ivory & Ink** | Dark | Chữ chính trên Thẻ than | `#F5F5F5` (Trắng mực in) | `#1E1E1E` (Bề mặt than) | **15.29:1** | $\ge 4.5:1$ | ✅ ĐẠT |
| **C. Ivory & Ink** | Dark | Chữ phụ trên Thẻ than | `#A3A3A3` (Xám bạc) | `#1E1E1E` (Bề mặt than) | **6.61:1** | $\ge 4.5:1$ | ✅ ĐẠT |
| **C. Ivory & Ink** | Dark | Ngọc lục bảo sáng trên Thẻ than | `#4EAE7C` (Ngọc lục bảo sáng) | `#1E1E1E` (Bề mặt than) | **6.09:1** | $\ge 4.5:1$ | ✅ ĐẠT |
| **C. Ivory & Ink** | Dark | Mục tiêu sáng trên Thẻ than | `#C084FC` (Thạch anh sáng) | `#1E1E1E` (Bề mặt than) | **6.31:1** | $\ge 4.5:1$ | ✅ ĐẠT |

---

### 3.2. Ảnh dựng thật 3 Hướng (12 ảnh render từ Golden Test Phone 390×844)

> Toàn bộ 12 ảnh dưới đây được sinh tự động bởi tệp kiểm thử [`mobile/test/render_preview_goldens_test.dart`](file:///d:/LifeOS/mobile/test/render_preview_goldens_test.dart) dùng đúng bộ seed dữ liệu chuẩn `DemoWorkspaceSeeder.defaultFixedSeedUtc`:

#### Hướng A: Botanical Premium (KHUYẾN NGHỊ — Sang trọng Hữu cơ)
*Triết lý: Giấy kem ấm dập nổi, xanh thông rừng sâu, hổ phách ấm, bề mặt đa lớp tạo chiều sâu tĩnh tại.*

| Chế độ | Màn hình Hôm nay (My Day) | Màn hình Tài chính (Finance) |
|:---:|:---:|:---:|
| **Sáng (Light)** | ![A Light My Day](file:///d:/LifeOS/docs/review/mobile_screenshots/ui_upgrade/preview/preview_dir_a_light_my_day.png) | ![A Light Finance](file:///d:/LifeOS/docs/review/mobile_screenshots/ui_upgrade/preview/preview_dir_a_light_finance.png) |
| **Tối (Dark)** | ![A Dark My Day](file:///d:/LifeOS/docs/review/mobile_screenshots/ui_upgrade/preview/preview_dir_a_dark_my_day.png) | ![A Dark Finance](file:///d:/LifeOS/docs/review/mobile_screenshots/ui_upgrade/preview/preview_dir_a_dark_finance.png) |

---

#### Hướng B: Midnight Aurora (Huyền diệu Công nghệ)
*Triết lý: Nền chàm đêm thẳm, cực quang ngọc lam (Cyan Neon), san hô phát quang (Coral Neon) hiện đại.*

| Chế độ | Màn hình Hôm nay (My Day) | Màn hình Tài chính (Finance) |
|:---:|:---:|:---:|
| **Sáng (Light)** | ![B Light My Day](file:///d:/LifeOS/docs/review/mobile_screenshots/ui_upgrade/preview/preview_dir_b_light_my_day.png) | ![B Light Finance](file:///d:/LifeOS/docs/review/mobile_screenshots/ui_upgrade/preview/preview_dir_b_light_finance.png) |
| **Tối (Dark)** | ![B Dark My Day](file:///d:/LifeOS/docs/review/mobile_screenshots/ui_upgrade/preview/preview_dir_b_dark_my_day.png) | ![B Dark Finance](file:///d:/LifeOS/docs/review/mobile_screenshots/ui_upgrade/preview/preview_dir_b_dark_finance.png) |

---

#### Hướng C: Ivory & Ink (Tạp chí Biên tập Thụy Sĩ)
*Triết lý: Tối giản tuyệt đối, màu ngà voi và mực in carbon, viền siêu mảnh 1px, một màu ngọc lục bảo độc bản.*

| Chế độ | Màn hình Hôm nay (My Day) | Màn hình Tài chính (Finance) |
|:---:|:---:|:---:|
| **Sáng (Light)** | ![C Light My Day](file:///d:/LifeOS/docs/review/mobile_screenshots/ui_upgrade/preview/preview_dir_c_light_my_day.png) | ![C Light Finance](file:///d:/LifeOS/docs/review/mobile_screenshots/ui_upgrade/preview/preview_dir_c_light_finance.png) |
| **Tối (Dark)** | ![C Dark My Day](file:///d:/LifeOS/docs/review/mobile_screenshots/ui_upgrade/preview/preview_dir_c_dark_my_day.png) | ![C Dark Finance](file:///d:/LifeOS/docs/review/mobile_screenshots/ui_upgrade/preview/preview_dir_c_dark_finance.png) |

---

## 4. Hệ thống Design Tokens mở rộng (Kiểu chữ, Màu sắc & Icon)

### 4.1. Rút gọn Bộ Phông chữ: Tối đa 2 Họ Phông (Không dùng Monospace)
Để tối ưu dung lượng tải ứng dụng và độ sắc nét tiếng Việt, bộ font được tinh giản còn **2 họ phông bản quyền mở SIL Open Font License 1.1**:
1. **Be Vietnam Pro (OFL 1.1):** Phông chữ chính cho toàn bộ nội dung văn bản (`bodyLarge`, `bodyMedium`, `labelSmall`, `titleMedium`).
2. **Plus Jakarta Sans (OFL 1.1):** Phông chữ hình học hiện đại dùng cho tiêu đề lớn (`displaySmall`, `headlineMedium`).
3. **Số liệu tài chính, giờ, chuỗi streak:** Sử dụng tính năng **`FontFeature.tabularFigures()`** của chính phông chữ `Be Vietnam Pro` để các chữ số có độ rộng đồng đều (proportional numeric alignment), **không dùng phông đơn cách (monospaced) cồng kềnh**.
- **Dung lượng đóng gói:** 4 tệp TTF chuẩn (`Regular`, `Italic`, `Medium`, `Bold`) tổng cộng khoảng **~450 KB**, nằm gọn trong `assets/fonts/`.

### 4.2. Màu sắc Định danh từng Module & Kiểm tra Mù màu (Colorblind Accessibility)
Mỗi module có một màu định danh và **biểu tượng cố định**, tuyệt đối không dùng màu sắc làm tín hiệu nhận biết duy nhất:

| Module | Màu định danh (Light / Dark) | Biểu tượng cố định | Mô phỏng mù màu (Protanopia / Deuteranopia) | Dấu hiệu phụ trợ bắt buộc |
|---|---|---|---|---|
| **Công việc (Task)** | Xanh rừng `#135E44` / `#34D399` | `Icons.task_alt_rounded` | Hiển thị sắc vàng rêu/nâu đất | Huy hiệu chữ ưu tiên: `[Không]`, `[Thấp]`, `[Trung bình]`, `[Cao]` |
| **Thói quen (Habit)** | Hổ phách `#8A3C01` / `#FBBF24` | `Icons.local_fire_department_rounded` | Hiển thị sắc vàng tươi | Huy hiệu chuỗi ngày: `🔥 12 ngày` + nút có chữ `"Điểm danh"` / `"Đã điểm danh"` |
| **Tài chính (Finance)** | Lục bảo `#0F766E` / San hô `#B91C1C` | `Icons.account_balance_wallet_rounded` | Thu/chi phân biệt rõ bằng dấu `+` và `-` | Dấu cộng `+` (Thu) / Dấu trừ `-` (Chi) kèm nhãn loại ví bằng chữ |
| **Lịch trình (Calendar)** | Chàm sâu `#3730A3` / `#818CF8` | `Icons.calendar_today_rounded` | Hiển thị sắc xanh lam đậm | Nhãn khung giờ cụ thể (ví dụ `09:00 - 10:30`) |
| **Tri thức (Knowledge)** | Tím mận `#6D28D9` / `#A78BFA` | `Icons.menu_book_rounded` | Hiển thị sắc xanh tím nhạt | Chip thẻ `🏷 Danh mục` và số lượng liên kết |
| **Mục tiêu (Goals)** | **Tím hoàng gia `#7E22CE` / `#C084FC`** | `Icons.flag_rounded` | Hiển thị sắc xám tím đặc trưng, tách biệt hẳn chàm và xanh | Nhãn trạng thái chữ: `[Đúng hạn]`, `[⚠️ Có nguy cơ]`, `[⏳ Quá hạn]` |

---

## 5. Cải tiến bố cục chi tiết theo từng Tab

### 5.1. Tab 1: Hôm nay (My Day)
- **Vấn đề v1:** Danh sách việc và thói quen phẳng lì, thiếu tổng quan nhịp ngày, không có khoảng thở.
- **Giải pháp v2:**
  1. **Life Rings Widget (Vòng đời hôm nay):** 3 vòng tròn tiến độ đồng tâm vẽ bằng `CustomPainter` (Việc 60% - Thói quen 100% - Ngân sách chi tiêu 45%) ở đầu trang.
  2. **Khu vực Trọng tâm sáng (Focus Top 3):** Ghim 3 việc quan trọng nhất lên đầu với thẻ nổi elevation cấp 2.
  3. **Thẻ Thói quen nâng cấp:** Nút điểm danh chữ tối trên nền cam hổ phách `#E58E26` (đạt tương phản 6.92:1), nút đã điểm danh viền xanh có icon tích.

### 5.2. Tab 2: Kế hoạch (Plan & Calendar)
- **Vấn đề v1:** Danh sách sự kiện cuộn dọc khô cứng, không thấy khoảng trống giữa các cuộc họp.
- **Giải pháp v2:**
  1. **Thanh chọn tuần tương tác (Interactive Week Strip):** Vuốt ngang 7 ngày, chấm trạng thái công việc dưới từng ngày.
  2. **Visual Daily Timeline Canvas:** Trục thời gian 24h hiển thị trực quan các khối sự kiện (`calendar_events`), khối tập trung (`time_blocks`) và công việc có giờ hẹn (`tasks`).

### 5.3. Tab 3: Tài chính (Finance)
- **Vấn đề v1:** Chỉ có biểu đồ cột 7 ngày đơn sắc; thẻ ví giống danh sách lặp; thiếu trực quan ngân sách.
- **Giải pháp v2:**
  1. **Thẻ Ví Điện tử Tinh xảo:** Hiển thị dạng lưới thẻ nổi, phân biệt tài khoản ngân hàng, ví điện tử, tiền mặt kèm nút ẩn/hiện số tiền an toàn (`•••••• ₫`).
  2. **Biểu đồ Vành khuyên (Donut Chart):** Phân bổ tỷ trọng chi tiêu (Ăn uống, Nhà cửa, Mua sắm) có chú giải phần trăm và chạm tương tác.
  3. **Thước đo Ngân sách (Budget Pace Gauge):** Thanh đo nhiệt trực quan so sánh tốc độ chi thực tế với tốc độ chi lý tưởng theo ngày trong tháng.

### 5.4. Tab 4: Tri thức & Mục tiêu (Growth)
- **Vấn đề v1:** Ghi chú dạng khối chữ đơn điệu; mục tiêu chỉ có thanh bar % thô sơ.
- **Giải pháp v2:**
  1. **Thẻ Ghi chú có Màu nhấn (Note Accent Cards):** Hiển thị thẻ gắn kèm chip danh mục và số lượng `note_links`.
  2. **Thẻ Mục tiêu Đa mốc (Milestone Goal Cards):** Phân đoạn tiến độ theo từng cột mốc (`milestones`), đi kèm huy hiệu phân loại rủi ro bằng chữ và màu tương phản.

### 5.5. Tab 5: Tổng quan & Nhìn lại (Insights & Review)
- **Vấn đề v1:** Khu vực phản tư giống bảng nhập liệu văn phòng.
- **Giải pháp v2:**
  1. **Interactive Guided Review Wizard:** Quy trình 3 bước dẫn dắt tổng kết tuần/tháng (1. Số liệu tổng hợp $\rightarrow$ 2. Đánh giá tâm trạng & năng lượng $\rightarrow$ 3. Đúc kết bài học).
  2. **Thiết kế Cài đặt Ngăn nắp:** Nhóm các mục Cài đặt, Thiết bị, Xuất dữ liệu và Chẩn đoán đồng bộ theo các khối thẻ bo tròn sạch sẽ.

---

## 6. Danh mục Tính năng & Ánh xạ Gói công việc (UPW)

### 6.1. Nhóm Tính năng dùng DỮ LIỆU CÓ SẴN (Chỉ đọc từ SQLite gốc, 0 bảng mới)
Toàn bộ dữ liệu được đọc từ các bảng đồng bộ hiện có trong `mobile/lib/core/database/drift_tables.dart` (lưu trong cột `payload_json`):

1. **Vòng đời hôm nay (Life Rings):** Tính từ `tasks` (trường `status`), `habit_checkins` (`status = 'DONE'`), `financial_transactions` (`type = 'EXPENSE'`) và `budgets` (`amount`).
2. **Dòng thời gian hôm nay (Timeline):** Đọc từ `calendar_events` (`start_time`, `end_time`), `time_blocks` (`start_at`, `end_at`), `tasks` (`due_time`), `routines` (`target_time`).
3. **Bản đồ nhiệt Thói quen (Heatmap):** Đọc từ lịch sử `habit_checkins` (`local_date`, `status`).
4. **Phân tích Tài chính Vành khuyên (Donut Chart):** Đọc từ `financial_transactions` (`amount`, `category_id`), `financial_categories` (`name`) và `budgets`.
5. **Interactive Review Wizard:** Đọc và ghi trực tiếp vào thực thể `reviews` (`period_type`, `period_key`, `mood_score`, `reflection`).
6. **Tìm kiếm Toàn cục Offline:** Truy vấn `LIKE` từ khóa trên các bảng `tasks` (`title`, `description`), `notes` (`title`, `content`), `financial_transactions` (`description`), `calendar_events` (`title`).

### 6.2. Nhóm Tính năng dùng KHO CỤC BỘ RIÊNG (`LOCAL_ONLY` — §7)
Lưu trong CSDL phụ `lifeos_local_extras_<userId>.sqlite`:

1. **Bánh xe Cuộc sống (Life Wheel):** Tự chấm điểm 8 khía cạnh (1–10), vẽ biểu đồ Radar so sánh các lần đánh giá. Lưu tại bảng `local_life_wheel_assessments`.
2. **Khu vườn Tập trung (Focus Garden / Pomodoro):** Đồng hồ bấm giờ tập trung (25/50p), mỗi phiên hoàn thành mọc một cái cây ảo gắn với `task_id`. Lưu tại bảng `local_focus_sessions`.

### 6.3. Bảng Ánh xạ Toàn diện "Tính năng → Gói UPW" & Phạm vi Đợt này

| STT | Tính năng | Nguồn dữ liệu | Bảng & Cột dữ liệu truy vấn | Gói UPW thực hiện | Trạng thái phạm vi |
|:---:|---|---|---|:---:|:---:|
| 1 | **Life Rings (Vòng đời hôm nay)** | CSDL gốc | `tasks`, `habit_checkins`, `financial_transactions`, `budgets` | **UPW4** | **Triển khai đợt này** |
| 2 | **Dòng thời gian hôm nay (Timeline)** | CSDL gốc | `calendar_events`, `time_blocks`, `tasks`, `routines` | **UPW4** | **Triển khai đợt này** |
| 3 | **Bản đồ nhiệt Thói quen (Heatmap)** | CSDL gốc | `habit_checkins` (`local_date`, `status`, `habit_id`) | **UPW4** | **Triển khai đợt này** |
| 4 | **Donut Tài chính & Thước đo Ngân sách** | CSDL gốc | `financial_transactions`, `financial_categories`, `budgets` | **UPW5** | **Triển khai đợt này** |
| 5 | **Interactive Review Wizard** | CSDL gốc | `reviews` (`period_type`, `period_key`, `mood_score`, `reflection`) | **UPW6** | **Triển khai đợt này** |
| 6 | **Tìm kiếm Toàn cục Offline** | CSDL gốc | `tasks`, `notes`, `financial_transactions`, `calendar_events` | **UPW6** | **Triển khai đợt này** |
| 7 | **Bánh xe Cuộc sống (Life Wheel Radar)** | `LOCAL_ONLY` | `local_life_wheel_assessments` trong `lifeos_local_extras_<userId>.db` | **UPW7** | **Triển khai đợt này** |
| 8 | **Khu vườn Tập trung (Focus Garden)** | `LOCAL_ONLY` | `local_focus_sessions` trong `lifeos_local_extras_<userId>.db` | **UPW7** | **Triển khai đợt này** |
| 9 | **Nhật ký Tâm trạng riêng lẻ (`local_mood_logs`)** | `LOCAL_ONLY` | — | — | **HOÃN (Đã tích hợp vào Review Wizard qua thực thể `reviews`)** |
| 10 | **Bộ Nghi thức Sáng/Tối (Rituals Player)** | `LOCAL_ONLY` | `local_rituals`, `local_ritual_steps` | — | **HOÃN (Chờ người dùng duyệt đợt sau)** |
| 11 | **Màn Onboarding chào mừng lần đầu** | `LOCAL_ONLY` | Preferences `is_onboarding_completed` | — | **HOÃN (Chờ người dùng duyệt đợt sau)** |
| 12 | **Huy hiệu & Cột mốc cá nhân (Badges)** | `LOCAL_ONLY` | `local_badges` | — | **HOÃN (Chờ người dùng duyệt đợt sau)** |

---

## 7. Kiến trúc Kho dữ liệu cục bộ riêng (`lifeos_local_extras_<userId>.sqlite`)

### 7.1. Cô lập dữ liệu theo từng Người dùng (`userId`)
- Tương tự như CSDL gốc (`lifeos_<userId>.sqlite`), CSDL phụ được tách theo từng user: **`lifeos_local_extras_<userId>.sqlite`**.
- *Lý do chọn file riêng thay vì cột `user_id`:* Cô lập vật lý hoàn toàn ở cấp độ tệp; khi thiết bị có nhiều tài khoản đăng nhập luân phiên, không có bất kỳ khả năng nào rò rỉ dữ liệu ngoài ý muốn do quên mệnh đề `WHERE user_id = ...`.

### 7.2. Đặc tả CSDL Phụ `LifeOsLocalExtrasDatabase`
1. **Lớp Drift riêng biệt:** `LifeOsLocalExtrasDatabase` (`schemaVersion = 1`), không liên kết với `LifeOsDriftDatabase`.
2. **Các bảng tiền tố `local_*`:**
   - `local_life_wheel_assessments (id TEXT PK, recorded_at INT, career INT, finance INT, health INT, relationships INT, growth INT, family INT, leisure INT, environment INT, notes TEXT)`
   - `local_focus_sessions (id TEXT PK, task_id TEXT, started_at INT, duration_minutes INT, completed INT, tag TEXT)`
3. **Không có cột `sync_state`, không có `version`, không ghi vào `outbox`, không gọi API.**
4. **Tài liệu theo dõi:** Được ghi chép và cập nhật tại [`docs/review/MOBILE_LOCAL_ONLY_FEATURES.md`](file:///d:/LifeOS/docs/review/MOBILE_LOCAL_ONLY_FEATURES.md).

### 7.3. Hành vi khi Đăng xuất & Xóa tài khoản
- **Khi Đăng xuất (Logout):** Giữ nguyên tệp `lifeos_local_extras_<userId>.sqlite` trên máy (`[PROPOSED]`). Khi user đó đăng nhập lại, dữ liệu được nạp lại nguyên vẹn. User khác đăng nhập trên cùng máy sẽ mở tệp riêng của họ.
- **Khi Xóa tài khoản (Account Deletion):** Ứng dụng xóa hoàn toàn cả tệp CSDL gốc và tệp CSDL phụ khi xác nhận xóa tài khoản thành công.
  - *Cơ chế phát hiện xóa tài khoản hoàn tất (`[OPEN]`):*
    - *Phương án A (Khuyến nghị):* Sau khi người dùng nhập mã xác nhận xóa (`POST /api/v1/account/deletion`), app nhận mã thành công kèm `deletionCooldownUntil` (7 ngày). App thực hiện đăng xuất ngay và đánh dấu trạng thái "đang chờ xóa". Khi hết 7 ngày, nếu user đăng nhập lại sẽ nhận lỗi 401/404 từ server $\rightarrow$ app xóa sạch toàn bộ tệp local.
    - *Phương án B:* Xóa sạch ngay lập tức toàn bộ tệp local ngay khi gọi thành công `POST /api/v1/account/deletion`. Nếu người dùng muốn hủy xóa trong 7 ngày, họ phải tạo dữ liệu lại từ đầu hoặc khôi phục qua web/hỗ trợ.
- **Xuất dữ liệu cục bộ (`[OPEN]`):** Dữ liệu `LOCAL_ONLY` có được gộp vào tệp JSON xuất dữ liệu cục bộ hay không (Đề xuất: có gộp dưới mục `"local_extras"` riêng).

---

## 8. Quy chuẩn 5 trạng thái giao diện (Design System States)

Mọi màn hình và widget đều hỗ trợ đầy đủ 5 trạng thái:
1. **Đang tải (Loading):** `ShimmerSkeletonLoader` quét nhẹ nhàng theo khung thẻ, giữ nguyên bố cục để không giật màn hình (layout shift).
2. **Rỗng (Empty):** `EmptyStateView` với hình vẽ vector tối giản + thông điệp tích cực + nút CTA hành động một chạm.
3. **Lỗi (Error):** `InlineErrorBanner` hiển thị tiếng Việt tự nhiên + nút `[ Thử lại ]`, không hiện mã lỗi kỹ thuật.
4. **Ngoại tuyến (Offline):** `OfflineStatusPill` hiển thị thanh thông báo dịu nhẹ *"Ngoại tuyến · Đã lưu an toàn trên máy"*, không chặn thao tác của người dùng.
5. **Thành công (Success):** `FeedbackToast` / `SuccessSnackBar` hiển thị tức thì với hoạt ảnh dấu tích $180\text{ms}$ + rung xúc giác nhẹ `HapticFeedback.lightImpact()` + nút `[ Hoàn tác ]` (5 giây).

---

## 9. Kế hoạch Kiểm thử & Tiêu chí Hiệu năng Đo đạc Đúng nghĩa

### 9.1. Tiêu chí Hiệu năng Định lượng Tách bạch
1. **Thời gian Truy vấn CSDL (Database Query Performance):**
   - Tải và render trang đầu $< 1000\text{ms}$ với cơ sở dữ liệu có $10.000$ bản ghi trong SQLite.
   - *Phương pháp đo:* Benchmark test đo bằng `Stopwatch` trong `test/domain_modules_and_perf_test.dart`.
2. **Tốc độ Khung hình Giao diện (Frame Rendering & Scrolling Smoothness):**
   - Tỷ lệ khung hình chậm (slow frames $> 16.6\text{ms}$) $< 5\%$ khi cuộn liên tục danh sách $10.000$ mục.
   - *Phương pháp đo:* Chạy integration test trên thiết bị thật hoặc máy ảo ở chế độ profile mode (`flutter run --profile test_driver/...` kết hợp `FrameTiming` listener).
   - *Ghi chú trung thực v2:* **"Chưa đo trên profile mode"** (sẽ thực hiện và báo cáo trong gói UPW8; lệnh chạy: `flutter drive --profile --target=test_driver/perf_scroll_test.dart`).

### 9.2. Cổng chất lượng bắt buộc (Quality Gate)
- `dart analyze`: Đạt `No issues found!` (0 lỗi, 0 cảnh báo).
- `flutter test` (Unit/Widget/Golden): Đạt 100% test xanh ngắt (35 test hiện có + bộ golden preview + WCAG test).
- **Golden Test:** 0 pixel sai khác khi chạy 2 lần liên tiếp.
- **E2E Real Backend (`LIFEOS_RUN_REAL_E2E=true`):** 9 luồng (a–i) giao tiếp với backend thật trên Docker (`http://localhost:8080`) phải đạt **100% xanh**, chứng minh việc nâng cấp UI và kho phụ không gây bất kỳ ảnh hưởng nào lên CSDL gốc hay giao thức đồng bộ.

---

## 10. Lộ trình Gói công việc (UPW1 – UPW8)

| Gói | Tên gói công việc | Nội dung chi tiết | Tiêu chí nghiệm thu |
|---|---|---|---|
| **UPW1** | **Design Tokens Mở rộng & Phông chữ** | Đóng gói phông chữ `Be Vietnam Pro` + `Plus Jakarta Sans` vào `assets/fonts/`; cập nhật `app_tokens.dart` với bảng màu v2 đã qua script kiểm tra tương phản; cấu hình `FontFeature.tabularFigures()`. | `dart analyze` sạch; unit test WCAG đạt 100% $\ge 4.5:1$. |
| **UPW2** | **Bộ Widget Nền tảng & 5 Trạng thái** | Nâng cấp `lifeos_widgets.dart`: Thẻ Card đổ bóng 3 lớp, Shimmer Skeleton, Empty State có minh họa, Toast phản hồi, Button 3 trạng thái. | Widget test kiểm tra đầy đủ 5 trạng thái giao diện; touch target $\ge 48\text{dp}$. |
| **UPW3** | **Kho CSDL Cục bộ `local_extras` theo User** | Khởi tạo `LifeOsLocalExtrasDatabase` lưu file `lifeos_local_extras_<userId>.sqlite`, các bảng `local_*`, repository cục bộ riêng và tài liệu `MOBILE_LOCAL_ONLY_FEATURES.md`. | Unit test CRUD kho local extras độc lập; E2E backend thật vẫn xanh 100%. |
| **UPW4** | **Nâng cấp Tab Hôm nay (My Day) & Life Rings** | Tích hợp widget Vòng đời hôm nay (Life Rings), làm mới thẻ việc trọng tâm, Dòng thời gian hôm nay, Bản đồ nhiệt thói quen. | Golden test màn Hôm nay (Light/Dark × Phone/Tablet) đạt chuẩn; thao tác một chạm mượt mà. |
| **UPW5** | **Nâng cấp Tab Tài chính & Biểu đồ Donut** | Làm mới thẻ ví, vẽ biểu đồ vành khuyên (Donut Chart) chi tiêu theo danh mục, thước đo ngân sách (Pace Gauge). | Biểu đồ render chuẩn xác, có nhãn và chú giải tiếng Việt; tính toán tiền 0 `double`. |
| **UPW6** | **Nâng cấp Tab Kế hoạch, Tri thức & Review Wizard** | Bổ sung thanh tuần ngang, làm mới thẻ ghi chú và mục tiêu đa mốc, tích hợp Interactive Guided Review Wizard và Tìm kiếm toàn cục. | Bố cục thoáng đãng, phân cấp thị giác rõ rệt; hỗ trợ `TextScaler(2.0)` không tràn viền. |
| **UPW7** | **Màn hình Mới (Life Wheel & Focus Garden)** | Xây dựng màn hình Bánh xe cuộc sống (Life Wheel Radar) và Khu vườn tập trung (Focus Garden / Pomodoro) lưu trong kho phụ. | 2 tính năng `LOCAL_ONLY` hoạt động trơn tru; có cảnh báo dữ liệu trên máy trong Cài đặt. |
| **UPW8** | **Chụp lại Golden, Đo Benchmark Profile & Báo cáo** | Chụp toàn bộ bộ ảnh golden chính thức trước/sau tại `mobile_screenshots/ui_upgrade/`, chạy benchmark đo khung hình profile mode, E2E thật và hoàn thiện báo cáo cuối. | Toàn bộ test xanh (gồm cả test E2E thật); golden 0 pixel sai khác; tài liệu đầy đủ. |

---

## 11. Danh sách Quyết định `[PROPOSED]`, `[OPEN]` & Câu hỏi cần người dùng chốt

### 11.1. Quyết định Đề xuất `[PROPOSED]`
- **UP-D01 `[PROPOSED]`:** Áp dụng hệ thống Design Tokens mở rộng với màu nhận diện riêng cho từng Domain (Việc, Thói quen, Tài chính, Lịch, Tri thức, Mục tiêu) trên cùng một nền tảng sắc độ hài hòa, 100% đạt tương phản WCAG AA $\ge 4.5:1$.
- **UP-D02 `[PROPOSED]`:** Khởi tạo kho CSDL SQLite riêng biệt theo từng user `lifeos_local_extras_<userId>.sqlite` (quản lý bởi Drift độc lập, các bảng tiền tố `local_*`) để phục vụ các tính năng mở rộng chưa có backend, cách ly 100% khỏi CSDL đồng bộ gốc.
- **UP-D03 `[PROPOSED]`:** Giữ lại tệp `lifeos_local_extras_<userId>.sqlite` trên máy khi đăng xuất, đảm bảo người dùng khác đăng nhập trên cùng thiết bị không thể truy cập dữ liệu của nhau.
- **UP-D04 `[PROPOSED]`:** Hoãn 4 tính năng mở rộng (`local_mood_logs`, `local_rituals`, `onboarding`, `local_badges`) khỏi đợt này; đánh giá tâm trạng được thực hiện 100% qua thực thể `reviews` của backend.

### 11.2. Các điểm mở `[OPEN]` & Câu hỏi xin ý kiến người dùng

Kính đề nghị người dùng xem xét và cho ý kiến về các câu hỏi sau để chốt triển khai Giai đoạn 2:

1. **Lựa chọn Hướng thiết kế Chủ đạo:**
   - Dựa trên **12 ảnh render thật** tại §3.2, bạn chọn hướng thiết kế nào:
     - **Hướng A (Botanical Premium — Khuyến nghị):** Giấy kem ấm, xanh thông rừng sâu, hổ phách ấm, sang trọng hữu cơ.
     - **Hướng B (Midnight Aurora):** Đêm thẳm, cực quang ngọc lam, hồng san hô, đậm chất công nghệ tương lai.
     - **Hướng C (Ivory & Ink):** Tạp chí Thụy Sĩ tối giản, ngà voi & mực carbon, ngọc lục bảo độc bản.
2. **Cơ chế Xóa sạch Dữ liệu khi Xóa tài khoản (`[OPEN]`):**
   - Khi người dùng gọi thành công `POST /api/v1/account/deletion` (bắt đầu 7 ngày chờ):
     - *Phương án A (Khuyến nghị):* Giữ dữ liệu trên máy ở trạng thái khóa. Nếu sau 7 ngày đăng nhập lại nhận lỗi 401/404 $\rightarrow$ app tự động xóa sạch toàn bộ tệp local (`lifeos_<userId>.sqlite` và `lifeos_local_extras_<userId>.sqlite`).
     - *Phương án B:* Xóa sạch ngay lập tức toàn bộ tệp local ngay khi bấm xác nhận xóa tài khoản.
3. **Dữ liệu `LOCAL_ONLY` trong Xuất dữ liệu Cục bộ (`[OPEN]`):**
   - Khi người dùng bấm "Xuất dữ liệu trên máy" trong Cài đặt, bạn có muốn gộp cả dữ liệu của Bánh xe cuộc sống và Khu vườn tập trung vào tệp JSON xuất ra (dưới khóa `"local_extras"`) không?

---

> **KẾT THÚC BẢN KẾ HOẠCH V2.**  
> Agent tạm dừng tại đây và chờ người dùng chọn hướng thiết kế cùng phê duyệt trước khi bắt đầu thực hiện Giai đoạn 2.
