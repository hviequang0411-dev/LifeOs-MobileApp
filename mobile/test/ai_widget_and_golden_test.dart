import 'dart:async';
import 'dart:io';

import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_localizations/flutter_localizations.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:lifeos_mobile/core/network/api_client.dart';
import 'package:lifeos_mobile/core/providers/app_providers.dart';
import 'package:lifeos_mobile/core/theme/app_theme.dart';
import 'package:lifeos_mobile/features/ai/data/ai_api.dart';
import 'package:lifeos_mobile/features/ai/data/ai_models.dart';
import 'package:lifeos_mobile/features/ai/data/ai_providers.dart';
import 'package:lifeos_mobile/features/ai/presentation/ai_assistant_screen.dart';
import 'package:lifeos_mobile/features/ai/presentation/widgets/ai_consent_sheet.dart';
import 'package:lifeos_mobile/features/ai/presentation/widgets/ai_usage_and_limits_sheet.dart';
import 'package:lifeos_mobile/features/workspace/data/demo_workspace_seeder.dart';

class FakeAiApiClient extends AiApiClient {
  FakeAiApiClient()
      : super(LifeOsApiClient(
          baseUrl: 'http://localhost:8080',
          tokenStorage: InMemoryTokenStorage(),
        ));

  AiPreferencesResponse preferences = const AiPreferencesResponse(
    taskCalendar: true,
    habit: true,
    goal: true,
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
  );

  Completer<AiChatResponse>? pendingChatCompleter;

  @override
  Future<AiPreferencesResponse> getPreferences() async => preferences;

  @override
  Future<AiPreferencesResponse> replacePreferences(
      AiPreferencesRequest req) async {
    preferences = AiPreferencesResponse(
      taskCalendar: req.taskCalendar,
      habit: req.habit,
      goal: req.goal,
      notes: req.notes,
      finance: req.finance,
      policyVersion: '2026-11-10',
      providerNoticeVersion: 'openai-api-2026-11-10',
      features: const AiFeatureFlags(
        chat: true,
        insights: true,
        notes: false,
        finance: false,
        rag: false,
        actions: false,
      ),
    );
    return preferences;
  }

  @override
  Future<AiConversationResponse> createConversation({String? title}) async {
    return const AiConversationResponse(
      id: 'conv-test-01',
      title: 'Hội thoại kiểm thử',
      createdAt: '2026-11-10T08:00:00Z',
      updatedAt: '2026-11-10T08:00:00Z',
      expiresAt: '2026-11-10T08:15:00Z',
    );
  }

  @override
  Future<AiConversationDetailResponse> getConversation(String id,
      {int limit = 50, String? cursor}) async {
    return const AiConversationDetailResponse(
      conversation: AiConversationResponse(
        id: 'conv-test-01',
        title: 'Hội thoại kiểm thử',
        createdAt: '2026-11-10T08:00:00Z',
        updatedAt: '2026-11-10T08:00:00Z',
        expiresAt: '2026-11-10T08:15:00Z',
      ),
      messages: AiMessagePage(
        items: <AiMessageResponse>[
          AiMessageResponse(
            id: 'msg-01',
            role: AiMessageRole.user,
            content: 'Hôm nay tôi nên ưu tiên việc nào?',
            mode: AiGenerationMode.chat,
            contextAvailable: false,
            createdAt: '2026-11-10T08:00:00Z',
          ),
          AiMessageResponse(
            id: 'msg-02',
            role: AiMessageRole.assistant,
            content: 'Ưu tiên hoàn thiện báo cáo trước cuộc họp lúc 14:00.',
            mode: AiGenerationMode.chat,
            contextAvailable: true,
            createdAt: '2026-11-10T08:00:03Z',
          ),
        ],
        hasMore: false,
      ),
    );
  }

  @override
  Future<AiChatResponse> sendMessage(
      String conversationId, AiChatRequest request) async {
    if (pendingChatCompleter != null) {
      return pendingChatCompleter!.future;
    }
    return AiChatResponse(
      conversationId: conversationId,
      userMessage: AiMessageResponse(
        id: 'user_msg_resp',
        role: AiMessageRole.user,
        content: request.message,
        mode: AiGenerationMode.chat,
        contextAvailable: false,
        createdAt: '2026-11-10T08:00:05Z',
      ),
      assistantMessage: const AiMessageResponse(
        id: 'asst_msg_resp',
        role: AiMessageRole.assistant,
        content: 'Tôi đã tiếp nhận yêu cầu và đối chiếu với dữ liệu đã cấp quyền.',
        mode: AiGenerationMode.chat,
        contextAvailable: true,
        createdAt: '2026-11-10T08:00:08Z',
      ),
    );
  }

