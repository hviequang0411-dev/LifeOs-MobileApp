import 'package:flutter/material.dart';

/// Hệ thống Design Tokens chính thức của LifeOS theo **Hướng A — Botanical Premium**
/// (Đã được người dùng duyệt chính thức ngày 2026-11-10 — MOBILE_UI_UPGRADE_PLAN.md v2).
///
/// Đảm bảo độ tương phản WCAG AA (>= 4.5:1 cho chữ thường, >= 3:1 cho thành phần UI),
/// lưới khoảng cách 4dp và mục tiêu chạm tối thiểu 48x48dp.
@immutable
class LifeOsTokens extends ThemeExtension<LifeOsTokens> {
  const LifeOsTokens({
    required this.bgPrimary,
    required this.bgSurface,
    required this.bgElevated,
    required this.textPrimary,
    required this.textSecondary,
    required this.textMuted,
    required this.accentPrimary,
    required this.accentSecondary,
    required this.borderSubtle,
    required this.success,
    required this.warning,
    required this.danger,
    required this.info,
    required this.financeIncome,
    required this.financeExpense,
    required this.financeTransfer,
    required this.domainTask,
    required this.domainHabit,
    required this.domainHabitButtonBg,
    required this.domainHabitButtonFg,
    required this.domainFinance,
    required this.domainCalendar,
    required this.domainKnowledge,
    required this.domainGoal,
    required this.priorityLowFg,
    required this.priorityMediumFg,
    required this.priorityHighFg,
    required this.riskOnTrackFg,
    required this.riskAtRiskFg,
    required this.riskOverdueFg,
  });

  final Color bgPrimary;
  final Color bgSurface;
  final Color bgElevated;
  final Color textPrimary;
  final Color textSecondary;
  final Color textMuted;
  final Color accentPrimary;
  final Color accentSecondary;
  final Color borderSubtle;
  final Color success;
  final Color warning;
  final Color danger;
  final Color info;
  final Color financeIncome;
  final Color financeExpense;
  final Color financeTransfer;

  // Domain Semantic Accents (Bản sắc nhận diện từng module)
  final Color domainTask;
  final Color domainHabit;
  final Color domainHabitButtonBg;
  final Color domainHabitButtonFg;
  final Color domainFinance;
  final Color domainCalendar;
  final Color domainKnowledge;
  final Color domainGoal;

  // Priority Chips Colors
  final Color priorityLowFg;
  final Color priorityMediumFg;
  final Color priorityHighFg;

  // Goal Risk Chips Colors
  final Color riskOnTrackFg;
  final Color riskAtRiskFg;
  final Color riskOverdueFg;

  // Lưới khoảng cách chuẩn 4dp
  static const double space1 = 4.0;
  static const double space2 = 8.0;
  static const double space3 = 12.0;
  static const double space4 = 16.0;
  static const double space5 = 20.0;
  static const double space6 = 24.0;
  static const double space8 = 32.0;
  static const double space10 = 40.0;
  static const double space12 = 48.0;

  // Bo góc chuẩn Hướng A (Botanical Premium)
  static const double radiusXs = 4.0;
  static const double radiusSm = 8.0;
  static const double radiusMd = 12.0;
  static const double radiusLg = 16.0;
  static const double radiusXl = 24.0;
  static const double radiusPill = 999.0;

  // Ngưỡng kích thước vùng chạm tối thiểu theo WCAG AA
  static const double minTouchTarget = 48.0;

  // Ngưỡng responsive (Điện thoại / Tablet / Màn hình rộng)
  static const double phoneMaxBreakpoint = 600.0;
  static const double tabletMaxBreakpoint = 840.0;

  // Token chuyển động (Motion & Animation)
  static const Duration durationFast = Duration(milliseconds: 150);
  static const Duration durationMedium = Duration(milliseconds: 250);
  static const Duration durationSlow = Duration(milliseconds: 400);
  static const Curve curveStandard = Curves.easeInOutCubic;
  static const Curve curveEmphasized = Curves.easeOutBack;

  // Hệ thống đổ bóng 3 lớp (Layered Depth Shadows)
  static const List<BoxShadow> shadowElevation1 = <BoxShadow>[
    BoxShadow(
      offset: Offset(0, 2),
      blurRadius: 6,
      color: Color(0x0A141A17),
    ),
  ];

  static const List<BoxShadow> shadowElevation2 = <BoxShadow>[
    BoxShadow(
      offset: Offset(0, 4),
      blurRadius: 14,
      color: Color(0x12141A17),
    ),
  ];

  static const List<BoxShadow> shadowElevation3 = <BoxShadow>[
    BoxShadow(
      offset: Offset(0, 8),
      blurRadius: 24,
      color: Color(0x18141A17),
    ),
  ];

