import 'dart:async';
import 'dart:convert';

import 'package:dio/dio.dart';

import '../database/app_database.dart';
import '../l10n/vi_strings.dart';
import '../network/api_client.dart';
import 'entity_types.dart';
import 'three_way_merger.dart';

/// Kết quả tổng hợp sau một chu kỳ đồng bộ (`push` + `pull`).
class SyncCycleSummary {
  const SyncCycleSummary({
    required this.pushedApplied,
    required this.pushedDuplicates,
    required this.pushedAutoMerged,
    required this.pushedConflictsNeedUser,
    required this.pushedRejected,
    required this.pulledChanges,
    required this.newCursor,
    required this.hadNetworkError,
    this.errorMessageVi,
  });

  final int pushedApplied;
  final int pushedDuplicates;
  final int pushedAutoMerged;
  final int pushedConflictsNeedUser;
  final int pushedRejected;
  final int pulledChanges;
  final int newCursor;
  final bool hadNetworkError;
  final String? errorMessageVi;
}

/// Lựa chọn khi người dùng đăng xuất trong lúc vẫn còn thay đổi chưa đồng bộ (`MOBILE_MASTER_PLAN.md` §3.5).
enum DirtyLogoutChoice {
  syncThenLogout,
  exportBackupThenLogout,
  forceDiscardAndLogout,
}

/// Bộ máy đồng bộ trung tâm của LifeOS (`SyncEngine`).
///
/// Tuân thủ nghiêm ngặt `SYNC_PROTOCOL.md`, `SYNC_RULES.md`, `FLUTTER_RULES.md`
/// và `PHASE_5_SYNC_ADVANCED.md`.
class SyncEngine {
  SyncEngine({
    required this.db,
    required this.apiClient,
  });

  final AppDatabase db;
  final LifeOsApiClient apiClient;

  static const int maxPushBatchItems = 50;
  static const int maxPushBatchBytes = 870400; // ~850 KB ngưỡng an toàn dưới 1 MB
  static const int defaultPullLimit = 200;

  /// Mã hóa số thứ tự `seq` sang base64url không padding (`SYNC_PROTOCOL.md` §6 mục 3, `D89`).
  static String encodeCursor(int seq) {
    if (seq <= 0) return '';
    return base64Url.encode(utf8.encode(seq.toString())).replaceAll('=', '');
  }

  /// Giải mã `nextCursor` (base64url của chuỗi số thập phân `seq`, hoặc số nguyên) về `int`.
  static int decodeCursor(dynamic rawCursor, int fallback) {
    if (rawCursor == null) return fallback;
    if (rawCursor is num) return rawCursor.toInt();
    final String text = rawCursor.toString().trim();
    if (text.isEmpty) return fallback;
    final int? directInt = int.tryParse(text);
    if (directInt != null) return directInt;
    try {
      final String decoded =
          utf8.decode(base64Url.decode(base64Url.normalize(text)));
      final int? parsedDecoded = int.tryParse(decoded);
      if (parsedDecoded != null) return parsedDecoded;
    } catch (_) {
      // Nếu không phải base64url hợp lệ
    }
    return fallback;
  }

  bool _isSyncing = false;
  bool get isSyncing => _isSyncing;

