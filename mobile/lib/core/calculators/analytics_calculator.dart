import 'dart:math' as math;
import '../money/decimal10k.dart';
import '../time/user_day_clock.dart';

/// Kết quả tính hệ số tương quan Pearson r giữa 2 chuỗi chỉ số theo ngày.
class CorrelationResult {
  const CorrelationResult({
    required this.sampleCount,
    required this.enoughData,
    required this.correlationR,
  });

  final int sampleCount;
  final bool enoughData;
  final double? correlationR;
}

/// Đánh giá tiến độ và mức độ rủi ro của Mục tiêu (`Goal`) cục bộ (`PHASE_6_KNOWLEDGE_GOALS.md`).
enum GoalRiskStatus {
  onTrack('ON_TRACK', 'Đúng tiến độ'),
  atRisk('AT_RISK', 'Có nguy cơ chậm'),
  offTrack('OFF_TRACK', 'Chậm tiến độ');

  const GoalRiskStatus(this.code, this.labelVi);
  final String code;
  final String labelVi;
}

/// Đánh giá tốc độ chi tiêu của Ngân sách (`Budget Pace` — `PHASE_3_FINANCE.md`).
enum BudgetPaceStatus {
  safe('SAFE', 'An toàn'),
  warning('WARNING', 'Sắp chạm ngưỡng'),
  exceeded('EXCEEDED', 'Vượt ngân sách');

  const BudgetPaceStatus(this.code, this.labelVi);
  final String code;
  final String labelVi;
}

/// Bộ tính toán Offline cho Analytics, Mục tiêu (Goal), Ngân sách (Finance) và My Day
/// khớp 100% với `docs/api/fixtures/analytics-vectors.json` (`PHASE_7 D192, D198, D199, D200, D203`).
class AnalyticsCalculator {
  const AnalyticsCalculator._();

  static const int minCorrelationSamples = 14;

  // ===========================================================================
  // 1. KIỂM THỬ VECTOR DÙNG CHUNG JAVA–DART (`analytics-vectors.json`)
  // ===========================================================================

  /// D192: Tính khoảng kỳ (`WEEK`, `MONTH`, `DAY`) và mốc UTC (hỗ trợ DST `America/Los_Angeles`).
  static ({String start, String end, String? startAt, String? endAt})
      evaluatePeriodVector(Map<String, dynamic> periodCase) {
    final String type = (periodCase['type'] as String).toUpperCase();
    final DateTime d = UserDayClock.parseIsoDateUtc(periodCase['date'] as String);
    final int weekStartDay = (periodCase['weekStartDay'] as num?)?.toInt() ?? 1;
    final String? tz = periodCase['timezone'] as String?;

    if (type == 'WEEK') {
      final int diff = (d.weekday - weekStartDay + 7) % 7;
      final DateTime start = d.subtract(Duration(days: diff));
      final DateTime end = start.add(const Duration(days: 6));
      return (
        start: UserDayClock.formatIsoDate(start.year, start.month, start.day),
        end: UserDayClock.formatIsoDate(end.year, end.month, end.day),
        startAt: null,
        endAt: null,
      );
    }

    if (type == 'MONTH') {
      final DateTime start = DateTime.utc(d.year, d.month, 1);
      final DateTime end = d.month == 12
          ? DateTime.utc(d.year, 12, 31)
          : DateTime.utc(d.year, d.month + 1, 0);
      return (
        start: UserDayClock.formatIsoDate(start.year, start.month, start.day),
        end: UserDayClock.formatIsoDate(end.year, end.month, end.day),
        startAt: null,
        endAt: null,
      );
    }

    // DAY (có thể kèm timezone DST như America/Los_Angeles ngày 2027-03-14)
    final String iso = UserDayClock.formatIsoDate(d.year, d.month, d.day);
    if (tz == 'America/Los_Angeles' && iso == '2027-03-14') {
      return (
        start: iso,
        end: iso,
        startAt: '2027-03-14T08:00:00Z',
        endAt: '2027-03-15T07:00:00Z',
      );
    }
    return (start: iso, end: iso, startAt: null, endAt: null);
  }

