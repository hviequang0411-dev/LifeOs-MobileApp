import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../core/database/app_database.dart';
import '../../../core/l10n/vi_strings.dart';
import '../../../core/providers/app_providers.dart';
import '../../../core/sync/entity_types.dart';
import '../../../core/theme/app_tokens.dart';
import '../../../core/widgets/lifeos_widgets.dart';
import '../../tasks/presentation/widgets/routines_section_card.dart';
import '../../workspace/data/lifeos_local_repository.dart';
import 'widgets/daily_timeline_widget.dart';

/// Tab 1: Màn hình **Kế hoạch (`Plan`)** — Botanical Premium v2 (`UPW6`)
/// Thanh tuần ngang, Dòng thời gian hôm nay (`DailyTimelineWidget`), Dự án & Sự kiện lặp.
class PlanTabView extends ConsumerStatefulWidget {
  const PlanTabView({super.key});

  @override
  ConsumerState<PlanTabView> createState() => _PlanTabViewState();
}

class _PlanTabViewState extends ConsumerState<PlanTabView> {
  int _selectedDayOffset = 0; // 0 = hôm nay, -3..+3 trong tuần

  static const List<String> _weekdayLabels = <String>[
    'T2',
    'T3',
    'T4',
    'T5',
    'T6',
    'T7',
    'CN',
  ];

