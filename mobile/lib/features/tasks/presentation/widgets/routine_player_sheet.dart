import 'dart:async';

import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../../core/database/app_database.dart';
import '../../../../core/providers/app_providers.dart';
import '../../../../core/theme/app_tokens.dart';
import '../../../../core/widgets/lifeos_widgets.dart';
import '../../../workspace/data/lifeos_local_repository.dart';

/// Trình chạy Chuỗi thói quen (`Routine Player`) đi từng bước (`MWP-C`):
/// - Đếm ngược theo `durationMinutes` của bước (`MM:SS`, Bắt đầu / Tạm dừng / Đặt lại).
/// - Ghi nhận `DONE` hoặc `SKIPPED` vào `routine_log` & `routine_step_log` (idempotent theo ngày) + Outbox.
class RoutinePlayerSheet extends ConsumerStatefulWidget {
  const RoutinePlayerSheet({
    super.key,
    required this.routine,
  });

  final LocalEntityRow routine;

  @override
  ConsumerState<RoutinePlayerSheet> createState() => _RoutinePlayerSheetState();
}

class _RoutinePlayerSheetState extends ConsumerState<RoutinePlayerSheet> {
  late final String _todayIso;
  late String _routineLogId;
  late List<LocalEntityRow> _steps;
  late Map<String, String> _stepStatuses;

  int _currentIndex = 0;
  int _remainingSeconds = 300;
  bool _timerRunning = false;
  Timer? _ticker;

  @override
  void initState() {
    super.initState();
    final String userId = ref.read(currentUserIdProvider);
    final LifeOsLocalRepository repo = ref.read(localRepositoryProvider);
    final DateTime refUtc = ref.read(referenceUtcProvider);
    _todayIso = repo.getUserDayClock(userId).businessDateForUtc(refUtc);
    _steps = repo.tasks.listRoutineSteps(
      userId: userId,
      routineId: widget.routine.id,
    );
    _routineLogId = repo.tasks.startOrGetRoutineLogForDate(
      userId: userId,
      routineId: widget.routine.id,
      localDateIso: _todayIso,
      totalSteps: _steps.isEmpty ? 1 : _steps.length,
      nowUtc: refUtc,
    );
    _stepStatuses = repo.tasks.getRoutineStepStatuses(
      userId: userId,
      routineLogId: _routineLogId,
    );

    final int firstPending = _steps.indexWhere(
      (LocalEntityRow s) => !_stepStatuses.containsKey(s.id),
    );
    _currentIndex = firstPending >= 0 ? firstPending : 0;
    _resetTimerForCurrentStep();
  }

  @override
  void dispose() {
    _ticker?.cancel();
    super.dispose();
  }

  void _resetTimerForCurrentStep() {
    _ticker?.cancel();
    _timerRunning = false;
    if (_steps.isEmpty || _currentIndex >= _steps.length) {
      _remainingSeconds = 0;
      return;
    }
    final int mins =
        (_steps[_currentIndex].payload['durationMinutes'] as num?)?.toInt() ??
            5;
    _remainingSeconds = mins.clamp(1, 240) * 60;
  }

  void _toggleTimer() {
    if (_timerRunning) {
      _ticker?.cancel();
      setState(() => _timerRunning = false);
      return;
    }
    if (_remainingSeconds <= 0) {
      _resetTimerForCurrentStep();
    }
    setState(() => _timerRunning = true);
    _ticker = Timer.periodic(const Duration(seconds: 1), (Timer t) {
      if (!mounted) {
        t.cancel();
        return;
      }
      if (_remainingSeconds <= 1) {
        t.cancel();
        setState(() {
          _remainingSeconds = 0;
          _timerRunning = false;
        });
      } else {
        setState(() => _remainingSeconds--);
      }
    });
  }

  void _markStep(String status) {
    if (_steps.isEmpty || _currentIndex >= _steps.length) return;
    final String userId = ref.read(currentUserIdProvider);
    final LifeOsLocalRepository repo = ref.read(localRepositoryProvider);
    final LocalEntityRow step = _steps[_currentIndex];

    repo.tasks.recordRoutineStepLog(
      userId: userId,
      routineLogId: _routineLogId,
      stepId: step.id,
      status: status,
      nowUtc: ref.read(referenceUtcProvider),
    );

    _ticker?.cancel();
    setState(() {
      _stepStatuses = <String, String>{..._stepStatuses, step.id: status};
      _timerRunning = false;
      final int nextPending = _steps.indexWhere(
        (LocalEntityRow s) => !_stepStatuses.containsKey(s.id),
      );
      if (nextPending >= 0) {
        _currentIndex = nextPending;
      } else if (_currentIndex < _steps.length - 1) {
        _currentIndex++;
      }
      _resetTimerForCurrentStep();
    });
  }

