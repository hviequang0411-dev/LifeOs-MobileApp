import 'dart:typed_data';

import 'package:dio/dio.dart';

import '../../../core/network/api_client.dart';

/// Dịch vụ gọi trực tiếp API cho các luồng **Online-Only** (được phép theo `SYSTEM_ARCHITECTURE.md` §5):
/// - Đăng ký (`POST /auth/register` -> `202`) / Xác minh email (`POST /auth/verify-email` -> `204`) / Đăng nhập (`POST /auth/login` -> `200`).
/// - Quản lý thiết bị đăng nhập (`GET /devices`, `DELETE /devices/{id}`).
/// - Xuất dữ liệu tài khoản (`POST /account/export`, `GET /account/export/{id}`, `GET /account/export/{id}/download`).
/// - Xóa tài khoản có thời gian chờ 7 ngày + mã xác nhận 6 chữ số (`POST /account/deletion/code`, `POST /account/deletion`, `GET /account/deletion`, `DELETE /account/deletion`).
class OnlineAccountService {
  const OnlineAccountService({required this.apiClient});

  final LifeOsApiClient apiClient;

  /// Đăng ký tài khoản mới (`POST /api/v1/auth/register` — `openapi.yaml` dòng 396–416, `RegisterRequest` dòng 7958–7980).
  /// Server trả về `202 Accepted` không có body; token xác minh được gửi qua email (Mailpit ở dev).
  Future<void> register({
    required String email,
    required String password,
    required String displayName,
    String timezone = 'Asia/Ho_Chi_Minh',
  }) async {
    try {
      await apiClient.dio.post<dynamic>(
        '/auth/register',
        data: <String, dynamic>{
          'email': email.trim(),
          'password': password,
          'displayName': displayName.trim(),
          'timezone': timezone,
        },
        options: Options(extra: const <String, dynamic>{'skipAuth': true}),
      );
    } catch (e) {
      throw LifeOsApiClient.normalizeError(e);
    }
  }

  /// Xác minh email bằng token nhận qua email (`POST /api/v1/auth/verify-email` -> `204`).
  Future<void> verifyEmail({required String token}) async {
    try {
      await apiClient.dio.post<dynamic>(
        '/auth/verify-email',
        data: <String, dynamic>{'token': token.trim()},
        options: Options(extra: const <String, dynamic>{'skipAuth': true}),
      );
    } catch (e) {
      throw LifeOsApiClient.normalizeError(e);
    }
  }

  /// Gửi lại email xác minh tài khoản (`POST /api/v1/auth/resend-verification` -> `202`).
  Future<void> resendVerification({required String email}) async {
    try {
      await apiClient.dio.post<dynamic>(
        '/auth/resend-verification',
        data: <String, dynamic>{'email': email.trim()},
        options: Options(extra: const <String, dynamic>{'skipAuth': true}),
      );
    } catch (e) {
      throw LifeOsApiClient.normalizeError(e);
    }
  }

  /// Yêu cầu gửi mã đặt lại mật khẩu (`POST /api/v1/auth/forgot-password` -> `202`).
  Future<void> forgotPassword({required String email}) async {
    try {
      await apiClient.dio.post<dynamic>(
        '/auth/forgot-password',
        data: <String, dynamic>{'email': email.trim()},
        options: Options(extra: const <String, dynamic>{'skipAuth': true}),
      );
    } catch (e) {
      throw LifeOsApiClient.normalizeError(e);
    }
  }

  /// Đặt lại mật khẩu mới bằng token nhận qua email (`POST /api/v1/auth/reset-password` -> `204`).
  Future<void> resetPassword({
    required String token,
    required String newPassword,
  }) async {
    try {
      await apiClient.dio.post<dynamic>(
        '/auth/reset-password',
        data: <String, dynamic>{
          'token': token.trim(),
          'newPassword': newPassword,
        },
        options: Options(extra: const <String, dynamic>{'skipAuth': true}),
      );
    } catch (e) {
      throw LifeOsApiClient.normalizeError(e);
    }
  }

