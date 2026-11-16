import 'dart:io';

import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_localizations/flutter_localizations.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:lifeos_mobile/core/database/app_database.dart';
import 'package:lifeos_mobile/core/money/decimal10k.dart';
import 'package:lifeos_mobile/core/providers/app_providers.dart';
import 'package:lifeos_mobile/core/sync/entity_types.dart';
import 'package:lifeos_mobile/core/theme/app_theme.dart';
import 'package:lifeos_mobile/features/auth/presentation/sign_in_welcome_view.dart';
import 'package:lifeos_mobile/features/finance/presentation/widgets/recurring_transaction_sheet.dart';
import 'package:lifeos_mobile/features/knowledge/presentation/widgets/goal_milestones_sheet.dart';
import 'package:lifeos_mobile/features/knowledge/presentation/widgets/note_editor_sheet.dart';
import 'package:lifeos_mobile/features/tasks/presentation/widgets/routine_player_sheet.dart';
import 'package:lifeos_mobile/features/workspace/data/demo_workspace_seeder.dart';
import 'package:lifeos_mobile/features/workspace/data/lifeos_local_repository.dart';

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

Widget _wrapGoldenSurface({
  required AppDatabase db,
  required ThemeMode themeMode,
  required Widget home,
}) {
  return ProviderScope(
    overrides: <Override>[
      appDatabaseProvider.overrideWithValue(db),
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
        home: home,
      ),
    ),
  );
}

