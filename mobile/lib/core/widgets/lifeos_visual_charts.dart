import 'dart:math' as math;
import 'package:flutter/material.dart';
import '../theme/app_tokens.dart';

/// Dữ liệu một lát cắt của biểu đồ vành khuyên (Donut Chart)
class DonutSliceData {
  const DonutSliceData({
    required this.label,
    required this.amount,
    required this.color,
    this.percentage = 0.0,
  });

  final String label;
  final int amount; // đơn vị tiền (Decimal10k int hoặc VND nguyên)
  final Color color;
  final double percentage; // 0.0 -> 1.0
}

/// Biểu đồ Vành khuyên (Donut Chart) vẽ bằng CustomPainter thuần túy
class LifeOsDonutChart extends StatelessWidget {
  const LifeOsDonutChart({
    super.key,
    required this.slices,
    required this.centerTitle,
    required this.centerSubtitle,
    this.size = 180.0,
    this.strokeWidth = 24.0,
  });

  final List<DonutSliceData> slices;
  final String centerTitle;
  final String centerSubtitle;
  final double size;
  final double strokeWidth;

  @override
  Widget build(BuildContext context) {
    final LifeOsTokens tokens = LifeOsTokens.of(context);

    return Semantics(
      label: 'Biểu đồ phân bổ chi tiêu: $centerSubtitle $centerTitle',
      child: Column(
        children: <Widget>[
          SizedBox(
            width: size,
            height: size,
            child: Stack(
              alignment: Alignment.center,
              children: <Widget>[
                CustomPaint(
                  size: Size(size, size),
                  painter: _DonutPainter(
                    slices: slices,
                    strokeWidth: strokeWidth,
                    emptyColor: tokens.borderSubtle,
                  ),
                ),
                SizedBox(
                  width: math.max(40, size - strokeWidth * 2 - 16),
                  height: math.max(40, size - strokeWidth * 2 - 16),
                  child: FittedBox(
                    fit: BoxFit.scaleDown,
                    child: Column(
                      mainAxisSize: MainAxisSize.min,
                      children: <Widget>[
                        Text(
                          centerSubtitle,
                          style: TextStyle(
                            fontSize: 11,
                            fontWeight: FontWeight.w500,
                            color: tokens.textSecondary,
                          ),
                        ),
                        const SizedBox(height: 2),
                        Text(
                          centerTitle,
                          style: TextStyle(
                            fontSize: 15,
                            fontWeight: FontWeight.w700,
                            color: tokens.textPrimary,
                            fontFeatures: const <FontFeature>[
                              FontFeature.tabularFigures(),
                            ],
                          ),
                        ),
                      ],
                    ),
                  ),
                ),
              ],
            ),
          ),
          const SizedBox(height: LifeOsTokens.space4),
          // Chú giải danh mục
          Wrap(
            spacing: LifeOsTokens.space3,
            runSpacing: LifeOsTokens.space2,
            alignment: WrapAlignment.center,
            children: slices.map((DonutSliceData slice) {
              return Row(
                mainAxisSize: MainAxisSize.min,
                children: <Widget>[
                  Container(
                    width: 10,
                    height: 10,
                    decoration: BoxDecoration(
                      color: slice.color,
                      shape: BoxShape.circle,
                    ),
                  ),
                  const SizedBox(width: 6),
                  Flexible(
                    child: Text(
                      '${slice.label} (${(slice.percentage * 100).toStringAsFixed(0)}%)',
                      style: TextStyle(
                        fontSize: 12,
                        fontWeight: FontWeight.w500,
                        color: tokens.textSecondary,
                      ),
                    ),
                  ),
                ],
              );
            }).toList(),
          ),
        ],
      ),
    );
  }
}

class _DonutPainter extends CustomPainter {
  _DonutPainter({
    required this.slices,
    required this.strokeWidth,
    required this.emptyColor,
  });

  final List<DonutSliceData> slices;
  final double strokeWidth;
  final Color emptyColor;

  @override
  void paint(Canvas canvas, Size size) {
    final Offset center = Offset(size.width / 2, size.height / 2);
    final double radius = (size.width - strokeWidth) / 2;
    final Rect rect = Rect.fromCircle(center: center, radius: radius);

    final Paint emptyPaint = Paint()
      ..color = emptyColor
      ..style = PaintingStyle.stroke
      ..strokeWidth = strokeWidth;

    if (slices.isEmpty) {
      canvas.drawCircle(center, radius, emptyPaint);
      return;
    }

    double startAngle = -math.pi / 2;
    const double gapAngle = 0.04; // Khoảng hở tinh tế giữa các lát

    for (final DonutSliceData slice in slices) {
      final double sweepAngle = (slice.percentage * 2 * math.pi) - gapAngle;
      if (sweepAngle <= 0) continue;

      final Paint paint = Paint()
        ..color = slice.color
        ..style = PaintingStyle.stroke
        ..strokeCap = StrokeCap.round
        ..strokeWidth = strokeWidth;

      canvas.drawArc(rect, startAngle + (gapAngle / 2), sweepAngle, false, paint);
      startAngle += slice.percentage * 2 * math.pi;
    }
  }

  @override
  bool shouldRepaint(covariant _DonutPainter oldDelegate) => true;
}

/// Biểu đồ Radar 8 trục cho Bánh xe Cuộc sống (Life Wheel Radar)
class LifeWheelRadarChart extends StatelessWidget {
  const LifeWheelRadarChart({
    super.key,
    required this.scores,
    this.size = 260.0,
  });

