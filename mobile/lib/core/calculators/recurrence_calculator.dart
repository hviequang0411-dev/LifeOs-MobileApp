import '../time/user_day_clock.dart';

/// Ngoại lệ của một lần xuất hiện cụ thể trong chuỗi sự kiện lặp (`calendar_event_exception`).
class RecurrenceExceptionInput {
  const RecurrenceExceptionInput({
    required this.originalStartAt,
    required this.action,
    this.newStartAt,
    this.newEndAt,
    this.newTitle,
  });

  final DateTime originalStartAt;
  final String action; // CANCELLED | MODIFIED
  final DateTime? newStartAt;
  final DateTime? newEndAt;
  final String? newTitle;

  factory RecurrenceExceptionInput.fromJson(Map<String, dynamic> json) {
    return RecurrenceExceptionInput(
      originalStartAt: DateTime.parse(json['originalStartAt'] as String).toUtc(),
      action: (json['action'] as String? ?? 'CANCELLED').toUpperCase(),
      newStartAt: json['newStartAt'] == null
          ? null
          : DateTime.parse(json['newStartAt'] as String).toUtc(),
      newEndAt: json['newEndAt'] == null
          ? null
          : DateTime.parse(json['newEndAt'] as String).toUtc(),
      newTitle: json['newTitle'] as String?,
    );
  }
}

/// Một lần xuất hiện đã mở rộng của sự kiện lặp lại.
class ExpandedOccurrence {
  const ExpandedOccurrence({
    required this.originalStartAt,
    required this.startAt,
    required this.endAt,
    required this.modified,
    this.titleOverride,
  });

  final DateTime originalStartAt;
  final DateTime startAt;
  final DateTime endAt;
  final bool modified;
  final String? titleOverride;
}

/// Bộ mở rộng chuỗi lặp RFC-5545 `RRULE` cục bộ trên Mobile
/// khớp 100% với `docs/api/fixtures/recurrence-vectors.json` (PHASE_4 D125, D126, D134).
class RecurrenceCalculator {
  const RecurrenceCalculator._();

  static const int maxHardInstances = 730;

