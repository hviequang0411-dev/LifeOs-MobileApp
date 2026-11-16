import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../../core/database/app_database.dart';
import '../../../../core/providers/app_providers.dart';
import '../../../../core/sync/entity_types.dart';
import '../../../../core/theme/app_tokens.dart';
import '../../../notification/data/local_notification_service.dart';
import '../../../workspace/data/lifeos_local_repository.dart';

/// Bảng Cài đặt Thông báo & Giờ im lặng (`MWP-A`):
/// - Xin quyền `POST_NOTIFICATIONS` / iOS Alert ĐÚNG LÚC người dùng bật thông báo.
/// - Bật/tắt theo loại (Công việc, Thói quen, Chuỗi thói quen, Lịch & Sự kiện).
/// - Giờ im lặng (`quietStart`/`quietEnd`) & Quyền riêng tư màn hình khóa đọc/ghi trực tiếp vào `user_settings`.
class NotificationSettingsSheet extends ConsumerStatefulWidget {
  const NotificationSettingsSheet({super.key});

  @override
  ConsumerState<NotificationSettingsSheet> createState() =>
      _NotificationSettingsSheetState();
}

class _NotificationSettingsSheetState
    extends ConsumerState<NotificationSettingsSheet> {
  bool _saving = false;
  String? _statusMessage;

  Future<void> _reschedule({bool requestPermissionFirst = false}) async {
    setState(() => _saving = true);
    try {
      final String userId = ref.read(currentUserIdProvider);
      final LifeOsLocalRepository repo = ref.read(localRepositoryProvider);
      final LocalNotificationCoordinator coordinator =
          ref.read(localNotificationCoordinatorProvider);
      NotificationPreferences prefs = ref.read(notificationPreferencesProvider);

      if (requestPermissionFirst && !prefs.permissionRequestedOnce) {
        final bool granted = await coordinator.requestPermissionOnUserEnable();
        prefs = prefs.copyWith(permissionRequestedOnce: true);
        ref.read(notificationPreferencesProvider.notifier).state = prefs;
        if (!granted && mounted) {
          _statusMessage =
              'Thiết bị chưa cấp quyền thông báo. Bạn có thể bật trong Cài đặt hệ thống.';
        }
      }

      final Map<String, dynamic> settings = repo.getUserSettings(userId);
      final List<ScheduledNotificationRecord> scheduled =
          await coordinator.rescheduleFromDatabase(
        userId: userId,
        userSettings: settings,
        preferences: prefs,
        nowUtc: ref.read(referenceUtcProvider),
      );
      if (mounted) {
        setState(() {
          _statusMessage ??=
              'Đã cập nhật lịch nhắc cục bộ (${scheduled.length}/${LocalNotificationScheduler.maxIosPendingNotifications} mục).';
        });
      }
    } finally {
      if (mounted) {
        setState(() => _saving = false);
      }
    }
  }

  Future<void> _updatePreference(
    NotificationPreferences nextPrefs, {
    bool requestPermissionIfEnabling = false,
  }) async {
    ref.read(notificationPreferencesProvider.notifier).state = nextPrefs;
    _statusMessage = null;
    await _reschedule(requestPermissionFirst: requestPermissionIfEnabling);
  }

  TimeOfDay _parseTimeOrDefault(String? raw, TimeOfDay fallback) {
    if (raw == null || !raw.contains(':')) return fallback;
    final List<String> parts = raw.split(':');
    final int? h = int.tryParse(parts[0]);
    final int? m = parts.length > 1 ? int.tryParse(parts[1]) : 0;
    if (h == null || m == null || h < 0 || h > 23 || m < 0 || m > 59) {
      return fallback;
    }
    return TimeOfDay(hour: h, minute: m);
  }

  String _formatTime(TimeOfDay t) =>
      '${t.hour.toString().padLeft(2, '0')}:${t.minute.toString().padLeft(2, '0')}';

  Future<void> _pickQuietTime({
    required bool isStart,
    required String? currentStart,
    required String? currentEnd,
  }) async {
    final TimeOfDay initial = isStart
        ? _parseTimeOrDefault(currentStart, const TimeOfDay(hour: 22, minute: 0))
        : _parseTimeOrDefault(currentEnd, const TimeOfDay(hour: 7, minute: 0));
    final TimeOfDay? picked = await showTimePicker(
      context: context,
      initialTime: initial,
    );
    if (picked == null || !mounted) return;

    final String nextStart =
        isStart ? _formatTime(picked) : (currentStart ?? '22:00');
    String nextEnd = isStart ? (currentEnd ?? '07:00') : _formatTime(picked);
    if (nextStart == nextEnd) {
      // Đảm bảo quy tắc backend: quietStart và quietEnd phải khác nhau.
      final int nextMin = (picked.hour * 60 + picked.minute + 60) % (24 * 60);
      nextEnd = _formatTime(
        TimeOfDay(hour: nextMin ~/ 60, minute: nextMin % 60),
      );
    }

    final String userId = ref.read(currentUserIdProvider);
    ref.read(localRepositoryProvider).updateUserSettings(
      userId: userId,
      patch: <String, dynamic>{
        'quietStart': nextStart,
        'quietEnd': nextEnd,
      },
    );
    _statusMessage = null;
    await _reschedule();
  }

  @override
  Widget build(BuildContext context) {
    final LifeOsTokens tokens = LifeOsTokens.of(context);
    final String userId = ref.watch(currentUserIdProvider);
    final LifeOsLocalRepository repo = ref.watch(localRepositoryProvider);
    ref.watch(entitiesByTypeStreamProvider(SyncEntityType.userSettings));

    final Map<String, dynamic> settings = repo.getUserSettings(userId);
    final String? quietStart = settings['quietStart'] as String?;
    final String? quietEnd = settings['quietEnd'] as String?;
    final bool quietEnabled = quietStart != null && quietEnd != null;
    final bool showDetailsOnLockScreen =
        settings['showDetailsOnLockScreen'] == true;

    final NotificationPreferences prefs =
        ref.watch(notificationPreferencesProvider);
    final FcmPushCoordinator fcm = ref.watch(fcmPushCoordinatorProvider);
    final int scheduledCount =
        repo.db.listScheduledNotifications(userId).length;
    final double bottomInset = MediaQuery.viewInsetsOf(context).bottom;

    return Material(
      color: tokens.bgSurface,
      borderRadius: const BorderRadius.vertical(
        top: Radius.circular(LifeOsTokens.radiusLg),
      ),
      clipBehavior: Clip.antiAlias,
      child: Padding(
        padding: EdgeInsets.fromLTRB(20, 16, 20, 20 + bottomInset),
      child: SingleChildScrollView(
        child: Column(
          mainAxisSize: MainAxisSize.min,
          crossAxisAlignment: CrossAxisAlignment.stretch,
          children: <Widget>[
            Center(
              child: Container(
                width: 40,
                height: 4,
                decoration: BoxDecoration(
                  color: tokens.borderSubtle,
                  borderRadius: BorderRadius.circular(2),
                ),
              ),
            ),
            const SizedBox(height: 12),
            Text(
              'Thông báo & Giờ im lặng',
              style: Theme.of(context).textTheme.titleLarge,
            ),
            const SizedBox(height: 4),
            Text(
              'Lịch nhắc chạy cục bộ trên thiết bị theo múi giờ ${(settings['timezone'] as String?) ?? 'Asia/Ho_Chi_Minh'} · Đang chờ $scheduledCount/${LocalNotificationScheduler.maxIosPendingNotifications}.',
              style: TextStyle(fontSize: 13, color: tokens.textSecondary),
            ),
            if (_statusMessage != null) ...<Widget>[
              const SizedBox(height: 10),
              Container(
                padding: const EdgeInsets.all(10),
                decoration: BoxDecoration(
                  color: tokens.accentPrimary.withValues(alpha: 0.1),
                  borderRadius: BorderRadius.circular(LifeOsTokens.radiusMd),
                ),
                child: Text(
                  _statusMessage!,
                  style: TextStyle(
                    fontSize: 12.5,
                    fontWeight: FontWeight.w600,
                    color: tokens.accentPrimary,
                  ),
                ),
              ),
            ],
            const SizedBox(height: 8),
            SwitchListTile(
              key: const Key('switch_master_notifications'),
              contentPadding: EdgeInsets.zero,
              title: const Text('Bật thông báo nhắc nhở'),
              subtitle: const Text(
                'Tự động lập lịch nhắc công việc, thói quen và lịch hẹn khi app đóng.',
              ),
              value: prefs.masterEnabled,
              onChanged: (bool value) => _updatePreference(
                prefs.copyWith(masterEnabled: value),
                requestPermissionIfEnabling: value,
              ),
            ),
            const Divider(height: 1),
            SwitchListTile(
              key: const Key('switch_type_task'),
              contentPadding: EdgeInsets.zero,
              title: const Text('Nhắc công việc'),
              value: prefs.masterEnabled && prefs.taskRemindersEnabled,
              onChanged: prefs.masterEnabled
                  ? (bool v) => _updatePreference(
                        prefs.copyWith(taskRemindersEnabled: v),
                        requestPermissionIfEnabling: v,
                      )
                  : null,
            ),
            SwitchListTile(
              key: const Key('switch_type_habit'),
              contentPadding: EdgeInsets.zero,
              title: const Text('Nhắc thói quen hằng ngày'),
              value: prefs.masterEnabled && prefs.habitRemindersEnabled,
              onChanged: prefs.masterEnabled
                  ? (bool v) => _updatePreference(
                        prefs.copyWith(habitRemindersEnabled: v),
                        requestPermissionIfEnabling: v,
                      )
                  : null,
            ),
            SwitchListTile(
              key: const Key('switch_type_routine'),
              contentPadding: EdgeInsets.zero,
              title: const Text('Nhắc chuỗi thói quen (Routine)'),
              value: prefs.masterEnabled && prefs.routineRemindersEnabled,
              onChanged: prefs.masterEnabled
                  ? (bool v) => _updatePreference(
                        prefs.copyWith(routineRemindersEnabled: v),
                        requestPermissionIfEnabling: v,
                      )
                  : null,
            ),
            SwitchListTile(
              key: const Key('switch_type_calendar'),
              contentPadding: EdgeInsets.zero,
              title: const Text('Nhắc lịch hẹn & sự kiện'),
              value: prefs.masterEnabled && prefs.calendarRemindersEnabled,
              onChanged: prefs.masterEnabled
                  ? (bool v) => _updatePreference(
                        prefs.copyWith(calendarRemindersEnabled: v),
                        requestPermissionIfEnabling: v,
                      )
                  : null,
            ),
            const Divider(height: 1),
            SwitchListTile(
              key: const Key('switch_quiet_hours'),
              contentPadding: EdgeInsets.zero,
              title: const Text('Khung giờ im lặng'),
              subtitle: Text(
                quietEnabled
                    ? 'Tạm hoãn thông báo từ $quietStart đến $quietEnd'
                    : 'Chưa bật — nhắc nhở sẽ nổ ngay khi đến giờ',
              ),
              value: quietEnabled,
              onChanged: (bool enable) async {
                repo.updateUserSettings(
                  userId: userId,
                  patch: enable
                      ? const <String, dynamic>{
                          'quietStart': '22:00',
                          'quietEnd': '07:00',
                        }
                      : const <String, dynamic>{
                          'quietStart': null,
                          'quietEnd': null,
                        },
                );
                _statusMessage = null;
                await _reschedule();
              },
            ),
            if (quietEnabled)
              Row(
                children: <Widget>[
                  Expanded(
                    child: OutlinedButton.icon(
                      key: const Key('btn_quiet_start'),
                      style: OutlinedButton.styleFrom(
                        minimumSize:
                            const Size.fromHeight(LifeOsTokens.minTouchTarget),
                      ),
                      onPressed: () => _pickQuietTime(
                        isStart: true,
                        currentStart: quietStart,
                        currentEnd: quietEnd,
                      ),
                      icon: const Icon(Icons.nightlight_outlined, size: 18),
                      label: Text('Bắt đầu: $quietStart'),
                    ),
                  ),
                  const SizedBox(width: 8),
                  Expanded(
                    child: OutlinedButton.icon(
                      key: const Key('btn_quiet_end'),
                      style: OutlinedButton.styleFrom(
                        minimumSize:
                            const Size.fromHeight(LifeOsTokens.minTouchTarget),
                      ),
                      onPressed: () => _pickQuietTime(
                        isStart: false,
                        currentStart: quietStart,
                        currentEnd: quietEnd,
                      ),
                      icon: const Icon(Icons.wb_sunny_outlined, size: 18),
                      label: Text('Kết thúc: $quietEnd'),
                    ),
                  ),
                ],
              ),
            const SizedBox(height: 8),
            SwitchListTile(
              key: const Key('switch_lock_screen_details'),
              contentPadding: EdgeInsets.zero,
              title: const Text('Hiện chi tiết nội dung trên màn hình khóa'),
              subtitle: const Text(
                'Tắt để ẩn tiêu đề công việc/giao dịch khi khóa máy.',
              ),
              value: showDetailsOnLockScreen,
              onChanged: (bool value) async {
                repo.updateUserSettings(
                  userId: userId,
                  patch: <String, dynamic>{'showDetailsOnLockScreen': value},
                );
                _statusMessage = null;
                await _reschedule();
              },
            ),
            const SizedBox(height: 8),
            Container(
              padding: const EdgeInsets.all(10),
              decoration: BoxDecoration(
                color: tokens.bgElevated,
                borderRadius: BorderRadius.circular(LifeOsTokens.radiusMd),
                border: Border.all(color: tokens.borderSubtle),
              ),
              child: Text(
                fcm.statusLabelVi,
                style: TextStyle(fontSize: 12, color: tokens.textSecondary),
              ),
            ),
            const SizedBox(height: 12),
            FilledButton.icon(
              key: const Key('btn_reschedule_notifications'),
              style: FilledButton.styleFrom(
                minimumSize: const Size.fromHeight(LifeOsTokens.minTouchTarget),
              ),
              onPressed: _saving
                  ? null
                  : () => _reschedule(requestPermissionFirst: true),
              icon: _saving
                  ? const SizedBox(
                      width: 18,
                      height: 18,
                      child: CircularProgressIndicator(strokeWidth: 2),
                    )
                  : const Icon(Icons.notifications_active_outlined),
              label: const Text('Cấp quyền & Đồng bộ lịch nhắc ngay'),
            ),
          ],
        ),
      ),
      ),
    );
  }
}
