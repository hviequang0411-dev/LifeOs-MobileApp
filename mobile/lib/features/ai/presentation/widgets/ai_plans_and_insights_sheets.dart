import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../../core/providers/app_providers.dart';
import '../../../../core/theme/app_tokens.dart';
import '../../../../core/widgets/lifeos_widgets.dart';
import '../../../workspace/data/lifeos_local_repository.dart';
import '../../data/ai_models.dart';

/// Sheet hiển thị Kế hoạch Ngày / Tuần do AI gợi ý (AWP-4).
///
/// Người dùng có thể nhấn "Tạo công việc từ gợi ý", việc tạo được thực hiện hoàn toàn
/// qua luồng thông thường của người dùng (`localRepository.createTask` + Outbox), KHÔNG qua AI action proposals.
class AiPlanResultSheet extends ConsumerWidget {
  const AiPlanResultSheet({
    super.key,
    required this.plan,
  });

  final AiPlanResponse plan;

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final LifeOsTokens tokens = LifeOsTokens.of(context);
    final LifeOsLocalRepository repo = ref.read(localRepositoryProvider);
    final String userId = ref.read(currentUserIdProvider);

    return Container(
      decoration: BoxDecoration(
        color: tokens.bgSurface,
        borderRadius: const BorderRadius.vertical(
          top: Radius.circular(LifeOsTokens.radiusLg),
        ),
      ),
      constraints: BoxConstraints(
        maxHeight: MediaQuery.sizeOf(context).height * 0.85,
      ),
      child: Column(
        children: <Widget>[
          Padding(
            padding: const EdgeInsets.fromLTRB(
              LifeOsTokens.space4,
              LifeOsTokens.space3,
              LifeOsTokens.space2,
              LifeOsTokens.space2,
            ),
            child: Row(
              children: <Widget>[
                Icon(
                  Icons.auto_awesome_rounded,
                  color: tokens.accentPrimary,
                  size: 22,
                ),
                const SizedBox(width: LifeOsTokens.space2),
                Expanded(
                  child: Text(
                    plan.periodType == 'WEEK'
                        ? 'Gợi ý Kế hoạch Tuần'
                        : 'Gợi ý Kế hoạch Hôm nay',
                    style: TextStyle(
                      fontSize: 17,
                      fontWeight: FontWeight.w700,
                      color: tokens.textPrimary,
                    ),
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
                // Summary card
                LifeOsCard(
                  elevation: 1,
                  padding: const EdgeInsets.all(LifeOsTokens.space3),
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: <Widget>[
                      Row(
                        children: <Widget>[
                          Container(
                            padding: const EdgeInsets.symmetric(
                              horizontal: 8,
                              vertical: 3,
                            ),
                            decoration: BoxDecoration(
                              color: tokens.accentPrimary.withValues(alpha: 0.14),
                              borderRadius: BorderRadius.circular(4),
                            ),
                            child: const Text(
                              'ĐỀ XUẤT CHỈ ĐỌC',
                              style: TextStyle(
                                fontSize: 11,
                                fontWeight: FontWeight.w700,
                              ),
                            ),
                          ),
                          const SizedBox(width: 8),
                          Text(
                            plan.periodStart,
                            style: TextStyle(
                              fontSize: 12.5,
                              color: tokens.textSecondary,
                            ),
                          ),
                        ],
                      ),
                      const SizedBox(height: LifeOsTokens.space2),
                      Text(
                        plan.summary,
                        style: TextStyle(
                          fontSize: 14,
                          height: 1.4,
                          color: tokens.textPrimary,
                        ),
                      ),
                    ],
                  ),
                ),
                const SizedBox(height: LifeOsTokens.space4),

                Text(
                  'Các mục được đề xuất (${plan.items.length}):',
                  style: TextStyle(
                    fontSize: 14,
                    fontWeight: FontWeight.w700,
                    color: tokens.textPrimary,
                  ),
                ),
                const SizedBox(height: LifeOsTokens.space2),

                ...plan.items.map((AiPlanItem item) {
                  return Padding(
                    padding: const EdgeInsets.only(bottom: LifeOsTokens.space3),
                    child: LifeOsCard(
                      elevation: 1,
                      padding: const EdgeInsets.all(LifeOsTokens.space3),
                      child: Column(
                        crossAxisAlignment: CrossAxisAlignment.start,
                        children: <Widget>[
                          Row(
                            children: <Widget>[
                              Expanded(
                                child: Text(
                                  item.title,
                                  style: TextStyle(
                                    fontSize: 14.5,
                                    fontWeight: FontWeight.w700,
                                    color: tokens.textPrimary,
                                  ),
                                ),
                              ),
                              Container(
                                padding: const EdgeInsets.symmetric(
                                  horizontal: 6,
                                  vertical: 2,
                                ),
                                decoration: BoxDecoration(
                                  color: item.priority == 'HIGH'
                                      ? tokens.danger.withValues(alpha: 0.12)
                                      : tokens.borderSubtle,
                                  borderRadius: BorderRadius.circular(4),
                                ),
                                child: Text(
                                  item.priority,
                                  style: TextStyle(
                                    fontSize: 11,
                                    fontWeight: FontWeight.w700,
                                    color: item.priority == 'HIGH'
                                        ? tokens.danger
                                        : tokens.textSecondary,
                                  ),
                                ),
                              ),
                            ],
                          ),
                          const SizedBox(height: 4),
                          Text(
                            item.reason,
                            style: TextStyle(
                              fontSize: 12.5,
                              color: tokens.textSecondary,
                            ),
                          ),
                          if (item.suggestedStartAt != null) ...<Widget>[
                            const SizedBox(height: 4),
                            Text(
                              'Dự kiến: ${item.suggestedStartAt!.length >= 16 ? item.suggestedStartAt!.substring(11, 16) : item.suggestedStartAt}',
                              style: TextStyle(
                                fontSize: 11.5,
                                color: tokens.accentPrimary,
                                fontWeight: FontWeight.w600,
                              ),
                            ),
                          ],
                          const SizedBox(height: LifeOsTokens.space2),
                          Align(
                            alignment: Alignment.centerRight,
                            child: OutlinedButton.icon(
                              style: OutlinedButton.styleFrom(
                                visualDensity: VisualDensity.compact,
                              ),
                              onPressed: () {
                                repo.createTask(
                                  userId: userId,
                                  title: item.title,
                                  priority: item.priority,
                                );
                                ScaffoldMessenger.of(context).showSnackBar(
                                  SnackBar(
                                    content: Text(
                                      'Đã tạo công việc "${item.title}" vào danh sách của bạn (tự tạo thủ công qua Outbox).',
                                    ),
                                  ),
                                );
                              },
                              icon: const Icon(Icons.add_task_rounded, size: 16),
                              label: const Text('Tạo việc từ gợi ý'),
                            ),
                          ),
                        ],
                      ),
                    ),
                  );
                }),
              ],
            ),
          ),
        ],
      ),
    );
  }
}

