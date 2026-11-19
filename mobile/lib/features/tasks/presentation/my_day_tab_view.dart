import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../core/database/app_database.dart';
import '../../../core/l10n/vi_strings.dart';
import '../../../core/money/decimal10k.dart';
import '../../../core/providers/app_providers.dart';
import '../../../core/sync/entity_types.dart';
import '../../../core/theme/app_tokens.dart';
import '../../../core/widgets/lifeos_widgets.dart';
import '../../workspace/data/lifeos_local_repository.dart';
import '../../workspace/presentation/widgets/focus_garden_sheet.dart';
import '../../workspace/presentation/widgets/global_search_sheet.dart';
import '../../workspace/presentation/widgets/life_wheel_sheet.dart';
import 'widgets/life_rings_widget.dart';
import 'widgets/my_day_habits_finance_section.dart';
import 'widgets/task_editor_sheet.dart';

/// Tab 0: Màn hình **Hôm nay (`My Day`)** — Trung tâm điều phối ngày đọc từ Stream Drift.
class MyDayTabView extends ConsumerWidget {
  const MyDayTabView({
    super.key,
    required this.onOpenQuickCapture,
    required this.onOpenDiagnostics,
  });

  final VoidCallback onOpenQuickCapture;
  final VoidCallback onOpenDiagnostics;

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final LifeOsTokens tokens = LifeOsTokens.of(context);
    final String userId = ref.watch(currentUserIdProvider);
    final LifeOsLocalRepository repo = ref.watch(localRepositoryProvider);
    final DateTime refUtc = ref.watch(referenceUtcProvider);

    final AsyncValue<LocalSyncStateRecord> syncStateAsync =
        ref.watch(syncStateStreamProvider);
    final AsyncValue<List<OutboxMutationRecord>> outboxAsync =
        ref.watch(outboxStreamProvider);
    final AsyncValue<List<LocalEntityRow>> tasksAsync =
        ref.watch(entitiesByTypeStreamProvider(SyncEntityType.task));
    final AsyncValue<List<LocalEntityRow>> habitsAsync =
        ref.watch(entitiesByTypeStreamProvider(SyncEntityType.habit));
    final AsyncValue<List<LocalEntityRow>> habitEntriesAsync =
        ref.watch(entitiesByTypeStreamProvider(SyncEntityType.habitEntry));
    final List<LocalEntityRow> allHabitEntries =
        habitEntriesAsync.valueOrNull ?? const <LocalEntityRow>[];
    final AsyncValue<List<LocalEntityRow>> txsAsync = ref.watch(
      entitiesByTypeStreamProvider(SyncEntityType.financeTransaction),
    );
    final AsyncValue<List<LocalEntityRow>> budgetsAsync = ref.watch(
      entitiesByTypeStreamProvider(SyncEntityType.budget),
    );

    final String todayIso =
        repo.getUserDayClock(userId).businessDateForUtc(refUtc);
    final String monthPrefix =
        todayIso.length >= 7 ? todayIso.substring(0, 7) : todayIso;
    final List<LocalEntityRow> myDayTasks =
        repo.queryMyDayTasks(userId: userId, todayIso: todayIso);
    final List<LocalEntityRow> habits =
        habitsAsync.valueOrNull ?? const <LocalEntityRow>[];
    final List<LocalEntityRow> txs =
        txsAsync.valueOrNull ?? const <LocalEntityRow>[];
    final List<LocalEntityRow> budgets =
        budgetsAsync.valueOrNull ?? const <LocalEntityRow>[];

    Decimal10k todayExpense = Decimal10k.zero;
    Decimal10k monthlyExpense = Decimal10k.zero;
    for (final LocalEntityRow tx in txs) {
      final String kind =
          ((tx.payload['kind'] ?? tx.payload['type']) as String? ?? 'EXPENSE')
              .toUpperCase();
      final String? occurredOn =
          (tx.payload['occurredOn'] ?? tx.payload['transactionDate']) as String?;
      if (kind == 'EXPENSE' && occurredOn != null) {
        final Decimal10k amt = Decimal10k.parseJsonString(
          (tx.payload['amount'] ?? '0').toString(),
        );
        if (occurredOn == todayIso) {
          todayExpense = todayExpense + amt;
        }
        if (occurredOn.startsWith(monthPrefix)) {
          monthlyExpense = monthlyExpense + amt;
        }
      }
    }

