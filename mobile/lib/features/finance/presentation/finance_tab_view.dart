import 'dart:async';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../core/database/app_database.dart';
import '../../../core/l10n/vi_strings.dart';
import '../../../core/money/decimal10k.dart';
import '../../../core/network/api_client.dart';
import '../../../core/providers/app_providers.dart';
import '../../../core/sync/entity_types.dart';
import '../../../core/theme/app_tokens.dart';
import '../../../core/time/user_day_clock.dart';
import '../../../core/widgets/lifeos_visual_charts.dart';
import '../../../core/widgets/lifeos_widgets.dart';
import '../../workspace/data/lifeos_local_repository.dart';
import 'widgets/budget_pace_gauge.dart';
import 'widgets/finance_forms_sheets.dart';
import 'widgets/recurring_and_saving_section.dart';
import '../../ai/data/ai_api.dart';
import '../../ai/data/ai_models.dart';
import '../../ai/data/ai_providers.dart';
import '../../ai/presentation/widgets/ai_consent_sheet.dart';
import '../../ai/presentation/widgets/ai_plans_and_insights_sheets.dart';

/// Tab 2: Màn hình **Tài chính (`My Finance`)** — Botanical Premium v2
/// Ví/Tài khoản, Phân bổ chi tiêu Donut, Tốc độ ngân sách, Dòng tiền 7 ngày.
class FinanceTabView extends ConsumerStatefulWidget {
  const FinanceTabView({super.key});

  @override
  ConsumerState<FinanceTabView> createState() => _FinanceTabViewState();
}

class _FinanceTabViewState extends ConsumerState<FinanceTabView> {
  bool _loadingAiInsight = false;

