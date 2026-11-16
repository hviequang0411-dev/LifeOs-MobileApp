import '../sync/entity_types.dart';

/// Mục hàng đợi Outbox cục bộ (`outbox` table) theo đúng `DATABASE_DESIGN.md` §9 & `FLUTTER_RULES.md`.
class OutboxMutationRecord {
  const OutboxMutationRecord({
    required this.mutationId,
    required this.userId,
    required this.entityType,
    required this.entityId,
    required this.op,
    required this.baseVersion,
    required this.payload,
    required this.status,
    required this.retryCount,
    required this.createdAt,
    required this.updatedAt,
    this.lastErrorCode,
    this.lastErrorMessage,
    this.serverConflict,
  });

  final String mutationId;
  final String userId;
  final SyncEntityType entityType;
  final String entityId;
  final String op; // CREATE | UPDATE | DELETE
  final int baseVersion;
  final Map<String, dynamic> payload;
  final String status; // PENDING | IN_FLIGHT | NEEDS_USER_ACTION | FAILED_PERMANENT
  final int retryCount;
  final String createdAt;
  final String updatedAt;
  final String? lastErrorCode;
  final String? lastErrorMessage;
  final Map<String, dynamic>? serverConflict;

  /// Kiểm tra giới hạn tuổi mutation 30 ngày (`SYNC_RULES.md` & `D154`).
  bool isOlderThan30Days(DateTime nowUtc) {
    final DateTime? created = DateTime.tryParse(createdAt)?.toUtc();
    if (created == null) return false;
    return nowUtc.toUtc().difference(created).inDays > 30;
  }

  Map<String, dynamic> toPushWireJson() {
    return <String, dynamic>{
      'mutationId': mutationId,
      'entityType': entityType.wireName,
      'entityId': entityId,
      'operation': op,
      'op': op,
      'baseVersion': baseVersion,
      'clientUpdatedAt': updatedAt,
      'payload': payload,
    };
  }
}

/// Bản ghi trạng thái đồng bộ của từng người dùng (`sync_state`).
class LocalSyncStateRecord {
  const LocalSyncStateRecord({
    required this.userId,
    required this.lastCursor,
    required this.syncPaused,
    this.lastSyncAt,
    this.deviceId,
  });

  final String userId;
  final int lastCursor;
  final bool syncPaused;
  final String? lastSyncAt;
  final String? deviceId;
}

/// Bản ghi nhật ký đồng bộ vòng lặp tối đa 500 dòng (`sync_log`).
class SyncLogRecord {
  const SyncLogRecord({
    required this.id,
    required this.userId,
    required this.direction,
    required this.outcome,
    required this.createdAt,
    this.entityType,
    this.entityId,
    this.mutationId,
    this.latencyMs,
    this.errorCode,
    this.details,
  });

  final int id;
  final String userId;
  final String direction; // PUSH | PULL | SNAPSHOT | RECONCILE
  final String outcome; // APPLIED | DUPLICATE | CONFLICT | REJECTED | SUCCESS | ERROR
  final String createdAt;
  final String? entityType;
  final String? entityId;
  final String? mutationId;
  final int? latencyMs;
  final String? errorCode;
  final String? details;
}

/// Thực thể đồng bộ đã lưu trong SQLite cục bộ (Drift).
class LocalEntityRow {
  const LocalEntityRow({
    required this.id,
    required this.userId,
    required this.entityType,
    required this.version,
    required this.updatedAt,
    required this.payload,
    this.syncState = 'synced',
    this.deletedAt,
  });

  final String id;
  final String userId;
  final SyncEntityType entityType;
  final int version;
  final String updatedAt;
  final String? deletedAt;
  final String syncState; // synced | pending | conflict
  final Map<String, dynamic> payload;

  bool get isDeleted => deletedAt != null && deletedAt!.isNotEmpty;
}

/// Bản ghi lịch thông báo cục bộ đã lên lịch (`scheduled_notifications` — `D133` giới hạn 64 thông báo iOS).
class ScheduledNotificationRecord {
  const ScheduledNotificationRecord({
    required this.dedupeKey,
    required this.userId,
    required this.targetType,
    required this.targetId,
    required this.fireAtUtcIso,
    required this.title,
    required this.body,
    required this.redactedForLockScreen,
  });

  final String dedupeKey;
  final String userId;
  final String targetType;
  final String targetId;
  final String fireAtUtcIso;
  final String title;
  final String body;
  final bool redactedForLockScreen;
}

/// Ngoại lệ khi payload vượt giới hạn dung lượng UTF-8 của giao thức đồng bộ (`D89`, `D180`).
class PayloadTooLargeLocalException implements Exception {
  const PayloadTooLargeLocalException({
    required this.entityType,
    required this.actualBytes,
    required this.maxBytes,
  });

  final SyncEntityType entityType;
  final int actualBytes;
  final int maxBytes;

  @override
  String toString() =>
      'Payload của ${entityType.wireName} vượt giới hạn ($actualBytes / $maxBytes byte)';
}

