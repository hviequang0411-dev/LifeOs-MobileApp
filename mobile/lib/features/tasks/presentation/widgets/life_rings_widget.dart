import 'dart:math' as math;
import 'package:flutter/material.dart';
import '../../../../core/theme/app_tokens.dart';

/// Widget Vòng đời hôm nay (Life Rings) hiển thị 3 vòng tiến độ đồng tâm
class LifeRingsWidget extends StatelessWidget {
  const LifeRingsWidget({
    super.key,
    required this.taskDone,
    required this.taskTotal,
    required this.habitDone,
    required this.habitTotal,
    required this.budgetSpentVnd,
    required this.budgetLimitVnd,
  });

  final int taskDone;
  final int taskTotal;
  final int habitDone;
  final int habitTotal;
  final int budgetSpentVnd;
  final int budgetLimitVnd;

  double get taskProgress => taskTotal > 0 ? (taskDone / taskTotal).clamp(0.0, 1.0) : 0.0;
  double get habitProgress => habitTotal > 0 ? (habitDone / habitTotal).clamp(0.0, 1.0) : 0.0;
  double get budgetProgress =>
      budgetLimitVnd > 0 ? (budgetSpentVnd / budgetLimitVnd).clamp(0.0, 1.0) : 0.0;

  @override
  Widget build(BuildContext context) {
    final LifeOsTokens tokens = LifeOsTokens.of(context);

    final double overallScore =
        ((taskProgress * 0.4) + (habitProgress * 0.4) + ((1.0 - budgetProgress).clamp(0.0, 1.0) * 0.2)) *
            100;

    return Semantics(
      label: 'Vòng đời hôm nay: Hoàn thành $taskDone trên $taskTotal việc, $habitDone trên $habitTotal thói quen',
      child: Container(
        padding: const EdgeInsets.all(LifeOsTokens.space4),
        decoration: BoxDecoration(
          color: tokens.bgSurface,
          borderRadius: BorderRadius.circular(LifeOsTokens.radiusLg),
          border: Border.all(color: tokens.borderSubtle),
          boxShadow: LifeOsTokens.shadowElevation1,
        ),
        child: Row(
          children: <Widget>[
            // 3 vòng tròn tiến độ đồng tâm
            SizedBox(
              width: 104,
              height: 104,
              child: Stack(
                alignment: Alignment.center,
                children: <Widget>[
                  CustomPaint(
                    size: const Size(104, 104),
                    painter: _LifeRingsPainter(
                      taskProgress: taskProgress,
                      habitProgress: habitProgress,
                      budgetProgress: budgetProgress,
                      taskColor: tokens.domainTask,
                      habitColor: tokens.domainHabitButtonBg,
                      budgetColor: tokens.domainFinance,
                      trackColor: tokens.borderSubtle.withValues(alpha: 0.5),
                    ),
                  ),
                  Column(
                    mainAxisSize: MainAxisSize.min,
                    children: <Widget>[
                      Text(
                        '${overallScore.toStringAsFixed(0)}%',
                        style: TextStyle(
                          fontSize: 16,
                          fontWeight: FontWeight.w700,
                          color: tokens.textPrimary,
                          fontFeatures: const <FontFeature>[
                            FontFeature.tabularFigures(),
                          ],
                        ),
                      ),
                      Text(
                        'Nhịp ngày',
                        style: TextStyle(
                          fontSize: 9.5,
                          fontWeight: FontWeight.w500,
                          color: tokens.textSecondary,
                        ),
                      ),
                    ],
                  ),
                ],
              ),
            ),
            const SizedBox(width: LifeOsTokens.space4),
            // Chi tiết 3 vòng
            Expanded(
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: <Widget>[
                  _buildRingItem(
                    context,
                    color: tokens.domainTask,
                    icon: Icons.task_alt_rounded,
                    label: 'Công việc',
                    value: '$taskDone/$taskTotal xong (${(taskProgress * 100).toStringAsFixed(0)}%)',
                  ),
                  const SizedBox(height: LifeOsTokens.space2),
                  _buildRingItem(
                    context,
                    color: tokens.domainHabitButtonBg,
                    icon: Icons.local_fire_department_rounded,
                    label: 'Thói quen',
                    value: '$habitDone/$habitTotal điểm danh',
                  ),
                  const SizedBox(height: LifeOsTokens.space2),
                  _buildRingItem(
                    context,
                    color: tokens.domainFinance,
                    icon: Icons.account_balance_wallet_rounded,
                    label: 'Ngân sách',
                    value: budgetLimitVnd > 0
                        ? '${(budgetProgress * 100).toStringAsFixed(0)}% hạn mức ngày'
                        : 'Chưa đặt hạn mức',
                  ),
                ],
              ),
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildRingItem(
    BuildContext context, {
    required Color color,
    required IconData icon,
    required String label,
    required String value,
  }) {
    final LifeOsTokens tokens = LifeOsTokens.of(context);
    return Row(
      children: <Widget>[
        Container(
          width: 8,
          height: 8,
          decoration: BoxDecoration(
            color: color,
            shape: BoxShape.circle,
          ),
        ),
        const SizedBox(width: 6),
        Icon(icon, size: 13, color: color),
        const SizedBox(width: 4),
        Expanded(
          child: Text(
            '$label: $value',
            style: TextStyle(
              fontSize: 11.5,
              fontWeight: FontWeight.w500,
              color: tokens.textPrimary,
              fontFeatures: const <FontFeature>[
                FontFeature.tabularFigures(),
              ],
            ),
            overflow: TextOverflow.ellipsis,
          ),
        ),
      ],
    );
  }
}

class _LifeRingsPainter extends CustomPainter {
  _LifeRingsPainter({
    required this.taskProgress,
    required this.habitProgress,
    required this.budgetProgress,
    required this.taskColor,
    required this.habitColor,
    required this.budgetColor,
    required this.trackColor,
  });

  final double taskProgress;
  final double habitProgress;
  final double budgetProgress;
  final Color taskColor;
  final Color habitColor;
  final Color budgetColor;
  final Color trackColor;

  @override
  void paint(Canvas canvas, Size size) {
    final Offset center = Offset(size.width / 2, size.height / 2);
    const double strokeWidth = 6.5;

    // Vòng ngoài cùng: Task (r = 46)
    _drawRing(
      canvas: canvas,
      center: center,
      radius: 46.0,
      progress: taskProgress,
      color: taskColor,
      strokeWidth: strokeWidth,
    );

    // Vòng giữa: Habit (r = 46 - 6.5 - 4 = 35.5)
    _drawRing(
      canvas: canvas,
      center: center,
      radius: 35.5,
      progress: habitProgress,
      color: habitColor,
      strokeWidth: strokeWidth,
    );

    // Vòng trong cùng: Budget (r = 35.5 - 6.5 - 4 = 25.0)
    _drawRing(
      canvas: canvas,
      center: center,
      radius: 25.0,
      progress: budgetProgress,
      color: budgetColor,
      strokeWidth: strokeWidth,
    );
  }

  void _drawRing({
    required Canvas canvas,
    required Offset center,
    required double radius,
    required double progress,
    required Color color,
    required double strokeWidth,
  }) {
    final Rect rect = Rect.fromCircle(center: center, radius: radius);

    // Vẽ track nền mờ
    final Paint trackPaint = Paint()
      ..color = trackColor
      ..style = PaintingStyle.stroke
      ..strokeWidth = strokeWidth;
    canvas.drawCircle(center, radius, trackPaint);

    if (progress <= 0) return;

    // Vẽ tiến độ có bo tròn đầu
    final Paint progressPaint = Paint()
      ..color = color
      ..style = PaintingStyle.stroke
      ..strokeCap = StrokeCap.round
      ..strokeWidth = strokeWidth;

    const double startAngle = -math.pi / 2;
    final double sweepAngle = 2 * math.pi * progress.clamp(0.0, 1.0);
    canvas.drawArc(rect, startAngle, sweepAngle, false, progressPaint);
  }

  @override
  bool shouldRepaint(covariant _LifeRingsPainter oldDelegate) => true;
}