    int realBudgetScaled = 0;
    for (final LocalEntityRow b in budgets) {
      if (b.payload['archived'] == true || b.payload['archivedAt'] != null) {
        continue;
      }
      final Object? raw = b.payload['amount'];
      if (raw != null) {
        try {
          realBudgetScaled +=
              Decimal10k.parseJsonString(raw.toString()).scaledValue;
        } catch (_) {}
      }
    }
    final int realBudgetWholeVnd = realBudgetScaled ~/ Decimal10k.scaleFactor;

    final int doneTasksCount = myDayTasks.where((LocalEntityRow r) {
      final String s = (r.payload['status'] as String? ?? 'OPEN').toUpperCase();
      return s == 'COMPLETED' || s == 'DONE';
    }).length;

    int doneHabitsCount = 0;
    final Map<String, int> heatmapMatrix = <String, int>{};
    for (final LocalEntityRow habit in habits) {
      for (final LocalEntityRow entry in allHabitEntries) {
        if (entry.payload['habitId'] == habit.id) {
          final String d = (entry.payload['entryDate'] ?? '').toString();
          final String st =
              (entry.payload['status'] ?? '').toString().toUpperCase();
          if (st == 'DONE') {
            heatmapMatrix[d] = (heatmapMatrix[d] ?? 0) + 1;
            if (d == todayIso) {
              doneHabitsCount++;
            }
          }
        }
      }
    }

    final double width = MediaQuery.sizeOf(context).width;
    final bool useTwoColumns = width >= LifeOsTokens.tabletMaxBreakpoint;

