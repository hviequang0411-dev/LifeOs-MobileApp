// LOCAL_ONLY: Màn hình Khu vườn Tập trung (Focus Garden / Pomodoro) lưu cục bộ trên máy
import 'dart:async';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:uuid/uuid.dart';

import '../../../../core/theme/app_tokens.dart';
import '../../data/local_extras_repository.dart';

class FocusGardenSheet extends ConsumerStatefulWidget {
  const FocusGardenSheet({super.key});

  @override
  ConsumerState<FocusGardenSheet> createState() => _FocusGardenSheetState();
}

class _FocusGardenSheetState extends ConsumerState<FocusGardenSheet> {
  static const int _defaultMinutes = 25;
  int _remainingSeconds = _defaultMinutes * 60;
  bool _isRunning = false;
  Timer? _timer;

  @override
  void dispose() {
    _timer?.cancel();
    super.dispose();
  }

  void _toggleTimer() {
    if (_isRunning) {
      _timer?.cancel();
      setState(() {
        _isRunning = false;
      });
    } else {
      setState(() {
        _isRunning = true;
      });
      _timer = Timer.periodic(const Duration(seconds: 1), (Timer timer) {
        if (_remainingSeconds > 0) {
          setState(() {
            _remainingSeconds--;
          });
        } else {
          timer.cancel();
          _onSessionCompleted();
        }
      });
    }
  }

  void _resetTimer() {
    _timer?.cancel();
    setState(() {
      _isRunning = false;
      _remainingSeconds = _defaultMinutes * 60;
    });
  }

  void _onSessionCompleted() {
    final LocalExtrasRepository repo = ref.read(localExtrasRepositoryProvider);
    final LocalFocusSession session = LocalFocusSession(
      id: const Uuid().v4(),
      startedAt: DateTime.now().subtract(const Duration(minutes: _defaultMinutes)).toUtc(),
      durationMinutes: _defaultMinutes,
      completed: true,
      tag: 'Tập trung sâu',
    );
    repo.recordFocusSession(session);
    setState(() {
      _isRunning = false;
      _remainingSeconds = _defaultMinutes * 60;
    });
    ScaffoldMessenger.of(context).showSnackBar(
      const SnackBar(
        content: Text('🎉 Xuất sắc! Bạn vừa hoàn thành một phiên tập trung và trồng thêm 1 cái cây!'),
        duration: Duration(seconds: 3),
      ),
    );
  }

