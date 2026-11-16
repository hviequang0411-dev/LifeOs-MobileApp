// GENERATED CODE - DO NOT MODIFY BY HAND

part of 'drift_connection.dart';

// ignore_for_file: type=lint
mixin _$DriftSyncInfrastructureDaoMixin
    on DatabaseAccessor<LifeOsDriftDatabase> {
  $SyncStateDriftTableTable get syncStateDriftTable =>
      attachedDatabase.syncStateDriftTable;
  $OutboxDriftTableTable get outboxDriftTable =>
      attachedDatabase.outboxDriftTable;
  $EntityBaseSnapshotDriftTableTable get entityBaseSnapshotDriftTable =>
      attachedDatabase.entityBaseSnapshotDriftTable;
  $SyncLogDriftTableTable get syncLogDriftTable =>
      attachedDatabase.syncLogDriftTable;
  $ScheduledNotificationDriftTableTable get scheduledNotificationDriftTable =>
      attachedDatabase.scheduledNotificationDriftTable;
  $DeliveredNotificationDriftTableTable get deliveredNotificationDriftTable =>
      attachedDatabase.deliveredNotificationDriftTable;
  DriftSyncInfrastructureDaoManager get managers =>
      DriftSyncInfrastructureDaoManager(this);
}

class DriftSyncInfrastructureDaoManager {
  final _$DriftSyncInfrastructureDaoMixin _db;
  DriftSyncInfrastructureDaoManager(this._db);
  $$SyncStateDriftTableTableTableManager get syncStateDriftTable =>
      $$SyncStateDriftTableTableTableManager(
          _db.attachedDatabase, _db.syncStateDriftTable);
  $$OutboxDriftTableTableTableManager get outboxDriftTable =>
      $$OutboxDriftTableTableTableManager(
          _db.attachedDatabase, _db.outboxDriftTable);
  $$EntityBaseSnapshotDriftTableTableTableManager
      get entityBaseSnapshotDriftTable =>
          $$EntityBaseSnapshotDriftTableTableTableManager(
              _db.attachedDatabase, _db.entityBaseSnapshotDriftTable);
  $$SyncLogDriftTableTableTableManager get syncLogDriftTable =>
      $$SyncLogDriftTableTableTableManager(
          _db.attachedDatabase, _db.syncLogDriftTable);
  $$ScheduledNotificationDriftTableTableTableManager
      get scheduledNotificationDriftTable =>
          $$ScheduledNotificationDriftTableTableTableManager(
              _db.attachedDatabase, _db.scheduledNotificationDriftTable);
  $$DeliveredNotificationDriftTableTableTableManager
      get deliveredNotificationDriftTable =>
          $$DeliveredNotificationDriftTableTableTableManager(
              _db.attachedDatabase, _db.deliveredNotificationDriftTable);
}

class $SyncStateDriftTableTable extends SyncStateDriftTable
    with TableInfo<$SyncStateDriftTableTable, SyncStateDriftTableData> {
  @override
  final GeneratedDatabase attachedDatabase;
  final String? _alias;
  $SyncStateDriftTableTable(this.attachedDatabase, [this._alias]);
  static const VerificationMeta _userIdMeta = const VerificationMeta('userId');
  @override
  late final GeneratedColumn<String> userId = GeneratedColumn<String>(
      'user_id', aliasedName, false,
      type: DriftSqlType.string, requiredDuringInsert: true);
  static const VerificationMeta _lastCursorMeta =
      const VerificationMeta('lastCursor');
  @override
  late final GeneratedColumn<int> lastCursor = GeneratedColumn<int>(
      'last_cursor', aliasedName, false,
      type: DriftSqlType.int,
      requiredDuringInsert: false,
      defaultValue: const Constant(0));
  static const VerificationMeta _lastSyncAtMeta =
      const VerificationMeta('lastSyncAt');
  @override
  late final GeneratedColumn<String> lastSyncAt = GeneratedColumn<String>(
      'last_sync_at', aliasedName, true,
      type: DriftSqlType.string, requiredDuringInsert: false);
  static const VerificationMeta _syncPausedMeta =
      const VerificationMeta('syncPaused');
  @override
  late final GeneratedColumn<int> syncPaused = GeneratedColumn<int>(
      'sync_paused', aliasedName, false,
      type: DriftSqlType.int,
      requiredDuringInsert: false,
      defaultValue: const Constant(0));
  static const VerificationMeta _deviceIdMeta =
      const VerificationMeta('deviceId');
  @override
  late final GeneratedColumn<String> deviceId = GeneratedColumn<String>(
      'device_id', aliasedName, true,
      type: DriftSqlType.string, requiredDuringInsert: false);
  @override
  List<GeneratedColumn> get $columns =>
      [userId, lastCursor, lastSyncAt, syncPaused, deviceId];
  @override
  String get aliasedName => _alias ?? actualTableName;
  @override
  String get actualTableName => $name;
  static const String $name = 'sync_state';
  @override
  VerificationContext validateIntegrity(
      Insertable<SyncStateDriftTableData> instance,
      {bool isInserting = false}) {
    final context = VerificationContext();
    final data = instance.toColumns(true);
    if (data.containsKey('user_id')) {
      context.handle(_userIdMeta,
          userId.isAcceptableOrUnknown(data['user_id']!, _userIdMeta));
    } else if (isInserting) {
      context.missing(_userIdMeta);
    }
    if (data.containsKey('last_cursor')) {
      context.handle(
          _lastCursorMeta,
          lastCursor.isAcceptableOrUnknown(
              data['last_cursor']!, _lastCursorMeta));
    }
    if (data.containsKey('last_sync_at')) {
      context.handle(
          _lastSyncAtMeta,
          lastSyncAt.isAcceptableOrUnknown(
              data['last_sync_at']!, _lastSyncAtMeta));
    }
    if (data.containsKey('sync_paused')) {
      context.handle(
          _syncPausedMeta,
          syncPaused.isAcceptableOrUnknown(
              data['sync_paused']!, _syncPausedMeta));
    }
    if (data.containsKey('device_id')) {
      context.handle(_deviceIdMeta,
          deviceId.isAcceptableOrUnknown(data['device_id']!, _deviceIdMeta));
    }
    return context;
  }

  @override
  Set<GeneratedColumn> get $primaryKey => {userId};
  @override
  SyncStateDriftTableData map(Map<String, dynamic> data,
      {String? tablePrefix}) {
    final effectivePrefix = tablePrefix != null ? '$tablePrefix.' : '';
    return SyncStateDriftTableData(
      userId: attachedDatabase.typeMapping
          .read(DriftSqlType.string, data['${effectivePrefix}user_id'])!,
      lastCursor: attachedDatabase.typeMapping
          .read(DriftSqlType.int, data['${effectivePrefix}last_cursor'])!,
      lastSyncAt: attachedDatabase.typeMapping
          .read(DriftSqlType.string, data['${effectivePrefix}last_sync_at']),
      syncPaused: attachedDatabase.typeMapping
          .read(DriftSqlType.int, data['${effectivePrefix}sync_paused'])!,
      deviceId: attachedDatabase.typeMapping
          .read(DriftSqlType.string, data['${effectivePrefix}device_id']),
    );
  }

  @override
  $SyncStateDriftTableTable createAlias(String alias) {
    return $SyncStateDriftTableTable(attachedDatabase, alias);
  }
}

class SyncStateDriftTableData extends DataClass
    implements Insertable<SyncStateDriftTableData> {
  final String userId;
  final int lastCursor;
  final String? lastSyncAt;
  final int syncPaused;
  final String? deviceId;
  const SyncStateDriftTableData(
      {required this.userId,
      required this.lastCursor,
      this.lastSyncAt,
      required this.syncPaused,
      this.deviceId});
  @override
  Map<String, Expression> toColumns(bool nullToAbsent) {
    final map = <String, Expression>{};
    map['user_id'] = Variable<String>(userId);
    map['last_cursor'] = Variable<int>(lastCursor);
    if (!nullToAbsent || lastSyncAt != null) {
      map['last_sync_at'] = Variable<String>(lastSyncAt);
    }
    map['sync_paused'] = Variable<int>(syncPaused);
    if (!nullToAbsent || deviceId != null) {
      map['device_id'] = Variable<String>(deviceId);
    }
    return map;
  }

  SyncStateDriftTableCompanion toCompanion(bool nullToAbsent) {
    return SyncStateDriftTableCompanion(
      userId: Value(userId),
      lastCursor: Value(lastCursor),
      lastSyncAt: lastSyncAt == null && nullToAbsent
          ? const Value.absent()
          : Value(lastSyncAt),
      syncPaused: Value(syncPaused),
      deviceId: deviceId == null && nullToAbsent
          ? const Value.absent()
          : Value(deviceId),
    );
  }

  factory SyncStateDriftTableData.fromJson(Map<String, dynamic> json,
      {ValueSerializer? serializer}) {
    serializer ??= driftRuntimeOptions.defaultSerializer;
    return SyncStateDriftTableData(
      userId: serializer.fromJson<String>(json['userId']),
      lastCursor: serializer.fromJson<int>(json['lastCursor']),
      lastSyncAt: serializer.fromJson<String?>(json['lastSyncAt']),
      syncPaused: serializer.fromJson<int>(json['syncPaused']),
      deviceId: serializer.fromJson<String?>(json['deviceId']),
    );
  }
  @override
  Map<String, dynamic> toJson({ValueSerializer? serializer}) {
    serializer ??= driftRuntimeOptions.defaultSerializer;
    return <String, dynamic>{
      'userId': serializer.toJson<String>(userId),
      'lastCursor': serializer.toJson<int>(lastCursor),
      'lastSyncAt': serializer.toJson<String?>(lastSyncAt),
      'syncPaused': serializer.toJson<int>(syncPaused),
      'deviceId': serializer.toJson<String?>(deviceId),
    };
  }

  SyncStateDriftTableData copyWith(
          {String? userId,
          int? lastCursor,
          Value<String?> lastSyncAt = const Value.absent(),
          int? syncPaused,
          Value<String?> deviceId = const Value.absent()}) =>
      SyncStateDriftTableData(
        userId: userId ?? this.userId,
        lastCursor: lastCursor ?? this.lastCursor,
        lastSyncAt: lastSyncAt.present ? lastSyncAt.value : this.lastSyncAt,
        syncPaused: syncPaused ?? this.syncPaused,
        deviceId: deviceId.present ? deviceId.value : this.deviceId,
      );
  SyncStateDriftTableData copyWithCompanion(SyncStateDriftTableCompanion data) {
    return SyncStateDriftTableData(
      userId: data.userId.present ? data.userId.value : this.userId,
      lastCursor:
          data.lastCursor.present ? data.lastCursor.value : this.lastCursor,
      lastSyncAt:
          data.lastSyncAt.present ? data.lastSyncAt.value : this.lastSyncAt,
      syncPaused:
          data.syncPaused.present ? data.syncPaused.value : this.syncPaused,
      deviceId: data.deviceId.present ? data.deviceId.value : this.deviceId,
    );
  }

  @override
  String toString() {
    return (StringBuffer('SyncStateDriftTableData(')
          ..write('userId: $userId, ')
          ..write('lastCursor: $lastCursor, ')
          ..write('lastSyncAt: $lastSyncAt, ')
          ..write('syncPaused: $syncPaused, ')
          ..write('deviceId: $deviceId')
          ..write(')'))
        .toString();
  }

  @override
  int get hashCode =>
      Object.hash(userId, lastCursor, lastSyncAt, syncPaused, deviceId);
  @override
  bool operator ==(Object other) =>
      identical(this, other) ||
      (other is SyncStateDriftTableData &&
          other.userId == this.userId &&
          other.lastCursor == this.lastCursor &&
          other.lastSyncAt == this.lastSyncAt &&
          other.syncPaused == this.syncPaused &&
          other.deviceId == this.deviceId);
}

class SyncStateDriftTableCompanion
    extends UpdateCompanion<SyncStateDriftTableData> {
  final Value<String> userId;
  final Value<int> lastCursor;
  final Value<String?> lastSyncAt;
  final Value<int> syncPaused;
  final Value<String?> deviceId;
  final Value<int> rowid;
  const SyncStateDriftTableCompanion({
    this.userId = const Value.absent(),
    this.lastCursor = const Value.absent(),
    this.lastSyncAt = const Value.absent(),
    this.syncPaused = const Value.absent(),
    this.deviceId = const Value.absent(),
    this.rowid = const Value.absent(),
  });
  SyncStateDriftTableCompanion.insert({
    required String userId,
    this.lastCursor = const Value.absent(),
    this.lastSyncAt = const Value.absent(),
    this.syncPaused = const Value.absent(),
    this.deviceId = const Value.absent(),
    this.rowid = const Value.absent(),
  }) : userId = Value(userId);
  static Insertable<SyncStateDriftTableData> custom({
    Expression<String>? userId,
    Expression<int>? lastCursor,
    Expression<String>? lastSyncAt,
    Expression<int>? syncPaused,
    Expression<String>? deviceId,
    Expression<int>? rowid,
  }) {
    return RawValuesInsertable({
      if (userId != null) 'user_id': userId,
      if (lastCursor != null) 'last_cursor': lastCursor,
      if (lastSyncAt != null) 'last_sync_at': lastSyncAt,
      if (syncPaused != null) 'sync_paused': syncPaused,
      if (deviceId != null) 'device_id': deviceId,
      if (rowid != null) 'rowid': rowid,
    });
  }

  SyncStateDriftTableCompanion copyWith(
      {Value<String>? userId,
      Value<int>? lastCursor,
      Value<String?>? lastSyncAt,
      Value<int>? syncPaused,
      Value<String?>? deviceId,
      Value<int>? rowid}) {
    return SyncStateDriftTableCompanion(
      userId: userId ?? this.userId,
      lastCursor: lastCursor ?? this.lastCursor,
      lastSyncAt: lastSyncAt ?? this.lastSyncAt,
      syncPaused: syncPaused ?? this.syncPaused,
      deviceId: deviceId ?? this.deviceId,
      rowid: rowid ?? this.rowid,
    );
  }

  @override
  Map<String, Expression> toColumns(bool nullToAbsent) {
    final map = <String, Expression>{};
    if (userId.present) {
      map['user_id'] = Variable<String>(userId.value);
    }
    if (lastCursor.present) {
      map['last_cursor'] = Variable<int>(lastCursor.value);
    }
    if (lastSyncAt.present) {
      map['last_sync_at'] = Variable<String>(lastSyncAt.value);
    }
    if (syncPaused.present) {
      map['sync_paused'] = Variable<int>(syncPaused.value);
    }
    if (deviceId.present) {
      map['device_id'] = Variable<String>(deviceId.value);
    }
    if (rowid.present) {
      map['rowid'] = Variable<int>(rowid.value);
    }
    return map;
  }

  @override
  String toString() {
    return (StringBuffer('SyncStateDriftTableCompanion(')
          ..write('userId: $userId, ')
          ..write('lastCursor: $lastCursor, ')
          ..write('lastSyncAt: $lastSyncAt, ')
          ..write('syncPaused: $syncPaused, ')
          ..write('deviceId: $deviceId, ')
          ..write('rowid: $rowid')
          ..write(')'))
        .toString();
  }
}

class $OutboxDriftTableTable extends OutboxDriftTable
    with TableInfo<$OutboxDriftTableTable, OutboxDriftTableData> {
  @override
  final GeneratedDatabase attachedDatabase;
  final String? _alias;
  $OutboxDriftTableTable(this.attachedDatabase, [this._alias]);
  static const VerificationMeta _mutationIdMeta =
      const VerificationMeta('mutationId');
  @override
  late final GeneratedColumn<String> mutationId = GeneratedColumn<String>(
      'mutation_id', aliasedName, false,
      type: DriftSqlType.string, requiredDuringInsert: true);
  static const VerificationMeta _userIdMeta = const VerificationMeta('userId');
  @override
  late final GeneratedColumn<String> userId = GeneratedColumn<String>(
      'user_id', aliasedName, false,
      type: DriftSqlType.string, requiredDuringInsert: true);
  static const VerificationMeta _entityTypeMeta =
      const VerificationMeta('entityType');
  @override
  late final GeneratedColumn<String> entityType = GeneratedColumn<String>(
      'entity_type', aliasedName, false,
      type: DriftSqlType.string, requiredDuringInsert: true);
  static const VerificationMeta _entityIdMeta =
      const VerificationMeta('entityId');
  @override
  late final GeneratedColumn<String> entityId = GeneratedColumn<String>(
      'entity_id', aliasedName, false,
      type: DriftSqlType.string, requiredDuringInsert: true);
  static const VerificationMeta _opMeta = const VerificationMeta('op');
  @override
  late final GeneratedColumn<String> op = GeneratedColumn<String>(
      'op', aliasedName, false,
      type: DriftSqlType.string, requiredDuringInsert: true);
  static const VerificationMeta _baseVersionMeta =
      const VerificationMeta('baseVersion');
  @override
  late final GeneratedColumn<int> baseVersion = GeneratedColumn<int>(
      'base_version', aliasedName, false,
      type: DriftSqlType.int,
      requiredDuringInsert: false,
      defaultValue: const Constant(0));
  static const VerificationMeta _payloadJsonMeta =
      const VerificationMeta('payloadJson');
  @override
  late final GeneratedColumn<String> payloadJson = GeneratedColumn<String>(
      'payload_json', aliasedName, false,
      type: DriftSqlType.string, requiredDuringInsert: true);
  static const VerificationMeta _statusMeta = const VerificationMeta('status');
  @override
  late final GeneratedColumn<String> status = GeneratedColumn<String>(
      'status', aliasedName, false,
      type: DriftSqlType.string,
      requiredDuringInsert: false,
      defaultValue: const Constant('PENDING'));
  static const VerificationMeta _retryCountMeta =
      const VerificationMeta('retryCount');
  @override
  late final GeneratedColumn<int> retryCount = GeneratedColumn<int>(
      'retry_count', aliasedName, false,
      type: DriftSqlType.int,
      requiredDuringInsert: false,
      defaultValue: const Constant(0));
  static const VerificationMeta _lastErrorCodeMeta =
      const VerificationMeta('lastErrorCode');
  @override
  late final GeneratedColumn<String> lastErrorCode = GeneratedColumn<String>(
      'last_error_code', aliasedName, true,
      type: DriftSqlType.string, requiredDuringInsert: false);
  static const VerificationMeta _lastErrorMessageMeta =
      const VerificationMeta('lastErrorMessage');
  @override
  late final GeneratedColumn<String> lastErrorMessage = GeneratedColumn<String>(
      'last_error_message', aliasedName, true,
      type: DriftSqlType.string, requiredDuringInsert: false);
  static const VerificationMeta _serverConflictJsonMeta =
      const VerificationMeta('serverConflictJson');
  @override
  late final GeneratedColumn<String> serverConflictJson =
      GeneratedColumn<String>('server_conflict_json', aliasedName, true,
          type: DriftSqlType.string, requiredDuringInsert: false);
  static const VerificationMeta _createdAtMeta =
      const VerificationMeta('createdAt');
  @override
  late final GeneratedColumn<String> createdAt = GeneratedColumn<String>(
      'created_at', aliasedName, false,
      type: DriftSqlType.string, requiredDuringInsert: true);
  static const VerificationMeta _updatedAtMeta =
      const VerificationMeta('updatedAt');
  @override
  late final GeneratedColumn<String> updatedAt = GeneratedColumn<String>(
      'updated_at', aliasedName, false,
      type: DriftSqlType.string, requiredDuringInsert: true);
  @override
  List<GeneratedColumn> get $columns => [
        mutationId,
        userId,
        entityType,
        entityId,
        op,
        baseVersion,
        payloadJson,
        status,
        retryCount,
        lastErrorCode,
        lastErrorMessage,
        serverConflictJson,
        createdAt,
        updatedAt
      ];
  @override
  String get aliasedName => _alias ?? actualTableName;
  @override
  String get actualTableName => $name;
  static const String $name = 'outbox';
  @override
  VerificationContext validateIntegrity(
      Insertable<OutboxDriftTableData> instance,
      {bool isInserting = false}) {
    final context = VerificationContext();
    final data = instance.toColumns(true);
    if (data.containsKey('mutation_id')) {
      context.handle(
          _mutationIdMeta,
          mutationId.isAcceptableOrUnknown(
              data['mutation_id']!, _mutationIdMeta));
    } else if (isInserting) {
      context.missing(_mutationIdMeta);
    }
    if (data.containsKey('user_id')) {
      context.handle(_userIdMeta,
          userId.isAcceptableOrUnknown(data['user_id']!, _userIdMeta));
    } else if (isInserting) {
      context.missing(_userIdMeta);
    }
    if (data.containsKey('entity_type')) {
      context.handle(
          _entityTypeMeta,
          entityType.isAcceptableOrUnknown(
              data['entity_type']!, _entityTypeMeta));
    } else if (isInserting) {
      context.missing(_entityTypeMeta);
    }
    if (data.containsKey('entity_id')) {
      context.handle(_entityIdMeta,
          entityId.isAcceptableOrUnknown(data['entity_id']!, _entityIdMeta));
    } else if (isInserting) {
      context.missing(_entityIdMeta);
    }
    if (data.containsKey('op')) {
      context.handle(_opMeta, op.isAcceptableOrUnknown(data['op']!, _opMeta));
    } else if (isInserting) {
      context.missing(_opMeta);
    }
    if (data.containsKey('base_version')) {
      context.handle(
          _baseVersionMeta,
          baseVersion.isAcceptableOrUnknown(
              data['base_version']!, _baseVersionMeta));
    }
    if (data.containsKey('payload_json')) {
      context.handle(
          _payloadJsonMeta,
          payloadJson.isAcceptableOrUnknown(
              data['payload_json']!, _payloadJsonMeta));
    } else if (isInserting) {
      context.missing(_payloadJsonMeta);
    }
    if (data.containsKey('status')) {
      context.handle(_statusMeta,
          status.isAcceptableOrUnknown(data['status']!, _statusMeta));
    }
    if (data.containsKey('retry_count')) {
      context.handle(
          _retryCountMeta,
          retryCount.isAcceptableOrUnknown(
              data['retry_count']!, _retryCountMeta));
    }
    if (data.containsKey('last_error_code')) {
      context.handle(
          _lastErrorCodeMeta,
          lastErrorCode.isAcceptableOrUnknown(
              data['last_error_code']!, _lastErrorCodeMeta));
    }
    if (data.containsKey('last_error_message')) {
      context.handle(
          _lastErrorMessageMeta,
          lastErrorMessage.isAcceptableOrUnknown(
              data['last_error_message']!, _lastErrorMessageMeta));
    }
    if (data.containsKey('server_conflict_json')) {
      context.handle(
          _serverConflictJsonMeta,
          serverConflictJson.isAcceptableOrUnknown(
              data['server_conflict_json']!, _serverConflictJsonMeta));
    }
    if (data.containsKey('created_at')) {
      context.handle(_createdAtMeta,
          createdAt.isAcceptableOrUnknown(data['created_at']!, _createdAtMeta));
    } else if (isInserting) {
      context.missing(_createdAtMeta);
    }
    if (data.containsKey('updated_at')) {
      context.handle(_updatedAtMeta,
          updatedAt.isAcceptableOrUnknown(data['updated_at']!, _updatedAtMeta));
    } else if (isInserting) {
      context.missing(_updatedAtMeta);
    }
    return context;
  }

  @override
  Set<GeneratedColumn> get $primaryKey => {mutationId};
  @override
  OutboxDriftTableData map(Map<String, dynamic> data, {String? tablePrefix}) {
    final effectivePrefix = tablePrefix != null ? '$tablePrefix.' : '';
    return OutboxDriftTableData(
      mutationId: attachedDatabase.typeMapping
          .read(DriftSqlType.string, data['${effectivePrefix}mutation_id'])!,
      userId: attachedDatabase.typeMapping
          .read(DriftSqlType.string, data['${effectivePrefix}user_id'])!,
      entityType: attachedDatabase.typeMapping
          .read(DriftSqlType.string, data['${effectivePrefix}entity_type'])!,
      entityId: attachedDatabase.typeMapping
          .read(DriftSqlType.string, data['${effectivePrefix}entity_id'])!,
      op: attachedDatabase.typeMapping
          .read(DriftSqlType.string, data['${effectivePrefix}op'])!,
      baseVersion: attachedDatabase.typeMapping
          .read(DriftSqlType.int, data['${effectivePrefix}base_version'])!,
      payloadJson: attachedDatabase.typeMapping
          .read(DriftSqlType.string, data['${effectivePrefix}payload_json'])!,
      status: attachedDatabase.typeMapping
          .read(DriftSqlType.string, data['${effectivePrefix}status'])!,
      retryCount: attachedDatabase.typeMapping
          .read(DriftSqlType.int, data['${effectivePrefix}retry_count'])!,
      lastErrorCode: attachedDatabase.typeMapping
          .read(DriftSqlType.string, data['${effectivePrefix}last_error_code']),
      lastErrorMessage: attachedDatabase.typeMapping.read(
          DriftSqlType.string, data['${effectivePrefix}last_error_message']),
      serverConflictJson: attachedDatabase.typeMapping.read(
          DriftSqlType.string, data['${effectivePrefix}server_conflict_json']),
      createdAt: attachedDatabase.typeMapping
          .read(DriftSqlType.string, data['${effectivePrefix}created_at'])!,
      updatedAt: attachedDatabase.typeMapping
          .read(DriftSqlType.string, data['${effectivePrefix}updated_at'])!,
    );
  }

  @override
  $OutboxDriftTableTable createAlias(String alias) {
    return $OutboxDriftTableTable(attachedDatabase, alias);
  }
}

class OutboxDriftTableData extends DataClass
    implements Insertable<OutboxDriftTableData> {
  final String mutationId;
  final String userId;
  final String entityType;
  final String entityId;
  final String op;
  final int baseVersion;
  final String payloadJson;
  final String status;
  final int retryCount;
  final String? lastErrorCode;
  final String? lastErrorMessage;
  final String? serverConflictJson;
  final String createdAt;
  final String updatedAt;
  const OutboxDriftTableData(
      {required this.mutationId,
      required this.userId,
      required this.entityType,
      required this.entityId,
      required this.op,
      required this.baseVersion,
      required this.payloadJson,
      required this.status,
      required this.retryCount,
      this.lastErrorCode,
      this.lastErrorMessage,
      this.serverConflictJson,
      required this.createdAt,
      required this.updatedAt});
  @override
  Map<String, Expression> toColumns(bool nullToAbsent) {
    final map = <String, Expression>{};
    map['mutation_id'] = Variable<String>(mutationId);
    map['user_id'] = Variable<String>(userId);
    map['entity_type'] = Variable<String>(entityType);
    map['entity_id'] = Variable<String>(entityId);
    map['op'] = Variable<String>(op);
    map['base_version'] = Variable<int>(baseVersion);
    map['payload_json'] = Variable<String>(payloadJson);
    map['status'] = Variable<String>(status);
    map['retry_count'] = Variable<int>(retryCount);
    if (!nullToAbsent || lastErrorCode != null) {
      map['last_error_code'] = Variable<String>(lastErrorCode);
    }
    if (!nullToAbsent || lastErrorMessage != null) {
      map['last_error_message'] = Variable<String>(lastErrorMessage);
    }
    if (!nullToAbsent || serverConflictJson != null) {
      map['server_conflict_json'] = Variable<String>(serverConflictJson);
    }
    map['created_at'] = Variable<String>(createdAt);
    map['updated_at'] = Variable<String>(updatedAt);
    return map;
  }

  OutboxDriftTableCompanion toCompanion(bool nullToAbsent) {
    return OutboxDriftTableCompanion(
      mutationId: Value(mutationId),
      userId: Value(userId),
      entityType: Value(entityType),
      entityId: Value(entityId),
      op: Value(op),
      baseVersion: Value(baseVersion),
      payloadJson: Value(payloadJson),
      status: Value(status),
      retryCount: Value(retryCount),
      lastErrorCode: lastErrorCode == null && nullToAbsent
          ? const Value.absent()
          : Value(lastErrorCode),
      lastErrorMessage: lastErrorMessage == null && nullToAbsent
          ? const Value.absent()
          : Value(lastErrorMessage),
      serverConflictJson: serverConflictJson == null && nullToAbsent
          ? const Value.absent()
          : Value(serverConflictJson),
      createdAt: Value(createdAt),
      updatedAt: Value(updatedAt),
    );
  }

  factory OutboxDriftTableData.fromJson(Map<String, dynamic> json,
      {ValueSerializer? serializer}) {
    serializer ??= driftRuntimeOptions.defaultSerializer;
    return OutboxDriftTableData(
      mutationId: serializer.fromJson<String>(json['mutationId']),
      userId: serializer.fromJson<String>(json['userId']),
      entityType: serializer.fromJson<String>(json['entityType']),
      entityId: serializer.fromJson<String>(json['entityId']),
      op: serializer.fromJson<String>(json['op']),
      baseVersion: serializer.fromJson<int>(json['baseVersion']),
      payloadJson: serializer.fromJson<String>(json['payloadJson']),
      status: serializer.fromJson<String>(json['status']),
      retryCount: serializer.fromJson<int>(json['retryCount']),
      lastErrorCode: serializer.fromJson<String?>(json['lastErrorCode']),
      lastErrorMessage: serializer.fromJson<String?>(json['lastErrorMessage']),
      serverConflictJson:
          serializer.fromJson<String?>(json['serverConflictJson']),
      createdAt: serializer.fromJson<String>(json['createdAt']),
      updatedAt: serializer.fromJson<String>(json['updatedAt']),
    );
  }
  @override
  Map<String, dynamic> toJson({ValueSerializer? serializer}) {
    serializer ??= driftRuntimeOptions.defaultSerializer;
    return <String, dynamic>{
      'mutationId': serializer.toJson<String>(mutationId),
      'userId': serializer.toJson<String>(userId),
      'entityType': serializer.toJson<String>(entityType),
      'entityId': serializer.toJson<String>(entityId),
      'op': serializer.toJson<String>(op),
      'baseVersion': serializer.toJson<int>(baseVersion),
      'payloadJson': serializer.toJson<String>(payloadJson),
      'status': serializer.toJson<String>(status),
      'retryCount': serializer.toJson<int>(retryCount),
      'lastErrorCode': serializer.toJson<String?>(lastErrorCode),
      'lastErrorMessage': serializer.toJson<String?>(lastErrorMessage),
      'serverConflictJson': serializer.toJson<String?>(serverConflictJson),
      'createdAt': serializer.toJson<String>(createdAt),
      'updatedAt': serializer.toJson<String>(updatedAt),
    };
  }

  OutboxDriftTableData copyWith(
          {String? mutationId,
          String? userId,
          String? entityType,
          String? entityId,
          String? op,
          int? baseVersion,
          String? payloadJson,
          String? status,
          int? retryCount,
          Value<String?> lastErrorCode = const Value.absent(),
          Value<String?> lastErrorMessage = const Value.absent(),
          Value<String?> serverConflictJson = const Value.absent(),
          String? createdAt,
          String? updatedAt}) =>
      OutboxDriftTableData(
        mutationId: mutationId ?? this.mutationId,
        userId: userId ?? this.userId,
        entityType: entityType ?? this.entityType,
        entityId: entityId ?? this.entityId,
        op: op ?? this.op,
        baseVersion: baseVersion ?? this.baseVersion,
        payloadJson: payloadJson ?? this.payloadJson,
        status: status ?? this.status,
        retryCount: retryCount ?? this.retryCount,
        lastErrorCode:
            lastErrorCode.present ? lastErrorCode.value : this.lastErrorCode,
        lastErrorMessage: lastErrorMessage.present
            ? lastErrorMessage.value
            : this.lastErrorMessage,
        serverConflictJson: serverConflictJson.present
            ? serverConflictJson.value
            : this.serverConflictJson,
        createdAt: createdAt ?? this.createdAt,
        updatedAt: updatedAt ?? this.updatedAt,
      );
  OutboxDriftTableData copyWithCompanion(OutboxDriftTableCompanion data) {
    return OutboxDriftTableData(
      mutationId:
          data.mutationId.present ? data.mutationId.value : this.mutationId,
      userId: data.userId.present ? data.userId.value : this.userId,
      entityType:
          data.entityType.present ? data.entityType.value : this.entityType,
      entityId: data.entityId.present ? data.entityId.value : this.entityId,
      op: data.op.present ? data.op.value : this.op,
      baseVersion:
          data.baseVersion.present ? data.baseVersion.value : this.baseVersion,
      payloadJson:
          data.payloadJson.present ? data.payloadJson.value : this.payloadJson,
      status: data.status.present ? data.status.value : this.status,
      retryCount:
          data.retryCount.present ? data.retryCount.value : this.retryCount,
      lastErrorCode: data.lastErrorCode.present
          ? data.lastErrorCode.value
          : this.lastErrorCode,
      lastErrorMessage: data.lastErrorMessage.present
          ? data.lastErrorMessage.value
          : this.lastErrorMessage,
      serverConflictJson: data.serverConflictJson.present
          ? data.serverConflictJson.value
          : this.serverConflictJson,
      createdAt: data.createdAt.present ? data.createdAt.value : this.createdAt,
      updatedAt: data.updatedAt.present ? data.updatedAt.value : this.updatedAt,
    );
  }

  @override
  String toString() {
    return (StringBuffer('OutboxDriftTableData(')
          ..write('mutationId: $mutationId, ')
          ..write('userId: $userId, ')
          ..write('entityType: $entityType, ')
          ..write('entityId: $entityId, ')
          ..write('op: $op, ')
          ..write('baseVersion: $baseVersion, ')
          ..write('payloadJson: $payloadJson, ')
          ..write('status: $status, ')
          ..write('retryCount: $retryCount, ')
          ..write('lastErrorCode: $lastErrorCode, ')
          ..write('lastErrorMessage: $lastErrorMessage, ')
          ..write('serverConflictJson: $serverConflictJson, ')
          ..write('createdAt: $createdAt, ')
          ..write('updatedAt: $updatedAt')
          ..write(')'))
        .toString();
  }

  @override
  int get hashCode => Object.hash(
      mutationId,
      userId,
      entityType,
      entityId,
      op,
      baseVersion,
      payloadJson,
      status,
      retryCount,
      lastErrorCode,
      lastErrorMessage,
      serverConflictJson,
      createdAt,
      updatedAt);
  @override
  bool operator ==(Object other) =>
      identical(this, other) ||
      (other is OutboxDriftTableData &&
          other.mutationId == this.mutationId &&
          other.userId == this.userId &&
          other.entityType == this.entityType &&
          other.entityId == this.entityId &&
          other.op == this.op &&
          other.baseVersion == this.baseVersion &&
          other.payloadJson == this.payloadJson &&
          other.status == this.status &&
          other.retryCount == this.retryCount &&
          other.lastErrorCode == this.lastErrorCode &&
          other.lastErrorMessage == this.lastErrorMessage &&
          other.serverConflictJson == this.serverConflictJson &&
          other.createdAt == this.createdAt &&
          other.updatedAt == this.updatedAt);
}

class OutboxDriftTableCompanion extends UpdateCompanion<OutboxDriftTableData> {
  final Value<String> mutationId;
  final Value<String> userId;
  final Value<String> entityType;
  final Value<String> entityId;
  final Value<String> op;
  final Value<int> baseVersion;
  final Value<String> payloadJson;
  final Value<String> status;
  final Value<int> retryCount;
  final Value<String?> lastErrorCode;
  final Value<String?> lastErrorMessage;
  final Value<String?> serverConflictJson;
  final Value<String> createdAt;
  final Value<String> updatedAt;
  final Value<int> rowid;
  const OutboxDriftTableCompanion({
    this.mutationId = const Value.absent(),
    this.userId = const Value.absent(),
    this.entityType = const Value.absent(),
    this.entityId = const Value.absent(),
    this.op = const Value.absent(),
    this.baseVersion = const Value.absent(),
    this.payloadJson = const Value.absent(),
    this.status = const Value.absent(),
    this.retryCount = const Value.absent(),
    this.lastErrorCode = const Value.absent(),
    this.lastErrorMessage = const Value.absent(),
    this.serverConflictJson = const Value.absent(),
    this.createdAt = const Value.absent(),
    this.updatedAt = const Value.absent(),
    this.rowid = const Value.absent(),
  });
  OutboxDriftTableCompanion.insert({
    required String mutationId,
    required String userId,
    required String entityType,
    required String entityId,
    required String op,
    this.baseVersion = const Value.absent(),
    required String payloadJson,
    this.status = const Value.absent(),
    this.retryCount = const Value.absent(),
    this.lastErrorCode = const Value.absent(),
    this.lastErrorMessage = const Value.absent(),
    this.serverConflictJson = const Value.absent(),
    required String createdAt,
    required String updatedAt,
    this.rowid = const Value.absent(),
  })  : mutationId = Value(mutationId),
        userId = Value(userId),
        entityType = Value(entityType),
        entityId = Value(entityId),
        op = Value(op),
        payloadJson = Value(payloadJson),
        createdAt = Value(createdAt),
        updatedAt = Value(updatedAt);
  static Insertable<OutboxDriftTableData> custom({
    Expression<String>? mutationId,
    Expression<String>? userId,
    Expression<String>? entityType,
    Expression<String>? entityId,
    Expression<String>? op,
    Expression<int>? baseVersion,
    Expression<String>? payloadJson,
    Expression<String>? status,
    Expression<int>? retryCount,
    Expression<String>? lastErrorCode,
    Expression<String>? lastErrorMessage,
    Expression<String>? serverConflictJson,
    Expression<String>? createdAt,
    Expression<String>? updatedAt,
    Expression<int>? rowid,
  }) {
    return RawValuesInsertable({
      if (mutationId != null) 'mutation_id': mutationId,
      if (userId != null) 'user_id': userId,
      if (entityType != null) 'entity_type': entityType,
      if (entityId != null) 'entity_id': entityId,
      if (op != null) 'op': op,
      if (baseVersion != null) 'base_version': baseVersion,
      if (payloadJson != null) 'payload_json': payloadJson,
      if (status != null) 'status': status,
      if (retryCount != null) 'retry_count': retryCount,
      if (lastErrorCode != null) 'last_error_code': lastErrorCode,
      if (lastErrorMessage != null) 'last_error_message': lastErrorMessage,
      if (serverConflictJson != null)
        'server_conflict_json': serverConflictJson,
      if (createdAt != null) 'created_at': createdAt,
      if (updatedAt != null) 'updated_at': updatedAt,
      if (rowid != null) 'rowid': rowid,
    });
  }

  OutboxDriftTableCompanion copyWith(
      {Value<String>? mutationId,
      Value<String>? userId,
      Value<String>? entityType,
      Value<String>? entityId,
      Value<String>? op,
      Value<int>? baseVersion,
      Value<String>? payloadJson,
      Value<String>? status,
      Value<int>? retryCount,
      Value<String?>? lastErrorCode,
      Value<String?>? lastErrorMessage,
      Value<String?>? serverConflictJson,
      Value<String>? createdAt,
      Value<String>? updatedAt,
      Value<int>? rowid}) {
    return OutboxDriftTableCompanion(
      mutationId: mutationId ?? this.mutationId,
      userId: userId ?? this.userId,
      entityType: entityType ?? this.entityType,
      entityId: entityId ?? this.entityId,
      op: op ?? this.op,
      baseVersion: baseVersion ?? this.baseVersion,
      payloadJson: payloadJson ?? this.payloadJson,
      status: status ?? this.status,
      retryCount: retryCount ?? this.retryCount,
      lastErrorCode: lastErrorCode ?? this.lastErrorCode,
      lastErrorMessage: lastErrorMessage ?? this.lastErrorMessage,
      serverConflictJson: serverConflictJson ?? this.serverConflictJson,
      createdAt: createdAt ?? this.createdAt,
      updatedAt: updatedAt ?? this.updatedAt,
      rowid: rowid ?? this.rowid,
    );
  }

  @override
  Map<String, Expression> toColumns(bool nullToAbsent) {
    final map = <String, Expression>{};
    if (mutationId.present) {
      map['mutation_id'] = Variable<String>(mutationId.value);
    }
    if (userId.present) {
      map['user_id'] = Variable<String>(userId.value);
    }
    if (entityType.present) {
      map['entity_type'] = Variable<String>(entityType.value);
    }
    if (entityId.present) {
      map['entity_id'] = Variable<String>(entityId.value);
    }
    if (op.present) {
      map['op'] = Variable<String>(op.value);
    }
    if (baseVersion.present) {
      map['base_version'] = Variable<int>(baseVersion.value);
    }
    if (payloadJson.present) {
      map['payload_json'] = Variable<String>(payloadJson.value);
    }
    if (status.present) {
      map['status'] = Variable<String>(status.value);
    }
    if (retryCount.present) {
      map['retry_count'] = Variable<int>(retryCount.value);
    }
    if (lastErrorCode.present) {
      map['last_error_code'] = Variable<String>(lastErrorCode.value);
    }
    if (lastErrorMessage.present) {
      map['last_error_message'] = Variable<String>(lastErrorMessage.value);
    }
    if (serverConflictJson.present) {
      map['server_conflict_json'] = Variable<String>(serverConflictJson.value);
    }
    if (createdAt.present) {
      map['created_at'] = Variable<String>(createdAt.value);
    }
    if (updatedAt.present) {
      map['updated_at'] = Variable<String>(updatedAt.value);
    }
    if (rowid.present) {
      map['rowid'] = Variable<int>(rowid.value);
    }
    return map;
  }

  @override
  String toString() {
    return (StringBuffer('OutboxDriftTableCompanion(')
          ..write('mutationId: $mutationId, ')
          ..write('userId: $userId, ')
          ..write('entityType: $entityType, ')
          ..write('entityId: $entityId, ')
          ..write('op: $op, ')
          ..write('baseVersion: $baseVersion, ')
          ..write('payloadJson: $payloadJson, ')
          ..write('status: $status, ')
          ..write('retryCount: $retryCount, ')
          ..write('lastErrorCode: $lastErrorCode, ')
          ..write('lastErrorMessage: $lastErrorMessage, ')
          ..write('serverConflictJson: $serverConflictJson, ')
          ..write('createdAt: $createdAt, ')
          ..write('updatedAt: $updatedAt, ')
          ..write('rowid: $rowid')
          ..write(')'))
        .toString();
  }
}

class $EntityBaseSnapshotDriftTableTable extends EntityBaseSnapshotDriftTable
    with
        TableInfo<$EntityBaseSnapshotDriftTableTable,
            EntityBaseSnapshotDriftTableData> {
  @override
  final GeneratedDatabase attachedDatabase;
  final String? _alias;
  $EntityBaseSnapshotDriftTableTable(this.attachedDatabase, [this._alias]);
  static const VerificationMeta _userIdMeta = const VerificationMeta('userId');
  @override
  late final GeneratedColumn<String> userId = GeneratedColumn<String>(
      'user_id', aliasedName, false,
      type: DriftSqlType.string, requiredDuringInsert: true);
  static const VerificationMeta _entityTypeMeta =
      const VerificationMeta('entityType');
  @override
  late final GeneratedColumn<String> entityType = GeneratedColumn<String>(
      'entity_type', aliasedName, false,
      type: DriftSqlType.string, requiredDuringInsert: true);
  static const VerificationMeta _entityIdMeta =
      const VerificationMeta('entityId');
  @override
  late final GeneratedColumn<String> entityId = GeneratedColumn<String>(
      'entity_id', aliasedName, false,
      type: DriftSqlType.string, requiredDuringInsert: true);
  static const VerificationMeta _baseVersionMeta =
      const VerificationMeta('baseVersion');
  @override
  late final GeneratedColumn<int> baseVersion = GeneratedColumn<int>(
      'base_version', aliasedName, false,
      type: DriftSqlType.int, requiredDuringInsert: true);
  static const VerificationMeta _snapshotJsonMeta =
      const VerificationMeta('snapshotJson');
  @override
  late final GeneratedColumn<String> snapshotJson = GeneratedColumn<String>(
      'snapshot_json', aliasedName, false,
      type: DriftSqlType.string, requiredDuringInsert: true);
  static const VerificationMeta _updatedAtMeta =
      const VerificationMeta('updatedAt');
  @override
  late final GeneratedColumn<String> updatedAt = GeneratedColumn<String>(
      'updated_at', aliasedName, false,
      type: DriftSqlType.string, requiredDuringInsert: true);
  @override
  List<GeneratedColumn> get $columns =>
      [userId, entityType, entityId, baseVersion, snapshotJson, updatedAt];
  @override
  String get aliasedName => _alias ?? actualTableName;
  @override
  String get actualTableName => $name;
  static const String $name = 'entity_base_snapshots';
  @override
  VerificationContext validateIntegrity(
      Insertable<EntityBaseSnapshotDriftTableData> instance,
      {bool isInserting = false}) {
    final context = VerificationContext();
    final data = instance.toColumns(true);
    if (data.containsKey('user_id')) {
      context.handle(_userIdMeta,
          userId.isAcceptableOrUnknown(data['user_id']!, _userIdMeta));
    } else if (isInserting) {
      context.missing(_userIdMeta);
    }
    if (data.containsKey('entity_type')) {
      context.handle(
          _entityTypeMeta,
          entityType.isAcceptableOrUnknown(
              data['entity_type']!, _entityTypeMeta));
    } else if (isInserting) {
      context.missing(_entityTypeMeta);
    }
    if (data.containsKey('entity_id')) {
      context.handle(_entityIdMeta,
          entityId.isAcceptableOrUnknown(data['entity_id']!, _entityIdMeta));
    } else if (isInserting) {
      context.missing(_entityIdMeta);
    }
    if (data.containsKey('base_version')) {
      context.handle(
          _baseVersionMeta,
          baseVersion.isAcceptableOrUnknown(
              data['base_version']!, _baseVersionMeta));
    } else if (isInserting) {
      context.missing(_baseVersionMeta);
    }
    if (data.containsKey('snapshot_json')) {
      context.handle(
          _snapshotJsonMeta,
          snapshotJson.isAcceptableOrUnknown(
              data['snapshot_json']!, _snapshotJsonMeta));
    } else if (isInserting) {
      context.missing(_snapshotJsonMeta);
    }
    if (data.containsKey('updated_at')) {
      context.handle(_updatedAtMeta,
          updatedAt.isAcceptableOrUnknown(data['updated_at']!, _updatedAtMeta));
    } else if (isInserting) {
      context.missing(_updatedAtMeta);
    }
    return context;
  }

  @override
  Set<GeneratedColumn> get $primaryKey => {userId, entityType, entityId};
  @override
  EntityBaseSnapshotDriftTableData map(Map<String, dynamic> data,
      {String? tablePrefix}) {
    final effectivePrefix = tablePrefix != null ? '$tablePrefix.' : '';
    return EntityBaseSnapshotDriftTableData(
      userId: attachedDatabase.typeMapping
          .read(DriftSqlType.string, data['${effectivePrefix}user_id'])!,
      entityType: attachedDatabase.typeMapping
          .read(DriftSqlType.string, data['${effectivePrefix}entity_type'])!,
      entityId: attachedDatabase.typeMapping
          .read(DriftSqlType.string, data['${effectivePrefix}entity_id'])!,
      baseVersion: attachedDatabase.typeMapping
          .read(DriftSqlType.int, data['${effectivePrefix}base_version'])!,
      snapshotJson: attachedDatabase.typeMapping
          .read(DriftSqlType.string, data['${effectivePrefix}snapshot_json'])!,
      updatedAt: attachedDatabase.typeMapping
          .read(DriftSqlType.string, data['${effectivePrefix}updated_at'])!,
    );
  }

  @override
  $EntityBaseSnapshotDriftTableTable createAlias(String alias) {
    return $EntityBaseSnapshotDriftTableTable(attachedDatabase, alias);
  }
}

class EntityBaseSnapshotDriftTableData extends DataClass
    implements Insertable<EntityBaseSnapshotDriftTableData> {
  final String userId;
  final String entityType;
  final String entityId;
  final int baseVersion;
  final String snapshotJson;
  final String updatedAt;
  const EntityBaseSnapshotDriftTableData(
      {required this.userId,
      required this.entityType,
      required this.entityId,
      required this.baseVersion,
      required this.snapshotJson,
      required this.updatedAt});
  @override
  Map<String, Expression> toColumns(bool nullToAbsent) {
    final map = <String, Expression>{};
    map['user_id'] = Variable<String>(userId);
    map['entity_type'] = Variable<String>(entityType);
    map['entity_id'] = Variable<String>(entityId);
    map['base_version'] = Variable<int>(baseVersion);
    map['snapshot_json'] = Variable<String>(snapshotJson);
    map['updated_at'] = Variable<String>(updatedAt);
    return map;
  }

  EntityBaseSnapshotDriftTableCompanion toCompanion(bool nullToAbsent) {
    return EntityBaseSnapshotDriftTableCompanion(
      userId: Value(userId),
      entityType: Value(entityType),
      entityId: Value(entityId),
      baseVersion: Value(baseVersion),
      snapshotJson: Value(snapshotJson),
      updatedAt: Value(updatedAt),
    );
  }

  factory EntityBaseSnapshotDriftTableData.fromJson(Map<String, dynamic> json,
      {ValueSerializer? serializer}) {
    serializer ??= driftRuntimeOptions.defaultSerializer;
    return EntityBaseSnapshotDriftTableData(
      userId: serializer.fromJson<String>(json['userId']),
      entityType: serializer.fromJson<String>(json['entityType']),
      entityId: serializer.fromJson<String>(json['entityId']),
      baseVersion: serializer.fromJson<int>(json['baseVersion']),
      snapshotJson: serializer.fromJson<String>(json['snapshotJson']),
      updatedAt: serializer.fromJson<String>(json['updatedAt']),
    );
  }
  @override
  Map<String, dynamic> toJson({ValueSerializer? serializer}) {
    serializer ??= driftRuntimeOptions.defaultSerializer;
    return <String, dynamic>{
      'userId': serializer.toJson<String>(userId),
      'entityType': serializer.toJson<String>(entityType),
      'entityId': serializer.toJson<String>(entityId),
      'baseVersion': serializer.toJson<int>(baseVersion),
      'snapshotJson': serializer.toJson<String>(snapshotJson),
      'updatedAt': serializer.toJson<String>(updatedAt),
    };
  }

  EntityBaseSnapshotDriftTableData copyWith(
          {String? userId,
          String? entityType,
          String? entityId,
          int? baseVersion,
          String? snapshotJson,
          String? updatedAt}) =>
      EntityBaseSnapshotDriftTableData(
        userId: userId ?? this.userId,
        entityType: entityType ?? this.entityType,
        entityId: entityId ?? this.entityId,
        baseVersion: baseVersion ?? this.baseVersion,
        snapshotJson: snapshotJson ?? this.snapshotJson,
        updatedAt: updatedAt ?? this.updatedAt,
      );
  EntityBaseSnapshotDriftTableData copyWithCompanion(
      EntityBaseSnapshotDriftTableCompanion data) {
    return EntityBaseSnapshotDriftTableData(
      userId: data.userId.present ? data.userId.value : this.userId,
      entityType:
          data.entityType.present ? data.entityType.value : this.entityType,
      entityId: data.entityId.present ? data.entityId.value : this.entityId,
      baseVersion:
          data.baseVersion.present ? data.baseVersion.value : this.baseVersion,
      snapshotJson: data.snapshotJson.present
          ? data.snapshotJson.value
          : this.snapshotJson,
      updatedAt: data.updatedAt.present ? data.updatedAt.value : this.updatedAt,
    );
  }

  @override
  String toString() {
    return (StringBuffer('EntityBaseSnapshotDriftTableData(')
          ..write('userId: $userId, ')
          ..write('entityType: $entityType, ')
          ..write('entityId: $entityId, ')
          ..write('baseVersion: $baseVersion, ')
          ..write('snapshotJson: $snapshotJson, ')
          ..write('updatedAt: $updatedAt')
          ..write(')'))
        .toString();
  }

  @override
  int get hashCode => Object.hash(
      userId, entityType, entityId, baseVersion, snapshotJson, updatedAt);
  @override
  bool operator ==(Object other) =>
      identical(this, other) ||
      (other is EntityBaseSnapshotDriftTableData &&
          other.userId == this.userId &&
          other.entityType == this.entityType &&
          other.entityId == this.entityId &&
          other.baseVersion == this.baseVersion &&
          other.snapshotJson == this.snapshotJson &&
          other.updatedAt == this.updatedAt);
}

class EntityBaseSnapshotDriftTableCompanion
    extends UpdateCompanion<EntityBaseSnapshotDriftTableData> {
  final Value<String> userId;
  final Value<String> entityType;
  final Value<String> entityId;
  final Value<int> baseVersion;
  final Value<String> snapshotJson;
  final Value<String> updatedAt;
  final Value<int> rowid;
  const EntityBaseSnapshotDriftTableCompanion({
    this.userId = const Value.absent(),
    this.entityType = const Value.absent(),
    this.entityId = const Value.absent(),
    this.baseVersion = const Value.absent(),
    this.snapshotJson = const Value.absent(),
    this.updatedAt = const Value.absent(),
    this.rowid = const Value.absent(),
  });
  EntityBaseSnapshotDriftTableCompanion.insert({
    required String userId,
    required String entityType,
    required String entityId,
    required int baseVersion,
    required String snapshotJson,
    required String updatedAt,
    this.rowid = const Value.absent(),
  })  : userId = Value(userId),
        entityType = Value(entityType),
        entityId = Value(entityId),
        baseVersion = Value(baseVersion),
        snapshotJson = Value(snapshotJson),
        updatedAt = Value(updatedAt);
  static Insertable<EntityBaseSnapshotDriftTableData> custom({
    Expression<String>? userId,
    Expression<String>? entityType,
    Expression<String>? entityId,
    Expression<int>? baseVersion,
    Expression<String>? snapshotJson,
    Expression<String>? updatedAt,
    Expression<int>? rowid,
  }) {
    return RawValuesInsertable({
      if (userId != null) 'user_id': userId,
      if (entityType != null) 'entity_type': entityType,
      if (entityId != null) 'entity_id': entityId,
      if (baseVersion != null) 'base_version': baseVersion,
      if (snapshotJson != null) 'snapshot_json': snapshotJson,
      if (updatedAt != null) 'updated_at': updatedAt,
      if (rowid != null) 'rowid': rowid,
    });
  }

  EntityBaseSnapshotDriftTableCompanion copyWith(
      {Value<String>? userId,
      Value<String>? entityType,
      Value<String>? entityId,
      Value<int>? baseVersion,
      Value<String>? snapshotJson,
      Value<String>? updatedAt,
      Value<int>? rowid}) {
    return EntityBaseSnapshotDriftTableCompanion(
      userId: userId ?? this.userId,
      entityType: entityType ?? this.entityType,
      entityId: entityId ?? this.entityId,
      baseVersion: baseVersion ?? this.baseVersion,
      snapshotJson: snapshotJson ?? this.snapshotJson,
      updatedAt: updatedAt ?? this.updatedAt,
      rowid: rowid ?? this.rowid,
    );
  }

  @override
  Map<String, Expression> toColumns(bool nullToAbsent) {
    final map = <String, Expression>{};
    if (userId.present) {
      map['user_id'] = Variable<String>(userId.value);
    }
    if (entityType.present) {
      map['entity_type'] = Variable<String>(entityType.value);
    }
    if (entityId.present) {
      map['entity_id'] = Variable<String>(entityId.value);
    }
    if (baseVersion.present) {
      map['base_version'] = Variable<int>(baseVersion.value);
    }
    if (snapshotJson.present) {
      map['snapshot_json'] = Variable<String>(snapshotJson.value);
    }
    if (updatedAt.present) {
      map['updated_at'] = Variable<String>(updatedAt.value);
    }
    if (rowid.present) {
      map['rowid'] = Variable<int>(rowid.value);
    }
    return map;
  }

  @override
  String toString() {
    return (StringBuffer('EntityBaseSnapshotDriftTableCompanion(')
          ..write('userId: $userId, ')
          ..write('entityType: $entityType, ')
          ..write('entityId: $entityId, ')
          ..write('baseVersion: $baseVersion, ')
          ..write('snapshotJson: $snapshotJson, ')
          ..write('updatedAt: $updatedAt, ')
          ..write('rowid: $rowid')
          ..write(')'))
        .toString();
  }
}

class $SyncLogDriftTableTable extends SyncLogDriftTable
    with TableInfo<$SyncLogDriftTableTable, SyncLogDriftTableData> {
  @override
  final GeneratedDatabase attachedDatabase;
  final String? _alias;
  $SyncLogDriftTableTable(this.attachedDatabase, [this._alias]);
  static const VerificationMeta _idMeta = const VerificationMeta('id');
  @override
  late final GeneratedColumn<int> id = GeneratedColumn<int>(
      'id', aliasedName, false,
      hasAutoIncrement: true,
      type: DriftSqlType.int,
      requiredDuringInsert: false,
      defaultConstraints:
          GeneratedColumn.constraintIsAlways('PRIMARY KEY AUTOINCREMENT'));
  static const VerificationMeta _userIdMeta = const VerificationMeta('userId');
  @override
  late final GeneratedColumn<String> userId = GeneratedColumn<String>(
      'user_id', aliasedName, false,
      type: DriftSqlType.string, requiredDuringInsert: true);
  static const VerificationMeta _directionMeta =
      const VerificationMeta('direction');
  @override
  late final GeneratedColumn<String> direction = GeneratedColumn<String>(
      'direction', aliasedName, false,
      type: DriftSqlType.string, requiredDuringInsert: true);
  static const VerificationMeta _outcomeMeta =
      const VerificationMeta('outcome');
  @override
  late final GeneratedColumn<String> outcome = GeneratedColumn<String>(
      'outcome', aliasedName, false,
      type: DriftSqlType.string, requiredDuringInsert: true);
  static const VerificationMeta _entityTypeMeta =
      const VerificationMeta('entityType');
  @override
  late final GeneratedColumn<String> entityType = GeneratedColumn<String>(
      'entity_type', aliasedName, true,
      type: DriftSqlType.string, requiredDuringInsert: false);
  static const VerificationMeta _entityIdMeta =
      const VerificationMeta('entityId');
  @override
  late final GeneratedColumn<String> entityId = GeneratedColumn<String>(
      'entity_id', aliasedName, true,
      type: DriftSqlType.string, requiredDuringInsert: false);
  static const VerificationMeta _mutationIdMeta =
      const VerificationMeta('mutationId');
  @override
  late final GeneratedColumn<String> mutationId = GeneratedColumn<String>(
      'mutation_id', aliasedName, true,
      type: DriftSqlType.string, requiredDuringInsert: false);
  static const VerificationMeta _latencyMsMeta =
      const VerificationMeta('latencyMs');
  @override
  late final GeneratedColumn<int> latencyMs = GeneratedColumn<int>(
      'latency_ms', aliasedName, true,
      type: DriftSqlType.int, requiredDuringInsert: false);
  static const VerificationMeta _errorCodeMeta =
      const VerificationMeta('errorCode');
  @override
  late final GeneratedColumn<String> errorCode = GeneratedColumn<String>(
      'error_code', aliasedName, true,
      type: DriftSqlType.string, requiredDuringInsert: false);
  static const VerificationMeta _detailsMeta =
      const VerificationMeta('details');
  @override
  late final GeneratedColumn<String> details = GeneratedColumn<String>(
      'details', aliasedName, true,
      type: DriftSqlType.string, requiredDuringInsert: false);
  static const VerificationMeta _createdAtMeta =
      const VerificationMeta('createdAt');
  @override
  late final GeneratedColumn<String> createdAt = GeneratedColumn<String>(
      'created_at', aliasedName, false,
      type: DriftSqlType.string, requiredDuringInsert: true);
  @override
  List<GeneratedColumn> get $columns => [
        id,
        userId,
        direction,
        outcome,
        entityType,
        entityId,
        mutationId,
        latencyMs,
        errorCode,
        details,
        createdAt
      ];
  @override
  String get aliasedName => _alias ?? actualTableName;
  @override
  String get actualTableName => $name;
  static const String $name = 'sync_log';
  @override
  VerificationContext validateIntegrity(
      Insertable<SyncLogDriftTableData> instance,
      {bool isInserting = false}) {
    final context = VerificationContext();
    final data = instance.toColumns(true);
    if (data.containsKey('id')) {
      context.handle(_idMeta, id.isAcceptableOrUnknown(data['id']!, _idMeta));
    }
    if (data.containsKey('user_id')) {
      context.handle(_userIdMeta,
          userId.isAcceptableOrUnknown(data['user_id']!, _userIdMeta));
    } else if (isInserting) {
      context.missing(_userIdMeta);
    }
    if (data.containsKey('direction')) {
      context.handle(_directionMeta,
          direction.isAcceptableOrUnknown(data['direction']!, _directionMeta));
    } else if (isInserting) {
      context.missing(_directionMeta);
    }
    if (data.containsKey('outcome')) {
      context.handle(_outcomeMeta,
          outcome.isAcceptableOrUnknown(data['outcome']!, _outcomeMeta));
    } else if (isInserting) {
      context.missing(_outcomeMeta);
    }
    if (data.containsKey('entity_type')) {
      context.handle(
          _entityTypeMeta,
          entityType.isAcceptableOrUnknown(
              data['entity_type']!, _entityTypeMeta));
    }
    if (data.containsKey('entity_id')) {
      context.handle(_entityIdMeta,
          entityId.isAcceptableOrUnknown(data['entity_id']!, _entityIdMeta));
    }
    if (data.containsKey('mutation_id')) {
      context.handle(
          _mutationIdMeta,
          mutationId.isAcceptableOrUnknown(
              data['mutation_id']!, _mutationIdMeta));
    }
    if (data.containsKey('latency_ms')) {
      context.handle(_latencyMsMeta,
          latencyMs.isAcceptableOrUnknown(data['latency_ms']!, _latencyMsMeta));
    }
    if (data.containsKey('error_code')) {
      context.handle(_errorCodeMeta,
          errorCode.isAcceptableOrUnknown(data['error_code']!, _errorCodeMeta));
    }
    if (data.containsKey('details')) {
      context.handle(_detailsMeta,
          details.isAcceptableOrUnknown(data['details']!, _detailsMeta));
    }
    if (data.containsKey('created_at')) {
      context.handle(_createdAtMeta,
          createdAt.isAcceptableOrUnknown(data['created_at']!, _createdAtMeta));
    } else if (isInserting) {
      context.missing(_createdAtMeta);
    }
    return context;
  }

  @override
  Set<GeneratedColumn> get $primaryKey => {id};
  @override
  SyncLogDriftTableData map(Map<String, dynamic> data, {String? tablePrefix}) {
    final effectivePrefix = tablePrefix != null ? '$tablePrefix.' : '';
    return SyncLogDriftTableData(
      id: attachedDatabase.typeMapping
          .read(DriftSqlType.int, data['${effectivePrefix}id'])!,
      userId: attachedDatabase.typeMapping
          .read(DriftSqlType.string, data['${effectivePrefix}user_id'])!,
      direction: attachedDatabase.typeMapping
          .read(DriftSqlType.string, data['${effectivePrefix}direction'])!,
      outcome: attachedDatabase.typeMapping
          .read(DriftSqlType.string, data['${effectivePrefix}outcome'])!,
      entityType: attachedDatabase.typeMapping
          .read(DriftSqlType.string, data['${effectivePrefix}entity_type']),
      entityId: attachedDatabase.typeMapping
          .read(DriftSqlType.string, data['${effectivePrefix}entity_id']),
      mutationId: attachedDatabase.typeMapping
          .read(DriftSqlType.string, data['${effectivePrefix}mutation_id']),
      latencyMs: attachedDatabase.typeMapping
          .read(DriftSqlType.int, data['${effectivePrefix}latency_ms']),
      errorCode: attachedDatabase.typeMapping
          .read(DriftSqlType.string, data['${effectivePrefix}error_code']),
      details: attachedDatabase.typeMapping
          .read(DriftSqlType.string, data['${effectivePrefix}details']),
      createdAt: attachedDatabase.typeMapping
          .read(DriftSqlType.string, data['${effectivePrefix}created_at'])!,
    );
  }

  @override
  $SyncLogDriftTableTable createAlias(String alias) {
    return $SyncLogDriftTableTable(attachedDatabase, alias);
  }
}

class SyncLogDriftTableData extends DataClass
    implements Insertable<SyncLogDriftTableData> {
  final int id;
  final String userId;
  final String direction;
  final String outcome;
  final String? entityType;
  final String? entityId;
  final String? mutationId;
  final int? latencyMs;
  final String? errorCode;
  final String? details;
  final String createdAt;
  const SyncLogDriftTableData(
      {required this.id,
      required this.userId,
      required this.direction,
      required this.outcome,
      this.entityType,
      this.entityId,
      this.mutationId,
      this.latencyMs,
      this.errorCode,
      this.details,
      required this.createdAt});
  @override
  Map<String, Expression> toColumns(bool nullToAbsent) {
    final map = <String, Expression>{};
    map['id'] = Variable<int>(id);
    map['user_id'] = Variable<String>(userId);
    map['direction'] = Variable<String>(direction);
    map['outcome'] = Variable<String>(outcome);
    if (!nullToAbsent || entityType != null) {
      map['entity_type'] = Variable<String>(entityType);
    }
    if (!nullToAbsent || entityId != null) {
      map['entity_id'] = Variable<String>(entityId);
    }
    if (!nullToAbsent || mutationId != null) {
      map['mutation_id'] = Variable<String>(mutationId);
    }
    if (!nullToAbsent || latencyMs != null) {
      map['latency_ms'] = Variable<int>(latencyMs);
    }
    if (!nullToAbsent || errorCode != null) {
      map['error_code'] = Variable<String>(errorCode);
    }
    if (!nullToAbsent || details != null) {
      map['details'] = Variable<String>(details);
    }
    map['created_at'] = Variable<String>(createdAt);
    return map;
  }

  SyncLogDriftTableCompanion toCompanion(bool nullToAbsent) {
    return SyncLogDriftTableCompanion(
      id: Value(id),
      userId: Value(userId),
      direction: Value(direction),
      outcome: Value(outcome),
      entityType: entityType == null && nullToAbsent
          ? const Value.absent()
          : Value(entityType),
      entityId: entityId == null && nullToAbsent
          ? const Value.absent()
          : Value(entityId),
      mutationId: mutationId == null && nullToAbsent
          ? const Value.absent()
          : Value(mutationId),
      latencyMs: latencyMs == null && nullToAbsent
          ? const Value.absent()
          : Value(latencyMs),
      errorCode: errorCode == null && nullToAbsent
          ? const Value.absent()
          : Value(errorCode),
      details: details == null && nullToAbsent
          ? const Value.absent()
          : Value(details),
      createdAt: Value(createdAt),
    );
  }

  factory SyncLogDriftTableData.fromJson(Map<String, dynamic> json,
      {ValueSerializer? serializer}) {
    serializer ??= driftRuntimeOptions.defaultSerializer;
    return SyncLogDriftTableData(
      id: serializer.fromJson<int>(json['id']),
      userId: serializer.fromJson<String>(json['userId']),
      direction: serializer.fromJson<String>(json['direction']),
      outcome: serializer.fromJson<String>(json['outcome']),
      entityType: serializer.fromJson<String?>(json['entityType']),
      entityId: serializer.fromJson<String?>(json['entityId']),
      mutationId: serializer.fromJson<String?>(json['mutationId']),
      latencyMs: serializer.fromJson<int?>(json['latencyMs']),
      errorCode: serializer.fromJson<String?>(json['errorCode']),
      details: serializer.fromJson<String?>(json['details']),
      createdAt: serializer.fromJson<String>(json['createdAt']),
    );
  }
  @override
  Map<String, dynamic> toJson({ValueSerializer? serializer}) {
    serializer ??= driftRuntimeOptions.defaultSerializer;
    return <String, dynamic>{
      'id': serializer.toJson<int>(id),
      'userId': serializer.toJson<String>(userId),
      'direction': serializer.toJson<String>(direction),
      'outcome': serializer.toJson<String>(outcome),
      'entityType': serializer.toJson<String?>(entityType),
      'entityId': serializer.toJson<String?>(entityId),
      'mutationId': serializer.toJson<String?>(mutationId),
      'latencyMs': serializer.toJson<int?>(latencyMs),
      'errorCode': serializer.toJson<String?>(errorCode),
      'details': serializer.toJson<String?>(details),
      'createdAt': serializer.toJson<String>(createdAt),
    };
  }

  SyncLogDriftTableData copyWith(
          {int? id,
          String? userId,
          String? direction,
          String? outcome,
          Value<String?> entityType = const Value.absent(),
          Value<String?> entityId = const Value.absent(),
          Value<String?> mutationId = const Value.absent(),
          Value<int?> latencyMs = const Value.absent(),
          Value<String?> errorCode = const Value.absent(),
          Value<String?> details = const Value.absent(),
          String? createdAt}) =>
      SyncLogDriftTableData(
        id: id ?? this.id,
        userId: userId ?? this.userId,
        direction: direction ?? this.direction,
        outcome: outcome ?? this.outcome,
        entityType: entityType.present ? entityType.value : this.entityType,
        entityId: entityId.present ? entityId.value : this.entityId,
        mutationId: mutationId.present ? mutationId.value : this.mutationId,
        latencyMs: latencyMs.present ? latencyMs.value : this.latencyMs,
        errorCode: errorCode.present ? errorCode.value : this.errorCode,
        details: details.present ? details.value : this.details,
        createdAt: createdAt ?? this.createdAt,
      );
  SyncLogDriftTableData copyWithCompanion(SyncLogDriftTableCompanion data) {
    return SyncLogDriftTableData(
      id: data.id.present ? data.id.value : this.id,
      userId: data.userId.present ? data.userId.value : this.userId,
      direction: data.direction.present ? data.direction.value : this.direction,
      outcome: data.outcome.present ? data.outcome.value : this.outcome,
      entityType:
          data.entityType.present ? data.entityType.value : this.entityType,
      entityId: data.entityId.present ? data.entityId.value : this.entityId,
      mutationId:
          data.mutationId.present ? data.mutationId.value : this.mutationId,
      latencyMs: data.latencyMs.present ? data.latencyMs.value : this.latencyMs,
      errorCode: data.errorCode.present ? data.errorCode.value : this.errorCode,
      details: data.details.present ? data.details.value : this.details,
      createdAt: data.createdAt.present ? data.createdAt.value : this.createdAt,
    );
  }

  @override
  String toString() {
    return (StringBuffer('SyncLogDriftTableData(')
          ..write('id: $id, ')
          ..write('userId: $userId, ')
          ..write('direction: $direction, ')
          ..write('outcome: $outcome, ')
          ..write('entityType: $entityType, ')
          ..write('entityId: $entityId, ')
          ..write('mutationId: $mutationId, ')
          ..write('latencyMs: $latencyMs, ')
          ..write('errorCode: $errorCode, ')
          ..write('details: $details, ')
          ..write('createdAt: $createdAt')
          ..write(')'))
        .toString();
  }

  @override
  int get hashCode => Object.hash(id, userId, direction, outcome, entityType,
      entityId, mutationId, latencyMs, errorCode, details, createdAt);
  @override
  bool operator ==(Object other) =>
      identical(this, other) ||
      (other is SyncLogDriftTableData &&
          other.id == this.id &&
          other.userId == this.userId &&
          other.direction == this.direction &&
          other.outcome == this.outcome &&
          other.entityType == this.entityType &&
          other.entityId == this.entityId &&
          other.mutationId == this.mutationId &&
          other.latencyMs == this.latencyMs &&
          other.errorCode == this.errorCode &&
          other.details == this.details &&
          other.createdAt == this.createdAt);
}

class SyncLogDriftTableCompanion
    extends UpdateCompanion<SyncLogDriftTableData> {
  final Value<int> id;
  final Value<String> userId;
  final Value<String> direction;
  final Value<String> outcome;
  final Value<String?> entityType;
  final Value<String?> entityId;
  final Value<String?> mutationId;
  final Value<int?> latencyMs;
  final Value<String?> errorCode;
  final Value<String?> details;
  final Value<String> createdAt;
  const SyncLogDriftTableCompanion({
    this.id = const Value.absent(),
    this.userId = const Value.absent(),
    this.direction = const Value.absent(),
    this.outcome = const Value.absent(),
    this.entityType = const Value.absent(),
    this.entityId = const Value.absent(),
    this.mutationId = const Value.absent(),
    this.latencyMs = const Value.absent(),
    this.errorCode = const Value.absent(),
    this.details = const Value.absent(),
    this.createdAt = const Value.absent(),
  });
  SyncLogDriftTableCompanion.insert({
    this.id = const Value.absent(),
    required String userId,
    required String direction,
    required String outcome,
    this.entityType = const Value.absent(),
    this.entityId = const Value.absent(),
    this.mutationId = const Value.absent(),
    this.latencyMs = const Value.absent(),
    this.errorCode = const Value.absent(),
    this.details = const Value.absent(),
    required String createdAt,
  })  : userId = Value(userId),
        direction = Value(direction),
        outcome = Value(outcome),
        createdAt = Value(createdAt);
  static Insertable<SyncLogDriftTableData> custom({
    Expression<int>? id,
    Expression<String>? userId,
    Expression<String>? direction,
    Expression<String>? outcome,
    Expression<String>? entityType,
    Expression<String>? entityId,
    Expression<String>? mutationId,
    Expression<int>? latencyMs,
    Expression<String>? errorCode,
    Expression<String>? details,
    Expression<String>? createdAt,
  }) {
    return RawValuesInsertable({
      if (id != null) 'id': id,
      if (userId != null) 'user_id': userId,
      if (direction != null) 'direction': direction,
      if (outcome != null) 'outcome': outcome,
      if (entityType != null) 'entity_type': entityType,
      if (entityId != null) 'entity_id': entityId,
      if (mutationId != null) 'mutation_id': mutationId,
      if (latencyMs != null) 'latency_ms': latencyMs,
      if (errorCode != null) 'error_code': errorCode,
      if (details != null) 'details': details,
      if (createdAt != null) 'created_at': createdAt,
    });
  }

  SyncLogDriftTableCompanion copyWith(
      {Value<int>? id,
      Value<String>? userId,
      Value<String>? direction,
      Value<String>? outcome,
      Value<String?>? entityType,
      Value<String?>? entityId,
      Value<String?>? mutationId,
      Value<int?>? latencyMs,
      Value<String?>? errorCode,
      Value<String?>? details,
      Value<String>? createdAt}) {
    return SyncLogDriftTableCompanion(
      id: id ?? this.id,
      userId: userId ?? this.userId,
      direction: direction ?? this.direction,
      outcome: outcome ?? this.outcome,
      entityType: entityType ?? this.entityType,
      entityId: entityId ?? this.entityId,
      mutationId: mutationId ?? this.mutationId,
      latencyMs: latencyMs ?? this.latencyMs,
      errorCode: errorCode ?? this.errorCode,
      details: details ?? this.details,
      createdAt: createdAt ?? this.createdAt,
    );
  }

  @override
  Map<String, Expression> toColumns(bool nullToAbsent) {
    final map = <String, Expression>{};
    if (id.present) {
      map['id'] = Variable<int>(id.value);
    }
    if (userId.present) {
      map['user_id'] = Variable<String>(userId.value);
    }
    if (direction.present) {
      map['direction'] = Variable<String>(direction.value);
    }
    if (outcome.present) {
      map['outcome'] = Variable<String>(outcome.value);
    }
    if (entityType.present) {
      map['entity_type'] = Variable<String>(entityType.value);
    }
    if (entityId.present) {
      map['entity_id'] = Variable<String>(entityId.value);
    }
    if (mutationId.present) {
      map['mutation_id'] = Variable<String>(mutationId.value);
    }
    if (latencyMs.present) {
      map['latency_ms'] = Variable<int>(latencyMs.value);
    }
    if (errorCode.present) {
      map['error_code'] = Variable<String>(errorCode.value);
    }
    if (details.present) {
      map['details'] = Variable<String>(details.value);
    }
    if (createdAt.present) {
      map['created_at'] = Variable<String>(createdAt.value);
    }
    return map;
  }

  @override
  String toString() {
    return (StringBuffer('SyncLogDriftTableCompanion(')
          ..write('id: $id, ')
          ..write('userId: $userId, ')
          ..write('direction: $direction, ')
          ..write('outcome: $outcome, ')
          ..write('entityType: $entityType, ')
          ..write('entityId: $entityId, ')
          ..write('mutationId: $mutationId, ')
          ..write('latencyMs: $latencyMs, ')
          ..write('errorCode: $errorCode, ')
          ..write('details: $details, ')
          ..write('createdAt: $createdAt')
          ..write(')'))
        .toString();
  }
}

class $ScheduledNotificationDriftTableTable
    extends ScheduledNotificationDriftTable
    with
        TableInfo<$ScheduledNotificationDriftTableTable,
            ScheduledNotificationDriftTableData> {
  @override
  final GeneratedDatabase attachedDatabase;
  final String? _alias;
  $ScheduledNotificationDriftTableTable(this.attachedDatabase, [this._alias]);
  static const VerificationMeta _dedupeKeyMeta =
      const VerificationMeta('dedupeKey');
  @override
  late final GeneratedColumn<String> dedupeKey = GeneratedColumn<String>(
      'dedupe_key', aliasedName, false,
      type: DriftSqlType.string, requiredDuringInsert: true);
  static const VerificationMeta _userIdMeta = const VerificationMeta('userId');
  @override
  late final GeneratedColumn<String> userId = GeneratedColumn<String>(
      'user_id', aliasedName, false,
      type: DriftSqlType.string, requiredDuringInsert: true);
  static const VerificationMeta _targetTypeMeta =
      const VerificationMeta('targetType');
  @override
  late final GeneratedColumn<String> targetType = GeneratedColumn<String>(
      'target_type', aliasedName, false,
      type: DriftSqlType.string, requiredDuringInsert: true);
  static const VerificationMeta _targetIdMeta =
      const VerificationMeta('targetId');
  @override
  late final GeneratedColumn<String> targetId = GeneratedColumn<String>(
      'target_id', aliasedName, false,
      type: DriftSqlType.string, requiredDuringInsert: true);
  static const VerificationMeta _fireAtUtcMeta =
      const VerificationMeta('fireAtUtc');
  @override
  late final GeneratedColumn<String> fireAtUtc = GeneratedColumn<String>(
      'fire_at_utc', aliasedName, false,
      type: DriftSqlType.string, requiredDuringInsert: true);
  static const VerificationMeta _titleMeta = const VerificationMeta('title');
  @override
  late final GeneratedColumn<String> title = GeneratedColumn<String>(
      'title', aliasedName, false,
      type: DriftSqlType.string, requiredDuringInsert: true);
  static const VerificationMeta _bodyMeta = const VerificationMeta('body');
  @override
  late final GeneratedColumn<String> body = GeneratedColumn<String>(
      'body', aliasedName, false,
      type: DriftSqlType.string, requiredDuringInsert: true);
  static const VerificationMeta _redactedLockScreenMeta =
      const VerificationMeta('redactedLockScreen');
  @override
  late final GeneratedColumn<int> redactedLockScreen = GeneratedColumn<int>(
      'redacted_lock_screen', aliasedName, false,
      type: DriftSqlType.int,
      requiredDuringInsert: false,
      defaultValue: const Constant(1));
  @override
  List<GeneratedColumn> get $columns => [
        dedupeKey,
        userId,
        targetType,
        targetId,
        fireAtUtc,
        title,
        body,
        redactedLockScreen
      ];
  @override
  String get aliasedName => _alias ?? actualTableName;
  @override
  String get actualTableName => $name;
  static const String $name = 'scheduled_notifications';
  @override
  VerificationContext validateIntegrity(
      Insertable<ScheduledNotificationDriftTableData> instance,
      {bool isInserting = false}) {
    final context = VerificationContext();
    final data = instance.toColumns(true);
    if (data.containsKey('dedupe_key')) {
      context.handle(_dedupeKeyMeta,
          dedupeKey.isAcceptableOrUnknown(data['dedupe_key']!, _dedupeKeyMeta));
    } else if (isInserting) {
      context.missing(_dedupeKeyMeta);
    }
    if (data.containsKey('user_id')) {
      context.handle(_userIdMeta,
          userId.isAcceptableOrUnknown(data['user_id']!, _userIdMeta));
    } else if (isInserting) {
      context.missing(_userIdMeta);
    }
    if (data.containsKey('target_type')) {
      context.handle(
          _targetTypeMeta,
          targetType.isAcceptableOrUnknown(
              data['target_type']!, _targetTypeMeta));
    } else if (isInserting) {
      context.missing(_targetTypeMeta);
    }
    if (data.containsKey('target_id')) {
      context.handle(_targetIdMeta,
          targetId.isAcceptableOrUnknown(data['target_id']!, _targetIdMeta));
    } else if (isInserting) {
      context.missing(_targetIdMeta);
    }
    if (data.containsKey('fire_at_utc')) {
      context.handle(
          _fireAtUtcMeta,
          fireAtUtc.isAcceptableOrUnknown(
              data['fire_at_utc']!, _fireAtUtcMeta));
    } else if (isInserting) {
      context.missing(_fireAtUtcMeta);
    }
    if (data.containsKey('title')) {
      context.handle(
          _titleMeta, title.isAcceptableOrUnknown(data['title']!, _titleMeta));
    } else if (isInserting) {
      context.missing(_titleMeta);
    }
    if (data.containsKey('body')) {
      context.handle(
          _bodyMeta, body.isAcceptableOrUnknown(data['body']!, _bodyMeta));
    } else if (isInserting) {
      context.missing(_bodyMeta);
    }
    if (data.containsKey('redacted_lock_screen')) {
      context.handle(
          _redactedLockScreenMeta,
          redactedLockScreen.isAcceptableOrUnknown(
              data['redacted_lock_screen']!, _redactedLockScreenMeta));
    }
    return context;
  }

  @override
  Set<GeneratedColumn> get $primaryKey => {dedupeKey};
  @override
  ScheduledNotificationDriftTableData map(Map<String, dynamic> data,
      {String? tablePrefix}) {
    final effectivePrefix = tablePrefix != null ? '$tablePrefix.' : '';
    return ScheduledNotificationDriftTableData(
      dedupeKey: attachedDatabase.typeMapping
          .read(DriftSqlType.string, data['${effectivePrefix}dedupe_key'])!,
      userId: attachedDatabase.typeMapping
          .read(DriftSqlType.string, data['${effectivePrefix}user_id'])!,
      targetType: attachedDatabase.typeMapping
          .read(DriftSqlType.string, data['${effectivePrefix}target_type'])!,
      targetId: attachedDatabase.typeMapping
          .read(DriftSqlType.string, data['${effectivePrefix}target_id'])!,
      fireAtUtc: attachedDatabase.typeMapping
          .read(DriftSqlType.string, data['${effectivePrefix}fire_at_utc'])!,
      title: attachedDatabase.typeMapping
          .read(DriftSqlType.string, data['${effectivePrefix}title'])!,
      body: attachedDatabase.typeMapping
          .read(DriftSqlType.string, data['${effectivePrefix}body'])!,
      redactedLockScreen: attachedDatabase.typeMapping.read(
          DriftSqlType.int, data['${effectivePrefix}redacted_lock_screen'])!,
    );
  }

  @override
  $ScheduledNotificationDriftTableTable createAlias(String alias) {
    return $ScheduledNotificationDriftTableTable(attachedDatabase, alias);
  }
}

class ScheduledNotificationDriftTableData extends DataClass
    implements Insertable<ScheduledNotificationDriftTableData> {
  final String dedupeKey;
  final String userId;
  final String targetType;
  final String targetId;
  final String fireAtUtc;
  final String title;
  final String body;
  final int redactedLockScreen;
  const ScheduledNotificationDriftTableData(
      {required this.dedupeKey,
      required this.userId,
      required this.targetType,
      required this.targetId,
      required this.fireAtUtc,
      required this.title,
      required this.body,
      required this.redactedLockScreen});
  @override
  Map<String, Expression> toColumns(bool nullToAbsent) {
    final map = <String, Expression>{};
    map['dedupe_key'] = Variable<String>(dedupeKey);
    map['user_id'] = Variable<String>(userId);
    map['target_type'] = Variable<String>(targetType);
    map['target_id'] = Variable<String>(targetId);
    map['fire_at_utc'] = Variable<String>(fireAtUtc);
    map['title'] = Variable<String>(title);
    map['body'] = Variable<String>(body);
    map['redacted_lock_screen'] = Variable<int>(redactedLockScreen);
    return map;
  }

  ScheduledNotificationDriftTableCompanion toCompanion(bool nullToAbsent) {
    return ScheduledNotificationDriftTableCompanion(
      dedupeKey: Value(dedupeKey),
      userId: Value(userId),
      targetType: Value(targetType),
      targetId: Value(targetId),
      fireAtUtc: Value(fireAtUtc),
      title: Value(title),
      body: Value(body),
      redactedLockScreen: Value(redactedLockScreen),
    );
  }

  factory ScheduledNotificationDriftTableData.fromJson(
      Map<String, dynamic> json,
      {ValueSerializer? serializer}) {
    serializer ??= driftRuntimeOptions.defaultSerializer;
    return ScheduledNotificationDriftTableData(
      dedupeKey: serializer.fromJson<String>(json['dedupeKey']),
      userId: serializer.fromJson<String>(json['userId']),
      targetType: serializer.fromJson<String>(json['targetType']),
      targetId: serializer.fromJson<String>(json['targetId']),
      fireAtUtc: serializer.fromJson<String>(json['fireAtUtc']),
      title: serializer.fromJson<String>(json['title']),
      body: serializer.fromJson<String>(json['body']),
      redactedLockScreen: serializer.fromJson<int>(json['redactedLockScreen']),
    );
  }
  @override
  Map<String, dynamic> toJson({ValueSerializer? serializer}) {
    serializer ??= driftRuntimeOptions.defaultSerializer;
    return <String, dynamic>{
      'dedupeKey': serializer.toJson<String>(dedupeKey),
      'userId': serializer.toJson<String>(userId),
      'targetType': serializer.toJson<String>(targetType),
      'targetId': serializer.toJson<String>(targetId),
      'fireAtUtc': serializer.toJson<String>(fireAtUtc),
      'title': serializer.toJson<String>(title),
      'body': serializer.toJson<String>(body),
      'redactedLockScreen': serializer.toJson<int>(redactedLockScreen),
    };
  }

  ScheduledNotificationDriftTableData copyWith(
          {String? dedupeKey,
          String? userId,
          String? targetType,
          String? targetId,
          String? fireAtUtc,
          String? title,
          String? body,
          int? redactedLockScreen}) =>
      ScheduledNotificationDriftTableData(
        dedupeKey: dedupeKey ?? this.dedupeKey,
        userId: userId ?? this.userId,
        targetType: targetType ?? this.targetType,
        targetId: targetId ?? this.targetId,
        fireAtUtc: fireAtUtc ?? this.fireAtUtc,
        title: title ?? this.title,
        body: body ?? this.body,
        redactedLockScreen: redactedLockScreen ?? this.redactedLockScreen,
      );
  ScheduledNotificationDriftTableData copyWithCompanion(
      ScheduledNotificationDriftTableCompanion data) {
    return ScheduledNotificationDriftTableData(
      dedupeKey: data.dedupeKey.present ? data.dedupeKey.value : this.dedupeKey,
      userId: data.userId.present ? data.userId.value : this.userId,
      targetType:
          data.targetType.present ? data.targetType.value : this.targetType,
      targetId: data.targetId.present ? data.targetId.value : this.targetId,
      fireAtUtc: data.fireAtUtc.present ? data.fireAtUtc.value : this.fireAtUtc,
      title: data.title.present ? data.title.value : this.title,
      body: data.body.present ? data.body.value : this.body,
      redactedLockScreen: data.redactedLockScreen.present
          ? data.redactedLockScreen.value
          : this.redactedLockScreen,
    );
  }

  @override
  String toString() {
    return (StringBuffer('ScheduledNotificationDriftTableData(')
          ..write('dedupeKey: $dedupeKey, ')
          ..write('userId: $userId, ')
          ..write('targetType: $targetType, ')
          ..write('targetId: $targetId, ')
          ..write('fireAtUtc: $fireAtUtc, ')
          ..write('title: $title, ')
          ..write('body: $body, ')
          ..write('redactedLockScreen: $redactedLockScreen')
          ..write(')'))
        .toString();
  }

  @override
  int get hashCode => Object.hash(dedupeKey, userId, targetType, targetId,
      fireAtUtc, title, body, redactedLockScreen);
  @override
  bool operator ==(Object other) =>
      identical(this, other) ||
      (other is ScheduledNotificationDriftTableData &&
          other.dedupeKey == this.dedupeKey &&
          other.userId == this.userId &&
          other.targetType == this.targetType &&
          other.targetId == this.targetId &&
          other.fireAtUtc == this.fireAtUtc &&
          other.title == this.title &&
          other.body == this.body &&
          other.redactedLockScreen == this.redactedLockScreen);
}

class ScheduledNotificationDriftTableCompanion
    extends UpdateCompanion<ScheduledNotificationDriftTableData> {
  final Value<String> dedupeKey;
  final Value<String> userId;
  final Value<String> targetType;
  final Value<String> targetId;
  final Value<String> fireAtUtc;
  final Value<String> title;
  final Value<String> body;
  final Value<int> redactedLockScreen;
  final Value<int> rowid;
  const ScheduledNotificationDriftTableCompanion({
    this.dedupeKey = const Value.absent(),
    this.userId = const Value.absent(),
    this.targetType = const Value.absent(),
    this.targetId = const Value.absent(),
    this.fireAtUtc = const Value.absent(),
    this.title = const Value.absent(),
    this.body = const Value.absent(),
    this.redactedLockScreen = const Value.absent(),
    this.rowid = const Value.absent(),
  });
  ScheduledNotificationDriftTableCompanion.insert({
    required String dedupeKey,
    required String userId,
    required String targetType,
    required String targetId,
    required String fireAtUtc,
    required String title,
    required String body,
    this.redactedLockScreen = const Value.absent(),
    this.rowid = const Value.absent(),
  })  : dedupeKey = Value(dedupeKey),
        userId = Value(userId),
        targetType = Value(targetType),
        targetId = Value(targetId),
        fireAtUtc = Value(fireAtUtc),
        title = Value(title),
        body = Value(body);
  static Insertable<ScheduledNotificationDriftTableData> custom({
    Expression<String>? dedupeKey,
    Expression<String>? userId,
    Expression<String>? targetType,
    Expression<String>? targetId,
    Expression<String>? fireAtUtc,
    Expression<String>? title,
    Expression<String>? body,
    Expression<int>? redactedLockScreen,
    Expression<int>? rowid,
  }) {
    return RawValuesInsertable({
      if (dedupeKey != null) 'dedupe_key': dedupeKey,
      if (userId != null) 'user_id': userId,
      if (targetType != null) 'target_type': targetType,
      if (targetId != null) 'target_id': targetId,
      if (fireAtUtc != null) 'fire_at_utc': fireAtUtc,
      if (title != null) 'title': title,
      if (body != null) 'body': body,
      if (redactedLockScreen != null)
        'redacted_lock_screen': redactedLockScreen,
      if (rowid != null) 'rowid': rowid,
    });
  }

  ScheduledNotificationDriftTableCompanion copyWith(
      {Value<String>? dedupeKey,
      Value<String>? userId,
      Value<String>? targetType,
      Value<String>? targetId,
      Value<String>? fireAtUtc,
      Value<String>? title,
      Value<String>? body,
      Value<int>? redactedLockScreen,
      Value<int>? rowid}) {
    return ScheduledNotificationDriftTableCompanion(
      dedupeKey: dedupeKey ?? this.dedupeKey,
      userId: userId ?? this.userId,
      targetType: targetType ?? this.targetType,
      targetId: targetId ?? this.targetId,
      fireAtUtc: fireAtUtc ?? this.fireAtUtc,
      title: title ?? this.title,
      body: body ?? this.body,
      redactedLockScreen: redactedLockScreen ?? this.redactedLockScreen,
      rowid: rowid ?? this.rowid,
    );
  }

  @override
  Map<String, Expression> toColumns(bool nullToAbsent) {
    final map = <String, Expression>{};
    if (dedupeKey.present) {
      map['dedupe_key'] = Variable<String>(dedupeKey.value);
    }
    if (userId.present) {
      map['user_id'] = Variable<String>(userId.value);
    }
    if (targetType.present) {
      map['target_type'] = Variable<String>(targetType.value);
    }
    if (targetId.present) {
      map['target_id'] = Variable<String>(targetId.value);
    }
    if (fireAtUtc.present) {
      map['fire_at_utc'] = Variable<String>(fireAtUtc.value);
    }
    if (title.present) {
      map['title'] = Variable<String>(title.value);
    }
    if (body.present) {
      map['body'] = Variable<String>(body.value);
    }
    if (redactedLockScreen.present) {
      map['redacted_lock_screen'] = Variable<int>(redactedLockScreen.value);
    }
    if (rowid.present) {
      map['rowid'] = Variable<int>(rowid.value);
    }
    return map;
  }

  @override
  String toString() {
    return (StringBuffer('ScheduledNotificationDriftTableCompanion(')
          ..write('dedupeKey: $dedupeKey, ')
          ..write('userId: $userId, ')
          ..write('targetType: $targetType, ')
          ..write('targetId: $targetId, ')
          ..write('fireAtUtc: $fireAtUtc, ')
          ..write('title: $title, ')
          ..write('body: $body, ')
          ..write('redactedLockScreen: $redactedLockScreen, ')
          ..write('rowid: $rowid')
          ..write(')'))
        .toString();
  }
}

class $DeliveredNotificationDriftTableTable
    extends DeliveredNotificationDriftTable
    with
        TableInfo<$DeliveredNotificationDriftTableTable,
            DeliveredNotificationDriftTableData> {
  @override
  final GeneratedDatabase attachedDatabase;
  final String? _alias;
  $DeliveredNotificationDriftTableTable(this.attachedDatabase, [this._alias]);
  static const VerificationMeta _dedupeKeyMeta =
      const VerificationMeta('dedupeKey');
  @override
  late final GeneratedColumn<String> dedupeKey = GeneratedColumn<String>(
      'dedupe_key', aliasedName, false,
      type: DriftSqlType.string, requiredDuringInsert: true);
  static const VerificationMeta _userIdMeta = const VerificationMeta('userId');
  @override
  late final GeneratedColumn<String> userId = GeneratedColumn<String>(
      'user_id', aliasedName, false,
      type: DriftSqlType.string, requiredDuringInsert: true);
  static const VerificationMeta _shownAtMeta =
      const VerificationMeta('shownAt');
  @override
  late final GeneratedColumn<String> shownAt = GeneratedColumn<String>(
      'shown_at', aliasedName, false,
      type: DriftSqlType.string, requiredDuringInsert: true);
  @override
  List<GeneratedColumn> get $columns => [dedupeKey, userId, shownAt];
  @override
  String get aliasedName => _alias ?? actualTableName;
  @override
  String get actualTableName => $name;
  static const String $name = 'delivered_notifications';
  @override
  VerificationContext validateIntegrity(
      Insertable<DeliveredNotificationDriftTableData> instance,
      {bool isInserting = false}) {
    final context = VerificationContext();
    final data = instance.toColumns(true);
    if (data.containsKey('dedupe_key')) {
      context.handle(_dedupeKeyMeta,
          dedupeKey.isAcceptableOrUnknown(data['dedupe_key']!, _dedupeKeyMeta));
    } else if (isInserting) {
      context.missing(_dedupeKeyMeta);
    }
    if (data.containsKey('user_id')) {
      context.handle(_userIdMeta,
          userId.isAcceptableOrUnknown(data['user_id']!, _userIdMeta));
    } else if (isInserting) {
      context.missing(_userIdMeta);
    }
    if (data.containsKey('shown_at')) {
      context.handle(_shownAtMeta,
          shownAt.isAcceptableOrUnknown(data['shown_at']!, _shownAtMeta));
    } else if (isInserting) {
      context.missing(_shownAtMeta);
    }
    return context;
  }

  @override
  Set<GeneratedColumn> get $primaryKey => {dedupeKey};
  @override
  DeliveredNotificationDriftTableData map(Map<String, dynamic> data,
      {String? tablePrefix}) {
    final effectivePrefix = tablePrefix != null ? '$tablePrefix.' : '';
    return DeliveredNotificationDriftTableData(
      dedupeKey: attachedDatabase.typeMapping
          .read(DriftSqlType.string, data['${effectivePrefix}dedupe_key'])!,
      userId: attachedDatabase.typeMapping
          .read(DriftSqlType.string, data['${effectivePrefix}user_id'])!,
      shownAt: attachedDatabase.typeMapping
          .read(DriftSqlType.string, data['${effectivePrefix}shown_at'])!,
    );
  }

  @override
  $DeliveredNotificationDriftTableTable createAlias(String alias) {
    return $DeliveredNotificationDriftTableTable(attachedDatabase, alias);
  }
}

class DeliveredNotificationDriftTableData extends DataClass
    implements Insertable<DeliveredNotificationDriftTableData> {
  final String dedupeKey;
  final String userId;
  final String shownAt;
  const DeliveredNotificationDriftTableData(
      {required this.dedupeKey, required this.userId, required this.shownAt});
  @override
  Map<String, Expression> toColumns(bool nullToAbsent) {
    final map = <String, Expression>{};
    map['dedupe_key'] = Variable<String>(dedupeKey);
    map['user_id'] = Variable<String>(userId);
    map['shown_at'] = Variable<String>(shownAt);
    return map;
  }

  DeliveredNotificationDriftTableCompanion toCompanion(bool nullToAbsent) {
    return DeliveredNotificationDriftTableCompanion(
      dedupeKey: Value(dedupeKey),
      userId: Value(userId),
      shownAt: Value(shownAt),
    );
  }

  factory DeliveredNotificationDriftTableData.fromJson(
      Map<String, dynamic> json,
      {ValueSerializer? serializer}) {
    serializer ??= driftRuntimeOptions.defaultSerializer;
    return DeliveredNotificationDriftTableData(
      dedupeKey: serializer.fromJson<String>(json['dedupeKey']),
      userId: serializer.fromJson<String>(json['userId']),
      shownAt: serializer.fromJson<String>(json['shownAt']),
    );
  }
  @override
  Map<String, dynamic> toJson({ValueSerializer? serializer}) {
    serializer ??= driftRuntimeOptions.defaultSerializer;
    return <String, dynamic>{
      'dedupeKey': serializer.toJson<String>(dedupeKey),
      'userId': serializer.toJson<String>(userId),
      'shownAt': serializer.toJson<String>(shownAt),
    };
  }

  DeliveredNotificationDriftTableData copyWith(
          {String? dedupeKey, String? userId, String? shownAt}) =>
      DeliveredNotificationDriftTableData(
        dedupeKey: dedupeKey ?? this.dedupeKey,
        userId: userId ?? this.userId,
        shownAt: shownAt ?? this.shownAt,
      );
  DeliveredNotificationDriftTableData copyWithCompanion(
      DeliveredNotificationDriftTableCompanion data) {
    return DeliveredNotificationDriftTableData(
      dedupeKey: data.dedupeKey.present ? data.dedupeKey.value : this.dedupeKey,
      userId: data.userId.present ? data.userId.value : this.userId,
      shownAt: data.shownAt.present ? data.shownAt.value : this.shownAt,
    );
  }

  @override
  String toString() {
    return (StringBuffer('DeliveredNotificationDriftTableData(')
          ..write('dedupeKey: $dedupeKey, ')
          ..write('userId: $userId, ')
          ..write('shownAt: $shownAt')
          ..write(')'))
        .toString();
  }

  @override
  int get hashCode => Object.hash(dedupeKey, userId, shownAt);
  @override
  bool operator ==(Object other) =>
      identical(this, other) ||
      (other is DeliveredNotificationDriftTableData &&
          other.dedupeKey == this.dedupeKey &&
          other.userId == this.userId &&
          other.shownAt == this.shownAt);
}

class DeliveredNotificationDriftTableCompanion
    extends UpdateCompanion<DeliveredNotificationDriftTableData> {
  final Value<String> dedupeKey;
  final Value<String> userId;
  final Value<String> shownAt;
  final Value<int> rowid;
  const DeliveredNotificationDriftTableCompanion({
    this.dedupeKey = const Value.absent(),
    this.userId = const Value.absent(),
    this.shownAt = const Value.absent(),
    this.rowid = const Value.absent(),
  });
  DeliveredNotificationDriftTableCompanion.insert({
    required String dedupeKey,
    required String userId,
    required String shownAt,
    this.rowid = const Value.absent(),
  })  : dedupeKey = Value(dedupeKey),
        userId = Value(userId),
        shownAt = Value(shownAt);
  static Insertable<DeliveredNotificationDriftTableData> custom({
    Expression<String>? dedupeKey,
    Expression<String>? userId,
    Expression<String>? shownAt,
    Expression<int>? rowid,
  }) {
    return RawValuesInsertable({
      if (dedupeKey != null) 'dedupe_key': dedupeKey,
      if (userId != null) 'user_id': userId,
      if (shownAt != null) 'shown_at': shownAt,
      if (rowid != null) 'rowid': rowid,
    });
  }

  DeliveredNotificationDriftTableCompanion copyWith(
      {Value<String>? dedupeKey,
      Value<String>? userId,
      Value<String>? shownAt,
      Value<int>? rowid}) {
    return DeliveredNotificationDriftTableCompanion(
      dedupeKey: dedupeKey ?? this.dedupeKey,
      userId: userId ?? this.userId,
      shownAt: shownAt ?? this.shownAt,
      rowid: rowid ?? this.rowid,
    );
  }

  @override
  Map<String, Expression> toColumns(bool nullToAbsent) {
    final map = <String, Expression>{};
    if (dedupeKey.present) {
      map['dedupe_key'] = Variable<String>(dedupeKey.value);
    }
    if (userId.present) {
      map['user_id'] = Variable<String>(userId.value);
    }
    if (shownAt.present) {
      map['shown_at'] = Variable<String>(shownAt.value);
    }
    if (rowid.present) {
      map['rowid'] = Variable<int>(rowid.value);
    }
    return map;
  }

  @override
  String toString() {
    return (StringBuffer('DeliveredNotificationDriftTableCompanion(')
          ..write('dedupeKey: $dedupeKey, ')
          ..write('userId: $userId, ')
          ..write('shownAt: $shownAt, ')
          ..write('rowid: $rowid')
          ..write(')'))
        .toString();
  }
}

class $UserSettingsDriftTableTable extends UserSettingsDriftTable
    with TableInfo<$UserSettingsDriftTableTable, UserSettingsDriftTableData> {
  @override
  final GeneratedDatabase attachedDatabase;
  final String? _alias;
  $UserSettingsDriftTableTable(this.attachedDatabase, [this._alias]);
  static const VerificationMeta _idMeta = const VerificationMeta('id');
  @override
  late final GeneratedColumn<String> id = GeneratedColumn<String>(
      'id', aliasedName, false,
      type: DriftSqlType.string, requiredDuringInsert: true);
  static const VerificationMeta _userIdMeta = const VerificationMeta('userId');
  @override
  late final GeneratedColumn<String> userId = GeneratedColumn<String>(
      'user_id', aliasedName, false,
      type: DriftSqlType.string, requiredDuringInsert: true);
  static const VerificationMeta _versionMeta =
      const VerificationMeta('version');
  @override
  late final GeneratedColumn<int> version = GeneratedColumn<int>(
      'version', aliasedName, false,
      type: DriftSqlType.int,
      requiredDuringInsert: false,
      defaultValue: const Constant(0));
  static const VerificationMeta _updatedAtMeta =
      const VerificationMeta('updatedAt');
  @override
  late final GeneratedColumn<String> updatedAt = GeneratedColumn<String>(
      'updated_at', aliasedName, false,
      type: DriftSqlType.string, requiredDuringInsert: true);
  static const VerificationMeta _deletedAtMeta =
      const VerificationMeta('deletedAt');
  @override
  late final GeneratedColumn<String> deletedAt = GeneratedColumn<String>(
      'deleted_at', aliasedName, true,
      type: DriftSqlType.string, requiredDuringInsert: false);
  static const VerificationMeta _syncStateMeta =
      const VerificationMeta('syncState');
  @override
  late final GeneratedColumn<String> syncState = GeneratedColumn<String>(
      'sync_state', aliasedName, false,
      type: DriftSqlType.string,
      requiredDuringInsert: false,
      defaultValue: const Constant('synced'));
  static const VerificationMeta _payloadJsonMeta =
      const VerificationMeta('payloadJson');
  @override
  late final GeneratedColumn<String> payloadJson = GeneratedColumn<String>(
      'payload_json', aliasedName, false,
      type: DriftSqlType.string, requiredDuringInsert: true);
  @override
  List<GeneratedColumn> get $columns =>
      [id, userId, version, updatedAt, deletedAt, syncState, payloadJson];
  @override
  String get aliasedName => _alias ?? actualTableName;
  @override
  String get actualTableName => $name;
  static const String $name = 'user_settings';
  @override
  VerificationContext validateIntegrity(
      Insertable<UserSettingsDriftTableData> instance,
      {bool isInserting = false}) {
    final context = VerificationContext();
    final data = instance.toColumns(true);
    if (data.containsKey('id')) {
      context.handle(_idMeta, id.isAcceptableOrUnknown(data['id']!, _idMeta));
    } else if (isInserting) {
      context.missing(_idMeta);
    }
    if (data.containsKey('user_id')) {
      context.handle(_userIdMeta,
          userId.isAcceptableOrUnknown(data['user_id']!, _userIdMeta));
    } else if (isInserting) {
      context.missing(_userIdMeta);
    }
    if (data.containsKey('version')) {
      context.handle(_versionMeta,
          version.isAcceptableOrUnknown(data['version']!, _versionMeta));
    }
    if (data.containsKey('updated_at')) {
      context.handle(_updatedAtMeta,
          updatedAt.isAcceptableOrUnknown(data['updated_at']!, _updatedAtMeta));
    } else if (isInserting) {
      context.missing(_updatedAtMeta);
    }
    if (data.containsKey('deleted_at')) {
      context.handle(_deletedAtMeta,
          deletedAt.isAcceptableOrUnknown(data['deleted_at']!, _deletedAtMeta));
    }
    if (data.containsKey('sync_state')) {
      context.handle(_syncStateMeta,
          syncState.isAcceptableOrUnknown(data['sync_state']!, _syncStateMeta));
    }
    if (data.containsKey('payload_json')) {
      context.handle(
          _payloadJsonMeta,
          payloadJson.isAcceptableOrUnknown(
              data['payload_json']!, _payloadJsonMeta));
    } else if (isInserting) {
      context.missing(_payloadJsonMeta);
    }
    return context;
  }

  @override
  Set<GeneratedColumn> get $primaryKey => {userId, id};
  @override
  UserSettingsDriftTableData map(Map<String, dynamic> data,
      {String? tablePrefix}) {
    final effectivePrefix = tablePrefix != null ? '$tablePrefix.' : '';
    return UserSettingsDriftTableData(
      id: attachedDatabase.typeMapping
          .read(DriftSqlType.string, data['${effectivePrefix}id'])!,
      userId: attachedDatabase.typeMapping
          .read(DriftSqlType.string, data['${effectivePrefix}user_id'])!,
      version: attachedDatabase.typeMapping
          .read(DriftSqlType.int, data['${effectivePrefix}version'])!,
      updatedAt: attachedDatabase.typeMapping
          .read(DriftSqlType.string, data['${effectivePrefix}updated_at'])!,
      deletedAt: attachedDatabase.typeMapping
          .read(DriftSqlType.string, data['${effectivePrefix}deleted_at']),
      syncState: attachedDatabase.typeMapping
          .read(DriftSqlType.string, data['${effectivePrefix}sync_state'])!,
      payloadJson: attachedDatabase.typeMapping
          .read(DriftSqlType.string, data['${effectivePrefix}payload_json'])!,
    );
  }

  @override
  $UserSettingsDriftTableTable createAlias(String alias) {
    return $UserSettingsDriftTableTable(attachedDatabase, alias);
  }
}

class UserSettingsDriftTableData extends DataClass
    implements Insertable<UserSettingsDriftTableData> {
  final String id;
  final String userId;
  final int version;
  final String updatedAt;
  final String? deletedAt;
  final String syncState;
  final String payloadJson;
  const UserSettingsDriftTableData(
      {required this.id,
      required this.userId,
      required this.version,
      required this.updatedAt,
      this.deletedAt,
      required this.syncState,
      required this.payloadJson});
  @override
  Map<String, Expression> toColumns(bool nullToAbsent) {
    final map = <String, Expression>{};
    map['id'] = Variable<String>(id);
    map['user_id'] = Variable<String>(userId);
    map['version'] = Variable<int>(version);
    map['updated_at'] = Variable<String>(updatedAt);
    if (!nullToAbsent || deletedAt != null) {
      map['deleted_at'] = Variable<String>(deletedAt);
    }
    map['sync_state'] = Variable<String>(syncState);
    map['payload_json'] = Variable<String>(payloadJson);
    return map;
  }

  UserSettingsDriftTableCompanion toCompanion(bool nullToAbsent) {
    return UserSettingsDriftTableCompanion(
      id: Value(id),
      userId: Value(userId),
      version: Value(version),
      updatedAt: Value(updatedAt),
      deletedAt: deletedAt == null && nullToAbsent
          ? const Value.absent()
          : Value(deletedAt),
      syncState: Value(syncState),
      payloadJson: Value(payloadJson),
    );
  }

  factory UserSettingsDriftTableData.fromJson(Map<String, dynamic> json,
      {ValueSerializer? serializer}) {
    serializer ??= driftRuntimeOptions.defaultSerializer;
    return UserSettingsDriftTableData(
      id: serializer.fromJson<String>(json['id']),
      userId: serializer.fromJson<String>(json['userId']),
      version: serializer.fromJson<int>(json['version']),
      updatedAt: serializer.fromJson<String>(json['updatedAt']),
      deletedAt: serializer.fromJson<String?>(json['deletedAt']),
      syncState: serializer.fromJson<String>(json['syncState']),
      payloadJson: serializer.fromJson<String>(json['payloadJson']),
    );
  }
  @override
  Map<String, dynamic> toJson({ValueSerializer? serializer}) {
    serializer ??= driftRuntimeOptions.defaultSerializer;
    return <String, dynamic>{
      'id': serializer.toJson<String>(id),
      'userId': serializer.toJson<String>(userId),
      'version': serializer.toJson<int>(version),
      'updatedAt': serializer.toJson<String>(updatedAt),
      'deletedAt': serializer.toJson<String?>(deletedAt),
      'syncState': serializer.toJson<String>(syncState),
      'payloadJson': serializer.toJson<String>(payloadJson),
    };
  }

  UserSettingsDriftTableData copyWith(
          {String? id,
          String? userId,
          int? version,
          String? updatedAt,
          Value<String?> deletedAt = const Value.absent(),
          String? syncState,
          String? payloadJson}) =>
      UserSettingsDriftTableData(
        id: id ?? this.id,
        userId: userId ?? this.userId,
        version: version ?? this.version,
        updatedAt: updatedAt ?? this.updatedAt,
        deletedAt: deletedAt.present ? deletedAt.value : this.deletedAt,
        syncState: syncState ?? this.syncState,
        payloadJson: payloadJson ?? this.payloadJson,
      );
  UserSettingsDriftTableData copyWithCompanion(
      UserSettingsDriftTableCompanion data) {
    return UserSettingsDriftTableData(
      id: data.id.present ? data.id.value : this.id,
      userId: data.userId.present ? data.userId.value : this.userId,
      version: data.version.present ? data.version.value : this.version,
      updatedAt: data.updatedAt.present ? data.updatedAt.value : this.updatedAt,
      deletedAt: data.deletedAt.present ? data.deletedAt.value : this.deletedAt,
      syncState: data.syncState.present ? data.syncState.value : this.syncState,
      payloadJson:
          data.payloadJson.present ? data.payloadJson.value : this.payloadJson,
    );
  }

  @override
  String toString() {
    return (StringBuffer('UserSettingsDriftTableData(')
          ..write('id: $id, ')
          ..write('userId: $userId, ')
          ..write('version: $version, ')
          ..write('updatedAt: $updatedAt, ')
          ..write('deletedAt: $deletedAt, ')
          ..write('syncState: $syncState, ')
          ..write('payloadJson: $payloadJson')
          ..write(')'))
        .toString();
  }

  @override
  int get hashCode => Object.hash(
      id, userId, version, updatedAt, deletedAt, syncState, payloadJson);
  @override
  bool operator ==(Object other) =>
      identical(this, other) ||
      (other is UserSettingsDriftTableData &&
          other.id == this.id &&
          other.userId == this.userId &&
          other.version == this.version &&
          other.updatedAt == this.updatedAt &&
          other.deletedAt == this.deletedAt &&
          other.syncState == this.syncState &&
          other.payloadJson == this.payloadJson);
}

class UserSettingsDriftTableCompanion
    extends UpdateCompanion<UserSettingsDriftTableData> {
  final Value<String> id;
  final Value<String> userId;
  final Value<int> version;
  final Value<String> updatedAt;
  final Value<String?> deletedAt;
  final Value<String> syncState;
  final Value<String> payloadJson;
  final Value<int> rowid;
  const UserSettingsDriftTableCompanion({
    this.id = const Value.absent(),
    this.userId = const Value.absent(),
    this.version = const Value.absent(),
    this.updatedAt = const Value.absent(),
    this.deletedAt = const Value.absent(),
    this.syncState = const Value.absent(),
    this.payloadJson = const Value.absent(),
    this.rowid = const Value.absent(),
  });
  UserSettingsDriftTableCompanion.insert({
    required String id,
    required String userId,
    this.version = const Value.absent(),
    required String updatedAt,
    this.deletedAt = const Value.absent(),
    this.syncState = const Value.absent(),
    required String payloadJson,
    this.rowid = const Value.absent(),
  })  : id = Value(id),
        userId = Value(userId),
        updatedAt = Value(updatedAt),
        payloadJson = Value(payloadJson);
  static Insertable<UserSettingsDriftTableData> custom({
    Expression<String>? id,
    Expression<String>? userId,
    Expression<int>? version,
    Expression<String>? updatedAt,
    Expression<String>? deletedAt,
    Expression<String>? syncState,
    Expression<String>? payloadJson,
    Expression<int>? rowid,
  }) {
    return RawValuesInsertable({
      if (id != null) 'id': id,
      if (userId != null) 'user_id': userId,
      if (version != null) 'version': version,
      if (updatedAt != null) 'updated_at': updatedAt,
      if (deletedAt != null) 'deleted_at': deletedAt,
      if (syncState != null) 'sync_state': syncState,
      if (payloadJson != null) 'payload_json': payloadJson,
      if (rowid != null) 'rowid': rowid,
    });
  }

  UserSettingsDriftTableCompanion copyWith(
      {Value<String>? id,
      Value<String>? userId,
      Value<int>? version,
      Value<String>? updatedAt,
      Value<String?>? deletedAt,
      Value<String>? syncState,
      Value<String>? payloadJson,
      Value<int>? rowid}) {
    return UserSettingsDriftTableCompanion(
      id: id ?? this.id,
      userId: userId ?? this.userId,
      version: version ?? this.version,
      updatedAt: updatedAt ?? this.updatedAt,
      deletedAt: deletedAt ?? this.deletedAt,
      syncState: syncState ?? this.syncState,
      payloadJson: payloadJson ?? this.payloadJson,
      rowid: rowid ?? this.rowid,
    );
  }

  @override
  Map<String, Expression> toColumns(bool nullToAbsent) {
    final map = <String, Expression>{};
    if (id.present) {
      map['id'] = Variable<String>(id.value);
    }
    if (userId.present) {
      map['user_id'] = Variable<String>(userId.value);
    }
    if (version.present) {
      map['version'] = Variable<int>(version.value);
    }
    if (updatedAt.present) {
      map['updated_at'] = Variable<String>(updatedAt.value);
    }
    if (deletedAt.present) {
      map['deleted_at'] = Variable<String>(deletedAt.value);
    }
    if (syncState.present) {
      map['sync_state'] = Variable<String>(syncState.value);
    }
    if (payloadJson.present) {
      map['payload_json'] = Variable<String>(payloadJson.value);
    }
    if (rowid.present) {
      map['rowid'] = Variable<int>(rowid.value);
    }
    return map;
  }

  @override
  String toString() {
    return (StringBuffer('UserSettingsDriftTableCompanion(')
          ..write('id: $id, ')
          ..write('userId: $userId, ')
          ..write('version: $version, ')
          ..write('updatedAt: $updatedAt, ')
          ..write('deletedAt: $deletedAt, ')
          ..write('syncState: $syncState, ')
          ..write('payloadJson: $payloadJson, ')
          ..write('rowid: $rowid')
          ..write(')'))
        .toString();
  }
}

class $CategoriesDriftTableTable extends CategoriesDriftTable
    with TableInfo<$CategoriesDriftTableTable, CategoriesDriftTableData> {
  @override
  final GeneratedDatabase attachedDatabase;
  final String? _alias;
  $CategoriesDriftTableTable(this.attachedDatabase, [this._alias]);
  static const VerificationMeta _idMeta = const VerificationMeta('id');
  @override
  late final GeneratedColumn<String> id = GeneratedColumn<String>(
      'id', aliasedName, false,
      type: DriftSqlType.string, requiredDuringInsert: true);
  static const VerificationMeta _userIdMeta = const VerificationMeta('userId');
  @override
  late final GeneratedColumn<String> userId = GeneratedColumn<String>(
      'user_id', aliasedName, false,
      type: DriftSqlType.string, requiredDuringInsert: true);
  static const VerificationMeta _versionMeta =
      const VerificationMeta('version');
  @override
  late final GeneratedColumn<int> version = GeneratedColumn<int>(
      'version', aliasedName, false,
      type: DriftSqlType.int,
      requiredDuringInsert: false,
      defaultValue: const Constant(0));
  static const VerificationMeta _updatedAtMeta =
      const VerificationMeta('updatedAt');
  @override
  late final GeneratedColumn<String> updatedAt = GeneratedColumn<String>(
      'updated_at', aliasedName, false,
      type: DriftSqlType.string, requiredDuringInsert: true);
  static const VerificationMeta _deletedAtMeta =
      const VerificationMeta('deletedAt');
  @override
  late final GeneratedColumn<String> deletedAt = GeneratedColumn<String>(
      'deleted_at', aliasedName, true,
      type: DriftSqlType.string, requiredDuringInsert: false);
  static const VerificationMeta _syncStateMeta =
      const VerificationMeta('syncState');
  @override
  late final GeneratedColumn<String> syncState = GeneratedColumn<String>(
      'sync_state', aliasedName, false,
      type: DriftSqlType.string,
      requiredDuringInsert: false,
      defaultValue: const Constant('synced'));
  static const VerificationMeta _payloadJsonMeta =
      const VerificationMeta('payloadJson');
  @override
  late final GeneratedColumn<String> payloadJson = GeneratedColumn<String>(
      'payload_json', aliasedName, false,
      type: DriftSqlType.string, requiredDuringInsert: true);
  @override
  List<GeneratedColumn> get $columns =>
      [id, userId, version, updatedAt, deletedAt, syncState, payloadJson];
  @override
  String get aliasedName => _alias ?? actualTableName;
  @override
  String get actualTableName => $name;
  static const String $name = 'categories';
  @override
  VerificationContext validateIntegrity(
      Insertable<CategoriesDriftTableData> instance,
      {bool isInserting = false}) {
    final context = VerificationContext();
    final data = instance.toColumns(true);
    if (data.containsKey('id')) {
      context.handle(_idMeta, id.isAcceptableOrUnknown(data['id']!, _idMeta));
    } else if (isInserting) {
      context.missing(_idMeta);
    }
    if (data.containsKey('user_id')) {
      context.handle(_userIdMeta,
          userId.isAcceptableOrUnknown(data['user_id']!, _userIdMeta));
    } else if (isInserting) {
      context.missing(_userIdMeta);
    }
    if (data.containsKey('version')) {
      context.handle(_versionMeta,
          version.isAcceptableOrUnknown(data['version']!, _versionMeta));
    }
    if (data.containsKey('updated_at')) {
      context.handle(_updatedAtMeta,
          updatedAt.isAcceptableOrUnknown(data['updated_at']!, _updatedAtMeta));
    } else if (isInserting) {
      context.missing(_updatedAtMeta);
    }
    if (data.containsKey('deleted_at')) {
      context.handle(_deletedAtMeta,
          deletedAt.isAcceptableOrUnknown(data['deleted_at']!, _deletedAtMeta));
    }
    if (data.containsKey('sync_state')) {
      context.handle(_syncStateMeta,
          syncState.isAcceptableOrUnknown(data['sync_state']!, _syncStateMeta));
    }
    if (data.containsKey('payload_json')) {
      context.handle(
          _payloadJsonMeta,
          payloadJson.isAcceptableOrUnknown(
              data['payload_json']!, _payloadJsonMeta));
    } else if (isInserting) {
      context.missing(_payloadJsonMeta);
    }
    return context;
  }

  @override
  Set<GeneratedColumn> get $primaryKey => {userId, id};
  @override
  CategoriesDriftTableData map(Map<String, dynamic> data,
      {String? tablePrefix}) {
    final effectivePrefix = tablePrefix != null ? '$tablePrefix.' : '';
    return CategoriesDriftTableData(
      id: attachedDatabase.typeMapping
          .read(DriftSqlType.string, data['${effectivePrefix}id'])!,
      userId: attachedDatabase.typeMapping
          .read(DriftSqlType.string, data['${effectivePrefix}user_id'])!,
      version: attachedDatabase.typeMapping
          .read(DriftSqlType.int, data['${effectivePrefix}version'])!,
      updatedAt: attachedDatabase.typeMapping
          .read(DriftSqlType.string, data['${effectivePrefix}updated_at'])!,
      deletedAt: attachedDatabase.typeMapping
          .read(DriftSqlType.string, data['${effectivePrefix}deleted_at']),
      syncState: attachedDatabase.typeMapping
          .read(DriftSqlType.string, data['${effectivePrefix}sync_state'])!,
      payloadJson: attachedDatabase.typeMapping
          .read(DriftSqlType.string, data['${effectivePrefix}payload_json'])!,
    );
  }

  @override
  $CategoriesDriftTableTable createAlias(String alias) {
    return $CategoriesDriftTableTable(attachedDatabase, alias);
  }
}

class CategoriesDriftTableData extends DataClass
    implements Insertable<CategoriesDriftTableData> {
  final String id;
  final String userId;
  final int version;
  final String updatedAt;
  final String? deletedAt;
  final String syncState;
  final String payloadJson;
  const CategoriesDriftTableData(
      {required this.id,
      required this.userId,
      required this.version,
      required this.updatedAt,
      this.deletedAt,
      required this.syncState,
      required this.payloadJson});
  @override
  Map<String, Expression> toColumns(bool nullToAbsent) {
    final map = <String, Expression>{};
    map['id'] = Variable<String>(id);
    map['user_id'] = Variable<String>(userId);
    map['version'] = Variable<int>(version);
    map['updated_at'] = Variable<String>(updatedAt);
    if (!nullToAbsent || deletedAt != null) {
      map['deleted_at'] = Variable<String>(deletedAt);
    }
    map['sync_state'] = Variable<String>(syncState);
    map['payload_json'] = Variable<String>(payloadJson);
    return map;
  }

  CategoriesDriftTableCompanion toCompanion(bool nullToAbsent) {
    return CategoriesDriftTableCompanion(
      id: Value(id),
      userId: Value(userId),
      version: Value(version),
      updatedAt: Value(updatedAt),
      deletedAt: deletedAt == null && nullToAbsent
          ? const Value.absent()
          : Value(deletedAt),
      syncState: Value(syncState),
      payloadJson: Value(payloadJson),
    );
  }

  factory CategoriesDriftTableData.fromJson(Map<String, dynamic> json,
      {ValueSerializer? serializer}) {
    serializer ??= driftRuntimeOptions.defaultSerializer;
    return CategoriesDriftTableData(
      id: serializer.fromJson<String>(json['id']),
      userId: serializer.fromJson<String>(json['userId']),
      version: serializer.fromJson<int>(json['version']),
      updatedAt: serializer.fromJson<String>(json['updatedAt']),
      deletedAt: serializer.fromJson<String?>(json['deletedAt']),
      syncState: serializer.fromJson<String>(json['syncState']),
      payloadJson: serializer.fromJson<String>(json['payloadJson']),
    );
  }
  @override
  Map<String, dynamic> toJson({ValueSerializer? serializer}) {
    serializer ??= driftRuntimeOptions.defaultSerializer;
    return <String, dynamic>{
      'id': serializer.toJson<String>(id),
      'userId': serializer.toJson<String>(userId),
      'version': serializer.toJson<int>(version),
      'updatedAt': serializer.toJson<String>(updatedAt),
      'deletedAt': serializer.toJson<String?>(deletedAt),
      'syncState': serializer.toJson<String>(syncState),
      'payloadJson': serializer.toJson<String>(payloadJson),
    };
  }

  CategoriesDriftTableData copyWith(
          {String? id,
          String? userId,
          int? version,
          String? updatedAt,
          Value<String?> deletedAt = const Value.absent(),
          String? syncState,
          String? payloadJson}) =>
      CategoriesDriftTableData(
        id: id ?? this.id,
        userId: userId ?? this.userId,
        version: version ?? this.version,
        updatedAt: updatedAt ?? this.updatedAt,
        deletedAt: deletedAt.present ? deletedAt.value : this.deletedAt,
        syncState: syncState ?? this.syncState,
        payloadJson: payloadJson ?? this.payloadJson,
      );
  CategoriesDriftTableData copyWithCompanion(
      CategoriesDriftTableCompanion data) {
    return CategoriesDriftTableData(
      id: data.id.present ? data.id.value : this.id,
      userId: data.userId.present ? data.userId.value : this.userId,
      version: data.version.present ? data.version.value : this.version,
      updatedAt: data.updatedAt.present ? data.updatedAt.value : this.updatedAt,
      deletedAt: data.deletedAt.present ? data.deletedAt.value : this.deletedAt,
      syncState: data.syncState.present ? data.syncState.value : this.syncState,
      payloadJson:
          data.payloadJson.present ? data.payloadJson.value : this.payloadJson,
    );
  }

  @override
  String toString() {
    return (StringBuffer('CategoriesDriftTableData(')
          ..write('id: $id, ')
          ..write('userId: $userId, ')
          ..write('version: $version, ')
          ..write('updatedAt: $updatedAt, ')
          ..write('deletedAt: $deletedAt, ')
          ..write('syncState: $syncState, ')
          ..write('payloadJson: $payloadJson')
          ..write(')'))
        .toString();
  }

  @override
  int get hashCode => Object.hash(
      id, userId, version, updatedAt, deletedAt, syncState, payloadJson);
  @override
  bool operator ==(Object other) =>
      identical(this, other) ||
      (other is CategoriesDriftTableData &&
          other.id == this.id &&
          other.userId == this.userId &&
          other.version == this.version &&
          other.updatedAt == this.updatedAt &&
          other.deletedAt == this.deletedAt &&
          other.syncState == this.syncState &&
          other.payloadJson == this.payloadJson);
}

class CategoriesDriftTableCompanion
    extends UpdateCompanion<CategoriesDriftTableData> {
  final Value<String> id;
  final Value<String> userId;
  final Value<int> version;
  final Value<String> updatedAt;
  final Value<String?> deletedAt;
  final Value<String> syncState;
  final Value<String> payloadJson;
  final Value<int> rowid;
  const CategoriesDriftTableCompanion({
    this.id = const Value.absent(),
    this.userId = const Value.absent(),
    this.version = const Value.absent(),
    this.updatedAt = const Value.absent(),
    this.deletedAt = const Value.absent(),
    this.syncState = const Value.absent(),
    this.payloadJson = const Value.absent(),
    this.rowid = const Value.absent(),
  });
  CategoriesDriftTableCompanion.insert({
    required String id,
    required String userId,
    this.version = const Value.absent(),
    required String updatedAt,
    this.deletedAt = const Value.absent(),
    this.syncState = const Value.absent(),
    required String payloadJson,
    this.rowid = const Value.absent(),
  })  : id = Value(id),
        userId = Value(userId),
        updatedAt = Value(updatedAt),
        payloadJson = Value(payloadJson);
  static Insertable<CategoriesDriftTableData> custom({
    Expression<String>? id,
    Expression<String>? userId,
    Expression<int>? version,
    Expression<String>? updatedAt,
    Expression<String>? deletedAt,
    Expression<String>? syncState,
    Expression<String>? payloadJson,
    Expression<int>? rowid,
  }) {
    return RawValuesInsertable({
      if (id != null) 'id': id,
      if (userId != null) 'user_id': userId,
      if (version != null) 'version': version,
      if (updatedAt != null) 'updated_at': updatedAt,
      if (deletedAt != null) 'deleted_at': deletedAt,
      if (syncState != null) 'sync_state': syncState,
      if (payloadJson != null) 'payload_json': payloadJson,
      if (rowid != null) 'rowid': rowid,
    });
  }

  CategoriesDriftTableCompanion copyWith(
      {Value<String>? id,
      Value<String>? userId,
      Value<int>? version,
      Value<String>? updatedAt,
      Value<String?>? deletedAt,
      Value<String>? syncState,
      Value<String>? payloadJson,
      Value<int>? rowid}) {
    return CategoriesDriftTableCompanion(
      id: id ?? this.id,
      userId: userId ?? this.userId,
      version: version ?? this.version,
      updatedAt: updatedAt ?? this.updatedAt,
      deletedAt: deletedAt ?? this.deletedAt,
      syncState: syncState ?? this.syncState,
      payloadJson: payloadJson ?? this.payloadJson,
      rowid: rowid ?? this.rowid,
    );
  }

  @override
  Map<String, Expression> toColumns(bool nullToAbsent) {
    final map = <String, Expression>{};
    if (id.present) {
      map['id'] = Variable<String>(id.value);
    }
    if (userId.present) {
      map['user_id'] = Variable<String>(userId.value);
    }
    if (version.present) {
      map['version'] = Variable<int>(version.value);
    }
    if (updatedAt.present) {
      map['updated_at'] = Variable<String>(updatedAt.value);
    }
    if (deletedAt.present) {
      map['deleted_at'] = Variable<String>(deletedAt.value);
    }
    if (syncState.present) {
      map['sync_state'] = Variable<String>(syncState.value);
    }
    if (payloadJson.present) {
      map['payload_json'] = Variable<String>(payloadJson.value);
    }
    if (rowid.present) {
      map['rowid'] = Variable<int>(rowid.value);
    }
    return map;
  }

  @override
  String toString() {
    return (StringBuffer('CategoriesDriftTableCompanion(')
          ..write('id: $id, ')
          ..write('userId: $userId, ')
          ..write('version: $version, ')
          ..write('updatedAt: $updatedAt, ')
          ..write('deletedAt: $deletedAt, ')
          ..write('syncState: $syncState, ')
          ..write('payloadJson: $payloadJson, ')
          ..write('rowid: $rowid')
          ..write(')'))
        .toString();
  }
}

class $ProjectsDriftTableTable extends ProjectsDriftTable
    with TableInfo<$ProjectsDriftTableTable, ProjectsDriftTableData> {
  @override
  final GeneratedDatabase attachedDatabase;
  final String? _alias;
  $ProjectsDriftTableTable(this.attachedDatabase, [this._alias]);
  static const VerificationMeta _idMeta = const VerificationMeta('id');
  @override
  late final GeneratedColumn<String> id = GeneratedColumn<String>(
      'id', aliasedName, false,
      type: DriftSqlType.string, requiredDuringInsert: true);
  static const VerificationMeta _userIdMeta = const VerificationMeta('userId');
  @override
  late final GeneratedColumn<String> userId = GeneratedColumn<String>(
      'user_id', aliasedName, false,
      type: DriftSqlType.string, requiredDuringInsert: true);
  static const VerificationMeta _versionMeta =
      const VerificationMeta('version');
  @override
  late final GeneratedColumn<int> version = GeneratedColumn<int>(
      'version', aliasedName, false,
      type: DriftSqlType.int,
      requiredDuringInsert: false,
      defaultValue: const Constant(0));
  static const VerificationMeta _updatedAtMeta =
      const VerificationMeta('updatedAt');
  @override
  late final GeneratedColumn<String> updatedAt = GeneratedColumn<String>(
      'updated_at', aliasedName, false,
      type: DriftSqlType.string, requiredDuringInsert: true);
  static const VerificationMeta _deletedAtMeta =
      const VerificationMeta('deletedAt');
  @override
  late final GeneratedColumn<String> deletedAt = GeneratedColumn<String>(
      'deleted_at', aliasedName, true,
      type: DriftSqlType.string, requiredDuringInsert: false);
  static const VerificationMeta _syncStateMeta =
      const VerificationMeta('syncState');
  @override
  late final GeneratedColumn<String> syncState = GeneratedColumn<String>(
      'sync_state', aliasedName, false,
      type: DriftSqlType.string,
      requiredDuringInsert: false,
      defaultValue: const Constant('synced'));
  static const VerificationMeta _payloadJsonMeta =
      const VerificationMeta('payloadJson');
  @override
  late final GeneratedColumn<String> payloadJson = GeneratedColumn<String>(
      'payload_json', aliasedName, false,
      type: DriftSqlType.string, requiredDuringInsert: true);
  @override
  List<GeneratedColumn> get $columns =>
      [id, userId, version, updatedAt, deletedAt, syncState, payloadJson];
  @override
  String get aliasedName => _alias ?? actualTableName;
  @override
  String get actualTableName => $name;
  static const String $name = 'projects';
  @override
  VerificationContext validateIntegrity(
      Insertable<ProjectsDriftTableData> instance,
      {bool isInserting = false}) {
    final context = VerificationContext();
    final data = instance.toColumns(true);
    if (data.containsKey('id')) {
      context.handle(_idMeta, id.isAcceptableOrUnknown(data['id']!, _idMeta));
    } else if (isInserting) {
      context.missing(_idMeta);
    }
    if (data.containsKey('user_id')) {
      context.handle(_userIdMeta,
          userId.isAcceptableOrUnknown(data['user_id']!, _userIdMeta));
    } else if (isInserting) {
      context.missing(_userIdMeta);
    }
    if (data.containsKey('version')) {
      context.handle(_versionMeta,
          version.isAcceptableOrUnknown(data['version']!, _versionMeta));
    }
    if (data.containsKey('updated_at')) {
      context.handle(_updatedAtMeta,
          updatedAt.isAcceptableOrUnknown(data['updated_at']!, _updatedAtMeta));
    } else if (isInserting) {
      context.missing(_updatedAtMeta);
    }
    if (data.containsKey('deleted_at')) {
      context.handle(_deletedAtMeta,
          deletedAt.isAcceptableOrUnknown(data['deleted_at']!, _deletedAtMeta));
    }
    if (data.containsKey('sync_state')) {
      context.handle(_syncStateMeta,
          syncState.isAcceptableOrUnknown(data['sync_state']!, _syncStateMeta));
    }
    if (data.containsKey('payload_json')) {
      context.handle(
          _payloadJsonMeta,
          payloadJson.isAcceptableOrUnknown(
              data['payload_json']!, _payloadJsonMeta));
    } else if (isInserting) {
      context.missing(_payloadJsonMeta);
    }
    return context;
  }

  @override
  Set<GeneratedColumn> get $primaryKey => {userId, id};
  @override
  ProjectsDriftTableData map(Map<String, dynamic> data, {String? tablePrefix}) {
    final effectivePrefix = tablePrefix != null ? '$tablePrefix.' : '';
    return ProjectsDriftTableData(
      id: attachedDatabase.typeMapping
          .read(DriftSqlType.string, data['${effectivePrefix}id'])!,
      userId: attachedDatabase.typeMapping
          .read(DriftSqlType.string, data['${effectivePrefix}user_id'])!,
      version: attachedDatabase.typeMapping
          .read(DriftSqlType.int, data['${effectivePrefix}version'])!,
      updatedAt: attachedDatabase.typeMapping
          .read(DriftSqlType.string, data['${effectivePrefix}updated_at'])!,
      deletedAt: attachedDatabase.typeMapping
          .read(DriftSqlType.string, data['${effectivePrefix}deleted_at']),
      syncState: attachedDatabase.typeMapping
          .read(DriftSqlType.string, data['${effectivePrefix}sync_state'])!,
      payloadJson: attachedDatabase.typeMapping
          .read(DriftSqlType.string, data['${effectivePrefix}payload_json'])!,
    );
  }

  @override
  $ProjectsDriftTableTable createAlias(String alias) {
    return $ProjectsDriftTableTable(attachedDatabase, alias);
  }
}

class ProjectsDriftTableData extends DataClass
    implements Insertable<ProjectsDriftTableData> {
  final String id;
  final String userId;
  final int version;
  final String updatedAt;
  final String? deletedAt;
  final String syncState;
  final String payloadJson;
  const ProjectsDriftTableData(
      {required this.id,
      required this.userId,
      required this.version,
      required this.updatedAt,
      this.deletedAt,
      required this.syncState,
      required this.payloadJson});
  @override
  Map<String, Expression> toColumns(bool nullToAbsent) {
    final map = <String, Expression>{};
    map['id'] = Variable<String>(id);
    map['user_id'] = Variable<String>(userId);
    map['version'] = Variable<int>(version);
    map['updated_at'] = Variable<String>(updatedAt);
    if (!nullToAbsent || deletedAt != null) {
      map['deleted_at'] = Variable<String>(deletedAt);
    }
    map['sync_state'] = Variable<String>(syncState);
    map['payload_json'] = Variable<String>(payloadJson);
    return map;
  }

  ProjectsDriftTableCompanion toCompanion(bool nullToAbsent) {
    return ProjectsDriftTableCompanion(
      id: Value(id),
      userId: Value(userId),
      version: Value(version),
      updatedAt: Value(updatedAt),
      deletedAt: deletedAt == null && nullToAbsent
          ? const Value.absent()
          : Value(deletedAt),
      syncState: Value(syncState),
      payloadJson: Value(payloadJson),
    );
  }

  factory ProjectsDriftTableData.fromJson(Map<String, dynamic> json,
      {ValueSerializer? serializer}) {
    serializer ??= driftRuntimeOptions.defaultSerializer;
    return ProjectsDriftTableData(
      id: serializer.fromJson<String>(json['id']),
      userId: serializer.fromJson<String>(json['userId']),
      version: serializer.fromJson<int>(json['version']),
      updatedAt: serializer.fromJson<String>(json['updatedAt']),
      deletedAt: serializer.fromJson<String?>(json['deletedAt']),
      syncState: serializer.fromJson<String>(json['syncState']),
      payloadJson: serializer.fromJson<String>(json['payloadJson']),
    );
  }
  @override
  Map<String, dynamic> toJson({ValueSerializer? serializer}) {
    serializer ??= driftRuntimeOptions.defaultSerializer;
    return <String, dynamic>{
      'id': serializer.toJson<String>(id),
      'userId': serializer.toJson<String>(userId),
      'version': serializer.toJson<int>(version),
      'updatedAt': serializer.toJson<String>(updatedAt),
      'deletedAt': serializer.toJson<String?>(deletedAt),
      'syncState': serializer.toJson<String>(syncState),
      'payloadJson': serializer.toJson<String>(payloadJson),
    };
  }

  ProjectsDriftTableData copyWith(
          {String? id,
          String? userId,
          int? version,
          String? updatedAt,
          Value<String?> deletedAt = const Value.absent(),
          String? syncState,
          String? payloadJson}) =>
      ProjectsDriftTableData(
        id: id ?? this.id,
        userId: userId ?? this.userId,
        version: version ?? this.version,
        updatedAt: updatedAt ?? this.updatedAt,
        deletedAt: deletedAt.present ? deletedAt.value : this.deletedAt,
        syncState: syncState ?? this.syncState,
        payloadJson: payloadJson ?? this.payloadJson,
      );
  ProjectsDriftTableData copyWithCompanion(ProjectsDriftTableCompanion data) {
    return ProjectsDriftTableData(
      id: data.id.present ? data.id.value : this.id,
      userId: data.userId.present ? data.userId.value : this.userId,
      version: data.version.present ? data.version.value : this.version,
      updatedAt: data.updatedAt.present ? data.updatedAt.value : this.updatedAt,
      deletedAt: data.deletedAt.present ? data.deletedAt.value : this.deletedAt,
      syncState: data.syncState.present ? data.syncState.value : this.syncState,
      payloadJson:
          data.payloadJson.present ? data.payloadJson.value : this.payloadJson,
    );
  }

  @override
  String toString() {
    return (StringBuffer('ProjectsDriftTableData(')
          ..write('id: $id, ')
          ..write('userId: $userId, ')
          ..write('version: $version, ')
          ..write('updatedAt: $updatedAt, ')
          ..write('deletedAt: $deletedAt, ')
          ..write('syncState: $syncState, ')
          ..write('payloadJson: $payloadJson')
          ..write(')'))
        .toString();
  }

  @override
  int get hashCode => Object.hash(
      id, userId, version, updatedAt, deletedAt, syncState, payloadJson);
  @override
  bool operator ==(Object other) =>
      identical(this, other) ||
      (other is ProjectsDriftTableData &&
          other.id == this.id &&
          other.userId == this.userId &&
          other.version == this.version &&
          other.updatedAt == this.updatedAt &&
          other.deletedAt == this.deletedAt &&
          other.syncState == this.syncState &&
          other.payloadJson == this.payloadJson);
}

class ProjectsDriftTableCompanion
    extends UpdateCompanion<ProjectsDriftTableData> {
  final Value<String> id;
  final Value<String> userId;
  final Value<int> version;
  final Value<String> updatedAt;
  final Value<String?> deletedAt;
  final Value<String> syncState;
  final Value<String> payloadJson;
  final Value<int> rowid;
  const ProjectsDriftTableCompanion({
    this.id = const Value.absent(),
    this.userId = const Value.absent(),
    this.version = const Value.absent(),
    this.updatedAt = const Value.absent(),
    this.deletedAt = const Value.absent(),
    this.syncState = const Value.absent(),
    this.payloadJson = const Value.absent(),
    this.rowid = const Value.absent(),
  });
  ProjectsDriftTableCompanion.insert({
    required String id,
    required String userId,
    this.version = const Value.absent(),
    required String updatedAt,
    this.deletedAt = const Value.absent(),
    this.syncState = const Value.absent(),
    required String payloadJson,
    this.rowid = const Value.absent(),
  })  : id = Value(id),
        userId = Value(userId),
        updatedAt = Value(updatedAt),
        payloadJson = Value(payloadJson);
  static Insertable<ProjectsDriftTableData> custom({
    Expression<String>? id,
    Expression<String>? userId,
    Expression<int>? version,
    Expression<String>? updatedAt,
    Expression<String>? deletedAt,
    Expression<String>? syncState,
    Expression<String>? payloadJson,
    Expression<int>? rowid,
  }) {
    return RawValuesInsertable({
      if (id != null) 'id': id,
      if (userId != null) 'user_id': userId,
      if (version != null) 'version': version,
      if (updatedAt != null) 'updated_at': updatedAt,
      if (deletedAt != null) 'deleted_at': deletedAt,
      if (syncState != null) 'sync_state': syncState,
      if (payloadJson != null) 'payload_json': payloadJson,
      if (rowid != null) 'rowid': rowid,
    });
  }

  ProjectsDriftTableCompanion copyWith(
      {Value<String>? id,
      Value<String>? userId,
      Value<int>? version,
      Value<String>? updatedAt,
      Value<String?>? deletedAt,
      Value<String>? syncState,
      Value<String>? payloadJson,
      Value<int>? rowid}) {
    return ProjectsDriftTableCompanion(
      id: id ?? this.id,
      userId: userId ?? this.userId,
      version: version ?? this.version,
      updatedAt: updatedAt ?? this.updatedAt,
      deletedAt: deletedAt ?? this.deletedAt,
      syncState: syncState ?? this.syncState,
      payloadJson: payloadJson ?? this.payloadJson,
      rowid: rowid ?? this.rowid,
    );
  }

  @override
  Map<String, Expression> toColumns(bool nullToAbsent) {
    final map = <String, Expression>{};
    if (id.present) {
      map['id'] = Variable<String>(id.value);
    }
    if (userId.present) {
      map['user_id'] = Variable<String>(userId.value);
    }
    if (version.present) {
      map['version'] = Variable<int>(version.value);
    }
    if (updatedAt.present) {
      map['updated_at'] = Variable<String>(updatedAt.value);
    }
    if (deletedAt.present) {
      map['deleted_at'] = Variable<String>(deletedAt.value);
    }
    if (syncState.present) {
      map['sync_state'] = Variable<String>(syncState.value);
    }
    if (payloadJson.present) {
      map['payload_json'] = Variable<String>(payloadJson.value);
    }
    if (rowid.present) {
      map['rowid'] = Variable<int>(rowid.value);
    }
    return map;
  }

  @override
  String toString() {
    return (StringBuffer('ProjectsDriftTableCompanion(')
          ..write('id: $id, ')
          ..write('userId: $userId, ')
          ..write('version: $version, ')
          ..write('updatedAt: $updatedAt, ')
          ..write('deletedAt: $deletedAt, ')
          ..write('syncState: $syncState, ')
          ..write('payloadJson: $payloadJson, ')
          ..write('rowid: $rowid')
          ..write(')'))
        .toString();
  }
}

class $TasksDriftTableTable extends TasksDriftTable
    with TableInfo<$TasksDriftTableTable, TasksDriftTableData> {
  @override
  final GeneratedDatabase attachedDatabase;
  final String? _alias;
  $TasksDriftTableTable(this.attachedDatabase, [this._alias]);
  static const VerificationMeta _idMeta = const VerificationMeta('id');
  @override
  late final GeneratedColumn<String> id = GeneratedColumn<String>(
      'id', aliasedName, false,
      type: DriftSqlType.string, requiredDuringInsert: true);
  static const VerificationMeta _userIdMeta = const VerificationMeta('userId');
  @override
  late final GeneratedColumn<String> userId = GeneratedColumn<String>(
      'user_id', aliasedName, false,
      type: DriftSqlType.string, requiredDuringInsert: true);
  static const VerificationMeta _versionMeta =
      const VerificationMeta('version');
  @override
  late final GeneratedColumn<int> version = GeneratedColumn<int>(
      'version', aliasedName, false,
      type: DriftSqlType.int,
      requiredDuringInsert: false,
      defaultValue: const Constant(0));
  static const VerificationMeta _updatedAtMeta =
      const VerificationMeta('updatedAt');
  @override
  late final GeneratedColumn<String> updatedAt = GeneratedColumn<String>(
      'updated_at', aliasedName, false,
      type: DriftSqlType.string, requiredDuringInsert: true);
  static const VerificationMeta _deletedAtMeta =
      const VerificationMeta('deletedAt');
  @override
  late final GeneratedColumn<String> deletedAt = GeneratedColumn<String>(
      'deleted_at', aliasedName, true,
      type: DriftSqlType.string, requiredDuringInsert: false);
  static const VerificationMeta _syncStateMeta =
      const VerificationMeta('syncState');
  @override
  late final GeneratedColumn<String> syncState = GeneratedColumn<String>(
      'sync_state', aliasedName, false,
      type: DriftSqlType.string,
      requiredDuringInsert: false,
      defaultValue: const Constant('synced'));
  static const VerificationMeta _payloadJsonMeta =
      const VerificationMeta('payloadJson');
  @override
  late final GeneratedColumn<String> payloadJson = GeneratedColumn<String>(
      'payload_json', aliasedName, false,
      type: DriftSqlType.string, requiredDuringInsert: true);
  @override
  List<GeneratedColumn> get $columns =>
      [id, userId, version, updatedAt, deletedAt, syncState, payloadJson];
  @override
  String get aliasedName => _alias ?? actualTableName;
  @override
  String get actualTableName => $name;
  static const String $name = 'tasks';
  @override
  VerificationContext validateIntegrity(
      Insertable<TasksDriftTableData> instance,
      {bool isInserting = false}) {
    final context = VerificationContext();
    final data = instance.toColumns(true);
    if (data.containsKey('id')) {
      context.handle(_idMeta, id.isAcceptableOrUnknown(data['id']!, _idMeta));
    } else if (isInserting) {
      context.missing(_idMeta);
    }
    if (data.containsKey('user_id')) {
      context.handle(_userIdMeta,
          userId.isAcceptableOrUnknown(data['user_id']!, _userIdMeta));
    } else if (isInserting) {
      context.missing(_userIdMeta);
    }
    if (data.containsKey('version')) {
      context.handle(_versionMeta,
          version.isAcceptableOrUnknown(data['version']!, _versionMeta));
    }
    if (data.containsKey('updated_at')) {
      context.handle(_updatedAtMeta,
          updatedAt.isAcceptableOrUnknown(data['updated_at']!, _updatedAtMeta));
    } else if (isInserting) {
      context.missing(_updatedAtMeta);
    }
    if (data.containsKey('deleted_at')) {
      context.handle(_deletedAtMeta,
          deletedAt.isAcceptableOrUnknown(data['deleted_at']!, _deletedAtMeta));
    }
    if (data.containsKey('sync_state')) {
      context.handle(_syncStateMeta,
          syncState.isAcceptableOrUnknown(data['sync_state']!, _syncStateMeta));
    }
    if (data.containsKey('payload_json')) {
      context.handle(
          _payloadJsonMeta,
          payloadJson.isAcceptableOrUnknown(
              data['payload_json']!, _payloadJsonMeta));
    } else if (isInserting) {
      context.missing(_payloadJsonMeta);
    }
    return context;
  }

  @override
  Set<GeneratedColumn> get $primaryKey => {userId, id};
  @override
  TasksDriftTableData map(Map<String, dynamic> data, {String? tablePrefix}) {
    final effectivePrefix = tablePrefix != null ? '$tablePrefix.' : '';
    return TasksDriftTableData(
      id: attachedDatabase.typeMapping
          .read(DriftSqlType.string, data['${effectivePrefix}id'])!,
      userId: attachedDatabase.typeMapping
          .read(DriftSqlType.string, data['${effectivePrefix}user_id'])!,
      version: attachedDatabase.typeMapping
          .read(DriftSqlType.int, data['${effectivePrefix}version'])!,
      updatedAt: attachedDatabase.typeMapping
          .read(DriftSqlType.string, data['${effectivePrefix}updated_at'])!,
      deletedAt: attachedDatabase.typeMapping
          .read(DriftSqlType.string, data['${effectivePrefix}deleted_at']),
      syncState: attachedDatabase.typeMapping
          .read(DriftSqlType.string, data['${effectivePrefix}sync_state'])!,
      payloadJson: attachedDatabase.typeMapping
          .read(DriftSqlType.string, data['${effectivePrefix}payload_json'])!,
    );
  }

  @override
  $TasksDriftTableTable createAlias(String alias) {
    return $TasksDriftTableTable(attachedDatabase, alias);
  }
}

class TasksDriftTableData extends DataClass
    implements Insertable<TasksDriftTableData> {
  final String id;
  final String userId;
  final int version;
  final String updatedAt;
  final String? deletedAt;
  final String syncState;
  final String payloadJson;
  const TasksDriftTableData(
      {required this.id,
      required this.userId,
      required this.version,
      required this.updatedAt,
      this.deletedAt,
      required this.syncState,
      required this.payloadJson});
  @override
  Map<String, Expression> toColumns(bool nullToAbsent) {
    final map = <String, Expression>{};
    map['id'] = Variable<String>(id);
    map['user_id'] = Variable<String>(userId);
    map['version'] = Variable<int>(version);
    map['updated_at'] = Variable<String>(updatedAt);
    if (!nullToAbsent || deletedAt != null) {
      map['deleted_at'] = Variable<String>(deletedAt);
    }
    map['sync_state'] = Variable<String>(syncState);
    map['payload_json'] = Variable<String>(payloadJson);
    return map;
  }

  TasksDriftTableCompanion toCompanion(bool nullToAbsent) {
    return TasksDriftTableCompanion(
      id: Value(id),
      userId: Value(userId),
      version: Value(version),
      updatedAt: Value(updatedAt),
      deletedAt: deletedAt == null && nullToAbsent
          ? const Value.absent()
          : Value(deletedAt),
      syncState: Value(syncState),
      payloadJson: Value(payloadJson),
    );
  }

  factory TasksDriftTableData.fromJson(Map<String, dynamic> json,
      {ValueSerializer? serializer}) {
    serializer ??= driftRuntimeOptions.defaultSerializer;
    return TasksDriftTableData(
      id: serializer.fromJson<String>(json['id']),
      userId: serializer.fromJson<String>(json['userId']),
      version: serializer.fromJson<int>(json['version']),
      updatedAt: serializer.fromJson<String>(json['updatedAt']),
      deletedAt: serializer.fromJson<String?>(json['deletedAt']),
      syncState: serializer.fromJson<String>(json['syncState']),
      payloadJson: serializer.fromJson<String>(json['payloadJson']),
    );
  }
  @override
  Map<String, dynamic> toJson({ValueSerializer? serializer}) {
    serializer ??= driftRuntimeOptions.defaultSerializer;
    return <String, dynamic>{
      'id': serializer.toJson<String>(id),
      'userId': serializer.toJson<String>(userId),
      'version': serializer.toJson<int>(version),
      'updatedAt': serializer.toJson<String>(updatedAt),
      'deletedAt': serializer.toJson<String?>(deletedAt),
      'syncState': serializer.toJson<String>(syncState),
      'payloadJson': serializer.toJson<String>(payloadJson),
    };
  }

  TasksDriftTableData copyWith(
          {String? id,
          String? userId,
          int? version,
          String? updatedAt,
          Value<String?> deletedAt = const Value.absent(),
          String? syncState,
          String? payloadJson}) =>
      TasksDriftTableData(
        id: id ?? this.id,
        userId: userId ?? this.userId,
        version: version ?? this.version,
        updatedAt: updatedAt ?? this.updatedAt,
        deletedAt: deletedAt.present ? deletedAt.value : this.deletedAt,
        syncState: syncState ?? this.syncState,
        payloadJson: payloadJson ?? this.payloadJson,
      );
  TasksDriftTableData copyWithCompanion(TasksDriftTableCompanion data) {
    return TasksDriftTableData(
      id: data.id.present ? data.id.value : this.id,
      userId: data.userId.present ? data.userId.value : this.userId,
      version: data.version.present ? data.version.value : this.version,
      updatedAt: data.updatedAt.present ? data.updatedAt.value : this.updatedAt,
      deletedAt: data.deletedAt.present ? data.deletedAt.value : this.deletedAt,
      syncState: data.syncState.present ? data.syncState.value : this.syncState,
      payloadJson:
          data.payloadJson.present ? data.payloadJson.value : this.payloadJson,
    );
  }

  @override
  String toString() {
    return (StringBuffer('TasksDriftTableData(')
          ..write('id: $id, ')
          ..write('userId: $userId, ')
          ..write('version: $version, ')
          ..write('updatedAt: $updatedAt, ')
          ..write('deletedAt: $deletedAt, ')
          ..write('syncState: $syncState, ')
          ..write('payloadJson: $payloadJson')
          ..write(')'))
        .toString();
  }

  @override
  int get hashCode => Object.hash(
      id, userId, version, updatedAt, deletedAt, syncState, payloadJson);
  @override
  bool operator ==(Object other) =>
      identical(this, other) ||
      (other is TasksDriftTableData &&
          other.id == this.id &&
          other.userId == this.userId &&
          other.version == this.version &&
          other.updatedAt == this.updatedAt &&
          other.deletedAt == this.deletedAt &&
          other.syncState == this.syncState &&
          other.payloadJson == this.payloadJson);
}

class TasksDriftTableCompanion extends UpdateCompanion<TasksDriftTableData> {
  final Value<String> id;
  final Value<String> userId;
  final Value<int> version;
  final Value<String> updatedAt;
  final Value<String?> deletedAt;
  final Value<String> syncState;
  final Value<String> payloadJson;
  final Value<int> rowid;
  const TasksDriftTableCompanion({
    this.id = const Value.absent(),
    this.userId = const Value.absent(),
    this.version = const Value.absent(),
    this.updatedAt = const Value.absent(),
    this.deletedAt = const Value.absent(),
    this.syncState = const Value.absent(),
    this.payloadJson = const Value.absent(),
    this.rowid = const Value.absent(),
  });
  TasksDriftTableCompanion.insert({
    required String id,
    required String userId,
    this.version = const Value.absent(),
    required String updatedAt,
    this.deletedAt = const Value.absent(),
    this.syncState = const Value.absent(),
    required String payloadJson,
    this.rowid = const Value.absent(),
  })  : id = Value(id),
        userId = Value(userId),
        updatedAt = Value(updatedAt),
        payloadJson = Value(payloadJson);
  static Insertable<TasksDriftTableData> custom({
    Expression<String>? id,
    Expression<String>? userId,
    Expression<int>? version,
    Expression<String>? updatedAt,
    Expression<String>? deletedAt,
    Expression<String>? syncState,
    Expression<String>? payloadJson,
    Expression<int>? rowid,
  }) {
    return RawValuesInsertable({
      if (id != null) 'id': id,
      if (userId != null) 'user_id': userId,
      if (version != null) 'version': version,
      if (updatedAt != null) 'updated_at': updatedAt,
      if (deletedAt != null) 'deleted_at': deletedAt,
      if (syncState != null) 'sync_state': syncState,
      if (payloadJson != null) 'payload_json': payloadJson,
      if (rowid != null) 'rowid': rowid,
    });
  }

  TasksDriftTableCompanion copyWith(
      {Value<String>? id,
      Value<String>? userId,
      Value<int>? version,
      Value<String>? updatedAt,
      Value<String?>? deletedAt,
      Value<String>? syncState,
      Value<String>? payloadJson,
      Value<int>? rowid}) {
    return TasksDriftTableCompanion(
      id: id ?? this.id,
      userId: userId ?? this.userId,
      version: version ?? this.version,
      updatedAt: updatedAt ?? this.updatedAt,
      deletedAt: deletedAt ?? this.deletedAt,
      syncState: syncState ?? this.syncState,
      payloadJson: payloadJson ?? this.payloadJson,
      rowid: rowid ?? this.rowid,
    );
  }

  @override
  Map<String, Expression> toColumns(bool nullToAbsent) {
    final map = <String, Expression>{};
    if (id.present) {
      map['id'] = Variable<String>(id.value);
    }
    if (userId.present) {
      map['user_id'] = Variable<String>(userId.value);
    }
    if (version.present) {
      map['version'] = Variable<int>(version.value);
    }
    if (updatedAt.present) {
      map['updated_at'] = Variable<String>(updatedAt.value);
    }
    if (deletedAt.present) {
      map['deleted_at'] = Variable<String>(deletedAt.value);
    }
    if (syncState.present) {
      map['sync_state'] = Variable<String>(syncState.value);
    }
    if (payloadJson.present) {
      map['payload_json'] = Variable<String>(payloadJson.value);
    }
    if (rowid.present) {
      map['rowid'] = Variable<int>(rowid.value);
    }
    return map;
  }

  @override
  String toString() {
    return (StringBuffer('TasksDriftTableCompanion(')
          ..write('id: $id, ')
          ..write('userId: $userId, ')
          ..write('version: $version, ')
          ..write('updatedAt: $updatedAt, ')
          ..write('deletedAt: $deletedAt, ')
          ..write('syncState: $syncState, ')
          ..write('payloadJson: $payloadJson, ')
          ..write('rowid: $rowid')
          ..write(')'))
        .toString();
  }
}

class $HabitsDriftTableTable extends HabitsDriftTable
    with TableInfo<$HabitsDriftTableTable, HabitsDriftTableData> {
  @override
  final GeneratedDatabase attachedDatabase;
  final String? _alias;
  $HabitsDriftTableTable(this.attachedDatabase, [this._alias]);
  static const VerificationMeta _idMeta = const VerificationMeta('id');
  @override
  late final GeneratedColumn<String> id = GeneratedColumn<String>(
      'id', aliasedName, false,
      type: DriftSqlType.string, requiredDuringInsert: true);
  static const VerificationMeta _userIdMeta = const VerificationMeta('userId');
  @override
  late final GeneratedColumn<String> userId = GeneratedColumn<String>(
      'user_id', aliasedName, false,
      type: DriftSqlType.string, requiredDuringInsert: true);
  static const VerificationMeta _versionMeta =
      const VerificationMeta('version');
  @override
  late final GeneratedColumn<int> version = GeneratedColumn<int>(
      'version', aliasedName, false,
      type: DriftSqlType.int,
      requiredDuringInsert: false,
      defaultValue: const Constant(0));
  static const VerificationMeta _updatedAtMeta =
      const VerificationMeta('updatedAt');
  @override
  late final GeneratedColumn<String> updatedAt = GeneratedColumn<String>(
      'updated_at', aliasedName, false,
      type: DriftSqlType.string, requiredDuringInsert: true);
  static const VerificationMeta _deletedAtMeta =
      const VerificationMeta('deletedAt');
  @override
  late final GeneratedColumn<String> deletedAt = GeneratedColumn<String>(
      'deleted_at', aliasedName, true,
      type: DriftSqlType.string, requiredDuringInsert: false);
  static const VerificationMeta _syncStateMeta =
      const VerificationMeta('syncState');
  @override
  late final GeneratedColumn<String> syncState = GeneratedColumn<String>(
      'sync_state', aliasedName, false,
      type: DriftSqlType.string,
      requiredDuringInsert: false,
      defaultValue: const Constant('synced'));
  static const VerificationMeta _payloadJsonMeta =
      const VerificationMeta('payloadJson');
  @override
  late final GeneratedColumn<String> payloadJson = GeneratedColumn<String>(
      'payload_json', aliasedName, false,
      type: DriftSqlType.string, requiredDuringInsert: true);
  @override
  List<GeneratedColumn> get $columns =>
      [id, userId, version, updatedAt, deletedAt, syncState, payloadJson];
  @override
  String get aliasedName => _alias ?? actualTableName;
  @override
  String get actualTableName => $name;
  static const String $name = 'habits';
  @override
  VerificationContext validateIntegrity(
      Insertable<HabitsDriftTableData> instance,
      {bool isInserting = false}) {
    final context = VerificationContext();
    final data = instance.toColumns(true);
    if (data.containsKey('id')) {
      context.handle(_idMeta, id.isAcceptableOrUnknown(data['id']!, _idMeta));
    } else if (isInserting) {
      context.missing(_idMeta);
    }
    if (data.containsKey('user_id')) {
      context.handle(_userIdMeta,
          userId.isAcceptableOrUnknown(data['user_id']!, _userIdMeta));
    } else if (isInserting) {
      context.missing(_userIdMeta);
    }
    if (data.containsKey('version')) {
      context.handle(_versionMeta,
          version.isAcceptableOrUnknown(data['version']!, _versionMeta));
    }
    if (data.containsKey('updated_at')) {
      context.handle(_updatedAtMeta,
          updatedAt.isAcceptableOrUnknown(data['updated_at']!, _updatedAtMeta));
    } else if (isInserting) {
      context.missing(_updatedAtMeta);
    }
    if (data.containsKey('deleted_at')) {
      context.handle(_deletedAtMeta,
          deletedAt.isAcceptableOrUnknown(data['deleted_at']!, _deletedAtMeta));
    }
    if (data.containsKey('sync_state')) {
      context.handle(_syncStateMeta,
          syncState.isAcceptableOrUnknown(data['sync_state']!, _syncStateMeta));
    }
    if (data.containsKey('payload_json')) {
      context.handle(
          _payloadJsonMeta,
          payloadJson.isAcceptableOrUnknown(
              data['payload_json']!, _payloadJsonMeta));
    } else if (isInserting) {
      context.missing(_payloadJsonMeta);
    }
    return context;
  }

  @override
  Set<GeneratedColumn> get $primaryKey => {userId, id};
  @override
  HabitsDriftTableData map(Map<String, dynamic> data, {String? tablePrefix}) {
    final effectivePrefix = tablePrefix != null ? '$tablePrefix.' : '';
    return HabitsDriftTableData(
      id: attachedDatabase.typeMapping
          .read(DriftSqlType.string, data['${effectivePrefix}id'])!,
      userId: attachedDatabase.typeMapping
          .read(DriftSqlType.string, data['${effectivePrefix}user_id'])!,
      version: attachedDatabase.typeMapping
          .read(DriftSqlType.int, data['${effectivePrefix}version'])!,
      updatedAt: attachedDatabase.typeMapping
          .read(DriftSqlType.string, data['${effectivePrefix}updated_at'])!,
      deletedAt: attachedDatabase.typeMapping
          .read(DriftSqlType.string, data['${effectivePrefix}deleted_at']),
      syncState: attachedDatabase.typeMapping
          .read(DriftSqlType.string, data['${effectivePrefix}sync_state'])!,
      payloadJson: attachedDatabase.typeMapping
          .read(DriftSqlType.string, data['${effectivePrefix}payload_json'])!,
    );
  }

  @override
  $HabitsDriftTableTable createAlias(String alias) {
    return $HabitsDriftTableTable(attachedDatabase, alias);
  }
}

class HabitsDriftTableData extends DataClass
    implements Insertable<HabitsDriftTableData> {
  final String id;
  final String userId;
  final int version;
  final String updatedAt;
  final String? deletedAt;
  final String syncState;
  final String payloadJson;
  const HabitsDriftTableData(
      {required this.id,
      required this.userId,
      required this.version,
      required this.updatedAt,
      this.deletedAt,
      required this.syncState,
      required this.payloadJson});
  @override
  Map<String, Expression> toColumns(bool nullToAbsent) {
    final map = <String, Expression>{};
    map['id'] = Variable<String>(id);
    map['user_id'] = Variable<String>(userId);
    map['version'] = Variable<int>(version);
    map['updated_at'] = Variable<String>(updatedAt);
    if (!nullToAbsent || deletedAt != null) {
      map['deleted_at'] = Variable<String>(deletedAt);
    }
    map['sync_state'] = Variable<String>(syncState);
    map['payload_json'] = Variable<String>(payloadJson);
    return map;
  }

  HabitsDriftTableCompanion toCompanion(bool nullToAbsent) {
    return HabitsDriftTableCompanion(
      id: Value(id),
      userId: Value(userId),
      version: Value(version),
      updatedAt: Value(updatedAt),
      deletedAt: deletedAt == null && nullToAbsent
          ? const Value.absent()
          : Value(deletedAt),
      syncState: Value(syncState),
      payloadJson: Value(payloadJson),
    );
  }

  factory HabitsDriftTableData.fromJson(Map<String, dynamic> json,
      {ValueSerializer? serializer}) {
    serializer ??= driftRuntimeOptions.defaultSerializer;
    return HabitsDriftTableData(
      id: serializer.fromJson<String>(json['id']),
      userId: serializer.fromJson<String>(json['userId']),
      version: serializer.fromJson<int>(json['version']),
      updatedAt: serializer.fromJson<String>(json['updatedAt']),
      deletedAt: serializer.fromJson<String?>(json['deletedAt']),
      syncState: serializer.fromJson<String>(json['syncState']),
      payloadJson: serializer.fromJson<String>(json['payloadJson']),
    );
  }
  @override
  Map<String, dynamic> toJson({ValueSerializer? serializer}) {
    serializer ??= driftRuntimeOptions.defaultSerializer;
    return <String, dynamic>{
      'id': serializer.toJson<String>(id),
      'userId': serializer.toJson<String>(userId),
      'version': serializer.toJson<int>(version),
      'updatedAt': serializer.toJson<String>(updatedAt),
      'deletedAt': serializer.toJson<String?>(deletedAt),
      'syncState': serializer.toJson<String>(syncState),
      'payloadJson': serializer.toJson<String>(payloadJson),
    };
  }

  HabitsDriftTableData copyWith(
          {String? id,
          String? userId,
          int? version,
          String? updatedAt,
          Value<String?> deletedAt = const Value.absent(),
          String? syncState,
          String? payloadJson}) =>
      HabitsDriftTableData(
        id: id ?? this.id,
        userId: userId ?? this.userId,
        version: version ?? this.version,
        updatedAt: updatedAt ?? this.updatedAt,
        deletedAt: deletedAt.present ? deletedAt.value : this.deletedAt,
        syncState: syncState ?? this.syncState,
        payloadJson: payloadJson ?? this.payloadJson,
      );
  HabitsDriftTableData copyWithCompanion(HabitsDriftTableCompanion data) {
    return HabitsDriftTableData(
      id: data.id.present ? data.id.value : this.id,
      userId: data.userId.present ? data.userId.value : this.userId,
      version: data.version.present ? data.version.value : this.version,
      updatedAt: data.updatedAt.present ? data.updatedAt.value : this.updatedAt,
      deletedAt: data.deletedAt.present ? data.deletedAt.value : this.deletedAt,
      syncState: data.syncState.present ? data.syncState.value : this.syncState,
      payloadJson:
          data.payloadJson.present ? data.payloadJson.value : this.payloadJson,
    );
  }

  @override
  String toString() {
    return (StringBuffer('HabitsDriftTableData(')
          ..write('id: $id, ')
          ..write('userId: $userId, ')
          ..write('version: $version, ')
          ..write('updatedAt: $updatedAt, ')
          ..write('deletedAt: $deletedAt, ')
          ..write('syncState: $syncState, ')
          ..write('payloadJson: $payloadJson')
          ..write(')'))
        .toString();
  }

  @override
  int get hashCode => Object.hash(
      id, userId, version, updatedAt, deletedAt, syncState, payloadJson);
  @override
  bool operator ==(Object other) =>
      identical(this, other) ||
      (other is HabitsDriftTableData &&
          other.id == this.id &&
          other.userId == this.userId &&
          other.version == this.version &&
          other.updatedAt == this.updatedAt &&
          other.deletedAt == this.deletedAt &&
          other.syncState == this.syncState &&
          other.payloadJson == this.payloadJson);
}

class HabitsDriftTableCompanion extends UpdateCompanion<HabitsDriftTableData> {
  final Value<String> id;
  final Value<String> userId;
  final Value<int> version;
  final Value<String> updatedAt;
  final Value<String?> deletedAt;
  final Value<String> syncState;
  final Value<String> payloadJson;
  final Value<int> rowid;
  const HabitsDriftTableCompanion({
    this.id = const Value.absent(),
    this.userId = const Value.absent(),
    this.version = const Value.absent(),
    this.updatedAt = const Value.absent(),
    this.deletedAt = const Value.absent(),
    this.syncState = const Value.absent(),
    this.payloadJson = const Value.absent(),
    this.rowid = const Value.absent(),
  });
  HabitsDriftTableCompanion.insert({
    required String id,
    required String userId,
    this.version = const Value.absent(),
    required String updatedAt,
    this.deletedAt = const Value.absent(),
    this.syncState = const Value.absent(),
    required String payloadJson,
    this.rowid = const Value.absent(),
  })  : id = Value(id),
        userId = Value(userId),
        updatedAt = Value(updatedAt),
        payloadJson = Value(payloadJson);
  static Insertable<HabitsDriftTableData> custom({
    Expression<String>? id,
    Expression<String>? userId,
    Expression<int>? version,
    Expression<String>? updatedAt,
    Expression<String>? deletedAt,
    Expression<String>? syncState,
    Expression<String>? payloadJson,
    Expression<int>? rowid,
  }) {
    return RawValuesInsertable({
      if (id != null) 'id': id,
      if (userId != null) 'user_id': userId,
      if (version != null) 'version': version,
      if (updatedAt != null) 'updated_at': updatedAt,
      if (deletedAt != null) 'deleted_at': deletedAt,
      if (syncState != null) 'sync_state': syncState,
      if (payloadJson != null) 'payload_json': payloadJson,
      if (rowid != null) 'rowid': rowid,
    });
  }

  HabitsDriftTableCompanion copyWith(
      {Value<String>? id,
      Value<String>? userId,
      Value<int>? version,
      Value<String>? updatedAt,
      Value<String?>? deletedAt,
      Value<String>? syncState,
      Value<String>? payloadJson,
      Value<int>? rowid}) {
    return HabitsDriftTableCompanion(
      id: id ?? this.id,
      userId: userId ?? this.userId,
      version: version ?? this.version,
      updatedAt: updatedAt ?? this.updatedAt,
      deletedAt: deletedAt ?? this.deletedAt,
      syncState: syncState ?? this.syncState,
      payloadJson: payloadJson ?? this.payloadJson,
      rowid: rowid ?? this.rowid,
    );
  }

  @override
  Map<String, Expression> toColumns(bool nullToAbsent) {
    final map = <String, Expression>{};
    if (id.present) {
      map['id'] = Variable<String>(id.value);
    }
    if (userId.present) {
      map['user_id'] = Variable<String>(userId.value);
    }
    if (version.present) {
      map['version'] = Variable<int>(version.value);
    }
    if (updatedAt.present) {
      map['updated_at'] = Variable<String>(updatedAt.value);
    }
    if (deletedAt.present) {
      map['deleted_at'] = Variable<String>(deletedAt.value);
    }
    if (syncState.present) {
      map['sync_state'] = Variable<String>(syncState.value);
    }
    if (payloadJson.present) {
      map['payload_json'] = Variable<String>(payloadJson.value);
    }
    if (rowid.present) {
      map['rowid'] = Variable<int>(rowid.value);
    }
    return map;
  }

  @override
  String toString() {
    return (StringBuffer('HabitsDriftTableCompanion(')
          ..write('id: $id, ')
          ..write('userId: $userId, ')
          ..write('version: $version, ')
          ..write('updatedAt: $updatedAt, ')
          ..write('deletedAt: $deletedAt, ')
          ..write('syncState: $syncState, ')
          ..write('payloadJson: $payloadJson, ')
          ..write('rowid: $rowid')
          ..write(')'))
        .toString();
  }
}

class $HabitCheckinsDriftTableTable extends HabitCheckinsDriftTable
    with TableInfo<$HabitCheckinsDriftTableTable, HabitCheckinsDriftTableData> {
  @override
  final GeneratedDatabase attachedDatabase;
  final String? _alias;
  $HabitCheckinsDriftTableTable(this.attachedDatabase, [this._alias]);
  static const VerificationMeta _idMeta = const VerificationMeta('id');
  @override
  late final GeneratedColumn<String> id = GeneratedColumn<String>(
      'id', aliasedName, false,
      type: DriftSqlType.string, requiredDuringInsert: true);
  static const VerificationMeta _userIdMeta = const VerificationMeta('userId');
  @override
  late final GeneratedColumn<String> userId = GeneratedColumn<String>(
      'user_id', aliasedName, false,
      type: DriftSqlType.string, requiredDuringInsert: true);
  static const VerificationMeta _versionMeta =
      const VerificationMeta('version');
  @override
  late final GeneratedColumn<int> version = GeneratedColumn<int>(
      'version', aliasedName, false,
      type: DriftSqlType.int,
      requiredDuringInsert: false,
      defaultValue: const Constant(0));
  static const VerificationMeta _updatedAtMeta =
      const VerificationMeta('updatedAt');
  @override
  late final GeneratedColumn<String> updatedAt = GeneratedColumn<String>(
      'updated_at', aliasedName, false,
      type: DriftSqlType.string, requiredDuringInsert: true);
  static const VerificationMeta _deletedAtMeta =
      const VerificationMeta('deletedAt');
  @override
  late final GeneratedColumn<String> deletedAt = GeneratedColumn<String>(
      'deleted_at', aliasedName, true,
      type: DriftSqlType.string, requiredDuringInsert: false);
  static const VerificationMeta _syncStateMeta =
      const VerificationMeta('syncState');
  @override
  late final GeneratedColumn<String> syncState = GeneratedColumn<String>(
      'sync_state', aliasedName, false,
      type: DriftSqlType.string,
      requiredDuringInsert: false,
      defaultValue: const Constant('synced'));
  static const VerificationMeta _payloadJsonMeta =
      const VerificationMeta('payloadJson');
  @override
  late final GeneratedColumn<String> payloadJson = GeneratedColumn<String>(
      'payload_json', aliasedName, false,
      type: DriftSqlType.string, requiredDuringInsert: true);
  @override
  List<GeneratedColumn> get $columns =>
      [id, userId, version, updatedAt, deletedAt, syncState, payloadJson];
  @override
  String get aliasedName => _alias ?? actualTableName;
  @override
  String get actualTableName => $name;
  static const String $name = 'habit_checkins';
  @override
  VerificationContext validateIntegrity(
      Insertable<HabitCheckinsDriftTableData> instance,
      {bool isInserting = false}) {
    final context = VerificationContext();
    final data = instance.toColumns(true);
    if (data.containsKey('id')) {
      context.handle(_idMeta, id.isAcceptableOrUnknown(data['id']!, _idMeta));
    } else if (isInserting) {
      context.missing(_idMeta);
    }
    if (data.containsKey('user_id')) {
      context.handle(_userIdMeta,
          userId.isAcceptableOrUnknown(data['user_id']!, _userIdMeta));
    } else if (isInserting) {
      context.missing(_userIdMeta);
    }
    if (data.containsKey('version')) {
      context.handle(_versionMeta,
          version.isAcceptableOrUnknown(data['version']!, _versionMeta));
    }
    if (data.containsKey('updated_at')) {
      context.handle(_updatedAtMeta,
          updatedAt.isAcceptableOrUnknown(data['updated_at']!, _updatedAtMeta));
    } else if (isInserting) {
      context.missing(_updatedAtMeta);
    }
    if (data.containsKey('deleted_at')) {
      context.handle(_deletedAtMeta,
          deletedAt.isAcceptableOrUnknown(data['deleted_at']!, _deletedAtMeta));
    }
    if (data.containsKey('sync_state')) {
      context.handle(_syncStateMeta,
          syncState.isAcceptableOrUnknown(data['sync_state']!, _syncStateMeta));
    }
    if (data.containsKey('payload_json')) {
      context.handle(
          _payloadJsonMeta,
          payloadJson.isAcceptableOrUnknown(
              data['payload_json']!, _payloadJsonMeta));
    } else if (isInserting) {
      context.missing(_payloadJsonMeta);
    }
    return context;
  }

  @override
  Set<GeneratedColumn> get $primaryKey => {userId, id};
  @override
  HabitCheckinsDriftTableData map(Map<String, dynamic> data,
      {String? tablePrefix}) {
    final effectivePrefix = tablePrefix != null ? '$tablePrefix.' : '';
    return HabitCheckinsDriftTableData(
      id: attachedDatabase.typeMapping
          .read(DriftSqlType.string, data['${effectivePrefix}id'])!,
      userId: attachedDatabase.typeMapping
          .read(DriftSqlType.string, data['${effectivePrefix}user_id'])!,
      version: attachedDatabase.typeMapping
          .read(DriftSqlType.int, data['${effectivePrefix}version'])!,
      updatedAt: attachedDatabase.typeMapping
          .read(DriftSqlType.string, data['${effectivePrefix}updated_at'])!,
      deletedAt: attachedDatabase.typeMapping
          .read(DriftSqlType.string, data['${effectivePrefix}deleted_at']),
      syncState: attachedDatabase.typeMapping
          .read(DriftSqlType.string, data['${effectivePrefix}sync_state'])!,
      payloadJson: attachedDatabase.typeMapping
          .read(DriftSqlType.string, data['${effectivePrefix}payload_json'])!,
    );
  }

  @override
  $HabitCheckinsDriftTableTable createAlias(String alias) {
    return $HabitCheckinsDriftTableTable(attachedDatabase, alias);
  }
}

class HabitCheckinsDriftTableData extends DataClass
    implements Insertable<HabitCheckinsDriftTableData> {
  final String id;
  final String userId;
  final int version;
  final String updatedAt;
  final String? deletedAt;
  final String syncState;
  final String payloadJson;
  const HabitCheckinsDriftTableData(
      {required this.id,
      required this.userId,
      required this.version,
      required this.updatedAt,
      this.deletedAt,
      required this.syncState,
      required this.payloadJson});
  @override
  Map<String, Expression> toColumns(bool nullToAbsent) {
    final map = <String, Expression>{};
    map['id'] = Variable<String>(id);
    map['user_id'] = Variable<String>(userId);
    map['version'] = Variable<int>(version);
    map['updated_at'] = Variable<String>(updatedAt);
    if (!nullToAbsent || deletedAt != null) {
      map['deleted_at'] = Variable<String>(deletedAt);
    }
    map['sync_state'] = Variable<String>(syncState);
    map['payload_json'] = Variable<String>(payloadJson);
    return map;
  }

  HabitCheckinsDriftTableCompanion toCompanion(bool nullToAbsent) {
    return HabitCheckinsDriftTableCompanion(
      id: Value(id),
      userId: Value(userId),
      version: Value(version),
      updatedAt: Value(updatedAt),
      deletedAt: deletedAt == null && nullToAbsent
          ? const Value.absent()
          : Value(deletedAt),
      syncState: Value(syncState),
      payloadJson: Value(payloadJson),
    );
  }

  factory HabitCheckinsDriftTableData.fromJson(Map<String, dynamic> json,
      {ValueSerializer? serializer}) {
    serializer ??= driftRuntimeOptions.defaultSerializer;
    return HabitCheckinsDriftTableData(
      id: serializer.fromJson<String>(json['id']),
      userId: serializer.fromJson<String>(json['userId']),
      version: serializer.fromJson<int>(json['version']),
      updatedAt: serializer.fromJson<String>(json['updatedAt']),
      deletedAt: serializer.fromJson<String?>(json['deletedAt']),
      syncState: serializer.fromJson<String>(json['syncState']),
      payloadJson: serializer.fromJson<String>(json['payloadJson']),
    );
  }
  @override
  Map<String, dynamic> toJson({ValueSerializer? serializer}) {
    serializer ??= driftRuntimeOptions.defaultSerializer;
    return <String, dynamic>{
      'id': serializer.toJson<String>(id),
      'userId': serializer.toJson<String>(userId),
      'version': serializer.toJson<int>(version),
      'updatedAt': serializer.toJson<String>(updatedAt),
      'deletedAt': serializer.toJson<String?>(deletedAt),
      'syncState': serializer.toJson<String>(syncState),
      'payloadJson': serializer.toJson<String>(payloadJson),
    };
  }

  HabitCheckinsDriftTableData copyWith(
          {String? id,
          String? userId,
          int? version,
          String? updatedAt,
          Value<String?> deletedAt = const Value.absent(),
          String? syncState,
          String? payloadJson}) =>
      HabitCheckinsDriftTableData(
        id: id ?? this.id,
        userId: userId ?? this.userId,
        version: version ?? this.version,
        updatedAt: updatedAt ?? this.updatedAt,
        deletedAt: deletedAt.present ? deletedAt.value : this.deletedAt,
        syncState: syncState ?? this.syncState,
        payloadJson: payloadJson ?? this.payloadJson,
      );
  HabitCheckinsDriftTableData copyWithCompanion(
      HabitCheckinsDriftTableCompanion data) {
    return HabitCheckinsDriftTableData(
      id: data.id.present ? data.id.value : this.id,
      userId: data.userId.present ? data.userId.value : this.userId,
      version: data.version.present ? data.version.value : this.version,
      updatedAt: data.updatedAt.present ? data.updatedAt.value : this.updatedAt,
      deletedAt: data.deletedAt.present ? data.deletedAt.value : this.deletedAt,
      syncState: data.syncState.present ? data.syncState.value : this.syncState,
      payloadJson:
          data.payloadJson.present ? data.payloadJson.value : this.payloadJson,
    );
  }

  @override
  String toString() {
    return (StringBuffer('HabitCheckinsDriftTableData(')
          ..write('id: $id, ')
          ..write('userId: $userId, ')
          ..write('version: $version, ')
          ..write('updatedAt: $updatedAt, ')
          ..write('deletedAt: $deletedAt, ')
          ..write('syncState: $syncState, ')
          ..write('payloadJson: $payloadJson')
          ..write(')'))
        .toString();
  }

  @override
  int get hashCode => Object.hash(
      id, userId, version, updatedAt, deletedAt, syncState, payloadJson);
  @override
  bool operator ==(Object other) =>
      identical(this, other) ||
      (other is HabitCheckinsDriftTableData &&
          other.id == this.id &&
          other.userId == this.userId &&
          other.version == this.version &&
          other.updatedAt == this.updatedAt &&
          other.deletedAt == this.deletedAt &&
          other.syncState == this.syncState &&
          other.payloadJson == this.payloadJson);
}

class HabitCheckinsDriftTableCompanion
    extends UpdateCompanion<HabitCheckinsDriftTableData> {
  final Value<String> id;
  final Value<String> userId;
  final Value<int> version;
  final Value<String> updatedAt;
  final Value<String?> deletedAt;
  final Value<String> syncState;
  final Value<String> payloadJson;
  final Value<int> rowid;
  const HabitCheckinsDriftTableCompanion({
    this.id = const Value.absent(),
    this.userId = const Value.absent(),
    this.version = const Value.absent(),
    this.updatedAt = const Value.absent(),
    this.deletedAt = const Value.absent(),
    this.syncState = const Value.absent(),
    this.payloadJson = const Value.absent(),
    this.rowid = const Value.absent(),
  });
  HabitCheckinsDriftTableCompanion.insert({
    required String id,
    required String userId,
    this.version = const Value.absent(),
    required String updatedAt,
    this.deletedAt = const Value.absent(),
    this.syncState = const Value.absent(),
    required String payloadJson,
    this.rowid = const Value.absent(),
  })  : id = Value(id),
        userId = Value(userId),
        updatedAt = Value(updatedAt),
        payloadJson = Value(payloadJson);
  static Insertable<HabitCheckinsDriftTableData> custom({
    Expression<String>? id,
    Expression<String>? userId,
    Expression<int>? version,
    Expression<String>? updatedAt,
    Expression<String>? deletedAt,
    Expression<String>? syncState,
    Expression<String>? payloadJson,
    Expression<int>? rowid,
  }) {
    return RawValuesInsertable({
      if (id != null) 'id': id,
      if (userId != null) 'user_id': userId,
      if (version != null) 'version': version,
      if (updatedAt != null) 'updated_at': updatedAt,
      if (deletedAt != null) 'deleted_at': deletedAt,
      if (syncState != null) 'sync_state': syncState,
      if (payloadJson != null) 'payload_json': payloadJson,
      if (rowid != null) 'rowid': rowid,
    });
  }

  HabitCheckinsDriftTableCompanion copyWith(
      {Value<String>? id,
      Value<String>? userId,
      Value<int>? version,
      Value<String>? updatedAt,
      Value<String?>? deletedAt,
      Value<String>? syncState,
      Value<String>? payloadJson,
      Value<int>? rowid}) {
    return HabitCheckinsDriftTableCompanion(
      id: id ?? this.id,
      userId: userId ?? this.userId,
      version: version ?? this.version,
      updatedAt: updatedAt ?? this.updatedAt,
      deletedAt: deletedAt ?? this.deletedAt,
      syncState: syncState ?? this.syncState,
      payloadJson: payloadJson ?? this.payloadJson,
      rowid: rowid ?? this.rowid,
    );
  }

  @override
  Map<String, Expression> toColumns(bool nullToAbsent) {
    final map = <String, Expression>{};
    if (id.present) {
      map['id'] = Variable<String>(id.value);
    }
    if (userId.present) {
      map['user_id'] = Variable<String>(userId.value);
    }
    if (version.present) {
      map['version'] = Variable<int>(version.value);
    }
    if (updatedAt.present) {
      map['updated_at'] = Variable<String>(updatedAt.value);
    }
    if (deletedAt.present) {
      map['deleted_at'] = Variable<String>(deletedAt.value);
    }
    if (syncState.present) {
      map['sync_state'] = Variable<String>(syncState.value);
    }
    if (payloadJson.present) {
      map['payload_json'] = Variable<String>(payloadJson.value);
    }
    if (rowid.present) {
      map['rowid'] = Variable<int>(rowid.value);
    }
    return map;
  }

  @override
  String toString() {
    return (StringBuffer('HabitCheckinsDriftTableCompanion(')
          ..write('id: $id, ')
          ..write('userId: $userId, ')
          ..write('version: $version, ')
          ..write('updatedAt: $updatedAt, ')
          ..write('deletedAt: $deletedAt, ')
          ..write('syncState: $syncState, ')
          ..write('payloadJson: $payloadJson, ')
          ..write('rowid: $rowid')
          ..write(')'))
        .toString();
  }
}

class $RoutinesDriftTableTable extends RoutinesDriftTable
    with TableInfo<$RoutinesDriftTableTable, RoutinesDriftTableData> {
  @override
  final GeneratedDatabase attachedDatabase;
  final String? _alias;
  $RoutinesDriftTableTable(this.attachedDatabase, [this._alias]);
  static const VerificationMeta _idMeta = const VerificationMeta('id');
  @override
  late final GeneratedColumn<String> id = GeneratedColumn<String>(
      'id', aliasedName, false,
      type: DriftSqlType.string, requiredDuringInsert: true);
  static const VerificationMeta _userIdMeta = const VerificationMeta('userId');
  @override
  late final GeneratedColumn<String> userId = GeneratedColumn<String>(
      'user_id', aliasedName, false,
      type: DriftSqlType.string, requiredDuringInsert: true);
  static const VerificationMeta _versionMeta =
      const VerificationMeta('version');
  @override
  late final GeneratedColumn<int> version = GeneratedColumn<int>(
      'version', aliasedName, false,
      type: DriftSqlType.int,
      requiredDuringInsert: false,
      defaultValue: const Constant(0));
  static const VerificationMeta _updatedAtMeta =
      const VerificationMeta('updatedAt');
  @override
  late final GeneratedColumn<String> updatedAt = GeneratedColumn<String>(
      'updated_at', aliasedName, false,
      type: DriftSqlType.string, requiredDuringInsert: true);
  static const VerificationMeta _deletedAtMeta =
      const VerificationMeta('deletedAt');
  @override
  late final GeneratedColumn<String> deletedAt = GeneratedColumn<String>(
      'deleted_at', aliasedName, true,
      type: DriftSqlType.string, requiredDuringInsert: false);
  static const VerificationMeta _syncStateMeta =
      const VerificationMeta('syncState');
  @override
  late final GeneratedColumn<String> syncState = GeneratedColumn<String>(
      'sync_state', aliasedName, false,
      type: DriftSqlType.string,
      requiredDuringInsert: false,
      defaultValue: const Constant('synced'));
  static const VerificationMeta _payloadJsonMeta =
      const VerificationMeta('payloadJson');
  @override
  late final GeneratedColumn<String> payloadJson = GeneratedColumn<String>(
      'payload_json', aliasedName, false,
      type: DriftSqlType.string, requiredDuringInsert: true);
  @override
  List<GeneratedColumn> get $columns =>
      [id, userId, version, updatedAt, deletedAt, syncState, payloadJson];
  @override
  String get aliasedName => _alias ?? actualTableName;
  @override
  String get actualTableName => $name;
  static const String $name = 'routines';
  @override
  VerificationContext validateIntegrity(
      Insertable<RoutinesDriftTableData> instance,
      {bool isInserting = false}) {
    final context = VerificationContext();
    final data = instance.toColumns(true);
    if (data.containsKey('id')) {
      context.handle(_idMeta, id.isAcceptableOrUnknown(data['id']!, _idMeta));
    } else if (isInserting) {
      context.missing(_idMeta);
    }
    if (data.containsKey('user_id')) {
      context.handle(_userIdMeta,
          userId.isAcceptableOrUnknown(data['user_id']!, _userIdMeta));
    } else if (isInserting) {
      context.missing(_userIdMeta);
    }
    if (data.containsKey('version')) {
      context.handle(_versionMeta,
          version.isAcceptableOrUnknown(data['version']!, _versionMeta));
    }
    if (data.containsKey('updated_at')) {
      context.handle(_updatedAtMeta,
          updatedAt.isAcceptableOrUnknown(data['updated_at']!, _updatedAtMeta));
    } else if (isInserting) {
      context.missing(_updatedAtMeta);
    }
    if (data.containsKey('deleted_at')) {
      context.handle(_deletedAtMeta,
          deletedAt.isAcceptableOrUnknown(data['deleted_at']!, _deletedAtMeta));
    }
    if (data.containsKey('sync_state')) {
      context.handle(_syncStateMeta,
          syncState.isAcceptableOrUnknown(data['sync_state']!, _syncStateMeta));
    }
    if (data.containsKey('payload_json')) {
      context.handle(
          _payloadJsonMeta,
          payloadJson.isAcceptableOrUnknown(
              data['payload_json']!, _payloadJsonMeta));
    } else if (isInserting) {
      context.missing(_payloadJsonMeta);
    }
    return context;
  }

  @override
  Set<GeneratedColumn> get $primaryKey => {userId, id};
  @override
  RoutinesDriftTableData map(Map<String, dynamic> data, {String? tablePrefix}) {
    final effectivePrefix = tablePrefix != null ? '$tablePrefix.' : '';
    return RoutinesDriftTableData(
      id: attachedDatabase.typeMapping
          .read(DriftSqlType.string, data['${effectivePrefix}id'])!,
      userId: attachedDatabase.typeMapping
          .read(DriftSqlType.string, data['${effectivePrefix}user_id'])!,
      version: attachedDatabase.typeMapping
          .read(DriftSqlType.int, data['${effectivePrefix}version'])!,
      updatedAt: attachedDatabase.typeMapping
          .read(DriftSqlType.string, data['${effectivePrefix}updated_at'])!,
      deletedAt: attachedDatabase.typeMapping
          .read(DriftSqlType.string, data['${effectivePrefix}deleted_at']),
      syncState: attachedDatabase.typeMapping
          .read(DriftSqlType.string, data['${effectivePrefix}sync_state'])!,
      payloadJson: attachedDatabase.typeMapping
          .read(DriftSqlType.string, data['${effectivePrefix}payload_json'])!,
    );
  }

  @override
  $RoutinesDriftTableTable createAlias(String alias) {
    return $RoutinesDriftTableTable(attachedDatabase, alias);
  }
}

class RoutinesDriftTableData extends DataClass
    implements Insertable<RoutinesDriftTableData> {
  final String id;
  final String userId;
  final int version;
  final String updatedAt;
  final String? deletedAt;
  final String syncState;
  final String payloadJson;
  const RoutinesDriftTableData(
      {required this.id,
      required this.userId,
      required this.version,
      required this.updatedAt,
      this.deletedAt,
      required this.syncState,
      required this.payloadJson});
  @override
  Map<String, Expression> toColumns(bool nullToAbsent) {
    final map = <String, Expression>{};
    map['id'] = Variable<String>(id);
    map['user_id'] = Variable<String>(userId);
    map['version'] = Variable<int>(version);
    map['updated_at'] = Variable<String>(updatedAt);
    if (!nullToAbsent || deletedAt != null) {
      map['deleted_at'] = Variable<String>(deletedAt);
    }
    map['sync_state'] = Variable<String>(syncState);
    map['payload_json'] = Variable<String>(payloadJson);
    return map;
  }

  RoutinesDriftTableCompanion toCompanion(bool nullToAbsent) {
    return RoutinesDriftTableCompanion(
      id: Value(id),
      userId: Value(userId),
      version: Value(version),
      updatedAt: Value(updatedAt),
      deletedAt: deletedAt == null && nullToAbsent
          ? const Value.absent()
          : Value(deletedAt),
      syncState: Value(syncState),
      payloadJson: Value(payloadJson),
    );
  }

  factory RoutinesDriftTableData.fromJson(Map<String, dynamic> json,
      {ValueSerializer? serializer}) {
    serializer ??= driftRuntimeOptions.defaultSerializer;
    return RoutinesDriftTableData(
      id: serializer.fromJson<String>(json['id']),
      userId: serializer.fromJson<String>(json['userId']),
      version: serializer.fromJson<int>(json['version']),
      updatedAt: serializer.fromJson<String>(json['updatedAt']),
      deletedAt: serializer.fromJson<String?>(json['deletedAt']),
      syncState: serializer.fromJson<String>(json['syncState']),
      payloadJson: serializer.fromJson<String>(json['payloadJson']),
    );
  }
  @override
  Map<String, dynamic> toJson({ValueSerializer? serializer}) {
    serializer ??= driftRuntimeOptions.defaultSerializer;
    return <String, dynamic>{
      'id': serializer.toJson<String>(id),
      'userId': serializer.toJson<String>(userId),
      'version': serializer.toJson<int>(version),
      'updatedAt': serializer.toJson<String>(updatedAt),
      'deletedAt': serializer.toJson<String?>(deletedAt),
      'syncState': serializer.toJson<String>(syncState),
      'payloadJson': serializer.toJson<String>(payloadJson),
    };
  }

  RoutinesDriftTableData copyWith(
          {String? id,
          String? userId,
          int? version,
          String? updatedAt,
          Value<String?> deletedAt = const Value.absent(),
          String? syncState,
          String? payloadJson}) =>
      RoutinesDriftTableData(
        id: id ?? this.id,
        userId: userId ?? this.userId,
        version: version ?? this.version,
        updatedAt: updatedAt ?? this.updatedAt,
        deletedAt: deletedAt.present ? deletedAt.value : this.deletedAt,
        syncState: syncState ?? this.syncState,
        payloadJson: payloadJson ?? this.payloadJson,
      );
  RoutinesDriftTableData copyWithCompanion(RoutinesDriftTableCompanion data) {
    return RoutinesDriftTableData(
      id: data.id.present ? data.id.value : this.id,
      userId: data.userId.present ? data.userId.value : this.userId,
      version: data.version.present ? data.version.value : this.version,
      updatedAt: data.updatedAt.present ? data.updatedAt.value : this.updatedAt,
      deletedAt: data.deletedAt.present ? data.deletedAt.value : this.deletedAt,
      syncState: data.syncState.present ? data.syncState.value : this.syncState,
      payloadJson:
          data.payloadJson.present ? data.payloadJson.value : this.payloadJson,
    );
  }

  @override
  String toString() {
    return (StringBuffer('RoutinesDriftTableData(')
          ..write('id: $id, ')
          ..write('userId: $userId, ')
          ..write('version: $version, ')
          ..write('updatedAt: $updatedAt, ')
          ..write('deletedAt: $deletedAt, ')
          ..write('syncState: $syncState, ')
          ..write('payloadJson: $payloadJson')
          ..write(')'))
        .toString();
  }

  @override
  int get hashCode => Object.hash(
      id, userId, version, updatedAt, deletedAt, syncState, payloadJson);
  @override
  bool operator ==(Object other) =>
      identical(this, other) ||
      (other is RoutinesDriftTableData &&
          other.id == this.id &&
          other.userId == this.userId &&
          other.version == this.version &&
          other.updatedAt == this.updatedAt &&
          other.deletedAt == this.deletedAt &&
          other.syncState == this.syncState &&
          other.payloadJson == this.payloadJson);
}

class RoutinesDriftTableCompanion
    extends UpdateCompanion<RoutinesDriftTableData> {
  final Value<String> id;
  final Value<String> userId;
  final Value<int> version;
  final Value<String> updatedAt;
  final Value<String?> deletedAt;
  final Value<String> syncState;
  final Value<String> payloadJson;
  final Value<int> rowid;
  const RoutinesDriftTableCompanion({
    this.id = const Value.absent(),
    this.userId = const Value.absent(),
    this.version = const Value.absent(),
    this.updatedAt = const Value.absent(),
    this.deletedAt = const Value.absent(),
    this.syncState = const Value.absent(),
    this.payloadJson = const Value.absent(),
    this.rowid = const Value.absent(),
  });
  RoutinesDriftTableCompanion.insert({
    required String id,
    required String userId,
    this.version = const Value.absent(),
    required String updatedAt,
    this.deletedAt = const Value.absent(),
    this.syncState = const Value.absent(),
    required String payloadJson,
    this.rowid = const Value.absent(),
  })  : id = Value(id),
        userId = Value(userId),
        updatedAt = Value(updatedAt),
        payloadJson = Value(payloadJson);
  static Insertable<RoutinesDriftTableData> custom({
    Expression<String>? id,
    Expression<String>? userId,
    Expression<int>? version,
    Expression<String>? updatedAt,
    Expression<String>? deletedAt,
    Expression<String>? syncState,
    Expression<String>? payloadJson,
    Expression<int>? rowid,
  }) {
    return RawValuesInsertable({
      if (id != null) 'id': id,
      if (userId != null) 'user_id': userId,
      if (version != null) 'version': version,
      if (updatedAt != null) 'updated_at': updatedAt,
      if (deletedAt != null) 'deleted_at': deletedAt,
      if (syncState != null) 'sync_state': syncState,
      if (payloadJson != null) 'payload_json': payloadJson,
      if (rowid != null) 'rowid': rowid,
    });
  }

  RoutinesDriftTableCompanion copyWith(
      {Value<String>? id,
      Value<String>? userId,
      Value<int>? version,
      Value<String>? updatedAt,
      Value<String?>? deletedAt,
      Value<String>? syncState,
      Value<String>? payloadJson,
      Value<int>? rowid}) {
    return RoutinesDriftTableCompanion(
      id: id ?? this.id,
      userId: userId ?? this.userId,
      version: version ?? this.version,
      updatedAt: updatedAt ?? this.updatedAt,
      deletedAt: deletedAt ?? this.deletedAt,
      syncState: syncState ?? this.syncState,
      payloadJson: payloadJson ?? this.payloadJson,
      rowid: rowid ?? this.rowid,
    );
  }

  @override
  Map<String, Expression> toColumns(bool nullToAbsent) {
    final map = <String, Expression>{};
    if (id.present) {
      map['id'] = Variable<String>(id.value);
    }
    if (userId.present) {
      map['user_id'] = Variable<String>(userId.value);
    }
    if (version.present) {
      map['version'] = Variable<int>(version.value);
    }
    if (updatedAt.present) {
      map['updated_at'] = Variable<String>(updatedAt.value);
    }
    if (deletedAt.present) {
      map['deleted_at'] = Variable<String>(deletedAt.value);
    }
    if (syncState.present) {
      map['sync_state'] = Variable<String>(syncState.value);
    }
    if (payloadJson.present) {
      map['payload_json'] = Variable<String>(payloadJson.value);
    }
    if (rowid.present) {
      map['rowid'] = Variable<int>(rowid.value);
    }
    return map;
  }

  @override
  String toString() {
    return (StringBuffer('RoutinesDriftTableCompanion(')
          ..write('id: $id, ')
          ..write('userId: $userId, ')
          ..write('version: $version, ')
          ..write('updatedAt: $updatedAt, ')
          ..write('deletedAt: $deletedAt, ')
          ..write('syncState: $syncState, ')
          ..write('payloadJson: $payloadJson, ')
          ..write('rowid: $rowid')
          ..write(')'))
        .toString();
  }
}

class $RoutineStepsDriftTableTable extends RoutineStepsDriftTable
    with TableInfo<$RoutineStepsDriftTableTable, RoutineStepsDriftTableData> {
  @override
  final GeneratedDatabase attachedDatabase;
  final String? _alias;
  $RoutineStepsDriftTableTable(this.attachedDatabase, [this._alias]);
  static const VerificationMeta _idMeta = const VerificationMeta('id');
  @override
  late final GeneratedColumn<String> id = GeneratedColumn<String>(
      'id', aliasedName, false,
      type: DriftSqlType.string, requiredDuringInsert: true);
  static const VerificationMeta _userIdMeta = const VerificationMeta('userId');
  @override
  late final GeneratedColumn<String> userId = GeneratedColumn<String>(
      'user_id', aliasedName, false,
      type: DriftSqlType.string, requiredDuringInsert: true);
  static const VerificationMeta _versionMeta =
      const VerificationMeta('version');
  @override
  late final GeneratedColumn<int> version = GeneratedColumn<int>(
      'version', aliasedName, false,
      type: DriftSqlType.int,
      requiredDuringInsert: false,
      defaultValue: const Constant(0));
  static const VerificationMeta _updatedAtMeta =
      const VerificationMeta('updatedAt');
  @override
  late final GeneratedColumn<String> updatedAt = GeneratedColumn<String>(
      'updated_at', aliasedName, false,
      type: DriftSqlType.string, requiredDuringInsert: true);
  static const VerificationMeta _deletedAtMeta =
      const VerificationMeta('deletedAt');
  @override
  late final GeneratedColumn<String> deletedAt = GeneratedColumn<String>(
      'deleted_at', aliasedName, true,
      type: DriftSqlType.string, requiredDuringInsert: false);
  static const VerificationMeta _syncStateMeta =
      const VerificationMeta('syncState');
  @override
  late final GeneratedColumn<String> syncState = GeneratedColumn<String>(
      'sync_state', aliasedName, false,
      type: DriftSqlType.string,
      requiredDuringInsert: false,
      defaultValue: const Constant('synced'));
  static const VerificationMeta _payloadJsonMeta =
      const VerificationMeta('payloadJson');
  @override
  late final GeneratedColumn<String> payloadJson = GeneratedColumn<String>(
      'payload_json', aliasedName, false,
      type: DriftSqlType.string, requiredDuringInsert: true);
  @override
  List<GeneratedColumn> get $columns =>
      [id, userId, version, updatedAt, deletedAt, syncState, payloadJson];
  @override
  String get aliasedName => _alias ?? actualTableName;
  @override
  String get actualTableName => $name;
  static const String $name = 'routine_steps';
  @override
  VerificationContext validateIntegrity(
      Insertable<RoutineStepsDriftTableData> instance,
      {bool isInserting = false}) {
    final context = VerificationContext();
    final data = instance.toColumns(true);
    if (data.containsKey('id')) {
      context.handle(_idMeta, id.isAcceptableOrUnknown(data['id']!, _idMeta));
    } else if (isInserting) {
      context.missing(_idMeta);
    }
    if (data.containsKey('user_id')) {
      context.handle(_userIdMeta,
          userId.isAcceptableOrUnknown(data['user_id']!, _userIdMeta));
    } else if (isInserting) {
      context.missing(_userIdMeta);
    }
    if (data.containsKey('version')) {
      context.handle(_versionMeta,
          version.isAcceptableOrUnknown(data['version']!, _versionMeta));
    }
    if (data.containsKey('updated_at')) {
      context.handle(_updatedAtMeta,
          updatedAt.isAcceptableOrUnknown(data['updated_at']!, _updatedAtMeta));
    } else if (isInserting) {
      context.missing(_updatedAtMeta);
    }
    if (data.containsKey('deleted_at')) {
      context.handle(_deletedAtMeta,
          deletedAt.isAcceptableOrUnknown(data['deleted_at']!, _deletedAtMeta));
    }
    if (data.containsKey('sync_state')) {
      context.handle(_syncStateMeta,
          syncState.isAcceptableOrUnknown(data['sync_state']!, _syncStateMeta));
    }
    if (data.containsKey('payload_json')) {
      context.handle(
          _payloadJsonMeta,
          payloadJson.isAcceptableOrUnknown(
              data['payload_json']!, _payloadJsonMeta));
    } else if (isInserting) {
      context.missing(_payloadJsonMeta);
    }
    return context;
  }

  @override
  Set<GeneratedColumn> get $primaryKey => {userId, id};
  @override
  RoutineStepsDriftTableData map(Map<String, dynamic> data,
      {String? tablePrefix}) {
    final effectivePrefix = tablePrefix != null ? '$tablePrefix.' : '';
    return RoutineStepsDriftTableData(
      id: attachedDatabase.typeMapping
          .read(DriftSqlType.string, data['${effectivePrefix}id'])!,
      userId: attachedDatabase.typeMapping
          .read(DriftSqlType.string, data['${effectivePrefix}user_id'])!,
      version: attachedDatabase.typeMapping
          .read(DriftSqlType.int, data['${effectivePrefix}version'])!,
      updatedAt: attachedDatabase.typeMapping
          .read(DriftSqlType.string, data['${effectivePrefix}updated_at'])!,
      deletedAt: attachedDatabase.typeMapping
          .read(DriftSqlType.string, data['${effectivePrefix}deleted_at']),
      syncState: attachedDatabase.typeMapping
          .read(DriftSqlType.string, data['${effectivePrefix}sync_state'])!,
      payloadJson: attachedDatabase.typeMapping
          .read(DriftSqlType.string, data['${effectivePrefix}payload_json'])!,
    );
  }

  @override
  $RoutineStepsDriftTableTable createAlias(String alias) {
    return $RoutineStepsDriftTableTable(attachedDatabase, alias);
  }
}

class RoutineStepsDriftTableData extends DataClass
    implements Insertable<RoutineStepsDriftTableData> {
  final String id;
  final String userId;
  final int version;
  final String updatedAt;
  final String? deletedAt;
  final String syncState;
  final String payloadJson;
  const RoutineStepsDriftTableData(
      {required this.id,
      required this.userId,
      required this.version,
      required this.updatedAt,
      this.deletedAt,
      required this.syncState,
      required this.payloadJson});
  @override
  Map<String, Expression> toColumns(bool nullToAbsent) {
    final map = <String, Expression>{};
    map['id'] = Variable<String>(id);
    map['user_id'] = Variable<String>(userId);
    map['version'] = Variable<int>(version);
    map['updated_at'] = Variable<String>(updatedAt);
    if (!nullToAbsent || deletedAt != null) {
      map['deleted_at'] = Variable<String>(deletedAt);
    }
    map['sync_state'] = Variable<String>(syncState);
    map['payload_json'] = Variable<String>(payloadJson);
    return map;
  }

  RoutineStepsDriftTableCompanion toCompanion(bool nullToAbsent) {
    return RoutineStepsDriftTableCompanion(
      id: Value(id),
      userId: Value(userId),
      version: Value(version),
      updatedAt: Value(updatedAt),
      deletedAt: deletedAt == null && nullToAbsent
          ? const Value.absent()
          : Value(deletedAt),
      syncState: Value(syncState),
      payloadJson: Value(payloadJson),
    );
  }

  factory RoutineStepsDriftTableData.fromJson(Map<String, dynamic> json,
      {ValueSerializer? serializer}) {
    serializer ??= driftRuntimeOptions.defaultSerializer;
    return RoutineStepsDriftTableData(
      id: serializer.fromJson<String>(json['id']),
      userId: serializer.fromJson<String>(json['userId']),
      version: serializer.fromJson<int>(json['version']),
      updatedAt: serializer.fromJson<String>(json['updatedAt']),
      deletedAt: serializer.fromJson<String?>(json['deletedAt']),
      syncState: serializer.fromJson<String>(json['syncState']),
      payloadJson: serializer.fromJson<String>(json['payloadJson']),
    );
  }
  @override
  Map<String, dynamic> toJson({ValueSerializer? serializer}) {
    serializer ??= driftRuntimeOptions.defaultSerializer;
    return <String, dynamic>{
      'id': serializer.toJson<String>(id),
      'userId': serializer.toJson<String>(userId),
      'version': serializer.toJson<int>(version),
      'updatedAt': serializer.toJson<String>(updatedAt),
      'deletedAt': serializer.toJson<String?>(deletedAt),
      'syncState': serializer.toJson<String>(syncState),
      'payloadJson': serializer.toJson<String>(payloadJson),
    };
  }

  RoutineStepsDriftTableData copyWith(
          {String? id,
          String? userId,
          int? version,
          String? updatedAt,
          Value<String?> deletedAt = const Value.absent(),
          String? syncState,
          String? payloadJson}) =>
      RoutineStepsDriftTableData(
        id: id ?? this.id,
        userId: userId ?? this.userId,
        version: version ?? this.version,
        updatedAt: updatedAt ?? this.updatedAt,
        deletedAt: deletedAt.present ? deletedAt.value : this.deletedAt,
        syncState: syncState ?? this.syncState,
        payloadJson: payloadJson ?? this.payloadJson,
      );
  RoutineStepsDriftTableData copyWithCompanion(
      RoutineStepsDriftTableCompanion data) {
    return RoutineStepsDriftTableData(
      id: data.id.present ? data.id.value : this.id,
      userId: data.userId.present ? data.userId.value : this.userId,
      version: data.version.present ? data.version.value : this.version,
      updatedAt: data.updatedAt.present ? data.updatedAt.value : this.updatedAt,
      deletedAt: data.deletedAt.present ? data.deletedAt.value : this.deletedAt,
      syncState: data.syncState.present ? data.syncState.value : this.syncState,
      payloadJson:
          data.payloadJson.present ? data.payloadJson.value : this.payloadJson,
    );
  }

  @override
  String toString() {
    return (StringBuffer('RoutineStepsDriftTableData(')
          ..write('id: $id, ')
          ..write('userId: $userId, ')
          ..write('version: $version, ')
          ..write('updatedAt: $updatedAt, ')
          ..write('deletedAt: $deletedAt, ')
          ..write('syncState: $syncState, ')
          ..write('payloadJson: $payloadJson')
          ..write(')'))
        .toString();
  }

  @override
  int get hashCode => Object.hash(
      id, userId, version, updatedAt, deletedAt, syncState, payloadJson);
  @override
  bool operator ==(Object other) =>
      identical(this, other) ||
      (other is RoutineStepsDriftTableData &&
          other.id == this.id &&
          other.userId == this.userId &&
          other.version == this.version &&
          other.updatedAt == this.updatedAt &&
          other.deletedAt == this.deletedAt &&
          other.syncState == this.syncState &&
          other.payloadJson == this.payloadJson);
}

class RoutineStepsDriftTableCompanion
    extends UpdateCompanion<RoutineStepsDriftTableData> {
  final Value<String> id;
  final Value<String> userId;
  final Value<int> version;
  final Value<String> updatedAt;
  final Value<String?> deletedAt;
  final Value<String> syncState;
  final Value<String> payloadJson;
  final Value<int> rowid;
  const RoutineStepsDriftTableCompanion({
    this.id = const Value.absent(),
    this.userId = const Value.absent(),
    this.version = const Value.absent(),
    this.updatedAt = const Value.absent(),
    this.deletedAt = const Value.absent(),
    this.syncState = const Value.absent(),
    this.payloadJson = const Value.absent(),
    this.rowid = const Value.absent(),
  });
  RoutineStepsDriftTableCompanion.insert({
    required String id,
    required String userId,
    this.version = const Value.absent(),
    required String updatedAt,
    this.deletedAt = const Value.absent(),
    this.syncState = const Value.absent(),
    required String payloadJson,
    this.rowid = const Value.absent(),
  })  : id = Value(id),
        userId = Value(userId),
        updatedAt = Value(updatedAt),
        payloadJson = Value(payloadJson);
  static Insertable<RoutineStepsDriftTableData> custom({
    Expression<String>? id,
    Expression<String>? userId,
    Expression<int>? version,
    Expression<String>? updatedAt,
    Expression<String>? deletedAt,
    Expression<String>? syncState,
    Expression<String>? payloadJson,
    Expression<int>? rowid,
  }) {
    return RawValuesInsertable({
      if (id != null) 'id': id,
      if (userId != null) 'user_id': userId,
      if (version != null) 'version': version,
      if (updatedAt != null) 'updated_at': updatedAt,
      if (deletedAt != null) 'deleted_at': deletedAt,
      if (syncState != null) 'sync_state': syncState,
      if (payloadJson != null) 'payload_json': payloadJson,
      if (rowid != null) 'rowid': rowid,
    });
  }

  RoutineStepsDriftTableCompanion copyWith(
      {Value<String>? id,
      Value<String>? userId,
      Value<int>? version,
      Value<String>? updatedAt,
      Value<String?>? deletedAt,
      Value<String>? syncState,
      Value<String>? payloadJson,
      Value<int>? rowid}) {
    return RoutineStepsDriftTableCompanion(
      id: id ?? this.id,
      userId: userId ?? this.userId,
      version: version ?? this.version,
      updatedAt: updatedAt ?? this.updatedAt,
      deletedAt: deletedAt ?? this.deletedAt,
      syncState: syncState ?? this.syncState,
      payloadJson: payloadJson ?? this.payloadJson,
      rowid: rowid ?? this.rowid,
    );
  }

  @override
  Map<String, Expression> toColumns(bool nullToAbsent) {
    final map = <String, Expression>{};
    if (id.present) {
      map['id'] = Variable<String>(id.value);
    }
    if (userId.present) {
      map['user_id'] = Variable<String>(userId.value);
    }
    if (version.present) {
      map['version'] = Variable<int>(version.value);
    }
    if (updatedAt.present) {
      map['updated_at'] = Variable<String>(updatedAt.value);
    }
    if (deletedAt.present) {
      map['deleted_at'] = Variable<String>(deletedAt.value);
    }
    if (syncState.present) {
      map['sync_state'] = Variable<String>(syncState.value);
    }
    if (payloadJson.present) {
      map['payload_json'] = Variable<String>(payloadJson.value);
    }
    if (rowid.present) {
      map['rowid'] = Variable<int>(rowid.value);
    }
    return map;
  }

  @override
  String toString() {
    return (StringBuffer('RoutineStepsDriftTableCompanion(')
          ..write('id: $id, ')
          ..write('userId: $userId, ')
          ..write('version: $version, ')
          ..write('updatedAt: $updatedAt, ')
          ..write('deletedAt: $deletedAt, ')
          ..write('syncState: $syncState, ')
          ..write('payloadJson: $payloadJson, ')
          ..write('rowid: $rowid')
          ..write(')'))
        .toString();
  }
}

class $RoutineLogsDriftTableTable extends RoutineLogsDriftTable
    with TableInfo<$RoutineLogsDriftTableTable, RoutineLogsDriftTableData> {
  @override
  final GeneratedDatabase attachedDatabase;
  final String? _alias;
  $RoutineLogsDriftTableTable(this.attachedDatabase, [this._alias]);
  static const VerificationMeta _idMeta = const VerificationMeta('id');
  @override
  late final GeneratedColumn<String> id = GeneratedColumn<String>(
      'id', aliasedName, false,
      type: DriftSqlType.string, requiredDuringInsert: true);
  static const VerificationMeta _userIdMeta = const VerificationMeta('userId');
  @override
  late final GeneratedColumn<String> userId = GeneratedColumn<String>(
      'user_id', aliasedName, false,
      type: DriftSqlType.string, requiredDuringInsert: true);
  static const VerificationMeta _versionMeta =
      const VerificationMeta('version');
  @override
  late final GeneratedColumn<int> version = GeneratedColumn<int>(
      'version', aliasedName, false,
      type: DriftSqlType.int,
      requiredDuringInsert: false,
      defaultValue: const Constant(0));
  static const VerificationMeta _updatedAtMeta =
      const VerificationMeta('updatedAt');
  @override
  late final GeneratedColumn<String> updatedAt = GeneratedColumn<String>(
      'updated_at', aliasedName, false,
      type: DriftSqlType.string, requiredDuringInsert: true);
  static const VerificationMeta _deletedAtMeta =
      const VerificationMeta('deletedAt');
  @override
  late final GeneratedColumn<String> deletedAt = GeneratedColumn<String>(
      'deleted_at', aliasedName, true,
      type: DriftSqlType.string, requiredDuringInsert: false);
  static const VerificationMeta _syncStateMeta =
      const VerificationMeta('syncState');
  @override
  late final GeneratedColumn<String> syncState = GeneratedColumn<String>(
      'sync_state', aliasedName, false,
      type: DriftSqlType.string,
      requiredDuringInsert: false,
      defaultValue: const Constant('synced'));
  static const VerificationMeta _payloadJsonMeta =
      const VerificationMeta('payloadJson');
  @override
  late final GeneratedColumn<String> payloadJson = GeneratedColumn<String>(
      'payload_json', aliasedName, false,
      type: DriftSqlType.string, requiredDuringInsert: true);
  @override
  List<GeneratedColumn> get $columns =>
      [id, userId, version, updatedAt, deletedAt, syncState, payloadJson];
  @override
  String get aliasedName => _alias ?? actualTableName;
  @override
  String get actualTableName => $name;
  static const String $name = 'routine_logs';
  @override
  VerificationContext validateIntegrity(
      Insertable<RoutineLogsDriftTableData> instance,
      {bool isInserting = false}) {
    final context = VerificationContext();
    final data = instance.toColumns(true);
    if (data.containsKey('id')) {
      context.handle(_idMeta, id.isAcceptableOrUnknown(data['id']!, _idMeta));
    } else if (isInserting) {
      context.missing(_idMeta);
    }
    if (data.containsKey('user_id')) {
      context.handle(_userIdMeta,
          userId.isAcceptableOrUnknown(data['user_id']!, _userIdMeta));
    } else if (isInserting) {
      context.missing(_userIdMeta);
    }
    if (data.containsKey('version')) {
      context.handle(_versionMeta,
          version.isAcceptableOrUnknown(data['version']!, _versionMeta));
    }
    if (data.containsKey('updated_at')) {
      context.handle(_updatedAtMeta,
          updatedAt.isAcceptableOrUnknown(data['updated_at']!, _updatedAtMeta));
    } else if (isInserting) {
      context.missing(_updatedAtMeta);
    }
    if (data.containsKey('deleted_at')) {
      context.handle(_deletedAtMeta,
          deletedAt.isAcceptableOrUnknown(data['deleted_at']!, _deletedAtMeta));
    }
    if (data.containsKey('sync_state')) {
      context.handle(_syncStateMeta,
          syncState.isAcceptableOrUnknown(data['sync_state']!, _syncStateMeta));
    }
    if (data.containsKey('payload_json')) {
      context.handle(
          _payloadJsonMeta,
          payloadJson.isAcceptableOrUnknown(
              data['payload_json']!, _payloadJsonMeta));
    } else if (isInserting) {
      context.missing(_payloadJsonMeta);
    }
    return context;
  }

  @override
  Set<GeneratedColumn> get $primaryKey => {userId, id};
  @override
  RoutineLogsDriftTableData map(Map<String, dynamic> data,
      {String? tablePrefix}) {
    final effectivePrefix = tablePrefix != null ? '$tablePrefix.' : '';
    return RoutineLogsDriftTableData(
      id: attachedDatabase.typeMapping
          .read(DriftSqlType.string, data['${effectivePrefix}id'])!,
      userId: attachedDatabase.typeMapping
          .read(DriftSqlType.string, data['${effectivePrefix}user_id'])!,
      version: attachedDatabase.typeMapping
          .read(DriftSqlType.int, data['${effectivePrefix}version'])!,
      updatedAt: attachedDatabase.typeMapping
          .read(DriftSqlType.string, data['${effectivePrefix}updated_at'])!,
      deletedAt: attachedDatabase.typeMapping
          .read(DriftSqlType.string, data['${effectivePrefix}deleted_at']),
      syncState: attachedDatabase.typeMapping
          .read(DriftSqlType.string, data['${effectivePrefix}sync_state'])!,
      payloadJson: attachedDatabase.typeMapping
          .read(DriftSqlType.string, data['${effectivePrefix}payload_json'])!,
    );
  }

  @override
  $RoutineLogsDriftTableTable createAlias(String alias) {
    return $RoutineLogsDriftTableTable(attachedDatabase, alias);
  }
}

class RoutineLogsDriftTableData extends DataClass
    implements Insertable<RoutineLogsDriftTableData> {
  final String id;
  final String userId;
  final int version;
  final String updatedAt;
  final String? deletedAt;
  final String syncState;
  final String payloadJson;
  const RoutineLogsDriftTableData(
      {required this.id,
      required this.userId,
      required this.version,
      required this.updatedAt,
      this.deletedAt,
      required this.syncState,
      required this.payloadJson});
  @override
  Map<String, Expression> toColumns(bool nullToAbsent) {
    final map = <String, Expression>{};
    map['id'] = Variable<String>(id);
    map['user_id'] = Variable<String>(userId);
    map['version'] = Variable<int>(version);
    map['updated_at'] = Variable<String>(updatedAt);
    if (!nullToAbsent || deletedAt != null) {
      map['deleted_at'] = Variable<String>(deletedAt);
    }
    map['sync_state'] = Variable<String>(syncState);
    map['payload_json'] = Variable<String>(payloadJson);
    return map;
  }

  RoutineLogsDriftTableCompanion toCompanion(bool nullToAbsent) {
    return RoutineLogsDriftTableCompanion(
      id: Value(id),
      userId: Value(userId),
      version: Value(version),
      updatedAt: Value(updatedAt),
      deletedAt: deletedAt == null && nullToAbsent
          ? const Value.absent()
          : Value(deletedAt),
      syncState: Value(syncState),
      payloadJson: Value(payloadJson),
    );
  }

  factory RoutineLogsDriftTableData.fromJson(Map<String, dynamic> json,
      {ValueSerializer? serializer}) {
    serializer ??= driftRuntimeOptions.defaultSerializer;
    return RoutineLogsDriftTableData(
      id: serializer.fromJson<String>(json['id']),
      userId: serializer.fromJson<String>(json['userId']),
      version: serializer.fromJson<int>(json['version']),
      updatedAt: serializer.fromJson<String>(json['updatedAt']),
      deletedAt: serializer.fromJson<String?>(json['deletedAt']),
      syncState: serializer.fromJson<String>(json['syncState']),
      payloadJson: serializer.fromJson<String>(json['payloadJson']),
    );
  }
  @override
  Map<String, dynamic> toJson({ValueSerializer? serializer}) {
    serializer ??= driftRuntimeOptions.defaultSerializer;
    return <String, dynamic>{
      'id': serializer.toJson<String>(id),
      'userId': serializer.toJson<String>(userId),
      'version': serializer.toJson<int>(version),
      'updatedAt': serializer.toJson<String>(updatedAt),
      'deletedAt': serializer.toJson<String?>(deletedAt),
      'syncState': serializer.toJson<String>(syncState),
      'payloadJson': serializer.toJson<String>(payloadJson),
    };
  }

  RoutineLogsDriftTableData copyWith(
          {String? id,
          String? userId,
          int? version,
          String? updatedAt,
          Value<String?> deletedAt = const Value.absent(),
          String? syncState,
          String? payloadJson}) =>
      RoutineLogsDriftTableData(
        id: id ?? this.id,
        userId: userId ?? this.userId,
        version: version ?? this.version,
        updatedAt: updatedAt ?? this.updatedAt,
        deletedAt: deletedAt.present ? deletedAt.value : this.deletedAt,
        syncState: syncState ?? this.syncState,
        payloadJson: payloadJson ?? this.payloadJson,
      );
  RoutineLogsDriftTableData copyWithCompanion(
      RoutineLogsDriftTableCompanion data) {
    return RoutineLogsDriftTableData(
      id: data.id.present ? data.id.value : this.id,
      userId: data.userId.present ? data.userId.value : this.userId,
      version: data.version.present ? data.version.value : this.version,
      updatedAt: data.updatedAt.present ? data.updatedAt.value : this.updatedAt,
      deletedAt: data.deletedAt.present ? data.deletedAt.value : this.deletedAt,
      syncState: data.syncState.present ? data.syncState.value : this.syncState,
      payloadJson:
          data.payloadJson.present ? data.payloadJson.value : this.payloadJson,
    );
  }

  @override
  String toString() {
    return (StringBuffer('RoutineLogsDriftTableData(')
          ..write('id: $id, ')
          ..write('userId: $userId, ')
          ..write('version: $version, ')
          ..write('updatedAt: $updatedAt, ')
          ..write('deletedAt: $deletedAt, ')
          ..write('syncState: $syncState, ')
          ..write('payloadJson: $payloadJson')
          ..write(')'))
        .toString();
  }

  @override
  int get hashCode => Object.hash(
      id, userId, version, updatedAt, deletedAt, syncState, payloadJson);
  @override
  bool operator ==(Object other) =>
      identical(this, other) ||
      (other is RoutineLogsDriftTableData &&
          other.id == this.id &&
          other.userId == this.userId &&
          other.version == this.version &&
          other.updatedAt == this.updatedAt &&
          other.deletedAt == this.deletedAt &&
          other.syncState == this.syncState &&
          other.payloadJson == this.payloadJson);
}

class RoutineLogsDriftTableCompanion
    extends UpdateCompanion<RoutineLogsDriftTableData> {
  final Value<String> id;
  final Value<String> userId;
  final Value<int> version;
  final Value<String> updatedAt;
  final Value<String?> deletedAt;
  final Value<String> syncState;
  final Value<String> payloadJson;
  final Value<int> rowid;
  const RoutineLogsDriftTableCompanion({
    this.id = const Value.absent(),
    this.userId = const Value.absent(),
    this.version = const Value.absent(),
    this.updatedAt = const Value.absent(),
    this.deletedAt = const Value.absent(),
    this.syncState = const Value.absent(),
    this.payloadJson = const Value.absent(),
    this.rowid = const Value.absent(),
  });
  RoutineLogsDriftTableCompanion.insert({
    required String id,
    required String userId,
    this.version = const Value.absent(),
    required String updatedAt,
    this.deletedAt = const Value.absent(),
    this.syncState = const Value.absent(),
    required String payloadJson,
    this.rowid = const Value.absent(),
  })  : id = Value(id),
        userId = Value(userId),
        updatedAt = Value(updatedAt),
        payloadJson = Value(payloadJson);
  static Insertable<RoutineLogsDriftTableData> custom({
    Expression<String>? id,
    Expression<String>? userId,
    Expression<int>? version,
    Expression<String>? updatedAt,
    Expression<String>? deletedAt,
    Expression<String>? syncState,
    Expression<String>? payloadJson,
    Expression<int>? rowid,
  }) {
    return RawValuesInsertable({
      if (id != null) 'id': id,
      if (userId != null) 'user_id': userId,
      if (version != null) 'version': version,
      if (updatedAt != null) 'updated_at': updatedAt,
      if (deletedAt != null) 'deleted_at': deletedAt,
      if (syncState != null) 'sync_state': syncState,
      if (payloadJson != null) 'payload_json': payloadJson,
      if (rowid != null) 'rowid': rowid,
    });
  }

  RoutineLogsDriftTableCompanion copyWith(
      {Value<String>? id,
      Value<String>? userId,
      Value<int>? version,
      Value<String>? updatedAt,
      Value<String?>? deletedAt,
      Value<String>? syncState,
      Value<String>? payloadJson,
      Value<int>? rowid}) {
    return RoutineLogsDriftTableCompanion(
      id: id ?? this.id,
      userId: userId ?? this.userId,
      version: version ?? this.version,
      updatedAt: updatedAt ?? this.updatedAt,
      deletedAt: deletedAt ?? this.deletedAt,
      syncState: syncState ?? this.syncState,
      payloadJson: payloadJson ?? this.payloadJson,
      rowid: rowid ?? this.rowid,
    );
  }

  @override
  Map<String, Expression> toColumns(bool nullToAbsent) {
    final map = <String, Expression>{};
    if (id.present) {
      map['id'] = Variable<String>(id.value);
    }
    if (userId.present) {
      map['user_id'] = Variable<String>(userId.value);
    }
    if (version.present) {
      map['version'] = Variable<int>(version.value);
    }
    if (updatedAt.present) {
      map['updated_at'] = Variable<String>(updatedAt.value);
    }
    if (deletedAt.present) {
      map['deleted_at'] = Variable<String>(deletedAt.value);
    }
    if (syncState.present) {
      map['sync_state'] = Variable<String>(syncState.value);
    }
    if (payloadJson.present) {
      map['payload_json'] = Variable<String>(payloadJson.value);
    }
    if (rowid.present) {
      map['rowid'] = Variable<int>(rowid.value);
    }
    return map;
  }

  @override
  String toString() {
    return (StringBuffer('RoutineLogsDriftTableCompanion(')
          ..write('id: $id, ')
          ..write('userId: $userId, ')
          ..write('version: $version, ')
          ..write('updatedAt: $updatedAt, ')
          ..write('deletedAt: $deletedAt, ')
          ..write('syncState: $syncState, ')
          ..write('payloadJson: $payloadJson, ')
          ..write('rowid: $rowid')
          ..write(')'))
        .toString();
  }
}

class $RoutineStepLogsDriftTableTable extends RoutineStepLogsDriftTable
    with
        TableInfo<$RoutineStepLogsDriftTableTable,
            RoutineStepLogsDriftTableData> {
  @override
  final GeneratedDatabase attachedDatabase;
  final String? _alias;
  $RoutineStepLogsDriftTableTable(this.attachedDatabase, [this._alias]);
  static const VerificationMeta _idMeta = const VerificationMeta('id');
  @override
  late final GeneratedColumn<String> id = GeneratedColumn<String>(
      'id', aliasedName, false,
      type: DriftSqlType.string, requiredDuringInsert: true);
  static const VerificationMeta _userIdMeta = const VerificationMeta('userId');
  @override
  late final GeneratedColumn<String> userId = GeneratedColumn<String>(
      'user_id', aliasedName, false,
      type: DriftSqlType.string, requiredDuringInsert: true);
  static const VerificationMeta _versionMeta =
      const VerificationMeta('version');
  @override
  late final GeneratedColumn<int> version = GeneratedColumn<int>(
      'version', aliasedName, false,
      type: DriftSqlType.int,
      requiredDuringInsert: false,
      defaultValue: const Constant(0));
  static const VerificationMeta _updatedAtMeta =
      const VerificationMeta('updatedAt');
  @override
  late final GeneratedColumn<String> updatedAt = GeneratedColumn<String>(
      'updated_at', aliasedName, false,
      type: DriftSqlType.string, requiredDuringInsert: true);
  static const VerificationMeta _deletedAtMeta =
      const VerificationMeta('deletedAt');
  @override
  late final GeneratedColumn<String> deletedAt = GeneratedColumn<String>(
      'deleted_at', aliasedName, true,
      type: DriftSqlType.string, requiredDuringInsert: false);
  static const VerificationMeta _syncStateMeta =
      const VerificationMeta('syncState');
  @override
  late final GeneratedColumn<String> syncState = GeneratedColumn<String>(
      'sync_state', aliasedName, false,
      type: DriftSqlType.string,
      requiredDuringInsert: false,
      defaultValue: const Constant('synced'));
  static const VerificationMeta _payloadJsonMeta =
      const VerificationMeta('payloadJson');
  @override
  late final GeneratedColumn<String> payloadJson = GeneratedColumn<String>(
      'payload_json', aliasedName, false,
      type: DriftSqlType.string, requiredDuringInsert: true);
  @override
  List<GeneratedColumn> get $columns =>
      [id, userId, version, updatedAt, deletedAt, syncState, payloadJson];
  @override
  String get aliasedName => _alias ?? actualTableName;
  @override
  String get actualTableName => $name;
  static const String $name = 'routine_step_logs';
  @override
  VerificationContext validateIntegrity(
      Insertable<RoutineStepLogsDriftTableData> instance,
      {bool isInserting = false}) {
    final context = VerificationContext();
    final data = instance.toColumns(true);
    if (data.containsKey('id')) {
      context.handle(_idMeta, id.isAcceptableOrUnknown(data['id']!, _idMeta));
    } else if (isInserting) {
      context.missing(_idMeta);
    }
    if (data.containsKey('user_id')) {
      context.handle(_userIdMeta,
          userId.isAcceptableOrUnknown(data['user_id']!, _userIdMeta));
    } else if (isInserting) {
      context.missing(_userIdMeta);
    }
    if (data.containsKey('version')) {
      context.handle(_versionMeta,
          version.isAcceptableOrUnknown(data['version']!, _versionMeta));
    }
    if (data.containsKey('updated_at')) {
      context.handle(_updatedAtMeta,
          updatedAt.isAcceptableOrUnknown(data['updated_at']!, _updatedAtMeta));
    } else if (isInserting) {
      context.missing(_updatedAtMeta);
    }
    if (data.containsKey('deleted_at')) {
      context.handle(_deletedAtMeta,
          deletedAt.isAcceptableOrUnknown(data['deleted_at']!, _deletedAtMeta));
    }
    if (data.containsKey('sync_state')) {
      context.handle(_syncStateMeta,
          syncState.isAcceptableOrUnknown(data['sync_state']!, _syncStateMeta));
    }
    if (data.containsKey('payload_json')) {
      context.handle(
          _payloadJsonMeta,
          payloadJson.isAcceptableOrUnknown(
              data['payload_json']!, _payloadJsonMeta));
    } else if (isInserting) {
      context.missing(_payloadJsonMeta);
    }
    return context;
  }

  @override
  Set<GeneratedColumn> get $primaryKey => {userId, id};
  @override
  RoutineStepLogsDriftTableData map(Map<String, dynamic> data,
      {String? tablePrefix}) {
    final effectivePrefix = tablePrefix != null ? '$tablePrefix.' : '';
    return RoutineStepLogsDriftTableData(
      id: attachedDatabase.typeMapping
          .read(DriftSqlType.string, data['${effectivePrefix}id'])!,
      userId: attachedDatabase.typeMapping
          .read(DriftSqlType.string, data['${effectivePrefix}user_id'])!,
      version: attachedDatabase.typeMapping
          .read(DriftSqlType.int, data['${effectivePrefix}version'])!,
      updatedAt: attachedDatabase.typeMapping
          .read(DriftSqlType.string, data['${effectivePrefix}updated_at'])!,
      deletedAt: attachedDatabase.typeMapping
          .read(DriftSqlType.string, data['${effectivePrefix}deleted_at']),
      syncState: attachedDatabase.typeMapping
          .read(DriftSqlType.string, data['${effectivePrefix}sync_state'])!,
      payloadJson: attachedDatabase.typeMapping
          .read(DriftSqlType.string, data['${effectivePrefix}payload_json'])!,
    );
  }

  @override
  $RoutineStepLogsDriftTableTable createAlias(String alias) {
    return $RoutineStepLogsDriftTableTable(attachedDatabase, alias);
  }
}

class RoutineStepLogsDriftTableData extends DataClass
    implements Insertable<RoutineStepLogsDriftTableData> {
  final String id;
  final String userId;
  final int version;
  final String updatedAt;
  final String? deletedAt;
  final String syncState;
  final String payloadJson;
  const RoutineStepLogsDriftTableData(
      {required this.id,
      required this.userId,
      required this.version,
      required this.updatedAt,
      this.deletedAt,
      required this.syncState,
      required this.payloadJson});
  @override
  Map<String, Expression> toColumns(bool nullToAbsent) {
    final map = <String, Expression>{};
    map['id'] = Variable<String>(id);
    map['user_id'] = Variable<String>(userId);
    map['version'] = Variable<int>(version);
    map['updated_at'] = Variable<String>(updatedAt);
    if (!nullToAbsent || deletedAt != null) {
      map['deleted_at'] = Variable<String>(deletedAt);
    }
    map['sync_state'] = Variable<String>(syncState);
    map['payload_json'] = Variable<String>(payloadJson);
    return map;
  }

  RoutineStepLogsDriftTableCompanion toCompanion(bool nullToAbsent) {
    return RoutineStepLogsDriftTableCompanion(
      id: Value(id),
      userId: Value(userId),
      version: Value(version),
      updatedAt: Value(updatedAt),
      deletedAt: deletedAt == null && nullToAbsent
          ? const Value.absent()
          : Value(deletedAt),
      syncState: Value(syncState),
      payloadJson: Value(payloadJson),
    );
  }

  factory RoutineStepLogsDriftTableData.fromJson(Map<String, dynamic> json,
      {ValueSerializer? serializer}) {
    serializer ??= driftRuntimeOptions.defaultSerializer;
    return RoutineStepLogsDriftTableData(
      id: serializer.fromJson<String>(json['id']),
      userId: serializer.fromJson<String>(json['userId']),
      version: serializer.fromJson<int>(json['version']),
      updatedAt: serializer.fromJson<String>(json['updatedAt']),
      deletedAt: serializer.fromJson<String?>(json['deletedAt']),
      syncState: serializer.fromJson<String>(json['syncState']),
      payloadJson: serializer.fromJson<String>(json['payloadJson']),
    );
  }
  @override
  Map<String, dynamic> toJson({ValueSerializer? serializer}) {
    serializer ??= driftRuntimeOptions.defaultSerializer;
    return <String, dynamic>{
      'id': serializer.toJson<String>(id),
      'userId': serializer.toJson<String>(userId),
      'version': serializer.toJson<int>(version),
      'updatedAt': serializer.toJson<String>(updatedAt),
      'deletedAt': serializer.toJson<String?>(deletedAt),
      'syncState': serializer.toJson<String>(syncState),
      'payloadJson': serializer.toJson<String>(payloadJson),
    };
  }

  RoutineStepLogsDriftTableData copyWith(
          {String? id,
          String? userId,
          int? version,
          String? updatedAt,
          Value<String?> deletedAt = const Value.absent(),
          String? syncState,
          String? payloadJson}) =>
      RoutineStepLogsDriftTableData(
        id: id ?? this.id,
        userId: userId ?? this.userId,
        version: version ?? this.version,
        updatedAt: updatedAt ?? this.updatedAt,
        deletedAt: deletedAt.present ? deletedAt.value : this.deletedAt,
        syncState: syncState ?? this.syncState,
        payloadJson: payloadJson ?? this.payloadJson,
      );
  RoutineStepLogsDriftTableData copyWithCompanion(
      RoutineStepLogsDriftTableCompanion data) {
    return RoutineStepLogsDriftTableData(
      id: data.id.present ? data.id.value : this.id,
      userId: data.userId.present ? data.userId.value : this.userId,
      version: data.version.present ? data.version.value : this.version,
      updatedAt: data.updatedAt.present ? data.updatedAt.value : this.updatedAt,
      deletedAt: data.deletedAt.present ? data.deletedAt.value : this.deletedAt,
      syncState: data.syncState.present ? data.syncState.value : this.syncState,
      payloadJson:
          data.payloadJson.present ? data.payloadJson.value : this.payloadJson,
    );
  }

  @override
  String toString() {
    return (StringBuffer('RoutineStepLogsDriftTableData(')
          ..write('id: $id, ')
          ..write('userId: $userId, ')
          ..write('version: $version, ')
          ..write('updatedAt: $updatedAt, ')
          ..write('deletedAt: $deletedAt, ')
          ..write('syncState: $syncState, ')
          ..write('payloadJson: $payloadJson')
          ..write(')'))
        .toString();
  }

  @override
  int get hashCode => Object.hash(
      id, userId, version, updatedAt, deletedAt, syncState, payloadJson);
  @override
  bool operator ==(Object other) =>
      identical(this, other) ||
      (other is RoutineStepLogsDriftTableData &&
          other.id == this.id &&
          other.userId == this.userId &&
          other.version == this.version &&
          other.updatedAt == this.updatedAt &&
          other.deletedAt == this.deletedAt &&
          other.syncState == this.syncState &&
          other.payloadJson == this.payloadJson);
}

class RoutineStepLogsDriftTableCompanion
    extends UpdateCompanion<RoutineStepLogsDriftTableData> {
  final Value<String> id;
  final Value<String> userId;
  final Value<int> version;
  final Value<String> updatedAt;
  final Value<String?> deletedAt;
  final Value<String> syncState;
  final Value<String> payloadJson;
  final Value<int> rowid;
  const RoutineStepLogsDriftTableCompanion({
    this.id = const Value.absent(),
    this.userId = const Value.absent(),
    this.version = const Value.absent(),
    this.updatedAt = const Value.absent(),
    this.deletedAt = const Value.absent(),
    this.syncState = const Value.absent(),
    this.payloadJson = const Value.absent(),
    this.rowid = const Value.absent(),
  });
  RoutineStepLogsDriftTableCompanion.insert({
    required String id,
    required String userId,
    this.version = const Value.absent(),
    required String updatedAt,
    this.deletedAt = const Value.absent(),
    this.syncState = const Value.absent(),
    required String payloadJson,
    this.rowid = const Value.absent(),
  })  : id = Value(id),
        userId = Value(userId),
        updatedAt = Value(updatedAt),
        payloadJson = Value(payloadJson);
  static Insertable<RoutineStepLogsDriftTableData> custom({
    Expression<String>? id,
    Expression<String>? userId,
    Expression<int>? version,
    Expression<String>? updatedAt,
    Expression<String>? deletedAt,
    Expression<String>? syncState,
    Expression<String>? payloadJson,
    Expression<int>? rowid,
  }) {
    return RawValuesInsertable({
      if (id != null) 'id': id,
      if (userId != null) 'user_id': userId,
      if (version != null) 'version': version,
      if (updatedAt != null) 'updated_at': updatedAt,
      if (deletedAt != null) 'deleted_at': deletedAt,
      if (syncState != null) 'sync_state': syncState,
      if (payloadJson != null) 'payload_json': payloadJson,
      if (rowid != null) 'rowid': rowid,
    });
  }

  RoutineStepLogsDriftTableCompanion copyWith(
      {Value<String>? id,
      Value<String>? userId,
      Value<int>? version,
      Value<String>? updatedAt,
      Value<String?>? deletedAt,
      Value<String>? syncState,
      Value<String>? payloadJson,
      Value<int>? rowid}) {
    return RoutineStepLogsDriftTableCompanion(
      id: id ?? this.id,
      userId: userId ?? this.userId,
      version: version ?? this.version,
      updatedAt: updatedAt ?? this.updatedAt,
      deletedAt: deletedAt ?? this.deletedAt,
      syncState: syncState ?? this.syncState,
      payloadJson: payloadJson ?? this.payloadJson,
      rowid: rowid ?? this.rowid,
    );
  }

  @override
  Map<String, Expression> toColumns(bool nullToAbsent) {
    final map = <String, Expression>{};
    if (id.present) {
      map['id'] = Variable<String>(id.value);
    }
    if (userId.present) {
      map['user_id'] = Variable<String>(userId.value);
    }
    if (version.present) {
      map['version'] = Variable<int>(version.value);
    }
    if (updatedAt.present) {
      map['updated_at'] = Variable<String>(updatedAt.value);
    }
    if (deletedAt.present) {
      map['deleted_at'] = Variable<String>(deletedAt.value);
    }
    if (syncState.present) {
      map['sync_state'] = Variable<String>(syncState.value);
    }
    if (payloadJson.present) {
      map['payload_json'] = Variable<String>(payloadJson.value);
    }
    if (rowid.present) {
      map['rowid'] = Variable<int>(rowid.value);
    }
    return map;
  }

  @override
  String toString() {
    return (StringBuffer('RoutineStepLogsDriftTableCompanion(')
          ..write('id: $id, ')
          ..write('userId: $userId, ')
          ..write('version: $version, ')
          ..write('updatedAt: $updatedAt, ')
          ..write('deletedAt: $deletedAt, ')
          ..write('syncState: $syncState, ')
          ..write('payloadJson: $payloadJson, ')
          ..write('rowid: $rowid')
          ..write(')'))
        .toString();
  }
}

class $FinancialAccountsDriftTableTable extends FinancialAccountsDriftTable
    with
        TableInfo<$FinancialAccountsDriftTableTable,
            FinancialAccountsDriftTableData> {
  @override
  final GeneratedDatabase attachedDatabase;
  final String? _alias;
  $FinancialAccountsDriftTableTable(this.attachedDatabase, [this._alias]);
  static const VerificationMeta _idMeta = const VerificationMeta('id');
  @override
  late final GeneratedColumn<String> id = GeneratedColumn<String>(
      'id', aliasedName, false,
      type: DriftSqlType.string, requiredDuringInsert: true);
  static const VerificationMeta _userIdMeta = const VerificationMeta('userId');
  @override
  late final GeneratedColumn<String> userId = GeneratedColumn<String>(
      'user_id', aliasedName, false,
      type: DriftSqlType.string, requiredDuringInsert: true);
  static const VerificationMeta _versionMeta =
      const VerificationMeta('version');
  @override
  late final GeneratedColumn<int> version = GeneratedColumn<int>(
      'version', aliasedName, false,
      type: DriftSqlType.int,
      requiredDuringInsert: false,
      defaultValue: const Constant(0));
  static const VerificationMeta _updatedAtMeta =
      const VerificationMeta('updatedAt');
  @override
  late final GeneratedColumn<String> updatedAt = GeneratedColumn<String>(
      'updated_at', aliasedName, false,
      type: DriftSqlType.string, requiredDuringInsert: true);
  static const VerificationMeta _deletedAtMeta =
      const VerificationMeta('deletedAt');
  @override
  late final GeneratedColumn<String> deletedAt = GeneratedColumn<String>(
      'deleted_at', aliasedName, true,
      type: DriftSqlType.string, requiredDuringInsert: false);
  static const VerificationMeta _syncStateMeta =
      const VerificationMeta('syncState');
  @override
  late final GeneratedColumn<String> syncState = GeneratedColumn<String>(
      'sync_state', aliasedName, false,
      type: DriftSqlType.string,
      requiredDuringInsert: false,
      defaultValue: const Constant('synced'));
  static const VerificationMeta _payloadJsonMeta =
      const VerificationMeta('payloadJson');
  @override
  late final GeneratedColumn<String> payloadJson = GeneratedColumn<String>(
      'payload_json', aliasedName, false,
      type: DriftSqlType.string, requiredDuringInsert: true);
  @override
  List<GeneratedColumn> get $columns =>
      [id, userId, version, updatedAt, deletedAt, syncState, payloadJson];
  @override
  String get aliasedName => _alias ?? actualTableName;
  @override
  String get actualTableName => $name;
  static const String $name = 'financial_accounts';
  @override
  VerificationContext validateIntegrity(
      Insertable<FinancialAccountsDriftTableData> instance,
      {bool isInserting = false}) {
    final context = VerificationContext();
    final data = instance.toColumns(true);
    if (data.containsKey('id')) {
      context.handle(_idMeta, id.isAcceptableOrUnknown(data['id']!, _idMeta));
    } else if (isInserting) {
      context.missing(_idMeta);
    }
    if (data.containsKey('user_id')) {
      context.handle(_userIdMeta,
          userId.isAcceptableOrUnknown(data['user_id']!, _userIdMeta));
    } else if (isInserting) {
      context.missing(_userIdMeta);
    }
    if (data.containsKey('version')) {
      context.handle(_versionMeta,
          version.isAcceptableOrUnknown(data['version']!, _versionMeta));
    }
    if (data.containsKey('updated_at')) {
      context.handle(_updatedAtMeta,
          updatedAt.isAcceptableOrUnknown(data['updated_at']!, _updatedAtMeta));
    } else if (isInserting) {
      context.missing(_updatedAtMeta);
    }
    if (data.containsKey('deleted_at')) {
      context.handle(_deletedAtMeta,
          deletedAt.isAcceptableOrUnknown(data['deleted_at']!, _deletedAtMeta));
    }
    if (data.containsKey('sync_state')) {
      context.handle(_syncStateMeta,
          syncState.isAcceptableOrUnknown(data['sync_state']!, _syncStateMeta));
    }
    if (data.containsKey('payload_json')) {
      context.handle(
          _payloadJsonMeta,
          payloadJson.isAcceptableOrUnknown(
              data['payload_json']!, _payloadJsonMeta));
    } else if (isInserting) {
      context.missing(_payloadJsonMeta);
    }
    return context;
  }

  @override
  Set<GeneratedColumn> get $primaryKey => {userId, id};
  @override
  FinancialAccountsDriftTableData map(Map<String, dynamic> data,
      {String? tablePrefix}) {
    final effectivePrefix = tablePrefix != null ? '$tablePrefix.' : '';
    return FinancialAccountsDriftTableData(
      id: attachedDatabase.typeMapping
          .read(DriftSqlType.string, data['${effectivePrefix}id'])!,
      userId: attachedDatabase.typeMapping
          .read(DriftSqlType.string, data['${effectivePrefix}user_id'])!,
      version: attachedDatabase.typeMapping
          .read(DriftSqlType.int, data['${effectivePrefix}version'])!,
      updatedAt: attachedDatabase.typeMapping
          .read(DriftSqlType.string, data['${effectivePrefix}updated_at'])!,
      deletedAt: attachedDatabase.typeMapping
          .read(DriftSqlType.string, data['${effectivePrefix}deleted_at']),
      syncState: attachedDatabase.typeMapping
          .read(DriftSqlType.string, data['${effectivePrefix}sync_state'])!,
      payloadJson: attachedDatabase.typeMapping
          .read(DriftSqlType.string, data['${effectivePrefix}payload_json'])!,
    );
  }

  @override
  $FinancialAccountsDriftTableTable createAlias(String alias) {
    return $FinancialAccountsDriftTableTable(attachedDatabase, alias);
  }
}

class FinancialAccountsDriftTableData extends DataClass
    implements Insertable<FinancialAccountsDriftTableData> {
  final String id;
  final String userId;
  final int version;
  final String updatedAt;
  final String? deletedAt;
  final String syncState;
  final String payloadJson;
  const FinancialAccountsDriftTableData(
      {required this.id,
      required this.userId,
      required this.version,
      required this.updatedAt,
      this.deletedAt,
      required this.syncState,
      required this.payloadJson});
  @override
  Map<String, Expression> toColumns(bool nullToAbsent) {
    final map = <String, Expression>{};
    map['id'] = Variable<String>(id);
    map['user_id'] = Variable<String>(userId);
    map['version'] = Variable<int>(version);
    map['updated_at'] = Variable<String>(updatedAt);
    if (!nullToAbsent || deletedAt != null) {
      map['deleted_at'] = Variable<String>(deletedAt);
    }
    map['sync_state'] = Variable<String>(syncState);
    map['payload_json'] = Variable<String>(payloadJson);
    return map;
  }

  FinancialAccountsDriftTableCompanion toCompanion(bool nullToAbsent) {
    return FinancialAccountsDriftTableCompanion(
      id: Value(id),
      userId: Value(userId),
      version: Value(version),
      updatedAt: Value(updatedAt),
      deletedAt: deletedAt == null && nullToAbsent
          ? const Value.absent()
          : Value(deletedAt),
      syncState: Value(syncState),
      payloadJson: Value(payloadJson),
    );
  }

  factory FinancialAccountsDriftTableData.fromJson(Map<String, dynamic> json,
      {ValueSerializer? serializer}) {
    serializer ??= driftRuntimeOptions.defaultSerializer;
    return FinancialAccountsDriftTableData(
      id: serializer.fromJson<String>(json['id']),
      userId: serializer.fromJson<String>(json['userId']),
      version: serializer.fromJson<int>(json['version']),
      updatedAt: serializer.fromJson<String>(json['updatedAt']),
      deletedAt: serializer.fromJson<String?>(json['deletedAt']),
      syncState: serializer.fromJson<String>(json['syncState']),
      payloadJson: serializer.fromJson<String>(json['payloadJson']),
    );
  }
  @override
  Map<String, dynamic> toJson({ValueSerializer? serializer}) {
    serializer ??= driftRuntimeOptions.defaultSerializer;
    return <String, dynamic>{
      'id': serializer.toJson<String>(id),
      'userId': serializer.toJson<String>(userId),
      'version': serializer.toJson<int>(version),
      'updatedAt': serializer.toJson<String>(updatedAt),
      'deletedAt': serializer.toJson<String?>(deletedAt),
      'syncState': serializer.toJson<String>(syncState),
      'payloadJson': serializer.toJson<String>(payloadJson),
    };
  }

  FinancialAccountsDriftTableData copyWith(
          {String? id,
          String? userId,
          int? version,
          String? updatedAt,
          Value<String?> deletedAt = const Value.absent(),
          String? syncState,
          String? payloadJson}) =>
      FinancialAccountsDriftTableData(
        id: id ?? this.id,
        userId: userId ?? this.userId,
        version: version ?? this.version,
        updatedAt: updatedAt ?? this.updatedAt,
        deletedAt: deletedAt.present ? deletedAt.value : this.deletedAt,
        syncState: syncState ?? this.syncState,
        payloadJson: payloadJson ?? this.payloadJson,
      );
  FinancialAccountsDriftTableData copyWithCompanion(
      FinancialAccountsDriftTableCompanion data) {
    return FinancialAccountsDriftTableData(
      id: data.id.present ? data.id.value : this.id,
      userId: data.userId.present ? data.userId.value : this.userId,
      version: data.version.present ? data.version.value : this.version,
      updatedAt: data.updatedAt.present ? data.updatedAt.value : this.updatedAt,
      deletedAt: data.deletedAt.present ? data.deletedAt.value : this.deletedAt,
      syncState: data.syncState.present ? data.syncState.value : this.syncState,
      payloadJson:
          data.payloadJson.present ? data.payloadJson.value : this.payloadJson,
    );
  }

  @override
  String toString() {
    return (StringBuffer('FinancialAccountsDriftTableData(')
          ..write('id: $id, ')
          ..write('userId: $userId, ')
          ..write('version: $version, ')
          ..write('updatedAt: $updatedAt, ')
          ..write('deletedAt: $deletedAt, ')
          ..write('syncState: $syncState, ')
          ..write('payloadJson: $payloadJson')
          ..write(')'))
        .toString();
  }

  @override
  int get hashCode => Object.hash(
      id, userId, version, updatedAt, deletedAt, syncState, payloadJson);
  @override
  bool operator ==(Object other) =>
      identical(this, other) ||
      (other is FinancialAccountsDriftTableData &&
          other.id == this.id &&
          other.userId == this.userId &&
          other.version == this.version &&
          other.updatedAt == this.updatedAt &&
          other.deletedAt == this.deletedAt &&
          other.syncState == this.syncState &&
          other.payloadJson == this.payloadJson);
}

class FinancialAccountsDriftTableCompanion
    extends UpdateCompanion<FinancialAccountsDriftTableData> {
  final Value<String> id;
  final Value<String> userId;
  final Value<int> version;
  final Value<String> updatedAt;
  final Value<String?> deletedAt;
  final Value<String> syncState;
  final Value<String> payloadJson;
  final Value<int> rowid;
  const FinancialAccountsDriftTableCompanion({
    this.id = const Value.absent(),
    this.userId = const Value.absent(),
    this.version = const Value.absent(),
    this.updatedAt = const Value.absent(),
    this.deletedAt = const Value.absent(),
    this.syncState = const Value.absent(),
    this.payloadJson = const Value.absent(),
    this.rowid = const Value.absent(),
  });
  FinancialAccountsDriftTableCompanion.insert({
    required String id,
    required String userId,
    this.version = const Value.absent(),
    required String updatedAt,
    this.deletedAt = const Value.absent(),
    this.syncState = const Value.absent(),
    required String payloadJson,
    this.rowid = const Value.absent(),
  })  : id = Value(id),
        userId = Value(userId),
        updatedAt = Value(updatedAt),
        payloadJson = Value(payloadJson);
  static Insertable<FinancialAccountsDriftTableData> custom({
    Expression<String>? id,
    Expression<String>? userId,
    Expression<int>? version,
    Expression<String>? updatedAt,
    Expression<String>? deletedAt,
    Expression<String>? syncState,
    Expression<String>? payloadJson,
    Expression<int>? rowid,
  }) {
    return RawValuesInsertable({
      if (id != null) 'id': id,
      if (userId != null) 'user_id': userId,
      if (version != null) 'version': version,
      if (updatedAt != null) 'updated_at': updatedAt,
      if (deletedAt != null) 'deleted_at': deletedAt,
      if (syncState != null) 'sync_state': syncState,
      if (payloadJson != null) 'payload_json': payloadJson,
      if (rowid != null) 'rowid': rowid,
    });
  }

  FinancialAccountsDriftTableCompanion copyWith(
      {Value<String>? id,
      Value<String>? userId,
      Value<int>? version,
      Value<String>? updatedAt,
      Value<String?>? deletedAt,
      Value<String>? syncState,
      Value<String>? payloadJson,
      Value<int>? rowid}) {
    return FinancialAccountsDriftTableCompanion(
      id: id ?? this.id,
      userId: userId ?? this.userId,
      version: version ?? this.version,
      updatedAt: updatedAt ?? this.updatedAt,
      deletedAt: deletedAt ?? this.deletedAt,
      syncState: syncState ?? this.syncState,
      payloadJson: payloadJson ?? this.payloadJson,
      rowid: rowid ?? this.rowid,
    );
  }

  @override
  Map<String, Expression> toColumns(bool nullToAbsent) {
    final map = <String, Expression>{};
    if (id.present) {
      map['id'] = Variable<String>(id.value);
    }
    if (userId.present) {
      map['user_id'] = Variable<String>(userId.value);
    }
    if (version.present) {
      map['version'] = Variable<int>(version.value);
    }
    if (updatedAt.present) {
      map['updated_at'] = Variable<String>(updatedAt.value);
    }
    if (deletedAt.present) {
      map['deleted_at'] = Variable<String>(deletedAt.value);
    }
    if (syncState.present) {
      map['sync_state'] = Variable<String>(syncState.value);
    }
    if (payloadJson.present) {
      map['payload_json'] = Variable<String>(payloadJson.value);
    }
    if (rowid.present) {
      map['rowid'] = Variable<int>(rowid.value);
    }
    return map;
  }

  @override
  String toString() {
    return (StringBuffer('FinancialAccountsDriftTableCompanion(')
          ..write('id: $id, ')
          ..write('userId: $userId, ')
          ..write('version: $version, ')
          ..write('updatedAt: $updatedAt, ')
          ..write('deletedAt: $deletedAt, ')
          ..write('syncState: $syncState, ')
          ..write('payloadJson: $payloadJson, ')
          ..write('rowid: $rowid')
          ..write(')'))
        .toString();
  }
}

class $FinancialCategoriesDriftTableTable extends FinancialCategoriesDriftTable
    with
        TableInfo<$FinancialCategoriesDriftTableTable,
            FinancialCategoriesDriftTableData> {
  @override
  final GeneratedDatabase attachedDatabase;
  final String? _alias;
  $FinancialCategoriesDriftTableTable(this.attachedDatabase, [this._alias]);
  static const VerificationMeta _idMeta = const VerificationMeta('id');
  @override
  late final GeneratedColumn<String> id = GeneratedColumn<String>(
      'id', aliasedName, false,
      type: DriftSqlType.string, requiredDuringInsert: true);
  static const VerificationMeta _userIdMeta = const VerificationMeta('userId');
  @override
  late final GeneratedColumn<String> userId = GeneratedColumn<String>(
      'user_id', aliasedName, false,
      type: DriftSqlType.string, requiredDuringInsert: true);
  static const VerificationMeta _versionMeta =
      const VerificationMeta('version');
  @override
  late final GeneratedColumn<int> version = GeneratedColumn<int>(
      'version', aliasedName, false,
      type: DriftSqlType.int,
      requiredDuringInsert: false,
      defaultValue: const Constant(0));
  static const VerificationMeta _updatedAtMeta =
      const VerificationMeta('updatedAt');
  @override
  late final GeneratedColumn<String> updatedAt = GeneratedColumn<String>(
      'updated_at', aliasedName, false,
      type: DriftSqlType.string, requiredDuringInsert: true);
  static const VerificationMeta _deletedAtMeta =
      const VerificationMeta('deletedAt');
  @override
  late final GeneratedColumn<String> deletedAt = GeneratedColumn<String>(
      'deleted_at', aliasedName, true,
      type: DriftSqlType.string, requiredDuringInsert: false);
  static const VerificationMeta _syncStateMeta =
      const VerificationMeta('syncState');
  @override
  late final GeneratedColumn<String> syncState = GeneratedColumn<String>(
      'sync_state', aliasedName, false,
      type: DriftSqlType.string,
      requiredDuringInsert: false,
      defaultValue: const Constant('synced'));
  static const VerificationMeta _payloadJsonMeta =
      const VerificationMeta('payloadJson');
  @override
  late final GeneratedColumn<String> payloadJson = GeneratedColumn<String>(
      'payload_json', aliasedName, false,
      type: DriftSqlType.string, requiredDuringInsert: true);
  @override
  List<GeneratedColumn> get $columns =>
      [id, userId, version, updatedAt, deletedAt, syncState, payloadJson];
  @override
  String get aliasedName => _alias ?? actualTableName;
  @override
  String get actualTableName => $name;
  static const String $name = 'financial_categories';
  @override
  VerificationContext validateIntegrity(
      Insertable<FinancialCategoriesDriftTableData> instance,
      {bool isInserting = false}) {
    final context = VerificationContext();
    final data = instance.toColumns(true);
    if (data.containsKey('id')) {
      context.handle(_idMeta, id.isAcceptableOrUnknown(data['id']!, _idMeta));
    } else if (isInserting) {
      context.missing(_idMeta);
    }
    if (data.containsKey('user_id')) {
      context.handle(_userIdMeta,
          userId.isAcceptableOrUnknown(data['user_id']!, _userIdMeta));
    } else if (isInserting) {
      context.missing(_userIdMeta);
    }
    if (data.containsKey('version')) {
      context.handle(_versionMeta,
          version.isAcceptableOrUnknown(data['version']!, _versionMeta));
    }
    if (data.containsKey('updated_at')) {
      context.handle(_updatedAtMeta,
          updatedAt.isAcceptableOrUnknown(data['updated_at']!, _updatedAtMeta));
    } else if (isInserting) {
      context.missing(_updatedAtMeta);
    }
    if (data.containsKey('deleted_at')) {
      context.handle(_deletedAtMeta,
          deletedAt.isAcceptableOrUnknown(data['deleted_at']!, _deletedAtMeta));
    }
    if (data.containsKey('sync_state')) {
      context.handle(_syncStateMeta,
          syncState.isAcceptableOrUnknown(data['sync_state']!, _syncStateMeta));
    }
    if (data.containsKey('payload_json')) {
      context.handle(
          _payloadJsonMeta,
          payloadJson.isAcceptableOrUnknown(
              data['payload_json']!, _payloadJsonMeta));
    } else if (isInserting) {
      context.missing(_payloadJsonMeta);
    }
    return context;
  }

  @override
  Set<GeneratedColumn> get $primaryKey => {userId, id};
  @override
  FinancialCategoriesDriftTableData map(Map<String, dynamic> data,
      {String? tablePrefix}) {
    final effectivePrefix = tablePrefix != null ? '$tablePrefix.' : '';
    return FinancialCategoriesDriftTableData(
      id: attachedDatabase.typeMapping
          .read(DriftSqlType.string, data['${effectivePrefix}id'])!,
      userId: attachedDatabase.typeMapping
          .read(DriftSqlType.string, data['${effectivePrefix}user_id'])!,
      version: attachedDatabase.typeMapping
          .read(DriftSqlType.int, data['${effectivePrefix}version'])!,
      updatedAt: attachedDatabase.typeMapping
          .read(DriftSqlType.string, data['${effectivePrefix}updated_at'])!,
      deletedAt: attachedDatabase.typeMapping
          .read(DriftSqlType.string, data['${effectivePrefix}deleted_at']),
      syncState: attachedDatabase.typeMapping
          .read(DriftSqlType.string, data['${effectivePrefix}sync_state'])!,
      payloadJson: attachedDatabase.typeMapping
          .read(DriftSqlType.string, data['${effectivePrefix}payload_json'])!,
    );
  }

  @override
  $FinancialCategoriesDriftTableTable createAlias(String alias) {
    return $FinancialCategoriesDriftTableTable(attachedDatabase, alias);
  }
}

class FinancialCategoriesDriftTableData extends DataClass
    implements Insertable<FinancialCategoriesDriftTableData> {
  final String id;
  final String userId;
  final int version;
  final String updatedAt;
  final String? deletedAt;
  final String syncState;
  final String payloadJson;
  const FinancialCategoriesDriftTableData(
      {required this.id,
      required this.userId,
      required this.version,
      required this.updatedAt,
      this.deletedAt,
      required this.syncState,
      required this.payloadJson});
  @override
  Map<String, Expression> toColumns(bool nullToAbsent) {
    final map = <String, Expression>{};
    map['id'] = Variable<String>(id);
    map['user_id'] = Variable<String>(userId);
    map['version'] = Variable<int>(version);
    map['updated_at'] = Variable<String>(updatedAt);
    if (!nullToAbsent || deletedAt != null) {
      map['deleted_at'] = Variable<String>(deletedAt);
    }
    map['sync_state'] = Variable<String>(syncState);
    map['payload_json'] = Variable<String>(payloadJson);
    return map;
  }

  FinancialCategoriesDriftTableCompanion toCompanion(bool nullToAbsent) {
    return FinancialCategoriesDriftTableCompanion(
      id: Value(id),
      userId: Value(userId),
      version: Value(version),
      updatedAt: Value(updatedAt),
      deletedAt: deletedAt == null && nullToAbsent
          ? const Value.absent()
          : Value(deletedAt),
      syncState: Value(syncState),
      payloadJson: Value(payloadJson),
    );
  }

  factory FinancialCategoriesDriftTableData.fromJson(Map<String, dynamic> json,
      {ValueSerializer? serializer}) {
    serializer ??= driftRuntimeOptions.defaultSerializer;
    return FinancialCategoriesDriftTableData(
      id: serializer.fromJson<String>(json['id']),
      userId: serializer.fromJson<String>(json['userId']),
      version: serializer.fromJson<int>(json['version']),
      updatedAt: serializer.fromJson<String>(json['updatedAt']),
      deletedAt: serializer.fromJson<String?>(json['deletedAt']),
      syncState: serializer.fromJson<String>(json['syncState']),
      payloadJson: serializer.fromJson<String>(json['payloadJson']),
    );
  }
  @override
  Map<String, dynamic> toJson({ValueSerializer? serializer}) {
    serializer ??= driftRuntimeOptions.defaultSerializer;
    return <String, dynamic>{
      'id': serializer.toJson<String>(id),
      'userId': serializer.toJson<String>(userId),
      'version': serializer.toJson<int>(version),
      'updatedAt': serializer.toJson<String>(updatedAt),
      'deletedAt': serializer.toJson<String?>(deletedAt),
      'syncState': serializer.toJson<String>(syncState),
      'payloadJson': serializer.toJson<String>(payloadJson),
    };
  }

  FinancialCategoriesDriftTableData copyWith(
          {String? id,
          String? userId,
          int? version,
          String? updatedAt,
          Value<String?> deletedAt = const Value.absent(),
          String? syncState,
          String? payloadJson}) =>
      FinancialCategoriesDriftTableData(
        id: id ?? this.id,
        userId: userId ?? this.userId,
        version: version ?? this.version,
        updatedAt: updatedAt ?? this.updatedAt,
        deletedAt: deletedAt.present ? deletedAt.value : this.deletedAt,
        syncState: syncState ?? this.syncState,
        payloadJson: payloadJson ?? this.payloadJson,
      );
  FinancialCategoriesDriftTableData copyWithCompanion(
      FinancialCategoriesDriftTableCompanion data) {
    return FinancialCategoriesDriftTableData(
      id: data.id.present ? data.id.value : this.id,
      userId: data.userId.present ? data.userId.value : this.userId,
      version: data.version.present ? data.version.value : this.version,
      updatedAt: data.updatedAt.present ? data.updatedAt.value : this.updatedAt,
      deletedAt: data.deletedAt.present ? data.deletedAt.value : this.deletedAt,
      syncState: data.syncState.present ? data.syncState.value : this.syncState,
      payloadJson:
          data.payloadJson.present ? data.payloadJson.value : this.payloadJson,
    );
  }

  @override
  String toString() {
    return (StringBuffer('FinancialCategoriesDriftTableData(')
          ..write('id: $id, ')
          ..write('userId: $userId, ')
          ..write('version: $version, ')
          ..write('updatedAt: $updatedAt, ')
          ..write('deletedAt: $deletedAt, ')
          ..write('syncState: $syncState, ')
          ..write('payloadJson: $payloadJson')
          ..write(')'))
        .toString();
  }

  @override
  int get hashCode => Object.hash(
      id, userId, version, updatedAt, deletedAt, syncState, payloadJson);
  @override
  bool operator ==(Object other) =>
      identical(this, other) ||
      (other is FinancialCategoriesDriftTableData &&
          other.id == this.id &&
          other.userId == this.userId &&
          other.version == this.version &&
          other.updatedAt == this.updatedAt &&
          other.deletedAt == this.deletedAt &&
          other.syncState == this.syncState &&
          other.payloadJson == this.payloadJson);
}

class FinancialCategoriesDriftTableCompanion
    extends UpdateCompanion<FinancialCategoriesDriftTableData> {
  final Value<String> id;
  final Value<String> userId;
  final Value<int> version;
  final Value<String> updatedAt;
  final Value<String?> deletedAt;
  final Value<String> syncState;
  final Value<String> payloadJson;
  final Value<int> rowid;
  const FinancialCategoriesDriftTableCompanion({
    this.id = const Value.absent(),
    this.userId = const Value.absent(),
    this.version = const Value.absent(),
    this.updatedAt = const Value.absent(),
    this.deletedAt = const Value.absent(),
    this.syncState = const Value.absent(),
    this.payloadJson = const Value.absent(),
    this.rowid = const Value.absent(),
  });
  FinancialCategoriesDriftTableCompanion.insert({
    required String id,
    required String userId,
    this.version = const Value.absent(),
    required String updatedAt,
    this.deletedAt = const Value.absent(),
    this.syncState = const Value.absent(),
    required String payloadJson,
    this.rowid = const Value.absent(),
  })  : id = Value(id),
        userId = Value(userId),
        updatedAt = Value(updatedAt),
        payloadJson = Value(payloadJson);
  static Insertable<FinancialCategoriesDriftTableData> custom({
    Expression<String>? id,
    Expression<String>? userId,
    Expression<int>? version,
    Expression<String>? updatedAt,
    Expression<String>? deletedAt,
    Expression<String>? syncState,
    Expression<String>? payloadJson,
    Expression<int>? rowid,
  }) {
    return RawValuesInsertable({
      if (id != null) 'id': id,
      if (userId != null) 'user_id': userId,
      if (version != null) 'version': version,
      if (updatedAt != null) 'updated_at': updatedAt,
      if (deletedAt != null) 'deleted_at': deletedAt,
      if (syncState != null) 'sync_state': syncState,
      if (payloadJson != null) 'payload_json': payloadJson,
      if (rowid != null) 'rowid': rowid,
    });
  }

  FinancialCategoriesDriftTableCompanion copyWith(
      {Value<String>? id,
      Value<String>? userId,
      Value<int>? version,
      Value<String>? updatedAt,
      Value<String?>? deletedAt,
      Value<String>? syncState,
      Value<String>? payloadJson,
      Value<int>? rowid}) {
    return FinancialCategoriesDriftTableCompanion(
      id: id ?? this.id,
      userId: userId ?? this.userId,
      version: version ?? this.version,
      updatedAt: updatedAt ?? this.updatedAt,
      deletedAt: deletedAt ?? this.deletedAt,
      syncState: syncState ?? this.syncState,
      payloadJson: payloadJson ?? this.payloadJson,
      rowid: rowid ?? this.rowid,
    );
  }

  @override
  Map<String, Expression> toColumns(bool nullToAbsent) {
    final map = <String, Expression>{};
    if (id.present) {
      map['id'] = Variable<String>(id.value);
    }
    if (userId.present) {
      map['user_id'] = Variable<String>(userId.value);
    }
    if (version.present) {
      map['version'] = Variable<int>(version.value);
    }
    if (updatedAt.present) {
      map['updated_at'] = Variable<String>(updatedAt.value);
    }
    if (deletedAt.present) {
      map['deleted_at'] = Variable<String>(deletedAt.value);
    }
    if (syncState.present) {
      map['sync_state'] = Variable<String>(syncState.value);
    }
    if (payloadJson.present) {
      map['payload_json'] = Variable<String>(payloadJson.value);
    }
    if (rowid.present) {
      map['rowid'] = Variable<int>(rowid.value);
    }
    return map;
  }

  @override
  String toString() {
    return (StringBuffer('FinancialCategoriesDriftTableCompanion(')
          ..write('id: $id, ')
          ..write('userId: $userId, ')
          ..write('version: $version, ')
          ..write('updatedAt: $updatedAt, ')
          ..write('deletedAt: $deletedAt, ')
          ..write('syncState: $syncState, ')
          ..write('payloadJson: $payloadJson, ')
          ..write('rowid: $rowid')
          ..write(')'))
        .toString();
  }
}

class $FinancialTransactionsDriftTableTable
    extends FinancialTransactionsDriftTable
    with
        TableInfo<$FinancialTransactionsDriftTableTable,
            FinancialTransactionsDriftTableData> {
  @override
  final GeneratedDatabase attachedDatabase;
  final String? _alias;
  $FinancialTransactionsDriftTableTable(this.attachedDatabase, [this._alias]);
  static const VerificationMeta _idMeta = const VerificationMeta('id');
  @override
  late final GeneratedColumn<String> id = GeneratedColumn<String>(
      'id', aliasedName, false,
      type: DriftSqlType.string, requiredDuringInsert: true);
  static const VerificationMeta _userIdMeta = const VerificationMeta('userId');
  @override
  late final GeneratedColumn<String> userId = GeneratedColumn<String>(
      'user_id', aliasedName, false,
      type: DriftSqlType.string, requiredDuringInsert: true);
  static const VerificationMeta _versionMeta =
      const VerificationMeta('version');
  @override
  late final GeneratedColumn<int> version = GeneratedColumn<int>(
      'version', aliasedName, false,
      type: DriftSqlType.int,
      requiredDuringInsert: false,
      defaultValue: const Constant(0));
  static const VerificationMeta _updatedAtMeta =
      const VerificationMeta('updatedAt');
  @override
  late final GeneratedColumn<String> updatedAt = GeneratedColumn<String>(
      'updated_at', aliasedName, false,
      type: DriftSqlType.string, requiredDuringInsert: true);
  static const VerificationMeta _deletedAtMeta =
      const VerificationMeta('deletedAt');
  @override
  late final GeneratedColumn<String> deletedAt = GeneratedColumn<String>(
      'deleted_at', aliasedName, true,
      type: DriftSqlType.string, requiredDuringInsert: false);
  static const VerificationMeta _syncStateMeta =
      const VerificationMeta('syncState');
  @override
  late final GeneratedColumn<String> syncState = GeneratedColumn<String>(
      'sync_state', aliasedName, false,
      type: DriftSqlType.string,
      requiredDuringInsert: false,
      defaultValue: const Constant('synced'));
  static const VerificationMeta _payloadJsonMeta =
      const VerificationMeta('payloadJson');
  @override
  late final GeneratedColumn<String> payloadJson = GeneratedColumn<String>(
      'payload_json', aliasedName, false,
      type: DriftSqlType.string, requiredDuringInsert: true);
  @override
  List<GeneratedColumn> get $columns =>
      [id, userId, version, updatedAt, deletedAt, syncState, payloadJson];
  @override
  String get aliasedName => _alias ?? actualTableName;
  @override
  String get actualTableName => $name;
  static const String $name = 'financial_transactions';
  @override
  VerificationContext validateIntegrity(
      Insertable<FinancialTransactionsDriftTableData> instance,
      {bool isInserting = false}) {
    final context = VerificationContext();
    final data = instance.toColumns(true);
    if (data.containsKey('id')) {
      context.handle(_idMeta, id.isAcceptableOrUnknown(data['id']!, _idMeta));
    } else if (isInserting) {
      context.missing(_idMeta);
    }
    if (data.containsKey('user_id')) {
      context.handle(_userIdMeta,
          userId.isAcceptableOrUnknown(data['user_id']!, _userIdMeta));
    } else if (isInserting) {
      context.missing(_userIdMeta);
    }
    if (data.containsKey('version')) {
      context.handle(_versionMeta,
          version.isAcceptableOrUnknown(data['version']!, _versionMeta));
    }
    if (data.containsKey('updated_at')) {
      context.handle(_updatedAtMeta,
          updatedAt.isAcceptableOrUnknown(data['updated_at']!, _updatedAtMeta));
    } else if (isInserting) {
      context.missing(_updatedAtMeta);
    }
    if (data.containsKey('deleted_at')) {
      context.handle(_deletedAtMeta,
          deletedAt.isAcceptableOrUnknown(data['deleted_at']!, _deletedAtMeta));
    }
    if (data.containsKey('sync_state')) {
      context.handle(_syncStateMeta,
          syncState.isAcceptableOrUnknown(data['sync_state']!, _syncStateMeta));
    }
    if (data.containsKey('payload_json')) {
      context.handle(
          _payloadJsonMeta,
          payloadJson.isAcceptableOrUnknown(
              data['payload_json']!, _payloadJsonMeta));
    } else if (isInserting) {
      context.missing(_payloadJsonMeta);
    }
    return context;
  }

  @override
  Set<GeneratedColumn> get $primaryKey => {userId, id};
  @override
  FinancialTransactionsDriftTableData map(Map<String, dynamic> data,
      {String? tablePrefix}) {
    final effectivePrefix = tablePrefix != null ? '$tablePrefix.' : '';
    return FinancialTransactionsDriftTableData(
      id: attachedDatabase.typeMapping
          .read(DriftSqlType.string, data['${effectivePrefix}id'])!,
      userId: attachedDatabase.typeMapping
          .read(DriftSqlType.string, data['${effectivePrefix}user_id'])!,
      version: attachedDatabase.typeMapping
          .read(DriftSqlType.int, data['${effectivePrefix}version'])!,
      updatedAt: attachedDatabase.typeMapping
          .read(DriftSqlType.string, data['${effectivePrefix}updated_at'])!,
      deletedAt: attachedDatabase.typeMapping
          .read(DriftSqlType.string, data['${effectivePrefix}deleted_at']),
      syncState: attachedDatabase.typeMapping
          .read(DriftSqlType.string, data['${effectivePrefix}sync_state'])!,
      payloadJson: attachedDatabase.typeMapping
          .read(DriftSqlType.string, data['${effectivePrefix}payload_json'])!,
    );
  }

  @override
  $FinancialTransactionsDriftTableTable createAlias(String alias) {
    return $FinancialTransactionsDriftTableTable(attachedDatabase, alias);
  }
}

class FinancialTransactionsDriftTableData extends DataClass
    implements Insertable<FinancialTransactionsDriftTableData> {
  final String id;
  final String userId;
  final int version;
  final String updatedAt;
  final String? deletedAt;
  final String syncState;
  final String payloadJson;
  const FinancialTransactionsDriftTableData(
      {required this.id,
      required this.userId,
      required this.version,
      required this.updatedAt,
      this.deletedAt,
      required this.syncState,
      required this.payloadJson});
  @override
  Map<String, Expression> toColumns(bool nullToAbsent) {
    final map = <String, Expression>{};
    map['id'] = Variable<String>(id);
    map['user_id'] = Variable<String>(userId);
    map['version'] = Variable<int>(version);
    map['updated_at'] = Variable<String>(updatedAt);
    if (!nullToAbsent || deletedAt != null) {
      map['deleted_at'] = Variable<String>(deletedAt);
    }
    map['sync_state'] = Variable<String>(syncState);
    map['payload_json'] = Variable<String>(payloadJson);
    return map;
  }

  FinancialTransactionsDriftTableCompanion toCompanion(bool nullToAbsent) {
    return FinancialTransactionsDriftTableCompanion(
      id: Value(id),
      userId: Value(userId),
      version: Value(version),
      updatedAt: Value(updatedAt),
      deletedAt: deletedAt == null && nullToAbsent
          ? const Value.absent()
          : Value(deletedAt),
      syncState: Value(syncState),
      payloadJson: Value(payloadJson),
    );
  }

  factory FinancialTransactionsDriftTableData.fromJson(
      Map<String, dynamic> json,
      {ValueSerializer? serializer}) {
    serializer ??= driftRuntimeOptions.defaultSerializer;
    return FinancialTransactionsDriftTableData(
      id: serializer.fromJson<String>(json['id']),
      userId: serializer.fromJson<String>(json['userId']),
      version: serializer.fromJson<int>(json['version']),
      updatedAt: serializer.fromJson<String>(json['updatedAt']),
      deletedAt: serializer.fromJson<String?>(json['deletedAt']),
      syncState: serializer.fromJson<String>(json['syncState']),
      payloadJson: serializer.fromJson<String>(json['payloadJson']),
    );
  }
  @override
  Map<String, dynamic> toJson({ValueSerializer? serializer}) {
    serializer ??= driftRuntimeOptions.defaultSerializer;
    return <String, dynamic>{
      'id': serializer.toJson<String>(id),
      'userId': serializer.toJson<String>(userId),
      'version': serializer.toJson<int>(version),
      'updatedAt': serializer.toJson<String>(updatedAt),
      'deletedAt': serializer.toJson<String?>(deletedAt),
      'syncState': serializer.toJson<String>(syncState),
      'payloadJson': serializer.toJson<String>(payloadJson),
    };
  }

  FinancialTransactionsDriftTableData copyWith(
          {String? id,
          String? userId,
          int? version,
          String? updatedAt,
          Value<String?> deletedAt = const Value.absent(),
          String? syncState,
          String? payloadJson}) =>
      FinancialTransactionsDriftTableData(
        id: id ?? this.id,
        userId: userId ?? this.userId,
        version: version ?? this.version,
        updatedAt: updatedAt ?? this.updatedAt,
        deletedAt: deletedAt.present ? deletedAt.value : this.deletedAt,
        syncState: syncState ?? this.syncState,
        payloadJson: payloadJson ?? this.payloadJson,
      );
  FinancialTransactionsDriftTableData copyWithCompanion(
      FinancialTransactionsDriftTableCompanion data) {
    return FinancialTransactionsDriftTableData(
      id: data.id.present ? data.id.value : this.id,
      userId: data.userId.present ? data.userId.value : this.userId,
      version: data.version.present ? data.version.value : this.version,
      updatedAt: data.updatedAt.present ? data.updatedAt.value : this.updatedAt,
      deletedAt: data.deletedAt.present ? data.deletedAt.value : this.deletedAt,
      syncState: data.syncState.present ? data.syncState.value : this.syncState,
      payloadJson:
          data.payloadJson.present ? data.payloadJson.value : this.payloadJson,
    );
  }

  @override
  String toString() {
    return (StringBuffer('FinancialTransactionsDriftTableData(')
          ..write('id: $id, ')
          ..write('userId: $userId, ')
          ..write('version: $version, ')
          ..write('updatedAt: $updatedAt, ')
          ..write('deletedAt: $deletedAt, ')
          ..write('syncState: $syncState, ')
          ..write('payloadJson: $payloadJson')
          ..write(')'))
        .toString();
  }

  @override
  int get hashCode => Object.hash(
      id, userId, version, updatedAt, deletedAt, syncState, payloadJson);
  @override
  bool operator ==(Object other) =>
      identical(this, other) ||
      (other is FinancialTransactionsDriftTableData &&
          other.id == this.id &&
          other.userId == this.userId &&
          other.version == this.version &&
          other.updatedAt == this.updatedAt &&
          other.deletedAt == this.deletedAt &&
          other.syncState == this.syncState &&
          other.payloadJson == this.payloadJson);
}

class FinancialTransactionsDriftTableCompanion
    extends UpdateCompanion<FinancialTransactionsDriftTableData> {
  final Value<String> id;
  final Value<String> userId;
  final Value<int> version;
  final Value<String> updatedAt;
  final Value<String?> deletedAt;
  final Value<String> syncState;
  final Value<String> payloadJson;
  final Value<int> rowid;
  const FinancialTransactionsDriftTableCompanion({
    this.id = const Value.absent(),
    this.userId = const Value.absent(),
    this.version = const Value.absent(),
    this.updatedAt = const Value.absent(),
    this.deletedAt = const Value.absent(),
    this.syncState = const Value.absent(),
    this.payloadJson = const Value.absent(),
    this.rowid = const Value.absent(),
  });
  FinancialTransactionsDriftTableCompanion.insert({
    required String id,
    required String userId,
    this.version = const Value.absent(),
    required String updatedAt,
    this.deletedAt = const Value.absent(),
    this.syncState = const Value.absent(),
    required String payloadJson,
    this.rowid = const Value.absent(),
  })  : id = Value(id),
        userId = Value(userId),
        updatedAt = Value(updatedAt),
        payloadJson = Value(payloadJson);
  static Insertable<FinancialTransactionsDriftTableData> custom({
    Expression<String>? id,
    Expression<String>? userId,
    Expression<int>? version,
    Expression<String>? updatedAt,
    Expression<String>? deletedAt,
    Expression<String>? syncState,
    Expression<String>? payloadJson,
    Expression<int>? rowid,
  }) {
    return RawValuesInsertable({
      if (id != null) 'id': id,
      if (userId != null) 'user_id': userId,
      if (version != null) 'version': version,
      if (updatedAt != null) 'updated_at': updatedAt,
      if (deletedAt != null) 'deleted_at': deletedAt,
      if (syncState != null) 'sync_state': syncState,
      if (payloadJson != null) 'payload_json': payloadJson,
      if (rowid != null) 'rowid': rowid,
    });
  }

  FinancialTransactionsDriftTableCompanion copyWith(
      {Value<String>? id,
      Value<String>? userId,
      Value<int>? version,
      Value<String>? updatedAt,
      Value<String?>? deletedAt,
      Value<String>? syncState,
      Value<String>? payloadJson,
      Value<int>? rowid}) {
    return FinancialTransactionsDriftTableCompanion(
      id: id ?? this.id,
      userId: userId ?? this.userId,
      version: version ?? this.version,
      updatedAt: updatedAt ?? this.updatedAt,
      deletedAt: deletedAt ?? this.deletedAt,
      syncState: syncState ?? this.syncState,
      payloadJson: payloadJson ?? this.payloadJson,
      rowid: rowid ?? this.rowid,
    );
  }

  @override
  Map<String, Expression> toColumns(bool nullToAbsent) {
    final map = <String, Expression>{};
    if (id.present) {
      map['id'] = Variable<String>(id.value);
    }
    if (userId.present) {
      map['user_id'] = Variable<String>(userId.value);
    }
    if (version.present) {
      map['version'] = Variable<int>(version.value);
    }
    if (updatedAt.present) {
      map['updated_at'] = Variable<String>(updatedAt.value);
    }
    if (deletedAt.present) {
      map['deleted_at'] = Variable<String>(deletedAt.value);
    }
    if (syncState.present) {
      map['sync_state'] = Variable<String>(syncState.value);
    }
    if (payloadJson.present) {
      map['payload_json'] = Variable<String>(payloadJson.value);
    }
    if (rowid.present) {
      map['rowid'] = Variable<int>(rowid.value);
    }
    return map;
  }

  @override
  String toString() {
    return (StringBuffer('FinancialTransactionsDriftTableCompanion(')
          ..write('id: $id, ')
          ..write('userId: $userId, ')
          ..write('version: $version, ')
          ..write('updatedAt: $updatedAt, ')
          ..write('deletedAt: $deletedAt, ')
          ..write('syncState: $syncState, ')
          ..write('payloadJson: $payloadJson, ')
          ..write('rowid: $rowid')
          ..write(')'))
        .toString();
  }
}

class $AccountTransfersDriftTableTable extends AccountTransfersDriftTable
    with
        TableInfo<$AccountTransfersDriftTableTable,
            AccountTransfersDriftTableData> {
  @override
  final GeneratedDatabase attachedDatabase;
  final String? _alias;
  $AccountTransfersDriftTableTable(this.attachedDatabase, [this._alias]);
  static const VerificationMeta _idMeta = const VerificationMeta('id');
  @override
  late final GeneratedColumn<String> id = GeneratedColumn<String>(
      'id', aliasedName, false,
      type: DriftSqlType.string, requiredDuringInsert: true);
  static const VerificationMeta _userIdMeta = const VerificationMeta('userId');
  @override
  late final GeneratedColumn<String> userId = GeneratedColumn<String>(
      'user_id', aliasedName, false,
      type: DriftSqlType.string, requiredDuringInsert: true);
  static const VerificationMeta _versionMeta =
      const VerificationMeta('version');
  @override
  late final GeneratedColumn<int> version = GeneratedColumn<int>(
      'version', aliasedName, false,
      type: DriftSqlType.int,
      requiredDuringInsert: false,
      defaultValue: const Constant(0));
  static const VerificationMeta _updatedAtMeta =
      const VerificationMeta('updatedAt');
  @override
  late final GeneratedColumn<String> updatedAt = GeneratedColumn<String>(
      'updated_at', aliasedName, false,
      type: DriftSqlType.string, requiredDuringInsert: true);
  static const VerificationMeta _deletedAtMeta =
      const VerificationMeta('deletedAt');
  @override
  late final GeneratedColumn<String> deletedAt = GeneratedColumn<String>(
      'deleted_at', aliasedName, true,
      type: DriftSqlType.string, requiredDuringInsert: false);
  static const VerificationMeta _syncStateMeta =
      const VerificationMeta('syncState');
  @override
  late final GeneratedColumn<String> syncState = GeneratedColumn<String>(
      'sync_state', aliasedName, false,
      type: DriftSqlType.string,
      requiredDuringInsert: false,
      defaultValue: const Constant('synced'));
  static const VerificationMeta _payloadJsonMeta =
      const VerificationMeta('payloadJson');
  @override
  late final GeneratedColumn<String> payloadJson = GeneratedColumn<String>(
      'payload_json', aliasedName, false,
      type: DriftSqlType.string, requiredDuringInsert: true);
  @override
  List<GeneratedColumn> get $columns =>
      [id, userId, version, updatedAt, deletedAt, syncState, payloadJson];
  @override
  String get aliasedName => _alias ?? actualTableName;
  @override
  String get actualTableName => $name;
  static const String $name = 'account_transfers';
  @override
  VerificationContext validateIntegrity(
      Insertable<AccountTransfersDriftTableData> instance,
      {bool isInserting = false}) {
    final context = VerificationContext();
    final data = instance.toColumns(true);
    if (data.containsKey('id')) {
      context.handle(_idMeta, id.isAcceptableOrUnknown(data['id']!, _idMeta));
    } else if (isInserting) {
      context.missing(_idMeta);
    }
    if (data.containsKey('user_id')) {
      context.handle(_userIdMeta,
          userId.isAcceptableOrUnknown(data['user_id']!, _userIdMeta));
    } else if (isInserting) {
      context.missing(_userIdMeta);
    }
    if (data.containsKey('version')) {
      context.handle(_versionMeta,
          version.isAcceptableOrUnknown(data['version']!, _versionMeta));
    }
    if (data.containsKey('updated_at')) {
      context.handle(_updatedAtMeta,
          updatedAt.isAcceptableOrUnknown(data['updated_at']!, _updatedAtMeta));
    } else if (isInserting) {
      context.missing(_updatedAtMeta);
    }
    if (data.containsKey('deleted_at')) {
      context.handle(_deletedAtMeta,
          deletedAt.isAcceptableOrUnknown(data['deleted_at']!, _deletedAtMeta));
    }
    if (data.containsKey('sync_state')) {
      context.handle(_syncStateMeta,
          syncState.isAcceptableOrUnknown(data['sync_state']!, _syncStateMeta));
    }
    if (data.containsKey('payload_json')) {
      context.handle(
          _payloadJsonMeta,
          payloadJson.isAcceptableOrUnknown(
              data['payload_json']!, _payloadJsonMeta));
    } else if (isInserting) {
      context.missing(_payloadJsonMeta);
    }
    return context;
  }

  @override
  Set<GeneratedColumn> get $primaryKey => {userId, id};
  @override
  AccountTransfersDriftTableData map(Map<String, dynamic> data,
      {String? tablePrefix}) {
    final effectivePrefix = tablePrefix != null ? '$tablePrefix.' : '';
    return AccountTransfersDriftTableData(
      id: attachedDatabase.typeMapping
          .read(DriftSqlType.string, data['${effectivePrefix}id'])!,
      userId: attachedDatabase.typeMapping
          .read(DriftSqlType.string, data['${effectivePrefix}user_id'])!,
      version: attachedDatabase.typeMapping
          .read(DriftSqlType.int, data['${effectivePrefix}version'])!,
      updatedAt: attachedDatabase.typeMapping
          .read(DriftSqlType.string, data['${effectivePrefix}updated_at'])!,
      deletedAt: attachedDatabase.typeMapping
          .read(DriftSqlType.string, data['${effectivePrefix}deleted_at']),
      syncState: attachedDatabase.typeMapping
          .read(DriftSqlType.string, data['${effectivePrefix}sync_state'])!,
      payloadJson: attachedDatabase.typeMapping
          .read(DriftSqlType.string, data['${effectivePrefix}payload_json'])!,
    );
  }

  @override
  $AccountTransfersDriftTableTable createAlias(String alias) {
    return $AccountTransfersDriftTableTable(attachedDatabase, alias);
  }
}

class AccountTransfersDriftTableData extends DataClass
    implements Insertable<AccountTransfersDriftTableData> {
  final String id;
  final String userId;
  final int version;
  final String updatedAt;
  final String? deletedAt;
  final String syncState;
  final String payloadJson;
  const AccountTransfersDriftTableData(
      {required this.id,
      required this.userId,
      required this.version,
      required this.updatedAt,
      this.deletedAt,
      required this.syncState,
      required this.payloadJson});
  @override
  Map<String, Expression> toColumns(bool nullToAbsent) {
    final map = <String, Expression>{};
    map['id'] = Variable<String>(id);
    map['user_id'] = Variable<String>(userId);
    map['version'] = Variable<int>(version);
    map['updated_at'] = Variable<String>(updatedAt);
    if (!nullToAbsent || deletedAt != null) {
      map['deleted_at'] = Variable<String>(deletedAt);
    }
    map['sync_state'] = Variable<String>(syncState);
    map['payload_json'] = Variable<String>(payloadJson);
    return map;
  }

  AccountTransfersDriftTableCompanion toCompanion(bool nullToAbsent) {
    return AccountTransfersDriftTableCompanion(
      id: Value(id),
      userId: Value(userId),
      version: Value(version),
      updatedAt: Value(updatedAt),
      deletedAt: deletedAt == null && nullToAbsent
          ? const Value.absent()
          : Value(deletedAt),
      syncState: Value(syncState),
      payloadJson: Value(payloadJson),
    );
  }

  factory AccountTransfersDriftTableData.fromJson(Map<String, dynamic> json,
      {ValueSerializer? serializer}) {
    serializer ??= driftRuntimeOptions.defaultSerializer;
    return AccountTransfersDriftTableData(
      id: serializer.fromJson<String>(json['id']),
      userId: serializer.fromJson<String>(json['userId']),
      version: serializer.fromJson<int>(json['version']),
      updatedAt: serializer.fromJson<String>(json['updatedAt']),
      deletedAt: serializer.fromJson<String?>(json['deletedAt']),
      syncState: serializer.fromJson<String>(json['syncState']),
      payloadJson: serializer.fromJson<String>(json['payloadJson']),
    );
  }
  @override
  Map<String, dynamic> toJson({ValueSerializer? serializer}) {
    serializer ??= driftRuntimeOptions.defaultSerializer;
    return <String, dynamic>{
      'id': serializer.toJson<String>(id),
      'userId': serializer.toJson<String>(userId),
      'version': serializer.toJson<int>(version),
      'updatedAt': serializer.toJson<String>(updatedAt),
      'deletedAt': serializer.toJson<String?>(deletedAt),
      'syncState': serializer.toJson<String>(syncState),
      'payloadJson': serializer.toJson<String>(payloadJson),
    };
  }

  AccountTransfersDriftTableData copyWith(
          {String? id,
          String? userId,
          int? version,
          String? updatedAt,
          Value<String?> deletedAt = const Value.absent(),
          String? syncState,
          String? payloadJson}) =>
      AccountTransfersDriftTableData(
        id: id ?? this.id,
        userId: userId ?? this.userId,
        version: version ?? this.version,
        updatedAt: updatedAt ?? this.updatedAt,
        deletedAt: deletedAt.present ? deletedAt.value : this.deletedAt,
        syncState: syncState ?? this.syncState,
        payloadJson: payloadJson ?? this.payloadJson,
      );
  AccountTransfersDriftTableData copyWithCompanion(
      AccountTransfersDriftTableCompanion data) {
    return AccountTransfersDriftTableData(
      id: data.id.present ? data.id.value : this.id,
      userId: data.userId.present ? data.userId.value : this.userId,
      version: data.version.present ? data.version.value : this.version,
      updatedAt: data.updatedAt.present ? data.updatedAt.value : this.updatedAt,
      deletedAt: data.deletedAt.present ? data.deletedAt.value : this.deletedAt,
      syncState: data.syncState.present ? data.syncState.value : this.syncState,
      payloadJson:
          data.payloadJson.present ? data.payloadJson.value : this.payloadJson,
    );
  }

  @override
  String toString() {
    return (StringBuffer('AccountTransfersDriftTableData(')
          ..write('id: $id, ')
          ..write('userId: $userId, ')
          ..write('version: $version, ')
          ..write('updatedAt: $updatedAt, ')
          ..write('deletedAt: $deletedAt, ')
          ..write('syncState: $syncState, ')
          ..write('payloadJson: $payloadJson')
          ..write(')'))
        .toString();
  }

  @override
  int get hashCode => Object.hash(
      id, userId, version, updatedAt, deletedAt, syncState, payloadJson);
  @override
  bool operator ==(Object other) =>
      identical(this, other) ||
      (other is AccountTransfersDriftTableData &&
          other.id == this.id &&
          other.userId == this.userId &&
          other.version == this.version &&
          other.updatedAt == this.updatedAt &&
          other.deletedAt == this.deletedAt &&
          other.syncState == this.syncState &&
          other.payloadJson == this.payloadJson);
}

class AccountTransfersDriftTableCompanion
    extends UpdateCompanion<AccountTransfersDriftTableData> {
  final Value<String> id;
  final Value<String> userId;
  final Value<int> version;
  final Value<String> updatedAt;
  final Value<String?> deletedAt;
  final Value<String> syncState;
  final Value<String> payloadJson;
  final Value<int> rowid;
  const AccountTransfersDriftTableCompanion({
    this.id = const Value.absent(),
    this.userId = const Value.absent(),
    this.version = const Value.absent(),
    this.updatedAt = const Value.absent(),
    this.deletedAt = const Value.absent(),
    this.syncState = const Value.absent(),
    this.payloadJson = const Value.absent(),
    this.rowid = const Value.absent(),
  });
  AccountTransfersDriftTableCompanion.insert({
    required String id,
    required String userId,
    this.version = const Value.absent(),
    required String updatedAt,
    this.deletedAt = const Value.absent(),
    this.syncState = const Value.absent(),
    required String payloadJson,
    this.rowid = const Value.absent(),
  })  : id = Value(id),
        userId = Value(userId),
        updatedAt = Value(updatedAt),
        payloadJson = Value(payloadJson);
  static Insertable<AccountTransfersDriftTableData> custom({
    Expression<String>? id,
    Expression<String>? userId,
    Expression<int>? version,
    Expression<String>? updatedAt,
    Expression<String>? deletedAt,
    Expression<String>? syncState,
    Expression<String>? payloadJson,
    Expression<int>? rowid,
  }) {
    return RawValuesInsertable({
      if (id != null) 'id': id,
      if (userId != null) 'user_id': userId,
      if (version != null) 'version': version,
      if (updatedAt != null) 'updated_at': updatedAt,
      if (deletedAt != null) 'deleted_at': deletedAt,
      if (syncState != null) 'sync_state': syncState,
      if (payloadJson != null) 'payload_json': payloadJson,
      if (rowid != null) 'rowid': rowid,
    });
  }

  AccountTransfersDriftTableCompanion copyWith(
      {Value<String>? id,
      Value<String>? userId,
      Value<int>? version,
      Value<String>? updatedAt,
      Value<String?>? deletedAt,
      Value<String>? syncState,
      Value<String>? payloadJson,
      Value<int>? rowid}) {
    return AccountTransfersDriftTableCompanion(
      id: id ?? this.id,
      userId: userId ?? this.userId,
      version: version ?? this.version,
      updatedAt: updatedAt ?? this.updatedAt,
      deletedAt: deletedAt ?? this.deletedAt,
      syncState: syncState ?? this.syncState,
      payloadJson: payloadJson ?? this.payloadJson,
      rowid: rowid ?? this.rowid,
    );
  }

  @override
  Map<String, Expression> toColumns(bool nullToAbsent) {
    final map = <String, Expression>{};
    if (id.present) {
      map['id'] = Variable<String>(id.value);
    }
    if (userId.present) {
      map['user_id'] = Variable<String>(userId.value);
    }
    if (version.present) {
      map['version'] = Variable<int>(version.value);
    }
    if (updatedAt.present) {
      map['updated_at'] = Variable<String>(updatedAt.value);
    }
    if (deletedAt.present) {
      map['deleted_at'] = Variable<String>(deletedAt.value);
    }
    if (syncState.present) {
      map['sync_state'] = Variable<String>(syncState.value);
    }
    if (payloadJson.present) {
      map['payload_json'] = Variable<String>(payloadJson.value);
    }
    if (rowid.present) {
      map['rowid'] = Variable<int>(rowid.value);
    }
    return map;
  }

  @override
  String toString() {
    return (StringBuffer('AccountTransfersDriftTableCompanion(')
          ..write('id: $id, ')
          ..write('userId: $userId, ')
          ..write('version: $version, ')
          ..write('updatedAt: $updatedAt, ')
          ..write('deletedAt: $deletedAt, ')
          ..write('syncState: $syncState, ')
          ..write('payloadJson: $payloadJson, ')
          ..write('rowid: $rowid')
          ..write(')'))
        .toString();
  }
}

class $BudgetsDriftTableTable extends BudgetsDriftTable
    with TableInfo<$BudgetsDriftTableTable, BudgetsDriftTableData> {
  @override
  final GeneratedDatabase attachedDatabase;
  final String? _alias;
  $BudgetsDriftTableTable(this.attachedDatabase, [this._alias]);
  static const VerificationMeta _idMeta = const VerificationMeta('id');
  @override
  late final GeneratedColumn<String> id = GeneratedColumn<String>(
      'id', aliasedName, false,
      type: DriftSqlType.string, requiredDuringInsert: true);
  static const VerificationMeta _userIdMeta = const VerificationMeta('userId');
  @override
  late final GeneratedColumn<String> userId = GeneratedColumn<String>(
      'user_id', aliasedName, false,
      type: DriftSqlType.string, requiredDuringInsert: true);
  static const VerificationMeta _versionMeta =
      const VerificationMeta('version');
  @override
  late final GeneratedColumn<int> version = GeneratedColumn<int>(
      'version', aliasedName, false,
      type: DriftSqlType.int,
      requiredDuringInsert: false,
      defaultValue: const Constant(0));
  static const VerificationMeta _updatedAtMeta =
      const VerificationMeta('updatedAt');
  @override
  late final GeneratedColumn<String> updatedAt = GeneratedColumn<String>(
      'updated_at', aliasedName, false,
      type: DriftSqlType.string, requiredDuringInsert: true);
  static const VerificationMeta _deletedAtMeta =
      const VerificationMeta('deletedAt');
  @override
  late final GeneratedColumn<String> deletedAt = GeneratedColumn<String>(
      'deleted_at', aliasedName, true,
      type: DriftSqlType.string, requiredDuringInsert: false);
  static const VerificationMeta _syncStateMeta =
      const VerificationMeta('syncState');
  @override
  late final GeneratedColumn<String> syncState = GeneratedColumn<String>(
      'sync_state', aliasedName, false,
      type: DriftSqlType.string,
      requiredDuringInsert: false,
      defaultValue: const Constant('synced'));
  static const VerificationMeta _payloadJsonMeta =
      const VerificationMeta('payloadJson');
  @override
  late final GeneratedColumn<String> payloadJson = GeneratedColumn<String>(
      'payload_json', aliasedName, false,
      type: DriftSqlType.string, requiredDuringInsert: true);
  @override
  List<GeneratedColumn> get $columns =>
      [id, userId, version, updatedAt, deletedAt, syncState, payloadJson];
  @override
  String get aliasedName => _alias ?? actualTableName;
  @override
  String get actualTableName => $name;
  static const String $name = 'budgets';
  @override
  VerificationContext validateIntegrity(
      Insertable<BudgetsDriftTableData> instance,
      {bool isInserting = false}) {
    final context = VerificationContext();
    final data = instance.toColumns(true);
    if (data.containsKey('id')) {
      context.handle(_idMeta, id.isAcceptableOrUnknown(data['id']!, _idMeta));
    } else if (isInserting) {
      context.missing(_idMeta);
    }
    if (data.containsKey('user_id')) {
      context.handle(_userIdMeta,
          userId.isAcceptableOrUnknown(data['user_id']!, _userIdMeta));
    } else if (isInserting) {
      context.missing(_userIdMeta);
    }
    if (data.containsKey('version')) {
      context.handle(_versionMeta,
          version.isAcceptableOrUnknown(data['version']!, _versionMeta));
    }
    if (data.containsKey('updated_at')) {
      context.handle(_updatedAtMeta,
          updatedAt.isAcceptableOrUnknown(data['updated_at']!, _updatedAtMeta));
    } else if (isInserting) {
      context.missing(_updatedAtMeta);
    }
    if (data.containsKey('deleted_at')) {
      context.handle(_deletedAtMeta,
          deletedAt.isAcceptableOrUnknown(data['deleted_at']!, _deletedAtMeta));
    }
    if (data.containsKey('sync_state')) {
      context.handle(_syncStateMeta,
          syncState.isAcceptableOrUnknown(data['sync_state']!, _syncStateMeta));
    }
    if (data.containsKey('payload_json')) {
      context.handle(
          _payloadJsonMeta,
          payloadJson.isAcceptableOrUnknown(
              data['payload_json']!, _payloadJsonMeta));
    } else if (isInserting) {
      context.missing(_payloadJsonMeta);
    }
    return context;
  }

  @override
  Set<GeneratedColumn> get $primaryKey => {userId, id};
  @override
  BudgetsDriftTableData map(Map<String, dynamic> data, {String? tablePrefix}) {
    final effectivePrefix = tablePrefix != null ? '$tablePrefix.' : '';
    return BudgetsDriftTableData(
      id: attachedDatabase.typeMapping
          .read(DriftSqlType.string, data['${effectivePrefix}id'])!,
      userId: attachedDatabase.typeMapping
          .read(DriftSqlType.string, data['${effectivePrefix}user_id'])!,
      version: attachedDatabase.typeMapping
          .read(DriftSqlType.int, data['${effectivePrefix}version'])!,
      updatedAt: attachedDatabase.typeMapping
          .read(DriftSqlType.string, data['${effectivePrefix}updated_at'])!,
      deletedAt: attachedDatabase.typeMapping
          .read(DriftSqlType.string, data['${effectivePrefix}deleted_at']),
      syncState: attachedDatabase.typeMapping
          .read(DriftSqlType.string, data['${effectivePrefix}sync_state'])!,
      payloadJson: attachedDatabase.typeMapping
          .read(DriftSqlType.string, data['${effectivePrefix}payload_json'])!,
    );
  }

  @override
  $BudgetsDriftTableTable createAlias(String alias) {
    return $BudgetsDriftTableTable(attachedDatabase, alias);
  }
}

class BudgetsDriftTableData extends DataClass
    implements Insertable<BudgetsDriftTableData> {
  final String id;
  final String userId;
  final int version;
  final String updatedAt;
  final String? deletedAt;
  final String syncState;
  final String payloadJson;
  const BudgetsDriftTableData(
      {required this.id,
      required this.userId,
      required this.version,
      required this.updatedAt,
      this.deletedAt,
      required this.syncState,
      required this.payloadJson});
  @override
  Map<String, Expression> toColumns(bool nullToAbsent) {
    final map = <String, Expression>{};
    map['id'] = Variable<String>(id);
    map['user_id'] = Variable<String>(userId);
    map['version'] = Variable<int>(version);
    map['updated_at'] = Variable<String>(updatedAt);
    if (!nullToAbsent || deletedAt != null) {
      map['deleted_at'] = Variable<String>(deletedAt);
    }
    map['sync_state'] = Variable<String>(syncState);
    map['payload_json'] = Variable<String>(payloadJson);
    return map;
  }

  BudgetsDriftTableCompanion toCompanion(bool nullToAbsent) {
    return BudgetsDriftTableCompanion(
      id: Value(id),
      userId: Value(userId),
      version: Value(version),
      updatedAt: Value(updatedAt),
      deletedAt: deletedAt == null && nullToAbsent
          ? const Value.absent()
          : Value(deletedAt),
      syncState: Value(syncState),
      payloadJson: Value(payloadJson),
    );
  }

  factory BudgetsDriftTableData.fromJson(Map<String, dynamic> json,
      {ValueSerializer? serializer}) {
    serializer ??= driftRuntimeOptions.defaultSerializer;
    return BudgetsDriftTableData(
      id: serializer.fromJson<String>(json['id']),
      userId: serializer.fromJson<String>(json['userId']),
      version: serializer.fromJson<int>(json['version']),
      updatedAt: serializer.fromJson<String>(json['updatedAt']),
      deletedAt: serializer.fromJson<String?>(json['deletedAt']),
      syncState: serializer.fromJson<String>(json['syncState']),
      payloadJson: serializer.fromJson<String>(json['payloadJson']),
    );
  }
  @override
  Map<String, dynamic> toJson({ValueSerializer? serializer}) {
    serializer ??= driftRuntimeOptions.defaultSerializer;
    return <String, dynamic>{
      'id': serializer.toJson<String>(id),
      'userId': serializer.toJson<String>(userId),
      'version': serializer.toJson<int>(version),
      'updatedAt': serializer.toJson<String>(updatedAt),
      'deletedAt': serializer.toJson<String?>(deletedAt),
      'syncState': serializer.toJson<String>(syncState),
      'payloadJson': serializer.toJson<String>(payloadJson),
    };
  }

  BudgetsDriftTableData copyWith(
          {String? id,
          String? userId,
          int? version,
          String? updatedAt,
          Value<String?> deletedAt = const Value.absent(),
          String? syncState,
          String? payloadJson}) =>
      BudgetsDriftTableData(
        id: id ?? this.id,
        userId: userId ?? this.userId,
        version: version ?? this.version,
        updatedAt: updatedAt ?? this.updatedAt,
        deletedAt: deletedAt.present ? deletedAt.value : this.deletedAt,
        syncState: syncState ?? this.syncState,
        payloadJson: payloadJson ?? this.payloadJson,
      );
  BudgetsDriftTableData copyWithCompanion(BudgetsDriftTableCompanion data) {
    return BudgetsDriftTableData(
      id: data.id.present ? data.id.value : this.id,
      userId: data.userId.present ? data.userId.value : this.userId,
      version: data.version.present ? data.version.value : this.version,
      updatedAt: data.updatedAt.present ? data.updatedAt.value : this.updatedAt,
      deletedAt: data.deletedAt.present ? data.deletedAt.value : this.deletedAt,
      syncState: data.syncState.present ? data.syncState.value : this.syncState,
      payloadJson:
          data.payloadJson.present ? data.payloadJson.value : this.payloadJson,
    );
  }

  @override
  String toString() {
    return (StringBuffer('BudgetsDriftTableData(')
          ..write('id: $id, ')
          ..write('userId: $userId, ')
          ..write('version: $version, ')
          ..write('updatedAt: $updatedAt, ')
          ..write('deletedAt: $deletedAt, ')
          ..write('syncState: $syncState, ')
          ..write('payloadJson: $payloadJson')
          ..write(')'))
        .toString();
  }

  @override
  int get hashCode => Object.hash(
      id, userId, version, updatedAt, deletedAt, syncState, payloadJson);
  @override
  bool operator ==(Object other) =>
      identical(this, other) ||
      (other is BudgetsDriftTableData &&
          other.id == this.id &&
          other.userId == this.userId &&
          other.version == this.version &&
          other.updatedAt == this.updatedAt &&
          other.deletedAt == this.deletedAt &&
          other.syncState == this.syncState &&
          other.payloadJson == this.payloadJson);
}

class BudgetsDriftTableCompanion
    extends UpdateCompanion<BudgetsDriftTableData> {
  final Value<String> id;
  final Value<String> userId;
  final Value<int> version;
  final Value<String> updatedAt;
  final Value<String?> deletedAt;
  final Value<String> syncState;
  final Value<String> payloadJson;
  final Value<int> rowid;
  const BudgetsDriftTableCompanion({
    this.id = const Value.absent(),
    this.userId = const Value.absent(),
    this.version = const Value.absent(),
    this.updatedAt = const Value.absent(),
    this.deletedAt = const Value.absent(),
    this.syncState = const Value.absent(),
    this.payloadJson = const Value.absent(),
    this.rowid = const Value.absent(),
  });
  BudgetsDriftTableCompanion.insert({
    required String id,
    required String userId,
    this.version = const Value.absent(),
    required String updatedAt,
    this.deletedAt = const Value.absent(),
    this.syncState = const Value.absent(),
    required String payloadJson,
    this.rowid = const Value.absent(),
  })  : id = Value(id),
        userId = Value(userId),
        updatedAt = Value(updatedAt),
        payloadJson = Value(payloadJson);
  static Insertable<BudgetsDriftTableData> custom({
    Expression<String>? id,
    Expression<String>? userId,
    Expression<int>? version,
    Expression<String>? updatedAt,
    Expression<String>? deletedAt,
    Expression<String>? syncState,
    Expression<String>? payloadJson,
    Expression<int>? rowid,
  }) {
    return RawValuesInsertable({
      if (id != null) 'id': id,
      if (userId != null) 'user_id': userId,
      if (version != null) 'version': version,
      if (updatedAt != null) 'updated_at': updatedAt,
      if (deletedAt != null) 'deleted_at': deletedAt,
      if (syncState != null) 'sync_state': syncState,
      if (payloadJson != null) 'payload_json': payloadJson,
      if (rowid != null) 'rowid': rowid,
    });
  }

  BudgetsDriftTableCompanion copyWith(
      {Value<String>? id,
      Value<String>? userId,
      Value<int>? version,
      Value<String>? updatedAt,
      Value<String?>? deletedAt,
      Value<String>? syncState,
      Value<String>? payloadJson,
      Value<int>? rowid}) {
    return BudgetsDriftTableCompanion(
      id: id ?? this.id,
      userId: userId ?? this.userId,
      version: version ?? this.version,
      updatedAt: updatedAt ?? this.updatedAt,
      deletedAt: deletedAt ?? this.deletedAt,
      syncState: syncState ?? this.syncState,
      payloadJson: payloadJson ?? this.payloadJson,
      rowid: rowid ?? this.rowid,
    );
  }

  @override
  Map<String, Expression> toColumns(bool nullToAbsent) {
    final map = <String, Expression>{};
    if (id.present) {
      map['id'] = Variable<String>(id.value);
    }
    if (userId.present) {
      map['user_id'] = Variable<String>(userId.value);
    }
    if (version.present) {
      map['version'] = Variable<int>(version.value);
    }
    if (updatedAt.present) {
      map['updated_at'] = Variable<String>(updatedAt.value);
    }
    if (deletedAt.present) {
      map['deleted_at'] = Variable<String>(deletedAt.value);
    }
    if (syncState.present) {
      map['sync_state'] = Variable<String>(syncState.value);
    }
    if (payloadJson.present) {
      map['payload_json'] = Variable<String>(payloadJson.value);
    }
    if (rowid.present) {
      map['rowid'] = Variable<int>(rowid.value);
    }
    return map;
  }

  @override
  String toString() {
    return (StringBuffer('BudgetsDriftTableCompanion(')
          ..write('id: $id, ')
          ..write('userId: $userId, ')
          ..write('version: $version, ')
          ..write('updatedAt: $updatedAt, ')
          ..write('deletedAt: $deletedAt, ')
          ..write('syncState: $syncState, ')
          ..write('payloadJson: $payloadJson, ')
          ..write('rowid: $rowid')
          ..write(')'))
        .toString();
  }
}

class $RecurringTransactionsDriftTableTable
    extends RecurringTransactionsDriftTable
    with
        TableInfo<$RecurringTransactionsDriftTableTable,
            RecurringTransactionsDriftTableData> {
  @override
  final GeneratedDatabase attachedDatabase;
  final String? _alias;
  $RecurringTransactionsDriftTableTable(this.attachedDatabase, [this._alias]);
  static const VerificationMeta _idMeta = const VerificationMeta('id');
  @override
  late final GeneratedColumn<String> id = GeneratedColumn<String>(
      'id', aliasedName, false,
      type: DriftSqlType.string, requiredDuringInsert: true);
  static const VerificationMeta _userIdMeta = const VerificationMeta('userId');
  @override
  late final GeneratedColumn<String> userId = GeneratedColumn<String>(
      'user_id', aliasedName, false,
      type: DriftSqlType.string, requiredDuringInsert: true);
  static const VerificationMeta _versionMeta =
      const VerificationMeta('version');
  @override
  late final GeneratedColumn<int> version = GeneratedColumn<int>(
      'version', aliasedName, false,
      type: DriftSqlType.int,
      requiredDuringInsert: false,
      defaultValue: const Constant(0));
  static const VerificationMeta _updatedAtMeta =
      const VerificationMeta('updatedAt');
  @override
  late final GeneratedColumn<String> updatedAt = GeneratedColumn<String>(
      'updated_at', aliasedName, false,
      type: DriftSqlType.string, requiredDuringInsert: true);
  static const VerificationMeta _deletedAtMeta =
      const VerificationMeta('deletedAt');
  @override
  late final GeneratedColumn<String> deletedAt = GeneratedColumn<String>(
      'deleted_at', aliasedName, true,
      type: DriftSqlType.string, requiredDuringInsert: false);
  static const VerificationMeta _syncStateMeta =
      const VerificationMeta('syncState');
  @override
  late final GeneratedColumn<String> syncState = GeneratedColumn<String>(
      'sync_state', aliasedName, false,
      type: DriftSqlType.string,
      requiredDuringInsert: false,
      defaultValue: const Constant('synced'));
  static const VerificationMeta _payloadJsonMeta =
      const VerificationMeta('payloadJson');
  @override
  late final GeneratedColumn<String> payloadJson = GeneratedColumn<String>(
      'payload_json', aliasedName, false,
      type: DriftSqlType.string, requiredDuringInsert: true);
  @override
  List<GeneratedColumn> get $columns =>
      [id, userId, version, updatedAt, deletedAt, syncState, payloadJson];
  @override
  String get aliasedName => _alias ?? actualTableName;
  @override
  String get actualTableName => $name;
  static const String $name = 'recurring_transactions';
  @override
  VerificationContext validateIntegrity(
      Insertable<RecurringTransactionsDriftTableData> instance,
      {bool isInserting = false}) {
    final context = VerificationContext();
    final data = instance.toColumns(true);
    if (data.containsKey('id')) {
      context.handle(_idMeta, id.isAcceptableOrUnknown(data['id']!, _idMeta));
    } else if (isInserting) {
      context.missing(_idMeta);
    }
    if (data.containsKey('user_id')) {
      context.handle(_userIdMeta,
          userId.isAcceptableOrUnknown(data['user_id']!, _userIdMeta));
    } else if (isInserting) {
      context.missing(_userIdMeta);
    }
    if (data.containsKey('version')) {
      context.handle(_versionMeta,
          version.isAcceptableOrUnknown(data['version']!, _versionMeta));
    }
    if (data.containsKey('updated_at')) {
      context.handle(_updatedAtMeta,
          updatedAt.isAcceptableOrUnknown(data['updated_at']!, _updatedAtMeta));
    } else if (isInserting) {
      context.missing(_updatedAtMeta);
    }
    if (data.containsKey('deleted_at')) {
      context.handle(_deletedAtMeta,
          deletedAt.isAcceptableOrUnknown(data['deleted_at']!, _deletedAtMeta));
    }
    if (data.containsKey('sync_state')) {
      context.handle(_syncStateMeta,
          syncState.isAcceptableOrUnknown(data['sync_state']!, _syncStateMeta));
    }
    if (data.containsKey('payload_json')) {
      context.handle(
          _payloadJsonMeta,
          payloadJson.isAcceptableOrUnknown(
              data['payload_json']!, _payloadJsonMeta));
    } else if (isInserting) {
      context.missing(_payloadJsonMeta);
    }
    return context;
  }

  @override
  Set<GeneratedColumn> get $primaryKey => {userId, id};
  @override
  RecurringTransactionsDriftTableData map(Map<String, dynamic> data,
      {String? tablePrefix}) {
    final effectivePrefix = tablePrefix != null ? '$tablePrefix.' : '';
    return RecurringTransactionsDriftTableData(
      id: attachedDatabase.typeMapping
          .read(DriftSqlType.string, data['${effectivePrefix}id'])!,
      userId: attachedDatabase.typeMapping
          .read(DriftSqlType.string, data['${effectivePrefix}user_id'])!,
      version: attachedDatabase.typeMapping
          .read(DriftSqlType.int, data['${effectivePrefix}version'])!,
      updatedAt: attachedDatabase.typeMapping
          .read(DriftSqlType.string, data['${effectivePrefix}updated_at'])!,
      deletedAt: attachedDatabase.typeMapping
          .read(DriftSqlType.string, data['${effectivePrefix}deleted_at']),
      syncState: attachedDatabase.typeMapping
          .read(DriftSqlType.string, data['${effectivePrefix}sync_state'])!,
      payloadJson: attachedDatabase.typeMapping
          .read(DriftSqlType.string, data['${effectivePrefix}payload_json'])!,
    );
  }

  @override
  $RecurringTransactionsDriftTableTable createAlias(String alias) {
    return $RecurringTransactionsDriftTableTable(attachedDatabase, alias);
  }
}

class RecurringTransactionsDriftTableData extends DataClass
    implements Insertable<RecurringTransactionsDriftTableData> {
  final String id;
  final String userId;
  final int version;
  final String updatedAt;
  final String? deletedAt;
  final String syncState;
  final String payloadJson;
  const RecurringTransactionsDriftTableData(
      {required this.id,
      required this.userId,
      required this.version,
      required this.updatedAt,
      this.deletedAt,
      required this.syncState,
      required this.payloadJson});
  @override
  Map<String, Expression> toColumns(bool nullToAbsent) {
    final map = <String, Expression>{};
    map['id'] = Variable<String>(id);
    map['user_id'] = Variable<String>(userId);
    map['version'] = Variable<int>(version);
    map['updated_at'] = Variable<String>(updatedAt);
    if (!nullToAbsent || deletedAt != null) {
      map['deleted_at'] = Variable<String>(deletedAt);
    }
    map['sync_state'] = Variable<String>(syncState);
    map['payload_json'] = Variable<String>(payloadJson);
    return map;
  }

  RecurringTransactionsDriftTableCompanion toCompanion(bool nullToAbsent) {
    return RecurringTransactionsDriftTableCompanion(
      id: Value(id),
      userId: Value(userId),
      version: Value(version),
      updatedAt: Value(updatedAt),
      deletedAt: deletedAt == null && nullToAbsent
          ? const Value.absent()
          : Value(deletedAt),
      syncState: Value(syncState),
      payloadJson: Value(payloadJson),
    );
  }

  factory RecurringTransactionsDriftTableData.fromJson(
      Map<String, dynamic> json,
      {ValueSerializer? serializer}) {
    serializer ??= driftRuntimeOptions.defaultSerializer;
    return RecurringTransactionsDriftTableData(
      id: serializer.fromJson<String>(json['id']),
      userId: serializer.fromJson<String>(json['userId']),
      version: serializer.fromJson<int>(json['version']),
      updatedAt: serializer.fromJson<String>(json['updatedAt']),
      deletedAt: serializer.fromJson<String?>(json['deletedAt']),
      syncState: serializer.fromJson<String>(json['syncState']),
      payloadJson: serializer.fromJson<String>(json['payloadJson']),
    );
  }
  @override
  Map<String, dynamic> toJson({ValueSerializer? serializer}) {
    serializer ??= driftRuntimeOptions.defaultSerializer;
    return <String, dynamic>{
      'id': serializer.toJson<String>(id),
      'userId': serializer.toJson<String>(userId),
      'version': serializer.toJson<int>(version),
      'updatedAt': serializer.toJson<String>(updatedAt),
      'deletedAt': serializer.toJson<String?>(deletedAt),
      'syncState': serializer.toJson<String>(syncState),
      'payloadJson': serializer.toJson<String>(payloadJson),
    };
  }

  RecurringTransactionsDriftTableData copyWith(
          {String? id,
          String? userId,
          int? version,
          String? updatedAt,
          Value<String?> deletedAt = const Value.absent(),
          String? syncState,
          String? payloadJson}) =>
      RecurringTransactionsDriftTableData(
        id: id ?? this.id,
        userId: userId ?? this.userId,
        version: version ?? this.version,
        updatedAt: updatedAt ?? this.updatedAt,
        deletedAt: deletedAt.present ? deletedAt.value : this.deletedAt,
        syncState: syncState ?? this.syncState,
        payloadJson: payloadJson ?? this.payloadJson,
      );
  RecurringTransactionsDriftTableData copyWithCompanion(
      RecurringTransactionsDriftTableCompanion data) {
    return RecurringTransactionsDriftTableData(
      id: data.id.present ? data.id.value : this.id,
      userId: data.userId.present ? data.userId.value : this.userId,
      version: data.version.present ? data.version.value : this.version,
      updatedAt: data.updatedAt.present ? data.updatedAt.value : this.updatedAt,
      deletedAt: data.deletedAt.present ? data.deletedAt.value : this.deletedAt,
      syncState: data.syncState.present ? data.syncState.value : this.syncState,
      payloadJson:
          data.payloadJson.present ? data.payloadJson.value : this.payloadJson,
    );
  }

  @override
  String toString() {
    return (StringBuffer('RecurringTransactionsDriftTableData(')
          ..write('id: $id, ')
          ..write('userId: $userId, ')
          ..write('version: $version, ')
          ..write('updatedAt: $updatedAt, ')
          ..write('deletedAt: $deletedAt, ')
          ..write('syncState: $syncState, ')
          ..write('payloadJson: $payloadJson')
          ..write(')'))
        .toString();
  }

  @override
  int get hashCode => Object.hash(
      id, userId, version, updatedAt, deletedAt, syncState, payloadJson);
  @override
  bool operator ==(Object other) =>
      identical(this, other) ||
      (other is RecurringTransactionsDriftTableData &&
          other.id == this.id &&
          other.userId == this.userId &&
          other.version == this.version &&
          other.updatedAt == this.updatedAt &&
          other.deletedAt == this.deletedAt &&
          other.syncState == this.syncState &&
          other.payloadJson == this.payloadJson);
}

class RecurringTransactionsDriftTableCompanion
    extends UpdateCompanion<RecurringTransactionsDriftTableData> {
  final Value<String> id;
  final Value<String> userId;
  final Value<int> version;
  final Value<String> updatedAt;
  final Value<String?> deletedAt;
  final Value<String> syncState;
  final Value<String> payloadJson;
  final Value<int> rowid;
  const RecurringTransactionsDriftTableCompanion({
    this.id = const Value.absent(),
    this.userId = const Value.absent(),
    this.version = const Value.absent(),
    this.updatedAt = const Value.absent(),
    this.deletedAt = const Value.absent(),
    this.syncState = const Value.absent(),
    this.payloadJson = const Value.absent(),
    this.rowid = const Value.absent(),
  });
  RecurringTransactionsDriftTableCompanion.insert({
    required String id,
    required String userId,
    this.version = const Value.absent(),
    required String updatedAt,
    this.deletedAt = const Value.absent(),
    this.syncState = const Value.absent(),
    required String payloadJson,
    this.rowid = const Value.absent(),
  })  : id = Value(id),
        userId = Value(userId),
        updatedAt = Value(updatedAt),
        payloadJson = Value(payloadJson);
  static Insertable<RecurringTransactionsDriftTableData> custom({
    Expression<String>? id,
    Expression<String>? userId,
    Expression<int>? version,
    Expression<String>? updatedAt,
    Expression<String>? deletedAt,
    Expression<String>? syncState,
    Expression<String>? payloadJson,
    Expression<int>? rowid,
  }) {
    return RawValuesInsertable({
      if (id != null) 'id': id,
      if (userId != null) 'user_id': userId,
      if (version != null) 'version': version,
      if (updatedAt != null) 'updated_at': updatedAt,
      if (deletedAt != null) 'deleted_at': deletedAt,
      if (syncState != null) 'sync_state': syncState,
      if (payloadJson != null) 'payload_json': payloadJson,
      if (rowid != null) 'rowid': rowid,
    });
  }

  RecurringTransactionsDriftTableCompanion copyWith(
      {Value<String>? id,
      Value<String>? userId,
      Value<int>? version,
      Value<String>? updatedAt,
      Value<String?>? deletedAt,
      Value<String>? syncState,
      Value<String>? payloadJson,
      Value<int>? rowid}) {
    return RecurringTransactionsDriftTableCompanion(
      id: id ?? this.id,
      userId: userId ?? this.userId,
      version: version ?? this.version,
      updatedAt: updatedAt ?? this.updatedAt,
      deletedAt: deletedAt ?? this.deletedAt,
      syncState: syncState ?? this.syncState,
      payloadJson: payloadJson ?? this.payloadJson,
      rowid: rowid ?? this.rowid,
    );
  }

  @override
  Map<String, Expression> toColumns(bool nullToAbsent) {
    final map = <String, Expression>{};
    if (id.present) {
      map['id'] = Variable<String>(id.value);
    }
    if (userId.present) {
      map['user_id'] = Variable<String>(userId.value);
    }
    if (version.present) {
      map['version'] = Variable<int>(version.value);
    }
    if (updatedAt.present) {
      map['updated_at'] = Variable<String>(updatedAt.value);
    }
    if (deletedAt.present) {
      map['deleted_at'] = Variable<String>(deletedAt.value);
    }
    if (syncState.present) {
      map['sync_state'] = Variable<String>(syncState.value);
    }
    if (payloadJson.present) {
      map['payload_json'] = Variable<String>(payloadJson.value);
    }
    if (rowid.present) {
      map['rowid'] = Variable<int>(rowid.value);
    }
    return map;
  }

  @override
  String toString() {
    return (StringBuffer('RecurringTransactionsDriftTableCompanion(')
          ..write('id: $id, ')
          ..write('userId: $userId, ')
          ..write('version: $version, ')
          ..write('updatedAt: $updatedAt, ')
          ..write('deletedAt: $deletedAt, ')
          ..write('syncState: $syncState, ')
          ..write('payloadJson: $payloadJson, ')
          ..write('rowid: $rowid')
          ..write(')'))
        .toString();
  }
}

class $SavingGoalsDriftTableTable extends SavingGoalsDriftTable
    with TableInfo<$SavingGoalsDriftTableTable, SavingGoalsDriftTableData> {
  @override
  final GeneratedDatabase attachedDatabase;
  final String? _alias;
  $SavingGoalsDriftTableTable(this.attachedDatabase, [this._alias]);
  static const VerificationMeta _idMeta = const VerificationMeta('id');
  @override
  late final GeneratedColumn<String> id = GeneratedColumn<String>(
      'id', aliasedName, false,
      type: DriftSqlType.string, requiredDuringInsert: true);
  static const VerificationMeta _userIdMeta = const VerificationMeta('userId');
  @override
  late final GeneratedColumn<String> userId = GeneratedColumn<String>(
      'user_id', aliasedName, false,
      type: DriftSqlType.string, requiredDuringInsert: true);
  static const VerificationMeta _versionMeta =
      const VerificationMeta('version');
  @override
  late final GeneratedColumn<int> version = GeneratedColumn<int>(
      'version', aliasedName, false,
      type: DriftSqlType.int,
      requiredDuringInsert: false,
      defaultValue: const Constant(0));
  static const VerificationMeta _updatedAtMeta =
      const VerificationMeta('updatedAt');
  @override
  late final GeneratedColumn<String> updatedAt = GeneratedColumn<String>(
      'updated_at', aliasedName, false,
      type: DriftSqlType.string, requiredDuringInsert: true);
  static const VerificationMeta _deletedAtMeta =
      const VerificationMeta('deletedAt');
  @override
  late final GeneratedColumn<String> deletedAt = GeneratedColumn<String>(
      'deleted_at', aliasedName, true,
      type: DriftSqlType.string, requiredDuringInsert: false);
  static const VerificationMeta _syncStateMeta =
      const VerificationMeta('syncState');
  @override
  late final GeneratedColumn<String> syncState = GeneratedColumn<String>(
      'sync_state', aliasedName, false,
      type: DriftSqlType.string,
      requiredDuringInsert: false,
      defaultValue: const Constant('synced'));
  static const VerificationMeta _payloadJsonMeta =
      const VerificationMeta('payloadJson');
  @override
  late final GeneratedColumn<String> payloadJson = GeneratedColumn<String>(
      'payload_json', aliasedName, false,
      type: DriftSqlType.string, requiredDuringInsert: true);
  @override
  List<GeneratedColumn> get $columns =>
      [id, userId, version, updatedAt, deletedAt, syncState, payloadJson];
  @override
  String get aliasedName => _alias ?? actualTableName;
  @override
  String get actualTableName => $name;
  static const String $name = 'saving_goals';
  @override
  VerificationContext validateIntegrity(
      Insertable<SavingGoalsDriftTableData> instance,
      {bool isInserting = false}) {
    final context = VerificationContext();
    final data = instance.toColumns(true);
    if (data.containsKey('id')) {
      context.handle(_idMeta, id.isAcceptableOrUnknown(data['id']!, _idMeta));
    } else if (isInserting) {
      context.missing(_idMeta);
    }
    if (data.containsKey('user_id')) {
      context.handle(_userIdMeta,
          userId.isAcceptableOrUnknown(data['user_id']!, _userIdMeta));
    } else if (isInserting) {
      context.missing(_userIdMeta);
    }
    if (data.containsKey('version')) {
      context.handle(_versionMeta,
          version.isAcceptableOrUnknown(data['version']!, _versionMeta));
    }
    if (data.containsKey('updated_at')) {
      context.handle(_updatedAtMeta,
          updatedAt.isAcceptableOrUnknown(data['updated_at']!, _updatedAtMeta));
    } else if (isInserting) {
      context.missing(_updatedAtMeta);
    }
    if (data.containsKey('deleted_at')) {
      context.handle(_deletedAtMeta,
          deletedAt.isAcceptableOrUnknown(data['deleted_at']!, _deletedAtMeta));
    }
    if (data.containsKey('sync_state')) {
      context.handle(_syncStateMeta,
          syncState.isAcceptableOrUnknown(data['sync_state']!, _syncStateMeta));
    }
    if (data.containsKey('payload_json')) {
      context.handle(
          _payloadJsonMeta,
          payloadJson.isAcceptableOrUnknown(
              data['payload_json']!, _payloadJsonMeta));
    } else if (isInserting) {
      context.missing(_payloadJsonMeta);
    }
    return context;
  }

  @override
  Set<GeneratedColumn> get $primaryKey => {userId, id};
  @override
  SavingGoalsDriftTableData map(Map<String, dynamic> data,
      {String? tablePrefix}) {
    final effectivePrefix = tablePrefix != null ? '$tablePrefix.' : '';
    return SavingGoalsDriftTableData(
      id: attachedDatabase.typeMapping
          .read(DriftSqlType.string, data['${effectivePrefix}id'])!,
      userId: attachedDatabase.typeMapping
          .read(DriftSqlType.string, data['${effectivePrefix}user_id'])!,
      version: attachedDatabase.typeMapping
          .read(DriftSqlType.int, data['${effectivePrefix}version'])!,
      updatedAt: attachedDatabase.typeMapping
          .read(DriftSqlType.string, data['${effectivePrefix}updated_at'])!,
      deletedAt: attachedDatabase.typeMapping
          .read(DriftSqlType.string, data['${effectivePrefix}deleted_at']),
      syncState: attachedDatabase.typeMapping
          .read(DriftSqlType.string, data['${effectivePrefix}sync_state'])!,
      payloadJson: attachedDatabase.typeMapping
          .read(DriftSqlType.string, data['${effectivePrefix}payload_json'])!,
    );
  }

  @override
  $SavingGoalsDriftTableTable createAlias(String alias) {
    return $SavingGoalsDriftTableTable(attachedDatabase, alias);
  }
}

class SavingGoalsDriftTableData extends DataClass
    implements Insertable<SavingGoalsDriftTableData> {
  final String id;
  final String userId;
  final int version;
  final String updatedAt;
  final String? deletedAt;
  final String syncState;
  final String payloadJson;
  const SavingGoalsDriftTableData(
      {required this.id,
      required this.userId,
      required this.version,
      required this.updatedAt,
      this.deletedAt,
      required this.syncState,
      required this.payloadJson});
  @override
  Map<String, Expression> toColumns(bool nullToAbsent) {
    final map = <String, Expression>{};
    map['id'] = Variable<String>(id);
    map['user_id'] = Variable<String>(userId);
    map['version'] = Variable<int>(version);
    map['updated_at'] = Variable<String>(updatedAt);
    if (!nullToAbsent || deletedAt != null) {
      map['deleted_at'] = Variable<String>(deletedAt);
    }
    map['sync_state'] = Variable<String>(syncState);
    map['payload_json'] = Variable<String>(payloadJson);
    return map;
  }

  SavingGoalsDriftTableCompanion toCompanion(bool nullToAbsent) {
    return SavingGoalsDriftTableCompanion(
      id: Value(id),
      userId: Value(userId),
      version: Value(version),
      updatedAt: Value(updatedAt),
      deletedAt: deletedAt == null && nullToAbsent
          ? const Value.absent()
          : Value(deletedAt),
      syncState: Value(syncState),
      payloadJson: Value(payloadJson),
    );
  }

  factory SavingGoalsDriftTableData.fromJson(Map<String, dynamic> json,
      {ValueSerializer? serializer}) {
    serializer ??= driftRuntimeOptions.defaultSerializer;
    return SavingGoalsDriftTableData(
      id: serializer.fromJson<String>(json['id']),
      userId: serializer.fromJson<String>(json['userId']),
      version: serializer.fromJson<int>(json['version']),
      updatedAt: serializer.fromJson<String>(json['updatedAt']),
      deletedAt: serializer.fromJson<String?>(json['deletedAt']),
      syncState: serializer.fromJson<String>(json['syncState']),
      payloadJson: serializer.fromJson<String>(json['payloadJson']),
    );
  }
  @override
  Map<String, dynamic> toJson({ValueSerializer? serializer}) {
    serializer ??= driftRuntimeOptions.defaultSerializer;
    return <String, dynamic>{
      'id': serializer.toJson<String>(id),
      'userId': serializer.toJson<String>(userId),
      'version': serializer.toJson<int>(version),
      'updatedAt': serializer.toJson<String>(updatedAt),
      'deletedAt': serializer.toJson<String?>(deletedAt),
      'syncState': serializer.toJson<String>(syncState),
      'payloadJson': serializer.toJson<String>(payloadJson),
    };
  }

  SavingGoalsDriftTableData copyWith(
          {String? id,
          String? userId,
          int? version,
          String? updatedAt,
          Value<String?> deletedAt = const Value.absent(),
          String? syncState,
          String? payloadJson}) =>
      SavingGoalsDriftTableData(
        id: id ?? this.id,
        userId: userId ?? this.userId,
        version: version ?? this.version,
        updatedAt: updatedAt ?? this.updatedAt,
        deletedAt: deletedAt.present ? deletedAt.value : this.deletedAt,
        syncState: syncState ?? this.syncState,
        payloadJson: payloadJson ?? this.payloadJson,
      );
  SavingGoalsDriftTableData copyWithCompanion(
      SavingGoalsDriftTableCompanion data) {
    return SavingGoalsDriftTableData(
      id: data.id.present ? data.id.value : this.id,
      userId: data.userId.present ? data.userId.value : this.userId,
      version: data.version.present ? data.version.value : this.version,
      updatedAt: data.updatedAt.present ? data.updatedAt.value : this.updatedAt,
      deletedAt: data.deletedAt.present ? data.deletedAt.value : this.deletedAt,
      syncState: data.syncState.present ? data.syncState.value : this.syncState,
      payloadJson:
          data.payloadJson.present ? data.payloadJson.value : this.payloadJson,
    );
  }

  @override
  String toString() {
    return (StringBuffer('SavingGoalsDriftTableData(')
          ..write('id: $id, ')
          ..write('userId: $userId, ')
          ..write('version: $version, ')
          ..write('updatedAt: $updatedAt, ')
          ..write('deletedAt: $deletedAt, ')
          ..write('syncState: $syncState, ')
          ..write('payloadJson: $payloadJson')
          ..write(')'))
        .toString();
  }

  @override
  int get hashCode => Object.hash(
      id, userId, version, updatedAt, deletedAt, syncState, payloadJson);
  @override
  bool operator ==(Object other) =>
      identical(this, other) ||
      (other is SavingGoalsDriftTableData &&
          other.id == this.id &&
          other.userId == this.userId &&
          other.version == this.version &&
          other.updatedAt == this.updatedAt &&
          other.deletedAt == this.deletedAt &&
          other.syncState == this.syncState &&
          other.payloadJson == this.payloadJson);
}

class SavingGoalsDriftTableCompanion
    extends UpdateCompanion<SavingGoalsDriftTableData> {
  final Value<String> id;
  final Value<String> userId;
  final Value<int> version;
  final Value<String> updatedAt;
  final Value<String?> deletedAt;
  final Value<String> syncState;
  final Value<String> payloadJson;
  final Value<int> rowid;
  const SavingGoalsDriftTableCompanion({
    this.id = const Value.absent(),
    this.userId = const Value.absent(),
    this.version = const Value.absent(),
    this.updatedAt = const Value.absent(),
    this.deletedAt = const Value.absent(),
    this.syncState = const Value.absent(),
    this.payloadJson = const Value.absent(),
    this.rowid = const Value.absent(),
  });
  SavingGoalsDriftTableCompanion.insert({
    required String id,
    required String userId,
    this.version = const Value.absent(),
    required String updatedAt,
    this.deletedAt = const Value.absent(),
    this.syncState = const Value.absent(),
    required String payloadJson,
    this.rowid = const Value.absent(),
  })  : id = Value(id),
        userId = Value(userId),
        updatedAt = Value(updatedAt),
        payloadJson = Value(payloadJson);
  static Insertable<SavingGoalsDriftTableData> custom({
    Expression<String>? id,
    Expression<String>? userId,
    Expression<int>? version,
    Expression<String>? updatedAt,
    Expression<String>? deletedAt,
    Expression<String>? syncState,
    Expression<String>? payloadJson,
    Expression<int>? rowid,
  }) {
    return RawValuesInsertable({
      if (id != null) 'id': id,
      if (userId != null) 'user_id': userId,
      if (version != null) 'version': version,
      if (updatedAt != null) 'updated_at': updatedAt,
      if (deletedAt != null) 'deleted_at': deletedAt,
      if (syncState != null) 'sync_state': syncState,
      if (payloadJson != null) 'payload_json': payloadJson,
      if (rowid != null) 'rowid': rowid,
    });
  }

  SavingGoalsDriftTableCompanion copyWith(
      {Value<String>? id,
      Value<String>? userId,
      Value<int>? version,
      Value<String>? updatedAt,
      Value<String?>? deletedAt,
      Value<String>? syncState,
      Value<String>? payloadJson,
      Value<int>? rowid}) {
    return SavingGoalsDriftTableCompanion(
      id: id ?? this.id,
      userId: userId ?? this.userId,
      version: version ?? this.version,
      updatedAt: updatedAt ?? this.updatedAt,
      deletedAt: deletedAt ?? this.deletedAt,
      syncState: syncState ?? this.syncState,
      payloadJson: payloadJson ?? this.payloadJson,
      rowid: rowid ?? this.rowid,
    );
  }

  @override
  Map<String, Expression> toColumns(bool nullToAbsent) {
    final map = <String, Expression>{};
    if (id.present) {
      map['id'] = Variable<String>(id.value);
    }
    if (userId.present) {
      map['user_id'] = Variable<String>(userId.value);
    }
    if (version.present) {
      map['version'] = Variable<int>(version.value);
    }
    if (updatedAt.present) {
      map['updated_at'] = Variable<String>(updatedAt.value);
    }
    if (deletedAt.present) {
      map['deleted_at'] = Variable<String>(deletedAt.value);
    }
    if (syncState.present) {
      map['sync_state'] = Variable<String>(syncState.value);
    }
    if (payloadJson.present) {
      map['payload_json'] = Variable<String>(payloadJson.value);
    }
    if (rowid.present) {
      map['rowid'] = Variable<int>(rowid.value);
    }
    return map;
  }

  @override
  String toString() {
    return (StringBuffer('SavingGoalsDriftTableCompanion(')
          ..write('id: $id, ')
          ..write('userId: $userId, ')
          ..write('version: $version, ')
          ..write('updatedAt: $updatedAt, ')
          ..write('deletedAt: $deletedAt, ')
          ..write('syncState: $syncState, ')
          ..write('payloadJson: $payloadJson, ')
          ..write('rowid: $rowid')
          ..write(')'))
        .toString();
  }
}

class $SavingContributionsDriftTableTable extends SavingContributionsDriftTable
    with
        TableInfo<$SavingContributionsDriftTableTable,
            SavingContributionsDriftTableData> {
  @override
  final GeneratedDatabase attachedDatabase;
  final String? _alias;
  $SavingContributionsDriftTableTable(this.attachedDatabase, [this._alias]);
  static const VerificationMeta _idMeta = const VerificationMeta('id');
  @override
  late final GeneratedColumn<String> id = GeneratedColumn<String>(
      'id', aliasedName, false,
      type: DriftSqlType.string, requiredDuringInsert: true);
  static const VerificationMeta _userIdMeta = const VerificationMeta('userId');
  @override
  late final GeneratedColumn<String> userId = GeneratedColumn<String>(
      'user_id', aliasedName, false,
      type: DriftSqlType.string, requiredDuringInsert: true);
  static const VerificationMeta _versionMeta =
      const VerificationMeta('version');
  @override
  late final GeneratedColumn<int> version = GeneratedColumn<int>(
      'version', aliasedName, false,
      type: DriftSqlType.int,
      requiredDuringInsert: false,
      defaultValue: const Constant(0));
  static const VerificationMeta _updatedAtMeta =
      const VerificationMeta('updatedAt');
  @override
  late final GeneratedColumn<String> updatedAt = GeneratedColumn<String>(
      'updated_at', aliasedName, false,
      type: DriftSqlType.string, requiredDuringInsert: true);
  static const VerificationMeta _deletedAtMeta =
      const VerificationMeta('deletedAt');
  @override
  late final GeneratedColumn<String> deletedAt = GeneratedColumn<String>(
      'deleted_at', aliasedName, true,
      type: DriftSqlType.string, requiredDuringInsert: false);
  static const VerificationMeta _syncStateMeta =
      const VerificationMeta('syncState');
  @override
  late final GeneratedColumn<String> syncState = GeneratedColumn<String>(
      'sync_state', aliasedName, false,
      type: DriftSqlType.string,
      requiredDuringInsert: false,
      defaultValue: const Constant('synced'));
  static const VerificationMeta _payloadJsonMeta =
      const VerificationMeta('payloadJson');
  @override
  late final GeneratedColumn<String> payloadJson = GeneratedColumn<String>(
      'payload_json', aliasedName, false,
      type: DriftSqlType.string, requiredDuringInsert: true);
  @override
  List<GeneratedColumn> get $columns =>
      [id, userId, version, updatedAt, deletedAt, syncState, payloadJson];
  @override
  String get aliasedName => _alias ?? actualTableName;
  @override
  String get actualTableName => $name;
  static const String $name = 'saving_contributions';
  @override
  VerificationContext validateIntegrity(
      Insertable<SavingContributionsDriftTableData> instance,
      {bool isInserting = false}) {
    final context = VerificationContext();
    final data = instance.toColumns(true);
    if (data.containsKey('id')) {
      context.handle(_idMeta, id.isAcceptableOrUnknown(data['id']!, _idMeta));
    } else if (isInserting) {
      context.missing(_idMeta);
    }
    if (data.containsKey('user_id')) {
      context.handle(_userIdMeta,
          userId.isAcceptableOrUnknown(data['user_id']!, _userIdMeta));
    } else if (isInserting) {
      context.missing(_userIdMeta);
    }
    if (data.containsKey('version')) {
      context.handle(_versionMeta,
          version.isAcceptableOrUnknown(data['version']!, _versionMeta));
    }
    if (data.containsKey('updated_at')) {
      context.handle(_updatedAtMeta,
          updatedAt.isAcceptableOrUnknown(data['updated_at']!, _updatedAtMeta));
    } else if (isInserting) {
      context.missing(_updatedAtMeta);
    }
    if (data.containsKey('deleted_at')) {
      context.handle(_deletedAtMeta,
          deletedAt.isAcceptableOrUnknown(data['deleted_at']!, _deletedAtMeta));
    }
    if (data.containsKey('sync_state')) {
      context.handle(_syncStateMeta,
          syncState.isAcceptableOrUnknown(data['sync_state']!, _syncStateMeta));
    }
    if (data.containsKey('payload_json')) {
      context.handle(
          _payloadJsonMeta,
          payloadJson.isAcceptableOrUnknown(
              data['payload_json']!, _payloadJsonMeta));
    } else if (isInserting) {
      context.missing(_payloadJsonMeta);
    }
    return context;
  }

  @override
  Set<GeneratedColumn> get $primaryKey => {userId, id};
  @override
  SavingContributionsDriftTableData map(Map<String, dynamic> data,
      {String? tablePrefix}) {
    final effectivePrefix = tablePrefix != null ? '$tablePrefix.' : '';
    return SavingContributionsDriftTableData(
      id: attachedDatabase.typeMapping
          .read(DriftSqlType.string, data['${effectivePrefix}id'])!,
      userId: attachedDatabase.typeMapping
          .read(DriftSqlType.string, data['${effectivePrefix}user_id'])!,
      version: attachedDatabase.typeMapping
          .read(DriftSqlType.int, data['${effectivePrefix}version'])!,
      updatedAt: attachedDatabase.typeMapping
          .read(DriftSqlType.string, data['${effectivePrefix}updated_at'])!,
      deletedAt: attachedDatabase.typeMapping
          .read(DriftSqlType.string, data['${effectivePrefix}deleted_at']),
      syncState: attachedDatabase.typeMapping
          .read(DriftSqlType.string, data['${effectivePrefix}sync_state'])!,
      payloadJson: attachedDatabase.typeMapping
          .read(DriftSqlType.string, data['${effectivePrefix}payload_json'])!,
    );
  }

  @override
  $SavingContributionsDriftTableTable createAlias(String alias) {
    return $SavingContributionsDriftTableTable(attachedDatabase, alias);
  }
}

class SavingContributionsDriftTableData extends DataClass
    implements Insertable<SavingContributionsDriftTableData> {
  final String id;
  final String userId;
  final int version;
  final String updatedAt;
  final String? deletedAt;
  final String syncState;
  final String payloadJson;
  const SavingContributionsDriftTableData(
      {required this.id,
      required this.userId,
      required this.version,
      required this.updatedAt,
      this.deletedAt,
      required this.syncState,
      required this.payloadJson});
  @override
  Map<String, Expression> toColumns(bool nullToAbsent) {
    final map = <String, Expression>{};
    map['id'] = Variable<String>(id);
    map['user_id'] = Variable<String>(userId);
    map['version'] = Variable<int>(version);
    map['updated_at'] = Variable<String>(updatedAt);
    if (!nullToAbsent || deletedAt != null) {
      map['deleted_at'] = Variable<String>(deletedAt);
    }
    map['sync_state'] = Variable<String>(syncState);
    map['payload_json'] = Variable<String>(payloadJson);
    return map;
  }

  SavingContributionsDriftTableCompanion toCompanion(bool nullToAbsent) {
    return SavingContributionsDriftTableCompanion(
      id: Value(id),
      userId: Value(userId),
      version: Value(version),
      updatedAt: Value(updatedAt),
      deletedAt: deletedAt == null && nullToAbsent
          ? const Value.absent()
          : Value(deletedAt),
      syncState: Value(syncState),
      payloadJson: Value(payloadJson),
    );
  }

  factory SavingContributionsDriftTableData.fromJson(Map<String, dynamic> json,
      {ValueSerializer? serializer}) {
    serializer ??= driftRuntimeOptions.defaultSerializer;
    return SavingContributionsDriftTableData(
      id: serializer.fromJson<String>(json['id']),
      userId: serializer.fromJson<String>(json['userId']),
      version: serializer.fromJson<int>(json['version']),
      updatedAt: serializer.fromJson<String>(json['updatedAt']),
      deletedAt: serializer.fromJson<String?>(json['deletedAt']),
      syncState: serializer.fromJson<String>(json['syncState']),
      payloadJson: serializer.fromJson<String>(json['payloadJson']),
    );
  }
  @override
  Map<String, dynamic> toJson({ValueSerializer? serializer}) {
    serializer ??= driftRuntimeOptions.defaultSerializer;
    return <String, dynamic>{
      'id': serializer.toJson<String>(id),
      'userId': serializer.toJson<String>(userId),
      'version': serializer.toJson<int>(version),
      'updatedAt': serializer.toJson<String>(updatedAt),
      'deletedAt': serializer.toJson<String?>(deletedAt),
      'syncState': serializer.toJson<String>(syncState),
      'payloadJson': serializer.toJson<String>(payloadJson),
    };
  }

  SavingContributionsDriftTableData copyWith(
          {String? id,
          String? userId,
          int? version,
          String? updatedAt,
          Value<String?> deletedAt = const Value.absent(),
          String? syncState,
          String? payloadJson}) =>
      SavingContributionsDriftTableData(
        id: id ?? this.id,
        userId: userId ?? this.userId,
        version: version ?? this.version,
        updatedAt: updatedAt ?? this.updatedAt,
        deletedAt: deletedAt.present ? deletedAt.value : this.deletedAt,
        syncState: syncState ?? this.syncState,
        payloadJson: payloadJson ?? this.payloadJson,
      );
  SavingContributionsDriftTableData copyWithCompanion(
      SavingContributionsDriftTableCompanion data) {
    return SavingContributionsDriftTableData(
      id: data.id.present ? data.id.value : this.id,
      userId: data.userId.present ? data.userId.value : this.userId,
      version: data.version.present ? data.version.value : this.version,
      updatedAt: data.updatedAt.present ? data.updatedAt.value : this.updatedAt,
      deletedAt: data.deletedAt.present ? data.deletedAt.value : this.deletedAt,
      syncState: data.syncState.present ? data.syncState.value : this.syncState,
      payloadJson:
          data.payloadJson.present ? data.payloadJson.value : this.payloadJson,
    );
  }

  @override
  String toString() {
    return (StringBuffer('SavingContributionsDriftTableData(')
          ..write('id: $id, ')
          ..write('userId: $userId, ')
          ..write('version: $version, ')
          ..write('updatedAt: $updatedAt, ')
          ..write('deletedAt: $deletedAt, ')
          ..write('syncState: $syncState, ')
          ..write('payloadJson: $payloadJson')
          ..write(')'))
        .toString();
  }

  @override
  int get hashCode => Object.hash(
      id, userId, version, updatedAt, deletedAt, syncState, payloadJson);
  @override
  bool operator ==(Object other) =>
      identical(this, other) ||
      (other is SavingContributionsDriftTableData &&
          other.id == this.id &&
          other.userId == this.userId &&
          other.version == this.version &&
          other.updatedAt == this.updatedAt &&
          other.deletedAt == this.deletedAt &&
          other.syncState == this.syncState &&
          other.payloadJson == this.payloadJson);
}

class SavingContributionsDriftTableCompanion
    extends UpdateCompanion<SavingContributionsDriftTableData> {
  final Value<String> id;
  final Value<String> userId;
  final Value<int> version;
  final Value<String> updatedAt;
  final Value<String?> deletedAt;
  final Value<String> syncState;
  final Value<String> payloadJson;
  final Value<int> rowid;
  const SavingContributionsDriftTableCompanion({
    this.id = const Value.absent(),
    this.userId = const Value.absent(),
    this.version = const Value.absent(),
    this.updatedAt = const Value.absent(),
    this.deletedAt = const Value.absent(),
    this.syncState = const Value.absent(),
    this.payloadJson = const Value.absent(),
    this.rowid = const Value.absent(),
  });
  SavingContributionsDriftTableCompanion.insert({
    required String id,
    required String userId,
    this.version = const Value.absent(),
    required String updatedAt,
    this.deletedAt = const Value.absent(),
    this.syncState = const Value.absent(),
    required String payloadJson,
    this.rowid = const Value.absent(),
  })  : id = Value(id),
        userId = Value(userId),
        updatedAt = Value(updatedAt),
        payloadJson = Value(payloadJson);
  static Insertable<SavingContributionsDriftTableData> custom({
    Expression<String>? id,
    Expression<String>? userId,
    Expression<int>? version,
    Expression<String>? updatedAt,
    Expression<String>? deletedAt,
    Expression<String>? syncState,
    Expression<String>? payloadJson,
    Expression<int>? rowid,
  }) {
    return RawValuesInsertable({
      if (id != null) 'id': id,
      if (userId != null) 'user_id': userId,
      if (version != null) 'version': version,
      if (updatedAt != null) 'updated_at': updatedAt,
      if (deletedAt != null) 'deleted_at': deletedAt,
      if (syncState != null) 'sync_state': syncState,
      if (payloadJson != null) 'payload_json': payloadJson,
      if (rowid != null) 'rowid': rowid,
    });
  }

  SavingContributionsDriftTableCompanion copyWith(
      {Value<String>? id,
      Value<String>? userId,
      Value<int>? version,
      Value<String>? updatedAt,
      Value<String?>? deletedAt,
      Value<String>? syncState,
      Value<String>? payloadJson,
      Value<int>? rowid}) {
    return SavingContributionsDriftTableCompanion(
      id: id ?? this.id,
      userId: userId ?? this.userId,
      version: version ?? this.version,
      updatedAt: updatedAt ?? this.updatedAt,
      deletedAt: deletedAt ?? this.deletedAt,
      syncState: syncState ?? this.syncState,
      payloadJson: payloadJson ?? this.payloadJson,
      rowid: rowid ?? this.rowid,
    );
  }

  @override
  Map<String, Expression> toColumns(bool nullToAbsent) {
    final map = <String, Expression>{};
    if (id.present) {
      map['id'] = Variable<String>(id.value);
    }
    if (userId.present) {
      map['user_id'] = Variable<String>(userId.value);
    }
    if (version.present) {
      map['version'] = Variable<int>(version.value);
    }
    if (updatedAt.present) {
      map['updated_at'] = Variable<String>(updatedAt.value);
    }
    if (deletedAt.present) {
      map['deleted_at'] = Variable<String>(deletedAt.value);
    }
    if (syncState.present) {
      map['sync_state'] = Variable<String>(syncState.value);
    }
    if (payloadJson.present) {
      map['payload_json'] = Variable<String>(payloadJson.value);
    }
    if (rowid.present) {
      map['rowid'] = Variable<int>(rowid.value);
    }
    return map;
  }

  @override
  String toString() {
    return (StringBuffer('SavingContributionsDriftTableCompanion(')
          ..write('id: $id, ')
          ..write('userId: $userId, ')
          ..write('version: $version, ')
          ..write('updatedAt: $updatedAt, ')
          ..write('deletedAt: $deletedAt, ')
          ..write('syncState: $syncState, ')
          ..write('payloadJson: $payloadJson, ')
          ..write('rowid: $rowid')
          ..write(')'))
        .toString();
  }
}

class $CalendarEventsDriftTableTable extends CalendarEventsDriftTable
    with
        TableInfo<$CalendarEventsDriftTableTable,
            CalendarEventsDriftTableData> {
  @override
  final GeneratedDatabase attachedDatabase;
  final String? _alias;
  $CalendarEventsDriftTableTable(this.attachedDatabase, [this._alias]);
  static const VerificationMeta _idMeta = const VerificationMeta('id');
  @override
  late final GeneratedColumn<String> id = GeneratedColumn<String>(
      'id', aliasedName, false,
      type: DriftSqlType.string, requiredDuringInsert: true);
  static const VerificationMeta _userIdMeta = const VerificationMeta('userId');
  @override
  late final GeneratedColumn<String> userId = GeneratedColumn<String>(
      'user_id', aliasedName, false,
      type: DriftSqlType.string, requiredDuringInsert: true);
  static const VerificationMeta _versionMeta =
      const VerificationMeta('version');
  @override
  late final GeneratedColumn<int> version = GeneratedColumn<int>(
      'version', aliasedName, false,
      type: DriftSqlType.int,
      requiredDuringInsert: false,
      defaultValue: const Constant(0));
  static const VerificationMeta _updatedAtMeta =
      const VerificationMeta('updatedAt');
  @override
  late final GeneratedColumn<String> updatedAt = GeneratedColumn<String>(
      'updated_at', aliasedName, false,
      type: DriftSqlType.string, requiredDuringInsert: true);
  static const VerificationMeta _deletedAtMeta =
      const VerificationMeta('deletedAt');
  @override
  late final GeneratedColumn<String> deletedAt = GeneratedColumn<String>(
      'deleted_at', aliasedName, true,
      type: DriftSqlType.string, requiredDuringInsert: false);
  static const VerificationMeta _syncStateMeta =
      const VerificationMeta('syncState');
  @override
  late final GeneratedColumn<String> syncState = GeneratedColumn<String>(
      'sync_state', aliasedName, false,
      type: DriftSqlType.string,
      requiredDuringInsert: false,
      defaultValue: const Constant('synced'));
  static const VerificationMeta _payloadJsonMeta =
      const VerificationMeta('payloadJson');
  @override
  late final GeneratedColumn<String> payloadJson = GeneratedColumn<String>(
      'payload_json', aliasedName, false,
      type: DriftSqlType.string, requiredDuringInsert: true);
  @override
  List<GeneratedColumn> get $columns =>
      [id, userId, version, updatedAt, deletedAt, syncState, payloadJson];
  @override
  String get aliasedName => _alias ?? actualTableName;
  @override
  String get actualTableName => $name;
  static const String $name = 'calendar_events';
  @override
  VerificationContext validateIntegrity(
      Insertable<CalendarEventsDriftTableData> instance,
      {bool isInserting = false}) {
    final context = VerificationContext();
    final data = instance.toColumns(true);
    if (data.containsKey('id')) {
      context.handle(_idMeta, id.isAcceptableOrUnknown(data['id']!, _idMeta));
    } else if (isInserting) {
      context.missing(_idMeta);
    }
    if (data.containsKey('user_id')) {
      context.handle(_userIdMeta,
          userId.isAcceptableOrUnknown(data['user_id']!, _userIdMeta));
    } else if (isInserting) {
      context.missing(_userIdMeta);
    }
    if (data.containsKey('version')) {
      context.handle(_versionMeta,
          version.isAcceptableOrUnknown(data['version']!, _versionMeta));
    }
    if (data.containsKey('updated_at')) {
      context.handle(_updatedAtMeta,
          updatedAt.isAcceptableOrUnknown(data['updated_at']!, _updatedAtMeta));
    } else if (isInserting) {
      context.missing(_updatedAtMeta);
    }
    if (data.containsKey('deleted_at')) {
      context.handle(_deletedAtMeta,
          deletedAt.isAcceptableOrUnknown(data['deleted_at']!, _deletedAtMeta));
    }
    if (data.containsKey('sync_state')) {
      context.handle(_syncStateMeta,
          syncState.isAcceptableOrUnknown(data['sync_state']!, _syncStateMeta));
    }
    if (data.containsKey('payload_json')) {
      context.handle(
          _payloadJsonMeta,
          payloadJson.isAcceptableOrUnknown(
              data['payload_json']!, _payloadJsonMeta));
    } else if (isInserting) {
      context.missing(_payloadJsonMeta);
    }
    return context;
  }

  @override
  Set<GeneratedColumn> get $primaryKey => {userId, id};
  @override
  CalendarEventsDriftTableData map(Map<String, dynamic> data,
      {String? tablePrefix}) {
    final effectivePrefix = tablePrefix != null ? '$tablePrefix.' : '';
    return CalendarEventsDriftTableData(
      id: attachedDatabase.typeMapping
          .read(DriftSqlType.string, data['${effectivePrefix}id'])!,
      userId: attachedDatabase.typeMapping
          .read(DriftSqlType.string, data['${effectivePrefix}user_id'])!,
      version: attachedDatabase.typeMapping
          .read(DriftSqlType.int, data['${effectivePrefix}version'])!,
      updatedAt: attachedDatabase.typeMapping
          .read(DriftSqlType.string, data['${effectivePrefix}updated_at'])!,
      deletedAt: attachedDatabase.typeMapping
          .read(DriftSqlType.string, data['${effectivePrefix}deleted_at']),
      syncState: attachedDatabase.typeMapping
          .read(DriftSqlType.string, data['${effectivePrefix}sync_state'])!,
      payloadJson: attachedDatabase.typeMapping
          .read(DriftSqlType.string, data['${effectivePrefix}payload_json'])!,
    );
  }

  @override
  $CalendarEventsDriftTableTable createAlias(String alias) {
    return $CalendarEventsDriftTableTable(attachedDatabase, alias);
  }
}

class CalendarEventsDriftTableData extends DataClass
    implements Insertable<CalendarEventsDriftTableData> {
  final String id;
  final String userId;
  final int version;
  final String updatedAt;
  final String? deletedAt;
  final String syncState;
  final String payloadJson;
  const CalendarEventsDriftTableData(
      {required this.id,
      required this.userId,
      required this.version,
      required this.updatedAt,
      this.deletedAt,
      required this.syncState,
      required this.payloadJson});
  @override
  Map<String, Expression> toColumns(bool nullToAbsent) {
    final map = <String, Expression>{};
    map['id'] = Variable<String>(id);
    map['user_id'] = Variable<String>(userId);
    map['version'] = Variable<int>(version);
    map['updated_at'] = Variable<String>(updatedAt);
    if (!nullToAbsent || deletedAt != null) {
      map['deleted_at'] = Variable<String>(deletedAt);
    }
    map['sync_state'] = Variable<String>(syncState);
    map['payload_json'] = Variable<String>(payloadJson);
    return map;
  }

  CalendarEventsDriftTableCompanion toCompanion(bool nullToAbsent) {
    return CalendarEventsDriftTableCompanion(
      id: Value(id),
      userId: Value(userId),
      version: Value(version),
      updatedAt: Value(updatedAt),
      deletedAt: deletedAt == null && nullToAbsent
          ? const Value.absent()
          : Value(deletedAt),
      syncState: Value(syncState),
      payloadJson: Value(payloadJson),
    );
  }

  factory CalendarEventsDriftTableData.fromJson(Map<String, dynamic> json,
      {ValueSerializer? serializer}) {
    serializer ??= driftRuntimeOptions.defaultSerializer;
    return CalendarEventsDriftTableData(
      id: serializer.fromJson<String>(json['id']),
      userId: serializer.fromJson<String>(json['userId']),
      version: serializer.fromJson<int>(json['version']),
      updatedAt: serializer.fromJson<String>(json['updatedAt']),
      deletedAt: serializer.fromJson<String?>(json['deletedAt']),
      syncState: serializer.fromJson<String>(json['syncState']),
      payloadJson: serializer.fromJson<String>(json['payloadJson']),
    );
  }
  @override
  Map<String, dynamic> toJson({ValueSerializer? serializer}) {
    serializer ??= driftRuntimeOptions.defaultSerializer;
    return <String, dynamic>{
      'id': serializer.toJson<String>(id),
      'userId': serializer.toJson<String>(userId),
      'version': serializer.toJson<int>(version),
      'updatedAt': serializer.toJson<String>(updatedAt),
      'deletedAt': serializer.toJson<String?>(deletedAt),
      'syncState': serializer.toJson<String>(syncState),
      'payloadJson': serializer.toJson<String>(payloadJson),
    };
  }

  CalendarEventsDriftTableData copyWith(
          {String? id,
          String? userId,
          int? version,
          String? updatedAt,
          Value<String?> deletedAt = const Value.absent(),
          String? syncState,
          String? payloadJson}) =>
      CalendarEventsDriftTableData(
        id: id ?? this.id,
        userId: userId ?? this.userId,
        version: version ?? this.version,
        updatedAt: updatedAt ?? this.updatedAt,
        deletedAt: deletedAt.present ? deletedAt.value : this.deletedAt,
        syncState: syncState ?? this.syncState,
        payloadJson: payloadJson ?? this.payloadJson,
      );
  CalendarEventsDriftTableData copyWithCompanion(
      CalendarEventsDriftTableCompanion data) {
    return CalendarEventsDriftTableData(
      id: data.id.present ? data.id.value : this.id,
      userId: data.userId.present ? data.userId.value : this.userId,
      version: data.version.present ? data.version.value : this.version,
      updatedAt: data.updatedAt.present ? data.updatedAt.value : this.updatedAt,
      deletedAt: data.deletedAt.present ? data.deletedAt.value : this.deletedAt,
      syncState: data.syncState.present ? data.syncState.value : this.syncState,
      payloadJson:
          data.payloadJson.present ? data.payloadJson.value : this.payloadJson,
    );
  }

  @override
  String toString() {
    return (StringBuffer('CalendarEventsDriftTableData(')
          ..write('id: $id, ')
          ..write('userId: $userId, ')
          ..write('version: $version, ')
          ..write('updatedAt: $updatedAt, ')
          ..write('deletedAt: $deletedAt, ')
          ..write('syncState: $syncState, ')
          ..write('payloadJson: $payloadJson')
          ..write(')'))
        .toString();
  }

  @override
  int get hashCode => Object.hash(
      id, userId, version, updatedAt, deletedAt, syncState, payloadJson);
  @override
  bool operator ==(Object other) =>
      identical(this, other) ||
      (other is CalendarEventsDriftTableData &&
          other.id == this.id &&
          other.userId == this.userId &&
          other.version == this.version &&
          other.updatedAt == this.updatedAt &&
          other.deletedAt == this.deletedAt &&
          other.syncState == this.syncState &&
          other.payloadJson == this.payloadJson);
}

class CalendarEventsDriftTableCompanion
    extends UpdateCompanion<CalendarEventsDriftTableData> {
  final Value<String> id;
  final Value<String> userId;
  final Value<int> version;
  final Value<String> updatedAt;
  final Value<String?> deletedAt;
  final Value<String> syncState;
  final Value<String> payloadJson;
  final Value<int> rowid;
  const CalendarEventsDriftTableCompanion({
    this.id = const Value.absent(),
    this.userId = const Value.absent(),
    this.version = const Value.absent(),
    this.updatedAt = const Value.absent(),
    this.deletedAt = const Value.absent(),
    this.syncState = const Value.absent(),
    this.payloadJson = const Value.absent(),
    this.rowid = const Value.absent(),
  });
  CalendarEventsDriftTableCompanion.insert({
    required String id,
    required String userId,
    this.version = const Value.absent(),
    required String updatedAt,
    this.deletedAt = const Value.absent(),
    this.syncState = const Value.absent(),
    required String payloadJson,
    this.rowid = const Value.absent(),
  })  : id = Value(id),
        userId = Value(userId),
        updatedAt = Value(updatedAt),
        payloadJson = Value(payloadJson);
  static Insertable<CalendarEventsDriftTableData> custom({
    Expression<String>? id,
    Expression<String>? userId,
    Expression<int>? version,
    Expression<String>? updatedAt,
    Expression<String>? deletedAt,
    Expression<String>? syncState,
    Expression<String>? payloadJson,
    Expression<int>? rowid,
  }) {
    return RawValuesInsertable({
      if (id != null) 'id': id,
      if (userId != null) 'user_id': userId,
      if (version != null) 'version': version,
      if (updatedAt != null) 'updated_at': updatedAt,
      if (deletedAt != null) 'deleted_at': deletedAt,
      if (syncState != null) 'sync_state': syncState,
      if (payloadJson != null) 'payload_json': payloadJson,
      if (rowid != null) 'rowid': rowid,
    });
  }

  CalendarEventsDriftTableCompanion copyWith(
      {Value<String>? id,
      Value<String>? userId,
      Value<int>? version,
      Value<String>? updatedAt,
      Value<String?>? deletedAt,
      Value<String>? syncState,
      Value<String>? payloadJson,
      Value<int>? rowid}) {
    return CalendarEventsDriftTableCompanion(
      id: id ?? this.id,
      userId: userId ?? this.userId,
      version: version ?? this.version,
      updatedAt: updatedAt ?? this.updatedAt,
      deletedAt: deletedAt ?? this.deletedAt,
      syncState: syncState ?? this.syncState,
      payloadJson: payloadJson ?? this.payloadJson,
      rowid: rowid ?? this.rowid,
    );
  }

  @override
  Map<String, Expression> toColumns(bool nullToAbsent) {
    final map = <String, Expression>{};
    if (id.present) {
      map['id'] = Variable<String>(id.value);
    }
    if (userId.present) {
      map['user_id'] = Variable<String>(userId.value);
    }
    if (version.present) {
      map['version'] = Variable<int>(version.value);
    }
    if (updatedAt.present) {
      map['updated_at'] = Variable<String>(updatedAt.value);
    }
    if (deletedAt.present) {
      map['deleted_at'] = Variable<String>(deletedAt.value);
    }
    if (syncState.present) {
      map['sync_state'] = Variable<String>(syncState.value);
    }
    if (payloadJson.present) {
      map['payload_json'] = Variable<String>(payloadJson.value);
    }
    if (rowid.present) {
      map['rowid'] = Variable<int>(rowid.value);
    }
    return map;
  }

  @override
  String toString() {
    return (StringBuffer('CalendarEventsDriftTableCompanion(')
          ..write('id: $id, ')
          ..write('userId: $userId, ')
          ..write('version: $version, ')
          ..write('updatedAt: $updatedAt, ')
          ..write('deletedAt: $deletedAt, ')
          ..write('syncState: $syncState, ')
          ..write('payloadJson: $payloadJson, ')
          ..write('rowid: $rowid')
          ..write(')'))
        .toString();
  }
}

class $EventExceptionsDriftTableTable extends EventExceptionsDriftTable
    with
        TableInfo<$EventExceptionsDriftTableTable,
            EventExceptionsDriftTableData> {
  @override
  final GeneratedDatabase attachedDatabase;
  final String? _alias;
  $EventExceptionsDriftTableTable(this.attachedDatabase, [this._alias]);
  static const VerificationMeta _idMeta = const VerificationMeta('id');
  @override
  late final GeneratedColumn<String> id = GeneratedColumn<String>(
      'id', aliasedName, false,
      type: DriftSqlType.string, requiredDuringInsert: true);
  static const VerificationMeta _userIdMeta = const VerificationMeta('userId');
  @override
  late final GeneratedColumn<String> userId = GeneratedColumn<String>(
      'user_id', aliasedName, false,
      type: DriftSqlType.string, requiredDuringInsert: true);
  static const VerificationMeta _versionMeta =
      const VerificationMeta('version');
  @override
  late final GeneratedColumn<int> version = GeneratedColumn<int>(
      'version', aliasedName, false,
      type: DriftSqlType.int,
      requiredDuringInsert: false,
      defaultValue: const Constant(0));
  static const VerificationMeta _updatedAtMeta =
      const VerificationMeta('updatedAt');
  @override
  late final GeneratedColumn<String> updatedAt = GeneratedColumn<String>(
      'updated_at', aliasedName, false,
      type: DriftSqlType.string, requiredDuringInsert: true);
  static const VerificationMeta _deletedAtMeta =
      const VerificationMeta('deletedAt');
  @override
  late final GeneratedColumn<String> deletedAt = GeneratedColumn<String>(
      'deleted_at', aliasedName, true,
      type: DriftSqlType.string, requiredDuringInsert: false);
  static const VerificationMeta _syncStateMeta =
      const VerificationMeta('syncState');
  @override
  late final GeneratedColumn<String> syncState = GeneratedColumn<String>(
      'sync_state', aliasedName, false,
      type: DriftSqlType.string,
      requiredDuringInsert: false,
      defaultValue: const Constant('synced'));
  static const VerificationMeta _payloadJsonMeta =
      const VerificationMeta('payloadJson');
  @override
  late final GeneratedColumn<String> payloadJson = GeneratedColumn<String>(
      'payload_json', aliasedName, false,
      type: DriftSqlType.string, requiredDuringInsert: true);
  @override
  List<GeneratedColumn> get $columns =>
      [id, userId, version, updatedAt, deletedAt, syncState, payloadJson];
  @override
  String get aliasedName => _alias ?? actualTableName;
  @override
  String get actualTableName => $name;
  static const String $name = 'event_exceptions';
  @override
  VerificationContext validateIntegrity(
      Insertable<EventExceptionsDriftTableData> instance,
      {bool isInserting = false}) {
    final context = VerificationContext();
    final data = instance.toColumns(true);
    if (data.containsKey('id')) {
      context.handle(_idMeta, id.isAcceptableOrUnknown(data['id']!, _idMeta));
    } else if (isInserting) {
      context.missing(_idMeta);
    }
    if (data.containsKey('user_id')) {
      context.handle(_userIdMeta,
          userId.isAcceptableOrUnknown(data['user_id']!, _userIdMeta));
    } else if (isInserting) {
      context.missing(_userIdMeta);
    }
    if (data.containsKey('version')) {
      context.handle(_versionMeta,
          version.isAcceptableOrUnknown(data['version']!, _versionMeta));
    }
    if (data.containsKey('updated_at')) {
      context.handle(_updatedAtMeta,
          updatedAt.isAcceptableOrUnknown(data['updated_at']!, _updatedAtMeta));
    } else if (isInserting) {
      context.missing(_updatedAtMeta);
    }
    if (data.containsKey('deleted_at')) {
      context.handle(_deletedAtMeta,
          deletedAt.isAcceptableOrUnknown(data['deleted_at']!, _deletedAtMeta));
    }
    if (data.containsKey('sync_state')) {
      context.handle(_syncStateMeta,
          syncState.isAcceptableOrUnknown(data['sync_state']!, _syncStateMeta));
    }
    if (data.containsKey('payload_json')) {
      context.handle(
          _payloadJsonMeta,
          payloadJson.isAcceptableOrUnknown(
              data['payload_json']!, _payloadJsonMeta));
    } else if (isInserting) {
      context.missing(_payloadJsonMeta);
    }
    return context;
  }

  @override
  Set<GeneratedColumn> get $primaryKey => {userId, id};
  @override
  EventExceptionsDriftTableData map(Map<String, dynamic> data,
      {String? tablePrefix}) {
    final effectivePrefix = tablePrefix != null ? '$tablePrefix.' : '';
    return EventExceptionsDriftTableData(
      id: attachedDatabase.typeMapping
          .read(DriftSqlType.string, data['${effectivePrefix}id'])!,
      userId: attachedDatabase.typeMapping
          .read(DriftSqlType.string, data['${effectivePrefix}user_id'])!,
      version: attachedDatabase.typeMapping
          .read(DriftSqlType.int, data['${effectivePrefix}version'])!,
      updatedAt: attachedDatabase.typeMapping
          .read(DriftSqlType.string, data['${effectivePrefix}updated_at'])!,
      deletedAt: attachedDatabase.typeMapping
          .read(DriftSqlType.string, data['${effectivePrefix}deleted_at']),
      syncState: attachedDatabase.typeMapping
          .read(DriftSqlType.string, data['${effectivePrefix}sync_state'])!,
      payloadJson: attachedDatabase.typeMapping
          .read(DriftSqlType.string, data['${effectivePrefix}payload_json'])!,
    );
  }

  @override
  $EventExceptionsDriftTableTable createAlias(String alias) {
    return $EventExceptionsDriftTableTable(attachedDatabase, alias);
  }
}

class EventExceptionsDriftTableData extends DataClass
    implements Insertable<EventExceptionsDriftTableData> {
  final String id;
  final String userId;
  final int version;
  final String updatedAt;
  final String? deletedAt;
  final String syncState;
  final String payloadJson;
  const EventExceptionsDriftTableData(
      {required this.id,
      required this.userId,
      required this.version,
      required this.updatedAt,
      this.deletedAt,
      required this.syncState,
      required this.payloadJson});
  @override
  Map<String, Expression> toColumns(bool nullToAbsent) {
    final map = <String, Expression>{};
    map['id'] = Variable<String>(id);
    map['user_id'] = Variable<String>(userId);
    map['version'] = Variable<int>(version);
    map['updated_at'] = Variable<String>(updatedAt);
    if (!nullToAbsent || deletedAt != null) {
      map['deleted_at'] = Variable<String>(deletedAt);
    }
    map['sync_state'] = Variable<String>(syncState);
    map['payload_json'] = Variable<String>(payloadJson);
    return map;
  }

  EventExceptionsDriftTableCompanion toCompanion(bool nullToAbsent) {
    return EventExceptionsDriftTableCompanion(
      id: Value(id),
      userId: Value(userId),
      version: Value(version),
      updatedAt: Value(updatedAt),
      deletedAt: deletedAt == null && nullToAbsent
          ? const Value.absent()
          : Value(deletedAt),
      syncState: Value(syncState),
      payloadJson: Value(payloadJson),
    );
  }

  factory EventExceptionsDriftTableData.fromJson(Map<String, dynamic> json,
      {ValueSerializer? serializer}) {
    serializer ??= driftRuntimeOptions.defaultSerializer;
    return EventExceptionsDriftTableData(
      id: serializer.fromJson<String>(json['id']),
      userId: serializer.fromJson<String>(json['userId']),
      version: serializer.fromJson<int>(json['version']),
      updatedAt: serializer.fromJson<String>(json['updatedAt']),
      deletedAt: serializer.fromJson<String?>(json['deletedAt']),
      syncState: serializer.fromJson<String>(json['syncState']),
      payloadJson: serializer.fromJson<String>(json['payloadJson']),
    );
  }
  @override
  Map<String, dynamic> toJson({ValueSerializer? serializer}) {
    serializer ??= driftRuntimeOptions.defaultSerializer;
    return <String, dynamic>{
      'id': serializer.toJson<String>(id),
      'userId': serializer.toJson<String>(userId),
      'version': serializer.toJson<int>(version),
      'updatedAt': serializer.toJson<String>(updatedAt),
      'deletedAt': serializer.toJson<String?>(deletedAt),
      'syncState': serializer.toJson<String>(syncState),
      'payloadJson': serializer.toJson<String>(payloadJson),
    };
  }

  EventExceptionsDriftTableData copyWith(
          {String? id,
          String? userId,
          int? version,
          String? updatedAt,
          Value<String?> deletedAt = const Value.absent(),
          String? syncState,
          String? payloadJson}) =>
      EventExceptionsDriftTableData(
        id: id ?? this.id,
        userId: userId ?? this.userId,
        version: version ?? this.version,
        updatedAt: updatedAt ?? this.updatedAt,
        deletedAt: deletedAt.present ? deletedAt.value : this.deletedAt,
        syncState: syncState ?? this.syncState,
        payloadJson: payloadJson ?? this.payloadJson,
      );
  EventExceptionsDriftTableData copyWithCompanion(
      EventExceptionsDriftTableCompanion data) {
    return EventExceptionsDriftTableData(
      id: data.id.present ? data.id.value : this.id,
      userId: data.userId.present ? data.userId.value : this.userId,
      version: data.version.present ? data.version.value : this.version,
      updatedAt: data.updatedAt.present ? data.updatedAt.value : this.updatedAt,
      deletedAt: data.deletedAt.present ? data.deletedAt.value : this.deletedAt,
      syncState: data.syncState.present ? data.syncState.value : this.syncState,
      payloadJson:
          data.payloadJson.present ? data.payloadJson.value : this.payloadJson,
    );
  }

  @override
  String toString() {
    return (StringBuffer('EventExceptionsDriftTableData(')
          ..write('id: $id, ')
          ..write('userId: $userId, ')
          ..write('version: $version, ')
          ..write('updatedAt: $updatedAt, ')
          ..write('deletedAt: $deletedAt, ')
          ..write('syncState: $syncState, ')
          ..write('payloadJson: $payloadJson')
          ..write(')'))
        .toString();
  }

  @override
  int get hashCode => Object.hash(
      id, userId, version, updatedAt, deletedAt, syncState, payloadJson);
  @override
  bool operator ==(Object other) =>
      identical(this, other) ||
      (other is EventExceptionsDriftTableData &&
          other.id == this.id &&
          other.userId == this.userId &&
          other.version == this.version &&
          other.updatedAt == this.updatedAt &&
          other.deletedAt == this.deletedAt &&
          other.syncState == this.syncState &&
          other.payloadJson == this.payloadJson);
}

class EventExceptionsDriftTableCompanion
    extends UpdateCompanion<EventExceptionsDriftTableData> {
  final Value<String> id;
  final Value<String> userId;
  final Value<int> version;
  final Value<String> updatedAt;
  final Value<String?> deletedAt;
  final Value<String> syncState;
  final Value<String> payloadJson;
  final Value<int> rowid;
  const EventExceptionsDriftTableCompanion({
    this.id = const Value.absent(),
    this.userId = const Value.absent(),
    this.version = const Value.absent(),
    this.updatedAt = const Value.absent(),
    this.deletedAt = const Value.absent(),
    this.syncState = const Value.absent(),
    this.payloadJson = const Value.absent(),
    this.rowid = const Value.absent(),
  });
  EventExceptionsDriftTableCompanion.insert({
    required String id,
    required String userId,
    this.version = const Value.absent(),
    required String updatedAt,
    this.deletedAt = const Value.absent(),
    this.syncState = const Value.absent(),
    required String payloadJson,
    this.rowid = const Value.absent(),
  })  : id = Value(id),
        userId = Value(userId),
        updatedAt = Value(updatedAt),
        payloadJson = Value(payloadJson);
  static Insertable<EventExceptionsDriftTableData> custom({
    Expression<String>? id,
    Expression<String>? userId,
    Expression<int>? version,
    Expression<String>? updatedAt,
    Expression<String>? deletedAt,
    Expression<String>? syncState,
    Expression<String>? payloadJson,
    Expression<int>? rowid,
  }) {
    return RawValuesInsertable({
      if (id != null) 'id': id,
      if (userId != null) 'user_id': userId,
      if (version != null) 'version': version,
      if (updatedAt != null) 'updated_at': updatedAt,
      if (deletedAt != null) 'deleted_at': deletedAt,
      if (syncState != null) 'sync_state': syncState,
      if (payloadJson != null) 'payload_json': payloadJson,
      if (rowid != null) 'rowid': rowid,
    });
  }

  EventExceptionsDriftTableCompanion copyWith(
      {Value<String>? id,
      Value<String>? userId,
      Value<int>? version,
      Value<String>? updatedAt,
      Value<String?>? deletedAt,
      Value<String>? syncState,
      Value<String>? payloadJson,
      Value<int>? rowid}) {
    return EventExceptionsDriftTableCompanion(
      id: id ?? this.id,
      userId: userId ?? this.userId,
      version: version ?? this.version,
      updatedAt: updatedAt ?? this.updatedAt,
      deletedAt: deletedAt ?? this.deletedAt,
      syncState: syncState ?? this.syncState,
      payloadJson: payloadJson ?? this.payloadJson,
      rowid: rowid ?? this.rowid,
    );
  }

  @override
  Map<String, Expression> toColumns(bool nullToAbsent) {
    final map = <String, Expression>{};
    if (id.present) {
      map['id'] = Variable<String>(id.value);
    }
    if (userId.present) {
      map['user_id'] = Variable<String>(userId.value);
    }
    if (version.present) {
      map['version'] = Variable<int>(version.value);
    }
    if (updatedAt.present) {
      map['updated_at'] = Variable<String>(updatedAt.value);
    }
    if (deletedAt.present) {
      map['deleted_at'] = Variable<String>(deletedAt.value);
    }
    if (syncState.present) {
      map['sync_state'] = Variable<String>(syncState.value);
    }
    if (payloadJson.present) {
      map['payload_json'] = Variable<String>(payloadJson.value);
    }
    if (rowid.present) {
      map['rowid'] = Variable<int>(rowid.value);
    }
    return map;
  }

  @override
  String toString() {
    return (StringBuffer('EventExceptionsDriftTableCompanion(')
          ..write('id: $id, ')
          ..write('userId: $userId, ')
          ..write('version: $version, ')
          ..write('updatedAt: $updatedAt, ')
          ..write('deletedAt: $deletedAt, ')
          ..write('syncState: $syncState, ')
          ..write('payloadJson: $payloadJson, ')
          ..write('rowid: $rowid')
          ..write(')'))
        .toString();
  }
}

class $TimeBlocksDriftTableTable extends TimeBlocksDriftTable
    with TableInfo<$TimeBlocksDriftTableTable, TimeBlocksDriftTableData> {
  @override
  final GeneratedDatabase attachedDatabase;
  final String? _alias;
  $TimeBlocksDriftTableTable(this.attachedDatabase, [this._alias]);
  static const VerificationMeta _idMeta = const VerificationMeta('id');
  @override
  late final GeneratedColumn<String> id = GeneratedColumn<String>(
      'id', aliasedName, false,
      type: DriftSqlType.string, requiredDuringInsert: true);
  static const VerificationMeta _userIdMeta = const VerificationMeta('userId');
  @override
  late final GeneratedColumn<String> userId = GeneratedColumn<String>(
      'user_id', aliasedName, false,
      type: DriftSqlType.string, requiredDuringInsert: true);
  static const VerificationMeta _versionMeta =
      const VerificationMeta('version');
  @override
  late final GeneratedColumn<int> version = GeneratedColumn<int>(
      'version', aliasedName, false,
      type: DriftSqlType.int,
      requiredDuringInsert: false,
      defaultValue: const Constant(0));
  static const VerificationMeta _updatedAtMeta =
      const VerificationMeta('updatedAt');
  @override
  late final GeneratedColumn<String> updatedAt = GeneratedColumn<String>(
      'updated_at', aliasedName, false,
      type: DriftSqlType.string, requiredDuringInsert: true);
  static const VerificationMeta _deletedAtMeta =
      const VerificationMeta('deletedAt');
  @override
  late final GeneratedColumn<String> deletedAt = GeneratedColumn<String>(
      'deleted_at', aliasedName, true,
      type: DriftSqlType.string, requiredDuringInsert: false);
  static const VerificationMeta _syncStateMeta =
      const VerificationMeta('syncState');
  @override
  late final GeneratedColumn<String> syncState = GeneratedColumn<String>(
      'sync_state', aliasedName, false,
      type: DriftSqlType.string,
      requiredDuringInsert: false,
      defaultValue: const Constant('synced'));
  static const VerificationMeta _payloadJsonMeta =
      const VerificationMeta('payloadJson');
  @override
  late final GeneratedColumn<String> payloadJson = GeneratedColumn<String>(
      'payload_json', aliasedName, false,
      type: DriftSqlType.string, requiredDuringInsert: true);
  @override
  List<GeneratedColumn> get $columns =>
      [id, userId, version, updatedAt, deletedAt, syncState, payloadJson];
  @override
  String get aliasedName => _alias ?? actualTableName;
  @override
  String get actualTableName => $name;
  static const String $name = 'time_blocks';
  @override
  VerificationContext validateIntegrity(
      Insertable<TimeBlocksDriftTableData> instance,
      {bool isInserting = false}) {
    final context = VerificationContext();
    final data = instance.toColumns(true);
    if (data.containsKey('id')) {
      context.handle(_idMeta, id.isAcceptableOrUnknown(data['id']!, _idMeta));
    } else if (isInserting) {
      context.missing(_idMeta);
    }
    if (data.containsKey('user_id')) {
      context.handle(_userIdMeta,
          userId.isAcceptableOrUnknown(data['user_id']!, _userIdMeta));
    } else if (isInserting) {
      context.missing(_userIdMeta);
    }
    if (data.containsKey('version')) {
      context.handle(_versionMeta,
          version.isAcceptableOrUnknown(data['version']!, _versionMeta));
    }
    if (data.containsKey('updated_at')) {
      context.handle(_updatedAtMeta,
          updatedAt.isAcceptableOrUnknown(data['updated_at']!, _updatedAtMeta));
    } else if (isInserting) {
      context.missing(_updatedAtMeta);
    }
    if (data.containsKey('deleted_at')) {
      context.handle(_deletedAtMeta,
          deletedAt.isAcceptableOrUnknown(data['deleted_at']!, _deletedAtMeta));
    }
    if (data.containsKey('sync_state')) {
      context.handle(_syncStateMeta,
          syncState.isAcceptableOrUnknown(data['sync_state']!, _syncStateMeta));
    }
    if (data.containsKey('payload_json')) {
      context.handle(
          _payloadJsonMeta,
          payloadJson.isAcceptableOrUnknown(
              data['payload_json']!, _payloadJsonMeta));
    } else if (isInserting) {
      context.missing(_payloadJsonMeta);
    }
    return context;
  }

  @override
  Set<GeneratedColumn> get $primaryKey => {userId, id};
  @override
  TimeBlocksDriftTableData map(Map<String, dynamic> data,
      {String? tablePrefix}) {
    final effectivePrefix = tablePrefix != null ? '$tablePrefix.' : '';
    return TimeBlocksDriftTableData(
      id: attachedDatabase.typeMapping
          .read(DriftSqlType.string, data['${effectivePrefix}id'])!,
      userId: attachedDatabase.typeMapping
          .read(DriftSqlType.string, data['${effectivePrefix}user_id'])!,
      version: attachedDatabase.typeMapping
          .read(DriftSqlType.int, data['${effectivePrefix}version'])!,
      updatedAt: attachedDatabase.typeMapping
          .read(DriftSqlType.string, data['${effectivePrefix}updated_at'])!,
      deletedAt: attachedDatabase.typeMapping
          .read(DriftSqlType.string, data['${effectivePrefix}deleted_at']),
      syncState: attachedDatabase.typeMapping
          .read(DriftSqlType.string, data['${effectivePrefix}sync_state'])!,
      payloadJson: attachedDatabase.typeMapping
          .read(DriftSqlType.string, data['${effectivePrefix}payload_json'])!,
    );
  }

  @override
  $TimeBlocksDriftTableTable createAlias(String alias) {
    return $TimeBlocksDriftTableTable(attachedDatabase, alias);
  }
}

class TimeBlocksDriftTableData extends DataClass
    implements Insertable<TimeBlocksDriftTableData> {
  final String id;
  final String userId;
  final int version;
  final String updatedAt;
  final String? deletedAt;
  final String syncState;
  final String payloadJson;
  const TimeBlocksDriftTableData(
      {required this.id,
      required this.userId,
      required this.version,
      required this.updatedAt,
      this.deletedAt,
      required this.syncState,
      required this.payloadJson});
  @override
  Map<String, Expression> toColumns(bool nullToAbsent) {
    final map = <String, Expression>{};
    map['id'] = Variable<String>(id);
    map['user_id'] = Variable<String>(userId);
    map['version'] = Variable<int>(version);
    map['updated_at'] = Variable<String>(updatedAt);
    if (!nullToAbsent || deletedAt != null) {
      map['deleted_at'] = Variable<String>(deletedAt);
    }
    map['sync_state'] = Variable<String>(syncState);
    map['payload_json'] = Variable<String>(payloadJson);
    return map;
  }

  TimeBlocksDriftTableCompanion toCompanion(bool nullToAbsent) {
    return TimeBlocksDriftTableCompanion(
      id: Value(id),
      userId: Value(userId),
      version: Value(version),
      updatedAt: Value(updatedAt),
      deletedAt: deletedAt == null && nullToAbsent
          ? const Value.absent()
          : Value(deletedAt),
      syncState: Value(syncState),
      payloadJson: Value(payloadJson),
    );
  }

  factory TimeBlocksDriftTableData.fromJson(Map<String, dynamic> json,
      {ValueSerializer? serializer}) {
    serializer ??= driftRuntimeOptions.defaultSerializer;
    return TimeBlocksDriftTableData(
      id: serializer.fromJson<String>(json['id']),
      userId: serializer.fromJson<String>(json['userId']),
      version: serializer.fromJson<int>(json['version']),
      updatedAt: serializer.fromJson<String>(json['updatedAt']),
      deletedAt: serializer.fromJson<String?>(json['deletedAt']),
      syncState: serializer.fromJson<String>(json['syncState']),
      payloadJson: serializer.fromJson<String>(json['payloadJson']),
    );
  }
  @override
  Map<String, dynamic> toJson({ValueSerializer? serializer}) {
    serializer ??= driftRuntimeOptions.defaultSerializer;
    return <String, dynamic>{
      'id': serializer.toJson<String>(id),
      'userId': serializer.toJson<String>(userId),
      'version': serializer.toJson<int>(version),
      'updatedAt': serializer.toJson<String>(updatedAt),
      'deletedAt': serializer.toJson<String?>(deletedAt),
      'syncState': serializer.toJson<String>(syncState),
      'payloadJson': serializer.toJson<String>(payloadJson),
    };
  }

  TimeBlocksDriftTableData copyWith(
          {String? id,
          String? userId,
          int? version,
          String? updatedAt,
          Value<String?> deletedAt = const Value.absent(),
          String? syncState,
          String? payloadJson}) =>
      TimeBlocksDriftTableData(
        id: id ?? this.id,
        userId: userId ?? this.userId,
        version: version ?? this.version,
        updatedAt: updatedAt ?? this.updatedAt,
        deletedAt: deletedAt.present ? deletedAt.value : this.deletedAt,
        syncState: syncState ?? this.syncState,
        payloadJson: payloadJson ?? this.payloadJson,
      );
  TimeBlocksDriftTableData copyWithCompanion(
      TimeBlocksDriftTableCompanion data) {
    return TimeBlocksDriftTableData(
      id: data.id.present ? data.id.value : this.id,
      userId: data.userId.present ? data.userId.value : this.userId,
      version: data.version.present ? data.version.value : this.version,
      updatedAt: data.updatedAt.present ? data.updatedAt.value : this.updatedAt,
      deletedAt: data.deletedAt.present ? data.deletedAt.value : this.deletedAt,
      syncState: data.syncState.present ? data.syncState.value : this.syncState,
      payloadJson:
          data.payloadJson.present ? data.payloadJson.value : this.payloadJson,
    );
  }

  @override
  String toString() {
    return (StringBuffer('TimeBlocksDriftTableData(')
          ..write('id: $id, ')
          ..write('userId: $userId, ')
          ..write('version: $version, ')
          ..write('updatedAt: $updatedAt, ')
          ..write('deletedAt: $deletedAt, ')
          ..write('syncState: $syncState, ')
          ..write('payloadJson: $payloadJson')
          ..write(')'))
        .toString();
  }

  @override
  int get hashCode => Object.hash(
      id, userId, version, updatedAt, deletedAt, syncState, payloadJson);
  @override
  bool operator ==(Object other) =>
      identical(this, other) ||
      (other is TimeBlocksDriftTableData &&
          other.id == this.id &&
          other.userId == this.userId &&
          other.version == this.version &&
          other.updatedAt == this.updatedAt &&
          other.deletedAt == this.deletedAt &&
          other.syncState == this.syncState &&
          other.payloadJson == this.payloadJson);
}

class TimeBlocksDriftTableCompanion
    extends UpdateCompanion<TimeBlocksDriftTableData> {
  final Value<String> id;
  final Value<String> userId;
  final Value<int> version;
  final Value<String> updatedAt;
  final Value<String?> deletedAt;
  final Value<String> syncState;
  final Value<String> payloadJson;
  final Value<int> rowid;
  const TimeBlocksDriftTableCompanion({
    this.id = const Value.absent(),
    this.userId = const Value.absent(),
    this.version = const Value.absent(),
    this.updatedAt = const Value.absent(),
    this.deletedAt = const Value.absent(),
    this.syncState = const Value.absent(),
    this.payloadJson = const Value.absent(),
    this.rowid = const Value.absent(),
  });
  TimeBlocksDriftTableCompanion.insert({
    required String id,
    required String userId,
    this.version = const Value.absent(),
    required String updatedAt,
    this.deletedAt = const Value.absent(),
    this.syncState = const Value.absent(),
    required String payloadJson,
    this.rowid = const Value.absent(),
  })  : id = Value(id),
        userId = Value(userId),
        updatedAt = Value(updatedAt),
        payloadJson = Value(payloadJson);
  static Insertable<TimeBlocksDriftTableData> custom({
    Expression<String>? id,
    Expression<String>? userId,
    Expression<int>? version,
    Expression<String>? updatedAt,
    Expression<String>? deletedAt,
    Expression<String>? syncState,
    Expression<String>? payloadJson,
    Expression<int>? rowid,
  }) {
    return RawValuesInsertable({
      if (id != null) 'id': id,
      if (userId != null) 'user_id': userId,
      if (version != null) 'version': version,
      if (updatedAt != null) 'updated_at': updatedAt,
      if (deletedAt != null) 'deleted_at': deletedAt,
      if (syncState != null) 'sync_state': syncState,
      if (payloadJson != null) 'payload_json': payloadJson,
      if (rowid != null) 'rowid': rowid,
    });
  }

  TimeBlocksDriftTableCompanion copyWith(
      {Value<String>? id,
      Value<String>? userId,
      Value<int>? version,
      Value<String>? updatedAt,
      Value<String?>? deletedAt,
      Value<String>? syncState,
      Value<String>? payloadJson,
      Value<int>? rowid}) {
    return TimeBlocksDriftTableCompanion(
      id: id ?? this.id,
      userId: userId ?? this.userId,
      version: version ?? this.version,
      updatedAt: updatedAt ?? this.updatedAt,
      deletedAt: deletedAt ?? this.deletedAt,
      syncState: syncState ?? this.syncState,
      payloadJson: payloadJson ?? this.payloadJson,
      rowid: rowid ?? this.rowid,
    );
  }

  @override
  Map<String, Expression> toColumns(bool nullToAbsent) {
    final map = <String, Expression>{};
    if (id.present) {
      map['id'] = Variable<String>(id.value);
    }
    if (userId.present) {
      map['user_id'] = Variable<String>(userId.value);
    }
    if (version.present) {
      map['version'] = Variable<int>(version.value);
    }
    if (updatedAt.present) {
      map['updated_at'] = Variable<String>(updatedAt.value);
    }
    if (deletedAt.present) {
      map['deleted_at'] = Variable<String>(deletedAt.value);
    }
    if (syncState.present) {
      map['sync_state'] = Variable<String>(syncState.value);
    }
    if (payloadJson.present) {
      map['payload_json'] = Variable<String>(payloadJson.value);
    }
    if (rowid.present) {
      map['rowid'] = Variable<int>(rowid.value);
    }
    return map;
  }

  @override
  String toString() {
    return (StringBuffer('TimeBlocksDriftTableCompanion(')
          ..write('id: $id, ')
          ..write('userId: $userId, ')
          ..write('version: $version, ')
          ..write('updatedAt: $updatedAt, ')
          ..write('deletedAt: $deletedAt, ')
          ..write('syncState: $syncState, ')
          ..write('payloadJson: $payloadJson, ')
          ..write('rowid: $rowid')
          ..write(')'))
        .toString();
  }
}

class $RemindersDriftTableTable extends RemindersDriftTable
    with TableInfo<$RemindersDriftTableTable, RemindersDriftTableData> {
  @override
  final GeneratedDatabase attachedDatabase;
  final String? _alias;
  $RemindersDriftTableTable(this.attachedDatabase, [this._alias]);
  static const VerificationMeta _idMeta = const VerificationMeta('id');
  @override
  late final GeneratedColumn<String> id = GeneratedColumn<String>(
      'id', aliasedName, false,
      type: DriftSqlType.string, requiredDuringInsert: true);
  static const VerificationMeta _userIdMeta = const VerificationMeta('userId');
  @override
  late final GeneratedColumn<String> userId = GeneratedColumn<String>(
      'user_id', aliasedName, false,
      type: DriftSqlType.string, requiredDuringInsert: true);
  static const VerificationMeta _versionMeta =
      const VerificationMeta('version');
  @override
  late final GeneratedColumn<int> version = GeneratedColumn<int>(
      'version', aliasedName, false,
      type: DriftSqlType.int,
      requiredDuringInsert: false,
      defaultValue: const Constant(0));
  static const VerificationMeta _updatedAtMeta =
      const VerificationMeta('updatedAt');
  @override
  late final GeneratedColumn<String> updatedAt = GeneratedColumn<String>(
      'updated_at', aliasedName, false,
      type: DriftSqlType.string, requiredDuringInsert: true);
  static const VerificationMeta _deletedAtMeta =
      const VerificationMeta('deletedAt');
  @override
  late final GeneratedColumn<String> deletedAt = GeneratedColumn<String>(
      'deleted_at', aliasedName, true,
      type: DriftSqlType.string, requiredDuringInsert: false);
  static const VerificationMeta _syncStateMeta =
      const VerificationMeta('syncState');
  @override
  late final GeneratedColumn<String> syncState = GeneratedColumn<String>(
      'sync_state', aliasedName, false,
      type: DriftSqlType.string,
      requiredDuringInsert: false,
      defaultValue: const Constant('synced'));
  static const VerificationMeta _payloadJsonMeta =
      const VerificationMeta('payloadJson');
  @override
  late final GeneratedColumn<String> payloadJson = GeneratedColumn<String>(
      'payload_json', aliasedName, false,
      type: DriftSqlType.string, requiredDuringInsert: true);
  @override
  List<GeneratedColumn> get $columns =>
      [id, userId, version, updatedAt, deletedAt, syncState, payloadJson];
  @override
  String get aliasedName => _alias ?? actualTableName;
  @override
  String get actualTableName => $name;
  static const String $name = 'reminders';
  @override
  VerificationContext validateIntegrity(
      Insertable<RemindersDriftTableData> instance,
      {bool isInserting = false}) {
    final context = VerificationContext();
    final data = instance.toColumns(true);
    if (data.containsKey('id')) {
      context.handle(_idMeta, id.isAcceptableOrUnknown(data['id']!, _idMeta));
    } else if (isInserting) {
      context.missing(_idMeta);
    }
    if (data.containsKey('user_id')) {
      context.handle(_userIdMeta,
          userId.isAcceptableOrUnknown(data['user_id']!, _userIdMeta));
    } else if (isInserting) {
      context.missing(_userIdMeta);
    }
    if (data.containsKey('version')) {
      context.handle(_versionMeta,
          version.isAcceptableOrUnknown(data['version']!, _versionMeta));
    }
    if (data.containsKey('updated_at')) {
      context.handle(_updatedAtMeta,
          updatedAt.isAcceptableOrUnknown(data['updated_at']!, _updatedAtMeta));
    } else if (isInserting) {
      context.missing(_updatedAtMeta);
    }
    if (data.containsKey('deleted_at')) {
      context.handle(_deletedAtMeta,
          deletedAt.isAcceptableOrUnknown(data['deleted_at']!, _deletedAtMeta));
    }
    if (data.containsKey('sync_state')) {
      context.handle(_syncStateMeta,
          syncState.isAcceptableOrUnknown(data['sync_state']!, _syncStateMeta));
    }
    if (data.containsKey('payload_json')) {
      context.handle(
          _payloadJsonMeta,
          payloadJson.isAcceptableOrUnknown(
              data['payload_json']!, _payloadJsonMeta));
    } else if (isInserting) {
      context.missing(_payloadJsonMeta);
    }
    return context;
  }

  @override
  Set<GeneratedColumn> get $primaryKey => {userId, id};
  @override
  RemindersDriftTableData map(Map<String, dynamic> data,
      {String? tablePrefix}) {
    final effectivePrefix = tablePrefix != null ? '$tablePrefix.' : '';
    return RemindersDriftTableData(
      id: attachedDatabase.typeMapping
          .read(DriftSqlType.string, data['${effectivePrefix}id'])!,
      userId: attachedDatabase.typeMapping
          .read(DriftSqlType.string, data['${effectivePrefix}user_id'])!,
      version: attachedDatabase.typeMapping
          .read(DriftSqlType.int, data['${effectivePrefix}version'])!,
      updatedAt: attachedDatabase.typeMapping
          .read(DriftSqlType.string, data['${effectivePrefix}updated_at'])!,
      deletedAt: attachedDatabase.typeMapping
          .read(DriftSqlType.string, data['${effectivePrefix}deleted_at']),
      syncState: attachedDatabase.typeMapping
          .read(DriftSqlType.string, data['${effectivePrefix}sync_state'])!,
      payloadJson: attachedDatabase.typeMapping
          .read(DriftSqlType.string, data['${effectivePrefix}payload_json'])!,
    );
  }

  @override
  $RemindersDriftTableTable createAlias(String alias) {
    return $RemindersDriftTableTable(attachedDatabase, alias);
  }
}

class RemindersDriftTableData extends DataClass
    implements Insertable<RemindersDriftTableData> {
  final String id;
  final String userId;
  final int version;
  final String updatedAt;
  final String? deletedAt;
  final String syncState;
  final String payloadJson;
  const RemindersDriftTableData(
      {required this.id,
      required this.userId,
      required this.version,
      required this.updatedAt,
      this.deletedAt,
      required this.syncState,
      required this.payloadJson});
  @override
  Map<String, Expression> toColumns(bool nullToAbsent) {
    final map = <String, Expression>{};
    map['id'] = Variable<String>(id);
    map['user_id'] = Variable<String>(userId);
    map['version'] = Variable<int>(version);
    map['updated_at'] = Variable<String>(updatedAt);
    if (!nullToAbsent || deletedAt != null) {
      map['deleted_at'] = Variable<String>(deletedAt);
    }
    map['sync_state'] = Variable<String>(syncState);
    map['payload_json'] = Variable<String>(payloadJson);
    return map;
  }

  RemindersDriftTableCompanion toCompanion(bool nullToAbsent) {
    return RemindersDriftTableCompanion(
      id: Value(id),
      userId: Value(userId),
      version: Value(version),
      updatedAt: Value(updatedAt),
      deletedAt: deletedAt == null && nullToAbsent
          ? const Value.absent()
          : Value(deletedAt),
      syncState: Value(syncState),
      payloadJson: Value(payloadJson),
    );
  }

  factory RemindersDriftTableData.fromJson(Map<String, dynamic> json,
      {ValueSerializer? serializer}) {
    serializer ??= driftRuntimeOptions.defaultSerializer;
    return RemindersDriftTableData(
      id: serializer.fromJson<String>(json['id']),
      userId: serializer.fromJson<String>(json['userId']),
      version: serializer.fromJson<int>(json['version']),
      updatedAt: serializer.fromJson<String>(json['updatedAt']),
      deletedAt: serializer.fromJson<String?>(json['deletedAt']),
      syncState: serializer.fromJson<String>(json['syncState']),
      payloadJson: serializer.fromJson<String>(json['payloadJson']),
    );
  }
  @override
  Map<String, dynamic> toJson({ValueSerializer? serializer}) {
    serializer ??= driftRuntimeOptions.defaultSerializer;
    return <String, dynamic>{
      'id': serializer.toJson<String>(id),
      'userId': serializer.toJson<String>(userId),
      'version': serializer.toJson<int>(version),
      'updatedAt': serializer.toJson<String>(updatedAt),
      'deletedAt': serializer.toJson<String?>(deletedAt),
      'syncState': serializer.toJson<String>(syncState),
      'payloadJson': serializer.toJson<String>(payloadJson),
    };
  }

  RemindersDriftTableData copyWith(
          {String? id,
          String? userId,
          int? version,
          String? updatedAt,
          Value<String?> deletedAt = const Value.absent(),
          String? syncState,
          String? payloadJson}) =>
      RemindersDriftTableData(
        id: id ?? this.id,
        userId: userId ?? this.userId,
        version: version ?? this.version,
        updatedAt: updatedAt ?? this.updatedAt,
        deletedAt: deletedAt.present ? deletedAt.value : this.deletedAt,
        syncState: syncState ?? this.syncState,
        payloadJson: payloadJson ?? this.payloadJson,
      );
  RemindersDriftTableData copyWithCompanion(RemindersDriftTableCompanion data) {
    return RemindersDriftTableData(
      id: data.id.present ? data.id.value : this.id,
      userId: data.userId.present ? data.userId.value : this.userId,
      version: data.version.present ? data.version.value : this.version,
      updatedAt: data.updatedAt.present ? data.updatedAt.value : this.updatedAt,
      deletedAt: data.deletedAt.present ? data.deletedAt.value : this.deletedAt,
      syncState: data.syncState.present ? data.syncState.value : this.syncState,
      payloadJson:
          data.payloadJson.present ? data.payloadJson.value : this.payloadJson,
    );
  }

  @override
  String toString() {
    return (StringBuffer('RemindersDriftTableData(')
          ..write('id: $id, ')
          ..write('userId: $userId, ')
          ..write('version: $version, ')
          ..write('updatedAt: $updatedAt, ')
          ..write('deletedAt: $deletedAt, ')
          ..write('syncState: $syncState, ')
          ..write('payloadJson: $payloadJson')
          ..write(')'))
        .toString();
  }

  @override
  int get hashCode => Object.hash(
      id, userId, version, updatedAt, deletedAt, syncState, payloadJson);
  @override
  bool operator ==(Object other) =>
      identical(this, other) ||
      (other is RemindersDriftTableData &&
          other.id == this.id &&
          other.userId == this.userId &&
          other.version == this.version &&
          other.updatedAt == this.updatedAt &&
          other.deletedAt == this.deletedAt &&
          other.syncState == this.syncState &&
          other.payloadJson == this.payloadJson);
}

class RemindersDriftTableCompanion
    extends UpdateCompanion<RemindersDriftTableData> {
  final Value<String> id;
  final Value<String> userId;
  final Value<int> version;
  final Value<String> updatedAt;
  final Value<String?> deletedAt;
  final Value<String> syncState;
  final Value<String> payloadJson;
  final Value<int> rowid;
  const RemindersDriftTableCompanion({
    this.id = const Value.absent(),
    this.userId = const Value.absent(),
    this.version = const Value.absent(),
    this.updatedAt = const Value.absent(),
    this.deletedAt = const Value.absent(),
    this.syncState = const Value.absent(),
    this.payloadJson = const Value.absent(),
    this.rowid = const Value.absent(),
  });
  RemindersDriftTableCompanion.insert({
    required String id,
    required String userId,
    this.version = const Value.absent(),
    required String updatedAt,
    this.deletedAt = const Value.absent(),
    this.syncState = const Value.absent(),
    required String payloadJson,
    this.rowid = const Value.absent(),
  })  : id = Value(id),
        userId = Value(userId),
        updatedAt = Value(updatedAt),
        payloadJson = Value(payloadJson);
  static Insertable<RemindersDriftTableData> custom({
    Expression<String>? id,
    Expression<String>? userId,
    Expression<int>? version,
    Expression<String>? updatedAt,
    Expression<String>? deletedAt,
    Expression<String>? syncState,
    Expression<String>? payloadJson,
    Expression<int>? rowid,
  }) {
    return RawValuesInsertable({
      if (id != null) 'id': id,
      if (userId != null) 'user_id': userId,
      if (version != null) 'version': version,
      if (updatedAt != null) 'updated_at': updatedAt,
      if (deletedAt != null) 'deleted_at': deletedAt,
      if (syncState != null) 'sync_state': syncState,
      if (payloadJson != null) 'payload_json': payloadJson,
      if (rowid != null) 'rowid': rowid,
    });
  }

  RemindersDriftTableCompanion copyWith(
      {Value<String>? id,
      Value<String>? userId,
      Value<int>? version,
      Value<String>? updatedAt,
      Value<String?>? deletedAt,
      Value<String>? syncState,
      Value<String>? payloadJson,
      Value<int>? rowid}) {
    return RemindersDriftTableCompanion(
      id: id ?? this.id,
      userId: userId ?? this.userId,
      version: version ?? this.version,
      updatedAt: updatedAt ?? this.updatedAt,
      deletedAt: deletedAt ?? this.deletedAt,
      syncState: syncState ?? this.syncState,
      payloadJson: payloadJson ?? this.payloadJson,
      rowid: rowid ?? this.rowid,
    );
  }

  @override
  Map<String, Expression> toColumns(bool nullToAbsent) {
    final map = <String, Expression>{};
    if (id.present) {
      map['id'] = Variable<String>(id.value);
    }
    if (userId.present) {
      map['user_id'] = Variable<String>(userId.value);
    }
    if (version.present) {
      map['version'] = Variable<int>(version.value);
    }
    if (updatedAt.present) {
      map['updated_at'] = Variable<String>(updatedAt.value);
    }
    if (deletedAt.present) {
      map['deleted_at'] = Variable<String>(deletedAt.value);
    }
    if (syncState.present) {
      map['sync_state'] = Variable<String>(syncState.value);
    }
    if (payloadJson.present) {
      map['payload_json'] = Variable<String>(payloadJson.value);
    }
    if (rowid.present) {
      map['rowid'] = Variable<int>(rowid.value);
    }
    return map;
  }

  @override
  String toString() {
    return (StringBuffer('RemindersDriftTableCompanion(')
          ..write('id: $id, ')
          ..write('userId: $userId, ')
          ..write('version: $version, ')
          ..write('updatedAt: $updatedAt, ')
          ..write('deletedAt: $deletedAt, ')
          ..write('syncState: $syncState, ')
          ..write('payloadJson: $payloadJson, ')
          ..write('rowid: $rowid')
          ..write(')'))
        .toString();
  }
}

class $NotesDriftTableTable extends NotesDriftTable
    with TableInfo<$NotesDriftTableTable, NotesDriftTableData> {
  @override
  final GeneratedDatabase attachedDatabase;
  final String? _alias;
  $NotesDriftTableTable(this.attachedDatabase, [this._alias]);
  static const VerificationMeta _idMeta = const VerificationMeta('id');
  @override
  late final GeneratedColumn<String> id = GeneratedColumn<String>(
      'id', aliasedName, false,
      type: DriftSqlType.string, requiredDuringInsert: true);
  static const VerificationMeta _userIdMeta = const VerificationMeta('userId');
  @override
  late final GeneratedColumn<String> userId = GeneratedColumn<String>(
      'user_id', aliasedName, false,
      type: DriftSqlType.string, requiredDuringInsert: true);
  static const VerificationMeta _versionMeta =
      const VerificationMeta('version');
  @override
  late final GeneratedColumn<int> version = GeneratedColumn<int>(
      'version', aliasedName, false,
      type: DriftSqlType.int,
      requiredDuringInsert: false,
      defaultValue: const Constant(0));
  static const VerificationMeta _updatedAtMeta =
      const VerificationMeta('updatedAt');
  @override
  late final GeneratedColumn<String> updatedAt = GeneratedColumn<String>(
      'updated_at', aliasedName, false,
      type: DriftSqlType.string, requiredDuringInsert: true);
  static const VerificationMeta _deletedAtMeta =
      const VerificationMeta('deletedAt');
  @override
  late final GeneratedColumn<String> deletedAt = GeneratedColumn<String>(
      'deleted_at', aliasedName, true,
      type: DriftSqlType.string, requiredDuringInsert: false);
  static const VerificationMeta _syncStateMeta =
      const VerificationMeta('syncState');
  @override
  late final GeneratedColumn<String> syncState = GeneratedColumn<String>(
      'sync_state', aliasedName, false,
      type: DriftSqlType.string,
      requiredDuringInsert: false,
      defaultValue: const Constant('synced'));
  static const VerificationMeta _payloadJsonMeta =
      const VerificationMeta('payloadJson');
  @override
  late final GeneratedColumn<String> payloadJson = GeneratedColumn<String>(
      'payload_json', aliasedName, false,
      type: DriftSqlType.string, requiredDuringInsert: true);
  @override
  List<GeneratedColumn> get $columns =>
      [id, userId, version, updatedAt, deletedAt, syncState, payloadJson];
  @override
  String get aliasedName => _alias ?? actualTableName;
  @override
  String get actualTableName => $name;
  static const String $name = 'notes';
  @override
  VerificationContext validateIntegrity(
      Insertable<NotesDriftTableData> instance,
      {bool isInserting = false}) {
    final context = VerificationContext();
    final data = instance.toColumns(true);
    if (data.containsKey('id')) {
      context.handle(_idMeta, id.isAcceptableOrUnknown(data['id']!, _idMeta));
    } else if (isInserting) {
      context.missing(_idMeta);
    }
    if (data.containsKey('user_id')) {
      context.handle(_userIdMeta,
          userId.isAcceptableOrUnknown(data['user_id']!, _userIdMeta));
    } else if (isInserting) {
      context.missing(_userIdMeta);
    }
    if (data.containsKey('version')) {
      context.handle(_versionMeta,
          version.isAcceptableOrUnknown(data['version']!, _versionMeta));
    }
    if (data.containsKey('updated_at')) {
      context.handle(_updatedAtMeta,
          updatedAt.isAcceptableOrUnknown(data['updated_at']!, _updatedAtMeta));
    } else if (isInserting) {
      context.missing(_updatedAtMeta);
    }
    if (data.containsKey('deleted_at')) {
      context.handle(_deletedAtMeta,
          deletedAt.isAcceptableOrUnknown(data['deleted_at']!, _deletedAtMeta));
    }
    if (data.containsKey('sync_state')) {
      context.handle(_syncStateMeta,
          syncState.isAcceptableOrUnknown(data['sync_state']!, _syncStateMeta));
    }
    if (data.containsKey('payload_json')) {
      context.handle(
          _payloadJsonMeta,
          payloadJson.isAcceptableOrUnknown(
              data['payload_json']!, _payloadJsonMeta));
    } else if (isInserting) {
      context.missing(_payloadJsonMeta);
    }
    return context;
  }

  @override
  Set<GeneratedColumn> get $primaryKey => {userId, id};
  @override
  NotesDriftTableData map(Map<String, dynamic> data, {String? tablePrefix}) {
    final effectivePrefix = tablePrefix != null ? '$tablePrefix.' : '';
    return NotesDriftTableData(
      id: attachedDatabase.typeMapping
          .read(DriftSqlType.string, data['${effectivePrefix}id'])!,
      userId: attachedDatabase.typeMapping
          .read(DriftSqlType.string, data['${effectivePrefix}user_id'])!,
      version: attachedDatabase.typeMapping
          .read(DriftSqlType.int, data['${effectivePrefix}version'])!,
      updatedAt: attachedDatabase.typeMapping
          .read(DriftSqlType.string, data['${effectivePrefix}updated_at'])!,
      deletedAt: attachedDatabase.typeMapping
          .read(DriftSqlType.string, data['${effectivePrefix}deleted_at']),
      syncState: attachedDatabase.typeMapping
          .read(DriftSqlType.string, data['${effectivePrefix}sync_state'])!,
      payloadJson: attachedDatabase.typeMapping
          .read(DriftSqlType.string, data['${effectivePrefix}payload_json'])!,
    );
  }

  @override
  $NotesDriftTableTable createAlias(String alias) {
    return $NotesDriftTableTable(attachedDatabase, alias);
  }
}

class NotesDriftTableData extends DataClass
    implements Insertable<NotesDriftTableData> {
  final String id;
  final String userId;
  final int version;
  final String updatedAt;
  final String? deletedAt;
  final String syncState;
  final String payloadJson;
  const NotesDriftTableData(
      {required this.id,
      required this.userId,
      required this.version,
      required this.updatedAt,
      this.deletedAt,
      required this.syncState,
      required this.payloadJson});
  @override
  Map<String, Expression> toColumns(bool nullToAbsent) {
    final map = <String, Expression>{};
    map['id'] = Variable<String>(id);
    map['user_id'] = Variable<String>(userId);
    map['version'] = Variable<int>(version);
    map['updated_at'] = Variable<String>(updatedAt);
    if (!nullToAbsent || deletedAt != null) {
      map['deleted_at'] = Variable<String>(deletedAt);
    }
    map['sync_state'] = Variable<String>(syncState);
    map['payload_json'] = Variable<String>(payloadJson);
    return map;
  }

  NotesDriftTableCompanion toCompanion(bool nullToAbsent) {
    return NotesDriftTableCompanion(
      id: Value(id),
      userId: Value(userId),
      version: Value(version),
      updatedAt: Value(updatedAt),
      deletedAt: deletedAt == null && nullToAbsent
          ? const Value.absent()
          : Value(deletedAt),
      syncState: Value(syncState),
      payloadJson: Value(payloadJson),
    );
  }

  factory NotesDriftTableData.fromJson(Map<String, dynamic> json,
      {ValueSerializer? serializer}) {
    serializer ??= driftRuntimeOptions.defaultSerializer;
    return NotesDriftTableData(
      id: serializer.fromJson<String>(json['id']),
      userId: serializer.fromJson<String>(json['userId']),
      version: serializer.fromJson<int>(json['version']),
      updatedAt: serializer.fromJson<String>(json['updatedAt']),
      deletedAt: serializer.fromJson<String?>(json['deletedAt']),
      syncState: serializer.fromJson<String>(json['syncState']),
      payloadJson: serializer.fromJson<String>(json['payloadJson']),
    );
  }
  @override
  Map<String, dynamic> toJson({ValueSerializer? serializer}) {
    serializer ??= driftRuntimeOptions.defaultSerializer;
    return <String, dynamic>{
      'id': serializer.toJson<String>(id),
      'userId': serializer.toJson<String>(userId),
      'version': serializer.toJson<int>(version),
      'updatedAt': serializer.toJson<String>(updatedAt),
      'deletedAt': serializer.toJson<String?>(deletedAt),
      'syncState': serializer.toJson<String>(syncState),
      'payloadJson': serializer.toJson<String>(payloadJson),
    };
  }

  NotesDriftTableData copyWith(
          {String? id,
          String? userId,
          int? version,
          String? updatedAt,
          Value<String?> deletedAt = const Value.absent(),
          String? syncState,
          String? payloadJson}) =>
      NotesDriftTableData(
        id: id ?? this.id,
        userId: userId ?? this.userId,
        version: version ?? this.version,
        updatedAt: updatedAt ?? this.updatedAt,
        deletedAt: deletedAt.present ? deletedAt.value : this.deletedAt,
        syncState: syncState ?? this.syncState,
        payloadJson: payloadJson ?? this.payloadJson,
      );
  NotesDriftTableData copyWithCompanion(NotesDriftTableCompanion data) {
    return NotesDriftTableData(
      id: data.id.present ? data.id.value : this.id,
      userId: data.userId.present ? data.userId.value : this.userId,
      version: data.version.present ? data.version.value : this.version,
      updatedAt: data.updatedAt.present ? data.updatedAt.value : this.updatedAt,
      deletedAt: data.deletedAt.present ? data.deletedAt.value : this.deletedAt,
      syncState: data.syncState.present ? data.syncState.value : this.syncState,
      payloadJson:
          data.payloadJson.present ? data.payloadJson.value : this.payloadJson,
    );
  }

  @override
  String toString() {
    return (StringBuffer('NotesDriftTableData(')
          ..write('id: $id, ')
          ..write('userId: $userId, ')
          ..write('version: $version, ')
          ..write('updatedAt: $updatedAt, ')
          ..write('deletedAt: $deletedAt, ')
          ..write('syncState: $syncState, ')
          ..write('payloadJson: $payloadJson')
          ..write(')'))
        .toString();
  }

  @override
  int get hashCode => Object.hash(
      id, userId, version, updatedAt, deletedAt, syncState, payloadJson);
  @override
  bool operator ==(Object other) =>
      identical(this, other) ||
      (other is NotesDriftTableData &&
          other.id == this.id &&
          other.userId == this.userId &&
          other.version == this.version &&
          other.updatedAt == this.updatedAt &&
          other.deletedAt == this.deletedAt &&
          other.syncState == this.syncState &&
          other.payloadJson == this.payloadJson);
}

class NotesDriftTableCompanion extends UpdateCompanion<NotesDriftTableData> {
  final Value<String> id;
  final Value<String> userId;
  final Value<int> version;
  final Value<String> updatedAt;
  final Value<String?> deletedAt;
  final Value<String> syncState;
  final Value<String> payloadJson;
  final Value<int> rowid;
  const NotesDriftTableCompanion({
    this.id = const Value.absent(),
    this.userId = const Value.absent(),
    this.version = const Value.absent(),
    this.updatedAt = const Value.absent(),
    this.deletedAt = const Value.absent(),
    this.syncState = const Value.absent(),
    this.payloadJson = const Value.absent(),
    this.rowid = const Value.absent(),
  });
  NotesDriftTableCompanion.insert({
    required String id,
    required String userId,
    this.version = const Value.absent(),
    required String updatedAt,
    this.deletedAt = const Value.absent(),
    this.syncState = const Value.absent(),
    required String payloadJson,
    this.rowid = const Value.absent(),
  })  : id = Value(id),
        userId = Value(userId),
        updatedAt = Value(updatedAt),
        payloadJson = Value(payloadJson);
  static Insertable<NotesDriftTableData> custom({
    Expression<String>? id,
    Expression<String>? userId,
    Expression<int>? version,
    Expression<String>? updatedAt,
    Expression<String>? deletedAt,
    Expression<String>? syncState,
    Expression<String>? payloadJson,
    Expression<int>? rowid,
  }) {
    return RawValuesInsertable({
      if (id != null) 'id': id,
      if (userId != null) 'user_id': userId,
      if (version != null) 'version': version,
      if (updatedAt != null) 'updated_at': updatedAt,
      if (deletedAt != null) 'deleted_at': deletedAt,
      if (syncState != null) 'sync_state': syncState,
      if (payloadJson != null) 'payload_json': payloadJson,
      if (rowid != null) 'rowid': rowid,
    });
  }

  NotesDriftTableCompanion copyWith(
      {Value<String>? id,
      Value<String>? userId,
      Value<int>? version,
      Value<String>? updatedAt,
      Value<String?>? deletedAt,
      Value<String>? syncState,
      Value<String>? payloadJson,
      Value<int>? rowid}) {
    return NotesDriftTableCompanion(
      id: id ?? this.id,
      userId: userId ?? this.userId,
      version: version ?? this.version,
      updatedAt: updatedAt ?? this.updatedAt,
      deletedAt: deletedAt ?? this.deletedAt,
      syncState: syncState ?? this.syncState,
      payloadJson: payloadJson ?? this.payloadJson,
      rowid: rowid ?? this.rowid,
    );
  }

  @override
  Map<String, Expression> toColumns(bool nullToAbsent) {
    final map = <String, Expression>{};
    if (id.present) {
      map['id'] = Variable<String>(id.value);
    }
    if (userId.present) {
      map['user_id'] = Variable<String>(userId.value);
    }
    if (version.present) {
      map['version'] = Variable<int>(version.value);
    }
    if (updatedAt.present) {
      map['updated_at'] = Variable<String>(updatedAt.value);
    }
    if (deletedAt.present) {
      map['deleted_at'] = Variable<String>(deletedAt.value);
    }
    if (syncState.present) {
      map['sync_state'] = Variable<String>(syncState.value);
    }
    if (payloadJson.present) {
      map['payload_json'] = Variable<String>(payloadJson.value);
    }
    if (rowid.present) {
      map['rowid'] = Variable<int>(rowid.value);
    }
    return map;
  }

  @override
  String toString() {
    return (StringBuffer('NotesDriftTableCompanion(')
          ..write('id: $id, ')
          ..write('userId: $userId, ')
          ..write('version: $version, ')
          ..write('updatedAt: $updatedAt, ')
          ..write('deletedAt: $deletedAt, ')
          ..write('syncState: $syncState, ')
          ..write('payloadJson: $payloadJson, ')
          ..write('rowid: $rowid')
          ..write(')'))
        .toString();
  }
}

class $TagsDriftTableTable extends TagsDriftTable
    with TableInfo<$TagsDriftTableTable, TagsDriftTableData> {
  @override
  final GeneratedDatabase attachedDatabase;
  final String? _alias;
  $TagsDriftTableTable(this.attachedDatabase, [this._alias]);
  static const VerificationMeta _idMeta = const VerificationMeta('id');
  @override
  late final GeneratedColumn<String> id = GeneratedColumn<String>(
      'id', aliasedName, false,
      type: DriftSqlType.string, requiredDuringInsert: true);
  static const VerificationMeta _userIdMeta = const VerificationMeta('userId');
  @override
  late final GeneratedColumn<String> userId = GeneratedColumn<String>(
      'user_id', aliasedName, false,
      type: DriftSqlType.string, requiredDuringInsert: true);
  static const VerificationMeta _versionMeta =
      const VerificationMeta('version');
  @override
  late final GeneratedColumn<int> version = GeneratedColumn<int>(
      'version', aliasedName, false,
      type: DriftSqlType.int,
      requiredDuringInsert: false,
      defaultValue: const Constant(0));
  static const VerificationMeta _updatedAtMeta =
      const VerificationMeta('updatedAt');
  @override
  late final GeneratedColumn<String> updatedAt = GeneratedColumn<String>(
      'updated_at', aliasedName, false,
      type: DriftSqlType.string, requiredDuringInsert: true);
  static const VerificationMeta _deletedAtMeta =
      const VerificationMeta('deletedAt');
  @override
  late final GeneratedColumn<String> deletedAt = GeneratedColumn<String>(
      'deleted_at', aliasedName, true,
      type: DriftSqlType.string, requiredDuringInsert: false);
  static const VerificationMeta _syncStateMeta =
      const VerificationMeta('syncState');
  @override
  late final GeneratedColumn<String> syncState = GeneratedColumn<String>(
      'sync_state', aliasedName, false,
      type: DriftSqlType.string,
      requiredDuringInsert: false,
      defaultValue: const Constant('synced'));
  static const VerificationMeta _payloadJsonMeta =
      const VerificationMeta('payloadJson');
  @override
  late final GeneratedColumn<String> payloadJson = GeneratedColumn<String>(
      'payload_json', aliasedName, false,
      type: DriftSqlType.string, requiredDuringInsert: true);
  @override
  List<GeneratedColumn> get $columns =>
      [id, userId, version, updatedAt, deletedAt, syncState, payloadJson];
  @override
  String get aliasedName => _alias ?? actualTableName;
  @override
  String get actualTableName => $name;
  static const String $name = 'tags';
  @override
  VerificationContext validateIntegrity(Insertable<TagsDriftTableData> instance,
      {bool isInserting = false}) {
    final context = VerificationContext();
    final data = instance.toColumns(true);
    if (data.containsKey('id')) {
      context.handle(_idMeta, id.isAcceptableOrUnknown(data['id']!, _idMeta));
    } else if (isInserting) {
      context.missing(_idMeta);
    }
    if (data.containsKey('user_id')) {
      context.handle(_userIdMeta,
          userId.isAcceptableOrUnknown(data['user_id']!, _userIdMeta));
    } else if (isInserting) {
      context.missing(_userIdMeta);
    }
    if (data.containsKey('version')) {
      context.handle(_versionMeta,
          version.isAcceptableOrUnknown(data['version']!, _versionMeta));
    }
    if (data.containsKey('updated_at')) {
      context.handle(_updatedAtMeta,
          updatedAt.isAcceptableOrUnknown(data['updated_at']!, _updatedAtMeta));
    } else if (isInserting) {
      context.missing(_updatedAtMeta);
    }
    if (data.containsKey('deleted_at')) {
      context.handle(_deletedAtMeta,
          deletedAt.isAcceptableOrUnknown(data['deleted_at']!, _deletedAtMeta));
    }
    if (data.containsKey('sync_state')) {
      context.handle(_syncStateMeta,
          syncState.isAcceptableOrUnknown(data['sync_state']!, _syncStateMeta));
    }
    if (data.containsKey('payload_json')) {
      context.handle(
          _payloadJsonMeta,
          payloadJson.isAcceptableOrUnknown(
              data['payload_json']!, _payloadJsonMeta));
    } else if (isInserting) {
      context.missing(_payloadJsonMeta);
    }
    return context;
  }

  @override
  Set<GeneratedColumn> get $primaryKey => {userId, id};
  @override
  TagsDriftTableData map(Map<String, dynamic> data, {String? tablePrefix}) {
    final effectivePrefix = tablePrefix != null ? '$tablePrefix.' : '';
    return TagsDriftTableData(
      id: attachedDatabase.typeMapping
          .read(DriftSqlType.string, data['${effectivePrefix}id'])!,
      userId: attachedDatabase.typeMapping
          .read(DriftSqlType.string, data['${effectivePrefix}user_id'])!,
      version: attachedDatabase.typeMapping
          .read(DriftSqlType.int, data['${effectivePrefix}version'])!,
      updatedAt: attachedDatabase.typeMapping
          .read(DriftSqlType.string, data['${effectivePrefix}updated_at'])!,
      deletedAt: attachedDatabase.typeMapping
          .read(DriftSqlType.string, data['${effectivePrefix}deleted_at']),
      syncState: attachedDatabase.typeMapping
          .read(DriftSqlType.string, data['${effectivePrefix}sync_state'])!,
      payloadJson: attachedDatabase.typeMapping
          .read(DriftSqlType.string, data['${effectivePrefix}payload_json'])!,
    );
  }

  @override
  $TagsDriftTableTable createAlias(String alias) {
    return $TagsDriftTableTable(attachedDatabase, alias);
  }
}

class TagsDriftTableData extends DataClass
    implements Insertable<TagsDriftTableData> {
  final String id;
  final String userId;
  final int version;
  final String updatedAt;
  final String? deletedAt;
  final String syncState;
  final String payloadJson;
  const TagsDriftTableData(
      {required this.id,
      required this.userId,
      required this.version,
      required this.updatedAt,
      this.deletedAt,
      required this.syncState,
      required this.payloadJson});
  @override
  Map<String, Expression> toColumns(bool nullToAbsent) {
    final map = <String, Expression>{};
    map['id'] = Variable<String>(id);
    map['user_id'] = Variable<String>(userId);
    map['version'] = Variable<int>(version);
    map['updated_at'] = Variable<String>(updatedAt);
    if (!nullToAbsent || deletedAt != null) {
      map['deleted_at'] = Variable<String>(deletedAt);
    }
    map['sync_state'] = Variable<String>(syncState);
    map['payload_json'] = Variable<String>(payloadJson);
    return map;
  }

  TagsDriftTableCompanion toCompanion(bool nullToAbsent) {
    return TagsDriftTableCompanion(
      id: Value(id),
      userId: Value(userId),
      version: Value(version),
      updatedAt: Value(updatedAt),
      deletedAt: deletedAt == null && nullToAbsent
          ? const Value.absent()
          : Value(deletedAt),
      syncState: Value(syncState),
      payloadJson: Value(payloadJson),
    );
  }

  factory TagsDriftTableData.fromJson(Map<String, dynamic> json,
      {ValueSerializer? serializer}) {
    serializer ??= driftRuntimeOptions.defaultSerializer;
    return TagsDriftTableData(
      id: serializer.fromJson<String>(json['id']),
      userId: serializer.fromJson<String>(json['userId']),
      version: serializer.fromJson<int>(json['version']),
      updatedAt: serializer.fromJson<String>(json['updatedAt']),
      deletedAt: serializer.fromJson<String?>(json['deletedAt']),
      syncState: serializer.fromJson<String>(json['syncState']),
      payloadJson: serializer.fromJson<String>(json['payloadJson']),
    );
  }
  @override
  Map<String, dynamic> toJson({ValueSerializer? serializer}) {
    serializer ??= driftRuntimeOptions.defaultSerializer;
    return <String, dynamic>{
      'id': serializer.toJson<String>(id),
      'userId': serializer.toJson<String>(userId),
      'version': serializer.toJson<int>(version),
      'updatedAt': serializer.toJson<String>(updatedAt),
      'deletedAt': serializer.toJson<String?>(deletedAt),
      'syncState': serializer.toJson<String>(syncState),
      'payloadJson': serializer.toJson<String>(payloadJson),
    };
  }

  TagsDriftTableData copyWith(
          {String? id,
          String? userId,
          int? version,
          String? updatedAt,
          Value<String?> deletedAt = const Value.absent(),
          String? syncState,
          String? payloadJson}) =>
      TagsDriftTableData(
        id: id ?? this.id,
        userId: userId ?? this.userId,
        version: version ?? this.version,
        updatedAt: updatedAt ?? this.updatedAt,
        deletedAt: deletedAt.present ? deletedAt.value : this.deletedAt,
        syncState: syncState ?? this.syncState,
        payloadJson: payloadJson ?? this.payloadJson,
      );
  TagsDriftTableData copyWithCompanion(TagsDriftTableCompanion data) {
    return TagsDriftTableData(
      id: data.id.present ? data.id.value : this.id,
      userId: data.userId.present ? data.userId.value : this.userId,
      version: data.version.present ? data.version.value : this.version,
      updatedAt: data.updatedAt.present ? data.updatedAt.value : this.updatedAt,
      deletedAt: data.deletedAt.present ? data.deletedAt.value : this.deletedAt,
      syncState: data.syncState.present ? data.syncState.value : this.syncState,
      payloadJson:
          data.payloadJson.present ? data.payloadJson.value : this.payloadJson,
    );
  }

  @override
  String toString() {
    return (StringBuffer('TagsDriftTableData(')
          ..write('id: $id, ')
          ..write('userId: $userId, ')
          ..write('version: $version, ')
          ..write('updatedAt: $updatedAt, ')
          ..write('deletedAt: $deletedAt, ')
          ..write('syncState: $syncState, ')
          ..write('payloadJson: $payloadJson')
          ..write(')'))
        .toString();
  }

  @override
  int get hashCode => Object.hash(
      id, userId, version, updatedAt, deletedAt, syncState, payloadJson);
  @override
  bool operator ==(Object other) =>
      identical(this, other) ||
      (other is TagsDriftTableData &&
          other.id == this.id &&
          other.userId == this.userId &&
          other.version == this.version &&
          other.updatedAt == this.updatedAt &&
          other.deletedAt == this.deletedAt &&
          other.syncState == this.syncState &&
          other.payloadJson == this.payloadJson);
}

class TagsDriftTableCompanion extends UpdateCompanion<TagsDriftTableData> {
  final Value<String> id;
  final Value<String> userId;
  final Value<int> version;
  final Value<String> updatedAt;
  final Value<String?> deletedAt;
  final Value<String> syncState;
  final Value<String> payloadJson;
  final Value<int> rowid;
  const TagsDriftTableCompanion({
    this.id = const Value.absent(),
    this.userId = const Value.absent(),
    this.version = const Value.absent(),
    this.updatedAt = const Value.absent(),
    this.deletedAt = const Value.absent(),
    this.syncState = const Value.absent(),
    this.payloadJson = const Value.absent(),
    this.rowid = const Value.absent(),
  });
  TagsDriftTableCompanion.insert({
    required String id,
    required String userId,
    this.version = const Value.absent(),
    required String updatedAt,
    this.deletedAt = const Value.absent(),
    this.syncState = const Value.absent(),
    required String payloadJson,
    this.rowid = const Value.absent(),
  })  : id = Value(id),
        userId = Value(userId),
        updatedAt = Value(updatedAt),
        payloadJson = Value(payloadJson);
  static Insertable<TagsDriftTableData> custom({
    Expression<String>? id,
    Expression<String>? userId,
    Expression<int>? version,
    Expression<String>? updatedAt,
    Expression<String>? deletedAt,
    Expression<String>? syncState,
    Expression<String>? payloadJson,
    Expression<int>? rowid,
  }) {
    return RawValuesInsertable({
      if (id != null) 'id': id,
      if (userId != null) 'user_id': userId,
      if (version != null) 'version': version,
      if (updatedAt != null) 'updated_at': updatedAt,
      if (deletedAt != null) 'deleted_at': deletedAt,
      if (syncState != null) 'sync_state': syncState,
      if (payloadJson != null) 'payload_json': payloadJson,
      if (rowid != null) 'rowid': rowid,
    });
  }

  TagsDriftTableCompanion copyWith(
      {Value<String>? id,
      Value<String>? userId,
      Value<int>? version,
      Value<String>? updatedAt,
      Value<String?>? deletedAt,
      Value<String>? syncState,
      Value<String>? payloadJson,
      Value<int>? rowid}) {
    return TagsDriftTableCompanion(
      id: id ?? this.id,
      userId: userId ?? this.userId,
      version: version ?? this.version,
      updatedAt: updatedAt ?? this.updatedAt,
      deletedAt: deletedAt ?? this.deletedAt,
      syncState: syncState ?? this.syncState,
      payloadJson: payloadJson ?? this.payloadJson,
      rowid: rowid ?? this.rowid,
    );
  }

  @override
  Map<String, Expression> toColumns(bool nullToAbsent) {
    final map = <String, Expression>{};
    if (id.present) {
      map['id'] = Variable<String>(id.value);
    }
    if (userId.present) {
      map['user_id'] = Variable<String>(userId.value);
    }
    if (version.present) {
      map['version'] = Variable<int>(version.value);
    }
    if (updatedAt.present) {
      map['updated_at'] = Variable<String>(updatedAt.value);
    }
    if (deletedAt.present) {
      map['deleted_at'] = Variable<String>(deletedAt.value);
    }
    if (syncState.present) {
      map['sync_state'] = Variable<String>(syncState.value);
    }
    if (payloadJson.present) {
      map['payload_json'] = Variable<String>(payloadJson.value);
    }
    if (rowid.present) {
      map['rowid'] = Variable<int>(rowid.value);
    }
    return map;
  }

  @override
  String toString() {
    return (StringBuffer('TagsDriftTableCompanion(')
          ..write('id: $id, ')
          ..write('userId: $userId, ')
          ..write('version: $version, ')
          ..write('updatedAt: $updatedAt, ')
          ..write('deletedAt: $deletedAt, ')
          ..write('syncState: $syncState, ')
          ..write('payloadJson: $payloadJson, ')
          ..write('rowid: $rowid')
          ..write(')'))
        .toString();
  }
}

class $NoteTagsDriftTableTable extends NoteTagsDriftTable
    with TableInfo<$NoteTagsDriftTableTable, NoteTagsDriftTableData> {
  @override
  final GeneratedDatabase attachedDatabase;
  final String? _alias;
  $NoteTagsDriftTableTable(this.attachedDatabase, [this._alias]);
  static const VerificationMeta _idMeta = const VerificationMeta('id');
  @override
  late final GeneratedColumn<String> id = GeneratedColumn<String>(
      'id', aliasedName, false,
      type: DriftSqlType.string, requiredDuringInsert: true);
  static const VerificationMeta _userIdMeta = const VerificationMeta('userId');
  @override
  late final GeneratedColumn<String> userId = GeneratedColumn<String>(
      'user_id', aliasedName, false,
      type: DriftSqlType.string, requiredDuringInsert: true);
  static const VerificationMeta _versionMeta =
      const VerificationMeta('version');
  @override
  late final GeneratedColumn<int> version = GeneratedColumn<int>(
      'version', aliasedName, false,
      type: DriftSqlType.int,
      requiredDuringInsert: false,
      defaultValue: const Constant(0));
  static const VerificationMeta _updatedAtMeta =
      const VerificationMeta('updatedAt');
  @override
  late final GeneratedColumn<String> updatedAt = GeneratedColumn<String>(
      'updated_at', aliasedName, false,
      type: DriftSqlType.string, requiredDuringInsert: true);
  static const VerificationMeta _deletedAtMeta =
      const VerificationMeta('deletedAt');
  @override
  late final GeneratedColumn<String> deletedAt = GeneratedColumn<String>(
      'deleted_at', aliasedName, true,
      type: DriftSqlType.string, requiredDuringInsert: false);
  static const VerificationMeta _syncStateMeta =
      const VerificationMeta('syncState');
  @override
  late final GeneratedColumn<String> syncState = GeneratedColumn<String>(
      'sync_state', aliasedName, false,
      type: DriftSqlType.string,
      requiredDuringInsert: false,
      defaultValue: const Constant('synced'));
  static const VerificationMeta _payloadJsonMeta =
      const VerificationMeta('payloadJson');
  @override
  late final GeneratedColumn<String> payloadJson = GeneratedColumn<String>(
      'payload_json', aliasedName, false,
      type: DriftSqlType.string, requiredDuringInsert: true);
  @override
  List<GeneratedColumn> get $columns =>
      [id, userId, version, updatedAt, deletedAt, syncState, payloadJson];
  @override
  String get aliasedName => _alias ?? actualTableName;
  @override
  String get actualTableName => $name;
  static const String $name = 'note_tags';
  @override
  VerificationContext validateIntegrity(
      Insertable<NoteTagsDriftTableData> instance,
      {bool isInserting = false}) {
    final context = VerificationContext();
    final data = instance.toColumns(true);
    if (data.containsKey('id')) {
      context.handle(_idMeta, id.isAcceptableOrUnknown(data['id']!, _idMeta));
    } else if (isInserting) {
      context.missing(_idMeta);
    }
    if (data.containsKey('user_id')) {
      context.handle(_userIdMeta,
          userId.isAcceptableOrUnknown(data['user_id']!, _userIdMeta));
    } else if (isInserting) {
      context.missing(_userIdMeta);
    }
    if (data.containsKey('version')) {
      context.handle(_versionMeta,
          version.isAcceptableOrUnknown(data['version']!, _versionMeta));
    }
    if (data.containsKey('updated_at')) {
      context.handle(_updatedAtMeta,
          updatedAt.isAcceptableOrUnknown(data['updated_at']!, _updatedAtMeta));
    } else if (isInserting) {
      context.missing(_updatedAtMeta);
    }
    if (data.containsKey('deleted_at')) {
      context.handle(_deletedAtMeta,
          deletedAt.isAcceptableOrUnknown(data['deleted_at']!, _deletedAtMeta));
    }
    if (data.containsKey('sync_state')) {
      context.handle(_syncStateMeta,
          syncState.isAcceptableOrUnknown(data['sync_state']!, _syncStateMeta));
    }
    if (data.containsKey('payload_json')) {
      context.handle(
          _payloadJsonMeta,
          payloadJson.isAcceptableOrUnknown(
              data['payload_json']!, _payloadJsonMeta));
    } else if (isInserting) {
      context.missing(_payloadJsonMeta);
    }
    return context;
  }

  @override
  Set<GeneratedColumn> get $primaryKey => {userId, id};
  @override
  NoteTagsDriftTableData map(Map<String, dynamic> data, {String? tablePrefix}) {
    final effectivePrefix = tablePrefix != null ? '$tablePrefix.' : '';
    return NoteTagsDriftTableData(
      id: attachedDatabase.typeMapping
          .read(DriftSqlType.string, data['${effectivePrefix}id'])!,
      userId: attachedDatabase.typeMapping
          .read(DriftSqlType.string, data['${effectivePrefix}user_id'])!,
      version: attachedDatabase.typeMapping
          .read(DriftSqlType.int, data['${effectivePrefix}version'])!,
      updatedAt: attachedDatabase.typeMapping
          .read(DriftSqlType.string, data['${effectivePrefix}updated_at'])!,
      deletedAt: attachedDatabase.typeMapping
          .read(DriftSqlType.string, data['${effectivePrefix}deleted_at']),
      syncState: attachedDatabase.typeMapping
          .read(DriftSqlType.string, data['${effectivePrefix}sync_state'])!,
      payloadJson: attachedDatabase.typeMapping
          .read(DriftSqlType.string, data['${effectivePrefix}payload_json'])!,
    );
  }

  @override
  $NoteTagsDriftTableTable createAlias(String alias) {
    return $NoteTagsDriftTableTable(attachedDatabase, alias);
  }
}

class NoteTagsDriftTableData extends DataClass
    implements Insertable<NoteTagsDriftTableData> {
  final String id;
  final String userId;
  final int version;
  final String updatedAt;
  final String? deletedAt;
  final String syncState;
  final String payloadJson;
  const NoteTagsDriftTableData(
      {required this.id,
      required this.userId,
      required this.version,
      required this.updatedAt,
      this.deletedAt,
      required this.syncState,
      required this.payloadJson});
  @override
  Map<String, Expression> toColumns(bool nullToAbsent) {
    final map = <String, Expression>{};
    map['id'] = Variable<String>(id);
    map['user_id'] = Variable<String>(userId);
    map['version'] = Variable<int>(version);
    map['updated_at'] = Variable<String>(updatedAt);
    if (!nullToAbsent || deletedAt != null) {
      map['deleted_at'] = Variable<String>(deletedAt);
    }
    map['sync_state'] = Variable<String>(syncState);
    map['payload_json'] = Variable<String>(payloadJson);
    return map;
  }

  NoteTagsDriftTableCompanion toCompanion(bool nullToAbsent) {
    return NoteTagsDriftTableCompanion(
      id: Value(id),
      userId: Value(userId),
      version: Value(version),
      updatedAt: Value(updatedAt),
      deletedAt: deletedAt == null && nullToAbsent
          ? const Value.absent()
          : Value(deletedAt),
      syncState: Value(syncState),
      payloadJson: Value(payloadJson),
    );
  }

  factory NoteTagsDriftTableData.fromJson(Map<String, dynamic> json,
      {ValueSerializer? serializer}) {
    serializer ??= driftRuntimeOptions.defaultSerializer;
    return NoteTagsDriftTableData(
      id: serializer.fromJson<String>(json['id']),
      userId: serializer.fromJson<String>(json['userId']),
      version: serializer.fromJson<int>(json['version']),
      updatedAt: serializer.fromJson<String>(json['updatedAt']),
      deletedAt: serializer.fromJson<String?>(json['deletedAt']),
      syncState: serializer.fromJson<String>(json['syncState']),
      payloadJson: serializer.fromJson<String>(json['payloadJson']),
    );
  }
  @override
  Map<String, dynamic> toJson({ValueSerializer? serializer}) {
    serializer ??= driftRuntimeOptions.defaultSerializer;
    return <String, dynamic>{
      'id': serializer.toJson<String>(id),
      'userId': serializer.toJson<String>(userId),
      'version': serializer.toJson<int>(version),
      'updatedAt': serializer.toJson<String>(updatedAt),
      'deletedAt': serializer.toJson<String?>(deletedAt),
      'syncState': serializer.toJson<String>(syncState),
      'payloadJson': serializer.toJson<String>(payloadJson),
    };
  }

  NoteTagsDriftTableData copyWith(
          {String? id,
          String? userId,
          int? version,
          String? updatedAt,
          Value<String?> deletedAt = const Value.absent(),
          String? syncState,
          String? payloadJson}) =>
      NoteTagsDriftTableData(
        id: id ?? this.id,
        userId: userId ?? this.userId,
        version: version ?? this.version,
        updatedAt: updatedAt ?? this.updatedAt,
        deletedAt: deletedAt.present ? deletedAt.value : this.deletedAt,
        syncState: syncState ?? this.syncState,
        payloadJson: payloadJson ?? this.payloadJson,
      );
  NoteTagsDriftTableData copyWithCompanion(NoteTagsDriftTableCompanion data) {
    return NoteTagsDriftTableData(
      id: data.id.present ? data.id.value : this.id,
      userId: data.userId.present ? data.userId.value : this.userId,
      version: data.version.present ? data.version.value : this.version,
      updatedAt: data.updatedAt.present ? data.updatedAt.value : this.updatedAt,
      deletedAt: data.deletedAt.present ? data.deletedAt.value : this.deletedAt,
      syncState: data.syncState.present ? data.syncState.value : this.syncState,
      payloadJson:
          data.payloadJson.present ? data.payloadJson.value : this.payloadJson,
    );
  }

  @override
  String toString() {
    return (StringBuffer('NoteTagsDriftTableData(')
          ..write('id: $id, ')
          ..write('userId: $userId, ')
          ..write('version: $version, ')
          ..write('updatedAt: $updatedAt, ')
          ..write('deletedAt: $deletedAt, ')
          ..write('syncState: $syncState, ')
          ..write('payloadJson: $payloadJson')
          ..write(')'))
        .toString();
  }

  @override
  int get hashCode => Object.hash(
      id, userId, version, updatedAt, deletedAt, syncState, payloadJson);
  @override
  bool operator ==(Object other) =>
      identical(this, other) ||
      (other is NoteTagsDriftTableData &&
          other.id == this.id &&
          other.userId == this.userId &&
          other.version == this.version &&
          other.updatedAt == this.updatedAt &&
          other.deletedAt == this.deletedAt &&
          other.syncState == this.syncState &&
          other.payloadJson == this.payloadJson);
}

class NoteTagsDriftTableCompanion
    extends UpdateCompanion<NoteTagsDriftTableData> {
  final Value<String> id;
  final Value<String> userId;
  final Value<int> version;
  final Value<String> updatedAt;
  final Value<String?> deletedAt;
  final Value<String> syncState;
  final Value<String> payloadJson;
  final Value<int> rowid;
  const NoteTagsDriftTableCompanion({
    this.id = const Value.absent(),
    this.userId = const Value.absent(),
    this.version = const Value.absent(),
    this.updatedAt = const Value.absent(),
    this.deletedAt = const Value.absent(),
    this.syncState = const Value.absent(),
    this.payloadJson = const Value.absent(),
    this.rowid = const Value.absent(),
  });
  NoteTagsDriftTableCompanion.insert({
    required String id,
    required String userId,
    this.version = const Value.absent(),
    required String updatedAt,
    this.deletedAt = const Value.absent(),
    this.syncState = const Value.absent(),
    required String payloadJson,
    this.rowid = const Value.absent(),
  })  : id = Value(id),
        userId = Value(userId),
        updatedAt = Value(updatedAt),
        payloadJson = Value(payloadJson);
  static Insertable<NoteTagsDriftTableData> custom({
    Expression<String>? id,
    Expression<String>? userId,
    Expression<int>? version,
    Expression<String>? updatedAt,
    Expression<String>? deletedAt,
    Expression<String>? syncState,
    Expression<String>? payloadJson,
    Expression<int>? rowid,
  }) {
    return RawValuesInsertable({
      if (id != null) 'id': id,
      if (userId != null) 'user_id': userId,
      if (version != null) 'version': version,
      if (updatedAt != null) 'updated_at': updatedAt,
      if (deletedAt != null) 'deleted_at': deletedAt,
      if (syncState != null) 'sync_state': syncState,
      if (payloadJson != null) 'payload_json': payloadJson,
      if (rowid != null) 'rowid': rowid,
    });
  }

  NoteTagsDriftTableCompanion copyWith(
      {Value<String>? id,
      Value<String>? userId,
      Value<int>? version,
      Value<String>? updatedAt,
      Value<String?>? deletedAt,
      Value<String>? syncState,
      Value<String>? payloadJson,
      Value<int>? rowid}) {
    return NoteTagsDriftTableCompanion(
      id: id ?? this.id,
      userId: userId ?? this.userId,
      version: version ?? this.version,
      updatedAt: updatedAt ?? this.updatedAt,
      deletedAt: deletedAt ?? this.deletedAt,
      syncState: syncState ?? this.syncState,
      payloadJson: payloadJson ?? this.payloadJson,
      rowid: rowid ?? this.rowid,
    );
  }

  @override
  Map<String, Expression> toColumns(bool nullToAbsent) {
    final map = <String, Expression>{};
    if (id.present) {
      map['id'] = Variable<String>(id.value);
    }
    if (userId.present) {
      map['user_id'] = Variable<String>(userId.value);
    }
    if (version.present) {
      map['version'] = Variable<int>(version.value);
    }
    if (updatedAt.present) {
      map['updated_at'] = Variable<String>(updatedAt.value);
    }
    if (deletedAt.present) {
      map['deleted_at'] = Variable<String>(deletedAt.value);
    }
    if (syncState.present) {
      map['sync_state'] = Variable<String>(syncState.value);
    }
    if (payloadJson.present) {
      map['payload_json'] = Variable<String>(payloadJson.value);
    }
    if (rowid.present) {
      map['rowid'] = Variable<int>(rowid.value);
    }
    return map;
  }

  @override
  String toString() {
    return (StringBuffer('NoteTagsDriftTableCompanion(')
          ..write('id: $id, ')
          ..write('userId: $userId, ')
          ..write('version: $version, ')
          ..write('updatedAt: $updatedAt, ')
          ..write('deletedAt: $deletedAt, ')
          ..write('syncState: $syncState, ')
          ..write('payloadJson: $payloadJson, ')
          ..write('rowid: $rowid')
          ..write(')'))
        .toString();
  }
}

class $NoteLinksDriftTableTable extends NoteLinksDriftTable
    with TableInfo<$NoteLinksDriftTableTable, NoteLinksDriftTableData> {
  @override
  final GeneratedDatabase attachedDatabase;
  final String? _alias;
  $NoteLinksDriftTableTable(this.attachedDatabase, [this._alias]);
  static const VerificationMeta _idMeta = const VerificationMeta('id');
  @override
  late final GeneratedColumn<String> id = GeneratedColumn<String>(
      'id', aliasedName, false,
      type: DriftSqlType.string, requiredDuringInsert: true);
  static const VerificationMeta _userIdMeta = const VerificationMeta('userId');
  @override
  late final GeneratedColumn<String> userId = GeneratedColumn<String>(
      'user_id', aliasedName, false,
      type: DriftSqlType.string, requiredDuringInsert: true);
  static const VerificationMeta _versionMeta =
      const VerificationMeta('version');
  @override
  late final GeneratedColumn<int> version = GeneratedColumn<int>(
      'version', aliasedName, false,
      type: DriftSqlType.int,
      requiredDuringInsert: false,
      defaultValue: const Constant(0));
  static const VerificationMeta _updatedAtMeta =
      const VerificationMeta('updatedAt');
  @override
  late final GeneratedColumn<String> updatedAt = GeneratedColumn<String>(
      'updated_at', aliasedName, false,
      type: DriftSqlType.string, requiredDuringInsert: true);
  static const VerificationMeta _deletedAtMeta =
      const VerificationMeta('deletedAt');
  @override
  late final GeneratedColumn<String> deletedAt = GeneratedColumn<String>(
      'deleted_at', aliasedName, true,
      type: DriftSqlType.string, requiredDuringInsert: false);
  static const VerificationMeta _syncStateMeta =
      const VerificationMeta('syncState');
  @override
  late final GeneratedColumn<String> syncState = GeneratedColumn<String>(
      'sync_state', aliasedName, false,
      type: DriftSqlType.string,
      requiredDuringInsert: false,
      defaultValue: const Constant('synced'));
  static const VerificationMeta _payloadJsonMeta =
      const VerificationMeta('payloadJson');
  @override
  late final GeneratedColumn<String> payloadJson = GeneratedColumn<String>(
      'payload_json', aliasedName, false,
      type: DriftSqlType.string, requiredDuringInsert: true);
  @override
  List<GeneratedColumn> get $columns =>
      [id, userId, version, updatedAt, deletedAt, syncState, payloadJson];
  @override
  String get aliasedName => _alias ?? actualTableName;
  @override
  String get actualTableName => $name;
  static const String $name = 'note_links';
  @override
  VerificationContext validateIntegrity(
      Insertable<NoteLinksDriftTableData> instance,
      {bool isInserting = false}) {
    final context = VerificationContext();
    final data = instance.toColumns(true);
    if (data.containsKey('id')) {
      context.handle(_idMeta, id.isAcceptableOrUnknown(data['id']!, _idMeta));
    } else if (isInserting) {
      context.missing(_idMeta);
    }
    if (data.containsKey('user_id')) {
      context.handle(_userIdMeta,
          userId.isAcceptableOrUnknown(data['user_id']!, _userIdMeta));
    } else if (isInserting) {
      context.missing(_userIdMeta);
    }
    if (data.containsKey('version')) {
      context.handle(_versionMeta,
          version.isAcceptableOrUnknown(data['version']!, _versionMeta));
    }
    if (data.containsKey('updated_at')) {
      context.handle(_updatedAtMeta,
          updatedAt.isAcceptableOrUnknown(data['updated_at']!, _updatedAtMeta));
    } else if (isInserting) {
      context.missing(_updatedAtMeta);
    }
    if (data.containsKey('deleted_at')) {
      context.handle(_deletedAtMeta,
          deletedAt.isAcceptableOrUnknown(data['deleted_at']!, _deletedAtMeta));
    }
    if (data.containsKey('sync_state')) {
      context.handle(_syncStateMeta,
          syncState.isAcceptableOrUnknown(data['sync_state']!, _syncStateMeta));
    }
    if (data.containsKey('payload_json')) {
      context.handle(
          _payloadJsonMeta,
          payloadJson.isAcceptableOrUnknown(
              data['payload_json']!, _payloadJsonMeta));
    } else if (isInserting) {
      context.missing(_payloadJsonMeta);
    }
    return context;
  }

  @override
  Set<GeneratedColumn> get $primaryKey => {userId, id};
  @override
  NoteLinksDriftTableData map(Map<String, dynamic> data,
      {String? tablePrefix}) {
    final effectivePrefix = tablePrefix != null ? '$tablePrefix.' : '';
    return NoteLinksDriftTableData(
      id: attachedDatabase.typeMapping
          .read(DriftSqlType.string, data['${effectivePrefix}id'])!,
      userId: attachedDatabase.typeMapping
          .read(DriftSqlType.string, data['${effectivePrefix}user_id'])!,
      version: attachedDatabase.typeMapping
          .read(DriftSqlType.int, data['${effectivePrefix}version'])!,
      updatedAt: attachedDatabase.typeMapping
          .read(DriftSqlType.string, data['${effectivePrefix}updated_at'])!,
      deletedAt: attachedDatabase.typeMapping
          .read(DriftSqlType.string, data['${effectivePrefix}deleted_at']),
      syncState: attachedDatabase.typeMapping
          .read(DriftSqlType.string, data['${effectivePrefix}sync_state'])!,
      payloadJson: attachedDatabase.typeMapping
          .read(DriftSqlType.string, data['${effectivePrefix}payload_json'])!,
    );
  }

  @override
  $NoteLinksDriftTableTable createAlias(String alias) {
    return $NoteLinksDriftTableTable(attachedDatabase, alias);
  }
}

class NoteLinksDriftTableData extends DataClass
    implements Insertable<NoteLinksDriftTableData> {
  final String id;
  final String userId;
  final int version;
  final String updatedAt;
  final String? deletedAt;
  final String syncState;
  final String payloadJson;
  const NoteLinksDriftTableData(
      {required this.id,
      required this.userId,
      required this.version,
      required this.updatedAt,
      this.deletedAt,
      required this.syncState,
      required this.payloadJson});
  @override
  Map<String, Expression> toColumns(bool nullToAbsent) {
    final map = <String, Expression>{};
    map['id'] = Variable<String>(id);
    map['user_id'] = Variable<String>(userId);
    map['version'] = Variable<int>(version);
    map['updated_at'] = Variable<String>(updatedAt);
    if (!nullToAbsent || deletedAt != null) {
      map['deleted_at'] = Variable<String>(deletedAt);
    }
    map['sync_state'] = Variable<String>(syncState);
    map['payload_json'] = Variable<String>(payloadJson);
    return map;
  }

  NoteLinksDriftTableCompanion toCompanion(bool nullToAbsent) {
    return NoteLinksDriftTableCompanion(
      id: Value(id),
      userId: Value(userId),
      version: Value(version),
      updatedAt: Value(updatedAt),
      deletedAt: deletedAt == null && nullToAbsent
          ? const Value.absent()
          : Value(deletedAt),
      syncState: Value(syncState),
      payloadJson: Value(payloadJson),
    );
  }

  factory NoteLinksDriftTableData.fromJson(Map<String, dynamic> json,
      {ValueSerializer? serializer}) {
    serializer ??= driftRuntimeOptions.defaultSerializer;
    return NoteLinksDriftTableData(
      id: serializer.fromJson<String>(json['id']),
      userId: serializer.fromJson<String>(json['userId']),
      version: serializer.fromJson<int>(json['version']),
      updatedAt: serializer.fromJson<String>(json['updatedAt']),
      deletedAt: serializer.fromJson<String?>(json['deletedAt']),
      syncState: serializer.fromJson<String>(json['syncState']),
      payloadJson: serializer.fromJson<String>(json['payloadJson']),
    );
  }
  @override
  Map<String, dynamic> toJson({ValueSerializer? serializer}) {
    serializer ??= driftRuntimeOptions.defaultSerializer;
    return <String, dynamic>{
      'id': serializer.toJson<String>(id),
      'userId': serializer.toJson<String>(userId),
      'version': serializer.toJson<int>(version),
      'updatedAt': serializer.toJson<String>(updatedAt),
      'deletedAt': serializer.toJson<String?>(deletedAt),
      'syncState': serializer.toJson<String>(syncState),
      'payloadJson': serializer.toJson<String>(payloadJson),
    };
  }

  NoteLinksDriftTableData copyWith(
          {String? id,
          String? userId,
          int? version,
          String? updatedAt,
          Value<String?> deletedAt = const Value.absent(),
          String? syncState,
          String? payloadJson}) =>
      NoteLinksDriftTableData(
        id: id ?? this.id,
        userId: userId ?? this.userId,
        version: version ?? this.version,
        updatedAt: updatedAt ?? this.updatedAt,
        deletedAt: deletedAt.present ? deletedAt.value : this.deletedAt,
        syncState: syncState ?? this.syncState,
        payloadJson: payloadJson ?? this.payloadJson,
      );
  NoteLinksDriftTableData copyWithCompanion(NoteLinksDriftTableCompanion data) {
    return NoteLinksDriftTableData(
      id: data.id.present ? data.id.value : this.id,
      userId: data.userId.present ? data.userId.value : this.userId,
      version: data.version.present ? data.version.value : this.version,
      updatedAt: data.updatedAt.present ? data.updatedAt.value : this.updatedAt,
      deletedAt: data.deletedAt.present ? data.deletedAt.value : this.deletedAt,
      syncState: data.syncState.present ? data.syncState.value : this.syncState,
      payloadJson:
          data.payloadJson.present ? data.payloadJson.value : this.payloadJson,
    );
  }

  @override
  String toString() {
    return (StringBuffer('NoteLinksDriftTableData(')
          ..write('id: $id, ')
          ..write('userId: $userId, ')
          ..write('version: $version, ')
          ..write('updatedAt: $updatedAt, ')
          ..write('deletedAt: $deletedAt, ')
          ..write('syncState: $syncState, ')
          ..write('payloadJson: $payloadJson')
          ..write(')'))
        .toString();
  }

  @override
  int get hashCode => Object.hash(
      id, userId, version, updatedAt, deletedAt, syncState, payloadJson);
  @override
  bool operator ==(Object other) =>
      identical(this, other) ||
      (other is NoteLinksDriftTableData &&
          other.id == this.id &&
          other.userId == this.userId &&
          other.version == this.version &&
          other.updatedAt == this.updatedAt &&
          other.deletedAt == this.deletedAt &&
          other.syncState == this.syncState &&
          other.payloadJson == this.payloadJson);
}

class NoteLinksDriftTableCompanion
    extends UpdateCompanion<NoteLinksDriftTableData> {
  final Value<String> id;
  final Value<String> userId;
  final Value<int> version;
  final Value<String> updatedAt;
  final Value<String?> deletedAt;
  final Value<String> syncState;
  final Value<String> payloadJson;
  final Value<int> rowid;
  const NoteLinksDriftTableCompanion({
    this.id = const Value.absent(),
    this.userId = const Value.absent(),
    this.version = const Value.absent(),
    this.updatedAt = const Value.absent(),
    this.deletedAt = const Value.absent(),
    this.syncState = const Value.absent(),
    this.payloadJson = const Value.absent(),
    this.rowid = const Value.absent(),
  });
  NoteLinksDriftTableCompanion.insert({
    required String id,
    required String userId,
    this.version = const Value.absent(),
    required String updatedAt,
    this.deletedAt = const Value.absent(),
    this.syncState = const Value.absent(),
    required String payloadJson,
    this.rowid = const Value.absent(),
  })  : id = Value(id),
        userId = Value(userId),
        updatedAt = Value(updatedAt),
        payloadJson = Value(payloadJson);
  static Insertable<NoteLinksDriftTableData> custom({
    Expression<String>? id,
    Expression<String>? userId,
    Expression<int>? version,
    Expression<String>? updatedAt,
    Expression<String>? deletedAt,
    Expression<String>? syncState,
    Expression<String>? payloadJson,
    Expression<int>? rowid,
  }) {
    return RawValuesInsertable({
      if (id != null) 'id': id,
      if (userId != null) 'user_id': userId,
      if (version != null) 'version': version,
      if (updatedAt != null) 'updated_at': updatedAt,
      if (deletedAt != null) 'deleted_at': deletedAt,
      if (syncState != null) 'sync_state': syncState,
      if (payloadJson != null) 'payload_json': payloadJson,
      if (rowid != null) 'rowid': rowid,
    });
  }

  NoteLinksDriftTableCompanion copyWith(
      {Value<String>? id,
      Value<String>? userId,
      Value<int>? version,
      Value<String>? updatedAt,
      Value<String?>? deletedAt,
      Value<String>? syncState,
      Value<String>? payloadJson,
      Value<int>? rowid}) {
    return NoteLinksDriftTableCompanion(
      id: id ?? this.id,
      userId: userId ?? this.userId,
      version: version ?? this.version,
      updatedAt: updatedAt ?? this.updatedAt,
      deletedAt: deletedAt ?? this.deletedAt,
      syncState: syncState ?? this.syncState,
      payloadJson: payloadJson ?? this.payloadJson,
      rowid: rowid ?? this.rowid,
    );
  }

  @override
  Map<String, Expression> toColumns(bool nullToAbsent) {
    final map = <String, Expression>{};
    if (id.present) {
      map['id'] = Variable<String>(id.value);
    }
    if (userId.present) {
      map['user_id'] = Variable<String>(userId.value);
    }
    if (version.present) {
      map['version'] = Variable<int>(version.value);
    }
    if (updatedAt.present) {
      map['updated_at'] = Variable<String>(updatedAt.value);
    }
    if (deletedAt.present) {
      map['deleted_at'] = Variable<String>(deletedAt.value);
    }
    if (syncState.present) {
      map['sync_state'] = Variable<String>(syncState.value);
    }
    if (payloadJson.present) {
      map['payload_json'] = Variable<String>(payloadJson.value);
    }
    if (rowid.present) {
      map['rowid'] = Variable<int>(rowid.value);
    }
    return map;
  }

  @override
  String toString() {
    return (StringBuffer('NoteLinksDriftTableCompanion(')
          ..write('id: $id, ')
          ..write('userId: $userId, ')
          ..write('version: $version, ')
          ..write('updatedAt: $updatedAt, ')
          ..write('deletedAt: $deletedAt, ')
          ..write('syncState: $syncState, ')
          ..write('payloadJson: $payloadJson, ')
          ..write('rowid: $rowid')
          ..write(')'))
        .toString();
  }
}

class $GoalsDriftTableTable extends GoalsDriftTable
    with TableInfo<$GoalsDriftTableTable, GoalsDriftTableData> {
  @override
  final GeneratedDatabase attachedDatabase;
  final String? _alias;
  $GoalsDriftTableTable(this.attachedDatabase, [this._alias]);
  static const VerificationMeta _idMeta = const VerificationMeta('id');
  @override
  late final GeneratedColumn<String> id = GeneratedColumn<String>(
      'id', aliasedName, false,
      type: DriftSqlType.string, requiredDuringInsert: true);
  static const VerificationMeta _userIdMeta = const VerificationMeta('userId');
  @override
  late final GeneratedColumn<String> userId = GeneratedColumn<String>(
      'user_id', aliasedName, false,
      type: DriftSqlType.string, requiredDuringInsert: true);
  static const VerificationMeta _versionMeta =
      const VerificationMeta('version');
  @override
  late final GeneratedColumn<int> version = GeneratedColumn<int>(
      'version', aliasedName, false,
      type: DriftSqlType.int,
      requiredDuringInsert: false,
      defaultValue: const Constant(0));
  static const VerificationMeta _updatedAtMeta =
      const VerificationMeta('updatedAt');
  @override
  late final GeneratedColumn<String> updatedAt = GeneratedColumn<String>(
      'updated_at', aliasedName, false,
      type: DriftSqlType.string, requiredDuringInsert: true);
  static const VerificationMeta _deletedAtMeta =
      const VerificationMeta('deletedAt');
  @override
  late final GeneratedColumn<String> deletedAt = GeneratedColumn<String>(
      'deleted_at', aliasedName, true,
      type: DriftSqlType.string, requiredDuringInsert: false);
  static const VerificationMeta _syncStateMeta =
      const VerificationMeta('syncState');
  @override
  late final GeneratedColumn<String> syncState = GeneratedColumn<String>(
      'sync_state', aliasedName, false,
      type: DriftSqlType.string,
      requiredDuringInsert: false,
      defaultValue: const Constant('synced'));
  static const VerificationMeta _payloadJsonMeta =
      const VerificationMeta('payloadJson');
  @override
  late final GeneratedColumn<String> payloadJson = GeneratedColumn<String>(
      'payload_json', aliasedName, false,
      type: DriftSqlType.string, requiredDuringInsert: true);
  @override
  List<GeneratedColumn> get $columns =>
      [id, userId, version, updatedAt, deletedAt, syncState, payloadJson];
  @override
  String get aliasedName => _alias ?? actualTableName;
  @override
  String get actualTableName => $name;
  static const String $name = 'goals';
  @override
  VerificationContext validateIntegrity(
      Insertable<GoalsDriftTableData> instance,
      {bool isInserting = false}) {
    final context = VerificationContext();
    final data = instance.toColumns(true);
    if (data.containsKey('id')) {
      context.handle(_idMeta, id.isAcceptableOrUnknown(data['id']!, _idMeta));
    } else if (isInserting) {
      context.missing(_idMeta);
    }
    if (data.containsKey('user_id')) {
      context.handle(_userIdMeta,
          userId.isAcceptableOrUnknown(data['user_id']!, _userIdMeta));
    } else if (isInserting) {
      context.missing(_userIdMeta);
    }
    if (data.containsKey('version')) {
      context.handle(_versionMeta,
          version.isAcceptableOrUnknown(data['version']!, _versionMeta));
    }
    if (data.containsKey('updated_at')) {
      context.handle(_updatedAtMeta,
          updatedAt.isAcceptableOrUnknown(data['updated_at']!, _updatedAtMeta));
    } else if (isInserting) {
      context.missing(_updatedAtMeta);
    }
    if (data.containsKey('deleted_at')) {
      context.handle(_deletedAtMeta,
          deletedAt.isAcceptableOrUnknown(data['deleted_at']!, _deletedAtMeta));
    }
    if (data.containsKey('sync_state')) {
      context.handle(_syncStateMeta,
          syncState.isAcceptableOrUnknown(data['sync_state']!, _syncStateMeta));
    }
    if (data.containsKey('payload_json')) {
      context.handle(
          _payloadJsonMeta,
          payloadJson.isAcceptableOrUnknown(
              data['payload_json']!, _payloadJsonMeta));
    } else if (isInserting) {
      context.missing(_payloadJsonMeta);
    }
    return context;
  }

  @override
  Set<GeneratedColumn> get $primaryKey => {userId, id};
  @override
  GoalsDriftTableData map(Map<String, dynamic> data, {String? tablePrefix}) {
    final effectivePrefix = tablePrefix != null ? '$tablePrefix.' : '';
    return GoalsDriftTableData(
      id: attachedDatabase.typeMapping
          .read(DriftSqlType.string, data['${effectivePrefix}id'])!,
      userId: attachedDatabase.typeMapping
          .read(DriftSqlType.string, data['${effectivePrefix}user_id'])!,
      version: attachedDatabase.typeMapping
          .read(DriftSqlType.int, data['${effectivePrefix}version'])!,
      updatedAt: attachedDatabase.typeMapping
          .read(DriftSqlType.string, data['${effectivePrefix}updated_at'])!,
      deletedAt: attachedDatabase.typeMapping
          .read(DriftSqlType.string, data['${effectivePrefix}deleted_at']),
      syncState: attachedDatabase.typeMapping
          .read(DriftSqlType.string, data['${effectivePrefix}sync_state'])!,
      payloadJson: attachedDatabase.typeMapping
          .read(DriftSqlType.string, data['${effectivePrefix}payload_json'])!,
    );
  }

  @override
  $GoalsDriftTableTable createAlias(String alias) {
    return $GoalsDriftTableTable(attachedDatabase, alias);
  }
}

class GoalsDriftTableData extends DataClass
    implements Insertable<GoalsDriftTableData> {
  final String id;
  final String userId;
  final int version;
  final String updatedAt;
  final String? deletedAt;
  final String syncState;
  final String payloadJson;
  const GoalsDriftTableData(
      {required this.id,
      required this.userId,
      required this.version,
      required this.updatedAt,
      this.deletedAt,
      required this.syncState,
      required this.payloadJson});
  @override
  Map<String, Expression> toColumns(bool nullToAbsent) {
    final map = <String, Expression>{};
    map['id'] = Variable<String>(id);
    map['user_id'] = Variable<String>(userId);
    map['version'] = Variable<int>(version);
    map['updated_at'] = Variable<String>(updatedAt);
    if (!nullToAbsent || deletedAt != null) {
      map['deleted_at'] = Variable<String>(deletedAt);
    }
    map['sync_state'] = Variable<String>(syncState);
    map['payload_json'] = Variable<String>(payloadJson);
    return map;
  }

  GoalsDriftTableCompanion toCompanion(bool nullToAbsent) {
    return GoalsDriftTableCompanion(
      id: Value(id),
      userId: Value(userId),
      version: Value(version),
      updatedAt: Value(updatedAt),
      deletedAt: deletedAt == null && nullToAbsent
          ? const Value.absent()
          : Value(deletedAt),
      syncState: Value(syncState),
      payloadJson: Value(payloadJson),
    );
  }

  factory GoalsDriftTableData.fromJson(Map<String, dynamic> json,
      {ValueSerializer? serializer}) {
    serializer ??= driftRuntimeOptions.defaultSerializer;
    return GoalsDriftTableData(
      id: serializer.fromJson<String>(json['id']),
      userId: serializer.fromJson<String>(json['userId']),
      version: serializer.fromJson<int>(json['version']),
      updatedAt: serializer.fromJson<String>(json['updatedAt']),
      deletedAt: serializer.fromJson<String?>(json['deletedAt']),
      syncState: serializer.fromJson<String>(json['syncState']),
      payloadJson: serializer.fromJson<String>(json['payloadJson']),
    );
  }
  @override
  Map<String, dynamic> toJson({ValueSerializer? serializer}) {
    serializer ??= driftRuntimeOptions.defaultSerializer;
    return <String, dynamic>{
      'id': serializer.toJson<String>(id),
      'userId': serializer.toJson<String>(userId),
      'version': serializer.toJson<int>(version),
      'updatedAt': serializer.toJson<String>(updatedAt),
      'deletedAt': serializer.toJson<String?>(deletedAt),
      'syncState': serializer.toJson<String>(syncState),
      'payloadJson': serializer.toJson<String>(payloadJson),
    };
  }

  GoalsDriftTableData copyWith(
          {String? id,
          String? userId,
          int? version,
          String? updatedAt,
          Value<String?> deletedAt = const Value.absent(),
          String? syncState,
          String? payloadJson}) =>
      GoalsDriftTableData(
        id: id ?? this.id,
        userId: userId ?? this.userId,
        version: version ?? this.version,
        updatedAt: updatedAt ?? this.updatedAt,
        deletedAt: deletedAt.present ? deletedAt.value : this.deletedAt,
        syncState: syncState ?? this.syncState,
        payloadJson: payloadJson ?? this.payloadJson,
      );
  GoalsDriftTableData copyWithCompanion(GoalsDriftTableCompanion data) {
    return GoalsDriftTableData(
      id: data.id.present ? data.id.value : this.id,
      userId: data.userId.present ? data.userId.value : this.userId,
      version: data.version.present ? data.version.value : this.version,
      updatedAt: data.updatedAt.present ? data.updatedAt.value : this.updatedAt,
      deletedAt: data.deletedAt.present ? data.deletedAt.value : this.deletedAt,
      syncState: data.syncState.present ? data.syncState.value : this.syncState,
      payloadJson:
          data.payloadJson.present ? data.payloadJson.value : this.payloadJson,
    );
  }

  @override
  String toString() {
    return (StringBuffer('GoalsDriftTableData(')
          ..write('id: $id, ')
          ..write('userId: $userId, ')
          ..write('version: $version, ')
          ..write('updatedAt: $updatedAt, ')
          ..write('deletedAt: $deletedAt, ')
          ..write('syncState: $syncState, ')
          ..write('payloadJson: $payloadJson')
          ..write(')'))
        .toString();
  }

  @override
  int get hashCode => Object.hash(
      id, userId, version, updatedAt, deletedAt, syncState, payloadJson);
  @override
  bool operator ==(Object other) =>
      identical(this, other) ||
      (other is GoalsDriftTableData &&
          other.id == this.id &&
          other.userId == this.userId &&
          other.version == this.version &&
          other.updatedAt == this.updatedAt &&
          other.deletedAt == this.deletedAt &&
          other.syncState == this.syncState &&
          other.payloadJson == this.payloadJson);
}

class GoalsDriftTableCompanion extends UpdateCompanion<GoalsDriftTableData> {
  final Value<String> id;
  final Value<String> userId;
  final Value<int> version;
  final Value<String> updatedAt;
  final Value<String?> deletedAt;
  final Value<String> syncState;
  final Value<String> payloadJson;
  final Value<int> rowid;
  const GoalsDriftTableCompanion({
    this.id = const Value.absent(),
    this.userId = const Value.absent(),
    this.version = const Value.absent(),
    this.updatedAt = const Value.absent(),
    this.deletedAt = const Value.absent(),
    this.syncState = const Value.absent(),
    this.payloadJson = const Value.absent(),
    this.rowid = const Value.absent(),
  });
  GoalsDriftTableCompanion.insert({
    required String id,
    required String userId,
    this.version = const Value.absent(),
    required String updatedAt,
    this.deletedAt = const Value.absent(),
    this.syncState = const Value.absent(),
    required String payloadJson,
    this.rowid = const Value.absent(),
  })  : id = Value(id),
        userId = Value(userId),
        updatedAt = Value(updatedAt),
        payloadJson = Value(payloadJson);
  static Insertable<GoalsDriftTableData> custom({
    Expression<String>? id,
    Expression<String>? userId,
    Expression<int>? version,
    Expression<String>? updatedAt,
    Expression<String>? deletedAt,
    Expression<String>? syncState,
    Expression<String>? payloadJson,
    Expression<int>? rowid,
  }) {
    return RawValuesInsertable({
      if (id != null) 'id': id,
      if (userId != null) 'user_id': userId,
      if (version != null) 'version': version,
      if (updatedAt != null) 'updated_at': updatedAt,
      if (deletedAt != null) 'deleted_at': deletedAt,
      if (syncState != null) 'sync_state': syncState,
      if (payloadJson != null) 'payload_json': payloadJson,
      if (rowid != null) 'rowid': rowid,
    });
  }

  GoalsDriftTableCompanion copyWith(
      {Value<String>? id,
      Value<String>? userId,
      Value<int>? version,
      Value<String>? updatedAt,
      Value<String?>? deletedAt,
      Value<String>? syncState,
      Value<String>? payloadJson,
      Value<int>? rowid}) {
    return GoalsDriftTableCompanion(
      id: id ?? this.id,
      userId: userId ?? this.userId,
      version: version ?? this.version,
      updatedAt: updatedAt ?? this.updatedAt,
      deletedAt: deletedAt ?? this.deletedAt,
      syncState: syncState ?? this.syncState,
      payloadJson: payloadJson ?? this.payloadJson,
      rowid: rowid ?? this.rowid,
    );
  }

  @override
  Map<String, Expression> toColumns(bool nullToAbsent) {
    final map = <String, Expression>{};
    if (id.present) {
      map['id'] = Variable<String>(id.value);
    }
    if (userId.present) {
      map['user_id'] = Variable<String>(userId.value);
    }
    if (version.present) {
      map['version'] = Variable<int>(version.value);
    }
    if (updatedAt.present) {
      map['updated_at'] = Variable<String>(updatedAt.value);
    }
    if (deletedAt.present) {
      map['deleted_at'] = Variable<String>(deletedAt.value);
    }
    if (syncState.present) {
      map['sync_state'] = Variable<String>(syncState.value);
    }
    if (payloadJson.present) {
      map['payload_json'] = Variable<String>(payloadJson.value);
    }
    if (rowid.present) {
      map['rowid'] = Variable<int>(rowid.value);
    }
    return map;
  }

  @override
  String toString() {
    return (StringBuffer('GoalsDriftTableCompanion(')
          ..write('id: $id, ')
          ..write('userId: $userId, ')
          ..write('version: $version, ')
          ..write('updatedAt: $updatedAt, ')
          ..write('deletedAt: $deletedAt, ')
          ..write('syncState: $syncState, ')
          ..write('payloadJson: $payloadJson, ')
          ..write('rowid: $rowid')
          ..write(')'))
        .toString();
  }
}

class $MilestonesDriftTableTable extends MilestonesDriftTable
    with TableInfo<$MilestonesDriftTableTable, MilestonesDriftTableData> {
  @override
  final GeneratedDatabase attachedDatabase;
  final String? _alias;
  $MilestonesDriftTableTable(this.attachedDatabase, [this._alias]);
  static const VerificationMeta _idMeta = const VerificationMeta('id');
  @override
  late final GeneratedColumn<String> id = GeneratedColumn<String>(
      'id', aliasedName, false,
      type: DriftSqlType.string, requiredDuringInsert: true);
  static const VerificationMeta _userIdMeta = const VerificationMeta('userId');
  @override
  late final GeneratedColumn<String> userId = GeneratedColumn<String>(
      'user_id', aliasedName, false,
      type: DriftSqlType.string, requiredDuringInsert: true);
  static const VerificationMeta _versionMeta =
      const VerificationMeta('version');
  @override
  late final GeneratedColumn<int> version = GeneratedColumn<int>(
      'version', aliasedName, false,
      type: DriftSqlType.int,
      requiredDuringInsert: false,
      defaultValue: const Constant(0));
  static const VerificationMeta _updatedAtMeta =
      const VerificationMeta('updatedAt');
  @override
  late final GeneratedColumn<String> updatedAt = GeneratedColumn<String>(
      'updated_at', aliasedName, false,
      type: DriftSqlType.string, requiredDuringInsert: true);
  static const VerificationMeta _deletedAtMeta =
      const VerificationMeta('deletedAt');
  @override
  late final GeneratedColumn<String> deletedAt = GeneratedColumn<String>(
      'deleted_at', aliasedName, true,
      type: DriftSqlType.string, requiredDuringInsert: false);
  static const VerificationMeta _syncStateMeta =
      const VerificationMeta('syncState');
  @override
  late final GeneratedColumn<String> syncState = GeneratedColumn<String>(
      'sync_state', aliasedName, false,
      type: DriftSqlType.string,
      requiredDuringInsert: false,
      defaultValue: const Constant('synced'));
  static const VerificationMeta _payloadJsonMeta =
      const VerificationMeta('payloadJson');
  @override
  late final GeneratedColumn<String> payloadJson = GeneratedColumn<String>(
      'payload_json', aliasedName, false,
      type: DriftSqlType.string, requiredDuringInsert: true);
  @override
  List<GeneratedColumn> get $columns =>
      [id, userId, version, updatedAt, deletedAt, syncState, payloadJson];
  @override
  String get aliasedName => _alias ?? actualTableName;
  @override
  String get actualTableName => $name;
  static const String $name = 'milestones';
  @override
  VerificationContext validateIntegrity(
      Insertable<MilestonesDriftTableData> instance,
      {bool isInserting = false}) {
    final context = VerificationContext();
    final data = instance.toColumns(true);
    if (data.containsKey('id')) {
      context.handle(_idMeta, id.isAcceptableOrUnknown(data['id']!, _idMeta));
    } else if (isInserting) {
      context.missing(_idMeta);
    }
    if (data.containsKey('user_id')) {
      context.handle(_userIdMeta,
          userId.isAcceptableOrUnknown(data['user_id']!, _userIdMeta));
    } else if (isInserting) {
      context.missing(_userIdMeta);
    }
    if (data.containsKey('version')) {
      context.handle(_versionMeta,
          version.isAcceptableOrUnknown(data['version']!, _versionMeta));
    }
    if (data.containsKey('updated_at')) {
      context.handle(_updatedAtMeta,
          updatedAt.isAcceptableOrUnknown(data['updated_at']!, _updatedAtMeta));
    } else if (isInserting) {
      context.missing(_updatedAtMeta);
    }
    if (data.containsKey('deleted_at')) {
      context.handle(_deletedAtMeta,
          deletedAt.isAcceptableOrUnknown(data['deleted_at']!, _deletedAtMeta));
    }
    if (data.containsKey('sync_state')) {
      context.handle(_syncStateMeta,
          syncState.isAcceptableOrUnknown(data['sync_state']!, _syncStateMeta));
    }
    if (data.containsKey('payload_json')) {
      context.handle(
          _payloadJsonMeta,
          payloadJson.isAcceptableOrUnknown(
              data['payload_json']!, _payloadJsonMeta));
    } else if (isInserting) {
      context.missing(_payloadJsonMeta);
    }
    return context;
  }

  @override
  Set<GeneratedColumn> get $primaryKey => {userId, id};
  @override
  MilestonesDriftTableData map(Map<String, dynamic> data,
      {String? tablePrefix}) {
    final effectivePrefix = tablePrefix != null ? '$tablePrefix.' : '';
    return MilestonesDriftTableData(
      id: attachedDatabase.typeMapping
          .read(DriftSqlType.string, data['${effectivePrefix}id'])!,
      userId: attachedDatabase.typeMapping
          .read(DriftSqlType.string, data['${effectivePrefix}user_id'])!,
      version: attachedDatabase.typeMapping
          .read(DriftSqlType.int, data['${effectivePrefix}version'])!,
      updatedAt: attachedDatabase.typeMapping
          .read(DriftSqlType.string, data['${effectivePrefix}updated_at'])!,
      deletedAt: attachedDatabase.typeMapping
          .read(DriftSqlType.string, data['${effectivePrefix}deleted_at']),
      syncState: attachedDatabase.typeMapping
          .read(DriftSqlType.string, data['${effectivePrefix}sync_state'])!,
      payloadJson: attachedDatabase.typeMapping
          .read(DriftSqlType.string, data['${effectivePrefix}payload_json'])!,
    );
  }

  @override
  $MilestonesDriftTableTable createAlias(String alias) {
    return $MilestonesDriftTableTable(attachedDatabase, alias);
  }
}

class MilestonesDriftTableData extends DataClass
    implements Insertable<MilestonesDriftTableData> {
  final String id;
  final String userId;
  final int version;
  final String updatedAt;
  final String? deletedAt;
  final String syncState;
  final String payloadJson;
  const MilestonesDriftTableData(
      {required this.id,
      required this.userId,
      required this.version,
      required this.updatedAt,
      this.deletedAt,
      required this.syncState,
      required this.payloadJson});
  @override
  Map<String, Expression> toColumns(bool nullToAbsent) {
    final map = <String, Expression>{};
    map['id'] = Variable<String>(id);
    map['user_id'] = Variable<String>(userId);
    map['version'] = Variable<int>(version);
    map['updated_at'] = Variable<String>(updatedAt);
    if (!nullToAbsent || deletedAt != null) {
      map['deleted_at'] = Variable<String>(deletedAt);
    }
    map['sync_state'] = Variable<String>(syncState);
    map['payload_json'] = Variable<String>(payloadJson);
    return map;
  }

  MilestonesDriftTableCompanion toCompanion(bool nullToAbsent) {
    return MilestonesDriftTableCompanion(
      id: Value(id),
      userId: Value(userId),
      version: Value(version),
      updatedAt: Value(updatedAt),
      deletedAt: deletedAt == null && nullToAbsent
          ? const Value.absent()
          : Value(deletedAt),
      syncState: Value(syncState),
      payloadJson: Value(payloadJson),
    );
  }

  factory MilestonesDriftTableData.fromJson(Map<String, dynamic> json,
      {ValueSerializer? serializer}) {
    serializer ??= driftRuntimeOptions.defaultSerializer;
    return MilestonesDriftTableData(
      id: serializer.fromJson<String>(json['id']),
      userId: serializer.fromJson<String>(json['userId']),
      version: serializer.fromJson<int>(json['version']),
      updatedAt: serializer.fromJson<String>(json['updatedAt']),
      deletedAt: serializer.fromJson<String?>(json['deletedAt']),
      syncState: serializer.fromJson<String>(json['syncState']),
      payloadJson: serializer.fromJson<String>(json['payloadJson']),
    );
  }
  @override
  Map<String, dynamic> toJson({ValueSerializer? serializer}) {
    serializer ??= driftRuntimeOptions.defaultSerializer;
    return <String, dynamic>{
      'id': serializer.toJson<String>(id),
      'userId': serializer.toJson<String>(userId),
      'version': serializer.toJson<int>(version),
      'updatedAt': serializer.toJson<String>(updatedAt),
      'deletedAt': serializer.toJson<String?>(deletedAt),
      'syncState': serializer.toJson<String>(syncState),
      'payloadJson': serializer.toJson<String>(payloadJson),
    };
  }

  MilestonesDriftTableData copyWith(
          {String? id,
          String? userId,
          int? version,
          String? updatedAt,
          Value<String?> deletedAt = const Value.absent(),
          String? syncState,
          String? payloadJson}) =>
      MilestonesDriftTableData(
        id: id ?? this.id,
        userId: userId ?? this.userId,
        version: version ?? this.version,
        updatedAt: updatedAt ?? this.updatedAt,
        deletedAt: deletedAt.present ? deletedAt.value : this.deletedAt,
        syncState: syncState ?? this.syncState,
        payloadJson: payloadJson ?? this.payloadJson,
      );
  MilestonesDriftTableData copyWithCompanion(
      MilestonesDriftTableCompanion data) {
    return MilestonesDriftTableData(
      id: data.id.present ? data.id.value : this.id,
      userId: data.userId.present ? data.userId.value : this.userId,
      version: data.version.present ? data.version.value : this.version,
      updatedAt: data.updatedAt.present ? data.updatedAt.value : this.updatedAt,
      deletedAt: data.deletedAt.present ? data.deletedAt.value : this.deletedAt,
      syncState: data.syncState.present ? data.syncState.value : this.syncState,
      payloadJson:
          data.payloadJson.present ? data.payloadJson.value : this.payloadJson,
    );
  }

  @override
  String toString() {
    return (StringBuffer('MilestonesDriftTableData(')
          ..write('id: $id, ')
          ..write('userId: $userId, ')
          ..write('version: $version, ')
          ..write('updatedAt: $updatedAt, ')
          ..write('deletedAt: $deletedAt, ')
          ..write('syncState: $syncState, ')
          ..write('payloadJson: $payloadJson')
          ..write(')'))
        .toString();
  }

  @override
  int get hashCode => Object.hash(
      id, userId, version, updatedAt, deletedAt, syncState, payloadJson);
  @override
  bool operator ==(Object other) =>
      identical(this, other) ||
      (other is MilestonesDriftTableData &&
          other.id == this.id &&
          other.userId == this.userId &&
          other.version == this.version &&
          other.updatedAt == this.updatedAt &&
          other.deletedAt == this.deletedAt &&
          other.syncState == this.syncState &&
          other.payloadJson == this.payloadJson);
}

class MilestonesDriftTableCompanion
    extends UpdateCompanion<MilestonesDriftTableData> {
  final Value<String> id;
  final Value<String> userId;
  final Value<int> version;
  final Value<String> updatedAt;
  final Value<String?> deletedAt;
  final Value<String> syncState;
  final Value<String> payloadJson;
  final Value<int> rowid;
  const MilestonesDriftTableCompanion({
    this.id = const Value.absent(),
    this.userId = const Value.absent(),
    this.version = const Value.absent(),
    this.updatedAt = const Value.absent(),
    this.deletedAt = const Value.absent(),
    this.syncState = const Value.absent(),
    this.payloadJson = const Value.absent(),
    this.rowid = const Value.absent(),
  });
  MilestonesDriftTableCompanion.insert({
    required String id,
    required String userId,
    this.version = const Value.absent(),
    required String updatedAt,
    this.deletedAt = const Value.absent(),
    this.syncState = const Value.absent(),
    required String payloadJson,
    this.rowid = const Value.absent(),
  })  : id = Value(id),
        userId = Value(userId),
        updatedAt = Value(updatedAt),
        payloadJson = Value(payloadJson);
  static Insertable<MilestonesDriftTableData> custom({
    Expression<String>? id,
    Expression<String>? userId,
    Expression<int>? version,
    Expression<String>? updatedAt,
    Expression<String>? deletedAt,
    Expression<String>? syncState,
    Expression<String>? payloadJson,
    Expression<int>? rowid,
  }) {
    return RawValuesInsertable({
      if (id != null) 'id': id,
      if (userId != null) 'user_id': userId,
      if (version != null) 'version': version,
      if (updatedAt != null) 'updated_at': updatedAt,
      if (deletedAt != null) 'deleted_at': deletedAt,
      if (syncState != null) 'sync_state': syncState,
      if (payloadJson != null) 'payload_json': payloadJson,
      if (rowid != null) 'rowid': rowid,
    });
  }

  MilestonesDriftTableCompanion copyWith(
      {Value<String>? id,
      Value<String>? userId,
      Value<int>? version,
      Value<String>? updatedAt,
      Value<String?>? deletedAt,
      Value<String>? syncState,
      Value<String>? payloadJson,
      Value<int>? rowid}) {
    return MilestonesDriftTableCompanion(
      id: id ?? this.id,
      userId: userId ?? this.userId,
      version: version ?? this.version,
      updatedAt: updatedAt ?? this.updatedAt,
      deletedAt: deletedAt ?? this.deletedAt,
      syncState: syncState ?? this.syncState,
      payloadJson: payloadJson ?? this.payloadJson,
      rowid: rowid ?? this.rowid,
    );
  }

  @override
  Map<String, Expression> toColumns(bool nullToAbsent) {
    final map = <String, Expression>{};
    if (id.present) {
      map['id'] = Variable<String>(id.value);
    }
    if (userId.present) {
      map['user_id'] = Variable<String>(userId.value);
    }
    if (version.present) {
      map['version'] = Variable<int>(version.value);
    }
    if (updatedAt.present) {
      map['updated_at'] = Variable<String>(updatedAt.value);
    }
    if (deletedAt.present) {
      map['deleted_at'] = Variable<String>(deletedAt.value);
    }
    if (syncState.present) {
      map['sync_state'] = Variable<String>(syncState.value);
    }
    if (payloadJson.present) {
      map['payload_json'] = Variable<String>(payloadJson.value);
    }
    if (rowid.present) {
      map['rowid'] = Variable<int>(rowid.value);
    }
    return map;
  }

  @override
  String toString() {
    return (StringBuffer('MilestonesDriftTableCompanion(')
          ..write('id: $id, ')
          ..write('userId: $userId, ')
          ..write('version: $version, ')
          ..write('updatedAt: $updatedAt, ')
          ..write('deletedAt: $deletedAt, ')
          ..write('syncState: $syncState, ')
          ..write('payloadJson: $payloadJson, ')
          ..write('rowid: $rowid')
          ..write(')'))
        .toString();
  }
}

class $ReviewsDriftTableTable extends ReviewsDriftTable
    with TableInfo<$ReviewsDriftTableTable, ReviewsDriftTableData> {
  @override
  final GeneratedDatabase attachedDatabase;
  final String? _alias;
  $ReviewsDriftTableTable(this.attachedDatabase, [this._alias]);
  static const VerificationMeta _idMeta = const VerificationMeta('id');
  @override
  late final GeneratedColumn<String> id = GeneratedColumn<String>(
      'id', aliasedName, false,
      type: DriftSqlType.string, requiredDuringInsert: true);
  static const VerificationMeta _userIdMeta = const VerificationMeta('userId');
  @override
  late final GeneratedColumn<String> userId = GeneratedColumn<String>(
      'user_id', aliasedName, false,
      type: DriftSqlType.string, requiredDuringInsert: true);
  static const VerificationMeta _versionMeta =
      const VerificationMeta('version');
  @override
  late final GeneratedColumn<int> version = GeneratedColumn<int>(
      'version', aliasedName, false,
      type: DriftSqlType.int,
      requiredDuringInsert: false,
      defaultValue: const Constant(0));
  static const VerificationMeta _updatedAtMeta =
      const VerificationMeta('updatedAt');
  @override
  late final GeneratedColumn<String> updatedAt = GeneratedColumn<String>(
      'updated_at', aliasedName, false,
      type: DriftSqlType.string, requiredDuringInsert: true);
  static const VerificationMeta _deletedAtMeta =
      const VerificationMeta('deletedAt');
  @override
  late final GeneratedColumn<String> deletedAt = GeneratedColumn<String>(
      'deleted_at', aliasedName, true,
      type: DriftSqlType.string, requiredDuringInsert: false);
  static const VerificationMeta _syncStateMeta =
      const VerificationMeta('syncState');
  @override
  late final GeneratedColumn<String> syncState = GeneratedColumn<String>(
      'sync_state', aliasedName, false,
      type: DriftSqlType.string,
      requiredDuringInsert: false,
      defaultValue: const Constant('synced'));
  static const VerificationMeta _payloadJsonMeta =
      const VerificationMeta('payloadJson');
  @override
  late final GeneratedColumn<String> payloadJson = GeneratedColumn<String>(
      'payload_json', aliasedName, false,
      type: DriftSqlType.string, requiredDuringInsert: true);
  @override
  List<GeneratedColumn> get $columns =>
      [id, userId, version, updatedAt, deletedAt, syncState, payloadJson];
  @override
  String get aliasedName => _alias ?? actualTableName;
  @override
  String get actualTableName => $name;
  static const String $name = 'reviews';
  @override
  VerificationContext validateIntegrity(
      Insertable<ReviewsDriftTableData> instance,
      {bool isInserting = false}) {
    final context = VerificationContext();
    final data = instance.toColumns(true);
    if (data.containsKey('id')) {
      context.handle(_idMeta, id.isAcceptableOrUnknown(data['id']!, _idMeta));
    } else if (isInserting) {
      context.missing(_idMeta);
    }
    if (data.containsKey('user_id')) {
      context.handle(_userIdMeta,
          userId.isAcceptableOrUnknown(data['user_id']!, _userIdMeta));
    } else if (isInserting) {
      context.missing(_userIdMeta);
    }
    if (data.containsKey('version')) {
      context.handle(_versionMeta,
          version.isAcceptableOrUnknown(data['version']!, _versionMeta));
    }
    if (data.containsKey('updated_at')) {
      context.handle(_updatedAtMeta,
          updatedAt.isAcceptableOrUnknown(data['updated_at']!, _updatedAtMeta));
    } else if (isInserting) {
      context.missing(_updatedAtMeta);
    }
    if (data.containsKey('deleted_at')) {
      context.handle(_deletedAtMeta,
          deletedAt.isAcceptableOrUnknown(data['deleted_at']!, _deletedAtMeta));
    }
    if (data.containsKey('sync_state')) {
      context.handle(_syncStateMeta,
          syncState.isAcceptableOrUnknown(data['sync_state']!, _syncStateMeta));
    }
    if (data.containsKey('payload_json')) {
      context.handle(
          _payloadJsonMeta,
          payloadJson.isAcceptableOrUnknown(
              data['payload_json']!, _payloadJsonMeta));
    } else if (isInserting) {
      context.missing(_payloadJsonMeta);
    }
    return context;
  }

  @override
  Set<GeneratedColumn> get $primaryKey => {userId, id};
  @override
  ReviewsDriftTableData map(Map<String, dynamic> data, {String? tablePrefix}) {
    final effectivePrefix = tablePrefix != null ? '$tablePrefix.' : '';
    return ReviewsDriftTableData(
      id: attachedDatabase.typeMapping
          .read(DriftSqlType.string, data['${effectivePrefix}id'])!,
      userId: attachedDatabase.typeMapping
          .read(DriftSqlType.string, data['${effectivePrefix}user_id'])!,
      version: attachedDatabase.typeMapping
          .read(DriftSqlType.int, data['${effectivePrefix}version'])!,
      updatedAt: attachedDatabase.typeMapping
          .read(DriftSqlType.string, data['${effectivePrefix}updated_at'])!,
      deletedAt: attachedDatabase.typeMapping
          .read(DriftSqlType.string, data['${effectivePrefix}deleted_at']),
      syncState: attachedDatabase.typeMapping
          .read(DriftSqlType.string, data['${effectivePrefix}sync_state'])!,
      payloadJson: attachedDatabase.typeMapping
          .read(DriftSqlType.string, data['${effectivePrefix}payload_json'])!,
    );
  }

  @override
  $ReviewsDriftTableTable createAlias(String alias) {
    return $ReviewsDriftTableTable(attachedDatabase, alias);
  }
}

class ReviewsDriftTableData extends DataClass
    implements Insertable<ReviewsDriftTableData> {
  final String id;
  final String userId;
  final int version;
  final String updatedAt;
  final String? deletedAt;
  final String syncState;
  final String payloadJson;
  const ReviewsDriftTableData(
      {required this.id,
      required this.userId,
      required this.version,
      required this.updatedAt,
      this.deletedAt,
      required this.syncState,
      required this.payloadJson});
  @override
  Map<String, Expression> toColumns(bool nullToAbsent) {
    final map = <String, Expression>{};
    map['id'] = Variable<String>(id);
    map['user_id'] = Variable<String>(userId);
    map['version'] = Variable<int>(version);
    map['updated_at'] = Variable<String>(updatedAt);
    if (!nullToAbsent || deletedAt != null) {
      map['deleted_at'] = Variable<String>(deletedAt);
    }
    map['sync_state'] = Variable<String>(syncState);
    map['payload_json'] = Variable<String>(payloadJson);
    return map;
  }

  ReviewsDriftTableCompanion toCompanion(bool nullToAbsent) {
    return ReviewsDriftTableCompanion(
      id: Value(id),
      userId: Value(userId),
      version: Value(version),
      updatedAt: Value(updatedAt),
      deletedAt: deletedAt == null && nullToAbsent
          ? const Value.absent()
          : Value(deletedAt),
      syncState: Value(syncState),
      payloadJson: Value(payloadJson),
    );
  }

  factory ReviewsDriftTableData.fromJson(Map<String, dynamic> json,
      {ValueSerializer? serializer}) {
    serializer ??= driftRuntimeOptions.defaultSerializer;
    return ReviewsDriftTableData(
      id: serializer.fromJson<String>(json['id']),
      userId: serializer.fromJson<String>(json['userId']),
      version: serializer.fromJson<int>(json['version']),
      updatedAt: serializer.fromJson<String>(json['updatedAt']),
      deletedAt: serializer.fromJson<String?>(json['deletedAt']),
      syncState: serializer.fromJson<String>(json['syncState']),
      payloadJson: serializer.fromJson<String>(json['payloadJson']),
    );
  }
  @override
  Map<String, dynamic> toJson({ValueSerializer? serializer}) {
    serializer ??= driftRuntimeOptions.defaultSerializer;
    return <String, dynamic>{
      'id': serializer.toJson<String>(id),
      'userId': serializer.toJson<String>(userId),
      'version': serializer.toJson<int>(version),
      'updatedAt': serializer.toJson<String>(updatedAt),
      'deletedAt': serializer.toJson<String?>(deletedAt),
      'syncState': serializer.toJson<String>(syncState),
      'payloadJson': serializer.toJson<String>(payloadJson),
    };
  }

  ReviewsDriftTableData copyWith(
          {String? id,
          String? userId,
          int? version,
          String? updatedAt,
          Value<String?> deletedAt = const Value.absent(),
          String? syncState,
          String? payloadJson}) =>
      ReviewsDriftTableData(
        id: id ?? this.id,
        userId: userId ?? this.userId,
        version: version ?? this.version,
        updatedAt: updatedAt ?? this.updatedAt,
        deletedAt: deletedAt.present ? deletedAt.value : this.deletedAt,
        syncState: syncState ?? this.syncState,
        payloadJson: payloadJson ?? this.payloadJson,
      );
  ReviewsDriftTableData copyWithCompanion(ReviewsDriftTableCompanion data) {
    return ReviewsDriftTableData(
      id: data.id.present ? data.id.value : this.id,
      userId: data.userId.present ? data.userId.value : this.userId,
      version: data.version.present ? data.version.value : this.version,
      updatedAt: data.updatedAt.present ? data.updatedAt.value : this.updatedAt,
      deletedAt: data.deletedAt.present ? data.deletedAt.value : this.deletedAt,
      syncState: data.syncState.present ? data.syncState.value : this.syncState,
      payloadJson:
          data.payloadJson.present ? data.payloadJson.value : this.payloadJson,
    );
  }

  @override
  String toString() {
    return (StringBuffer('ReviewsDriftTableData(')
          ..write('id: $id, ')
          ..write('userId: $userId, ')
          ..write('version: $version, ')
          ..write('updatedAt: $updatedAt, ')
          ..write('deletedAt: $deletedAt, ')
          ..write('syncState: $syncState, ')
          ..write('payloadJson: $payloadJson')
          ..write(')'))
        .toString();
  }

  @override
  int get hashCode => Object.hash(
      id, userId, version, updatedAt, deletedAt, syncState, payloadJson);
  @override
  bool operator ==(Object other) =>
      identical(this, other) ||
      (other is ReviewsDriftTableData &&
          other.id == this.id &&
          other.userId == this.userId &&
          other.version == this.version &&
          other.updatedAt == this.updatedAt &&
          other.deletedAt == this.deletedAt &&
          other.syncState == this.syncState &&
          other.payloadJson == this.payloadJson);
}

class ReviewsDriftTableCompanion
    extends UpdateCompanion<ReviewsDriftTableData> {
  final Value<String> id;
  final Value<String> userId;
  final Value<int> version;
  final Value<String> updatedAt;
  final Value<String?> deletedAt;
  final Value<String> syncState;
  final Value<String> payloadJson;
  final Value<int> rowid;
  const ReviewsDriftTableCompanion({
    this.id = const Value.absent(),
    this.userId = const Value.absent(),
    this.version = const Value.absent(),
    this.updatedAt = const Value.absent(),
    this.deletedAt = const Value.absent(),
    this.syncState = const Value.absent(),
    this.payloadJson = const Value.absent(),
    this.rowid = const Value.absent(),
  });
  ReviewsDriftTableCompanion.insert({
    required String id,
    required String userId,
    this.version = const Value.absent(),
    required String updatedAt,
    this.deletedAt = const Value.absent(),
    this.syncState = const Value.absent(),
    required String payloadJson,
    this.rowid = const Value.absent(),
  })  : id = Value(id),
        userId = Value(userId),
        updatedAt = Value(updatedAt),
        payloadJson = Value(payloadJson);
  static Insertable<ReviewsDriftTableData> custom({
    Expression<String>? id,
    Expression<String>? userId,
    Expression<int>? version,
    Expression<String>? updatedAt,
    Expression<String>? deletedAt,
    Expression<String>? syncState,
    Expression<String>? payloadJson,
    Expression<int>? rowid,
  }) {
    return RawValuesInsertable({
      if (id != null) 'id': id,
      if (userId != null) 'user_id': userId,
      if (version != null) 'version': version,
      if (updatedAt != null) 'updated_at': updatedAt,
      if (deletedAt != null) 'deleted_at': deletedAt,
      if (syncState != null) 'sync_state': syncState,
      if (payloadJson != null) 'payload_json': payloadJson,
      if (rowid != null) 'rowid': rowid,
    });
  }

  ReviewsDriftTableCompanion copyWith(
      {Value<String>? id,
      Value<String>? userId,
      Value<int>? version,
      Value<String>? updatedAt,
      Value<String?>? deletedAt,
      Value<String>? syncState,
      Value<String>? payloadJson,
      Value<int>? rowid}) {
    return ReviewsDriftTableCompanion(
      id: id ?? this.id,
      userId: userId ?? this.userId,
      version: version ?? this.version,
      updatedAt: updatedAt ?? this.updatedAt,
      deletedAt: deletedAt ?? this.deletedAt,
      syncState: syncState ?? this.syncState,
      payloadJson: payloadJson ?? this.payloadJson,
      rowid: rowid ?? this.rowid,
    );
  }

  @override
  Map<String, Expression> toColumns(bool nullToAbsent) {
    final map = <String, Expression>{};
    if (id.present) {
      map['id'] = Variable<String>(id.value);
    }
    if (userId.present) {
      map['user_id'] = Variable<String>(userId.value);
    }
    if (version.present) {
      map['version'] = Variable<int>(version.value);
    }
    if (updatedAt.present) {
      map['updated_at'] = Variable<String>(updatedAt.value);
    }
    if (deletedAt.present) {
      map['deleted_at'] = Variable<String>(deletedAt.value);
    }
    if (syncState.present) {
      map['sync_state'] = Variable<String>(syncState.value);
    }
    if (payloadJson.present) {
      map['payload_json'] = Variable<String>(payloadJson.value);
    }
    if (rowid.present) {
      map['rowid'] = Variable<int>(rowid.value);
    }
    return map;
  }

  @override
  String toString() {
    return (StringBuffer('ReviewsDriftTableCompanion(')
          ..write('id: $id, ')
          ..write('userId: $userId, ')
          ..write('version: $version, ')
          ..write('updatedAt: $updatedAt, ')
          ..write('deletedAt: $deletedAt, ')
          ..write('syncState: $syncState, ')
          ..write('payloadJson: $payloadJson, ')
          ..write('rowid: $rowid')
          ..write(')'))
        .toString();
  }
}

abstract class _$LifeOsDriftDatabase extends GeneratedDatabase {
  _$LifeOsDriftDatabase(QueryExecutor e) : super(e);
  $LifeOsDriftDatabaseManager get managers => $LifeOsDriftDatabaseManager(this);
  late final $SyncStateDriftTableTable syncStateDriftTable =
      $SyncStateDriftTableTable(this);
  late final $OutboxDriftTableTable outboxDriftTable =
      $OutboxDriftTableTable(this);
  late final $EntityBaseSnapshotDriftTableTable entityBaseSnapshotDriftTable =
      $EntityBaseSnapshotDriftTableTable(this);
  late final $SyncLogDriftTableTable syncLogDriftTable =
      $SyncLogDriftTableTable(this);
  late final $ScheduledNotificationDriftTableTable
      scheduledNotificationDriftTable =
      $ScheduledNotificationDriftTableTable(this);
  late final $DeliveredNotificationDriftTableTable
      deliveredNotificationDriftTable =
      $DeliveredNotificationDriftTableTable(this);
  late final $UserSettingsDriftTableTable userSettingsDriftTable =
      $UserSettingsDriftTableTable(this);
  late final $CategoriesDriftTableTable categoriesDriftTable =
      $CategoriesDriftTableTable(this);
  late final $ProjectsDriftTableTable projectsDriftTable =
      $ProjectsDriftTableTable(this);
  late final $TasksDriftTableTable tasksDriftTable =
      $TasksDriftTableTable(this);
  late final $HabitsDriftTableTable habitsDriftTable =
      $HabitsDriftTableTable(this);
  late final $HabitCheckinsDriftTableTable habitCheckinsDriftTable =
      $HabitCheckinsDriftTableTable(this);
  late final $RoutinesDriftTableTable routinesDriftTable =
      $RoutinesDriftTableTable(this);
  late final $RoutineStepsDriftTableTable routineStepsDriftTable =
      $RoutineStepsDriftTableTable(this);
  late final $RoutineLogsDriftTableTable routineLogsDriftTable =
      $RoutineLogsDriftTableTable(this);
  late final $RoutineStepLogsDriftTableTable routineStepLogsDriftTable =
      $RoutineStepLogsDriftTableTable(this);
  late final $FinancialAccountsDriftTableTable financialAccountsDriftTable =
      $FinancialAccountsDriftTableTable(this);
  late final $FinancialCategoriesDriftTableTable financialCategoriesDriftTable =
      $FinancialCategoriesDriftTableTable(this);
  late final $FinancialTransactionsDriftTableTable
      financialTransactionsDriftTable =
      $FinancialTransactionsDriftTableTable(this);
  late final $AccountTransfersDriftTableTable accountTransfersDriftTable =
      $AccountTransfersDriftTableTable(this);
  late final $BudgetsDriftTableTable budgetsDriftTable =
      $BudgetsDriftTableTable(this);
  late final $RecurringTransactionsDriftTableTable
      recurringTransactionsDriftTable =
      $RecurringTransactionsDriftTableTable(this);
  late final $SavingGoalsDriftTableTable savingGoalsDriftTable =
      $SavingGoalsDriftTableTable(this);
  late final $SavingContributionsDriftTableTable savingContributionsDriftTable =
      $SavingContributionsDriftTableTable(this);
  late final $CalendarEventsDriftTableTable calendarEventsDriftTable =
      $CalendarEventsDriftTableTable(this);
  late final $EventExceptionsDriftTableTable eventExceptionsDriftTable =
      $EventExceptionsDriftTableTable(this);
  late final $TimeBlocksDriftTableTable timeBlocksDriftTable =
      $TimeBlocksDriftTableTable(this);
  late final $RemindersDriftTableTable remindersDriftTable =
      $RemindersDriftTableTable(this);
  late final $NotesDriftTableTable notesDriftTable =
      $NotesDriftTableTable(this);
  late final $TagsDriftTableTable tagsDriftTable = $TagsDriftTableTable(this);
  late final $NoteTagsDriftTableTable noteTagsDriftTable =
      $NoteTagsDriftTableTable(this);
  late final $NoteLinksDriftTableTable noteLinksDriftTable =
      $NoteLinksDriftTableTable(this);
  late final $GoalsDriftTableTable goalsDriftTable =
      $GoalsDriftTableTable(this);
  late final $MilestonesDriftTableTable milestonesDriftTable =
      $MilestonesDriftTableTable(this);
  late final $ReviewsDriftTableTable reviewsDriftTable =
      $ReviewsDriftTableTable(this);
  late final DriftSyncInfrastructureDao driftSyncInfrastructureDao =
      DriftSyncInfrastructureDao(this as LifeOsDriftDatabase);
  @override
  Iterable<TableInfo<Table, Object?>> get allTables =>
      allSchemaEntities.whereType<TableInfo<Table, Object?>>();
  @override
  List<DatabaseSchemaEntity> get allSchemaEntities => [
        syncStateDriftTable,
        outboxDriftTable,
        entityBaseSnapshotDriftTable,
        syncLogDriftTable,
        scheduledNotificationDriftTable,
        deliveredNotificationDriftTable,
        userSettingsDriftTable,
        categoriesDriftTable,
        projectsDriftTable,
        tasksDriftTable,
        habitsDriftTable,
        habitCheckinsDriftTable,
        routinesDriftTable,
        routineStepsDriftTable,
        routineLogsDriftTable,
        routineStepLogsDriftTable,
        financialAccountsDriftTable,
        financialCategoriesDriftTable,
        financialTransactionsDriftTable,
        accountTransfersDriftTable,
        budgetsDriftTable,
        recurringTransactionsDriftTable,
        savingGoalsDriftTable,
        savingContributionsDriftTable,
        calendarEventsDriftTable,
        eventExceptionsDriftTable,
        timeBlocksDriftTable,
        remindersDriftTable,
        notesDriftTable,
        tagsDriftTable,
        noteTagsDriftTable,
        noteLinksDriftTable,
        goalsDriftTable,
        milestonesDriftTable,
        reviewsDriftTable
      ];
}

typedef $$SyncStateDriftTableTableCreateCompanionBuilder
    = SyncStateDriftTableCompanion Function({
  required String userId,
  Value<int> lastCursor,
  Value<String?> lastSyncAt,
  Value<int> syncPaused,
  Value<String?> deviceId,
  Value<int> rowid,
});
typedef $$SyncStateDriftTableTableUpdateCompanionBuilder
    = SyncStateDriftTableCompanion Function({
  Value<String> userId,
  Value<int> lastCursor,
  Value<String?> lastSyncAt,
  Value<int> syncPaused,
  Value<String?> deviceId,
  Value<int> rowid,
});

class $$SyncStateDriftTableTableFilterComposer
    extends Composer<_$LifeOsDriftDatabase, $SyncStateDriftTableTable> {
  $$SyncStateDriftTableTableFilterComposer({
    required super.$db,
    required super.$table,
    super.joinBuilder,
    super.$addJoinBuilderToRootComposer,
    super.$removeJoinBuilderFromRootComposer,
  });
  ColumnFilters<String> get userId => $composableBuilder(
      column: $table.userId, builder: (column) => ColumnFilters(column));

  ColumnFilters<int> get lastCursor => $composableBuilder(
      column: $table.lastCursor, builder: (column) => ColumnFilters(column));

  ColumnFilters<String> get lastSyncAt => $composableBuilder(
      column: $table.lastSyncAt, builder: (column) => ColumnFilters(column));

  ColumnFilters<int> get syncPaused => $composableBuilder(
      column: $table.syncPaused, builder: (column) => ColumnFilters(column));

  ColumnFilters<String> get deviceId => $composableBuilder(
      column: $table.deviceId, builder: (column) => ColumnFilters(column));
}

class $$SyncStateDriftTableTableOrderingComposer
    extends Composer<_$LifeOsDriftDatabase, $SyncStateDriftTableTable> {
  $$SyncStateDriftTableTableOrderingComposer({
    required super.$db,
    required super.$table,
    super.joinBuilder,
    super.$addJoinBuilderToRootComposer,
    super.$removeJoinBuilderFromRootComposer,
  });
  ColumnOrderings<String> get userId => $composableBuilder(
      column: $table.userId, builder: (column) => ColumnOrderings(column));

  ColumnOrderings<int> get lastCursor => $composableBuilder(
      column: $table.lastCursor, builder: (column) => ColumnOrderings(column));

  ColumnOrderings<String> get lastSyncAt => $composableBuilder(
      column: $table.lastSyncAt, builder: (column) => ColumnOrderings(column));

  ColumnOrderings<int> get syncPaused => $composableBuilder(
      column: $table.syncPaused, builder: (column) => ColumnOrderings(column));

  ColumnOrderings<String> get deviceId => $composableBuilder(
      column: $table.deviceId, builder: (column) => ColumnOrderings(column));
}

class $$SyncStateDriftTableTableAnnotationComposer
    extends Composer<_$LifeOsDriftDatabase, $SyncStateDriftTableTable> {
  $$SyncStateDriftTableTableAnnotationComposer({
    required super.$db,
    required super.$table,
    super.joinBuilder,
    super.$addJoinBuilderToRootComposer,
    super.$removeJoinBuilderFromRootComposer,
  });
  GeneratedColumn<String> get userId =>
      $composableBuilder(column: $table.userId, builder: (column) => column);

  GeneratedColumn<int> get lastCursor => $composableBuilder(
      column: $table.lastCursor, builder: (column) => column);

  GeneratedColumn<String> get lastSyncAt => $composableBuilder(
      column: $table.lastSyncAt, builder: (column) => column);

  GeneratedColumn<int> get syncPaused => $composableBuilder(
      column: $table.syncPaused, builder: (column) => column);

  GeneratedColumn<String> get deviceId =>
      $composableBuilder(column: $table.deviceId, builder: (column) => column);
}

class $$SyncStateDriftTableTableTableManager extends RootTableManager<
    _$LifeOsDriftDatabase,
    $SyncStateDriftTableTable,
    SyncStateDriftTableData,
    $$SyncStateDriftTableTableFilterComposer,
    $$SyncStateDriftTableTableOrderingComposer,
    $$SyncStateDriftTableTableAnnotationComposer,
    $$SyncStateDriftTableTableCreateCompanionBuilder,
    $$SyncStateDriftTableTableUpdateCompanionBuilder,
    (
      SyncStateDriftTableData,
      BaseReferences<_$LifeOsDriftDatabase, $SyncStateDriftTableTable,
          SyncStateDriftTableData>
    ),
    SyncStateDriftTableData,
    PrefetchHooks Function()> {
  $$SyncStateDriftTableTableTableManager(
      _$LifeOsDriftDatabase db, $SyncStateDriftTableTable table)
      : super(TableManagerState(
          db: db,
          table: table,
          createFilteringComposer: () =>
              $$SyncStateDriftTableTableFilterComposer($db: db, $table: table),
          createOrderingComposer: () =>
              $$SyncStateDriftTableTableOrderingComposer(
                  $db: db, $table: table),
          createComputedFieldComposer: () =>
              $$SyncStateDriftTableTableAnnotationComposer(
                  $db: db, $table: table),
          updateCompanionCallback: ({
            Value<String> userId = const Value.absent(),
            Value<int> lastCursor = const Value.absent(),
            Value<String?> lastSyncAt = const Value.absent(),
            Value<int> syncPaused = const Value.absent(),
            Value<String?> deviceId = const Value.absent(),
            Value<int> rowid = const Value.absent(),
          }) =>
              SyncStateDriftTableCompanion(
            userId: userId,
            lastCursor: lastCursor,
            lastSyncAt: lastSyncAt,
            syncPaused: syncPaused,
            deviceId: deviceId,
            rowid: rowid,
          ),
          createCompanionCallback: ({
            required String userId,
            Value<int> lastCursor = const Value.absent(),
            Value<String?> lastSyncAt = const Value.absent(),
            Value<int> syncPaused = const Value.absent(),
            Value<String?> deviceId = const Value.absent(),
            Value<int> rowid = const Value.absent(),
          }) =>
              SyncStateDriftTableCompanion.insert(
            userId: userId,
            lastCursor: lastCursor,
            lastSyncAt: lastSyncAt,
            syncPaused: syncPaused,
            deviceId: deviceId,
            rowid: rowid,
          ),
          withReferenceMapper: (p0) => p0
              .map((e) => (e.readTable(table), BaseReferences(db, table, e)))
              .toList(),
          prefetchHooksCallback: null,
        ));
}

typedef $$SyncStateDriftTableTableProcessedTableManager = ProcessedTableManager<
    _$LifeOsDriftDatabase,
    $SyncStateDriftTableTable,
    SyncStateDriftTableData,
    $$SyncStateDriftTableTableFilterComposer,
    $$SyncStateDriftTableTableOrderingComposer,
    $$SyncStateDriftTableTableAnnotationComposer,
    $$SyncStateDriftTableTableCreateCompanionBuilder,
    $$SyncStateDriftTableTableUpdateCompanionBuilder,
    (
      SyncStateDriftTableData,
      BaseReferences<_$LifeOsDriftDatabase, $SyncStateDriftTableTable,
          SyncStateDriftTableData>
    ),
    SyncStateDriftTableData,
    PrefetchHooks Function()>;
typedef $$OutboxDriftTableTableCreateCompanionBuilder
    = OutboxDriftTableCompanion Function({
  required String mutationId,
  required String userId,
  required String entityType,
  required String entityId,
  required String op,
  Value<int> baseVersion,
  required String payloadJson,
  Value<String> status,
  Value<int> retryCount,
  Value<String?> lastErrorCode,
  Value<String?> lastErrorMessage,
  Value<String?> serverConflictJson,
  required String createdAt,
  required String updatedAt,
  Value<int> rowid,
});
typedef $$OutboxDriftTableTableUpdateCompanionBuilder
    = OutboxDriftTableCompanion Function({
  Value<String> mutationId,
  Value<String> userId,
  Value<String> entityType,
  Value<String> entityId,
  Value<String> op,
  Value<int> baseVersion,
  Value<String> payloadJson,
  Value<String> status,
  Value<int> retryCount,
  Value<String?> lastErrorCode,
  Value<String?> lastErrorMessage,
  Value<String?> serverConflictJson,
  Value<String> createdAt,
  Value<String> updatedAt,
  Value<int> rowid,
});

class $$OutboxDriftTableTableFilterComposer
    extends Composer<_$LifeOsDriftDatabase, $OutboxDriftTableTable> {
  $$OutboxDriftTableTableFilterComposer({
    required super.$db,
    required super.$table,
    super.joinBuilder,
    super.$addJoinBuilderToRootComposer,
    super.$removeJoinBuilderFromRootComposer,
  });
  ColumnFilters<String> get mutationId => $composableBuilder(
      column: $table.mutationId, builder: (column) => ColumnFilters(column));

  ColumnFilters<String> get userId => $composableBuilder(
      column: $table.userId, builder: (column) => ColumnFilters(column));

  ColumnFilters<String> get entityType => $composableBuilder(
      column: $table.entityType, builder: (column) => ColumnFilters(column));

  ColumnFilters<String> get entityId => $composableBuilder(
      column: $table.entityId, builder: (column) => ColumnFilters(column));

  ColumnFilters<String> get op => $composableBuilder(
      column: $table.op, builder: (column) => ColumnFilters(column));

  ColumnFilters<int> get baseVersion => $composableBuilder(
      column: $table.baseVersion, builder: (column) => ColumnFilters(column));

  ColumnFilters<String> get payloadJson => $composableBuilder(
      column: $table.payloadJson, builder: (column) => ColumnFilters(column));

  ColumnFilters<String> get status => $composableBuilder(
      column: $table.status, builder: (column) => ColumnFilters(column));

  ColumnFilters<int> get retryCount => $composableBuilder(
      column: $table.retryCount, builder: (column) => ColumnFilters(column));

  ColumnFilters<String> get lastErrorCode => $composableBuilder(
      column: $table.lastErrorCode, builder: (column) => ColumnFilters(column));

  ColumnFilters<String> get lastErrorMessage => $composableBuilder(
      column: $table.lastErrorMessage,
      builder: (column) => ColumnFilters(column));

  ColumnFilters<String> get serverConflictJson => $composableBuilder(
      column: $table.serverConflictJson,
      builder: (column) => ColumnFilters(column));

  ColumnFilters<String> get createdAt => $composableBuilder(
      column: $table.createdAt, builder: (column) => ColumnFilters(column));

  ColumnFilters<String> get updatedAt => $composableBuilder(
      column: $table.updatedAt, builder: (column) => ColumnFilters(column));
}

class $$OutboxDriftTableTableOrderingComposer
    extends Composer<_$LifeOsDriftDatabase, $OutboxDriftTableTable> {
  $$OutboxDriftTableTableOrderingComposer({
    required super.$db,
    required super.$table,
    super.joinBuilder,
    super.$addJoinBuilderToRootComposer,
    super.$removeJoinBuilderFromRootComposer,
  });
  ColumnOrderings<String> get mutationId => $composableBuilder(
      column: $table.mutationId, builder: (column) => ColumnOrderings(column));

  ColumnOrderings<String> get userId => $composableBuilder(
      column: $table.userId, builder: (column) => ColumnOrderings(column));

  ColumnOrderings<String> get entityType => $composableBuilder(
      column: $table.entityType, builder: (column) => ColumnOrderings(column));

  ColumnOrderings<String> get entityId => $composableBuilder(
      column: $table.entityId, builder: (column) => ColumnOrderings(column));

  ColumnOrderings<String> get op => $composableBuilder(
      column: $table.op, builder: (column) => ColumnOrderings(column));

  ColumnOrderings<int> get baseVersion => $composableBuilder(
      column: $table.baseVersion, builder: (column) => ColumnOrderings(column));

  ColumnOrderings<String> get payloadJson => $composableBuilder(
      column: $table.payloadJson, builder: (column) => ColumnOrderings(column));

  ColumnOrderings<String> get status => $composableBuilder(
      column: $table.status, builder: (column) => ColumnOrderings(column));

  ColumnOrderings<int> get retryCount => $composableBuilder(
      column: $table.retryCount, builder: (column) => ColumnOrderings(column));

  ColumnOrderings<String> get lastErrorCode => $composableBuilder(
      column: $table.lastErrorCode,
      builder: (column) => ColumnOrderings(column));

  ColumnOrderings<String> get lastErrorMessage => $composableBuilder(
      column: $table.lastErrorMessage,
      builder: (column) => ColumnOrderings(column));

  ColumnOrderings<String> get serverConflictJson => $composableBuilder(
      column: $table.serverConflictJson,
      builder: (column) => ColumnOrderings(column));

  ColumnOrderings<String> get createdAt => $composableBuilder(
      column: $table.createdAt, builder: (column) => ColumnOrderings(column));

  ColumnOrderings<String> get updatedAt => $composableBuilder(
      column: $table.updatedAt, builder: (column) => ColumnOrderings(column));
}

class $$OutboxDriftTableTableAnnotationComposer
    extends Composer<_$LifeOsDriftDatabase, $OutboxDriftTableTable> {
  $$OutboxDriftTableTableAnnotationComposer({
    required super.$db,
    required super.$table,
    super.joinBuilder,
    super.$addJoinBuilderToRootComposer,
    super.$removeJoinBuilderFromRootComposer,
  });
  GeneratedColumn<String> get mutationId => $composableBuilder(
      column: $table.mutationId, builder: (column) => column);

  GeneratedColumn<String> get userId =>
      $composableBuilder(column: $table.userId, builder: (column) => column);

  GeneratedColumn<String> get entityType => $composableBuilder(
      column: $table.entityType, builder: (column) => column);

  GeneratedColumn<String> get entityId =>
      $composableBuilder(column: $table.entityId, builder: (column) => column);

  GeneratedColumn<String> get op =>
      $composableBuilder(column: $table.op, builder: (column) => column);

  GeneratedColumn<int> get baseVersion => $composableBuilder(
      column: $table.baseVersion, builder: (column) => column);

  GeneratedColumn<String> get payloadJson => $composableBuilder(
      column: $table.payloadJson, builder: (column) => column);

  GeneratedColumn<String> get status =>
      $composableBuilder(column: $table.status, builder: (column) => column);

  GeneratedColumn<int> get retryCount => $composableBuilder(
      column: $table.retryCount, builder: (column) => column);

  GeneratedColumn<String> get lastErrorCode => $composableBuilder(
      column: $table.lastErrorCode, builder: (column) => column);

  GeneratedColumn<String> get lastErrorMessage => $composableBuilder(
      column: $table.lastErrorMessage, builder: (column) => column);

  GeneratedColumn<String> get serverConflictJson => $composableBuilder(
      column: $table.serverConflictJson, builder: (column) => column);

  GeneratedColumn<String> get createdAt =>
      $composableBuilder(column: $table.createdAt, builder: (column) => column);

  GeneratedColumn<String> get updatedAt =>
      $composableBuilder(column: $table.updatedAt, builder: (column) => column);
}

class $$OutboxDriftTableTableTableManager extends RootTableManager<
    _$LifeOsDriftDatabase,
    $OutboxDriftTableTable,
    OutboxDriftTableData,
    $$OutboxDriftTableTableFilterComposer,
    $$OutboxDriftTableTableOrderingComposer,
    $$OutboxDriftTableTableAnnotationComposer,
    $$OutboxDriftTableTableCreateCompanionBuilder,
    $$OutboxDriftTableTableUpdateCompanionBuilder,
    (
      OutboxDriftTableData,
      BaseReferences<_$LifeOsDriftDatabase, $OutboxDriftTableTable,
          OutboxDriftTableData>
    ),
    OutboxDriftTableData,
    PrefetchHooks Function()> {
  $$OutboxDriftTableTableTableManager(
      _$LifeOsDriftDatabase db, $OutboxDriftTableTable table)
      : super(TableManagerState(
          db: db,
          table: table,
          createFilteringComposer: () =>
              $$OutboxDriftTableTableFilterComposer($db: db, $table: table),
          createOrderingComposer: () =>
              $$OutboxDriftTableTableOrderingComposer($db: db, $table: table),
          createComputedFieldComposer: () =>
              $$OutboxDriftTableTableAnnotationComposer($db: db, $table: table),
          updateCompanionCallback: ({
            Value<String> mutationId = const Value.absent(),
            Value<String> userId = const Value.absent(),
            Value<String> entityType = const Value.absent(),
            Value<String> entityId = const Value.absent(),
            Value<String> op = const Value.absent(),
            Value<int> baseVersion = const Value.absent(),
            Value<String> payloadJson = const Value.absent(),
            Value<String> status = const Value.absent(),
            Value<int> retryCount = const Value.absent(),
            Value<String?> lastErrorCode = const Value.absent(),
            Value<String?> lastErrorMessage = const Value.absent(),
            Value<String?> serverConflictJson = const Value.absent(),
            Value<String> createdAt = const Value.absent(),
            Value<String> updatedAt = const Value.absent(),
            Value<int> rowid = const Value.absent(),
          }) =>
              OutboxDriftTableCompanion(
            mutationId: mutationId,
            userId: userId,
            entityType: entityType,
            entityId: entityId,
            op: op,
            baseVersion: baseVersion,
            payloadJson: payloadJson,
            status: status,
            retryCount: retryCount,
            lastErrorCode: lastErrorCode,
            lastErrorMessage: lastErrorMessage,
            serverConflictJson: serverConflictJson,
            createdAt: createdAt,
            updatedAt: updatedAt,
            rowid: rowid,
          ),
          createCompanionCallback: ({
            required String mutationId,
            required String userId,
            required String entityType,
            required String entityId,
            required String op,
            Value<int> baseVersion = const Value.absent(),
            required String payloadJson,
            Value<String> status = const Value.absent(),
            Value<int> retryCount = const Value.absent(),
            Value<String?> lastErrorCode = const Value.absent(),
            Value<String?> lastErrorMessage = const Value.absent(),
            Value<String?> serverConflictJson = const Value.absent(),
            required String createdAt,
            required String updatedAt,
            Value<int> rowid = const Value.absent(),
          }) =>
              OutboxDriftTableCompanion.insert(
            mutationId: mutationId,
            userId: userId,
            entityType: entityType,
            entityId: entityId,
            op: op,
            baseVersion: baseVersion,
            payloadJson: payloadJson,
            status: status,
            retryCount: retryCount,
            lastErrorCode: lastErrorCode,
            lastErrorMessage: lastErrorMessage,
            serverConflictJson: serverConflictJson,
            createdAt: createdAt,
            updatedAt: updatedAt,
            rowid: rowid,
          ),
          withReferenceMapper: (p0) => p0
              .map((e) => (e.readTable(table), BaseReferences(db, table, e)))
              .toList(),
          prefetchHooksCallback: null,
        ));
}

typedef $$OutboxDriftTableTableProcessedTableManager = ProcessedTableManager<
    _$LifeOsDriftDatabase,
    $OutboxDriftTableTable,
    OutboxDriftTableData,
    $$OutboxDriftTableTableFilterComposer,
    $$OutboxDriftTableTableOrderingComposer,
    $$OutboxDriftTableTableAnnotationComposer,
    $$OutboxDriftTableTableCreateCompanionBuilder,
    $$OutboxDriftTableTableUpdateCompanionBuilder,
    (
      OutboxDriftTableData,
      BaseReferences<_$LifeOsDriftDatabase, $OutboxDriftTableTable,
          OutboxDriftTableData>
    ),
    OutboxDriftTableData,
    PrefetchHooks Function()>;
typedef $$EntityBaseSnapshotDriftTableTableCreateCompanionBuilder
    = EntityBaseSnapshotDriftTableCompanion Function({
  required String userId,
  required String entityType,
  required String entityId,
  required int baseVersion,
  required String snapshotJson,
  required String updatedAt,
  Value<int> rowid,
});
typedef $$EntityBaseSnapshotDriftTableTableUpdateCompanionBuilder
    = EntityBaseSnapshotDriftTableCompanion Function({
  Value<String> userId,
  Value<String> entityType,
  Value<String> entityId,
  Value<int> baseVersion,
  Value<String> snapshotJson,
  Value<String> updatedAt,
  Value<int> rowid,
});

class $$EntityBaseSnapshotDriftTableTableFilterComposer extends Composer<
    _$LifeOsDriftDatabase, $EntityBaseSnapshotDriftTableTable> {
  $$EntityBaseSnapshotDriftTableTableFilterComposer({
    required super.$db,
    required super.$table,
    super.joinBuilder,
    super.$addJoinBuilderToRootComposer,
    super.$removeJoinBuilderFromRootComposer,
  });
  ColumnFilters<String> get userId => $composableBuilder(
      column: $table.userId, builder: (column) => ColumnFilters(column));

  ColumnFilters<String> get entityType => $composableBuilder(
      column: $table.entityType, builder: (column) => ColumnFilters(column));

  ColumnFilters<String> get entityId => $composableBuilder(
      column: $table.entityId, builder: (column) => ColumnFilters(column));

  ColumnFilters<int> get baseVersion => $composableBuilder(
      column: $table.baseVersion, builder: (column) => ColumnFilters(column));

  ColumnFilters<String> get snapshotJson => $composableBuilder(
      column: $table.snapshotJson, builder: (column) => ColumnFilters(column));

  ColumnFilters<String> get updatedAt => $composableBuilder(
      column: $table.updatedAt, builder: (column) => ColumnFilters(column));
}

class $$EntityBaseSnapshotDriftTableTableOrderingComposer extends Composer<
    _$LifeOsDriftDatabase, $EntityBaseSnapshotDriftTableTable> {
  $$EntityBaseSnapshotDriftTableTableOrderingComposer({
    required super.$db,
    required super.$table,
    super.joinBuilder,
    super.$addJoinBuilderToRootComposer,
    super.$removeJoinBuilderFromRootComposer,
  });
  ColumnOrderings<String> get userId => $composableBuilder(
      column: $table.userId, builder: (column) => ColumnOrderings(column));

  ColumnOrderings<String> get entityType => $composableBuilder(
      column: $table.entityType, builder: (column) => ColumnOrderings(column));

  ColumnOrderings<String> get entityId => $composableBuilder(
      column: $table.entityId, builder: (column) => ColumnOrderings(column));

  ColumnOrderings<int> get baseVersion => $composableBuilder(
      column: $table.baseVersion, builder: (column) => ColumnOrderings(column));

  ColumnOrderings<String> get snapshotJson => $composableBuilder(
      column: $table.snapshotJson,
      builder: (column) => ColumnOrderings(column));

  ColumnOrderings<String> get updatedAt => $composableBuilder(
      column: $table.updatedAt, builder: (column) => ColumnOrderings(column));
}

class $$EntityBaseSnapshotDriftTableTableAnnotationComposer extends Composer<
    _$LifeOsDriftDatabase, $EntityBaseSnapshotDriftTableTable> {
  $$EntityBaseSnapshotDriftTableTableAnnotationComposer({
    required super.$db,
    required super.$table,
    super.joinBuilder,
    super.$addJoinBuilderToRootComposer,
    super.$removeJoinBuilderFromRootComposer,
  });
  GeneratedColumn<String> get userId =>
      $composableBuilder(column: $table.userId, builder: (column) => column);

  GeneratedColumn<String> get entityType => $composableBuilder(
      column: $table.entityType, builder: (column) => column);

  GeneratedColumn<String> get entityId =>
      $composableBuilder(column: $table.entityId, builder: (column) => column);

  GeneratedColumn<int> get baseVersion => $composableBuilder(
      column: $table.baseVersion, builder: (column) => column);

  GeneratedColumn<String> get snapshotJson => $composableBuilder(
      column: $table.snapshotJson, builder: (column) => column);

  GeneratedColumn<String> get updatedAt =>
      $composableBuilder(column: $table.updatedAt, builder: (column) => column);
}

class $$EntityBaseSnapshotDriftTableTableTableManager extends RootTableManager<
    _$LifeOsDriftDatabase,
    $EntityBaseSnapshotDriftTableTable,
    EntityBaseSnapshotDriftTableData,
    $$EntityBaseSnapshotDriftTableTableFilterComposer,
    $$EntityBaseSnapshotDriftTableTableOrderingComposer,
    $$EntityBaseSnapshotDriftTableTableAnnotationComposer,
    $$EntityBaseSnapshotDriftTableTableCreateCompanionBuilder,
    $$EntityBaseSnapshotDriftTableTableUpdateCompanionBuilder,
    (
      EntityBaseSnapshotDriftTableData,
      BaseReferences<_$LifeOsDriftDatabase, $EntityBaseSnapshotDriftTableTable,
          EntityBaseSnapshotDriftTableData>
    ),
    EntityBaseSnapshotDriftTableData,
    PrefetchHooks Function()> {
  $$EntityBaseSnapshotDriftTableTableTableManager(
      _$LifeOsDriftDatabase db, $EntityBaseSnapshotDriftTableTable table)
      : super(TableManagerState(
          db: db,
          table: table,
          createFilteringComposer: () =>
              $$EntityBaseSnapshotDriftTableTableFilterComposer(
                  $db: db, $table: table),
          createOrderingComposer: () =>
              $$EntityBaseSnapshotDriftTableTableOrderingComposer(
                  $db: db, $table: table),
          createComputedFieldComposer: () =>
              $$EntityBaseSnapshotDriftTableTableAnnotationComposer(
                  $db: db, $table: table),
          updateCompanionCallback: ({
            Value<String> userId = const Value.absent(),
            Value<String> entityType = const Value.absent(),
            Value<String> entityId = const Value.absent(),
            Value<int> baseVersion = const Value.absent(),
            Value<String> snapshotJson = const Value.absent(),
            Value<String> updatedAt = const Value.absent(),
            Value<int> rowid = const Value.absent(),
          }) =>
              EntityBaseSnapshotDriftTableCompanion(
            userId: userId,
            entityType: entityType,
            entityId: entityId,
            baseVersion: baseVersion,
            snapshotJson: snapshotJson,
            updatedAt: updatedAt,
            rowid: rowid,
          ),
          createCompanionCallback: ({
            required String userId,
            required String entityType,
            required String entityId,
            required int baseVersion,
            required String snapshotJson,
            required String updatedAt,
            Value<int> rowid = const Value.absent(),
          }) =>
              EntityBaseSnapshotDriftTableCompanion.insert(
            userId: userId,
            entityType: entityType,
            entityId: entityId,
            baseVersion: baseVersion,
            snapshotJson: snapshotJson,
            updatedAt: updatedAt,
            rowid: rowid,
          ),
          withReferenceMapper: (p0) => p0
              .map((e) => (e.readTable(table), BaseReferences(db, table, e)))
              .toList(),
          prefetchHooksCallback: null,
        ));
}

typedef $$EntityBaseSnapshotDriftTableTableProcessedTableManager
    = ProcessedTableManager<
        _$LifeOsDriftDatabase,
        $EntityBaseSnapshotDriftTableTable,
        EntityBaseSnapshotDriftTableData,
        $$EntityBaseSnapshotDriftTableTableFilterComposer,
        $$EntityBaseSnapshotDriftTableTableOrderingComposer,
        $$EntityBaseSnapshotDriftTableTableAnnotationComposer,
        $$EntityBaseSnapshotDriftTableTableCreateCompanionBuilder,
        $$EntityBaseSnapshotDriftTableTableUpdateCompanionBuilder,
        (
          EntityBaseSnapshotDriftTableData,
          BaseReferences<
              _$LifeOsDriftDatabase,
              $EntityBaseSnapshotDriftTableTable,
              EntityBaseSnapshotDriftTableData>
        ),
        EntityBaseSnapshotDriftTableData,
        PrefetchHooks Function()>;
typedef $$SyncLogDriftTableTableCreateCompanionBuilder
    = SyncLogDriftTableCompanion Function({
  Value<int> id,
  required String userId,
  required String direction,
  required String outcome,
  Value<String?> entityType,
  Value<String?> entityId,
  Value<String?> mutationId,
  Value<int?> latencyMs,
  Value<String?> errorCode,
  Value<String?> details,
  required String createdAt,
});
typedef $$SyncLogDriftTableTableUpdateCompanionBuilder
    = SyncLogDriftTableCompanion Function({
  Value<int> id,
  Value<String> userId,
  Value<String> direction,
  Value<String> outcome,
  Value<String?> entityType,
  Value<String?> entityId,
  Value<String?> mutationId,
  Value<int?> latencyMs,
  Value<String?> errorCode,
  Value<String?> details,
  Value<String> createdAt,
});

class $$SyncLogDriftTableTableFilterComposer
    extends Composer<_$LifeOsDriftDatabase, $SyncLogDriftTableTable> {
  $$SyncLogDriftTableTableFilterComposer({
    required super.$db,
    required super.$table,
    super.joinBuilder,
    super.$addJoinBuilderToRootComposer,
    super.$removeJoinBuilderFromRootComposer,
  });
  ColumnFilters<int> get id => $composableBuilder(
      column: $table.id, builder: (column) => ColumnFilters(column));

  ColumnFilters<String> get userId => $composableBuilder(
      column: $table.userId, builder: (column) => ColumnFilters(column));

  ColumnFilters<String> get direction => $composableBuilder(
      column: $table.direction, builder: (column) => ColumnFilters(column));

  ColumnFilters<String> get outcome => $composableBuilder(
      column: $table.outcome, builder: (column) => ColumnFilters(column));

  ColumnFilters<String> get entityType => $composableBuilder(
      column: $table.entityType, builder: (column) => ColumnFilters(column));

  ColumnFilters<String> get entityId => $composableBuilder(
      column: $table.entityId, builder: (column) => ColumnFilters(column));

  ColumnFilters<String> get mutationId => $composableBuilder(
      column: $table.mutationId, builder: (column) => ColumnFilters(column));

  ColumnFilters<int> get latencyMs => $composableBuilder(
      column: $table.latencyMs, builder: (column) => ColumnFilters(column));

  ColumnFilters<String> get errorCode => $composableBuilder(
      column: $table.errorCode, builder: (column) => ColumnFilters(column));

  ColumnFilters<String> get details => $composableBuilder(
      column: $table.details, builder: (column) => ColumnFilters(column));

  ColumnFilters<String> get createdAt => $composableBuilder(
      column: $table.createdAt, builder: (column) => ColumnFilters(column));
}

class $$SyncLogDriftTableTableOrderingComposer
    extends Composer<_$LifeOsDriftDatabase, $SyncLogDriftTableTable> {
  $$SyncLogDriftTableTableOrderingComposer({
    required super.$db,
    required super.$table,
    super.joinBuilder,
    super.$addJoinBuilderToRootComposer,
    super.$removeJoinBuilderFromRootComposer,
  });
  ColumnOrderings<int> get id => $composableBuilder(
      column: $table.id, builder: (column) => ColumnOrderings(column));

  ColumnOrderings<String> get userId => $composableBuilder(
      column: $table.userId, builder: (column) => ColumnOrderings(column));

  ColumnOrderings<String> get direction => $composableBuilder(
      column: $table.direction, builder: (column) => ColumnOrderings(column));

  ColumnOrderings<String> get outcome => $composableBuilder(
      column: $table.outcome, builder: (column) => ColumnOrderings(column));

  ColumnOrderings<String> get entityType => $composableBuilder(
      column: $table.entityType, builder: (column) => ColumnOrderings(column));

  ColumnOrderings<String> get entityId => $composableBuilder(
      column: $table.entityId, builder: (column) => ColumnOrderings(column));

  ColumnOrderings<String> get mutationId => $composableBuilder(
      column: $table.mutationId, builder: (column) => ColumnOrderings(column));

  ColumnOrderings<int> get latencyMs => $composableBuilder(
      column: $table.latencyMs, builder: (column) => ColumnOrderings(column));

  ColumnOrderings<String> get errorCode => $composableBuilder(
      column: $table.errorCode, builder: (column) => ColumnOrderings(column));

  ColumnOrderings<String> get details => $composableBuilder(
      column: $table.details, builder: (column) => ColumnOrderings(column));

  ColumnOrderings<String> get createdAt => $composableBuilder(
      column: $table.createdAt, builder: (column) => ColumnOrderings(column));
}

class $$SyncLogDriftTableTableAnnotationComposer
    extends Composer<_$LifeOsDriftDatabase, $SyncLogDriftTableTable> {
  $$SyncLogDriftTableTableAnnotationComposer({
    required super.$db,
    required super.$table,
    super.joinBuilder,
    super.$addJoinBuilderToRootComposer,
    super.$removeJoinBuilderFromRootComposer,
  });
  GeneratedColumn<int> get id =>
      $composableBuilder(column: $table.id, builder: (column) => column);

  GeneratedColumn<String> get userId =>
      $composableBuilder(column: $table.userId, builder: (column) => column);

  GeneratedColumn<String> get direction =>
      $composableBuilder(column: $table.direction, builder: (column) => column);

  GeneratedColumn<String> get outcome =>
      $composableBuilder(column: $table.outcome, builder: (column) => column);

  GeneratedColumn<String> get entityType => $composableBuilder(
      column: $table.entityType, builder: (column) => column);

  GeneratedColumn<String> get entityId =>
      $composableBuilder(column: $table.entityId, builder: (column) => column);

  GeneratedColumn<String> get mutationId => $composableBuilder(
      column: $table.mutationId, builder: (column) => column);

  GeneratedColumn<int> get latencyMs =>
      $composableBuilder(column: $table.latencyMs, builder: (column) => column);

  GeneratedColumn<String> get errorCode =>
      $composableBuilder(column: $table.errorCode, builder: (column) => column);

  GeneratedColumn<String> get details =>
      $composableBuilder(column: $table.details, builder: (column) => column);

  GeneratedColumn<String> get createdAt =>
      $composableBuilder(column: $table.createdAt, builder: (column) => column);
}

class $$SyncLogDriftTableTableTableManager extends RootTableManager<
    _$LifeOsDriftDatabase,
    $SyncLogDriftTableTable,
    SyncLogDriftTableData,
    $$SyncLogDriftTableTableFilterComposer,
    $$SyncLogDriftTableTableOrderingComposer,
    $$SyncLogDriftTableTableAnnotationComposer,
    $$SyncLogDriftTableTableCreateCompanionBuilder,
    $$SyncLogDriftTableTableUpdateCompanionBuilder,
    (
      SyncLogDriftTableData,
      BaseReferences<_$LifeOsDriftDatabase, $SyncLogDriftTableTable,
          SyncLogDriftTableData>
    ),
    SyncLogDriftTableData,
    PrefetchHooks Function()> {
  $$SyncLogDriftTableTableTableManager(
      _$LifeOsDriftDatabase db, $SyncLogDriftTableTable table)
      : super(TableManagerState(
          db: db,
          table: table,
          createFilteringComposer: () =>
              $$SyncLogDriftTableTableFilterComposer($db: db, $table: table),
          createOrderingComposer: () =>
              $$SyncLogDriftTableTableOrderingComposer($db: db, $table: table),
          createComputedFieldComposer: () =>
              $$SyncLogDriftTableTableAnnotationComposer(
                  $db: db, $table: table),
          updateCompanionCallback: ({
            Value<int> id = const Value.absent(),
            Value<String> userId = const Value.absent(),
            Value<String> direction = const Value.absent(),
            Value<String> outcome = const Value.absent(),
            Value<String?> entityType = const Value.absent(),
            Value<String?> entityId = const Value.absent(),
            Value<String?> mutationId = const Value.absent(),
            Value<int?> latencyMs = const Value.absent(),
            Value<String?> errorCode = const Value.absent(),
            Value<String?> details = const Value.absent(),
            Value<String> createdAt = const Value.absent(),
          }) =>
              SyncLogDriftTableCompanion(
            id: id,
            userId: userId,
            direction: direction,
            outcome: outcome,
            entityType: entityType,
            entityId: entityId,
            mutationId: mutationId,
            latencyMs: latencyMs,
            errorCode: errorCode,
            details: details,
            createdAt: createdAt,
          ),
          createCompanionCallback: ({
            Value<int> id = const Value.absent(),
            required String userId,
            required String direction,
            required String outcome,
            Value<String?> entityType = const Value.absent(),
            Value<String?> entityId = const Value.absent(),
            Value<String?> mutationId = const Value.absent(),
            Value<int?> latencyMs = const Value.absent(),
            Value<String?> errorCode = const Value.absent(),
            Value<String?> details = const Value.absent(),
            required String createdAt,
          }) =>
              SyncLogDriftTableCompanion.insert(
            id: id,
            userId: userId,
            direction: direction,
            outcome: outcome,
            entityType: entityType,
            entityId: entityId,
            mutationId: mutationId,
            latencyMs: latencyMs,
            errorCode: errorCode,
            details: details,
            createdAt: createdAt,
          ),
          withReferenceMapper: (p0) => p0
              .map((e) => (e.readTable(table), BaseReferences(db, table, e)))
              .toList(),
          prefetchHooksCallback: null,
        ));
}

typedef $$SyncLogDriftTableTableProcessedTableManager = ProcessedTableManager<
    _$LifeOsDriftDatabase,
    $SyncLogDriftTableTable,
    SyncLogDriftTableData,
    $$SyncLogDriftTableTableFilterComposer,
    $$SyncLogDriftTableTableOrderingComposer,
    $$SyncLogDriftTableTableAnnotationComposer,
    $$SyncLogDriftTableTableCreateCompanionBuilder,
    $$SyncLogDriftTableTableUpdateCompanionBuilder,
    (
      SyncLogDriftTableData,
      BaseReferences<_$LifeOsDriftDatabase, $SyncLogDriftTableTable,
          SyncLogDriftTableData>
    ),
    SyncLogDriftTableData,
    PrefetchHooks Function()>;
typedef $$ScheduledNotificationDriftTableTableCreateCompanionBuilder
    = ScheduledNotificationDriftTableCompanion Function({
  required String dedupeKey,
  required String userId,
  required String targetType,
  required String targetId,
  required String fireAtUtc,
  required String title,
  required String body,
  Value<int> redactedLockScreen,
  Value<int> rowid,
});
typedef $$ScheduledNotificationDriftTableTableUpdateCompanionBuilder
    = ScheduledNotificationDriftTableCompanion Function({
  Value<String> dedupeKey,
  Value<String> userId,
  Value<String> targetType,
  Value<String> targetId,
  Value<String> fireAtUtc,
  Value<String> title,
  Value<String> body,
  Value<int> redactedLockScreen,
  Value<int> rowid,
});

class $$ScheduledNotificationDriftTableTableFilterComposer extends Composer<
    _$LifeOsDriftDatabase, $ScheduledNotificationDriftTableTable> {
  $$ScheduledNotificationDriftTableTableFilterComposer({
    required super.$db,
    required super.$table,
    super.joinBuilder,
    super.$addJoinBuilderToRootComposer,
    super.$removeJoinBuilderFromRootComposer,
  });
  ColumnFilters<String> get dedupeKey => $composableBuilder(
      column: $table.dedupeKey, builder: (column) => ColumnFilters(column));

  ColumnFilters<String> get userId => $composableBuilder(
      column: $table.userId, builder: (column) => ColumnFilters(column));

  ColumnFilters<String> get targetType => $composableBuilder(
      column: $table.targetType, builder: (column) => ColumnFilters(column));

  ColumnFilters<String> get targetId => $composableBuilder(
      column: $table.targetId, builder: (column) => ColumnFilters(column));

  ColumnFilters<String> get fireAtUtc => $composableBuilder(
      column: $table.fireAtUtc, builder: (column) => ColumnFilters(column));

  ColumnFilters<String> get title => $composableBuilder(
      column: $table.title, builder: (column) => ColumnFilters(column));

  ColumnFilters<String> get body => $composableBuilder(
      column: $table.body, builder: (column) => ColumnFilters(column));

  ColumnFilters<int> get redactedLockScreen => $composableBuilder(
      column: $table.redactedLockScreen,
      builder: (column) => ColumnFilters(column));
}

class $$ScheduledNotificationDriftTableTableOrderingComposer extends Composer<
    _$LifeOsDriftDatabase, $ScheduledNotificationDriftTableTable> {
  $$ScheduledNotificationDriftTableTableOrderingComposer({
    required super.$db,
    required super.$table,
    super.joinBuilder,
    super.$addJoinBuilderToRootComposer,
    super.$removeJoinBuilderFromRootComposer,
  });
  ColumnOrderings<String> get dedupeKey => $composableBuilder(
      column: $table.dedupeKey, builder: (column) => ColumnOrderings(column));

  ColumnOrderings<String> get userId => $composableBuilder(
      column: $table.userId, builder: (column) => ColumnOrderings(column));

  ColumnOrderings<String> get targetType => $composableBuilder(
      column: $table.targetType, builder: (column) => ColumnOrderings(column));

  ColumnOrderings<String> get targetId => $composableBuilder(
      column: $table.targetId, builder: (column) => ColumnOrderings(column));

  ColumnOrderings<String> get fireAtUtc => $composableBuilder(
      column: $table.fireAtUtc, builder: (column) => ColumnOrderings(column));

  ColumnOrderings<String> get title => $composableBuilder(
      column: $table.title, builder: (column) => ColumnOrderings(column));

  ColumnOrderings<String> get body => $composableBuilder(
      column: $table.body, builder: (column) => ColumnOrderings(column));

  ColumnOrderings<int> get redactedLockScreen => $composableBuilder(
      column: $table.redactedLockScreen,
      builder: (column) => ColumnOrderings(column));
}

class $$ScheduledNotificationDriftTableTableAnnotationComposer extends Composer<
    _$LifeOsDriftDatabase, $ScheduledNotificationDriftTableTable> {
  $$ScheduledNotificationDriftTableTableAnnotationComposer({
    required super.$db,
    required super.$table,
    super.joinBuilder,
    super.$addJoinBuilderToRootComposer,
    super.$removeJoinBuilderFromRootComposer,
  });
  GeneratedColumn<String> get dedupeKey =>
      $composableBuilder(column: $table.dedupeKey, builder: (column) => column);

  GeneratedColumn<String> get userId =>
      $composableBuilder(column: $table.userId, builder: (column) => column);

  GeneratedColumn<String> get targetType => $composableBuilder(
      column: $table.targetType, builder: (column) => column);

  GeneratedColumn<String> get targetId =>
      $composableBuilder(column: $table.targetId, builder: (column) => column);

  GeneratedColumn<String> get fireAtUtc =>
      $composableBuilder(column: $table.fireAtUtc, builder: (column) => column);

  GeneratedColumn<String> get title =>
      $composableBuilder(column: $table.title, builder: (column) => column);

  GeneratedColumn<String> get body =>
      $composableBuilder(column: $table.body, builder: (column) => column);

  GeneratedColumn<int> get redactedLockScreen => $composableBuilder(
      column: $table.redactedLockScreen, builder: (column) => column);
}

class $$ScheduledNotificationDriftTableTableTableManager
    extends RootTableManager<
        _$LifeOsDriftDatabase,
        $ScheduledNotificationDriftTableTable,
        ScheduledNotificationDriftTableData,
        $$ScheduledNotificationDriftTableTableFilterComposer,
        $$ScheduledNotificationDriftTableTableOrderingComposer,
        $$ScheduledNotificationDriftTableTableAnnotationComposer,
        $$ScheduledNotificationDriftTableTableCreateCompanionBuilder,
        $$ScheduledNotificationDriftTableTableUpdateCompanionBuilder,
        (
          ScheduledNotificationDriftTableData,
          BaseReferences<
              _$LifeOsDriftDatabase,
              $ScheduledNotificationDriftTableTable,
              ScheduledNotificationDriftTableData>
        ),
        ScheduledNotificationDriftTableData,
        PrefetchHooks Function()> {
  $$ScheduledNotificationDriftTableTableTableManager(
      _$LifeOsDriftDatabase db, $ScheduledNotificationDriftTableTable table)
      : super(TableManagerState(
          db: db,
          table: table,
          createFilteringComposer: () =>
              $$ScheduledNotificationDriftTableTableFilterComposer(
                  $db: db, $table: table),
          createOrderingComposer: () =>
              $$ScheduledNotificationDriftTableTableOrderingComposer(
                  $db: db, $table: table),
          createComputedFieldComposer: () =>
              $$ScheduledNotificationDriftTableTableAnnotationComposer(
                  $db: db, $table: table),
          updateCompanionCallback: ({
            Value<String> dedupeKey = const Value.absent(),
            Value<String> userId = const Value.absent(),
            Value<String> targetType = const Value.absent(),
            Value<String> targetId = const Value.absent(),
            Value<String> fireAtUtc = const Value.absent(),
            Value<String> title = const Value.absent(),
            Value<String> body = const Value.absent(),
            Value<int> redactedLockScreen = const Value.absent(),
            Value<int> rowid = const Value.absent(),
          }) =>
              ScheduledNotificationDriftTableCompanion(
            dedupeKey: dedupeKey,
            userId: userId,
            targetType: targetType,
            targetId: targetId,
            fireAtUtc: fireAtUtc,
            title: title,
            body: body,
            redactedLockScreen: redactedLockScreen,
            rowid: rowid,
          ),
          createCompanionCallback: ({
            required String dedupeKey,
            required String userId,
            required String targetType,
            required String targetId,
            required String fireAtUtc,
            required String title,
            required String body,
            Value<int> redactedLockScreen = const Value.absent(),
            Value<int> rowid = const Value.absent(),
          }) =>
              ScheduledNotificationDriftTableCompanion.insert(
            dedupeKey: dedupeKey,
            userId: userId,
            targetType: targetType,
            targetId: targetId,
            fireAtUtc: fireAtUtc,
            title: title,
            body: body,
            redactedLockScreen: redactedLockScreen,
            rowid: rowid,
          ),
          withReferenceMapper: (p0) => p0
              .map((e) => (e.readTable(table), BaseReferences(db, table, e)))
              .toList(),
          prefetchHooksCallback: null,
        ));
}

typedef $$ScheduledNotificationDriftTableTableProcessedTableManager
    = ProcessedTableManager<
        _$LifeOsDriftDatabase,
        $ScheduledNotificationDriftTableTable,
        ScheduledNotificationDriftTableData,
        $$ScheduledNotificationDriftTableTableFilterComposer,
        $$ScheduledNotificationDriftTableTableOrderingComposer,
        $$ScheduledNotificationDriftTableTableAnnotationComposer,
        $$ScheduledNotificationDriftTableTableCreateCompanionBuilder,
        $$ScheduledNotificationDriftTableTableUpdateCompanionBuilder,
        (
          ScheduledNotificationDriftTableData,
          BaseReferences<
              _$LifeOsDriftDatabase,
              $ScheduledNotificationDriftTableTable,
              ScheduledNotificationDriftTableData>
        ),
        ScheduledNotificationDriftTableData,
        PrefetchHooks Function()>;
typedef $$DeliveredNotificationDriftTableTableCreateCompanionBuilder
    = DeliveredNotificationDriftTableCompanion Function({
  required String dedupeKey,
  required String userId,
  required String shownAt,
  Value<int> rowid,
});
typedef $$DeliveredNotificationDriftTableTableUpdateCompanionBuilder
    = DeliveredNotificationDriftTableCompanion Function({
  Value<String> dedupeKey,
  Value<String> userId,
  Value<String> shownAt,
  Value<int> rowid,
});

class $$DeliveredNotificationDriftTableTableFilterComposer extends Composer<
    _$LifeOsDriftDatabase, $DeliveredNotificationDriftTableTable> {
  $$DeliveredNotificationDriftTableTableFilterComposer({
    required super.$db,
    required super.$table,
    super.joinBuilder,
    super.$addJoinBuilderToRootComposer,
    super.$removeJoinBuilderFromRootComposer,
  });
  ColumnFilters<String> get dedupeKey => $composableBuilder(
      column: $table.dedupeKey, builder: (column) => ColumnFilters(column));

  ColumnFilters<String> get userId => $composableBuilder(
      column: $table.userId, builder: (column) => ColumnFilters(column));

  ColumnFilters<String> get shownAt => $composableBuilder(
      column: $table.shownAt, builder: (column) => ColumnFilters(column));
}

class $$DeliveredNotificationDriftTableTableOrderingComposer extends Composer<
    _$LifeOsDriftDatabase, $DeliveredNotificationDriftTableTable> {
  $$DeliveredNotificationDriftTableTableOrderingComposer({
    required super.$db,
    required super.$table,
    super.joinBuilder,
    super.$addJoinBuilderToRootComposer,
    super.$removeJoinBuilderFromRootComposer,
  });
  ColumnOrderings<String> get dedupeKey => $composableBuilder(
      column: $table.dedupeKey, builder: (column) => ColumnOrderings(column));

  ColumnOrderings<String> get userId => $composableBuilder(
      column: $table.userId, builder: (column) => ColumnOrderings(column));

  ColumnOrderings<String> get shownAt => $composableBuilder(
      column: $table.shownAt, builder: (column) => ColumnOrderings(column));
}

class $$DeliveredNotificationDriftTableTableAnnotationComposer extends Composer<
    _$LifeOsDriftDatabase, $DeliveredNotificationDriftTableTable> {
  $$DeliveredNotificationDriftTableTableAnnotationComposer({
    required super.$db,
    required super.$table,
    super.joinBuilder,
    super.$addJoinBuilderToRootComposer,
    super.$removeJoinBuilderFromRootComposer,
  });
  GeneratedColumn<String> get dedupeKey =>
      $composableBuilder(column: $table.dedupeKey, builder: (column) => column);

  GeneratedColumn<String> get userId =>
      $composableBuilder(column: $table.userId, builder: (column) => column);

  GeneratedColumn<String> get shownAt =>
      $composableBuilder(column: $table.shownAt, builder: (column) => column);
}

class $$DeliveredNotificationDriftTableTableTableManager
    extends RootTableManager<
        _$LifeOsDriftDatabase,
        $DeliveredNotificationDriftTableTable,
        DeliveredNotificationDriftTableData,
        $$DeliveredNotificationDriftTableTableFilterComposer,
        $$DeliveredNotificationDriftTableTableOrderingComposer,
        $$DeliveredNotificationDriftTableTableAnnotationComposer,
        $$DeliveredNotificationDriftTableTableCreateCompanionBuilder,
        $$DeliveredNotificationDriftTableTableUpdateCompanionBuilder,
        (
          DeliveredNotificationDriftTableData,
          BaseReferences<
              _$LifeOsDriftDatabase,
              $DeliveredNotificationDriftTableTable,
              DeliveredNotificationDriftTableData>
        ),
        DeliveredNotificationDriftTableData,
        PrefetchHooks Function()> {
  $$DeliveredNotificationDriftTableTableTableManager(
      _$LifeOsDriftDatabase db, $DeliveredNotificationDriftTableTable table)
      : super(TableManagerState(
          db: db,
          table: table,
          createFilteringComposer: () =>
              $$DeliveredNotificationDriftTableTableFilterComposer(
                  $db: db, $table: table),
          createOrderingComposer: () =>
              $$DeliveredNotificationDriftTableTableOrderingComposer(
                  $db: db, $table: table),
          createComputedFieldComposer: () =>
              $$DeliveredNotificationDriftTableTableAnnotationComposer(
                  $db: db, $table: table),
          updateCompanionCallback: ({
            Value<String> dedupeKey = const Value.absent(),
            Value<String> userId = const Value.absent(),
            Value<String> shownAt = const Value.absent(),
            Value<int> rowid = const Value.absent(),
          }) =>
              DeliveredNotificationDriftTableCompanion(
            dedupeKey: dedupeKey,
            userId: userId,
            shownAt: shownAt,
            rowid: rowid,
          ),
          createCompanionCallback: ({
            required String dedupeKey,
            required String userId,
            required String shownAt,
            Value<int> rowid = const Value.absent(),
          }) =>
              DeliveredNotificationDriftTableCompanion.insert(
            dedupeKey: dedupeKey,
            userId: userId,
            shownAt: shownAt,
            rowid: rowid,
          ),
          withReferenceMapper: (p0) => p0
              .map((e) => (e.readTable(table), BaseReferences(db, table, e)))
              .toList(),
          prefetchHooksCallback: null,
        ));
}

typedef $$DeliveredNotificationDriftTableTableProcessedTableManager
    = ProcessedTableManager<
        _$LifeOsDriftDatabase,
        $DeliveredNotificationDriftTableTable,
        DeliveredNotificationDriftTableData,
        $$DeliveredNotificationDriftTableTableFilterComposer,
        $$DeliveredNotificationDriftTableTableOrderingComposer,
        $$DeliveredNotificationDriftTableTableAnnotationComposer,
        $$DeliveredNotificationDriftTableTableCreateCompanionBuilder,
        $$DeliveredNotificationDriftTableTableUpdateCompanionBuilder,
        (
          DeliveredNotificationDriftTableData,
          BaseReferences<
              _$LifeOsDriftDatabase,
              $DeliveredNotificationDriftTableTable,
              DeliveredNotificationDriftTableData>
        ),
        DeliveredNotificationDriftTableData,
        PrefetchHooks Function()>;
typedef $$UserSettingsDriftTableTableCreateCompanionBuilder
    = UserSettingsDriftTableCompanion Function({
  required String id,
  required String userId,
  Value<int> version,
  required String updatedAt,
  Value<String?> deletedAt,
  Value<String> syncState,
  required String payloadJson,
  Value<int> rowid,
});
typedef $$UserSettingsDriftTableTableUpdateCompanionBuilder
    = UserSettingsDriftTableCompanion Function({
  Value<String> id,
  Value<String> userId,
  Value<int> version,
  Value<String> updatedAt,
  Value<String?> deletedAt,
  Value<String> syncState,
  Value<String> payloadJson,
  Value<int> rowid,
});

class $$UserSettingsDriftTableTableFilterComposer
    extends Composer<_$LifeOsDriftDatabase, $UserSettingsDriftTableTable> {
  $$UserSettingsDriftTableTableFilterComposer({
    required super.$db,
    required super.$table,
    super.joinBuilder,
    super.$addJoinBuilderToRootComposer,
    super.$removeJoinBuilderFromRootComposer,
  });
  ColumnFilters<String> get id => $composableBuilder(
      column: $table.id, builder: (column) => ColumnFilters(column));

  ColumnFilters<String> get userId => $composableBuilder(
      column: $table.userId, builder: (column) => ColumnFilters(column));

  ColumnFilters<int> get version => $composableBuilder(
      column: $table.version, builder: (column) => ColumnFilters(column));

  ColumnFilters<String> get updatedAt => $composableBuilder(
      column: $table.updatedAt, builder: (column) => ColumnFilters(column));

  ColumnFilters<String> get deletedAt => $composableBuilder(
      column: $table.deletedAt, builder: (column) => ColumnFilters(column));

  ColumnFilters<String> get syncState => $composableBuilder(
      column: $table.syncState, builder: (column) => ColumnFilters(column));

  ColumnFilters<String> get payloadJson => $composableBuilder(
      column: $table.payloadJson, builder: (column) => ColumnFilters(column));
}

class $$UserSettingsDriftTableTableOrderingComposer
    extends Composer<_$LifeOsDriftDatabase, $UserSettingsDriftTableTable> {
  $$UserSettingsDriftTableTableOrderingComposer({
    required super.$db,
    required super.$table,
    super.joinBuilder,
    super.$addJoinBuilderToRootComposer,
    super.$removeJoinBuilderFromRootComposer,
  });
  ColumnOrderings<String> get id => $composableBuilder(
      column: $table.id, builder: (column) => ColumnOrderings(column));

  ColumnOrderings<String> get userId => $composableBuilder(
      column: $table.userId, builder: (column) => ColumnOrderings(column));

  ColumnOrderings<int> get version => $composableBuilder(
      column: $table.version, builder: (column) => ColumnOrderings(column));

  ColumnOrderings<String> get updatedAt => $composableBuilder(
      column: $table.updatedAt, builder: (column) => ColumnOrderings(column));

  ColumnOrderings<String> get deletedAt => $composableBuilder(
      column: $table.deletedAt, builder: (column) => ColumnOrderings(column));

  ColumnOrderings<String> get syncState => $composableBuilder(
      column: $table.syncState, builder: (column) => ColumnOrderings(column));

  ColumnOrderings<String> get payloadJson => $composableBuilder(
      column: $table.payloadJson, builder: (column) => ColumnOrderings(column));
}

class $$UserSettingsDriftTableTableAnnotationComposer
    extends Composer<_$LifeOsDriftDatabase, $UserSettingsDriftTableTable> {
  $$UserSettingsDriftTableTableAnnotationComposer({
    required super.$db,
    required super.$table,
    super.joinBuilder,
    super.$addJoinBuilderToRootComposer,
    super.$removeJoinBuilderFromRootComposer,
  });
  GeneratedColumn<String> get id =>
      $composableBuilder(column: $table.id, builder: (column) => column);

  GeneratedColumn<String> get userId =>
      $composableBuilder(column: $table.userId, builder: (column) => column);

  GeneratedColumn<int> get version =>
      $composableBuilder(column: $table.version, builder: (column) => column);

  GeneratedColumn<String> get updatedAt =>
      $composableBuilder(column: $table.updatedAt, builder: (column) => column);

  GeneratedColumn<String> get deletedAt =>
      $composableBuilder(column: $table.deletedAt, builder: (column) => column);

  GeneratedColumn<String> get syncState =>
      $composableBuilder(column: $table.syncState, builder: (column) => column);

  GeneratedColumn<String> get payloadJson => $composableBuilder(
      column: $table.payloadJson, builder: (column) => column);
}

class $$UserSettingsDriftTableTableTableManager extends RootTableManager<
    _$LifeOsDriftDatabase,
    $UserSettingsDriftTableTable,
    UserSettingsDriftTableData,
    $$UserSettingsDriftTableTableFilterComposer,
    $$UserSettingsDriftTableTableOrderingComposer,
    $$UserSettingsDriftTableTableAnnotationComposer,
    $$UserSettingsDriftTableTableCreateCompanionBuilder,
    $$UserSettingsDriftTableTableUpdateCompanionBuilder,
    (
      UserSettingsDriftTableData,
      BaseReferences<_$LifeOsDriftDatabase, $UserSettingsDriftTableTable,
          UserSettingsDriftTableData>
    ),
    UserSettingsDriftTableData,
    PrefetchHooks Function()> {
  $$UserSettingsDriftTableTableTableManager(
      _$LifeOsDriftDatabase db, $UserSettingsDriftTableTable table)
      : super(TableManagerState(
          db: db,
          table: table,
          createFilteringComposer: () =>
              $$UserSettingsDriftTableTableFilterComposer(
                  $db: db, $table: table),
          createOrderingComposer: () =>
              $$UserSettingsDriftTableTableOrderingComposer(
                  $db: db, $table: table),
          createComputedFieldComposer: () =>
              $$UserSettingsDriftTableTableAnnotationComposer(
                  $db: db, $table: table),
          updateCompanionCallback: ({
            Value<String> id = const Value.absent(),
            Value<String> userId = const Value.absent(),
            Value<int> version = const Value.absent(),
            Value<String> updatedAt = const Value.absent(),
            Value<String?> deletedAt = const Value.absent(),
            Value<String> syncState = const Value.absent(),
            Value<String> payloadJson = const Value.absent(),
            Value<int> rowid = const Value.absent(),
          }) =>
              UserSettingsDriftTableCompanion(
            id: id,
            userId: userId,
            version: version,
            updatedAt: updatedAt,
            deletedAt: deletedAt,
            syncState: syncState,
            payloadJson: payloadJson,
            rowid: rowid,
          ),
          createCompanionCallback: ({
            required String id,
            required String userId,
            Value<int> version = const Value.absent(),
            required String updatedAt,
            Value<String?> deletedAt = const Value.absent(),
            Value<String> syncState = const Value.absent(),
            required String payloadJson,
            Value<int> rowid = const Value.absent(),
          }) =>
              UserSettingsDriftTableCompanion.insert(
            id: id,
            userId: userId,
            version: version,
            updatedAt: updatedAt,
            deletedAt: deletedAt,
            syncState: syncState,
            payloadJson: payloadJson,
            rowid: rowid,
          ),
          withReferenceMapper: (p0) => p0
              .map((e) => (e.readTable(table), BaseReferences(db, table, e)))
              .toList(),
          prefetchHooksCallback: null,
        ));
}

typedef $$UserSettingsDriftTableTableProcessedTableManager
    = ProcessedTableManager<
        _$LifeOsDriftDatabase,
        $UserSettingsDriftTableTable,
        UserSettingsDriftTableData,
        $$UserSettingsDriftTableTableFilterComposer,
        $$UserSettingsDriftTableTableOrderingComposer,
        $$UserSettingsDriftTableTableAnnotationComposer,
        $$UserSettingsDriftTableTableCreateCompanionBuilder,
        $$UserSettingsDriftTableTableUpdateCompanionBuilder,
        (
          UserSettingsDriftTableData,
          BaseReferences<_$LifeOsDriftDatabase, $UserSettingsDriftTableTable,
              UserSettingsDriftTableData>
        ),
        UserSettingsDriftTableData,
        PrefetchHooks Function()>;
typedef $$CategoriesDriftTableTableCreateCompanionBuilder
    = CategoriesDriftTableCompanion Function({
  required String id,
  required String userId,
  Value<int> version,
  required String updatedAt,
  Value<String?> deletedAt,
  Value<String> syncState,
  required String payloadJson,
  Value<int> rowid,
});
typedef $$CategoriesDriftTableTableUpdateCompanionBuilder
    = CategoriesDriftTableCompanion Function({
  Value<String> id,
  Value<String> userId,
  Value<int> version,
  Value<String> updatedAt,
  Value<String?> deletedAt,
  Value<String> syncState,
  Value<String> payloadJson,
  Value<int> rowid,
});

class $$CategoriesDriftTableTableFilterComposer
    extends Composer<_$LifeOsDriftDatabase, $CategoriesDriftTableTable> {
  $$CategoriesDriftTableTableFilterComposer({
    required super.$db,
    required super.$table,
    super.joinBuilder,
    super.$addJoinBuilderToRootComposer,
    super.$removeJoinBuilderFromRootComposer,
  });
  ColumnFilters<String> get id => $composableBuilder(
      column: $table.id, builder: (column) => ColumnFilters(column));

  ColumnFilters<String> get userId => $composableBuilder(
      column: $table.userId, builder: (column) => ColumnFilters(column));

  ColumnFilters<int> get version => $composableBuilder(
      column: $table.version, builder: (column) => ColumnFilters(column));

  ColumnFilters<String> get updatedAt => $composableBuilder(
      column: $table.updatedAt, builder: (column) => ColumnFilters(column));

  ColumnFilters<String> get deletedAt => $composableBuilder(
      column: $table.deletedAt, builder: (column) => ColumnFilters(column));

  ColumnFilters<String> get syncState => $composableBuilder(
      column: $table.syncState, builder: (column) => ColumnFilters(column));

  ColumnFilters<String> get payloadJson => $composableBuilder(
      column: $table.payloadJson, builder: (column) => ColumnFilters(column));
}

class $$CategoriesDriftTableTableOrderingComposer
    extends Composer<_$LifeOsDriftDatabase, $CategoriesDriftTableTable> {
  $$CategoriesDriftTableTableOrderingComposer({
    required super.$db,
    required super.$table,
    super.joinBuilder,
    super.$addJoinBuilderToRootComposer,
    super.$removeJoinBuilderFromRootComposer,
  });
  ColumnOrderings<String> get id => $composableBuilder(
      column: $table.id, builder: (column) => ColumnOrderings(column));

  ColumnOrderings<String> get userId => $composableBuilder(
      column: $table.userId, builder: (column) => ColumnOrderings(column));

  ColumnOrderings<int> get version => $composableBuilder(
      column: $table.version, builder: (column) => ColumnOrderings(column));

  ColumnOrderings<String> get updatedAt => $composableBuilder(
      column: $table.updatedAt, builder: (column) => ColumnOrderings(column));

  ColumnOrderings<String> get deletedAt => $composableBuilder(
      column: $table.deletedAt, builder: (column) => ColumnOrderings(column));

  ColumnOrderings<String> get syncState => $composableBuilder(
      column: $table.syncState, builder: (column) => ColumnOrderings(column));

  ColumnOrderings<String> get payloadJson => $composableBuilder(
      column: $table.payloadJson, builder: (column) => ColumnOrderings(column));
}

class $$CategoriesDriftTableTableAnnotationComposer
    extends Composer<_$LifeOsDriftDatabase, $CategoriesDriftTableTable> {
  $$CategoriesDriftTableTableAnnotationComposer({
    required super.$db,
    required super.$table,
    super.joinBuilder,
    super.$addJoinBuilderToRootComposer,
    super.$removeJoinBuilderFromRootComposer,
  });
  GeneratedColumn<String> get id =>
      $composableBuilder(column: $table.id, builder: (column) => column);

  GeneratedColumn<String> get userId =>
      $composableBuilder(column: $table.userId, builder: (column) => column);

  GeneratedColumn<int> get version =>
      $composableBuilder(column: $table.version, builder: (column) => column);

  GeneratedColumn<String> get updatedAt =>
      $composableBuilder(column: $table.updatedAt, builder: (column) => column);

  GeneratedColumn<String> get deletedAt =>
      $composableBuilder(column: $table.deletedAt, builder: (column) => column);

  GeneratedColumn<String> get syncState =>
      $composableBuilder(column: $table.syncState, builder: (column) => column);

  GeneratedColumn<String> get payloadJson => $composableBuilder(
      column: $table.payloadJson, builder: (column) => column);
}

class $$CategoriesDriftTableTableTableManager extends RootTableManager<
    _$LifeOsDriftDatabase,
    $CategoriesDriftTableTable,
    CategoriesDriftTableData,
    $$CategoriesDriftTableTableFilterComposer,
    $$CategoriesDriftTableTableOrderingComposer,
    $$CategoriesDriftTableTableAnnotationComposer,
    $$CategoriesDriftTableTableCreateCompanionBuilder,
    $$CategoriesDriftTableTableUpdateCompanionBuilder,
    (
      CategoriesDriftTableData,
      BaseReferences<_$LifeOsDriftDatabase, $CategoriesDriftTableTable,
          CategoriesDriftTableData>
    ),
    CategoriesDriftTableData,
    PrefetchHooks Function()> {
  $$CategoriesDriftTableTableTableManager(
      _$LifeOsDriftDatabase db, $CategoriesDriftTableTable table)
      : super(TableManagerState(
          db: db,
          table: table,
          createFilteringComposer: () =>
              $$CategoriesDriftTableTableFilterComposer($db: db, $table: table),
          createOrderingComposer: () =>
              $$CategoriesDriftTableTableOrderingComposer(
                  $db: db, $table: table),
          createComputedFieldComposer: () =>
              $$CategoriesDriftTableTableAnnotationComposer(
                  $db: db, $table: table),
          updateCompanionCallback: ({
            Value<String> id = const Value.absent(),
            Value<String> userId = const Value.absent(),
            Value<int> version = const Value.absent(),
            Value<String> updatedAt = const Value.absent(),
            Value<String?> deletedAt = const Value.absent(),
            Value<String> syncState = const Value.absent(),
            Value<String> payloadJson = const Value.absent(),
            Value<int> rowid = const Value.absent(),
          }) =>
              CategoriesDriftTableCompanion(
            id: id,
            userId: userId,
            version: version,
            updatedAt: updatedAt,
            deletedAt: deletedAt,
            syncState: syncState,
            payloadJson: payloadJson,
            rowid: rowid,
          ),
          createCompanionCallback: ({
            required String id,
            required String userId,
            Value<int> version = const Value.absent(),
            required String updatedAt,
            Value<String?> deletedAt = const Value.absent(),
            Value<String> syncState = const Value.absent(),
            required String payloadJson,
            Value<int> rowid = const Value.absent(),
          }) =>
              CategoriesDriftTableCompanion.insert(
            id: id,
            userId: userId,
            version: version,
            updatedAt: updatedAt,
            deletedAt: deletedAt,
            syncState: syncState,
            payloadJson: payloadJson,
            rowid: rowid,
          ),
          withReferenceMapper: (p0) => p0
              .map((e) => (e.readTable(table), BaseReferences(db, table, e)))
              .toList(),
          prefetchHooksCallback: null,
        ));
}

typedef $$CategoriesDriftTableTableProcessedTableManager
    = ProcessedTableManager<
        _$LifeOsDriftDatabase,
        $CategoriesDriftTableTable,
        CategoriesDriftTableData,
        $$CategoriesDriftTableTableFilterComposer,
        $$CategoriesDriftTableTableOrderingComposer,
        $$CategoriesDriftTableTableAnnotationComposer,
        $$CategoriesDriftTableTableCreateCompanionBuilder,
        $$CategoriesDriftTableTableUpdateCompanionBuilder,
        (
          CategoriesDriftTableData,
          BaseReferences<_$LifeOsDriftDatabase, $CategoriesDriftTableTable,
              CategoriesDriftTableData>
        ),
        CategoriesDriftTableData,
        PrefetchHooks Function()>;
typedef $$ProjectsDriftTableTableCreateCompanionBuilder
    = ProjectsDriftTableCompanion Function({
  required String id,
  required String userId,
  Value<int> version,
  required String updatedAt,
  Value<String?> deletedAt,
  Value<String> syncState,
  required String payloadJson,
  Value<int> rowid,
});
typedef $$ProjectsDriftTableTableUpdateCompanionBuilder
    = ProjectsDriftTableCompanion Function({
  Value<String> id,
  Value<String> userId,
  Value<int> version,
  Value<String> updatedAt,
  Value<String?> deletedAt,
  Value<String> syncState,
  Value<String> payloadJson,
  Value<int> rowid,
});

class $$ProjectsDriftTableTableFilterComposer
    extends Composer<_$LifeOsDriftDatabase, $ProjectsDriftTableTable> {
  $$ProjectsDriftTableTableFilterComposer({
    required super.$db,
    required super.$table,
    super.joinBuilder,
    super.$addJoinBuilderToRootComposer,
    super.$removeJoinBuilderFromRootComposer,
  });
  ColumnFilters<String> get id => $composableBuilder(
      column: $table.id, builder: (column) => ColumnFilters(column));

  ColumnFilters<String> get userId => $composableBuilder(
      column: $table.userId, builder: (column) => ColumnFilters(column));

  ColumnFilters<int> get version => $composableBuilder(
      column: $table.version, builder: (column) => ColumnFilters(column));

  ColumnFilters<String> get updatedAt => $composableBuilder(
      column: $table.updatedAt, builder: (column) => ColumnFilters(column));

  ColumnFilters<String> get deletedAt => $composableBuilder(
      column: $table.deletedAt, builder: (column) => ColumnFilters(column));

  ColumnFilters<String> get syncState => $composableBuilder(
      column: $table.syncState, builder: (column) => ColumnFilters(column));

  ColumnFilters<String> get payloadJson => $composableBuilder(
      column: $table.payloadJson, builder: (column) => ColumnFilters(column));
}

class $$ProjectsDriftTableTableOrderingComposer
    extends Composer<_$LifeOsDriftDatabase, $ProjectsDriftTableTable> {
  $$ProjectsDriftTableTableOrderingComposer({
    required super.$db,
    required super.$table,
    super.joinBuilder,
    super.$addJoinBuilderToRootComposer,
    super.$removeJoinBuilderFromRootComposer,
  });
  ColumnOrderings<String> get id => $composableBuilder(
      column: $table.id, builder: (column) => ColumnOrderings(column));

  ColumnOrderings<String> get userId => $composableBuilder(
      column: $table.userId, builder: (column) => ColumnOrderings(column));

  ColumnOrderings<int> get version => $composableBuilder(
      column: $table.version, builder: (column) => ColumnOrderings(column));

  ColumnOrderings<String> get updatedAt => $composableBuilder(
      column: $table.updatedAt, builder: (column) => ColumnOrderings(column));

  ColumnOrderings<String> get deletedAt => $composableBuilder(
      column: $table.deletedAt, builder: (column) => ColumnOrderings(column));

  ColumnOrderings<String> get syncState => $composableBuilder(
      column: $table.syncState, builder: (column) => ColumnOrderings(column));

  ColumnOrderings<String> get payloadJson => $composableBuilder(
      column: $table.payloadJson, builder: (column) => ColumnOrderings(column));
}

class $$ProjectsDriftTableTableAnnotationComposer
    extends Composer<_$LifeOsDriftDatabase, $ProjectsDriftTableTable> {
  $$ProjectsDriftTableTableAnnotationComposer({
    required super.$db,
    required super.$table,
    super.joinBuilder,
    super.$addJoinBuilderToRootComposer,
    super.$removeJoinBuilderFromRootComposer,
  });
  GeneratedColumn<String> get id =>
      $composableBuilder(column: $table.id, builder: (column) => column);

  GeneratedColumn<String> get userId =>
      $composableBuilder(column: $table.userId, builder: (column) => column);

  GeneratedColumn<int> get version =>
      $composableBuilder(column: $table.version, builder: (column) => column);

  GeneratedColumn<String> get updatedAt =>
      $composableBuilder(column: $table.updatedAt, builder: (column) => column);

  GeneratedColumn<String> get deletedAt =>
      $composableBuilder(column: $table.deletedAt, builder: (column) => column);

  GeneratedColumn<String> get syncState =>
      $composableBuilder(column: $table.syncState, builder: (column) => column);

  GeneratedColumn<String> get payloadJson => $composableBuilder(
      column: $table.payloadJson, builder: (column) => column);
}

class $$ProjectsDriftTableTableTableManager extends RootTableManager<
    _$LifeOsDriftDatabase,
    $ProjectsDriftTableTable,
    ProjectsDriftTableData,
    $$ProjectsDriftTableTableFilterComposer,
    $$ProjectsDriftTableTableOrderingComposer,
    $$ProjectsDriftTableTableAnnotationComposer,
    $$ProjectsDriftTableTableCreateCompanionBuilder,
    $$ProjectsDriftTableTableUpdateCompanionBuilder,
    (
      ProjectsDriftTableData,
      BaseReferences<_$LifeOsDriftDatabase, $ProjectsDriftTableTable,
          ProjectsDriftTableData>
    ),
    ProjectsDriftTableData,
    PrefetchHooks Function()> {
  $$ProjectsDriftTableTableTableManager(
      _$LifeOsDriftDatabase db, $ProjectsDriftTableTable table)
      : super(TableManagerState(
          db: db,
          table: table,
          createFilteringComposer: () =>
              $$ProjectsDriftTableTableFilterComposer($db: db, $table: table),
          createOrderingComposer: () =>
              $$ProjectsDriftTableTableOrderingComposer($db: db, $table: table),
          createComputedFieldComposer: () =>
              $$ProjectsDriftTableTableAnnotationComposer(
                  $db: db, $table: table),
          updateCompanionCallback: ({
            Value<String> id = const Value.absent(),
            Value<String> userId = const Value.absent(),
            Value<int> version = const Value.absent(),
            Value<String> updatedAt = const Value.absent(),
            Value<String?> deletedAt = const Value.absent(),
            Value<String> syncState = const Value.absent(),
            Value<String> payloadJson = const Value.absent(),
            Value<int> rowid = const Value.absent(),
          }) =>
              ProjectsDriftTableCompanion(
            id: id,
            userId: userId,
            version: version,
            updatedAt: updatedAt,
            deletedAt: deletedAt,
            syncState: syncState,
            payloadJson: payloadJson,
            rowid: rowid,
          ),
          createCompanionCallback: ({
            required String id,
            required String userId,
            Value<int> version = const Value.absent(),
            required String updatedAt,
            Value<String?> deletedAt = const Value.absent(),
            Value<String> syncState = const Value.absent(),
            required String payloadJson,
            Value<int> rowid = const Value.absent(),
          }) =>
              ProjectsDriftTableCompanion.insert(
            id: id,
            userId: userId,
            version: version,
            updatedAt: updatedAt,
            deletedAt: deletedAt,
            syncState: syncState,
            payloadJson: payloadJson,
            rowid: rowid,
          ),
          withReferenceMapper: (p0) => p0
              .map((e) => (e.readTable(table), BaseReferences(db, table, e)))
              .toList(),
          prefetchHooksCallback: null,
        ));
}

typedef $$ProjectsDriftTableTableProcessedTableManager = ProcessedTableManager<
    _$LifeOsDriftDatabase,
    $ProjectsDriftTableTable,
    ProjectsDriftTableData,
    $$ProjectsDriftTableTableFilterComposer,
    $$ProjectsDriftTableTableOrderingComposer,
    $$ProjectsDriftTableTableAnnotationComposer,
    $$ProjectsDriftTableTableCreateCompanionBuilder,
    $$ProjectsDriftTableTableUpdateCompanionBuilder,
    (
      ProjectsDriftTableData,
      BaseReferences<_$LifeOsDriftDatabase, $ProjectsDriftTableTable,
          ProjectsDriftTableData>
    ),
    ProjectsDriftTableData,
    PrefetchHooks Function()>;
typedef $$TasksDriftTableTableCreateCompanionBuilder = TasksDriftTableCompanion
    Function({
  required String id,
  required String userId,
  Value<int> version,
  required String updatedAt,
  Value<String?> deletedAt,
  Value<String> syncState,
  required String payloadJson,
  Value<int> rowid,
});
typedef $$TasksDriftTableTableUpdateCompanionBuilder = TasksDriftTableCompanion
    Function({
  Value<String> id,
  Value<String> userId,
  Value<int> version,
  Value<String> updatedAt,
  Value<String?> deletedAt,
  Value<String> syncState,
  Value<String> payloadJson,
  Value<int> rowid,
});

class $$TasksDriftTableTableFilterComposer
    extends Composer<_$LifeOsDriftDatabase, $TasksDriftTableTable> {
  $$TasksDriftTableTableFilterComposer({
    required super.$db,
    required super.$table,
    super.joinBuilder,
    super.$addJoinBuilderToRootComposer,
    super.$removeJoinBuilderFromRootComposer,
  });
  ColumnFilters<String> get id => $composableBuilder(
      column: $table.id, builder: (column) => ColumnFilters(column));

  ColumnFilters<String> get userId => $composableBuilder(
      column: $table.userId, builder: (column) => ColumnFilters(column));

  ColumnFilters<int> get version => $composableBuilder(
      column: $table.version, builder: (column) => ColumnFilters(column));

  ColumnFilters<String> get updatedAt => $composableBuilder(
      column: $table.updatedAt, builder: (column) => ColumnFilters(column));

  ColumnFilters<String> get deletedAt => $composableBuilder(
      column: $table.deletedAt, builder: (column) => ColumnFilters(column));

  ColumnFilters<String> get syncState => $composableBuilder(
      column: $table.syncState, builder: (column) => ColumnFilters(column));

  ColumnFilters<String> get payloadJson => $composableBuilder(
      column: $table.payloadJson, builder: (column) => ColumnFilters(column));
}

class $$TasksDriftTableTableOrderingComposer
    extends Composer<_$LifeOsDriftDatabase, $TasksDriftTableTable> {
  $$TasksDriftTableTableOrderingComposer({
    required super.$db,
    required super.$table,
    super.joinBuilder,
    super.$addJoinBuilderToRootComposer,
    super.$removeJoinBuilderFromRootComposer,
  });
  ColumnOrderings<String> get id => $composableBuilder(
      column: $table.id, builder: (column) => ColumnOrderings(column));

  ColumnOrderings<String> get userId => $composableBuilder(
      column: $table.userId, builder: (column) => ColumnOrderings(column));

  ColumnOrderings<int> get version => $composableBuilder(
      column: $table.version, builder: (column) => ColumnOrderings(column));

  ColumnOrderings<String> get updatedAt => $composableBuilder(
      column: $table.updatedAt, builder: (column) => ColumnOrderings(column));

  ColumnOrderings<String> get deletedAt => $composableBuilder(
      column: $table.deletedAt, builder: (column) => ColumnOrderings(column));

  ColumnOrderings<String> get syncState => $composableBuilder(
      column: $table.syncState, builder: (column) => ColumnOrderings(column));

  ColumnOrderings<String> get payloadJson => $composableBuilder(
      column: $table.payloadJson, builder: (column) => ColumnOrderings(column));
}

class $$TasksDriftTableTableAnnotationComposer
    extends Composer<_$LifeOsDriftDatabase, $TasksDriftTableTable> {
  $$TasksDriftTableTableAnnotationComposer({
    required super.$db,
    required super.$table,
    super.joinBuilder,
    super.$addJoinBuilderToRootComposer,
    super.$removeJoinBuilderFromRootComposer,
  });
  GeneratedColumn<String> get id =>
      $composableBuilder(column: $table.id, builder: (column) => column);

  GeneratedColumn<String> get userId =>
      $composableBuilder(column: $table.userId, builder: (column) => column);

  GeneratedColumn<int> get version =>
      $composableBuilder(column: $table.version, builder: (column) => column);

  GeneratedColumn<String> get updatedAt =>
      $composableBuilder(column: $table.updatedAt, builder: (column) => column);

  GeneratedColumn<String> get deletedAt =>
      $composableBuilder(column: $table.deletedAt, builder: (column) => column);

  GeneratedColumn<String> get syncState =>
      $composableBuilder(column: $table.syncState, builder: (column) => column);

  GeneratedColumn<String> get payloadJson => $composableBuilder(
      column: $table.payloadJson, builder: (column) => column);
}

class $$TasksDriftTableTableTableManager extends RootTableManager<
    _$LifeOsDriftDatabase,
    $TasksDriftTableTable,
    TasksDriftTableData,
    $$TasksDriftTableTableFilterComposer,
    $$TasksDriftTableTableOrderingComposer,
    $$TasksDriftTableTableAnnotationComposer,
    $$TasksDriftTableTableCreateCompanionBuilder,
    $$TasksDriftTableTableUpdateCompanionBuilder,
    (
      TasksDriftTableData,
      BaseReferences<_$LifeOsDriftDatabase, $TasksDriftTableTable,
          TasksDriftTableData>
    ),
    TasksDriftTableData,
    PrefetchHooks Function()> {
  $$TasksDriftTableTableTableManager(
      _$LifeOsDriftDatabase db, $TasksDriftTableTable table)
      : super(TableManagerState(
          db: db,
          table: table,
          createFilteringComposer: () =>
              $$TasksDriftTableTableFilterComposer($db: db, $table: table),
          createOrderingComposer: () =>
              $$TasksDriftTableTableOrderingComposer($db: db, $table: table),
          createComputedFieldComposer: () =>
              $$TasksDriftTableTableAnnotationComposer($db: db, $table: table),
          updateCompanionCallback: ({
            Value<String> id = const Value.absent(),
            Value<String> userId = const Value.absent(),
            Value<int> version = const Value.absent(),
            Value<String> updatedAt = const Value.absent(),
            Value<String?> deletedAt = const Value.absent(),
            Value<String> syncState = const Value.absent(),
            Value<String> payloadJson = const Value.absent(),
            Value<int> rowid = const Value.absent(),
          }) =>
              TasksDriftTableCompanion(
            id: id,
            userId: userId,
            version: version,
            updatedAt: updatedAt,
            deletedAt: deletedAt,
            syncState: syncState,
            payloadJson: payloadJson,
            rowid: rowid,
          ),
          createCompanionCallback: ({
            required String id,
            required String userId,
            Value<int> version = const Value.absent(),
            required String updatedAt,
            Value<String?> deletedAt = const Value.absent(),
            Value<String> syncState = const Value.absent(),
            required String payloadJson,
            Value<int> rowid = const Value.absent(),
          }) =>
              TasksDriftTableCompanion.insert(
            id: id,
            userId: userId,
            version: version,
            updatedAt: updatedAt,
            deletedAt: deletedAt,
            syncState: syncState,
            payloadJson: payloadJson,
            rowid: rowid,
          ),
          withReferenceMapper: (p0) => p0
              .map((e) => (e.readTable(table), BaseReferences(db, table, e)))
              .toList(),
          prefetchHooksCallback: null,
        ));
}

typedef $$TasksDriftTableTableProcessedTableManager = ProcessedTableManager<
    _$LifeOsDriftDatabase,
    $TasksDriftTableTable,
    TasksDriftTableData,
    $$TasksDriftTableTableFilterComposer,
    $$TasksDriftTableTableOrderingComposer,
    $$TasksDriftTableTableAnnotationComposer,
    $$TasksDriftTableTableCreateCompanionBuilder,
    $$TasksDriftTableTableUpdateCompanionBuilder,
    (
      TasksDriftTableData,
      BaseReferences<_$LifeOsDriftDatabase, $TasksDriftTableTable,
          TasksDriftTableData>
    ),
    TasksDriftTableData,
    PrefetchHooks Function()>;
typedef $$HabitsDriftTableTableCreateCompanionBuilder
    = HabitsDriftTableCompanion Function({
  required String id,
  required String userId,
  Value<int> version,
  required String updatedAt,
  Value<String?> deletedAt,
  Value<String> syncState,
  required String payloadJson,
  Value<int> rowid,
});
typedef $$HabitsDriftTableTableUpdateCompanionBuilder
    = HabitsDriftTableCompanion Function({
  Value<String> id,
  Value<String> userId,
  Value<int> version,
  Value<String> updatedAt,
  Value<String?> deletedAt,
  Value<String> syncState,
  Value<String> payloadJson,
  Value<int> rowid,
});

class $$HabitsDriftTableTableFilterComposer
    extends Composer<_$LifeOsDriftDatabase, $HabitsDriftTableTable> {
  $$HabitsDriftTableTableFilterComposer({
    required super.$db,
    required super.$table,
    super.joinBuilder,
    super.$addJoinBuilderToRootComposer,
    super.$removeJoinBuilderFromRootComposer,
  });
  ColumnFilters<String> get id => $composableBuilder(
      column: $table.id, builder: (column) => ColumnFilters(column));

  ColumnFilters<String> get userId => $composableBuilder(
      column: $table.userId, builder: (column) => ColumnFilters(column));

  ColumnFilters<int> get version => $composableBuilder(
      column: $table.version, builder: (column) => ColumnFilters(column));

  ColumnFilters<String> get updatedAt => $composableBuilder(
      column: $table.updatedAt, builder: (column) => ColumnFilters(column));

  ColumnFilters<String> get deletedAt => $composableBuilder(
      column: $table.deletedAt, builder: (column) => ColumnFilters(column));

  ColumnFilters<String> get syncState => $composableBuilder(
      column: $table.syncState, builder: (column) => ColumnFilters(column));

  ColumnFilters<String> get payloadJson => $composableBuilder(
      column: $table.payloadJson, builder: (column) => ColumnFilters(column));
}

class $$HabitsDriftTableTableOrderingComposer
    extends Composer<_$LifeOsDriftDatabase, $HabitsDriftTableTable> {
  $$HabitsDriftTableTableOrderingComposer({
    required super.$db,
    required super.$table,
    super.joinBuilder,
    super.$addJoinBuilderToRootComposer,
    super.$removeJoinBuilderFromRootComposer,
  });
  ColumnOrderings<String> get id => $composableBuilder(
      column: $table.id, builder: (column) => ColumnOrderings(column));

  ColumnOrderings<String> get userId => $composableBuilder(
      column: $table.userId, builder: (column) => ColumnOrderings(column));

  ColumnOrderings<int> get version => $composableBuilder(
      column: $table.version, builder: (column) => ColumnOrderings(column));

  ColumnOrderings<String> get updatedAt => $composableBuilder(
      column: $table.updatedAt, builder: (column) => ColumnOrderings(column));

  ColumnOrderings<String> get deletedAt => $composableBuilder(
      column: $table.deletedAt, builder: (column) => ColumnOrderings(column));

  ColumnOrderings<String> get syncState => $composableBuilder(
      column: $table.syncState, builder: (column) => ColumnOrderings(column));

  ColumnOrderings<String> get payloadJson => $composableBuilder(
      column: $table.payloadJson, builder: (column) => ColumnOrderings(column));
}

class $$HabitsDriftTableTableAnnotationComposer
    extends Composer<_$LifeOsDriftDatabase, $HabitsDriftTableTable> {
  $$HabitsDriftTableTableAnnotationComposer({
    required super.$db,
    required super.$table,
    super.joinBuilder,
    super.$addJoinBuilderToRootComposer,
    super.$removeJoinBuilderFromRootComposer,
  });
  GeneratedColumn<String> get id =>
      $composableBuilder(column: $table.id, builder: (column) => column);

  GeneratedColumn<String> get userId =>
      $composableBuilder(column: $table.userId, builder: (column) => column);

  GeneratedColumn<int> get version =>
      $composableBuilder(column: $table.version, builder: (column) => column);

  GeneratedColumn<String> get updatedAt =>
      $composableBuilder(column: $table.updatedAt, builder: (column) => column);

  GeneratedColumn<String> get deletedAt =>
      $composableBuilder(column: $table.deletedAt, builder: (column) => column);

  GeneratedColumn<String> get syncState =>
      $composableBuilder(column: $table.syncState, builder: (column) => column);

  GeneratedColumn<String> get payloadJson => $composableBuilder(
      column: $table.payloadJson, builder: (column) => column);
}

class $$HabitsDriftTableTableTableManager extends RootTableManager<
    _$LifeOsDriftDatabase,
    $HabitsDriftTableTable,
    HabitsDriftTableData,
    $$HabitsDriftTableTableFilterComposer,
    $$HabitsDriftTableTableOrderingComposer,
    $$HabitsDriftTableTableAnnotationComposer,
    $$HabitsDriftTableTableCreateCompanionBuilder,
    $$HabitsDriftTableTableUpdateCompanionBuilder,
    (
      HabitsDriftTableData,
      BaseReferences<_$LifeOsDriftDatabase, $HabitsDriftTableTable,
          HabitsDriftTableData>
    ),
    HabitsDriftTableData,
    PrefetchHooks Function()> {
  $$HabitsDriftTableTableTableManager(
      _$LifeOsDriftDatabase db, $HabitsDriftTableTable table)
      : super(TableManagerState(
          db: db,
          table: table,
          createFilteringComposer: () =>
              $$HabitsDriftTableTableFilterComposer($db: db, $table: table),
          createOrderingComposer: () =>
              $$HabitsDriftTableTableOrderingComposer($db: db, $table: table),
          createComputedFieldComposer: () =>
              $$HabitsDriftTableTableAnnotationComposer($db: db, $table: table),
          updateCompanionCallback: ({
            Value<String> id = const Value.absent(),
            Value<String> userId = const Value.absent(),
            Value<int> version = const Value.absent(),
            Value<String> updatedAt = const Value.absent(),
            Value<String?> deletedAt = const Value.absent(),
            Value<String> syncState = const Value.absent(),
            Value<String> payloadJson = const Value.absent(),
            Value<int> rowid = const Value.absent(),
          }) =>
              HabitsDriftTableCompanion(
            id: id,
            userId: userId,
            version: version,
            updatedAt: updatedAt,
            deletedAt: deletedAt,
            syncState: syncState,
            payloadJson: payloadJson,
            rowid: rowid,
          ),
          createCompanionCallback: ({
            required String id,
            required String userId,
            Value<int> version = const Value.absent(),
            required String updatedAt,
            Value<String?> deletedAt = const Value.absent(),
            Value<String> syncState = const Value.absent(),
            required String payloadJson,
            Value<int> rowid = const Value.absent(),
          }) =>
              HabitsDriftTableCompanion.insert(
            id: id,
            userId: userId,
            version: version,
            updatedAt: updatedAt,
            deletedAt: deletedAt,
            syncState: syncState,
            payloadJson: payloadJson,
            rowid: rowid,
          ),
          withReferenceMapper: (p0) => p0
              .map((e) => (e.readTable(table), BaseReferences(db, table, e)))
              .toList(),
          prefetchHooksCallback: null,
        ));
}

typedef $$HabitsDriftTableTableProcessedTableManager = ProcessedTableManager<
    _$LifeOsDriftDatabase,
    $HabitsDriftTableTable,
    HabitsDriftTableData,
    $$HabitsDriftTableTableFilterComposer,
    $$HabitsDriftTableTableOrderingComposer,
    $$HabitsDriftTableTableAnnotationComposer,
    $$HabitsDriftTableTableCreateCompanionBuilder,
    $$HabitsDriftTableTableUpdateCompanionBuilder,
    (
      HabitsDriftTableData,
      BaseReferences<_$LifeOsDriftDatabase, $HabitsDriftTableTable,
          HabitsDriftTableData>
    ),
    HabitsDriftTableData,
    PrefetchHooks Function()>;
typedef $$HabitCheckinsDriftTableTableCreateCompanionBuilder
    = HabitCheckinsDriftTableCompanion Function({
  required String id,
  required String userId,
  Value<int> version,
  required String updatedAt,
  Value<String?> deletedAt,
  Value<String> syncState,
  required String payloadJson,
  Value<int> rowid,
});
typedef $$HabitCheckinsDriftTableTableUpdateCompanionBuilder
    = HabitCheckinsDriftTableCompanion Function({
  Value<String> id,
  Value<String> userId,
  Value<int> version,
  Value<String> updatedAt,
  Value<String?> deletedAt,
  Value<String> syncState,
  Value<String> payloadJson,
  Value<int> rowid,
});

class $$HabitCheckinsDriftTableTableFilterComposer
    extends Composer<_$LifeOsDriftDatabase, $HabitCheckinsDriftTableTable> {
  $$HabitCheckinsDriftTableTableFilterComposer({
    required super.$db,
    required super.$table,
    super.joinBuilder,
    super.$addJoinBuilderToRootComposer,
    super.$removeJoinBuilderFromRootComposer,
  });
  ColumnFilters<String> get id => $composableBuilder(
      column: $table.id, builder: (column) => ColumnFilters(column));

  ColumnFilters<String> get userId => $composableBuilder(
      column: $table.userId, builder: (column) => ColumnFilters(column));

  ColumnFilters<int> get version => $composableBuilder(
      column: $table.version, builder: (column) => ColumnFilters(column));

  ColumnFilters<String> get updatedAt => $composableBuilder(
      column: $table.updatedAt, builder: (column) => ColumnFilters(column));

  ColumnFilters<String> get deletedAt => $composableBuilder(
      column: $table.deletedAt, builder: (column) => ColumnFilters(column));

  ColumnFilters<String> get syncState => $composableBuilder(
      column: $table.syncState, builder: (column) => ColumnFilters(column));

  ColumnFilters<String> get payloadJson => $composableBuilder(
      column: $table.payloadJson, builder: (column) => ColumnFilters(column));
}

class $$HabitCheckinsDriftTableTableOrderingComposer
    extends Composer<_$LifeOsDriftDatabase, $HabitCheckinsDriftTableTable> {
  $$HabitCheckinsDriftTableTableOrderingComposer({
    required super.$db,
    required super.$table,
    super.joinBuilder,
    super.$addJoinBuilderToRootComposer,
    super.$removeJoinBuilderFromRootComposer,
  });
  ColumnOrderings<String> get id => $composableBuilder(
      column: $table.id, builder: (column) => ColumnOrderings(column));

  ColumnOrderings<String> get userId => $composableBuilder(
      column: $table.userId, builder: (column) => ColumnOrderings(column));

  ColumnOrderings<int> get version => $composableBuilder(
      column: $table.version, builder: (column) => ColumnOrderings(column));

  ColumnOrderings<String> get updatedAt => $composableBuilder(
      column: $table.updatedAt, builder: (column) => ColumnOrderings(column));

  ColumnOrderings<String> get deletedAt => $composableBuilder(
      column: $table.deletedAt, builder: (column) => ColumnOrderings(column));

  ColumnOrderings<String> get syncState => $composableBuilder(
      column: $table.syncState, builder: (column) => ColumnOrderings(column));

  ColumnOrderings<String> get payloadJson => $composableBuilder(
      column: $table.payloadJson, builder: (column) => ColumnOrderings(column));
}

class $$HabitCheckinsDriftTableTableAnnotationComposer
    extends Composer<_$LifeOsDriftDatabase, $HabitCheckinsDriftTableTable> {
  $$HabitCheckinsDriftTableTableAnnotationComposer({
    required super.$db,
    required super.$table,
    super.joinBuilder,
    super.$addJoinBuilderToRootComposer,
    super.$removeJoinBuilderFromRootComposer,
  });
  GeneratedColumn<String> get id =>
      $composableBuilder(column: $table.id, builder: (column) => column);

  GeneratedColumn<String> get userId =>
      $composableBuilder(column: $table.userId, builder: (column) => column);

  GeneratedColumn<int> get version =>
      $composableBuilder(column: $table.version, builder: (column) => column);

  GeneratedColumn<String> get updatedAt =>
      $composableBuilder(column: $table.updatedAt, builder: (column) => column);

  GeneratedColumn<String> get deletedAt =>
      $composableBuilder(column: $table.deletedAt, builder: (column) => column);

  GeneratedColumn<String> get syncState =>
      $composableBuilder(column: $table.syncState, builder: (column) => column);

  GeneratedColumn<String> get payloadJson => $composableBuilder(
      column: $table.payloadJson, builder: (column) => column);
}

class $$HabitCheckinsDriftTableTableTableManager extends RootTableManager<
    _$LifeOsDriftDatabase,
    $HabitCheckinsDriftTableTable,
    HabitCheckinsDriftTableData,
    $$HabitCheckinsDriftTableTableFilterComposer,
    $$HabitCheckinsDriftTableTableOrderingComposer,
    $$HabitCheckinsDriftTableTableAnnotationComposer,
    $$HabitCheckinsDriftTableTableCreateCompanionBuilder,
    $$HabitCheckinsDriftTableTableUpdateCompanionBuilder,
    (
      HabitCheckinsDriftTableData,
      BaseReferences<_$LifeOsDriftDatabase, $HabitCheckinsDriftTableTable,
          HabitCheckinsDriftTableData>
    ),
    HabitCheckinsDriftTableData,
    PrefetchHooks Function()> {
  $$HabitCheckinsDriftTableTableTableManager(
      _$LifeOsDriftDatabase db, $HabitCheckinsDriftTableTable table)
      : super(TableManagerState(
          db: db,
          table: table,
          createFilteringComposer: () =>
              $$HabitCheckinsDriftTableTableFilterComposer(
                  $db: db, $table: table),
          createOrderingComposer: () =>
              $$HabitCheckinsDriftTableTableOrderingComposer(
                  $db: db, $table: table),
          createComputedFieldComposer: () =>
              $$HabitCheckinsDriftTableTableAnnotationComposer(
                  $db: db, $table: table),
          updateCompanionCallback: ({
            Value<String> id = const Value.absent(),
            Value<String> userId = const Value.absent(),
            Value<int> version = const Value.absent(),
            Value<String> updatedAt = const Value.absent(),
            Value<String?> deletedAt = const Value.absent(),
            Value<String> syncState = const Value.absent(),
            Value<String> payloadJson = const Value.absent(),
            Value<int> rowid = const Value.absent(),
          }) =>
              HabitCheckinsDriftTableCompanion(
            id: id,
            userId: userId,
            version: version,
            updatedAt: updatedAt,
            deletedAt: deletedAt,
            syncState: syncState,
            payloadJson: payloadJson,
            rowid: rowid,
          ),
          createCompanionCallback: ({
            required String id,
            required String userId,
            Value<int> version = const Value.absent(),
            required String updatedAt,
            Value<String?> deletedAt = const Value.absent(),
            Value<String> syncState = const Value.absent(),
            required String payloadJson,
            Value<int> rowid = const Value.absent(),
          }) =>
              HabitCheckinsDriftTableCompanion.insert(
            id: id,
            userId: userId,
            version: version,
            updatedAt: updatedAt,
            deletedAt: deletedAt,
            syncState: syncState,
            payloadJson: payloadJson,
            rowid: rowid,
          ),
          withReferenceMapper: (p0) => p0
              .map((e) => (e.readTable(table), BaseReferences(db, table, e)))
              .toList(),
          prefetchHooksCallback: null,
        ));
}

typedef $$HabitCheckinsDriftTableTableProcessedTableManager
    = ProcessedTableManager<
        _$LifeOsDriftDatabase,
        $HabitCheckinsDriftTableTable,
        HabitCheckinsDriftTableData,
        $$HabitCheckinsDriftTableTableFilterComposer,
        $$HabitCheckinsDriftTableTableOrderingComposer,
        $$HabitCheckinsDriftTableTableAnnotationComposer,
        $$HabitCheckinsDriftTableTableCreateCompanionBuilder,
        $$HabitCheckinsDriftTableTableUpdateCompanionBuilder,
        (
          HabitCheckinsDriftTableData,
          BaseReferences<_$LifeOsDriftDatabase, $HabitCheckinsDriftTableTable,
              HabitCheckinsDriftTableData>
        ),
        HabitCheckinsDriftTableData,
        PrefetchHooks Function()>;
typedef $$RoutinesDriftTableTableCreateCompanionBuilder
    = RoutinesDriftTableCompanion Function({
  required String id,
  required String userId,
  Value<int> version,
  required String updatedAt,
  Value<String?> deletedAt,
  Value<String> syncState,
  required String payloadJson,
  Value<int> rowid,
});
typedef $$RoutinesDriftTableTableUpdateCompanionBuilder
    = RoutinesDriftTableCompanion Function({
  Value<String> id,
  Value<String> userId,
  Value<int> version,
  Value<String> updatedAt,
  Value<String?> deletedAt,
  Value<String> syncState,
  Value<String> payloadJson,
  Value<int> rowid,
});

class $$RoutinesDriftTableTableFilterComposer
    extends Composer<_$LifeOsDriftDatabase, $RoutinesDriftTableTable> {
  $$RoutinesDriftTableTableFilterComposer({
    required super.$db,
    required super.$table,
    super.joinBuilder,
    super.$addJoinBuilderToRootComposer,
    super.$removeJoinBuilderFromRootComposer,
  });
  ColumnFilters<String> get id => $composableBuilder(
      column: $table.id, builder: (column) => ColumnFilters(column));

  ColumnFilters<String> get userId => $composableBuilder(
      column: $table.userId, builder: (column) => ColumnFilters(column));

  ColumnFilters<int> get version => $composableBuilder(
      column: $table.version, builder: (column) => ColumnFilters(column));

  ColumnFilters<String> get updatedAt => $composableBuilder(
      column: $table.updatedAt, builder: (column) => ColumnFilters(column));

  ColumnFilters<String> get deletedAt => $composableBuilder(
      column: $table.deletedAt, builder: (column) => ColumnFilters(column));

  ColumnFilters<String> get syncState => $composableBuilder(
      column: $table.syncState, builder: (column) => ColumnFilters(column));

  ColumnFilters<String> get payloadJson => $composableBuilder(
      column: $table.payloadJson, builder: (column) => ColumnFilters(column));
}

class $$RoutinesDriftTableTableOrderingComposer
    extends Composer<_$LifeOsDriftDatabase, $RoutinesDriftTableTable> {
  $$RoutinesDriftTableTableOrderingComposer({
    required super.$db,
    required super.$table,
    super.joinBuilder,
    super.$addJoinBuilderToRootComposer,
    super.$removeJoinBuilderFromRootComposer,
  });
  ColumnOrderings<String> get id => $composableBuilder(
      column: $table.id, builder: (column) => ColumnOrderings(column));

  ColumnOrderings<String> get userId => $composableBuilder(
      column: $table.userId, builder: (column) => ColumnOrderings(column));

  ColumnOrderings<int> get version => $composableBuilder(
      column: $table.version, builder: (column) => ColumnOrderings(column));

  ColumnOrderings<String> get updatedAt => $composableBuilder(
      column: $table.updatedAt, builder: (column) => ColumnOrderings(column));

  ColumnOrderings<String> get deletedAt => $composableBuilder(
      column: $table.deletedAt, builder: (column) => ColumnOrderings(column));

  ColumnOrderings<String> get syncState => $composableBuilder(
      column: $table.syncState, builder: (column) => ColumnOrderings(column));

  ColumnOrderings<String> get payloadJson => $composableBuilder(
      column: $table.payloadJson, builder: (column) => ColumnOrderings(column));
}

class $$RoutinesDriftTableTableAnnotationComposer
    extends Composer<_$LifeOsDriftDatabase, $RoutinesDriftTableTable> {
  $$RoutinesDriftTableTableAnnotationComposer({
    required super.$db,
    required super.$table,
    super.joinBuilder,
    super.$addJoinBuilderToRootComposer,
    super.$removeJoinBuilderFromRootComposer,
  });
  GeneratedColumn<String> get id =>
      $composableBuilder(column: $table.id, builder: (column) => column);

  GeneratedColumn<String> get userId =>
      $composableBuilder(column: $table.userId, builder: (column) => column);

  GeneratedColumn<int> get version =>
      $composableBuilder(column: $table.version, builder: (column) => column);

  GeneratedColumn<String> get updatedAt =>
      $composableBuilder(column: $table.updatedAt, builder: (column) => column);

  GeneratedColumn<String> get deletedAt =>
      $composableBuilder(column: $table.deletedAt, builder: (column) => column);

  GeneratedColumn<String> get syncState =>
      $composableBuilder(column: $table.syncState, builder: (column) => column);

  GeneratedColumn<String> get payloadJson => $composableBuilder(
      column: $table.payloadJson, builder: (column) => column);
}

class $$RoutinesDriftTableTableTableManager extends RootTableManager<
    _$LifeOsDriftDatabase,
    $RoutinesDriftTableTable,
    RoutinesDriftTableData,
    $$RoutinesDriftTableTableFilterComposer,
    $$RoutinesDriftTableTableOrderingComposer,
    $$RoutinesDriftTableTableAnnotationComposer,
    $$RoutinesDriftTableTableCreateCompanionBuilder,
    $$RoutinesDriftTableTableUpdateCompanionBuilder,
    (
      RoutinesDriftTableData,
      BaseReferences<_$LifeOsDriftDatabase, $RoutinesDriftTableTable,
          RoutinesDriftTableData>
    ),
    RoutinesDriftTableData,
    PrefetchHooks Function()> {
  $$RoutinesDriftTableTableTableManager(
      _$LifeOsDriftDatabase db, $RoutinesDriftTableTable table)
      : super(TableManagerState(
          db: db,
          table: table,
          createFilteringComposer: () =>
              $$RoutinesDriftTableTableFilterComposer($db: db, $table: table),
          createOrderingComposer: () =>
              $$RoutinesDriftTableTableOrderingComposer($db: db, $table: table),
          createComputedFieldComposer: () =>
              $$RoutinesDriftTableTableAnnotationComposer(
                  $db: db, $table: table),
          updateCompanionCallback: ({
            Value<String> id = const Value.absent(),
            Value<String> userId = const Value.absent(),
            Value<int> version = const Value.absent(),
            Value<String> updatedAt = const Value.absent(),
            Value<String?> deletedAt = const Value.absent(),
            Value<String> syncState = const Value.absent(),
            Value<String> payloadJson = const Value.absent(),
            Value<int> rowid = const Value.absent(),
          }) =>
              RoutinesDriftTableCompanion(
            id: id,
            userId: userId,
            version: version,
            updatedAt: updatedAt,
            deletedAt: deletedAt,
            syncState: syncState,
            payloadJson: payloadJson,
            rowid: rowid,
          ),
          createCompanionCallback: ({
            required String id,
            required String userId,
            Value<int> version = const Value.absent(),
            required String updatedAt,
            Value<String?> deletedAt = const Value.absent(),
            Value<String> syncState = const Value.absent(),
            required String payloadJson,
            Value<int> rowid = const Value.absent(),
          }) =>
              RoutinesDriftTableCompanion.insert(
            id: id,
            userId: userId,
            version: version,
            updatedAt: updatedAt,
            deletedAt: deletedAt,
            syncState: syncState,
            payloadJson: payloadJson,
            rowid: rowid,
          ),
          withReferenceMapper: (p0) => p0
              .map((e) => (e.readTable(table), BaseReferences(db, table, e)))
              .toList(),
          prefetchHooksCallback: null,
        ));
}

typedef $$RoutinesDriftTableTableProcessedTableManager = ProcessedTableManager<
    _$LifeOsDriftDatabase,
    $RoutinesDriftTableTable,
    RoutinesDriftTableData,
    $$RoutinesDriftTableTableFilterComposer,
    $$RoutinesDriftTableTableOrderingComposer,
    $$RoutinesDriftTableTableAnnotationComposer,
    $$RoutinesDriftTableTableCreateCompanionBuilder,
    $$RoutinesDriftTableTableUpdateCompanionBuilder,
    (
      RoutinesDriftTableData,
      BaseReferences<_$LifeOsDriftDatabase, $RoutinesDriftTableTable,
          RoutinesDriftTableData>
    ),
    RoutinesDriftTableData,
    PrefetchHooks Function()>;
typedef $$RoutineStepsDriftTableTableCreateCompanionBuilder
    = RoutineStepsDriftTableCompanion Function({
  required String id,
  required String userId,
  Value<int> version,
  required String updatedAt,
  Value<String?> deletedAt,
  Value<String> syncState,
  required String payloadJson,
  Value<int> rowid,
});
typedef $$RoutineStepsDriftTableTableUpdateCompanionBuilder
    = RoutineStepsDriftTableCompanion Function({
  Value<String> id,
  Value<String> userId,
  Value<int> version,
  Value<String> updatedAt,
  Value<String?> deletedAt,
  Value<String> syncState,
  Value<String> payloadJson,
  Value<int> rowid,
});

class $$RoutineStepsDriftTableTableFilterComposer
    extends Composer<_$LifeOsDriftDatabase, $RoutineStepsDriftTableTable> {
  $$RoutineStepsDriftTableTableFilterComposer({
    required super.$db,
    required super.$table,
    super.joinBuilder,
    super.$addJoinBuilderToRootComposer,
    super.$removeJoinBuilderFromRootComposer,
  });
  ColumnFilters<String> get id => $composableBuilder(
      column: $table.id, builder: (column) => ColumnFilters(column));

  ColumnFilters<String> get userId => $composableBuilder(
      column: $table.userId, builder: (column) => ColumnFilters(column));

  ColumnFilters<int> get version => $composableBuilder(
      column: $table.version, builder: (column) => ColumnFilters(column));

  ColumnFilters<String> get updatedAt => $composableBuilder(
      column: $table.updatedAt, builder: (column) => ColumnFilters(column));

  ColumnFilters<String> get deletedAt => $composableBuilder(
      column: $table.deletedAt, builder: (column) => ColumnFilters(column));

  ColumnFilters<String> get syncState => $composableBuilder(
      column: $table.syncState, builder: (column) => ColumnFilters(column));

  ColumnFilters<String> get payloadJson => $composableBuilder(
      column: $table.payloadJson, builder: (column) => ColumnFilters(column));
}

class $$RoutineStepsDriftTableTableOrderingComposer
    extends Composer<_$LifeOsDriftDatabase, $RoutineStepsDriftTableTable> {
  $$RoutineStepsDriftTableTableOrderingComposer({
    required super.$db,
    required super.$table,
    super.joinBuilder,
    super.$addJoinBuilderToRootComposer,
    super.$removeJoinBuilderFromRootComposer,
  });
  ColumnOrderings<String> get id => $composableBuilder(
      column: $table.id, builder: (column) => ColumnOrderings(column));

  ColumnOrderings<String> get userId => $composableBuilder(
      column: $table.userId, builder: (column) => ColumnOrderings(column));

  ColumnOrderings<int> get version => $composableBuilder(
      column: $table.version, builder: (column) => ColumnOrderings(column));

  ColumnOrderings<String> get updatedAt => $composableBuilder(
      column: $table.updatedAt, builder: (column) => ColumnOrderings(column));

  ColumnOrderings<String> get deletedAt => $composableBuilder(
      column: $table.deletedAt, builder: (column) => ColumnOrderings(column));

  ColumnOrderings<String> get syncState => $composableBuilder(
      column: $table.syncState, builder: (column) => ColumnOrderings(column));

  ColumnOrderings<String> get payloadJson => $composableBuilder(
      column: $table.payloadJson, builder: (column) => ColumnOrderings(column));
}

class $$RoutineStepsDriftTableTableAnnotationComposer
    extends Composer<_$LifeOsDriftDatabase, $RoutineStepsDriftTableTable> {
  $$RoutineStepsDriftTableTableAnnotationComposer({
    required super.$db,
    required super.$table,
    super.joinBuilder,
    super.$addJoinBuilderToRootComposer,
    super.$removeJoinBuilderFromRootComposer,
  });
  GeneratedColumn<String> get id =>
      $composableBuilder(column: $table.id, builder: (column) => column);

  GeneratedColumn<String> get userId =>
      $composableBuilder(column: $table.userId, builder: (column) => column);

  GeneratedColumn<int> get version =>
      $composableBuilder(column: $table.version, builder: (column) => column);

  GeneratedColumn<String> get updatedAt =>
      $composableBuilder(column: $table.updatedAt, builder: (column) => column);

  GeneratedColumn<String> get deletedAt =>
      $composableBuilder(column: $table.deletedAt, builder: (column) => column);

  GeneratedColumn<String> get syncState =>
      $composableBuilder(column: $table.syncState, builder: (column) => column);

  GeneratedColumn<String> get payloadJson => $composableBuilder(
      column: $table.payloadJson, builder: (column) => column);
}

class $$RoutineStepsDriftTableTableTableManager extends RootTableManager<
    _$LifeOsDriftDatabase,
    $RoutineStepsDriftTableTable,
    RoutineStepsDriftTableData,
    $$RoutineStepsDriftTableTableFilterComposer,
    $$RoutineStepsDriftTableTableOrderingComposer,
    $$RoutineStepsDriftTableTableAnnotationComposer,
    $$RoutineStepsDriftTableTableCreateCompanionBuilder,
    $$RoutineStepsDriftTableTableUpdateCompanionBuilder,
    (
      RoutineStepsDriftTableData,
      BaseReferences<_$LifeOsDriftDatabase, $RoutineStepsDriftTableTable,
          RoutineStepsDriftTableData>
    ),
    RoutineStepsDriftTableData,
    PrefetchHooks Function()> {
  $$RoutineStepsDriftTableTableTableManager(
      _$LifeOsDriftDatabase db, $RoutineStepsDriftTableTable table)
      : super(TableManagerState(
          db: db,
          table: table,
          createFilteringComposer: () =>
              $$RoutineStepsDriftTableTableFilterComposer(
                  $db: db, $table: table),
          createOrderingComposer: () =>
              $$RoutineStepsDriftTableTableOrderingComposer(
                  $db: db, $table: table),
          createComputedFieldComposer: () =>
              $$RoutineStepsDriftTableTableAnnotationComposer(
                  $db: db, $table: table),
          updateCompanionCallback: ({
            Value<String> id = const Value.absent(),
            Value<String> userId = const Value.absent(),
            Value<int> version = const Value.absent(),
            Value<String> updatedAt = const Value.absent(),
            Value<String?> deletedAt = const Value.absent(),
            Value<String> syncState = const Value.absent(),
            Value<String> payloadJson = const Value.absent(),
            Value<int> rowid = const Value.absent(),
          }) =>
              RoutineStepsDriftTableCompanion(
            id: id,
            userId: userId,
            version: version,
            updatedAt: updatedAt,
            deletedAt: deletedAt,
            syncState: syncState,
            payloadJson: payloadJson,
            rowid: rowid,
          ),
          createCompanionCallback: ({
            required String id,
            required String userId,
            Value<int> version = const Value.absent(),
            required String updatedAt,
            Value<String?> deletedAt = const Value.absent(),
            Value<String> syncState = const Value.absent(),
            required String payloadJson,
            Value<int> rowid = const Value.absent(),
          }) =>
              RoutineStepsDriftTableCompanion.insert(
            id: id,
            userId: userId,
            version: version,
            updatedAt: updatedAt,
            deletedAt: deletedAt,
            syncState: syncState,
            payloadJson: payloadJson,
            rowid: rowid,
          ),
          withReferenceMapper: (p0) => p0
              .map((e) => (e.readTable(table), BaseReferences(db, table, e)))
              .toList(),
          prefetchHooksCallback: null,
        ));
}

typedef $$RoutineStepsDriftTableTableProcessedTableManager
    = ProcessedTableManager<
        _$LifeOsDriftDatabase,
        $RoutineStepsDriftTableTable,
        RoutineStepsDriftTableData,
        $$RoutineStepsDriftTableTableFilterComposer,
        $$RoutineStepsDriftTableTableOrderingComposer,
        $$RoutineStepsDriftTableTableAnnotationComposer,
        $$RoutineStepsDriftTableTableCreateCompanionBuilder,
        $$RoutineStepsDriftTableTableUpdateCompanionBuilder,
        (
          RoutineStepsDriftTableData,
          BaseReferences<_$LifeOsDriftDatabase, $RoutineStepsDriftTableTable,
              RoutineStepsDriftTableData>
        ),
        RoutineStepsDriftTableData,
        PrefetchHooks Function()>;
typedef $$RoutineLogsDriftTableTableCreateCompanionBuilder
    = RoutineLogsDriftTableCompanion Function({
  required String id,
  required String userId,
  Value<int> version,
  required String updatedAt,
  Value<String?> deletedAt,
  Value<String> syncState,
  required String payloadJson,
  Value<int> rowid,
});
typedef $$RoutineLogsDriftTableTableUpdateCompanionBuilder
    = RoutineLogsDriftTableCompanion Function({
  Value<String> id,
  Value<String> userId,
  Value<int> version,
  Value<String> updatedAt,
  Value<String?> deletedAt,
  Value<String> syncState,
  Value<String> payloadJson,
  Value<int> rowid,
});

class $$RoutineLogsDriftTableTableFilterComposer
    extends Composer<_$LifeOsDriftDatabase, $RoutineLogsDriftTableTable> {
  $$RoutineLogsDriftTableTableFilterComposer({
    required super.$db,
    required super.$table,
    super.joinBuilder,
    super.$addJoinBuilderToRootComposer,
    super.$removeJoinBuilderFromRootComposer,
  });
  ColumnFilters<String> get id => $composableBuilder(
      column: $table.id, builder: (column) => ColumnFilters(column));

  ColumnFilters<String> get userId => $composableBuilder(
      column: $table.userId, builder: (column) => ColumnFilters(column));

  ColumnFilters<int> get version => $composableBuilder(
      column: $table.version, builder: (column) => ColumnFilters(column));

  ColumnFilters<String> get updatedAt => $composableBuilder(
      column: $table.updatedAt, builder: (column) => ColumnFilters(column));

  ColumnFilters<String> get deletedAt => $composableBuilder(
      column: $table.deletedAt, builder: (column) => ColumnFilters(column));

  ColumnFilters<String> get syncState => $composableBuilder(
      column: $table.syncState, builder: (column) => ColumnFilters(column));

  ColumnFilters<String> get payloadJson => $composableBuilder(
      column: $table.payloadJson, builder: (column) => ColumnFilters(column));
}

class $$RoutineLogsDriftTableTableOrderingComposer
    extends Composer<_$LifeOsDriftDatabase, $RoutineLogsDriftTableTable> {
  $$RoutineLogsDriftTableTableOrderingComposer({
    required super.$db,
    required super.$table,
    super.joinBuilder,
    super.$addJoinBuilderToRootComposer,
    super.$removeJoinBuilderFromRootComposer,
  });
  ColumnOrderings<String> get id => $composableBuilder(
      column: $table.id, builder: (column) => ColumnOrderings(column));

  ColumnOrderings<String> get userId => $composableBuilder(
      column: $table.userId, builder: (column) => ColumnOrderings(column));

  ColumnOrderings<int> get version => $composableBuilder(
      column: $table.version, builder: (column) => ColumnOrderings(column));

  ColumnOrderings<String> get updatedAt => $composableBuilder(
      column: $table.updatedAt, builder: (column) => ColumnOrderings(column));

  ColumnOrderings<String> get deletedAt => $composableBuilder(
      column: $table.deletedAt, builder: (column) => ColumnOrderings(column));

  ColumnOrderings<String> get syncState => $composableBuilder(
      column: $table.syncState, builder: (column) => ColumnOrderings(column));

  ColumnOrderings<String> get payloadJson => $composableBuilder(
      column: $table.payloadJson, builder: (column) => ColumnOrderings(column));
}

class $$RoutineLogsDriftTableTableAnnotationComposer
    extends Composer<_$LifeOsDriftDatabase, $RoutineLogsDriftTableTable> {
  $$RoutineLogsDriftTableTableAnnotationComposer({
    required super.$db,
    required super.$table,
    super.joinBuilder,
    super.$addJoinBuilderToRootComposer,
    super.$removeJoinBuilderFromRootComposer,
  });
  GeneratedColumn<String> get id =>
      $composableBuilder(column: $table.id, builder: (column) => column);

  GeneratedColumn<String> get userId =>
      $composableBuilder(column: $table.userId, builder: (column) => column);

  GeneratedColumn<int> get version =>
      $composableBuilder(column: $table.version, builder: (column) => column);

  GeneratedColumn<String> get updatedAt =>
      $composableBuilder(column: $table.updatedAt, builder: (column) => column);

  GeneratedColumn<String> get deletedAt =>
      $composableBuilder(column: $table.deletedAt, builder: (column) => column);

  GeneratedColumn<String> get syncState =>
      $composableBuilder(column: $table.syncState, builder: (column) => column);

  GeneratedColumn<String> get payloadJson => $composableBuilder(
      column: $table.payloadJson, builder: (column) => column);
}

class $$RoutineLogsDriftTableTableTableManager extends RootTableManager<
    _$LifeOsDriftDatabase,
    $RoutineLogsDriftTableTable,
    RoutineLogsDriftTableData,
    $$RoutineLogsDriftTableTableFilterComposer,
    $$RoutineLogsDriftTableTableOrderingComposer,
    $$RoutineLogsDriftTableTableAnnotationComposer,
    $$RoutineLogsDriftTableTableCreateCompanionBuilder,
    $$RoutineLogsDriftTableTableUpdateCompanionBuilder,
    (
      RoutineLogsDriftTableData,
      BaseReferences<_$LifeOsDriftDatabase, $RoutineLogsDriftTableTable,
          RoutineLogsDriftTableData>
    ),
    RoutineLogsDriftTableData,
    PrefetchHooks Function()> {
  $$RoutineLogsDriftTableTableTableManager(
      _$LifeOsDriftDatabase db, $RoutineLogsDriftTableTable table)
      : super(TableManagerState(
          db: db,
          table: table,
          createFilteringComposer: () =>
              $$RoutineLogsDriftTableTableFilterComposer(
                  $db: db, $table: table),
          createOrderingComposer: () =>
              $$RoutineLogsDriftTableTableOrderingComposer(
                  $db: db, $table: table),
          createComputedFieldComposer: () =>
              $$RoutineLogsDriftTableTableAnnotationComposer(
                  $db: db, $table: table),
          updateCompanionCallback: ({
            Value<String> id = const Value.absent(),
            Value<String> userId = const Value.absent(),
            Value<int> version = const Value.absent(),
            Value<String> updatedAt = const Value.absent(),
            Value<String?> deletedAt = const Value.absent(),
            Value<String> syncState = const Value.absent(),
            Value<String> payloadJson = const Value.absent(),
            Value<int> rowid = const Value.absent(),
          }) =>
              RoutineLogsDriftTableCompanion(
            id: id,
            userId: userId,
            version: version,
            updatedAt: updatedAt,
            deletedAt: deletedAt,
            syncState: syncState,
            payloadJson: payloadJson,
            rowid: rowid,
          ),
          createCompanionCallback: ({
            required String id,
            required String userId,
            Value<int> version = const Value.absent(),
            required String updatedAt,
            Value<String?> deletedAt = const Value.absent(),
            Value<String> syncState = const Value.absent(),
            required String payloadJson,
            Value<int> rowid = const Value.absent(),
          }) =>
              RoutineLogsDriftTableCompanion.insert(
            id: id,
            userId: userId,
            version: version,
            updatedAt: updatedAt,
            deletedAt: deletedAt,
            syncState: syncState,
            payloadJson: payloadJson,
            rowid: rowid,
          ),
          withReferenceMapper: (p0) => p0
              .map((e) => (e.readTable(table), BaseReferences(db, table, e)))
              .toList(),
          prefetchHooksCallback: null,
        ));
}

typedef $$RoutineLogsDriftTableTableProcessedTableManager
    = ProcessedTableManager<
        _$LifeOsDriftDatabase,
        $RoutineLogsDriftTableTable,
        RoutineLogsDriftTableData,
        $$RoutineLogsDriftTableTableFilterComposer,
        $$RoutineLogsDriftTableTableOrderingComposer,
        $$RoutineLogsDriftTableTableAnnotationComposer,
        $$RoutineLogsDriftTableTableCreateCompanionBuilder,
        $$RoutineLogsDriftTableTableUpdateCompanionBuilder,
        (
          RoutineLogsDriftTableData,
          BaseReferences<_$LifeOsDriftDatabase, $RoutineLogsDriftTableTable,
              RoutineLogsDriftTableData>
        ),
        RoutineLogsDriftTableData,
        PrefetchHooks Function()>;
typedef $$RoutineStepLogsDriftTableTableCreateCompanionBuilder
    = RoutineStepLogsDriftTableCompanion Function({
  required String id,
  required String userId,
  Value<int> version,
  required String updatedAt,
  Value<String?> deletedAt,
  Value<String> syncState,
  required String payloadJson,
  Value<int> rowid,
});
typedef $$RoutineStepLogsDriftTableTableUpdateCompanionBuilder
    = RoutineStepLogsDriftTableCompanion Function({
  Value<String> id,
  Value<String> userId,
  Value<int> version,
  Value<String> updatedAt,
  Value<String?> deletedAt,
  Value<String> syncState,
  Value<String> payloadJson,
  Value<int> rowid,
});

class $$RoutineStepLogsDriftTableTableFilterComposer
    extends Composer<_$LifeOsDriftDatabase, $RoutineStepLogsDriftTableTable> {
  $$RoutineStepLogsDriftTableTableFilterComposer({
    required super.$db,
    required super.$table,
    super.joinBuilder,
    super.$addJoinBuilderToRootComposer,
    super.$removeJoinBuilderFromRootComposer,
  });
  ColumnFilters<String> get id => $composableBuilder(
      column: $table.id, builder: (column) => ColumnFilters(column));

  ColumnFilters<String> get userId => $composableBuilder(
      column: $table.userId, builder: (column) => ColumnFilters(column));

  ColumnFilters<int> get version => $composableBuilder(
      column: $table.version, builder: (column) => ColumnFilters(column));

  ColumnFilters<String> get updatedAt => $composableBuilder(
      column: $table.updatedAt, builder: (column) => ColumnFilters(column));

  ColumnFilters<String> get deletedAt => $composableBuilder(
      column: $table.deletedAt, builder: (column) => ColumnFilters(column));

  ColumnFilters<String> get syncState => $composableBuilder(
      column: $table.syncState, builder: (column) => ColumnFilters(column));

  ColumnFilters<String> get payloadJson => $composableBuilder(
      column: $table.payloadJson, builder: (column) => ColumnFilters(column));
}

class $$RoutineStepLogsDriftTableTableOrderingComposer
    extends Composer<_$LifeOsDriftDatabase, $RoutineStepLogsDriftTableTable> {
  $$RoutineStepLogsDriftTableTableOrderingComposer({
    required super.$db,
    required super.$table,
    super.joinBuilder,
    super.$addJoinBuilderToRootComposer,
    super.$removeJoinBuilderFromRootComposer,
  });
  ColumnOrderings<String> get id => $composableBuilder(
      column: $table.id, builder: (column) => ColumnOrderings(column));

  ColumnOrderings<String> get userId => $composableBuilder(
      column: $table.userId, builder: (column) => ColumnOrderings(column));

  ColumnOrderings<int> get version => $composableBuilder(
      column: $table.version, builder: (column) => ColumnOrderings(column));

  ColumnOrderings<String> get updatedAt => $composableBuilder(
      column: $table.updatedAt, builder: (column) => ColumnOrderings(column));

  ColumnOrderings<String> get deletedAt => $composableBuilder(
      column: $table.deletedAt, builder: (column) => ColumnOrderings(column));

  ColumnOrderings<String> get syncState => $composableBuilder(
      column: $table.syncState, builder: (column) => ColumnOrderings(column));

  ColumnOrderings<String> get payloadJson => $composableBuilder(
      column: $table.payloadJson, builder: (column) => ColumnOrderings(column));
}

class $$RoutineStepLogsDriftTableTableAnnotationComposer
    extends Composer<_$LifeOsDriftDatabase, $RoutineStepLogsDriftTableTable> {
  $$RoutineStepLogsDriftTableTableAnnotationComposer({
    required super.$db,
    required super.$table,
    super.joinBuilder,
    super.$addJoinBuilderToRootComposer,
    super.$removeJoinBuilderFromRootComposer,
  });
  GeneratedColumn<String> get id =>
      $composableBuilder(column: $table.id, builder: (column) => column);

  GeneratedColumn<String> get userId =>
      $composableBuilder(column: $table.userId, builder: (column) => column);

  GeneratedColumn<int> get version =>
      $composableBuilder(column: $table.version, builder: (column) => column);

  GeneratedColumn<String> get updatedAt =>
      $composableBuilder(column: $table.updatedAt, builder: (column) => column);

  GeneratedColumn<String> get deletedAt =>
      $composableBuilder(column: $table.deletedAt, builder: (column) => column);

  GeneratedColumn<String> get syncState =>
      $composableBuilder(column: $table.syncState, builder: (column) => column);

  GeneratedColumn<String> get payloadJson => $composableBuilder(
      column: $table.payloadJson, builder: (column) => column);
}

class $$RoutineStepLogsDriftTableTableTableManager extends RootTableManager<
    _$LifeOsDriftDatabase,
    $RoutineStepLogsDriftTableTable,
    RoutineStepLogsDriftTableData,
    $$RoutineStepLogsDriftTableTableFilterComposer,
    $$RoutineStepLogsDriftTableTableOrderingComposer,
    $$RoutineStepLogsDriftTableTableAnnotationComposer,
    $$RoutineStepLogsDriftTableTableCreateCompanionBuilder,
    $$RoutineStepLogsDriftTableTableUpdateCompanionBuilder,
    (
      RoutineStepLogsDriftTableData,
      BaseReferences<_$LifeOsDriftDatabase, $RoutineStepLogsDriftTableTable,
          RoutineStepLogsDriftTableData>
    ),
    RoutineStepLogsDriftTableData,
    PrefetchHooks Function()> {
  $$RoutineStepLogsDriftTableTableTableManager(
      _$LifeOsDriftDatabase db, $RoutineStepLogsDriftTableTable table)
      : super(TableManagerState(
          db: db,
          table: table,
          createFilteringComposer: () =>
              $$RoutineStepLogsDriftTableTableFilterComposer(
                  $db: db, $table: table),
          createOrderingComposer: () =>
              $$RoutineStepLogsDriftTableTableOrderingComposer(
                  $db: db, $table: table),
          createComputedFieldComposer: () =>
              $$RoutineStepLogsDriftTableTableAnnotationComposer(
                  $db: db, $table: table),
          updateCompanionCallback: ({
            Value<String> id = const Value.absent(),
            Value<String> userId = const Value.absent(),
            Value<int> version = const Value.absent(),
            Value<String> updatedAt = const Value.absent(),
            Value<String?> deletedAt = const Value.absent(),
            Value<String> syncState = const Value.absent(),
            Value<String> payloadJson = const Value.absent(),
            Value<int> rowid = const Value.absent(),
          }) =>
              RoutineStepLogsDriftTableCompanion(
            id: id,
            userId: userId,
            version: version,
            updatedAt: updatedAt,
            deletedAt: deletedAt,
            syncState: syncState,
            payloadJson: payloadJson,
            rowid: rowid,
          ),
          createCompanionCallback: ({
            required String id,
            required String userId,
            Value<int> version = const Value.absent(),
            required String updatedAt,
            Value<String?> deletedAt = const Value.absent(),
            Value<String> syncState = const Value.absent(),
            required String payloadJson,
            Value<int> rowid = const Value.absent(),
          }) =>
              RoutineStepLogsDriftTableCompanion.insert(
            id: id,
            userId: userId,
            version: version,
            updatedAt: updatedAt,
            deletedAt: deletedAt,
            syncState: syncState,
            payloadJson: payloadJson,
            rowid: rowid,
          ),
          withReferenceMapper: (p0) => p0
              .map((e) => (e.readTable(table), BaseReferences(db, table, e)))
              .toList(),
          prefetchHooksCallback: null,
        ));
}

typedef $$RoutineStepLogsDriftTableTableProcessedTableManager
    = ProcessedTableManager<
        _$LifeOsDriftDatabase,
        $RoutineStepLogsDriftTableTable,
        RoutineStepLogsDriftTableData,
        $$RoutineStepLogsDriftTableTableFilterComposer,
        $$RoutineStepLogsDriftTableTableOrderingComposer,
        $$RoutineStepLogsDriftTableTableAnnotationComposer,
        $$RoutineStepLogsDriftTableTableCreateCompanionBuilder,
        $$RoutineStepLogsDriftTableTableUpdateCompanionBuilder,
        (
          RoutineStepLogsDriftTableData,
          BaseReferences<_$LifeOsDriftDatabase, $RoutineStepLogsDriftTableTable,
              RoutineStepLogsDriftTableData>
        ),
        RoutineStepLogsDriftTableData,
        PrefetchHooks Function()>;
typedef $$FinancialAccountsDriftTableTableCreateCompanionBuilder
    = FinancialAccountsDriftTableCompanion Function({
  required String id,
  required String userId,
  Value<int> version,
  required String updatedAt,
  Value<String?> deletedAt,
  Value<String> syncState,
  required String payloadJson,
  Value<int> rowid,
});
typedef $$FinancialAccountsDriftTableTableUpdateCompanionBuilder
    = FinancialAccountsDriftTableCompanion Function({
  Value<String> id,
  Value<String> userId,
  Value<int> version,
  Value<String> updatedAt,
  Value<String?> deletedAt,
  Value<String> syncState,
  Value<String> payloadJson,
  Value<int> rowid,
});

class $$FinancialAccountsDriftTableTableFilterComposer
    extends Composer<_$LifeOsDriftDatabase, $FinancialAccountsDriftTableTable> {
  $$FinancialAccountsDriftTableTableFilterComposer({
    required super.$db,
    required super.$table,
    super.joinBuilder,
    super.$addJoinBuilderToRootComposer,
    super.$removeJoinBuilderFromRootComposer,
  });
  ColumnFilters<String> get id => $composableBuilder(
      column: $table.id, builder: (column) => ColumnFilters(column));

  ColumnFilters<String> get userId => $composableBuilder(
      column: $table.userId, builder: (column) => ColumnFilters(column));

  ColumnFilters<int> get version => $composableBuilder(
      column: $table.version, builder: (column) => ColumnFilters(column));

  ColumnFilters<String> get updatedAt => $composableBuilder(
      column: $table.updatedAt, builder: (column) => ColumnFilters(column));

  ColumnFilters<String> get deletedAt => $composableBuilder(
      column: $table.deletedAt, builder: (column) => ColumnFilters(column));

  ColumnFilters<String> get syncState => $composableBuilder(
      column: $table.syncState, builder: (column) => ColumnFilters(column));

  ColumnFilters<String> get payloadJson => $composableBuilder(
      column: $table.payloadJson, builder: (column) => ColumnFilters(column));
}

class $$FinancialAccountsDriftTableTableOrderingComposer
    extends Composer<_$LifeOsDriftDatabase, $FinancialAccountsDriftTableTable> {
  $$FinancialAccountsDriftTableTableOrderingComposer({
    required super.$db,
    required super.$table,
    super.joinBuilder,
    super.$addJoinBuilderToRootComposer,
    super.$removeJoinBuilderFromRootComposer,
  });
  ColumnOrderings<String> get id => $composableBuilder(
      column: $table.id, builder: (column) => ColumnOrderings(column));

  ColumnOrderings<String> get userId => $composableBuilder(
      column: $table.userId, builder: (column) => ColumnOrderings(column));

  ColumnOrderings<int> get version => $composableBuilder(
      column: $table.version, builder: (column) => ColumnOrderings(column));

  ColumnOrderings<String> get updatedAt => $composableBuilder(
      column: $table.updatedAt, builder: (column) => ColumnOrderings(column));

  ColumnOrderings<String> get deletedAt => $composableBuilder(
      column: $table.deletedAt, builder: (column) => ColumnOrderings(column));

  ColumnOrderings<String> get syncState => $composableBuilder(
      column: $table.syncState, builder: (column) => ColumnOrderings(column));

  ColumnOrderings<String> get payloadJson => $composableBuilder(
      column: $table.payloadJson, builder: (column) => ColumnOrderings(column));
}

class $$FinancialAccountsDriftTableTableAnnotationComposer
    extends Composer<_$LifeOsDriftDatabase, $FinancialAccountsDriftTableTable> {
  $$FinancialAccountsDriftTableTableAnnotationComposer({
    required super.$db,
    required super.$table,
    super.joinBuilder,
    super.$addJoinBuilderToRootComposer,
    super.$removeJoinBuilderFromRootComposer,
  });
  GeneratedColumn<String> get id =>
      $composableBuilder(column: $table.id, builder: (column) => column);

  GeneratedColumn<String> get userId =>
      $composableBuilder(column: $table.userId, builder: (column) => column);

  GeneratedColumn<int> get version =>
      $composableBuilder(column: $table.version, builder: (column) => column);

  GeneratedColumn<String> get updatedAt =>
      $composableBuilder(column: $table.updatedAt, builder: (column) => column);

  GeneratedColumn<String> get deletedAt =>
      $composableBuilder(column: $table.deletedAt, builder: (column) => column);

  GeneratedColumn<String> get syncState =>
      $composableBuilder(column: $table.syncState, builder: (column) => column);

  GeneratedColumn<String> get payloadJson => $composableBuilder(
      column: $table.payloadJson, builder: (column) => column);
}

class $$FinancialAccountsDriftTableTableTableManager extends RootTableManager<
    _$LifeOsDriftDatabase,
    $FinancialAccountsDriftTableTable,
    FinancialAccountsDriftTableData,
    $$FinancialAccountsDriftTableTableFilterComposer,
    $$FinancialAccountsDriftTableTableOrderingComposer,
    $$FinancialAccountsDriftTableTableAnnotationComposer,
    $$FinancialAccountsDriftTableTableCreateCompanionBuilder,
    $$FinancialAccountsDriftTableTableUpdateCompanionBuilder,
    (
      FinancialAccountsDriftTableData,
      BaseReferences<_$LifeOsDriftDatabase, $FinancialAccountsDriftTableTable,
          FinancialAccountsDriftTableData>
    ),
    FinancialAccountsDriftTableData,
    PrefetchHooks Function()> {
  $$FinancialAccountsDriftTableTableTableManager(
      _$LifeOsDriftDatabase db, $FinancialAccountsDriftTableTable table)
      : super(TableManagerState(
          db: db,
          table: table,
          createFilteringComposer: () =>
              $$FinancialAccountsDriftTableTableFilterComposer(
                  $db: db, $table: table),
          createOrderingComposer: () =>
              $$FinancialAccountsDriftTableTableOrderingComposer(
                  $db: db, $table: table),
          createComputedFieldComposer: () =>
              $$FinancialAccountsDriftTableTableAnnotationComposer(
                  $db: db, $table: table),
          updateCompanionCallback: ({
            Value<String> id = const Value.absent(),
            Value<String> userId = const Value.absent(),
            Value<int> version = const Value.absent(),
            Value<String> updatedAt = const Value.absent(),
            Value<String?> deletedAt = const Value.absent(),
            Value<String> syncState = const Value.absent(),
            Value<String> payloadJson = const Value.absent(),
            Value<int> rowid = const Value.absent(),
          }) =>
              FinancialAccountsDriftTableCompanion(
            id: id,
            userId: userId,
            version: version,
            updatedAt: updatedAt,
            deletedAt: deletedAt,
            syncState: syncState,
            payloadJson: payloadJson,
            rowid: rowid,
          ),
          createCompanionCallback: ({
            required String id,
            required String userId,
            Value<int> version = const Value.absent(),
            required String updatedAt,
            Value<String?> deletedAt = const Value.absent(),
            Value<String> syncState = const Value.absent(),
            required String payloadJson,
            Value<int> rowid = const Value.absent(),
          }) =>
              FinancialAccountsDriftTableCompanion.insert(
            id: id,
            userId: userId,
            version: version,
            updatedAt: updatedAt,
            deletedAt: deletedAt,
            syncState: syncState,
            payloadJson: payloadJson,
            rowid: rowid,
          ),
          withReferenceMapper: (p0) => p0
              .map((e) => (e.readTable(table), BaseReferences(db, table, e)))
              .toList(),
          prefetchHooksCallback: null,
        ));
}

typedef $$FinancialAccountsDriftTableTableProcessedTableManager
    = ProcessedTableManager<
        _$LifeOsDriftDatabase,
        $FinancialAccountsDriftTableTable,
        FinancialAccountsDriftTableData,
        $$FinancialAccountsDriftTableTableFilterComposer,
        $$FinancialAccountsDriftTableTableOrderingComposer,
        $$FinancialAccountsDriftTableTableAnnotationComposer,
        $$FinancialAccountsDriftTableTableCreateCompanionBuilder,
        $$FinancialAccountsDriftTableTableUpdateCompanionBuilder,
        (
          FinancialAccountsDriftTableData,
          BaseReferences<
              _$LifeOsDriftDatabase,
              $FinancialAccountsDriftTableTable,
              FinancialAccountsDriftTableData>
        ),
        FinancialAccountsDriftTableData,
        PrefetchHooks Function()>;
typedef $$FinancialCategoriesDriftTableTableCreateCompanionBuilder
    = FinancialCategoriesDriftTableCompanion Function({
  required String id,
  required String userId,
  Value<int> version,
  required String updatedAt,
  Value<String?> deletedAt,
  Value<String> syncState,
  required String payloadJson,
  Value<int> rowid,
});
typedef $$FinancialCategoriesDriftTableTableUpdateCompanionBuilder
    = FinancialCategoriesDriftTableCompanion Function({
  Value<String> id,
  Value<String> userId,
  Value<int> version,
  Value<String> updatedAt,
  Value<String?> deletedAt,
  Value<String> syncState,
  Value<String> payloadJson,
  Value<int> rowid,
});

class $$FinancialCategoriesDriftTableTableFilterComposer extends Composer<
    _$LifeOsDriftDatabase, $FinancialCategoriesDriftTableTable> {
  $$FinancialCategoriesDriftTableTableFilterComposer({
    required super.$db,
    required super.$table,
    super.joinBuilder,
    super.$addJoinBuilderToRootComposer,
    super.$removeJoinBuilderFromRootComposer,
  });
  ColumnFilters<String> get id => $composableBuilder(
      column: $table.id, builder: (column) => ColumnFilters(column));

  ColumnFilters<String> get userId => $composableBuilder(
      column: $table.userId, builder: (column) => ColumnFilters(column));

  ColumnFilters<int> get version => $composableBuilder(
      column: $table.version, builder: (column) => ColumnFilters(column));

  ColumnFilters<String> get updatedAt => $composableBuilder(
      column: $table.updatedAt, builder: (column) => ColumnFilters(column));

  ColumnFilters<String> get deletedAt => $composableBuilder(
      column: $table.deletedAt, builder: (column) => ColumnFilters(column));

  ColumnFilters<String> get syncState => $composableBuilder(
      column: $table.syncState, builder: (column) => ColumnFilters(column));

  ColumnFilters<String> get payloadJson => $composableBuilder(
      column: $table.payloadJson, builder: (column) => ColumnFilters(column));
}

class $$FinancialCategoriesDriftTableTableOrderingComposer extends Composer<
    _$LifeOsDriftDatabase, $FinancialCategoriesDriftTableTable> {
  $$FinancialCategoriesDriftTableTableOrderingComposer({
    required super.$db,
    required super.$table,
    super.joinBuilder,
    super.$addJoinBuilderToRootComposer,
    super.$removeJoinBuilderFromRootComposer,
  });
  ColumnOrderings<String> get id => $composableBuilder(
      column: $table.id, builder: (column) => ColumnOrderings(column));

  ColumnOrderings<String> get userId => $composableBuilder(
      column: $table.userId, builder: (column) => ColumnOrderings(column));

  ColumnOrderings<int> get version => $composableBuilder(
      column: $table.version, builder: (column) => ColumnOrderings(column));

  ColumnOrderings<String> get updatedAt => $composableBuilder(
      column: $table.updatedAt, builder: (column) => ColumnOrderings(column));

  ColumnOrderings<String> get deletedAt => $composableBuilder(
      column: $table.deletedAt, builder: (column) => ColumnOrderings(column));

  ColumnOrderings<String> get syncState => $composableBuilder(
      column: $table.syncState, builder: (column) => ColumnOrderings(column));

  ColumnOrderings<String> get payloadJson => $composableBuilder(
      column: $table.payloadJson, builder: (column) => ColumnOrderings(column));
}

class $$FinancialCategoriesDriftTableTableAnnotationComposer extends Composer<
    _$LifeOsDriftDatabase, $FinancialCategoriesDriftTableTable> {
  $$FinancialCategoriesDriftTableTableAnnotationComposer({
    required super.$db,
    required super.$table,
    super.joinBuilder,
    super.$addJoinBuilderToRootComposer,
    super.$removeJoinBuilderFromRootComposer,
  });
  GeneratedColumn<String> get id =>
      $composableBuilder(column: $table.id, builder: (column) => column);

  GeneratedColumn<String> get userId =>
      $composableBuilder(column: $table.userId, builder: (column) => column);

  GeneratedColumn<int> get version =>
      $composableBuilder(column: $table.version, builder: (column) => column);

  GeneratedColumn<String> get updatedAt =>
      $composableBuilder(column: $table.updatedAt, builder: (column) => column);

  GeneratedColumn<String> get deletedAt =>
      $composableBuilder(column: $table.deletedAt, builder: (column) => column);

  GeneratedColumn<String> get syncState =>
      $composableBuilder(column: $table.syncState, builder: (column) => column);

  GeneratedColumn<String> get payloadJson => $composableBuilder(
      column: $table.payloadJson, builder: (column) => column);
}

class $$FinancialCategoriesDriftTableTableTableManager extends RootTableManager<
    _$LifeOsDriftDatabase,
    $FinancialCategoriesDriftTableTable,
    FinancialCategoriesDriftTableData,
    $$FinancialCategoriesDriftTableTableFilterComposer,
    $$FinancialCategoriesDriftTableTableOrderingComposer,
    $$FinancialCategoriesDriftTableTableAnnotationComposer,
    $$FinancialCategoriesDriftTableTableCreateCompanionBuilder,
    $$FinancialCategoriesDriftTableTableUpdateCompanionBuilder,
    (
      FinancialCategoriesDriftTableData,
      BaseReferences<_$LifeOsDriftDatabase, $FinancialCategoriesDriftTableTable,
          FinancialCategoriesDriftTableData>
    ),
    FinancialCategoriesDriftTableData,
    PrefetchHooks Function()> {
  $$FinancialCategoriesDriftTableTableTableManager(
      _$LifeOsDriftDatabase db, $FinancialCategoriesDriftTableTable table)
      : super(TableManagerState(
          db: db,
          table: table,
          createFilteringComposer: () =>
              $$FinancialCategoriesDriftTableTableFilterComposer(
                  $db: db, $table: table),
          createOrderingComposer: () =>
              $$FinancialCategoriesDriftTableTableOrderingComposer(
                  $db: db, $table: table),
          createComputedFieldComposer: () =>
              $$FinancialCategoriesDriftTableTableAnnotationComposer(
                  $db: db, $table: table),
          updateCompanionCallback: ({
            Value<String> id = const Value.absent(),
            Value<String> userId = const Value.absent(),
            Value<int> version = const Value.absent(),
            Value<String> updatedAt = const Value.absent(),
            Value<String?> deletedAt = const Value.absent(),
            Value<String> syncState = const Value.absent(),
            Value<String> payloadJson = const Value.absent(),
            Value<int> rowid = const Value.absent(),
          }) =>
              FinancialCategoriesDriftTableCompanion(
            id: id,
            userId: userId,
            version: version,
            updatedAt: updatedAt,
            deletedAt: deletedAt,
            syncState: syncState,
            payloadJson: payloadJson,
            rowid: rowid,
          ),
          createCompanionCallback: ({
            required String id,
            required String userId,
            Value<int> version = const Value.absent(),
            required String updatedAt,
            Value<String?> deletedAt = const Value.absent(),
            Value<String> syncState = const Value.absent(),
            required String payloadJson,
            Value<int> rowid = const Value.absent(),
          }) =>
              FinancialCategoriesDriftTableCompanion.insert(
            id: id,
            userId: userId,
            version: version,
            updatedAt: updatedAt,
            deletedAt: deletedAt,
            syncState: syncState,
            payloadJson: payloadJson,
            rowid: rowid,
          ),
          withReferenceMapper: (p0) => p0
              .map((e) => (e.readTable(table), BaseReferences(db, table, e)))
              .toList(),
          prefetchHooksCallback: null,
        ));
}

typedef $$FinancialCategoriesDriftTableTableProcessedTableManager
    = ProcessedTableManager<
        _$LifeOsDriftDatabase,
        $FinancialCategoriesDriftTableTable,
        FinancialCategoriesDriftTableData,
        $$FinancialCategoriesDriftTableTableFilterComposer,
        $$FinancialCategoriesDriftTableTableOrderingComposer,
        $$FinancialCategoriesDriftTableTableAnnotationComposer,
        $$FinancialCategoriesDriftTableTableCreateCompanionBuilder,
        $$FinancialCategoriesDriftTableTableUpdateCompanionBuilder,
        (
          FinancialCategoriesDriftTableData,
          BaseReferences<
              _$LifeOsDriftDatabase,
              $FinancialCategoriesDriftTableTable,
              FinancialCategoriesDriftTableData>
        ),
        FinancialCategoriesDriftTableData,
        PrefetchHooks Function()>;
typedef $$FinancialTransactionsDriftTableTableCreateCompanionBuilder
    = FinancialTransactionsDriftTableCompanion Function({
  required String id,
  required String userId,
  Value<int> version,
  required String updatedAt,
  Value<String?> deletedAt,
  Value<String> syncState,
  required String payloadJson,
  Value<int> rowid,
});
typedef $$FinancialTransactionsDriftTableTableUpdateCompanionBuilder
    = FinancialTransactionsDriftTableCompanion Function({
  Value<String> id,
  Value<String> userId,
  Value<int> version,
  Value<String> updatedAt,
  Value<String?> deletedAt,
  Value<String> syncState,
  Value<String> payloadJson,
  Value<int> rowid,
});

class $$FinancialTransactionsDriftTableTableFilterComposer extends Composer<
    _$LifeOsDriftDatabase, $FinancialTransactionsDriftTableTable> {
  $$FinancialTransactionsDriftTableTableFilterComposer({
    required super.$db,
    required super.$table,
    super.joinBuilder,
    super.$addJoinBuilderToRootComposer,
    super.$removeJoinBuilderFromRootComposer,
  });
  ColumnFilters<String> get id => $composableBuilder(
      column: $table.id, builder: (column) => ColumnFilters(column));

  ColumnFilters<String> get userId => $composableBuilder(
      column: $table.userId, builder: (column) => ColumnFilters(column));

  ColumnFilters<int> get version => $composableBuilder(
      column: $table.version, builder: (column) => ColumnFilters(column));

  ColumnFilters<String> get updatedAt => $composableBuilder(
      column: $table.updatedAt, builder: (column) => ColumnFilters(column));

  ColumnFilters<String> get deletedAt => $composableBuilder(
      column: $table.deletedAt, builder: (column) => ColumnFilters(column));

  ColumnFilters<String> get syncState => $composableBuilder(
      column: $table.syncState, builder: (column) => ColumnFilters(column));

  ColumnFilters<String> get payloadJson => $composableBuilder(
      column: $table.payloadJson, builder: (column) => ColumnFilters(column));
}

class $$FinancialTransactionsDriftTableTableOrderingComposer extends Composer<
    _$LifeOsDriftDatabase, $FinancialTransactionsDriftTableTable> {
  $$FinancialTransactionsDriftTableTableOrderingComposer({
    required super.$db,
    required super.$table,
    super.joinBuilder,
    super.$addJoinBuilderToRootComposer,
    super.$removeJoinBuilderFromRootComposer,
  });
  ColumnOrderings<String> get id => $composableBuilder(
      column: $table.id, builder: (column) => ColumnOrderings(column));

  ColumnOrderings<String> get userId => $composableBuilder(
      column: $table.userId, builder: (column) => ColumnOrderings(column));

  ColumnOrderings<int> get version => $composableBuilder(
      column: $table.version, builder: (column) => ColumnOrderings(column));

  ColumnOrderings<String> get updatedAt => $composableBuilder(
      column: $table.updatedAt, builder: (column) => ColumnOrderings(column));

  ColumnOrderings<String> get deletedAt => $composableBuilder(
      column: $table.deletedAt, builder: (column) => ColumnOrderings(column));

  ColumnOrderings<String> get syncState => $composableBuilder(
      column: $table.syncState, builder: (column) => ColumnOrderings(column));

  ColumnOrderings<String> get payloadJson => $composableBuilder(
      column: $table.payloadJson, builder: (column) => ColumnOrderings(column));
}

class $$FinancialTransactionsDriftTableTableAnnotationComposer extends Composer<
    _$LifeOsDriftDatabase, $FinancialTransactionsDriftTableTable> {
  $$FinancialTransactionsDriftTableTableAnnotationComposer({
    required super.$db,
    required super.$table,
    super.joinBuilder,
    super.$addJoinBuilderToRootComposer,
    super.$removeJoinBuilderFromRootComposer,
  });
  GeneratedColumn<String> get id =>
      $composableBuilder(column: $table.id, builder: (column) => column);

  GeneratedColumn<String> get userId =>
      $composableBuilder(column: $table.userId, builder: (column) => column);

  GeneratedColumn<int> get version =>
      $composableBuilder(column: $table.version, builder: (column) => column);

  GeneratedColumn<String> get updatedAt =>
      $composableBuilder(column: $table.updatedAt, builder: (column) => column);

  GeneratedColumn<String> get deletedAt =>
      $composableBuilder(column: $table.deletedAt, builder: (column) => column);

  GeneratedColumn<String> get syncState =>
      $composableBuilder(column: $table.syncState, builder: (column) => column);

  GeneratedColumn<String> get payloadJson => $composableBuilder(
      column: $table.payloadJson, builder: (column) => column);
}

class $$FinancialTransactionsDriftTableTableTableManager
    extends RootTableManager<
        _$LifeOsDriftDatabase,
        $FinancialTransactionsDriftTableTable,
        FinancialTransactionsDriftTableData,
        $$FinancialTransactionsDriftTableTableFilterComposer,
        $$FinancialTransactionsDriftTableTableOrderingComposer,
        $$FinancialTransactionsDriftTableTableAnnotationComposer,
        $$FinancialTransactionsDriftTableTableCreateCompanionBuilder,
        $$FinancialTransactionsDriftTableTableUpdateCompanionBuilder,
        (
          FinancialTransactionsDriftTableData,
          BaseReferences<
              _$LifeOsDriftDatabase,
              $FinancialTransactionsDriftTableTable,
              FinancialTransactionsDriftTableData>
        ),
        FinancialTransactionsDriftTableData,
        PrefetchHooks Function()> {
  $$FinancialTransactionsDriftTableTableTableManager(
      _$LifeOsDriftDatabase db, $FinancialTransactionsDriftTableTable table)
      : super(TableManagerState(
          db: db,
          table: table,
          createFilteringComposer: () =>
              $$FinancialTransactionsDriftTableTableFilterComposer(
                  $db: db, $table: table),
          createOrderingComposer: () =>
              $$FinancialTransactionsDriftTableTableOrderingComposer(
                  $db: db, $table: table),
          createComputedFieldComposer: () =>
              $$FinancialTransactionsDriftTableTableAnnotationComposer(
                  $db: db, $table: table),
          updateCompanionCallback: ({
            Value<String> id = const Value.absent(),
            Value<String> userId = const Value.absent(),
            Value<int> version = const Value.absent(),
            Value<String> updatedAt = const Value.absent(),
            Value<String?> deletedAt = const Value.absent(),
            Value<String> syncState = const Value.absent(),
            Value<String> payloadJson = const Value.absent(),
            Value<int> rowid = const Value.absent(),
          }) =>
              FinancialTransactionsDriftTableCompanion(
            id: id,
            userId: userId,
            version: version,
            updatedAt: updatedAt,
            deletedAt: deletedAt,
            syncState: syncState,
            payloadJson: payloadJson,
            rowid: rowid,
          ),
          createCompanionCallback: ({
            required String id,
            required String userId,
            Value<int> version = const Value.absent(),
            required String updatedAt,
            Value<String?> deletedAt = const Value.absent(),
            Value<String> syncState = const Value.absent(),
            required String payloadJson,
            Value<int> rowid = const Value.absent(),
          }) =>
              FinancialTransactionsDriftTableCompanion.insert(
            id: id,
            userId: userId,
            version: version,
            updatedAt: updatedAt,
            deletedAt: deletedAt,
            syncState: syncState,
            payloadJson: payloadJson,
            rowid: rowid,
          ),
          withReferenceMapper: (p0) => p0
              .map((e) => (e.readTable(table), BaseReferences(db, table, e)))
              .toList(),
          prefetchHooksCallback: null,
        ));
}

typedef $$FinancialTransactionsDriftTableTableProcessedTableManager
    = ProcessedTableManager<
        _$LifeOsDriftDatabase,
        $FinancialTransactionsDriftTableTable,
        FinancialTransactionsDriftTableData,
        $$FinancialTransactionsDriftTableTableFilterComposer,
        $$FinancialTransactionsDriftTableTableOrderingComposer,
        $$FinancialTransactionsDriftTableTableAnnotationComposer,
        $$FinancialTransactionsDriftTableTableCreateCompanionBuilder,
        $$FinancialTransactionsDriftTableTableUpdateCompanionBuilder,
        (
          FinancialTransactionsDriftTableData,
          BaseReferences<
              _$LifeOsDriftDatabase,
              $FinancialTransactionsDriftTableTable,
              FinancialTransactionsDriftTableData>
        ),
        FinancialTransactionsDriftTableData,
        PrefetchHooks Function()>;
typedef $$AccountTransfersDriftTableTableCreateCompanionBuilder
    = AccountTransfersDriftTableCompanion Function({
  required String id,
  required String userId,
  Value<int> version,
  required String updatedAt,
  Value<String?> deletedAt,
  Value<String> syncState,
  required String payloadJson,
  Value<int> rowid,
});
typedef $$AccountTransfersDriftTableTableUpdateCompanionBuilder
    = AccountTransfersDriftTableCompanion Function({
  Value<String> id,
  Value<String> userId,
  Value<int> version,
  Value<String> updatedAt,
  Value<String?> deletedAt,
  Value<String> syncState,
  Value<String> payloadJson,
  Value<int> rowid,
});

class $$AccountTransfersDriftTableTableFilterComposer
    extends Composer<_$LifeOsDriftDatabase, $AccountTransfersDriftTableTable> {
  $$AccountTransfersDriftTableTableFilterComposer({
    required super.$db,
    required super.$table,
    super.joinBuilder,
    super.$addJoinBuilderToRootComposer,
    super.$removeJoinBuilderFromRootComposer,
  });
  ColumnFilters<String> get id => $composableBuilder(
      column: $table.id, builder: (column) => ColumnFilters(column));

  ColumnFilters<String> get userId => $composableBuilder(
      column: $table.userId, builder: (column) => ColumnFilters(column));

  ColumnFilters<int> get version => $composableBuilder(
      column: $table.version, builder: (column) => ColumnFilters(column));

  ColumnFilters<String> get updatedAt => $composableBuilder(
      column: $table.updatedAt, builder: (column) => ColumnFilters(column));

  ColumnFilters<String> get deletedAt => $composableBuilder(
      column: $table.deletedAt, builder: (column) => ColumnFilters(column));

  ColumnFilters<String> get syncState => $composableBuilder(
      column: $table.syncState, builder: (column) => ColumnFilters(column));

  ColumnFilters<String> get payloadJson => $composableBuilder(
      column: $table.payloadJson, builder: (column) => ColumnFilters(column));
}

class $$AccountTransfersDriftTableTableOrderingComposer
    extends Composer<_$LifeOsDriftDatabase, $AccountTransfersDriftTableTable> {
  $$AccountTransfersDriftTableTableOrderingComposer({
    required super.$db,
    required super.$table,
    super.joinBuilder,
    super.$addJoinBuilderToRootComposer,
    super.$removeJoinBuilderFromRootComposer,
  });
  ColumnOrderings<String> get id => $composableBuilder(
      column: $table.id, builder: (column) => ColumnOrderings(column));

  ColumnOrderings<String> get userId => $composableBuilder(
      column: $table.userId, builder: (column) => ColumnOrderings(column));

  ColumnOrderings<int> get version => $composableBuilder(
      column: $table.version, builder: (column) => ColumnOrderings(column));

  ColumnOrderings<String> get updatedAt => $composableBuilder(
      column: $table.updatedAt, builder: (column) => ColumnOrderings(column));

  ColumnOrderings<String> get deletedAt => $composableBuilder(
      column: $table.deletedAt, builder: (column) => ColumnOrderings(column));

  ColumnOrderings<String> get syncState => $composableBuilder(
      column: $table.syncState, builder: (column) => ColumnOrderings(column));

  ColumnOrderings<String> get payloadJson => $composableBuilder(
      column: $table.payloadJson, builder: (column) => ColumnOrderings(column));
}

class $$AccountTransfersDriftTableTableAnnotationComposer
    extends Composer<_$LifeOsDriftDatabase, $AccountTransfersDriftTableTable> {
  $$AccountTransfersDriftTableTableAnnotationComposer({
    required super.$db,
    required super.$table,
    super.joinBuilder,
    super.$addJoinBuilderToRootComposer,
    super.$removeJoinBuilderFromRootComposer,
  });
  GeneratedColumn<String> get id =>
      $composableBuilder(column: $table.id, builder: (column) => column);

  GeneratedColumn<String> get userId =>
      $composableBuilder(column: $table.userId, builder: (column) => column);

  GeneratedColumn<int> get version =>
      $composableBuilder(column: $table.version, builder: (column) => column);

  GeneratedColumn<String> get updatedAt =>
      $composableBuilder(column: $table.updatedAt, builder: (column) => column);

  GeneratedColumn<String> get deletedAt =>
      $composableBuilder(column: $table.deletedAt, builder: (column) => column);

  GeneratedColumn<String> get syncState =>
      $composableBuilder(column: $table.syncState, builder: (column) => column);

  GeneratedColumn<String> get payloadJson => $composableBuilder(
      column: $table.payloadJson, builder: (column) => column);
}

class $$AccountTransfersDriftTableTableTableManager extends RootTableManager<
    _$LifeOsDriftDatabase,
    $AccountTransfersDriftTableTable,
    AccountTransfersDriftTableData,
    $$AccountTransfersDriftTableTableFilterComposer,
    $$AccountTransfersDriftTableTableOrderingComposer,
    $$AccountTransfersDriftTableTableAnnotationComposer,
    $$AccountTransfersDriftTableTableCreateCompanionBuilder,
    $$AccountTransfersDriftTableTableUpdateCompanionBuilder,
    (
      AccountTransfersDriftTableData,
      BaseReferences<_$LifeOsDriftDatabase, $AccountTransfersDriftTableTable,
          AccountTransfersDriftTableData>
    ),
    AccountTransfersDriftTableData,
    PrefetchHooks Function()> {
  $$AccountTransfersDriftTableTableTableManager(
      _$LifeOsDriftDatabase db, $AccountTransfersDriftTableTable table)
      : super(TableManagerState(
          db: db,
          table: table,
          createFilteringComposer: () =>
              $$AccountTransfersDriftTableTableFilterComposer(
                  $db: db, $table: table),
          createOrderingComposer: () =>
              $$AccountTransfersDriftTableTableOrderingComposer(
                  $db: db, $table: table),
          createComputedFieldComposer: () =>
              $$AccountTransfersDriftTableTableAnnotationComposer(
                  $db: db, $table: table),
          updateCompanionCallback: ({
            Value<String> id = const Value.absent(),
            Value<String> userId = const Value.absent(),
            Value<int> version = const Value.absent(),
            Value<String> updatedAt = const Value.absent(),
            Value<String?> deletedAt = const Value.absent(),
            Value<String> syncState = const Value.absent(),
            Value<String> payloadJson = const Value.absent(),
            Value<int> rowid = const Value.absent(),
          }) =>
              AccountTransfersDriftTableCompanion(
            id: id,
            userId: userId,
            version: version,
            updatedAt: updatedAt,
            deletedAt: deletedAt,
            syncState: syncState,
            payloadJson: payloadJson,
            rowid: rowid,
          ),
          createCompanionCallback: ({
            required String id,
            required String userId,
            Value<int> version = const Value.absent(),
            required String updatedAt,
            Value<String?> deletedAt = const Value.absent(),
            Value<String> syncState = const Value.absent(),
            required String payloadJson,
            Value<int> rowid = const Value.absent(),
          }) =>
              AccountTransfersDriftTableCompanion.insert(
            id: id,
            userId: userId,
            version: version,
            updatedAt: updatedAt,
            deletedAt: deletedAt,
            syncState: syncState,
            payloadJson: payloadJson,
            rowid: rowid,
          ),
          withReferenceMapper: (p0) => p0
              .map((e) => (e.readTable(table), BaseReferences(db, table, e)))
              .toList(),
          prefetchHooksCallback: null,
        ));
}

typedef $$AccountTransfersDriftTableTableProcessedTableManager
    = ProcessedTableManager<
        _$LifeOsDriftDatabase,
        $AccountTransfersDriftTableTable,
        AccountTransfersDriftTableData,
        $$AccountTransfersDriftTableTableFilterComposer,
        $$AccountTransfersDriftTableTableOrderingComposer,
        $$AccountTransfersDriftTableTableAnnotationComposer,
        $$AccountTransfersDriftTableTableCreateCompanionBuilder,
        $$AccountTransfersDriftTableTableUpdateCompanionBuilder,
        (
          AccountTransfersDriftTableData,
          BaseReferences<_$LifeOsDriftDatabase,
              $AccountTransfersDriftTableTable, AccountTransfersDriftTableData>
        ),
        AccountTransfersDriftTableData,
        PrefetchHooks Function()>;
typedef $$BudgetsDriftTableTableCreateCompanionBuilder
    = BudgetsDriftTableCompanion Function({
  required String id,
  required String userId,
  Value<int> version,
  required String updatedAt,
  Value<String?> deletedAt,
  Value<String> syncState,
  required String payloadJson,
  Value<int> rowid,
});
typedef $$BudgetsDriftTableTableUpdateCompanionBuilder
    = BudgetsDriftTableCompanion Function({
  Value<String> id,
  Value<String> userId,
  Value<int> version,
  Value<String> updatedAt,
  Value<String?> deletedAt,
  Value<String> syncState,
  Value<String> payloadJson,
  Value<int> rowid,
});

class $$BudgetsDriftTableTableFilterComposer
    extends Composer<_$LifeOsDriftDatabase, $BudgetsDriftTableTable> {
  $$BudgetsDriftTableTableFilterComposer({
    required super.$db,
    required super.$table,
    super.joinBuilder,
    super.$addJoinBuilderToRootComposer,
    super.$removeJoinBuilderFromRootComposer,
  });
  ColumnFilters<String> get id => $composableBuilder(
      column: $table.id, builder: (column) => ColumnFilters(column));

  ColumnFilters<String> get userId => $composableBuilder(
      column: $table.userId, builder: (column) => ColumnFilters(column));

  ColumnFilters<int> get version => $composableBuilder(
      column: $table.version, builder: (column) => ColumnFilters(column));

  ColumnFilters<String> get updatedAt => $composableBuilder(
      column: $table.updatedAt, builder: (column) => ColumnFilters(column));

  ColumnFilters<String> get deletedAt => $composableBuilder(
      column: $table.deletedAt, builder: (column) => ColumnFilters(column));

  ColumnFilters<String> get syncState => $composableBuilder(
      column: $table.syncState, builder: (column) => ColumnFilters(column));

  ColumnFilters<String> get payloadJson => $composableBuilder(
      column: $table.payloadJson, builder: (column) => ColumnFilters(column));
}

class $$BudgetsDriftTableTableOrderingComposer
    extends Composer<_$LifeOsDriftDatabase, $BudgetsDriftTableTable> {
  $$BudgetsDriftTableTableOrderingComposer({
    required super.$db,
    required super.$table,
    super.joinBuilder,
    super.$addJoinBuilderToRootComposer,
    super.$removeJoinBuilderFromRootComposer,
  });
  ColumnOrderings<String> get id => $composableBuilder(
      column: $table.id, builder: (column) => ColumnOrderings(column));

  ColumnOrderings<String> get userId => $composableBuilder(
      column: $table.userId, builder: (column) => ColumnOrderings(column));

  ColumnOrderings<int> get version => $composableBuilder(
      column: $table.version, builder: (column) => ColumnOrderings(column));

  ColumnOrderings<String> get updatedAt => $composableBuilder(
      column: $table.updatedAt, builder: (column) => ColumnOrderings(column));

  ColumnOrderings<String> get deletedAt => $composableBuilder(
      column: $table.deletedAt, builder: (column) => ColumnOrderings(column));

  ColumnOrderings<String> get syncState => $composableBuilder(
      column: $table.syncState, builder: (column) => ColumnOrderings(column));

  ColumnOrderings<String> get payloadJson => $composableBuilder(
      column: $table.payloadJson, builder: (column) => ColumnOrderings(column));
}

class $$BudgetsDriftTableTableAnnotationComposer
    extends Composer<_$LifeOsDriftDatabase, $BudgetsDriftTableTable> {
  $$BudgetsDriftTableTableAnnotationComposer({
    required super.$db,
    required super.$table,
    super.joinBuilder,
    super.$addJoinBuilderToRootComposer,
    super.$removeJoinBuilderFromRootComposer,
  });
  GeneratedColumn<String> get id =>
      $composableBuilder(column: $table.id, builder: (column) => column);

  GeneratedColumn<String> get userId =>
      $composableBuilder(column: $table.userId, builder: (column) => column);

  GeneratedColumn<int> get version =>
      $composableBuilder(column: $table.version, builder: (column) => column);

  GeneratedColumn<String> get updatedAt =>
      $composableBuilder(column: $table.updatedAt, builder: (column) => column);

  GeneratedColumn<String> get deletedAt =>
      $composableBuilder(column: $table.deletedAt, builder: (column) => column);

  GeneratedColumn<String> get syncState =>
      $composableBuilder(column: $table.syncState, builder: (column) => column);

  GeneratedColumn<String> get payloadJson => $composableBuilder(
      column: $table.payloadJson, builder: (column) => column);
}

class $$BudgetsDriftTableTableTableManager extends RootTableManager<
    _$LifeOsDriftDatabase,
    $BudgetsDriftTableTable,
    BudgetsDriftTableData,
    $$BudgetsDriftTableTableFilterComposer,
    $$BudgetsDriftTableTableOrderingComposer,
    $$BudgetsDriftTableTableAnnotationComposer,
    $$BudgetsDriftTableTableCreateCompanionBuilder,
    $$BudgetsDriftTableTableUpdateCompanionBuilder,
    (
      BudgetsDriftTableData,
      BaseReferences<_$LifeOsDriftDatabase, $BudgetsDriftTableTable,
          BudgetsDriftTableData>
    ),
    BudgetsDriftTableData,
    PrefetchHooks Function()> {
  $$BudgetsDriftTableTableTableManager(
      _$LifeOsDriftDatabase db, $BudgetsDriftTableTable table)
      : super(TableManagerState(
          db: db,
          table: table,
          createFilteringComposer: () =>
              $$BudgetsDriftTableTableFilterComposer($db: db, $table: table),
          createOrderingComposer: () =>
              $$BudgetsDriftTableTableOrderingComposer($db: db, $table: table),
          createComputedFieldComposer: () =>
              $$BudgetsDriftTableTableAnnotationComposer(
                  $db: db, $table: table),
          updateCompanionCallback: ({
            Value<String> id = const Value.absent(),
            Value<String> userId = const Value.absent(),
            Value<int> version = const Value.absent(),
            Value<String> updatedAt = const Value.absent(),
            Value<String?> deletedAt = const Value.absent(),
            Value<String> syncState = const Value.absent(),
            Value<String> payloadJson = const Value.absent(),
            Value<int> rowid = const Value.absent(),
          }) =>
              BudgetsDriftTableCompanion(
            id: id,
            userId: userId,
            version: version,
            updatedAt: updatedAt,
            deletedAt: deletedAt,
            syncState: syncState,
            payloadJson: payloadJson,
            rowid: rowid,
          ),
          createCompanionCallback: ({
            required String id,
            required String userId,
            Value<int> version = const Value.absent(),
            required String updatedAt,
            Value<String?> deletedAt = const Value.absent(),
            Value<String> syncState = const Value.absent(),
            required String payloadJson,
            Value<int> rowid = const Value.absent(),
          }) =>
              BudgetsDriftTableCompanion.insert(
            id: id,
            userId: userId,
            version: version,
            updatedAt: updatedAt,
            deletedAt: deletedAt,
            syncState: syncState,
            payloadJson: payloadJson,
            rowid: rowid,
          ),
          withReferenceMapper: (p0) => p0
              .map((e) => (e.readTable(table), BaseReferences(db, table, e)))
              .toList(),
          prefetchHooksCallback: null,
        ));
}

typedef $$BudgetsDriftTableTableProcessedTableManager = ProcessedTableManager<
    _$LifeOsDriftDatabase,
    $BudgetsDriftTableTable,
    BudgetsDriftTableData,
    $$BudgetsDriftTableTableFilterComposer,
    $$BudgetsDriftTableTableOrderingComposer,
    $$BudgetsDriftTableTableAnnotationComposer,
    $$BudgetsDriftTableTableCreateCompanionBuilder,
    $$BudgetsDriftTableTableUpdateCompanionBuilder,
    (
      BudgetsDriftTableData,
      BaseReferences<_$LifeOsDriftDatabase, $BudgetsDriftTableTable,
          BudgetsDriftTableData>
    ),
    BudgetsDriftTableData,
    PrefetchHooks Function()>;
typedef $$RecurringTransactionsDriftTableTableCreateCompanionBuilder
    = RecurringTransactionsDriftTableCompanion Function({
  required String id,
  required String userId,
  Value<int> version,
  required String updatedAt,
  Value<String?> deletedAt,
  Value<String> syncState,
  required String payloadJson,
  Value<int> rowid,
});
typedef $$RecurringTransactionsDriftTableTableUpdateCompanionBuilder
    = RecurringTransactionsDriftTableCompanion Function({
  Value<String> id,
  Value<String> userId,
  Value<int> version,
  Value<String> updatedAt,
  Value<String?> deletedAt,
  Value<String> syncState,
  Value<String> payloadJson,
  Value<int> rowid,
});

class $$RecurringTransactionsDriftTableTableFilterComposer extends Composer<
    _$LifeOsDriftDatabase, $RecurringTransactionsDriftTableTable> {
  $$RecurringTransactionsDriftTableTableFilterComposer({
    required super.$db,
    required super.$table,
    super.joinBuilder,
    super.$addJoinBuilderToRootComposer,
    super.$removeJoinBuilderFromRootComposer,
  });
  ColumnFilters<String> get id => $composableBuilder(
      column: $table.id, builder: (column) => ColumnFilters(column));

  ColumnFilters<String> get userId => $composableBuilder(
      column: $table.userId, builder: (column) => ColumnFilters(column));

  ColumnFilters<int> get version => $composableBuilder(
      column: $table.version, builder: (column) => ColumnFilters(column));

  ColumnFilters<String> get updatedAt => $composableBuilder(
      column: $table.updatedAt, builder: (column) => ColumnFilters(column));

  ColumnFilters<String> get deletedAt => $composableBuilder(
      column: $table.deletedAt, builder: (column) => ColumnFilters(column));

  ColumnFilters<String> get syncState => $composableBuilder(
      column: $table.syncState, builder: (column) => ColumnFilters(column));

  ColumnFilters<String> get payloadJson => $composableBuilder(
      column: $table.payloadJson, builder: (column) => ColumnFilters(column));
}

class $$RecurringTransactionsDriftTableTableOrderingComposer extends Composer<
    _$LifeOsDriftDatabase, $RecurringTransactionsDriftTableTable> {
  $$RecurringTransactionsDriftTableTableOrderingComposer({
    required super.$db,
    required super.$table,
    super.joinBuilder,
    super.$addJoinBuilderToRootComposer,
    super.$removeJoinBuilderFromRootComposer,
  });
  ColumnOrderings<String> get id => $composableBuilder(
      column: $table.id, builder: (column) => ColumnOrderings(column));

  ColumnOrderings<String> get userId => $composableBuilder(
      column: $table.userId, builder: (column) => ColumnOrderings(column));

  ColumnOrderings<int> get version => $composableBuilder(
      column: $table.version, builder: (column) => ColumnOrderings(column));

  ColumnOrderings<String> get updatedAt => $composableBuilder(
      column: $table.updatedAt, builder: (column) => ColumnOrderings(column));

  ColumnOrderings<String> get deletedAt => $composableBuilder(
      column: $table.deletedAt, builder: (column) => ColumnOrderings(column));

  ColumnOrderings<String> get syncState => $composableBuilder(
      column: $table.syncState, builder: (column) => ColumnOrderings(column));

  ColumnOrderings<String> get payloadJson => $composableBuilder(
      column: $table.payloadJson, builder: (column) => ColumnOrderings(column));
}

class $$RecurringTransactionsDriftTableTableAnnotationComposer extends Composer<
    _$LifeOsDriftDatabase, $RecurringTransactionsDriftTableTable> {
  $$RecurringTransactionsDriftTableTableAnnotationComposer({
    required super.$db,
    required super.$table,
    super.joinBuilder,
    super.$addJoinBuilderToRootComposer,
    super.$removeJoinBuilderFromRootComposer,
  });
  GeneratedColumn<String> get id =>
      $composableBuilder(column: $table.id, builder: (column) => column);

  GeneratedColumn<String> get userId =>
      $composableBuilder(column: $table.userId, builder: (column) => column);

  GeneratedColumn<int> get version =>
      $composableBuilder(column: $table.version, builder: (column) => column);

  GeneratedColumn<String> get updatedAt =>
      $composableBuilder(column: $table.updatedAt, builder: (column) => column);

  GeneratedColumn<String> get deletedAt =>
      $composableBuilder(column: $table.deletedAt, builder: (column) => column);

  GeneratedColumn<String> get syncState =>
      $composableBuilder(column: $table.syncState, builder: (column) => column);

  GeneratedColumn<String> get payloadJson => $composableBuilder(
      column: $table.payloadJson, builder: (column) => column);
}

class $$RecurringTransactionsDriftTableTableTableManager
    extends RootTableManager<
        _$LifeOsDriftDatabase,
        $RecurringTransactionsDriftTableTable,
        RecurringTransactionsDriftTableData,
        $$RecurringTransactionsDriftTableTableFilterComposer,
        $$RecurringTransactionsDriftTableTableOrderingComposer,
        $$RecurringTransactionsDriftTableTableAnnotationComposer,
        $$RecurringTransactionsDriftTableTableCreateCompanionBuilder,
        $$RecurringTransactionsDriftTableTableUpdateCompanionBuilder,
        (
          RecurringTransactionsDriftTableData,
          BaseReferences<
              _$LifeOsDriftDatabase,
              $RecurringTransactionsDriftTableTable,
              RecurringTransactionsDriftTableData>
        ),
        RecurringTransactionsDriftTableData,
        PrefetchHooks Function()> {
  $$RecurringTransactionsDriftTableTableTableManager(
      _$LifeOsDriftDatabase db, $RecurringTransactionsDriftTableTable table)
      : super(TableManagerState(
          db: db,
          table: table,
          createFilteringComposer: () =>
              $$RecurringTransactionsDriftTableTableFilterComposer(
                  $db: db, $table: table),
          createOrderingComposer: () =>
              $$RecurringTransactionsDriftTableTableOrderingComposer(
                  $db: db, $table: table),
          createComputedFieldComposer: () =>
              $$RecurringTransactionsDriftTableTableAnnotationComposer(
                  $db: db, $table: table),
          updateCompanionCallback: ({
            Value<String> id = const Value.absent(),
            Value<String> userId = const Value.absent(),
            Value<int> version = const Value.absent(),
            Value<String> updatedAt = const Value.absent(),
            Value<String?> deletedAt = const Value.absent(),
            Value<String> syncState = const Value.absent(),
            Value<String> payloadJson = const Value.absent(),
            Value<int> rowid = const Value.absent(),
          }) =>
              RecurringTransactionsDriftTableCompanion(
            id: id,
            userId: userId,
            version: version,
            updatedAt: updatedAt,
            deletedAt: deletedAt,
            syncState: syncState,
            payloadJson: payloadJson,
            rowid: rowid,
          ),
          createCompanionCallback: ({
            required String id,
            required String userId,
            Value<int> version = const Value.absent(),
            required String updatedAt,
            Value<String?> deletedAt = const Value.absent(),
            Value<String> syncState = const Value.absent(),
            required String payloadJson,
            Value<int> rowid = const Value.absent(),
          }) =>
              RecurringTransactionsDriftTableCompanion.insert(
            id: id,
            userId: userId,
            version: version,
            updatedAt: updatedAt,
            deletedAt: deletedAt,
            syncState: syncState,
            payloadJson: payloadJson,
            rowid: rowid,
          ),
          withReferenceMapper: (p0) => p0
              .map((e) => (e.readTable(table), BaseReferences(db, table, e)))
              .toList(),
          prefetchHooksCallback: null,
        ));
}

typedef $$RecurringTransactionsDriftTableTableProcessedTableManager
    = ProcessedTableManager<
        _$LifeOsDriftDatabase,
        $RecurringTransactionsDriftTableTable,
        RecurringTransactionsDriftTableData,
        $$RecurringTransactionsDriftTableTableFilterComposer,
        $$RecurringTransactionsDriftTableTableOrderingComposer,
        $$RecurringTransactionsDriftTableTableAnnotationComposer,
        $$RecurringTransactionsDriftTableTableCreateCompanionBuilder,
        $$RecurringTransactionsDriftTableTableUpdateCompanionBuilder,
        (
          RecurringTransactionsDriftTableData,
          BaseReferences<
              _$LifeOsDriftDatabase,
              $RecurringTransactionsDriftTableTable,
              RecurringTransactionsDriftTableData>
        ),
        RecurringTransactionsDriftTableData,
        PrefetchHooks Function()>;
typedef $$SavingGoalsDriftTableTableCreateCompanionBuilder
    = SavingGoalsDriftTableCompanion Function({
  required String id,
  required String userId,
  Value<int> version,
  required String updatedAt,
  Value<String?> deletedAt,
  Value<String> syncState,
  required String payloadJson,
  Value<int> rowid,
});
typedef $$SavingGoalsDriftTableTableUpdateCompanionBuilder
    = SavingGoalsDriftTableCompanion Function({
  Value<String> id,
  Value<String> userId,
  Value<int> version,
  Value<String> updatedAt,
  Value<String?> deletedAt,
  Value<String> syncState,
  Value<String> payloadJson,
  Value<int> rowid,
});

class $$SavingGoalsDriftTableTableFilterComposer
    extends Composer<_$LifeOsDriftDatabase, $SavingGoalsDriftTableTable> {
  $$SavingGoalsDriftTableTableFilterComposer({
    required super.$db,
    required super.$table,
    super.joinBuilder,
    super.$addJoinBuilderToRootComposer,
    super.$removeJoinBuilderFromRootComposer,
  });
  ColumnFilters<String> get id => $composableBuilder(
      column: $table.id, builder: (column) => ColumnFilters(column));

  ColumnFilters<String> get userId => $composableBuilder(
      column: $table.userId, builder: (column) => ColumnFilters(column));

  ColumnFilters<int> get version => $composableBuilder(
      column: $table.version, builder: (column) => ColumnFilters(column));

  ColumnFilters<String> get updatedAt => $composableBuilder(
      column: $table.updatedAt, builder: (column) => ColumnFilters(column));

  ColumnFilters<String> get deletedAt => $composableBuilder(
      column: $table.deletedAt, builder: (column) => ColumnFilters(column));

  ColumnFilters<String> get syncState => $composableBuilder(
      column: $table.syncState, builder: (column) => ColumnFilters(column));

  ColumnFilters<String> get payloadJson => $composableBuilder(
      column: $table.payloadJson, builder: (column) => ColumnFilters(column));
}

class $$SavingGoalsDriftTableTableOrderingComposer
    extends Composer<_$LifeOsDriftDatabase, $SavingGoalsDriftTableTable> {
  $$SavingGoalsDriftTableTableOrderingComposer({
    required super.$db,
    required super.$table,
    super.joinBuilder,
    super.$addJoinBuilderToRootComposer,
    super.$removeJoinBuilderFromRootComposer,
  });
  ColumnOrderings<String> get id => $composableBuilder(
      column: $table.id, builder: (column) => ColumnOrderings(column));

  ColumnOrderings<String> get userId => $composableBuilder(
      column: $table.userId, builder: (column) => ColumnOrderings(column));

  ColumnOrderings<int> get version => $composableBuilder(
      column: $table.version, builder: (column) => ColumnOrderings(column));

  ColumnOrderings<String> get updatedAt => $composableBuilder(
      column: $table.updatedAt, builder: (column) => ColumnOrderings(column));

  ColumnOrderings<String> get deletedAt => $composableBuilder(
      column: $table.deletedAt, builder: (column) => ColumnOrderings(column));

  ColumnOrderings<String> get syncState => $composableBuilder(
      column: $table.syncState, builder: (column) => ColumnOrderings(column));

  ColumnOrderings<String> get payloadJson => $composableBuilder(
      column: $table.payloadJson, builder: (column) => ColumnOrderings(column));
}

class $$SavingGoalsDriftTableTableAnnotationComposer
    extends Composer<_$LifeOsDriftDatabase, $SavingGoalsDriftTableTable> {
  $$SavingGoalsDriftTableTableAnnotationComposer({
    required super.$db,
    required super.$table,
    super.joinBuilder,
    super.$addJoinBuilderToRootComposer,
    super.$removeJoinBuilderFromRootComposer,
  });
  GeneratedColumn<String> get id =>
      $composableBuilder(column: $table.id, builder: (column) => column);

  GeneratedColumn<String> get userId =>
      $composableBuilder(column: $table.userId, builder: (column) => column);

  GeneratedColumn<int> get version =>
      $composableBuilder(column: $table.version, builder: (column) => column);

  GeneratedColumn<String> get updatedAt =>
      $composableBuilder(column: $table.updatedAt, builder: (column) => column);

  GeneratedColumn<String> get deletedAt =>
      $composableBuilder(column: $table.deletedAt, builder: (column) => column);

  GeneratedColumn<String> get syncState =>
      $composableBuilder(column: $table.syncState, builder: (column) => column);

  GeneratedColumn<String> get payloadJson => $composableBuilder(
      column: $table.payloadJson, builder: (column) => column);
}

class $$SavingGoalsDriftTableTableTableManager extends RootTableManager<
    _$LifeOsDriftDatabase,
    $SavingGoalsDriftTableTable,
    SavingGoalsDriftTableData,
    $$SavingGoalsDriftTableTableFilterComposer,
    $$SavingGoalsDriftTableTableOrderingComposer,
    $$SavingGoalsDriftTableTableAnnotationComposer,
    $$SavingGoalsDriftTableTableCreateCompanionBuilder,
    $$SavingGoalsDriftTableTableUpdateCompanionBuilder,
    (
      SavingGoalsDriftTableData,
      BaseReferences<_$LifeOsDriftDatabase, $SavingGoalsDriftTableTable,
          SavingGoalsDriftTableData>
    ),
    SavingGoalsDriftTableData,
    PrefetchHooks Function()> {
  $$SavingGoalsDriftTableTableTableManager(
      _$LifeOsDriftDatabase db, $SavingGoalsDriftTableTable table)
      : super(TableManagerState(
          db: db,
          table: table,
          createFilteringComposer: () =>
              $$SavingGoalsDriftTableTableFilterComposer(
                  $db: db, $table: table),
          createOrderingComposer: () =>
              $$SavingGoalsDriftTableTableOrderingComposer(
                  $db: db, $table: table),
          createComputedFieldComposer: () =>
              $$SavingGoalsDriftTableTableAnnotationComposer(
                  $db: db, $table: table),
          updateCompanionCallback: ({
            Value<String> id = const Value.absent(),
            Value<String> userId = const Value.absent(),
            Value<int> version = const Value.absent(),
            Value<String> updatedAt = const Value.absent(),
            Value<String?> deletedAt = const Value.absent(),
            Value<String> syncState = const Value.absent(),
            Value<String> payloadJson = const Value.absent(),
            Value<int> rowid = const Value.absent(),
          }) =>
              SavingGoalsDriftTableCompanion(
            id: id,
            userId: userId,
            version: version,
            updatedAt: updatedAt,
            deletedAt: deletedAt,
            syncState: syncState,
            payloadJson: payloadJson,
            rowid: rowid,
          ),
          createCompanionCallback: ({
            required String id,
            required String userId,
            Value<int> version = const Value.absent(),
            required String updatedAt,
            Value<String?> deletedAt = const Value.absent(),
            Value<String> syncState = const Value.absent(),
            required String payloadJson,
            Value<int> rowid = const Value.absent(),
          }) =>
              SavingGoalsDriftTableCompanion.insert(
            id: id,
            userId: userId,
            version: version,
            updatedAt: updatedAt,
            deletedAt: deletedAt,
            syncState: syncState,
            payloadJson: payloadJson,
            rowid: rowid,
          ),
          withReferenceMapper: (p0) => p0
              .map((e) => (e.readTable(table), BaseReferences(db, table, e)))
              .toList(),
          prefetchHooksCallback: null,
        ));
}

typedef $$SavingGoalsDriftTableTableProcessedTableManager
    = ProcessedTableManager<
        _$LifeOsDriftDatabase,
        $SavingGoalsDriftTableTable,
        SavingGoalsDriftTableData,
        $$SavingGoalsDriftTableTableFilterComposer,
        $$SavingGoalsDriftTableTableOrderingComposer,
        $$SavingGoalsDriftTableTableAnnotationComposer,
        $$SavingGoalsDriftTableTableCreateCompanionBuilder,
        $$SavingGoalsDriftTableTableUpdateCompanionBuilder,
        (
          SavingGoalsDriftTableData,
          BaseReferences<_$LifeOsDriftDatabase, $SavingGoalsDriftTableTable,
              SavingGoalsDriftTableData>
        ),
        SavingGoalsDriftTableData,
        PrefetchHooks Function()>;
typedef $$SavingContributionsDriftTableTableCreateCompanionBuilder
    = SavingContributionsDriftTableCompanion Function({
  required String id,
  required String userId,
  Value<int> version,
  required String updatedAt,
  Value<String?> deletedAt,
  Value<String> syncState,
  required String payloadJson,
  Value<int> rowid,
});
typedef $$SavingContributionsDriftTableTableUpdateCompanionBuilder
    = SavingContributionsDriftTableCompanion Function({
  Value<String> id,
  Value<String> userId,
  Value<int> version,
  Value<String> updatedAt,
  Value<String?> deletedAt,
  Value<String> syncState,
  Value<String> payloadJson,
  Value<int> rowid,
});

class $$SavingContributionsDriftTableTableFilterComposer extends Composer<
    _$LifeOsDriftDatabase, $SavingContributionsDriftTableTable> {
  $$SavingContributionsDriftTableTableFilterComposer({
    required super.$db,
    required super.$table,
    super.joinBuilder,
    super.$addJoinBuilderToRootComposer,
    super.$removeJoinBuilderFromRootComposer,
  });
  ColumnFilters<String> get id => $composableBuilder(
      column: $table.id, builder: (column) => ColumnFilters(column));

  ColumnFilters<String> get userId => $composableBuilder(
      column: $table.userId, builder: (column) => ColumnFilters(column));

  ColumnFilters<int> get version => $composableBuilder(
      column: $table.version, builder: (column) => ColumnFilters(column));

  ColumnFilters<String> get updatedAt => $composableBuilder(
      column: $table.updatedAt, builder: (column) => ColumnFilters(column));

  ColumnFilters<String> get deletedAt => $composableBuilder(
      column: $table.deletedAt, builder: (column) => ColumnFilters(column));

  ColumnFilters<String> get syncState => $composableBuilder(
      column: $table.syncState, builder: (column) => ColumnFilters(column));

  ColumnFilters<String> get payloadJson => $composableBuilder(
      column: $table.payloadJson, builder: (column) => ColumnFilters(column));
}

class $$SavingContributionsDriftTableTableOrderingComposer extends Composer<
    _$LifeOsDriftDatabase, $SavingContributionsDriftTableTable> {
  $$SavingContributionsDriftTableTableOrderingComposer({
    required super.$db,
    required super.$table,
    super.joinBuilder,
    super.$addJoinBuilderToRootComposer,
    super.$removeJoinBuilderFromRootComposer,
  });
  ColumnOrderings<String> get id => $composableBuilder(
      column: $table.id, builder: (column) => ColumnOrderings(column));

  ColumnOrderings<String> get userId => $composableBuilder(
      column: $table.userId, builder: (column) => ColumnOrderings(column));

  ColumnOrderings<int> get version => $composableBuilder(
      column: $table.version, builder: (column) => ColumnOrderings(column));

  ColumnOrderings<String> get updatedAt => $composableBuilder(
      column: $table.updatedAt, builder: (column) => ColumnOrderings(column));

  ColumnOrderings<String> get deletedAt => $composableBuilder(
      column: $table.deletedAt, builder: (column) => ColumnOrderings(column));

  ColumnOrderings<String> get syncState => $composableBuilder(
      column: $table.syncState, builder: (column) => ColumnOrderings(column));

  ColumnOrderings<String> get payloadJson => $composableBuilder(
      column: $table.payloadJson, builder: (column) => ColumnOrderings(column));
}

class $$SavingContributionsDriftTableTableAnnotationComposer extends Composer<
    _$LifeOsDriftDatabase, $SavingContributionsDriftTableTable> {
  $$SavingContributionsDriftTableTableAnnotationComposer({
    required super.$db,
    required super.$table,
    super.joinBuilder,
    super.$addJoinBuilderToRootComposer,
    super.$removeJoinBuilderFromRootComposer,
  });
  GeneratedColumn<String> get id =>
      $composableBuilder(column: $table.id, builder: (column) => column);

  GeneratedColumn<String> get userId =>
      $composableBuilder(column: $table.userId, builder: (column) => column);

  GeneratedColumn<int> get version =>
      $composableBuilder(column: $table.version, builder: (column) => column);

  GeneratedColumn<String> get updatedAt =>
      $composableBuilder(column: $table.updatedAt, builder: (column) => column);

  GeneratedColumn<String> get deletedAt =>
      $composableBuilder(column: $table.deletedAt, builder: (column) => column);

  GeneratedColumn<String> get syncState =>
      $composableBuilder(column: $table.syncState, builder: (column) => column);

  GeneratedColumn<String> get payloadJson => $composableBuilder(
      column: $table.payloadJson, builder: (column) => column);
}

class $$SavingContributionsDriftTableTableTableManager extends RootTableManager<
    _$LifeOsDriftDatabase,
    $SavingContributionsDriftTableTable,
    SavingContributionsDriftTableData,
    $$SavingContributionsDriftTableTableFilterComposer,
    $$SavingContributionsDriftTableTableOrderingComposer,
    $$SavingContributionsDriftTableTableAnnotationComposer,
    $$SavingContributionsDriftTableTableCreateCompanionBuilder,
    $$SavingContributionsDriftTableTableUpdateCompanionBuilder,
    (
      SavingContributionsDriftTableData,
      BaseReferences<_$LifeOsDriftDatabase, $SavingContributionsDriftTableTable,
          SavingContributionsDriftTableData>
    ),
    SavingContributionsDriftTableData,
    PrefetchHooks Function()> {
  $$SavingContributionsDriftTableTableTableManager(
      _$LifeOsDriftDatabase db, $SavingContributionsDriftTableTable table)
      : super(TableManagerState(
          db: db,
          table: table,
          createFilteringComposer: () =>
              $$SavingContributionsDriftTableTableFilterComposer(
                  $db: db, $table: table),
          createOrderingComposer: () =>
              $$SavingContributionsDriftTableTableOrderingComposer(
                  $db: db, $table: table),
          createComputedFieldComposer: () =>
              $$SavingContributionsDriftTableTableAnnotationComposer(
                  $db: db, $table: table),
          updateCompanionCallback: ({
            Value<String> id = const Value.absent(),
            Value<String> userId = const Value.absent(),
            Value<int> version = const Value.absent(),
            Value<String> updatedAt = const Value.absent(),
            Value<String?> deletedAt = const Value.absent(),
            Value<String> syncState = const Value.absent(),
            Value<String> payloadJson = const Value.absent(),
            Value<int> rowid = const Value.absent(),
          }) =>
              SavingContributionsDriftTableCompanion(
            id: id,
            userId: userId,
            version: version,
            updatedAt: updatedAt,
            deletedAt: deletedAt,
            syncState: syncState,
            payloadJson: payloadJson,
            rowid: rowid,
          ),
          createCompanionCallback: ({
            required String id,
            required String userId,
            Value<int> version = const Value.absent(),
            required String updatedAt,
            Value<String?> deletedAt = const Value.absent(),
            Value<String> syncState = const Value.absent(),
            required String payloadJson,
            Value<int> rowid = const Value.absent(),
          }) =>
              SavingContributionsDriftTableCompanion.insert(
            id: id,
            userId: userId,
            version: version,
            updatedAt: updatedAt,
            deletedAt: deletedAt,
            syncState: syncState,
            payloadJson: payloadJson,
            rowid: rowid,
          ),
          withReferenceMapper: (p0) => p0
              .map((e) => (e.readTable(table), BaseReferences(db, table, e)))
              .toList(),
          prefetchHooksCallback: null,
        ));
}

typedef $$SavingContributionsDriftTableTableProcessedTableManager
    = ProcessedTableManager<
        _$LifeOsDriftDatabase,
        $SavingContributionsDriftTableTable,
        SavingContributionsDriftTableData,
        $$SavingContributionsDriftTableTableFilterComposer,
        $$SavingContributionsDriftTableTableOrderingComposer,
        $$SavingContributionsDriftTableTableAnnotationComposer,
        $$SavingContributionsDriftTableTableCreateCompanionBuilder,
        $$SavingContributionsDriftTableTableUpdateCompanionBuilder,
        (
          SavingContributionsDriftTableData,
          BaseReferences<
              _$LifeOsDriftDatabase,
              $SavingContributionsDriftTableTable,
              SavingContributionsDriftTableData>
        ),
        SavingContributionsDriftTableData,
        PrefetchHooks Function()>;
typedef $$CalendarEventsDriftTableTableCreateCompanionBuilder
    = CalendarEventsDriftTableCompanion Function({
  required String id,
  required String userId,
  Value<int> version,
  required String updatedAt,
  Value<String?> deletedAt,
  Value<String> syncState,
  required String payloadJson,
  Value<int> rowid,
});
typedef $$CalendarEventsDriftTableTableUpdateCompanionBuilder
    = CalendarEventsDriftTableCompanion Function({
  Value<String> id,
  Value<String> userId,
  Value<int> version,
  Value<String> updatedAt,
  Value<String?> deletedAt,
  Value<String> syncState,
  Value<String> payloadJson,
  Value<int> rowid,
});

class $$CalendarEventsDriftTableTableFilterComposer
    extends Composer<_$LifeOsDriftDatabase, $CalendarEventsDriftTableTable> {
  $$CalendarEventsDriftTableTableFilterComposer({
    required super.$db,
    required super.$table,
    super.joinBuilder,
    super.$addJoinBuilderToRootComposer,
    super.$removeJoinBuilderFromRootComposer,
  });
  ColumnFilters<String> get id => $composableBuilder(
      column: $table.id, builder: (column) => ColumnFilters(column));

  ColumnFilters<String> get userId => $composableBuilder(
      column: $table.userId, builder: (column) => ColumnFilters(column));

  ColumnFilters<int> get version => $composableBuilder(
      column: $table.version, builder: (column) => ColumnFilters(column));

  ColumnFilters<String> get updatedAt => $composableBuilder(
      column: $table.updatedAt, builder: (column) => ColumnFilters(column));

  ColumnFilters<String> get deletedAt => $composableBuilder(
      column: $table.deletedAt, builder: (column) => ColumnFilters(column));

  ColumnFilters<String> get syncState => $composableBuilder(
      column: $table.syncState, builder: (column) => ColumnFilters(column));

  ColumnFilters<String> get payloadJson => $composableBuilder(
      column: $table.payloadJson, builder: (column) => ColumnFilters(column));
}

class $$CalendarEventsDriftTableTableOrderingComposer
    extends Composer<_$LifeOsDriftDatabase, $CalendarEventsDriftTableTable> {
  $$CalendarEventsDriftTableTableOrderingComposer({
    required super.$db,
    required super.$table,
    super.joinBuilder,
    super.$addJoinBuilderToRootComposer,
    super.$removeJoinBuilderFromRootComposer,
  });
  ColumnOrderings<String> get id => $composableBuilder(
      column: $table.id, builder: (column) => ColumnOrderings(column));

  ColumnOrderings<String> get userId => $composableBuilder(
      column: $table.userId, builder: (column) => ColumnOrderings(column));

  ColumnOrderings<int> get version => $composableBuilder(
      column: $table.version, builder: (column) => ColumnOrderings(column));

  ColumnOrderings<String> get updatedAt => $composableBuilder(
      column: $table.updatedAt, builder: (column) => ColumnOrderings(column));

  ColumnOrderings<String> get deletedAt => $composableBuilder(
      column: $table.deletedAt, builder: (column) => ColumnOrderings(column));

  ColumnOrderings<String> get syncState => $composableBuilder(
      column: $table.syncState, builder: (column) => ColumnOrderings(column));

  ColumnOrderings<String> get payloadJson => $composableBuilder(
      column: $table.payloadJson, builder: (column) => ColumnOrderings(column));
}

class $$CalendarEventsDriftTableTableAnnotationComposer
    extends Composer<_$LifeOsDriftDatabase, $CalendarEventsDriftTableTable> {
  $$CalendarEventsDriftTableTableAnnotationComposer({
    required super.$db,
    required super.$table,
    super.joinBuilder,
    super.$addJoinBuilderToRootComposer,
    super.$removeJoinBuilderFromRootComposer,
  });
  GeneratedColumn<String> get id =>
      $composableBuilder(column: $table.id, builder: (column) => column);

  GeneratedColumn<String> get userId =>
      $composableBuilder(column: $table.userId, builder: (column) => column);

  GeneratedColumn<int> get version =>
      $composableBuilder(column: $table.version, builder: (column) => column);

  GeneratedColumn<String> get updatedAt =>
      $composableBuilder(column: $table.updatedAt, builder: (column) => column);

  GeneratedColumn<String> get deletedAt =>
      $composableBuilder(column: $table.deletedAt, builder: (column) => column);

  GeneratedColumn<String> get syncState =>
      $composableBuilder(column: $table.syncState, builder: (column) => column);

  GeneratedColumn<String> get payloadJson => $composableBuilder(
      column: $table.payloadJson, builder: (column) => column);
}

class $$CalendarEventsDriftTableTableTableManager extends RootTableManager<
    _$LifeOsDriftDatabase,
    $CalendarEventsDriftTableTable,
    CalendarEventsDriftTableData,
    $$CalendarEventsDriftTableTableFilterComposer,
    $$CalendarEventsDriftTableTableOrderingComposer,
    $$CalendarEventsDriftTableTableAnnotationComposer,
    $$CalendarEventsDriftTableTableCreateCompanionBuilder,
    $$CalendarEventsDriftTableTableUpdateCompanionBuilder,
    (
      CalendarEventsDriftTableData,
      BaseReferences<_$LifeOsDriftDatabase, $CalendarEventsDriftTableTable,
          CalendarEventsDriftTableData>
    ),
    CalendarEventsDriftTableData,
    PrefetchHooks Function()> {
  $$CalendarEventsDriftTableTableTableManager(
      _$LifeOsDriftDatabase db, $CalendarEventsDriftTableTable table)
      : super(TableManagerState(
          db: db,
          table: table,
          createFilteringComposer: () =>
              $$CalendarEventsDriftTableTableFilterComposer(
                  $db: db, $table: table),
          createOrderingComposer: () =>
              $$CalendarEventsDriftTableTableOrderingComposer(
                  $db: db, $table: table),
          createComputedFieldComposer: () =>
              $$CalendarEventsDriftTableTableAnnotationComposer(
                  $db: db, $table: table),
          updateCompanionCallback: ({
            Value<String> id = const Value.absent(),
            Value<String> userId = const Value.absent(),
            Value<int> version = const Value.absent(),
            Value<String> updatedAt = const Value.absent(),
            Value<String?> deletedAt = const Value.absent(),
            Value<String> syncState = const Value.absent(),
            Value<String> payloadJson = const Value.absent(),
            Value<int> rowid = const Value.absent(),
          }) =>
              CalendarEventsDriftTableCompanion(
            id: id,
            userId: userId,
            version: version,
            updatedAt: updatedAt,
            deletedAt: deletedAt,
            syncState: syncState,
            payloadJson: payloadJson,
            rowid: rowid,
          ),
          createCompanionCallback: ({
            required String id,
            required String userId,
            Value<int> version = const Value.absent(),
            required String updatedAt,
            Value<String?> deletedAt = const Value.absent(),
            Value<String> syncState = const Value.absent(),
            required String payloadJson,
            Value<int> rowid = const Value.absent(),
          }) =>
              CalendarEventsDriftTableCompanion.insert(
            id: id,
            userId: userId,
            version: version,
            updatedAt: updatedAt,
            deletedAt: deletedAt,
            syncState: syncState,
            payloadJson: payloadJson,
            rowid: rowid,
          ),
          withReferenceMapper: (p0) => p0
              .map((e) => (e.readTable(table), BaseReferences(db, table, e)))
              .toList(),
          prefetchHooksCallback: null,
        ));
}

typedef $$CalendarEventsDriftTableTableProcessedTableManager
    = ProcessedTableManager<
        _$LifeOsDriftDatabase,
        $CalendarEventsDriftTableTable,
        CalendarEventsDriftTableData,
        $$CalendarEventsDriftTableTableFilterComposer,
        $$CalendarEventsDriftTableTableOrderingComposer,
        $$CalendarEventsDriftTableTableAnnotationComposer,
        $$CalendarEventsDriftTableTableCreateCompanionBuilder,
        $$CalendarEventsDriftTableTableUpdateCompanionBuilder,
        (
          CalendarEventsDriftTableData,
          BaseReferences<_$LifeOsDriftDatabase, $CalendarEventsDriftTableTable,
              CalendarEventsDriftTableData>
        ),
        CalendarEventsDriftTableData,
        PrefetchHooks Function()>;
typedef $$EventExceptionsDriftTableTableCreateCompanionBuilder
    = EventExceptionsDriftTableCompanion Function({
  required String id,
  required String userId,
  Value<int> version,
  required String updatedAt,
  Value<String?> deletedAt,
  Value<String> syncState,
  required String payloadJson,
  Value<int> rowid,
});
typedef $$EventExceptionsDriftTableTableUpdateCompanionBuilder
    = EventExceptionsDriftTableCompanion Function({
  Value<String> id,
  Value<String> userId,
  Value<int> version,
  Value<String> updatedAt,
  Value<String?> deletedAt,
  Value<String> syncState,
  Value<String> payloadJson,
  Value<int> rowid,
});

class $$EventExceptionsDriftTableTableFilterComposer
    extends Composer<_$LifeOsDriftDatabase, $EventExceptionsDriftTableTable> {
  $$EventExceptionsDriftTableTableFilterComposer({
    required super.$db,
    required super.$table,
    super.joinBuilder,
    super.$addJoinBuilderToRootComposer,
    super.$removeJoinBuilderFromRootComposer,
  });
  ColumnFilters<String> get id => $composableBuilder(
      column: $table.id, builder: (column) => ColumnFilters(column));

  ColumnFilters<String> get userId => $composableBuilder(
      column: $table.userId, builder: (column) => ColumnFilters(column));

  ColumnFilters<int> get version => $composableBuilder(
      column: $table.version, builder: (column) => ColumnFilters(column));

  ColumnFilters<String> get updatedAt => $composableBuilder(
      column: $table.updatedAt, builder: (column) => ColumnFilters(column));

  ColumnFilters<String> get deletedAt => $composableBuilder(
      column: $table.deletedAt, builder: (column) => ColumnFilters(column));

  ColumnFilters<String> get syncState => $composableBuilder(
      column: $table.syncState, builder: (column) => ColumnFilters(column));

  ColumnFilters<String> get payloadJson => $composableBuilder(
      column: $table.payloadJson, builder: (column) => ColumnFilters(column));
}

class $$EventExceptionsDriftTableTableOrderingComposer
    extends Composer<_$LifeOsDriftDatabase, $EventExceptionsDriftTableTable> {
  $$EventExceptionsDriftTableTableOrderingComposer({
    required super.$db,
    required super.$table,
    super.joinBuilder,
    super.$addJoinBuilderToRootComposer,
    super.$removeJoinBuilderFromRootComposer,
  });
  ColumnOrderings<String> get id => $composableBuilder(
      column: $table.id, builder: (column) => ColumnOrderings(column));

  ColumnOrderings<String> get userId => $composableBuilder(
      column: $table.userId, builder: (column) => ColumnOrderings(column));

  ColumnOrderings<int> get version => $composableBuilder(
      column: $table.version, builder: (column) => ColumnOrderings(column));

  ColumnOrderings<String> get updatedAt => $composableBuilder(
      column: $table.updatedAt, builder: (column) => ColumnOrderings(column));

  ColumnOrderings<String> get deletedAt => $composableBuilder(
      column: $table.deletedAt, builder: (column) => ColumnOrderings(column));

  ColumnOrderings<String> get syncState => $composableBuilder(
      column: $table.syncState, builder: (column) => ColumnOrderings(column));

  ColumnOrderings<String> get payloadJson => $composableBuilder(
      column: $table.payloadJson, builder: (column) => ColumnOrderings(column));
}

class $$EventExceptionsDriftTableTableAnnotationComposer
    extends Composer<_$LifeOsDriftDatabase, $EventExceptionsDriftTableTable> {
  $$EventExceptionsDriftTableTableAnnotationComposer({
    required super.$db,
    required super.$table,
    super.joinBuilder,
    super.$addJoinBuilderToRootComposer,
    super.$removeJoinBuilderFromRootComposer,
  });
  GeneratedColumn<String> get id =>
      $composableBuilder(column: $table.id, builder: (column) => column);

  GeneratedColumn<String> get userId =>
      $composableBuilder(column: $table.userId, builder: (column) => column);

  GeneratedColumn<int> get version =>
      $composableBuilder(column: $table.version, builder: (column) => column);

  GeneratedColumn<String> get updatedAt =>
      $composableBuilder(column: $table.updatedAt, builder: (column) => column);

  GeneratedColumn<String> get deletedAt =>
      $composableBuilder(column: $table.deletedAt, builder: (column) => column);

  GeneratedColumn<String> get syncState =>
      $composableBuilder(column: $table.syncState, builder: (column) => column);

  GeneratedColumn<String> get payloadJson => $composableBuilder(
      column: $table.payloadJson, builder: (column) => column);
}

class $$EventExceptionsDriftTableTableTableManager extends RootTableManager<
    _$LifeOsDriftDatabase,
    $EventExceptionsDriftTableTable,
    EventExceptionsDriftTableData,
    $$EventExceptionsDriftTableTableFilterComposer,
    $$EventExceptionsDriftTableTableOrderingComposer,
    $$EventExceptionsDriftTableTableAnnotationComposer,
    $$EventExceptionsDriftTableTableCreateCompanionBuilder,
    $$EventExceptionsDriftTableTableUpdateCompanionBuilder,
    (
      EventExceptionsDriftTableData,
      BaseReferences<_$LifeOsDriftDatabase, $EventExceptionsDriftTableTable,
          EventExceptionsDriftTableData>
    ),
    EventExceptionsDriftTableData,
    PrefetchHooks Function()> {
  $$EventExceptionsDriftTableTableTableManager(
      _$LifeOsDriftDatabase db, $EventExceptionsDriftTableTable table)
      : super(TableManagerState(
          db: db,
          table: table,
          createFilteringComposer: () =>
              $$EventExceptionsDriftTableTableFilterComposer(
                  $db: db, $table: table),
          createOrderingComposer: () =>
              $$EventExceptionsDriftTableTableOrderingComposer(
                  $db: db, $table: table),
          createComputedFieldComposer: () =>
              $$EventExceptionsDriftTableTableAnnotationComposer(
                  $db: db, $table: table),
          updateCompanionCallback: ({
            Value<String> id = const Value.absent(),
            Value<String> userId = const Value.absent(),
            Value<int> version = const Value.absent(),
            Value<String> updatedAt = const Value.absent(),
            Value<String?> deletedAt = const Value.absent(),
            Value<String> syncState = const Value.absent(),
            Value<String> payloadJson = const Value.absent(),
            Value<int> rowid = const Value.absent(),
          }) =>
              EventExceptionsDriftTableCompanion(
            id: id,
            userId: userId,
            version: version,
            updatedAt: updatedAt,
            deletedAt: deletedAt,
            syncState: syncState,
            payloadJson: payloadJson,
            rowid: rowid,
          ),
          createCompanionCallback: ({
            required String id,
            required String userId,
            Value<int> version = const Value.absent(),
            required String updatedAt,
            Value<String?> deletedAt = const Value.absent(),
            Value<String> syncState = const Value.absent(),
            required String payloadJson,
            Value<int> rowid = const Value.absent(),
          }) =>
              EventExceptionsDriftTableCompanion.insert(
            id: id,
            userId: userId,
            version: version,
            updatedAt: updatedAt,
            deletedAt: deletedAt,
            syncState: syncState,
            payloadJson: payloadJson,
            rowid: rowid,
          ),
          withReferenceMapper: (p0) => p0
              .map((e) => (e.readTable(table), BaseReferences(db, table, e)))
              .toList(),
          prefetchHooksCallback: null,
        ));
}

typedef $$EventExceptionsDriftTableTableProcessedTableManager
    = ProcessedTableManager<
        _$LifeOsDriftDatabase,
        $EventExceptionsDriftTableTable,
        EventExceptionsDriftTableData,
        $$EventExceptionsDriftTableTableFilterComposer,
        $$EventExceptionsDriftTableTableOrderingComposer,
        $$EventExceptionsDriftTableTableAnnotationComposer,
        $$EventExceptionsDriftTableTableCreateCompanionBuilder,
        $$EventExceptionsDriftTableTableUpdateCompanionBuilder,
        (
          EventExceptionsDriftTableData,
          BaseReferences<_$LifeOsDriftDatabase, $EventExceptionsDriftTableTable,
              EventExceptionsDriftTableData>
        ),
        EventExceptionsDriftTableData,
        PrefetchHooks Function()>;
typedef $$TimeBlocksDriftTableTableCreateCompanionBuilder
    = TimeBlocksDriftTableCompanion Function({
  required String id,
  required String userId,
  Value<int> version,
  required String updatedAt,
  Value<String?> deletedAt,
  Value<String> syncState,
  required String payloadJson,
  Value<int> rowid,
});
typedef $$TimeBlocksDriftTableTableUpdateCompanionBuilder
    = TimeBlocksDriftTableCompanion Function({
  Value<String> id,
  Value<String> userId,
  Value<int> version,
  Value<String> updatedAt,
  Value<String?> deletedAt,
  Value<String> syncState,
  Value<String> payloadJson,
  Value<int> rowid,
});

class $$TimeBlocksDriftTableTableFilterComposer
    extends Composer<_$LifeOsDriftDatabase, $TimeBlocksDriftTableTable> {
  $$TimeBlocksDriftTableTableFilterComposer({
    required super.$db,
    required super.$table,
    super.joinBuilder,
    super.$addJoinBuilderToRootComposer,
    super.$removeJoinBuilderFromRootComposer,
  });
  ColumnFilters<String> get id => $composableBuilder(
      column: $table.id, builder: (column) => ColumnFilters(column));

  ColumnFilters<String> get userId => $composableBuilder(
      column: $table.userId, builder: (column) => ColumnFilters(column));

  ColumnFilters<int> get version => $composableBuilder(
      column: $table.version, builder: (column) => ColumnFilters(column));

  ColumnFilters<String> get updatedAt => $composableBuilder(
      column: $table.updatedAt, builder: (column) => ColumnFilters(column));

  ColumnFilters<String> get deletedAt => $composableBuilder(
      column: $table.deletedAt, builder: (column) => ColumnFilters(column));

  ColumnFilters<String> get syncState => $composableBuilder(
      column: $table.syncState, builder: (column) => ColumnFilters(column));

  ColumnFilters<String> get payloadJson => $composableBuilder(
      column: $table.payloadJson, builder: (column) => ColumnFilters(column));
}

class $$TimeBlocksDriftTableTableOrderingComposer
    extends Composer<_$LifeOsDriftDatabase, $TimeBlocksDriftTableTable> {
  $$TimeBlocksDriftTableTableOrderingComposer({
    required super.$db,
    required super.$table,
    super.joinBuilder,
    super.$addJoinBuilderToRootComposer,
    super.$removeJoinBuilderFromRootComposer,
  });
  ColumnOrderings<String> get id => $composableBuilder(
      column: $table.id, builder: (column) => ColumnOrderings(column));

  ColumnOrderings<String> get userId => $composableBuilder(
      column: $table.userId, builder: (column) => ColumnOrderings(column));

  ColumnOrderings<int> get version => $composableBuilder(
      column: $table.version, builder: (column) => ColumnOrderings(column));

  ColumnOrderings<String> get updatedAt => $composableBuilder(
      column: $table.updatedAt, builder: (column) => ColumnOrderings(column));

  ColumnOrderings<String> get deletedAt => $composableBuilder(
      column: $table.deletedAt, builder: (column) => ColumnOrderings(column));

  ColumnOrderings<String> get syncState => $composableBuilder(
      column: $table.syncState, builder: (column) => ColumnOrderings(column));

  ColumnOrderings<String> get payloadJson => $composableBuilder(
      column: $table.payloadJson, builder: (column) => ColumnOrderings(column));
}

class $$TimeBlocksDriftTableTableAnnotationComposer
    extends Composer<_$LifeOsDriftDatabase, $TimeBlocksDriftTableTable> {
  $$TimeBlocksDriftTableTableAnnotationComposer({
    required super.$db,
    required super.$table,
    super.joinBuilder,
    super.$addJoinBuilderToRootComposer,
    super.$removeJoinBuilderFromRootComposer,
  });
  GeneratedColumn<String> get id =>
      $composableBuilder(column: $table.id, builder: (column) => column);

  GeneratedColumn<String> get userId =>
      $composableBuilder(column: $table.userId, builder: (column) => column);

  GeneratedColumn<int> get version =>
      $composableBuilder(column: $table.version, builder: (column) => column);

  GeneratedColumn<String> get updatedAt =>
      $composableBuilder(column: $table.updatedAt, builder: (column) => column);

  GeneratedColumn<String> get deletedAt =>
      $composableBuilder(column: $table.deletedAt, builder: (column) => column);

  GeneratedColumn<String> get syncState =>
      $composableBuilder(column: $table.syncState, builder: (column) => column);

  GeneratedColumn<String> get payloadJson => $composableBuilder(
      column: $table.payloadJson, builder: (column) => column);
}

class $$TimeBlocksDriftTableTableTableManager extends RootTableManager<
    _$LifeOsDriftDatabase,
    $TimeBlocksDriftTableTable,
    TimeBlocksDriftTableData,
    $$TimeBlocksDriftTableTableFilterComposer,
    $$TimeBlocksDriftTableTableOrderingComposer,
    $$TimeBlocksDriftTableTableAnnotationComposer,
    $$TimeBlocksDriftTableTableCreateCompanionBuilder,
    $$TimeBlocksDriftTableTableUpdateCompanionBuilder,
    (
      TimeBlocksDriftTableData,
      BaseReferences<_$LifeOsDriftDatabase, $TimeBlocksDriftTableTable,
          TimeBlocksDriftTableData>
    ),
    TimeBlocksDriftTableData,
    PrefetchHooks Function()> {
  $$TimeBlocksDriftTableTableTableManager(
      _$LifeOsDriftDatabase db, $TimeBlocksDriftTableTable table)
      : super(TableManagerState(
          db: db,
          table: table,
          createFilteringComposer: () =>
              $$TimeBlocksDriftTableTableFilterComposer($db: db, $table: table),
          createOrderingComposer: () =>
              $$TimeBlocksDriftTableTableOrderingComposer(
                  $db: db, $table: table),
          createComputedFieldComposer: () =>
              $$TimeBlocksDriftTableTableAnnotationComposer(
                  $db: db, $table: table),
          updateCompanionCallback: ({
            Value<String> id = const Value.absent(),
            Value<String> userId = const Value.absent(),
            Value<int> version = const Value.absent(),
            Value<String> updatedAt = const Value.absent(),
            Value<String?> deletedAt = const Value.absent(),
            Value<String> syncState = const Value.absent(),
            Value<String> payloadJson = const Value.absent(),
            Value<int> rowid = const Value.absent(),
          }) =>
              TimeBlocksDriftTableCompanion(
            id: id,
            userId: userId,
            version: version,
            updatedAt: updatedAt,
            deletedAt: deletedAt,
            syncState: syncState,
            payloadJson: payloadJson,
            rowid: rowid,
          ),
          createCompanionCallback: ({
            required String id,
            required String userId,
            Value<int> version = const Value.absent(),
            required String updatedAt,
            Value<String?> deletedAt = const Value.absent(),
            Value<String> syncState = const Value.absent(),
            required String payloadJson,
            Value<int> rowid = const Value.absent(),
          }) =>
              TimeBlocksDriftTableCompanion.insert(
            id: id,
            userId: userId,
            version: version,
            updatedAt: updatedAt,
            deletedAt: deletedAt,
            syncState: syncState,
            payloadJson: payloadJson,
            rowid: rowid,
          ),
          withReferenceMapper: (p0) => p0
              .map((e) => (e.readTable(table), BaseReferences(db, table, e)))
              .toList(),
          prefetchHooksCallback: null,
        ));
}

typedef $$TimeBlocksDriftTableTableProcessedTableManager
    = ProcessedTableManager<
        _$LifeOsDriftDatabase,
        $TimeBlocksDriftTableTable,
        TimeBlocksDriftTableData,
        $$TimeBlocksDriftTableTableFilterComposer,
        $$TimeBlocksDriftTableTableOrderingComposer,
        $$TimeBlocksDriftTableTableAnnotationComposer,
        $$TimeBlocksDriftTableTableCreateCompanionBuilder,
        $$TimeBlocksDriftTableTableUpdateCompanionBuilder,
        (
          TimeBlocksDriftTableData,
          BaseReferences<_$LifeOsDriftDatabase, $TimeBlocksDriftTableTable,
              TimeBlocksDriftTableData>
        ),
        TimeBlocksDriftTableData,
        PrefetchHooks Function()>;
typedef $$RemindersDriftTableTableCreateCompanionBuilder
    = RemindersDriftTableCompanion Function({
  required String id,
  required String userId,
  Value<int> version,
  required String updatedAt,
  Value<String?> deletedAt,
  Value<String> syncState,
  required String payloadJson,
  Value<int> rowid,
});
typedef $$RemindersDriftTableTableUpdateCompanionBuilder
    = RemindersDriftTableCompanion Function({
  Value<String> id,
  Value<String> userId,
  Value<int> version,
  Value<String> updatedAt,
  Value<String?> deletedAt,
  Value<String> syncState,
  Value<String> payloadJson,
  Value<int> rowid,
});

class $$RemindersDriftTableTableFilterComposer
    extends Composer<_$LifeOsDriftDatabase, $RemindersDriftTableTable> {
  $$RemindersDriftTableTableFilterComposer({
    required super.$db,
    required super.$table,
    super.joinBuilder,
    super.$addJoinBuilderToRootComposer,
    super.$removeJoinBuilderFromRootComposer,
  });
  ColumnFilters<String> get id => $composableBuilder(
      column: $table.id, builder: (column) => ColumnFilters(column));

  ColumnFilters<String> get userId => $composableBuilder(
      column: $table.userId, builder: (column) => ColumnFilters(column));

  ColumnFilters<int> get version => $composableBuilder(
      column: $table.version, builder: (column) => ColumnFilters(column));

  ColumnFilters<String> get updatedAt => $composableBuilder(
      column: $table.updatedAt, builder: (column) => ColumnFilters(column));

  ColumnFilters<String> get deletedAt => $composableBuilder(
      column: $table.deletedAt, builder: (column) => ColumnFilters(column));

  ColumnFilters<String> get syncState => $composableBuilder(
      column: $table.syncState, builder: (column) => ColumnFilters(column));

  ColumnFilters<String> get payloadJson => $composableBuilder(
      column: $table.payloadJson, builder: (column) => ColumnFilters(column));
}

class $$RemindersDriftTableTableOrderingComposer
    extends Composer<_$LifeOsDriftDatabase, $RemindersDriftTableTable> {
  $$RemindersDriftTableTableOrderingComposer({
    required super.$db,
    required super.$table,
    super.joinBuilder,
    super.$addJoinBuilderToRootComposer,
    super.$removeJoinBuilderFromRootComposer,
  });
  ColumnOrderings<String> get id => $composableBuilder(
      column: $table.id, builder: (column) => ColumnOrderings(column));

  ColumnOrderings<String> get userId => $composableBuilder(
      column: $table.userId, builder: (column) => ColumnOrderings(column));

  ColumnOrderings<int> get version => $composableBuilder(
      column: $table.version, builder: (column) => ColumnOrderings(column));

  ColumnOrderings<String> get updatedAt => $composableBuilder(
      column: $table.updatedAt, builder: (column) => ColumnOrderings(column));

  ColumnOrderings<String> get deletedAt => $composableBuilder(
      column: $table.deletedAt, builder: (column) => ColumnOrderings(column));

  ColumnOrderings<String> get syncState => $composableBuilder(
      column: $table.syncState, builder: (column) => ColumnOrderings(column));

  ColumnOrderings<String> get payloadJson => $composableBuilder(
      column: $table.payloadJson, builder: (column) => ColumnOrderings(column));
}

class $$RemindersDriftTableTableAnnotationComposer
    extends Composer<_$LifeOsDriftDatabase, $RemindersDriftTableTable> {
  $$RemindersDriftTableTableAnnotationComposer({
    required super.$db,
    required super.$table,
    super.joinBuilder,
    super.$addJoinBuilderToRootComposer,
    super.$removeJoinBuilderFromRootComposer,
  });
  GeneratedColumn<String> get id =>
      $composableBuilder(column: $table.id, builder: (column) => column);

  GeneratedColumn<String> get userId =>
      $composableBuilder(column: $table.userId, builder: (column) => column);

  GeneratedColumn<int> get version =>
      $composableBuilder(column: $table.version, builder: (column) => column);

  GeneratedColumn<String> get updatedAt =>
      $composableBuilder(column: $table.updatedAt, builder: (column) => column);

  GeneratedColumn<String> get deletedAt =>
      $composableBuilder(column: $table.deletedAt, builder: (column) => column);

  GeneratedColumn<String> get syncState =>
      $composableBuilder(column: $table.syncState, builder: (column) => column);

  GeneratedColumn<String> get payloadJson => $composableBuilder(
      column: $table.payloadJson, builder: (column) => column);
}

class $$RemindersDriftTableTableTableManager extends RootTableManager<
    _$LifeOsDriftDatabase,
    $RemindersDriftTableTable,
    RemindersDriftTableData,
    $$RemindersDriftTableTableFilterComposer,
    $$RemindersDriftTableTableOrderingComposer,
    $$RemindersDriftTableTableAnnotationComposer,
    $$RemindersDriftTableTableCreateCompanionBuilder,
    $$RemindersDriftTableTableUpdateCompanionBuilder,
    (
      RemindersDriftTableData,
      BaseReferences<_$LifeOsDriftDatabase, $RemindersDriftTableTable,
          RemindersDriftTableData>
    ),
    RemindersDriftTableData,
    PrefetchHooks Function()> {
  $$RemindersDriftTableTableTableManager(
      _$LifeOsDriftDatabase db, $RemindersDriftTableTable table)
      : super(TableManagerState(
          db: db,
          table: table,
          createFilteringComposer: () =>
              $$RemindersDriftTableTableFilterComposer($db: db, $table: table),
          createOrderingComposer: () =>
              $$RemindersDriftTableTableOrderingComposer(
                  $db: db, $table: table),
          createComputedFieldComposer: () =>
              $$RemindersDriftTableTableAnnotationComposer(
                  $db: db, $table: table),
          updateCompanionCallback: ({
            Value<String> id = const Value.absent(),
            Value<String> userId = const Value.absent(),
            Value<int> version = const Value.absent(),
            Value<String> updatedAt = const Value.absent(),
            Value<String?> deletedAt = const Value.absent(),
            Value<String> syncState = const Value.absent(),
            Value<String> payloadJson = const Value.absent(),
            Value<int> rowid = const Value.absent(),
          }) =>
              RemindersDriftTableCompanion(
            id: id,
            userId: userId,
            version: version,
            updatedAt: updatedAt,
            deletedAt: deletedAt,
            syncState: syncState,
            payloadJson: payloadJson,
            rowid: rowid,
          ),
          createCompanionCallback: ({
            required String id,
            required String userId,
            Value<int> version = const Value.absent(),
            required String updatedAt,
            Value<String?> deletedAt = const Value.absent(),
            Value<String> syncState = const Value.absent(),
            required String payloadJson,
            Value<int> rowid = const Value.absent(),
          }) =>
              RemindersDriftTableCompanion.insert(
            id: id,
            userId: userId,
            version: version,
            updatedAt: updatedAt,
            deletedAt: deletedAt,
            syncState: syncState,
            payloadJson: payloadJson,
            rowid: rowid,
          ),
          withReferenceMapper: (p0) => p0
              .map((e) => (e.readTable(table), BaseReferences(db, table, e)))
              .toList(),
          prefetchHooksCallback: null,
        ));
}

typedef $$RemindersDriftTableTableProcessedTableManager = ProcessedTableManager<
    _$LifeOsDriftDatabase,
    $RemindersDriftTableTable,
    RemindersDriftTableData,
    $$RemindersDriftTableTableFilterComposer,
    $$RemindersDriftTableTableOrderingComposer,
    $$RemindersDriftTableTableAnnotationComposer,
    $$RemindersDriftTableTableCreateCompanionBuilder,
    $$RemindersDriftTableTableUpdateCompanionBuilder,
    (
      RemindersDriftTableData,
      BaseReferences<_$LifeOsDriftDatabase, $RemindersDriftTableTable,
          RemindersDriftTableData>
    ),
    RemindersDriftTableData,
    PrefetchHooks Function()>;
typedef $$NotesDriftTableTableCreateCompanionBuilder = NotesDriftTableCompanion
    Function({
  required String id,
  required String userId,
  Value<int> version,
  required String updatedAt,
  Value<String?> deletedAt,
  Value<String> syncState,
  required String payloadJson,
  Value<int> rowid,
});
typedef $$NotesDriftTableTableUpdateCompanionBuilder = NotesDriftTableCompanion
    Function({
  Value<String> id,
  Value<String> userId,
  Value<int> version,
  Value<String> updatedAt,
  Value<String?> deletedAt,
  Value<String> syncState,
  Value<String> payloadJson,
  Value<int> rowid,
});

class $$NotesDriftTableTableFilterComposer
    extends Composer<_$LifeOsDriftDatabase, $NotesDriftTableTable> {
  $$NotesDriftTableTableFilterComposer({
    required super.$db,
    required super.$table,
    super.joinBuilder,
    super.$addJoinBuilderToRootComposer,
    super.$removeJoinBuilderFromRootComposer,
  });
  ColumnFilters<String> get id => $composableBuilder(
      column: $table.id, builder: (column) => ColumnFilters(column));

  ColumnFilters<String> get userId => $composableBuilder(
      column: $table.userId, builder: (column) => ColumnFilters(column));

  ColumnFilters<int> get version => $composableBuilder(
      column: $table.version, builder: (column) => ColumnFilters(column));

  ColumnFilters<String> get updatedAt => $composableBuilder(
      column: $table.updatedAt, builder: (column) => ColumnFilters(column));

  ColumnFilters<String> get deletedAt => $composableBuilder(
      column: $table.deletedAt, builder: (column) => ColumnFilters(column));

  ColumnFilters<String> get syncState => $composableBuilder(
      column: $table.syncState, builder: (column) => ColumnFilters(column));

  ColumnFilters<String> get payloadJson => $composableBuilder(
      column: $table.payloadJson, builder: (column) => ColumnFilters(column));
}

class $$NotesDriftTableTableOrderingComposer
    extends Composer<_$LifeOsDriftDatabase, $NotesDriftTableTable> {
  $$NotesDriftTableTableOrderingComposer({
    required super.$db,
    required super.$table,
    super.joinBuilder,
    super.$addJoinBuilderToRootComposer,
    super.$removeJoinBuilderFromRootComposer,
  });
  ColumnOrderings<String> get id => $composableBuilder(
      column: $table.id, builder: (column) => ColumnOrderings(column));

  ColumnOrderings<String> get userId => $composableBuilder(
      column: $table.userId, builder: (column) => ColumnOrderings(column));

  ColumnOrderings<int> get version => $composableBuilder(
      column: $table.version, builder: (column) => ColumnOrderings(column));

  ColumnOrderings<String> get updatedAt => $composableBuilder(
      column: $table.updatedAt, builder: (column) => ColumnOrderings(column));

  ColumnOrderings<String> get deletedAt => $composableBuilder(
      column: $table.deletedAt, builder: (column) => ColumnOrderings(column));

  ColumnOrderings<String> get syncState => $composableBuilder(
      column: $table.syncState, builder: (column) => ColumnOrderings(column));

  ColumnOrderings<String> get payloadJson => $composableBuilder(
      column: $table.payloadJson, builder: (column) => ColumnOrderings(column));
}

class $$NotesDriftTableTableAnnotationComposer
    extends Composer<_$LifeOsDriftDatabase, $NotesDriftTableTable> {
  $$NotesDriftTableTableAnnotationComposer({
    required super.$db,
    required super.$table,
    super.joinBuilder,
    super.$addJoinBuilderToRootComposer,
    super.$removeJoinBuilderFromRootComposer,
  });
  GeneratedColumn<String> get id =>
      $composableBuilder(column: $table.id, builder: (column) => column);

  GeneratedColumn<String> get userId =>
      $composableBuilder(column: $table.userId, builder: (column) => column);

  GeneratedColumn<int> get version =>
      $composableBuilder(column: $table.version, builder: (column) => column);

  GeneratedColumn<String> get updatedAt =>
      $composableBuilder(column: $table.updatedAt, builder: (column) => column);

  GeneratedColumn<String> get deletedAt =>
      $composableBuilder(column: $table.deletedAt, builder: (column) => column);

  GeneratedColumn<String> get syncState =>
      $composableBuilder(column: $table.syncState, builder: (column) => column);

  GeneratedColumn<String> get payloadJson => $composableBuilder(
      column: $table.payloadJson, builder: (column) => column);
}

class $$NotesDriftTableTableTableManager extends RootTableManager<
    _$LifeOsDriftDatabase,
    $NotesDriftTableTable,
    NotesDriftTableData,
    $$NotesDriftTableTableFilterComposer,
    $$NotesDriftTableTableOrderingComposer,
    $$NotesDriftTableTableAnnotationComposer,
    $$NotesDriftTableTableCreateCompanionBuilder,
    $$NotesDriftTableTableUpdateCompanionBuilder,
    (
      NotesDriftTableData,
      BaseReferences<_$LifeOsDriftDatabase, $NotesDriftTableTable,
          NotesDriftTableData>
    ),
    NotesDriftTableData,
    PrefetchHooks Function()> {
  $$NotesDriftTableTableTableManager(
      _$LifeOsDriftDatabase db, $NotesDriftTableTable table)
      : super(TableManagerState(
          db: db,
          table: table,
          createFilteringComposer: () =>
              $$NotesDriftTableTableFilterComposer($db: db, $table: table),
          createOrderingComposer: () =>
              $$NotesDriftTableTableOrderingComposer($db: db, $table: table),
          createComputedFieldComposer: () =>
              $$NotesDriftTableTableAnnotationComposer($db: db, $table: table),
          updateCompanionCallback: ({
            Value<String> id = const Value.absent(),
            Value<String> userId = const Value.absent(),
            Value<int> version = const Value.absent(),
            Value<String> updatedAt = const Value.absent(),
            Value<String?> deletedAt = const Value.absent(),
            Value<String> syncState = const Value.absent(),
            Value<String> payloadJson = const Value.absent(),
            Value<int> rowid = const Value.absent(),
          }) =>
              NotesDriftTableCompanion(
            id: id,
            userId: userId,
            version: version,
            updatedAt: updatedAt,
            deletedAt: deletedAt,
            syncState: syncState,
            payloadJson: payloadJson,
            rowid: rowid,
          ),
          createCompanionCallback: ({
            required String id,
            required String userId,
            Value<int> version = const Value.absent(),
            required String updatedAt,
            Value<String?> deletedAt = const Value.absent(),
            Value<String> syncState = const Value.absent(),
            required String payloadJson,
            Value<int> rowid = const Value.absent(),
          }) =>
              NotesDriftTableCompanion.insert(
            id: id,
            userId: userId,
            version: version,
            updatedAt: updatedAt,
            deletedAt: deletedAt,
            syncState: syncState,
            payloadJson: payloadJson,
            rowid: rowid,
          ),
          withReferenceMapper: (p0) => p0
              .map((e) => (e.readTable(table), BaseReferences(db, table, e)))
              .toList(),
          prefetchHooksCallback: null,
        ));
}

typedef $$NotesDriftTableTableProcessedTableManager = ProcessedTableManager<
    _$LifeOsDriftDatabase,
    $NotesDriftTableTable,
    NotesDriftTableData,
    $$NotesDriftTableTableFilterComposer,
    $$NotesDriftTableTableOrderingComposer,
    $$NotesDriftTableTableAnnotationComposer,
    $$NotesDriftTableTableCreateCompanionBuilder,
    $$NotesDriftTableTableUpdateCompanionBuilder,
    (
      NotesDriftTableData,
      BaseReferences<_$LifeOsDriftDatabase, $NotesDriftTableTable,
          NotesDriftTableData>
    ),
    NotesDriftTableData,
    PrefetchHooks Function()>;
typedef $$TagsDriftTableTableCreateCompanionBuilder = TagsDriftTableCompanion
    Function({
  required String id,
  required String userId,
  Value<int> version,
  required String updatedAt,
  Value<String?> deletedAt,
  Value<String> syncState,
  required String payloadJson,
  Value<int> rowid,
});
typedef $$TagsDriftTableTableUpdateCompanionBuilder = TagsDriftTableCompanion
    Function({
  Value<String> id,
  Value<String> userId,
  Value<int> version,
  Value<String> updatedAt,
  Value<String?> deletedAt,
  Value<String> syncState,
  Value<String> payloadJson,
  Value<int> rowid,
});

class $$TagsDriftTableTableFilterComposer
    extends Composer<_$LifeOsDriftDatabase, $TagsDriftTableTable> {
  $$TagsDriftTableTableFilterComposer({
    required super.$db,
    required super.$table,
    super.joinBuilder,
    super.$addJoinBuilderToRootComposer,
    super.$removeJoinBuilderFromRootComposer,
  });
  ColumnFilters<String> get id => $composableBuilder(
      column: $table.id, builder: (column) => ColumnFilters(column));

  ColumnFilters<String> get userId => $composableBuilder(
      column: $table.userId, builder: (column) => ColumnFilters(column));

  ColumnFilters<int> get version => $composableBuilder(
      column: $table.version, builder: (column) => ColumnFilters(column));

  ColumnFilters<String> get updatedAt => $composableBuilder(
      column: $table.updatedAt, builder: (column) => ColumnFilters(column));

  ColumnFilters<String> get deletedAt => $composableBuilder(
      column: $table.deletedAt, builder: (column) => ColumnFilters(column));

  ColumnFilters<String> get syncState => $composableBuilder(
      column: $table.syncState, builder: (column) => ColumnFilters(column));

  ColumnFilters<String> get payloadJson => $composableBuilder(
      column: $table.payloadJson, builder: (column) => ColumnFilters(column));
}

class $$TagsDriftTableTableOrderingComposer
    extends Composer<_$LifeOsDriftDatabase, $TagsDriftTableTable> {
  $$TagsDriftTableTableOrderingComposer({
    required super.$db,
    required super.$table,
    super.joinBuilder,
    super.$addJoinBuilderToRootComposer,
    super.$removeJoinBuilderFromRootComposer,
  });
  ColumnOrderings<String> get id => $composableBuilder(
      column: $table.id, builder: (column) => ColumnOrderings(column));

  ColumnOrderings<String> get userId => $composableBuilder(
      column: $table.userId, builder: (column) => ColumnOrderings(column));

  ColumnOrderings<int> get version => $composableBuilder(
      column: $table.version, builder: (column) => ColumnOrderings(column));

  ColumnOrderings<String> get updatedAt => $composableBuilder(
      column: $table.updatedAt, builder: (column) => ColumnOrderings(column));

  ColumnOrderings<String> get deletedAt => $composableBuilder(
      column: $table.deletedAt, builder: (column) => ColumnOrderings(column));

  ColumnOrderings<String> get syncState => $composableBuilder(
      column: $table.syncState, builder: (column) => ColumnOrderings(column));

  ColumnOrderings<String> get payloadJson => $composableBuilder(
      column: $table.payloadJson, builder: (column) => ColumnOrderings(column));
}

class $$TagsDriftTableTableAnnotationComposer
    extends Composer<_$LifeOsDriftDatabase, $TagsDriftTableTable> {
  $$TagsDriftTableTableAnnotationComposer({
    required super.$db,
    required super.$table,
    super.joinBuilder,
    super.$addJoinBuilderToRootComposer,
    super.$removeJoinBuilderFromRootComposer,
  });
  GeneratedColumn<String> get id =>
      $composableBuilder(column: $table.id, builder: (column) => column);

  GeneratedColumn<String> get userId =>
      $composableBuilder(column: $table.userId, builder: (column) => column);

  GeneratedColumn<int> get version =>
      $composableBuilder(column: $table.version, builder: (column) => column);

  GeneratedColumn<String> get updatedAt =>
      $composableBuilder(column: $table.updatedAt, builder: (column) => column);

  GeneratedColumn<String> get deletedAt =>
      $composableBuilder(column: $table.deletedAt, builder: (column) => column);

  GeneratedColumn<String> get syncState =>
      $composableBuilder(column: $table.syncState, builder: (column) => column);

  GeneratedColumn<String> get payloadJson => $composableBuilder(
      column: $table.payloadJson, builder: (column) => column);
}

class $$TagsDriftTableTableTableManager extends RootTableManager<
    _$LifeOsDriftDatabase,
    $TagsDriftTableTable,
    TagsDriftTableData,
    $$TagsDriftTableTableFilterComposer,
    $$TagsDriftTableTableOrderingComposer,
    $$TagsDriftTableTableAnnotationComposer,
    $$TagsDriftTableTableCreateCompanionBuilder,
    $$TagsDriftTableTableUpdateCompanionBuilder,
    (
      TagsDriftTableData,
      BaseReferences<_$LifeOsDriftDatabase, $TagsDriftTableTable,
          TagsDriftTableData>
    ),
    TagsDriftTableData,
    PrefetchHooks Function()> {
  $$TagsDriftTableTableTableManager(
      _$LifeOsDriftDatabase db, $TagsDriftTableTable table)
      : super(TableManagerState(
          db: db,
          table: table,
          createFilteringComposer: () =>
              $$TagsDriftTableTableFilterComposer($db: db, $table: table),
          createOrderingComposer: () =>
              $$TagsDriftTableTableOrderingComposer($db: db, $table: table),
          createComputedFieldComposer: () =>
              $$TagsDriftTableTableAnnotationComposer($db: db, $table: table),
          updateCompanionCallback: ({
            Value<String> id = const Value.absent(),
            Value<String> userId = const Value.absent(),
            Value<int> version = const Value.absent(),
            Value<String> updatedAt = const Value.absent(),
            Value<String?> deletedAt = const Value.absent(),
            Value<String> syncState = const Value.absent(),
            Value<String> payloadJson = const Value.absent(),
            Value<int> rowid = const Value.absent(),
          }) =>
              TagsDriftTableCompanion(
            id: id,
            userId: userId,
            version: version,
            updatedAt: updatedAt,
            deletedAt: deletedAt,
            syncState: syncState,
            payloadJson: payloadJson,
            rowid: rowid,
          ),
          createCompanionCallback: ({
            required String id,
            required String userId,
            Value<int> version = const Value.absent(),
            required String updatedAt,
            Value<String?> deletedAt = const Value.absent(),
            Value<String> syncState = const Value.absent(),
            required String payloadJson,
            Value<int> rowid = const Value.absent(),
          }) =>
              TagsDriftTableCompanion.insert(
            id: id,
            userId: userId,
            version: version,
            updatedAt: updatedAt,
            deletedAt: deletedAt,
            syncState: syncState,
            payloadJson: payloadJson,
            rowid: rowid,
          ),
          withReferenceMapper: (p0) => p0
              .map((e) => (e.readTable(table), BaseReferences(db, table, e)))
              .toList(),
          prefetchHooksCallback: null,
        ));
}

typedef $$TagsDriftTableTableProcessedTableManager = ProcessedTableManager<
    _$LifeOsDriftDatabase,
    $TagsDriftTableTable,
    TagsDriftTableData,
    $$TagsDriftTableTableFilterComposer,
    $$TagsDriftTableTableOrderingComposer,
    $$TagsDriftTableTableAnnotationComposer,
    $$TagsDriftTableTableCreateCompanionBuilder,
    $$TagsDriftTableTableUpdateCompanionBuilder,
    (
      TagsDriftTableData,
      BaseReferences<_$LifeOsDriftDatabase, $TagsDriftTableTable,
          TagsDriftTableData>
    ),
    TagsDriftTableData,
    PrefetchHooks Function()>;
typedef $$NoteTagsDriftTableTableCreateCompanionBuilder
    = NoteTagsDriftTableCompanion Function({
  required String id,
  required String userId,
  Value<int> version,
  required String updatedAt,
  Value<String?> deletedAt,
  Value<String> syncState,
  required String payloadJson,
  Value<int> rowid,
});
typedef $$NoteTagsDriftTableTableUpdateCompanionBuilder
    = NoteTagsDriftTableCompanion Function({
  Value<String> id,
  Value<String> userId,
  Value<int> version,
  Value<String> updatedAt,
  Value<String?> deletedAt,
  Value<String> syncState,
  Value<String> payloadJson,
  Value<int> rowid,
});

class $$NoteTagsDriftTableTableFilterComposer
    extends Composer<_$LifeOsDriftDatabase, $NoteTagsDriftTableTable> {
  $$NoteTagsDriftTableTableFilterComposer({
    required super.$db,
    required super.$table,
    super.joinBuilder,
    super.$addJoinBuilderToRootComposer,
    super.$removeJoinBuilderFromRootComposer,
  });
  ColumnFilters<String> get id => $composableBuilder(
      column: $table.id, builder: (column) => ColumnFilters(column));

  ColumnFilters<String> get userId => $composableBuilder(
      column: $table.userId, builder: (column) => ColumnFilters(column));

  ColumnFilters<int> get version => $composableBuilder(
      column: $table.version, builder: (column) => ColumnFilters(column));

  ColumnFilters<String> get updatedAt => $composableBuilder(
      column: $table.updatedAt, builder: (column) => ColumnFilters(column));

  ColumnFilters<String> get deletedAt => $composableBuilder(
      column: $table.deletedAt, builder: (column) => ColumnFilters(column));

  ColumnFilters<String> get syncState => $composableBuilder(
      column: $table.syncState, builder: (column) => ColumnFilters(column));

  ColumnFilters<String> get payloadJson => $composableBuilder(
      column: $table.payloadJson, builder: (column) => ColumnFilters(column));
}

class $$NoteTagsDriftTableTableOrderingComposer
    extends Composer<_$LifeOsDriftDatabase, $NoteTagsDriftTableTable> {
  $$NoteTagsDriftTableTableOrderingComposer({
    required super.$db,
    required super.$table,
    super.joinBuilder,
    super.$addJoinBuilderToRootComposer,
    super.$removeJoinBuilderFromRootComposer,
  });
  ColumnOrderings<String> get id => $composableBuilder(
      column: $table.id, builder: (column) => ColumnOrderings(column));

  ColumnOrderings<String> get userId => $composableBuilder(
      column: $table.userId, builder: (column) => ColumnOrderings(column));

  ColumnOrderings<int> get version => $composableBuilder(
      column: $table.version, builder: (column) => ColumnOrderings(column));

  ColumnOrderings<String> get updatedAt => $composableBuilder(
      column: $table.updatedAt, builder: (column) => ColumnOrderings(column));

  ColumnOrderings<String> get deletedAt => $composableBuilder(
      column: $table.deletedAt, builder: (column) => ColumnOrderings(column));

  ColumnOrderings<String> get syncState => $composableBuilder(
      column: $table.syncState, builder: (column) => ColumnOrderings(column));

  ColumnOrderings<String> get payloadJson => $composableBuilder(
      column: $table.payloadJson, builder: (column) => ColumnOrderings(column));
}

class $$NoteTagsDriftTableTableAnnotationComposer
    extends Composer<_$LifeOsDriftDatabase, $NoteTagsDriftTableTable> {
  $$NoteTagsDriftTableTableAnnotationComposer({
    required super.$db,
    required super.$table,
    super.joinBuilder,
    super.$addJoinBuilderToRootComposer,
    super.$removeJoinBuilderFromRootComposer,
  });
  GeneratedColumn<String> get id =>
      $composableBuilder(column: $table.id, builder: (column) => column);

  GeneratedColumn<String> get userId =>
      $composableBuilder(column: $table.userId, builder: (column) => column);

  GeneratedColumn<int> get version =>
      $composableBuilder(column: $table.version, builder: (column) => column);

  GeneratedColumn<String> get updatedAt =>
      $composableBuilder(column: $table.updatedAt, builder: (column) => column);

  GeneratedColumn<String> get deletedAt =>
      $composableBuilder(column: $table.deletedAt, builder: (column) => column);

  GeneratedColumn<String> get syncState =>
      $composableBuilder(column: $table.syncState, builder: (column) => column);

  GeneratedColumn<String> get payloadJson => $composableBuilder(
      column: $table.payloadJson, builder: (column) => column);
}

class $$NoteTagsDriftTableTableTableManager extends RootTableManager<
    _$LifeOsDriftDatabase,
    $NoteTagsDriftTableTable,
    NoteTagsDriftTableData,
    $$NoteTagsDriftTableTableFilterComposer,
    $$NoteTagsDriftTableTableOrderingComposer,
    $$NoteTagsDriftTableTableAnnotationComposer,
    $$NoteTagsDriftTableTableCreateCompanionBuilder,
    $$NoteTagsDriftTableTableUpdateCompanionBuilder,
    (
      NoteTagsDriftTableData,
      BaseReferences<_$LifeOsDriftDatabase, $NoteTagsDriftTableTable,
          NoteTagsDriftTableData>
    ),
    NoteTagsDriftTableData,
    PrefetchHooks Function()> {
  $$NoteTagsDriftTableTableTableManager(
      _$LifeOsDriftDatabase db, $NoteTagsDriftTableTable table)
      : super(TableManagerState(
          db: db,
          table: table,
          createFilteringComposer: () =>
              $$NoteTagsDriftTableTableFilterComposer($db: db, $table: table),
          createOrderingComposer: () =>
              $$NoteTagsDriftTableTableOrderingComposer($db: db, $table: table),
          createComputedFieldComposer: () =>
              $$NoteTagsDriftTableTableAnnotationComposer(
                  $db: db, $table: table),
          updateCompanionCallback: ({
            Value<String> id = const Value.absent(),
            Value<String> userId = const Value.absent(),
            Value<int> version = const Value.absent(),
            Value<String> updatedAt = const Value.absent(),
            Value<String?> deletedAt = const Value.absent(),
            Value<String> syncState = const Value.absent(),
            Value<String> payloadJson = const Value.absent(),
            Value<int> rowid = const Value.absent(),
          }) =>
              NoteTagsDriftTableCompanion(
            id: id,
            userId: userId,
            version: version,
            updatedAt: updatedAt,
            deletedAt: deletedAt,
            syncState: syncState,
            payloadJson: payloadJson,
            rowid: rowid,
          ),
          createCompanionCallback: ({
            required String id,
            required String userId,
            Value<int> version = const Value.absent(),
            required String updatedAt,
            Value<String?> deletedAt = const Value.absent(),
            Value<String> syncState = const Value.absent(),
            required String payloadJson,
            Value<int> rowid = const Value.absent(),
          }) =>
              NoteTagsDriftTableCompanion.insert(
            id: id,
            userId: userId,
            version: version,
            updatedAt: updatedAt,
            deletedAt: deletedAt,
            syncState: syncState,
            payloadJson: payloadJson,
            rowid: rowid,
          ),
          withReferenceMapper: (p0) => p0
              .map((e) => (e.readTable(table), BaseReferences(db, table, e)))
              .toList(),
          prefetchHooksCallback: null,
        ));
}

typedef $$NoteTagsDriftTableTableProcessedTableManager = ProcessedTableManager<
    _$LifeOsDriftDatabase,
    $NoteTagsDriftTableTable,
    NoteTagsDriftTableData,
    $$NoteTagsDriftTableTableFilterComposer,
    $$NoteTagsDriftTableTableOrderingComposer,
    $$NoteTagsDriftTableTableAnnotationComposer,
    $$NoteTagsDriftTableTableCreateCompanionBuilder,
    $$NoteTagsDriftTableTableUpdateCompanionBuilder,
    (
      NoteTagsDriftTableData,
      BaseReferences<_$LifeOsDriftDatabase, $NoteTagsDriftTableTable,
          NoteTagsDriftTableData>
    ),
    NoteTagsDriftTableData,
    PrefetchHooks Function()>;
typedef $$NoteLinksDriftTableTableCreateCompanionBuilder
    = NoteLinksDriftTableCompanion Function({
  required String id,
  required String userId,
  Value<int> version,
  required String updatedAt,
  Value<String?> deletedAt,
  Value<String> syncState,
  required String payloadJson,
  Value<int> rowid,
});
typedef $$NoteLinksDriftTableTableUpdateCompanionBuilder
    = NoteLinksDriftTableCompanion Function({
  Value<String> id,
  Value<String> userId,
  Value<int> version,
  Value<String> updatedAt,
  Value<String?> deletedAt,
  Value<String> syncState,
  Value<String> payloadJson,
  Value<int> rowid,
});

class $$NoteLinksDriftTableTableFilterComposer
    extends Composer<_$LifeOsDriftDatabase, $NoteLinksDriftTableTable> {
  $$NoteLinksDriftTableTableFilterComposer({
    required super.$db,
    required super.$table,
    super.joinBuilder,
    super.$addJoinBuilderToRootComposer,
    super.$removeJoinBuilderFromRootComposer,
  });
  ColumnFilters<String> get id => $composableBuilder(
      column: $table.id, builder: (column) => ColumnFilters(column));

  ColumnFilters<String> get userId => $composableBuilder(
      column: $table.userId, builder: (column) => ColumnFilters(column));

  ColumnFilters<int> get version => $composableBuilder(
      column: $table.version, builder: (column) => ColumnFilters(column));

  ColumnFilters<String> get updatedAt => $composableBuilder(
      column: $table.updatedAt, builder: (column) => ColumnFilters(column));

  ColumnFilters<String> get deletedAt => $composableBuilder(
      column: $table.deletedAt, builder: (column) => ColumnFilters(column));

  ColumnFilters<String> get syncState => $composableBuilder(
      column: $table.syncState, builder: (column) => ColumnFilters(column));

  ColumnFilters<String> get payloadJson => $composableBuilder(
      column: $table.payloadJson, builder: (column) => ColumnFilters(column));
}

class $$NoteLinksDriftTableTableOrderingComposer
    extends Composer<_$LifeOsDriftDatabase, $NoteLinksDriftTableTable> {
  $$NoteLinksDriftTableTableOrderingComposer({
    required super.$db,
    required super.$table,
    super.joinBuilder,
    super.$addJoinBuilderToRootComposer,
    super.$removeJoinBuilderFromRootComposer,
  });
  ColumnOrderings<String> get id => $composableBuilder(
      column: $table.id, builder: (column) => ColumnOrderings(column));

  ColumnOrderings<String> get userId => $composableBuilder(
      column: $table.userId, builder: (column) => ColumnOrderings(column));

  ColumnOrderings<int> get version => $composableBuilder(
      column: $table.version, builder: (column) => ColumnOrderings(column));

  ColumnOrderings<String> get updatedAt => $composableBuilder(
      column: $table.updatedAt, builder: (column) => ColumnOrderings(column));

  ColumnOrderings<String> get deletedAt => $composableBuilder(
      column: $table.deletedAt, builder: (column) => ColumnOrderings(column));

  ColumnOrderings<String> get syncState => $composableBuilder(
      column: $table.syncState, builder: (column) => ColumnOrderings(column));

  ColumnOrderings<String> get payloadJson => $composableBuilder(
      column: $table.payloadJson, builder: (column) => ColumnOrderings(column));
}

class $$NoteLinksDriftTableTableAnnotationComposer
    extends Composer<_$LifeOsDriftDatabase, $NoteLinksDriftTableTable> {
  $$NoteLinksDriftTableTableAnnotationComposer({
    required super.$db,
    required super.$table,
    super.joinBuilder,
    super.$addJoinBuilderToRootComposer,
    super.$removeJoinBuilderFromRootComposer,
  });
  GeneratedColumn<String> get id =>
      $composableBuilder(column: $table.id, builder: (column) => column);

  GeneratedColumn<String> get userId =>
      $composableBuilder(column: $table.userId, builder: (column) => column);

  GeneratedColumn<int> get version =>
      $composableBuilder(column: $table.version, builder: (column) => column);

  GeneratedColumn<String> get updatedAt =>
      $composableBuilder(column: $table.updatedAt, builder: (column) => column);

  GeneratedColumn<String> get deletedAt =>
      $composableBuilder(column: $table.deletedAt, builder: (column) => column);

  GeneratedColumn<String> get syncState =>
      $composableBuilder(column: $table.syncState, builder: (column) => column);

  GeneratedColumn<String> get payloadJson => $composableBuilder(
      column: $table.payloadJson, builder: (column) => column);
}

class $$NoteLinksDriftTableTableTableManager extends RootTableManager<
    _$LifeOsDriftDatabase,
    $NoteLinksDriftTableTable,
    NoteLinksDriftTableData,
    $$NoteLinksDriftTableTableFilterComposer,
    $$NoteLinksDriftTableTableOrderingComposer,
    $$NoteLinksDriftTableTableAnnotationComposer,
    $$NoteLinksDriftTableTableCreateCompanionBuilder,
    $$NoteLinksDriftTableTableUpdateCompanionBuilder,
    (
      NoteLinksDriftTableData,
      BaseReferences<_$LifeOsDriftDatabase, $NoteLinksDriftTableTable,
          NoteLinksDriftTableData>
    ),
    NoteLinksDriftTableData,
    PrefetchHooks Function()> {
  $$NoteLinksDriftTableTableTableManager(
      _$LifeOsDriftDatabase db, $NoteLinksDriftTableTable table)
      : super(TableManagerState(
          db: db,
          table: table,
          createFilteringComposer: () =>
              $$NoteLinksDriftTableTableFilterComposer($db: db, $table: table),
          createOrderingComposer: () =>
              $$NoteLinksDriftTableTableOrderingComposer(
                  $db: db, $table: table),
          createComputedFieldComposer: () =>
              $$NoteLinksDriftTableTableAnnotationComposer(
                  $db: db, $table: table),
          updateCompanionCallback: ({
            Value<String> id = const Value.absent(),
            Value<String> userId = const Value.absent(),
            Value<int> version = const Value.absent(),
            Value<String> updatedAt = const Value.absent(),
            Value<String?> deletedAt = const Value.absent(),
            Value<String> syncState = const Value.absent(),
            Value<String> payloadJson = const Value.absent(),
            Value<int> rowid = const Value.absent(),
          }) =>
              NoteLinksDriftTableCompanion(
            id: id,
            userId: userId,
            version: version,
            updatedAt: updatedAt,
            deletedAt: deletedAt,
            syncState: syncState,
            payloadJson: payloadJson,
            rowid: rowid,
          ),
          createCompanionCallback: ({
            required String id,
            required String userId,
            Value<int> version = const Value.absent(),
            required String updatedAt,
            Value<String?> deletedAt = const Value.absent(),
            Value<String> syncState = const Value.absent(),
            required String payloadJson,
            Value<int> rowid = const Value.absent(),
          }) =>
              NoteLinksDriftTableCompanion.insert(
            id: id,
            userId: userId,
            version: version,
            updatedAt: updatedAt,
            deletedAt: deletedAt,
            syncState: syncState,
            payloadJson: payloadJson,
            rowid: rowid,
          ),
          withReferenceMapper: (p0) => p0
              .map((e) => (e.readTable(table), BaseReferences(db, table, e)))
              .toList(),
          prefetchHooksCallback: null,
        ));
}

typedef $$NoteLinksDriftTableTableProcessedTableManager = ProcessedTableManager<
    _$LifeOsDriftDatabase,
    $NoteLinksDriftTableTable,
    NoteLinksDriftTableData,
    $$NoteLinksDriftTableTableFilterComposer,
    $$NoteLinksDriftTableTableOrderingComposer,
    $$NoteLinksDriftTableTableAnnotationComposer,
    $$NoteLinksDriftTableTableCreateCompanionBuilder,
    $$NoteLinksDriftTableTableUpdateCompanionBuilder,
    (
      NoteLinksDriftTableData,
      BaseReferences<_$LifeOsDriftDatabase, $NoteLinksDriftTableTable,
          NoteLinksDriftTableData>
    ),
    NoteLinksDriftTableData,
    PrefetchHooks Function()>;
typedef $$GoalsDriftTableTableCreateCompanionBuilder = GoalsDriftTableCompanion
    Function({
  required String id,
  required String userId,
  Value<int> version,
  required String updatedAt,
  Value<String?> deletedAt,
  Value<String> syncState,
  required String payloadJson,
  Value<int> rowid,
});
typedef $$GoalsDriftTableTableUpdateCompanionBuilder = GoalsDriftTableCompanion
    Function({
  Value<String> id,
  Value<String> userId,
  Value<int> version,
  Value<String> updatedAt,
  Value<String?> deletedAt,
  Value<String> syncState,
  Value<String> payloadJson,
  Value<int> rowid,
});

class $$GoalsDriftTableTableFilterComposer
    extends Composer<_$LifeOsDriftDatabase, $GoalsDriftTableTable> {
  $$GoalsDriftTableTableFilterComposer({
    required super.$db,
    required super.$table,
    super.joinBuilder,
    super.$addJoinBuilderToRootComposer,
    super.$removeJoinBuilderFromRootComposer,
  });
  ColumnFilters<String> get id => $composableBuilder(
      column: $table.id, builder: (column) => ColumnFilters(column));

  ColumnFilters<String> get userId => $composableBuilder(
      column: $table.userId, builder: (column) => ColumnFilters(column));

  ColumnFilters<int> get version => $composableBuilder(
      column: $table.version, builder: (column) => ColumnFilters(column));

  ColumnFilters<String> get updatedAt => $composableBuilder(
      column: $table.updatedAt, builder: (column) => ColumnFilters(column));

  ColumnFilters<String> get deletedAt => $composableBuilder(
      column: $table.deletedAt, builder: (column) => ColumnFilters(column));

  ColumnFilters<String> get syncState => $composableBuilder(
      column: $table.syncState, builder: (column) => ColumnFilters(column));

  ColumnFilters<String> get payloadJson => $composableBuilder(
      column: $table.payloadJson, builder: (column) => ColumnFilters(column));
}

class $$GoalsDriftTableTableOrderingComposer
    extends Composer<_$LifeOsDriftDatabase, $GoalsDriftTableTable> {
  $$GoalsDriftTableTableOrderingComposer({
    required super.$db,
    required super.$table,
    super.joinBuilder,
    super.$addJoinBuilderToRootComposer,
    super.$removeJoinBuilderFromRootComposer,
  });
  ColumnOrderings<String> get id => $composableBuilder(
      column: $table.id, builder: (column) => ColumnOrderings(column));

  ColumnOrderings<String> get userId => $composableBuilder(
      column: $table.userId, builder: (column) => ColumnOrderings(column));

  ColumnOrderings<int> get version => $composableBuilder(
      column: $table.version, builder: (column) => ColumnOrderings(column));

  ColumnOrderings<String> get updatedAt => $composableBuilder(
      column: $table.updatedAt, builder: (column) => ColumnOrderings(column));

  ColumnOrderings<String> get deletedAt => $composableBuilder(
      column: $table.deletedAt, builder: (column) => ColumnOrderings(column));

  ColumnOrderings<String> get syncState => $composableBuilder(
      column: $table.syncState, builder: (column) => ColumnOrderings(column));

  ColumnOrderings<String> get payloadJson => $composableBuilder(
      column: $table.payloadJson, builder: (column) => ColumnOrderings(column));
}

class $$GoalsDriftTableTableAnnotationComposer
    extends Composer<_$LifeOsDriftDatabase, $GoalsDriftTableTable> {
  $$GoalsDriftTableTableAnnotationComposer({
    required super.$db,
    required super.$table,
    super.joinBuilder,
    super.$addJoinBuilderToRootComposer,
    super.$removeJoinBuilderFromRootComposer,
  });
  GeneratedColumn<String> get id =>
      $composableBuilder(column: $table.id, builder: (column) => column);

  GeneratedColumn<String> get userId =>
      $composableBuilder(column: $table.userId, builder: (column) => column);

  GeneratedColumn<int> get version =>
      $composableBuilder(column: $table.version, builder: (column) => column);

  GeneratedColumn<String> get updatedAt =>
      $composableBuilder(column: $table.updatedAt, builder: (column) => column);

  GeneratedColumn<String> get deletedAt =>
      $composableBuilder(column: $table.deletedAt, builder: (column) => column);

  GeneratedColumn<String> get syncState =>
      $composableBuilder(column: $table.syncState, builder: (column) => column);

  GeneratedColumn<String> get payloadJson => $composableBuilder(
      column: $table.payloadJson, builder: (column) => column);
}

class $$GoalsDriftTableTableTableManager extends RootTableManager<
    _$LifeOsDriftDatabase,
    $GoalsDriftTableTable,
    GoalsDriftTableData,
    $$GoalsDriftTableTableFilterComposer,
    $$GoalsDriftTableTableOrderingComposer,
    $$GoalsDriftTableTableAnnotationComposer,
    $$GoalsDriftTableTableCreateCompanionBuilder,
    $$GoalsDriftTableTableUpdateCompanionBuilder,
    (
      GoalsDriftTableData,
      BaseReferences<_$LifeOsDriftDatabase, $GoalsDriftTableTable,
          GoalsDriftTableData>
    ),
    GoalsDriftTableData,
    PrefetchHooks Function()> {
  $$GoalsDriftTableTableTableManager(
      _$LifeOsDriftDatabase db, $GoalsDriftTableTable table)
      : super(TableManagerState(
          db: db,
          table: table,
          createFilteringComposer: () =>
              $$GoalsDriftTableTableFilterComposer($db: db, $table: table),
          createOrderingComposer: () =>
              $$GoalsDriftTableTableOrderingComposer($db: db, $table: table),
          createComputedFieldComposer: () =>
              $$GoalsDriftTableTableAnnotationComposer($db: db, $table: table),
          updateCompanionCallback: ({
            Value<String> id = const Value.absent(),
            Value<String> userId = const Value.absent(),
            Value<int> version = const Value.absent(),
            Value<String> updatedAt = const Value.absent(),
            Value<String?> deletedAt = const Value.absent(),
            Value<String> syncState = const Value.absent(),
            Value<String> payloadJson = const Value.absent(),
            Value<int> rowid = const Value.absent(),
          }) =>
              GoalsDriftTableCompanion(
            id: id,
            userId: userId,
            version: version,
            updatedAt: updatedAt,
            deletedAt: deletedAt,
            syncState: syncState,
            payloadJson: payloadJson,
            rowid: rowid,
          ),
          createCompanionCallback: ({
            required String id,
            required String userId,
            Value<int> version = const Value.absent(),
            required String updatedAt,
            Value<String?> deletedAt = const Value.absent(),
            Value<String> syncState = const Value.absent(),
            required String payloadJson,
            Value<int> rowid = const Value.absent(),
          }) =>
              GoalsDriftTableCompanion.insert(
            id: id,
            userId: userId,
            version: version,
            updatedAt: updatedAt,
            deletedAt: deletedAt,
            syncState: syncState,
            payloadJson: payloadJson,
            rowid: rowid,
          ),
          withReferenceMapper: (p0) => p0
              .map((e) => (e.readTable(table), BaseReferences(db, table, e)))
              .toList(),
          prefetchHooksCallback: null,
        ));
}

typedef $$GoalsDriftTableTableProcessedTableManager = ProcessedTableManager<
    _$LifeOsDriftDatabase,
    $GoalsDriftTableTable,
    GoalsDriftTableData,
    $$GoalsDriftTableTableFilterComposer,
    $$GoalsDriftTableTableOrderingComposer,
    $$GoalsDriftTableTableAnnotationComposer,
    $$GoalsDriftTableTableCreateCompanionBuilder,
    $$GoalsDriftTableTableUpdateCompanionBuilder,
    (
      GoalsDriftTableData,
      BaseReferences<_$LifeOsDriftDatabase, $GoalsDriftTableTable,
          GoalsDriftTableData>
    ),
    GoalsDriftTableData,
    PrefetchHooks Function()>;
typedef $$MilestonesDriftTableTableCreateCompanionBuilder
    = MilestonesDriftTableCompanion Function({
  required String id,
  required String userId,
  Value<int> version,
  required String updatedAt,
  Value<String?> deletedAt,
  Value<String> syncState,
  required String payloadJson,
  Value<int> rowid,
});
typedef $$MilestonesDriftTableTableUpdateCompanionBuilder
    = MilestonesDriftTableCompanion Function({
  Value<String> id,
  Value<String> userId,
  Value<int> version,
  Value<String> updatedAt,
  Value<String?> deletedAt,
  Value<String> syncState,
  Value<String> payloadJson,
  Value<int> rowid,
});

class $$MilestonesDriftTableTableFilterComposer
    extends Composer<_$LifeOsDriftDatabase, $MilestonesDriftTableTable> {
  $$MilestonesDriftTableTableFilterComposer({
    required super.$db,
    required super.$table,
    super.joinBuilder,
    super.$addJoinBuilderToRootComposer,
    super.$removeJoinBuilderFromRootComposer,
  });
  ColumnFilters<String> get id => $composableBuilder(
      column: $table.id, builder: (column) => ColumnFilters(column));

  ColumnFilters<String> get userId => $composableBuilder(
      column: $table.userId, builder: (column) => ColumnFilters(column));

  ColumnFilters<int> get version => $composableBuilder(
      column: $table.version, builder: (column) => ColumnFilters(column));

  ColumnFilters<String> get updatedAt => $composableBuilder(
      column: $table.updatedAt, builder: (column) => ColumnFilters(column));

  ColumnFilters<String> get deletedAt => $composableBuilder(
      column: $table.deletedAt, builder: (column) => ColumnFilters(column));

  ColumnFilters<String> get syncState => $composableBuilder(
      column: $table.syncState, builder: (column) => ColumnFilters(column));

  ColumnFilters<String> get payloadJson => $composableBuilder(
      column: $table.payloadJson, builder: (column) => ColumnFilters(column));
}

class $$MilestonesDriftTableTableOrderingComposer
    extends Composer<_$LifeOsDriftDatabase, $MilestonesDriftTableTable> {
  $$MilestonesDriftTableTableOrderingComposer({
    required super.$db,
    required super.$table,
    super.joinBuilder,
    super.$addJoinBuilderToRootComposer,
    super.$removeJoinBuilderFromRootComposer,
  });
  ColumnOrderings<String> get id => $composableBuilder(
      column: $table.id, builder: (column) => ColumnOrderings(column));

  ColumnOrderings<String> get userId => $composableBuilder(
      column: $table.userId, builder: (column) => ColumnOrderings(column));

  ColumnOrderings<int> get version => $composableBuilder(
      column: $table.version, builder: (column) => ColumnOrderings(column));

  ColumnOrderings<String> get updatedAt => $composableBuilder(
      column: $table.updatedAt, builder: (column) => ColumnOrderings(column));

  ColumnOrderings<String> get deletedAt => $composableBuilder(
      column: $table.deletedAt, builder: (column) => ColumnOrderings(column));

  ColumnOrderings<String> get syncState => $composableBuilder(
      column: $table.syncState, builder: (column) => ColumnOrderings(column));

  ColumnOrderings<String> get payloadJson => $composableBuilder(
      column: $table.payloadJson, builder: (column) => ColumnOrderings(column));
}

class $$MilestonesDriftTableTableAnnotationComposer
    extends Composer<_$LifeOsDriftDatabase, $MilestonesDriftTableTable> {
  $$MilestonesDriftTableTableAnnotationComposer({
    required super.$db,
    required super.$table,
    super.joinBuilder,
    super.$addJoinBuilderToRootComposer,
    super.$removeJoinBuilderFromRootComposer,
  });
  GeneratedColumn<String> get id =>
      $composableBuilder(column: $table.id, builder: (column) => column);

  GeneratedColumn<String> get userId =>
      $composableBuilder(column: $table.userId, builder: (column) => column);

  GeneratedColumn<int> get version =>
      $composableBuilder(column: $table.version, builder: (column) => column);

  GeneratedColumn<String> get updatedAt =>
      $composableBuilder(column: $table.updatedAt, builder: (column) => column);

  GeneratedColumn<String> get deletedAt =>
      $composableBuilder(column: $table.deletedAt, builder: (column) => column);

  GeneratedColumn<String> get syncState =>
      $composableBuilder(column: $table.syncState, builder: (column) => column);

  GeneratedColumn<String> get payloadJson => $composableBuilder(
      column: $table.payloadJson, builder: (column) => column);
}

class $$MilestonesDriftTableTableTableManager extends RootTableManager<
    _$LifeOsDriftDatabase,
    $MilestonesDriftTableTable,
    MilestonesDriftTableData,
    $$MilestonesDriftTableTableFilterComposer,
    $$MilestonesDriftTableTableOrderingComposer,
    $$MilestonesDriftTableTableAnnotationComposer,
    $$MilestonesDriftTableTableCreateCompanionBuilder,
    $$MilestonesDriftTableTableUpdateCompanionBuilder,
    (
      MilestonesDriftTableData,
      BaseReferences<_$LifeOsDriftDatabase, $MilestonesDriftTableTable,
          MilestonesDriftTableData>
    ),
    MilestonesDriftTableData,
    PrefetchHooks Function()> {
  $$MilestonesDriftTableTableTableManager(
      _$LifeOsDriftDatabase db, $MilestonesDriftTableTable table)
      : super(TableManagerState(
          db: db,
          table: table,
          createFilteringComposer: () =>
              $$MilestonesDriftTableTableFilterComposer($db: db, $table: table),
          createOrderingComposer: () =>
              $$MilestonesDriftTableTableOrderingComposer(
                  $db: db, $table: table),
          createComputedFieldComposer: () =>
              $$MilestonesDriftTableTableAnnotationComposer(
                  $db: db, $table: table),
          updateCompanionCallback: ({
            Value<String> id = const Value.absent(),
            Value<String> userId = const Value.absent(),
            Value<int> version = const Value.absent(),
            Value<String> updatedAt = const Value.absent(),
            Value<String?> deletedAt = const Value.absent(),
            Value<String> syncState = const Value.absent(),
            Value<String> payloadJson = const Value.absent(),
            Value<int> rowid = const Value.absent(),
          }) =>
              MilestonesDriftTableCompanion(
            id: id,
            userId: userId,
            version: version,
            updatedAt: updatedAt,
            deletedAt: deletedAt,
            syncState: syncState,
            payloadJson: payloadJson,
            rowid: rowid,
          ),
          createCompanionCallback: ({
            required String id,
            required String userId,
            Value<int> version = const Value.absent(),
            required String updatedAt,
            Value<String?> deletedAt = const Value.absent(),
            Value<String> syncState = const Value.absent(),
            required String payloadJson,
            Value<int> rowid = const Value.absent(),
          }) =>
              MilestonesDriftTableCompanion.insert(
            id: id,
            userId: userId,
            version: version,
            updatedAt: updatedAt,
            deletedAt: deletedAt,
            syncState: syncState,
            payloadJson: payloadJson,
            rowid: rowid,
          ),
          withReferenceMapper: (p0) => p0
              .map((e) => (e.readTable(table), BaseReferences(db, table, e)))
              .toList(),
          prefetchHooksCallback: null,
        ));
}

typedef $$MilestonesDriftTableTableProcessedTableManager
    = ProcessedTableManager<
        _$LifeOsDriftDatabase,
        $MilestonesDriftTableTable,
        MilestonesDriftTableData,
        $$MilestonesDriftTableTableFilterComposer,
        $$MilestonesDriftTableTableOrderingComposer,
        $$MilestonesDriftTableTableAnnotationComposer,
        $$MilestonesDriftTableTableCreateCompanionBuilder,
        $$MilestonesDriftTableTableUpdateCompanionBuilder,
        (
          MilestonesDriftTableData,
          BaseReferences<_$LifeOsDriftDatabase, $MilestonesDriftTableTable,
              MilestonesDriftTableData>
        ),
        MilestonesDriftTableData,
        PrefetchHooks Function()>;
typedef $$ReviewsDriftTableTableCreateCompanionBuilder
    = ReviewsDriftTableCompanion Function({
  required String id,
  required String userId,
  Value<int> version,
  required String updatedAt,
  Value<String?> deletedAt,
  Value<String> syncState,
  required String payloadJson,
  Value<int> rowid,
});
typedef $$ReviewsDriftTableTableUpdateCompanionBuilder
    = ReviewsDriftTableCompanion Function({
  Value<String> id,
  Value<String> userId,
  Value<int> version,
  Value<String> updatedAt,
  Value<String?> deletedAt,
  Value<String> syncState,
  Value<String> payloadJson,
  Value<int> rowid,
});

class $$ReviewsDriftTableTableFilterComposer
    extends Composer<_$LifeOsDriftDatabase, $ReviewsDriftTableTable> {
  $$ReviewsDriftTableTableFilterComposer({
    required super.$db,
    required super.$table,
    super.joinBuilder,
    super.$addJoinBuilderToRootComposer,
    super.$removeJoinBuilderFromRootComposer,
  });
  ColumnFilters<String> get id => $composableBuilder(
      column: $table.id, builder: (column) => ColumnFilters(column));

  ColumnFilters<String> get userId => $composableBuilder(
      column: $table.userId, builder: (column) => ColumnFilters(column));

  ColumnFilters<int> get version => $composableBuilder(
      column: $table.version, builder: (column) => ColumnFilters(column));

  ColumnFilters<String> get updatedAt => $composableBuilder(
      column: $table.updatedAt, builder: (column) => ColumnFilters(column));

  ColumnFilters<String> get deletedAt => $composableBuilder(
      column: $table.deletedAt, builder: (column) => ColumnFilters(column));

  ColumnFilters<String> get syncState => $composableBuilder(
      column: $table.syncState, builder: (column) => ColumnFilters(column));

  ColumnFilters<String> get payloadJson => $composableBuilder(
      column: $table.payloadJson, builder: (column) => ColumnFilters(column));
}

class $$ReviewsDriftTableTableOrderingComposer
    extends Composer<_$LifeOsDriftDatabase, $ReviewsDriftTableTable> {
  $$ReviewsDriftTableTableOrderingComposer({
    required super.$db,
    required super.$table,
    super.joinBuilder,
    super.$addJoinBuilderToRootComposer,
    super.$removeJoinBuilderFromRootComposer,
  });
  ColumnOrderings<String> get id => $composableBuilder(
      column: $table.id, builder: (column) => ColumnOrderings(column));

  ColumnOrderings<String> get userId => $composableBuilder(
      column: $table.userId, builder: (column) => ColumnOrderings(column));

  ColumnOrderings<int> get version => $composableBuilder(
      column: $table.version, builder: (column) => ColumnOrderings(column));

  ColumnOrderings<String> get updatedAt => $composableBuilder(
      column: $table.updatedAt, builder: (column) => ColumnOrderings(column));

  ColumnOrderings<String> get deletedAt => $composableBuilder(
      column: $table.deletedAt, builder: (column) => ColumnOrderings(column));

  ColumnOrderings<String> get syncState => $composableBuilder(
      column: $table.syncState, builder: (column) => ColumnOrderings(column));

  ColumnOrderings<String> get payloadJson => $composableBuilder(
      column: $table.payloadJson, builder: (column) => ColumnOrderings(column));
}

class $$ReviewsDriftTableTableAnnotationComposer
    extends Composer<_$LifeOsDriftDatabase, $ReviewsDriftTableTable> {
  $$ReviewsDriftTableTableAnnotationComposer({
    required super.$db,
    required super.$table,
    super.joinBuilder,
    super.$addJoinBuilderToRootComposer,
    super.$removeJoinBuilderFromRootComposer,
  });
  GeneratedColumn<String> get id =>
      $composableBuilder(column: $table.id, builder: (column) => column);

  GeneratedColumn<String> get userId =>
      $composableBuilder(column: $table.userId, builder: (column) => column);

  GeneratedColumn<int> get version =>
      $composableBuilder(column: $table.version, builder: (column) => column);

  GeneratedColumn<String> get updatedAt =>
      $composableBuilder(column: $table.updatedAt, builder: (column) => column);

  GeneratedColumn<String> get deletedAt =>
      $composableBuilder(column: $table.deletedAt, builder: (column) => column);

  GeneratedColumn<String> get syncState =>
      $composableBuilder(column: $table.syncState, builder: (column) => column);

  GeneratedColumn<String> get payloadJson => $composableBuilder(
      column: $table.payloadJson, builder: (column) => column);
}

class $$ReviewsDriftTableTableTableManager extends RootTableManager<
    _$LifeOsDriftDatabase,
    $ReviewsDriftTableTable,
    ReviewsDriftTableData,
    $$ReviewsDriftTableTableFilterComposer,
    $$ReviewsDriftTableTableOrderingComposer,
    $$ReviewsDriftTableTableAnnotationComposer,
    $$ReviewsDriftTableTableCreateCompanionBuilder,
    $$ReviewsDriftTableTableUpdateCompanionBuilder,
    (
      ReviewsDriftTableData,
      BaseReferences<_$LifeOsDriftDatabase, $ReviewsDriftTableTable,
          ReviewsDriftTableData>
    ),
    ReviewsDriftTableData,
    PrefetchHooks Function()> {
  $$ReviewsDriftTableTableTableManager(
      _$LifeOsDriftDatabase db, $ReviewsDriftTableTable table)
      : super(TableManagerState(
          db: db,
          table: table,
          createFilteringComposer: () =>
              $$ReviewsDriftTableTableFilterComposer($db: db, $table: table),
          createOrderingComposer: () =>
              $$ReviewsDriftTableTableOrderingComposer($db: db, $table: table),
          createComputedFieldComposer: () =>
              $$ReviewsDriftTableTableAnnotationComposer(
                  $db: db, $table: table),
          updateCompanionCallback: ({
            Value<String> id = const Value.absent(),
            Value<String> userId = const Value.absent(),
            Value<int> version = const Value.absent(),
            Value<String> updatedAt = const Value.absent(),
            Value<String?> deletedAt = const Value.absent(),
            Value<String> syncState = const Value.absent(),
            Value<String> payloadJson = const Value.absent(),
            Value<int> rowid = const Value.absent(),
          }) =>
              ReviewsDriftTableCompanion(
            id: id,
            userId: userId,
            version: version,
            updatedAt: updatedAt,
            deletedAt: deletedAt,
            syncState: syncState,
            payloadJson: payloadJson,
            rowid: rowid,
          ),
          createCompanionCallback: ({
            required String id,
            required String userId,
            Value<int> version = const Value.absent(),
            required String updatedAt,
            Value<String?> deletedAt = const Value.absent(),
            Value<String> syncState = const Value.absent(),
            required String payloadJson,
            Value<int> rowid = const Value.absent(),
          }) =>
              ReviewsDriftTableCompanion.insert(
            id: id,
            userId: userId,
            version: version,
            updatedAt: updatedAt,
            deletedAt: deletedAt,
            syncState: syncState,
            payloadJson: payloadJson,
            rowid: rowid,
          ),
          withReferenceMapper: (p0) => p0
              .map((e) => (e.readTable(table), BaseReferences(db, table, e)))
              .toList(),
          prefetchHooksCallback: null,
        ));
}

typedef $$ReviewsDriftTableTableProcessedTableManager = ProcessedTableManager<
    _$LifeOsDriftDatabase,
    $ReviewsDriftTableTable,
    ReviewsDriftTableData,
    $$ReviewsDriftTableTableFilterComposer,
    $$ReviewsDriftTableTableOrderingComposer,
    $$ReviewsDriftTableTableAnnotationComposer,
    $$ReviewsDriftTableTableCreateCompanionBuilder,
    $$ReviewsDriftTableTableUpdateCompanionBuilder,
    (
      ReviewsDriftTableData,
      BaseReferences<_$LifeOsDriftDatabase, $ReviewsDriftTableTable,
          ReviewsDriftTableData>
    ),
    ReviewsDriftTableData,
    PrefetchHooks Function()>;

class $LifeOsDriftDatabaseManager {
  final _$LifeOsDriftDatabase _db;
  $LifeOsDriftDatabaseManager(this._db);
  $$SyncStateDriftTableTableTableManager get syncStateDriftTable =>
      $$SyncStateDriftTableTableTableManager(_db, _db.syncStateDriftTable);
  $$OutboxDriftTableTableTableManager get outboxDriftTable =>
      $$OutboxDriftTableTableTableManager(_db, _db.outboxDriftTable);
  $$EntityBaseSnapshotDriftTableTableTableManager
      get entityBaseSnapshotDriftTable =>
          $$EntityBaseSnapshotDriftTableTableTableManager(
              _db, _db.entityBaseSnapshotDriftTable);
  $$SyncLogDriftTableTableTableManager get syncLogDriftTable =>
      $$SyncLogDriftTableTableTableManager(_db, _db.syncLogDriftTable);
  $$ScheduledNotificationDriftTableTableTableManager
      get scheduledNotificationDriftTable =>
          $$ScheduledNotificationDriftTableTableTableManager(
              _db, _db.scheduledNotificationDriftTable);
  $$DeliveredNotificationDriftTableTableTableManager
      get deliveredNotificationDriftTable =>
          $$DeliveredNotificationDriftTableTableTableManager(
              _db, _db.deliveredNotificationDriftTable);
  $$UserSettingsDriftTableTableTableManager get userSettingsDriftTable =>
      $$UserSettingsDriftTableTableTableManager(
          _db, _db.userSettingsDriftTable);
  $$CategoriesDriftTableTableTableManager get categoriesDriftTable =>
      $$CategoriesDriftTableTableTableManager(_db, _db.categoriesDriftTable);
  $$ProjectsDriftTableTableTableManager get projectsDriftTable =>
      $$ProjectsDriftTableTableTableManager(_db, _db.projectsDriftTable);
  $$TasksDriftTableTableTableManager get tasksDriftTable =>
      $$TasksDriftTableTableTableManager(_db, _db.tasksDriftTable);
  $$HabitsDriftTableTableTableManager get habitsDriftTable =>
      $$HabitsDriftTableTableTableManager(_db, _db.habitsDriftTable);
  $$HabitCheckinsDriftTableTableTableManager get habitCheckinsDriftTable =>
      $$HabitCheckinsDriftTableTableTableManager(
          _db, _db.habitCheckinsDriftTable);
  $$RoutinesDriftTableTableTableManager get routinesDriftTable =>
      $$RoutinesDriftTableTableTableManager(_db, _db.routinesDriftTable);
  $$RoutineStepsDriftTableTableTableManager get routineStepsDriftTable =>
      $$RoutineStepsDriftTableTableTableManager(
          _db, _db.routineStepsDriftTable);
  $$RoutineLogsDriftTableTableTableManager get routineLogsDriftTable =>
      $$RoutineLogsDriftTableTableTableManager(_db, _db.routineLogsDriftTable);
  $$RoutineStepLogsDriftTableTableTableManager get routineStepLogsDriftTable =>
      $$RoutineStepLogsDriftTableTableTableManager(
          _db, _db.routineStepLogsDriftTable);
  $$FinancialAccountsDriftTableTableTableManager
      get financialAccountsDriftTable =>
          $$FinancialAccountsDriftTableTableTableManager(
              _db, _db.financialAccountsDriftTable);
  $$FinancialCategoriesDriftTableTableTableManager
      get financialCategoriesDriftTable =>
          $$FinancialCategoriesDriftTableTableTableManager(
              _db, _db.financialCategoriesDriftTable);
  $$FinancialTransactionsDriftTableTableTableManager
      get financialTransactionsDriftTable =>
          $$FinancialTransactionsDriftTableTableTableManager(
              _db, _db.financialTransactionsDriftTable);
  $$AccountTransfersDriftTableTableTableManager
      get accountTransfersDriftTable =>
          $$AccountTransfersDriftTableTableTableManager(
              _db, _db.accountTransfersDriftTable);
  $$BudgetsDriftTableTableTableManager get budgetsDriftTable =>
      $$BudgetsDriftTableTableTableManager(_db, _db.budgetsDriftTable);
  $$RecurringTransactionsDriftTableTableTableManager
      get recurringTransactionsDriftTable =>
          $$RecurringTransactionsDriftTableTableTableManager(
              _db, _db.recurringTransactionsDriftTable);
  $$SavingGoalsDriftTableTableTableManager get savingGoalsDriftTable =>
      $$SavingGoalsDriftTableTableTableManager(_db, _db.savingGoalsDriftTable);
  $$SavingContributionsDriftTableTableTableManager
      get savingContributionsDriftTable =>
          $$SavingContributionsDriftTableTableTableManager(
              _db, _db.savingContributionsDriftTable);
  $$CalendarEventsDriftTableTableTableManager get calendarEventsDriftTable =>
      $$CalendarEventsDriftTableTableTableManager(
          _db, _db.calendarEventsDriftTable);
  $$EventExceptionsDriftTableTableTableManager get eventExceptionsDriftTable =>
      $$EventExceptionsDriftTableTableTableManager(
          _db, _db.eventExceptionsDriftTable);
  $$TimeBlocksDriftTableTableTableManager get timeBlocksDriftTable =>
      $$TimeBlocksDriftTableTableTableManager(_db, _db.timeBlocksDriftTable);
  $$RemindersDriftTableTableTableManager get remindersDriftTable =>
      $$RemindersDriftTableTableTableManager(_db, _db.remindersDriftTable);
  $$NotesDriftTableTableTableManager get notesDriftTable =>
      $$NotesDriftTableTableTableManager(_db, _db.notesDriftTable);
  $$TagsDriftTableTableTableManager get tagsDriftTable =>
      $$TagsDriftTableTableTableManager(_db, _db.tagsDriftTable);
  $$NoteTagsDriftTableTableTableManager get noteTagsDriftTable =>
      $$NoteTagsDriftTableTableTableManager(_db, _db.noteTagsDriftTable);
  $$NoteLinksDriftTableTableTableManager get noteLinksDriftTable =>
      $$NoteLinksDriftTableTableTableManager(_db, _db.noteLinksDriftTable);
  $$GoalsDriftTableTableTableManager get goalsDriftTable =>
      $$GoalsDriftTableTableTableManager(_db, _db.goalsDriftTable);
  $$MilestonesDriftTableTableTableManager get milestonesDriftTable =>
      $$MilestonesDriftTableTableTableManager(_db, _db.milestonesDriftTable);
  $$ReviewsDriftTableTableTableManager get reviewsDriftTable =>
      $$ReviewsDriftTableTableTableManager(_db, _db.reviewsDriftTable);
}
