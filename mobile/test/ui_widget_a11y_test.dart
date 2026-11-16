import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:lifeos_mobile/core/database/app_database.dart';
import 'package:lifeos_mobile/core/providers/app_providers.dart';
import 'package:lifeos_mobile/core/sync/entity_types.dart';
import 'package:lifeos_mobile/core/theme/app_tokens.dart';
import 'package:lifeos_mobile/features/workspace/data/lifeos_local_repository.dart';
import 'package:lifeos_mobile/main.dart';

void main() {
  group(
      'PWP1 & PWP13 — Kiểm thử Widget, Responsive (Phone/Tablet), Light/Dark & Accessibility',
      () {
    testWidgets(
      'Hiển thị màn hình My Day trên Điện thoại (NavigationBar), mở Quick Capture và tạo việc cần làm vào SQLite + Outbox',
      (WidgetTester tester) async {
        tester.view.physicalSize = const Size(390, 844);
        tester.view.devicePixelRatio = 1.0;
        addTearDown(tester.view.resetPhysicalSize);
        addTearDown(tester.view.resetDevicePixelRatio);

        final AppDatabase db = AppDatabase.inMemory();
        addTearDown(db.close);

        await tester.pumpWidget(
          ProviderScope(
            overrides: <Override>[
              appDatabaseProvider.overrideWithValue(db),
            ],
            child: const LifeOsApp(),
          ),
        );
        await tester.pumpAndSettle();

        expect(find.byType(NavigationBar), findsOneWidget);
        expect(find.byType(NavigationRail), findsNothing);
        expect(find.text('Chào ngày mới, bạn'), findsOneWidget);

        // Kiểm tra vùng chạm tối thiểu >= 48dp của nút Thêm nhanh
        final Size fabSize =
            tester.getSize(find.byKey(const Key('fab_quick_capture')));
        expect(fabSize.height, greaterThanOrEqualTo(48.0));

        // Mở Quick Capture và thêm 1 công việc mới
        await tester.tap(find.byKey(const Key('fab_quick_capture')));
        await tester.pumpAndSettle();

        await tester.enterText(
          find.byKey(const Key('input_quick_task_title')),
          'Kiểm thử giao diện Calm Botanical',
        );
        await tester.tap(find.byKey(const Key('btn_submit_quick_task')));
        await tester.pumpAndSettle();

        // Công việc xuất hiện ngay trên UI từ SQLite Stream và nằm trong Outbox
        expect(find.text('Kiểm thử giao diện Calm Botanical'), findsOneWidget);
        expect(
          db.listEntities(
            userId: 'user-local-01',
            entityType: SyncEntityType.task,
          ).length,
          1,
        );
        expect(db.listOutboxMutations(userId: 'user-local-01').length, 1);
      },
    );

    testWidgets(
      'Bố cục thích ứng trên Tablet (>= 840dp) tự chuyển sang NavigationRail + 2 cột và hỗ trợ Chế độ Tối + Ẩn số tiền ••••••',
      (WidgetTester tester) async {
        tester.view.physicalSize = const Size(1024, 768);
        tester.view.devicePixelRatio = 1.0;
        addTearDown(tester.view.resetPhysicalSize);
        addTearDown(tester.view.resetDevicePixelRatio);

        final AppDatabase db = AppDatabase.inMemory();
        addTearDown(db.close);

        await tester.pumpWidget(
          ProviderScope(
            overrides: <Override>[
              appDatabaseProvider.overrideWithValue(db),
              themeModeProvider.overrideWith((Ref ref) => ThemeMode.dark),
            ],
            child: const LifeOsApp(),
          ),
        );
        await tester.pumpAndSettle();

        expect(find.byType(NavigationRail), findsOneWidget);
        expect(find.byType(NavigationBar), findsNothing);

        // Kiểm tra nút ẩn/hiện số tiền tài chính (••••••) kèm Tooltip & Semantics
        expect(find.text('0 ₫'), findsOneWidget);
        expect(find.byTooltip('Ẩn số tiền'), findsOneWidget);
        await tester.tap(find.byKey(const Key('btn_toggle_mask_amount')));
        await tester.pumpAndSettle();
        expect(find.text('•••••• ₫'), findsOneWidget);
        expect(find.byTooltip('Hiện số tiền'), findsOneWidget);
      },
    );

    testWidgets(
      'Nút điểm danh phân biệt rõ 2 trạng thái Chưa điểm danh (FilledButton cam hổ phách) và Đã điểm danh (OutlinedButton xanh nhạt + icon tích)',
      (WidgetTester tester) async {
        tester.view.physicalSize = const Size(390, 844);
        tester.view.devicePixelRatio = 1.0;
        addTearDown(tester.view.resetPhysicalSize);
        addTearDown(tester.view.resetDevicePixelRatio);

        final AppDatabase db = AppDatabase.inMemory();
        addTearDown(db.close);

        final LifeOsLocalRepository repo = LifeOsLocalRepository(db: db);
        const String userId = 'user-local-01';
        final String habitId = repo.createHabit(
          userId: userId,
          name: 'Đọc sách 20 trang',
          frequencyType: 'DAILY',
        );

        await tester.pumpWidget(
          ProviderScope(
            overrides: <Override>[
              appDatabaseProvider.overrideWithValue(db),
            ],
            child: const LifeOsApp(),
          ),
        );
        await tester.pumpAndSettle();

        // 1. Trạng thái Chưa điểm danh: nút nổi bật FilledButton, nhãn "Điểm danh", vùng chạm >= 48dp
        final Finder pendingBtnFinder =
            find.byKey(Key('btn_habit_${habitId}_pending'));
        await tester.ensureVisible(pendingBtnFinder);
        await tester.pumpAndSettle();
        expect(pendingBtnFinder, findsOneWidget);
        expect(find.text('Điểm danh'), findsOneWidget);
        final Size pendingSize = tester.getSize(pendingBtnFinder);
        expect(
          pendingSize.height,
          greaterThanOrEqualTo(LifeOsTokens.minTouchTarget),
        );

        // 2. Chạm vào nút -> Chuyển sang trạng thái Đã điểm danh
        await tester.tap(pendingBtnFinder);
        await tester.pumpAndSettle();

        // 3. Trạng thái Đã điểm danh: nút OutlinedButton dịu hẳn có icon check_rounded, nhãn "Đã điểm danh"
        final Finder doneBtnFinder =
            find.byKey(Key('btn_habit_${habitId}_done'));
        await tester.ensureVisible(doneBtnFinder);
        await tester.pumpAndSettle();
        expect(doneBtnFinder, findsOneWidget);
        expect(find.text('Đã điểm danh'), findsOneWidget);
        expect(find.byIcon(Icons.check_rounded), findsOneWidget);
        final Size doneSize = tester.getSize(doneBtnFinder);
        expect(
          doneSize.height,
          greaterThanOrEqualTo(LifeOsTokens.minTouchTarget),
        );

        // 4. Chạm tiếp -> Chuyển sang SKIPPED (Đã bỏ qua, kiểu thứ ba)
        await tester.tap(doneBtnFinder);
        await tester.pumpAndSettle();

        final Finder skippedBtnFinder =
            find.byKey(Key('btn_habit_${habitId}_skipped'));
        await tester.ensureVisible(skippedBtnFinder);
        await tester.pumpAndSettle();
        expect(skippedBtnFinder, findsOneWidget);
        expect(find.text('Đã bỏ qua'), findsOneWidget);
        expect(find.byIcon(Icons.redo_rounded), findsOneWidget);
      },
    );

    testWidgets(
      'Accessibility (WCAG AA): Phóng chữ hệ thống 200% (textScaler 2.0) và bật Reduce Motion (disableAnimations) không gây tràn bố cục',
      (WidgetTester tester) async {
        tester.view.physicalSize = const Size(430, 932);
        tester.view.devicePixelRatio = 1.0;
        addTearDown(tester.view.resetPhysicalSize);
        addTearDown(tester.view.resetDevicePixelRatio);

        final AppDatabase db = AppDatabase.inMemory();
        addTearDown(db.close);

        await tester.pumpWidget(
          ProviderScope(
            overrides: <Override>[
              appDatabaseProvider.overrideWithValue(db),
            ],
            child: const MediaQuery(
              data: MediaQueryData(
                size: Size(430, 932),
                textScaler: TextScaler.linear(2.0),
                disableAnimations: true,
              ),
              child: LifeOsApp(),
            ),
          ),
        );
        await tester.pumpAndSettle();

        // Chuyển qua từng tab chính mà không phát sinh lỗi RenderFlex overflow
        await tester.tap(find.text('Kế hoạch'));
        await tester.pumpAndSettle();
        await tester.tap(find.text('Tài chính'));
        await tester.pumpAndSettle();
        await tester.tap(find.text('Tri thức'));
        await tester.pumpAndSettle();
        await tester.tap(find.text('Tổng quan'));
        await tester.pumpAndSettle();

        expect(tester.takeException(), isNull);
      },
    );
  });
}
