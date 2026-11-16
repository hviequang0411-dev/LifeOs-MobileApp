import 'dart:convert';

import 'package:drift/drift.dart';
import 'package:uuid/uuid.dart';

import '../sync/entity_types.dart';
import 'database_records.dart';
import 'drift_connection.dart';

/// DAO Drift (`extends DatabaseAccessor<LifeOsDriftDatabase>`) chuyên trách ghi nguyên tử
/// vào bảng thực thể + hợp nhất `outbox` (`MutationCoalescer` — `FLUTTER_RULES.md` §3.3).
class MutationCoalescerDao extends DatabaseAccessor<LifeOsDriftDatabase> {
  MutationCoalescerDao({
    required LifeOsDriftDatabase driftDb,
    required this.runInTransaction,
    required this.notifyTables,
    required this.getEntity,
    required this.listEntities,
    required this.upsertSyncState,
  }) : super(driftDb);

  final T Function<T>(T Function() action, {Set<String> affectedTables})
      runInTransaction;
  final void Function(Iterable<String> tables) notifyTables;
  final LocalEntityRow? Function({
    required String userId,
    required SyncEntityType entityType,
    required String entityId,
    bool includeDeleted,
  }) getEntity;
  final List<LocalEntityRow> Function({
    required String userId,
    required SyncEntityType entityType,
    bool includeDeleted,
  }) listEntities;
  final void Function({
    required String userId,
    int? lastCursor,
    String? lastSyncAt,
    bool? syncPaused,
    String? deviceId,
  }) upsertSyncState;

  final Uuid _uuid = const Uuid();

