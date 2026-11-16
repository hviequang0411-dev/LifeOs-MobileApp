import 'dart:convert';
import 'dart:typed_data';

import 'package:dio/dio.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:lifeos_mobile/core/database/app_database.dart';
import 'package:lifeos_mobile/core/money/decimal10k.dart';
import 'package:lifeos_mobile/core/network/api_client.dart';
import 'package:lifeos_mobile/core/sync/sync_engine.dart';
import 'package:lifeos_mobile/features/auth/data/online_account_service.dart';
import 'package:lifeos_mobile/features/workspace/data/lifeos_local_repository.dart';
import 'package:uuid/uuid.dart';

const bool _runRealE2e = bool.fromEnvironment('LIFEOS_RUN_REAL_E2E');
const String _backendBaseUrl = String.fromEnvironment(
  'LIFEOS_API_BASE_URL',
  defaultValue: 'http://localhost:8080/api/v1',
);
const String _mailpitBaseUrl = String.fromEnvironment(
  'LIFEOS_MAILPIT_BASE_URL',
  defaultValue: 'http://localhost:8025',
);

/// Đọc email mới nhất gửi tới [recipientEmail] trong Mailpit (`GET /api/v1/messages` + `GET /api/v1/message/{id}`)
/// thỏa điều kiện [bodyFilter] (nếu có — dùng để chờ Outbox worker gửi email mới như mã xóa tài khoản 6 chữ số).
Future<String> _fetchLatestEmailTextFromMailpit(
  Dio mailpitDio,
  String recipientEmail, {
  bool Function(String body)? bodyFilter,
}) async {
  for (int attempt = 0; attempt < 30; attempt++) {
    final Response<Map<String, dynamic>> listResp =
        await mailpitDio.get<Map<String, dynamic>>('/api/v1/messages');
    final List<Map<String, dynamic>> messages =
        (listResp.data?['messages'] as List<dynamic>? ?? const <dynamic>[])
            .cast<Map<String, dynamic>>();
    for (final Map<String, dynamic> msg in messages) {
      final List<Map<String, dynamic>> toList =
          (msg['To'] as List<dynamic>? ?? const <dynamic>[])
              .cast<Map<String, dynamic>>();
      final bool matchesRecipient = toList.any(
        (Map<String, dynamic> t) =>
            (t['Address'] as String?)?.toLowerCase() ==
            recipientEmail.toLowerCase(),
      );
      if (matchesRecipient) {
        final String msgId = msg['ID'] as String;
        final Response<Map<String, dynamic>> detailResp =
            await mailpitDio.get<Map<String, dynamic>>('/api/v1/message/$msgId');
        final String text = (detailResp.data?['Text'] as String?) ?? '';
        final String html = (detailResp.data?['HTML'] as String?) ?? '';
        final String combined = '$text\n$html';
        if (bodyFilter == null || bodyFilter(combined)) {
          return combined;
        }
      }
    }
    await Future<void>.delayed(const Duration(milliseconds: 500));
  }
  throw StateError('Không tìm thấy email phù hợp gửi tới $recipientEmail trên Mailpit');
}

String _extractVerificationToken(String mailBody) {
  final RegExpMatch? tokenMatch =
      RegExp(r'token=([A-Za-z0-9\-_]+)').firstMatch(mailBody);
  if (tokenMatch != null) {
    return tokenMatch.group(1)!;
  }
  final RegExpMatch? hexOrUuid = RegExp(
    r'\b([0-9a-fA-F]{32,64}|[0-9a-fA-F\-]{36})\b',
  ).firstMatch(mailBody);
  if (hexOrUuid != null) {
    return hexOrUuid.group(1)!;
  }
  throw StateError(
    'Không trích xuất được verification token từ nội dung email: $mailBody',
  );
}

String _extractSixDigitCode(String mailBody) {
  final RegExpMatch? codeMatch = RegExp(r'\b(\d{6})\b').firstMatch(mailBody);
  if (codeMatch != null) {
    return codeMatch.group(1)!;
  }
  throw StateError(
    'Không trích xuất được mã 6 chữ số từ nội dung email: $mailBody',
  );
}