  String _formatCountdown(int totalSeconds) {
    final int mins = totalSeconds ~/ 60;
    final int secs = totalSeconds % 60;
    return '${mins.toString().padLeft(2, '0')}:${secs.toString().padLeft(2, '0')}';
  }

  @override
  Widget build(BuildContext context) {
    final LifeOsTokens tokens = LifeOsTokens.of(context);
    final String routineName =
        (widget.routine.payload['name'] as String?) ?? 'Chuỗi thói quen';
    final int doneCount = _stepStatuses.values
        .where((String s) => s == 'DONE' || s == 'SKIPPED')
        .length;
    final bool allCompleted =
        _steps.isNotEmpty && doneCount >= _steps.length;

    return Material(
      color: tokens.bgSurface,
      borderRadius: const BorderRadius.vertical(
        top: Radius.circular(LifeOsTokens.radiusLg),
      ),
      clipBehavior: Clip.antiAlias,
      child: Padding(
        padding: const EdgeInsets.fromLTRB(20, 16, 20, 24),
      child: SingleChildScrollView(
        child: Column(
          mainAxisSize: MainAxisSize.min,
          crossAxisAlignment: CrossAxisAlignment.stretch,
          children: <Widget>[
            Row(
              mainAxisAlignment: MainAxisAlignment.spaceBetween,
              children: <Widget>[
                Expanded(
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: <Widget>[
                      Text(
                        routineName,
                        style: Theme.of(context).textTheme.titleLarge,
                      ),
                      const SizedBox(height: 2),
                      Text(
                        'Ngày $_todayIso · Đã xử lý $doneCount/${_steps.length} bước',
                        style: TextStyle(
                          fontSize: 13,
                          color: tokens.textSecondary,
                        ),
                      ),
                    ],
                  ),
                ),
                IconButton(
                  tooltip: 'Đóng',
                  onPressed: () => Navigator.of(context).pop(),
                  icon: const Icon(Icons.close_rounded),
                ),
              ],
            ),
            const SizedBox(height: 12),
            if (_steps.isEmpty)
              const LifeOsEmptyState(
                icon: Icons.playlist_add_check_outlined,
                title: 'Chuỗi thói quen chưa có bước nào',
                subtitle: 'Hãy tạo các bước cho chuỗi thói quen này trước khi chạy.',
              )
            else ...<Widget>[
              ClipRRect(
                borderRadius: BorderRadius.circular(4),
                child: LinearProgressIndicator(
                  minHeight: 8,
                  value: (doneCount / _steps.length).clamp(0.0, 1.0),
                  backgroundColor: tokens.borderSubtle,
                  valueColor:
                      AlwaysStoppedAnimation<Color>(tokens.accentPrimary),
                ),
              ),
              const SizedBox(height: 16),
              LifeOsCard(
                elevation: 1,
                padding: const EdgeInsets.all(LifeOsTokens.space4),
                child: Column(
                  children: <Widget>[
                    Text(
                      'Bước ${_currentIndex + 1} / ${_steps.length}',
                      style: TextStyle(
                        fontSize: 12.5,
                        fontWeight: FontWeight.w700,
                        color: tokens.accentPrimary,
                      ),
                    ),
                    const SizedBox(height: 6),
                    Text(
                      (_steps[_currentIndex].payload['title'] as String?) ??
                          'Bước thực hiện',
                      textAlign: TextAlign.center,
                      style: TextStyle(
                        fontSize: 20,
                        fontWeight: FontWeight.w800,
                        color: tokens.textPrimary,
                      ),
                    ),
                    const SizedBox(height: 14),
                    Text(
                      _formatCountdown(_remainingSeconds),
                      key: const Key('routine_timer_display'),
                      style: TextStyle(
                        fontSize: 38,
                        fontWeight: FontWeight.w800,
                        color: _timerRunning
                            ? tokens.accentPrimary
                            : tokens.textPrimary,
                        fontFeatures: const <FontFeature>[
                          FontFeature.tabularFigures(),
                        ],
                      ),
                    ),
                    const SizedBox(height: 12),
                    Wrap(
                      spacing: 8,
                      runSpacing: 8,
                      alignment: WrapAlignment.center,
                      children: <Widget>[
                        FilledButton.tonalIcon(
                          key: const Key('btn_routine_toggle_timer'),
                          style: FilledButton.styleFrom(
                            minimumSize: const Size(
                              120,
                              LifeOsTokens.minTouchTarget,
                            ),
                          ),
                          onPressed: _toggleTimer,
                          icon: Icon(
                            _timerRunning
                                ? Icons.pause_rounded
                                : Icons.play_arrow_rounded,
                          ),
                          label: Text(_timerRunning ? 'Tạm dừng' : 'Bấm giờ'),
                        ),
                        OutlinedButton.icon(
                          key: const Key('btn_routine_reset_timer'),
                          style: OutlinedButton.styleFrom(
                            minimumSize: const Size(
                              100,
                              LifeOsTokens.minTouchTarget,
                            ),
                          ),
                          onPressed: () {
                            setState(_resetTimerForCurrentStep);
                          },
                          icon: const Icon(Icons.replay_rounded, size: 18),
                          label: const Text('Đặt lại'),
                        ),
                      ],
                    ),
                  ],
                ),
              ),
              const SizedBox(height: 14),
              Row(
                children: <Widget>[
                  Expanded(
                    child: OutlinedButton(
                      key: const Key('btn_routine_skip_step'),
                      style: OutlinedButton.styleFrom(
                        minimumSize:
                            const Size.fromHeight(LifeOsTokens.minTouchTarget),
                      ),
                      onPressed: () => _markStep('SKIPPED'),
                      child: const Text('Bỏ qua bước'),
                    ),
                  ),
                  const SizedBox(width: 10),
                  Expanded(
                    child: FilledButton(
                      key: const Key('btn_routine_complete_step'),
                      style: FilledButton.styleFrom(
                        minimumSize:
                            const Size.fromHeight(LifeOsTokens.minTouchTarget),
                      ),
                      onPressed: () => _markStep('DONE'),
                      child: const Text('Hoàn thành'),
                    ),
                  ),
                ],
              ),
              const SizedBox(height: 14),
              if (allCompleted)
                Container(
                  padding: const EdgeInsets.all(12),
                  decoration: BoxDecoration(
                    color: tokens.success.withValues(alpha: 0.12),
                    borderRadius: BorderRadius.circular(LifeOsTokens.radiusMd),
                  ),
                  child: Text(
                    'Tuyệt vời! Bạn đã hoàn tất toàn bộ chuỗi thói quen hôm nay.',
                    textAlign: TextAlign.center,
                    style: TextStyle(
                      fontSize: 13.5,
                      fontWeight: FontWeight.w700,
                      color: tokens.success,
                    ),
                  ),
                ),
              const SizedBox(height: 8),
              ...List<Widget>.generate(_steps.length, (int i) {
                final LocalEntityRow s = _steps[i];
                final String? st = _stepStatuses[s.id];
                final int mins =
                    (s.payload['durationMinutes'] as num?)?.toInt() ?? 5;
                return ListTile(
                  dense: true,
                  contentPadding: EdgeInsets.zero,
                  leading: Icon(
                    st == 'DONE'
                        ? Icons.check_circle_rounded
                        : st == 'SKIPPED'
                            ? Icons.fast_forward_rounded
                            : i == _currentIndex
                                ? Icons.radio_button_checked_rounded
                                : Icons.radio_button_unchecked_rounded,
                    color: st == 'DONE'
                        ? tokens.success
                        : st == 'SKIPPED'
                            ? tokens.warning
                            : i == _currentIndex
                                ? tokens.accentPrimary
                                : tokens.textMuted,
                  ),
                  title: Text(
                    (s.payload['title'] as String?) ?? 'Bước ${i + 1}',
                    style: TextStyle(
                      fontWeight:
                          i == _currentIndex ? FontWeight.w700 : FontWeight.w500,
                      color: tokens.textPrimary,
                    ),
                  ),
                  trailing: Text(
                    '$mins phút',
                    style: TextStyle(fontSize: 12, color: tokens.textSecondary),
                  ),
                  onTap: () {
                    setState(() {
                      _currentIndex = i;
                      _resetTimerForCurrentStep();
                    });
                  },
                );
              }),
            ],
          ],
        ),
      ),
      ),
    );
  }
}
