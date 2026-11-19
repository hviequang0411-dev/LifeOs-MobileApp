import 'package:uuid/uuid.dart';

import '../../../core/calculators/habit_calculator.dart';
import '../../../core/database/app_database.dart';
import '../../../core/sync/entity_types.dart';

/// Repository cục bộ cho miền **Task, Project, Category, My Day, Habit & Routine** (`PHASE_1` & `PHASE_2`).
class TaskAndHabitLocalRepository {
  TaskAndHabitLocalRepository({
    required this.db,
    Uuid? uuid,
  }) : _uuid = uuid ?? const Uuid();

  final AppDatabase db;
  final Uuid _uuid;

  static String _normalizeTaskStatus(String raw) {
    final String upper = raw.trim().toUpperCase();
    if (upper == 'TODO') return 'OPEN';
    if (upper == 'COMPLETED') return 'DONE';
    return upper;
  }

  static String _normalizeTaskPriority(String raw) {
    final String upper = raw.trim().toUpperCase();
    if (upper == 'NONE' ||
        upper == 'LOW' ||
        upper == 'MEDIUM' ||
        upper == 'HIGH') {
      return upper;
    }
    return 'MEDIUM';
  }

  String createTask({
    required String userId,
    required String title,
    String? description,
    String priority = 'MEDIUM',
    String status = 'OPEN',
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
  }) {
    final String id = _uuid.v4();
    db.mutateLocalAndEnqueueOutbox(
      userId: userId,
      entityType: SyncEntityType.task,
      entityId: id,
      op: 'CREATE',
      payload: <String, dynamic>{
        'id': id,
        'title': title.trim(),
        'description': description?.trim(),
        'priority': _normalizeTaskPriority(priority),
        'status': _normalizeTaskStatus(status),
        'dueDate': dueDate,
        'dueTime': dueTime,
        'scheduledDate': scheduledDate,
        'isMyDay': isMyDay,
        'projectId': projectId,
        'categoryId': categoryId,
        'goalId': goalId,
        'milestoneId': milestoneId,
        'estimatedMinutes': estimatedMinutes,
      },
      nowUtc: nowUtc,
    );
    return id;
  }

  void toggleTaskCompletion({
    required String userId,
    required String taskId,
    DateTime? nowUtc,
  }) {
    final LocalEntityRow? existing = db.getEntity(
      userId: userId,
      entityType: SyncEntityType.task,
      entityId: taskId,
    );
    if (existing == null) return;
    final String currentStatus =
        (existing.payload['status'] as String? ?? 'OPEN').toUpperCase();
    final bool nextDone =
        currentStatus != 'COMPLETED' && currentStatus != 'DONE';
    final DateTime ts = (nowUtc ?? DateTime.now()).toUtc();

    db.mutateLocalAndEnqueueOutbox(
      userId: userId,
      entityType: SyncEntityType.task,
      entityId: taskId,
      op: 'UPDATE',
      payload: <String, dynamic>{
        ...existing.payload,
        'status': nextDone ? 'DONE' : 'OPEN',
        'completedAt': nextDone ? ts.toIso8601String() : null,
      },
      nowUtc: ts,
    );
  }

  void updateTask({
    required String userId,
    required String taskId,
    required Map<String, dynamic> patch,
    DateTime? nowUtc,
  }) {
    final LocalEntityRow? existing = db.getEntity(
      userId: userId,
      entityType: SyncEntityType.task,
      entityId: taskId,
    );
    if (existing == null) return;
    db.mutateLocalAndEnqueueOutbox(
      userId: userId,
      entityType: SyncEntityType.task,
      entityId: taskId,
      op: 'UPDATE',
      payload: <String, dynamic>{
        ...existing.payload,
        ...patch,
        'id': taskId,
      },
      nowUtc: nowUtc,
    );
  }

