import 'package:flutter/material.dart';
import '../../../../core/theme/app_tokens.dart';
import '../../../../core/time/user_day_clock.dart';

/// Widget Bản đồ nhiệt thói quen (Habit Contribution Heatmap)
class HabitHeatmapWidget extends StatelessWidget {
  const HabitHeatmapWidget({
    super.key,
    required this.streakDays,
    required this.completionMatrix, // Map<String, int> dateKey ('yyyy-MM-dd') -> count of completed habits
    this.weeks = 8,
    this.referenceDateIso,
  });

  final int streakDays;
  final Map<String, int> completionMatrix;
  final int weeks;
  final String? referenceDateIso;

  @override
  Widget build(BuildContext context) {
    final LifeOsTokens tokens = LifeOsTokens.of(context);
    final DateTime now = referenceDateIso != null
        ? UserDayClock.parseIsoDateUtc(referenceDateIso!)
        : DateTime.now().toUtc();

    return Semantics(
      label: 'Bản đồ nhiệt thói quen: Chuỗi duy trì hiện tại $streakDays ngày',
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
                        Icons.local_fire_department_rounded,
                        size: 18,
                        color: tokens.domainHabitButtonBg,
                      ),
                      const SizedBox(width: 6),
                      Flexible(
                        child: Text(
                          'Bản đồ nhiệt & Chuỗi kiên trì',
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
                    padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 4),
                    decoration: BoxDecoration(
                      color: tokens.domainHabitButtonBg.withValues(alpha: 0.15),
                      borderRadius: BorderRadius.circular(LifeOsTokens.radiusPill),
                    ),
                    child: Text(
                      '🔥 $streakDays ngày liên tục',
                      style: TextStyle(
                        fontSize: 11.5,
                        fontWeight: FontWeight.w700,
                        color: tokens.domainHabit,
                        fontFeatures: const <FontFeature>[
                          FontFeature.tabularFigures(),
                        ],
                      ),
                    ),
                  ),
                ],
              ),
            ),
            const SizedBox(height: LifeOsTokens.space3),
            // Lưới bản đồ nhiệt (7 hàng = Thứ 2 đến Chủ nhật, cột = tuần)
            SingleChildScrollView(
              scrollDirection: Axis.horizontal,
              physics: const BouncingScrollPhysics(),
              child: Row(
                children: List<Widget>.generate(weeks, (int col) {
                  return Padding(
                    padding: const EdgeInsets.only(right: 4.0),
                    child: Column(
                      children: List<Widget>.generate(7, (int row) {
                        // Tính ngày tương ứng
                        final int daysBack = ((weeks - 1 - col) * 7) + (6 - row);
                        final DateTime d = now.subtract(Duration(days: daysBack));
                        final String dateKey =
                            '${d.year}-${d.month.toString().padLeft(2, '0')}-${d.day.toString().padLeft(2, '0')}';
                        final int count = completionMatrix[dateKey] ?? 0;

                        Color cellColor = tokens.borderSubtle.withValues(alpha: 0.4);
                        if (count >= 3) {
                          cellColor = tokens.domainHabitButtonBg;
                        } else if (count == 2) {
                          cellColor = tokens.domainHabitButtonBg.withValues(alpha: 0.7);
                        } else if (count == 1) {
                          cellColor = tokens.domainHabitButtonBg.withValues(alpha: 0.35);
                        }

                        return Container(
                          width: 13,
                          height: 13,
                          margin: const EdgeInsets.only(bottom: 3.0),
                          decoration: BoxDecoration(
                            color: cellColor,
                            borderRadius: BorderRadius.circular(3),
                          ),
                        );
                      }),
                    ),
                  );
                }),
              ),
            ),
            const SizedBox(height: 6),
            Row(
              mainAxisAlignment: MainAxisAlignment.spaceBetween,
              children: <Widget>[
                Text(
                  '${weeks * 7} ngày qua',
                  style: TextStyle(
                    fontSize: 10.5,
                    fontWeight: FontWeight.w500,
                    color: tokens.textSecondary,
                  ),
                ),
                Row(
                  children: <Widget>[
                    Text(
                      'Ít',
                      style: TextStyle(
                        fontSize: 10,
                        color: tokens.textMuted,
                      ),
                    ),
                    const SizedBox(width: 4),
                    _buildLegendSquare(tokens.borderSubtle.withValues(alpha: 0.4)),
                    _buildLegendSquare(tokens.domainHabitButtonBg.withValues(alpha: 0.35)),
                    _buildLegendSquare(tokens.domainHabitButtonBg.withValues(alpha: 0.7)),
                    _buildLegendSquare(tokens.domainHabitButtonBg),
                    const SizedBox(width: 4),
                    Text(
                      'Nhiều',
                      style: TextStyle(
                        fontSize: 10,
                        color: tokens.textMuted,
                      ),
                    ),
                  ],
                ),
              ],
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildLegendSquare(Color color) {
    return Container(
      width: 9,
      height: 9,
      margin: const EdgeInsets.symmetric(horizontal: 1.5),
      decoration: BoxDecoration(
        color: color,
        borderRadius: BorderRadius.circular(2),
      ),
    );
  }
}

