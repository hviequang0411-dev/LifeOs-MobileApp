import 'dart:io';

import 'package:drift/drift.dart';
import 'package:drift/native.dart';
import 'package:sqlite3/common.dart';
import 'package:sqlite3/sqlite3.dart' as sql;

typedef OpenedDriftBackend = ({CommonDatabase rawDb, QueryExecutor executor});

Future<void> initializeSqliteRuntime() async {
  // Trên Android/iOS/Desktop (Dart IO), thư viện FFI sqlite3 luôn sẵn sàng đồng bộ.
}

OpenedDriftBackend openInMemoryDriftBackend({
  void Function(CommonDatabase db)? setup,
}) {
  final sql.Database raw = sql.sqlite3.openInMemory();
  setup?.call(raw);
  return (
    rawDb: raw,
    executor: NativeDatabase.opened(raw, closeUnderlyingOnClose: false),
  );
}

OpenedDriftBackend openPersistentDriftBackend(
  String pathOrName, {
  void Function(CommonDatabase db)? setup,
}) {
  final File file = File(pathOrName);
  if (!file.parent.existsSync()) {
    file.parent.createSync(recursive: true);
  }
  final sql.Database raw = sql.sqlite3.open(pathOrName);
  setup?.call(raw);
  return (
    rawDb: raw,
    executor: NativeDatabase.opened(raw, closeUnderlyingOnClose: false),
  );
}

CommonDatabase openInMemoryRawDatabase() {
  return sql.sqlite3.openInMemory();
}

CommonDatabase openUserExtrasRawDatabase(
  String userId, {
  String? baseDirectory,
}) {
  final String dirPath = baseDirectory ?? Directory.current.path;
  final String filePath = '$dirPath/lifeos_local_extras_$userId.sqlite';
  final File file = File(filePath);
  if (!file.parent.existsSync()) {
    file.parent.createSync(recursive: true);
  }
  return sql.sqlite3.open(filePath);
}

void deleteLocalDatabaseFiles(
  String userId, {
  String? baseDirectory,
}) {
  final String dirPath = baseDirectory ?? Directory.current.path;
  for (final String fileName in <String>[
    'lifeos_$userId.sqlite',
    'lifeos_local_extras_$userId.sqlite',
  ]) {
    try {
      final File f = File('$dirPath/$fileName');
      if (f.existsSync()) {
        f.deleteSync();
      }
    } catch (_) {
      // Bỏ qua nếu đang chạy trong bộ nhớ hoặc tệp bị khóa.
    }
  }
}