  void deleteTask({
    required String userId,
    required String taskId,
    DateTime? nowUtc,
  }) {
    db.mutateLocalAndEnqueueOutbox(
      userId: userId,
      entityType: SyncEntityType.task,
      entityId: taskId,
      op: 'DELETE',
      payload: <String, dynamic>{'id': taskId},
      nowUtc: nowUtc,
    );
  }

  String createProject({
    required String userId,
    required String name,
    String? description,
    String color = '#1B6B50',
    String? categoryId,
    DateTime? nowUtc,
  }) {
    final String id = _uuid.v4();
    db.mutateLocalAndEnqueueOutbox(
      userId: userId,
      entityType: SyncEntityType.project,
      entityId: id,
      op: 'CREATE',
      payload: <String, dynamic>{
        'id': id,
        'name': name.trim(),
        'description': description?.trim(),
        'archived': false,
        'color': color,
        'categoryId': categoryId,
      },
      nowUtc: nowUtc,
    );
    return id;
  }

  void deleteProject({
    required String userId,
    required String projectId,
    DateTime? nowUtc,
  }) {
    db.mutateLocalAndEnqueueOutbox(
      userId: userId,
      entityType: SyncEntityType.project,
      entityId: projectId,
      op: 'DELETE',
      payload: <String, dynamic>{'id': projectId},
      nowUtc: nowUtc,
    );
  }

  String createCategory({
    required String userId,
    required String name,
    String color = '#D97724',
    DateTime? nowUtc,
  }) {
    final String id = _uuid.v4();
    db.mutateLocalAndEnqueueOutbox(
      userId: userId,
      entityType: SyncEntityType.category,
      entityId: id,
      op: 'CREATE',
      payload: <String, dynamic>{
        'id': id,
        'name': name.trim(),
        'color': color,
      },
      nowUtc: nowUtc,
    );
    return id;
  }

  List<LocalEntityRow> queryMyDayTasks({
    required String userId,
    required String todayIso,
  }) {
    final List<LocalEntityRow> all = db.listEntities(
      userId: userId,
      entityType: SyncEntityType.task,
    );

    final List<LocalEntityRow> filtered = all.where((LocalEntityRow row) {
      final Map<String, dynamic> p = row.payload;
      final bool isMyDay = p['isMyDay'] == true;
      final String? due = p['dueDate'] as String?;
      final String? scheduled = p['scheduledDate'] as String?;
      final bool dueTodayOrOverdue = due != null && due.compareTo(todayIso) <= 0;
      final bool scheduledToday = scheduled == todayIso;
      return isMyDay || dueTodayOrOverdue || scheduledToday;
    }).toList();

    int priorityRank(String? prio) => switch ((prio ?? 'MEDIUM').toUpperCase()) {
          'HIGH' => 0,
          'MEDIUM' => 1,
          'LOW' => 2,
          'NONE' => 3,
          _ => 1,
        };

    filtered.sort((LocalEntityRow a, LocalEntityRow b) {
      final String aStatus = (a.payload['status'] as String? ?? 'TODO').toUpperCase();
      final String bStatus = (b.payload['status'] as String? ?? 'TODO').toUpperCase();
      final bool aDone = aStatus == 'COMPLETED' || aStatus == 'DONE';
      final bool bDone = bStatus == 'COMPLETED' || bStatus == 'DONE';
      if (aDone != bDone) return aDone ? 1 : -1;

      final String? aDue = a.payload['dueDate'] as String?;
      final String? bDue = b.payload['dueDate'] as String?;
      final bool aOverdue = !aDone && aDue != null && aDue.compareTo(todayIso) < 0;
      final bool bOverdue = !bDone && bDue != null && bDue.compareTo(todayIso) < 0;
      if (aOverdue != bOverdue) return aOverdue ? -1 : 1;

      final int pCmp = priorityRank(a.payload['priority'] as String?)
          .compareTo(priorityRank(b.payload['priority'] as String?));
      if (pCmp != 0) return pCmp;
      return a.updatedAt.compareTo(b.updatedAt);
    });

    return filtered;
  }

