import 'package:uuid/uuid.dart';

import '../../../core/calculators/habit_calculator.dart';
import '../../../core/database/app_database.dart';
import '../../../core/money/decimal10k.dart';
import '../../../core/sync/entity_types.dart';
import '../../../core/time/user_day_clock.dart';
import '../../calendar/data/calendar_local_repository.dart';
import '../../finance/data/finance_local_repository.dart';
import '../../knowledge/data/knowledge_local_repository.dart';
import '../../notification/domain/local_notification_scheduler.dart';
import '../../tasks/data/task_local_repository.dart';

export '../../calendar/data/calendar_local_repository.dart';
export '../../finance/data/finance_local_repository.dart';
export '../../knowledge/data/knowledge_local_repository.dart';
export '../../notification/domain/local_notification_scheduler.dart';
export '../../tasks/data/task_local_repository.dart';

/// Facade kho lưu trữ cục bộ (Offline-First Repository) điều phối các repository theo từng miền
/// (`tasks`, `finance`, `calendar`, `knowledge`, `notification`).
///
/// Quy tắc bất biến (`FLUTTER_RULES.md`):
/// - Mọi truy vấn UI đều đọc từ `AppDatabase` (Drift/SQLite).
/// - Mọi thao tác tạo/sửa/xóa đều ghi vào bảng chính + `outbox` trong cùng 1 transaction.
/// - Không bao giờ gọi mạng (Dio) từ lớp này.
class LifeOsLocalRepository {
  LifeOsLocalRepository({
    required this.db,
    Uuid? uuid,
  })  : tasks = TaskAndHabitLocalRepository(db: db, uuid: uuid),
        finance = FinanceLocalRepository(db: db, uuid: uuid),
        calendar = CalendarLocalRepository(db: db, uuid: uuid),
        knowledge = KnowledgeAndGoalLocalRepository(db: db, uuid: uuid),
        notifications = LocalNotificationScheduler(db: db);

  final AppDatabase db;
  final TaskAndHabitLocalRepository tasks;
  final FinanceLocalRepository finance;
  final CalendarLocalRepository calendar;
  final KnowledgeAndGoalLocalRepository knowledge;
  final LocalNotificationScheduler notifications;

  // ===========================================================================
  // 1. CÀI ĐẶT NGƯỜI DÙNG (`user_settings` — `SYNC_PROTOCOL.md` §3.4 & `openapi.yaml` dòng 8175–8201)
  // ===========================================================================

  Map<String, dynamic> getUserSettings(String userId) {
    final List<LocalEntityRow> rows = db.listEntities(
      userId: userId,
      entityType: SyncEntityType.userSettings,
    );
    if (rows.isEmpty) {
      return <String, dynamic>{
        'id': userId,
        'timezone': 'Asia/Ho_Chi_Minh',
        'locale': 'vi',
        'defaultCurrency': 'VND',
        'weekStartDay': 1,
        'quietStart': null,
        'quietEnd': null,
        'showDetailsOnLockScreen': false,
      };
    }
    return rows.first.payload;
  }

  UserDayClock getUserDayClock(String userId) {
    final Map<String, dynamic> s = getUserSettings(userId);
    return UserDayClock.fromUserSettings(
      timezone: (s['timezone'] as String?) ?? 'Asia/Ho_Chi_Minh',
      weekStartDay: (s['weekStartDay'] as num?)?.toInt() ?? 1,
    );
  }

  void updateUserSettings({
    required String userId,
    required Map<String, dynamic> patch,
    DateTime? nowUtc,
  }) {
    final Map<String, dynamic> current = getUserSettings(userId);
    final String settingsId = (current['id'] as String?) ?? userId;
    final bool exists = db.getEntity(
          userId: userId,
          entityType: SyncEntityType.userSettings,
          entityId: settingsId,
        ) !=
        null;

    final Map<String, dynamic> merged = <String, dynamic>{
      ...current,
      ...patch,
      'id': settingsId,
    }..remove('dayStartHour');

    db.mutateLocalAndEnqueueOutbox(
      userId: userId,
      entityType: SyncEntityType.userSettings,
      entityId: settingsId,
      op: exists ? 'UPDATE' : 'CREATE',
      payload: merged,
      nowUtc: nowUtc,
    );
  }

  // ===========================================================================
  // 2. ỦY QUYỀN TASK, PROJECT, CATEGORY, HABIT & ROUTINE
  // ===========================================================================

