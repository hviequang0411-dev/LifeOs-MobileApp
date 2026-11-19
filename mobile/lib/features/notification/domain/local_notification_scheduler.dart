import '../../../core/database/app_database.dart';
import '../../../core/sync/entity_types.dart';
import '../../../core/time/user_day_clock.dart';

/// Tùy chọn bật/tắt thông báo cục bộ theo loại trên thiết bị.
class NotificationPreferences {
  const NotificationPreferences({
    this.masterEnabled = true,
    this.taskRemindersEnabled = true,
    this.habitRemindersEnabled = true,
    this.routineRemindersEnabled = true,
    this.calendarRemindersEnabled = true,
    this.permissionRequestedOnce = false,
  });

  final bool masterEnabled;
  final bool taskRemindersEnabled;
  final bool habitRemindersEnabled;
  final bool routineRemindersEnabled;
  final bool calendarRemindersEnabled;
  final bool permissionRequestedOnce;

  bool isTypeEnabled(String targetType) {
    if (!masterEnabled) return false;
    return switch (targetType.toUpperCase()) {
      'TASK' => taskRemindersEnabled,
      'HABIT' => habitRemindersEnabled,
      'ROUTINE' => routineRemindersEnabled,
      'EVENT' || 'CALENDAR' => calendarRemindersEnabled,
      _ => true,
    };
  }

  NotificationPreferences copyWith({
    bool? masterEnabled,
    bool? taskRemindersEnabled,
    bool? habitRemindersEnabled,
    bool? routineRemindersEnabled,
    bool? calendarRemindersEnabled,
    bool? permissionRequestedOnce,
  }) {
    return NotificationPreferences(
      masterEnabled: masterEnabled ?? this.masterEnabled,
      taskRemindersEnabled: taskRemindersEnabled ?? this.taskRemindersEnabled,
      habitRemindersEnabled:
          habitRemindersEnabled ?? this.habitRemindersEnabled,
      routineRemindersEnabled:
          routineRemindersEnabled ?? this.routineRemindersEnabled,
      calendarRemindersEnabled:
          calendarRemindersEnabled ?? this.calendarRemindersEnabled,
      permissionRequestedOnce:
          permissionRequestedOnce ?? this.permissionRequestedOnce,
    );
  }

  Map<String, dynamic> toJson() => <String, dynamic>{
        'masterEnabled': masterEnabled,
        'taskRemindersEnabled': taskRemindersEnabled,
        'habitRemindersEnabled': habitRemindersEnabled,
        'routineRemindersEnabled': routineRemindersEnabled,
        'calendarRemindersEnabled': calendarRemindersEnabled,
        'permissionRequestedOnce': permissionRequestedOnce,
      };

  factory NotificationPreferences.fromJson(Map<String, dynamic> json) {
    return NotificationPreferences(
      masterEnabled: json['masterEnabled'] as bool? ?? true,
      taskRemindersEnabled: json['taskRemindersEnabled'] as bool? ?? true,
      habitRemindersEnabled: json['habitRemindersEnabled'] as bool? ?? true,
      routineRemindersEnabled: json['routineRemindersEnabled'] as bool? ?? true,
      calendarRemindersEnabled: json['calendarRemindersEnabled'] as bool? ?? true,
      permissionRequestedOnce:
          json['permissionRequestedOnce'] as bool? ?? false,
    );
  }
}

/// Bộ lập lịch thông báo cục bộ và khử trùng thông báo (`docs/phases/PHASE_4_CALENDAR_NOTIFICATION.md` D132–D138):
/// - Tổng hợp từ `reminder`, `habit` (`reminderTime`) và `routine` (`reminderTime`) trong SQLite.
/// - Áp dụng khung giờ im lặng `quietStart`..`quietEnd` và bảo mật màn hình khóa `showDetailsOnLockScreen` từ `user_settings`.
/// - **Giới hạn 64 thông báo sắp tới trên iOS (`D133`):** sắp xếp tăng dần theo `fireAtUtcIso` và giữ tối đa **64 thông báo gần nhất**.
/// - **Khử trùng Local vs Push (`D132`):** dùng bảng `delivered_notifications(dedupe_key, shown_at)`.
class LocalNotificationScheduler {
  const LocalNotificationScheduler({required this.db});

