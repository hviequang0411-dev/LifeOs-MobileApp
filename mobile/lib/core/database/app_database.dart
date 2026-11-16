import 'dart:async';
import 'dart:convert';

import 'package:drift/drift.dart';

import '../sync/entity_types.dart';
import 'database_records.dart';
import 'drift_connection.dart';
import 'mutation_coalescer_dao.dart';
import 'notification_and_log_dao.dart';

export 'database_records.dart';
export 'drift_connection.dart';

/// Cơ sở dữ liệu cục bộ trung tâm của LifeOS trên nền **Drift (`package:drift/drift.dart` + `package:drift/native.dart`)**
/// theo đúng quyết định `[CONFIRMED] D10` & `D13` (`FLUTTER_RULES.md` §12.2).
///
/// Đảm bảo:
/// 1. UI chỉ đọc từ Stream (`watchEntities`, `watchOutboxMutations`, `watchSyncState`) qua `driftDb.tableUpdates`.
/// 2. Mọi thao tác ghi cục bộ đều chạy trong một transaction nguyên tử qua `MutationCoalescerDao`.
/// 3. Giới hạn vòng đệm `sync_log` tối đa 500 dòng và hỗ trợ khử trùng thông báo (`delivered_notifications` `D132`).
class AppDatabase {
  AppDatabase._(this.driftDb) {
    _coalescerDao = MutationCoalescerDao(
      driftDb: driftDb,
      runInTransaction: runInTransaction,
      notifyTables: _notifyTables,
      getEntity: getEntity,
      listEntities: listEntities,
      upsertSyncState: upsertSyncState,
    );
    _notificationAndLogDao = NotificationAndLogDao(
      driftDb: driftDb,
      runInTransaction: runInTransaction,
      notifyTables: _notifyTables,
    );
  }

  factory AppDatabase.inMemory() {
    return AppDatabase._(LifeOsDriftDatabase.inMemory());
  }

  factory AppDatabase.openFile(String filePath) {
    return AppDatabase._(LifeOsDriftDatabase.openFile(filePath));
  }

  final LifeOsDriftDatabase driftDb;
  late final MutationCoalescerDao _coalescerDao;
  late final NotificationAndLogDao _notificationAndLogDao;
  bool _isClosed = false;

  static const int maxSyncLogRows = 500;
  static const int maxIosScheduledNotifications = 64;

  /// Kiểm tra toàn vẹn cơ sở dữ liệu cục bộ (`PRAGMA quick_check`).
  bool runQuickIntegrityCheck() {
    final List<QueryRow> rs = driftDb.selectSql('PRAGMA quick_check;');
    if (rs.isEmpty) return false;
    final String first =
        (rs.first.data.values.first ?? '').toString().toLowerCase();
    return first == 'ok';
  }

  /// Chạy một khối lệnh trong SQLite transaction nguyên tử và phát sự kiện cập nhật bảng tới Drift (`TableUpdate`).
  T runInTransaction<T>(
    T Function() action, {
    Set<String> affectedTables = const <String>{},
  }) {
    driftDb.executeSql('BEGIN IMMEDIATE TRANSACTION;');
    try {
      final T result = action();
      driftDb.executeSql('COMMIT;');
      if (affectedTables.isNotEmpty && !_isClosed) {
        _notifyTables(affectedTables);
      }
      return result;
    } catch (e) {
      driftDb.executeSql('ROLLBACK;');
      rethrow;
    }
  }

  void _notifyTables(Iterable<String> tables) {
    if (_isClosed) return;
    final Set<String> tableSet = tables.toSet();
    if (tableSet.isEmpty) return;
    driftDb.notifyDriftTableUpdates(tableSet);
  }

  /// Lắng nghe thay đổi trên một tập bảng Drift qua `driftDb.tableUpdates`.
  Stream<T> watchQuery<T>({
    required Set<String> tables,
    required T Function() evaluate,
  }) {
    late StreamController<T> controller;
    StreamSubscription<Set<TableUpdate>>? sub;

    void emitCurrent() {
      if (!controller.isClosed && !_isClosed) {
        controller.add(evaluate());
      }
    }

    controller = StreamController<T>(
      onListen: () {
        emitCurrent();
        final List<TableInfo<Table, Object?>> targetTables = driftDb.allTables
            .where(
              (TableInfo<Table, Object?> t) =>
                  tables.contains(t.actualTableName),
            )
            .toList();
        final TableUpdateQuery updateQuery = targetTables.isEmpty
            ? const TableUpdateQuery.any()
            : TableUpdateQuery.onAllTables(targetTables);
        sub = driftDb.tableUpdates(updateQuery).listen((_) {
          emitCurrent();
        });
      },
      onCancel: () async {
        await sub?.cancel();
      },
    );
    return controller.stream;
  }

