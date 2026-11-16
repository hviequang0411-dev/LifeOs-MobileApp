import '../time/user_day_clock.dart';

/// Đầu vào cấu hình thói quen để tính chuỗi ngày (Streak) & thống kê cục bộ.
class HabitSpec {
  const HabitSpec({
    required this.frequencyType,
    required this.trackingType,
    this.targetCount,
    this.targetDurationSeconds,
    this.daysOfWeek = const <int>[],
    this.weekdaysMask,
    this.timesPerWeek,
    this.intervalDays,
    this.startOn,
    this.endOn,
    this.weekStart = 1,
    this.streakOnScheduleChange = 'CONTINUE_EXISTING',
  });

  final String frequencyType; // DAILY | WEEKDAYS | WEEKLY_COUNT | TIMES_PER_WEEK | INTERVAL_DAYS | EVERY_N_DAYS
  final String trackingType; // BOOLEAN | COUNT | DURATION
  final int? targetCount;
  final int? targetDurationSeconds;
  final List<int> daysOfWeek; // 1 (Thứ Hai) .. 7 (Chủ Nhật) theo chuẩn ISO-8601
  final int? weekdaysMask; // Bitmask bit 0 = Thứ Hai .. bit 6 = Chủ Nhật (VD: 21 = T2, T4, T6)
  final int? timesPerWeek;
  final int? intervalDays;
  final String? startOn;
  final String? endOn;
  final int weekStart;
  final String streakOnScheduleChange;

  factory HabitSpec.fromJson(Map<String, dynamic> json) {
    final List<dynamic>? rawDays = json['daysOfWeek'] as List<dynamic>?;
    final int? mask = (json['weekdays'] as num?)?.toInt();
    final List<int> decodedDays = <int>[];
    if (rawDays != null) {
      decodedDays.addAll(rawDays.map((dynamic e) => (e as num).toInt()));
    } else if (mask != null) {
      for (int bit = 0; bit < 7; bit++) {
        if ((mask & (1 << bit)) != 0) {
          decodedDays.add(bit + 1);
        }
      }
    }

    return HabitSpec(
      frequencyType:
          (json['frequencyType'] ?? json['type'] ?? 'DAILY').toString().toUpperCase(),
      trackingType: (json['trackingType'] as String? ?? 'BOOLEAN').toUpperCase(),
      targetCount: (json['targetCount'] as num?)?.toInt(),
      targetDurationSeconds: (json['targetDurationSeconds'] as num?)?.toInt(),
      daysOfWeek: decodedDays,
      weekdaysMask: mask,
      timesPerWeek: (json['timesPerWeek'] as num?)?.toInt(),
      intervalDays: (json['intervalDays'] as num?)?.toInt(),
      startOn: json['startOn'] as String?,
      endOn: json['endOn'] as String?,
      weekStart: (json['weekStart'] as num?)?.toInt() ?? 1,
      streakOnScheduleChange:
          (json['streakOnScheduleChange'] as String?) ?? 'CONTINUE_EXISTING',
    );
  }
}

/// Bản ghi điểm danh thói quen theo ngày (`habit_entry`).
class HabitEntryInput {
  const HabitEntryInput({
    required this.entryDate,
    required this.status,
    this.value,
  });

  final String entryDate; // YYYY-MM-DD
  final String status; // COMPLETED | DONE | SKIPPED | FAILED | PARTIAL
  final int? value;

  factory HabitEntryInput.fromJson(Map<String, dynamic> json) {
    return HabitEntryInput(
      entryDate: json['entryDate'] as String,
      status: (json['status'] as String? ?? 'COMPLETED').toUpperCase(),
      value: (json['value'] as num?)?.toInt(),
    );
  }
}

/// Kết quả tính toán thống kê thói quen cho UI và kiểm thử vector.
class HabitStatsResult {
  const HabitStatsResult({
    required this.currentStreak,
    required this.bestStreak,
    required this.completionRate30d,
    required this.totalCheckins,
    required this.completedOnAsOfDate,
    this.scheduledCount = 0,
    this.doneCount = 0,
    this.skippedCount = 0,
    this.missedCount = 0,
    this.completionRateString,
  });