/// Sheet hiển thị Chia nhỏ công việc (Task Breakdown, AWP-4).
class AiTaskBreakdownSheet extends ConsumerWidget {
  const AiTaskBreakdownSheet({
    super.key,
    required this.breakdown,
  });

  final AiTaskBreakdownResponse breakdown;

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final LifeOsTokens tokens = LifeOsTokens.of(context);
    final LifeOsLocalRepository repo = ref.read(localRepositoryProvider);
    final String userId = ref.read(currentUserIdProvider);

    return Container(
      decoration: BoxDecoration(
        color: tokens.bgSurface,
        borderRadius: const BorderRadius.vertical(
          top: Radius.circular(LifeOsTokens.radiusLg),
        ),
      ),
      constraints: BoxConstraints(
        maxHeight: MediaQuery.sizeOf(context).height * 0.85,
      ),
      child: Column(
        children: <Widget>[
          Padding(
            padding: const EdgeInsets.fromLTRB(
              LifeOsTokens.space4,
              LifeOsTokens.space3,
              LifeOsTokens.space2,
              LifeOsTokens.space2,
            ),
            child: Row(
              children: <Widget>[
                Icon(
                  Icons.alt_route_rounded,
                  color: tokens.accentPrimary,
                  size: 22,
                ),
                const SizedBox(width: LifeOsTokens.space2),
                Expanded(
                  child: Text(
                    'Chia nhỏ công việc (AI Breakdown)',
                    style: TextStyle(
                      fontSize: 17,
                      fontWeight: FontWeight.w700,
                      color: tokens.textPrimary,
                    ),
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
                Text(
                  'Đề xuất chia nhỏ thành ${breakdown.items.length} việc độc lập:',
                  style: TextStyle(
                    fontSize: 13.5,
                    fontWeight: FontWeight.w600,
                    color: tokens.textSecondary,
                  ),
                ),
                const SizedBox(height: LifeOsTokens.space3),
                ...breakdown.items.map((AiTaskDraft item) {
                  return Padding(
                    padding: const EdgeInsets.only(bottom: LifeOsTokens.space3),
                    child: LifeOsCard(
                      elevation: 1,
                      padding: const EdgeInsets.all(LifeOsTokens.space3),
                      child: Column(
                        crossAxisAlignment: CrossAxisAlignment.start,
                        children: <Widget>[
                          Text(
                            item.title,
                            style: TextStyle(
                              fontSize: 14.5,
                              fontWeight: FontWeight.w700,
                              color: tokens.textPrimary,
                            ),
                          ),
                          if (item.description != null &&
                              item.description!.isNotEmpty) ...<Widget>[
                            const SizedBox(height: 4),
                            Text(
                              item.description!,
                              style: TextStyle(
                                fontSize: 12.5,
                                color: tokens.textSecondary,
                              ),
                            ),
                          ],
                          const SizedBox(height: 4),
                          Text(
                            'Lý do: ${item.reason}',
                            style: TextStyle(
                              fontSize: 12,
                              fontStyle: FontStyle.italic,
                              color: tokens.textSecondary,
                            ),
                          ),
                          const SizedBox(height: LifeOsTokens.space2),
                          Align(
                            alignment: Alignment.centerRight,
                            child: FilledButton.tonalIcon(
                              style: FilledButton.styleFrom(
                                visualDensity: VisualDensity.compact,
                              ),
                              onPressed: () {
                                repo.createTask(
                                  userId: userId,
                                  title: item.title,
                                  description: item.description,
                                  priority: item.priority,
                                  dueDate: item.suggestedDueDate,
                                );
                                ScaffoldMessenger.of(context).showSnackBar(
                                  SnackBar(
                                    content: Text(
                                      'Đã tạo việc "${item.title}" qua hàng đợi đồng bộ.',
                                    ),
                                  ),
                                );
                              },
                              icon: const Icon(Icons.add_rounded, size: 16),
                              label: const Text('Thêm vào danh sách việc'),
                            ),
                          ),
                        ],
                      ),
                    ),
                  );
                }),
              ],
            ),
          ),
        ],
      ),
    );
  }
}

/// Sheet hiển thị Nhận xét & Xu hướng Thói quen / Tài chính (AWP-4).
class AiInsightSheet extends StatelessWidget {
  const AiInsightSheet({
    super.key,
    required this.title,
    required this.insight,
  });