  String createTask({
    required String userId,
    required String title,
    String? description,
    String priority = 'MEDIUM',
    String status = 'TODO',
    String? dueDate,
    String? dueTime,
    String? scheduledDate,
    bool isMyDay = false,
    String? projectId,
    String? categoryId,
    String? goalId,
    String? milestoneId,
    int? estimatedMinutes,
    DateTime? nowUtc,
  }) =>
      tasks.createTask(
        userId: userId,
        title: title,
        description: description,
        priority: priority,
        status: status,
        dueDate: dueDate,
        dueTime: dueTime,
        scheduledDate: scheduledDate,
        isMyDay: isMyDay,
        projectId: projectId,
        categoryId: categoryId,
        goalId: goalId,
        milestoneId: milestoneId,
        estimatedMinutes: estimatedMinutes,
        nowUtc: nowUtc,
      );

  void toggleTaskCompletion({
    required String userId,
    required String taskId,
    DateTime? nowUtc,
  }) =>
      tasks.toggleTaskCompletion(
        userId: userId,
        taskId: taskId,
        nowUtc: nowUtc,
      );

  void updateTask({
    required String userId,
    required String taskId,
    required Map<String, dynamic> patch,
    DateTime? nowUtc,
  }) =>
      tasks.updateTask(
        userId: userId,
        taskId: taskId,
        patch: patch,
        nowUtc: nowUtc,
      );

  void deleteTask({
    required String userId,
    required String taskId,
    DateTime? nowUtc,
  }) =>
      tasks.deleteTask(userId: userId, taskId: taskId, nowUtc: nowUtc);

  String createProject({
    required String userId,
    required String name,
    String? description,
    String color = '#1B6B50',
    String? categoryId,
    DateTime? nowUtc,
  }) =>
      tasks.createProject(
        userId: userId,
        name: name,
        description: description,
        color: color,
        categoryId: categoryId,
        nowUtc: nowUtc,
      );

  void deleteProject({
    required String userId,
    required String projectId,
    DateTime? nowUtc,
  }) =>
      tasks.deleteProject(
        userId: userId,
        projectId: projectId,
        nowUtc: nowUtc,
      );

  String createCategory({
    required String userId,
    required String name,
    String color = '#D97724',
    DateTime? nowUtc,
  }) =>
      tasks.createCategory(
        userId: userId,
        name: name,
        color: color,
        nowUtc: nowUtc,
      );

  List<LocalEntityRow> queryMyDayTasks({
    required String userId,
    required String todayIso,
  }) =>
      tasks.queryMyDayTasks(userId: userId, todayIso: todayIso);

  String createHabit({
    required String userId,
    required String name,
    String frequencyType = 'DAILY',
    String trackingType = 'BOOLEAN',
    int? targetCount,
    int? targetDurationSeconds,
    List<int> daysOfWeek = const <int>[],
    int? timesPerWeek,
    int? intervalDays,
    String? startOn,
    DateTime? nowUtc,
  }) =>
      tasks.createHabit(
        userId: userId,
        name: name,
        frequencyType: frequencyType,
        trackingType: trackingType,
        targetCount: targetCount,
        targetDurationSeconds: targetDurationSeconds,
        daysOfWeek: daysOfWeek,
        timesPerWeek: timesPerWeek,
        intervalDays: intervalDays,
        startOn: startOn,
        nowUtc: nowUtc,
      );

  String checkInHabit({
    required String userId,
    required String habitId,
    required String entryDateIso,
    String status = 'COMPLETED',
    String? note,
    int? value,
    DateTime? nowUtc,
  }) =>
      tasks.checkInHabit(
        userId: userId,
        habitId: habitId,
        entryDateIso: entryDateIso,
        status: status,
        note: note,
        value: value,
        nowUtc: nowUtc,
      );

  HabitStatsResult computeHabitStats({
    required String userId,
    required String habitId,
    required String asOfDateIso,
  }) =>
      tasks.computeHabitStats(
        userId: userId,
        habitId: habitId,
        asOfDateIso: asOfDateIso,
      );

  String createRoutine({
    required String userId,
    required String name,
    String kind = 'DAILY',
    int weekdays = 127,
    List<String> stepTitles = const <String>[],
    DateTime? nowUtc,
  }) =>
      tasks.createRoutine(
        userId: userId,
        name: name,
        kind: kind,
        weekdays: weekdays,
        stepTitles: stepTitles,
        nowUtc: nowUtc,
      );