  @override
  Future<AiLimitsResponse> getLimits() async {
    return const AiLimitsResponse(
      enabled: true,
      budgetAvailable: true,
      daily: AiQuotaWindow(
        requestLimit: 20,
        requestsUsed: 12,
        inputTokenLimit: 100000,
        inputTokensUsed: 18400,
        outputTokenLimit: 10000,
        outputTokensUsed: 3200,
        resetsAt: '2026-11-10T24:00:00Z',
      ),
      monthly: AiQuotaWindow(
        requestLimit: 300,
        requestsUsed: 85,
        inputTokenLimit: 1000000,
        inputTokensUsed: 154000,
        outputTokenLimit: 100000,
        outputTokensUsed: 28000,
        resetsAt: '2026-11-30T24:00:00Z',
      ),
      rateLimitPerMinute: 5,
      maxConcurrentPerUser: 2,
    );
  }

  @override
  Future<AiUsageResponse> getUsage(
      {required String from, required String to}) async {
    return const AiUsageResponse(
      from: '2026-11-10',
      to: '2026-11-10',
      days: <AiUsageDay>[
        AiUsageDay(
          date: '2026-11-10',
          requests: 12,
          inputTokens: 18400,
          outputTokens: 3200,
          estimatedCostMicrousd: 68800,
        ),
      ],
      totals: AiUsageTotals(
        requests: 12,
        inputTokens: 18400,
        outputTokens: 3200,
        estimatedCostMicrousd: 68800,
      ),
    );
  }

  @override
  Future<void> deleteAllData() async {
    preferences = AiPreferencesResponse.initialDefault();
  }
}

Future<void> _loadRealFonts() async {
  Future<ByteData> readFontBytes(String relativePath) async {
    final File file = File(relativePath);
    final Uint8List bytes = await file.readAsBytes();
    return ByteData.view(bytes.buffer);
  }

  final List<String> robotoAssets = <String>[
    'assets/fonts/Roboto-Regular.ttf',
    'assets/fonts/Roboto-Italic.ttf',
    'assets/fonts/Roboto-Medium.ttf',
    'assets/fonts/Roboto-Bold.ttf',
  ];

  for (final String family in const <String>['LifeOsSans', 'Roboto']) {
    final FontLoader loader = FontLoader(family);
    for (final String assetPath in robotoAssets) {
      loader.addFont(readFontBytes(assetPath));
    }
    await loader.load();
  }

  final FontLoader iconsLoader = FontLoader('MaterialIcons')
    ..addFont(readFontBytes('assets/fonts/MaterialIcons-Regular.otf'));
  await iconsLoader.load();
}

Widget _wrapAiTestSurface({
  required ThemeMode themeMode,
  required Widget home,
  required FakeAiApiClient fakeApi,
}) {
  return ProviderScope(
    overrides: <Override>[
      aiApiClientProvider.overrideWithValue(fakeApi),
      themeModeProvider.overrideWith((Ref ref) => themeMode),
      referenceUtcProvider.overrideWithValue(
        DemoWorkspaceSeeder.defaultFixedSeedUtc,
      ),
    ],
    child: MediaQuery(
      data: const MediaQueryData(
        size: Size(390, 844),
        devicePixelRatio: 1.0,
        disableAnimations: true,
        textScaler: TextScaler.noScaling,
      ),
      child: MaterialApp(
        debugShowCheckedModeBanner: false,
        theme: LifeOsTheme.light(),
        darkTheme: LifeOsTheme.dark(),
        themeMode: themeMode,
        locale: const Locale('vi', 'VN'),
        supportedLocales: const <Locale>[
          Locale('vi', 'VN'),
          Locale('en', 'US'),
        ],
        localizationsDelegates: const <LocalizationsDelegate<dynamic>>[
          GlobalMaterialLocalizations.delegate,
          GlobalWidgetsLocalizations.delegate,
          GlobalCupertinoLocalizations.delegate,
        ],
        home: Scaffold(body: home),
      ),
    ),
  );
}