  final AppDatabase db;

  static const int maxIosPendingNotifications = 64;

  /// Xây dựng lại lịch thông báo cục bộ từ SQLite (tối đa 64 thông báo gần nhất).
  List<ScheduledNotificationRecord> rebuildSchedule({
    required String userId,
    required UserDayClock dayClock,
    required bool showDetailsOnLockScreen,
    String? quietStart,
    String? quietEnd,
    NotificationPreferences preferences = const NotificationPreferences(),
    DateTime? nowUtc,
  }) {
    final DateTime referenceUtc = (nowUtc ?? DateTime.now()).toUtc();
    if (!preferences.masterEnabled) {
      return db.replaceScheduledNotifications(
        userId: userId,
        candidates: const <ScheduledNotificationRecord>[],
      );
    }

    final List<ScheduledNotificationRecord> candidates =
        <ScheduledNotificationRecord>[];

    // Tạo bảng tra cứu tiêu đề mục tiêu để thông báo hiển thị rõ tên công việc/thói quen/sự kiện
    final Map<String, String> targetTitles = <String, String>{};
    for (final SyncEntityType t in const <SyncEntityType>[
      SyncEntityType.task,
      SyncEntityType.habit,
      SyncEntityType.routine,
      SyncEntityType.calendarEvent,
    ]) {
      for (final LocalEntityRow entity in db.listEntities(
        userId: userId,
        entityType: t,
      )) {
        final String? title =
            (entity.payload['title'] ?? entity.payload['name']) as String?;
        if (title != null && title.trim().isNotEmpty) {
          targetTitles[entity.id] = title.trim();
        }
      }
    }

    // 1. Nguồn `reminder`
    final List<LocalEntityRow> reminders = db.listEntities(
      userId: userId,
      entityType: SyncEntityType.reminder,
    );
    for (final LocalEntityRow row in reminders) {
      if (row.payload['enabled'] == false || row.payload['paused'] == true) {
        continue;
      }
      final String rawTargetType =
          (row.payload['targetType'] as String? ?? 'NONE').toUpperCase();
      final String targetType =
          rawTargetType == 'NONE' ? 'STANDALONE' : rawTargetType;
      if (!preferences.isTypeEnabled(targetType)) continue;

      final String targetId = (row.payload['targetId'] as String?) ?? row.id;
      final String rawTitle = (row.payload['title'] as String?) ??
          targetTitles[targetId] ??
          'Nhắc nhở LifeOS';
      final String? localDate = row.payload['localDate'] as String?;
      final String localTime = (row.payload['localTime'] as String?) ?? '09:00';
      final bool urgent = row.payload['urgent'] == true;

      DateTime fireUtc = _computeNextFireUtc(
        dayClock: dayClock,
        referenceUtc: referenceUtc,
        localDate: localDate,
        localTime: localTime,
      );
      if (fireUtc.isBefore(referenceUtc)) continue;

      if (!urgent) {
        fireUtc = shiftOutOfQuietHours(
          fireUtc: fireUtc,
          dayClock: dayClock,
          quietStart: quietStart,
          quietEnd: quietEnd,
        );
      }

      final String dateBucket = dayClock.businessDateForUtc(fireUtc);
      final String dedupeKey = 'rem:${row.id}:$dateBucket:$localTime';

      candidates.add(
        _buildRecord(
          dedupeKey: dedupeKey,
          userId: userId,
          targetType: targetType,
          targetId: targetId,
          fireUtc: fireUtc,
          rawTitle: rawTitle,
          showDetailsOnLockScreen: showDetailsOnLockScreen,
        ),
      );
    }

    // 2. Nguồn `habit` có `reminderTime`
    if (preferences.habitRemindersEnabled) {
      final List<LocalEntityRow> habits = db.listEntities(
        userId: userId,
        entityType: SyncEntityType.habit,
      );
      for (final LocalEntityRow row in habits) {
        if (row.payload['archived'] == true) continue;
        final String? reminderTime =
            (row.payload['reminderTime'] as String?)?.trim();
        if (reminderTime == null || reminderTime.isEmpty) continue;

        final String rawTitle =
            (row.payload['name'] as String?) ?? 'Thói quen trong ngày';
        DateTime fireUtc = _computeNextFireUtc(
          dayClock: dayClock,
          referenceUtc: referenceUtc,
          localDate: null,
          localTime: reminderTime,
        );
        fireUtc = shiftOutOfQuietHours(
          fireUtc: fireUtc,
          dayClock: dayClock,
          quietStart: quietStart,
          quietEnd: quietEnd,
        );

        final String dateBucket = dayClock.businessDateForUtc(fireUtc);
        candidates.add(
          _buildRecord(
            dedupeKey: 'hab:${row.id}:$dateBucket:$reminderTime',
            userId: userId,
            targetType: 'HABIT',
            targetId: row.id,
            fireUtc: fireUtc,
            rawTitle: rawTitle,
            showDetailsOnLockScreen: showDetailsOnLockScreen,
          ),
        );
      }
    }

    // 3. Nguồn `routine` có `reminderTime` (tuân thủ `kind`: DAILY hoặc WEEKLY bitmask)
    if (preferences.routineRemindersEnabled) {
      final List<LocalEntityRow> routines = db.listEntities(
        userId: userId,
        entityType: SyncEntityType.routine,
      );
      for (final LocalEntityRow row in routines) {
        if (row.payload['archived'] == true) continue;
        final String? reminderTime =
            (row.payload['reminderTime'] as String?)?.trim();
        if (reminderTime == null || reminderTime.isEmpty) continue;

        final String kind =
            (row.payload['kind'] as String? ?? 'DAILY').toUpperCase();
        final int? weekdaysMask = (row.payload['weekdays'] as num?)?.toInt();

        DateTime? fireUtc = _computeNextRoutineFireUtc(
          dayClock: dayClock,
          referenceUtc: referenceUtc,
          localTime: reminderTime,
          kind: kind,
          weekdaysMask: weekdaysMask,
        );
        if (fireUtc == null) continue;

        fireUtc = shiftOutOfQuietHours(
          fireUtc: fireUtc,
          dayClock: dayClock,
          quietStart: quietStart,
          quietEnd: quietEnd,
        );

        final String dateBucket = dayClock.businessDateForUtc(fireUtc);
        final String rawTitle =
            (row.payload['name'] as String?) ?? 'Chuỗi thói quen';
        candidates.add(
          _buildRecord(
            dedupeKey: 'rtn:${row.id}:$dateBucket:$reminderTime',
            userId: userId,
            targetType: 'ROUTINE',
            targetId: row.id,
            fireUtc: fireUtc,
            rawTitle: rawTitle,
            showDetailsOnLockScreen: showDetailsOnLockScreen,
          ),
        );
      }
    }

    final List<ScheduledNotificationRecord> capped =
        selectTop64Candidates(candidates);
    return db.replaceScheduledNotifications(
      userId: userId,
      candidates: capped,
    );
  }