  // ===========================================================================
  // ĐỌC / GHI TRẠNG THÁI ĐỒNG BỘ (`sync_state`)
  // ===========================================================================

  LocalSyncStateRecord getSyncState(String userId) {
    final List<QueryRow> rows = driftDb.selectSql(
      'SELECT user_id, last_cursor, last_sync_at, sync_paused, device_id '
      'FROM sync_state WHERE user_id = ?;',
      <Object?>[userId],
    );
    if (rows.isEmpty) {
      return LocalSyncStateRecord(
        userId: userId,
        lastCursor: 0,
        syncPaused: false,
      );
    }
    final Map<String, dynamic> row = rows.first.data;
    return LocalSyncStateRecord(
      userId: row['user_id'] as String,
      lastCursor: (row['last_cursor'] as int?) ?? 0,
      lastSyncAt: row['last_sync_at'] as String?,
      syncPaused: ((row['sync_paused'] as int?) ?? 0) != 0,
      deviceId: row['device_id'] as String?,
    );
  }

  Stream<LocalSyncStateRecord> watchSyncState(String userId) {
    return watchQuery<LocalSyncStateRecord>(
      tables: const <String>{'sync_state'},
      evaluate: () => getSyncState(userId),
    );
  }

  void upsertSyncState({
    required String userId,
    int? lastCursor,
    String? lastSyncAt,
    bool? syncPaused,
    String? deviceId,
  }) {
    final LocalSyncStateRecord current = getSyncState(userId);
    final int nextCursor = lastCursor ?? current.lastCursor;
    final String? nextSyncAt = lastSyncAt ?? current.lastSyncAt;
    final int nextPaused = (syncPaused ?? current.syncPaused) ? 1 : 0;
    final String? nextDevice = deviceId ?? current.deviceId;

    driftDb.executeSql(
      '''
      INSERT INTO sync_state (user_id, last_cursor, last_sync_at, sync_paused, device_id)
      VALUES (?, ?, ?, ?, ?)
      ON CONFLICT(user_id) DO UPDATE SET
        last_cursor = excluded.last_cursor,
        last_sync_at = excluded.last_sync_at,
        sync_paused = excluded.sync_paused,
        device_id = excluded.device_id;
      ''',
      <Object?>[userId, nextCursor, nextSyncAt, nextPaused, nextDevice],
    );
    _notifyTables(const <String>['sync_state']);
  }

  // ===========================================================================
  // ĐỌC THỰC THỂ TỪ DRIFT (`29 entityType`)
  // ===========================================================================

  LocalEntityRow? getEntity({
    required String userId,
    required SyncEntityType entityType,
    required String entityId,
    bool includeDeleted = false,
  }) {
    final String sqlQuery = includeDeleted
        ? 'SELECT id, user_id, version, updated_at, deleted_at, sync_state, payload_json '
            'FROM ${entityType.tableName} WHERE user_id = ? AND id = ?;'
        : 'SELECT id, user_id, version, updated_at, deleted_at, sync_state, payload_json '
            'FROM ${entityType.tableName} WHERE user_id = ? AND id = ? AND deleted_at IS NULL;';
    final List<QueryRow> rows =
        driftDb.selectSql(sqlQuery, <Object?>[userId, entityId]);
    if (rows.isEmpty) return null;
    return _mapEntityRow(entityType, rows.first);
  }

  List<LocalEntityRow> listEntities({
    required String userId,
    required SyncEntityType entityType,
    bool includeDeleted = false,
  }) {
    final String sqlQuery = includeDeleted
        ? 'SELECT id, user_id, version, updated_at, deleted_at, sync_state, payload_json '
            'FROM ${entityType.tableName} WHERE user_id = ? ORDER BY updated_at DESC;'
        : 'SELECT id, user_id, version, updated_at, deleted_at, sync_state, payload_json '
            'FROM ${entityType.tableName} WHERE user_id = ? AND deleted_at IS NULL ORDER BY updated_at DESC;';
    final List<QueryRow> rows = driftDb.selectSql(sqlQuery, <Object?>[userId]);
    return rows.map((QueryRow r) => _mapEntityRow(entityType, r)).toList();
  }