  /// Thực thi một chu kỳ đồng bộ đầy đủ:
  /// 1. Kiểm tra `sync_paused`.
  /// 2. Khôi phục các mutation `IN_FLIGHT` bị kẹt từ lần mất mạng trước.
  /// 3. Đẩy các lô `PENDING` từ `outbox` qua `POST /sync/push`.
  /// 4. Kéo thay đổi mới qua `GET /sync/pull` (vòng lặp `hasMore == true`).
  Future<SyncCycleSummary> runSyncCycle({
    required String userId,
    DateTime? nowUtc,
  }) async {
    final LocalSyncStateRecord state = db.getSyncState(userId);
    if (state.syncPaused || _isSyncing) {
      return SyncCycleSummary(
        pushedApplied: 0,
        pushedDuplicates: 0,
        pushedAutoMerged: 0,
        pushedConflictsNeedUser: 0,
        pushedRejected: 0,
        pulledChanges: 0,
        newCursor: state.lastCursor,
        hadNetworkError: false,
      );
    }

    _isSyncing = true;
    final Stopwatch stopwatch = Stopwatch()..start();
    int applied = 0;
    int duplicates = 0;
    int autoMerged = 0;
    int conflictsNeedUser = 0;
    int rejected = 0;
    int pulled = 0;
    int currentCursor = state.lastCursor;

    try {
      // Khôi phục IN_FLIGHT bị đứt kết nối lần trước về PENDING (giữ nguyên mutationId)
      db.resetInFlightToPending(userId);

      final DateTime effectiveNow = (nowUtc ?? DateTime.now()).toUtc();

      // 1. Kiểm tra tuổi mutation > 30 ngày trước khi gửi
      final List<OutboxMutationRecord> allPending = db.listOutboxMutations(
        userId: userId,
        statuses: const <String>['PENDING'],
      );
      for (final OutboxMutationRecord mut in allPending) {
        if (mut.isOlderThan30Days(effectiveNow)) {
          db.markMutationNeedsUserAction(
            mutationId: mut.mutationId,
            status: 'NEEDS_USER_ACTION',
            errorCode: 'MUTATION_TOO_OLD',
            errorMessage: ViStrings.mapBackendErrorCode('MUTATION_TOO_OLD'),
          );
          rejected++;
        }
      }

      // 2. Đẩy theo lô FIFO (<= 50 mục và <= 850 KB)
      while (true) {
        final List<OutboxMutationRecord> pendingList = db.listOutboxMutations(
          userId: userId,
          statuses: const <String>['PENDING'],
          limit: maxPushBatchItems,
        );
        if (pendingList.isEmpty) break;

        final List<OutboxMutationRecord> batch = _sliceSafeBatch(pendingList);
        if (batch.isEmpty) break;

        db.markMutationsInFlight(
          batch.map((OutboxMutationRecord m) => m.mutationId).toList(),
        );

        final Response<dynamic> pushResp = await apiClient.dio.post<dynamic>(
          '/sync/push',
          data: <String, dynamic>{
            'deviceId': state.deviceId,
            'mutations': batch
                .map((OutboxMutationRecord m) => m.toPushWireJson())
                .toList(),
          },
        );

        final Map<String, dynamic> pushData =
            (pushResp.data as Map<dynamic, dynamic>).cast<String, dynamic>();
        final List<dynamic> results =
            (pushData['results'] as List<dynamic>?) ?? const <dynamic>[];

        final Map<String, OutboxMutationRecord> byMutId =
            <String, OutboxMutationRecord>{
          for (final OutboxMutationRecord m in batch) m.mutationId: m,
        };

        for (final dynamic rawRes in results) {
          final Map<String, dynamic> item =
              (rawRes as Map<dynamic, dynamic>).cast<String, dynamic>();
          final String mutId = item['mutationId'] as String;
          final String rawStatus =
              (item['status'] as String? ?? 'REJECTED').toUpperCase();
          final bool isDuplicate =
              rawStatus == 'DUPLICATE' || rawStatus == 'ALREADY_APPLIED';
          final String effectiveStatus = isDuplicate
              ? ((item['originalStatus'] as String?) ?? 'APPLIED').toUpperCase()
              : rawStatus;
          final OutboxMutationRecord? origin = byMutId[mutId];
          if (origin == null) continue;

          if (effectiveStatus == 'APPLIED') {
            final int version =
                ((item['entityVersion'] ?? item['version']) as num?)?.toInt() ??
                    (origin.baseVersion + 1);
            final String serverUpdatedAt =
                (item['updatedAt'] as String?) ??
                    effectiveNow.toIso8601String();
            final Map<String, dynamic>? serverEntity =
                item['serverCurrent'] is Map<dynamic, dynamic>
                    ? (item['serverCurrent'] as Map<dynamic, dynamic>)
                        .cast<String, dynamic>()
                    : null;

            db.completeOutboxMutationApplied(
              userId: userId,
              mutation: origin,
              assignedVersion: version,
              serverUpdatedAt: serverUpdatedAt,
              serverEntityState: serverEntity,
            );
            if (!isDuplicate) {
              applied++;
            } else {
              duplicates++;
            }
          } else if (effectiveStatus == 'CONFLICT') {
            final Map<String, dynamic> rawSnap =
                ((item['serverSnapshot'] ?? item['serverCurrent'])
                            as Map<dynamic, dynamic>? ??
                        <dynamic, dynamic>{})
                    .cast<String, dynamic>();
            final Map<String, dynamic> serverCurrent =
                rawSnap['data'] is Map<dynamic, dynamic>
                    ? <String, dynamic>{
                        ...(rawSnap['data'] as Map<dynamic, dynamic>)
                            .cast<String, dynamic>(),
                        'id': (rawSnap['entityId'] ?? origin.entityId)
                            .toString(),
                        'version': (rawSnap['version'] as num?)?.toInt() ??
                            ((item['entityVersion'] as num?)?.toInt() ??
                                (origin.baseVersion + 1)),
                      }
                    : rawSnap;
            final Map<String, dynamic> baseSnap = db.getBaseSnapshot(
                  userId: userId,
                  entityType: origin.entityType,
                  entityId: origin.entityId,
                ) ??
                <String, dynamic>{};

            final ThreeWayMergeOutcome mergeOutcome = ThreeWayMerger.merge(
              entityType: origin.entityType,
              baseSnapshot: baseSnap,
              serverCurrent: serverCurrent,
              clientLocal: origin.payload,
            );

            if (mergeOutcome.autoMerged &&
                origin.entityType.conflictStrategy ==
                    ConflictStrategy.field3Way) {
              // Tự động hợp nhất không đụng trường -> xếp lại PENDING với baseVersion mới
              db.completeOutboxMutationApplied(
                userId: userId,
                mutation: origin,
                assignedVersion: mergeOutcome.newBaseVersion,
                serverUpdatedAt: effectiveNow.toIso8601String(),
                serverEntityState: serverCurrent,
              );
              db.mutateLocalAndEnqueueOutbox(
                userId: userId,
                entityType: origin.entityType,
                entityId: origin.entityId,
                op: 'UPDATE',
                payload: mergeOutcome.mergedPayload,
                nowUtc: effectiveNow,
              );
              autoMerged++;
            } else {
              db.markMutationNeedsUserAction(
                mutationId: origin.mutationId,
                status: 'NEEDS_USER_ACTION',
                errorCode: (item['errorCode'] as String?) ?? 'VERSION_CONFLICT',
                errorMessage: ViStrings.mapBackendErrorCode('VERSION_CONFLICT'),
                serverCurrent: serverCurrent,
              );
              conflictsNeedUser++;
            }
          } else {
            // REJECTED
            final String errCode =
                (item['errorCode'] as String?) ?? 'VALIDATION_ERROR';
            db.markMutationNeedsUserAction(
              mutationId: origin.mutationId,
              status: 'FAILED_PERMANENT',
              errorCode: errCode,
              errorMessage: ViStrings.mapBackendErrorCode(
                errCode,
                serverMessage: item['message'] as String?,
              ),
            );
            rejected++;
          }
        }
      }

      // 3. Kéo thay đổi từ Server qua GET /sync/pull (lặp cho đến khi hasMore == false)
      bool hasMore = true;
      int safetyPages = 0;
      while (hasMore && safetyPages < 500) {
        safetyPages++;
        final String encodedCursor = encodeCursor(currentCursor);
        final Response<dynamic> pullResp = await apiClient.dio.get<dynamic>(
          '/sync/pull',
          queryParameters: <String, dynamic>{
            if (encodedCursor.isNotEmpty) 'cursor': encodedCursor,
            'limit': defaultPullLimit,
          },
        );
        final Map<String, dynamic> pullData =
            (pullResp.data as Map<dynamic, dynamic>).cast<String, dynamic>();
        final List<dynamic> rawChanges =
            (pullData['changes'] as List<dynamic>?) ?? const <dynamic>[];
        final List<Map<String, dynamic>> pageChanges = rawChanges
            .map((dynamic e) => (e as Map<dynamic, dynamic>).cast<String, dynamic>())
            .toList();

        final int nextCursor =
            decodeCursor(pullData['nextCursor'], currentCursor);
        hasMore = (pullData['hasMore'] as bool?) ?? false;

        db.applyPulledChangesBatch(
          userId: userId,
          changes: pageChanges,
          nextCursor: nextCursor,
          syncedAtIso: effectiveNow.toIso8601String(),
        );
        pulled += pageChanges.length;
        currentCursor = nextCursor;

        if (pageChanges.isEmpty) {
          break;
        }
      }

      stopwatch.stop();
      db.appendSyncLog(
        userId: userId,
        direction: 'SYNC_CYCLE',
        outcome: 'SUCCESS',
        latencyMs: stopwatch.elapsedMilliseconds,
        details:
            'applied=$applied, dup=$duplicates, merged=$autoMerged, conflict=$conflictsNeedUser, pulled=$pulled, cursor=$currentCursor',
      );

      return SyncCycleSummary(
        pushedApplied: applied,
        pushedDuplicates: duplicates,
        pushedAutoMerged: autoMerged,
        pushedConflictsNeedUser: conflictsNeedUser,
        pushedRejected: rejected,
        pulledChanges: pulled,
        newCursor: currentCursor,
        hadNetworkError: false,
      );
    } catch (err) {
      stopwatch.stop();
      db.resetInFlightToPending(userId);
      final LifeOsApiException norm = LifeOsApiClient.normalizeError(err);
      db.appendSyncLog(
        userId: userId,
        direction: 'SYNC_CYCLE',
        outcome: 'ERROR',
        latencyMs: stopwatch.elapsedMilliseconds,
        errorCode: norm.errorCode,
        details: norm.userMessageVi,
      );
      return SyncCycleSummary(
        pushedApplied: applied,
        pushedDuplicates: duplicates,
        pushedAutoMerged: autoMerged,
        pushedConflictsNeedUser: conflictsNeedUser,
        pushedRejected: rejected,
        pulledChanges: pulled,
        newCursor: currentCursor,
        hadNetworkError: true,
        errorMessageVi: norm.userMessageVi,
      );
    } finally {
      _isSyncing = false;
    }
  }

