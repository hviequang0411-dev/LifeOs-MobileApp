import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../../core/database/app_database.dart';
import '../../../../core/l10n/vi_strings.dart';
import '../../../../core/providers/app_providers.dart';
import '../../../../core/sync/entity_types.dart';
import '../../../../core/theme/app_tokens.dart';
import '../../../../core/widgets/lifeos_widgets.dart';
import '../../../workspace/data/lifeos_local_repository.dart';
import 'routine_player_sheet.dart';

/// Khối hiển thị **Chuỗi thói quen (`Routine`)** trong Tab Kế hoạch + mở **Trình chạy Routine (`MWP-C`)**.
class PlanRoutinesSection extends ConsumerWidget {
  const PlanRoutinesSection({super.key});

  Future<void> _openCreateRoutineDialog(
    BuildContext context,
    WidgetRef ref,
    String userId,
    LifeOsLocalRepository repo,
  ) async {
    final TextEditingController nameCtrl = TextEditingController();
    final TextEditingController timeCtrl =
        TextEditingController(text: '07:00');
    final TextEditingController stepsCtrl = TextEditingController(
      text: 'Uống 1 ly nước ấm (2p)\nGiãn cơ & hít thở (5p)\nChọn 3 việc ưu tiên (5p)',
    );
    final bool? created = await showDialog<bool>(
      context: context,
      builder: (BuildContext ctx) => AlertDialog(
        title: const Text('Tạo chuỗi thói quen mới'),
        content: SingleChildScrollView(
          child: Column(
            mainAxisSize: MainAxisSize.min,
            children: <Widget>[
              TextField(
                key: const Key('input_create_routine_name'),
                controller: nameCtrl,
                decoration: const InputDecoration(
                  labelText: 'Tên chuỗi thói quen *',
                  hintText: 'Ví dụ: Nghi thức buổi sáng',
                  border: OutlineInputBorder(),
                ),
              ),
              const SizedBox(height: 10),
              TextField(
                key: const Key('input_create_routine_time'),
                controller: timeCtrl,
                decoration: const InputDecoration(
                  labelText: 'Giờ nhắc nhở (tùy chọn)',
                  hintText: 'HH:mm',
                  border: OutlineInputBorder(),
                ),
              ),
              const SizedBox(height: 10),
              TextField(
                key: const Key('input_create_routine_steps'),
                controller: stepsCtrl,
                maxLines: 3,
                decoration: const InputDecoration(
                  labelText: 'Các bước thực hiện (mỗi dòng 1 bước)',
                  border: OutlineInputBorder(),
                ),
              ),
            ],
          ),
        ),
        actions: <Widget>[
          TextButton(
            onPressed: () => Navigator.of(ctx).pop(false),
            child: const Text(ViStrings.cancelAction),
          ),
          FilledButton(
            key: const Key('btn_confirm_create_routine'),
            onPressed: () {
              if (nameCtrl.text.trim().isEmpty) return;
              Navigator.of(ctx).pop(true);
            },
            child: const Text('Tạo chuỗi'),
          ),
        ],
      ),
    );

    if (created == true) {
      final List<String> lines = stepsCtrl.text
          .split('\n')
          .map((String e) => e.trim())
          .where((String e) => e.isNotEmpty)
          .toList();
      final List<String> titles = lines.isEmpty
          ? <String>['Bước 1']
          : lines;
      repo.tasks.createRoutine(
        userId: userId,
        name: nameCtrl.text,
        kind: 'DAILY',
        reminderTime: timeCtrl.text.trim().isEmpty ? null : timeCtrl.text.trim(),
        stepTitles: titles,
        stepDurationsMinutes: List<int>.filled(titles.length, 5),
        nowUtc: ref.read(referenceUtcProvider),
      );
      final NotificationPreferences prefs =
          ref.read(notificationPreferencesProvider);
      if (prefs.masterEnabled && timeCtrl.text.trim().isNotEmpty) {
        await ref
            .read(localNotificationCoordinatorProvider)
            .rescheduleFromDatabase(
              userId: userId,
              userSettings: repo.getUserSettings(userId),
              preferences: prefs,
              nowUtc: ref.read(referenceUtcProvider),
            );
      }
    }
    nameCtrl.dispose();
    timeCtrl.dispose();
    stepsCtrl.dispose();
  }

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final LifeOsTokens tokens = LifeOsTokens.of(context);
    final String userId = ref.watch(currentUserIdProvider);
    final LifeOsLocalRepository repo = ref.watch(localRepositoryProvider);
    final List<LocalEntityRow> routines = ref
            .watch(entitiesByTypeStreamProvider(SyncEntityType.routine))
            .valueOrNull ??
        const <LocalEntityRow>[];
    ref.watch(entitiesByTypeStreamProvider(SyncEntityType.routineStep));

    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: <Widget>[
        Wrap(
          spacing: 8,
          runSpacing: 8,
          alignment: WrapAlignment.spaceBetween,
          crossAxisAlignment: WrapCrossAlignment.center,
          children: <Widget>[
            Text(
              'Chuỗi thói quen (Routine)',
              style: Theme.of(context).textTheme.titleLarge,
            ),
            FilledButton.tonalIcon(
              key: const Key('btn_open_create_routine'),
              onPressed: () =>
                  _openCreateRoutineDialog(context, ref, userId, repo),
              icon: const Icon(Icons.playlist_add_outlined, size: 18),
              label: const Text('Thêm chuỗi'),
            ),
          ],
        ),
        const SizedBox(height: LifeOsTokens.space2),
        if (routines.isEmpty)
          const LifeOsEmptyState(
            icon: Icons.wb_twilight_rounded,
            title: 'Chưa có chuỗi thói quen nào',
            subtitle:
                'Tạo chuỗi thói quen sáng/tối gồm nhiều bước có đồng hồ đếm ngược.',
          )
        else
          Column(
            children: routines.map((LocalEntityRow r) {
              final List<LocalEntityRow> steps = repo.tasks.listRoutineSteps(
                userId: userId,
                routineId: r.id,
              );
              final String? reminderTime =
                  r.payload['reminderTime'] as String?;
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
                          color: tokens.domainHabit.withValues(alpha: 0.12),
                          borderRadius:
                              BorderRadius.circular(LifeOsTokens.radiusMd),
                        ),
                        child: Icon(
                          Icons.play_lesson_outlined,
                          color: tokens.domainHabit,
                          size: 20,
                        ),
                      ),
                      const SizedBox(width: LifeOsTokens.space3),
                      Expanded(
                        child: Column(
                          crossAxisAlignment: CrossAxisAlignment.start,
                          children: <Widget>[
                            Text(
                              (r.payload['name'] as String?) ??
                                  'Chuỗi thói quen',
                              style: TextStyle(
                                fontWeight: FontWeight.w700,
                                fontSize: 15,
                                color: tokens.textPrimary,
                              ),
                            ),
                            const SizedBox(height: 2),
                            Text(
                              '${steps.length} bước${reminderTime != null && reminderTime.isNotEmpty ? ' · Nhắc lúc $reminderTime' : ''}',
                              style: TextStyle(
                                fontSize: 12.5,
                                color: tokens.textSecondary,
                              ),
                            ),
                          ],
                        ),
                      ),
                      FilledButton.tonalIcon(
                        key: Key('btn_run_routine_${r.id}'),
                        style: FilledButton.styleFrom(
                          minimumSize:
                              const Size(96, LifeOsTokens.minTouchTarget),
                        ),
                        onPressed: () {
                          showModalBottomSheet<void>(
                            context: context,
                            isScrollControlled: true,
                            useSafeArea: true,
                            backgroundColor: Colors.transparent,
                            builder: (BuildContext ctx) =>
                                RoutinePlayerSheet(routine: r),
                          );
                        },
                        icon: const Icon(Icons.play_arrow_rounded, size: 18),
                        label: const Text('Bắt đầu'),
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
