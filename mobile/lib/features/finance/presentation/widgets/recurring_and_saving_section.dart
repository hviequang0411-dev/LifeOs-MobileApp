import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../../core/database/app_database.dart';
import '../../../../core/money/decimal10k.dart';
import '../../../../core/providers/app_providers.dart';
import '../../../../core/sync/entity_types.dart';
import '../../../../core/theme/app_tokens.dart';
import '../../../../core/widgets/lifeos_widgets.dart';
import '../../../workspace/data/lifeos_local_repository.dart';
import 'recurring_transaction_sheet.dart';
import 'saving_goal_sheet.dart';

/// Khối **Giao dịch định kỳ (`recurring_transaction`)** & **Mục tiêu tiết kiệm (`saving_goal`)**
/// nhúng cuối Tab Tài chính (`MWP-C`).
class FinanceRecurringAndSavingSection extends ConsumerWidget {
  const FinanceRecurringAndSavingSection({super.key});

  String _formatFrequencyVi(String freq, int interval) {
    final String unit = switch (freq.toUpperCase()) {
      'DAILY' => 'ngày',
      'WEEKLY' => 'tuần',
      'MONTHLY' => 'tháng',
      'YEARLY' => 'năm',
      _ => 'kỳ',
    };
    return interval <= 1 ? 'Mỗi $unit' : 'Mỗi $interval $unit';
  }

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final LifeOsTokens tokens = LifeOsTokens.of(context);
    final String userId = ref.watch(currentUserIdProvider);
    final LifeOsLocalRepository repo = ref.watch(localRepositoryProvider);
    final bool masked = ref.watch(privacyMaskAmountsProvider);