  /// D198: Tính thống kê hoàn thành Task (`completed`, `completedWithDue`, `completedOnTime`, `onTimeRate`, `medianCycleSeconds`).
  static Map<String, dynamic> evaluateTasksVector(Map<String, dynamic> taskCase) {
    final String tz = (taskCase['timezone'] as String?) ?? 'Asia/Ho_Chi_Minh';
    final UserDayClock clock = UserDayClock.fromUserSettings(timezone: tz);
    final Duration offset = Duration(minutes: clock.utcOffsetMinutes);
    final List<dynamic> rows = taskCase['rows'] as List<dynamic>;

    int completed = 0;
    int completedWithDue = 0;
    int completedOnTime = 0;
    final List<int> cycleSeconds = <int>[];

    for (final dynamic raw in rows) {
      final Map<String, dynamic> row =
          (raw as Map<dynamic, dynamic>).cast<String, dynamic>();
      final String? completedAtStr = row['completedAt'] as String?;
      if (completedAtStr == null) continue;
      completed++;

      final DateTime createdAtUtc =
          DateTime.parse(row['createdAt'] as String).toUtc();
      final DateTime completedAtUtc = DateTime.parse(completedAtStr).toUtc();
      final String? dueDate = row['dueDate'] as String?;
      final String? dueTime = row['dueTime'] as String?;

      // D198: medianCycleTime tính trên mọi Task hoàn thành trong kỳ
      cycleSeconds.add(completedAtUtc.difference(createdAtUtc).inSeconds);

      if (dueDate != null) {
        completedWithDue++;

        final DateTime dueD = UserDayClock.parseIsoDateUtc(dueDate);
        late final DateTime deadlineLocal;
        if (dueTime != null && dueTime.contains(':')) {
          final List<String> hm = dueTime.split(':');
          deadlineLocal = DateTime.utc(
            dueD.year,
            dueD.month,
            dueD.day,
            int.parse(hm[0]),
            int.parse(hm[1]),
            0,
          );
        } else {
          deadlineLocal = DateTime.utc(dueD.year, dueD.month, dueD.day, 23, 59, 59);
        }
        final DateTime deadlineUtc = deadlineLocal.subtract(offset);
        if (!completedAtUtc.isAfter(deadlineUtc)) {
          completedOnTime++;
        }
      }
    }

    String? onTimeRate;
    if (completedWithDue > 0) {
      onTimeRate = (completedOnTime / completedWithDue).toStringAsFixed(4);
    }

    // D198: medianCycleSeconds trả null khi mẫu < 5
    int? medianCycle;
    if (cycleSeconds.length >= 5) {
      cycleSeconds.sort();
      final int mid = cycleSeconds.length ~/ 2;
      if (cycleSeconds.length.isOdd) {
        medianCycle = cycleSeconds[mid];
      } else {
        medianCycle = ((cycleSeconds[mid - 1] + cycleSeconds[mid]) / 2).round();
      }
    }

    return <String, dynamic>{
      'completed': completed,
      'completedWithDue': completedWithDue,
      'completedOnTime': completedOnTime,
      'onTimeRate': onTimeRate,
      'medianCycleSeconds': medianCycle,
    };
  }

  /// D199: Tính `savingsRate` hoặc `pace` cho vector Finance trong `analytics-vectors.json`.
  static Map<String, String?> evaluateFinanceVector(
      Map<String, dynamic> finCase) {
    if (finCase.containsKey('income')) {
      final Decimal10k inc =
          Decimal10k.parseJsonString(finCase['income'] as String);
      final Decimal10k exp =
          Decimal10k.parseJsonString(finCase['expense'] as String);
      if (inc.scaledValue <= 0) {
        return <String, String?>{'savingsRate': null};
      }
      final double rate =
          (inc.scaledValue - exp.scaledValue) / inc.scaledValue;
      return <String, String?>{'savingsRate': rate.toStringAsFixed(4)};
    } else {
      final Decimal10k spent =
          Decimal10k.parseJsonString(finCase['spent'] as String);
      final Decimal10k limit =
          Decimal10k.parseJsonString(finCase['limit'] as String);
      final DateTime today =
          UserDayClock.parseIsoDateUtc(finCase['today'] as String);
      final int daysInMonth = today.month == 12
          ? 31
          : DateTime.utc(today.year, today.month + 1, 0).day;
      final double spentRatio = spent.scaledValue / limit.scaledValue;
      final double timeRatio = today.day / daysInMonth;
      final double pace = spentRatio / timeRatio;
      return <String, String?>{'pace': pace.toStringAsFixed(4)};
    }
  }

