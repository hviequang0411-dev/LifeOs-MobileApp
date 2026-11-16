import 'package:uuid/uuid.dart';

import '../../../core/database/app_database.dart';
import '../../../core/sync/entity_types.dart';

/// Bản nháp cột mốc khi thiết lập Mục tiêu dài hạn (`milestone`).
class GoalMilestoneDraft {
  const GoalMilestoneDraft({
    required this.title,
    this.dueOnIso,
  });

  final String title;
  final String? dueOnIso;
}

/// Repository cục bộ cho miền **Knowledge (Notes, Tags, Note Links), Goals/Milestones & Life Review**
/// (`docs/phases/PHASE_6_KNOWLEDGE_GOALS.md` D180–D189, `PHASE_7` D202 & `SYNC_PROTOCOL.md` §3.8–§3.9).
///
/// Tuân thủ tuyệt đối:
/// - Không có liên kết hai chiều kiểu wiki (`PHASE_6_KNOWLEDGE_GOALS.md` dòng 6: ngoài phạm vi).
/// - `note_link` (`D189` dòng 56, `SYNC_PROTOCOL.md` §3.8 dòng 173, `openapi.yaml` dòng 6257–6271):
///   chỉ liên kết một chiều từ Ghi chú tới `TASK` hoặc `GOAL` (`{id, noteId, targetType, targetId}`).
class KnowledgeAndGoalLocalRepository {
  KnowledgeAndGoalLocalRepository({
    required this.db,
    Uuid? uuid,
  }) : _uuid = uuid ?? const Uuid();

  final AppDatabase db;
  final Uuid _uuid;

  /// Tạo hoặc cập nhật Ghi chú văn bản thuần (`note` — `D180`, giới hạn 192 KB UTF-8, `SYNC_PROTOCOL.md` §3.8 dòng 170).
  String saveNote({
    required String userId,
    String? noteId,
    required String title,
    required String content,
    bool pinned = false,
    bool archived = false,
    DateTime? nowUtc,
  }) {
    final String resolvedId = noteId ?? _uuid.v4();
    final bool isUpdate = noteId != null &&
        db.getEntity(
              userId: userId,
              entityType: SyncEntityType.note,
              entityId: noteId,
            ) !=
            null;

    db.mutateLocalAndEnqueueOutbox(
      userId: userId,
      entityType: SyncEntityType.note,
      entityId: resolvedId,
      op: isUpdate ? 'UPDATE' : 'CREATE',
      payload: <String, dynamic>{
        'id': resolvedId,
        'title': title.trim(),
        'content': content,
        'pinned': pinned,
        'archived': archived,
      },
      nowUtc: nowUtc,
    );
    return resolvedId;
  }

  /// Tạo liên kết một chiều từ Ghi chú tới `TASK` hoặc `GOAL`
  /// (`note_link` — `PHASE_6_KNOWLEDGE_GOALS.md` D189 dòng 56 & `openapi.yaml` `CreateNoteLinkRequest` dòng 6257–6271).
  String linkNoteToTarget({
    required String userId,
    required String noteId,
    required String targetType, // 'TASK' | 'GOAL'
    required String targetId,
    DateTime? nowUtc,
  }) {
    final String normalizedType = targetType.toUpperCase();
    if (normalizedType != 'TASK' && normalizedType != 'GOAL') {
      throw ArgumentError(
        'targetType của note_link chỉ được phép là TASK hoặc GOAL (D189): "$targetType"',
      );
    }

    final List<LocalEntityRow> existing = db.listEntities(
      userId: userId,
      entityType: SyncEntityType.noteLink,
    );
    for (final LocalEntityRow row in existing) {
      if (row.payload['noteId'] == noteId &&
          row.payload['targetType'] == normalizedType &&
          row.payload['targetId'] == targetId) {
        return row.id;
      }
    }

    final String linkId = _uuid.v4();
    db.mutateLocalAndEnqueueOutbox(
      userId: userId,
      entityType: SyncEntityType.noteLink,
      entityId: linkId,
      op: 'CREATE',
      payload: <String, dynamic>{
        'id': linkId,
        'noteId': noteId,
        'targetType': normalizedType,
        'targetId': targetId,
      },
      nowUtc: nowUtc,
    );
    return linkId;
  }

  /// Truy vấn danh sách các liên kết (`TASK`/`GOAL`) của một Ghi chú (`note_link` D189).
  List<LocalEntityRow> listLinksForNote({
    required String userId,
    required String noteId,
  }) {
    return db
        .listEntities(userId: userId, entityType: SyncEntityType.noteLink)
        .where((LocalEntityRow l) => l.payload['noteId'] == noteId)
        .toList();
  }

