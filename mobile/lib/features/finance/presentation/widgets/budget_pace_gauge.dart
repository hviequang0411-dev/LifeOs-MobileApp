import 'package:flutter/material.dart';
import '../../../../core/theme/app_tokens.dart';

/// Thước đo tốc độ tiêu dùng Ngân sách (Budget Pace Gauge)
class BudgetPaceGauge extends StatelessWidget {
  const BudgetPaceGauge({
    super.key,
    required this.spentFormatted,
    required this.limitFormatted,
    required this.spentRatio, // 0.0 -> 1.0 (hoặc > 1.0 nếu vượt)
    required this.monthElapsedRatio, // 0.0 -> 1.0 (ví dụ ngày 10/30 là 0.33)
  });

  final String spentFormatted;
  final String limitFormatted;
  final double spentRatio;
  final double monthElapsedRatio;

  bool get isOverPaced => spentRatio > (monthElapsedRatio + 0.08);

  @override
  Widget build(BuildContext context) {
    final LifeOsTokens tokens = LifeOsTokens.of(context);

    final Color paceColor = spentRatio > 1.0
        ? tokens.danger
        : isOverPaced
            ? tokens.warning
            : tokens.success;

    final String paceStatusText = spentRatio > 1.0
        ? 'Vượt hạn mức'
        : isOverPaced
            ? 'Đang chi nhanh hơn tiến độ'
            : 'Tốc độ chi tiêu an toàn';

    return Semantics(
      label: 'Thước đo ngân sách: Đã chi $spentFormatted trên $limitFormatted ($paceStatusText)',
      child: Container(
        padding: const EdgeInsets.all(LifeOsTokens.space4),
        decoration: BoxDecoration(
          color: tokens.bgSurface,
          borderRadius: BorderRadius.circular(LifeOsTokens.radiusLg),
          border: Border.all(color: tokens.borderSubtle),
          boxShadow: LifeOsTokens.shadowElevation1,
        ),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: <Widget>[
            SizedBox(
              width: double.infinity,
              child: Wrap(
                spacing: 8,
                runSpacing: 6,
                alignment: WrapAlignment.spaceBetween,
                crossAxisAlignment: WrapCrossAlignment.center,
                children: <Widget>[
                  Row(
                    mainAxisSize: MainAxisSize.min,
                    children: <Widget>[
                      Icon(
                        Icons.speed_rounded,
                        size: 18,
                        color: paceColor,
                      ),
                      const SizedBox(width: 6),
                      Flexible(
                        child: Text(
                          'Tốc độ tiêu dùng tháng',
                          style: TextStyle(
                            fontSize: 14,
                            fontWeight: FontWeight.w700,
                            color: tokens.textPrimary,
                          ),
                        ),
                      ),
                    ],
                  ),
                  Container(
                    padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 3),
                    decoration: BoxDecoration(
                      color: paceColor.withValues(alpha: 0.12),
                      borderRadius: BorderRadius.circular(LifeOsTokens.radiusPill),
                    ),
                    child: Text(
                      paceStatusText,
                      style: TextStyle(
                        fontSize: 11,
                        fontWeight: FontWeight.w600,
                        color: paceColor,
                      ),
                    ),
                  ),
                ],
              ),
            ),
            const SizedBox(height: LifeOsTokens.space3),
            // Thanh tiến độ kép (vạch chi tiêu + vạch ngày trong tháng)
            Stack(
              alignment: Alignment.centerLeft,
              children: <Widget>[
                // Track nền
                Container(
                  height: 10,
                  width: double.infinity,
                  decoration: BoxDecoration(
                    color: tokens.borderSubtle,
                    borderRadius: BorderRadius.circular(5),
                  ),
                ),
                // Vạch ngày trôi qua trong tháng (điểm mốc so sánh)
                FractionallySizedBox(
                  widthFactor: monthElapsedRatio.clamp(0.0, 1.0),
                  child: Container(
                    height: 10,
                    decoration: BoxDecoration(
                      color: tokens.textSecondary.withValues(alpha: 0.25),
                      borderRadius: BorderRadius.circular(5),
                    ),
                  ),
                ),
                // Vạch chi tiêu thực tế
                FractionallySizedBox(
                  widthFactor: spentRatio.clamp(0.0, 1.0),
                  child: Container(
                    height: 10,
                    decoration: BoxDecoration(
                      color: paceColor,
                      borderRadius: BorderRadius.circular(5),
                    ),
                  ),
                ),
              ],
            ),
            const SizedBox(height: LifeOsTokens.space2),
            SizedBox(
              width: double.infinity,
              child: Wrap(
                spacing: 8,
                runSpacing: 4,
                alignment: WrapAlignment.spaceBetween,
                crossAxisAlignment: WrapCrossAlignment.center,
                children: <Widget>[
                  Text(
                    'Đã chi: $spentFormatted',
                    style: TextStyle(
                      fontSize: 12,
                      fontWeight: FontWeight.w600,
                      color: tokens.textPrimary,
                      fontFeatures: const <FontFeature>[
                        FontFeature.tabularFigures(),
                      ],
                    ),
                  ),
                  Text(
                    'Hạn mức: $limitFormatted',
                    style: TextStyle(
                      fontSize: 12,
                      fontWeight: FontWeight.w500,
                      color: tokens.textSecondary,
                      fontFeatures: const <FontFeature>[
                        FontFeature.tabularFigures(),
                      ],
                    ),
                  ),
                ],
              ),
            ),
          ],
        ),
      ),
    );
  }
}

