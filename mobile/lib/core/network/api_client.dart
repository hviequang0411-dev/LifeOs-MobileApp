import 'dart:async';

import 'package:dio/dio.dart';
import 'package:flutter_secure_storage/flutter_secure_storage.dart';

import '../l10n/vi_strings.dart';

/// Lưu trữ bảo mật cặp `accessToken`, `refreshToken`, `userId`, `deviceId`
/// trong Keychain (iOS) / EncryptedSharedPreferences Keystore (Android).
abstract class TokenStorage {
  Future<String?> readAccessToken();
  Future<String?> readRefreshToken();
  Future<String?> readUserId();
  Future<String?> readDeviceId();

  Future<void> saveSession({
    required String accessToken,
    required String refreshToken,
    required String userId,
    String? deviceId,
  });

  Future<void> clearTokensOnly();
}

class SecureTokenStorage implements TokenStorage {
  const SecureTokenStorage([
    this._storage = const FlutterSecureStorage(),
  ]);

  final FlutterSecureStorage _storage;

  static const String _kAccess = 'lifeos.auth.access_token';
  static const String _kRefresh = 'lifeos.auth.refresh_token';
  static const String _kUserId = 'lifeos.auth.user_id';
  static const String _kDeviceId = 'lifeos.auth.device_id';

  @override
  Future<String?> readAccessToken() => _storage.read(key: _kAccess);

  @override
  Future<String?> readRefreshToken() => _storage.read(key: _kRefresh);

  @override
  Future<String?> readUserId() => _storage.read(key: _kUserId);

  @override
  Future<String?> readDeviceId() => _storage.read(key: _kDeviceId);

  @override
  Future<void> saveSession({
    required String accessToken,
    required String refreshToken,
    required String userId,
    String? deviceId,
  }) async {
    await _storage.write(key: _kAccess, value: accessToken);
    await _storage.write(key: _kRefresh, value: refreshToken);
    await _storage.write(key: _kUserId, value: userId);
    if (deviceId != null) {
      await _storage.write(key: _kDeviceId, value: deviceId);
    }
  }

  @override
  Future<void> clearTokensOnly() async {
    await _storage.delete(key: _kAccess);
    await _storage.delete(key: _kRefresh);
  }
}

/// Bản lưu trữ Token trong bộ nhớ phục vụ kiểm thử Unit / Widget / E2E.
class InMemoryTokenStorage implements TokenStorage {
  String? _accessToken;
  String? _refreshToken;
  String? _userId;
  String? _deviceId;

  @override
  Future<String?> readAccessToken() async => _accessToken;

  @override
  Future<String?> readRefreshToken() async => _refreshToken;

  @override
  Future<String?> readUserId() async => _userId;

  @override
  Future<String?> readDeviceId() async => _deviceId;

  @override
  Future<void> saveSession({
    required String accessToken,
    required String refreshToken,
    required String userId,
    String? deviceId,
  }) async {
    _accessToken = accessToken;
    _refreshToken = refreshToken;
    _userId = userId;
    if (deviceId != null) {
      _deviceId = deviceId;
    }
  }

  @override
  Future<void> clearTokensOnly() async {
    _accessToken = null;
    _refreshToken = null;
  }
}

/// Lỗi chuẩn hóa phía Client đã ánh xạ từ cấu trúc lỗi của Backend (`API_CONVENTIONS.md`).
class LifeOsApiException implements Exception {
  const LifeOsApiException({
    required this.statusCode,
    required this.errorCode,
    required this.userMessageVi,
    this.fieldErrors = const <String, String>{},
    this.traceId,
  });

  final int? statusCode;
  final String errorCode;
  final String userMessageVi;
  final Map<String, String> fieldErrors;
  final String? traceId;

  bool get isAccountDeletionPending =>
      statusCode == 403 && errorCode == 'ACCOUNT_DELETION_PENDING';

  @override
  String toString() =>
      'LifeOsApiException(status=$statusCode, code=$errorCode, msg=$userMessageVi)';
}

/// Tầng giao tiếp mạng trung tâm (chỉ được gọi bởi `SyncEngine` và các dịch vụ Online-Only).
///
/// Tích hợp:
/// - Tự động đính kèm `Authorization: Bearer`.
/// - Khóa đơn luồng (Single-Flight Lock) khi làm mới token `401` qua `POST /auth/refresh`.
/// - Không bao giờ xóa dữ liệu SQLite / `outbox` khi hết hạn phiên đăng nhập (`RequiresReAuth`).
class LifeOsApiClient {
  LifeOsApiClient({
    required String baseUrl,
    required this.tokenStorage,
    Dio? customDio,
    this.onRequiresReAuth,
    this.onAccountDeletionPending,
  }) : dio = customDio ??
            Dio(
              BaseOptions(
                baseUrl: baseUrl,
                connectTimeout: const Duration(seconds: 10),
                receiveTimeout: const Duration(seconds: 25),
                sendTimeout: const Duration(seconds: 20),
                headers: const <String, String>{
                  'Content-Type': 'application/json',
                  'Accept': 'application/json',
                },
              ),
            ) {
    dio.interceptors.add(
      InterceptorsWrapper(
        onRequest: _onRequest,
        onError: _onError,
      ),
    );
  }

  final Dio dio;
  final TokenStorage tokenStorage;
  final void Function()? onRequiresReAuth;
  final void Function()? onAccountDeletionPending;

  Completer<bool>? _refreshCompleter;

  Future<void> _onRequest(
    RequestOptions options,
    RequestInterceptorHandler handler,
  ) async {
    final bool skipAuth = options.extra['skipAuth'] == true;
    if (!skipAuth) {
      final String? token = await tokenStorage.readAccessToken();
      if (token != null && token.isNotEmpty) {
        options.headers['Authorization'] = 'Bearer $token';
      }
    }
    handler.next(options);
  }

