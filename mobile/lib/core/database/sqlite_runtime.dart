import 'package:drift/drift.dart';
import 'package:sqlite3/common.dart';

import 'sqlite_runtime_stub.dart'
    if (dart.library.io) 'sqlite_runtime_io.dart'
    if (dart.library.js_interop) 'sqlite_runtime_web.dart' as impl;

export 'package:sqlite3/common.dart';

typedef OpenedDriftBackend = ({CommonDatabase rawDb, QueryExecutor executor});

Future<void> initializeSqliteRuntime() => impl.initializeSqliteRuntime();

OpenedDriftBackend openInMemoryDriftBackend({
  void Function(CommonDatabase db)? setup,
}) =>
    impl.openInMemoryDriftBackend(setup: setup);

OpenedDriftBackend openPersistentDriftBackend(
  String pathOrName, {
  void Function(CommonDatabase db)? setup,
}) =>
    impl.openPersistentDriftBackend(pathOrName, setup: setup);

CommonDatabase openInMemoryRawDatabase() => impl.openInMemoryRawDatabase();

CommonDatabase openUserExtrasRawDatabase(
  String userId, {
  String? baseDirectory,
}) =>
    impl.openUserExtrasRawDatabase(userId, baseDirectory: baseDirectory);

void deleteLocalDatabaseFiles(String userId) =>
    impl.deleteLocalDatabaseFiles(userId);

