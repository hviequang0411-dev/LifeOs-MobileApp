// LOCAL_ONLY: Màn hình Bánh xe Cuộc sống (Life Wheel Radar) lưu cục bộ trên máy
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:uuid/uuid.dart';

import '../../../../core/theme/app_tokens.dart';
import '../../../../core/widgets/lifeos_visual_charts.dart';
import '../../data/local_extras_repository.dart';

class LifeWheelSheet extends ConsumerStatefulWidget {
  const LifeWheelSheet({super.key});

  @override
  ConsumerState<LifeWheelSheet> createState() => _LifeWheelSheetState();
}

class _LifeWheelSheetState extends ConsumerState<LifeWheelSheet> {
  final List<int> _scores = <int>[7, 8, 7, 6, 8, 7, 6, 8]; // 8 khía cạnh
  final TextEditingController _notesController = TextEditingController();

  static const List<String> _dimensions = <String>[
    'Sự nghiệp',
    'Tài chính',
    'Sức khỏe',
    'Mối quan hệ',
    'Phát triển bản thân',
    'Gia đình',
    'Vui chơi giải trí',
    'Không gian sống',
  ];

  @override
  void dispose() {
    _notesController.dispose();
    super.dispose();
  }

  void _saveAssessment() {
    final LocalExtrasRepository repo = ref.read(localExtrasRepositoryProvider);
    final LocalLifeWheelAssessment assessment = LocalLifeWheelAssessment(
      id: const Uuid().v4(),
      recordedAt: DateTime.now().toUtc(),
      career: _scores[0],
      finance: _scores[1],
      health: _scores[2],
      relationships: _scores[3],
      growth: _scores[4],
      family: _scores[5],
      leisure: _scores[6],
      environment: _scores[7],
      notes: _notesController.text.trim().isEmpty ? null : _notesController.text.trim(),
    );
    repo.recordLifeWheelAssessment(assessment);
    setState(() {});
    ScaffoldMessenger.of(context).showSnackBar(
      const SnackBar(
        content: Text('Đã lưu đánh giá Bánh xe cuộc sống trên thiết bị này.'),
        duration: Duration(seconds: 2),
      ),
    );
  }