  /// Bảng màu Chế độ Sáng — Botanical Premium v2 (Đã qua script kiểm thử tương phản)
  static const LifeOsTokens light = LifeOsTokens(
    bgPrimary: Color(0xFFF8F7F3),
    bgSurface: Color(0xFFFFFFFF),
    bgElevated: Color(0xFFF1EFE8),
    textPrimary: Color(0xFF141A17),
    textSecondary: Color(0xFF3D4843),
    textMuted: Color(0xFF505C56),
    accentPrimary: Color(0xFF135E44),
    accentSecondary: Color(0xFFE58E26),
    borderSubtle: Color(0xFFE8E5DD),
    success: Color(0xFF15803D),
    warning: Color(0xFF8A3C01),
    danger: Color(0xFFB91C1C),
    info: Color(0xFF3730A3),
    financeIncome: Color(0xFF15803D),
    financeExpense: Color(0xFFB91C1C),
    financeTransfer: Color(0xFF3730A3),
    domainTask: Color(0xFF135E44),
    domainHabit: Color(0xFF8A3C01),
    domainHabitButtonBg: Color(0xFFE58E26),
    domainHabitButtonFg: Color(0xFF141A17),
    domainFinance: Color(0xFF0F766E),
    domainCalendar: Color(0xFF3730A3),
    domainKnowledge: Color(0xFF6D28D9),
    domainGoal: Color(0xFF7E22CE),
    priorityLowFg: Color(0xFF3D4843),
    priorityMediumFg: Color(0xFF8A3C01),
    priorityHighFg: Color(0xFFB91C1C),
    riskOnTrackFg: Color(0xFF15803D),
    riskAtRiskFg: Color(0xFF8A3C01),
    riskOverdueFg: Color(0xFFB91C1C),
  );

  /// Bảng màu Chế độ Tối — Botanical Premium v2 (Đá phiến ngọc bích sẫm, tương phản cao)
  static const LifeOsTokens dark = LifeOsTokens(
    bgPrimary: Color(0xFF0E1311),
    bgSurface: Color(0xFF161E1A),
    bgElevated: Color(0xFF1F2924),
    textPrimary: Color(0xFFEDF2EE),
    textSecondary: Color(0xFF9BA9A1),
    textMuted: Color(0xFF7E8F87),
    accentPrimary: Color(0xFF34D399),
    accentSecondary: Color(0xFFFBBF24),
    borderSubtle: Color(0xFF232D28),
    success: Color(0xFF34D399),
    warning: Color(0xFFFBBF24),
    danger: Color(0xFFF87171),
    info: Color(0xFF818CF8),
    financeIncome: Color(0xFF34D399),
    financeExpense: Color(0xFFF87171),
    financeTransfer: Color(0xFF818CF8),
    domainTask: Color(0xFF34D399),
    domainHabit: Color(0xFFFBBF24),
    domainHabitButtonBg: Color(0xFFFBBF24),
    domainHabitButtonFg: Color(0xFF161E1A),
    domainFinance: Color(0xFF2DD4BF),
    domainCalendar: Color(0xFF818CF8),
    domainKnowledge: Color(0xFFA78BFA),
    domainGoal: Color(0xFFC084FC),
    priorityLowFg: Color(0xFF9BA9A1),
    priorityMediumFg: Color(0xFFFBBF24),
    priorityHighFg: Color(0xFFF87171),
    riskOnTrackFg: Color(0xFF34D399),
    riskAtRiskFg: Color(0xFFFBBF24),
    riskOverdueFg: Color(0xFFF87171),
  );

  static LifeOsTokens of(BuildContext context) {
    return Theme.of(context).extension<LifeOsTokens>() ?? light;
  }

