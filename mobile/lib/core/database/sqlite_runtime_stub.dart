import 'package:drift/drift.dart';
import 'package:sqlite3/common.dart';

typedef OpenedDriftBackend = ({CommonDatabase rawDb, QueryExecutor executor});

Future<void> initializeSqliteRuntime() {
  throw UnsupportedError(
    'Nền tảng hiện tại không hỗ trợ runtime SQLite (chỉ hỗ trợ Dart IO hoặc Web WASM).',
  );
}

OpenedDriftBackend openInMemoryDriftBackend({
  void Function(CommonDatabase db)? setup,
}) {
  throw UnsupportedError('Không hỗ trợ mở Drift in-memory trên nền tảng này.');
}

OpenedDriftBackend openPersistentDriftBackend(
  String pathOrName, {
  void Function(CommonDatabase db)? setup,
}) {
  throw UnsupportedError('Không hỗ trợ mở Drift cục bộ trên nền tảng này.');
}

CommonDatabase openInMemoryRawDatabase() {
  throw UnsupportedError('Không hỗ trợ mở SQLite raw trên nền tảng này.');
}

CommonDatabase openUserExtrasRawDatabase(
  String userId, {
  String? baseDirectory,
}) {
  throw UnsupportedError(
    'Không hỗ trợ mở kho cục bộ mở rộng trên nền tảng này.',
  );
}

void deleteLocalDatabaseFiles(
  String userId, {
  String? baseDirectory,
}) {
  // Không có thao tác tệp trên nền tảng không xác định.
}

