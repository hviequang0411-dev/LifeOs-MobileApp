import '../../../core/database/app_database.dart';
import '../../../core/money/decimal10k.dart';
import '../../../core/sync/entity_types.dart';
import 'lifeos_local_repository.dart';

/// Khởi tạo bộ dữ liệu mẫu cố định **chỉ dành riêng cho Golden Test / chụp ảnh kiểm thử**
/// (không được gọi trong bản ứng dụng thường `dev`/`staging`/`prod`).
class DemoWorkspaceSeeder {
  const DemoWorkspaceSeeder._();

  static final DateTime defaultFixedSeedUtc = DateTime.utc(2026, 11, 8, 2, 0);

  static void seedIfEmpty({
    required LifeOsLocalRepository repo,
    required String userId,
    DateTime? nowUtc,
  }) {
    final List<LocalEntityRow> existingTasks = repo.db.listEntities(
      userId: userId,
      entityType: SyncEntityType.task,
    );
    if (existingTasks.isNotEmpty) return;

    final DateTime baseUtc = (nowUtc ?? defaultFixedSeedUtc).toUtc();
    int stepCounter = 0;
    DateTime nextStepUtc() {
      stepCounter += 1;
      return baseUtc.add(Duration(seconds: stepCounter));
    }

    final String todayIso =
        repo.getUserDayClock(userId).businessDateForUtc(baseUtc);
    final String yesterdayIso = repo
        .getUserDayClock(userId)
        .businessDateForUtc(baseUtc.subtract(const Duration(days: 1)));

    final String p1 = repo.createProject(
      userId: userId,
      name: 'Hệ sinh thái LifeOS Mobile',
      color: '#1B6B50',
      nowUtc: nextStepUtc(),
    );
    repo.createProject(
      userId: userId,
      name: 'Sức khỏe & Cân bằng cuộc sống',
      color: '#D97724',
      nowUtc: nextStepUtc(),
    );

    final String tDone = repo.createTask(
      userId: userId,
      title: 'Hoàn thiện kiến trúc Offline-First 29 thực thể',
      projectId: p1,
      priority: 'HIGH',
      dueDate: todayIso,
      isMyDay: true,
      nowUtc: nextStepUtc(),
    );
    repo.toggleTaskCompletion(
      userId: userId,
      taskId: tDone,
      nowUtc: nextStepUtc(),
    );

    final String tActive = repo.createTask(
      userId: userId,
      title: 'Rà soát giao diện Calm Botanical Light & Dark',
      projectId: p1,
      priority: 'HIGH',
      dueDate: todayIso,
      isMyDay: true,
      nowUtc: nextStepUtc(),
    );
    repo.createTask(
      userId: userId,
      title: 'Đọc 25 trang sách Thiết kế Hệ thống Phân tán',
      priority: 'MEDIUM',
      dueDate: todayIso,
      isMyDay: true,
      nowUtc: nextStepUtc(),
    );

    final String h1 = repo.createHabit(
      userId: userId,
      name: 'Uống đủ 2 lít nước ấm',
      frequencyType: 'DAILY',
      nowUtc: nextStepUtc(),
    );
    repo.checkInHabit(
      userId: userId,
      habitId: h1,
      entryDateIso: yesterdayIso,
      status: 'DONE',
      nowUtc: nextStepUtc(),
    );
    repo.checkInHabit(
      userId: userId,
      habitId: h1,
      entryDateIso: todayIso,
      status: 'DONE',
      nowUtc: nextStepUtc(),
    );
    final String h2 = repo.createHabit(
      userId: userId,
      name: 'Thiền thở 10 phút buổi sáng',
      frequencyType: 'DAILY',
      nowUtc: nextStepUtc(),
    );
    repo.checkInHabit(
      userId: userId,
      habitId: h2,
      entryDateIso: yesterdayIso,
      status: 'DONE',
      nowUtc: nextStepUtc(),
    );

    repo.createCalendarEvent(
      userId: userId,
      title: 'Họp đồng bộ kiến trúc tuần',
      startLocal: '${todayIso}T09:00:00',
      endLocal: '${todayIso}T10:00:00',
      recurrence: const <String, dynamic>{
        'freq': 'WEEKLY',
        'interval': 1,
        'byWeekday': <int>[1, 3, 5],
        'count': 12,
      },
      nowUtc: nextStepUtc(),
    );
    repo.createCalendarEvent(
      userId: userId,
      title: 'Khối tập trung sâu (Deep Work Time Block)',
      startLocal: '${todayIso}T14:00:00',
      endLocal: '${todayIso}T16:00:00',
      recurrence: const <String, dynamic>{
        'freq': 'DAILY',
        'interval': 1,
        'count': 10,
      },
      nowUtc: nextStepUtc(),
    );

    final String bankAcc = repo.createFinanceAccount(
      userId: userId,
      name: 'Vietcombank Chính',
      accountType: 'BANK',
      openingBalance: Decimal10k.fromWhole(18500000),
      nowUtc: nextStepUtc(),
    );
    final String cashAcc = repo.createFinanceAccount(
      userId: userId,
      name: 'Ví Tiền mặt hàng ngày',
      accountType: 'CASH',
      openingBalance: Decimal10k.fromWhole(2200000),
      nowUtc: nextStepUtc(),
    );
    repo.createFinanceTransaction(
      userId: userId,
      accountId: cashAcc,
      kind: 'EXPENSE',
      amount: Decimal10k.fromWhole(65000),
      occurredOnIso: todayIso,
      note: 'Cà phê sáng & Bánh mì nguyên cám',
      nowUtc: nextStepUtc(),
    );
    repo.createFinanceTransaction(
      userId: userId,
      accountId: bankAcc,
      kind: 'EXPENSE',
      amount: Decimal10k.fromWhole(120000),
      occurredOnIso: todayIso,
      note: 'Mua sách kỹ thuật phần mềm',
      nowUtc: nextStepUtc(),
    );

    final String goalId = repo.createGoal(
      userId: userId,
      title: 'Ra mắt LifeOS v1.0 (Backend + Mobile)',
      startDateIso: '2026-01-01',
      targetDateIso: '2026-12-31',
      milestoneTitles: const <String>[
        'Hoàn thành Backend 847 test xanh',
        'Hoàn thành Mobile Offline-First',
      ],
      nowUtc: nextStepUtc(),
    );

    final String n1 = repo.saveNote(
      userId: userId,
      title: 'Mục tiêu Kiến trúc 2026',
      content:
          '# Tầm nhìn 2026\nXây dựng nền tảng quản lý cuộc sống cá nhân vững chắc.',
      pinned: true,
      nowUtc: nextStepUtc(),
    );
    final String n2 = repo.saveNote(
      userId: userId,
      title: 'Sổ tay Thiết kế Calm Botanical',
      content:
          '# Ngôn ngữ thiết kế\nBố cục ấm áp, chữ tiếng Việt rõ nét, ưu tiên thao tác 1 chạm.',
      nowUtc: nextStepUtc(),
    );
    repo.linkNoteToTarget(
      userId: userId,
      noteId: n1,
      targetType: 'GOAL',
      targetId: goalId,
      nowUtc: nextStepUtc(),
    );
    repo.linkNoteToTarget(
      userId: userId,
      noteId: n2,
      targetType: 'TASK',
      targetId: tActive,
      nowUtc: nextStepUtc(),
    );

    repo.saveLifeReview(
      userId: userId,
      periodType: 'WEEK',
      periodKey: '2026-11-02',
      reflection: 'Hoàn tất rà soát hợp đồng đồng bộ và 4 bộ Vector.',
      mood: 5,
      nowUtc: nextStepUtc(),
    );

    // Đánh dấu dữ liệu mẫu đã đồng bộ để màn hình Golden hiển thị trạng thái "Đã lưu trên máy · Đã đồng bộ"
    final Set<String> touched = <String>{
      'outbox',
      'sync_state',
      for (final SyncEntityType t in SyncEntityType.values) t.tableName,
    };
    repo.db.runInTransaction<void>(
      affectedTables: touched,
      () {
        repo.db.driftDb.executeSql(
          'DELETE FROM outbox WHERE user_id = ?;',
          <Object?>[userId],
        );
        for (final SyncEntityType t in SyncEntityType.values) {
          repo.db.driftDb.executeSql(
            "UPDATE ${t.tableName} SET sync_state = 'synced', version = 1 WHERE user_id = ?;",
            <Object?>[userId],
          );
        }
        repo.db.upsertSyncState(
          userId: userId,
          lastCursor: 23,
          lastSyncAt: baseUtc.toIso8601String(),
        );
      },
    );
  }
}