  /// Thực hiện ghi cục bộ (`CREATE` / `UPDATE` / `DELETE`) vào bảng thực thể
  /// VÀ thêm/gộp mutation vào bảng `outbox` trong cùng 1 transaction nguyên tử.
  ///
  /// Áp dụng đúng 4 quy tắc `MutationCoalescer` (`FLUTTER_RULES.md` §3.3):
  /// 1. `CREATE(pending)` + `UPDATE` -> Giữ `CREATE`, cập nhật payload mới (`baseVersion = 0`).
  /// 2. `CREATE(pending)` + `DELETE` -> Xóa `CREATE` khỏi `outbox` và xóa bản ghi cục bộ.
  /// 3. `UPDATE(pending, baseVersion=V)` + `UPDATE` -> Giữ `UPDATE`, gộp payload mới, giữ nguyên `baseVersion = V`.
  /// 4. `UPDATE(pending, baseVersion=V)` + `DELETE` -> Đổi thành `DELETE`, giữ nguyên `baseVersion = V`.
  OutboxMutationRecord? mutateLocalAndEnqueueOutbox({
    required String userId,
    required SyncEntityType entityType,
    required String entityId,
    required String op,
    required Map<String, dynamic> payload,
    DateTime? nowUtc,
  }) {
    final String normalizedOp = op.toUpperCase();
    if (normalizedOp != 'CREATE' &&
        normalizedOp != 'UPDATE' &&
        normalizedOp != 'DELETE') {
      throw ArgumentError('op không hợp lệ: "$op"');
    }

    final DateTime timestamp = (nowUtc ?? DateTime.now()).toUtc();
    final String isoNow = timestamp.toIso8601String();

    final Map<String, dynamic> normalizedPayload = <String, dynamic>{
      ...payload,
      'id': entityId,
    };
    final String encodedPayload = jsonEncode(normalizedPayload);
    final int utf8Bytes = utf8.encode(encodedPayload).length;
    if (utf8Bytes > entityType.maxPayloadBytes) {
      throw PayloadTooLargeLocalException(
        entityType: entityType,
        actualBytes: utf8Bytes,
        maxBytes: entityType.maxPayloadBytes,
      );
    }

    final Set<String> touchedTables = <String>{
      entityType.tableName,
      'outbox',
      'entity_base_snapshots',
    };
    if (normalizedOp == 'DELETE' &&
        (entityType == SyncEntityType.project ||
            entityType == SyncEntityType.category)) {
      touchedTables.add(SyncEntityType.task.tableName);
    }

    return runInTransaction<OutboxMutationRecord?>(
      affectedTables: touchedTables,
      () {
        final LocalEntityRow? existingEntity = getEntity(
          userId: userId,
          entityType: entityType,
          entityId: entityId,
          includeDeleted: true,
        );
        final int currentVersion = existingEntity?.version ?? 0;

        if (existingEntity != null && existingEntity.version > 0) {
          final List<QueryRow> snapRows = attachedDatabase.selectSql(
            'SELECT base_version FROM entity_base_snapshots '
            'WHERE user_id = ? AND entity_type = ? AND entity_id = ?;',
            <Object?>[userId, entityType.wireName, entityId],
          );
          if (snapRows.isEmpty) {
            attachedDatabase.executeSql(
              '''
              INSERT INTO entity_base_snapshots (
                user_id, entity_type, entity_id, base_version, snapshot_json, updated_at
              ) VALUES (?, ?, ?, ?, ?, ?);
              ''',
              <Object?>[
                userId,
                entityType.wireName,
                entityId,
                existingEntity.version,
                jsonEncode(existingEntity.payload),
                isoNow,
              ],
            );
          }
        }

        final List<QueryRow> pendingRows = attachedDatabase.selectSql(
          '''
          SELECT mutation_id, op, base_version, payload_json, created_at, retry_count
          FROM outbox
          WHERE user_id = ? AND entity_type = ? AND entity_id = ? AND status = 'PENDING'
          ORDER BY created_at DESC LIMIT 1;
          ''',
          <Object?>[userId, entityType.wireName, entityId],
        );

        // Quy tắc 2: CREATE(pending) + DELETE -> triệt tiêu tại chỗ
        if (pendingRows.isNotEmpty &&
            (pendingRows.first.data['op'] as String) == 'CREATE' &&
            normalizedOp == 'DELETE') {
          final String pendingId =
              pendingRows.first.data['mutation_id'] as String;
          attachedDatabase.executeSql(
            'DELETE FROM outbox WHERE mutation_id = ?;',
            <Object?>[pendingId],
          );
          attachedDatabase.executeSql(
            'DELETE FROM ${entityType.tableName} WHERE user_id = ? AND id = ?;',
            <Object?>[userId, entityId],
          );
          _applyLocalCascadeOnDelete(
            userId: userId,
            entityType: entityType,
            entityId: entityId,
            isoNow: isoNow,
          );
          return null;
        }

        if (normalizedOp == 'DELETE') {
          final Map<String, dynamic> tombPayload = <String, dynamic>{
            ...(existingEntity?.payload ?? normalizedPayload),
            'id': entityId,
            'deletedAt': isoNow,
          };
          attachedDatabase.executeSql(
            '''
            INSERT INTO ${entityType.tableName} (id, user_id, version, updated_at, deleted_at, sync_state, payload_json)
            VALUES (?, ?, ?, ?, ?, 'pending', ?)
            ON CONFLICT(user_id, id) DO UPDATE SET
              updated_at = excluded.updated_at,
              deleted_at = excluded.deleted_at,
              sync_state = 'pending',
              payload_json = excluded.payload_json;
            ''',
            <Object?>[
              entityId,
              userId,
              currentVersion,
              isoNow,
              isoNow,
              jsonEncode(tombPayload),
            ],
          );
          _applyLocalCascadeOnDelete(
            userId: userId,
            entityType: entityType,
            entityId: entityId,
            isoNow: isoNow,
          );
        } else {
          final Map<String, dynamic> mergedEntityPayload = <String, dynamic>{
            if (existingEntity != null) ...existingEntity.payload,
            ...normalizedPayload,
            'id': entityId,
            'updatedAt': isoNow,
          };
          attachedDatabase.executeSql(
            '''
            INSERT INTO ${entityType.tableName} (id, user_id, version, updated_at, deleted_at, sync_state, payload_json)
            VALUES (?, ?, ?, ?, NULL, 'pending', ?)
            ON CONFLICT(user_id, id) DO UPDATE SET
              updated_at = excluded.updated_at,
              deleted_at = NULL,
              sync_state = 'pending',
              payload_json = excluded.payload_json;
            ''',
            <Object?>[
              entityId,
              userId,
              currentVersion,
              isoNow,
              jsonEncode(mergedEntityPayload),
            ],
          );
        }

        if (pendingRows.isNotEmpty) {
          final Map<String, dynamic> existingMut = pendingRows.first.data;
          final String mutId = existingMut['mutation_id'] as String;
          final String prevOp = existingMut['op'] as String;
          final int preservedBaseVersion =
              (existingMut['base_version'] as int?) ?? currentVersion;
          final String createdAt = existingMut['created_at'] as String;
          final int retryCount = (existingMut['retry_count'] as int?) ?? 0;
          final Map<String, dynamic> prevPayload =
              (jsonDecode(existingMut['payload_json'] as String)
                      as Map<dynamic, dynamic>)
                  .cast<String, dynamic>();

          late final String coalescedOp;
          late final int coalescedBaseVersion;
          late final Map<String, dynamic> coalescedPayload;

          if (prevOp == 'CREATE' && normalizedOp == 'UPDATE') {
            coalescedOp = 'CREATE';
            coalescedBaseVersion = 0;
            coalescedPayload = <String, dynamic>{
              ...prevPayload,
              ...normalizedPayload,
            };
          } else if (prevOp == 'UPDATE' && normalizedOp == 'UPDATE') {
            coalescedOp = 'UPDATE';
            coalescedBaseVersion = preservedBaseVersion;
            coalescedPayload = <String, dynamic>{
              ...prevPayload,
              ...normalizedPayload,
            };
          } else if (prevOp == 'UPDATE' && normalizedOp == 'DELETE') {
            coalescedOp = 'DELETE';
            coalescedBaseVersion = preservedBaseVersion;
            coalescedPayload = <String, dynamic>{'id': entityId};
          } else {
            coalescedOp = normalizedOp;
            coalescedBaseVersion = preservedBaseVersion;
            coalescedPayload = <String, dynamic>{
              ...prevPayload,
              ...normalizedPayload,
            };
          }

          attachedDatabase.executeSql(
            '''
            UPDATE outbox
            SET op = ?, base_version = ?, payload_json = ?, updated_at = ?
            WHERE mutation_id = ?;
            ''',
            <Object?>[
              coalescedOp,
              coalescedBaseVersion,
              jsonEncode(coalescedPayload),
              isoNow,
              mutId,
            ],
          );

          return OutboxMutationRecord(
            mutationId: mutId,
            userId: userId,
            entityType: entityType,
            entityId: entityId,
            op: coalescedOp,
            baseVersion: coalescedBaseVersion,
            payload: coalescedPayload,
            status: 'PENDING',
            retryCount: retryCount,
            createdAt: createdAt,
            updatedAt: isoNow,
          );
        } else {
          final String newMutationId = _uuid.v4();
          final int baseVersion = normalizedOp == 'CREATE' ? 0 : currentVersion;
          final Map<String, dynamic> finalPayload = normalizedOp == 'DELETE'
              ? <String, dynamic>{'id': entityId}
              : normalizedPayload;

          attachedDatabase.executeSql(
            '''
            INSERT INTO outbox (
              mutation_id, user_id, entity_type, entity_id, op,
              base_version, payload_json, status, retry_count, created_at, updated_at
            ) VALUES (?, ?, ?, ?, ?, ?, ?, 'PENDING', 0, ?, ?);
            ''',
            <Object?>[
              newMutationId,
              userId,
              entityType.wireName,
              entityId,
              normalizedOp,
              baseVersion,
              jsonEncode(finalPayload),
              isoNow,
              isoNow,
            ],
          );

          return OutboxMutationRecord(
            mutationId: newMutationId,
            userId: userId,
            entityType: entityType,
            entityId: entityId,
            op: normalizedOp,
            baseVersion: baseVersion,
            payload: finalPayload,
            status: 'PENDING',
            retryCount: 0,
            createdAt: isoNow,
            updatedAt: isoNow,
          );
        }
      },
    );
  }

  void _applyLocalCascadeOnDelete({
    required String userId,
    required SyncEntityType entityType,
    required String entityId,
    required String isoNow,
  }) {
    if (entityType != SyncEntityType.project &&
        entityType != SyncEntityType.category) {
      return;
    }
    final String fkField =
        entityType == SyncEntityType.project ? 'projectId' : 'categoryId';
    final List<LocalEntityRow> tasks = listEntities(
      userId: userId,
      entityType: SyncEntityType.task,
      includeDeleted: false,
    );
    for (final LocalEntityRow task in tasks) {
      if (task.payload[fkField] == entityId) {
        final Map<String, dynamic> updated = <String, dynamic>{
          ...task.payload,
          fkField: null,
        };
        attachedDatabase.executeSql(
          '''
          UPDATE ${SyncEntityType.task.tableName}
          SET payload_json = ?, updated_at = ?
          WHERE user_id = ? AND id = ?;
          ''',
          <Object?>[jsonEncode(updated), isoNow, userId, task.id],
        );
      }
    }
  }
}
