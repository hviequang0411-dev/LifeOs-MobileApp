import 'dart:io';

import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:lifeos_mobile/core/database/app_database.dart';
import 'package:lifeos_mobile/core/providers/app_providers.dart';
import 'package:lifeos_mobile/features/workspace/data/demo_workspace_seeder.dart';
import 'package:lifeos_mobile/features/workspace/data/lifeos_local_repository.dart';
import 'package:lifeos_mobile/main.dart';

Future<void> _loadRealFontsForGolden() async {
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

AppDatabase _buildDeterministicDatabase() {
  final AppDatabase db = AppDatabase.inMemory();
  final LifeOsLocalRepository repo = LifeOsLocalRepository(db: db);
  DemoWorkspaceSeeder.seedIfEmpty(
    repo: repo,
    userId: 'user-local-01',
    nowUtc: DemoWorkspaceSeeder.defaultFixedSeedUtc,
  );
  return db;
}

Widget _buildGoldenHarness({
  required AppDatabase db,
  required ThemeMode themeMode,
  required Size size,
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
      data: MediaQueryData(
        size: size,
        devicePixelRatio: 1.0,
        disableAnimations: true,
        textScaler: TextScaler.noScaling,
      ),
      child: const LifeOsApp(),
    ),
  );
}

void main() {
  setUpAll(() async {
    await _loadRealFontsForGolden();
  });

  group(
      'PWP2 & UPW8 — Golden Tests (matchesGoldenFile: 5 màn hình × Light/Dark × Phone + Tablet với font Roboto tiếng Việt)',
      () {
    final List<(String, String)> tabs = <(String, String)>[
      ('01_my_day', 'Hôm nay'),
      ('02_plan', 'Kế hoạch'),
      ('03_finance', 'Tài chính'),
      ('04_knowledge', 'Tri thức'),
      ('05_overview', 'Tổng quan'),
    ];

    for (final (String slug, String tabLabel) in tabs) {
      testWidgets('Golden Phone Light — $slug ($tabLabel)',
          (WidgetTester tester) async {
        const Size phoneSize = Size(390, 844);
        tester.view.physicalSize = phoneSize;
        tester.view.devicePixelRatio = 1.0;
        addTearDown(tester.view.resetPhysicalSize);
        addTearDown(tester.view.resetDevicePixelRatio);

        final AppDatabase db = _buildDeterministicDatabase();
        addTearDown(db.close);

        await tester.pumpWidget(
          _buildGoldenHarness(
            db: db,
            themeMode: ThemeMode.light,
            size: phoneSize,
          ),
        );
        await tester.pumpAndSettle();

        if (tabLabel != 'Hôm nay') {
          await tester.tap(find.text(tabLabel).last);
          await tester.pumpAndSettle();
        }

        await expectLater(
          find.byType(LifeOsApp),
          matchesGoldenFile('goldens/phone_light_$slug.png'),
        );
      });

      testWidgets('Golden Phone Dark — $slug ($tabLabel)',
          (WidgetTester tester) async {
        const Size phoneSize = Size(390, 844);
        tester.view.physicalSize = phoneSize;
        tester.view.devicePixelRatio = 1.0;
        addTearDown(tester.view.resetPhysicalSize);
        addTearDown(tester.view.resetDevicePixelRatio);

        final AppDatabase db = _buildDeterministicDatabase();
        addTearDown(db.close);

        await tester.pumpWidget(
          _buildGoldenHarness(
            db: db,
            themeMode: ThemeMode.dark,
            size: phoneSize,
          ),
        );
        await tester.pumpAndSettle();

        if (tabLabel != 'Hôm nay') {
          await tester.tap(find.text(tabLabel).last);
          await tester.pumpAndSettle();
        }

        await expectLater(
          find.byType(LifeOsApp),
          matchesGoldenFile('goldens/phone_dark_$slug.png'),
        );
      });
    }

    testWidgets('Golden Tablet Light — 01_my_day (NavigationRail 1024x768)',
        (WidgetTester tester) async {
      const Size tabletSize = Size(1024, 768);
      tester.view.physicalSize = tabletSize;
      tester.view.devicePixelRatio = 1.0;
      addTearDown(tester.view.resetPhysicalSize);
      addTearDown(tester.view.resetDevicePixelRatio);

      final AppDatabase db = _buildDeterministicDatabase();
      addTearDown(db.close);

      await tester.pumpWidget(
        _buildGoldenHarness(
          db: db,
          themeMode: ThemeMode.light,
          size: tabletSize,
        ),
      );
      await tester.pumpAndSettle();

      await expectLater(
        find.byType(LifeOsApp),
        matchesGoldenFile('goldens/tablet_light_01_my_day.png'),
      );
    });
  });
}
