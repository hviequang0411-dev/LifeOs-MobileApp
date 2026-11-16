import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:lifeos_mobile/core/database/app_database.dart';
import 'package:lifeos_mobile/core/providers/app_providers.dart';
import 'package:lifeos_mobile/core/sync/entity_types.dart';
import 'package:lifeos_mobile/core/time/user_day_clock.dart';
import 'package:lifeos_mobile/features/notification/data/local_notification_service.dart';
import 'package:lifeos_mobile/features/workspace/data/lifeos_local_repository.dart';
import 'package:lifeos_mobile/main.dart';

class _RecordingFakeNotificationGateway implements LocalNotificationGateway {
  int initCount = 0;
  int permissionRequestCount = 0;
  bool grantPermission = true;
  final List<List<ScheduledNotificationRecord>> syncedBatches =
      <List<ScheduledNotificationRecord>>[];
  void Function(String? payload)? tapHandler;
  String? launchPayload;

  @override
  Future<void> initialize({
    required void Function(String? payload) onNotificationTap,
  }) async {
    initCount++;
    tapHandler = onNotificationTap;
  }

  @override
  Future<bool> requestPermission() async {
    permissionRequestCount++;
    return grantPermission;
  }

  @override
  Future<bool> arePermissionsGranted() async => grantPermission;

  @override
  Future<String?> getLaunchPayload() async => launchPayload;

  @override
  Future<void> syncScheduledNotifications({
    required List<ScheduledNotificationRecord> records,
    required String ianaTimezone,
  }) async {
    syncedBatches.add(List<ScheduledNotificationRecord>.from(records));
  }
}