    // Khối việc cần làm
    final Widget tasksColumn = Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: <Widget>[
        SizedBox(
          width: double.infinity,
          child: Wrap(
            spacing: 8,
            runSpacing: 4,
            alignment: WrapAlignment.spaceBetween,
            crossAxisAlignment: WrapCrossAlignment.center,
            children: <Widget>[
              Text(
                'Việc cần làm hôm nay',
                style: Theme.of(context).textTheme.titleMedium?.copyWith(
                      fontWeight: FontWeight.w700,
                    ),
              ),
              Text(
                '$doneTasksCount/${myDayTasks.length} hoàn tất',
                style: TextStyle(
                  fontSize: 12,
                  fontWeight: FontWeight.w600,
                  color: tokens.domainTask,
                  fontFeatures: const <FontFeature>[
                    FontFeature.tabularFigures(),
                  ],
                ),
              ),
            ],
          ),
        ),
        const SizedBox(height: LifeOsTokens.space2),
        if (tasksAsync.isLoading)
          const LifeOsShimmerSkeleton(height: 120)
        else if (myDayTasks.isEmpty)
          LifeOsEmptyState(
            icon: Icons.task_alt_rounded,
            title: 'Hôm nay bạn chưa có việc cần làm',
            subtitle:
                'Chạm vào nút Thêm nhanh để ghi lại việc quan trọng nhất trong ngày.',
            actionLabel: ViStrings.quickCaptureTitle,
            onAction: onOpenQuickCapture,
          )
        else
          LifeOsCard(
            elevation: 1,
            margin: EdgeInsets.zero,
            padding: EdgeInsets.zero,
            child: Column(
              children: myDayTasks.map((LocalEntityRow row) {
                final String status =
                    (row.payload['status'] as String? ?? 'OPEN').toUpperCase();
                final bool done = status == 'COMPLETED' || status == 'DONE';
                final String title =
                    (row.payload['title'] as String?) ?? 'Công việc';
                final String prio =
                    ((row.payload['priority'] as String?) ?? 'MEDIUM')
                        .toUpperCase();
                final String prioLabelVi = ViStrings.formatTaskPriority(prio);
                final bool isHighPriority = prio == 'HIGH';
                final String? dueDate = row.payload['dueDate'] as String?;
                final bool overdue =
                    !done && dueDate != null && dueDate.compareTo(todayIso) < 0;

                final Color prioColor = isHighPriority
                    ? tokens.priorityHighFg
                    : prio == 'LOW'
                        ? tokens.priorityLowFg
                        : tokens.priorityMediumFg;

                return ListTile(
                  dense: true,
                  visualDensity: const VisualDensity(
                    horizontal: 0,
                    vertical: -2,
                  ),
                  minVerticalPadding: 4,
                  contentPadding: const EdgeInsets.symmetric(
                    horizontal: 12,
                    vertical: 2,
                  ),
                  onTap: () {
                    showModalBottomSheet<void>(
                      context: context,
                      isScrollControlled: true,
                      backgroundColor: Colors.transparent,
                      builder: (_) => TaskEditorSheet(task: row),
                    );
                  },
                  leading: Checkbox(
                    value: done,
                    activeColor: tokens.domainTask,
                    onChanged: (_) {
                      repo.toggleTaskCompletion(
                        userId: userId,
                        taskId: row.id,
                      );
                    },
                  ),
                  title: Text(
                    title,
                    maxLines: 1,
                    overflow: TextOverflow.ellipsis,
                    style: TextStyle(
                      fontWeight: FontWeight.w600,
                      decoration: done ? TextDecoration.lineThrough : null,
                      color: done ? tokens.textSecondary : tokens.textPrimary,
                    ),
                  ),
                  subtitle: Text(
                    overdue
                        ? 'Quá hạn · $dueDate'
                        : (dueDate ?? 'Trong ngày hôm nay'),
                    style: TextStyle(
                      fontSize: 12,
                      color: overdue ? tokens.danger : tokens.textSecondary,
                      fontWeight: overdue ? FontWeight.w600 : FontWeight.w400,
                    ),
                  ),
                  trailing: Container(
                    padding: const EdgeInsets.symmetric(
                      horizontal: 8,
                      vertical: 4,
                    ),
                    decoration: BoxDecoration(
                      color: prioColor.withValues(alpha: 0.14),
                      borderRadius:
                          BorderRadius.circular(LifeOsTokens.radiusSm),
                    ),
                    child: Text(
                      prioLabelVi,
                      style: TextStyle(
                        fontSize: 11,
                        fontWeight: FontWeight.w700,
                        color: prioColor,
                      ),
                    ),
                  ),
                );
              }).toList(),
            ),
          ),
      ],
    );

    final Widget habitsAndFinanceColumn = MyDayHabitsFinanceSection(
      userId: userId,
      todayIso: todayIso,
      todayExpense: todayExpense,
      monthlyExpense: monthlyExpense,
      habits: habits,
      allHabitEntries: allHabitEntries,
      heatmapMatrix: heatmapMatrix,
    );

    // Hàng nút truy cập nhanh tính năng mới
    final Widget creativeShortcutsRow = SingleChildScrollView(
      scrollDirection: Axis.horizontal,
      physics: const BouncingScrollPhysics(),
      child: Row(
        children: <Widget>[
          _buildShortcutChip(
            context,
            icon: Icons.eco_rounded,
            label: 'Khu vườn tập trung',
            color: tokens.domainTask,
            onTap: () {
              showModalBottomSheet<void>(
                context: context,
                isScrollControlled: true,
                backgroundColor: Colors.transparent,
                builder: (_) => const FocusGardenSheet(),
              );
            },
          ),
          const SizedBox(width: 8),
          _buildShortcutChip(
            context,
            icon: Icons.radar_rounded,
            label: 'Bánh xe cuộc sống',
            color: tokens.domainGoal,
            onTap: () {
              showModalBottomSheet<void>(
                context: context,
                isScrollControlled: true,
                backgroundColor: Colors.transparent,
                builder: (_) => const LifeWheelSheet(),
              );
            },
          ),
          const SizedBox(width: 8),
          _buildShortcutChip(
            context,
            icon: Icons.search_rounded,
            label: 'Tìm kiếm',
            color: tokens.textSecondary,
            onTap: () {
              showModalBottomSheet<void>(
                context: context,
                isScrollControlled: true,
                backgroundColor: Colors.transparent,
                builder: (_) => const GlobalSearchSheet(),
              );
            },
          ),
        ],
      ),
    );

    final String displayName = ref.watch(userDisplayNameProvider).trim();
    final String greetingName = displayName.isEmpty ? 'bạn' : displayName;

    return ListView(
      padding: const EdgeInsets.fromLTRB(
        0,
        LifeOsTokens.space3,
        0,
        112,
      ),
      children: <Widget>[
        Padding(
          padding: const EdgeInsets.symmetric(horizontal: LifeOsTokens.space4),
          child: Wrap(
            spacing: 12,
            runSpacing: 8,
            alignment: WrapAlignment.spaceBetween,
            crossAxisAlignment: WrapCrossAlignment.center,
            children: <Widget>[
              Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: <Widget>[
                  Text(
                    'Hôm nay · $todayIso',
                    style: Theme.of(context).textTheme.bodySmall,
                  ),
                  Text(
                    'Chào ngày mới, $greetingName',
                    style: Theme.of(context).textTheme.titleLarge?.copyWith(
                          fontSize: useTwoColumns ? 24 : 20,
                          fontWeight: FontWeight.w700,
                        ),
                  ),
                ],
              ),
              SyncStatusPill(
                outboxItems:
                    outboxAsync.valueOrNull ?? const <OutboxMutationRecord>[],
                syncPaused: syncStateAsync.valueOrNull?.syncPaused ?? false,
                onTap: onOpenDiagnostics,
              ),
            ],
          ),
        ),
        const SizedBox(height: LifeOsTokens.space3),
        // 1. Vòng đời hôm nay (Life Rings)
        Padding(
          padding: const EdgeInsets.symmetric(horizontal: LifeOsTokens.space4),
          child: LifeRingsWidget(
            taskDone: doneTasksCount,
            taskTotal: myDayTasks.length,
            habitDone: doneHabitsCount,
            habitTotal: habits.length,
            budgetSpentVnd: monthlyExpense.scaledValue ~/ Decimal10k.scaleFactor,
            budgetLimitVnd: realBudgetWholeVnd,
          ),
        ),
        const SizedBox(height: LifeOsTokens.space3),
        // 2. Phím tắt tính năng sáng tạo mới
        Padding(
          padding: const EdgeInsets.symmetric(horizontal: LifeOsTokens.space4),
          child: creativeShortcutsRow,
        ),
        const SizedBox(height: LifeOsTokens.space4),
        if (useTwoColumns)
          Padding(
            padding: const EdgeInsets.symmetric(horizontal: LifeOsTokens.space4),
            child: Row(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: <Widget>[
                Expanded(flex: 6, child: tasksColumn),
                const SizedBox(width: LifeOsTokens.space4),
                Expanded(flex: 5, child: habitsAndFinanceColumn),
              ],
            ),
          )
        else ...<Widget>[
          Padding(
            padding: const EdgeInsets.symmetric(horizontal: LifeOsTokens.space4),
            child: tasksColumn,
          ),
          const SizedBox(height: LifeOsTokens.space4),
          Padding(
            padding: const EdgeInsets.symmetric(horizontal: LifeOsTokens.space4),
            child: habitsAndFinanceColumn,
          ),
        ],
      ],
    );
  }

  Widget _buildShortcutChip(
    BuildContext context, {
    required IconData icon,
    required String label,
    required Color color,
    required VoidCallback onTap,
  }) {
    final LifeOsTokens tokens = LifeOsTokens.of(context);
    return InkWell(
      borderRadius: BorderRadius.circular(LifeOsTokens.radiusPill),
      onTap: onTap,
      child: Container(
        padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 8),
        decoration: BoxDecoration(
          color: tokens.bgSurface,
          borderRadius: BorderRadius.circular(LifeOsTokens.radiusPill),
          border: Border.all(color: tokens.borderSubtle),
          boxShadow: LifeOsTokens.shadowElevation1,
        ),
        child: Row(
          mainAxisSize: MainAxisSize.min,
          children: <Widget>[
            Icon(icon, size: 16, color: color),
            const SizedBox(width: 6),
            Text(
              label,
              style: TextStyle(
                fontSize: 12,
                fontWeight: FontWeight.w600,
                color: tokens.textPrimary,
              ),
            ),
          ],
        ),
      ),
    );
  }
}
