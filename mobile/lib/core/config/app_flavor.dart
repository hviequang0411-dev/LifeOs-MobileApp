import 'package:flutter/foundation.dart' show kIsWeb;

/// Cấu hình Flavor (`dev` / `staging` / `prod`) và biến môi trường `--dart-define`
/// theo đúng hợp đồng cho tác nhân triển khai (MOBILE_MASTER_PLAN §8).
enum AppEnvironment {
  dev,
  staging,
  prod,
}

class AppFlavorConfig {
  const AppFlavorConfig({
    required this.environment,
    required this.appName,
    required this.applicationId,
    required this.apiBaseUrl,
    required this.enableSyncDiagnostics,
    required this.defaultCurrency,
    this.enableFcm = false,
  });

  final AppEnvironment environment;
  final String appName;
  final String applicationId;
  final String apiBaseUrl;
  final bool enableSyncDiagnostics;
  final String defaultCurrency;
  final bool enableFcm;

  static const String _envDefine = String.fromEnvironment(
    'LIFEOS_ENV',
    defaultValue: 'dev',
  );

  static const String _apiUrlDefine = String.fromEnvironment(
    'LIFEOS_API_BASE_URL',
    defaultValue: 'http://localhost:8080/api/v1',
  );

  /// Cờ cấu hình bật FCM Push (`[PROPOSED]`: mặc định tắt cho tới khi người dùng cung cấp Firebase config thật).
  static const bool enableFcmDefine = bool.fromEnvironment(
    'LIFEOS_ENABLE_FCM',
    defaultValue: false,
  );

  static String _resolveDevApiBaseUrl() {
    if (!kIsWeb || _apiUrlDefine != 'http://localhost:8080/api/v1') {
      return _apiUrlDefine;
    }
    final Uri base = Uri.base;
    if (base.hasScheme && (base.scheme == 'http' || base.scheme == 'https')) {
      return '${base.origin}/api/v1';
    }
    return _apiUrlDefine;
  }

  static AppFlavorConfig fromEnvironment([AppEnvironment? forcedEnv]) {
    final AppEnvironment resolvedEnv = forcedEnv ??
        switch (_envDefine.toLowerCase()) {
          'prod' || 'production' => AppEnvironment.prod,
          'staging' || 'stg' => AppEnvironment.staging,
          _ => AppEnvironment.dev,
        };

    return switch (resolvedEnv) {
      AppEnvironment.dev => AppFlavorConfig(
          environment: AppEnvironment.dev,
          appName: 'LifeOS (Dev)',
          applicationId: 'vn.lifeos.app.dev',
          apiBaseUrl: _resolveDevApiBaseUrl(),
          enableSyncDiagnostics: true,
          defaultCurrency: 'VND',
          enableFcm: enableFcmDefine,
        ),
      AppEnvironment.staging => const AppFlavorConfig(
          environment: AppEnvironment.staging,
          appName: 'LifeOS (Staging)',
          applicationId: 'vn.lifeos.app.staging',
          apiBaseUrl: _apiUrlDefine == 'http://localhost:8080/api/v1'
              ? 'https://staging-api.lifeos.vn/api/v1'
              : _apiUrlDefine,
          enableSyncDiagnostics: true,
          defaultCurrency: 'VND',
          enableFcm: enableFcmDefine,
        ),
      AppEnvironment.prod => const AppFlavorConfig(
          environment: AppEnvironment.prod,
          appName: 'LifeOS',
          applicationId: 'vn.lifeos.app',
          apiBaseUrl: _apiUrlDefine == 'http://localhost:8080/api/v1'
              ? 'https://api.lifeos.vn/api/v1'
              : _apiUrlDefine,
          enableSyncDiagnostics: true,
          defaultCurrency: 'VND',
          enableFcm: enableFcmDefine,
        ),
    };
  }
}
