import 'package:drift/drift.dart';

/// Bảng trạng thái đồng bộ của người dùng (`sync_state`).
class SyncStateDriftTable extends Table {
  @override
  String get tableName => 'sync_state';

  TextColumn get userId => text().named('user_id')();
  IntColumn get lastCursor =>
      integer().named('last_cursor').withDefault(const Constant(0))();
  TextColumn get lastSyncAt => text().named('last_sync_at').nullable()();
  IntColumn get syncPaused =>
      integer().named('sync_paused').withDefault(const Constant(0))();
  TextColumn get deviceId => text().named('device_id').nullable()();

  @override
  Set<Column<Object>> get primaryKey => <Column<Object>>{userId};
}

/// Hàng đợi thay đổi cục bộ (`outbox`).
class OutboxDriftTable extends Table {
  @override
  String get tableName => 'outbox';

  TextColumn get mutationId => text().named('mutation_id')();
  TextColumn get userId => text().named('user_id')();
  TextColumn get entityType => text().named('entity_type')();
  TextColumn get entityId => text().named('entity_id')();
  TextColumn get op => text()();
  IntColumn get baseVersion =>
      integer().named('base_version').withDefault(const Constant(0))();
  TextColumn get payloadJson => text().named('payload_json')();
  TextColumn get status => text().withDefault(const Constant('PENDING'))();
  IntColumn get retryCount =>
      integer().named('retry_count').withDefault(const Constant(0))();
  TextColumn get lastErrorCode => text().named('last_error_code').nullable()();
  TextColumn get lastErrorMessage =>
      text().named('last_error_message').nullable()();
  TextColumn get serverConflictJson =>
      text().named('server_conflict_json').nullable()();
  TextColumn get createdAt => text().named('created_at')();
  TextColumn get updatedAt => text().named('updated_at')();

  @override
  Set<Column<Object>> get primaryKey => <Column<Object>>{mutationId};
}

/// Bản chụp gốc trước khi sửa offline phục vụ hợp nhất 3 chiều (`entity_base_snapshots` — `D159`).
class EntityBaseSnapshotDriftTable extends Table {
  @override
  String get tableName => 'entity_base_snapshots';

  TextColumn get userId => text().named('user_id')();
  TextColumn get entityType => text().named('entity_type')();
  TextColumn get entityId => text().named('entity_id')();
  IntColumn get baseVersion => integer().named('base_version')();
  TextColumn get snapshotJson => text().named('snapshot_json')();
  TextColumn get updatedAt => text().named('updated_at')();

  @override
  Set<Column<Object>> get primaryKey =>
      <Column<Object>>{userId, entityType, entityId};
}

/// Nhật ký đồng bộ vòng đệm 500 dòng (`sync_log` — `D165`).
class SyncLogDriftTable extends Table {
  @override
  String get tableName => 'sync_log';

  IntColumn get id => integer().autoIncrement()();
  TextColumn get userId => text().named('user_id')();
  TextColumn get direction => text()();
  TextColumn get outcome => text()();
  TextColumn get entityType => text().named('entity_type').nullable()();
  TextColumn get entityId => text().named('entity_id').nullable()();
  TextColumn get mutationId => text().named('mutation_id').nullable()();
  IntColumn get latencyMs => integer().named('latency_ms').nullable()();
  TextColumn get errorCode => text().named('error_code').nullable()();
  TextColumn get details => text().nullable()();
  TextColumn get createdAt => text().named('created_at')();
}

/// Bảng lịch nhắc cục bộ giới hạn 64 thông báo gần nhất cho iOS (`scheduled_notifications` — `D133`).
class ScheduledNotificationDriftTable extends Table {
  @override
  String get tableName => 'scheduled_notifications';

  TextColumn get dedupeKey => text().named('dedupe_key')();
  TextColumn get userId => text().named('user_id')();
  TextColumn get targetType => text().named('target_type')();
  TextColumn get targetId => text().named('target_id')();
  TextColumn get fireAtUtc => text().named('fire_at_utc')();
  TextColumn get title => text()();
  TextColumn get body => text()();
  IntColumn get redactedLockScreen =>
      integer().named('redacted_lock_screen').withDefault(const Constant(1))();

  @override
  Set<Column<Object>> get primaryKey => <Column<Object>>{dedupeKey};
}

/// Bảng khử trùng thông báo giữa Local Notification và FCM Push (`delivered_notifications` — `D132`).
class DeliveredNotificationDriftTable extends Table {
  @override
  String get tableName => 'delivered_notifications';

  TextColumn get dedupeKey => text().named('dedupe_key')();
  TextColumn get userId => text().named('user_id')();
  TextColumn get shownAt => text().named('shown_at')();

  @override
  Set<Column<Object>> get primaryKey => <Column<Object>>{dedupeKey};
}

