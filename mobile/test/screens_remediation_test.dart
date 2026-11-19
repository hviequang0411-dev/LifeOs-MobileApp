import 'dart:async';
import 'dart:convert';
import 'dart:typed_data';

import 'package:dio/dio.dart';
import 'package:flutter/material.dart';
import 'package:flutter_localizations/flutter_localizations.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:lifeos_mobile/core/database/app_database.dart';
import 'package:lifeos_mobile/core/l10n/vi_strings.dart';
import 'package:lifeos_mobile/core/money/decimal10k.dart';
import 'package:lifeos_mobile/core/network/api_client.dart';
import 'package:lifeos_mobile/core/providers/app_providers.dart';
import 'package:lifeos_mobile/core/sync/entity_types.dart';
import 'package:lifeos_mobile/core/theme/app_theme.dart';
import 'package:lifeos_mobile/core/widgets/lifeos_widgets.dart';
import 'package:lifeos_mobile/features/ai/data/ai_api.dart';
import 'package:lifeos_mobile/features/ai/data/ai_models.dart';
import 'package:lifeos_mobile/features/ai/data/ai_providers.dart';
import 'package:lifeos_mobile/features/ai/presentation/ai_assistant_screen.dart';
import 'package:lifeos_mobile/features/ai/presentation/widgets/ai_context_disclosure_sheet.dart';
import 'package:lifeos_mobile/features/ai/presentation/widgets/ai_plans_and_insights_sheets.dart';
import 'package:lifeos_mobile/features/habits/presentation/widgets/habit_editor_sheet.dart';
import 'package:lifeos_mobile/features/knowledge/presentation/widgets/goal_milestones_sheet.dart';
import 'package:lifeos_mobile/features/knowledge/presentation/widgets/note_editor_sheet.dart';
import 'package:lifeos_mobile/features/review/presentation/widgets/account_and_settings_sheets.dart';
import 'package:lifeos_mobile/features/review/presentation/widgets/review_wizard_sheet.dart';
import 'package:lifeos_mobile/features/tasks/presentation/widgets/task_editor_sheet.dart';
import 'package:lifeos_mobile/features/workspace/data/lifeos_local_repository.dart';
import 'package:lifeos_mobile/features/workspace/data/local_extras_repository.dart';
import 'package:lifeos_mobile/features/workspace/presentation/lifeos_shell_screen.dart';
import 'package:lifeos_mobile/features/workspace/presentation/widgets/global_search_sheet.dart';

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

class _TestAiApiClient extends AiApiClient {
  _TestAiApiClient(
    super.apiClient, {
    required this.prefs,
    this.onWeeklyPlanCalled,
  });

  final AiPreferencesResponse prefs;
  final void Function(String periodStart, List<String> domains)?
      onWeeklyPlanCalled;

  @override
  Future<AiPreferencesResponse> getPreferences() async => prefs;

  @override
  Future<AiConversationResponse> createConversation({String? title}) async {
    return const AiConversationResponse(
      id: 'conv-remediation-1',
      title: 'Phiên kiểm thử',
      createdAt: '2026-11-08T02:00:00Z',
      updatedAt: '2026-11-08T02:00:00Z',
      expiresAt: '2026-11-08T02:30:00Z',
    );
  }

  @override
  Future<AiPlanResponse> createWeeklyPlan(
    String periodStart,
    List<String> domains,
  ) async {
    onWeeklyPlanCalled?.call(periodStart, domains);
    return AiPlanResponse(
      periodType: 'WEEK',
      periodStart: periodStart,
      summary: 'Ưu tiên hoàn thành 3 đầu việc trọng tâm đầu tuần.',
      items: const <AiPlanItem>[
        AiPlanItem(
          title: 'Rà soát backlog Sprint 1',
          reason: 'Chốt phạm vi trước thứ Ba',
          priority: 'HIGH',
        ),
      ],
      contextMessageId: 'msg-ctx-week',
    );
  }
}

Widget _wrapApp({
  required AppDatabase db,
  LifeOsLocalExtrasDatabase? extras,
  LifeOsApiClient? apiClient,
  AiApiClient? aiApiClient,
  DateTime? referenceUtc,
  required Widget home,
  List<Override> extraOverrides = const <Override>[],
}) {
  return ProviderScope(
    overrides: <Override>[
      appDatabaseProvider.overrideWithValue(db),
      if (extras != null) localExtrasDatabaseProvider.overrideWithValue(extras),
      if (apiClient != null) apiClientProvider.overrideWithValue(apiClient),
      if (aiApiClient != null) aiApiClientProvider.overrideWithValue(aiApiClient),
      referenceUtcProvider.overrideWithValue(
        referenceUtc ?? DateTime.utc(2026, 11, 8, 2, 0),
      ),
      ...extraOverrides,
    ],
    child: MaterialApp(
      theme: LifeOsTheme.light(),
      localizationsDelegates: GlobalMaterialLocalizations.delegates,
      supportedLocales: const <Locale>[Locale('vi'), Locale('en')],
      home: home,
    ),
  );
}

