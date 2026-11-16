import 'dart:convert';

import 'package:flutter/foundation.dart';
import 'package:flutter/services.dart';
import 'package:flutter_local_notifications/flutter_local_notifications.dart';
import 'package:timezone/data/latest_all.dart' as tzdata;
import 'package:timezone/timezone.dart' as tz;

import '../../../core/database/app_database.dart';
import '../../../core/time/user_day_clock.dart';
import '../domain/local_notification_scheduler.dart';

/// Mục tiêu điều hướng khi người dùng bấm vào thông báo cục bộ hoặc Push.
@immutable
class NotificationRouteTarget {
  const NotificationRouteTarget({
    required this.targetType,
    required this.targetId,
    required this.tabIndex,
    this.dedupeKey,
  });

  final String targetType;
  final String targetId;
  final int tabIndex;
  final String? dedupeKey;

  static NotificationRouteTarget? fromPayload(String? rawPayload) {
    if (rawPayload == null || rawPayload.trim().isEmpty) return null;
    try {
      final Map<String, dynamic> map =
          (jsonDecode(rawPayload) as Map<dynamic, dynamic>)
              .cast<String, dynamic>();
      final String type =
          (map['targetType'] as String? ?? 'TASK').toUpperCase();
      final String id = (map['targetId'] as String? ?? '').trim();
      final int tabIndex = switch (type) {
        'HABIT' || 'TASK' => 0,
        'ROUTINE' || 'EVENT' || 'CALENDAR' => 1,
        'FINANCE' || 'SAVING_GOAL' => 2,
        'GOAL' || 'NOTE' => 3,
        _ => 0,
      };
      return NotificationRouteTarget(
        targetType: type,
        targetId: id,
        tabIndex: tabIndex,
        dedupeKey: map['dedupeKey'] as String?,
      );
    } catch (_) {
      return null;
    }
  }

  String toPayloadJson() => jsonEncode(<String, dynamic>{
        'targetType': targetType,
        'targetId': targetId,
        if (dedupeKey != null) 'dedupeKey': dedupeKey,
      });
}

/// Giao diện trừu tượng hóa kênh thông báo hệ điều hành (cho phép test không cần MethodChannel thật).
abstract class LocalNotificationGateway {
  Future<void> initialize({
    required void Function(String? payload) onNotificationTap,
  });

  Future<bool> requestPermission();

  Future<bool> arePermissionsGranted();

  Future<void> syncScheduledNotifications({
    required List<ScheduledNotificationRecord> records,
    required String ianaTimezone,
  });

  Future<String?> getLaunchPayload();
}

/// Kênh thông báo Android khai báo bằng tiếng Việt rõ nghĩa.
class LifeOsNotificationChannels {
  LifeOsNotificationChannels._();

  static const String tasksId = 'lifeos_tasks';
  static const String habitsId = 'lifeos_habits';
  static const String routinesId = 'lifeos_routines';
  static const String calendarId = 'lifeos_calendar';

  static const List<AndroidNotificationChannel> all =
      <AndroidNotificationChannel>[
    AndroidNotificationChannel(
      tasksId,
      'Nhắc việc',
      description: 'Nhắc nhở công việc đến hạn và ưu tiên trong ngày',
      importance: Importance.high,
    ),
    AndroidNotificationChannel(
      habitsId,
      'Thói quen hằng ngày',
      description: 'Nhắc giờ thực hiện thói quen của bạn',
      importance: Importance.defaultImportance,
    ),
    AndroidNotificationChannel(
      routinesId,
      'Chuỗi thói quen (Routine)',
      description: 'Nhắc giờ bắt đầu chuỗi thói quen buổi sáng hoặc tối',
      importance: Importance.high,
    ),
    AndroidNotificationChannel(
      calendarId,
      'Lịch & Sự kiện',
      description: 'Nhắc nhở sự kiện lịch và khối tập trung',
      importance: Importance.high,
    ),
  ];

  static String channelIdForType(String targetType) {
    return switch (targetType.toUpperCase()) {
      'HABIT' => habitsId,
      'ROUTINE' => routinesId,
      'EVENT' || 'CALENDAR' => calendarId,
      _ => tasksId,
    };
  }

  static String channelNameForId(String id) {
    return switch (id) {
      habitsId => 'Thói quen hằng ngày',
      routinesId => 'Chuỗi thói quen (Routine)',
      calendarId => 'Lịch & Sự kiện',
      _ => 'Nhắc việc',
    };
  }
}

/// Cài đặt thật qua `flutter_local_notifications` + `timezone`.
class FlutterLocalNotificationsGateway implements LocalNotificationGateway {
  FlutterLocalNotificationsGateway({FlutterLocalNotificationsPlugin? plugin})
      : _plugin = plugin ?? FlutterLocalNotificationsPlugin();

  final FlutterLocalNotificationsPlugin _plugin;
  bool _initialized = false;
  static bool _tzInitialized = false;

  void _ensureTimeZonesInitialized() {
    if (_tzInitialized) return;
    tzdata.initializeTimeZones();
    _tzInitialized = true;
  }