  String createGoal({
    required String userId,
    required String title,
    String? description,
    String? category,
    required String startDateIso,
    required String targetDateIso,
    List<String> milestoneTitles = const <String>[],
    List<GoalMilestoneDraft> milestoneDrafts = const <GoalMilestoneDraft>[],
    DateTime? nowUtc,
  }) {
    final String goalId = _uuid.v4();
    db.mutateLocalAndEnqueueOutbox(
      userId: userId,
      entityType: SyncEntityType.goal,
      entityId: goalId,
      op: 'CREATE',
      payload: <String, dynamic>{
        'id': goalId,
        'title': title.trim(),
        if (description != null && description.trim().isNotEmpty)
          'description': description.trim(),
        if (category != null && category.trim().isNotEmpty)
          'category': category.trim(),
        'status': 'ACTIVE',
        'startOn': startDateIso,
        'targetOn': targetDateIso,
        'startDate': startDateIso,
        'targetDate': targetDateIso,
      },
      nowUtc: nowUtc,
    );

    int pos = 1000;
    // 1. Lưu các cột mốc từ milestoneTitles (cho tương thích ngược)
    for (int i = 0; i < milestoneTitles.length; i++) {
      final String t = milestoneTitles[i].trim();
      if (t.isEmpty) continue;
      final String msId = _uuid.v4();
      db.mutateLocalAndEnqueueOutbox(
        userId: userId,
        entityType: SyncEntityType.goalMilestone,
        entityId: msId,
        op: 'CREATE',
        payload: <String, dynamic>{
          'id': msId,
          'goalId': goalId,
          'title': t,
          'position': pos,
          'completed': false,
          'completedAt': null,
        },
        nowUtc: nowUtc,
      );
      pos += 1000;
    }

    // 2. Lưu các cột mốc chi tiết từ milestoneDrafts (kèm dueOn)
    for (int i = 0; i < milestoneDrafts.length; i++) {
      final GoalMilestoneDraft d = milestoneDrafts[i];
      final String t = d.title.trim();
      if (t.isEmpty) continue;
      final String msId = _uuid.v4();
      db.mutateLocalAndEnqueueOutbox(
        userId: userId,
        entityType: SyncEntityType.goalMilestone,
        entityId: msId,
        op: 'CREATE',
        payload: <String, dynamic>{
          'id': msId,
          'goalId': goalId,
          'title': t,
          if (d.dueOnIso != null && d.dueOnIso!.trim().isNotEmpty)
            'dueOn': d.dueOnIso!.trim(),
          'position': pos,
          'completed': false,
          'completedAt': null,
        },
        nowUtc: nowUtc,
      );
      pos += 1000;
    }
    return goalId;
  }

  /// Cập nhật thông tin mục tiêu dài hạn (`goal` — `SYNC_PROTOCOL.md` §3.8).
  void updateGoal({
    required String userId,
    required String goalId,
    required String title,
    String? description,
    String? category,
    required String startDateIso,
    required String targetDateIso,
    String? status,
    DateTime? nowUtc,
  }) {
    final LocalEntityRow? existing = db.getEntity(
      userId: userId,
      entityType: SyncEntityType.goal,
      entityId: goalId,
    );
    if (existing == null) return;

    final Map<String, dynamic> updated = <String, dynamic>{
      ...existing.payload,
      'title': title.trim(),
      if (description != null) 'description': description.trim(),
      if (category != null) 'category': category.trim(),
      if (status != null) 'status': status,
      'startOn': startDateIso,
      'targetOn': targetDateIso,
      'startDate': startDateIso,
      'targetDate': targetDateIso,
    };

    db.mutateLocalAndEnqueueOutbox(
      userId: userId,
      entityType: SyncEntityType.goal,
      entityId: goalId,
      op: 'UPDATE',
      payload: updated,
      nowUtc: nowUtc,
    );
  }

  /// Xóa mục tiêu dài hạn (cascade xóa cột mốc + Outbox `DELETE`).
  void deleteGoal({
    required String userId,
    required String goalId,
    DateTime? nowUtc,
  }) {
    db.mutateLocalAndEnqueueOutbox(
      userId: userId,
      entityType: SyncEntityType.goal,
      entityId: goalId,
      op: 'DELETE',
      payload: <String, dynamic>{'id': goalId},
      nowUtc: nowUtc,
    );
  }

  /// Xóa ghi chú (`note` — `DELETE` cascade `note_link` theo D180/D189).
  void deleteNote({
    required String userId,
    required String noteId,
    DateTime? nowUtc,
  }) {
    db.mutateLocalAndEnqueueOutbox(
      userId: userId,
      entityType: SyncEntityType.note,
      entityId: noteId,
      op: 'DELETE',
      payload: <String, dynamic>{'id': noteId},
      nowUtc: nowUtc,
    );
  }

  /// Xóa liên kết của ghi chú (`note_link`).
  void deleteNoteLink({
    required String userId,
    required String linkId,
    DateTime? nowUtc,
  }) {
    db.mutateLocalAndEnqueueOutbox(
      userId: userId,
      entityType: SyncEntityType.noteLink,
      entityId: linkId,
      op: 'DELETE',
      payload: <String, dynamic>{'id': linkId},
      nowUtc: nowUtc,
    );
  }

  static String _normalizeReviewPeriodType(String raw) {
    final String upper = raw.trim().toUpperCase();
    if (upper == 'DAILY') return 'DAY';
    if (upper == 'WEEKLY') return 'WEEK';
    if (upper == 'MONTHLY') return 'MONTH';
    return upper;
  }