  final String title;
  final AiInsightResponse insight;

  @override
  Widget build(BuildContext context) {
    final LifeOsTokens tokens = LifeOsTokens.of(context);

    return Container(
      decoration: BoxDecoration(
        color: tokens.bgSurface,
        borderRadius: const BorderRadius.vertical(
          top: Radius.circular(LifeOsTokens.radiusLg),
        ),
      ),
      constraints: BoxConstraints(
        maxHeight: MediaQuery.sizeOf(context).height * 0.85,
      ),
      child: Column(
        children: <Widget>[
          Padding(
            padding: const EdgeInsets.fromLTRB(
              LifeOsTokens.space4,
              LifeOsTokens.space3,
              LifeOsTokens.space2,
              LifeOsTokens.space2,
            ),
            child: Row(
              children: <Widget>[
                Icon(
                  Icons.insights_rounded,
                  color: tokens.accentPrimary,
                  size: 22,
                ),
                const SizedBox(width: LifeOsTokens.space2),
                Expanded(
                  child: Text(
                    title,
                    style: TextStyle(
                      fontSize: 17,
                      fontWeight: FontWeight.w700,
                      color: tokens.textPrimary,
                    ),
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
                LifeOsCard(
                  elevation: 1,
                  padding: const EdgeInsets.all(LifeOsTokens.space3),
                  child: Text(
                    insight.summary,
                    style: TextStyle(
                      fontSize: 14,
                      height: 1.45,
                      color: tokens.textPrimary,
                    ),
                  ),
                ),
                const SizedBox(height: LifeOsTokens.space4),
                Text(
                  'Các quan sát chi tiết (${insight.observations.length}):',
                  style: TextStyle(
                    fontSize: 14,
                    fontWeight: FontWeight.w700,
                    color: tokens.textPrimary,
                  ),
                ),
                const SizedBox(height: LifeOsTokens.space2),
                ...insight.observations.map((AiObservation obs) {
                  return Padding(
                    padding: const EdgeInsets.only(bottom: LifeOsTokens.space3),
                    child: LifeOsCard(
                      elevation: 1,
                      padding: const EdgeInsets.all(LifeOsTokens.space3),
                      child: Column(
                        crossAxisAlignment: CrossAxisAlignment.start,
                        children: <Widget>[
                          Row(
                            children: <Widget>[
                              Expanded(
                                child: Text(
                                  obs.title,
                                  style: TextStyle(
                                    fontSize: 14,
                                    fontWeight: FontWeight.w700,
                                    color: tokens.textPrimary,
                                  ),
                                ),
                              ),
                              _buildTrendIcon(obs.trend, tokens),
                            ],
                          ),
                          const SizedBox(height: 4),
                          Text(
                            obs.detail,
                            style: TextStyle(
                              fontSize: 12.5,
                              color: tokens.textSecondary,
                            ),
                          ),
                        ],
                      ),
                    ),
                  );
                }),
              ],
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildTrendIcon(String trend, LifeOsTokens tokens) {
    switch (trend) {
      case 'UP':
        return Icon(Icons.trending_up_rounded, color: tokens.domainGoal, size: 20);
      case 'DOWN':
        return Icon(Icons.trending_down_rounded, color: tokens.warning, size: 20);
      default:
        return Icon(Icons.trending_flat_rounded, color: tokens.textSecondary, size: 20);
    }
  }
}

/// Sheet hiển thị Tóm tắt Ghi chú (AWP-4).
class AiNoteSummarySheet extends StatelessWidget {
  const AiNoteSummarySheet({
    super.key,
    required this.summary,
  });

  final AiNoteSummaryResponse summary;

  @override
  Widget build(BuildContext context) {
    final LifeOsTokens tokens = LifeOsTokens.of(context);

    return Container(
      decoration: BoxDecoration(
        color: tokens.bgSurface,
        borderRadius: const BorderRadius.vertical(
          top: Radius.circular(LifeOsTokens.radiusLg),
        ),
      ),
      constraints: BoxConstraints(
        maxHeight: MediaQuery.sizeOf(context).height * 0.85,
      ),
      child: Column(
        children: <Widget>[
          Padding(
            padding: const EdgeInsets.fromLTRB(
              LifeOsTokens.space4,
              LifeOsTokens.space3,
              LifeOsTokens.space2,
              LifeOsTokens.space2,
            ),
            child: Row(
              children: <Widget>[
                Icon(
                  Icons.summarize_outlined,
                  color: tokens.accentPrimary,
                  size: 22,
                ),
                const SizedBox(width: LifeOsTokens.space2),
                Expanded(
                  child: Text(
                    'Tóm tắt Ghi chú AI',
                    style: TextStyle(
                      fontSize: 17,
                      fontWeight: FontWeight.w700,
                      color: tokens.textPrimary,
                    ),
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
                LifeOsCard(
                  elevation: 1,
                  padding: const EdgeInsets.all(LifeOsTokens.space3),
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: <Widget>[
                      Text(
                        'Nội dung tóm lược:',
                        style: TextStyle(
                          fontSize: 12.5,
                          fontWeight: FontWeight.w700,
                          color: tokens.textSecondary,
                        ),
                      ),
                      const SizedBox(height: LifeOsTokens.space2),
                      Text(
                        summary.summary,
                        style: TextStyle(
                          fontSize: 14,
                          height: 1.45,
                          color: tokens.textPrimary,
                        ),
                      ),
                    ],
                  ),
                ),
                if (summary.keyPoints.isNotEmpty) ...<Widget>[
                  const SizedBox(height: LifeOsTokens.space4),
                  Text(
                    'Ý chính rút ra:',
                    style: TextStyle(
                      fontSize: 14,
                      fontWeight: FontWeight.w700,
                      color: tokens.textPrimary,
                    ),
                  ),
                  const SizedBox(height: LifeOsTokens.space2),
                  ...summary.keyPoints.map((String pt) {
                    return Padding(
                      padding: const EdgeInsets.symmetric(vertical: 4),
                      child: Row(
                        crossAxisAlignment: CrossAxisAlignment.start,
                        children: <Widget>[
                          Text('• ',
                              style: TextStyle(
                                  color: tokens.accentPrimary,
                                  fontWeight: FontWeight.bold)),
                          Expanded(
                            child: Text(
                              pt,
                              style: TextStyle(
                                fontSize: 13,
                                color: tokens.textPrimary,
                              ),
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