  final int currentStreak;
  final int bestStreak;
  final double completionRate30d;
  final int totalCheckins;
  final bool completedOnAsOfDate;
  final int scheduledCount;
  final int doneCount;
  final int skippedCount;
  final int missedCount;
  final String? completionRateString;
}

/// Bộ tính toán Chuỗi ngày (Streak) và thống kê Thói quen
/// khớp 100% với đặc tả `PHASE_2_HABIT_ROUTINE.md` (D230, D233, D234) và `docs/api/fixtures/habit-vectors.json`.
class HabitCalculator {
  const HabitCalculator._();

  /// Đánh giá trực tiếp một ca kiểm thử từ `docs/api/fixtures/habit-vectors.json`.
  static HabitStatsResult evaluateSharedVectorCase(Map<String, dynamic> vectorCase) {
    final Map<String, dynamic> schedule =
        (vectorCase['schedule'] as Map<dynamic, dynamic>).cast<String, dynamic>();
    final int weekStart = (vectorCase['weekStart'] as num?)?.toInt() ?? 1;
    final String todayStr = vectorCase['today'] as String;
    final Map<String, dynamic> rawCheckins =
        (vectorCase['checkins'] as Map<dynamic, dynamic>).cast<String, dynamic>();
    final Map<String, dynamic> expectedMap =
        (vectorCase['expected'] as Map<dynamic, dynamic>).cast<String, dynamic>();
    final Map<String, dynamic> statsWindow =
        (expectedMap['stats'] as Map<dynamic, dynamic>).cast<String, dynamic>();

    final HabitSpec spec = HabitSpec.fromJson(<String, dynamic>{
      ...schedule,
      'weekStart': weekStart,
    });

    final Map<String, String> checkins = <String, String>{
      for (final MapEntry<String, dynamic> e in rawCheckins.entries)
        e.key: e.value.toString().toUpperCase(),
    };

    return evaluateWithWindow(
      spec: spec,
      checkinsByDate: checkins,
      todayIso: todayStr,
      windowFromIso: statsWindow['from'] as String,
      windowToIso: statsWindow['to'] as String,
    );
  }

  /// Đánh giá cho UI Mobile (`LifeOsLocalRepository`).
  static HabitStatsResult evaluate({
    required HabitSpec habit,
    required List<HabitEntryInput> entries,
    required String asOfDate,
  }) {
    final Map<String, String> checkins = <String, String>{};
    for (final HabitEntryInput e in entries) {
      if (_isEntryCompleted(habit, e)) {
        checkins[e.entryDate] = 'DONE';
      } else if (e.status == 'SKIPPED') {
        checkins[e.entryDate] = 'SKIPPED';
      }
    }

    final DateTime asOf = UserDayClock.parseIsoDateUtc(asOfDate);
    final DateTime winFrom = asOf.subtract(const Duration(days: 29));
    final String fromIso =
        UserDayClock.formatIsoDate(winFrom.year, winFrom.month, winFrom.day);

    // Xác định startOn mặc định nếu không khai báo
    String? effectiveStartOn = habit.startOn;
    if (effectiveStartOn == null && checkins.isNotEmpty) {
      final List<String> sorted = checkins.keys.toList()..sort();
      effectiveStartOn = sorted.first;
    }

    final HabitSpec normalizedSpec = HabitSpec(
      frequencyType: habit.frequencyType,
      trackingType: habit.trackingType,
      targetCount: habit.targetCount,
      targetDurationSeconds: habit.targetDurationSeconds,
      daysOfWeek: habit.daysOfWeek,
      weekdaysMask: habit.weekdaysMask,
      timesPerWeek: habit.timesPerWeek,
      intervalDays: habit.intervalDays,
      startOn: effectiveStartOn ?? fromIso,
      endOn: habit.endOn,
      weekStart: habit.weekStart,
    );

    return evaluateWithWindow(
      spec: normalizedSpec,
      checkinsByDate: checkins,
      todayIso: asOfDate,
      windowFromIso: effectiveStartOn ?? fromIso,
      windowToIso: asOfDate,
    );
  }

