import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:lifeos_mobile/core/database/app_database.dart';
import 'package:lifeos_mobile/core/providers/app_providers.dart';
import 'package:lifeos_mobile/core/sync/entity_types.dart';
import 'package:lifeos_mobile/core/widgets/lifeos_visual_charts.dart';
import 'package:lifeos_mobile/features/auth/presentation/sign_in_welcome_view.dart';
import 'package:lifeos_mobile/features/finance/presentation/widgets/budget_pace_gauge.dart';
import 'package:lifeos_mobile/features/workspace/data/lifeos_local_repository.dart';
import 'package:lifeos_mobile/features/workspace/data/local_extras_repository.dart';
import 'package:lifeos_mobile/main.dart';

void main() {
  group('UPW3 — Kho CSDL phụ LOCAL_ONLY (LifeOsLocalExtrasDatabase)', () {
    test('CRUD Bánh xe cuộc sống & Khu vườn tập trung hoàn toàn độc lập với Outbox', () {
      final LifeOsLocalExtrasDatabase extrasDb = LifeOsLocalExtrasDatabase.inMemory();
      addTearDown(extrasDb.close);

      extrasDb.insertLifeWheelAssessment(
        LocalLifeWheelAssessment(
          id: 'lw-1',
          recordedAt: DateTime.utc(2026, 11, 10, 8, 0),
          career: 8,
          finance: 7,
          health: 9,
          relationships: 8,
          growth: 9,
          family: 8,
          leisure: 6,
          environment: 7,
          notes: 'Cân bằng tốt tuần đầu tháng 11',
        ),
      );

      extrasDb.insertFocusSession(
        LocalFocusSession(
          id: 'fs-1',
          startedAt: DateTime.utc(2026, 11, 10, 9, 0),
          durationMinutes: 25,
          completed: true,
          tag: 'Lập trình Flutter',
        ),
      );

      expect(extrasDb.getLifeWheelHistory().length, 1);
      expect(extrasDb.getLatestLifeWheelAssessment()?.health, 9);
      expect(extrasDb.getPlantedTreesCount(), 1);
      expect(extrasDb.getTotalCompletedFocusMinutes(), 25);

      extrasDb.clearAll();
      expect(extrasDb.getLifeWheelHistory(), isEmpty);
      expect(extrasDb.getFocusSessions(), isEmpty);
    });
  });

  group('UPW5, UPW6 & UPW7 — Kiểm thử giao diện mới, Review Wizard, LOCAL_ONLY & Phương án C', () {
    testWidgets('Tab Kế hoạch, Tài chính (Donut & Pace Gauge) & Tri thức hiển thị đầy đủ widget mới',
        (WidgetTester tester) async {
      tester.view.physicalSize = const Size(430, 932);
      tester.view.devicePixelRatio = 1.0;
      addTearDown(tester.view.resetPhysicalSize);
      addTearDown(tester.view.resetDevicePixelRatio);

      final AppDatabase db = AppDatabase.inMemory();
      addTearDown(db.close);
      final LifeOsLocalExtrasDatabase extras = LifeOsLocalExtrasDatabase.inMemory();
      addTearDown(extras.close);

      final LifeOsLocalRepository repo = LifeOsLocalRepository(db: db);
      const String userId = 'user-local-01';
      repo.createProject(userId: userId, name: 'Nâng cấp UI LifeOS');
      repo.saveNote(userId: userId, title: 'Ghi chú Botanical', content: 'Màu sắc hài hòa');
      repo.createGoal(
        userId: userId,
        title: 'Hoàn thiện UPW1-UPW8',
        startDateIso: '2026-01-01',
        targetDateIso: '2026-12-31',
      );

      await tester.pumpWidget(
        ProviderScope(
          overrides: <Override>[
            appDatabaseProvider.overrideWithValue(db),
            localExtrasDatabaseProvider.overrideWithValue(extras),
          ],
          child: const LifeOsApp(),
        ),
      );
      await tester.pumpAndSettle();

      // Chuyển sang Tab Kế hoạch (UPW6)
      await tester.tap(find.text('Kế hoạch'));
      await tester.pumpAndSettle();
      expect(find.text('Dòng thời gian hôm nay'), findsOneWidget);
      expect(find.text('Nâng cấp UI LifeOS'), findsOneWidget);

      // Chuyển sang Tab Tài chính (UPW5)
      await tester.tap(find.text('Tài chính'));
      await tester.pumpAndSettle();
      expect(find.byType(BudgetPaceGauge), findsOneWidget);
      expect(find.byType(LifeOsDonutChart), findsOneWidget);
      expect(find.text('Phân bổ chi tiêu danh mục'), findsOneWidget);

      // Chuyển sang Tab Tri thức (UPW6)
      await tester.tap(find.text('Tri thức'));
      await tester.pumpAndSettle();
      expect(find.text('Ghi chú Botanical'), findsOneWidget);
      expect(find.text('Hoàn thiện UPW1-UPW8'), findsOneWidget);
    });

    testWidgets(
      'Phương án C [CONFIRMED]: Nút "Xóa dữ liệu trên máy" cảnh báo Outbox + LOCAL_ONLY, bắt nhập "XÓA DỮ LIỆU", xóa sạch cả 2 kho & về màn đăng nhập',
      (WidgetTester tester) async {
        tester.view.physicalSize = const Size(430, 932);
        tester.view.devicePixelRatio = 1.0;
        addTearDown(tester.view.resetPhysicalSize);
        addTearDown(tester.view.resetDevicePixelRatio);

        final AppDatabase db = AppDatabase.inMemory();
        addTearDown(db.close);
        final LifeOsLocalExtrasDatabase extras = LifeOsLocalExtrasDatabase.inMemory();
        addTearDown(extras.close);

        final LifeOsLocalRepository repo = LifeOsLocalRepository(db: db);
        const String userId = 'user-local-01';
        repo.createTask(userId: userId, title: 'Việc chưa đồng bộ #1');
        extras.insertFocusSession(
          LocalFocusSession(
            id: 'fs-wipe-1',
            startedAt: DateTime.utc(2026, 11, 10, 10, 0),
            durationMinutes: 25,
            completed: true,
          ),
        );

        await tester.pumpWidget(
          ProviderScope(
            overrides: <Override>[
              appDatabaseProvider.overrideWithValue(db),
              localExtrasDatabaseProvider.overrideWithValue(extras),
            ],
            child: const LifeOsApp(),
          ),
        );
        await tester.pumpAndSettle();

        // Sang Tab Tổng quan & cuộn xuống nút Xóa dữ liệu trên máy
        await tester.tap(find.text('Tổng quan'));
        await tester.pumpAndSettle();

        final Finder wipeRow = find.text('Xóa dữ liệu trên máy');
        await tester.scrollUntilVisible(
          wipeRow,
          300,
          scrollable: find.byType(Scrollable).last,
        );
        await tester.ensureVisible(wipeRow);
        await tester.pumpAndSettle();
        await tester.tap(wipeRow);
        await tester.pumpAndSettle();

        // Kiểm tra hộp thoại cảnh báo rõ số lượng Outbox (1 mục) & LOCAL_ONLY
        expect(
          find.textContaining('1 thay đổi chưa đồng bộ trong Outbox sẽ bị mất'),
          findsOneWidget,
        );
        expect(
          find.textContaining('LOCAL_ONLY'),
          findsNothing, // Tiếng Việt thân thiện không lộ mã kỹ thuật thô
        );
        expect(
          find.textContaining('Bánh xe cuộc sống, Khu vườn tập trung'),
          findsOneWidget,
        );

        // Trước khi gõ cụm từ xác nhận -> nút Xác nhận xóa vĩnh viễn bị khóa
        final FilledButton confirmBtnBefore = tester.widget<FilledButton>(
          find.widgetWithText(FilledButton, 'Xác nhận xóa vĩnh viễn'),
        );
        expect(confirmBtnBefore.onPressed, isNull);

        // Nhập "XÓA DỮ LIỆU" và bấm xác nhận
        await tester.enterText(find.byType(TextField), 'XÓA DỮ LIỆU');
        await tester.pumpAndSettle();

        await tester.tap(find.text('Xác nhận xóa vĩnh viễn'));
        await tester.pumpAndSettle();

        // CSDL gốc, Outbox và kho phụ đều sạch, đồng thời đưa về màn đăng nhập
        expect(db.listEntities(userId: userId, entityType: SyncEntityType.task), isEmpty);
        expect(db.listOutboxMutations(userId: userId), isEmpty);
        expect(extras.getFocusSessions(), isEmpty);
        expect(find.byType(SignInWelcomeView), findsOneWidget);
      },
    );

    testWidgets(
      'Review Wizard 3 bước lưu vào life_reviews; Life Wheel & Focus Garden chỉ lưu kho phụ LOCAL_ONLY',
      (WidgetTester tester) async {
        tester.view.physicalSize = const Size(430, 932);
        tester.view.devicePixelRatio = 1.0;
        addTearDown(tester.view.resetPhysicalSize);
        addTearDown(tester.view.resetDevicePixelRatio);

        final AppDatabase db = AppDatabase.inMemory();
        addTearDown(db.close);
        final LifeOsLocalExtrasDatabase extras = LifeOsLocalExtrasDatabase.inMemory();
        addTearDown(extras.close);

        await tester.pumpWidget(
          ProviderScope(
            overrides: <Override>[
              appDatabaseProvider.overrideWithValue(db),
              localExtrasDatabaseProvider.overrideWithValue(extras),
            ],
            child: const LifeOsApp(),
          ),
        );
        await tester.pumpAndSettle();

        // 1. Mở Bánh xe cuộc sống từ phím tắt Tab Hôm nay và lưu -> không tạo Outbox!
        final Finder wheelShortcut = find.text('Bánh xe cuộc sống');
        await tester.ensureVisible(wheelShortcut);
        await tester.pumpAndSettle();
        await tester.tap(wheelShortcut);
        await tester.pumpAndSettle();
        final Finder saveWheelBtn = find.text('Lưu đánh giá Bánh xe cuộc sống');
        await tester.scrollUntilVisible(
          saveWheelBtn,
          300,
          scrollable: find.byType(Scrollable).last,
        );
        await tester.ensureVisible(saveWheelBtn);
        await tester.pumpAndSettle();
        await tester.tap(saveWheelBtn);
        await tester.pumpAndSettle();
        expect(extras.getLifeWheelHistory().length, 1);
        expect(db.listOutboxMutations(userId: 'user-local-01'), isEmpty);

        await tester.tap(find.byIcon(Icons.close_rounded));
        await tester.pumpAndSettle();

        // 2. Sang Tab Tổng quan và hoàn thành Review Wizard 3 bước
        await tester.tap(find.text('Tổng quan'));
        await tester.pumpAndSettle();
        await tester.tap(find.text('Mở trình hướng dẫn phản tư'));
        await tester.pumpAndSettle();

        await tester.tap(find.text('Tiếp theo'));
        await tester.pumpAndSettle();
        await tester.tap(find.text('Tiếp theo'));
        await tester.pumpAndSettle();
        await tester.enterText(
          find.byType(TextField),
          'Hoàn thành kiểm thử tự động giao diện mới.',
        );
        await tester.tap(find.text('Hoàn tất'));
        await tester.pumpAndSettle();

        expect(
          db.listEntities(
            userId: 'user-local-01',
            entityType: SyncEntityType.lifeReview,
          ).length,
          1,
        );
        expect(db.listOutboxMutations(userId: 'user-local-01').length, 1);
      },
    );
  });
}

