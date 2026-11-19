import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../core/calculators/analytics_calculator.dart';
import '../../../core/database/app_database.dart';
import '../../../core/providers/app_providers.dart';
import '../../../core/sync/entity_types.dart';
import '../../../core/theme/app_tokens.dart';
import '../../../core/widgets/lifeos_widgets.dart';
import '../../workspace/data/lifeos_local_repository.dart';
import 'widgets/goal_milestones_sheet.dart';
import 'widgets/note_editor_sheet.dart';

/// Tab 3: Màn hình **Tri thức & Mục tiêu (`Notes` & `Goals`)** — Botanical Premium v2 (`UPW6`)
class KnowledgeAndGoalsTabView extends ConsumerStatefulWidget {
  const KnowledgeAndGoalsTabView({super.key});

  @override
  ConsumerState<KnowledgeAndGoalsTabView> createState() =>
      _KnowledgeAndGoalsTabViewState();
}

class _KnowledgeAndGoalsTabViewState
    extends ConsumerState<KnowledgeAndGoalsTabView> {
  bool _showArchivedNotes = false;

  @override
  Widget build(BuildContext context) {
    final String userId = ref.watch(currentUserIdProvider);
    final LifeOsLocalRepository repo = ref.watch(localRepositoryProvider);
    final LifeOsTokens tokens = LifeOsTokens.of(context);

    final List<LocalEntityRow> allNotes =
        ref.watch(entitiesByTypeStreamProvider(SyncEntityType.note)).valueOrNull ??
            const <LocalEntityRow>[];
    ref.watch(entitiesByTypeStreamProvider(SyncEntityType.noteLink));
    final List<LocalEntityRow> tags =
        ref.watch(entitiesByTypeStreamProvider(SyncEntityType.tag)).valueOrNull ??
            const <LocalEntityRow>[];
    final List<LocalEntityRow> noteTags = ref
            .watch(entitiesByTypeStreamProvider(SyncEntityType.noteTag))
            .valueOrNull ??
        const <LocalEntityRow>[];
    final List<LocalEntityRow> goals =
        ref.watch(entitiesByTypeStreamProvider(SyncEntityType.goal)).valueOrNull ??
            const <LocalEntityRow>[];
    final List<LocalEntityRow> milestones = ref
            .watch(entitiesByTypeStreamProvider(SyncEntityType.goalMilestone))
            .valueOrNull ??
        const <LocalEntityRow>[];

    final Map<String, LocalEntityRow> tagById = <String, LocalEntityRow>{
      for (final LocalEntityRow t in tags) t.id: t,
    };
    final Map<String, List<LocalEntityRow>> tagsByNoteId =
        <String, List<LocalEntityRow>>{};
    for (final LocalEntityRow nt in noteTags) {
      final String? nId = nt.payload['noteId'] as String?;
      final String? tId = nt.payload['tagId'] as String?;
      final LocalEntityRow? tag = tId != null ? tagById[tId] : null;
      if (nId != null && tag != null) {
        tagsByNoteId.putIfAbsent(nId, () => <LocalEntityRow>[]).add(tag);
      }
    }

    final int archivedCount =
        allNotes.where((LocalEntityRow n) => n.payload['archived'] == true).length;
    final List<LocalEntityRow> filteredNotes = _showArchivedNotes
        ? allNotes
        : allNotes
            .where((LocalEntityRow n) => n.payload['archived'] != true)
            .toList();
    final List<LocalEntityRow> notes = <LocalEntityRow>[
      ...filteredNotes.where((LocalEntityRow n) => n.payload['pinned'] == true),
      ...filteredNotes.where((LocalEntityRow n) => n.payload['pinned'] != true),
    ];

    final DateTime refUtc = ref.watch(referenceUtcProvider);
    final String todayIso =
        repo.getUserDayClock(userId).businessDateForUtc(refUtc);

    return ListView(
      padding: const EdgeInsets.fromLTRB(
        LifeOsTokens.space4,
        LifeOsTokens.space4,
        LifeOsTokens.space4,
        112,
      ),
      children: <Widget>[
        // Ghi chú & Liên kết
        Wrap(
          spacing: 8,
          runSpacing: 8,
          alignment: WrapAlignment.spaceBetween,
          crossAxisAlignment: WrapCrossAlignment.center,
          children: <Widget>[
            Row(
              mainAxisSize: MainAxisSize.min,
              children: <Widget>[
                Icon(
                  Icons.auto_stories_rounded,
                  size: 20,
                  color: tokens.domainKnowledge,
                ),
                const SizedBox(width: 8),
                Flexible(
                  child: Text(
                    'Ghi chú & Liên kết',
                    style: Theme.of(context).textTheme.titleLarge,
                  ),
                ),
              ],
            ),
            FilledButton.tonalIcon(
              key: const Key('btn_open_create_note'),
              onPressed: () {
                showModalBottomSheet<void>(
                  context: context,
                  isScrollControlled: true,
                  useSafeArea: true,
                  backgroundColor: Colors.transparent,
                  builder: (BuildContext ctx) => const NoteEditorSheet(),
                );
              },
              icon: const Icon(Icons.note_add_outlined, size: 18),
              label: const Text('Ghi chú mới'),
            ),
          ],
        ),
        const SizedBox(height: LifeOsTokens.space2),
        if (archivedCount > 0) ...<Widget>[
          Align(
            alignment: Alignment.centerLeft,
            child: FilterChip(
              key: const Key('chip_toggle_archived_notes'),
              selected: _showArchivedNotes,
              avatar: const Icon(Icons.inventory_2_outlined, size: 16),
              label: Text('Lưu trữ ($archivedCount)'),
              onSelected: (bool v) => setState(() => _showArchivedNotes = v),
            ),
          ),
          const SizedBox(height: LifeOsTokens.space2),
        ],
        if (allNotes.isEmpty)
          const LifeOsEmptyState(
            icon: Icons.menu_book_outlined,
            title: 'Kho tri thức cá nhân đang trống',
            subtitle:
                'Viết ghi chú và gắn liên kết tới Công việc hoặc Mục tiêu.',
          )
        else if (notes.isEmpty)
          LifeOsEmptyState(
            icon: Icons.inventory_2_outlined,
            title: 'Không có ghi chú đang hoạt động',
            subtitle:
                'Hiện có $archivedCount ghi chú đã lưu trữ. Bật "Lưu trữ ($archivedCount)" ở trên để xem lại.',
          )
        else
          Column(
            children: notes.map((LocalEntityRow n) {
              final List<LocalEntityRow> links = repo.listLinksForNote(
                userId: userId,
                noteId: n.id,
              );
              final List<LocalEntityRow> attachedTags =
                  tagsByNoteId[n.id] ?? const <LocalEntityRow>[];
              final String rawContent =
                  (n.payload['content'] as String?)?.trim() ?? '';
              final String preview = rawContent
                  .replaceAll('#', '')
                  .replaceAll('\n', ' ')
                  .trim();

              final bool isPinned = n.payload['pinned'] == true;
              final bool isArchived = n.payload['archived'] == true;
              return Padding(
                padding: const EdgeInsets.only(bottom: LifeOsTokens.space2),
                child: LifeOsCard(
                  elevation: 1,
                  padding: EdgeInsets.zero,
                  child: InkWell(
                    key: Key('btn_edit_note_${n.id}'),
                    borderRadius: BorderRadius.circular(LifeOsTokens.radiusLg),
                    onTap: () {
                      showModalBottomSheet<void>(
                        context: context,
                        isScrollControlled: true,
                        useSafeArea: true,
                        backgroundColor: Colors.transparent,
                        builder: (BuildContext ctx) => NoteEditorSheet(note: n),
                      );
                    },
                    child: Padding(
                      padding: const EdgeInsets.all(LifeOsTokens.space3),
                      child: Row(
                        crossAxisAlignment: CrossAxisAlignment.start,
                        children: <Widget>[
                          Container(
                            padding: const EdgeInsets.all(10),
                            decoration: BoxDecoration(
                              color: tokens.domainKnowledge.withValues(alpha: 0.12),
                              borderRadius:
                                  BorderRadius.circular(LifeOsTokens.radiusMd),
                            ),
                            child: Icon(
                              Icons.description_outlined,
                              color: tokens.domainKnowledge,
                              size: 20,
                            ),
                          ),
                          const SizedBox(width: LifeOsTokens.space3),
                          Expanded(
                            child: Column(
                              crossAxisAlignment: CrossAxisAlignment.start,
                              children: <Widget>[
                                Row(
                                  children: <Widget>[
                                    Expanded(
                                      child: Text(
                                        (n.payload['title'] as String?) ?? 'Ghi chú',
                                        style: TextStyle(
                                          fontWeight: FontWeight.w700,
                                          fontSize: 15,
                                          color: tokens.textPrimary,
                                        ),
                                      ),
                                    ),
                                    if (isPinned) ...<Widget>[
                                      const SizedBox(width: 4),
                                      Icon(
                                        Icons.push_pin_rounded,
                                        size: 14,
                                        color: tokens.domainKnowledge,
                                      ),
                                    ],
                                    if (isArchived) ...<Widget>[
                                      const SizedBox(width: 4),
                                      Icon(
                                        Icons.archive_outlined,
                                        size: 14,
                                        color: tokens.textMuted,
                                      ),
                                    ],
                                  ],
                                ),
                            if (preview.isNotEmpty) ...<Widget>[
                              const SizedBox(height: 4),
                              Text(
                                preview,
                                maxLines: 2,
                                overflow: TextOverflow.ellipsis,
                                style: TextStyle(
                                  fontSize: 13,
                                  color: tokens.textSecondary,
                                  height: 1.35,
                                ),
                              ),
                            ],
                            const SizedBox(height: 8),
                            Wrap(
                              spacing: 6,
                              runSpacing: 6,
                              crossAxisAlignment: WrapCrossAlignment.center,
                              children: <Widget>[
                                Container(
                                  padding: const EdgeInsets.symmetric(
                                    horizontal: 8,
                                    vertical: 3,
                                  ),
                                  decoration: BoxDecoration(
                                    color: tokens.domainKnowledge
                                        .withValues(alpha: 0.1),
                                    borderRadius: BorderRadius.circular(
                                      LifeOsTokens.radiusPill,
                                    ),
                                  ),
                                  child: Row(
                                    mainAxisSize: MainAxisSize.min,
                                    children: <Widget>[
                                      Icon(
                                        Icons.link_rounded,
                                        size: 13,
                                        color: tokens.domainKnowledge,
                                      ),
                                      const SizedBox(width: 4),
                                      Text(
                                        '${links.length} liên kết',
                                        style: TextStyle(
                                          fontSize: 11,
                                          fontWeight: FontWeight.w600,
                                          color: tokens.domainKnowledge,
                                          fontFeatures: const <FontFeature>[
                                            FontFeature.tabularFigures(),
                                          ],
                                        ),
                                      ),
                                    ],
                                  ),
                                ),
                                ...attachedTags.map((LocalEntityRow tag) {
                                  final String tagName =
                                      (tag.payload['name'] as String?) ??
                                          'Nhãn';
                                  return Container(
                                    padding: const EdgeInsets.symmetric(
                                      horizontal: 8,
                                      vertical: 3,
                                    ),
                                    decoration: BoxDecoration(
                                      color: tokens.bgElevated,
                                      borderRadius: BorderRadius.circular(
                                        LifeOsTokens.radiusPill,
                                      ),
                                      border: Border.all(
                                        color: tokens.borderSubtle,
                                      ),
                                    ),
                                    child: Text(
                                      '#$tagName',
                                      style: TextStyle(
                                        fontSize: 11,
                                        fontWeight: FontWeight.w600,
                                        color: tokens.textSecondary,
                                      ),
                                    ),
                                  );
                                }),
                              ],
                            ),
                          ],
                        ),
                      ),
                    ],
                  ),
                ),
              ),
            ),
          );
            }).toList(),
          ),

        const SizedBox(height: LifeOsTokens.space5),

        // Mục tiêu dài hạn
        Wrap(
          spacing: 8,
          runSpacing: 8,
          alignment: WrapAlignment.spaceBetween,
          crossAxisAlignment: WrapCrossAlignment.center,
          children: <Widget>[
            Row(
              mainAxisSize: MainAxisSize.min,
              children: <Widget>[
                Icon(
                  Icons.track_changes_rounded,
                  size: 20,
                  color: tokens.domainGoal,
                ),
                const SizedBox(width: 8),
                Flexible(
                  child: Text(
                    'Mục tiêu dài hạn',
                    style: Theme.of(context).textTheme.titleLarge,
                  ),
                ),
              ],
            ),
            FilledButton.tonalIcon(
              key: const Key('btn_open_create_goal'),
              onPressed: () {
                showModalBottomSheet<void>(
                  context: context,
                  isScrollControlled: true,
                  useSafeArea: true,
                  backgroundColor: Colors.transparent,
                  builder: (BuildContext ctx) => const CreateGoalSheet(),
                );
              },
              icon: const Icon(Icons.flag_outlined, size: 18),
              label: const Text('Thêm mục tiêu'),
            ),
          ],
        ),
        const SizedBox(height: LifeOsTokens.space2),
        if (goals.isEmpty)
          const LifeOsEmptyState(
            icon: Icons.flag_circle_outlined,
            title: 'Thiết lập mục tiêu dài hạn',
            subtitle:
                'Theo dõi tiến độ cột mốc và cảnh báo nguy cơ trễ hạn tự động.',
          )
        else
          Column(
            children: goals.map((LocalEntityRow g) {
              final List<LocalEntityRow> goalMilestones = milestones
                  .where((LocalEntityRow m) => m.payload['goalId'] == g.id)
                  .toList();
              final int totalMs = goalMilestones.length;
              final int doneMs = goalMilestones
                  .where((LocalEntityRow m) =>
                      (m.payload['completed'] == true) ||
                      (m.payload['completedAt'] != null) ||
                      (m.payload['status']?.toString().toUpperCase() == 'DONE'))
                  .length;

              final ({int progressPercent, GoalRiskStatus risk}) eval =
                  AnalyticsCalculator.evaluateGoal(
                completedMilestones: doneMs,
                totalMilestones: totalMs,
                startDateIso:
                    (g.payload['startOn'] ?? g.payload['startDate']) as String?,
                targetDateIso:
                    (g.payload['targetOn'] ?? g.payload['targetDate'])
                        as String?,
                todayIso: todayIso,
              );

              final Color riskColor = switch (eval.risk) {
                GoalRiskStatus.onTrack => tokens.riskOnTrackFg,
                GoalRiskStatus.atRisk => tokens.riskAtRiskFg,
                GoalRiskStatus.offTrack => tokens.riskOverdueFg,
              };

              return Padding(
                padding: const EdgeInsets.only(bottom: LifeOsTokens.space2),
                child: LifeOsCard(
                  elevation: 1,
                  padding: EdgeInsets.zero,
                  child: InkWell(
                    key: Key('btn_edit_goal_${g.id}'),
                    borderRadius: BorderRadius.circular(LifeOsTokens.radiusLg),
                    onTap: () {
                      showModalBottomSheet<void>(
                        context: context,
                        isScrollControlled: true,
                        useSafeArea: true,
                        backgroundColor: Colors.transparent,
                        builder: (BuildContext ctx) =>
                            GoalEditorSheet(goal: g),
                      );
                    },
                    child: Padding(
                      padding: const EdgeInsets.all(LifeOsTokens.space4),
                      child: Column(
                        crossAxisAlignment: CrossAxisAlignment.start,
                        children: <Widget>[
                          Row(
                            crossAxisAlignment: CrossAxisAlignment.start,
                            children: <Widget>[
                              Container(
                                padding: const EdgeInsets.all(10),
                                decoration: BoxDecoration(
                                  color: tokens.domainGoal.withValues(alpha: 0.12),
                                  borderRadius: BorderRadius.circular(
                                    LifeOsTokens.radiusMd,
                                  ),
                                ),
                                child: Icon(
                                  Icons.flag_rounded,
                                  color: tokens.domainGoal,
                                  size: 20,
                                ),
                              ),
                              const SizedBox(width: LifeOsTokens.space3),
                              Expanded(
                                child: Column(
                                  crossAxisAlignment: CrossAxisAlignment.start,
                                  children: <Widget>[
                                    Text(
                                      (g.payload['title'] as String?) ?? 'Mục tiêu',
                                      style: TextStyle(
                                        fontWeight: FontWeight.w700,
                                        fontSize: 15,
                                        color: tokens.textPrimary,
                                      ),
                                    ),
                                    const SizedBox(height: 6),
                                    Wrap(
                                      spacing: 8,
                                      runSpacing: 4,
                                      crossAxisAlignment: WrapCrossAlignment.center,
                                      children: <Widget>[
                                        Container(
                                          padding: const EdgeInsets.symmetric(
                                            horizontal: 8,
                                            vertical: 3,
                                          ),
                                          decoration: BoxDecoration(
                                            color: riskColor.withValues(alpha: 0.12),
                                            borderRadius: BorderRadius.circular(
                                              LifeOsTokens.radiusPill,
                                            ),
                                          ),
                                          child: Text(
                                            eval.risk.labelVi,
                                            style: TextStyle(
                                              fontSize: 11,
                                              fontWeight: FontWeight.w700,
                                              color: riskColor,
                                            ),
                                          ),
                                        ),
                                        Text(
                                          '$doneMs/$totalMs cột mốc',
                                          style: TextStyle(
                                            fontSize: 12,
                                            color: tokens.textSecondary,
                                            fontFeatures: const <FontFeature>[
                                              FontFeature.tabularFigures(),
                                            ],
                                          ),
                                        ),
                                      ],
                                    ),
                                  ],
                                ),
                              ),
                              Text(
                                '${eval.progressPercent}%',
                                style: TextStyle(
                                  fontSize: 16,
                                  fontWeight: FontWeight.w800,
                                  color: tokens.domainGoal,
                                  fontFeatures: const <FontFeature>[
                                    FontFeature.tabularFigures(),
                                  ],
                                ),
                              ),
                            ],
                          ),
                          const SizedBox(height: LifeOsTokens.space3),
                          ClipRRect(
                            borderRadius: BorderRadius.circular(4),
                            child: LinearProgressIndicator(
                              minHeight: 8,
                              value: (eval.progressPercent / 100.0).clamp(0.0, 1.0),
                              backgroundColor: tokens.borderSubtle,
                              valueColor: AlwaysStoppedAnimation<Color>(
                                tokens.domainGoal,
                              ),
                            ),
                          ),
                          const SizedBox(height: LifeOsTokens.space2),
                          Align(
                            alignment: Alignment.centerRight,
                            child: TextButton.icon(
                              key: Key('btn_goal_milestones_${g.id}'),
                              style: TextButton.styleFrom(
                                minimumSize: const Size(
                                  120,
                                  LifeOsTokens.minTouchTarget,
                                ),
                              ),
                              onPressed: () {
                                showModalBottomSheet<void>(
                                  context: context,
                                  isScrollControlled: true,
                                  useSafeArea: true,
                                  backgroundColor: Colors.transparent,
                                  builder: (BuildContext ctx) =>
                                      GoalMilestonesSheet(goal: g),
                                );
                              },
                              icon: const Icon(Icons.checklist_rounded, size: 18),
                              label: Text(
                                totalMs == 0
                                    ? 'Thêm cột mốc'
                                    : 'Quản lý $totalMs cột mốc',
                              ),
                            ),
                          ),
                        ],
                      ),
                    ),
                  ),
                ),
              );
            }).toList(),
          ),
      ],
    );
  }
}