  Stream<List<LocalEntityRow>> watchEntities({
    required String userId,
    required SyncEntityType entityType,
    bool includeDeleted = false,
  }) {
    return watchQuery<List<LocalEntityRow>>(
      tables: <String>{entityType.tableName},
      evaluate: () => listEntities(
        userId: userId,
        entityType: entityType,
        includeDeleted: includeDeleted,
      ),
    );
  }

  LocalEntityRow _mapEntityRow(SyncEntityType type, QueryRow row) {
    final Map<String, dynamic> data = row.data;
    final String rawJson = data['payload_json'] as String;
    final Map<String, dynamic> decoded =
        (jsonDecode(rawJson) as Map<dynamic, dynamic>).cast<String, dynamic>();
    return LocalEntityRow(
      id: data['id'] as String,
      userId: data['user_id'] as String,
      entityType: type,
      version: (data['version'] as int?) ?? 0,
      updatedAt: data['updated_at'] as String,
      deletedAt: data['deleted_at'] as String?,
      syncState: (data['sync_state'] as String?) ?? 'synced',
      payload: decoded,
    );
  }

  Map<String, int> countActiveEntitiesByType(String userId) {
    final Map<String, int> counts = <String, int>{};
    for (final SyncEntityType type in SyncEntityType.values) {
      final List<QueryRow> rs = driftDb.selectSql(
        'SELECT COUNT(*) AS cnt FROM ${type.tableName} WHERE user_id = ? AND deleted_at IS NULL;',
        <Object?>[userId],
      );
      counts[type.wireName] = (rs.first.data['cnt'] as int?) ?? 0;
    }
    return counts;
  }

  // ===========================================================================
  // GHI CỤC BỘ + HỢP NHẤT OUTBOX (`MutationCoalescerDao`)
  // ===========================================================================

  OutboxMutationRecord? mutateLocalAndEnqueueOutbox({
    required String userId,
    required SyncEntityType entityType,
    required String entityId,
    required String op,
    required Map<String, dynamic> payload,
    DateTime? nowUtc,
  }) {
    return _coalescerDao.mutateLocalAndEnqueueOutbox(
      userId: userId,
      entityType: entityType,
      entityId: entityId,
      op: op,
      payload: payload,
      nowUtc: nowUtc,
    );
  }

  // ===========================================================================
  // QUẢN LÝ OUTBOX & SNAPSHOT CƠ SỞ CHO SYNC ENGINE
  // ===========================================================================

  List<OutboxMutationRecord> listOutboxMutations({
    required String userId,
    List<String>? statuses,
    int? limit,
  }) {
    final StringBuffer query = StringBuffer(
      'SELECT mutation_id, user_id, entity_type, entity_id, op, base_version, '
      'payload_json, status, retry_count, last_error_code, last_error_message, '
      'server_conflict_json, created_at, updated_at '
      'FROM outbox WHERE user_id = ?',
    );
    final List<Object?> args = <Object?>[userId];

    if (statuses != null && statuses.isNotEmpty) {
      final String placeholders =
          List<String>.filled(statuses.length, '?').join(', ');
      query.write(' AND status IN ($placeholders)');
      args.addAll(statuses);
    }
    query.write(' ORDER BY created_at ASC');
    if (limit != null) {
      query.write(' LIMIT ?');
      args.add(limit);
    }
    query.write(';');

    final List<QueryRow> rows = driftDb.selectSql(query.toString(), args);
    return rows.map(_mapOutboxRow).toList();
  }

  Stream<List<OutboxMutationRecord>> watchOutboxMutations(String userId) {
    return watchQuery<List<OutboxMutationRecord>>(
      tables: const <String>{'outbox'},
      evaluate: () => listOutboxMutations(userId: userId),
    );
  }

