import 'package:drift/drift.dart';

import 'database_records.dart';
import 'drift_connection.dart';

/// DAO Drift (`extends DatabaseAccessor<LifeOsDriftDatabase>`) quản lý lịch nhắc cục bộ
/// (`scheduled_notifications` tối đa 64 mục `D133`), bảng khử trùng thông báo 7 ngày
/// (`delivered_notifications` `D132`), và nhật ký đồng bộ vòng đệm 500 dòng (`sync_log`).
class NotificationAndLogDao extends DatabaseAccessor<LifeOsDriftDatabase> {
  NotificationAndLogDao({
    required LifeOsDriftDatabase driftDb,
    required this.runInTransaction,
    required this.notifyTables,
  }) : super(driftDb);

  final T Function<T>(T Function() action, {Set<String> affectedTables})
      runInTransaction;
  final void Function(Iterable<String> tables) notifyTables;

  static const int maxSyncLogRows = 500;
  static const int maxIosScheduledNotifications = 64;

  List<ScheduledNotificationRecord> replaceScheduledNotifications({
    required String userId,
    required List<ScheduledNotificationRecord> candidates,
  }) {
    final List<ScheduledNotificationRecord> sorted =
        List<ScheduledNotificationRecord>.from(candidates)
          ..sort(
            (ScheduledNotificationRecord a, ScheduledNotificationRecord b) =>
                a.fireAtUtcIso.compareTo(b.fireAtUtcIso),
          );
    final List<ScheduledNotificationRecord> capped =
        sorted.take(maxIosScheduledNotifications).toList();

    runInTransaction<void>(
      affectedTables: const <String>{'scheduled_notifications'},
      () {
        attachedDatabase.executeSql(
          'DELETE FROM scheduled_notifications WHERE user_id = ?;',
          <Object?>[userId],
        );
        for (final ScheduledNotificationRecord item in capped) {
          attachedDatabase.executeSql(
            '''
            INSERT OR REPLACE INTO scheduled_notifications (
              dedupe_key, user_id, target_type, target_id, fire_at_utc, title, body, redacted_lock_screen
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?);
            ''',
            <Object?>[
              item.dedupeKey,
              userId,
              item.targetType,
              item.targetId,
              item.fireAtUtcIso,
              item.title,
              item.body,
              item.redactedForLockScreen ? 1 : 0,
            ],
          );
        }
      },
    );
    return capped;
  }

  List<ScheduledNotificationRecord> listScheduledNotifications(String userId) {
    final List<QueryRow> rows = attachedDatabase.selectSql(
      'SELECT dedupe_key, user_id, target_type, target_id, fire_at_utc, title, body, redacted_lock_screen '
      'FROM scheduled_notifications WHERE user_id = ? ORDER BY fire_at_utc ASC;',
      <Object?>[userId],
    );
    return rows
        .map(
          (QueryRow r) => ScheduledNotificationRecord(
            dedupeKey: r.data['dedupe_key'] as String,
            userId: r.data['user_id'] as String,
            targetType: r.data['target_type'] as String,
            targetId: r.data['target_id'] as String,
            fireAtUtcIso: r.data['fire_at_utc'] as String,
            title: r.data['title'] as String,
            body: r.data['body'] as String,
            redactedForLockScreen:
                ((r.data['redacted_lock_screen'] as int?) ?? 1) != 0,
          ),
        )
        .toList();
  }

  bool recordNotificationDeliveryIfNew({
    required String userId,
    required String dedupeKey,
    DateTime? nowUtc,
  }) {
    final DateTime now = (nowUtc ?? DateTime.now()).toUtc();
    final String cutoffIso =
        now.subtract(const Duration(days: 7)).toIso8601String();
    attachedDatabase.executeSql(
      'DELETE FROM delivered_notifications WHERE shown_at < ?;',
      <Object?>[cutoffIso],
    );
    final List<QueryRow> existing = attachedDatabase.selectSql(
      'SELECT dedupe_key FROM delivered_notifications WHERE dedupe_key = ?;',
      <Object?>[dedupeKey],
    );
    if (existing.isNotEmpty) {
      return false;
    }
    attachedDatabase.executeSql(
      'INSERT INTO delivered_notifications (dedupe_key, user_id, shown_at) VALUES (?, ?, ?);',
      <Object?>[dedupeKey, userId, now.toIso8601String()],
    );
    notifyTables(const <String>['delivered_notifications']);
    return true;
  }

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
  }) {
    final String nowIso = DateTime.now().toUtc().toIso8601String();
    attachedDatabase.executeSql(
      '''
      INSERT INTO sync_log (
        user_id, direction, outcome, entity_type, entity_id,
        mutation_id, latency_ms, error_code, details, created_at
      ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?);
      ''',
      <Object?>[
        userId,
        direction,
        outcome,
        entityType,
        entityId,
        mutationId,
        latencyMs,
        errorCode,
        details,
        nowIso,
      ],
    );

    attachedDatabase.executeSql(
      '''
      DELETE FROM sync_log
      WHERE id NOT IN (
        SELECT id FROM sync_log ORDER BY id DESC LIMIT ?
      );
      ''',
      <Object?>[maxSyncLogRows],
    );
    notifyTables(const <String>['sync_log']);
  }

  List<SyncLogRecord> listSyncLogs(String userId, {int limit = 500}) {
    final List<QueryRow> rows = attachedDatabase.selectSql(
      'SELECT id, user_id, direction, outcome, entity_type, entity_id, '
      'mutation_id, latency_ms, error_code, details, created_at '
      'FROM sync_log WHERE user_id = ? ORDER BY id DESC LIMIT ?;',
      <Object?>[userId, limit],
    );
    return rows
        .map(
          (QueryRow r) => SyncLogRecord(
            id: r.data['id'] as int,
            userId: r.data['user_id'] as String,
            direction: r.data['direction'] as String,
            outcome: r.data['outcome'] as String,
            entityType: r.data['entity_type'] as String?,
            entityId: r.data['entity_id'] as String?,
            mutationId: r.data['mutation_id'] as String?,
            latencyMs: r.data['latency_ms'] as int?,
            errorCode: r.data['error_code'] as String?,
            details: r.data['details'] as String?,
            createdAt: r.data['created_at'] as String,
          ),
        )
        .toList();
  }
}