  // 8 điểm theo thứ tự: Sự nghiệp, Tài chính, Sức khỏe, Mối quan hệ, Phát triển, Gia đình, Vui chơi, Không gian sống
  final List<int> scores;
  final double size;

  static const List<String> axisLabels = <String>[
    'Sự nghiệp',
    'Tài chính',
    'Sức khỏe',
    'Quan hệ',
    'Phát triển',
    'Gia đình',
    'Giải trí',
    'Không gian',
  ];

  @override
  Widget build(BuildContext context) {
    final LifeOsTokens tokens = LifeOsTokens.of(context);

    return Semantics(
      label: 'Biểu đồ radar Bánh xe cuộc sống 8 khía cạnh',
      child: Center(
        child: SizedBox(
          width: size,
          height: size,
          child: CustomPaint(
            size: Size(size, size),
            painter: _RadarChartPainter(
              scores: scores,
              labels: axisLabels,
              gridColor: tokens.borderSubtle,
              fillColor: tokens.domainGoal.withValues(alpha: 0.25),
              lineColor: tokens.domainGoal,
              labelColor: tokens.textSecondary,
            ),
          ),
        ),
      ),
    );
  }
}

class _RadarChartPainter extends CustomPainter {
  _RadarChartPainter({
    required this.scores,
    required this.labels,
    required this.gridColor,
    required this.fillColor,
    required this.lineColor,
    required this.labelColor,
  });

  final List<int> scores;
  final List<String> labels;
  final Color gridColor;
  final Color fillColor;
  final Color lineColor;
  final Color labelColor;

  @override
  void paint(Canvas canvas, Size size) {
    final Offset center = Offset(size.width / 2, size.height / 2);
    final double maxRadius = (size.width / 2) - 32.0;
    const int count = 8;
    const double angleStep = (2 * math.pi) / count;

    final Paint gridPaint = Paint()
      ..color = gridColor
      ..style = PaintingStyle.stroke
      ..strokeWidth = 1.0;

    // Vẽ 3 vòng mạng lưới đồng tâm (điểm 3, 7, 10)
    for (int step = 1; step <= 3; step++) {
      final double r = (maxRadius / 3) * step;
      final Path gridPath = Path();
      for (int i = 0; i < count; i++) {
        final double angle = -math.pi / 2 + (i * angleStep);
        final double x = center.dx + r * math.cos(angle);
        final double y = center.dy + r * math.sin(angle);
        if (i == 0) {
          gridPath.moveTo(x, y);
        } else {
          gridPath.lineTo(x, y);
        }
      }
      gridPath.close();
      canvas.drawPath(gridPath, gridPaint);
    }

    // Vẽ các trục nan hoa
    for (int i = 0; i < count; i++) {
      final double angle = -math.pi / 2 + (i * angleStep);
      final double x = center.dx + maxRadius * math.cos(angle);
      final double y = center.dy + maxRadius * math.sin(angle);
      canvas.drawLine(center, Offset(x, y), gridPaint);

      // Vẽ nhãn văn bản quanh trục
      final String label = labels[i];
      final double labelX = center.dx + (maxRadius + 18) * math.cos(angle);
      final double labelY = center.dy + (maxRadius + 18) * math.sin(angle);

      final TextSpan span = TextSpan(
        text: label,
        style: TextStyle(
          fontSize: 9.5,
          fontWeight: FontWeight.w600,
          color: labelColor,
        ),
      );
      final TextPainter tp = TextPainter(
        text: span,
        textDirection: TextDirection.ltr,
      )..layout();
      tp.paint(canvas, Offset(labelX - (tp.width / 2), labelY - (tp.height / 2)));
    }

    // Vẽ đa giác dữ liệu của người dùng
    if (scores.length == count) {
      final Path dataPath = Path();
      final Paint fillPaint = Paint()
        ..color = fillColor
        ..style = PaintingStyle.fill;
      final Paint strokePaint = Paint()
        ..color = lineColor
        ..style = PaintingStyle.stroke
        ..strokeWidth = 2.5
        ..strokeCap = StrokeCap.round;

      for (int i = 0; i < count; i++) {
        final double val = scores[i].clamp(1, 10) / 10.0;
        final double r = maxRadius * val;
        final double angle = -math.pi / 2 + (i * angleStep);
        final double x = center.dx + r * math.cos(angle);
        final double y = center.dy + r * math.sin(angle);

        if (i == 0) {
          dataPath.moveTo(x, y);
        } else {
          dataPath.lineTo(x, y);
        }
      }
      dataPath.close();
      canvas.drawPath(dataPath, fillPaint);
      canvas.drawPath(dataPath, strokePaint);

      // Vẽ các chấm điểm tròn ở đỉnh
      final Paint dotPaint = Paint()..color = lineColor;
      for (int i = 0; i < count; i++) {
        final double val = scores[i].clamp(1, 10) / 10.0;
        final double r = maxRadius * val;
        final double angle = -math.pi / 2 + (i * angleStep);
        final double x = center.dx + r * math.cos(angle);
        final double y = center.dy + r * math.sin(angle);
        canvas.drawCircle(Offset(x, y), 3.5, dotPaint);
      }
    }
  }

  @override
  bool shouldRepaint(covariant _RadarChartPainter oldDelegate) => true;
}

