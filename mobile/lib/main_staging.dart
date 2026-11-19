import 'package:flutter/widgets.dart';

import 'core/config/app_flavor.dart';
import 'core/database/sqlite_runtime.dart';
import 'main.dart';

Future<void> main() async {
  WidgetsFlutterBinding.ensureInitialized();
  await initializeSqliteRuntime();
  await runLifeOsWithFlavor(AppEnvironment.staging);
}

