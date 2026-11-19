import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../core/calculators/recurrence_calculator.dart';
import '../../../core/database/app_database.dart';
import '../../../core/l10n/vi_strings.dart';
import '../../../core/providers/app_providers.dart';
import '../../../core/sync/entity_types.dart';
import '../../../core/theme/app_tokens.dart';
import '../../../core/time/user_day_clock.dart';
import '../../../core/widgets/lifeos_widgets.dart';
import '../../tasks/presentation/widgets/routines_section_card.dart';
import '../../tasks/presentation/widgets/task_editor_sheet.dart';
import '../../workspace/data/lifeos_local_repository.dart';
import 'widgets/daily_timeline_widget.dart';
import 'widgets/plan_forms_sheets.dart';

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

  static String? _weekdayCode(int day) => switch (day) {
        1 => 'MO',
        2 => 'TU',
        3 => 'WE',
        4 => 'TH',
        5 => 'FR',
        6 => 'SA',
        7 => 'SU',
        _ => null,
      };

  static DateTime _parseWallToUtc(String iso, int offsetMinutes) {
    final DateTime? parsed = DateTime.tryParse(iso);
    if (parsed == null) return DateTime.now().toUtc();
    if (parsed.isUtc ||
        iso.endsWith('Z') ||
        RegExp(r'[+-]\d{2}:?\d{2}$').hasMatch(iso)) {
      return parsed.toUtc();
    }
    return DateTime.utc(
      parsed.year,
      parsed.month,
      parsed.day,
      parsed.hour,
      parsed.minute,
      parsed.second,
    ).subtract(Duration(minutes: offsetMinutes));
  }

  static String _formatWallIsoSecond(DateTime wallUtc) {
    final String y = wallUtc.year.toString().padLeft(4, '0');
    final String m = wallUtc.month.toString().padLeft(2, '0');
    final String d = wallUtc.day.toString().padLeft(2, '0');
    final String h = wallUtc.hour.toString().padLeft(2, '0');
    final String min = wallUtc.minute.toString().padLeft(2, '0');
    final String s = wallUtc.second.toString().padLeft(2, '0');
    return '$y-$m-${d}T$h:$min:$s';
  }

  static String? _mapRecurrenceToRrule(
    Map<dynamic, dynamic> rec,
    String? untilOverride,
  ) {
    final String freq = (rec['freq'] as String? ?? '').trim().toUpperCase();
    if (freq.isEmpty || freq == 'NONE') return null;
    final List<String> parts = <String>['FREQ=$freq'];
    final int interval = (rec['interval'] as num?)?.toInt() ?? 1;
    if (interval > 1) parts.add('INTERVAL=$interval');
    final int? count = (rec['count'] as num?)?.toInt();
    if (count != null && count > 0) parts.add('COUNT=$count');

    final List<dynamic>? byWeekday = rec['byWeekday'] as List<dynamic>?;
    if (byWeekday != null && byWeekday.isNotEmpty) {
      final List<String> codes = byWeekday
          .whereType<num>()
          .map((num n) => _weekdayCode(n.toInt()))
          .whereType<String>()
          .toList();
      if (codes.isNotEmpty) {
        parts.add('BYDAY=${codes.join(',')}');
      }
    }
    final int? byMonthDay = (rec['byMonthDay'] as num?)?.toInt();
    if (byMonthDay != null && byMonthDay >= 1 && byMonthDay <= 31) {
      parts.add('BYMONTHDAY=$byMonthDay');
    }
    final String rawUntil =
        (untilOverride ?? (rec['until'] as String?) ?? '').trim();
    if (rawUntil.length >= 10) {
      final String compact = rawUntil.substring(0, 10).replaceAll('-', '');
      if (compact.length == 8) {
        parts.add('UNTIL=${compact}T235959Z');
      }
    }
    return parts.join(';');
  }

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
    final List<LocalEntityRow> eventExceptions = ref
            .watch(
              entitiesByTypeStreamProvider(
                SyncEntityType.calendarEventException,
              ),
            )
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
    final UserDayClock clock = repo.getUserDayClock(userId);
    final String todayIso = clock.businessDateForUtc(refUtc);
    final String selectedIso =
        UserDayClock.addDays(todayIso, _selectedDayOffset);
    final DateTime selectedDate = UserDayClock.parseIsoDateUtc(selectedIso);

    final DateTime rangeFromUtc =
        DateTime.utc(selectedDate.year, selectedDate.month, selectedDate.day)
            .subtract(Duration(minutes: clock.utcOffsetMinutes));
    final DateTime rangeToUtc = rangeFromUtc.add(const Duration(days: 1));

    final Map<String, List<LocalEntityRow>> exceptionsByEvent =
        <String, List<LocalEntityRow>>{};
    for (final LocalEntityRow ex in eventExceptions) {
      final String? eid = ex.payload['eventId'] as String?;
      if (eid != null) {
        exceptionsByEvent.putIfAbsent(eid, () => <LocalEntityRow>[]).add(ex);
      }
    }
    final Map<String, LocalEntityRow> eventById = <String, LocalEntityRow>{
      for (final LocalEntityRow ev in events) ev.id: ev,
    };
    final Map<String, LocalEntityRow> timeBlockById = <String, LocalEntityRow>{
      for (final LocalEntityRow tb in timeBlocks) tb.id: tb,
    };
    final Map<String, LocalEntityRow> taskById = <String, LocalEntityRow>{
      for (final LocalEntityRow t in tasks) t.id: t,
    };

    // Dựng danh sách dòng thời gian kết hợp cho `selectedIso` (không rò rỉ sang ngày khác)
    final String defaultTz =
        (repo.getUserSettings(userId)['timezone'] as String?) ??
            'Asia/Ho_Chi_Minh';
    final List<TimelineEntryItem> timelineItems = <TimelineEntryItem>[];
    for (final LocalEntityRow ev in events) {
      final String evTz = (ev.payload['timezone'] as String?) ?? defaultTz;
      final UserDayClock evClock =
          UserDayClock.fromUserSettings(timezone: evTz);
      final bool isAllDay = ev.payload['allDay'] == true;
      final Object? rawRec = ev.payload['recurrence'];

      if (rawRec is Map &&
          (rawRec['freq'] as String? ?? '').trim().isNotEmpty &&
          (rawRec['freq'] as String).toUpperCase() != 'NONE') {
        final String? rrule = _mapRecurrenceToRrule(
          rawRec,
          ev.payload['recurrenceUntil'] as String?,
        );
        final String rawStart = (ev.payload['startLocal'] ??
                ev.payload['startAt'] ??
                '${ev.payload['startDate'] ?? todayIso}T09:00:00')
            .toString();
        final String rawEnd = (ev.payload['endLocal'] ??
                ev.payload['endAt'] ??
                '${ev.payload['endDate'] ?? todayIso}T10:00:00')
            .toString();
        final DateTime startAtUtc =
            _parseWallToUtc(rawStart, evClock.utcOffsetMinutes);
        DateTime endAtUtc = _parseWallToUtc(rawEnd, evClock.utcOffsetMinutes);
        if (!endAtUtc.isAfter(startAtUtc)) {
          endAtUtc = startAtUtc.add(const Duration(hours: 1));
        }

        final List<RecurrenceExceptionInput> exInputs =
            (exceptionsByEvent[ev.id] ?? const <LocalEntityRow>[])
                .map((LocalEntityRow ex) {
          final String origStr =
              (ex.payload['originalStartLocal'] as String?) ?? rawStart;
          final String? newStartStr = ex.payload['startLocal'] as String?;
          final String? newEndStr = ex.payload['endLocal'] as String?;
          return RecurrenceExceptionInput(
            originalStartAt:
                _parseWallToUtc(origStr, evClock.utcOffsetMinutes),
            action: (ex.payload['kind'] as String? ?? 'CANCELLED').toUpperCase(),
            newStartAt: newStartStr != null
                ? _parseWallToUtc(newStartStr, evClock.utcOffsetMinutes)
                : null,
            newEndAt: newEndStr != null
                ? _parseWallToUtc(newEndStr, evClock.utcOffsetMinutes)
                : null,
            newTitle: ex.payload['title'] as String?,
          );
        }).toList();

        final List<ExpandedOccurrence> occs = RecurrenceCalculator.expand(
          startAtUtc: startAtUtc,
          endAtUtc: endAtUtc,
          timezone: evTz,
          rrule: rrule,
          rangeFromUtc: rangeFromUtc,
          rangeToUtc: rangeToUtc,
          exceptions: exInputs,
        );

        for (final ExpandedOccurrence occ in occs) {
          if (clock.businessDateForUtc(occ.startAt) != selectedIso) continue;
          final DateTime effWall = clock.toUserWallClock(occ.startAt);
          final DateTime origWall = evClock.toUserWallClock(occ.originalStartAt);
          final String timePart = isAllDay
              ? 'Cả ngày'
              : '${effWall.hour.toString().padLeft(2, '0')}:${effWall.minute.toString().padLeft(2, '0')}';
          final int mins =
              occ.endAt.difference(occ.startAt).inMinutes.clamp(15, 720);
          timelineItems.add(
            TimelineEntryItem(
              id: '${ev.id}@${_formatWallIsoSecond(origWall)}',
              entityId: ev.id,
              occurrenceStartLocal: _formatWallIsoSecond(origWall),
              title: occ.titleOverride ??
                  (ev.payload['title'] as String?) ??
                  'Sự kiện lịch',
              timeStr: timePart,
              kind: 'EVENT',
              isDone: false,
              durationMinutes: isAllDay ? null : mins,
            ),
          );
        }
      } else if (isAllDay) {
        final String? rawStartLocal = ev.payload['startLocal'] as String?;
        final String? sDate = (ev.payload['startDate'] as String?) ??
            ((rawStartLocal != null && rawStartLocal.length >= 10)
                ? rawStartLocal.substring(0, 10)
                : null);
        final String? eDate = (ev.payload['endDate'] as String?) ?? sDate;
        if (sDate != null &&
            eDate != null &&
            selectedIso.compareTo(sDate) >= 0 &&
            selectedIso.compareTo(eDate) <= 0) {
          timelineItems.add(
            TimelineEntryItem(
              id: ev.id,
              entityId: ev.id,
              title: (ev.payload['title'] as String?) ?? 'Sự kiện cả ngày',
              timeStr: 'Cả ngày',
              kind: 'EVENT',
              isDone: false,
            ),
          );
        }
      } else {
        final String startLocal =
            (ev.payload['startLocal'] ?? ev.payload['startAt'] ?? '')
                .toString();
        final String evDate = startLocal.contains('Z')
            ? clock.businessDateForUtc(
                DateTime.tryParse(startLocal)?.toUtc() ?? refUtc,
              )
            : (startLocal.length >= 10 ? startLocal.substring(0, 10) : '');
        if (evDate == selectedIso) {
          final String timePart = startLocal.contains('T') &&
                  startLocal.split('T').last.length >= 5
              ? startLocal.split('T').last.substring(0, 5)
              : '09:00';
          timelineItems.add(
            TimelineEntryItem(
              id: ev.id,
              entityId: ev.id,
              title: (ev.payload['title'] as String?) ?? 'Sự kiện lịch',
              timeStr: timePart,
              kind: 'EVENT',
              isDone: false,
              durationMinutes: 60,
            ),
          );
        }
      }
    }

    for (final LocalEntityRow tb in timeBlocks) {
      final String startRaw =
          (tb.payload['startLocal'] ?? tb.payload['startAt'] ?? '').toString();
      if (startRaw.isEmpty) continue;
      final DateTime? parsedStart = DateTime.tryParse(startRaw);
      String tbDate = '';
      String timePart = '14:00';
      if (parsedStart != null &&
          (startRaw.endsWith('Z') || tb.payload['startAt'] != null)) {
        final DateTime utcStart = parsedStart.toUtc();
        tbDate = clock.businessDateForUtc(utcStart);
        final DateTime wall = clock.toUserWallClock(utcStart);
        timePart =
            '${wall.hour.toString().padLeft(2, '0')}:${wall.minute.toString().padLeft(2, '0')}';
      } else {
        tbDate = startRaw.length >= 10 ? startRaw.substring(0, 10) : '';
        if (startRaw.contains('T') && startRaw.split('T').last.length >= 5) {
          timePart = startRaw.split('T').last.substring(0, 5);
        }
      }
      if (tbDate != selectedIso) continue;

      int durationMins = (tb.payload['durationMinutes'] as num?)?.toInt() ?? 45;
      final String endRaw =
          (tb.payload['endLocal'] ?? tb.payload['endAt'] ?? '').toString();
      final DateTime? parsedEnd = DateTime.tryParse(endRaw);
      if (parsedStart != null &&
          parsedEnd != null &&
          parsedEnd.isAfter(parsedStart)) {
        durationMins =
            parsedEnd.difference(parsedStart).inMinutes.clamp(15, 720);
      }

      final String? linkedTaskTitle = tb.payload['taskId'] != null
          ? (taskById[tb.payload['taskId']]?.payload['title'] as String?)
          : null;
      final String tbTitle =
          ((tb.payload['note'] as String?)?.trim().isNotEmpty == true)
              ? (tb.payload['note'] as String).trim()
              : (linkedTaskTitle ??
                  (tb.payload['title'] as String?) ??
                  'Khối tập trung sâu');

      timelineItems.add(
        TimelineEntryItem(
          id: tb.id,
          entityId: tb.id,
          title: tbTitle,
          timeStr: timePart,
          kind: 'TIME_BLOCK',
          isDone: false,
          durationMinutes: durationMins,
        ),
      );
    }

    int unscheduledAdded = 0;
    for (final LocalEntityRow t in tasks) {
      final String? dueDate = t.payload['dueDate'] as String?;
      final bool isForSelectedDate = dueDate == selectedIso;
      final bool isUnscheduledToday =
          dueDate == null && _selectedDayOffset == 0 && unscheduledAdded < 4;
      if (!isForSelectedDate && !isUnscheduledToday) continue;
      if (dueDate == null) unscheduledAdded++;
      final String status =
          (t.payload['status'] ?? 'OPEN').toString().toUpperCase();
      timelineItems.add(
        TimelineEntryItem(
          id: t.id,
          entityId: t.id,
          title: (t.payload['title'] as String?) ?? 'Công việc',
          timeStr: (t.payload['dueTime'] as String?)?.length == 5
              ? (t.payload['dueTime'] as String)
              : 'Trong ngày',
          kind: 'TASK',
          isDone: status == 'DONE' || status == 'COMPLETED',
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
                  final DateTime d = UserDayClock.parseIsoDateUtc(
                    UserDayClock.addDays(todayIso, offset),
                  );
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
        DailyTimelineWidget(
          items: timelineItems,
          onItemTap: (TimelineEntryItem item) {
            final String entityId = item.entityId ?? item.id;
            if (item.kind == 'EVENT' && eventById.containsKey(entityId)) {
              showModalBottomSheet<void>(
                context: context,
                isScrollControlled: true,
                useSafeArea: true,
                backgroundColor: Colors.transparent,
                builder: (_) => CalendarEventSheet(
                  event: eventById[entityId],
                  initialDateIso: selectedIso,
                  occurrenceStartLocal: item.occurrenceStartLocal,
                ),
              );
            } else if (item.kind == 'TIME_BLOCK' &&
                timeBlockById.containsKey(entityId)) {
              showModalBottomSheet<void>(
                context: context,
                isScrollControlled: true,
                useSafeArea: true,
                backgroundColor: Colors.transparent,
                builder: (_) => TimeBlockSheet(
                  timeBlock: timeBlockById[entityId],
                  initialDateIso: selectedIso,
                ),
              );
            } else if (item.kind == 'TASK' && taskById.containsKey(entityId)) {
              showModalBottomSheet<void>(
                context: context,
                isScrollControlled: true,
                useSafeArea: true,
                backgroundColor: Colors.transparent,
                builder: (_) => TaskEditorSheet(task: taskById[entityId]!),
              );
            }
          },
        ),

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
              key: const Key('btn_open_create_project'),
              onPressed: () {
                showModalBottomSheet<void>(
                  context: context,
                  isScrollControlled: true,
                  useSafeArea: true,
                  backgroundColor: Colors.transparent,
                  builder: (_) => const ProjectEditorSheet(),
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
                child: InkWell(
                  borderRadius: BorderRadius.circular(LifeOsTokens.radiusMd),
                  onTap: () {
                    showModalBottomSheet<void>(
                      context: context,
                      isScrollControlled: true,
                      useSafeArea: true,
                      backgroundColor: Colors.transparent,
                      builder: (_) => ProjectEditorSheet(
                        project: proj,
                        taskCount: taskCount,
                      ),
                    );
                  },
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
            Wrap(
              spacing: 8,
              runSpacing: 8,
              children: <Widget>[
                OutlinedButton.icon(
                  key: const Key('btn_open_create_timeblock'),
                  onPressed: () {
                    showModalBottomSheet<void>(
                      context: context,
                      isScrollControlled: true,
                      useSafeArea: true,
                      backgroundColor: Colors.transparent,
                      builder: (_) =>
                          TimeBlockSheet(initialDateIso: selectedIso),
                    );
                  },
                  icon: const Icon(Icons.timelapse_rounded, size: 18),
                  label: const Text('Khối tập trung'),
                ),
                FilledButton.tonalIcon(
                  key: const Key('btn_open_create_event'),
                  onPressed: () {
                    showModalBottomSheet<void>(
                      context: context,
                      isScrollControlled: true,
                      useSafeArea: true,
                      backgroundColor: Colors.transparent,
                      builder: (_) =>
                          CalendarEventSheet(initialDateIso: selectedIso),
                    );
                  },
                  icon: const Icon(Icons.event_available_outlined, size: 18),
                  label: const Text('Thêm sự kiện'),
                ),
              ],
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
                child: InkWell(
                  borderRadius: BorderRadius.circular(LifeOsTokens.radiusMd),
                  onTap: () {
                    showModalBottomSheet<void>(
                      context: context,
                      isScrollControlled: true,
                      useSafeArea: true,
                      backgroundColor: Colors.transparent,
                      builder: (_) => CalendarEventSheet(
                        event: ev,
                        initialDateIso: selectedIso,
                      ),
                    );
                  },
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
                ),
              );
            }).toList(),
          ),
      ],
    );
  }
}
