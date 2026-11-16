import 'package:flutter_test/flutter_test.dart';
import 'package:lifeos_mobile/core/calculators/analytics_calculator.dart';
import 'package:lifeos_mobile/core/database/app_database.dart';
import 'package:lifeos_mobile/core/money/decimal10k.dart';
import 'package:lifeos_mobile/core/sync/entity_types.dart';
import 'package:lifeos_mobile/core/time/user_day_clock.dart';
import 'package:lifeos_mobile/features/workspace/data/lifeos_local_repository.dart';

void main() {
  group('PWP5..PWP12 — Kiểm thử Nghiệp vụ Offline 12 Module & Ràng buộc Hợp đồng', () {
    late AppDatabase db;
    late LifeOsLocalRepository repo;
    const String userId = 'u-domain-01';

    setUp(() {
      db = AppDatabase.inMemory();
      repo = LifeOsLocalRepository(db: db);
    });

    tearDown(() {
      db.close();
    });

    test('Decimal10k: tính toán số nguyên × 10⁴, làm tròn HALF_UP và xuất chuỗi JSON chuẩn', () {
      final Decimal10k a = Decimal10k.parseJsonString('125000');
      final Decimal10k b = Decimal10k.parseJsonString('10.5000');
      expect(a.scaledValue, 1250000000);
      expect(b.toJsonString(), '10.5');
      expect(a.formatDisplay(), '125.000 ₫');
      expect(a.formatDisplay(masked: true), '•••••• ₫');

      final Decimal10k usd = Decimal10k.parseJsonString('100');
      final Decimal10k rate = Decimal10k.parseJsonString('25450.5');
      expect(usd.multiplyRate(rate).toJsonString(), '2545050');
    });

    test('UserDayClock (D192): ngày = [00:00, 24:00) theo timezone và tuần bắt đầu theo weekStartDay (1..7)', () {
      final UserDayClock clock = UserDayClock.fromUserSettings(
        timezone: 'Asia/Ho_Chi_Minh',
        weekStartDay: 1, // Thứ Hai
      );
      // 2026-11-07T16:59:59Z = 2026-11-07 23:59:59 giờ Việt Nam -> 2026-11-07
      expect(
        clock.businessDateForUtc(DateTime.parse('2026-11-07T16:59:59Z')),
        '2026-11-07',
      );
      // 2026-11-07T17:00:00Z = 2026-11-08 00:00:00 giờ Việt Nam -> 2026-11-08
      expect(
        clock.businessDateForUtc(DateTime.parse('2026-11-07T17:00:00Z')),
        '2026-11-08',
      );
      // 2026-11-08 là Chủ Nhật -> đầu tuần (Thứ Hai) là 2026-11-02
      expect(clock.startOfWeekForDate('2026-11-08'), '2026-11-02');
    });

    test('PWP8 Finance (D102, D104, D112, D118): Tài khoản, Giao dịch, account_transfer & Đối soát số dư ADJUSTMENT', () {
      final String cashId = repo.createFinanceAccount(
        userId: userId,
        name: 'Tiền mặt',
        accountType: 'CASH',
        currency: 'VND',
        openingBalance: Decimal10k.fromWhole(1000000),
      );
      final String bankId = repo.createFinanceAccount(
        userId: userId,
        name: 'Ngân hàng VCB',
        accountType: 'BANK',
        currency: 'VND',
        openingBalance: Decimal10k.fromWhole(5000000),
      );
      final String usdId = repo.createFinanceAccount(
        userId: userId,
        name: 'Tài khoản USD',
        accountType: 'BANK',
        currency: 'USD',
        openingBalance: Decimal10k.fromWhole(200),
      );

      // Ghi khoản chi 150.000 ₫ từ ví Tiền mặt
      repo.createFinanceTransaction(
        userId: userId,
        accountId: cashId,
        kind: 'EXPENSE',
        amount: Decimal10k.fromWhole(150000),
        occurredOnIso: '2026-11-08',
      );
      expect(
        repo.computeAccountBalance(userId: userId, accountId: cashId),
        Decimal10k.fromWhole(850000),
      );

      // Chuyển khoản 500.000 ₫ từ Ngân hàng sang Tiền mặt qua thực thể account_transfer (D102)
      final String transferId = repo.createAccountTransfer(
        userId: userId,
        fromAccountId: bankId,
        toAccountId: cashId,
        amount: Decimal10k.fromWhole(500000),
        occurredOnIso: '2026-11-08',
      );
      expect(transferId, isNotEmpty);
      expect(
        db.getEntity(
          userId: userId,
          entityType: SyncEntityType.accountTransfer,
          entityId: transferId,
        ),
        isNotNull,
      );
      expect(
        repo.computeAccountBalance(userId: userId, accountId: bankId),
        Decimal10k.fromWhole(4500000),
      );
      expect(
        repo.computeAccountBalance(userId: userId, accountId: cashId),
        Decimal10k.fromWhole(1350000),
      );

      // Chặn chuyển khoản khác tiền tệ (D104, D118 -> FINANCE_CURRENCY_MISMATCH)
      expect(
        () => repo.createAccountTransfer(
          userId: userId,
          fromAccountId: bankId,
          toAccountId: usdId,
          amount: Decimal10k.fromWhole(100),
          occurredOnIso: '2026-11-08',
        ),
        throwsStateError,
      );

      // Đối soát số dư thực tế (Reconcile D112 & D119) ví Tiền mặt về 1.400.000 ₫
      final String? adjId = repo.reconcileAccountBalance(
        userId: userId,
        accountId: cashId,
        actualBalance: Decimal10k.fromWhole(1400000),
        occurredOnIso: '2026-11-08',
      );
      expect(adjId, isNotNull);
      expect(
        repo.computeAccountBalance(userId: userId, accountId: cashId),
        Decimal10k.fromWhole(1400000),
      );

      // Đánh giá tốc độ chi tiêu ngân sách (Budget Pace)
      final ({int consumedBasisPoints, BudgetPaceStatus status}) pace =
          AnalyticsCalculator.evaluateBudgetPace(
        spentAmount: Decimal10k.fromWhole(850000),
        limitAmount: Decimal10k.fromWhole(1000000),
        alertThresholdPercent: 80,
      );
      expect(pace.consumedBasisPoints, 8500);
      expect(pace.status, BudgetPaceStatus.warning);
    });

    test('PWP9 Calendar (D126, D127, UC-CAL-03): recurrence JSON, event_exception & Tách chuỗi ("Từ lần này trở đi")', () {
      final String origId = repo.createCalendarEvent(
        userId: userId,
        title: 'Họp đứng mỗi ngày',
        startLocal: '2026-04-01T09:00:00',
        endLocal: '2026-04-01T09:30:00',
        recurrence: const <String, dynamic>{
          'freq': 'DAILY',
          'interval': 1,
          'count': 10,
        },
      );

      final String excId = repo.recordEventOccurrenceException(
        userId: userId,
        eventId: origId,
        originalStartLocal: '2026-04-02T09:00:00',
        kind: 'CANCELLED',
      );
      expect(excId, isNotEmpty);

      final String newSeriesId = repo.splitRecurringEventFrom(
        userId: userId,
        originalEventId: origId,
        originalStartLocal: '2026-04-04T09:00:00',
        newStartLocal: '2026-04-04T10:00:00',
        newEndLocal: '2026-04-04T10:30:00',
        newTitle: 'Họp đứng khung giờ mới',
      );

      final LocalEntityRow? updatedOrig = db.getEntity(
        userId: userId,
        entityType: SyncEntityType.calendarEvent,
        entityId: origId,
      );
      final Map<String, dynamic> updatedRec =
          (updatedOrig!.payload['recurrence'] as Map<dynamic, dynamic>)
              .cast<String, dynamic>();
      expect(updatedRec['until'], '2026-04-03');
      expect(updatedRec['count'], isNull);
      expect(newSeriesId, isNotEmpty);
    });

    test('PWP10 Knowledge (D180, D189): Ghi chú và liên kết một chiều note_link tới TASK hoặc GOAL', () {
      final String taskId = repo.createTask(
        userId: userId,
        title: 'Hoàn thành thiết kế module Note',
      );
      final String goalId = repo.createGoal(
        userId: userId,
        title: 'Mục tiêu Quý 4',
        startDateIso: '2026-10-01',
        targetDateIso: '2026-12-31',
      );
      final String noteId = repo.saveNote(
        userId: userId,
        title: 'Biên bản Thiết kế Đồng bộ',
        content: 'Ghi chú liên kết tới Task và Goal theo đúng D189.',
      );

      final String linkTask = repo.linkNoteToTarget(
        userId: userId,
        noteId: noteId,
        targetType: 'TASK',
        targetId: taskId,
      );
      final String linkGoal = repo.linkNoteToTarget(
        userId: userId,
        noteId: noteId,
        targetType: 'GOAL',
        targetId: goalId,
      );
      expect(linkTask, isNotEmpty);
      expect(linkGoal, isNotEmpty);

      final List<LocalEntityRow> links = repo.listLinksForNote(
        userId: userId,
        noteId: noteId,
      );
      expect(links.length, 2);

      // Chặn targetType ngoài TASK/GOAL
      expect(
        () => repo.linkNoteToTarget(
          userId: userId,
          noteId: noteId,
          targetType: 'NOTE',
          targetId: noteId,
        ),
        throwsArgumentError,
      );
    });

    test('PWP9 NotificationScheduler (D132, D133, D138): giới hạn 64 lịch nhắc iOS, khử trùng dedupeKey & che màn hình khóa', () {
      for (int i = 0; i < 80; i++) {
        repo.createReminder(
          userId: userId,
          targetType: 'STANDALONE',
          mode: 'ABSOLUTE',
          localDate: '2026-12-01',
          localTime: '09:${(i % 60).toString().padLeft(2, '0')}',
          title: 'Nhắc việc #$i',
        );
      }

      final List<ScheduledNotificationRecord> scheduled =
          repo.notifications.rebuildSchedule(
        userId: userId,
        dayClock: repo.getUserDayClock(userId),
        showDetailsOnLockScreen: false,
        nowUtc: DateTime.parse('2026-11-08T00:00:00Z'),
      );
      expect(scheduled.length, 64);

      // Khử trùng dedupeKey giữa Local Notification và Push (D132)
      final bool firstShow = repo.notifications.shouldDisplayAndMarkDelivered(
        userId: userId,
        dedupeKey: 'rem:test-1:2026-11-08:09:00',
        nowUtc: DateTime.parse('2026-11-08T02:00:00Z'),
      );
      final bool duplicatePushShow =
          repo.notifications.shouldDisplayAndMarkDelivered(
        userId: userId,
        dedupeKey: 'rem:test-1:2026-11-08:09:00',
        nowUtc: DateTime.parse('2026-11-08T02:01:00Z'),
      );
      expect(firstShow, isTrue);
      expect(duplicatePushShow, isFalse);

      // Che nội dung nhạy cảm khi showDetailsOnLockScreen == false (D138)
      final String redacted = repo.formatLockScreenSafeNotificationBody(
        userId: userId,
        moduleType: 'FINANCE',
        rawTitle: 'Thanh toán thẻ tín dụng 25.000.000 ₫',
      );
      expect(redacted.contains('25.000.000'), isFalse);
    });
  });

  group('PWP15 — Kiểm thử Hiệu năng (50.000 bản ghi đồng bộ lần đầu & Truy vấn My Day trên 10.000 mục)', () {
    test('Nạp 50.000 bản ghi vào Drift/SQLite và truy vấn danh sách 10.000 mục trong ngưỡng hiệu năng', () {
      final AppDatabase db = AppDatabase.inMemory();
      final LifeOsLocalRepository repo = LifeOsLocalRepository(db: db);
      const String userId = 'u-perf';

      final List<Map<String, dynamic>> batch50k =
          List<Map<String, dynamic>>.generate(50000, (int i) {
        final bool isTask = i < 10000;
        return <String, dynamic>{
          'entityType': isTask ? 'task' : 'note',
          'entityId': 'perf-$i',
          'version': 1,
          'updatedAt': '2026-11-08T00:00:00Z',
          'data': isTask
              ? <String, dynamic>{
                  'id': 'perf-$i',
                  'title': 'Công việc hiệu năng #$i',
                  'priority': i % 4 == 0 ? 'HIGH' : 'MEDIUM',
                  'status': 'TODO',
                  'isMyDay': i < 200,
                  'dueDate': i < 200 ? '2026-11-08' : '2026-12-01',
                }
              : <String, dynamic>{
                  'id': 'perf-$i',
                  'title': 'Ghi chú #$i',
                  'content': 'Nội dung ngắn #$i',
                },
        };
      });

      final Stopwatch ingestWatch = Stopwatch()..start();
      db.applyPulledChangesBatch(
        userId: userId,
        changes: batch50k,
        nextCursor: 50000,
        syncedAtIso: '2026-11-08T00:00:00Z',
      );
      ingestWatch.stop();

      // Ngưỡng 60s cho 50.000 bản ghi trên môi trường test debug / máy ảo CI
      expect(ingestWatch.elapsedMilliseconds, lessThan(60000));

      final Stopwatch queryWatch = Stopwatch()..start();
      final List<LocalEntityRow> myDay = repo.queryMyDayTasks(
        userId: userId,
        todayIso: '2026-11-08',
      );
      queryWatch.stop();

      expect(myDay.length, 200);
      expect(queryWatch.elapsedMilliseconds, lessThan(1000));
      db.close();
    });
  });
}