  Future<void> _onError(
    DioException err,
    ErrorInterceptorHandler handler,
  ) async {
    final int? status = err.response?.statusCode;
    final String path = err.requestOptions.path;

    // Phát hiện trạng thái tài khoản đang chờ xóa 7 ngày (403 ACCOUNT_DELETION_PENDING)
    final Map<String, dynamic>? errBody = _asMap(err.response?.data);
    final String? errCode = errBody?['errorCode'] as String?;
    if (status == 403 && errCode == 'ACCOUNT_DELETION_PENDING') {
      onAccountDeletionPending?.call();
    }

    // Xử lý 401 bằng Single-Flight Refresh Lock (trừ chính các đường dẫn /auth/login, /auth/refresh)
    final bool isAuthEndpoint =
        path.contains('/auth/login') ||
        path.contains('/auth/register') ||
        path.contains('/auth/refresh');
    final bool alreadyRetried = err.requestOptions.extra['retriedAfterRefresh'] == true;

    if (status == 401 && !isAuthEndpoint && !alreadyRetried) {
      final bool refreshed = await _trySingleFlightRefresh();
      if (refreshed) {
        final String? newAccess = await tokenStorage.readAccessToken();
        final RequestOptions retryOptions = err.requestOptions;
        retryOptions.extra['retriedAfterRefresh'] = true;
        if (newAccess != null) {
          retryOptions.headers['Authorization'] = 'Bearer $newAccess';
        }
        try {
          final Response<dynamic> retryResponse = await dio.fetch<dynamic>(retryOptions);
          return handler.resolve(retryResponse);
        } on DioException catch (retryErr) {
          return handler.next(retryErr);
        }
      } else {
        await tokenStorage.clearTokensOnly();
        onRequiresReAuth?.call();
      }
    }

    handler.next(err);
  }

  Future<bool> _trySingleFlightRefresh() async {
    if (_refreshCompleter != null) {
      return _refreshCompleter!.future;
    }
    final Completer<bool> completer = Completer<bool>();
    _refreshCompleter = completer;

    try {
      final String? refreshToken = await tokenStorage.readRefreshToken();
      final String? userId = await tokenStorage.readUserId();
      if (refreshToken == null || refreshToken.isEmpty) {
        completer.complete(false);
        return false;
      }

      final Response<dynamic> resp = await dio.post<dynamic>(
        '/auth/refresh',
        data: <String, dynamic>{'refreshToken': refreshToken},
        options: Options(extra: <String, dynamic>{'skipAuth': true}),
      );

      final Map<String, dynamic>? data = _asMap(resp.data);
      final String? newAccess = data?['accessToken'] as String?;
      final String? newRefresh = data?['refreshToken'] as String?;
      final Map<String, dynamic>? userMap = _asMap(data?['user']);
      final String resolvedUserId =
          (userMap?['id'] as String?) ?? userId ?? '';

      if (newAccess != null && newRefresh != null && resolvedUserId.isNotEmpty) {
        await tokenStorage.saveSession(
          accessToken: newAccess,
          refreshToken: newRefresh,
          userId: resolvedUserId,
        );
        completer.complete(true);
        return true;
      }
      completer.complete(false);
      return false;
    } catch (_) {
      completer.complete(false);
      return false;
    } finally {
      _refreshCompleter = null;
    }
  }

  /// Chuyển đổi mọi ngoại lệ Dio sang `LifeOsApiException` với thông điệp tiếng Việt chuẩn.
  static LifeOsApiException normalizeError(Object error) {
    if (error is LifeOsApiException) return error;
    if (error is DioException) {
      final int? status = error.response?.statusCode;
      final Map<String, dynamic>? body = _asMap(error.response?.data);
      final String code = (body?['errorCode'] as String?) ??
          (status == 401
              ? 'UNAUTHORIZED'
              : status == 403
                  ? 'FORBIDDEN'
                  : status == 404
                      ? 'RESOURCE_NOT_FOUND'
                      : status == 409
                          ? 'CONFLICT'
                          : status == 429
                              ? 'RATE_LIMITED'
                              : 'NETWORK_ERROR');

      final Map<String, String> fieldErrs = <String, String>{};
      final dynamic rawDetails = body?['details'] ?? body?['fieldErrors'];
      if (rawDetails is List<dynamic>) {
        for (final dynamic item in rawDetails) {
          if (item is Map<dynamic, dynamic>) {
            final String? f = item['field'] as String?;
            final String? m = item['message'] as String?;
            if (f != null && m != null) {
              fieldErrs[f] = m;
            }
          }
        }
      } else if (rawDetails is Map<dynamic, dynamic>) {
        rawDetails.forEach((dynamic k, dynamic v) {
          fieldErrs[k.toString()] = v.toString();
        });
      }

      return LifeOsApiException(
        statusCode: status,
        errorCode: code,
        userMessageVi: ViStrings.mapBackendErrorCode(
          code,
          serverMessage: body?['message'] as String?,
        ),
        fieldErrors: fieldErrs,
        traceId: body?['traceId'] as String?,
      );
    }

    return LifeOsApiException(
      statusCode: null,
      errorCode: 'NETWORK_ERROR',
      userMessageVi: ViStrings.mapBackendErrorCode('NETWORK_ERROR'),
    );
  }

  static Map<String, dynamic>? _asMap(dynamic raw) {
    if (raw is Map<String, dynamic>) return raw;
    if (raw is Map<dynamic, dynamic>) return raw.cast<String, dynamic>();
    return null;
  }
}