  tz.Location _resolveLocation(String ianaTimezone) {
    _ensureTimeZonesInitialized();
    try {
      return tz.getLocation(ianaTimezone);
    } catch (_) {
      return tz.getLocation('Asia/Ho_Chi_Minh');
    }
  }

  @override
  Future<void> initialize({
    required void Function(String? payload) onNotificationTap,
  }) async {
    if (kIsWeb || _initialized) return;
    _ensureTimeZonesInitialized();
    try {
      const AndroidInitializationSettings androidSettings =
          AndroidInitializationSettings('@mipmap/ic_launcher');
      // Tuyệt đối KHÔNG xin quyền iOS ngay khi khởi động app (chỉ xin khi người dùng bật nhắc).
      const DarwinInitializationSettings darwinSettings =
          DarwinInitializationSettings(
        requestAlertPermission: false,
        requestBadgePermission: false,
        requestSoundPermission: false,
      );
      const InitializationSettings settings = InitializationSettings(
        android: androidSettings,
        iOS: darwinSettings,
      );

      await _plugin.initialize(
        settings: settings,
        onDidReceiveNotificationResponse: (NotificationResponse response) {
          onNotificationTap(response.payload);
        },
      );

      if (defaultTargetPlatform == TargetPlatform.android) {
        final AndroidFlutterLocalNotificationsPlugin? androidPlugin =
            _plugin.resolvePlatformSpecificImplementation<
                AndroidFlutterLocalNotificationsPlugin>();
        if (androidPlugin != null) {
          for (final AndroidNotificationChannel ch
              in LifeOsNotificationChannels.all) {
            await androidPlugin.createNotificationChannel(ch);
          }
        }
      }
      _initialized = true;
    } on MissingPluginException {
      // Môi trường unit/widget test không có MethodChannel native.
    } catch (_) {
      // Degrade êm nếu plugin chưa sẵn sàng trên nền tảng desktop/test.
    }
  }

  @override
  Future<bool> requestPermission() async {
    if (kIsWeb) return false;
    try {
      if (defaultTargetPlatform == TargetPlatform.android) {
        final AndroidFlutterLocalNotificationsPlugin? androidPlugin =
            _plugin.resolvePlatformSpecificImplementation<
                AndroidFlutterLocalNotificationsPlugin>();
        return await androidPlugin?.requestNotificationsPermission() ?? true;
      }
      if (defaultTargetPlatform == TargetPlatform.iOS) {
        final IOSFlutterLocalNotificationsPlugin? iosPlugin =
            _plugin.resolvePlatformSpecificImplementation<
                IOSFlutterLocalNotificationsPlugin>();
        return await iosPlugin?.requestPermissions(
              alert: true,
              badge: true,
              sound: true,
            ) ??
            true;
      }
      return true;
    } on MissingPluginException {
      return true;
    } catch (_) {
      return false;
    }
  }

  @override
  Future<bool> arePermissionsGranted() async {
    if (kIsWeb) return false;
    try {
      if (defaultTargetPlatform == TargetPlatform.android) {
        final AndroidFlutterLocalNotificationsPlugin? androidPlugin =
            _plugin.resolvePlatformSpecificImplementation<
                AndroidFlutterLocalNotificationsPlugin>();
        return await androidPlugin?.areNotificationsEnabled() ?? true;
      }
      return true;
    } on MissingPluginException {
      return true;
    } catch (_) {
      return true;
    }
  }

  @override
  Future<String?> getLaunchPayload() async {
    if (kIsWeb) return null;
    try {
      final NotificationAppLaunchDetails? details =
          await _plugin.getNotificationAppLaunchDetails();
      if (details != null && details.didNotificationLaunchApp) {
        return details.notificationResponse?.payload;
      }
      return null;
    } on MissingPluginException {
      return null;
    } catch (_) {
      return null;
    }
  }

  @override
  Future<void> syncScheduledNotifications({
    required List<ScheduledNotificationRecord> records,
    required String ianaTimezone,
  }) async {
    if (kIsWeb) return;
    try {
      await _plugin.cancelAllPendingNotifications();
      final tz.Location location = _resolveLocation(ianaTimezone);
      final DateTime nowUtc = DateTime.now().toUtc();

      for (final ScheduledNotificationRecord record in records) {
        final DateTime? fireUtc = DateTime.tryParse(record.fireAtUtcIso);
        if (fireUtc == null || !fireUtc.isAfter(nowUtc)) continue;

        final tz.TZDateTime scheduledTz = tz.TZDateTime.from(fireUtc, location);
        final String channelId =
            LifeOsNotificationChannels.channelIdForType(record.targetType);
        final String channelName =
            LifeOsNotificationChannels.channelNameForId(channelId);
        final int stableId = _stableNotificationId(record.dedupeKey);
        final String payloadJson = NotificationRouteTarget(
          targetType: record.targetType,
          targetId: record.targetId,
          tabIndex: 0,
          dedupeKey: record.dedupeKey,
        ).toPayloadJson();

        // Mặc định dùng inexact alarm để tránh đòi quyền SCHEDULE_EXACT_ALARM trên Android 14+.
        await _plugin.zonedSchedule(
          id: stableId,
          scheduledDate: scheduledTz,
          notificationDetails: NotificationDetails(
            android: AndroidNotificationDetails(
              channelId,
              channelName,
              importance: Importance.high,
              priority: Priority.high,
            ),
            iOS: const DarwinNotificationDetails(
              presentAlert: true,
              presentBadge: true,
              presentSound: true,
            ),
          ),
          androidScheduleMode: AndroidScheduleMode.inexactAllowWhileIdle,
          title: record.title,
          body: record.body,
          payload: payloadJson,
        );
      }
    } on MissingPluginException {
      // Bỏ qua trong môi trường test không có native channel.
    } catch (_) {
      // Giữ ứng dụng ổn định kể cả khi plugin nền tảng trả về ngoại lệ.
    }
  }

