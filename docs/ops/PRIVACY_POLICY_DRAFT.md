# Chính sách quyền riêng tư LifeOS — BẢN NHÁP

> Chưa dùng làm văn bản pháp lý hoặc đăng store. Cần điền tên pháp nhân/cá nhân vận hành, địa chỉ, quốc gia/pháp luật áp dụng, email/URL privacy-support, ngày hiệu lực và xác minh hành vi thực tế của ứng dụng.

## 1. Dữ liệu và mục đích

LifeOS xử lý dữ liệu tài khoản/xác thực; nội dung người dùng tự nhập cho Task, Project, Habit, Routine, Calendar, Finance, Note, Goal và các tính năng liên quan; dữ liệu đồng bộ, thiết bị/phiên, notification token nếu tính năng được bật; log bảo mật/vận hành tối thiểu; yêu cầu xuất/xóa tài khoản.

Dữ liệu được dùng để cung cấp tính năng quản lý cá nhân, đồng bộ đa thiết bị, nhắc việc/thông báo, bảo mật, hỗ trợ và vận hành. Không bán dữ liệu cá nhân. `[PLACEHOLDER: xác nhận analytics/crash SDK và mục đích trước khi công bố]`.

Ứng dụng có cơ sở dữ liệu cục bộ để hoạt động offline và backend để đồng bộ. Người dùng chịu trách nhiệm về nội dung họ nhập; LifeOS tối thiểu hóa quyền truy cập và không yêu cầu camera, microphone hay vị trí thiết bị trong manifest hiện được review. Notification/network phải được khai báo đúng bản release thực tế.

## 2. AI Assistant

AI là tính năng tùy chọn, online-only và mặc định tắt theo từng miền: Task/Calendar, Habit, Goal, Notes, Finance. Trước khi gửi yêu cầu, người dùng chọn miền dữ liệu và có thể xem context disclosure. Finance có consent riêng và chỉ dùng cho insight đọc; AI không tự tạo/sửa/xóa giao dịch hoặc số dư. Notes/Finance production chỉ được bật sau khi tài khoản nhà cung cấp có Zero Data Retention được xác nhận.

Khi AI được bật, phần dữ liệu tối thiểu cần cho yêu cầu có thể được gửi từ backend LifeOS tới OpenAI. LifeOS dùng API với `store=false` và không opt-in chia sẻ để huấn luyện. Theo thiết kế đã duyệt, nếu chưa có Zero Data Retention, dữ liệu API có thể chịu abuse-monitoring retention của provider tối đa 30 ngày; phải đối chiếu điều khoản/provider account thực tế tại ngày phát hành. `[PLACEHOLDER: link chính sách OpenAI và trạng thái ZDR của tài khoản production]`.

LifeOS lưu hội thoại/context snapshot gắn người dùng mặc định 90 ngày từ hoạt động cuối; usage log tối đa 365 ngày để đối soát chi phí. Người dùng có thể xóa một hội thoại, toàn bộ dữ liệu AI, thu hồi consent hoặc xuất dữ liệu. Thu hồi có hiệu lực cho yêu cầu kế tiếp; một yêu cầu đã bắt đầu có thể hoàn tất theo consent snapshot lúc bắt đầu.

AI có thể sai. Kết quả không phải tư vấn y tế, pháp lý hay tài chính; người dùng phải kiểm tra trước khi hành động. Bản đầu là read-only; action Task tương lai chỉ sau xác nhận trực tiếp và feature flag riêng.

## 3. Chia sẻ và bên xử lý

Các nhóm bên xử lý có thể gồm nhà cung cấp hosting/database, email/push notification, backup/object storage và OpenAI khi người dùng bật AI. Chỉ chia sẻ dữ liệu cần cho dịch vụ và theo hợp đồng/cấu hình được duyệt. `[PLACEHOLDER: tên nhà cung cấp, vùng xử lý, DPA/SCC/chuyển dữ liệu quốc tế]`.

LifeOS có thể tiết lộ dữ liệu khi pháp luật bắt buộc hoặc để bảo vệ an toàn/quyền hợp pháp, theo quy trình tối thiểu hóa và thông báo khi được phép.

## 4. Lưu giữ, bảo mật và quyền người dùng

Dữ liệu nghiệp vụ được giữ khi tài khoản hoạt động và theo retention cụ thể của tính năng; export tạm hết hạn sau khoảng 24 giờ. Backup mã hóa có chu kỳ riêng và dữ liệu đã xóa có thể còn trong backup cho tới khi hết retention, không được khôi phục để sử dụng bình thường. `[PLACEHOLDER: retention chính thức cho log/account tombstone/backup]`.

Biện pháp gồm TLS, phân tách môi trường, least privilege, secret manager, backup/restore drill và giám sát không chứa nội dung/PII trong metric. Không hệ thống nào an toàn tuyệt đối; incident sẽ được xử lý và thông báo theo nghĩa vụ áp dụng.

Tùy luật áp dụng, người dùng có thể truy cập, sửa, xuất, xóa, hạn chế/phản đối xử lý và thu hồi consent. Trong ứng dụng có luồng xuất/xóa tài khoản và xóa AI riêng. `[PLACEHOLDER: contact và thời hạn phản hồi]`.

## 5. Trẻ em, thay đổi và liên hệ

`[PLACEHOLDER: độ tuổi tối thiểu/content rating và cơ chế xử lý dữ liệu trẻ em]`. Chính sách thay đổi sẽ có ngày hiệu lực/version; thay đổi material cần thông báo và xin lại consent khi phù hợp.

Data controller: `[TÊN]`  
Privacy/support email: `[EMAIL]`  
Privacy URL: `[HTTPS URL]`  
Địa chỉ/quốc gia: `[ĐỊA CHỈ]`
