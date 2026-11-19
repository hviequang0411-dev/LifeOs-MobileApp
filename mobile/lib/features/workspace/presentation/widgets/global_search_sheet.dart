import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../../core/database/app_database.dart';
import '../../../../core/providers/app_providers.dart';
import '../../../../core/sync/entity_types.dart';
import '../../../../core/theme/app_tokens.dart';
import '../../../calendar/presentation/widgets/plan_forms_sheets.dart';
import '../../../finance/presentation/widgets/finance_forms_sheets.dart';
import '../../../habits/presentation/widgets/habit_editor_sheet.dart';
import '../../../knowledge/presentation/widgets/goal_milestones_sheet.dart';
import '../../../knowledge/presentation/widgets/note_editor_sheet.dart';
import '../../../tasks/presentation/widgets/task_editor_sheet.dart';

class GlobalSearchSheet extends ConsumerStatefulWidget {
  const GlobalSearchSheet({super.key});

  @override
  ConsumerState<GlobalSearchSheet> createState() => _GlobalSearchSheetState();
}

class _GlobalSearchSheetState extends ConsumerState<GlobalSearchSheet> {
  final TextEditingController _searchController = TextEditingController();
  String _query = '';

  @override
  void dispose() {
    _searchController.dispose();
    super.dispose();
  }

  void _openEditorFor(BuildContext context, Widget sheet) {
    final NavigatorState nav = Navigator.of(context);
    final BuildContext hostContext = nav.context;
    if (nav.canPop()) {
      nav.pop();
    }
    showModalBottomSheet<void>(
      context: hostContext,
      isScrollControlled: true,
      useSafeArea: true,
      backgroundColor: Colors.transparent,
      builder: (BuildContext ctx) => sheet,
    );
  }

