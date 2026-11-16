# Prompt nâng cấp giao diện Mobile — Premium & Polish (`ANTIGRAVITY_UI_UPGRADE_PROMPT.md`)

> Gửi nguyên văn cho Antigravity. Vai trò: bạn là Product Designer + Flutter UI Engineer. Làm theo hai giai đoạn: **(1) lập kế hoạch rồi DỪNG chờ duyệt, (2) triển khai sau khi người dùng duyệt.** Không bỏ qua giai đoạn 1.

---

## 0. Mục tiêu

Người dùng đánh giá giao diện hiện tại **bố cục và nhận diện tính năng đã tốt, nhưng màu sắc và thiết kế đơn điệu**. Hãy nâng LifeOS lên mức **premium, hiện đại, tinh tế (polish)**, đồng thời:

1. **Thân thiện, trực quan:** nhìn vào là hiểu tính năng; giữ điểm mạnh của bố cục cũ, chỉ cải thiện bố cục ở chỗ đang yếu (mật độ thông tin, thứ bậc thị giác, khoảng thở, trạng thái rỗng, phản hồi sau thao tác).
2. **Màu sắc và thiết kế phong phú hơn:** thoát khỏi cảm giác "một màu xanh + một màu cam".
3. **Màn hình mới sáng tạo** cho các chức năng backend chưa có (xem §4), có bản sắc riêng của LifeOS, tham khảo app nổi tiếng nhưng không sao chép.
4. Người dùng cho phép bạn đề xuất thêm ý tưởng. Cần thông tin gì, **hỏi người dùng** (liệt kê câu hỏi ở cuối bản kế hoạch).

## 1. Ranh giới BẤT KHẢ XÂM PHẠM (vi phạm = hủy kết quả)

- **Cấm** sửa `backend/`, `docs/api/openapi.yaml`, bất kỳ hợp đồng API nào, và `docs/engineering/SYNC_PROTOCOL.md`.
- **Cấm** thay đổi lược đồ của **CSDL gốc** của app: 29 bảng thực thể đồng bộ, `outbox`, `sync_state`, `entity_base_snapshots`, `sync_log`, bảng thông báo; cấm thêm giá trị vào `SyncEntityType`; cấm đổi `SyncEngine`, `LifeOsApiClient`, `ThreeWayMerger`, `MutationCoalescer`. Cấm tăng `schemaVersion` của CSDL gốc.
- **Cấm** gửi bất kỳ dữ liệu nào của tính năng mới lên server, cấm gọi endpoint không có trong OpenAPI.
- Mọi quy tắc cũ vẫn giữ: UI chỉ đọc từ SQLite; ghi qua giao dịch cục bộ + `outbox` (đối với dữ liệu đã có trong spec); tiền không dùng `double` (`Decimal10k`); chỉ tiếng Việt (qua `ViStrings`/`app_vi.arb`); VND mặc định; vùng chạm ≥ 48dp; chữ ≥ WCAG AA 4.5:1; hỗ trợ `textScaler` 2.0 và Reduce Motion; không chuỗi kỹ thuật lộ ra người dùng.
- Không tự bịa luật. Mọi quyết định mới ghi nhãn `[PROPOSED]`; không tự đổi thành `[CONFIRMED]`. Điểm chưa rõ ghi `[OPEN]` và hỏi.

## 2. Giai đoạn 1 — Lập kế hoạch (bắt buộc, rồi DỪNG)

Đọc trước: `AGENTS.md`, `docs/engineering/FLUTTER_RULES.md`, `MOBILE_MASTER_PLAN.md`, `ANTIGRAVITY_FLUTTER_UI_PROMPT.md`, các `docs/phases/*`, `MOBILE_PROGRESS.md`, `MOBILE_FINAL_REPORT.md`. Sau đó viết **`docs/engineering/MOBILE_UI_UPGRADE_PLAN.md`** gồm:

1. **Kiểm toán giao diện hiện tại:** chụp lại các màn chính, liệt kê điểm đơn điệu và điểm bố cục yếu theo từng tab (có ảnh).
2. **3 hướng thiết kế để người dùng chọn** (mỗi hướng: bảng màu Light/Dark kèm mã màu, vai trò màu, cặp tương phản đã tính, ảnh minh họa màn Hôm nay + Tài chính). Gợi ý khởi điểm, bạn được đề xuất tốt hơn:
   - **A. Botanical Premium:** phát triển từ Calm Botanical: xanh rừng đậm, cát ấm, hổ phách, thêm gradient rất nhẹ và bề mặt nhiều lớp.
   - **B. Midnight Aurora:** nền mực/chàm sâu, nhấn xanh ngọc và san hô, thẻ có độ sâu tinh tế.
   - **C. Ivory & Ink:** phong cách biên tập tối giản, một màu nhấn "ngọc" duy nhất, kiểu chữ có cá tính.
   Nguyên tắc chung: **màu nhấn riêng cho từng module** (Việc, Thói quen, Tài chính, Tri thức, Mục tiêu, Lịch) nhưng cùng một họ sắc độ để vẫn hài hòa; tránh cầu vồng, tránh glassmorphism lạm dụng, tránh trang trí không có chức năng.