    final List<LocalEntityRow> recurringTxs = ref
            .watch(
              entitiesByTypeStreamProvider(
                SyncEntityType.recurringTransaction,
              ),
            )
            .valueOrNull ??
        const <LocalEntityRow>[];
    final List<LocalEntityRow> savingGoals = ref
            .watch(entitiesByTypeStreamProvider(SyncEntityType.savingGoal))
            .valueOrNull ??
        const <LocalEntityRow>[];
    ref.watch(
      entitiesByTypeStreamProvider(SyncEntityType.savingContribution),
    );

    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: <Widget>[
        // 1. Giao dịch định kỳ
        Wrap(
          spacing: 8,
          runSpacing: 8,
          alignment: WrapAlignment.spaceBetween,
          crossAxisAlignment: WrapCrossAlignment.center,
          children: <Widget>[
            Text(
              'Giao dịch định kỳ',
              style: Theme.of(context).textTheme.titleLarge,
            ),
            FilledButton.tonalIcon(
              key: const Key('btn_open_add_recurring_tx'),
              onPressed: () {
                showModalBottomSheet<void>(
                  context: context,
                  isScrollControlled: true,
                  useSafeArea: true,
                  backgroundColor: Colors.transparent,
                  builder: (BuildContext ctx) =>
                      const RecurringTransactionSheet(),
                );
              },
              icon: const Icon(Icons.repeat_rounded, size: 18),
              label: const Text('Thêm định kỳ'),
            ),
          ],
        ),
        const SizedBox(height: LifeOsTokens.space2),
        if (recurringTxs.isEmpty)
          const LifeOsEmptyState(
            icon: Icons.event_repeat_outlined,
            title: 'Chưa có giao dịch định kỳ',
            subtitle:
                'Thiết lập tiền thuê nhà, lương tháng hoặc các khoản thu/chi lặp lại tự động.',
          )
        else
          Column(
            children: recurringTxs.map((LocalEntityRow item) {
              final String kind =
                  (item.payload['kind'] as String? ?? 'EXPENSE').toUpperCase();
              final Decimal10k amount = Decimal10k.parseJsonString(
                (item.payload['amount'] ?? '0').toString(),
              );
              final String freq =
                  (item.payload['frequency'] as String? ?? 'MONTHLY')
                      .toUpperCase();
              final int interval =
                  (item.payload['intervalCount'] as num?)?.toInt() ?? 1;
              final bool isActive =
                  (item.payload['status'] as String? ?? 'ACTIVE')
                          .toUpperCase() ==
                      'ACTIVE';
              final String note = (item.payload['note'] as String?)?.trim() ??
                  (kind == 'INCOME' ? 'Thu nhập định kỳ' : 'Chi tiêu định kỳ');

              return Padding(
                padding: const EdgeInsets.only(bottom: LifeOsTokens.space2),
                child: LifeOsCard(
                  elevation: 1,
                  padding: const EdgeInsets.all(LifeOsTokens.space3),
                  child: InkWell(
                    onTap: () {
                      showModalBottomSheet<void>(
                        context: context,
                        isScrollControlled: true,
                        useSafeArea: true,
                        backgroundColor: Colors.transparent,
                        builder: (BuildContext ctx) =>
                            RecurringTransactionSheet(existing: item),
                      );
                    },
                    child: Row(
                      children: <Widget>[
                        Container(
                          padding: const EdgeInsets.all(10),
                          decoration: BoxDecoration(
                            color: tokens.domainFinance.withValues(alpha: 0.1),
                            borderRadius:
                                BorderRadius.circular(LifeOsTokens.radiusMd),
                          ),
                          child: Icon(
                            Icons.autorenew_rounded,
                            color: tokens.domainFinance,
                            size: 20,
                          ),
                        ),
                        const SizedBox(width: LifeOsTokens.space3),
                        Expanded(
                          child: Column(
                            crossAxisAlignment: CrossAxisAlignment.start,
                            children: <Widget>[
                              Text(
                                note,
                                style: TextStyle(
                                  fontWeight: FontWeight.w700,
                                  fontSize: 15,
                                  color: tokens.textPrimary,
                                ),
                              ),
                              const SizedBox(height: 2),
                              Text(
                                '${_formatFrequencyVi(freq, interval)} · Bắt đầu ${(item.payload['startOn'] as String?) ?? ''} · ${isActive ? 'Đang chạy' : 'Tạm dừng'}',
                                style: TextStyle(
                                  fontSize: 12.5,
                                  color: tokens.textSecondary,
                                ),
                              ),
                            ],
                          ),
                        ),
                        Text(
                          '${kind == 'INCOME' ? '+' : '-'}${amount.formatDisplay(masked: masked)}',
                          style: TextStyle(
                            fontWeight: FontWeight.w700,
                            fontSize: 15,
                            color: kind == 'INCOME'
                                ? tokens.success
                                : tokens.textPrimary,
                            fontFeatures: const <FontFeature>[
                              FontFeature.tabularFigures(),
                            ],
                          ),
                        ),
                      ],
                    ),
                  ),
                ),
              );
            }).toList(),
          ),

        const SizedBox(height: LifeOsTokens.space4),

        // 2. Mục tiêu tiết kiệm & Đóng góp
        Wrap(
          spacing: 8,
          runSpacing: 8,
          alignment: WrapAlignment.spaceBetween,
          crossAxisAlignment: WrapCrossAlignment.center,
          children: <Widget>[
            Text(
              'Mục tiêu tiết kiệm',
              style: Theme.of(context).textTheme.titleLarge,
            ),
            FilledButton.tonalIcon(
              key: const Key('btn_open_add_saving_goal'),
              onPressed: () {
                showModalBottomSheet<void>(
                  context: context,
                  isScrollControlled: true,
                  useSafeArea: true,
                  backgroundColor: Colors.transparent,
                  builder: (BuildContext ctx) => const SavingGoalSheet(),
                );
              },
              icon: const Icon(Icons.savings_outlined, size: 18),
              label: const Text('Thêm tiết kiệm'),
            ),
          ],
        ),
        const SizedBox(height: LifeOsTokens.space2),
        if (savingGoals.isEmpty)
          const LifeOsEmptyState(
            icon: Icons.savings_outlined,
            title: 'Chưa có mục tiêu tiết kiệm',
            subtitle: 'Theo dõi quỹ dự phòng hoặc khoản tiết kiệm lớn theo tiến độ thực tế.',
          )
        else
          Column(
            children: savingGoals.map((LocalEntityRow goal) {
              final String goalName =
                  (goal.payload['name'] as String?) ?? 'Tiết kiệm';
              final Decimal10k target = Decimal10k.parseJsonString(
                (goal.payload['targetAmount'] ?? '0').toString(),
              );
              final Decimal10k saved = repo.finance.computeSavingGoalSavedAmount(
                userId: userId,
                goalId: goal.id,
              );
              final int pctBp =
                  target.scaledValue > 0 ? saved.percentageBasisPointsOf(target) : 0;
              final int pctInt = (pctBp / 100).round().clamp(0, 100);

              return Padding(
                padding: const EdgeInsets.only(bottom: LifeOsTokens.space2),
                child: LifeOsCard(
                  elevation: 1,
                  padding: const EdgeInsets.all(LifeOsTokens.space3),
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: <Widget>[
                      Row(
                        children: <Widget>[
                          Expanded(
                            child: Column(
                              crossAxisAlignment: CrossAxisAlignment.start,
                              children: <Widget>[
                                Text(
                                  goalName,
                                  style: TextStyle(
                                    fontWeight: FontWeight.w700,
                                    fontSize: 15,
                                    color: tokens.textPrimary,
                                  ),
                                ),
                                const SizedBox(height: 2),
                                Text(
                                  '${saved.formatDisplay(masked: masked)} / ${target.formatDisplay(masked: masked)} ($pctInt%)',
                                  style: TextStyle(
                                    fontSize: 12.5,
                                    color: tokens.textSecondary,
                                    fontFeatures: const <FontFeature>[
                                      FontFeature.tabularFigures(),
                                    ],
                                  ),
                                ),
                              ],
                            ),
                          ),
                          FilledButton.tonalIcon(
                            key: Key('btn_contribute_${goal.id}'),
                            style: FilledButton.styleFrom(
                              minimumSize: const Size(
                                100,
                                LifeOsTokens.minTouchTarget,
                              ),
                            ),
                            onPressed: () {
                              showDialog<void>(
                                context: context,
                                builder: (BuildContext ctx) =>
                                    SavingContributionDialog(
                                  goalId: goal.id,
                                  goalName: goalName,
                                ),
                              );
                            },
                            icon: const Icon(Icons.add_rounded, size: 18),
                            label: const Text('Góp thêm'),
                          ),
                        ],
                      ),
                      const SizedBox(height: 10),
                      ClipRRect(
                        borderRadius: BorderRadius.circular(4),
                        child: LinearProgressIndicator(
                          minHeight: 8,
                          value: (pctBp / 10000.0).clamp(0.0, 1.0),
                          backgroundColor: tokens.borderSubtle,
                          valueColor: AlwaysStoppedAnimation<Color>(
                            tokens.domainFinance,
                          ),
                        ),
                      ),
                    ],
                  ),
                ),
              );
            }).toList(),
          ),
      ],
    );
  }
}

