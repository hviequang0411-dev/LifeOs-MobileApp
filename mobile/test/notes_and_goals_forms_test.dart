import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:lifeos_mobile/core/database/app_database.dart';
import 'package:lifeos_mobile/core/providers/app_providers.dart';
import 'package:lifeos_mobile/core/sync/entity_types.dart';
import 'package:lifeos_mobile/features/knowledge/presentation/widgets/goal_milestones_sheet.dart';
import 'package:lifeos_mobile/features/knowledge/presentation/widgets/note_editor_sheet.dart';
import 'package:lifeos_mobile/features/workspace/data/lifeos_local_repository.dart';
import 'package:lifeos_mobile/main.dart';

void main() {
  group('Biểu mẫu Ghi chú & Mục tiêu dài hạn kèm Cột mốc (Notes & Goals Forms)', () {
    test('KnowledgeLocalRepository hỗ trợ GoalMilestoneDraft, updateGoal, deleteGoal, deleteNote, deleteNoteLink', () {
      final AppDatabase db = AppDatabase.inMemory();
      addTearDown(db.close);
      final LifeOsLocalRepository repo = LifeOsLocalRepository(db: db);
      const String userId = 'u-test-kg';

      // 1. Tạo Note
      final String noteId = repo.saveNote(
        userId: userId,
        title: 'Ghi chú kiến trúc',
        content: 'Nội dung phân tán',
        pinned: true,
      );
      final LocalEntityRow? noteRow = db.getEntity(
        userId: userId,
        entityType: SyncEntityType.note,
        entityId: noteId,
      );
      expect(noteRow, isNotNull);
      expect(noteRow!.payload['title'], 'Ghi chú kiến trúc');
      expect(noteRow.payload['pinned'], isTrue);

      // 2. Tạo Goal kèm nhiều cột mốc qua GoalMilestoneDraft
      final String goalId = repo.createGoal(
        userId: userId,
        title: 'Mục tiêu tiếng Nhật N2',
        description: 'Tự học 6 tháng',
        startDateIso: '2026-01-01',
        targetDateIso: '2026-06-30',
        milestoneDrafts: const <GoalMilestoneDraft>[
          GoalMilestoneDraft(title: 'Học 500 chữ Hán', dueOnIso: '2026-03-01'),
          GoalMilestoneDraft(title: 'Luyện đề Choukai', dueOnIso: '2026-05-15'),
          GoalMilestoneDraft(title: 'Thi thử 120 điểm', dueOnIso: '2026-06-15'),
        ],
      );

      final List<LocalEntityRow> msList = db
          .listEntities(userId: userId, entityType: SyncEntityType.goalMilestone)
          .where((LocalEntityRow m) => m.payload['goalId'] == goalId)
          .toList()
        ..sort((LocalEntityRow a, LocalEntityRow b) =>
            (a.payload['position'] as num).toInt().compareTo((b.payload['position'] as num).toInt()));
      expect(msList.length, 3);
      expect(msList[0].payload['title'], 'Học 500 chữ Hán');
      expect(msList[0].payload['dueOn'], '2026-03-01');
      expect(msList[1].payload['title'], 'Luyện đề Choukai');
      expect(msList[2].payload['title'], 'Thi thử 120 điểm');

      // 3. Liên kết Note tới Goal
      final String linkId = repo.linkNoteToTarget(
        userId: userId,
        noteId: noteId,
        targetType: 'GOAL',
        targetId: goalId,
      );
      expect(repo.listLinksForNote(userId: userId, noteId: noteId).length, 1);

      // 4. Update Goal
      repo.updateGoal(
        userId: userId,
        goalId: goalId,
        title: 'Mục tiêu tiếng Nhật N2 (Cập nhật)',
        description: 'Đã hoàn thành 50% chặng đường',
        startDateIso: '2026-01-01',
        targetDateIso: '2026-07-15',
        status: 'COMPLETED',
      );
      final LocalEntityRow? updatedGoal = db.getEntity(
        userId: userId,
        entityType: SyncEntityType.goal,
        entityId: goalId,
      );
      expect(updatedGoal!.payload['title'], 'Mục tiêu tiếng Nhật N2 (Cập nhật)');
      expect(updatedGoal.payload['status'], 'COMPLETED');
      expect(updatedGoal.payload['targetOn'], '2026-07-15');

      // 5. Delete Note Link & Delete Note
      repo.deleteNoteLink(userId: userId, linkId: linkId);
      expect(repo.listLinksForNote(userId: userId, noteId: noteId), isEmpty);

      repo.deleteNote(userId: userId, noteId: noteId);
      expect(
        db.getEntity(userId: userId, entityType: SyncEntityType.note, entityId: noteId),
        isNull,
      );

      // 6. Delete Goal
      repo.deleteGoal(userId: userId, goalId: goalId);
      expect(
        db.getEntity(userId: userId, entityType: SyncEntityType.goal, entityId: goalId),
        isNull,
      );
    });

    testWidgets('Luồng UI NoteEditorSheet: Tạo ghi chú mới, kiểm tra validate, sửa ghi chú và ghim',
        (WidgetTester tester) async {
      final AppDatabase db = AppDatabase.inMemory();
      addTearDown(db.close);
      const String userId = 'u-test-ui-note';

      await tester.pumpWidget(
        ProviderScope(
          overrides: <Override>[
            appDatabaseProvider.overrideWithValue(db),
            currentUserIdProvider.overrideWith((Ref ref) => userId),
            referenceUtcProvider.overrideWithValue(DateTime.utc(2026, 11, 13, 10, 0)),
          ],
          child: const MaterialApp(
            home: Scaffold(
              body: NoteEditorSheet(),
            ),
          ),
        ),
      );
      await tester.pumpAndSettle();

      // Tiêu đề Sheet tạo mới
      expect(find.text('Ghi chú mới'), findsOneWidget);

      // Bấm lưu khi tiêu đề rỗng -> hiện báo lỗi
      await tester.tap(find.byKey(const Key('btn_save_note')));
      await tester.pumpAndSettle();
      expect(find.text('Vui lòng nhập tiêu đề ghi chú.'), findsOneWidget);

      // Nhập tiêu đề, nội dung và bật ghim
      await tester.enterText(
        find.byKey(const Key('input_note_title')),
        'Chiến lược Offline-First 2026',
      );
      await tester.enterText(
        find.byKey(const Key('input_note_content')),
        'Dữ liệu luôn lưu cục bộ trước trên SQLite rồi mới đẩy qua Outbox.',
      );
      await tester.tap(find.byKey(const Key('switch_note_pinned')));
      await tester.pumpAndSettle();

      await tester.tap(find.byKey(const Key('btn_save_note')));
      await tester.pumpAndSettle();

      final List<LocalEntityRow> savedNotes = db.listEntities(
        userId: userId,
        entityType: SyncEntityType.note,
      );
      expect(savedNotes.length, 1);
      final LocalEntityRow note = savedNotes.first;
      expect(note.payload['title'], 'Chiến lược Offline-First 2026');
      expect(note.payload['pinned'], isTrue);

      // Mở Sheet ở chế độ Chỉnh sửa
      await tester.pumpWidget(
        ProviderScope(
          overrides: <Override>[
            appDatabaseProvider.overrideWithValue(db),
            currentUserIdProvider.overrideWith((Ref ref) => userId),
            referenceUtcProvider.overrideWithValue(DateTime.utc(2026, 11, 13, 10, 0)),
          ],
          child: MaterialApp(
            home: Scaffold(
              body: NoteEditorSheet(note: note),
            ),
          ),
        ),
      );
      await tester.pumpAndSettle();

      expect(find.text('Chỉnh sửa ghi chú'), findsOneWidget);
      expect(find.byKey(const Key('switch_note_archived')), findsOneWidget);

      // Sửa tiêu đề và lưu trữ
      await tester.enterText(
        find.byKey(const Key('input_note_title')),
        'Chiến lược Offline-First (Đã duyệt)',
      );
      await tester.tap(find.byKey(const Key('switch_note_archived')));
      await tester.pumpAndSettle();

      await tester.tap(find.byKey(const Key('btn_save_note')));
      await tester.pumpAndSettle();

      final LocalEntityRow updated = db.getEntity(
        userId: userId,
        entityType: SyncEntityType.note,
        entityId: note.id,
      )!;
      expect(updated.payload['title'], 'Chiến lược Offline-First (Đã duyệt)');
      expect(updated.payload['archived'], isTrue);
    });

    testWidgets('Luồng UI CreateGoalSheet & GoalEditorSheet: Tạo mục tiêu kèm nhiều cột mốc và chỉnh sửa',
        (WidgetTester tester) async {
      final AppDatabase db = AppDatabase.inMemory();
      addTearDown(db.close);
      const String userId = 'u-test-ui-goal';

      await tester.pumpWidget(
        ProviderScope(
          overrides: <Override>[
            appDatabaseProvider.overrideWithValue(db),
            currentUserIdProvider.overrideWith((Ref ref) => userId),
            referenceUtcProvider.overrideWithValue(DateTime.utc(2026, 11, 13, 10, 0)),
          ],
          child: const MaterialApp(
            home: Scaffold(
              body: CreateGoalSheet(),
            ),
          ),
        ),
      );
      await tester.pumpAndSettle();

      expect(find.text('Thiết lập mục tiêu mới'), findsOneWidget);

      // Nhập thông tin mục tiêu
      await tester.enterText(
        find.byKey(const Key('input_create_goal_title')),
        'Tốt nghiệp Thạc sĩ AI',
      );
      await tester.enterText(
        find.byKey(const Key('input_create_goal_description')),
        'Bảo vệ luận văn xuất sắc tại ĐHQG',
      );
      await tester.enterText(
        find.byKey(const Key('input_create_goal_first_milestone')),
        'Hoàn thành các môn lý thuyết',
      );

      // Thêm cột mốc bổ sung vào mục tiêu dài hạn
      await tester.ensureVisible(find.byKey(const Key('input_create_goal_extra_milestone_title')));
      await tester.enterText(
        find.byKey(const Key('input_create_goal_extra_milestone_title')),
        'Xuất bản bài báo hội thảo',
      );
      await tester.enterText(
        find.byKey(const Key('input_create_goal_extra_milestone_due')),
        '2026-08-15',
      );
      await tester.ensureVisible(find.byKey(const Key('btn_add_draft_milestone')));
      await tester.tap(find.byKey(const Key('btn_add_draft_milestone')));
      await tester.pumpAndSettle();

      expect(find.text('Xuất bản bài báo hội thảo'), findsOneWidget);

      // Bấm Tạo mục tiêu
      await tester.ensureVisible(find.byKey(const Key('btn_submit_create_goal')));
      await tester.tap(find.byKey(const Key('btn_submit_create_goal')));
      await tester.pumpAndSettle();

      final List<LocalEntityRow> goals = db.listEntities(
        userId: userId,
        entityType: SyncEntityType.goal,
      );
      expect(goals.length, 1);
      final LocalEntityRow goal = goals.first;
      expect(goal.payload['title'], 'Tốt nghiệp Thạc sĩ AI');
      expect(goal.payload['description'], 'Bảo vệ luận văn xuất sắc tại ĐHQG');

      final List<LocalEntityRow> milestones = db
          .listEntities(userId: userId, entityType: SyncEntityType.goalMilestone)
          .where((LocalEntityRow m) => m.payload['goalId'] == goal.id)
          .toList()
        ..sort((LocalEntityRow a, LocalEntityRow b) =>
            (a.payload['position'] as num).toInt().compareTo((b.payload['position'] as num).toInt()));
      expect(milestones.length, 2);
      expect(milestones[0].payload['title'], 'Hoàn thành các môn lý thuyết');
      expect(milestones[1].payload['title'], 'Xuất bản bài báo hội thảo');
      expect(milestones[1].payload['dueOn'], '2026-08-15');

      // Mở Sheet GoalEditorSheet để kiểm tra chỉnh sửa và thêm cột mốc inline
      await tester.pumpWidget(
        ProviderScope(
          overrides: <Override>[
            appDatabaseProvider.overrideWithValue(db),
            currentUserIdProvider.overrideWith((Ref ref) => userId),
            referenceUtcProvider.overrideWithValue(DateTime.utc(2026, 11, 13, 10, 0)),
          ],
          child: MaterialApp(
            home: Scaffold(
              body: GoalEditorSheet(goal: goal),
            ),
          ),
        ),
      );
      await tester.pumpAndSettle();

      expect(find.text('Chỉnh sửa mục tiêu'), findsOneWidget);
      expect(find.text('Hoàn thành các môn lý thuyết'), findsOneWidget);

      // Sửa tên mục tiêu & chuyển sang trạng thái COMPLETED
      await tester.enterText(
        find.byKey(const Key('input_edit_goal_title')),
        'Tốt nghiệp Thạc sĩ AI (Thủ khoa)',
      );
      await tester.tap(find.text('Hoàn thành'));
      await tester.pumpAndSettle();

      // Thêm cột mốc thứ 3 ngay trong form chỉnh sửa
      await tester.ensureVisible(find.byKey(const Key('input_edit_goal_new_milestone_title')));
      await tester.enterText(
        find.byKey(const Key('input_edit_goal_new_milestone_title')),
        'Bảo vệ luận văn trước hội đồng',
      );
      await tester.ensureVisible(find.byKey(const Key('btn_edit_goal_add_milestone')));
      await tester.tap(find.byKey(const Key('btn_edit_goal_add_milestone')));
      await tester.pumpAndSettle();

      expect(find.text('Bảo vệ luận văn trước hội đồng'), findsOneWidget);

      await tester.ensureVisible(find.byKey(const Key('btn_save_goal_changes')));
      await tester.tap(find.byKey(const Key('btn_save_goal_changes')));
      await tester.pumpAndSettle();

      final LocalEntityRow updatedGoal = db.getEntity(
        userId: userId,
        entityType: SyncEntityType.goal,
        entityId: goal.id,
      )!;
      expect(updatedGoal.payload['title'], 'Tốt nghiệp Thạc sĩ AI (Thủ khoa)');
      expect(updatedGoal.payload['status'], 'COMPLETED');

      final int totalMs = db
          .listEntities(userId: userId, entityType: SyncEntityType.goalMilestone)
          .where((LocalEntityRow m) => m.payload['goalId'] == goal.id)
          .length;
      expect(totalMs, 3);
    });

    testWidgets('Tích hợp đầy đủ trên Tab Tri thức (LifeOsApp): Chạm vào thẻ mở form sửa, ghim icon hiển thị đúng',
        (WidgetTester tester) async {
      final AppDatabase db = AppDatabase.inMemory();
      addTearDown(db.close);
      final LifeOsLocalRepository repo = LifeOsLocalRepository(db: db);
      const String userId = 'user-local-01';

      repo.saveNote(
        userId: userId,
        title: 'Ghi chú quan trọng',
        content: 'Nội dung mẫu',
        pinned: true,
      );
      repo.createGoal(
        userId: userId,
        title: 'Mục tiêu chạy bộ',
        startDateIso: '2026-01-01',
        targetDateIso: '2026-12-31',
        milestoneTitles: const <String>['Chạy 5km đầu tiên'],
      );

      await tester.pumpWidget(
        ProviderScope(
          overrides: <Override>[
            appDatabaseProvider.overrideWithValue(db),
            currentUserIdProvider.overrideWith((Ref ref) => userId),
            referenceUtcProvider.overrideWithValue(DateTime.utc(2026, 11, 13, 10, 0)),
          ],
          child: const LifeOsApp(),
        ),
      );
      await tester.pumpAndSettle();

      // Chuyển sang Tab Tri thức
      await tester.tap(find.text('Tri thức'));
      await tester.pumpAndSettle();

      expect(find.text('Ghi chú quan trọng'), findsOneWidget);
      expect(find.byIcon(Icons.push_pin_rounded), findsOneWidget);
      expect(find.text('Mục tiêu chạy bộ'), findsOneWidget);

      // Bấm nút "Ghi chú mới" -> Mở NoteEditorSheet
      await tester.tap(find.byKey(const Key('btn_open_create_note')));
      await tester.pumpAndSettle();
      expect(find.byType(NoteEditorSheet), findsOneWidget);
      Navigator.of(tester.element(find.byType(NoteEditorSheet))).pop();
      await tester.pumpAndSettle();
      expect(find.byType(NoteEditorSheet), findsNothing);

      // Chạm vào thẻ Goal -> Mở GoalEditorSheet
      final LocalEntityRow goalEntity = db
          .listEntities(userId: userId, entityType: SyncEntityType.goal)
          .first;
      await tester.tap(find.byKey(Key('btn_edit_goal_${goalEntity.id}')));
      await tester.pumpAndSettle();
      expect(find.text('Chỉnh sửa mục tiêu'), findsOneWidget);
    });
  });
}

