import 'package:drift/drift.dart';

import '../sync/entity_types.dart';
import 'drift_tables.dart';
import 'sqlite_runtime.dart';

export 'drift_tables.dart';

part 'drift_connection.g.dart';

/// DAO hạ tầng Drift (`@DriftAccessor`) được sinh mã bởi `drift_dev` (`build_runner`).
@DriftAccessor(
  tables: <Type>[
    SyncStateDriftTable,
    OutboxDriftTable,
    EntityBaseSnapshotDriftTable,
    SyncLogDriftTable,
    ScheduledNotificationDriftTable,
    DeliveredNotificationDriftTable,
  ],
)
class DriftSyncInfrastructureDao extends DatabaseAccessor<LifeOsDriftDatabase>
    with _$DriftSyncInfrastructureDaoMixin {
  DriftSyncInfrastructureDao(super.attachedDatabase);
}

/// Cơ sở dữ liệu Drift chính thức của LifeOS (`@DriftDatabase` + `drift_dev` sinh `drift_connection.g.dart`)
/// tuân thủ quyết định `[CONFIRMED] D10` & `D13` (`CODE_GUIDELINES.md` §15, `FLUTTER_RULES.md` §12.2).
///
/// Quản lý:
/// - **29 bảng thực thể đồng bộ** (`SyncEntityType.values`), mỗi bảng có `sync_state TEXT NOT NULL DEFAULT 'synced'`.
/// - **6 bảng hạ tầng cục bộ**: `sync_state`, `outbox`, `entity_base_snapshots`, `sync_log` (vòng đệm 500 dòng),
///   `scheduled_notifications` (`D133` giới hạn 64 thông báo iOS) và `delivered_notifications` (`D132` khử trùng 7 ngày).
@DriftDatabase(
  tables: <Type>[
    // 6 bảng hạ tầng
    SyncStateDriftTable,
    OutboxDriftTable,
    EntityBaseSnapshotDriftTable,
    SyncLogDriftTable,
    ScheduledNotificationDriftTable,
    DeliveredNotificationDriftTable,
    // 29 bảng thực thể đồng bộ
    UserSettingsDriftTable,
    CategoriesDriftTable,
    ProjectsDriftTable,
    TasksDriftTable,
    HabitsDriftTable,
    HabitCheckinsDriftTable,
    RoutinesDriftTable,
    RoutineStepsDriftTable,
    RoutineLogsDriftTable,
    RoutineStepLogsDriftTable,
    FinancialAccountsDriftTable,
    FinancialCategoriesDriftTable,
    FinancialTransactionsDriftTable,
    AccountTransfersDriftTable,
    BudgetsDriftTable,
    RecurringTransactionsDriftTable,
    SavingGoalsDriftTable,
    SavingContributionsDriftTable,
    CalendarEventsDriftTable,
    EventExceptionsDriftTable,
    TimeBlocksDriftTable,
    RemindersDriftTable,
    NotesDriftTable,
    TagsDriftTable,
    NoteTagsDriftTable,
    NoteLinksDriftTable,
    GoalsDriftTable,
    MilestonesDriftTable,
    ReviewsDriftTable,
  ],
  daos: <Type>[
    DriftSyncInfrastructureDao,
  ],
)
class LifeOsDriftDatabase extends _$LifeOsDriftDatabase {
  LifeOsDriftDatabase._(super.executor, this._rawDb);

  factory LifeOsDriftDatabase.inMemory() {
    final OpenedDriftBackend backend = openInMemoryDriftBackend(
      setup: initializeSchemaOnRaw,
    );
    return LifeOsDriftDatabase._(backend.executor, backend.rawDb);
  }

  factory LifeOsDriftDatabase.openFile(String filePath) {
    final OpenedDriftBackend backend = openPersistentDriftBackend(
      filePath,
      setup: initializeSchemaOnRaw,
    );
    return LifeOsDriftDatabase._(backend.executor, backend.rawDb);
  }

  final CommonDatabase _rawDb;

  @override
  int get schemaVersion => 1;

  @override
  MigrationStrategy get migration => MigrationStrategy(
        onCreate: (Migrator m) async {
          await m.createAll();
          initializeSchemaOnRaw(_rawDb);
        },
      );

  /// Thực thi câu lệnh SQL ghi đồng bộ trên kết nối SQLite của Drift (`NativeDatabase` trên IO / `WasmDatabase` trên Web).
  void executeSql(
    String sqlQuery, [
    List<Object?> parameters = const <Object?>[],
  ]) {
    _rawDb.execute(sqlQuery, parameters);
  }

  /// Thực thi câu lệnh SQL đọc và trả về danh sách `QueryRow` chuẩn của `package:drift/drift.dart`.
  List<QueryRow> selectSql(
    String sqlQuery, [
    List<Object?> parameters = const <Object?>[],
  ]) {
    final ResultSet rs = _rawDb.select(sqlQuery, parameters);
    return rs
        .map(
          (Row row) => QueryRow(
            Map<String, dynamic>.from(row),
            this,
          ),
        )
        .toList();
  }