void main() {
  group('MWP-A — Bộ lập lịch thông báo cục bộ, giới hạn 64 iOS, giờ im lặng & Cài đặt', () {
    test('Chọn tối đa 64 thông báo sắp tới gần nhất và gộp từ reminder + habit + routine', () {
      final AppDatabase db = AppDatabase.inMemory();
      addTearDown(db.close);
      final LifeOsLocalRepository repo = LifeOsLocalRepository(db: db);
      const String userId = 'u-notif-64';

      // Mốc 2026-11-11 08:00 giờ VN (01:00 UTC)
      final DateTime nowUtc = DateTime.utc(2026, 11, 11, 1, 0);
      final UserDayClock clock = UserDayClock.fromUserSettings(
        timezone: 'Asia/Ho_Chi_Minh',
        weekStartDay: 1,
      );

      // Tạo 70 reminder ở các ngày khác nhau (trộn lẫn thứ tự để kiểm tra sort)
      for (int i = 70; i >= 1; i--) {
        final DateTime date = DateTime.utc(2026, 11, 11).add(Duration(days: i));
        final String iso =
            '${date.year.toString().padLeft(4, '0')}-${date.month.toString().padLeft(2, '0')}-${date.day.toString().padLeft(2, '0')}';
        db.mutateLocalAndEnqueueOutbox(
          userId: userId,
          entityType: SyncEntityType.reminder,
          entityId: 'rem-$i',
          op: 'CREATE',
          payload: <String, dynamic>{
            'id': 'rem-$i',
            'targetType': 'TASK',
            'targetId': 'task-$i',
            'localDate': iso,
            'localTime': '09:30',
            'title': 'Việc số $i',
          },
          nowUtc: nowUtc,
        );
      }

      // Tạo thêm 1 Habit có reminderTime 11:00 và 1 Routine DAILY lúc 06:30 (ngày kế tiếp)
      db.mutateLocalAndEnqueueOutbox(
        userId: userId,
        entityType: SyncEntityType.habit,
        entityId: 'hab-water',
        op: 'CREATE',
        payload: <String, dynamic>{
          'id': 'hab-water',
          'name': 'Uống 2L nước',
          'frequencyType': 'DAILY',
          'reminderTime': '11:00',
          'archived': false,
        },
        nowUtc: nowUtc,
      );
      repo.tasks.createRoutine(
        userId: userId,
        name: 'Nghi thức sáng',
        kind: 'DAILY',
        reminderTime: '06:30',
        stepTitles: const <String>['Giãn cơ'],
        nowUtc: nowUtc,
      );

      final List<ScheduledNotificationRecord> result =
          repo.notifications.rebuildSchedule(
        userId: userId,
        dayClock: clock,
        showDetailsOnLockScreen: true,
        nowUtc: nowUtc,
      );

      // Bắt buộc <= 64 và sắp xếp tăng dần theo thời điểm nổ UTC
      expect(result.length, 64);
      for (int i = 1; i < result.length; i++) {
        expect(
          result[i - 1].fireAtUtcIso.compareTo(result[i].fireAtUtcIso) <= 0,
          isTrue,
        );
      }
      // Habit 11:00 hôm nay (gần nhất) phải nằm ở phần tử đầu tiên
      expect(result.first.targetType, 'HABIT');
      expect(result.first.body, 'Uống 2L nước');
      // Kiểm tra routine 06:30 sáng mai cũng lọt top 64
      expect(
        result.any((ScheduledNotificationRecord r) => r.targetType == 'ROUTINE'),
        isTrue,
      );
    });

    test('Áp dụng khung giờ im lặng qua đêm 22:00..07:00 và tôn trọng cờ urgent + ẩn màn hình khóa', () {
      final AppDatabase db = AppDatabase.inMemory();
      addTearDown(db.close);
      final LifeOsLocalRepository repo = LifeOsLocalRepository(db: db);
      const String userId = 'u-quiet';
      final DateTime nowUtc = DateTime.utc(2026, 11, 11, 1, 0);
      final UserDayClock clock = UserDayClock.fromUserSettings(
        timezone: 'Asia/Ho_Chi_Minh',
        weekStartDay: 1,
      );

      // 23:30 tối -> phải bị dời sang 07:00 sáng hôm sau
      db.mutateLocalAndEnqueueOutbox(
        userId: userId,
        entityType: SyncEntityType.reminder,
        entityId: 'rem-late',
        op: 'CREATE',
        payload: <String, dynamic>{
          'id': 'rem-late',
          'targetType': 'TASK',
          'targetId': 't-late',
          'localDate': '2026-11-11',
          'localTime': '23:30',
          'title': 'Việc bí mật ban đêm',
          'urgent': false,
        },
        nowUtc: nowUtc,
      );

      // 23:45 khẩn cấp -> giữ nguyên trong giờ im lặng
      db.mutateLocalAndEnqueueOutbox(
        userId: userId,
        entityType: SyncEntityType.reminder,
        entityId: 'rem-urgent',
        op: 'CREATE',
        payload: <String, dynamic>{
          'id': 'rem-urgent',
          'targetType': 'TASK',
          'targetId': 't-urgent',
          'localDate': '2026-11-11',
          'localTime': '23:45',
          'title': 'Việc khẩn cấp',
          'urgent': true,
        },
        nowUtc: nowUtc,
      );

      final List<ScheduledNotificationRecord> scheduled =
          repo.notifications.rebuildSchedule(
        userId: userId,
        dayClock: clock,
        showDetailsOnLockScreen: false,
        quietStart: '22:00',
        quietEnd: '07:00',
        nowUtc: nowUtc,
      );

      expect(scheduled.length, 2);
      // Việc khẩn cấp nổ lúc 23:45 VN (16:45 UTC) trước, rồi việc dời sáng 07:00 hôm sau (00:00 UTC)
      expect(scheduled[0].targetId, 't-urgent');
      expect(scheduled[0].fireAtUtcIso, DateTime.utc(2026, 11, 11, 16, 45).toIso8601String());
      expect(scheduled[0].redactedForLockScreen, isTrue);
      expect(scheduled[0].body, isNot(contains('Việc khẩn cấp')));

      expect(scheduled[1].targetId, 't-late');
      expect(scheduled[1].fireAtUtcIso, DateTime.utc(2026, 11, 12, 0, 0).toIso8601String());
    });

    testWidgets('Không xin quyền khi mới mở app; chỉ xin khi bật nhắc ở Cài đặt & điều hướng khi bấm thông báo', (WidgetTester tester) async {
      tester.view.physicalSize = const Size(390, 844);
      tester.view.devicePixelRatio = 1.0;
      addTearDown(tester.view.resetPhysicalSize);
      addTearDown(tester.view.resetDevicePixelRatio);

      final AppDatabase db = AppDatabase.inMemory();
      addTearDown(db.close);
      final _RecordingFakeNotificationGateway gateway =
          _RecordingFakeNotificationGateway();

      await tester.pumpWidget(
        ProviderScope(
          overrides: <Override>[
            appDatabaseProvider.overrideWithValue(db),
            notificationGatewayProvider.overrideWithValue(gateway),
            referenceUtcProvider.overrideWithValue(
              DateTime.utc(2026, 11, 11, 2, 0),
            ),
          ],
          child: const LifeOsApp(),
        ),
      );
      await tester.pumpAndSettle();

      // Khi mới mở app, KHÔNG tự bật popup xin quyền thông báo
      expect(gateway.initCount, 1);
      expect(gateway.permissionRequestCount, 0);

      // Vào Tổng quan -> mở Thông báo & Giờ im lặng
      await tester.tap(find.text('Tổng quan'));
      await tester.pumpAndSettle();
      await tester.scrollUntilVisible(
        find.byKey(const Key('tile_notification_settings')),
        200,
        scrollable: find.byType(Scrollable).first,
      );
      await tester.drag(find.byType(Scrollable).first, const Offset(0, -100));
      await tester.pumpAndSettle();
      await tester.tap(find.byKey(const Key('tile_notification_settings')));
      await tester.pumpAndSettle();

      expect(find.byKey(const Key('switch_master_notifications')), findsOneWidget);
      expect(gateway.permissionRequestCount, 0);

      // Tắt rồi bật lại công tắc thông báo -> xin quyền đúng lúc bật
      await tester.tap(find.byKey(const Key('switch_master_notifications')));
      await tester.pumpAndSettle();
      expect(gateway.permissionRequestCount, 0);

      await tester.tap(find.byKey(const Key('switch_master_notifications')));
      await tester.pumpAndSettle();
      expect(gateway.permissionRequestCount, 1);

      // Bật khung giờ im lặng -> ghi quietStart/quietEnd hợp lệ vào user_settings + Outbox
      final Finder quietSwitch = find.byKey(const Key('switch_quiet_hours'));
      await tester.ensureVisible(quietSwitch);
      await tester.tap(quietSwitch);
      await tester.pumpAndSettle();

      final LifeOsLocalRepository repo = LifeOsLocalRepository(db: db);
      final Map<String, dynamic> settings = repo.getUserSettings('user-local-01');
      expect(settings['quietStart'], '22:00');
      expect(settings['quietEnd'], '07:00');

      // Đóng sheet cài đặt
      Navigator.of(tester.element(find.byKey(const Key('switch_master_notifications')))).pop();
      await tester.pumpAndSettle();

      // Giả lập bấm vào thông báo FINANCE -> tự chuyển sang Tab Tài chính
      gateway.tapHandler?.call('{"targetType":"FINANCE","targetId":"tx-1"}');
      await tester.pumpAndSettle();
      expect(find.text('Tổng tài sản khả dụng'), findsOneWidget);
    });
  });
}

