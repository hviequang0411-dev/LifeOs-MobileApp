import 'dart:convert';
import 'dart:typed_data';

import 'package:dio/dio.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:lifeos_mobile/core/database/app_database.dart';
import 'package:lifeos_mobile/core/l10n/vi_strings.dart';
import 'package:lifeos_mobile/core/network/api_client.dart';
import 'package:lifeos_mobile/core/providers/app_providers.dart';
import 'package:lifeos_mobile/features/auth/data/online_account_service.dart';
import 'package:lifeos_mobile/features/auth/presentation/account_flows_sheets.dart';
import 'package:lifeos_mobile/features/workspace/data/lifeos_local_repository.dart';
import 'package:lifeos_mobile/main.dart';

typedef _StubHandler = Future<ResponseBody> Function(RequestOptions options);

class _StubAdapter implements HttpClientAdapter {
  _StubAdapter(this.handler);
  final _StubHandler handler;

  @override
  void close({bool force = false}) {}

  @override
  Future<ResponseBody> fetch(
    RequestOptions options,
    Stream<Uint8List>? requestStream,
    Future<void>? cancelFuture,
  ) =>
      handler(options);
}

ResponseBody _jsonResponse(Map<String, dynamic> body, int status) {
  return ResponseBody.fromString(
    jsonEncode(body),
    status,
    headers: <String, List<String>>{
      Headers.contentTypeHeader: <String>[Headers.jsonContentType],
    },
  );
}