void main() {
  const String userId = 'user-local-01';
  final DateTime fixedUtc = DateTime.utc(2026, 11, 8, 2, 0);

  group('Wave 1 — Đường dẫn AI, LocalPrefs, QuickCapture Finance, Logout & Banners', () {
    test('AiApiClient nối đường dẫn /ai/... lên baseUrl .../api/v1 không trùng /api/v1/api/v1', () async {
      final List<String> seenPaths = <String>[];
      final InMemoryTokenStorage storage = InMemoryTokenStorage();
      await storage.saveSession(
        accessToken: 'acc',
        refreshToken: 'ref',
        userId: userId,
      );
      final LifeOsApiClient apiClient = LifeOsApiClient(
        baseUrl: 'http://localhost:8080/api/v1',
        tokenStorage: storage,
      );
      apiClient.dio.httpClientAdapter = _StubAdapter((RequestOptions options) async {
        seenPaths.add(options.uri.path);
        if (options.path == '/ai/preferences') {
          return _jsonResponse(
            AiPreferencesResponse.initialDefault().toJson(),
            200,
          );
        }
        if (options.path == '/ai/plans/daily') {
          return _jsonResponse(<String, dynamic>{
            'periodType': 'DAY',
            'periodStart': '2026-11-08',
            'summary': 'OK',
            'items': const <dynamic>[],
            'contextMessageId': 'msg-1',
          }, 200);
        }
        if (options.path == '/ai/messages/msg-1/context') {
          return _jsonResponse(<String, dynamic>{
            'messageId': 'msg-1',
            'capturedAt': '2026-11-08T02:00:00Z',
            'domains': const <String>['TASK_CALENDAR'],
            'entities': const <dynamic>[],
            'truncated': false,
          }, 200);
        }
        return _jsonResponse(<String, dynamic>{}, 404);
      });

      final AiApiClient aiApi = AiApiClient(apiClient);
      await aiApi.getPreferences();
      await aiApi.createDailyPlan('2026-11-08', const <String>['TASK_CALENDAR']);
      await aiApi.getMessageContext('msg-1');

      expect(
        seenPaths,
        const <String>[
          '/api/v1/ai/preferences',
          '/api/v1/ai/plans/daily',
          '/api/v1/ai/messages/msg-1/context',
        ],
      );
      for (final String p in seenPaths) {
        expect(p.contains('/api/v1/api/v1'), isFalse);
      }
    });

    test('LifeOsLocalExtrasDatabase lưu & xóa local_device_prefs trong clearAll()', () {
      final LifeOsLocalExtrasDatabase extras = LifeOsLocalExtrasDatabase.inMemory();
      addTearDown(extras.close);

      expect(extras.getPref('theme_mode'), isNull);
      extras.setPref('theme_mode', 'dark');
      extras.setPref('notification_prefs', '{"tasksEnabled":false}');
      expect(extras.getPref('theme_mode'), 'dark');

      extras.setPref('theme_mode', 'light');
      expect(extras.getPref('theme_mode'), 'light');

      extras.deletePref('theme_mode');
      expect(extras.getPref('theme_mode'), isNull);
      expect(extras.getPref('notification_prefs'), isNotNull);

      extras.clearAll();
      expect(extras.getPref('notification_prefs'), isNull);
    });

    testWidgets('QuickCaptureBottomSheet tab Thu/Chi tạo ví thật UUID thay cho default-wallet',
        (WidgetTester tester) async {
      tester.view.physicalSize = const Size(430, 932);
      tester.view.devicePixelRatio = 1.0;
      addTearDown(tester.view.resetPhysicalSize);
      addTearDown(tester.view.resetDevicePixelRatio);

      final AppDatabase db = AppDatabase.inMemory();
      addTearDown(db.close);

      await tester.pumpWidget(
        _wrapApp(
          db: db,
          home: const Scaffold(body: QuickCaptureBottomSheet()),
        ),
      );
      await tester.pumpAndSettle();

      await tester.tap(find.text(ViStrings.quickTabFinance));
      await tester.pumpAndSettle();

      // Chưa có ví nào -> bấm nút tạo nhanh ví tiền mặt
      await tester.ensureVisible(find.byKey(const Key('btn_quick_create_cash_wallet')));
      await tester.tap(find.byKey(const Key('btn_quick_create_cash_wallet')));
      await tester.pumpAndSettle();

      final List<LocalEntityRow> accounts =
          db.listEntities(userId: userId, entityType: SyncEntityType.financeAccount);
      expect(accounts.length, 1);
      final String realAccountId = accounts.first.id;
      expect(realAccountId, isNot('default-wallet'));

      // Mặc định 50.000 -> xóa 1 số (5000) rồi nhập thêm 0 (50000) qua bàn phím số
      final Finder delKey = find.descendant(
        of: find.byType(CustomFinanceNumpad),
        matching: find.text('Xóa'),
      );
      final Finder zeroKey = find.descendant(
        of: find.byType(CustomFinanceNumpad),
        matching: find.text('0'),
      );
      await tester.ensureVisible(delKey);
      await tester.tap(delKey);
      await tester.pumpAndSettle();
      await tester.ensureVisible(zeroKey);
      await tester.tap(zeroKey);
      await tester.pumpAndSettle();

      await tester.ensureVisible(find.byKey(const Key('btn_submit_quick_finance')));
      await tester.tap(find.byKey(const Key('btn_submit_quick_finance')));
      await tester.pumpAndSettle();

      final List<LocalEntityRow> txs = db.listEntities(
        userId: userId,
        entityType: SyncEntityType.financeTransaction,
      );
      expect(txs.length, 1);
      expect(txs.first.payload['accountId'], realAccountId);
      expect(txs.first.payload['amount'], '50000');
    });

    testWidgets('LifeOsShellScreen banners gọi DELETE /account/deletion và đặt isSignedOutProvider = true',
        (WidgetTester tester) async {
      tester.view.physicalSize = const Size(430, 932);
      tester.view.devicePixelRatio = 1.0;
      addTearDown(tester.view.resetPhysicalSize);
      addTearDown(tester.view.resetDevicePixelRatio);

      final AppDatabase db = AppDatabase.inMemory();
      addTearDown(db.close);
      final LifeOsLocalExtrasDatabase extras = LifeOsLocalExtrasDatabase.inMemory();
      addTearDown(extras.close);

      bool cancelledOnServer = false;
      final InMemoryTokenStorage storage = InMemoryTokenStorage();
      await storage.saveSession(accessToken: 'a', refreshToken: 'r', userId: userId);
      final LifeOsApiClient apiClient = LifeOsApiClient(
        baseUrl: 'http://localhost:8080/api/v1',
        tokenStorage: storage,
      );
      apiClient.dio.httpClientAdapter = _StubAdapter((RequestOptions options) async {
        if (options.method == 'DELETE' && options.path == '/account/deletion') {
          cancelledOnServer = true;
          return ResponseBody.fromString('', 204);
        }
        return _jsonResponse(<String, dynamic>{}, 200);
      });

      late ProviderContainer capturedContainer;
      await tester.pumpWidget(
        _wrapApp(
          db: db,
          extras: extras,
          apiClient: apiClient,
          home: Consumer(
            builder: (BuildContext ctx, WidgetRef ref, Widget? _) {
              capturedContainer = ProviderScope.containerOf(ctx);
              return const LifeOsShellScreen();
            },
          ),
        ),
      );
      await tester.pumpAndSettle();

      capturedContainer.read(accountDeletionPendingBannerProvider.notifier).state = true;
      capturedContainer.read(requiresReAuthBannerProvider.notifier).state = true;
      await tester.pumpAndSettle();

      await tester.tap(find.byKey(const Key('btn_banner_cancel_deletion')));
      await tester.pumpAndSettle();
      expect(cancelledOnServer, isTrue);
      expect(capturedContainer.read(accountDeletionPendingBannerProvider), isFalse);

      await tester.tap(find.byKey(const Key('btn_banner_reauth')));
      await tester.pumpAndSettle();
      expect(capturedContainer.read(requiresReAuthBannerProvider), isFalse);
      expect(capturedContainer.read(isSignedOutProvider), isTrue);
    });

    testWidgets('SafeLogoutDialog khi Outbox sạch đặt isSignedOutProvider = true',
        (WidgetTester tester) async {
      final AppDatabase db = AppDatabase.inMemory();
      addTearDown(db.close);
      final InMemoryTokenStorage storage = InMemoryTokenStorage();
      await storage.saveSession(accessToken: 'a', refreshToken: 'r', userId: userId);
      final LifeOsApiClient apiClient = LifeOsApiClient(
        baseUrl: 'http://localhost:8080/api/v1',
        tokenStorage: storage,
      );
      apiClient.dio.httpClientAdapter = _StubAdapter((RequestOptions options) async {
        return ResponseBody.fromString('', 204);
      });

      late ProviderContainer capturedContainer;
      await tester.pumpWidget(
        _wrapApp(
          db: db,
          apiClient: apiClient,
          home: Consumer(
            builder: (BuildContext ctx, WidgetRef ref, Widget? _) {
              capturedContainer = ProviderScope.containerOf(ctx);
              return const Scaffold(body: SafeLogoutDialog());
            },
          ),
        ),
      );
      await tester.pumpAndSettle();

      await tester.tap(find.byKey(const Key('btn_confirm_clean_logout')));
      await tester.pumpAndSettle();
      expect(capturedContainer.read(isSignedOutProvider), isTrue);
    });
  });

  group('Wave 2 — Task/Habit Editor, Finance F1/F6, Plan Day Offset & AI Weekly Plan', () {
    testWidgets('TaskEditorSheet ẩn nút AI Breakdown khi chưa consent & cho phép sửa/xóa mềm công việc',
        (WidgetTester tester) async {
      final AppDatabase db = AppDatabase.inMemory();
      addTearDown(db.close);
      final LifeOsLocalRepository repo = LifeOsLocalRepository(db: db);
      final String taskId = repo.createTask(
        userId: userId,
        title: 'Chuẩn bị tài liệu họp',
        priority: 'MEDIUM',
        nowUtc: fixedUtc,
      );
      final LocalEntityRow task = db.getEntity(
        userId: userId,
        entityType: SyncEntityType.task,
        entityId: taskId,
      )!;

      final LifeOsApiClient apiClient = LifeOsApiClient(
        baseUrl: 'http://localhost:8080/api/v1',
        tokenStorage: InMemoryTokenStorage(),
      );
      final _TestAiApiClient aiApi = _TestAiApiClient(
        apiClient,
        prefs: AiPreferencesResponse.initialDefault(),
      );

      await tester.pumpWidget(
        _wrapApp(
          db: db,
          apiClient: apiClient,
          aiApiClient: aiApi,
          home: Scaffold(body: TaskEditorSheet(task: task)),
        ),
      );
      await tester.pumpAndSettle();

      // Khi taskCalendar consent TẮT -> nút btn_ai_task_breakdown phải ẩn (AWP-6 gating)
      expect(find.byKey(const Key('btn_ai_task_breakdown')), findsNothing);

      // Chỉnh sửa tiêu đề và lưu
      await tester.ensureVisible(find.byKey(const Key('input_edit_task_title')));
      await tester.enterText(
        find.byKey(const Key('input_edit_task_title')),
        'Chuẩn bị tài liệu họp (Đã duyệt)',
      );
      await tester.ensureVisible(find.byKey(const Key('btn_save_task')));
      await tester.tap(find.byKey(const Key('btn_save_task')));
      await tester.pumpAndSettle();

      final LocalEntityRow updated = db.getEntity(
        userId: userId,
        entityType: SyncEntityType.task,
        entityId: taskId,
      )!;
      expect(updated.payload['title'], 'Chuẩn bị tài liệu họp (Đã duyệt)');
    });

    testWidgets('TaskEditorSheet hiện nút AI Breakdown khi taskCalendar consent BẬT và hỗ trợ xóa mềm',
        (WidgetTester tester) async {
      final AppDatabase db = AppDatabase.inMemory();
      addTearDown(db.close);
      final LifeOsLocalRepository repo = LifeOsLocalRepository(db: db);
      final String taskId = repo.createTask(
        userId: userId,
        title: 'Việc cần chia nhỏ',
        nowUtc: fixedUtc,
      );
      final LocalEntityRow task = db.getEntity(
        userId: userId,
        entityType: SyncEntityType.task,
        entityId: taskId,
      )!;

      final LifeOsApiClient apiClient = LifeOsApiClient(
        baseUrl: 'http://localhost:8080/api/v1',
        tokenStorage: InMemoryTokenStorage(),
      );
      final _TestAiApiClient aiApi = _TestAiApiClient(
        apiClient,
        prefs: const AiPreferencesResponse(
          taskCalendar: true,
          habit: false,
          goal: false,
          notes: false,
          finance: false,
          policyVersion: '2026-11-10',
          providerNoticeVersion: 'openai-api-2026-11-10',
          features: AiFeatureFlags(
            chat: true,
            insights: true,
            notes: false,
            finance: false,
            rag: false,
            actions: false,
          ),
        ),
      );

      late ProviderContainer container;
      await tester.pumpWidget(
        _wrapApp(
          db: db,
          apiClient: apiClient,
          aiApiClient: aiApi,
          home: Consumer(
            builder: (BuildContext ctx, WidgetRef ref, Widget? _) {
              container = ProviderScope.containerOf(ctx);
              return Scaffold(body: TaskEditorSheet(task: task));
            },
          ),
        ),
      );
      await tester.pumpAndSettle();
      await container.read(aiPreferencesProvider.notifier).loadPreferences();
      await tester.pumpAndSettle();

      await tester.ensureVisible(find.byKey(const Key('btn_ai_task_breakdown')));
      expect(find.byKey(const Key('btn_ai_task_breakdown')), findsOneWidget);

      await tester.tap(find.byKey(const Key('btn_delete_task')));
      await tester.pumpAndSettle();
      await tester.tap(find.byKey(const Key('btn_confirm_delete_task')));
      await tester.pumpAndSettle();

      expect(
        db.getEntity(
          userId: userId,
          entityType: SyncEntityType.task,
          entityId: taskId,
        ),
        isNull,
      );
    });

    testWidgets('HabitEditorSheet tạo thói quen SPECIFIC_DAYS kèm bitmask weekdays',
        (WidgetTester tester) async {
      tester.view.physicalSize = const Size(430, 932);
      tester.view.devicePixelRatio = 1.0;
      addTearDown(tester.view.resetPhysicalSize);
      addTearDown(tester.view.resetDevicePixelRatio);

      final AppDatabase db = AppDatabase.inMemory();
      addTearDown(db.close);

      await tester.pumpWidget(
        _wrapApp(
          db: db,
          home: const Scaffold(body: HabitEditorSheet()),
        ),
      );
      await tester.pumpAndSettle();

      await tester.enterText(
        find.byKey(const Key('input_habit_name')),
        'Đọc tài liệu kỹ thuật',
      );
      await tester.ensureVisible(find.text('Chọn thứ'));
      await tester.tap(find.text('Chọn thứ'));
      await tester.pumpAndSettle();

      // Mặc định bật cả 7 ngày {1..7}; tắt T3, T5, T6, T7, CN để giữ lại T2 (1) & T4 (3)
      for (final String label in const <String>['T3', 'T5', 'T6', 'T7', 'CN']) {
        final Finder chip = find.widgetWithText(FilterChip, label);
        await tester.ensureVisible(chip);
        await tester.pumpAndSettle();
        await tester.tap(chip);
        await tester.pumpAndSettle();
      }

      await tester.ensureVisible(find.byKey(const Key('btn_save_habit')));
      await tester.tap(find.byKey(const Key('btn_save_habit')));
      await tester.pumpAndSettle();

      final List<LocalEntityRow> habits =
          db.listEntities(userId: userId, entityType: SyncEntityType.habit);
      expect(habits.length, 1);
      expect(habits.first.payload['name'], 'Đọc tài liệu kỹ thuật');
      expect(habits.first.payload['daysOfWeek'], const <int>[1, 3]);
    });

    test('Finance F1 & F6: chặn chuyển đổi khác tiền tệ và lưu trữ ví tự động PAUSED định kỳ giữ nguyên nextRunOn', () {
      final AppDatabase db = AppDatabase.inMemory();
      addTearDown(db.close);
      final LifeOsLocalRepository repo = LifeOsLocalRepository(db: db);

      final String vndAccId = repo.createFinanceAccount(
        userId: userId,
        name: 'Ví VND',
        currency: 'VND',
        openingBalance: Decimal10k.fromWhole(1000000),
        nowUtc: fixedUtc,
      );
      final String usdAccId = repo.createFinanceAccount(
        userId: userId,
        name: 'Ví USD',
        currency: 'USD',
        openingBalance: Decimal10k.fromWhole(100),
        nowUtc: fixedUtc,
      );
      final String recId = repo.finance.saveRecurringTransaction(
        userId: userId,
        accountId: vndAccId,
        kind: 'EXPENSE',
        amount: Decimal10k.fromWhole(50000),
        frequency: 'MONTHLY',
        intervalCount: 1,
        startOnIso: '2026-11-01',
        nowUtc: fixedUtc,
      );

      final LocalEntityRow recBefore = db.getEntity(
        userId: userId,
        entityType: SyncEntityType.recurringTransaction,
        entityId: recId,
      )!;
      final dynamic nextRunBefore = recBefore.payload['nextRunOn'];

      // F1: đổi accountId sang ví khác tiền tệ -> ném FINANCE_CURRENCY_MISMATCH
      expect(
        () => repo.finance.saveRecurringTransaction(
          userId: userId,
          recurringId: recId,
          accountId: usdAccId,
          kind: 'EXPENSE',
          amount: Decimal10k.fromWhole(50000),
          frequency: 'MONTHLY',
          intervalCount: 1,
          startOnIso: '2026-11-01',
          nowUtc: fixedUtc,
        ),
        throwsA(isA<StateError>().having(
          (StateError e) => e.message,
          'message',
          contains('FINANCE_CURRENCY_MISMATCH'),
        )),
      );

      // F6: lưu trữ ví VND -> tự động PAUSED giao dịch định kỳ và giữ nguyên nextRunOn
      repo.finance.updateFinanceAccount(
        userId: userId,
        accountId: vndAccId,
        name: 'Ví VND',
        archived: true,
        nowUtc: fixedUtc.add(const Duration(minutes: 1)),
      );

      final LocalEntityRow recAfter = db.getEntity(
        userId: userId,
        entityType: SyncEntityType.recurringTransaction,
        entityId: recId,
      )!;
      expect(recAfter.payload['status'], 'PAUSED');
      expect(recAfter.payload['nextRunOn'], nextRunBefore);

      // F6: không thể tạo giao dịch định kỳ ACTIVE trên ví đã lưu trữ
      expect(
        () => repo.finance.saveRecurringTransaction(
          userId: userId,
          accountId: vndAccId,
          kind: 'EXPENSE',
          amount: Decimal10k.fromWhole(10000),
          frequency: 'WEEKLY',
          intervalCount: 1,
          startOnIso: '2026-11-08',
          status: 'ACTIVE',
          nowUtc: fixedUtc,
        ),
        throwsA(isA<StateError>().having(
          (StateError e) => e.message,
          'message',
          contains('FINANCE_ACCOUNT_ARCHIVED'),
        )),
      );
    });

    testWidgets('PlanTabView không rò rỉ sự kiện đơn lẻ sang ngày khác và hỗ trợ ngoại lệ chuỗi lặp',
        (WidgetTester tester) async {
      tester.view.physicalSize = const Size(430, 932);
      tester.view.devicePixelRatio = 1.0;
      addTearDown(tester.view.resetPhysicalSize);
      addTearDown(tester.view.resetDevicePixelRatio);

      final AppDatabase db = AppDatabase.inMemory();
      addTearDown(db.close);
      final LifeOsLocalRepository repo = LifeOsLocalRepository(db: db);

      repo.createCalendarEvent(
        userId: userId,
        title: 'Họp Chủ Nhật duy nhất',
        startLocal: '2026-11-08T09:00:00',
        endLocal: '2026-11-08T10:00:00',
        nowUtc: fixedUtc,
      );

      final String recEventId = repo.createCalendarEvent(
        userId: userId,
        title: 'Standup hằng ngày',
        startLocal: '2026-11-08T08:30:00',
        endLocal: '2026-11-08T08:45:00',
        recurrence: const <String, dynamic>{'freq': 'DAILY', 'interval': 1},
        nowUtc: fixedUtc,
      );
      repo.calendar.recordEventOccurrenceException(
        userId: userId,
        eventId: recEventId,
        originalStartLocal: '2026-11-09T08:30:00',
        kind: 'MODIFIED',
        startLocal: '2026-11-09T09:30:00',
        endLocal: '2026-11-09T10:00:00',
        title: 'Standup dời lịch T2',
        nowUtc: fixedUtc,
      );

      await tester.pumpWidget(
        _wrapApp(
          db: db,
          referenceUtc: fixedUtc,
          home: const Scaffold(body: PlanTabView()),
        ),
      );
      await tester.pumpAndSettle();

      expect(find.text('Họp Chủ Nhật duy nhất'), findsOneWidget);
      expect(find.text('Standup hằng ngày'), findsOneWidget);

      // Chuyển sang ngày 2026-11-09 (số ngày "9" trong dải lịch 7 ngày)
      await tester.tap(find.text('9'));
      await tester.pumpAndSettle();

      expect(find.text('Họp Chủ Nhật duy nhất'), findsNothing);
      expect(find.text('Standup dời lịch T2'), findsOneWidget);
    });

    testWidgets('AiAssistantScreen hiển thị chip Lập kế hoạch tuần và gọi POST /ai/plans/weekly',
        (WidgetTester tester) async {
      final AppDatabase db = AppDatabase.inMemory();
      addTearDown(db.close);
      String? capturedWeekStart;

      final LifeOsApiClient apiClient = LifeOsApiClient(
        baseUrl: 'http://localhost:8080/api/v1',
        tokenStorage: InMemoryTokenStorage(),
      );
      final _TestAiApiClient aiApi = _TestAiApiClient(
        apiClient,
        prefs: const AiPreferencesResponse(
          taskCalendar: true,
          habit: false,
          goal: false,
          notes: false,
          finance: false,
          policyVersion: '2026-11-10',
          providerNoticeVersion: 'openai-api-2026-11-10',
          features: AiFeatureFlags(
            chat: true,
            insights: true,
            notes: false,
            finance: false,
            rag: false,
            actions: false,
          ),
        ),
        onWeeklyPlanCalled: (String periodStart, _) {
          capturedWeekStart = periodStart;
        },
      );

      await tester.pumpWidget(
        _wrapApp(
          db: db,
          apiClient: apiClient,
          aiApiClient: aiApi,
          referenceUtc: fixedUtc,
          extraOverrides: <Override>[
            aiIsOnlineProvider.overrideWith((Ref ref) => true),
          ],
          home: const AiAssistantScreen(),
        ),
      );
      await tester.pumpAndSettle();

      await tester.tap(find.byKey(const Key('chip_ai_weekly_plan')));
      await tester.pumpAndSettle();

      expect(capturedWeekStart, '2026-11-02');
      expect(find.text('Gợi ý Kế hoạch Tuần'), findsOneWidget);
      expect(find.text('Cao'), findsOneWidget);
      expect(find.textContaining('Outbox'), findsNothing);
    });
  });

  group('Wave 3 — Lưu trữ/Nhãn Note, Validate Goal Date, Global Search, Review Đa Kỳ, Thông báo & Sạch UI', () {
    testWidgets('KnowledgeAndGoalsTabView lọc ghi chú lưu trữ, ưu tiên ghim trước & hiển thị #nhãn',
        (WidgetTester tester) async {
      tester.view.physicalSize = const Size(430, 932);
      tester.view.devicePixelRatio = 1.0;
      addTearDown(tester.view.resetPhysicalSize);
      addTearDown(tester.view.resetDevicePixelRatio);

      final AppDatabase db = AppDatabase.inMemory();
      addTearDown(db.close);
      final LifeOsLocalRepository repo = LifeOsLocalRepository(db: db);

      repo.saveNote(
        userId: userId,
        title: 'Ghi chú thường',
        content: 'Nội dung 1',
        pinned: false,
        nowUtc: fixedUtc,
      );
      repo.saveNote(
        userId: userId,
        title: 'Ghi chú đã lưu trữ',
        content: 'Nội dung ẩn',
        archived: true,
        nowUtc: fixedUtc,
      );
      final String pinnedId = repo.saveNote(
        userId: userId,
        title: 'Ghi chú được ghim',
        content: 'Ưu tiên lên đầu',
        pinned: true,
        nowUtc: fixedUtc,
      );
      final String tagId = repo.createTag(
        userId: userId,
        name: 'UuTien',
        nowUtc: fixedUtc,
      );
      repo.attachTagToNote(
        userId: userId,
        noteId: pinnedId,
        tagId: tagId,
        nowUtc: fixedUtc,
      );

      await tester.pumpWidget(
        _wrapApp(
          db: db,
          home: const Scaffold(body: KnowledgeAndGoalsTabView()),
        ),
      );
      await tester.pumpAndSettle();

      expect(find.text('Ghi chú được ghim'), findsOneWidget);
      expect(find.text('Ghi chú thường'), findsOneWidget);
      expect(find.text('Ghi chú đã lưu trữ'), findsNothing);
      expect(find.text('#UuTien'), findsOneWidget);

      final double pinnedY = tester.getTopLeft(find.text('Ghi chú được ghim')).dy;
      final double normalY = tester.getTopLeft(find.text('Ghi chú thường')).dy;
      expect(pinnedY, lessThan(normalY));

      await tester.tap(find.byKey(const Key('chip_toggle_archived_notes')));
      await tester.pumpAndSettle();
      expect(find.text('Ghi chú đã lưu trữ'), findsOneWidget);
    });

    testWidgets('NoteEditorSheet tạo nhãn mới qua dialog và gắn note_tag sau khi lưu ghi chú',
        (WidgetTester tester) async {
      tester.view.physicalSize = const Size(430, 932);
      tester.view.devicePixelRatio = 1.0;
      addTearDown(tester.view.resetPhysicalSize);
      addTearDown(tester.view.resetDevicePixelRatio);

      final AppDatabase db = AppDatabase.inMemory();
      addTearDown(db.close);

      await tester.pumpWidget(
        _wrapApp(
          db: db,
          referenceUtc: fixedUtc,
          home: const Scaffold(body: NoteEditorSheet()),
        ),
      );
      await tester.pumpAndSettle();

      await tester.enterText(
        find.byKey(const Key('input_note_title')),
        'Ghi chú có nhãn mới',
      );
      await tester.ensureVisible(find.byKey(const Key('btn_add_note_tag')));
      await tester.tap(find.byKey(const Key('btn_add_note_tag')));
      await tester.pumpAndSettle();

      await tester.enterText(
        find.byKey(const Key('input_new_tag_name')),
        'KienTruc',
      );
      await tester.tap(find.byKey(const Key('btn_confirm_create_tag')));
      await tester.pumpAndSettle();

      await tester.ensureVisible(find.byKey(const Key('btn_save_note')));
      await tester.tap(find.byKey(const Key('btn_save_note')));
      await tester.pumpAndSettle();

      final List<LocalEntityRow> tags =
          db.listEntities(userId: userId, entityType: SyncEntityType.tag);
      expect(tags.any((LocalEntityRow t) => t.payload['name'] == 'KienTruc'), isTrue);
      final List<LocalEntityRow> noteTags =
          db.listEntities(userId: userId, entityType: SyncEntityType.noteTag);
      expect(noteTags.length, 1);
    });

    testWidgets('CreateGoalSheet chặn ngày sai định dạng YYYY-MM-DD (2026-02-30)',
        (WidgetTester tester) async {
      final AppDatabase db = AppDatabase.inMemory();
      addTearDown(db.close);

      await tester.pumpWidget(
        _wrapApp(
          db: db,
          referenceUtc: fixedUtc,
          home: const Scaffold(body: CreateGoalSheet()),
        ),
      );
      await tester.pumpAndSettle();

      await tester.enterText(
        find.byKey(const Key('input_create_goal_title')),
        'Mục tiêu kiểm tra ngày',
      );
      await tester.enterText(
        find.byKey(const Key('input_create_goal_target_date')),
        '2026-02-30',
      );
      await tester.ensureVisible(find.byKey(const Key('btn_submit_create_goal')));
      await tester.tap(find.byKey(const Key('btn_submit_create_goal')));
      await tester.pumpAndSettle();

      expect(
        find.text('Ngày hoàn thành phải đúng định dạng YYYY-MM-DD.'),
        findsOneWidget,
      );
      expect(db.listEntities(userId: userId, entityType: SyncEntityType.goal), isEmpty);
    });

    testWidgets('GoalEditorSheet chặn ngày hoàn thành trước ngày bắt đầu (target < start)',
        (WidgetTester tester) async {
      final AppDatabase db = AppDatabase.inMemory();
      addTearDown(db.close);
      final LifeOsLocalRepository repo = LifeOsLocalRepository(db: db);

      final String goalId = repo.createGoal(
        userId: userId,
        title: 'Mục tiêu mẫu',
        startDateIso: '2026-11-01',
        targetDateIso: '2026-12-31',
        nowUtc: fixedUtc,
      );
      final LocalEntityRow goal = db.getEntity(
        userId: userId,
        entityType: SyncEntityType.goal,
        entityId: goalId,
      )!;

      await tester.pumpWidget(
        _wrapApp(
          db: db,
          referenceUtc: fixedUtc,
          home: Scaffold(body: GoalEditorSheet(goal: goal)),
        ),
      );
      await tester.pumpAndSettle();

      await tester.enterText(
        find.byKey(const Key('input_edit_goal_target_date')),
        '2026-10-01',
      );
      await tester.ensureVisible(find.byKey(const Key('btn_save_goal_changes')));
      await tester.tap(find.byKey(const Key('btn_save_goal_changes')));
      await tester.pumpAndSettle();

      expect(
        find.text('Ngày hoàn thành không được trước ngày bắt đầu.'),
        findsOneWidget,
      );
      expect(
        db.getEntity(userId: userId, entityType: SyncEntityType.goal, entityId: goalId)!
            .payload['targetOn'],
        '2026-12-31',
      );
    });

    testWidgets('GlobalSearchSheet tìm kiếm Mục tiêu & Thói quen và mở sheet chỉnh sửa khi chạm',
        (WidgetTester tester) async {
      final AppDatabase db = AppDatabase.inMemory();
      addTearDown(db.close);
      final LifeOsLocalRepository repo = LifeOsLocalRepository(db: db);

      final String goalId = repo.createGoal(
        userId: userId,
        title: 'Chạy marathon 42km',
        startDateIso: '2026-11-01',
        targetDateIso: '2026-12-31',
        nowUtc: fixedUtc,
      );
      repo.createHabit(
        userId: userId,
        name: 'Chạy bộ buổi sáng',
        nowUtc: fixedUtc,
      );

      await tester.pumpWidget(
        _wrapApp(
          db: db,
          home: const Scaffold(body: GlobalSearchSheet()),
        ),
      );
      await tester.pumpAndSettle();

      await tester.enterText(find.byKey(const Key('input_global_search')), 'Chạy');
      await tester.pumpAndSettle();

      expect(find.text('Chạy bộ buổi sáng'), findsOneWidget);
      expect(find.text('Chạy marathon 42km'), findsOneWidget);

      await tester.tap(find.byKey(Key('search_result_goal_$goalId')));
      await tester.pumpAndSettle();

      expect(find.byType(GoalEditorSheet), findsOneWidget);
    });

    testWidgets('ReviewWizardSheet hỗ trợ DAY/WEEK/MONTH theo UserDayClock, đếm COMPLETED và mở ReviewHistorySheet',
        (WidgetTester tester) async {
      tester.view.physicalSize = const Size(430, 932);
      tester.view.devicePixelRatio = 1.0;
      addTearDown(tester.view.resetPhysicalSize);
      addTearDown(tester.view.resetDevicePixelRatio);

      final AppDatabase db = AppDatabase.inMemory();
      addTearDown(db.close);
      final LifeOsLocalRepository repo = LifeOsLocalRepository(db: db);

      repo.createTask(
        userId: userId,
        title: 'Việc DONE',
        status: 'DONE',
        nowUtc: fixedUtc,
      );
      repo.createTask(
        userId: userId,
        title: 'Việc COMPLETED',
        status: 'COMPLETED',
        nowUtc: fixedUtc,
      );
      repo.createTask(
        userId: userId,
        title: 'Việc TODO',
        status: 'TODO',
        nowUtc: fixedUtc,
      );

      await tester.pumpWidget(
        _wrapApp(
          db: db,
          referenceUtc: fixedUtc,
          home: Builder(
            builder: (BuildContext ctx) => Scaffold(
              body: ElevatedButton(
                key: const Key('btn_open_wizard_host'),
                onPressed: () {
                  unawaited(
                    showModalBottomSheet<void>(
                      context: ctx,
                      isScrollControlled: true,
                      backgroundColor: Colors.transparent,
                      builder: (_) => const ReviewWizardSheet(),
                    ),
                  );
                },
                child: const Text('Mở Review'),
              ),
            ),
          ),
        ),
      );
      await tester.pumpAndSettle();

      await tester.tap(find.byKey(const Key('btn_open_wizard_host')));
      await tester.pumpAndSettle();

      // Đếm đúng cả DONE lẫn COMPLETED => 2 / 3 công việc hoàn tất
      expect(find.text('2 / 3 công việc hoàn tất'), findsOneWidget);

      // Chuyển chu kỳ Tuần -> 2026-11-08 là Chủ Nhật => đầu tuần Thứ Hai = 2026-11-02
      await tester.tap(find.byKey(const Key('chip_review_period_WEEK')));
      await tester.pumpAndSettle();
      expect(find.text('Chọn chu kỳ phản tư (2026-11-02)'), findsOneWidget);

      await tester.tap(find.text('Tiếp theo'));
      await tester.pumpAndSettle();
      await tester.tap(find.text('Tiếp theo'));
      await tester.pumpAndSettle();
      await tester.enterText(
        find.byType(TextField),
        'Tuần làm việc hiệu quả cao.',
      );
      await tester.tap(find.text('Hoàn tất'));
      await tester.pumpAndSettle();

      final List<LocalEntityRow> reviews =
          db.listEntities(userId: userId, entityType: SyncEntityType.lifeReview);
      expect(reviews.length, 1);
      expect(reviews.first.payload['periodType'], 'WEEK');
      expect(reviews.first.payload['periodKey'], '2026-11-02');

      // Mở lại ReviewWizardSheet -> bấm Lịch sử mở ReviewHistorySheet
      await tester.tap(find.byKey(const Key('btn_open_wizard_host')));
      await tester.pumpAndSettle();
      await tester.tap(find.byKey(const Key('btn_review_history')));
      await tester.pumpAndSettle();

      expect(find.byType(ReviewHistorySheet), findsOneWidget);
      expect(find.text('2026-11-02'), findsOneWidget);
      expect(find.text('Tuần làm việc hiệu quả cao.'), findsOneWidget);
    });

    testWidgets('NotificationsInboxSheet định dạng finance.recurring.paused, lọc chưa đọc & đánh dấu đã đọc',
        (WidgetTester tester) async {
      final AppDatabase db = AppDatabase.inMemory();
      addTearDown(db.close);

      bool markedRead = false;
      final LifeOsApiClient apiClient = LifeOsApiClient(
        baseUrl: 'http://localhost:8080/api/v1',
        tokenStorage: InMemoryTokenStorage(),
      );
      apiClient.dio.httpClientAdapter = _StubAdapter((RequestOptions options) async {
        if (options.method == 'GET' && options.path == '/notifications') {
          final bool unreadOnly = options.queryParameters['unreadOnly'] == true;
          final List<Map<String, dynamic>> items = <Map<String, dynamic>>[
            <String, dynamic>{
              'id': 'notif-1',
              'templateKey': 'finance.recurring.paused',
              'title': 'Recurring paused',
              'body': 'Tài khoản Ví Tiền Mặt đã lưu trữ.',
              'createdAt': '2026-11-08T02:00:00Z',
              'readAt': null,
            },
            if (!unreadOnly)
              <String, dynamic>{
                'id': 'notif-2',
                'title': 'Thông báo đã đọc trước đó',
                'body': 'Chi tiết cũ',
                'createdAt': '2026-11-07T02:00:00Z',
                'readAt': '2026-11-07T03:00:00Z',
              },
          ];
          return _jsonResponse(<String, dynamic>{
            'items': items,
            'nextCursor': null,
            'hasMore': false,
          }, 200);
        }
        if (options.method == 'POST' && options.path == '/notifications/notif-1/read') {
          markedRead = true;
          return _jsonResponse(<String, dynamic>{'id': 'notif-1'}, 200);
        }
        return _jsonResponse(<String, dynamic>{}, 200);
      });

      await tester.pumpWidget(
        _wrapApp(
          db: db,
          apiClient: apiClient,
          referenceUtc: fixedUtc,
          home: const Scaffold(body: NotificationsInboxSheet()),
        ),
      );
      await tester.pumpAndSettle();

      expect(
        find.text('Giao dịch định kỳ đã tạm dừng vì tài khoản đang lưu trữ'),
        findsOneWidget,
      );
      expect(find.text('Thông báo đã đọc trước đó'), findsOneWidget);

      await tester.tap(find.byKey(const Key('chip_notif_unread')));
      await tester.pumpAndSettle();
      expect(find.text('Thông báo đã đọc trước đó'), findsNothing);

      await tester.tap(find.byKey(const Key('notif_item_notif-1')));
      await tester.pumpAndSettle();
      expect(markedRead, isTrue);
    });

    testWidgets('AiContextDisclosureSheet không lộ (AWP-3) và dịch nhãn miền sang tiếng Việt',
        (WidgetTester tester) async {
      final AppDatabase db = AppDatabase.inMemory();
      addTearDown(db.close);
      final LifeOsApiClient apiClient = LifeOsApiClient(
        baseUrl: 'http://localhost:8080/api/v1',
        tokenStorage: InMemoryTokenStorage(),
      );
      apiClient.dio.httpClientAdapter = _StubAdapter((RequestOptions options) async {
        return _jsonResponse(<String, dynamic>{
          'messageId': 'msg-leak-check',
          'capturedAt': '2026-11-08T02:00:00Z',
          'domains': const <String>['TASK_CALENDAR'],
          'entities': const <dynamic>[
            <String, dynamic>{
              'domain': 'TASK_CALENDAR',
              'entityType': 'task',
              'entityId': 't-1',
              'entityVersion': 1,
              'fields': <String, dynamic>{'title': 'Việc mẫu'},
            },
          ],
          'truncated': false,
        }, 200);
      });

      await tester.pumpWidget(
        _wrapApp(
          db: db,
          apiClient: apiClient,
          home: const Scaffold(
            body: AiContextDisclosureSheet(messageId: 'msg-leak-check'),
          ),
        ),
      );
      await tester.pumpAndSettle();

      expect(find.textContaining('AWP-3'), findsNothing);
      expect(find.text('Công việc & Lịch'), findsWidgets);
    });

    testWidgets('AiTaskBreakdownSheet không lộ AI Breakdown hoặc Outbox trên UI',
        (WidgetTester tester) async {
      final AppDatabase db = AppDatabase.inMemory();
      addTearDown(db.close);

      await tester.pumpWidget(
        _wrapApp(
          db: db,
          home: const Scaffold(
            body: AiTaskBreakdownSheet(
              breakdown: AiTaskBreakdownResponse(
                sourceTaskId: 't-1',
                items: <AiTaskDraft>[
                  AiTaskDraft(
                    title: 'Bước 1',
                    priority: 'MEDIUM',
                    reason: 'Khởi động nhanh',
                  ),
                ],
                contextMessageId: 'msg-1',
              ),
            ),
          ),
        ),
      );
      await tester.pumpAndSettle();

      expect(find.textContaining('AI Breakdown'), findsNothing);
      await tester.tap(find.text('Thêm vào danh sách việc'));
      await tester.pumpAndSettle();
      expect(find.textContaining('Outbox'), findsNothing);
    });

    testWidgets('OverviewAndSettingsTabView tách riêng mục AI và không lộ (DELETE /ai/data)',
        (WidgetTester tester) async {
      final AppDatabase db = AppDatabase.inMemory();
      addTearDown(db.close);

      await tester.pumpWidget(
        _wrapApp(
          db: db,
          home: Scaffold(
            body: OverviewAndSettingsTabView(onOpenDiagnostics: () {}),
          ),
        ),
      );
      await tester.pumpAndSettle();

      await tester.scrollUntilVisible(
        find.byKey(const Key('tile_delete_ai_data')),
        300,
        scrollable: find.byType(Scrollable).last,
      );
      await tester.pumpAndSettle();

      expect(find.textContaining('DELETE /ai/data'), findsNothing);
      expect(find.byKey(const Key('tile_ai_usage_limits')), findsOneWidget);
      expect(find.byKey(const Key('tile_notifications_inbox')), findsOneWidget);
      expect(find.byKey(const Key('tile_personal_settings')), findsOneWidget);
    });
  });
}