  OutboxMutationRecord _mapOutboxRow(QueryRow row) {
    final Map<String, dynamic> data = row.data;
    final Map<String, dynamic> payload =
        (jsonDecode(data['payload_json'] as String) as Map<dynamic, dynamic>)
            .cast<String, dynamic>();
    final String? conflictRaw = data['server_conflict_json'] as String?;
    final Map<String, dynamic>? conflict = conflictRaw == null
        ? null
        : (jsonDecode(conflictRaw) as Map<dynamic, dynamic>)
            .cast<String, dynamic>();

    return OutboxMutationRecord(
      mutationId: data['mutation_id'] as String,
      userId: data['user_id'] as String,
      entityType: SyncEntityType.fromWireName(data['entity_type'] as String),
      entityId: data['entity_id'] as String,
      op: data['op'] as String,
      baseVersion: (data['base_version'] as int?) ?? 0,
      payload: payload,
      status: data['status'] as String,
      retryCount: (data['retry_count'] as int?) ?? 0,
      lastErrorCode: data['last_error_code'] as String?,
      lastErrorMessage: data['last_error_message'] as String?,
      serverConflict: conflict,
      createdAt: data['created_at'] as String,
      updatedAt: data['updated_at'] as String,
    );
  }

  Map<String, dynamic>? getBaseSnapshot({
    required String userId,
    required SyncEntityType entityType,
    required String entityId,
  }) {
    final List<QueryRow> rows = driftDb.selectSql(
      'SELECT snapshot_json FROM entity_base_snapshots '
      'WHERE user_id = ? AND entity_type = ? AND entity_id = ?;',
      <Object?>[userId, entityType.wireName, entityId],
    );
    if (rows.isEmpty) return null;
    return (jsonDecode(rows.first.data['snapshot_json'] as String)
            as Map<dynamic, dynamic>)
        .cast<String, dynamic>();
  }

  void markMutationsInFlight(List<String> mutationIds) {
    if (mutationIds.isEmpty) return;
    final String placeholders =
        List<String>.filled(mutationIds.length, '?').join(', ');
    driftDb.executeSql(
      "UPDATE outbox SET status = 'IN_FLIGHT' WHERE mutation_id IN ($placeholders);",
      mutationIds,
    );
    _notifyTables(const <String>['outbox']);
  }

  void resetInFlightToPending(String userId) {
    driftDb.executeSql(
      "UPDATE outbox SET status = 'PENDING', retry_count = retry_count + 1 "
      "WHERE user_id = ? AND status = 'IN_FLIGHT';",
      <Object?>[userId],
    );
    _notifyTables(const <String>['outbox']);
  }

  void completeOutboxMutationApplied({
    required String userId,
    required OutboxMutationRecord mutation,
    required int assignedVersion,
    required String serverUpdatedAt,
    Map<String, dynamic>? serverEntityState,
  }) {
    runInTransaction<void>(
      affectedTables: <String>{
        mutation.entityType.tableName,
        'outbox',
        'entity_base_snapshots',
      },
      () {
        driftDb.executeSql(
          'DELETE FROM outbox WHERE mutation_id = ?;',
          <Object?>[mutation.mutationId],
        );
        final Map<String, dynamic> finalState = <String, dynamic>{
          ...mutation.payload,
          if (serverEntityState != null) ...serverEntityState,
          'id': mutation.entityId,
          'version': assignedVersion,
          'updatedAt': serverUpdatedAt,
        };
        final String? deletedAt = mutation.op == 'DELETE'
            ? (finalState['deletedAt'] as String? ?? serverUpdatedAt)
            : (finalState['deletedAt'] as String?);

        driftDb.executeSql(
          '''
          INSERT INTO ${mutation.entityType.tableName} (
            id, user_id, version, updated_at, deleted_at, sync_state, payload_json
          ) VALUES (?, ?, ?, ?, ?, 'synced', ?)
          ON CONFLICT(user_id, id) DO UPDATE SET
            version = excluded.version,
            updated_at = excluded.updated_at,
            deleted_at = excluded.deleted_at,
            sync_state = 'synced',
            payload_json = excluded.payload_json;
          ''',
          <Object?>[
            mutation.entityId,
            userId,
            assignedVersion,
            serverUpdatedAt,
            deletedAt,
            jsonEncode(finalState),
          ],
        );

        driftDb.executeSql(
          '''
          INSERT INTO entity_base_snapshots (
            user_id, entity_type, entity_id, base_version, snapshot_json, updated_at
          ) VALUES (?, ?, ?, ?, ?, ?)
          ON CONFLICT(user_id, entity_type, entity_id) DO UPDATE SET
            base_version = excluded.base_version,
            snapshot_json = excluded.snapshot_json,
            updated_at = excluded.updated_at;
          ''',
          <Object?>[
            userId,
            mutation.entityType.wireName,
            mutation.entityId,
            assignedVersion,
            jsonEncode(finalState),
            serverUpdatedAt,
          ],
        );
      },
    );
  }