  static int _stableNotificationId(String key) {
    int hash = 2166136261;
    for (final int unit in key.codeUnits) {
      hash ^= unit;
      hash = (hash * 16777619) & 0x7fffffff;
    }
    return hash == 0 ? 1 : hash;
  }
}

/// Điều phối viên lập lịch nhắc nhở từ SQLite ra plugin hệ điều hành + điều hướng khi bấm thông báo.
class LocalNotificationCoordinator {
  LocalNotificationCoordinator({
    required this.db,
    required this.scheduler,
    required this.gateway,
    required this.onRouteRequested,
  });

  final AppDatabase db;
  final LocalNotificationScheduler scheduler;
  final LocalNotificationGateway gateway;
  final void Function(NotificationRouteTarget target) onRouteRequested;

  bool _initialized = false;

  Future<void> initializeOnAppStart() async {
    if (_initialized) return;
    _initialized = true;
    await gateway.initialize(
      onNotificationTap: (String? payload) {
        final NotificationRouteTarget? target =
            NotificationRouteTarget.fromPayload(payload);
        if (target != null) {
          onRouteRequested(target);
        }
      },
    );
    final String? launchPayload = await gateway.getLaunchPayload();
    final NotificationRouteTarget? launchTarget =
        NotificationRouteTarget.fromPayload(launchPayload);
    if (launchTarget != null) {
      onRouteRequested(launchTarget);
    }
  }

  /// Xin quyền gửi thông báo ĐÚNG LÚC người dùng bật nhắc lần đầu (không gọi khi mở app).
  Future<bool> requestPermissionOnUserEnable() async {
    await initializeOnAppStart();
    return gateway.requestPermission();
  }

  /// Tổng hợp lại tối đa 64 thông báo sắp tới từ SQLite và đẩy xuống Android/iOS.
  Future<List<ScheduledNotificationRecord>> rescheduleFromDatabase({
    required String userId,
    required Map<String, dynamic> userSettings,
    required NotificationPreferences preferences,
    DateTime? nowUtc,
  }) async {
    await initializeOnAppStart();
    final String tzName =
        (userSettings['timezone'] as String?) ?? 'Asia/Ho_Chi_Minh';
    final int weekStartDay =
        (userSettings['weekStartDay'] as num?)?.toInt() ?? 1;
    final bool showDetailsOnLockScreen =
        userSettings['showDetailsOnLockScreen'] == true;
    final String? quietStart = userSettings['quietStart'] as String?;
    final String? quietEnd = userSettings['quietEnd'] as String?;

    final UserDayClock dayClock = UserDayClock.fromUserSettings(
      timezone: tzName,
      weekStartDay: weekStartDay,
    );

    final List<ScheduledNotificationRecord> capped = scheduler.rebuildSchedule(
      userId: userId,
      dayClock: dayClock,
      showDetailsOnLockScreen: showDetailsOnLockScreen,
      quietStart: quietStart,
      quietEnd: quietEnd,
      preferences: preferences,
      nowUtc: nowUtc,
    );

    await gateway.syncScheduledNotifications(
      records: capped,
      ianaTimezone: tzName,
    );
    return capped;
  }
}

/// Giao diện tích hợp FCM Push sau cờ cấu hình (`enableFcm`, mặc định `false`).
/// Khi chưa có `google-services.json`/`GoogleService-Info.plist` hoặc cờ tắt, degrade êm về no-op.
class FcmPushCoordinator {
  const FcmPushCoordinator({required this.enabled});

  final bool enabled;

  bool get isReady => false;

  String get statusLabelVi => enabled
      ? 'FCM đã bật cờ nhưng đang chờ tệp cấu hình Firebase (google-services.json / GoogleService-Info.plist).'
      : 'Thông báo đẩy từ máy chủ (FCM) đang tắt theo cấu hình mặc định; dùng lịch nhắc cục bộ trên thiết bị.';

  Future<String?> registerTokenIfAvailable() async {
    if (!enabled) return null;
    // Chỉ kích hoạt native FCM khi có cấu hình Firebase thật để tránh lỗi build/runtime thiếu google-services.json.
    return null;
  }
}