  static HabitStatsResult evaluateWithWindow({
    required HabitSpec spec,
    required Map<String, String> checkinsByDate,
    required String todayIso,
    required String windowFromIso,
    required String windowToIso,
  }) {
    final DateTime today = UserDayClock.parseIsoDateUtc(todayIso);
    final DateTime startOn = spec.startOn != null
        ? UserDayClock.parseIsoDateUtc(spec.startOn!)
        : UserDayClock.parseIsoDateUtc(windowFromIso);
    final DateTime? endOn =
        spec.endOn != null ? UserDayClock.parseIsoDateUtc(spec.endOn!) : null;
    final DateTime effectiveEnd =
        (endOn != null && endOn.isBefore(today)) ? endOn : today;

    final DateTime winFrom = UserDayClock.parseIsoDateUtc(windowFromIso);
    final DateTime winTo = UserDayClock.parseIsoDateUtc(windowToIso);

    final bool completedOnToday = checkinsByDate[todayIso] == 'DONE';
    final int totalDoneAll =
        checkinsByDate.values.where((String v) => v == 'DONE').length;

    // Nếu hôm nay chưa tới ngày bắt đầu (startOn > today)
    if (startOn.isAfter(today)) {
      return const HabitStatsResult(
        currentStreak: 0,
        bestStreak: 0,
        completionRate30d: 0.0,
        totalCheckins: 0,
        completedOnAsOfDate: false,
        scheduledCount: 0,
        doneCount: 0,
        skippedCount: 0,
        missedCount: 0,
        completionRateString: null,
      );
    }

    final String type = spec.frequencyType.toUpperCase();
    if (type == 'TIMES_PER_WEEK' || type == 'WEEKLY_COUNT') {
      return _evaluateTimesPerWeek(
        spec: spec,
        checkinsByDate: checkinsByDate,
        startOn: startOn,
        effectiveEnd: effectiveEnd,
        today: today,
        winFrom: winFrom,
        winTo: winTo,
        completedOnToday: completedOnToday,
      );
    }

    // Các lịch theo ngày: DAILY, WEEKDAYS, EVERY_N_DAYS / INTERVAL_DAYS
    int longestStreak = 0;
    int runningStreak = 0;
    for (DateTime d = startOn;
        !d.isAfter(effectiveEnd);
        d = d.add(const Duration(days: 1))) {
      if (!_isScheduledDay(spec, d, startOn)) continue;
      final String iso = UserDayClock.formatIsoDate(d.year, d.month, d.day);
      final String? state = checkinsByDate[iso];
      if (state == 'DONE') {
        runningStreak++;
        if (runningStreak > longestStreak) {
          longestStreak = runningStreak;
        }
      } else if (state == 'SKIPPED') {
        continue;
      } else {
        // Nếu là chính ngày hôm nay (today) và chưa check-in -> ân hạn chưa đứt chuỗi
        if (d.isAtSameMomentAs(today)) {
          continue;
        }
        runningStreak = 0;
      }
    }

    int currentStreak = 0;
    for (DateTime d = effectiveEnd;
        !d.isBefore(startOn);
        d = d.subtract(const Duration(days: 1))) {
      if (!_isScheduledDay(spec, d, startOn)) continue;
      final String iso = UserDayClock.formatIsoDate(d.year, d.month, d.day);
      final String? state = checkinsByDate[iso];
      if (state == 'DONE') {
        currentStreak++;
      } else if (state == 'SKIPPED') {
        continue;
      } else {
        if (d.isAtSameMomentAs(today)) {
          // Ngày hôm nay chưa qua hết nên chưa làm đứt chuỗi
          continue;
        }
        break;
      }
    }

    // Thống kê trong cửa sổ [max(winFrom, startOn) .. min(winTo, effectiveEnd)]
    final DateTime statStart = winFrom.isAfter(startOn) ? winFrom : startOn;
    final DateTime statEnd =
        winTo.isBefore(effectiveEnd) ? winTo : effectiveEnd;

    int scheduled = 0;
    int done = 0;
    int skipped = 0;
    int missed = 0;

    if (!statStart.isAfter(statEnd)) {
      for (DateTime d = statStart;
          !d.isAfter(statEnd);
          d = d.add(const Duration(days: 1))) {
        if (!_isScheduledDay(spec, d, startOn)) continue;
        final String iso = UserDayClock.formatIsoDate(d.year, d.month, d.day);
        final String? state = checkinsByDate[iso];
        if (state == 'DONE') {
          scheduled++;
          done++;
        } else if (state == 'SKIPPED') {
          scheduled++;
          skipped++;
        } else {
          // Nếu d == today mà chưa check-in thì ngày hôm nay đang diễn ra, chưa tính vào scheduled đã kết thúc trừ khi chỉ có 1 ngày
          if (d.isAtSameMomentAs(today)) {
            continue;
          }
          scheduled++;
          missed++;
        }
      }
    }

    final int denom = scheduled - skipped;
    final String? rateStr =
        denom <= 0 ? null : (done / denom).toStringAsFixed(4);
    final double rateDouble =
        denom <= 0 ? 0.0 : ((done * 10000 / denom).round() / 10000.0);

    return HabitStatsResult(
      currentStreak: currentStreak,
      bestStreak: longestStreak,
      completionRate30d: rateDouble,
      totalCheckins: totalDoneAll,
      completedOnAsOfDate: completedOnToday,
      scheduledCount: scheduled,
      doneCount: done,
      skippedCount: skipped,
      missedCount: missed,
      completionRateString: rateStr,
    );
  }