  // ===========================================================================
  // 3. ỦY QUYỀN PERSONAL FINANCE (`account_transfer`, `D102`, `D112`)
  // ===========================================================================

  String createFinanceAccount({
    required String userId,
    required String name,
    String accountType = 'BANK',
    String currency = 'VND',
    required Decimal10k openingBalance,
    DateTime? nowUtc,
  }) =>
      finance.createFinanceAccount(
        userId: userId,
        name: name,
        accountType: accountType,
        currency: currency,
        openingBalance: openingBalance,
        nowUtc: nowUtc,
      );

  String createFinanceTransaction({
    required String userId,
    required String accountId,
    required String kind,
    required Decimal10k amount,
    required String occurredOnIso,
    String? categoryId,
    String? note,
    DateTime? nowUtc,
  }) =>
      finance.createFinanceTransaction(
        userId: userId,
        accountId: accountId,
        kind: kind,
        amount: amount,
        occurredOnIso: occurredOnIso,
        categoryId: categoryId,
        note: note,
        nowUtc: nowUtc,
      );

  String createAccountTransfer({
    required String userId,
    required String fromAccountId,
    required String toAccountId,
    required Decimal10k amount,
    required String occurredOnIso,
    String? note,
    DateTime? nowUtc,
  }) =>
      finance.createAccountTransfer(
        userId: userId,
        fromAccountId: fromAccountId,
        toAccountId: toAccountId,
        amount: amount,
        occurredOnIso: occurredOnIso,
        note: note,
        nowUtc: nowUtc,
      );

  Decimal10k computeAccountBalance({
    required String userId,
    required String accountId,
    String? asOfDateIso,
  }) =>
      finance.computeAccountBalance(
        userId: userId,
        accountId: accountId,
        asOfDateIso: asOfDateIso,
      );

  String? reconcileAccountBalance({
    required String userId,
    required String accountId,
    required Decimal10k actualBalance,
    required String occurredOnIso,
    DateTime? nowUtc,
  }) =>
      finance.reconcileAccountBalance(
        userId: userId,
        accountId: accountId,
        actualBalance: actualBalance,
        occurredOnIso: occurredOnIso,
        nowUtc: nowUtc,
      );

  String createBudget({
    required String userId,
    String? categoryId,
    required String currency,
    required Decimal10k amount,
    DateTime? nowUtc,
  }) =>
      finance.createBudget(
        userId: userId,
        categoryId: categoryId,
        currency: currency,
        amount: amount,
        nowUtc: nowUtc,
      );

  // ===========================================================================
  // 4. ỦY QUYỀN CALENDAR (`D126` JSON `recurrence`), REMINDER & NOTIFICATION (`D132`–`D138`)
  // ===========================================================================

  String createCalendarEvent({
    required String userId,
    required String title,
    required String startLocal,
    required String endLocal,
    bool allDay = false,
    String? startDate,
    String? endDate,
    String timezone = 'Asia/Ho_Chi_Minh',
    Map<String, dynamic>? recurrence,
    String? description,
    String? location,
    DateTime? nowUtc,
  }) =>
      calendar.createCalendarEvent(
        userId: userId,
        title: title,
        startLocal: startLocal,
        endLocal: endLocal,
        allDay: allDay,
        startDate: startDate,
        endDate: endDate,
        timezone: timezone,
        recurrence: recurrence,
        description: description,
        location: location,
        nowUtc: nowUtc,
      );

  String recordEventOccurrenceException({
    required String userId,
    required String eventId,
    required String originalStartLocal,
    required String kind,
    String? startLocal,
    String? endLocal,
    String? title,
    String? description,
    String? location,
    DateTime? nowUtc,
  }) =>
      calendar.recordEventOccurrenceException(
        userId: userId,
        eventId: eventId,
        originalStartLocal: originalStartLocal,
        kind: kind,
        startLocal: startLocal,
        endLocal: endLocal,
        title: title,
        description: description,
        location: location,
        nowUtc: nowUtc,
      );

  String splitRecurringEventFrom({
    required String userId,
    required String originalEventId,
    required String originalStartLocal,
    required String newStartLocal,
    required String newEndLocal,
    required String newTitle,
    Map<String, dynamic>? newRecurrence,
    DateTime? nowUtc,
  }) =>
      calendar.splitRecurringEventFrom(
        userId: userId,
        originalEventId: originalEventId,
        originalStartLocal: originalStartLocal,
        newStartLocal: newStartLocal,
        newEndLocal: newEndLocal,
        newTitle: newTitle,
        newRecurrence: newRecurrence,
        nowUtc: nowUtc,
      );

