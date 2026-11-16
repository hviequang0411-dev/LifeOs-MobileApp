import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:lifeos_mobile/core/database/app_database.dart';
import 'package:lifeos_mobile/core/money/decimal10k.dart';
import 'package:lifeos_mobile/core/providers/app_providers.dart';
import 'package:lifeos_mobile/core/sync/entity_types.dart';
import 'package:lifeos_mobile/features/workspace/data/lifeos_local_repository.dart';
import 'package:lifeos_mobile/main.dart';

void main() {
  group('MWP-C & MWP-D — Routine Player, Recurring Tx, Saving Goal, Milestones & Polish', () {
    test('Xem trước 3 kỳ giao dịch định kỳ khớp Recurrence.java (kẹp cuối tháng / năm nhuận)', () {
      expect(
        FinanceLocalRepository.computeRecurringOccurrencesPreview(
          frequency: 'MONTHLY',
          intervalCount: 1,
          startOnIso: '2026-01-31',
        ),
        <String>['2026-01-31', '2026-02-28', '2026-03-31'],
      );

      expect(
        FinanceLocalRepository.computeRecurringOccurrencesPreview(
          frequency: 'YEARLY',
          intervalCount: 1,
          startOnIso: '2024-02-29',
        ),
        <String>['2024-02-29', '2025-02-28', '2026-02-28'],
      );

      expect(
        FinanceLocalRepository.computeRecurringOccurrencesPreview(
          frequency: 'WEEKLY',
          intervalCount: 2,
          startOnIso: '2026-11-01',
          endOnIso: '2026-11-20',
        ),
        <String>['2026-11-01', '2026-11-15'],
      );
    });

    test('RoutineLog và RoutineStepLog idempotent khi mở lại cùng ngày & SavingGoal tính bằng Decimal10k', () {
      final AppDatabase db = AppDatabase.inMemory();
      addTearDown(db.close);
      final LifeOsLocalRepository repo = LifeOsLocalRepository(db: db);
      const String userId = 'u-forms';

      final String rId = repo.tasks.createRoutine(
        userId: userId,
        name: 'Sáng tập trung',
        stepTitles: const <String>['Uống nước', 'Đọc sách'],
        stepDurationsMinutes: const <int>[2, 10],
      );
      final List<LocalEntityRow> steps =
          repo.tasks.listRoutineSteps(userId: userId, routineId: rId);
      expect(steps.length, 2);
      expect(steps[1].payload['durationMinutes'], 10);

      final String logId1 = repo.tasks.startOrGetRoutineLogForDate(
        userId: userId,
        routineId: rId,
        localDateIso: '2026-11-11',
        totalSteps: 2,
      );
      final String logId2 = repo.tasks.startOrGetRoutineLogForDate(
        userId: userId,
        routineId: rId,
        localDateIso: '2026-11-11',
        totalSteps: 2,
      );
      expect(logId2, logId1);

      final String stepLogId1 = repo.tasks.recordRoutineStepLog(
        userId: userId,
        routineLogId: logId1,
        stepId: steps[0].id,
        status: 'DONE',
      );
      final String stepLogId2 = repo.tasks.recordRoutineStepLog(
        userId: userId,
        routineLogId: logId1,
        stepId: steps[0].id,
        status: 'SKIPPED',
      );
      expect(stepLogId2, stepLogId1);
      expect(
        repo.tasks.getRoutineStepStatuses(userId: userId, routineLogId: logId1)[steps[0].id],
        'SKIPPED',
      );

      final String sgId = repo.finance.saveSavingGoal(
        userId: userId,
        name: 'Quỹ dự phòng',
        currency: 'VND',
        targetAmount: Decimal10k.fromWhole(10000000),
      );
      repo.finance.addSavingContribution(
        userId: userId,
        goalId: sgId,
        amount: Decimal10k.fromWhole(2500000),
        occurredOnIso: '2026-11-11',
      );
      repo.finance.addSavingContribution(
        userId: userId,
        goalId: sgId,
        amount: Decimal10k.fromWhole(1500000),
        occurredOnIso: '2026-11-11',
      );
      expect(
        repo.finance.computeSavingGoalSavedAmount(userId: userId, goalId: sgId),
        Decimal10k.fromWhole(4000000),
      );
    });

    testWidgets('Luồng UI đầy đủ: Routine Player, Recurring Tx, Saving Goal, Milestones & FAB ẩn khi cuộn', (WidgetTester tester) async {
      tester.view.physicalSize = const Size(390, 844);
      tester.view.devicePixelRatio = 1.0;
      addTearDown(tester.view.resetPhysicalSize);
      addTearDown(tester.view.resetDevicePixelRatio);

      final AppDatabase db = AppDatabase.inMemory();
      addTearDown(db.close);
      final LifeOsLocalRepository repo = LifeOsLocalRepository(db: db);
      const String userId = 'user-local-01';

      repo.createFinanceAccount(
        userId: userId,
        name: 'Ví chính',
        openingBalance: Decimal10k.fromWhole(5000000),
      );
      final String routineId = repo.tasks.createRoutine(
        userId: userId,
        name: 'Nghi thức buổi sáng',
        stepTitles: const <String>['Uống nước', 'Lên kế hoạch'],
        stepDurationsMinutes: const <int>[2, 5],
      );
      final String goalId = repo.createGoal(
        userId: userId,
        title: 'Học Tiếng Nhật N2',
        startDateIso: '2026-01-01',
        targetDateIso: '2026-12-31',
      );

      // Tạo thêm nhiều task để Tab Hôm nay cuộn được xuống & kiểm tra FAB tự ẩn
      for (int i = 0; i < 12; i++) {
        repo.createTask(
          userId: userId,
          title: 'Tác vụ kiểm thử cuộn #$i',
          isMyDay: true,
        );
      }

      await tester.pumpWidget(
        ProviderScope(
          overrides: <Override>[
            appDatabaseProvider.overrideWithValue(db),
            referenceUtcProvider.overrideWithValue(
              DateTime.utc(2026, 11, 11, 2, 0),
            ),
          ],
          child: const LifeOsApp(),
        ),
      );
      await tester.pumpAndSettle();

      // MWP-D #2: Lời chào dùng fallback "bạn" thay vì "LifeOS"
      expect(find.text('Chào ngày mới, bạn'), findsOneWidget);

      // MWP-D #1: Cuộn xuống -> FAB mờ đi (opacity = 0), cuộn lên -> hiện lại
      await tester.drag(find.byType(ListView).first, const Offset(0, -400));
      await tester.pumpAndSettle();
      AnimatedOpacity fabOpacity =
          tester.widget(find.byType(AnimatedOpacity));
      expect(fabOpacity.opacity, 0.0);

      await tester.drag(find.byType(ListView).first, const Offset(0, 400));
      await tester.pumpAndSettle();
      fabOpacity = tester.widget(find.byType(AnimatedOpacity));
      expect(fabOpacity.opacity, 1.0);

      // MWP-C #1: Tab Kế hoạch -> Chạy Routine Player
      await tester.tap(find.text('Kế hoạch'));
      await tester.pumpAndSettle();
      final Finder runRoutineBtn =
          find.byKey(Key('btn_run_routine_$routineId'));
      await tester.scrollUntilVisible(
        runRoutineBtn,
        250,
        scrollable: find.byType(Scrollable).first,
      );
      await tester.tap(runRoutineBtn);
      await tester.pumpAndSettle();
      expect(find.text('02:00'), findsOneWidget);

      await tester.tap(find.byKey(const Key('btn_routine_complete_step')));
      await tester.pumpAndSettle();
      expect(find.text('05:00'), findsOneWidget);

      await tester.tap(find.byKey(const Key('btn_routine_skip_step')));
      await tester.pumpAndSettle();
      expect(
        find.textContaining('hoàn tất toàn bộ chuỗi thói quen'),
        findsOneWidget,
      );
      await tester.tap(find.byIcon(Icons.close_rounded));
      await tester.pumpAndSettle();

      // MWP-C #2 & #3: Tab Tài chính -> không còn chuỗi mẫu '6.550.000 ₫'; tạo Recurring Tx & Saving Goal
      await tester.tap(find.text('Tài chính'));
      await tester.pumpAndSettle();
      expect(find.text('6.550.000 ₫'), findsNothing);
      expect(find.text('Tổng chi mẫu'), findsNothing);

      final Finder addRecBtn =
          find.byKey(const Key('btn_open_add_recurring_tx'));
      await tester.scrollUntilVisible(
        addRecBtn,
        300,
        scrollable: find.byType(Scrollable).first,
      );
      await tester.tap(addRecBtn);
      await tester.pumpAndSettle();
      expect(find.text('Xem trước 3 lần lặp kế tiếp'), findsOneWidget);
      await tester.enterText(
        find.byKey(const Key('input_recurring_note')),
        'Tiền thuê nhà',
      );
      await tester.tap(find.byKey(const Key('btn_save_recurring_tx')));
      await tester.pumpAndSettle();
      expect(find.text('Tiền thuê nhà'), findsOneWidget);
      expect(
        db.listEntities(
          userId: userId,
          entityType: SyncEntityType.recurringTransaction,
        ).length,
        1,
      );

      final Finder addSavBtn =
          find.byKey(const Key('btn_open_add_saving_goal'));
      await tester.scrollUntilVisible(
        addSavBtn,
        300,
        scrollable: find.byType(Scrollable).first,
      );
      await tester.tap(addSavBtn);
      await tester.pumpAndSettle();
      await tester.enterText(
        find.byKey(const Key('input_saving_goal_name')),
        'Mua xe đạp',
      );
      await tester.tap(find.byKey(const Key('btn_save_saving_goal')));
      await tester.pumpAndSettle();
      expect(find.text('Mua xe đạp'), findsOneWidget);

      // MWP-C #4: Tab Tri thức -> Quản lý cột mốc (thêm + đánh dấu + xóa)
      await tester.tap(find.text('Tri thức'));
      await tester.pumpAndSettle();
      final Finder msBtn = find.byKey(Key('btn_goal_milestones_$goalId'));
      await tester.scrollUntilVisible(
        msBtn,
        250,
        scrollable: find.byType(Scrollable).first,
      );
      await tester.tap(msBtn);
      await tester.pumpAndSettle();
      await tester.enterText(
        find.byKey(const Key('input_new_milestone_title')),
        'Thuộc 500 Kanji',
      );
      await tester.tap(find.byKey(const Key('btn_add_milestone')));
      await tester.pumpAndSettle();
      expect(find.text('Thuộc 500 Kanji'), findsOneWidget);
      await tester.tap(find.byType(Checkbox).first);
      await tester.pumpAndSettle();
      expect(
        db.listEntities(
          userId: userId,
          entityType: SyncEntityType.goalMilestone,
        ).first.payload['completed'],
        isTrue,
      );
    });
  });
}

