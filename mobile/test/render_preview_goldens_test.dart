import 'dart:io';
import 'dart:ui' as ui;

import 'package:flutter/material.dart';
import 'package:flutter/rendering.dart';
import 'package:flutter/services.dart';
import 'package:flutter_localizations/flutter_localizations.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:lifeos_mobile/core/database/app_database.dart';
import 'package:lifeos_mobile/core/providers/app_providers.dart';
import 'package:lifeos_mobile/core/theme/app_theme.dart';
import 'package:lifeos_mobile/core/theme/app_tokens.dart';
import 'package:lifeos_mobile/features/workspace/data/demo_workspace_seeder.dart';
import 'package:lifeos_mobile/features/workspace/data/lifeos_local_repository.dart';
import 'package:lifeos_mobile/features/workspace/presentation/lifeos_shell_screen.dart';

Future<void> _loadFonts() async {
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

// Direction A (Botanical Premium v2)
const LifeOsTokens tokensALight = LifeOsTokens.light;
const LifeOsTokens tokensADark = LifeOsTokens.dark;

// Direction B (Midnight Aurora)
const LifeOsTokens tokensBLight = LifeOsTokens(
  bgPrimary: Color(0xFFF4F6FB),
  bgSurface: Color(0xFFFFFFFF),
  bgElevated: Color(0xFFEBF0FA),
  textPrimary: Color(0xFF0B132B),
  textSecondary: Color(0xFF334155),
  textMuted: Color(0xFF475569),
  accentPrimary: Color(0xFF0369A1),
  accentSecondary: Color(0xFFBE123C),
  borderSubtle: Color(0xFFE2E8F0),
  success: Color(0xFF059669),
  warning: Color(0xFFD97706),
  danger: Color(0xFFBE123C),
  info: Color(0xFF0284C7),
  financeIncome: Color(0xFF059669),
  financeExpense: Color(0xFFBE123C),
  financeTransfer: Color(0xFF0284C7),
  domainTask: Color(0xFF0369A1),
  domainHabit: Color(0xFF059669),
  domainHabitButtonBg: Color(0xFF0369A1),
  domainHabitButtonFg: Color(0xFFFFFFFF),
  domainFinance: Color(0xFFBE123C),
  domainCalendar: Color(0xFF0284C7),
  domainKnowledge: Color(0xFF0D9488),
  domainGoal: Color(0xFF7C3AED),
  priorityLowFg: Color(0xFF475569),
  priorityMediumFg: Color(0xFFB45309),
  priorityHighFg: Color(0xFFBE123C),
  riskOnTrackFg: Color(0xFF059669),
  riskAtRiskFg: Color(0xFFB45309),
  riskOverdueFg: Color(0xFFBE123C),
);

const LifeOsTokens tokensBDark = LifeOsTokens(
  bgPrimary: Color(0xFF0A0E1A),
  bgSurface: Color(0xFF131B2E),
  bgElevated: Color(0xFF1E293B),
  textPrimary: Color(0xFFF8FAFC),
  textSecondary: Color(0xFF94A3B8),
  textMuted: Color(0xFF7E8F87),
  accentPrimary: Color(0xFF38BDF8),
  accentSecondary: Color(0xFFFB7185),
  borderSubtle: Color(0xFF24334D),
  success: Color(0xFF34D399),
  warning: Color(0xFFFBBF24),
  danger: Color(0xFFFB7185),
  info: Color(0xFF38BDF8),
  financeIncome: Color(0xFF34D399),
  financeExpense: Color(0xFFFB7185),
  financeTransfer: Color(0xFF38BDF8),
  domainTask: Color(0xFF38BDF8),
  domainHabit: Color(0xFF34D399),
  domainHabitButtonBg: Color(0xFF38BDF8),
  domainHabitButtonFg: Color(0xFF0A0E1A),
  domainFinance: Color(0xFFFB7185),
  domainCalendar: Color(0xFF818CF8),
  domainKnowledge: Color(0xFF2DD4BF),
  domainGoal: Color(0xFFA78BFA),
  priorityLowFg: Color(0xFF94A3B8),
  priorityMediumFg: Color(0xFFFBBF24),
  priorityHighFg: Color(0xFFFB7185),
  riskOnTrackFg: Color(0xFF34D399),
  riskAtRiskFg: Color(0xFFFBBF24),
  riskOverdueFg: Color(0xFFFB7185),
);

// Direction C (Ivory & Ink)
const LifeOsTokens tokensCLight = LifeOsTokens(
  bgPrimary: Color(0xFFFAF9F6),
  bgSurface: Color(0xFFFFFFFF),
  bgElevated: Color(0xFFF2F0EB),
  textPrimary: Color(0xFF111111),
  textSecondary: Color(0xFF444444),
  textMuted: Color(0xFF555555),
  accentPrimary: Color(0xFF0F5132),
  accentSecondary: Color(0xFF854D0E),
  borderSubtle: Color(0xFFE5E5E5),
  success: Color(0xFF0F5132),
  warning: Color(0xFF854D0E),
  danger: Color(0xFF991B1B),
  info: Color(0xFF1E3A8A),
  financeIncome: Color(0xFF0F5132),
  financeExpense: Color(0xFF991B1B),
  financeTransfer: Color(0xFF1E3A8A),
  domainTask: Color(0xFF0F5132),
  domainHabit: Color(0xFF15803D),
  domainHabitButtonBg: Color(0xFF854D0E),
  domainHabitButtonFg: Color(0xFFFFFFFF),
  domainFinance: Color(0xFF991B1B),
  domainCalendar: Color(0xFF1E3A8A),
  domainKnowledge: Color(0xFF0F766E),
  domainGoal: Color(0xFF6B21A8),
  priorityLowFg: Color(0xFF555555),
  priorityMediumFg: Color(0xFF854D0E),
  priorityHighFg: Color(0xFF991B1B),
  riskOnTrackFg: Color(0xFF0F5132),
  riskAtRiskFg: Color(0xFF854D0E),
  riskOverdueFg: Color(0xFF991B1B),
);

const LifeOsTokens tokensCDark = LifeOsTokens(
  bgPrimary: Color(0xFF121212),
  bgSurface: Color(0xFF1E1E1E),
  bgElevated: Color(0xFF282828),
  textPrimary: Color(0xFFF5F5F5),
  textSecondary: Color(0xFFA3A3A3),
  textMuted: Color(0xFF737373),
  accentPrimary: Color(0xFF4EAE7C),
  accentSecondary: Color(0xFFFBBF24),
  borderSubtle: Color(0xFF333333),
  success: Color(0xFF4EAE7C),
  warning: Color(0xFFFBBF24),
  danger: Color(0xFFF87171),
  info: Color(0xFF60A5FA),
  financeIncome: Color(0xFF4EAE7C),
  financeExpense: Color(0xFFF87171),
  financeTransfer: Color(0xFF60A5FA),
  domainTask: Color(0xFF4EAE7C),
  domainHabit: Color(0xFF34D399),
  domainHabitButtonBg: Color(0xFFFBBF24),
  domainHabitButtonFg: Color(0xFF121212),
  domainFinance: Color(0xFFF87171),
  domainCalendar: Color(0xFF60A5FA),
  domainKnowledge: Color(0xFF2DD4BF),
  domainGoal: Color(0xFFC084FC),
  priorityLowFg: Color(0xFFA3A3A3),
  priorityMediumFg: Color(0xFFFBBF24),
  priorityHighFg: Color(0xFFF87171),
  riskOnTrackFg: Color(0xFF4EAE7C),
  riskAtRiskFg: Color(0xFFFBBF24),
  riskOverdueFg: Color(0xFFF87171),
);

void main() {
  setUpAll(() async {
    await _loadFonts();
    final Directory dir = Directory('../docs/review/mobile_screenshots/ui_upgrade/preview');
    if (!dir.existsSync()) {
      dir.createSync(recursive: true);
    }
  });

  final List<(String, LifeOsTokens, LifeOsTokens)> directions = <(String, LifeOsTokens, LifeOsTokens)>[
    ('dir_a', tokensALight, tokensADark),
    ('dir_b', tokensBLight, tokensBDark),
    ('dir_c', tokensCLight, tokensCDark),
  ];

  final List<(String, String)> screens = <(String, String)>[
    ('my_day', 'Hôm nay'),
    ('finance', 'Tài chính'),
  ];

  for (final (String dirKey, LifeOsTokens tLight, LifeOsTokens tDark) in directions) {
    for (final (String screenKey, String tabLabel) in screens) {
      for (final String mode in <String>['light', 'dark']) {
        final bool isDark = mode == 'dark';
        final LifeOsTokens tokens = isDark ? tDark : tLight;
        final Brightness brightness = isDark ? Brightness.dark : Brightness.light;
        final ThemeData theme = LifeOsTheme.buildWithTokens(brightness: brightness, tokens: tokens);

        testWidgets('Render Preview: $dirKey - $mode - $screenKey', (WidgetTester tester) async {
          const Size phoneSize = Size(390, 844);
          tester.view.physicalSize = phoneSize;
          tester.view.devicePixelRatio = 1.0;
          addTearDown(tester.view.resetPhysicalSize);
          addTearDown(tester.view.resetDevicePixelRatio);

          final AppDatabase db = _buildDeterministicDatabase();
          final GlobalKey repaintKey = GlobalKey();

          await tester.pumpWidget(
            ProviderScope(
              overrides: <Override>[
                appDatabaseProvider.overrideWithValue(db),
                themeModeProvider.overrideWith((Ref ref) => isDark ? ThemeMode.dark : ThemeMode.light),
                referenceUtcProvider.overrideWithValue(DemoWorkspaceSeeder.defaultFixedSeedUtc),
              ],
              child: MediaQuery(
                data: const MediaQueryData(
                  size: phoneSize,
                  devicePixelRatio: 1.0,
                  disableAnimations: true,
                  textScaler: TextScaler.noScaling,
                ),
                child: MaterialApp(
                  debugShowCheckedModeBanner: false,
                  theme: theme,
                  darkTheme: theme,
                  themeMode: isDark ? ThemeMode.dark : ThemeMode.light,
                  locale: const Locale('vi', 'VN'),
                  supportedLocales: const <Locale>[Locale('vi', 'VN')],
                  localizationsDelegates: const <LocalizationsDelegate<dynamic>>[
                    GlobalMaterialLocalizations.delegate,
                    GlobalWidgetsLocalizations.delegate,
                    GlobalCupertinoLocalizations.delegate,
                  ],
                  home: RepaintBoundary(
                    key: repaintKey,
                    child: const LifeOsShellScreen(),
                  ),
                ),
              ),
            ),
          );

          await tester.pumpAndSettle();

          if (tabLabel != 'Hôm nay') {
            await tester.tap(find.text(tabLabel));
            await tester.pumpAndSettle();
          }

          await tester.runAsync(() async {
            final RenderRepaintBoundary boundary =
                repaintKey.currentContext!.findRenderObject()! as RenderRepaintBoundary;
            final ui.Image image = await boundary.toImage(pixelRatio: 1.0);
            final ByteData? byteData = await image.toByteData(format: ui.ImageByteFormat.png);
            final Uint8List pngBytes = byteData!.buffer.asUint8List();

            final File file = File('../docs/review/mobile_screenshots/ui_upgrade/preview/preview_${dirKey}_${mode}_$screenKey.png');
            await file.writeAsBytes(pngBytes);
          });
        });
      }
    }
  }
}