  /// Lưu bản nhìn lại (`review` — `PHASE_7` D202 dòng 59, `SYNC_PROTOCOL.md` §3.9 dòng 181 & `openapi.yaml` dòng 6360–6378).
  String saveLifeReview({
    required String userId,
    required String periodType, // DAY | WEEK | MONTH
    required String periodKey, // YYYY-MM-DD (ngày bắt đầu kỳ theo D192)
    required String reflection,
    int? mood, // 1..5
    DateTime? nowUtc,
  }) {
    final String id = _uuid.v4();
    db.mutateLocalAndEnqueueOutbox(
      userId: userId,
      entityType: SyncEntityType.lifeReview,
      entityId: id,
      op: 'CREATE',
      payload: <String, dynamic>{
        'id': id,
        'periodType': _normalizeReviewPeriodType(periodType),
        'periodKey': periodKey,
        'reflection': reflection.trim(),
        if (mood != null) 'mood': mood.clamp(1, 5),
      },
      nowUtc: nowUtc,
    );
    return id;
  }

  // ===========================================================================
  // CỘT MỐC MỤC TIÊU (`milestone`) — thêm / sửa / đánh dấu / xóa cục bộ + Outbox
  // ===========================================================================

  /// Thêm cột mốc mới cho Mục tiêu (`milestone` trong `SYNC_PROTOCOL.md` §3.8).
  String addMilestone({
    required String userId,
    required String goalId,
    required String title,
    String? dueOnIso,
    DateTime? nowUtc,
  }) {
    final List<LocalEntityRow> existing = db
        .listEntities(userId: userId, entityType: SyncEntityType.goalMilestone)
        .where((LocalEntityRow m) => m.payload['goalId'] == goalId)
        .toList();
    int maxPos = 0;
    for (final LocalEntityRow m in existing) {
      final int p = (m.payload['position'] as num?)?.toInt() ?? 0;
      if (p > maxPos) maxPos = p;
    }

    final String id = _uuid.v4();
    db.mutateLocalAndEnqueueOutbox(
      userId: userId,
      entityType: SyncEntityType.goalMilestone,
      entityId: id,
      op: 'CREATE',
      payload: <String, dynamic>{
        'id': id,
        'goalId': goalId,
        'title': title.trim(),
        if (dueOnIso != null && dueOnIso.trim().isNotEmpty)
          'dueOn': dueOnIso.trim(),
        'position': maxPos + 1000,
        'completed': false,
        'completedAt': null,
      },
      nowUtc: nowUtc,
    );
    return id;
  }

  /// Cập nhật tiêu đề, hạn (`dueOn`) hoặc trạng thái hoàn thành của cột mốc.
  void updateMilestone({
    required String userId,
    required String milestoneId,
    String? title,
    String? dueOnIso,
    bool? completed,
    DateTime? nowUtc,
  }) {
    final LocalEntityRow? existing = db.getEntity(
      userId: userId,
      entityType: SyncEntityType.goalMilestone,
      entityId: milestoneId,
    );
    if (existing == null) return;

    final DateTime now = (nowUtc ?? DateTime.now()).toUtc();
    final bool nextCompleted =
        completed ?? (existing.payload['completed'] == true);
    final Map<String, dynamic> updated = <String, dynamic>{
      ...existing.payload,
      if (title != null) 'title': title.trim(),
      if (dueOnIso != null)
        'dueOn': dueOnIso.trim().isEmpty ? null : dueOnIso.trim(),
      'completed': nextCompleted,
      'completedAt': nextCompleted ? now.toIso8601String() : null,
    };

    db.mutateLocalAndEnqueueOutbox(
      userId: userId,
      entityType: SyncEntityType.goalMilestone,
      entityId: milestoneId,
      op: 'UPDATE',
      payload: updated,
      nowUtc: nowUtc,
    );
  }

  /// Đảo nhanh trạng thái hoàn thành cột mốc.
  void toggleMilestoneCompletion({
    required String userId,
    required String milestoneId,
    DateTime? nowUtc,
  }) {
    final LocalEntityRow? existing = db.getEntity(
      userId: userId,
      entityType: SyncEntityType.goalMilestone,
      entityId: milestoneId,
    );
    if (existing == null) return;
    final bool currentlyDone = existing.payload['completed'] == true;
    updateMilestone(
      userId: userId,
      milestoneId: milestoneId,
      completed: !currentlyDone,
      nowUtc: nowUtc,
    );
  }

  /// Xóa cột mốc (tombstone cục bộ + Outbox `DELETE`).
  void deleteMilestone({
    required String userId,
    required String milestoneId,
    DateTime? nowUtc,
  }) {
    db.mutateLocalAndEnqueueOutbox(
      userId: userId,
      entityType: SyncEntityType.goalMilestone,
      entityId: milestoneId,
      op: 'DELETE',
      payload: <String, dynamic>{'id': milestoneId},
      nowUtc: nowUtc,
    );
  }
}