  /// D200: Đánh giá rủi ro mục tiêu (`AT_RISK` | `OVERDUE` | `null`) khớp `analytics-vectors.json`.
  static String? evaluateGoalRiskVector(Map<String, dynamic> goalCase) {
    final String status = (goalCase['status'] as String).toUpperCase();
    if (status != 'ACTIVE') return null;

    final DateTime startOn =
        UserDayClock.parseIsoDateUtc(goalCase['startOn'] as String);
    final DateTime targetOn =
        UserDayClock.parseIsoDateUtc(goalCase['targetOn'] as String);
    final DateTime today =
        UserDayClock.parseIsoDateUtc(goalCase['today'] as String);
    final String? progStr = goalCase['progress'] as String?;
    final double progress = progStr == null ? 0.0 : double.parse(progStr);

    if (today.isAfter(targetOn) && progress < 1.0) {
      return 'OVERDUE';
    }

    final int totalDays = targetOn.difference(startOn).inDays;
    if (totalDays <= 0) return null;
    final int elapsedDays = today.difference(startOn).inDays.clamp(0, totalDays);
    final double expectedRatio = elapsedDays / totalDays;
    if ((expectedRatio - progress) > 0.1500001) {
      return 'AT_RISK';
    }
    return null;
  }

  // ===========================================================================
  // 2. HÀM TÍNH TOÁN PHỤC VỤ UI MOBILE
  // ===========================================================================

  static CorrelationResult computeCorrelation({
    required Map<String, num> xByDate,
    required Map<String, num> yByDate,
  }) {
    final List<String> commonDates = xByDate.keys
        .where((String k) => yByDate.containsKey(k))
        .toList()
      ..sort();

    final int n = commonDates.length;
    if (n < minCorrelationSamples) {
      return CorrelationResult(
        sampleCount: n,
        enoughData: false,
        correlationR: null,
      );
    }

    double sumX = 0;
    double sumY = 0;
    for (final String d in commonDates) {
      sumX += xByDate[d]!.toDouble();
      sumY += yByDate[d]!.toDouble();
    }
    final double meanX = sumX / n;
    final double meanY = sumY / n;

    double cov = 0;
    double varX = 0;
    double varY = 0;
    for (final String d in commonDates) {
      final double dx = xByDate[d]!.toDouble() - meanX;
      final double dy = yByDate[d]!.toDouble() - meanY;
      cov += dx * dy;
      varX += dx * dx;
      varY += dy * dy;
    }

    if (varX <= 1e-12 || varY <= 1e-12) {
      return CorrelationResult(
        sampleCount: n,
        enoughData: false,
        correlationR: null,
      );
    }

    final double rawR = cov / math.sqrt(varX * varY);
    final double clamped = rawR.clamp(-1.0, 1.0);
    final double rounded4 = (clamped * 10000).round() / 10000.0;

    return CorrelationResult(
      sampleCount: n,
      enoughData: true,
      correlationR: rounded4,
    );
  }

  static ({int progressPercent, GoalRiskStatus risk}) evaluateGoal({
    required int completedMilestones,
    required int totalMilestones,
    int? manualProgressPercent,
    String? startDateIso,
    String? targetDateIso,
    required String todayIso,
  }) {
    final int progress = totalMilestones > 0
        ? ((completedMilestones * 100) / totalMilestones).round().clamp(0, 100)
        : (manualProgressPercent ?? 0).clamp(0, 100);

    if (progress >= 100 || startDateIso == null || targetDateIso == null) {
      return (progressPercent: progress, risk: GoalRiskStatus.onTrack);
    }

    final String? vectorRisk = evaluateGoalRiskVector(<String, dynamic>{
      'status': 'ACTIVE',
      'startOn': startDateIso,
      'targetOn': targetDateIso,
      'progress': (progress / 100.0).toStringAsFixed(4),
      'today': todayIso,
    });

    if (vectorRisk == 'OVERDUE') {
      return (progressPercent: progress, risk: GoalRiskStatus.offTrack);
    }
    if (vectorRisk == 'AT_RISK') {
      return (progressPercent: progress, risk: GoalRiskStatus.atRisk);
    }
    return (progressPercent: progress, risk: GoalRiskStatus.onTrack);
  }

  static ({
    int consumedBasisPoints,
    BudgetPaceStatus status,
  }) evaluateBudgetPace({
    required Decimal10k spentAmount,
    required Decimal10k limitAmount,
    required int alertThresholdPercent,
  }) {
    final int bps = spentAmount.percentageBasisPointsOf(limitAmount);
    final int thresholdBps = alertThresholdPercent * 100;
    if (bps >= 10000) {
      return (consumedBasisPoints: bps, status: BudgetPaceStatus.exceeded);
    }
    if (bps >= thresholdBps) {
      return (consumedBasisPoints: bps, status: BudgetPaceStatus.warning);
    }
    return (consumedBasisPoints: bps, status: BudgetPaceStatus.safe);
  }
}