  @override
  Widget build(BuildContext context) {
    final LifeOsTokens tokens = LifeOsTokens.of(context);
    final List<LocalEntityRow> allTasks =
        ref.watch(entitiesByTypeStreamProvider(SyncEntityType.task)).valueOrNull ??
            const <LocalEntityRow>[];
    final List<LocalEntityRow> allHabits =
        ref.watch(entitiesByTypeStreamProvider(SyncEntityType.habit)).valueOrNull ??
            const <LocalEntityRow>[];
    final List<LocalEntityRow> allGoals =
        ref.watch(entitiesByTypeStreamProvider(SyncEntityType.goal)).valueOrNull ??
            const <LocalEntityRow>[];
    final List<LocalEntityRow> allNotes =
        ref.watch(entitiesByTypeStreamProvider(SyncEntityType.note)).valueOrNull ??
            const <LocalEntityRow>[];
    final List<LocalEntityRow> allEvents = ref
            .watch(entitiesByTypeStreamProvider(SyncEntityType.calendarEvent))
            .valueOrNull ??
        const <LocalEntityRow>[];
    final List<LocalEntityRow> allTxs = ref
            .watch(entitiesByTypeStreamProvider(SyncEntityType.financeTransaction))
            .valueOrNull ??
        const <LocalEntityRow>[];
    final List<LocalEntityRow> allCategories = ref
            .watch(entitiesByTypeStreamProvider(SyncEntityType.financeCategory))
            .valueOrNull ??
        const <LocalEntityRow>[];

    final Map<String, String> categoryNameById = <String, String>{
      for (final LocalEntityRow c in allCategories)
        c.id: (c.payload['name'] ?? '').toString(),
    };

    final String q = _query.toLowerCase();

    final List<LocalEntityRow> matchedTasks = _query.isEmpty
        ? const <LocalEntityRow>[]
        : allTasks.where((LocalEntityRow t) {
            final String title =
                (t.payload['title'] ?? '').toString().toLowerCase();
            final String desc =
                (t.payload['description'] ?? '').toString().toLowerCase();
            return title.contains(q) || desc.contains(q);
          }).toList();

    final List<LocalEntityRow> matchedHabits = _query.isEmpty
        ? const <LocalEntityRow>[]
        : allHabits.where((LocalEntityRow h) {
            final String name =
                (h.payload['name'] ?? '').toString().toLowerCase();
            final String desc =
                (h.payload['description'] ?? '').toString().toLowerCase();
            return name.contains(q) || desc.contains(q);
          }).toList();

    final List<LocalEntityRow> matchedGoals = _query.isEmpty
        ? const <LocalEntityRow>[]
        : allGoals.where((LocalEntityRow g) {
            final String title =
                (g.payload['title'] ?? '').toString().toLowerCase();
            final String desc =
                (g.payload['description'] ?? '').toString().toLowerCase();
            return title.contains(q) || desc.contains(q);
          }).toList();

    final List<LocalEntityRow> matchedNotes = _query.isEmpty
        ? const <LocalEntityRow>[]
        : allNotes.where((LocalEntityRow n) {
            final String title =
                (n.payload['title'] ?? '').toString().toLowerCase();
            final String content =
                (n.payload['content'] ?? '').toString().toLowerCase();
            return title.contains(q) || content.contains(q);
          }).toList();

    final List<LocalEntityRow> matchedEvents = _query.isEmpty
        ? const <LocalEntityRow>[]
        : allEvents.where((LocalEntityRow e) {
            final String title =
                (e.payload['title'] ?? '').toString().toLowerCase();
            final String location =
                (e.payload['location'] ?? '').toString().toLowerCase();
            return title.contains(q) || location.contains(q);
          }).toList();

    final List<LocalEntityRow> matchedTxs = _query.isEmpty
        ? const <LocalEntityRow>[]
        : allTxs.where((LocalEntityRow tx) {
            final String note =
                (tx.payload['note'] ?? '').toString().toLowerCase();
            final String desc =
                (tx.payload['description'] ?? '').toString().toLowerCase();
            final String? catId = tx.payload['categoryId'] as String?;
            final String cat = ((catId != null ? categoryNameById[catId] : null) ??
                    tx.payload['categoryName'] ??
                    '')
                .toString()
                .toLowerCase();
            return note.contains(q) || desc.contains(q) || cat.contains(q);
          }).toList();

    final bool hasAnyResult = matchedTasks.isNotEmpty ||
        matchedHabits.isNotEmpty ||
        matchedGoals.isNotEmpty ||
        matchedNotes.isNotEmpty ||
        matchedEvents.isNotEmpty ||
        matchedTxs.isNotEmpty;

    return Material(
      color: tokens.bgSurface,
      borderRadius: const BorderRadius.vertical(
        top: Radius.circular(LifeOsTokens.radiusXl),
      ),
      clipBehavior: Clip.antiAlias,
      child: SizedBox(
        height: MediaQuery.of(context).size.height * 0.88,
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
            padding: const EdgeInsets.symmetric(
              horizontal: LifeOsTokens.space4,
              vertical: 8,
            ),
            child: Row(
              children: <Widget>[
                Expanded(
                  child: TextField(
                    key: const Key('input_global_search'),
                    controller: _searchController,
                    autofocus: true,
                    decoration: InputDecoration(
                      hintText:
                          'Tìm công việc, thói quen, mục tiêu, ghi chú, lịch, giao dịch...',
                      prefixIcon: Icon(
                        Icons.search_rounded,
                        color: tokens.textSecondary,
                      ),
                      suffixIcon: _query.isNotEmpty
                          ? IconButton(
                              icon: const Icon(Icons.clear_rounded),
                              onPressed: () {
                                _searchController.clear();
                                setState(() {
                                  _query = '';
                                });
                              },
                            )
                          : null,
                      filled: true,
                      fillColor: tokens.bgElevated,
                      border: OutlineInputBorder(
                        borderRadius:
                            BorderRadius.circular(LifeOsTokens.radiusMd),
                        borderSide: BorderSide(color: tokens.borderSubtle),
                      ),
                      contentPadding: const EdgeInsets.symmetric(
                        horizontal: 12,
                        vertical: 10,
                      ),
                    ),
                    onChanged: (String val) {
                      setState(() {
                        _query = val.trim();
                      });
                    },
                  ),
                ),
                const SizedBox(width: 8),
                TextButton(
                  onPressed: () => Navigator.of(context).pop(),
                  child: Text(
                    'Đóng',
                    style: TextStyle(color: tokens.textSecondary),
                  ),
                ),
              ],
            ),
          ),
          const Divider(height: 1),
          Expanded(
            child: _query.isEmpty
                ? Center(
                    child: Text(
                      'Nhập từ khóa để tìm kiếm nhanh trên thiết bị.',
                      style: TextStyle(
                        fontSize: 13,
                        color: tokens.textSecondary,
                      ),
                    ),
                  )
                : !hasAnyResult
                    ? Center(
                        child: Text(
                          'Không tìm thấy kết quả nào phù hợp.',
                          style: TextStyle(
                            fontSize: 13,
                            color: tokens.textSecondary,
                          ),
                        ),
                      )
                    : ListView(
                        padding: const EdgeInsets.all(LifeOsTokens.space4),
                        children: <Widget>[
                          if (matchedTasks.isNotEmpty) ...<Widget>[
                            Text(
                              'Công việc (${matchedTasks.length})',
                              style: TextStyle(
                                fontSize: 13,
                                fontWeight: FontWeight.w700,
                                color: tokens.domainTask,
                              ),
                            ),
                            const SizedBox(height: 8),
                            ...matchedTasks.map((LocalEntityRow t) {
                              final String status =
                                  (t.payload['status'] ?? '').toString().toUpperCase();
                              final bool isDone =
                                  status == 'DONE' || status == 'COMPLETED';
                              return _SearchResultTile(
                                tileKey: Key('search_result_task_${t.id}'),
                                icon: isDone
                                    ? Icons.check_circle_rounded
                                    : Icons.circle_outlined,
                                iconColor: tokens.domainTask,
                                title: (t.payload['title'] ?? 'Công việc')
                                    .toString(),
                                tokens: tokens,
                                onTap: () => _openEditorFor(
                                  context,
                                  TaskEditorSheet(task: t),
                                ),
                              );
                            }),
                            const SizedBox(height: 16),
                          ],
                          if (matchedHabits.isNotEmpty) ...<Widget>[
                            Text(
                              'Thói quen (${matchedHabits.length})',
                              style: TextStyle(
                                fontSize: 13,
                                fontWeight: FontWeight.w700,
                                color: tokens.domainHabit,
                              ),
                            ),
                            const SizedBox(height: 8),
                            ...matchedHabits.map((LocalEntityRow h) {
                              return _SearchResultTile(
                                tileKey: Key('search_result_habit_${h.id}'),
                                icon: Icons.repeat_rounded,
                                iconColor: tokens.domainHabit,
                                title: (h.payload['name'] ?? 'Thói quen')
                                    .toString(),
                                subtitle: (h.payload['description'] ?? '')
                                    .toString()
                                    .trim(),
                                tokens: tokens,
                                onTap: () => _openEditorFor(
                                  context,
                                  HabitEditorSheet(habit: h),
                                ),
                              );
                            }),
                            const SizedBox(height: 16),
                          ],
                          if (matchedGoals.isNotEmpty) ...<Widget>[
                            Text(
                              'Mục tiêu dài hạn (${matchedGoals.length})',
                              style: TextStyle(
                                fontSize: 13,
                                fontWeight: FontWeight.w700,
                                color: tokens.domainGoal,
                              ),
                            ),
                            const SizedBox(height: 8),
                            ...matchedGoals.map((LocalEntityRow g) {
                              return _SearchResultTile(
                                tileKey: Key('search_result_goal_${g.id}'),
                                icon: Icons.flag_rounded,
                                iconColor: tokens.domainGoal,
                                title: (g.payload['title'] ?? 'Mục tiêu')
                                    .toString(),
                                subtitle: (g.payload['targetOn'] ??
                                        g.payload['targetDate'] ??
                                        '')
                                    .toString(),
                                tokens: tokens,
                                onTap: () => _openEditorFor(
                                  context,
                                  GoalEditorSheet(goal: g),
                                ),
                              );
                            }),
                            const SizedBox(height: 16),
                          ],
                          if (matchedNotes.isNotEmpty) ...<Widget>[
                            Text(
                              'Ghi chú (${matchedNotes.length})',
                              style: TextStyle(
                                fontSize: 13,
                                fontWeight: FontWeight.w700,
                                color: tokens.domainKnowledge,
                              ),
                            ),
                            const SizedBox(height: 8),
                            ...matchedNotes.map((LocalEntityRow n) {
                              return _SearchResultTile(
                                tileKey: Key('search_result_note_${n.id}'),
                                icon: Icons.note_rounded,
                                iconColor: tokens.domainKnowledge,
                                title: (n.payload['title'] ?? 'Ghi chú')
                                    .toString(),
                                subtitle: (n.payload['content'] ?? '')
                                    .toString()
                                    .replaceAll('#', '')
                                    .trim(),
                                tokens: tokens,
                                onTap: () => _openEditorFor(
                                  context,
                                  NoteEditorSheet(note: n),
                                ),
                              );
                            }),
                            const SizedBox(height: 16),
                          ],
                          if (matchedEvents.isNotEmpty) ...<Widget>[
                            Text(
                              'Sự kiện lịch (${matchedEvents.length})',
                              style: TextStyle(
                                fontSize: 13,
                                fontWeight: FontWeight.w700,
                                color: tokens.domainCalendar,
                              ),
                            ),
                            const SizedBox(height: 8),
                            ...matchedEvents.map((LocalEntityRow e) {
                              return _SearchResultTile(
                                tileKey: Key('search_result_event_${e.id}'),
                                icon: Icons.event_rounded,
                                iconColor: tokens.domainCalendar,
                                title:
                                    (e.payload['title'] ?? 'Sự kiện').toString(),
                                subtitle:
                                    (e.payload['startLocal'] ?? '').toString(),
                                tokens: tokens,
                                onTap: () => _openEditorFor(
                                  context,
                                  CalendarEventSheet(event: e),
                                ),
                              );
                            }),
                            const SizedBox(height: 16),
                          ],
                          if (matchedTxs.isNotEmpty) ...<Widget>[
                            Text(
                              'Giao dịch (${matchedTxs.length})',
                              style: TextStyle(
                                fontSize: 13,
                                fontWeight: FontWeight.w700,
                                color: tokens.domainFinance,
                              ),
                            ),
                            const SizedBox(height: 8),
                            ...matchedTxs.map((LocalEntityRow tx) {
                              final String? catId =
                                  tx.payload['categoryId'] as String?;
                              final String catName =
                                  (catId != null ? categoryNameById[catId] : null) ??
                                      (tx.payload['categoryName'] as String?) ??
                                      '';
                              final String note =
                                  (tx.payload['note'] as String?)?.trim() ?? '';
                              final String title = note.isNotEmpty
                                  ? note
                                  : (catName.isNotEmpty ? catName : 'Giao dịch');
                              return _SearchResultTile(
                                tileKey: Key('search_result_tx_${tx.id}'),
                                icon: Icons.receipt_long_rounded,
                                iconColor: tokens.domainFinance,
                                title: title,
                                subtitle:
                                    (tx.payload['occurredOn'] ?? '').toString(),
                                tokens: tokens,
                                onTap: () => _openEditorFor(
                                  context,
                                  FinanceTransactionSheet(transaction: tx),
                                ),
                              );
                            }),
                          ],
                        ],
                      ),
          ),
        ],
      ),
      ),
    );
  }
}