  /// Thuật toán chọn lọc tối đa 64 thông báo sắp tới gần nhất cho giới hạn iOS (`D133`).
  static List<ScheduledNotificationRecord> selectTop64Candidates(
    List<ScheduledNotificationRecord> candidates,
  ) {
    final Map<String, ScheduledNotificationRecord> deduped =
        <String, ScheduledNotificationRecord>{};
    for (final ScheduledNotificationRecord item in candidates) {
      deduped[item.dedupeKey] = item;
    }
    final List<ScheduledNotificationRecord> sorted = deduped.values.toList()
      ..sort(
        (ScheduledNotificationRecord a, ScheduledNotificationRecord b) =>
            a.fireAtUtcIso.compareTo(b.fireAtUtcIso),
      );
    return sorted.take(maxIosPendingNotifications).toList();
  }

  /// Kiểm tra một giờ cục bộ (`HH:mm` dạng phút trong ngày) có rơi vào giờ im lặng hay không.
  static bool isWithinQuietHours({
    required int minuteOfDay,
    String? quietStart,
    String? quietEnd,
  }) {
    final int? startMin = _parseMinutes(quietStart);
    final int? endMin = _parseMinutes(quietEnd);
    if (startMin == null || endMin == null || startMin == endMin) {
      return false;
    }
    if (startMin < endMin) {
      return minuteOfDay >= startMin && minuteOfDay < endMin;
    }
    // Khoảng qua đêm (vd: 22:00 -> 07:00)
    return minuteOfDay >= startMin || minuteOfDay < endMin;
  }

