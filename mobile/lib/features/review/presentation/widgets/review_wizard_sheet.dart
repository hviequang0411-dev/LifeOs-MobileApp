import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../../core/database/app_database.dart';
import '../../../../core/providers/app_providers.dart';
import '../../../../core/sync/entity_types.dart';
import '../../../../core/theme/app_tokens.dart';
import '../../../workspace/data/lifeos_local_repository.dart';

class ReviewWizardSheet extends ConsumerStatefulWidget {
  const ReviewWizardSheet({super.key});

  @override
  ConsumerState<ReviewWizardSheet> createState() => _ReviewWizardSheetState();
}

class _ReviewWizardSheetState extends ConsumerState<ReviewWizardSheet> {
  int _step = 0; // 0: Nhìn lại số liệu, 1: Tâm trạng, 2: Bài học đúc kết
  int _moodScore = 4; // 1..5
  final TextEditingController _reflectionController = TextEditingController();

  static const List<(int, String, String)> _moodLevels = <(int, String, String)>[
    (1, 'Kiệt sức', 'Cần nghỉ ngơi sâu và nạp lại năng lượng'),
    (2, 'Căng thẳng', 'Nhiều áp lực dồn nén, cần giảm bớt việc'),
    (3, 'Bình ổn', 'Một ngày trôi qua cân bằng, êm ả'),
    (4, 'Tích cực', 'Cảm giác hoàn thành tốt và nhiều năng lượng'),
    (5, 'Thăng hoa', 'Trạng thái dòng chảy xuất sắc, rất hứng khởi'),
  ];

  @override
  void dispose() {
    _reflectionController.dispose();
    super.dispose();
  }

  void _saveReview() {
    final LifeOsLocalRepository repo = ref.read(localRepositoryProvider);
    final String userId = ref.read(currentUserIdProvider);
    final DateTime now = DateTime.now();
    final String periodKey =
        '${now.year}-${now.month.toString().padLeft(2, '0')}-${now.day.toString().padLeft(2, '0')}';

    repo.saveLifeReview(
      userId: userId,
      periodType: 'DAILY',
      periodKey: periodKey,
      mood: _moodScore,
      reflection: _reflectionController.text.trim(),
    );

    Navigator.of(context).pop();
    ScaffoldMessenger.of(context).showSnackBar(
      const SnackBar(
        content: Text('✨ Đã lưu phản tư và chỉ số tâm trạng hôm nay vào hồ sơ của bạn.'),
        duration: Duration(seconds: 3),
      ),
    );
  }

