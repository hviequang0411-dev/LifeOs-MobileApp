import 'dart:convert';

import 'package:flutter/foundation.dart' show kIsWeb;
import 'package:flutter/material.dart';
import 'package:flutter_localizations/flutter_localizations.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:path_provider/path_provider.dart';

import 'core/config/app_flavor.dart';
import 'core/database/app_database.dart';
import 'core/database/sqlite_runtime.dart';
import 'core/network/api_client.dart';
import 'core/providers/app_providers.dart';
import 'core/theme/app_theme.dart';
import 'features/notification/domain/local_notification_scheduler.dart';
import 'features/workspace/data/local_extras_repository.dart';
import 'features/workspace/presentation/lifeos_shell_screen.dart';

Future<void> main() async {
  WidgetsFlutterBinding.ensureInitialized();
  await initializeSqliteRuntime();
  await runLifeOsWithFlavor(AppEnvironment.dev);
}

Future<void> runLifeOsWithFlavor(AppEnvironment env) async {
  WidgetsFlutterBinding.ensureInitialized();
  final AppFlavorConfig config = AppFlavorConfig.fromEnvironment(env);

  String? storageDir;
  late final AppDatabase db;
  if (kIsWeb) {
    db = _openWebDatabaseSafe();
  } else {
    try {
      final dynamic docsDir = await getApplicationDocumentsDirectory();
      final String path = (docsDir.path as String).replaceAll('\\', '/');
      storageDir = path;
      db = AppDatabase.openFile('$path/lifeos_main.sqlite');
    } catch (_) {
      db = AppDatabase.inMemory();
    }
  }

  const SecureTokenStorage tokenStorage = SecureTokenStorage();
  String initialUserId = 'user-local-01';
  try {
    final String? savedUid = await tokenStorage.readUserId();
    if (savedUid != null && savedUid.trim().isNotEmpty) {
      initialUserId = savedUid.trim();
    }
  } catch (_) {}

  ThemeMode initialThemeMode = ThemeMode.light;
  NotificationPreferences initialNotifPrefs = const NotificationPreferences();
  try {
    final LifeOsLocalExtrasDatabase extrasDb =
        LifeOsLocalExtrasDatabase.openForUser(
      initialUserId,
      baseDirectory: storageDir,
    );
    try {
      if (extrasDb.getPref('theme_mode') == 'dark') {
        initialThemeMode = ThemeMode.dark;
      }
      final String? rawNotif = extrasDb.getPref('notification_prefs');
      if (rawNotif != null && rawNotif.isNotEmpty) {
        final Object? decoded = jsonDecode(rawNotif);
        if (decoded is Map<dynamic, dynamic>) {
          initialNotifPrefs = NotificationPreferences.fromJson(
            decoded.cast<String, dynamic>(),
          );
        }
      }
    } finally {
      extrasDb.close();
    }
  } catch (_) {}

  runApp(
    ProviderScope(
      overrides: <Override>[
        appFlavorProvider.overrideWithValue(config),
        appDatabaseProvider.overrideWithValue(db),
        tokenStorageProvider.overrideWithValue(tokenStorage),
        currentUserIdProvider.overrideWith((Ref ref) => initialUserId),
        themeModeProvider.overrideWith((Ref ref) => initialThemeMode),
        notificationPreferencesProvider
            .overrideWith((Ref ref) => initialNotifPrefs),
        usePersistentLocalExtrasProvider.overrideWithValue(true),
        localStorageBaseDirProvider.overrideWithValue(storageDir),
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

