# LifeOS — Store listing draft

> Bản nháp cho Google Play/App Store. Không submit trước khi thay toàn bộ placeholder, kiểm screenshot/claim với bản build thật và hoàn tất privacy/data-safety form.

## Metadata đề xuất

- Tên: `LifeOS`
- Subtitle/short description: `Sắp xếp công việc và thói quen, ngay cả khi offline.`
- Category: Productivity (`[xác minh category phụ]`)
- Support URL: `[HTTPS SUPPORT URL]`
- Privacy URL: `[HTTPS PRIVACY URL]`
- Marketing URL: `[OPTIONAL]`
- Copyright/seller: `[LEGAL NAME]`
- Content rating/age: `[HOÀN TẤT BẢNG HỎI STORE]`

## Mô tả dài nháp

LifeOS giúp bạn quản lý công việc, dự án, thói quen, lịch, mục tiêu, ghi chú và theo dõi tài chính cá nhân trong một không gian thống nhất. Thiết kế offline-first cho phép tiếp tục làm việc khi mạng không ổn định và đồng bộ lại khi có kết nối.

Tính năng chính:

- tổ chức Task/Project, Habit/Routine, Calendar và Goal;
- ghi chú và tổng quan cá nhân;
- theo dõi Finance theo currency mà bạn chọn;
- nhắc việc/thông báo khi bản phát hành đã cấu hình quyền tương ứng;
- xuất hoặc xóa dữ liệu tài khoản;
- AI Assistant tùy chọn theo từng miền, có context disclosure và có thể xóa riêng. AI có thể sai và không phải tư vấn tài chính.

`[XÓA claim chưa có trong build; không quảng bá RAG, action tự động, push hoặc AI nếu feature flag production chưa bật.]`

## Screenshot/copy checklist

1. Dashboard không chứa dữ liệu thật.
2. Task/Calendar và offline/sync state.
3. Habit/Goal.
4. Finance dùng dữ liệu giả, không hiện tài khoản/số tiền thật.
5. Notes dùng nội dung giả.
6. AI chỉ nếu đã bật production: màn consent theo miền, context disclosure, cảnh báo AI có thể sai.
7. Export/delete account và privacy control.

Không đưa email, token, notification, user ID, dữ liệu tài chính/ghi chú thật hoặc domain staging vào screenshot.

## Data safety / App Privacy worksheet

Phải đối chiếu binary và backend thật, không chỉ tài liệu:

| Nhóm | Có thể thu thập | Mục đích | Người dùng xóa được | Cần xác minh |
|---|---|---|---|---|
| Account/auth | Có | Account, security | Có | Trường cụ thể, retention |
| User content | Có | Core features/sync | Có | Task/Calendar/Habit/Goal/Notes/Finance |
| Identifiers/device | Có thể | Session, sync, notification | Có/theo TTL | FCM/APNs và SDK thực tế |
| Diagnostics | Có thể | Stability/security | Theo retention | Crash/analytics SDK, log fields |
| AI content | Opt-in | AI response | Xóa AI/account | OpenAI, retention, ZDR |
| Payment | Chưa xác định | Store/billing | Theo store | Không claim nếu không có IAP |
| Location/camera/microphone/contacts | Không thấy trong manifest review | Không | N/A | Re-scan signed binary |

Google Play cần Data safety, account deletion URL, content rating, app access/test account và privacy policy. App Store cần App Privacy, export compliance, content rights, review notes/test account và privacy manifest. Người sở hữu account chịu trách nhiệm xác nhận khai báo cuối.

## Human assets/blockers

Legal identity/contact, domain privacy/support, icon/feature graphic/screenshots, developer accounts/fees, Play keystore/App Signing, Apple certificate/profile/API key/Mac, FCM/APNs production, test account và reviewer notes. Không đưa credential vào tài liệu hoặc chat.