/// Cấu trúc cột chuẩn cho toàn bộ **29 bảng thực thể đồng bộ** (`MOBILE_MASTER_PLAN.md` §2.3).
abstract class SyncEntityDriftTableBase extends Table {
  TextColumn get id => text()();
  TextColumn get userId => text().named('user_id')();
  IntColumn get version => integer().withDefault(const Constant(0))();
  TextColumn get updatedAt => text().named('updated_at')();
  TextColumn get deletedAt => text().named('deleted_at').nullable()();
  TextColumn get syncState =>
      text().named('sync_state').withDefault(const Constant('synced'))();
  TextColumn get payloadJson => text().named('payload_json')();

  @override
  Set<Column<Object>> get primaryKey => <Column<Object>>{userId, id};
}

// 1. user_settings
class UserSettingsDriftTable extends SyncEntityDriftTableBase {
  @override
  String get tableName => 'user_settings';
}

// 2. categories
class CategoriesDriftTable extends SyncEntityDriftTableBase {
  @override
  String get tableName => 'categories';
}

// 3. projects
class ProjectsDriftTable extends SyncEntityDriftTableBase {
  @override
  String get tableName => 'projects';
}

// 4. tasks
class TasksDriftTable extends SyncEntityDriftTableBase {
  @override
  String get tableName => 'tasks';
}

// 5. habits
class HabitsDriftTable extends SyncEntityDriftTableBase {
  @override
  String get tableName => 'habits';
}

// 6. habit_checkins
class HabitCheckinsDriftTable extends SyncEntityDriftTableBase {
  @override
  String get tableName => 'habit_checkins';
}

// 7. routines
class RoutinesDriftTable extends SyncEntityDriftTableBase {
  @override
  String get tableName => 'routines';
}

// 8. routine_steps
class RoutineStepsDriftTable extends SyncEntityDriftTableBase {
  @override
  String get tableName => 'routine_steps';
}

// 9. routine_logs
class RoutineLogsDriftTable extends SyncEntityDriftTableBase {
  @override
  String get tableName => 'routine_logs';
}

// 10. routine_step_logs
class RoutineStepLogsDriftTable extends SyncEntityDriftTableBase {
  @override
  String get tableName => 'routine_step_logs';
}

// 11. financial_accounts
class FinancialAccountsDriftTable extends SyncEntityDriftTableBase {
  @override
  String get tableName => 'financial_accounts';
}

// 12. financial_categories
class FinancialCategoriesDriftTable extends SyncEntityDriftTableBase {
  @override
  String get tableName => 'financial_categories';
}

// 13. financial_transactions
class FinancialTransactionsDriftTable extends SyncEntityDriftTableBase {
  @override
  String get tableName => 'financial_transactions';
}

// 14. account_transfers
class AccountTransfersDriftTable extends SyncEntityDriftTableBase {
  @override
  String get tableName => 'account_transfers';
}

// 15. budgets
class BudgetsDriftTable extends SyncEntityDriftTableBase {
  @override
  String get tableName => 'budgets';
}

// 16. recurring_transactions
class RecurringTransactionsDriftTable extends SyncEntityDriftTableBase {
  @override
  String get tableName => 'recurring_transactions';
}

// 17. saving_goals
class SavingGoalsDriftTable extends SyncEntityDriftTableBase {
  @override
  String get tableName => 'saving_goals';
}

// 18. saving_contributions
class SavingContributionsDriftTable extends SyncEntityDriftTableBase {
  @override
  String get tableName => 'saving_contributions';
}

// 19. calendar_events
class CalendarEventsDriftTable extends SyncEntityDriftTableBase {
  @override
  String get tableName => 'calendar_events';
}

// 20. event_exceptions
class EventExceptionsDriftTable extends SyncEntityDriftTableBase {
  @override
  String get tableName => 'event_exceptions';
}

// 21. time_blocks
class TimeBlocksDriftTable extends SyncEntityDriftTableBase {
  @override
  String get tableName => 'time_blocks';
}

// 22. reminders
class RemindersDriftTable extends SyncEntityDriftTableBase {
  @override
  String get tableName => 'reminders';
}

// 23. notes
class NotesDriftTable extends SyncEntityDriftTableBase {
  @override
  String get tableName => 'notes';
}

// 24. tags
class TagsDriftTable extends SyncEntityDriftTableBase {
  @override
  String get tableName => 'tags';
}

// 25. note_tags
class NoteTagsDriftTable extends SyncEntityDriftTableBase {
  @override
  String get tableName => 'note_tags';
}

// 26. note_links
class NoteLinksDriftTable extends SyncEntityDriftTableBase {
  @override
  String get tableName => 'note_links';
}

// 27. goals
class GoalsDriftTable extends SyncEntityDriftTableBase {
  @override
  String get tableName => 'goals';
}

// 28. milestones
class MilestonesDriftTable extends SyncEntityDriftTableBase {
  @override
  String get tableName => 'milestones';
}

// 29. reviews
class ReviewsDriftTable extends SyncEntityDriftTableBase {
  @override
  String get tableName => 'reviews';
}