  static int _encodeWeekdaysBitmask(String frequencyType, List<int> daysOfWeek) {
    final String upper = frequencyType.trim().toUpperCase();
    if (upper == 'DAILY' || daysOfWeek.isEmpty) {
      return 127;
    }
    int mask = 0;
    for (final int day in daysOfWeek) {
      if (day >= 1 && day <= 7) {
        mask |= 1 << (day - 1);
      }
    }
    return mask == 0 ? 127 : mask;
  }

  String createHabit({
    required String userId,
    required String name,
    String? description,
    String? color,
    String? icon,
    String frequencyType = 'DAILY',
    String trackingType = 'BOOLEAN',
    int? targetCount,
    int? targetDurationSeconds,
    List<int> daysOfWeek = const <int>[],
    int? timesPerWeek,
    int? intervalDays,
    String? startOn,
    String? endOn,
    String? reminderTime,
    DateTime? nowUtc,
  }) {
    final String id = _uuid.v4();
    final int weekdaysBitmask = _encodeWeekdaysBitmask(frequencyType, daysOfWeek);
    db.mutateLocalAndEnqueueOutbox(
      userId: userId,
      entityType: SyncEntityType.habit,
      entityId: id,
      op: 'CREATE',
      payload: <String, dynamic>{
        'id': id,
        'name': name.trim(),
        if (description != null && description.trim().isNotEmpty)
          'description': description.trim(),
        if (color != null && color.trim().isNotEmpty) 'color': color.trim(),
        if (icon != null && icon.trim().isNotEmpty) 'icon': icon.trim(),
        'scheduleType': frequencyType,
        'frequencyType': frequencyType,
        'trackingType': trackingType,
        'targetCount': targetCount,
        'targetDurationSeconds': targetDurationSeconds,
        'daysOfWeek': daysOfWeek,
        'weekdays': weekdaysBitmask,
        'timesPerWeek': timesPerWeek,
        'intervalDays': intervalDays,
        if (startOn != null && startOn.trim().isNotEmpty)
          'startOn': startOn.trim(),
        if (endOn != null && endOn.trim().isNotEmpty) 'endOn': endOn.trim(),
        if (reminderTime != null && reminderTime.trim().isNotEmpty)
          'reminderTime': reminderTime.trim(),
        'archived': false,
      },
      nowUtc: nowUtc,
    );
    return id;
  }

  /// Điểm danh 1 chạm (`DONE` / `COMPLETED` / `SKIPPED`) cho Thói quen theo `(habitId, localDate)`.
  String checkInHabit({
    required String userId,
    required String habitId,
    required String entryDateIso,
    String status = 'COMPLETED',
    String? note,
    int? value,
    DateTime? nowUtc,
  }) {
    final List<LocalEntityRow> entries = db.listEntities(
      userId: userId,
      entityType: SyncEntityType.habitEntry,
    );
    LocalEntityRow? existing;
    for (final LocalEntityRow e in entries) {
      final String? rowDate =
          (e.payload['localDate'] ?? e.payload['entryDate']) as String?;
      if (e.payload['habitId'] == habitId && rowDate == entryDateIso) {
        existing = e;
        break;
      }
    }

    final String entryId = existing?.id ?? _uuid.v4();
    db.mutateLocalAndEnqueueOutbox(
      userId: userId,
      entityType: SyncEntityType.habitEntry,
      entityId: entryId,
      op: existing == null ? 'CREATE' : 'UPDATE',
      payload: <String, dynamic>{
        'id': entryId,
        'habitId': habitId,
        'localDate': entryDateIso,
        'entryDate': entryDateIso,
        'status': status,
        'note': note,
        'value': value ?? 1,
      },
      nowUtc: nowUtc,
    );
    return entryId;
  }