  /// Dời thời điểm nổ thông báo ra ngay sau `quietEnd` nếu rơi vào khung giờ im lặng.
  static DateTime shiftOutOfQuietHours({
    required DateTime fireUtc,
    required UserDayClock dayClock,
    String? quietStart,
    String? quietEnd,
  }) {
    final int? startMin = _parseMinutes(quietStart);
    final int? endMin = _parseMinutes(quietEnd);
    if (startMin == null || endMin == null || startMin == endMin) {
      return fireUtc;
    }
    final DateTime localWall =
        fireUtc.add(Duration(minutes: dayClock.utcOffsetMinutes));
    final int minuteOfDay = localWall.hour * 60 + localWall.minute;
    if (!isWithinQuietHours(
      minuteOfDay: minuteOfDay,
      quietStart: quietStart,
      quietEnd: quietEnd,
    )) {
      return fireUtc;
    }

    final DateTime endDayBase =
        DateTime.utc(localWall.year, localWall.month, localWall.day);
    final int daysToAdd =
        (startMin > endMin && minuteOfDay >= startMin) ? 1 : 0;
    final DateTime shiftedLocalWall = endDayBase.add(
      Duration(days: daysToAdd, minutes: endMin),
    );
    return shiftedLocalWall
        .subtract(Duration(minutes: dayClock.utcOffsetMinutes));
  }

  /// Kiểm tra và ghi nhận hiển thị thông báo theo `dedupeKey` (`D132`).
  bool shouldDisplayAndMarkDelivered({
    required String userId,
    required String dedupeKey,
    DateTime? nowUtc,
  }) {
    return db.recordNotificationDeliveryIfNew(
      userId: userId,
      dedupeKey: dedupeKey,
      nowUtc: nowUtc,
    );
  }

  static ScheduledNotificationRecord _buildRecord({
    required String dedupeKey,
    required String userId,
    required String targetType,
    required String targetId,
    required DateTime fireUtc,
    required String rawTitle,
    required bool showDetailsOnLockScreen,
  }) {
    return ScheduledNotificationRecord(
      dedupeKey: dedupeKey,
      userId: userId,
      targetType: targetType,
      targetId: targetId,
      fireAtUtcIso: fireUtc.toIso8601String(),
      title: _channelTitleForType(targetType),
      body: showDetailsOnLockScreen
          ? rawTitle
          : formatLockScreenSafeBody(
              showDetailsOnLockScreen: false,
              moduleType: targetType,
              rawTitle: rawTitle,
            ),
      redactedForLockScreen: !showDetailsOnLockScreen,
    );
  }

  static String _channelTitleForType(String targetType) {
    return switch (targetType.toUpperCase()) {
      'TASK' => 'Nhắc việc · LifeOS',
      'HABIT' => 'Thói quen · LifeOS',
      'ROUTINE' => 'Chuỗi thói quen · LifeOS',
      'EVENT' || 'CALENDAR' => 'Lịch hẹn · LifeOS',
      _ => 'LifeOS',
    };
  }