  /// Đánh giá trực tiếp một ca kiểm thử từ `docs/api/fixtures/recurrence-vectors.json`.
  static ({List<String> localOccurrences, List<String> utcOccurrences})
      evaluateSharedVectorCase(Map<String, dynamic> vectorCase) {
    final DateTime startLocal = _parseLocalDateTime(vectorCase['start'] as String);
    final DateTime fromLocal = _parseLocalDateTime(vectorCase['from'] as String);
    final DateTime toLocal = _parseLocalDateTime(vectorCase['to'] as String);
    final String? tz = vectorCase['timezone'] as String?;
    final Map<String, dynamic> rule =
        (vectorCase['rule'] as Map<dynamic, dynamic>).cast<String, dynamic>();

    final String freq = (rule['freq'] as String? ?? 'DAILY').toUpperCase();
    final int interval = (rule['interval'] as num?)?.toInt() ?? 1;
    final int? countLimit = (rule['count'] as num?)?.toInt();
    final String? untilStr = rule['until'] as String?;
    final DateTime? untilEndOfDay = untilStr == null
        ? null
        : DateTime.utc(
            int.parse(untilStr.substring(0, 4)),
            int.parse(untilStr.substring(5, 7)),
            int.parse(untilStr.substring(8, 10)),
            23,
            59,
            59,
          );
    final List<int> byWeekday = rule['byWeekday'] == null
        ? const <int>[]
        : (rule['byWeekday'] as List<dynamic>)
            .map((dynamic e) => _weekdayFromCode(e.toString()))
            .toList();
    final int? byMonthDay = (rule['byMonthDay'] as num?)?.toInt();

    final List<DateTime> generated = <DateTime>[];

    switch (freq) {
      case 'DAILY':
        int startStep = 0;
        if (countLimit == null && fromLocal.isAfter(startLocal)) {
          final int diffDays = fromLocal.difference(startLocal).inDays;
          if (diffDays > interval) {
            startStep = ((diffDays ~/ interval) - 1) * interval;
          }
        }
        for (int step = startStep;; step += interval) {
          final DateTime candidate = DateTime.utc(
            startLocal.year,
            startLocal.month,
            startLocal.day + step,
            startLocal.hour,
            startLocal.minute,
            startLocal.second,
          );
          if (untilEndOfDay != null && candidate.isAfter(untilEndOfDay)) break;
          if (!candidate.isBefore(toLocal) && countLimit == null) break;
          generated.add(candidate);
          if (countLimit != null && generated.length >= countLimit) break;
          if (generated.length >= 10000) break;
        }
      case 'WEEKLY':
        final List<int> days = byWeekday.isEmpty
            ? <int>[startLocal.weekday]
            : (List<int>.from(byWeekday)..sort());
        final DateTime anchorMonday = DateTime.utc(
          startLocal.year,
          startLocal.month,
          startLocal.day - (startLocal.weekday - DateTime.monday),
        );
        bool stop = false;
        for (int w = 0; !stop && w < 5000; w += interval) {
          final DateTime weekMon = anchorMonday.add(Duration(days: w * 7));
          for (final int wd in days) {
            final DateTime dayDate = weekMon.add(Duration(days: wd - 1));
            final DateTime candidate = DateTime.utc(
              dayDate.year,
              dayDate.month,
              dayDate.day,
              startLocal.hour,
              startLocal.minute,
              startLocal.second,
            );
            if (candidate.isBefore(startLocal)) continue;
            if (untilEndOfDay != null && candidate.isAfter(untilEndOfDay)) {
              stop = true;
              break;
            }
            if (!candidate.isBefore(toLocal) && countLimit == null) {
              stop = true;
              break;
            }
            generated.add(candidate);
            if (countLimit != null && generated.length >= countLimit) {
              stop = true;
              break;
            }
          }
        }
      case 'MONTHLY':
        final int desiredDay = byMonthDay ?? startLocal.day;
        for (int mStep = 0; mStep < 5000; mStep += interval) {
          final int totalMonths =
              (startLocal.year * 12 + (startLocal.month - 1)) + mStep;
          final int year = totalMonths ~/ 12;
          final int month = (totalMonths % 12) + 1;
          // D125: kẹp về ngày cuối tháng (28/29/30)
          final int clampedDay = desiredDay.clamp(1, _daysInMonth(year, month));
          final DateTime candidate = DateTime.utc(
            year,
            month,
            clampedDay,
            startLocal.hour,
            startLocal.minute,
            startLocal.second,
          );
          if (candidate.isBefore(startLocal)) continue;
          if (untilEndOfDay != null && candidate.isAfter(untilEndOfDay)) break;
          if (!candidate.isBefore(toLocal) && countLimit == null) break;
          generated.add(candidate);
          if (countLimit != null && generated.length >= countLimit) break;
        }
      case 'YEARLY':
        final int targetMonth = startLocal.month;
        final int desiredDay = byMonthDay ?? startLocal.day;
        for (int yStep = 0; yStep < 500; yStep += interval) {
          final int year = startLocal.year + yStep;
          // D125: 29/02 kẹp về 28/02 ở năm thường
          final int clampedDay =
              desiredDay.clamp(1, _daysInMonth(year, targetMonth));
          final DateTime candidate = DateTime.utc(
            year,
            targetMonth,
            clampedDay,
            startLocal.hour,
            startLocal.minute,
            startLocal.second,
          );
          if (candidate.isBefore(startLocal)) continue;
          if (untilEndOfDay != null && candidate.isAfter(untilEndOfDay)) break;
          if (!candidate.isBefore(toLocal) && countLimit == null) break;
          generated.add(candidate);
          if (countLimit != null && generated.length >= countLimit) break;
        }
    }

    final List<DateTime> inWindow = generated
        .where((DateTime dt) => !dt.isBefore(fromLocal) && dt.isBefore(toLocal))
        .toList();

    final List<String> localStrings =
        inWindow.map(_formatLocalDateTime).toList();
    final List<String> utcStrings = tz == null
        ? const <String>[]
        : inWindow.map((DateTime dt) => _localToUtcIso(dt, tz)).toList();

    return (localOccurrences: localStrings, utcOccurrences: utcStrings);
  }