  @override
  Widget build(BuildContext context) {
    final String userId = ref.watch(currentUserIdProvider);
    final LifeOsLocalRepository repo = ref.watch(localRepositoryProvider);
    final bool masked = ref.watch(privacyMaskAmountsProvider);
    final LifeOsTokens tokens = LifeOsTokens.of(context);

    final List<LocalEntityRow> accounts = ref
            .watch(entitiesByTypeStreamProvider(SyncEntityType.financeAccount))
            .valueOrNull ??
        const <LocalEntityRow>[];

    final List<LocalEntityRow> transactions = ref
            .watch(
                entitiesByTypeStreamProvider(SyncEntityType.financeTransaction))
            .valueOrNull ??
        const <LocalEntityRow>[];
    final List<LocalEntityRow> categories = ref
            .watch(entitiesByTypeStreamProvider(SyncEntityType.financeCategory))
            .valueOrNull ??
        const <LocalEntityRow>[];
    final Map<String, String> catNameById = <String, String>{
      for (final LocalEntityRow c in categories)
        c.id: (c.payload['name'] ?? 'Danh mục').toString(),
    };
    final List<LocalEntityRow> budgets = ref
            .watch(entitiesByTypeStreamProvider(SyncEntityType.budget))
            .valueOrNull ??
        const <LocalEntityRow>[];
    final String todayIso =
        repo.getUserDayClock(userId).businessDateForUtc(ref.watch(referenceUtcProvider));
    final String monthPrefix =
        todayIso.length >= 7 ? todayIso.substring(0, 7) : todayIso;

    // Tính tổng số dư tất cả ví đang hoạt động (Decimal10k nguyên)
    int totalBalanceScaled = 0;
    for (final LocalEntityRow acc in accounts) {
      if (acc.payload['archived'] == true || acc.payload['archivedAt'] != null) {
        continue;
      }
      final Decimal10k bal = repo.computeAccountBalance(
        userId: userId,
        accountId: acc.id,
      );
      totalBalanceScaled += bal.scaledValue;
    }
    final Decimal10k totalBalance = Decimal10k.fromScaled(totalBalanceScaled);

    // Tính phân bổ chi tiêu danh mục TRONG THÁNG HIỆN TẠI (0 double với tiền)
    final Map<String, int> categoryAmounts = <String, int>{};
    int totalExpenseScaled = 0;

    for (final LocalEntityRow tx in transactions) {
      final String txKind =
          (tx.payload['kind'] ?? tx.payload['type'] ?? 'EXPENSE')
              .toString()
              .toUpperCase();
      final String occurredOn =
          ((tx.payload['occurredOn'] ?? tx.payload['transactionDate']) as String?) ??
              '';
      if (txKind == 'EXPENSE' && occurredOn.startsWith(monthPrefix)) {
        final Object? rawAmt = tx.payload['amount'];
        int scaled = 0;
        if (rawAmt != null) {
          try {
            scaled = Decimal10k.parseJsonString(rawAmt.toString()).scaledValue;
          } catch (_) {
            scaled = 0;
          }
        }
        final String? cid = tx.payload['categoryId'] as String?;
        final String noteLower =
            (tx.payload['note'] ?? '').toString().toLowerCase();
        final String inferredBucket = noteLower.contains('cà phê') ||
                noteLower.contains('bánh') ||
                noteLower.contains('ăn')
            ? 'Ăn uống'
            : noteLower.contains('sách') || noteLower.contains('học')
                ? 'Học tập & Sách'
                : 'Chi tiêu khác';
        final String cat = (tx.payload['categoryName'] as String?) ??
            (cid != null ? catNameById[cid] : null) ??
            inferredBucket;
        categoryAmounts[cat] = (categoryAmounts[cat] ?? 0) + scaled;
        totalExpenseScaled += scaled;
      }
    }

    int budgetLimitScaled = 0;
    if (budgets.isNotEmpty) {
      int sumBudget = 0;
      for (final LocalEntityRow b in budgets) {
        if (b.payload['archived'] == true || b.payload['archivedAt'] != null) {
          continue;
        }
        final Object? raw = b.payload['amount'];
        if (raw != null) {
          try {
            sumBudget += Decimal10k.parseJsonString(raw.toString()).scaledValue;
          } catch (_) {}
        }
      }
      if (sumBudget > 0) budgetLimitScaled = sumBudget;
    }
    final Decimal10k totalExpense = Decimal10k.fromScaled(totalExpenseScaled);
    final Decimal10k budgetLimit = Decimal10k.fromScaled(budgetLimitScaled);

    // Chỉ dựng lát cắt Donut từ dữ liệu chi tiêu thực (không dùng số liệu mẫu giả lập)
    final List<DonutSliceData> donutSlices = <DonutSliceData>[];
    if (totalExpenseScaled > 0) {
      final List<Color> sliceColors = <Color>[
        tokens.domainFinance,
        tokens.domainHabit,
        tokens.domainGoal,
        tokens.domainKnowledge,
        tokens.info,
      ];
      int colorIdx = 0;
      categoryAmounts.forEach((String cat, int scaled) {
        final Decimal10k catAmount = Decimal10k.fromScaled(scaled);
        final Decimal10k totalAmount = Decimal10k.fromScaled(totalExpenseScaled);
        final double pct =
            catAmount.percentageBasisPointsOf(totalAmount) / 10000.0;
        donutSlices.add(DonutSliceData(
          label: cat,
          amount: scaled ~/ Decimal10k.scaleFactor,
          percentage: pct,
          color: sliceColors[colorIdx % sliceColors.length],
        ));
        colorIdx++;
      });
    }

    // Dòng tiền chi tiêu thực tế 7 ngày gần nhất quanh `todayIso`
    final DateTime todayDate = UserDayClock.parseIsoDateUtc(todayIso);
    final List<int> last7DaysExpenseUnits = <int>[];
    final List<String> last7DayLabels = <String>[];
    const List<String> wdShort = <String>[
      'T2',
      'T3',
      'T4',
      'T5',
      'T6',
      'T7',
      'CN',
    ];
    for (int offset = 6; offset >= 0; offset--) {
      final DateTime day = todayDate.subtract(Duration(days: offset));
      final String dayIso =
          UserDayClock.formatIsoDate(day.year, day.month, day.day);
      last7DayLabels.add(wdShort[(day.weekday - 1).clamp(0, 6)]);
      int dayScaled = 0;
      for (final LocalEntityRow tx in transactions) {
        final String kind =
            (tx.payload['kind'] ?? tx.payload['type'] ?? 'EXPENSE')
                .toString()
                .toUpperCase();
        final String? occurredOn =
            (tx.payload['occurredOn'] ?? tx.payload['transactionDate'])
                as String?;
        if (kind == 'EXPENSE' && occurredOn == dayIso) {
          dayScaled +=
              Decimal10k.parseJsonString((tx.payload['amount'] ?? '0').toString())
                  .scaledValue;
        }
      }
      last7DaysExpenseUnits.add(dayScaled ~/ Decimal10k.scaleFactor);
    }

    final int nowDay = todayDate.day;
    final double monthElapsedRatio = (nowDay / 30.0).clamp(0.05, 1.0);

    return ListView(
      padding: const EdgeInsets.fromLTRB(
        LifeOsTokens.space4,
        LifeOsTokens.space4,
        LifeOsTokens.space4,
        112,
      ),
      children: <Widget>[
        // Thẻ Tổng quan Tài chính Premium
        LifeOsCard(
          elevation: 2,
          padding: const EdgeInsets.all(LifeOsTokens.space4),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: <Widget>[
              Row(
                mainAxisAlignment: MainAxisAlignment.spaceBetween,
                children: <Widget>[
                  Expanded(
                    child: Row(
                      children: <Widget>[
                        Container(
                          padding: const EdgeInsets.all(8),
                          decoration: BoxDecoration(
                            color: tokens.domainFinance.withValues(alpha: 0.12),
                            shape: BoxShape.circle,
                          ),
                          child: Icon(
                            Icons.account_balance_wallet_rounded,
                            size: 20,
                            color: tokens.domainFinance,
                          ),
                        ),
                        const SizedBox(width: LifeOsTokens.space2),
                        Flexible(
                          child: Text(
                            'Tổng tài sản khả dụng',
                            style: TextStyle(
                              fontSize: 13,
                              fontWeight: FontWeight.w600,
                              color: tokens.textSecondary,
                            ),
                          ),
                        ),
                      ],
                    ),
                  ),
                  IconButton(
                    tooltip: masked ? 'Hiện số tiền' : 'Ẩn số tiền',
                    onPressed: () {
                      ref.read(privacyMaskAmountsProvider.notifier).state =
                          !masked;
                    },
                    icon: Semantics(
                      label: masked ? 'Hiện số tiền' : 'Ẩn số tiền',
                      child: Icon(
                        masked
                            ? Icons.visibility_off_outlined
                            : Icons.visibility_outlined,
                        color: tokens.textSecondary,
                        size: 20,
                      ),
                    ),
                  ),
                ],
              ),
              const SizedBox(height: LifeOsTokens.space2),
              Text(
                totalBalance.formatDisplay(masked: masked),
                style: TextStyle(
                  fontSize: 28,
                  fontWeight: FontWeight.w800,
                  letterSpacing: -0.5,
                  color: tokens.textPrimary,
                  fontFeatures: const <FontFeature>[
                    FontFeature.tabularFigures(),
                  ],
                ),
              ),
              const SizedBox(height: LifeOsTokens.space3),
              Row(
                children: <Widget>[
                  Expanded(
                    child: OutlinedButton.icon(
                      style: OutlinedButton.styleFrom(
                        padding: const EdgeInsets.symmetric(vertical: 12),
                        side: BorderSide(color: tokens.borderSubtle),
                      ),
                      onPressed: () {
                        showModalBottomSheet<void>(
                          context: context,
                          isScrollControlled: true,
                          useSafeArea: true,
                          backgroundColor: Colors.transparent,
                          builder: (_) => const FinanceAccountSheet(),
                        );
                      },
                      icon: const Icon(Icons.add_card_rounded, size: 18),
                      label: const Text('Thêm ví'),
                    ),
                  ),
                  const SizedBox(width: LifeOsTokens.space2),
                  Expanded(
                    child: FilledButton.tonalIcon(
                      style: FilledButton.styleFrom(
                        padding: const EdgeInsets.symmetric(vertical: 12),
                      ),
                      onPressed: () {
                        showModalBottomSheet<void>(
                          context: context,
                          isScrollControlled: true,
                          useSafeArea: true,
                          backgroundColor: Colors.transparent,
                          builder: (_) => const FinanceTransactionSheet(),
                        );
                      },
                      icon: const Icon(Icons.receipt_long_rounded, size: 18),
                      label: const Text('Ghi giao dịch'),
                    ),
                  ),
                ],
              ),
              const SizedBox(height: LifeOsTokens.space2),
              Row(
                children: <Widget>[
                  Expanded(
                    child: OutlinedButton.icon(
                      key: const Key('btn_open_budgets'),
                      onPressed: () {
                        showModalBottomSheet<void>(
                          context: context,
                          isScrollControlled: true,
                          useSafeArea: true,
                          backgroundColor: Colors.transparent,
                          builder: (_) => const BudgetManagementSheet(),
                        );
                      },
                      icon: const Icon(Icons.pie_chart_outline_rounded, size: 18),
                      label: const Text('Ngân sách'),
                    ),
                  ),
                  const SizedBox(width: LifeOsTokens.space2),
                  Expanded(
                    child: OutlinedButton.icon(
                      key: const Key('btn_open_tx_history'),
                      onPressed: () {
                        showModalBottomSheet<void>(
                          context: context,
                          isScrollControlled: true,
                          useSafeArea: true,
                          backgroundColor: Colors.transparent,
                          builder: (_) => const TransactionHistorySheet(),
                        );
                      },
                      icon: const Icon(Icons.history_rounded, size: 18),
                      label: const Text('Lịch sử'),
                    ),
                  ),
                ],
              ),
              if (ref.watch(aiPreferencesProvider).valueOrNull?.finance == true) ...<Widget>[
                const SizedBox(height: LifeOsTokens.space2),
                SizedBox(
                  width: double.infinity,
                  child: TextButton.icon(
                    key: const Key('btn_ai_finance_insight'),
                    onPressed: _loadingAiInsight
                        ? null
                        : () => _handleAiFinanceInsight(context),
                    icon: _loadingAiInsight
                        ? const SizedBox(
                            width: 14,
                            height: 14,
                            child: CircularProgressIndicator(strokeWidth: 2),
                          )
                        : Icon(
                            Icons.auto_awesome_rounded,
                            size: 16,
                            color: tokens.domainFinance,
                          ),
                    label: Text(
                      _loadingAiInsight
                          ? 'Đang phân tích tài chính...'
                          : 'Phân tích Tài chính với AI',
                      style: TextStyle(
                        fontSize: 12.5,
                        color: tokens.domainFinance,
                      ),
                    ),
                  ),
                ),
              ],
            ],
          ),
        ),

        const SizedBox(height: LifeOsTokens.space4),

        // Thước đo Tốc độ Ngân sách
        BudgetPaceGauge(
          spentFormatted: totalExpense.formatDisplay(masked: masked),
          limitFormatted: budgetLimitScaled > 0
              ? budgetLimit.formatDisplay(masked: masked)
              : 'Chưa đặt',
          spentRatio: budgetLimitScaled > 0
              ? (totalExpense.percentageBasisPointsOf(budgetLimit) / 10000.0)
                  .clamp(0.0, 1.5)
              : 0.0,
          monthElapsedRatio: monthElapsedRatio,
        ),

        const SizedBox(height: LifeOsTokens.space4),

        // Biểu đồ Vành khuyên (Donut Chart) phân bổ chi tiêu
        LifeOsCard(
          elevation: 1,
          padding: const EdgeInsets.all(LifeOsTokens.space4),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: <Widget>[
              Row(
                children: <Widget>[
                  Icon(
                    Icons.pie_chart_outline_rounded,
                    size: 20,
                    color: tokens.domainFinance,
                  ),
                  const SizedBox(width: 8),
                  Expanded(
                    child: Text(
                      'Phân bổ chi tiêu danh mục',
                      style: TextStyle(
                        fontSize: 16,
                        fontWeight: FontWeight.w700,
                        color: tokens.textPrimary,
                      ),
                    ),
                  ),
                ],
              ),
              const SizedBox(height: LifeOsTokens.space4),
              Center(
                child: LifeOsDonutChart(
                  slices: donutSlices,
                  centerSubtitle:
                      totalExpenseScaled > 0 ? 'Đã chi thực tế' : 'Chưa có chi tiêu',
                  centerTitle: totalExpense.formatDisplay(masked: masked),
                  size: 200,
                  strokeWidth: 26,
                ),
              ),
            ],
          ),
        ),

        const SizedBox(height: LifeOsTokens.space4),

        // Danh sách Ví & Tài khoản
        Wrap(
          spacing: 8,
          runSpacing: 4,
          alignment: WrapAlignment.spaceBetween,
          crossAxisAlignment: WrapCrossAlignment.center,
          children: <Widget>[
            Text(
              'Danh sách Ví & Tài khoản',
              style: TextStyle(
                fontSize: 16,
                fontWeight: FontWeight.w700,
                color: tokens.textPrimary,
              ),
            ),
            Text(
              '${accounts.length} tài khoản',
              style: TextStyle(
                fontSize: 13,
                fontWeight: FontWeight.w500,
                color: tokens.textSecondary,
              ),
            ),
          ],
        ),
        const SizedBox(height: LifeOsTokens.space2),
        if (accounts.isEmpty)
          const LifeOsEmptyState(
            icon: Icons.account_balance_wallet_outlined,
            title: 'Chưa có ví hoặc tài khoản ngân hàng',
            subtitle: 'Ghi chép chi tiêu, chuyển khoản và theo dõi dòng tiền minh bạch.',
          )
        else
          Column(
            children: accounts.map((LocalEntityRow acc) {
              final Decimal10k bal = repo.computeAccountBalance(
                userId: userId,
                accountId: acc.id,
              );
              final String rawType = (acc.payload['accountType'] ??
                      acc.payload['type'] ??
                      'BANK')
                  .toString()
                  .toUpperCase();
              final (String label, IconData icon) = switch (rawType) {
                'CASH' => ('Tiền mặt', Icons.payments_outlined),
                'BANK' => ('Ngân hàng', Icons.account_balance_outlined),
                'CREDIT' || 'CREDIT_CARD' => (
                    'Thẻ tín dụng',
                    Icons.credit_card_outlined
                  ),
                'E_WALLET' || 'WALLET' => (
                    'Ví điện tử',
                    Icons.account_balance_wallet_outlined
                  ),
                'INVESTMENT' => ('Đầu tư', Icons.trending_up_rounded),
                _ => ('Khác', Icons.savings_outlined),
              };

              final bool isArchived = acc.payload['archived'] == true ||
                  acc.payload['archivedAt'] != null;
              return Padding(
                padding: const EdgeInsets.only(bottom: LifeOsTokens.space2),
                child: InkWell(
                  borderRadius: BorderRadius.circular(LifeOsTokens.radiusMd),
                  onTap: () {
                    showModalBottomSheet<void>(
                      context: context,
                      isScrollControlled: true,
                      useSafeArea: true,
                      backgroundColor: Colors.transparent,
                      builder: (_) => FinanceAccountSheet(account: acc),
                    );
                  },
                  child: LifeOsCard(
                  elevation: 1,
                  padding: const EdgeInsets.symmetric(
                    horizontal: LifeOsTokens.space3,
                    vertical: LifeOsTokens.space3,
                  ),
                  child: Row(
                    children: <Widget>[
                      Container(
                        padding: const EdgeInsets.all(10),
                        decoration: BoxDecoration(
                          color: tokens.domainFinance.withValues(alpha: 0.1),
                          borderRadius:
                              BorderRadius.circular(LifeOsTokens.radiusMd),
                        ),
                        child: Icon(icon, color: tokens.domainFinance, size: 22),
                      ),
                      const SizedBox(width: LifeOsTokens.space3),
                      Expanded(
                        child: Column(
                          crossAxisAlignment: CrossAxisAlignment.start,
                          children: <Widget>[
                            Text(
                              (acc.payload['name'] as String?) ?? 'Tài khoản',
                              style: TextStyle(
                                fontWeight: FontWeight.w600,
                                fontSize: 15,
                                color: tokens.textPrimary,
                              ),
                            ),
                            const SizedBox(height: 2),
                            Text(
                              isArchived ? '$label · Đã lưu trữ' : label,
                              style: TextStyle(
                                fontSize: 12,
                                color: tokens.textSecondary,
                              ),
                            ),
                          ],
                        ),
                      ),
                      Text(
                        bal.formatDisplay(masked: masked),
                        style: TextStyle(
                          fontWeight: FontWeight.w700,
                          fontSize: 16,
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
              );
            }).toList(),
          ),

        const SizedBox(height: LifeOsTokens.space4),

        // Xu hướng dòng tiền 7 ngày
        LifeOsCard(
          elevation: 1,
          padding: const EdgeInsets.all(LifeOsTokens.space4),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: <Widget>[
              Row(
                children: <Widget>[
                  Icon(
                    Icons.bar_chart_rounded,
                    size: 20,
                    color: tokens.domainFinance,
                  ),
                  const SizedBox(width: 8),
                  Expanded(
                    child: Text(
                      'Xu hướng dòng tiền 7 ngày',
                      style: TextStyle(
                        fontSize: 16,
                        fontWeight: FontWeight.w700,
                        color: tokens.textPrimary,
                      ),
                    ),
                  ),
                ],
              ),
              const SizedBox(height: LifeOsTokens.space3),
              if (last7DaysExpenseUnits.every((int v) => v == 0))
                Text(
                  'Chưa phát sinh giao dịch chi tiêu trong 7 ngày qua.',
                  style: TextStyle(fontSize: 13, color: tokens.textSecondary),
                )
              else
                SimpleBarTrendChart(
                  values: last7DaysExpenseUnits,
                  labels: last7DayLabels,
                ),
            ],
          ),
        ),

        const SizedBox(height: LifeOsTokens.space4),

        // Giao dịch định kỳ & Mục tiêu tiết kiệm (MWP-C)
        const FinanceRecurringAndSavingSection(),
      ],
    );
  }

  Future<void> _handleAiFinanceInsight(BuildContext context) async {
    final AiPreferencesResponse prefs =
        ref.read(aiPreferencesProvider).valueOrNull ??
            AiPreferencesResponse.initialDefault();
    if (!prefs.finance) {
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(
          content: Text(
            'Phân tích tài chính cần cấp quyền miền "Tài chính cá nhân" trong cài đặt quyền riêng tư AI.',
          ),
        ),
      );
      unawaited(
        showModalBottomSheet<void>(
          context: context,
          isScrollControlled: true,
          useSafeArea: true,
          backgroundColor: Colors.transparent,
          builder: (_) => const AiConsentSheet(),
        ),
      );
      return;
    }

    if (!ref.read(aiIsOnlineProvider)) {
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(
          content: Text('Cần kết nối mạng để dùng AI phân tích tài chính.'),
        ),
      );
      return;
    }

    if (_loadingAiInsight) return;
    setState(() => _loadingAiInsight = true);

    final String userId = ref.read(currentUserIdProvider);
    final LifeOsLocalRepository repo = ref.read(localRepositoryProvider);
    final String today = repo
        .getUserDayClock(userId)
        .businessDateForUtc(ref.read(referenceUtcProvider));
    final String fromDate = UserDayClock.addDays(today, -29);
    final String currency =
        ((repo.getUserSettings(userId)['defaultCurrency'] as String?) ?? 'VND')
            .trim()
            .toUpperCase();
    final AiApiClient api = ref.read(aiApiClientProvider);

    try {
      final AiInsightResponse res = await api.createFinanceInsight(
        from: fromDate,
        to: today,
        currency: currency.isEmpty ? 'VND' : currency,
        contextDomains: <String>[AiContextDomain.finance.code],
      );
      if (mounted && context.mounted) {
        unawaited(
          showModalBottomSheet<void>(
            context: context,
            isScrollControlled: true,
            useSafeArea: true,
            backgroundColor: Colors.transparent,
            builder: (_) =>
                AiInsightSheet(title: 'Phân tích Tài chính AI', insight: res),
          ),
        );
      }
    } catch (e) {
      if (mounted && context.mounted) {
        final AiErrorDescriptor err =
            ViStrings.mapAiError(e is LifeOsApiException ? e.errorCode : null);
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(content: Text(err.message)),
        );
      }
    } finally {
      if (mounted) {
        setState(() => _loadingAiInsight = false);
      }
    }
  }
}
