import 'package:drift/drift.dart';
import 'package:drift/wasm.dart';
import 'package:sqlite3/wasm.dart';

typedef OpenedDriftBackend = ({CommonDatabase rawDb, QueryExecutor executor});

CommonSqlite3? _webSqlite;
VirtualFileSystem? _webVfs;
bool _usingPersistentIndexedDb = false;

/// Cho biết runtime SQLite trên trình duyệt có đang lưu trữ bền vững bằng IndexedDB hay fallback RAM.
bool get isWebSqlitePersistent => _usingPersistentIndexedDb;

Future<void> initializeSqliteRuntime() async {
  if (_webSqlite != null) return;
  final WasmSqlite3 wasm = await WasmSqlite3.loadFromUrl(
    Uri.parse('sqlite3.wasm'),
  );
  try {
    final IndexedDbFileSystem idb =
        await IndexedDbFileSystem.open(dbName: 'lifeos_web');
    wasm.registerVirtualFileSystem(idb, makeDefault: true);
    _webVfs = idb;
    _usingPersistentIndexedDb = true;
  } catch (_) {
    final InMemoryFileSystem mem = InMemoryFileSystem();
    wasm.registerVirtualFileSystem(mem, makeDefault: true);
    _webVfs = mem;
    _usingPersistentIndexedDb = false;
  }
  _webSqlite = wasm;
}

CommonSqlite3 _requireWebSqlite() {
  final CommonSqlite3? sqlite = _webSqlite;
  if (sqlite == null) {
    throw StateError(
      'SQLite WASM chưa được khởi tạo trên Web. Hãy gọi initializeSqliteRuntime() trước khi mở AppDatabase.',
    );
  }
  return sqlite;
}

String _normalizeVirtualPath(String pathOrName) {
  final String trimmed = pathOrName.trim().replaceAll('\\', '/');
  if (trimmed.isEmpty) return '/lifeos_local.sqlite';
  final int slash = trimmed.lastIndexOf('/');
  final String fileName = slash >= 0 ? trimmed.substring(slash + 1) : trimmed;
  return fileName.isEmpty ? '/lifeos_local.sqlite' : '/$fileName';
}

OpenedDriftBackend openInMemoryDriftBackend({
  void Function(CommonDatabase db)? setup,
}) {
  final CommonDatabase raw = _requireWebSqlite().openInMemory();
  setup?.call(raw);
  return (
    rawDb: raw,
    executor: WasmDatabase.opened(raw, closeUnderlyingOnClose: false),
  );
}

OpenedDriftBackend openPersistentDriftBackend(
  String pathOrName, {
  void Function(CommonDatabase db)? setup,
}) {
  final String vfsPath = _normalizeVirtualPath(pathOrName);
  final CommonDatabase raw = _requireWebSqlite().open(vfsPath);
  setup?.call(raw);
  return (
    rawDb: raw,
    executor: WasmDatabase.opened(raw, closeUnderlyingOnClose: false),
  );
}

CommonDatabase openInMemoryRawDatabase() {
  return _requireWebSqlite().openInMemory();
}

CommonDatabase openUserExtrasRawDatabase(
  String userId, {
  String? baseDirectory,
}) {
  return _requireWebSqlite().open('/lifeos_local_extras_$userId.sqlite');
}

void deleteLocalDatabaseFiles(String userId) {
  final VirtualFileSystem? vfs = _webVfs;
  if (vfs == null) return;
  for (final String path in <String>[
    '/lifeos_$userId.sqlite',
    '/lifeos_local_extras_$userId.sqlite',
  ]) {
    try {
      vfs.xDelete(path, 0);
    } catch (_) {
      // Bỏ qua nếu tệp ảo chưa tồn tại.
    }
  }
}
