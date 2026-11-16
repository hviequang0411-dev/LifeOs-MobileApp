# Prompt sửa lỗi Mobile — vòng 3 (`ANTIGRAVITY_FIX_PROMPT_2.md`)

> Gửi nguyên văn cho Antigravity. Phạm vi: `mobile/`, `ci/mobile-ci.yml`, `docs/review/MOBILE_*`. **Không sửa `backend/`.**

---

Vòng 2 đã được chấp nhận phần lớn (ISS-01..09, tách E2E giả/thật, `android/` và `ios/`, `ci/mobile-ci.yml`, tách module, bảng trạng thái). Còn 5 mục dưới đây. Nguyên tắc cũ giữ nguyên: không bịa luật, không đổi nhãn `[CONFIRMED]`/`[PROPOSED]`/`[OPEN]` khi chưa có câu duyệt, báo cáo phải đúng sự thật.

## 1. Drift: sửa lời khai sai (ưu tiên cao nhất)

`MOBILE_PROGRESS.md` (PWP2, D-M02) ghi `LifeOsDriftDatabase extends GeneratedDatabase`, nhưng `lib/` không có `GeneratedDatabase` và không có `import 'package:drift/...'`. Chữ "Drift" chỉ nằm trong chú thích đầu `app_database.dart`; code dùng `package:sqlite3`.

Chọn **một** trong hai và nói rõ bạn chọn gì:

- **(a) Chuyển sang Drift thật:** khai báo `drift_dev` và `build_runner` trong `dev_dependencies`, định nghĩa bảng/DAO bằng Drift, sinh code, giữ toàn bộ test xanh. Mô tả đúng trong báo cáo.
- **(b) Giữ `sqlite3`:** ghi `MP-04 [PROPOSED]` (lý do, rủi ro, so sánh với stack đã chốt), gỡ `drift` khỏi `pubspec.yaml`, sửa chú thích đầu `app_database.dart`, sửa PWP2 và D-M02 trong `MOBILE_PROGRESS.md` cho đúng thực tế. Sau đó **dừng chờ tôi duyệt** mục này, không coi là xong.

Dù chọn nhánh nào, không được để tài liệu nói "Drift" khi code không dùng Drift.

## 2. Golden test và ảnh chụp: làm cho kiểm chứng được giao diện

Hiện ảnh trong `mobile/test/goldens/` và `docs/review/mobile_screenshots/phone_*.png` hiển thị chữ thành ô đen (font Ahem mặc định của test), nên không xem được giao diện. Ngoài ra `test/failures/` còn các ca lệch (ảnh gốc 16.091 byte so với ảnh test 16.084 byte), nghĩa là golden có dấu hiệu không ổn định.

1. Nạp font thật (Roboto hoặc font của `app_theme.dart`) trong test bằng `FontLoader` hoặc `loadAppFonts`, để golden hiển thị chữ tiếng Việt đọc được. Nếu font cần đóng gói, khai báo trong `pubspec.yaml`.
2. Làm golden ổn định: cố định `devicePixelRatio`, tắt animation, dùng dữ liệu seed cố định, chạy hai lần liên tiếp và không được có ca lệch.
3. Tạo lại toàn bộ golden và `phone_*.png`, `tablet_*.png`. Xóa các ảnh `preview_*.png` cũ trong `mobile_screenshots/` (chụp trước vòng sửa, không còn đúng).
4. Xóa thư mục `test/failures/` và thêm `mobile/test/failures/` vào `.gitignore`.
5. Báo cáo ghi rõ: golden dùng font nào, đã chạy lặp 2 lần không lệch.

## 3. Sửa 4 lỗi giao diện thấy trên ảnh (màn Hôm nay)

1. **Nhãn ưu tiên tiếng Anh:** `URGENT/MEDIUM/HIGH` hiện nguyên trên UI tiếng Việt. Chỉ giữ `NONE/LOW/MEDIUM/HIGH` ở tầng dữ liệu theo `SYNC_PROTOCOL.md` §3.1; hiển thị bằng chuỗi tiếng Việt lấy từ `ViStrings`/`app_vi.arb` (ví dụ Không, Thấp, Trung bình, Cao). Không còn mức `URGENT` ở bất kỳ đâu (kể cả dữ liệu seed).
2. **Ký tự lỗi `▯`:** nhãn "Chuỗi N ngày" đang có glyph không hiển thị được. Dùng icon của Flutter (`Icons.*`) thay cho ký tự emoji, hoặc bỏ.
3. **Nút "Thêm nhanh" (FAB):** đè lên thẻ "Tài chính hôm nay" và có viền đen dày bất thường. Chừa khoảng đệm cuối danh sách cho FAB, bỏ viền đen (nếu là vòng focus thì chỉ hiện khi dùng bàn phím).
4. **Dữ liệu demo:** màn hình đang hiện "23 thay đổi chờ đồng bộ" từ dữ liệu seed. Seed demo chỉ dùng cho golden/ảnh chụp, không được chạy trong bản app thường (`dev`/`staging`/`prod`). Kiểm tra `demo_workspace_seeder.dart` chỉ được gọi từ test.

## 4. Đánh số PWP

Báo cáo vòng 1 và vòng 2 đánh số PWP13/PWP14 khác nhau. Đối chiếu với `MOBILE_MASTER_PLAN.md` và dùng đúng số của kế hoạch gốc trong mọi báo cáo. Nếu đã đổi tên gói, thêm một bảng ánh xạ "tên cũ → tên mới" ở đầu `MOBILE_PROGRESS.md`.

## 5. Bằng chứng khi báo cáo lại

- Dán nguyên văn đầu ra của `dart analyze`, `flutter test` thường, và `flutter test --dart-define=LIFEOS_RUN_REAL_E2E=true` (kèm số ca đạt/bỏ qua).
- Với mỗi mục 1–4: liệt kê file đã tạo, đổi, xóa. Mục chưa làm ghi "chưa làm" và lý do.
- Chụp lại ảnh các màn Hôm nay, Kế hoạch, Tài chính, Tri thức (Light/Dark, phone) và 1 ảnh tablet, bằng bản app hiện tại, rồi lưu vào `docs/review/mobile_screenshots/`.