  HabitStatsResult computeHabitStats({
    required String userId,
    required String habitId,
    required String asOfDateIso,
  }) {
    final LocalEntityRow? habitRow = db.getEntity(
      userId: userId,
      entityType: SyncEntityType.habit,
      entityId: habitId,
    );
    if (habitRow == null) {
      return const HabitStatsResult(
        currentStreak: 0,
        bestStreak: 0,
        completionRate30d: 0,
        totalCheckins: 0,
        completedOnAsOfDate: false,
      );
    }

    final HabitSpec spec = HabitSpec.fromJson(habitRow.payload);
    final List<HabitEntryInput> entries = db
        .listEntities(userId: userId, entityType: SyncEntityType.habitEntry)
        .where((LocalEntityRow e) => e.payload['habitId'] == habitId)
        .map((LocalEntityRow e) => HabitEntryInput.fromJson(e.payload))
        .toList();

    return HabitCalculator.evaluate(
      habit: spec,
      entries: entries,
      asOfDate: asOfDateIso,
    );
  }

  String createRoutine({
    required String userId,
    required String name,
    String kind = 'DAILY',
    int weekdays = 127,
    String? reminderTime,
    List<String> stepTitles = const <String>[],
    List<int>? stepDurationsMinutes,
    DateTime? nowUtc,
  }) {
    final String routineId = _uuid.v4();
    final String normalizedKind = kind.trim().toUpperCase();
    db.mutateLocalAndEnqueueOutbox(
      userId: userId,
      entityType: SyncEntityType.routine,
      entityId: routineId,
      op: 'CREATE',
      payload: <String, dynamic>{
        'id': routineId,
        'name': name.trim(),
        'kind': normalizedKind,
        'weekdays': normalizedKind == 'DAILY' ? null : weekdays,
        if (reminderTime != null && reminderTime.trim().isNotEmpty)
          'reminderTime': reminderTime.trim(),
        'archived': false,
      },
      nowUtc: nowUtc,
    );

    for (int i = 0; i < stepTitles.length; i++) {
      final String stepId = _uuid.v4();
      final int duration = (stepDurationsMinutes != null &&
              i < stepDurationsMinutes.length)
          ? stepDurationsMinutes[i].clamp(1, 240)
          : 5;
      db.mutateLocalAndEnqueueOutbox(
        userId: userId,
        entityType: SyncEntityType.routineStep,
        entityId: stepId,
        op: 'CREATE',
        payload: <String, dynamic>{
          'id': stepId,
          'routineId': routineId,
          'title': stepTitles[i].trim(),
          'position': (i + 1) * 1000,
          'durationMinutes': duration,
        },
        nowUtc: nowUtc,
      );
    }
    return routineId;
  }

  /// Danh sách bước của một Routine, sắp xếp theo `position`.
  List<LocalEntityRow> listRoutineSteps({
    required String userId,
    required String routineId,
  }) {
    final List<LocalEntityRow> rows = db
        .listEntities(userId: userId, entityType: SyncEntityType.routineStep)
        .where((LocalEntityRow r) => r.payload['routineId'] == routineId)
        .toList();
    rows.sort((LocalEntityRow a, LocalEntityRow b) {
      final int pa = (a.payload['position'] as num?)?.toInt() ?? 0;
      final int pb = (b.payload['position'] as num?)?.toInt() ?? 0;
      return pa.compareTo(pb);
    });
    return rows;
  }

  /// Khởi tạo hoặc lấy phiên chạy Routine theo ngày `(routineId, localDate)` một cách idempotent (`SYNC_PROTOCOL.md`).
  String startOrGetRoutineLogForDate({
    required String userId,
    required String routineId,
    required String localDateIso,
    required int totalSteps,
    DateTime? nowUtc,
  }) {
    final List<LocalEntityRow> logs = db.listEntities(
      userId: userId,
      entityType: SyncEntityType.routineSession,
    );
    for (final LocalEntityRow log in logs) {
      if (log.payload['routineId'] == routineId &&
          log.payload['localDate'] == localDateIso) {
        return log.id;
      }
    }

    final String logId = _uuid.v4();
    db.mutateLocalAndEnqueueOutbox(
      userId: userId,
      entityType: SyncEntityType.routineSession,
      entityId: logId,
      op: 'CREATE',
      payload: <String, dynamic>{
        'id': logId,
        'routineId': routineId,
        'localDate': localDateIso,
        'totalSteps': totalSteps.clamp(1, 30),
      },
      nowUtc: nowUtc,
    );
    return logId;
  }