  @override
  Widget build(BuildContext context) {
    final LifeOsTokens tokens = LifeOsTokens.of(context);
    final LocalExtrasRepository repo = ref.watch(localExtrasRepositoryProvider);
    final int treeCount = repo.getPlantedTreesCount();
    final int totalMinutes = repo.getTotalFocusMinutes();

    final int minutes = _remainingSeconds ~/ 60;
    final int seconds = _remainingSeconds % 60;
    final String timeDisplay =
        '${minutes.toString().padLeft(2, '0')}:${seconds.toString().padLeft(2, '0')}';
    final double progress = 1.0 - (_remainingSeconds / (_defaultMinutes * 60));

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
                      'Khu vườn tập trung',
                      style: TextStyle(
                        fontSize: 18,
                        fontWeight: FontWeight.w700,
                        color: tokens.textPrimary,
                      ),
                    ),
                    Text(
                      'Hoàn thành phiên làm việc để gieo mầm cây ảo',
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
                // Đồng hồ bấm giờ trung tâm
                Center(
                  child: SizedBox(
                    width: 200,
                    height: 200,
                    child: Stack(
                      alignment: Alignment.center,
                      children: <Widget>[
                        CircularProgressIndicator(
                          value: progress,
                          strokeWidth: 10,
                          backgroundColor: tokens.borderSubtle,
                          color: tokens.domainTask,
                          strokeCap: StrokeCap.round,
                        ),
                        Column(
                          mainAxisSize: MainAxisSize.min,
                          children: <Widget>[
                            Icon(
                              _isRunning ? Icons.spa_rounded : Icons.eco_rounded,
                              size: 32,
                              color: tokens.domainTask,
                            ),
                            const SizedBox(height: 4),
                            Text(
                              timeDisplay,
                              style: TextStyle(
                                fontSize: 36,
                                fontWeight: FontWeight.w700,
                                color: tokens.textPrimary,
                                fontFeatures: const <FontFeature>[
                                  FontFeature.tabularFigures(),
                                ],
                              ),
                            ),
                            Text(
                              _isRunning ? 'Đang tập trung...' : '25 phút làm việc sâu',
                              style: TextStyle(
                                fontSize: 11,
                                fontWeight: FontWeight.w500,
                                color: tokens.textSecondary,
                              ),
                            ),
                          ],
                        ),
                      ],
                    ),
                  ),
                ),
                const SizedBox(height: LifeOsTokens.space4),
                // Nút điều khiển
                Row(
                  mainAxisAlignment: MainAxisAlignment.center,
                  children: <Widget>[
                    OutlinedButton.icon(
                      onPressed: _resetTimer,
                      style: OutlinedButton.styleFrom(
                        foregroundColor: tokens.textSecondary,
                        minimumSize: const Size(120, LifeOsTokens.minTouchTarget),
                        shape: RoundedRectangleBorder(
                          borderRadius: BorderRadius.circular(LifeOsTokens.radiusMd),
                        ),
                      ),
                      icon: const Icon(Icons.refresh_rounded),
                      label: const Text('Đặt lại'),
                    ),
                    const SizedBox(width: LifeOsTokens.space3),
                    FilledButton.icon(
                      onPressed: _toggleTimer,
                      style: FilledButton.styleFrom(
                        backgroundColor: tokens.domainTask,
                        foregroundColor: Colors.white,
                        minimumSize: const Size(140, LifeOsTokens.minTouchTarget),
                        shape: RoundedRectangleBorder(
                          borderRadius: BorderRadius.circular(LifeOsTokens.radiusMd),
                        ),
                      ),
                      icon: Icon(_isRunning ? Icons.pause_rounded : Icons.play_arrow_rounded),
                      label: Text(_isRunning ? 'Tạm dừng' : 'Bắt đầu'),
                    ),
                  ],
                ),
                const SizedBox(height: LifeOsTokens.space6),
                // Khu vườn thống kê
                Container(
                  padding: const EdgeInsets.all(LifeOsTokens.space4),
                  decoration: BoxDecoration(
                    color: tokens.bgElevated,
                    borderRadius: BorderRadius.circular(LifeOsTokens.radiusLg),
                    border: Border.all(color: tokens.borderSubtle),
                  ),
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: <Widget>[
                      Row(
                        mainAxisAlignment: MainAxisAlignment.spaceBetween,
                        children: <Widget>[
                          Text(
                            'Khu vườn của bạn',
                            style: TextStyle(
                              fontSize: 14,
                              fontWeight: FontWeight.w700,
                              color: tokens.textPrimary,
                            ),
                          ),
                          Text(
                            'Tổng $totalMinutes phút',
                            style: TextStyle(
                              fontSize: 12,
                              fontWeight: FontWeight.w600,
                              color: tokens.domainTask,
                            ),
                          ),
                        ],
                      ),
                      const SizedBox(height: LifeOsTokens.space2),
                      Text(
                        'Đã gieo trồng thành công: $treeCount cây',
                        style: TextStyle(
                          fontSize: 12.5,
                          color: tokens.textSecondary,
                        ),
                      ),
                      const SizedBox(height: LifeOsTokens.space3),
                      // Hiển thị các cây đã trồng
                      if (treeCount == 0)
                        Padding(
                          padding: const EdgeInsets.symmetric(vertical: 12.0),
                          child: Center(
                            child: Text(
                              'Chưa có cây nào. Hãy hoàn thành phiên tập trung đầu tiên!',
                              style: TextStyle(
                                fontSize: 12,
                                color: tokens.textMuted,
                              ),
                            ),
                          ),
                        )
                      else
                        Wrap(
                          spacing: 12,
                          runSpacing: 12,
                          children: List<Widget>.generate(
                            treeCount.clamp(0, 30),
                            (int index) => Container(
                              padding: const EdgeInsets.all(8),
                              decoration: BoxDecoration(
                                color: tokens.bgSurface,
                                borderRadius: BorderRadius.circular(LifeOsTokens.radiusMd),
                                border: Border.all(color: tokens.borderSubtle),
                              ),
                              child: Icon(
                                index % 3 == 0
                                    ? Icons.forest_rounded
                                    : index % 2 == 0
                                        ? Icons.park_rounded
                                        : Icons.nature_rounded,
                                size: 28,
                                color: tokens.domainTask,
                              ),
                            ),
                          ),
                        ),
                    ],
                  ),
                ),
              ],
            ),
          ),
        ],
      ),
    );
  }
}