3. **Hệ thống thiết kế (design tokens) mở rộng:** màu ngữ nghĩa (thành công, cảnh báo, nguy hiểm, thông tin), thang chữ (chọn font tiếng Việt có bản quyền mở OFL, đóng gói trong assets, nêu rõ tên và giấy phép), thang khoảng cách 4/8, bán kính, độ nổi, icon (một bộ nhất quán), **token chuyển động** (thời lượng, easing), haptics. Mọi giá trị ở một nơi (`app_tokens.dart`), không hard-code.
4. **Cải thiện bố cục theo từng tab:** Hôm nay, Kế hoạch, Tài chính, Tri thức, Tổng quan; mỗi tab nêu vấn đề, giải pháp, wireframe/ảnh.
5. **Danh sách màn hình/chức năng mới** (xem §4), mỗi mục có: mục đích, cách dùng, dữ liệu lấy từ đâu (dữ liệu có sẵn hay kho cục bộ mới), nhãn `LOCAL_ONLY` nếu không có trong backend, wireframe.
6. **Thiết kế kho dữ liệu cục bộ cho chức năng mới** (xem §3).
7. **Trạng thái mọi màn:** đang tải (skeleton), rỗng (minh họa + nút hành động), lỗi (có cách khôi phục), offline, thành công (phản hồi tức thì).
8. **Kế hoạch kiểm thử:** golden mới (Light/Dark × phone/tablet), a11y, hiệu năng cuộn 60fps với 10.000 mục, `dart analyze` sạch.
9. **Gói công việc UPW1..UPWn** theo thứ tự, mỗi gói có tiêu chí nghiệm thu.
10. **Câu hỏi cho người dùng** (nếu cần) và danh sách `[PROPOSED]`/`[OPEN]`.

Sau khi ghi kế hoạch: **dừng và báo người dùng.** Chỉ làm Giai đoạn 2 khi người dùng nói rõ đã duyệt và chọn hướng thiết kế.

## 3. Kho dữ liệu cục bộ cho chức năng "chưa có backend" `[PROPOSED]`

Để không đụng CSDL gốc:

- Tạo **CSDL Drift riêng** `LifeOsLocalExtrasDatabase` (file SQLite riêng, ví dụ `lifeos_local_extras.db`), có `schemaVersion` và migration riêng. Mọi bảng có tiền tố `local_`.
- **Không** có cột `sync_state`, **không** ghi vào `outbox`, **không** thêm `SyncEntityType`, **không** gọi API.
- Mọi chức năng dùng kho này gắn nhãn trong code `// LOCAL_ONLY: chưa có backend` và có mục trong tệp **`docs/review/MOBILE_LOCAL_ONLY_FEATURES.md`**: tên chức năng, bảng `local_*`, cột, ghi chú "cần backend nào để đồng bộ sau" (ghi chú để phát triển sau, không triển khai).
- Mọi truy cập qua một lớp repository riêng (`features/<module>/data/local_*_repository.dart`) để sau này đổi sang đồng bộ mà không sửa UI.
- Cảnh báo người dùng trong UI rằng dữ liệu `LOCAL_ONLY` chỉ nằm trên máy này (một dòng nhỏ trong Cài đặt, không lặp ở mọi màn), và đưa chúng vào chức năng **xuất dữ liệu cục bộ** nếu đơn giản; nếu không, ghi `[OPEN]`.
- Khi đăng xuất hoặc xóa tài khoản, hành vi đối với kho `local_*` phải được nêu trong kế hoạch (`[OPEN]` hỏi người dùng).

## 4. Gợi ý màn hình mới (được phép đề xuất thêm/bớt, phải có bản sắc riêng)