  static HabitStatsResult _evaluateTimesPerWeek({
    required HabitSpec spec,
    required Map<String, String> checkinsByDate,
    required DateTime startOn,
    required DateTime effectiveEnd,
    required DateTime today,
    required DateTime winFrom,
    required DateTime winTo,
    required bool completedOnToday,
  }) {
    final int target = (spec.timesPerWeek ?? 1).clamp(1, 7);
    final int weekStartDay = spec.weekStart.clamp(1, 7);

    DateTime startOfWeek(DateTime d) {
      final int diff = (d.weekday - weekStartDay + 7) % 7;
      return d.subtract(Duration(days: diff));
    }

    final DateTime firstWeekStart = startOfWeek(startOn);
    final DateTime lastWeekStart = startOfWeek(effectiveEnd);
    final DateTime currentWeekStart = startOfWeek(today);

    int longestStreak = 0;
    int runningStreak = 0;

    for (DateTime ws = firstWeekStart;
        !ws.isAfter(lastWeekStart);
        ws = ws.add(const Duration(days: 7))) {
      int doneInW = 0;
      int skipInW = 0;
      for (int i = 0; i < 7; i++) {
        final DateTime d = ws.add(Duration(days: i));
        if (d.isBefore(startOn) || d.isAfter(effectiveEnd)) continue;
        final String iso = UserDayClock.formatIsoDate(d.year, d.month, d.day);
        final String? st = checkinsByDate[iso];
        if (st == 'DONE') doneInW++;
        if (st == 'SKIPPED') skipInW++;
      }

      final int effectiveTarget = (target - skipInW).clamp(0, target);
      if (effectiveTarget == 0 && doneInW == 0) {
        // Tuần được SKIPPED đủ chỉ tiêu -> tuần trung tính (không tăng, không đứt chuỗi)
        continue;
      } else if (doneInW >= effectiveTarget && doneInW > 0) {
        runningStreak++;
        if (runningStreak > longestStreak) {
          longestStreak = runningStreak;
        }
      } else {
        // Nếu là tuần hiện tại đang diễn ra (chứa today và today chưa phải ngày cuối tuần đã khóa)
        final DateTime weekEnd = ws.add(const Duration(days: 6));
        if (ws.isAtSameMomentAs(currentWeekStart) && today.isBefore(weekEnd)) {
          continue;
        }
        runningStreak = 0;
      }
    }

    int currentStreak = 0;
    for (DateTime ws = lastWeekStart;
        !ws.isBefore(firstWeekStart);
        ws = ws.subtract(const Duration(days: 7))) {
      int doneInW = 0;
      int skipInW = 0;
      for (int i = 0; i < 7; i++) {
        final DateTime d = ws.add(Duration(days: i));
        if (d.isBefore(startOn) || d.isAfter(effectiveEnd)) continue;
        final String iso = UserDayClock.formatIsoDate(d.year, d.month, d.day);
        final String? st = checkinsByDate[iso];
        if (st == 'DONE') doneInW++;
        if (st == 'SKIPPED') skipInW++;
      }

      final int effectiveTarget = (target - skipInW).clamp(0, target);
      if (effectiveTarget == 0 && doneInW == 0) {
        continue;
      } else if (doneInW >= effectiveTarget && doneInW > 0) {
        currentStreak++;
      } else {
        final DateTime weekEnd = ws.add(const Duration(days: 6));
        if (ws.isAtSameMomentAs(currentWeekStart) && today.isBefore(weekEnd)) {
          continue;
        }
        break;
      }
    }

    // Thống kê theo từng tuần trong cửa sổ [winFrom .. winTo]
    final DateTime firstStatWeek = startOfWeek(winFrom.isAfter(startOn) ? winFrom : startOn);
    final DateTime lastStatWeek = startOfWeek(winTo.isBefore(effectiveEnd) ? winTo : effectiveEnd);

    int scheduled = 0;
    int done = 0;
    int skipped = 0;
    int missed = 0;

    for (DateTime ws = firstStatWeek;
        !ws.isAfter(lastStatWeek);
        ws = ws.add(const Duration(days: 7))) {
      int doneInW = 0;
      int skipInW = 0;
      for (int i = 0; i < 7; i++) {
        final DateTime d = ws.add(Duration(days: i));
        if (d.isBefore(startOn) || d.isAfter(effectiveEnd)) continue;
        if (d.isBefore(winFrom) || d.isAfter(winTo)) continue;
        final String iso = UserDayClock.formatIsoDate(d.year, d.month, d.day);
        final String? st = checkinsByDate[iso];
        if (st == 'DONE') doneInW++;
        if (st == 'SKIPPED') skipInW++;
      }

      final DateTime weekEnd = ws.add(const Duration(days: 6));
      final bool isIncompleteCurrentWeek =
          ws.isAtSameMomentAs(currentWeekStart) && today.isBefore(weekEnd);

      if (isIncompleteCurrentWeek) {
        final int progressed = doneInW + skipInW;
        scheduled += progressed;
        done += doneInW;
        skipped += skipInW;
      } else {
        scheduled += target;
        done += doneInW;
        skipped += skipInW;
        final int shortFall = target - (doneInW + skipInW);
        if (shortFall > 0) {
          missed += shortFall;
        }
      }
    }

    final int denom = scheduled - skipped;
    final String? rateStr =
        denom <= 0 ? null : (done / denom).toStringAsFixed(4);
    final double rateDouble =
        denom <= 0 ? 0.0 : ((done * 10000 / denom).round() / 10000.0);

    return HabitStatsResult(
      currentStreak: currentStreak,
      bestStreak: longestStreak,
      completionRate30d: rateDouble,
      totalCheckins: done,
      completedOnAsOfDate: completedOnToday,
      scheduledCount: scheduled,
      doneCount: done,
      skippedCount: skipped,
      missedCount: missed,
      completionRateString: rateStr,
    );
  }

  static bool _isScheduledDay(
    HabitSpec spec,
    DateTime date,
    DateTime startOn,
  ) {
    final String type = spec.frequencyType.toUpperCase();
    switch (type) {
      case 'DAILY':
        return true;
      case 'WEEKDAYS':
        return spec.daysOfWeek.contains(date.weekday);
      case 'EVERY_N_DAYS':
      case 'INTERVAL_DAYS':
        final int step = (spec.intervalDays ?? 1).clamp(1, 365);
        final int diff = date.difference(startOn).inDays;
        return diff >= 0 && (diff % step == 0);
      default:
        return true;
    }
  }

  static bool _isEntryCompleted(HabitSpec habit, HabitEntryInput entry) {
    final String st = entry.status.toUpperCase();
    if (st != 'COMPLETED' && st != 'DONE') {
      return false;
    }
    switch (habit.trackingType) {
      case 'COUNT':
        final int target = habit.targetCount ?? 1;
        return (entry.value ?? 1) >= target;
      case 'DURATION':
        final int targetSec = habit.targetDurationSeconds ?? 1;
        return (entry.value ?? 1) >= targetSec;
      case 'BOOLEAN':
      default:
        return (entry.value ?? 1) >= 1;
    }
  }
}