  @override
  Widget build(BuildContext context) {
    final LifeOsTokens tokens = LifeOsTokens.of(context);
    final LocalExtrasRepository repo = ref.watch(localExtrasRepositoryProvider);
    final List<LocalLifeWheelAssessment> history = repo.getLifeWheelHistory();

    final double avg = _scores.reduce((int a, int b) => a + b) / 8.0;

    return Container(
      height: MediaQuery.of(context).size.height * 0.88,
      decoration: BoxDecoration(
        color: tokens.bgSurface,
        borderRadius: const BorderRadius.vertical(
          top: Radius.circular(LifeOsTokens.radiusXl),
        ),
      ),
      child: Column(
        children: <Widget>[
          // Header kéo thanh
          Container(
            margin: const EdgeInsets.only(top: 10, bottom: 6),
            width: 40,
            height: 4,
            decoration: BoxDecoration(
              color: tokens.borderSubtle,
              borderRadius: BorderRadius.circular(2),
            ),
          ),
          Padding(
            padding: const EdgeInsets.symmetric(horizontal: LifeOsTokens.space4),
            child: Row(
              mainAxisAlignment: MainAxisAlignment.spaceBetween,
              children: <Widget>[
                Expanded(
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: <Widget>[
                      Text(
                        'Bánh xe cuộc sống',
                        style: TextStyle(
                          fontSize: 18,
                          fontWeight: FontWeight.w700,
                          color: tokens.textPrimary,
                        ),
                      ),
                      Text(
                        'Tự chấm điểm 8 lĩnh vực (Lưu an toàn trên máy)',
                        style: TextStyle(
                          fontSize: 12,
                          color: tokens.textSecondary,
                        ),
                      ),
                    ],
                  ),
                ),
                IconButton(
                  icon: const Icon(Icons.close_rounded),
                  onPressed: () => Navigator.of(context).pop(),
                ),
              ],
            ),
          ),
          const Divider(height: 1),
          Expanded(
            child: ListView(
              padding: const EdgeInsets.all(LifeOsTokens.space4),
              children: <Widget>[
                // Biểu đồ Radar trung tâm
                LifeWheelRadarChart(scores: _scores, size: 240),
                const SizedBox(height: LifeOsTokens.space3),
                Center(
                  child: Container(
                    padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 6),
                    decoration: BoxDecoration(
                      color: tokens.domainGoal.withValues(alpha: 0.15),
                      borderRadius: BorderRadius.circular(LifeOsTokens.radiusPill),
                    ),
                    child: Text(
                      'Điểm cân bằng trung bình: ${avg.toStringAsFixed(1)} / 10',
                      style: TextStyle(
                        fontSize: 13,
                        fontWeight: FontWeight.w700,
                        color: tokens.domainGoal,
                        fontFeatures: const <FontFeature>[
                          FontFeature.tabularFigures(),
                        ],
                      ),
                    ),
                  ),
                ),
                const SizedBox(height: LifeOsTokens.space4),
                // Thanh trượt chấm điểm 8 lĩnh vực
                Text(
                  'Tự đánh giá từng khía cạnh:',
                  style: TextStyle(
                    fontSize: 14,
                    fontWeight: FontWeight.w700,
                    color: tokens.textPrimary,
                  ),
                ),
                const SizedBox(height: LifeOsTokens.space2),
                ...List<Widget>.generate(8, (int index) {
                  return Padding(
                    padding: const EdgeInsets.only(bottom: 8.0),
                    child: Row(
                      children: <Widget>[
                        SizedBox(
                          width: 110,
                          child: Text(
                            _dimensions[index],
                            style: TextStyle(
                              fontSize: 12.5,
                              fontWeight: FontWeight.w500,
                              color: tokens.textPrimary,
                            ),
                          ),
                        ),
                        Expanded(
                          child: Slider(
                            value: _scores[index].toDouble(),
                            min: 1,
                            max: 10,
                            divisions: 9,
                            activeColor: tokens.domainGoal,
                            label: '${_scores[index]}',
                            onChanged: (double val) {
                              setState(() {
                                _scores[index] = val.toInt();
                              });
                            },
                          ),
                        ),
                        SizedBox(
                          width: 24,
                          child: Text(
                            '${_scores[index]}',
                            style: TextStyle(
                              fontSize: 13,
                              fontWeight: FontWeight.w700,
                              color: tokens.domainGoal,
                              fontFeatures: const <FontFeature>[
                                FontFeature.tabularFigures(),
                              ],
                            ),
                          ),
                        ),
                      ],
                    ),
                  );
                }),
                const SizedBox(height: LifeOsTokens.space3),
                TextField(
                  controller: _notesController,
                  decoration: InputDecoration(
                    hintText: 'Ghi chú đúc kết cho lần đánh giá này (tùy chọn)...',
                    filled: true,
                    fillColor: tokens.bgElevated,
                    border: OutlineInputBorder(
                      borderRadius: BorderRadius.circular(LifeOsTokens.radiusMd),
                      borderSide: BorderSide(color: tokens.borderSubtle),
                    ),
                  ),
                  maxLines: 2,
                ),
                const SizedBox(height: LifeOsTokens.space4),
                FilledButton.icon(
                  onPressed: _saveAssessment,
                  style: FilledButton.styleFrom(
                    backgroundColor: tokens.domainGoal,
                    foregroundColor:
                        Theme.of(context).brightness == Brightness.dark
                            ? tokens.bgPrimary
                            : Colors.white,
                    minimumSize: const Size(double.infinity, LifeOsTokens.minTouchTarget),
                    shape: RoundedRectangleBorder(
                      borderRadius: BorderRadius.circular(LifeOsTokens.radiusMd),
                    ),
                  ),
                  icon: const Icon(Icons.bookmark_added_rounded),
                  label: const Text('Lưu đánh giá Bánh xe cuộc sống'),
                ),
                if (history.isNotEmpty) ...<Widget>[
                  const SizedBox(height: LifeOsTokens.space6),
                  Text(
                    'Lịch sử đánh giá (${history.length} lần)',
                    style: TextStyle(
                      fontSize: 14,
                      fontWeight: FontWeight.w700,
                      color: tokens.textPrimary,
                    ),
                  ),
                  const SizedBox(height: LifeOsTokens.space2),
                  ...history.map((LocalLifeWheelAssessment item) {
                    return Container(
                      margin: const EdgeInsets.only(bottom: 8),
                      padding: const EdgeInsets.all(10),
                      decoration: BoxDecoration(
                        color: tokens.bgElevated,
                        borderRadius: BorderRadius.circular(LifeOsTokens.radiusMd),
                        border: Border.all(color: tokens.borderSubtle),
                      ),
                      child: Row(
                        mainAxisAlignment: MainAxisAlignment.spaceBetween,
                        children: <Widget>[
                          Text(
                            '${item.recordedAt.toLocal().day}/${item.recordedAt.toLocal().month}/${item.recordedAt.toLocal().year}',
                            style: TextStyle(
                              fontSize: 12.5,
                              fontWeight: FontWeight.w600,
                              color: tokens.textPrimary,
                            ),
                          ),
                          Text(
                            'Điểm TB: ${item.averageScore.toStringAsFixed(1)} / 10',
                            style: TextStyle(
                              fontSize: 12.5,
                              fontWeight: FontWeight.w700,
                              color: tokens.domainGoal,
                            ),
                          ),
                        ],
                      ),
                    );
                  }),
                ],
              ],
            ),
          ),
        ],
      ),
    );
  }
}

