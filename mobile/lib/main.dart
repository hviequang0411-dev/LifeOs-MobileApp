import 'package:flutter/foundation.dart' show kIsWeb;
import 'package:flutter/material.dart';
import 'package:flutter_localizations/flutter_localizations.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import 'core/config/app_flavor.dart';
import 'core/database/app_database.dart';
import 'core/database/sqlite_runtime.dart';
import 'core/network/api_client.dart';
import 'core/providers/app_providers.dart';
import 'core/theme/app_theme.dart';
import 'features/workspace/presentation/lifeos_shell_screen.dart';

Future<void> main() async {
  WidgetsFlutterBinding.ensureInitialized();
  await initializeSqliteRuntime();
  runLifeOsWithFlavor(AppEnvironment.dev);
}

void runLifeOsWithFlavor(AppEnvironment env) {
  WidgetsFlutterBinding.ensureInitialized();
  final AppFlavorConfig config = AppFlavorConfig.fromEnvironment(env);
  final AppDatabase db = kIsWeb
      ? _openWebDatabaseSafe()
      : AppDatabase.inMemory();
  runApp(
    ProviderScope(
      overrides: <Override>[
        appFlavorProvider.overrideWithValue(config),
        appDatabaseProvider.overrideWithValue(db),
        tokenStorageProvider.overrideWithValue(const SecureTokenStorage()),
      ],
      child: const LifeOsApp(),
    ),
  );
}

AppDatabase _openWebDatabaseSafe() {
  try {
    return AppDatabase.openFile('/lifeos_web.sqlite');
  } catch (_) {
    return AppDatabase.inMemory();
  }
}

class LifeOsApp extends ConsumerWidget {
  const LifeOsApp({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final AppFlavorConfig flavor = ref.watch(appFlavorProvider);
    final ThemeMode themeMode = ref.watch(themeModeProvider);

    return MaterialApp(
      debugShowCheckedModeBanner: false,
      title: flavor.appName,
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
      home: const LifeOsShellScreen(),
    );
  }
}