  String createReminder({
    required String userId,
    required String targetType,
    String? targetId,
    required String mode,
    int? offsetMinutes,
    String? localDate,
    String? localTime,
    String? title,
    bool urgent = false,
    DateTime? nowUtc,
  }) =>
      calendar.createReminder(
        userId: userId,
        targetType: targetType,
        targetId: targetId,
        mode: mode,
        offsetMinutes: offsetMinutes,
        localDate: localDate,
        localTime: localTime,
        title: title,
        urgent: urgent,
        nowUtc: nowUtc,
      );

  String formatLockScreenSafeNotificationBody({
    required String userId,
    required String moduleType,
    required String rawTitle,
  }) {
    final Map<String, dynamic> settings = getUserSettings(userId);
    final bool showDetails =
        (settings['showDetailsOnLockScreen'] as bool?) ?? false;
    return LocalNotificationScheduler.formatLockScreenSafeBody(
      showDetailsOnLockScreen: showDetails,
      moduleType: moduleType,
      rawTitle: rawTitle,
    );
  }

  // ===========================================================================
  // 5. ỦY QUYỀN KNOWLEDGE (`note`, `note_link` D189), GOALS & LIFE REVIEW (`D202`)
  // ===========================================================================

  String saveNote({
    required String userId,
    String? noteId,
    required String title,
    required String content,
    bool pinned = false,
    bool archived = false,
    DateTime? nowUtc,
  }) =>
      knowledge.saveNote(
        userId: userId,
        noteId: noteId,
        title: title,
        content: content,
        pinned: pinned,
        archived: archived,
        nowUtc: nowUtc,
      );

  String linkNoteToTarget({
    required String userId,
    required String noteId,
    required String targetType,
    required String targetId,
    DateTime? nowUtc,
  }) =>
      knowledge.linkNoteToTarget(
        userId: userId,
        noteId: noteId,
        targetType: targetType,
        targetId: targetId,
        nowUtc: nowUtc,
      );

  List<LocalEntityRow> listLinksForNote({
    required String userId,
    required String noteId,
  }) =>
      knowledge.listLinksForNote(userId: userId, noteId: noteId);

  String createGoal({
    required String userId,
    required String title,
    String? description,
    String? category,
    required String startDateIso,
    required String targetDateIso,
    List<String> milestoneTitles = const <String>[],
    List<GoalMilestoneDraft> milestoneDrafts = const <GoalMilestoneDraft>[],
    DateTime? nowUtc,
  }) =>
      knowledge.createGoal(
        userId: userId,
        title: title,
        description: description,
        category: category,
        startDateIso: startDateIso,
        targetDateIso: targetDateIso,
        milestoneTitles: milestoneTitles,
        milestoneDrafts: milestoneDrafts,
        nowUtc: nowUtc,
      );

  void updateGoal({
    required String userId,
    required String goalId,
    required String title,
    String? description,
    String? category,
    required String startDateIso,
    required String targetDateIso,
    String? status,
    DateTime? nowUtc,
  }) =>
      knowledge.updateGoal(
        userId: userId,
        goalId: goalId,
        title: title,
        description: description,
        category: category,
        startDateIso: startDateIso,
        targetDateIso: targetDateIso,
        status: status,
        nowUtc: nowUtc,
      );

  void deleteGoal({
    required String userId,
    required String goalId,
    DateTime? nowUtc,
  }) =>
      knowledge.deleteGoal(
        userId: userId,
        goalId: goalId,
        nowUtc: nowUtc,
      );

  void deleteNote({
    required String userId,
    required String noteId,
    DateTime? nowUtc,
  }) =>
      knowledge.deleteNote(
        userId: userId,
        noteId: noteId,
        nowUtc: nowUtc,
      );

  void deleteNoteLink({
    required String userId,
    required String linkId,
    DateTime? nowUtc,
  }) =>
      knowledge.deleteNoteLink(
        userId: userId,
        linkId: linkId,
        nowUtc: nowUtc,
      );

  String saveLifeReview({
    required String userId,
    required String periodType,
    required String periodKey,
    required String reflection,
    int? mood,
    DateTime? nowUtc,
  }) =>
      knowledge.saveLifeReview(
        userId: userId,
        periodType: periodType,
        periodKey: periodKey,
        reflection: reflection,
        mood: mood,
        nowUtc: nowUtc,
      );
}