Dữ liệu **có sẵn** (không cần kho mới, chỉ đọc từ SQLite hiện có):
- **Vòng đời hôm nay (Life Rings):** các vòng tiến độ Việc, Thói quen, Tài chính, Mục tiêu, kiểu vòng hoạt động nhưng theo ngôn ngữ LifeOS.
- **Dòng thời gian hôm nay:** gộp sự kiện lịch, việc đến hạn, thói quen, routine trên một trục thời gian.
- **Bản đồ nhiệt thói quen** (kiểu lịch đóng góp) và chuỗi ngày có hiệu ứng nhẹ khi đạt mốc.
- **Tài chính trực quan:** biểu đồ vành khuyên chi tiêu theo danh mục, "phong bì" ngân sách còn lại, xu hướng 30 ngày có nhãn và chú giải.
- **Wizard Tổng kết Tuần/Tháng** (dùng thực thể `review` đã có), dẫn dắt từng bước.
- **Tìm kiếm toàn cục** (chỉ đọc dữ liệu cục bộ hiện có).

Dữ liệu **`LOCAL_ONLY`** (dùng kho §3, bản sắc riêng LifeOS):
- **Bánh xe cuộc sống (Life Wheel):** tự chấm điểm 8 lĩnh vực theo thời gian, biểu đồ radar, so sánh các lần.
- **Khu vườn tập trung (Focus Garden):** bấm giờ tập trung; mỗi phiên hoàn thành "mọc" một cây trong khu vườn riêng, liên kết với một Việc (không gửi lên server).
- **Nhật ký cảm xúc & năng lượng:** ghi nhanh tâm trạng, năng lượng, kèm biểu đồ tương quan với thói quen (chỉ tính trên máy).
- **Nghi thức buổi tối / buổi sáng:** chuỗi bước ngắn có đồng hồ và lời nhắc nhẹ.
- **Huy hiệu & cột mốc cá nhân** (tính từ dữ liệu có sẵn, lưu huy hiệu đã nhận ở kho cục bộ).
- **Màn chào và giới thiệu lần đầu** (onboarding) có minh họa.

Bạn được thêm ý tưởng khác, miễn: (i) thuộc phạm vi quản lý cuộc sống cá nhân, (ii) không cần API, (iii) có nhãn `LOCAL_ONLY` nếu dùng kho mới, (iv) không phình quá một lượt làm việc: **xếp hạng ưu tiên và đề nghị người dùng chọn tập con** trong kế hoạch.

## 5. Giai đoạn 2 — Triển khai (chỉ sau khi được duyệt)

- Làm theo gói UPW trong kế hoạch đã duyệt, mỗi gói xong là test xanh.
- Giữ cấu trúc theo tính năng `features/<module>/{data,domain,presentation}`; không dồn code vào file khổng lồ (mỗi file dưới ~600 dòng).
- Tất cả chuỗi hiển thị qua `ViStrings`/`app_vi.arb`; không chuỗi kỹ thuật (tên class, JSON, mã enum tiếng Anh) trên UI.
- Chuyển động tinh tế, có thể tắt khi bật Reduce Motion; không làm chậm cuộn (đo và báo cáo).
- Biểu đồ vẽ bằng `CustomPainter` hoặc thư viện đã được kế hoạch duyệt; có nhãn, chú giải, `Semantics` đầy đủ.
- Tái dùng widget chung (`lifeos_widgets.dart` và các widget mới tách riêng), không sao chép.

## 6. Bằng chứng và báo cáo

- Tạo lại golden với font thật, chạy lặp hai lần không lệch; ảnh chụp **trước/sau** mỗi tab lưu ở `docs/review/mobile_screenshots/ui_upgrade/` (Light/Dark, phone, tablet).
- Dán nguyên văn đầu ra `dart analyze`, `flutter test`, và `flutter test --dart-define=LIFEOS_RUN_REAL_E2E=true` (E2E phải vẫn xanh: chứng minh không đụng CSDL gốc/đồng bộ).
- Liệt kê file đã tạo/sửa/xóa; xác nhận bằng `git diff --stat` rằng **không có thay đổi** trong `backend/`, `docs/api/`, `SyncEngine`, `api_client.dart`, `entity_types.dart` và các bảng gốc.
- Cập nhật `MOBILE_PROGRESS.md` (bảng UPW dùng 3 nhãn trạng thái cũ) và `MOBILE_LOCAL_ONLY_FEATURES.md`.
- Mục chưa làm ghi "chưa làm" và lý do. Không báo "hoàn thành" khi thiếu bằng chứng.

## 7. Việc xử lý kèm theo (nếu chưa làm ở yêu cầu trước)

Nút "Đã điểm danh" phải khác hẳn nút "Điểm danh" (kiểu và màu), nhãn tương phản tốt; bỏ các chuỗi kỹ thuật "Lưu trữ chính xác số nguyên × 10⁴ · Chuỗi thập phân JSON", "(CustomPainter)", "Ví & Tài khoản tài chính" → "Ví & Tài khoản"; nút ẩn/hiện số tiền có `Semantics`/tooltip. Gộp vào các gói UPW.