class _SearchResultTile extends StatelessWidget {
  const _SearchResultTile({
    this.tileKey,
    required this.icon,
    required this.iconColor,
    required this.title,
    required this.tokens,
    this.subtitle,
    this.onTap,
  });

  final Key? tileKey;
  final IconData icon;
  final Color iconColor;
  final String title;
  final String? subtitle;
  final LifeOsTokens tokens;
  final VoidCallback? onTap;

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.only(bottom: 8),
      child: Material(
        color: tokens.bgElevated,
        shape: RoundedRectangleBorder(
          borderRadius: BorderRadius.circular(LifeOsTokens.radiusMd),
          side: BorderSide(color: tokens.borderSubtle),
        ),
        clipBehavior: Clip.antiAlias,
        child: InkWell(
          key: tileKey,
          onTap: onTap,
          child: Padding(
            padding: const EdgeInsets.all(12),
            child: Row(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: <Widget>[
                Icon(icon, size: 18, color: iconColor),
                const SizedBox(width: 10),
                Expanded(
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: <Widget>[
                      Text(
                        title,
                        style: TextStyle(
                          fontSize: 13,
                          fontWeight: FontWeight.w600,
                          color: tokens.textPrimary,
                        ),
                      ),
                      if (subtitle != null && subtitle!.isNotEmpty) ...<Widget>[
                        const SizedBox(height: 2),
                        Text(
                          subtitle!,
                          maxLines: 2,
                          overflow: TextOverflow.ellipsis,
                          style: TextStyle(
                            fontSize: 11.5,
                            color: tokens.textSecondary,
                          ),
                        ),
                      ],
                    ],
                  ),
                ),
                Icon(
                  Icons.chevron_right_rounded,
                  size: 18,
                  color: tokens.textMuted,
                ),
              ],
            ),
          ),
        ),
      ),
    );
  }
}