  /// Giải quyết xung đột từ lựa chọn của người dùng trên Conflict Bottom Sheet.
  void resolveConflictByUser({
    required String userId,
    required OutboxMutationRecord conflictMutation,
    required Map<String, dynamic> chosenPayload,
    required int serverVersion,
    bool alsoCreateConflictCopyForNote = false,
  }) {
    final DateTime now = DateTime.now().toUtc();
    // 1. Chấp nhận phiên bản server làm mốc baseVersion mới
    db.completeOutboxMutationApplied(
      userId: userId,
      mutation: conflictMutation,
      assignedVersion: serverVersion,
      serverUpdatedAt: now.toIso8601String(),
      serverEntityState: conflictMutation.serverConflict,
    );

    // 2. Nếu là Note và người dùng chọn "Giữ cả hai thành bản sao"
    if (alsoCreateConflictCopyForNote &&
        conflictMutation.entityType == SyncEntityType.note) {
      final String copyId =
          '${conflictMutation.entityId.substring(0, 24)}-copy-${now.millisecondsSinceEpoch % 1000000}';
      final Map<String, dynamic> copyPayload =
          ThreeWayMerger.createNoteConflictCopy(
        newNoteId: copyId,
        clientNotePayload: conflictMutation.payload,
        conflictDateLabel: now.toIso8601String().substring(0, 10),
      );
      db.mutateLocalAndEnqueueOutbox(
        userId: userId,
        entityType: SyncEntityType.note,
        entityId: copyId,
        op: 'CREATE',
        payload: copyPayload,
        nowUtc: now,
      );
      return;
    }

    // 3. Đẩy lại bản đã hợp nhất với baseVersion = serverVersion
    db.mutateLocalAndEnqueueOutbox(
      userId: userId,
      entityType: conflictMutation.entityType,
      entityId: conflictMutation.entityId,
      op: 'UPDATE',
      payload: chosenPayload,
      nowUtc: now,
    );
  }