  /// Ghi nhận trạng thái từng bước (`DONE` hoặc `SKIPPED`) idempotent theo `(routineLogId, stepId)`.
  String recordRoutineStepLog({
    required String userId,
    required String routineLogId,
    required String stepId,
    required String status, // 'DONE' | 'SKIPPED'
    DateTime? nowUtc,
  }) {
    final String normalizedStatus = status.trim().toUpperCase();
    if (normalizedStatus != 'DONE' && normalizedStatus != 'SKIPPED') {
      throw ArgumentError(
        'Trạng thái routine_step_log phải là DONE hoặc SKIPPED: "$status"',
      );
    }
    final List<LocalEntityRow> existingList = db.listEntities(
      userId: userId,
      entityType: SyncEntityType.routineStepLog,
    );
    LocalEntityRow? existing;
    for (final LocalEntityRow row in existingList) {
      if (row.payload['routineLogId'] == routineLogId &&
          row.payload['stepId'] == stepId) {
        existing = row;
        break;
      }
    }

    final String id = existing?.id ?? _uuid.v4();
    db.mutateLocalAndEnqueueOutbox(
      userId: userId,
      entityType: SyncEntityType.routineStepLog,
      entityId: id,
      op: existing == null ? 'CREATE' : 'UPDATE',
      payload: <String, dynamic>{
        'id': id,
        'routineLogId': routineLogId,
        'stepId': stepId,
        'status': normalizedStatus,
      },
      nowUtc: nowUtc,
    );
    return id;
  }

  /// Lấy map `stepId -> status` cho một phiên `routineLogId`.
  Map<String, String> getRoutineStepStatuses({
    required String userId,
    required String routineLogId,
  }) {
    final Map<String, String> map = <String, String>{};
    for (final LocalEntityRow row in db.listEntities(
      userId: userId,
      entityType: SyncEntityType.routineStepLog,
    )) {
      if (row.payload['routineLogId'] == routineLogId) {
        final String? sid = row.payload['stepId'] as String?;
        final String? st = row.payload['status'] as String?;
        if (sid != null && st != null) {
          map[sid] = st.toUpperCase();
        }
      }
    }
    return map;
  }

