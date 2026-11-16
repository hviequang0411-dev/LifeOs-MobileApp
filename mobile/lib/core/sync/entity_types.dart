/// Chiến lược giải quyết xung đột theo từng loại thực thể (`SYNC_RULES.md` §9.5 & `MOBILE_MASTER_PLAN.md` §2.3).
enum ConflictStrategy {
  /// Hợp nhất 3 bên theo từng trường (`baseSnapshot` vs `serverCurrent` vs `clientLocal`).
  field3Way,

  /// Yêu cầu người dùng xác nhận tường minh khi lệch số tiền/tài khoản (D105 — không âm thầm ghi đè tài chính).
  financeStrict,

  /// Hỗ trợ so sánh cạnh nhau / lưu thành bản sao nếu cả 2 bên cùng sửa `content` (`D183`, `D202`).
  noteConflictCopyCapable,

  /// Khóa phức hợp tự nhiên (`habit_checkin`, `routine_log`, `routine_step_log`, `event_exception`).
  compositeKeyUpsert,

  /// Bảng nối nhiều-nhiều (`note_tag`, `note_link`): tập hợp hợp nhất idempotent (`D184`, `D189`).
  joinTableSet,
}

/// Danh mục chính tắc **29 `entityType` đồng bộ** của LifeOS
/// (khớp 100% với `BACKEND_FINAL_REPORT.md` §3, `SYNC_PROTOCOL.md` §3.1–§3.9 và `MOBILE_MASTER_PLAN.md` §2.3).
enum SyncEntityType {
  // Phase 1 — Identity & Task Management (4)
  userSettings(
    wireName: 'user_settings',
    tableName: 'user_settings',
    conflictStrategy: ConflictStrategy.field3Way,
  ),
  category(
    wireName: 'category',
    tableName: 'categories',
    conflictStrategy: ConflictStrategy.field3Way,
  ),
  project(
    wireName: 'project',
    tableName: 'projects',
    conflictStrategy: ConflictStrategy.field3Way,
  ),
  task(
    wireName: 'task',
    tableName: 'tasks',
    conflictStrategy: ConflictStrategy.field3Way,
  ),

  // Phase 2 — Habit & Routine (6)
  habit(
    wireName: 'habit',
    tableName: 'habits',
    conflictStrategy: ConflictStrategy.field3Way,
  ),
  habitEntry(
    wireName: 'habit_checkin',
    tableName: 'habit_checkins',
    conflictStrategy: ConflictStrategy.compositeKeyUpsert,
  ),
  routine(
    wireName: 'routine',
    tableName: 'routines',
    conflictStrategy: ConflictStrategy.field3Way,
  ),
  routineStep(
    wireName: 'routine_step',
    tableName: 'routine_steps',
    conflictStrategy: ConflictStrategy.field3Way,
  ),
  routineSession(
    wireName: 'routine_log',
    tableName: 'routine_logs',
    conflictStrategy: ConflictStrategy.compositeKeyUpsert,
  ),
  routineStepLog(
    wireName: 'routine_step_log',
    tableName: 'routine_step_logs',
    conflictStrategy: ConflictStrategy.compositeKeyUpsert,
  ),

  // Phase 3 — Personal Finance (8)
  financeAccount(
    wireName: 'financial_account',
    tableName: 'financial_accounts',
    conflictStrategy: ConflictStrategy.financeStrict,
  ),
  financeCategory(
    wireName: 'financial_category',
    tableName: 'financial_categories',
    conflictStrategy: ConflictStrategy.financeStrict,
  ),
  financeTransaction(
    wireName: 'financial_transaction',
    tableName: 'financial_transactions',
    conflictStrategy: ConflictStrategy.financeStrict,
  ),
  accountTransfer(
    wireName: 'account_transfer',
    tableName: 'account_transfers',
    conflictStrategy: ConflictStrategy.financeStrict,
  ),
  budget(
    wireName: 'budget',
    tableName: 'budgets',
    conflictStrategy: ConflictStrategy.financeStrict,
  ),
  recurringTransaction(
    wireName: 'recurring_transaction',
    tableName: 'recurring_transactions',
    conflictStrategy: ConflictStrategy.financeStrict,
  ),
  savingGoal(
    wireName: 'saving_goal',
    tableName: 'saving_goals',
    conflictStrategy: ConflictStrategy.financeStrict,
  ),
  savingContribution(
    wireName: 'saving_contribution',
    tableName: 'saving_contributions',
    conflictStrategy: ConflictStrategy.financeStrict,
  ),