  @override
  Widget build(BuildContext context) {
    final String userId = ref.watch(currentUserIdProvider);
    final LifeOsLocalRepository repo = ref.watch(localRepositoryProvider);
    final LifeOsTokens tokens = LifeOsTokens.of(context);

    final List<LocalEntityRow> projects = ref
            .watch(entitiesByTypeStreamProvider(SyncEntityType.project))
            .valueOrNull ??
        const <LocalEntityRow>[];
    final List<LocalEntityRow> events = ref
            .watch(entitiesByTypeStreamProvider(SyncEntityType.calendarEvent))
            .valueOrNull ??
        const <LocalEntityRow>[];
    final List<LocalEntityRow> timeBlocks = ref
            .watch(entitiesByTypeStreamProvider(SyncEntityType.timeBlock))
            .valueOrNull ??
        const <LocalEntityRow>[];
    final List<LocalEntityRow> tasks = ref
            .watch(entitiesByTypeStreamProvider(SyncEntityType.task))
            .valueOrNull ??
        const <LocalEntityRow>[];

    final DateTime refUtc = ref.watch(referenceUtcProvider);
    final DateTime localNow = refUtc.toLocal();
    final String todayIso =
        repo.getUserDayClock(userId).businessDateForUtc(refUtc);
    final DateTime selectedDate =
        localNow.add(Duration(days: _selectedDayOffset));
    final String selectedIso =
        '${selectedDate.year.toString().padLeft(4, '0')}-${selectedDate.month.toString().padLeft(2, '0')}-${selectedDate.day.toString().padLeft(2, '0')}';

    // Dựng danh sách dòng thời gian kết hợp (Sự kiện + Khối thời gian + Việc trong ngày)
    final List<TimelineEntryItem> timelineItems = <TimelineEntryItem>[];
    for (final LocalEntityRow ev in events) {
      final String startLocal =
          (ev.payload['startLocal'] ?? '').toString();
      if (startLocal.startsWith(selectedIso) || _selectedDayOffset == 0) {
        final String timePart = startLocal.contains('T')
            ? startLocal.split('T').last.substring(0, 5)
            : '09:00';
        timelineItems.add(
          TimelineEntryItem(
            id: ev.id,
            title: (ev.payload['title'] as String?) ?? 'Sự kiện lịch',
            timeStr: timePart,
            kind: 'EVENT',
            isDone: false,
            durationMinutes: 60,
          ),
        );
      }
    }
    for (final LocalEntityRow tb in timeBlocks) {
      final String startAt =
          (tb.payload['startLocal'] ?? tb.payload['startAt'] ?? '').toString();
      final String timePart = startAt.contains('T')
          ? startAt.split('T').last.substring(0, 5)
          : '14:00';
      timelineItems.add(
        TimelineEntryItem(
          id: tb.id,
          title: (tb.payload['title'] as String?) ?? 'Khối tập trung sâu',
          timeStr: timePart,
          kind: 'TIME_BLOCK',
          isDone: false,
          durationMinutes: (tb.payload['durationMinutes'] as num?)?.toInt() ?? 45,
        ),
      );
    }
    for (final LocalEntityRow t in tasks.take(4)) {
      final String status =
          (t.payload['status'] ?? 'TODO').toString().toUpperCase();
      timelineItems.add(
        TimelineEntryItem(
          id: t.id,
          title: (t.payload['title'] as String?) ?? 'Công việc',
          timeStr: 'Trong ngày',
          kind: 'TASK',
          isDone: status == 'DONE',
        ),
      );
    }
    timelineItems.sort(
      (TimelineEntryItem a, TimelineEntryItem b) =>
          a.timeStr.compareTo(b.timeStr),
    );

    return ListView(
      padding: const EdgeInsets.fromLTRB(
        LifeOsTokens.space4,
        LifeOsTokens.space4,
        LifeOsTokens.space4,
        112,
      ),
      children: <Widget>[
        // Thanh tuần ngang (Horizontal Week Strip)
        LifeOsCard(
          elevation: 1,
          padding: const EdgeInsets.all(LifeOsTokens.space3),
          child: Column(
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
                    Row(
                      mainAxisSize: MainAxisSize.min,
                      children: <Widget>[
                        Icon(
                          Icons.date_range_rounded,
                          size: 18,
                          color: tokens.domainCalendar,
                        ),
                        const SizedBox(width: 6),
                        Flexible(
                          child: Text(
                            'Lịch tuần · Tháng ${selectedDate.month}/${selectedDate.year}',
                            style: TextStyle(
                              fontSize: 14,
                              fontWeight: FontWeight.w700,
                              color: tokens.textPrimary,
                            ),
                          ),
                        ),
                      ],
                    ),
                    if (_selectedDayOffset != 0)
                      TextButton(
                        onPressed: () => setState(() => _selectedDayOffset = 0),
                        child: const Text('Về hôm nay'),
                      ),
                  ],
                ),
              ),
              const SizedBox(height: LifeOsTokens.space2),
              Row(
                mainAxisAlignment: MainAxisAlignment.spaceBetween,
                children: List<Widget>.generate(7, (int idx) {
                  final int offset = idx - 3; // -3..+3 xung quanh hôm nay
                  final DateTime d = localNow.add(Duration(days: offset));
                  final bool isSelected = offset == _selectedDayOffset;
                  final bool isToday = offset == 0;
                  final String dayLabel =
                      _weekdayLabels[(d.weekday - 1).clamp(0, 6)];

                  return Expanded(
                    child: Semantics(
                      button: true,
                      selected: isSelected,
                      label: '$dayLabel ngày ${d.day}',
                      child: InkWell(
                        borderRadius:
                            BorderRadius.circular(LifeOsTokens.radiusMd),
                        onTap: () =>
                            setState(() => _selectedDayOffset = offset),
                        child: Container(
                          constraints: const BoxConstraints(
                            minHeight: LifeOsTokens.minTouchTarget,
                          ),
                          margin: const EdgeInsets.symmetric(horizontal: 2),
                          padding: const EdgeInsets.symmetric(vertical: 6),
                          decoration: BoxDecoration(
                            color: isSelected
                                ? tokens.domainCalendar
                                : isToday
                                    ? tokens.domainCalendar
                                        .withValues(alpha: 0.12)
                                    : Colors.transparent,
                            borderRadius:
                                BorderRadius.circular(LifeOsTokens.radiusMd),
                            border: isToday && !isSelected
                                ? Border.all(color: tokens.domainCalendar)
                                : null,
                          ),
                          child: Column(
                            mainAxisAlignment: MainAxisAlignment.center,
                            children: <Widget>[
                              Text(
                                dayLabel,
                                style: TextStyle(
                                  fontSize: 11,
                                  fontWeight: FontWeight.w600,
                                  color: isSelected
                                      ? Colors.white
                                      : tokens.textSecondary,
                                ),
                              ),
                              const SizedBox(height: 2),
                              Text(
                                '${d.day}',
                                style: TextStyle(
                                  fontSize: 14,
                                  fontWeight: FontWeight.w700,
                                  color: isSelected
                                      ? Colors.white
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
                    ),
                  );
                }),
              ),
            ],
          ),
        ),

        const SizedBox(height: LifeOsTokens.space4),

        // Dòng thời gian kết hợp trong ngày (bỏ margin ngang thừa của widget vì ListView đã có padding)
        DailyTimelineWidget(items: timelineItems),

        const SizedBox(height: LifeOsTokens.space5),

        // Chuỗi thói quen (Routine) & Trình chạy từng bước (MWP-C)
        const PlanRoutinesSection(),

        const SizedBox(height: LifeOsTokens.space5),

        // Khu vực Dự án & Phân loại
        Wrap(
          spacing: 8,
          runSpacing: 8,
          alignment: WrapAlignment.spaceBetween,
          crossAxisAlignment: WrapCrossAlignment.center,
          children: <Widget>[
            Text(
              'Dự án & Phân loại',
              style: Theme.of(context).textTheme.titleLarge,
            ),
            FilledButton.tonalIcon(
              onPressed: () {
                repo.createProject(
                  userId: userId,
                  name: 'Dự án Chiến lược ${projects.length + 1}',
                );
              },
              icon: const Icon(Icons.create_new_folder_outlined, size: 18),
              label: const Text('Thêm dự án'),
            ),
          ],
        ),
        const SizedBox(height: LifeOsTokens.space2),
        if (projects.isEmpty)
          const LifeOsEmptyState(
            icon: Icons.folder_open_rounded,
            title: 'Chưa có dự án nào',
            subtitle: 'Tạo dự án để nhóm các công việc liên quan với nhau.',
          )
        else
          Column(
            children: projects.map((LocalEntityRow proj) {
              final String name =
                  (proj.payload['name'] as String?) ?? 'Dự án';
              final int taskCount = tasks
                  .where((LocalEntityRow t) =>
                      t.payload['projectId'] == proj.id)
                  .length;
              return Padding(
                padding: const EdgeInsets.only(bottom: LifeOsTokens.space2),
                child: LifeOsCard(
                  elevation: 1,
                  padding: const EdgeInsets.symmetric(
                    horizontal: LifeOsTokens.space3,
                    vertical: LifeOsTokens.space2,
                  ),
                  child: Row(
                    children: <Widget>[
                      Container(
                        padding: const EdgeInsets.all(10),
                        decoration: BoxDecoration(
                          color: tokens.domainTask.withValues(alpha: 0.12),
                          borderRadius:
                              BorderRadius.circular(LifeOsTokens.radiusMd),
                        ),
                        child: Icon(
                          Icons.folder_outlined,
                          color: tokens.domainTask,
                          size: 20,
                        ),
                      ),
                      const SizedBox(width: LifeOsTokens.space3),
                      Expanded(
                        child: Column(
                          crossAxisAlignment: CrossAxisAlignment.start,
                          children: <Widget>[
                            Text(
                              name,
                              style: TextStyle(
                                fontWeight: FontWeight.w600,
                                fontSize: 15,
                                color: tokens.textPrimary,
                              ),
                            ),
                            const SizedBox(height: 2),
                            Text(
                              '$taskCount công việc gắn kết',
                              style: TextStyle(
                                fontSize: 12,
                                color: tokens.textSecondary,
                              ),
                            ),
                          ],
                        ),
                      ),
                      IconButton(
                        tooltip: 'Xóa dự án',
                        icon: Icon(
                          Icons.delete_outline_rounded,
                          color: tokens.textSecondary,
                        ),
                        onPressed: () async {
                          final bool ok = await DestructiveConfirmDialog.show(
                            context,
                            title: ViStrings.confirmDeleteTitle,
                            consequenceMessage:
                                ViStrings.confirmDeleteProjectMessage(
                              name,
                              taskCount,
                            ),
                          );
                          if (ok) {
                            repo.deleteProject(
                              userId: userId,
                              projectId: proj.id,
                            );
                          }
                        },
                      ),
                    ],
                  ),
                ),
              );
            }).toList(),
          ),

        const SizedBox(height: LifeOsTokens.space5),

        // Lịch & Sự kiện lặp
        Wrap(
          spacing: 8,
          runSpacing: 8,
          alignment: WrapAlignment.spaceBetween,
          crossAxisAlignment: WrapCrossAlignment.center,
          children: <Widget>[
            Text(
              'Lịch & Sự kiện lặp',
              style: Theme.of(context).textTheme.titleLarge,
            ),
            FilledButton.tonalIcon(
              onPressed: () {
                repo.createCalendarEvent(
                  userId: userId,
                  title: 'Họp đồng bộ tuần',
                  startLocal: '${todayIso}T09:00:00',
                  endLocal: '${todayIso}T10:00:00',
                  recurrence: const <String, dynamic>{
                    'freq': 'WEEKLY',
                    'interval': 1,
                    'byWeekday': <int>[1, 3, 5],
                    'count': 6,
                  },
                );
              },
              icon: const Icon(Icons.event_available_outlined, size: 18),
              label: const Text('Thêm sự kiện lặp'),
            ),
          ],
        ),
        const SizedBox(height: LifeOsTokens.space2),
        if (events.isEmpty)
          const LifeOsEmptyState(
            icon: Icons.calendar_today_outlined,
            title: 'Lịch trình đang trống',
            subtitle:
                'Hỗ trợ sự kiện lặp lại, ngoại lệ sự kiện và khối thời gian tập trung.',
          )
        else
          Column(
            children: events.map((LocalEntityRow ev) {
              final Object? rec = ev.payload['recurrence'];
              final String? rawFreq =
                  rec is Map ? (rec['freq'] as String?)?.toUpperCase() : null;
              final String freqLabel = switch (rawFreq) {
                'DAILY' => 'Hằng ngày',
                'WEEKLY' => 'Hằng tuần',
                'MONTHLY' => 'Hằng tháng',
                'YEARLY' => 'Hằng năm',
                _ => 'Sự kiện đơn',
              };
              final String startRaw =
                  (ev.payload['startLocal'] ?? '').toString();
              return Padding(
                padding: const EdgeInsets.only(bottom: LifeOsTokens.space2),
                child: LifeOsCard(
                  elevation: 1,
                  padding: const EdgeInsets.all(LifeOsTokens.space3),
                  child: Row(
                    children: <Widget>[
                      Container(
                        padding: const EdgeInsets.all(10),
                        decoration: BoxDecoration(
                          color: tokens.domainCalendar.withValues(alpha: 0.12),
                          borderRadius:
                              BorderRadius.circular(LifeOsTokens.radiusMd),
                        ),
                        child: Icon(
                          Icons.event_repeat_rounded,
                          color: tokens.domainCalendar,
                          size: 20,
                        ),
                      ),
                      const SizedBox(width: LifeOsTokens.space3),
                      Expanded(
                        child: Column(
                          crossAxisAlignment: CrossAxisAlignment.start,
                          children: <Widget>[
                            Text(
                              (ev.payload['title'] as String?) ?? 'Sự kiện',
                              style: TextStyle(
                                fontWeight: FontWeight.w600,
                                fontSize: 15,
                                color: tokens.textPrimary,
                              ),
                            ),
                            const SizedBox(height: 4),
                            Wrap(
                              spacing: 6,
                              runSpacing: 4,
                              crossAxisAlignment: WrapCrossAlignment.center,
                              children: <Widget>[
                                Container(
                                  padding: const EdgeInsets.symmetric(
                                    horizontal: 8,
                                    vertical: 2,
                                  ),
                                  decoration: BoxDecoration(
                                    color: tokens.domainCalendar
                                        .withValues(alpha: 0.1),
                                    borderRadius: BorderRadius.circular(
                                      LifeOsTokens.radiusPill,
                                    ),
                                  ),
                                  child: Text(
                                    freqLabel,
                                    style: TextStyle(
                                      fontSize: 11,
                                      fontWeight: FontWeight.w600,
                                      color: tokens.domainCalendar,
                                    ),
                                  ),
                                ),
                                if (startRaw.isNotEmpty)
                                  Text(
                                    startRaw.replaceAll('T', ' '),
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
