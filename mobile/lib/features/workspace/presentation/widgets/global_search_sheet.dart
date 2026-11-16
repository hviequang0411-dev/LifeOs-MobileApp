import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../../core/database/app_database.dart';
import '../../../../core/providers/app_providers.dart';
import '../../../../core/sync/entity_types.dart';
import '../../../../core/theme/app_tokens.dart';

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

  @override
  Widget build(BuildContext context) {
    final LifeOsTokens tokens = LifeOsTokens.of(context);
    final List<LocalEntityRow> allTasks =
        ref.watch(entitiesByTypeStreamProvider(SyncEntityType.task)).valueOrNull ??
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
            return title.contains(q);
          }).toList();

    final List<LocalEntityRow> matchedTxs = _query.isEmpty
        ? const <LocalEntityRow>[]
        : allTxs.where((LocalEntityRow tx) {
            final String note =
                (tx.payload['note'] ?? '').toString().toLowerCase();
            final String cat =
                (tx.payload['categoryName'] ?? '').toString().toLowerCase();
            return note.contains(q) || cat.contains(q);
          }).toList();

    final bool hasAnyResult = matchedTasks.isNotEmpty ||
        matchedNotes.isNotEmpty ||
        matchedEvents.isNotEmpty ||
        matchedTxs.isNotEmpty;

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
            padding: const EdgeInsets.symmetric(
              horizontal: LifeOsTokens.space4,
              vertical: 8,
            ),
            child: Row(
              children: <Widget>[
                Expanded(
                  child: TextField(
                    controller: _searchController,
                    autofocus: true,
                    decoration: InputDecoration(
                      hintText: 'Tìm công việc, ghi chú, lịch hẹn, giao dịch...',
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
                              final bool isDone = (t.payload['status'] ?? '')
                                      .toString()
                                      .toUpperCase() ==
                                  'DONE';
                              return _SearchResultTile(
                                icon: isDone
                                    ? Icons.check_circle_rounded
                                    : Icons.circle_outlined,
                                iconColor: tokens.domainTask,
                                title: (t.payload['title'] ?? 'Công việc')
                                    .toString(),
                                tokens: tokens,
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
                                icon: Icons.note_rounded,
                                iconColor: tokens.domainKnowledge,
                                title: (n.payload['title'] ?? 'Ghi chú')
                                    .toString(),
                                subtitle: (n.payload['content'] ?? '')
                                    .toString()
                                    .replaceAll('#', '')
                                    .trim(),
                                tokens: tokens,
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
                                icon: Icons.event_rounded,
                                iconColor: tokens.domainCalendar,
                                title:
                                    (e.payload['title'] ?? 'Sự kiện').toString(),
                                subtitle:
                                    (e.payload['startLocal'] ?? '').toString(),
                                tokens: tokens,
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
                              return _SearchResultTile(
                                icon: Icons.receipt_long_rounded,
                                iconColor: tokens.domainFinance,
                                title: (tx.payload['note'] ?? 'Giao dịch')
                                    .toString(),
                                subtitle:
                                    (tx.payload['occurredOn'] ?? '').toString(),
                                tokens: tokens,
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

class _SearchResultTile extends StatelessWidget {
  const _SearchResultTile({
    required this.icon,
    required this.iconColor,
    required this.title,
    required this.tokens,
    this.subtitle,
  });

  final IconData icon;
  final Color iconColor;
  final String title;
  final String? subtitle;
  final LifeOsTokens tokens;

  @override
  Widget build(BuildContext context) {
    return Container(
      margin: const EdgeInsets.only(bottom: 8),
      padding: const EdgeInsets.all(12),
      decoration: BoxDecoration(
        color: tokens.bgElevated,
        borderRadius: BorderRadius.circular(LifeOsTokens.radiusMd),
        border: Border.all(color: tokens.borderSubtle),
      ),
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
        ],
      ),
    );
  }
}