  // Phase 4 — Calendar & Notification (4)
  calendarEvent(
    wireName: 'calendar_event',
    tableName: 'calendar_events',
    conflictStrategy: ConflictStrategy.field3Way,
  ),
  calendarEventException(
    wireName: 'event_exception',
    tableName: 'event_exceptions',
    conflictStrategy: ConflictStrategy.compositeKeyUpsert,
  ),
  timeBlock(
    wireName: 'time_block',
    tableName: 'time_blocks',
    conflictStrategy: ConflictStrategy.field3Way,
  ),
  reminder(
    wireName: 'reminder',
    tableName: 'reminders',
    conflictStrategy: ConflictStrategy.field3Way,
  ),

  // Phase 6 — Knowledge & Goals (6)
  note(
    wireName: 'note',
    tableName: 'notes',
    conflictStrategy: ConflictStrategy.noteConflictCopyCapable,
    maxPayloadBytes: 196608, // 192 KB UTF-8 cho Note (D180)
  ),
  tag(
    wireName: 'tag',
    tableName: 'tags',
    conflictStrategy: ConflictStrategy.field3Way,
  ),
  noteTag(
    wireName: 'note_tag',
    tableName: 'note_tags',
    conflictStrategy: ConflictStrategy.joinTableSet,
  ),
  noteLink(
    wireName: 'note_link',
    tableName: 'note_links',
    conflictStrategy: ConflictStrategy.joinTableSet,
  ),
  goal(
    wireName: 'goal',
    tableName: 'goals',
    conflictStrategy: ConflictStrategy.field3Way,
  ),
  goalMilestone(
    wireName: 'milestone',
    tableName: 'milestones',
    conflictStrategy: ConflictStrategy.field3Way,
  ),

  // Phase 7 — Life Intelligence & Review (1)
  lifeReview(
    wireName: 'review',
    tableName: 'reviews',
    conflictStrategy: ConflictStrategy.noteConflictCopyCapable,
  );

  const SyncEntityType({
    required this.wireName,
    required this.tableName,
    required this.conflictStrategy,
    this.maxPayloadBytes = 65536, // Mặc định 64 KB UTF-8
  });

  final String wireName;
  final String tableName;
  final ConflictStrategy conflictStrategy;
  final int maxPayloadBytes;

  static final Map<String, SyncEntityType> _byWireName = <String, SyncEntityType>{
    for (final SyncEntityType t in SyncEntityType.values) t.wireName: t,
    // Các bí danh tương thích cho dữ liệu bộ kiểm thử cục bộ
    'habit_entry': SyncEntityType.habitEntry,
    'routine_session': SyncEntityType.routineSession,
    'finance_account': SyncEntityType.financeAccount,
    'finance_category': SyncEntityType.financeCategory,
    'finance_transaction': SyncEntityType.financeTransaction,
    'calendar_event_exception': SyncEntityType.calendarEventException,
    'goal_milestone': SyncEntityType.goalMilestone,
    'goal_contribution': SyncEntityType.savingContribution,
    'life_review': SyncEntityType.lifeReview,
  };

  static SyncEntityType? tryFromWireName(String wireName) =>
      _byWireName[wireName];

  static SyncEntityType fromWireName(String wireName) {
    final SyncEntityType? found = _byWireName[wireName];
    if (found == null) {
      throw ArgumentError('Không hỗ trợ entityType: $wireName');
    }
    return found;
  }
}