  void markMutationNeedsUserAction({
    required String mutationId,
    required String status,
    required String errorCode,
    required String errorMessage,
    Map<String, dynamic>? serverCurrent,
  }) {
    driftDb.executeSql(
      '''
      UPDATE outbox
      SET status = ?,
          last_error_code = ?,
          last_error_message = ?,
          server_conflict_json = ?,
          updated_at = ?
      WHERE mutation_id = ?;
      ''',
      <Object?>[
        status,
        errorCode,
        errorMessage,
        serverCurrent == null ? null : jsonEncode(serverCurrent),
        DateTime.now().toUtc().toIso8601String(),
        mutationId,
      ],
    );
    _notifyTables(const <String>['outbox']);
  }

  void applyPulledChangesBatch({
    required String userId,
    required List<Map<String, dynamic>> changes,
    required int nextCursor,
    required String syncedAtIso,
  }) {
    final Set<String> affected = <String>{'sync_state', 'entity_base_snapshots'};
    for (final Map<String, dynamic> change in changes) {
      final String? wireType = change['entityType'] as String?;
      final SyncEntityType? type =
          wireType == null ? null : SyncEntityType.tryFromWireName(wireType);
      if (type != null) {
        affected.add(type.tableName);
      }
    }

    runInTransaction<void>(
      affectedTables: affected,
      () {
        for (final Map<String, dynamic> change in changes) {
          final String? wireType = change['entityType'] as String?;
          final SyncEntityType? type =
              wireType == null ? null : SyncEntityType.tryFromWireName(wireType);
          if (type == null) continue;

          final String entityId = (change['entityId'] ?? change['id']).toString();
          final int version = (change['version'] as num?)?.toInt() ?? 1;
          final String updatedAt =
              (change['updatedAt'] as String?) ?? syncedAtIso;
          final String? deletedAt = change['deletedAt'] as String?;
          final String op =
              ((change['operation'] ?? change['op']) as String? ?? 'UPSERT')
                  .toUpperCase();

          final List<QueryRow> pendingForEntity = driftDb.selectSql(
            "SELECT mutation_id FROM outbox "
            "WHERE user_id = ? AND entity_type = ? AND entity_id = ? AND status IN ('PENDING', 'IN_FLIGHT');",
            <Object?>[userId, type.wireName, entityId],
          );
          if (pendingForEntity.isNotEmpty) {
            continue;
          }

          final Map<String, dynamic> entityData =
              (change['data'] is Map<dynamic, dynamic>)
                  ? (change['data'] as Map<dynamic, dynamic>)
                      .cast<String, dynamic>()
                  : (change['payload'] is Map<dynamic, dynamic>)
                      ? (change['payload'] as Map<dynamic, dynamic>)
                          .cast<String, dynamic>()
                      : <String, dynamic>{...change};

          final Map<String, dynamic> normalized = <String, dynamic>{
            ...entityData,
            'id': entityId,
            'version': version,
            'updatedAt': updatedAt,
            if (deletedAt != null) 'deletedAt': deletedAt,
          };

          final String? effectiveDeletedAt =
              (op == 'DELETE' || deletedAt != null)
                  ? (deletedAt ?? updatedAt)
                  : null;

          driftDb.executeSql(
            '''
            INSERT INTO ${type.tableName} (
              id, user_id, version, updated_at, deleted_at, sync_state, payload_json
            ) VALUES (?, ?, ?, ?, ?, 'synced', ?)
            ON CONFLICT(user_id, id) DO UPDATE SET
              version = excluded.version,
              updated_at = excluded.updated_at,
              deleted_at = excluded.deleted_at,
              sync_state = 'synced',
              payload_json = excluded.payload_json
            WHERE excluded.version >= ${type.tableName}.version;
            ''',
            <Object?>[
              entityId,
              userId,
              version,
              updatedAt,
              effectiveDeletedAt,
              jsonEncode(normalized),
            ],
          );

          driftDb.executeSql(
            '''
            INSERT INTO entity_base_snapshots (
              user_id, entity_type, entity_id, base_version, snapshot_json, updated_at
            ) VALUES (?, ?, ?, ?, ?, ?)
            ON CONFLICT(user_id, entity_type, entity_id) DO UPDATE SET
              base_version = excluded.base_version,
              snapshot_json = excluded.snapshot_json,
              updated_at = excluded.updated_at;
            ''',
            <Object?>[
              userId,
              type.wireName,
              entityId,
              version,
              jsonEncode(normalized),
              updatedAt,
            ],
          );
        }

        upsertSyncState(
          userId: userId,
          lastCursor: nextCursor,
          lastSyncAt: syncedAtIso,
        );
      },
    );
  }