  static int? _parseMinutes(String? hhmm) {
    if (hhmm == null || hhmm.trim().isEmpty) return null;
    final List<String> parts = hhmm.trim().split(':');
    if (parts.length < 2) return null;
    final int? h = int.tryParse(parts[0]);
    final int? m = int.tryParse(parts[1]);
    if (h == null || m == null || h < 0 || h > 23 || m < 0 || m > 59) {
      return null;
    }
    return h * 60 + m;
  }

  static DateTime _computeNextFireUtc({
    required UserDayClock dayClock,
    required DateTime referenceUtc,
    required String? localDate,
    required String localTime,
  }) {
    final String effectiveDate =
        localDate ?? dayClock.businessDateForUtc(referenceUtc);
    final DateTime dateParts = UserDayClock.parseIsoDateUtc(effectiveDate);
    final List<String> hm = localTime.split(':');
    final int hour = hm.isNotEmpty ? (int.tryParse(hm[0]) ?? 9) : 9;
    final int minute = hm.length > 1 ? (int.tryParse(hm[1]) ?? 0) : 0;

    final DateTime localWall = DateTime.utc(
      dateParts.year,
      dateParts.month,
      dateParts.day,
      hour,
      minute,
    );
    DateTime utcCandidate =
        localWall.subtract(Duration(minutes: dayClock.utcOffsetMinutes));
    if (localDate == null && utcCandidate.isBefore(referenceUtc)) {
      utcCandidate = utcCandidate.add(const Duration(days: 1));
    }
    return utcCandidate;
  }

  static DateTime? _computeNextRoutineFireUtc({
    required UserDayClock dayClock,
    required DateTime referenceUtc,
    required String localTime,
    required String kind,
    required int? weekdaysMask,
  }) {
    if (kind != 'WEEKLY' || weekdaysMask == null || weekdaysMask <= 0) {
      return _computeNextFireUtc(
        dayClock: dayClock,
        referenceUtc: referenceUtc,
        localDate: null,
        localTime: localTime,
      );
    }

    final String todayIso = dayClock.businessDateForUtc(referenceUtc);
    final DateTime baseDate = UserDayClock.parseIsoDateUtc(todayIso);
    final List<String> hm = localTime.split(':');
    final int hour = hm.isNotEmpty ? (int.tryParse(hm[0]) ?? 9) : 9;
    final int minute = hm.length > 1 ? (int.tryParse(hm[1]) ?? 0) : 0;

    for (int offset = 0; offset <= 7; offset++) {
      final DateTime day = baseDate.add(Duration(days: offset));
      // weekday 1=Mon..7=Sun -> bitmask 1<<(weekday-1)
      final int bit = 1 << (day.weekday - 1);
      if ((weekdaysMask & bit) == 0) continue;

      final DateTime localWall =
          DateTime.utc(day.year, day.month, day.day, hour, minute);
      final DateTime utcCandidate =
          localWall.subtract(Duration(minutes: dayClock.utcOffsetMinutes));
      if (!utcCandidate.isBefore(referenceUtc)) {
        return utcCandidate;
      }
    }
    return null;
  }

  static String formatLockScreenSafeBody({
    required bool showDetailsOnLockScreen,
    required String moduleType,
    required String rawTitle,
  }) {
    if (showDetailsOnLockScreen) return rawTitle;
    return switch (moduleType.toUpperCase()) {
      'FINANCE' => 'Bạn có một nhắc nhở giao dịch tài chính cần kiểm tra.',
      'TASK' => 'Bạn có một công việc đến hạn trong LifeOS.',
      'HABIT' => 'Đã đến giờ thực hiện thói quen trong ngày của bạn.',
      'ROUTINE' => 'Đã đến giờ bắt đầu chuỗi thói quen hằng ngày.',
      'EVENT' || 'CALENDAR' => 'Bạn có một sự kiện sắp diễn ra trong lịch.',
      _ => 'Bạn có một thông báo mới trong LifeOS.',
    };
  }
}