  /// Đóng kết nối Drift và giải phóng tài nguyên cơ sở dữ liệu.
  void disposeDatabase() {
    _rawDb.dispose();
  }

  /// Thông báo cho bộ điều phối luồng bảng của Drift (`StreamQueries`) khi có bảng thay đổi.
  void notifyDriftTableUpdates(Iterable<String> tableNames) {
    final Set<TableUpdate> updates =
        tableNames.map((String name) => TableUpdate(name)).toSet();
    if (updates.isNotEmpty) {
      notifyUpdates(updates);
    }
  }

  /// Khởi tạo đầy đủ DDL 29 bảng thực thể đồng bộ + 6 bảng hạ tầng đồng bộ/thông báo + chỉ mục.
  static void initializeSchemaOnRaw(CommonDatabase db) {
    db.execute('PRAGMA foreign_keys = ON;');
    db.execute('PRAGMA busy_timeout = 5000;');

    db.execute('''
      CREATE TABLE IF NOT EXISTS sync_state (
        user_id TEXT PRIMARY KEY,
        last_cursor INTEGER NOT NULL DEFAULT 0,
        last_sync_at TEXT,
        sync_paused INTEGER NOT NULL DEFAULT 0,
        device_id TEXT
      );
    ''');

    db.execute('''
      CREATE TABLE IF NOT EXISTS outbox (
        mutation_id TEXT PRIMARY KEY,
        user_id TEXT NOT NULL,
        entity_type TEXT NOT NULL,
        entity_id TEXT NOT NULL,
        op TEXT NOT NULL,
        base_version INTEGER NOT NULL DEFAULT 0,
        payload_json TEXT NOT NULL,
        status TEXT NOT NULL DEFAULT 'PENDING',
        retry_count INTEGER NOT NULL DEFAULT 0,
        last_error_code TEXT,
        last_error_message TEXT,
        server_conflict_json TEXT,
        created_at TEXT NOT NULL,
        updated_at TEXT NOT NULL
      );
    ''');
    db.execute('''
      CREATE INDEX IF NOT EXISTS idx_outbox_user_status_created
      ON outbox (user_id, status, created_at ASC);
    ''');
    db.execute('''
      CREATE INDEX IF NOT EXISTS idx_outbox_entity_lookup
      ON outbox (user_id, entity_type, entity_id);
    ''');

    db.execute('''
      CREATE TABLE IF NOT EXISTS entity_base_snapshots (
        user_id TEXT NOT NULL,
        entity_type TEXT NOT NULL,
        entity_id TEXT NOT NULL,
        base_version INTEGER NOT NULL,
        snapshot_json TEXT NOT NULL,
        updated_at TEXT NOT NULL,
        PRIMARY KEY (user_id, entity_type, entity_id)
      );
    ''');

    db.execute('''
      CREATE TABLE IF NOT EXISTS sync_log (
        id INTEGER PRIMARY KEY AUTOINCREMENT,
        user_id TEXT NOT NULL,
        direction TEXT NOT NULL,
        outcome TEXT NOT NULL,
        entity_type TEXT,
        entity_id TEXT,
        mutation_id TEXT,
        latency_ms INTEGER,
        error_code TEXT,
        details TEXT,
        created_at TEXT NOT NULL
      );
    ''');

    // Bảng thông báo cục bộ đã lên lịch (D133 — tối đa 64 thông báo gần nhất cho iOS)
    db.execute('''
      CREATE TABLE IF NOT EXISTS scheduled_notifications (
        dedupe_key TEXT PRIMARY KEY,
        user_id TEXT NOT NULL,
        target_type TEXT NOT NULL,
        target_id TEXT NOT NULL,
        fire_at_utc TEXT NOT NULL,
        title TEXT NOT NULL,
        body TEXT NOT NULL,
        redacted_lock_screen INTEGER NOT NULL DEFAULT 1
      );
    ''');

    // Bảng khử trùng thông báo giữa Local Notification và FCM Push (D132 — lưu 7 ngày)
    db.execute('''
      CREATE TABLE IF NOT EXISTS delivered_notifications (
        dedupe_key TEXT PRIMARY KEY,
        user_id TEXT NOT NULL,
        shown_at TEXT NOT NULL
      );
    ''');

    // 29 bảng thực thể đồng bộ theo danh mục SyncEntityType
    for (final SyncEntityType type in SyncEntityType.values) {
      db.execute('''
        CREATE TABLE IF NOT EXISTS ${type.tableName} (
          id TEXT NOT NULL,
          user_id TEXT NOT NULL,
          version INTEGER NOT NULL DEFAULT 0,
          updated_at TEXT NOT NULL,
          deleted_at TEXT,
          sync_state TEXT NOT NULL DEFAULT 'synced',
          payload_json TEXT NOT NULL,
          PRIMARY KEY (user_id, id)
        );
      ''');
      db.execute('''
        CREATE INDEX IF NOT EXISTS idx_${type.tableName}_user_active
        ON ${type.tableName} (user_id, deleted_at, updated_at DESC);
      ''');
    }
  }
}
