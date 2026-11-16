# LifeOS — Mobile release handoff

`ci/mobile-release-source.yml` là workflow nguồn; chủ repo chép sang `.github/workflows/mobile-release.yml` sau khi agent mobile hoàn tất release configuration. DWP7 không sửa `mobile/` hoặc `ci/mobile-ci.yml`.

## Blocker hiện tại đã kiểm chứng

- Android `applicationId` là `com.lifeos.lifeos_mobile`, trong khi config ứng dụng dự kiến `vn.lifeos.app`; release đang dùng debug signing và file còn TODO.
- iOS bundle ID là `com.lifeos.lifeosMobile`; chưa có Development Team/signing handoff và chưa có `PrivacyInfo.xcprivacy`.
- Manifest hiện chưa khai báo notification/FCM production; pubspec cũng chưa cho thấy Firebase plugin. Không được quảng bá push notification production trước khi agent mobile xác minh.
- `enableSyncDiagnostics` của flavor production hiện là `true`; agent mobile/security phải xác nhận không lộ dữ liệu trước release.

Workflow cố ý fail ở preflight khi các blocker trên còn tồn tại. Không bỏ gate bằng cách nới `grep`; sửa release project trong phạm vi agent mobile và review thay đổi.

## Secret/environment cần cấu hình

Android: `ANDROID_KEYSTORE_BASE64`, `ANDROID_KEY_ALIAS`, `ANDROID_KEY_PASSWORD`, `ANDROID_STORE_PASSWORD`, `GOOGLE_PLAY_SERVICE_ACCOUNT_JSON`.

iOS: `IOS_CERTIFICATE_P12_BASE64`, `IOS_CERTIFICATE_PASSWORD`, `IOS_PROVISIONING_PROFILE_BASE64`, `IOS_KEYCHAIN_PASSWORD`, `APPLE_TEAM_ID`, `APP_STORE_CONNECT_KEY_ID`, `APP_STORE_CONNECT_ISSUER_ID`, `APP_STORE_CONNECT_PRIVATE_KEY`.

Dùng GitHub environments `mobile-build`, `google-play-production`, `app-store-production`; hai environment production cần reviewer con người. Không dùng secret production trên PR/fork.

## Quy trình

1. Agent mobile chốt application/bundle ID, signing config, privacy manifest, icon/name, production API define, FCM/APNs và versioning.
2. Chạy workflow với `publish=false`; tải artifact, kiểm signature, cài trên thiết bị production-like, smoke login/offline CRUD/sync/export/delete/notification/AI consent.
3. Xác nhận backend `minSupportedClientVersion` chưa chặn bản; API URL HTTPS production và không có staging/localhost.
4. Android: `publish=true` chỉ lên Play Internal ở trạng thái draft; chuyển Closed/Production bằng Play Console sau review Data safety/content rating.
5. iOS: upload TestFlight; hoàn tất App Privacy/export compliance/content rights trong App Store Connect trước external/production.
6. Staged rollout, theo dõi crash/auth/sync 4xx/5xx; dừng rollout trước khi force-update. Không xóa dữ liệu local khi trả 426.

## Kiểm chứng chưa chạy

Chưa có keystore/account/macOS runner/store credential nên chưa build signed AAB/IPA và chưa upload store. Cần GitHub run URL, Play release ID và TestFlight build ID làm bằng chứng DWP8.