  /// Cập nhật Thói quen và đẩy qua Outbox.
  void updateHabit({
    required String userId,
    required String habitId,
    String? name,
    String? description,
    String? color,
    String? icon,
    String? frequencyType,
    List<int>? daysOfWeek,
    int? timesPerWeek,
    int? intervalDays,
    String? startOn,
    String? endOn,
    String? reminderTime,
    bool? archived,
    DateTime? nowUtc,
  }) {
    final LocalEntityRow? existing = db.getEntity(
      userId: userId,
      entityType: SyncEntityType.habit,
      entityId: habitId,
    );
    if (existing == null) return;
    final Map<String, dynamic> next =
        Map<String, dynamic>.from(existing.payload);
    if (name != null && name.trim().isNotEmpty) {
      next['name'] = name.trim();
    }
    if (description != null) {
      if (description.trim().isEmpty) {
        next.remove('description');
      } else {
        next['description'] = description.trim();
      }
    }
    if (color != null) {
      if (color.trim().isEmpty) {
        next.remove('color');
      } else {
        next['color'] = color.trim();
      }
    }
    if (icon != null) {
      if (icon.trim().isEmpty) {
        next.remove('icon');
      } else {
        next['icon'] = icon.trim();
      }
    }
    final String effectiveType = (frequencyType ??
            next['scheduleType'] as String? ??
            next['frequencyType'] as String? ??
            'DAILY')
        .toUpperCase();
    final List<int> effectiveDays = daysOfWeek ??
        ((next['daysOfWeek'] as List<dynamic>?)
                ?.whereType<num>()
                .map((num n) => n.toInt())
                .toList() ??
            const <int>[]);
    if (frequencyType != null || daysOfWeek != null) {
      next['scheduleType'] = effectiveType;
      next['frequencyType'] = effectiveType;
      next['daysOfWeek'] = effectiveDays;
      next['weekdays'] = _encodeWeekdaysBitmask(effectiveType, effectiveDays);
      next['timesPerWeek'] = effectiveType == 'TIMES_PER_WEEK'
          ? (timesPerWeek ?? next['timesPerWeek'] ?? 3)
          : null;
      next['intervalDays'] = effectiveType == 'INTERVAL'
          ? (intervalDays ?? next['intervalDays'] ?? 2)
          : null;
    } else {
      if (timesPerWeek != null) next['timesPerWeek'] = timesPerWeek;
      if (intervalDays != null) next['intervalDays'] = intervalDays;
    }
    if (startOn != null) {
      if (startOn.trim().isEmpty) {
        next.remove('startOn');
      } else {
        next['startOn'] = startOn.trim();
      }
    }
    if (endOn != null) {
      if (endOn.trim().isEmpty) {
        next.remove('endOn');
      } else {
        next['endOn'] = endOn.trim();
      }
    }
    if (reminderTime != null) {
      if (reminderTime.trim().isEmpty) {
        next.remove('reminderTime');
      } else {
        next['reminderTime'] = reminderTime.trim();
      }
    }
    if (archived != null) {
      next['archived'] = archived;
      next['archivedAt'] =
          archived ? (nowUtc ?? DateTime.now().toUtc()).toIso8601String() : null;
    }
    db.mutateLocalAndEnqueueOutbox(
      userId: userId,
      entityType: SyncEntityType.habit,
      entityId: habitId,
      op: 'UPDATE',
      payload: <String, dynamic>{...next, 'id': habitId},
      nowUtc: nowUtc,
    );
  }

  /// Xóa mềm Thói quen.
  void deleteHabit({
    required String userId,
    required String habitId,
    DateTime? nowUtc,
  }) {
    db.mutateLocalAndEnqueueOutbox(
      userId: userId,
      entityType: SyncEntityType.habit,
      entityId: habitId,
      op: 'DELETE',
      payload: const <String, dynamic>{},
      nowUtc: nowUtc,
    );
  }

  /// Cập nhật Dự án (`project`). Lưu ý SYNC_PROTOCOL L350: payload dùng `archived` boolean.
  void updateProject({
    required String userId,
    required String projectId,
    required String name,
    String? description,
    String? color,
    bool? archived,
    DateTime? nowUtc,
  }) {
    final LocalEntityRow? existing = db.getEntity(
      userId: userId,
      entityType: SyncEntityType.project,
      entityId: projectId,
    );
    if (existing == null) return;
    final Map<String, dynamic> next =
        Map<String, dynamic>.from(existing.payload);
    next['name'] = name.trim();
    if (description != null) {
      next['description'] =
          description.trim().isEmpty ? null : description.trim();
    }
    if (color != null && color.trim().isNotEmpty) {
      next['color'] = color.trim();
    }
    if (archived != null) {
      next['archived'] = archived;
      next['archivedAt'] =
          archived ? (nowUtc ?? DateTime.now().toUtc()).toIso8601String() : null;
    }
    db.mutateLocalAndEnqueueOutbox(
      userId: userId,
      entityType: SyncEntityType.project,
      entityId: projectId,
      op: 'UPDATE',
      payload: <String, dynamic>{...next, 'id': projectId},
      nowUtc: nowUtc,
    );
  }