void main() {
  setUpAll(() async {
    await _loadRealFonts();
  });

  group('MWP-E — Golden Tests đợt 2 (Login, Routine Player, Recurring Tx Light/Dark)', () {
    for (final ThemeMode mode in const <ThemeMode>[
      ThemeMode.light,
      ThemeMode.dark,
    ]) {
      final String suffix = mode == ThemeMode.light ? 'light' : 'dark';

      testWidgets('Golden Daily Use Login ($suffix)',
          (WidgetTester tester) async {
        tester.view.physicalSize = const Size(390, 844);
        tester.view.devicePixelRatio = 1.0;
        addTearDown(tester.view.resetPhysicalSize);
        addTearDown(tester.view.resetDevicePixelRatio);

        final AppDatabase db = AppDatabase.inMemory();
        addTearDown(db.close);

        await tester.pumpWidget(
          _wrapGoldenSurface(
            db: db,
            themeMode: mode,
            home: const SignInWelcomeView(),
          ),
        );
        await tester.pumpAndSettle();

        await expectLater(
          find.byType(MaterialApp),
          matchesGoldenFile('goldens/daily_use_login_$suffix.png'),
        );
      });

      testWidgets('Golden Daily Use Routine Player ($suffix)',
          (WidgetTester tester) async {
        tester.view.physicalSize = const Size(390, 844);
        tester.view.devicePixelRatio = 1.0;
        addTearDown(tester.view.resetPhysicalSize);
        addTearDown(tester.view.resetDevicePixelRatio);

        final AppDatabase db = AppDatabase.inMemory();
        addTearDown(db.close);
        final LifeOsLocalRepository repo = LifeOsLocalRepository(db: db);
        final String routineId = repo.tasks.createRoutine(
          userId: 'user-local-01',
          name: 'Nghi thức buổi sáng',
          reminderTime: '06:30',
          stepTitles: const <String>[
            'Uống 1 ly nước ấm',
            'Thiền thở 5 phút',
            'Viết 3 mục tiêu trong ngày',
          ],
          stepDurationsMinutes: const <int>[2, 5, 5],
          nowUtc: DemoWorkspaceSeeder.defaultFixedSeedUtc,
        );
        final LocalEntityRow routine = db.getEntity(
          userId: 'user-local-01',
          entityType: SyncEntityType.routine,
          entityId: routineId,
        )!;

        await tester.pumpWidget(
          _wrapGoldenSurface(
            db: db,
            themeMode: mode,
            home: Scaffold(
              body: Align(
                alignment: Alignment.bottomCenter,
                child: RoutinePlayerSheet(routine: routine),
              ),
            ),
          ),
        );
        await tester.pumpAndSettle();

        await expectLater(
          find.byType(MaterialApp),
          matchesGoldenFile('goldens/daily_use_routine_player_$suffix.png'),
        );
      });

      testWidgets('Golden Daily Use Recurring Transaction ($suffix)',
          (WidgetTester tester) async {
        tester.view.physicalSize = const Size(390, 844);
        tester.view.devicePixelRatio = 1.0;
        addTearDown(tester.view.resetPhysicalSize);
        addTearDown(tester.view.resetDevicePixelRatio);

        final AppDatabase db = AppDatabase.inMemory();
        addTearDown(db.close);
        final LifeOsLocalRepository repo = LifeOsLocalRepository(db: db);
        repo.createFinanceAccount(
          userId: 'user-local-01',
          name: 'Ví chính',
          openingBalance: Decimal10k.fromWhole(15000000),
          nowUtc: DemoWorkspaceSeeder.defaultFixedSeedUtc,
        );

        await tester.pumpWidget(
          _wrapGoldenSurface(
            db: db,
            themeMode: mode,
            home: const Scaffold(
              body: Align(
                alignment: Alignment.bottomCenter,
                child: RecurringTransactionSheet(),
              ),
            ),
          ),
        );
        await tester.pumpAndSettle();

        await expectLater(
          find.byType(MaterialApp),
          matchesGoldenFile('goldens/daily_use_recurring_tx_$suffix.png'),
        );
      });

      testWidgets('Golden Daily Use Note Editor ($suffix)',
          (WidgetTester tester) async {
        tester.view.physicalSize = const Size(390, 844);
        tester.view.devicePixelRatio = 1.0;
        addTearDown(tester.view.resetPhysicalSize);
        addTearDown(tester.view.resetDevicePixelRatio);

        final AppDatabase db = AppDatabase.inMemory();
        addTearDown(db.close);
        final LifeOsLocalRepository repo = LifeOsLocalRepository(db: db);
        const String userId = 'user-local-01';

        final String noteId = repo.saveNote(
          userId: userId,
          title: 'Chiến lược Kiến trúc LifeOS',
          content: 'Kiến trúc Offline-First với Drift/SQLite và đồng bộ Outbox đảm bảo tính toàn vẹn dữ liệu.',
          pinned: true,
          nowUtc: DemoWorkspaceSeeder.defaultFixedSeedUtc,
        );
        final LocalEntityRow note = db.getEntity(
          userId: userId,
          entityType: SyncEntityType.note,
          entityId: noteId,
        )!;

        await tester.pumpWidget(
          _wrapGoldenSurface(
            db: db,
            themeMode: mode,
            home: Scaffold(
              body: Align(
                alignment: Alignment.bottomCenter,
                child: NoteEditorSheet(note: note),
              ),
            ),
          ),
        );
        await tester.pumpAndSettle();

        await expectLater(
          find.byType(MaterialApp),
          matchesGoldenFile('goldens/daily_use_note_editor_$suffix.png'),
        );
      });

      testWidgets('Golden Daily Use Goal Editor ($suffix)',
          (WidgetTester tester) async {
        tester.view.physicalSize = const Size(390, 844);
        tester.view.devicePixelRatio = 1.0;
        addTearDown(tester.view.resetPhysicalSize);
        addTearDown(tester.view.resetDevicePixelRatio);

        final AppDatabase db = AppDatabase.inMemory();
        addTearDown(db.close);
        final LifeOsLocalRepository repo = LifeOsLocalRepository(db: db);
        const String userId = 'user-local-01';

        final String goalId = repo.createGoal(
          userId: userId,
          title: 'Chinh phục tiếng Nhật N2',
          description: 'Học tiếng Nhật để đọc tài liệu chuyên ngành và giao tiếp hàng ngày.',
          startDateIso: '2026-01-01',
          targetDateIso: '2026-06-30',
          milestoneDrafts: const <GoalMilestoneDraft>[
            GoalMilestoneDraft(title: 'Nắm vững 500 chữ Hán N2', dueOnIso: '2026-03-01'),
            GoalMilestoneDraft(title: 'Luyện 10 đề thi Nghe hiểu Choukai', dueOnIso: '2026-05-15'),
            GoalMilestoneDraft(title: 'Thi thử đạt trên 120/180 điểm', dueOnIso: '2026-06-15'),
          ],
          nowUtc: DemoWorkspaceSeeder.defaultFixedSeedUtc,
        );
        final LocalEntityRow goal = db.getEntity(
          userId: userId,
          entityType: SyncEntityType.goal,
          entityId: goalId,
        )!;

        await tester.pumpWidget(
          _wrapGoldenSurface(
            db: db,
            themeMode: mode,
            home: Scaffold(
              body: Align(
                alignment: Alignment.bottomCenter,
                child: GoalEditorSheet(goal: goal),
              ),
            ),
          ),
        );
        await tester.pumpAndSettle();

        await expectLater(
          find.byType(MaterialApp),
          matchesGoldenFile('goldens/daily_use_goal_editor_$suffix.png'),
        );
      });
    }
  });
}