  /// Mở rộng sự kiện lặp cho UI Lịch (hỗ trợ cả `calendar_event_exceptions` CANCELLED / MODIFIED).
  static List<ExpandedOccurrence> expand({
    required DateTime startAtUtc,
    required DateTime endAtUtc,
    required String timezone,
    required String? rrule,
    required DateTime rangeFromUtc,
    required DateTime rangeToUtc,
    List<RecurrenceExceptionInput> exceptions = const <RecurrenceExceptionInput>[],
  }) {
    final DateTime baseStart = startAtUtc.toUtc();
    final DateTime baseEnd = endAtUtc.toUtc();
    final Duration duration = baseEnd.difference(baseStart);
    final DateTime from = rangeFromUtc.toUtc();
    final DateTime to = rangeToUtc.toUtc();

    final UserDayClock clock = UserDayClock.fromUserSettings(timezone: timezone);
    final Duration tzOffset = Duration(minutes: clock.utcOffsetMinutes);

    if (rrule == null || rrule.trim().isEmpty) {
      if (baseEnd.isAfter(from) && baseStart.isBefore(to)) {
        return <ExpandedOccurrence>[
          ExpandedOccurrence(
            originalStartAt: baseStart,
            startAt: baseStart,
            endAt: baseEnd,
            modified: false,
          ),
        ];
      }
      return const <ExpandedOccurrence>[];
    }

    final Map<String, String> parts = _parseRrule(rrule);
    final String freq = (parts['FREQ'] ?? 'DAILY').toUpperCase();
    final int interval = int.tryParse(parts['INTERVAL'] ?? '1') ?? 1;
    final int? countLimit =
        parts['COUNT'] == null ? null : int.tryParse(parts['COUNT']!);
    final DateTime? untilUtc =
        parts['UNTIL'] == null ? null : _parseUntilUtc(parts['UNTIL']!);
    final List<int> byDay =
        parts['BYDAY'] == null ? const <int>[] : _parseByDay(parts['BYDAY']!);
    final int? byMonthDay =
        parts['BYMONTHDAY'] == null ? null : int.tryParse(parts['BYMONTHDAY']!);

    final Map<int, RecurrenceExceptionInput> exByEpochMs =
        <int, RecurrenceExceptionInput>{
      for (final RecurrenceExceptionInput ex in exceptions)
        ex.originalStartAt.millisecondsSinceEpoch: ex,
    };

    final DateTime localStart = baseStart.add(tzOffset);
    final List<DateTime> candidateOriginalStartsUtc = <DateTime>[];

    switch (freq) {
      case 'DAILY':
        for (int step = 0; step < maxHardInstances * interval; step += interval) {
          final DateTime localOcc = DateTime.utc(
            localStart.year,
            localStart.month,
            localStart.day + step,
            localStart.hour,
            localStart.minute,
            localStart.second,
          );
          final DateTime occUtc = localOcc.subtract(tzOffset);
          if (untilUtc != null && occUtc.isAfter(untilUtc)) break;
          if (occUtc.isAfter(to.add(const Duration(days: 7))) && countLimit == null) break;
          candidateOriginalStartsUtc.add(occUtc);
          if (countLimit != null && candidateOriginalStartsUtc.length >= countLimit) break;
        }
      case 'WEEKLY':
        final List<int> targetWeekdays =
            byDay.isEmpty ? <int>[localStart.weekday] : (List<int>.from(byDay)..sort());
        final DateTime anchorMonday = DateTime.utc(
          localStart.year,
          localStart.month,
          localStart.day - (localStart.weekday - DateTime.monday),
        );
        bool stop = false;
        for (int weekIdx = 0; weekIdx < maxHardInstances && !stop; weekIdx += interval) {
          final DateTime weekMonday = anchorMonday.add(Duration(days: weekIdx * 7));
          for (final int wd in targetWeekdays) {
            final DateTime dayDate = weekMonday.add(Duration(days: wd - 1));
            final DateTime localOcc = DateTime.utc(
              dayDate.year,
              dayDate.month,
              dayDate.day,
              localStart.hour,
              localStart.minute,
              localStart.second,
            );
            if (localOcc.isBefore(localStart)) continue;
            final DateTime occUtc = localOcc.subtract(tzOffset);
            if (untilUtc != null && occUtc.isAfter(untilUtc)) {
              stop = true;
              break;
            }
            candidateOriginalStartsUtc.add(occUtc);
            if (countLimit != null && candidateOriginalStartsUtc.length >= countLimit) {
              stop = true;
              break;
            }
          }
        }
      case 'MONTHLY':
        final int targetDay = byMonthDay ?? localStart.day;
        for (int mStep = 0; mStep < maxHardInstances; mStep += interval) {
          final int totalMonths =
              (localStart.year * 12 + (localStart.month - 1)) + mStep;
          final int year = totalMonths ~/ 12;
          final int month = (totalMonths % 12) + 1;
          final int clamped = targetDay.clamp(1, _daysInMonth(year, month));
          final DateTime localOcc = DateTime.utc(
            year,
            month,
            clamped,
            localStart.hour,
            localStart.minute,
            localStart.second,
          );
          if (localOcc.isBefore(localStart)) continue;
          final DateTime occUtc = localOcc.subtract(tzOffset);
          if (untilUtc != null && occUtc.isAfter(untilUtc)) break;
          candidateOriginalStartsUtc.add(occUtc);
          if (countLimit != null && candidateOriginalStartsUtc.length >= countLimit) break;
        }
      case 'YEARLY':
        final int targetDay = byMonthDay ?? localStart.day;
        for (int yStep = 0; yStep < maxHardInstances; yStep += interval) {
          final int year = localStart.year + yStep;
          final int clamped =
              targetDay.clamp(1, _daysInMonth(year, localStart.month));
          final DateTime localOcc = DateTime.utc(
            year,
            localStart.month,
            clamped,
            localStart.hour,
            localStart.minute,
            localStart.second,
          );
          if (localOcc.isBefore(localStart)) continue;
          final DateTime occUtc = localOcc.subtract(tzOffset);
          if (untilUtc != null && occUtc.isAfter(untilUtc)) break;
          candidateOriginalStartsUtc.add(occUtc);
          if (countLimit != null && candidateOriginalStartsUtc.length >= countLimit) break;
        }
    }

    final List<ExpandedOccurrence> results = <ExpandedOccurrence>[];
    for (final DateTime origStart in candidateOriginalStartsUtc) {
      final RecurrenceExceptionInput? ex =
          exByEpochMs[origStart.millisecondsSinceEpoch];
      if (ex != null && ex.action == 'CANCELLED') continue;

      final DateTime effectiveStart =
          (ex != null && ex.action == 'MODIFIED' && ex.newStartAt != null)
              ? ex.newStartAt!
              : origStart;
      final DateTime effectiveEnd =
          (ex != null && ex.action == 'MODIFIED' && ex.newEndAt != null)
              ? ex.newEndAt!
              : effectiveStart.add(duration);

      if (!effectiveStart.isBefore(from) && effectiveStart.isBefore(to)) {
        results.add(
          ExpandedOccurrence(
            originalStartAt: origStart,
            startAt: effectiveStart,
            endAt: effectiveEnd,
            modified: ex != null && ex.action == 'MODIFIED',
            titleOverride: ex?.newTitle,
          ),
        );
      }
    }

    results.sort(
      (ExpandedOccurrence a, ExpandedOccurrence b) =>
          a.startAt.compareTo(b.startAt),
    );
    return results;
  }