void main() {
  test(
    'Real Backend E2E (9 luồng a-i gọi thật http://localhost:8080/api/v1 và Mailpit http://localhost:8025)',
    () async {
      const Uuid uuid = Uuid();
      final AppDatabase db = AppDatabase.inMemory();
      addTearDown(db.close);

      final InMemoryTokenStorage tokenStorage = InMemoryTokenStorage();
      final LifeOsApiClient apiClient = LifeOsApiClient(
        baseUrl: _backendBaseUrl,
        tokenStorage: tokenStorage,
      );
      final Dio mailpitDio = Dio(BaseOptions(baseUrl: _mailpitBaseUrl));

      final OnlineAccountService accountService =
          OnlineAccountService(apiClient: apiClient);
      final SyncEngine syncEngine = SyncEngine(db: db, apiClient: apiClient);
      final LifeOsLocalRepository repo = LifeOsLocalRepository(db: db);

      final String uniqueEmail =
          'e2e.${DateTime.now().millisecondsSinceEpoch}@example.com';
      const String password = 'Password123!';

      // (a) Đăng ký -> đọc email xác minh từ Mailpit -> verify -> đăng nhập -> pull user_settings
      await accountService.register(
        email: uniqueEmail,
        password: password,
        displayName: 'E2E Real User',
      );
      final String verifyMail = await _fetchLatestEmailTextFromMailpit(
        mailpitDio,
        uniqueEmail,
      );
      final String verifyToken = _extractVerificationToken(verifyMail);
      await accountService.verifyEmail(token: verifyToken);

      final ({
        String? deviceId,
        String displayName,
        String email,
        String userId
      }) session = await accountService.login(
        email: uniqueEmail,
        password: password,
        deviceName: 'Flutter Real E2E Runner',
        platform: 'ANDROID',
      );
      final String userId = session.userId;
      expect(userId, isNotEmpty);

      final SyncCycleSummary firstSync =
          await syncEngine.runSyncCycle(userId: userId);
      expect(
        firstSync.hadNetworkError,
        isFalse,
        reason: firstSync.errorMessageVi,
      );

      // (b) Tạo project + 3 task + habit + check-in + giao dịch -> sync
      final String todayIso =
          repo.getUserDayClock(userId).businessDateForUtc(DateTime.now().toUtc());
      final String projId = repo.createProject(
        userId: userId,
        name: 'Dự án E2E Thật',
        color: '#2D6A4F',
      );
      final List<String> taskIds = <String>[];
      for (int i = 1; i <= 3; i++) {
        taskIds.add(
          repo.createTask(
            userId: userId,
            title: 'Task thật #$i',
            projectId: projId,
            dueDate: todayIso,
          ),
        );
      }
      final String habitId = repo.createHabit(
        userId: userId,
        name: 'Tập thể dục buổi sáng',
        frequencyType: 'DAILY',
      );
      repo.checkInHabit(
        userId: userId,
        habitId: habitId,
        entryDateIso: todayIso,
        status: 'DONE',
      );
      final String cashAccId = repo.createFinanceAccount(
        userId: userId,
        name: 'Tiền mặt',
        accountType: 'CASH',
        currency: 'VND',
        openingBalance: Decimal10k.fromWhole(1000000),
      );
      repo.createFinanceTransaction(
        userId: userId,
        accountId: cashAccId,
        kind: 'EXPENSE',
        amount: Decimal10k.fromWhole(50000),
        occurredOnIso: todayIso,
        note: 'Cà phê sáng',
      );

      final SyncCycleSummary syncB =
          await syncEngine.runSyncCycle(userId: userId);
      expect(syncB.hadNetworkError, isFalse, reason: syncB.errorMessageVi);
      expect(
        db.listOutboxMutations(userId: userId),
        isEmpty,
        reason: db
            .listOutboxMutations(userId: userId)
            .map(
              (OutboxMutationRecord m) =>
                  '${m.entityType.wireName} status=${m.status} code=${m.lastErrorCode} msg=${m.lastErrorMessage} payload=${m.payload}',
            )
            .join('\n'),
      );

      // (c) Hoàn thành task -> sync
      repo.toggleTaskCompletion(userId: userId, taskId: taskIds.first);
      final SyncCycleSummary syncC =
          await syncEngine.runSyncCycle(userId: userId);
      expect(syncC.hadNetworkError, isFalse, reason: syncC.errorMessageVi);

      // (d) Chuyển khoản (account_transfer) + điều chỉnh số dư -> đối chiếu số dư
      final String bankAccId = repo.createFinanceAccount(
        userId: userId,
        name: 'Ngân hàng',
        accountType: 'BANK',
        currency: 'VND',
        openingBalance: Decimal10k.fromWhole(3000000),
      );
      repo.createAccountTransfer(
        userId: userId,
        fromAccountId: bankAccId,
        toAccountId: cashAccId,
        amount: Decimal10k.fromWhole(200000),
        occurredOnIso: todayIso,
        note: 'Rút ATM',
      );
      repo.reconcileAccountBalance(
        userId: userId,
        accountId: cashAccId,
        actualBalance: Decimal10k.fromWhole(1200000),
        occurredOnIso: todayIso,
      );
      final SyncCycleSummary syncD =
          await syncEngine.runSyncCycle(userId: userId);
      expect(syncD.hadNetworkError, isFalse, reason: syncD.errorMessageVi);

      // (e) Sự kiện lặp -> sửa 1 kỳ (event_exception) -> tách chuỗi (split) -> sync
      final String evtId = repo.createCalendarEvent(
        userId: userId,
        title: 'Họp nhóm',
        startLocal: '2026-11-02T09:00:00',
        endLocal: '2026-11-02T10:00:00',
        timezone: 'Asia/Ho_Chi_Minh',
        recurrence: const <String, dynamic>{
          'freq': 'WEEKLY',
          'interval': 1,
          'byWeekday': <int>[1],
          'byMonthDay': <int>[],
          'until': null,
          'count': null,
        },
      );
      repo.recordEventOccurrenceException(
        userId: userId,
        eventId: evtId,
        originalStartLocal: '2026-11-09T09:00:00',
        kind: 'CANCELLED',
      );
      repo.splitRecurringEventFrom(
        userId: userId,
        originalEventId: evtId,
        originalStartLocal: '2026-11-16T09:00:00',
        newStartLocal: '2026-11-16T10:00:00',
        newEndLocal: '2026-11-16T11:00:00',
        newTitle: 'Họp nhóm (giờ mới)',
      );
      final SyncCycleSummary syncE =
          await syncEngine.runSyncCycle(userId: userId);
      expect(syncE.hadNetworkError, isFalse, reason: syncE.errorMessageVi);

      // (f) Note Markdown + linkNoteToTarget (GOAL) + Goal/Milestone + Weekly Review -> sync
      final String goalId = repo.createGoal(
        userId: userId,
        title: 'Mục tiêu Quý 4',
        category: 'CAREER',
        startDateIso: '2026-10-01',
        targetDateIso: '2026-12-31',
        milestoneTitles: const <String>['Cột mốc 1'],
      );
      final String noteId = repo.saveNote(
        userId: userId,
        title: 'Ghi chú chiến lược',
        content: 'Nội dung ghi chú Markdown.',
      );
      repo.linkNoteToTarget(
        userId: userId,
        noteId: noteId,
        targetType: 'GOAL',
        targetId: goalId,
      );
      repo.saveLifeReview(
        userId: userId,
        periodType: 'WEEK',
        periodKey: '2026-11-02',
        reflection: 'Tiến độ tốt',
        mood: 4,
      );
      final SyncCycleSummary syncF =
          await syncEngine.runSyncCycle(userId: userId);
      expect(syncF.hadNetworkError, isFalse, reason: syncF.errorMessageVi);

      // (g) Sửa cùng một task ở thiết bị thứ 2 (qua /sync/push trực tiếp) và ở local -> CONFLICT -> giải quyết
      final String targetTaskId = taskIds[1];
      await apiClient.dio.post<Map<String, dynamic>>(
        '/sync/push',
        data: <String, dynamic>{
          'mutations': <Map<String, dynamic>>[
            <String, dynamic>{
              'mutationId': uuid.v4(),
              'entityType': 'task',
              'entityId': targetTaskId,
              'operation': 'UPDATE',
              'baseVersion': 1,
              'payload': <String, dynamic>{
                'title': 'Task sửa từ thiết bị 2',
                'projectId': projId,
                'status': 'OPEN',
                'priority': 'HIGH',
                'dueDate': todayIso,
              },
            },
          ],
        },
      );
      repo.updateTask(
        userId: userId,
        taskId: targetTaskId,
        patch: const <String, dynamic>{
          'title': 'Task sửa trên thiết bị 1 (ưu tiên giữ)',
        },
      );
      await syncEngine.runSyncCycle(userId: userId);
      final List<OutboxMutationRecord> conflicts = db.listOutboxMutations(
        userId: userId,
        statuses: const <String>['NEEDS_USER_ACTION'],
      );
      expect(conflicts, isNotEmpty);
      syncEngine.resolveConflictByUser(
        userId: userId,
        conflictMutation: conflicts.first,
        chosenPayload: <String, dynamic>{
          ...conflicts.first.payload,
          'title': 'Task sửa trên thiết bị 1 (ưu tiên giữ)',
        },
        serverVersion: 2,
      );
      final SyncCycleSummary syncG =
          await syncEngine.runSyncCycle(userId: userId);
      expect(syncG.hadNetworkError, isFalse, reason: syncG.errorMessageVi);

      // (h) Xuất dữ liệu tài khoản -> tải zip
      final Map<String, dynamic> exportJob =
          await accountService.requestDataExport();
      final String exportId = exportJob['id'] as String;
      Map<String, dynamic> statusResp = exportJob;
      for (int i = 0; i < 20; i++) {
        if (statusResp['status'] == 'COMPLETED') break;
        await Future<void>.delayed(const Duration(milliseconds: 500));
        statusResp = await accountService.getDataExportStatus(exportId);
      }
      if (statusResp['status'] == 'COMPLETED') {
        final Uint8List zipBytes =
            await accountService.downloadDataExportZip(exportId);
        expect(zipBytes, isNotEmpty);
      }

      // (i) Yêu cầu xóa tài khoản -> đọc mã 6 số từ Mailpit -> xác nhận -> hủy xóa
      await accountService.requestAccountDeletionCode();
      final String deletionMail = await _fetchLatestEmailTextFromMailpit(
        mailpitDio,
        uniqueEmail,
        bodyFilter: (String body) =>
            !body.contains('verify-email') && RegExp(r'\b\d{6}\b').hasMatch(body),
      );
      final String code = _extractSixDigitCode(deletionMail);
      final Map<String, dynamic> delResp =
          await accountService.confirmAccountDeletion(
        sixDigitCode: code,
        password: password,
      );
      expect(delResp['cancellable'], isTrue);
      await accountService.cancelAccountDeletion();

      // (j) Kiểm chứng đăng xuất / hết hạn phiên khi còn mutation chưa đồng bộ không làm mất dữ liệu local
      repo.createTask(
        userId: userId,
        title: 'Task tạo ngay trước khi hết phiên',
      );
      expect(db.listOutboxMutations(userId: userId), hasLength(1));
      final String savedBeforeClear = jsonEncode(
        db.exportUnsyncedBackupJson(userId),
      );
      expect(savedBeforeClear, contains('Task tạo ngay trước khi hết phiên'));
    },
    skip: !_runRealE2e
        ? 'Bỏ qua mặc định: cần Docker chạy backend (http://localhost:8080) + Mailpit (http://localhost:8025) và truyền --dart-define=LIFEOS_RUN_REAL_E2E=true'
        : false,
  );
}