  /// Cập nhật Routine cùng danh sách bước (`routine_step`).
  void updateRoutine({
    required String userId,
    required String routineId,
    required String name,
    String? kind,
    int? weekdays,
    String? reminderTime,
    List<({String title, int? durationMinutes})>? steps,
    DateTime? nowUtc,
  }) {
    final LocalEntityRow? existing = db.getEntity(
      userId: userId,
      entityType: SyncEntityType.routine,
      entityId: routineId,
    );
    if (existing == null) return;
    final Map<String, dynamic> next =
        Map<String, dynamic>.from(existing.payload);
    final String normalizedKind =
        (kind ?? next['kind'] as String? ?? 'DAILY').trim().toUpperCase();
    next['name'] = name.trim();
    next['kind'] = normalizedKind;
    next['weekdays'] =
        normalizedKind == 'DAILY' ? null : (weekdays ?? next['weekdays'] ?? 127);
    if (reminderTime != null) {
      if (reminderTime.trim().isEmpty) {
        next.remove('reminderTime');
      } else {
        next['reminderTime'] = reminderTime.trim();
      }
    }
    db.mutateLocalAndEnqueueOutbox(
      userId: userId,
      entityType: SyncEntityType.routine,
      entityId: routineId,
      op: 'UPDATE',
      payload: <String, dynamic>{...next, 'id': routineId},
      nowUtc: nowUtc,
    );

    if (steps != null) {
      final List<LocalEntityRow> oldSteps =
          listRoutineSteps(userId: userId, routineId: routineId);
      final int sharedCount =
          steps.length < oldSteps.length ? steps.length : oldSteps.length;
      for (int i = 0; i < sharedCount; i++) {
        final LocalEntityRow oldStep = oldSteps[i];
        final ({String title, int? durationMinutes}) spec = steps[i];
        final int duration = (spec.durationMinutes ?? 5).clamp(1, 240);
        db.mutateLocalAndEnqueueOutbox(
          userId: userId,
          entityType: SyncEntityType.routineStep,
          entityId: oldStep.id,
          op: 'UPDATE',
          payload: <String, dynamic>{
            ...oldStep.payload,
            'id': oldStep.id,
            'routineId': routineId,
            'title': spec.title.trim(),
            'position': (i + 1) * 1000,
            'durationMinutes': duration,
          },
          nowUtc: nowUtc,
        );
      }
      for (int i = sharedCount; i < steps.length; i++) {
        final String stepId = _uuid.v4();
        final ({String title, int? durationMinutes}) spec = steps[i];
        final int duration = (spec.durationMinutes ?? 5).clamp(1, 240);
        db.mutateLocalAndEnqueueOutbox(
          userId: userId,
          entityType: SyncEntityType.routineStep,
          entityId: stepId,
          op: 'CREATE',
          payload: <String, dynamic>{
            'id': stepId,
            'routineId': routineId,
            'title': spec.title.trim(),
            'position': (i + 1) * 1000,
            'durationMinutes': duration,
          },
          nowUtc: nowUtc,
        );
      }
      for (int i = sharedCount; i < oldSteps.length; i++) {
        db.mutateLocalAndEnqueueOutbox(
          userId: userId,
          entityType: SyncEntityType.routineStep,
          entityId: oldSteps[i].id,
          op: 'DELETE',
          payload: const <String, dynamic>{},
          nowUtc: nowUtc,
        );
      }
    }
  }

  /// Xóa mềm Routine cùng toàn bộ các bước con (`routine_step`).
  void deleteRoutine({
    required String userId,
    required String routineId,
    DateTime? nowUtc,
  }) {
    final List<LocalEntityRow> steps =
        listRoutineSteps(userId: userId, routineId: routineId);
    for (final LocalEntityRow step in steps) {
      db.mutateLocalAndEnqueueOutbox(
        userId: userId,
        entityType: SyncEntityType.routineStep,
        entityId: step.id,
        op: 'DELETE',
        payload: const <String, dynamic>{},
        nowUtc: nowUtc,
      );
    }
    db.mutateLocalAndEnqueueOutbox(
      userId: userId,
      entityType: SyncEntityType.routine,
      entityId: routineId,
      op: 'DELETE',
      payload: const <String, dynamic>{},
      nowUtc: nowUtc,
    );
  }
}