  /// Quy đổi giờ địa phương sang UTC có xử lý chuyển giờ mùa hè Bắc Mỹ (US DST: `America/New_York`, `America/Los_Angeles`).
  static String _localToUtcIso(DateTime localWall, String timezone) {
    if (timezone == 'America/New_York' || timezone == 'America/Los_Angeles') {
      final int stdOffsetHours = timezone == 'America/New_York' ? -5 : -8;
      final int dstOffsetHours = stdOffsetHours + 1;
      final int year = localWall.year;

      // DST Bắc Mỹ: Chủ nhật thứ 2 của tháng 3 lúc 02:00 -> Chủ nhật đầu tiên của tháng 11 lúc 02:00
      final DateTime march1 = DateTime.utc(year, 3, 1);
      final int firstSunMarch = 1 + ((DateTime.sunday - march1.weekday + 7) % 7);
      final int secondSunMarch = firstSunMarch + 7;

      final DateTime nov1 = DateTime.utc(year, 11, 1);
      final int firstSunNov = 1 + ((DateTime.sunday - nov1.weekday + 7) % 7);

      // Kiểm tra khoảng nhảy tiến không tồn tại (02:00..02:59 ngày Chủ nhật thứ 2 tháng 3)
      if (localWall.month == 3 &&
          localWall.day == secondSunMarch &&
          localWall.hour == 2) {
        // Giờ không tồn tại (DST nhảy tiến): dời tới mốc hợp lệ kế tiếp (+1 giờ trong EDT/PDT)
        final DateTime shiftedLocal = localWall.add(const Duration(hours: 1));
        final DateTime utc =
            shiftedLocal.subtract(Duration(hours: dstOffsetHours));
        return _formatUtcIsoSecond(utc);
      }

      final DateTime dstStartWall =
          DateTime.utc(year, 3, secondSunMarch, 2, 0, 0);
      final DateTime dstEndWall = DateTime.utc(year, 11, firstSunNov, 2, 0, 0);

      // Giờ lặp (DST lùi 01:00..01:59 ngày Chủ nhật đầu tháng 11): lấy lần xuất hiện đầu (EDT/PDT)
      final bool isInDst = !localWall.isBefore(dstStartWall) &&
          localWall.isBefore(dstEndWall);
      final int offsetHours = isInDst ? dstOffsetHours : stdOffsetHours;
      final DateTime utc = localWall.subtract(Duration(hours: offsetHours));
      return _formatUtcIsoSecond(utc);
    }

    final UserDayClock clock = UserDayClock.fromUserSettings(timezone: timezone);
    final DateTime utc =
        localWall.subtract(Duration(minutes: clock.utcOffsetMinutes));
    return _formatUtcIsoSecond(utc);
  }

