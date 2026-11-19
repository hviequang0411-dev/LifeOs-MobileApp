import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../../core/calculators/habit_calculator.dart';
import '../../../../core/database/app_database.dart';
import '../../../../core/l10n/vi_strings.dart';
import '../../../../core/money/decimal10k.dart';
import '../../../../core/network/api_client.dart';
import '../../../../core/providers/app_providers.dart';
import '../../../../core/sync/entity_types.dart';
import '../../../../core/theme/app_tokens.dart';
import '../../../../core/time/user_day_clock.dart';
import '../../../../core/widgets/lifeos_widgets.dart';
import '../../../ai/data/ai_models.dart';
import '../../../ai/data/ai_providers.dart';
import '../../../ai/presentation/widgets/ai_plans_and_insights_sheets.dart';
import '../../../finance/presentation/widgets/budget_pace_gauge.dart';
import '../../../habits/presentation/widgets/habit_editor_sheet.dart';
import '../../../habits/presentation/widgets/habit_heatmap_widget.dart';
import '../../../workspace/data/lifeos_local_repository.dart';

/// Khối Tài chính hôm nay + Thước đo ngân sách & Thói quen 1 chạm trên tab My Day.
class MyDayHabitsFinanceSection extends ConsumerStatefulWidget {
  const MyDayHabitsFinanceSection({
    super.key,
    required this.userId,
    required this.todayIso,
    required this.todayExpense,
    this.monthlyExpense,
    required this.habits,
    required this.allHabitEntries,
    required this.heatmapMatrix,
  });

  final String userId;
  final String todayIso;
  final Decimal10k todayExpense;
  final Decimal10k? monthlyExpense;
  final List<LocalEntityRow> habits;
  final List<LocalEntityRow> allHabitEntries;
  final Map<String, int> heatmapMatrix;

  @override
  ConsumerState<MyDayHabitsFinanceSection> createState() =>
      _MyDayHabitsFinanceSectionState();
}