  @override
  Widget build(BuildContext context) {
    final LifeOsTokens tokens = LifeOsTokens.of(context);
    final List<LocalEntityRow> tasks =
        ref.watch(entitiesByTypeStreamProvider(SyncEntityType.task)).valueOrNull ??
            const <LocalEntityRow>[];
    final int doneCount = tasks
        .where((LocalEntityRow t) =>
            (t.payload['status'] ?? '').toString().toUpperCase() == 'DONE')
        .length;

    return Container(
      height: MediaQuery.of(context).size.height * 0.82,
      decoration: BoxDecoration(
        color: tokens.bgSurface,
        borderRadius: const BorderRadius.vertical(
          top: Radius.circular(LifeOsTokens.radiusXl),
        ),
      ),
      child: Column(
        children: <Widget>[
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
                      'Tổng kết & Phản tư ngày',
                      style: TextStyle(
                        fontSize: 18,
                        fontWeight: FontWeight.w700,
                        color: tokens.textPrimary,
                      ),
                    ),
                    Text(
                      'Bước ${_step + 1} / 3: ${_step == 0 ? "Nhìn lại con số" : _step == 1 ? "Chỉ số tâm trạng" : "Đúc kết bài học"}',
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
            child: Padding(
              padding: const EdgeInsets.all(LifeOsTokens.space4),
              child: _step == 2
                  ? _buildStepReflection(context)
                  : SingleChildScrollView(
                      child: _step == 0
                          ? _buildStepNumbers(context, doneCount, tasks.length)
                          : _buildStepMood(context),
                    ),
            ),
          ),
          // Footer navigation
          Container(
            padding: const EdgeInsets.all(LifeOsTokens.space4),
            decoration: BoxDecoration(
              border: Border(top: BorderSide(color: tokens.borderSubtle)),
            ),
            child: Row(
              mainAxisAlignment: MainAxisAlignment.spaceBetween,
              children: <Widget>[
                if (_step > 0)
                  OutlinedButton(
                    onPressed: () {
                      setState(() {
                        _step--;
                      });
                    },
                    style: OutlinedButton.styleFrom(
                      foregroundColor: tokens.textSecondary,
                      minimumSize: const Size(100, LifeOsTokens.minTouchTarget),
                    ),
                    child: const Text('Quay lại'),
                  )
                else
                  const SizedBox(width: 100),
                FilledButton(
                  onPressed: () {
                    if (_step < 2) {
                      setState(() {
                        _step++;
                      });
                    } else {
                      _saveReview();
                    }
                  },
                  style: FilledButton.styleFrom(
                    backgroundColor: tokens.domainTask,
                    foregroundColor:
                        Theme.of(context).brightness == Brightness.dark
                            ? tokens.bgPrimary
                            : Colors.white,
                    minimumSize: const Size(130, LifeOsTokens.minTouchTarget),
                    shape: RoundedRectangleBorder(
                      borderRadius: BorderRadius.circular(LifeOsTokens.radiusMd),
                    ),
                  ),
                  child: Text(_step == 2 ? 'Hoàn tất' : 'Tiếp theo'),
                ),
              ],
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildStepNumbers(BuildContext context, int doneCount, int totalCount) {
    final LifeOsTokens tokens = LifeOsTokens.of(context);
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: <Widget>[
        Text(
          'Hôm nay bạn đã làm được những gì?',
          style: TextStyle(
            fontSize: 15,
            fontWeight: FontWeight.w700,
            color: tokens.textPrimary,
          ),
        ),
        const SizedBox(height: LifeOsTokens.space3),
        Container(
          padding: const EdgeInsets.all(LifeOsTokens.space4),
          decoration: BoxDecoration(
            color: tokens.bgElevated,
            borderRadius: BorderRadius.circular(LifeOsTokens.radiusLg),
            border: Border.all(color: tokens.borderSubtle),
          ),
          child: Row(
            children: <Widget>[
              Icon(Icons.check_circle_rounded, size: 36, color: tokens.domainTask),
              const SizedBox(width: 16),
              Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: <Widget>[
                    Text(
                      '$doneCount / $totalCount công việc hoàn tất',
                      style: TextStyle(
                        fontSize: 15,
                        fontWeight: FontWeight.w700,
                        color: tokens.textPrimary,
                      ),
                    ),
                    const SizedBox(height: 2),
                    Text(
                      'Mỗi bước nhỏ đều đưa bạn đến gần mục tiêu dài hạn hơn.',
                      style: TextStyle(
                        fontSize: 12,
                        color: tokens.textSecondary,
                      ),
                    ),
                  ],
                ),
              ),
            ],
          ),
        ),
      ],
    );
  }

  Widget _buildStepMood(BuildContext context) {
    final LifeOsTokens tokens = LifeOsTokens.of(context);
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: <Widget>[
        Text(
          'Bạn cảm thấy thế nào về ngày hôm nay?',
          style: TextStyle(
            fontSize: 15,
            fontWeight: FontWeight.w700,
            color: tokens.textPrimary,
          ),
        ),
        const SizedBox(height: LifeOsTokens.space3),
        ..._moodLevels.map(((int, String, String) item) {
          final bool isSelected = _moodScore == item.$1;
          return Padding(
            padding: const EdgeInsets.only(bottom: 8.0),
            child: InkWell(
              borderRadius: BorderRadius.circular(LifeOsTokens.radiusMd),
              onTap: () {
                setState(() {
                  _moodScore = item.$1;
                });
              },
              child: Container(
                padding: const EdgeInsets.all(12),
                decoration: BoxDecoration(
                  color: isSelected ? tokens.domainHabitButtonBg.withValues(alpha: 0.15) : tokens.bgElevated,
                  borderRadius: BorderRadius.circular(LifeOsTokens.radiusMd),
                  border: Border.all(
                    color: isSelected ? tokens.domainHabitButtonBg : tokens.borderSubtle,
                    width: isSelected ? 1.5 : 1.0,
                  ),
                ),
                child: Row(
                  children: <Widget>[
                    Text(
                      '${item.$1} ⭐',
                      style: TextStyle(
                        fontSize: 15,
                        fontWeight: FontWeight.w700,
                        color: isSelected ? tokens.domainHabit : tokens.textSecondary,
                      ),
                    ),
                    const SizedBox(width: 12),
                    Expanded(
                      child: Column(
                        crossAxisAlignment: CrossAxisAlignment.start,
                        children: <Widget>[
                          Text(
                            item.$2,
                            style: TextStyle(
                              fontSize: 13.5,
                              fontWeight: FontWeight.w600,
                              color: tokens.textPrimary,
                            ),
                          ),
                          Text(
                            item.$3,
                            style: TextStyle(
                              fontSize: 11.5,
                              color: tokens.textSecondary,
                            ),
                          ),
                        ],
                      ),
                    ),
                  ],
                ),
              ),
            ),
          );
        }),
      ],
    );
  }

  Widget _buildStepReflection(BuildContext context) {
    final LifeOsTokens tokens = LifeOsTokens.of(context);
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: <Widget>[
        Text(
          'Đúc kết & Bài học hôm nay',
          style: TextStyle(
            fontSize: 15,
            fontWeight: FontWeight.w700,
            color: tokens.textPrimary,
          ),
        ),
        const SizedBox(height: 4),
        Text(
          'Một điều bạn biết ơn hoặc điều có thể làm tốt hơn vào ngày mai:',
          style: TextStyle(
            fontSize: 12,
            color: tokens.textSecondary,
          ),
        ),
        const SizedBox(height: LifeOsTokens.space3),
        Expanded(
          child: TextField(
            controller: _reflectionController,
            maxLines: null,
            expands: true,
            textAlignVertical: TextAlignVertical.top,
            decoration: InputDecoration(
              hintText: 'Hôm nay tôi đã nhận ra rằng...',
              filled: true,
              fillColor: tokens.bgElevated,
              border: OutlineInputBorder(
                borderRadius: BorderRadius.circular(LifeOsTokens.radiusMd),
                borderSide: BorderSide(color: tokens.borderSubtle),
              ),
            ),
          ),
        ),
      ],
    );
  }
}