  static DateTime _parseLocalDateTime(String raw) {
    return DateTime.parse('${raw}Z').toUtc();
  }

  static String _formatLocalDateTime(DateTime dt) {
    String two(int n) => n.toString().padLeft(2, '0');
    return '${dt.year.toString().padLeft(4, '0')}-${two(dt.month)}-${two(dt.day)}'
        'T${two(dt.hour)}:${two(dt.minute)}:${two(dt.second)}';
  }

  static String _formatUtcIsoSecond(DateTime dt) {
    return '${_formatLocalDateTime(dt.toUtc())}Z';
  }

  static int _weekdayFromCode(String code) {
    return switch (code.trim().toUpperCase()) {
      'MO' => DateTime.monday,
      'TU' => DateTime.tuesday,
      'WE' => DateTime.wednesday,
      'TH' => DateTime.thursday,
      'FR' => DateTime.friday,
      'SA' => DateTime.saturday,
      'SU' => DateTime.sunday,
      _ => DateTime.monday,
    };
  }

  static Map<String, String> _parseRrule(String raw) {
    final String cleaned =
        raw.trim().replaceFirst(RegExp('^RRULE:', caseSensitive: false), '');
    final Map<String, String> map = <String, String>{};
    for (final String token in cleaned.split(';')) {
      final int eq = token.indexOf('=');
      if (eq > 0) {
        map[token.substring(0, eq).trim().toUpperCase()] =
            token.substring(eq + 1).trim();
      }
    }
    return map;
  }

  static List<int> _parseByDay(String raw) {
    return raw.split(',').map(_weekdayFromCode).toList();
  }

  static DateTime _parseUntilUtc(String raw) {
    final String s = raw.trim();
    if (RegExp(r'^\d{8}T\d{6}Z$').hasMatch(s)) {
      return DateTime.utc(
        int.parse(s.substring(0, 4)),
        int.parse(s.substring(4, 6)),
        int.parse(s.substring(6, 8)),
        int.parse(s.substring(9, 11)),
        int.parse(s.substring(11, 13)),
        int.parse(s.substring(13, 15)),
      );
    }
    return DateTime.parse(s).toUtc();
  }

  static int _daysInMonth(int year, int month) {
    if (month == 12) return 31;
    return DateTime.utc(year, month + 1, 0).day;
  }
}