class _MyDayHabitsFinanceSectionState
    extends ConsumerState<MyDayHabitsFinanceSection> {
  bool _loadingHabitInsight = false;

  Future<void> _handleAiHabitInsight() async {
    if (_loadingHabitInsight) return;
    setState(() => _loadingHabitInsight = true);
    try {
      final String fromIso = UserDayClock.addDays(widget.todayIso, -29);
      final AiInsightResponse insight = await ref
          .read(aiApiClientProvider)
          .createHabitInsight(
            from: fromIso,
            to: widget.todayIso,
            contextDomains: const <String>['HABIT'],
          );
      if (!mounted) return;
      await showModalBottomSheet<void>(
        context: context,
        isScrollControlled: true,
        backgroundColor: Colors.transparent,
        builder: (_) => AiInsightSheet(
          title: 'Phân tích thói quen từ AI',
          insight: insight,
        ),
      );
    } catch (e) {
      if (!mounted) return;
      final AiErrorDescriptor err =
          ViStrings.mapAiError(e is LifeOsApiException ? e.errorCode : null);
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(content: Text(err.message)),
      );
    } finally {
      if (mounted) {
        setState(() => _loadingHabitInsight = false);
      }
    }
  }

  @override
  Widget build(BuildContext context) {
    final String userId = widget.userId;
    final String todayIso = widget.todayIso;
    final Decimal10k todayExpense = widget.todayExpense;
    final Decimal10k paceSpent = widget.monthlyExpense ?? todayExpense;
    final List<LocalEntityRow> habits = widget.habits;
    final List<LocalEntityRow> allHabitEntries = widget.allHabitEntries;
    final Map<String, int> heatmapMatrix = widget.heatmapMatrix;
    final AiPreferencesResponse? aiPrefs =
        ref.watch(aiPreferencesProvider).valueOrNull;
    final bool allowHabitInsight = aiPrefs?.habit == true;
    final bool isOnline = ref.watch(aiIsOnlineProvider);
    final LifeOsTokens tokens = LifeOsTokens.of(context);
    final LifeOsLocalRepository repo = ref.watch(localRepositoryProvider);
    final bool masked = ref.watch(privacyMaskAmountsProvider);
    final DateTime refUtc = ref.watch(referenceUtcProvider);
    final List<LocalEntityRow> budgets = ref
            .watch(entitiesByTypeStreamProvider(SyncEntityType.budget))
            .valueOrNull ??
        const <LocalEntityRow>[];
    int realBudgetScaled = 0;
    for (final LocalEntityRow b in budgets) {
      final Object? raw = b.payload['amount'];
      if (raw != null) {
        try {
          realBudgetScaled +=
              Decimal10k.parseJsonString(raw.toString()).scaledValue;
        } catch (_) {}
      }
    }
    final Decimal10k realBudgetLimit = Decimal10k.fromScaled(realBudgetScaled);

    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: <Widget>[
        // Tài chính hôm nay + nút ẩn/hiện số tiền
        LifeOsCard(
          elevation: 1,
          margin: EdgeInsets.zero,
          padding: const EdgeInsets.symmetric(
            horizontal: LifeOsTokens.space4,
            vertical: LifeOsTokens.space3,
          ),
          child: Row(
            children: <Widget>[
              Container(
                padding: const EdgeInsets.all(8),
                decoration: BoxDecoration(
                  color: tokens.domainFinance.withValues(alpha: 0.12),
                  borderRadius: BorderRadius.circular(LifeOsTokens.radiusMd),
                ),
                child: Icon(
                  Icons.account_balance_wallet_outlined,
                  size: 18,
                  color: tokens.domainFinance,
                ),
              ),
              const SizedBox(width: LifeOsTokens.space3),
              Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: <Widget>[
                    Text(
                      'Tài chính hôm nay',
                      style: TextStyle(
                        fontSize: 13,
                        fontWeight: FontWeight.w600,
                        color: tokens.textSecondary,
                      ),
                    ),
                    const SizedBox(height: 2),
                    Text(
                      todayExpense.formatDisplay(masked: masked),
                      style: TextStyle(
                        fontSize: 18,
                        fontWeight: FontWeight.w800,
                        color: tokens.textPrimary,
                        fontFeatures: const <FontFeature>[
                          FontFeature.tabularFigures(),
                        ],
                      ),
                    ),
                  ],
                ),
              ),
              IconButton(
                key: const Key('btn_toggle_mask_amount'),
                tooltip: masked ? 'Hiện số tiền' : 'Ẩn số tiền',
                onPressed: () {
                  ref.read(privacyMaskAmountsProvider.notifier).state = !masked;
                },
                icon: Semantics(
                  label: masked ? 'Hiện số tiền' : 'Ẩn số tiền',
                  child: Icon(
                    masked
                        ? Icons.visibility_off_outlined
                        : Icons.visibility_outlined,
                    color: tokens.textSecondary,
                  ),
                ),
              ),
            ],
          ),
        ),
        const SizedBox(height: LifeOsTokens.space3),
        BudgetPaceGauge(
          spentFormatted: paceSpent.formatDisplay(masked: masked),
          limitFormatted: realBudgetScaled > 0
              ? realBudgetLimit.formatDisplay(masked: masked)
              : 'Chưa đặt',
          spentRatio: realBudgetScaled > 0
              ? (paceSpent.percentageBasisPointsOf(realBudgetLimit) / 10000.0)
                  .clamp(0.0, 1.5)
              : 0.0,
          monthElapsedRatio: (refUtc.toLocal().day / 30.0).clamp(0.05, 1.0),
        ),
        const SizedBox(height: LifeOsTokens.space4),
        Row(
          children: <Widget>[
            Expanded(
              child: Text(
                'Thói quen trong ngày (1 chạm)',
                style: Theme.of(context).textTheme.titleMedium?.copyWith(
                      fontWeight: FontWeight.w700,
                    ),
              ),
            ),
            if (allowHabitInsight)
              TextButton.icon(
                key: const Key('btn_ai_habit_insight'),
                onPressed: isOnline && !_loadingHabitInsight
                    ? _handleAiHabitInsight
                    : null,
                icon: _loadingHabitInsight
                    ? const SizedBox(
                        width: 14,
                        height: 14,
                        child: CircularProgressIndicator(strokeWidth: 2),
                      )
                    : const Icon(Icons.auto_awesome_rounded, size: 16),
                label: const Text('AI phân tích'),
              ),
            IconButton(
              key: const Key('btn_open_create_habit'),
              tooltip: 'Thêm thói quen mới',
              onPressed: () {
                showModalBottomSheet<void>(
                  context: context,
                  isScrollControlled: true,
                  backgroundColor: Colors.transparent,
                  builder: (_) => const HabitEditorSheet(),
                );
              },
              icon: const Icon(Icons.add_circle_outline_rounded),
            ),
          ],
        ),
        const SizedBox(height: LifeOsTokens.space2),
        if (habits.isNotEmpty) ...<Widget>[
          HabitHeatmapWidget(
            streakDays: repo
                .computeHabitStats(
                  userId: userId,
                  habitId: habits.first.id,
                  asOfDateIso: todayIso,
                )
                .currentStreak,
            completionMatrix: heatmapMatrix,
            referenceDateIso: todayIso,
          ),
          const SizedBox(height: LifeOsTokens.space3),
        ],
        if (habits.isEmpty)
          LifeOsCard(
            elevation: 1,
            padding: const EdgeInsets.all(LifeOsTokens.space4),
            child: Wrap(
              spacing: 12,
              runSpacing: 10,
              alignment: WrapAlignment.spaceBetween,
              crossAxisAlignment: WrapCrossAlignment.center,
              children: <Widget>[
                Row(
                  mainAxisSize: MainAxisSize.min,
                  children: <Widget>[
                    Icon(Icons.spa_outlined, color: tokens.domainHabit),
                    const SizedBox(width: 12),
                    Flexible(
                      child: Column(
                        crossAxisAlignment: CrossAxisAlignment.start,
                        children: <Widget>[
                          Text(
                            'Chưa có thói quen nào',
                            style: Theme.of(context).textTheme.titleMedium,
                          ),
                          Text(
                            'Điểm danh 1 chạm, tự động theo dõi chuỗi ngày',
                            style: Theme.of(context).textTheme.bodySmall,
                          ),
                        ],
                      ),
                    ),
                  ],
                ),
                FilledButton.tonal(
                  onPressed: () {
                    repo.createHabit(
                      userId: userId,
                      name: 'Uống đủ 2 lít nước',
                    );
                  },
                  child: const Text('Tạo nhanh'),
                ),
              ],
            ),
          )
        else
          LifeOsCard(
            elevation: 1,
            padding: EdgeInsets.zero,
            child: Column(
              children: habits.map((LocalEntityRow habitRow) {
                final HabitStatsResult stats = repo.computeHabitStats(
                  userId: userId,
                  habitId: habitRow.id,
                  asOfDateIso: todayIso,
                );

                String? todayStatus;
                for (final LocalEntityRow entry in allHabitEntries) {
                  if (entry.payload['habitId'] == habitRow.id &&
                      entry.payload['entryDate'] == todayIso) {
                    todayStatus =
                        (entry.payload['status'] as String?)?.toUpperCase();
                    break;
                  }
                }
                todayStatus ??= stats.completedOnAsOfDate ? 'DONE' : null;

                final Widget habitActionButton = _buildHabitButton(
                  tokens: tokens,
                  repo: repo,
                  userId: userId,
                  todayIso: todayIso,
                  habitId: habitRow.id,
                  todayStatus: todayStatus,
                );

                return InkWell(
                  onTap: () {
                    showModalBottomSheet<void>(
                      context: context,
                      isScrollControlled: true,
                      backgroundColor: Colors.transparent,
                      builder: (_) => HabitEditorSheet(habit: habitRow),
                    );
                  },
                  child: Padding(
                    padding: const EdgeInsets.symmetric(
                      horizontal: LifeOsTokens.space3,
                      vertical: 8,
                    ),
                    child: Row(
                    children: <Widget>[
                      Expanded(
                        child: Column(
                          crossAxisAlignment: CrossAxisAlignment.start,
                          children: <Widget>[
                            Text(
                              (habitRow.payload['name'] as String?) ??
                                  'Thói quen',
                              style:
                                  const TextStyle(fontWeight: FontWeight.w600),
                            ),
                            const SizedBox(height: 2),
                            Row(
                              mainAxisSize: MainAxisSize.min,
                              children: <Widget>[
                                Icon(
                                  Icons.local_fire_department_rounded,
                                  size: 15,
                                  color: tokens.domainHabitButtonBg,
                                ),
                                const SizedBox(width: 4),
                                Flexible(
                                  child: Text(
                                    'Chuỗi ${stats.currentStreak} ngày · Kỷ lục ${stats.bestStreak} ngày',
                                    style:
                                        Theme.of(context).textTheme.bodySmall,
                                  ),
                                ),
                              ],
                            ),
                          ],
                        ),
                      ),
                      const SizedBox(width: 8),
                      habitActionButton,
                    ],
                  ),
                ),
                );
              }).toList(),
            ),
          ),
      ],
    );
  }

  Widget _buildHabitButton({
    required LifeOsTokens tokens,
    required LifeOsLocalRepository repo,
    required String userId,
    required String todayIso,
    required String habitId,
    required String? todayStatus,
  }) {
    if (todayStatus == 'DONE') {
      return OutlinedButton.icon(
        key: Key('btn_habit_${habitId}_done'),
        style: OutlinedButton.styleFrom(
          minimumSize: const Size(
            LifeOsTokens.minTouchTarget,
            LifeOsTokens.minTouchTarget,
          ),
          side: BorderSide(color: tokens.accentPrimary.withValues(alpha: 0.36)),
          backgroundColor: tokens.accentPrimary.withValues(alpha: 0.12),
          foregroundColor: tokens.accentPrimary,
          padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 8),
        ),
        onPressed: () => repo.checkInHabit(
          userId: userId,
          habitId: habitId,
          entryDateIso: todayIso,
          status: 'SKIPPED',
        ),
        icon: const Icon(Icons.check_rounded, size: 16),
        label: const Text(
          'Đã điểm danh',
          style: TextStyle(fontWeight: FontWeight.w600),
        ),
      );
    }
    if (todayStatus == 'SKIPPED') {
      return OutlinedButton.icon(
        key: Key('btn_habit_${habitId}_skipped'),
        style: OutlinedButton.styleFrom(
          minimumSize: const Size(
            LifeOsTokens.minTouchTarget,
            LifeOsTokens.minTouchTarget,
          ),
          side: BorderSide(color: tokens.borderSubtle),
          backgroundColor: tokens.bgElevated,
          foregroundColor: tokens.textSecondary,
          padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 8),
        ),
        onPressed: () => repo.checkInHabit(
          userId: userId,
          habitId: habitId,
          entryDateIso: todayIso,
          status: 'DONE',
        ),
        icon: const Icon(Icons.redo_rounded, size: 16),
        label: const Text(
          'Đã bỏ qua',
          style: TextStyle(fontWeight: FontWeight.w600),
        ),
      );
    }
    return FilledButton(
      key: Key('btn_habit_${habitId}_pending'),
      style: FilledButton.styleFrom(
        minimumSize: const Size(
          LifeOsTokens.minTouchTarget,
          LifeOsTokens.minTouchTarget,
        ),
        backgroundColor: tokens.domainHabitButtonBg,
        foregroundColor: tokens.domainHabitButtonFg,
        elevation: 0,
        padding: const EdgeInsets.symmetric(horizontal: 14, vertical: 8),
      ),
      onPressed: () => repo.checkInHabit(
        userId: userId,
        habitId: habitId,
        entryDateIso: todayIso,
        status: 'DONE',
      ),
      child: const Text(
        'Điểm danh',
        style: TextStyle(fontWeight: FontWeight.w700),
      ),
    );
  }
}