  // ===========================================================================
  // LỊCH NHẮC CỤC BỘ (GIỚI HẠN 64 MỐC iOS — D133) & KHỬ TRÙNG (`delivered_notifications` — D132)
  // ===========================================================================

  List<ScheduledNotificationRecord> replaceScheduledNotifications({
    required String userId,
    required List<ScheduledNotificationRecord> candidates,
  }) =>
      _notificationAndLogDao.replaceScheduledNotifications(
        userId: userId,
        candidates: candidates,
      );

  List<ScheduledNotificationRecord> listScheduledNotifications(String userId) =>
      _notificationAndLogDao.listScheduledNotifications(userId);

  bool recordNotificationDeliveryIfNew({
    required String userId,
    required String dedupeKey,
    DateTime? nowUtc,
  }) =>
      _notificationAndLogDao.recordNotificationDeliveryIfNew(
        userId: userId,
        dedupeKey: dedupeKey,
        nowUtc: nowUtc,
      );

  // ===========================================================================
  // NHẬT KÝ ĐỒNG BỘ VÒNG ĐỆM 500 DÒNG (`sync_log`)
  // ===========================================================================

  void appendSyncLog({
    required String userId,
    required String direction,
    required String outcome,
    String? entityType,
    String? entityId,
    String? mutationId,
    int? latencyMs,
    String? errorCode,
    String? details,
  }) =>
      _notificationAndLogDao.appendSyncLog(
        userId: userId,
        direction: direction,
        outcome: outcome,
        entityType: entityType,
        entityId: entityId,
        mutationId: mutationId,
        latencyMs: latencyMs,
        errorCode: errorCode,
        details: details,
      );

  List<SyncLogRecord> listSyncLogs(String userId, {int limit = 500}) =>
      _notificationAndLogDao.listSyncLogs(userId, limit: limit);

  Map<String, dynamic> exportUnsyncedBackupJson(String userId) {
    final List<OutboxMutationRecord> pending =
        listOutboxMutations(userId: userId);
    return <String, dynamic>{
      'exportedAt': DateTime.now().toUtc().toIso8601String(),
      'userId': userId,
      'pendingCount': pending.length,
      'mutations':
          pending.map((OutboxMutationRecord m) => m.toPushWireJson()).toList(),
    };
  }

  void clearUserData(String userId) {
    final Set<String> allTables = <String>{
      'sync_state',
      'outbox',
      'entity_base_snapshots',
      'sync_log',
      'scheduled_notifications',
      'delivered_notifications',
      for (final SyncEntityType t in SyncEntityType.values) t.tableName,
    };
    runInTransaction<void>(
      affectedTables: allTables,
      () {
        driftDb.executeSql(
          'DELETE FROM sync_state WHERE user_id = ?;',
          <Object?>[userId],
        );
        driftDb.executeSql(
          'DELETE FROM outbox WHERE user_id = ?;',
          <Object?>[userId],
        );
        driftDb.executeSql(
          'DELETE FROM entity_base_snapshots WHERE user_id = ?;',
          <Object?>[userId],
        );
        driftDb.executeSql(
          'DELETE FROM sync_log WHERE user_id = ?;',
          <Object?>[userId],
        );
        driftDb.executeSql(
          'DELETE FROM scheduled_notifications WHERE user_id = ?;',
          <Object?>[userId],
        );
        driftDb.executeSql(
          'DELETE FROM delivered_notifications WHERE user_id = ?;',
          <Object?>[userId],
        );
        for (final SyncEntityType t in SyncEntityType.values) {
          driftDb.executeSql(
            'DELETE FROM ${t.tableName} WHERE user_id = ?;',
            <Object?>[userId],
          );
        }
      },
    );
  }

  void close() {
    if (_isClosed) return;
    _isClosed = true;
    unawaited(driftDb.close());
    driftDb.disposeDatabase();
  }
}