  /// Quy trình Đăng xuất an toàn 3 lựa chọn (`MOBILE_MASTER_PLAN.md` §3.5).
  Future<({bool loggedOut, Map<String, dynamic>? exportedBackup})>
      performSafeLogout({
    required String userId,
    DirtyLogoutChoice? choiceWhenDirty,
    String? confirmationWordForForceDiscard,
  }) async {
    final List<OutboxMutationRecord> pending =
        db.listOutboxMutations(userId: userId);

    if (pending.isNotEmpty) {
      if (choiceWhenDirty == null) {
        return (loggedOut: false, exportedBackup: null);
      }
      switch (choiceWhenDirty) {
        case DirtyLogoutChoice.syncThenLogout:
          final SyncCycleSummary syncRes = await runSyncCycle(userId: userId);
          final List<OutboxMutationRecord> remaining =
              db.listOutboxMutations(userId: userId);
          if (syncRes.hadNetworkError || remaining.isNotEmpty) {
            return (loggedOut: false, exportedBackup: null);
          }
        case DirtyLogoutChoice.exportBackupThenLogout:
          final Map<String, dynamic> backup =
              db.exportUnsyncedBackupJson(userId);
          await _revokeServerTokenAndClearLocal(userId);
          return (loggedOut: true, exportedBackup: backup);
        case DirtyLogoutChoice.forceDiscardAndLogout:
          if (confirmationWordForForceDiscard?.trim().toUpperCase() !=
              ViStrings.logoutForceConfirmWord) {
            throw ArgumentError(
              'Phải nhập chính xác từ khóa "${ViStrings.logoutForceConfirmWord}" để buộc xóa dữ liệu chưa đồng bộ.',
            );
          }
      }
    }

    await _revokeServerTokenAndClearLocal(userId);
    return (loggedOut: true, exportedBackup: null);
  }

  Future<void> _revokeServerTokenAndClearLocal(String userId) async {
    try {
      final String? refresh = await apiClient.tokenStorage.readRefreshToken();
      if (refresh != null && refresh.isNotEmpty) {
        await apiClient.dio.post<dynamic>(
          '/auth/logout',
          data: <String, dynamic>{'refreshToken': refresh},
        );
      }
    } catch (_) {
      // Ngay cả khi mạng lỗi lúc thu hồi refresh token, vẫn dọn sạch phiên cục bộ
    }
    await apiClient.tokenStorage.clearTokensOnly();
    db.clearUserData(userId);
  }

  List<OutboxMutationRecord> _sliceSafeBatch(
    List<OutboxMutationRecord> candidates,
  ) {
    final List<OutboxMutationRecord> result = <OutboxMutationRecord>[];
    int runningBytes = 0;
    for (final OutboxMutationRecord m in candidates) {
      if (result.length >= maxPushBatchItems) break;
      final int itemBytes = utf8.encode(jsonEncode(m.toPushWireJson())).length;
      if (result.isNotEmpty && (runningBytes + itemBytes) > maxPushBatchBytes) {
        break;
      }
      result.add(m);
      runningBytes += itemBytes;
    }
    return result;
  }
}