void main() {
  group('MWP-B — Tài khoản đầy đủ, ánh xạ lỗi OpenAPI & giữ dữ liệu Phương án C', () {
    test('Ánh xạ toàn bộ mã lỗi AUTH_* và VALIDATION_FAILED sang tiếng Việt không lộ mã kỹ thuật', () {
      for (final String code in const <String>[
        'AUTH_INVALID_CREDENTIALS',
        'AUTH_EMAIL_NOT_VERIFIED',
        'AUTH_TOKEN_EXPIRED',
        'AUTH_TOKEN_INVALID',
        'AUTH_REFRESH_TOKEN_INVALID',
        'AUTH_VERIFICATION_TOKEN_INVALID',
        'AUTH_RESET_TOKEN_INVALID',
        'AUTH_PASSWORD_TOO_WEAK',
        'AUTH_CURRENT_PASSWORD_WRONG',
        'VALIDATION_FAILED',
        'RATE_LIMITED',
      ]) {
        final String msg = ViStrings.mapBackendErrorCode(code);
        expect(msg, isNotEmpty);
        expect(msg, isNot(contains('AUTH_')));
        expect(msg, isNot(contains('VALIDATION_')));
      }
    });

    testWidgets('Đăng nhập, xử lý lỗi server/offline, vào Hôm nay với tên người dùng & Đăng xuất giữ SQLite', (WidgetTester tester) async {
      tester.view.physicalSize = const Size(390, 844);
      tester.view.devicePixelRatio = 1.0;
      addTearDown(tester.view.resetPhysicalSize);
      addTearDown(tester.view.resetDevicePixelRatio);

      final AppDatabase db = AppDatabase.inMemory();
      addTearDown(db.close);
      final InMemoryTokenStorage storage = InMemoryTokenStorage();
      bool simulateOffline = false;
      bool wrongPass = true;

      final Dio dio = Dio(BaseOptions(baseUrl: 'http://localhost:8080/api/v1'));
      dio.httpClientAdapter = _StubAdapter((RequestOptions options) async {
        if (simulateOffline) {
          throw DioException(
            requestOptions: options,
            type: DioExceptionType.connectionError,
          );
        }
        if (options.path == '/auth/login') {
          if (wrongPass) {
            return _jsonResponse(
              <String, dynamic>{
                'errorCode': 'AUTH_INVALID_CREDENTIALS',
                'message': 'Bad credentials',
              },
              401,
            );
          }
          return _jsonResponse(
            <String, dynamic>{
              'accessToken': 'acc-1',
              'refreshToken': 'ref-1',
              'deviceId': 'dev-1',
              'user': <String, dynamic>{
                'id': 'user-local-01',
                'email': 'minhanh@lifeos.vn',
                'displayName': 'Minh Anh',
              },
            },
            200,
          );
        }
        return _jsonResponse(<String, dynamic>{}, 404);
      });

      final LifeOsApiClient apiClient = LifeOsApiClient(
        baseUrl: 'http://localhost:8080/api/v1',
        tokenStorage: storage,
        customDio: dio,
      );

      // Seed sẵn 1 task trên máy để xác minh Đăng xuất giữ dữ liệu cục bộ (Phương án C)
      LifeOsLocalRepository(db: db).createTask(
        userId: 'user-local-01',
        title: 'Việc cần giữ sau đăng xuất',
      );

      await tester.pumpWidget(
        ProviderScope(
          overrides: <Override>[
            appDatabaseProvider.overrideWithValue(db),
            tokenStorageProvider.overrideWithValue(storage),
            apiClientProvider.overrideWithValue(apiClient),
            isSignedOutProvider.overrideWith((Ref ref) => true),
          ],
          child: const LifeOsApp(),
        ),
      );
      await tester.pumpAndSettle();

      expect(find.text('Chào mừng trở lại LifeOS'), findsOneWidget);
      // Kiểm tra vùng chạm nút Đăng nhập >= 48dp
      expect(
        tester.getSize(find.byKey(const Key('btn_submit_login'))).height,
        greaterThanOrEqualTo(48.0),
      );

      // Kiểm tra validate blur-first khi để trống email
      await tester.tap(find.byKey(const Key('input_login_email')));
      await tester.pump();
      await tester.tap(find.byKey(const Key('input_login_password')));
      await tester.pump();
      expect(find.text('Vui lòng nhập email.'), findsOneWidget);

      // Trường hợp 1: Mất mạng -> báo rõ cần mạng
      await tester.enterText(
        find.byKey(const Key('input_login_email')),
        'minhanh@lifeos.vn',
      );
      await tester.enterText(
        find.byKey(const Key('input_login_password')),
        'Password123!',
      );
      simulateOffline = true;
      await tester.tap(find.byKey(const Key('btn_submit_login')));
      await tester.pumpAndSettle();
      expect(find.textContaining('Cần kết nối mạng'), findsOneWidget);

      // Trường hợp 2: Sai mật khẩu -> thông điệp tiếng Việt chuẩn (không lộ AUTH_INVALID_CREDENTIALS)
      simulateOffline = false;
      wrongPass = true;
      await tester.tap(find.byKey(const Key('btn_submit_login')));
      await tester.pumpAndSettle();
      expect(
        find.text('Email hoặc mật khẩu chưa chính xác. Vui lòng kiểm tra và thử lại.'),
        findsOneWidget,
      );

      // Trường hợp 3: Đăng nhập thành công -> vào thẳng Hôm nay với tên người dùng
      wrongPass = false;
      await tester.tap(find.byKey(const Key('btn_submit_login')));
      await tester.pumpAndSettle();
      expect(find.text('Chào ngày mới, Minh Anh'), findsOneWidget);
      expect(await storage.readAccessToken(), 'acc-1');

      // Đăng xuất trong Cài đặt -> token xóa nhưng dữ liệu SQLite + Outbox còn nguyên (Phương án C)
      await tester.tap(find.text('Tổng quan'));
      await tester.pumpAndSettle();
      final Finder logoutTile = find.byKey(const Key('tile_logout_keep_local'));
      await tester.scrollUntilVisible(
        logoutTile,
        300,
        scrollable: find.byType(Scrollable).first,
      );
      await tester.ensureVisible(logoutTile);
      await tester.pumpAndSettle();
      await tester.tap(logoutTile);
      await tester.pumpAndSettle();

      expect(find.text('Chào mừng trở lại LifeOS'), findsOneWidget);
      expect(await storage.readAccessToken(), isNull);
      expect(db.listOutboxMutations(userId: 'user-local-01').length, 1);
    });

    testWidgets('Đăng ký + Xác minh email + Gửi lại mã + Quên mật khẩu + Đổi mật khẩu qua OpenAPI contract', (WidgetTester tester) async {
      tester.view.physicalSize = const Size(390, 844);
      tester.view.devicePixelRatio = 1.0;
      addTearDown(tester.view.resetPhysicalSize);
      addTearDown(tester.view.resetDevicePixelRatio);

      final List<String> calledPaths = <String>[];
      final Dio dio = Dio(BaseOptions(baseUrl: 'http://localhost:8080/api/v1'));
      dio.httpClientAdapter = _StubAdapter((RequestOptions options) async {
        calledPaths.add(options.path);
        return _jsonResponse(<String, dynamic>{}, options.path.contains('register') || options.path.contains('forgot') || options.path.contains('resend') ? 202 : 204);
      });

      final OnlineAccountService service = OnlineAccountService(
        apiClient: LifeOsApiClient(
          baseUrl: 'http://localhost:8080/api/v1',
          tokenStorage: InMemoryTokenStorage(),
          customDio: dio,
        ),
      );

      await tester.pumpWidget(
        ProviderScope(
          overrides: <Override>[
            onlineAccountServiceProvider.overrideWithValue(service),
          ],
          child: MaterialApp(
            home: Scaffold(
              body: Builder(
                builder: (BuildContext context) => Column(
                  children: <Widget>[
                    ElevatedButton(
                      onPressed: () => showModalBottomSheet<void>(
                        context: context,
                        isScrollControlled: true,
                        builder: (_) => const RegisterAndVerifySheet(),
                      ),
                      child: const Text('Open Register'),
                    ),
                    ElevatedButton(
                      onPressed: () => showModalBottomSheet<void>(
                        context: context,
                        isScrollControlled: true,
                        builder: (_) => const ForgotPasswordSheet(),
                      ),
                      child: const Text('Open Forgot'),
                    ),
                    ElevatedButton(
                      onPressed: () => showDialog<void>(
                        context: context,
                        builder: (_) => const ChangePasswordDialog(),
                      ),
                      child: const Text('Open ChangePass'),
                    ),
                  ],
                ),
              ),
            ),
          ),
        ),
      );

      // 1. Đăng ký -> Gửi lại mã -> Xác minh
      await tester.tap(find.text('Open Register'));
      await tester.pumpAndSettle();
      await tester.enterText(find.byKey(const Key('input_register_name')), 'Bảo Ngọc');
      await tester.enterText(find.byKey(const Key('input_register_email')), 'baongoc@lifeos.vn');
      await tester.enterText(find.byKey(const Key('input_register_password')), 'MatKhauManh123');
      await tester.enterText(find.byKey(const Key('input_register_confirm_password')), 'MatKhauManh123');
      await tester.tap(find.byKey(const Key('btn_submit_register')));
      await tester.pumpAndSettle();

      await tester.tap(find.byKey(const Key('btn_resend_verification')));
      await tester.pumpAndSettle();
      await tester.enterText(find.byKey(const Key('input_verify_token')), 'tok-email-verify');
      await tester.tap(find.byKey(const Key('btn_submit_verify_email')));
      await tester.pumpAndSettle();

      // 2. Quên mật khẩu -> Đặt lại mật khẩu
      await tester.tap(find.text('Open Forgot'));
      await tester.pumpAndSettle();
      await tester.enterText(find.byKey(const Key('input_forgot_email')), 'baongoc@lifeos.vn');
      await tester.tap(find.byKey(const Key('btn_send_forgot_email')));
      await tester.pumpAndSettle();
      await tester.enterText(find.byKey(const Key('input_reset_token')), 'tok-reset-99');
      await tester.enterText(find.byKey(const Key('input_reset_new_password')), 'MatKhauMoi456');
      await tester.tap(find.byKey(const Key('btn_submit_reset_password')));
      await tester.pumpAndSettle();

      // 3. Đổi mật khẩu
      await tester.tap(find.text('Open ChangePass'));
      await tester.pumpAndSettle();
      await tester.enterText(find.byKey(const Key('input_change_current_password')), 'MatKhauMoi456');
      await tester.enterText(find.byKey(const Key('input_change_new_password')), 'MatKhauSieuManh789');
      await tester.enterText(find.byKey(const Key('input_change_confirm_password')), 'MatKhauSieuManh789');
      await tester.tap(find.byKey(const Key('btn_submit_change_password')));
      await tester.pumpAndSettle();

      expect(calledPaths, <String>[
        '/auth/register',
        '/auth/resend-verification',
        '/auth/verify-email',
        '/auth/forgot-password',
        '/auth/reset-password',
        '/me/change-password',
      ]);
    });
  });
}