  /// Lấy hồ sơ cá nhân hiện tại (`GET /api/v1/me`).
  Future<({String id, String email, String displayName, bool emailVerified})>
      fetchProfile() async {
    try {
      final Response<dynamic> resp = await apiClient.dio.get<dynamic>('/me');
      final Map<String, dynamic> body =
          (resp.data as Map<dynamic, dynamic>).cast<String, dynamic>();
      return (
        id: body['id'] as String,
        email: (body['email'] as String?) ?? '',
        displayName: (body['displayName'] as String?)?.trim().isNotEmpty == true
            ? (body['displayName'] as String).trim()
            : 'bạn',
        emailVerified: body['emailVerified'] == true,
      );
    } catch (e) {
      throw LifeOsApiClient.normalizeError(e);
    }
  }

  /// Cập nhật tên hiển thị (`PATCH /api/v1/me`).
  Future<String> updateDisplayName({required String displayName}) async {
    try {
      final Response<dynamic> resp = await apiClient.dio.patch<dynamic>(
        '/me',
        data: <String, dynamic>{'displayName': displayName.trim()},
      );
      final Map<String, dynamic> body =
          (resp.data as Map<dynamic, dynamic>? ?? <dynamic, dynamic>{})
              .cast<String, dynamic>();
      return (body['displayName'] as String?) ?? displayName.trim();
    } catch (e) {
      throw LifeOsApiClient.normalizeError(e);
    }
  }

  /// Đổi mật khẩu khi đã đăng nhập (`POST /api/v1/me/change-password` -> `204`).
  Future<void> changePassword({
    required String currentPassword,
    required String newPassword,
  }) async {
    try {
      await apiClient.dio.post<dynamic>(
        '/me/change-password',
        data: <String, dynamic>{
          'currentPassword': currentPassword,
          'newPassword': newPassword,
        },
      );
    } catch (e) {
      throw LifeOsApiClient.normalizeError(e);
    }
  }

  /// Chuyển đổi ngoại lệ tài khoản sang thông điệp tiếng Việt thân thiện (nêu rõ cần mạng khi ngoại tuyến).
  static String formatAccountErrorVi(Object error) {
    final LifeOsApiException apiErr = LifeOsApiClient.normalizeError(error);
    if (apiErr.errorCode == 'NETWORK_ERROR') {
      return 'Cần kết nối mạng để thực hiện thao tác tài khoản. Nếu đã dùng trên thiết bị này, bạn có thể chọn "Tiếp tục dùng ngoại tuyến".';
    }
    return apiErr.userMessageVi;
  }

  /// Đăng nhập (`POST /api/v1/auth/login` — `openapi.yaml` `LoginRequest` dòng 7241–7261 & `auth-login.request.json`).
  Future<({String userId, String email, String displayName, String? deviceId})>
      login({
    required String email,
    required String password,
    required String deviceName,
    String platform = 'ANDROID',
    String appVersion = '1.0.0',
    String? deviceId,
  }) async {
    try {
      final Response<dynamic> resp = await apiClient.dio.post<dynamic>(
        '/auth/login',
        data: <String, dynamic>{
          'email': email.trim(),
          'password': password,
          if (deviceId != null) 'deviceId': deviceId,
          'device': <String, dynamic>{
            'name': deviceName,
            'platform': platform,
            'appVersion': appVersion,
          },
        },
        options: Options(extra: const <String, dynamic>{'skipAuth': true}),
      );
      final Map<String, dynamic> body =
          (resp.data as Map<dynamic, dynamic>).cast<String, dynamic>();
      final String accessToken = body['accessToken'] as String;
      final String refreshToken = body['refreshToken'] as String;
      final Map<String, dynamic> user =
          (body['user'] as Map<dynamic, dynamic>).cast<String, dynamic>();
      final String userId = user['id'] as String;
      final String? resolvedDeviceId = body['deviceId'] as String?;

      await apiClient.tokenStorage.saveSession(
        accessToken: accessToken,
        refreshToken: refreshToken,
        userId: userId,
        deviceId: resolvedDeviceId,
      );

      return (
        userId: userId,
        email: (user['email'] as String?) ?? email,
        displayName: (user['displayName'] as String?) ?? 'Người dùng',
        deviceId: resolvedDeviceId,
      );
    } catch (e) {
      throw LifeOsApiClient.normalizeError(e);
    }
  }