void main() {
  setUpAll(() async {
    await _loadRealFonts();
  });

  group('AWP-1: Consent Sheet & Consent Gating Widget Tests', () {
    testWidgets('Toggles phản ánh đúng trạng thái và gửi cập nhật lên server',
        (WidgetTester tester) async {
      final FakeAiApiClient fakeApi = FakeAiApiClient();
      await tester.pumpWidget(_wrapAiTestSurface(
        themeMode: ThemeMode.light,
        home: const AiConsentSheet(),
        fakeApi: fakeApi,
      ));
      await tester.pumpAndSettle();

      // Kiểm tra các công tắc
      expect(find.byKey(const Key('switch_consent_task')), findsOneWidget);
      expect(find.byKey(const Key('switch_consent_habit')), findsOneWidget);
      expect(find.byKey(const Key('switch_consent_goal')), findsOneWidget);
      expect(find.byKey(const Key('switch_consent_notes')), findsOneWidget);

      await tester.scrollUntilVisible(
        find.byKey(const Key('switch_consent_finance')),
        200,
        scrollable: find.byType(Scrollable).first,
      );
      expect(find.byKey(const Key('switch_consent_finance')), findsOneWidget);

      // Bật công tắc Notes
      await tester.tap(find.byKey(const Key('switch_consent_notes')));
      await tester.pumpAndSettle();

      // Bấm nút lưu
      await tester.tap(find.byKey(const Key('btn_save_ai_consent')));
      await tester.pumpAndSettle();

      expect(fakeApi.preferences.notes, isTrue);
    });
  });

  group('AWP-2: Concurrency Gate & Waiting State Widget Tests', () {
    testWidgets('Chặn gửi chồng: khi đang chờ thì ô nhập và nút gửi bị vô hiệu hóa',
        (WidgetTester tester) async {
      final FakeAiApiClient fakeApi = FakeAiApiClient();
      fakeApi.pendingChatCompleter = Completer<AiChatResponse>(); // Treo request để kiểm tra in-flight

      await tester.pumpWidget(_wrapAiTestSurface(
        themeMode: ThemeMode.light,
        home: const AiAssistantScreen(),
        fakeApi: fakeApi,
      ));
      await tester.pumpAndSettle();

      // Nhập tin nhắn
      await tester.enterText(
        find.byKey(const Key('input_ai_chat_message')),
        'Lập kế hoạch hôm nay giúp tôi',
      );
      await tester.pumpAndSettle();

      // Bấm gửi
      await tester.tap(find.byKey(const Key('btn_send_ai_message')));
      await tester.pump(); // Không dùng pumpAndSettle vì completer chưa xong

      // Kiểm tra trạng thái chờ
      expect(find.textContaining('Đang xử lý (0 / 30s)...'), findsOneWidget);

      // Thử bấm gửi lần nữa -> Nút gửi bị vô hiệu hóa
      final IconButton sendBtn =
          tester.widget<IconButton>(find.byKey(const Key('btn_send_ai_message')));
      expect(sendBtn.onPressed, isNull, reason: 'Chặn gửi chồng (Concurrency gate)');

      // Giải phóng completer
      fakeApi.pendingChatCompleter!.complete(const AiChatResponse(
        conversationId: 'conv-test-01',
        userMessage: AiMessageResponse(
          id: 'u-1',
          role: AiMessageRole.user,
          content: 'Lập kế hoạch hôm nay giúp tôi',
          mode: AiGenerationMode.chat,
          contextAvailable: false,
          createdAt: '2026-11-10T08:00:00Z',
        ),
        assistantMessage: AiMessageResponse(
          id: 'a-1',
          role: AiMessageRole.assistant,
          content: 'Đây là kế hoạch gợi ý cho bạn.',
          mode: AiGenerationMode.chat,
          contextAvailable: true,
          createdAt: '2026-11-10T08:00:02Z',
        ),
      ));
      await tester.pumpAndSettle();

      expect(find.text('Đây là kế hoạch gợi ý cho bạn.'), findsOneWidget);
    });

    testWidgets('Quá thời hạn 30s hiển thị bong bóng lỗi kèm nút Thử lại',
        (WidgetTester tester) async {
      final FakeAiApiClient fakeApi = FakeAiApiClient();
      fakeApi.pendingChatCompleter = Completer<AiChatResponse>();

      await tester.pumpWidget(_wrapAiTestSurface(
        themeMode: ThemeMode.light,
        home: const AiAssistantScreen(),
        fakeApi: fakeApi,
      ));
      await tester.pumpAndSettle();

      await tester.enterText(
        find.byKey(const Key('input_ai_chat_message')),
        'Tin nhắn sẽ bị timeout',
      );
      await tester.pumpAndSettle();
      await tester.tap(find.byKey(const Key('btn_send_ai_message')));
      await tester.pump();

      // Giả lập đồng hồ nhảy 31 giây
      await tester.pump(const Duration(seconds: 31));
      await tester.pumpAndSettle();

      expect(find.textContaining('quá thời gian chờ (30 giây)'), findsOneWidget);
      expect(find.byKey(const Key('btn_ai_error_action')), findsOneWidget);
    });
  });

  group('AWP-5: Dangerous Delete AI Data Dialog Tests', () {
    testWidgets('Yêu cầu nhập đúng chữ XOA DU LIEU AI mới kích hoạt nút xóa',
        (WidgetTester tester) async {
      tester.view.physicalSize = const Size(1080, 2400);
      tester.view.devicePixelRatio = 2.0;
      addTearDown(() {
        tester.view.resetPhysicalSize();
        tester.view.resetDevicePixelRatio();
      });

      final FakeAiApiClient fakeApi = FakeAiApiClient();
      await tester.pumpWidget(_wrapAiTestSurface(
        themeMode: ThemeMode.light,
        home: const AiUsageAndLimitsSheet(),
        fakeApi: fakeApi,
      ));
      await tester.pumpAndSettle();

      // Mở hộp thoại xóa
      await tester.ensureVisible(find.byKey(const Key('btn_delete_ai_data')));
      await tester.pumpAndSettle();
      await tester.tap(find.byKey(const Key('btn_delete_ai_data')));
      await tester.pumpAndSettle();

      // Nút xóa ban đầu bị vô hiệu hóa
      final FilledButton delBtnInit = tester
          .widget<FilledButton>(find.byKey(const Key('btn_confirm_delete_ai_data')));
      expect(delBtnInit.onPressed, isNull);

      // Nhập sai
      await tester.enterText(
        find.byKey(const Key('input_confirm_delete_ai_data')),
        'XOA',
      );
      await tester.pumpAndSettle();
      final FilledButton delBtnWrong = tester
          .widget<FilledButton>(find.byKey(const Key('btn_confirm_delete_ai_data')));
      expect(delBtnWrong.onPressed, isNull);

      // Nhập đúng
      await tester.enterText(
        find.byKey(const Key('input_confirm_delete_ai_data')),
        'XOA DU LIEU AI',
      );
      await tester.pumpAndSettle();
      final FilledButton delBtnCorrect = tester
          .widget<FilledButton>(find.byKey(const Key('btn_confirm_delete_ai_data')));
      expect(delBtnCorrect.onPressed, isNotNull);

      // Bấm xác nhận xóa
      await tester.tap(find.byKey(const Key('btn_confirm_delete_ai_data')));
      await tester.pumpAndSettle();

      expect(fakeApi.preferences.allowedDomainsCount, equals(0));
    });
  });

  group('AWP-6: Golden Tests (Consent, Chat Waiting, Usage Limits Light/Dark)', () {
    void copyGoldenToScreenshots(String goldenFilename, String targetFilename) {
      final File src = File('test/goldens/$goldenFilename');
      if (src.existsSync()) {
        final File dest = File('../docs/review/mobile_screenshots/ai_chat/$targetFilename');
        dest.parent.createSync(recursive: true);
        src.copySync(dest.path);
      }
    }

    testWidgets('Golden 1 — AI Consent Sheet (Light)', (WidgetTester tester) async {
      const Size phoneSize = Size(390, 844);
      tester.view.physicalSize = phoneSize;
      tester.view.devicePixelRatio = 1.0;
      addTearDown(tester.view.resetPhysicalSize);
      addTearDown(tester.view.resetDevicePixelRatio);

      final FakeAiApiClient fakeApi = FakeAiApiClient();
      await tester.pumpWidget(_wrapAiTestSurface(
        themeMode: ThemeMode.light,
        home: const AiConsentSheet(),
        fakeApi: fakeApi,
      ));
      await tester.pumpAndSettle();

      await expectLater(
        find.byType(MaterialApp),
        matchesGoldenFile('goldens/ai_consent_light.png'),
      );
      copyGoldenToScreenshots('ai_consent_light.png', 'ai_consent_light.png');
    });

    testWidgets('Golden 2 — AI Consent Sheet (Dark)', (WidgetTester tester) async {
      const Size phoneSize = Size(390, 844);
      tester.view.physicalSize = phoneSize;
      tester.view.devicePixelRatio = 1.0;
      addTearDown(tester.view.resetPhysicalSize);
      addTearDown(tester.view.resetDevicePixelRatio);

      final FakeAiApiClient fakeApi = FakeAiApiClient();
      await tester.pumpWidget(_wrapAiTestSurface(
        themeMode: ThemeMode.dark,
        home: const AiConsentSheet(),
        fakeApi: fakeApi,
      ));
      await tester.pumpAndSettle();

      await expectLater(
        find.byType(MaterialApp),
        matchesGoldenFile('goldens/ai_consent_dark.png'),
      );
      copyGoldenToScreenshots('ai_consent_dark.png', 'ai_consent_dark.png');
    });

    testWidgets('Golden 3 — AI Chat & Waiting State (Light)', (WidgetTester tester) async {
      const Size phoneSize = Size(390, 844);
      tester.view.physicalSize = phoneSize;
      tester.view.devicePixelRatio = 1.0;
      addTearDown(tester.view.resetPhysicalSize);
      addTearDown(tester.view.resetDevicePixelRatio);

      final FakeAiApiClient fakeApi = FakeAiApiClient();
      fakeApi.pendingChatCompleter = Completer<AiChatResponse>();

      await tester.pumpWidget(_wrapAiTestSurface(
        themeMode: ThemeMode.light,
        home: const AiAssistantScreen(),
        fakeApi: fakeApi,
      ));
      await tester.pumpAndSettle();

      await tester.enterText(
        find.byKey(const Key('input_ai_chat_message')),
        'Lập kế hoạch tuần giúp tôi',
      );
      await tester.pumpAndSettle();
      await tester.tap(find.byKey(const Key('btn_send_ai_message')));
      await tester.pump();
      await tester.pump(const Duration(seconds: 4));

      await expectLater(
        find.byType(MaterialApp),
        matchesGoldenFile('goldens/ai_chat_waiting_light.png'),
      );
      copyGoldenToScreenshots('ai_chat_waiting_light.png', 'ai_chat_waiting_light.png');
    });

    testWidgets('Golden 4 — AI Chat & Waiting State (Dark)', (WidgetTester tester) async {
      const Size phoneSize = Size(390, 844);
      tester.view.physicalSize = phoneSize;
      tester.view.devicePixelRatio = 1.0;
      addTearDown(tester.view.resetPhysicalSize);
      addTearDown(tester.view.resetDevicePixelRatio);

      final FakeAiApiClient fakeApi = FakeAiApiClient();
      fakeApi.pendingChatCompleter = Completer<AiChatResponse>();

      await tester.pumpWidget(_wrapAiTestSurface(
        themeMode: ThemeMode.dark,
        home: const AiAssistantScreen(),
        fakeApi: fakeApi,
      ));
      await tester.pumpAndSettle();

      await tester.enterText(
        find.byKey(const Key('input_ai_chat_message')),
        'Lập kế hoạch tuần giúp tôi',
      );
      await tester.pumpAndSettle();
      await tester.tap(find.byKey(const Key('btn_send_ai_message')));
      await tester.pump();
      await tester.pump(const Duration(seconds: 4));

      await expectLater(
        find.byType(MaterialApp),
        matchesGoldenFile('goldens/ai_chat_waiting_dark.png'),
      );
      copyGoldenToScreenshots('ai_chat_waiting_dark.png', 'ai_chat_waiting_dark.png');
    });

    testWidgets('Golden 5 — AI Usage & Limits (Light)', (WidgetTester tester) async {
      const Size phoneSize = Size(390, 844);
      tester.view.physicalSize = phoneSize;
      tester.view.devicePixelRatio = 1.0;
      addTearDown(tester.view.resetPhysicalSize);
      addTearDown(tester.view.resetDevicePixelRatio);

      final FakeAiApiClient fakeApi = FakeAiApiClient();
      await tester.pumpWidget(_wrapAiTestSurface(
        themeMode: ThemeMode.light,
        home: const AiUsageAndLimitsSheet(),
        fakeApi: fakeApi,
      ));
      await tester.pumpAndSettle();

      await expectLater(
        find.byType(MaterialApp),
        matchesGoldenFile('goldens/ai_usage_limits_light.png'),
      );
      copyGoldenToScreenshots('ai_usage_limits_light.png', 'ai_usage_limits_light.png');
    });

    testWidgets('Golden 6 — AI Usage & Limits (Dark)', (WidgetTester tester) async {
      const Size phoneSize = Size(390, 844);
      tester.view.physicalSize = phoneSize;
      tester.view.devicePixelRatio = 1.0;
      addTearDown(tester.view.resetPhysicalSize);
      addTearDown(tester.view.resetDevicePixelRatio);

      final FakeAiApiClient fakeApi = FakeAiApiClient();
      await tester.pumpWidget(_wrapAiTestSurface(
        themeMode: ThemeMode.dark,
        home: const AiUsageAndLimitsSheet(),
        fakeApi: fakeApi,
      ));
      await tester.pumpAndSettle();

      await expectLater(
        find.byType(MaterialApp),
        matchesGoldenFile('goldens/ai_usage_limits_dark.png'),
      );
      copyGoldenToScreenshots('ai_usage_limits_dark.png', 'ai_usage_limits_dark.png');
    });
  });
}