  @override
  LifeOsTokens copyWith({
    Color? bgPrimary,
    Color? bgSurface,
    Color? bgElevated,
    Color? textPrimary,
    Color? textSecondary,
    Color? textMuted,
    Color? accentPrimary,
    Color? accentSecondary,
    Color? borderSubtle,
    Color? success,
    Color? warning,
    Color? danger,
    Color? info,
    Color? financeIncome,
    Color? financeExpense,
    Color? financeTransfer,
    Color? domainTask,
    Color? domainHabit,
    Color? domainHabitButtonBg,
    Color? domainHabitButtonFg,
    Color? domainFinance,
    Color? domainCalendar,
    Color? domainKnowledge,
    Color? domainGoal,
    Color? priorityLowFg,
    Color? priorityMediumFg,
    Color? priorityHighFg,
    Color? riskOnTrackFg,
    Color? riskAtRiskFg,
    Color? riskOverdueFg,
  }) {
    return LifeOsTokens(
      bgPrimary: bgPrimary ?? this.bgPrimary,
      bgSurface: bgSurface ?? this.bgSurface,
      bgElevated: bgElevated ?? this.bgElevated,
      textPrimary: textPrimary ?? this.textPrimary,
      textSecondary: textSecondary ?? this.textSecondary,
      textMuted: textMuted ?? this.textMuted,
      accentPrimary: accentPrimary ?? this.accentPrimary,
      accentSecondary: accentSecondary ?? this.accentSecondary,
      borderSubtle: borderSubtle ?? this.borderSubtle,
      success: success ?? this.success,
      warning: warning ?? this.warning,
      danger: danger ?? this.danger,
      info: info ?? this.info,
      financeIncome: financeIncome ?? this.financeIncome,
      financeExpense: financeExpense ?? this.financeExpense,
      financeTransfer: financeTransfer ?? this.financeTransfer,
      domainTask: domainTask ?? this.domainTask,
      domainHabit: domainHabit ?? this.domainHabit,
      domainHabitButtonBg: domainHabitButtonBg ?? this.domainHabitButtonBg,
      domainHabitButtonFg: domainHabitButtonFg ?? this.domainHabitButtonFg,
      domainFinance: domainFinance ?? this.domainFinance,
      domainCalendar: domainCalendar ?? this.domainCalendar,
      domainKnowledge: domainKnowledge ?? this.domainKnowledge,
      domainGoal: domainGoal ?? this.domainGoal,
      priorityLowFg: priorityLowFg ?? this.priorityLowFg,
      priorityMediumFg: priorityMediumFg ?? this.priorityMediumFg,
      priorityHighFg: priorityHighFg ?? this.priorityHighFg,
      riskOnTrackFg: riskOnTrackFg ?? this.riskOnTrackFg,
      riskAtRiskFg: riskAtRiskFg ?? this.riskAtRiskFg,
      riskOverdueFg: riskOverdueFg ?? this.riskOverdueFg,
    );
  }

  @override
  LifeOsTokens lerp(ThemeExtension<LifeOsTokens>? other, double t) {
    if (other is! LifeOsTokens) return this;
    return LifeOsTokens(
      bgPrimary: Color.lerp(bgPrimary, other.bgPrimary, t)!,
      bgSurface: Color.lerp(bgSurface, other.bgSurface, t)!,
      bgElevated: Color.lerp(bgElevated, other.bgElevated, t)!,
      textPrimary: Color.lerp(textPrimary, other.textPrimary, t)!,
      textSecondary: Color.lerp(textSecondary, other.textSecondary, t)!,
      textMuted: Color.lerp(textMuted, other.textMuted, t)!,
      accentPrimary: Color.lerp(accentPrimary, other.accentPrimary, t)!,
      accentSecondary: Color.lerp(accentSecondary, other.accentSecondary, t)!,
      borderSubtle: Color.lerp(borderSubtle, other.borderSubtle, t)!,
      success: Color.lerp(success, other.success, t)!,
      warning: Color.lerp(warning, other.warning, t)!,
      danger: Color.lerp(danger, other.danger, t)!,
      info: Color.lerp(info, other.info, t)!,
      financeIncome: Color.lerp(financeIncome, other.financeIncome, t)!,
      financeExpense: Color.lerp(financeExpense, other.financeExpense, t)!,
      financeTransfer: Color.lerp(financeTransfer, other.financeTransfer, t)!,
      domainTask: Color.lerp(domainTask, other.domainTask, t)!,
      domainHabit: Color.lerp(domainHabit, other.domainHabit, t)!,
      domainHabitButtonBg: Color.lerp(domainHabitButtonBg, other.domainHabitButtonBg, t)!,
      domainHabitButtonFg: Color.lerp(domainHabitButtonFg, other.domainHabitButtonFg, t)!,
      domainFinance: Color.lerp(domainFinance, other.domainFinance, t)!,
      domainCalendar: Color.lerp(domainCalendar, other.domainCalendar, t)!,
      domainKnowledge: Color.lerp(domainKnowledge, other.domainKnowledge, t)!,
      domainGoal: Color.lerp(domainGoal, other.domainGoal, t)!,
      priorityLowFg: Color.lerp(priorityLowFg, other.priorityLowFg, t)!,
      priorityMediumFg: Color.lerp(priorityMediumFg, other.priorityMediumFg, t)!,
      priorityHighFg: Color.lerp(priorityHighFg, other.priorityHighFg, t)!,
      riskOnTrackFg: Color.lerp(riskOnTrackFg, other.riskOnTrackFg, t)!,
      riskAtRiskFg: Color.lerp(riskAtRiskFg, other.riskAtRiskFg, t)!,
      riskOverdueFg: Color.lerp(riskOverdueFg, other.riskOverdueFg, t)!,
    );
  }
}