  Future<List<Map<String, dynamic>>> listDevices() async {
    try {
      final Response<dynamic> resp =
          await apiClient.dio.get<dynamic>('/devices');
      final dynamic data = resp.data;
      final List<dynamic> rawList = data is List<dynamic>
          ? data
          : ((data as Map<dynamic, dynamic>?)?['items'] as List<dynamic>? ??
              const <dynamic>[]);
      return rawList
          .map(
            (dynamic e) => (e as Map<dynamic, dynamic>).cast<String, dynamic>(),
          )
          .toList();
    } catch (e) {
      throw LifeOsApiClient.normalizeError(e);
    }
  }

  Future<void> revokeDevice(String deviceId) async {
    try {
      await apiClient.dio.delete<dynamic>('/devices/$deviceId');
    } catch (e) {
      throw LifeOsApiClient.normalizeError(e);
    }
  }

  /// Yêu cầu xuất dữ liệu cá nhân (`POST /api/v1/account/export` — `ExportResponse` `openapi.yaml` dòng 6665–6685).
  Future<Map<String, dynamic>> requestDataExport() async {
    try {
      final Response<dynamic> resp =
          await apiClient.dio.post<dynamic>('/account/export');
      return (resp.data as Map<dynamic, dynamic>? ?? <dynamic, dynamic>{})
          .cast<String, dynamic>();
    } catch (e) {
      throw LifeOsApiClient.normalizeError(e);
    }
  }

  /// Kiểm tra trạng thái bản xuất dữ liệu (`GET /api/v1/account/export/{id}`).
  Future<Map<String, dynamic>> getDataExportStatus(String exportId) async {
    try {
      final Response<dynamic> resp =
          await apiClient.dio.get<dynamic>('/account/export/$exportId');
      return (resp.data as Map<dynamic, dynamic>? ?? <dynamic, dynamic>{})
          .cast<String, dynamic>();
    } catch (e) {
      throw LifeOsApiClient.normalizeError(e);
    }
  }

  /// Tải tệp `.zip` xuất dữ liệu (`GET /api/v1/account/export/{id}/download`).
  Future<Uint8List> downloadDataExportZip(String exportId) async {
    try {
      final Response<List<int>> resp = await apiClient.dio.get<List<int>>(
        '/account/export/$exportId/download',
        options: Options(responseType: ResponseType.bytes),
      );
      return Uint8List.fromList(resp.data ?? const <int>[]);
    } catch (e) {
      throw LifeOsApiClient.normalizeError(e);
    }
  }

  /// Bước 1 xóa tài khoản: yêu cầu gửi mã xác nhận 6 chữ số qua email (`POST /account/deletion/code` -> `202`).
  Future<void> requestAccountDeletionCode() async {
    try {
      await apiClient.dio.post<dynamic>('/account/deletion/code');
    } catch (e) {
      throw LifeOsApiClient.normalizeError(e);
    }
  }

  /// Bước 2 xóa tài khoản: xác nhận bằng mật khẩu + mã 6 số (`POST /account/deletion`),
  /// trả về `AccountDeletionResponse` (`{cancellable: true, deletionScheduledAt: ...}`).
  Future<Map<String, dynamic>> confirmAccountDeletion({
    required String password,
    required String sixDigitCode,
  }) async {
    try {
      final Response<dynamic> resp = await apiClient.dio.post<dynamic>(
        '/account/deletion',
        data: <String, dynamic>{
          'password': password,
          'code': sixDigitCode.trim(),
        },
      );
      return (resp.data as Map<dynamic, dynamic>? ?? <dynamic, dynamic>{})
          .cast<String, dynamic>();
    } catch (e) {
      throw LifeOsApiClient.normalizeError(e);
    }
  }

  /// Kiểm tra trạng thái chờ xóa tài khoản (`GET /account/deletion`).
  Future<Map<String, dynamic>> getAccountDeletionStatus() async {
    try {
      final Response<dynamic> resp =
          await apiClient.dio.get<dynamic>('/account/deletion');
      return (resp.data as Map<dynamic, dynamic>? ?? <dynamic, dynamic>{})
          .cast<String, dynamic>();
    } catch (e) {
      throw LifeOsApiClient.normalizeError(e);
    }
  }

  /// Hủy yêu cầu xóa tài khoản trong vòng 7 ngày (`DELETE /account/deletion` -> `204`).
  Future<void> cancelAccountDeletion() async {
    try {
      await apiClient.dio.delete<dynamic>('/account/deletion');
    } catch (e) {
      throw LifeOsApiClient.normalizeError(e);
    }
  }
}
